package com.example.ui.minigames

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.R

// Money Wheel artwork, part 2: top bar, chips, spin button, console. VISUAL ONLY.
// u = 1% of the screen width.

private val WhGoldEdge = Brush.linearGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4)))
private val WhGoldRing = Brush.sweepGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4)))
val WhChipColors = mapOf(
    1 to (Color(0xFF3A5BE8) to Color(0xFF14208A)), 3 to (Color(0xFF9B4DFF) to Color(0xFF4A1A9E)),
    5 to (Color(0xFFFF4D62) to Color(0xFF9E0F2A)), 8 to (Color(0xFF22C58E) to Color(0xFF0B6A48)),
    10 to (Color(0xFFFFB21E) to Color(0xFF9A5A00))
)

private fun Dp.sp(): TextUnit = androidx.compose.ui.unit.TextUnit(this.value, androidx.compose.ui.unit.TextUnitType.Sp)

/** Moving white shine across a shape. */
@Composable
private fun Modifier.whShine(ms: Int = 3000): Modifier {
    val a = rememberInfiniteTransition(label = "whShine")
    val p by a.animateFloat(-0.4f, 1.6f, infiniteRepeatable(tween(ms, easing = FastOutSlowInEasing)), label = "whShineP")
    return drawWithContent {
        drawContent()
        val x = size.width * p
        rotate(20f, Offset(x, size.height / 2)) {
            drawRect(Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x70FFFFFF), Color(0x00FFFFFF)), startX = x - size.height * 0.4f, endX = x + size.height * 0.4f),
                topLeft = Offset(x - size.height * 0.4f, -size.height), size = Size(size.height * 0.8f, size.height * 3f))
        }
    }
}

/** Top bar: gold back button, main balance (left), game balance with + (right). */
@Composable
fun WhTopBar(u: Dp, mainText: String, gameText: String, mainLabel: String, gameLabel: String, enabled: Boolean, onBack: () -> Unit, onGame: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = u * 3.5f, vertical = u * 2f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u * 2)) {
        Box(
            Modifier.size(u * 11).shadow(u, CircleShape).clip(CircleShape).background(WhGoldRing).padding(u * 0.9f).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF3A4AC8), Color(0xFF121A61))))
                .clickable(enabled = enabled, onClick = onBack).testTag("spin_back_button"),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFFFFE27A), modifier = Modifier.size(u * 5.6f)) }
        WhBalance(u, R.drawable.wh_coin, mainLabel, mainText, false, null, "spin_main_balance")
        Spacer(Modifier.weight(1f))
        WhBalance(u, R.drawable.wh_pad, gameLabel, gameText, true, if (enabled) onGame else null, "spin_game_balance")
    }
}

@Composable
fun WhBalance(u: Dp, icon: Int, label: String, amount: String, game: Boolean, onClick: (() -> Unit)?, tag: String) {
    val shape = RoundedCornerShape(u * 7)
    val pulse = rememberInfiniteTransition(label = "whPlus").animateFloat(1f, 1.1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "whPlusS")
    Row(
        Modifier.shadow(u, shape).clip(shape).background(WhGoldEdge).padding(u * 0.45f).clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF24309A), Color(0xFF0E1350)))).whShine(3400)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = u * 0.4f, end = u * if (game) 0.8f else 2.6f, top = u * 0.3f, bottom = u * 0.3f).testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(icon), null, Modifier.size(u * 8))
        Spacer(Modifier.width(u * 1.2f))
        Column {
            Text(label, fontSize = (u * 2.5f).sp(), fontWeight = FontWeight.Black, color = if (game) Color(0xFF04210F) else Color(0xFF3A1A00),
                modifier = Modifier.clip(RoundedCornerShape(u * 2)).background(Brush.verticalGradient(if (game) listOf(Color(0xFFC8FFE0), Color(0xFF22C55E)) else listOf(Color(0xFFFFF3B0), Color(0xFFE0A82A)))).padding(horizontal = u * 1.6f))
            Text(amount, maxLines = 1, softWrap = false, style = TextStyle(
                fontSize = (u * 4.2f).sp(), fontWeight = FontWeight.Black,
                brush = Brush.verticalGradient(if (game) listOf(Color.White, Color(0xFFB8FFD0), Color(0xFF22C55E)) else listOf(Color.White, Color(0xFFFFF0A0), Color(0xFFFFC21E), Color(0xFFF08A00))),
                shadow = Shadow(if (game) Color(0xFF04210F) else Color(0xFF3A1A00), Offset(0f, 3f), 2f)
            ))
        }
        if (game) {
            Spacer(Modifier.width(u * 1.2f))
            Box(Modifier.size(u * 6.6f).graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }.clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFFFF6C8), Color(0xFFFFC21E), Color(0xFFC98A10)))), contentAlignment = Alignment.Center) {
                Text("+", fontSize = (u * 4.8f).sp(), fontWeight = FontWeight.Black, color = Color(0xFF3A1A00))
            }
        }
    }
}

/** One casino chip: striped edge, gold ring, coloured centre. */
@Composable
fun WhChipFace(chip: Dp, value: Int, label: String) {
    val (c1, c2) = WhChipColors[value] ?: (Color(0xFF3A5BE8) to Color(0xFF14208A))
    Box(Modifier.size(chip), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            drawCircle(c2, r)
            for (k in 0 until 12) drawArc(Color(0xFFFFF6DC), k * 30f + 7f, 9f, true)
            drawCircle(Brush.radialGradient(listOf(c1, c2), center = Offset(r * 0.76f, r * 0.6f), radius = r), r * 0.74f)
            drawCircle(Color(0xFFE8B84A), r * 0.74f, style = Stroke(r * 0.09f))
        }
        Text(label, fontSize = (chip * 0.28f).sp(), fontWeight = FontWeight.Black, color = Color(0xFFFFF3C4),
            style = TextStyle(shadow = Shadow(Color(0x99000000), Offset(0f, 2f), 1f)))
    }
}

@Composable
fun WhChipRow(u: Dp, options: List<Int>, selected: Int, enabled: Boolean, label: (Int) -> String, onSelect: (Int) -> Unit) {
    val spin = rememberInfiniteTransition(label = "whChip").animateFloat(0f, 360f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "whChipR")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        options.forEach { v ->
            val sel = v == selected
            val lift by animateDpAsState(if (sel) -(u * 1.8f) else 0.dp, spring(0.45f, 600f), label = "whLift")
            val sc by animateFloatAsState(if (sel) 1.08f else 1f, spring(0.45f, 600f), label = "whSc")
            Box(Modifier.offset(y = lift).graphicsLayer { scaleX = sc; scaleY = sc }.size(u * 15)
                .clip(CircleShape).clickable(enabled = enabled) { onSelect(v) }.testTag("spin_chip_$v"), contentAlignment = Alignment.Center) {
                if (sel) Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Brush.radialGradient(listOf(Color(0xCCFFD45A), Color.Transparent)), size.minDimension / 2)
                    rotate(spin.value) { drawArc(Brush.sweepGradient(listOf(Color.Transparent, Color(0xDDFFF0B4), Color.Transparent)), 0f, 120f, false, style = Stroke(size.minDimension * 0.06f)) }
                }
                WhChipFace(u * 12.5f, v, label(v))
            }
        }
    }
}

/** Big gold 3D spin button; the chosen chip sits inside it. */
@Composable
fun WhSpinButton(u: Dp, text: String, enabled: Boolean, bet: Int, chipLabel: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val shape = RoundedCornerShape(u * 5)
    val popKey = remember { Animatable(1f) }
    LaunchedEffect(bet) { popKey.snapTo(0.3f); popKey.animateTo(1f, spring(0.4f, 500f)) }
    Box(Modifier.fillMaxWidth().height(u * 16)) {
        Box(Modifier.fillMaxWidth().height(u * 14.4f).offset(y = u * 1.6f).clip(shape).background(Color(0xFF8A4A06)))
        Row(
            Modifier.fillMaxWidth().height(u * 14.4f).offset(y = if (pressed) u * 1.2f else 0.dp).clip(shape)
                .background(if (enabled) Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC21E), Color(0xFFE8860A))) else Brush.verticalGradient(listOf(Color(0xFFE8DDB0), Color(0xFFC9A860))))
                .then(if (enabled) Modifier.whShine(2600) else Modifier)
                .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick).testTag("spin_action_button"),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, fontSize = (u * if (text.length > 9) 5.2f else 6.2f).sp(), fontWeight = FontWeight.Black, color = Color(0xFF3A1A00), maxLines = 1, softWrap = false, textAlign = TextAlign.Center, modifier = Modifier.weight(1f, fill = false),
                style = TextStyle(shadow = Shadow(Color(0x80FFFFFF), Offset(0f, 2f), 0f)))
            Spacer(Modifier.width(u * 3))
            Box(Modifier.graphicsLayer { scaleX = popKey.value; scaleY = popKey.value; rotationZ = (1f - popKey.value) * -250f }
                .border(u * 0.5f, Color(0xFFFFF3C4), CircleShape)) { WhChipFace(u * 10.5f, bet, chipLabel) }
        }
    }
}

/** Glass console with gold edge holding the chips and the spin button. */
@Composable
fun WhConsole(u: Dp, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(u * 6)
    Column(
        modifier.fillMaxWidth().shadow(u * 2, shape).clip(shape).background(Color(0xD90E1250))
            .background(Brush.verticalGradient(listOf(Color(0xC72634A0), Color(0xEB0A0E3E))))
            .border(1.5.dp, Color(0xD9FFD678), shape).padding(horizontal = u * 3.4f, vertical = u * 3.2f),
        verticalArrangement = Arrangement.spacedBy(u * 3.4f), content = content
    )
}

/** Shown when the admin has switched the game off. */
@Composable
fun WhClosedBanner(u: Dp, text: String) {
    Text(text, fontSize = (u * 3.8f).sp(), fontWeight = FontWeight.Black, color = Color(0xFFFFE3E8), textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(u * 3)).background(Brush.verticalGradient(listOf(Color(0xE6B3122F), Color(0xE66A0A1C))))
            .border(1.dp, Color(0xFFFF8A9A), RoundedCornerShape(u * 3)).padding(vertical = u * 1.8f, horizontal = u * 3))
}
