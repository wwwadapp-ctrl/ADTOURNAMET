package com.example.core.i18n

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
  BN("bn", "Bengali", "বাংলা"),
  EN("en", "English", "English");

  companion object {
    fun fromCode(code: String?): AppLanguage = when (code?.lowercase()) {
      "en" -> EN
      else -> BN // Default is ALWAYS Bengali
    }
  }
}

interface AppStrings {
  // Brand & General
  val appName: String
  val appTagline: String
  val loading: String
  val errorGeneric: String
  val retry: String
  val cancel: String
  val save: String
  val close: String
  val back: String
  val offlineMode: String
  val optional: String
  val locked: String
  val adminPanel: String

  // Navigation
  val navHome: String
  val navWallet: String
  val navMatches: String
  val navHistory: String
  val navProfile: String

  // Auth - Common & Validation
  val phoneNumber: String
  val enterPhoneNumber: String
  val password: String
  val enterPassword: String
  val fullName: String
  val enterFullName: String
  val confirmPassword: String
  val enterConfirmPassword: String
  val invalidPhoneError: String
  val passwordMinLengthError: String
  val passwordsDoNotMatchError: String
  val nameRequiredError: String
  val otpRequiredError: String

  // Auth - Login
  val loginTitle: String
  val loginSubtitle: String
  val loginButton: String
  val forgotPasswordLink: String
  val noAccountRegister: String
  val playerSignIn: String
  val playerSignInSubtitle: String
  val signInButton: String
  val noAccountPrompt: String
  val createAccountAction: String
  val togglePassword: String
  val phoneEmptyError: String
  val passwordEmptyError: String
  val invalidCredentialsError: String
  val accountNotFoundError: String

  // Auth - Register
  val registerTitle: String
  val registerSubtitle: String
  val registerButton: String
  val alreadyHaveAccountLogin: String

  // Auth - Forgot Password & OTP
  val forgotPasswordTitle: String
  val forgotPasswordSubtitle: String
  val sendOtpButton: String
  val verifyOtpTitle: String
  val verifyOtpSubtitle: String
  val verifyOtpButton: String
  val resetPasswordTitle: String
  val resetPasswordSubtitle: String
  val resetPasswordButton: String
  val backToLogin: String

  // Home Screen
  val balanceCurrent: String
  val depositAction: String
  val withdrawAction: String
  val quickMatches: String
  val quickWallet: String
  val quickRules: String
  val quickSupport: String
  val featuredBattles: String
  val liveAndUpcoming: String
  val filterAll: String
  val filterLudo: String
  val filterCarrom: String
  val noMatchesAvailable: String
  val noMatchesSubtext: String
  val tournamentsHeading: String
  val allMatchesQuickTitle: String
  val allMatchesQuickSubtitle: String
  val filterAllGames: String
  val availableMatchesTitle: String
  val loadingMatches: String
  val failedToLoadMatches: String
  val noAvailableMatchesTitle: String
  val noAvailableMatchesSubtext: String
  val refreshMatches: String
  fun viewAllMatchesCount(count: Int): String
  val officialVideoNotice: String
  val depositGuideTitle: String
  val depositGuideSub: String
  val matchJoinGuideTitle: String
  val matchJoinGuideSub: String
  val resultGuideTitle: String
  val resultGuideSub: String
  val rulesGuideTitle: String
  val rulesGuideSub: String
  val viewGuideCta: String
  val homeEmptyMatchesTitle: String


  fun homeReferTitle(amount: String): String
  val homeReferSubtitle: String
  val homeReferCta: String
  val homeQuickDepositSub: String
  val homeQuickWithdrawSub: String
  val homeAutoGamesHeading: String
  val homeAutoLudoTitle: String
  val homeAutoLudoSub: String
  val homeAutoLudoBadge: String
  val homeCarromTitle: String
  val homeCarromSub: String
  val homeNewBadge: String
  val homeCarromComingSoon: String
  val homeNoticeTitle: String
  val homeOk: String
  val homeBadgeDeposit: String
  val homeBadgeJoin: String
  val homeBadgeResult: String
  val homeBadgeRules: String


  // Wallet & Financial
  val walletTitle: String
  val totalBalance: String
  val availableToPlay: String
  val lockedInContests: String
  val recentTransactions: String
  val viewAll: String
  val noTransactionsYet: String
  val noTransactionsSubtext: String
  val walletSubtitle: String
  val statDeposited: String
  val statWinnings: String
  val statWithdrawn: String
  val txnPrize: String
  val txnRefund: String
  val txnEntryFee: String
  fun pendingEscrowNotice(amount: String): String
  val walletMainCashLabel: String
  val walletBonusLabel: String
  val walletBonusUsageHint: String
  val walletTxDeposit: String
  val walletTxWithdraw: String
  val walletTxMatchJoin: String
  val walletTxMatchRefund: String
  val walletTxPrize: String
  val walletTxMiniGameTransferIn: String
  val walletTxMiniGameTransferOut: String
  val walletTxAdminAdjustment: String
  val walletTxBonus: String
  val walletTxOther: String
  val walletTxStatusCompleted: String
  val walletTxStatusPending: String
  val walletTxStatusRejected: String
  val walletTxStatusCancelled: String
  val walletTxDescDeposit: String
  val walletTxDescWithdraw: String
  val walletTxDescMatchJoin: String
  val walletTxDescMatchRefund: String
  val walletTxDescPrize: String
  val walletTxDescTransferIn: String
  val walletTxDescTransferOut: String
  val walletTxDescBonus: String
  val walletTxDescDefault: String
  fun walletTxDescMatch(matchId: String): String

  // Deposit Screen
  val depositTitle: String
  val selectPaymentMethod: String
  val paymentMethodBkash: String
  val paymentMethodNagad: String
  val sendMoneyInstructions: String
  val copyNumber: String
  val numberCopied: String
  val depositAmount: String
  val enterDepositAmount: String
  val trxIdLabel: String
  val enterTrxId: String
  val senderMobileNumber: String
  val enterSenderMobile: String
  val screenshotProofLabel: String
  val submitDepositButton: String
  val depositProcessingTime: String
  val depositSecurityWarning: String
  fun minDepositNotice(amount: String): String
  val depositSubmittedTitle: String
  val depositSubmittedDesc: String
  val backToWallet: String
  val addMoneyTitle: String
  val depositSubtitle: String
  val stepSelectMethod: String
  val selectedBadge: String
  val personalBadge: String
  fun sendMoneyLabel(method: String): String
  val copied: String
  val copy: String
  val howToDepositTitle: String
  val stepDepositDetails: String
  val depositRange: String
  val enterAmountLabel: String
  val senderMobileLabel: String
  val paymentScreenshotLabel: String
  val uploadScreenshotTitle: String
  val uploadScreenshotSub: String
  val screenshotAttached: String
  val replace: String
  val remove: String
  val depositSecurityNotice: String
  val depositMinError: String
  val depositMaxError: String
  val invalidMobileError: String
  val invalidTrxIdError: String
  val depositDuplicateError: String
  val depositLimitExceededError: String

  // Withdraw Screen
  val withdrawTitle: String
  val withdrawAmount: String
  val enterWithdrawAmount: String
  val recipientMobileNumber: String
  val enterRecipientMobile: String
  val requestWithdrawalButton: String
  val withdrawProcessingTime: String
  fun availableBalanceNotice(amount: String): String
  fun minWithdrawNotice(amount: String): String

  // Withdraw Screen Extensions
  val withdrawHeaderTitle: String
  val withdrawHeaderSubtitle: String
  val withdrawSelectMethodLabel: String
  val withdrawMethodSelected: String
  val withdrawMethodPersonal: String
  val withdrawAmountLabel: String
  val withdrawAvailableBalanceLabel: String
  fun withdrawLimitNotice(min: String): String
  val withdrawEnterAmountHint: String
  val withdrawEnterAmountPlaceholder: String
  fun withdrawRecipientLabel(method: String): String
  val withdrawUseSavedNumber: String
  fun withdrawRecipientHint(method: String): String
  val withdrawRecipientPlaceholder: String
  fun withdrawVerifiedBadge(method: String): String
  fun withdrawSecurityHint(method: String): String
  val withdrawSummaryTitle: String
  val withdrawSummaryAmount: String
  val withdrawSummaryMethod: String
  val withdrawSummaryNumber: String
  val withdrawSummaryTotal: String
  val withdrawSecurityTitle: String
  val withdrawGuideline1: String
  fun withdrawGuideline2(min: String): String
  val withdrawGuideline3: String
  val withdrawGuideline4: String
  val withdrawGuideline5: String
  val withdrawNowButton: String
  fun withdrawMinError(min: String): String
  val withdrawMaxError: String
  fun withdrawInsufficientError(balance: String): String
  val withdrawInvalidMobileError: String
  val withdrawSuccessTitle: String
  fun withdrawSuccessDesc(amount: String, method: String): String
  val withdrawStatusLabel: String
  val withdrawStatusPending: String
  val withdrawPayoutMethodLabel: String
  val withdrawRecipientAccountLabel: String
  val withdrawRequestedAmountLabel: String
  val withdrawBackToWallet: String
  val txHistoryTitle: String
  val txHistorySubtitle: String
  val txHistoryFilterAll: String
  val txHistoryFilterDeposit: String
  val txHistoryFilterMatch: String
  val txHistoryFilterWinning: String
  val txHistoryFilterWithdraw: String
  val txHistoryEmptyTitle: String
  val txHistoryEmptyDesc: String
  val txHistoryEmptyFilterTitle: String
  val txHistoryEmptyFilterDesc: String
  val txHistoryRefLabel: String
  val txHistoryCopyAction: String
  val txHistoryCopied: String

  // Transaction History Screen
  val transactionHistoryTitle: String
  val filterDeposits: String
  val filterWithdrawals: String
  val statusCompleted: String
  val statusPending: String
  val statusRejected: String
  val statusProcessing: String

  // Matches Lobby
  val tournamentsTitle: String
  val entryFeeLabel: String
  val prizePoolLabel: String
  val playersLabel: String
  val joinNowButton: String
  val matchFull: String
  val matchLive: String

  // Match Details
  val matchDetailTitle: String
  val roomCodeLabel: String
  val copyRoomCodeButton: String
  val roomCodeCopiedNotice: String
  val playersHeader: String
  val waitingForOpponent: String
  val matchRulesHeader: String
  val submitResultHeader: String
  val actionWon: String
  val actionLost: String
  val uploadWinnerScreenshot: String
  val resultWarningNotice: String

  // Match History
  val matchHistoryTitle: String
  val matchHistorySubtitle: String
  val totalPlayed: String
  val totalWon: String
  val winRateLabel: String
  val badgeVictory: String
  val badgeDefeat: String
  val badgeRefunded: String
  val badgePending: String
  val badgeUnderReview: String
  val badgeRejected: String
  val matchCancelled: String
  val cancellationReason: String
  val labelGame: String
  val labelEntry: String
  val labelPrize: String
  val labelRefund: String
  val noMatchHistoryTitle: String
  val noMatchHistorySubtext: String
  val noFilteredMatchesTitle: String
  val noFilteredMatchesSubtext: String

  // Matches Screen Specific
  val matchesHeaderTitle: String
  val matchesHeaderSubtitle: String
  val tabAvailable: String
  val tabMyJoined: String
  val tabUpcoming: String
  val filterLudo1v1: String
  val filterCarrom1v1: String
  val labelFormat: String
  val label1v1Battle: String
  val labelJoined: String
  val labelMatchCompleted: String
  val labelStartsAt: String
  val labelViewResult: String
  val actionJoinNowCta: String
  val emptyAvailableTitleLabel: String
  val emptyAvailableDescLabel: String
  val emptyJoinedTitleLabel: String
  val emptyJoinedDescLabel: String
  val emptyUpcomingTitleLabel: String
  val emptyUpcomingDescLabel: String
  val emptyHistoryTitleLabel: String
  val emptyHistoryDescLabel: String
  val browseAvailableCta: String
  fun loadingTabMatches(tab: String): String

  // Notifications
  val notificationsTitle: String
  val noNotificationsTitle: String
  val noNotificationsSubtext: String

  // Profile
  val profileTitle: String
  val playerLabel: String
  val labelId: String
  val mobileLockedNotice: String
  val careerStatistics: String
  val statMatches: String
  val statMatchesSub: String
  val statWins: String
  val statLosses: String
  val statLossesSub: String
  val statTotalWinnings: String
  val statAllTimeRewards: String
  val statMemberSince: String
  val statVerifiedPlayer: String
  val matchHistoryMenu: String
  val matchHistoryMenuSub: String
  val fairPlayTitle: String
  val languageSettingTitle: String
  val languageSettingSubtitle: String
  val languageBengali: String
  val languageEnglish: String
  val signOutButton: String
  val updateDisplayNameTitle: String
  val enterNewNamePlaceholder: String
  val nameUpdateSuccess: String
  fun userCopied(id: String): String
  fun winRateSubtitle(rate: Int): String
  fun fairPlayText(status: String): String

  // Support
  val supportTitle: String
  val supportWhatsapp: String
  val supportTelegram: String
  val supportFaq: String
  val helpAndSupportTitle: String
  val helpAndSupportSubtitle: String
  val aiSupportTitle: String
  val aiSupportSubtitle: String
  val liveAdminChatTitle: String
  val liveAdminChatSubtitle: String
  val supportTicketsTitle: String
  val supportTicketsSubtitle: String
  val contactUnavailable: String
  val changePhotoTitle: String
  val chooseAvatarPreset: String
  val uploadFromGallery: String
  val statRecentlyJoined: String
  val comingSoonTitle: String
  val comingSoonMessage: String
  val loginPromptToViewProfile: String

  // Rules
  val rulesTitle: String
  val rulesLudo: String
  val rulesCarrom: String
  val rulesFairPlay: String
  val topBarSubtitle: String

  val miniGamesTitle: String
  val miniGamesViewAll: String
  val miniGamesRoyalWheelTitle: String
  val miniGamesRoyalWheelSub: String
  val miniGamesRoyalWheelBadge: String
  val miniGamesRoyalWheelPayout: String
  val miniGamesDiceTitle: String
  val miniGamesDiceSub: String
  val miniGamesDiceBadge: String
  val miniGamesDicePayout: String
  val miniGamesHeadTailTitle: String
  val miniGamesHeadTailSub: String
  val miniGamesHeadTailBadge: String
  val miniGamesScratchTitle: String
  val miniGamesScratchSub: String
  val miniGamesScratchBadge: String
  val miniGamesScratchAction: String
  
  // History
  val historyResultWon: String
  val historyResultLost: String
  val historyResultPending: String
  val historyResultCancelled: String
  val historyDateCompleted: String
  val historyGameLabel: String
  val historyEntryLabel: String
  val historyPrizeLabel: String
  val historyRefundLabel: String
  fun historyMatchTitle(game: String, id: String): String

  // Match Card
  val matchCardLudoChip: String
  val matchCardCarromChip: String
  val matchCardStatusCompleted: String
  val matchCardStatusJoined: String
  val matchCardStatusCancelled: String
  val matchCardStatusPaused: String
  val matchCardStatusDisabled: String
  val matchCardStatusUpcoming: String
  val matchCardStatusRunning: String
  val matchCardStatusReview: String
  val matchCardStatusCodeReady: String
  fun matchCardStatusFull(count: String, max: String): String
  fun matchCardStatusSlots(count: String, max: String): String
  fun matchCardSeatJoined(count: String, max: String): String
  fun matchCardWinnerBanner(prize: String): String
  val matchCardDefeatBanner: String
  val matchCardCancelledBanner: String
  val matchCardPausedBanner: String
  val matchCardDisabledBanner: String
  val matchCardButtonInProgress: String
  val matchCardButtonCodeReady: String
  val matchCardButtonUpcoming: String
  val matchCardButtonSeatFull: String
  val matchCardTimeLeft: String
  val matchCardUpcoming: String

  // Match Detail Screen
  val matchDetailLudoHeader: String
  val matchDetailCarromHeader: String
  val matchDetailWinnerPrizeLabel: String
  fun matchDetailYouWonBanner(prize: String): String
  val matchDetailProofRejectedLoser: String
  val matchDetailMatchFinishedLoser: String
  fun matchDetailRegisteredSlot(slot: String): String
  fun matchDetailSlot(number: String): String
  val matchDetailUpcomingNotice: String
  val matchDetailRoomCodeTitle: String
  val matchDetailUnlockedBadge: String
  val matchDetailRoomCodeInstructions: String
  val matchDetailWaitingRoomCode: String
  val matchDetailConfidentialNotice: String
  val matchDetailApprovedTitle: String
  val matchDetailRejectedTitle: String
  val matchDetailScreenshotSubmittedTitle: String
  val matchDetailApprovedBadge: String
  val matchDetailRejectedBadge: String
  val matchDetailUnderReviewBadge: String
  val matchDetailReviewWaitingText: String
  val matchDetailNotesLabel: String
  val matchDetailSubmittingScreenshotTitle: String
  val matchDetailSubmittingScreenshotSubtitle: String
  val matchDetailProofRejectedTitle: String
  val matchDetailProofRejectedDesc: String
  val matchDetailMatchFinishedTitle: String
  val matchDetailMatchFinishedWinnerDesc: String
  val matchDetailMatchFinishedLoserDesc: String
  val matchDetailResultApprovedTitle: String
  val matchDetailResultApprovedDesc: String
  val matchDetailDefeatedTitle: String
  val matchDetailDefeatedDesc: String
  val matchDetailSubmitProofCta: String
  val matchDetailWaitingRoomCodeForProof: String
  val matchDetailTwoMinutesWaitingText: String
  fun matchDetailRegisteredPlayersTitle(count: String, max: String): String
  val matchDetailNoPlayersJoined: String
  val matchDetailYouBadge: String
  val matchDetailUpcomingButton: String
  fun matchDetailJoinMatchButton(fee: String): String
  val matchDetailJoinDialogTitle: String
  val matchDetailLudoNameNotice: String
  val matchDetailCarromNameNotice: String
  fun matchDetailJoinFeeNotice(fee: String): String
  val matchDetailAgreeAndJoin: String
  val matchDetailSubmitProofTitle: String
  val matchDetailScreenshotRequired: String
  val matchDetailSelectScreenshot: String
  val matchDetailChangeScreenshot: String
  val matchDetailRoomCodeRequired: String
  val matchDetailRoomCodePlaceholder: String
  val matchDetailMatchNumberRequired: String
  val matchDetailMatchNumberLabel: String
  val matchDetailMatchNumberPlaceholder: String
  val matchDetailRemarksLabel: String
  val matchDetailRemarksPlaceholder: String
  val matchDetailSubmitProofButton: String

  // Profile
  val profileStatusActive: String
  val profileStatusBanned: String
  val profileStatusSuspended: String
  val profileStatusPending: String
  val profileSupportOffline: String
  val profileLogoutDialogTitle: String
  val profileLogoutDialogMessage: String
  val profileLogoutConfirmButton: String
  val profilePhotoUpdateSuccess: String
  fun profileAvatarPreset(id: String): String

  // Deposit
  val depositMethodLabel: String
  val depositAmountSummaryLabel: String
  val depositStatusLabel: String
  val depositStatusPending: String
  val depositOfficialBadge: String
  fun depositNumberCopiedToast(method: String, number: String): String
  val depositAmountPlaceholder: String
  val depositTrxIdPlaceholder: String
  val depositScreenshotPreview: String
  fun depositMinAmountError(amount: String): String

  // Notifications
  val notifSubtitle: String
  val notifMarkAllRead: String
  val notifFilterAll: String
  val notifFilterUnread: String
  val notifBadgeNew: String
  val notifCategoryWallet: String
  val notifCategoryMatch: String
  val notifCategoryRoomCode: String
  val notifCategorySystem: String
  val notifTimeJustNow: String
  fun notifTimeMinutesAgo(mins: String): String
  fun notifTimeHoursAgo(hours: String): String
  val notifTimeYesterday: String
  val notifTimeRecent: String
  val notifDialogClose: String
  val notifAllReadSuccess: String
  val notifEmptyFilterTitle: String
  val notifEmptyFilterSubtext: String

  // Mini Games Shared
  val mgWalletTitle: String
  val mgAddMoneyTitle: String
  val mgAddMoneySubtitle: String
  val mgWithdrawMoneyTitle: String
  val mgWithdrawMoneySubtitle: String
  val mgCloseButton: String
  val mgTransferAddTitle: String
  val mgTransferWithdrawTitle: String
  val mgLabelUid: String
  val mgLabelUserName: String
  val mgLabelMobile: String
  fun mgMainBalance(amt: String): String
  fun mgGameBalance(amt: String): String
  val mgAmountPlaceholder: String
  val mgErrorInsufficient: String
  val mgErrorInvalidAmount: String
  val mgConfirmRequest: String
  val mgWinTitle: String
  val mgLossTitle: String
  fun mgWinMessage(amt: String): String
  val mgLossMessage: String

  // Dice Game
  val diceTitle: String
  val diceSubtitle: String
  val diceMainBalanceLabel: String
  val diceGameWalletLabel: String
  val diceAddFunds: String
  val diceInstruction: String
  val diceRolling: String
  val diceRollButton: String
  val diceNoBalance: String
  val diceChoiceEven: String
  val diceChoiceOdd: String
  val dicePayout: String
  val diceClosedBanner: String
  val diceToastNoBalance: String
  val diceToastSuccess: String
  val diceToastFailed: String
  fun diceRollResult(sum: String, type: String): String

  // Head & Tail Game
  val headTailTitle: String
  val headTailSubtitle: String
  val headTailInstruction: String
  val headTailTossing: String
  val headTailTossButton: String
  val headTailChoiceHead: String
  val headTailChoiceTail: String
  val headTailPayout: String
  val headTailClosedBanner: String
  val headTailToastNoBalance: String
  val headTailToastSuccess: String
  val headTailToastFailed: String
  fun headTailResult(side: String): String

  // Spin Wheel Game
  val spinTitle: String
  val spinSubtitle: String
  val spinInstruction: String
  val spinSpinning: String
  val spinButton: String
  val spinNoBalance: String
  val spinClosedBanner: String
  val spinToastNoBalance: String
  val spinToastFailed: String
  fun spinResultWin(multiplier: String): String
  fun spinResultAdded(amount: String): String
  val spinChipMain: String
  val spinChipGame: String

  // Scratch Card Game
  val scratchTitle: String
  val scratchSubtitle: String
  val scratchInstructionNew: String
  val scratchInstructionRevealed: String
  val scratchInstructionScratching: String
  val scratchRevealAll: String
  val scratchNewTicketButton: String
  val scratchCardScratching: String
  val scratchNoBalance: String
  val scratchClosedBanner: String
  val scratchToastFailed: String
  val scratchTransferToast: String
  val scratchTryAgain: String
  val scratchLossMessage: String
  fun scratchWinX(multiplier: String): String
  fun scratchWinMessage(amount: String): String

  // Auto Ludo Lobby
  val autoLudoHeaderTitle: String
  val autoLudoCreateBannerText: String
  val autoLudoCreateButton: String
  val autoLudoFilterAll: String
  val autoLudoFilterRecent: String
  val autoLudoFilterMy: String
  val autoLudoFilterFree: String
  val autoLudoNoMatches: String
  val autoLudoAutoMatchBadge: String
  val autoLudoMyMatchBadge: String
  val autoLudoPrizeLabel: String
  val autoLudoEntryLabel: String
  val autoLudoStatusOpponentMissing: String
  val autoLudoStatusPlayerAbsent: String
  val autoLudoStatusStarting: String
  val autoLudoStatusPlaying: String
  val autoLudoStatusFull: String
  val autoLudoStatusWaitingOpponent: String
  val autoLudoStatusWaitingTwo: String
  fun autoLudoPlayersCount(current: Int, max: Int): String
  fun autoLudoCardTitle(number: String): String
  val autoLudoTutorialButton: String
  val autoLudoRefundCancelled: String
  val autoLudoRefundAbsent: String
  val autoLudoMatchWonFinished: String
  val autoLudoMatchLostFinished: String
  val autoLudoMatchFinished: String
  val autoLudoWaitingStatus: String
  fun autoLudoStartingIn(time: String): String
  val autoLudoEnterGameButton: String
  val autoLudoMatchFullButton: String
  val autoLudoTimeExpired: String
  val autoLudoJoinButton: String
  val autoLudoCreateDialogTitle: String
  val autoLudoGameWalletBalance: String
  val autoLudoEntryFeeHeader: String
  val autoLudoEntryFeePlaceholder: String
  val autoLudoStartTimeLabel: String
  val autoLudoFutureTimeError: String
  val autoLudoWinningPrizeLabel: String
  val autoLudoWinningPrizeSub: String
  fun autoLudoInsufficientBalanceCreate(required: String, current: String): String
  val autoLudoConfirm: String
  val autoLudoJoinDialogTitle: String
  fun autoLudoJoinFeeNotice(amount: String): String
  fun autoLudoBalanceLabel(amount: String): String
  val autoLudoMatchTimeExpiredError: String
  fun autoLudoInsufficientBalanceJoin(amount: String): String
  val autoLudoTimeExpiredToast: String
  val autoLudoTutorialSoonToast: String
  val autoLudoMatchCreatedSuccess: String
  val autoLudoMatchCreatedFail: String
  val autoLudoJoinSuccessToast: String
  val autoLudoJoinFailToast: String
  val autoLudoTransferReqSuccess: String
  fun autoLudoTransferReqFail(err: String): String
  val autoLudoMatchEndedToast: String
  val autoLudoCancelOpponentMissing: String
  val autoLudoCancelBothNotReady: String
  val autoLudoHourLabel: String
  val autoLudoMinLabel: String
  val autoLudoAmPmLabel: String

  // Ready Room Dialog
  val readyRoomTitle: String
  fun readyRoomEntryPrize(entry: String, prize: String): String
  val readyRoomVs: String
  fun readyRoomPlayer(num: Int): String
  val readyRoomNotReady: String
  val readyRoomReady: String
  val readyRoomStartsIn: String
  val readyRoomPlayButton: String
  val readyRoomGoBack: String
  val readyRoomPlayerYou: String
  val readyRoomWaitingOpponent: String
  val readyRoomTimeUpDeciding: String
  val readyRoomStartGame: String
  val readyRoomBack: String
  val readyRoomStarting: String

  // AutoLudo Logs
  fun autoLudoLog(raw: String): String
  
  // New AutoLudoRedesign functions
  fun displayActionLog(raw: String): String
  
  // AutoLudo Result Dialog
  val autoLudoResultWinTitle: String
  val autoLudoResultWinReason: String
  fun autoLudoResultWinPrize(prize: String): String
  val autoLudoResultLoseTitle: String
  fun autoLudoResultLoseReason(winnerName: String): String
  val autoLudoResultOk: String
  
  // AutoLudo Exit Dialog
  val autoLudoExitTitle: String
  fun autoLudoExitMessage(opponent: String): String
  val autoLudoExitConfirm: String
  val autoLudoExitCancel: String
  val autoLudoExitYes: String
  val autoLudoExitNo: String
}

object BengaliStrings : AppStrings {
  // Brand & General
  override val appName = "AD Tournament"
  override val appTagline = "১v১ এস্পোর্টস ব্যাটল"
  override val loading = "লোড হচ্ছে..."
  override val errorGeneric = "একটি সমস্যা হয়েছে"
  override val retry = "আবার চেষ্টা করুন"
  override val cancel = "বাতিল"
  override val save = "সংরক্ষণ"
  override val close = "বন্ধ করুন"
  override val back = "ফিরে যান"
  override val offlineMode = "ইন্টারনেট সংযোগ নেই - অফলাইন মোড"
  override val optional = "ঐচ্ছিক"
  override val locked = "লকড"
  override val adminPanel = "অ্যাডমিন প্যানেল"

  // Navigation
  override val navHome = "হোম"
  override val navWallet = "ওয়ালেট"
  override val navMatches = "ম্যাচ"
  override val navHistory = "হিস্ট্রি"
  override val navProfile = "প্রোফাইল"

  // Auth
  override val phoneNumber = "মোবাইল নম্বর"
  override val enterPhoneNumber = "আপনার ১১-সংখ্যার মোবাইল নম্বর দিন"
  override val password = "পাসওয়ার্ড"
  override val enterPassword = "পাসওয়ার্ড লিখুন"
  override val fullName = "সম্পূর্ণ নাম"
  override val enterFullName = "আপনার সম্পূর্ণ নাম লিখুন"
  override val confirmPassword = "কনফার্ম পাসওয়ার্ড"
  override val enterConfirmPassword = "পাসওয়ার্ড পুনরায় লিখুন"
  override val invalidPhoneError = "অনুগ্রহ করে সঠিক ১১-সংখ্যার নম্বর লিখুন"
  override val passwordMinLengthError = "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে"
  override val passwordsDoNotMatchError = "পাসওয়ার্ড দুটি মিলছে না"
  override val nameRequiredError = "অনুগ্রহ করে আপনার নাম লিখুন"
  override val otpRequiredError = "সঠিক ৬ সংখ্যার ওটিপি কোড দিন"

  // Auth - Login
  override val loginTitle = "স্বাগতম"
  override val loginSubtitle = "ব্যাটল এরিনায় প্রবেশ করতে ফোন নম্বর ও পাসওয়ার্ড দিন"
  override val loginButton = "লগইন"
  override val forgotPasswordLink = "পাসওয়ার্ড ভুলে গেছেন?"
  override val noAccountRegister = "অ্যাকাউন্ট নেই? রেজিস্ট্রেশন করুন"
  override val playerSignIn = "প্লেয়ার সাইন ইন"
  override val playerSignInSubtitle = "আপনার ১১-সংখ্যার মোবাইল নম্বর ও পাসওয়ার্ড দিন"
  override val signInButton = "সাইন ইন"
  override val noAccountPrompt = "অ্যাকাউন্ট নেই?"
  override val createAccountAction = "অ্যাকাউন্ট তৈরি করুন"
  override val togglePassword = "পাসওয়ার্ড দেখুন বা লুকান"
  override val phoneEmptyError = "মোবাইল নম্বর খালি রাখা যাবে না"
  override val passwordEmptyError = "পাসওয়ার্ড খালি রাখা যাবে না"
  override val invalidCredentialsError = "ভুল মোবাইল নম্বর বা পাসওয়ার্ড"
  override val accountNotFoundError = "এই নম্বরে কোনো অ্যাকাউন্ট নিবন্ধিত নেই"

  // Auth - Register
  override val registerTitle = "অ্যাকাউন্ট তৈরি করুন"
  override val registerSubtitle = "হাজারো প্লেয়ারের সাথে ১v১ এস্পোর্টসে যোগ দিন"
  override val registerButton = "রেজিস্ট্রেশন করুন"
  override val alreadyHaveAccountLogin = "ইতিমধ্যে অ্যাকাউন্ট আছে? লগইন করুন"

  // Auth - Forgot Password & OTP
  override val forgotPasswordTitle = "পাসওয়ার্ড রিসেট"
  override val forgotPasswordSubtitle = "ভেরিফিকেশন কোড পেতে আপনার মোবাইল নম্বর দিন"
  override val sendOtpButton = "ওটিপি পাঠান"
  override val verifyOtpTitle = "ওটিপি যাচাই"
  override val verifyOtpSubtitle = "আপনার মোবাইলে পাঠানো ৬ সংখ্যার ওটিপি দিন"
  override val verifyOtpButton = "যাচাই করুন"
  override val resetPasswordTitle = "নতুন পাসওয়ার্ড"
  override val resetPasswordSubtitle = "আপনার অ্যাকাউন্টের জন্য নতুন পাসওয়ার্ড সেট করুন"
  override val resetPasswordButton = "পাসওয়ার্ড নিশ্চিত করুন"
  override val backToLogin = "লগইনে ফিরে যান"

  // Home Screen
  override val balanceCurrent = "বর্তমান ব্যালেন্স"
  override val depositAction = "টাকা যোগ"
  override val withdrawAction = "উত্তোলন"
  override val quickMatches = "ম্যাচ"
  override val quickWallet = "ওয়ালেট"
  override val quickRules = "নিয়মাবলী"
  override val quickSupport = "সাপোর্ট"
  override val featuredBattles = "ফিচার্ড ব্যাটল"
  override val liveAndUpcoming = "লাইভ ও আপকামিং"
  override val filterAll = "সব"
  override val filterLudo = "লুডু"
  override val filterCarrom = "ক্যারম"
  override val noMatchesAvailable = "কোনো ম্যাচ পাওয়া যায়নি"
  override val noMatchesSubtext = "নতুন টুর্নামেন্টের জন্য শীঘ্রই আবার দেখুন"
  override val tournamentsHeading = "টুর্নামেন্টস"
  override val allMatchesQuickTitle = "সকল ম্যাচ"
  override val allMatchesQuickSubtitle = "সব Ludo ও Carrom 1v1 ম্যাচ এক জায়গায়"
  override val filterAllGames = "সব গেম"
  override val availableMatchesTitle = "চলমান ও উন্মুক্ত ম্যাচ"
  override val loadingMatches = "ম্যাচ লোড হচ্ছে..."
  override val failedToLoadMatches = "ম্যাচ লোড করা সম্ভব হয়নি"
  override val noAvailableMatchesTitle = "কোনো উন্মুক্ত ম্যাচ নেই"
  override val noAvailableMatchesSubtext = "বর্তমানে সকল ম্যাচ পূর্ণ বা চলছে। সুপার অ্যাডমিন শীঘ্রই নতুন 1v1 ম্যাচ পোস্ট করবেন।"
  override val refreshMatches = "রিফ্রেশ করুন"
  override fun viewAllMatchesCount(count: Int) = "সকল ম্যাচ দেখুন ($count)"
  override val officialVideoNotice = "ℹ️ অফিসিয়াল ভিডিও টিউটোরিয়াল সরাসরি অ্যাপে শীঘ্রই উন্মুক্ত করা হবে।"
  override val depositGuideTitle = "কীভাবে Deposit করবেন?"
  override val depositGuideSub = "bKash / Nagad দিয়ে সহজেই টাকা Add করুন"
  override val matchJoinGuideTitle = "কীভাবে Match Join করবেন?"
  override val matchJoinGuideSub = "ম্যাচ সিলেক্ট করে 1v1 ব্যাটলে যোগ দিন"
  override val resultGuideTitle = "কীভাবে Result Submit করবেন?"
  override val resultGuideSub = "ম্যাচ শেষে স্ক্রিনশট দিয়ে উইন ক্লেইম করুন"
  override val rulesGuideTitle = "Tournament Rules জানুন"
  override val rulesGuideSub = "ফেয়ার প্লে ও টুর্নামেন্ট নিয়মাবলী পড়ুন"
  override val viewGuideCta = "▶ বিবরণ দেখুন"

  override fun homeReferTitle(amount: String) = "বন্ধু রেফার করলেই $amount বোনাস!"
  override val homeReferSubtitle = "প্রথম ডিপোজিটে ক্যাশ রিওয়ার্ড"
  override val homeReferCta = "রেফার করুন >"
  override val homeQuickDepositSub = "টাকা যোগ করুন"
  override val homeQuickWithdrawSub = "উত্তোলন করুন"
  override val homeAutoGamesHeading = "★ অটো গেম ★"
  override val homeAutoLudoTitle = "অটো লুডু"
  override val homeAutoLudoSub = "ইনস্ট্যান্ট বোর্ড গেম"
  override val homeAutoLudoBadge = "১v১ ফাস্ট"
  override val homeCarromTitle = "ক্যারম পুল"
  override val homeCarromSub = "বোর্ড স্ট্রাইক ব্যাটেল"
  override val homeNewBadge = "নতুন"
  override val homeCarromComingSoon = "ক্যারম পুল শীঘ্রই আসছে"
  override val homeNoticeTitle = "📢 জরুরি বিজ্ঞপ্তি"
  override val homeOk = "ঠিক আছে"
  override val homeBadgeDeposit = "ডিপোজিট গাইড"
  override val homeBadgeJoin = "জয়েন গাইড"
  override val homeBadgeResult = "রেজাল্ট গাইড"
  override val homeBadgeRules = "ফেয়ার প্লে নিয়ম"
  override val homeEmptyMatchesTitle = "বর্তমানে কোনো ম্যাচ উপলব্ধ নেই"



  // Wallet
  override val walletTitle = "ওয়ালেট ব্যালেন্স"
  override val totalBalance = "মোট ব্যালেন্স"
  override val availableToPlay = "খেলার জন্য ব্যালেন্স"
  override val lockedInContests = "ম্যাচে লকড"
  override val recentTransactions = "সাম্প্রতিক লেনদেন"
  override val viewAll = "সব দেখুন"
  override val noTransactionsYet = "কোনো লেনদেন পাওয়া যায়নি"
  override val noTransactionsSubtext = "আপনার সকল ডিপোজিট, উইথড্র ও ম্যাচের লেনদেন এখানে থাকবে"
  override val walletSubtitle = "আপনার উপার্জন • সুরক্ষিত ও বিশ্বস্ত"
  override val statDeposited = "ডিপোজিট"
  override val statWinnings = "বিজয়ী অর্থ"
  override val statWithdrawn = "উত্তোলিত"
  override val txnPrize = "টুর্নামেন্ট পুরস্কার"
  override val txnRefund = "ম্যাচ রিফান্ড"
  override val txnEntryFee = "ম্যাচ এন্ট্রি ফি"
  override fun pendingEscrowNotice(amount: String) = "চলমান ম্যাচ / উইথড্রতে পেন্ডিং: ৳$amount"
  override val walletMainCashLabel = "মেইন ক্যাশ ব্যালেন্স"
  override val walletBonusLabel = "বোনাস ব্যালেন্স"
  override val walletBonusUsageHint = "প্রতিটি ম্যাচে ১০% বোনাস ব্যবহার করা যাবে"
  override val walletTxDeposit = "ডিপোজিট"
  override val walletTxWithdraw = "উত্তোলন"
  override val walletTxMatchJoin = "ম্যাচে অংশগ্রহণ"
  override val walletTxMatchRefund = "ম্যাচ রিফান্ড"
  override val walletTxPrize = "টুর্নামেন্ট পুরস্কার"
  override val walletTxMiniGameTransferIn = "গেম থেকে ট্রান্সফার"
  override val walletTxMiniGameTransferOut = "গেমে ট্রান্সফার"
  override val walletTxAdminAdjustment = "অ্যাডমিন অ্যাডজাস্টমেন্ট"
  override val walletTxBonus = "বোনাস ক্রেডিট"
  override val walletTxOther = "অন্যান্য লেনদেন"
  override val walletTxStatusCompleted = "সফল"
  override val walletTxStatusPending = "অপেক্ষমাণ"
  override val walletTxStatusRejected = "প্রত্যাখ্যাত"
  override val walletTxStatusCancelled = "বাতিল"
  override val walletTxDescDeposit = "ওয়ালেটে টাকা যোগ করা হয়েছে"
  override val walletTxDescWithdraw = "ওয়ালেট থেকে টাকা উত্তোলন"
  override val walletTxDescMatchJoin = "ম্যাচ এন্ট্রি ফি কাটা হয়েছে"
  override val walletTxDescMatchRefund = "ম্যাচ বাতিল হওয়ায় রিফান্ড"
  override val walletTxDescPrize = "ম্যাচ জয়ের পুরস্কার যোগ হয়েছে"
  override val walletTxDescTransferIn = "মিনি-গেম ব্যালেন্স থেকে ওয়ালেটে ট্রান্সফার"
  override val walletTxDescTransferOut = "ওয়ালেট থেকে মিনি-গেম ব্যালেন্সে ট্রান্সফার"
  override val walletTxDescBonus = "প্রমোショナル বোনাস যোগ করা হয়েছে"
  override val walletTxDescDefault = "লেনদেন সফলভাবে সম্পন্ন হয়েছে"
  override fun walletTxDescMatch(matchId: String) = "ম্যাচ #$matchId এর জন্য লেনদেন"

  // Deposit Screen
  override val depositTitle = "টাকা যোগ / ডিপোজিট"
  override val selectPaymentMethod = "পেমেন্ট মেথড নির্বাচন করুন"
  override val paymentMethodBkash = "বিকাশ (bKash)"
  override val paymentMethodNagad = "নগদ (Nagad)"
  override val sendMoneyInstructions = "অফিসিয়াল নম্বরে সেন্ড মানি (Personal) করুন, তারপর TrxID সাবমিট করুন।"
  override val copyNumber = "নম্বর কপি করুন"
  override val numberCopied = "নম্বর ক্লিপবোর্ডে কপি হয়েছে"
  override val depositAmount = "ডিপোজিট পরিমাণ"
  override val enterDepositAmount = "পরিমাণ লিখুন"
  override val trxIdLabel = "ট্রানজেকশন আইডি (TrxID)"
  override val enterTrxId = "সঠিক TrxID লিখুন"
  override val senderMobileNumber = "প্রেরকের মোবাইল নম্বর"
  override val enterSenderMobile = "যে নম্বর থেকে টাকা পাঠিয়েছেন"
  override val screenshotProofLabel = "স্ক্রিনশট প্রুফ (ঐচ্ছিক)"
  override val submitDepositButton = "ডিপোজিট সাবমিট করুন"
  override val depositProcessingTime = "ডিপোজিট সাধারণত ৫-১৫ মিনিটের মধ্যে ভেরিফাই ও যোগ করা হয়"
  override val depositSecurityWarning = "ভুয়া TrxID বা ভুয়া প্রমাণ সাবমিট করলে আইডি আজীবনের জন্য ব্যান হবে।"
  override fun minDepositNotice(amount: String) = "সর্বনিম্ন ডিপোজিট ৳$amount"
  override val depositSubmittedTitle = "ডিপোজিট রিকোয়েস্ট সফলভাবে জমা হয়েছে"
  override val depositSubmittedDesc = "আপনার ডিপোজিট অ্যাডমিন ভেরিফিকেশনের জন্য পাঠানো হয়েছে। ৫-১৫ মিনিটের মধ্যে ব্যালেন্সে যোগ হবে।"
  override val backToWallet = "ওয়ালেটে ফিরে যান"
  override val addMoneyTitle = "টাকা যোগ করুন"
  override val depositSubtitle = "খেলতে ডিপোজিট করুন • দ্রুত ও নিরাপদ"
  override val stepSelectMethod = "১. পেমেন্ট মেথড নির্বাচন করুন"
  override val selectedBadge = "বাছাইকৃত"
  override val personalBadge = "ব্যক্তিগত"
  override fun sendMoneyLabel(method: String) = "সেন্ড মানি ($method পার্সোনাল):"
  override val copied = "কপি হয়েছে"
  override val copy = "কপি"
  override val howToDepositTitle = "ডিপোজিট করার নিয়ম:"
  override val stepDepositDetails = "২. ডিপোজিট বিস্তারিত"
  override val depositRange = "সর্বনিম্ন ৳৫০ • সর্বোচ্চ ৳২৫,০০০"
  override val enterAmountLabel = "পরিমাণ লিখুন (টাকা)"
  override val senderMobileLabel = "প্রেরকের মোবাইল নম্বর"
  override val paymentScreenshotLabel = "পেমেন্টের স্ক্রিনশট"
  override val uploadScreenshotTitle = "পেমেন্ট স্ক্রিনশট আপলোড করুন"
  override val uploadScreenshotSub = "গ্যালারি থেকে ছবি বাছাই করতে চাপুন"
  override val screenshotAttached = "স্ক্রিনশট যুক্ত হয়েছে"
  override val replace = "বদলান"
  override val remove = "মুছুন"
  override val depositSecurityNotice = "আপনার পেমেন্ট স্ক্রিনশট এবং ট্রানজেকশন তথ্য অ্যাডমিন ভেরিফিকেশনের জন্য নিরাপদে সংরক্ষিত।"
  override val depositMinError = "সর্বনিম্ন ডিপোজিট পরিমাণ ৳ ৫০"
  override val depositMaxError = "সর্বোচ্চ ডিপোজিট পরিমাণ ৳ ২৫,০০০"
  override val invalidMobileError = "অনুগ্রহ করে সঠিক ১১-সংখ্যার প্রেরক নম্বর লিখুন"
  override val invalidTrxIdError = "অনুগ্রহ করে সঠিক ট্রানজেকশন আইডি লিখুন"
  override val depositDuplicateError = "একই প্রেরক নম্বর, ট্রানজেকশন আইডি এবং পরিমাণের ডিপোজিট রিকোয়েস্ট ইতিমধ্যে জমা দেওয়া হয়েছে।"
  override val depositLimitExceededError = "২০ মিনিটের মধ্যে সর্বোচ্চ ৩টি ডিপোজিট রিকোয়েস্ট জমা দেওয়া যাবে। কিছুক্ষণ পর আবার চেষ্টা করুন।"

  // Withdraw Screen
  override val withdrawTitle = "টাকা উত্তোলন / উইথড্র"
  override val withdrawAmount = "উত্তোলনের পরিমাণ"
  override val enterWithdrawAmount = "পরিমাণ লিখুন"
  override val recipientMobileNumber = "উত্তোলনের নম্বর"
  override val enterRecipientMobile = "যে নম্বরে টাকা নিতে চান"
  override val requestWithdrawalButton = "উইথড্র রিকোয়েস্ট পাঠান"
  override val withdrawProcessingTime = "উইথড্র রিকোয়েস্ট ৩০-৬০ মিনিটের মধ্যে প্রসেস করা হয়"
  override fun availableBalanceNotice(amount: String) = "উত্তোলনযোগ্য ব্যালেন্স: ৳$amount"
  override fun minWithdrawNotice(amount: String) = "সর্বনিম্ন উত্তোলন ৳$amount"

  // Withdraw Screen Extensions
  override val withdrawHeaderTitle = "উইথড্র / উত্তোলন"
  override val withdrawHeaderSubtitle = "আপনার উপার্জন • সুরক্ষিত ও বিশ্বস্ত"
  override val withdrawSelectMethodLabel = "পেমেন্ট মেথড নির্বাচন করুন"
  override val withdrawMethodSelected = "বাছাইকৃত"
  override val withdrawMethodPersonal = "ব্যক্তিগত"
  override val withdrawAmountLabel = "উত্তোলনের পরিমাণ"
  override val withdrawAvailableBalanceLabel = "উত্তোলনযোগ্য ব্যালেন্স"
  override fun withdrawLimitNotice(min: String) = "সর্বনিম্ন ৳$min • সর্বোচ্চ ৳১০,০০০"
  override val withdrawEnterAmountHint = "পরিমাণ লিখুন (টাকা)"
  override val withdrawEnterAmountPlaceholder = "যেমন: ২০০"
  override fun withdrawRecipientLabel(method: String) = "$method উত্তোলনের নম্বর"
  override val withdrawUseSavedNumber = "সংরক্ষিত নম্বর ব্যবহার করুন"
  override fun withdrawRecipientHint(method: String) = "আপনার পার্সোনাল $method নম্বর"
  override val withdrawRecipientPlaceholder = "০১XXXXXXXXX"
  override fun withdrawVerifiedBadge(method: String) = "ভেরিফাইড সংরক্ষিত $method অ্যাকাউন্ট"
  override fun withdrawSecurityHint(method: String) = "দ্রুত পেমেন্টের জন্য এই $method অ্যাকাউন্টটি আপনার নিজের নিশ্চিত করুন।"
  override val withdrawSummaryTitle = "উত্তোলনের বিবরণ"
  override val withdrawSummaryAmount = "উত্তোলনের পরিমাণ"
  override val withdrawSummaryMethod = "পেমেন্ট মেথড"
  override val withdrawSummaryNumber = "গ্রহণকারী নম্বর"
  override val withdrawSummaryTotal = "সর্বমোট পাবেন"
  override val withdrawSecurityTitle = "নিরাপত্তা ও নির্দেশিকা"
  override val withdrawGuideline1 = "• একবারে সর্বোচ্চ একটি সক্রিয় বা পেন্ডিং উইথড্র রিকোয়েস্ট অনুমোদিত।"
  override fun withdrawGuideline2(min: String) = "• সর্বনিম্ন উত্তোলন ৳$min (একবারে সর্বোচ্চ ৳১০,০০০)।"
  override val withdrawGuideline3 = "• প্রসেস হওয়ার আগ পর্যন্ত উত্তোলনের টাকা পেন্ডিং ব্যালেন্সে সুরক্ষিত থাকবে।"
  override val withdrawGuideline4 = "• অ্যাডমিন অনুমোদন ও ভেরিফিকেশন প্রয়োজন।"
  override val withdrawGuideline5 = "• প্রত্যাখ্যাত হলে, হোল্ড করা টাকা স্বয়ংক্রিয়ভাবে ওয়ালেটে ফেরত আসবে।"
  override val withdrawNowButton = "উত্তোলন করুন"
  override fun withdrawMinError(min: String) = "সর্বনিম্ন উত্তোলনের পরিমাণ ৳ $min"
  override val withdrawMaxError = "সর্বোচ্চ উত্তোলনের পরিমাণ ৳ ১০,০০০"
  override fun withdrawInsufficientError(balance: String) = "পর্যাপ্ত ব্যালেন্স নেই। আপনার কাছে আছে ৳ $balance"
  override val withdrawInvalidMobileError = "অনুগ্রহ করে সঠিক ১১-সংখ্যার উত্তোলনের মোবাইল নম্বর লিখুন"
  override val withdrawSuccessTitle = "উত্তোলন রিকোয়েস্ট পাঠানো হয়েছে"
  override fun withdrawSuccessDesc(amount: String, method: String) = "$method এর মাধ্যমে $amount টাকা উত্তোলনের রিকোয়েস্ট অ্যাডমিন প্রসেসিংয়ের জন্য জমা দেওয়া হয়েছে।"
  override val withdrawStatusLabel = "অবস্থা"
  override val withdrawStatusPending = "অ্যাডমিন অনুমোদনের অপেক্ষায়"
  override val withdrawPayoutMethodLabel = "পেমেন্ট মেথড"
  override val withdrawRecipientAccountLabel = "প্রাপক অ্যাকাউন্ট"
  override val withdrawRequestedAmountLabel = "অনুরোধকৃত পরিমাণ"
  override val withdrawBackToWallet = "ওয়ালেটে ফিরে যান"
  override val txHistoryTitle = "লেনদেন হিস্ট্রি"
  override val txHistorySubtitle = "আপনার ডিপোজিট, ম্যাচ এন্ট্রি, উইনিং এবং উত্তোলনের তথ্য দেখুন"
  override val txHistoryFilterAll = "সব"
  override val txHistoryFilterDeposit = "ডিপোজিট"
  override val txHistoryFilterMatch = "ম্যাচ"
  override val txHistoryFilterWinning = "উইনিং"
  override val txHistoryFilterWithdraw = "উত্তোলন"
  override val txHistoryEmptyTitle = "এখনও কোনো লেনদেন হয়নি"
  override val txHistoryEmptyDesc = "আপনার সম্পন্ন ডিপোজিট, ম্যাচ এন্ট্রি এবং উত্তোলনগুলো এখানে সংরক্ষিত থাকবে।"
  override val txHistoryEmptyFilterTitle = "এই ক্যাটাগরিতে কোনো লেনদেন নেই"
  override val txHistoryEmptyFilterDesc = "নির্বাচিত ফিল্টারের জন্য কোনো লেনদেনের তথ্য পাওয়া যায়নি।"
  override val txHistoryRefLabel = "রেফারেন্স:"
  override val txHistoryCopyAction = "কপি"
  override val txHistoryCopied = "কপি হয়েছে"

  // Transaction History Screen
  override val transactionHistoryTitle = "লেনদেন হিস্ট্রি"
  override val filterDeposits = "ডিপোজিট"
  override val filterWithdrawals = "উইথড্র"
  override val statusCompleted = "সফল"
  override val statusPending = "অপেক্ষমাণ"
  override val statusRejected = "প্রত্যাখ্যাত"
  override val statusProcessing = "চলমান"

  // Matches Lobby
  override val tournamentsTitle = "১v১ টুর্নামেন্ট"
  override val entryFeeLabel = "এন্ট্রি ফি"
  override val prizePoolLabel = "পুরস্কার"
  override val playersLabel = "প্লেয়ার"
  override val joinNowButton = "যোগ দিন"
  override val matchFull = "পূর্ণ"
  override val matchLive = "লাইভ"

  // Match Details
  override val matchDetailTitle = "ম্যাচ বিস্তারিত"
  override val roomCodeLabel = "রুম কোড"
  override val copyRoomCodeButton = "রুম কোড কপি করুন"
  override val roomCodeCopiedNotice = "রুম কোড ক্লিপবোর্ডে কপি হয়েছে"
  override val playersHeader = "প্লেয়ার তালিকা"
  override val waitingForOpponent = "প্রতিপক্ষের জন্য অপেক্ষা করা হচ্ছে..."
  override val matchRulesHeader = "ম্যাচের নিয়মাবলী"
  override val submitResultHeader = "ফলাফল জমা দিন"
  override val actionWon = "আমি জিতেছি"
  override val actionLost = "আমি হেরেছি"
  override val uploadWinnerScreenshot = "বিজয়ী স্ক্রিনশট আপলোড করুন"
  override val resultWarningNotice = "ভুয়া স্ক্রিনশট বা মিথ্যা ফলাফল দিলে অ্যাকাউন্ট চিরতরে বাতিল হবে।"

  // Match History
  override val matchHistoryTitle = "ম্যাচ হিস্ট্রি"
  override val matchHistorySubtitle = "আপনার অতীত ১v১ ব্যাটল, পুরস্কার এবং ফলাফল দেখুন।"
  override val totalPlayed = "মোট ম্যাচ"
  override val totalWon = "জয়"
  override val winRateLabel = "জয়ের হার"
  override val badgeVictory = "বিজয়ী"
  override val badgeDefeat = "পরাজিত"
  override val badgeRefunded = "ফেরত"
  override val badgePending = "অপেক্ষমাণ"
  override val badgeUnderReview = "পর্যালোচনাধীন"
  override val badgeRejected = "প্রত্যাখ্যাত"
  override val matchCancelled = "ম্যাচ বাতিল হয়েছে"
  override val cancellationReason = "বাতিলের কারণ"
  override val labelGame = "গেম:"
  override val labelEntry = "এন্ট্রি:"
  override val labelPrize = "পুরস্কার:"
  override val labelRefund = "ফেরত:"
  override val noMatchHistoryTitle = "এখনও কোনো ম্যাচ খেলেননি"
  override val noMatchHistorySubtext = "আপনার সমাপ্ত ১v১ ব্যাটল এখানে দেখা যাবে।"
  override val noFilteredMatchesTitle = "কোনো ম্যাচ পাওয়া যায়নি"
  override val noFilteredMatchesSubtext = "অন্য কোনো গেম ফিল্টার চেষ্টা করুন।"
 
  // Matches Screen Specific
  override val matchesHeaderTitle = "টুর্নামেন্ট ম্যাচ"
  override val matchesHeaderSubtitle = "রিয়েল-টাইম ১v১ লুডু ও ক্যারম ব্যাটল"
  override val tabAvailable = "উপলব্ধ"
  override val tabMyJoined = "আমার যুক্ত ম্যাচ"
  override val tabUpcoming = "আসন্ন"
  override val filterLudo1v1 = "লুডু ১v১"
  override val filterCarrom1v1 = "ক্যারম ১v১"
  override val labelFormat = "ফরম্যাট"
  override val label1v1Battle = "১v১ ব্যাটল"
  override val labelJoined = "যুক্ত হয়েছে"
  override val labelMatchCompleted = "ম্যাচ সমাপ্ত"
  override val labelStartsAt = "শুরু হবে"
  override val labelViewResult = "ফলাফল দেখুন"
  override val actionJoinNowCta = "জয়েন করুন"
  override val emptyAvailableTitleLabel = "কোনো উন্মুক্ত ম্যাচ নেই"
  override val emptyAvailableDescLabel = "বর্তমানে সকল ম্যাচ পূর্ণ বা চলছে। সুপার অ্যাডমিন শীঘ্রই নতুন ১v১ ম্যাচ পোস্ট করবেন।"
  override val emptyJoinedTitleLabel = "কোনো যুক্ত ম্যাচ নেই"
  override val emptyJoinedDescLabel = "আপনি এখনও কোনো সক্রিয় টুর্নামেন্টে যোগ দেননি। ১v১ ম্যাচে প্রবেশ করতে 'উপলব্ধ' ট্যাবটি দেখুন!"
  override val emptyUpcomingTitleLabel = "কোনো আসন্ন টুর্নামেন্ট নেই"
  override val emptyUpcomingDescLabel = "আসন্ন টুর্নামেন্টগুলো এখানে প্রদর্শিত হবে। বিশেষ সপ্তাহান্তের টুর্নামেন্টের জন্য সাথেই থাকুন!"
  override val emptyHistoryTitleLabel = "কোনো ম্যাচ হিস্ট্রি নেই"
  override val emptyHistoryDescLabel = "আপনার সমাপ্ত ম্যাচ এবং টুর্নামেন্টের ফলাফল এখানে সংরক্ষিত থাকবে।"
  override val browseAvailableCta = "উপলব্ধ ম্যাচ দেখুন"
  override fun loadingTabMatches(tab: String) = "$tab ম্যাচ লোড হচ্ছে..."


  // Notifications
  override val notificationsTitle = "নোটিফিকেশন"
  override val noNotificationsTitle = "কোনো নতুন নোটিফিকেশন নেই"
  override val noNotificationsSubtext = "ম্যাচ ও ওয়ালেট সংক্রান্ত সকল আপডেট এখানে আসবে"

  // Profile
  override val profileTitle = "প্লেয়ার প্রোফাইল"
  override val playerLabel = "প্লেয়ার"
  override val labelId = "আইডি:"
  override val mobileLockedNotice = "নিরাপত্তা ও অ্যান্টি-ফ্রড সুরক্ষার জন্য মোবাইল নম্বর পরিবর্তন করা যাবে না।"
  override val careerStatistics = "ক্যারিয়ার পরিসংখ্যান"
  override val statMatches = "ম্যাচ"
  override val statMatchesSub = "১v১ ব্যাটল"
  override val statWins = "জয়"
  override val statLosses = "পরাজয়"
  override val statLossesSub = "প্রতিযোগিতা"
  override val statTotalWinnings = "মোট অর্জন"
  override val statAllTimeRewards = "সর্বকালীন পুরস্কার"
  override val statMemberSince = "সদস্য পদ"
  override val statVerifiedPlayer = "ভেরিফাইড প্লেয়ার"
  override val matchHistoryMenu = "ম্যাচ হিস্ট্রি"
  override val matchHistoryMenuSub = "অতীত ব্যাটল, ফলাফল ও অর্জিত পুরস্কার"
  override val fairPlayTitle = "ন্যায্য খেলা ও সততা নীতি"
  override val languageSettingTitle = "অ্যাপের ভাষা / Language"
  override val languageSettingSubtitle = "পছন্দের ভাষা নির্বাচন করুন"
  override val languageBengali = "বাংলা (ডিফল্ট)"
  override val languageEnglish = "English"
  override val signOutButton = "লগআউট"
  override val updateDisplayNameTitle = "নাম পরিবর্তন"
  override val enterNewNamePlaceholder = "নতুন ডিসপ্লে নাম লিখুন"
  override val nameUpdateSuccess = "ডিসপ্লে নাম সফলভাবে পরিবর্তন হয়েছে"
  override fun userCopied(id: String) = "ইউজার আইডি $id ক্লিপবোর্ডে কপি হয়েছে"
  override fun winRateSubtitle(rate: Int) = "${digits(rate.toString())}% জয়ের হার"
  override fun fairPlayText(status: String) =
    "• অ্যাকাউন্ট স্ট্যাটাস: $status\n• একক অ্যাকাউন্ট নীতি: প্রতি প্লেয়ারের জন্য একটি ভেরিফাইড মোবাইল নম্বর\n• জিরো টলারেন্স: চিটিং, ভুয়া স্ক্রিনশট বা ম্যাচ ত্যাগ করলে চিরতরে অ্যাকাউন্ট স্থগিত ও ব্যালেন্স বাজেয়াপ্ত করা হবে।"

  // Support
  override val supportTitle = "সহায়তা ও সাপোর্ট"
  override val supportWhatsapp = "হোয়াটসঅ্যাপ সাপোর্ট"
  override val supportTelegram = "অফিসিয়াল টেলিগ্রাম চ্যানেল"
  override val supportFaq = "সাধারণ প্রশ্নোত্তর (FAQ)"
  override val helpAndSupportTitle = "হেল্প ও সাপোর্ট সেন্টার"
  override val helpAndSupportSubtitle = "যেকোনো সমস্যায় আমরা আপনার পাশে আছি"
  override val aiSupportTitle = "এআই লাইভ সাপোর্ট"
  override val aiSupportSubtitle = "তাৎক্ষণিক সাহায্য ও প্রশ্নের সমাধান"
  override val liveAdminChatTitle = "অ্যাডমিন লাইভ চ্যাট"
  override val liveAdminChatSubtitle = "সরাসরি সাপোর্ট টিমের সাথে কথা বলুন"
  override val supportTicketsTitle = "সাপোর্ট টিকিট ও হিস্ট্রি"
  override val supportTicketsSubtitle = "পূর্বের অভিযোগ ও বর্তমান অবস্থা দেখুন"
  override val contactUnavailable = "বর্তমানে অনুপলব্ধ"
  override val changePhotoTitle = "প্রোফাইল ছবি পরিবর্তন"
  override val chooseAvatarPreset = "এস্পোর্টস অ্যাভাটার বেছে নিন"
  override val uploadFromGallery = "গ্যালারি থেকে ছবি আপলোড"
  override val statRecentlyJoined = "সম্প্রতি যুক্ত হয়েছেন"
  override val comingSoonTitle = "শীঘ্রই আসছে"
  override val comingSoonMessage = "এই ফিচারটি পরবর্তী আপডেটে চালু করা হবে।"
  override val loginPromptToViewProfile = "প্রোফাইল দেখতে অনুগ্রহ করে সাইন ইন করুন"

  // Rules
  override val rulesTitle = "টুর্নামেন্ট নিয়মাবলী"
  override val rulesLudo = "লুডু টুর্নামেন্ট নিয়ম"
  override val rulesCarrom = "ক্যারম টুর্নামেন্ট নিয়ম"
  override val rulesFairPlay = "ন্যায্য খেলার নীতিমালা"
  override val topBarSubtitle = "১v১ এস্পোর্টস ব্যাটল"

  override val miniGamesTitle = "মিনি গেমস"
  override val miniGamesViewAll = "সব দেখুন"
  override val miniGamesRoyalWheelTitle = "রয়্যাল হুইল"
  override val miniGamesRoyalWheelSub = "স্পিন করে জিতুন"
  override val miniGamesRoyalWheelBadge = "হট"
  override val miniGamesRoyalWheelPayout = "সর্বোচ্চ ৫০x"
  override val miniGamesDiceTitle = "জোড়-বেজোড়"
  override val miniGamesDiceSub = "ছক্কার চাল"
  override val miniGamesDiceBadge = "১.৯ গুণ জয়"
  override val miniGamesDicePayout = "জিতলে ১.৯ গুণ"
  override val miniGamesHeadTailTitle = "হেড-টেল"
  override val miniGamesHeadTailSub = "ইনস্ট্যান্ট টস"
  override val miniGamesHeadTailBadge = "জনপ্রিয়"
  override val miniGamesScratchTitle = "স্ক্র্যাচ কার্ড"
  override val miniGamesScratchSub = "ঘষে জিতুন"
  override val miniGamesScratchBadge = "জ্যাকপট"
  override val miniGamesScratchAction = "ঘষে জিতুন"
  
  // History
  override val historyResultWon = "বিজয়ী 🏆"
  override val historyResultLost = "পরাজিত"
  override val historyResultPending = "পর্যালোচনাধীন"
  override val historyResultCancelled = "বাতিল"
  override val historyDateCompleted = "সম্পন্ন"
  override val historyGameLabel = "গেম:"
  override val historyEntryLabel = "এন্ট্রি:"
  override val historyPrizeLabel = "পুরস্কার:"
  override val historyRefundLabel = "ফেরত:"
  override fun historyMatchTitle(game: String, id: String) = "$game ১v১ #$id"

  // Match Card
  override val matchCardLudoChip = "লুডু ১v১"
  override val matchCardCarromChip = "ক্যারম ১v১"
  override val matchCardStatusCompleted = "সম্পন্ন"
  override val matchCardStatusJoined = "যুক্ত"
  override val matchCardStatusCancelled = "বাতিল"
  override val matchCardStatusPaused = "স্থগিত"
  override val matchCardStatusDisabled = "অক্ষম"
  override val matchCardStatusUpcoming = "আসন্ন"
  override val matchCardStatusRunning = "চলমান"
  override val matchCardStatusReview = "রিভিউ"
  override val matchCardStatusCodeReady = "কোড রেডি"
  override fun matchCardStatusFull(count: String, max: String) = "পূর্ণ ($count/$max)"
  override fun matchCardStatusSlots(count: String, max: String) = "$count/$max স্লট"
  override fun matchCardSeatJoined(count: String, max: String) = "$count/$max যুক্ত"
  override fun matchCardWinnerBanner(prize: String) = "ম্যাচ সমাপ্ত • বিজয়ী ($prize)"
  override val matchCardDefeatBanner = "ম্যাচ সমাপ্ত • পরাজিত"
  override val matchCardCancelledBanner = "ম্যাচ বাতিল • এন্ট্রি ফি ফেরত দেওয়া হয়েছে"
  override val matchCardPausedBanner = "সুপার অ্যাডমিন কর্তৃক ম্যাচ স্থগিত"
  override val matchCardDisabledBanner = "ম্যাচ অক্ষম করা হয়েছে"
  override val matchCardButtonInProgress = "চলমান"
  override val matchCardButtonCodeReady = "কোড প্রস্তুত"
  override val matchCardButtonUpcoming = "আসন্ন"
  override val matchCardButtonSeatFull = "আসন পূর্ণ"
  override val matchCardTimeLeft = "বাকি"
  override val matchCardUpcoming = "আসন্ন"

  // Match Detail Screen
  override val matchDetailLudoHeader = "লুডু ১v১ ব্যাটল"
  override val matchDetailCarromHeader = "ক্যারম ১v১ ব্যাটল"
  override val matchDetailWinnerPrizeLabel = "বিজয়ী পুরস্কার"
  override fun matchDetailYouWonBanner(prize: String) = "আপনি বিজয়ী! পুরস্কার: $prize"
  override val matchDetailProofRejectedLoser = "প্রুফ বাতিল করা হয়েছে (পরাজিত)"
  override val matchDetailMatchFinishedLoser = "ম্যাচ সমাপ্ত / আপনি পরাজিত"
  override fun matchDetailRegisteredSlot(slot: String) = "আপনি নিবন্ধিত ($slot)"
  override fun matchDetailSlot(number: String) = "স্লট $number"
  override val matchDetailUpcomingNotice = "এই ম্যাচটি আপকামিং এবং অ্যাডমিন রিলিজ করার পর জয়েন করার জন্য উন্মুক্ত হবে।"
  override val matchDetailRoomCodeTitle = "গেম রুম কোড"
  override val matchDetailUnlockedBadge = "আনলক হয়েছে"
  override val matchDetailRoomCodeInstructions = "লুডু কিং / ক্যারম পুল ওপেন করে এই কোডটি দিয়ে গেমে প্রবেশ করুন"
  override val matchDetailWaitingRoomCode = "অ্যাডমিন গেম শুরু হওয়ার ৫-১০ মিনিট আগে রুম কোড প্রদান করবেন। অনুগ্রহ করে অপেক্ষা করুন।"
  override val matchDetailConfidentialNotice = "গেম রুম কোডটি গোপনীয় এবং শুধুমাত্র ম্যাচে যুক্ত প্লেয়ারদের জন্য উন্মুক্ত।"
  override val matchDetailApprovedTitle = "✓ অনুমোদিত"
  override val matchDetailRejectedTitle = "✕ প্রত্যাখ্যাত"
  override val matchDetailScreenshotSubmittedTitle = "স্ক্রিনশট জমা হয়েছে ✓"
  override val matchDetailApprovedBadge = "✓ অনুমোদিত"
  override val matchDetailRejectedBadge = "✕ প্রত্যাখ্যাত"
  override val matchDetailUnderReviewBadge = "পর্যালোচনাধীন"
  override val matchDetailReviewWaitingText = "অনুগ্রহ করে অপেক্ষা করুন, ফলাফল যাচাই করা হচ্ছে।"
  override val matchDetailNotesLabel = "নোট:"
  override val matchDetailSubmittingScreenshotTitle = "স্ক্রিনশট প্রসেস ও সাবমিট হচ্ছে..."
  override val matchDetailSubmittingScreenshotSubtitle = "অনুগ্রহ করে অপেক্ষা করুন, ফলাফল জমা দেওয়া হচ্ছে।"
  override val matchDetailProofRejectedTitle = "প্রুফ বাতিল করা হয়েছে"
  override val matchDetailProofRejectedDesc = "ফলাফল প্রত্যাখ্যাত হওয়ায় পুনরায় প্রুফ সাবমিট করার সুযোগ নেই।"
  override val matchDetailMatchFinishedTitle = "ম্যাচ সমাপ্ত / ফলাফল নিশ্চিত হয়েছে"
  override val matchDetailMatchFinishedWinnerDesc = "আপনি বিজয়ী! পুরস্কার ওয়ালেটে যুক্ত হয়েছে।"
  override val matchDetailMatchFinishedLoserDesc = "ম্যাচটি সমাপ্ত হয়েছে। নতুন কোনো প্রুফ গ্রহণ করা হবে না।"
  override val matchDetailResultApprovedTitle = "ফলাফল নিশ্চিত ও অনুমোদিত হয়েছে"
  override val matchDetailResultApprovedDesc = "আপনার জয়ের প্রমাণ সফলভাবে অনুমোদিত হয়েছে।"
  override val matchDetailDefeatedTitle = "ম্যাচ সমাপ্ত / ফলাফল নিশ্চিত হয়েছে"
  override val matchDetailDefeatedDesc = "পরাজিত খেলোয়াড়দের জন্য প্রুফ সাবমিশন প্রযোজ্য নয়।"
  override val matchDetailSubmitProofCta = "ফলাফল স্ক্রিনশট জমা দিন"
  override val matchDetailWaitingRoomCodeForProof = "অ্যাডমিন রুম কোড দেওয়ার পর স্ক্রিনশট সাবমিট করার অপশনটি চালু হবে।"
  override val matchDetailTwoMinutesWaitingText = "রুম কোড দেওয়ার ২ মিনিট পর স্ক্রিনশট সাবমিট করা যাবে"
  override fun matchDetailRegisteredPlayersTitle(count: String, max: String) = "নিবন্ধিত প্লেয়ার ($count/$max)"
  override val matchDetailNoPlayersJoined = "এখনও কোনো প্লেয়ার যুক্ত হননি। আপনিই প্রথম জয়েন করুন!"
  override val matchDetailYouBadge = "আপনি"
  override val matchDetailUpcomingButton = "আসন্ন (এখনও উন্মুক্ত হয়নি)"
  override fun matchDetailJoinMatchButton(fee: String) = "ম্যাচে জয়েন করুন ($fee)"
  override val matchDetailJoinDialogTitle = "⚠️ জরুরি নোটিশ ও নিয়মাবলী"
  override val matchDetailLudoNameNotice = "প্রিয় ইউজার, আপনার AD Tournament অ্যাপের নাম এবং Ludo King অ্যাপের নাম অবশ্যই একই হতে হবে। নাম অমিল থাকলে ফলাফল বাতিল হতে পারে।"
  override val matchDetailCarromNameNotice = "প্রিয় ইউজার, আপনার AD Tournament অ্যাপের নাম এবং Carrom Pool অ্যাপের নাম অবশ্যই একই হতে হবে। নাম অমিল থাকলে ফলাফল বাতিল হতে পারে।"
  override fun matchDetailJoinFeeNotice(fee: String) = "ম্যাচ এন্ট্রি ফি $fee আপনার ওয়ালেট থেকে কাটা হবে।"
  override val matchDetailAgreeAndJoin = "সম্মত ও জয়েন করুন"
  override val matchDetailSubmitProofTitle = "ফলাফল স্ক্রিনশট জমা দিন"
  override val matchDetailScreenshotRequired = "আপনার স্ক্রিনশট বাধ্যতামূলক দিতে হবে"
  override val matchDetailSelectScreenshot = "গ্যালারি থেকে স্ক্রিনশট নির্বাচন করুন"
  override val matchDetailChangeScreenshot = "স্ক্রিনশট পরিবর্তন করুন"
  override val matchDetailRoomCodeRequired = "রুম কোড বাধ্যতামূলক দিতে হবে"
  override val matchDetailRoomCodePlaceholder = "রুম কোড লিখুন"
  override val matchDetailMatchNumberRequired = "আপনি কত নাম্বার ম্যাচ খেলেছেন সেটাও লিখুন"
  override val matchDetailMatchNumberLabel = "ম্যাচ নাম্বার"
  override val matchDetailMatchNumberPlaceholder = "উদা: ম্যাচ #১০১"
  override val matchDetailRemarksLabel = "নোট / বিজয়ীর মন্তব্য (ঐচ্ছিক)"
  override val matchDetailRemarksPlaceholder = "উদা: লুডু কিং ১ম স্থান"
  override val matchDetailSubmitProofButton = "প্রুফ সাবমিট করুন"

  // Profile
  override val profileStatusActive = "সক্রিয়"
  override val profileStatusBanned = "ব্যান"
  override val profileStatusSuspended = "স্থগিত"
  override val profileStatusPending = "অপেক্ষমাণ"
  override val profileSupportOffline = "অফলাইন"
  override val profileLogoutDialogTitle = "লগআউট নিশ্চিত করুন"
  override val profileLogoutDialogMessage = "আপনি কি নিশ্চিত যে লগআউট করতে চান?"
  override val profileLogoutConfirmButton = "লগআউট"
  override val profilePhotoUpdateSuccess = "প্রোফাইল ছবি সফলভাবে পরিবর্তন করা হয়েছে"
  override fun profileAvatarPreset(id: String) = when {
    id.contains("dragon", ignoreCase = true) -> "ড্রাগন অ্যাভাটার"
    id.contains("cyber", ignoreCase = true) -> "সাইবার অ্যাভাটার"
    id.contains("ninja", ignoreCase = true) -> "নিনজা অ্যাভাটার"
    id.contains("crown", ignoreCase = true) -> "ক্রাউন অ্যাভাটার"
    id.contains("sniper", ignoreCase = true) -> "স্নাইপার অ্যাভাটার"
    id.contains("shield", ignoreCase = true) -> "নাইট অ্যাভাটার"
    else -> "এস্পোর্টস অ্যাভাটার"
  }

  // Deposit
  override val depositMethodLabel = "পেমেন্ট মাধ্যম"
  override val depositAmountSummaryLabel = "টাকার পরিমাণ"
  override val depositStatusLabel = "স্ট্যাটাস"
  override val depositStatusPending = "অপেক্ষমাণ"
  override val depositOfficialBadge = "অফিসিয়াল"
  override fun depositNumberCopiedToast(method: String, number: String) = "$method নম্বর কপি হয়েছে: $number"
  override val depositAmountPlaceholder = "উদা: ১০০"
  override val depositTrxIdPlaceholder = "উদা: 9J8K7L6M"
  override val depositScreenshotPreview = "পেমেন্ট স্ক্রিনশট প্রিভিউ"
  override fun depositMinAmountError(amount: String) = "সর্বনিম্ন ডিপোজিট পরিমাণ $amount"

  // Notifications
  override val notifSubtitle = "আপনার টুর্নামেন্ট ও ওয়ালেটের সাম্প্রতিক আপডেট"
  override val notifMarkAllRead = "সব পঠিত"
  override val notifFilterAll = "সকল"
  override val notifFilterUnread = "অপঠিত"
  override val notifBadgeNew = "নতুন"
  override val notifCategoryWallet = "ওয়ালেট"
  override val notifCategoryMatch = "ম্যাচ"
  override val notifCategoryRoomCode = "রুম কোড"
  override val notifCategorySystem = "সিস্টেম"
  override val notifTimeJustNow = "এইমাত্র"
  override fun notifTimeMinutesAgo(mins: String) = "$mins মিনিট আগে"
  override fun notifTimeHoursAgo(hours: String) = "$hours ঘণ্টা আগে"
  override val notifTimeYesterday = "গতকাল"
  override val notifTimeRecent = "সম্প্রতি"
  override val notifDialogClose = "ঠিক আছে"
  override val notifAllReadSuccess = "সকল নোটিফিকেশন পঠিত হয়েছে"
  override val notifEmptyFilterTitle = "কোনো অপঠিত নোটিফিকেশন নেই"
  override val notifEmptyFilterSubtext = "আপনার সব নোটিফিকেশন পঠিত হয়েছে"

  // Mini Games Shared
  override val mgWalletTitle = "মিনি গেম ওয়ালেট"
  override val mgAddMoneyTitle = "টাকা যোগ করুন"
  override val mgAddMoneySubtitle = "মেইন ব্যালেন্স থেকে গেম ওয়ালেটে টাকা আনুন"
  override val mgWithdrawMoneyTitle = "টাকা তুলুন"
  override val mgWithdrawMoneySubtitle = "গেম ব্যালেন্স থেকে জিতে নেওয়া টাকা মেইন ওয়ালেটে নিন"
  override val mgCloseButton = "বন্ধ করুন"
  override val mgTransferAddTitle = "গেম ওয়ালেটে যোগ"
  override val mgTransferWithdrawTitle = "মেইন ওয়ালেটে উত্তোলন"
  override val mgLabelUid = "ইউজার আইডি (UID)"
  override val mgLabelUserName = "ইউজারের নাম"
  override val mgLabelMobile = "মোবাইল নম্বর"
  override fun mgMainBalance(amt: String) = "আপনার মেইন ব্যালেন্স: $amt"
  override fun mgGameBalance(amt: String) = "আপনার গেম ব্যালেন্স: $amt"
  override val mgAmountPlaceholder = "পরিমাণ (৳)"
  override val mgErrorInsufficient = "ব্যালেন্স অপর্যাপ্ত!"
  override val mgErrorInvalidAmount = "সঠিক পরিমাণ লিখুন"
  override val mgConfirmRequest = "কনফার্ম রিকোয়েস্ট"
  override val mgWinTitle = "অভিনন্দন!"
  override val mgLossTitle = "দুঃখিত!"
  override fun mgWinMessage(amt: String) = "আপনি $amt জিতেছেন!"
  override val mgLossMessage = "এবার ভাগ্য সহায় হয়নি। আবার চেষ্টা করুন!"

  // Dice Game
  override val diceTitle = "জোড়-বেজোড়"
  override val diceSubtitle = "ছক্কার চাল"
  override val diceMainBalanceLabel = "মেইন ব্যালেন্স"
  override val diceGameWalletLabel = "গেম ওয়ালেট"
  override val diceAddFunds = "টাকা যোগ করুন"
  override val diceInstruction = "ভাগ্য পরীক্ষা করতে রোল করুন"
  override val diceRolling = "ডাইস ঘুরছে..."
  override val diceRollButton = "রোল করুন"
  override val diceNoBalance = "পর্যাপ্ত ব্যালেন্স নেই"
  override val diceChoiceEven = "জোড়"
  override val diceChoiceOdd = "বেজোড়"
  override val dicePayout = "পুরস্কার: ১.৯ গুণ"
  override val diceClosedBanner = "গেমটি সাময়িকভাবে বন্ধ আছে"
  override val diceToastNoBalance = "গেম ব্যালেন্স অপর্যাপ্ত"
  override val diceToastSuccess = "রোল সফল হয়েছে!"
  override val diceToastFailed = "ট্রানজেকশন ব্যর্থ হয়েছে"
  override fun diceRollResult(sum: String, type: String) = "ফলাফল: $sum ($type)"

  // Head & Tail Game
  override val headTailTitle = "হেড-টেল"
  override val headTailSubtitle = "ইনস্ট্যান্ট টস"
  override val headTailInstruction = "ভাগ্য পরীক্ষা করতে টস করুন"
  override val headTailTossing = "কয়েন ঘুরছে..."
  override val headTailTossButton = "টস করুন"
  override val headTailChoiceHead = "হেড"
  override val headTailChoiceTail = "টেল"
  override val headTailPayout = "পুরস্কার: ১.৯ গুণ"
  override val headTailClosedBanner = "গেমটি সাময়িকভাবে বন্ধ আছে"
  override val headTailToastNoBalance = "গেম ব্যালেন্স অপর্যাপ্ত"
  override val headTailToastSuccess = "টস সফল হয়েছে!"
  override val headTailToastFailed = "ট্রানজেকশন ব্যর্থ হয়েছে"
  override fun headTailResult(side: String) = "ফলাফল: $side"

  // Spin Wheel Game
  override val spinTitle = "MONEY WHEEL"
  override val spinSubtitle = "★ রয়্যাল হুইল ★"
  override val spinInstruction = "ভাগ্য পরীক্ষা করতে স্পিন করুন"
  override val spinSpinning = "স্পিন হচ্ছে..."
  override val spinButton = "স্পিন করুন"
  override val spinNoBalance = "পর্যাপ্ত ব্যালেন্স নেই"
  override val spinClosedBanner = "গেমটি সাময়িকভাবে বন্ধ আছে"
  override val spinToastNoBalance = "গেম ব্যালেন্স অপর্যাপ্ত"
  override val spinToastFailed = "ট্রানজেকশন ব্যর্থ হয়েছে"
  override fun spinResultWin(multiplier: String) = "আপনি ${multiplier}x জিতেছেন!"
  override fun spinResultAdded(amount: String) = "$amount আপনার ওয়ালেটে যুক্ত হয়েছে।"
  override val spinChipMain = "মেইন"
  override val spinChipGame = "গেম"

  // Scratch Card Game
  override val scratchTitle = "স্ক্র্যাচ কার্ড"
  override val scratchSubtitle = "ঘষে জিতুন"
  override val scratchInstructionNew = "প্রথমে নিচের বাটনে চাপ দিয়ে নতুন কার্ড নিন"
  override val scratchInstructionRevealed = "কার্ড খোলা হয়েছে"
  override val scratchInstructionScratching = "আঙুল দিয়ে কার্ডটি ঘষুন"
  override val scratchRevealAll = "পুরোটা খুলুন"
  override val scratchNewTicketButton = "নতুন কার্ড নিন"
  override val scratchCardScratching = "কার্ডটি ঘষুন..."
  override val scratchNoBalance = "অপর্যাপ্ত ব্যালেন্স"
  override val scratchClosedBanner = "গেমটি সাময়িকভাবে বন্ধ আছে"
  override val scratchToastFailed = "ট্রানজেকশন ব্যর্থ হয়েছে"
  override val scratchTransferToast = "অনুরোধটি সফলভাবে অ্যাডমিনের কাছে পাঠানো হয়েছে"
  override val scratchTryAgain = "আবার চেষ্টা করুন"
  override val scratchLossMessage = "এবার কোনো মিল পাওয়া যায়নি। আবার চেষ্টা করুন!"
  override fun scratchWinX(multiplier: String) = "${multiplier}X উইন"
  override fun scratchWinMessage(amount: String) = "আপনি $amount জিতেছেন!\nটাকা আপনার গেম ওয়ালেটে যুক্ত হয়েছে।"

  // Auto Ludo Lobby
  override val autoLudoHeaderTitle = "ম্যাচ লিস্ট"
  override val autoLudoCreateBannerText = "আপনি নিজের মন মত ম্যাচ তৈরি করতে চাইলে নিচের বাটনে ক্লিক করে তৈরি করুন।"
  override val autoLudoCreateButton = "ম্যাচ তৈরি করুন"
  override val autoLudoFilterAll = "সব ম্যাচ"
  override val autoLudoFilterRecent = "সাম্প্রতিক ম্যাচ"
  override val autoLudoFilterMy = "আমার ম্যাচ"
  override val autoLudoFilterFree = "ফ্রি ম্যাচ"
  override val autoLudoNoMatches = "কোন ম্যাচ পাওয়া যায়নি"
  override val autoLudoAutoMatchBadge = "🤖 এটা সম্পূর্ণ অটো ম্যাচ"
  override val autoLudoMyMatchBadge = "আপনার ম্যাচ"
  override val autoLudoPrizeLabel = "প্রাইজ"
  override val autoLudoEntryLabel = "এন্ট্রি"
  override val autoLudoStatusOpponentMissing = "❌ প্রতিপক্ষ আসেনি"
  override val autoLudoStatusPlayerAbsent = "❌ প্লেয়ার অনুপস্থিত"
  override val autoLudoStatusStarting = "ম্যাচ শুরু হচ্ছে..."
  override val autoLudoStatusPlaying = "ম্যাচ চলছে"
  override val autoLudoStatusFull = "ম্যাচ পূর্ণ হয়েছে"
  override val autoLudoStatusWaitingOpponent = "প্রতিপক্ষের জন্য অপেক্ষা"
  override val autoLudoStatusWaitingTwo = "২ জন হলেই ম্যাচ শুরু হবে"
  override fun autoLudoPlayersCount(current: Int, max: Int) = "প্লেয়ার: ${number(current)}/${number(max)}"
  override fun autoLudoCardTitle(number: String) = "অটো লুডু · ম্যাচ নং #$number"
  override val autoLudoTutorialButton = "টিউটোরিয়াল"
  override val autoLudoRefundCancelled = "বাতিল (টাকা ফেরত)"
  override val autoLudoRefundAbsent = "অনুপস্থিত (টাকা ফেরত)"
  override val autoLudoMatchWonFinished = "🏆 আপনি জিতেছেন · ম্যাচ শেষ"
  override val autoLudoMatchLostFinished = "আপনি হেরেছেন · ম্যাচ শেষ"
  override val autoLudoMatchFinished = "ম্যাচ শেষ"
  override val autoLudoWaitingStatus = "অপেক্ষায় আছেন"
  override fun autoLudoStartingIn(time: String) = "শুরুর বাকি: $time"
  override val autoLudoEnterGameButton = "খেলায় প্রবেশ করুন"
  override val autoLudoMatchFullButton = "ম্যাচ ফুল"
  override val autoLudoTimeExpired = "সময় শেষ"
  override val autoLudoJoinButton = "জয়েন করুন"
  override val autoLudoCreateDialogTitle = "নতুন ম্যাচ তৈরি করুন"
  override val autoLudoGameWalletBalance = "গেম ওয়ালেট ব্যালেন্স"
  override val autoLudoEntryFeeHeader = "এন্ট্রি ফি (১০ - ৫০০)"
  override val autoLudoEntryFeePlaceholder = "এন্ট্রি ফি দিন"
  override val autoLudoStartTimeLabel = "ম্যাচ শুরুর সময় (আজ)"
  override val autoLudoFutureTimeError = "অনুগ্রহ করে বর্তমান সময়ের পরের সময় নির্বাচন করুন"
  override val autoLudoWinningPrizeLabel = "উইনিং প্রাইজ:"
  override val autoLudoWinningPrizeSub = "ম্যাচ শেষে আপনার ওয়ালেটে যোগ হবে"
  override fun autoLudoInsufficientBalanceCreate(required: String, current: String) = "আপনার গেম ওয়ালেটে পর্যাপ্ত ব্যালেন্স নেই (প্রয়োজন: $required, বর্তমান ব্যালেন্স: $current)"
  override val autoLudoConfirm = "নিশ্চিত করুন"
  override val autoLudoJoinDialogTitle = "ম্যাচে জয়েন করুন"
  override fun autoLudoJoinFeeNotice(amount: String) = "এন্ট্রি ফি: $amount কাটা হবে"
  override fun autoLudoBalanceLabel(amount: String) = "ব্যালেন্স: $amount"
  override val autoLudoMatchTimeExpiredError = "ম্যাচ শুরুর সময় উত্তীর্ণ হয়ে গেছে!"
  override fun autoLudoInsufficientBalanceJoin(amount: String) = "আপনার গেম ওয়ালেটে পর্যাপ্ত ব্যালেন্স নেই\n(প্রয়োজন: $amount)"
  override val autoLudoTimeExpiredToast = "ম্যাচ শুরুর সময় উত্তীর্ণ হয়ে গেছে!"
  override val autoLudoTutorialSoonToast = "টিউটোরিয়াল শীঘ্রই আসছে"
  override val autoLudoMatchCreatedSuccess = "ম্যাচ সফলভাবে তৈরি হয়েছে"
  override val autoLudoMatchCreatedFail = "ম্যাচ তৈরিতে ব্যর্থ হয়েছে"
  override val autoLudoJoinSuccessToast = "ম্যাচে সফলভাবে জয়েন করেছেন! নির্ধারিত সময়ে প্রবেশ করুন।"
  override val autoLudoJoinFailToast = "জয়েন করতে ব্যর্থ হয়েছে"
  override val autoLudoTransferReqSuccess = "অনুরোধটি সফলভাবে অ্যাডমিনের কাছে পাঠানো হয়েছে"
  override fun autoLudoTransferReqFail(err: String) = "অনুরোধ পাঠাতে ব্যর্থ হয়েছে: $err"
  override val autoLudoMatchEndedToast = "ম্যাচ শেষ হয়েছে! ফলাফল নির্ধারিত হয়েছে।"
  override val autoLudoCancelOpponentMissing = "প্রতিপক্ষ না পাওয়ায় ম্যাচ বাতিল ও টাকা ফেরত দেওয়া হয়েছে।"
  override val autoLudoCancelBothNotReady = "উভয় প্লেয়ার প্রস্তুত না থাকায় ম্যাচ বাতিল করা হয়েছে।"
  override val autoLudoHourLabel = "ঘণ্টা"
  override val autoLudoMinLabel = "মিনিট"
  override val autoLudoAmPmLabel = "এএম/পিএম"

  // Ready Room Dialog
  override val readyRoomTitle = "লুডু ব্যাটল রুম"
  override fun readyRoomEntryPrize(entry: String, prize: String) = "এন্ট্রি $entry · পুরস্কার $prize"
  override val readyRoomVs = "VS"
  override fun readyRoomPlayer(num: Int) = "প্লেয়ার ${number(num)}"
  override val readyRoomNotReady = "অপ্রস্তুত"
  override val readyRoomReady = "প্রস্তুত ✓"
  override val readyRoomStartsIn = "ম্যাচ শুরু হতে বাকি"
  override val readyRoomPlayButton = "খেলা শুরু করুন"
  override val readyRoomGoBack = "পিছনে যান"
  override val readyRoomPlayerYou = "আপনি"
  override val readyRoomWaitingOpponent = "প্রতিপক্ষের অপেক্ষায়..."
  override val readyRoomTimeUpDeciding = "সময় শেষ · ফলাফল নির্ধারণ হচ্ছে..."
  override val readyRoomStartGame = "খেলা শুরু করুন"
  override val readyRoomBack = "পিছনে যান"
  override val readyRoomStarting = "খেলা শুরু হচ্ছে..."

  // AutoLudo Logs
  override fun autoLudoLog(raw: String) = when (raw.lowercase()) {
      "auto-move! strike +1" -> "অটো-মুভ! স্ট্রাইক +১"
      "auto-pass! strike +1" -> "অটো-পাস! স্ট্রাইক +১"
      "auto-roll! strike +1" -> "অটো-রোল! স্ট্রাইক +১"
      "both players absent. match cancelled." -> "উভয় প্লেয়ার অনুপস্থিত। ম্যাচ বাতিল।"
      "captured! bonus roll" -> "গুটি কাটা! বোনাস চাল"
      "disqualified (3 misses)!" -> "খেলোয়াড় বাদ পড়েছেন (৩ বার মিস)!"
      "goal! bonus roll" -> "ঘরে পৌঁছেছে! বোনাস চাল"
      "no moves! turn passed" -> "কোনো চাল নেই! পালা বদল"
      "pawn moved" -> "চাল দেওয়া হয়েছে"
      "player 1 no-show. player 2 walkover victory!" -> "প্লেয়ার ১ অনুপস্থিত। প্লেয়ার ২ জয়ী!"
      "player 2 no-show. player 1 walkover victory!" -> "প্লেয়ার ২ অনুপস্থিত। প্লেয়ার ১ জয়ী!"
      "player disqualified (3 misses)!" -> "খেলোয়াড় বাদ পড়েছেন (৩ বার মিস)!"
      "player surrendered. opponent declared winner." -> "প্লেয়ার সারেন্ডার করেছেন! প্রতিপক্ষ জয়ী।"
      "rolled 6! bonus turn" -> "৬ উঠেছে! বোনাস চাল"
      "opponent absent. walkover victory declared." -> "প্রতিপক্ষ অনুপস্থিত। ওয়াকওভার জয়!"
      else -> raw
  }

  override fun displayActionLog(raw: String) = autoLudoLog(raw)
  
  // AutoLudo Result Dialog
  override val autoLudoResultWinTitle = "অভিনন্দন! আপনি বিজয়ী! 🏆"
  override val autoLudoResultWinReason = "অভিনন্দন! আপনি বিজয়ী হয়েছেন!"
  override fun autoLudoResultWinPrize(prize: String) = "+$prize গেম ওয়ালেটে যোগ হয়েছে"
  override val autoLudoResultLoseTitle = "এবার জেতা হয়নি"
  override fun autoLudoResultLoseReason(winnerName: String) = "$winnerName গেমটি জিতেছেন। ভালো খেলার জন্য ধন্যবাদ।"
  override val autoLudoResultOk = "ঠিক আছে"
  
  // AutoLudo Exit Dialog
  override val autoLudoExitTitle = "আপনি কি ম্যাচ থেকে বের হতে চান?"
  override fun autoLudoExitMessage(opponent: String) = "আপনি বের হয়ে গেলে $opponent-কে বিজয়ী ঘোষণা করা হবে।"
  override val autoLudoExitConfirm = "হ্যাঁ, বের হব"
  override val autoLudoExitCancel = "না"
  override val autoLudoExitYes = "হ্যাঁ"
  override val autoLudoExitNo = "না"
}

object EnglishStrings : AppStrings {
  // Brand & General
  override val appName = "AD Tournament"
  override val appTagline = "1v1 ESPORTS BATTLES"
  override val loading = "Loading..."
  override val errorGeneric = "Something went wrong"
  override val retry = "Retry"
  override val cancel = "Cancel"
  override val save = "Save"
  override val close = "Close"
  override val back = "Back"
  override val offlineMode = "No Internet Connection - Offline Mode"
  override val optional = "Optional"
  override val locked = "LOCKED"
  override val adminPanel = "Admin Panel"

  // Navigation
  override val navHome = "Home"
  override val navWallet = "Wallet"
  override val navMatches = "Matches"
  override val navHistory = "History"
  override val navProfile = "Profile"

  // Auth
  override val phoneNumber = "Mobile Number"
  override val enterPhoneNumber = "Enter your 11-digit mobile number"
  override val password = "Password"
  override val enterPassword = "Enter your password"
  override val fullName = "Full Name"
  override val enterFullName = "Enter your full name"
  override val confirmPassword = "Confirm Password"
  override val enterConfirmPassword = "Re-enter your password"
  override val invalidPhoneError = "Please enter a valid 11-digit mobile number"
  override val passwordMinLengthError = "Password must be at least 6 characters"
  override val passwordsDoNotMatchError = "Passwords do not match"
  override val nameRequiredError = "Please enter your name"
  override val otpRequiredError = "Please enter a valid 6-digit OTP code"

  // Auth - Login
  override val loginTitle = "Welcome Back"
  override val loginSubtitle = "Enter your phone number and password to enter the battle arena"
  override val loginButton = "LOGIN"
  override val forgotPasswordLink = "Forgot Password?"
  override val noAccountRegister = "Don't have an account? Register"
  override val playerSignIn = "Player Sign In"
  override val playerSignInSubtitle = "Enter your 11-digit mobile number and password"
  override val signInButton = "SIGN IN"
  override val noAccountPrompt = "Don't have an account?"
  override val createAccountAction = "Create Account"
  override val togglePassword = "Toggle password"
  override val phoneEmptyError = "Mobile number cannot be empty"
  override val passwordEmptyError = "Password cannot be empty"
  override val invalidCredentialsError = "Invalid mobile number or password"
  override val accountNotFoundError = "No account registered with this mobile number"

  // Auth - Register
  override val registerTitle = "Create Account"
  override val registerSubtitle = "Join thousands of players in 1v1 esports"
  override val registerButton = "REGISTER"
  override val alreadyHaveAccountLogin = "Already have an account? Login"

  // Auth - Forgot Password & OTP
  override val forgotPasswordTitle = "Reset Password"
  override val forgotPasswordSubtitle = "Enter your mobile number to receive a verification code"
  override val sendOtpButton = "SEND OTP"
  override val verifyOtpTitle = "Verify OTP"
  override val verifyOtpSubtitle = "Enter the 6-digit OTP code sent to your phone"
  override val verifyOtpButton = "VERIFY"
  override val resetPasswordTitle = "New Password"
  override val resetPasswordSubtitle = "Set a new secure password for your account"
  override val resetPasswordButton = "CONFIRM PASSWORD"
  override val backToLogin = "Back to Login"

  // Home Screen
  override val balanceCurrent = "Current Balance"
  override val depositAction = "Deposit"
  override val withdrawAction = "Withdraw"
  override val quickMatches = "Matches"
  override val quickWallet = "Wallet"
  override val quickRules = "Rules"
  override val quickSupport = "Support"
  override val featuredBattles = "FEATURED BATTLES"
  override val liveAndUpcoming = "LIVE & UPCOMING"
  override val filterAll = "ALL"
  override val filterLudo = "LUDO"
  override val filterCarrom = "CARROM"
  override val noMatchesAvailable = "No matches available"
  override val noMatchesSubtext = "Check back soon for new tournaments"
  override val tournamentsHeading = "TOURNAMENTS"
  override val allMatchesQuickTitle = "ALL MATCHES"
  override val allMatchesQuickSubtitle = "All Ludo & Carrom 1v1 matches in one place"
  override val filterAllGames = "ALL GAMES"
  override val availableMatchesTitle = "AVAILABLE MATCHES"
  override val loadingMatches = "Loading available matches..."
  override val failedToLoadMatches = "Failed to load matches"
  override val noAvailableMatchesTitle = "No Available Matches"
  override val noAvailableMatchesSubtext = "All current contests are either in progress or filled. New 1v1 matches are posted frequently by Super Admin."
  override val refreshMatches = "Refresh Matches"
  override fun viewAllMatchesCount(count: Int) = "VIEW ALL MATCHES ($count)"
  override val officialVideoNotice = "ℹ️ Official video tutorials will be available in the app soon."
  override val depositGuideTitle = "How to Deposit?"
  override val depositGuideSub = "Easily add money using bKash / Nagad"
  override val matchJoinGuideTitle = "How to Join Match?"
  override val matchJoinGuideSub = "Select match and join 1v1 battle"
  override val resultGuideTitle = "How to Submit Result?"
  override val resultGuideSub = "Claim victory with screenshot after match"
  override val rulesGuideTitle = "Tournament Rules"
  override val rulesGuideSub = "Read fair play and tournament guidelines"
  override val viewGuideCta = "▶ View Guide"

  override fun homeReferTitle(amount: String) = "Get a $amount bonus for referral!"
  override val homeReferSubtitle = "Cash reward on first deposit"
  override val homeReferCta = "Refer Now >"
  override val homeQuickDepositSub = "Add Cash"
  override val homeQuickWithdrawSub = "Cash Out"
  override val homeAutoGamesHeading = "★ AUTO GAMES ★"
  override val homeAutoLudoTitle = "Auto Ludo"
  override val homeAutoLudoSub = "Instant Board Game"
  override val homeAutoLudoBadge = "1v1 FAST"
  override val homeCarromTitle = "Carrom Pool"
  override val homeCarromSub = "Board Strike Battle"
  override val homeNewBadge = "NEW"
  override val homeCarromComingSoon = "Carrom Pool coming soon"
  override val homeNoticeTitle = "📢 Important Notice"
  override val homeOk = "OK"
  override val homeBadgeDeposit = "DEPOSIT GUIDE"
  override val homeBadgeJoin = "JOIN GUIDE"
  override val homeBadgeResult = "RESULT GUIDE"
  override val homeBadgeRules = "FAIR PLAY RULES"
  override val homeEmptyMatchesTitle = "No matches currently available"



  // Wallet
  override val walletTitle = "WALLET BALANCE"
  override val totalBalance = "Total Balance"
  override val availableToPlay = "Available to Play"
  override val lockedInContests = "Locked in Contests"
  override val recentTransactions = "RECENT TRANSACTIONS"
  override val viewAll = "View All"
  override val noTransactionsYet = "No transactions found"
  override val noTransactionsSubtext = "Your deposits, withdrawals, and match transactions will appear here"
  override val walletSubtitle = "Your Earnings • Safe & Secure"
  override val statDeposited = "DEPOSITED"
  override val statWinnings = "WINNINGS"
  override val statWithdrawn = "WITHDRAWN"
  override val txnPrize = "Tournament Prize"
  override val txnRefund = "Match Refund"
  override val txnEntryFee = "Match Entry Fee"
  override fun pendingEscrowNotice(amount: String) = "Pending in Escrow / Withdrawal: ৳$amount"
  override val walletMainCashLabel = "Main Cash Balance"
  override val walletBonusLabel = "Bonus Balance"
  override val walletBonusUsageHint = "10% bonus can be used in each match"
  override val walletTxDeposit = "Deposit"
  override val walletTxWithdraw = "Withdrawal"
  override val walletTxMatchJoin = "Match Participation"
  override val walletTxMatchRefund = "Match Refund"
  override val walletTxPrize = "Tournament Prize"
  override val walletTxMiniGameTransferIn = "Transfer from Game"
  override val walletTxMiniGameTransferOut = "Transfer to Game"
  override val walletTxAdminAdjustment = "Admin Adjustment"
  override val walletTxBonus = "Bonus Credit"
  override val walletTxOther = "Other Transaction"
  override val walletTxStatusCompleted = "Completed"
  override val walletTxStatusPending = "Pending"
  override val walletTxStatusRejected = "Rejected"
  override val walletTxStatusCancelled = "Cancelled"
  override val walletTxDescDeposit = "Money added to your wallet"
  override val walletTxDescWithdraw = "Funds withdrawn from wallet"
  override val walletTxDescMatchJoin = "Match entry fee deducted"
  override val walletTxDescMatchRefund = "Refund for a cancelled match"
  override val walletTxDescPrize = "Prize added for winning a match"
  override val walletTxDescTransferIn = "Transfer from mini-game to wallet"
  override val walletTxDescTransferOut = "Transfer from wallet to mini-game"
  override val walletTxDescBonus = "Promotional bonus added"
  override val walletTxDescDefault = "Transaction processed successfully"
  override fun walletTxDescMatch(matchId: String) = "Transaction for Match #$matchId"

  // Deposit Screen
  override val depositTitle = "ADD MONEY / DEPOSIT"
  override val selectPaymentMethod = "Select Payment Method"
  override val paymentMethodBkash = "bKash"
  override val paymentMethodNagad = "Nagad"
  override val sendMoneyInstructions = "Send Money (Personal) to official number, then submit your transaction ID."
  override val copyNumber = "Copy Number"
  override val numberCopied = "Number copied to clipboard"
  override val depositAmount = "Deposit Amount"
  override val enterDepositAmount = "Enter amount"
  override val trxIdLabel = "Transaction ID (TrxID)"
  override val enterTrxId = "Enter TrxID"
  override val senderMobileNumber = "Sender Mobile Number"
  override val enterSenderMobile = "Mobile number you sent from"
  override val screenshotProofLabel = "Screenshot Proof (Optional)"
  override val submitDepositButton = "SUBMIT DEPOSIT"
  override val depositProcessingTime = "Deposits are typically verified and added within 5-15 minutes"
  override val depositSecurityWarning = "Submitting fake TrxID or fraudulent proofs will result in permanent ban."
  override fun minDepositNotice(amount: String) = "Minimum deposit is ৳$amount"
  override val depositSubmittedTitle = "Deposit Request Submitted"
  override val depositSubmittedDesc = "Your deposit has been submitted for admin verification. It will be credited within 10-15 minutes."
  override val backToWallet = "BACK TO WALLET"
  override val addMoneyTitle = "ADD MONEY"
  override val depositSubtitle = "Deposit to play • Fast & Secure"
  override val stepSelectMethod = "1. Select Payment Method"
  override val selectedBadge = "Selected"
  override val personalBadge = "Personal"
  override fun sendMoneyLabel(method: String) = "Send Money ($method Personal):"
  override val copied = "COPIED"
  override val copy = "COPY"
  override val howToDepositTitle = "How to deposit:"
  override val stepDepositDetails = "2. Deposit Details"
  override val depositRange = "Min ৳50 • Max ৳25,000"
  override val enterAmountLabel = "Enter Amount (BDT)"
  override val senderMobileLabel = "Sender Mobile Number"
  override val paymentScreenshotLabel = "Payment Screenshot"
  override val uploadScreenshotTitle = "UPLOAD PAYMENT SCREENSHOT"
  override val uploadScreenshotSub = "Tap to choose image from gallery"
  override val screenshotAttached = "Screenshot attached"
  override val replace = "Replace"
  override val remove = "Remove"
  override val depositSecurityNotice = "Your payment screenshot and transaction details are securely submitted for admin verification."
  override val depositMinError = "Minimum deposit amount is ৳ 50"
  override val depositMaxError = "Maximum deposit amount is ৳ 25,000"
  override val invalidMobileError = "Please enter a valid 11-digit sender mobile number"
  override val invalidTrxIdError = "Please enter a valid Transaction ID"
  override val depositDuplicateError = "A deposit request with the same sender number, transaction ID, and amount has already been submitted."
  override val depositLimitExceededError = "You can submit at most 3 deposit requests within 20 minutes. Please wait before submitting again."

  // Withdraw Screen
  override val withdrawTitle = "WITHDRAW MONEY"
  override val withdrawAmount = "Withdraw Amount"
  override val enterWithdrawAmount = "Enter amount"
  override val recipientMobileNumber = "Recipient Mobile Number"
  override val enterRecipientMobile = "Mobile number to receive payment"
  override val requestWithdrawalButton = "REQUEST WITHDRAWAL"
  override val withdrawProcessingTime = "Withdrawal requests are processed within 30-60 minutes"
  override fun availableBalanceNotice(amount: String) = "Available Balance: ৳$amount"
  override fun minWithdrawNotice(amount: String) = "Minimum withdrawal is ৳$amount"

  // Withdraw Screen Extensions
  override val withdrawHeaderTitle = "WITHDRAW"
  override val withdrawHeaderSubtitle = "Cash out your winnings • Fast & Secure"
  override val withdrawSelectMethodLabel = "SELECT PAYMENT METHOD"
  override val withdrawMethodSelected = "Selected"
  override val withdrawMethodPersonal = "Personal"
  override val withdrawAmountLabel = "WITHDRAW AMOUNT"
  override val withdrawAvailableBalanceLabel = "AVAILABLE BALANCE"
  override fun withdrawLimitNotice(min: String) = "Min ৳$min • Max ৳10,000"
  override val withdrawEnterAmountHint = "Enter Amount (BDT)"
  override val withdrawEnterAmountPlaceholder = "e.g. 200"
  override fun withdrawRecipientLabel(method: String) = "RECIPIENT $method NUMBER"
  override val withdrawUseSavedNumber = "Use Saved Number"
  override fun withdrawRecipientHint(method: String) = "Your Personal $method Number"
  override val withdrawRecipientPlaceholder = "01XXXXXXXXX"
  override fun withdrawVerifiedBadge(method: String) = "Verified Saved $method Account"
  override fun withdrawSecurityHint(method: String) = "Ensure this $method account belongs to you for prompt payout."
  override val withdrawSummaryTitle = "WITHDRAWAL SUMMARY"
  override val withdrawSummaryAmount = "Withdrawal Amount"
  override val withdrawSummaryMethod = "Payment Method"
  override val withdrawSummaryNumber = "Receiving Number"
  override val withdrawSummaryTotal = "Total to Receive"
  override val withdrawSecurityTitle = "SECURITY & GUIDELINES"
  override val withdrawGuideline1 = "• At most ONE active or pending withdrawal is permitted at a time."
  override fun withdrawGuideline2(min: String) = "• Minimum withdrawal is ৳$min (Max ৳10,000 per transaction)."
  override val withdrawGuideline3 = "• Withdrawal amount is held securely in pending funds until processed."
  override val withdrawGuideline4 = "• Admin approval & verification is required."
  override val withdrawGuideline5 = "• If rejected, the held amount is automatically released back to your wallet."
  override val withdrawNowButton = "WITHDRAW NOW"
  override fun withdrawMinError(min: String) = "Minimum withdrawal amount is ৳ $min"
  override val withdrawMaxError = "Maximum withdrawal amount is ৳ 10,000"
  override fun withdrawInsufficientError(balance: String) = "Insufficient available balance. You have ৳ $balance"
  override val withdrawInvalidMobileError = "Please enter a valid 11-digit recipient mobile number"
  override val withdrawSuccessTitle = "Withdrawal Requested"
  override fun withdrawSuccessDesc(amount: String, method: String) = "Your withdrawal request of $amount via $method has been submitted for admin processing."
  override val withdrawStatusLabel = "Status"
  override val withdrawStatusPending = "PENDING ADMIN APPROVAL"
  override val withdrawPayoutMethodLabel = "Payout Method"
  override val withdrawRecipientAccountLabel = "Recipient Account"
  override val withdrawRequestedAmountLabel = "Requested Amount"
  override val withdrawBackToWallet = "BACK TO WALLET"
  override val txHistoryTitle = "TRANSACTION HISTORY"
  override val txHistorySubtitle = "Track your deposits, match entries, winnings and withdrawals"
  override val txHistoryFilterAll = "ALL"
  override val txHistoryFilterDeposit = "DEPOSITS"
  override val txHistoryFilterMatch = "MATCHES"
  override val txHistoryFilterWinning = "WINNINGS"
  override val txHistoryFilterWithdraw = "WITHDRAWALS"
  override val txHistoryEmptyTitle = "No transactions yet"
  override val txHistoryEmptyDesc = "Your completed deposits, match entries, and withdrawals will be safely recorded here."
  override val txHistoryEmptyFilterTitle = "No transactions found"
  override val txHistoryEmptyFilterDesc = "No transaction data found for the selected filter."
  override val txHistoryRefLabel = "Ref:"
  override val txHistoryCopyAction = "COPY"
  override val txHistoryCopied = "Copied"

  // Transaction History Screen
  override val transactionHistoryTitle = "TRANSACTION HISTORY"
  override val filterDeposits = "DEPOSITS"
  override val filterWithdrawals = "WITHDRAWALS"
  override val statusCompleted = "COMPLETED"
  override val statusPending = "PENDING"
  override val statusRejected = "REJECTED"
  override val statusProcessing = "PROCESSING"

  // Matches Lobby
  override val tournamentsTitle = "1v1 TOURNAMENTS"
  override val entryFeeLabel = "ENTRY"
  override val prizePoolLabel = "PRIZE"
  override val playersLabel = "PLAYERS"
  override val joinNowButton = "JOIN NOW"
  override val matchFull = "FULL"
  override val matchLive = "LIVE"

  // Match Details
  override val matchDetailTitle = "MATCH DETAILS"
  override val roomCodeLabel = "ROOM CODE"
  override val copyRoomCodeButton = "COPY CODE"
  override val roomCodeCopiedNotice = "Room code copied to clipboard"
  override val playersHeader = "PLAYERS"
  override val waitingForOpponent = "Waiting for opponent..."
  override val matchRulesHeader = "MATCH RULES"
  override val submitResultHeader = "SUBMIT RESULT"
  override val actionWon = "I WON"
  override val actionLost = "I LOST"
  override val uploadWinnerScreenshot = "Upload Winner Screenshot"
  override val resultWarningNotice = "Fake screenshots or false claims result in permanent ban and forfeiture."

  // Match History
  override val matchHistoryTitle = "MATCH HISTORY"
  override val matchHistorySubtitle = "Review your past 1v1 battles, winnings and results."
  override val totalPlayed = "TOTAL PLAYED"
  override val totalWon = "WON"
  override val winRateLabel = "WIN RATE"
  override val badgeVictory = "WINNER"
  override val badgeDefeat = "LOST"
  override val badgeRefunded = "REFUNDED"
  override val badgePending = "RESULT PENDING"
  override val badgeUnderReview = "UNDER REVIEW"
  override val badgeRejected = "REJECTED"
  override val matchCancelled = "Match Cancelled"
  override val cancellationReason = "Cancellation Reason"
  override val labelGame = "Game:"
  override val labelEntry = "Entry:"
  override val labelPrize = "Prize:"
  override val labelRefund = "Refund:"
  override val noMatchHistoryTitle = "NO MATCH HISTORY YET"
  override val noMatchHistorySubtext = "Your completed 1v1 battles will appear here."
  override val noFilteredMatchesTitle = "NO MATCHES FOUND"
  override val noFilteredMatchesSubtext = "Try another game filter."
 
  // Matches Screen Specific
  override val matchesHeaderTitle = "Tournament Matches"
  override val matchesHeaderSubtitle = "Real-time 1v1 Ludo & Carrom Battles"
  override val tabAvailable = "Available"
  override val tabMyJoined = "My Joined"
  override val tabUpcoming = "Upcoming"
  override val filterLudo1v1 = "Ludo 1v1"
  override val filterCarrom1v1 = "Carrom 1v1"
  override val labelFormat = "FORMAT"
  override val label1v1Battle = "1v1 Battle"
  override val labelJoined = "JOINED"
  override val labelMatchCompleted = "Match Completed"
  override val labelStartsAt = "Starts at"
  override val labelViewResult = "View Result"
  override val actionJoinNowCta = "Join Now"
  override val emptyAvailableTitleLabel = "No Available Matches"
  override val emptyAvailableDescLabel = "All current contests are either in progress or filled. New 1v1 matches are posted frequently by Super Admin."
  override val emptyJoinedTitleLabel = "No Joined Matches"
  override val emptyJoinedDescLabel = "You have not joined any active tournaments yet. Check the Available tab to enter a 1v1 match!"
  override val emptyUpcomingTitleLabel = "No Upcoming Tournaments"
  override val emptyUpcomingDescLabel = "Upcoming scheduled contests will appear here. Stay tuned for special weekend tournaments!"
  override val emptyHistoryTitleLabel = "No Match History"
  override val emptyHistoryDescLabel = "Your completed matches and tournament results will be archived here."
  override val browseAvailableCta = "Browse Available Matches"
  override fun loadingTabMatches(tab: String) = "Loading $tab matches..."


  // Notifications
  override val notificationsTitle = "NOTIFICATIONS"
  override val noNotificationsTitle = "No notifications yet"
  override val noNotificationsSubtext = "Updates on matches and wallet will appear here"

  // Profile
  override val profileTitle = "Player Profile"
  override val playerLabel = "Player"
  override val labelId = "ID:"
  override val mobileLockedNotice = "Mobile number cannot be changed from the app for security & anti-fraud protection."
  override val careerStatistics = "CAREER STATISTICS"
  override val statMatches = "MATCHES"
  override val statMatchesSub = "1v1 Battles"
  override val statWins = "WINS"
  override val statLosses = "LOSSES"
  override val statLossesSub = "Contests"
  override val statTotalWinnings = "TOTAL WINNINGS"
  override val statAllTimeRewards = "All-Time Rewards"
  override val statMemberSince = "MEMBER SINCE"
  override val statVerifiedPlayer = "Verified Player"
  override val matchHistoryMenu = "Match History"
  override val matchHistoryMenuSub = "Past battles, game outcomes, and rewards"
  override val fairPlayTitle = "FAIR PLAY & INTEGRITY"
  override val languageSettingTitle = "অ্যাপের ভাষা / Language"
  override val languageSettingSubtitle = "Select your preferred language"
  override val languageBengali = "বাংলা (Default)"
  override val languageEnglish = "English"
  override val signOutButton = "SIGN OUT"
  override val updateDisplayNameTitle = "Update Display Name"
  override val enterNewNamePlaceholder = "Enter new display name"
  override val nameUpdateSuccess = "Display name updated successfully"
  override fun userCopied(id: String) = "User ID $id copied to clipboard"
  override fun winRateSubtitle(rate: Int) = "$rate% Win Rate"
  override fun fairPlayText(status: String) =
    "• Account Status: $status\n• Single Account Policy: One verified mobile number per player\n• Zero Tolerance: Cheating, fake screenshots, or match abandonment results in permanent account suspension and wallet forfeiture."

  // Support
  override val supportTitle = "Help & Support"
  override val supportWhatsapp = "WhatsApp Support"
  override val supportTelegram = "Official Telegram Channel"
  override val supportFaq = "Frequently Asked Questions (FAQ)"
  override val helpAndSupportTitle = "Help & Support Center"
  override val helpAndSupportSubtitle = "24/7 assistance for all your tournament inquiries"
  override val aiSupportTitle = "AI Live Support"
  override val aiSupportSubtitle = "Instant automated assistance for common queries"
  override val liveAdminChatTitle = "Admin Live Chat"
  override val liveAdminChatSubtitle = "Direct one-on-one conversation with support staff"
  override val supportTicketsTitle = "Support Tickets & Issues"
  override val supportTicketsSubtitle = "Track submitted complaints and resolution status"
  override val contactUnavailable = "Currently Unavailable"
  override val changePhotoTitle = "Change Profile Picture"
  override val chooseAvatarPreset = "Choose Esports Avatar"
  override val uploadFromGallery = "Upload from Gallery"
  override val statRecentlyJoined = "Recently Joined"
  override val comingSoonTitle = "Coming Soon"
  override val comingSoonMessage = "This feature will be enabled in an upcoming release."
  override val loginPromptToViewProfile = "Please sign in to view your profile"

  // Rules
  override val rulesTitle = "Tournament Rules"
  override val rulesLudo = "Ludo Tournament Rules"
  override val rulesCarrom = "Carrom Tournament Rules"
  override val rulesFairPlay = "Fair Play Guidelines"
  override val topBarSubtitle = "1V1 ESPORTS BATTLE"

  override val miniGamesTitle = "Mini Games"
  override val miniGamesViewAll = "View All"
  override val miniGamesRoyalWheelTitle = "Royal Wheel"
  override val miniGamesRoyalWheelSub = "Spin to Win"
  override val miniGamesRoyalWheelBadge = "HOT"
  override val miniGamesRoyalWheelPayout = "Max 50x"
  override val miniGamesDiceTitle = "Even-Odd"
  override val miniGamesDiceSub = "Dice Roll"
  override val miniGamesDiceBadge = "1.9x WIN"
  override val miniGamesDicePayout = "Payout: 1.9x"
  override val miniGamesHeadTailTitle = "Head-Tail"
  override val miniGamesHeadTailSub = "Instant Toss"
  override val miniGamesHeadTailBadge = "POPULAR"
  override val miniGamesScratchTitle = "Scratch Card"
  override val miniGamesScratchSub = "Scratch & Win"
  override val miniGamesScratchBadge = "JACKPOT"
  override val miniGamesScratchAction = "Scratch & Win"
  
  // History
  override val historyResultWon = "WINNER ������"
  override val historyResultLost = "LOST"
  override val historyResultPending = "PENDING"
  override val historyResultCancelled = "CANCELLED"
  override val historyDateCompleted = "Completed"
  override val historyGameLabel = "Game:"
  override val historyEntryLabel = "Entry:"
  override val historyPrizeLabel = "Prize:"
  override val historyRefundLabel = "Refund:"
  override fun historyMatchTitle(game: String, id: String) =
    "${game.lowercase().replaceFirstChar { it.uppercase() }} 1v1 #$id"

  // Match Card
  override val matchCardLudoChip = "Ludo 1v1"
  override val matchCardCarromChip = "Carrom 1v1"
  override val matchCardStatusCompleted = "COMPLETED"
  override val matchCardStatusJoined = "JOINED"
  override val matchCardStatusCancelled = "CANCELLED"
  override val matchCardStatusPaused = "PAUSED"
  override val matchCardStatusDisabled = "DISABLED"
  override val matchCardStatusUpcoming = "UPCOMING"
  override val matchCardStatusRunning = "RUNNING"
  override val matchCardStatusReview = "REVIEW"
  override val matchCardStatusCodeReady = "CODE READY"
  override fun matchCardStatusFull(count: String, max: String) = "FULL ($count/$max)"
  override fun matchCardStatusSlots(count: String, max: String) = "$count/$max SLOTS"
  override fun matchCardSeatJoined(count: String, max: String) = "$count/$max JOINED"
  override fun matchCardWinnerBanner(prize: String) = "Match Completed • Winner ($prize)"
  override val matchCardDefeatBanner = "Match Completed • Defeat"
  override val matchCardCancelledBanner = "Match Cancelled • Entry fees refunded"
  override val matchCardPausedBanner = "Match Paused by Super Admin"
  override val matchCardDisabledBanner = "Match Disabled"
  override val matchCardButtonInProgress = "IN PROGRESS"
  override val matchCardButtonCodeReady = "CODE READY"
  override val matchCardButtonUpcoming = "UPCOMING"
  override val matchCardButtonSeatFull = "SEAT FULL"
  override val matchCardTimeLeft = "Left"
  override val matchCardUpcoming = "Upcoming"

  // Match Detail Screen
  override val matchDetailLudoHeader = "Ludo 1v1 Battle"
  override val matchDetailCarromHeader = "Carrom 1v1 Battle"
  override val matchDetailWinnerPrizeLabel = "WINNER PRIZE"
  override fun matchDetailYouWonBanner(prize: String) = "You won! Prize: $prize"
  override val matchDetailProofRejectedLoser = "Proof rejected (Defeat)"
  override val matchDetailMatchFinishedLoser = "Match finished / You lost"
  override fun matchDetailRegisteredSlot(slot: String) = "You are registered ($slot)"
  override fun matchDetailSlot(number: String) = "Slot $number"
  override val matchDetailUpcomingNotice = "This match is upcoming and will open for joining once released by admin."
  override val matchDetailRoomCodeTitle = "Game Room Code"
  override val matchDetailUnlockedBadge = "UNLOCKED"
  override val matchDetailRoomCodeInstructions = "Open Ludo King / Carrom Pool and enter this code to join match room"
  override val matchDetailWaitingRoomCode = "Super Admin will generate the room code 5-10 minutes before the scheduled match time. Please keep this screen open."
  override val matchDetailConfidentialNotice = "Game room code is confidential and accessible only to registered match participants."
  override val matchDetailApprovedTitle = "✓ Approved"
  override val matchDetailRejectedTitle = "✕ Rejected"
  override val matchDetailScreenshotSubmittedTitle = "Screenshot Submitted ✓"
  override val matchDetailApprovedBadge = "✓ Approved"
  override val matchDetailRejectedBadge = "✕ Rejected"
  override val matchDetailUnderReviewBadge = "Under Review"
  override val matchDetailReviewWaitingText = "Please wait while your result is being verified."
  override val matchDetailNotesLabel = "Notes:"
  override val matchDetailSubmittingScreenshotTitle = "Processing and submitting screenshot..."
  override val matchDetailSubmittingScreenshotSubtitle = "Please wait while your result is being submitted."
  override val matchDetailProofRejectedTitle = "Proof has been rejected"
  override val matchDetailProofRejectedDesc = "Result was rejected; proof cannot be resubmitted."
  override val matchDetailMatchFinishedTitle = "Match completed / Result finalized"
  override val matchDetailMatchFinishedWinnerDesc = "You are the winner! Prize has been credited to your wallet."
  override val matchDetailMatchFinishedLoserDesc = "Match is completed. No further proofs will be accepted."
  override val matchDetailResultApprovedTitle = "Result confirmed and approved"
  override val matchDetailResultApprovedDesc = "Your victory proof has been successfully approved."
  override val matchDetailDefeatedTitle = "Match completed / Result finalized"
  override val matchDetailDefeatedDesc = "Proof submission is not applicable for defeated players."
  override val matchDetailSubmitProofCta = "SUBMIT RESULT SCREENSHOT"
  override val matchDetailWaitingRoomCodeForProof = "Screenshot submission will be available once Admin provides the Room Code."
  override val matchDetailTwoMinutesWaitingText = "Screenshot can be submitted 2 minutes after room code is provided"
  override fun matchDetailRegisteredPlayersTitle(count: String, max: String) = "Registered Players ($count/$max)"
  override val matchDetailNoPlayersJoined = "No players have joined yet. Be the first to join!"
  override val matchDetailYouBadge = "YOU"
  override val matchDetailUpcomingButton = "UPCOMING (NOT RELEASED)"
  override fun matchDetailJoinMatchButton(fee: String) = "JOIN MATCH ($fee)"
  override val matchDetailJoinDialogTitle = "⚠️ Important Notice & Rules"
  override val matchDetailLudoNameNotice = "Dear user, your AD Tournament name and Ludo King name must be the same. Mismatch may lead to result cancellation."
  override val matchDetailCarromNameNotice = "Dear user, your AD Tournament name and Carrom Pool name must be the same. Mismatch may lead to result cancellation."
  override fun matchDetailJoinFeeNotice(fee: String) = "Match entry fee $fee will be deducted from your wallet."
  override val matchDetailAgreeAndJoin = "Agree & Join"
  override val matchDetailSubmitProofTitle = "Submit Result Screenshot"
  override val matchDetailScreenshotRequired = "Screenshot is required"
  override val matchDetailSelectScreenshot = "Select Screenshot from Gallery"
  override val matchDetailChangeScreenshot = "Change Screenshot"
  override val matchDetailRoomCodeRequired = "Room code is required"
  override val matchDetailRoomCodePlaceholder = "Enter room code"
  override val matchDetailMatchNumberRequired = "Enter the match number you played"
  override val matchDetailMatchNumberLabel = "Match Number"
  override val matchDetailMatchNumberPlaceholder = "e.g., Match #101"
  override val matchDetailRemarksLabel = "Notes / Winner Remarks (Optional)"
  override val matchDetailRemarksPlaceholder = "e.g., Ludo King 1st place"
  override val matchDetailSubmitProofButton = "SUBMIT PROOF"

  // Profile
  override val profileStatusActive = "Active"
  override val profileStatusBanned = "Banned"
  override val profileStatusSuspended = "Suspended"
  override val profileStatusPending = "Pending"
  override val profileSupportOffline = "OFFLINE"
  override val profileLogoutDialogTitle = "Confirm Logout"
  override val profileLogoutDialogMessage = "Are you sure you want to log out?"
  override val profileLogoutConfirmButton = "LOG OUT"
  override val profilePhotoUpdateSuccess = "Profile picture updated successfully"
  override fun profileAvatarPreset(id: String) = when {
    id.contains("dragon", ignoreCase = true) -> "Dragon Avatar"
    id.contains("cyber", ignoreCase = true) -> "Cyber Avatar"
    id.contains("ninja", ignoreCase = true) -> "Ninja Avatar"
    id.contains("crown", ignoreCase = true) -> "Crown Avatar"
    id.contains("sniper", ignoreCase = true) -> "Sniper Avatar"
    id.contains("shield", ignoreCase = true) -> "Knight Avatar"
    else -> "Esports Avatar"
  }

  // Deposit
  override val depositMethodLabel = "Method"
  override val depositAmountSummaryLabel = "Amount"
  override val depositStatusLabel = "Status"
  override val depositStatusPending = "PENDING"
  override val depositOfficialBadge = "OFFICIAL"
  override fun depositNumberCopiedToast(method: String, number: String) = "$method number copied: $number"
  override val depositAmountPlaceholder = "e.g. 100"
  override val depositTrxIdPlaceholder = "e.g. 9J8K7L6M"
  override val depositScreenshotPreview = "Payment Screenshot Preview"
  override fun depositMinAmountError(amount: String) = "Minimum deposit amount is $amount"

  // Notifications
  override val notifSubtitle = "Recent updates on matches and wallet"
  override val notifMarkAllRead = "Mark all as read"
  override val notifFilterAll = "All"
  override val notifFilterUnread = "Unread"
  override val notifBadgeNew = "NEW"
  override val notifCategoryWallet = "Wallet"
  override val notifCategoryMatch = "Match"
  override val notifCategoryRoomCode = "Room Code"
  override val notifCategorySystem = "System"
  override val notifTimeJustNow = "Just now"
  override fun notifTimeMinutesAgo(mins: String) = "$mins min ago"
  override fun notifTimeHoursAgo(hours: String) = "$hours hr ago"
  override val notifTimeYesterday = "Yesterday"
  override val notifTimeRecent = "Recent"
  override val notifDialogClose = "Close"
  override val notifAllReadSuccess = "All notifications marked as read"
  override val notifEmptyFilterTitle = "No unread notifications"
  override val notifEmptyFilterSubtext = "All your notifications have been read"

  // Mini Games Shared
  override val mgWalletTitle = "Mini Game Wallet"
  override val mgAddMoneyTitle = "Add Money"
  override val mgAddMoneySubtitle = "Transfer from main balance to game wallet"
  override val mgWithdrawMoneyTitle = "Withdraw to Main"
  override val mgWithdrawMoneySubtitle = "Transfer winnings to your main wallet"
  override val mgCloseButton = "Close"
  override val mgTransferAddTitle = "Add to Game Wallet"
  override val mgTransferWithdrawTitle = "Withdraw to Main Wallet"
  override val mgLabelUid = "User ID (UID)"
  override val mgLabelUserName = "User Name"
  override val mgLabelMobile = "Mobile Number"
  override fun mgMainBalance(amt: String) = "Your Main Balance: $amt"
  override fun mgGameBalance(amt: String) = "Your Game Balance: $amt"
  override val mgAmountPlaceholder = "Amount (৳)"
  override val mgErrorInsufficient = "Insufficient Balance!"
  override val mgErrorInvalidAmount = "Enter a valid amount"
  override val mgConfirmRequest = "Confirm Request"
  override val mgWinTitle = "Congratulations!"
  override val mgLossTitle = "Sorry!"
  override fun mgWinMessage(amt: String) = "You won $amt!"
  override val mgLossMessage = "Better luck next time. Try again!"

  // Dice Game
  override val diceTitle = "EVEN & ODD"
  override val diceSubtitle = "Test your luck with dice"
  override val diceMainBalanceLabel = "Main Balance"
  override val diceGameWalletLabel = "Game Wallet"
  override val diceAddFunds = "Add Funds"
  override val diceInstruction = "Roll to test your luck"
  override val diceRolling = "Rolling..."
  override val diceRollButton = "Roll Dice"
  override val diceNoBalance = "Insufficient Balance"
  override val diceChoiceEven = "Even"
  override val diceChoiceOdd = "Odd"
  override val dicePayout = "Payout: 1.9x"
  override val diceClosedBanner = "Game Temporarily Closed"
  override val diceToastNoBalance = "Insufficient game balance"
  override val diceToastSuccess = "Roll successful!"
  override val diceToastFailed = "Transaction failed"
  override fun diceRollResult(sum: String, type: String) = "Roll Result: $sum ($type)"

  // Head & Tail Game
  override val headTailTitle = "HEAD & TAIL"
  override val headTailSubtitle = "Instant Toss"
  override val headTailInstruction = "Toss to test your luck"
  override val headTailTossing = "Tossing..."
  override val headTailTossButton = "Toss Coin"
  override val headTailChoiceHead = "Head"
  override val headTailChoiceTail = "Tail"
  override val headTailPayout = "Payout: 1.9x"
  override val headTailClosedBanner = "Game Temporarily Closed"
  override val headTailToastNoBalance = "Insufficient game balance"
  override val headTailToastSuccess = "Toss successful!"
  override val headTailToastFailed = "Transaction failed"
  override fun headTailResult(side: String) = "Result: $side"

  // Spin Wheel Game
  override val spinTitle = "MONEY WHEEL"
  override val spinSubtitle = "★ ROYAL WHEEL ★"
  override val spinInstruction = "Spin to test your luck"
  override val spinSpinning = "Spinning..."
  override val spinButton = "SPIN"
  override val spinNoBalance = "Insufficient Balance"
  override val spinClosedBanner = "Game Temporarily Closed"
  override val spinToastNoBalance = "Insufficient game balance"
  override val spinToastFailed = "Transaction failed"
  override fun spinResultWin(multiplier: String) = "You won ${multiplier}x!"
  override fun spinResultAdded(amount: String) = "$amount has been added to your wallet."
  override val spinChipMain = "Main"
  override val spinChipGame = "Game"

  // Scratch Card Game
  override val scratchTitle = "SCRATCH CARD"
  override val scratchSubtitle = "Scratch & Win"
  override val scratchInstructionNew = "Tap button below to get a new card"
  override val scratchInstructionRevealed = "Card Revealed"
  override val scratchInstructionScratching = "Scratch with your finger"
  override val scratchRevealAll = "Reveal All"
  override val scratchNewTicketButton = "New Ticket"
  override val scratchCardScratching = "Scratching card..."
  override val scratchNoBalance = "Insufficient Balance"
  override val scratchClosedBanner = "Game Temporarily Closed"
  override val scratchToastFailed = "Transaction failed"
  override val scratchTransferToast = "Request successfully sent to admin"
  override val scratchTryAgain = "Try Again"
  override val scratchLossMessage = "No match found. Try again!"
  override fun scratchWinX(multiplier: String) = "${multiplier}X WIN"
  override fun scratchWinMessage(amount: String) = "You won $amount!\nAdded to your game wallet."

  // Auto Ludo Lobby
  override val autoLudoHeaderTitle = "Match List"
  override val autoLudoCreateBannerText = "If you want to create a match of your choice, click the button below."
  override val autoLudoCreateButton = "Create Match"
  override val autoLudoFilterAll = "All Matches"
  override val autoLudoFilterRecent = "Recent Matches"
  override val autoLudoFilterMy = "My Matches"
  override val autoLudoFilterFree = "Free Matches"
  override val autoLudoNoMatches = "No matches found"
  override val autoLudoAutoMatchBadge = "🤖 Fully Automated Match"
  override val autoLudoMyMatchBadge = "My Match"
  override val autoLudoPrizeLabel = "Prize"
  override val autoLudoEntryLabel = "Entry"
  override val autoLudoStatusOpponentMissing = "❌ Opponent not arrived"
  override val autoLudoStatusPlayerAbsent = "❌ Player absent"
  override val autoLudoStatusStarting = "Match starting..."
  override val autoLudoStatusPlaying = "Match running"
  override val autoLudoStatusFull = "Match full"
  override val autoLudoStatusWaitingOpponent = "Waiting for opponent"
  override val autoLudoStatusWaitingTwo = "Waiting for 2 players"
  override fun autoLudoPlayersCount(current: Int, max: Int) = "Players: $current/$max"
  override fun autoLudoCardTitle(number: String) = "Auto Ludo · Match No. #$number"
  override val autoLudoTutorialButton = "Tutorial"
  override val autoLudoRefundCancelled = "Cancelled (Refunded)"
  override val autoLudoRefundAbsent = "Absent (Refunded)"
  override val autoLudoMatchWonFinished = "🏆 You Won · Match Finished"
  override val autoLudoMatchLostFinished = "You Lost · Match Finished"
  override val autoLudoMatchFinished = "Match Finished"
  override val autoLudoWaitingStatus = "Waiting"
  override fun autoLudoStartingIn(time: String) = "Starting in: $time"
  override val autoLudoEnterGameButton = "Enter Game"
  override val autoLudoMatchFullButton = "Match Full"
  override val autoLudoTimeExpired = "Time Expired"
  override val autoLudoJoinButton = "Join Match"
  override val autoLudoCreateDialogTitle = "Create New Match"
  override val autoLudoGameWalletBalance = "Game Wallet Balance"
  override val autoLudoEntryFeeHeader = "Entry Fee (10 - 500)"
  override val autoLudoEntryFeePlaceholder = "Enter entry fee"
  override val autoLudoStartTimeLabel = "Match Start Time (Today)"
  override val autoLudoFutureTimeError = "Please select a time after current time"
  override val autoLudoWinningPrizeLabel = "Winning Prize:"
  override val autoLudoWinningPrizeSub = "Will be added to your wallet after match"
  override fun autoLudoInsufficientBalanceCreate(required: String, current: String) = "Insufficient game wallet balance (Required: $required, Current: $current)"
  override val autoLudoConfirm = "Confirm"
  override val autoLudoJoinDialogTitle = "Join Match"
  override fun autoLudoJoinFeeNotice(amount: String) = "Entry fee: $amount will be deducted"
  override fun autoLudoBalanceLabel(amount: String) = "Balance: $amount"
  override val autoLudoMatchTimeExpiredError = "Match start time has expired!"
  override fun autoLudoInsufficientBalanceJoin(amount: String) = "Insufficient game wallet balance\n(Required: $amount)"
  override val autoLudoTimeExpiredToast = "Match start time has expired!"
  override val autoLudoTutorialSoonToast = "Tutorial coming soon"
  override val autoLudoMatchCreatedSuccess = "Match created successfully"
  override val autoLudoMatchCreatedFail = "Failed to create match"
  override val autoLudoJoinSuccessToast = "Successfully joined match! Enter at scheduled time."
  override val autoLudoJoinFailToast = "Failed to join match"
  override val autoLudoTransferReqSuccess = "Request sent successfully to admin"
  override fun autoLudoTransferReqFail(err: String) = "Failed to send request: $err"
  override val autoLudoMatchEndedToast = "Match ended! Result has been decided."
  override val autoLudoCancelOpponentMissing = "Match cancelled and refunded as no opponent found."
  override val autoLudoCancelBothNotReady = "Match cancelled as both players were not ready."
  override val autoLudoHourLabel = "Hour"
  override val autoLudoMinLabel = "Min"
  override val autoLudoAmPmLabel = "AM/PM"

  // Ready Room Dialog
  override val readyRoomTitle = "Ludo Battle Room"
  override fun readyRoomEntryPrize(entry: String, prize: String) = "Entry $entry · Prize $prize"
  override val readyRoomVs = "VS"
  override fun readyRoomPlayer(num: Int) = "Player $num"
  override val readyRoomNotReady = "Not ready"
  override val readyRoomReady = "Ready ✓"
  override val readyRoomStartsIn = "Match starts in"
  override val readyRoomPlayButton = "Start Match"
  override val readyRoomGoBack = "Go back"
  override val readyRoomPlayerYou = "You"
  override val readyRoomWaitingOpponent = "Waiting for opponent..."
  override val readyRoomTimeUpDeciding = "Time is up · Deciding result..."
  override val readyRoomStartGame = "Start Game"
  override val readyRoomBack = "Go back"
  override val readyRoomStarting = "Starting..."

  // AutoLudo Logs
  override fun autoLudoLog(raw: String) = raw
  override fun displayActionLog(raw: String) = raw
  
  // AutoLudo Result Dialog
  override val autoLudoResultWinTitle = "Congratulations! You won! 🏆"
  override val autoLudoResultWinReason = "Congratulations! You won!"
  override fun autoLudoResultWinPrize(prize: String) = "+$prize added to your game wallet"
  override val autoLudoResultLoseTitle = "Not this time"
  override fun autoLudoResultLoseReason(winnerName: String) = "$winnerName won the game. Thanks for playing!"
  override val autoLudoResultOk = "OK"
  
  // AutoLudo Exit Dialog
  override val autoLudoExitTitle = "Are you sure you want to exit?"
  override fun autoLudoExitMessage(opponent: String) = "If you exit, $opponent will be declared the winner."
  override val autoLudoExitConfirm = "Yes, exit"
  override val autoLudoExitCancel = "No"
  override val autoLudoExitYes = "Yes"
  override val autoLudoExitNo = "No"
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> {
  BengaliStrings // Default is strictly Bengali
}

fun appStringsFor(language: AppLanguage): AppStrings = when (language) {
  AppLanguage.BN -> BengaliStrings
  AppLanguage.EN -> EnglishStrings
}
