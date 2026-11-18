package com.example.ui.notifications

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.drawscope.rotate as drawScopeRotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.home.adPressScale
import com.example.ui.wallet.WlImage
import com.example.ui.wallet.WlText3D
import com.example.ui.wallet.WlTone

// Notifications page artwork, part 2: notification card. VISUAL ONLY.

/** One notification. Unread: glowing gold frame, red dot, red "new" pill, moving shine. Read: calm blue. */
@Composable
fun NfCard(
    @DrawableRes picture: Int, ring: Color, title: String, message: String, unread: Boolean, newText: String,
    category: String, time: String, onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    val anim = rememberInfiniteTransition(label = "nfCard")
    val sweep by anim.animateFloat(-0.3f, 2.4f, infiniteRepeatable(tween(4500, easing = FastOutSlowInEasing)), label = "nfSweep")
    val blink by anim.animateFloat(0.45f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "nfBlink")
    val press = remember { MutableInteractionSource() }
    val outer = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .adPressScale(press, 0.97f)
            .fillMaxWidth()
            .shadow(if (unread) 10.dp else 5.dp, outer, ambientColor = Color(0xFF050532), spotColor = if (unread) Color(0xFFFFC850) else Color.Black)
            .clip(outer)
            .background(
                if (unread) Brush.linearGradient(NfGoldRing)
                else Brush.linearGradient(listOf(Color(0xFF6A78C8), Color(0xFF2A3488)))
            )
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
            .padding(if (unread) 1.8.dp else 1.2.dp)
            .clip(RoundedCornerShape(16.5.dp))
            .background(
                if (unread) Brush.linearGradient(listOf(Color(0xFF2A36A4), Color(0xFF1B2382), Color(0xFF131A66)), start = Offset(0f, 0f), end = Offset(300f, 900f))
                else Brush.verticalGradient(listOf(Color(0xFF151B66), Color(0xFF0E1250)))
            )
            .drawWithContent {
                drawContent()
                if (unread) {
                    val x = size.width * sweep
                    drawScopeRotate(22f, pivot = Offset(x, size.height / 2f)) {
                        drawRect(Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x1FFFFFFF), Color(0x00FFFFFF)), startX = x - 20.dp.toPx(), endX = x + 20.dp.toPx()), topLeft = Offset(x - 20.dp.toPx(), -size.height), size = Size(40.dp.toPx(), size.height * 3f))
                    }
                }
            }
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Box(modifier = Modifier.size(54.dp)) {
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
            if (unread) {
                Box(Modifier.align(Alignment.TopEnd).size(13.dp).graphicsLayer { alpha = blink }.clip(CircleShape).background(Color(0xFF0C1150)).padding(2.dp).clip(CircleShape).background(Color(0xFFFF2D45)))
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WlText3D(title, 14.5.sp, if (unread) WlTone.GOLD else WlTone.CREAM, modifier = Modifier.weight(1f, fill = false), overflow = TextOverflow.Ellipsis)
                if (unread) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        newText, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Brush.verticalGradient(listOf(Color(0xFFFF5A70), Color(0xFFE11D48)))).padding(horizontal = 7.dp, vertical = 1.dp)
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(message, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp, color = if (unread) Color(0xFFFFF3D8) else Color(0xFFC8CEEA), maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(category, fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = ring, maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(ring.copy(alpha = 0.14f)).border(1.dp, ring, RoundedCornerShape(9.dp)).padding(horizontal = 8.dp, vertical = 1.dp))
                Spacer(Modifier.weight(1f))
                Text(time, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF2C94C), maxLines = 1)
            }
        }
    }
}
