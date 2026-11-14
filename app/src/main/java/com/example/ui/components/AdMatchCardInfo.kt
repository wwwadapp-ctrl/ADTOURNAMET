package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate as drawScopeRotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

// Match card artwork, part 2: numbers panel, seat bar and result strip. VISUAL ONLY.

enum class AdMatchBannerKind { WIN, LOSE, DONE, CANCEL, PAUSE }

private fun DrawScope.bannerShine(x: Float, half: Float, alpha: Float) {
    drawScopeRotate(22f, pivot = Offset(x, size.height / 2f)) {
        drawRect(
            Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color.White.copy(alpha = alpha), Color(0x00FFFFFF)), startX = x - half, endX = x + half),
            topLeft = Offset(x - half, -size.height), size = Size(half * 2f, size.height * 3f)
        )
    }
}

/** Dark glass panel: entry fee | format | prize (prize in gold). */
@Composable
fun AdMatchStats(entryLabel: String, entry: String, formatLabel: String, format: String, prizeLabel: String, prize: String) {
    val divider = Brush.verticalGradient(listOf(Color(0x00E0A82A), Color(0xFFE0A82A), Color(0x00E0A82A)))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xBF080C32), Color(0x990E1450))))
            .border(1.dp, Color(0x73E0A82A), RoundedCornerShape(14.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        StatCell(Icons.Filled.ConfirmationNumber, Color(0xFFFFB4C0), entryLabel, Modifier.weight(1f)) {
            Text(entry, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1, softWrap = false)
        }
        Box(Modifier.width(1.dp).height(30.dp).background(divider))
        StatCell(Icons.Filled.Bolt, Color(0xFF7DE8FF), formatLabel, Modifier.weight(1f)) {
            Text(format, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false,
                style = TextStyle(color = Color(0xFF7DE8FF), shadow = Shadow(Color(0x9950DCFF), blurRadius = 10f)))
        }
        Box(Modifier.width(1.dp).height(30.dp).background(divider))
        StatCell(Icons.Filled.EmojiEvents, Color(0xFFFFD45A), prizeLabel, Modifier.weight(1f)) {
            Text(prize, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false,
                style = TextStyle(brush = McGoldText, shadow = Shadow(Color(0xFF5A2A00), Offset(0f, 2f), 1f)))
        }
    }
}

@Composable
private fun StatCell(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, label: String, modifier: Modifier, value: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(3.dp))
            Text(label, fontSize = 10.5.sp, color = Color(0xBFDCE1FF), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(1.dp))
        value()
    }
}

/** Seat circles (filled = a player sat) and a glossy bar with the seat text in the middle. */
@Composable
fun AdMatchSeatBar(joined: Int, maxSeats: Int, text: String, fill: AdSeatFill, modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "mcSeat")
    val sweep by anim.animateFloat(-0.2f, 1.8f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing)), label = "mcSeatSweep")
    val seats = maxSeats.coerceIn(1, 4)
    val ratio = when (fill) {
        AdSeatFill.EMPTY -> 0f
        AdSeatFill.GOLD -> (joined.toFloat() / maxSeats.coerceAtLeast(1)).coerceIn(0f, 1f)
        else -> 1f
    }
    val brush = when (fill) {
        AdSeatFill.RED -> Brush.horizontalGradient(listOf(Color(0xFFFB7185), Color(0xFFE11D48)))
        AdSeatFill.GREEN -> Brush.horizontalGradient(listOf(Color(0xFF6EF0A6), Color(0xFF16A34A)))
        else -> Brush.horizontalGradient(listOf(Color(0xFFFFE27A), Color(0xFFFFB21E)))
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        repeat(seats) { i ->
            val on = i < joined
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(22.dp)
                    .then(if (on) Modifier.shadow(6.dp, CircleShape, spotColor = Color(0xFFFFC850)) else Modifier)
                    .clip(CircleShape)
                    .background(if (on) Brush.radialGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08))) else Brush.linearGradient(listOf(Color(0xB30A0E3C), Color(0xB30A0E3C))))
                    .then(if (on) Modifier else Modifier.border(1.5.dp, Color(0x8CFFD45A), CircleShape))
            ) {
                Icon(if (on) Icons.Filled.Person else Icons.Filled.Add, contentDescription = null, tint = if (on) Color(0xFF3A2200) else Color(0x99FFE6AA), modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.width(4.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .height(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color(0xBF080C32))
                .border(1.dp, Color(0x73E0A82A), RoundedCornerShape(11.dp))
                .drawWithContent {
                    if (ratio > 0f) drawRect(brush, size = Size(size.width * ratio, size.height))
                    val x = size.width * sweep
                    drawScopeRotate(22f, pivot = Offset(x, size.height / 2f)) {
                        drawRect(
                            Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x8CFFFFFF), Color(0x00FFFFFF)), startX = x - 9.dp.toPx(), endX = x + 9.dp.toPx()),
                            topLeft = Offset(x - 9.dp.toPx(), -size.height), size = Size(18.dp.toPx(), size.height * 3f)
                        )
                    }
                    drawContent()
                }
        ) {
            Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false,
                style = TextStyle(color = Color.White, shadow = Shadow(Color(0xCC000000), Offset(0f, 1f), 3f)))
        }
    }
}

/** Result / cancelled / paused strip at the bottom of the card. */
@Composable
fun AdMatchBanner(kind: AdMatchBannerKind, text: String, linkText: String?, modifier: Modifier = Modifier) {
    val edge = when (kind) {
        AdMatchBannerKind.WIN -> Color(0xFFFFD45A)
        AdMatchBannerKind.DONE -> Color(0xFF4ADE80)
        AdMatchBannerKind.PAUSE -> Color(0xFFFBBF24)
        else -> Color(0xFFFB7185)
    }
    val icon = when (kind) {
        AdMatchBannerKind.WIN, AdMatchBannerKind.DONE -> Icons.Filled.EmojiEvents
        AdMatchBannerKind.LOSE -> Icons.Filled.Close
        AdMatchBannerKind.CANCEL -> Icons.Filled.Replay
        AdMatchBannerKind.PAUSE -> Icons.Filled.Pause
    }
    val anim = rememberInfiniteTransition(label = "mcBanner")
    val sweep by anim.animateFloat(-0.2f, 1.8f, infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing)), label = "mcBannerSweep")
    val bob by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "mcBannerBob")
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, spotColor = edge, ambientColor = edge)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(edge.copy(alpha = 0.3f), edge.copy(alpha = 0.08f))))
            .border(1.5.dp, edge, shape)
            .drawWithContent { drawContent(); bannerShine(size.width * sweep, 15.dp.toPx(), 0.4f) }
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {
        Icon(icon, contentDescription = null, tint = edge, modifier = Modifier.size(17.dp).graphicsLayer { translationY = -3f * bob * density })
        Spacer(Modifier.width(7.dp))
        Text(
            text, fontSize = 12.5.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f),
            style = if (kind == AdMatchBannerKind.WIN) TextStyle(brush = McGoldText, shadow = Shadow(Color(0xFF5A2A00), Offset(0f, 1.5f), 1f)) else TextStyle(color = edge)
        )
        if (linkText != null) {
            Spacer(Modifier.width(6.dp))
            Text(linkText, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFF3C4), maxLines = 1, softWrap = false)
        }
    }
}
