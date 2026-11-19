package com.example.ui.autocarrom

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// Drawing helpers for the AD Carrom loading screen (AdCarromSplash.kt). VISUAL ONLY.

internal val CS_TAU = (2 * PI).toFloat()

internal fun csGoldBrush(a: Offset, b: Offset) = Brush.linearGradient(
    0f to Color(0xFFFFF6C8), 0.25f to Color(0xFFF2C14E), 0.5f to Color(0xFF9A6410),
    0.72f to Color(0xFFF5CF62), 1f to Color(0xFFFFF0B0), start = a, end = b
)

internal class CsSpark(val x: Float, val y: Float, val r: Float, val p: Float, val k: Int)

private fun Path.csSpiral(cx: Float, cy: Float, r: Float, turns: Float, dir: Float, a0: Float) {
    for (i in 0..40) {
        val t = i / 40f; val a = a0 + dir * t * turns * CS_TAU; val rr = r * (1 - t * 0.85f)
        val x = cx + cos(a) * rr; val y = cy + sin(a) * rr
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
}

private fun Path.csLeaf(x: Float, y: Float, len: Float, ang: Float) {
    val c = cos(ang); val s = sin(ang)
    fun px(a: Float, b: Float) = x + a * c - b * s
    fun py(a: Float, b: Float) = y + a * s + b * c
    moveTo(x, y)
    quadraticBezierTo(px(len * 0.5f, -len * 0.35f), py(len * 0.5f, -len * 0.35f), px(len, 0f), py(len, 0f))
    quadraticBezierTo(px(len * 0.5f, len * 0.35f), py(len * 0.5f, len * 0.35f), x, y)
    close()
}

/** Gold filigree scroll (vine, three curls, three leaves). Local size about 100 units times [s]. */
internal fun DrawScope.csScroll(x: Float, y: Float, s: Float, rot: Float, flip: Boolean, col: Color, w: Float) {
    withTransform({
        translate(x, y)
        rotate(rot * 180f / PI.toFloat(), Offset.Zero)
        scale(if (flip) -s else s, s, Offset.Zero)
    }) {
        val p = Path().apply {
            moveTo(0f, 0f); cubicTo(30f, -5f, 55f, -30f, 60f, -58f)
            csSpiral(48f, -62f, 13f, 1.4f, 1f, 0f)
            moveTo(22f, -8f); cubicTo(30f, 8f, 48f, 14f, 62f, 8f)
            csSpiral(62f, 0f, 8f, 1.2f, -1f, PI.toFloat() / 2)
            moveTo(40f, -30f); cubicTo(22f, -40f, 16f, -56f, 24f, -70f)
            csSpiral(30f, -74f, 6f, 1.1f, 1f, PI.toFloat())
        }
        drawPath(p, col, style = Stroke(w / s, cap = StrokeCap.Round))
        val l = Path().apply { csLeaf(10f, -2f, 16f, -0.9f); csLeaf(52f, -22f, 14f, -0.2f); csLeaf(35f, 6f, 12f, 0.6f) }
        drawPath(l, col)
    }
}

internal fun DrawScope.csSparkles(list: List<CsSpark>, tw: Float, ox: Float, oy: Float, unit: Float) {
    for (s in list) {
        val a = 0.25f + 0.75f * abs(sin(tw * CS_TAU * s.k + s.p))
        val c = Offset(ox + s.x * unit, oy + s.y * unit); val r = s.r * unit * 3f
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE6A0).copy(alpha = a), Color(0x00FFC85A)), center = c, radius = r), r, c)
        if (s.r > 2.6f && a > 0.8f) {
            val l = s.r * unit * 5f; val col = Color(0xFFFFF0C8).copy(alpha = a * 0.7f)
            drawLine(col, c - Offset(l, 0f), c + Offset(l, 0f), 1f); drawLine(col, c - Offset(0f, l), c + Offset(0f, l), 1f)
        }
    }
}

/** Crest drawn in a 520-unit box: gold rings, filigree band, blue dome, ribbon, carrom board. */
internal fun DrawScope.drawCarromCrest() {
    val u = size.width / 520f
    withTransform({ scale(u, u, Offset.Zero) }) {
        val c = 260f; val o = Offset(c, c)
        drawCircle(Brush.radialGradient(0.69f to Color(0x73FFC85A), 1f to Color(0x00FFC85A), center = o, radius = 258f), 258f, o)
        drawCircle(csGoldBrush(Offset(28f, 28f), Offset(492f, 492f)), 223f, o, style = Stroke(18f))
        drawCircle(Brush.radialGradient(0.7f to Color(0xFF0D1A5A), 1f to Color(0xFF16267A), center = o, radius = 214f), 214f, o)
        for (i in 0 until 16) {
            val a = i * CS_TAU / 16; val x = c + cos(a) * 196f; val y = c + sin(a) * 196f
            csScroll(x, y, 0.28f, a + PI.toFloat() / 2, false, Color(0xFFE3B450), 2.2f)
            csScroll(x, y, 0.28f, a + PI.toFloat() / 2, true, Color(0xFFE3B450), 2.2f)
        }
        drawCircle(Color(0x99FFDC82), 206f, o, style = Stroke(2f))
        drawCircle(csGoldBrush(Offset(84f, 84f), Offset(436f, 436f)), 169f, o, style = Stroke(14f))
        drawCircle(Brush.radialGradient(0f to Color(0xFF4D72E6), 0.45f to Color(0xFF1E3AA8), 1f to Color(0xFF0A165A), center = Offset(c - 40f, c - 50f), radius = 165f), 162f, o)
        rotate(-14.3f, Offset(c - 30f, c - 80f)) { drawOval(Color(0x1AFFFFFF), Offset(c - 125f, c - 120f), Size(190f, 80f)) }
        csScroll(c - 170f, c + 30f, 0.5f, -0.6f * PI.toFloat(), false, Color(0xFFF2C55A), 3f)
        csScroll(c + 170f, c + 30f, 0.5f, 0.6f * PI.toFloat(), true, Color(0xFFF2C55A), 3f)

        // ribbon
        val a0 = 203.4f; val sw = 133.2f
        val rib = Path().apply {
            arcTo(Rect(o, 172f), a0, sw, true)
            arcTo(Rect(o, 128f), a0 + sw, -sw, false)
            close()
        }
        drawPath(rib, csGoldBrush(Offset(c - 170f, c - 170f), Offset(c + 170f, c - 60f)))
        drawPath(rib, Color(0xFF7A4E06), style = Stroke(3f))
        drawArc(Color(0xCCFFF5C8), a0 + 1.2f, sw - 2.4f, false, Offset(c - 166f, c - 166f), Size(332f, 332f), style = Stroke(1.5f))
        drawArc(Color(0xCCFFF5C8), a0 + 1.7f, sw - 3.4f, false, Offset(c - 134f, c - 134f), Size(268f, 268f), style = Stroke(1.5f))
        for (end in 0..1) {
            val a = (if (end == 0) a0 else a0 + sw) * PI.toFloat() / 180f
            val x = c + cos(a) * 150f + if (end == 0) -6f else 6f; val y = c + sin(a) * 150f + 10f
            drawPath(Path().apply { csSpiral(x, y, 10f, 1.2f, if (end == 0) 1f else -1f, 0f) }, Color(0xFFF5D06A), style = Stroke(3f, cap = StrokeCap.Round))
        }
        drawIntoCanvas { cv ->
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF1A1F55.toInt(); textSize = 34f; typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            val txt = "AD TOURNAMENT"
            var tw = 0f; for (ch in txt) tw += paint.measureText(ch.toString()) + 3f
            var ang = 1.5f * PI.toFloat() - tw / 2f / 150f
            for (ch in txt) {
                val w = paint.measureText(ch.toString())
                ang += w / 2f / 150f
                cv.nativeCanvas.save()
                cv.nativeCanvas.translate(c + cos(ang) * 150f, c + sin(ang) * 150f)
                cv.nativeCanvas.rotate((ang + PI.toFloat() / 2) * 180f / PI.toFloat())
                cv.nativeCanvas.drawText(ch.toString(), -w / 2f, 12f, paint)
                cv.nativeCanvas.restore()
                ang += (w / 2f + 3f) / 150f
            }
        }

        // carrom board
        val s = 152f; val x0 = c - s / 2; val y0 = c - s / 2 + 30f; val bx = x0 + s; val by = y0 + s; val cy = y0 + s / 2
        drawRoundRect(Color(0x66000000), Offset(x0 - 14f, y0 - 4f), Size(s + 28f, s + 28f), CornerRadius(14f))
        drawRoundRect(Brush.linearGradient(listOf(Color(0xFFC88A4A), Color(0xFF7A3A12), Color(0xFF4A1E06)), Offset(x0, y0), Offset(bx, by)),
            Offset(x0 - 14f, y0 - 14f), Size(s + 28f, s + 28f), CornerRadius(14f))
        drawRoundRect(csGoldBrush(Offset(x0, y0), Offset(bx, by)), Offset(x0 - 14f, y0 - 14f), Size(s + 28f, s + 28f), CornerRadius(14f), style = Stroke(3f))
        drawRect(Brush.linearGradient(listOf(Color(0xFFFBE9C2), Color(0xFFEFCF95), Color(0xFFE2B878)), Offset(x0, y0), Offset(bx, by)), Offset(x0, y0), Size(s, s))
        val line = Color(0xBF5A280A)
        drawLine(line, Offset(x0 + 28f, y0 + 20f), Offset(bx - 28f, y0 + 20f), 1.3f)
        drawLine(line, Offset(x0 + 28f, by - 20f), Offset(bx - 28f, by - 20f), 1.3f)
        drawLine(line, Offset(x0 + 20f, y0 + 28f), Offset(x0 + 20f, by - 28f), 1.3f)
        drawLine(line, Offset(bx - 20f, y0 + 28f), Offset(bx - 20f, by - 28f), 1.3f)
        drawCircle(line, 26f, Offset(c, cy), style = Stroke(1.3f))
        drawCircle(Color(0xFFC02030), 9f, Offset(c, cy), style = Stroke(1.3f))
        for (dx in listOf(-1f, 1f)) for (dy in listOf(-1f, 1f))
            drawLine(Color(0x995A280A), Offset(c + dx * 34f, cy + dy * 34f), Offset(c + dx * 52f, cy + dy * 52f), 1.3f)
        for (p in listOf(Offset(x0 + 11f, y0 + 11f), Offset(bx - 11f, y0 + 11f), Offset(x0 + 11f, by - 11f), Offset(bx - 11f, by - 11f))) {
            drawCircle(Brush.radialGradient(listOf(Color(0xFF333333), Color.Black), center = p - Offset(2f, 2f), radius = 10f), 10f, p)
            drawCircle(Color(0xFFD6A64A), 10f, p, style = Stroke(2.5f))
        }
        drawCoin('q', c, cy, 7f)
        for (i in 0 until 6) {
            val a = i * PI.toFloat() / 3 + PI.toFloat() / 6
            drawCoin(if (i % 2 == 1) 'k' else 'w', c + cos(a) * 15f, cy + sin(a) * 15f, 7f)
        }
        drawPath(Path().apply { moveTo(x0, y0); lineTo(x0 + s * 0.55f, y0); lineTo(x0, y0 + s * 0.55f); close() }, Color(0x1AFFFFFF))
    }
}

/** Ornate milestone medal (120-unit box) with a coin inside: 'w', 's' striker, 'q', 'k'. */
internal fun DrawScope.drawCarromMedal(coin: Char) {
    val u = size.width / 120f
    withTransform({ scale(u, u, Offset.Zero) }) {
        val c = 60f
        for (i in 0 until 8) {
            rotate(i * 45f, Offset(c, c)) {
                val p = Path().apply { moveTo(c - 10f, c - 30f); quadraticBezierTo(c, c - 62f, c + 10f, c - 30f); close() }
                if (i % 2 == 1) drawPath(p, Color(0xFFC9CCD8)) else drawPath(p, csGoldBrush(Offset(c - 10f, c - 60f), Offset(c + 10f, c - 30f)))
                drawPath(p, Color(0xFF5A3A00), style = Stroke(1f))
            }
        }
        csScroll(c - 8f, c + 40f, 0.25f, PI.toFloat() / 2, false, Color(0xFFE8B848), 1.6f)
        csScroll(c + 8f, c + 40f, 0.25f, PI.toFloat() / 2, true, Color(0xFFE8B848), 1.6f)
        drawCircle(csGoldBrush(Offset(24f, 24f), Offset(96f, 96f)), 36f, Offset(c, c))
        drawCircle(Color(0xFF0E1A50), 31f, Offset(c, c))
        if (coin == 's') drawStriker(c, c, 24f, 0f) else drawCoin(coin, c, c, 26f)
    }
}


internal fun csNearSparks(): List<CsSpark> {
    val r = java.util.Random(7)
    return List(110) {
        val a = r.nextFloat() * CS_TAU; val d = 172f + r.nextFloat() * 280f
        CsSpark(cos(a) * d * 1.15f, sin(a) * d * 0.8f, 0.8f + r.nextFloat() * 2.6f, r.nextFloat() * CS_TAU, 1 + r.nextInt(3))
    }
}

internal fun csFarSparks(): List<CsSpark> {
    val r = java.util.Random(11)
    return List(50) { CsSpark(r.nextFloat() * 780f, r.nextFloat() * 1680f, 0.8f + r.nextFloat() * 2.6f, r.nextFloat() * CS_TAU, 1 + r.nextInt(3)) }
}

/** Full-screen background: radial navy, gold corner filigree, twinkling sparkles. */
internal fun DrawScope.drawCarromSplashBg(far: List<CsSpark>, tw: Float) {
    drawRect(Brush.radialGradient(0f to Color(0xFF1C3384), 0.55f to Color(0xFF0F1C58), 1f to Color(0xFF08103A),
        center = Offset(size.width / 2, size.height * 0.3f), radius = size.maxDimension * 0.75f))
    val u = size.width / 780f; val W = size.width; val H = size.height
    val fc = Color(0xCCE1B45A); val fc2 = Color(0x59D7AA50); val fc3 = Color(0x38D7AA50)
    csScroll(W, 40f * u, 2.6f * u, 0.62f * PI.toFloat(), false, fc, 2.2f * u)
    csScroll(W - 80f * u, 0f, 1.6f * u, 0.9f * PI.toFloat(), true, fc2, 1.8f * u)
    csScroll(0f, H - 20f * u, 2.4f * u, -0.45f * PI.toFloat(), false, fc, 2.2f * u)
    csScroll(W, H - 20f * u, 2.4f * u, -0.55f * PI.toFloat(), true, fc, 2.2f * u)
    csScroll(0f, H * 0.71f, 1.6f * u, -0.15f * PI.toFloat(), false, fc3, 1.6f * u)
    csScroll(W, H * 0.31f, 1.5f * u, 1.1f * PI.toFloat(), true, fc3, 1.6f * u)
    csSparkles(far, tw, 0f, 0f, u)
}

/** Glow, gold swooshes and sparkles around the crest (drawn in the crest's box, may spill outside). */
internal fun DrawScope.drawCarromAura(near: List<CsSpark>, tw: Float) {
    val u = size.width / 520f; val o = center
    drawCircle(Brush.radialGradient(0f to Color(0x38FFCD6E), 0.5f to Color(0x1F5A82FF), 1f to Color(0x005A82FF), center = o, radius = 450f * u), 450f * u, o)
    rotate(-18.3f, o) {
        for (i in 0 until 3) {
            val rx = (300f + i * 14f) * 1.075f * u; val ry = (90f + i * 8f) * 1.075f * u
            val st = Stroke((3f - i * 0.8f) * u * 1.6f)
            val col = Color(0xFFFFC85A).copy(alpha = 0.25f - i * 0.07f)
            drawArc(col, 147.6f, 77.4f, false, Offset(o.x - rx, o.y - ry), Size(rx * 2, ry * 2), style = st)
            drawArc(col, 333f, 63f, false, Offset(o.x - rx, o.y - ry), Size(rx * 2, ry * 2), style = st)
        }
    }
    csSparkles(near, tw, o.x, o.y, u * 1.075f)
}
