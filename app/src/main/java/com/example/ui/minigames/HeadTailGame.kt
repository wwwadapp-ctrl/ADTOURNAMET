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

                        if (!isGameActive) {
                            Surface(
                                modifier = Modifier.padding(bottom = 12.dp).fillMaxWidth().padding(horizontal = 20.dp),
                                color = Color.Red.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    strings.headTailClosedBanner,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                                    color = Color.Red,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Title
                        Text(
                            strings.headTailTitle,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp,
                                letterSpacing = if (strings == com.example.core.i18n.BengaliStrings) 0.sp else 2.sp,
                                brush = Brush.verticalGradient(listOf(HTGoldLight, HTGold, HTAmber))
                            )
                        )
                        Text(
                            strings.headTailSubtitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = HTGold.copy(alpha = 0.8f),
                                letterSpacing = if (strings == com.example.core.i18n.BengaliStrings) 0.sp else 4.sp,
                                fontSize = 18.sp
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
                                strings.headTailTossing,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = HTGold,
                                    letterSpacing = if (com.example.core.i18n.LocalAppStrings.current == com.example.core.i18n.BengaliStrings) 0.sp else 0.5.sp
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
                            Text(
                                strings.headTailInstruction,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
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
                                onClick = { if (!isRolling && isGameActive) selectedSide = it },
                                modifier = Modifier.weight(1f)
                            )
                            ChoiceCard(
                                side = CoinSide.TAIL,
                                isSelected = selectedSide == CoinSide.TAIL,
                                onClick = { if (!isRolling && isGameActive) selectedSide = it },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Betting Console
                        CasinoBettingConsole(
                            selectedBet = selectedBetAmount.toInt(),
                            onBetSelected = { selectedBetAmount = it.toLong() },
                            actionButtonText = when {
                                !isGameActive -> strings.headTailClosedBanner
                                gameBalance < selectedBetAmount -> strings.headTailToastNoBalance
                                else -> strings.headTailTossButton
                            },
                            isActionEnabled = !isRolling && gameBalance >= selectedBetAmount && isGameActive,
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
            val strings = LocalAppStrings.current
            // Main Balance
            BalancePill(label = strings.diceMainBalanceLabel, amount = mainBalance, icon = Icons.Default.AccountBalanceWallet)
            // Game Wallet
            BalancePill(
                label = strings.diceGameWalletLabel,
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
                val strings = LocalAppStrings.current
                Text(strings.taka(amount), fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
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
                val strings = LocalAppStrings.current
                Text(
                    strings.digits("1.9") + "x",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 9.sp,
                    fontWeight = if (strings == com.example.core.i18n.BengaliStrings) FontWeight.Bold else FontWeight.Black,
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
                val strings = LocalAppStrings.current
                Icon(
                    imageVector = if (side == CoinSide.HEAD) Icons.Default.Face else Icons.Default.Star,
                    contentDescription = null,
                    tint = if (isSelected) HTGold else Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(34.dp)
                )
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        if (side == CoinSide.HEAD) strings.headTailChoiceHead else strings.headTailChoiceTail,
                        color = Color.White,
                        fontWeight = if (strings == com.example.core.i18n.BengaliStrings) FontWeight.Bold else FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = if (strings == com.example.core.i18n.BengaliStrings) 0.sp else 1.sp
                    )
                    Text(
                        strings.headTailPayout,
                        color = if (isSelected) HTGold else Color.White.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}


