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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
                "৳$amount",
                color = textColor,
                fontSize = (chipSize.value * 0.32).sp,
                fontWeight = FontWeight.Black
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Big Selected Chip
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .drawBehind {
                        drawCircle(Color(0xFFFFD700).copy(alpha = 0.1f), radius = size.width / 1.6f)
                    },
                contentAlignment = Alignment.Center
            ) {
                CasinoPokerChip(amount = selectedBet, isSelected = true, onClick = {}, enabled = true, chipSize = 72.dp)
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    betOptions.forEach { amount ->
                        CasinoPokerChip(
                            amount = amount,
                            isSelected = selectedBet == amount,
                            onClick = { onBetSelected(amount) },
                            chipSize = 38.dp,
                            enabled = isActionEnabled || selectedBet == amount
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onActionClick,
                    enabled = isActionEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(8.dp, RoundedCornerShape(25.dp)),
                    shape = RoundedCornerShape(25.dp),
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
                            actionButtonText,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White
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
    Surface(
        modifier = Modifier
            .size(56.dp)
            .clickable(enabled = enabled) { onClick(amount) },
        shape = CircleShape,
        color = if (isSelected) accentColor else Color.Transparent,
        border = BorderStroke(2.dp, accentColor),
        shadowElevation = if (isSelected) 10.dp else 0.dp
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
                text = "৳$amount",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                color = if (isSelected) Color.Black else accentColor
            )
        }
    }
}

@Composable
fun TransferModal(
    uid: String,
    userName: String,
    mainBalance: Double,
    gameBalance: Double,
    accentColor: Color,
    accentColorLight: Color,
    backgroundColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    var selectedType by remember { mutableStateOf("ADD") } // ADD or WITHDRAW
    var transferAmount by remember { mutableStateOf("") }

    // Helper to convert Bengali numerals to English
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
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentStep == 1) {
                    Text(
                        "মিনি গেম ওয়ালেট",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            brush = Brush.verticalGradient(listOf(accentColorLight, accentColor))
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    TransferActionCard(
                        title = "টাকা যোগ করুন (Add Money)",
                        subtitle = "মেইন ব্যালেন্স থেকে গেম ওয়ালেটে টাকা আনুন",
                        icon = Icons.Default.AddCircle,
                        color = accentColor,
                        onClick = {
                            selectedType = "ADD"
                            currentStep = 2
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TransferActionCard(
                        title = "টাকা তুলুন (Withdraw to Main)",
                        subtitle = "গেম ব্যালেন্স থেকে জিতে নেওয়া টাকা মেইন ওয়ালেটে নিন",
                        icon = Icons.Default.ArrowUpward,
                        color = Color(0xFF4CAF50),
                        onClick = {
                            selectedType = "WITHDRAW"
                            currentStep = 2
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    TournamentButton(text = "বন্ধ করুন", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentStep = 1 }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text(
                            text = if (selectedType == "ADD") "গেম ওয়ালেটে যোগ" else "মেইন ওয়ালেটে উত্তোলন",
                            style = MaterialTheme.typography.titleMedium,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LockedField(label = "ইউজার আইডি (UID)", value = uid)
                    Spacer(modifier = Modifier.height(10.dp))
                    LockedField(label = "ইউজারের নাম", value = userName)
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (selectedType == "ADD") "আপনার মেইন ব্যালেন্স: ৳${"%.2f".format(mainBalance)}" 
                                   else "আপনার গেম ব্যালেন্স: ৳${"%.2f".format(gameBalance)}",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor
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
                                    text = "৳$amount",
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (transferAmount == amount.toString()) accentColor else Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = transferAmount,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' || c in '০'..'৯' }) transferAmount = it },
                        label = { Text("পরিমাণ (৳)", color = Color.White.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = accentColor, unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    if (transferAmount.isNotBlank() && !isAmountValid) {
                        Text(
                            text = if (amountDouble > sourceBalance) "ব্যালেন্স অপর্যাপ্ত!" else "সঠিক পরিমাণ লিখুন",
                            color = Color.Red,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onConfirm(amountDouble, selectedType) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = isAmountValid,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("কনফার্ম রিকোয়েস্ট", color = Color.Black, fontWeight = FontWeight.Bold)
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
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun LockedField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.03f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Text(text = value, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
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
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
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
                    text = if (isWin) "অভিনন্দন!" else "দুঃখিত!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        brush = if (isWin) Brush.verticalGradient(listOf(accentColorLight, accentColor)) else null
                    ),
                    color = if (isWin) Color.Unspecified else Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isWin) "আপনি ৳${"%.2f".format(amount)} জিতেছেন!" else "এবার ভাগ্য সহায় হয়নি। আবার চেষ্টা করুন!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(28.dp))
                TournamentButton(text = "বন্ধ করুন", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
