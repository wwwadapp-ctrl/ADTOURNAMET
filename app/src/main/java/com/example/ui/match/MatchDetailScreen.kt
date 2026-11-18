package com.example.ui.match

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.i18n.*
import com.example.domain.model.MatchStatus
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import com.example.ui.components.UnifiedEsportsTopBar
import com.example.ui.components.MatchStatusBadge
import com.example.ui.components.TournamentButton
import com.example.ui.components.TournamentButtonVariant
import com.example.ui.components.TournamentCard
import com.example.ui.theme.*
import androidx.compose.foundation.text.KeyboardOptions
import com.example.R
import com.example.ui.components.EsportsAvatar
import com.example.ui.profile.PfStatTile
import com.example.ui.wallet.DpBigButton
import com.example.ui.wallet.DpField
import com.example.ui.wallet.DpNotice
import com.example.ui.wallet.DpUploadBox
import com.example.ui.wallet.WlTone
import com.example.ui.wallet.wlBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
  viewModel: MatchDetailViewModel,
  currentUser: UserEntity? = null,
  wallet: WalletEntity? = null,
  unreadNotificationsCount: Int = 0,
  onWalletClick: () -> Unit = {},
  onNotificationClick: () -> Unit = {},
  onProfileClick: () -> Unit = {},
  onAdminClick: (() -> Unit)? = null,
  onNavigateBack: () -> Unit,
  onViewRoomCode: (String) -> Unit = {},
  onSubmitProof: (String) -> Unit = {},
) {
  val strings = LocalAppStrings.current
  val uiState by viewModel.uiState.collectAsState()
  val match = uiState.match
  val isLudo = match?.gameType?.equals("LUDO", ignoreCase = true) == true

  // Real room code from the protected node; falls back to the match field (never shows the hidden marker)
  val secretRoomCode by viewModel.roomCode.collectAsState()
  val displayRoomCode: String = if (secretRoomCode.isNotBlank()) {
    secretRoomCode
  } else {
    (match?.gameCode ?: "").let { if (it == "__HIDDEN__") "" else it }
  }

  var showProofDialog by remember { mutableStateOf(false) }
  var showJoinConfirmationDialog by remember { mutableStateOf(false) }
  var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
  var roomCodeInput by remember { mutableStateOf(displayRoomCode) }
  var matchNumberInput by remember { mutableStateOf(match?.matchNumber ?: "") }
  var proofNotesInput by remember { mutableStateOf("") }
  var proofError by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(displayRoomCode, match?.matchNumber) {
    if (match != null) {
      if (roomCodeInput.isBlank() && displayRoomCode.isNotBlank()) roomCodeInput = displayRoomCode
      if (matchNumberInput.isBlank() && match.matchNumber.isNotBlank()) matchNumberInput = match.matchNumber
    }
  }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
    onResult = { uri ->
      if (uri != null) {
        selectedImageUri = uri
        proofError = null
      }
    }
  )

  val authUid = com.example.core.firebase.FirebaseManager.getAuth()?.currentUser?.uid.orEmpty()
  val activeUid = viewModel.currentUserId.ifBlank { authUid }
  val currentResult = uiState.submittedResult
  val isResultApproved = currentResult?.status?.equals("APPROVED", ignoreCase = true) == true
  val isResultRejected = currentResult?.status?.equals("REJECTED", ignoreCase = true) == true
  val isResultLost = currentResult?.status?.equals("LOST", ignoreCase = true) == true
  val isCompleted = match?.status?.equals(MatchStatus.COMPLETED.name, ignoreCase = true) == true

  val registeredPlayer = uiState.players.find {
    it.effectiveUid == activeUid ||
    it.userId == activeUid ||
    it.uid == activeUid ||
    (!authUid.isBlank() && (it.effectiveUid == authUid || it.uid == authUid || it.userId == authUid))
  }
  val isUserRegistered = registeredPlayer != null || uiState.isJoined

  val isWinnerDeclared = match?.winnerUserId?.isNotBlank() == true
  val isDeclaredWinner = isWinnerDeclared && (
    match?.winnerUserId == activeUid ||
    match?.winnerUserId == viewModel.currentUserId ||
    match?.winnerUserId == "WINNER" ||
    (!authUid.isBlank() && match?.winnerUserId == authUid)
  )
  val isDeclaredLoser = isWinnerDeclared && !isDeclaredWinner
  val isPlayerLost = registeredPlayer?.status?.equals("LOST", ignoreCase = true) == true

  val isUserWinner = (isCompleted && isDeclaredWinner) || isResultApproved
  val isUserLoser = (isCompleted && !isUserWinner) || isResultLost || isResultRejected || isPlayerLost || isDeclaredLoser
  val isProofLocked = isCompleted || isResultApproved || isResultRejected || isResultLost || isUserLoser || (currentResult != null)

  Scaffold(
    topBar = {
      UnifiedEsportsTopBar(
        currentUser = currentUser,
        wallet = wallet,
        unreadNotificationsCount = unreadNotificationsCount,
        onWalletClick = onWalletClick,
        onNotificationClick = onNotificationClick,
        onProfileClick = onProfileClick,
        onAdminClick = onAdminClick,
      )
    },
    containerColor = DeepNavyBg,
  ) { innerPadding ->
    if (match == null) {
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Gold500)
      }
      return@Scaffold
    }

    val isUpcoming = match.status.equals(MatchStatus.UPCOMING.name, ignoreCase = true)
    val isFull = match.joinedPlayersCount >= match.maxPlayers || match.status.equals(MatchStatus.FULL.name, ignoreCase = true)
    val isRunning = match.status.equals(MatchStatus.RUNNING.name, ignoreCase = true)

    val formatSlot = { slotString: String ->
      when {
        slotString.contains("1") || slotString.equals(com.example.domain.model.PlayerSlot.PLAYER_1.name, ignoreCase = true) -> strings.matchDetailSlot(strings.number(1))
        slotString.contains("2") || slotString.equals(com.example.domain.model.PlayerSlot.PLAYER_2.name, ignoreCase = true) -> strings.matchDetailSlot(strings.number(2))
        slotString.isNotBlank() -> slotString.replace("PLAYER_", "").replace("_", " ").let { strings.matchDetailSlot(strings.digits(it)) }
        else -> strings.matchDetailSlot(strings.number(1))
      }
    }
    val registeredSlotLabel = registeredPlayer?.let { p ->
      formatSlot(p.slot)
    } ?: strings.matchDetailSlot(strings.number(1))

    val displayPlayersCount = maxOf(match.joinedPlayersCount, uiState.players.size)

    val titleRegex = remember { Regex("(?i)^(ludo|carrom)\\s*1v1\\s*#(\\S+)$") }
    val displayGameName = if (isLudo) strings.filterLudo else strings.filterCarrom
    val finalTitle = remember(match.title, match.matchNumber, strings) {
      val mTitle = match.title
      val mNum = match.matchNumber.removePrefix("#").trim()
      mTitle.takeIf { it.isNotBlank() }?.let { title ->
        val matchResult = titleRegex.find(title)
        if (matchResult != null) {
          val game = matchResult.groupValues[1]
          val id = matchResult.groupValues[2]
          val localizedGame = if (game.equals("ludo", ignoreCase = true)) strings.filterLudo else strings.filterCarrom
          strings.historyMatchTitle(localizedGame, strings.digits(id))
        } else {
          title
        }
      } ?: run {
        if (mNum.isNotBlank()) {
          strings.historyMatchTitle(displayGameName, strings.digits(mNum))
        } else {
          "${displayGameName} ${strings.label1v1Battle}"
        }
      }
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .wlBackground()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    ) {
      MdHeroCard(
        isLudo = isLudo,
        chipText = if (isLudo) strings.matchDetailLudoHeader else strings.matchDetailCarromHeader,
        title = finalTitle,
        badge = {
          MatchStatusBadge(
            status = match.status,
            joinedPlayersCount = displayPlayersCount,
            maxPlayers = match.maxPlayers,
            isJoined = isUserRegistered,
            isWinner = isUserWinner,
            isLoser = isUserLoser,
            premium = true,
          )
        },
        statusChip = if (!isUserRegistered) null else {
          {
            if (isCompleted || isResultApproved || isResultLost || isResultRejected || isUserWinner || isUserLoser) {
              if (isUserWinner) {
                MdStatusChip(strings.matchDetailYouWonBanner(strings.taka(match.displayPrizePool)), 1, "winner_chip")
              } else {
                MdStatusChip(if (isResultRejected) strings.matchDetailProofRejectedLoser else strings.matchDetailMatchFinishedLoser, 2, "loser_chip")
              }
            } else {
              MdStatusChip(strings.matchDetailRegisteredSlot(registeredSlotLabel), 0, "registered_chip")
            }
          }
        },
        tiles = {
          PfStatTile(R.drawable.wl_moneybag, strings.entryFeeLabel, strings.taka(match.displayEntryFee), "", Color(0xFF4ADE80), WlTone.CREAM, Modifier.weight(1f))
          PfStatTile(R.drawable.wl_gamepad, strings.playersLabel, "${strings.number(displayPlayersCount)} / ${strings.number(match.maxPlayers)}", "", Color(0xFF67E8F9), WlTone.CREAM, Modifier.weight(1f))
          PfStatTile(R.drawable.wl_trophy, strings.matchDetailWinnerPrizeLabel, strings.taka(match.displayPrizePool), "", Color(0xFFFFD45A), WlTone.GOLD, Modifier.weight(1f))
        },
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(16.dp))

      if (uiState.successMessage != null) {
        MdInfoCard(R.drawable.wl_trophy, Color(0xFF4ADE80), null, mdShownMessage(uiState.successMessage ?: "", strings), Modifier.padding(bottom = 12.dp))
      }

      if (isUpcoming) {
        MdInfoCard(R.drawable.wl_hourglass, Color(0xFFF59E0B), null, strings.matchDetailUpcomingNotice, Modifier.padding(bottom = 12.dp), tiltPicture = true)
      }

      if (uiState.errorMessage != null) {
        DpNotice(mdShownMessage(uiState.errorMessage ?: "", strings), Icons.Default.Error, true, Modifier.padding(bottom = 12.dp))
      }

      // Game Room Code section with strict privacy
      MdRoomCodeCard(
        title = strings.matchDetailRoomCodeTitle,
        unlockedText = if (isUserRegistered) strings.matchDetailUnlockedBadge else null,
        modifier = Modifier.fillMaxWidth(),
      ) {
        if (isUserRegistered || uiState.isAdmin) {
          if (match.gameCode.isNotBlank()) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
              MdCodeDigits(strings.roomCodeLabel, displayRoomCode)
              Text(
                text = strings.matchDetailRoomCodeInstructions,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFF3D8),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 9.dp),
              )
              Spacer(modifier = Modifier.height(12.dp))
              val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
              val context = androidx.compose.ui.platform.LocalContext.current
              MdCopyButton(
                text = strings.copy,
                onClick = {
                  clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(displayRoomCode))
                  Toast.makeText(context, strings.roomCodeCopiedNotice, Toast.LENGTH_SHORT).show()

                  // Authoritative Anti-Cheat Audit Log (Fire-and-forget)
                  viewModel.logRoomCodeCopy(
                    matchId = match.matchId,
                    matchNumber = match.matchNumber.toString(),
                    roomCode = displayRoomCode,
                    userUid = activeUid,
                    userName = currentUser?.effectiveName ?: "",
                    userPhone = currentUser?.effectiveMobile ?: ""
                  )
                },
                tag = "detail_copy_room_code_btn",
              )
            }
          } else {
            MdNote(strings.matchDetailWaitingRoomCode, "⏳")
          }
        } else {
          MdNote(strings.matchDetailConfidentialNotice, "🔒")
        }
      }
      Spacer(modifier = Modifier.height(16.dp))

      // Result Submission Status or Action
      val isRejectedResult = currentResult?.status?.equals("REJECTED", ignoreCase = true) == true

      if (currentResult != null) {
        val result = currentResult
        val isApprovedRes = result.status == "APPROVED"
        val isRejectedRes = result.status == "REJECTED"
        val resultDesc = listOfNotNull(
          if (!result.status.equals("APPROVED", ignoreCase = true) && !result.status.equals("REJECTED", ignoreCase = true)) strings.matchDetailReviewWaitingText else null,
          if (result.reviewNotes.isNotBlank()) "${strings.matchDetailNotesLabel} ${result.reviewNotes}" else null,
        ).joinToString("\n")
        MdInfoCard(
          picture = if (isApprovedRes) R.drawable.wl_trophy else if (isRejectedRes) R.drawable.mg_dice else R.drawable.wl_hourglass,
          ring = if (isApprovedRes) Color(0xFF4ADE80) else if (isRejectedRes) Color(0xFFFF6B81) else Color(0xFFFFD45A),
          title = when (result.status) {
            "APPROVED" -> strings.matchDetailApprovedTitle
            "REJECTED" -> strings.matchDetailRejectedTitle
            else -> strings.matchDetailScreenshotSubmittedTitle
          },
          description = resultDesc,
          badge = when (result.status) {
            "APPROVED" -> strings.matchDetailApprovedBadge
            "REJECTED" -> strings.matchDetailRejectedBadge
            else -> strings.matchDetailUnderReviewBadge
          },
          tiltPicture = !isApprovedRes && !isRejectedRes,
        )
        Spacer(modifier = Modifier.height(16.dp))
      }

      if (isUserRegistered) {
        val isMatchCompleted = match.status.equals(MatchStatus.COMPLETED.name, ignoreCase = true)
        val isLoser = isUserLoser

        val gameCodeProvidedTime = if (match.updatedAt > 0L) match.updatedAt else match.createdAt
        val currentTime = System.currentTimeMillis()
        val twoMinutesMillis = 2 * 60 * 1000L
        val hasRoomCode = match.gameCode.isNotBlank()
        val isTwoMinutesPassed = hasRoomCode && (currentTime - gameCodeProvidedTime) >= twoMinutesMillis

        val isLocked = isCompleted || isMatchCompleted || isResultApproved || isResultRejected || isResultLost || isLoser || currentResult != null
        val canSubmitProof = !isLocked && hasRoomCode && isTwoMinutesPassed

        if (uiState.isSubmittingResult) {
          MdInfoCard(R.drawable.wl_hourglass, Color(0xFF67E8F9), strings.matchDetailSubmittingScreenshotTitle, strings.matchDetailSubmittingScreenshotSubtitle, tiltPicture = true)
          Spacer(modifier = Modifier.height(16.dp))
        } else if (isResultRejected) {
          MdInfoCard(R.drawable.mg_dice, Color(0xFFFF6B81), strings.matchDetailProofRejectedTitle, strings.matchDetailProofRejectedDesc)
          Spacer(modifier = Modifier.height(16.dp))
        } else if (isCompleted || isMatchCompleted) {
          MdInfoCard(
            if (isUserWinner) R.drawable.wl_trophy else R.drawable.mg_dice,
            if (isUserWinner) Color(0xFFFFD45A) else Color(0xFF8088B0),
            strings.matchDetailMatchFinishedTitle,
            if (isUserWinner) strings.matchDetailMatchFinishedWinnerDesc else strings.matchDetailMatchFinishedLoserDesc,
          )
          Spacer(modifier = Modifier.height(16.dp))
        } else if (isResultApproved) {
          MdInfoCard(R.drawable.wl_trophy, Color(0xFF4ADE80), strings.matchDetailResultApprovedTitle, strings.matchDetailResultApprovedDesc)
          Spacer(modifier = Modifier.height(16.dp))
        } else if (isResultLost || isLoser) {
          MdInfoCard(R.drawable.mg_dice, Color(0xFF8088B0), strings.matchDetailDefeatedTitle, strings.matchDetailDefeatedDesc)
          Spacer(modifier = Modifier.height(16.dp))
        } else if (canSubmitProof) {
          DpBigButton(
            text = strings.matchDetailSubmitProofCta,
            onClick = { showProofDialog = true },
            tag = "detail_submit_proof_btn",
          )
          Spacer(modifier = Modifier.height(16.dp))
        } else if (!hasRoomCode) {
          MdInfoCard(R.drawable.wl_hourglass, Color(0xFFF59E0B), null, strings.matchDetailWaitingRoomCodeForProof, tiltPicture = true)
          Spacer(modifier = Modifier.height(16.dp))
        } else if (!isTwoMinutesPassed) {
          MdInfoCard(R.drawable.wl_hourglass, Color(0xFFF59E0B), null, strings.matchDetailTwoMinutesWaitingText, tiltPicture = true)
          Spacer(modifier = Modifier.height(16.dp))
        }
      }

      MdPlayersCard(
        title = strings.matchDetailRegisteredPlayersTitle(
          strings.number(displayPlayersCount),
          strings.number(match.maxPlayers)
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        if (uiState.players.isEmpty()) {
          Text(
            text = strings.matchDetailNoPlayersJoined,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFE7A0),
          )
        } else {
          uiState.players.forEach { p ->
            val isCurrent = p.effectiveUid == viewModel.currentUserId ||
                p.userId == viewModel.currentUserId ||
                p.uid == viewModel.currentUserId ||
                (!authUid.isNullOrBlank() && (p.effectiveUid == authUid || p.uid == authUid || p.userId == authUid))
            MdPlayerRow(
              name = p.username,
              slot = formatSlot(p.slot),
              isCurrent = isCurrent,
              youText = strings.matchDetailYouBadge,
              avatar = {
                EsportsAvatar(
                  photoUrl = if (isCurrent) currentUser?.effectivePhoto else null,
                  name = p.username,
                  size = 38.dp,
                  borderWidth = 0.dp,
                )
              },
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      if (isUserRegistered || uiState.isJoined) {
        val buttonText = when {
          isCompleted && isUserWinner -> "${strings.badgeVictory} • ${strings.taka(match.displayPrizePool)}"
          isCompleted -> strings.badgeDefeat
          isResultApproved -> "${strings.badgeVictory} • ${strings.taka(match.displayPrizePool)}"
          isResultRejected || isResultLost -> strings.badgeDefeat
          else -> strings.labelJoined
        }
        val buttonKind = when {
          (isCompleted && isUserWinner) || isResultApproved -> 1
          isCompleted || isResultRejected || isResultLost -> 2
          else -> 0
        }
        MdStateButton(text = buttonText, kind = buttonKind, tag = "detail_joined_button")
      } else if (isUpcoming) {
        MdStateButton(text = strings.matchDetailUpcomingButton, kind = 2, tag = "detail_upcoming_button")
      } else if (!isFull && !isRunning && !isCompleted) {
        DpBigButton(
          text = strings.matchDetailJoinMatchButton(strings.taka(match.displayEntryFee)),
          onClick = { showJoinConfirmationDialog = true },
          tag = "detail_join_button",
          isLoading = uiState.isJoining,
        )
      }
    }
  }

  if (showJoinConfirmationDialog && match != null) {
    MdJoinWindow(
      title = strings.matchDetailJoinDialogTitle,
      nameNotice = if (isLudo) strings.matchDetailLudoNameNotice else strings.matchDetailCarromNameNotice,
      feeNotice = strings.matchDetailJoinFeeNotice(strings.taka(match.displayEntryFee)),
      cancelText = strings.cancel,
      confirmText = strings.matchDetailAgreeAndJoin,
      onConfirm = {
        showJoinConfirmationDialog = false
        viewModel.joinMatch()
      },
      onDismiss = { showJoinConfirmationDialog = false },
    )
  }

  if (showProofDialog && !isProofLocked) {
    MdProofWindow(
      title = strings.matchDetailSubmitProofTitle,
      cancelText = strings.cancel,
      cancelEnabled = !uiState.isSubmittingResult,
      onDismiss = { showProofDialog = false },
      submit = {
        DpBigButton(
          text = strings.matchDetailSubmitProofButton,
          onClick = {
            if (!uiState.isSubmittingResult && !isProofLocked) {
              when {
                selectedImageUri == null -> {
                  proofError = strings.matchDetailScreenshotRequired
                }
                roomCodeInput.trim().isEmpty() -> {
                  proofError = strings.matchDetailRoomCodeRequired
                }
                matchNumberInput.trim().isEmpty() -> {
                  proofError = strings.matchDetailMatchNumberRequired
                }
                else -> {
                  viewModel.submitVictoryProof(selectedImageUri!!, roomCodeInput.trim(), matchNumberInput.trim(), proofNotesInput.trim())
                  showProofDialog = false
                }
              }
            }
          },
          tag = "dialog_confirm_proof_btn",
          isLoading = uiState.isSubmittingResult,
        )
      },
    ) {
      // A) SCREENSHOT
      DpUploadBox(
        title = if (selectedImageUri == null) strings.matchDetailSelectScreenshot else strings.matchDetailChangeScreenshot,
        subtitle = strings.matchDetailScreenshotRequired,
        icon = Icons.Default.AddPhotoAlternate,
        onClick = {
          if (!uiState.isSubmittingResult) {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
          }
        },
      )
      if (selectedImageUri != null) {
        AsyncImage(
          model = selectedImageUri,
          contentDescription = "Selected screenshot preview",
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Slate900),
          contentScale = ContentScale.Crop,
        )
      }
      // B) ROOM CODE
      DpField(
        label = strings.matchDetailRoomCodeRequired,
        value = roomCodeInput,
        onValueChange = {
          roomCodeInput = it
          proofError = null
        },
        placeholder = strings.matchDetailRoomCodePlaceholder,
        icon = Icons.Default.VpnKey,
        keyboardOptions = KeyboardOptions.Default,
        tag = "proof_room_code_input",
      )
      // C) MATCH NUMBER
      DpField(
        label = strings.matchDetailMatchNumberRequired,
        value = matchNumberInput,
        onValueChange = {
          matchNumberInput = it
          proofError = null
        },
        placeholder = strings.matchDetailMatchNumberPlaceholder,
        icon = Icons.Default.Tag,
        keyboardOptions = KeyboardOptions.Default,
        tag = "proof_match_number_input",
      )
      DpField(
        label = strings.matchDetailRemarksLabel,
        value = proofNotesInput,
        onValueChange = { proofNotesInput = it },
        placeholder = strings.matchDetailRemarksPlaceholder,
        icon = Icons.Default.EditNote,
        keyboardOptions = KeyboardOptions.Default,
        tag = "proof_notes_input",
      )
      if (proofError != null) {
        DpNotice(proofError!!, Icons.Default.Error, true)
      }
    }
  }
}

/**
 * Display only: turns the known English join / proof messages into clear Bengali sentences
 * when the app is in Bengali. Any other message is shown as it is.
 */
private fun mdShownMessage(msg: String, strings: AppStrings): String {
  if (strings !== BengaliStrings) return msg
  val m = msg.trim()
  return when {
    m.contains("registration deadline has passed", ignoreCase = true) ->
      "দুঃখিত, এই ম্যাচে জয়েন করার সময় শেষ হয়ে গেছে। অনুগ্রহ করে অন্য একটি ম্যাচ বেছে নিন।"
    m.contains("already joined", ignoreCase = true) || m.contains("entry transaction already exists", ignoreCase = true) ->
      "আপনি ইতিমধ্যেই এই ম্যাচে যুক্ত আছেন।"
    m.contains("Match is full", ignoreCase = true) ->
      "দুঃখিত, এই ম্যাচের সব আসন পূর্ণ হয়ে গেছে। অনুগ্রহ করে অন্য একটি ম্যাচ বেছে নিন।"
    m.contains("not available to join", ignoreCase = true) ->
      "এই ম্যাচে এখন জয়েন করা যাচ্ছে না। অনুগ্রহ করে অন্য একটি ম্যাচ বেছে নিন।"
    m.contains("account is blocked", ignoreCase = true) ->
      "আপনার অ্যাকাউন্টটি বন্ধ রয়েছে, তাই ম্যাচে জয়েন করা যাবে না। সাহায্যের জন্য সাপোর্টে যোগাযোগ করুন।"
    m.contains("Authentication required", ignoreCase = true) || m.contains("must be authenticated", ignoreCase = true) ->
      "অনুগ্রহ করে আবার লগইন করে চেষ্টা করুন।"
    m.contains("Invalid entry fee", ignoreCase = true) ->
      "এই ম্যাচের এন্ট্রি ফি-তে সমস্যা আছে। অনুগ্রহ করে অন্য একটি ম্যাচ বেছে নিন।"
    m.contains("Wallet not found", ignoreCase = true) ->
      "আপনার ওয়ালেট খুঁজে পাওয়া যায়নি। কিছুক্ষণ পর আবার চেষ্টা করুন।"
    m.startsWith("Match ", ignoreCase = true) && m.contains("not found", ignoreCase = true) ->
      "ম্যাচটি খুঁজে পাওয়া যায়নি। কিছুক্ষণ পর আবার চেষ্টা করুন।"
    m.contains("Successfully joined", ignoreCase = true) ->
      "অভিনন্দন! আপনি সফলভাবে ম্যাচে যুক্ত হয়েছেন। 🎉"
    m.contains("proof submitted successfully", ignoreCase = true) ->
      "আপনার জয়ের প্রমাণ সফলভাবে জমা হয়েছে। অ্যাডমিন যাচাই করে দেখবেন।"
    m.contains("Failed to upload screenshot", ignoreCase = true) ->
      "স্ক্রিনশট আপলোড করা যায়নি। ইন্টারনেট সংযোগ দেখে আবার চেষ্টা করুন।"
    else -> msg
  }
}
