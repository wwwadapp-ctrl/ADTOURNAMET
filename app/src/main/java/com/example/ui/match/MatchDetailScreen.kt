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
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    ) {
      TournamentCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NavyCard,
        borderColor = if (isCompleted && isUserWinner) Gold400 else if (isCompleted && isUserLoser) Rose600.copy(alpha = 0.6f) else Gold500.copy(alpha = 0.4f),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (isLudo) strings.matchDetailLudoHeader else strings.matchDetailCarromHeader,
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
            ),
            color = if (isLudo) Indigo400 else Cyan400,
          )
          MatchStatusBadge(
            status = match.status,
            joinedPlayersCount = displayPlayersCount,
            maxPlayers = match.maxPlayers,
            isJoined = isUserRegistered,
            isWinner = isUserWinner,
            isLoser = isUserLoser,
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = finalTitle,
          style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
          ),
          color = Color.White,
        )

        if (isUserRegistered) {
          Spacer(modifier = Modifier.height(8.dp))
          if (isCompleted || isResultApproved || isResultLost || isResultRejected || isUserWinner || isUserLoser) {
            if (isUserWinner) {
              Surface(
                color = Gold500.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Gold400.copy(alpha = 0.7f)),
                modifier = Modifier.testTag("winner_chip"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = strings.badgeVictory,
                    tint = Gold400,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = strings.matchDetailYouWonBanner(strings.taka(match.displayPrizePool)),
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Bold,
                      fontSize = 13.sp,
                    ),
                    color = Gold400,
                  )
                }
              }
            } else {
              Surface(
                color = Rose900.copy(alpha = 0.45f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Rose500.copy(alpha = 0.6f)),
                modifier = Modifier.testTag("loser_chip"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = strings.badgeDefeat,
                    tint = Rose400,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = if (isResultRejected) strings.matchDetailProofRejectedLoser else strings.matchDetailMatchFinishedLoser,
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                    ),
                    color = Rose400,
                  )
                }
              }
            }
          } else {
            Surface(
              color = Emerald900.copy(alpha = 0.55f),
              shape = RoundedCornerShape(20.dp),
              border = BorderStroke(1.dp, Emerald500),
              modifier = Modifier.testTag("registered_chip"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = Emerald400,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = strings.matchDetailRegisteredSlot(registeredSlotLabel),
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                  ),
                  color = Emerald300,
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(Slate900, RoundedCornerShape(10.dp))
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = strings.entryFeeLabel,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
              color = Slate400,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = strings.taka(match.displayEntryFee),
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.sp,
              ),
              color = Color.White,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(end = 2.dp),
            )
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = strings.playersLabel,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
              color = Slate400,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "${strings.number(displayPlayersCount)} / ${strings.number(match.maxPlayers)}",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.sp,
              ),
              color = Cyan400,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(end = 2.dp),
            )
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = strings.matchDetailWinnerPrizeLabel,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
              color = Slate400,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = strings.taka(match.displayPrizePool),
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (strings === BengaliStrings) FontWeight.Bold else FontWeight.Black,
                fontSize = 15.sp,
                letterSpacing = 0.sp,
              ),
              color = Gold400,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(end = 2.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (uiState.successMessage != null) {
        Surface(
          color = Emerald900.copy(alpha = 0.5f),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, Emerald500),
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
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = Emerald400,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = uiState.successMessage ?: "",
              color = Emerald400,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            )
          }
        }
      }

      if (isUpcoming) {
        Surface(
          color = Amber900.copy(alpha = 0.35f),
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, Amber500.copy(alpha = 0.5f)),
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
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = Amber400,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = strings.matchDetailUpcomingNotice,
              color = Amber400,
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
              ),
            )
          }
        }
      }

      if (uiState.errorMessage != null) {
        Surface(
          color = Rose900.copy(alpha = 0.4f),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        ) {
          Text(
            text = uiState.errorMessage ?: "",
            color = Rose400,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      // Game Room Code section with strict privacy
      TournamentCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NavyCard,
        borderColor = if (isUserRegistered || uiState.isAdmin) Indigo600 else NavyCardBorder,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = strings.matchDetailRoomCodeTitle,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
            ),
            color = Color.White,
          )
          if (isUserRegistered) {
            Surface(
              color = Emerald900.copy(alpha = 0.4f),
              shape = RoundedCornerShape(6.dp),
              border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.7f)),
            ) {
              Text(
                text = strings.matchDetailUnlockedBadge,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp,
                ),
                color = Emerald400,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (isUserRegistered || uiState.isAdmin) {
          if (match.gameCode.isNotBlank()) {
            Surface(
              color = Indigo900.copy(alpha = 0.5f),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, Indigo600),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = strings.roomCodeLabel,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                  color = Gold400
                )
                Text(
                  text = displayRoomCode,
                  style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                  ),
                  color = Color.White,
                )
                Text(
                  text = strings.matchDetailRoomCodeInstructions,
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                  color = Slate400,
                  modifier = Modifier.padding(top = 6.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))
                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                val context = androidx.compose.ui.platform.LocalContext.current
                TournamentButton(
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
                  variant = TournamentButtonVariant.SECONDARY,
                  modifier = Modifier.widthIn(min = 120.dp),
                )
              }
            }
          } else {
            Text(
              text = strings.matchDetailWaitingRoomCode,
              style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
              color = Slate300,
            )
          }
        } else {
          Surface(
            color = Slate850,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Default.Lock, contentDescription = null, tint = Amber400)
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = strings.matchDetailConfidentialNotice,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = Slate400,
              )
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))

      // Result Submission Status or Action
      val isRejectedResult = currentResult?.status?.equals("REJECTED", ignoreCase = true) == true

      if (currentResult != null) {
        val result = currentResult
        TournamentCard(
          modifier = Modifier.fillMaxWidth(),
          backgroundColor = NavyCard,
          borderColor = when (result.status) {
            "APPROVED" -> Emerald500
            "REJECTED" -> Rose500
            else -> Gold400
          },
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = when (result.status) {
                "APPROVED" -> strings.matchDetailApprovedTitle
                "REJECTED" -> strings.matchDetailRejectedTitle
                else -> strings.matchDetailScreenshotSubmittedTitle
              },
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
              ),
              color = when (result.status) {
                "APPROVED" -> Emerald400
                "REJECTED" -> Rose400
                else -> Color.White
              },
            )
            Surface(
              color = when (result.status) {
                "APPROVED" -> Emerald900.copy(alpha = 0.5f)
                "REJECTED" -> Rose900.copy(alpha = 0.5f)
                else -> Amber900.copy(alpha = 0.5f)
              },
              shape = RoundedCornerShape(6.dp),
              border = BorderStroke(
                1.dp,
                when (result.status) {
                  "APPROVED" -> Emerald500
                  "REJECTED" -> Rose500
                  else -> Gold400
                },
              ),
            ) {
              Text(
                text = when (result.status) {
                  "APPROVED" -> strings.matchDetailApprovedBadge
                  "REJECTED" -> strings.matchDetailRejectedBadge
                  else -> strings.matchDetailUnderReviewBadge
                },
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.5.sp,
                ),
                color = when (result.status) {
                  "APPROVED" -> Emerald400
                  "REJECTED" -> Rose400
                  else -> Gold400
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          if (!result.status.equals("APPROVED", ignoreCase = true) && !result.status.equals("REJECTED", ignoreCase = true)) {
            Text(
              text = strings.matchDetailReviewWaitingText,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
              color = Slate300,
            )
          }
          if (result.reviewNotes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "${strings.matchDetailNotesLabel} ${result.reviewNotes}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
              color = if (result.status == "REJECTED") Rose400 else Slate400,
            )
          }
        }
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
          TournamentCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NavyCard,
            borderColor = Indigo600,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.padding(4.dp),
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Gold400,
                strokeWidth = 2.5.dp,
              )
              Column {
                Text(
                  text = strings.matchDetailSubmittingScreenshotTitle,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                  color = Color.White,
                )
                Text(
                  text = strings.matchDetailSubmittingScreenshotSubtitle,
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                  color = Slate400,
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        } else if (isResultRejected) {
          TournamentCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Rose900.copy(alpha = 0.2f),
            borderColor = Rose500.copy(alpha = 0.5f),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(Icons.Default.Cancel, contentDescription = null, tint = Rose400, modifier = Modifier.size(22.dp))
              Column {
                Text(
                  text = strings.matchDetailProofRejectedTitle,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                  color = Rose400,
                )
                Text(
                  text = strings.matchDetailProofRejectedDesc,
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                  color = Slate300,
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        } else if (isCompleted || isMatchCompleted) {
          TournamentCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NavyCard,
            borderColor = if (isUserWinner) Gold400.copy(alpha = 0.5f) else CardBorder,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = if (isUserWinner) Icons.Default.EmojiEvents else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isUserWinner) Gold400 else Slate400,
                modifier = Modifier.size(22.dp),
              )
              Column {
                Text(
                  text = strings.matchDetailMatchFinishedTitle,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                  color = Color.White,
                )
                Text(
                  text = if (isUserWinner) strings.matchDetailMatchFinishedWinnerDesc else strings.matchDetailMatchFinishedLoserDesc,
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                  color = Slate300,
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        } else if (isResultApproved) {
          TournamentCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Emerald900.copy(alpha = 0.2f),
            borderColor = Emerald500.copy(alpha = 0.5f),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald400, modifier = Modifier.size(22.dp))
              Column {
                Text(
                  text = strings.matchDetailResultApprovedTitle,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                  color = Emerald300,
                )
                Text(
                  text = strings.matchDetailResultApprovedDesc,
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                  color = Slate300,
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        } else if (isResultLost || isLoser) {
          TournamentCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NavyCard,
            borderColor = CardBorder,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(Icons.Default.Info, contentDescription = null, tint = Slate400, modifier = Modifier.size(22.dp))
              Column {
                Text(
                  text = strings.matchDetailDefeatedTitle,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                  color = Slate300,
                )
                Text(
                  text = strings.matchDetailDefeatedDesc,
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                  color = Slate400,
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        } else if (canSubmitProof) {
          TournamentButton(
            text = strings.matchDetailSubmitProofCta,
            onClick = { showProofDialog = true },
            modifier = Modifier.fillMaxWidth(),
            testTag = "detail_submit_proof_btn",
          )
          Spacer(modifier = Modifier.height(16.dp))
        } else if (!hasRoomCode) {
          TournamentCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NavyCard,
            borderColor = CardBorder,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Default.Schedule, contentDescription = null, tint = Amber400, modifier = Modifier.size(20.dp))
              Text(
                text = strings.matchDetailWaitingRoomCodeForProof,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = Slate300,
              )
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        } else if (!isTwoMinutesPassed) {
          TournamentCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NavyCard,
            borderColor = CardBorder,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Default.Schedule, contentDescription = null, tint = Amber400, modifier = Modifier.size(20.dp))
              Text(
                text = strings.matchDetailTwoMinutesWaitingText,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = Slate300,
              )
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        }
      }

      TournamentCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NavyCard,
        borderColor = NavyCardBorder,
      ) {
        Text(
          text = strings.matchDetailRegisteredPlayersTitle(
            strings.number(displayPlayersCount),
            strings.number(match.maxPlayers)
          ),
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
          ),
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (uiState.players.isEmpty()) {
          Text(
            text = strings.matchDetailNoPlayersJoined,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = Slate400,
          )
        } else {
          uiState.players.forEach { p ->
            val isCurrent = p.effectiveUid == viewModel.currentUserId ||
                p.userId == viewModel.currentUserId ||
                p.uid == viewModel.currentUserId ||
                (!authUid.isNullOrBlank() && (p.effectiveUid == authUid || p.uid == authUid || p.userId == authUid))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(
                  if (isCurrent) Emerald900.copy(alpha = 0.25f) else Color.Transparent,
                  shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  Icons.Default.AccountCircle,
                  contentDescription = null,
                  tint = if (isCurrent) Emerald400 else Gold400,
                  modifier = Modifier.size(24.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(
                    text = p.username,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                      fontSize = 14.sp,
                    ),
                    color = Color.White
                  )
                  if (isCurrent) {
                    Surface(
                      color = Emerald500.copy(alpha = 0.2f),
                      shape = RoundedCornerShape(4.dp),
                    ) {
                      Text(
                        text = strings.matchDetailYouBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontWeight = FontWeight.Bold,
                          fontSize = 10.sp,
                        ),
                        color = Emerald400,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                      )
                    }
                  }
                }
              }
              Surface(
                color = if (isCurrent) Emerald900.copy(alpha = 0.5f) else Slate850,
                shape = RoundedCornerShape(6.dp),
                border = if (isCurrent) BorderStroke(1.dp, Emerald500.copy(alpha = 0.5f)) else null,
              ) {
                Text(
                  text = formatSlot(p.slot),
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                  ),
                  color = if (isCurrent) Emerald300 else Cyan400,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
              }
            }
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
        val buttonVariant = when {
          (isCompleted && isUserWinner) || isResultApproved -> TournamentButtonVariant.PRIMARY
          isCompleted || isResultRejected || isResultLost -> TournamentButtonVariant.SECONDARY
          else -> TournamentButtonVariant.JOINED
        }
        TournamentButton(
          text = buttonText,
          onClick = { /* non-joinable */ },
          enabled = false,
          variant = buttonVariant,
          modifier = Modifier.fillMaxWidth(),
          testTag = "detail_joined_button",
        )
      } else if (isUpcoming) {
        TournamentButton(
          text = strings.matchDetailUpcomingButton,
          onClick = { /* non-joinable */ },
          enabled = false,
          variant = TournamentButtonVariant.SECONDARY,
          modifier = Modifier.fillMaxWidth(),
          testTag = "detail_upcoming_button",
        )
      } else if (!isFull && !isRunning && !isCompleted) {
        TournamentButton(
          text = strings.matchDetailJoinMatchButton(strings.taka(match.displayEntryFee)),
          onClick = { showJoinConfirmationDialog = true },
          isLoading = uiState.isJoining,
          modifier = Modifier.fillMaxWidth(),
          testTag = "detail_join_button",
        )
      }
    }
  }

  if (showJoinConfirmationDialog && match != null) {
    AlertDialog(
      onDismissRequest = { showJoinConfirmationDialog = false },
      containerColor = NavyCard,
      title = {
        Text(
          text = strings.matchDetailJoinDialogTitle,
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = Color.White
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          val gameNotice = if (isLudo) strings.matchDetailLudoNameNotice else strings.matchDetailCarromNameNotice
          val feeNotice = strings.matchDetailJoinFeeNotice(strings.taka(match.displayEntryFee))

          Text(text = gameNotice, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp), color = Slate300)
          Text(text = feeNotice, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = Gold400)
        }
      },
      confirmButton = {
        TournamentButton(
          text = strings.matchDetailAgreeAndJoin,
          onClick = {
            showJoinConfirmationDialog = false
            viewModel.joinMatch()
          },
          variant = TournamentButtonVariant.PRIMARY
        )
      },
      dismissButton = {
        TextButton(onClick = { showJoinConfirmationDialog = false }) {
          Text(text = strings.cancel, color = Slate400)
        }
      }
    )
  }

  if (showProofDialog && !isProofLocked) {
    AlertDialog(
      onDismissRequest = {
        if (!uiState.isSubmittingResult) {
          showProofDialog = false
        }
      },
      containerColor = NavyCard,
      title = {
        Text(
          text = strings.matchDetailSubmitProofTitle,
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
        )
      },
      text = {
        Column(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
          // A) SCREENSHOT
          Text(
            text = strings.matchDetailScreenshotRequired,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Gold400,
          )

          TournamentButton(
            text = if (selectedImageUri == null) {
              strings.matchDetailSelectScreenshot
            } else {
              strings.matchDetailChangeScreenshot
            },
            onClick = {
              photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            enabled = !uiState.isSubmittingResult,
            variant = TournamentButtonVariant.SECONDARY,
            modifier = Modifier.fillMaxWidth(),
          )

          if (selectedImageUri != null) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Slate900, RoundedCornerShape(8.dp))
                .padding(4.dp),
              contentAlignment = Alignment.Center,
            ) {
              AsyncImage(
                model = selectedImageUri,
                contentDescription = "Selected screenshot preview",
                modifier = Modifier
                  .fillMaxSize()
                  .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          // B) ROOM CODE
          Text(
            text = strings.matchDetailRoomCodeRequired,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Gold400,
          )
          OutlinedTextField(
            value = roomCodeInput,
            onValueChange = {
              roomCodeInput = it
              proofError = null
            },
            enabled = !uiState.isSubmittingResult,
            label = { Text(strings.roomCodeLabel) },
            placeholder = { Text(strings.matchDetailRoomCodePlaceholder) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = Gold400,
              unfocusedBorderColor = NavyCardBorder,
              focusedLabelColor = Gold400,
              unfocusedLabelColor = Slate400,
            ),
          )

          Spacer(modifier = Modifier.height(4.dp))

          // C) MATCH NUMBER
          Text(
            text = strings.matchDetailMatchNumberRequired,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Gold400,
          )
          OutlinedTextField(
            value = matchNumberInput,
            onValueChange = {
              matchNumberInput = it
              proofError = null
            },
            enabled = !uiState.isSubmittingResult,
            label = { Text(strings.matchDetailMatchNumberLabel) },
            placeholder = { Text(strings.matchDetailMatchNumberPlaceholder) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = Gold400,
              unfocusedBorderColor = NavyCardBorder,
              focusedLabelColor = Gold400,
              unfocusedLabelColor = Slate400,
            ),
          )

          Spacer(modifier = Modifier.height(4.dp))

          OutlinedTextField(
            value = proofNotesInput,
            onValueChange = { proofNotesInput = it },
            enabled = !uiState.isSubmittingResult,
            label = { Text(strings.matchDetailRemarksLabel) },
            placeholder = { Text(strings.matchDetailRemarksPlaceholder) },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White,
              focusedBorderColor = Gold400,
              unfocusedBorderColor = NavyCardBorder,
              focusedLabelColor = Gold400,
              unfocusedLabelColor = Slate400,
            ),
          )

          if (proofError != null) {
            Text(
              text = proofError!!,
              style = MaterialTheme.typography.bodySmall,
              color = Rose400,
            )
          }
        }
      },
      confirmButton = {
        TournamentButton(
          text = strings.matchDetailSubmitProofButton,
          onClick = {
            if (uiState.isSubmittingResult || isProofLocked) return@TournamentButton
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
          },
          enabled = !uiState.isSubmittingResult && !isProofLocked,
          isLoading = uiState.isSubmittingResult,
          testTag = "dialog_confirm_proof_btn",
        )
      },
      dismissButton = {
        TextButton(
          onClick = { showProofDialog = false },
          enabled = !uiState.isSubmittingResult,
        ) {
          Text(strings.cancel, color = Slate400)
        }
      },
    )
  }
}
