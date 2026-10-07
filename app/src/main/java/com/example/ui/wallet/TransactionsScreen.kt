package com.example.ui.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.TransactionEntity
import com.example.domain.model.TransactionStatus
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import com.example.ui.components.UnifiedEsportsTopBar
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Slate900 = Color(0xFF0F172A)
private val Slate850 = Color(0xFF131E35)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate400 = Color(0xFF94A3B8)
private val Slate300 = Color(0xFFCBD5E1)

enum class TransactionFilter(val displayName: String, val testTag: String) {
  ALL("ALL", "transactions_filter_all"),
  DEPOSIT("DEPOSIT", "transactions_filter_deposit"),
  MATCH("MATCH", "transactions_filter_match"),
  WINNING("WINNING", "transactions_filter_winning"),
  WITHDRAW("WITHDRAW", "transactions_filter_withdraw"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
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
  val uiState by viewModel.uiState.collectAsState()
  val wallet = uiState.wallet
  val allTransactions = uiState.transactions
  val validTransactions = remember(allTransactions) {
    allTransactions.filter { it.amount != 0L }
  }
  var selectedFilter by remember { mutableStateOf(TransactionFilter.ALL) }

  val filteredTransactions = remember(validTransactions, selectedFilter) {
    validTransactions.filter { txn ->
      val type = txn.type.uppercase()
      when (selectedFilter) {
        TransactionFilter.ALL -> true
        TransactionFilter.DEPOSIT -> type == "DEPOSIT"
        TransactionFilter.MATCH -> type in listOf("MATCH_ENTRY", "MATCH_ENTRY_FEE", "MATCH_REFUND", "REFUND")
        TransactionFilter.WINNING -> type in listOf("MATCH_PRIZE", "MATCH_PRIZE_CREDIT", "PRIZE_WIN", "WIN", "WINNING")
        TransactionFilter.WITHDRAW -> type in listOf("WITHDRAW", "WITHDRAWAL", "WITHDRAW_HOLD", "WITHDRAW_RELEASE")
      }
    }
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
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .testTag("transactions_list"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // 2. PAGE IDENTITY
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp),
        ) {
          Text(
            text = "TRANSACTION HISTORY",
            style = MaterialTheme.typography.headlineSmall.copy(
              fontWeight = FontWeight.Black,
              letterSpacing = 1.sp,
            ),
            color = Color.White,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Track your deposits, match entries, winnings and withdrawals.",
            style = MaterialTheme.typography.bodySmall.copy(
              color = Slate400,
              fontSize = 12.sp,
            ),
          )
        }
      }

      // 3. BALANCE SUMMARY CARD
      item {
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
          Column(modifier = Modifier.padding(14.dp)) {
            // Main row: Available Balance
            Row(
              modifier = Modifier.fillMaxWidth(),
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
                    text = "AVAILABLE BALANCE",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 0.5.sp,
                      fontSize = 11.sp,
                    ),
                    color = Gold400,
                  )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = "৳ ${"%.2f".format(wallet?.availableAmount ?: 0.0)}",
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

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Slate800, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Stats: Deposited, Winnings, Withdrawn
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Deposited",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                  ),
                  color = Slate400,
                )
                Text(
                  text = "৳ ${"%,.2f".format(wallet?.totalDepositedAmount ?: 0.0)}",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = Cyan400,
                )
              }

              Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = "Winnings",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                  ),
                  color = Slate400,
                )
                Text(
                  text = "৳ ${"%,.2f".format(wallet?.winningsAmount ?: 0.0)}",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = Emerald400,
                )
              }

              Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
              ) {
                Text(
                  text = "Withdrawn",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                  ),
                  color = Slate400,
                )
                Text(
                  text = "৳ ${"%,.2f".format(wallet?.totalWithdrawnAmount ?: 0.0)}",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = Amber400,
                )
              }
            }
          }
        }
      }

      // 4. FILTER BAR
      item {
        LazyRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(TransactionFilter.values()) { filter ->
            val isSelected = selectedFilter == filter
            Surface(
              onClick = { selectedFilter = filter },
              shape = RoundedCornerShape(20.dp),
              color = if (isSelected) Gold500.copy(alpha = 0.22f) else Slate850,
              border = BorderStroke(
                1.dp,
                if (isSelected) Gold400 else Slate700,
              ),
              modifier = Modifier.testTag(filter.testTag),
            ) {
              Box(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = filter.displayName,
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

      // 9. ERROR / LOADING STATUS
      if (uiState.errorMessage != null) {
        item {
          Surface(
            color = Rose900.copy(alpha = 0.45f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Rose600.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
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
                text = uiState.errorMessage ?: "Failed to load transactions",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }
      }

      if (uiState.isLoading && allTransactions.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 36.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(
              color = Gold400,
              strokeWidth = 3.dp,
              modifier = Modifier.size(36.dp),
            )
          }
        }
      } else if (filteredTransactions.isEmpty()) {
        // 8. EMPTY STATE
        item {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Slate850,
            border = BorderStroke(1.dp, Slate700),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 12.dp),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp, horizontal = 20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
            ) {
              Surface(
                shape = CircleShape,
                color = Slate900,
                border = BorderStroke(1.dp, Slate700),
                modifier = Modifier.size(60.dp),
              ) {
                Box(
                  modifier = Modifier.fillMaxSize(),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(30.dp),
                  )
                }
              }
              Spacer(modifier = Modifier.height(14.dp))
              Text(
                text = if (selectedFilter == TransactionFilter.ALL) "No transactions yet" else "No transactions in this category",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = if (selectedFilter == TransactionFilter.ALL)
                  "Your completed deposits, match entries, winnings and withdrawals will be safely recorded here."
                else
                  "No transactions found for the selected filter.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Slate400,
                modifier = Modifier.padding(horizontal = 16.dp),
              )
            }
          }
        }
      } else {
        // 6. TRANSACTION CARDS
        items(filteredTransactions, key = { it.effectiveId }) { txn ->
          TransactionCard(txn = txn)
        }
      }
    }
  }
}

@Composable
private fun TransactionCard(txn: TransactionEntity) {
  val clipboardManager = LocalClipboardManager.current
  val typeUpper = txn.type.uppercase()

  val isWithdraw = typeUpper in listOf("WITHDRAW", "WITHDRAWAL", "WITHDRAW_HOLD", "WITHDRAW_RELEASE")
  val isDeposit = typeUpper == "DEPOSIT"
  val isCredit = !isWithdraw && (typeUpper in listOf("DEPOSIT", "MATCH_PRIZE", "MATCH_PRIZE_CREDIT", "PRIZE_WIN", "WIN", "WINNING", "MATCH_REFUND", "REFUND", "ADMIN_CREDIT"))

  // Visual Category Styling & Labels (Strictly approved user-facing labels)
  val (categoryIcon, iconTint, iconBg, titleText) = when {
    isWithdraw -> Quadruple(
      Icons.Default.ArrowUpward,
      Amber400,
      Amber400.copy(alpha = 0.15f),
      "উইথড্র",
    )
    isDeposit -> Quadruple(
      Icons.Default.AddCircle,
      Cyan400,
      Cyan400.copy(alpha = 0.15f),
      "ডিপোজিট",
    )
    typeUpper in listOf("MATCH_PRIZE", "MATCH_PRIZE_CREDIT", "PRIZE_WIN", "WIN", "WINNING") -> Quadruple(
      Icons.Default.EmojiEvents,
      Gold400,
      Gold500.copy(alpha = 0.15f),
      "Winning",
    )
    typeUpper in listOf("MATCH_REFUND", "REFUND") -> Quadruple(
      Icons.AutoMirrored.Filled.RotateLeft,
      Emerald400,
      Emerald400.copy(alpha = 0.15f),
      "Match Refund",
    )
    typeUpper in listOf("MATCH_ENTRY", "MATCH_ENTRY_FEE") -> Quadruple(
      Icons.Default.SportsEsports,
      Rose400,
      Rose400.copy(alpha = 0.15f),
      "Match Entry",
    )
    typeUpper == "ADMIN_CREDIT" -> Quadruple(
      Icons.Default.AddCircle,
      Emerald400,
      Emerald400.copy(alpha = 0.15f),
      "Admin Credit",
    )
    typeUpper == "ADMIN_DEBIT" -> Quadruple(
      Icons.Default.RemoveCircle,
      Rose400,
      Rose400.copy(alpha = 0.15f),
      "Admin Debit",
    )
    typeUpper == "ADMIN_ADJUSTMENT" -> Quadruple(
      Icons.Default.Tune,
      Slate400,
      Slate800,
      "Admin Adjustment",
    )
    typeUpper == "COMMISSION" -> Quadruple(
      Icons.Default.ReceiptLong,
      Slate400,
      Slate800,
      "Commission",
    )
    else -> Quadruple(
      Icons.Default.ReceiptLong,
      Slate400,
      Slate800,
      txn.title.ifBlank { txn.description.ifBlank { txn.type.replace('_', ' ') } },
    )
  }

  // Date Formatting with safe fallback
  val txTime = if (txn.timestamp > 0L) txn.timestamp else txn.createdAt
  val dateFormatted = remember(txTime) {
    if (txTime > 0L) {
      SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(txTime))
    } else {
      "Date unavailable"
    }
  }

  // Subtitle / Description / Source
  val displayDescription = remember(txn.description, txn.source) {
    if (txn.description.isNotBlank() && txn.description != titleText) {
      txn.description
    } else {
      txn.source.ifBlank { "APP" }
    }
  }

  // Status Badge using exact domain TransactionStatus (No invented statuses)
  val statusUpper = txn.status.uppercase()
  val (statusColor, statusBg, statusLabel) = when (statusUpper) {
    TransactionStatus.COMPLETED.name -> Triple(
      Emerald400,
      Emerald500.copy(alpha = 0.12f),
      TransactionStatus.COMPLETED.name,
    )
    TransactionStatus.PENDING.name -> Triple(
      Gold400,
      Gold500.copy(alpha = 0.12f),
      TransactionStatus.PENDING.name,
    )
    TransactionStatus.FAILED.name -> Triple(
      Rose400,
      Rose600.copy(alpha = 0.12f),
      TransactionStatus.FAILED.name,
    )
    TransactionStatus.CANCELLED.name -> Triple(
      Slate400,
      Slate700.copy(alpha = 0.35f),
      TransactionStatus.CANCELLED.name,
    )
    TransactionStatus.REJECTED.name -> Triple(
      Rose400,
      Rose600.copy(alpha = 0.12f),
      TransactionStatus.REJECTED.name,
    )
    else -> Triple(
      Slate400,
      Slate800,
      statusUpper.ifBlank { TransactionStatus.COMPLETED.name },
    )
  }

  Surface(
    color = Slate850,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, Slate700),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = iconBg,
            modifier = Modifier.size(40.dp),
          ) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = categoryIcon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
              )
            }
          }

          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = titleText,
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                ),
                color = Color.White,
              )

              // Status Badge
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = statusBg,
                border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.4f)),
              ) {
                Text(
                  text = statusLabel,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                  ),
                  color = statusColor,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                )
              }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
              text = "$dateFormatted • $displayDescription",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
              color = Slate400,
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Amount presentation with ৳ (Never ₹, INR, or $)
        val amountPrefix = if (isWithdraw) "- ৳" else if (isDeposit) "+ ৳" else if (isCredit) "+ ৳" else "- ৳"
        Text(
          text = "$amountPrefix ${"%,.2f".format(txn.amountInCurrency)}",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
          ),
          color = if (isCredit) Emerald400 else Rose400,
        )
      }

      // Reference / TrxID with one-tap copy if present
      if (txn.referenceId.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Slate800, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Ref: ${txn.referenceId}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = Slate400,
            modifier = Modifier.weight(1f),
          )

          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Slate800,
            modifier = Modifier
              .clickable {
                clipboardManager.setText(AnnotatedString(txn.referenceId))
              }
              .testTag("copy_ref_${txn.transactionId}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy Reference",
                tint = Gold400,
                modifier = Modifier.size(11.dp),
              )
              Text(
                text = "COPY",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold,
                ),
                color = Gold400,
              )
            }
          }
        }
      }
    }
  }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

