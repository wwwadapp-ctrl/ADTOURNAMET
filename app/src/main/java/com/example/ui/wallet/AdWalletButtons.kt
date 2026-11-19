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
