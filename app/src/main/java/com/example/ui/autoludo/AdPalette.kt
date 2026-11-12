package com.example.ui.autoludo

import androidx.compose.ui.graphics.Color

// VISUAL ONLY: which color sits in which corner of the board for a match.
// The game engine is unchanged: player 1 always uses the bottom-left home and path,
// player 2 always uses the top-right home and path. Only the colors rotate per match.
data class AdPalette(
    val bl: Color,   // bottom-left home  = player 1
    val tl: Color,   // top-left home
    val tr: Color,   // top-right home    = player 2
    val br: Color,   // bottom-right home
    val p1Pawn: Color,
    val p2Pawn: Color,
    val p1IsYellow: Boolean,
    val p2IsYellow: Boolean
)

private enum class AdHue(val board: Color, val pawn: Color) {
    RED(Color(0xFFE0201C), Color(0xFFE53935)),
    GREEN(Color(0xFF22A84A), Color(0xFF2EAD4B)),
    YELLOW(Color(0xFFFFD91A), Color(0xFFFBC02D)),
    BLUE(Color(0xFF29A9F0), Color(0xFF1E88E5))
}

// Clockwise order from the bottom-left corner: red, green, yellow, blue.
private val AdOrder = listOf(AdHue.RED, AdHue.GREEN, AdHue.YELLOW, AdHue.BLUE)

// The same match always gives the same colors on both phones.
fun adMatchPalette(matchNumber: String, matchId: String): AdPalette {
    val n = matchNumber.trim().toIntOrNull() ?: matchId.hashCode()
    val shift = ((n % 4) + 4) % 4
    fun slot(i: Int) = AdOrder[(i + shift) % 4]
    val bl = slot(0); val tl = slot(1); val tr = slot(2); val br = slot(3)
    return AdPalette(
        bl = bl.board, tl = tl.board, tr = tr.board, br = br.board,
        p1Pawn = bl.pawn, p2Pawn = tr.pawn,
        p1IsYellow = bl == AdHue.YELLOW, p2IsYellow = tr == AdHue.YELLOW
    )
}

// The classic layout (red bottom-left, green top-left, yellow top-right, blue bottom-right).
val AdDefaultPalette = AdPalette(
    bl = AdHue.RED.board, tl = AdHue.GREEN.board, tr = AdHue.YELLOW.board, br = AdHue.BLUE.board,
    p1Pawn = AdHue.RED.pawn, p2Pawn = AdHue.YELLOW.pawn,
    p1IsYellow = false, p2IsYellow = true
)
