package com.example.ui.autoludo

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp

@Composable
fun AdLudoPawnToken(
    pawnColor: Color,
    isEligible: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) { drawAdPawn(pawnColor, isEligible) }
}

private fun DrawScope.drawAdPawn(color: Color, eligible: Boolean) {
    val s = size.width / 18.5f
    val x = size.width / 2f
    val footY = size.height
    val y = footY - 7.6f * s
    val light = lerp(color, Color.White, 0.55f)
    val dark = lerp(color, Color.Black, 0.5f)
    fun ballBrush(cx: Float, cy: Float, r: Float) =
        Brush.radialGradient(listOf(light, color, dark), center = Offset(cx, cy), radius = r)
    fun oval(cx: Float, cy: Float, rx: Float, ry: Float) = Rect(cx - rx, cy - ry, cx + rx, cy + ry)

    if (eligible) {
        val g = oval(x, footY, 11.5f * s, 4.8f * s)
        drawOval(
            Brush.radialGradient(listOf(Color(0xBFFFD740), Color(0x00FFAA00)), center = g.center, radius = 11.5f * s),
            g.topLeft, g.size
        )
        val ring = oval(x, footY, 9.8f * s, 4.1f * s)
        drawOval(Color(0xFFFFC107), ring.topLeft, ring.size, style = Stroke(width = 1.4f * s))
    }
    val sh = oval(x, y + 10.5f * s, 9.6f * s, 3.7f * s)
    drawOval(
        Brush.radialGradient(listOf(Color(0x73000000), Color(0x00000000)), center = sh.center, radius = 9.6f * s),
        sh.topLeft, sh.size
    )
    val b1 = oval(x, y + 8.6f * s, 8.8f * s, 4.1f * s)
    drawOval(ballBrush(x - 3f * s, y + 6f * s, 11f * s), b1.topLeft, b1.size)
    drawOval(Color(0x73000000), b1.topLeft, b1.size, style = Stroke(width = 0.7f * s))
    val b2 = oval(x, y + 7.6f * s, 8.8f * s, 3.9f * s)
    drawOval(ballBrush(x - 3f * s, y + 5f * s, 11f * s), b2.topLeft, b2.size)
    drawOval(Color(0x59000000), b2.topLeft, b2.size, style = Stroke(width = 0.6f * s))
    val dash = oval(x, y + 7.6f * s, 6.8f * s, 2.85f * s)
    drawOval(
        Color.White, dash.topLeft, dash.size,
        style = Stroke(width = 1.3f * s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.2f * s, 1.6f * s)))
    )
    val hole = oval(x, y + 7.6f * s, 4.2f * s, 1.8f * s)
    drawOval(Color(0x40000000), hole.topLeft, hole.size)
    val cy = y - 10.2f * s
    val body = Path().apply {
        moveTo(x - 8.4f * s, y - 7.6f * s)
        arcTo(Rect(Offset(x, cy), 8.8f * s), 162.7f, 214.6f, false)
        lineTo(x, y + 7.4f * s)
        close()
    }
    drawPath(
        body,
        Brush.horizontalGradient(
            0f to Color(0xFF9E9E9E), 0.35f to Color.White, 1f to Color(0xFFBDBDBD),
            startX = x - 8.8f * s, endX = x + 8.8f * s
        )
    )
    drawPath(body, Color(0x80000000), style = Stroke(width = 0.7f * s, join = StrokeJoin.Round))
    val hc = Offset(x, y - 9.2f * s)
    drawCircle(ballBrush(x - 2.6f * s, y - 11.4f * s, 10.5f * s), 7.5f * s, hc)
    drawCircle(Color.White, 7.5f * s, hc, style = Stroke(width = 1.6f * s))
    drawCircle(Color(0x59000000), 8.4f * s, hc, style = Stroke(width = 0.5f * s))
    val hl = oval(x - 2.7f * s, y - 12.2f * s, 2.7f * s, 1.7f * s)
    drawOval(Color(0xE6FFFFFF), hl.topLeft, hl.size)
    val hl2 = oval(x + 3f * s, y - 6.2f * s, 1.8f * s, 0.9f * s)
    drawOval(Color(0x59FFFFFF), hl2.topLeft, hl2.size)
}
