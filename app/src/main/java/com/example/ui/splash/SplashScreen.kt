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

    // AD premium splash artwork (visual only; see AdSplashArt.kt)
    AdSplashContent(
      progress = progress.value,
      screenWidth = screenWidth,
      compact = isCompactHeight
    )
  }
}
