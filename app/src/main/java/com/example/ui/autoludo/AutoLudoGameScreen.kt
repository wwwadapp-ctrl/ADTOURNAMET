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
    val pawnColor: Long = 0xFFE53935
)

@Composable
fun AutoLudoGameScreen(
    matchId: String,
    onBackClick: () -> Unit
) {
    val isPreviewMode = LocalInspectionMode.current || matchId.contains("preview")
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
    var showBoard by remember { mutableStateOf(isPreviewMode) } // Skip splash in preview mode

    // Observe real-time match state from Firebase with safety
    LaunchedEffect(matchId) {
        if (!isPreviewMode) {
            try {
                AutoLudoManager.observeMatch(matchId).collect {
                    if (it != null) match = it
                }
            } catch (e: Exception) {
                android.util.Log.e("AutoLudo", "Firebase Observation Error: ${e.message}")
                if (match == null) match = mockMatch
            }
        } else {
            match = mockMatch
        }
    }

    // Immediate fallback if still null after a short timeout to prevent unfreeze
    LaunchedEffect(Unit) {
        if (!isPreviewMode) {
            delay(1000)
            if (match == null) match = mockMatch
        }
    }

    if (match == null && !isPreviewMode) {
        // Simple loading or error state if match not found
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFBF953F))
        }
        return
    }

    val currentMatch = match ?: mockMatch
    val currentUid = remember { 
        try { 
            if (!isPreviewMode) com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "player_1"
            else "player_1"
        } catch (_: Exception) { "player_1" }
    }

    // Local Preview Game Engine State
    var localDiceValue by remember { mutableStateOf(if (isPreviewMode) 0 else currentMatch.gameState.diceValue) }
    var localIsDiceRolled by remember { mutableStateOf(if (isPreviewMode) false else currentMatch.gameState.isDiceRolled) }
    var previewTurnUid by remember { mutableStateOf("player_1") }
    
    // Synchronize local state with match state in non-preview mode
    LaunchedEffect(currentMatch.gameState.diceValue, currentMatch.gameState.isDiceRolled) {
        if (!isPreviewMode) {
            localDiceValue = currentMatch.gameState.diceValue
            localIsDiceRolled = currentMatch.gameState.isDiceRolled
        }
    }

    if (!showBoard) {
        AutoLudoGameSplashScreen(onFinished = { showBoard = true })
    } else {
        AutoLudoBoardScreen(
            match = if (isPreviewMode) {
                currentMatch.copy(
                    gameState = currentMatch.gameState.copy(
                        currentTurnUid = previewTurnUid,
                        diceValue = localDiceValue,
                        isDiceRolled = localIsDiceRolled
                    )
                )
            } else currentMatch,
            currentUid = currentUid,
            localDiceValue = localDiceValue,
            localIsDiceRolled = localIsDiceRolled,
            onDiceRoll = { value ->
                if (isPreviewMode) {
                    localDiceValue = value
                    localIsDiceRolled = true
                } else {
                    coroutineScope.launch {
                        AutoLudoManager.rollDice(matchId, currentUid, value)
                    }
                }
            },
            onMovePawn = { pawnIndex, newPos ->
                if (isPreviewMode) {
                    // Update mock state locally for visual feedback
                    val p1Pawns = currentMatch.gameState.player1Pawns.toMutableList()
                    val p2Pawns = currentMatch.gameState.player2Pawns.toMutableList()
                    if (previewTurnUid == "player_1") p1Pawns[pawnIndex] = newPos
                    else p2Pawns[pawnIndex] = newPos
                    
                    match = currentMatch.copy(
                        gameState = currentMatch.gameState.copy(
                            player1Pawns = p1Pawns,
                            player2Pawns = p2Pawns,
                            isDiceRolled = false,
                            currentTurnUid = if (localDiceValue == 6 || newPos == 57) previewTurnUid else (if (previewTurnUid == "player_1") "player_2" else "player_1")
                        )
                    )
                    localIsDiceRolled = false
                    localDiceValue = 0
                    if (!(match?.gameState?.diceValue == 6 || newPos == 57)) {
                        previewTurnUid = if (previewTurnUid == "player_1") "player_2" else "player_1" // Toggle turn if no bonus
                    }
                } else {
                    coroutineScope.launch {
                        AutoLudoManager.movePawn(matchId, currentUid, pawnIndex, newPos)
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
    onDiceRoll: (Int) -> Unit,
    onMovePawn: (Int, Int) -> Unit,
    onBackClick: () -> Unit
) {
    val isPreviewMode = LocalInspectionMode.current || match.matchId.contains("preview")
    val gameState = match.gameState
    // Force turn in preview to allow interaction
    val isMyTurn = if (isPreviewMode) true else gameState.currentTurnUid == currentUid
    val diceRolled = if (isPreviewMode) localIsDiceRolled else gameState.isDiceRolled
    val diceVal = if (isPreviewMode) localDiceValue else gameState.diceValue
    
    val player1 = LudoPlayer(
        userId = match.player1Uid,
        name = match.player1Name,
        avatarUrl = match.player1Avatar,
        isCurrentTurn = gameState.currentTurnUid == match.player1Uid,
        pawnColor = 0xFFC62828
    )
    val player2 = LudoPlayer(
        userId = match.player2Uid,
        name = match.player2Name,
        avatarUrl = match.player2Avatar,
        isCurrentTurn = gameState.currentTurnUid == match.player2Uid,
        pawnColor = 0xFFFFA000
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
                        val isEligible = isMyTurn && (if (isPreviewMode) gameState.currentTurnUid == "player_1" else match.player1Uid == currentUid) && diceRolled && (
                            (gameState.player1Pawns[index] == -1 && (if (isPreviewMode) localDiceValue == 6 else gameState.diceValue == 6)) ||
                            (gameState.player1Pawns[index] >= 0 && gameState.player1Pawns[index] + (if (isPreviewMode) localDiceValue else gameState.diceValue) <= 57)
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
                                val dice = if (isPreviewMode) localDiceValue else gameState.diceValue
                                var newPos = pos
                                if (pos == -1 && dice == 6) {
                                    newPos = 0
                                } else if (pos >= 0) {
                                    newPos = pos + dice
                                    if (newPos > 57) return@LudoPawnView
                                }
                                if (newPos != pos) {
                                    onMovePawn(index, newPos)
                                }
                            }
                        )
                    }

                    gameState.player2Pawns.forEachIndexed { index, pos ->
                        val isEligible = isMyTurn && (if (isPreviewMode) gameState.currentTurnUid == "player_2" else match.player2Uid == currentUid) && diceRolled && (
                            (gameState.player2Pawns[index] == -1 && (if (isPreviewMode) localDiceValue == 6 else gameState.diceValue == 6)) ||
                            (gameState.player2Pawns[index] >= 0 && gameState.player2Pawns[index] + (if (isPreviewMode) localDiceValue else gameState.diceValue) <= 57)
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
                                val dice = if (isPreviewMode) localDiceValue else gameState.diceValue
                                var newPos = pos
                                if (pos == -1 && dice == 6) {
                                    newPos = 0
                                } else if (pos >= 0) {
                                    newPos = pos + dice
                                    if (newPos > 57) return@LudoPawnView
                                }
                                if (newPos != pos) {
                                    onMovePawn(index, newPos)
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
                currentDiceValue = diceVal,
                isDiceRolled = diceRolled,
                onDiceRoll = onDiceRoll,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .zIndex(20f)
            )
        }
    }
}

@Composable
fun PlayerAvatarCard(player: LudoPlayer, isLeft: Boolean) {
    val glowColor = if (isLeft) Color(0xFF00E5FF) else Color(0xFF00C853) // Cyan for P1, Emerald for P2
    val borderColor = if (player.isCurrentTurn) glowColor else Color.White.copy(alpha = 0.4f)
    val borderStroke = if (player.isCurrentTurn) 3.5.dp else 1.5.dp
    
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

    LudoKingPawnToken(
        pawnColor = pawnColor,
        deepShadeColor = deepShadeColor,
        outlineColor = outlineColor,
        isEligibleToMove = isEligibleToMove,
        modifier = Modifier.offset(
            x = with(density) { animOffset.value.x.toDp() } - 16.dp,
            y = with(density) { (animOffset.value.y + jumpAnim.value).toDp() } - 40.dp
        ).clickable(
            enabled = isEligibleToMove,
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onClick() }
    )
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
                PlayerAvatarCard(player = player1, isLeft = true)
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
                PlayerAvatarCard(player = player2, isLeft = false)
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
            val isPreviewMode = androidx.compose.ui.platform.LocalInspectionMode.current || player1.userId.contains("player") || player2.userId.contains("player")
            Luxury3DCubeDice(
                diceValue = currentDiceValue,
                isEnabled = (if (isPreviewMode) true else player1.isCurrentTurn || player2.isCurrentTurn) && !isDiceRolled,
                isPlayer1Turn = isPlayer1Turn,
                onRoll = onDiceRoll
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
    // This is a simplified version of the path
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
    
    // Rotate path for Yellow (player2) if needed
    // In this simplified logic, let's just use the same path with an offset for Yellow
    val p1StartIdx = 0 // Red starts at (1, 6)
    val p2StartIdx = 26 // Yellow starts at (13, 8) roughly half way
    
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
    onRoll: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val localDensity = LocalDensity.current
    var isRolling by remember { mutableStateOf(false) }
    var displayValue by remember { mutableStateOf(diceValue) }
    
    val jumpY = remember { Animatable(0f) }
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }
    val rotZ = remember { Animatable(0f) }
    val dScaleX = remember { Animatable(1f) }
    val dScaleY = remember { Animatable(1f) }

    LaunchedEffect(diceValue) {
        if (!isRolling) displayValue = diceValue
    }

    Box(
        modifier = Modifier
            .size(64.dp)
            .zIndex(50f)
            .graphicsLayer {
                translationY = jumpY.value
                rotationX = rotX.value
                rotationY = rotY.value
                rotationZ = rotZ.value
                cameraDistance = 16f * this.density
                scaleX = dScaleX.value
                scaleY = dScaleY.value
            }
            .clickable(
                enabled = isEnabled && !isRolling,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isRolling) {
                    isRolling = true
                    coroutineScope.launch {
                        val jumpTarget = -localDensity.run { 48.dp.toPx() }
                        // 1. Jump up and rotate
                        launch {
                            jumpY.animateTo(jumpTarget, tween(180, easing = FastOutSlowInEasing))
                            jumpY.animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium))
                        }
                        
                        launch { rotX.animateTo(rotX.value + 720f, tween(600, easing = LinearEasing)) }
                        launch { rotY.animateTo(rotY.value + 540f, tween(600, easing = LinearEasing)) }
                        launch { rotZ.animateTo(rotZ.value + 360f, tween(600, easing = LinearEasing)) }

                        // 2. Shuffle numbers rapidly
                        val tumbleJob = launch {
                            while (true) {
                                displayValue = (1..6).random()
                                delay(45)
                            }
                        }
                        
                        delay(600)
                        tumbleJob.cancel()
                        
                        // 3. Final settled value
                        val finalValue = (1..6).random()
                        displayValue = finalValue
                        
                        launch { rotX.snapTo(0f) }
                        launch { rotY.snapTo(0f) }
                        launch { rotZ.snapTo(0f) }
                        
                        launch {
                            dScaleX.animateTo(1.15f, tween(100))
                            dScaleX.animateTo(1f, spring(Spring.DampingRatioHighBouncy))
                        }
                        launch {
                            dScaleY.animateTo(0.84f, tween(100))
                            dScaleY.animateTo(1.06f, tween(100))
                            dScaleY.animateTo(1f, spring(Spring.DampingRatioHighBouncy))
                        }
                        
                        delay(200)
                        onRoll(finalValue)
                        isRolling = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        LudoVibrantDiceFace(value = displayValue, isPlayer1 = isPlayer1Turn)
    }
}

@Composable
fun LudoVibrantDiceFace(value: Int, isPlayer1: Boolean) {
    val gradient = if (isPlayer1) {
        Brush.verticalGradient(listOf(Color(0xFFFF1744), Color(0xFFD50000))) // Crimson
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFEA00), Color(0xFFFFB300))) // Sunshine Gold
    }

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(gradient)
            .border(
                BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f)), // Bevel highlight
                RoundedCornerShape(10.dp)
            )
            .padding(6.dp)
    ) {
        VibrantDicePips(value = value)
    }
}

@Composable
fun VibrantDicePips(value: Int) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (value) {
            1 -> VibrantPip(Alignment.Center)
            2 -> {
                VibrantPip(Alignment.TopEnd)
                VibrantPip(Alignment.BottomStart)
            }
            3 -> {
                VibrantPip(Alignment.TopEnd)
                VibrantPip(Alignment.Center)
                VibrantPip(Alignment.BottomStart)
            }
            4 -> {
                VibrantPip(Alignment.TopStart)
                VibrantPip(Alignment.TopEnd)
                VibrantPip(Alignment.BottomStart)
                VibrantPip(Alignment.BottomEnd)
            }
            5 -> {
                VibrantPip(Alignment.TopStart)
                VibrantPip(Alignment.TopEnd)
                VibrantPip(Alignment.Center)
                VibrantPip(Alignment.BottomStart)
                VibrantPip(Alignment.BottomEnd)
            }
            6 -> {
                VibrantPip(Alignment.TopStart)
                VibrantPip(Alignment.TopEnd)
                VibrantPip(Alignment.CenterStart)
                VibrantPip(Alignment.CenterEnd)
                VibrantPip(Alignment.BottomStart)
                VibrantPip(Alignment.BottomEnd)
            }
        }
    }
}

@Composable
fun BoxScope.VibrantPip(alignment: Alignment) {
    Box(
        modifier = Modifier
            .align(alignment)
            .size(9.dp)
            .shadow(1.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .border(0.5.dp, Color(0xFF1565C0).copy(alpha = 0.2f), CircleShape)
    )
}
