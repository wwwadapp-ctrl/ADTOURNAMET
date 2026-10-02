package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Gold400
import com.example.ui.theme.MidnightNavyBorder
import com.example.ui.theme.MidnightNavyCard
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate300

@Composable
fun PremiumNotificationBellButton(
  unreadCount: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String = "home_notification_button",
  contentDescription: String = "Notifications",
) {
  val rotation = remember { Animatable(0f) }
  var prevCount by remember { mutableIntStateOf(unreadCount) }
  var isInitial by remember { mutableStateOf(true) }

  LaunchedEffect(unreadCount) {
    if (isInitial) {
      isInitial = false
      prevCount = unreadCount
      return@LaunchedEffect
    }
    // Subtle shake animation triggers ONLY when new unread notifications arrive
    if (unreadCount > prevCount) {
      val angles = listOf(-10f, 10f, -7f, 7f, -4f, 4f, 0f)
      for (angle in angles) {
        rotation.animateTo(
          targetValue = angle,
          animationSpec = tween(durationMillis = 40, easing = FastOutSlowInEasing)
        )
      }
    }
    prevCount = unreadCount
  }

  Surface(
    onClick = onClick,
    shape = CircleShape,
    color = MidnightNavyCard,
    border = BorderStroke(
      0.8.dp,
      if (unreadCount > 0) Rose600.copy(alpha = 0.4f) else MidnightNavyBorder
    ),
    modifier = modifier
      .size(40.dp)
      .testTag(testTag),
  ) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Default.Notifications,
        contentDescription = contentDescription,
        tint = if (unreadCount > 0) Gold400 else Slate300,
        modifier = Modifier
          .size(22.dp)
          .graphicsLayer {
            rotationZ = rotation.value
          },
      )
      if (unreadCount > 0) {
        Box(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 8.dp, end = 8.dp)
            .size(7.dp)
            .clip(CircleShape)
            .background(Rose600)
            .border(1.dp, MidnightNavyCard, CircleShape)
            .testTag("notification_unread_dot"),
        )
      }
    }
  }
}
