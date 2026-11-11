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
fun CasinoPokerChip(
    amount: Int,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: (Int) -> Unit,
    chipSize: androidx.compose.ui.unit.Dp = 50.dp,
    accentColor: Color = Color(0xFFFFD700) // Gold
) {
    val strings = LocalAppStrings.current
    val chipColor = if (isSelected) Color(0xFF4B0082) else Color.Transparent
    val textColor = if (isSelected) Color.White else accentColor

    Surface(
        modifier = Modifier
            .size(chipSize)
            .clickable(enabled = enabled) { onClick(amount) },
        shape = CircleShape,
        color = chipColor,
        border = BorderStroke(1.5.dp, accentColor),
        shadowElevation = if (isSelected) 8.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Dashed inner ring
                drawCircle(
                    color = accentColor.copy(alpha = 0.4f),
                    radius = size.width / 2.4f,
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                )
                
                // Border dots for luxury look
                if (isSelected) {
                    val dotRadius = 1.5.dp.toPx()
                    val count = 8
                    for (i in 0 until count) {
                        val angle = (i * (360f / count)) * (Math.PI / 180).toFloat()
                        val dx = (size.width / 2) + (size.width / 2.2f) * kotlin.math.cos(angle.toDouble()).toFloat()
                        val dy = (size.height / 2) + (size.width / 2.2f) * kotlin.math.sin(angle.toDouble()).toFloat()
                        drawCircle(accentColor, dotRadius, Offset(dx, dy))
                    }
                }
            }
            Text(
                text = "৳${strings.digits(amount.toString())}",
                color = textColor,
                fontSize = (chipSize.value * 0.32).sp,
                fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
fun CasinoBettingConsole(
    selectedBet: Int,
    onBetSelected: (Int) -> Unit,
    actionButtonText: String,
    isActionEnabled: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    betOptions: List<Int> = listOf(1, 3, 5, 8, 10)
) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Big Selected Chip
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .drawBehind {
                        drawCircle(Color(0xFFFFD700).copy(alpha = 0.1f), radius = size.width / 1.6f)
                    },
                contentAlignment = Alignment.Center
            ) {
                CasinoPokerChip(amount = selectedBet, isSelected = true, onClick = {}, enabled = true, chipSize = 64.dp)
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    betOptions.forEach { amount ->
                        CasinoPokerChip(
                            amount = amount,
                            isSelected = selectedBet == amount,
                            onClick = { onBetSelected(amount) },
                            chipSize = 36.dp,
                            enabled = isActionEnabled || selectedBet == amount
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onActionClick,
                    enabled = isActionEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(6.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isActionEnabled) Brush.horizontalGradient(listOf(Color(0xFF5E17EB), Color(0xFF8B5CF6)))
                                else Brush.horizontalGradient(listOf(Color.Gray, Color.DarkGray))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = actionButtonText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.ExtraBold,
                                fontSize = 15.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PokerChip(
    amount: Long,
    isSelected: Boolean,
    enabled: Boolean,
    accentColor: Color,
    onClick: (Long) -> Unit
) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = Modifier
            .size(54.dp)
            .clickable(enabled = enabled) { onClick(amount) },
        shape = CircleShape,
        color = if (isSelected) accentColor else Color.Transparent,
        border = BorderStroke(2.dp, accentColor),
        shadowElevation = if (isSelected) 8.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = if (isSelected) Color.Black.copy(alpha = 0.1f) else accentColor.copy(alpha = 0.3f),
                    radius = size.width / 2.2f,
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
                )
            }
            Text(
                text = "৳${strings.digits(amount.toString())}",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
                    fontSize = 13.sp
                ),
                color = if (isSelected) Color.Black else accentColor,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

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
    val strings = LocalAppStrings.current
    var currentStep by remember { mutableStateOf(1) }
    var selectedType by remember { mutableStateOf("ADD") } // ADD or WITHDRAW
    var transferAmount by remember { mutableStateOf("") }

    // Helper to convert Bengali numerals to English for logic
    fun String.toEnglishDigits(): String {
        val bengali = listOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        var result = this
        bengali.forEachIndexed { index, char ->
            result = result.replace(char, '0' + index)
        }
        return result
    }

    val sourceBalance = if (selectedType == "ADD") mainBalance else gameBalance
    val cleanAmount = transferAmount.toEnglishDigits()
    val amountDouble = cleanAmount.toDoubleOrNull() ?: 0.0
    val isAmountValid = amountDouble > 0 && amountDouble <= sourceBalance

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = backgroundColor,
            border = BorderStroke(2.dp, accentColor)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentStep == 1) {
                    Text(
                        text = strings.mgWalletTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            brush = Brush.verticalGradient(listOf(accentColorLight, accentColor))
                        )
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    TransferActionCard(
                        title = strings.mgAddMoneyTitle,
                        subtitle = strings.mgAddMoneySubtitle,
                        icon = Icons.Default.AddCircle,
                        color = accentColor,
                        onClick = {
                            selectedType = "ADD"
                            currentStep = 2
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TransferActionCard(
                        title = strings.mgWithdrawMoneyTitle,
                        subtitle = strings.mgWithdrawMoneySubtitle,
                        icon = Icons.Default.ArrowUpward,
                        color = Color(0xFF4CAF50),
                        onClick = {
                            selectedType = "WITHDRAW"
                            currentStep = 2
                        }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    TournamentButton(
                        text = strings.mgCloseButton,
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentStep = 1 }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = Color.White)
                        }
                        Text(
                            text = if (selectedType == "ADD") strings.mgTransferAddTitle else strings.mgTransferWithdrawTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LockedField(label = strings.mgLabelUid, value = uid)
                    Spacer(modifier = Modifier.height(8.dp))
                    val displayUserName = if (userName.isNotBlank() && userName != "Player" && userName != "User") userName 
                        else (com.example.core.firebase.FirebaseManager.getAuth()?.currentUser?.displayName?.ifBlank { null } ?: "User")
                    LockedField(label = strings.mgLabelUserName, value = displayUserName)
                    
                    val displayPhone = if (userPhone.isNotBlank()) userPhone 
                        else (com.example.core.firebase.FirebaseManager.getAuth()?.currentUser?.phoneNumber?.ifBlank { null } ?: "")
                    
                    if (displayPhone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LockedField(label = strings.mgLabelMobile, value = displayPhone)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (selectedType == "ADD") strings.mgMainBalance(strings.taka(mainBalance))
                                   else strings.mgGameBalance(strings.taka(gameBalance)),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                            color = accentColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    val quickAmounts = listOf(10L, 20L, 50L, 100L)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        quickAmounts.forEach { amount ->
                            Surface(
                                modifier = Modifier.weight(1f).clickable { transferAmount = amount.toString() },
                                shape = RoundedCornerShape(8.dp),
                                color = if (transferAmount == amount.toString()) Color(0xFF5E17EB) else Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, if (transferAmount == amount.toString()) accentColor else Color.White.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    text = "৳${strings.digits(amount.toString())}",
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = if (transferAmount == amount.toString()) accentColor else Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = transferAmount,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' || c in '০'..'৯' }) transferAmount = it },
                        label = { Text(strings.mgAmountPlaceholder, color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = accentColor, unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    if (transferAmount.isNotBlank() && !isAmountValid) {
                        Text(
                            text = if (amountDouble > sourceBalance) strings.mgErrorInsufficient else strings.mgErrorInvalidAmount,
                            color = Color.Red,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { onConfirm(amountDouble, selectedType) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = isAmountValid,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = strings.mgConfirmRequest,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransferActionCard(title: String, subtitle: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.2.dp, color.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = Color.White.copy(alpha = 0.6f),
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun LockedField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp), color = Color.White.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.03f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun WinLossDialog(
    isWin: Boolean,
    amount: Double,
    backgroundColor: Color,
    accentColor: Color,
    accentColorLight: Color,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = backgroundColor,
            border = BorderStroke(2.dp, accentColor)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isWin) strings.mgWinTitle else strings.mgLossTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        brush = if (isWin) Brush.verticalGradient(listOf(accentColorLight, accentColor)) else null
                    ),
                    color = if (isWin) Color.Unspecified else Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isWin) strings.mgWinMessage(strings.taka(amount)) else strings.mgLossMessage,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        lineHeight = 24.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(28.dp))
                TournamentButton(
                    text = strings.mgCloseButton,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
