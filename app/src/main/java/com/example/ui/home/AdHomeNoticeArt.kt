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

// Home page part 2 artwork: notice bar, refer card, deposit / withdraw cards.
// VISUAL ONLY: the callers keep every text, click action and test tag.

internal val HaGoldLight = Color(0xFFFFF3C4)
internal val HaGold = Color(0xFFFFD45A)
internal val HaGoldMid = Color(0xFFE0A82A)
internal val HaGoldDark = Color(0xFF8A5A08)
internal val HaInk = Color(0xFF3A2200)
internal val HaCtaFill = Brush.verticalGradient(listOf(Color(0xFFFFE98A), Color(0xFFFFC83A), Color(0xFFE09A18)))

/**
 * Small "press" animation for any tappable card or button: it sinks a little while pressed
 * and springs back with a soft bounce. Pass the same [source] to the clickable. VISUAL ONLY.
 */
fun Modifier.adPressScale(source: MutableInteractionSource, pressedScale: Float = 0.95f): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 520f),
        label = "adPress"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

private val NoticeTagShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width * 0.84f, size.height)
    lineTo(0f, size.height)
    close()
}

/** Glass light on the upper part of a card. */
internal fun DrawScope.haGlassTop(fraction: Float, alpha: Float, corner: Float) {
    drawRoundRect(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = alpha), Color.White.copy(alpha = 0f)),
            startY = 0f, endY = size.height * fraction
        ),
        size = Size(size.width, size.height * fraction),
        cornerRadius = CornerRadius(corner)
    )
}

/** A soft diagonal light streak; `x` is where it crosses the top edge. */
internal fun DrawScope.haStreak(x: Float, width: Float, alpha: Float) {
    rotate(24f, pivot = Offset(x, size.height / 2f)) {
        drawRect(
            Brush.horizontalGradient(
                listOf(Color(0x00FFF0BE), Color(0xFFFFF0BE).copy(alpha = alpha), Color(0x00FFF0BE)),
                startX = x - width / 2f, endX = x + width / 2f
            ),
            topLeft = Offset(x - width / 2f, -size.height),
            size = Size(width, size.height * 3f)
        )
    }
}

@Composable
internal fun HaChevron(color: Color, size: Dp, stroke: Float = 3f) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width / 24f
        val p = Path().apply { moveTo(9f * s, 6f * s); lineTo(15f * s, 12f * s); lineTo(9f * s, 18f * s) }
        drawPath(p, color, style = Stroke(width = stroke * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
internal fun HaArrowRight(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val s = this.size.width / 24f
        val st = Stroke(width = 3f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(color, Offset(5f * s, 12f * s), Offset(19f * s, 12f * s), strokeWidth = 3f * s, cap = StrokeCap.Round)
        drawPath(Path().apply { moveTo(13f * s, 6f * s); lineTo(19f * s, 12f * s); lineTo(13f * s, 18f * s) }, color, style = st)
    }
}

// ---------------------------------------------------------------- 1. notice bar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdNoticeBarArt(
    message: String,
    tagText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val anim = rememberInfiniteTransition(label = "notice")
    val shine by anim.animateFloat(
        initialValue = -0.2f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "noticeShine"
    )
    val dot by anim.animateFloat(
        initialValue = 0.45f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "noticeDot"
    )
    val outer = RoundedCornerShape(13.dp)
    val press = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .adPressScale(press, 0.97f)
            .fillMaxWidth()
            .shadow(8.dp, outer, ambientColor = Color(0xFF050A3C), spotColor = Color(0xFF050A3C))
            .clip(outer)
            .background(Brush.horizontalGradient(listOf(HaGoldLight, HaGoldMid, Color(0xFF6B4A10), HaGoldMid, HaGoldLight)))
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
            .padding(1.3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(Color(0xF22834AA), Color(0xFA10165A))))
            .drawBehind {
                haGlassTop(0.48f, 0.14f, 12.dp.toPx())
                haStreak(size.width * shine, 26.dp.toPx(), 0.28f)
            }
            .height(40.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(NoticeTagShape)
                    .background(HaCtaFill)
                    .drawBehind {
                        drawRect(Color.White.copy(alpha = 0.6f), size = Size(size.width, 1.dp.toPx()))
                    }
                    .padding(start = 9.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Canvas(modifier = Modifier.size(16.dp)) {
                    val s = size.width / 24f
                    val c = Color(0xFF4A2C00)
                    drawPath(
                        Path().apply {
                            moveTo(3f * s, 10f * s); lineTo(3f * s, 14f * s); lineTo(6f * s, 14f * s)
                            lineTo(11f * s, 18f * s); lineTo(11f * s, 6f * s); lineTo(6f * s, 10f * s); close()
                        }, c
                    )
                    drawArc(c, -50f, 100f, false, Offset(10f * s, 7.5f * s), Size(7f * s, 9f * s), style = Stroke(2f * s, cap = StrokeCap.Round))
                    drawArc(c, -55f, 110f, false, Offset(9f * s, 3.5f * s), Size(11f * s, 17f * s), style = Stroke(2f * s, cap = StrokeCap.Round))
                }
                Text(tagText, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Black, color = HaInk), maxLines = 1)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .drawBehind { drawCircle(Color(0xFFF43F5E).copy(alpha = 0.5f * dot), radius = size.minDimension) }
                    .clip(CircleShape)
                    .background(Color(0xFFF43F5E).copy(alpha = dot))
            )
            Text(
                text = message,
                style = TextStyle(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = Color.White,
                    shadow = Shadow(Color(0x80000000), Offset(0f, 2f), 4f)
                ),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
                    .basicMarquee(iterations = Int.MAX_VALUE)
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(HaGold.copy(alpha = 0.12f))
                    .border(1.dp, HaGold.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                HaChevron(HaGold, 11.dp)
            }
        }
    }
}
