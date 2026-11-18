package com.example.ui.match

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AdMatchGameArt
import com.example.ui.components.AdMatchGameChip
import com.example.ui.home.adPressScale
import com.example.ui.wallet.WlFrame
import com.example.ui.wallet.WlImage
import com.example.ui.wallet.WlText3D
import com.example.ui.wallet.WlTone

// Match detail artwork, part 1: top card, status chip, room code, info card. VISUAL ONLY.

internal val MdGoldRing = listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4))
internal val MdOrange = listOf(Color(0xFFFFE27A), Color(0xFFFFB21E), Color(0xFFF58A0A))

/** Big gold top card: game picture, game chip + badge, 3D title, optional status chip, then the tiles row. */
@Composable
fun MdHeroCard(
    isLudo: Boolean, chipText: String, title: String,
    badge: @Composable () -> Unit,
    statusChip: (@Composable () -> Unit)?,
    tiles: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    WlFrame(modifier = modifier, gold = true, pad = 14.dp) {
        AdMatchGameArt(isLudo = isLudo, dim = false, modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-6).dp))
        Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 76.dp)) {
                AdMatchGameChip(isLudo = isLudo, label = chipText)
                Spacer(Modifier.width(8.dp))
                badge()
            }
            Spacer(Modifier.height(9.dp))
            WlText3D(title, if (title.length > 18) 18.sp else 23.sp, WlTone.GOLD, modifier = Modifier.padding(end = 76.dp), overflow = TextOverflow.Ellipsis)
            if (statusChip != null) {
                Spacer(Modifier.height(8.dp))
                statusChip()
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), content = tiles)
        }
    }
}

/** Glowing status chip. kind: 0 green (registered), 1 gold (winner), 2 red (lost). */
@Composable
fun MdStatusChip(text: String, kind: Int, tag: String) {
    val (light, dark, edge, ink) = when (kind) {
        1 -> listOf(Color(0x66FFD45A), Color(0x998A5A08), Color(0xFFFFD45A), Color(0xFFFFF3B0))
        2 -> listOf(Color(0x55E11D48), Color(0x995A0A18), Color(0xFFFB7185), Color(0xFFFFD5DC))
        else -> listOf(Color(0x4D22C55E), Color(0x8C0B5A2A), Color(0xFF4ADE80), Color(0xFFC8FFE0))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = edge, spotColor = edge)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(light, dark)))
            .border(1.4.dp, edge, RoundedCornerShape(16.dp))
            .testTag(tag)
            .padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(18.dp).clip(CircleShape).background(edge)) {
            Text(if (kind == 2) "✕" else if (kind == 1) "★" else "✓", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF0C1150))
        }
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = ink, maxLines = 2)
    }
}

/** Gold framed room code card: gold lock medal, 3D title, optional green "unlocked" pill, then content. */
@Composable
fun MdRoomCodeCard(title: String, unlockedText: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    WlFrame(modifier = modifier, gold = true, pad = 16.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp).shadow(6.dp, CircleShape, spotColor = Color(0xFFFFC850)).clip(CircleShape)
                        .background(Brush.sweepGradient(MdGoldRing)).padding(2.dp).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFFFFD45A), Color(0xFF8A5A08))))
                ) { Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(10.dp))
                WlText3D(title, 18.sp, WlTone.CREAM, modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis)
                if (unlockedText != null) {
                    Text(unlockedText, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFB8FFC8), maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x3322C55E)).border(1.dp, Color(0xFF4ADE80), RoundedCornerShape(10.dp)).padding(horizontal = 9.dp, vertical = 2.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** Room code: every character in its own glowing dark box (boxes get narrower for long codes). */
@Composable
fun MdCodeDigits(label: String, code: String) {
    val anim = rememberInfiniteTransition(label = "mdCode")
    val glow by anim.animateFloat(4f, 12f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "mdCodeGlow")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFF2C94C))
        Spacer(Modifier.height(6.dp))
        val boxW = if (code.length > 9) 22.dp else 30.dp
        Row(horizontalArrangement = Arrangement.spacedBy(if (code.length > 9) 3.dp else 5.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
            code.forEach { c ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = boxW, height = 40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF05081E), Color(0xFF10164A))))
                        .border(1.3.dp, Color(0x99FFD45A), RoundedCornerShape(8.dp))
                ) {
                    Text(c.toString(), style = TextStyle(fontSize = if (code.length > 9) 16.sp else 21.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFE27A), shadow = Shadow(Color(0xCCFFD45A), blurRadius = glow)))
                }
            }
        }
    }
}

/** Orange raised copy button. */
@Composable
fun MdCopyButton(text: String, onClick: () -> Unit, tag: String) {
    val press = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .adPressScale(press, 0.92f)
            .shadow(10.dp, shape, ambientColor = Color(0xFFFFAA3C), spotColor = Color(0xFFFFAA3C))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A))))
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
            .testTag(tag)
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(MdOrange))
            .padding(horizontal = 26.dp, vertical = 8.dp)
    ) {
        Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = Color(0xFF3A1A00), modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF3A1A00))
    }
}

/** Dark dashed note (locked room code, waiting text). */
@Composable
fun MdNote(text: String, emoji: String = "🔒") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0E3C), Color(0xFF101655))))
            .border(1.2.dp, Color(0x99E8B84A), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFE7A0), lineHeight = 18.sp)
    }
}

/** State card: 3D picture in a coloured ring, optional 3D title, description, optional small badge. */
@Composable
fun MdInfoCard(@DrawableRes picture: Int, ring: Color, title: String?, description: String, modifier: Modifier = Modifier, badge: String? = null, tiltPicture: Boolean = false) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = ring, spotColor = ring)
            .clip(shape)
            .background(Brush.linearGradient(listOf(ring, Color(0xFF2A3488), ring)))
            .padding(1.5.dp)
            .clip(RoundedCornerShape(16.5.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF151B66), Color(0xFF0E1250))))
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp).shadow(6.dp, CircleShape, ambientColor = ring, spotColor = ring).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF2C2F7A), Color(0xFF0C0F3C)))).border(2.dp, ring, CircleShape)
        ) { WlImage(picture, 38.dp, tilt = tiltPicture) }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (title != null || badge != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (title != null) WlText3D(title, if (title.length > 24) 12.5.sp else 14.sp, WlTone.CREAM, modifier = Modifier.weight(1f, fill = false), overflow = TextOverflow.Ellipsis)
                    if (badge != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(badge, fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF0C1150), maxLines = 1,
                            modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(ring).padding(horizontal = 8.dp, vertical = 1.dp))
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
            Text(description, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp, color = Color(0xFFFFE7A0), textAlign = TextAlign.Start)
        }
    }
}
