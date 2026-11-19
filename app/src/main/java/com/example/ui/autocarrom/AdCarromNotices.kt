package com.example.ui.autocarrom

import com.google.firebase.database.ServerValue

/**
 * Auto Carrom notification texts (Bengali + English) written to the player's OWN userNotifications.
 * Same shape as AdLudoNotices; ids start with "notif_ac_" so they never clash with Auto Ludo.
 */
object AdCarromNotices {
    private fun bn(s: String) = CarromRules.bn(s)
    private fun tkBn(t: Double) = "৳" + bn(CarromRules.amt(t))
    private fun tkEn(t: Double) = "৳" + CarromRules.amt(t)

    fun refundId(matchId: String, uid: String) = "notif_ac_refund_${matchId}_$uid"
    fun prizeId(matchId: String, uid: String) = "notif_ac_prize_${matchId}_$uid"
    fun joinedId(matchId: String, uid: String) = "notif_ac_joined_${matchId}_$uid"

    private fun record(
        id: String, uid: String, type: String,
        titleBn: String, messageBn: String, titleEn: String, messageEn: String
    ): HashMap<String, Any?> = hashMapOf(
        "id" to id,
        "userId" to uid,
        "title" to titleBn,
        "titleEn" to titleEn,
        "message" to messageBn,
        "messageEn" to messageEn,
        "type" to type,
        "timestamp" to ServerValue.TIMESTAMP,
        "read" to false
    )

    fun refundRecord(matchId: String, matchNumber: String, uid: String, amount: Double) = record(
        refundId(matchId, uid), uid, "AUTO_CARROM_REFUND",
        "টাকা ফেরত এসেছে 💛",
        "অটো ক্যারম ম্যাচ #${bn(matchNumber)} বাতিল হওয়ায় আপনার ${tkBn(amount)} গেম ওয়ালেটে ফেরত দেওয়া হয়েছে। আবার খেলুন, শুভকামনা! 🎯",
        "Money Refunded 💛",
        "Auto Carrom match #$matchNumber was cancelled, so your ${tkEn(amount)} has been returned to your game wallet. Play again, good luck! 🎯"
    )

    fun prizeRecord(matchId: String, matchNumber: String, uid: String, amount: Double) = record(
        prizeId(matchId, uid), uid, "AUTO_CARROM_PRIZE",
        "পুরস্কার যোগ হয়েছে 🏆",
        "অটো ক্যারম ম্যাচ #${bn(matchNumber)}-এ জয়ের ${tkBn(amount)} আপনার গেম ওয়ালেটে যোগ হয়েছে। অভিনন্দন! 🎉",
        "Prize Added 🏆",
        "Your ${tkEn(amount)} prize for winning Auto Carrom match #$matchNumber has been added to your game wallet. Congratulations! 🎉"
    )

    /** Written once inside a transaction, so it carries a plain time instead of ServerValue.TIMESTAMP. */
    fun joinedRecord(
        matchId: String, matchNumber: String, uid: String,
        opponentName: String, startText: String, now: Long
    ): HashMap<String, Any?> = hashMapOf(
        "id" to joinedId(matchId, uid),
        "userId" to uid,
        "title" to "প্রতিপক্ষ চলে এসেছে! 🎉",
        "titleEn" to "Your opponent is here! 🎉",
        "message" to "$opponentName আপনার অটো ক্যারম ম্যাচ #${bn(matchNumber)}-এ যোগ দিয়েছেন। খেলা শুরু ${bn(startText)}-এ। প্রস্তুত থাকুন, জয় আপনার অপেক্ষায়! 🎯",
        "messageEn" to "$opponentName has joined your Auto Carrom match #$matchNumber. The game starts at $startText. Get ready, victory is waiting for you! 🎯",
        "type" to "AUTO_CARROM_OPPONENT_JOINED",
        "timestamp" to now,
        "read" to false
    )

    fun reminderTitleBn() = "খেলার সময় হয়ে গেছে! ⏰"
    fun reminderTitleEn() = "It's game time! ⏰"
    fun reminderMessageBn(matchNumber: String, opponent: String) =
        "অটো ক্যারম ম্যাচ #${bn(matchNumber)}-এ $opponent আপনার অপেক্ষায়। এখনই খেলায় প্রবেশ করুন, হাতে আছে ১০ মিনিট। সময়মতো না এলে, আর $opponent প্রস্তুত থাকলে, তাঁকেই জয়ী ঘোষণা করা হবে।"
    fun reminderMessageEn(matchNumber: String, opponent: String) =
        "$opponent is waiting for you in Auto Carrom match #$matchNumber. Enter the game now, you have 10 minutes. If you don't come in time and $opponent is ready, they will be declared the winner."
}
