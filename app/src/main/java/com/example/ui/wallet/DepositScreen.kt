package com.example.ui.wallet

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import com.example.ui.components.*
import com.example.core.i18n.*
import com.example.ui.theme.*

private val BkashPink = Color(0xFFE2136E)
private val NagadOrange = Color(0xFFF7941D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositScreen(
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
  val isBengali = strings === BengaliStrings
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current

  var selectedMethod by remember { mutableStateOf("BKASH") }
  var amountInput by remember { mutableStateOf("100") }
  var senderNumber by remember { mutableStateOf("") }
  var trxId by remember { mutableStateOf("") }
  var clientError by remember { mutableStateOf<String?>(null) }
  var copiedFeedback by remember { mutableStateOf(false) }

  // Screenshot upload state
  var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
  var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    selectedImageUri = uri
  }

  LaunchedEffect(selectedImageUri) {
    selectedImageUri?.let { uri ->
      try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
          val bmp = BitmapFactory.decodeStream(stream)
          imageBitmap = bmp?.asImageBitmap()
        }
      } catch (_: Exception) {
        imageBitmap = null
      }
    } ?: run {
      imageBitmap = null
    }
  }

  val appSettings by viewModel.appSettings.collectAsState()

  val dynamicBkashNumber = remember(appSettings) {
    appSettings?.effectiveBkashNumber ?: "01700000000"
  }
  val dynamicNagadNumber = remember(appSettings) {
    appSettings?.effectiveNagadNumber ?: "01800000000"
  }

  val configuredPaymentNumber = if (selectedMethod == "BKASH") dynamicBkashNumber else dynamicNagadNumber
  val rawPaymentNumber = configuredPaymentNumber.replace("-", "").replace(" ", "").trim()

  LaunchedEffect(selectedMethod) {
    copiedFeedback = false
  }

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
      if (uiState.depositSubmitted) {
        DpSuccessDialog(
          title = strings.depositSubmittedTitle,
          message = strings.depositSubmittedDesc,
          rows = listOf(
            strings.depositMethodLabel to selectedMethod,
            strings.depositAmountSummaryLabel to strings.taka(amountInput.toDoubleOrNull() ?: 0.0),
            strings.senderMobileLabel to senderNumber,
            strings.trxIdLabel to trxId,
          ),
          statusLabel = strings.depositStatusLabel,
          statusText = strings.depositStatusPending,
          buttonText = strings.backToWallet,
          onButton = {
            viewModel.clearMessages()
            onNavigateBack()
          },
          onDismiss = {
            viewModel.clearMessages()
            onNavigateBack()
          },
          buttonTag = "deposit_success_confirm_btn",
          dialogTag = "deposit_success_dialog",
        )
      }

      // 2. PAGE IDENTITY
      WlPageTitle(title = strings.addMoneyTitle, subtitle = strings.depositSubtitle, modifier = Modifier.padding(top = 4.dp))

      Spacer(modifier = Modifier.height(14.dp))

      // 3. CURRENT BALANCE CARD
      val currentBalanceMinor = uiState.wallet?.availableBalance ?: 0L
      WlFrame(gold = true, pad = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 6.dp, top = 4.dp, bottom = 4.dp)) {
          Column(modifier = Modifier.weight(1f)) {
            WlPill(strings.balanceCurrent.uppercase(), Color(0xFF8CFF4A), listOf(Color(0xFF0E3A2A), Color(0xFF07241A)), Color(0xFF4ADE80), Color(0xFFB8FFC8))
            Spacer(modifier = Modifier.height(8.dp))
            val balText = strings.takaFromMinor(currentBalanceMinor)
            WlText3D(balText, if (balText.length > 10) 24.sp else 30.sp, WlTone.GOLD)
          }
          WlCoinMedal(66)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 4. PAYMENT METHODS (bKash & Nagad ONLY)
      val brand = if (selectedMethod == "BKASH") BkashPink else NagadOrange
      WlFrame(gold = true, pad = 14.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp)) {
          DpStepHead(number = strings.digits("1"), title = strings.stepSelectMethod)
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val isBkash = selectedMethod == "BKASH"
            DpMethodTile(
              name = "bKash", badge = if (isBkash) strings.selectedBadge else strings.personalBadge,
              logo = R.drawable.ic_bkash, brand = BkashPink, selected = isBkash,
              onClick = { selectedMethod = "BKASH"; copiedFeedback = false },
              modifier = Modifier.weight(1f),
            )
            val isNagad = selectedMethod == "NAGAD"
            DpMethodTile(
              name = "Nagad", badge = if (isNagad) strings.selectedBadge else strings.personalBadge,
              logo = R.drawable.ic_nagad, brand = NagadOrange, selected = isNagad,
              onClick = { selectedMethod = "NAGAD"; copiedFeedback = false },
              modifier = Modifier.weight(1f),
            )
          }
          Spacer(modifier = Modifier.height(12.dp))
          DpNumberBox(
            label = strings.sendMoneyLabel(if (selectedMethod == "BKASH") "bKash" else "Nagad"),
            officialText = strings.depositOfficialBadge,
            number = configuredPaymentNumber,
            copyText = if (copiedFeedback) strings.copied.uppercase() else strings.copy.uppercase(),
            copied = copiedFeedback,
            brand = brand,
            onCopy = {
              clipboardManager.setText(AnnotatedString(rawPaymentNumber))
              copiedFeedback = true
              android.widget.Toast.makeText(
                context,
                strings.depositNumberCopiedToast(selectedMethod, rawPaymentNumber),
                android.widget.Toast.LENGTH_SHORT
              ).show()
            },
            copyTag = "deposit_copy_number_btn",
          )
          Spacer(modifier = Modifier.height(12.dp))
          DpRules(
            title = strings.howToDepositTitle,
            steps = listOf(
              strings.digits("1") to strings.stepSelectMethod.removePrefix("1. ").removePrefix("১. "),
              strings.digits("2") to strings.sendMoneyInstructions,
              strings.digits("3") to strings.trxIdLabel,
              strings.digits("4") to strings.paymentScreenshotLabel,
              strings.digits("5") to strings.submitDepositButton,
            ),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 5. DEPOSIT DETAILS FORM
      WlFrame(gold = true, pad = 14.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp)) {
          DpStepHead(number = strings.digits("2"), title = strings.stepDepositDetails)

          val displayError = clientError ?: when {
            uiState.errorMessage?.contains("same sender number", ignoreCase = true) == true ||
              uiState.errorMessage?.contains("একই প্রেরক", ignoreCase = true) == true -> strings.depositDuplicateError
            uiState.errorMessage?.contains("20 minutes", ignoreCase = true) == true ||
              uiState.errorMessage?.contains("২০ মিনিট", ignoreCase = true) == true -> strings.depositLimitExceededError
            else -> uiState.errorMessage
          }
          if (displayError != null) {
            DpNotice(text = displayError, icon = Icons.Default.Warning, error = true, modifier = Modifier.padding(bottom = 12.dp))
          }

          // Amount box
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .background(Color(0x8C0A0E3C))
              .border(1.dp, Color(0x8CE8B84A), RoundedCornerShape(18.dp))
              .padding(12.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
              Image(painter = painterResource(id = R.drawable.mg_coin), contentDescription = null, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(6.dp))
              WlText3D(strings.depositAmount.uppercase(), 14.sp, WlTone.GOLD, modifier = Modifier.weight(1f))
              Text(
                text = strings.depositRange, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFF3C4), maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x26FFD45A)).border(1.dp, Color(0x99FFD45A), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            DpField(
              label = strings.enterAmountLabel,
              value = amountInput,
              onValueChange = {
                amountInput = it.filter { char -> char.isDigit() }
                clientError = null
              },
              placeholder = strings.depositAmountPlaceholder,
              icon = Icons.Default.CurrencyExchange,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              tag = "deposit_amount_input",
              big = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf(50, 100, 200, 500, 1000).forEach { chipAmount ->
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

          Spacer(modifier = Modifier.height(14.dp))

          // Sender Mobile Number
          DpField(
            label = strings.senderMobileLabel,
            value = senderNumber,
            onValueChange = {
              if (it.length <= 11) {
                senderNumber = it.filter { char -> char.isDigit() }
                clientError = null
              }
            },
            placeholder = "01XXXXXXXXX",
            icon = Icons.Default.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            tag = "deposit_sender_input",
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Transaction ID (TrxID)
          DpField(
            label = strings.trxIdLabel,
            value = trxId,
            onValueChange = {
              trxId = it.uppercase()
              clientError = null
            },
            placeholder = strings.depositTrxIdPlaceholder,
            icon = Icons.Default.Receipt,
            keyboardOptions = KeyboardOptions.Default,
            tag = "deposit_trx_input",
          )

          Spacer(modifier = Modifier.height(14.dp))

          // 6. PAYMENT SCREENSHOT UPLOAD
          Text(strings.paymentScreenshotLabel, fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFE7A0), modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
          if (imageBitmap != null) {
            DpScreenshotPreview(
              bitmap = imageBitmap!!,
              previewText = strings.depositScreenshotPreview,
              attachedText = strings.screenshotAttached,
              replaceText = strings.replace,
              removeText = strings.remove,
              onReplace = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              onRemove = {
                selectedImageUri = null
                imageBitmap = null
              },
            )
          } else {
            DpUploadBox(
              title = strings.uploadScreenshotTitle,
              subtitle = strings.uploadScreenshotSub,
              icon = Icons.Default.CloudUpload,
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // 7. SECURITY NOTICE
          DpNotice(text = strings.depositSecurityNotice, icon = Icons.Default.Security, error = false)

          Spacer(modifier = Modifier.height(16.dp))

          // 8. SUBMIT BUTTON
          DpBigButton(
            text = strings.submitDepositButton,
            onClick = {
              val minAmount = (appSettings?.minDepositTaka ?: 50.0).toLong()
              val amountNum = amountInput.toLongOrNull() ?: 0L
              if (amountNum < minAmount) {
                clientError = strings.depositMinAmountError(strings.takaWhole(minAmount.toDouble()))
                return@DpBigButton
              }
              if (amountNum > 25000L) {
                clientError = strings.depositMaxError
                return@DpBigButton
              }
              if (senderNumber.length < 11) {
                clientError = strings.invalidMobileError
                return@DpBigButton
              }
              if (trxId.length < 6) {
                clientError = strings.invalidTrxIdError
                return@DpBigButton
              }
              viewModel.submitDeposit(
                amountMinorUnits = amountNum * 100L,
                method = selectedMethod,
                senderNumber = senderNumber,
                trxId = trxId,
              )
            },
            isLoading = uiState.isSubmitting,
            tag = "submit_deposit_button",
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

