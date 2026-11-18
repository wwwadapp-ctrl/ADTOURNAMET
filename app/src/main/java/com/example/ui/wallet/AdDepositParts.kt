package com.example.ui.wallet

import androidx.annotation.DrawableRes
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.home.adPressScale

// Deposit / withdraw pages artwork, part 2: payment method tile, number box, rules list, screenshot preview, success dialog.
// VISUAL ONLY: the caller keeps every value, check and click.

/** bKash / Nagad tile. Selected glows in the brand colour with a tick in the corner. */
@Composable
fun DpMethodTile(name: String, badge: String, @DrawableRes logo: Int, brand: Color, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val press = remember { MutableInteractionSource() }
  val shape = RoundedCornerShape(18.dp)
  Box(modifier = modifier.adPressScale(press, 0.95f).padding(top = 6.dp, end = 6.dp)) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .shadow(if (selected) 12.dp else 6.dp, shape, ambientColor = if (selected) brand else Color.Black, spotColor = if (selected) brand else Color.Black)
        .clip(shape)
        .background(if (selected) brand.copy(alpha = 0.55f) else Color(0xFF0A0E3C))
        .padding(bottom = 5.dp)
        .clip(shape)
        .background(Brush.linearGradient(if (selected) listOf(Color.White, brand, brand) else listOf(Color(0xFFE0A82A), Color(0xFF8A5A08))))
        .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
        .padding(if (selected) 2.dp else 1.5.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(
          if (selected) Brush.verticalGradient(listOf(brand.copy(alpha = 0.45f), Color(0xFF1B2382), Color(0xFF121860)))
          else Brush.verticalGradient(listOf(Color(0xFF2A36A4), Color(0xFF141A66)))
        )
        .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.size(42.dp).shadow(4.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).background(Color.White)) {
        Image(painter = painterResource(id = logo), contentDescription = name, modifier = Modifier.size(30.dp))
      }
      Spacer(Modifier.width(10.dp))
      Column {
        WlText3D(name, 16.sp, WlTone.CREAM)
        Text(badge, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = if (selected) Color(0xFFFFF3C4) else Color(0xFFF2C94C), maxLines = 1)
      }
    }
    if (selected) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp).size(22.dp)
          .shadow(6.dp, CircleShape, ambientColor = brand, spotColor = brand).clip(CircleShape).background(brand).border(2.dp, Color.White, CircleShape)
      ) { Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp)) }
    }
  }
}

/** Official number box: label + "অফিসিয়াল" pill, big 3D number, brand-coloured copy button (green after copy). */
@Composable
fun DpNumberBox(label: String, officialText: String, number: String, copyText: String, copied: Boolean, brand: Color, onCopy: () -> Unit, copyTag: String, modifier: Modifier = Modifier) {
  val shape = RoundedCornerShape(18.dp)
  val press = remember { MutableInteractionSource() }
  val btn = if (copied) Color(0xFF16A34A) else brand
  Column(
    modifier = modifier
      .fillMaxWidth()
      .shadow(8.dp, shape, ambientColor = brand, spotColor = brand)
      .clip(shape)
      .background(Brush.verticalGradient(listOf(Color(0xFF0A0E3C), Color(0xFF101655))))
      .border(1.5.dp, brand, shape)
      .padding(12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.size(18.dp).clip(CircleShape).background(brand)) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
      }
      Spacer(Modifier.width(6.dp))
      Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFF3D8), maxLines = 1, modifier = Modifier.weight(1f))
      Text(
        officialText, fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF04210F), maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Brush.verticalGradient(listOf(Color(0xFFB8FFC8), Color(0xFF4ADE80)))).padding(horizontal = 9.dp, vertical = 2.dp)
      )
    }
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      WlText3D(number, if (number.length > 12) 20.sp else 24.sp, WlTone.GOLD, modifier = Modifier.weight(1f))
      Spacer(Modifier.width(8.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .adPressScale(press, 0.9f)
          .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = btn, spotColor = btn)
          .clip(RoundedCornerShape(14.dp))
          .background(btn.copy(alpha = 0.6f))
          .padding(bottom = 4.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(Brush.verticalGradient(listOf(btn.copy(alpha = 0.75f), btn)))
          .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onCopy)
          .testTag(copyTag)
          .padding(horizontal = 14.dp, vertical = 8.dp)
      ) {
        Icon(if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(copyText, fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
      }
    }
  }
}

/** Dashed box with a 3D title and numbered gold steps. */
@Composable
fun DpRules(title: String, steps: List<Pair<String, String>>, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color(0x8C0A0E3C))
      .border(1.dp, Color(0x99E8B84A), RoundedCornerShape(16.dp))
      .padding(12.dp)
  ) {
    WlText3D(title, 14.sp, WlTone.GOLD)
    steps.forEach { (n, t) ->
      Spacer(Modifier.height(7.dp))
      Row(verticalAlignment = Alignment.Top) {
        DpMedal(n, null, 20)
        Spacer(Modifier.width(8.dp))
        Text(t, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFF3D8), lineHeight = 17.sp)
      }
    }
  }
}

/** Chosen screenshot with "attached" note and replace / remove buttons. */
@Composable
fun DpScreenshotPreview(bitmap: ImageBitmap, previewText: String, attachedText: String, replaceText: String, removeText: String, onReplace: () -> Unit, onRemove: () -> Unit) {
  val shape = RoundedCornerShape(18.dp)
  Column(
    modifier = Modifier.fillMaxWidth().clip(shape).background(Color(0xB30A0E3C)).border(1.5.dp, Color(0xB34ADE80), shape).padding(10.dp)
  ) {
    Image(bitmap = bitmap, contentDescription = previewText, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)))
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(16.dp))
      Spacer(Modifier.width(4.dp))
      Text(attachedText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80), modifier = Modifier.weight(1f))
      DpAmountChip(replaceText, selected = false, onClick = onReplace, modifier = Modifier.width(70.dp))
      Spacer(Modifier.width(6.dp))
      DpAmountChip(removeText, selected = true, onClick = onRemove, modifier = Modifier.width(64.dp))
    }
  }
}

/** Success popup: green tick medal on rays, message, summary rows, gold button. rows = label to value; statusText shown as a blue pill. */
@Composable
fun DpSuccessDialog(
  title: String, message: String, rows: List<Pair<String, String>>, statusLabel: String, statusText: String,
  buttonText: String, onButton: () -> Unit, onDismiss: () -> Unit, buttonTag: String, dialogTag: String,
) {
  Dialog(onDismissRequest = onDismiss) {
    WlFrame(gold = true, pad = 16.dp, modifier = Modifier.testTag(dialogTag)) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
          WlRays(130.dp, color = Color(0xFF78FFAA))
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(64.dp).shadow(12.dp, CircleShape, ambientColor = Color(0xFF4ADE80), spotColor = Color(0xFF4ADE80)).clip(CircleShape)
              .background(Brush.sweepGradient(listOf(Color(0xFFD8FFE6), Color(0xFF22C55E), Color(0xFF0B5A2A), Color(0xFF22C55E), Color(0xFFD8FFE6))))
              .padding(3.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFB8FFC8), Color(0xFF22C55E), Color(0xFF0B5A2A))))
          ) { Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp)) }
        }
        Spacer(Modifier.height(8.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFF3C4), textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFF3D8), textAlign = TextAlign.Center, lineHeight = 18.sp)
        Spacer(Modifier.height(12.dp))
        Column(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(listOf(Color(0xFF0A0E3C), Color(0xFF101655))))
            .border(1.3.dp, Color(0xB3E8B84A), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          rows.forEach { (k, v) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
              Text(k, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C94C), modifier = Modifier.weight(1f))
              Text(v, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
            }
          }
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(statusLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C94C), modifier = Modifier.weight(1f))
            WlStatusPill(statusText, 1)
          }
        }
        Spacer(Modifier.height(14.dp))
        DpBigButton(buttonText, onButton, buttonTag)
      }
    }
  }
}
