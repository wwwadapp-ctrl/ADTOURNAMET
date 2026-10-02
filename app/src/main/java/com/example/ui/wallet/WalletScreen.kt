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
import com.example.R
import com.example.core.i18n.LocalAppStrings
import com.example.ui.components.PremiumNotificationBellButton
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
  viewModel: WalletViewModel,
  onNavigateBack: () -> Unit,
  onNavigateToDeposit: () -> Unit,
  onNavigateToWithdraw: () -> Unit,
  onNavigateToTransactions: () -> Unit,
  onNotificationClick: () -> Unit = {},
  onProfileClick: () -> Unit = {},
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
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Slate900,
              border = BorderStroke(1.dp, Gold400.copy(alpha = 0.7f)),
              modifier = Modifier.size(38.dp),
            ) {
              Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
              ) {
                Image(
                  painter = painterResource(id = R.drawable.ic_launcher_foreground),
                  contentDescription = "AD TOURNAMENT",
                  modifier = Modifier.size(34.dp),
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
                  fontSize = 15.sp,
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
            testTag = "wallet_notification_button",
            contentDescription = "Notifications",
          )

          Spacer(modifier = Modifier.width(4.dp))

          // Profile button
          IconButton(
            onClick = onProfileClick,
            modifier = Modifier.testTag("wallet_profile_button"),
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
            fontWeight = FontWeight.Medium,
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
                  text = "মেইন ক্যাশ ব্যালেন্স (CASH)",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                  ),
                  color = Gold400,
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "৳ ${"%.2f".format(uiState.mainBalance)}",
                style = MaterialTheme.typography.displaySmall.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = (-0.5).sp,
                ),
                color = Color.White,
              )
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
                  text = strings.pendingEscrowNotice("%.2f".format(wallet?.pendingAmount ?: 0.0)),
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
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
                  text = "বোনাস ব্যালেন্স (BONUS)",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                  ),
                  color = Gold400,
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "৳ ${"%.2f".format(uiState.bonusBalance)}",
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = (-0.5).sp,
                ),
                color = Color.White,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "প্রতিটি ম্যাচে ১০% বোনাস ব্যবহার করা যাবে",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
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
              text = "${strings.depositAction} →",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
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
              text = "${strings.withdrawAction} →",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
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
            Text(
              text = "৳ ${"%.2f".format(effectiveTotalDeposited)}",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
              ),
              color = Color.White,
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
            Text(
              text = "৳ ${"%.2f".format(effectiveTotalWinnings)}",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
              ),
              color = Emerald400,
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
            Text(
              text = "৳ ${"%.2f".format(effectiveTotalWithdrawn)}",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
              ),
              color = Amber400,
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
                text = "${strings.viewAll} →",
                color = Gold400,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          val validTxns = remember(uiState.transactions) {
            uiState.transactions.filter { it.amount != 0L }
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
              val typeUpper = txn.type.uppercase()
              val isWithdraw = typeUpper.contains("WITHDRAW")
              val isDeposit = typeUpper.contains("DEPOSIT") && !isWithdraw
              val isCredit = !isWithdraw && (typeUpper.contains("PRIZE") || isDeposit || typeUpper.contains("REFUND") || typeUpper.contains("CREDIT") || typeUpper.contains("WIN"))
              val (icon, iconTint, iconBg) = when {
                isWithdraw -> Triple(Icons.Default.ArrowUpward, Amber400, Amber400.copy(alpha = 0.15f))
                isDeposit -> Triple(Icons.Default.AddCircleOutline, Cyan400, Cyan400.copy(alpha = 0.15f))
                typeUpper.contains("PRIZE") || typeUpper.contains("WIN") -> Triple(Icons.Default.EmojiEvents, Gold400, Gold500.copy(alpha = 0.15f))
                typeUpper.contains("REFUND") -> Triple(Icons.Default.RotateLeft, Emerald400, Emerald400.copy(alpha = 0.15f))
                else -> Triple(Icons.Default.SportsEsports, Rose400, Rose400.copy(alpha = 0.15f))
              }

              val displayTitle = when {
                isWithdraw -> "উইথড্র"
                isDeposit -> "ডিপোজিট"
                typeUpper.contains("PRIZE") || typeUpper.contains("WIN") -> strings.txnPrize
                typeUpper.contains("REFUND") -> strings.txnRefund
                typeUpper.contains("FEE") || typeUpper.contains("ENTRY") -> strings.txnEntryFee
                else -> txn.title.ifBlank { txn.type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() } }
              }

              val subText = txn.description.ifBlank { txn.referenceId.ifBlank { txn.source } }
              val dateText = formatTransactionDate(if (txn.timestamp > 0L) txn.timestamp else txn.createdAt)

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
                    text = displayTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                    ),
                    color = Color.White,
                  )
                  if (subText.isNotBlank()) {
                    Text(
                      text = subText,
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                      color = Slate400,
                      maxLines = 1,
                    )
                  }
                  Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Slate500,
                  )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                  val amountPrefix = if (isWithdraw) "- ৳" else if (isDeposit) "+ ৳" else if (isCredit) "+ ৳" else "- ৳"
                  Text(
                    text = "$amountPrefix ${"%.2f".format(txn.amount / 100.0)}",
                    style = MaterialTheme.typography.titleSmall.copy(
                      fontWeight = FontWeight.Black,
                      fontSize = 14.sp,
                    ),
                    color = if (isCredit) Emerald400 else Rose400,
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  StatusBadge(statusText = txn.status)
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

private fun formatTransactionDate(timestamp: Long): String {
  if (timestamp <= 0L) return "Recently"
  return try {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
    sdf.format(Date(timestamp))
  } catch (_: Exception) {
    "Recently"
  }
}

