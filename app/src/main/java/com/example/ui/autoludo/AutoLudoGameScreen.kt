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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import coil.compose.AsyncImage
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()
    val mockMatch = remember {
        AutoLudoMatchEntity(
            matchId = "preview_match_123",
            player1Uid = "player_1",
            player1Name = "MD DALUAR",
            player2Uid = "player_2",
            player2Name = "MISS AYSHA",
            status = "IN_GAME",
            gameState = LudoGameState(
                currentTurnUid = "player_1",
                diceValue = 0,
                isDiceRolled = false,
                player1Pawns = listOf(-1, -1, -1, -1),
                player2Pawns = listOf(-1, -1, -1, -1)
            )
        )
    }

    var match by remember { mutableStateOf<AutoLudoMatchEntity?>(if (isPreviewMode) mockMatch else null) }
    var showBoard by remember { mutableStateOf(isPreviewMode) } 

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
                android.util.Log.e("AutoLudo", "Firebase Observation Error: ${e.message}")
                if (match == null) match = mockMatch
            }
        } else {
            match = mockMatch
        }
    }

    // Immediate fallback if still null after a short timeout
    LaunchedEffect(Unit) {
        if (!isPreviewMode) {
            delay(1500)
            if (match == null) {
                match = mockMatch
                android.util.Log.d("AutoLudo", "Falling back to mock match (Offline/Loading)")
            }
        }
    }

    if (match == null && !isPreviewMode) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFBF953F))
        }
        return
    }

    val currentMatch = match ?: mockMatch
    val isActuallyLocal = isPreviewMode || currentMatch.matchId == "preview_match_123"
    
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
        }
    }

    // Sync local dice state with match state in live mode
    LaunchedEffect(currentMatch.gameState.diceValue, currentMatch.gameState.isDiceRolled) {
        if (!isActuallyLocal) {
            // CRITICAL FIX: Only clobber local state if Firebase has a TRUE roll OR if we aren't locally rolled.
            // This prevents the "Dice Button Not Disabling" glitch where Firebase resets localIsDiceRolled to false before it updates.
            if (currentMatch.gameState.isDiceRolled) {
                localDiceValue = currentMatch.gameState.diceValue
                localIsDiceRolled = true
            } else if (!localIsDiceRolled) {
                // If both are false, sync is fine
                localDiceValue = 0
                localIsDiceRolled = false
            }
        }
    }

    if (!showBoard) {
        AutoLudoGameSplashScreen(onFinished = { showBoard = true })
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
                if (newPos in 0..51) {
                    val isSafe = AutoLudoManager.isSafeCell(newPos, isP1)
                    if (!isSafe) {
                        val myGlobal = AutoLudoManager.getGlobalTrackPos(newPos, isP1)
                        val oppPawns = if (isP1) boardGameState.player2Pawns else boardGameState.player1Pawns
                        oppPawns.forEach { oppPos ->
                            if (oppPos in 0..51) {
                                val oppGlobal = AutoLudoManager.getGlobalTrackPos(oppPos, !isP1)
                                if (myGlobal == oppGlobal) {
                                    localCapture = true
                                }
                            }
                        }
                    }
                }

                val isBonusTurn = rolledDice == 6 || newPos == 57 || localCapture

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
                                if (oppPos in 0..51 && AutoLudoManager.getGlobalTrackPos(oppPos, false) == myGlobal) {
                                    p2Pawns[idx] = -1
                                }
                            }
                        }
                    } else {
                        p2Pawns[pawnIndex] = newPos
                        if (localCapture) {
                            val myGlobal = AutoLudoManager.getGlobalTrackPos(newPos, false)
                            p1Pawns.forEachIndexed { idx, oppPos ->
                                if (oppPos in 0..51 && AutoLudoManager.getGlobalTrackPos(oppPos, true) == myGlobal) {
                                    p1Pawns[idx] = -1
                                }
                            }
                        }
                    }
                    
                    val allP1Goal = p1Pawns.all { it == 57 }
                    val allP2Goal = p2Pawns.all { it == 57 }
                    val isDisqualified = p1Strikes >= 5 || p2Strikes >= 5
                    val isCompleted = allP1Goal || allP2Goal || isDisqualified
                    val winner = when {
                        allP1Goal -> activeMatch.player1Uid.ifBlank { "player_1" }
                        allP2Goal -> activeMatch.player2Uid.ifBlank { "player_2" }
                        p1Strikes >= 5 -> activeMatch.player2Uid.ifBlank { "player_2" }
                        p2Strikes >= 5 -> activeMatch.player1Uid.ifBlank { "player_1" }
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
                                isDisqualified -> "Player Disqualified (5 Misses)!"
                                localCapture -> "Captured! Bonus Roll"
                                newPos == 57 -> "Goal! Bonus Roll"
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
                    val isDisqualified = p1Strikes >= 5 || p2Strikes >= 5
                    val winner = when {
                        p1Strikes >= 5 -> currentMatch.player2Uid.ifBlank { "player_2" }
                        p2Strikes >= 5 -> currentMatch.player1Uid.ifBlank { "player_1" }
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
                            lastActionLog = if (isDisqualified) "Disqualified (5 Misses)!" else if (isTimeout) "Auto-Pass! Strike +1" else "No Moves! Turn Passed"
                        )
                    )
                } else {
                    coroutineScope.launch {
                        AutoLudoManager.passTurn(matchId, currentUid, isTimeout)
                    }
                }
            },
            onBackClick = onBackClick
        )
        
        // Handle back press to exit game
        androidx.activity.compose.BackHandler {
            onBackClick()
        }

        // Winner Dialog
        if (currentMatch.status == "COMPLETED") {
            AlertDialog(
                onDismissRequest = { onBackClick() },
                title = { Text("গেম সমাপ্ত!", fontWeight = FontWeight.Bold) },
                text = {
                    val winnerUid = currentMatch.gameState.winnerUid
                    val winnerName = if (winnerUid == currentMatch.player1Uid) currentMatch.player1Name else if (winnerUid == currentMatch.player2Uid) currentMatch.player2Name else "বিজয়ী"
                    Text("অভিনন্দন! $winnerName গেমটি জিতেছেন।")
                },
                confirmButton = {
                    Button(onClick = { onBackClick() }) {
                        Text("ঠিক আছে")
                    }
                },
                containerColor = Color(0xFF1565C0),
                titleContentColor = Color(0xFFFFD700),
                textContentColor = Color.White
            )
        }
    }
}

@Composable
fun AutoLudoGameSplashScreen(onFinished: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 3000, easing = LinearEasing),
        label = "SplashProgress",
        finishedListener = { if (it >= 1f) onFinished() }
    )

    LaunchedEffect(Unit) {
        progress = 1f
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.splash_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "অপেক্ষা করুন গেম লোড হচ্ছে...",
                color = Color(0xFFFFDF7A),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Luxury Golden Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(1.dp, Color(0xFFBF953F), RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFBF953F), Color(0xFFFCF6BA), Color(0xFFB38728))
                            )
                        )
                )
            }
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
    val isPreviewMode = LocalInspectionMode.current || match.matchId.contains("preview") || match.matchId.contains("mock")
    val gameState = match.gameState
    
    // Use the values already injected into the gameState by the parent
    val effectiveDiceValue = gameState.diceValue
    val isRolled = gameState.isDiceRolled
    
    // Timer Logic
    var timerProgress by remember { mutableStateOf(0f) }
    val totalTime = if (isRolled) 20000L else 15000L
    
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

    // Interactive permission: allow control if it's our turn or if we are in local session
    val canControlPlayer1 = isPlayer1Turn && (
        isPreviewMode || 
        currentUid == "player_1" || 
        match.player1Uid == currentUid ||
        match.player1Uid == "player_1" ||
        match.player1Uid.isBlank()
    )

    val canControlPlayer2 = isPlayer2Turn && (
        isPreviewMode || 
        currentUid == "player_2" || 
        match.player2Uid == currentUid ||
        match.player2Uid == "player_2" ||
        match.player2Uid.isBlank()
    )

    LaunchedEffect(gameState.currentTurnUid, isRolled, match.status) {
        if (match.status == "COMPLETED") return@LaunchedEffect
        
        // Monotonic local anchor to prevent runaway loops due to server clock drift
        val localStartTime = System.currentTimeMillis()
        var actionDispatched = false
        
        while (!actionDispatched) {
            val now = System.currentTimeMillis()
            val elapsed = now - localStartTime
            timerProgress = (elapsed.toFloat() / totalTime).coerceIn(0f, 1f)
            
            if (timerProgress >= 1f) {
                val isAuthoritativeTimeout = elapsed > (totalTime + 1500L)
                val isCurrentPlayerMovable = if (isPlayer1Turn) canControlPlayer1 else canControlPlayer2
                
                if (isCurrentPlayerMovable || isAuthoritativeTimeout) {
                    actionDispatched = true
                    if (!isRolled) {
                        onDiceRoll((1..6).random(), true)
                    } else {
                        // Find first eligible pawn
                        val pawns = if (isPlayer1Turn) gameState.player1Pawns else gameState.player2Pawns
                        val eligibleIndex = pawns.indexOfFirst { p -> 
                            (p == -1 && effectiveDiceValue == 6) || (p >= 0 && p + effectiveDiceValue <= 57) 
                        }
                        if (eligibleIndex != -1) {
                            val p = pawns[eligibleIndex]
                            val newPos = if (p == -1) 0 else p + effectiveDiceValue
                            onMovePawn(eligibleIndex, newPos, true)
                        } else {
                            onPassTurn(true)
                        }
                    }
                    // Mandatory lock/debounce to prevent rapid consecutive timeouts
                    delay(1500L)
                }
            }
            delay(100)
        }
    }
    
    // Auto-pass turn if rolled 1-5 and no legal moves exist (Roll 6 Guard applied)
    LaunchedEffect(isRolled, effectiveDiceValue, isPlayer1Turn, isPlayer2Turn) {
        if (isRolled && effectiveDiceValue in 1..5) {
            val isCurrentPlayerMovable = if (isPlayer1Turn) canControlPlayer1 else canControlPlayer2
            
            // Only auto-pass if the local user HAS control but NO moves
            if (isCurrentPlayerMovable) {
                val pawns = if (isPlayer1Turn) gameState.player1Pawns else gameState.player2Pawns
                val hasLegalMove = pawns.any { p -> 
                    (p == -1 && effectiveDiceValue == 6) || (p >= 0 && p + effectiveDiceValue <= 57) 
                }
                
                if (!hasLegalMove) {
                    delay(1200L) // Wait for UI/Animations
                    onPassTurn(false)
                }
            }
        }
    }
    
    val player1 = LudoPlayer(
        userId = match.player1Uid.ifBlank { "player_1" },
        name = match.player1Name.ifBlank { "Player 1" },
        avatarUrl = match.player1Avatar,
        isCurrentTurn = isPlayer1Turn,
        pawnColor = 0xFFC62828,
        strikes = gameState.player1Strikes
    )
    val player2 = LudoPlayer(
        userId = match.player2Uid.ifBlank { "player_2" },
        name = match.player2Name.ifBlank { "Player 2" },
        avatarUrl = match.player2Avatar,
        isCurrentTurn = isPlayer2Turn,
        pawnColor = 0xFFFFA000,
        strikes = gameState.player2Strikes
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        Image(
            painter = painterResource(id = R.drawable.game_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

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
            if (gameState.lastActionLog.isNotBlank()) {
                Text(
                    text = gameState.lastActionLog,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Spacer(modifier = Modifier.height(30.dp)) // Placeholder to avoid jump
            }
            
            // 2. Centered Ludo Board Container (Flex-weighted to guarantee fit)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .aspectRatio(1f)
                        .shadow(12.dp, RoundedCornerShape(8.dp))
                        .border(2.dp, Color(0xFFBF953F), RoundedCornerShape(8.dp))
                        .background(Color.White)
                ) {
                    val boardSizePx = constraints.maxWidth.toFloat()
                    val unit = boardSizePx / 15f

                    Image(
                        painter = painterResource(id = R.drawable.ludo_board),
                        contentDescription = "Ludo Board",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Render pawns as individual composables for better scaling, performance and ANIMATION
                    val density = LocalDensity.current

                    // Red Pawn Colors (Deep Royal Ruby - Saturated)
                    val redPrimary = Color(0xFF9B0000)
                    val redDeep = Color(0xFF4A0000)
                    val redOutline = Color(0xFF2E0000)

                    // Yellow Pawn Colors (Rich Golden Amber - Saturated)
                    val yellowPrimary = Color(0xFFFF9100)
                    val yellowDeep = Color(0xFF9E5B00)
                    val yellowOutline = Color(0xFF4E2D00)

                    gameState.player1Pawns.forEachIndexed { index, pos ->
                        val isEligible = canControlPlayer1 && isRolled && (
                            (pos == -1 && effectiveDiceValue == 6) ||
                            (pos >= 0 && pos + effectiveDiceValue <= 57)
                        )
                        
                        LudoPawnView(
                            targetPos = pos,
                            index = index,
                            isPlayer1 = true,
                            boardSizePx = boardSizePx,
                            pawnColor = redPrimary,
                            deepShadeColor = redDeep,
                            outlineColor = redOutline,
                            isEligibleToMove = isEligible,
                            onClick = {
                                val dice = effectiveDiceValue
                                var newPos = pos
                                if (pos == -1 && dice == 6) {
                                    newPos = 0 // Unlock onto starting cell (Standard Ludo King)
                                } else if (pos >= 0) {
                                    newPos = pos + dice
                                    if (newPos > 57) return@LudoPawnView
                                }
                                if (newPos != pos) {
                                    onMovePawn(index, newPos, false)
                                }
                            }
                        )
                    }

                    gameState.player2Pawns.forEachIndexed { index, pos ->
                        val isEligible = canControlPlayer2 && isRolled && (
                            (pos == -1 && effectiveDiceValue == 6) ||
                            (pos >= 0 && pos + effectiveDiceValue <= 57)
                        )
                        
                        LudoPawnView(
                            targetPos = pos,
                            index = index,
                            isPlayer1 = false,
                            boardSizePx = boardSizePx,
                            pawnColor = yellowPrimary,
                            deepShadeColor = yellowDeep,
                            outlineColor = yellowOutline,
                            isEligibleToMove = isEligible,
                            onClick = {
                                val dice = effectiveDiceValue
                                var newPos = pos
                                if (pos == -1 && dice == 6) {
                                    newPos = 0 // Unlock onto starting cell (Standard Ludo King)
                                } else if (pos >= 0) {
                                    newPos = pos + dice
                                    if (newPos > 57) return@LudoPawnView
                                }
                                if (newPos != pos) {
                                    onMovePawn(index, newPos, false)
                                }
                            }
                        )
                    }
                }
            }

            // 3. Bottom Battle Dock (Always visible)
            LudoBottomBattleDock(
                player1 = player1,
                player2 = player2,
                currentDiceValue = effectiveDiceValue,
                isDiceRolled = isRolled,
                timerProgress = timerProgress,
                onDiceRoll = { onDiceRoll(it, false) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .zIndex(20f)
            )
        }
    }
}

@Composable
fun PlayerAvatarCard(player: LudoPlayer, isLeft: Boolean, timerProgress: Float = 0f) {
    val glowColor = if (isLeft) Color(0xFF00E5FF) else Color(0xFF00C853) // Cyan for P1, Emerald for P2
    val borderColor = if (player.isCurrentTurn) glowColor else Color.White.copy(alpha = 0.4f)
    val borderStroke = if (player.isCurrentTurn) 3.5.dp else 1.5.dp
    
    Box(contentAlignment = Alignment.Center) {
        // Authoritative Cooldown Ring (Only for active turn)
        if (player.isCurrentTurn) {
            Canvas(modifier = Modifier.size(64.dp)) {
                drawArc(
                    color = glowColor.copy(alpha = 0.3f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = glowColor,
                    startAngle = -90f,
                    sweepAngle = 360f * (1f - timerProgress),
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Elevated Square Avatar Box with Vibrant Frame
        Surface(
            modifier = Modifier
                .size(54.dp)
                .shadow(
                    elevation = if (player.isCurrentTurn) 12.dp else 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = glowColor,
                    spotColor = glowColor
                ),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.15f), // Translucent Frame
            border = BorderStroke(borderStroke, borderColor)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // Placeholder Icon (Royal Blue Background)
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A237E))) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).align(Alignment.Center),
                        tint = Color(0xFFFFD54F) // Bright Gold Icon
                    )
                }

                AsyncImage(
                    model = player.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Neon Glow Overlay
                if (player.isCurrentTurn) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                BorderStroke(2.dp, Brush.verticalGradient(listOf(glowColor.copy(alpha = 0.8f), Color.Transparent))),
                                RoundedCornerShape(12.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun LudoTopBar(onBackClick: () -> Unit) {
    var isMuted by remember { mutableStateOf(false) }
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
            onClick = { isMuted = !isMuted },
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
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    
    // Maintain visual position state for step-by-step hopping
    var visualPos by remember { mutableStateOf(targetPos) }
    val animOffset = remember { Animatable(getPawnOffset(targetPos, index, boardSizePx, isPlayer1), Offset.VectorConverter) }
    
    // Jump height animation for parabolic effect
    val jumpAnim = remember { Animatable(0f) }

    LaunchedEffect(targetPos, boardSizePx) {
        if (boardSizePx <= 0f) return@LaunchedEffect
        
        if (targetPos == visualPos) {
             animOffset.snapTo(getPawnOffset(targetPos, index, boardSizePx, isPlayer1))
             return@LaunchedEffect
        }

        // Logic for movement: step-by-step hopping
        if (visualPos == -1 && targetPos >= 0) {
            // From yard to start: one big jump
            val startOffset = getPawnOffset(0, index, boardSizePx, isPlayer1)
            launch {
                jumpAnim.animateTo(-24f, tween(180, easing = FastOutLinearInEasing))
                jumpAnim.animateTo(0f, tween(180, easing = LinearOutSlowInEasing))
            }
            animOffset.animateTo(startOffset, tween(360))
            visualPos = 0
        }
        
        if (targetPos > visualPos && visualPos >= 0) {
            // Step-by-step hop through the track
            for (step in (visualPos + 1)..targetPos) {
                val stepOffset = getPawnOffset(step, index, boardSizePx, isPlayer1)
                launch {
                    jumpAnim.animateTo(-18f, tween(150, easing = FastOutLinearInEasing))
                    jumpAnim.animateTo(0f, tween(150, easing = LinearOutSlowInEasing))
                }
                animOffset.animateTo(stepOffset, tween(300, easing = LinearEasing))
                visualPos = step
            }
        } else if (targetPos < visualPos) {
            // Captured or teleported back (yard)
            animOffset.animateTo(getPawnOffset(targetPos, index, boardSizePx, isPlayer1), tween(500))
            visualPos = targetPos
        }
    }

    Box(
        modifier = Modifier
            .offset(
                x = with(density) { animOffset.value.x.toDp() } - 22.dp,
                y = with(density) { (animOffset.value.y + jumpAnim.value).toDp() } - 46.dp
            )
            .size(width = 44.dp, height = 56.dp)
            .clickable(
                enabled = isEligibleToMove,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        LudoKingPawnToken(
            pawnColor = pawnColor,
            deepShadeColor = deepShadeColor,
            outlineColor = outlineColor,
            isEligibleToMove = isEligibleToMove
        )
    }
}

@Composable
fun LudoKingPawnToken(
    pawnColor: Color,
    deepShadeColor: Color,
    outlineColor: Color,
    isEligibleToMove: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "halo_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(modifier = modifier.size(width = 32.dp, height = 46.dp), contentAlignment = Alignment.BottomCenter) {
        // 1. Authentic Ludo King Segmented Rotating Ring (ONLY when eligible)
        if (isEligibleToMove) {
            Canvas(modifier = Modifier.size(36.dp).offset(y = 12.dp)) {
                rotate(rotation) {
                    val segmentCount = 12
                    val sweepAngle = 360f / segmentCount
                    for (i in 0 until segmentCount) {
                        val color = if (i % 2 == 0) pawnColor else Color.White
                        drawArc(
                            color = color,
                            startAngle = i * sweepAngle,
                            sweepAngle = sweepAngle * 0.75f,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Butt)
                        )
                    }
                }
                // Thin high-contrast outer ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = 18.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val centerX = w * 0.5f
            val headCenterY = h * 0.32f
            val headRadius = w * 0.38f
            val tipY = h * 0.86f

            // NO STATIC SAUCER CIRCLE OR CONTACT SHADOW AS REQUESTED

            // 2. Exact Standing Map-Pin (Teardrop) Path with Crisp Outline
            val pinPath = Path().apply {
                moveTo(centerX, tipY)
                cubicTo(
                    centerX - headRadius * 0.95f, h * 0.60f,
                    centerX - headRadius, headCenterY + headRadius * 0.45f,
                    centerX - headRadius, headCenterY
                )
                arcTo(
                    rect = Rect(
                        left = centerX - headRadius,
                        top = headCenterY - headRadius,
                        right = centerX + headRadius,
                        bottom = headCenterY + headRadius
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                cubicTo(
                    centerX + headRadius, headCenterY + headRadius * 0.45f,
                    centerX + headRadius * 0.95f, h * 0.60f,
                    centerX, tipY
                )
                close()
            }

            // Draw Subtle Shadow for depth (ONLY a small drop shadow for the pin itself, not a base circle)
            translate(left = 1.5.dp.toPx(), top = 1.5.dp.toPx()) {
                drawPath(
                    path = pinPath,
                    color = Color(0xFF0D47A1).copy(alpha = 0.35f)
                )
            }

            // Fill Pin Body with DEEP 3D team gradient
            drawPath(
                path = pinPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(deepShadeColor, pawnColor, deepShadeColor)
                )
            )

            // High-contrast CRISP Outer Outline (Obsidian/Deep rim)
            drawPath(
                path = pinPath,
                color = outlineColor,
                style = Stroke(width = 1.8.dp.toPx())
            )

            // Metallic light rim inner accent
            drawPath(
                path = pinPath,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)
                ),
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. Iconic Bold White Core Circle inside Head
            val whiteCoreRadius = headRadius * 0.52f
            drawCircle(
                color = Color.White,
                center = Offset(centerX, headCenterY),
                radius = whiteCoreRadius
            )

            // Specular highlight on the white core
            drawCircle(
                color = Color.White,
                center = Offset(centerX - whiteCoreRadius * 0.25f, headCenterY - whiteCoreRadius * 0.25f),
                radius = whiteCoreRadius * 0.35f
            )
        }
    }
}

@Composable
fun LudoBottomBattleDock(
    player1: LudoPlayer,
    player2: LudoPlayer,
    currentDiceValue: Int,
    isDiceRolled: Boolean,
    timerProgress: Float = 0f,
    onDiceRoll: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.wrapContentHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. Floating Avatars, Names & Background Chassis
        Box(
            modifier = Modifier.fillMaxWidth().height(120.dp), // Increased height for floating names
            contentAlignment = Alignment.BottomCenter
        ) {
            // Vibrant Royal Blue Bottom Dock Bar (Now cleaner)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                color = Color.Transparent,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                border = BorderStroke(
                    2.5.dp,
                    Brush.verticalGradient(listOf(Color(0xFFFFE082), Color(0xFFFFD700)))
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF1565C0), Color(0xFF0D47A1))),
                            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        )
                )
            }

            // Player 1 Area (Name + Avatar)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MiniPawnIcon(color = Color(0xFF9B0000))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        player1.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = Shadow(
                                color = Color(0xFF0D47A1).copy(alpha = 0.7f),
                                offset = Offset(2f, 2f),
                                blurRadius = 4f
                            )
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                
                // Strike Life Dots
                StrikeDots(strikes = player1.strikes)
                Spacer(modifier = Modifier.height(4.dp))
                
                PlayerAvatarCard(player = player1, isLeft = true, timerProgress = if (player1.isCurrentTurn) timerProgress else 0f)
            }

            // Player 2 Area (Name + Avatar)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        player2.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = Shadow(
                                color = Color(0xFF0D47A1).copy(alpha = 0.7f),
                                offset = Offset(2f, 2f),
                                blurRadius = 4f
                            )
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    MiniPawnIcon(color = Color(0xFFFF9100))
                }
                Spacer(modifier = Modifier.height(6.dp))
                
                // Strike Life Dots
                StrikeDots(strikes = player2.strikes)
                Spacer(modifier = Modifier.height(4.dp))
                
                PlayerAvatarCard(player = player2, isLeft = false, timerProgress = if (player2.isCurrentTurn) timerProgress else 0f)
            }
        }

        // 2. Central Elevated Pearlescent Dice Tray (Authentic Ludo King)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-18).dp)
                .size(72.dp)
                .shadow(8.dp, RoundedCornerShape(14.dp))
                .background(Color(0xFFFFF8E7), RoundedCornerShape(14.dp))
                .border(2.dp, Color(0xFFFFCA28), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            val isPlayer1Turn = player1.isCurrentTurn
            val isPreviewMode = LocalInspectionMode.current || player1.userId.contains("player") || player2.userId.contains("player")
            val activeDiceColor = if (isPlayer1Turn) Color(0xFFEF4444) else Color(0xFFF59E0B)
            
            Luxury3DCubeDice(
                diceValue = currentDiceValue,
                isEnabled = (player1.isCurrentTurn || player2.isCurrentTurn) && !isDiceRolled,
                isPlayer1Turn = isPlayer1Turn,
                diceColor = activeDiceColor,
                onRoll = onDiceRoll
            )
        }
    }
}

@Composable
fun StrikeDots(strikes: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(5) { i ->
            val isStruck = i < strikes
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isStruck) Color(0xFFEF4444) else Color(0xFF00E5FF))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )
        }
    }
}

@Composable
fun MiniPawnIcon(color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.9f)
            cubicTo(w * 0.1f, h * 0.6f, w * 0.1f, h * 0.1f, w * 0.5f, h * 0.1f)
            cubicTo(w * 0.9f, h * 0.1f, w * 0.9f, h * 0.6f, w * 0.5f, h * 0.9f)
            close()
        }
        drawPath(path = path, color = color)
        drawCircle(color = Color.White, radius = w * 0.2f, center = Offset(w * 0.5f, h * 0.35f))
    }
}

// Helper to calculate a single pawn offset
private fun getPawnOffset(pos: Int, index: Int, boardSize: Float, isPlayer1: Boolean): Offset {
    if (pos == -1) {
        // Home positions
        return if (isPlayer1) {
            when (index) {
                0 -> Offset(boardSize * 0.133f, boardSize * 0.733f)
                1 -> Offset(boardSize * 0.266f, boardSize * 0.733f)
                2 -> Offset(boardSize * 0.133f, boardSize * 0.866f)
                else -> Offset(boardSize * 0.266f, boardSize * 0.866f)
            }
        } else {
            when (index) {
                0 -> Offset(boardSize * 0.733f, boardSize * 0.133f)
                1 -> Offset(boardSize * 0.866f, boardSize * 0.133f)
                2 -> Offset(boardSize * 0.733f, boardSize * 0.266f)
                else -> Offset(boardSize * 0.866f, boardSize * 0.266f)
            }
        }
    } else {
        // Real path mapping logic
        val unit = boardSize / 15f
        val gridPos = getGridPositionForPath(pos, isPlayer1)
        return Offset(gridPos.x * unit + unit / 2f, gridPos.y * unit + unit / 2f)
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
        val stretchIdx = pos - 51
        return if (isPlayer1) {
            // Red home stretch (Row 7, Left to Right)
            GridPos(1 + stretchIdx, 7)
        } else {
            // Yellow home stretch (Row 7, Right to Left)
            GridPos(13 - stretchIdx, 7)
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
    onRoll: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val localDensity = LocalDensity.current
    var isRolling by remember { mutableStateOf(false) }
    var displayValue by remember { mutableStateOf(diceValue) }
    
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

    LaunchedEffect(diceValue) {
        if (!isRolling) displayValue = diceValue
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
                    rotationX = rotX.value
                    rotationY = rotY.value
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
                        isRolling = true
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
                                rotX.animateTo(rotX.value + 720f, tween(500, easing = LinearEasing))
                            }
                            launch { rotY.animateTo(rotY.value + 1080f, tween(500, easing = LinearEasing)) }
                            launch { rotZ.animateTo(rotZ.value + 180f, tween(500, easing = LinearEasing)) }

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
                            val finalValue = (1..6).random()
                            displayValue = finalValue
                            
                            // Snap tilt to neutral
                            launch { rotX.animateTo(0f, spring()) }
                            launch { rotY.animateTo(0f, spring()) }
                            launch { rotZ.animateTo(0f, spring()) }
                            
                            delay(200)
                            onRoll(finalValue)
                            isRolling = false
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            LudoVibrantDiceFace(
                value = displayValue, 
                isPlayer1 = isPlayer1Turn,
                diceColor = diceColor,
                modifier = Modifier.size(54.dp)
            )
        }
    }
}

@Composable
fun LudoVibrantDiceFace(
    value: Int, 
    isPlayer1: Boolean,
    diceColor: Color = if (isPlayer1) Color(0xFFEF4444) else Color(0xFFF59E0B),
    modifier: Modifier = Modifier
) {
    // Shading palette derived from base color
    val sideColor = diceColor.copy(alpha = 1f).darken(0.35f)
    val topHighlight = diceColor.copy(alpha = 1f).lighten(0.25f)
    val bevelColor = Color.White.copy(alpha = 0.5f)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val depth = 12.dp.toPx() // Volumetric depth thickness
            val cornerRadius = 10.dp.toPx()

            // 1. Right/Side Facet (Depth Extrusion)
            val sidePath = Path().apply {
                moveTo(w, cornerRadius)
                lineTo(w + depth * 0.5f, cornerRadius - depth * 0.3f)
                lineTo(w + depth * 0.5f, h - cornerRadius - depth * 0.3f)
                lineTo(w, h - cornerRadius)
                close()
            }
            drawPath(sidePath, sideColor)

            // 2. Top Facet (Perspective Quad)
            val topPath = Path().apply {
                moveTo(cornerRadius, 0f)
                lineTo(cornerRadius + depth * 0.5f, -depth * 0.3f)
                lineTo(w - cornerRadius + depth * 0.5f, -depth * 0.3f)
                lineTo(w - cornerRadius, 0f)
                close()
            }
            drawPath(topPath, topHighlight)

            // 3. Main Front Face (Rounded Core)
            val gradientRadius = (w * 0.8f).coerceAtLeast(1f)
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(diceColor.lighten(0.1f), diceColor),
                    center = Offset(w * 0.3f, h * 0.3f),
                    radius = gradientRadius
                ),
                size = Size(w, h),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )

            // 4. Luxury Chamfered Bevel Lines
            drawRoundRect(
                color = bevelColor,
                size = Size(w, h),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 1.5.dp.toPx())
            )
            
            // Specular Glint Across Face
            drawPath(
                path = Path().apply {
                    moveTo(0f, h * 0.2f)
                    lineTo(w * 0.8f, 0f)
                    lineTo(w, 0f)
                    lineTo(0f, h * 0.4f)
                    close()
                },
                color = Color.White.copy(alpha = 0.15f)
            )
        }

        // 5. Carved Pips with Radial Depth
        VibrantDicePips(value = value, diceColor = diceColor)
    }
}

@Composable
fun VibrantDicePips(value: Int, diceColor: Color) {
    Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
        when (value) {
            1 -> CarvedPip(Alignment.Center, diceColor)
            2 -> {
                CarvedPip(Alignment.TopEnd, diceColor)
                CarvedPip(Alignment.BottomStart, diceColor)
            }
            3 -> {
                CarvedPip(Alignment.TopEnd, diceColor)
                CarvedPip(Alignment.Center, diceColor)
                CarvedPip(Alignment.BottomStart, diceColor)
            }
            4 -> {
                CarvedPip(Alignment.TopStart, diceColor)
                CarvedPip(Alignment.TopEnd, diceColor)
                CarvedPip(Alignment.BottomStart, diceColor)
                CarvedPip(Alignment.BottomEnd, diceColor)
            }
            5 -> {
                CarvedPip(Alignment.TopStart, diceColor)
                CarvedPip(Alignment.TopEnd, diceColor)
                CarvedPip(Alignment.Center, diceColor)
                CarvedPip(Alignment.BottomStart, diceColor)
                CarvedPip(Alignment.BottomEnd, diceColor)
            }
            6 -> {
                CarvedPip(Alignment.TopStart, diceColor)
                CarvedPip(Alignment.TopEnd, diceColor)
                CarvedPip(Alignment.CenterStart, diceColor)
                CarvedPip(Alignment.CenterEnd, diceColor)
                CarvedPip(Alignment.BottomStart, diceColor)
                CarvedPip(Alignment.BottomEnd, diceColor)
            }
        }
    }
}

@Composable
fun BoxScope.CarvedPip(alignment: Alignment, diceColor: Color) {
    val pipDepthColor = diceColor.darken(0.4f)
    Box(
        modifier = Modifier
            .align(alignment)
            .size(10.dp)
            .shadow(1.dp, CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFE0E0E0), pipDepthColor.copy(alpha = 0.4f)),
                    center = Offset.Zero,
                    radius = 30f
                ),
                CircleShape
            )
            .border(0.5.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
    )
}

// Utility Extensions for Shading
fun Color.darken(factor: Float): Color = Color(
    red = (red * (1f - factor)).coerceIn(0f, 1f),
    green = (green * (1f - factor)).coerceIn(0f, 1f),
    blue = (blue * (1f - factor)).coerceIn(0f, 1f),
    alpha = alpha
)

fun Color.lighten(factor: Float): Color = Color(
    red = (red + (1f - red) * factor).coerceIn(0f, 1f),
    green = (green + (1f - green) * factor).coerceIn(0f, 1f),
    blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
    alpha = alpha
)
