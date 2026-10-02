package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun EmptyState(
  title: String,
  description: String,
  modifier: Modifier = Modifier,
  icon: ImageVector = Icons.Default.EmojiEvents,
  actionButtonText: String? = null,
  onActionClick: (() -> Unit)? = null,
  testTag: String = "empty_state",
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
          .size(72.dp)
          .clip(CircleShape)
          .background(Gold500.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = Gold500,
          modifier = Modifier.size(36.dp),
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
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = Slate400,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 24.dp),
      )
      if (actionButtonText != null && onActionClick != null) {
        Spacer(modifier = Modifier.height(20.dp))
        TournamentButton(
          text = actionButtonText,
          onClick = onActionClick,
          variant = TournamentButtonVariant.PRIMARY,
          testTag = "empty_action_button",
        )
      }
    }
  }
}
