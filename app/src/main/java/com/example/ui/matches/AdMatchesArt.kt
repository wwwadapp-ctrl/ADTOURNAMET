package com.example.ui.matches

import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate as drawScopeRotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.home.adPressScale
import com.example.ui.wallet.WlFrame
import com.example.ui.wallet.WlImage
import com.example.ui.wallet.WlRays
import com.example.ui.wallet.WlText3D
import com.example.ui.wallet.WlTone

// Matches page artwork: tab bar, tab buttons, empty message. VISUAL ONLY: the caller gives texts and clicks.

/** Gold-framed strip that holds the tab buttons; scrolls sideways if they do not fit. */
@Composable
fun AdMatchesTabBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = Color(0xFF050532), spotColor = Color(0xFFFFC850))
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF8A5A08), Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFFFFF3C4), Color(0xFF8A5A08))))
            .padding(2.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1B2382), Color(0xFF0E1250))))
            .drawBehind {
                drawCircle(Brush.radialGradient(listOf(Color(0x38FFC85A), Color.Transparent), center = Offset(size.width / 2f, 0f), radius = size.width * 0.45f), size.width * 0.45f, Offset(size.width / 2f, 0f))
            }
            .horizontalScroll(rememberScrollState())
            .padding(start = 7.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
        content = content
    )
}

/** One tab button. Selected = orange-gold with a glowing dot and a moving shine; otherwise royal blue with a gold edge and 3D text. */
@Composable
fun AdMatchesTab(text: String, selected: Boolean, onClick: () -> Unit, tag: String) {
    val anim = rememberInfiniteTransition(label = "mtTab")
    val sweep by anim.animateFloat(-0.4f, 2.4f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing)), label = "mtSweep")
    val press = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .adPressScale(press, 0.9f)
            .then(if (selected) Modifier.shadow(8.dp, shape, ambientColor = Color(0xFFFFC850), spotColor = Color(0xFFFFC850)) else Modifier)
            .clip(shape)
            .background(if (selected) Color(0xFF8A4A06) else Color(0xFF0A0E3C))
            .padding(bottom = 4.dp)
            .clip(shape)
            .background(Brush.verticalGradient(if (selected) listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A)) else listOf(Color(0xFFE0A82A), Color(0xFF8A5A08))))
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
            .testTag(tag)
            .padding(if (selected) 2.dp else 1.5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(if (selected) listOf(Color(0xFFFFE27A), Color(0xFFFFB21E), Color(0xFFF58A0A)) else listOf(Color(0xFF2E3CB0), Color(0xFF141A66))))
            .drawWithContent {
                drawContent()
                if (selected) {
                    val x = size.width * sweep
                    drawScopeRotate(22f, pivot = Offset(x, size.height / 2f)) {
                        drawRect(Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0xB3FFFFFF), Color(0x00FFFFFF)), startX = x - 8.dp.toPx(), endX = x + 8.dp.toPx()), topLeft = Offset(x - 8.dp.toPx(), -size.height), size = Size(16.dp.toPx(), size.height * 3f))
                    }
                }
            }
            .padding(horizontal = if (selected) 13.dp else 11.dp, vertical = 6.dp)
    ) {
        if (selected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).shadow(4.dp, CircleShape, ambientColor = Color.White, spotColor = Color.White).clip(CircleShape).background(Color.White))
                Spacer(Modifier.width(5.dp))
                Text(text, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF3A1A00), maxLines = 1, softWrap = false)
            }
        } else {
            WlText3D(text, 13.5.sp, WlTone.CREAM)
        }
    }
}

/** Empty list message: trophy on turning rays inside the ornate gold frame, optional gold button. */
@Composable
fun AdMatchesEmpty(title: String, description: String, actionText: String?, onAction: (() -> Unit)?, modifier: Modifier = Modifier) {
    WlFrame(modifier = modifier, gold = true, pad = 14.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
            Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                WlRays(150.dp)
                WlImage(R.drawable.wl_trophy, 90.dp)
            }
            Spacer(Modifier.height(8.dp))
            WlText3D(title, if (title.length > 26) 15.sp else 18.sp, WlTone.CREAM)
            Spacer(Modifier.height(5.dp))
            Text(description, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C94C), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 10.dp))
            if (actionText != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                val press = remember { MutableInteractionSource() }
                val shape = RoundedCornerShape(16.dp)
                Text(
                    "$actionText ›", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF3A1A00), maxLines = 1,
                    modifier = Modifier
                        .adPressScale(press, 0.92f)
                        .shadow(10.dp, shape, ambientColor = Color(0xFFFFB43C), spotColor = Color(0xFFFFB43C))
                        .clip(shape)
                        .background(Color(0xFF8A4A06))
                        .padding(bottom = 4.dp)
                        .clip(shape)
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A))))
                        .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onAction)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFE27A), Color(0xFFFFB21E), Color(0xFFF58A0A))))
                        .padding(horizontal = 22.dp, vertical = 8.dp)
                )
            }
        }
    }
}
