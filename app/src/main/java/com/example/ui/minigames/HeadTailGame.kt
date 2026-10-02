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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

// --- Head & Tail Palette ---
private val HTNavy = Color(0xFF0B0D21)
private val HTDeepPurple = Color(0xFF1E1538)
private val HTGold = Color(0xFFFFD700)
private val HTGoldLight = Color(0xFFFFF7C2)
private val HTAmber = Color(0xFFFFBF00)

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
    var selectedSide by remember { mutableStateOf(CoinSide.HEAD) }
    var selectedBetAmount by remember { mutableStateOf(1L) }
    var currentCoinSide by remember { mutableStateOf(CoinSide.HEAD) }
    var resultText by remember { mutableStateOf<String?>(null) }
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
        if (isRolling) return
        if (gameBalance < selectedBetAmount) {
            Toast.makeText(context, "গেম ব্যালেন্স অপর্যাপ্ত", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isRolling = true
            resultText = null
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

            // 2. Determine Outcome
            val potentialWin = betAmount * 1.9
            val canWin = potentialWin <= currentReservePool
            val random = SecureRandom()
            val isUserWin = if (!canWin) false else random.nextBoolean()
            
            val finalSide = if (isUserWin) selectedSide else {
                if (selectedSide == CoinSide.HEAD) CoinSide.TAIL else CoinSide.HEAD
            }

            // 3. Animation (1.8s)
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
            
            // Settle
            currentCoinSide = finalSide
            launch { coinTranslationY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
            launch { coinRotationX.animateTo(if (finalSide == CoinSide.HEAD) 0f else 180f, spring()) }
            
            isRolling = false
            resultText = "ফলফল: ${if (finalSide == CoinSide.HEAD) "হেড" else "টেল"}"

            // 4. Resolve Win
            val winAmount = if (isUserWin) potentialWin else 0.0
            if (winAmount > 0) {
                val winUpdates = mapOf(
                    "gameWallets/$uid/balance" to ServerValue.increment(winAmount),
                    "miniGames/vault/reservePool" to ServerValue.increment(-winAmount)
                )
                database.reference.updateChildren(winUpdates)
            }

            // 5. Log History
            val historyRef = database.reference.child("miniGames/history").child(uid).push()
            val historyRecord = mapOf(
                "historyId" to (historyRef.key ?: ""),
                "gameType" to "HEAD_TAIL",
                "betAmount" to betAmount,
                "multiplier" to "1.9x",
                "winAmount" to winAmount,
                "status" to if (winAmount > 0) "WIN" else "LOSS",
                "timestamp" to ServerValue.TIMESTAMP
            )
            historyRef.setValue(historyRecord)

            lastWinAmount = winAmount
            isLastWin = winAmount > 0
            delay(500)
            showWinDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0D21), Color(0xFF151838), Color(0xFF1E1538))))
    ) {
        // Particles (Simple simulation)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val random = java.util.Random(42)
            repeat(40) {
                drawCircle(
                    color = HTGold.copy(alpha = particleAlpha),
                    radius = random.nextFloat() * 5f,
                    center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
                )
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopBalanceHUD(
                    mainBalance = userBalance,
                    gameWallet = gameBalance,
                    onBack = onNavigateBack,
                    onWalletClick = { isTransferModalVisible = true },
                    isRolling = isRolling
                )
            }
        ) { padding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val minHeight = maxHeight
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = minHeight),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Title
                        Text(
                            "HEAD & TAIL",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp,
                                letterSpacing = 2.sp,
                                brush = Brush.verticalGradient(listOf(HTGoldLight, HTGold, HTAmber))
                            )
                        )
                        Text(
                            "হেড-টেল",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = HTGold.copy(alpha = 0.8f),
                                letterSpacing = 4.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Podium and Coin
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Glass Podium
                            PodiumStage()

                            // Coin
                            Coin3D(
                                side = currentCoinSide,
                                modifier = Modifier.graphicsLayer {
                                    translationY = coinTranslationY.value
                                    rotationX = coinRotationX.value
                                    scaleX = coinScale.value
                                    scaleY = coinScale.value
                                }
                            )
                        }

                        if (isRolling) {
                            Text(
                                "কয়েন ঘুরছে...",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = HTGold
                                ),
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .alpha(tossingAlpha)
                            )
                        } else if (resultText != null) {
                            Text(
                                resultText!!,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(30.dp))
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Selection Cards
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ChoiceCard(
                                side = CoinSide.HEAD,
                                isSelected = selectedSide == CoinSide.HEAD,
                                onClick = { if (!isRolling) selectedSide = it },
                                modifier = Modifier.weight(1f)
                            )
                            ChoiceCard(
                                side = CoinSide.TAIL,
                                isSelected = selectedSide == CoinSide.TAIL,
                                onClick = { if (!isRolling) selectedSide = it },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Betting Console
                        CasinoBettingConsole(
                            selectedBet = selectedBetAmount.toInt(),
                            onBetSelected = { selectedBetAmount = it.toLong() },
                            actionButtonText = if (gameBalance < selectedBetAmount) "ব্যালেন্স নেই" else "টস করুন",
                            isActionEnabled = !isRolling && gameBalance >= selectedBetAmount,
                            onActionClick = { performFlip() }
                        )

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
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
            accentColor = HTGold,
            accentColorLight = HTGoldLight,
            backgroundColor = HTDeepPurple,
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
            backgroundColor = HTDeepPurple,
            accentColor = HTGold,
            accentColorLight = HTGoldLight,
            onDismiss = { showWinDialog = false }
        )
    }
}

@Composable
private fun TopBalanceHUD(
    mainBalance: Double,
    gameWallet: Double,
    onBack: () -> Unit,
    onWalletClick: () -> Unit,
    isRolling: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            enabled = !isRolling,
            modifier = Modifier
                .size(42.dp)
                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Main Balance
            BalancePill(label = "Main Balance", amount = mainBalance, icon = Icons.Default.AccountBalanceWallet)
            // Game Wallet
            BalancePill(
                label = "Game Wallet",
                amount = gameWallet,
                icon = Icons.Default.AccountBalanceWallet,
                isInteractive = true,
                onClick = onWalletClick,
                enabled = !isRolling
            )
        }
    }
}

@Composable
private fun BalancePill(
    label: String,
    amount: Double,
    icon: ImageVector,
    isInteractive: Boolean = false,
    onClick: () -> Unit = {},
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier
            .defaultMinSize(minHeight = 46.dp)
            .wrapContentHeight()
            .then(if (isInteractive) Modifier.clickable(enabled = enabled) { onClick() } else Modifier),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.75f),
        border = BorderStroke(if (isInteractive) 1.5.dp else 1.dp, if (isInteractive) HTGold else Color.White.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = HTGold, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(label, fontSize = 9.sp, color = if (isInteractive) HTGold else Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                Text("৳${"%.2f".format(amount)}", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            if (isInteractive) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = HTGold, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun PodiumStage() {
    Canvas(modifier = Modifier.size(200.dp)) {
        val center = Offset(size.width / 2, size.height / 2 + 25.dp.toPx())
        
        // Bottom Rings
        repeat(3) { i ->
            drawCircle(
                color = HTGold.copy(alpha = 0.3f - i * 0.1f),
                radius = 85.dp.toPx() - (i * 6.dp.toPx()),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        
        // Glass Surface
        drawCircle(
            brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)),
            radius = 80.dp.toPx(),
            center = center
        )
        
        // Inner detail
        drawCircle(
            color = HTGold.copy(alpha = 0.5f),
            radius = 80.dp.toPx(),
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun Coin3D(side: CoinSide, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(120.dp)
            .shadow(20.dp, CircleShape, spotColor = HTGold),
        contentAlignment = Alignment.Center
    ) {
        // We simulate 3D thickness by drawing an offset background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 3.dp)
                .background(Color(0xFF8B4513), CircleShape)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(listOf(HTGoldLight, HTGold, HTAmber)),
                    CircleShape
                )
                .border(3.5.dp, Brush.linearGradient(listOf(HTGoldLight, HTAmber)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Inner rim
            Box(
                modifier = Modifier
                    .fillMaxSize(0.88f)
                    .border(1.5.dp, HTGold.copy(alpha = 0.4f), CircleShape)
            )
            
            // Emblem
            Icon(
                imageVector = if (side == CoinSide.HEAD) Icons.Default.Face else Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFF5D4037).copy(alpha = 0.85f),
                modifier = Modifier.size(62.dp)
            )
            
            // Specular Reflection
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .background(
                        Brush.radialGradient(
                            0.0f to Color.White.copy(alpha = 0.25f),
                            0.6f to Color.Transparent,
                            center = Offset(25f, 25f)
                        ),
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    side: CoinSide,
    isSelected: Boolean,
    onClick: (CoinSide) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) HTGold else Color.White.copy(alpha = 0.15f)
    val bgColor = if (isSelected) Color(0xFF1E1B4B) else Color.Black.copy(alpha = 0.35f)
    
    Surface(
        modifier = modifier
            .height(78.dp)
            .clickable { onClick(side) },
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 2.5.dp else 1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1.9x Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) HTGold else Color.White.copy(alpha = 0.2f)
            ) {
                Text(
                    "1.9x",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) Color.Black else Color.White
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (side == CoinSide.HEAD) Icons.Default.Face else Icons.Default.Star,
                    contentDescription = null,
                    tint = if (isSelected) HTGold else Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(34.dp)
                )
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        if (side == CoinSide.HEAD) "HEAD" else "TAIL",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        if (side == CoinSide.HEAD) "হেড" else "টেল",
                        color = if (isSelected) HTGold else Color.White.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}


