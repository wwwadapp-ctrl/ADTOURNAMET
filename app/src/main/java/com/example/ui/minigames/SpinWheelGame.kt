package com.example.ui.minigames

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.domain.repository.AdminRepository
import com.example.ui.components.TournamentButton
import com.example.ui.theme.*
import com.example.ui.wallet.WalletViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.example.core.firebase.FirebaseManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import java.security.SecureRandom
import kotlin.math.PI
import com.example.core.config.FirebaseConfig
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.digits
import com.example.core.i18n.taka
import com.example.core.i18n.takaWhole
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.cos
import kotlin.math.sin

// --- Custom Casino Palette ---
val RoyalPurple = Color(0xFF4A0072)
val DeepVelvet = Color(0xFF2E004B)
val CasinoGold = Color(0xFFD4AF37)
val CasinoGoldLight = Color(0xFFF9E79F)
val CasinoGoldDark = Color(0xFF9A7B2C)
val RadiantViolet = Color(0xFF7B1FA2)
val RadiantGold = Color(0xFFFFD600)

data class MoneySlice(
    val multiplier: Int,
    val label: String,
    val weight: Double
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpinWheelScreen(
    walletViewModel: WalletViewModel,
    adminRepository: AdminRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val walletState by walletViewModel.uiState.collectAsState()
    val userBalance = walletState.mainBalance
    
    val authUser = remember { FirebaseManager.getAuth()?.currentUser }
    val uid = remember { authUser?.uid ?: walletState.wallet?.uid ?: "" }
    var gameBalance by remember { mutableStateOf(0.0) }
    var userPhone by remember(walletState.user) { mutableStateOf(walletState.user?.effectiveMobile ?: authUser?.phoneNumber ?: "") }
    var userName by remember(walletState.user) { mutableStateOf(walletState.user?.effectiveName ?: authUser?.displayName?.ifBlank { null } ?: "User") }
    var isTransferModalVisible by remember { mutableStateOf(false) }
    var currentReservePool by remember { mutableStateOf(0.0) }

    // Global Config states
    var isMiniGamesEnabled by remember { mutableStateOf(true) }
    var isSpinGameActive by remember { mutableStateOf(true) }
    val isGameActive = isMiniGamesEnabled && isSpinGameActive

    // Observe Reserve Pool balance
    DisposableEffect(Unit) {
        val database = FirebaseManager.getDatabase()
        val ref = database?.getReference("miniGames/vault/reservePool")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentReservePool = snapshot.value?.toString()?.toDoubleOrNull() ?: 0.0
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref?.addValueEventListener(listener)

        val configRef = database?.getReference("miniGames/globalConfig")
        val configListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isMiniGamesEnabled = snapshot.child("miniGamesEnabled").value?.toString()?.toBoolean() ?: true
                isSpinGameActive = snapshot.child("spinGameActive").value?.toString()?.toBoolean() ?: true
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        configRef?.addValueEventListener(configListener)

        onDispose { 
            ref?.removeEventListener(listener)
            configRef?.removeEventListener(configListener)
        }
    }

    // Observe Game Wallet Balance
    DisposableEffect(uid) {
        if (uid.isBlank()) return@DisposableEffect onDispose {}
        val database = FirebaseManager.getDatabase()
        val ref = database?.getReference("gameWallets")?.child(uid)?.child("balance")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                gameBalance = snapshot.value?.toString()?.toDoubleOrNull() ?: 0.0
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref?.addValueEventListener(listener)
        onDispose { ref?.removeEventListener(listener) }
    }

    // Observe User Info (Phone & Name) for Transfer Requests
    DisposableEffect(uid) {
        if (uid.isBlank()) return@DisposableEffect onDispose {}
        val database = FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL)
        val ref = database.getReference("users").child(uid)
        
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
        
        ref.addListenerForSingleValueEvent(profileValueListener)
        ref.addValueEventListener(profileValueListener)
        onDispose { ref.removeEventListener(profileValueListener) }
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
    
    var isSpinning by remember { mutableStateOf(false) }
    val animatedRotation = remember { Animatable(0f) }
    var selectedBetAmount by remember { mutableStateOf(1L) } 
    
    val strings = LocalAppStrings.current
    
    var showWinDialog by remember { mutableStateOf(false) }
    var lastWinAmount by remember { mutableStateOf(0.0) }
    var lastWinMultiplier by remember { mutableStateOf(0) }

    // Exactly 24 slices as per multiplier requirements
    val wheelSlices = remember {
        listOf(
            MoneySlice(0, "x0", 0.55),
            MoneySlice(1, "x1", 0.20),
            MoneySlice(2, "x2", 0.10),
            MoneySlice(0, "x0", 0.55),
            MoneySlice(5, "x5", 0.05),
            MoneySlice(0, "x0", 0.55),
            MoneySlice(10, "x10", 0.02),
            MoneySlice(0, "x0", 0.55),
            MoneySlice(1, "x1", 0.20),
            MoneySlice(2, "x2", 0.10),
            MoneySlice(0, "x0", 0.55),
            MoneySlice(25, "x25", 0.01),
            MoneySlice(0, "x0", 0.55),
            MoneySlice(1, "x1", 0.20),
            MoneySlice(5, "x5", 0.05),
            MoneySlice(0, "x0", 0.55),
            MoneySlice(2, "x2", 0.10),
            MoneySlice(0, "x0", 0.55),
            MoneySlice(5, "x5", 0.05),
            MoneySlice(0, "x0", 0.55),
            MoneySlice(10, "x10", 0.02),
            MoneySlice(0, "x0", 0.55),
            MoneySlice(50, "x50", 0.005),
            MoneySlice(0, "x0", 0.55)
        )
    }

    // LED blinking animation (Marquee style)
    val infiniteTransition = rememberInfiniteTransition(label = "LEDs")
    val ledAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ledAlpha"
    )

    val context = LocalContext.current

    fun performBetAndSpin() {
        if (isSpinning || !isGameActive) return
        if (gameBalance < selectedBetAmount) {
            Toast.makeText(context, strings.spinToastNoBalance, Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            val betAmount = selectedBetAmount.toDouble()
            
            // 1. Authoritative Engine Execution
            val result = MiniGameCoreEngine.playSpinWheel(
                uid = uid,
                stakeAmount = betAmount,
                availableReservePool = currentReservePool,
                slices = wheelSlices.map { it.multiplier.toDouble() }
            )

            result.onSuccess { gameResult ->
                isSpinning = true
                
                // 2. Animation Target Calculation
                val winnerIndex = gameResult.winnerIndex
                val sliceAngle = 360f / 24f
                val random = SecureRandom()
                
                val currentRotationOffset = animatedRotation.value % 360f
                val winnerSliceCenterAngle = winnerIndex * sliceAngle + sliceAngle / 2
                val desiredFinalAngle = (270f - winnerSliceCenterAngle + 360f) % 360f
                
                val rotations = 360f * (7 + random.nextInt(3)) 
                val targetTotalRotation = animatedRotation.value + rotations + (desiredFinalAngle - currentRotationOffset + 360f) % 360f

                // 3. Trigger 6-Second Spin Animation
                animatedRotation.animateTo(
                    targetValue = targetTotalRotation,
                    animationSpec = tween(
                        durationMillis = 6000,
                        easing = FastOutSlowInEasing
                    )
                )

                // 4. Visual Resolution (Financials already committed by Engine)
                lastWinAmount = gameResult.winAmount
                lastWinMultiplier = gameResult.multiplier.toInt()
                showWinDialog = true
                isSpinning = false
            }.onFailure { error ->
                Toast.makeText(
                    context, 
                    error.message ?: strings.spinToastFailed, 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ---------------- NEW MONEY WHEEL LOOK (visual only; game logic above is unchanged) ----------------
    val whScope = rememberCoroutineScope()
    LaunchedEffect(Unit) { WhSfx.prepare(context) }
    val pegGlow = remember { FloatArray(24) }
    var glowTick by remember { mutableStateOf(0) }
    val pointerKick = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val sl = 360f / 24f
        fun under(r: Float) = (((270f - r) % 360f + 360f) % 360f / sl).toInt()
        var last = under(animatedRotation.value)
        snapshotFlow { animatedRotation.value }.collect { r ->
            for (q in 0 until 24) pegGlow[q] *= 0.86f
            val now = under(r)
            if (now != last) {
                last = now
                val j = ((Math.round((270f - r) / sl) % 24) + 24) % 24
                pegGlow[j] = 1f
                WhSfx.play(WhSfx.S.TICK, 0.9f)
                whScope.launch { pointerKick.snapTo(-22f); pointerKick.animateTo(0f, spring(0.35f, 900f)) }
            }
            glowTick++
        }
    }
    LaunchedEffect(isSpinning) {
        if (isSpinning) WhSfx.play(WhSfx.S.SPIN, 0.8f)
        else repeat(20) { for (q in 0 until 24) pegGlow[q] *= 0.8f; glowTick++; delay(25) }
    }
    LaunchedEffect(showWinDialog) {
        if (showWinDialog) {
            WhSfx.play(if (lastWinMultiplier > 0) WhSfx.S.WIN else WhSfx.S.LOSE)
            delay(3500)
            showWinDialog = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1E2D93), Color(0xFF121A61))))
    ) {
        val u = maxWidth / 100
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val geo = WhGeo(maxWidth, maxWidth * 2.0514f, topInset + maxWidth * 0.113f)

        WhStage(geo, isSpinning)
        WhWheel(
            geo = geo,
            rotation = animatedRotation.value,
            labels = wheelSlices.map { it.multiplier },
            digits = { strings.digits(it) },
            pegGlow = pegGlow,
            glowTick = glowTick,
            pointerKick = pointerKick.value
        )
        WhHost(geo, clap = showWinDialog && lastWinMultiplier > 0)

        WhTopBar(
            u = u,
            mainText = strings.taka(userBalance),
            gameText = strings.taka(gameBalance),
            mainLabel = strings.spinChipMain,
            gameLabel = strings.spinChipGame,
            enabled = !isSpinning,
            onBack = onNavigateBack,
            onGame = { isTransferModalVisible = true }
        )

        WhConsole(u, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = u * 4, end = u * 4, bottom = u * 5)) {
            if (!isGameActive) WhClosedBanner(u, strings.spinClosedBanner)
            WhChipRow(
                u = u,
                options = listOf(1, 3, 5, 8, 10),
                selected = selectedBetAmount.toInt(),
                enabled = !isSpinning,
                label = { "৳" + strings.digits(it.toString()) },
                onSelect = { WhSfx.play(WhSfx.S.CLICK, 0.8f); selectedBetAmount = it.toLong() }
            )
            WhSpinButton(
                u = u,
                text = when {
                    !isGameActive -> strings.spinClosedBanner
                    gameBalance < selectedBetAmount -> strings.spinNoBalance
                    isSpinning -> strings.spinSpinning
                    else -> strings.spinButton
                },
                enabled = !isSpinning && selectedBetAmount > 0 && gameBalance >= selectedBetAmount && isGameActive,
                bet = selectedBetAmount.toInt(),
                chipLabel = "৳" + strings.digits(selectedBetAmount.toString()),
                onClick = { WhSfx.play(WhSfx.S.CLICK); performBetAndSpin() }
            )
        }

        if (showWinDialog) {
            val isWin = lastWinMultiplier > 0
            if (isWin) WhConfetti()
            Box(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = maxHeight * 0.27f)) {
                WhResultCard(
                    u = u,
                    win = isWin,
                    bigWin = lastWinMultiplier >= 10,
                    title = if (isWin) strings.mgWinTitle else strings.mgLossTitle,
                    amount = "+" + strings.taka(lastWinAmount),
                    sub = if (isWin) strings.spinResultWin(strings.digits(lastWinMultiplier.toString())) else strings.mgLossMessage,
                    onClose = { showWinDialog = false }
                )
            }
        }
    }

    // --- TRANSFER MODAL ---
    if (isTransferModalVisible) {
        val context = LocalContext.current
        TransferModal(
            uid = uid,
            userName = userName,
            userPhone = userPhone,
            mainBalance = userBalance,
            gameBalance = gameBalance,
            accentColor = CasinoGold,
            accentColorLight = RadiantGold,
            backgroundColor = DeepVelvet,
            onDismiss = { isTransferModalVisible = false },
            onConfirm = { amount, type ->
                // Dismiss dialog immediately
                isTransferModalVisible = false
                
                val database = FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL)
                if (uid.isNotBlank()) {
                    val requestRef = database.getReference("miniGames/transferRequests").push()
                    val requestData = mapOf(
                        "uid" to uid,
                        "userName" to userName,
                        "userPhone" to userPhone,
                        "amount" to amount,
                        "type" to type, // ADD or WITHDRAW
                        "status" to "PENDING",
                        "timestamp" to ServerValue.TIMESTAMP,
                        "source" to "SPIN_WHEEL"
                    )
                    
                    requestRef.setValue(requestData).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(
                                context,
                                "রিকোয়েস্ট সফলভাবে জমা হয়েছে! এডমিন অ্যাপ্রুভ করলে ব্যালেন্স আপডেট হবে।",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        )
    }
}

