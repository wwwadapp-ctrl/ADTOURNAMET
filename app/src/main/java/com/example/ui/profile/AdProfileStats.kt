package com.example.ui.profile

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
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
import androidx.compose.ui.graphics.drawscope.rotate as drawScopeRotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.VerifiedUser
import com.example.R
import com.example.ui.home.adPressScale
import com.example.ui.wallet.WlImage
import com.example.ui.wallet.WlRays
import com.example.ui.wallet.WlFrame
import com.example.ui.wallet.WlText3D
import com.example.ui.wallet.WlTone

// Profile page artwork, part 2: section title, stat tiles, AI card, support rows. VISUAL ONLY.

/** Section title with a blinking red dot, optional gold subtitle. */
@Composable
fun PfSectionTitle(title: String, subtitle: String? = null) {
    val anim = rememberInfiniteTransition(label = "pfDot")
    val a by anim.animateFloat(0.35f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pfDotA")
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).graphicsLayer { alpha = a }.shadow(6.dp, CircleShape, spotColor = Color(0xFFFF2D45)).clip(CircleShape).background(Color(0xFFFF2D45)))
            Spacer(Modifier.width(9.dp))
            WlText3D(title, if (title.length > 20) 18.sp else 21.sp, WlTone.CREAM)
        }
        if (subtitle != null) {
            Text(subtitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C94C), modifier = Modifier.padding(start = 19.dp, top = 2.dp))
        }
    }
}

/** Small stat tile with a 3D picture sitting on top. */
@Composable
fun PfStatTile(@DrawableRes picture: Int, label: String, value: String, sub: String, color: Color, tone: WlTone, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(17.dp)
    Box(modifier = modifier.padding(top = 27.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, shape, ambientColor = color, spotColor = color)
                .clip(shape)
                .background(Brush.linearGradient(listOf(Color(0xFFFFF3C4), color, Color(0xFF5A3A08))))
                .padding(1.8.dp)
                .clip(RoundedCornerShape(15.5.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF232E96), Color(0xFF121860))))
                .padding(top = 32.dp, bottom = 9.dp, start = 3.dp, end = 3.dp)
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = color, maxLines = 1, softWrap = false)
            WlText3D(value, if (value.length > 5) 17.sp else 23.sp, tone)
            Text(sub, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xD9FFF3D8), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-27).dp).size(56.dp)) {
            WlRays(90.dp, color = color)
            WlImage(picture, 56.dp)
        }
    }
}

/** Wide stat tile: picture on the left (flipping coin when coin = true), texts on the right. */
@Composable
fun PfWideTile(@DrawableRes picture: Int, label: String, value: String, sub: String, color: Color, tone: WlTone, modifier: Modifier = Modifier, coin: Boolean = false) {
    val shape = RoundedCornerShape(17.dp)
    val anim = rememberInfiniteTransition(label = "pfWide")
    val flip by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing)), label = "pfFlip")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .shadow(8.dp, shape, ambientColor = color, spotColor = color)
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFFFFF3C4), color, Color(0xFF5A3A08))))
            .padding(1.8.dp)
            .clip(RoundedCornerShape(15.5.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF232E96), Color(0xFF121860))))
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
            WlRays(84.dp, color = color)
            if (coin) {
                Image(painterResource(picture), null, modifier = Modifier.size(48.dp).graphicsLayer { rotationY = flip; cameraDistance = 12f * density })
            } else {
                WlImage(picture, 52.dp)
            }
        }
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = color, maxLines = 1)
            WlText3D(value, if (value.length > 10) 13.sp else if (value.length > 7) 17.sp else 20.sp, tone, overflow = TextOverflow.Ellipsis)
            Text(sub, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xD9FFF3D8), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun Modifier.pfShine(sweep: Float, halfDp: Dp, alpha: Float) = drawWithContent {
    drawContent()
    val x = size.width * sweep
    val half = halfDp.toPx()
    drawScopeRotate(22f, pivot = Offset(x, size.height / 2f)) {
        drawRect(Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color.White.copy(alpha = alpha), Color(0x00FFFFFF)), startX = x - half, endX = x + half), topLeft = Offset(x - half, -size.height), size = Size(half * 2f, size.height * 3f))
    }
}

/** Glowing cyan AI support card with a LIVE dot. */
@Composable
fun PfAiCard(title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "pfAi")
    val glow by anim.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pfAiGlow")
    val sweep by anim.animateFloat(-0.3f, 2.2f, infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing)), label = "pfAiSweep")
    val press = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(20.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .adPressScale(press, 0.96f)
            .fillMaxWidth()
            .shadow((6f + 12f * glow).dp, shape, ambientColor = Color(0xFF22D3EE), spotColor = Color(0xFF22D3EE))
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFFD0F6FF), Color(0xFF22D3EE), Color(0xFF0B4A6A), Color(0xFF22D3EE))))
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
            .padding(2.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1E3AA0), Color(0xFF121860))))
            .pfShine(sweep, 20.dp, 0.25f)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        PfMedal(Icons.Filled.Bolt, Color(0xFF22D3EE), Color(0xFF0B4A6A), 50.dp, 26.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            WlText3D(title, if (title.length > 16) 15.sp else 18.sp, WlTone.CREAM, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFA5F3FC), maxLines = 2)
        }
        Spacer(Modifier.width(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x3322C55E)).border(1.dp, Color(0xFF4ADE80), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Box(Modifier.size(7.dp).graphicsLayer { alpha = glow }.clip(CircleShape).background(Color(0xFF4ADE80)))
            Spacer(Modifier.width(5.dp))
            Text("LIVE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFB8FFC8))
        }
    }
}

/** WhatsApp / Telegram row. Grey and not clickable when not available. */
@Composable
fun PfChannelRow(icon: ImageVector, light: Color, dark: Color, title: String, subtitle: String, available: Boolean, offlineText: String, onClick: () -> Unit) {
    val press = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(if (available) Modifier.adPressScale(press, 0.97f) else Modifier)
            .fillMaxWidth()
            .shadow(if (available) 8.dp else 3.dp, shape, ambientColor = light, spotColor = if (available) light else Color.Black)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF151B66), Color(0xFF0E1250))))
            .border(1.3.dp, if (available) light else Color(0x598C96FF), shape)
            .clickable(enabled = available, interactionSource = press, indication = LocalIndication.current, onClick = onClick)
            .graphicsLayer { alpha = if (available) 1f else 0.65f }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        PfMedal(icon, if (available) light else Color(0xFF5A6280), if (available) dark else Color(0xFF262B4E), 42.dp, 20.dp)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            WlText3D(title, if (title.length > 20) 13.sp else 15.sp, WlTone.CREAM, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C94C), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (available) {
            Text("›", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD45A))
        } else {
            Text(offlineText, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFE6EBFF), modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF3A4060)).padding(horizontal = 7.dp, vertical = 2.dp))
        }
    }
}

/** Fair play rules in a gold frame with a green shield. */
@Composable
fun PfFairPlayCard(title: String, text: String, modifier: Modifier = Modifier) {
    WlFrame(modifier = modifier, gold = true, pad = 16.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PfMedal(Icons.Filled.VerifiedUser, Color(0xFF4ADE80), Color(0xFF0B5A2A), 40.dp, 22.dp)
                Spacer(Modifier.width(10.dp))
                WlText3D(title, if (title.length > 22) 14.sp else 16.sp, WlTone.GOLD)
            }
            Spacer(Modifier.height(9.dp))
            Text(
                text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFF3D8), lineHeight = 19.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF0A0E3C), Color(0xFF101655))))
                    .border(1.2.dp, Color(0x80E8B84A), RoundedCornerShape(13.dp))
                    .padding(horizontal = 11.dp, vertical = 9.dp)
            )
        }
    }
}
