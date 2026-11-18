package com.example.ui.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
        .wlBackground()
        .padding(innerPadding)
        .testTag("transactions_list"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // 2. PAGE IDENTITY
      item {
        WlPageTitle(title = strings.txHistoryTitle, subtitle = strings.txHistorySubtitle, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
      }

      // 3. BALANCE SUMMARY CARD
      item {
        WlFrame(gold = true, pad = 14.dp) {
          Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 6.dp)) {
              Column(modifier = Modifier.weight(1f)) {
                WlPill(strings.availableBalanceNotice("").replace(": ৳", "").uppercase(), Color(0xFF8CFF4A), listOf(Color(0xFF0E3A2A), Color(0xFF07241A)), Color(0xFF4ADE80), Color(0xFFB8FFC8))
                Spacer(modifier = Modifier.height(8.dp))
                val availText = strings.taka(wallet?.availableAmount ?: 0.0)
                WlText3D(availText, if (availText.length > 10) 26.sp else 32.sp, WlTone.GOLD)
              }
              WlCoinMedal(74)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF0A0E3C), Color(0xFF101655))))
                .border(1.3.dp, Color(0xB3E8B84A), RoundedCornerShape(16.dp))
                .padding(horizontal = 4.dp, vertical = 8.dp),
            ) {
              WlMiniStat(R.drawable.wl_moneybag, strings.statDeposited, strings.takaWhole(wallet?.totalDepositedAmount ?: 0.0), Color(0xFF4ADE80), WlTone.CREAM, Modifier.weight(1f))
              WlMiniStat(R.drawable.wl_trophy, strings.statWinnings, strings.takaWhole(wallet?.winningsAmount ?: 0.0), Color(0xFF7DA8FF), WlTone.CREAM, Modifier.weight(1f))
              WlMiniStat(R.drawable.wl_withdraw, strings.statWithdrawn, strings.takaWhole(wallet?.totalWithdrawnAmount ?: 0.0), Color(0xFFFF9A4A), WlTone.GOLD, Modifier.weight(1f))
            }
          }
        }
      }

      // 4. FILTER BAR
      item {
        LazyRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(7.dp),
          contentPadding = PaddingValues(vertical = 4.dp),
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
            WlFilterChip(
              text = displayName,
              selected = isSelected,
              onClick = { selectedFilter = filter },
              tag = filter.testTag,
            )
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
          WlFrame(gold = true, pad = 12.dp, modifier = Modifier.padding(vertical = 8.dp)) {
            WlEmptyState(
              title = if (selectedFilter == TransactionFilter.ALL) strings.txHistoryEmptyTitle else strings.txHistoryEmptyFilterTitle,
              description = if (selectedFilter == TransactionFilter.ALL) strings.txHistoryEmptyDesc else strings.txHistoryEmptyFilterDesc,
              modifier = Modifier.padding(vertical = 8.dp),
            )
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
  val look = wlTxLook(display.iconKind, txn.type)
  val amountPrefix = if (display.isVoid) "" else if (display.isCredit) "+ " else "- "
  WlTxRow(
    picture = look.first,
    ring = look.second,
    title = display.title,
    statusText = display.statusText,
    statusKind = if (display.isVoid) 2 else if (display.statusText == strings.walletTxStatusPending) 1 else 0,
    description = display.description,
    date = display.dateText,
    amountText = "$amountPrefix${strings.takaFromMinor(txn.displayMinorUnits)}",
    amountTone = if (display.isVoid) WlTone.GREY else if (display.isCredit) WlTone.GREEN else WlTone.RED,
    refText = if (txn.referenceId.isNotBlank()) "${strings.txHistoryRefLabel} ${txn.referenceId}" else null,
    copyText = strings.txHistoryCopyAction,
    copyTag = "copy_ref_${txn.effectiveId}",
    onCopy = {
      clipboardManager.setText(AnnotatedString(txn.referenceId))
      Toast.makeText(context, strings.txHistoryCopied, Toast.LENGTH_SHORT).show()
    },
  )
}
