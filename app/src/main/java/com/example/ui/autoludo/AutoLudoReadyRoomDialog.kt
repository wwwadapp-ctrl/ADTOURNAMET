package com.example.ui.autoludo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AutoLudoReadyRoomDialog(
    match: AutoLudoMatchEntity,
    currentUid: String,
    onDismiss: () -> Unit,
    onReadyClick: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Authoritative Timer Logic: (countdownStartedAt + 10 mins) - current
    var remainingSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(match.countdownStartedAt) {
        while (true) {
            val now = System.currentTimeMillis()
            val expiry = match.countdownStartedAt + (600 * 1000L) // 10 minutes
            val diff = (expiry - now) / 1000
            remainingSeconds = diff.coerceAtLeast(0L)
            if (remainingSeconds <= 0) break
            delay(1000)
        }
    }

    val isPlayer1 = match.player1Uid == currentUid
    val isPlayer2 = match.player2Uid == currentUid
    val iAmReady = if (isPlayer1) match.player1Ready else if (isPlayer2) match.player2Ready else false
    val opponentReady = if (isPlayer1) match.player2Ready else match.player1Ready
    val opponentName = if (isPlayer1) match.player2Name else match.player1Name

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D47A1).copy(alpha = 0.9f)),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header
                Text(
                    text = "লুডু লবি",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "ম্যাচ শুরু হতে বাকি",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )

                // Timer
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60
                val timerText = String.format("%02d:%02d", minutes, seconds)
                
                Text(
                    text = timerText,
                    style = MaterialTheme.typography.displayMedium,
                    color = Amber400,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Players Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        PlayerStatusRow(
                            name = "আপনি",
                            isReady = iAmReady,
                            isMe = true
                        )
                        
                        Divider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = Slate700
                        )
                        
                        PlayerStatusRow(
                            name = opponentName.ifBlank { "অপেক্ষায়..." },
                            isReady = opponentReady,
                            isMe = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Action Button
                if (!iAmReady) {
                    Button(
                        onClick = { onReadyClick(match.matchId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("▶ প্লে করুন", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(Slate700, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (opponentReady) "ম্যাচ শুরু হচ্ছে..." else "⏳ প্রতিপক্ষের জন্য অপেক্ষা করুন",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Cancel Button
                TextButton(onClick = onDismiss) {
                    Text("পিছনে যান", color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun PlayerStatusRow(name: String, isReady: Boolean, isMe: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = if (isMe) "প্লেয়ার ১" else "প্লেয়ার ২",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isReady) Emerald600.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isReady) Emerald600 else Color.White.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isReady) Emerald500 else Color.Gray, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isReady) "প্রস্তুত" else "অপ্রস্তুত",
                    color = if (isReady) Emerald400 else Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
