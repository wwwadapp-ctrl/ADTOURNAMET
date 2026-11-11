package com.example.ui.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

private val Slate900 = Color(0xFF0F172A)
private val Slate850 = Color(0xFF131E35)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate400 = Color(0xFF94A3B8)
private val Slate300 = Color(0xFFCBD5E1)
private val CardBorder = Color(0x33F59E0B)
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
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      if (uiState.withdrawalSubmitted) {
        TournamentCard(
          modifier = Modifier.fillMaxWidth(),
          backgroundColor = Slate900,
          borderColor = Emerald500,
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Emerald400,
            modifier = Modifier.size(48.dp),
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = strings.withdrawSuccessTitle,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
          )
          Text(
            text = uiState.successMessage
              ?: strings.withdrawSuccessDesc(strings.taka(amountNum.toDouble()), methodLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = Slate300,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
          )

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Slate850,
            border = BorderStroke(1.dp, Slate700),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp),
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(text = strings.withdrawStatusLabel, style = MaterialTheme.typography.bodySmall, color = Slate400)
                Text(
                  text = strings.withdrawStatusPending,
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                  color = Gold400,
                )
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(text = strings.withdrawPayoutMethodLabel, style = MaterialTheme.typography.bodySmall, color = Slate400)
                Text(text = methodLabel, style = MaterialTheme.typography.bodySmall, color = Color.White)
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(text = strings.withdrawRecipientAccountLabel, style = MaterialTheme.typography.bodySmall, color = Slate400)
                Text(text = recipientNumber, style = MaterialTheme.typography.bodySmall, color = Color.White)
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(text = strings.withdrawRequestedAmountLabel, style = MaterialTheme.typography.bodySmall, color = Slate400)
                Text(
                  text = strings.taka(amountNum.toDouble()),
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                  color = Emerald400,
                )
              }
            }
          }

          TournamentButton(
            text = strings.withdrawBackToWallet,
            onClick = {
              viewModel.clearMessages()
              onNavigateBack()
            },
            modifier = Modifier.fillMaxWidth(),
          )
        }
        return@Column
      }

      // 2. PAGE IDENTITY
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 2.dp, bottom = 12.dp),
      ) {
        Text(
          text = strings.withdrawHeaderTitle,
          style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            fontSize = 20.sp
          ),
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = strings.withdrawHeaderSubtitle,
          style = MaterialTheme.typography.bodySmall.copy(
            color = Slate400,
            fontSize = 13.sp,
          ),
        )
      }

      // 3. AVAILABLE BALANCE CARD
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
          Column(modifier = Modifier.weight(1f)) {
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
                text = strings.withdrawAvailableBalanceLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp,
                  fontSize = 12.sp
                ),
                color = Gold400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = strings.taka(uiState.wallet?.availableAmount ?: 0.0),
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
              ),
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
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
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = NavyCard,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = strings.withdrawSelectMethodLabel,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              fontSize = 15.sp,
            ),
            color = Gold400,
          )
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            val isBkash = selectedMethod == "BKASH"
            val isNagad = selectedMethod == "NAGAD"

            // bKash Option
            Surface(
              onClick = { onSelectMethod("BKASH") },
              color = if (isBkash) BkashPink.copy(alpha = 0.12f) else Slate900,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(
                if (isBkash) 1.5.dp else 1.dp,
                if (isBkash) BkashPink else Slate700,
              ),
              modifier = Modifier.weight(1f),
            ) {
              Row(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
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
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = "bKash",
                    style = MaterialTheme.typography.titleSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = if (isBkash) strings.withdrawMethodSelected else strings.withdrawMethodPersonal,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Medium,
                    ),
                    color = if (isBkash) BkashPink else Slate400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }

            // Nagad Option
            Surface(
              onClick = { onSelectMethod("NAGAD") },
              color = if (isNagad) NagadOrange.copy(alpha = 0.12f) else Slate900,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(
                if (isNagad) 1.5.dp else 1.dp,
                if (isNagad) NagadOrange else Slate700,
              ),
              modifier = Modifier.weight(1f),
            ) {
              Row(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
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
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = "Nagad",
                    style = MaterialTheme.typography.titleSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = if (isNagad) strings.withdrawMethodSelected else strings.withdrawMethodPersonal,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Medium,
                    ),
                    color = if (isNagad) NagadOrange else Slate400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 5. WITHDRAW AMOUNT CARD / BANNER
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
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.Paid,
                contentDescription = null,
                tint = Gold400,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = strings.withdrawAmountLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp,
                  fontSize = 15.sp,
                ),
                color = Gold400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            val minAmount = (appSettings?.minWithdrawalTaka ?: 200.0).toLong()
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Gold500.copy(alpha = 0.12f),
              border = BorderStroke(0.5.dp, Gold400.copy(alpha = 0.4f)),
            ) {
              Text(
                text = strings.withdrawLimitNotice(strings.number(minAmount)),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                ),
                color = Slate300,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
            label = strings.withdrawEnterAmountHint,
            placeholder = strings.withdrawEnterAmountPlaceholder,
            helperText = null,
            leadingIcon = Icons.Default.CurrencyExchange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            testTag = "withdraw_amount_input",
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Quick-Select Amount Pills / Chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            listOf(200, 500, 1000, 2000, 5000).forEach { chipAmount ->
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
                      fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                      fontSize = 12.sp,
                    ),
                    color = if (isSelected) Gold400 else Slate300,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 6. SAVED / OWN NUMBER INPUT CARD
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
            Text(
              text = strings.withdrawRecipientLabel(methodLabel.uppercase()),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontSize = 15.sp,
              ),
              color = Gold400,
              modifier = Modifier.weight(1f, fill = false),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            if (savedNumber.isNotBlank() && !isUsingSavedNumber) {
              TextButton(
                onClick = {
                  recipientNumber = savedNumber
                  clientError = null
                },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                modifier = Modifier.height(24.dp),
              ) {
                Text(
                  text = strings.withdrawUseSavedNumber,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                  ),
                  color = Cyan400,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          TournamentTextField(
            value = recipientNumber,
            onValueChange = {
              if (it.length <= 11) {
                recipientNumber = it.filter { char -> char.isDigit() }
                clientError = null
              }
            },
            label = strings.withdrawRecipientHint(methodLabel),
            placeholder = strings.withdrawRecipientPlaceholder,
            leadingIcon = Icons.Default.Phone,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            testTag = "withdraw_recipient_input",
          )

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            if (isUsingSavedNumber) {
              Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = Emerald400,
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = strings.withdrawVerifiedBadge(methodLabel),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = Emerald400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            } else {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(13.dp),
              )
              Text(
                text = strings.withdrawSecurityHint(methodLabel),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = Slate400,
                lineHeight = 16.sp
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 7. WITHDRAW SUMMARY CARD
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Slate900,
        border = BorderStroke(1.dp, Slate700),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = strings.withdrawSummaryTitle,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              fontSize = 15.sp,
            ),
            color = Slate300,
          )

          HorizontalDivider(color = Slate800, thickness = 1.dp)

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(text = strings.withdrawSummaryAmount, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = Slate400)
            Text(
              text = strings.taka(amountNum.toDouble()),
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
              color = Color.White,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(text = strings.withdrawSummaryMethod, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = Slate400)
            Text(
              text = methodLabel,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
              color = if (selectedMethod == "BKASH") BkashPink else NagadOrange,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(text = strings.withdrawSummaryNumber, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = Slate400)
            Text(
              text = recipientNumber.ifBlank { strings.optional },
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          HorizontalDivider(color = Slate800, thickness = 1.dp)

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = strings.withdrawSummaryTotal,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
              color = Gold400,
            )
            Text(
              text = strings.taka(amountNum.toDouble()),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 18.sp),
              color = Emerald400,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 8. SECURITY / GUIDELINE CARD
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Slate900.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, Slate800),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              tint = Cyan400,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = strings.withdrawSecurityTitle,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                fontSize = 15.sp,
              ),
              color = Cyan400,
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          val minWithdrawal = (appSettings?.minWithdrawalTaka ?: 200.0).toLong()
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = strings.withdrawGuideline1, style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 14.sp, lineHeight = 18.sp))
            Text(text = strings.withdrawGuideline2(strings.number(minWithdrawal)), style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 14.sp, lineHeight = 18.sp))
            Text(text = strings.withdrawGuideline3, style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 14.sp, lineHeight = 18.sp))
            Text(text = strings.withdrawGuideline4, style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 14.sp, lineHeight = 18.sp))
            Text(text = strings.withdrawGuideline5, style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 14.sp, lineHeight = 18.sp))
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 10. ERROR / VALIDATION MESSAGE
      val displayError = clientError ?: uiState.errorMessage
      if (displayError != null) {
        Surface(
          color = Rose900.copy(alpha = 0.45f),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, Rose600.copy(alpha = 0.5f)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = Rose400,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = displayError,
              color = Color.White,
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
              ),
            )
          }
        }
      }

      // 9. MAIN CTA: "WITHDRAW NOW"
      TournamentButton(
        text = strings.withdrawNowButton,
        onClick = {
          val minAmount = (appSettings?.minWithdrawalTaka ?: 200.0).toLong()
          if (amountNum < minAmount) {
            clientError = strings.withdrawMinError(strings.number(minAmount))
            return@TournamentButton
          }
          if (amountNum > 10000L) {
            clientError = strings.withdrawMaxError
            return@TournamentButton
          }
          if (amountMinor > (uiState.wallet?.availableBalance ?: 0L)) {
            clientError = strings.withdrawInsufficientError(strings.taka(uiState.wallet?.availableAmount ?: 0.0))
            return@TournamentButton
          }
          if (recipientNumber.length < 11) {
            clientError = strings.withdrawInvalidMobileError
            return@TournamentButton
          }
          viewModel.submitWithdrawal(
            amountMinorUnits = amountMinor,
            method = selectedMethod,
            recipientNumber = recipientNumber,
          )
        },
        isLoading = uiState.isSubmitting,
        enabled = !uiState.isSubmitting && amountInput.isNotBlank() && recipientNumber.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
        testTag = "submit_withdrawal_button",
      )

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
