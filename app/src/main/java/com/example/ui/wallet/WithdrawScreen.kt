package com.example.ui.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.example.R
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import com.example.ui.components.UnifiedEsportsTopBar
import com.example.ui.components.TournamentButton
import com.example.ui.components.TournamentCard
import com.example.ui.components.TournamentTextField
import com.example.ui.theme.*
import com.example.core.i18n.*
import com.example.ui.minigames.TransferModal

private val BkashPink = Color(0xFFE2136E)
private val NagadOrange = Color(0xFFF7941D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawScreen(
  viewModel: WalletViewModel,
  currentUser: UserEntity? = null,
  wallet: WalletEntity? = null,
  unreadNotificationsCount: Int = 0,
  onWalletClick: () -> Unit = {},
  onNotificationClick: () -> Unit = {},
  onProfileClick: () -> Unit = {},
  onAdminClick: (() -> Unit)? = null,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val uiState by viewModel.uiState.collectAsState()
  val appSettings by viewModel.appSettings.collectAsState()
  val availableBalance = uiState.wallet?.availableBalance ?: 0L

  fun getSavedNumberFor(method: String): String {
    return when (method) {
      "BKASH" -> currentUser?.savedBkashNumber?.takeIf { it.isNotBlank() }
        ?: currentUser?.effectiveMobile?.takeIf { it.isNotBlank() }
        ?: ""
      "NAGAD" -> currentUser?.savedNagadNumber?.takeIf { it.isNotBlank() }
        ?: currentUser?.effectiveMobile?.takeIf { it.isNotBlank() }
        ?: ""
      else -> currentUser?.effectiveMobile?.takeIf { it.isNotBlank() } ?: ""
    }
  }

  var selectedMethod by remember { mutableStateOf("BKASH") }
  val initialSavedNumber = remember(currentUser) { getSavedNumberFor("BKASH") }
  var recipientNumber by remember(currentUser) { mutableStateOf(initialSavedNumber) }
  var amountInput by remember { mutableStateOf("200") }
  var clientError by remember { mutableStateOf<String?>(null) }

  fun onSelectMethod(newMethod: String) {
    if (selectedMethod != newMethod) {
      selectedMethod = newMethod
      val savedForNewMethod = getSavedNumberFor(newMethod)
      if (savedForNewMethod.isNotBlank()) {
        recipientNumber = savedForNewMethod
      }
      clientError = null
    }
  }

  val amountNum = amountInput.toLongOrNull() ?: 0L
  val amountMinor = amountNum * 100L
  val methodLabel = if (selectedMethod == "BKASH") "bKash" else "Nagad"
  val savedNumber = getSavedNumberFor(selectedMethod)
  val isUsingSavedNumber = recipientNumber.isNotBlank() && recipientNumber == savedNumber

  Scaffold(
    topBar = {
      UnifiedEsportsTopBar(
        currentUser = currentUser,
        wallet = wallet,
        unreadNotificationsCount = unreadNotificationsCount,
        onWalletClick = onWalletClick,
        onNotificationClick = onNotificationClick,
        onProfileClick = onProfileClick,
        onAdminClick = onAdminClick
      )
    },
    containerColor = DeepNavyBg,
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .wlBackground()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      if (uiState.withdrawalSubmitted) {
        WdSuccessCard(
          title = strings.withdrawSuccessTitle,
          message = strings.withdrawSuccessDesc(strings.taka(amountNum.toDouble()), methodLabel),
          statusLabel = strings.withdrawStatusLabel,
          statusText = strings.withdrawStatusPending,
          rows = listOf(
            Triple(strings.withdrawPayoutMethodLabel, methodLabel, null),
            Triple(strings.withdrawRecipientAccountLabel, recipientNumber, null),
          ),
          amountLabel = strings.withdrawRequestedAmountLabel,
          amountText = strings.taka(amountNum.toDouble()),
          buttonText = strings.withdrawBackToWallet,
          onButton = {
            viewModel.clearMessages()
            onNavigateBack()
          },
          modifier = Modifier.padding(top = 8.dp),
        )
        return@Column
      }

      // 2. PAGE IDENTITY
      WlPageTitle(title = strings.withdrawHeaderTitle, subtitle = strings.withdrawHeaderSubtitle, modifier = Modifier.padding(top = 4.dp))

      Spacer(modifier = Modifier.height(14.dp))

      // 3. AVAILABLE BALANCE CARD
      WlFrame(gold = true, pad = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 6.dp, top = 4.dp, bottom = 4.dp)) {
          Column(modifier = Modifier.weight(1f)) {
            WlPill(strings.withdrawAvailableBalanceLabel, Color(0xFF8CFF4A), listOf(Color(0xFF0E3A2A), Color(0xFF07241A)), Color(0xFF4ADE80), Color(0xFFB8FFC8))
            Spacer(modifier = Modifier.height(8.dp))
            val balText = strings.taka(uiState.wallet?.availableAmount ?: 0.0)
            WlText3D(balText, if (balText.length > 10) 24.sp else 30.sp, WlTone.GOLD)
          }
          WlCoinMedal(66)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 4 + 5. PAYMENT METHOD + AMOUNT
      WlFrame(gold = true, pad = 14.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp)) {
          DpStepHead(number = strings.digits("1"), title = strings.withdrawSelectMethodLabel)
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val isBkash = selectedMethod == "BKASH"
            DpMethodTile(
              name = "bKash", badge = if (isBkash) strings.withdrawMethodSelected else strings.withdrawMethodPersonal,
              logo = R.drawable.ic_bkash, brand = BkashPink, selected = isBkash,
              onClick = { onSelectMethod("BKASH") }, modifier = Modifier.weight(1f),
            )
            val isNagad = selectedMethod == "NAGAD"
            DpMethodTile(
              name = "Nagad", badge = if (isNagad) strings.withdrawMethodSelected else strings.withdrawMethodPersonal,
              logo = R.drawable.ic_nagad, brand = NagadOrange, selected = isNagad,
              onClick = { onSelectMethod("NAGAD") }, modifier = Modifier.weight(1f),
            )
          }
          Spacer(modifier = Modifier.height(14.dp))
          Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x8C0A0E3C))
              .border(1.dp, Color(0x8CE8B84A), RoundedCornerShape(18.dp)).padding(12.dp),
          ) {
            val minAmount = (appSettings?.minWithdrawalTaka ?: 200.0).toLong()
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
              Image(painter = painterResource(id = R.drawable.wl_withdraw), contentDescription = null, modifier = Modifier.size(24.dp))
              Spacer(modifier = Modifier.width(6.dp))
              WlText3D(strings.withdrawAmountLabel, 14.sp, WlTone.GOLD, modifier = Modifier.weight(1f))
              Text(
                text = strings.withdrawLimitNotice(strings.number(minAmount)), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFF3C4), maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x26FFD45A)).border(1.dp, Color(0x99FFD45A), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            DpField(
              label = strings.withdrawEnterAmountHint,
              value = amountInput,
              onValueChange = {
                amountInput = it.filter { char -> char.isDigit() }
                clientError = null
              },
              placeholder = strings.withdrawEnterAmountPlaceholder,
              icon = Icons.Default.CurrencyExchange,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              tag = "withdraw_amount_input",
              big = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf(200, 500, 1000, 2000, 5000).forEach { chipAmount ->
                DpAmountChip(
                  text = strings.takaWhole(chipAmount.toDouble()),
                  selected = amountInput == chipAmount.toString(),
                  onClick = {
                    amountInput = chipAmount.toString()
                    clientError = null
                  },
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 6 + 7 + 8. NUMBER, SUMMARY, GUIDELINES
      WlFrame(gold = true, pad = 14.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            WlText3D(strings.withdrawRecipientLabel(methodLabel.uppercase()), 14.sp, WlTone.CREAM, modifier = Modifier.weight(1f))
            if (savedNumber.isNotBlank() && !isUsingSavedNumber) {
              WdSmallButton(
                text = strings.withdrawUseSavedNumber,
                onClick = {
                  recipientNumber = savedNumber
                  clientError = null
                },
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          DpField(
            label = strings.withdrawRecipientHint(methodLabel),
            value = recipientNumber,
            onValueChange = {
              if (it.length <= 11) {
                recipientNumber = it.filter { char -> char.isDigit() }
                clientError = null
              }
            },
            placeholder = strings.withdrawRecipientPlaceholder,
            icon = Icons.Default.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            tag = "withdraw_recipient_input",
          )
          Spacer(modifier = Modifier.height(9.dp))
          if (isUsingSavedNumber) {
            DpNotice(text = strings.withdrawVerifiedBadge(methodLabel), icon = Icons.Default.VerifiedUser, error = false)
          } else {
            DpNotice(text = strings.withdrawSecurityHint(methodLabel), icon = Icons.Default.Shield, error = false)
          }
          Spacer(modifier = Modifier.height(14.dp))
          WdSummaryCard(
            title = strings.withdrawSummaryTitle,
            rows = listOf(
              Triple(strings.withdrawSummaryAmount, strings.taka(amountNum.toDouble()), null),
              Triple(strings.withdrawSummaryMethod, methodLabel, if (selectedMethod == "BKASH") BkashPink else NagadOrange),
              Triple(strings.withdrawSummaryNumber, recipientNumber.ifBlank { strings.optional }, null),
            ),
            totalLabel = strings.withdrawSummaryTotal,
            totalText = strings.taka(amountNum.toDouble()),
          )
          Spacer(modifier = Modifier.height(14.dp))
          val minWithdrawal = (appSettings?.minWithdrawalTaka ?: 200.0).toLong()
          DpRules(
            title = strings.withdrawSecurityTitle,
            steps = listOf(
              strings.withdrawGuideline1,
              strings.withdrawGuideline2(strings.number(minWithdrawal)),
              strings.withdrawGuideline3,
              strings.withdrawGuideline4,
              strings.withdrawGuideline5,
            ).mapIndexed { i, g -> strings.digits((i + 1).toString()) to g.removePrefix("• ").removePrefix("•").trim() },
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 10. ERROR / VALIDATION MESSAGE
      val displayError = clientError ?: uiState.errorMessage
      if (displayError != null) {
        DpNotice(text = displayError, icon = Icons.Default.Warning, error = true, modifier = Modifier.padding(bottom = 12.dp))
      }

      // 9. MAIN CTA: "WITHDRAW NOW"
      val canSubmit = !uiState.isSubmitting && amountInput.isNotBlank() && recipientNumber.isNotBlank()
      DpBigButton(
        text = strings.withdrawNowButton,
        onClick = {
          if (!canSubmit) return@DpBigButton
          val minAmount = (appSettings?.minWithdrawalTaka ?: 200.0).toLong()
          if (amountNum < minAmount) {
            clientError = strings.withdrawMinError(strings.number(minAmount))
            return@DpBigButton
          }
          if (amountNum > 10000L) {
            clientError = strings.withdrawMaxError
            return@DpBigButton
          }
          if (amountMinor > (uiState.wallet?.availableBalance ?: 0L)) {
            clientError = strings.withdrawInsufficientError(strings.taka(uiState.wallet?.availableAmount ?: 0.0))
            return@DpBigButton
          }
          if (recipientNumber.length < 11) {
            clientError = strings.withdrawInvalidMobileError
            return@DpBigButton
          }
          viewModel.submitWithdrawal(
            amountMinorUnits = amountMinor,
            method = selectedMethod,
            recipientNumber = recipientNumber,
          )
        },
        isLoading = uiState.isSubmitting,
        tag = "submit_withdrawal_button",
        modifier = Modifier.alpha(if (canSubmit || uiState.isSubmitting) 1f else 0.55f),
      )

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
