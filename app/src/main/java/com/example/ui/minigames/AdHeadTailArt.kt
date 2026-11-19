package com.example.ui.minigames

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.animation.core.*
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

// Head / Tail game artwork. VISUAL ONLY. Positions are in the stage picture's pixels (768 x 1376).

private fun Dp.sp(): TextUnit = TextUnit(this.value, TextUnitType.Sp)

/** Stage (fitted to width) + strip above it + gold title on the empty sign. */
@Composable
fun HtStage(geo: WhGeo, title: String, subtitle: String, u: Dp) {
    val img = ImageBitmap.imageResource(R.drawable.ht_stage)
    if (geo.top > 0.dp) Canvas(Modifier.fillMaxWidth().height(geo.top + 2.dp)) {
        drawImage(img, srcOffset = IntOffset(0, 0), srcSize = IntSize(img.width, 3),
            dstOffset = IntOffset(geo.left.toPx().toInt(), 0), dstSize = IntSize(geo.sceneW.toPx().toInt() + 1, size.height.toInt() + 1))
    }
    Box(Modifier.offset(x = geo.left, y = geo.top).wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(geo.sceneW, geo.sceneH)) {
        Image(painterResource(R.drawable.ht_stage), null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
    }
    Column(
        Modifier.offset(x = geo.x(138f), y = geo.y(210f)).size(geo.k * 492f, geo.k * 179f),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        com.example.ui.components.AdGoldText(title, fontSize = (u * 7.6f).sp())
        Spacer(Modifier.height(u * 0.6f))
        Text(subtitle, fontSize = (u * 3f).sp(), fontWeight = FontWeight.Black, color = Color(0xFF3A1A00), maxLines = 1,
            modifier = Modifier.clip(RoundedCornerShape(u * 3)).background(Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFE0A82A)))).padding(horizontal = u * 2.6f))
    }
}

/** Blue enamel ring with gold edges, dark blue backdrop and 16 twinkling jewels, behind the coin. */
@Composable
fun HtRing(geo: WhGeo) {
    val inf = rememberInfiniteTransition(label = "htRing")
    val glow by inf.animateFloat(0.45f, 1f, infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "htGlow")
    val tw by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "htTw")
    val d = geo.k * 399f
    Canvas(Modifier.offset(x = geo.x(184f), y = geo.y(506f)).size(d)) {
        val r = size.width / 2
        drawCircle(Brush.radialGradient(0.6f to Color.Transparent, 0.66f to Color(0xFFFFD678).copy(alpha = 0.45f * glow), 0.74f to Color.Transparent, center = center, radius = r), r * (0.98f + 0.04f * glow))
        drawCircle(Brush.radialGradient(listOf(Color(0xFF1D3FA8), Color(0xFF0E1F6A), Color(0xFF060C33)), center = center + Offset(0f, -r * 0.1f), radius = r * 0.74f), r * 0.74f)
        val ro = r * 0.84f
        drawCircle(Brush.radialGradient(0f to Color(0xFF0B2A8A), 0.5f to Color(0xFF1E4ED8), 1f to Color(0xFF0B2A8A), center = center, radius = ro), ro - r * 0.06f, style = Stroke(r * 0.1f))
        drawCircle(Color(0xFFE0A82A), ro - r * 0.115f, style = Stroke(r * 0.02f))
        drawCircle(Color(0xFFFFE7A0), ro - r * 0.005f, style = Stroke(r * 0.022f))
        drawCircle(Color(0xFF8A5A08), ro + r * 0.012f, style = Stroke(r * 0.01f))
        val cols = listOf(Color(0xFFFFF3C4), Color(0xFFE8364A), Color(0xFFFFF3C4), Color(0xFF22C55E))
        for (k in 0 until 16) {
            val a = k * 22.5f * PI.toFloat() / 180f
            val p = center + Offset(cos(a), sin(a)) * (ro - r * 0.06f)
            val ph = (tw + k / 16f) % 1f
            val lit = 0.55f + 0.45f * sin(ph * 2f * PI.toFloat())
            drawCircle(Color.White.copy(alpha = 0.35f * lit), r * 0.06f, p)
            drawCircle(Brush.radialGradient(listOf(Color.White, cols[k % 4], Color(0xFF5A3A04)), center = p - Offset(r * 0.012f, r * 0.012f), radius = r * 0.045f), r * 0.034f, p)
        }
    }
}

/**
 * The Bangladeshi coin. While [tossing] it flies up and flips; when tossing ends it falls,
 * bounces and lands showing [side] (true = head). [onLanded] is called when it is still.
 */
@Composable
fun HtCoin(geo: WhGeo, head: Boolean, tossing: Boolean, onLanded: () -> Unit) {
    val headImg = ImageBitmap.imageResource(R.drawable.ht_head)
    val tailImg = ImageBitmap.imageResource(R.drawable.ht_tail)
    var phase by remember { mutableStateOf(0) }
    var t by remember { mutableStateOf(0f) }
    var angle by remember { mutableStateOf(if (head) 0f else 180f) }
    var h by remember { mutableStateOf(0f) }
    LaunchedEffect(tossing) {
        if (tossing) {
            phase = 1
            val a0 = angle
            val s = withFrameNanos { it }
            while (true) withFrameNanos {
                t = (it - s) / 1e9f
                val up = (t / 0.35f).coerceAtMost(1f)
                h = 17f * (1f - (1f - up).pow(2)) + 2f * sin(t * 8f) * up
                angle = a0 + t * 1500f
            }
        } else if (phase == 1) {
            phase = 2
            val a0 = angle; val h0 = h
            val tgt = if (head) 0f else 180f
            val end = ceil((a0 + 720f - tgt) / 360f) * 360f + tgt
            val s = withFrameNanos { it }
            var uu = 0f
            while (uu < 1f) withFrameNanos {
                uu = ((it - s) / 0.8e9f).coerceAtMost(1f)
                h = when {
                    uu < 0.45f -> h0 * (1f - (uu / 0.45f).pow(2))
                    uu < 0.75f -> { val q = (uu - 0.45f) / 0.3f; 6f * 4f * q * (1f - q) }
                    else -> { val q = (uu - 0.75f) / 0.25f; 1.5f * 4f * q * (1f - q) }
                }
                val e = 1f - (1f - (uu / 0.8f).coerceAtMost(1f)).pow(3)
                angle = a0 + (end - a0) * e
            }
            phase = 0; onLanded()
        }
    }
    LaunchedEffect(head) { if (phase == 0) angle = if (head) 0f else 180f }
    val u = geo.screenW / 100
    // shadow on the pedestal
    Box(Modifier.offset(x = geo.x(284f), y = geo.y(917f)).size(geo.k * 200f, geo.k * 41f)
        .graphicsLayer { val s = 1f - h / 50f; scaleX = s; scaleY = s; alpha = (1f - h / 45f).coerceIn(0f, 1f) }
        .background(Brush.radialGradient(listOf(Color(0x99000000), Color.Transparent)), CircleShape))
    Canvas(Modifier.offset(x = geo.x(246f), y = geo.y(558f) - u * h).size(geo.k * 276f).testTag("headtail_coin")) {
        val r = size.width * 128f / 300f
        val c = center
        val rad = angle * PI.toFloat() / 180f
        val co = cos(rad)
        val sy = abs(co).coerceAtLeast(0.02f)
        val th = r * 0.1f * abs(sin(rad))
        val dir = if (co >= 0f) 1f else -1f
        var i = ceil(th).toInt()
        while (i > 0) {
            val dy = i * dir * 0.9f
            drawOval(if (i % 3 != 0) Color(0xFF9A6A10) else Color(0xFFC99A30), topLeft = Offset(c.x - r, c.y + dy - r * sy), size = Size(r * 2, r * 2 * sy))
            i--
        }
        val img = if (co >= 0f) headImg else tailImg
        withTransform({ scale(1f, sy, c) }) {
            drawImage(img, dstOffset = IntOffset((c.x - r).toInt(), (c.y - r).toInt()), dstSize = IntSize((r * 2).toInt(), (r * 2).toInt()))
            drawCircle(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f * (1f - sy)), Color.Black.copy(alpha = 0.35f * (1f - sy))), c - Offset(r, r), c + Offset(r, r)), r, c)
        }
    }
}

/** Gold tag above the ring with the last result (e.g. "ফলাফল: হেড"). */
@Composable
fun HtResultTag(geo: WhGeo, u: Dp, text: String) {
    Box(Modifier.offset(y = geo.y(470f)).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Text(text, fontSize = (u * 4.2f).sp(), fontWeight = FontWeight.Black, color = Color(0xFF3A1A00), maxLines = 1,
            modifier = Modifier.shadow(u * 2, RoundedCornerShape(u * 4), ambientColor = Color(0xFFFFC85A), spotColor = Color(0xFFFFC85A))
                .clip(RoundedCornerShape(u * 4)).background(Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC21E))))
                .padding(horizontal = u * 3.4f, vertical = u * 0.6f).testTag("headtail_result_tag"))
    }
}

/** Head / Tail card: green velvet, coin face, name, 1.9x medal. */
@Composable
fun HtChoiceCard(u: Dp, label: String, medal: String, head: Boolean, selected: Boolean, enabled: Boolean, tag: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(u * 4)
    Box(modifier.offset(y = if (selected) -u else 0.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = u * 2.4f, end = u * 1.6f)
                .then(if (selected) Modifier.shadow(u * 3, shape, ambientColor = Color(0xFFFFC85A), spotColor = Color(0xFFFFC85A)) else Modifier)
                .clip(shape)
                .background(Brush.verticalGradient(if (selected) listOf(Color(0xFF15A35E), Color(0xFF0A5A33)) else listOf(Color(0xFF0E6A3E), Color(0xFF063A22))))
                .border(u * 0.5f, if (selected) Color(0xFFFFE27A) else Color(0x8CE8B84A), shape)
                .clickable(enabled = enabled, onClick = onClick).padding(horizontal = u * 1.6f, vertical = u * 1.4f).testTag(tag),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(if (head) R.drawable.ht_head else R.drawable.ht_tail), null, Modifier.size(u * 7.4f).shadow(u, CircleShape))
            Spacer(Modifier.width(u * 2))
            Text(label, fontSize = (u * 4.6f).sp(), fontWeight = FontWeight.Black, color = Color(0xFFFFE7A0), maxLines = 1, softWrap = false,
                style = TextStyle(shadow = Shadow(Color(0xFF02200F), Offset(0f, 3f), 1f)), modifier = Modifier.padding(end = u * 5))
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

/** Host in the green dress (Android 9+ animated, older phones show the first frame). */
@Composable
fun HtHost(geo: WhGeo, clap: Boolean) {
    val w = geo.k * 384f
    Box(Modifier.offset(x = geo.x(514f), y = geo.y(553f)).size(w, w * (737f / 400f))) {
        AndroidView(
            factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
            update = { v ->
                val res = if (clap) R.raw.ht_host_clap else R.raw.ht_host_idle
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
