package com.example.ui.autocarrom

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

// Auto Carrom board + pieces artwork (drawn in code, no pictures). VISUAL ONLY.
// Board picture = 852 units: 56 frame + 740 playing area + 56 frame. mm -> px: (CB_F + mm) * k.

const val CB_W = 852f
const val CB_F = 56f

private val Gold = listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4))

/** The whole static board (frame, maple, lines, rosette, pockets). Call from a Canvas that never changes. */
fun DrawScope.drawCarromBoard() {
    val k = size.width / CB_W
    val W = size.width
    fun p(v: Float) = v * k
    val gold = Brush.linearGradient(Gold, Offset.Zero, Offset(W, W))
    // mahogany frame
    drawRoundRect(Brush.linearGradient(listOf(Color(0xFF7A3010), Color(0xFF451606), Color(0xFF62260C), Color(0xFF2E0E02)), Offset.Zero, Offset(W, W)), cornerRadius = CornerRadius(p(34f)))
    val grain = Path()
    for (i in 0 until 40) { val o = p(i * 22f); grain.moveTo(o, 0f); grain.cubicTo(o + p(30f), W * 0.3f, o - p(30f), W * 0.6f, o + p(10f), W) }
    drawPath(grain, Color(0x1F1A0600), style = Stroke(p(2f)))
    drawRoundRect(Brush.linearGradient(0f to Color(0x59FFDCAA), 0.25f to Color(0x00FFDCAA), 0.75f to Color(0x00000000), 1f to Color(0x73000000), start = Offset.Zero, end = Offset(W, W)), cornerRadius = CornerRadius(p(34f)))
    drawRoundRect(gold, Offset(p(3.5f), p(3.5f)), Size(W - p(7f), W - p(7f)), CornerRadius(p(31f)), style = Stroke(p(7f)))
    drawRoundRect(gold, Offset(p(18f), p(18f)), Size(W - p(36f), W - p(36f)), CornerRadius(p(22f)), style = Stroke(p(2.5f)))
    drawRoundRect(gold, Offset(p(CB_F - 12f), p(CB_F - 12f)), Size(W - p(2 * (CB_F - 12f)), W - p(2 * (CB_F - 12f))), CornerRadius(p(10f)), style = Stroke(p(2.5f)))
    val mid = (18f + CB_F - 12f) / 2f
    var t = 90f
    while (t < CB_W - 90f) {
        for ((x, y) in listOf(t to mid, t to CB_W - mid, mid to t, CB_W - mid to t)) {
            val d = Path().apply { moveTo(p(x), p(y - 5)); lineTo(p(x + 5), p(y)); lineTo(p(x), p(y + 5)); lineTo(p(x - 5), p(y)); close() }
            drawPath(d, gold)
        }
        t += 34f
    }
    for ((x, y) in listOf(mid to mid, CB_W - mid to mid, mid to CB_W - mid, CB_W - mid to CB_W - mid)) {
        val c = Offset(p(x), p(y))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF8D8), Color(0xFFE0A82A), Color(0xFF6A4404)), c - Offset(p(5f), p(5f)), p(24f)), p(22f), c)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFB0BA), Color(0xFFD8102A), Color(0xFF5A0010)), c - Offset(p(3f), p(3f)), p(10f)), p(9f), c)
    }
    // playing surface
    val f = p(CB_F); val s = p(CarromRules.S.toFloat())
    drawRect(Brush.linearGradient(listOf(Color(0xFFF9E4B4), Color(0xFFF1D39A), Color(0xFFE6BE7C)), Offset(f, f), Offset(f + s, f + s)), Offset(f, f), Size(s, s))
    clipRect(f, f, f + s, f + s) {
        val g2 = Path()
        for (i in 0 until 70) { val y = f + p(i * 10.7f + sin(i * 2.1f) * 3f); g2.moveTo(f, y); g2.cubicTo(f + p(250f), y + p(7f * sin(i * 1.7f)), f + p(500f), y - p(7f * cos(i.toFloat())), f + s, y + p(4f)) }
        drawPath(g2, Color(0x128A5A20), style = Stroke(p(1.3f)))
        drawRect(Brush.radialGradient(0f to Color(0x73FFFAE6), 0.6f to Color(0x00FFF0C8), 1f to Color(0x475A2800), center = Offset(f + s / 2, f + s / 2), radius = s * 0.72f), Offset(f, f), Size(s, s))
        drawRect(Brush.verticalGradient(listOf(Color(0x73280A00), Color(0x00280A00)), f, f + p(26f)), Offset(f, f), Size(s, p(26f)))
        drawRect(Brush.horizontalGradient(listOf(Color(0x59280A00), Color(0x00280A00)), f, f + p(26f)), Offset(f, f), Size(p(26f), s))
    }
    drawRect(Color(0x80000000), Offset(f - p(1.5f), f - p(1.5f)), Size(s + p(3f), s + p(3f)), style = Stroke(p(3f)))
    val ink = Color(0xFF3A1A08)
    fun mm(x: Double, y: Double) = Offset(p(CB_F + x.toFloat()), p(CB_F + y.toFloat()))
    fun line(a: Offset, b: Offset, w: Float) = drawLine(ink, a, b, p(w))
    fun redDot(c: Offset, r: Float) {
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFF6A7A), Color(0xFF8A0A1E)), c - Offset(p(r * 0.3f), p(r * 0.3f)), p(r)), p(r), c)
        drawCircle(Color(0xFFC98A10), p(r), c, style = Stroke(p(2f)))
    }
    val S = CarromRules.S; val B = S - CarromRules.BASE
    for (side in 0 until 4) {
        rotate(side * 90f, mm(S / 2, S / 2)) {
            drawRect(Color(0x14B3122F), mm(CarromRules.XMIN, B - 16), Size(p((CarromRules.XMAX - CarromRules.XMIN).toFloat()), p(32f)))
            line(mm(CarromRules.XMIN, B - 16), mm(CarromRules.XMAX, B - 16), 2.2f)
            line(mm(CarromRules.XMIN, B + 16), mm(CarromRules.XMAX, B + 16), 1.4f)
            redDot(mm(CarromRules.XMIN, B), 16f); redDot(mm(CarromRules.XMAX, B), 16f)
        }
    }
    for ((px, py) in CarromSim.POCKETS) {
        val dx = sign(S / 2 - px); val dy = sign(S / 2 - py)
        line(mm(px + dx * 58, py + dy * 58), mm(px + dx * 236, py + dy * 236), 1.6f)
        drawCircle(ink, p(22f), mm(px + dx * 214, py + dy * 214), style = Stroke(p(1.6f)))
        line(mm(px + dx * 58, py + dy * 58), mm(px + dx * 70, py + dy * 58), 1.6f)
        line(mm(px + dx * 58, py + dy * 58), mm(px + dx * 58, py + dy * 70), 1.6f)
    }
    val C = mm(S / 2, S / 2)
    drawCircle(Brush.radialGradient(listOf(Color(0x2EB3122F), Color(0x00B3122F)), C, p(92f)), p(92f), C)
    drawCircle(ink, p(88f), C, style = Stroke(p(2.2f)))
    drawCircle(ink, p(82f), C, style = Stroke(p(1.2f)))
    for (layer in 0..1) {
        val star = Path()
        for (i in 0 until 16) {
            val rr = if (i % 2 == 1) (if (layer == 1) 22f else 30f) else (if (layer == 1) 50f else 74f)
            val a = i * PI.toFloat() / 8 - PI.toFloat() / 2 + layer * PI.toFloat() / 8
            val pt = C + Offset(p(cos(a) * rr), p(sin(a) * rr))
            if (i == 0) star.moveTo(pt.x, pt.y) else star.lineTo(pt.x, pt.y)
        }
        star.close()
        drawPath(star, if (layer == 1) Color(0x8CE0A82A) else Color(0x8CB3122F))
        drawPath(star, if (layer == 1) Color(0xFF8A5A08) else Color(0xFF6A0010), style = Stroke(p(1f)))
    }
    redDot(C, 15f)
    val rp = CarromRules.RP.toFloat()
    for ((px, py) in CarromSim.POCKETS) {
        val c = mm(px, py)
        drawCircle(Brush.radialGradient(0f to Color(0xFFFFF3C4), 0.4f to Color(0xFFE0A82A), 1f to Color(0xFF6A4404), center = c, radius = p(rp + 10f)), p(rp + 9f), c)
        drawPocketHole(c, p(rp + 1f))
    }
}

fun DrawScope.drawPocketHole(c: Offset, r: Float) {
    drawCircle(Brush.radialGradient(0f to Color.Black, 0.8f to Color(0xFF120602), 1f to Color(0xFF3A1A06), center = c - Offset(r * 0.17f, r * 0.17f), radius = r), r, c)
}

/** One coin: 'w' ivory, 'k' ebony, 'q' ruby queen. All with a gold rim, carved grooves and gloss. */
fun DrawScope.drawCoin(c: Char, cx: Float, cy: Float, r: Float, alpha: Float = 1f) {
    val cols = when (c) {
        'w' -> listOf(Color.White, Color(0xFFFFF6E0), Color(0xFFE9D7AE), Color(0xFFA88A50))
        'k' -> listOf(Color(0xFF9A9AAA), Color(0xFF3A3A48), Color(0xFF15151C), Color.Black)
        else -> listOf(Color(0xFFFFC8D0), Color(0xFFF0304A), Color(0xFFA0081E), Color(0xFF4A0008))
    }
    val o = Offset(cx, cy)
    drawOval(Color(0x61280A00), Offset(cx - r + r * 0.18f, cy - r * 0.95f + r * 0.28f), Size(r * 2, r * 1.9f), alpha = alpha)
    drawCircle(Brush.radialGradient(0f to cols[0], 0.35f to cols[1], 0.8f to cols[2], 1f to cols[3], center = o - Offset(r * 0.35f, r * 0.4f), radius = r * 1.3f), r, o, alpha = alpha)
    drawCircle(Brush.linearGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFFFE7A0)), o - Offset(r, r), o + Offset(r, r)), r - r * 0.08f, o, alpha = alpha, style = Stroke(r * 0.15f))
    val groove = when (c) { 'w' -> Color(0x8C8C6932); 'k' -> Color(0x38FFFFFF); else -> Color(0x99FFDC8C) }
    drawCircle(groove, r * 0.66f, o, alpha = alpha, style = Stroke(r * 0.07f))
    drawCircle(groove, r * 0.42f, o, alpha = alpha, style = Stroke(r * 0.07f))
    if (c == 'q') {
        val st = Path()
        for (i in 0 until 10) {
            val rr = if (i % 2 == 1) r * 0.16f else r * 0.36f
            val a = i * PI.toFloat() / 5 - PI.toFloat() / 2
            if (i == 0) st.moveTo(cx + cos(a) * rr, cy + sin(a) * rr) else st.lineTo(cx + cos(a) * rr, cy + sin(a) * rr)
        }
        st.close(); drawPath(st, Color(0xFFFFD45A), alpha = alpha)
    } else drawCircle(if (c == 'w') Color(0xBFC8A050) else Color(0xD9E0A82A), r * 0.16f, o, alpha = alpha)
    drawOval(if (c == 'k') Color(0x59FFFFFF) else Color(0x99FFFFFF), Offset(cx - r * 0.72f, cy - r * 0.6f), Size(r * 0.84f, r * 0.36f), alpha = alpha)
}

/** Premium striker: glow, gold rim, glossy blue centre, gold star, rim shine and a twinkle ([tw] 0..1). */
fun DrawScope.drawStriker(cx: Float, cy: Float, r: Float, tw: Float, alpha: Float = 1f) {
    val o = Offset(cx, cy)
    drawCircle(Brush.radialGradient(0f to Color(0xBFFFE27A), 1f to Color(0x00FFE27A), center = o, radius = r * 1.75f), r * 1.75f, o, alpha = alpha)
    drawOval(Color(0x61280A00), Offset(cx - r + r * 0.12f, cy - r * 0.95f + r * 0.18f), Size(r * 2, r * 1.9f), alpha = alpha)
    drawCircle(Brush.linearGradient(listOf(Color.White, Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4)), o - Offset(r, r), o + Offset(r, r)), r, o, alpha = alpha)
    drawCircle(Brush.radialGradient(0f to Color(0xFFBFD0FF), 0.35f to Color(0xFF4A6BFF), 0.8f to Color(0xFF1A2AB0), 1f to Color(0xFF0A1050), center = o - Offset(r * 0.3f, r * 0.35f), radius = r * 0.85f), r * 0.8f, o, alpha = alpha)
    drawCircle(Color(0xD9FFE27A), r * 0.66f, o, alpha = alpha, style = Stroke(r * 0.06f))
    val st = Path()
    for (i in 0 until 10) {
        val rr = if (i % 2 == 1) r * 0.2f else r * 0.48f
        val a = i * PI.toFloat() / 5 - PI.toFloat() / 2
        if (i == 0) st.moveTo(cx + cos(a) * rr, cy + sin(a) * rr) else st.lineTo(cx + cos(a) * rr, cy + sin(a) * rr)
    }
    st.close()
    drawPath(st, Brush.verticalGradient(listOf(Color(0xFFFFF8D0), Color(0xFFE0A82A)), cy - r * 0.5f, cy + r * 0.5f), alpha = alpha)
    drawOval(Color(0xB3FFFFFF), Offset(cx - r * 0.7f, cy - r * 0.6f), Size(r * 0.76f, r * 0.36f), alpha = alpha)
    drawArc(Color.White, -149f, 80f, false, Offset(cx - r + r * 0.05f, cy - r + r * 0.05f), Size(r * 1.9f, r * 1.9f), alpha = alpha, style = Stroke(r * 0.1f))
    val sr = r * (0.35f + 0.25f * tw); val sx = cx + r * 0.55f; val sy = cy - r * 0.55f
    val sp = Path()
    for (i in 0 until 8) {
        val rr = if (i % 2 == 1) sr * 0.18f else sr
        val a = i * PI.toFloat() / 4
        if (i == 0) sp.moveTo(sx + cos(a) * rr, sy + sin(a) * rr) else sp.lineTo(sx + cos(a) * rr, sy + sin(a) * rr)
    }
    sp.close(); drawPath(sp, Color.White, alpha = alpha * (0.55f + 0.45f * tw))
}
