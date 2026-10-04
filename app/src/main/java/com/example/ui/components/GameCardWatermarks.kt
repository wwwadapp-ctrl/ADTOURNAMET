package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated High-Fidelity 3D Ludo Dice Asset for Match Cards.
 * Features Reference 1:
 * - Floating isometric gold dice with neon cyan glowing pips.
 * - Pulsing orbital energy rings and particle aura.
 * - Bobbing animation with GPU-accelerated graphicsLayer.
 */
@Composable
fun AnimatedLudoAsset(
  modifier: Modifier = Modifier,
  size: Dp = 100.dp,
  alpha: Float = 0.92f,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "LudoAssetAnim")
  
  // Bobbing floating animation
  val translationY by infiniteTransition.animateFloat(
    initialValue = -4f,
    targetValue = 4f,
    animationSpec = infiniteRepeatable(
      animation = tween(2200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "DiceBob"
  )

  // Subtle tilt animation
  val subtleTilt by infiniteTransition.animateFloat(
    initialValue = -2f,
    targetValue = 2f,
    animationSpec = infiniteRepeatable(
      animation = tween(3000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "DiceTilt"
  )

  // Orbital ring rotation
  val ringRotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(6000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "RingRotate"
  )

  // Pip glow pulse
  val pipGlow by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "PipGlow"
  )

  Box(
    modifier = modifier
      .size(size)
      .graphicsLayer { 
        this.translationY = translationY.dp.toPx() 
        this.rotationZ = subtleTilt
      },
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = this.size.width
      val h = this.size.height
      val centerX = w / 2f
      val centerY = h / 2f

      // 1. Cyber Core Radial Glow
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            Color(0xFF06B6D4).copy(alpha = 0.4f * alpha),
            Color.Transparent
          ),
          center = Offset(centerX, centerY),
          radius = w * 0.45f
        )
      )

      // 2. Segmented Orbital Cyber Energy Rings
      rotate(ringRotation, Offset(centerX, centerY)) {
        drawArc(
          brush = Brush.sweepGradient(
            colors = listOf(Color(0xFF06B6D4), Color(0xFFF59E0B), Color(0xFF06B6D4)),
            center = Offset(centerX, centerY)
          ),
          startAngle = 0f,
          sweepAngle = 60f,
          useCenter = false,
          style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
          size = Size(w * 0.84f, h * 0.84f),
          topLeft = Offset(w * 0.08f, h * 0.08f)
        )
        drawArc(
          brush = Brush.sweepGradient(
            colors = listOf(Color(0xFF06B6D4), Color(0xFFF59E0B), Color(0xFF06B6D4)),
            center = Offset(centerX, centerY)
          ),
          startAngle = 180f,
          sweepAngle = 90f,
          useCenter = false,
          style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
          size = Size(w * 0.84f, h * 0.84f),
          topLeft = Offset(w * 0.08f, h * 0.08f)
        )
      }

      // 3. 3D Isometric Gold Dice
      val diceRadius = w * 0.28f
      
      // Face Paths
      val topPath = Path().apply {
        moveTo(centerX, centerY - diceRadius)
        lineTo(centerX + diceRadius * 0.866f, centerY - diceRadius * 0.5f)
        lineTo(centerX, centerY)
        lineTo(centerX - diceRadius * 0.866f, centerY - diceRadius * 0.5f)
        close()
      }
      val leftPath = Path().apply {
        moveTo(centerX - diceRadius * 0.866f, centerY - diceRadius * 0.5f)
        lineTo(centerX, centerY)
        lineTo(centerX, centerY + diceRadius)
        lineTo(centerX - diceRadius * 0.866f, centerY + diceRadius * 0.5f)
        close()
      }
      val rightPath = Path().apply {
        moveTo(centerX + diceRadius * 0.866f, centerY - diceRadius * 0.5f)
        lineTo(centerX, centerY)
        lineTo(centerX, centerY + diceRadius)
        lineTo(centerX + diceRadius * 0.866f, centerY + diceRadius * 0.5f)
        close()
      }

      // Draw Faces with 4-stop metallic gradients
      drawPath(
        path = topPath,
        brush = Brush.linearGradient(
          colors = listOf(Color(0xFFFFFBEB), Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFFB45309)),
          start = Offset(centerX, centerY - diceRadius),
          end = Offset(centerX, centerY)
        )
      )
      drawPath(
        path = leftPath,
        brush = Brush.linearGradient(
          colors = listOf(Color(0xFFFCD34D), Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFF92400E)),
          start = Offset(centerX - diceRadius * 0.866f, centerY),
          end = Offset(centerX, centerY + diceRadius)
        )
      )
      drawPath(
        path = rightPath,
        brush = Brush.linearGradient(
          colors = listOf(Color(0xFFD97706), Color(0xFFB45309), Color(0xFF78350F), Color(0xFF451A03)),
          start = Offset(centerX, centerY),
          end = Offset(centerX + diceRadius * 0.866f, centerY + diceRadius)
        )
      )

      // Bevel Edge Glow
      val bevelColor = Color(0xFFFEF08A).copy(alpha = 0.85f * alpha)
      drawPath(topPath, color = bevelColor, style = Stroke(width = 1.dp.toPx()))
      drawPath(leftPath, color = bevelColor, style = Stroke(width = 1.dp.toPx()))
      drawPath(rightPath, color = bevelColor, style = Stroke(width = 1.dp.toPx()))

      // 4. Radiant Cyber Pips & Specular Glints
      val pipRadius = diceRadius * 0.12f
      val pipCoreColor = Color(0xFF22D3EE).copy(alpha = pipGlow * alpha)
      val pipAuraColor = Color(0xFF06B6D4)
      
      fun drawRadiantPip(center: Offset) {
          // Outer aura
          drawCircle(
            brush = Brush.radialGradient(listOf(pipAuraColor.copy(alpha = 0.6f * pipGlow * alpha), Color.Transparent)),
            radius = pipRadius * 2.2f,
            center = center
          )
          // Inner core
          drawCircle(color = pipCoreColor, radius = pipRadius, center = center)
          // Specular glint
          drawCircle(
            color = Color.White.copy(alpha = 0.95f * alpha),
            radius = pipRadius * 0.25f,
            center = Offset(center.x - pipRadius * 0.3f, center.y - pipRadius * 0.3f)
          )
      }

      // Top face (Pip 1)
      drawRadiantPip(Offset(centerX, centerY - diceRadius * 0.5f))

      // Left face (Pips 2)
      drawRadiantPip(Offset(centerX - diceRadius * 0.45f, centerY + diceRadius * 0.1f))
      drawRadiantPip(Offset(centerX - diceRadius * 0.25f, centerY + diceRadius * 0.5f))
    }
  }
}

/**
 * Animated High-Fidelity 3D Carrom Asset for Match Cards.
 * Features Reference 2:
 * - Rotating obsidian-gold striker with glowing cyber engravings.
 * - Pulsing ruby queen coin with radiant amber flare.
 * - Cyber shimmer motion trails.
 */
@Composable
fun AnimatedCarromAsset(
  modifier: Modifier = Modifier,
  size: Dp = 100.dp,
  alpha: Float = 0.92f,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "CarromAssetAnim")
  
  // Ambient Rotation
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(12000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "StrikerRotate"
  )

  // Ruby Queen Pulse
  val rubyPulse by infiniteTransition.animateFloat(
    initialValue = 0.7f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "RubyPulse"
  )

  // Shimmer Offset
  val shimmerOffset by infiniteTransition.animateFloat(
    initialValue = -100f,
    targetValue = 100f,
    animationSpec = infiniteRepeatable(
      animation = tween(3000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "CyberShimmer"
  )

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = this.size.width
      val h = this.size.height
      val centerX = w / 2f
      val centerY = h / 2f

      // 1. Ambient Cyber Blue Aura
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(NeonCyan.copy(alpha = 0.3f * alpha), Color.Transparent),
          center = Offset(centerX, centerY),
          radius = w * 0.5f
        )
      )

      // 2. Obsidian Striker (Reference 2)
      rotate(rotation, Offset(centerX, centerY)) {
        val strikerRadius = w * 0.38f
        
        // Base Disc
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(Slate950.copy(alpha = alpha), Color.Black.copy(alpha = alpha)),
            center = Offset(centerX, centerY),
            radius = strikerRadius
          ),
          radius = strikerRadius
        )

        // Golden Outer Ring Bevel
        drawCircle(
          brush = Brush.sweepGradient(
            colors = listOf(Gold400.copy(alpha = alpha), Gold500.copy(alpha = alpha), Gold400.copy(alpha = alpha)),
            center = Offset(centerX, centerY)
          ),
          radius = strikerRadius,
          style = Stroke(width = 3.dp.toPx())
        )

        // Cyber Engravings (Reference 2 Patterns)
        for (i in 0..7) {
          val angle = i * 45f * (PI.toFloat() / 180f)
          val rStart = strikerRadius * 0.4f
          val rEnd = strikerRadius * 0.85f
          
          drawLine(
            color = NeonCyan.copy(alpha = 0.8f * alpha),
            start = Offset(centerX + cos(angle) * rStart, centerY + sin(angle) * rStart),
            end = Offset(centerX + cos(angle) * rEnd, centerY + sin(angle) * rEnd),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
          )
        }

        // Animated Shimmer Sweep
        drawCircle(
          brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.3f * alpha), Color.Transparent),
            start = Offset(shimmerOffset, shimmerOffset),
            end = Offset(shimmerOffset + 50f, shimmerOffset + 50f)
          ),
          radius = strikerRadius
        )
      }

      // 3. Radiant Ruby Queen (Central - Reference 2)
      val queenRadius = w * 0.15f
      val rubyColor = Color(0xFFE11D48) // Rich Ruby
      
      // Ruby Body
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(rubyColor.copy(alpha = alpha), Color(0xFF4C0519).copy(alpha = alpha)),
          center = Offset(centerX, centerY),
          radius = queenRadius
        ),
        radius = queenRadius,
        center = Offset(centerX, centerY)
      )

      // Amber Flare Flare (Reference 2)
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Amber400.copy(alpha = 0.5f * rubyPulse * alpha), Color.Transparent),
          center = Offset(centerX, centerY),
          radius = queenRadius * 2.2f
        ),
        radius = queenRadius * 2.2f,
        center = Offset(centerX, centerY)
      )

      // Specular highlight
      drawCircle(
        color = Color.White.copy(alpha = 0.4f * alpha),
        radius = queenRadius * 0.3f,
        center = Offset(centerX - queenRadius * 0.3f, centerY - queenRadius * 0.3f)
      )
    }
  }
}

/**
 * Lightweight, hardware-accelerated subtle 3D Ludo watermark for Match Cards.
 * Features:
 * - Isometric 3D golden dice with white pips & glowing bevels
 * - Red and Blue 3D Ludo pawn tokens
 * - Ambient warm gold/amber radial glow
 * Zero bitmap allocation, zero main-thread decode overhead.
 */
@Composable
fun Ludo3DWatermark(
  modifier: Modifier = Modifier,
  size: Dp = 120.dp,
  alpha: Float = 0.16f,
) {
  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = this.size.width
      val h = this.size.height

      // 1. Ambient warm golden/amber glow
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            Gold400.copy(alpha = alpha * 1.5f),
            Amber600.copy(alpha = alpha * 0.6f),
            Color.Transparent,
          ),
          center = Offset(w * 0.65f, h * 0.45f),
          radius = w * 0.55f,
        )
      )

      // 2. Isometric 3D Golden Dice (Top-Right)
      val diceCenterX = w * 0.65f
      val diceCenterY = h * 0.42f
      val diceRadius = w * 0.26f

      // Top face
      val topPath = Path().apply {
        moveTo(diceCenterX, diceCenterY - diceRadius)
        lineTo(diceCenterX + diceRadius * 0.866f, diceCenterY - diceRadius * 0.5f)
        lineTo(diceCenterX, diceCenterY)
        lineTo(diceCenterX - diceRadius * 0.866f, diceCenterY - diceRadius * 0.5f)
        close()
      }
      drawPath(
        path = topPath,
        brush = Brush.linearGradient(
          colors = listOf(Gold400.copy(alpha = alpha * 1.8f), Amber500.copy(alpha = alpha * 1.2f)),
          start = Offset(diceCenterX, diceCenterY - diceRadius),
          end = Offset(diceCenterX, diceCenterY),
        ),
      )

      // Left face
      val leftPath = Path().apply {
        moveTo(diceCenterX - diceRadius * 0.866f, diceCenterY - diceRadius * 0.5f)
        lineTo(diceCenterX, diceCenterY)
        lineTo(diceCenterX, diceCenterY + diceRadius)
        lineTo(diceCenterX - diceRadius * 0.866f, diceCenterY + diceRadius * 0.5f)
        close()
      }
      drawPath(
        path = leftPath,
        brush = Brush.linearGradient(
          colors = listOf(Amber500.copy(alpha = alpha * 1.3f), Amber600.copy(alpha = alpha * 0.8f)),
          start = Offset(diceCenterX - diceRadius * 0.866f, diceCenterY),
          end = Offset(diceCenterX, diceCenterY + diceRadius),
        ),
      )

      // Right face
      val rightPath = Path().apply {
        moveTo(diceCenterX + diceRadius * 0.866f, diceCenterY - diceRadius * 0.5f)
        lineTo(diceCenterX, diceCenterY)
        lineTo(diceCenterX, diceCenterY + diceRadius)
        lineTo(diceCenterX + diceRadius * 0.866f, diceCenterY + diceRadius * 0.5f)
        close()
      }
      drawPath(
        path = rightPath,
        brush = Brush.linearGradient(
          colors = listOf(Amber600.copy(alpha = alpha * 1.0f), Color(0xFF78350F).copy(alpha = alpha * 0.7f)),
          start = Offset(diceCenterX, diceCenterY),
          end = Offset(diceCenterX + diceRadius * 0.866f, diceCenterY + diceRadius),
        ),
      )

      // Dice Wireframe edges
      val edgeBrush = Brush.linearGradient(
        colors = listOf(Gold400.copy(alpha = alpha * 2.2f), Amber500.copy(alpha = alpha * 1.4f))
      )
      drawPath(path = topPath, brush = edgeBrush, style = Stroke(width = 1.5f))
      drawPath(path = leftPath, brush = edgeBrush, style = Stroke(width = 1.5f))
      drawPath(path = rightPath, brush = edgeBrush, style = Stroke(width = 1.5f))

      // Dice Pip Dots on top face (Center dot for '1')
      drawCircle(
        color = Color.White.copy(alpha = alpha * 2.2f),
        radius = diceRadius * 0.12f,
        center = Offset(diceCenterX, diceCenterY - diceRadius * 0.5f),
      )

      // Pip Dots on left face
      drawCircle(
        color = Color.White.copy(alpha = alpha * 1.8f),
        radius = diceRadius * 0.09f,
        center = Offset(diceCenterX - diceRadius * 0.45f, diceCenterY + diceRadius * 0.1f),
      )
      drawCircle(
        color = Color.White.copy(alpha = alpha * 1.8f),
        radius = diceRadius * 0.09f,
        center = Offset(diceCenterX - diceRadius * 0.25f, diceCenterY + diceRadius * 0.5f),
      )

      // 3. 3D Ludo RED Pawn Token (Left)
      drawPawn(
        w = w,
        h = h,
        pawnCenterX = w * 0.22f,
        pawnBaseY = h * 0.84f,
        pawnHeight = h * 0.46f,
        baseColor = Rose600,
        highlightColor = Color(0xFFFF8080),
        darkColor = Color(0xFF4C0519),
        alpha = alpha,
      )

      // 4. 3D Ludo BLUE Pawn Token (Middle-Right)
      drawPawn(
        w = w,
        h = h,
        pawnCenterX = w * 0.46f,
        pawnBaseY = h * 0.88f,
        pawnHeight = h * 0.42f,
        baseColor = Color(0xFF2563EB),
        highlightColor = Color(0xFF93C5FD),
        darkColor = Color(0xFF1E3A8A),
        alpha = alpha,
      )
    }
  }
}

private fun DrawScope.drawPawn(
  w: Float,
  h: Float,
  pawnCenterX: Float,
  pawnBaseY: Float,
  pawnHeight: Float,
  baseColor: Color,
  highlightColor: Color,
  darkColor: Color,
  alpha: Float,
) {
  // Base Ellipse
  drawOval(
    brush = Brush.radialGradient(
      colors = listOf(baseColor.copy(alpha = alpha * 1.6f), darkColor.copy(alpha = alpha * 0.9f)),
    ),
    topLeft = Offset(pawnCenterX - w * 0.14f, pawnBaseY - h * 0.05f),
    size = Size(w * 0.28f, h * 0.10f),
  )

  // Body Cone
  val pawnBodyPath = Path().apply {
    moveTo(pawnCenterX - w * 0.12f, pawnBaseY)
    quadraticTo(
      pawnCenterX - w * 0.04f, pawnBaseY - pawnHeight * 0.55f,
      pawnCenterX - w * 0.06f, pawnBaseY - pawnHeight * 0.65f
    )
    lineTo(pawnCenterX + w * 0.06f, pawnBaseY - pawnHeight * 0.65f)
    quadraticTo(
      pawnCenterX + w * 0.04f, pawnBaseY - pawnHeight * 0.55f,
      pawnCenterX + w * 0.12f, pawnBaseY
    )
    close()
  }
  drawPath(
    path = pawnBodyPath,
    brush = Brush.linearGradient(
      colors = listOf(baseColor.copy(alpha = alpha * 1.7f), darkColor.copy(alpha = alpha * 0.8f)),
      start = Offset(pawnCenterX - w * 0.12f, pawnBaseY - pawnHeight),
      end = Offset(pawnCenterX + w * 0.12f, pawnBaseY),
    ),
  )

  // Head Sphere with highlight
  val headCenter = Offset(pawnCenterX, pawnBaseY - pawnHeight * 0.80f)
  val headRadius = w * 0.09f
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(
        highlightColor.copy(alpha = alpha * 2.0f),
        baseColor.copy(alpha = alpha * 1.6f),
        darkColor.copy(alpha = alpha * 0.9f),
      ),
      center = Offset(headCenter.x - headRadius * 0.3f, headCenter.y - headRadius * 0.3f),
      radius = headRadius,
    ),
    radius = headRadius,
    center = headCenter,
  )
}

/**
 * Lightweight, hardware-accelerated subtle 3D Carrom watermark for Match Cards.
 * Features:
 * - 3D polished wooden carrom striker disc
 * - 3D Ruby Red Queen Coin
 * - 3D Black & White Carrom coins
 * - Ambient Neon Cyan glow and pocket curve
 * Zero bitmap allocation, zero main-thread decode overhead.
 */
@Composable
fun Carrom3DWatermark(
  modifier: Modifier = Modifier,
  size: Dp = 120.dp,
  alpha: Float = 0.16f,
) {
  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = this.size.width
      val h = this.size.height

      // 1. Ambient Neon Cyan Glow
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            NeonCyan.copy(alpha = alpha * 1.4f),
            Color(0xFF0891B2).copy(alpha = alpha * 0.5f),
            Color.Transparent,
          ),
          center = Offset(w * 0.60f, h * 0.50f),
          radius = w * 0.55f,
        )
      )

      // 2. 3D Carrom Board Corner Pocket Arc
      val boardArcPath = Path().apply {
        moveTo(w * 0.30f, 0f)
        cubicTo(w * 0.40f, h * 0.30f, w * 0.70f, h * 0.40f, w, h * 0.45f)
      }
      drawPath(
        path = boardArcPath,
        brush = Brush.linearGradient(
          colors = listOf(NeonCyan.copy(alpha = alpha * 1.5f), Color(0xFF0E7490).copy(alpha = alpha * 0.6f))
        ),
        style = Stroke(width = 2f),
      )

      // 3. 3D Carrom Striker Ring (Beveled Isometric Disc)
      val strikerCenter = Offset(w * 0.64f, h * 0.48f)
      val strikerRx = w * 0.24f
      val strikerRy = h * 0.15f

      // Striker Base Thickness / Shadow
      drawOval(
        brush = Brush.linearGradient(
          colors = listOf(Color(0xFF0891B2).copy(alpha = alpha * 1.2f), Color(0xFF164E63).copy(alpha = alpha * 0.8f))
        ),
        topLeft = Offset(strikerCenter.x - strikerRx, strikerCenter.y - strikerRy + 4f),
        size = Size(strikerRx * 2, strikerRy * 2),
      )

      // Striker Top Bevel Face
      drawOval(
        brush = Brush.radialGradient(
          colors = listOf(
            Color.White.copy(alpha = alpha * 2.0f),
            NeonCyan.copy(alpha = alpha * 1.7f),
            Color(0xFF0E7490).copy(alpha = alpha * 1.1f),
          ),
          center = Offset(strikerCenter.x - strikerRx * 0.2f, strikerCenter.y - strikerRy * 0.2f),
          radius = strikerRx,
        ),
        topLeft = Offset(strikerCenter.x - strikerRx, strikerCenter.y - strikerRy),
        size = Size(strikerRx * 2, strikerRy * 2),
      )

      // Striker Inner Groove Ring
      drawOval(
        color = NeonCyan.copy(alpha = alpha * 2.2f),
        topLeft = Offset(strikerCenter.x - strikerRx * 0.65f, strikerCenter.y - strikerRy * 0.65f),
        size = Size(strikerRx * 1.3f, strikerRy * 1.3f),
        style = Stroke(width = 1.5f),
      )

      // Striker Center Point
      drawCircle(
        color = Color.White.copy(alpha = alpha * 2.4f),
        radius = strikerRx * 0.12f,
        center = strikerCenter,
      )

      // 4. 3D Carrom Red Queen Coin (Front-Left)
      val queenCenter = Offset(w * 0.26f, h * 0.70f)
      val queenRx = w * 0.14f
      val queenRy = h * 0.085f

      // Queen Coin Thickness
      drawOval(
        color = Color(0xFF881337).copy(alpha = alpha * 1.3f),
        topLeft = Offset(queenCenter.x - queenRx, queenCenter.y - queenRy + 3f),
        size = Size(queenRx * 2, queenRy * 2),
      )

      // Queen Coin Face (Rich Ruby Red)
      drawOval(
        brush = Brush.radialGradient(
          colors = listOf(
            Color(0xFFFF6B81).copy(alpha = alpha * 2.2f),
            Rose600.copy(alpha = alpha * 1.8f),
            Color(0xFF9F1239).copy(alpha = alpha * 1.2f),
          ),
          center = Offset(queenCenter.x - queenRx * 0.2f, queenCenter.y - queenRy * 0.2f),
          radius = queenRx,
        ),
        topLeft = Offset(queenCenter.x - queenRx, queenCenter.y - queenRy),
        size = Size(queenRx * 2, queenRy * 2),
      )

      // Queen Star / Emblem Dot
      drawCircle(
        color = Gold400.copy(alpha = alpha * 2.4f),
        radius = queenRx * 0.20f,
        center = queenCenter,
      )

      // 5. 3D Carrom White Coin (Middle-Bottom)
      val whiteCenter = Offset(w * 0.50f, h * 0.82f)
      val coinRx = w * 0.12f
      val coinRy = h * 0.075f

      drawOval(
        brush = Brush.radialGradient(
          colors = listOf(
            Color.White.copy(alpha = alpha * 2.0f),
            Color(0xFFCBD5E1).copy(alpha = alpha * 1.5f),
            Color(0xFF64748B).copy(alpha = alpha * 0.9f),
          ),
          center = Offset(whiteCenter.x - coinRx * 0.2f, whiteCenter.y - coinRy * 0.2f),
          radius = coinRx,
        ),
        topLeft = Offset(whiteCenter.x - coinRx, whiteCenter.y - coinRy),
        size = Size(coinRx * 2, coinRy * 2),
      )
    }
  }
}
