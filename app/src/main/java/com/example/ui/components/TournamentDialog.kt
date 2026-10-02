package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun TournamentDialog(
  title: String,
  message: String,
  onDismiss: () -> Unit,
  confirmButtonText: String = "Confirm",
  onConfirm: (() -> Unit)? = null,
  dismissButtonText: String? = "Cancel",
  testTag: String = "tournament_dialog",
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag(testTag),
    shape = RoundedCornerShape(16.dp),
    containerColor = Slate900,
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = Slate50,
      )
    },
    text = {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = Slate400,
      )
    },
    confirmButton = {
      if (onConfirm != null) {
        TournamentButton(
          text = confirmButtonText,
          onClick = {
            onConfirm()
            onDismiss()
          },
          variant = TournamentButtonVariant.PRIMARY,
          testTag = "dialog_confirm_button",
        )
      } else {
        TournamentButton(
          text = "OK",
          onClick = onDismiss,
          variant = TournamentButtonVariant.PRIMARY,
          testTag = "dialog_ok_button",
        )
      }
    },
    dismissButton = {
      if (dismissButtonText != null && onConfirm != null) {
        TournamentButton(
          text = dismissButtonText,
          onClick = onDismiss,
          variant = TournamentButtonVariant.OUTLINED,
          testTag = "dialog_dismiss_button",
        )
      }
    },
  )
}
