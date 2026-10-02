package com.example.ui.minigames

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class MiniGame(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector,
    val color: Color,
    val type: MiniGameType
)

enum class MiniGameType {
    SPIN, DICE, EVEN_ODD, HEAD_TAIL, SCRATCH
}

@Composable
fun MiniGamesSection(
    onGameClick: (MiniGameType) -> Unit
) {
    val games = remember {
        listOf(
            MiniGame(
                "1", "রয়্যাল হুইল", "স্পিন করে জিতুন", "HOT", 
                Icons.Default.Refresh, Color(0xFFFFD700), MiniGameType.SPIN
            ),
            MiniGame(
                "2", "জোড়-বেজোড়", "ছক্কার চাল", "1.9x WIN", 
                Icons.Default.Build, Emerald400, MiniGameType.DICE
            ),
            MiniGame(
                "3", "হেড-টেল", "ইনস্ট্যান্ট টস", "POPULAR", 
                Icons.Default.Face, Cyan400, MiniGameType.HEAD_TAIL
            ),
            MiniGame(
                "4", "স্ক্র্যাচ কার্ড", "ঘষে জিতুন", "JACKPOT", 
                Icons.Default.Star, Amber500, MiniGameType.SCRATCH
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "মিনি গেমস (Mini Games)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = Color.White
            )
            Text(
                text = "সব দেখুন",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Gold400
                ),
                modifier = Modifier.clickable { }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.height(265.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = false
        ) {
            items(games) { game ->
                MiniGameCard(game = game, onClick = { onGameClick(game.type) })
            }
        }
    }
}

@Composable
fun MiniGameCard(
    game: MiniGame,
    onClick: () -> Unit
) {
    val isRoyalWheel = game.type == MiniGameType.SPIN
    val isDiceRoll = game.type == MiniGameType.DICE
    val isHeadTail = game.type == MiniGameType.HEAD_TAIL
    val isScratch = game.type == MiniGameType.SCRATCH
    val isLuxury = true // All games are now luxury
    
    val cardBg = when {
        isRoyalWheel -> Brush.horizontalGradient(listOf(Color(0xFF1E0836), Color(0xFF0D0216)))
        isDiceRoll -> Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1B4B)))
        isHeadTail -> Brush.horizontalGradient(listOf(Color(0xFF0B0D21), Color(0xFF1E1538)))
        isScratch -> Brush.horizontalGradient(listOf(Color(0xFF2E1A47), Color(0xFF1A1A2E)))
        else -> Brush.verticalGradient(listOf(Slate900, Slate950))
    }
    
    val borderColor = when {
        isRoyalWheel -> Color(0xFFFFD700).copy(alpha = 0.6f)
        isDiceRoll -> Color(0xFFFFD700).copy(alpha = 0.4f)
        isHeadTail -> Color(0xFFFFD700).copy(alpha = 0.5f)
        isScratch -> Color(0xFFFFD700).copy(alpha = 0.45f)
        else -> game.color.copy(alpha = 0.3f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cardBg)
        ) {
            if (isLuxury) {
                // LUXURY LAYOUT FOR PREMIUM MINI GAMES
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        BadgeCard(
                            text = when {
                                isRoyalWheel -> "রয়্যাল হুইল"
                                isHeadTail -> "১.৯ গুণ জয়"
                                isScratch -> "জ্যাকপট কার্ড"
                                else -> "১.৯ গুণ জয়"
                            }, 
                            color = when {
                                isRoyalWheel -> Color(0xFFFFD700)
                                isScratch -> Amber500
                                else -> Emerald400
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Column {
                            Text(
                                text = game.title,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )
                            Text(
                                text = game.subtitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    color = (if (isRoyalWheel || isScratch) Color(0xFFFFD700) else Emerald400).copy(alpha = 0.8f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.4f),
                            border = BorderStroke(0.5.dp, (if (isRoyalWheel || isScratch) Color(0xFFFFD700) else Emerald400).copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = when {
                                    isRoyalWheel -> "সর্বোচ্চ ৫০x"
                                    isScratch -> "ঘষে জিতুন"
                                    else -> "Payout: 1.9x"
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isRoyalWheel || isScratch) Color(0xFFFFD700) else Emerald400
                                )
                            )
                        }
                    }
                    
                    when {
                        isRoyalWheel -> {
                            MiniRoyalWheelGraphic(
                                modifier = Modifier
                                    .size(85.dp)
                                    .offset(x = 8.dp)
                            )
                        }
                        isDiceRoll -> {
                            MiniDiceRollGraphic(
                                modifier = Modifier
                                    .size(68.dp)
                                    .offset(x = 4.dp)
                            )
                        }
                        isHeadTail -> {
                            MiniHeadTailGraphic(
                                modifier = Modifier
                                    .size(85.dp)
                                    .offset(x = 8.dp)
                            )
                        }
                        isScratch -> {
                            MiniScratchGraphic(
                                modifier = Modifier
                                    .size(85.dp)
                                    .offset(x = 8.dp)
                            )
                        }
                    }
                }
            } else {
                // Background Glow for standard cards
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-10).dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(game.color.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(game.color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = game.icon,
                                contentDescription = null,
                                tint = game.color,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        BadgeCard(text = game.badge, color = game.color)
                    }

                    Column {
                        Text(
                            text = game.title,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = game.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp
                            ),
                            color = Slate400
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniDiceRollGraphic(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "MiniDice")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(100.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color(0xFFFFD700).copy(alpha = 0.25f),
                    0.7f to Color(0xFFFFD700).copy(alpha = 0.05f),
                    1.0f to Color.Transparent
                )
            )
        }
        
        Row(
            modifier = Modifier.offset(y = bounce.dp).rotate(rotation),
            horizontalArrangement = Arrangement.spacedBy((-12).dp)
        ) {
            // 3D Styled Mini Dice 1
            MiniLuxuryDice(value = 6, rotation = 12f)
            // 3D Styled Mini Dice 2
            MiniLuxuryDice(value = 5, rotation = -8f, offset = Offset(0f, 10f))
        }
    }
}

@Composable
fun MiniLuxuryDice(value: Int, rotation: Float, offset: Offset = Offset.Zero) {
    Surface(
        modifier = Modifier
            .size(38.dp)
            .offset(offset.x.dp, offset.y.dp)
            .rotate(rotation),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFA51C30),
        border = BorderStroke(1.2.dp, Color(0xFFFFD700).copy(alpha = 0.6f)),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFEF4444), Color(0xFFA51C30), Color(0xFF7F1D1D)),
                        center = Offset(20f, 20f)
                    )
                )
                .padding(6.dp)
        ) {
            // Gloss effect
            Box(
                modifier = Modifier
                    .size(15.dp, 10.dp)
                    .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)))
            )

            val dotColor = Color(0xFFFFD700)
            when (value) {
                5 -> {
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(dotColor).align(Alignment.TopStart))
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(dotColor).align(Alignment.TopEnd))
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(dotColor).align(Alignment.Center))
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(dotColor).align(Alignment.BottomStart))
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(dotColor).align(Alignment.BottomEnd))
                }
                6 -> {
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(dotColor).align(Alignment.TopStart))
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(dotColor).align(Alignment.TopEnd))
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(dotColor).align(Alignment.CenterStart))
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(dotColor).align(Alignment.CenterEnd))
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(dotColor).align(Alignment.BottomStart))
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(dotColor).align(Alignment.BottomEnd))
                }
            }
        }
    }
}

@Composable
fun MiniRoyalWheelGraphic(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "MiniWheel")
    
    // Slow ambient rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Pulsing LED effect
    val ledAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ledAlpha"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Background Glow Aura
        Canvas(modifier = Modifier.size(100.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color(0xFFFFD700).copy(alpha = 0.15f),
                    0.6f to Color(0xFFFFD700).copy(alpha = 0.05f),
                    1.0f to Color.Transparent
                ),
                radius = size.width / 2
            )
        }

        // The Wheel
        Canvas(modifier = Modifier.size(72.dp)) {
            val radius = size.width / 2
            val center = this.center

            // 1. Metallic Outer Rim
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(Color(0xFFAA771C), Color(0xFFFFD700), Color(0xFFAA771C))
                ),
                radius = radius,
                style = Stroke(width = 4.dp.toPx())
            )

            // 2. Rotating Inner Face
            this.rotate(rotation) {
                val sliceCount = 12
                val sliceAngle = 360f / sliceCount
                for (i in 0 until sliceCount) {
                    drawArc(
                        color = if (i % 2 == 0) Color(0xFF2D0B4E) else Color(0xFF3F126D),
                        startAngle = i * sliceAngle,
                        sweepAngle = sliceAngle,
                        useCenter = true,
                        size = size
                    )
                    
                    // Spokes
                    val angleRad = (i * sliceAngle) * (Math.PI / 180).toFloat()
                    drawLine(
                        color = Color(0xFFFFD700).copy(alpha = 0.25f),
                        start = center,
                        end = Offset(
                            center.x + radius * Math.cos(angleRad.toDouble()).toFloat(),
                            center.y + radius * Math.sin(angleRad.toDouble()).toFloat()
                        ),
                        strokeWidth = 0.8.dp.toPx()
                    )
                }
            }

            // 3. LED Bulbs along the rim
            val ledCount = 12
            for (i in 0 until ledCount) {
                val angleRad = (i * (360f / ledCount)) * (Math.PI / 180).toFloat()
                val lx = center.x + (radius - 2.dp.toPx()) * Math.cos(angleRad.toDouble()).toFloat()
                val ly = center.y + (radius - 2.dp.toPx()) * Math.sin(angleRad.toDouble()).toFloat()
                drawCircle(
                    color = if (i % 2 == 0) Color.White.copy(alpha = ledAlpha) else Color(0xFFFFD700).copy(alpha = 1f - ledAlpha),
                    radius = 1.6.dp.toPx(),
                    center = Offset(lx, ly)
                )
            }

            // 4. Pointer Arrow (Left side pointing in)
            val pointerWidth = 8.dp.toPx()
            val pointerHeight = 10.dp.toPx()
            val pointerPath = Path().apply {
                moveTo(0f, center.y) // Tip
                lineTo(pointerWidth, center.y - pointerHeight / 2)
                lineTo(pointerWidth, center.y + pointerHeight / 2)
                close()
            }
            drawPath(pointerPath, color = Color(0xFFFFD700))
            drawPath(pointerPath, color = Color.Black.copy(alpha = 0.2f), style = Stroke(width = 0.5.dp.toPx()))
        }

        // 5. Center Hub Medallion
        Surface(
            modifier = Modifier.size(16.dp),
            shape = CircleShape,
            color = Color(0xFFFFD700),
            border = BorderStroke(1.2.dp, Color(0xFFF9E79F)),
            tonalElevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            0.0f to Color(0xFFF9E79F),
                            0.7f to Color(0xFFAA771C),
                            1.0f to Color(0xFF7D510D)
                        )
                    )
            )
        }
    }
}

@Composable
fun MiniHeadTailGraphic(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "MiniCoin")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Glow
        Canvas(modifier = Modifier.size(80.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFFD700).copy(alpha = 0.3f), Color.Transparent)
                )
            )
        }
        
        // Coin
        Surface(
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer {
                    rotationY = rotation
                },
            shape = CircleShape,
            color = Color(0xFFFFD700),
            border = BorderStroke(2.dp, Color(0xFFFFF7C2)),
            shadowElevation = 6.dp
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = Color(0xFF8B4513).copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun MiniScratchGraphic(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.size(75.dp, 55.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFD4AF37),
            border = BorderStroke(1.5.dp, Color(0xFFFFF7C2)),
            shadowElevation = 4.dp
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                // Scratched area
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.6f)
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .align(Alignment.Center)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).align(Alignment.Center),
                        tint = Color(0xFFD4AF37)
                    )
                }
                
                // Unscratched part
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFC0C0C0), Color(0xFFE5E4E2))),
                            RoundedCornerShape(4.dp)
                        )
                        .alpha(0.5f)
                )
            }
        }
    }
}

@Composable
fun BadgeCard(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Black
            ),
            color = color
        )
    }
}
