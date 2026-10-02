package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun StatusBadge(
  statusText: String,
  modifier: Modifier = Modifier,
  badgeColor: Color = Gold500,
  testTag: String = "status_badge",
) {
  val (bgColor, textColor, borderColor) = when (statusText.uppercase()) {
    "LIVE" -> Triple(Rose500.copy(alpha = 0.2f), Rose500, Rose500.copy(alpha = 0.5f))
    "UPCOMING", "PENDING", "PENDING_REVIEW", "SUBMITTED", "অপেক্ষমাণ" -> Triple(Gold500.copy(alpha = 0.2f), Gold400, Gold500.copy(alpha = 0.5f))
    "COMPLETED", "APPROVED", "সম্পন্ন" -> Triple(Emerald500.copy(alpha = 0.2f), Emerald500, Emerald500.copy(alpha = 0.5f))
    "CANCELLED", "FAILED" -> Triple(Slate700.copy(alpha = 0.4f), Slate400, Slate600)
    "REJECTED", "বাতিল", "প্রত্যাখ্যাত" -> Triple(Rose500.copy(alpha = 0.2f), Rose400, Rose500.copy(alpha = 0.5f))
    else -> Triple(badgeColor.copy(alpha = 0.2f), badgeColor, badgeColor.copy(alpha = 0.5f))
  }

  Box(
    modifier = modifier
      .testTag(testTag)
      .background(bgColor, shape = RoundedCornerShape(8.dp))
      .border(1.dp, borderColor, shape = RoundedCornerShape(8.dp))
      .padding(horizontal = 10.dp, vertical = 4.dp),
  ) {
    Text(
      text = statusText.replace("_", " "),
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
      color = textColor,
    )
  }
}
