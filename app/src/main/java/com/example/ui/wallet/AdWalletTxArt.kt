package com.example.ui.wallet

import androidx.annotation.DrawableRes
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.home.adPressScale

// Wallet pages artwork, part 5: transaction rows, section header, filter chip, empty state.
// VISUAL ONLY: the caller gives every text, amount and click action.

/** Picture and ring colour for a transaction. iconKind comes from buildTxDisplay; type is the raw transaction type. */
fun wlTxLook(iconKind: String, type: String): Pair<Int, Color> = when {
    type.uppercase().contains("BONUS") -> R.drawable.wl_gift to Color(0xFFFF6B81)
    iconKind == "withdraw" -> R.drawable.wl_withdraw to Color(0xFFFFB03A)
    iconKind == "deposit" -> R.drawable.wl_moneybag to Color(0xFF4ADE80)
    iconKind == "prize" -> R.drawable.wl_trophy to Color(0xFFFFD45A)
    iconKind == "refund" -> R.drawable.wl_transfer to Color(0xFF4ADE80)
    iconKind == "game" -> R.drawable.wl_transfer to Color(0xFFFF9A3A)
    iconKind == "admin" -> R.drawable.mg_coin to Color(0xFF7DA8FF)
    else -> R.drawable.wl_gamepad to Color(0xFF9B6BFF)
}

/** Status pill. kind: 0 = done (gold), 1 = pending (blue), 2 = rejected / cancelled (red). */
@Composable
fun WlStatusPill(text: String, kind: Int) {
    val bg = when (kind) {
        1 -> listOf(Color(0xFFBFE0FF), Color(0xFF4A9BFF))
        2 -> listOf(Color(0xFFFFC0C8), Color(0xFFFF4D62))
        else -> listOf(Color(0xFFFFF3B0), Color(0xFFFFC21E))
    }
    Text(
        text, fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (kind == 2) Color(0xFF3A0008) else if (kind == 1) Color(0xFF06204A) else Color(0xFF3A1A00), maxLines = 1, softWrap = false,
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(11.dp), ambientColor = bg[1], spotColor = bg[1])
            .clip(RoundedCornerShape(11.dp))
            .background(Brush.verticalGradient(bg))
            .border(1.dp, Color(0xCCFFFFFF), RoundedCornerShape(11.dp))
            .padding(horizontal = 9.dp, vertical = 1.dp)
    )
}

/**
 * One transaction: round picture, title + status, description, date, 3D amount.
 * refText != null adds the reference strip with a copy button under it.
 */
@Composable
fun WlTxRow(
    @DrawableRes picture: Int, ring: Color, title: String, statusText: String, statusKind: Int,
    description: String, date: String, amountText: String, amountTone: WlTone,
    modifier: Modifier = Modifier,
    refText: String? = null, copyText: String = "", copyTag: String = "", onCopy: () -> Unit = {},
) {
    val topShape = if (refText == null) RoundedCornerShape(20.dp) else RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    Column(modifier = modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color.Black)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(topShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF151B66), Color(0xFF0E1250))))
                .border(1.2.dp, Color(0x598C96FF), topShape)
                .padding(horizontal = 11.dp, vertical = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .shadow(6.dp, CircleShape, ambientColor = ring, spotColor = ring)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF2C2F7A), Color(0xFF0C0F3C))))
                    .border(2.dp, ring, CircleShape)
                    .padding(3.5.dp)
                    .border(1.dp, Color(0x99E8B84A), CircleShape)
            ) {
                WlImage(picture, 40.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WlText3D(title, 14.5.sp, WlTone.GOLD, modifier = Modifier.weight(1f, fill = false), overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(6.dp))
                    WlStatusPill(statusText, statusKind)
                }
                if (description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(description, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFF3D8), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (date.isNotBlank()) {
                    Text(date, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C94C), maxLines = 1)
                }
            }
            Spacer(Modifier.width(6.dp))
            WlText3D(amountText, if (amountText.length > 11) 12.sp else 14.5.sp, amountTone)
        }
        if (refText != null) {
            val bottom = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            val press = remember { MutableInteractionSource() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(bottom)
                    .background(Brush.verticalGradient(listOf(Color(0xFF0A0E3C), Color(0xFF0E1250))))
                    .border(1.2.dp, Color(0x598C96FF), bottom)
                    .padding(horizontal = 13.dp, vertical = 6.dp)
            ) {
                Text(refText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C94C), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .adPressScale(press, 0.9f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC21E))))
                        .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onCopy)
                        .testTag(copyTag)
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = copyText, tint = Color(0xFF3A1A00), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(copyText, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF3A1A00), maxLines = 1)
                }
            }
        }
    }
}

/** Section heading with a blinking red dot and a "view all" pill. */
@Composable
fun WlSectionHeader(title: String, viewAllText: String, onViewAll: () -> Unit, modifier: Modifier = Modifier) {
    val press = remember { MutableInteractionSource() }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Box(Modifier.size(10.dp).shadow(6.dp, CircleShape, ambientColor = Color(0xFFFF2D45), spotColor = Color(0xFFFF2D45)).clip(CircleShape).background(Color(0xFFFF2D45)))
        Spacer(Modifier.width(9.dp))
        WlText3D(title, if (title.length > 18) 18.sp else 22.sp, WlTone.CREAM, modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(8.dp))
        Text(
            "$viewAllText ›", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFF3C4), maxLines = 1, softWrap = false,
            modifier = Modifier
                .adPressScale(press, 0.92f)
                .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = Color(0xFFFFC850), spotColor = Color(0xFFFFC850))
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A))))
                .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onViewAll)
                .padding(2.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF232E96), Color(0xFF10165A))))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

/** Filter chip: selected = orange-gold 3D, otherwise royal blue with a gold edge. */
@Composable
fun WlFilterChip(text: String, selected: Boolean, onClick: () -> Unit, tag: String) {
    val press = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(20.dp)
    Text(
        text, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.ExtraBold, color = if (selected) Color(0xFF3A1A00) else Color(0xFFFFF3C4), maxLines = 1, softWrap = false,
        modifier = Modifier
            .adPressScale(press, 0.9f)
            .clip(shape)
            .background(if (selected) Color(0xFF8A4A06) else Color(0xFF0A0E3C))
            .padding(bottom = 4.dp)
            .clip(shape)
            .background(Brush.verticalGradient(if (selected) listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A)) else listOf(Color(0xFFE0A82A), Color(0xFF8A5A08))))
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
            .testTag(tag)
            .padding(if (selected) 2.dp else 1.5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(if (selected) listOf(Color(0xFFFFE27A), Color(0xFFFFB21E), Color(0xFFF58A0A)) else listOf(Color(0xFF2A36A4), Color(0xFF141A66))))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

/** Empty list message: tilting hourglass on rays, title and description. */
@Composable
fun WlEmptyState(title: String, description: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Box(modifier = Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            WlRays(130.dp)
            WlImage(R.drawable.wl_hourglass, 76.dp, tilt = true)
        }
        Spacer(Modifier.height(8.dp))
        WlText3D(title, if (title.length > 26) 14.sp else 16.sp, WlTone.CREAM)
        Spacer(Modifier.height(4.dp))
        Text(description, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C94C), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))
    }
}

/** Small picture + label + value, used three in a row inside the history summary card. */
@Composable
fun WlMiniStat(@DrawableRes picture: Int, label: String, value: String, color: Color, tone: WlTone, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = modifier) {
        WlImage(picture, 28.dp)
        Spacer(Modifier.width(4.dp))
        Column {
            Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1, softWrap = false)
            WlText3D(value, if (value.length > 7) 12.sp else 14.sp, tone)
        }
    }
}
