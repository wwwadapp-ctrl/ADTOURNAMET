package com.example.ui.minigames

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

// Home page part 4 artwork (mini games): the animated game pictures on their stage.
// The 4 pictures are app/src/main/res/drawable-nodpi/mg_wheel / mg_dice / mg_coin / mg_scratch.webp. VISUAL ONLY.

@Composable
private fun MgImage(@DrawableRes res: Int, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = res),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}

/** Glowing gold stage (104 x 30 dp). */
@Composable
private fun MgPedestal(glow: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 104.dp, height = 30.dp)) {
        val w = size.width
        drawOval(
            Brush.radialGradient(listOf(Color(0xFFFFD45A).copy(alpha = 0.55f * glow), Color.Transparent), center = Offset(w / 2f, 12.dp.toPx()), radius = w * 0.62f),
            topLeft = Offset(-10.dp.toPx(), -6.dp.toPx()), size = Size(w + 20.dp.toPx(), 36.dp.toPx())
        )
        drawOval(
            Brush.verticalGradient(listOf(Color(0xFFB9A0D8), Color(0xFF4A3A7A)), startY = 8.dp.toPx(), endY = 28.dp.toPx()),
            topLeft = Offset(0f, 8.dp.toPx()), size = Size(w, 20.dp.toPx())
        )
        drawOval(
            Brush.radialGradient(
                listOf(MgGoldLight, MgGoldMid, MgGoldDark),
                center = Offset(w / 2f, 9.dp.toPx()), radius = 48.dp.toPx()
            ),
            topLeft = Offset(6.dp.toPx(), 4.dp.toPx()), size = Size(w - 12.dp.toPx(), 16.dp.toPx())
        )
    }
}

/**
 * The picture for each game, laid out inside a 171 x 160 dp area at the top of the card.
 * Wheel spins, dice hop, coin flips with a light ring, scratch card floats with gold confetti.
 */
@Composable
internal fun MgGameArt(type: MiniGameType, modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "mgArt")
    val spin by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "mgSpin")
    val flip by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "mgFlip")
    val orbit by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "mgOrbit")
    val bob by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "mgBob")
    val glow by anim.animateFloat(0.6f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "mgGlow")

    Box(modifier = modifier.fillMaxWidth().height(160.dp)) {
        when (type) {
            MiniGameType.SPIN -> {
                Canvas(modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp).size(124.dp)) {
                    drawCircle(Brush.radialGradient(listOf(Color(0x8CFFD25A), Color.Transparent)))
                }
                Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 28.dp).size(112.dp)) {
                    MgImage(R.drawable.mg_wheel, 112.dp, Modifier.graphicsLayer { rotationZ = spin })
                    // fixed gold pointer with a gem, on top
                    Canvas(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-8).dp)
                            .size(width = 14.dp, height = 22.dp)
                    ) {
                        val w = size.width; val h = size.height
                        val p = Path().apply {
                            moveTo(w / 2f, h)
                            lineTo(0f, w / 2f)
                            arcTo(androidx.compose.ui.geometry.Rect(0f, 0f, w, w), 180f, 180f, false)
                            close()
                        }
                        drawPath(p, Brush.verticalGradient(listOf(MgGold, MgGoldDark)))
                        drawPath(p, MgGoldDark, style = Stroke(1.dp.toPx()))
                        drawCircle(Color(0xFF5EEAD4), radius = w * 0.25f, center = Offset(w / 2f, w / 2f))
                        drawCircle(MgGoldLight, radius = w * 0.25f, center = Offset(w / 2f, w / 2f), style = Stroke(1.dp.toPx()))
                    }
                }
            }
            MiniGameType.DICE, MiniGameType.EVEN_ODD -> {
                MgPedestal(glow, Modifier.align(Alignment.TopCenter).padding(top = 112.dp))
                MgImage(
                    R.drawable.mg_dice, 68.dp,
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 18.dp, y = 42.dp)
                        .graphicsLayer { translationY = -10.dp.toPx() * bob; rotationZ = -12f + 10f * bob }
                )
                MgImage(
                    R.drawable.mg_dice, 62.dp,
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-16).dp, y = 52.dp)
                        .graphicsLayer { translationY = -4.dp.toPx() + 8.dp.toPx() * bob; rotationZ = 14f + 12f * bob }
                )
                Text(
                    "★",
                    style = TextStyle(fontSize = 15.sp, color = MgGold.copy(alpha = 0.3f + 0.7f * bob), shadow = Shadow(MgGold, Offset.Zero, 8f)),
                    modifier = Modifier.align(Alignment.TopStart).offset(x = 80.dp, y = 22.dp)
                )
            }
            MiniGameType.HEAD_TAIL -> {
                MgPedestal(glow, Modifier.align(Alignment.TopCenter).padding(top = 114.dp))
                MgImage(
                    R.drawable.mg_coin, 96.dp,
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 22.dp)
                        .graphicsLayer {
                            translationY = -6.dp.toPx() * bob
                            rotationY = flip
                            cameraDistance = 12f * density
                        }
                )
                // light ring circling the coin
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 58.dp)
                        .size(width = 140.dp, height = 40.dp)
                        .graphicsLayer { rotationZ = -12f }
                ) {
                    rotate(0f) {
                        drawArc(
                            Brush.sweepGradient(listOf(Color.Transparent, Color(0xBFFFE18C), Color.Transparent), center = center),
                            startAngle = orbit, sweepAngle = 260f, useCenter = false,
                            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
            MiniGameType.SCRATCH -> {
                MgImage(
                    R.drawable.mg_scratch, 132.dp,
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 22.dp)
                        .graphicsLayer { translationY = -6.dp.toPx() * bob }
                )
                // gold confetti pieces
                Canvas(modifier = Modifier.matchParentSize()) {
                    val pieces = listOf(
                        floatArrayOf(18f, 40f, 9f, 30f), floatArrayOf(140f, 34f, 8f, -40f), floatArrayOf(150f, 120f, 10f, 20f),
                        floatArrayOf(24f, 128f, 7f, -25f), floatArrayOf(132f, 80f, 6f, 60f)
                    )
                    pieces.forEachIndexed { i, f ->
                        val a = if (i % 2 == 0) 0.3f + 0.7f * bob else 1f - 0.7f * bob
                        val tl = Offset(f[0].dp.toPx(), f[1].dp.toPx())
                        rotate(f[3], pivot = tl) {
                            drawRect(
                                Brush.horizontalGradient(listOf(Color(0xFFFFE7A0), MgGoldDeep), startX = tl.x, endX = tl.x + f[2].dp.toPx()),
                                topLeft = tl, size = Size(f[2].dp.toPx(), 4.dp.toPx()), alpha = a
                            )
                        }
                    }
                }
            }
        }
    }
}
