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
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalAppStrings
import com.example.domain.model.UserEntity
import com.example.ui.components.LoadingState
import com.example.ui.components.TournamentButton
import com.example.ui.components.TournamentButtonVariant
import com.example.ui.components.TournamentCard
import com.example.ui.components.TournamentTextField
import com.example.ui.theme.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class AvatarPreset(
  val id: String,
  val label: String,
  val icon: ImageVector,
  val gradient: List<Color>,
)

private val avatarPresets = listOf(
  AvatarPreset("preset://dragon", "Dragon", Icons.Default.EmojiEvents, listOf(Color(0xFFE53935), Color(0xFFFFB300))),
  AvatarPreset("preset://cyber", "Cyber", Icons.Default.SportsEsports, listOf(Color(0xFF7C4DFF), Color(0xFF00E5FF))),
  AvatarPreset("preset://ninja", "Ninja", Icons.Default.Bolt, listOf(Color(0xFF00B0FF), Color(0xFF1DE9B6))),
  AvatarPreset("preset://crown", "Crown", Icons.Default.Star, listOf(Color(0xFFFFD700), Color(0xFFFF6D00))),
  AvatarPreset("preset://sniper", "Sniper", Icons.Default.Security, listOf(Color(0xFF00E676), Color(0xFF00B0FF))),
  AvatarPreset("preset://shield", "Knight", Icons.Default.Shield, listOf(Color(0xFF90A4AE), Color(0xFF37474F))),
)

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
            bannerMessage = strings.changePhotoTitle
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
      val locale = if (currentLanguage == AppLanguage.BN) Locale("bn", "BD") else Locale.ENGLISH
      val formatted = SimpleDateFormat("d MMMM yyyy", locale).format(Date(date))
      if (currentLanguage == AppLanguage.BN) {
        // Only translate ASCII digits to avoid re-translating existing Bengali digits (Unicode arithmetic fix)
        formatted.map { if (it in '0'..'9') '০' + (it - '0') else it }.joinToString("")
      } else formatted
    } else {
      strings.statRecentlyJoined
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = strings.profileTitle,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("profile_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = Color.White)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = NavySurface),
      )
    },
    containerColor = DeepNavyBg,
    modifier = modifier.fillMaxSize(),
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
            Surface(
              color = Cyan500.copy(alpha = 0.15f),
              border = BorderStroke(1.dp, Cyan400),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Cyan400, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = bannerMessage ?: "",
                  color = Color.White,
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { bannerMessage = null }, modifier = Modifier.size(24.dp)) {
                  Icon(Icons.Default.Close, contentDescription = strings.close, tint = Slate400, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }

        // 1. ESPORTS PROFILE HEADER CARD
        item {
          TournamentCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NavyCard,
            borderColor = NavyCardBorder,
          ) {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              // Interactive Avatar with Edit Photo Badge
              Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                  .clickable { showAvatarDialog = true }
                  .testTag("profile_avatar_badge"),
              ) {
                EsportsAvatarView(
                  photoUrl = currentUser.effectivePhoto,
                  name = currentUser.effectiveName,
                  size = 84.dp,
                )
                Surface(
                  shape = CircleShape,
                  color = Gold400,
                  border = BorderStroke(2.dp, NavyCard),
                  modifier = Modifier.size(26.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.CameraAlt,
                      contentDescription = strings.changePhotoTitle,
                      tint = Color.Black,
                      modifier = Modifier.size(14.dp),
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Player Name + Edit action
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showEditNameDialog = true },
              ) {
                Text(
                  text = currentUser.effectiveName.ifBlank { strings.playerLabel },
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                  color = Color.White,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = strings.updateDisplayNameTitle,
                  tint = Gold400,
                  modifier = Modifier.size(18.dp),
                )
              }

              Spacer(modifier = Modifier.height(4.dp))

              // User ID (Copyable)
              val uidToDisplay = currentUser.userId.ifBlank { currentUser.uid }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .clickable {
                    clipboardManager.setText(AnnotatedString(uidToDisplay))
                    bannerMessage = strings.userCopied(uidToDisplay)
                  }
                  .testTag("copy_user_id_button"),
              ) {
                Text(
                  text = "${strings.labelId} $uidToDisplay",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = Cyan400,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  imageVector = Icons.Default.ContentCopy,
                  contentDescription = strings.labelId,
                  tint = Cyan400,
                  modifier = Modifier.size(14.dp),
                )
              }

              Spacer(modifier = Modifier.height(14.dp))

              // Verified Mobile Number Badge (Locked)
              Surface(
                color = Slate900,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = currentUser.effectiveMobile,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                      color = Color.White,
                    )
                  }
                  Surface(
                    color = Slate800,
                    shape = RoundedCornerShape(6.dp),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Icon(Icons.Default.Lock, contentDescription = null, tint = Slate400, modifier = Modifier.size(12.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = strings.locked,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Slate400,
                      )
                    }
                  }
                }
              }

              Text(
                text = strings.mobileLockedNotice,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Slate500,
                modifier = Modifier.padding(top = 6.dp),
              )
            }
          }
        }

        // 2. CAREER STATISTICS GRID
        item {
          Column {
            Text(
              text = strings.careerStatistics,
              style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
              ),
              color = Gold400,
            )
            Spacer(modifier = Modifier.height(10.dp))
            val totalMatches = uiState.totalMatches
            val wins = uiState.wins
            val losses = uiState.losses
            val winRate = uiState.winRate
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              StatBox(title = strings.statMatches, value = "$totalMatches", subtitle = strings.statMatchesSub, modifier = Modifier.weight(1f))
              StatBox(
                title = strings.statWins,
                value = "$wins",
                subtitle = strings.winRateSubtitle(winRate),
                color = Emerald500,
                glowColor = Emerald500,
                modifier = Modifier.weight(1f)
              )
              StatBox(title = strings.statLosses, value = "$losses", subtitle = strings.statLossesSub, color = Rose500, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              StatBox(
                title = strings.statTotalWinnings,
                value = "৳ ${"%.2f".format(currentUser.getDisplayTotalWinnings())}",
                subtitle = strings.statAllTimeRewards,
                color = Gold400,
                glowColor = Gold400,
                modifier = Modifier.weight(1f),
              )
              StatBox(
                title = strings.statMemberSince,
                value = joinDateFormatted,
                subtitle = strings.statVerifiedPlayer,
                color = Cyan400,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }

        // 3. SUPPORT CENTER HUB
        item {
          Column {
            Text(
              text = strings.helpAndSupportTitle,
              style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
              ),
              color = Gold400,
            )
            Text(
              text = strings.helpAndSupportSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = Slate400,
              modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
            )

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
          TournamentCard(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("profile_language_card"),
            backgroundColor = NavyCard,
            borderColor = CardBorder,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(Cyan400.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.Language,
                  contentDescription = null,
                  tint = Cyan400,
                  modifier = Modifier.size(22.dp),
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = strings.languageSettingTitle,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = Color.White,
                )
                Text(
                  text = strings.languageSettingSubtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = Slate400,
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              val isBnSelected = currentLanguage == AppLanguage.BN
              Surface(
                modifier = Modifier
                  .weight(1f)
                  .clickable { onLanguageChange(AppLanguage.BN) }
                  .testTag("language_option_bn"),
                shape = RoundedCornerShape(10.dp),
                color = if (isBnSelected) Indigo600.copy(alpha = 0.4f) else Slate900,
                border = BorderStroke(1.dp, if (isBnSelected) Gold400 else Slate800),
              ) {
                Row(
                  modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center,
                ) {
                  if (isBnSelected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Gold400, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                  }
                  Text(
                    text = strings.languageBengali,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isBnSelected) Gold400 else Slate300,
                  )
                }
              }

              val isEnSelected = currentLanguage == AppLanguage.EN
              Surface(
                modifier = Modifier
                  .weight(1f)
                  .clickable { onLanguageChange(AppLanguage.EN) }
                  .testTag("language_option_en"),
                shape = RoundedCornerShape(10.dp),
                color = if (isEnSelected) Indigo600.copy(alpha = 0.4f) else Slate900,
                border = BorderStroke(1.dp, if (isEnSelected) Gold400 else Slate800),
              ) {
                Row(
                  modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center,
                ) {
                  if (isEnSelected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Gold400, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                  }
                  Text(
                    text = strings.languageEnglish,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isEnSelected) Gold400 else Slate300,
                  )
                }
              }
            }
          }
        }

        // 6. FAIR PLAY & INTEGRITY POLICY
        item {
          TournamentCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NavyCard,
            borderColor = CardBorder,
          ) {
            Text(
              text = strings.fairPlayTitle,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = Gold400,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = strings.fairPlayText(currentUser.status),
              style = MaterialTheme.typography.bodySmall,
              color = Slate400,
              lineHeight = 18.sp,
            )
          }
        }

        // 7. SIGN OUT BUTTON
        item {
          val interactionSource = remember { MutableInteractionSource() }
          val isPressed by interactionSource.collectIsPressedAsState()
          val pressScale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "LogoutPress")

          TournamentButton(
            text = strings.signOutButton,
            onClick = { showSignOutDialog = true },
            variant = TournamentButtonVariant.DANGER,
            leadingIcon = {
              Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            },
            modifier = Modifier
              .fillMaxWidth()
              .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
              }
              .border(
                BorderStroke(1.dp, Brush.linearGradient(listOf(Rose500.copy(alpha = 0.5f), Color.Transparent))),
                shape = RoundedCornerShape(12.dp)
              ),
            testTag = "profile_logout_button",
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
    AlertDialog(
      onDismissRequest = { showAvatarDialog = false },
      title = {
        Text(
          text = strings.changePhotoTitle,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
        )
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = strings.chooseAvatarPreset,
            style = MaterialTheme.typography.bodySmall,
            color = Slate400,
            modifier = Modifier.padding(bottom = 12.dp),
          )

          // 3x2 Preset Grid
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            avatarPresets.take(3).forEach { preset ->
              AvatarPresetItem(
                preset = preset,
                isSelected = currentUser.effectivePhoto == preset.id,
                onClick = {
                  val uid = currentUser.userId.ifBlank { currentUser.uid }
                  viewModel.updatePhoto(uid, preset.id)
                  showAvatarDialog = false
                },
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
          ) {
            avatarPresets.drop(3).take(3).forEach { preset ->
              AvatarPresetItem(
                preset = preset,
                isSelected = currentUser.effectivePhoto == preset.id,
                onClick = {
                  val uid = currentUser.userId.ifBlank { currentUser.uid }
                  viewModel.updatePhoto(uid, preset.id)
                  showAvatarDialog = false
                },
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Gallery Upload Option
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                showAvatarDialog = false
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
            shape = RoundedCornerShape(10.dp),
            color = Slate900,
            border = BorderStroke(1.dp, CardBorder),
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Gold400, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = strings.uploadFromGallery,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Gold400,
              )
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showAvatarDialog = false }) {
          Text(strings.cancel, color = Slate400)
        }
      },
      containerColor = NavyCard,
    )
  }

  // SIGN OUT CONFIRMATION DIALOG
  if (showSignOutDialog) {
    AlertDialog(
      onDismissRequest = { showSignOutDialog = false },
      title = {
        Text(
          text = if (currentLanguage == AppLanguage.BN) "লগআউট নিশ্চিত করুন" else "Confirm Logout",
          color = Color.White
        )
      },
      text = {
        Text(
          text = if (currentLanguage == AppLanguage.BN) "আপনি কি লগআউট করতে চান?" else "Are you sure you want to logout?",
          color = Slate300,
          style = MaterialTheme.typography.bodyMedium,
        )
      },
      confirmButton = {
        TournamentButton(
          text = strings.signOutButton,
          variant = TournamentButtonVariant.DANGER,
          onClick = {
            showSignOutDialog = false
            viewModel.signOut(onSignedOut)
          },
        )
      },
      dismissButton = {
        TextButton(onClick = { showSignOutDialog = false }) {
          Text(if (currentLanguage == AppLanguage.BN) "বাতিল" else strings.cancel, color = Slate400)
        }
      },
      containerColor = NavyCard,
    )
  }

  // COMING SOON / FEATURE NOTICE DIALOG
  showFeatureNoticeDialog?.let { (title, desc) ->
    AlertDialog(
      onDismissRequest = { showFeatureNoticeDialog = null },
      title = { Text(title, color = Color.White) },
      text = { Text(desc, color = Slate300, style = MaterialTheme.typography.bodyMedium) },
      confirmButton = {
        TournamentButton(
          text = strings.close,
          onClick = { showFeatureNoticeDialog = null },
        )
      },
      containerColor = NavyCard,
    )
  }
}

@Composable
private fun EsportsAvatarView(
  photoUrl: String?,
  name: String,
  size: Dp = 80.dp,
  modifier: Modifier = Modifier,
) {
  val preset = remember(photoUrl) {
    if (photoUrl?.startsWith("preset://") == true) {
      avatarPresets.firstOrNull { it.id == photoUrl }
    } else null
  }

  val bitmap = remember(photoUrl) {
    if (photoUrl?.startsWith("data:image/") == true) {
      try {
        val base64 = photoUrl.substringAfter("base64,")
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
      } catch (_: Exception) {
        null
      }
    } else null
  }

  val infiniteTransition = rememberInfiniteTransition(label = "AvatarPulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.98f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "PulseScale"
  )
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
    label = "Rotation"
  )

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier.size(size + 12.dp)
  ) {
    // Cyber Aura Ring
    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          scaleX = pulseScale
          scaleY = pulseScale
        }
        .drawBehind {
          rotate(rotation) {
            drawCircle(
              brush = Brush.sweepGradient(
                colors = listOf(Color(0xFF00E5FF), Color(0xFFFFD700), Color(0xFF00E5FF)),
                center = center
              ),
              style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
          }
          // Ambient Glow
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.2f), Color.Transparent),
              radius = size.toPx() * 0.7f
            )
          )
        }
    )

    Box(
      modifier = Modifier
        .size(size)
        .clip(CircleShape)
        .background(
          Brush.linearGradient(
            preset?.gradient ?: listOf(Indigo600, Purple600)
          )
        ),
      contentAlignment = Alignment.Center,
    ) {
      if (bitmap != null) {
        Image(
          bitmap = bitmap,
          contentDescription = "Profile Photo",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
      } else if (preset != null) {
        Icon(
          imageVector = preset.icon,
          contentDescription = preset.label,
          tint = Color.White,
          modifier = Modifier.size(size * 0.55f),
        )
      } else {
        val firstLetter = name.trim().take(1).uppercase()
        if (firstLetter.isNotBlank()) {
          Text(
            text = firstLetter,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            color = Gold400,
          )
        } else {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Avatar",
            tint = Gold400,
            modifier = Modifier.size(size * 0.55f),
          )
        }
      }
    }
  }
}

@Composable
private fun AvatarPresetItem(
  preset: AvatarPreset,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .size(54.dp)
      .clickable { onClick() },
    shape = CircleShape,
    color = Color.Transparent,
    border = BorderStroke(2.dp, if (isSelected) Gold400 else Slate700),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Brush.linearGradient(preset.gradient)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = preset.icon,
        contentDescription = preset.label,
        tint = Color.White,
        modifier = Modifier.size(28.dp),
      )
    }
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
  val infiniteTransition = rememberInfiniteTransition(label = "AISupportGlow")
  val haloScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.6f,
    animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Restart),
    label = "HaloScale"
  )
  val haloAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Restart),
    label = "HaloAlpha"
  )

  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val pressScale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "PressScale")

  TournamentCard(
    modifier = modifier
      .graphicsLayer {
        scaleX = pressScale
        scaleY = pressScale
      }
      .clickable(interactionSource = interactionSource, indication = null) { onClick() },
    backgroundColor = NavyCard,
    borderColor = Cyan400.copy(alpha = 0.6f),
    backgroundContent = {
       Box(
         modifier = Modifier
           .fillMaxSize()
           .background(
             Brush.linearGradient(
               colors = listOf(Indigo900.copy(alpha = 0.15f), Color.Transparent),
               start = Offset(0f, 0f),
               end = Offset(1000f, 1000f)
             )
           )
       )
    }
  ) {
    Column {
      Box(
        modifier = Modifier.size(36.dp),
        contentAlignment = Alignment.Center
      ) {
        // Pulsing Energy Halo
        Canvas(modifier = Modifier.fillMaxSize()) {
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(Cyan400.copy(alpha = haloAlpha), Color.Transparent),
              radius = (size.minDimension / 2) * haloScale
            )
          )
        }
        
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(iconBg),
          contentAlignment = Alignment.Center,
        ) {
          Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = Color.White,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        color = Slate400,
        lineHeight = 14.sp,
      )
    }
  }
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
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val pressScale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "PressScale")

  TournamentCard(
    modifier = Modifier
      .fillMaxWidth()
      .graphicsLayer {
        scaleX = pressScale
        scaleY = pressScale
      }
      .clickable(enabled = isAvailable, interactionSource = interactionSource, indication = null) { onClick() },
    backgroundColor = NavyCard,
    borderColor = CardBorder,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(iconTint.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isAvailable) Color.White else Slate400,
          )
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = if (isAvailable) Slate400 else Slate500,
          )
        }
      }
      if (isAvailable) {
        Icon(
          imageVector = Icons.Default.ChevronRight,
          contentDescription = null,
          tint = Slate400,
          modifier = Modifier.size(20.dp),
        )
      } else {
        Surface(
          color = Slate800,
          shape = RoundedCornerShape(4.dp),
        ) {
          Text(
            text = "OFFLINE",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = Slate400,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun StatBox(
  title: String,
  value: String,
  subtitle: String,
  color: Color = Color.White,
  glowColor: Color? = null,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "StatGlow")
  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.04f,
    targetValue = 0.14f,
    animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "GlowAlpha"
  )

  val entranceScale = remember { androidx.compose.animation.core.Animatable(0.9f) }
  LaunchedEffect(value) {
    entranceScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
  }

  Surface(
    color = NavyCard,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(
      width = 1.dp,
      brush = Brush.linearGradient(
        listOf(CardBorder, CardBorder.copy(alpha = 0.2f), CardBorder)
      )
    ),
    modifier = modifier
      .graphicsLayer {
        scaleX = entranceScale.value
        scaleY = entranceScale.value
      }
      .drawBehind {
        if (glowColor != null) {
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(glowColor.copy(alpha = glowAlpha), Color.Transparent),
              radius = size.maxDimension * 0.9f,
              center = Offset(size.width / 2, size.height)
            )
          )
        }
      },
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = Slate400,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
        color = color,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = Slate500,
      )
    }
  }
}
