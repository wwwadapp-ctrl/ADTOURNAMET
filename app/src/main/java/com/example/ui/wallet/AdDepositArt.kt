package com.example.ui.wallet

import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as drawScopeRotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.home.adPressScale

// Deposit / withdraw pages artwork, part 1: step heading, input field, amount chip, upload box, notices, big button.
// VISUAL ONLY: the caller keeps every value, check and click.

internal val DpGoldCoin = Brush.radialGradient(listOf(Color(0xFFFFF6C8), Color(0xFFFFC83A), Color(0xFFC98A10)))

/** Small gold medal with a number or icon inside. */
@Composable
fun DpMedal(text: String?, icon: ImageVector?, size: Int, modifier: Modifier = Modifier) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .size(size.dp)
      .shadow(5.dp, CircleShape, ambientColor = Color(0xFFFFC850), spotColor = Color(0xFFFFC850))
      .clip(CircleShape)
      .background(Brush.sweepGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4))))
      .padding(2.5.dp)
      .clip(CircleShape)
      .background(DpGoldCoin)
  ) {
    if (icon != null) Icon(icon, contentDescription = null, tint = Color(0xFF1B2382), modifier = Modifier.size((size * 0.55f).dp))
    else Text(text ?: "", fontSize = (size * 0.48f).sp, fontWeight = FontWeight.Black, color = Color(0xFF1B2382), maxLines = 1)
  }
}

/** "১. পেমেন্ট মেথড নির্বাচন করুন" → gold number medal + 3D heading. The leading "১. " / "1. " is taken off the text. */
@Composable
fun DpStepHead(number: String, title: String, modifier: Modifier = Modifier) {
  val clean = title.replace(Regex("^\\s*[0-9০-৯]+\\.\\s*"), "")
  Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().padding(start = 2.dp, top = 4.dp, bottom = 12.dp)) {
    DpMedal(number, null, 30)
    Spacer(Modifier.width(9.dp))
    WlText3D(clean, if (clean.length > 26) 14.sp else 17.sp, WlTone.CREAM)
  }
}

/** Dark glass input with a gold edge, a gold icon medal on the left and the label above. big = the amount field. */
@Composable
fun DpField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  icon: ImageVector,
  keyboardOptions: KeyboardOptions,
  tag: String,
  modifier: Modifier = Modifier,
  big: Boolean = false,
) {
  var focused by remember { mutableStateOf(false) }
  val shape = RoundedCornerShape(16.dp)
  Column(modifier = modifier.fillMaxWidth()) {
    Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFE7A0), modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
    BasicTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = true,
      keyboardOptions = keyboardOptions,
      cursorBrush = SolidColor(Color(0xFFFFD45A)),
      textStyle = TextStyle(
        fontSize = if (big) 24.sp else 15.sp,
        fontWeight = FontWeight.Black,
        color = if (big) Color(0xFFFFD45A) else Color.White,
        letterSpacing = 0.5.sp
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag(tag)
        .onFocusChanged { focused = it.isFocused },
      decorationBox = { inner ->
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .then(if (big || focused) Modifier.shadow(8.dp, shape, ambientColor = Color(0xFFFFC850), spotColor = Color(0xFFFFC850)) else Modifier)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF070A2E), Color(0xFF0E1350))))
            .border(1.5.dp, if (big || focused) Color(0xFFFFD45A) else Color(0xB3E8B84A), shape)
            .padding(horizontal = 12.dp, vertical = if (big) 11.dp else 9.dp)
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(if (big) 34.dp else 30.dp).shadow(3.dp, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)).background(DpGoldCoin)
          ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF1B2382), modifier = Modifier.size(if (big) 20.dp else 17.dp))
          }
          Spacer(Modifier.width(10.dp))
          Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, fontSize = if (big) 20.sp else 14.sp, fontWeight = FontWeight.Bold, color = Color(0x73E6EBFF), maxLines = 1)
            inner()
          }
        }
      }
    )
  }
}

/** Quick amount pill: selected = orange-gold 3D, otherwise royal blue with a gold edge. */
@Composable
fun DpAmountChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val press = remember { MutableInteractionSource() }
  val shape = RoundedCornerShape(16.dp)
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .adPressScale(press, 0.9f)
      .clip(shape)
      .background(if (selected) Color(0xFF8A4A06) else Color(0xFF0A0E3C))
      .padding(bottom = 4.dp)
      .clip(shape)
      .background(Brush.verticalGradient(if (selected) listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A)) else listOf(Color(0xFFE0A82A), Color(0xFF8A5A08))))
      .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
      .padding(if (selected) 2.dp else 1.5.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(Brush.verticalGradient(if (selected) listOf(Color(0xFFFFE27A), Color(0xFFFFB21E), Color(0xFFF58A0A)) else listOf(Color(0xFF2A36A4), Color(0xFF141A66))))
      .padding(vertical = 7.dp)
  ) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (selected) Color(0xFF3A1A00) else Color(0xFFFFF3C4), maxLines = 1, softWrap = false)
  }
}

/** Dashed gold box that opens the gallery. */
@Composable
fun DpUploadBox(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val anim = rememberInfiniteTransition(label = "dpUpload")
  val bob by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dpBob")
  val press = remember { MutableInteractionSource() }
  val shape = RoundedCornerShape(18.dp)
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
      .fillMaxWidth()
      .adPressScale(press, 0.97f)
      .clip(shape)
      .background(Brush.verticalGradient(listOf(Color(0xB30A0E3C), Color(0x99141A66))))
      .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
      .drawWithContent {
        drawContent()
        drawRoundRect(Color(0xBFFFD45A), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))))
      }
      .padding(vertical = 16.dp, horizontal = 10.dp)
  ) {
    DpMedal(null, icon, 50, modifier = Modifier.offset(y = (-4 * bob).dp))
    Spacer(Modifier.height(8.dp))
    WlText3D(title, 14.sp, WlTone.CREAM)
    Spacer(Modifier.height(3.dp))
    Text(subtitle, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF2C94C), textAlign = TextAlign.Center)
  }
}

/** Coloured message strip: green = safe notice, red = error. */
@Composable
fun DpNotice(text: String, icon: ImageVector, error: Boolean, modifier: Modifier = Modifier) {
  val c = if (error) Color(0xFFFB7185) else Color(0xFF4ADE80)
  val shape = RoundedCornerShape(14.dp)
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .background(Brush.horizontalGradient(listOf(c.copy(alpha = 0.2f), c.copy(alpha = 0.05f))))
      .border(1.3.dp, c.copy(alpha = 0.7f), shape)
      .padding(horizontal = 12.dp, vertical = 10.dp)
  ) {
    Icon(icon, contentDescription = null, tint = c, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(9.dp))
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (error) Color(0xFFFFD5DC) else Color(0xFFD8FFE6))
  }
}

/** Big orange-gold 3D button with a moving shine. Shows a spinner and ignores taps while loading. */
@Composable
fun DpBigButton(text: String, onClick: () -> Unit, tag: String, modifier: Modifier = Modifier, isLoading: Boolean = false) {
  val anim = rememberInfiniteTransition(label = "dpBtn")
  val sweep by anim.animateFloat(-0.3f, 2.3f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing)), label = "dpBtnSweep")
  val press = remember { MutableInteractionSource() }
  val shape = RoundedCornerShape(20.dp)
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .fillMaxWidth()
      .adPressScale(press, 0.95f)
      .shadow(12.dp, shape, ambientColor = Color(0xFFFFB43C), spotColor = Color(0xFFFFB43C))
      .clip(shape)
      .background(Color(0xFF8A4A06))
      .padding(bottom = 6.dp)
      .clip(shape)
      .background(Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A))))
      .clickable(interactionSource = press, indication = LocalIndication.current, enabled = !isLoading, onClick = onClick)
      .testTag(tag)
      .padding(2.dp)
      .clip(RoundedCornerShape(18.dp))
      .background(Brush.verticalGradient(listOf(Color(0xFFFFE27A), Color(0xFFFFB21E), Color(0xFFF58A0A))))
      .drawWithContent {
        drawContent()
        val x = size.width * sweep
        drawScopeRotate(22f, pivot = Offset(x, size.height / 2f)) {
          drawRect(Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0xBFFFFFFF), Color(0x00FFFFFF)), startX = x - 15.dp.toPx(), endX = x + 15.dp.toPx()), topLeft = Offset(x - 15.dp.toPx(), -size.height), size = Size(30.dp.toPx(), size.height * 3f))
        }
      }
      .padding(vertical = 14.dp)
  ) {
    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color(0xFF3A1A00), strokeWidth = 2.5.dp)
    else Text("$text ›", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color(0xFF3A1A00), maxLines = 1)
  }
}
