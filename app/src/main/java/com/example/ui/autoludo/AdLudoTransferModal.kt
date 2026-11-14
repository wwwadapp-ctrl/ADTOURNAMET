package com.example.ui.autoludo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.i18n.BengaliStrings
import com.example.core.i18n.LocalAppStrings
import com.example.core.i18n.digits
import com.example.core.i18n.taka

// AD premium game-wallet transfer box for Auto Ludo only.
// Same behaviour as TransferModal (two steps, same amount checks, same onConfirm); only the look is new.
// TransferModal itself is untouched and still used by the other mini games.

private val TmGoldText = Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFFFC94A), Color(0xFFC98A10)))

@Composable
fun AdLudoTransferModal(
    uid: String,
    userName: String,
    userPhone: String,
    mainBalance: Double,
    gameBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    val strings = LocalAppStrings.current
    val isBn = strings === BengaliStrings
    var currentStep by remember { mutableStateOf(1) }
    var selectedType by remember { mutableStateOf("ADD") } // ADD or WITHDRAW
    var transferAmount by remember { mutableStateOf("") }

    // Helper to convert Bengali numerals to English for logic (same as TransferModal)
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
        TmFrame {
            if (currentStep == 1) {
                TmDivider(Modifier.align(Alignment.CenterHorizontally))
                Text(
                    text = strings.mgWalletTitle,
                    style = TextStyle(
                        brush = TmGoldText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        shadow = Shadow(Color(0xFF3A2600), Offset(0f, 4f), 0f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                TmActionCard(
                    title = strings.mgAddMoneyTitle,
                    subtitle = strings.mgAddMoneySubtitle,
                    icon = Icons.Default.Add,
                    iconBrush = Brush.radialGradient(listOf(Color(0xFFFFF3C4), Color(0xFFFFC94A), Color(0xFFC98A10))),
                    iconTint = Color(0xFF3A2300),
                    border = Color(0xFFC9A04A),
                    onClick = {
                        selectedType = "ADD"
                        currentStep = 2
                    }
                )
                TmActionCard(
                    title = strings.mgWithdrawMoneyTitle,
                    subtitle = strings.mgWithdrawMoneySubtitle,
                    icon = Icons.Default.ArrowUpward,
                    iconBrush = Brush.radialGradient(listOf(Color(0xFFC8FFE0), Color(0xFF3FD585), Color(0xFF0B8A42))),
                    iconTint = Color(0xFF06341A),
                    border = Color(0xFF3FA86A),
                    onClick = {
                        selectedType = "WITHDRAW"
                        currentStep = 2
                    }
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF34406E), Color(0xFF1A2048), Color(0xFF10142E))))
                        .border(1.5.dp, Color(0xFFC9A04A), RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button) { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(strings.mgCloseButton, style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White))
                }
            } else {
                // header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { currentStep = 1 }) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color(0xFF2E4BB8), Color(0xFF0F1A52))))
                                .border(2.dp, Color(0xFFE8B23A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(
                        text = if (selectedType == "ADD") strings.mgTransferAddTitle else strings.mgTransferWithdrawTitle,
                        style = TextStyle(brush = TmGoldText, fontSize = 21.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // locked details (same values and fallbacks as TransferModal)
                val displayUserName = if (userName.isNotBlank() && userName != "Player" && userName != "User") userName
                    else (com.example.core.firebase.FirebaseManager.getAuth()?.currentUser?.displayName?.ifBlank { null } ?: "User")
                val displayPhone = if (userPhone.isNotBlank()) userPhone
                    else (com.example.core.firebase.FirebaseManager.getAuth()?.currentUser?.phoneNumber?.ifBlank { null } ?: "")
                val rows = buildList {
                    add(strings.mgLabelUid to uid)
                    add(strings.mgLabelUserName to displayUserName)
                    if (displayPhone.isNotBlank()) add(strings.mgLabelMobile to displayPhone)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF141B44), Color(0xFF0E1438))))
                        .border(1.dp, Color(0x8CC9A04A), RoundedCornerShape(14.dp))
                ) {
                    rows.forEachIndexed { i, (label, value) ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x40C9A04A)))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFAEB6D8)), maxLines = 1)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                value,
                                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE6ECFF), textAlign = TextAlign.End),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF8C97C4), modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // balance with direction
                val fromWord = if (selectedType == "ADD") (if (isBn) "মেইন" else "Main") else (if (isBn) "গেম" else "Game")
                val toWord = if (selectedType == "ADD") (if (isBn) "গেম" else "Game") else (if (isBn) "মেইন" else "Main")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF3A2E10), Color(0xFF1E1606))))
                        .border(1.5.dp, Color(0xFFC9A04A), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            (if (selectedType == "ADD") strings.mgMainBalance("") else strings.mgGameBalance("")).trimEnd(' ', ':'),
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFF3E6C0)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TmFlowChip(fromWord, highlighted = false)
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFFFFC94A),
                                modifier = Modifier.padding(horizontal = 6.dp).size(16.dp)
                            )
                            TmFlowChip(toWord, highlighted = true)
                        }
                    }
                    Text(
                        strings.taka(sourceBalance),
                        style = TextStyle(brush = TmGoldText, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }

                // quick amounts
                val quickAmounts = listOf(10L, 20L, 50L, 100L)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickAmounts.forEach { amount ->
                        val on = transferAmount == amount.toString()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (on) Brush.radialGradient(listOf(Color(0xFFFFF3C4), Color(0xFFFFC94A), Color(0xFFD98A10)))
                                    else Brush.verticalGradient(listOf(Color(0xFF2A3156), Color(0xFF151A33)))
                                )
                                .border(1.5.dp, if (on) Color(0xFFFFF3C4) else Color(0xFFC9A04A), RoundedCornerShape(12.dp))
                                .clickable(role = Role.Button) { transferAmount = amount.toString() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "৳${strings.digits(amount.toString())}",
                                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (on) Color(0xFF3A2300) else Color(0xFFFFE7A0)),
                                maxLines = 1
                            )
                        }
                    }
                }

                // amount field
                BasicTextField(
                    value = transferAmount,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' || c in '০'..'৯' }) transferAmount = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White),
                    cursorBrush = SolidColor(Color(0xFFFFC107)),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.verticalGradient(listOf(Color(0xFF2A3156), Color(0xFF151A33))))
                                .border(1.5.dp, if (transferAmount.isNotBlank()) Color(0xFFFFC107) else Color(0xFFC9A04A), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("৳", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFC94A)))
                            Spacer(Modifier.width(10.dp))
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (transferAmount.isEmpty()) {
                                    Text(
                                        if (isBn) "পরিমাণ লিখুন" else "Enter amount",
                                        style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color(0xFFAEB6D8))
                                    )
                                }
                                inner()
                            }
                        }
                    }
                )
                if (transferAmount.isNotBlank() && !isAmountValid) {
                    Text(
                        text = if (amountDouble > sourceBalance) strings.mgErrorInsufficient else strings.mgErrorInvalidAmount,
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB4A8)),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // confirm
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isAmountValid) 1f else 0.45f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFF9A6400))))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFFFFE29A), Color(0xFFF6B73C), Color(0xFFD98A10))))
                            .clickable(enabled = isAmountValid, role = Role.Button) { onConfirm(amountDouble, selectedType) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(start = 10.dp, end = 10.dp, top = 3.dp)
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0f))))
                        )
                        Text(strings.mgConfirmRequest, style = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3A2300)))
                    }
                }
            }
        }
    }
}

@Composable
private fun TmFrame(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .border(2.dp, Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFC98A10), Color(0xFFFFE7A0))), RoundedCornerShape(22.dp))
            .padding(2.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1E2E7E), Color(0xFF131E5A), Color(0xFF0D1648))))
            .border(2.dp, Color(0xFF8A6414), RoundedCornerShape(20.dp))
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(8.dp)
                .border(1.dp, Color(0x8CF0BE50), RoundedCornerShape(14.dp))
        )
        TmCorner(Modifier.align(Alignment.TopStart).padding(6.dp), flipX = false, flipY = false)
        TmCorner(Modifier.align(Alignment.TopEnd).padding(6.dp), flipX = true, flipY = false)
        TmCorner(Modifier.align(Alignment.BottomStart).padding(6.dp), flipX = false, flipY = true)
        TmCorner(Modifier.align(Alignment.BottomEnd).padding(6.dp), flipX = true, flipY = true)
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
private fun TmCorner(modifier: Modifier, flipX: Boolean, flipY: Boolean) {
    Canvas(
        modifier = modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = if (flipX) -1f else 1f
                scaleY = if (flipY) -1f else 1f
            }
    ) {
        val s = size.width / 44f
        val path = Path().apply {
            moveTo(40f * s, 4f * s); cubicTo(20f * s, 4f * s, 4f * s, 20f * s, 4f * s, 40f * s)
            moveTo(28f * s, 4f * s); cubicTo(28f * s, 18f * s, 14f * s, 18f * s, 15f * s, 13f * s)
            cubicTo(16f * s, 9f * s, 21f * s, 10f * s, 20f * s, 14f * s)
            moveTo(4f * s, 28f * s); cubicTo(18f * s, 28f * s, 18f * s, 14f * s, 13f * s, 15f * s)
            cubicTo(9f * s, 16f * s, 10f * s, 21f * s, 14f * s, 20f * s)
        }
        drawPath(path, Color(0xFFE8B23A), style = Stroke(width = 1.5f * s, cap = StrokeCap.Round))
        drawCircle(Color(0xFFFFE08A), 2.4f * s, Offset(15f * s, 15f * s))
    }
}

@Composable
private fun TmDivider(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.width(200.dp).height(22.dp)) {
        val s = size.width / 230f
        val gold = Color(0xFFE8B23A)
        val line = Color(0xFFC98A10)
        drawLine(line, Offset(2f * s, 14f * s), Offset(82f * s, 14f * s), strokeWidth = 1.4f * s, cap = StrokeCap.Round)
        drawLine(line, Offset(148f * s, 14f * s), Offset(228f * s, 14f * s), strokeWidth = 1.4f * s, cap = StrokeCap.Round)
        val curls = Path().apply {
            moveTo(84f * s, 14f * s); cubicTo(92f * s, 6f * s, 100f * s, 6f * s, 104f * s, 12f * s)
            cubicTo(100f * s, 16f * s, 94f * s, 16f * s, 92f * s, 12f * s)
            moveTo(146f * s, 14f * s); cubicTo(138f * s, 6f * s, 130f * s, 6f * s, 126f * s, 12f * s)
            cubicTo(130f * s, 16f * s, 136f * s, 16f * s, 138f * s, 12f * s)
        }
        drawPath(curls, gold, style = Stroke(width = 1.5f * s, cap = StrokeCap.Round))
        val leaf = Path().apply {
            moveTo(115f * s, 2f * s); cubicTo(119f * s, 7f * s, 119f * s, 13f * s, 115f * s, 18f * s)
            cubicTo(111f * s, 13f * s, 111f * s, 7f * s, 115f * s, 2f * s); close()
        }
        drawPath(leaf, Color(0xFFFFC107))
        drawCircle(Color(0xFFFFE08A), 2f * s, Offset(106f * s, 15f * s))
        drawCircle(Color(0xFFFFE08A), 2f * s, Offset(124f * s, 15f * s))
    }
}

@Composable
private fun TmActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBrush: Brush,
    iconTint: Color,
    border: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2A3156), Color(0xFF151A33))))
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button) { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(iconBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White))
            Text(subtitle, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFC9D3FF), lineHeight = 19.sp))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFE8B23A), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun TmFlowChip(text: String, highlighted: Boolean) {
    Text(
        text,
        style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = if (highlighted) Color(0xFFFFE7A0) else Color(0xFFC9D3FF)),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (highlighted) Color(0x1FFFC107) else Color(0x0FFFFFFF))
            .border(1.dp, if (highlighted) Color(0xFFE8B23A) else Color(0x80C9A04A), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 2.dp)
    )
}
