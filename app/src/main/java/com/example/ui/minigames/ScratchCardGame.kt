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
import com.example.core.i18n.BengaliStrings
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.digits
import com.example.core.i18n.taka
import androidx.compose.ui.unit.TextUnit
import com.example.ui.components.TournamentButton
import com.example.ui.wallet.WalletViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.SecureRandom

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
    val strings = LocalAppStrings.current
    val walletState by walletViewModel.uiState.collectAsState()
    val mainBalance = walletState.mainBalance

    val authUser = remember { FirebaseManager.getAuth()?.currentUser }
    val uid = remember { authUser?.uid ?: walletState.wallet?.uid ?: "" }
    var gameWalletBalance by remember { mutableDoubleStateOf(0.0) }
    var reservePool by remember { mutableDoubleStateOf(0.0) }
    var userName by remember(walletState.user) { mutableStateOf(walletState.user?.effectiveName ?: authUser?.displayName?.ifBlank { null } ?: "User") }
    var userPhone by remember(walletState.user) { mutableStateOf(walletState.user?.effectiveMobile ?: authUser?.phoneNumber ?: "") }
    var showTransferModal by remember { mutableStateOf(false) }

    // Global Config states
    var isMiniGamesEnabled by remember { mutableStateOf(true) }
    var isScratchCardGameActive by remember { mutableStateOf(true) }
    val isGameActive = isMiniGamesEnabled && isScratchCardGameActive

    // Observers (Synchronized with Dice and Wheel mini-games)
    DisposableEffect(uid) {
        if (uid.isBlank()) return@DisposableEffect onDispose {}
        val database = com.google.firebase.database.FirebaseDatabase.getInstance(com.example.core.config.FirebaseConfig.DATABASE_URL)
        
        val balanceRef = database.getReference("gameWallets").child(uid).child("balance")
        val balanceListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                gameWalletBalance = snapshot.value?.toString()?.toDoubleOrNull() ?: 0.0
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        balanceRef.addValueEventListener(balanceListener)

        val poolRef = database.getReference("miniGames/vault/reservePool")
        val poolListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                reservePool = snapshot.value?.toString()?.toDoubleOrNull() ?: 0.0
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        poolRef.addValueEventListener(poolListener)

        val configRef = database.getReference("miniGames/globalConfig")
        val configListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isMiniGamesEnabled = snapshot.child("miniGamesEnabled").value?.toString()?.toBoolean() ?: true
                isScratchCardGameActive = snapshot.child("scratchCardGameActive").value?.toString()?.toBoolean() ?: true
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        configRef.addValueEventListener(configListener)

        val userRef = database.getReference("users").child(uid)
        val profileValueListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val fetchedName = snapshot.child("name").value?.toString()?.trim()
                    ?: snapshot.child("displayName").value?.toString()?.trim()
                    ?: snapshot.child("userName").value?.toString()?.trim()
                    ?: authUser?.displayName?.trim()
                    ?: "User"
                val fetchedPhone = snapshot.child("phone").value?.toString()?.trim()
                    ?: snapshot.child("mobile").value?.toString()?.trim()
                    ?: snapshot.child("phoneNumber").value?.toString()?.trim()
                    ?: authUser?.phoneNumber?.trim()
                    ?: ""
                
                userName = if (fetchedName.isNotBlank() && fetchedName != "User" && fetchedName != "Player") fetchedName else userName
                userPhone = if (fetchedPhone.isNotBlank()) fetchedPhone else userPhone
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        
        userRef.addListenerForSingleValueEvent(profileValueListener)
        userRef.addValueEventListener(profileValueListener)

        onDispose {
            balanceRef.removeEventListener(balanceListener)
            poolRef.removeEventListener(poolListener)
            configRef.removeEventListener(configListener)
            userRef.removeEventListener(profileValueListener)
        }
    }

    // Sync with walletState user profile
    LaunchedEffect(walletState.user) {
        walletState.user?.let {
            if (it.effectiveName.isNotBlank() && it.effectiveName != "User" && it.effectiveName != "Player") {
                userName = it.effectiveName
            }
            if (it.effectiveMobile.isNotBlank()) {
                userPhone = it.effectiveMobile
            }
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
        if ((isTicketPurchased && !isRevealed) || !isGameActive) return
        if (gameWalletBalance < selectedBetAmount) {
            Toast.makeText(context, strings.scratchNoBalance, Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, error.message ?: strings.scratchToastFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------------- NEW SCRATCH CARD LOOK (visual only; game logic above is unchanged) ----------------
    LaunchedEffect(Unit) { WhSfx.prepare(context); ScSfx.prepare(context) }
    var ticketNo by remember { mutableIntStateOf(0) }
    var curtainOpen by remember { mutableStateOf(false) }
    var revealStep by remember { mutableIntStateOf(0) }
    LaunchedEffect(isTicketPurchased, isRevealed) {
        if (isTicketPurchased && !isRevealed) { ticketNo++; curtainOpen = true }
    }
    LaunchedEffect(isRevealed) {
        revealStep = 0
        if (isRevealed) { delay(600); revealStep = 1; delay(500); revealStep = 2 }
    }
    val showCard = showWinDialog && revealStep >= 2
    LaunchedEffect(showCard) {
        if (showCard) {
            curtainOpen = false
            WhSfx.play(if (resultMultiplier > 0) WhSfx.S.WIN else WhSfx.S.LOSE)
            delay(3500)
            showWinDialog = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B1040), Color(0xFF121A61))))
    ) {
        val u = maxWidth / 100
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val geo = WhGeo(maxWidth, maxWidth * 1.7917f, topInset + maxWidth * 0.041f)

        ScStage(geo, strings.scratchTitle, strings.scratchSubtitle, u)
        ScHost(geo, clap = showCard && resultMultiplier > 0)
        if (isTicketPurchased && isRevealed && revealStep >= 1) {
            ScResultTag(geo, u, if (resultMultiplier > 0) strings.scratchWinX(strings.digits(resultMultiplier.toString())) else strings.scratchTryAgain)
        }
        ScTicket(
            geo = geo,
            u = u,
            mult = resultMultiplier,
            ticketNo = ticketNo,
            purchased = isTicketPurchased,
            revealed = isRevealed,
            curtainOpen = curtainOpen,
            scratchedPath = scratchPath.value,
            foilText = strings.scratchInstructionScratching,
            onScratchPoint = { offset, canvasSize ->
                if (!isTicketPurchased || isRevealed) return@ScTicket
                ScSfx.scratch()
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
        if (isTicketPurchased && !isRevealed && curtainOpen) {
            ScRevealButton(geo, u, strings.scratchRevealAll) { WhSfx.play(WhSfx.S.CLICK, 0.8f); revealCard() }
        }
        ScLegend(geo, u) { strings.digits(it.toString()) + "x" }

        WhTopBar(
            u = u,
            mainText = strings.taka(mainBalance),
            gameText = strings.taka(gameWalletBalance),
            mainLabel = strings.spinChipMain,
            gameLabel = strings.spinChipGame,
            enabled = !(isTicketPurchased && !isRevealed),
            onBack = onNavigateBack,
            onGame = { showTransferModal = true }
        )

        val isActionEnabled = (!isTicketPurchased || isRevealed) && gameWalletBalance >= selectedBetAmount && isGameActive
        val buttonText = when {
            !isGameActive -> strings.scratchClosedBanner
            gameWalletBalance < selectedBetAmount -> strings.scratchNoBalance
            isTicketPurchased && !isRevealed -> strings.scratchCardScratching
            else -> strings.scratchNewTicketButton
        }
        WhConsole(u, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = u * 4, end = u * 4, bottom = u * 5)) {
            if (!isGameActive) WhClosedBanner(u, strings.scratchClosedBanner)
            WhChipRow(
                u = u,
                options = listOf(1, 3, 5, 8, 10),
                selected = selectedBetAmount.toInt(),
                enabled = !isTicketPurchased || isRevealed,
                label = { "৳" + strings.digits(it.toString()) },
                onSelect = {
                    if (!isTicketPurchased || isRevealed) {
                        WhSfx.play(WhSfx.S.CLICK, 0.8f); selectedBetAmount = it.toLong()
                    }
                }
            )
            WhSpinButton(
                u = u,
                text = buttonText,
                enabled = isActionEnabled,
                bet = selectedBetAmount.toInt(),
                chipLabel = "৳" + strings.digits(selectedBetAmount.toString()),
                onClick = { WhSfx.play(WhSfx.S.CLICK); buyTicket() }
            )
        }

        if (showCard) {
            if (resultMultiplier > 0) WhConfetti()
            Box(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = maxHeight * 0.24f)) {
                WhResultCard(
                    u = u,
                    win = resultMultiplier > 0,
                    bigWin = resultMultiplier >= 5,
                    title = if (resultMultiplier > 0) strings.mgWinTitle else strings.mgLossTitle,
                    amount = "+" + strings.taka(winAmount),
                    sub = if (resultMultiplier > 0) strings.scratchWinX(strings.digits(resultMultiplier.toString())) else strings.scratchLossMessage,
                    onClose = { showWinDialog = false }
                )
            }
        }
    }

    // Modal Overlays
    if (showTransferModal) {
        TransferModal(
            uid = uid,
            userName = userName,
            userPhone = userPhone,
            mainBalance = mainBalance,
            gameBalance = gameWalletBalance,
            accentColor = GoldMetallic,
            accentColorLight = Color(0xFFFFF7C2),
            backgroundColor = ScratchDeepPurple,
            onDismiss = { showTransferModal = false },
            onConfirm = { amount, type ->
                showTransferModal = false
                val database = com.google.firebase.database.FirebaseDatabase.getInstance(com.example.core.config.FirebaseConfig.DATABASE_URL)
                if (uid.isNotBlank()) {
                    val requestRef = database.getReference("miniGames/transferRequests").push()
                    val requestData = mapOf(
                        "uid" to uid,
                        "userName" to userName,
                        "userPhone" to userPhone,
                        "amount" to amount,
                        "type" to type,
                        "status" to "PENDING",
                        "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP,
                        "source" to "SCRATCH_CARD"
                    )
                    requestRef.setValue(requestData).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, strings.scratchTransferToast, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
}

