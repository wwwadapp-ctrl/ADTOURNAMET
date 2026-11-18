package com.example.ui.minigames

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.i18n.*
import com.example.ui.components.TournamentButton

@Composable
fun TransferModal(
    uid: String,
    userName: String,
    userPhone: String,
    mainBalance: Double,
    gameBalance: Double,
    accentColor: Color,
    accentColorLight: Color,
    backgroundColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    // Same premium wallet box as Auto Ludo (same two steps, same amount checks, same onConfirm).
    // The color parameters are kept so the 4 mini game screens need no change.
    com.example.ui.autoludo.AdLudoTransferModal(
        uid = uid,
        userName = userName,
        userPhone = userPhone,
        mainBalance = mainBalance,
        gameBalance = gameBalance,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

