package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.i18n.LocalAppStrings
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedEsportsTopBar(
    currentUser: UserEntity?,
    wallet: WalletEntity?,
    unreadNotificationsCount: Int = 0,
    onWalletClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAdminClick: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isAdmin = currentUser?.role?.equals("ADMIN", ignoreCase = true) == true ||
            currentUser?.role?.equals("SUPER_ADMIN", ignoreCase = true) == true

    val formattedBalance = remember(wallet?.balance, wallet?.availableBalance) {
        val bal = wallet?.let { w ->
            if (w.availableAmount > 0.0) w.availableAmount else w.availableBalance / 100.0
        } ?: 0.0
        "৳ ${"%.2f".format(bal)}"
    }
    val formattedBonus = remember(wallet?.bonusBalance, wallet?.bonusAmount) {
        val bonus = wallet?.let { w ->
            if (w.bonusAmount > 0.0) w.bonusAmount else w.bonusBalance / 100.0
        } ?: 0.0
        "🎁 ৳ ${"%.2f".format(bonus)}"
    }

    Box(modifier = modifier.fillMaxWidth()) {
        TopAppBar(
            navigationIcon = navigationIcon ?: {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Left: Circular Neon Logo (36.dp total size)
                    LogoWithNeonRing(size = 36.dp)
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Left: Clock & Title Stack
                    Column(modifier = Modifier.width(IntrinsicSize.Min)) {
                        HeaderLuxuryDigitalClock()
                        Text(
                            text = "AD TOURNAMENT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.4.sp,
                                fontSize = 12.sp,
                            ),
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            },
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    // Wallet Balance Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Slate900,
                        border = BorderStroke(1.dp, Gold400.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onWalletClick)
                            .testTag("top_bar_wallet_chip"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Wallet",
                                tint = Gold400,
                                modifier = Modifier.size(14.dp),
                            )
                            Column(horizontalAlignment = Alignment.Start) {
                                if (wallet == null) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = Gold400,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = formattedBalance,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                        ),
                                        color = Gold400,
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = formattedBonus,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp,
                                        ),
                                        color = Cyan400,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }

                    // Notification Bell (size 32.dp)
                    PremiumNotificationBellButton(
                        unreadCount = unreadNotificationsCount,
                        onClick = onNotificationClick,
                        contentDescription = strings.notificationsTitle,
                        modifier = Modifier.size(32.dp)
                    )

                    // Profile Avatar (size 34.dp)
                    key(currentUser?.effectivePhoto) {
                        EsportsAvatar(
                            photoUrl = currentUser?.effectivePhoto,
                            name = currentUser?.effectiveName ?: "Player",
                            size = 34.dp,
                            modifier = Modifier.testTag("top_bar_profile_button"),
                            onClick = onProfileClick
                        )
                    }

                    // Admin Settings
                    if (isAdmin && onAdminClick != null) {
                        IconButton(
                            onClick = onAdminClick,
                            modifier = Modifier.size(32.dp).testTag("top_bar_admin_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = strings.adminPanel,
                                tint = Cyan400,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = NavySurface,
            ),
        )

        // Center Section: Top Subtitle (Bengali) - Absolute Top Center
        Text(
            text = strings.appTagline,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontSize = 10.sp,
            ),
            color = Gold400,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 2.dp)
        )
    }
}

@Composable
private fun LogoWithNeonRing(size: androidx.compose.ui.unit.Dp = 40.dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "LogoPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "PulseScale"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "Rotation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        // Neon Border Ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .drawBehind {
                    rotate(rotation) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(Color(0xFF00E5FF), Color(0xFFFFD700), Color(0xFF00E5FF)),
                                center = center
                            ),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    // Ambient Glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.15f), Color.Transparent),
                            radius = (size / 2).toPx()
                        )
                    )
                }
        )

        Surface(
            shape = CircleShape,
            color = Slate900,
            modifier = Modifier.size(size * 0.8f),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "AD TOURNAMENT",
                    modifier = Modifier.size(size * 0.7f),
                )
            }
        }
    }
}

@Composable
fun HeaderLuxuryDigitalClock() {
    var timeText by remember { mutableStateOf("") }
    val timeFormat = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        while (true) {
            timeText = timeFormat.format(Date())
            delay(1000L)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Surface(
        shape = RoundedCornerShape(3.dp),
        color = NavyCard,
        border = BorderStroke(
            1.dp,
            Gold400.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Emerald500.copy(alpha = pulseAlpha))
            )
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                color = Gold400,
                maxLines = 1
            )
        }
    }
}
