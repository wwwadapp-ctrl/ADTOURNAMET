package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.network.NetworkStatus
import com.example.ui.theme.Rose600

@Composable
fun NetworkStatusBar(
  networkStatus: NetworkStatus,
  modifier: Modifier = Modifier,
) {
  val isOffline = networkStatus == NetworkStatus.Unavailable || networkStatus == NetworkStatus.Lost

  AnimatedVisibility(
    visible = isOffline,
    enter = expandVertically(),
    exit = shrinkVertically(),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Rose600)
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .testTag("offline_status_bar"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = Icons.Default.WifiOff,
        contentDescription = "Offline",
        tint = Color.White,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "You are currently offline. Tournament actions require a connection.",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
      )
    }
  }
}
