package com.example.ui.autoludo

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import com.example.core.config.FirebaseConfig
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.BengaliStrings
import com.example.core.i18n.taka
import com.example.core.i18n.takaWhole
import com.example.core.i18n.digits
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
    val strings = LocalAppStrings.current

    var showCreateMatchDialog by remember { mutableStateOf(false) }
    var showJoinConfirmDialog by remember { mutableStateOf(false) }
    var selectedMatchToJoin by remember { mutableStateOf<AutoLudoMatchEntity?>(null) }
    var activeReadyRoomMatchId by remember { mutableStateOf<String?>(null) }
    var showTransferModal by remember { mutableStateOf(false) }
    
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filters = listOf(strings.autoLudoFilterAll, strings.autoLudoFilterRecent, strings.autoLudoFilterMy, strings.autoLudoFilterFree)
    val selectedFilter = filters.getOrElse(selectedFilterIndex) { filters[0] }
    
    // User Data for TransferModal
    val authUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }
    val currentUid = if (isPreview) {
        "player_1"
    } else {
        authUser?.uid?.ifBlank { null }
            ?: user?.uid?.ifBlank { null }
            ?: wallet?.uid?.ifBlank { null } 
            ?: wallet?.userId?.ifBlank { null } 
            ?: ""
    }
    var userName by remember(user) { 
        mutableStateOf(
            user?.effectiveName?.ifBlank { null }
                ?: authUser?.displayName?.ifBlank { null }
                ?: if (isPreview) "MD DALUAR" else "User"
        ) 
    }
    var userPhone by remember(user) { mutableStateOf(user?.effectiveMobile ?: authUser?.phoneNumber ?: "") }
    
    // Immediate sync with passed user object if available
    LaunchedEffect(user) {
        user?.let {
            if (it.effectiveName.isNotBlank() && it.effectiveName != "User" && it.effectiveName != "Player") {
                userName = it.effectiveName
            }
            if (it.effectiveMobile.isNotBlank()) {
                userPhone = it.effectiveMobile
            }
        }
    }
    
    // Live Game Wallet Balance
    var gameWalletBalance by remember { mutableDoubleStateOf(if (isPreview) 500.0 else 0.0) }

    // Real-time matches with safe initial state and timeout fallback
    val liveMatchesResult = if (!isPreview) {
        val matchesState = remember { 
            try {
                AutoLudoManager.observeMatches() 
            } catch (e: Exception) {
                kotlinx.coroutines.flow.flowOf(emptyList<AutoLudoMatchEntity>())
            }
        }.collectAsState(initial = emptyList())
        matchesState
    } else {
        remember { 
            mutableStateOf(listOf(
                AutoLudoMatchEntity(
                    matchId = "preview_1",
                    title = "Auto Ludo || Match No:- 123",
                    matchNumber = "3",
                    entryFee = 50.0,
                    prizePool = 90.0,
                    joinedPlayersCount = 1,
                    status = "WAITING",
                    scheduledTimeFormatted = "Today 05:55 PM"
                )
            ))
        }
    }
    
    val liveMatches = liveMatchesResult.value

    // Auto-Recovery on Match List / App Launch: Cancel expired solo matches & claim refunds
    LaunchedEffect(liveMatches, currentUid) {
        if (!isPreview) {
            while (true) {
                AutoLudoManager.checkAndCancelExpiredSoloMatches(liveMatches, currentUid)
                delay(30_000L)
            }
        }
    }

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
            userRef.removeEventListener(profileValueListener)
        }
    }
    
    val filteredMatches = when (selectedFilter) {
        strings.autoLudoFilterMy -> liveMatches.filter { it.creatorUid == currentUid || it.player1Uid == currentUid || it.player2Uid == currentUid }
        strings.autoLudoFilterFree -> liveMatches.filter { it.entryFee == 0.0 }
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
                            strings.autoLudoHeaderTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 21.sp,
                                brush = Brush.verticalGradient(
                                    listOf(Color(0xFFFFF3C4), Color(0xFFFFC107), Color(0xFFC98A10))
                                ),
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color(0xFF4A3000),
                                    offset = Offset(0f, 3f),
                                    blurRadius = 0f
                                )
                            ),
                            modifier = Modifier.padding(start = 6.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .border(1.dp, Color(0xFFFFE7A0), CircleShape)
                                .padding(1.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xFF2E4BB8), Color(0xFF0F1A52))))
                                .border(2.dp, Color(0xFFE8B23A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = strings.back,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
                filters.forEachIndexed { index, filter ->
                    val isSelected = selectedFilterIndex == index
                    Surface(
                        onClick = { selectedFilterIndex = index },
                        shape = RoundedCornerShape(50),
                        color = Color.Transparent,
                        border = BorderStroke(
                            if (isSelected) 1.8.dp else 1.5.dp,
                            if (isSelected) Color(0xFFFFE7A0) else Color(0xFFB9861C)
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .background(
                                    if (isSelected) {
                                        Brush.verticalGradient(listOf(Color(0xFF9B6BFF), Color(0xFF6A2BD9)))
                                    } else {
                                        Brush.verticalGradient(listOf(Color(0xFF1A2870), Color(0xFF0F1A52)))
                                    }
                                )
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = if (isSelected) Color.White else Color(0xFFC9D3FF),
                                maxLines = 1
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
                            Text(strings.autoLudoNoMatches, color = Slate500)
                        }
                    }
                }
                items(filteredMatches) { match ->
                    AutoLudoMatchCard(
                        match = match,
                        currentUid = currentUid,
                        onJoinClick = {
                            val epoch = if (match.scheduledTimeMillis > 0L) match.scheduledTimeMillis else parseScheduledEpoch(match.scheduledTimeFormatted, match.createdAt)
                            if (epoch > 0L && System.currentTimeMillis() >= epoch) {
                                Toast.makeText(context, strings.autoLudoTimeExpiredToast, Toast.LENGTH_SHORT).show()
                            } else {
                                selectedMatchToJoin = match
                                showJoinConfirmDialog = true
                            }
                        },
                        onEnterClick = {
                            activeReadyRoomMatchId = match.matchId
                        },
                        onResumeClick = {
                            onPlayMatch(match.matchId)
                        },
                        onTutorialClick = {
                            Toast.makeText(context, strings.autoLudoTutorialSoonToast, Toast.LENGTH_SHORT).show()
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
            onConfirm = { fee, selectedTimeText, scheduledEpochMillis ->
                showCreateMatchDialog = false
                scope.launch {
                    val res = AutoLudoManager.createMatch(
                        creatorUid = currentUid,
                        creatorName = user?.effectiveName ?: "Player",
                        entryFee = fee.toDouble(),
                        scheduledTimeFormatted = "Today $selectedTimeText",
                        scheduledTimeMillis = scheduledEpochMillis
                    )
                    if (res.isSuccess) {
                        Toast.makeText(context, strings.autoLudoMatchCreatedSuccess, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: strings.autoLudoMatchCreatedFail, Toast.LENGTH_LONG).show()
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
                val targetMatchId = selectedMatchToJoin!!.matchId
                scope.launch {
                    val res = AutoLudoManager.joinMatch(
                        matchId = targetMatchId,
                        playerUid = currentUid,
                        playerName = userName
                    )
                    if (res.isSuccess) {
                        Toast.makeText(context, strings.autoLudoJoinSuccessToast, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, res.exceptionOrNull()?.message ?: strings.autoLudoJoinFailToast, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    if (showTransferModal) {
        TransferModal(
            uid = currentUid,
            userName = userName,
            userPhone = userPhone,
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
                        Toast.makeText(context, strings.autoLudoTransferReqSuccess, Toast.LENGTH_SHORT).show()
                        showTransferModal = false
                    } else {
                        Toast.makeText(context, strings.autoLudoTransferReqFail(err.message ?: ""), Toast.LENGTH_SHORT).show()
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
                    if (res.isFailure || matchId.contains("mock") || matchId.contains("preview")) {
                        onPlayMatch(matchId)
                        activeReadyRoomMatchId = null
                    }
                }
            }
        )
    }

    // Auto-navigate to game or dismiss lobby based on status
    LaunchedEffect(currentMatch?.status) {
        when (currentMatch?.status) {
            "IN_GAME" -> {
                onPlayMatch(currentMatch.matchId)
                activeReadyRoomMatchId = null
            }
            "COMPLETED" -> {
                if (activeReadyRoomMatchId != null) {
                    Toast.makeText(context, strings.autoLudoMatchEndedToast, Toast.LENGTH_LONG).show()
                    activeReadyRoomMatchId = null
                }
            }
            "CANCELLED" -> {
                if (activeReadyRoomMatchId != null) {
                    val cancelMatchId = activeReadyRoomMatchId
                    if (!cancelMatchId.isNullOrBlank() && currentUid.isNotBlank()) {
                        AutoLudoManager.claimRefund(cancelMatchId, currentUid) {
                            // Successfully claimed
                        }
                    }
                    val actionLog = currentMatch.gameState.lastActionLog
                    val msg = if (actionLog.contains("Opponent", ignoreCase = true)) {
                        strings.autoLudoCancelOpponentMissing
                    } else {
                        strings.autoLudoCancelBothNotReady
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    activeReadyRoomMatchId = null
                }
            }
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
    val strings = LocalAppStrings.current
    Row(
        modifier = modifier.padding(end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main Balance Pill
        BalancePill(
            label = strings.spinChipMain,
            amount = mainBalance,
            isHighlight = false
        )
        // Game Wallet Pill (Gold Luxury Border)
        BalancePill(
            label = strings.spinChipGame,
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
    val strings = LocalAppStrings.current
    val shape = RoundedCornerShape(14.dp)
    val outerColor = if (isHighlight) Color(0xFFFFF3C4) else Color(0xFFFFE7A0)
    val innerColor = if (isHighlight) Color(0xFFFFC107) else Color(0xFFB9861C)

    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .border(1.dp, outerColor, shape)
            .padding(1.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF1E2E7E), Color(0xFF0F1A52))))
            .border(if (isHighlight) 1.8.dp else 1.5.dp, innerColor, shape)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = if (isHighlight) Color(0xFFFFC107) else Color(0xFFFFD27A),
                modifier = Modifier.size(16.dp)
            )
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color(0xFFFFD27A),
                    maxLines = 1
                )
                Text(
                    text = strings.taka(amount),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = if (strings === BengaliStrings) 0.sp else 0.5.sp
                    ),
                    color = Color.White,
                    maxLines = 1
                )
            }

            if (isHighlight && onAddClick != null) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFFFF3C4), Color(0xFFFFC107), Color(0xFFC98A10))
                            )
                        )
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = label,
                        tint = Color(0xFF3A2300),
                        modifier = Modifier.size(18.dp)
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
    val strings = LocalAppStrings.current
    val frameShape = RoundedCornerShape(18.dp)
    val hexShape = GenericShape { size, _ ->
        moveTo(size.width * 0.06f, 0f)
        lineTo(size.width * 0.94f, 0f)
        lineTo(size.width, size.height / 2f)
        lineTo(size.width * 0.94f, size.height)
        lineTo(size.width * 0.06f, size.height)
        lineTo(0f, size.height / 2f)
        close()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(1.5.dp, Color(0xFFFFE7A0), frameShape)
            .padding(1.5.dp)
            .clip(frameShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E2E7E), Color(0xFF131E5A), Color(0xFF0D1648))
                )
            )
            .border(2.5.dp, Color(0xFFB9861C), frameShape)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(6.dp)
                .border(1.dp, Color(0xB3F0BE50), RoundedCornerShape(13.dp))
        )
        BannerCornerOrnament(Modifier.align(Alignment.TopStart).padding(8.dp), flipX = false, flipY = false)
        BannerCornerOrnament(Modifier.align(Alignment.TopEnd).padding(8.dp), flipX = true, flipY = false)
        BannerCornerOrnament(Modifier.align(Alignment.BottomStart).padding(8.dp), flipX = false, flipY = true)
        BannerCornerOrnament(Modifier.align(Alignment.BottomEnd).padding(8.dp), flipX = true, flipY = true)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.autoLudoCreateBannerTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFFF3C4), Color(0xFFFFC107), Color(0xFFC98A10))
                    ),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = strings.autoLudoCreateBannerText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 14.sp,
                    color = Color(0xFFE6ECFF),
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp
                ),
                modifier = Modifier.padding(bottom = 14.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .height(52.dp)
                    .clip(hexShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFC98A10))))
                    .clickable { onCreateClick() }
                    .padding(2.5.dp)
                    .clip(hexShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFE36BFF), Color(0xFFA21CC9), Color(0xFF6B0A8A))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                        .fillMaxWidth(0.84f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0f))
                            )
                        )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        strings.autoLudoCreateButton,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color.White,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color(0x993C0050),
                                offset = Offset(0f, 3f),
                                blurRadius = 0f
                            )
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun BannerCornerOrnament(
    modifier: Modifier = Modifier,
    flipX: Boolean,
    flipY: Boolean
) {
    Canvas(
        modifier = modifier
            .size(22.dp)
            .graphicsLayer {
                scaleX = if (flipX) -1f else 1f
                scaleY = if (flipY) -1f else 1f
            }
    ) {
        val s = size.width / 22f
        val path = Path().apply {
            moveTo(20f * s, 2f * s)
            cubicTo(10f * s, 2f * s, 2f * s, 10f * s, 2f * s, 20f * s)
            moveTo(13f * s, 2f * s)
            cubicTo(13f * s, 10f * s, 6f * s, 10f * s, 7f * s, 7f * s)
            moveTo(2f * s, 13f * s)
            cubicTo(10f * s, 13f * s, 10f * s, 6f * s, 7f * s, 7f * s)
        }
        drawPath(
            path = path,
            color = Color(0xFFE8B23A),
            style = Stroke(width = 1.4f * s, cap = StrokeCap.Round)
        )
        drawCircle(
            color = Color(0xFFFFE08A),
            radius = 1.7f * s,
            center = Offset(7f * s, 7f * s)
        )
    }
}

private fun parseScheduledEpoch(formatted: String, createdAt: Long = 0L): Long {
    return try {
        val clean = formatted.replace("Today", "").replace("Tomorrow", "").trim()
        val parts = clean.split(" ")
        val timeParts = parts[0].split(":")
        val h = timeParts[0].toInt()
        val m = timeParts[1].toInt()
        val amPm = parts.getOrNull(1) ?: "PM"

        val hour24 = when {
            amPm.equals("AM", ignoreCase = true) -> if (h == 12) 0 else h
            else -> if (h == 12) 12 else h + 12
        }

        val cal = java.util.Calendar.getInstance()
        if (createdAt > 0L) {
            cal.timeInMillis = createdAt
        }
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour24)
        cal.set(java.util.Calendar.MINUTE, m)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    } catch (e: Exception) {
        0L
    }
}

@Composable
private fun AutoLudoMatchCard(
    match: AutoLudoMatchEntity,
    currentUid: String,
    onJoinClick: () -> Unit,
    onEnterClick: () -> Unit,
    onResumeClick: () -> Unit = {},
    onTutorialClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isJoined = match.player1Uid == currentUid || match.player2Uid == currentUid
    val isFull = match.joinedPlayersCount >= match.maxPlayers || match.status != "WAITING"
    
    // Live reactive countdown ticker until match scheduled epoch
    val targetEpoch = remember(match.scheduledTimeMillis, match.scheduledTimeFormatted, match.createdAt) {
        if (match.scheduledTimeMillis > 0L) match.scheduledTimeMillis
        else parseScheduledEpoch(match.scheduledTimeFormatted, match.createdAt)
    }
    var remainingMillis by remember(targetEpoch) {
        mutableLongStateOf(if (targetEpoch > 0L) (targetEpoch - System.currentTimeMillis()).coerceAtLeast(0L) else 0L)
    }
    val isExpired = targetEpoch > 0L && remainingMillis <= 0L
    val isMissingOpponent = (match.status == "CANCELLED" && match.joinedPlayersCount < 2) || (match.joinedPlayersCount < match.maxPlayers && isExpired)
    val isInactivityCancelled = match.status == "CANCELLED" && match.joinedPlayersCount >= 2

    LaunchedEffect(targetEpoch, match.status, match.joinedPlayersCount) {
        if (targetEpoch <= 0L) return@LaunchedEffect
        val initialDiff = targetEpoch - System.currentTimeMillis()
        if (initialDiff <= 0L) {
            remainingMillis = 0L
            if (match.status == "WAITING" && match.joinedPlayersCount < 2) {
                AutoLudoManager.cancelExpiredSoloMatch(match)
            }
            return@LaunchedEffect
        }
        while (true) {
            val diff = targetEpoch - System.currentTimeMillis()
            remainingMillis = diff.coerceAtLeast(0L)
            if (remainingMillis <= 0L) {
                if (match.status == "WAITING" && match.joinedPlayersCount < 2) {
                    AutoLudoManager.cancelExpiredSoloMatch(match)
                }
                break
            }
            delay(1000L)
        }
    }

    val matchNum = match.matchNumber.ifBlank {
        val regex = Regex("\\d+")
        val matchResult = regex.find(match.title)
        matchResult?.value ?: "1"
    }
    val formattedMatchNum = strings.digits(matchNum)
    val displayTitle = strings.autoLudoCardTitle(formattedMatchNum)

    val formattedTime = remember(match.scheduledTimeFormatted, strings) {
        val raw = match.scheduledTimeFormatted
        if (raw.startsWith("Today", ignoreCase = true)) {
            val timePart = raw.replace("Today", "", ignoreCase = true).trim()
            val digitsTime = strings.digits(timePart)
            if (strings === BengaliStrings) "আজ $digitsTime" else "Today $timePart"
        } else if (raw.startsWith("Tomorrow", ignoreCase = true)) {
            val timePart = raw.replace("Tomorrow", "", ignoreCase = true).trim()
            val digitsTime = strings.digits(timePart)
            if (strings === BengaliStrings) "আগামীকাল $digitsTime" else "Tomorrow $timePart"
        } else {
            strings.digits(raw)
        }
    }
    
    val cardShape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFFFFE7A0), cardShape)
            .padding(1.5.dp)
            .clip(cardShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E2E7E), Color(0xFF131E5A), Color(0xFF0D1648))
                )
            )
            .border(2.5.dp, Color(0xFFB9861C), cardShape)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(6.dp)
                .border(1.dp, Color(0xB3F0BE50), RoundedCornerShape(13.dp))
        )
        BannerCornerOrnament(Modifier.align(Alignment.TopStart).padding(8.dp), flipX = false, flipY = false)
        BannerCornerOrnament(Modifier.align(Alignment.TopEnd).padding(8.dp), flipX = true, flipY = false)
        BannerCornerOrnament(Modifier.align(Alignment.BottomStart).padding(8.dp), flipX = false, flipY = true)
        BannerCornerOrnament(Modifier.align(Alignment.BottomEnd).padding(8.dp), flipX = true, flipY = true)

        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp)) {
            // Top Row: Title & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        val parts = displayTitle.split(" · ", limit = 2)
                        if (parts.size == 2) {
                            withStyle(SpanStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)) {
                                append(parts[0] + " · ")
                            }
                            withStyle(
                                SpanStyle(
                                    brush = Brush.verticalGradient(
                                        listOf(Color(0xFFFFF3C4), Color(0xFFFFC107), Color(0xFFC98A10))
                                    ),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            ) {
                                append(parts[1])
                            }
                        } else {
                            withStyle(SpanStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)) {
                                append(displayTitle)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFB547), Color(0xFFFF6A00))))
                        .border(1.dp, Color(0xFFFFE0B0), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val autoTagShimmer = rememberInfiniteTransition(label = "autoTagShimmer")
                val autoTagShimmerX by autoTagShimmer.animateFloat(
                    initialValue = -1f,
                    targetValue = 2f,
                    animationSpec = infiniteRepeatable(tween(durationMillis = 3000, easing = LinearEasing)),
                    label = "autoTagShimmerX"
                )
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0E7A45), Color(0xFF16B85A), Color(0xFF0E7A45))
                            )
                        )
                        .border(1.dp, Color(0xFF8CF5C0), RoundedCornerShape(50))
                        .drawWithContent {
                            drawContent()
                            val w = size.width
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                                    start = Offset(w * autoTagShimmerX - w * 0.3f, 0f),
                                    end = Offset(w * autoTagShimmerX + w * 0.3f, size.height)
                                )
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = strings.autoLudoAutoMatchBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color(0xFFEFFFF5),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isJoined) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFC107).copy(alpha = 0.10f))
                            .border(1.dp, Color(0xFFB9861C), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = strings.autoLudoMyMatchBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFFFD27A),
                            maxLines = 1
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
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFB88A2C), Color(0xFF6E4F14), Color(0xFF4A3410))
                            )
                        )
                        .border(1.5.dp, Color(0xFFFFE7A0), RoundedCornerShape(12.dp))
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        strings.autoLudoPrizeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = Color(0xFFF3E6C0),
                        maxLines = 1
                    )
                    Text(
                        strings.takaWhole(match.prizePool),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
                            fontSize = 19.sp,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color(0xFF3A2600),
                                offset = Offset(0f, 3f),
                                blurRadius = 0f
                            )
                        ),
                        color = Color(0xFFFFF3C4),
                        maxLines = 1
                    )
                }

                // Entry
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF1E2E7E), Color(0xFF0F1A52))))
                        .border(1.5.dp, Color(0xFFB9861C), RoundedCornerShape(12.dp))
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        strings.autoLudoEntryLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = Color(0xFFF3E6C0),
                        maxLines = 1
                    )
                    Text(
                        strings.takaWhole(match.entryFee),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
                            fontSize = 19.sp
                        ),
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                val isRoomOpen = isJoined &&
                    match.joinedPlayersCount >= match.maxPlayers &&
                    (match.status == "WAITING" || match.status == "READY_COUNTDOWN") &&
                    targetEpoch > 0L && remainingMillis <= 0L

                val (progressText, statusKind) = when {
                    match.status == "COMPLETED" -> strings.autoLudoMatchFinished to "done"
                    isMissingOpponent && !isJoined -> strings.autoLudoStatusTimeUp to "bad"
                    isMissingOpponent -> strings.autoLudoStatusOpponentMissing to "bad"
                    isInactivityCancelled -> strings.autoLudoStatusPlayerAbsent to "bad"
                    isRoomOpen -> strings.autoLudoStatusRoomOpen to "ready"
                    match.status == "READY_COUNTDOWN" -> strings.autoLudoStatusStarting to "ready"
                    match.status == "PLAYING" || match.status == "IN_GAME" -> strings.autoLudoStatusPlaying to "live"
                    isFull && !isJoined -> strings.autoLudoStatusFull to "full"
                    isJoined && match.status == "WAITING" && match.joinedPlayersCount >= match.maxPlayers -> strings.autoLudoStatusStarting to "ready"
                    isJoined && match.status == "WAITING" -> strings.autoLudoStatusWaitingOpponent to "wait"
                    else -> strings.autoLudoStatusSpotsLeft((match.maxPlayers - match.joinedPlayersCount).coerceAtLeast(1)) to "wait"
                }

                val (pillText, pillBg, pillBorder) = when (statusKind) {
                    "wait" -> Triple(Color(0xFFFFD27A), Color(0xFFFFC107).copy(alpha = 0.10f), Color(0xFFE8B23A).copy(alpha = 0.60f))
                    "live", "ready" -> Triple(Color(0xFF5BF0A0), Color(0xFF1EDC6E).copy(alpha = 0.10f), Color(0xFF5BF0A0).copy(alpha = 0.50f))
                    "full" -> Triple(Color(0xFFC8B8FF), Color(0xFFA082FF).copy(alpha = 0.12f), Color(0xFFA082FF).copy(alpha = 0.50f))
                    "bad" -> Triple(Color(0xFFFFB4A8), Color(0xFFFF645A).copy(alpha = 0.10f), Color(0xFFFF7864).copy(alpha = 0.45f))
                    else -> Triple(Color(0xFFC9D3FF), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.15f))
                }
                val blinkMs = when (statusKind) {
                    "wait" -> 1000
                    "live" -> 800
                    "ready" -> 600
                    else -> 0
                }
                val statusBlink = rememberInfiniteTransition(label = "statusDotBlink")
                val statusDotAlpha by statusBlink.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.25f,
                    animationSpec = infiniteRepeatable(
                        tween(durationMillis = if (blinkMs > 0) blinkMs / 2 else 500),
                        RepeatMode.Reverse
                    ),
                    label = "statusDotAlpha"
                )
                val dotAlpha = if (blinkMs > 0) statusDotAlpha else 1f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(50))
                            .background(pillBg)
                            .border(1.dp, pillBorder, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .graphicsLayer { alpha = dotAlpha }
                                .clip(CircleShape)
                                .background(pillText)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = pillText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.autoLudoPlayersCount(match.joinedPlayersCount, match.maxPlayers),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color(0xFFFFE9A8),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val progressVal = if (match.maxPlayers > 0) match.joinedPlayersCount.toFloat() / match.maxPlayers.toFloat() else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF0A1238))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressVal.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF7CFFB0), Color(0xFF16C060), Color(0xFF0B8A42))
                                )
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action Buttons
            val hasRefund = match.refundClaimed[currentUid] == true
            var btnLine1 = ""
            var btnLine2 = ""
            var btnStyle = "gold"
            var hintText = ""
            var isEnabled = true
            var onClickAction: () -> Unit = {}

            when {
                isMissingOpponent || isInactivityCancelled -> {
                    if (isJoined) {
                        btnLine1 = strings.autoLudoBtnCancelled
                        btnLine2 = if (hasRefund) strings.autoLudoBtnRefundedSub(strings.takaWhole(match.entryFee)) else strings.autoLudoBtnRefundPendingSub
                        if (hasRefund) hintText = strings.autoLudoHintRefunded
                    } else {
                        btnLine1 = strings.autoLudoBtnTimeUp
                        btnLine2 = strings.autoLudoBtnTimeUpSub
                    }
                    btnStyle = "red"
                    isEnabled = false
                }
                match.status == "COMPLETED" -> {
                    when {
                        match.winnerUid == currentUid -> {
                            btnLine1 = strings.autoLudoBtnWon
                            btnLine2 = if (match.prizeClaimed) strings.autoLudoBtnWonSub(strings.takaWhole(match.prizePool)) else strings.autoLudoBtnWonPendingSub
                            btnStyle = "gold"
                        }
                        isJoined -> {
                            btnLine1 = strings.autoLudoBtnLost
                            btnLine2 = strings.autoLudoBtnLostSub
                            btnStyle = "dim"
                        }
                        else -> {
                            btnLine1 = strings.autoLudoMatchFinished
                            btnLine2 = ""
                            btnStyle = "dim"
                        }
                    }
                    isEnabled = false
                }
                isJoined && (match.status == "IN_GAME" || match.status == "PLAYING") -> {
                    btnLine1 = strings.autoLudoBtnResume
                    btnLine2 = strings.autoLudoBtnResumeSub
                    btnStyle = "green"
                    isEnabled = true
                    onClickAction = onResumeClick
                }
                isJoined && match.status == "WAITING" && match.joinedPlayersCount < match.maxPlayers -> {
                    btnLine1 = strings.autoLudoBtnWaitOpp
                    btnLine2 = strings.autoLudoBtnWaitOppSub
                    btnStyle = "wait"
                    if (match.creatorUid == currentUid) hintText = strings.autoLudoHintCreatorWaiting
                    isEnabled = false
                }
                isJoined && (match.status == "READY_COUNTDOWN" || match.joinedPlayersCount >= match.maxPlayers) -> {
                    if (remainingMillis > 0L) {
                        val sec = (remainingMillis / 1000) % 60
                        val min = (remainingMillis / (1000 * 60)) % 60
                        val hrs = remainingMillis / (1000 * 60 * 60)
                        val timeStr = if (hrs > 0) {
                            String.format("%02d:%02d:%02d", hrs, min, sec)
                        } else {
                            String.format("%02d:%02d", min, sec)
                        }
                        btnLine1 = strings.autoLudoBtnCountdown(strings.digits(timeStr))
                        btnLine2 = strings.autoLudoBtnCountdownSub
                        btnStyle = "count"
                        hintText = strings.autoLudoHintCountdown
                        isEnabled = false
                    } else {
                        btnLine1 = strings.autoLudoBtnEnter
                        btnLine2 = strings.autoLudoBtnEnterSub
                        btnStyle = "enter"
                        isEnabled = true
                        onClickAction = { onEnterClick() }
                    }
                }
                isFull -> {
                    btnLine1 = strings.autoLudoBtnFull
                    btnLine2 = strings.autoLudoBtnFullSub
                    btnStyle = "full"
                    hintText = strings.autoLudoHintFull
                    isEnabled = false
                }
                isExpired -> {
                    btnLine1 = strings.autoLudoBtnTimeUp
                    btnLine2 = strings.autoLudoBtnTimeUpSub
                    btnStyle = "red"
                    isEnabled = false
                }
                else -> {
                    btnLine1 = strings.autoLudoBtnJoin
                    btnLine2 = strings.autoLudoBtnJoinSub(strings.takaWhole(match.entryFee), strings.takaWhole(match.prizePool))
                    btnStyle = "gold"
                    if (match.joinedPlayersCount == match.maxPlayers - 1) hintText = strings.autoLudoHintJoin
                    isEnabled = true
                    onClickAction = { onJoinClick() }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tutorial
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF1E2E7E), Color(0xFF0F1A52))))
                        .border(1.5.dp, Color(0xFFB9861C), RoundedCornerShape(12.dp))
                        .clickable { onTutorialClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color(0xFFFFF3C4), Color(0xFFFFC107), Color(0xFFC98A10))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "i",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = Color(0xFF3A2300)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                strings.autoLudoTutorialButton,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Color(0xFFFFD27A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            strings.autoLudoBtnTutorialSub,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFFFD27A).copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }
                }

                LobbyCardButton(
                    line1 = btnLine1,
                    line2 = btnLine2,
                    style = btnStyle,
                    enabled = isEnabled,
                    onClick = { onClickAction() },
                    modifier = Modifier.weight(1f)
                )
            }

            if (hintText.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = hintText,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    ),
                    color = Color(0xFFFFE9A8)
                )
            }
        }
    }
}

@Composable
private fun LobbyCardButton(
    line1: String,
    line2: String,
    style: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val anim = rememberInfiniteTransition(label = "lobbyBtnAnim")
    val shimmerX by anim.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2400, easing = LinearEasing)),
        label = "lobbyBtnShimmer"
    )
    val pulse by anim.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 600), RepeatMode.Reverse),
        label = "lobbyBtnPulse"
    )
    val blink by anim.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 700), RepeatMode.Reverse),
        label = "lobbyBtnBlink"
    )

    val textColor: Color
    val bg: Brush
    val borderColor: Color
    when (style) {
        "gold" -> {
            textColor = Color(0xFF3A2300)
            bg = Brush.verticalGradient(listOf(Color(0xFFFFF4C2), Color(0xFFF6C84E), Color(0xFFD99A16)))
            borderColor = Color(0xFFFFF3C4)
        }
        "green", "enter" -> {
            textColor = Color.White
            bg = Brush.verticalGradient(listOf(Color(0xFF5BF0A0), Color(0xFF16B85A), Color(0xFF0B7A3C)))
            borderColor = if (style == "enter") Color(0xFFC8FFE0).copy(alpha = pulse) else Color(0xFFC8FFE0)
        }
        "wait" -> {
            textColor = Color(0xFFFFE7A0)
            bg = Brush.verticalGradient(listOf(Color(0xFF3A2E10), Color(0xFF251C08)))
            borderColor = Color(0xFFE8B23A).copy(alpha = 0.7f)
        }
        "count" -> {
            textColor = Color(0xFFFFF3C4)
            bg = Brush.verticalGradient(listOf(Color(0xFF5A3A08), Color(0xFF2E1E04)))
            borderColor = Color(0xFFFFC107)
        }
        "full" -> {
            textColor = Color(0xFFC8B8FF)
            bg = Brush.verticalGradient(listOf(Color(0xFF2A1E5E), Color(0xFF1A1240)))
            borderColor = Color(0xFFA082FF).copy(alpha = 0.5f)
        }
        "red" -> {
            textColor = Color(0xFFFFB4A8)
            bg = Brush.verticalGradient(listOf(Color(0xFF3A1418), Color(0xFF240A0E)))
            borderColor = Color(0xFFFF7864).copy(alpha = 0.45f)
        }
        else -> {
            textColor = Color(0xFF9AA6D6)
            bg = Brush.verticalGradient(listOf(Color(0xFF1A2560), Color(0xFF121B48)))
            borderColor = Color.White.copy(alpha = 0.08f)
        }
    }

    Box(
        modifier = modifier
            .height(50.dp)
            .clip(shape)
            .background(bg)
            .border(if (style == "enter") 2.dp else 1.5.dp, borderColor, shape)
            .then(
                if (style == "gold") {
                    Modifier.drawWithContent {
                        drawContent()
                        val w = size.width
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.6f), Color.Transparent),
                                start = Offset(w * shimmerX - w * 0.3f, 0f),
                                end = Offset(w * shimmerX + w * 0.3f, size.height)
                            )
                        )
                    }
                } else Modifier
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Text(
                text = line1,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = if (line1.length > 16) 13.sp else 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 17.sp
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer { alpha = if (style == "wait") blink else 1f }
            )
            if (line2.isNotBlank()) {
                Text(
                    text = line2,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 12.sp
                    ),
                    color = textColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
    onConfirm: (Int, String, Long) -> Unit
) {
    val strings = LocalAppStrings.current
    var entryFeeText by remember { mutableStateOf("") }
    val entryFee = entryFeeText.toIntOrNull() ?: 0
    val winnings = if (entryFee > 0) (entryFee * 1.8).toInt() else 0
    val hasSufficientBalance = gameWalletBalance >= entryFee

    // Smart default: Current time + 15 mins rounded up to nearest 5-minute interval
    val initialTime = remember {
        val cal = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.MINUTE, 15)
            val currentMin = get(java.util.Calendar.MINUTE)
            val remainder = currentMin % 5
            if (remainder != 0) {
                add(java.util.Calendar.MINUTE, 5 - remainder)
            }
        }
        val hour12 = when (val h = cal.get(java.util.Calendar.HOUR)) {
            0 -> 12
            else -> h
        }
        val minute = cal.get(java.util.Calendar.MINUTE)
        val amPm = if (cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM) "AM" else "PM"
        Triple(
            hour12.toString().padStart(2, '0'),
            minute.toString().padStart(2, '0'),
            amPm
        )
    }

    // Time Selection State
    var selectedHour by remember { mutableStateOf(initialTime.first) }
    var selectedMinute by remember { mutableStateOf(initialTime.second) }
    var selectedAmPm by remember { mutableStateOf(initialTime.third) }

    val hours = (1..12).map { it.toString().padStart(2, '0') }
    val minutes = (0..55 step 5).map { it.toString().padStart(2, '0') }
    val amPmList = listOf("AM", "PM")

    // Reactive Future Time Validation
    val isFutureTime by remember(selectedHour, selectedMinute, selectedAmPm) {
        derivedStateOf {
            try {
                val now = System.currentTimeMillis()
                val h = selectedHour.toIntOrNull() ?: 12
                val m = selectedMinute.toIntOrNull() ?: 0
                val hour24 = when {
                    selectedAmPm.equals("AM", ignoreCase = true) -> if (h == 12) 0 else h
                    else -> if (h == 12) 12 else h + 12
                }

                val targetCal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, hour24)
                    set(java.util.Calendar.MINUTE, m)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }

                targetCal.timeInMillis > now
            } catch (e: Exception) {
                true
            }
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            CreateDialogRibbon(
                title = strings.autoLudoCreateDialogTitle,
                modifier = Modifier.zIndex(1f)
            )
            Box(
                modifier = Modifier
                    .padding(top = 34.dp)
                    .fillMaxWidth()
                    .border(
                        2.dp,
                        Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFC98A10), Color(0xFFFFE7A0))),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(2.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E2E7E), Color(0xFF131E5A), Color(0xFF0D1648))
                        )
                    )
                    .border(2.dp, Color(0xFF8A6414), RoundedCornerShape(18.dp))
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(7.dp)
                        .border(1.dp, Color(0xA6F0BE50), RoundedCornerShape(13.dp))
                )
                BannerCornerOrnament(Modifier.align(Alignment.TopStart).padding(9.dp), flipX = false, flipY = false)
                BannerCornerOrnament(Modifier.align(Alignment.TopEnd).padding(9.dp), flipX = true, flipY = false)
                BannerCornerOrnament(Modifier.align(Alignment.BottomStart).padding(9.dp), flipX = false, flipY = true)
                BannerCornerOrnament(Modifier.align(Alignment.BottomEnd).padding(9.dp), flipX = true, flipY = true)

                Column(
                    modifier = Modifier
                        .padding(start = 18.dp, end = 18.dp, top = 44.dp, bottom = 20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Balance Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF2A3156), Color(0xFF171C38))))
                            .border(1.5.dp, Color(0xFFC9A04A), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color(0xFFE8B23A),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                strings.autoLudoGameWalletBalance,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFFF3E6C0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (gameWalletBalance > 0.0) {
                            Text(
                                strings.taka(gameWalletBalance),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
                                    fontSize = 20.sp,
                                    brush = Brush.verticalGradient(
                                        listOf(Color(0xFFFFF3C4), Color(0xFFFFC107), Color(0xFFC98A10))
                                    )
                                ),
                                maxLines = 1
                            )
                        } else {
                            Text(
                                strings.taka(gameWalletBalance),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
                                    fontSize = 20.sp
                                ),
                                color = Rose400,
                                maxLines = 1
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Input Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            strings.autoLudoEntryFeeHeader,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFFFD27A),
                            modifier = Modifier.padding(bottom = 8.dp),
                            maxLines = 1
                        )
                        OutlinedTextField(
                            value = entryFeeText,
                            onValueChange = { if (it.all { char -> char.isDigit() }) entryFeeText = it },
                            placeholder = { Text(strings.autoLudoEntryFeePlaceholder, color = Color(0xFF8C97C4), maxLines = 1) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFC107),
                                unfocusedBorderColor = Color(0xFFB9861C),
                                focusedContainerColor = Color(0xFF151E50),
                                unfocusedContainerColor = Color(0xFF151E50),
                                cursorColor = Color(0xFFFFC107)
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    // Time Selector
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            strings.autoLudoStartTimeLabel,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFE6ECFF),
                            modifier = Modifier.padding(bottom = 4.dp),
                            maxLines = 1
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimeDropdown(
                                label = strings.autoLudoHourLabel,
                                options = hours,
                                selected = selectedHour,
                                onSelect = { selectedHour = it },
                                modifier = Modifier.weight(1f)
                            )
                            TimeDropdown(
                                label = strings.autoLudoMinLabel,
                                options = minutes,
                                selected = selectedMinute,
                                onSelect = { selectedMinute = it },
                                modifier = Modifier.weight(1f)
                            )
                            TimeDropdown(
                                label = strings.autoLudoAmPmLabel,
                                options = amPmList,
                                selected = selectedAmPm,
                                onSelect = { selectedAmPm = it },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!isFutureTime) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.verticalGradient(listOf(Color(0xFF4A1418), Color(0xFF2A0A0E))))
                                    .border(1.5.dp, Color(0xFFB8452E), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.autoLudoFutureTimeError,
                                    color = Color(0xFFFFCF6B),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Prize Calculation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF2A3156), Color(0xFF151A33))))
                            .border(1.5.dp, Color(0xFFC9A04A), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                strings.autoLudoWinningPrizeLabel,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFFFFD27A),
                                maxLines = 1
                            )
                            Text(
                                strings.autoLudoWinningPrizeSub,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFFC9D3FF),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            strings.taka(winnings.toDouble()),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
                                fontSize = 28.sp,
                                brush = Brush.verticalGradient(
                                    listOf(Color(0xFFFFF3C4), Color(0xFFFFC107), Color(0xFFC98A10))
                                )
                            ),
                            maxLines = 1
                        )
                    }
                    
                    // Visual Warning Banner when balance is insufficient
                    if (entryFee >= 10 && !hasSufficientBalance) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.verticalGradient(listOf(Color(0xFF4A1418), Color(0xFF2A0A0E))))
                                .border(1.5.dp, Color(0xFFB8452E), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.autoLudoInsufficientBalanceCreate(strings.taka(entryFee.toDouble()), strings.taka(gameWalletBalance)),
                                color = Color(0xFFFFCF6B),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 18.sp
                                )
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
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFC9A04A)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1E2444))
                        ) {
                            Text(
                                strings.cancel,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                        }
                        
                        val confirmHexShape = GenericShape { size, _ ->
                            moveTo(size.width * 0.09f, 0f)
                            lineTo(size.width * 0.91f, 0f)
                            lineTo(size.width, size.height / 2f)
                            lineTo(size.width * 0.91f, size.height)
                            lineTo(size.width * 0.09f, size.height)
                            lineTo(0f, size.height / 2f)
                            close()
                        }
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Button(
                                onClick = { 
                                    if (entryFee >= 10 && isFutureTime && hasSufficientBalance) {
                                        val timeStr = "$selectedHour:$selectedMinute $selectedAmPm"
                                        val h = selectedHour.toIntOrNull() ?: 12
                                        val m = selectedMinute.toIntOrNull() ?: 0
                                        val hour24 = when {
                                            selectedAmPm.equals("AM", ignoreCase = true) -> if (h == 12) 0 else h
                                            else -> if (h == 12) 12 else h + 12
                                        }
                                        val targetCal = java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.HOUR_OF_DAY, hour24)
                                            set(java.util.Calendar.MINUTE, m)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }
                                        val epochMillis = targetCal.timeInMillis
                                        onConfirm(entryFee, timeStr, epochMillis)
                                    } 
                                },
                                enabled = entryFee >= 10 && isFutureTime && hasSufficientBalance,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .alpha(if (entryFee >= 10 && isFutureTime && hasSufficientBalance) 1f else 0.45f),
                                shape = confirmHexShape,
                                border = BorderStroke(
                                    2.5.dp,
                                    Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFC98A10)))
                                ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFFF25FB0), Color(0xFFB8197A), Color(0xFF7A0B50))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        strings.autoLudoConfirm,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            shadow = androidx.compose.ui.graphics.Shadow(
                                                color = Color(0xA6460028),
                                                offset = Offset(0f, 3f),
                                                blurRadius = 0f
                                            )
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .offset(y = (-6).dp)
                                    .size(12.dp)
                                    .rotate(45f)
                                    .background(Brush.linearGradient(listOf(Color(0xFFFFF3C4), Color(0xFFC98A10))))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateDialogRibbon(
    title: String,
    modifier: Modifier = Modifier
) {
    val hexShape = GenericShape { size, _ ->
        moveTo(size.width * 0.07f, 0f)
        lineTo(size.width * 0.93f, 0f)
        lineTo(size.width, size.height / 2f)
        lineTo(size.width * 0.93f, size.height)
        lineTo(size.width * 0.07f, size.height)
        lineTo(0f, size.height / 2f)
        close()
    }
    Column(
        modifier = modifier.fillMaxWidth(0.86f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .zIndex(1f)
                .offset(y = 7.dp)
                .size(14.dp)
                .rotate(45f)
                .background(Brush.linearGradient(listOf(Color(0xFFFFF3C4), Color(0xFFC98A10))))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(59.dp)
                .clip(hexShape)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFC98A10))))
                .padding(2.5.dp)
                .clip(hexShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF9B6BFF), Color(0xFF6A2BD9), Color(0xFF3F1592))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .fillMaxWidth(0.8f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0f))
                        )
                    )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xB3280050),
                        offset = Offset(0f, 3f),
                        blurRadius = 0f
                    )
                ),
                maxLines = 1
            )
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
    val strings = LocalAppStrings.current
    val targetEpoch = remember(match.scheduledTimeMillis, match.scheduledTimeFormatted, match.createdAt) {
        if (match.scheduledTimeMillis > 0L) match.scheduledTimeMillis
        else parseScheduledEpoch(match.scheduledTimeFormatted, match.createdAt)
    }
    val isExpired = targetEpoch > 0L && System.currentTimeMillis() >= targetEpoch

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    2.dp,
                    Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFC98A10), Color(0xFFFFE7A0))),
                    RoundedCornerShape(20.dp)
                )
                .padding(2.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1E2E7E), Color(0xFF131E5A), Color(0xFF0D1648))
                    )
                )
                .border(2.dp, Color(0xFF8A6414), RoundedCornerShape(18.dp))
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(7.dp)
                    .border(1.dp, Color(0xA6F0BE50), RoundedCornerShape(13.dp))
            )
            BannerCornerOrnament(Modifier.align(Alignment.TopStart).padding(9.dp), flipX = false, flipY = false)
            BannerCornerOrnament(Modifier.align(Alignment.TopEnd).padding(9.dp), flipX = true, flipY = false)
            BannerCornerOrnament(Modifier.align(Alignment.BottomStart).padding(9.dp), flipX = false, flipY = true)
            BannerCornerOrnament(Modifier.align(Alignment.BottomEnd).padding(9.dp), flipX = true, flipY = true)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                JoinDialogDivider()

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    strings.autoLudoJoinDialogTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color(0xFF8A6414),
                            offset = Offset(0f, 3f),
                            blurRadius = 0f
                        )
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    strings.autoLudoJoinFeeNotice(strings.taka(match.entryFee)),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFFFF3C4), Color(0xFFFFC107), Color(0xFFC98A10))
                        )
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(Brush.verticalGradient(listOf(Color(0xFF2A3156), Color(0xFF151A33))))
                        .border(1.5.dp, Color(0xFFC9A04A), RoundedCornerShape(50))
                        .padding(start = 6.dp, end = 18.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 54.dp, height = 46.dp)
                            .clip(RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 14.dp, bottomEnd = 14.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF1A2150), Color(0xFF0E1330)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color(0xFF16C060),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        strings.autoLudoBalanceLabel(strings.taka(gameWalletBalance)),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                val hasSufficientBalance = gameWalletBalance >= match.entryFee
                
                if (isExpired) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        strings.autoLudoMatchTimeExpiredError,
                        color = Color(0xFFFFCF6B),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF4A1418), Color(0xFF2A0A0E))))
                            .border(1.5.dp, Color(0xFFB8452E), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                } else if (!hasSufficientBalance) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        strings.autoLudoInsufficientBalanceJoin(strings.taka(match.entryFee)),
                        color = Color(0xFFFFCF6B),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF4A1418), Color(0xFF2A0A0E))))
                            .border(1.5.dp, Color(0xFFB8452E), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFC9A04A)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1E2444))
                    ) {
                        Text(
                            strings.cancel,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }
                    
                    Button(
                        onClick = onConfirm,
                        enabled = hasSufficientBalance && !isExpired,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .alpha(if (hasSufficientBalance && !isExpired) 1f else 0.45f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFE8C24A)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF5BF0A0), Color(0xFF16B85A), Color(0xFF0B7A3C))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                strings.autoLudoConfirm,
                                color = Color(0xFFFFF6A8),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color(0xB3003C19),
                                        offset = Offset(0f, 3f),
                                        blurRadius = 0f
                                    )
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinDialogDivider() {
    Canvas(
        modifier = Modifier
            .width(230.dp)
            .height(24.dp)
    ) {
        val s = size.width / 230f
        val gold = Color(0xFFE8B23A)
        val line = Color(0xFFC98A10)
        drawLine(line, Offset(2f * s, 14f * s), Offset(82f * s, 14f * s), strokeWidth = 1.4f * s, cap = StrokeCap.Round)
        drawLine(line, Offset(148f * s, 14f * s), Offset(228f * s, 14f * s), strokeWidth = 1.4f * s, cap = StrokeCap.Round)
        val curls = Path().apply {
            moveTo(84f * s, 14f * s)
            cubicTo(92f * s, 6f * s, 100f * s, 6f * s, 104f * s, 12f * s)
            cubicTo(100f * s, 16f * s, 94f * s, 16f * s, 92f * s, 12f * s)
            moveTo(146f * s, 14f * s)
            cubicTo(138f * s, 6f * s, 130f * s, 6f * s, 126f * s, 12f * s)
            cubicTo(130f * s, 16f * s, 136f * s, 16f * s, 138f * s, 12f * s)
        }
        drawPath(curls, gold, style = Stroke(width = 1.5f * s, cap = StrokeCap.Round))
        val leaf = Path().apply {
            moveTo(115f * s, 2f * s)
            cubicTo(119f * s, 7f * s, 119f * s, 13f * s, 115f * s, 18f * s)
            cubicTo(111f * s, 13f * s, 111f * s, 7f * s, 115f * s, 2f * s)
            close()
        }
        drawPath(leaf, Color(0xFFFFC107))
        drawPath(leaf, Color(0xFFFFE7A0), style = Stroke(width = 1f * s))
        drawCircle(Color(0xFFFFE08A), radius = 2f * s, center = Offset(106f * s, 15f * s))
        drawCircle(Color(0xFFFFE08A), radius = 2f * s, center = Offset(124f * s, 15f * s))
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
    val strings = LocalAppStrings.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            OutlinedTextField(
                value = strings.digits(selected),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFFC107),
                    unfocusedBorderColor = Color(0xFFC9A04A),
                    focusedContainerColor = Color(0xFF1F2547),
                    unfocusedContainerColor = Color(0xFF1F2547),
                    focusedTrailingIconColor = Color(0xFFFFC107),
                    unfocusedTrailingIconColor = Color(0xFFFFC107)
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFFFFF3C4),
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-10).dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.verticalGradient(listOf(Color(0xFF3A2E10), Color(0xFF1E1606))))
                    .border(1.dp, Color(0xFFE8B23A), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 1.dp)
            )
        }
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 280.dp),
            shape = RoundedCornerShape(14.dp),
            containerColor = Color(0xFF151C42),
            border = BorderStroke(1.5.dp, Color(0xFFC9A04A)),
            shadowElevation = 10.dp
        ) {
            options.forEach { option ->
                val isSelectedOption = option == selected
                DropdownMenuItem(
                    text = {
                        Text(
                            strings.digits(option),
                            color = if (isSelectedOption) Color(0xFFFFD27A) else Color.White,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = if (isSelectedOption) FontWeight.ExtraBold else FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            ),
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelectedOption) Color(0xFFFFC107).copy(alpha = 0.16f) else Color.Transparent
                        )
                        .border(
                            1.dp,
                            if (isSelectedOption) Color(0xFFE8B23A) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
