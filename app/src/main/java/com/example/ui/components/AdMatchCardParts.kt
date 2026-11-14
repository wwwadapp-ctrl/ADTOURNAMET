package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate as drawScopeRotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.home.adPressScale

// Match card artwork, part 3: premium clock and buttons. VISUAL ONLY.

enum class AdMatchButtonKind { JOINED, FULL, UPCOMING, LIVE }

private fun DrawScope.shine(x: Float, half: Float, alpha: Float) {
    drawScopeRotate(22f, pivot = Offset(x, size.height / 2f)) {
        drawRect(
            Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color.White.copy(alpha = alpha), Color(0x00FFFFFF)), startX = x - half, endX = x + half),
            topLeft = Offset(x - half, -size.height), size = Size(half * 2f, size.height * 3f)
        )
    }
}

/** Gold framed clock: real clock face with a moving second hand, start time, and the countdown digits in dark boxes. */
@Composable
fun AdMatchClock(timeText: String, leftLabel: String, countdown: String, expired: Boolean, secondAngle: Float, modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "mcClock")
    val blink by anim.animateFloat(1f, 0.2f, infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse), label = "mcBlink")
    val sweep by anim.animateFloat(-0.3f, 2.2f, infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing)), label = "mcClockSweep")
    val col = if (expired) Color(0xFFFF6B81) else Color(0xFFFFE27A)
    val edge = if (expired) Brush.linearGradient(listOf(Color(0xFFFFC8D0), Color(0xFFE11D48), Color(0xFF5A0A18), Color(0xFFE11D48)))
    else Brush.linearGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF6B4A10), Color(0xFFE0A82A)))
    val shape = RoundedCornerShape(15.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .testTag("match_time_hud_capsule")
            .shadow(8.dp, shape, spotColor = if (expired) Color(0xFFE11D48) else Color(0xFFFFC850))
            .clip(shape)
            .background(edge)
            .padding(1.4.dp)
            .clip(RoundedCornerShape(13.6.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1E2A8C), Color(0xFF0E1450))))
            .drawWithContent { drawContent(); shine(size.width * sweep, 8.dp.toPx(), 0.35f) }
            .padding(start = 5.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Canvas(modifier = Modifier.size(30.dp)) {
            val r = size.minDimension / 2f
            drawCircle(Color(if (expired) 0x66FF5A6E else 0x66FFC85A), r)
            drawCircle(Brush.sweepGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4))), r)
            drawCircle(Brush.radialGradient(listOf(Color(0xFF26338F), Color(0xFF070B2E)), center = Offset(r * 0.8f, r * 0.7f), radius = r), r - 2.5.dp.toPx())
            for (i in 0 until 12) {
                drawScopeRotate(i * 30f) {
                    val big = i % 3 == 0
                    drawLine(if (big) Color(0xFFFFE7A0) else Color(0x80FFE7A0), Offset(center.x, 4.dp.toPx()), Offset(center.x, (if (big) 7.5f else 5.5f) * density), strokeWidth = (if (big) 1.5f else 1f) * density)
                }
            }
            drawScopeRotate(300f) { drawLine(Color(0xFFFFE7A0), center, Offset(center.x, center.y - 7.dp.toPx()), 2.dp.toPx(), StrokeCap.Round) }
            drawScopeRotate(secondAngle) { drawLine(col, center, Offset(center.x, center.y - 10.dp.toPx()), 1.dp.toPx(), StrokeCap.Round) }
            drawCircle(col, 2.dp.toPx())
        }
        Spacer(Modifier.width(7.dp))
        Column {
            Text(timeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xCCFFF0C8), maxLines = 1, softWrap = false)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(leftLabel, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = col, maxLines = 1, softWrap = false)
                Spacer(Modifier.width(4.dp))
                val parts = countdown.split(":")
                parts.forEachIndexed { i, p ->
                    Text(
                        p, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false,
                        style = TextStyle(color = col, shadow = Shadow(col, blurRadius = 8f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF05081E), Color(0xFF10164A))))
                            .border(1.dp, Color(0x40FFD45A), RoundedCornerShape(4.dp))
                            .padding(horizontal = 3.dp)
                    )
                    if (i < parts.size - 1) Text(":", fontSize = 13.sp, fontWeight = FontWeight.Black, color = col.copy(alpha = blink), modifier = Modifier.padding(horizontal = 1.dp))
                }
            }
        }
    }
}

/** Glowing orange "জয়েন করুন" button with a shine and press bounce. */
@Composable
fun AdMatchJoinButton(text: String, onClick: () -> Unit, tag: String, modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "mcJoin")
    val glow by anim.animateFloat(0.35f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "mcJoinGlow")
    val sweep by anim.animateFloat(-0.3f, 2.2f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing)), label = "mcJoinSweep")
    val press = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .adPressScale(press, 0.92f)
            .height(42.dp)
            .shadow((6f + 10f * glow).dp, shape, spotColor = Color(0xFFFFAA3C), ambientColor = Color(0xFFFFAA3C))
            .clip(shape)
            .background(Color(0xFF8A4A06))
            .padding(bottom = 4.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFE58A), Color(0xFFFFB21E), Color(0xFFF07A10))))
            .drawWithContent { drawContent(); shine(size.width * sweep, 11.dp.toPx(), 0.75f) }
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 18.dp)
    ) {
        Text("$text ›", fontSize = 14.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF3A1A00), maxLines = 1, softWrap = false)
    }
}

/** Glass state button: joined (green ✓), seat full (red 🔒), upcoming (amber), live / code ready (cyan, clickable). */
@Composable
fun AdMatchStateButton(text: String, kind: AdMatchButtonKind, tag: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    data class St(val top: Color, val bottom: Color, val edge: Color, val text: Color, val icon: ImageVector, val iconBg: Color, val iconTint: Color)
    val st = when (kind) {
        AdMatchButtonKind.JOINED -> St(Color(0x5922C55E), Color(0x8C166534), Color(0xFF4ADE80), Color(0xFFC8FFE0), Icons.Filled.Check, Color(0xFF22C55E), Color(0xFF04210F))
        AdMatchButtonKind.FULL -> St(Color(0x4DE11D48), Color(0x995A0A18), Color(0xFFFB7185), Color(0xFFFFD5DC), Icons.Filled.Lock, Color(0xFFE11D48), Color.White)
        AdMatchButtonKind.UPCOMING -> St(Color(0x4DF59E0B), Color(0x8C783C05), Color(0xFFFBBF24), Color(0xFFFFF1C8), Icons.Filled.HourglassTop, Color(0xFFF59E0B), Color(0xFF3A1A00))
        AdMatchButtonKind.LIVE -> St(Color(0x5922D3EE), Color(0xA60E466E), Color(0xFF67E8F9), Color(0xFFE0FBFF), Icons.Filled.Check, Color(0xFF22D3EE), Color(0xFF03222E))
    }
    val anim = rememberInfiniteTransition(label = "mcState")
    val pulse by anim.animateFloat(0.3f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "mcStatePulse")
    val live = kind == AdMatchButtonKind.LIVE
    val press = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .then(if (live) Modifier.adPressScale(press, 0.92f) else Modifier)
            .height(42.dp)
            .shadow((if (live) 4f + 12f * pulse else 8f).dp, shape, spotColor = st.edge, ambientColor = st.edge)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(st.top, st.bottom)))
            .border(1.5.dp, st.edge, shape)
            .then(if (live && onClick != null) Modifier.clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick) else Modifier)
            .testTag(tag)
            .padding(horizontal = 14.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(19.dp).graphicsLayer { alpha = if (live) 0.4f + 0.6f * pulse else 1f }.clip(CircleShape).background(st.iconBg)) {
            if (live) Box(Modifier.size(7.dp).clip(CircleShape).background(st.iconTint))
            else Icon(st.icon, contentDescription = null, tint = st.iconTint, modifier = Modifier.size(12.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Black, color = st.text, maxLines = 1, softWrap = false)
    }
}
