package com.example.ui.autoludo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private val AdGreen = Color(0xFF22A84A)
private val AdYellow = Color(0xFFFFD91A)
private val AdRed = Color(0xFFE0201C)
private val AdBlue = Color(0xFF29A9F0)
private val AdGrid = Color(0xFF9E9E9E)

@Composable
fun AdLudoBoard(boardRotation: Float, modifier: Modifier = Modifier, palette: AdPalette = AdDefaultPalette) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) { drawAdLudoBoard(palette) }
        AdLudoCenterLogo(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(2.72f / 15f)
                .graphicsLayer { rotationZ = -boardRotation }
        )
    }
}

private fun DrawScope.drawAdLudoBoard(p: AdPalette) {
    val u = size.width / 15f
    drawRect(Color.White)
    fun cell(r: Int, c: Int, color: Color = Color.White) {
        val tl = Offset(c * u, r * u)
        drawRect(color, tl, Size(u, u))
        drawRect(AdGrid, tl, Size(u, u), style = Stroke(width = 1f))
    }
    for (r in 0..14) for (c in 0..14) {
        val arm = c in 6..8 || r in 6..8
        val center = r in 6..8 && c in 6..8
        if (arm && !center) cell(r, c)
    }
    for (i in 1..5) {
        cell(i, 7, p.tr); cell(7, i, p.tl); cell(14 - i, 7, p.bl); cell(7, 14 - i, p.br)
    }
    cell(6, 1, p.tl); cell(1, 8, p.tr); cell(8, 13, p.br); cell(13, 6, p.bl)
    fun home(r: Int, c: Int, color: Color) {
        drawRect(color, Offset(c * u, r * u), Size(6 * u, 6 * u))
        drawRect(Color.White, Offset((c + 1) * u, (r + 1) * u), Size(4 * u, 4 * u))
        drawRect(Color.Black.copy(alpha = 0.15f), Offset((c + 1) * u, (r + 1) * u), Size(4 * u, 4 * u), style = Stroke(width = 1f))
        listOf(2f to 2f, 4f to 2f, 2f to 4f, 4f to 4f).forEach { (dx, dy) ->
            drawCircle(color, radius = 0.5f * u, center = Offset((c + dx) * u, (r + dy) * u))
        }
    }
    home(0, 0, p.tl); home(0, 9, p.tr); home(9, 0, p.bl); home(9, 9, p.br)
    val a = 6 * u; val b = 9 * u; val m = 7.5f * u
    fun tri(p1: Offset, p2: Offset, color: Color) {
        drawPath(Path().apply { moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(m, m); close() }, color)
    }
    tri(Offset(a, a), Offset(b, a), p.tr)
    tri(Offset(a, a), Offset(a, b), p.tl)
    tri(Offset(a, b), Offset(b, b), p.bl)
    tri(Offset(b, a), Offset(b, b), p.br)
    fun star(r: Int, c: Int) {
        val cx = c * u + u / 2f; val cy = r * u + u / 2f
        val p = Path()
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) 0.36f * u else 0.155f * u
            val an = -Math.PI / 2 + i * Math.PI / 5
            val x = cx + rad * cos(an).toFloat(); val y = cy + rad * sin(an).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        drawPath(p, Color.White)
        drawPath(p, Color(0xFF757575), style = Stroke(width = 1.2.dp.toPx(), join = StrokeJoin.Round))
    }
    star(8, 2); star(2, 6); star(6, 12); star(12, 8)
    fun arrow(r: Int, c: Int, dx: Float, dy: Float, color: Color) {
        val cx = c * u + u / 2f; val cy = r * u + u / 2f
        val len = 0.27f * u; val h = 0.18f * u; val w = 1.8.dp.toPx()
        val tip = Offset(cx + dx * len, cy + dy * len)
        drawLine(color, Offset(cx - dx * len, cy - dy * len), tip, w, StrokeCap.Round)
        drawLine(color, tip, Offset(tip.x - dx * h - dy * h, tip.y - dy * h - dx * h), w, StrokeCap.Round)
        drawLine(color, tip, Offset(tip.x - dx * h + dy * h, tip.y - dy * h + dx * h), w, StrokeCap.Round)
    }
    arrow(0, 7, 0f, 1f, androidx.compose.ui.graphics.lerp(p.tr, Color.Black, 0.2f))
    arrow(7, 0, 1f, 0f, p.tl)
    arrow(14, 7, 0f, -1f, p.bl)
    arrow(7, 14, -1f, 0f, p.br)
}

@Composable
private fun AdLudoCenterLogo(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        val c = center
        val k = r / 30f
        fun o(x: Float, y: Float) = Offset(c.x + x * k, c.y + y * k)
        val gold = Brush.linearGradient(
            listOf(Color(0xFFFFF3B0), Color(0xFFFFC107), Color(0xFFB07A00)),
            start = Offset(c.x - r, c.y - r), end = Offset(c.x + r, c.y + r)
        )
        val dark = Color(0xFF7A5200)
        drawCircle(Color.Black.copy(alpha = 0.25f), r, c + Offset(0f, 1.5.dp.toPx()))
        drawCircle(brush = gold, radius = r, center = c)
        drawCircle(Color(0xFF0B1640).copy(alpha = 0.88f), r * 0.927f, c)
        drawCircle(brush = gold, radius = r * 0.653f, center = c)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF2E4BB8), Color(0xFF0B1640)),
                center = Offset(c.x - r * 0.15f, c.y - r * 0.2f), radius = r * 0.7f
            ),
            radius = r * 0.607f, center = c
        )
        val ar = r * 0.787f
        val textPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#FFD54F")
            textSize = 6.6f * k
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.LEFT
            letterSpacing = 0.05f
        }
        val arc = android.graphics.Path().apply {
            addArc(android.graphics.RectF(c.x - ar, c.y - ar, c.x + ar, c.y + ar), 155f, 230f)
        }
        val arcLen = android.graphics.PathMeasure(arc, false).length
        drawIntoCanvas {
            val label = "AD TOURNAMENT"
            val start = ((arcLen - textPaint.measureText(label)) / 2f).coerceAtLeast(0f)
            it.nativeCanvas.drawTextOnPath(label, arc, start, textPaint.textSize * 0.35f, textPaint)
        }
        listOf(59.0, 90.0, 121.0).forEach { deg ->
            val an = Math.toRadians(deg)
            val sx = c.x + ar * cos(an).toFloat(); val sy = c.y + ar * sin(an).toFloat()
            val p = Path()
            for (i in 0 until 10) {
                val rad = if (i % 2 == 0) 3f * k else 1.3f * k
                val a2 = -Math.PI / 2 + i * Math.PI / 5
                val x = sx + rad * cos(a2).toFloat(); val y = sy + rad * sin(a2).toFloat()
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            p.close()
            drawPath(p, Color(0xFFFFD54F))
        }
        val cup = Path().apply {
            moveTo(o(-7.5f, -11f).x, o(-7.5f, -11f).y)
            lineTo(o(7.5f, -11f).x, o(7.5f, -11f).y)
            lineTo(o(6.6f, -4.5f).x, o(6.6f, -4.5f).y)
            quadraticBezierTo(o(0f, 1.5f).x, o(0f, 1.5f).y, o(-6.6f, -4.5f).x, o(-6.6f, -4.5f).y)
            close()
        }
        drawPath(cup, gold)
        drawPath(cup, dark, style = Stroke(width = 0.6f * k))
        val leftHandle = Path().apply {
            moveTo(o(-7.5f, -9.6f).x, o(-7.5f, -9.6f).y)
            quadraticBezierTo(o(-11.5f, -9.2f).x, o(-11.5f, -9.2f).y, o(-9.8f, -5.6f).x, o(-9.8f, -5.6f).y)
            quadraticBezierTo(o(-8.4f, -3.8f).x, o(-8.4f, -3.8f).y, o(-6.6f, -4.5f).x, o(-6.6f, -4.5f).y)
        }
        val rightHandle = Path().apply {
            moveTo(o(7.5f, -9.6f).x, o(7.5f, -9.6f).y)
            quadraticBezierTo(o(11.5f, -9.2f).x, o(11.5f, -9.2f).y, o(9.8f, -5.6f).x, o(9.8f, -5.6f).y)
            quadraticBezierTo(o(8.4f, -3.8f).x, o(8.4f, -3.8f).y, o(6.6f, -4.5f).x, o(6.6f, -4.5f).y)
        }
        drawPath(leftHandle, gold, style = Stroke(width = 1.5f * k))
        drawPath(rightHandle, gold, style = Stroke(width = 1.5f * k))
        drawRect(gold, o(-1.5f, -0.5f), Size(3f * k, 3.5f * k))
        drawRoundRect(gold, o(-5.5f, 2.8f), Size(11f * k, 2.8f * k), CornerRadius(1.1f * k))
        drawOval(Color.White.copy(alpha = 0.9f), o(-5f, -9.7f), Size(3.6f * k, 2.2f * k))
        val ludoPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 6.5f * k
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
            letterSpacing = 0.1f
        }
        drawIntoCanvas { it.nativeCanvas.drawText("LUDO", c.x, c.y + 13.2f * k, ludoPaint) }
    }
}
