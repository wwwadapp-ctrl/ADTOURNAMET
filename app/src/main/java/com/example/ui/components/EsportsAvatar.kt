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
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import com.example.R

data class AvatarPreset(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    @DrawableRes val image: Int = 0,
    val female: Boolean = false,
)

private val AvatarBg = listOf(Color(0xFF2C3AB0), Color(0xFF0C1150))

/** 12 picture avatars: 6 male (m1..m6) then 6 female (f1..f6). */
val avatarPresets = listOf(
    AvatarPreset("preset://m1", "Gamer", Icons.Default.SportsEsports, AvatarBg, R.drawable.av_m1),
    AvatarPreset("preset://m2", "Cool", Icons.Default.Bolt, AvatarBg, R.drawable.av_m2),
    AvatarPreset("preset://m3", "Champion", Icons.Default.EmojiEvents, AvatarBg, R.drawable.av_m3),
    AvatarPreset("preset://m4", "Smart", Icons.Default.Star, AvatarBg, R.drawable.av_m4),
    AvatarPreset("preset://m5", "King", Icons.Default.Star, AvatarBg, R.drawable.av_m5),
    AvatarPreset("preset://m6", "Fan", Icons.Default.Star, AvatarBg, R.drawable.av_m6),
    AvatarPreset("preset://f1", "Gamer", Icons.Default.SportsEsports, AvatarBg, R.drawable.av_f1, female = true),
    AvatarPreset("preset://f2", "Hijabi", Icons.Default.Star, AvatarBg, R.drawable.av_f2, female = true),
    AvatarPreset("preset://f3", "Sporty", Icons.Default.Bolt, AvatarBg, R.drawable.av_f3, female = true),
    AvatarPreset("preset://f4", "Smart", Icons.Default.Star, AvatarBg, R.drawable.av_f4, female = true),
    AvatarPreset("preset://f5", "Queen", Icons.Default.EmojiEvents, AvatarBg, R.drawable.av_f5, female = true),
    AvatarPreset("preset://f6", "Traditional", Icons.Default.Star, AvatarBg, R.drawable.av_f6, female = true),
)

/** Avatar ids saved before the redesign still show a picture. */
private val legacyAvatarIds = mapOf(
    "preset://dragon" to "preset://m6",
    "preset://cyber" to "preset://m1",
    "preset://ninja" to "preset://m2",
    "preset://crown" to "preset://m5",
    "preset://sniper" to "preset://m4",
    "preset://shield" to "preset://m3",
)

/** Finds the avatar for a saved "preset://..." id (old ids included). Returns null for photos. */
fun findAvatarPreset(id: String?): AvatarPreset? {
    if (id.isNullOrBlank() || !id.startsWith("preset://")) return null
    val realId = legacyAvatarIds[id] ?: id
    return avatarPresets.firstOrNull { it.id == realId }
}

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

    val preset = remember(photoUrl) { findAvatarPreset(photoUrl) }

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
                preset != null && preset.image != 0 -> {
                    Image(
                        painter = painterResource(id = preset.image),
                        contentDescription = preset.label,
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
                    // Default picture avatar by guessed gender (first male / first female avatar)
                    Image(
                        painter = painterResource(id = if (genderFallback == GenderType.FEMALE) R.drawable.av_f1 else R.drawable.av_m1),
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}

private enum class GenderType {
    MALE, FEMALE, NEUTRAL
}
