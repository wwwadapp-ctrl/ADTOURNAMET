package com.example.ui.minigames

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.firebase.FirebaseManager
import com.example.ui.components.TournamentButton
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.digits
import com.example.core.i18n.taka
import com.example.ui.theme.*
import com.example.ui.wallet.WalletViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.example.core.config.FirebaseConfig
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.SecureRandom
import kotlin.coroutines.resume

// --- Head & Tail Palette ---
private val HTDeepPurple = Color(0xFF1E1538)
private val HTGold = Color(0xFFFFD700)
private val HTGoldLight = Color(0xFFFFF7C2)

enum class CoinSide { HEAD, TAIL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeadTailGameScreen(
    walletViewModel: WalletViewModel,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val walletState by walletViewModel.uiState.collectAsState()
    val userBalance = walletState.mainBalance

    val authUser = remember { FirebaseManager.getAuth()?.currentUser }
    val uid = remember { authUser?.uid ?: walletState.wallet?.uid ?: "" }
    var gameBalance by remember { mutableStateOf(0.0) }
    var userName by remember(walletState.user) { mutableStateOf(walletState.user?.effectiveName ?: authUser?.displayName?.ifBlank { null } ?: "User") }
    var userPhone by remember(walletState.user) { mutableStateOf(walletState.user?.effectiveMobile ?: authUser?.phoneNumber ?: "") }
    var currentReservePool by remember { mutableStateOf(0.0) }
    var isTransferModalVisible by remember { mutableStateOf(false) }

    // Global Config states
    var isMiniGamesEnabled by remember { mutableStateOf(true) }
    var isHeadTailGameActive by remember { mutableStateOf(true) }
    val isGameActive = isMiniGamesEnabled && isHeadTailGameActive

    // Observers
    DisposableEffect(uid) {
        if (uid.isBlank()) return@DisposableEffect onDispose {}
        val database = FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL)
        
        val balanceRef = database.getReference("gameWallets").child(uid).child("balance")
        val balanceListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                gameBalance = snapshot.value?.toString()?.toDoubleOrNull() ?: 0.0
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        balanceRef.addValueEventListener(balanceListener)

        val poolRef = database.getReference("miniGames/vault/reservePool")
        val poolListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentReservePool = snapshot.value?.toString()?.toDoubleOrNull() ?: 0.0
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        poolRef.addValueEventListener(poolListener)

        val configRef = database.getReference("miniGames/globalConfig")
        val configListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isMiniGamesEnabled = snapshot.child("miniGamesEnabled").value?.toString()?.toBoolean() ?: true
                isHeadTailGameActive = snapshot.child("headTailGameActive").value?.toString()?.toBoolean() ?: true
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
    var isRolling by remember { mutableStateOf(false) }
    var selectedSide by remember { mutableStateOf(CoinSide.HEAD) }
    var selectedBetAmount by remember { mutableStateOf(1L) }
    var currentCoinSide by remember { mutableStateOf(CoinSide.HEAD) }
    
    val strings = LocalAppStrings.current
    
    var resultText by remember(strings) { mutableStateOf<String?>(null) }
    var showWinDialog by remember { mutableStateOf(false) }
    var lastWinAmount by remember { mutableStateOf(0.0) }
    var isLastWin by remember { mutableStateOf(false) }

    // Animation States
    val infiniteTransition = rememberInfiniteTransition(label = "Particles")
    val particleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "particleAlpha"
    )

    val coinRotationX = remember { Animatable(0f) }
    val coinTranslationY = remember { Animatable(0f) }
    val coinScale = remember { Animatable(1f) }

    val tossingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "tossingAlpha"
    )

    fun performFlip() {
        if (isRolling || !isGameActive) return
        if (gameBalance < selectedBetAmount) {
            Toast.makeText(context, strings.headTailToastNoBalance, Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isRolling = true
            resultText = null
            val betAmount = selectedBetAmount.toDouble()

            // 1. Authoritative Engine Execution
            val result = MiniGameCoreEngine.playHeadTail(
                uid = uid,
                stakeAmount = betAmount,
                choice = selectedSide.name, // "HEAD" or "TAIL"
                availableReservePool = currentReservePool
            )

            result.onSuccess { gameResult ->
                val finalSide = if (gameResult.coinSide == "HEAD") CoinSide.HEAD else CoinSide.TAIL

                // 2. Animation (1.8s)
                val animationJob = launch {
                    // High-speed rotation
                    repeat(18) { i ->
                        currentCoinSide = if (i % 2 == 0) CoinSide.HEAD else CoinSide.TAIL
                        launch { coinTranslationY.animateTo(-150f, tween(100, easing = LinearEasing)) }
                        launch { coinRotationX.animateTo(i * 360f, tween(100, easing = LinearEasing)) }
                        delay(100)
                    }
                }
                
                delay(1800)
                animationJob.cancel()
                
                // 3. Settle conclusively to pre-determined side
                currentCoinSide = finalSide
                launch { coinTranslationY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                launch { coinRotationX.animateTo(if (finalSide == CoinSide.HEAD) 0f else 180f, spring()) }
                
                isRolling = false
                val sideStr = if (finalSide == CoinSide.HEAD) strings.headTailChoiceHead else strings.headTailChoiceTail
                resultText = strings.headTailResult(sideStr)

                // 4. Visual Resolution (Financials already committed by Engine)
                lastWinAmount = gameResult.winAmount
                isLastWin = gameResult.isWin
                delay(500)
                showWinDialog = true
            }.onFailure { error ->
                isRolling = false
                Toast.makeText(
                    context, 
                    error.message ?: strings.headTailToastFailed, 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ---------------- NEW HEAD/TAIL LOOK (visual only; game logic above is unchanged) ----------------
    LaunchedEffect(Unit) { WhSfx.prepare(context) }
    var coinLanded by remember { mutableStateOf(true) }
    LaunchedEffect(isRolling) { if (isRolling) { coinLanded = false; WhSfx.play(WhSfx.S.SPIN, 0.6f) } }
    val showCard = showWinDialog && coinLanded
    LaunchedEffect(showCard) {
        if (showCard) {
            WhSfx.play(if (isLastWin) WhSfx.S.WIN else WhSfx.S.LOSE)
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
        val geo = WhGeo(maxWidth, maxWidth * 1.7917f, topInset + maxWidth * 0.015f)

        HtStage(geo, strings.headTailTitle, strings.headTailSubtitle, u)
        HtRing(geo)
        if (!isRolling && coinLanded && resultText != null) HtResultTag(geo, u, resultText ?: "")
        HtCoin(geo, currentCoinSide == CoinSide.HEAD, isRolling) { coinLanded = true }
        HtHost(geo, clap = showCard && isLastWin)

        WhTopBar(
            u = u,
            mainText = strings.taka(userBalance),
            gameText = strings.taka(gameBalance),
            mainLabel = strings.spinChipMain,
            gameLabel = strings.spinChipGame,
            enabled = !isRolling,
            onBack = onNavigateBack,
            onGame = { isTransferModalVisible = true }
        )

        WhConsole(u, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = u * 4, end = u * 4, bottom = u * 5)) {
            if (!isGameActive) WhClosedBanner(u, strings.headTailClosedBanner)
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(u * 3)) {
                    HtChoiceCard(u, strings.headTailChoiceHead, strings.digits("1.9") + "x", true, selectedSide == CoinSide.HEAD, !isRolling && isGameActive, "headtail_choice_head", Modifier.weight(1f)) {
                        WhSfx.play(WhSfx.S.CLICK, 0.8f); selectedSide = CoinSide.HEAD
                    }
                    HtChoiceCard(u, strings.headTailChoiceTail, strings.digits("1.9") + "x", false, selectedSide == CoinSide.TAIL, !isRolling && isGameActive, "headtail_choice_tail", Modifier.weight(1f)) {
                        WhSfx.play(WhSfx.S.CLICK, 0.8f); selectedSide = CoinSide.TAIL
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = u * 1.2f)) {
                    Text(strings.headTailPayout, fontSize = (u * 2.9f).value.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFE7A0), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    Text(strings.headTailPayout, fontSize = (u * 2.9f).value.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFE7A0), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            WhChipRow(
                u = u,
                options = listOf(1, 3, 5, 8, 10),
                selected = selectedBetAmount.toInt(),
                enabled = !isRolling,
                label = { "৳" + strings.digits(it.toString()) },
                onSelect = { WhSfx.play(WhSfx.S.CLICK, 0.8f); selectedBetAmount = it.toLong() }
            )
            WhSpinButton(
                u = u,
                text = when {
                    !isGameActive -> strings.headTailClosedBanner
                    isRolling -> strings.headTailTossing
                    else -> strings.headTailTossButton
                },
                enabled = !isRolling && gameBalance >= selectedBetAmount && isGameActive,
                bet = selectedBetAmount.toInt(),
                chipLabel = "৳" + strings.digits(selectedBetAmount.toString()),
                onClick = { WhSfx.play(WhSfx.S.CLICK); performFlip() }
            )
        }

        if (showCard) {
            if (isLastWin) WhConfetti()
            Box(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = maxHeight * 0.24f)) {
                WhResultCard(
                    u = u,
                    win = isLastWin,
                    bigWin = false,
                    title = if (isLastWin) strings.mgWinTitle else strings.mgLossTitle,
                    amount = "+" + strings.taka(lastWinAmount),
                    sub = if (isLastWin) (resultText ?: "") else strings.mgLossMessage,
                    onClose = { showWinDialog = false }
                )
            }
        }
    }

    // Modal Overlays
    if (isTransferModalVisible) {
        TransferModal(
            uid = uid,
            userName = userName,
            userPhone = userPhone,
            mainBalance = userBalance,
            gameBalance = gameBalance,
            accentColor = HTGold,
            accentColorLight = HTGoldLight,
            backgroundColor = HTDeepPurple,
            onDismiss = { isTransferModalVisible = false },
            onConfirm = { amount, type ->
                isTransferModalVisible = false
                val database = FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL)
                if (uid.isNotBlank()) {
                    val requestRef = database.getReference("miniGames/transferRequests").push()
                    val requestData = mapOf(
                        "uid" to uid, 
                        "userName" to userName, 
                        "userPhone" to userPhone,
                        "amount" to amount, 
                        "type" to type, 
                        "status" to "PENDING", 
                        "timestamp" to ServerValue.TIMESTAMP,
                        "source" to "HEAD_TAIL"
                    )
                    requestRef.setValue(requestData).addOnCompleteListener { task ->
                        if (task.isSuccessful) Toast.makeText(context, "রিকোয়েস্ট সফলভাবে জমা হয়েছে!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

}
