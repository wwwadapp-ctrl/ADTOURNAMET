package com.example.ui.autoludo

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import com.example.core.config.FirebaseConfig
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import com.example.ui.components.TournamentButton
import com.example.ui.theme.*
import com.example.ui.minigames.TransferModal
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoLudoScreen(
    user: UserEntity?,
    wallet: WalletEntity?,
    onNavigateBack: () -> Unit,
    onPlayMatch: (String) -> Unit
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCreateMatchDialog by remember { mutableStateOf(false) }
    var showJoinConfirmDialog by remember { mutableStateOf(false) }
    var selectedMatchToJoin by remember { mutableStateOf<AutoLudoMatchEntity?>(null) }
    var activeReadyRoomMatchId by remember { mutableStateOf<String?>(null) }
    var showTransferModal by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("সব ম্যাচ") }
    
    // User Data for TransferModal
    var userName by remember { mutableStateOf(user?.effectiveName ?: if (isPreview) "MD DALUAR" else "User") }
    var userPhone by remember { mutableStateOf(user?.effectiveMobile ?: "") }
    
    // Live Game Wallet Balance
    var gameWalletBalance by remember { mutableDoubleStateOf(if (isPreview) 500.0 else 0.0) }
    val currentUid = if (isPreview) "player_1" else (wallet?.uid ?: wallet?.userId ?: "")

    // Real-time matches with safe initial state
    val liveMatchesResult = if (!isPreview) {
        remember { 
            try {
                AutoLudoManager.observeMatches() 
            } catch (e: Exception) {
                kotlinx.coroutines.flow.flowOf(emptyList<AutoLudoMatchEntity>())
            }
        }.collectAsState(initial = emptyList())
    } else {
        remember { 
            mutableStateOf(listOf(
                AutoLudoMatchEntity(
                    matchId = "preview_1",
                    title = "Auto Ludo || Match No:- 123",
                    entryFee = 50.0,
                    prizePool = 90.0,
                    joinedPlayersCount = 1,
                    status = "WAITING",
                    scheduledTimeFormatted = "Today 07:30 PM"
                )
            ))
        }
    }
    val liveMatches = liveMatchesResult.value

    // Observe specific match for Ready Room
    val activeReadyRoomMatchResult = if (!isPreview) {
        remember(activeReadyRoomMatchId) {
            try {
                AutoLudoManager.observeMatch(activeReadyRoomMatchId ?: "")
            } catch (e: Exception) {
                kotlinx.coroutines.flow.flowOf(null)
            }
        }.collectAsState(initial = null)
    } else {
        remember { mutableStateOf(null) }
    }
    val activeReadyRoomMatch = activeReadyRoomMatchResult.value

    DisposableEffect(currentUid) {
        if (currentUid.isBlank() || isPreview) return@DisposableEffect onDispose {}
        
        val db = try { FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL) } catch (_: Exception) { null }
        if (db == null) return@DisposableEffect onDispose {}
        
        val balanceRef = db.getReference("gameWallets")
            .child(currentUid)
            .child("balance")

        val balanceListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val raw = snapshot.value
                gameWalletBalance = when (raw) {
                    is Number -> raw.toDouble()
                    is String -> raw.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        balanceRef.addValueEventListener(balanceListener)

        // Fetch user name and phone for transfer requests
        val userRef = db.getReference("users").child(currentUid)
        val userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java)
                    ?: snapshot.child("fullName").getValue(String::class.java)
                    ?: snapshot.child("displayName").getValue(String::class.java)
                val phone = snapshot.child("mobileNumber").getValue(String::class.java)
                    ?: snapshot.child("phoneNumber").getValue(String::class.java)
                
                if (name != null) userName = name
                if (phone != null) userPhone = phone
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        userRef.addValueEventListener(userListener)

        onDispose {
            balanceRef.removeEventListener(balanceListener)
            userRef.removeEventListener(userListener)
        }
    }
    
    val filters = listOf("সব ম্যাচ", "সাম্প্রতিক ম্যাচ", "আমার ম্যাচ", "ফ্রি ম্যাচ")
    
    val filteredMatches = when (selectedFilter) {
        "আমার ম্যাচ" -> liveMatches.filter { it.creatorUid == currentUid || it.player1Uid == currentUid || it.player2Uid == currentUid }
        "ফ্রি ম্যাচ" -> liveMatches.filter { it.entryFee == 0.0 }
        else -> liveMatches
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ম্যাচ লিস্ট",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Dual Balance HUD
                        TopBalanceHUD(
                            mainBalance = wallet?.availableAmount ?: (wallet?.balance ?: 0.0),
                            gameWalletBalance = gameWalletBalance,
                            onGameWalletAddClick = { showTransferModal = true }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepNavyBg,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = DeepNavyBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MidnightBackgroundGradient)
                .padding(innerPadding)
        ) {
            // 1. CREATE MATCH BANNER
            CreateMatchBanner(
                onCreateClick = { showCreateMatchDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. FILTER TABS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        onClick = { selectedFilter = filter },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Purple600.copy(alpha = 0.8f) else MidnightNavyCard,
                        border = BorderStroke(1.dp, if (isSelected) Purple400 else Slate800),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) Color.White else Slate400
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. MATCH LIST
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredMatches.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("কোন ম্যাচ পাওয়া যায়নি", color = Slate500)
                        }
                    }
                }
                items(filteredMatches) { match ->
                    AutoLudoMatchCard(
                        match = match,
                        currentUid = currentUid,
                        onJoinClick = {
                            selectedMatchToJoin = match
                            showJoinConfirmDialog = true
                        },
                        onEnterClick = {
                            activeReadyRoomMatchId = match.matchId
                        },
                        onTutorialClick = {
                            Toast.makeText(context, "টিউটোরিয়াল শীঘ্রই আসছে", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    if (showCreateMatchDialog) {
        CreateAutoLudoMatchDialog(
            wallet = wallet,
            gameWalletBalance = gameWalletBalance,
            onDismiss = { showCreateMatchDialog = false },
            onConfirm = { fee, selectedTimeText ->
                showCreateMatchDialog = false
                scope.launch {
                    val res = AutoLudoManager.createMatch(
                        creatorUid = currentUid,
                        creatorName = user?.effectiveName ?: "Player",
                        entryFee = fee.toDouble(),
                        scheduledTimeFormatted = "Today $selectedTimeText"
                    )
                    if (res.isSuccess) {
                        Toast.makeText(context, "ম্যাচ সফলভাবে তৈরি হয়েছে", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "ম্যাচ তৈরিতে ব্যর্থ হয়েছে", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showJoinConfirmDialog && selectedMatchToJoin != null) {
        JoinMatchConfirmDialog(
            match = selectedMatchToJoin!!,
            gameWalletBalance = gameWalletBalance,
            onDismiss = { showJoinConfirmDialog = false },
            onConfirm = {
                showJoinConfirmDialog = false
                scope.launch {
                    val res = AutoLudoManager.joinMatch(
                        matchId = selectedMatchToJoin!!.matchId,
                        playerUid = currentUid,
                        playerName = userName
                    )
                    if (res.isSuccess) {
                        Toast.makeText(context, "ম্যাচে সফলভাবে জয়েন করেছেন!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "ম্যাচে জয়েন হতে ব্যর্থ হয়েছে", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showTransferModal) {
        TransferModal(
            uid = currentUid,
            userName = userName,
            mainBalance = wallet?.availableAmount ?: (wallet?.balance ?: 0.0),
            gameBalance = gameWalletBalance,
            accentColor = Gold400,
            accentColorLight = Color(0xFFFFF7C2),
            backgroundColor = MidnightNavyCard,
            onDismiss = { showTransferModal = false },
            onConfirm = { enteredAmount, selectedType ->
                val requestRef = FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL)
                    .getReference("miniGames/transferRequests").push()
                val requestData = hashMapOf<String, Any?>(
                    "requestId" to requestRef.key,
                    "uid" to currentUid,
                    "userId" to currentUid,
                    "userName" to userName,
                    "userPhone" to userPhone,
                    "amount" to enteredAmount,
                    "type" to selectedType, // "ADD" or "WITHDRAW"
                    "status" to "PENDING",
                    "timestamp" to ServerValue.TIMESTAMP,
                    "source" to "AUTO_LUDO"
                )
                requestRef.setValue(requestData) { err, _ ->
                    if (err == null) {
                        Toast.makeText(context, "অনুরোধটি সফলভাবে অ্যাডমিনের কাছে পাঠানো হয়েছে", Toast.LENGTH_SHORT).show()
                        showTransferModal = false
                    } else {
                        Toast.makeText(context, "অনুরোধ পাঠাতে ব্যর্থ হয়েছে: ${err.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    val currentMatch = activeReadyRoomMatch
    
    if (currentMatch != null) {
        AutoLudoReadyRoomDialog(
            match = currentMatch,
            currentUid = currentUid,
            onDismiss = { activeReadyRoomMatchId = null },
            onReadyClick = { matchId ->
                scope.launch {
                    val res = AutoLudoManager.setPlayerReady(matchId, currentUid)
                    if (res.isFailure) {
                        Toast.makeText(context, "রেডি হতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Auto-navigate to game if status is IN_GAME
    LaunchedEffect(currentMatch?.status) {
        if (currentMatch?.status == "IN_GAME") {
            onPlayMatch(currentMatch.matchId)
            activeReadyRoomMatchId = null
        }
    }
}

@Composable
fun AutoLudoSplashScreen(onFinished: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 3000, easing = LinearEasing),
        label = "ProgressAnimation"
    )

    LaunchedEffect(Unit) {
        progress = 1f
        delay(3000)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = "https://i.postimg.cc/m1FBNgvY",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(32.dp)
                .padding(bottom = 32.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Gold400,
            trackColor = Color(0xFF0D47A1).copy(alpha = 0.3f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun TopBalanceHUD(
    mainBalance: Double,
    gameWalletBalance: Double,
    onGameWalletAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main Balance Pill
        BalancePill(
            label = "Main Balance",
            amount = mainBalance,
            isHighlight = false
        )
// Game Wallet Pill (Gold Luxury Border)
        BalancePill(
            label = "Game Wallet",
            amount = gameWalletBalance,
            isHighlight = true,
            onAddClick = onGameWalletAddClick
        )
    }
}

@Composable
private fun BalancePill(
    label: String,
    amount: Double,
    isHighlight: Boolean,
    onAddClick: (() -> Unit)? = null
) {
    val borderColor = if (isHighlight) Gold400 else Color.White.copy(alpha = 0.15f)
    val borderWidth = if (isHighlight) 1.5.dp else 1.dp

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.85f),
        border = BorderStroke(borderWidth, borderColor),
        modifier = Modifier.heightIn(min = 40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = if (isHighlight) Gold400 else Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (isHighlight) Gold400 else Color(0xFF94A3B8)
                )
                Text(
                    text = "৳${"%.2f".format(amount)}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = Color.White
                )
            }

            if (isHighlight && onAddClick != null) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Gold400)
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Funds",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateMatchBanner(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MidnightNavyCard,
        border = BorderStroke(1.dp, Slate800)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "আপনি নিজের মন মত ম্যাচ তৈরি করতে চাইলে নিচের বাটনে ক্লিক করে তৈরি করুন।",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = Slate300,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            val createBtnGradient = Brush.horizontalGradient(
                listOf(Color(0xFF8B5CF6), Color(0xFFD946EF))
            )
            
            Surface(
                onClick = onCreateClick,
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Box(
                    modifier = Modifier
                        .background(createBtnGradient)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "+ ম্যাচ তৈরি করুন",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoLudoMatchCard(
    match: AutoLudoMatchEntity,
    currentUid: String,
    onJoinClick: () -> Unit,
    onEnterClick: () -> Unit,
    onTutorialClick: () -> Unit
) {
    val isJoined = match.player1Uid == currentUid || match.player2Uid == currentUid
    val isFull = match.joinedPlayersCount >= match.maxPlayers || match.status != "WAITING"
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MidnightNavyCard,
        border = BorderStroke(1.dp, Slate800),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Title & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF0D47A1).copy(alpha = 0.4f),
                    border = BorderStroke(0.5.dp, Slate700)
                ) {
                    Text(
                        text = match.scheduledTimeFormatted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Slate300
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Android,
                    contentDescription = null,
                    tint = Emerald400,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "🤖 এটা সম্পূর্ণ অটো ম্যাচ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Emerald400
                    )
                )

                if (isJoined) {
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Amber500.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, Amber600.copy(alpha = 0.5f))
                    ) {
                        Text(
                            "আপনার ম্যাচ",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = Amber400
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Prize & Entry Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Prize
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0D47A1).copy(alpha = 0.35f),
                    border = BorderStroke(0.8.dp, Gold400.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("প্রাইজ", style = MaterialTheme.typography.labelSmall, color = Slate400)
                        Text("${match.prizePool.toInt()} ৳", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = Gold400)
                    }
                }
                
                // Entry
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0D47A1).copy(alpha = 0.35f),
                    border = BorderStroke(0.8.dp, Slate700)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("এন্ট্রি", style = MaterialTheme.typography.labelSmall, color = Slate400)
                        Text("${match.entryFee.toInt()} ৳", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = Slate200)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val progressText = when {
                        match.status == "READY_COUNTDOWN" -> "ম্যাচ শুরু হচ্ছে..."
                        match.status == "PLAYING" -> "ম্যাচ চলছে"
                        isFull && !isJoined -> "ম্যাচ পূর্ণ হয়েছে"
                        isJoined && match.status == "WAITING" -> "প্রতিপক্ষের জন্য অপেক্ষা"
                        else -> "২ জন হলেই ম্যাচ শুরু হবে"
                    }
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFull) Emerald400 else Rose400
                    )
                    Text(
                        text = "Players: ${match.joinedPlayersCount}/${match.maxPlayers}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                val progressVal = if (match.maxPlayers > 0) match.joinedPlayersCount.toFloat() / match.maxPlayers.toFloat() else 0f
                LinearProgressIndicator(
                    progress = progressVal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isFull) Emerald400 else Rose400,
                    trackColor = Slate800,
                    strokeCap = StrokeCap.Round
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tutorial
                OutlinedButton(
                    onClick = onTutorialClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Slate700),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ⓘ টিউটোরিয়াল", style = MaterialTheme.typography.labelMedium)
                    }
                }
                
                // Button logic
                var buttonText = ""
                var buttonColor = Emerald600
                var isEnabled = true
                var onClickAction: () -> Unit = {}

                when {
                    isJoined && match.status == "WAITING" -> {
                        buttonText = "⏳ অপেক্ষায় আছেন"
                        buttonColor = Amber600.copy(alpha = 0.2f)
                        isEnabled = false
                        onClickAction = { }
                    }
                    isJoined && match.status == "READY_COUNTDOWN" -> {
                        buttonText = "🎮 খেলায় প্রবেশ করুন"
                        buttonColor = Emerald600
                        isEnabled = true
                        onClickAction = { onEnterClick() }
                    }
                    isFull -> {
                        buttonText = "✖ ম্যাচ ফুল"
                        buttonColor = Rose600.copy(alpha = 0.2f)
                        isEnabled = false
                        onClickAction = { }
                    }
                    else -> {
                        buttonText = "● জয়েন করুন"
                        buttonColor = Emerald600
                        isEnabled = true
                        onClickAction = { onJoinClick() }
                    }
                }

                val buttonIcon = when (buttonText) {
                    "● জয়েন করুন" -> Icons.Default.PlayArrow
                    "🎮 খেলায় প্রবেশ করুন" -> Icons.Default.Games
                    "✖ ম্যাচ ফুল" -> Icons.Default.Close
                    else -> Icons.Default.HourglassEmpty
                }

                Button(
                    onClick = { onClickAction() },
                    enabled = isEnabled,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (buttonText == "🎮 খেলায় প্রবেশ করুন") Gold400 else buttonColor,
                        disabledContainerColor = buttonColor,
                        contentColor = if (isEnabled) (if (buttonText == "🎮 খেলায় প্রবেশ করুন") Color(0xFF0D47A1) else Color.White) else Slate400,
                        disabledContentColor = Slate400
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            buttonIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            buttonText,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAutoLudoMatchDialog(
    wallet: WalletEntity?,
    gameWalletBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    var entryFeeText by remember { mutableStateOf("") }
    val entryFee = entryFeeText.toIntOrNull() ?: 0
    val winnings = if (entryFee > 0) (entryFee * 1.8).toInt() else 0

    // Time Selection State
    var selectedHour by remember { mutableStateOf("07") }
    var selectedMinute by remember { mutableStateOf("30") }
    var selectedAmPm by remember { mutableStateOf("PM") }

    val hours = (1..12).map { it.toString().padStart(2, '0') }
    val minutes = (0..55 step 5).map { it.toString().padStart(2, '0') }
    val amPmList = listOf("AM", "PM")
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MidnightNavyCard,
            border = BorderStroke(1.dp, Purple500.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Purple600, Color(0xFF4C1D95)))
                        )
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "নতুন ম্যাচ তৈরি করুন",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                }
                
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Balance Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0D47A1).copy(alpha = 0.4f),
                        border = BorderStroke(0.5.dp, Slate800)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("আপনার বর্তমান ব্যালেন্স", style = MaterialTheme.typography.labelMedium, color = Slate300)
                            Text(
                                "৳ %.2f".format(gameWalletBalance),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Emerald400
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Input Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "এন্ট্রি ফি (10 - 10,000)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate200,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = entryFeeText,
                            onValueChange = { if (it.all { char -> char.isDigit() }) entryFeeText = it },
                            placeholder = { Text("এন্ট্রি ফি দিন", color = Slate500) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Gold400,
                                unfocusedBorderColor = Slate700,
                                focusedContainerColor = Color(0xFF0D47A1).copy(alpha = 0.3f),
                                unfocusedContainerColor = Color(0xFF0D47A1).copy(alpha = 0.3f)
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    // Time Selector
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "ম্যাচ শুরুর সময় (আজ)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate200,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimeDropdown(
                                label = "Hour",
                                options = hours,
                                selected = selectedHour,
                                onSelect = { selectedHour = it },
                                modifier = Modifier.weight(1f)
                            )
                            TimeDropdown(
                                label = "Min",
                                options = minutes,
                                selected = selectedMinute,
                                onSelect = { selectedMinute = it },
                                modifier = Modifier.weight(1f)
                            )
                            TimeDropdown(
                                label = "AM/PM",
                                options = amPmList,
                                selected = selectedAmPm,
                                onSelect = { selectedAmPm = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Prize Calculation
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Gold400.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Gold400.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("উইনিং প্রাইজ:", style = MaterialTheme.typography.labelSmall, color = Gold400)
                                Text("ম্যাচ শেষে আপনার ওয়ালেটে যোগ হবে", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Slate400)
                            }
                            Text(
                                "$winnings ৳",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = Gold400
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Slate700)
                        ) {
                            Text("বাতিল", color = Slate300)
                        }
                        
                        Button(
                            onClick = { 
                                if (entryFee >= 10) {
                                    val timeStr = "$selectedHour:$selectedMinute $selectedAmPm"
                                    onConfirm(entryFee, timeStr)
                                } 
                            },
                            enabled = entryFee >= 10,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Purple600,
                                disabledContainerColor = Purple600.copy(alpha = 0.3f)
                            )
                        ) {
                            Text("নিশ্চিত করুন", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinMatchConfirmDialog(
    match: AutoLudoMatchEntity,
    gameWalletBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MidnightNavyCard,
            border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "ম্যাচে জয়েন করুন",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "এন্ট্রি ফি: ৳ %.2f কাটা হবে".format(match.entryFee),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate300,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0D47A1).copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ব্যালেন্স: ৳ %.2f".format(gameWalletBalance),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Slate700)
                    ) {
                        Text("বাতিল", color = Slate300)
                    }
                    
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                    ) {
                        Text("নিশ্চিত করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Purple400,
                unfocusedBorderColor = Slate700,
                focusedContainerColor = Color(0xFF0D47A1).copy(alpha = 0.2f),
                unfocusedContainerColor = Color(0xFF0D47A1).copy(alpha = 0.2f)
            )
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MidnightNavyCard)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = Color.White) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}


