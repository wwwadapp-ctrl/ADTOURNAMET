package com.example.ui.wallet

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      if (uiState.depositSubmitted) {
        AlertDialog(
          onDismissRequest = {
            viewModel.clearMessages()
            onNavigateBack()
          },
          shape = RoundedCornerShape(16.dp),
          containerColor = Slate900,
          icon = {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = Emerald400,
              modifier = Modifier.size(52.dp),
            )
          },
          title = {
            Text(
              text = strings.depositSubmittedTitle,
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
              color = Color.White,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
          },
          text = {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = strings.depositSubmittedDesc,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp),
                color = Slate200,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              )
              Spacer(modifier = Modifier.height(14.dp))
              Surface(
                color = Slate850,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Slate700),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(12.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(strings.depositMethodLabel, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = Slate400)
                    Text(
                      text = selectedMethod,
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                      color = if (selectedMethod == "BKASH") BkashPink else NagadOrange,
                    )
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(strings.depositAmountSummaryLabel, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = Slate400)
                    Text(
                      text = strings.taka(amountInput.toDoubleOrNull() ?: 0.0),
                      style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isBengali) FontWeight.Bold else FontWeight.ExtraBold,
                        fontSize = 13.sp,
                      ),
                      color = Gold400,
                      modifier = Modifier.padding(end = 2.dp),
                    )
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(strings.senderMobileLabel, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = Slate400)
                    Text(
                      text = senderNumber,
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                      color = Color.White,
                    )
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(strings.trxIdLabel, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = Slate400)
                    Text(
                      text = trxId,
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                      color = Color.White,
                    )
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(strings.depositStatusLabel, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = Slate400)
                    Surface(
                      color = Amber500.copy(alpha = 0.2f),
                      shape = RoundedCornerShape(4.dp),
                    ) {
                      Text(
                        text = strings.depositStatusPending,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = Amber400,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      )
                    }
                  }
                }
              }
            }
          },
          confirmButton = {
            TournamentButton(
              text = strings.backToWallet,
              onClick = {
                viewModel.clearMessages()
                onNavigateBack()
              },
              modifier = Modifier.fillMaxWidth(),
              testTag = "deposit_success_confirm_btn",
            )
          },
          modifier = Modifier.testTag("deposit_success_dialog"),
        )
      }

      // 2. PAGE IDENTITY
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 2.dp, bottom = 12.dp),
      ) {
        Text(
          text = strings.addMoneyTitle,
          style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = if (isBengali) 0.sp else 0.5.sp,
            fontSize = 20.sp,
          ),
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = strings.depositSubtitle,
          style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
          ),
          color = Gold400,
        )
      }

      // 3. CURRENT BALANCE CARD
      val currentBalanceMinor = uiState.wallet?.availableBalance ?: 0L
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = NavyCard,
        border = BorderStroke(
          1.dp,
          Brush.horizontalGradient(
            listOf(
              Gold500.copy(alpha = 0.5f),
              Cyan400.copy(alpha = 0.4f),
            )
          ),
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(Emerald400),
              )
              Text(
                text = strings.balanceCurrent.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp,
                  fontSize = 11.sp,
                ),
                color = Gold400,
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = strings.takaFromMinor(currentBalanceMinor),
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = if (isBengali) FontWeight.Bold else FontWeight.ExtraBold,
                fontSize = 22.sp,
              ),
              color = Color.White,
              modifier = Modifier.padding(end = 2.dp),
            )
          }

          Surface(
            shape = CircleShape,
            color = Slate900,
            border = BorderStroke(1.dp, Gold500.copy(alpha = 0.3f)),
            modifier = Modifier.size(42.dp),
          ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = Gold400,
                modifier = Modifier.size(22.dp),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 4. PAYMENT METHODS (bKash & Nagad ONLY)
      TournamentCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NavyCard,
        borderColor = NavyCardBorder,
      ) {
        Text(
          text = strings.stepSelectMethod,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
          ),
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          // bKash
          val isBkash = selectedMethod == "BKASH"
          Surface(
            onClick = {
              selectedMethod = "BKASH"
              copiedFeedback = false
            },
            color = if (isBkash) BkashPink.copy(alpha = 0.12f) else Slate900,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
              if (isBkash) 1.5.dp else 1.dp,
              if (isBkash) BkashPink else Slate700,
            ),
            modifier = Modifier.weight(1f),
          ) {
            Row(
              modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Start,
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isBkash) BkashPink.copy(alpha = 0.2f) else Slate800,
                border = BorderStroke(
                  1.dp,
                  if (isBkash) BkashPink.copy(alpha = 0.5f) else Color.Transparent,
                ),
                modifier = Modifier.size(36.dp),
              ) {
                Box(
                  modifier = Modifier.fillMaxSize(),
                  contentAlignment = Alignment.Center,
                ) {
                  Image(
                    painter = painterResource(id = R.drawable.ic_bkash),
                    contentDescription = "bKash Logo",
                    modifier = Modifier.size(24.dp),
                  )
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "bKash",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                  ),
                  color = Color.White,
                )
                Text(
                  text = if (isBkash) strings.selectedBadge else strings.personalBadge,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                  ),
                  color = if (isBkash) BkashPink else Slate400,
                )
              }
            }
          }

          // Nagad
          val isNagad = selectedMethod == "NAGAD"
          Surface(
            onClick = {
              selectedMethod = "NAGAD"
              copiedFeedback = false
            },
            color = if (isNagad) NagadOrange.copy(alpha = 0.12f) else Slate900,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
              if (isNagad) 1.5.dp else 1.dp,
              if (isNagad) NagadOrange else Slate700,
            ),
            modifier = Modifier.weight(1f),
          ) {
            Row(
              modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Start,
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isNagad) NagadOrange.copy(alpha = 0.2f) else Slate800,
                border = BorderStroke(
                  1.dp,
                  if (isNagad) NagadOrange.copy(alpha = 0.5f) else Color.Transparent,
                ),
                modifier = Modifier.size(36.dp),
              ) {
                Box(
                  modifier = Modifier.fillMaxSize(),
                  contentAlignment = Alignment.Center,
                ) {
                  Image(
                    painter = painterResource(id = R.drawable.ic_nagad),
                    contentDescription = "Nagad Logo",
                    modifier = Modifier.size(24.dp),
                  )
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Nagad",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                  ),
                  color = Color.White,
                )
                Text(
                  text = if (isNagad) strings.selectedBadge else strings.personalBadge,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                  ),
                  color = if (isNagad) NagadOrange else Slate400,
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Configured Payment Number Display & Copy
        Surface(
          color = Slate850,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, if (selectedMethod == "BKASH") BkashPink.copy(alpha = 0.5f) else NagadOrange.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Verified,
                  contentDescription = null,
                  tint = if (selectedMethod == "BKASH") BkashPink else NagadOrange,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = strings.sendMoneyLabel(if (selectedMethod == "BKASH") "bKash" else "Nagad"),
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                  color = Slate300,
                )
              }
              Surface(
                color = Emerald500.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp),
              ) {
                Text(
                  text = strings.depositOfficialBadge,
                  color = Emerald400,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = configuredPaymentNumber,
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp,
                  fontSize = 20.sp,
                ),
                color = Gold400,
              )
              Button(
                onClick = {
                  clipboardManager.setText(AnnotatedString(rawPaymentNumber))
                  copiedFeedback = true
                  android.widget.Toast.makeText(
                    context,
                    strings.depositNumberCopiedToast(selectedMethod, rawPaymentNumber),
                    android.widget.Toast.LENGTH_SHORT
                  ).show()
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (copiedFeedback) Emerald600 else if (selectedMethod == "BKASH") BkashPink else NagadOrange,
                  contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("deposit_copy_number_btn"),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = if (copiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = if (copiedFeedback) strings.copied.uppercase() else strings.copy.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Compact Payment Steps
        Surface(
          color = Slate900.copy(alpha = 0.6f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = strings.howToDepositTitle,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Slate300,
              ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            val steps = listOf(
              "${strings.digits("1")}. ${strings.stepSelectMethod.removePrefix("1. ").removePrefix("১. ")}",
              "${strings.digits("2")}. ${strings.sendMoneyInstructions}",
              "${strings.digits("3")}. ${strings.trxIdLabel}",
              "${strings.digits("4")}. ${strings.paymentScreenshotLabel}",
              "${strings.digits("5")}. ${strings.submitDepositButton}",
            )
            steps.forEach { step ->
              Text(
                text = step,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Slate400,
                lineHeight = 16.sp,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 5. DEPOSIT DETAILS FORM
      TournamentCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NavyCard,
        borderColor = NavyCardBorder,
      ) {
        Text(
          text = strings.stepDepositDetails,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
          ),
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(12.dp))

        val displayError = clientError ?: when {
          uiState.errorMessage?.contains("same sender number", ignoreCase = true) == true ||
            uiState.errorMessage?.contains("একই প্রেরক", ignoreCase = true) == true -> strings.depositDuplicateError
          uiState.errorMessage?.contains("20 minutes", ignoreCase = true) == true ||
            uiState.errorMessage?.contains("২০ মিনিট", ignoreCase = true) == true -> strings.depositLimitExceededError
          else -> uiState.errorMessage
        }
        if (displayError != null) {
          Surface(
            color = Rose900.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Rose600.copy(alpha = 0.5f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp),
          ) {
            Text(
              text = displayError,
              color = Rose400,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
              modifier = Modifier.padding(10.dp),
            )
          }
        }

        // Premium Deposit Amount Card / Banner
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Slate850,
          border = BorderStroke(1.dp, CardBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Paid,
                  contentDescription = null,
                  tint = Gold400,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = strings.depositAmount.uppercase(),
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    fontSize = 12.sp,
                  ),
                  color = Gold400,
                )
              }
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Gold500.copy(alpha = 0.12f),
                border = BorderStroke(0.5.dp, Gold400.copy(alpha = 0.4f)),
              ) {
                Text(
                  text = strings.depositRange,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                  ),
                  color = Slate300,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prominent Amount Input Field
            TournamentTextField(
              value = amountInput,
              onValueChange = {
                amountInput = it.filter { char -> char.isDigit() }
                clientError = null
              },
              label = strings.enterAmountLabel,
              placeholder = strings.depositAmountPlaceholder,
              helperText = null,
              leadingIcon = Icons.Default.CurrencyExchange,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              testTag = "deposit_amount_input",
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick-Select Amount Pills / Chips
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              listOf(50, 100, 200, 500, 1000).forEach { chipAmount ->
                val isSelected = amountInput == chipAmount.toString()
                Surface(
                  onClick = {
                    amountInput = chipAmount.toString()
                    clientError = null
                  },
                  shape = RoundedCornerShape(20.dp),
                  color = if (isSelected) Gold500.copy(alpha = 0.22f) else Slate900,
                  border = BorderStroke(
                    1.dp,
                    if (isSelected) Gold400 else Slate700,
                  ),
                  modifier = Modifier.weight(1f),
                ) {
                  Box(
                    modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = strings.takaWhole(chipAmount.toDouble()),
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) (if (isBengali) FontWeight.Bold else FontWeight.ExtraBold) else FontWeight.Bold,
                        fontSize = 11.5.sp,
                      ),
                      color = if (isSelected) Gold400 else Slate300,
                      maxLines = 1,
                      softWrap = false,
                      modifier = Modifier.padding(end = 2.dp),
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sender Mobile Number
        TournamentTextField(
          value = senderNumber,
          onValueChange = {
            if (it.length <= 11) {
              senderNumber = it.filter { char -> char.isDigit() }
              clientError = null
            }
          },
          label = strings.senderMobileLabel,
          placeholder = "01XXXXXXXXX",
          leadingIcon = Icons.Default.Phone,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          testTag = "deposit_sender_input",
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Transaction ID (TrxID)
        TournamentTextField(
          value = trxId,
          onValueChange = {
            trxId = it.uppercase()
            clientError = null
          },
          label = strings.trxIdLabel,
          placeholder = strings.depositTrxIdPlaceholder,
          leadingIcon = Icons.Default.Receipt,
          testTag = "deposit_trx_input",
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 6. PAYMENT SCREENSHOT UPLOAD
        Text(
          text = strings.paymentScreenshotLabel,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
          ),
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (imageBitmap != null) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Slate900,
            border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Image(
                bitmap = imageBitmap!!,
                contentDescription = strings.depositScreenshotPreview,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(160.dp)
                  .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Emerald400,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = strings.screenshotAttached,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Emerald400,
                  )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  TextButton(
                    onClick = {
                      photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                      )
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  ) {
                    Text(
                      text = strings.replace,
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                      color = Cyan400,
                    )
                  }
                  TextButton(
                    onClick = {
                      selectedImageUri = null
                      imageBitmap = null
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                  ) {
                    Text(
                      text = strings.remove,
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                      color = Rose400,
                    )
                  }
                }
              }
            }
          }
        } else {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Slate900,
            border = BorderStroke(1.dp, Slate700),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .clickable {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Surface(
                shape = CircleShape,
                color = Slate800,
                modifier = Modifier.size(40.dp),
              ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = Cyan400,
                    modifier = Modifier.size(22.dp),
                  )
                }
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = strings.uploadScreenshotTitle,
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp,
                  fontSize = 13.sp,
                ),
                color = Color.White,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = strings.uploadScreenshotSub,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Slate400,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 7. SECURITY NOTICE
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Slate900.copy(alpha = 0.8f),
          border = BorderStroke(0.5.dp, Cyan400.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              tint = Cyan400,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = strings.depositSecurityNotice,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
              color = Slate300,
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 8. SUBMIT BUTTON
        TournamentButton(
          text = strings.submitDepositButton,
          onClick = {
            val minAmount = (appSettings?.minDepositTaka ?: 50.0).toLong()
            val amountNum = amountInput.toLongOrNull() ?: 0L
            if (amountNum < minAmount) {
              clientError = strings.depositMinAmountError(strings.takaWhole(minAmount.toDouble()))
              return@TournamentButton
            }
            if (amountNum > 25000L) {
              clientError = strings.depositMaxError
              return@TournamentButton
            }
            if (senderNumber.length < 11) {
              clientError = strings.invalidMobileError
              return@TournamentButton
            }
            if (trxId.length < 6) {
              clientError = strings.invalidTrxIdError
              return@TournamentButton
            }
            viewModel.submitDeposit(
              amountMinorUnits = amountNum * 100L,
              method = selectedMethod,
              senderNumber = senderNumber,
              trxId = trxId,
            )
          },
          isLoading = uiState.isSubmitting,
          modifier = Modifier.fillMaxWidth(),
          testTag = "submit_deposit_button",
        )
      }
    }
  }
}

