package com.example.ui.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.home.adPressScale
import com.example.ui.wallet.WlRays
import com.example.ui.wallet.WlText3D
import com.example.ui.wallet.WlTone

// Profile page artwork, part 1: medal, header, avatar halo, name, id, mobile. VISUAL ONLY.


internal val PfGoldRing = listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4))
internal val PfOrange = listOf(Color(0xFFFFE27A), Color(0xFFFFB21E), Color(0xFFF58A0A))

/** Round picture medal: gold ring, coloured glossy middle, white icon. */
@Composable
fun PfMedal(icon: ImageVector, light: Color, dark: Color, size: Dp = 40.dp, iconSize: Dp = 20.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .shadow(6.dp, CircleShape, ambientColor = light, spotColor = light)
            .clip(CircleShape)
            .background(Brush.sweepGradient(PfGoldRing))
            .padding(2.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(light, dark)))
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}

/** Gold round back button + big 3D title. */
@Composable
fun PfHeader(title: String, backLabel: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val press = remember { MutableInteractionSource() }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .adPressScale(press, 0.9f)
                .size(40.dp)
                .shadow(6.dp, CircleShape, ambientColor = Color(0xFFFFC850), spotColor = Color(0xFFFFC850))
                .clip(CircleShape)
                .background(Brush.sweepGradient(PfGoldRing))
                .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onBack)
                .testTag("profile_back_button")
                .padding(2.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF2E3CB0), Color(0xFF141A66))))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backLabel, tint = Color(0xFFFFE7A0), modifier = Modifier.size(20.dp))
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).padding(end = 40.dp)) {
            WlText3D(title, if (title.length > 18) 21.sp else 25.sp, WlTone.CREAM)
        }
    }
}

/** Big avatar: turning rays, glowing gold ring, turning dashed ring, orange camera button. */
@Composable
fun PfAvatarHalo(cameraLabel: String, onClick: () -> Unit, modifier: Modifier = Modifier, avatar: @Composable () -> Unit) {
    val anim = rememberInfiniteTransition(label = "pfHalo")
    val spin by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "pfSpin")
    val glow by anim.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "pfGlow")
    val press = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .adPressScale(press, 0.95f)
            .size(150.dp)
            .clickable(interactionSource = press, indication = null, onClick = onClick)
            .testTag("profile_avatar_badge")
    ) {
        WlRays(230.dp)
        Canvas(modifier = Modifier.size(146.dp).graphicsLayer { rotationZ = spin }) {
            drawCircle(Color(0x8CFFE7A0), size.minDimension / 2f - 1.dp.toPx(), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))))
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(128.dp)
                .shadow((10f + 14f * glow).dp, CircleShape, ambientColor = Color(0xFFFFC850), spotColor = Color(0xFFFFC850))
                .clip(CircleShape)
                .background(Brush.sweepGradient(PfGoldRing))
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color(0xFF0C1150))
                .padding(3.dp)
                .clip(CircleShape)
        ) { avatar() }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-12).dp, y = (-12).dp)
                .size(38.dp)
                .shadow(8.dp, CircleShape, ambientColor = Color(0xFFFFAA3C), spotColor = Color(0xFFFFAA3C))
                .clip(CircleShape)
                .background(Color(0xFF0C1150))
                .padding(2.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(PfOrange))
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = cameraLabel, tint = Color(0xFF3A1A00), modifier = Modifier.size(19.dp))
        }
    }
}

/** 3D gold name with the orange pencil button. */
@Composable
fun PfNameRow(name: String, editLabel: String, onEdit: () -> Unit) {
    val press = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.adPressScale(press, 0.96f).clickable(interactionSource = press, indication = null, onClick = onEdit)
    ) {
        WlText3D(name, if (name.length > 16) 20.sp else 26.sp, WlTone.GOLD, modifier = Modifier.weight(1f, fill = false), overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(8.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(28.dp).shadow(4.dp, CircleShape).clip(CircleShape).background(Brush.verticalGradient(PfOrange))
        ) {
            Icon(Icons.Filled.Edit, contentDescription = editLabel, tint = Color(0xFF3A1A00), modifier = Modifier.size(15.dp))
        }
    }
}

/** Dark pill "ID: xxx" with a gold copy chip. */
@Composable
fun PfIdPill(text: String, copyLabel: String, onClick: () -> Unit) {
    val press = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .adPressScale(press, 0.95f)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0E3C), Color(0xFF101655))))
            .border(1.4.dp, Color(0xCCE8B84A), RoundedCornerShape(18.dp))
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
            .testTag("copy_user_id_button")
            .padding(start = 14.dp, end = 5.dp, top = 5.dp, bottom = 5.dp)
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFE7A0), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC21E)))).padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = Color(0xFF3A1A00), modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(3.dp))
            Text(copyLabel, fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF3A1A00), maxLines = 1)
        }
    }
}

/** Mobile number row with the grey "locked" pill, and the notice under it. */
@Composable
fun PfMobileRow(number: String, lockedText: String, notice: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF0A0E3C), Color(0xFF101655))))
                .border(1.3.dp, Color(0x99E8B84A), RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            PfMedal(Icons.Filled.Phone, Color(0xFF4ADE80), Color(0xFF0B5A2A), 36.dp, 17.dp)
            Spacer(Modifier.width(10.dp))
            WlText3D(number, 16.sp, WlTone.CREAM, modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF4A5280), Color(0xFF262B4E))))
                    .border(1.dp, Color(0xFFAEB6D8), RoundedCornerShape(12.dp))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFE6EBFF), modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(lockedText, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFE6EBFF), maxLines = 1)
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(notice, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C94C), textAlign = TextAlign.Center)
    }
}
