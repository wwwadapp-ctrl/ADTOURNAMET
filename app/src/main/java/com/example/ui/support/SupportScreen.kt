package com.example.ui.support

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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import com.example.ui.components.UnifiedEsportsTopBar
import com.example.ui.components.TournamentCard
import com.example.ui.theme.*

@Composable
fun SupportScreen(
  currentUser: UserEntity? = null,
  wallet: WalletEntity? = null,
  unreadNotificationsCount: Int = 0,
  onWalletClick: () -> Unit = {},
  onNotificationClick: () -> Unit = {},
  onProfileClick: () -> Unit = {},
  onAdminClick: (() -> Unit)? = null,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val clipboardManager: ClipboardManager = LocalClipboardManager.current
  var copiedNotice by remember { mutableStateOf<String?>(null) }

  Scaffold(
    topBar = {
      UnifiedEsportsTopBar(
        currentUser = currentUser,
        wallet = wallet,
        unreadNotificationsCount = unreadNotificationsCount,
        onWalletClick = onWalletClick,
        onNotificationClick = onNotificationClick,
        onProfileClick = onProfileClick,
        onAdminClick = onAdminClick
      )
    },
    containerColor = DeepNavyBg,
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .testTag("support_screen"),
    ) {
      // Page Identity Title
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.HeadsetMic,
            contentDescription = null,
            tint = Gold400,
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "HELP & SUPPORT",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Black,
              letterSpacing = 1.sp,
            ),
            color = Color.White,
          )
        }
      }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Feedback banner
      item {
        AnimatedVisibility(visible = copiedNotice != null) {
          Surface(
            color = Cyan500.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, Cyan400),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Cyan400, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = copiedNotice ?: "", color = Color.White, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
              IconButton(onClick = { copiedNotice = null }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400, modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }

      // 1. 24/7 SUPPORT BANNER
      item {
        TournamentCard(
          modifier = Modifier.fillMaxWidth(),
          backgroundColor = NavyCard,
          borderColor = NavyCardBorder,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Emerald600.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(Icons.Default.SupportAgent, contentDescription = null, tint = Emerald500, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "24/7 Match Assistance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
              )
              Text(
                text = "Contact Super Admin for room codes, disputes, or deposit queries",
                style = MaterialTheme.typography.bodySmall,
                color = Slate400,
              )
            }
          }
        }
      }

      // 2. OFFICIAL SUPPORT CHANNELS
      item {
        Column {
          Text(
            text = "DIRECT CHANNELS",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = Gold400,
          )
          Spacer(modifier = Modifier.height(10.dp))

          // WhatsApp Channel
          ContactChannelCard(
            icon = Icons.AutoMirrored.Filled.Chat,
            title = "WhatsApp Support",
            detail = "+880 1700 000000",
            actionLabel = "COPY NUMBER",
            onAction = {
              clipboardManager.setText(AnnotatedString("+8801700000000"))
              copiedNotice = "WhatsApp support number copied to clipboard"
            },
            badgeColor = Emerald500,
          )
          Spacer(modifier = Modifier.height(8.dp))

          // Telegram Channel
          ContactChannelCard(
            icon = Icons.Default.Send,
            title = "Telegram Official",
            detail = "@ADTournamentOfficial",
            actionLabel = "COPY HANDLE",
            onAction = {
              clipboardManager.setText(AnnotatedString("@ADTournamentOfficial"))
              copiedNotice = "Telegram channel copied to clipboard"
            },
            badgeColor = Cyan400,
          )
          Spacer(modifier = Modifier.height(8.dp))

          // Email Support
          ContactChannelCard(
            icon = Icons.Default.Email,
            title = "Official Email",
            detail = "support@adtournament.com",
            actionLabel = "COPY EMAIL",
            onAction = {
              clipboardManager.setText(AnnotatedString("support@adtournament.com"))
              copiedNotice = "Email address copied to clipboard"
            },
            badgeColor = Gold400,
          )
        }
      }

      // 3. HOW IT WORKS / MATCH PROTOCOL
      item {
        TournamentCard(
          modifier = Modifier.fillMaxWidth(),
          backgroundColor = NavyCard,
          borderColor = CardBorder,
        ) {
          Text(
            text = "HOW 1v1 MATCH COORDINATION WORKS",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = Gold400,
          )
          Spacer(modifier = Modifier.height(10.dp))
          val steps = listOf(
            "1. Join a match in AD TOURNAMENT using your playable wallet balance.",
            "2. Super Admin creates the room in original Ludo / Carrom and adds the code.",
            "3. Copy the Game Code revealed on your match card.",
            "4. Open the original game app (Ludo King or Carrom Disc Pool) and enter room code.",
            "5. Play the 1v1 battle with your opponent.",
            "6. Winner takes a full screenshot of the victory screen.",
            "7. Submit victory proof in AD TOURNAMENT. Super Admin verifies and credits prize!",
          )
          steps.forEach { step ->
            Text(
              text = step,
              style = MaterialTheme.typography.bodySmall,
              color = Slate300,
              modifier = Modifier.padding(vertical = 3.dp),
              lineHeight = 18.sp,
            )
          }
        }
      }

      // 4. FAQS
      item {
        Column {
          Text(
            text = "FREQUENTLY ASKED QUESTIONS",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = Gold400,
          )
          Spacer(modifier = Modifier.height(10.dp))
          FaqItem(
            question = "When does the Super Admin add the Room Code?",
            answer = "Room codes are published 5 to 10 minutes before the scheduled match time once both player slots are filled.",
          )
          Spacer(modifier = Modifier.height(8.dp))
          FaqItem(
            question = "What if my opponent doesn't show up in the game?",
            answer = "Take a screenshot showing you waited in the room for 5+ minutes. Contact WhatsApp support or submit a dispute to receive a full entry fee refund.",
          )
          Spacer(modifier = Modifier.height(8.dp))
          FaqItem(
            question = "How are winning prizes credited?",
            answer = "Upon victory proof verification by the Super Admin, the prize pool (minus platform commission) is credited to your Winning Balance, which is directly withdrawable via bKash or Nagad.",
          )
          Spacer(modifier = Modifier.height(8.dp))
          FaqItem(
            question = "Why can't I edit my registered mobile number?",
            answer = "To protect wallet funds and maintain strict single-account fairness, mobile numbers cannot be altered in-app. Contact support if you need verified number migration.",
          )
        }
      }
    }
  }
}
}

@Composable
private fun ContactChannelCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  detail: String,
  actionLabel: String,
  onAction: () -> Unit,
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
            .size(36.dp)
            .clip(CircleShape)
            .background(badgeColor.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(text = title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
          Text(text = detail, style = MaterialTheme.typography.bodySmall, color = Slate400)
        }
      }
      OutlinedButton(
        onClick = onAction,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, badgeColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = badgeColor),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(34.dp),
      ) {
        Text(actionLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
      }
    }
  }
}

@Composable
private fun FaqItem(question: String, answer: String) {
  var isExpanded by remember { mutableStateOf(false) }
  TournamentCard(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { isExpanded = !isExpanded },
    backgroundColor = NavyCard,
    borderColor = CardBorder,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = question,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
        modifier = Modifier.weight(1f),
      )
      Icon(
        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
        contentDescription = null,
        tint = Gold400,
      )
    }
    AnimatedVisibility(visible = isExpanded) {
      Text(
        text = answer,
        style = MaterialTheme.typography.bodySmall,
        color = Slate300,
        modifier = Modifier.padding(top = 10.dp),
        lineHeight = 18.sp,
      )
    }
  }
}
