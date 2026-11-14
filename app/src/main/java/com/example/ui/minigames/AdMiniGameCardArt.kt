package com.example.ui.minigames

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as drawScopeRotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.home.adPressScale

// Home page part 4 artwork (mini games): one royal blue card with a gold frame, carved corners,
// a corner ribbon, a gold medal, the animated picture, the title and a raised orange button. VISUAL ONLY.

private fun mgRibbon(type: MiniGameType): Triple<Color, Color, Color> = when (type) {
    MiniGameType.SPIN -> Triple(Color(0xFFF97316), Color(0xFFB91C1C), MgGoldLight)
    MiniGameType.DICE, MiniGameType.EVEN_ODD -> Triple(Color(0xFF34D399), Color(0xFF047857), MgGoldLight)
    MiniGameType.HEAD_TAIL -> Triple(Color(0xFF22D3EE), Color(0xFF0E7490), MgGoldLight)
    MiniGameType.SCRATCH -> Triple(Color(0xFF3A3A3A), Color(0xFF111111), MgGold)
}

private fun mgCardBg(type: MiniGameType): List<Color> =
    if (type == MiniGameType.SCRATCH) listOf(Color(0xFF3B2FA8), Color(0xFF2A2290), Color(0xFF1A166A))
    else listOf(Color(0xFF2B3AA8), Color(0xFF1E2A86), Color(0xFF151E66))

/** Carved gold corner (top-left shape; flip it with scale for the other corners). */
@Composable
private fun MgCorner(modifier: Modifier) {
    Canvas(modifier = modifier.size(26.dp)) {
        val s = size.width / 26f
        val st = Stroke(1.3f * s, cap = StrokeCap.Round)
        val gold = Color(0xFFE0A82A)
        drawPath(Path().apply { moveTo(2f * s, 24f * s); lineTo(2f * s, 8f * s); quadraticBezierTo(2f * s, 2f * s, 8f * s, 2f * s); lineTo(24f * s, 2f * s) }, gold, style = st)
        drawPath(Path().apply { moveTo(6f * s, 20f * s); quadraticBezierTo(6f * s, 6f * s, 20f * s, 6f * s) }, gold, style = st)
        drawPath(Path().apply { moveTo(6f * s, 12f * s); quadraticBezierTo(9f * s, 11f * s, 10f * s, 8f * s); quadraticBezierTo(6f * s, 8f * s, 6f * s, 12f * s); close() }, gold)
        drawCircle(MgGold, 1.6f * s, Offset(10f * s, 10f * s))
    }
}

@Composable
fun AdMiniGameCard(
    type: MiniGameType,
    title: String,
    subtitle: String,
    ribbonText: String,
    multiplierText: String,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val anim = rememberInfiniteTransition(label = "mgCard")
    val sweep by anim.animateFloat(
        initialValue = -0.4f, targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            tween(3400, delayMillis = when (type) { MiniGameType.SPIN -> 0; MiniGameType.HEAD_TAIL -> 1200; MiniGameType.SCRATCH -> 1800; else -> 600 }, easing = FastOutSlowInEasing)
        ),
        label = "mgSweep"
    )
    val twinkle by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "mgTwinkle")
    val press = remember { MutableInteractionSource() }
    val outer = RoundedCornerShape(16.dp)
    val ribbon = mgRibbon(type)
    val dots = remember(type) {
        val r = java.util.Random(type.ordinal.toLong() + 7L)
        List(9) { floatArrayOf(14f + r.nextInt(136), 20f + r.nextInt(110), listOf(2f, 2f, 3f, 4f)[r.nextInt(4)], r.nextFloat()) }
    }

    Box(
        modifier = modifier
            .adPressScale(press)
            .fillMaxWidth()
            .height(230.dp)
            .shadow(12.dp, outer, ambientColor = Color(0xFF050A3C), spotColor = Color(0xFF050A3C))
            .clip(outer)
            .background(Brush.linearGradient(listOf(MgGoldLight, MgGoldMid, MgGoldDark, MgGoldMid, MgGoldLight)))
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
            .testTag("mini_game_card_${type.name.lowercase()}")
            .padding(2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(mgCardBg(type), start = Offset(0f, 0f), end = Offset(200f, 600f)))
            .border(1.dp, Color(0x59FFD45A), RoundedCornerShape(14.dp))
            .drawBehind {
                // glass light on the upper half
                drawRect(
                    Brush.linearGradient(listOf(Color(0x24FFFFFF), Color(0x00FFFFFF)), start = Offset.Zero, end = Offset(size.width * 0.7f, size.height * 0.5f)),
                    size = Size(size.width, size.height * 0.5f)
                )
                // twinkling gold dots
                dots.forEach { d ->
                    val a = ((twinkle + d[3]) % 1f).let { if (it > 0.5f) 1f - it else it } * 2f
                    val c = Offset(d[0].dp.toPx(), d[1].dp.toPx())
                    drawCircle(Color(0xFFFFD264).copy(alpha = 0.5f * a), radius = d[2].dp.toPx() * 1.6f, center = c)
                    drawCircle(Color(0xFFFFE7A0).copy(alpha = 0.2f + 0.8f * a), radius = d[2].dp.toPx() / 2f, center = c)
                }
            }
            .drawWithContent {
                drawContent()
                // light sweep crossing the card
                val x = size.width * sweep
                drawScopeRotate(22f, pivot = Offset(x, size.height / 2f)) {
                    drawRect(
                        Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x33FFFFFF), Color(0x00FFFFFF)), startX = x - 13.dp.toPx(), endX = x + 13.dp.toPx()),
                        topLeft = Offset(x - 13.dp.toPx(), -size.height),
                        size = Size(26.dp.toPx(), size.height * 3f)
                    )
                }
            }
    ) {
        // the animated picture on its stage
        MgGameArt(type = type)

        // carved gold corners
        MgCorner(Modifier.align(Alignment.TopStart).padding(4.dp))
        MgCorner(Modifier.align(Alignment.TopEnd).padding(4.dp).scale(scaleX = -1f, scaleY = 1f))
        MgCorner(Modifier.align(Alignment.BottomStart).padding(4.dp).scale(scaleX = 1f, scaleY = -1f))
        MgCorner(Modifier.align(Alignment.BottomEnd).padding(4.dp).scale(-1f))

        // gold medal with the multiplier
        if (multiplierText.isNotBlank()) {
            MgScallopBadge(multiplierText, 46.dp, Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 6.dp))
        }

        // title, subtitle and the raised orange button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = TextStyle(
                    fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFF8E6), textAlign = TextAlign.Center,
                    shadow = Shadow(MgBrown, Offset(0f, 3f), 2f)
                ),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = TextStyle(
                    fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFE7A0), textAlign = TextAlign.Center,
                    shadow = Shadow(Color(0x99000000), Offset(0f, 2f), 3f)
                ),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(7.dp))
            Box(modifier = Modifier.fillMaxWidth(0.88f).height(30.dp)) {
                // the raised edge under the button
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(y = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF8A4A06))
                )
                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFD27A), Color(0xFFFFA62B), Color(0xFFE07B0A))))
                        .border(1.dp, Color(0xFFFFE7A0), RoundedCornerShape(10.dp))
                        .drawBehind {
                            drawRect(
                                Brush.verticalGradient(listOf(Color(0x8CFFFFFF), Color(0x00FFFFFF)), startY = 0f, endY = size.height * 0.5f),
                                size = Size(size.width, size.height * 0.5f)
                            )
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(width = 8.dp, height = 9.dp)) {
                        drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width, size.height / 2f); lineTo(0f, size.height); close() }, Color(0xFF3A1A00))
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(buttonText, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF3A1A00)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // diagonal ribbon across the top-left corner
        if (ribbonText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .offset(x = (-30).dp, y = 22.dp)
                    .rotate(-45f)
                    .width(118.dp)
                    .shadow(4.dp)
                    .background(Brush.verticalGradient(listOf(ribbon.first, ribbon.second)))
                    .drawBehind {
                        drawRect(Color.White.copy(alpha = 0.5f), size = Size(size.width, 1.dp.toPx()))
                        drawRect(Color.Black.copy(alpha = 0.4f), topLeft = Offset(0f, size.height - 1.dp.toPx()), size = Size(size.width, 1.dp.toPx()))
                    }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    ribbonText,
                    style = TextStyle(
                        fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = ribbon.third, textAlign = TextAlign.Center,
                        shadow = Shadow(Color(0x59000000), Offset(0f, 1f), 1f)
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
