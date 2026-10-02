package com.example.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.channels.awaitClose
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.core.di.AppContainer
import com.example.core.error.Resource
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalAppStrings
import androidx.compose.ui.platform.LocalContext
import com.example.core.notification.RoomCodeNotificationManager
import com.example.core.notification.UserNotificationManager
import com.example.data.repository.LocalDataStore
import com.google.firebase.auth.FirebaseAuth
import com.example.domain.model.NotificationEntity
import com.example.domain.model.AppSettingsEntity
import com.example.ui.components.ForceUpdateDialog
import com.example.BuildConfig
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import com.example.ui.admin.AdminScreen
import com.example.ui.admin.AdminViewModel
import com.example.ui.auth.*
import com.example.ui.history.HistoryScreen
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.match.MatchDetailScreen
import com.example.ui.match.MatchDetailViewModel
import com.example.ui.matches.MatchesScreen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.profile.ProfileViewModel
import com.example.ui.rules.RulesScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.support.AiSupportScreen
import com.example.ui.support.SupportScreen
import com.example.ui.theme.*
import com.example.ui.wallet.*

object Destinations {
  const val SPLASH = "splash"
  const val LOGIN = "login"
  const val REGISTER = "register"
  const val FORGOT_PASSWORD = "forgot_password"
  const val OTP = "otp/{phone}"
  const val RESET_PASSWORD = "reset_password/{phone}/{otp}"

  const val HOME = "home"
  const val WALLET = "wallet"
  const val MATCHES = "matches"
  const val NOTIFICATIONS = "notifications"
  const val PROFILE = "profile"

  const val DEPOSIT = "deposit"
  const val WITHDRAW = "withdraw"
  const val TRANSACTIONS = "transactions"
  const val MATCH_DETAIL = "match_detail/{matchId}"
  const val HISTORY = "history"
  const val SUPPORT = "support"
  const val RULES = "rules"
  const val ADMIN = "admin"
  const val REFER_EARN = "refer_earn"
  const val AI_SUPPORT = "ai_support"
  const val SPIN_WHEEL = "spin_wheel"
  const val DICE_ROLL = "dice_roll"
  const val HEAD_TAIL = "head_tail"
  const val SCRATCH_CARD = "scratch_card"

  fun otp(phone: String) = "otp/$phone"
  fun resetPassword(phone: String, otp: String) = "reset_password/$phone/$otp"
  fun matchDetail(matchId: String) = "match_detail/$matchId"
}

data class BottomNavigationItem(
  val route: String,
  val label: String,
  val icon: ImageVector,
)

val PrimaryBottomNavItems = listOf(
  BottomNavigationItem(Destinations.HOME, "Home", Icons.Default.Home),
  BottomNavigationItem(Destinations.WALLET, "Wallet", Icons.Default.AccountBalanceWallet),
  BottomNavigationItem(Destinations.MATCHES, "Matches", Icons.Default.SportsEsports),
  BottomNavigationItem(Destinations.HISTORY, "History", Icons.Default.History),
  BottomNavigationItem(Destinations.PROFILE, "Profile", Icons.Default.Person),
)

@Composable
fun AppNavigation(
  navController: NavHostController,
  container: AppContainer,
  currentLanguage: AppLanguage = AppLanguage.BN,
  onLanguageChange: (AppLanguage) -> Unit = {},
) {
  val strings = LocalAppStrings.current
  val startDestination = Destinations.SPLASH

  val bottomNavItems = remember(strings) {
    listOf(
      BottomNavigationItem(Destinations.HOME, strings.navHome, Icons.Default.Home),
      BottomNavigationItem(Destinations.WALLET, strings.navWallet, Icons.Default.AccountBalanceWallet),
      BottomNavigationItem(Destinations.MATCHES, strings.navMatches, Icons.Default.SportsEsports),
      BottomNavigationItem(Destinations.HISTORY, strings.navHistory, Icons.Default.History),
      BottomNavigationItem(Destinations.PROFILE, strings.navProfile, Icons.Default.Person),
    )
  }

  // Observe App Settings for global configurations (Force Update, Maintenance Mode)
  val appSettingsRes by remember(container.settingsRepository) {
    container.settingsRepository.getAppSettings()
  }.collectAsState(initial = Resource.Loading)

  val currentSettings = (appSettingsRes as? Resource.Success)?.data
  val isUpdateRequired = remember(currentSettings) {
    val remoteVersionCode = currentSettings?.appVersionCode ?: 0
    remoteVersionCode > BuildConfig.VERSION_CODE
  }

  // 1. FORCE UPDATE OVERLAY: Highest priority, blocks entire app interaction
  if (isUpdateRequired) {
    ForceUpdateDialog(
      remoteVersionName = currentSettings?.appVersionCode?.let { "v$it" } ?: "Latest",
      updateUrl = "https://adturnamet.web.app"
    )
  }

  val authViewModel: AuthViewModel = viewModel(
    factory = remember(container.authRepository) { AuthViewModel.Factory(container.authRepository) }
  )

  val currentUser by authViewModel.currentUser.collectAsState()

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  val context = LocalContext.current

  // Authority: Use AtomicReference to strictly detect real UID changes and avoid recomposition loops
  val initialAuthUid = remember { 
    try { FirebaseAuth.getInstance().currentUser?.uid ?: "" } catch (_: Exception) { "" }
  }
  val lastKnownUid = remember { java.util.concurrent.atomic.AtomicReference(initialAuthUid) }
  var currentAuthUid by remember { mutableStateOf(initialAuthUid) }

  // Authoritative Auth Lifecycle Listener: triggers strictly on auth state changes
  DisposableEffect(Unit) {
    val auth = try { FirebaseAuth.getInstance() } catch (_: Exception) { null }
    if (auth == null) return@DisposableEffect onDispose {}
    
    val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
      val newUid = firebaseAuth.currentUser?.uid ?: ""
      val prevUid = lastKnownUid.getAndSet(newUid)
      if (prevUid != newUid) {
        currentAuthUid = newUid
        if (newUid.isBlank()) {
          // Explicit Logout: Scrub local cache and stop listeners
          authViewModel.clearSuccessFlags()
          LocalDataStore.clearUserData()
          UserNotificationManager.stopListening()

          // Force immediate navigation to Login and clear backstack
          val currentRoute = navController.currentDestination?.route
          val authRoutes = listOf(Destinations.LOGIN, Destinations.REGISTER, Destinations.SPLASH, Destinations.FORGOT_PASSWORD)
          if (currentRoute !in authRoutes) {
            navController.navigate(Destinations.LOGIN) {
              popUpTo(0) { inclusive = true }
              launchSingleTop = true
            }
          }
        }
      }
    }
    auth.addAuthStateListener(authListener)
    onDispose {
      auth.removeAuthStateListener(authListener)
    }
  }

  // Authoritative dynamic userId
  val activeUserId = currentAuthUid
  val isUserAuthenticated = activeUserId.isNotBlank()

  // Trigger Auth based side effects without duplicate calls
  LaunchedEffect(activeUserId) {
    if (isUserAuthenticated) {
      // Start real-time system tray notification listener with REAL UID
      runCatching {
        UserNotificationManager.startListening(
          context = context.applicationContext,
          userId = activeUserId,
          notificationRepository = container.notificationRepository,
          scope = this
        )
      }.onFailure { e ->
        com.example.core.logging.AppLogger.e("AppNavigation", "Failed to start UserNotificationManager: ${e.message}")
      }

      container.matchRepository.getMyJoinedMatches(activeUserId)
        .collect { res ->
          if (res is Resource.Success) {
            val joinedMatches = res.data
            val joinedIds = joinedMatches.map { it.matchId }.toSet()
            RoomCodeNotificationManager.processMatches(
              userId = activeUserId,
              joinedMatchIds = joinedIds,
              matches = joinedMatches,
              notificationRepository = container.notificationRepository,
              context = context,
              triggerAlert = true,
            )
          }
        }
    } else {
      // User logged out or session invalid - stop listeners
      UserNotificationManager.stopListening()
    }
  }

  // Hoist unread notification state to AppNavigation level so it survives
  // Home -> Notifications -> Home navigation without dropping to 0 or flickering
  val initialNotifications: List<NotificationEntity> = remember(activeUserId) {
    if (!isUserAuthenticated) emptyList() else LocalDataStore.localUserNotifications[activeUserId]?.toList()?.take(50) ?: emptyList()
  }
  val notificationsResource by remember(activeUserId, container.notificationRepository) {
    if (!isUserAuthenticated) kotlinx.coroutines.flow.flowOf(Resource.Success(emptyList()))
    else container.notificationRepository.getNotifications(activeUserId, 50)
  }.collectAsState(initial = Resource.Success(initialNotifications))

  val unreadNotificationsCount = remember(notificationsResource) {
    (notificationsResource as? Resource.Success)?.data?.count { !it.isRead } ?: 0
  }

  val primaryRoutes = remember {
    setOf(
      Destinations.HOME,
      Destinations.WALLET,
      Destinations.MATCHES,
      Destinations.HISTORY,
      Destinations.PROFILE,
    )
  }
  val isBottomNavVisible = currentRoute in primaryRoutes && isUserAuthenticated

  Scaffold(
    containerColor = DeepNavyBg,
    bottomBar = {
      if (isBottomNavVisible) {
        Surface(
          color = NavySurface,
          border = BorderStroke(1.dp, NavyCardBorder),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("main_bottom_navigation"),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .navigationBarsPadding()
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            bottomNavItems.forEach { item ->
              val isSelected = currentRoute == item.route
              GamingBottomNavItem(
                item = item,
                isSelected = isSelected,
                onClick = {
                  if (currentRoute != item.route) {
                    navController.navigate(item.route) {
                      popUpTo(Destinations.HOME) {
                        saveState = true
                      }
                      launchSingleTop = true
                      restoreState = true
                    }
                  }
                },
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }
    },
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = startDestination,
      enterTransition = { fadeIn(animationSpec = tween(180)) },
      exitTransition = { fadeOut(animationSpec = tween(180)) },
      popEnterTransition = { fadeIn(animationSpec = tween(180)) },
      popExitTransition = { fadeOut(animationSpec = tween(180)) },
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      // 0. BRANDED SPLASH SCREEN
      composable(Destinations.SPLASH) {
        SplashScreen(
          onSplashFinished = {
            val targetDestination = if (container.authRepository.isUserSignedIn()) {
              Destinations.HOME
            } else {
              Destinations.LOGIN
            }
            navController.navigate(targetDestination) {
              popUpTo(Destinations.SPLASH) { inclusive = true }
            }
          },
        )
      }

      // 1. AUTHENTICATION: LOGIN
      composable(Destinations.LOGIN) {
        LoginScreen(
          viewModel = authViewModel,
          onLoginSuccess = {
            if (FirebaseAuth.getInstance().currentUser != null) {
              navController.navigate(Destinations.HOME) {
                popUpTo(Destinations.LOGIN) { inclusive = true }
              }
            }
          },
          onNavigateToRegister = { navController.navigate(Destinations.REGISTER) },
          onNavigateToForgotPassword = { navController.navigate(Destinations.FORGOT_PASSWORD) },
        )
      }

      // 2. AUTHENTICATION: REGISTER
      composable(Destinations.REGISTER) {
        RegisterScreen(
          viewModel = authViewModel,
          onRegisterSuccess = {
            if (FirebaseAuth.getInstance().currentUser != null) {
              navController.navigate(Destinations.HOME) {
                popUpTo(Destinations.REGISTER) { inclusive = true }
              }
            }
          },
          onNavigateToLogin = { navController.popBackStack() },
        )
      }

      // 3. AUTHENTICATION: FORGOT PASSWORD
      composable(Destinations.FORGOT_PASSWORD) {
        ForgotPasswordScreen(
          viewModel = authViewModel,
          onNavigateToOtp = { phone -> navController.navigate(Destinations.otp(phone)) },
          onNavigateBack = { navController.popBackStack() },
        )
      }

      // 4. AUTHENTICATION: OTP VERIFICATION
      composable(
        route = Destinations.OTP,
        arguments = listOf(navArgument("phone") { type = NavType.StringType }),
      ) { backStackEntry ->
        val phone = backStackEntry.arguments?.getString("phone") ?: ""
        OtpScreen(
          phoneNumber = phone,
          viewModel = authViewModel,
          onOtpVerified = { verifiedPhone, otp ->
            navController.navigate(Destinations.resetPassword(verifiedPhone, otp))
          },
          onNavigateBack = { navController.popBackStack() },
        )
      }

      // 5. AUTHENTICATION: RESET PASSWORD
      composable(
        route = Destinations.RESET_PASSWORD,
        arguments = listOf(
          navArgument("phone") { type = NavType.StringType },
          navArgument("otp") { type = NavType.StringType },
        ),
      ) { backStackEntry ->
        val phone = backStackEntry.arguments?.getString("phone") ?: ""
        val otp = backStackEntry.arguments?.getString("otp") ?: ""
        ResetPasswordScreen(
          phoneNumber = phone,
          otp = otp,
          viewModel = authViewModel,
          onResetSuccess = {
            navController.navigate(Destinations.LOGIN) {
              popUpTo(Destinations.LOGIN) { inclusive = true }
            }
          },
          onNavigateBack = { navController.popBackStack() },
        )
      }

      // 6. MAIN HUB: HOME SCREEN
      composable(Destinations.HOME) { backStackEntry ->
        val homeViewModel: HomeViewModel = viewModel(
          viewModelStoreOwner = backStackEntry,
          key = "HomeViewModel_$activeUserId",
          factory = remember(activeUserId) {
            HomeViewModel.Factory(container.matchRepository, activeUserId, container.settingsRepository)
          }
        )
        val walletViewModel: WalletViewModel = viewModel(
          viewModelStoreOwner = backStackEntry,
          key = "WalletViewModel_$activeUserId",
          factory = remember(activeUserId) {
            WalletViewModel.Factory(container.walletRepository, activeUserId, authRepository = container.authRepository)
          }
        )
        val walletUiState by walletViewModel.uiState.collectAsState()

        HomeScreen(
          viewModel = homeViewModel,
          currentUser = currentUser,
          wallet = walletUiState.wallet,
          onMatchClick = { matchId -> navController.navigate(Destinations.matchDetail(matchId)) },
          onAllMatchesClick = {
            navController.navigate(Destinations.MATCHES) {
              popUpTo(Destinations.HOME) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          },
          onWalletClick = {
            navController.navigate(Destinations.WALLET) {
              popUpTo(Destinations.HOME) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          },
          onDepositClick = { navController.navigate(Destinations.DEPOSIT) },
          onWithdrawClick = { navController.navigate(Destinations.WITHDRAW) },
          onNotificationClick = { navController.navigate(Destinations.NOTIFICATIONS) },
          onProfileClick = {
            navController.navigate(Destinations.PROFILE) {
              popUpTo(Destinations.HOME) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          },
          onRulesClick = { navController.navigate(Destinations.RULES) },
          onSupportClick = { navController.navigate(Destinations.SUPPORT) },
          onAdminClick = { navController.navigate(Destinations.ADMIN) },
          onReferClick = { navController.navigate(Destinations.REFER_EARN) },
          onHistoryClick = { navController.navigate(Destinations.HISTORY) },
          onSpinWheelClick = { navController.navigate(Destinations.SPIN_WHEEL) },
          onDiceRollClick = { navController.navigate(Destinations.DICE_ROLL) },
          onHeadTailClick = { navController.navigate(Destinations.HEAD_TAIL) },
          onScratchCardClick = { navController.navigate(Destinations.SCRATCH_CARD) },
          unreadNotificationsCount = unreadNotificationsCount,
          notificationRepository = container.notificationRepository,
        )
      }

      // 7. PRIMARY TAB: WALLET SCREEN
      composable(Destinations.WALLET) { backStackEntry ->
        val walletViewModel: WalletViewModel = viewModel(
          viewModelStoreOwner = backStackEntry,
          key = "WalletViewModel_$activeUserId",
          factory = remember(activeUserId) {
            WalletViewModel.Factory(container.walletRepository, activeUserId, authRepository = container.authRepository)
          }
        )
        WalletScreen(
          viewModel = walletViewModel,
          onNavigateBack = { navController.popBackStack() },
          onNavigateToDeposit = { navController.navigate(Destinations.DEPOSIT) },
          onNavigateToWithdraw = { navController.navigate(Destinations.WITHDRAW) },
          onNavigateToTransactions = { navController.navigate(Destinations.TRANSACTIONS) },
          onNotificationClick = { navController.navigate(Destinations.NOTIFICATIONS) },
          onProfileClick = {
            navController.navigate(Destinations.PROFILE) {
              popUpTo(Destinations.HOME) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          },
          unreadNotificationsCount = unreadNotificationsCount,
        )
      }

      // 8. PRIMARY TAB: MATCHES SCREEN
      composable(Destinations.MATCHES) {
        MatchesScreen(
          userId = activeUserId,
          onNavigateToMatchDetails = { matchId -> navController.navigate(Destinations.matchDetail(matchId)) },
          onHistoryClick = { navController.navigate(Destinations.HISTORY) },
          matchRepository = container.matchRepository,
        )
      }

      // 9. PRIMARY TAB: NOTIFICATIONS SCREEN
      composable(Destinations.NOTIFICATIONS) {
        NotificationsScreen(
          userId = activeUserId,
          onNavigateBack = { navController.popBackStack() },
          notificationRepository = container.notificationRepository,
          onNavigateToTarget = { target ->
            when (target.uppercase()) {
              "WALLET" -> navController.navigate(Destinations.WALLET)
              "MATCHES" -> navController.navigate(Destinations.MATCHES)
              "HISTORY" -> navController.navigate(Destinations.HISTORY)
              "HOME" -> navController.navigate(Destinations.HOME)
            }
          },
        )
      }

      // 10. PRIMARY TAB: PROFILE SCREEN
      composable(Destinations.PROFILE) {
        val profileViewModel: ProfileViewModel = viewModel(
          key = "ProfileViewModel_$activeUserId",
          factory = remember(activeUserId) {
            ProfileViewModel.Factory(
              authRepository = container.authRepository,
              settingsRepository = container.settingsRepository,
            )
          }
        )
        ProfileScreen(
          viewModel = profileViewModel,
          user = currentUser,
          currentLanguage = currentLanguage,
          onLanguageChange = onLanguageChange,
          onNavigateBack = { navController.popBackStack() },
          onHistoryClick = { navController.navigate(Destinations.HISTORY) },
          onNavigateToSupport = { navController.navigate(Destinations.SUPPORT) },
          onNavigateToRules = { navController.navigate(Destinations.RULES) },
          onNavigateToAiSupport = { navController.navigate(Destinations.AI_SUPPORT) },
          onSignedOut = {
            // Handled by AuthStateListener in AppNavigation for atomic consistency
          },
        )
      }

      // 11. MATCH DETAIL SCREEN
      composable(
        route = Destinations.MATCH_DETAIL,
        arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
      ) { backStackEntry ->
        val matchId = backStackEntry.arguments?.getString("matchId") ?: ""
        val isAdmin = currentUser?.role == "SUPER_ADMIN" || currentUser?.role == "ADMIN"
        val detailViewModel: MatchDetailViewModel = viewModel(
          key = "${matchId}_$activeUserId",
          factory = MatchDetailViewModel.Factory(
            matchRepository = container.matchRepository,
            resultRepository = container.resultRepository,
            matchId = matchId,
            currentUserId = activeUserId,
            isAdmin = isAdmin,
          )
        )
        MatchDetailScreen(
          viewModel = detailViewModel,
          onNavigateBack = { navController.popBackStack() },
          onViewRoomCode = { /* Handled in-screen */ },
          onSubmitProof = { /* Handled in-screen */ },
        )
      }

      // 12. WALLET: DEPOSIT SCREEN
      composable(Destinations.DEPOSIT) {
        val walletViewModel: WalletViewModel = viewModel(
          key = "WalletViewModel_Deposit_$activeUserId",
          factory = remember(activeUserId) {
            WalletViewModel.Factory(container.walletRepository, activeUserId, container.settingsRepository, authRepository = container.authRepository)
          }
        )
        DepositScreen(
          viewModel = walletViewModel,
          onNavigateBack = { navController.popBackStack() },
          onNotificationClick = { navController.navigate(Destinations.NOTIFICATIONS) },
          onProfileClick = {
            navController.navigate(Destinations.PROFILE) {
              popUpTo(Destinations.HOME) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          },
          unreadNotificationsCount = unreadNotificationsCount,
        )
      }

      // 13. WALLET: WITHDRAW SCREEN
      composable(Destinations.WITHDRAW) {
        val walletViewModel: WalletViewModel = viewModel(
          key = "WalletViewModel_Withdraw_$activeUserId",
          factory = remember(activeUserId) {
            WalletViewModel.Factory(container.walletRepository, activeUserId, authRepository = container.authRepository)
          }
        )
        WithdrawScreen(
          viewModel = walletViewModel,
          currentUser = currentUser,
          onNavigateBack = { navController.popBackStack() },
          onNotificationClick = { navController.navigate(Destinations.NOTIFICATIONS) },
          onProfileClick = {
            navController.navigate(Destinations.PROFILE) {
              popUpTo(Destinations.HOME) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          },
          unreadNotificationsCount = unreadNotificationsCount,
        )
      }

      // 14. WALLET: TRANSACTIONS SCREEN
      composable(Destinations.TRANSACTIONS) {
        val walletViewModel: WalletViewModel = viewModel(
          key = "WalletViewModel_Transactions_$activeUserId",
          factory = remember(activeUserId) {
            WalletViewModel.Factory(container.walletRepository, activeUserId, authRepository = container.authRepository)
          }
        )
        TransactionsScreen(
          viewModel = walletViewModel,
          onNavigateBack = { navController.popBackStack() },
          onNotificationClick = { navController.navigate(Destinations.NOTIFICATIONS) },
          onProfileClick = {
            navController.navigate(Destinations.PROFILE) {
              popUpTo(Destinations.HOME) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          },
          unreadNotificationsCount = unreadNotificationsCount,
        )
      }

      // 15. MATCH HISTORY SCREEN
      composable(Destinations.HISTORY) {
        val historyResource by remember(activeUserId) {
          container.matchRepository.getMatchHistory(activeUserId)
        }.collectAsStateWithLifecycle(initialValue = com.example.core.error.Resource.Loading)

        val historyMatches = (historyResource as? com.example.core.error.Resource.Success)?.data.orEmpty()

        HistoryScreen(
          matches = historyMatches,
          currentUserId = activeUserId,
          onMatchClick = { matchId ->
            navController.navigate(Destinations.matchDetail(matchId))
          },
          onBackClick = {
            navController.popBackStack()
          },
          onNotificationClick = {
            navController.navigate(Destinations.NOTIFICATIONS)
          },
          onProfileClick = {
            navController.navigate(Destinations.PROFILE) {
              popUpTo(Destinations.HOME) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          },
          unreadNotificationsCount = unreadNotificationsCount,
        )
      }

      // 16. SUPPORT SCREEN
      composable(Destinations.SUPPORT) {
        SupportScreen(
          onNavigateBack = { navController.popBackStack() },
        )
      }

      // 17. RULES SCREEN
      composable(Destinations.RULES) {
        RulesScreen(
          onNavigateBack = { navController.popBackStack() },
        )
      }

      // 18. SUPER ADMIN DASHBOARD
      composable(Destinations.ADMIN) {
        val adminViewModel: AdminViewModel = viewModel(
          key = "AdminViewModel_$activeUserId",
          factory = remember(activeUserId) {
            AdminViewModel.Factory(container.adminRepository, activeUserId, container.settingsRepository)
          }
        )
        AdminScreen(
          viewModel = adminViewModel,
          onNavigateBack = { navController.popBackStack() },
        )
      }

      // 19. REFER & EARN SCREEN
      composable(Destinations.REFER_EARN) {
        val referViewModel: com.example.ui.referral.ReferAndEarnViewModel = viewModel(
          key = "ReferAndEarnViewModel_$activeUserId",
          factory = remember(activeUserId) {
            com.example.ui.referral.ReferAndEarnViewModel.Factory(
              authRepository = container.authRepository,
              walletRepository = container.walletRepository
            )
          }
        )
        com.example.ui.referral.ReferAndEarnScreen(
          navController = navController,
          viewModel = referViewModel
        )
      }

      // 20. AI LIVE SUPPORT CHAT
      composable(Destinations.AI_SUPPORT) { backStackEntry ->
        val profileViewModel: ProfileViewModel = viewModel(
          key = "ProfileViewModel_AiSupport_$activeUserId",
          factory = remember(activeUserId) {
            ProfileViewModel.Factory(
              authRepository = container.authRepository,
              settingsRepository = container.settingsRepository,
            )
          }
        )
        val walletViewModel: WalletViewModel = viewModel(
          viewModelStoreOwner = backStackEntry,
          key = "WalletViewModel_AiSupport_$activeUserId",
          factory = remember(activeUserId) {
            WalletViewModel.Factory(container.walletRepository, activeUserId, authRepository = container.authRepository)
          }
        )
        val appSettings by profileViewModel.appSettings.collectAsState()
        val walletUiState by walletViewModel.uiState.collectAsState()

        AiSupportScreen(
          appSettings = appSettings,
          currentUser = currentUser,
          wallet = walletUiState.wallet,
          onNavigateBack = { navController.popBackStack() },
          onNavigateToMatches = {
            navController.navigate(Destinations.MATCHES) {
              popUpTo(Destinations.HOME) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          }
        )
      }

      // 21. LUCKY SPIN MINI GAME
      composable(Destinations.SPIN_WHEEL) {
        val walletViewModel: WalletViewModel = viewModel(
          key = "WalletViewModel_SpinWheel_$activeUserId",
          factory = remember(activeUserId) {
            WalletViewModel.Factory(container.walletRepository, activeUserId, container.settingsRepository, authRepository = container.authRepository)
          }
        )
        val adminRepository = container.adminRepository
        com.example.ui.minigames.SpinWheelScreen(
          walletViewModel = walletViewModel,
          adminRepository = adminRepository,
          onNavigateBack = { navController.popBackStack() }
        )
      }

      // 22. DICE ROLL MINI GAME
      composable(Destinations.DICE_ROLL) {
        val walletViewModel: WalletViewModel = viewModel(
          key = "WalletViewModel_DiceRoll_$activeUserId",
          factory = remember(activeUserId) {
            WalletViewModel.Factory(container.walletRepository, activeUserId, container.settingsRepository, authRepository = container.authRepository)
          }
        )
        com.example.ui.minigames.DiceRollGameScreen(
          walletViewModel = walletViewModel,
          onNavigateBack = { navController.popBackStack() }
        )
      }

      // 22b. HEAD & TAIL MINI GAME
      composable(Destinations.HEAD_TAIL) {
        val walletViewModel: WalletViewModel = viewModel(
          key = "WalletViewModel_HeadTail_$activeUserId",
          factory = remember(activeUserId) {
            WalletViewModel.Factory(container.walletRepository, activeUserId, container.settingsRepository, authRepository = container.authRepository)
          }
        )
        com.example.ui.minigames.HeadTailGameScreen(
          walletViewModel = walletViewModel,
          onNavigateBack = { navController.popBackStack() }
        )
      }

      // 23. SCRATCH CARD MINI GAME
      composable(Destinations.SCRATCH_CARD) {
        val walletViewModel: WalletViewModel = viewModel(
          key = "WalletViewModel_ScratchCard_$activeUserId",
          factory = remember(activeUserId) {
            WalletViewModel.Factory(container.walletRepository, activeUserId, container.settingsRepository, authRepository = container.authRepository)
          }
        )
        com.example.ui.minigames.ScratchCardGameScreen(
          walletViewModel = walletViewModel,
          onNavigateBack = { navController.popBackStack() }
        )
      }
    }
  }
}

/**
 * Polished esports/gaming bottom navigation item with:
 * - Touch-down scale compression (~0.92) and snappy spring bounce-back to 1.0.
 * - Expanding circular energy glow pulse (Gold/Cyan) reminiscent of a Ludo token aura.
 * - 6 lightweight radial micro-sparks on tap.
 * - Persistent subtle ambient gold halo for the active route.
 */
@Composable
private fun GamingBottomNavItem(
  item: BottomNavigationItem,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val coroutineScope = rememberCoroutineScope()
  val scale = remember { Animatable(1f) }
  val pulseProgress = remember { Animatable(0f) }
  val pulseAlpha = remember { Animatable(0f) }

  // Subtle ambient gold glow for active tab
  val ambientGlowAlpha by animateFloatAsState(
    targetValue = if (isSelected) 0.22f else 0f,
    animationSpec = tween(durationMillis = 250),
    label = "ambientGlowAlpha",
  )

  fun triggerGamingTapEffect() {
    coroutineScope.launch {
      // 1. Icon press & bounce: scale down to 0.92, then spring back to 1.0
      launch {
        scale.animateTo(
          targetValue = 0.92f,
          animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing),
        )
        scale.animateTo(
          targetValue = 1.0f,
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
          ),
        )
      }

      // 2. Circular energy pulse & micro-sparks (~350ms)
      launch {
        pulseProgress.snapTo(0f)
        pulseAlpha.snapTo(1f)
        launch {
          pulseProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
          )
        }
        launch {
          pulseAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
          )
        }
      }
    }
  }

  // Trigger effect if item becomes selected via external navigation
  var wasSelected by remember { mutableStateOf(isSelected) }
  LaunchedEffect(isSelected) {
    if (isSelected && !wasSelected) {
      triggerGamingTapEffect()
    }
    wasSelected = isSelected
  }

  val interactionSource = remember { MutableInteractionSource() }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = {
          triggerGamingTapEffect()
          onClick()
        },
      )
      .padding(vertical = 4.dp)
      .testTag(if (item.route == Destinations.HISTORY) "bottom_nav_history" else "nav_item_${item.route}"),
  ) {
    Box(
      modifier = Modifier
        .size(width = 44.dp, height = 32.dp)
        .then(
          if (isSelected) {
            Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(Gold400.copy(alpha = 0.16f))
              .border(1.dp, Gold400.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
          } else {
            Modifier
          }
        ),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        // 1. Subtle steady ambient gold/cyan aura when selected
        if (ambientGlowAlpha > 0.01f) {
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                Gold400.copy(alpha = ambientGlowAlpha),
                Cyan400.copy(alpha = ambientGlowAlpha * 0.35f),
                Color.Transparent,
              ),
              radius = 16.dp.toPx(),
              center = center,
            ),
            radius = 16.dp.toPx(),
            center = center,
          )
        }

        // 2. Gaming tap animation: circular energy pulse + micro-sparks
        val currentAlpha = pulseAlpha.value
        if (currentAlpha > 0.01f) {
          val progress = pulseProgress.value
          val waveRadius = 10.dp.toPx() + (7.dp.toPx() * progress)

          // Radial energy glow pulse
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                Gold400.copy(alpha = 0.40f * currentAlpha),
                Cyan400.copy(alpha = 0.20f * currentAlpha),
                Color.Transparent,
              ),
              radius = waveRadius,
              center = center,
            ),
            radius = waveRadius,
            center = center,
          )

          // Expanding energy ring (Ludo token aura)
          drawCircle(
            color = Gold400.copy(alpha = 0.65f * currentAlpha),
            radius = waveRadius,
            center = center,
            style = Stroke(width = (1.2f * (1f - progress * 0.5f)).dp.toPx()),
          )

          // 6 Micro-sparks (esports spark effect)
          val sparkDistance = 11.dp.toPx() + (6.dp.toPx() * progress)
          val sparkRadius = (1.8f * (1f - progress * 0.4f)).dp.toPx()
          for (i in 0 until 6) {
            val angleRad = Math.toRadians((i * 60.0) + 15.0)
            val sx = center.x + (cos(angleRad) * sparkDistance).toFloat()
            val sy = center.y + (sin(angleRad) * sparkDistance).toFloat()
            val sparkColor = if (i % 2 == 0) {
              Gold400.copy(alpha = 0.85f * currentAlpha)
            } else {
              Cyan400.copy(alpha = 0.75f * currentAlpha)
            }
            drawCircle(
              color = sparkColor,
              radius = sparkRadius,
              center = Offset(sx, sy),
            )
          }
        }
      }

      Icon(
        imageVector = item.icon,
        contentDescription = item.label,
        tint = if (isSelected) Gold400 else Slate200,
        modifier = Modifier
          .size(22.dp)
          .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
          },
      )
    }

    Spacer(modifier = Modifier.height(2.dp))

    Text(
      text = item.label,
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        fontSize = 11.sp,
      ),
      color = if (isSelected) Gold400 else Slate300,
    )
  }
}

fun com.example.domain.repository.MatchRepository.observeMatches(): kotlinx.coroutines.flow.Flow<List<com.example.domain.model.MatchEntity>> =
  kotlinx.coroutines.flow.callbackFlow {
    val ref = com.example.core.firebase.FirebaseManager.getNodeReference(com.example.core.config.FirebaseConfig.NODE_MATCHES)
    if (ref == null) {
      trySend(emptyList())
      awaitClose { }
      return@callbackFlow
    }
    val listener = object : com.google.firebase.database.ValueEventListener {
      override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
        val matches = snapshot.children.mapNotNull { it.getValue(com.example.domain.model.MatchEntity::class.java) }
        trySend(matches)
      }
      override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
        trySend(emptyList())
      }
    }
    ref.addValueEventListener(listener)
    awaitClose { ref.removeEventListener(listener) }
  }

