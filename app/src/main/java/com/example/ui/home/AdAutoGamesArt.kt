package com.example.ui.home

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

// Home page part 5 artwork: the "অটো গেম" heading and the auto game card frame. VISUAL ONLY:
// the caller keeps every text, click action and test tag.

private val AgOrangeGold = Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC83A), Color(0xFFFF8A1A)))

/** "অটো গেম" in big orange-gold letters with stars, diamonds and gold lines. */
@Composable
fun AdAutoGamesHeading(text: String, modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "agHeading")
    val glow by anim.animateFloat(0.55f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "agGlow")
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(modifier = Modifier.weight(1f).height(1.5.dp).background(Brush.horizontalGradient(listOf(Color(0x00FFD45A), Color(0xFFFFD45A)))))
        Box(modifier = Modifier.size(6.dp).rotate(45f).background(Color(0xFFFFE7A0)))
        Text("★", style = TextStyle(fontSize = 20.sp, color = Color(0xFFFFD45A).copy(alpha = glow), shadow = Shadow(Color(0xFF8A5A08), Offset(0f, 3f), 2f)))
        Text(
            text = text.replace("★", "").trim(),
            style = TextStyle(brush = AgOrangeGold, fontSize = 26.sp, fontWeight = FontWeight.Black, shadow = Shadow(Color(0xFF5A2A00), Offset(0f, 4f), 0f)),
            maxLines = 1
        )
        Text("★", style = TextStyle(fontSize = 20.sp, color = Color(0xFFFFD45A).copy(alpha = glow), shadow = Shadow(Color(0xFF8A5A08), Offset(0f, 3f), 2f)))
        Box(modifier = Modifier.size(6.dp).rotate(45f).background(Color(0xFFFFE7A0)))
        Box(modifier = Modifier.weight(1f).height(1.5.dp).background(Brush.horizontalGradient(listOf(Color(0xFFFFD45A), Color(0x00FFD45A)))))
    }
}

/** Curly gold corner (top-left shape; flipped with scale for the other corners). */
@Composable
private fun AgFlourish(modifier: Modifier) {
    Canvas(modifier = modifier.size(30.dp)) {
        val s = size.width / 30f
        val gold = Color(0xFFE0A82A)
        val st = Stroke(1.2f * s, cap = StrokeCap.Round)
        drawPath(Path().apply { moveTo(3f * s, 28f * s); lineTo(3f * s, 3f * s); lineTo(28f * s, 3f * s) }, gold, style = st)
        drawPath(Path().apply { moveTo(7f * s, 16f * s); quadraticBezierTo(7f * s, 7f * s, 16f * s, 7f * s) }, gold, style = st)
        drawPath(Path().apply { moveTo(9f * s, 7f * s); quadraticBezierTo(13f * s, 4f * s, 17f * s, 7f * s); quadraticBezierTo(13f * s, 9f * s, 9f * s, 7f * s); close() }, gold)
        drawPath(Path().apply { moveTo(7f * s, 9f * s); quadraticBezierTo(4f * s, 13f * s, 7f * s, 17f * s); quadraticBezierTo(9f * s, 13f * s, 7f * s, 9f * s); close() }, gold)
        drawCircle(Color(0xFFFFD45A), 1.6f * s, Offset(12f * s, 12f * s))
    }
}

/**
 * One auto game card: glassy royal blue with a gold frame, an inner gold line, curly corners, a corner ribbon,
 * the animated board ([art]), the big orange-gold title, and a raised button (orange, or gold when [goldButton]).
 */
@Composable
fun AdAutoGameCard(
    title: String,
    subtitle: String,
    ribbonText: String,
    ribbonColors: List<Color>,
    ribbonTextColor: Color,
    buttonText: String,
    goldButton: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    art: @Composable BoxScope.() -> Unit
) {
    val anim = rememberInfiniteTransition(label = "agCard")
    val sweep by anim.animateFloat(
        -0.4f, 2.4f,
        infiniteRepeatable(tween(3600, delayMillis = if (goldButton) 1400 else 0, easing = FastOutSlowInEasing)),
        label = "agSweep"
    )
    val twinkle by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "agTwinkle")
    val press = remember { MutableInteractionSource() }
    val outer = RoundedCornerShape(18.dp)
    val dots = remember(tag) {
        val r = java.util.Random(tag.hashCode().toLong())
        List(14) { floatArrayOf(10f + r.nextInt(150), 14f + r.nextInt(136), listOf(2f, 2f, 3f, 3f, 4f)[r.nextInt(5)], r.nextFloat()) }
    }

    Box(
        modifier = modifier
            .adPressScale(press)
            .fillMaxWidth()
            .height(235.dp)
            .shadow(14.dp, outer, ambientColor = Color(0xFF050A3C), spotColor = Color(0xFFFFC850))
            .clip(outer)
            .background(Brush.linearGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4))))
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
            .testTag(tag)
            .padding(1.5.dp)
            .clip(RoundedCornerShape(16.5.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF26318F), Color(0xFF1B2470), Color(0xFF141A58))))
            .drawBehind {
                val w = size.width; val h = size.height
                // blue glow behind the board
                drawOval(
                    Brush.radialGradient(listOf(Color(0x596E78FF), Color.Transparent), center = Offset(w / 2f, h * 0.3f), radius = w * 0.65f),
                    topLeft = Offset(w * -0.15f, h * -0.05f), size = Size(w * 1.3f, h * 0.7f)
                )
                // diagonal glass light
                drawScopeRotate(25f, pivot = Offset(w * 0.15f, h * 0.3f)) {
                    drawRect(
                        Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x14FFFFFF), Color(0x1FFFFFFF), Color(0x00FFFFFF)), startX = -w * 0.2f, endX = w * 0.5f),
                        topLeft = Offset(-w * 0.2f, -h * 0.3f), size = Size(w * 0.7f, h * 1.6f)
                    )
                }
                // twinkling gold dots
                dots.forEach { d ->
                    val a = ((twinkle + d[3]) % 1f).let { if (it > 0.5f) 1f - it else it } * 2f
                    val c = Offset(d[0].dp.toPx(), d[1].dp.toPx())
                    drawCircle(Color(0xFFFFC85A).copy(alpha = 0.55f * a), d[2].dp.toPx() * 1.7f, c)
                    drawCircle(Color(0xFFFFE7A0).copy(alpha = 0.2f + 0.8f * a), d[2].dp.toPx() / 2f, c)
                }
                // inner thin gold frame
                drawRoundRect(
                    Color(0x8CE0A82A),
                    topLeft = Offset(7.dp.toPx(), 7.dp.toPx()),
                    size = Size(w - 14.dp.toPx(), h - 14.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(11.dp.toPx()),
                    style = Stroke(1.dp.toPx())
                )
            }
            .drawWithContent {
                drawContent()
                val x = size.width * sweep
                drawScopeRotate(22f, pivot = Offset(x, size.height / 2f)) {
                    drawRect(
                        Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x38FFFFFF), Color(0x00FFFFFF)), startX = x - 14.dp.toPx(), endX = x + 14.dp.toPx()),
                        topLeft = Offset(x - 14.dp.toPx(), -size.height), size = Size(28.dp.toPx(), size.height * 3f)
                    )
                }
            }
    ) {
        art()

        AgFlourish(Modifier.align(Alignment.TopStart).padding(5.dp))
        AgFlourish(Modifier.align(Alignment.TopEnd).padding(5.dp).scale(scaleX = -1f, scaleY = 1f))
        AgFlourish(Modifier.align(Alignment.BottomStart).padding(5.dp).scale(scaleX = 1f, scaleY = -1f))
        AgFlourish(Modifier.align(Alignment.BottomEnd).padding(5.dp).scale(-1f))

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = TextStyle(brush = AgOrangeGold, fontSize = 21.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, shadow = Shadow(Color(0xFF5A2A00), Offset(0f, 3f), 0f)),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFE7A0), textAlign = TextAlign.Center, shadow = Shadow(Color(0x99000000), Offset(0f, 2f), 3f)),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.9f).height(31.dp)) {
                Box(
                    modifier = Modifier.matchParentSize().offset(y = 4.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (goldButton) Color(0xFF6E4A06) else Color(0xFF8A4A06))
                )
                Row(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                if (goldButton) listOf(Color(0xFFFFE9A0), Color(0xFFE9B84A), Color(0xFFB98418))
                                else listOf(Color(0xFFFFD27A), Color(0xFFFFA62B), Color(0xFFE07B0A))
                            )
                        )
                        .border(1.5.dp, Color(0xFFFFE7A0), RoundedCornerShape(12.dp))
                        .drawBehind {
                            drawRect(
                                Brush.verticalGradient(listOf(Color(0x99FFFFFF), Color(0x00FFFFFF)), startY = 0f, endY = size.height * 0.5f),
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
                    Text(buttonText, style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF3A1A00)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // ribbon across the top-left corner (inside the card)
        if (ribbonText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .offset(x = (-28).dp, y = 22.dp)
                    .rotate(-45f)
                    .width(116.dp)
                    .shadow(5.dp)
                    .background(Brush.verticalGradient(ribbonColors))
                    .drawBehind {
                        drawRect(Color.White.copy(alpha = 0.6f), size = Size(size.width, 1.dp.toPx()))
                        drawRect(Color.Black.copy(alpha = 0.4f), topLeft = Offset(0f, size.height - 1.dp.toPx()), size = Size(size.width, 1.dp.toPx()))
                    }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(ribbonText, style = TextStyle(fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = ribbonTextColor, textAlign = TextAlign.Center), maxLines = 1)
            }
        }
    }
}
