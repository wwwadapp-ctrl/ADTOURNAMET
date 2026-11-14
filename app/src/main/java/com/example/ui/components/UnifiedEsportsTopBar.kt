package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.*
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// Royal blue + gold glass top bar (artwork in AdTopBarArt.kt).
// VISUAL ONLY: every value, click, test tag and timer works exactly as before.

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

    // Same numbers as before; the ৳ sign is drawn on the gold coin next to the balance
    val formattedBalance = remember(liveBalance, strings) {
        strings.digits(String.format(Locale.US, "%.2f", liveBalance))
    }
    val formattedBonus = remember(bonusBalance, strings) {
        strings.taka(bonusBalance)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF2A3BB8), Color(0xFF2232A2), Color(0xFF1D2B93))))
            .drawBehind { drawTopBarRays() }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            val k = (maxWidth.value / 400f).coerceIn(0.82f, 1f)

            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                // Tier 1 — ornamental subtitle strip
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    TbDiamond(5.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier.weight(1f).height(1.dp)
                            .background(Brush.horizontalGradient(listOf(TbGold.copy(alpha = 0.2f), Color(0xFFFFE28C))))
                    )
                    Text("✦", fontSize = 9.sp, color = Color(0xFFFFE7A0), modifier = Modifier.padding(horizontal = 5.dp))
                    Text(
                        text = strings.topBarSubtitle,
                        style = TextStyle(
                            brush = Brush.verticalGradient(listOf(Color(0xFFFFF8D8), TbGold, TbGoldMid)),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp,
                            shadow = Shadow(Color(0x99000000), Offset(0f, 2f), 4f)
                        ),
                        maxLines = 1
                    )
                    Text("✦", fontSize = 9.sp, color = Color(0xFFFFE7A0), modifier = Modifier.padding(horizontal = 5.dp))
                    Box(
                        modifier = Modifier.weight(1f).height(1.dp)
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFFE28C), TbGold.copy(alpha = 0.2f))))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    TbDiamond(5.dp)
                }

                // Tier 2 — glass plate with a gold frame
                val plateShape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .padding(start = 7.dp, end = 7.dp, top = 4.dp)
                        .fillMaxWidth()
                        .shadow(10.dp, plateShape, ambientColor = Color(0xFF050A3C), spotColor = Color(0xFF050A3C))
                        .clip(plateShape)
                        .background(Brush.linearGradient(listOf(Color(0x4D788CFF), Color(0x8C2837AA), Color(0xC7121A64))))
                        .drawBehind {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0f)),
                                    startY = 0f, endY = size.height * 0.48f
                                ),
                                size = Size(size.width, size.height * 0.48f),
                                cornerRadius = CornerRadius(19.dp.toPx())
                            )
                        }
                        .border(
                            1.3.dp,
                            Brush.linearGradient(listOf(TbGoldLight, TbGoldMid, TbGoldMid.copy(alpha = 0.25f), TbGoldMid, TbGoldLight)),
                            plateShape
                        )
                        .padding(horizontal = (9 * k).dp, vertical = (8 * k).dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Cluster (Emblem + Clock + Title)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (navigationIcon != null) {
                                navigationIcon()
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            TopBarEmblemBadge(badge = (52 * k).dp)
                            Spacer(modifier = Modifier.width((7 * k).dp))
                            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                                HeaderLuxuryDigitalClock(scale = k)
                                Spacer(modifier = Modifier.height((4 * k).dp))
                                Text(
                                    text = "AD TOURNAMENT",
                                    style = TextStyle(
                                        brush = TbGoldText,
                                        fontSize = (13.5f * k).sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.6.sp,
                                        shadow = Shadow(TbBrown, Offset(0f, 3f), 0f)
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
                            horizontalArrangement = Arrangement.spacedBy((6 * k).dp)
                        ) {
                            TbWalletCapsule(
                                loading = wallet == null,
                                balanceText = formattedBalance,
                                bonusText = formattedBonus,
                                showBonus = bonusBalance > 0.0,
                                k = k,
                                onClick = onWalletClick
                            )

                            TbBellButton(
                                unreadCount = unreadNotificationsCount,
                                contentDescription = strings.notificationsTitle,
                                k = k,
                                onClick = onNotificationClick
                            )

                            TbAvatarRing(ring = (42 * k).dp, onClick = onProfileClick) {
                                key(currentUser?.effectivePhoto) {
                                    EsportsAvatar(
                                        photoUrl = currentUser?.effectivePhoto,
                                        name = currentUser?.effectiveName ?: "Player",
                                        size = (42 * k).dp - 6.dp,
                                        borderWidth = 0.dp,
                                        onClick = null
                                    )
                                }
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
                                        tint = TbGold,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom ornament: gold line with a diamond
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 30.dp, end = 30.dp, top = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f).height(1.dp)
                            .background(Brush.horizontalGradient(listOf(TbGold.copy(alpha = 0f), TbGold)))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    TbDiamond(6.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier.weight(1f).height(1.dp)
                            .background(Brush.horizontalGradient(listOf(TbGold, TbGold.copy(alpha = 0f))))
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderLuxuryDigitalClock(modifier: Modifier = Modifier, scale: Float = 1f) {
    val strings = LocalAppStrings.current
    var timeText by remember { mutableStateOf("") }
    val timeFormat = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        while (true) {
            timeText = strings.digits(timeFormat.format(Date()))
            delay(1000L)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(Brush.horizontalGradient(listOf(TbGoldLight, TbGoldDeep, Color(0xFFFFE07A), TbGoldDeep)))
            .padding(1.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.verticalGradient(listOf(Color(0xEB3C2806), Color(0xF21A1102))))
            .drawBehind {
                drawRect(Color(0x4DFFF0BE), topLeft = Offset.Zero, size = Size(size.width, 1.dp.toPx()))
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = (7 * scale).dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .drawBehind {
                        drawCircle(Color(0xFF34D399).copy(alpha = pulseAlpha * 0.5f), radius = size.minDimension)
                    }
                    .clip(CircleShape)
                    .background(Color(0xFF34D399).copy(alpha = pulseAlpha))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = timeText,
                style = TextStyle(
                    fontSize = (10.5f * scale).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.3.sp,
                    color = Color(0xFFFFE07A),
                    shadow = Shadow(Color(0x99FFC850), Offset.Zero, 8f)
                ),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(end = 2.dp)
            )
        }
    }
}
