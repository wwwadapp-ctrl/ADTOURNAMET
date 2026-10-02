package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun ErrorState(
  message: String,
  modifier: Modifier = Modifier,
  title: String = "Unable to Load",
  onRetry: (() -> Unit)? = null,
  retryButtonText: String = "Try Again",
  testTag: String = "error_state",
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(24.dp)
      .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Box(
        modifier = Modifier
          .size(64.dp)
          .clip(CircleShape)
          .background(Rose500.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = "Error",
          tint = Rose500,
          modifier = Modifier.size(32.dp),
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = Slate50,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = Slate400,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp),
      )
      if (onRetry != null) {
        Spacer(modifier = Modifier.height(20.dp))
        TournamentButton(
          text = retryButtonText,
          onClick = onRetry,
          variant = TournamentButtonVariant.OUTLINED,
          testTag = "error_retry_button",
        )
      }
    }
  }
}
