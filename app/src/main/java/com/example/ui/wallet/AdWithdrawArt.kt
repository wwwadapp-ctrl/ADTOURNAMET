package com.example.ui.wallet

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.home.adPressScale

// Withdraw page artwork: small gold button, summary card, in-page success card.
// VISUAL ONLY: the caller keeps every value, check and click.

/** Small royal-blue button with a gold edge (e.g. "use saved number"). */
@Composable
fun WdSmallButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val press = remember { MutableInteractionSource() }
  Text(
    text, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFE7A0), maxLines = 1,
    modifier = modifier
      .adPressScale(press, 0.9f)
      .clip(RoundedCornerShape(12.dp))
      .background(Brush.verticalGradient(listOf(Color(0xFFE0A82A), Color(0xFF8A5A08))))
      .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
      .padding(1.5.dp)
      .clip(RoundedCornerShape(10.5.dp))
      .background(Brush.verticalGradient(listOf(Color(0xFF2A36A4), Color(0xFF141A66))))
      .padding(horizontal = 9.dp, vertical = 4.dp)
  )
}

/** Label / value rows with dashed lines. A row whose colour is null shows the value in white. */
@Composable
private fun WdRows(rows: List<Triple<String, String, Color?>>) {
  rows.forEach { (k, v, c) ->
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
      Text(k, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C94C), modifier = Modifier.weight(1f))
      Text(v, fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = c ?: Color.White, maxLines = 1)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x59E8B84A)))
  }
}

/** "উত্তোলনের বিবরণ" card: tray picture + 3D title, rows, then a big gold total. */
@Composable
fun WdSummaryCard(title: String, rows: List<Triple<String, String, Color?>>, totalLabel: String, totalText: String, modifier: Modifier = Modifier) {
  val shape = RoundedCornerShape(18.dp)
  Column(
    modifier = modifier.fillMaxWidth().clip(shape)
      .background(Brush.verticalGradient(listOf(Color(0xFF0A0E3C), Color(0xFF101655))))
      .border(1.5.dp, Color(0xCCE8B84A), shape)
      .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Image(painter = painterResource(id = R.drawable.wl_withdraw), contentDescription = null, modifier = Modifier.size(26.dp))
      Spacer(Modifier.width(7.dp))
      WlText3D(title, 15.sp, WlTone.CREAM)
    }
    Spacer(Modifier.height(2.dp))
    WdRows(rows)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
      Text(totalLabel, fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFE7A0), modifier = Modifier.weight(1f))
      WlText3D(totalText, if (totalText.length > 10) 20.sp else 24.sp, WlTone.GOLD)
    }
  }
}

/** In-page success card: green tick medal on rays, message, status pill, rows, green amount, gold button. */
@Composable
fun WdSuccessCard(
  title: String, message: String, statusLabel: String, statusText: String,
  rows: List<Triple<String, String, Color?>>, amountLabel: String, amountText: String,
  buttonText: String, onButton: () -> Unit, modifier: Modifier = Modifier,
) {
  WlFrame(gold = true, pad = 16.dp, modifier = modifier) {
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
          .border(1.3.dp, Color(0xB3E8B84A), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 4.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
          Text(statusLabel, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C94C), modifier = Modifier.weight(1f))
          WlStatusPill(statusText, 1)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x59E8B84A)))
        WdRows(rows)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
          Text(amountLabel, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C94C), modifier = Modifier.weight(1f))
          WlText3D(amountText, 16.sp, WlTone.GREEN)
        }
      }
      Spacer(Modifier.height(14.dp))
      DpBigButton(buttonText, onButton, "withdraw_success_back_btn")
    }
  }
}
