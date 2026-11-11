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
        .background(MidnightBackgroundGradient)
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      // 2. PAGE IDENTITY
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 2.dp, bottom = 12.dp),
      ) {
        Text(
          text = strings.walletTitle,
          style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
          ),
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = strings.walletSubtitle,
          style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          ),
          color = Gold400,
        )
      }

      // 3. MAIN BALANCE CARD
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MidnightNavyCard,
        border = BorderStroke(
          0.8.dp,
          Brush.horizontalGradient(
            listOf(
              Gold400.copy(alpha = 0.6f),
              MidnightNavyBorder,
              NeonCyan.copy(alpha = 0.4f),
            )
          ),
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("wallet_balance_card"),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
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
                    .background(NeonEmerald),
                )
                Text(
                  text = strings.walletMainCashLabel,
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
              Spacer(modifier = Modifier.height(8.dp))
              if (uiState.isLoading && uiState.wallet == null) {
                CircularProgressIndicator(
                  modifier = Modifier.size(24.dp).padding(vertical = 2.dp),
                  color = Gold400,
                  strokeWidth = 2.dp,
                )
              } else {
                val mainBalanceText = strings.taka(uiState.mainBalance)
                Text(
                  text = mainBalanceText,
                  style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
                    letterSpacing = 0.sp,
                    fontSize = if (mainBalanceText.length > 10) 24.sp else 28.sp
                  ),
                  color = Color.White,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(end = 4.dp)
                )
              }
            }

            // 3D-Styled Gold Coin / Wallet Emblem Accent with gentle multi-layer radial glow
            Box(
              modifier = Modifier.size(62.dp),
              contentAlignment = Alignment.Center,
            ) {
              // Layer 1: Soft Ambient Outer Glow
              Box(
                modifier = Modifier
                  .size(58.dp)
                  .clip(CircleShape)
                  .background(
                    Brush.radialGradient(
                      colors = listOf(
                        Gold400.copy(alpha = 0.35f),
                        Color.Transparent,
                      )
                    )
                  )
              )

              // Layer 2: Metallic Gold Beveled Rim
              Surface(
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(
                  1.5.dp,
                  Brush.sweepGradient(
                    listOf(
                      Color(0xFFFFD54F),
                      Color(0xFFFF9100),
                      Color(0xFFFFE082),
                      Color(0xFFFF6D00),
                      Color(0xFFFFD54F),
                    )
                  ),
                ),
                modifier = Modifier.size(52.dp),
              ) {
                // Layer 3: Embossed Deep Gold Core
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(
                      Brush.radialGradient(
                        colors = listOf(
                          Color(0xFF2E2008),
                          Color(0xFF0F1524),
                        )
                      )
                    ),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Gold400,
                    modifier = Modifier.size(26.dp),
                  )
                }
              }
            }
          }

          if (pendingBalance > 0L) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Slate900.copy(alpha = 0.9f),
              border = BorderStroke(0.8.dp, Amber500.copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.HourglassEmpty,
                  contentDescription = null,
                  tint = Amber400,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = strings.pendingEscrowNotice(strings.takaWhole(wallet?.pendingAmount ?: 0.0)),
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                  color = Slate300,
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 3b. BONUS BALANCE CARD
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MidnightNavyCard,
        border = BorderStroke(
          0.8.dp,
          Brush.horizontalGradient(
            listOf(
              Cyan400.copy(alpha = 0.6f),
              MidnightNavyBorder,
              Emerald400.copy(alpha = 0.4f),
            )
          ),
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("wallet_bonus_balance_card"),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
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
                    .background(Gold400),
                )
                Text(
                  text = strings.walletBonusLabel,
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
              Spacer(modifier = Modifier.height(8.dp))
              if (uiState.isLoading && uiState.wallet == null) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp).padding(vertical = 2.dp),
                  color = Cyan400,
                  strokeWidth = 2.dp,
                )
              } else {
                val bonusBalanceText = strings.taka(uiState.bonusBalance)
                Text(
                  text = bonusBalanceText,
                  style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
                    letterSpacing = 0.sp,
                    fontSize = if (bonusBalanceText.length > 10) 18.sp else 20.sp
                  ),
                  color = Color.White,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(end = 4.dp)
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = strings.walletBonusUsageHint,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Slate400,
              )
            }

            Box(
              modifier = Modifier.size(52.dp),
              contentAlignment = Alignment.Center,
            ) {
              Surface(
                shape = CircleShape,
                color = Cyan400.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Cyan400.copy(alpha = 0.3f)),
                modifier = Modifier.size(48.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = Cyan400,
                    modifier = Modifier.size(24.dp),
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 4. ACTION CARDS
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // LEFT: ADD MONEY
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = NavyCard,
          border = BorderStroke(1.dp, Cyan400.copy(alpha = 0.45f)),
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onNavigateToDeposit)
            .testTag("wallet_deposit_button"),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = CircleShape,
                color = Cyan400.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp),
              ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = Cyan400,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
              Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Cyan400,
                modifier = Modifier.size(16.dp),
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = strings.addMoneyTitle,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp,
              ),
              color = Color.White,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = strings.depositAction,
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
              ),
              color = Cyan400,
            )
          }
        }

        // RIGHT: WITHDRAW
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = NavyCard,
          border = BorderStroke(1.dp, Gold500.copy(alpha = 0.45f)),
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onNavigateToWithdraw)
            .testTag("wallet_withdraw_button"),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = CircleShape,
                color = Gold500.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp),
              ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = Gold400,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
              Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Gold400,
                modifier = Modifier.size(16.dp),
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = strings.withdrawAction.uppercase(),
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp,
              ),
              color = Color.White,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = strings.withdrawAction,
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
              ),
              color = Gold400,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 5. WALLET STATISTICS (3-COLUMN)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // Total Deposited
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Slate900,
          border = BorderStroke(1.dp, Cyan400.copy(alpha = 0.25f)),
          modifier = Modifier.weight(1f),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 12.dp),
          ) {
            Text(
              text = strings.statDeposited,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
              ),
              color = Cyan400,
            )
            Spacer(modifier = Modifier.height(4.dp))
            val depositedText = strings.takaWhole(effectiveTotalDeposited)
            Text(
              text = depositedText,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
                letterSpacing = 0.sp,
                fontSize = if (depositedText.length > 8) 12.sp else 14.sp,
              ),
              color = Color.White,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(end = 4.dp)
            )
          }
        }

        // Total Winnings
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Slate900,
          border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.25f)),
          modifier = Modifier.weight(1f),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 12.dp),
          ) {
            Text(
              text = strings.statWinnings,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
              ),
              color = Emerald400,
            )
            Spacer(modifier = Modifier.height(4.dp))
            val winningsText = strings.takaWhole(effectiveTotalWinnings)
            Text(
              text = winningsText,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
                letterSpacing = 0.sp,
                fontSize = if (winningsText.length > 8) 12.sp else 14.sp,
              ),
              color = Emerald400,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(end = 4.dp)
            )
          }
        }

        // Total Withdrawn
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Slate900,
          border = BorderStroke(1.dp, Amber400.copy(alpha = 0.25f)),
          modifier = Modifier.weight(1f),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 12.dp),
          ) {
            Text(
              text = strings.statWithdrawn,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
              ),
              color = Amber400,
            )
            Spacer(modifier = Modifier.height(4.dp))
            val withdrawnText = strings.takaWhole(effectiveTotalWithdrawn)
            Text(
              text = withdrawnText,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
                letterSpacing = 0.sp,
                fontSize = if (withdrawnText.length > 8) 12.sp else 14.sp,
              ),
              color = Amber400,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(end = 4.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // 6. RECENT TRANSACTIONS
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = NavyCard,
        border = BorderStroke(1.dp, NavyCardBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = strings.recentTransactions,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
              ),
              color = Color.White,
            )
            TextButton(
              onClick = onNavigateToTransactions,
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
              Text(
                text = strings.viewAll,
                color = Gold400,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          val validTxns = remember(uiState.transactions) {
            uiState.transactions.filter { it.amount != 0L }
              .sortedByDescending { if (it.timestamp > 0L) it.timestamp else it.createdAt }
          }
          if (validTxns.isEmpty()) {
            // 7. EMPTY STATE
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Surface(
                shape = CircleShape,
                color = Slate900,
                border = BorderStroke(1.dp, Gold500.copy(alpha = 0.3f)),
                modifier = Modifier.size(56.dp),
              ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = Gold400.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp),
                  )
                }
              }
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = strings.noTransactionsYet,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                ),
                color = Color.White,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = strings.noTransactionsSubtext,
                style = MaterialTheme.typography.bodySmall,
                color = Slate400,
              )
            }
          } else {
            validTxns.take(5).forEachIndexed { index, txn ->
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

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  shape = CircleShape,
                  color = iconBg,
                  modifier = Modifier.size(38.dp),
                ) {
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = icon,
                      contentDescription = null,
                      tint = iconTint,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = display.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  if (display.description.isNotBlank()) {
                    Text(
                      text = display.description,
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                      color = Slate400,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  if (display.dateText.isNotBlank()) {
                    Text(
                      text = display.dateText,
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                      color = Slate500,
                    )
                  }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                  val amountPrefix = if (display.isVoid) "" else if (display.isCredit) "+ " else "- "
                  Text(
                    text = "$amountPrefix${strings.takaFromMinor(txn.displayMinorUnits)}",
                    style = MaterialTheme.typography.titleSmall.copy(
                      fontWeight = FontWeight.Black,
                      fontSize = 14.sp,
                    ),
                    color = if (display.isVoid) Slate400 else if (display.isCredit) Emerald400 else Rose400,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  StatusBadge(statusText = display.statusText)
                }
              }

              if (index < validTxns.take(5).size - 1) {
                HorizontalDivider(color = Slate800, thickness = 0.5.dp)
              }
            }
          }
        }
      }
    }
  }
}
