package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import kotlin.math.roundToInt

// Crisp gold title text (same idea as the splash "AD TOURNAMENT"): VISUAL ONLY.
// 3 layers, no blur: dark 3D depth below, sharp brown outline, bright gold fill on top.

private val AdGoldFill = Brush.verticalGradient(
    listOf(Color(0xFFFFFDE8), Color(0xFFFFF09A), Color(0xFFFFD02A), Color(0xFFFFA800), Color(0xFFE87A00))
)

@Composable
fun AdGoldText(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Black,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = false,
    fill: Brush = AdGoldFill,
    outline: Color = Color(0xFF5A2A00),
    depth: Color = Color(0xFF2A1200),
) {
    val px = with(LocalDensity.current) { fontSize.toPx() }
    val base = TextStyle(fontSize = fontSize, fontWeight = fontWeight, letterSpacing = letterSpacing, textAlign = textAlign)
    Box(modifier = modifier) {
        // 1. dark depth, pushed a little down
        Text(
            text = text,
            style = base.copy(color = depth, drawStyle = Stroke(width = px * 0.16f, join = StrokeJoin.Round)),
            maxLines = maxLines, overflow = overflow, softWrap = softWrap,
            modifier = Modifier.offset { IntOffset(0, (px * 0.09f).roundToInt()) }
        )
        // 2. sharp brown outline
        Text(
            text = text,
            style = base.copy(color = outline, drawStyle = Stroke(width = px * 0.12f, join = StrokeJoin.Round)),
            maxLines = maxLines, overflow = overflow, softWrap = softWrap
        )
        // 3. bright gold fill
        Text(
            text = text,
            style = base.copy(brush = fill),
            maxLines = maxLines, overflow = overflow, softWrap = softWrap
        )
    }
}
