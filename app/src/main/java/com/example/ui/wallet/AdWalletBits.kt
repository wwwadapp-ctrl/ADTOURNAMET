package com.example.ui.wallet

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate as drawScopeRotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Wallet pages artwork, part 2: label pill, light rays, floating picture. VISUAL ONLY.

/** Glowing label pill with a blinking dot. */
@Composable
fun WlPill(text: String, dot: Color, bg: List<Color>, border: Color, textColor: Color, modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "wlPill")
    val tw by anim.animateFloat(0.3f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "wlPillTw")
    val shape = RoundedCornerShape(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .shadow(6.dp, shape, ambientColor = border, spotColor = border)
            .clip(shape)
            .background(Brush.verticalGradient(bg))
            .border(1.5.dp, border, shape)
            .padding(start = 10.dp, end = 14.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Box(Modifier.size(9.dp).graphicsLayer { alpha = tw }.clip(CircleShape).background(dot))
        Spacer(Modifier.width(7.dp))
        Text(text, fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = textColor, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
    }
}

/** Slowly turning light rays, to put behind a picture. */
@Composable
fun WlRays(size: Dp, modifier: Modifier = Modifier, color: Color = Color(0xFFFFD678)) {
    val anim = rememberInfiniteTransition(label = "wlRays")
    val spin by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "wlSpin")
    Canvas(modifier = modifier.requiredSize(size)) {
        val r = this.size.minDimension / 2f
        val brush = Brush.radialGradient(listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0f)), radius = r)
        var a = spin
        repeat(18) {
            val r1 = Math.toRadians(a.toDouble()); val r2 = Math.toRadians((a + 8f).toDouble())
            drawPath(Path().apply {
                moveTo(center.x, center.y)
                lineTo(center.x + r * cos(r1).toFloat(), center.y + r * sin(r1).toFloat())
                lineTo(center.x + r * cos(r2).toFloat(), center.y + r * sin(r2).toFloat())
                close()
            }, brush)
            a += 20f
        }
    }
}

/** A picture that gently floats up and down. */
@Composable
fun WlImage(@DrawableRes res: Int, size: Dp, modifier: Modifier = Modifier, tilt: Boolean = false) {
    val anim = rememberInfiniteTransition(label = "wlImg")
    val bob by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "wlBob")
    Image(
        painter = painterResource(id = res),
        contentDescription = null,
        modifier = modifier.size(size).graphicsLayer {
            if (tilt) rotationZ = -10f + 22f * bob else translationY = -4f * density * bob
        }
    )
}
