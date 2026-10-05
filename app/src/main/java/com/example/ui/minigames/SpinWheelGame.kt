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
    
    val uid = remember { FirebaseManager.getAuth()?.currentUser?.uid ?: "" }
    var gameBalance by remember { mutableStateOf(0.0) }
    var userPhone by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var isTransferModalVisible by remember { mutableStateOf(false) }
    var currentReservePool by remember { mutableStateOf(0.0) }

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
        onDispose { ref?.removeEventListener(listener) }
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
        val database = FirebaseManager.getDatabase()
        val ref = database?.getReference("users")?.child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userPhone = snapshot.child("mobileNumber").getValue(String::class.java)
                    ?: snapshot.child("phoneNumber").getValue(String::class.java) ?: ""
                userName = snapshot.child("name").getValue(String::class.java)
                    ?: snapshot.child("fullName").getValue(String::class.java) ?: "User"
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref?.addValueEventListener(listener)
        onDispose { ref?.removeEventListener(listener) }
    }
    
    var isSpinning by remember { mutableStateOf(false) }
    val animatedRotation = remember { Animatable(0f) }
    var selectedBetAmount by remember { mutableStateOf(1L) } 
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
        if (isSpinning) return
        if (gameBalance < selectedBetAmount) {
            Toast.makeText(context, "অপর্যাপ্ত ব্যালেন্স", Toast.LENGTH_SHORT).show()
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
                    error.message ?: "ট্রানজেকশন ব্যর্থ হয়েছে", 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF3A1054), Color(0xFF0E0318)),
                    center = Offset.Unspecified,
                    radius = 2000f
                )
            )
    ) {
        // --- STAGE BACKDROP ELEMENTS ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Ambient stage light rays
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.5f to RoyalPurple.copy(alpha = 0.3f),
                    1.0f to Color.Transparent
                )
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        Surface(
                            modifier = Modifier.padding(start = 12.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            IconButton(onClick = onNavigateBack, enabled = !isSpinning, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        }
                    },
                    actions = {
                        Row(
                            modifier = Modifier.padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Redesigned Main Wallet Badge
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF1E1538),
                                border = BorderStroke(1.dp, Color(0x33FFD700))
                            ) {
                                Text(
                                    text = "মেইন: ৳${"%.2f".format(userBalance)}",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                    color = Color.White
                                )
                            }

                            // Redesigned Game Balance as a Full Clickable Casino Button
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(enabled = !isSpinning) { isTransferModalVisible = true },
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.8f),
                                border = BorderStroke(1.5.dp, Color(0xFFFFD700))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .height(44.dp)
                                        .padding(start = 14.dp, end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "গেম: ৳${"%.2f".format(gameBalance)}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        ),
                                        color = CasinoGold
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(CasinoGold),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Add",
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
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
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- CENTERED LUXURY GAME TITLE ---
                Spacer(modifier = Modifier.height(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "MONEY WHEEL",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            letterSpacing = 2.sp,
                            brush = Brush.verticalGradient(listOf(CasinoGoldLight, CasinoGold))
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CasinoGold.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, CasinoGold.copy(alpha = 0.3f))
                    ) {
                        Text(
                            "★ রয়্যাল হুইল ★",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CasinoGold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                // --- THE WHEEL AREA ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Wheel Stand (Tripod)
                    Canvas(modifier = Modifier.size(150.dp, 100.dp).align(Alignment.BottomCenter).offset(y = 20.dp)) {
                        val path = Path().apply {
                            moveTo(size.width / 2, 0f)
                            lineTo(size.width * 0.2f, size.height)
                            lineTo(size.width * 0.8f, size.height)
                            close()
                        }
                        drawPath(path, brush = Brush.verticalGradient(listOf(CasinoGold, CasinoGoldDark)))
                        drawCircle(CasinoGold, radius = 10f, center = Offset(size.width/2, 0f))
                    }

                    // The Main Wheel Container
                    Box(modifier = Modifier.size(340.dp), contentAlignment = Alignment.Center) {
                        // Outer Rim & LED Lights
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val radius = size.width / 2
                            // Rim
                            drawCircle(
                                brush = Brush.sweepGradient(listOf(CasinoGoldDark, CasinoGold, CasinoGoldDark)),
                                radius = radius,
                                style = Stroke(width = 14.dp.toPx())
                            )
                            // Inner Rim Line
                            drawCircle(
                                color = CasinoGoldLight.copy(alpha = 0.5f),
                                radius = radius - 14.dp.toPx(),
                                style = Stroke(width = 1.dp.toPx())
                            )
                            
                            // LED Bulbs (20 bulbs)
                            val ledCount = 20
                            for (i in 0 until ledCount) {
                                val angle = (i * (360f / ledCount)) * (PI / 180).toFloat()
                                val lx = center.x + (radius - 7.dp.toPx()) * cos(angle).toFloat()
                                val ly = center.y + (radius - 7.dp.toPx()) * sin(angle).toFloat()
                                drawCircle(
                                    color = if (i % 2 == 0) Color(0xFFFFF7C2).copy(alpha = ledAlpha) else CasinoGoldLight.copy(alpha = 1f - ledAlpha),
                                    radius = 5.dp.toPx(),
                                    center = Offset(lx, ly)
                                )
                            }
                        }

                        // Top Crown Ornament
                        Canvas(modifier = Modifier.size(64.dp, 44.dp).align(Alignment.TopCenter).offset(y = (-28).dp)) {
                            val path = Path().apply {
                                moveTo(size.width / 2, 0f)
                                quadraticBezierTo(size.width * 0.8f, size.height * 0.5f, size.width, size.height)
                                lineTo(0f, size.height)
                                quadraticBezierTo(size.width * 0.2f, size.height * 0.5f, size.width / 2, 0f)
                            }
                            drawPath(path, brush = Brush.verticalGradient(listOf(CasinoGoldLight, CasinoGold)))
                            drawPath(path, color = Color.White.copy(alpha = 0.3f), style = Stroke(width = 1.dp.toPx()))
                        }

                        // Rotating Slices
                        Canvas(
                            modifier = Modifier
                                .size(308.dp)
                                .rotate(animatedRotation.value)
                        ) {
                            val sliceAngle = 360f / 24f
                            wheelSlices.forEachIndexed { index, slice ->
                                val startAngle = index * sliceAngle - 90f // Start from top
                                drawArc(
                                    color = RoyalPurple,
                                    startAngle = startAngle,
                                    sweepAngle = sliceAngle,
                                    useCenter = true,
                                    size = size
                                )
                                // Spoke lines
                                val lineAngle = startAngle * (PI / 180).toFloat()
                                val lx = center.x + (size.width / 2) * cos(lineAngle).toFloat()
                                val ly = center.y + (size.width / 2) * sin(lineAngle).toFloat()
                                drawLine(CasinoGold.copy(alpha = 0.4f), center, Offset(lx, ly), strokeWidth = 1.dp.toPx())
                                
                                // Text Multiplier Labels
                                val textAngle = (startAngle + sliceAngle / 2) * (PI / 180).toFloat()
                                val radius = size.width * 0.38f
                                val tx = center.x + radius * cos(textAngle).toFloat()
                                val ty = center.y + radius * sin(textAngle).toFloat()
                                
                                drawContext.canvas.nativeCanvas.save()
                                drawContext.canvas.nativeCanvas.rotate(startAngle + sliceAngle / 2 + 90f, tx, ty)
                                drawContext.canvas.nativeCanvas.drawText(
                                    slice.label,
                                    tx,
                                    ty,
                                    android.graphics.Paint().apply {
                                        color = if (slice.multiplier > 0) android.graphics.Color.WHITE else android.graphics.Color.argb(128, 255, 255, 255)
                                        textSize = 13.sp.toPx()
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                                        isAntiAlias = true
                                    }
                                )
                                drawContext.canvas.nativeCanvas.restore()
                            }
                        }

                        // Left Pointer Arrow (Pointing Right/Inward)
                        Canvas(modifier = Modifier.size(44.dp, 34.dp).align(Alignment.CenterStart).offset(x = (-12).dp)) {
                            val path = Path().apply {
                                moveTo(size.width, size.height / 2) // Tip at right
                                lineTo(0f, 0f)
                                lineTo(0f, size.height)
                                close()
                            }
                            drawPath(
                                path = path,
                                brush = Brush.horizontalGradient(listOf(CasinoGold, CasinoGoldLight))
                            )
                            drawPath(path, Color.Black.copy(alpha = 0.4f), style = Stroke(width = 1.dp.toPx()))
                        }

                        // Center Hub Medallion with Specular Reflection
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = CasinoGold,
                            border = BorderStroke(2.dp, CasinoGoldLight),
                            tonalElevation = 12.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Canvas(modifier = Modifier.size(44.dp)) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            0.0f to CasinoGoldLight,
                                            0.6f to CasinoGold,
                                            1.0f to CasinoGoldDark
                                        ),
                                        radius = size.width / 2
                                    )
                                    // Specular Reflection
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.4f),
                                        radius = size.width / 4,
                                        center = Offset(size.width * 0.35f, size.height * 0.35f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Betting Console
                CasinoBettingConsole(
                    selectedBet = selectedBetAmount.toInt(),
                    onBetSelected = { selectedBetAmount = it.toLong() },
                    actionButtonText = if (gameBalance < selectedBetAmount) "অপর্যাপ্ত ব্যালেন্স" else "স্পিন করুন (SPIN)",
                    isActionEnabled = !isSpinning && selectedBetAmount > 0 && gameBalance >= selectedBetAmount,
                    onActionClick = { performBetAndSpin() }
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
            mainBalance = userBalance,
            gameBalance = gameBalance,
            accentColor = CasinoGold,
            accentColorLight = RadiantGold,
            backgroundColor = DeepVelvet,
            onDismiss = { isTransferModalVisible = false },
            onConfirm = { amount, type ->
                // Dismiss dialog immediately
                isTransferModalVisible = false
                
                val database = FirebaseManager.getDatabase()
                if (database != null && uid.isNotBlank()) {
                    val requestRef = database.getReference("miniGames/transferRequests").push()
                    val requestData = mapOf(
                        "uid" to uid,
                        "userName" to userName,
                        "userPhone" to userPhone,
                        "amount" to amount,
                        "type" to type, // ADD or WITHDRAW
                        "status" to "PENDING",
                        "timestamp" to ServerValue.TIMESTAMP
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

    // Win/Result Dialog
    if (showWinDialog) {
        Dialog(onDismissRequest = { showWinDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = DeepVelvet,
                border = BorderStroke(2.dp, CasinoGold)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isWin = lastWinMultiplier > 0
                    Text(
                        text = if (isWin) "অভিনন্দন!" else "দুঃখিত!",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            brush = if (isWin) Brush.verticalGradient(listOf(RadiantGold, CasinoGold)) else null
                        ),
                        color = if (isWin) Color.Unspecified else Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isWin) {
                        Text(
                            text = "আপনি ${lastWinMultiplier}x জিতেছেন!",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "৳ ${"%.2f".format(lastWinAmount)} আপনার ওয়ালেটে যুক্ত হয়েছে।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CasinoGoldLight,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        Text(
                            text = "এবার ভাগ্য সহায় হয়নি। আবার চেষ্টা করুন!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    TournamentButton(
                        text = "বন্ধ করুন",
                        onClick = { showWinDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

