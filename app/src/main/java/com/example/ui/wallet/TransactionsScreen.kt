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
import com.example.ui.components.StatusBadge
import com.example.ui.components.UnifiedEsportsTopBar
import com.example.ui.theme.*
import com.example.core.i18n.*
import androidx.compose.ui.text.style.TextOverflow
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

private val Slate900 = Color(0xFF0F172A)
private val Slate850 = Color(0xFF131E35)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate50 = Color(0xFFF8FAFC)

enum class TransactionFilter(val testTag: String) {
  ALL("transactions_filter_all"),
  DEPOSIT("transactions_filter_deposit"),
  MATCH("transactions_filter_match"),
  WINNING("transactions_filter_winning"),
  WITHDRAW("transactions_filter_withdraw"),
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
        TransactionFilter.MATCH -> type in listOf("MATCH_ENTRY", "MATCH_JOIN", "MATCH_REFUND", "REFUND", "MATCH_ENTRY_FEE")
        TransactionFilter.WINNING -> type in listOf("MATCH_PRIZE", "MATCH_PRIZE_CREDIT", "PRIZE_WIN", "WIN", "WINNING")
        TransactionFilter.WITHDRAW -> type in listOf("WITHDRAW", "WITHDRAWAL", "WITHDRAW_HOLD", "WITHDRAW_RELEASE")
      }
    }.sortedByDescending { if (it.timestamp > 0L) it.timestamp else it.createdAt }
  }
  
  val strings = LocalAppStrings.current

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
            text = strings.txHistoryTitle,
            style = MaterialTheme.typography.headlineSmall.copy(
              fontWeight = FontWeight.Black,
              letterSpacing = 1.sp,
              fontSize = 20.sp,
            ),
            color = Color.White,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = strings.txHistorySubtitle,
            style = MaterialTheme.typography.bodySmall.copy(
              color = Slate400,
              fontSize = 13.sp,
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
                    text = strings.availableBalanceNotice("").replace(": ৳", "").uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 0.5.sp,
                      fontSize = 12.sp,
                    ),
                    color = Gold400,
                  )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = strings.taka(wallet?.availableAmount ?: 0.0),
                  style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
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
                  text = strings.statDeposited,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                  ),
                  color = Slate400,
                )
                Text(
                  text = strings.takaWhole(wallet?.totalDepositedAmount ?: 0.0),
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                  color = Cyan400,
                )
              }

              Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = strings.statWinnings,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                  ),
                  color = Slate400,
                )
                Text(
                  text = strings.takaWhole(wallet?.winningsAmount ?: 0.0),
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                  color = Emerald400,
                )
              }

              Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
              ) {
                Text(
                  text = strings.statWithdrawn,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                  ),
                  color = Slate400,
                )
                Text(
                  text = strings.takaWhole(wallet?.totalWithdrawnAmount ?: 0.0),
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
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
            val displayName = when (filter) {
              TransactionFilter.ALL -> strings.txHistoryFilterAll
              TransactionFilter.DEPOSIT -> strings.txHistoryFilterDeposit
              TransactionFilter.MATCH -> strings.txHistoryFilterMatch
              TransactionFilter.WINNING -> strings.txHistoryFilterWinning
              TransactionFilter.WITHDRAW -> strings.txHistoryFilterWithdraw
            }
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
                  text = displayName,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    fontSize = 12.sp,
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
                text = if (selectedFilter == TransactionFilter.ALL) strings.txHistoryEmptyTitle else strings.txHistoryEmptyFilterTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                color = Color.White,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = if (selectedFilter == TransactionFilter.ALL)
                  strings.txHistoryEmptyDesc
                else
                  strings.txHistoryEmptyFilterDesc,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
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
  val strings = LocalAppStrings.current
  val clipboardManager = LocalClipboardManager.current
  val context = LocalContext.current
  val display = buildTxDisplay(txn, strings)

  val icon = when (display.iconKind) {
    "withdraw" -> Icons.Default.ArrowUpward
    "deposit" -> Icons.Default.AddCircleOutline
    "prize" -> Icons.Default.EmojiEvents
    "refund" -> Icons.Default.RotateLeft
    "game" -> Icons.Default.SportsEsports
    "admin" -> Icons.Default.Build
    else -> Icons.Default.SportsEsports
  }
  
  val iconTint = when (display.iconKind) {
    "withdraw" -> Amber400
    "deposit" -> Cyan400
    "prize" -> Gold400
    "refund" -> Emerald400
    "admin" -> Rose400
    else -> Rose400
  }
  val iconBg = iconTint.copy(alpha = 0.15f)

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
                imageVector = icon,
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
                text = display.title,
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
              )

              StatusBadge(statusText = display.statusText)
            }

            if (display.description.isNotBlank()) {
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = display.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Slate400,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }

            if (display.dateText.isNotBlank()) {
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = display.dateText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = Slate500,
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        val amountPrefix = if (display.isVoid) "" else if (display.isCredit) "+ " else "- "
        Text(
          text = "$amountPrefix${strings.takaFromMinor(txn.displayMinorUnits)}",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
          ),
          color = if (display.isVoid) Slate400 else if (display.isCredit) Emerald400 else Rose400,
        )
      }

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
            text = "${strings.txHistoryRefLabel} ${txn.referenceId}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = Slate400,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Slate800,
            modifier = Modifier
              .clickable {
                clipboardManager.setText(AnnotatedString(txn.referenceId))
                Toast.makeText(context, strings.txHistoryCopied, Toast.LENGTH_SHORT).show()
              }
              .testTag("copy_ref_${txn.effectiveId}"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = strings.txHistoryCopyAction,
                tint = Gold400,
                modifier = Modifier.size(12.dp),
              )
              Text(
                text = strings.txHistoryCopyAction,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
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

