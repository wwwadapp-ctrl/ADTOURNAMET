package com.example.ui.referral

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import com.example.ui.components.UnifiedEsportsTopBar
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferAndEarnScreen(
    navController: NavController,
    viewModel: ReferAndEarnViewModel,
    currentUser: UserEntity? = null,
    wallet: WalletEntity? = null,
    unreadNotificationsCount: Int = 0,
    onWalletClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onAdminClick: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepNavyBg)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Identity Title
                Text(
                    text = "রেফার করুন ও ইনকাম করুন",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    ),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                // Hero Banner
                HeroBanner()

                Spacer(modifier = Modifier.height(24.dp))

                // Referral Code Card
                val referralCode = uiState.user?.referralCode?.ifBlank {
                    uiState.user?.uid?.take(6)?.uppercase() ?: ""
                } ?: ""
                ReferralCodeCard(referralCode, context)

                Spacer(modifier = Modifier.height(24.dp))

                // Share Button
                ShareButton(referralCode, context)

                Spacer(modifier = Modifier.height(24.dp))

                // Stats Section
                StatsSection(
                    totalRefers = uiState.user?.totalRefers ?: 0,
                    lockedBonus = (uiState.wallet?.lockedBonus ?: 0.0) / 100.0,
                    availableBonus = (uiState.wallet?.bonusBalance ?: 0.0) / 100.0
                )

                Spacer(modifier = Modifier.height(24.dp))

                // How it works
                HowItWorksSection()
            }
        }
    }
}

@Composable
fun HeroBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                    )
                )
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CardGiftcard,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "বন্ধুকে ইনভাইট করুন!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "আপনার রেফার কোড ব্যবহার করে বন্ধু প্রথম ডিপোজিট করলেই আপনি পাবেন ৳৪০ এবং বন্ধু পাবে ৳২০ ওয়েলকাম ক্যাশ!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ReferralCodeCard(code: String, context: Context) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("আপনার রেফার কোড", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { copyToClipboard(context, code) },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { copyToClipboard(context, code) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ShareButton(code: String, context: Context) {
    Button(
        onClick = { shareReferral(context, code) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("share_referral_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)) // WhatsApp Green
    ) {
        Icon(Icons.Default.Share, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text("বন্ধুদের সাথে শেয়ার করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun StatsSection(totalRefers: Int, lockedBonus: Double, availableBonus: Double) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("আপনার রেফারেল রিপোর্ট", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("মোট রেফার", totalRefers.toString(), Icons.Default.People, Modifier.weight(1f))
            StatCard("লকড বোনাস", "৳${lockedBonus.toInt()}", Icons.Default.Lock, Modifier.weight(1f))
            StatCard("বোনাস ব্যালেন্স", "৳${availableBonus.toInt()}", Icons.Default.AccountBalanceWallet, Modifier.weight(1f))
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun HowItWorksSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text("সহজ ৩টি নিয়ম", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        HowItWorksItem("১", "বন্ধুকে আপনার রেফার কোড দিয়ে অ্যাপটি শেয়ার করুন।")
        HowItWorksItem("২", "আপনার বন্ধুকে প্রথমবার ডিপোজিট করতে বলুন।")
        HowItWorksItem("৩", "বন্ধু ডিপোজিট করলেই আপনারা দুজনেই বোনাস পাবেন!")
    }
}

@Composable
fun HowItWorksItem(number: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(number, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Referral Code", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "রেফার কোড কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
}

private fun shareReferral(context: Context, code: String) {
    val shareText = """
        🎮 AD TOURNAMENT - ১vs১ লুডো ও ক্যারম টুর্নামেন্ট!
        
        আমার রেফার কোড ব্যবহার করে অ্যাকাউন্ট খুলে ডিপোজিট করলেই পাবেন ৳২০ ওয়েলকাম ক্যাশ বোনাস!
        
        👉 রেফার কোড: $code
        📥 ডাউনলোড লিংক: https://adturnamet.web.app
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "AD Tournament Invitation")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "বন্ধুদের সাথে শেয়ার করুন"))
}
