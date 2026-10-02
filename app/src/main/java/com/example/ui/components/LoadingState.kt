package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Gold500
import com.example.ui.theme.Slate400

@Composable
fun LoadingState(
  modifier: Modifier = Modifier,
  message: String = "Loading tournament data...",
  testTag: String = "loading_state",
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(32.dp)
      .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      CircularProgressIndicator(
        modifier = Modifier.size(44.dp),
        color = Gold500,
        strokeWidth = 3.5.dp,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = Slate400,
      )
    }
  }
}
