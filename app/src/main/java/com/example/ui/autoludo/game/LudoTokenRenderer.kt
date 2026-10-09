package com.example.ui.autoludo.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class TokenColor(val main: Color, val light: Color, val dark: Color, val glow: Color) {
    BLUE(Color(0xFF007BFF), Color(0xFF64B5F6), Color(0xFF004BA0), Color(0xFF29B6F6)),
    GREEN(Color(0xFF00C853), Color(0xFF69F0AE), Color(0xFF007E33), Color(0xFF00E676)),
    RED(Color(0xFFD50000), Color(0xFFFF5252), Color(0xFF9B0000), Color(0xFFFF1744)),
    YELLOW(Color(0xFFFFD600), Color(0xFFFFFF72), Color(0xFFC79A00), Color(0xFFFFEA00))
}

@Composable
fun LudoTokenRenderer(
    tokenColor: TokenColor,
    isSelectable: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    onClick: () -> Unit = {}
) {
    // ঘূর্ণায়মান গিয়ার/রিং অ্যানিমেশন (চাল এলে ঘুরবে)
    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // গুটি আলতোভাবে উপরে-নিচে বাউন্স অ্যানিমেশন
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isSelectable) -6f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Box(
        modifier = modifier
            .size(size * 1.6f)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                enabled = isSelectable,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // ১. নিচে ঘূর্ণায়মান গিয়ার/সার্কেল (Selection Ring)
        if (isSelectable) {
            Canvas(modifier = Modifier.size(size * 1.45f)) {
                rotate(rotationAngle) {
                    val strokeWidth = 3.dp.toPx()
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = 0.85f),
                        radius = (size.toPx() * 0.58f),
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(12f, 10f), 0f
                            )
                        )
                    )
                    // ছোট গিয়ার দাঁত/ডটস
                    for (i in 0 until 6) {
                        val angleRad = Math.toRadians((i * 60).toDouble())
                        val cx = center.x + (size.toPx() * 0.58f) * kotlin.math.cos(angleRad).toFloat()
                        val cy = center.y + (size.toPx() * 0.58f) * kotlin.math.sin(angleRad).toFloat()
                        drawCircle(
                            color = Color(0xFFFFF176),
                            radius = 2.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }
                }
            }
        }

        // ২. ৩ডি ড্রপ-পিন গুটি (Drop-pin Pawn)
        Canvas(
            modifier = Modifier
                .size(size)
                .offset(y = bounceOffset.dp)
        ) {
            val w = this.size.width
            val h = this.size.height

            // ড্রপ শ্যাডো (মেঝেতে গুটির ছায়া)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.95f),
                    radius = w * 0.45f
                ),
                topLeft = Offset(w * 0.1f, h * 0.82f),
                size = Size(w * 0.8f, h * 0.25f)
            )

            // গুটির নিচের বেস (Base Ring)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(tokenColor.light, tokenColor.dark),
                    center = Offset(w * 0.4f, h * 0.8f),
                    radius = w * 0.4f
                ),
                topLeft = Offset(w * 0.15f, h * 0.72f),
                size = Size(w * 0.7f, h * 0.22f)
            )

            // গুটির বডি (Tapered Neck Path)
            val bodyPath = Path().apply {
                moveTo(w * 0.25f, h * 0.8f)
                cubicTo(w * 0.3f, h * 0.55f, w * 0.35f, h * 0.45f, w * 0.38f, h * 0.38f)
                lineTo(w * 0.62f, h * 0.38f)
                cubicTo(w * 0.65f, h * 0.45f, w * 0.7f, h * 0.55f, w * 0.75f, h * 0.8f)
                close()
            }
            drawPath(
                path = bodyPath,
                brush = Brush.linearGradient(
                    colors = listOf(tokenColor.light, tokenColor.main, tokenColor.dark),
                    start = Offset(w * 0.2f, h * 0.4f),
                    end = Offset(w * 0.8f, h * 0.8f)
                )
            )

            // গুটির মাথা (3D Glossy Ball/Head)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.9f), tokenColor.light, tokenColor.main, tokenColor.dark),
                    center = Offset(w * 0.4f, h * 0.22f),
                    radius = w * 0.32f
                ),
                radius = w * 0.26f,
                center = Offset(w * 0.5f, h * 0.28f)
            )

            // গ্লসি রিফ্লেকশন হাইলাইট (চকচকে ভাব)
            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = w * 0.07f,
                center = Offset(w * 0.42f, h * 0.22f)
            )
        }
    }
}

@Composable
fun LudoToken(
    color: Color,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    size: Dp = 28.dp,
    onClick: () -> Unit = {}
) {
    val tokenColor = when (color) {
        LudoRed -> TokenColor.RED
        LudoBlue -> TokenColor.BLUE
        LudoGreen -> TokenColor.GREEN
        LudoYellow -> TokenColor.YELLOW
        else -> TokenColor.RED
    }
    LudoTokenRenderer(
        tokenColor = tokenColor,
        isSelectable = isSelected,
        modifier = modifier,
        size = size,
        onClick = onClick
    )
}
