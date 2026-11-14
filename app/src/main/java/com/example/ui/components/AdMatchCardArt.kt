package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as drawScopeRotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

// Match card artwork, part 1: frame, game picture, chip, badge, numbers panel, seat bar. VISUAL ONLY.

enum class AdMatchFrame { GOLD, GREEN, RED, CYAN, WIN, AMBER }
enum class AdSeatFill { EMPTY, GOLD, RED, GREEN }

internal val McGoldText = Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC83A), Color(0xFFFF8A1A)))
internal val McGoldEdge = Brush.linearGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4)))

private fun frameColors(f: AdMatchFrame): Pair<List<Color>, Color> = when (f) {
    AdMatchFrame.GOLD -> listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4)) to Color(0xFFFFC850)
    AdMatchFrame.GREEN -> listOf(Color(0xFFC8FFE0), Color(0xFF22C55E), Color(0xFF0B5A2A), Color(0xFF22C55E), Color(0xFFC8FFE0)) to Color(0xFF22C55E)
    AdMatchFrame.RED -> listOf(Color(0xFFFFC8D0), Color(0xFFE11D48), Color(0xFF5A0A18), Color(0xFFE11D48), Color(0xFFFFC8D0)) to Color(0xFFE11D48)
    AdMatchFrame.CYAN -> listOf(Color(0xFFD0F6FF), Color(0xFF22D3EE), Color(0xFF0B4A6A), Color(0xFF22D3EE), Color(0xFFD0F6FF)) to Color(0xFF22D3EE)
    AdMatchFrame.WIN -> listOf(Color(0xFFFFF8D8), Color(0xFFFFD45A), Color(0xFFB07A10), Color(0xFFFFD45A), Color(0xFFFFF8D8)) to Color(0xFFFFD45A)
    AdMatchFrame.AMBER -> listOf(Color(0xFFFFF1C8), Color(0xFFF59E0B), Color(0xFF7A3E05), Color(0xFFF59E0B), Color(0xFFFFF1C8)) to Color(0xFFF59E0B)
}

/** Royal blue glass card with a coloured metal edge, inner gold line, twinkles and a moving shine. */
@Composable
fun AdMatchCardFrame(frame: AdMatchFrame, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val anim = rememberInfiniteTransition(label = "mcFrame")
    val sweep by anim.animateFloat(-0.3f, 2.4f, infiniteRepeatable(tween(4500, easing = FastOutSlowInEasing)), label = "mcSweep")
    val tw by anim.animateFloat(0.15f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "mcTw")
    val (edge, glow) = frameColors(frame)
    val outer = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, outer, ambientColor = Color(0xFF050A3C), spotColor = glow)
            .clip(outer)
            .background(Brush.linearGradient(edge))
            .padding(1.6.dp)
            .clip(RoundedCornerShape(18.5.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2B3AA8), Color(0xFF1E2780), Color(0xFF151B5E)), start = Offset(0f, 0f), end = Offset(300f, 900f)))
            .drawBehind {
                val w = size.width; val h = size.height
                drawCircle(Brush.radialGradient(listOf(Color(0x38FFC85A), Color.Transparent), center = Offset(w * 0.88f, h * 0.18f), radius = w * 0.4f), w * 0.4f, Offset(w * 0.88f, h * 0.18f))
                drawCircle(Brush.radialGradient(listOf(Color(0x4D8C6EFF), Color.Transparent), center = Offset(0f, h), radius = w * 0.55f), w * 0.55f, Offset(0f, h))
                drawRoundRect(Color(0x59E0A82A), topLeft = Offset(5.dp.toPx(), 5.dp.toPx()), size = Size(w - 10.dp.toPx(), h - 10.dp.toPx()), cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(1.dp.toPx()))
                val pts = listOf(0.3f to 0.12f, 0.55f to 0.4f, 0.72f to 0.08f, 0.15f to 0.62f, 0.62f to 0.7f, 0.9f to 0.55f)
                pts.forEachIndexed { i, (px, py) ->
                    val a = if (i % 2 == 0) tw else 1.15f - tw
                    drawCircle(Color(0xFFFFE7A0).copy(alpha = 0.25f * a), 4.dp.toPx(), Offset(w * px, h * py))
                    drawCircle(Color(0xFFFFE7A0).copy(alpha = a), 1.2.dp.toPx(), Offset(w * px, h * py))
                }
            }
            .drawWithContent {
                drawContent()
                val x = size.width * sweep
                drawScopeRotate(22f, pivot = Offset(x, size.height / 2f)) {
                    drawRect(
                        Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x1FFFFFFF), Color(0x00FFFFFF)), startX = x - 20.dp.toPx(), endX = x + 20.dp.toPx()),
                        topLeft = Offset(x - 20.dp.toPx(), -size.height), size = Size(40.dp.toPx(), size.height * 3f)
                    )
                }
            }
            .padding(12.dp),
        content = content
    )
}

/** Dice (ludo) or carrom picture in a glowing medal with a turning gold arc. */
@Composable
fun AdMatchGameArt(isLudo: Boolean, dim: Boolean, modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "mcArt")
    val spin by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "mcSpin")
    val hop by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "mcHop")
    val grey = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.45f) }) }
    Box(modifier = modifier.size(74.dp).graphicsLayer { alpha = if (dim) 0.8f else 1f }, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(Color(0x8CFFD25A), Color.Transparent)), size.minDimension * 0.46f)
            drawScopeRotate(spin) {
                drawArc(Color(0xFFFFE7A0), -90f, 90f, false, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                drawArc(Color(0x73FFC85A), 0f, 90f, false, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        Image(
            painter = painterResource(id = if (isLudo) R.drawable.mg_dice else R.drawable.mg_carrom),
            contentDescription = null,
            colorFilter = if (dim) grey else null,
            modifier = Modifier.size(52.dp).graphicsLayer {
                translationY = -(if (isLudo) 6f else 4f) * hop * density
                rotationZ = if (isLudo) -8f + 18f * hop else 0f
            }
        )
    }
}

/** Small gold-edged pill with the game picture and "লুডু ১v১". */
@Composable
fun AdMatchGameChip(isLudo: Boolean, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF25338F), Color(0xFF141C5E))))
            .border(1.2.dp, Color(0xFFE0A82A), RoundedCornerShape(12.dp))
            .padding(start = 4.dp, end = 9.dp, top = 3.dp, bottom = 3.dp)
    ) {
        Image(painter = painterResource(id = if (isLudo) R.drawable.mg_dice else R.drawable.mg_carrom), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFF3C4), maxLines = 1, softWrap = false)
    }
}

/** Glowing outlined status pill. */
@Composable
fun AdMatchBadge(label: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(12.dp), ambientColor = color, spotColor = color)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x990A0E32))
            .border(1.dp, color, RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    )
}
