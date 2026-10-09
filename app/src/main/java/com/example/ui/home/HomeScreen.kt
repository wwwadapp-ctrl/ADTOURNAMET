package com.example.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.media.RingtoneManager
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.core.notification.RoomCodeNotificationManager
import com.example.data.repository.FirebaseNotificationRepository
import com.example.domain.repository.NotificationRepository
import coil.request.ImageRequest
import coil.size.Precision
import com.example.R
import com.example.core.i18n.LocalAppStrings
import com.example.domain.model.AppSettingsEntity
import com.example.domain.model.GameType
import com.example.domain.model.MatchEntity
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.minigames.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GuideType {
  DEPOSIT,
  MATCH_JOIN,
  RESULT_SUBMIT,
  RULES,
}

data class GuideBannerItem(
  val id: String,
  val title: String,
  val description: String,
  val ctaText: String,
  val badgeText: String,
  val icon: ImageVector,
  val accentColor: Color,
  val secondaryColor: Color,
  val guideType: GuideType,
  val videoUrlOverride: String? = null,
)

@Composable
private fun rememberGuideBanners(): List<GuideBannerItem> {
  val strings = LocalAppStrings.current
  return remember(strings) {
    listOf(
      GuideBannerItem(
        id = "deposit_guide",
        title = strings.depositGuideTitle,
        description = strings.depositGuideSub,
        ctaText = strings.viewGuideCta,
        badgeText = "DEPOSIT GUIDE",
        icon = Icons.Default.AccountBalanceWallet,
        accentColor = Gold400,
        secondaryColor = Cyan400,
        guideType = GuideType.DEPOSIT,
      ),
      GuideBannerItem(
        id = "match_join_guide",
        title = strings.matchJoinGuideTitle,
        description = strings.matchJoinGuideSub,
        ctaText = strings.viewGuideCta,
        badgeText = "JOIN GUIDE",
        icon = Icons.Default.PlayArrow,
        accentColor = Cyan400,
        secondaryColor = Indigo400,
        guideType = GuideType.MATCH_JOIN,
      ),
      GuideBannerItem(
        id = "result_guide",
        title = strings.resultGuideTitle,
        description = strings.resultGuideSub,
        ctaText = strings.viewGuideCta,
        badgeText = "RESULT GUIDE",
        icon = Icons.Default.Star,
        accentColor = Amber400,
        secondaryColor = Emerald400,
        guideType = GuideType.RESULT_SUBMIT,
      ),
      GuideBannerItem(
        id = "rules_guide",
        title = strings.rulesGuideTitle,
        description = strings.rulesGuideSub,
        ctaText = strings.viewGuideCta,
        badgeText = "FAIR PLAY RULES",
        icon = Icons.Default.Info,
        accentColor = Gold400,
        secondaryColor = Rose400,
        guideType = GuideType.RULES,
      ),
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  viewModel: HomeViewModel,
  currentUser: UserEntity?,
  wallet: WalletEntity?,
  onMatchClick: (String) -> Unit,
  onWalletClick: () -> Unit,
  onDepositClick: () -> Unit,
  onWithdrawClick: () -> Unit = {},
  onNotificationClick: () -> Unit,
  onProfileClick: () -> Unit,
  onRulesClick: () -> Unit,
  onSupportClick: () -> Unit,
  onAdminClick: () -> Unit,
  onReferClick: () -> Unit = {},
  onHistoryClick: () -> Unit = {},
  onAllMatchesClick: () -> Unit = {},
  onSpinWheelClick: () -> Unit = {},
  onDiceRollClick: () -> Unit = {},
  onHeadTailClick: () -> Unit = {},
  onScratchCardClick: () -> Unit = {},
  onAutoLudoClick: () -> Unit = {},
  unreadNotificationsCount: Int = 0,
  notificationRepository: NotificationRepository = remember { FirebaseNotificationRepository() },
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val uiState by viewModel.uiState.collectAsState()
  val appSettings by viewModel.appSettings.collectAsState()
  val isAdmin = currentUser?.role?.equals("ADMIN", ignoreCase = true) == true ||
      currentUser?.role?.equals("SUPER_ADMIN", ignoreCase = true) == true

  var selectedGuideBanner by remember { mutableStateOf<GuideBannerItem?>(null) }
  var selectedMiniGame by remember { mutableStateOf<MiniGameType?>(null) }
  val guideBanners = rememberGuideBanners()

  val context = LocalContext.current
  val activeUserId = currentUser?.userId?.ifBlank { currentUser.uid } ?: currentUser?.uid ?: ""

  val liveBalance = wallet?.let { w ->
    if (w.availableAmount > 0.0) w.availableAmount else w.availableBalance / 100.0
  } ?: 0.0
  val bonusBalance = wallet?.let { w ->
    if (w.bonusAmount > 0.0) w.bonusAmount else w.bonusBalance / 100.0
  } ?: 0.0
  val formattedBalance = "৳ ${"%.2f".format(liveBalance)}"
  val formattedBonus = "🎁 ৳ ${"%.2f".format(bonusBalance)}"

  Scaffold(
    topBar = {
      UnifiedEsportsTopBar(
        currentUser = currentUser,
        wallet = wallet,
        unreadNotificationsCount = unreadNotificationsCount,
        onWalletClick = onWalletClick,
        onNotificationClick = onNotificationClick,
        onProfileClick = onProfileClick,
        onAdminClick = if (isAdmin) onAdminClick else null,
      )
    },
    containerColor = DeepNavyBg,
    modifier = modifier.fillMaxSize(),
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MidnightBackgroundGradient)
        .padding(innerPadding),
      contentPadding = PaddingValues(bottom = 24.dp),
    ) {
      if (uiState.appSettings.announcementActive && uiState.appSettings.announcementMessage.isNotBlank()) {
        item {
          AnnouncementBar(message = uiState.appSettings.announcementMessage)
        }
      }

      // 0. REFERRAL PROMOTIONAL BANNER
      item {
        HomeReferralBanner(onClick = onReferClick)
      }

      // 1. QUICK ACTIONS: DEPOSIT / WITHDRAW
      item {
        HomeQuickActions(
          onDepositClick = onDepositClick,
          onWithdrawClick = onWithdrawClick,
        )
      }

      // 2. PREMIUM GUIDE CAROUSEL
      item {
        HomeGuideCarousel(
          banners = guideBanners,
          appSettings = uiState.appSettings,
          onBannerClick = { banner -> selectedGuideBanner = banner },
        )
      }

      // 2.5 MINI GAMES SECTION
      item {
        MiniGamesSection(
            onGameClick = { selectedMiniGame = it }
        )
      }

      // 2.6 AUTO GAMES SECTION HEADING
      item {
        AutoGamesSectionHeading()
      }

      // 2.7 BOARD GAMES SECTION (AUTO LUDO & CARROM POOL)
      item {
        LobbyBoardGamesSection(
            onLudoClick = onAutoLudoClick,
            onCarromClick = { android.widget.Toast.makeText(context, "ক্যারম পুল শীঘ্রই আসছে", android.widget.Toast.LENGTH_SHORT).show() }
        )
      }

      // 3. TOURNAMENT SECTION HEADING
      item {
        TournamentSectionHeading()
      }

      // 4. ALL MATCHES QUICK CARD
      item {
        AllMatchesQuickCard(
          onAllMatchesClick = onAllMatchesClick,
        )
      }

      // 5. GAME FILTER
      item {
        GameFilterSection(
          selectedFilter = uiState.selectedGameFilter,
          onFilterSelect = { viewModel.setGameFilter(it) },
        )
      }

      // 6. AVAILABLE MATCHES HEADER
      item {
        AvailableMatchesHeader(
          onSeeAllClick = onAllMatchesClick,
        )
      }

      // AVAILABLE MATCHES FEED
      when {
        uiState.isLoading -> {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 16.dp),
              contentAlignment = Alignment.Center,
            ) {
              LoadingState(message = strings.loadingMatches)
            }
          }
        }
        uiState.errorMessage != null -> {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            ) {
              ErrorState(
                message = uiState.errorMessage ?: strings.failedToLoadMatches,
                onRetry = { viewModel.refresh() },
              )
            }
          }
        }
        uiState.filteredMatches.isEmpty() -> {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            ) {
              EmptyState(
                title = "বর্তমানে কোনো ম্যাচ উপলব্ধ নেই",
                description = strings.noAvailableMatchesSubtext,
                actionButtonText = strings.refreshMatches,
                onActionClick = { viewModel.refresh() },
              )
            }
          }
        }
        else -> {
          val homeMatches = uiState.filteredMatches.take(6)
          items(homeMatches, key = { it.matchId }) { match ->
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
              MatchCard(
                match = match,
                isJoined = uiState.joinedMatchIds.contains(match.matchId),
                onCardClick = { onMatchClick(match.matchId) },
                onJoinClick = { onMatchClick(match.matchId) },
                onViewCodeClick = { onMatchClick(match.matchId) },
                onSubmitProofClick = { onMatchClick(match.matchId) },
              )
            }
          }

          if (uiState.filteredMatches.size > 6) {
            item {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 12.dp),
              ) {
                TournamentButton(
                  text = strings.viewAllMatchesCount(uiState.filteredMatches.size),
                  onClick = onAllMatchesClick,
                  variant = TournamentButtonVariant.OUTLINED,
                  modifier = Modifier.fillMaxWidth(),
                  testTag = "home_view_all_matches_btn",
                )
              }
            }
          }
        }
      }
    }

    // Guide Modal Dialog when CTA is tapped
    selectedGuideBanner?.let { banner ->
      GuideDetailDialog(
        banner = banner,
        appSettings = appSettings,
        onDismiss = { selectedGuideBanner = null },
        onAction = {
          selectedGuideBanner = null
          when (banner.guideType) {
            GuideType.DEPOSIT -> onDepositClick()
            GuideType.MATCH_JOIN -> onAllMatchesClick()
            GuideType.RESULT_SUBMIT -> onAllMatchesClick()
            GuideType.RULES -> onRulesClick()
          }
        },
      )
    }

    // Mini Games Dialogs
    when (selectedMiniGame) {
      MiniGameType.SPIN -> {
          selectedMiniGame = null
          onSpinWheelClick()
      }
      MiniGameType.DICE -> {
          selectedMiniGame = null
          onDiceRollClick()
      }
      MiniGameType.EVEN_ODD -> {
          selectedMiniGame = null
          onDiceRollClick()
      }
      MiniGameType.HEAD_TAIL -> {
          selectedMiniGame = null
          onHeadTailClick()
      }
      MiniGameType.SCRATCH -> {
          selectedMiniGame = null
          onScratchCardClick()
      }
      null -> {}
    }
  }
}

@Composable
fun HomeReferralBanner(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .clip(RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .testTag("home_referral_banner"),
    color = Slate900,
    border = BorderStroke(1.dp, Gold400.copy(alpha = 0.5f))
  ) {
    Row(
      modifier = Modifier
        .background(
          Brush.horizontalGradient(
            colors = listOf(Gold400.copy(alpha = 0.15f), Color.Transparent)
          )
        )
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Surface(
          shape = CircleShape,
          color = Gold400.copy(alpha = 0.2f),
          modifier = Modifier.size(40.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Gold400, modifier = Modifier.size(24.dp))
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            "বন্ধু রেফার করলেই ৳৪০ বোনাস!",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
          )
          Text(
            "প্রথম ডিপোজিটে ক্যাশ রিওয়ার্ড",
            style = MaterialTheme.typography.labelSmall,
            color = Gold400
          )
        }
      }
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = Gold400,
        modifier = Modifier.padding(start = 8.dp)
      ) {
        Text(
          "রেফার করুন >",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
          color = Slate950,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }
    }
  }
}

// -----------------------------------------------------------------------
// 1. HOME QUICK ACTIONS (DEPOSIT / WITHDRAW)
// -----------------------------------------------------------------------

@Composable
fun HomeQuickActions(
  onDepositClick: () -> Unit,
  onWithdrawClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Left: ADD MONEY / DEPOSIT
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MidnightNavyCard,
      border = BorderStroke(0.8.dp, NeonCyan.copy(alpha = 0.5f)),
      modifier = Modifier
        .weight(1f)
        .height(56.dp)
        .clickable(onClick = onDepositClick)
        .testTag("home_quick_deposit_btn"),
    ) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.horizontalGradient(
              colors = listOf(
                NeonCyan.copy(alpha = 0.12f),
                Color.Transparent,
              )
            )
          )
          .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Surface(
          shape = CircleShape,
          color = NeonCyan.copy(alpha = 0.18f),
          border = BorderStroke(0.8.dp, NeonCyan.copy(alpha = 0.6f)),
          modifier = Modifier.size(34.dp),
        ) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.AddCircle,
              contentDescription = strings.addMoneyTitle,
              tint = NeonCyan,
              modifier = Modifier.size(20.dp),
            )
          }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.Center) {
          Text(
            text = strings.addMoneyTitle,
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
            ),
            color = Color.White,
            maxLines = 1,
          )
          Text(
            text = "${strings.depositAction} ৳",
            style = MaterialTheme.typography.labelSmall,
            color = NeonCyan,
            maxLines = 1,
          )
        }
      }
    }

    // Right: WITHDRAW
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MidnightNavyCard,
      border = BorderStroke(0.8.dp, Gold400.copy(alpha = 0.5f)),
      modifier = Modifier
        .weight(1f)
        .height(56.dp)
        .clickable(onClick = onWithdrawClick)
        .testTag("home_quick_withdraw_btn"),
    ) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.horizontalGradient(
              colors = listOf(
                Gold400.copy(alpha = 0.12f),
                Color.Transparent,
              )
            )
          )
          .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Surface(
          shape = CircleShape,
          color = Gold400.copy(alpha = 0.18f),
          border = BorderStroke(0.8.dp, Gold400.copy(alpha = 0.6f)),
          modifier = Modifier.size(34.dp),
        ) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.ArrowUpward,
              contentDescription = strings.withdrawAction,
              tint = Gold400,
              modifier = Modifier.size(20.dp),
            )
          }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.Center) {
          Text(
            text = strings.withdrawAction.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
            ),
            color = Color.White,
            maxLines = 1,
          )
          Text(
            text = "${strings.withdrawAction} ৳",
            style = MaterialTheme.typography.labelSmall,
            color = Gold400,
            maxLines = 1,
          )
        }
      }
    }
  }
}

// -----------------------------------------------------------------------
// 2. PREMIUM GUIDE CAROUSEL COMPONENT
// -----------------------------------------------------------------------

@Composable
private fun HomeGuideCarousel(
  banners: List<GuideBannerItem>,
  appSettings: AppSettingsEntity,
  onBannerClick: (GuideBannerItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  val pagerState = rememberPagerState(pageCount = { banners.size })
  val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
  val context = LocalContext.current

  // Single authoritative auto-slide mechanism
  LaunchedEffect(isDragged, banners.size) {
    if (!isDragged && banners.size > 1) {
      while (true) {
        delay(4000L)
        val targetPage = (pagerState.currentPage + 1) % banners.size
        pagerState.animateScrollToPage(
          page = targetPage,
          animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing,
          ),
        )
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(top = 10.dp, bottom = 4.dp),
  ) {
    HorizontalPager(
      state = pagerState,
      contentPadding = PaddingValues(horizontal = 16.dp),
      pageSpacing = 12.dp,
      modifier = Modifier
        .fillMaxWidth()
        .height(190.dp)
        .testTag("home_guide_carousel"),
    ) { page ->
      val banner = banners[page]
      GuideBannerCard(
        banner = banner,
        appSettings = appSettings,
        onClick = {
          val videoUrl = when (banner.guideType) {
            GuideType.DEPOSIT -> appSettings.howToDepositVideoUrl
            GuideType.MATCH_JOIN -> appSettings.howToJoinVideoUrl
            GuideType.RESULT_SUBMIT -> appSettings.howToSubmitResultVideoUrl
            GuideType.RULES -> appSettings.tournamentRulesVideoUrl
          }.trim()

          if (videoUrl.isNotBlank()) {
            try {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }
              context.startActivity(intent)
            } catch (_: Exception) {
              onBannerClick(banner)
            }
          } else {
            onBannerClick(banner)
          }
        },
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Dot indicators
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("home_guide_indicator_dots"),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      banners.indices.forEach { index ->
        val isSelected = pagerState.currentPage == index
        Box(
          modifier = Modifier
            .padding(horizontal = 3.dp)
            .height(5.dp)
            .width(if (isSelected) 20.dp else 5.dp)
            .clip(CircleShape)
            .background(if (isSelected) Gold400 else Slate700),
        )
      }
    }
  }
}

@Composable
private fun GuideBannerCard(
  banner: GuideBannerItem,
  appSettings: AppSettingsEntity,
  onClick: () -> Unit,
) {
  val bannerUrl = when (banner.guideType) {
    GuideType.DEPOSIT -> appSettings.depositBannerImageUrl
    GuideType.MATCH_JOIN -> appSettings.matchJoinBannerImageUrl
    GuideType.RESULT_SUBMIT -> appSettings.resultSubmitBannerImageUrl
    GuideType.RULES -> appSettings.rulesBannerImageUrl
  }.trim()

  val decodedImageBitmap: ImageBitmap? = remember(bannerUrl) {
    if (bannerUrl.isNotBlank() && (bannerUrl.startsWith("data:image") || (bannerUrl.length > 80 && !bannerUrl.startsWith("http")))) {
      try {
        val payload = if (bannerUrl.contains(",")) bannerUrl.substringAfter(",") else bannerUrl
        val cleanBytes = Base64.decode(payload.trim(), Base64.DEFAULT)
        BitmapFactory.decodeByteArray(cleanBytes, 0, cleanBytes.size)?.asImageBitmap()
      } catch (e: Exception) {
        null
      }
    } else {
      null
    }
  }

  val hasCustomBanner = decodedImageBitmap != null || (bannerUrl.isNotBlank() && (bannerUrl.startsWith("http://") || bannerUrl.startsWith("https://")))

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = Color.Transparent,
    border = BorderStroke(
      1.dp,
      Brush.horizontalGradient(
        listOf(
          banner.accentColor.copy(alpha = 0.55f),
          banner.secondaryColor.copy(alpha = 0.35f),
        )
      )
    ),
    modifier = Modifier
      .fillMaxWidth()
      .fillMaxHeight()
      .clip(RoundedCornerShape(16.dp))
      .clickable { onClick() }
      .testTag("guide_banner_${banner.id}"),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      if (hasCustomBanner) {
        // FULL-BLEED CUSTOM BANNER BACKGROUND (CLEAN - NO OVERLAYS)
        if (decodedImageBitmap != null) {
          Image(
            bitmap = decodedImageBitmap,
            contentDescription = banner.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          AsyncImage(
            model = bannerUrl,
            contentDescription = banner.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }
      } else {
        // ORIGINAL SPLIT LAYOUT FALLBACK
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.linearGradient(
                colors = listOf(
                  Color(0xFF0F172A),
                  Color(0xFF1E2640),
                  Color(0xFF0F172A),
                )
              )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
              verticalArrangement = Arrangement.SpaceBetween,
            ) {
              Column {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = banner.accentColor.copy(alpha = 0.15f),
                  border = BorderStroke(1.dp, banner.accentColor.copy(alpha = 0.4f)),
                ) {
                  Text(
                    text = banner.badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Black,
                      fontSize = 9.sp,
                      letterSpacing = 0.8.sp,
                    ),
                    color = banner.accentColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                  text = banner.title,
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                  ),
                  color = Color.White,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                  text = banner.description,
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                  ),
                  color = Slate300,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }

              Surface(
                shape = RoundedCornerShape(20.dp),
                color = banner.accentColor,
                modifier = Modifier
                  .clickable { onClick() }
                  .testTag("banner_cta_${banner.id}"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = banner.ctaText,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Black,
                      fontSize = 11.sp,
                    ),
                    color = Slate950,
                  )
                }
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Large Hero Artwork Container
            Box(
              modifier = Modifier
                .width(120.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(
                  Brush.radialGradient(
                    colors = listOf(
                      banner.accentColor.copy(alpha = 0.3f),
                      banner.secondaryColor.copy(alpha = 0.1f),
                      Color(0xFF0B1120).copy(alpha = 0.8f),
                    )
                  )
                )
                .border(
                  BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                      listOf(
                        banner.accentColor.copy(alpha = 0.6f),
                        banner.secondaryColor.copy(alpha = 0.2f),
                      )
                    )
                  ),
                  RoundedCornerShape(12.dp),
                ),
              contentAlignment = Alignment.Center,
            ) {
              GuideCard3DArt(
                guideType = banner.guideType,
                accentColor = banner.accentColor,
                secondaryColor = banner.secondaryColor,
                size = 100.dp,
              )
            }
          }
        }
      }
    }
  }
}

// -----------------------------------------------------------------------
// 2.6 AUTO GAMES SECTION HEADING
// -----------------------------------------------------------------------

@Composable
private fun AutoGamesSectionHeading(modifier: Modifier = Modifier) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    HorizontalDivider(
      modifier = Modifier.weight(1f),
      color = Slate800,
      thickness = 1.dp
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 12.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Star,
        contentDescription = null,
        tint = Gold400,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "★ অটো গেম ★",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Black,
          fontSize = 14.sp,
          letterSpacing = 2.sp
        ),
        color = Gold400
      )
      Spacer(modifier = Modifier.width(6.dp))
      Icon(
        imageVector = Icons.Default.Star,
        contentDescription = null,
        tint = Gold400,
        modifier = Modifier.size(16.dp)
      )
    }
    HorizontalDivider(
      modifier = Modifier.weight(1f),
      color = Slate800,
      thickness = 1.dp
    )
  }
}

// -----------------------------------------------------------------------
// 3. TOURNAMENT SECTION HEADING
// -----------------------------------------------------------------------

@Composable
private fun TournamentSectionHeading(modifier: Modifier = Modifier) {
  val strings = LocalAppStrings.current
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    HorizontalDivider(
      modifier = Modifier.weight(1f),
      color = Slate800,
      thickness = 1.dp,
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 12.dp),
    ) {
      Icon(
        imageVector = Icons.Default.Star,
        contentDescription = null,
        tint = Gold400,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = strings.tournamentsHeading,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = 2.sp,
          fontSize = 14.sp,
        ),
        color = Gold400,
      )
      Spacer(modifier = Modifier.width(6.dp))
      Icon(
        imageVector = Icons.Default.Star,
        contentDescription = null,
        tint = Gold400,
        modifier = Modifier.size(16.dp),
      )
    }
    HorizontalDivider(
      modifier = Modifier.weight(1f),
      color = Slate800,
      thickness = 1.dp,
    )
  }
}

// -----------------------------------------------------------------------
// 4. ALL MATCHES QUICK CARD
// -----------------------------------------------------------------------

@Composable
private fun AllMatchesQuickCard(
  onAllMatchesClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Slate900,
    border = BorderStroke(1.dp, Gold400.copy(alpha = 0.45f)),
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .clip(RoundedCornerShape(14.dp))
      .clickable { onAllMatchesClick() }
      .testTag("home_all_matches_quick_card"),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.horizontalGradient(
            listOf(
              Color(0xFF161D32),
              Color(0xFF1E2744),
              Color(0xFF161D32),
            )
          )
        )
        .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(
                Brush.linearGradient(
                  listOf(Amber700.copy(alpha = 0.35f), Gold500.copy(alpha = 0.15f))
                )
              )
              .border(1.dp, Gold400.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = strings.allMatchesQuickTitle,
              tint = Gold400,
              modifier = Modifier.size(24.dp),
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = strings.allMatchesQuickTitle,
              style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                fontSize = 14.sp,
              ),
              color = Color.White,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = strings.allMatchesQuickSubtitle,
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
              ),
              color = Slate300,
            )
          }
        }

        Icon(
          imageVector = Icons.Default.ChevronRight,
          contentDescription = strings.allMatchesQuickTitle,
          tint = Gold400,
          modifier = Modifier.size(24.dp),
        )
      }
    }
  }
}

// -----------------------------------------------------------------------
// 5. GAME FILTER
// -----------------------------------------------------------------------

@Composable
private fun GameFilterSection(
  selectedFilter: GameType?,
  onFilterSelect: (GameType?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    val filterItems = listOf(
      Triple(null, strings.filterAllGames, Icons.Default.List),
      Triple(GameType.LUDO, strings.filterLudo, Icons.Default.Build),
      Triple(GameType.CARROM, strings.filterCarrom, Icons.Default.CheckCircle),
    )

    filterItems.forEach { (type, label, icon) ->
      val isSelected = selectedFilter == type
      val accentColor = when (type) {
        GameType.LUDO -> Gold400
        GameType.CARROM -> NeonCyan
        null -> Indigo400
      }

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color.Transparent else MidnightNavyCard,
        border = BorderStroke(
          0.8.dp,
          if (isSelected) Gold400 else MidnightNavyBorder,
        ),
        shadowElevation = if (isSelected) 4.dp else 2.dp,
        modifier = Modifier
          .weight(1f)
          .height(44.dp)
          .clip(RoundedCornerShape(14.dp))
          .then(
            if (isSelected) {
              Modifier.background(ElectricAmberGradient)
            } else {
              Modifier
            }
          )
          .clickable { onFilterSelect(type) }
          .testTag("filter_chip_${type?.name ?: "ALL"}"),
      ) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          // 3D Isometric Tactile Icon Container with Radial Glow
          Box(
            modifier = Modifier
              .size(24.dp)
              .clip(RoundedCornerShape(6.dp))
              .then(
                if (isSelected) {
                  Modifier.background(Slate950.copy(alpha = 0.25f))
                } else {
                  Modifier.background(
                    Brush.radialGradient(
                      colors = listOf(
                        accentColor.copy(alpha = 0.3f),
                        Color.Transparent,
                      )
                    )
                  )
                }
              )
              .border(
                0.8.dp,
                if (isSelected) Slate950.copy(alpha = 0.35f) else accentColor.copy(alpha = 0.5f),
                RoundedCornerShape(6.dp),
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = if (isSelected) Slate950 else accentColor,
              modifier = Modifier.size(15.dp),
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
              fontSize = 11.5.sp,
              letterSpacing = 0.3.sp,
            ),
            color = if (isSelected) Slate950 else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

// -----------------------------------------------------------------------
// 6. AVAILABLE MATCHES HEADER
// -----------------------------------------------------------------------

@Composable
private fun AvailableMatchesHeader(
  onSeeAllClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = strings.availableMatchesTitle,
      style = MaterialTheme.typography.titleSmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
      ),
      color = Color.White,
    )

    Text(
      text = strings.viewAll,
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
      ),
      color = Gold400,
      modifier = Modifier
        .clickable { onSeeAllClick() }
        .padding(vertical = 4.dp, horizontal = 4.dp)
        .testTag("home_see_all_matches"),
    )
  }
}

// -----------------------------------------------------------------------
// GUIDE DETAIL DIALOG
// -----------------------------------------------------------------------

@Composable
private fun GuideDetailDialog(
  banner: GuideBannerItem,
  appSettings: AppSettingsEntity? = null,
  onDismiss: () -> Unit,
  onAction: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val context = LocalContext.current
  val videoUrl = banner.videoUrlOverride?.trim().takeUnless { it.isNullOrBlank() }
    ?: when (banner.guideType) {
      GuideType.MATCH_JOIN -> appSettings?.howToJoinVideoUrl.orEmpty()
      GuideType.DEPOSIT -> appSettings?.howToDepositVideoUrl.orEmpty()
      GuideType.RESULT_SUBMIT -> appSettings?.howToSubmitResultVideoUrl.orEmpty()
      GuideType.RULES -> appSettings?.tournamentRulesVideoUrl.orEmpty()
    }.trim()

  val (actionBtnText, guideSteps) = when (banner.guideType) {
    GuideType.DEPOSIT -> Pair(
      if (strings is com.example.core.i18n.EnglishStrings) "Go to Deposit Page" else "Deposit পেজে যান",
      if (strings is com.example.core.i18n.EnglishStrings) listOf(
        "1. Go to Wallet page and tap 'Deposit' button.",
        "2. Select bKash or Nagad and Cash In / Send Money to the provided number.",
        "3. Submit the Transaction ID (TrxID) and payment screenshot.",
        "4. Admin will verify and credit your balance within 5-10 minutes.",
      ) else listOf(
        "১. Wallet পেজে গিয়ে 'Deposit' বাটনে ট্যাপ করুন।",
        "২. bKash বা Nagad সিলেক্ট করে প্রদত্ত নাম্বারে Cash In / Send Money করুন।",
        "৩. ট্রানজেকশন আইডি (TrxID) ও স্ক্রিনশট সাবমিট করুন।",
        "৪. অ্যাডমিন ভেরিফাই করে ৫-১০ মিনিটের মধ্যে ব্যালেন্স যুক্ত করবে।",
      )
    )
    GuideType.MATCH_JOIN -> Pair(
      if (strings is com.example.core.i18n.EnglishStrings) "View Matches" else "ম্যাচ তালিকা দেখুন",
      if (strings is com.example.core.i18n.EnglishStrings) listOf(
        "1. Choose your preferred 1v1 match from Home or Matches lobby.",
        "2. Tap 'JOIN NOW', pay the entry fee, and confirm your slot.",
        "3. Game Room Code will appear on screen at the scheduled match time.",
        "4. Paste the room code in Ludo King or Carrom game to battle.",
      ) else listOf(
        "১. হোমপেজ বা Matches পেজ থেকে আপনার পছন্দমতো 1v1 ম্যাচ বেছে নিন।",
        "২. 'JOIN NOW' বাটনে ট্যাপ করে এন্ট্রি ফি পরিশোধ করে স্লট নিশ্চিত করুন।",
        "৩. ম্যাচ শুরুর নির্দিষ্ট সময়ে স্ক্রিনে 'Game Room Code' দেখতে পাবেন।",
        "৪. কোডটি Ludo King বা Carrom গেমে পেস্ট করে ব্যাটল শুরু করুন।",
      )
    )
    GuideType.RESULT_SUBMIT -> Pair(
      if (strings is com.example.core.i18n.EnglishStrings) "Go to Match" else "ম্যাচ পেজে যান",
      if (strings is com.example.core.i18n.EnglishStrings) listOf(
        "1. Take a clear screenshot of the victory screen right after the match.",
        "2. Go to the match and tap 'SUBMIT VICTORY PROOF' button.",
        "3. Upload the victory screenshot and press submit.",
        "4. Once reviewed by admin, prize money is instantly credited to your wallet.",
      ) else listOf(
        "১. খেলা শেষ হওয়া মাত্র বিজয়ী স্ক্রিনের পরিষ্কার স্ক্রিনশট নিন।",
        "২. ম্যাচে গিয়ে 'SUBMIT VICTORY PROOF' বাটনে ট্যাপ করুন।",
        "৩. স্ক্রিনশট আপলোড করে সাবমিট বাটনে চাপুন।",
        "৪. রিভিউ সম্পন্ন হলে প্রাইজমানি স্বয়ংক্রিয়ভাবে ওয়ালেটে যুক্ত হবে।",
      )
    )
    GuideType.RULES -> Pair(
      if (strings is com.example.core.i18n.EnglishStrings) "Read Full Rules" else "সম্পূর্ণ নিয়মাবলী পড়ুন",
      if (strings is com.example.core.i18n.EnglishStrings) listOf(
        "1. Any hacks, cheats, or fake screenshots are strictly prohibited.",
        "2. Players must join the game within 5 minutes of room code release.",
        "3. Admin decision is final in case of any disputes.",
        "4. For any issues, contact live support immediately.",
      ) else listOf(
        "১. কোনো প্রকার হ্যাক, চিট বা ফেক স্ক্রিনশট সম্পূর্ণ নিষিদ্ধ।",
        "২. রুম কোড দেওয়ার ৫ মিনিটের মধ্যে খেলায় অংশ নিতে হবে।",
        "৩. কোনো বিতর্ক দেখা দিলে অ্যাডমিনের সিদ্ধান্তই চূড়ান্ত।",
        "৪. কোনো সমস্যায় তাৎক্ষণিক লাইভ সাপোর্টে যোগাযোগ করুন।",
      )
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = banner.icon,
          contentDescription = null,
          tint = banner.accentColor,
          modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = banner.title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
        )
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        guideSteps.forEach { step ->
          Text(
            text = step,
            style = MaterialTheme.typography.bodyMedium,
            color = Slate200,
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (videoUrl.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF16203A),
            border = BorderStroke(
              1.2.dp,
              Brush.horizontalGradient(
                listOf(
                  Color(0xFFFF334B),
                  Gold400,
                )
              )
            ),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                try {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                  }
                  context.startActivity(intent)
                } catch (_: Exception) {
                  // Fallback safe
                }
              }
              .testTag("guide_tutorial_video_button"),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = null,
                tint = Color(0xFFFF334B),
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (strings is com.example.core.i18n.EnglishStrings) "▶ Watch Tutorial Video" else "▶ ভিডিও টিউটোরিয়াল দেখুন",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.3.sp,
                ),
                color = Color.White,
              )
            }
          }
        } else {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Slate850,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = strings.officialVideoNotice,
              style = MaterialTheme.typography.labelSmall,
              color = Gold400,
              modifier = Modifier.padding(8.dp),
            )
          }
        }
      }
    },
    confirmButton = {
      TournamentButton(
        text = actionBtnText,
        onClick = onAction,
        modifier = Modifier.height(38.dp),
      )
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(strings.close, color = Slate400)
      }
    },
    containerColor = NavyCard,
    tonalElevation = 6.dp,
  )
}

fun playRoomCodeNotificationSoundAndVibrate(context: Context) {
  RoomCodeNotificationManager.playRoomCodeAlert(context)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnnouncementBar(
  message: String,
  modifier: Modifier = Modifier,
) {
  if (message.isBlank()) return

  var showNoticeDialog by remember { mutableStateOf(false) }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable { showNoticeDialog = true },
    shape = RoundedCornerShape(10.dp),
    color = NavyCard,
    border = BorderStroke(1.dp, Gold400.copy(alpha = 0.6f)),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = Icons.Default.Notifications,
        contentDescription = "Announcement",
        tint = Gold400,
        modifier = Modifier.size(20.dp),
      )
      Text(
        text = "📢  $message",
        style = MaterialTheme.typography.bodySmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
        ),
        color = Color.White,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
          .weight(1f)
          .basicMarquee(iterations = Int.MAX_VALUE)
      )
    }
  }

  if (showNoticeDialog) {
    NoticeDialog(
      message = message,
      onDismiss = { showNoticeDialog = false }
    )
  }
}

@Composable
fun NoticeDialog(
  message: String,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth(0.92f)
      .clip(RoundedCornerShape(24.dp))
      .border(1.dp, Gold400.copy(alpha = 0.7f), RoundedCornerShape(24.dp)),
    containerColor = NavySurface,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "📢 জরুরি বিজ্ঞপ্তি",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Black,
            color = Gold400
          )
        )
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = Gold400)
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 400.dp)
          .verticalScroll(rememberScrollState())
          .padding(vertical = 8.dp)
      ) {
        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium.copy(
            color = Color.White,
            lineHeight = 22.sp
          )
        )
      }
    },
    confirmButton = {
      TournamentButton(
        text = "ঠিক আছে",
        onClick = onDismiss,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      )
    }
  )
}

// -----------------------------------------------------------------------
// BOARD GAMES SECTION (AUTO LUDO & CARROM POOL)
// -----------------------------------------------------------------------

@Composable
private fun LobbyBoardGamesSection(
  onLudoClick: () -> Unit,
  onCarromClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    BoardGameCard(
      title = "অটো লুডু",
      subtitle = "ইনস্ট্যান্ট বোর্ড গেম",
      badge = "১v১ ফাস্ট",
      badgeColor = Emerald400,
      iconType = 0, // Dice
      modifier = Modifier.weight(1f),
      onClick = onLudoClick
    )
    BoardGameCard(
      title = "ক্যারম পুল",
      subtitle = "বোর্ড স্ট্রাইক ব্যাটেল",
      badge = "নতুন",
      badgeColor = Gold400,
      iconType = 1, // Striker/Coin
      modifier = Modifier.weight(1f),
      onClick = onCarromClick
    )
  }
}

@Composable
private fun BoardGameCard(
  title: String,
  subtitle: String,
  badge: String,
  badgeColor: Color,
  iconType: Int, // 0: Ludo Dice, 1: Carrom Striker
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "BoardGame")
  
  // Animation for Dice (Rotation) or Striker (Pulse/Glow)
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(10000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotation"
  )
  
  val pulse by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse"
  )

  Surface(
    modifier = modifier
      .height(126.dp)
      .clip(RoundedCornerShape(16.dp))
      .clickable { onClick() },
    shape = RoundedCornerShape(16.dp),
    color = Color.Transparent,
    border = BorderStroke(1.2.dp, Gold400.copy(alpha = 0.45f))
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.horizontalGradient(
            listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
          )
        )
    ) {
      // Content Row
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          // Badge
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = badgeColor.copy(alpha = 0.2f),
            border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.5f))
          ) {
            Text(
              text = badge,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Black
              ),
              color = badgeColor
            )
          }
          
          Spacer(modifier = Modifier.height(12.dp))
          
          Column {
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
              ),
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = subtitle,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                color = Slate400
              ),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
        
        // Graphic Section
        Box(
          modifier = Modifier
            .size(70.dp)
            .padding(4.dp),
          contentAlignment = Alignment.Center
        ) {
          if (iconType == 0) {
            // Ludo Dice Graphic
            Box(
              modifier = Modifier
                .size(48.dp)
                .rotate(rotation)
                .background(
                  Brush.radialGradient(
                    listOf(Color(0xFFEF4444), Color(0xFF991B1B))
                  ),
                  RoundedCornerShape(10.dp)
                )
                .border(1.2.dp, Gold400.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                .padding(8.dp)
            ) {
              // Dice Dots (Simplified 5)
              Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Gold400).align(Alignment.TopStart))
              Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Gold400).align(Alignment.TopEnd))
              Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Gold400).align(Alignment.Center))
              Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Gold400).align(Alignment.BottomStart))
              Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Gold400).align(Alignment.BottomEnd))
            }
          } else {
            // Carrom Striker/Coin Graphic
            Box(contentAlignment = Alignment.Center) {
               // Ambient Glow
               Canvas(modifier = Modifier.size(60.dp)) {
                 drawCircle(
                   brush = Brush.radialGradient(
                     listOf(Gold400.copy(alpha = 0.15f), Color.Transparent)
                   )
                 )
               }
               // Striker
               Surface(
                 modifier = Modifier.size(44.dp * pulse),
                 shape = CircleShape,
                 color = Color(0xFF1E293B),
                 border = BorderStroke(2.5.dp, Gold400),
                 shadowElevation = 4.dp
               ) {
                 Box(contentAlignment = Alignment.Center) {
                   Box(
                     modifier = Modifier
                       .size(24.dp)
                       .border(1.dp, Gold400.copy(alpha = 0.4f), CircleShape)
                   )
                   Icon(
                     Icons.Default.Adjust,
                     contentDescription = null,
                     tint = Gold400,
                     modifier = Modifier.size(16.dp)
                   )
                 }
               }
            }
          }
        }
      }
    }
  }
}
