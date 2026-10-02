package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.DepositEntity
import com.example.domain.model.MatchEntity
import com.example.domain.model.ResultEntity
import com.example.domain.model.WithdrawalEntity
import com.example.ui.components.EmptyState
import com.example.ui.components.MatchStatusBadge
import com.example.ui.components.TournamentButton
import com.example.ui.components.TournamentButtonVariant
import com.example.ui.components.TournamentCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
  viewModel: AdminViewModel,
  onNavigateBack: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsState()
  val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

  var showCreateMatchDialog by remember { mutableStateOf(false) }
  var showBulkDialog by remember { mutableStateOf(false) }
  var matchToSetCode by remember { mutableStateOf<MatchEntity?>(null) }
  var matchToCancel by remember { mutableStateOf<MatchEntity?>(null) }
  var gameCodeInput by remember { mutableStateOf("") }
  var cancelReasonInput by remember { mutableStateOf("") }

  var newMatchTitle by remember { mutableStateOf("") }
  var newMatchGame by remember { mutableStateOf("LUDO") }
  var newMatchFee by remember { mutableStateOf("50") }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Super Admin Dashboard",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
            )
            Text(
              text = "Admin ID: ${viewModel.adminUid}",
              style = MaterialTheme.typography.labelSmall,
              color = Gold400,
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
          }
        },
        actions = {
          IconButton(onClick = { viewModel.loadAllData() }) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Gold400)
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
        .padding(innerPadding),
    ) {
      // Tab Bar
      ScrollableTabRow(
        selectedTabIndex = uiState.selectedTab.ordinal,
        containerColor = NavySurface,
        contentColor = Gold400,
        edgePadding = 12.dp,
        divider = {},
      ) {
        AdminTab.values().forEach { tab ->
          val count = when (tab) {
            AdminTab.DEPOSITS -> uiState.pendingDeposits.size
            AdminTab.WITHDRAWALS -> uiState.pendingWithdrawals.size
            AdminTab.MATCHES -> uiState.allMatches.size
            AdminTab.RESULTS -> uiState.pendingResults.size
            AdminTab.AUDIT_LOGS -> uiState.auditLogs.size
            AdminTab.SETTINGS -> 0
          }
          Tab(
            selected = uiState.selectedTab == tab,
            onClick = { viewModel.setTab(tab) },
            text = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = tab.name.replace("_", " "),
                  fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                )
                if (count > 0 && (tab == AdminTab.DEPOSITS || tab == AdminTab.WITHDRAWALS || tab == AdminTab.RESULTS)) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    color = if (tab == AdminTab.RESULTS) Amber500 else Emerald500,
                    shape = RoundedCornerShape(10.dp),
                  ) {
                    Text(
                      text = "$count",
                      color = Color.Black,
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                  }
                }
              }
            },
          )
        }
      }

      // Success & Error Banners
      if (uiState.successMessage != null) {
        Surface(
          color = Emerald900.copy(alpha = 0.6f),
          border = BorderStroke(1.dp, Emerald500),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          shape = RoundedCornerShape(8.dp),
        ) {
          Text(
            text = uiState.successMessage!!,
            color = Emerald400,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(10.dp),
          )
        }
      }

      if (uiState.errorMessage != null) {
        Surface(
          color = Rose900.copy(alpha = 0.6f),
          border = BorderStroke(1.dp, Rose500),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          shape = RoundedCornerShape(8.dp),
        ) {
          Text(
            text = uiState.errorMessage!!,
            color = Rose400,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(10.dp),
          )
        }
      }

      // Content based on tab
      when (uiState.selectedTab) {
        AdminTab.DEPOSITS -> DepositsTab(
          deposits = uiState.pendingDeposits,
          onApprove = { viewModel.approveDeposit(it) },
          onReject = { id -> viewModel.rejectDeposit(id, "Verification failed / Invalid TrxID") },
        )
        AdminTab.WITHDRAWALS -> WithdrawalsTab(
          withdrawals = uiState.pendingWithdrawals,
          processingIds = uiState.processingWithdrawalIds,
          onApprove = { viewModel.approveWithdrawal(it) },
          onReject = { w, reason -> viewModel.rejectWithdrawal(w, reason) },
        )
        AdminTab.MATCHES -> MatchesTab(
          matches = uiState.allMatches,
          onCreateMatchClick = { showCreateMatchDialog = true },
          onBulkCreateClick = { showBulkDialog = true },
          onSetCodeClick = { match ->
            matchToSetCode = match
            gameCodeInput = match.gameCode
          },
          onStartMatchClick = { match -> viewModel.startMatch(match.matchId) },
          onCancelMatchClick = { match ->
            matchToCancel = match
            cancelReasonInput = ""
          },
        )
        AdminTab.RESULTS -> ResultsTab(
          results = uiState.pendingResults,
          onApprove = { viewModel.approveResult(it) },
          onReject = { id, matchId -> viewModel.rejectResult(id, matchId, "Victory proof rejected by admin") },
        )
        AdminTab.AUDIT_LOGS -> AuditLogsTab(
          logs = uiState.auditLogs,
          dateFormat = dateFormat,
        )
        AdminTab.SETTINGS -> SettingsTab(
          settings = uiState.appSettings,
          isSaving = uiState.isSavingSettings,
          onSave = { viewModel.updateSettings(it) },
        )
      }
    }
  }

  // Dialogs
  if (showCreateMatchDialog) {
    AlertDialog(
      onDismissRequest = { showCreateMatchDialog = false },
      containerColor = NavyCard,
      title = { Text("Create New Tournament Match", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = newMatchTitle,
            onValueChange = { newMatchTitle = it },
            label = { Text("Match Title") },
            placeholder = { Text("e.g. Ludo Mega 1v1 #201") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("LUDO", "CARROM").forEach { g ->
              FilterChip(
                selected = newMatchGame == g,
                onClick = { newMatchGame = g },
                label = { Text(g) },
              )
            }
          }
          OutlinedTextField(
            value = newMatchFee,
            onValueChange = { newMatchFee = it },
            label = { Text("Entry Fee (৳)") },
            placeholder = { Text("50") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
          )
        }
      },
      confirmButton = {
        TournamentButton(
          text = "CREATE",
          onClick = {
            val fee = newMatchFee.toDoubleOrNull() ?: 50.0
            viewModel.createSingleMatch(newMatchTitle, newMatchGame, fee, 0L)
            showCreateMatchDialog = false
          },
        )
      },
      dismissButton = {
        TextButton(onClick = { showCreateMatchDialog = false }) { Text("Cancel", color = Slate400) }
      },
    )
  }

  if (showBulkDialog) {
    AlertDialog(
      onDismissRequest = { showBulkDialog = false },
      containerColor = NavyCard,
      title = { Text("Bulk Create 50 Matches", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          text = "This will automatically generate 50 tournament matches scheduled across the day with 15-minute intervals. Entry fee: ৳50 (Prize: ৳90).",
          color = Slate300,
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        TournamentButton(
          text = "CREATE 50 MATCHES",
          onClick = {
            viewModel.bulkCreateMatches("LUDO", 50, 50.0)
            showBulkDialog = false
          },
        )
      },
      dismissButton = {
        TextButton(onClick = { showBulkDialog = false }) { Text("Cancel", color = Slate400) }
      },
    )
  }

  if (matchToSetCode != null) {
    val m = matchToSetCode!!
    AlertDialog(
      onDismissRequest = { matchToSetCode = null },
      containerColor = NavyCard,
      title = { Text("Set Room Code for ${m.matchNumber}", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Enter 6-8 digit game room code from Ludo King or Carrom Pool.", color = Slate300, style = MaterialTheme.typography.bodySmall)
          OutlinedTextField(
            value = gameCodeInput,
            onValueChange = { gameCodeInput = it },
            label = { Text("Game Room Code") },
            placeholder = { Text("e.g. 05829104") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
          )
        }
      },
      confirmButton = {
        TournamentButton(
          text = "SET CODE",
          onClick = {
            viewModel.setMatchGameCode(m.matchId, gameCodeInput.trim())
            matchToSetCode = null
          },
        )
      },
      dismissButton = {
        TextButton(onClick = { matchToSetCode = null }) { Text("Cancel", color = Slate400) }
      },
    )
  }

  if (matchToCancel != null) {
    val m = matchToCancel!!
    AlertDialog(
      onDismissRequest = { matchToCancel = null },
      containerColor = NavyCard,
      title = { Text("Cancel Match ${m.matchNumber}?", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Cancelling will immediately refund held entry fees back to all joined players.", color = Slate300, style = MaterialTheme.typography.bodySmall)
          OutlinedTextField(
            value = cancelReasonInput,
            onValueChange = { cancelReasonInput = it },
            label = { Text("Cancellation Reason") },
            placeholder = { Text("e.g. Technical server error") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
          )
        }
      },
      confirmButton = {
        TournamentButton(
          text = "CANCEL & REFUND",
          variant = TournamentButtonVariant.DANGER,
          onClick = {
            viewModel.cancelMatch(m.matchId, cancelReasonInput.ifBlank { "Cancelled by admin" })
            matchToCancel = null
          },
        )
      },
      dismissButton = {
        TextButton(onClick = { matchToCancel = null }) { Text("Dismiss", color = Slate400) }
      },
    )
  }
}

@Composable
private fun DepositsTab(
  deposits: List<DepositEntity>,
  onApprove: (DepositEntity) -> Unit,
  onReject: (String) -> Unit,
) {
  if (deposits.isEmpty()) {
    EmptyState(
      title = "No Pending Deposits",
      description = "No deposit verification requests awaiting review.",
      modifier = Modifier.padding(24.dp),
    )
  } else {
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(deposits, key = { it.depositId }) { dep ->
        TournamentCard(modifier = Modifier.fillMaxWidth(), backgroundColor = NavyCard, borderColor = NavyCardBorder) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
              Text(
                text = "৳ ${"%.0f".format(dep.amount / 100.0)} via ${dep.method}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
              )
              Text(text = "Sender: ${dep.senderNumber}", style = MaterialTheme.typography.bodySmall, color = Slate400)
              Text(text = "TrxID: ${dep.trxId}", style = MaterialTheme.typography.bodySmall, color = Gold400)
              Text(text = "User UID: ${dep.userId.ifEmpty { dep.uid }}", style = MaterialTheme.typography.labelSmall, color = Slate500)
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TournamentButton(
              text = "APPROVE",
              onClick = { onApprove(dep) },
              modifier = Modifier.weight(1f),
            )
            TournamentButton(
              text = "REJECT",
              onClick = { onReject(dep.depositId) },
              variant = TournamentButtonVariant.DANGER,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun WithdrawalsTab(
  withdrawals: List<WithdrawalEntity>,
  processingIds: Set<String> = emptySet(),
  onApprove: (WithdrawalEntity) -> Unit,
  onReject: (WithdrawalEntity, String) -> Unit,
) {
  var withdrawalToApprove by remember { mutableStateOf<WithdrawalEntity?>(null) }
  var withdrawalToReject by remember { mutableStateOf<WithdrawalEntity?>(null) }
  var rejectReason by remember { mutableStateOf("") }

  if (withdrawals.isEmpty()) {
    EmptyState(
      title = "No Pending Withdrawals",
      description = "No withdrawal payout requests awaiting review.",
      modifier = Modifier.padding(24.dp),
    )
  } else {
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(withdrawals, key = { it.withdrawalId }) { w ->
        val isProcessing = processingIds.contains(w.withdrawalId)
        TournamentCard(modifier = Modifier.fillMaxWidth(), backgroundColor = NavyCard, borderColor = NavyCardBorder) {
          Column {
            Text(
              text = "৳ ${"%.0f".format(w.amount / 100.0)} payout via ${w.method}",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
            )
            Text(text = "Recipient: ${w.recipientNumber}", style = MaterialTheme.typography.bodySmall, color = Gold400)
            Text(text = "Payment Details: ${w.paymentDetails}", style = MaterialTheme.typography.bodySmall, color = Slate400)
            Text(text = "User UID: ${w.userId.ifEmpty { w.uid }}", style = MaterialTheme.typography.labelSmall, color = Slate500)
          }
          Spacer(modifier = Modifier.height(10.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TournamentButton(
              text = if (isProcessing) "PROCESSING..." else "APPROVE PAYOUT",
              onClick = { withdrawalToApprove = w },
              enabled = !isProcessing,
              modifier = Modifier.weight(1f),
            )
            TournamentButton(
              text = "REJECT",
              onClick = {
                withdrawalToReject = w
                rejectReason = ""
              },
              variant = TournamentButtonVariant.DANGER,
              enabled = !isProcessing,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }

  // Confirmation Dialog for Approval
  if (withdrawalToApprove != null) {
    val w = withdrawalToApprove!!
    AlertDialog(
      onDismissRequest = { withdrawalToApprove = null },
      containerColor = NavyCard,
      title = { Text("Confirm Withdrawal Payout", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "Are you sure you want to approve this payout?",
            color = Slate200,
            style = MaterialTheme.typography.bodyMedium,
          )
          Text(
            "• Amount: ৳ ${"%.2f".format(w.amount / 100.0)}",
            color = Gold400,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          )
          Text(
            "• Recipient: ${w.recipientNumber} (${w.method})",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
          )
          Text(
            "• User UID: ${w.userId.ifEmpty { w.uid }}",
            color = Slate400,
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            "This will mark the withdrawal COMPLETED and finalize balance deduction.",
            color = Slate400,
            style = MaterialTheme.typography.labelSmall,
          )
        }
      },
      confirmButton = {
        TournamentButton(
          text = "CONFIRM PAYOUT",
          onClick = {
            onApprove(w)
            withdrawalToApprove = null
          },
        )
      },
      dismissButton = {
        TournamentButton(
          text = "CANCEL",
          onClick = { withdrawalToApprove = null },
          variant = TournamentButtonVariant.SECONDARY,
        )
      },
    )
  }

  // Confirmation Dialog for Rejection
  if (withdrawalToReject != null) {
    val w = withdrawalToReject!!
    AlertDialog(
      onDismissRequest = { withdrawalToReject = null },
      containerColor = NavyCard,
      title = { Text("Reject Withdrawal Request", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "Rejecting this request will immediately refund ৳ ${"%.2f".format(w.amount / 100.0)} back to the user's available balance.",
            color = Slate200,
            style = MaterialTheme.typography.bodyMedium,
          )
          Text(
            "Recipient: ${w.recipientNumber} (${w.method})",
            color = Slate400,
            style = MaterialTheme.typography.bodySmall,
          )
          OutlinedTextField(
            value = rejectReason,
            onValueChange = { rejectReason = it },
            label = { Text("Rejection Reason (Required)") },
            placeholder = { Text("e.g. Invalid account number or limit reached") },
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        TournamentButton(
          text = "CONFIRM REJECTION",
          onClick = {
            if (rejectReason.trim().isNotEmpty()) {
              onReject(w, rejectReason.trim())
              withdrawalToReject = null
            }
          },
          variant = TournamentButtonVariant.DANGER,
          enabled = rejectReason.trim().isNotEmpty(),
        )
      },
      dismissButton = {
        TournamentButton(
          text = "CANCEL",
          onClick = { withdrawalToReject = null },
          variant = TournamentButtonVariant.SECONDARY,
        )
      },
    )
  }
}

@Composable
private fun MatchesTab(
  matches: List<MatchEntity>,
  onCreateMatchClick: () -> Unit,
  onBulkCreateClick: () -> Unit,
  onSetCodeClick: (MatchEntity) -> Unit,
  onStartMatchClick: (MatchEntity) -> Unit,
  onCancelMatchClick: (MatchEntity) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TournamentButton(
          text = "+ CREATE MATCH",
          onClick = onCreateMatchClick,
          modifier = Modifier.weight(1f),
        )
        TournamentButton(
          text = "BULK 50 MATCHES",
          onClick = onBulkCreateClick,
          variant = TournamentButtonVariant.SECONDARY,
          modifier = Modifier.weight(1f),
        )
      }
    }

    if (matches.isEmpty()) {
      item {
        EmptyState(
          title = "No Matches Found",
          description = "No tournament matches created yet. Create one or bulk generate.",
          modifier = Modifier.padding(24.dp),
        )
      }
    } else {
      items(matches, key = { it.matchId }) { m ->
        TournamentCard(modifier = Modifier.fillMaxWidth(), backgroundColor = NavyCard, borderColor = NavyCardBorder) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
              Text(text = m.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
              Text(text = "${m.gameType} • Fee: ৳${"%.0f".format(m.displayEntryFee)} • Prize: ৳${"%.0f".format(m.displayPrizePool)}", style = MaterialTheme.typography.bodySmall, color = Slate400)
              if (m.gameCode.isNotBlank()) {
                Text(text = "Code: ${m.gameCode}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Gold400)
              }
            }
            MatchStatusBadge(
              status = m.status,
              joinedPlayersCount = m.joinedPlayersCount,
              maxPlayers = m.maxPlayers,
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TournamentButton(
              text = if (m.gameCode.isBlank()) "SET CODE" else "EDIT CODE",
              onClick = { onSetCodeClick(m) },
              modifier = Modifier.weight(1f),
            )
            if (m.status != "RUNNING" && m.status != "COMPLETED") {
              TournamentButton(
                text = "START",
                onClick = { onStartMatchClick(m) },
                variant = TournamentButtonVariant.SECONDARY,
                modifier = Modifier.weight(0.8f),
              )
            }
            if (m.status != "COMPLETED" && m.status != "CANCELLED") {
              TournamentButton(
                text = "CANCEL",
                onClick = { onCancelMatchClick(m) },
                variant = TournamentButtonVariant.DANGER,
                modifier = Modifier.weight(0.8f),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ResultsTab(
  results: List<ResultEntity>,
  onApprove: (ResultEntity) -> Unit,
  onReject: (String, String) -> Unit,
) {
  var previewImageUrl by remember { mutableStateOf<String?>(null) }

  if (results.isEmpty()) {
    EmptyState(
      title = "No Pending Results",
      description = "No victory proofs awaiting review.",
      modifier = Modifier.padding(24.dp),
    )
  } else {
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(results, key = { it.resultId }) { res ->
        TournamentCard(modifier = Modifier.fillMaxWidth(), backgroundColor = NavyCard, borderColor = Amber500) {
          Text(text = "Victory Claim: Match ${res.matchId}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = "Winner UID: ${res.claimedWinnerUserId.ifEmpty { res.submittedByUserId }}", style = MaterialTheme.typography.bodySmall, color = Gold400)
          
          if (res.proofScreenshotUrl.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Uploaded Screenshot Proof:", style = MaterialTheme.typography.labelSmall, color = Slate400)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Slate900, RoundedCornerShape(8.dp))
                .clickable { previewImageUrl = res.proofScreenshotUrl }
                .padding(4.dp),
              contentAlignment = Alignment.Center,
            ) {
              AsyncImage(
                model = res.proofScreenshotUrl,
                contentDescription = "Victory proof screenshot",
                modifier = Modifier
                  .fillMaxSize()
                  .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "Tap image to inspect full size", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = Gold400)
          }

          if (res.reviewNotes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Notes: ${res.reviewNotes}", style = MaterialTheme.typography.bodySmall, color = Slate400)
          }
          Spacer(modifier = Modifier.height(10.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TournamentButton(
              text = "APPROVE & PAY",
              onClick = { onApprove(res) },
              modifier = Modifier.weight(1f),
            )
            TournamentButton(
              text = "REJECT",
              onClick = { onReject(res.resultId, res.matchId) },
              variant = TournamentButtonVariant.DANGER,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }
    }
  }

  if (previewImageUrl != null) {
    AlertDialog(
      onDismissRequest = { previewImageUrl = null },
      containerColor = NavyCard,
      title = {
        Text(text = "Screenshot Inspection", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
      },
      text = {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .background(Slate900, RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center,
        ) {
          AsyncImage(
            model = previewImageUrl,
            contentDescription = "Full size screenshot proof",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
          )
        }
      },
      confirmButton = {
        TournamentButton(
          text = "CLOSE",
          onClick = { previewImageUrl = null },
        )
      },
    )
  }
}

@Composable
private fun SettingsTab(
  settings: com.example.domain.model.AppSettingsEntity,
  isSaving: Boolean,
  onSave: (com.example.domain.model.AppSettingsEntity) -> Unit,
) {
  var bkashNumber by remember(settings) { mutableStateOf(settings.effectiveBkashNumber) }
  var nagadNumber by remember(settings) { mutableStateOf(settings.effectiveNagadNumber) }
  var minDeposit by remember(settings.minDepositAmount) { mutableStateOf(settings.minDepositAmount.toInt().toString()) }
  var minWithdrawal by remember(settings.minWithdrawalAmount) { mutableStateOf(settings.minWithdrawalAmount.toInt().toString()) }
  var howToJoinUrl by remember(settings.howToJoinVideoUrl) { mutableStateOf(settings.howToJoinVideoUrl) }
  var howToDepositUrl by remember(settings.howToDepositVideoUrl) { mutableStateOf(settings.howToDepositVideoUrl) }
  var howToSubmitResultUrl by remember(settings.howToSubmitResultVideoUrl) { mutableStateOf(settings.howToSubmitResultVideoUrl) }
  var tournamentRulesUrl by remember(settings.tournamentRulesVideoUrl) { mutableStateOf(settings.tournamentRulesVideoUrl) }
  var customBannerUrl by remember(settings.customBannerImageUrl) { mutableStateOf(settings.customBannerImageUrl) }
  var depositBannerUrl by remember(settings.depositBannerImageUrl) { mutableStateOf(settings.depositBannerImageUrl) }
  var matchJoinBannerUrl by remember(settings.matchJoinBannerImageUrl) { mutableStateOf(settings.matchJoinBannerImageUrl) }
  var resultSubmitBannerUrl by remember(settings.resultSubmitBannerImageUrl) { mutableStateOf(settings.resultSubmitBannerImageUrl) }
  var rulesBannerUrl by remember(settings.rulesBannerImageUrl) { mutableStateOf(settings.rulesBannerImageUrl) }
  var whatsappNumber by remember(settings.supportWhatsappNumber) { mutableStateOf(settings.supportWhatsappNumber) }
  var telegramUrl by remember(settings.supportTelegramUrl) { mutableStateOf(settings.supportTelegramUrl) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    TournamentCard(
      modifier = Modifier.fillMaxWidth(),
      backgroundColor = NavyCard,
      borderColor = NavyCardBorder,
    ) {
      Text(
        text = "Support & Social Channels (/appSettings)",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = Gold400,
      )
      Text(
        text = "Configure WhatsApp support and official Telegram channel shown in player profiles.",
        style = MaterialTheme.typography.bodySmall,
        color = Slate400,
      )
      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = whatsappNumber,
        onValueChange = { whatsappNumber = it },
        label = { Text("WhatsApp Support Number") },
        placeholder = { Text("e.g. 8801614899093") },
        leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = Gold400) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = Gold400,
          unfocusedBorderColor = Slate700,
        ),
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = telegramUrl,
        onValueChange = { telegramUrl = it },
        label = { Text("Official Telegram Link") },
        placeholder = { Text("e.g. https://t.me/adtournament") },
        leadingIcon = { Icon(Icons.Default.Send, contentDescription = null, tint = Cyan400) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = Cyan400,
          unfocusedBorderColor = Slate700,
        ),
      )

      Spacer(modifier = Modifier.height(20.dp))

      TournamentButton(
        text = "SAVE SUPPORT SETTINGS",
        isLoading = isSaving,
        onClick = {
          val updated = settings.copy(
            bkashNumber = bkashNumber.trim(),
            nagadNumber = nagadNumber.trim(),
            finance = settings.finance.copy(
              bkashNumber = bkashNumber.trim(),
              nagadNumber = nagadNumber.trim(),
            ),
            minDepositAmount = minDeposit.toDoubleOrNull() ?: 50.0,
            minWithdrawalAmount = minWithdrawal.toDoubleOrNull() ?: 100.0,
            howToJoinVideoUrl = howToJoinUrl.trim(),
            howToDepositVideoUrl = howToDepositUrl.trim(),
            howToSubmitResultVideoUrl = howToSubmitResultUrl.trim(),
            tournamentRulesVideoUrl = tournamentRulesUrl.trim(),
            customBannerImageUrl = customBannerUrl.trim(),
            depositBannerImageUrl = depositBannerUrl.trim(),
            matchJoinBannerImageUrl = matchJoinBannerUrl.trim(),
            resultSubmitBannerImageUrl = resultSubmitBannerUrl.trim(),
            rulesBannerImageUrl = rulesBannerUrl.trim(),
            supportWhatsappNumber = whatsappNumber.trim(),
            supportTelegramUrl = telegramUrl.trim(),
          )
          onSave(updated)
        },
        modifier = Modifier.fillMaxWidth(),
      )
    }

    TournamentCard(
      modifier = Modifier.fillMaxWidth(),
      backgroundColor = NavyCard,
      borderColor = NavyCardBorder,
    ) {
      Text(
        text = "Payment Numbers & Limits (/appSettings)",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = Gold400,
      )
      Text(
        text = "Configure official bKash and Nagad numbers shown to all players in the deposit flow.",
        style = MaterialTheme.typography.bodySmall,
        color = Slate400,
      )
      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = bkashNumber,
        onValueChange = { bkashNumber = it },
        label = { Text("Official bKash (Personal) Number") },
        placeholder = { Text("e.g. 01700000000") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = Color(0xFFE2136E),
          unfocusedBorderColor = Slate700,
        ),
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = nagadNumber,
        onValueChange = { nagadNumber = it },
        label = { Text("Official Nagad (Personal) Number") },
        placeholder = { Text("e.g. 01800000000") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = Color(0xFFF7941D),
          unfocusedBorderColor = Slate700,
        ),
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = minDeposit,
          onValueChange = { minDeposit = it },
          label = { Text("Min Deposit (৳)") },
          singleLine = true,
          modifier = Modifier.weight(1f),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Cyan400,
            unfocusedBorderColor = Slate700,
          ),
        )
        OutlinedTextField(
          value = minWithdrawal,
          onValueChange = { minWithdrawal = it },
          label = { Text("Min Withdrawal (৳)") },
          singleLine = true,
          modifier = Modifier.weight(1f),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Cyan400,
            unfocusedBorderColor = Slate700,
          ),
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      TournamentButton(
        text = "SAVE PAYMENT SETTINGS",
        isLoading = isSaving,
        onClick = {
          val updated = settings.copy(
            bkashNumber = bkashNumber.trim(),
            nagadNumber = nagadNumber.trim(),
            finance = settings.finance.copy(
              bkashNumber = bkashNumber.trim(),
              nagadNumber = nagadNumber.trim(),
            ),
            minDepositAmount = minDeposit.toDoubleOrNull() ?: 50.0,
            minWithdrawalAmount = minWithdrawal.toDoubleOrNull() ?: 100.0,
            howToJoinVideoUrl = howToJoinUrl.trim(),
            howToDepositVideoUrl = howToDepositUrl.trim(),
            howToSubmitResultVideoUrl = howToSubmitResultUrl.trim(),
            tournamentRulesVideoUrl = tournamentRulesUrl.trim(),
            customBannerImageUrl = customBannerUrl.trim(),
            depositBannerImageUrl = depositBannerUrl.trim(),
            matchJoinBannerImageUrl = matchJoinBannerUrl.trim(),
            resultSubmitBannerImageUrl = resultSubmitBannerUrl.trim(),
            rulesBannerImageUrl = rulesBannerUrl.trim(),
            supportWhatsappNumber = whatsappNumber.trim(),
            supportTelegramUrl = telegramUrl.trim(),
          )
          onSave(updated)
        },
        modifier = Modifier.fillMaxWidth(),
      )
    }

    TournamentCard(
      modifier = Modifier.fillMaxWidth(),
      backgroundColor = NavyCard,
      borderColor = NavyCardBorder,
    ) {
      Text(
        text = "Tutorial Videos & Banner Media (/appSettings)",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = Cyan400,
      )
      Text(
        text = "Add YouTube or video URLs for the 4 guide modals, and optional custom banner image URL.",
        style = MaterialTheme.typography.bodySmall,
        color = Slate400,
      )
      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = howToJoinUrl,
        onValueChange = { howToJoinUrl = it },
        label = { Text("Match Join Video URL (YouTube/Web)") },
        placeholder = { Text("https://youtube.com/watch?v=...") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = Cyan400,
          unfocusedBorderColor = Slate700,
        ),
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = howToDepositUrl,
        onValueChange = { howToDepositUrl = it },
        label = { Text("Deposit Tutorial Video URL") },
        placeholder = { Text("https://youtube.com/watch?v=...") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = Cyan400,
          unfocusedBorderColor = Slate700,
        ),
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = howToSubmitResultUrl,
        onValueChange = { howToSubmitResultUrl = it },
        label = { Text("Result Submit Tutorial Video URL") },
        placeholder = { Text("https://youtube.com/watch?v=...") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = Cyan400,
          unfocusedBorderColor = Slate700,
        ),
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = tournamentRulesUrl,
        onValueChange = { tournamentRulesUrl = it },
        label = { Text("Tournament Rules Video URL") },
        placeholder = { Text("https://youtube.com/watch?v=...") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = Cyan400,
          unfocusedBorderColor = Slate700,
        ),
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = depositBannerUrl,
        onValueChange = { depositBannerUrl = it },
        label = { Text("Deposit Banner Image URL (3D/HD)") },
        placeholder = { Text("https://example.com/deposit_banner.png") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = Cyan400,
          unfocusedBorderColor = Slate700,
        ),
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = matchJoinBannerUrl,
        onValueChange = { matchJoinBannerUrl = it },
        label = { Text("Match Join Banner Image URL (3D/HD)") },
        placeholder = { Text("https://example.com/match_banner.png") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = Cyan400,
          unfocusedBorderColor = Slate700,
        ),
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = resultSubmitBannerUrl,
        onValueChange = { resultSubmitBannerUrl = it },
        label = { Text("Result Submit Banner Image URL (3D/HD)") },
        placeholder = { Text("https://example.com/result_banner.png") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = Cyan400,
          unfocusedBorderColor = Slate700,
        ),
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = rulesBannerUrl,
        onValueChange = { rulesBannerUrl = it },
        label = { Text("Rules Banner Image URL (3D/HD)") },
        placeholder = { Text("https://example.com/rules_banner.png") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = Cyan400,
          unfocusedBorderColor = Slate700,
        ),
      )

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = customBannerUrl,
        onValueChange = { customBannerUrl = it },
        label = { Text("Default / Fallback Banner Image URL (Optional)") },
        placeholder = { Text("https://example.com/banner.png") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = Color.White,
          unfocusedTextColor = Color.White,
          focusedBorderColor = Cyan400,
          unfocusedBorderColor = Slate700,
        ),
      )

      Spacer(modifier = Modifier.height(20.dp))

      TournamentButton(
        text = "SAVE TUTORIAL & BANNER SETTINGS",
        isLoading = isSaving,
        onClick = {
          val updated = settings.copy(
            bkashNumber = bkashNumber.trim(),
            nagadNumber = nagadNumber.trim(),
            finance = settings.finance.copy(
              bkashNumber = bkashNumber.trim(),
              nagadNumber = nagadNumber.trim(),
            ),
            minDepositAmount = minDeposit.toDoubleOrNull() ?: 50.0,
            minWithdrawalAmount = minWithdrawal.toDoubleOrNull() ?: 100.0,
            howToJoinVideoUrl = howToJoinUrl.trim(),
            howToDepositVideoUrl = howToDepositUrl.trim(),
            howToSubmitResultVideoUrl = howToSubmitResultUrl.trim(),
            tournamentRulesVideoUrl = tournamentRulesUrl.trim(),
            customBannerImageUrl = customBannerUrl.trim(),
            depositBannerImageUrl = depositBannerUrl.trim(),
            matchJoinBannerImageUrl = matchJoinBannerUrl.trim(),
            resultSubmitBannerImageUrl = resultSubmitBannerUrl.trim(),
            rulesBannerImageUrl = rulesBannerUrl.trim(),
            supportWhatsappNumber = whatsappNumber.trim(),
            supportTelegramUrl = telegramUrl.trim(),
          )
          onSave(updated)
        },
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun AuditLogsTab(
  logs: List<com.example.domain.model.AuditLogEntity>,
  dateFormat: SimpleDateFormat,
) {
  if (logs.isEmpty()) {
    EmptyState(
      title = "No Audit Logs",
      description = "No administrative action records logged yet.",
      modifier = Modifier.padding(24.dp),
    )
  } else {
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(logs, key = { it.logId }) { log ->
        TournamentCard(modifier = Modifier.fillMaxWidth(), backgroundColor = NavyCard, borderColor = NavyCardBorder) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = log.action, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Gold400)
            Text(text = dateFormat.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall, color = Slate500)
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = log.details, style = MaterialTheme.typography.bodySmall, color = Slate300)
          Text(text = "Admin: ${log.adminUid} • Target: ${log.targetUid}", style = MaterialTheme.typography.labelSmall, color = Slate500)
        }
      }
    }
  }
}


