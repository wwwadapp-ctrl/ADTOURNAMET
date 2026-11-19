package com.example.ui.autocarrom

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AdGoldText
import com.example.ui.components.EsportsAvatar

// Auto Carrom HUD: player cards (like Auto Ludo), powder jar, striker slider. VISUAL ONLY.

private val CcGold = Brush.linearGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC107), Color(0xFFB07A00)))

data class CcPlayerUi(
    val name: String, val photo: String, val isMe: Boolean, val active: Boolean,
    val timer: Float, val pocketed: Int, val coin: Char, val hasQueen: Boolean, val strikes: Int, val turnText: String
)

/** Running light bulbs + glow around the active card (same idea as the Auto Ludo turn glow). */
@Composable
private fun CcMarquee(modifier: Modifier) {
    val t by rememberInfiniteTransition(label = "ccBulbs").animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "ccT")
    Canvas(modifier) {
        val ins = 1.dp.toPx(); val cr = 16.dp.toPx()
        val rr = RoundRect(ins, ins, size.width - ins, size.height - ins, CornerRadius(cr))
        val path = Path().apply { addRoundRect(rr) }
        val pulse = 0.5f + 0.5f * kotlin.math.sin(t * 6.28f)
        for ((w, a) in listOf(16f to 0.14f, 11f to 0.26f, 7f to 0.45f)) drawPath(path, Color(0xFFFFC400).copy(alpha = a + 0.08f * pulse), style = Stroke(w.dp.toPx() / 2.2f))
        drawPath(path, Color.White, style = Stroke(1.6.dp.toPx()))
        val pm = PathMeasure().apply { setPath(path, true) }
        val len = pm.length; val n = 34; val step = kotlin.math.floor(t * 8f).toInt()
        for (i in 0 until n) {
            val f = (((i.toFloat() / n + t * 0.12f) % 1f) + 1f) % 1f
            val p = pm.getPosition(f * len)
            val a = if ((i + step) % 2 == 0) 1f else 0.35f
            drawCircle(Color(0xFFFFDC5A).copy(alpha = 0.35f * a), 3.dp.toPx(), p)
            drawCircle(Color.White.copy(alpha = a), 1.6.dp.toPx(), p)
            drawCircle(Color(0xFFC98A00).copy(alpha = a), 1.6.dp.toPx(), p, style = Stroke(0.5.dp.toPx()))
        }
    }
}

@Composable
fun CcPlayerCard(p: CcPlayerUi, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Box(modifier.fillMaxWidth(), contentAlignment = if (p.isMe) Alignment.CenterStart else Alignment.CenterEnd) {
        Box(Modifier.fillMaxWidth(0.86f).height(88.dp)) {
            Box(
                Modifier.matchParentSize()
                    .shadow(if (p.active) 18.dp else 8.dp, shape, ambientColor = if (p.active) Color(0xFFFFB300) else Color.Black, spotColor = if (p.active) Color(0xFFFFA000) else Color.Black)
                    .background(Brush.verticalGradient(listOf(Color(0xFF22348C), Color(0xFF14205E), Color(0xFF0E1748))), shape)
                    .border(2.5.dp, if (p.active) CcGold else Brush.linearGradient(listOf(Color(0xFFE8C46A), Color(0xFFB9861C), Color(0xFF8A5E10))), shape)
            )
            Canvas(Modifier.matchParentSize()) {
                val i = 5.dp.toPx()
                drawRoundRect(Color(0xBFF0BE50), Offset(i, i), Size(size.width - 2 * i, size.height - 2 * i), CornerRadius(12.dp.toPx()), style = Stroke(1.dp.toPx()))
                drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent), i, i + 22.dp.toPx()), Offset(12.dp.toPx(), i), Size(size.width - 24.dp.toPx(), 22.dp.toPx()), CornerRadius(12.dp.toPx()))
            }
            Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (p.isMe) { CcAvatar(p); CcInfo(p, Modifier.weight(1f)); CcMedal(p) }
                else { CcMedal(p); CcInfo(p, Modifier.weight(1f)); CcAvatar(p) }
            }
            if (p.active) CcMarquee(Modifier.matchParentSize())
        }
    }
}

@Composable
private fun CcAvatar(p: CcPlayerUi) {
    Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 3.5.dp.toPx(); val r = size.minDimension / 2 - sw / 2
            drawCircle(Color.White.copy(alpha = 0.18f), r, style = Stroke(sw))
            if (p.active) drawArc(if (p.timer < 0.25f) SolidColor(Color(0xFFFF5A6A)) else CcGold, -90f, 360f * p.timer.coerceIn(0f, 1f), false,
                Offset(center.x - r, center.y - r), Size(r * 2, r * 2), style = Stroke(sw, cap = StrokeCap.Round))
            drawCircle(CcGold, r - sw * 1.15f, style = Stroke(2.6.dp.toPx()))
        }
        Box(Modifier.size(44.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF5A6BE8), Color(0xFF1A2685)))), contentAlignment = Alignment.Center) {
            EsportsAvatar(photoUrl = p.photo.ifBlank { null }, name = p.name, size = 44.dp, borderWidth = 0.dp)
        }
        if (p.active) Text("👑", fontSize = 18.sp, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-16).dp))
    }
}

@Composable
private fun CcInfo(p: CcPlayerUi, modifier: Modifier) {
    Column(modifier, horizontalAlignment = if (p.isMe) Alignment.Start else Alignment.End) {
        AdGoldText(text = p.name, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val pill = @Composable {
                Text(p.turnText, color = Color(0xFF1B1200), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                    modifier = Modifier.background(Brush.verticalGradient(listOf(Color(0xFFFFE082), Color(0xFFFFB300))), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp).alpha(if (p.active) 1f else 0f))
            }
            val dots = @Composable {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) { i -> Box(Modifier.size(8.dp).clip(CircleShape).background(if (i < p.strikes) Color(0xFFE53935) else Color(0xFF43A047)).border(0.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)) }
                }
            }
            if (p.isMe) { pill(); dots() } else { dots(); pill() }
        }
    }
}

@Composable
private fun CcMedal(p: CcPlayerUi) {
    Box(Modifier.size(66.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            drawCircle(Brush.radialGradient(listOf(Color(0xFF1D2B78), Color(0xFF0C1446))), r - 1.dp.toPx())
            drawCircle(CcGold, r - 1.2.dp.toPx(), style = Stroke(2.2.dp.toPx()))
            drawCircle(Color(0xCCFFE08A), r - 5.dp.toPx(), style = Stroke(0.9.dp.toPx()))
            drawCoin(p.coin, center.x, center.y - 9.dp.toPx(), 10.dp.toPx())
            if (p.hasQueen) drawCoin('q', center.x + 16.dp.toPx(), center.y - 16.dp.toPx(), 6.dp.toPx())
        }
        Text(CarromRules.bn("${p.pocketed}/${CarromRules.COINS}"), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.offset(y = 13.dp))
    }
}

/** Glass powder jar with gold lid and red label; [left] = uses left. */
@Composable
fun CcPowderJar(left: Int, on: Boolean, enabled: Boolean, label: String, onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "ccJar").animateFloat(1f, 1.08f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "ccJarP")
    Box(Modifier.size(64.dp).testTag("carrom_powder"), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(60.dp).graphicsLayer { val s = if (on) pulse else 1f; scaleX = s; scaleY = s; alpha = if (enabled) 1f else 0.45f }
                .shadow(if (on) 14.dp else 6.dp, CircleShape, ambientColor = Color(0xFFFFD45A), spotColor = Color(0xFFFFD45A))
                .clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF3A4AC8), Color(0xFF121A61))))
                .border(2.5.dp, if (on) SolidColor(Color(0xFFFFF3B0)) else CcGold, CircleShape)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(34.dp, 40.dp)) {
                val w = size.width; val h = size.height
                drawRoundRect(Brush.horizontalGradient(listOf(Color(0x8CC9D4FF), Color(0xD9FFFFFF), Color(0x80E6ECFF), Color(0x998FA0E0))), Offset(w * 0.06f, h * 0.28f), Size(w * 0.88f, h * 0.7f), CornerRadius(w * 0.16f))
                drawRoundRect(Color.White, Offset(w * 0.1f, h * 0.55f), Size(w * 0.8f, h * 0.4f), CornerRadius(w * 0.12f))
                drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF8A0A1E), Color(0xFFD8102A), Color(0xFF8A0A1E))), Offset(w * 0.12f, h * 0.6f), Size(w * 0.76f, h * 0.2f), CornerRadius(w * 0.05f))
                drawRoundRect(Color(0xFFFFD45A), Offset(w * 0.12f, h * 0.6f), Size(w * 0.76f, h * 0.2f), CornerRadius(w * 0.05f), style = Stroke(1.dp.toPx()))
                drawLine(Color.White.copy(alpha = 0.7f), Offset(w * 0.2f, h * 0.34f), Offset(w * 0.2f, h * 0.9f), 2.dp.toPx(), StrokeCap.Round)
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFF6C8), Color(0xFFFFC21E), Color(0xFF8A5A08))), Offset(0f, h * 0.1f), Size(w, h * 0.2f), CornerRadius(w * 0.08f))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFF6C8), Color(0xFFFFC21E), Color(0xFF8A5A08))), Offset(w * 0.22f, 0f), Size(w * 0.56f, h * 0.11f), CornerRadius(w * 0.06f))
            }
            Text(label, color = Color(0xFFFFF3C4), fontSize = 6.sp, fontWeight = FontWeight.Black, modifier = Modifier.offset(y = 12.dp))
        }
        Box(Modifier.align(Alignment.TopEnd).size(22.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC21E)))), contentAlignment = Alignment.Center) {
            Text(CarromRules.bn(left.toString()), color = Color(0xFF3A1A00), fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

/** Gold knob slider for the striker position ([value] 0..1). */
@Composable
fun CcSlider(value: Float, enabled: Boolean, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.height(44.dp).fillMaxWidth()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(onDragStart = { onChange((it.x / size.width).coerceIn(0f, 1f)) }) { ch, _ -> ch.consume(); onChange((ch.position.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(enabled) { if (enabled) detectTapGestures { onChange((it.x / size.width).coerceIn(0f, 1f)) } }
            .testTag("carrom_slider"),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height / 2; val th = 12.dp.toPx()
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF0B1040), Color(0xFF26369A)), y - th / 2, y + th / 2), Offset(0f, y - th / 2), Size(size.width, th), CornerRadius(th / 2))
            drawRoundRect(Color(0xB3FFE7A0), Offset(0f, y - th / 2), Size(size.width, th), CornerRadius(th / 2), style = Stroke(1.5.dp.toPx()))
            val kx = 22.dp.toPx() + value * (size.width - 44.dp.toPx()); val kr = 21.dp.toPx()
            drawCircle(Color(0xFF8A4A06), kr, Offset(kx, y + 3.dp.toPx()))
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF6C8), Color(0xFFFFC21E), Color(0xFFC98A10)), Offset(kx - kr * 0.25f, y - kr * 0.35f), kr * 1.2f), kr, Offset(kx, y))
            val a = Path().apply { moveTo(kx - 3.dp.toPx(), y - 6.dp.toPx()); lineTo(kx - 10.dp.toPx(), y); lineTo(kx - 3.dp.toPx(), y + 6.dp.toPx()); close()
                moveTo(kx + 3.dp.toPx(), y - 6.dp.toPx()); lineTo(kx + 10.dp.toPx(), y); lineTo(kx + 3.dp.toPx(), y + 6.dp.toPx()); close() }
            drawPath(a, Color(0xFF3A1A00))
        }
    }
}
