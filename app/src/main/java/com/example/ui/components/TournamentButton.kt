package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

enum class TournamentButtonVariant {
  PRIMARY,
  SECONDARY,
  OUTLINED,
  DANGER,
  JOINED,
}

@Composable
fun TournamentButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  variant: TournamentButtonVariant = TournamentButtonVariant.PRIMARY,
  enabled: Boolean = true,
  isLoading: Boolean = false,
  leadingIcon: @Composable (() -> Unit)? = null,
  testTag: String = "tournament_button",
) {
  val shape = RoundedCornerShape(12.dp)
  val isButtonEnabled = enabled && !isLoading

  when (variant) {
    TournamentButtonVariant.PRIMARY -> {
      Button(
        onClick = onClick,
        modifier = modifier
          .defaultMinSize(minHeight = 48.dp)
          .testTag(testTag)
          .background(
            brush = if (isButtonEnabled) ElectricAmberGradient else Brush.horizontalGradient(listOf(Slate800, Slate800)),
            shape = shape,
          ),
        enabled = isButtonEnabled,
        shape = shape,
        border = if (isButtonEnabled) BorderStroke(0.8.dp, Gold400.copy(alpha = 0.4f)) else null,
        colors = ButtonDefaults.buttonColors(
          containerColor = Color.Transparent,
          contentColor = Slate950,
          disabledContainerColor = Color.Transparent,
          disabledContentColor = Slate600,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
      ) {
        ButtonContent(
          text = text,
          isLoading = isLoading,
          contentColor = if (isButtonEnabled) Slate950 else Slate600,
          leadingIcon = leadingIcon,
        )
      }
    }
    TournamentButtonVariant.SECONDARY -> {
      Button(
        onClick = onClick,
        modifier = modifier
          .defaultMinSize(minHeight = 48.dp)
          .testTag(testTag),
        enabled = isButtonEnabled,
        shape = shape,
        border = BorderStroke(0.8.dp, MidnightNavyBorder),
        colors = ButtonDefaults.buttonColors(
          containerColor = MidnightNavyCard,
          contentColor = Gold400,
          disabledContainerColor = Slate900,
          disabledContentColor = Slate600,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
      ) {
        ButtonContent(text = text, isLoading = isLoading, contentColor = Gold400, leadingIcon = leadingIcon)
      }
    }
    TournamentButtonVariant.OUTLINED -> {
      OutlinedButton(
        onClick = onClick,
        modifier = modifier
          .defaultMinSize(minHeight = 48.dp)
          .testTag(testTag),
        enabled = isButtonEnabled,
        shape = shape,
        border = BorderStroke(1.2.dp, if (isButtonEnabled) Gold400 else Slate700),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = Gold400,
          disabledContentColor = Slate600,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
      ) {
        ButtonContent(text = text, isLoading = isLoading, contentColor = Gold400, leadingIcon = leadingIcon)
      }
    }
    TournamentButtonVariant.DANGER -> {
      Button(
        onClick = onClick,
        modifier = modifier
          .defaultMinSize(minHeight = 48.dp)
          .testTag(testTag),
        enabled = isButtonEnabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
          containerColor = Rose600,
          contentColor = Color.White,
          disabledContainerColor = Slate800,
          disabledContentColor = Slate600,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
      ) {
        ButtonContent(text = text, isLoading = isLoading, contentColor = Color.White, leadingIcon = leadingIcon)
      }
    }
    TournamentButtonVariant.JOINED -> {
      Button(
        onClick = onClick,
        modifier = modifier
          .defaultMinSize(minHeight = 40.dp)
          .testTag(testTag),
        enabled = isButtonEnabled,
        shape = shape,
        border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.8f)),
        colors = ButtonDefaults.buttonColors(
          containerColor = Emerald900.copy(alpha = 0.45f),
          contentColor = Emerald400,
          disabledContainerColor = Emerald900.copy(alpha = 0.35f),
          disabledContentColor = Emerald400,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      ) {
        ButtonContent(
          text = text,
          isLoading = isLoading,
          contentColor = Emerald400,
          leadingIcon = leadingIcon ?: {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = Emerald400,
              modifier = Modifier.size(16.dp),
            )
          },
        )
      }
    }
  }
}

@Composable
private fun ButtonContent(
  text: String,
  isLoading: Boolean,
  contentColor: Color,
  leadingIcon: @Composable (() -> Unit)? = null,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    if (isLoading) {
      CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        strokeWidth = 2.5.dp,
        color = contentColor,
      )
      Spacer(modifier = Modifier.width(10.dp))
    } else if (leadingIcon != null) {
      leadingIcon()
      Spacer(modifier = Modifier.width(8.dp))
    }
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
    )
  }
}
