package com.example.ui.autocarrom

import java.util.Locale

// Auto Carrom data + game rules. Pure Kotlin (no Android), used by the engine and by the game screen.
// Board units are millimetres of a real 740 x 740 mm playing area. Player 1 = white (bottom), Player 2 = black.

data class CarromGameState(
    val currentTurnUid: String = "",
    val shotNo: Int = 0,
    val board: String = "",            // "c,x,y;c,x,y;..." c = w/k/q, x/y in 0.1 mm. Pieces not listed are pocketed.
    val player1Pocketed: Int = 0,      // white coins pocketed (player 1)
    val player2Pocketed: Int = 0,      // black coins pocketed (player 2)
    val queenOwnerUid: String = "",
    val queenPendingUid: String = "",
    val player1Strikes: Int = 0,
    val player2Strikes: Int = 0,
    val player1Powder: Int = 3,
    val player2Powder: Int = 3,
    val powder: String = "",           // "x,y,strength;..." x/y in 0.1 mm, strength 0..100
    val lastShot: String = "",         // "shotNo|uid|strikerX|strikerY|dirX|dirY|power" for the replay on the other phone
    val actionStartedAt: Long = 0L,
    val suddenDeath: Boolean = false,
    val winnerUid: String = "",
    val lastMoveAt: Long = 0L,
    val lastActionLog: String = ""
)

data class AutoCarromMatchEntity(
    val matchId: String = "",
    val matchNumber: String = "",
    val title: String = "",
    val entryFee: Double = 0.0,
    val prizePool: Double = 0.0,
    val creatorUid: String = "",
    val creatorName: String = "",
    val player1Uid: String = "",
    val player1Name: String = "",
    val player1Avatar: String = "",
    val player2Uid: String = "",
    val player2Name: String = "",
    val player2Avatar: String = "",
    val status: String = "WAITING", // WAITING, READY_COUNTDOWN, IN_GAME, COMPLETED, CANCELLED
    val joinedPlayersCount: Int = 1,
    val maxPlayers: Int = 2,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val scheduledTimeFormatted: String = "Today",
    val scheduledTimeMillis: Long = 0L,
    val readyTimerStartedAt: Long = 0L,
    val countdownStartedAt: Long = 0L,
    val player1Ready: Boolean = false,
    val player2Ready: Boolean = false,
    val gameStartedAt: Long = 0L,
    val prizeDistributed: Boolean = false,
    val gameState: CarromGameState = CarromGameState(),
    val forfeitedByUid: String = "",
    val winnerUid: String = "",
    val prizeClaimed: Boolean = false,
    val refundClaimed: Map<String, Boolean> = emptyMap()
)

data class CPiece(val c: Char, val x: Double, val y: Double)

object CarromRules {
    const val S = 740.0          // playing area (mm)
    const val RC = 15.9          // coin radius
    const val RS = 24.5          // striker radius
    const val RP = 22.5          // pocket radius
    const val BASE = 103.0       // base line distance from the shooter's edge
    const val XMIN = 150.0
    const val XMAX = 590.0
    const val COINS = 9
    const val TURN_MS = 20_000L
    const val GAME_MS = 600_000L
    const val MAX_STRIKES = 3
    const val POWDER_PER_PLAYER = 3

    fun initialBoard(): String {
        val c = S / 2
        val list = mutableListOf(CPiece('q', c, c))
        val d = RC * 2.02
        for (i in 0 until 6) {
            val a = i * Math.PI / 3
            list.add(CPiece(if (i % 2 == 1) 'k' else 'w', c + Math.cos(a) * d, c + Math.sin(a) * d))
        }
        for (i in 0 until 12) {
            val a = i * Math.PI / 6
            val r = d * (if (i % 2 == 1) Math.sqrt(3.0) else 2.0)
            list.add(CPiece(if (i % 2 == 1) 'w' else 'k', c + Math.cos(a) * r, c + Math.sin(a) * r))
        }
        return encode(list)
    }

    fun encode(list: List<CPiece>): String =
        list.joinToString(";") { "${it.c},${Math.round(it.x * 10)},${Math.round(it.y * 10)}" }

    fun decode(s: String): MutableList<CPiece> {
        val out = mutableListOf<CPiece>()
        if (s.isBlank()) return out
        for (p in s.split(";")) {
            val f = p.split(",")
            if (f.size != 3 || f[0].isEmpty()) continue
            val x = f[1].toDoubleOrNull() ?: continue
            val y = f[2].toDoubleOrNull() ?: continue
            out.add(CPiece(f[0][0], x / 10.0, y / 10.0))
        }
        return out
    }

    /** First free spot spiralling out from the centre (for returned coins / queen). */
    fun freeSpot(pieces: List<CPiece>): Pair<Double, Double> {
        var a = 0.0; var r = 0.0
        for (k in 0 until 400) {
            val x = S / 2 + Math.cos(a) * r
            val y = S / 2 + Math.sin(a) * r
            val free = pieces.none { val dx = it.x - x; val dy = it.y - y; Math.sqrt(dx * dx + dy * dy) < RC * 2 + 1 }
            if (free) return x to y
            a += 1.1; r += 3.0
        }
        return S / 2 to S / 2
    }

    fun decayPowder(s: String): String {
        if (s.isBlank()) return ""
        return s.split(";").mapNotNull { p ->
            val f = p.split(",")
            if (f.size != 3) return@mapNotNull null
            val st = (f[2].toIntOrNull() ?: 0) - 20
            if (st <= 5) null else "${f[0]},${f[1]},$st"
        }.joinToString(";")
    }

    data class Outcome(val state: CarromGameState, val status: String, val winnerUid: String)

    /**
     * Applies one finished shot. [finalBoard] = pieces left on the board after the physics (encoded),
     * [pocketed] = colours pocketed in this shot (e.g. "wwq"), [strikerIn] = striker fell in a pocket (foul).
     * [gameEndsAt] = gameStartedAt + GAME_MS.
     */
    fun resolve(
        st: CarromGameState, p1: String, p2: String, shooter: String,
        finalBoard: String, pocketed: String, strikerIn: Boolean, shotInput: String,
        gameEndsAt: Long, now: Long
    ): Outcome {
        val isP1 = shooter == p1
        val other = if (isP1) p2 else p1
        val my = if (isP1) 'w' else 'k'
        val own = pocketed.count { it == my }
        val queenIn = pocketed.contains('q')
        var p1Pot = st.player1Pocketed + pocketed.count { it == 'w' }
        var p2Pot = st.player2Pocketed + pocketed.count { it == 'k' }
        val pieces = decode(finalBoard)
        var queenOwner = st.queenOwnerUid
        var queenPending = st.queenPendingUid
        var again = false
        var log: String
        fun putBack(c: Char) { val (x, y) = freeSpot(pieces); pieces.add(CPiece(c, x, y)) }

        if (strikerIn) {
            log = "Foul! Striker pocketed."
            val mine = if (isP1) p1Pot else p2Pot
            if (mine > 0) { if (isP1) p1Pot-- else p2Pot--; putBack(my) }
            if (queenIn) putBack('q')
            if (queenPending == shooter) { if (!queenIn) putBack('q'); queenPending = "" }
        } else {
            log = if (own > 0) "Pocketed $own." else "No coin pocketed."
            if (queenIn) {
                if (own > 0) { queenOwner = shooter; queenPending = ""; log = "Queen covered!" }
                else { queenPending = shooter; log = "Queen pocketed! Cover it next shot." }
                again = true
            } else if (queenPending == shooter) {
                if (own > 0) { queenOwner = shooter; log = "Queen covered!" } else { putBack('q'); log = "Queen returned." }
                queenPending = ""
            }
            if (own > 0) again = true
        }

        var status = "IN_GAME"
        var winner = ""
        var sudden = st.suddenDeath
        if (p1Pot >= COINS || p2Pot >= COINS) {
            winner = if (p1Pot >= COINS) p1 else p2
        } else if (gameEndsAt in 1..now || sudden) {
            when {
                p1Pot != p2Pot -> winner = if (p1Pot > p2Pot) p1 else p2
                !sudden && queenOwner.isNotBlank() -> winner = queenOwner
                else -> { sudden = true; log = "Time over! Sudden death." }
            }
        }
        if (winner.isNotBlank()) { status = "COMPLETED"; log = "Game over." }

        val next = st.copy(
            currentTurnUid = if (again) shooter else other,
            shotNo = st.shotNo + 1,
            board = encode(pieces),
            player1Pocketed = p1Pot,
            player2Pocketed = p2Pot,
            queenOwnerUid = queenOwner,
            queenPendingUid = queenPending,
            powder = decayPowder(st.powder),
            lastShot = shotInput,
            actionStartedAt = now,
            suddenDeath = sudden,
            winnerUid = winner,
            lastMoveAt = now,
            lastActionLog = log
        )
        return Outcome(next, status, winner)
    }

    fun bn(s: String) = s.map { if (it in '0'..'9') '০' + (it - '0') else it }.joinToString("")
    fun amt(t: Double): String =
        if (t % 1.0 == 0.0) String.format(Locale.US, "%.0f", t) else String.format(Locale.US, "%.2f", t)
}
