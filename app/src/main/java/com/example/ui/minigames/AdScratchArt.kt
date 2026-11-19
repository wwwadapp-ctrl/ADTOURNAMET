package com.example.ui.minigames

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.widget.ImageView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import kotlin.random.Random

// Scratch card game artwork. VISUAL ONLY. Positions are in the stage picture's pixels (768 x 1376).

private fun Dp.sp(): TextUnit = TextUnit(this.value, TextUnitType.Sp)

// ticket box on the easel (stage px)
private const val SC_TX = 139.8f
private const val SC_TY = 606.5f
private const val SC_TW = 488.4f
private const val SC_TH = 306.9f

/** Scratch sound (played at most every 180 ms while the finger moves). */
object ScSfx {
    private var pool: SoundPool? = null
    private var id = 0
    private var last = 0L
    fun prepare(context: Context) {
        if (pool != null) return
        try {
            val p = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            ).build()
            id = p.load(context.applicationContext, R.raw.sc_scratch, 1)
            pool = p
        } catch (_: Throwable) { }
    }
    fun scratch() {
        val now = System.currentTimeMillis()
        if (now - last < 180) return
        last = now
        val p = pool ?: return
        try { p.play(id, 0.6f, 0.6f, 1, 0, 1f) } catch (_: Throwable) { }
    }
}

/** Stage (fitted to width) + strip above it + gold title on the empty sign. */
@Composable
fun ScStage(geo: WhGeo, title: String, subtitle: String, u: Dp) {
    val img = ImageBitmap.imageResource(R.drawable.sc_stage)
    if (geo.top > 0.dp) Canvas(Modifier.fillMaxWidth().height(geo.top + 2.dp)) {
        drawImage(img, srcOffset = IntOffset(0, 0), srcSize = IntSize(img.width, 3),
            dstOffset = IntOffset(geo.left.toPx().toInt(), 0), dstSize = IntSize(geo.sceneW.toPx().toInt() + 1, size.height.toInt() + 1))
    }
    Box(Modifier.offset(x = geo.left, y = geo.top).wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(geo.sceneW, geo.sceneH)) {
        Image(painterResource(R.drawable.sc_stage), null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
    }
    Column(
        Modifier.offset(x = geo.x(138f), y = geo.y(316f)).size(geo.k * 492f, geo.k * 179f),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        com.example.ui.components.AdGoldText(title, fontSize = (u * 6.4f).sp())
        Spacer(Modifier.height(u * 0.6f))
        Text(subtitle, fontSize = (u * 3f).sp(), fontWeight = FontWeight.Black, color = Color(0xFF3A1A00), maxLines = 1,
            modifier = Modifier.clip(RoundedCornerShape(u * 3)).background(Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFE0A82A)))).padding(horizontal = u * 2.6f))
    }
}

/**
 * The golden ticket on the easel: three symbols, the scratch foil (finger erases it),
 * the gold frame, and two purple curtains that part to the sides when [curtainOpen].
 * [mult] 5 = 3 crowns, 2 = 3 diamonds, 1 = 3 coins, 0 = one of each (no match).
 */
@Composable
fun ScTicket(
    geo: WhGeo, u: Dp, mult: Int, ticketNo: Int, purchased: Boolean, revealed: Boolean, curtainOpen: Boolean,
    scratchedPath: Path, foilText: String,
    onScratchPoint: (Offset, Size) -> Unit, onDragEnded: () -> Unit
) {
    val lossOrder = remember(ticketNo) { listOf(listOf(5, 2, 1), listOf(2, 1, 5), listOf(1, 5, 2))[Random.nextInt(3)] }
    val syms = if (mult > 0) listOf(mult, mult, mult) else lossOrder
    val inf = rememberInfiniteTransition(label = "scT")
    val bob by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "scBob")
    val foilAlpha = remember { Animatable(1f) }
    LaunchedEffect(revealed) { if (revealed) foilAlpha.animateTo(0f, tween(500, easing = FastOutSlowInEasing)) else foilAlpha.snapTo(1f) }
    val open by animateFloatAsState(if (curtainOpen) 1f else 0f, tween(750, easing = CubicBezierEasing(0.6f, 0f, 0.25f, 1f)), label = "scCurtain")
    val shine = remember { Animatable(-0.6f) }
    LaunchedEffect(curtainOpen) { if (curtainOpen) { shine.snapTo(-0.6f); kotlinx.coroutines.delay(650); shine.animateTo(1.7f, tween(700)) } }
    val speck = remember { List(220) { floatArrayOf(Random.nextFloat(), Random.nextFloat(), if (Random.nextBoolean()) 1f else 0f) } }
    val textPaint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true; color = android.graphics.Color.argb(190, 90, 58, 4) } }

    val onPoint by rememberUpdatedState(onScratchPoint)
    val onEnd by rememberUpdatedState(onDragEnded)
    val panelW = geo.k * SC_TW * 0.766f
    Box(Modifier.offset(x = geo.x(SC_TX), y = geo.y(SC_TY)).size(geo.k * SC_TW, geo.k * SC_TH).testTag("scratch_ticket")) {
        // inner panel
        Box(Modifier.offset(x = geo.k * SC_TW * 0.116f, y = geo.k * SC_TH * 0.178f).size(panelW, geo.k * SC_TH * 0.644f)
            .clip(RoundedCornerShape(u * 2.4f))
            .background(Brush.radialGradient(listOf(Color(0xFFFFF8E4), Color(0xFFF3E2B8), Color(0xFFD9BE7E))))
        ) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(u * 2.6f, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                syms.forEach { m ->
                    Image(painterResource(symbolRes(m)), null, contentScale = ContentScale.Fit,
                        modifier = Modifier.size(panelW * 0.24f).offset(y = if (revealed && mult > 0) -u * 1.2f * bob else 0.dp))
                }
            }
            if (foilAlpha.value > 0.01f) Canvas(
                Modifier.fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen; alpha = foilAlpha.value }
                    .pointerInput(purchased, revealed) {
                        if (!purchased || revealed) return@pointerInput
                        val currentSize = Size(size.width.toFloat(), size.height.toFloat())
                        detectDragGestures(
                            onDragStart = { offset -> onPoint(offset, currentSize) },
                            onDrag = { change, _ -> change.consume(); onPoint(change.position, currentSize) },
                            onDragEnd = { onEnd() },
                            onDragCancel = { onEnd() }
                        )
                    }
                    .testTag("scratch_foil")
            ) {
                drawRect(Brush.linearGradient(listOf(Color(0xFFC9C9D2), Color(0xFFE8C46A), Color(0xFFF3F3F7), Color(0xFFFFD45A), Color(0xFFB9B9C6)), Offset.Zero, Offset(size.width, size.height)))
                val step = size.height / 13f
                for (i in -10..30) drawLine(Color.White.copy(alpha = 0.18f), Offset(0f, i * step), Offset(size.width, i * step + size.height * 0.4f), strokeWidth = 1.5.dp.toPx())
                speck.forEach { s -> drawRect(if (s[2] > 0f) Color(0x59FFFFFF) else Color(0x598A6A20), Offset(s[0] * size.width, s[1] * size.height), Size(2.dp.toPx() * 0.7f, 2.dp.toPx() * 0.7f)) }
                textPaint.textSize = size.height * 0.088f
                drawIntoCanvas { it.nativeCanvas.drawText("✦ $foilText ✦", size.width / 2, size.height / 2 + textPaint.textSize * 0.35f, textPaint) }
                drawPath(scratchedPath, Color.Transparent, style = Stroke(width = 44.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = BlendMode.Clear)
            }
            // one light sweep after the curtains part
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width * shine.value
                if (shine.value > -0.5f && shine.value < 1.6f) drawRect(Brush.linearGradient(
                    0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.85f), 1f to Color.Transparent,
                    start = Offset(cx - size.width * 0.22f, 0f), end = Offset(cx + size.width * 0.22f, size.height * 0.3f)))
            }
        }
        // gold frame on top
        Image(painterResource(R.drawable.sc_frame), null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        // curtains
        ScCurtains(u, open, Modifier.align(Alignment.Center).requiredSize(geo.k * SC_TW * 1.02f, geo.k * SC_TH * 1.06f))
    }
}
