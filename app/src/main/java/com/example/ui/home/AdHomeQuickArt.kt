package com.example.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// Home page part 2 artwork: deposit / withdraw cards. VISUAL ONLY.

// ---------------------------------------------------------------- 3. deposit / withdraw cards

private class HaQuickPalette(
    val edge: List<Color>,
    val fill: List<Color>,
    val corner: Color,
    val orbEdge: List<Color>,
    val orb: List<Color>,
    val orbInset: Color,
    val glyph: Color,
    val accent: Color,
    val glow: Color
)

private val HaCyanPalette = HaQuickPalette(
    edge = listOf(Color(0xFFECFEFF), Color(0xFF22D3EE), Color(0xFF0E5A78), Color(0xFF22D3EE), Color(0xFFCFFAFE)),
    fill = listOf(Color(0xFF0E6E8E), Color(0xFF0B3F78), Color(0xFF0A1E5E)),
    corner = Color(0x7322D3EE),
    orbEdge = listOf(Color(0xFFECFEFF), Color(0xFF0E7490)),
    orb = listOf(Color(0xFFCFFAFE), Color(0xFF22D3EE), Color(0xFF0E7490)),
    orbInset = Color(0x8C08465A),
    glyph = Color.White,
    accent = Color(0xFF67E8F9),
    glow = Color(0xFF22D3EE)
)

private val HaGoldPalette = HaQuickPalette(
    edge = listOf(Color(0xFFFFF8D8), Color(0xFFFFD45A), Color(0xFF6B4A10), Color(0xFFFFD45A), Color(0xFFFFF3C4)),
    fill = listOf(Color(0xFF7A5212), Color(0xFF4A300A), Color(0xFF241604)),
    corner = Color(0x73FFC850),
    orbEdge = listOf(Color(0xFFFFF8D8), Color(0xFF8A5A08)),
    orb = listOf(Color(0xFFFFF8D8), Color(0xFFFFD45A), Color(0xFFB9780C)),
    orbInset = Color(0x8C6E4100),
    glyph = Color(0xFF5A3700),
    accent = Color(0xFFFFD45A),
    glow = Color(0xFFFFD25A)
)

@Composable
fun AdQuickActionCardArt(
    title: String,
    subtitle: String,
    isDeposit: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    val p = if (isDeposit) HaCyanPalette else HaGoldPalette
    val outer = RoundedCornerShape(16.dp)
    val press = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .adPressScale(press)
            .height(92.dp)
            .shadow(10.dp, outer, ambientColor = Color(0xFF050A3C), spotColor = Color(0xFF050A3C))
            .clip(outer)
            .background(Brush.linearGradient(p.edge))
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
            .testTag(tag)
            .padding(1.4.dp)
            .clip(RoundedCornerShape(14.6.dp))
            .background(Brush.linearGradient(p.fill))
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(p.corner, Color.Transparent), center = Offset(size.width * 0.15f, size.height * 0.2f), radius = size.width * 0.7f),
                    radius = size.width * 0.7f, center = Offset(size.width * 0.15f, size.height * 0.2f)
                )
                haGlassTop(0.42f, 0.18f, 14.dp.toPx())
                haStreak(size.width * 0.55f, 22.dp.toPx(), 0.16f)
                // big faint watermark icon in the corner
                val wc = Offset(size.width - 25.dp.toPx(), size.height - 23.dp.toPx())
                val wr = 33.dp.toPx()
                val wm = p.orb.first().copy(alpha = 0.14f)
                drawCircle(wm, wr, wc, style = Stroke(2.5.dp.toPx()))
                if (isDeposit) {
                    drawLine(wm, Offset(wc.x, wc.y - wr * 0.45f), Offset(wc.x, wc.y + wr * 0.45f), 3.dp.toPx(), StrokeCap.Round)
                    drawLine(wm, Offset(wc.x - wr * 0.45f, wc.y), Offset(wc.x + wr * 0.45f, wc.y), 3.dp.toPx(), StrokeCap.Round)
                } else {
                    drawLine(wm, Offset(wc.x, wc.y + wr * 0.45f), Offset(wc.x, wc.y - wr * 0.45f), 3.dp.toPx(), StrokeCap.Round)
                    drawPath(
                        Path().apply {
                            moveTo(wc.x - wr * 0.35f, wc.y - wr * 0.1f); lineTo(wc.x, wc.y - wr * 0.45f); lineTo(wc.x + wr * 0.35f, wc.y - wr * 0.1f)
                        }, wm, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .drawBehind { drawCircle(p.glow.copy(alpha = 0.55f), radius = size.minDimension * 0.68f) }
                        .clip(CircleShape)
                        .background(Brush.linearGradient(p.orbEdge))
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(p.orb, center = Offset(30f, 26f)))
                        .drawBehind {
                            drawCircle(p.orbInset, radius = size.minDimension / 2f, style = Stroke(3.dp.toPx()), center = Offset(size.width / 2f, size.height / 2f + 2.dp.toPx()))
                            drawArc(Color.White.copy(alpha = 0.55f), 200f, 140f, false, topLeft = Offset(3.dp.toPx(), 2.dp.toPx()), size = Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val s = size.width / 24f
                        val st = 3.4f * s
                        if (isDeposit) {
                            drawLine(p.glyph, Offset(12f * s, 5f * s), Offset(12f * s, 19f * s), st, StrokeCap.Round)
                            drawLine(p.glyph, Offset(5f * s, 12f * s), Offset(19f * s, 12f * s), st, StrokeCap.Round)
                        } else {
                            drawLine(p.glyph, Offset(12f * s, 19f * s), Offset(12f * s, 5f * s), st, StrokeCap.Round)
                            drawPath(
                                Path().apply { moveTo(6f * s, 11f * s); lineTo(12f * s, 5f * s); lineTo(18f * s, 11f * s) },
                                p.glyph, style = Stroke(st, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(p.accent.copy(alpha = 0.15f))
                        .border(1.dp, p.accent.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { HaArrowRight(p.accent, 11.dp) }
            }
            Column {
                Text(
                    title,
                    style = TextStyle(
                        fontSize = 14.5.sp, fontWeight = FontWeight.Black, color = Color.White,
                        shadow = Shadow(Color(0x80000000), Offset(0f, 2f), 4f)
                    ),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.accent),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
