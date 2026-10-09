package com.example.ui.autoludo.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*
import kotlinx.coroutines.delay

val LudoRed = Color(0xFFE53935)
val LudoBlue = Color(0xFF1E88E5)
val LudoGreen = Color(0xFF43A047)
val LudoYellow = Color(0xFFFFEB3B)

/**
 * LudoGameScreen: A high-fidelity, Ludo King inspired game screen.
 * Optimized for a clean look: No corner cards, integrated dashboard, and polished animations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoGameScreen(
    matchId: String,
    onBack: () -> Unit
) {
    var diceValue by remember { mutableIntStateOf(1) }
    var isRolling by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }

    // Animation for the background gradient
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient"
    )

    if (showSplash) {
        LudoSplashScreen { showSplash = false }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Slate900, Slate800),
                        startY = 0f,
                        endY = 1000f * gradientShift
                    )
                )
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("অটো লুডু", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                Text("ID: #$matchId", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White
                        )
                    )
                },
                containerColor = Color.Transparent
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Central Game Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Transparent)
                            .border(4.dp, Gold400.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = "https://i.postimg.cc/mhQvCN4L",
                            contentDescription = "Ludo Board",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        // Demo Tokens
                        LudoToken(
                            color = LudoRed,
                            isSelected = true,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = 35.dp, y = (-20).dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Dashboard Area (Integrated Dice & Controls)
                    DashboardArea(
                        diceValue = diceValue,
                        isRolling = isRolling,
                        onRoll = {
                            if (!isRolling) {
                                isRolling = true
                            }
                        }
                    )
                }
            }
        }
    }

    // Dice animation logic
    LaunchedEffect(isRolling) {
        if (isRolling) {
            repeat(12) {
                diceValue = (1..6).random()
                delay(80)
            }
            isRolling = false
        }
    }
}

@Composable
private fun DashboardArea(
    diceValue: Int,
    isRolling: Boolean,
    onRoll: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isRolling) 720f else 0f,
        animationSpec = if (isRolling) infiniteRepeatable(tween(300, easing = LinearEasing)) else tween(600),
        label = "dice_rot"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(Slate800.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Player Info (Simplified)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(LudoRed, CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("P1", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("আপনার চাল", color = Color.White, fontSize = 12.sp)
        }

        // Dice
        Box(
            modifier = Modifier
                .size(80.dp)
                .rotate(rotation)
                .background(
                    brush = Brush.radialGradient(listOf(Color.White, Color(0xFFEEEEEE))),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(enabled = !isRolling) { onRoll() }
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            DiceFace(diceValue)
        }

        // Opponent Info (Simplified)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Slate700, CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("P2", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("অপেক্ষমান", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun DiceFace(value: Int) {
    Box(modifier = Modifier.fillMaxSize()) {
        val dotColor = Color.Black
        val dotSize = 10.dp
        val dotModifier = Modifier
            .size(dotSize)
            .background(dotColor, CircleShape)

        when (value) {
            1 -> Box(modifier = dotModifier.align(Alignment.Center))
            2 -> {
                Box(modifier = dotModifier.align(Alignment.TopStart))
                Box(modifier = dotModifier.align(Alignment.BottomEnd))
            }
            3 -> {
                Box(modifier = dotModifier.align(Alignment.TopStart))
                Box(modifier = dotModifier.align(Alignment.Center))
                Box(modifier = dotModifier.align(Alignment.BottomEnd))
            }
            4 -> {
                Box(modifier = dotModifier.align(Alignment.TopStart))
                Box(modifier = dotModifier.align(Alignment.TopEnd))
                Box(modifier = dotModifier.align(Alignment.BottomStart))
                Box(modifier = dotModifier.align(Alignment.BottomEnd))
            }
            5 -> {
                Box(modifier = dotModifier.align(Alignment.TopStart))
                Box(modifier = dotModifier.align(Alignment.TopEnd))
                Box(modifier = dotModifier.align(Alignment.Center))
                Box(modifier = dotModifier.align(Alignment.BottomStart))
                Box(modifier = dotModifier.align(Alignment.BottomEnd))
            }
            6 -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(modifier = dotModifier); Box(modifier = dotModifier)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(modifier = dotModifier); Box(modifier = dotModifier)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(modifier = dotModifier); Box(modifier = dotModifier)
                    }
                }
            }
        }
    }
}
