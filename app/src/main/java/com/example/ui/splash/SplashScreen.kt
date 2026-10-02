package com.example.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
  onSplashFinished: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // Splash progress animation (0f -> 1f over 2.2 seconds)
  val progress = remember { Animatable(0f) }
  var canSkip by remember { mutableStateOf(false) }

  // Ambient floating animation for dice & sparks
  val infiniteTransition = rememberInfiniteTransition(label = "splash_ambient")
  val floatOffset by infiniteTransition.animateFloat(
    initialValue = -4f,
    targetValue = 4f,
    animationSpec = infiniteRepeatable(
      animation = tween(1600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "float_offset",
  )
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "pulse_alpha",
  )

  LaunchedEffect(Unit) {
    delay(400)
    canSkip = true
    progress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
    )
    delay(150)
    onSplashFinished()
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF070B18))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = canSkip,
      ) {
        onSplashFinished()
      }
      .testTag("splash_screen_root"),
  ) {
    val screenHeight = maxHeight
    val screenWidth = maxWidth
    val isCompactHeight = screenHeight < 640.dp
    val scaleFactor = if (isCompactHeight) 0.82f else 1f

    // 1. ATMOSPHERIC ESPORTS BACKGROUND CANVAS
    SplashBackgroundCanvas(pulseAlpha = pulseAlpha)

    // 2. MAIN SPLASH CONTENT
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(
          horizontal = 24.dp,
          vertical = if (isCompactHeight) 16.dp else 28.dp,
        ),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // TOP / CENTER SECTION: Royal Crown + AD Tournament Shield Logo
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxWidth()
          .scale(scaleFactor),
      ) {
        Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 16.dp))

        // A. Royal Championship Crown with Jewels
        RoyalCrown(modifier = Modifier.size(width = 110.dp, height = 48.dp))

        Spacer(modifier = Modifier.height(4.dp))

        // B. AD TOURNAMENT Crest Shield (housing official launcher logo)
        TournamentLogoBadge(modifier = Modifier.size(116.dp))

        Spacer(modifier = Modifier.height(10.dp))

        // C. 3D Metallic "AD TOURNAMENT" Title
        AdTournamentTitle()

        Spacer(modifier = Modifier.height(6.dp))

        // D. Ribbon Subtitle Tag
        TournamentRibbonTag(text = "LUDO & ESPORTS ARENA")
      }

      // MIDDLE SECTION: 3D Isometric Ludo Board & Glowing Floating Dice
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(if (isCompactHeight) 160.dp else 220.dp)
          .scale(scaleFactor),
        contentAlignment = Alignment.Center,
      ) {
        LudoBoardAndDiceCanvas(
          floatOffset = floatOffset,
          pulseAlpha = pulseAlpha,
          modifier = Modifier.fillMaxSize(),
        )
      }

      // BOTTOM SECTION: Tagline + Loading Bar + Crown Footer
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = if (isCompactHeight) 12.dp else 20.dp)
          .scale(scaleFactor),
      ) {
        // Tagline: PLAY 👑 WIN 👑 BE KING
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.padding(bottom = 14.dp),
        ) {
          Text(
            text = "PLAY",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 2.sp,
            ),
            color = Gold400,
          )
          Text(
            text = "  👑  ",
            fontSize = 13.sp,
          )
          Text(
            text = "WIN",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 2.sp,
            ),
            color = Gold400,
          )
          Text(
            text = "  👑  ",
            fontSize = 13.sp,
          )
          Text(
            text = "BE KING",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 2.sp,
            ),
            color = Gold400,
          )
        }

        // Animated Golden Loading Progress Bar
        Box(
          modifier = Modifier
            .widthIn(max = 240.dp)
            .fillMaxWidth(0.68f)
            .height(7.dp)
            .clip(CircleShape)
            .background(Color(0xFF0F172A))
            .testTag("splash_loading_bar"),
        ) {
          // Glow border
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color(0xFF1E293B)),
          )
          // Progress fill with gold/amber gradient
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .fillMaxWidth(progress.value)
              .clip(CircleShape)
              .background(
                Brush.horizontalGradient(
                  colors = listOf(
                    Gold400,
                    Amber500,
                    Color(0xFFFDE047),
                    Gold400,
                  )
                )
              ),
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // "Loading..." status text
        Text(
          text = "Loading...",
          style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Medium,
          ),
          color = Slate400.copy(alpha = pulseAlpha),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Mini Golden Crown Footer Emblem
        MiniCrownIcon(
          tint = Gold500.copy(alpha = 0.85f),
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

/**
 * Atmospheric background with subtle diagonal rays, ambient glows, and faint dice silhouettes.
 */
@Composable
private fun SplashBackgroundCanvas(
  pulseAlpha: Float,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.fillMaxSize()) {
    val width = size.width
    val height = size.height

    // Radial dark navy background
    drawRect(
      brush = Brush.radialGradient(
        colors = listOf(
          Color(0xFF0F1A30),
          Color(0xFF0A0F1D),
          Color(0xFF060913),
        ),
        center = Offset(width * 0.5f, height * 0.45f),
        radius = width * 0.9f,
      )
    )

    // Top Cyan Ambient Light Cone
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          Cyan500.copy(alpha = 0.14f * pulseAlpha),
          Color.Transparent,
        ),
        center = Offset(width * 0.5f, height * 0.15f),
        radius = width * 0.6f,
      ),
      radius = width * 0.6f,
      center = Offset(width * 0.5f, height * 0.15f),
    )

    // Center Gold Ambient Aura (under dice)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          Gold500.copy(alpha = 0.16f * pulseAlpha),
          Color.Transparent,
        ),
        center = Offset(width * 0.5f, height * 0.55f),
        radius = width * 0.5f,
      ),
      radius = width * 0.5f,
      center = Offset(width * 0.5f, height * 0.55f),
    )

    // Diagonal Gaming Light Beams (top-left to bottom-right)
    drawLine(
      brush = Brush.linearGradient(
        colors = listOf(Color.Transparent, Cyan400.copy(alpha = 0.08f), Color.Transparent),
        start = Offset(0f, height * 0.1f),
        end = Offset(width, height * 0.5f),
      ),
      start = Offset(0f, height * 0.1f),
      end = Offset(width, height * 0.5f),
      strokeWidth = 24f,
    )
    drawLine(
      brush = Brush.linearGradient(
        colors = listOf(Color.Transparent, Gold400.copy(alpha = 0.06f), Color.Transparent),
        start = Offset(0f, height * 0.35f),
        end = Offset(width, height * 0.75f),
      ),
      start = Offset(0f, height * 0.35f),
      end = Offset(width, height * 0.75f),
      strokeWidth = 18f,
    )

    // Faint floating ambient particle sparkles
    val particles = listOf(
      Offset(width * 0.18f, height * 0.22f) to 2.5f,
      Offset(width * 0.82f, height * 0.19f) to 3f,
      Offset(width * 0.12f, height * 0.52f) to 2f,
      Offset(width * 0.88f, height * 0.48f) to 3.5f,
      Offset(width * 0.28f, height * 0.78f) to 2f,
      Offset(width * 0.74f, height * 0.82f) to 2.5f,
    )
    particles.forEach { (pos, r) ->
      drawCircle(
        color = Gold400.copy(alpha = 0.5f * pulseAlpha),
        radius = r,
        center = pos,
      )
    }
  }
}

/**
 * Royal Golden Crown with Ruby & Sapphire Gems inspired by the tournament king crown.
 */
@Composable
private fun RoyalCrown(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    val crownPath = Path().apply {
      // Crown base bottom left
      moveTo(w * 0.12f, h * 0.88f)
      // Crown bottom line
      lineTo(w * 0.88f, h * 0.88f)
      // Right edge up to outer peak
      lineTo(w * 0.94f, h * 0.32f)
      // Inner right valley
      lineTo(w * 0.72f, h * 0.62f)
      // Center-right peak
      lineTo(w * 0.62f, h * 0.20f)
      // Inner center valley
      lineTo(w * 0.50f, h * 0.55f)
      // Center peak (tallest)
      lineTo(w * 0.50f, h * 0.08f)
      // Inner left valley
      lineTo(w * 0.50f, h * 0.55f)
      // Center-left peak
      lineTo(w * 0.38f, h * 0.20f)
      // Inner left valley
      lineTo(w * 0.28f, h * 0.62f)
      // Outer left peak
      lineTo(w * 0.06f, h * 0.32f)
      close()
    }

    // Golden Crown Gradient Fill
    drawPath(
      path = crownPath,
      brush = Brush.verticalGradient(
        colors = listOf(
          Color(0xFFFFFBEB),
          Gold400,
          Amber600,
          Color(0xFF78350F),
        )
      )
    )

    // Crown Golden Outline
    drawPath(
      path = crownPath,
      color = Color(0xFFFDE047),
      style = Stroke(width = 2.5f)
    )

    // Crown Base Trim Band
    val baseBand = Path().apply {
      moveTo(w * 0.10f, h * 0.74f)
      lineTo(w * 0.90f, h * 0.74f)
      lineTo(w * 0.88f, h * 0.90f)
      lineTo(w * 0.12f, h * 0.90f)
      close()
    }
    drawPath(
      path = baseBand,
      brush = Brush.verticalGradient(
        colors = listOf(Gold400, Color(0xFF92400E))
      )
    )
    drawPath(
      path = baseBand,
      color = Color(0xFFFDE047),
      style = Stroke(width = 1.5f)
    )

    // Spherical Golden Finials on Crown Peaks
    val peakFinials = listOf(
      Offset(w * 0.06f, h * 0.30f),
      Offset(w * 0.38f, h * 0.18f),
      Offset(w * 0.50f, h * 0.06f),
      Offset(w * 0.62f, h * 0.18f),
      Offset(w * 0.94f, h * 0.30f),
    )
    peakFinials.forEachIndexed { index, center ->
      val radius = if (index == 2) 4.5f else 3.5f
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color.White, Gold400, Amber600),
          center = center - Offset(1f, 1f),
          radius = radius,
        ),
        radius = radius,
        center = center,
      )
    }

    // Faceted Gemstones on Crown Base Band
    // Center Ruby (Crimson Red)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color(0xFFFDA4AF), Rose600, Color(0xFF881337)),
        center = Offset(w * 0.50f, h * 0.82f),
        radius = 5f,
      ),
      radius = 4.5f,
      center = Offset(w * 0.50f, h * 0.82f),
    )
    // Left Sapphire (Cyan Blue)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color(0xFFBAE6FD), Cyan500, Color(0xFF0369A1)),
        center = Offset(w * 0.32f, h * 0.82f),
        radius = 3.5f,
      ),
      radius = 3.5f,
      center = Offset(w * 0.32f, h * 0.82f),
    )
    // Right Sapphire (Cyan Blue)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color(0xFFBAE6FD), Cyan500, Color(0xFF0369A1)),
        center = Offset(w * 0.68f, h * 0.82f),
        radius = 3.5f,
      ),
      radius = 3.5f,
      center = Offset(w * 0.68f, h * 0.82f),
    )
  }
}

/**
 * Tournament Logo Badge housing the existing project launcher logo.
 */
@Composable
private fun TournamentLogoBadge(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center,
  ) {
    // Golden shield backplate canvas
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      val shield = Path().apply {
        moveTo(w * 0.5f, h * 0.04f)
        lineTo(w * 0.92f, h * 0.18f)
        lineTo(w * 0.88f, h * 0.66f)
        cubicTo(w * 0.88f, h * 0.84f, w * 0.68f, h * 0.96f, w * 0.5f, h * 0.98f)
        cubicTo(w * 0.32f, h * 0.96f, w * 0.12f, h * 0.84f, w * 0.12f, h * 0.66f)
        lineTo(w * 0.08f, h * 0.18f)
        close()
      }

      // Outer radial glow
      drawPath(
        path = shield,
        brush = Brush.radialGradient(
          colors = listOf(GoldGlow, Color.Transparent),
          center = Offset(w * 0.5f, h * 0.5f),
          radius = w * 0.55f,
        ),
      )

      // Dark navy shield fill
      drawPath(
        path = shield,
        brush = Brush.verticalGradient(
          colors = listOf(Color(0xFF131D33), Color(0xFF0A0F1D)),
        ),
      )

      // Gold shield border
      drawPath(
        path = shield,
        brush = Brush.verticalGradient(
          colors = listOf(Color(0xFFFDE047), Gold400, Amber600),
        ),
        style = Stroke(width = 3.5f),
      )

      // Cyan inner accent contour
      val innerShield = Path().apply {
        moveTo(w * 0.5f, h * 0.10f)
        lineTo(w * 0.84f, h * 0.22f)
        lineTo(w * 0.80f, h * 0.64f)
        cubicTo(w * 0.80f, h * 0.78f, w * 0.64f, h * 0.88f, w * 0.5f, h * 0.91f)
        cubicTo(w * 0.36f, h * 0.88f, w * 0.20f, h * 0.78f, w * 0.20f, h * 0.64f)
        lineTo(w * 0.16f, h * 0.22f)
        close()
      }
      drawPath(
        path = innerShield,
        color = Cyan400.copy(alpha = 0.5f),
        style = Stroke(width = 1.2f),
      )
    }

    // Reuse the existing official AD TOURNAMENT launcher icon asset
    Image(
      painter = painterResource(id = R.drawable.ic_launcher_foreground),
      contentDescription = "AD Tournament Logo",
      modifier = Modifier
        .size(92.dp)
        .testTag("splash_ad_logo"),
    )
  }
}

/**
 * 3D Metallic "AD TOURNAMENT" Title with gold shine and tournament banner.
 */
@Composable
private fun AdTournamentTitle() {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(contentAlignment = Alignment.Center) {
      // 3D Shadow layer
      Text(
        text = "AD TOURNAMENT",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = 2.sp,
          fontSize = 26.sp,
        ),
        color = Color(0xFF030712),
        modifier = Modifier.offset(y = 2.5.dp),
      )

      // Front metallic gold gradient text
      Text(
        text = "AD TOURNAMENT",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = 2.sp,
          fontSize = 26.sp,
          brush = Brush.verticalGradient(
            colors = listOf(
              Color(0xFFFFFBEB),
              Color(0xFFFDE047),
              Gold400,
              Amber600,
            )
          ),
        ),
      )
    }
  }
}

/**
 * Tournament Ribbon Pill with stars on sides.
 */
@Composable
private fun TournamentRibbonTag(text: String) {
  Surface(
    color = Color(0xFFB91C1C), // Tournament crimson red banner ribbon
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.2.dp, Gold400),
    shadowElevation = 4.dp,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Text(
        text = "★",
        color = Gold400,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 1.2.sp,
          fontSize = 11.sp,
        ),
        color = Color.White,
      )
      Text(
        text = "★",
        color = Gold400,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

/**
 * Isometric 3D Ludo Board with 4 colored bases, player pawns, and floating glowing dice with aura.
 */
@Composable
private fun LudoBoardAndDiceCanvas(
  floatOffset: Float,
  pulseAlpha: Float,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.58f

    // Scale dimensions for the isometric board
    val boardW = w * 0.82f
    val boardH = boardW * 0.52f
    val halfW = boardW * 0.5f
    val halfH = boardH * 0.5f

    // 1. BOARD 3D THICKNESS SLAB (Extruded Base)
    val thickness = 14f
    val bottomSlab = Path().apply {
      moveTo(cx - halfW, cy)
      lineTo(cx, cy + halfH)
      lineTo(cx + halfW, cy)
      lineTo(cx + halfW, cy + thickness)
      lineTo(cx, cy + halfH + thickness)
      lineTo(cx - halfW, cy + thickness)
      close()
    }
    drawPath(
      path = bottomSlab,
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF060913)),
        startY = cy,
        endY = cy + halfH + thickness,
      )
    )
    drawPath(
      path = bottomSlab,
      color = Color(0xFF334155),
      style = Stroke(width = 1.5f),
    )

    // 2. TOP ISOMETRIC DIAMOND SURFACE
    val boardSurface = Path().apply {
      moveTo(cx, cy - halfH)
      lineTo(cx + halfW, cy)
      lineTo(cx, cy + halfH)
      lineTo(cx - halfW, cy)
      close()
    }
    drawPath(
      path = boardSurface,
      brush = Brush.linearGradient(
        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
        start = Offset(cx, cy - halfH),
        end = Offset(cx, cy + halfH),
      )
    )
    drawPath(
      path = boardSurface,
      color = Gold400,
      style = Stroke(width = 2.2f),
    )

    // Helper function for isometric projection of normalized square [-1, 1] x [-1, 1]
    fun toIso(nx: Float, ny: Float): Offset {
      val x = cx + (nx - ny) * (halfW * 0.48f)
      val y = cy + (nx + ny) * (halfH * 0.48f)
      return Offset(x, y)
    }

    // 3. FOUR LUDO CORNER BASES (Red, Green, Blue, Yellow)
    fun drawIsoBase(
      nx: Float,
      ny: Float,
      sizeNorm: Float,
      fillColor: Color,
      borderColor: Color,
    ) {
      val path = Path().apply {
        val p1 = toIso(nx - sizeNorm, ny - sizeNorm)
        val p2 = toIso(nx + sizeNorm, ny - sizeNorm)
        val p3 = toIso(nx + sizeNorm, ny + sizeNorm)
        val p4 = toIso(nx - sizeNorm, ny + sizeNorm)
        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        lineTo(p4.x, p4.y)
        close()
      }
      drawPath(path = path, color = fillColor)
      drawPath(path = path, color = borderColor, style = Stroke(width = 1.8f))

      // Inner white disc
      val center = toIso(nx, ny)
      drawOval(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(center.x - 12f, center.y - 7f),
        size = Size(24f, 14f),
      )
    }

    // Top Quadrant (Red): (-0.5, -0.5)
    drawIsoBase(-0.52f, -0.52f, 0.40f, Color(0xFFDC2626), Color(0xFFFCA5A5))
    // Right Quadrant (Green): (0.5, -0.5)
    drawIsoBase(0.52f, -0.52f, 0.40f, Color(0xFF16A34A), Color(0xFF86EFAC))
    // Left Quadrant (Blue): (-0.5, 0.5)
    drawIsoBase(-0.52f, 0.52f, 0.40f, Color(0xFF2563EB), Color(0xFF93C5FD))
    // Bottom Quadrant (Yellow): (0.5, 0.5)
    drawIsoBase(0.52f, 0.52f, 0.40f, Color(0xFFEAB308), Color(0xFFFEF08A))

    // 4. FOUR COLORED PLAYER TOKENS (PAWNS) ON BASES
    fun drawPawn(pos: Offset, color: Color, highlight: Color) {
      // Pawn shadow
      drawOval(
        color = Color.Black.copy(alpha = 0.4f),
        topLeft = Offset(pos.x - 9f, pos.y + 2f),
        size = Size(18f, 9f),
      )
      // Pawn body
      val pawnPath = Path().apply {
        moveTo(pos.x - 7f, pos.y + 4f)
        lineTo(pos.x - 3f, pos.y - 8f)
        lineTo(pos.x + 3f, pos.y - 8f)
        lineTo(pos.x + 7f, pos.y + 4f)
        close()
      }
      drawPath(path = pawnPath, color = color)
      // Pawn head sphere
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(highlight, color, Color.Black.copy(alpha = 0.3f)),
          center = Offset(pos.x - 2f, pos.y - 12f),
          radius = 6f,
        ),
        radius = 5.5f,
        center = Offset(pos.x, pos.y - 11f),
      )
    }

    drawPawn(toIso(-0.52f, -0.52f), Color(0xFFDC2626), Color(0xFFF87171))
    drawPawn(toIso(0.52f, -0.52f), Color(0xFF16A34A), Color(0xFF4ADE80))
    drawPawn(toIso(-0.52f, 0.52f), Color(0xFF2563EB), Color(0xFF60A5FA))
    drawPawn(toIso(0.52f, 0.52f), Color(0xFFEAB308), Color(0xFFFDE047))

    // 5. CENTER GLOWING AURA RING (ORBITING ENERGY RING)
    val diceCenterY = cy - 22f + floatOffset
    val auraRadius = 38f

    // Soft yellow glow beneath dice
    drawOval(
      brush = Brush.radialGradient(
        colors = listOf(
          Gold400.copy(alpha = 0.35f * pulseAlpha),
          Gold400.copy(alpha = 0.12f),
          Color.Transparent,
        ),
        center = Offset(cx, cy + 6f),
        radius = auraRadius * 1.5f,
      ),
      topLeft = Offset(cx - auraRadius * 1.3f, cy - 8f),
      size = Size(auraRadius * 2.6f, auraRadius * 1.3f),
    )

    // Glowing Golden Orbit Ring around dice
    drawOval(
      brush = Brush.sweepGradient(
        colors = listOf(
          Gold400,
          Amber500,
          Color.White,
          Gold400,
        ),
        center = Offset(cx, diceCenterY),
      ),
      topLeft = Offset(cx - auraRadius * 1.1f, diceCenterY - auraRadius * 0.45f),
      size = Size(auraRadius * 2.2f, auraRadius * 0.9f),
      style = Stroke(width = 3.5f),
    )

    // 6. 3D CUBE LUDO DICE FLOATING ABOVE CENTER
    val diceSize = 24f
    val dTop = diceCenterY - diceSize
    val dMid = diceCenterY
    val dBot = diceCenterY + diceSize * 0.9f
    val dLeft = cx - diceSize * 1.25f
    val dRight = cx + diceSize * 1.25f

    // Drop shadow under dice on board surface
    drawOval(
      color = Color.Black.copy(alpha = 0.45f),
      topLeft = Offset(cx - 20f, cy + 4f),
      size = Size(40f, 16f),
    )

    // Top Face (White diamond, showing 6 pips)
    val topFace = Path().apply {
      moveTo(cx, dTop - diceSize * 0.4f)
      lineTo(dRight, dMid - diceSize * 0.4f)
      lineTo(cx, dMid + diceSize * 0.2f)
      lineTo(dLeft, dMid - diceSize * 0.4f)
      close()
    }
    drawPath(
      path = topFace,
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9)),
      ),
    )
    drawPath(path = topFace, color = Color(0xFFCBD5E1), style = Stroke(width = 1.5f))

    // 6 Pips on Top Face
    val topPipCenters = listOf(
      Offset(cx - 14f, dMid - diceSize * 0.35f),
      Offset(cx, dMid - diceSize * 0.35f),
      Offset(cx + 14f, dMid - diceSize * 0.35f),
      Offset(cx - 10f, dMid - diceSize * 0.05f),
      Offset(cx + 4f, dMid - diceSize * 0.05f),
      Offset(cx + 18f, dMid - diceSize * 0.05f),
    )
    topPipCenters.forEach { pip ->
      drawOval(
        color = Color(0xFF0F172A),
        topLeft = Offset(pip.x - 2.5f, pip.y - 1.5f),
        size = Size(5f, 3f),
      )
    }

    // Left Face (Shaded face, showing 4 pips)
    val leftFace = Path().apply {
      moveTo(dLeft, dMid - diceSize * 0.4f)
      lineTo(cx, dMid + diceSize * 0.2f)
      lineTo(cx, dBot)
      lineTo(dLeft, dBot - diceSize * 0.6f)
      close()
    }
    drawPath(
      path = leftFace,
      brush = Brush.horizontalGradient(
        colors = listOf(Color(0xFFCBD5E1), Color(0xFFE2E8F0)),
      ),
    )
    drawPath(path = leftFace, color = Color(0xFF94A3B8), style = Stroke(width = 1.5f))

    // 4 Pips on Left Face
    val leftPipCenters = listOf(
      Offset(cx - 18f, dMid + 2f),
      Offset(cx - 8f, dMid + 7f),
      Offset(cx - 18f, dBot - 14f),
      Offset(cx - 8f, dBot - 9f),
    )
    leftPipCenters.forEach { pip ->
      drawCircle(color = Color(0xFF0F172A), radius = 2.4f, center = pip)
    }

    // Right Face (Side face, showing 5 pips)
    val rightFace = Path().apply {
      moveTo(cx, dMid + diceSize * 0.2f)
      lineTo(dRight, dMid - diceSize * 0.4f)
      lineTo(dRight, dBot - diceSize * 0.6f)
      lineTo(cx, dBot)
      close()
    }
    drawPath(
      path = rightFace,
      brush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1)),
      ),
    )
    drawPath(path = rightFace, color = Color(0xFF64748B), style = Stroke(width = 1.5f))

    // 5 Pips on Right Face
    val rightPipCenters = listOf(
      Offset(cx + 8f, dMid + 7f),
      Offset(cx + 18f, dMid + 2f),
      Offset(cx + 13f, dMid + 13f),
      Offset(cx + 8f, dBot - 9f),
      Offset(cx + 18f, dBot - 14f),
    )
    rightPipCenters.forEach { pip ->
      drawCircle(color = Color(0xFF0F172A), radius = 2.4f, center = pip)
    }
  }
}

/**
 * Small crown icon for the bottom footer.
 */
@Composable
private fun MiniCrownIcon(
  tint: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    val path = Path().apply {
      moveTo(w * 0.15f, h * 0.85f)
      lineTo(w * 0.85f, h * 0.85f)
      lineTo(w * 0.90f, h * 0.35f)
      lineTo(w * 0.68f, h * 0.60f)
      lineTo(w * 0.50f, h * 0.20f)
      lineTo(w * 0.32f, h * 0.60f)
      lineTo(w * 0.10f, h * 0.35f)
      close()
    }
    drawPath(path = path, color = tint)
  }
}
