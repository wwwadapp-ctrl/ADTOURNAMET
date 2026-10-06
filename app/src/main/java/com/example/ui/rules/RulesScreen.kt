package com.example.ui.rules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TournamentCard
import com.example.ui.theme.*

@Composable
fun RulesScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DeepNavyBg)
      .testTag("rules_screen"),
  ) {
    // Top Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("rules_back_button")) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
      }
      Icon(
        imageVector = Icons.Default.Gavel,
        contentDescription = null,
        tint = Gold400,
        modifier = Modifier.size(24.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = "TOURNAMENT RULES",
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = 1.sp,
        ),
        color = Color.White,
      )
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 1. Overview Rule Banner
      item {
        TournamentCard(
          modifier = Modifier.fillMaxWidth(),
          backgroundColor = NavyCard,
          borderColor = Gold500.copy(alpha = 0.5f),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Gold500.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Gold400, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Fair Play & Anti-Fraud Guarantee",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
              )
              Text(
                text = "Strictly 1v1 matchmaking. Admin verified results and escrowed payouts.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400,
              )
            }
          }
        }
      }

      // Rule Sections
      item {
        RuleCard(
          number = "1",
          title = "1v1 Match Structure (Max 2 Players)",
          content = "Every tournament match is strictly 1v1 between exactly 2 players (Slot 1 vs Slot 2). Once both slots are occupied, the match locks as FULL. No extra players can enter.",
          badgeText = "STRICT 1v1",
          badgeColor = Cyan400,
        )
      }

      item {
        RuleCard(
          number = "2",
          title = "Locked Entries (No Cancellations)",
          content = "Once you register and join a match, the entry fee is held in secure match escrow. You CANNOT leave or cancel a match after joining. Ensure your internet connection and game app are ready before entering.",
          badgeText = "NO REFUND ON LEAVE",
          badgeColor = Amber400,
        )
      }

      item {
        RuleCard(
          number = "3",
          title = "Game Room Code Confidentiality",
          content = "Room codes are created by the Super Admin 5-10 minutes prior to scheduled start time. The code is strictly confidential and visible ONLY to the 2 registered participants and the Super Admin. Non-joined users cannot view the code.",
          badgeText = "CONFIDENTIAL",
          badgeColor = Indigo400,
        )
      }

      item {
        RuleCard(
          number = "4",
          title = "100% Winner Takes All Prize System",
          content = "Total Prize Pool is calculated deterministically as:\n• Total Pool = Entry Fee × 2\n• Winner Prize = Total Pool - Platform Commission\nThe verified winner takes the entire prize pool into their Withdrawable Winning Balance.",
          badgeText = "WINNER TAKES ALL",
          badgeColor = Emerald400,
        )
      }

      item {
        RuleCard(
          number = "5",
          title = "Platform Commission Tiers",
          content = "Platform commissions are based on fixed integer tiers:\n• Entry Fee ≤ ৳100: ৳10 platform commission\n• Entry Fee ৳101 - ৳200: ৳20 platform commission\n• Entry Fee > ৳200: ৳40 platform commission\nNo hidden deduction or floating-point rounding.",
          badgeText = "TRANSPARENT",
          badgeColor = Gold400,
        )
      }

      item {
        RuleCard(
          number = "6",
          title = "Victory Screenshot & Result Proof",
          content = "The winning player MUST take a clear screenshot of the final victory screen showing player names and room code. The screenshot must be submitted in AD TOURNAMENT for Super Admin review and prize approval. Losers are strictly prohibited from submitting victory claims.",
          badgeText = "VERIFIED PROOF",
          badgeColor = Cyan400,
        )
      }

      item {
        RuleCard(
          number = "7",
          title = "Zero Tolerance: Cheating & Fake Proofs",
          content = "Manipulating screenshots, using third-party mods/hacks, leaving mid-game, or claiming false victories will trigger permanent account suspension, device ID blacklisting, and complete balance forfeiture.",
          badgeText = "PERMANENT BAN",
          badgeColor = Rose500,
        )
      }

      item {
        RuleCard(
          number = "8",
          title = "Wallet Deposit & Withdrawal Limits",
          content = "• Minimum Deposit: ৳50 via bKash or Nagad\n• Minimum Withdrawal: ৳200 to your verified mobile number\n• All financial transactions use non-floating integer accounting\n• Payouts processed after Super Admin security review",
          badgeText = "bKash / Nagad",
          badgeColor = Emerald400,
        )
      }
    }
  }
}

@Composable
private fun RuleCard(
  number: String,
  title: String,
  content: String,
  badgeText: String,
  badgeColor: Color,
) {
  TournamentCard(
    modifier = Modifier.fillMaxWidth(),
    backgroundColor = NavyCard,
    borderColor = CardBorder,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Indigo600.copy(alpha = 0.3f)),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = number,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Cyan400,
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = Color.White,
        )
      }
      Surface(
        color = badgeColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
      ) {
        Text(
          text = badgeText,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
          color = badgeColor,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = content,
      style = MaterialTheme.typography.bodySmall,
      color = Slate300,
      lineHeight = 18.sp,
    )
  }
}
