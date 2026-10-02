package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun TournamentCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  borderColor: Color = MidnightNavyBorder,
  backgroundColor: Color = MidnightNavyCard,
  elevation: Dp = 3.dp,
  testTag: String = "tournament_card",
  backgroundContent: (@Composable BoxScope.() -> Unit)? = null,
  bottomBar: (@Composable () -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val cardShape = RoundedCornerShape(16.dp)
  val clickableModifier = if (onClick != null) {
    modifier
      .testTag(testTag)
      .clickable(onClick = onClick)
  } else {
    modifier.testTag(testTag)
  }

  Card(
    modifier = clickableModifier,
    shape = cardShape,
    colors = CardDefaults.cardColors(containerColor = backgroundColor),
    border = BorderStroke(0.8.dp, borderColor),
    elevation = CardDefaults.cardElevation(defaultElevation = elevation),
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      if (backgroundContent != null) {
        backgroundContent()
      }
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        content = content,
      )
    }
    if (bottomBar != null) {
      bottomBar()
    }
  }
}
