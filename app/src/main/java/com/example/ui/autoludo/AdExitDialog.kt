package com.example.ui.autoludo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.i18n.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val ExGold = Brush.linearGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE8B23A), Color(0xFF9A6400), Color(0xFFF6D27A)))

@Composable
fun AdExitDialog(
    opponentName: String,
    entryFee: Double,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    androidx.compose.runtime.LaunchedEffect(Unit) { AdGameSfx.play(AdGameSfx.Sfx.ALERT) }
    val inf = rememberInfiniteTransition(label = "exitFx")
    val twinkle by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "tw")
    val pulse by inf.animateFloat(0.85f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "pl")

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth(0.9f), contentAlignment = Alignment.TopCenter) {
            Canvas(modifier = Modifier.matchParentSize()) { drawSparkles(twinkle) }
            Box(modifier = Modifier.padding(top = 44.dp).fillMaxWidth()) {
                val outer = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .shadow(28.dp, outer, ambientColor = Color(0xFFFFC107), spotColor = Color(0xFFFFA000), clip = false)
                        .background(Brush.verticalGradient(listOf(Color(0xFF1A2A72), Color(0xFF0E1A4E), Color(0xFF0A1238))), outer)
                        .border(3.dp, ExGold, outer)
                )
                Canvas(modifier = Modifier.matchParentSize()) { drawFiligree() }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 54.dp, bottom = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.autoLudoExitTitle,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            lineHeight = 30.sp,
                            shadow = Shadow(Color(0xCCFFC107), Offset(0f, 0f), 18f)
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xFFC9D6FF))) { append(strings.autoLudoExitMessagePrefix) }
                            withStyle(SpanStyle(color = Color(0xFFFFD54F), fontWeight = FontWeight.ExtraBold)) { append(opponentName) }
                            withStyle(SpanStyle(color = Color(0xFFC9D6FF))) { append(strings.autoLudoExitMessageSuffix) }
                        },
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = strings.autoLudoExitWarningFee(strings.taka(entryFee)),
                        color = Color(0xFFFFD27A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF3A0610), Color(0xFF6A0E1C), Color(0xFF3A0610))), RoundedCornerShape(50))
                            .border(1.5.dp, ExGold, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                    Spacer(Modifier.height(22.dp))
                    val btnShape = RoundedCornerShape(16.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .shadow(12.dp, btnShape, ambientColor = Color(0xFFFFB300), spotColor = Color(0xFFFFA000), clip = false)
                            .background(Brush.verticalGradient(listOf(Color(0xFFFFF4C2), Color(0xFFF6C84E), Color(0xFFE0A21E), Color(0xFFB57A08))), btnShape)
                            .border(1.5.dp, Brush.verticalGradient(listOf(Color(0xFFFFFBE6), Color(0xFF9A6400))), btnShape)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { AdGameSfx.play(AdGameSfx.Sfx.TAP); onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .fillMaxWidth()
                                .height(18.dp)
                                .background(Brush.verticalGradient(listOf(Color(0x99FFFFFF), Color(0x00FFFFFF))), RoundedCornerShape(12.dp))
                        )
                        Text(strings.autoLudoExitKeepPlaying, color = Color(0xFF2A1700), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(Brush.verticalGradient(listOf(Color(0xFF221A3A), Color(0xFF120E24))), btnShape)
                            .border(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFFB3261E), Color(0xFFE8B23A), Color(0xFFB3261E))), btnShape)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { AdGameSfx.play(AdGameSfx.Sfx.TAP); onConfirm() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(strings.autoLudoExitConfirm, color = Color(0xFFFF4D4D), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Canvas(modifier = Modifier.size(96.dp)) { drawMedallion(pulse) }
        }
    }
}

private fun DrawScope.drawSparkles(t: Float) {
    val seeds = listOf(0.05f to 0.20f, 0.95f to 0.25f, 0.02f to 0.55f, 0.98f to 0.60f, 0.08f to 0.85f, 0.93f to 0.88f, 0.30f to 0.04f, 0.70f to 0.05f, 0.15f to 0.40f, 0.86f to 0.45f, 0.5f to 0.98f, 0.04f to 0.72f)
    seeds.forEachIndexed { i, (sx, sy) ->
        val ph = (t + i * 0.137f) % 1f
        val a = (sin(ph * 2 * PI).toFloat() * 0.5f + 0.5f)
        val c = Offset(size.width * sx, size.height * sy)
        val r = (2f + (i % 3) * 1.2f) * density
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE8A0).copy(alpha = a), Color(0x00FFC107)), center = c, radius = r * 3f), radius = r * 3f, center = c)
        drawCircle(Color(0xFFFFF6D0).copy(alpha = a), radius = r * 0.6f, center = c)
    }
}

private fun DrawScope.drawFiligree() {
    val inset = 9.dp.toPx(); val w = size.width; val h = size.height
    drawRoundRect(
        brush = ExGold, topLeft = Offset(inset, inset), size = Size(w - inset * 2, h - inset * 2),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()), style = Stroke(width = 1.2.dp.toPx())
    )
    val s = 26.dp.toPx(); val sw = 1.6.dp.toPx()
    fun corner(cx: Float, cy: Float, dx: Float, dy: Float) {
        val p = Path().apply {
            moveTo(cx + dx * s, cy + dy * 2f)
            cubicTo(cx + dx * s * 0.5f, cy + dy * 2f, cx + dx * 2f, cy + dy * s * 0.5f, cx + dx * 2f, cy + dy * s)
            moveTo(cx + dx * s * 0.62f, cy + dy * 2f)
            cubicTo(cx + dx * s * 0.62f, cy + dy * s * 0.45f, cx + dx * s * 0.25f, cy + dy * s * 0.45f, cx + dx * s * 0.3f, cy + dy * s * 0.3f)
            moveTo(cx + dx * 2f, cy + dy * s * 0.62f)
            cubicTo(cx + dx * s * 0.45f, cy + dy * s * 0.62f, cx + dx * s * 0.45f, cy + dy * s * 0.25f, cx + dx * s * 0.3f, cy + dy * s * 0.3f)
        }
        drawPath(p, ExGold, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(Color(0xFFFFE08A), radius = 2.2.dp.toPx(), center = Offset(cx + dx * s * 0.3f, cy + dy * s * 0.3f))
    }
    val o = inset + 4.dp.toPx()
    corner(o, o, 1f, 1f); corner(w - o, o, -1f, 1f); corner(o, h - o, 1f, -1f); corner(w - o, h - o, -1f, -1f)
}

private fun DrawScope.drawMedallion(pulse: Float) {
    val c = center; val r = size.minDimension / 2f
    drawCircle(Brush.radialGradient(listOf(Color(0x88FFC107).copy(alpha = 0.55f * pulse), Color(0x00FFC107)), center = c, radius = r), radius = r, center = c)
    val ring = Path()
    val n = 12; val ro = r * 0.78f; val ri = r * 0.68f
    for (k in 0..(n * 8)) {
        val t = k / (n * 8f) * 2f * PI.toFloat()
        val rr = ri + (ro - ri) * (0.5f + 0.5f * cos(t * n))
        val x = c.x + rr * cos(t); val y = c.y + rr * sin(t)
        if (k == 0) ring.moveTo(x, y) else ring.lineTo(x, y)
    }
    ring.close()
    drawPath(ring, Brush.linearGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE8B23A), Color(0xFF8A5600)), Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r)))
    drawPath(ring, Color(0xFF6E4300), style = Stroke(width = 1.dp.toPx()))
    val jr = r * 0.6f
    drawCircle(Color(0xFF5A0010), radius = jr + 2.dp.toPx(), center = c)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFF6B78), Color(0xFFC2132B), Color(0xFF6E0010)), center = Offset(c.x - jr * 0.25f, c.y - jr * 0.35f), radius = jr * 1.2f), radius = jr, center = c)
    drawOval(Color(0x80FFFFFF), topLeft = Offset(c.x - jr * 0.55f, c.y - jr * 0.85f), size = Size(jr * 1.1f, jr * 0.6f))
    val th = jr * 1.0f; val tw = jr * 1.15f
    val tri = Path().apply {
        moveTo(c.x, c.y - th * 0.55f); lineTo(c.x + tw * 0.5f, c.y + th * 0.42f); lineTo(c.x - tw * 0.5f, c.y + th * 0.42f); close()
    }
    drawPath(tri, Brush.verticalGradient(listOf(Color(0xFFFFFBEA), Color(0xFFF6D27A)), startY = c.y - th * 0.55f, endY = c.y + th * 0.42f))
    drawPath(tri, Color(0xFFB57A08), style = Stroke(width = 1.4.dp.toPx(), join = StrokeJoin.Round))
    drawLine(Color(0xFF8A0E1C), Offset(c.x, c.y - th * 0.22f), Offset(c.x, c.y + th * 0.12f), strokeWidth = jr * 0.13f, cap = StrokeCap.Round)
    drawCircle(Color(0xFF8A0E1C), radius = jr * 0.075f, center = Offset(c.x, c.y + th * 0.28f))
}
