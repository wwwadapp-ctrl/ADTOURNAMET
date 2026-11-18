package com.example.ui.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.*
import com.example.domain.model.UserEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.R
import com.example.ui.wallet.WlFrame
import com.example.ui.wallet.WlTone
import com.example.ui.wallet.wlBackground
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
  viewModel: ProfileViewModel,
  user: UserEntity?,
  onNavigateBack: () -> Unit,
  onSignedOut: () -> Unit,
  modifier: Modifier = Modifier,
  onHistoryClick: () -> Unit = {},
  onNavigateToSupport: () -> Unit = {},
  onNavigateToRules: () -> Unit = {},
  onNavigateToAiSupport: () -> Unit = {},
  currentLanguage: AppLanguage = AppLanguage.BN,
  onLanguageChange: (AppLanguage) -> Unit = {},
) {
  val strings = LocalAppStrings.current
  val context = LocalContext.current
  val userState by viewModel.currentUser.collectAsState()
  val currentUser = userState ?: user
  val appSettings by viewModel.appSettings.collectAsState()
  val uiState by viewModel.uiState.collectAsState()
  val clipboardManager: ClipboardManager = LocalClipboardManager.current

  var showEditNameDialog by remember { mutableStateOf(false) }
  var showAvatarDialog by remember { mutableStateOf(false) }
  var showSignOutDialog by remember { mutableStateOf(false) }
  var showFeatureNoticeDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
  var bannerMessage by remember { mutableStateOf<String?>(null) }

  // Sync success and error messages from ViewModel
  LaunchedEffect(uiState.successMessage) {
    uiState.successMessage?.let {
      bannerMessage = it
      viewModel.clearFeedback()
    }
  }
  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let {
      bannerMessage = it
      viewModel.clearFeedback()
    }
  }

  // Handle atomic logout navigation
  LaunchedEffect(uiState.isLoggedOut) {
    if (uiState.isLoggedOut) {
      onSignedOut()
    }
  }

  // Gallery photo picker with automatic downscaling to compact JPEG Base64 (<25KB)
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri: Uri? ->
    uri?.let { selectedUri ->
      try {
        val stream = context.contentResolver.openInputStream(selectedUri)
        val originalBitmap = BitmapFactory.decodeStream(stream)
        stream?.close()
        if (originalBitmap != null) {
          val maxDim = 128
          val width = originalBitmap.width
          val height = originalBitmap.height
          val scale = if (width > height) maxDim.toFloat() / width else maxDim.toFloat() / height
          val scaledBitmap = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(originalBitmap, (width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1), true)
          } else {
            originalBitmap
          }
          val outStream = ByteArrayOutputStream()
          scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outStream)
          val bytes = outStream.toByteArray()
          val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
          val dataUrl = "data:image/jpeg;base64,$base64"
          val uid = currentUser?.userId ?: currentUser?.uid ?: ""
          if (uid.isNotBlank()) {
            viewModel.updatePhoto(uid, dataUrl)
            bannerMessage = strings.profilePhotoUpdateSuccess
          }
        }
      } catch (_: Exception) {
        bannerMessage = strings.errorGeneric
      }
    }
  }

  val joinDateFormatted = remember(uiState.resolvedJoinDate, currentLanguage) {
    val date = uiState.resolvedJoinDate
    if (date > 0L) {
      if (currentLanguage == AppLanguage.BN) {
        val bengaliMonths = listOf(
          "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
          "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
        )
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = date }
        val day = strings.digits(cal.get(java.util.Calendar.DAY_OF_MONTH).toString())
        val month = bengaliMonths[cal.get(java.util.Calendar.MONTH)]
        val year = strings.digits(cal.get(java.util.Calendar.YEAR).toString())
        "$day $month $year"
      } else {
        SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH).format(Date(date))
      }
    } else {
      strings.statRecentlyJoined
    }
  }

  Scaffold(
    topBar = {
      PfHeader(title = strings.profileTitle, backLabel = strings.back, onBack = onNavigateBack)
    },
    containerColor = Color.Transparent,
    modifier = modifier.fillMaxSize().wlBackground(),
  ) { innerPadding ->
    if (currentUser == null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentAlignment = Alignment.Center,
      ) {
        LoadingState(message = strings.loading)
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Feedback Banner
        item {
          AnimatedVisibility(visible = bannerMessage != null) {
            PfMessageBanner(
              message = bannerMessage ?: "",
              closeLabel = strings.close,
              onClose = { bannerMessage = null },
            )
          }
        }

        // 1. ESPORTS PROFILE HEADER CARD
        item {
          WlFrame(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), gold = true, pad = 14.dp) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              PfAvatarHalo(cameraLabel = strings.changePhotoTitle, onClick = { showAvatarDialog = true }) {
                EsportsAvatar(
                  photoUrl = currentUser.effectivePhoto,
                  name = currentUser.effectiveName,
                  size = 114.dp,
                  borderWidth = 0.dp,
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              PfNameRow(
                name = currentUser.effectiveName.ifBlank { strings.playerLabel },
                editLabel = strings.updateDisplayNameTitle,
                onEdit = { showEditNameDialog = true },
              )
              Spacer(modifier = Modifier.height(8.dp))
              val uidToDisplay = currentUser.userId.ifBlank { currentUser.uid }
              PfIdPill(
                text = "${strings.labelId} $uidToDisplay",
                copyLabel = strings.copy,
                onClick = {
                  clipboardManager.setText(AnnotatedString(uidToDisplay))
                  bannerMessage = strings.userCopied(uidToDisplay)
                },
              )
              Spacer(modifier = Modifier.height(14.dp))
              PfMobileRow(
                number = currentUser.effectiveMobile,
                lockedText = strings.locked,
                notice = strings.mobileLockedNotice,
              )
            }
          }
        }

        // 2. CAREER STATISTICS GRID
        item {
          Column {
            PfSectionTitle(strings.careerStatistics)
            val totalMatches = uiState.totalMatches
            val wins = uiState.wins
            val losses = uiState.losses
            val winRate = uiState.winRate
            Row(
              modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
              PfStatTile(R.drawable.wl_gamepad, strings.statMatches, strings.number(totalMatches), strings.statMatchesSub, Color(0xFF7DA8FF), WlTone.CREAM, Modifier.weight(1f))
              PfStatTile(R.drawable.wl_trophy, strings.statWins, strings.number(wins), strings.winRateSubtitle(winRate), Color(0xFF4ADE80), WlTone.GOLD, Modifier.weight(1f))
              PfStatTile(R.drawable.mg_dice, strings.statLosses, strings.number(losses), strings.statLossesSub, Color(0xFFFF6B81), WlTone.RED, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
              PfWideTile(R.drawable.wl_moneybag, strings.statTotalWinnings, strings.taka(currentUser.getDisplayTotalWinnings()), strings.statAllTimeRewards, Color(0xFFFFD45A), WlTone.GOLD, Modifier.weight(1f))
              PfWideTile(R.drawable.mg_coin, strings.statMemberSince, joinDateFormatted, strings.statVerifiedPlayer, Color(0xFF67E8F9), WlTone.CREAM, Modifier.weight(1f), coin = true)
            }
          }
        }

        // 3. SUPPORT CENTER HUB
        item {
          Column {
            PfSectionTitle(strings.helpAndSupportTitle, strings.helpAndSupportSubtitle)
            Spacer(modifier = Modifier.height(10.dp))

            // AI Support Card (Full Width)
            SupportActionCard(
              title = strings.aiSupportTitle,
              subtitle = strings.aiSupportSubtitle,
              icon = Icons.Default.Bolt,
              iconTint = Cyan400,
              iconBg = Cyan400.copy(alpha = 0.15f),
              modifier = Modifier.fillMaxWidth(),
              onClick = {
                onNavigateToAiSupport()
              },
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic WhatsApp Contact
            val rawWhatsapp = appSettings?.activeWhatsappNumber?.trim().orEmpty()
            val isWhatsappAvailable = rawWhatsapp.isNotBlank()
            SupportChannelItem(
              title = strings.supportWhatsapp,
              subtitle = if (isWhatsappAvailable) rawWhatsapp else strings.contactUnavailable,
              icon = Icons.Default.Phone,
              iconTint = if (isWhatsappAvailable) Emerald400 else Slate500,
              isAvailable = isWhatsappAvailable,
              onClick = {
                if (isWhatsappAvailable) {
                  try {
                    val cleanPhone = rawWhatsapp.replace("+", "").replace(" ", "").replace("-", "")
                    val uri = if (cleanPhone.startsWith("http")) Uri.parse(cleanPhone) else Uri.parse("https://wa.me/$cleanPhone")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                  } catch (_: Exception) {
                    bannerMessage = strings.errorGeneric
                  }
                }
              },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Dynamic Telegram Contact
            val rawTelegram = appSettings?.activeTelegramUrl?.trim().orEmpty()
            val isTelegramAvailable = rawTelegram.isNotBlank()
            SupportChannelItem(
              title = strings.supportTelegram,
              subtitle = if (isTelegramAvailable) rawTelegram else strings.contactUnavailable,
              icon = Icons.Default.Send,
              iconTint = if (isTelegramAvailable) Cyan400 else Slate500,
              isAvailable = isTelegramAvailable,
              onClick = {
                if (isTelegramAvailable) {
                  try {
                    val cleanUrl = if (rawTelegram.startsWith("http")) rawTelegram else "https://t.me/${rawTelegram.removePrefix("@")}"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)))
                  } catch (_: Exception) {
                    bannerMessage = strings.errorGeneric
                  }
                }
              },
            )
          }
        }

        // 5. APP LANGUAGE SELECTION CARD
        item {
          PfLanguageCard(
            title = strings.languageSettingTitle,
            subtitle = strings.languageSettingSubtitle,
            bnText = strings.languageBengali,
            enText = strings.languageEnglish,
            bnSelected = currentLanguage == AppLanguage.BN,
            onBn = { onLanguageChange(AppLanguage.BN) },
            onEn = { onLanguageChange(AppLanguage.EN) },
            modifier = Modifier.fillMaxWidth().testTag("profile_language_card"),
          )
        }

        // 6. FAIR PLAY & INTEGRITY POLICY
        item {
          val localizedStatus = when (currentUser.status.trim().uppercase()) {
            "ACTIVE" -> strings.profileStatusActive
            "BANNED" -> strings.profileStatusBanned
            "SUSPENDED" -> strings.profileStatusSuspended
            "PENDING" -> strings.profileStatusPending
            else -> currentUser.status
          }
          PfFairPlayCard(
            title = strings.fairPlayTitle,
            text = strings.fairPlayText(localizedStatus),
            modifier = Modifier.fillMaxWidth(),
          )
        }

        // 7. SIGN OUT BUTTON
        item {
          PfLogoutButton(
            text = strings.signOutButton,
            onClick = { showSignOutDialog = true },
            tag = "profile_logout_button",
            modifier = Modifier.padding(top = 4.dp),
          )
        }
      }
    }
  }

  // EDIT DISPLAY NAME DIALOG
  if (showEditNameDialog && currentUser != null) {
    var newName by remember { mutableStateOf(currentUser.effectiveName) }
    AlertDialog(
      onDismissRequest = { showEditNameDialog = false },
      title = { Text(strings.updateDisplayNameTitle, color = Color.White) },
      text = {
        Column {
          TournamentTextField(
            value = newName,
            onValueChange = { newName = it },
            label = strings.fullName,
            placeholder = strings.enterNewNamePlaceholder,
            leadingIcon = Icons.Default.Person,
            testTag = "edit_name_input",
            modifier = Modifier.fillMaxWidth(),
          )
        }
      },
      confirmButton = {
        TournamentButton(
          text = strings.save,
          onClick = {
            if (newName.isNotBlank()) {
              val uid = currentUser.userId.ifBlank { currentUser.uid }
              viewModel.updateName(uid, newName)
              showEditNameDialog = false
              bannerMessage = strings.nameUpdateSuccess
            }
          },
          testTag = "confirm_edit_name_button",
        )
      },
      dismissButton = {
        TextButton(onClick = { showEditNameDialog = false }) {
          Text(strings.cancel, color = Slate400)
        }
      },
      containerColor = NavyCard,
    )
  }

  // AVATAR PICKER DIALOG (Presets + Gallery)
  if (showAvatarDialog && currentUser != null) {
    val isBn = strings === BengaliStrings
    PfAvatarPicker(
      title = strings.changePhotoTitle,
      subtitle = strings.chooseAvatarPreset,
      maleText = if (isBn) "ছেলে" else "Male",
      femaleText = if (isBn) "মেয়ে" else "Female",
      presets = avatarPresets,
      selectedId = findAvatarPreset(currentUser.effectivePhoto)?.id,
      labelOf = { strings.profileAvatarPreset(it) },
      onPick = { id ->
        val uid = currentUser.userId.ifBlank { currentUser.uid }
        viewModel.updatePhoto(uid, id)
        showAvatarDialog = false
      },
      galleryText = strings.uploadFromGallery,
      onGallery = {
        showAvatarDialog = false
        photoPickerLauncher.launch(
          PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
      },
      cancelText = strings.cancel,
      onDismiss = { showAvatarDialog = false },
    )
  }

  // SIGN OUT CONFIRMATION DIALOG
  if (showSignOutDialog) {
    PfLogoutConfirm(
      title = strings.profileLogoutDialogTitle,
      message = strings.profileLogoutDialogMessage,
      confirmText = strings.profileLogoutConfirmButton,
      cancelText = strings.cancel,
      onConfirm = {
        showSignOutDialog = false
        viewModel.signOut(onSignedOut)
      },
      onDismiss = { showSignOutDialog = false },
    )
  }

  // COMING SOON / FEATURE NOTICE DIALOG
  showFeatureNoticeDialog?.let { (title, desc) ->
    PfNoticeWindow(
      title = title,
      description = desc,
      closeText = strings.close,
      onClose = { showFeatureNoticeDialog = null },
    )
  }
}

@Composable
private fun SupportActionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color,
  iconBg: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  PfAiCard(title = title, subtitle = subtitle, onClick = onClick, modifier = modifier)
}

@Composable
private fun SupportChannelItem(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color,
  isAvailable: Boolean,
  onClick: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val isWhatsapp = icon == Icons.Default.Phone
  PfChannelRow(
    icon = icon,
    light = if (isWhatsapp) Color(0xFF25D366) else Color(0xFF2AABEE),
    dark = if (isWhatsapp) Color(0xFF0B5A2A) else Color(0xFF0B4A6A),
    title = title,
    subtitle = subtitle,
    available = isAvailable,
    offlineText = strings.profileSupportOffline,
    onClick = onClick,
  )
}
