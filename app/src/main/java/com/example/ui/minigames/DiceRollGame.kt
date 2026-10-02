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
private val DiceNavy = Color(0xFF0F172A)
private val DiceDeepPurple = Color(0xFF1E1B4B)
private val MetallicGold = Color(0xFFFFD700)
private val DiceGoldLight = Color(0xFFFFF7C2)
private val AmberGlow = Color(0xFFFFBF00)
private val DiceRed = Color(0xFFDC143C)
private val DiceGoldDot = Color(0xFFFFD700)

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
    
    val uid = remember { FirebaseManager.getAuth()?.currentUser?.uid ?: "" }
    var gameBalance by remember { mutableStateOf(0.0) }
    var userName by remember { mutableStateOf("") }
    var userPhone by remember { mutableStateOf("") }
    var currentReservePool by remember { mutableStateOf(0.0) }
    var isTransferModalVisible by remember { mutableStateOf(false) }

    // Observers
    DisposableEffect(uid) {
        if (uid.isBlank()) return@DisposableEffect onDispose {}
        val database = FirebaseManager.getDatabase()
        
        val balanceRef = database?.getReference("gameWallets")?.child(uid)?.child("balance")
        val balanceListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                gameBalance = snapshot.value?.toString()?.toDoubleOrNull() ?: 0.0
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        balanceRef?.addValueEventListener(balanceListener)

        val poolRef = database?.getReference("miniGames/vault/reservePool")
        val poolListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentReservePool = snapshot.value?.toString()?.toDoubleOrNull() ?: 0.0
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        poolRef?.addValueEventListener(poolListener)

        val userRef = database?.getReference("users")?.child(uid)
        val userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userName = snapshot.child("name").getValue(String::class.java) ?: "User"
                userPhone = snapshot.child("mobileNumber").getValue(String::class.java) ?: ""
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
    var isRolling by remember { mutableStateOf(false) }
    var selectedChoice by remember { mutableStateOf(DiceChoice.EVEN) }
    var selectedBetAmount by remember { mutableStateOf(1L) }
    var dice1Result by remember { mutableStateOf(5) }
    var dice2Result by remember { mutableStateOf(4) }
    var rollResultText by remember { mutableStateOf<String?>("ভাগ্য পরীক্ষা করতে রোল করুন") }
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
        if (isRolling) return
        if (gameBalance < selectedBetAmount) {
            Toast.makeText(context, "গেম ব্যালেন্স অপর্যাপ্ত", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isRolling = true
            val betAmount = selectedBetAmount.toDouble()
            val database = FirebaseManager.getDatabase() ?: return@launch

            // 1. Zero-Loss Distribution
            val adminCut = betAmount * 0.10
            val poolAddition = betAmount * 0.90

            val distributionSuccess = suspendCancellableCoroutine<Boolean> { continuation ->
                val updates = mapOf(
                    "gameWallets/$uid/balance" to ServerValue.increment(-betAmount),
                    "miniGames/vault/adminProfit" to ServerValue.increment(adminCut),
                    "miniGames/vault/reservePool" to ServerValue.increment(poolAddition)
                )
                database.reference.updateChildren(updates) { error, _ ->
                    continuation.resume(error == null)
                }
            }

            if (!distributionSuccess) {
                isRolling = false
                Toast.makeText(context, "ট্রানজেকশন ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // 2. Pre-determine Outcome (Zero-Loss Guard)
            val potentialWin = betAmount * 1.9
            val canWin = potentialWin <= currentReservePool
            val random = SecureRandom()
            val forceLoss = !canWin
            val isUserWin = if (forceLoss) false else random.nextBoolean()
            
            val finalSum = if (isUserWin) {
                if (selectedChoice == DiceChoice.EVEN) listOf(2, 4, 6, 8, 10, 12).random()
                else listOf(3, 5, 7, 9, 11).random()
            } else {
                if (selectedChoice == DiceChoice.EVEN) listOf(3, 5, 7, 9, 11).random()
                else listOf(2, 4, 6, 8, 10, 12).random()
            }

            val (d1, d2) = when {
                finalSum == 2 -> 1 to 1
                finalSum == 12 -> 6 to 6
                else -> {
                    var v1 = (1..6).random()
                    var v2 = finalSum - v1
                    while (v2 < 1 || v2 > 6) {
                        v1 = (1..6).random()
                        v2 = finalSum - v1
                    }
                    v1 to v2
                }
            }

            // 3. Authentic Casino Tumbling Animation (1.8 Seconds)
            val animationJob = launch {
                // 30 cycles * 60ms = 1800ms
                repeat(30) { i ->
                    // High-speed face cycling
                    dice1Result = (1..6).random()
                    dice2Result = (1..6).random()
                    
                    // Vertical hop & bounce (-50dp to 0dp)
                    val hopTarget = if (i < 24) -50f else -18f
                    launch { 
                        diceTranslationY.animateTo(
                            if (i % 2 == 0) hopTarget else 0f, 
                            tween(60, easing = FastOutLinearInEasing)
                        ) 
                    }

                    // Dynamic Z-wobble (+/- 20 degrees)
                    launch { diceZRotation.animateTo(if (i % 2 == 0) 20f else -20f, tween(60)) }
                    
                    // Squash & stretch scale (0.95f to 1.15f)
                    launch { diceScale.animateTo(if (i % 2 == 0) 1.15f else 0.95f, tween(60)) }
                    
                    delay(60)
                }
            }

            // Wait the full 1.8 seconds (1800ms)
            delay(1800)
            animationJob.cancel()

            // Settle conclusively to pre-determined faces
            dice1Result = d1
            dice2Result = d2
            
            // Decisive physical landing with spring
            launch { diceTranslationY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy)) }
            launch { diceZRotation.animateTo(0f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy)) }
            launch { diceScale.animateTo(1f, spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy)) }

            val totalSum = d1 + d2
            val isEven = (totalSum % 2 == 0)
            rollResultText = "Roll Result: $totalSum (${if (isEven) "Even" else "Odd"})"
            isRolling = false

            // 4. Resolve Win & Distribution
            val winAmount = if (isUserWin) potentialWin else 0.0
            if (winAmount > 0) {
                val winUpdates = mapOf(
                    "gameWallets/$uid/balance" to ServerValue.increment(winAmount),
                    "miniGames/vault/reservePool" to ServerValue.increment(-winAmount)
                )
                database.reference.updateChildren(winUpdates)
            }

            // 5. Log History (Asynchronous)
            val historyRef = database.reference.child("miniGames/history").child(uid).push()
            val historyRecord = mapOf(
                "historyId" to (historyRef.key ?: ""),
                "gameType" to "DICE",
                "betAmount" to betAmount,
                "multiplier" to "1.9x",
                "multiplierVal" to 1.9,
                "winAmount" to winAmount,
                "netProfit" to (winAmount - betAmount),
                "status" to if (winAmount > 0) "WIN" else "LOSS",
                "timestamp" to ServerValue.TIMESTAMP
            )
            historyRef.setValue(historyRecord)

            lastWinAmount = winAmount
            isLastWin = winAmount > 0
            delay(350) // Brief pause to visually absorb the settled dice result
            showWinDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0B0D21), Color(0xFF151838), Color(0xFF1E1538)))
            )
    ) {
        // --- HUD ---
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        Surface(
                            modifier = Modifier.padding(start = 12.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            IconButton(onClick = onNavigateBack, enabled = !isRolling, modifier = Modifier.size(42.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        }
                    },
                    actions = {
                        Row(
                            modifier = Modifier.padding(end = 12.dp),
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
                                border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MetallicGold,
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
                                            "৳${"%.2f".format(userBalance)}",
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
                                    .clickable(enabled = !isRolling) { isTransferModalVisible = true },
                                shape = RoundedCornerShape(22.dp),
                                color = Color(0xFF1E1538).copy(alpha = 0.85f),
                                border = BorderStroke(1.5.dp, MetallicGold)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MetallicGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(verticalArrangement = Arrangement.Center) {
                                        Text(
                                            "Game Wallet",
                                            fontSize = 10.sp,
                                            color = MetallicGold.copy(alpha = 0.85f),
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            "৳${"%.2f".format(gameBalance)}",
                                            fontSize = 13.sp,
                                            color = MetallicGold,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = MetallicGold,
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                
                // --- GRAND CASINO TITLE & DYNAMIC STATUS ---
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "EVEN & ODD",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 30.sp,
                            letterSpacing = 2.sp,
                            brush = Brush.verticalGradient(listOf(DiceGoldLight, MetallicGold, AmberGlow))
                        )
                    )
                    Text(
                        "জোড়-বেজোড়",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MetallicGold.copy(alpha = 0.85f),
                            letterSpacing = 4.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Result / Dynamic Rolling Status
                    if (isRolling) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MetallicGold.copy(alpha = pulseAlpha))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ডাইস ঘুরছে...",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = MetallicGold.copy(alpha = pulseAlpha),
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    } else {
                        Text(
                            text = rollResultText ?: "ভাগ্য পরীক্ষা করতে রোল করুন",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White.copy(alpha = 0.95f),
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- 3D PODIUM STAGE WITH ISOMETRIC DICE & GLOWING AURA ---
                Box(contentAlignment = Alignment.Center) {
                    // Podium Base Layers & Golden Vortex Aura
                    Canvas(modifier = Modifier.size(200.dp)) {
                        val center = this.center
                        // Radiant Ambient Glow Aura
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(MetallicGold.copy(alpha = 0.22f), Color(0xFFFFBF00).copy(alpha = 0.08f), Color.Transparent),
                                radius = size.width / 2f
                            )
                        )
                        // Layered Luxury Metallic Rings
                        drawCircle(MetallicGold.copy(alpha = 0.45f), radius = size.width / 2.6f, style = Stroke(width = 2.dp.toPx()))
                        drawCircle(MetallicGold.copy(alpha = 0.2f), radius = size.width / 2.3f, style = Stroke(width = 1.dp.toPx()))
                        
                        // Podium Surface (Frosted Glass Reflection)
                        drawCircle(
                            brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent)),
                            radius = size.width / 2.8f
                        )
                        
                        // Golden Sparkles Whirlwind (matching reference image)
                        val random = java.util.Random(1337)
                        repeat(16) {
                            val angle = random.nextFloat() * 360f
                            val dist = (size.width / 3.4f) + random.nextFloat() * 26.dp.toPx()
                            val sx = center.x + dist * cos(Math.toRadians(angle.toDouble())).toFloat()
                            val sy = center.y + dist * sin(Math.toRadians(angle.toDouble())).toFloat()
                            val pSize = (1.5f + random.nextFloat() * 2f).dp.toPx()
                            drawCircle(MetallicGold.copy(alpha = 0.7f), radius = pSize, center = Offset(sx, sy))
                        }
                    }

                    // Dual Clean Casino Dice with Dynamic Physical Shadows
                    Row(
                        modifier = Modifier.padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dice 1
                        Box(contentAlignment = Alignment.BottomCenter) {
                            // Dynamic Oval Shadow underneath on podium floor
                            val heightRatio1 = (Math.abs(diceTranslationY.value) / 50f).coerceIn(0f, 1f)
                            val shadowScale1 = (1f - heightRatio1 * 0.45f) * diceScale.value
                            val shadowAlpha1 = (0.45f * (1f - heightRatio1 * 0.65f)).coerceIn(0.1f, 0.45f)
                            Canvas(modifier = Modifier.size(80.dp, 20.dp).offset(y = 12.dp)) {
                                drawOval(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color.Black.copy(alpha = shadowAlpha1), Color.Transparent),
                                        center = center,
                                        radius = size.width / 2f
                                    ),
                                    size = Size(size.width * shadowScale1, size.height * shadowScale1),
                                    topLeft = Offset(center.x - (size.width * shadowScale1) / 2f, center.y - (size.height * shadowScale1) / 2f)
                                )
                            }
                            IsometricCasinoDice(
                                value = dice1Result,
                                modifier = Modifier.graphicsLayer {
                                    this.rotationZ = diceZRotation.value
                                    this.translationY = diceTranslationY.value
                                    this.scaleX = diceScale.value
                                    this.scaleY = diceScale.value
                                }
                            )
                        }

                        // Dice 2
                        Box(contentAlignment = Alignment.BottomCenter) {
                            val heightRatio2 = (Math.abs(diceTranslationY.value * 0.9f) / 50f).coerceIn(0f, 1f)
                            val shadowScale2 = (1f - heightRatio2 * 0.45f) * diceScale.value
                            val shadowAlpha2 = (0.45f * (1f - heightRatio2 * 0.65f)).coerceIn(0.1f, 0.45f)
                            Canvas(modifier = Modifier.size(80.dp, 20.dp).offset(y = 12.dp)) {
                                drawOval(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color.Black.copy(alpha = shadowAlpha2), Color.Transparent),
                                        center = center,
                                        radius = size.width / 2f
                                    ),
                                    size = Size(size.width * shadowScale2, size.height * shadowScale2),
                                    topLeft = Offset(center.x - (size.width * shadowScale2) / 2f, center.y - (size.height * shadowScale2) / 2f)
                                )
                            }
                            IsometricCasinoDice(
                                value = dice2Result,
                                modifier = Modifier.graphicsLayer {
                                    this.rotationZ = -diceZRotation.value * 1.1f
                                    this.translationY = diceTranslationY.value * 0.9f
                                    this.scaleX = diceScale.value
                                    this.scaleY = diceScale.value
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- CHOICE CARDS (EVEN / ODD) ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LuxuryChoiceCard(
                        choice = DiceChoice.EVEN,
                        label = "জোড় (EVEN)",
                        isSelected = selectedChoice == DiceChoice.EVEN,
                        enabled = !isRolling,
                        onSelect = { selectedChoice = it },
                        modifier = Modifier.weight(1f)
                    )
                    LuxuryChoiceCard(
                        choice = DiceChoice.ODD,
                        label = "বেজোড় (ODD)",
                        isSelected = selectedChoice == DiceChoice.ODD,
                        enabled = !isRolling,
                        onSelect = { selectedChoice = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Betting Console
                CasinoBettingConsole(
                    selectedBet = selectedBetAmount.toInt(),
                    onBetSelected = { selectedBetAmount = it.toLong() },
                    actionButtonText = if (gameBalance < selectedBetAmount) "পর্যাপ্ত ব্যালেন্স নেই" else "🎲 রোল করুন (ROLL DICE)",
                    isActionEnabled = !isRolling && gameBalance >= selectedBetAmount,
                    onActionClick = { performRoll() }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Overlays
    if (isTransferModalVisible) {
        TransferModal(
            uid = uid,
            userName = userName,
            mainBalance = userBalance,
            gameBalance = gameBalance,
            accentColor = MetallicGold,
            accentColorLight = DiceGoldLight,
            backgroundColor = DiceDeepPurple,
            onDismiss = { isTransferModalVisible = false },
            onConfirm = { amount, type ->
                isTransferModalVisible = false
                val database = FirebaseManager.getDatabase()
                if (database != null && uid.isNotBlank()) {
                    val requestRef = database.getReference("miniGames/transferRequests").push()
                    val requestData = mapOf(
                        "uid" to uid, "userName" to userName, "userPhone" to userPhone,
                        "amount" to amount, "type" to type, "status" to "PENDING", "timestamp" to ServerValue.TIMESTAMP
                    )
                    requestRef.setValue(requestData).addOnCompleteListener { task ->
                        if (task.isSuccessful) Toast.makeText(context, "রিকোয়েস্ট সফলভাবে জমা হয়েছে!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showWinDialog) {
        WinLossDialog(
            isWin = isLastWin,
            amount = lastWinAmount,
            backgroundColor = DiceDeepPurple,
            accentColor = MetallicGold,
            accentColorLight = DiceGoldLight,
            onDismiss = { showWinDialog = false }
        )
    }
}

/**
 * Clean, Standalone Rounded Casino Dice rendered on Canvas.
 * 72dp x 72dp ruby red cube with smooth rounded corners, specular gloss,
 * and circular gold-rimmed cream dots.
 */
@Composable
fun IsometricCasinoDice(
    value: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(72.dp)) {
        val w = size.width
        val h = size.height
        val cornerRad = 16.dp.toPx()

        // Ruby Red Gradient
        val diceBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFFDC143C), Color(0xFF800000))
        )

        // Draw Base Cube
        drawRoundRect(
            brush = diceBrush,
            size = Size(w, h),
            cornerRadius = CornerRadius(cornerRad, cornerRad)
        )

        // Realistic Gloss: Top-left corner specular highlight curve
        val glossPath = Path().apply {
            moveTo(cornerRad, 4.dp.toPx())
            quadraticTo(4.dp.toPx(), 4.dp.toPx(), 4.dp.toPx(), cornerRad)
            lineTo(4.dp.toPx(), cornerRad + 8.dp.toPx())
            quadraticTo(4.dp.toPx(), 4.dp.toPx(), cornerRad + 8.dp.toPx(), 4.dp.toPx())
            close()
        }
        drawPath(
            path = glossPath,
            color = Color.White.copy(alpha = 0.35f)
        )
        
        // Subtle top gloss gradient
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                startY = 0f,
                endY = h * 0.3f
            ),
            size = Size(w, h),
            cornerRadius = CornerRadius(cornerRad, cornerRad)
        )

        // Pips (Dots)
        val fcx = w / 2f
        val fcy = h / 2f
        val offsetDist = w * 0.28f
        val pipRadius = 5.dp.toPx()

        val dotPositions = mutableListOf<Offset>()
        when (value) {
            1 -> dotPositions.add(Offset(fcx, fcy))
            2 -> {
                dotPositions.add(Offset(fcx + offsetDist, fcy - offsetDist))
                dotPositions.add(Offset(fcx - offsetDist, fcy + offsetDist))
            }
            3 -> {
                dotPositions.add(Offset(fcx + offsetDist, fcy - offsetDist))
                dotPositions.add(Offset(fcx, fcy))
                dotPositions.add(Offset(fcx - offsetDist, fcy + offsetDist))
            }
            4 -> {
                dotPositions.add(Offset(fcx - offsetDist, fcy - offsetDist))
                dotPositions.add(Offset(fcx + offsetDist, fcy - offsetDist))
                dotPositions.add(Offset(fcx - offsetDist, fcy + offsetDist))
                dotPositions.add(Offset(fcx + offsetDist, fcy + offsetDist))
            }
            5 -> {
                dotPositions.add(Offset(fcx - offsetDist, fcy - offsetDist))
                dotPositions.add(Offset(fcx + offsetDist, fcy - offsetDist))
                dotPositions.add(Offset(fcx, fcy))
                dotPositions.add(Offset(fcx - offsetDist, fcy + offsetDist))
                dotPositions.add(Offset(fcx + offsetDist, fcy + offsetDist))
            }
            6 -> {
                dotPositions.add(Offset(fcx - offsetDist, fcy - offsetDist))
                dotPositions.add(Offset(fcx + offsetDist, fcy - offsetDist))
                dotPositions.add(Offset(fcx - offsetDist, fcy))
                dotPositions.add(Offset(fcx + offsetDist, fcy))
                dotPositions.add(Offset(fcx - offsetDist, fcy + offsetDist))
                dotPositions.add(Offset(fcx + offsetDist, fcy + offsetDist))
            }
            else -> dotPositions.add(Offset(fcx, fcy))
        }

        dotPositions.forEach { pos ->
            // Gold rim for dots
            drawCircle(
                color = MetallicGold.copy(alpha = 0.5f),
                radius = pipRadius + 1.dp.toPx(),
                center = pos
            )
            // Cream Pip
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White, Color(0xFFFFFDD0)),
                    center = pos,
                    radius = pipRadius
                ),
                radius = pipRadius,
                center = pos
            )
        }
    }
}

@Composable
fun LuxuryChoiceCard(
    choice: DiceChoice,
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    onSelect: (DiceChoice) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = enabled) { onSelect(choice) },
            color = if (isSelected) Color(0xFF1A1A40) else Color.Black.copy(alpha = 0.4f),
            border = BorderStroke(if (isSelected) 2.5.dp else 1.dp, if (isSelected) MetallicGold else Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1.9x Badge
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    color = MetallicGold,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "1.9x",
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = Color.Black, fontSize = 9.sp)
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (choice == DiceChoice.EVEN) {
                            MiniCasinoDice(2)
                            MiniCasinoDice(4)
                        } else {
                            MiniCasinoDice(1)
                            MiniCasinoDice(3)
                        }
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (isSelected) MetallicGold else Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("Payout: 1.9x", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MiniCasinoDice(val1: Int) {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = RoundedCornerShape(6.dp),
        color = DiceRed,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Box(modifier = Modifier.padding(4.dp)) {
            val dotSize = 4.dp
            val dotColor = DiceGoldDot
            when (val1) {
                1 -> Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor).align(Alignment.Center))
                2 -> {
                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor).align(Alignment.TopEnd))
                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor).align(Alignment.BottomStart))
                }
                3 -> {
                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor).align(Alignment.TopEnd))
                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor).align(Alignment.Center))
                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor).align(Alignment.BottomStart))
                }
                4 -> {
                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor).align(Alignment.TopStart))
                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor).align(Alignment.TopEnd))
                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor).align(Alignment.BottomStart))
                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor).align(Alignment.BottomEnd))
                }
            }
        }
    }
}
