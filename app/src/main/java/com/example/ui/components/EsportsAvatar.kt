package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.*
import com.example.ui.theme.*

data class AvatarPreset(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val gradient: List<Color>,
)

val avatarPresets = listOf(
    AvatarPreset("preset://dragon", "Dragon", Icons.Default.EmojiEvents, listOf(Color(0xFFE53935), Color(0xFFFFB300))),
    AvatarPreset("preset://cyber", "Cyber", Icons.Default.SportsEsports, listOf(Color(0xFF7C4DFF), Color(0xFF00E5FF))),
    AvatarPreset("preset://ninja", "Ninja", Icons.Default.Bolt, listOf(Color(0xFF00B0FF), Color(0xFF1DE9B6))),
    AvatarPreset("preset://crown", "Crown", Icons.Default.Star, listOf(Color(0xFFFFD700), Color(0xFFFF6D00))),
    AvatarPreset("preset://sniper", "Sniper", Icons.Default.Security, listOf(Color(0xFF00E676), Color(0xFF00B0FF))),
    AvatarPreset("preset://shield", "Knight", Icons.Default.Shield, listOf(Color(0xFF90A4AE), Color(0xFF37474F))),
)

@Composable
fun EsportsAvatar(
    photoUrl: String?,
    name: String,
    size: Dp = 36.dp,
    borderWidth: Dp = 2.dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val cleanName = name.trim().uppercase()
    
    val genderFallback = remember(cleanName) {
        val femaleKeywords = listOf("MST", "MOSAMMAT", "BEGUM", "AKTER", "KHATUN", "JAHAN", "SULTANA", "PARVIN", "NASRIN", "RANI", "FATEMA", "AYESHA")
        val maleKeywords = listOf("MD", "MOHAMMAD", "MUHAMMAD", "AHMED", "ISLAM", "HOSSAIN", "HASAN", "KHAN", "ALI", "SHEIKH", "CHOWDHURY")
        
        when {
            femaleKeywords.any { cleanName.contains(it) || cleanName.startsWith(it) } -> GenderType.FEMALE
            maleKeywords.any { cleanName.contains(it) || cleanName.startsWith(it) } -> GenderType.MALE
            else -> GenderType.MALE // Default to Male as requested
        }
    }

    val preset = remember(photoUrl) {
        if (photoUrl?.startsWith("preset://") == true) {
            avatarPresets.firstOrNull { it.id == photoUrl }
        } else null
    }

    val bitmap = remember(photoUrl) {
        if (photoUrl?.startsWith("data:image/") == true) {
            try {
                val base64 = photoUrl.substringAfter("base64,")
                val bytes = Base64.decode(base64, Base64.NO_WRAP)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val infiniteTransition = rememberInfiniteTransition(label = "AvatarPulse")
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

    // Dual-tone circular border ring (Neon Cyan/Gold)
    val ringColors = listOf(Color(0xFF00E5FF), Color(0xFFFFD700), Color(0xFF00E5FF))

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size + (borderWidth * 4)) // Extra space for gap
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        // Outer Glowing Neon Border Ring
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
                                colors = ringColors,
                                center = center
                            ),
                            style = Stroke(width = borderWidth.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    // Ambient Glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.2f), Color.Transparent),
                            radius = size.toPx() * 0.9f
                        )
                    )
                }
        )

        // Inner Avatar with 2.dp Gap
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        preset?.gradient ?: when (genderFallback) {
                            GenderType.FEMALE -> listOf(Color(0xFF2D0A1D), Color(0xFF4C1D3B))
                            else -> listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                        }
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                bitmap != null -> {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                preset != null -> {
                    Icon(
                        imageVector = preset.icon,
                        contentDescription = preset.label,
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.55f),
                    )
                }
                !photoUrl.isNullOrBlank() && !photoUrl.startsWith("preset://") -> {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    // Stylized Gamer Silhouette instead of plain letter
                    GamerSilhouette(gender = genderFallback, avatarSize = size)
                }
            }
        }
    }
}

@Composable
private fun GamerSilhouette(gender: GenderType, avatarSize: Dp) {
    val accentColor = when (gender) {
        GenderType.FEMALE -> Rose400
        else -> Cyan400
    }
    
    Canvas(modifier = Modifier.size(avatarSize * 0.85f)) {
        val w = this.size.width
        val h = this.size.height
        
        // Head silhouette
        drawCircle(
            color = accentColor.copy(alpha = 0.15f),
            radius = w * 0.28f,
            center = Offset(w / 2, h * 0.42f)
        )
        
        // Body/Shoulders
        val bodyPath = Path().apply {
            moveTo(w * 0.15f, h * 0.95f)
            quadraticBezierTo(w * 0.15f, h * 0.65f, w * 0.5f, h * 0.65f)
            quadraticBezierTo(w * 0.85f, h * 0.65f, w * 0.85f, h * 0.95f)
            close()
        }
        drawPath(bodyPath, color = accentColor.copy(alpha = 0.15f))

        // Gaming Headset Band
        drawArc(
            color = accentColor,
            startAngle = 185f,
            sweepAngle = 170f,
            useCenter = false,
            size = Size(w * 0.54f, h * 0.45f),
            topLeft = Offset(w * 0.23f, h * 0.22f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Earpads
        drawRoundRect(
            color = accentColor,
            topLeft = Offset(w * 0.2f, h * 0.38f),
            size = Size(w * 0.12f, h * 0.22f),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        drawRoundRect(
            color = accentColor,
            topLeft = Offset(w * 0.68f, h * 0.38f),
            size = Size(w * 0.12f, h * 0.22f),
            cornerRadius = CornerRadius(4.dp.toPx())
        )
        
        // Visor/Eyes Glow
        drawLine(
            color = accentColor,
            start = Offset(w * 0.38f, h * 0.45f),
            end = Offset(w * 0.62f, h * 0.45f),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        // Headset Mic (Only for Male for slight variation)
        if (gender == GenderType.MALE) {
            drawLine(
                color = accentColor,
                start = Offset(w * 0.25f, h * 0.58f),
                end = Offset(w * 0.35f, h * 0.65f),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

private enum class GenderType {
    MALE, FEMALE, NEUTRAL
}
