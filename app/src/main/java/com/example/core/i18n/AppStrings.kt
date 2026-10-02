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
  override fun winRateSubtitle(rate: Int) = "$rate% জয়ের হার"
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
  override val languageSettingTitle = "App Language / ভাষা"
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
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> {
  BengaliStrings // Default is strictly Bengali
}

fun appStringsFor(language: AppLanguage): AppStrings = when (language) {
  AppLanguage.BN -> BengaliStrings
  AppLanguage.EN -> EnglishStrings
}
