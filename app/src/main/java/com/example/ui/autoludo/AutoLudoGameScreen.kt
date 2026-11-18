package com.example.ui.autoludo

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.taka
import com.example.R
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.ceil

data class LudoPlayer(
    val userId: String = "",
    val name: String = "Player",
    val avatarUrl: String = "",
    val isCurrentTurn: Boolean = false,
    val pawnColor: Long = 0xFFE53935,
    val strikes: Int = 0
)

@Composable
fun AutoLudoGameScreen(
    matchId: String,
    onBackClick: () -> Unit
) {
    val isPreviewMode = LocalInspectionMode.current || matchId.contains("preview") || matchId.contains("mock")
    val strings = LocalAppStrings.current
    val coroutineScope = rememberCoroutineScope()

    var match by remember { mutableStateOf<AutoLudoMatchEntity?>(null) }
    var showBoard by remember { mutableStateOf(isPreviewMode) } 
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    val serverConnectedOuter by rememberServerConnected()

    // Observe real-time match state from Firebase with safety
    LaunchedEffect(matchId) {
        if (!isPreviewMode) {
            try {
                AutoLudoManager.observeMatch(matchId).collect {
                    if (it != null) {
                        match = it
                    }
                }
            } catch (e: Exception) {
                match = null
            }
        }
    }


    if (match == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFBF953F))
        }
        return
    }

    val currentMatch = match!!
    val isActuallyLocal = isPreviewMode || currentMatch.matchId.contains("preview")
    
    val currentUid = remember(currentMatch, isActuallyLocal) { 
        try { 
            if (isActuallyLocal) {
                "player_1"
            } else {
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "player_1"
            }
        } catch (_: Exception) { "player_1" }
    }

    // Local Preview Game Engine State
    var localDiceValue by remember { mutableStateOf(0) }
    var localIsDiceRolled by remember { mutableStateOf(false) }
    var previewTurnUid by remember { mutableStateOf("player_1") }
    
    // Synchronize local turn with match state if available
    LaunchedEffect(currentMatch.gameState.currentTurnUid) {
        if (currentMatch.gameState.currentTurnUid.isNotBlank()) {
            previewTurnUid = currentMatch.gameState.currentTurnUid
            // Reset local dice state when turn changes
            localDiceValue = 0
            localIsDiceRolled = false
        }
    }

    // Sync local dice state with match state in live mode
    LaunchedEffect(currentMatch.gameState.diceValue, currentMatch.gameState.isDiceRolled) {
        if (!isActuallyLocal) {
            val serverDice = currentMatch.gameState.diceValue
            val serverIsRolled = currentMatch.gameState.isDiceRolled
            if (serverIsRolled && serverDice in 1..6) {
                localDiceValue = serverDice
                localIsDiceRolled = true
            } else if (!serverIsRolled && serverDice == 0 && !localIsDiceRolled) {
                localDiceValue = 0
                localIsDiceRolled = false
            }
        }
    }

    if (!showBoard) {
        AdLudoSplashScreen(onFinished = { showBoard = true })
    } else {
        // Derive effective game state for the board
        val boardGameState = currentMatch.gameState.copy(
            currentTurnUid = if (isActuallyLocal) previewTurnUid else currentMatch.gameState.currentTurnUid,
            diceValue = if (localIsDiceRolled && localDiceValue > 0) localDiceValue else currentMatch.gameState.diceValue,
            isDiceRolled = localIsDiceRolled || currentMatch.gameState.isDiceRolled
        )

        AutoLudoBoardScreen(
            match = currentMatch.copy(gameState = boardGameState),
            currentUid = currentUid,
            localDiceValue = localDiceValue,
            localIsDiceRolled = localIsDiceRolled,
            onDiceRoll = { value, isTimeout ->
                // Immediate optimistic state update
                localDiceValue = value
                localIsDiceRolled = true

                if (!isActuallyLocal) {
                    coroutineScope.launch {
                        AutoLudoManager.rollDice(matchId, currentUid, value, isTimeout)
                    }
                }
            },
            onMovePawn = { pawnIndex, newPos, isTimeout ->
                val activeMatch = currentMatch.copy(gameState = boardGameState)
                val rolledDice = boardGameState.diceValue
                
                val currentTurnUid = boardGameState.currentTurnUid
                val isP1 = currentTurnUid == activeMatch.player1Uid || currentTurnUid == "player_1"
                
                var localCapture = false
                if (newPos in 0..50) {
                    val isSafe = AutoLudoManager.isSafeCell(newPos, isP1)
                    if (!isSafe) {
                        val myGlobal = AutoLudoManager.getGlobalTrackPos(newPos, isP1)
                        val oppPawns = if (isP1) boardGameState.player2Pawns else boardGameState.player1Pawns
                        oppPawns.forEach { oppPos ->
                            if (oppPos in 0..50) {
                                val oppGlobal = AutoLudoManager.getGlobalTrackPos(oppPos, !isP1)
                                if (myGlobal == oppGlobal) {
                                    localCapture = true
                                }
                            }
                        }
                    }
                }

                val isBonusTurn = rolledDice == 6 || newPos == 56 || localCapture

                // Clear optimistic roll state immediately upon move
                localIsDiceRolled = false
                localDiceValue = 0

                if (isActuallyLocal) {
                    val p1Pawns = boardGameState.player1Pawns.toMutableList()
                    val p2Pawns = boardGameState.player2Pawns.toMutableList()
                    var p1Strikes = boardGameState.player1Strikes
                    var p2Strikes = boardGameState.player2Strikes
                    
                    if (isTimeout) {
                        if (isP1) p1Strikes++ else p2Strikes++
                    }

                    if (isP1) {
                        p1Pawns[pawnIndex] = newPos
                        if (localCapture) {
                            val myGlobal = AutoLudoManager.getGlobalTrackPos(newPos, true)
                            p2Pawns.forEachIndexed { idx, oppPos ->
                                if (oppPos in 0..50 && AutoLudoManager.getGlobalTrackPos(oppPos, false) == myGlobal) {
                                    p2Pawns[idx] = -1
                                }
                            }
                        }
                    } else {
                        p2Pawns[pawnIndex] = newPos
                        if (localCapture) {
                            val myGlobal = AutoLudoManager.getGlobalTrackPos(newPos, false)
                            p1Pawns.forEachIndexed { idx, oppPos ->
                                if (oppPos in 0..50 && AutoLudoManager.getGlobalTrackPos(oppPos, true) == myGlobal) {
                                    p1Pawns[idx] = -1
                                }
                            }
                        }
                    }
                    
                    val allP1Goal = p1Pawns.all { it == 56 }
                    val allP2Goal = p2Pawns.all { it == 56 }
                    val isDisqualified = p1Strikes >= 3 || p2Strikes >= 3
                    val isCompleted = allP1Goal || allP2Goal || isDisqualified
                    val winner = when {
                        allP1Goal -> activeMatch.player1Uid.ifBlank { "player_1" }
                        allP2Goal -> activeMatch.player2Uid.ifBlank { "player_2" }
                        p1Strikes >= 3 -> activeMatch.player2Uid.ifBlank { "player_2" }
                        p2Strikes >= 3 -> activeMatch.player1Uid.ifBlank { "player_1" }
                        else -> ""
                    }
                    
                    val nextTurn = if (isBonusTurn || isCompleted) previewTurnUid else (if (isP1) "player_2" else "player_1")
                    
                    previewTurnUid = nextTurn
                    
                    match = activeMatch.copy(
                        status = if (isCompleted) "COMPLETED" else activeMatch.status,
                        gameState = boardGameState.copy(
                            player1Pawns = p1Pawns,
                            player2Pawns = p2Pawns,
                            player1Strikes = p1Strikes,
                            player2Strikes = p2Strikes,
                            isDiceRolled = false,
                            diceValue = 0,
                            currentTurnUid = nextTurn,
                            winnerUid = winner,
                            actionStartedAt = System.currentTimeMillis(),
                            lastMoveAt = System.currentTimeMillis(),
                            lastActionLog = when {
                                isDisqualified -> "Player Disqualified (3 Misses)!"
                                localCapture -> "Captured! Bonus Roll"
                                newPos == 56 -> "Goal! Bonus Roll"
                                rolledDice == 6 -> "Rolled 6! Bonus Turn"
                                isTimeout -> "Auto-Move! Strike +1"
                                else -> "Pawn Moved"
                            }
                        )
                    )
                } else {
                    coroutineScope.launch {
                        AutoLudoManager.movePawn(matchId, currentUid, pawnIndex, newPos, isTimeout)
                    }
                }
            },
            onPassTurn = { isTimeout ->
                localIsDiceRolled = false
                localDiceValue = 0
                if (isActuallyLocal) {
                    val isP1 = previewTurnUid == currentMatch.player1Uid || previewTurnUid == "player_1"
                    val nextTurn = if (isP1) "player_2" else "player_1"
                    var p1Strikes = boardGameState.player1Strikes
                    var p2Strikes = boardGameState.player2Strikes
                    if (isTimeout) {
                        if (isP1) p1Strikes++ else p2Strikes++
                    }
                    val isDisqualified = p1Strikes >= 3 || p2Strikes >= 3
                    val winner = when {
                        p1Strikes >= 3 -> currentMatch.player2Uid.ifBlank { "player_2" }
                        p2Strikes >= 3 -> currentMatch.player1Uid.ifBlank { "player_1" }
                        else -> ""
                    }
                    
                    previewTurnUid = nextTurn
                    match = currentMatch.copy(
                        status = if (isDisqualified) "COMPLETED" else currentMatch.status,
                        gameState = currentMatch.gameState.copy(
                            currentTurnUid = nextTurn,
                            player1Strikes = p1Strikes,
                            player2Strikes = p2Strikes,
                            isDiceRolled = false,
                            diceValue = 0,
                            winnerUid = winner,
                            actionStartedAt = System.currentTimeMillis(),
                            lastActionLog = if (isDisqualified) "Disqualified (3 Misses)!" else if (isTimeout) "Auto-Pass! Strike +1" else "No Moves! Turn Passed"
                        )
                    )
                } else {
                    coroutineScope.launch {
                        AutoLudoManager.passTurn(matchId, currentUid, isTimeout)
                    }
                }
            },
            onBackClick = { showExitConfirmDialog = true }
        )
        
        // Handle back press to exit game with forfeit confirmation
        androidx.activity.compose.BackHandler {
            showExitConfirmDialog = true
        }

        // Automatic prize claim when match is completed and current user is winner
        val isWinner = currentMatch.status == "COMPLETED" && 
            (currentMatch.winnerUid == currentUid || currentMatch.gameState.winnerUid == currentUid)

        LaunchedEffect(isWinner) {
            if (isWinner && !isActuallyLocal) {
                val targetMatchId = if (currentMatch.matchId.isNotBlank()) currentMatch.matchId else matchId
                AutoLudoManager.claimWinPrize(targetMatchId, currentUid) { _ -> }
            }
        }

        // Winner / Loser Dialog
        if (currentMatch.status == "COMPLETED") {
            val winnerUid = if (currentMatch.winnerUid.isNotBlank()) currentMatch.winnerUid else currentMatch.gameState.winnerUid
            val isMe = currentUid == winnerUid
            val isOpponentForfeit = currentMatch.forfeitedByUid.isNotBlank() && currentMatch.forfeitedByUid != currentUid
            val isOpponentDisqualified = currentMatch.gameState.player1Strikes >= 3 || 
                currentMatch.gameState.player2Strikes >= 3 || 
                currentMatch.gameState.lastActionLog.contains("Disqualified")
            val winnerName = if (winnerUid == currentMatch.player1Uid) currentMatch.player1Name else if (winnerUid == currentMatch.player2Uid) currentMatch.player2Name else "বিজয়ী"
            
            LaunchedEffect(currentMatch.matchId, isMe) {
                AdGameSfx.play(if (isMe) AdGameSfx.Sfx.WIN else AdGameSfx.Sfx.LOSS)
            }

            AdResultDialog(
                isWin = isMe,
                prizePool = currentMatch.prizePool,
                winnerName = winnerName,
                opponentName = if (currentUid == currentMatch.player1Uid) currentMatch.player2Name.ifBlank { "প্লেয়ার ২" } else currentMatch.player1Name.ifBlank { "প্লেয়ার ১" },
                isOpponentForfeit = isOpponentForfeit,
                isOpponentDisqualified = isOpponentDisqualified,
                onDismiss = onBackClick
            )
        }

        // Forfeit / Exit Confirmation Dialog
        if (showExitConfirmDialog) {
            val opponentName = if (currentUid == currentMatch.player1Uid) {
                currentMatch.player2Name.ifBlank { "প্লেয়ার ২" }
            } else {
                currentMatch.player1Name.ifBlank { "প্লেয়ার ১" }
            }

            AdExitDialog(
                opponentName = opponentName,
                entryFee = currentMatch.entryFee,
                onConfirm = {
                    showExitConfirmDialog = false
                    if (serverConnectedOuter) {
                        AutoLudoManager.forfeitMatch(currentMatch.matchId, currentUid) { _, _ -> onBackClick() }
                    } else {
                        onBackClick()
                    }
                },
                onDismiss = { showExitConfirmDialog = false }
            )
        }
    }
}

@Composable
fun AutoLudoBoardScreen(
    match: AutoLudoMatchEntity,
    currentUid: String,
    localDiceValue: Int = 0,
    localIsDiceRolled: Boolean = false,
    onDiceRoll: (Int, Boolean) -> Unit,
    onMovePawn: (Int, Int, Boolean) -> Unit,
    onPassTurn: (Boolean) -> Unit = {},
    onBackClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isPreviewMode = LocalInspectionMode.current || match.matchId.contains("preview") || match.matchId.contains("mock")
    val gameState = match.gameState
    
    // Use the values already injected into the gameState by the parent
    val effectiveDiceValue = gameState.diceValue
    val isRolled = gameState.isDiceRolled
    
    // Timer Logic - strictly 15000L for both Roll and Move phases
    var timerProgress by remember { mutableStateOf(0f) }
    val totalTime = 15000L

    // Determine Local Perspective Rotation
    val isPlayer2 = currentUid == match.player2Uid
    val boardRotation = if (isPlayer2) 180f else 0f

    val context = LocalContext.current

    // Native Ticking Sound via ToneGenerator (Preserved strictly for final 5-second countdown urgency)
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 75)
        } catch (e: Exception) {
            null
        }
    }

    // Modern Low-Latency SoundPool for authentic Game Sonification (Dice Roll & Pawn Step)
    val soundPool = remember {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(audioAttributes)
                .build()
        } catch (e: Exception) {
            null
        }
    }

    var diceSoundId by remember { mutableIntStateOf(0) }
    var pawnSoundId by remember { mutableIntStateOf(0) }

    LaunchedEffect(soundPool) {
        soundPool?.let { pool ->
            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    android.util.Log.d("AutoLudoAudio", "Sample loaded successfully: $sampleId")
                }
            }
            try {
                diceSoundId = pool.load(context, R.raw.dice_roll, 1)
                pawnSoundId = pool.load(context, R.raw.pawn_step, 1)
                AdGameSfx.prepare(context)
            } catch (e: Exception) {
                android.util.Log.e("AutoLudoAudio", "Failed to load audio assets: ${e.message}")
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            soundPool?.release()
            toneGenerator?.release()
        }
    }
    
    // Identify active player turn
    val isPlayer1Turn = when {
        gameState.currentTurnUid == match.player1Uid -> true
        gameState.currentTurnUid == "player_1" -> true
        gameState.currentTurnUid.isBlank() -> true
        else -> false
    }
    val isPlayer2Turn = when {
        gameState.currentTurnUid == match.player2Uid -> true
        gameState.currentTurnUid == "player_2" -> true
        else -> false
    }

    val isActuallyLocal = isPreviewMode || match.matchId.contains("preview") || match.matchId.contains("mock")
    val isLocalOrBot = isActuallyLocal || 
        match.player2Uid.isBlank() || 
        match.player2Uid == "bot" || 
        match.player2Uid == "computer" ||
        match.player1Uid == "player_1" || 
        match.player1Uid.isBlank() || 
        currentUid == "player_1"

    // Interactive permission: allow control if it's our turn or if we are in local session
    val canControlPlayer1 = isPlayer1Turn && (
        isLocalOrBot || 
        currentUid == match.player1Uid || 
        currentUid == gameState.currentTurnUid ||
        (currentUid.isNotBlank() && currentUid != match.player2Uid)
    )

    val canControlPlayer2 = isPlayer2Turn && (
        (isLocalOrBot && match.player2Uid != "bot" && match.player2Uid != "computer" && match.player2Uid.isNotBlank()) || 
        currentUid == match.player2Uid || 
        currentUid == gameState.currentTurnUid
    )

    val isMyTurn = if (isPlayer1Turn) canControlPlayer1 else canControlPlayer2
    val serverConnected by rememberServerConnected()

    // DETERMINISTIC 15-SECOND PHASE TIMER (Keyed on turn, roll state, actionStartedAt, status)
    LaunchedEffect(gameState.currentTurnUid, isRolled, gameState.actionStartedAt, match.status) {
        if (match.status == "COMPLETED") return@LaunchedEffect
        
        val localStartTime = System.currentTimeMillis()
        var actionDispatched = false
        var lastBeepSec = 0
        
        while (!actionDispatched) {
            val now = System.currentTimeMillis()
            val elapsed = now - localStartTime
            timerProgress = (elapsed.toFloat() / totalTime).coerceIn(0f, 1f)

            // Last 5 Seconds Ticking Sound (Only for current active user)
            val remainingSec = ceil((totalTime - elapsed) / 1000f).toInt()
            if (isMyTurn && remainingSec in 1..5 && remainingSec != lastBeepSec) {
                lastBeepSec = remainingSec
                if (!AdGameSfx.muted) {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                }
            }
            
            if (timerProgress >= 1f) {
                if (!serverConnected) { delay(500L); continue }
                // If local user is observer, wait 800ms grace period so active player has first chance
                if (!isMyTurn) {
                    delay(800L)
                }
                
                if (!isRolled) {
                    // PHASE 1 TIMEOUT: Auto-Roll
                    val randomValue = if (gameState.consecutiveSixesCount >= 2) (1..5).random() else (1..6).random()
                    onDiceRoll(randomValue, true)
                    actionDispatched = true
                } else {
                    // PHASE 2 TIMEOUT: Auto-Move or Auto-Pass
                    val pawns = if (isPlayer1Turn) gameState.player1Pawns else gameState.player2Pawns
                    val eligibleIndex = pawns.indexOfFirst { p -> 
                        (p == -1 && effectiveDiceValue == 6) || (p >= 0 && p + effectiveDiceValue <= 56) 
                    }
                    if (eligibleIndex != -1) {
                        val p = pawns[eligibleIndex]
                        val newPos = if (p == -1) 0 else p + effectiveDiceValue
                        onMovePawn(eligibleIndex, newPos, true)
                    } else {
                        onPassTurn(true)
                    }
                    actionDispatched = true
                }
                
                delay(1000L)
            }
            delay(100)
        }
    }
    
    // PHASE 2: Immediate auto-pass after 1200ms if dice rolled and NO legal moves exist
    LaunchedEffect(isRolled, effectiveDiceValue, isPlayer1Turn, isPlayer2Turn) {
        if (!serverConnected) return@LaunchedEffect
        if (isRolled && effectiveDiceValue in 1..6) {
            val pawns = if (isPlayer1Turn) gameState.player1Pawns else gameState.player2Pawns
            val hasLegalMove = (effectiveDiceValue == 6 && pawns.any { it == -1 }) ||
                               pawns.any { it >= 0 && it + effectiveDiceValue <= 56 }
            
            if (!hasLegalMove) {
                delay(800L) // Wait exactly 800ms for roll animation to complete
                // Authoritative pass: any connected client (active or observer) can trigger pass when no moves exist
                onPassTurn(false)
            }
        }
    }
    
    val adPalette = remember(match.matchNumber, match.matchId) { adMatchPalette(match.matchNumber, match.matchId) }
    val player1 = LudoPlayer(
        userId = match.player1Uid.ifBlank { "player_1" },
        name = match.player1Name.ifBlank { "Player 1" },
        avatarUrl = match.player1Avatar,
        isCurrentTurn = isPlayer1Turn,
        pawnColor = (adPalette.p1Pawn.toArgb().toLong() and 0xFFFFFFFFL),
        strikes = gameState.player1Strikes
    )
    val player2 = LudoPlayer(
        userId = match.player2Uid.ifBlank { "player_2" },
        name = match.player2Name.ifBlank { "Player 2" },
        avatarUrl = match.player2Avatar,
        isCurrentTurn = isPlayer2Turn,
        pawnColor = (adPalette.p2Pawn.toArgb().toLong() and 0xFFFFFFFFL),
        strikes = gameState.player2Strikes
    )

    val adMe = if (isPlayer2) player2 else player1
    val adOpp = if (isPlayer2) player1 else player2
    val adCanRoll = (isPlayer1Turn && canControlPlayer1) || (isPlayer2Turn && canControlPlayer2)
    val adDice = rememberAdDiceContent()
    val adDiceArgs = AdDiceArgs(
        value = effectiveDiceValue,
        enabled = adCanRoll && !isRolled && serverConnected,
        isPlayer1Turn = adMe.isCurrentTurn,
        color = Color(if (adOpp.isCurrentTurn) adOpp.pawnColor else adMe.pawnColor),
        onRoll = { onDiceRoll(it, false) },
        soundPool = soundPool,
        diceSoundId = diceSoundId,
        toneGenerator = toneGenerator,
        forbidSix = gameState.consecutiveSixesCount >= 2
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF2A4BB0), Color(0xFF13205A), Color(0xFF0B1640)))))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar
            LudoTopBar(onBackClick = onBackClick)

            // Action Log Indicator (Non-blocking)
            Box(modifier = Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                if (!serverConnected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xE6B71C1C), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (strings === com.example.core.i18n.BengaliStrings) "⚠️ সংযোগ বিচ্ছিন্ন, আবার যুক্ত হওয়ার চেষ্টা চলছে..." else "⚠️ Connection lost, reconnecting...",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                } else if (gameState.lastActionLog.isNotBlank()) {
                    Text(
                        text = strings.displayActionLog(gameState.lastActionLog),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
            
            // 2. Centered Ludo Board Container (Flex-weighted to guarantee fit)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AdPlayerBattleCard(
                        player = adOpp,
                        isMe = false,
                        timerProgress = if (adOpp.isCurrentTurn) timerProgress else 0f,
                        dice = if (adOpp.isCurrentTurn) { { adDice(adDiceArgs) } } else null,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(0.94f)
                            .aspectRatio(1f)
                            .graphicsLayer {
                                rotationZ = boardRotation
                            }
                    ) {
                        val boardSizePx = constraints.maxWidth.toFloat()
                        val unit = boardSizePx / 15f

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(elevation = 22.dp, shape = RoundedCornerShape(8.dp), ambientColor = Color(0xFFFFC107), spotColor = Color(0xFFFFA000), clip = false)
                                .background(Color.White, RoundedCornerShape(8.dp))
                        )
                        AdLudoBoard(boardRotation = boardRotation, modifier = Modifier.fillMaxSize(), palette = adPalette)
                        AdTurnGlow(isPlayer1Turn = isPlayer1Turn, palette = adPalette, modifier = Modifier.fillMaxSize())
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(3.dp, Brush.linearGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC107), Color(0xFFB07A00))), RoundedCornerShape(8.dp))
                        )

                        // Render pawns as individual composables for better scaling, performance and ANIMATION
                        val density = LocalDensity.current

                        // Red Pawn Colors (Deep Royal Ruby - Saturated)
                        val redPrimary = adPalette.p1Pawn
                        val redDeep = Color(0xFF4A0000)
                        val redOutline = Color(0xFF2E0000)

                        // Yellow Pawn Colors (Rich Golden Amber - Saturated)
                        val yellowPrimary = adPalette.p2Pawn
                        val yellowDeep = Color(0xFF9E5B00)
                        val yellowOutline = Color(0xFF4E2D00)

                        // 1. Group all pawns on track to identify overlaps
                        val onTrackPawns = (gameState.player1Pawns.mapIndexed { i, p -> Triple(i, p, true) } +
                                           gameState.player2Pawns.mapIndexed { i, p -> Triple(i, p, false) })
                                           .filter { it.second >= 0 }
                        val clusterMap = onTrackPawns.groupBy { (_, p, isP1) ->
                            if (p >= 56) "home_$isP1" else getGridPositionForPath(p, isP1).let { "${it.x}_${it.y}" }
                        }
                        
                        val lastP1Pawns = remember { mutableStateOf(gameState.player1Pawns) }
                        val lastP2Pawns = remember { mutableStateOf(gameState.player2Pawns) }
                        fun attackerHops(old: List<Int>, new: List<Int>): Int {
                            var best = 0
                            for (i in new.indices) {
                                val o = old.getOrNull(i) ?: continue
                                val n = new[i]
                                if (n > o) best = maxOf(best, if (o < 0) 1 else n - o)
                            }
                            return best
                        }
                        val p1CaptureDelay = attackerHops(lastP2Pawns.value, gameState.player2Pawns) * 300L + 150L
                        val p2CaptureDelay = attackerHops(lastP1Pawns.value, gameState.player1Pawns) * 300L + 150L
                        SideEffect {
                            lastP1Pawns.value = gameState.player1Pawns
                            lastP2Pawns.value = gameState.player2Pawns
                        }

                        // 2. Render Player 1 Pawns
                        gameState.player1Pawns.forEachIndexed { index, pos ->
                            val isEligible = serverConnected && canControlPlayer1 && isRolled && (
                                (pos == -1 && effectiveDiceValue == 6) ||
                                (pos >= 0 && pos + effectiveDiceValue <= 56)
                            )
                            
                            val cluster = if (pos >= 0) clusterMap[if (pos >= 56) "home_true" else getGridPositionForPath(pos, true).let { "${it.x}_${it.y}" }] ?: emptyList() else emptyList()
                            val stackIdx = if (pos >= 0) cluster.indexOfFirst { it.first == index && it.third } else 0
                            val total = if (pos >= 0) cluster.size else 1

                            LudoPawnView(
                                targetPos = pos,
                                index = index,
                                isPlayer1 = true,
                                boardSizePx = boardSizePx,
                                pawnColor = redPrimary,
                                deepShadeColor = redDeep,
                                outlineColor = redOutline,
                                isEligibleToMove = isEligible,
                                boardRotation = boardRotation,
                                stackIndex = stackIdx,
                                totalInStack = total,
                                soundPool = soundPool,
                                pawnSoundId = pawnSoundId,
                                toneGenerator = toneGenerator,
                                captureDelayMs = p1CaptureDelay,
                                onClick = {
                                    val dice = effectiveDiceValue
                                    var newPos = pos
                                    if (pos == -1 && dice == 6) {
                                        newPos = 0 
                                    } else if (pos >= 0) {
                                        newPos = pos + dice
                                        if (newPos > 56) return@LudoPawnView
                                    }
                                    if (newPos != pos) {
                                        onMovePawn(index, newPos, false)
                                    }
                                }
                            )
                        }

                        // 3. Render Player 2 Pawns
                        gameState.player2Pawns.forEachIndexed { index, pos ->
                            val isEligible = serverConnected && canControlPlayer2 && isRolled && (
                                (pos == -1 && effectiveDiceValue == 6) ||
                                (pos >= 0 && pos + effectiveDiceValue <= 56)
                            )
                            
                            val cluster = if (pos >= 0) clusterMap[if (pos >= 56) "home_false" else getGridPositionForPath(pos, false).let { "${it.x}_${it.y}" }] ?: emptyList() else emptyList()
                            val stackIdx = if (pos >= 0) cluster.indexOfFirst { it.first == index && !it.third } else 0
                            val total = if (pos >= 0) cluster.size else 1

                            LudoPawnView(
                                targetPos = pos,
                                index = index,
                                isPlayer1 = false,
                                boardSizePx = boardSizePx,
                                pawnColor = yellowPrimary,
                                deepShadeColor = yellowDeep,
                                outlineColor = yellowOutline,
                                isEligibleToMove = isEligible,
                                boardRotation = boardRotation,
                                stackIndex = stackIdx,
                                totalInStack = total,
                                soundPool = soundPool,
                                pawnSoundId = pawnSoundId,
                                toneGenerator = toneGenerator,
                                captureDelayMs = p2CaptureDelay,
                                onClick = {
                                    val dice = effectiveDiceValue
                                    var newPos = pos
                                    if (pos == -1 && dice == 6) {
                                        newPos = 0 
                                    } else if (pos >= 0) {
                                        newPos = pos + dice
                                        if (newPos > 56) return@LudoPawnView
                                    }
                                    if (newPos != pos) {
                                        onMovePawn(index, newPos, false)
                                    }
                                }
                            )
                        }
                    }

                    AdPlayerBattleCard(
                        player = adMe,
                        isMe = true,
                        timerProgress = if (adMe.isCurrentTurn) timerProgress else 0f,
                        dice = if (!adOpp.isCurrentTurn) { { adDice(adDiceArgs) } } else null,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).zIndex(20f)
                    )
                }
            }

            // 3. Bottom Battle Dock (Always visible)
            val canActivePlayerRoll = (isPlayer1Turn && canControlPlayer1) || (isPlayer2Turn && canControlPlayer2)
        }
    }
}

@Composable
fun LudoTopBar(onBackClick: () -> Unit) {
    var isMuted by remember { mutableStateOf(AdGameSfx.muted) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.background(Color(0xFF1565C0).copy(alpha = 0.6f), CircleShape) // Vibrant Blue
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        IconButton(
            onClick = { isMuted = !isMuted; AdGameSfx.muted = isMuted },
            modifier = Modifier.background(Color(0xFF1565C0).copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = "Sound",
                tint = Color.White
            )
        }
    }
}

@Composable
fun LudoPawnView(
    targetPos: Int,
    index: Int,
    isPlayer1: Boolean,
    boardSizePx: Float,
    pawnColor: Color,
    deepShadeColor: Color,
    outlineColor: Color,
    isEligibleToMove: Boolean,
    boardRotation: Float = 0f,
    stackIndex: Int = 0,
    totalInStack: Int = 1,
    soundPool: SoundPool? = null,
    pawnSoundId: Int = 0,
    toneGenerator: android.media.ToneGenerator? = null,
    captureDelayMs: Long = 0L,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    
    // Maintain visual position state for step-by-step hopping
    var visualPos by remember { mutableStateOf(targetPos) }
    val animOffset = remember { Animatable(getPawnOffset(targetPos, index, boardSizePx, isPlayer1, stackIndex, totalInStack), Offset.VectorConverter) }
    
    // Jump height animation for parabolic effect
    val jumpAnim = remember { Animatable(0f) }

    LaunchedEffect(targetPos, boardSizePx, stackIndex, totalInStack) {
        if (boardSizePx <= 0f) return@LaunchedEffect
        
        if (targetPos == visualPos) {
             animOffset.animateTo(getPawnOffset(targetPos, index, boardSizePx, isPlayer1, stackIndex, totalInStack), tween(300))
             return@LaunchedEffect
        }

        // Logic for movement: step-by-step hopping
        if (visualPos == -1 && targetPos >= 0) {
            // From yard to start: one big jump
            val startOffset = getPawnOffset(0, index, boardSizePx, isPlayer1, stackIndex, totalInStack)
            launch {
                jumpAnim.animateTo(-24f, tween(180, easing = FastOutLinearInEasing))
                jumpAnim.animateTo(0f, tween(180, easing = LinearOutSlowInEasing))
            }
            animOffset.animateTo(startOffset, tween(360))
            visualPos = 0
            if (!AdGameSfx.muted) {
                try {
                    val streamId = if (soundPool != null && pawnSoundId > 0) {
                        soundPool.play(pawnSoundId, 1f, 1f, 1, 0, 1f)
                    } else 0
                    if (streamId == 0) {
                        toneGenerator?.startTone(android.media.ToneGenerator.TONE_DTMF_1, 60)
                    }
                } catch (e: Exception) { }
            }
        }
        
        if (targetPos > visualPos && visualPos >= 0) {
            // Step-by-step hop through the track
            for (step in (visualPos + 1)..targetPos) {
                val stepOffset = if (step == targetPos) {
                    getPawnOffset(step, index, boardSizePx, isPlayer1, stackIndex, totalInStack)
                } else {
                    getPawnOffset(step, index, boardSizePx, isPlayer1)
                }
                launch {
                    jumpAnim.animateTo(-18f, tween(150, easing = FastOutLinearInEasing))
                    jumpAnim.animateTo(0f, tween(150, easing = LinearOutSlowInEasing))
                }
                animOffset.animateTo(stepOffset, tween(300, easing = LinearEasing))
                visualPos = step
                if (!AdGameSfx.muted) {
                    try {
                        val streamId = if (soundPool != null && pawnSoundId > 0) {
                            soundPool.play(pawnSoundId, 1f, 1f, 1, 0, 1f)
                        } else 0
                        if (streamId == 0) {
                            toneGenerator?.startTone(android.media.ToneGenerator.TONE_DTMF_1, 60)
                        }
                    } catch (e: Exception) { }
                }
            }
            if (targetPos == 56) {
                AdGameSfx.play(AdGameSfx.Sfx.GOAL)
            } else if (targetPos in 0..50 && AutoLudoManager.isSafeCell(targetPos, isPlayer1)) {
                AdGameSfx.play(AdGameSfx.Sfx.STAR)
            }
        } else if (targetPos < visualPos) {
            // Captured (Cut) or sent back to yard
            if (targetPos == -1 && visualPos >= 0) {
                if (captureDelayMs > 0L) kotlinx.coroutines.delay(captureDelayMs)
                AdGameSfx.play(AdGameSfx.Sfx.CAPTURE)

                // High-speed reverse slide along the exact traveled track down to track coordinate 0 (40ms per step)
                val startTrackPos = visualPos.coerceAtMost(56)
                for (step in (startTrackPos - 1) downTo 0) {
                    val rewindOffset = getPawnOffset(step, index, boardSizePx, isPlayer1)
                    animOffset.animateTo(rewindOffset, tween(40, easing = LinearEasing))
                    visualPos = step
                }

                // Final landing hop from track 0 into yard position -1
                val yardOffset = getPawnOffset(-1, index, boardSizePx, isPlayer1, stackIndex, totalInStack)
                launch {
                    jumpAnim.animateTo(-16f, tween(110, easing = FastOutLinearInEasing))
                    jumpAnim.animateTo(0f, tween(110, easing = LinearOutSlowInEasing))
                }
                animOffset.animateTo(yardOffset, tween(200, easing = FastOutSlowInEasing))
                visualPos = -1
            } else {
                // Direct retreat fallback if negative or relative
                animOffset.animateTo(getPawnOffset(targetPos, index, boardSizePx, isPlayer1, stackIndex, totalInStack), tween(300))
                visualPos = targetPos
            }
        }
    }

    val eligibleScale by if (isEligibleToMove) {
        rememberInfiniteTransition(label = "pawnBreath").animateFloat(
            initialValue = 1.0f,
            targetValue = 1.14f,
            animationSpec = infiniteRepeatable(
                animation = tween(650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eligibleScale"
        )
    } else {
        rememberUpdatedState(1.0f)
    }

    val pawnWidth = 24.dp
    val pawnHeight = 34.dp

    val isRotated180 = boardRotation == 180f

    val stackScale = when {
        targetPos >= 56 -> 0.72f
        totalInStack >= 5 -> 0.58f
        totalInStack >= 3 -> 0.70f
        totalInStack > 1 -> 0.82f
        else -> 1f
    }

    Box(
        modifier = Modifier
            .zIndex(if (isEligibleToMove) 100f else (stackIndex.toFloat() + 1f))
            .offset {
                val yOffset = if (isRotated180) {
                    (animOffset.value.y - jumpAnim.value).roundToInt()
                } else {
                    (animOffset.value.y + jumpAnim.value - pawnHeight.toPx()).roundToInt()
                }
                IntOffset(
                    x = (animOffset.value.x - (pawnWidth / 2).toPx()).roundToInt(),
                    y = yOffset
                )
            }
            .graphicsLayer {
                rotationZ = -boardRotation
                transformOrigin = if (isRotated180) {
                    androidx.compose.ui.graphics.TransformOrigin.Center
                } else {
                    androidx.compose.ui.graphics.TransformOrigin(0.5f, 1.0f)
                }
                scaleX = eligibleScale * stackScale
                scaleY = eligibleScale * stackScale
            }
            .size(width = pawnWidth, height = pawnHeight)
            .clickable(
                enabled = isEligibleToMove,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onClick()
            }
    ) {
        AdLudoPawnToken(
            pawnColor = pawnColor,
            isEligible = isEligibleToMove,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Calibrated home base 4-circle coordinates for all 4 colors (Red, Yellow, Green, Blue)
 * Exactly concentric within the 15x15 Ludo board artwork discs.
 */
fun getHomeBaseCircleOffset(colorName: String, index: Int, boardSize: Float): Offset {
    val unit = boardSize / 15f
    val (baseCol, baseRow) = when (colorName.uppercase()) {
        "GREEN" -> Pair(0f, 0f)
        "YELLOW" -> Pair(9f, 0f)
        "RED" -> Pair(0f, 9f)
        "BLUE" -> Pair(9f, 9f)
        else -> Pair(0f, 9f)
    }
    // Each 6x6 base has 4 concentric circle spots at relative (2.0, 2.0), (4.0, 2.0), (2.0, 4.0), (4.0, 4.0)
    val colOffset = if (index % 2 == 0) 2.0f else 4.0f
    val rowOffset = if (index < 2) 2.0f else 4.0f
    return Offset((baseCol + colOffset) * unit, (baseRow + rowOffset) * unit)
}

// Helper to calculate a single pawn offset
private fun getPawnOffset(
    pos: Int, 
    index: Int, 
    boardSize: Float, 
    isPlayer1: Boolean,
    stackIndex: Int = 0,
    totalInStack: Int = 1
): Offset {
    if (pos == -1) {
        // Home yard positions aligned to exact 15x15 grid concentric circle centers
        return if (isPlayer1) {
            getHomeBaseCircleOffset("RED", index, boardSize)
        } else {
            getHomeBaseCircleOffset("YELLOW", index, boardSize)
        }
    } else {
        // Real path mapping logic
        val unit = boardSize / 15f
        if (pos >= 56) {
            val footY = if (isPlayer1) 8.55f else 6.45f
            val x = 7.5f + (stackIndex - (totalInStack - 1) / 2f) * 0.42f
            return Offset(x * unit, footY * unit)
        }
        val gridPos = getGridPositionForPath(pos, isPlayer1)
        var baseOffset = Offset(gridPos.x * unit + unit / 2f, gridPos.y * unit + unit / 2f)

        // Apply micro-offset clustering if multiple pawns are in the same cell
        if (totalInStack > 1) {
            val shift = unit * (when { totalInStack >= 5 -> 0.34f; totalInStack >= 3 -> 0.28f; else -> 0.22f }) // Fan-out distance within the cell
            val angle = (stackIndex * 360f / totalInStack).toDouble()
            val rad = Math.toRadians(angle)
            baseOffset = Offset(
                baseOffset.x + (shift * Math.cos(rad)).toFloat(),
                baseOffset.y + (shift * Math.sin(rad)).toFloat()
            )
        }
        return baseOffset
    }
}

private data class GridPos(val x: Int, val y: Int)

private fun getGridPositionForPath(pos: Int, isPlayer1: Boolean): GridPos {
    // Basic Ludo path for a 15x15 grid
    val pathPoints = listOf(
        // Segment 1: Red Start area (Left-Middle)
        GridPos(1, 6), GridPos(2, 6), GridPos(3, 6), GridPos(4, 6), GridPos(5, 6),
        GridPos(6, 5), GridPos(6, 4), GridPos(6, 3), GridPos(6, 2), GridPos(6, 1), GridPos(6, 0),
        GridPos(7, 0), GridPos(8, 0),
        GridPos(8, 1), GridPos(8, 2), GridPos(8, 3), GridPos(8, 4), GridPos(8, 5),
        GridPos(9, 6), GridPos(10, 6), GridPos(11, 6), GridPos(12, 6), GridPos(13, 6), GridPos(14, 6),
        GridPos(14, 7), GridPos(14, 8),
        GridPos(13, 8), GridPos(12, 8), GridPos(11, 8), GridPos(10, 8), GridPos(9, 8),
        GridPos(8, 9), GridPos(8, 10), GridPos(8, 11), GridPos(8, 12), GridPos(8, 13), GridPos(8, 14),
        GridPos(7, 14), GridPos(6, 14),
        GridPos(6, 13), GridPos(6, 12), GridPos(6, 11), GridPos(6, 10), GridPos(6, 9),
        GridPos(5, 8), GridPos(4, 8), GridPos(3, 8), GridPos(2, 8), GridPos(1, 8), GridPos(0, 8),
        GridPos(0, 7), GridPos(0, 6)
    )
    
    // Home stretch mapping
    if (pos >= 51) {
        val stretchIdx = (pos - 51).coerceIn(0, 5)
        return if (isPlayer1) {
            // Red: Column 7 moving UP: 51->(7,13), 52->(7,12), 53->(7,11), 54->(7,10), 55->(7,9), 56->(7,7)
            when (stretchIdx) {
                0 -> GridPos(7, 13)
                1 -> GridPos(7, 12)
                2 -> GridPos(7, 11)
                3 -> GridPos(7, 10)
                4 -> GridPos(7, 9)
                else -> GridPos(7, 7) // Final Center Goal
            }
        } else {
            // Yellow: Column 7 moving DOWN: 51->(7,1), 52->(7,2), 53->(7,3), 54->(7,4), 55->(7,5), 56->(7,7)
            when (stretchIdx) {
                0 -> GridPos(7, 1)
                1 -> GridPos(7, 2)
                2 -> GridPos(7, 3)
                3 -> GridPos(7, 4)
                4 -> GridPos(7, 5)
                else -> GridPos(7, 7) // Final Center Goal
            }
        }
    }
    
    val p1StartIdx = 39 // Red starts at (1, 8) - Bottom Arm Exit
    val p2StartIdx = 13 // Yellow starts at (13, 6) - Top Arm Exit
    
    val actualIdx = if (isPlayer1) {
        (p1StartIdx + pos) % 52
    } else {
        (p2StartIdx + pos) % 52
    }
    
    return pathPoints.getOrElse(actualIdx) { GridPos(7, 7) }
}

@Composable
fun Luxury3DCubeDice(
    diceValue: Int,
    isEnabled: Boolean,
    isPlayer1Turn: Boolean,
    diceColor: Color = if (isPlayer1Turn) Color(0xFFEF4444) else Color(0xFFF59E0B),
    onRoll: (Int) -> Unit,
    soundPool: SoundPool? = null,
    diceSoundId: Int = 0,
    toneGenerator: android.media.ToneGenerator? = null,
    forbidSix: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    val localDensity = LocalDensity.current
    var isRolling by remember { mutableStateOf(false) }
    var displayValue by remember { mutableStateOf(if (diceValue in 1..6) diceValue else 1) }
    var faceValue by remember { mutableIntStateOf(if (diceValue in 1..6) diceValue else 1) }
    var lastSettledValue by remember { mutableIntStateOf(diceValue) }
    var isLocalRollInitiated by remember { mutableStateOf(false) }
    
    // Physical Animatables
    val jumpY = remember { Animatable(0f) }
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }
    val rotZ = remember { Animatable(0f) }
    val squashX = remember { Animatable(1f) }
    val squashY = remember { Animatable(1f) }
    
    // Shadow Animatables
    val shadowAlpha = remember { Animatable(0.4f) }
    val shadowScale = remember { Animatable(1f) }

    fun triggerRollAnimation(targetValue: Int, isLocal: Boolean) {
        if (isRolling) return
        isRolling = true
        faceValue = targetValue
        if (!AdGameSfx.muted) {
            try {
                val streamId = if (soundPool != null && diceSoundId > 0) {
                    soundPool.play(diceSoundId, 1f, 1f, 1, 0, 1f)
                } else 0
                if (streamId == 0) {
                    toneGenerator?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 120)
                }
            } catch (_: Exception) { }
        }

        coroutineScope.launch {
            val jumpTarget = -localDensity.run { 65.dp.toPx() }
            
            // Phase 1: Anticipation Squash
            squashY.animateTo(0.82f, tween(60, easing = FastOutSlowInEasing))
            squashX.animateTo(1.15f, tween(60, easing = FastOutSlowInEasing))

            // Phase 2: Parabolic Tumbling Jump & Erratic Multi-axis Rotation
            launch {
                // shadow fading
                launch {
                    shadowAlpha.animateTo(0.12f, tween(250))
                    shadowScale.animateTo(1.4f, tween(250))
                }
                jumpY.animateTo(jumpTarget, tween(250, easing = LinearOutSlowInEasing))
                
                // Return with shadow tightening
                launch {
                    shadowAlpha.animateTo(0.55f, tween(250))
                    shadowScale.animateTo(0.85f, tween(250))
                }
                jumpY.animateTo(0f, tween(250, easing = FastOutLinearInEasing))
                
                // Phase 3: Impact Bounce & Settle
                launch {
                    shadowAlpha.animateTo(0.4f, spring(Spring.DampingRatioMediumBouncy))
                    shadowScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy))
                }
                
                squashY.animateTo(0.85f, tween(80))
                squashY.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
                squashX.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
            }
            
            // Multi-axis Erratic Tumbling (Synchronized to not clash with initial squash)
            launch {
                squashY.animateTo(1f, tween(100))
                squashX.animateTo(1f, tween(100))
            }
            launch { rotX.animateTo(rotX.value + 720f, tween(500, easing = FastOutSlowInEasing)) }
            launch { rotY.animateTo(rotY.value + 1080f, tween(500, easing = FastOutSlowInEasing)) }
            launch { rotZ.animateTo(rotZ.value + 360f, tween(500, easing = FastOutSlowInEasing)) }

            // Rapid Number Shuffling
            val tumbleJob = launch {
                while (true) {
                    displayValue = (1..6).random()
                    delay(50)
                }
            }
            
            delay(500)
            tumbleJob.cancel()
            
            // Final Settle Value
            displayValue = targetValue
            
            // Snap tilt to neutral
            rotX.snapTo(0f)
            rotY.snapTo(0f)
            rotZ.snapTo(0f)
            
            delay(200)
            if (isLocal) {
                onRoll(targetValue)
            }
            isRolling = false
        }
    }

    LaunchedEffect(diceValue) {
        if (diceValue in 1..6) {
            if (isLocalRollInitiated) {
                // Local player already completed animation; sync state without replaying
                isLocalRollInitiated = false
                lastSettledValue = diceValue
                if (!isRolling) displayValue = diceValue
            } else if (diceValue != lastSettledValue && !isRolling) {
                // Remote player roll or authoritative timeout roll: run 3D tumble & play audio
                lastSettledValue = diceValue
                triggerRollAnimation(targetValue = diceValue, isLocal = false)
            }
        } else if (diceValue == 0) {
            // Reset for next turn roll
            lastSettledValue = 0
            isLocalRollInitiated = false
        }
    }

    Box(
        modifier = Modifier
            .size(80.dp)
            .zIndex(50f),
        contentAlignment = Alignment.Center
    ) {
        // 1. Authoritative Dynamic Drop Shadow (Anchored to ground)
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-4).dp)
                .size(width = 40.dp, height = 12.dp)
        ) {
            drawOval(
                color = Color.Black.copy(alpha = shadowAlpha.value),
                size = Size(size.width * shadowScale.value, size.height * shadowScale.value),
                topLeft = Offset(
                    (size.width * (1f - shadowScale.value)) / 2,
                    (size.height * (1f - shadowScale.value)) / 2
                )
            )
        }

        // 2. The Volumetric 3D Cube Dice
        Box(
            modifier = Modifier
                .size(54.dp)
                .graphicsLayer {
                    translationY = jumpY.value
                    rotationZ = rotZ.value
                    scaleX = squashX.value
                    scaleY = squashY.value
                    cameraDistance = 12f * density
                }
                .clickable(
                    enabled = isEnabled && !isRolling,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!isRolling) {
                        isLocalRollInitiated = true
                        val localRoll = if (forbidSix) (1..5).random() else (1..6).random()
                        lastSettledValue = localRoll
                        triggerRollAnimation(targetValue = localRoll, isLocal = true)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AdDice3D(
                value = if (isRolling) faceValue else displayValue,
                spinX = rotX.value,
                spinY = rotY.value,
                color = diceColor,
                modifier = Modifier.size(54.dp)
            )
        }
    }
}

