package com.example.core.config

object FirebaseConfig {
  const val DATABASE_URL = "https://adturnamet-default-rtdb.asia-southeast1.firebasedatabase.app"

  const val NODE_USERS = "users"
  const val NODE_WALLETS = "wallets"
  const val NODE_TRANSACTIONS = "transactions"
  const val NODE_MATCHES = "matches"
  const val NODE_MATCH_PLAYERS = "matchPlayers"
  const val NODE_RESULTS = "results"
  const val NODE_DEPOSITS = "deposits"
  const val NODE_WITHDRAWALS = "withdrawals"
  const val NODE_NOTIFICATIONS = "notifications"
  const val NODE_ADMIN_USERS = "adminUsers"
  const val NODE_AUDIT_LOGS = "auditLogs"
  const val NODE_APP_SETTINGS = "appSettings"
  const val NODE_PHONE_INDEX = "phoneIndex"

  const val NODE_USER_TRANSACTIONS = "userTransactions"
  const val NODE_USER_DEPOSITS = "userDeposits"
  const val NODE_USER_WITHDRAWALS = "userWithdrawals"
  const val NODE_USER_NOTIFICATIONS = "userNotifications"
  const val NODE_USER_MATCHES = "userMatches"

  const val STORAGE_RESULTS_PROOF = "proofs/results"
  const val STORAGE_DEPOSIT_PROOF = "proofs/deposits"
  const val STORAGE_USER_AVATARS = "avatars/users"

  const val FCM_TOPIC_ANNOUNCEMENTS = "announcements"
  const val FCM_TOPIC_MATCH_ALERTS = "match_alerts"
  const val FCM_TOPIC_ADMIN_ALERTS = "admin_alerts"
}
