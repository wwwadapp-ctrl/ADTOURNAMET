package com.example.ui.notifications

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.home.adPressScale
import com.example.ui.wallet.WlFrame
import com.example.ui.wallet.WlImage
import com.example.ui.wallet.WlRays
import com.example.ui.wallet.WlText3D
import com.example.ui.wallet.WlTone

// Notifications page artwork, part 1: picture map, header, tabs, detail window. VISUAL ONLY.

internal val NfGoldRing = listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4))
internal val NfOrange = listOf(Color(0xFFFFE27A), Color(0xFFFFB21E), Color(0xFFF58A0A))

/** Picture + ring colour for a notification type (look only). Unknown types get the gold coin. */
fun nfLook(type: String): Pair<Int, Color> {
    val t = type.uppercase()
    val red = Color(0xFFFF6B81)
    return when {
        "PROOF" in t -> R.drawable.wl_hourglass to red
        "DEPOSIT" in t -> R.drawable.wl_moneybag to if ("REJECT" in t) red else Color(0xFF4ADE80)
        "WITHDRAW" in t -> R.drawable.wl_withdraw to if ("REJECT" in t) red else Color(0xFFFF9A3A)
        "WIN" in t || "PRIZE" in t -> R.drawable.wl_trophy to Color(0xFFFFD45A)
        "REFUND" in t || "TRANSFER" in t -> R.drawable.wl_transfer to Color(0xFFFF9A3A)
        "LOST" in t -> R.drawable.mg_dice to red
        "ROOM" in t -> R.drawable.wl_gamepad to Color(0xFF67E8F9)
        "REFER" in t || "BONUS" in t || "GIFT" in t -> R.drawable.wl_gift to red
        "LUDO" in t || "MATCH" in t || "JOIN" in t || "OPPONENT" in t -> R.drawable.mg_dice to Color(0xFF9B6BFF)
        else -> R.drawable.mg_coin to Color(0xFFFFD45A)
    }
}

/** Gold back button, 3D title + subtitle, and the "mark all read" button (hidden when markAllText = null). */
@Composable
fun NfHeader(title: String, subtitle: String, backLabel: String, onBack: () -> Unit, markAllText: String?, onMarkAll: () -> Unit) {
    val press = remember { MutableInteractionSource() }
    val press2 = remember { MutableInteractionSource() }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .adPressScale(press, 0.9f)
                .size(40.dp)
                .shadow(6.dp, CircleShape, ambientColor = Color(0xFFFFC850), spotColor = Color(0xFFFFC850))
                .clip(CircleShape)
                .background(Brush.sweepGradient(NfGoldRing))
                .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onBack)
                .testTag("notifications_back_button")
                .padding(2.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF2E3CB0), Color(0xFF141A66))))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backLabel, tint = Color(0xFFFFE7A0), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            WlText3D(title, if (title.length > 14) 19.sp else 23.sp, WlTone.CREAM, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF2C94C), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (markAllText != null) {
            Spacer(Modifier.width(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .adPressScale(press2, 0.92f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF0A0E3C), Color(0xFF101655))))
                    .border(1.4.dp, Color(0xFFE8B84A), RoundedCornerShape(14.dp))
                    .clickable(interactionSource = press2, indication = LocalIndication.current, onClick = onMarkAll)
                    .testTag("mark_all_read_button")
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.DoneAll, contentDescription = null, tint = Color(0xFFFFD45A), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text(markAllText, fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFE7A0), maxLines = 1)
            }
        }
    }
}

/** Raised tab: orange when selected, royal blue otherwise. badge = small red count (or null). */
@Composable
fun NfTab(text: String, selected: Boolean, onClick: () -> Unit, tag: String, modifier: Modifier = Modifier, badge: String? = null) {
    val press = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)
    Box(modifier = modifier.adPressScale(press, 0.94f).height(46.dp)) {
        Box(Modifier.fillMaxWidth().height(42.dp).offset(y = 4.dp).clip(shape).background(if (selected) Color(0xFF8A4A06) else Color(0xFF0A0E3C)))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .shadow(if (selected) 10.dp else 4.dp, shape, ambientColor = Color(0xFFFFAA3C), spotColor = if (selected) Color(0xFFFFAA3C) else Color.Black)
                .clip(shape)
                .background(Brush.verticalGradient(if (selected) listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A)) else listOf(Color(0xFFE0A82A), Color(0xFF8A5A08))))
                .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
                .testTag(tag)
                .padding(if (selected) 2.dp else 1.5.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(if (selected) NfOrange else listOf(Color(0xFF2E3CB0), Color(0xFF141A66))))
        ) {
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (selected) Color(0xFF3A1A00) else Color(0xFFFFF3C4), maxLines = 1)
            if (badge != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    badge, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1,
                    modifier = Modifier
                        .shadow(4.dp, CircleShape, spotColor = Color(0xFFFF5A70))
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Brush.verticalGradient(listOf(Color(0xFF5A2A00), Color(0xFF3A1A00))) else Brush.verticalGradient(listOf(Color(0xFFFF5A70), Color(0xFFE11D48))))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/** Detail window: big picture on rays, 3D title, category + date, message panel, orange close button. */
@Composable
fun NfDetailWindow(@DrawableRes picture: Int, ring: Color, title: String, category: String, date: String, message: String, closeText: String, onClose: () -> Unit) {
    val closePress = remember { MutableInteractionSource() }
    Dialog(onDismissRequest = onClose) {
        WlFrame(modifier = Modifier.testTag("notification_detail_dialog"), gold = true, pad = 18.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                    WlRays(140.dp, color = ring)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(70.dp)
                            .shadow(12.dp, CircleShape, ambientColor = Color(0xFFFFC850), spotColor = Color(0xFFFFC850))
                            .clip(CircleShape)
                            .background(Brush.sweepGradient(NfGoldRing))
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(Color(0xFF2C3AB0), Color(0xFF0C1150))))
                    ) { WlImage(picture, 54.dp) }
                }
                Spacer(Modifier.height(4.dp))
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFF3C4), textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category, fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = ring,
                        modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(ring.copy(alpha = 0.14f)).border(1.dp, ring, RoundedCornerShape(9.dp)).padding(horizontal = 8.dp, vertical = 1.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(date, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF2C94C))
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF0A0E3C), Color(0xFF101655))))
                        .border(1.2.dp, Color(0x80E8B84A), RoundedCornerShape(14.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 11.dp)
                ) {
                    Text(message, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFF3D8), lineHeight = 22.sp)
                }
                Spacer(Modifier.height(16.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .adPressScale(closePress, 0.95f)
                        .fillMaxWidth()
                        .height(46.dp)
                        .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFFFFAA3C), spotColor = Color(0xFFFFAA3C))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A))))
                        .clickable(interactionSource = closePress, indication = LocalIndication.current, onClick = onClose)
                        .testTag("notification_dialog_close_button")
                        .padding(2.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(NfOrange))
                ) {
                    Text(closeText, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF3A1A00))
                }
            }
        }
    }
}
