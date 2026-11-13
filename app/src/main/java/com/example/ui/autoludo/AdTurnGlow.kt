package com.example.ui.autoludo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

@Composable
fun AdTurnGlow(isPlayer1Turn: Boolean, palette: AdPalette, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "turnGlow")
    val t by inf.animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t")
    val yellow = if (isPlayer1Turn) palette.p1IsYellow else palette.p2IsYellow
    Canvas(modifier = modifier) { drawTurnGlow(isPlayer1Turn, yellow, t) }
}

private fun DrawScope.drawTurnGlow(isPlayer1Turn: Boolean, yellow: Boolean, t: Float) {
    val u = size.width / 15f
    val glow = if (yellow) Color(0xFFFF6D00) else Color(0xFFFFC400)
    val ring = if (yellow) Color(0xFFFF8F00) else Color(0xFFFFD54F)
    val bulbEdge = if (yellow) Color(0xFFE65100) else Color(0xFFC98A00)
    val bright = if (yellow) 0.08f else 0.10f

    val bl = 0f to 9f; val tl = 0f to 0f; val tr = 9f to 0f; val br = 9f to 9f
    val active = if (isPlayer1Turn) bl else tr

    for (h in listOf(bl, tl, tr, br)) {
        if (h == active) continue
        drawRect(Color(0x380A0A28), topLeft = Offset(h.first * u, h.second * u), size = Size(6 * u, 6 * u))
    }

    val x0 = active.first * u; val y0 = active.second * u; val w = 6 * u
    val pulse = 0.5f + 0.5f * sin(t * 2f * PI.toFloat())

    clipRect(left = x0, top = y0, right = x0 + w, bottom = y0 + w) {
        drawRect(Color.White.copy(alpha = bright * (0.35f + 0.65f * pulse)), topLeft = Offset(x0, y0), size = Size(w, w))
        val ph = (t * 0.6f) % 1.4f
        val shift = (ph - 0.2f) * 190f / 132f * w
        drawRect(
            Brush.linearGradient(
                0f to Color.Transparent, 0.45f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.55f), 0.55f to Color.Transparent, 1f to Color.Transparent,
                start = Offset(x0 - w + shift, y0 - w + shift), end = Offset(x0 + w + shift, y0 + w + shift)
            ),
            topLeft = Offset(x0, y0), size = Size(w, w)
        )
        val ringR = 13f / 22f * u * (1f + 0.18f * pulse)
        for ((a, b) in listOf(2f to 2f, 4f to 2f, 2f to 4f, 4f to 4f)) {
            drawCircle(ring.copy(alpha = 0.5f + 0.5f * pulse), ringR, Offset(x0 + a * u, y0 + b * u), style = Stroke(width = 2.8f / 22f * u))
        }
        val bx = x0 + u - 2f / 22f * u; val by = y0 + u - 2f / 22f * u; val bw = 4 * u + 4f / 22f * u
        for ((wd, al) in listOf(6f to 0.18f, 3f to 0.55f)) {
            drawRoundRect(ring.copy(alpha = al), topLeft = Offset(bx, by), size = Size(bw, bw), cornerRadius = CornerRadius(3f / 22f * u), style = Stroke(width = wd / 22f * u))
        }
    }

    val ins = 4f / 22f * u
    val gx = x0 + ins; val gy = y0 + ins; val gw = w - 2 * ins
    val cr = CornerRadius(8f / 22f * u)
    for ((wd, al) in listOf(16f to 0.14f, 11f to 0.24f, 7f to 0.40f)) {
        drawRoundRect(glow.copy(alpha = al), topLeft = Offset(gx, gy), size = Size(gw, gw), cornerRadius = cr, style = Stroke(width = wd / 22f * u))
    }
    drawRoundRect(Color.White, topLeft = Offset(gx, gy), size = Size(gw, gw), cornerRadius = cr, style = Stroke(width = 3.2f / 22f * u))

    val n = 28
    val step = floor(t * 8f).toInt()
    for (i in 0 until n) {
        val f = ((i.toFloat() / n + t * 0.12f) % 1f + 1f) % 1f
        val d = f * 4f * gw
        val p = when {
            d < gw -> Offset(gx + d, gy)
            d < 2 * gw -> Offset(gx + gw, gy + d - gw)
            d < 3 * gw -> Offset(gx + gw - (d - 2 * gw), gy + gw)
            else -> Offset(gx, gy + gw - (d - 3 * gw))
        }
        val a = if ((i + step) % 2 == 0) 1f else 0.35f
        val r = 2.6f / 22f * u
        drawCircle(Color.White.copy(alpha = a), r, p)
        drawCircle(bulbEdge.copy(alpha = a), r, p, style = Stroke(width = 1f / 22f * u))
    }
}
