package com.example.ui.minigames

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.ui.autoludo.AdDice3D
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

// Even/Odd dice game artwork. VISUAL ONLY. Positions are in the stage picture's pixels (768 x 1376).

val DcRed = Color(0xFFCC121F)
private fun Dp.sp(): TextUnit = TextUnit(this.value, TextUnitType.Sp)

/** Dice roll sound (res/raw/dice_roll). */
object DcSfx {
    private var pool: SoundPool? = null
    private var id = 0
    fun prepare(context: Context) {
        if (pool != null) return
        try {
            val p = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            ).build()
            id = p.load(context.applicationContext, R.raw.dice_roll, 1); pool = p
        } catch (_: Throwable) { }
    }
    fun roll() { try { pool?.play(id, 1f, 1f, 1, 0, 1f) } catch (_: Throwable) { } }
}

/** Stage picture (fitted to the screen width) + the strip above it + the gold title on the empty sign. */
@Composable
fun DcStage(geo: WhGeo, title: String, subtitle: String, u: Dp) {
    val stageImg = ImageBitmap.imageResource(R.drawable.dc_stage)
    if (geo.top > 0.dp) Canvas(Modifier.fillMaxWidth().height(geo.top + 2.dp)) {
        drawImage(stageImg, srcOffset = androidx.compose.ui.unit.IntOffset(0, 0), srcSize = androidx.compose.ui.unit.IntSize(stageImg.width, 3),
            dstOffset = androidx.compose.ui.unit.IntOffset(geo.left.toPx().toInt(), 0), dstSize = androidx.compose.ui.unit.IntSize(geo.sceneW.toPx().toInt() + 1, size.height.toInt() + 1))
    }
    Box(Modifier.offset(x = geo.left, y = geo.top).wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(geo.sceneW, geo.sceneH)) {
        Image(painterResource(R.drawable.dc_stage), null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
    }
    Column(
        Modifier.offset(x = geo.x(184f), y = geo.y(298f)).size(geo.k * 400f, geo.k * 195f),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text(title, maxLines = 1, softWrap = false, style = TextStyle(
            fontSize = (u * 7.6f).sp(), fontWeight = FontWeight.Black,
            brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFFFF0A0), Color(0xFFFFC21E), Color(0xFFF08A00))),
            shadow = Shadow(Color(0xFF5A3200), Offset(0f, 5f), 2f)
        ))
        Spacer(Modifier.height(u * 0.6f))
        Text(subtitle, fontSize = (u * 3f).sp(), fontWeight = FontWeight.Black, color = Color(0xFF3A1A00), maxLines = 1,
            modifier = Modifier.clip(RoundedCornerShape(u * 3)).background(Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFE0A82A)))).padding(horizontal = u * 2.6f))
    }
}

private class DcToss(val x0: Float, val sx: Float, val sy: Float, val rz: Float)

/**
 * Two real 3D dice (the same AdDice3D as Ludo) on the table.
 * While [rolling] they tumble high in the air; when rolling ends they fall, bounce and land showing [v1] / [v2].
 * [onLanded] is called when they are still.
 */
@Composable
fun DcDiceTable(geo: WhGeo, u: Dp, v1: Int, v2: Int, rolling: Boolean, onLanded: () -> Unit) {
    var phase by remember { mutableStateOf(0) }
    var t by remember { mutableStateOf(0f) }
    var shown by remember { mutableStateOf(v1 to v2) }
    var h0 by remember { mutableStateOf(0f) }
    val toss = remember { mutableStateOf(listOf(DcToss(-5f, 1f, 1f, 1f), DcToss(5f, -1f, 1f, -1f))) }
    LaunchedEffect(rolling) {
        if (rolling) {
            val r = { if (Random.nextBoolean()) 1f else -1f }
            toss.value = listOf(DcToss(-5f, r(), r(), r()), DcToss(5f, r(), r(), r()))
            shown = v1 to v2
            phase = 1
            val s = withFrameNanos { it }
            while (true) withFrameNanos { t = (it - s) / 1e9f }
        } else if (phase == 1) {
            h0 = 22f + 3f * sin(t * 9f)
            phase = 2; t = 0f
            val s = withFrameNanos { it }
            while (t < 0.75f) withFrameNanos { t = ((it - s) / 1e9f).coerceAtMost(0.75f) }
            phase = 0; onLanded()
        }
    }
    val centres = listOf(307f, 461f)
    val dieSize = geo.k * 138f
    centres.forEachIndexed { k, cx ->
        val p = toss.value[k]
        var h = 0f; var spx = 0f; var spy = 0f; var rz = 0f; var x = 0f; var sq = 1f
        var value = if (phase == 1) (if (k == 0) shown.first else shown.second) else (if (k == 0) v1 else v2)
        when (phase) {
            1 -> {
                val up = (t / 0.25f).coerceAtMost(1f)
                h = 22f * (1f - (1f - up).pow(2)) + 3f * sin(t * 9f) * up
                spx = p.sx * t * 900f; spy = p.sy * t * 720f
                rz = p.rz * 25f * sin(t * 13f); x = p.x0 * up
            }
            2 -> {
                val uu = t / 0.75f
                value = if (k == 0) v1 else v2
                h = when {
                    uu < 0.35f -> h0 * (1f - (uu / 0.35f).pow(2))
                    uu < 0.62f -> { val q = (uu - 0.35f) / 0.27f; 8f * 4f * q * (1f - q) }
                    uu < 0.82f -> { val q = (uu - 0.62f) / 0.2f; 3f * 4f * q * (1f - q) }
                    else -> { val q = (uu - 0.82f) / 0.18f; 0.9f * 4f * q * (1f - q) }
                }
                val e = 1f - (1f - (uu / 0.85f).coerceAtMost(1f)).pow(3)
                spx = p.sx * 720f * (1f - e); spy = p.sy * 720f * (1f - e)
                rz = p.rz * 25f * (1f - uu).pow(2) * sin(uu * 14f); x = p.x0 * (1f - uu)
                if (h < 0.5f && uu > 0.3f && uu < 0.95f) sq = 1f - (0.5f - h) * 0.14f
            }
        }
        // shadow on the table
        Box(Modifier.offset(x = geo.x(cx) - geo.k * 50f + u * x, y = geo.y(901f) - geo.k * 17f)
            .size(geo.k * 100f, geo.k * 35f)
            .graphicsLayer { val s = 1f - h / 40f; scaleX = s; scaleY = s; alpha = (1f - h / 34f).coerceIn(0f, 1f) }
            .background(Brush.radialGradient(listOf(Color(0x8C000000), Color.Transparent)), CircleShape))
        AdDice3D(
            value = value, spinX = spx, spinY = spy, color = DcRed,
            modifier = Modifier.offset(x = geo.x(cx) - dieSize / 2 + u * x, y = geo.y(809f) - dieSize / 2 - u * h)
                .size(dieSize)
                .graphicsLayer { rotationZ = rz; scaleX = 1f / sq; scaleY = sq; transformOrigin = TransformOrigin(0.5f, 0.85f) }
                .testTag("dice_$k")
        )
    }
}

/** Gold tag above the dice with the last result (e.g. "ফলাফল: ৯ (বেজোড়)"). */
@Composable
fun DcResultTag(geo: WhGeo, u: Dp, text: String) {
    Box(Modifier.offset(y = geo.y(694f)).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Text(text, fontSize = (u * 4.2f).sp(), fontWeight = FontWeight.Black, color = Color(0xFF3A1A00), maxLines = 1,
            modifier = Modifier.shadow(u * 2, RoundedCornerShape(u * 4), ambientColor = Color(0xFFFFC85A), spotColor = Color(0xFFFFC85A))
                .clip(RoundedCornerShape(u * 4)).background(Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC21E))))
                .padding(horizontal = u * 3.4f, vertical = u * 0.6f).testTag("dice_result_tag"))
    }
}

/** Even / Odd card: red velvet, two small 3D dice, name, gold-rimmed 1.9x medal. */
@Composable
fun DcChoiceCard(u: Dp, label: String, medal: String, dice: Pair<Int, Int>, selected: Boolean, enabled: Boolean, tag: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(u * 4)
    Box(modifier.offset(y = if (selected) -u else 0.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = u * 2.4f, end = u * 1.6f)
                .then(if (selected) Modifier.shadow(u * 3, shape, ambientColor = Color(0xFFFFC85A), spotColor = Color(0xFFFFC85A)) else Modifier)
                .clip(shape)
                .background(Brush.verticalGradient(if (selected) listOf(Color(0xFFC21A38), Color(0xFF6A0A1C)) else listOf(Color(0xFF7A0E22), Color(0xFF3E0612))))
                .border(u * 0.5f, if (selected) Color(0xFFFFE27A) else Color(0x8CE8B84A), shape)
                .clickable(enabled = enabled, onClick = onClick).padding(horizontal = u * 1.4f, vertical = u * 1.2f).testTag(tag),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdDice3D(dice.first, -12f, 18f, DcRed, Modifier.size(u * 6.6f))
            AdDice3D(dice.second, -12f, 18f, DcRed, Modifier.size(u * 6.6f).offset(x = -u * 1.2f))
            Text(label, fontSize = (u * 4.2f).sp(), fontWeight = FontWeight.Black, color = Color(0xFFFFE7A0), maxLines = 1, softWrap = false,
                style = TextStyle(shadow = Shadow(Color(0xFF2A0008), Offset(0f, 3f), 1f)), modifier = Modifier.padding(end = u * 5))
        }
        Box(Modifier.align(Alignment.TopEnd).size(u * 8).clip(CircleShape).background(Color(0xFFFFD45A)).padding(u * 0.5f).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF3A5BE8), Color(0xFF121A61)))), contentAlignment = Alignment.Center) {
            Text(medal, fontSize = (u * 2.7f).sp(), fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
        }
        if (selected) Box(Modifier.offset(x = -u * 1.4f, y = u * 0.4f).size(u * 6).clip(CircleShape).background(Color(0xFF4ADE80)), contentAlignment = Alignment.Center) {
            Text("✓", fontSize = (u * 3.6f).sp(), fontWeight = FontWeight.Black, color = Color(0xFF04210F))
        }
    }
}

/** Host in the maroon dress (Android 9+ animated, older phones show the first frame). */
@Composable
fun DcHost(geo: WhGeo, clap: Boolean) {
    val w = geo.k * 384f
    Box(Modifier.offset(x = geo.x(506f), y = geo.y(563f)).size(w, w * (737f / 400f))) {
        AndroidView(
            factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
            update = { v ->
                val res = if (clap) R.raw.dc_host_clap else R.raw.dc_host_idle
                if (v.tag != res) {
                    v.tag = res
                    if (Build.VERSION.SDK_INT >= 28) {
                        try {
                            val d = ImageDecoder.decodeDrawable(ImageDecoder.createSource(v.resources, res))
                            if (d is AnimatedImageDrawable) {
                                d.repeatCount = if (clap) 0 else AnimatedImageDrawable.REPEAT_INFINITE
                                v.setImageDrawable(d); d.start()
                            } else v.setImageDrawable(d)
                        } catch (_: Throwable) { }
                    } else {
                        try { v.setImageBitmap(BitmapFactory.decodeStream(v.resources.openRawResource(res))) } catch (_: Throwable) { }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
