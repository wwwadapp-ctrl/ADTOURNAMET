package com.example.ui.wallet

import com.example.core.i18n.*
import com.example.domain.model.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TxDisplay(
    val title: String,
    val description: String,
    val isCredit: Boolean,
    val isVoid: Boolean,
    val statusText: String,
    val dateText: String,
    val iconKind: String
)

fun buildTxDisplay(txn: TransactionEntity, strings: AppStrings): TxDisplay {
    val type = txn.type.uppercase()
    
    val isWithdraw = type.contains("WITHDRAW")
    val isDeposit = type.contains("DEPOSIT") && !isWithdraw
    val isMatchJoin = type == "MATCH_JOIN" || type.contains("ENTRY")
    val isRefund = type.contains("REFUND")
    val isPrize = type.contains("PRIZE") || type.contains("WIN")
    val isMiniGameOut = type == "MINI_GAME_TRANSFER_OUT" || type == "MINIGAME_TRANSFER_OUT" || type.contains("TRANSFER_TO_GAME")
    val isMiniGameIn = type == "MINI_GAME_TRANSFER_IN" || type == "MINIGAME_TRANSFER_IN" || type.contains("TRANSFER_FROM_GAME")
    val isAdmin = type.contains("ADMIN") || type.contains("ADJUSTMENT")
    val isBonus = type.contains("BONUS")

    // Credit/Debit Rule
    val isCredit = when {
        isWithdraw || isMatchJoin || isMiniGameOut -> false
        isAdmin -> txn.direction.uppercase() != "DEBIT" // treat empty or "CREDIT" as credit
        isDeposit || isPrize || isRefund || isMiniGameIn || isBonus || type.contains("CREDIT") -> true
        else -> false
    }

    // Title
    val title = when {
        isWithdraw -> strings.walletTxWithdraw
        isDeposit -> strings.walletTxDeposit
        isPrize -> strings.walletTxPrize
        isRefund -> strings.walletTxMatchRefund
        isMatchJoin -> strings.walletTxMatchJoin
        isMiniGameIn -> strings.walletTxMiniGameTransferIn
        isMiniGameOut -> strings.walletTxMiniGameTransferOut
        isAdmin -> strings.walletTxAdminAdjustment
        isBonus -> strings.walletTxBonus
        else -> txn.title.ifBlank { strings.walletTxOther }
    }

    // Description
    val description = if (strings === BengaliStrings) {
        when {
            isMatchJoin && txn.referenceId.isNotBlank() && txn.referenceId.length < 15 -> strings.walletTxDescMatch(txn.referenceId)
            isMatchJoin -> strings.walletTxDescMatchJoin
            isDeposit -> strings.walletTxDescDeposit
            isWithdraw -> strings.walletTxDescWithdraw
            isRefund -> strings.walletTxDescMatchRefund
            isPrize -> strings.walletTxDescPrize
            isMiniGameIn -> strings.walletTxDescTransferIn
            isMiniGameOut -> strings.walletTxDescTransferOut
            isBonus -> strings.walletTxDescBonus
            else -> strings.walletTxDescDefault
        }
    } else {
        txn.description.ifBlank { txn.referenceId.ifBlank { txn.source } }
    }

    // Status
    val statusUpper = txn.status.uppercase()
    val isVoid = statusUpper == "REJECTED" || statusUpper == "FAILED" || statusUpper == "CANCELLED"

    val statusText = when (statusUpper) {
        "COMPLETED", "SUCCESS", "APPROVED" -> strings.walletTxStatusCompleted
        "PENDING", "PENDING_REVIEW", "SUBMITTED" -> strings.walletTxStatusPending
        "REJECTED", "FAILED" -> strings.walletTxStatusRejected
        "CANCELLED" -> strings.walletTxStatusCancelled
        else -> txn.status
    }

    // Date
    val dateText = formatLocalizedDate(if (txn.timestamp > 0L) txn.timestamp else txn.createdAt, strings)

    // Icon
    val iconKind = when {
        isWithdraw -> "withdraw"
        isDeposit -> "deposit"
        isPrize -> "prize"
        isRefund -> "refund"
        isMiniGameIn || isMiniGameOut -> "game"
        isAdmin -> "admin"
        else -> "transfer"
    }

    return TxDisplay(
        title = title,
        description = description,
        isCredit = isCredit,
        isVoid = isVoid,
        statusText = statusText,
        dateText = dateText,
        iconKind = iconKind
    )
}

private fun formatLocalizedDate(timestamp: Long, strings: AppStrings): String {
    if (timestamp <= 0L) return ""
    val date = Date(timestamp)
    return if (strings === BengaliStrings) {
        val day = SimpleDateFormat("dd", Locale.US).format(date)
        val month = when (SimpleDateFormat("MM", Locale.US).format(date)) {
            "01" -> "জানুয়ারি"
            "02" -> "ফেব্রুয়ারি"
            "03" -> "মার্চ"
            "04" -> "এপ্রিল"
            "05" -> "মে"
            "06" -> "জুন"
            "07" -> "জুলাই"
            "08" -> "আগস্ট"
            "09" -> "সেপ্টেম্বর"
            "10" -> "অক্টোবর"
            "11" -> "নভেম্বর"
            "12" -> "ডিসেম্বর"
            else -> ""
        }
        val time = SimpleDateFormat("hh:mm a", Locale.US).format(date)
        "${strings.digits(day)} $month, ${strings.digits(time)}"
    } else {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).format(date)
    }
}
