package com.example.ui.minigames

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.firebase.FirebaseManager
import com.example.ui.components.TournamentButton
import com.example.ui.wallet.WalletViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.SecureRandom
import kotlin.coroutines.resume

// --- Luxury Palette ---
private val ScratchNavy = Color(0xFF0B0D21)
private val ScratchDeepPurple = Color(0xFF1E1B4B)
private val GoldMetallic = Color(0xFFFFD700)
private val AmberGlow = Color(0xFFFFBF00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScratchCardGameScreen(
    walletViewModel: WalletViewModel,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val walletState by walletViewModel.uiState.collectAsState()
    val mainBalance = walletState.mainBalance

    val uid = remember { FirebaseManager.getAuth()?.currentUser?.uid ?: "" }
    var gameWalletBalance by remember { mutableDoubleStateOf(0.0) }
    var reservePool by remember { mutableDoubleStateOf(0.0) }
    var userName by remember { mutableStateOf("") }
    var userPhone by remember { mutableStateOf("") }
    var showTransferModal by remember { mutableStateOf(false) }

    // Observers (Synchronized with Dice and Wheel mini-games)
    DisposableEffect(uid) {
        if (uid.isBlank()) return@DisposableEffect onDispose {}
        val database = FirebaseManager.getDatabase()
        
        val balanceRef = database?.getReference("gameWallets")?.child(uid)?.child("balance")
        val balanceListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                gameWalletBalance = snapshot.value?.toString()?.toDoubleOrNull() ?: 0.0
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        balanceRef?.addValueEventListener(balanceListener)

        val poolRef = database?.getReference("miniGames/vault/reservePool")
        val poolListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                reservePool = snapshot.value?.toString()?.toDoubleOrNull() ?: 0.0
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        poolRef?.addValueEventListener(poolListener)

        val userRef = database?.getReference("users")?.child(uid)
        val userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userName = snapshot.child("name").getValue(String::class.java)
                    ?: snapshot.child("fullName").getValue(String::class.java) ?: "User"
                userPhone = snapshot.child("mobileNumber").getValue(String::class.java)
                    ?: snapshot.child("phoneNumber").getValue(String::class.java) ?: ""
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        userRef?.addValueEventListener(userListener)

        onDispose {
            balanceRef?.removeEventListener(balanceListener)
            poolRef?.removeEventListener(poolListener)
            userRef?.removeEventListener(userListener)
        }
    }

    // Game States
    var selectedBetAmount by remember { mutableLongStateOf(1L) }
    var isTicketPurchased by remember { mutableStateOf(false) }
    var isRevealed by remember { mutableStateOf(false) }
    var resultMultiplier by remember { mutableIntStateOf(0) }
    var winAmount by remember { mutableDoubleStateOf(0.0) }
    var showWinDialog by remember { mutableStateOf(false) }

    // Scratch State (80 logical cells = 10 cols x 8 rows)
    val totalCells = 80
    val revealedCells = remember { BooleanArray(totalCells) }
    var revealedCellCount by remember { mutableIntStateOf(0) }
    var dragPointCount by remember { mutableIntStateOf(0) }
    val scratchPath = remember { mutableStateOf(Path()) }

    // Secret Layer Icons
    val icons = remember(isTicketPurchased, resultMultiplier) {
        if (resultMultiplier > 0) {
            when (resultMultiplier) {
                5 -> listOf(Icons.Default.Star, Icons.Default.Star, Icons.Default.Star)
                2 -> listOf(Icons.Default.Favorite, Icons.Default.Favorite, Icons.Default.Favorite)
                else -> listOf(Icons.Default.Face, Icons.Default.Face, Icons.Default.Face)
            }
        } else {
            listOf(Icons.Default.Star, Icons.Default.Close, Icons.Default.Favorite)
        }
    }

    // Instant Result Trigger (UI Only - Financials handled by Engine in buyTicket)
    fun revealCard() {
        if (!isTicketPurchased || isRevealed) return
        isRevealed = true

        scope.launch {
            delay(400)
            showWinDialog = true
        }
    }

    // Buy New Ticket (Authoritative execution via MiniGameCoreEngine)
    fun buyTicket() {
        if (isTicketPurchased && !isRevealed) return
        if (gameWalletBalance < selectedBetAmount) {
            Toast.makeText(context, "গেম ব্যালেন্স অপর্যাপ্ত", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            val betAmount = selectedBetAmount.toDouble()
            
            // Execute authoritative transaction
            val result = MiniGameCoreEngine.playScratchCard(
                uid = uid,
                stakeAmount = betAmount,
                availableReservePool = reservePool
            )

            result.onSuccess { gameResult ->
                // Authoritative Outcome Sync
                resultMultiplier = gameResult.multiplier.toInt()
                winAmount = gameResult.winAmount

                // Reset Scratch UI State
                scratchPath.value = Path()
                revealedCells.fill(false)
                revealedCellCount = 0
                dragPointCount = 0
                isRevealed = false
                isTicketPurchased = true
            }.onFailure { error ->
                Toast.makeText(context, error.message ?: "ট্রানজেকশন ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0D21), Color(0xFF151838), Color(0xFF1E1538))))
    ) {
        // --- TOP HUD (Identical to Dice & Spin Wheel with Glowing Add Button) ---
        TopBalanceHUD(
            mainBalance = mainBalance,
            gameWallet = gameWalletBalance,
            onBack = onNavigateBack,
            onWalletClick = { showTransferModal = true },
            isScratching = isTicketPurchased && !isRevealed
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 150.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Grand Casino Title
            Text(
                "SCRATCH CARD",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    brush = Brush.verticalGradient(listOf(Color(0xFFFFF7C2), Color(0xFFFFD700), Color(0xFFD97706)))
                ),
                modifier = Modifier.shadow(8.dp, spotColor = AmberGlow)
            )
            Text(
                "ঘষে জিতুন",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFD700).copy(alpha = 0.85f),
                    letterSpacing = 4.sp
                )
            )

            Spacer(modifier = Modifier.weight(0.15f))

            // --- 3D Scratch Ticket ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                TicketFrame(
                    scratchedPath = scratchPath.value,
                    isRevealed = isRevealed,
                    isTicketPurchased = isTicketPurchased,
                    icons = icons,
                    winText = if (resultMultiplier > 0) "${resultMultiplier}X WIN" else "TRY AGAIN",
                    onScratchPoint = { offset, canvasSize ->
                        if (!isTicketPurchased || isRevealed) return@TicketFrame
                        scratchPath.value = Path().apply {
                            addPath(scratchPath.value)
                            addOval(Rect(offset, 32.dp.value))
                        }

                        dragPointCount++

                        if (canvasSize.width > 0 && canvasSize.height > 0) {
                            val cellW = canvasSize.width / 10f
                            val cellH = canvasSize.height / 8f
                            val centerCol = (offset.x / cellW).toInt().coerceIn(0, 9)
                            val centerRow = (offset.y / cellH).toInt().coerceIn(0, 7)

                            for (r in (centerRow - 1)..(centerRow + 1)) {
                                for (c in (centerCol - 1)..(centerCol + 1)) {
                                    if (r in 0..7 && c in 0..9) {
                                        val idx = r * 10 + c
                                        if (!revealedCells[idx]) {
                                            revealedCells[idx] = true
                                            revealedCellCount++
                                        }
                                    }
                                }
                            }

                            val progress = revealedCellCount.toFloat() / totalCells.toFloat()
                            if (progress >= 0.30f) {
                                revealCard()
                            }
                        }
                    },
                    onDragEnded = {
                        if (isTicketPurchased && !isRevealed && dragPointCount >= 12) {
                            revealCard()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Instruction Badge
            Surface(
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Text(
                    text = when {
                        !isTicketPurchased -> "প্রথমে নিচের বাটনে চাপ দিয়ে নতুন কার্ড নিন"
                        isRevealed -> "কার্ড খোলা হয়েছে (Card Revealed)"
                        else -> "আঙুল দিয়ে কার্ডটি ঘষুন (Scratch to Reveal)"
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Failsafe "পুরোটা খুলুন (Reveal All)" Button
            if (isTicketPurchased && !isRevealed) {
                TextButton(
                    onClick = { revealCard() },
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        "পুরোটা খুলুন (Reveal All)",
                        color = GoldMetallic,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(40.dp))
            }

            Spacer(modifier = Modifier.weight(0.45f))

            // Betting Console
            val isActionEnabled = (!isTicketPurchased || isRevealed) && gameWalletBalance >= selectedBetAmount
            val buttonText = when {
                gameWalletBalance < selectedBetAmount -> "অপর্যাপ্ত ব্যালেন্স"
                isTicketPurchased && !isRevealed -> "কার্ডটি ঘষুন..."
                else -> "নতুন কার্ড নিন (NEW TICKET)"
            }

            CasinoBettingConsole(
                selectedBet = selectedBetAmount.toInt(),
                onBetSelected = { 
                    if (!isTicketPurchased || isRevealed) {
                        selectedBetAmount = it.toLong()
                    }
                },
                actionButtonText = buttonText,
                isActionEnabled = isActionEnabled,
                onActionClick = { buyTicket() }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal Overlays (Centralized TransferModal from MiniGameUtils.kt)
    if (showTransferModal) {
        TransferModal(
            uid = uid,
            userName = userName,
            mainBalance = mainBalance,
            gameBalance = gameWalletBalance,
            accentColor = GoldMetallic,
            accentColorLight = Color(0xFFFFF7C2),
            backgroundColor = ScratchDeepPurple,
            onDismiss = { showTransferModal = false },
            onConfirm = { amount, type ->
                showTransferModal = false
                val database = FirebaseManager.getDatabase()
                if (database != null && uid.isNotBlank()) {
                    val requestRef = database.getReference("miniGames/transferRequests").push()
                    val requestData = mapOf(
                        "uid" to uid,
                        "userName" to userName,
                        "userPhone" to userPhone,
                        "amount" to amount,
                        "type" to type,
                        "status" to "PENDING",
                        "timestamp" to ServerValue.TIMESTAMP
                    )
                    requestRef.setValue(requestData).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "রিকোয়েস্ট সফলভাবে জমা হয়েছে!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }

    if (showWinDialog) {
        WinResultDialog(
            isWin = resultMultiplier > 0,
            multiplier = resultMultiplier,
            winAmount = winAmount,
            onDismiss = { showWinDialog = false }
        )
    }
}

@Composable
fun TicketFrame(
    scratchedPath: Path,
    isRevealed: Boolean,
    isTicketPurchased: Boolean,
    icons: List<ImageVector>,
    winText: String,
    onScratchPoint: (Offset, Size) -> Unit,
    onDragEnded: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(20.dp, RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFFE5B14B), Color(0xFFD4AF37), Color(0xFFB8860B))),
                RoundedCornerShape(16.dp)
            )
            .border(4.dp, Color(0xFF8B4513).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ticket Notches (Left & Right)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val notchRadius = 12.dp.toPx()
            drawCircle(
                color = ScratchNavy,
                radius = notchRadius,
                center = Offset(0f, size.height / 2),
                blendMode = BlendMode.SrcOver
            )
            drawCircle(
                color = ScratchNavy,
                radius = notchRadius,
                center = Offset(size.width, size.height / 2),
                blendMode = BlendMode.SrcOver
            )
        }

        // Inner Ticket Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
        ) {
            // Secret Layer (Underneath content)
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    icons.forEach { icon ->
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFFD4AF37)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .border(2.dp, Color.DarkGray.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        winText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.DarkGray.copy(alpha = 0.8f)
                        )
                    )
                }
            }

            // Scratch Layer (Foil)
            val alpha by animateFloatAsState(
                targetValue = if (isRevealed) 0f else 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                label = "foilAlpha"
            )

            if (alpha > 0.01f) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isTicketPurchased, isRevealed) {
                            if (!isTicketPurchased || isRevealed) return@pointerInput
                            val currentSize = Size(size.width.toFloat(), size.height.toFloat())
                            detectDragGestures(
                                onDragStart = { offset ->
                                    onScratchPoint(offset, currentSize)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    onScratchPoint(change.position, currentSize)
                                },
                                onDragEnd = {
                                    onDragEnded()
                                },
                                onDragCancel = {
                                    onDragEnded()
                                }
                            )
                        }
                ) {
                    val offscreen = Paint().apply {
                        blendMode = BlendMode.SrcOver
                    }

                    drawContext.canvas.saveLayer(Rect(Offset.Zero, size), offscreen)

                    // Holographic Foil Gradient (Silver-Gold shimmering)
                    drawRect(
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFFC0C0C0), // Silver
                                Color(0xFFE5B14B), // Gold
                                Color(0xFFE5E4E2), // Platinum
                                Color(0xFFFFD700), // Gold
                                Color(0xFFC0C0C0)  // Silver
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        ),
                        alpha = alpha
                    )

                    // Textured Shimmer lines
                    for (i in -10..20) {
                        drawLine(
                            Color.White.copy(alpha = 0.15f * alpha),
                            Offset(0f, i * 30f),
                            Offset(size.width, i * 30f + 150f),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    // Scratched transparent path (revealing content underneath)
                    drawPath(
                        path = scratchedPath,
                        color = Color.Transparent,
                        style = Stroke(width = 44.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                        blendMode = BlendMode.Clear
                    )

                    drawContext.canvas.restore()
                }
            }
        }

        // Diamond Studs on corners
        val studs = listOf(
            Alignment.TopStart, Alignment.TopEnd, 
            Alignment.BottomStart, Alignment.BottomEnd,
            Alignment.TopCenter, Alignment.BottomCenter
        )
        studs.forEach { alignment ->
            Box(
                modifier = Modifier
                    .align(alignment)
                    .padding(4.dp)
                    .size(8.dp)
                    .background(
                        Brush.radialGradient(listOf(Color.White, Color.LightGray)),
                        CircleShape
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            )
        }
    }
}

/**
 * Top HUD identical to DiceRollGame and SpinWheelGame with:
 * - Frosted circular back button
 * - Main Balance Pill
 * - Interactive Game Wallet Button with glowing '+' badge
 */
@Composable
private fun TopBalanceHUD(
    mainBalance: Double,
    gameWallet: Double,
    onBack: () -> Unit,
    onWalletClick: () -> Unit,
    isScratching: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Frosted Glass Back Button
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
        ) {
            IconButton(
                onClick = onBack,
                enabled = !isScratching,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Main Balance Pill (Frosted dark glass with gold accent, min 46dp height)
            Surface(
                modifier = Modifier
                    .defaultMinSize(minHeight = 46.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.75f),
                border = BorderStroke(1.dp, GoldMetallic.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = GoldMetallic,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            "Main Balance",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                        Text(
                            "৳${"%.2f".format(mainBalance)}",
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            // 2. Game Wallet Interactive Button (Gold border, coin icon, gold text, '+' badge)
            Surface(
                modifier = Modifier
                    .defaultMinSize(minHeight = 46.dp)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(enabled = !isScratching) { onWalletClick() },
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF1E1538).copy(alpha = 0.85f),
                border = BorderStroke(1.5.dp, GoldMetallic)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = GoldMetallic,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            "Game Wallet",
                            fontSize = 10.sp,
                            color = GoldMetallic.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            "৳${"%.2f".format(gameWallet)}",
                            fontSize = 13.sp,
                            color = GoldMetallic,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = GoldMetallic,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Funds",
                                tint = Color.Black,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WinResultDialog(
    isWin: Boolean,
    multiplier: Int,
    winAmount: Double,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1E1538),
            border = BorderStroke(2.dp, if (isWin) Color(0xFFFFD700) else Color.White.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = if (isWin) Color(0xFFFFD700).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                    border = BorderStroke(1.5.dp, if (isWin) Color(0xFFFFD700) else Color.White.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isWin) Icons.Default.Star else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isWin) Color(0xFFFFD700) else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isWin) "অভিনন্দন!" else "দুঃখিত!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    ),
                    color = if (isWin) Color(0xFFFFD700) else Color.White
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (isWin) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "${multiplier}X WIN",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD700)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "আপনি ৳${"%.2f".format(winAmount)} জিতেছেন!\nটাকা আপনার গেম ওয়ালেটে যুক্ত হয়েছে।",
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        text = "এবার কোনো মিল পাওয়া যায়নি। আবার চেষ্টা করুন!",
                        textAlign = TextAlign.Center,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                TournamentButton(
                    text = "বন্ধ করুন",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
