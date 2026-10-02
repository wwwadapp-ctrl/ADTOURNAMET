package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.home.GuideType
import com.example.ui.theme.*

/**
 * High-performance, hardware-accelerated 3D tactile vector artwork for the 4 Home Guide Cards.
 * 1. DEPOSIT: 3D Leather Wallet with Golden Stash Coins & Holographic Currency Glow
 * 2. MATCH_JOIN: 3D Esports Gamepad Controller with Holographic D-Pad & Room-Code Glow
 * 3. RESULT_SUBMIT: 3D Championship Trophy Cup with Victory Starburst & Golden Gleam
 * 4. RULES: 3D High-Security Crest Shield with Gavel of Justice & Fair-Play Laurel
 *
 * 100% Canvas rendered: zero bitmap allocations, zero network latency, zero main-thread decode lags.
 */
@Composable
fun GuideCard3DArt(
  guideType: GuideType,
  accentColor: Color,
  secondaryColor: Color,
  modifier: Modifier = Modifier,
  size: Dp = 56.dp,
) {
  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = this.size.width
      val h = this.size.height

      // Ambient radial backlight
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            accentColor.copy(alpha = 0.35f),
            secondaryColor.copy(alpha = 0.12f),
            Color.Transparent,
          ),
          center = Offset(w * 0.5f, h * 0.5f),
          radius = w * 0.55f,
        )
      )

      when (guideType) {
        GuideType.DEPOSIT -> drawDepositWalletArt(w, h, accentColor, secondaryColor)
        GuideType.MATCH_JOIN -> drawMatchJoinGamepadArt(w, h, accentColor, secondaryColor)
        GuideType.RESULT_SUBMIT -> drawResultTrophyArt(w, h, accentColor, secondaryColor)
        GuideType.RULES -> drawFairPlayShieldArt(w, h, accentColor, secondaryColor)
      }
    }
  }
}

/**
 * 1. DEPOSIT GUIDE ART: 3D Bi-fold Wallet with Stacked Golden Coins
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDepositWalletArt(
  w: Float,
  h: Float,
  accentColor: Color,
  secondaryColor: Color,
) {
  // 1. Stack of 2 Golden Coins in Background
  val coin1Center = Offset(w * 0.65f, h * 0.32f)
  val coinRadius = w * 0.16f

  // Gold Coin 1 (Back)
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Gold400, Gold500, Amber700),
      center = Offset(coin1Center.x - coinRadius * 0.3f, coin1Center.y - coinRadius * 0.3f),
      radius = coinRadius,
    ),
    radius = coinRadius,
    center = coin1Center,
  )
  drawCircle(
    color = Color.White.copy(alpha = 0.6f),
    radius = coinRadius * 0.75f,
    center = coin1Center,
    style = Stroke(width = 1.5f),
  )

  // Gold Coin 2 (Middle)
  val coin2Center = Offset(w * 0.42f, h * 0.28f)
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Gold400, Gold500, Amber700),
      center = Offset(coin2Center.x - coinRadius * 0.3f, coin2Center.y - coinRadius * 0.3f),
      radius = coinRadius,
    ),
    radius = coinRadius,
    center = coin2Center,
  )
  drawCircle(
    color = Color.White.copy(alpha = 0.7f),
    radius = coinRadius * 0.75f,
    center = coin2Center,
    style = Stroke(width = 1.5f),
  )

  // 2. 3D Isometric Wallet Body (Rich Dark Leather with Cyan/Gold Trim)
  val walletPath = Path().apply {
    moveTo(w * 0.16f, h * 0.44f)
    lineTo(w * 0.82f, h * 0.38f)
    quadraticTo(w * 0.88f, h * 0.40f, w * 0.86f, h * 0.78f)
    lineTo(w * 0.20f, h * 0.86f)
    quadraticTo(w * 0.14f, h * 0.84f, w * 0.16f, h * 0.44f)
    close()
  }

  // Wallet Base Shadow/Backdrop
  drawPath(
    path = walletPath,
    brush = Brush.linearGradient(
      colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
      start = Offset(w * 0.2f, h * 0.4f),
      end = Offset(w * 0.8f, h * 0.85f),
    ),
  )

  // Wallet Stitching & Border
  drawPath(
    path = walletPath,
    brush = Brush.linearGradient(
      colors = listOf(Gold400, NeonCyan.copy(alpha = 0.8f)),
    ),
    style = Stroke(width = 1.8f),
  )

  // 3. Wallet Flap & Gold Clasp
  val flapPath = Path().apply {
    moveTo(w * 0.52f, h * 0.52f)
    lineTo(w * 0.86f, h * 0.50f)
    lineTo(w * 0.86f, h * 0.68f)
    lineTo(w * 0.52f, h * 0.70f)
    close()
  }
  drawPath(
    path = flapPath,
    brush = Brush.linearGradient(
      colors = listOf(Color(0xFF334155), Color(0xFF1E293B)),
    ),
  )
  drawPath(
    path = flapPath,
    color = Gold400.copy(alpha = 0.7f),
    style = Stroke(width = 1.2f),
  )

  // Gold Clasp Emblem Dot (circle)
  val claspCenter = Offset(w * 0.58f, h * 0.61f)
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color.White, Gold400, Amber600),
      center = claspCenter,
      radius = w * 0.07f,
    ),
    radius = w * 0.07f,
    center = claspCenter,
  )
}

/**
 * 2. MATCH JOIN GUIDE ART: 3D Esports Gamepad Controller with Holographic D-Pad
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMatchJoinGamepadArt(
  w: Float,
  h: Float,
  accentColor: Color,
  secondaryColor: Color,
) {
  // 1. Controller Body
  val bodyPath = Path().apply {
    moveTo(w * 0.28f, h * 0.32f)
    cubicTo(w * 0.40f, h * 0.36f, w * 0.60f, h * 0.36f, w * 0.72f, h * 0.32f)
    cubicTo(w * 0.90f, h * 0.38f, w * 0.92f, h * 0.75f, w * 0.80f, h * 0.82f)
    cubicTo(w * 0.70f, h * 0.88f, w * 0.64f, h * 0.68f, w * 0.50f, h * 0.66f)
    cubicTo(w * 0.36f, h * 0.68f, w * 0.30f, h * 0.88f, w * 0.20f, h * 0.82f)
    cubicTo(w * 0.08f, h * 0.75f, w * 0.10f, h * 0.38f, w * 0.28f, h * 0.32f)
    close()
  }

  // Controller Base
  drawPath(
    path = bodyPath,
    brush = Brush.linearGradient(
      colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
      start = Offset(w * 0.5f, h * 0.3f),
      end = Offset(w * 0.5f, h * 0.85f),
    ),
  )

  // Controller Neon Cyan Glowing Outline
  drawPath(
    path = bodyPath,
    brush = Brush.linearGradient(
      colors = listOf(NeonCyan, Indigo400, NeonCyan),
    ),
    style = Stroke(width = 1.8f),
  )

  // 2. D-Pad on Left Wing (Cyan Cross)
  val dpadCenter = Offset(w * 0.28f, h * 0.52f)
  val armLen = w * 0.08f
  val armThick = w * 0.05f

  drawRect(
    color = NeonCyan,
    topLeft = Offset(dpadCenter.x - armLen, dpadCenter.y - armThick / 2),
    size = Size(armLen * 2, armThick),
  )
  drawRect(
    color = NeonCyan,
    topLeft = Offset(dpadCenter.x - armThick / 2, dpadCenter.y - armLen),
    size = Size(armThick, armLen * 2),
  )

  // 3. Action Buttons on Right Wing (Diamond of 4 glowing dots)
  val btnCenter = Offset(w * 0.72f, h * 0.52f)
  val btnOffset = w * 0.06f
  val btnRadius = w * 0.032f

  drawCircle(color = Gold400, radius = btnRadius, center = Offset(btnCenter.x, btnCenter.y - btnOffset))
  drawCircle(color = Rose500, radius = btnRadius, center = Offset(btnCenter.x + btnOffset, btnCenter.y))
  drawCircle(color = NeonCyan, radius = btnRadius, center = Offset(btnCenter.x, btnCenter.y + btnOffset))
  drawCircle(color = NeonEmerald, radius = btnRadius, center = Offset(btnCenter.x - btnOffset, btnCenter.y))

  // 4. Center Holographic Status Dot
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color.White, NeonCyan),
      center = Offset(w * 0.5f, h * 0.48f),
      radius = w * 0.06f,
    ),
    radius = w * 0.05f,
    center = Offset(w * 0.5f, h * 0.48f),
  )
}

/**
 * 3. RESULT SUBMIT GUIDE ART: 3D Championship Trophy Cup with Victory Starburst
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawResultTrophyArt(
  w: Float,
  h: Float,
  accentColor: Color,
  secondaryColor: Color,
) {
  // 1. Trophy Cup Body
  val cupPath = Path().apply {
    moveTo(w * 0.26f, h * 0.22f)
    lineTo(w * 0.74f, h * 0.22f)
    lineTo(w * 0.68f, h * 0.52f)
    cubicTo(w * 0.64f, h * 0.68f, w * 0.36f, h * 0.68f, w * 0.32f, h * 0.52f)
    close()
  }

  // Gold Cup Shading
  drawPath(
    path = cupPath,
    brush = Brush.linearGradient(
      colors = listOf(Gold400, Gold500, Amber700),
      start = Offset(w * 0.26f, h * 0.22f),
      end = Offset(w * 0.74f, h * 0.65f),
    ),
  )

  // Cup Rim & Outline
  drawPath(
    path = cupPath,
    color = Color.White.copy(alpha = 0.8f),
    style = Stroke(width = 1.6f),
  )

  // 2. Left and Right Handles
  val leftHandle = Path().apply {
    moveTo(w * 0.26f, h * 0.26f)
    cubicTo(w * 0.10f, h * 0.28f, w * 0.10f, h * 0.50f, w * 0.32f, h * 0.52f)
  }
  val rightHandle = Path().apply {
    moveTo(w * 0.74f, h * 0.26f)
    cubicTo(w * 0.90f, h * 0.28f, w * 0.90f, h * 0.50f, w * 0.68f, h * 0.52f)
  }
  drawPath(path = leftHandle, brush = Brush.linearGradient(listOf(Gold400, Amber600)), style = Stroke(width = 2.4f))
  drawPath(path = rightHandle, brush = Brush.linearGradient(listOf(Gold400, Amber600)), style = Stroke(width = 2.4f))

  // 3. Trophy Stem & Pedestal Base
  val stemPath = Path().apply {
    moveTo(w * 0.44f, h * 0.65f)
    lineTo(w * 0.56f, h * 0.65f)
    lineTo(w * 0.54f, h * 0.76f)
    lineTo(w * 0.46f, h * 0.76f)
    close()
  }
  drawPath(path = stemPath, color = Amber700)

  // Base
  val basePath = Path().apply {
    moveTo(w * 0.30f, h * 0.76f)
    lineTo(w * 0.70f, h * 0.76f)
    lineTo(w * 0.76f, h * 0.88f)
    lineTo(w * 0.24f, h * 0.88f)
    close()
  }
  drawPath(
    path = basePath,
    brush = Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF0F172A))),
  )
  drawPath(path = basePath, color = Gold400, style = Stroke(width = 1.4f))

  // 4. Center Victory Star
  val starCenter = Offset(w * 0.5f, h * 0.40f)
  val starR = w * 0.08f
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(Color.White, Gold400),
      center = starCenter,
      radius = starR,
    ),
    radius = starR,
    center = starCenter,
  )
}

/**
 * 4. FAIR PLAY RULES ART: 3D High-Security Crest Shield with Gavel of Justice
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFairPlayShieldArt(
  w: Float,
  h: Float,
  accentColor: Color,
  secondaryColor: Color,
) {
  // 1. 3D Knight Crest Shield
  val shieldPath = Path().apply {
    moveTo(w * 0.22f, h * 0.20f)
    lineTo(w * 0.78f, h * 0.20f)
    lineTo(w * 0.78f, h * 0.52f)
    cubicTo(w * 0.78f, h * 0.74f, w * 0.50f, h * 0.88f, w * 0.50f, h * 0.88f)
    cubicTo(w * 0.50f, h * 0.88f, w * 0.22f, h * 0.74f, w * 0.22f, h * 0.52f)
    close()
  }

  // Shield Base
  drawPath(
    path = shieldPath,
    brush = Brush.linearGradient(
      colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
      start = Offset(w * 0.5f, h * 0.2f),
      end = Offset(w * 0.5f, h * 0.88f),
    ),
  )

  // Shield Golden / Ruby Trim
  drawPath(
    path = shieldPath,
    brush = Brush.linearGradient(
      colors = listOf(Gold400, Rose500, Gold400),
    ),
    style = Stroke(width = 1.8f),
  )

  // Inner Shield Inset
  val innerShieldPath = Path().apply {
    moveTo(w * 0.30f, h * 0.28f)
    lineTo(w * 0.70f, h * 0.28f)
    lineTo(w * 0.70f, h * 0.50f)
    cubicTo(w * 0.70f, h * 0.68f, w * 0.50f, h * 0.78f, w * 0.50f, h * 0.78f)
    cubicTo(w * 0.50f, h * 0.78f, w * 0.30f, h * 0.68f, w * 0.30f, h * 0.50f)
    close()
  }
  drawPath(
    path = innerShieldPath,
    brush = Brush.linearGradient(
      colors = listOf(Gold400.copy(alpha = 0.15f), Rose600.copy(alpha = 0.15f)),
    ),
  )

  // 2. Gavel / Scales of Justice Emblem
  val gavelCenterX = w * 0.50f
  val gavelCenterY = h * 0.44f

  val gavelHead = Path().apply {
    moveTo(gavelCenterX - w * 0.14f, gavelCenterY - h * 0.08f)
    lineTo(gavelCenterX + w * 0.08f, gavelCenterY + h * 0.10f)
    lineTo(gavelCenterX + w * 0.04f, gavelCenterY + h * 0.14f)
    lineTo(gavelCenterX - w * 0.18f, gavelCenterY - h * 0.04f)
    close()
  }
  drawPath(
    path = gavelHead,
    brush = Brush.linearGradient(listOf(Gold400, Amber600)),
  )

  // Gavel Handle
  val handlePath = Path().apply {
    moveTo(gavelCenterX - w * 0.05f, gavelCenterY + h * 0.01f)
    lineTo(gavelCenterX + w * 0.16f, gavelCenterY + h * 0.20f)
  }
  drawPath(
    path = handlePath,
    color = Color.White.copy(alpha = 0.9f),
    style = Stroke(width = 2.4f),
  )

  // Star Emblem at bottom of Shield
  drawCircle(
    color = Gold400,
    radius = w * 0.035f,
    center = Offset(w * 0.5f, h * 0.72f),
  )
}
