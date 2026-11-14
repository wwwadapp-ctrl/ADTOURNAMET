package com.example.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.splash.AdSplashBoard
import kotlin.math.cos
import kotlin.math.sin

// Home page part 5 artwork: the two animated boards on the auto game cards. VISUAL ONLY.

/** The real 3D ludo board from the splash page: pawns hop, our red dice picture rolls, a gold ring glows around it. */
@Composable
internal fun AdAutoLudoArt(modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "autoLudoArt")
    val dicePhase by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "alDice")
    val hop by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "alHop")
    val pulse by anim.animateFloat(0.6f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "alPulse")
    val bob by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "alBob")

    Box(modifier = modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.TopCenter) {
        // soft gold light behind the board
        Canvas(modifier = Modifier.padding(top = 22.dp).size(width = 176.dp, height = 120.dp)) {
            drawOval(Brush.radialGradient(listOf(Color(0x73FFD25A), Color.Transparent)))
        }
        Box(modifier = Modifier.padding(top = 26.dp).graphicsLayer { translationY = -5.dp.toPx() * bob }) {
            AdSplashBoard(boardSize = 112.dp, dicePhase = dicePhase, pulse = pulse, pawnHop = hop, imageDice = true)
        }
        // glowing gold ring around the board
        Canvas(modifier = Modifier.padding(top = 48.dp).size(width = 178.dp, height = 58.dp)) {
            rotate(-14f) {
                drawOval(
                    Brush.sweepGradient(listOf(Color(0x00FFE18C), Color(0xFFFFF5C8), Color(0x66FFE18C), Color(0x00FFE18C)), center = center),
                    style = Stroke(2.dp.toPx()),
                    alpha = 0.35f + 0.65f * pulse
                )
            }
        }
    }
}

/** Our carrom board picture laid in 3D (same angle as the ludo board) with coins that scatter when the striker hits. */
@Composable
internal fun AdCarromArt(modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "carromArt")
    val p by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "crPhase")
    val bob by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "crBob")

    Box(modifier = modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.TopCenter) {
        Canvas(modifier = Modifier.padding(top = 22.dp).size(width = 176.dp, height = 120.dp)) {
            drawOval(Brush.radialGradient(listOf(Color(0x66FFD25A), Color.Transparent)))
        }
        Box(modifier = Modifier.padding(top = 24.dp).graphicsLayer { translationY = -5.dp.toPx() * bob }) {
            CarromBoard3D(boardSize = 118.dp, p = p)
        }
    }
}

@Composable
private fun CarromBoard3D(boardSize: Dp, p: Float) {
    val diag = boardSize * 1.41421f
    val squash = 0.56f
    val thick = 8.dp
    Box(modifier = Modifier.width(diag).height(diag * squash + thick + 4.dp)) {
        // shadow and the wooden side of the board
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dPx = diag.toPx()
            val cx = dPx / 2f; val cy = dPx * squash / 2f
            val th = thick.toPx()
            drawOval(
                Brush.radialGradient(listOf(Color(0x8C000000), Color.Transparent), center = Offset(cx, cy + 22.dp.toPx()), radius = dPx * 0.52f),
                topLeft = Offset(cx - dPx * 0.52f, cy + 22.dp.toPx() - dPx * 0.19f), size = Size(dPx * 1.04f, dPx * 0.38f)
            )
            val l = Offset(0f, cy); val b = Offset(cx, cy * 2f); val r = Offset(dPx, cy)
            drawPath(
                Path().apply { moveTo(l.x, l.y); lineTo(b.x, b.y); lineTo(b.x, b.y + th); lineTo(l.x, l.y + th); close() },
                Brush.verticalGradient(listOf(Color(0xFF5A2A14), Color(0xFF2A1208)), startY = l.y, endY = b.y + th)
            )
            drawPath(
                Path().apply { moveTo(b.x, b.y); lineTo(r.x, r.y); lineTo(r.x, r.y + th); lineTo(b.x, b.y + th); close() },
                Color(0xFF3A1A0A)
            )
        }
        // the board picture, turned 45 degrees and laid flat
        Box(
            modifier = Modifier
                .offset(y = -(thick + 4.dp) / 2f)
                .requiredSize(diag)
                .graphicsLayer { scaleY = squash },
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(boardSize).graphicsLayer { rotationZ = 45f }) {
                Image(
                    painter = painterResource(id = R.drawable.mg_carrom),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
                Canvas(modifier = Modifier.fillMaxSize()) { drawCarromPieces(p) }
            }
        }
    }
}

private fun easeOut(t: Float): Float = 1f - (1f - t) * (1f - t)

/** Coins and the striker, in board coordinates. p goes 0..1 once per shot. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCarromPieces(p: Float) {
    val s = size.width
    val c = Offset(s / 2f, s / 2f)
    val r = s * 0.052f
    // how far the coins are pushed out: 0 = packed in the centre, 1 = scattered
    val spread = when {
        p < 0.24f -> 0f
        p < 0.45f -> easeOut((p - 0.24f) / 0.21f)
        p < 0.8f -> 1f
        else -> 1f - easeOut((p - 0.8f) / 0.2f)
    }
    fun coin(at: Offset, top: List<Color>, side: Color) {
        drawCircle(Color(0x73000000), r * 1.05f, at + Offset(r * 0.35f, r * 0.35f))
        drawCircle(side, r, at + Offset(r * 0.18f, r * 0.18f))
        drawCircle(Brush.radialGradient(top, center = at + Offset(-r * 0.3f, -r * 0.3f), radius = r * 1.3f), r, at)
        drawCircle(Color(0x26000000), r * 0.7f, at, style = Stroke(r * 0.12f))
    }
    // the queen in the middle
    coin(c + Offset(0f, -s * 0.12f * spread), listOf(Color(0xFFFF9A9A), Color(0xFFE11D2E), Color(0xFF8B0F1A)), Color(0xFF5A0A10))
    // six coins around it, white and black
    for (i in 0 until 6) {
        val a = (i * 60.0 + 17.0) * Math.PI / 180.0
        val d = s * (0.11f + 0.13f * spread)
        val at = c + Offset((d * cos(a)).toFloat(), (d * sin(a)).toFloat())
        if (i % 2 == 0) coin(at, listOf(Color.White, Color(0xFFF4EBD6), Color(0xFFC8B48E)), Color(0xFF8E7A55))
        else coin(at, listOf(Color(0xFF7A7A7A), Color(0xFF262626), Color(0xFF050505)), Color.Black)
    }
    // the striker comes in from the baseline and hits
    if (p < 0.3f) {
        val t = (p / 0.25f).coerceAtMost(1f)
        val at = Offset(s / 2f, s * (0.8f - 0.16f * t))
        val alpha = if (p < 0.25f) 1f else 1f - (p - 0.25f) / 0.05f
        drawCircle(Color(0xFFFFDC78).copy(alpha = 0.6f * alpha), r * 1.9f, at)
        drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFEEF3FF), Color(0xFF9FB2D6)), center = at + Offset(-r * 0.3f, -r * 0.3f), radius = r * 1.5f), r * 1.2f, at, alpha = alpha)
        drawCircle(Color(0xFFFFD45A).copy(alpha = alpha), r * 1.2f, at, style = Stroke(r * 0.25f))
    }
}
