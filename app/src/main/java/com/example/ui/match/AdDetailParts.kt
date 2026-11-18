package com.example.ui.match

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.profile.PfChoiceButton
import com.example.ui.wallet.WlFrame
import com.example.ui.wallet.WlImage
import com.example.ui.wallet.WlRays
import com.example.ui.wallet.WlText3D
import com.example.ui.wallet.WlTone

// Match detail artwork, part 2: state button, players list, join window, proof window. VISUAL ONLY.

/** Big 3D status button (not clickable). kind: 0 green joined, 1 gold winner, 2 grey (lost / upcoming). */
@Composable
fun MdStateButton(text: String, kind: Int, tag: String, modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "mdState")
    val sweep by anim.animateFloat(-0.3f, 2.3f, infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing)), label = "mdStateSweep")
    val (edge, fill, base, ink) = when (kind) {
        1 -> listOf(listOf(Color(0xFFFFF8D8), Color(0xFFFFD45A)), listOf(Color(0xFFFFF3B0), Color(0xFFFFC21E), Color(0xFFC98A10)), listOf(Color(0xFF7A4A06)), listOf(Color(0xFF3A1A00)))
        0 -> listOf(listOf(Color(0xFFC8FFE0), Color(0xFF22C55E)), listOf(Color(0xFF5EF09A), Color(0xFF16A34A), Color(0xFF0B5A2A)), listOf(Color(0xFF064E1F)), listOf(Color.White))
        else -> listOf(listOf(Color(0xFFAEB6D8), Color(0xFF4A5280)), listOf(Color(0xFF3A4270), Color(0xFF1E2450)), listOf(Color(0xFF0A0E3C)), listOf(Color(0xFFC8CEEA)))
    }
    val shape = RoundedCornerShape(18.dp)
    Box(modifier = modifier.fillMaxWidth().height(58.dp).testTag(tag)) {
        Box(Modifier.fillMaxWidth().height(52.dp).offset(y = 6.dp).clip(shape).background(base[0]))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(if (kind == 2) 4.dp else 12.dp, shape, ambientColor = edge[1], spotColor = edge[1])
                .clip(shape)
                .background(Brush.verticalGradient(edge))
                .padding(2.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(fill))
                .drawWithContent {
                    drawContent()
                    if (kind != 2) {
                        val x = size.width * sweep
                        drawScopeRotate(22f, pivot = Offset(x, size.height / 2f)) {
                            drawRect(Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x80FFFFFF), Color(0x00FFFFFF)), startX = x - 11.dp.toPx(), endX = x + 11.dp.toPx()), topLeft = Offset(x - 11.dp.toPx(), -size.height), size = Size(22.dp.toPx(), size.height * 3f))
                        }
                    }
                }
        ) {
            Text((if (kind == 1) "🏆 " else if (kind == 0) "✓ " else "") + text, fontSize = 16.sp, fontWeight = FontWeight.Black, color = ink[0], maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Gold framed players list with the gamepad picture and a 3D title. */
@Composable
fun MdPlayersCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    WlFrame(modifier = modifier, gold = true, pad = 14.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WlImage(R.drawable.wl_gamepad, 34.dp)
                Spacer(Modifier.width(9.dp))
                WlText3D(title, 17.sp, WlTone.CREAM)
            }
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

/** One player row: avatar in a gold ring, name, "you" pill, slot pill. Current player = green row. */
@Composable
fun MdPlayerRow(name: String, slot: String, isCurrent: Boolean, youText: String, avatar: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(if (isCurrent) Brush.horizontalGradient(listOf(Color(0x3822C55E), Color(0x990E1250))) else Brush.verticalGradient(listOf(Color(0xFF0A0E3C), Color(0xFF101655))))
            .border(1.2.dp, if (isCurrent) Color(0xFF4ADE80) else Color(0x73E8B84A), shape)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(42.dp).clip(CircleShape).background(Brush.sweepGradient(MdGoldRing)).padding(2.dp).clip(CircleShape)) { avatar() }
        Spacer(Modifier.width(10.dp))
        Text(name, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFF3D8), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        if (isCurrent) {
            Spacer(Modifier.width(6.dp))
            Text(youText, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF04210F), modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(Color(0xFF4ADE80)).padding(horizontal = 7.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(slot, fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = if (isCurrent) Color(0xFFC8FFE0) else Color(0xFF67E8F9),
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (isCurrent) Color(0x4022C55E) else Color(0x1F67E8F9)).border(1.dp, if (isCurrent) Color(0xFF4ADE80) else Color(0xFF67E8F9), RoundedCornerShape(10.dp)).padding(horizontal = 9.dp, vertical = 2.dp))
    }
}

/** Big picture on turning rays at the top of a window. */
@Composable
private fun MdWindowTop(picture: Int?, emoji: String?, title: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
        WlRays(130.dp)
        if (picture != null) WlImage(picture, 64.dp) else Text(emoji ?: "", fontSize = 44.sp)
    }
    WlText3D(title, if (title.length > 20) 17.sp else 19.sp, WlTone.GOLD)
}

/** Join confirm window: warning sign, name rule panel, gold fee strip with money bag, cancel + join buttons. */
@Composable
fun MdJoinWindow(title: String, nameNotice: String, feeNotice: String, cancelText: String, confirmText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        WlFrame(gold = true, pad = 16.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                MdWindowTop(null, "⚠️", title.removePrefix("⚠️").trim())
                Spacer(Modifier.height(10.dp))
                Text(nameNotice, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFF3D8), lineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(listOf(Color(0xFF0A0E3C), Color(0xFF101655)))).border(1.2.dp, Color(0x80E8B84A), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp))
                Spacer(Modifier.height(9.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brush.horizontalGradient(listOf(Color(0x38FFD45A), Color(0x0DFFD45A)))).border(1.3.dp, Color(0xFFFFD45A), RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    WlImage(R.drawable.wl_moneybag, 30.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(feeNotice, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFE27A))
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    PfChoiceButton(cancelText, false, onDismiss, "detail_join_cancel_btn", Modifier.weight(1f))
                    PfChoiceButton(confirmText, true, onConfirm, "detail_join_confirm_btn", Modifier.weight(1f))
                }
            }
        }
    }
}

/** Proof window: trophy on rays, 3D title, scrolling content (upload box + fields), submit slot, cancel text. */
@Composable
fun MdProofWindow(
    title: String, cancelText: String, cancelEnabled: Boolean, onDismiss: () -> Unit,
    submit: @Composable () -> Unit, content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = { if (cancelEnabled) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxWidth(0.92f)) {
            WlFrame(gold = true, pad = 16.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                    MdWindowTop(R.drawable.wl_trophy, null, title)
                    Spacer(Modifier.height(10.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                        content = content
                    )
                    Spacer(Modifier.height(14.dp))
                    submit()
                    Text(
                        cancelText, fontSize = 14.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                        color = if (cancelEnabled) Color(0xFFAEB6D8) else Color(0x66AEB6D8),
                        modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(10.dp)).clickable(enabled = cancelEnabled, onClick = onDismiss).padding(horizontal = 18.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
