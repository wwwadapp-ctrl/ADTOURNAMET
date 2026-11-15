package com.example.ui.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import com.example.ui.components.StatusBadge
import com.example.ui.components.UnifiedEsportsTopBar
import com.example.ui.theme.*
import com.example.core.i18n.*
import androidx.compose.ui.text.style.TextOverflow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Slate900 = Color(0xFF0D1527)
private val Slate850 = Color(0xFF131E3A)
private val Slate800 = Color(0xFF1F2E4D)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate50 = Color(0xFFF8FAFC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
  viewModel: WalletViewModel,
  currentUser: UserEntity?,
  onNavigateBack: () -> Unit,
  onNavigateToDeposit: () -> Unit,
  onNavigateToWithdraw: () -> Unit,
  onNavigateToTransactions: () -> Unit,
  onNotificationClick: () -> Unit = {},
  onProfileClick: () -> Unit = {},
  onAdminClick: (() -> Unit)? = null,
  unreadNotificationsCount: Int = 0,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val wallet = uiState.wallet

  val availableBalance = wallet?.availableBalance ?: 0L
  val pendingBalance = wallet?.pendingBalance ?: 0L
  val totalDeposited = wallet?.totalDeposited ?: 0L
  val totalWithdrawn = wallet?.totalWithdrawn ?: 0L
  val totalWinnings = wallet?.totalWinnings ?: 0L

  val effectiveTotalWinnings: Double = remember(wallet, uiState.transactions) {
    val walletVal = wallet?.winningsAmount ?: 0.0

    val winningTxnsSum = uiState.transactions.filter { txn ->
      txn.amount != 0L &&
      txn.status.equals("COMPLETED", ignoreCase = true) &&
      (txn.type.equals("MATCH_PRIZE", ignoreCase = true) ||
       txn.type.equals("MATCH_PRIZE_CREDIT", ignoreCase = true) ||
       txn.type.equals("PRIZE_WIN", ignoreCase = true) ||
       txn.type.equals("WIN", ignoreCase = true) ||
       txn.type.equals("WINNING", ignoreCase = true))
    }.distinctBy { it.transactionId.ifBlank { it.referenceId } }.sumOf { txn ->
      txn.amountInCurrency
    }

    maxOf(walletVal, winningTxnsSum)
  }

  val effectiveTotalDeposited: Double = remember(wallet, uiState.transactions) {
    val fromWallet = wallet?.totalDepositedAmount ?: 0.0
    val fromTxns = uiState.transactions.filter { txn ->
      txn.amount != 0L &&
      txn.status.equals("COMPLETED", ignoreCase = true) &&
      txn.type.equals("DEPOSIT", ignoreCase = true)
    }.distinctBy { it.transactionId.ifBlank { it.referenceId } }.sumOf { it.amountInCurrency }
    maxOf(fromWallet, fromTxns)
  }

  val effectiveTotalWithdrawn: Double = remember(wallet, uiState.transactions) {
    val fromWallet = wallet?.totalWithdrawnAmount ?: 0.0
    val fromTxns = uiState.transactions.filter { txn ->
      txn.amount != 0L &&
      txn.status.equals("COMPLETED", ignoreCase = true) &&
      (txn.type.equals("WITHDRAW", ignoreCase = true) || txn.type.equals("WITHDRAWAL", ignoreCase = true))
    }.distinctBy { it.transactionId.ifBlank { it.referenceId } }.sumOf { it.amountInCurrency }
    maxOf(fromWallet, fromTxns)
  }

  Scaffold(
    topBar = {
      UnifiedEsportsTopBar(
        currentUser = currentUser,
        wallet = wallet,
        unreadNotificationsCount = unreadNotificationsCount,
        onWalletClick = {}, // Already on Wallet screen
        onNotificationClick = onNotificationClick,
        onProfileClick = onProfileClick,
        onAdminClick = onAdminClick,
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
      // 2. PAGE IDENTITY
      WlPageTitle(title = strings.walletTitle, subtitle = strings.walletSubtitle, modifier = Modifier.padding(top = 4.dp))

      Spacer(modifier = Modifier.height(14.dp))

      // 3. MAIN BALANCE CARD
      WlMainBalanceCard(
        label = strings.walletMainCashLabel,
        amountText = if (uiState.isLoading && uiState.wallet == null) null else strings.taka(uiState.mainBalance),
        pendingText = if (pendingBalance > 0L) strings.pendingEscrowNotice(strings.takaWhole(wallet?.pendingAmount ?: 0.0)) else null,
        modifier = Modifier.testTag("wallet_balance_card"),
      )

      Spacer(modifier = Modifier.height(14.dp))

      // 3b. BONUS BALANCE CARD
      WlBonusCard(
        label = strings.walletBonusLabel,
        amountText = if (uiState.isLoading && uiState.wallet == null) null else strings.taka(uiState.bonusBalance),
        hint = strings.walletBonusUsageHint,
        modifier = Modifier.testTag("wallet_bonus_balance_card"),
      )

      Spacer(modifier = Modifier.height(10.dp))

      // 4. ACTION CARDS
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        WlActionButton(
          primary = true,
          title = strings.addMoneyTitle,
          subtitle = strings.depositAction,
          picture = R.drawable.wl_moneybag,
          symbol = Icons.Default.Add,
          onClick = onNavigateToDeposit,
          tag = "wallet_deposit_button",
          modifier = Modifier.weight(1f),
        )
        WlActionButton(
          primary = false,
          title = strings.withdrawAction.uppercase(),
          subtitle = strings.withdrawAction,
          picture = R.drawable.wl_withdraw,
          symbol = Icons.Default.ArrowUpward,
          onClick = onNavigateToWithdraw,
          tag = "wallet_withdraw_button",
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 5. WALLET STATISTICS (3-COLUMN)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        WlStatTile(strings.statDeposited, strings.takaWhole(effectiveTotalDeposited), R.drawable.wl_moneybag, Color(0xFF4ADE80), WlTone.CREAM, Modifier.weight(1f))
        WlStatTile(strings.statWinnings, strings.takaWhole(effectiveTotalWinnings), R.drawable.wl_trophy, Color(0xFF7DA8FF), WlTone.CREAM, Modifier.weight(1f))
        WlStatTile(strings.statWithdrawn, strings.takaWhole(effectiveTotalWithdrawn), R.drawable.wl_withdraw, Color(0xFFFF9A4A), WlTone.GOLD, Modifier.weight(1f))
      }

      Spacer(modifier = Modifier.height(22.dp))

      // 6. RECENT TRANSACTIONS
      WlSectionHeader(title = strings.recentTransactions, viewAllText = strings.viewAll, onViewAll = onNavigateToTransactions)

      Spacer(modifier = Modifier.height(12.dp))

      WlFrame(gold = true, pad = 14.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
          val validTxns = remember(uiState.transactions) {
            uiState.transactions.filter { it.amount != 0L }
              .sortedByDescending { if (it.timestamp > 0L) it.timestamp else it.createdAt }
          }

          if (validTxns.isEmpty()) {
            // 7. EMPTY STATE
            WlEmptyState(title = strings.noTransactionsYet, description = strings.noTransactionsSubtext)
          } else {
            validTxns.take(5).forEachIndexed { index, txn ->
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
              )
              if (index < validTxns.take(5).size - 1) {
                Spacer(modifier = Modifier.height(10.dp))
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
