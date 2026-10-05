package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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

    val liveBalance = wallet?.let { w ->
        if (w.availableAmount > 0.0) w.availableAmount else w.availableBalance / 100.0
    } ?: 0.0
    val bonusBalance = wallet?.let { w ->
        if (w.bonusAmount > 0.0) w.bonusAmount else w.bonusBalance / 100.0
    } ?: 0.0

    val formattedBalance = remember(liveBalance) {
        "৳ ${"%.2f".format(liveBalance)}"
    }
    val formattedBonus = remember(bonusBalance) {
        "🎁 ৳ ${"%.2f".format(bonusBalance)}"
    }

    Surface(
        color = NavySurface,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Tier 1 — Branded Esports Subtitle Strip (~22.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Left cyber divider
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, Gold400.copy(alpha = 0.5f))))
                )

                // Centered Subtitle
                Text(
                    text = "১v১ এস্পোর্টস ব্যাটল",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Gold400,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Right cyber divider
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Brush.horizontalGradient(listOf(Gold400.copy(alpha = 0.5f), Color.Transparent)))
                )
            }

            // Tier 2 — Main Action Row (~52.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Cluster (Trophy + Clock + Title)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (navigationIcon != null) {
                        navigationIcon()
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Circular Neon Trophy Logo: Size = 40.dp with 2.dp dual-tone neon sweep gradient border
                    LogoWithNeonRing(size = 40.dp)

                    Spacer(modifier = Modifier.width(8.dp))

                    // Clock + Title Stack
                    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                        // Live Digital Clock
                        HeaderLuxuryDigitalClock()

                        Spacer(modifier = Modifier.height(3.dp))

                        // Brand Title (ZERO Truncation)
                        Text(
                            text = "AD TOURNAMENT",
                            style = TextStyle(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible
                        )
                    }
                }

                // Right Cluster (Wallet + Bell + Avatar)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Wallet Capsule
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
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
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
                                            fontSize = 11.sp,
                                        ),
                                        color = Gold400,
                                        maxLines = 1,
                                    )
                                    if (bonusBalance > 0.0) {
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
                    }

                    // Notification Bell (size = 36.dp)
                    Surface(
                        onClick = onNotificationClick,
                        shape = CircleShape,
                        color = MidnightNavyCard,
                        border = BorderStroke(
                            0.8.dp,
                            if (unreadNotificationsCount > 0) Rose600.copy(alpha = 0.5f) else MidnightNavyBorder
                        ),
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("top_bar_notification_button"),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = strings.notificationsTitle,
                                tint = if (unreadNotificationsCount > 0) Gold400 else Slate300,
                                modifier = Modifier.size(19.dp),
                            )
                            if (unreadNotificationsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 5.dp, end = 5.dp)
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Rose600)
                                        .border(1.dp, MidnightNavyCard, CircleShape)
                                        .testTag("notification_unread_dot"),
                                    )
                            }
                        }
                    }

                    // Profile Avatar (size = 38.dp)
                    key(currentUser?.effectivePhoto) {
                        EsportsAvatar(
                            photoUrl = currentUser?.effectivePhoto,
                            name = currentUser?.effectiveName ?: "Player",
                            size = 38.dp,
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
            }
        }
    }
}

@Composable
private fun LogoWithNeonRing(size: Dp = 40.dp) {
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
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "AD TOURNAMENT",
                    tint = Gold400,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
fun HeaderLuxuryDigitalClock(modifier: Modifier = Modifier) {
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
        shape = RoundedCornerShape(4.dp),
        color = NavyCard,
        border = BorderStroke(
            1.dp,
            Gold400.copy(alpha = 0.7f)
        ),
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Emerald500.copy(alpha = pulseAlpha))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = timeText,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                ),
                color = Gold400,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
