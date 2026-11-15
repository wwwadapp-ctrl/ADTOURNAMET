package com.example.ui.wallet

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.graphics.drawscope.rotate as drawScopeRotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Wallet pages artwork, part 1: shared pieces (3D text, page background, ornate frame, pill, rays, picture).
// VISUAL ONLY: no data, no clicks.

enum class WlTone { GOLD, CREAM, RED, GREEN, GREY }

private fun toneColors(t: WlTone): Triple<Color, Color, List<Color>> = when (t) {
    WlTone.GOLD -> Triple(Color(0xFF3A1A00), Color(0xFF7A3E00), listOf(Color(0xFFFFFDE8), Color(0xFFFFE66A), Color(0xFFFFC21E), Color(0xFFF08A00)))
    WlTone.CREAM -> Triple(Color(0xFF5A3200), Color(0xFFB07A1E), listOf(Color.White, Color(0xFFFFF8E0), Color(0xFFF2D9A0)))
    WlTone.RED -> Triple(Color(0xFF3A0008), Color(0xFF7A0A1A), listOf(Color(0xFFFFB0B8), Color(0xFFFF4D62), Color(0xFFD81838)))
    WlTone.GREEN -> Triple(Color(0xFF00280F), Color(0xFF0A5A2A), listOf(Color(0xFFD8FFE6), Color(0xFF5EF09A), Color(0xFF16A34A)))
    WlTone.GREY -> Triple(Color(0xFF10142E), Color(0xFF3A4060), listOf(Color(0xFFE6EBFF), Color(0xFFAEB6D8), Color(0xFF8088B0)))
}

/** Chunky 3D text: dark depth below, sharp outline, bright fill on top. No blur. */
@Composable
fun WlText3D(
    text: String,
    fontSize: TextUnit,
    tone: WlTone,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Black,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val px = with(LocalDensity.current) { fontSize.toPx() }
    val (deep, line, fill) = toneColors(tone)
    val base = TextStyle(fontSize = fontSize, fontWeight = fontWeight)
    Box(modifier = modifier) {
        Text(text, style = base.copy(color = deep, drawStyle = Stroke(px * 0.14f, join = StrokeJoin.Round)), maxLines = 1, softWrap = false, overflow = overflow,
            modifier = Modifier.offset { IntOffset(0, (px * 0.1f).roundToInt()) })
        Text(text, style = base.copy(color = line, drawStyle = Stroke(px * 0.1f, join = StrokeJoin.Round)), maxLines = 1, softWrap = false, overflow = overflow)
        Text(text, style = base.copy(brush = Brush.verticalGradient(fill)), maxLines = 1, softWrap = false, overflow = overflow)
    }
}

/** Deep royal page background with soft colour clouds and twinkling gold dots. */
fun Modifier.wlBackground(): Modifier = composed {
    val anim = rememberInfiniteTransition(label = "wlBg")
    val tw by anim.animateFloat(0.15f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "wlTw")
    background(Brush.verticalGradient(listOf(Color(0xFF1A1F8A), Color(0xFF161870), Color(0xFF0E0F4A)))).drawBehind {
        val w = size.width; val h = size.height
        drawCircle(Brush.radialGradient(listOf(Color(0x73783CDC), Color.Transparent), center = Offset(w * 0.85f, h * 0.12f), radius = w * 0.7f), w * 0.7f, Offset(w * 0.85f, h * 0.12f))
        drawCircle(Brush.radialGradient(listOf(Color(0x4D3C50FF), Color.Transparent), center = Offset(w * 0.1f, h * 0.6f), radius = w * 0.7f), w * 0.7f, Offset(w * 0.1f, h * 0.6f))
        val pts = floatArrayOf(0.08f, 0.05f, 0.9f, 0.09f, 0.35f, 0.16f, 0.72f, 0.3f, 0.05f, 0.38f, 0.95f, 0.47f, 0.4f, 0.55f, 0.15f, 0.7f, 0.8f, 0.72f, 0.55f, 0.86f, 0.25f, 0.93f, 0.92f, 0.95f)
        for (i in 0 until pts.size / 2) {
            val a = if (i % 2 == 0) tw else 1.15f - tw
            val c = Offset(w * pts[i * 2], h * pts[i * 2 + 1])
            val big = i % 4 == 0
            drawCircle(Color(0xFFFFD68C).copy(alpha = (if (big) 0.18f else 0.3f) * a), (if (big) 9f else 4.5f) * density, c)
            if (!big) drawCircle(Color(0xFFFFF3C4).copy(alpha = a), 1.3f * density, c)
        }
    }
}

/** One ornate corner. (x, y) is the outer corner; sx / sy = +1 or -1 choose which corner. */
private fun DrawScope.wlCorner(x: Float, y: Float, sx: Float, sy: Float, col: Color) {
    val s = 0.85f * density
    fun o(px: Float, py: Float) = Offset(x + px * s * sx, y + py * s * sy)
    val st = Stroke(1.5f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun curl(swap: Boolean) {
        fun p(a: Float, b: Float) = if (swap) o(b, a) else o(a, b)
        val path = Path().apply {
            p(8f, 30f).let { moveTo(it.x, it.y) }
            cubicTo(p(8f, 22f).x, p(8f, 22f).y, p(11f, 17f).x, p(11f, 17f).y, p(17f, 15f).x, p(17f, 15f).y)
            cubicTo(p(23f, 13f).x, p(23f, 13f).y, p(24f, 7f).x, p(24f, 7f).y, p(20f, 5f).x, p(20f, 5f).y)
            cubicTo(p(16f, 3f).x, p(16f, 3f).y, p(12f, 7f).x, p(12f, 7f).y, p(15f, 10f).x, p(15f, 10f).y)
        }
        drawPath(path, col, style = st)
    }
    val edge = Path().apply {
        moveTo(o(3f, 38f).x, o(3f, 38f).y); lineTo(o(3f, 14f).x, o(3f, 14f).y)
        cubicTo(o(3f, 8f).x, o(3f, 8f).y, o(8f, 3f).x, o(8f, 3f).y, o(14f, 3f).x, o(14f, 3f).y)
        lineTo(o(38f, 3f).x, o(38f, 3f).y)
    }
    drawPath(edge, col, style = st)
    curl(false); curl(true)
    drawCircle(col, 1.8f * s, o(12f, 12f))
}

/**
 * Ornate card: metal edge, royal glass inside, glass light on top, moving shine.
 * gold = true: gold edge with four ornate corners. gold = false: silver-blue double line.
 */
@Composable
fun WlFrame(modifier: Modifier = Modifier, gold: Boolean = true, pad: Dp = 16.dp, content: @Composable BoxScope.() -> Unit) {
    val anim = rememberInfiniteTransition(label = "wlFrame")
    val sweep by anim.animateFloat(-0.3f, 2.4f, infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing)), label = "wlSweep")
    val edge = if (gold) listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4))
    else listOf(Color(0xFFE6ECFF), Color(0xFF8FA0E8), Color(0xFF3A48A8), Color(0xFFC8D2FF))
    val line = if (gold) Color(0xFFE8B84A) else Color(0xFFC8D2FF)
    val outer = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(14.dp, outer, ambientColor = Color(0xFF050532), spotColor = if (gold) Color(0xFFFFC850) else Color(0xFF8CA0FF))
            .clip(outer)
            .background(Brush.linearGradient(edge))
            .padding(2.5.dp)
            .clip(RoundedCornerShape(21.5.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2A36A4), Color(0xFF1B2382), Color(0xFF131A66)), start = Offset(0f, 0f), end = Offset(400f, 900f)))
            .drawBehind {
                val w = size.width; val h = size.height
                drawCircle(Brush.radialGradient(listOf(Color(0x597882FF), Color.Transparent), center = Offset(w * 0.8f, 0f), radius = w * 0.6f), w * 0.6f, Offset(w * 0.8f, 0f))
                // glass light on the upper part
                drawOval(Brush.verticalGradient(listOf(Color(0x24FFFFFF), Color(0x05FFFFFF)), startY = -h * 0.6f, endY = h * 0.35f), topLeft = Offset(-w * 0.1f, -h * 0.6f), size = Size(w * 1.2f, h * 0.95f))
                if (gold) {
                    val e = 6.dp.toPx()
                    wlCorner(e, e, 1f, 1f, line); wlCorner(w - e, e, -1f, 1f, line); wlCorner(e, h - e, 1f, -1f, line); wlCorner(w - e, h - e, -1f, -1f, line)
                    val m = 44.dp.toPx(); val t = 9.dp.toPx()
                    drawLine(line, Offset(m, t), Offset(w - m, t), 1.4.dp.toPx(), alpha = 0.85f)
                    drawLine(line, Offset(m, h - t), Offset(w - m, h - t), 1.4.dp.toPx(), alpha = 0.85f)
                } else {
                    val i = 7.dp.toPx()
                    drawRoundRect(line, topLeft = Offset(i, i), size = Size(w - 2 * i, h - 2 * i), cornerRadius = CornerRadius(17.dp.toPx()), style = Stroke(1.4.dp.toPx()), alpha = 0.9f)
                }
            }
            .drawWithContent {
                drawContent()
                val x = size.width * sweep
                drawScopeRotate(22f, pivot = Offset(x, size.height / 2f)) {
                    drawRect(Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x1AFFFFFF), Color(0x00FFFFFF)), startX = x - 30.dp.toPx(), endX = x + 30.dp.toPx()), topLeft = Offset(x - 30.dp.toPx(), -size.height), size = Size(60.dp.toPx(), size.height * 3f))
                }
            }
            .padding(pad),
        content = content
    )
}
