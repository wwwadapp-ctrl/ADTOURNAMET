package com.example.ui.minigames

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.core.i18n.*

data class MiniGame(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector,
    val color: Color,
    val type: MiniGameType
)

enum class MiniGameType {
    SPIN, DICE, EVEN_ODD, HEAD_TAIL, SCRATCH
}

@Composable
fun MiniGamesSection(
    onGameClick: (MiniGameType) -> Unit
) {
    val strings = LocalAppStrings.current
    val games = remember(strings) {
        listOf(
            MiniGame(
                "1", strings.miniGamesRoyalWheelTitle, strings.miniGamesRoyalWheelSub, strings.miniGamesRoyalWheelBadge, 
                Icons.Default.Refresh, Color(0xFFFFD700), MiniGameType.SPIN
            ),
            MiniGame(
                "2", strings.miniGamesDiceTitle, strings.miniGamesDiceSub, strings.miniGamesDiceBadge, 
                Icons.Default.Build, Emerald400, MiniGameType.DICE
            ),
            MiniGame(
                "3", strings.miniGamesHeadTailTitle, strings.miniGamesHeadTailSub, strings.miniGamesHeadTailBadge, 
                Icons.Default.Face, Cyan400, MiniGameType.HEAD_TAIL
            ),
            MiniGame(
                "4", strings.miniGamesScratchTitle, strings.miniGamesScratchSub, strings.miniGamesScratchBadge, 
                Icons.Default.Star, Amber500, MiniGameType.SCRATCH
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        AdMiniGamesHeader(
            title = strings.miniGamesTitle,
            viewAllText = strings.miniGamesViewAll,
            onViewAllClick = { }
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.height(474.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            userScrollEnabled = false
        ) {
            items(games) { game ->
                // Localized game data
                val localizedGame = remember(game, strings) {
                    when(game.type) {
                        MiniGameType.SPIN -> game.copy(title = strings.miniGamesRoyalWheelTitle, subtitle = strings.miniGamesRoyalWheelSub, badge = strings.miniGamesRoyalWheelBadge)
                        MiniGameType.DICE -> game.copy(title = strings.miniGamesDiceTitle, subtitle = strings.miniGamesDiceSub, badge = strings.miniGamesDiceBadge)
                        MiniGameType.HEAD_TAIL -> game.copy(title = strings.miniGamesHeadTailTitle, subtitle = strings.miniGamesHeadTailSub, badge = strings.miniGamesHeadTailBadge)
                        MiniGameType.SCRATCH -> game.copy(title = strings.miniGamesScratchTitle, subtitle = strings.miniGamesScratchSub, badge = strings.miniGamesScratchBadge)
                        MiniGameType.EVEN_ODD -> game // Keep or define key if needed
                    }
                }
                MiniGameCard(game = localizedGame, onClick = { onGameClick(game.type) })
            }
        }
    }
}

@Composable
fun MiniGameCard(
    game: MiniGame,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val buttonText = when (game.type) {
        MiniGameType.SPIN -> strings.miniGamesRoyalWheelPayout
        MiniGameType.SCRATCH -> strings.miniGamesScratchAction
        else -> strings.miniGamesDicePayout
    }
    val multiplierText = when (game.type) {
        MiniGameType.SPIN -> strings.digits("50") + "x"
        MiniGameType.SCRATCH -> ""
        else -> strings.digits("1.9") + "x"
    }
    AdMiniGameCard(
        type = game.type,
        title = game.title,
        subtitle = game.subtitle,
        ribbonText = game.badge,
        multiplierText = multiplierText,
        buttonText = buttonText,
        onClick = onClick
    )
}
