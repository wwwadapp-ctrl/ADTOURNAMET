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

// Home page part 2 artwork: the refer card. VISUAL ONLY.

// ---------------------------------------------------------------- 2. refer card

private fun DrawScope.haGiftBox(left: Float, top: Float, box: Float) {
    val s = box / 48f
    fun o(x: Float, y: Float) = Offset(left + x * s, top + y * s)
    val gold = Brush.verticalGradient(
        listOf(Color(0xFFFFF0A8), Color(0xFFFFC83A), Color(0xFFB9780C)),
        startY = top + 15f * s, endY = top + 41f * s
    )
    val red = Brush.verticalGradient(listOf(Color(0xFFFF7A8A), Color(0xFFB3122F)), startY = top + 6f * s, endY = top + 41f * s)
    drawRoundRect(gold, o(9f, 21f), Size(30f * s, 20f * s), CornerRadius(2.5f * s))
    drawRoundRect(HaGoldDark.copy(alpha = 0.28f), o(25f, 21f), Size(14f * s, 20f * s), CornerRadius(2f * s))
    drawRoundRect(gold, o(6f, 15f), Size(36f * s, 8f * s), CornerRadius(2.5f * s))
    drawRoundRect(HaGoldDark, o(6f, 15f), Size(36f * s, 8f * s), CornerRadius(2.5f * s), style = Stroke(0.7f * s))
    drawRect(red, o(21f, 15f), Size(6f * s, 26f * s))
    val bow = Path().apply {
        moveTo(o(24f, 15f).x, o(24f, 15f).y)
        cubicTo(o(20.5f, 8f).x, o(20.5f, 8f).y, o(12f, 7f).x, o(12f, 7f).y, o(13f, 12.5f).x, o(13f, 12.5f).y)
        cubicTo(o(13.6f, 15.3f).x, o(13.6f, 15.3f).y, o(20f, 15.3f).x, o(20f, 15.3f).y, o(24f, 15f).x, o(24f, 15f).y)
        cubicTo(o(27.5f, 8f).x, o(27.5f, 8f).y, o(36f, 7f).x, o(36f, 7f).y, o(35f, 12.5f).x, o(35f, 12.5f).y)
        cubicTo(o(34.4f, 15.3f).x, o(34.4f, 15.3f).y, o(28f, 15.3f).x, o(28f, 15.3f).y, o(24f, 15f).x, o(24f, 15f).y)
        close()
    }
    drawPath(bow, red)
    drawPath(bow, Color(0xFF7A0A1E), style = Stroke(0.7f * s))
    drawRoundRect(Color(0xFFFFFBE8).copy(alpha = 0.75f), o(9.5f, 16.2f), Size(11f * s, 2f * s), CornerRadius(1f * s))
    drawCircle(Color(0xFFFFF8D8), 1.4f * s, o(40f, 12f))
    drawCircle(Color(0xFFFFF8D8), 1f * s, o(7f, 10f))
}

@Composable
fun AdReferCardArt(
    titleBefore: String,
    amountText: String,
    titleAfter: String,
    subtitle: String,
    ctaText: String,
    tagText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val anim = rememberInfiniteTransition(label = "refer")
    val burst by anim.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)), label = "referBurst"
    )
    val bob by anim.animateFloat(
        initialValue = 0f, targetValue = -3f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "referFloat"
    )
    val outer = RoundedCornerShape(18.dp)
    val press = remember { MutableInteractionSource() }
    Box(modifier = modifier.adPressScale(press).fillMaxWidth().padding(top = 9.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, outer, ambientColor = Color(0xFF050A3C), spotColor = Color(0xFF050A3C))
                .clip(outer)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFF8D8), HaGold, HaGoldDark, Color(0xFFFFE07A), HaGoldDark, HaGoldLight)
                    )
                )
                .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
                .testTag("home_referral_banner")
                .padding(1.6.dp)
                .clip(RoundedCornerShape(16.5.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF5B2FB8), Color(0xFF3A2DB0), Color(0xFF2232A2), Color(0xFF141C66)))
                )
                .drawBehind {
                    val gc = Offset(42.dp.toPx(), size.height / 2f)
                    drawCircle(
                        Brush.radialGradient(listOf(Color(0x59FFC850), Color(0x00FFC850)), center = gc, radius = 90.dp.toPx()),
                        radius = 90.dp.toPx(), center = gc
                    )
                    // sunburst behind the gift
                    val rayLen = 78.dp.toPx()
                    val rays = Brush.radialGradient(
                        listOf(Color(0x4DFFDC82), Color(0x00FFDC82)), center = gc, radius = rayLen
                    )
                    var a = burst
                    repeat(20) {
                        val r1 = Math.toRadians(a.toDouble()); val r2 = Math.toRadians((a + 7f).toDouble())
                        drawPath(
                            Path().apply {
                                moveTo(gc.x, gc.y)
                                lineTo(gc.x + rayLen * cos(r1).toFloat(), gc.y + rayLen * sin(r1).toFloat())
                                lineTo(gc.x + rayLen * cos(r2).toFloat(), gc.y + rayLen * sin(r2).toFloat())
                                close()
                            }, rays
                        )
                        a += 18f
                    }
                    haGlassTop(0.46f, 0.16f, 16.dp.toPx())
                    haStreak(size.width * 0.58f, 34.dp.toPx(), 0.22f)
                    // confetti and sparkles
                    val w = size.width; val h = size.height
                    drawRect(Color(0xFFFFE07A), Offset(w * 0.40f, h * 0.12f), Size(4.dp.toPx(), 4.dp.toPx()))
                    drawCircle(Color(0xFF5EEAD4), 2.5.dp.toPx(), Offset(w * 0.70f, h * 0.80f))
                    drawRect(Color(0xFFFF6B8A), Offset(w * 0.33f, h * 0.84f), Size(6.dp.toPx(), 3.dp.toPx()))
                    drawRect(Color(0xFFFFE07A), Offset(w * 0.80f, h * 0.10f), Size(6.dp.toPx(), 3.dp.toPx()))
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 84.dp, end = 10.dp, top = 14.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (titleBefore.isNotBlank()) {
                        Text(
                            titleBefore,
                            style = TextStyle(
                                fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                                shadow = Shadow(Color(0x8C000000), Offset(0f, 2f), 4f)
                            ),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (amountText.isNotBlank()) {
                            Text(
                                amountText,
                                style = TextStyle(
                                    brush = Brush.verticalGradient(
                                        listOf(Color(0xFFFFFBE8), Color(0xFFFFE07A), Color(0xFFF2B21E), Color(0xFFC98A10))
                                    ),
                                    fontSize = 19.sp, fontWeight = FontWeight.Black,
                                    shadow = Shadow(Color(0xFF3A2600), Offset(0f, 3f), 0f)
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            titleAfter,
                            style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = Color.White),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        subtitle,
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HaGold),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Bottom)
                        .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black, spotColor = Color(0xFFFFC850))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFF8D8), HaGoldDark)))
                        .padding(1.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(HaCtaFill)
                        .drawBehind {
                            drawRoundRect(
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0f)),
                                    startY = 0f, endY = size.height * 0.48f
                                ),
                                topLeft = Offset(6.dp.toPx(), 1.dp.toPx()),
                                size = Size(size.width - 12.dp.toPx(), size.height * 0.45f),
                                cornerRadius = CornerRadius(10.dp.toPx())
                            )
                        }
                        .padding(start = 11.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(ctaText, style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = HaInk), maxLines = 1)
                        Box(
                            modifier = Modifier.size(16.dp).clip(CircleShape).background(HaInk),
                            contentAlignment = Alignment.Center
                        ) { HaChevron(HaGold, 9.dp, 4f) }
                    }
                }
            }
            // the gift, floating a little
            Canvas(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 9.dp)
                    .offset(y = bob.dp)
                    .size(66.dp)
            ) {
                drawCircle(Color(0x8CFFD25A), radius = size.minDimension * 0.42f, center = Offset(size.width / 2f, size.height * 0.62f))
                haGiftBox(0f, 0f, size.minDimension)
            }
        }
        // red ribbon tag on the top edge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 92.dp, y = (-9).dp)
                .shadow(5.dp, RoundedCornerShape(6.dp), ambientColor = Color(0xFFD11A45), spotColor = Color(0xFFD11A45))
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFF6B8A), Color(0xFFD11A45))))
                .border(1.dp, Color(0xFFFFC1CF), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Text(tagText, style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 0.3.sp), maxLines = 1)
        }
    }
}
