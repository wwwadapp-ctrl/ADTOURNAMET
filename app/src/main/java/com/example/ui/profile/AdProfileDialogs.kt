package com.example.ui.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.home.adPressScale
import com.example.ui.wallet.WlFrame
import com.example.ui.wallet.WlRays
import com.example.ui.wallet.WlText3D
import com.example.ui.wallet.WlTone

// Profile page artwork, part 4: logout confirm window, notice window (also used by the home notice), message banner. VISUAL ONLY.

/** Big medal at the top of a window, on turning rays, with a soft pulse. */
@Composable
private fun PfDialogMedal(content: @Composable () -> Unit) {
    val anim = rememberInfiniteTransition(label = "pfDlg")
    val pulse by anim.animateFloat(0.94f, 1.06f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pfDlgPulse")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
        WlRays(130.dp)
        Box(Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse }) { content() }
    }
}

/** Logout confirm window: red medal, 3D title, message, blue cancel + red logout buttons. */
@Composable
fun PfLogoutConfirm(title: String, message: String, confirmText: String, cancelText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        WlFrame(gold = true, pad = 18.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                PfDialogMedal { PfMedal(Icons.Filled.Logout, Color(0xFFFF5A70), Color(0xFF7A0A1A), 62.dp, 30.dp) }
                Spacer(Modifier.height(6.dp))
                WlText3D(title, if (title.length > 18) 18.sp else 21.sp, WlTone.CREAM)
                Spacer(Modifier.height(6.dp))
                Text(message, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C94C), textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    PfChoiceButton(cancelText, false, onDismiss, "logout_cancel_button", Modifier.weight(1f))
                    PfLogoutButton(confirmText, onConfirm, "logout_confirm_button", Modifier.weight(1f))
                }
            }
        }
    }
}

/** Orange raised OK button (no tick). */
@Composable
private fun PfOkButton(text: String, onClick: () -> Unit, tag: String) {
    val press = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)
    Box(modifier = Modifier.adPressScale(press, 0.95f).fillMaxWidth().height(50.dp)) {
        Box(Modifier.fillMaxWidth().height(46.dp).offset(y = 4.dp).clip(shape).background(Color(0xFF8A4A06)))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .shadow(10.dp, shape, ambientColor = Color(0xFFFFAA3C), spotColor = Color(0xFFFFAA3C))
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A))))
                .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
                .testTag(tag)
                .padding(2.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(PfOrange))
        ) {
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF3A1A00), maxLines = 1)
        }
    }
}

/**
 * Notice window: megaphone medal on turning rays, 3D title, long text in a dark scrolling panel,
 * orange OK button. closeLabel != null shows a round ✕ at the top right.
 */
@Composable
fun PfNoticeWindow(title: String, description: String, closeText: String, onClose: () -> Unit, closeLabel: String? = null, okTag: String = "notice_close_button") {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth(0.92f)) {
            WlFrame(gold = true, pad = 18.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    PfDialogMedal { PfMedal(Icons.Filled.Campaign, Color(0xFFFF9A3A), Color(0xFF8A3A08), 62.dp, 30.dp) }
                    Spacer(Modifier.height(4.dp))
                    WlText3D(title, if (title.length > 18) 18.sp else 22.sp, WlTone.GOLD)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF0A0E3C), Color(0xFF101655))))
                            .border(1.2.dp, Color(0x80E8B84A), RoundedCornerShape(14.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 13.dp, vertical = 12.dp)
                    ) {
                        Text(description, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFF3D8), lineHeight = 22.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    PfOkButton(closeText, onClose, okTag)
                }
            }
            if (closeLabel != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(Color(0xFF2E3CB0), Color(0xFF141A66))))
                        .border(1.5.dp, Color(0xFFE8B84A), CircleShape)
                        .clickable(onClick = onClose)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = closeLabel, tint = Color(0xFFFFE7A0), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** Message banner: glass strip with gold edge, green tick medal, message and a round close button. */
@Composable
fun PfMessageBanner(message: String, closeLabel: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "pfBan")
    val glow by anim.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pfBanGlow")
    val shape = RoundedCornerShape(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .shadow((4f + 8f * glow).dp, shape, ambientColor = Color(0xFF4ADE80), spotColor = Color(0xFF4ADE80))
            .clip(shape)
            .background(Brush.linearGradient(PfGoldRing))
            .padding(1.5.dp)
            .clip(RoundedCornerShape(14.5.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF0E3A2A), Color(0xFF151B66), Color(0xFF0E1250))))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        PfMedal(Icons.Filled.Check, Color(0xFF4ADE80), Color(0xFF0B5A2A), 32.dp, 18.dp)
        Spacer(Modifier.width(10.dp))
        Text(message, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFF3D8), modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(0x33FFFFFF))
                .border(1.dp, Color(0x80E8B84A), CircleShape)
                .clickable(onClick = onClose)
                .testTag("profile_banner_close")
        ) {
            Icon(Icons.Filled.Close, contentDescription = closeLabel, tint = Color(0xFFFFE7A0), modifier = Modifier.size(15.dp))
        }
    }
}
