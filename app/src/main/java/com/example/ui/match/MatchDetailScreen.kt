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
import com.example.core.i18n.LocalAppStrings
import com.example.domain.model.MatchStatus
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import com.example.ui.components.UnifiedEsportsTopBar
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
  val isBN = strings is com.example.core.i18n.BengaliStrings
  val uiState by viewModel.uiState.collectAsState()
  val match = uiState.match
  val isLudo = match?.gameType?.equals("LUDO", ignoreCase = true) == true

  var showProofDialog by remember { mutableStateOf(false) }
  var showJoinConfirmationDialog by remember { mutableStateOf(false) }
  var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
  var roomCodeInput by remember { mutableStateOf(match?.gameCode ?: "") }
  var matchNumberInput by remember { mutableStateOf(match?.matchNumber ?: "") }
  var proofNotesInput by remember { mutableStateOf("") }
  var proofError by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(match?.gameCode, match?.matchNumber) {
    if (match != null) {
      if (roomCodeInput.isBlank() && match.gameCode.isNotBlank()) roomCodeInput = match.gameCode
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

    val slotLabel = if (isBN) "স্লট" else "Slot"
    val registeredSlotLabel = registeredPlayer?.let { p ->
      when {
        p.slot.contains("1") || p.slot.equals(com.example.domain.model.PlayerSlot.PLAYER_1.name, ignoreCase = true) -> "$slotLabel ${if (isBN) "১" else "1"}"
        p.slot.contains("2") || p.slot.equals(com.example.domain.model.PlayerSlot.PLAYER_2.name, ignoreCase = true) -> "$slotLabel ${if (isBN) "২" else "2"}"
        p.slot.isNotBlank() -> p.slot.replace("PLAYER_", "$slotLabel ").replace("_", " ")
        else -> "$slotLabel ${if (isBN) "১" else "1"}"
      }
    } ?: "$slotLabel ${if (isBN) "১" else "1"}"

    val displayPlayersCount = maxOf(match.joinedPlayersCount, uiState.players.size)

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
            text = if (isLudo) {
              if (isBN) "লুডু ১v১ ব্যাটল" else "LUDO 1v1 BATTLE"
            } else {
              if (isBN) "ক্যারম ১v১ ব্যাটল" else "CARROM 1v1 BATTLE"
            },
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
          text = match.title,
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
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
                    contentDescription = if (isBN) "বিজয়ী" else "Winner",
                    tint = Gold400,
                    modifier = Modifier.size(16.dp),
                  )
                  val prizeText = if (isBN) "বিজয়ী পুরস্কার" else "Prize"
                  Text(
                    text = if (isBN) "আপনি বিজয়ী! $prizeText: ৳ ${"%.2f".format(match.displayPrizePool)}" else "You won! $prizeText: ৳ ${"%.2f".format(match.displayPrizePool)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
                    contentDescription = "Lost",
                    tint = Rose400,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = if (isResultRejected) "প্রুফ বাতিল করা হয়েছে (পরাজিত)" else "ম্যাচ সমাপ্ত / আপনি পরাজিত",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
                  contentDescription = "Registered",
                  tint = Emerald400,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = "You are registered ($registeredSlotLabel)",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
        ) {
          Column {
            Text(text = if (isBN) "এন্ট্রি ফি" else "ENTRY FEE", style = MaterialTheme.typography.labelSmall, color = Slate400)
            Text(
              text = "৳ ${"%.2f".format(match.displayEntryFee)}",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
            )
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = if (isBN) "প্লেয়ার" else "PLAYERS", style = MaterialTheme.typography.labelSmall, color = Slate400)
            Text(
              text = "$displayPlayersCount / ${match.maxPlayers}",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = Cyan400,
            )
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(text = if (isBN) "বিজয়ী পুরস্কার" else "WINNER PRIZE", style = MaterialTheme.typography.labelSmall, color = Slate400)
            Text(
              text = "৳ ${"%.2f".format(match.displayPrizePool)}",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
              color = Gold400,
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
              text = if (isBN) "এই ম্যাচটি আপকামিং এবং অ্যাডমিন রিলিজ করার পর জয়েন করার জন্য উন্মুক্ত হবে।" else "This match is Upcoming and will open for joining once released by admin.",
              color = Amber400,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
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
            style = MaterialTheme.typography.bodySmall,
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
            text = if (isBN) "গেম রুম কোড" else "Game Room Code",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
          )
          if (isUserRegistered) {
            Surface(
              color = Emerald900.copy(alpha = 0.4f),
              shape = RoundedCornerShape(6.dp),
              border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.7f)),
            ) {
              Text(
                text = if (isBN) "আনলক হয়েছে" else "UNLOCKED",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
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
                  text = if (isBN) "গেম রুম কোড" else "ROOM CODE",
                  style = MaterialTheme.typography.labelSmall,
                  color = Gold400
                )
                Text(
                  text = match.gameCode,
                  style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                  ),
                  color = Color.White,
                )
                Text(
                  text = if (isBN) "লুডু কিং / ক্যারম পুল ওপেন করে এই কোডটি দিয়ে গেমে প্রবেশ করুন" else "Open Ludo King / Carrom Pool and enter this code to join match room",
                  style = MaterialTheme.typography.bodySmall,
                  color = Slate400,
                  modifier = Modifier.padding(top = 6.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))
                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                val context = androidx.compose.ui.platform.LocalContext.current
                TournamentButton(
                  text = strings.copy,
                  onClick = {
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(match.gameCode))
                    Toast.makeText(context, strings.roomCodeCopiedNotice, Toast.LENGTH_SHORT).show()
                  },
                  variant = TournamentButtonVariant.SECONDARY,
                  modifier = Modifier.widthIn(min = 120.dp),
                )
              }
            }
          } else {
            Text(
              text = if (isBN) "অ্যাডমিন গেম শুরু হওয়ার ৫-১০ মিনিট আগে রুম কোড জেনারেট করবেন। অনুগ্রহ করে অপেক্ষা করুন।" else "Super Admin will generate the room code 5-10 minutes before the scheduled match time. Please keep this screen open.",
              style = MaterialTheme.typography.bodyMedium,
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
                text = if (isBN) "গেম রুম কোডটি গোপনীয় এবং শুধুমাত্র ম্যাচে যুক্ত প্লেয়ারদের জন্য উন্মুক্ত।" else "Game room code is confidential and accessible only to registered match participants.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400,
              )
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))

      // Result Submission Status or Action
      val currentResult = uiState.submittedResult
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
                "APPROVED" -> "✓ Approved"
                "REJECTED" -> "✕ Rejected"
                else -> "Screenshot Submitted ✓"
              },
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
                  "APPROVED" -> "✓ Approved"
                  "REJECTED" -> "✕ Rejected"
                  else -> "Under Review"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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
              text = "অনুগ্রহ করে অপেক্ষা করুন, ফলাফল যাচাই করা হচ্ছে।",
              style = MaterialTheme.typography.bodySmall,
              color = Slate300,
            )
          }
          if (result.reviewNotes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Notes: ${result.reviewNotes}",
              style = MaterialTheme.typography.bodySmall,
              color = if (result.status == "REJECTED") Rose400 else Slate400,
            )
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
      }

      if (isUserRegistered) {
        val currentPlayer = uiState.players.find {
          it.effectiveUid == activeUid ||
          it.userId == activeUid ||
          it.uid == activeUid ||
          (!authUid.isBlank() && (it.effectiveUid == authUid || it.uid == authUid || it.userId == authUid))
        }
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
                  text = "স্ক্রিনশট প্রসেস ও সাবমিট হচ্ছে...",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color.White,
                )
                Text(
                  text = "অনুগ্রহ করে অপেক্ষা করুন, ফলাফল জমা দেওয়া হচ্ছে।",
                  style = MaterialTheme.typography.bodySmall,
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
                  text = "প্রুফ বাতিল করা হয়েছে",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = Rose400,
                )
                Text(
                  text = "ফলাফল প্রত্যাখ্যাত হওয়ায় পুনরায় প্রুফ সাবমিট করার সুযোগ নেই।",
                  style = MaterialTheme.typography.bodySmall,
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
                  text = "ম্যাচ সমাপ্ত / ফলাফল নিশ্চিত হয়েছে",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color.White,
                )
                Text(
                  text = if (isUserWinner) "আপনি বিজয়ী! পুরস্কার ওয়ালেটে যুক্ত হয়েছে।" else "ম্যাচটি সমাপ্ত হয়েছে। নতুন কোনো প্রুফ গ্রহণ করা হবে না।",
                  style = MaterialTheme.typography.bodySmall,
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
                  text = "ফলাফল নিশ্চিত ও অনুমোদিত হয়েছে",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = Emerald300,
                )
                Text(
                  text = "আপনার জয়ের প্রমাণ সফলভাবে অনুমোদিত হয়েছে।",
                  style = MaterialTheme.typography.bodySmall,
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
                  text = "ম্যাচ সমাপ্ত / ফলাফল নিশ্চিত হয়েছে",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = Slate300,
                )
                Text(
                  text = "পরাজিত খেলোয়াড়দের জন্য প্রুফ সাবমিশন প্রযোজ্য নয়।",
                  style = MaterialTheme.typography.bodySmall,
                  color = Slate400,
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        } else if (currentResult != null) {
          // Already shown in top review card
        } else if (canSubmitProof) {
          TournamentButton(
            text = "SUBMIT RESULT SCREENSHOT",
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
                text = if (isBN) "অ্যাডমিন রুম কোড দেওয়ার পর স্ক্রিনশট সাবমিট করার অপশনটি চালু হবে।" else "Screenshot submission will be available once Admin provides the Room Code.",
                style = MaterialTheme.typography.bodySmall,
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
                text = "রুমকোড দেওয়ার ২ মিনিট পর স্ক্রিনশট সাবমিট করা যাবে",
                style = MaterialTheme.typography.bodySmall,
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
          text = if (isBN) "নিবন্ধিত প্লেয়ার ($displayPlayersCount/${match.maxPlayers})" else "Registered Players ($displayPlayersCount/${match.maxPlayers})",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (uiState.players.isEmpty()) {
          Text(
            text = if (isBN) "এখনও কোনো প্লেয়ার যুক্ত হননি। আপনিই প্রথম জয়েন করুন!" else "No players have joined yet. Be the first to join!",
            style = MaterialTheme.typography.bodyMedium,
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
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal),
                    color = Color.White
                  )
                  if (isCurrent) {
                    Surface(
                      color = Emerald500.copy(alpha = 0.2f),
                      shape = RoundedCornerShape(4.dp),
                    ) {
                      Text(
                        text = if (isBN) "আপনি" else "YOU",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp),
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
                val formattedSlot = when {
                  p.slot.contains("1") || p.slot.equals(com.example.domain.model.PlayerSlot.PLAYER_1.name, ignoreCase = true) -> "Slot 1"
                  p.slot.contains("2") || p.slot.equals(com.example.domain.model.PlayerSlot.PLAYER_2.name, ignoreCase = true) -> "Slot 2"
                  p.slot.isNotBlank() -> p.slot.replace("PLAYER_", "Slot ").replace("_", " ")
                  else -> p.slot
                }
                Text(
                  text = formattedSlot,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
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
          isCompleted && isUserWinner -> (if (isBN) "বিজয়ী" else "WINNER") + " • ৳ ${"%.2f".format(match.displayPrizePool)}"
          isCompleted -> if (isBN) "পরাজিত" else "LOST"
          isResultApproved -> (if (isBN) "বিজয়ী" else "WINNER") + " • ৳ ${"%.2f".format(match.displayPrizePool)}"
          isResultRejected || isResultLost -> if (isBN) "পরাজিত" else "LOST"
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
          text = if (isBN) "আসন্ন (এখনও শুরু হয়নি)" else "UPCOMING (NOT RELEASED)",
          onClick = { /* non-joinable */ },
          enabled = false,
          variant = TournamentButtonVariant.SECONDARY,
          modifier = Modifier.fillMaxWidth(),
          testTag = "detail_upcoming_button",
        )
      } else if (!isFull && !isRunning && !isCompleted) {
        TournamentButton(
          text = if (isBN) "ম্যাচে জয়েন করুন (৳ ${"%.2f".format(match.displayEntryFee)})" else "JOIN MATCH (৳ ${"%.2f".format(match.displayEntryFee)})",
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
          text = if (isBN) "⚠️ জরুরি নোটিশ ও নিয়মাবলী" else "⚠️ Important Notice & Rules",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = Color.White
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          val gameNotice = if (isBN) {
            if (isLudo) "প্রিয় ইউজার, আপনার AD Tournament অ্যাপের নাম এবং Ludo King অ্যাপের নাম অবশ্যই একই (Same) হতে হবে। নাম অমিল থাকলে ফলাফল বাতিল হতে পারে।"
            else "প্রিয় ইউজার, আপনার AD Tournament অ্যাপের নাম এবং Carrom Pool অ্যাপের নাম অবশ্যই একই (Same) হতে হবে। নাম অমিল থাকলে ফলাফল বাতিল হতে পারে।"
          } else {
            if (isLudo) "Dear user, your AD Tournament name and Ludo King name must be the same. Mismatch may lead to result cancellation."
            else "Dear user, your AD Tournament name and Carrom Pool name must be the same. Mismatch may lead to result cancellation."
          }

          val feeNotice = if (isBN) "ম্যাচ এন্ট্রি ফি ৳${"%.2f".format(match.displayEntryFee)} আপনার ওয়ালেট থেকে কাটা হবে।"
          else "Match entry fee ৳${"%.2f".format(match.displayEntryFee)} will be deducted from your wallet."

          Text(text = gameNotice, style = MaterialTheme.typography.bodyMedium, color = Slate300)
          Text(text = feeNotice, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Gold400)
        }
      },
      confirmButton = {
        TournamentButton(
          text = if (isBN) "সম্মত ও জয়েন করুন" else "Agree & Join",
          onClick = {
            showJoinConfirmationDialog = false
            viewModel.joinMatch()
          },
          variant = TournamentButtonVariant.PRIMARY
        )
      },
      dismissButton = {
        TextButton(onClick = { showJoinConfirmationDialog = false }) {
          Text(text = if (isBN) "বাতিল" else "Cancel", color = Slate400)
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
          text = if (isBN) "ফলাফল স্ক্রিনশট জমা দিন" else "Submit Result Screenshot",
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
            text = "আপনার স্ক্রিনশট বাধ্যতামূলক দিতে হবে",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Gold400,
          )

          TournamentButton(
            text = if (selectedImageUri == null) {
              if (isBN) "গ্যালারি থেকে স্ক্রিনশট নির্বাচন করুন" else "Select Screenshot from Gallery"
            } else {
              if (isBN) "স্ক্রিনশট পরিবর্তন করুন" else "Change Screenshot"
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
            text = "রুমকোড বাধ্যতামূলক দিতে হবে",
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
            label = { Text(if (isBN) "রুম কোড" else "Room Code") },
            placeholder = { Text(if (isBN) "রুম কোড লিখুন" else "Enter room code") },
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
            text = "আপনি কত নাম্বার ম্যাচ খেলেছেন সেটাও লিখুন",
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
            label = { Text(if (isBN) "ম্যাচ নাম্বার" else "Match Number") },
            placeholder = { Text(if (isBN) "উদা: ম্যাচ #১০১" else "e.g., Match #101") },
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
            label = { Text(if (isBN) "নোট / বিজয়ীর মন্তব্য (ঐচ্ছিক)" else "Notes / Winner Remarks (Optional)") },
            placeholder = { Text(if (isBN) "উদা: লুডু কিং ১ম স্থান" else "e.g., Ludo King 1st place") },
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
          text = if (isBN) "প্রুফ সাবমিট করুন" else "SUBMIT PROOF",
          onClick = {
            if (uiState.isSubmittingResult || isProofLocked) return@TournamentButton
            when {
              selectedImageUri == null -> {
                proofError = if (isBN) "আপনার স্ক্রিনশট বাধ্যতামূলক দিতে হবে" else "আপনার স্ক্রিনশট বাধ্যতামূলক দিতে হবে"
              }
              roomCodeInput.trim().isEmpty() -> {
                proofError = if (isBN) "রুমকোড বাধ্যতামূলক দিতে হবে" else "রুমকোড বাধ্যতামূলক দিতে হবে"
              }
              matchNumberInput.trim().isEmpty() -> {
                proofError = if (isBN) "আপনি কত নাম্বার ম্যাচ খেলেছেন সেটাও লিখুন" else "আপনি কত নাম্বার ম্যাচ খেলেছেন সেটাও লিখুন"
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
          Text(if (isBN) "বাতিল" else "Cancel", color = Slate400)
        }
      },
    )
  }
}
