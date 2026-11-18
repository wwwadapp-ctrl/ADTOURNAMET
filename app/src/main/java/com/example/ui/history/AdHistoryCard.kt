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

// History page artwork, part 2: result-coloured match card and the cancel box. VISUAL ONLY: the caller gives every text, number and click action.

private fun hsColors(k: HsKind): List<Color> = when (k) {
    HsKind.WON -> listOf(Color(0xFFFFF8D8), Color(0xFFFFD45A), Color(0xFFB07A10))
    HsKind.LOST -> listOf(Color(0xFFFFC8D0), Color(0xFFE11D48), Color(0xFF5A0A18))
    HsKind.PENDING -> listOf(Color(0xFFFFF1C8), Color(0xFFF59E0B), Color(0xFF7A3E05))
    HsKind.CANCELLED -> listOf(Color(0xFFE6EBFF), Color(0xFF8088B0), Color(0xFF30365A))
}

/** Result-coloured history card. extra = optional slot under the numbers strip (cancellation box). */
@Composable
fun HsMatchCard(
    kind: HsKind, @DrawableRes picture: Int, resultText: String,
    title: String, dateLine: String,
    gameLabel: String, gameText: String,
    entryLabel: String, entryText: String,
    prizeLabel: String, prizeText: String, prizeTone: WlTone,
    onClick: () -> Unit, modifier: Modifier = Modifier,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val (light, mid, dark) = hsColors(kind)
    val anim = rememberInfiniteTransition(label = "hsCard")
    val sweep by anim.animateFloat(-0.3f, 2.4f, infiniteRepeatable(tween(4500, easing = FastOutSlowInEasing)), label = "hsSweep")
    val press = remember { MutableInteractionSource() }
    val outer = RoundedCornerShape(20.dp)
    val grey = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    Column(
        modifier = modifier
            .adPressScale(press, 0.97f)
            .fillMaxWidth()
            .shadow(10.dp, outer, ambientColor = Color(0xFF050532), spotColor = mid)
            .clip(outer)
            .background(Brush.linearGradient(listOf(light, mid, dark, mid, light)))
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
            .padding(2.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2B3AA8), Color(0xFF1E2780), Color(0xFF151B5E)), start = Offset(0f, 0f), end = Offset(300f, 900f)))
            .border(1.dp, mid.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .drawWithContent {
                drawContent()
                val x = size.width * sweep
                drawScopeRotate(22f, pivot = Offset(x, size.height / 2f)) {
                    drawRect(Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x1FFFFFFF), Color(0x00FFFFFF)), startX = x - 20.dp.toPx(), endX = x + 20.dp.toPx()), topLeft = Offset(x - 20.dp.toPx(), -size.height), size = Size(40.dp.toPx(), size.height * 3f))
                }
            }
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(60.dp)
                    .shadow(8.dp, CircleShape, ambientColor = mid, spotColor = mid)
                    .clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(light, mid, dark, mid, light)))
                    .padding(2.5.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF2C3AB0), Color(0xFF0C1150))))
            ) {
                if (kind == HsKind.WON) WlRays(92.dp)
                if (kind == HsKind.CANCELLED) {
                    androidx.compose.foundation.Image(painterResource(picture), null, colorFilter = grey, modifier = Modifier.size(46.dp).graphicsLayer { alpha = 0.85f })
                } else {
                    WlImage(picture, 48.dp, tilt = kind == HsKind.PENDING)
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    resultText, fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false,
                    color = if (kind == HsKind.WON || kind == HsKind.PENDING) Color(0xFF3A1A00) else Color(0xFFFFEEF2),
                    modifier = Modifier
                        .align(Alignment.End)
                        .shadow(6.dp, RoundedCornerShape(12.dp), ambientColor = mid, spotColor = mid)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.verticalGradient(when (kind) {
                            HsKind.WON -> listOf(Color(0xFFFFF3B0), Color(0xFFFFC21E))
                            HsKind.LOST -> listOf(Color(0xFFE11D48), Color(0xFF7A0A1A))
                            HsKind.PENDING -> listOf(Color(0xFFFFE58A), Color(0xFFF59E0B))
                            HsKind.CANCELLED -> listOf(Color(0xFF4A5280), Color(0xFF262B4E))
                        }))
                        .border(1.dp, light, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                )
                Spacer(Modifier.height(3.dp))
                WlText3D(title, if (title.length > 20) 14.sp else 16.sp, WlTone.CREAM, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(dateLine, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C94C), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF0A0E3C), Color(0xFF101655))))
                .border(1.2.dp, Color(0x99E8B84A), RoundedCornerShape(14.dp))
                .padding(vertical = 5.dp)
        ) {
            HsStripCell(gameLabel, gameText, WlTone.CREAM, Modifier.weight(1f))
            HsStripLine()
            HsStripCell(entryLabel, entryText, WlTone.GOLD, Modifier.weight(1f))
            HsStripLine()
            HsStripCell(prizeLabel, prizeText, prizeTone, Modifier.weight(1f))
        }
        extra()
    }
}

@Composable
private fun HsStripCell(label: String, value: String, tone: WlTone, modifier: Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF2C94C), maxLines = 1, softWrap = false)
        WlText3D(value, if (value.length > 8) 12.sp else 15.sp, tone)
    }
}

@Composable
private fun HsStripLine() {
    Box(Modifier.width(1.dp).height(30.dp).background(Brush.verticalGradient(listOf(Color(0x00E8B84A), Color(0xCCE8B84A), Color(0x00E8B84A)))))
}

/** Red box under a cancelled card. Tags are kept exactly as before. */
@Composable
fun HsCancelBox(title: String, reasonLabel: String, reason: String?, matchId: String) {
    Column(
        modifier = Modifier
            .padding(top = 9.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(Brush.verticalGradient(listOf(Color(0x38E11D48), Color(0x735A0A18))))
            .border(1.2.dp, Color(0xB3FB7185), RoundedCornerShape(13.dp))
            .testTag("history_cancellation_section_$matchId")
            .padding(horizontal = 11.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(0xFFE11D48))) {
                Text("!", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Spacer(Modifier.width(6.dp))
            Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFB0BC), modifier = Modifier.testTag("history_cancelled_title_$matchId"))
        }
        if (!reason.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Row {
                Text("$reasonLabel: ", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C94C))
                Text(reason, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFE0E5), modifier = Modifier.testTag("history_cancelled_reason_$matchId"))
            }
        }
    }
}
