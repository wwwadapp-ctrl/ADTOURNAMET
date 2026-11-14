package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.home.adPressScale
import com.example.ui.splash.AdSplashEmblem
import kotlin.math.cos
import kotlin.math.sin

// Artwork for the royal blue + gold glass top bar. VISUAL ONLY: no data, no navigation, no logic.

internal val TbGoldLight = Color(0xFFFFF3C4)
internal val TbGold = Color(0xFFFFD45A)
internal val TbGoldMid = Color(0xFFE0A82A)
internal val TbGoldDeep = Color(0xFFC98A10)
internal val TbGoldDark = Color(0xFF8A5A08)
internal val TbBrown = Color(0xFF3A2600)

internal val TbRingBrush = Brush.sweepGradient(
    listOf(Color(0xFFFFF8D8), TbGoldDeep, Color(0xFFFFE07A), TbGoldDark, TbGold, Color(0xFFFFF8D8))
)
internal val TbGoldEdge = Brush.linearGradient(listOf(TbGoldLight, TbGoldDeep, Color(0xFFFFE07A), TbGoldDark))
internal val TbGoldText = Brush.verticalGradient(
    listOf(Color(0xFFFFFBE8), Color(0xFFFFE07A), Color(0xFFF2B21E), Color(0xFFB9780C))
)
private val TbDarkGoldFill = Brush.verticalGradient(listOf(Color(0xF2463008), Color(0xF51E1403)))

/** Golden light rays and the glow coming from the top centre. */
internal fun DrawScope.drawTopBarRays() {
    val cx = size.width / 2f
    val top = -12f
    val len = size.height * 1.7f
    val rayBrush = Brush.radialGradient(
        listOf(Color(0x3DFFDC82), Color(0x14FFDC82), Color.Transparent),
        center = Offset(cx, top),
        radius = len
    )
    var deg = 22f
    while (deg < 158f) {
        val a1 = Math.toRadians(deg.toDouble())
        val a2 = Math.toRadians((deg + 2.5f).toDouble())
        val p = Path().apply {
            moveTo(cx, top)
            lineTo(cx + len * cos(a1).toFloat(), top + len * sin(a1).toFloat())
            lineTo(cx + len * cos(a2).toFloat(), top + len * sin(a2).toFloat())
            close()
        }
        drawPath(p, rayBrush)
        deg += 8f
    }
    val glowR = 95.dp.toPx()
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xBFFFF0BE), Color(0x38FFD678), Color.Transparent),
            center = Offset(cx, top - 10f),
            radius = glowR
        ),
        radius = glowR,
        center = Offset(cx, top - 10f)
    )
}

@Composable
internal fun TbDiamond(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .rotate(45f)
            .background(Brush.linearGradient(listOf(Color(0xFFFFF8D8), TbGoldMid)))
    )
}

@Composable
private fun TbTakaCoin(coin: Dp) {
    Box(
        modifier = Modifier
            .size(coin)
            .drawBehind { drawCircle(Color(0xB3FFD25A), radius = size.minDimension * 0.62f) }
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFFFFF8D8), TbGold, TbGoldDeep)))
            .drawBehind {
                drawCircle(
                    Color(0x99784600),
                    radius = size.minDimension / 2f - 1.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "৳",
            style = TextStyle(color = Color(0xFF6A4300), fontSize = (coin.value * 0.55f).sp, fontWeight = FontWeight.Black)
        )
    }
}

/** The app's own emblem (same drawing as Splash and Login) inside a double gold ring. */
@Composable
internal fun TopBarEmblemBadge(badge: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "EmblemGlow")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "EmblemGlowAlpha"
    )
    Box(
        modifier = Modifier
            .size(badge)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFCD5A).copy(alpha = glow), Color.Transparent),
                        radius = size.minDimension * 0.75f
                    ),
                    radius = size.minDimension * 0.75f
                )
            }
            .clip(CircleShape)
            .background(TbRingBrush)
            .padding(2.5.dp)
            .clip(CircleShape)
            .background(TbBrown)
            .padding(1.5.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF2A3AB0), Color(0xFF141C66), Color(0xFF0B1040)))),
        contentAlignment = Alignment.Center
    ) {
        AdSplashEmblem(
            modifier = Modifier
                .requiredSize(width = badge, height = badge * 0.9f)
                .offset(y = badge * 0.03f)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawOval(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0f)),
                            startY = 0f, endY = size.height * 0.45f
                        ),
                        topLeft = Offset(size.width * 0.08f, 0f),
                        size = Size(size.width * 0.84f, size.height * 0.42f)
                    )
                }
        )
    }
}

/** Gold wallet capsule: coin + balance, and the bonus line under it when there is a bonus. */
@Composable
internal fun TbWalletCapsule(
    loading: Boolean,
    balanceText: String,
    bonusText: String,
    showBonus: Boolean,
    k: Float,
    onClick: () -> Unit
) {
    val capsule = RoundedCornerShape(22.dp)
    val press = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .adPressScale(press, 0.92f)
            .shadow(6.dp, capsule, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(capsule)
            .background(TbGoldEdge)
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
            .testTag("top_bar_wallet_chip")
            .padding(1.2.dp)
            .clip(capsule)
            .background(TbDarkGoldFill)
    ) {
        Row(
            modifier = Modifier.padding(start = 3.dp, end = (9 * k).dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            TbTakaCoin(coin = (20 * k).dp)
            Column(horizontalAlignment = Alignment.Start) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = TbGold, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = balanceText,
                        style = TextStyle(
                            color = Color(0xFFFFF0B8),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = (11.5f * k).sp,
                            letterSpacing = 0.sp,
                            shadow = Shadow(Color(0x73FFC850), Offset.Zero, 8f)
                        ),
                        maxLines = 1,
                        softWrap = false,
                    )
                    if (showBonus) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = Color(0xFF5EEAD4),
                                modifier = Modifier.size(9.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = bonusText,
                                style = TextStyle(
                                    color = Color(0xFF5EEAD4),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = (8.5f * k).sp,
                                    shadow = Shadow(Color(0x8C5EEAD4), Offset.Zero, 6f)
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Blue glass bell with a gold rim and gold bell; red glowing dot when there are unread notifications. */
@Composable
internal fun TbBellButton(
    unreadCount: Int,
    contentDescription: String,
    k: Float,
    onClick: () -> Unit
) {
    val press = remember { MutableInteractionSource() }
    Box(modifier = Modifier.adPressScale(press, 0.88f).size((36 * k).dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        Brush.radialGradient(listOf(Color(0xA65A78FF), Color.Transparent), radius = size.minDimension * 0.75f),
                        radius = size.minDimension * 0.75f
                    )
                }
                .clip(CircleShape)
                .background(TbGoldEdge)
                .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
                .testTag("top_bar_notification_button")
                .padding(1.5.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF6A82FF), Color(0xFF2E44D0), Color(0xFF1A2794))))
                .drawBehind {
                    drawOval(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0f)),
                            startY = 0f, endY = size.height * 0.45f
                        ),
                        topLeft = Offset(size.width * 0.12f, size.height * 0.02f),
                        size = Size(size.width * 0.76f, size.height * 0.42f)
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier
                    .size((18 * k).dp)
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.verticalGradient(listOf(Color(0xFFFFFBE8), TbGold, TbGoldDeep)),
                            blendMode = BlendMode.SrcAtop
                        )
                    },
            )
        }
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 1.dp, end = 1.dp)
                    .size(9.dp)
                    .drawBehind { drawCircle(Color(0x99F43F5E), radius = size.minDimension * 0.9f) }
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFFFF8FA3), Color(0xFFF43F5E), Color(0xFFB3122F))))
                    .border(1.5.dp, Color(0xFF1A2794), CircleShape)
                    .testTag("notification_unread_dot"),
            )
        }
    }
}

/** Double gold ring with a soft glow around the profile photo. */
@Composable
internal fun TbAvatarRing(ring: Dp, onClick: () -> Unit, content: @Composable () -> Unit) {
    val press = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .adPressScale(press, 0.88f)
            .size(ring)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(Color(0x99FFCD5A), Color.Transparent), radius = size.minDimension * 0.72f),
                    radius = size.minDimension * 0.72f
                )
            }
            .clip(CircleShape)
            .background(TbRingBrush)
            .clickable(interactionSource = press, indication = LocalIndication.current, onClick = onClick)
            .testTag("top_bar_profile_button")
            .padding(2.dp)
            .clip(CircleShape)
            .background(TbBrown)
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
