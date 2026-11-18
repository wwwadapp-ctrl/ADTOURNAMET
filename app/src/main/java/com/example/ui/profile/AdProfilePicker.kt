package com.example.ui.profile

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.components.AvatarPreset
import com.example.ui.home.adPressScale
import com.example.ui.wallet.WlFrame
import com.example.ui.wallet.WlText3D
import com.example.ui.wallet.WlTone

// Profile page artwork, part 3: choice buttons, language, fair play, logout, avatar picker. VISUAL ONLY.

private fun Modifier.pfShine3(sweep: Float, halfDp: Dp, alpha: Float) = drawWithContent {
    drawContent()
    val x = size.width * sweep
    val half = halfDp.toPx()
    drawScopeRotate(22f, pivot = Offset(x, size.height / 2f)) {
        drawRect(Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color.White.copy(alpha = alpha), Color(0x00FFFFFF)), startX = x - half, endX = x + half), topLeft = Offset(x - half, -size.height), size = Size(half * 2f, size.height * 3f))
    }
}

/** Raised orange (selected) or royal blue button. Used for language and the male / female tabs. */
@Composable
fun PfChoiceButton(text: String, selected: Boolean, onClick: () -> Unit, tag: String, modifier: Modifier = Modifier) {
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
                .background(Brush.verticalGradient(if (selected) PfOrange else listOf(Color(0xFF2E3CB0), Color(0xFF141A66))))
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF3A1A00), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
            }
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Black, color = if (selected) Color(0xFF3A1A00) else Color(0xFFFFF3C4), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Language card in a gold frame with two choice buttons. */
@Composable
fun PfLanguageCard(title: String, subtitle: String, bnText: String, enText: String, bnSelected: Boolean, onBn: () -> Unit, onEn: () -> Unit, modifier: Modifier = Modifier) {
    WlFrame(modifier = modifier, gold = true, pad = 16.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PfMedal(Icons.Filled.Language, Color(0xFF7DA8FF), Color(0xFF1B2382), 42.dp, 22.dp)
                Spacer(Modifier.width(11.dp))
                Column {
                    WlText3D(title, if (title.length > 20) 14.sp else 16.sp, WlTone.CREAM)
                    Text(subtitle, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C94C))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PfChoiceButton(bnText, bnSelected, onBn, "language_option_bn", Modifier.weight(1f))
                PfChoiceButton(enText, !bnSelected, onEn, "language_option_en", Modifier.weight(1f))
            }
        }
    }
}

/** Red raised 3D logout button with a moving shine. */
@Composable
fun PfLogoutButton(text: String, onClick: () -> Unit, tag: String, modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "pfOut")
    val sweep by anim.animateFloat(-0.3f, 2.2f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing)), label = "pfOutSweep")
    val press = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)
    Box(modifier = modifier.adPressScale(press, 0.95f).fillMaxWidth().height(58.dp)) {
        Box(Modifier.fillMaxWidth().height(52.dp).offset(y = 6.dp).clip(shape).background(Color(0xFF5A0A18)))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(12.dp, shape, ambientColor = Color(0xFFE11D48), spotColor = Color(0xFFE11D48))
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFC8D0), Color(0xFFE11D48))))
                .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
                .testTag(tag)
                .padding(2.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFF5A70), Color(0xFFE11D48), Color(0xFF9F1239))))
                .pfShine3(sweep, 11.dp, 0.5f)
        ) {
            Icon(Icons.Filled.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}

/** One round avatar in the picker: gold glowing ring + green tick when selected. */
@Composable
private fun PfAvatarCell(preset: AvatarPreset, selected: Boolean, label: String, onClick: () -> Unit) {
    val press = remember { MutableInteractionSource() }
    Box(modifier = Modifier.adPressScale(press, 0.9f).size(80.dp)) {
        Image(
            painter = painterResource(preset.image),
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .shadow(if (selected) 14.dp else 4.dp, CircleShape, ambientColor = Color(0xFFFFC850), spotColor = if (selected) Color(0xFFFFC850) else Color.Black)
                .clip(CircleShape)
                .background(if (selected) Brush.sweepGradient(PfGoldRing) else Brush.verticalGradient(listOf(Color(0xFF8FA0E8), Color(0xFF3A48A8))))
                .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
                .padding(if (selected) 3.5.dp else 2.dp)
                .clip(CircleShape)
        )
        if (selected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.align(Alignment.BottomEnd).size(24.dp).clip(CircleShape).background(Color(0xFF0C1150)).padding(2.dp).clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF4ADE80), Color(0xFF16A34A))))
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/**
 * Avatar picker window: male / female tabs (6 pictures each), gallery button, cancel.
 * presets = the 12 picture avatars. onPick gets the chosen preset id.
 */
@Composable
fun PfAvatarPicker(
    title: String, subtitle: String, maleText: String, femaleText: String,
    presets: List<AvatarPreset>, selectedId: String?, labelOf: (String) -> String,
    onPick: (String) -> Unit, galleryText: String, onGallery: () -> Unit, cancelText: String, onDismiss: () -> Unit,
) {
    val startFemale = presets.firstOrNull { it.id == selectedId }?.female == true
    var female by remember { mutableStateOf(startFemale) }
    val shown = presets.filter { it.female == female && it.image != 0 }
    Dialog(onDismissRequest = onDismiss) {
        WlFrame(gold = true, pad = 16.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                WlText3D(title, if (title.length > 20) 17.sp else 20.sp, WlTone.CREAM)
                Text(subtitle, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF2C94C), textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    PfChoiceButton(maleText, !female, { female = false }, "avatar_tab_male", Modifier.weight(1f))
                    PfChoiceButton(femaleText, female, { female = true }, "avatar_tab_female", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                shown.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        row.forEach { p -> PfAvatarCell(p, p.id == selectedId, labelOf(p.id)) { onPick(p.id) } }
                    }
                }
                Spacer(Modifier.height(10.dp))
                val press = remember { MutableInteractionSource() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .adPressScale(press, 0.96f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF0A0E3C), Color(0xFF101655))))
                        .border(1.4.dp, Color(0xFFE8B84A), RoundedCornerShape(15.dp))
                        .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onGallery)
                        .testTag("avatar_gallery_button")
                        .padding(vertical = 11.dp)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Color(0xFFFFD45A), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(galleryText, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFE7A0))
                }
                Text(
                    cancelText, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFAEB6D8),
                    modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onDismiss).padding(horizontal = 18.dp, vertical = 6.dp)
                )
            }
        }
    }
}
