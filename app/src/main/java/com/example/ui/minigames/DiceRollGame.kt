package com.example.ui.minigames

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.core.firebase.FirebaseManager
import com.example.core.i18n.digits
import com.example.core.i18n.taka
import com.example.ui.components.TournamentButton
import com.example.ui.theme.*
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// --- Custom Casino Palette ---
private val DiceDeepPurple = Color(0xFF1E1B4B)
private val MetallicGold = Color(0xFFFFD700)
private val DiceGoldLight = Color(0xFFFFF7C2)

enum class DiceChoice { EVEN, ODD }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiceRollGameScreen(
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
    var isDiceGameActive by remember { mutableStateOf(true) }
    val isGameActive = isMiniGamesEnabled && isDiceGameActive

    // Observers
    DisposableEffect(uid) {
        if (uid.isBlank()) return@DisposableEffect onDispose {}
        val database = com.google.firebase.database.FirebaseDatabase.getInstance(com.example.core.config.FirebaseConfig.DATABASE_URL)
        
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
                isDiceGameActive = snapshot.child("diceGameActive").value?.toString()?.toBoolean() ?: true
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
    var selectedChoice by remember { mutableStateOf(DiceChoice.EVEN) }
    var selectedBetAmount by remember { mutableStateOf(1L) }
    var dice1Result by remember { mutableStateOf(5) }
    var dice2Result by remember { mutableStateOf(4) }
    
    val strings = com.example.core.i18n.LocalAppStrings.current
    
    var rollResultText by remember(strings) { mutableStateOf<String?>(strings.diceInstruction) }
    var showWinDialog by remember { mutableStateOf(false) }
    var lastWinAmount by remember { mutableStateOf(0.0) }
    var isLastWin by remember { mutableStateOf(false) }

    // Animation Physics States
    val diceZRotation = remember { Animatable(0f) }
    val diceTranslationY = remember { Animatable(0f) }
    val diceScale = remember { Animatable(1f) }

    // Pulsating status glow during rolling
    val infiniteTransition = rememberInfiniteTransition(label = "rollingGlow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    fun performRoll() {
        if (isRolling || !isGameActive) return
        if (gameBalance < selectedBetAmount) {
            Toast.makeText(context, strings.diceToastNoBalance, Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            val betAmount = selectedBetAmount.toDouble()
            
            // 1. Authoritative Engine Execution
            val result = MiniGameCoreEngine.playDiceRoll(
                uid = uid,
                stakeAmount = betAmount,
                choice = selectedChoice.name, // "EVEN" or "ODD"
                availableReservePool = currentReservePool
            )

            result.onSuccess { gameResult ->
                isRolling = true
                val d1 = gameResult.dice1
                val d2 = gameResult.dice2

                // 2. Authentic Casino Tumbling Animation (1.8 Seconds)
                val animationJob = launch {
                    repeat(30) { i ->
                        dice1Result = (1..6).random()
                        dice2Result = (1..6).random()
                        
                        val hopTarget = if (i < 24) -50f else -18f
                        launch { 
                            diceTranslationY.animateTo(
                                if (i % 2 == 0) hopTarget else 0f, 
                                tween(60, easing = FastOutLinearInEasing)
                            ) 
                        }
                        launch { diceZRotation.animateTo(if (i % 2 == 0) 20f else -20f, tween(60)) }
                        launch { diceScale.animateTo(if (i % 2 == 0) 1.15f else 0.95f, tween(60)) }
                        delay(60)
                    }
                }

                delay(1800)
                animationJob.cancel()

                // 3. Settle conclusively to pre-determined faces
                dice1Result = d1
                dice2Result = d2
                
                launch { diceTranslationY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy)) }
                launch { diceZRotation.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy)) }
                launch { diceScale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy)) }

                val totalSum = d1 + d2
                val isEven = (totalSum % 2 == 0)
                val typeStr = if (isEven) strings.diceChoiceEven else strings.diceChoiceOdd
                rollResultText = strings.diceRollResult(strings.digits(totalSum.toString()), typeStr)
                isRolling = false

                // 4. Visual Resolution (Financials already committed by Engine)
                lastWinAmount = gameResult.winAmount
                isLastWin = gameResult.isWin
                delay(350)
                showWinDialog = true
            }.onFailure { error ->
                Toast.makeText(context, error.message ?: strings.diceToastFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------------- NEW EVEN/ODD LOOK (visual only; game logic above is unchanged) ----------------
    LaunchedEffect(Unit) { WhSfx.prepare(context); DcSfx.prepare(context) }
    var diceLanded by remember { mutableStateOf(true) }
    LaunchedEffect(isRolling) { if (isRolling) { diceLanded = false; DcSfx.roll() } }
    val showCard = showWinDialog && diceLanded
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
        val geo = WhGeo(maxWidth, maxWidth * 1.7917f, topInset - maxWidth * 0.038f)

        DcStage(geo, strings.diceTitle, strings.diceSubtitle, u)
        if (!isRolling && diceLanded && rollResultText != null && rollResultText != strings.diceInstruction) {
            DcResultTag(geo, u, rollResultText ?: "")
        }
        DcDiceTable(geo, u, dice1Result, dice2Result, isRolling) { diceLanded = true }
        DcHost(geo, clap = showCard && isLastWin)

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
            if (!isGameActive) WhClosedBanner(u, strings.diceClosedBanner)
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(u * 3)) {
                    DcChoiceCard(u, strings.diceChoiceEven, strings.digits("1.9") + "x", 2 to 6, selectedChoice == DiceChoice.EVEN, !isRolling && isGameActive, "dice_choice_even", Modifier.weight(1f)) {
                        WhSfx.play(WhSfx.S.CLICK, 0.8f); selectedChoice = DiceChoice.EVEN
                    }
                    DcChoiceCard(u, strings.diceChoiceOdd, strings.digits("1.9") + "x", 3 to 4, selectedChoice == DiceChoice.ODD, !isRolling && isGameActive, "dice_choice_odd", Modifier.weight(1f)) {
                        WhSfx.play(WhSfx.S.CLICK, 0.8f); selectedChoice = DiceChoice.ODD
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = u * 1.2f)) {
                    Text(strings.dicePayout, fontSize = (u * 2.9f).value.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFE7A0), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    Text(strings.dicePayout, fontSize = (u * 2.9f).value.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFE7A0), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
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
                    !isGameActive -> strings.diceClosedBanner
                    gameBalance < selectedBetAmount -> strings.diceNoBalance
                    isRolling -> strings.diceRolling
                    else -> strings.diceRollButton
                },
                enabled = !isRolling && gameBalance >= selectedBetAmount && isGameActive,
                bet = selectedBetAmount.toInt(),
                chipLabel = "৳" + strings.digits(selectedBetAmount.toString()),
                onClick = { WhSfx.play(WhSfx.S.CLICK); performRoll() }
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
                    sub = if (isLastWin) (rollResultText ?: "") else strings.mgLossMessage,
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
            accentColor = MetallicGold,
            accentColorLight = DiceGoldLight,
            backgroundColor = DiceDeepPurple,
            onDismiss = { isTransferModalVisible = false },
            onConfirm = { amount, type ->
                isTransferModalVisible = false
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
                        "source" to "DICE_ROLL"
                    )
                    requestRef.setValue(requestData).addOnCompleteListener { task ->
                        if (task.isSuccessful) Toast.makeText(context, "রিকোয়েস্ট সফলভাবে জমা হয়েছে!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

}
