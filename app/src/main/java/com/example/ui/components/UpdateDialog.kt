package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.DeepNavyBg
import com.example.ui.theme.Gold400
import com.example.ui.theme.Slate300

@Composable
fun ForceUpdateDialog(
  remoteVersionName: String = "1.0.1",
  updateUrl: String = "https://adturnamet.web.app"
) {
  val context = LocalContext.current

  Dialog(
    onDismissRequest = { /* Non-dismissible */ },
    properties = DialogProperties(
      dismissOnBackPress = false,
      dismissOnClickOutside = false,
      usePlatformDefaultWidth = false
    )
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.85f)
        .wrapContentHeight(),
      shape = RoundedCornerShape(24.dp),
      color = DeepNavyBg,
      tonalElevation = 8.dp
    ) {
      Column(
        modifier = Modifier
          .padding(24.dp)
          .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .background(Gold400.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.SystemUpdate,
            contentDescription = null,
            tint = Gold400,
            modifier = Modifier.size(40.dp)
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = "New Update Available!",
          style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            color = Color.White
          ),
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "A new version ($remoteVersionName) is available. Please update to the latest version to continue playing and enjoy new features.",
          style = MaterialTheme.typography.bodyMedium.copy(
            color = Slate300,
            lineHeight = 20.sp
          ),
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
          onClick = {
            try {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
              context.startActivity(intent)
            } catch (e: Exception) {
              // Fallback
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Gold400,
            contentColor = DeepNavyBg
          ),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(
            text = "Update Now",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Black
            )
          )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
          text = "Required for security and performance",
          style = MaterialTheme.typography.labelSmall.copy(
            color = Slate300.copy(alpha = 0.6f)
          ),
          textAlign = TextAlign.Center
        )
      }
    }
  }
}
