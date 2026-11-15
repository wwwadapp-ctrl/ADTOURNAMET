package com.example.ui.wallet

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as drawScopeRotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.home.adPressScale

// Wallet pages artwork, part 3: page title, balance cards, action buttons, stat tiles.
// VISUAL ONLY: the caller gives every text and click action.

private val WlGoldRing = listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4))

/** Big centred page title with a gold subtitle. */
@Composable
fun WlPageTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxWidth()) {
        WlText3D(title, if (title.length > 16) 26.sp else 31.sp, WlTone.CREAM)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, textAlign = TextAlign.Center, maxLines = 2,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF2C94C), shadow = Shadow(Color(0xFF5A3200), Offset(0f, 1.5f), 0f)))
    }
}

/** Gold ring with turning rays and the flipping gold coin. */
@Composable
fun WlCoinMedal(ringSize: Int, modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "wlCoin")
    val flip by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing)), label = "wlFlip")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(ringSize.dp)
            .shadow(10.dp, CircleShape, ambientColor = Color(0xFFFFC850), spotColor = Color(0xFFFFC850))
            .clip(CircleShape)
            .background(Brush.sweepGradient(WlGoldRing))
            .padding(3.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF2C3AB0), Color(0xFF0C1150))))
            .border(1.5.dp, Color(0x80E0A82A), CircleShape)
    ) {
        WlRays((ringSize * 1.3f).dp)
        Image(
            painter = painterResource(id = R.drawable.mg_coin),
            contentDescription = null,
            modifier = Modifier.size((ringSize * 0.76f).dp).graphicsLayer { rotationY = flip; cameraDistance = 12f * density }
        )
    }
}

/** Main balance card. amountText = null shows a loading spinner. pendingText = null hides the pending strip. */
@Composable
fun WlMainBalanceCard(label: String, amountText: String?, pendingText: String?, modifier: Modifier = Modifier) {
    WlFrame(modifier = modifier, gold = true, pad = 16.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 6.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    WlPill(label, Color(0xFF8CFF4A), listOf(Color(0xFF0E3A2A), Color(0xFF07241A)), Color(0xFF4ADE80), Color(0xFFB8FFC8))
                    Spacer(Modifier.height(10.dp))
                    if (amountText == null) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Color(0xFFFFD45A), strokeWidth = 2.5.dp)
                    } else {
                        WlText3D(amountText, if (amountText.length > 12) 24.sp else if (amountText.length > 9) 30.sp else 38.sp, WlTone.GOLD)
                    }
                }
                WlCoinMedal(88)
            }
            if (pendingText != null) {
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF0A0E3C), Color(0xFF101655))))
                        .border(1.4.dp, Color(0xB3E8B84A), RoundedCornerShape(22.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    WlImage(R.drawable.wl_hourglass, 28.dp, tilt = true)
                    Spacer(Modifier.width(8.dp))
                    Text(pendingText, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFF3C4), maxLines = 2)
                }
            }
        }
    }
}

/** Bonus card with the gift box. amountText = null shows a loading spinner. */
@Composable
fun WlBonusCard(label: String, amountText: String?, hint: String, modifier: Modifier = Modifier) {
    WlFrame(modifier = modifier, gold = false, pad = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 6.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                WlPill(label, Color(0xFFC98A10), listOf(Color(0xFFFFE58A), Color(0xFFE0A82A)), Color(0xFFFFF3C4), Color(0xFF3A1A00))
                Spacer(Modifier.height(10.dp))
                if (amountText == null) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFC8D2FF), strokeWidth = 2.5.dp)
                } else {
                    WlText3D(amountText, if (amountText.length > 10) 24.sp else 32.sp, WlTone.CREAM)
                }
                Spacer(Modifier.height(8.dp))
                Text(hint, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xE6EBEEFF))
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(78.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = Color(0xFFFFC850), spotColor = Color(0xFFFFC850))
                    .background(Brush.radialGradient(listOf(Color(0xFF3A48C0), Color(0xFF141A66))), RoundedCornerShape(20.dp))
                    .border(2.dp, Color(0xFFE8B84A), RoundedCornerShape(20.dp))
                    .padding(4.dp)
                    .border(1.2.dp, Color(0xB3E8B84A), RoundedCornerShape(15.dp))
            ) {
                WlRays(120.dp, color = Color(0xFFFFA0A0))
                WlImage(R.drawable.wl_gift, 74.dp, modifier = Modifier.requiredSize(74.dp).offset(y = (-6).dp))
            }
        }
    }
}
