package com.example.ui.minigames

import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Money Wheel artwork, part 1: stage picture, blinking bulbs, the wheel, the host. VISUAL ONLY.
// All positions are in the stage picture's own pixels (768 x 1376).

const val WH_IMG_W = 768f
const val WH_IMG_H = 1376f
private const val WH_CX = 382f
private const val WH_CY = 680f
private const val WH_RING = 470.8f
private const val WH_FACE = 168.4f

private val WhBulbs = intArrayOf(324,81,444,81,318,117,451,117,288,125,479,125,316,128,456,129,199,130,259,134,508,134,287,136,372,136,493,140,539,140,230,141,199,143,569,143,508,144,169,149,599,149,151,181,617,181,650,197,119,198,110,228,658,228,107,260,660,260,110,292,658,292,715,320,118,325,650,326,153,344,616,344,647,368,121,369,170,376,599,376,201,382,567,382,64,387,89,397,679,397,65,433,703,433,51,472,717,472,751,484,17,485,14,508,755,508,597,531,48,547,720,547,754,580,48,590,720,590,48,632,720,632,48,675,720,675,48,718,720,718,48,760,720,760,24,797,48,803,720,803,501,829,494,836,283,844,48,846,720,846,296,852,720,888,48,889,16,908,753,909,333,924,433,924,360,926,394,926,298,933,464,934,424,936,381,937,423,949,339,950,378,950,392,951,536,951,507,956,292,960,470,960,335,962,426,962,380,963,278,1024,312,1025,382,1026)
private val WhSparks = intArrayOf(191,194,209,204,239,190,253,202,272,299,293,289,309,193,334,291,347,190,359,204,388,188,397,294,420,292,452,198,462,304,484,188,496,172,523,172,537,297)

/** Where the stage picture sits on screen: fills the height, centred, pushed down by [top]. */
class WhGeo(val screenW: Dp, val sceneH: Dp, val top: Dp) {
    val sceneW: Dp = sceneH * (WH_IMG_W / WH_IMG_H)
    val left: Dp = (screenW - sceneW) / 2
    val k: Dp = sceneH / WH_IMG_H
    fun x(px: Float): Dp = left + k * px
    fun y(px: Float): Dp = top + k * px
}

/** Stage picture + bulbs that blink in turns + a light sweep and sparkles on the MONEY WHEEL sign. */
@Composable
fun WhStage(geo: WhGeo, spinning: Boolean) {
    val anim = rememberInfiniteTransition(label = "whStage")
    val t by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "whBulb")
    val sweep by anim.animateFloat(-0.4f, 1.6f, infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing)), label = "whSweep")
    val tw by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "whTw")
    Box(Modifier.offset(x = geo.left, y = geo.top).wrapContentSize(androidx.compose.ui.Alignment.TopStart, unbounded = true).requiredSize(geo.sceneW, geo.sceneH)) {
        Image(painterResource(R.drawable.wh_stage), null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            val s = size.width / WH_IMG_W
            val tt = if (spinning) (t * 4f) % 1f else t
            val br = size.width * 0.013f
            for (i in 0 until WhBulbs.size / 2) {
                val c = Offset(WhBulbs[i * 2] * s, WhBulbs[i * 2 + 1] * s)
                val on = ((tt + (i % 4) * 0.25f) % 1f) < 0.5f
                val r = br * if (on) 1.25f else 0.8f
                drawCircle(
                    Brush.radialGradient(
                        0f to Color(0xFFFFFDF0).copy(alpha = if (on) 1f else 0.15f),
                        0.4f to Color(0xFFFFE28C).copy(alpha = if (on) 0.85f else 0.1f),
                        1f to Color.Transparent, center = c, radius = r * 1.6f
                    ), radius = r * 1.6f, center = c
                )
            }
            // light sweep over the sign
            val l = size.width * 0.23f; val tp = size.height * 0.116f; val w = size.width * 0.54f; val h = size.height * 0.138f
            val x = l + w * sweep
            clipRect(l, tp, l + w, tp + h) {
                rotate(18f, Offset(x, tp + h / 2)) {
                    drawRect(
                        Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0xB3FFFFFF), Color(0x00FFFFFF)), startX = x - w * 0.11f, endX = x + w * 0.11f),
                        topLeft = Offset(x - w * 0.11f, tp - h), size = Size(w * 0.22f, h * 3f)
                    )
                }
            }
            // twinkling stars on the diamond letters
            for (i in 0 until WhSparks.size / 2) {
                val ph = (tw + i * 0.37f) % 1f
                val a = sin(ph * PI).toFloat()
                if (a < 0.05f) continue
                val c = Offset(WhSparks[i * 2] * s, WhSparks[i * 2 + 1] * s)
                val r = size.width * 0.016f * a
                val p = Path().apply {
                    moveTo(c.x, c.y - r); lineTo(c.x + r * 0.16f, c.y - r * 0.16f); lineTo(c.x + r, c.y)
                    lineTo(c.x + r * 0.16f, c.y + r * 0.16f); lineTo(c.x, c.y + r); lineTo(c.x - r * 0.16f, c.y + r * 0.16f)
                    lineTo(c.x - r, c.y); lineTo(c.x - r * 0.16f, c.y - r * 0.16f); close()
                }
                drawPath(p, Color.White.copy(alpha = a))
            }
        }
    }
}

/**
 * The wheel. Slices start at angle 0 (instead of -90) so the slice the engine picks
 * stops under the TOP pointer with the SAME rotation maths as before.
 */
@Composable
fun WhWheel(geo: WhGeo, rotation: Float, labels: List<Int>, digits: (String) -> String, pegGlow: FloatArray, glowTick: Int, pointerKick: Float) {
    val ring = geo.k * WH_RING
    val face = geo.k * WH_FACE
    Box(Modifier.offset(x = geo.x(WH_CX) - ring / 2, y = geo.y(WH_CY) - ring / 2).size(ring)) {
        Canvas(Modifier.align(androidx.compose.ui.Alignment.Center).size(face * 2)) {
            glowTick.hashCode()
            val r = size.width / 2
            val sl = 360f / labels.size
            rotate(rotation) {
                labels.forEachIndexed { i, m ->
                    val cols = when {
                        m >= 25 -> listOf(Color(0xFFFFE27A), Color(0xFFC98A10))
                        m == 0 -> listOf(Color(0xFF24308F), Color(0xFF111A5E))
                        i % 2 == 1 -> listOf(Color(0xFF3550D8), Color(0xFF1E2A9A))
                        else -> listOf(Color(0xFF2A42C4), Color(0xFF16228A))
                    }
                    drawArc(Brush.radialGradient(cols, center = center, radius = r), i * sl, sl, true)
                    drawArc(Color(0xFFE8B84A), i * sl, sl, true, style = Stroke(r * 0.012f))
                }
                val paint = android.graphics.Paint().apply {
                    textSize = r * 0.12f; textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD; isAntiAlias = true
                    setShadowLayer(r * 0.03f, 0f, r * 0.01f, android.graphics.Color.argb(180, 0, 0, 0))
                }
                labels.forEachIndexed { i, m ->
                    val a = (i * sl + sl / 2) * (PI / 180).toFloat()
                    val tx = center.x + r * 0.66f * cos(a); val ty = center.y + r * 0.66f * sin(a)
                    paint.color = if (m >= 25) android.graphics.Color.rgb(58, 26, 0) else android.graphics.Color.rgb(255, 240, 168)
                    drawContext.canvas.nativeCanvas.apply {
                        save(); rotate(i * sl + sl / 2, tx, ty)
                        drawText("x" + digits(m.toString()), tx, ty + r * 0.04f, paint); restore()
                    }
                }
                // golden pegs between slices; the one that just hit the pointer glows
                for (j in labels.indices) {
                    val a = j * sl * (PI / 180).toFloat()
                    val p = Offset(center.x + r * 0.945f * cos(a), center.y + r * 0.945f * sin(a))
                    val pr = r * 0.034f
                    val g = pegGlow[j]
                    if (g > 0.02f) drawCircle(Brush.radialGradient(0f to Color(0xFFFFFAD2).copy(alpha = g), 0.35f to Color(0xFFFFD45A).copy(alpha = g * 0.7f), 1f to Color.Transparent, center = p, radius = pr * 4.5f), pr * 4.5f, p)
                    drawCircle(Color(0x99000000), pr, p + Offset(0f, r * 0.008f))
                    drawCircle(Brush.radialGradient(listOf(if (g > 0.02f) Color.White else Color(0xFFFFF6C8), Color(0xFFFFD45A), Color(0xFF8A5A08)), center = p - Offset(pr * 0.35f, pr * 0.35f), radius = pr * 1.3f), pr, p)
                    drawCircle(Color(0xFF5A3A04), pr, p, style = Stroke(r * 0.006f))
                }
            }
            drawCircle(Brush.radialGradient(listOf(Color(0x24FFFFFF), Color.Transparent), center = Offset(r * 0.7f, r * 0.6f), radius = r), r)
        }
        Image(painterResource(R.drawable.wh_ring), null, Modifier.fillMaxSize())
        Image(painterResource(R.drawable.wh_hub), null, Modifier.align(androidx.compose.ui.Alignment.Center).size(face * 0.42f))
        val pw = face * 0.26f
        Image(
            painterResource(R.drawable.wh_pointer), null,
            Modifier
                .align(androidx.compose.ui.Alignment.TopCenter)
                .offset(y = ring / 2 - face - pw * 0.72f)
                .size(pw, pw * 1.5f)
                .graphicsLayer { rotationZ = pointerKick; transformOrigin = TransformOrigin(0.5f, 0.12f) }
        )
    }
}

/** The host: real-person animation (Android 9+). Older phones show the first frame. [clap] plays the clap once. */
@Composable
fun WhHost(geo: WhGeo, clap: Boolean) {
    val w = geo.k * 375.6f
    Box(Modifier.offset(x = geo.x(458f), y = geo.y(470.7f)).size(w, w * (737f / 400f))) {
        AndroidView(
            factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
            update = { v ->
                val res = if (clap) R.raw.wh_host_clap else R.raw.wh_host_idle
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
