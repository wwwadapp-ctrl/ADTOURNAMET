package com.example.ui.history

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as drawScopeRotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.home.AdGameFilterChip
import com.example.ui.home.adPressScale
import com.example.ui.wallet.WlFrame
import com.example.ui.wallet.WlImage
import com.example.ui.wallet.WlRays
import com.example.ui.wallet.WlText3D
import com.example.ui.wallet.WlTone

// History page artwork, part 1: summary, filter cards, empty message. VISUAL ONLY: the caller gives every text, number and click action.

enum class HsKind { WON, LOST, PENDING, CANCELLED }

/** One summary tile: 3D picture sitting on top, label and big 3D number. */
@Composable
private fun HsStatTile(@DrawableRes picture: Int, label: String, value: String, color: Color, tone: WlTone, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Box(modifier = modifier.padding(top = 24.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, shape, ambientColor = color, spotColor = color)
                .clip(shape)
                .background(Brush.linearGradient(listOf(Color(0xFFFFF3C4), color, Color(0xFF5A3A08))))
                .padding(1.6.dp)
                .clip(RoundedCornerShape(14.5.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF232E96), Color(0xFF121860))))
                .padding(top = 30.dp, bottom = 8.dp, start = 2.dp, end = 2.dp)
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black, color = color, maxLines = 1, softWrap = false)
            WlText3D(value, if (value.length > 4) 16.sp else 21.sp, tone)
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-24).dp).size(50.dp)) {
            WlRays(80.dp, color = color)
            WlImage(picture, 50.dp)
        }
    }
}

/** Gold ring that fills to the win rate. */
@Composable
private fun HsRateRing(label: String, rateText: String, fraction: Float, modifier: Modifier = Modifier) {
    val shown by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(1200, easing = FastOutSlowInEasing), label = "hsRate")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(78.dp).shadow(10.dp, CircleShape, ambientColor = Color(0xFFFFC850), spotColor = Color(0xFFFFC850))) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = 8.dp.toPx()
                drawCircle(Color(0xF00A0E3C), size.minDimension / 2f)
                drawArc(Color(0xFF0A0E3C), 0f, 360f, false, topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w), style = Stroke(w))
                drawArc(
                    Brush.sweepGradient(listOf(Color(0xFFFFF3C4), Color(0xFFFFD45A), Color(0xFFF08A00), Color(0xFFFFF3C4))),
                    -90f, 360f * shown, false, topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Round)
                )
                drawCircle(Color(0xFFE8B84A), size.minDimension / 2f - w - 1.dp.toPx(), style = Stroke(1.2.dp.toPx()))
            }
            WlText3D(rateText, if (rateText.length > 4) 15.sp else 18.sp, WlTone.GOLD)
        }
        Spacer(Modifier.height(5.dp))
        Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD45A), maxLines = 1, softWrap = false)
    }
}

/** Gold framed summary: total / won / lost tiles and the win-rate ring. */
@Composable
fun HsSummaryCard(
    totalLabel: String, totalText: String,
    wonLabel: String, wonText: String,
    lostLabel: String, lostText: String,
    rateLabel: String, rateText: String, rateFraction: Float,
    modifier: Modifier = Modifier,
) {
    WlFrame(modifier = modifier, gold = true, pad = 12.dp) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
            HsStatTile(R.drawable.wl_gamepad, totalLabel, totalText, Color(0xFF7DA8FF), WlTone.CREAM, Modifier.weight(1f))
            HsStatTile(R.drawable.wl_trophy, wonLabel, wonText, Color(0xFF4ADE80), WlTone.GOLD, Modifier.weight(1f))
            HsStatTile(R.drawable.mg_dice, lostLabel, lostText, Color(0xFFFF6B81), WlTone.RED, Modifier.weight(1f))
            HsRateRing(rateLabel, rateText, rateFraction, Modifier.weight(1.15f))
        }
    }
}

/** Game filter card (same as home) with an optional small gold "⚡ AUTO" badge on top. */
@Composable
fun HsFilterCard(label: String, selected: Boolean, iconKind: Int, onClick: () -> Unit, tag: String, modifier: Modifier = Modifier, badge: String? = null) {
    Box(modifier = modifier) {
        AdGameFilterChip(label = label, selected = selected, iconKind = iconKind, onClick = onClick, tag = tag, modifier = Modifier.fillMaxWidth().padding(top = if (badge != null) 7.dp else 0.dp))
        if (badge != null) {
            Text(
                badge, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF3A1A00), maxLines = 1, softWrap = false,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp)
                    .shadow(6.dp, RoundedCornerShape(8.dp), ambientColor = Color(0xFFFFC850), spotColor = Color(0xFFFFC850))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC21E))))
                    .border(1.dp, Color(0xFFFFF3C4), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp)
            )
        }
    }
}

/** Empty message in a gold frame: trophy on turning rays, 3D title, gold subtitle. */
@Composable
fun HsEmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    WlFrame(modifier = modifier, gold = true, pad = 16.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
                WlRays(140.dp)
                WlImage(R.drawable.wl_trophy, 86.dp)
            }
            Spacer(Modifier.height(8.dp))
            WlText3D(title, if (title.length > 22) 16.sp else 19.sp, WlTone.CREAM)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C94C), textAlign = TextAlign.Center)
        }
    }
}
