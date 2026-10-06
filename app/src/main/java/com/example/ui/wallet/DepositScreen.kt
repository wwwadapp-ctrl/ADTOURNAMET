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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.ui.components.PremiumNotificationBellButton
import com.example.ui.components.TournamentButton
import com.example.ui.components.TournamentButtonVariant
import com.example.ui.components.TournamentCard
import com.example.ui.components.TournamentTextField
import com.example.core.i18n.LocalAppStrings
import com.example.ui.theme.*

private val BkashPink = Color(0xFFE2136E)
private val NagadOrange = Color(0xFFF7941D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositScreen(
  viewModel: WalletViewModel,
  onNavigateBack: () -> Unit,
  onNotificationClick: () -> Unit = {},
  onProfileClick: () -> Unit = {},
  unreadNotificationsCount: Int = 0,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
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
      TopAppBar(
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Color.White,
            )
          }
        },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Slate900,
              border = BorderStroke(1.dp, Gold400.copy(alpha = 0.7f)),
              modifier = Modifier.size(36.dp),
            ) {
              Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
              ) {
                Image(
                  painter = painterResource(id = R.drawable.ic_launcher_foreground),
                  contentDescription = "AD TOURNAMENT",
                  modifier = Modifier.size(32.dp),
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "AD TOURNAMENT",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = 1.sp,
                  fontSize = 14.sp,
                ),
                color = Color.White,
              )
              Text(
                text = "1v1 ESPORTS BATTLES",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp,
                  fontSize = 10.sp,
                ),
                color = Gold400,
              )
            }
          }
        },
        actions = {
          // Premium Unified Notification Bell Button
          PremiumNotificationBellButton(
            unreadCount = unreadNotificationsCount,
            onClick = onNotificationClick,
            testTag = "deposit_notification_button",
            contentDescription = "Notifications",
          )

          Spacer(modifier = Modifier.width(4.dp))

          IconButton(
            onClick = onProfileClick,
            modifier = Modifier.testTag("deposit_profile_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = "Profile",
              tint = Gold400,
              modifier = Modifier.size(26.dp),
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = NavySurface),
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
              text = "Deposit Request Submitted",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
                text = "Deposit request submitted! Please wait for Admin approval.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
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
                    Text("Method", style = MaterialTheme.typography.bodySmall, color = Slate400)
                    Text(
                      text = selectedMethod,
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                      color = if (selectedMethod == "BKASH") BkashPink else NagadOrange,
                    )
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text("Amount", style = MaterialTheme.typography.bodySmall, color = Slate400)
                    Text(
                      text = "৳ ${"%.2f".format(amountInput.toDoubleOrNull() ?: 0.0)}",
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                      color = Gold400,
                    )
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text("Sender Number", style = MaterialTheme.typography.bodySmall, color = Slate400)
                    Text(
                      text = senderNumber,
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                      color = Color.White,
                    )
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text("TrxID", style = MaterialTheme.typography.bodySmall, color = Slate400)
                    Text(
                      text = trxId,
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                      color = Color.White,
                    )
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text("Status", style = MaterialTheme.typography.bodySmall, color = Slate400)
                    Surface(
                      color = Amber500.copy(alpha = 0.2f),
                      shape = RoundedCornerShape(4.dp),
                    ) {
                      Text(
                        text = "PENDING",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
          ),
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = strings.depositSubtitle,
          style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Medium,
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
                ),
                color = Gold400,
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "৳ ${"%.2f".format(currentBalanceMinor / 100.0)}",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
              ),
              color = Color.White,
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
            fontSize = 14.sp,
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
                    fontSize = 10.sp,
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
                    fontSize = 10.sp,
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
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                  color = Slate300,
                )
              }
              Surface(
                color = Emerald500.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp),
              ) {
                Text(
                  text = "OFFICIAL",
                  color = Emerald400,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
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
                  fontWeight = FontWeight.Black,
                  letterSpacing = 1.sp,
                ),
                color = Gold400,
              )
              Button(
                onClick = {
                  clipboardManager.setText(AnnotatedString(rawPaymentNumber))
                  copiedFeedback = true
                  android.widget.Toast.makeText(
                    context,
                    "$selectedMethod number copied: $rawPaymentNumber",
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
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
                color = Slate300,
              ),
            )
            Spacer(modifier = Modifier.height(4.dp))
            val steps = listOf(
              "1. ${strings.stepSelectMethod.removePrefix("1. ").removePrefix("১. ")}",
              "2. ${strings.sendMoneyInstructions}",
              "3. ${strings.trxIdLabel}",
              "4. ${strings.paymentScreenshotLabel}",
              "5. ${strings.submitDepositButton}",
            )
            steps.forEach { step ->
              Text(
                text = step,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Slate400,
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
            fontSize = 14.sp,
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
              style = MaterialTheme.typography.bodySmall,
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
                    fontSize = 11.sp,
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
                    fontSize = 9.sp,
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
              placeholder = "e.g. 100",
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
                    modifier = Modifier.padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    Text(
                      text = "৳$chipAmount",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        fontSize = 11.sp,
                      ),
                      color = if (isSelected) Gold400 else Slate300,
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
          placeholder = "e.g. 9J8K7L6M",
          leadingIcon = Icons.Default.Receipt,
          testTag = "deposit_trx_input",
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 6. PAYMENT SCREENSHOT UPLOAD
        Text(
          text = strings.paymentScreenshotLabel,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
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
                contentDescription = "Payment Screenshot Preview",
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
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
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
                      style = MaterialTheme.typography.labelSmall,
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
                      style = MaterialTheme.typography.labelSmall,
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
                ),
                color = Color.White,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = strings.uploadScreenshotSub,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
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
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
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
              clientError = "Minimum deposit amount is ৳ $minAmount"
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

