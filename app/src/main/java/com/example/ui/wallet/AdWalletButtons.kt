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

// Wallet pages artwork, part 4: action buttons and stat tiles. VISUAL ONLY: the caller gives texts and clicks.

private val WlBtnRing = listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4))

/** Big 3D action button. primary = orange-gold (add money); otherwise royal blue with a gold edge (withdraw). */
@Composable
fun WlActionButton(primary: Boolean, title: String, subtitle: String, @DrawableRes picture: Int, symbol: ImageVector, onClick: () -> Unit, tag: String, modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "wlBtn")
    val sweep by anim.animateFloat(-0.4f, 2.4f, infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing)), label = "wlBtnSweep")
    val press = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(20.dp)
    Box(modifier = modifier.adPressScale(press, 0.94f).padding(top = 14.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(98.dp)
                .shadow(10.dp, shape, ambientColor = if (primary) Color(0xFFFFB43C) else Color(0xFFFFC850), spotColor = if (primary) Color(0xFFFFB43C) else Color.Black)
                .clip(shape)
                .background(if (primary) Color(0xFF9A5A06) else Color(0xFF0A0E3C))
                .padding(bottom = 6.dp)
                .clip(shape)
                .background(Brush.linearGradient(WlBtnRing))
                .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
                .testTag(tag)
                .padding(2.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (primary) Brush.verticalGradient(listOf(Color(0xFFFFE27A), Color(0xFFFFB21E), Color(0xFFF58A0A)))
                    else Brush.linearGradient(listOf(Color(0xFF2A36A4), Color(0xFF141A66)))
                )
                .drawWithContent {
                    if (!primary) drawRoundRect(Color(0xBFE8B84A), topLeft = Offset(4.dp.toPx(), 4.dp.toPx()), size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()), cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(1.3.dp.toPx()))
                    drawContent()
                    val x = size.width * sweep
                    drawScopeRotate(22f, pivot = Offset(x, size.height / 2f)) {
                        drawRect(Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color.White.copy(alpha = if (primary) 0.6f else 0.14f), Color(0x00FFFFFF)), startX = x - 13.dp.toPx(), endX = x + 13.dp.toPx()), topLeft = Offset(x - 13.dp.toPx(), -size.height), size = Size(26.dp.toPx(), size.height * 3f))
                    }
                }
        ) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp).size(50.dp), contentAlignment = Alignment.Center) {
                if (!primary) WlRays(86.dp)
                WlImage(picture, 48.dp)
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 12.dp, bottom = 7.dp, end = 8.dp)) {
                if (primary) Text(title, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B1040), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                else WlText3D(title, 16.sp, WlTone.CREAM)
                Text(subtitle, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (primary) Color(0xFF5A2A00) else Color(0xFFF2C94C), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        // gold medal on the top-left corner
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(x = 12.dp, y = (-14).dp)
                .size(40.dp)
                .shadow(5.dp, CircleShape)
                .clip(CircleShape)
                .background(Brush.sweepGradient(WlBtnRing))
                .padding(4.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFFFF6C8), Color(0xFFFFC83A), Color(0xFFC98A10))))
        ) {
            Icon(symbol, contentDescription = null, tint = Color(0xFF1B2382), modifier = Modifier.size(22.dp))
        }
    }
}

/** Small glowing tile with a floating picture on top: label + value. */
@Composable
fun WlStatTile(label: String, value: String, @DrawableRes picture: Int, color: Color, tone: WlTone, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Box(modifier = modifier.padding(top = 30.dp), contentAlignment = Alignment.TopCenter) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, shape, ambientColor = color, spotColor = color)
                .clip(shape)
                .background(Color(0xFF0A0E3C))
                .padding(bottom = 5.dp)
                .clip(shape)
                .background(Brush.linearGradient(listOf(Color(0xFFFFF3C4), color, Color(0xFF8A5A08), color)))
                .padding(2.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF232E96), Color(0xFF121860))))
                .drawBehind {
                    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent), center = Offset(size.width / 2f, size.height), radius = size.width * 0.7f), size.width * 0.7f, Offset(size.width / 2f, size.height))
                    drawRoundRect(color.copy(alpha = 0.8f), topLeft = Offset(4.dp.toPx(), 4.dp.toPx()), size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()), cornerRadius = CornerRadius(12.dp.toPx()), style = Stroke(1.2.dp.toPx()))
                }
                .padding(start = 4.dp, end = 4.dp, top = 32.dp, bottom = 10.dp)
        ) {
            Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = color, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            WlText3D(value, if (value.length > 8) 14.sp else if (value.length > 6) 16.sp else 19.sp, tone)
        }
        Box(modifier = Modifier.offset(y = (-30).dp).size(60.dp), contentAlignment = Alignment.Center) {
            WlRays(96.dp)
            WlImage(picture, 58.dp)
        }
    }
}
