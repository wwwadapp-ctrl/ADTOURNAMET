package com.example.ui.autoludo

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.config.FirebaseConfig
import com.example.core.i18n.BengaliStrings
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.digits
import com.example.core.i18n.taka
import com.example.ui.theme.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.delay

@Composable
fun AutoLudoReadyRoomDialog(
    match: AutoLudoMatchEntity,
    currentUid: String,
    onDismiss: () -> Unit,
    onReadyClick: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val isPlayer1 = match.player1Uid == currentUid
    val iAmReady = if (isPlayer1) match.player1Ready else if (match.player2Uid == currentUid) match.player2Ready else false
    val opponentReady = if (isPlayer1) match.player2Ready else match.player1Ready
    val opponentName = if (isPlayer1) match.player2Name else match.player1Name
    
    var remainingSeconds by remember { mutableLongStateOf(0L) }
    var isAdjudicating by remember { mutableStateOf(false) }

    val schedEpoch = remember(match.scheduledTimeFormatted, match.createdAt) {
        AutoLudoManager.parseScheduledEpoch(match.scheduledTimeFormatted, match.createdAt)
    }

    LaunchedEffect(schedEpoch, match.status, iAmReady, opponentReady) {
        if (match.status == "IN_GAME" || match.status == "COMPLETED" || match.status == "CANCELLED") return@LaunchedEffect

        val expiry = if (schedEpoch > 0L) {
            schedEpoch + (10 * 60 * 1000L) 
        } else if (match.countdownStartedAt > 0L) {
            match.countdownStartedAt + (10 * 60 * 1000L)
        } else {
            System.currentTimeMillis() + (10 * 60 * 1000L)
        }

        while (true) {
            val now = System.currentTimeMillis()
            val diff = (expiry - now) / 1000
            remainingSeconds = diff.coerceAtLeast(0L)
            if (remainingSeconds <= 0) {
                isAdjudicating = true
                val db = try { FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL) } catch (_: Exception) { null }
                if (db != null) {
                    val matchRef = db.getReference("miniGames/autoLudo/matches").child(match.matchId)
                    if (iAmReady && !opponentReady) {
                        val updates = hashMapOf<String, Any?>(
                            "status" to "COMPLETED",
                            "winnerUid" to currentUid,
                            "updatedAt" to ServerValue.TIMESTAMP,
                            "lastActionLog" to "Opponent absent. Walkover victory declared."
                        )
                        matchRef.updateChildren(updates) { _, _ ->
                            AutoLudoManager.claimWinPrize(match.matchId, currentUid) { onDismiss() }
                        }
                    } else if (!iAmReady && !opponentReady) {
                        val updates = hashMapOf<String, Any?>(
                            "status" to "CANCELLED",
                            "updatedAt" to ServerValue.TIMESTAMP,
                            "lastActionLog" to "Both players absent. Match cancelled."
                        )
                        matchRef.updateChildren(updates) { _, _ ->
                            AutoLudoManager.claimRefund(match.matchId, currentUid) { onDismiss() }
                        }
                    } else {
                        onDismiss()
                    }
                } else {
                    onDismiss()
                }
                break
            }
            delay(1000)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ReadyRoomAnims")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().background(MidnightBackgroundGradient),
            color = Color.Transparent
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                // Top radial glow
                Box(
                    modifier = Modifier
                        .size(400.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (-100).dp)
                        .background(Brush.radialGradient(listOf(Gold400.copy(alpha = 0.15f), Color.Transparent)))
                )
                
                // MAIN CARD
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .shadow(24.dp, RoundedCornerShape(24.dp), ambientColor = Gold400, spotColor = Gold400),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent,
                    border = BorderStroke(
                        1.5.dp, 
                        Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF9800)))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .background(Brush.verticalGradient(listOf(Color(0xFF0F1B4D), Color(0xFF2A1B5E))))
                            .drawBehind {
                                // Faint Grid/Dot Pattern
                                val dotRadius = 1.dp.toPx()
                                val spacing = 20.dp.toPx()
                                for (x in 0..size.width.toInt() step spacing.toInt()) {
                                    for (y in 0..size.height.toInt() step spacing.toInt()) {
                                        drawCircle(Color.White.copy(alpha = 0.04f), radius = dotRadius, center = Offset(x.toFloat(), y.toFloat()))
                                    }
                                }
                            }
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // HEADER
                        Text(
                            text = strings.readyRoomTitle,
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // INFO PILL
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Gold400.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, Gold400.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = strings.readyRoomEntryPrize(strings.taka(match.entryFee), strings.taka(match.prizePool)),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Gold400,
                                    letterSpacing = if (strings === BengaliStrings) 0.sp else 0.5.sp
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // VS SECTION
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // ME
                                PlayerPanel(
                                    name = strings.readyRoomPlayerYou,
                                    isMe = true,
                                    isReady = iAmReady,
                                    playerLabel = strings.readyRoomPlayer(if (isPlayer1) 1 else 2),
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Spacer(modifier = Modifier.width(48.dp))
                                
                                // OPPONENT
                                PlayerPanel(
                                    name = opponentName.ifBlank { "..." },
                                    isMe = false,
                                    isReady = opponentReady,
                                    playerLabel = strings.readyRoomPlayer(if (isPlayer1) 2 else 1),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            // VS GOLD TEXT
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .drawBehind {
                                        drawCircle(Gold400.copy(alpha = 0.1f), radius = size.minDimension / 2f)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.readyRoomVs,
                                    style = TextStyle(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        brush = Brush.verticalGradient(listOf(Color(0xFFFFE082), Color(0xFFFFB300))),
                                        shadow = Shadow(Gold400.copy(alpha = 0.5f), offset = Offset(0f, 2f), blurRadius = 8f)
                                    )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // TIMER
                        Box(contentAlignment = Alignment.Center) {
                            val isLast10 = remainingSeconds < 10 && remainingSeconds > 0
                            val timerColor by animateColorAsState(
                                if (isLast10) Color(0xFFEF4444) else Gold400,
                                label = "TimerColor"
                            )
                            
                            val timerPulse by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = if (isLast10) 1.08f else 1f,
                                animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                                label = "TimerPulse"
                            )

                            // Track
                            CircularProgressIndicator(
                                progress = 1f,
                                modifier = Modifier.size(120.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                strokeWidth = 10.dp,
                                strokeCap = StrokeCap.Round
                            )
                            // Progress
                            val progress = remainingSeconds.toFloat() / 600f // 10 mins base
                            CircularProgressIndicator(
                                progress = progress,
                                modifier = Modifier
                                    .size(120.dp)
                                    .graphicsLayer {
                                        scaleX = timerPulse
                                        scaleY = timerPulse
                                    }
                                    .drawBehind {
                                        // Glow
                                        drawCircle(
                                            color = timerColor.copy(alpha = 0.15f),
                                            radius = size.minDimension / 2 + 8.dp.toPx(),
                                            style = Stroke(width = 16.dp.toPx())
                                        )
                                    },
                                color = timerColor,
                                strokeWidth = 10.dp,
                                strokeCap = StrokeCap.Round
                            )
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val mm = remainingSeconds / 60
                                val ss = remainingSeconds % 60
                                Text(
                                    text = strings.digits(String.format("%02d:%02d", mm, ss)),
                                    style = TextStyle(
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = if (strings === BengaliStrings) 0.sp else 1.sp
                                    )
                                )
                                Text(
                                    text = strings.readyRoomStartsIn,
                                    style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // MAIN BUTTON
                        if (!iAmReady) {
                            val canPlay = remainingSeconds > 0 && !isAdjudicating
                            val shineOffset by infiniteTransition.animateFloat(
                                initialValue = -1f,
                                targetValue = 2f,
                                animationSpec = infiniteRepeatable(tween(2500, delayMillis = 500)),
                                label = "Shine"
                            )

                            Button(
                                onClick = { onReadyClick(match.matchId) },
                                enabled = canPlay,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .shadow(if (canPlay) 8.dp else 0.dp, RoundedCornerShape(28.dp)),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    disabledContainerColor = Slate800.copy(alpha = 0.5f)
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(
                                            if (canPlay) {
                                                Modifier.background(
                                                    Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFA000)))
                                                ).drawBehind {
                                                    // Shine sweep
                                                    drawRect(
                                                        brush = Brush.linearGradient(
                                                            0.0f to Color.Transparent,
                                                            0.4f to Color.White.copy(alpha = 0.4f),
                                                            0.6f to Color.White.copy(alpha = 0.4f),
                                                            1.0f to Color.Transparent,
                                                            start = Offset(size.width * shineOffset, 0f),
                                                            end = Offset(size.width * (shineOffset + 0.5f), size.height)
                                                        )
                                                    )
                                                }
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (canPlay) DeepNavyBg else Slate500)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (remainingSeconds <= 0) strings.readyRoomTimeUpDeciding else strings.readyRoomStartGame,
                                            style = TextStyle(
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (canPlay) DeepNavyBg else Slate500
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            // WAITING FOR OPPONENT
                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = Color.White.copy(alpha = 0.06f),
                                    disabledContentColor = Color.White
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Gold400, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (isAdjudicating) strings.readyRoomTimeUpDeciding else if (opponentReady) strings.readyRoomStarting else strings.readyRoomWaitingOpponent,
                                        style = TextStyle(fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // BACK BUTTON
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.height(44.dp).padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = strings.readyRoomBack, style = TextStyle(fontSize = 14.sp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerPanel(
    name: String,
    isMe: Boolean,
    isReady: Boolean,
    playerLabel: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val infiniteTransition = rememberInfiniteTransition(label = "PanelAnim")
    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "Breathe"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // RING COLOR
        val ringColor = when {
            isReady -> Color(0xFF22C55E)
            isMe -> Gold400
            else -> Color(0xFF22D3EE)
        }

        Box(contentAlignment = Alignment.Center) {
            if (isReady) {
                // Breathing Glow
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .drawBehind {
                            drawCircle(color = ringColor.copy(alpha = breatheAlpha), radius = size.minDimension / 2f)
                        }
                )
            }
            
            Surface(
                shape = CircleShape,
                modifier = Modifier.size(64.dp),
                color = Color.Transparent,
                border = BorderStroke(2.dp, ringColor)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f))
                ) {
                    Text(
                        text = name.firstOrNull()?.toString() ?: "?",
                        style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = name,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = playerLabel,
            style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // READY CHIP
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isReady) Color(0xFF22C55E).copy(alpha = 0.2f) else Color.Transparent,
            border = BorderStroke(1.dp, if (isReady) Color(0xFF22C55E) else Color.White.copy(alpha = 0.2f))
        ) {
            Text(
                text = if (isReady) strings.readyRoomReady else strings.readyRoomNotReady,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = TextStyle(
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = if (isReady) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.4f)
                )
            )
        }
    }
}
