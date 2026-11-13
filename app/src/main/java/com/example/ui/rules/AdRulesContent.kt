package com.example.ui.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.BengaliStrings
import com.example.core.i18n.LocalAppStrings
import com.example.ui.auth.AdAuthBackground

// AD Tournament rules & fair play. VISUAL / TEXT ONLY: this page never reads or writes user data.
// Deliberately says only what a player must do and what they get, never how the system works inside.

private sealed class RuleBlock {
    class Para(val text: String) : RuleBlock()
    class Label(val text: String) : RuleBlock()
    class Bullets(val items: List<String>) : RuleBlock()
    class Steps(val items: List<String>) : RuleBlock()
    class Chips(val items: List<String>) : RuleBlock()
    class Warn(val text: String) : RuleBlock()
    class Prize(val rows: List<Pair<String, String>>) : RuleBlock()
}

private class RuleSection(val icon: ImageVector, val title: String, val blocks: List<RuleBlock>)

private class RulesText(
    val header: String,
    val heroTitle: String,
    val heroSubtitle: String,
    val intro: String,
    val ruleWord: String,
    val sections: List<RuleSection>,
    val footer1: String,
    val footer2: String
)

private fun bnDigits(s: String): String =
    s.map { if (it in '0'..'9') '০' + (it - '0') else it }.joinToString("")

private fun amount(t: Double): String =
    if (t % 1.0 == 0.0) String.format(java.util.Locale.US, "%.0f", t) else String.format(java.util.Locale.US, "%.2f", t)

private fun bengaliRules(minDeposit: Double, minWithdraw: Double) = RulesText(
    header = "নিয়মাবলী",
    heroTitle = "নিয়মাবলী ও ফেয়ার-প্লে",
    heroSubtitle = "আপনার প্রতিটি খেলা হোক নিরাপদ, সৎ আর আনন্দময়",
    intro = "প্রিয় খেলোয়াড়, AD Tournament পরিবারে আপনাকে আন্তরিক স্বাগতম! আমরা চাই প্রতিটি ম্যাচ হোক সম্পূর্ণ ন্যায্য, নিরাপদ আর আনন্দে ভরা। তাই অনুগ্রহ করে নিচের নিয়মগুলো মন দিয়ে পড়ে নিন।",
    ruleWord = "নিয়ম",
    sections = listOf(
        RuleSection(Icons.Default.VerifiedUser, "ফেয়ার-প্লে নীতি", listOf(
            RuleBlock.Para("সৎ খেলাই আমাদের সবচেয়ে বড় শক্তি। এখানে সবাই পান সমান সুযোগ, আর জয় আসে শুধু নিজের দক্ষতায়।"),
            RuleBlock.Label("যা কঠোরভাবে নিষিদ্ধ:"),
            RuleBlock.Bullets(listOf(
                "জাল বা এডিট করা স্ক্রিনশট জমা দেওয়া",
                "মড, হ্যাক বা যেকোনো অসৎ উপায় ব্যবহার করা",
                "খেলার মাঝপথে ইচ্ছাকৃতভাবে বেরিয়ে যাওয়া",
                "মিথ্যা জয় দাবি করা"
            )),
            RuleBlock.Warn("এসব ধরা পড়লে অ্যাকাউন্ট স্থায়ীভাবে বন্ধ করা হবে, ডিভাইস ব্ল্যাকলিস্ট হবে এবং পুরো ব্যালেন্স বাজেয়াপ্ত হবে।")
        )),
        RuleSection(Icons.Default.Casino, "লুডু ম্যাচ কীভাবে খেলবেন", listOf(
            RuleBlock.Steps(listOf(
                "পছন্দের ম্যাচে জয়েন করুন। প্রতিটি ম্যাচ ১ বনাম ১।",
                "জয়েন করার পর ম্যাচ ছেড়ে যাওয়া যায় না, তাই আগেই ইন্টারনেট আর গেম প্রস্তুত রাখুন।",
                "খেলা শুরুর ৫-১০ মিনিট আগে রুম কোড পাবেন, যা শুধু আপনি আর আপনার প্রতিপক্ষ দেখতে পাবেন।",
                "Ludo King অ্যাপে সেই কোড দিয়ে রুমে ঢুকুন, আর মন দিয়ে খেলুন। শুভকামনা রইল!"
            ))
        )),
        RuleSection(Icons.Default.PhotoCamera, "জয়ের স্ক্রিনশট জমা", listOf(
            RuleBlock.Bullets(listOf(
                "শুধু বিজয়ীই স্ক্রিনশট জমা দেবেন।",
                "জয়ের পর্দার একটি পরিষ্কার ছবি দিন, যেখানে দুই খেলোয়াড়ের নাম আর রুম কোড স্পষ্ট দেখা যায়।",
                "যাচাই শেষে পুরস্কার আপনার ওয়ালেটে যোগ হয়ে যাবে।",
                "হেরে গেলে স্ক্রিনশট জমা দেবেন না। মন খারাপ নয়, পরের জয়টা আপনারই হতে পারে!"
            ))
        )),
        RuleSection(Icons.Default.EmojiEvents, "পুরস্কার কত পাবেন", listOf(
            RuleBlock.Para("দুজনের এন্ট্রি ফি থেকে মাত্র ১০% প্ল্যাটফর্ম ফি বাদে বাকি পুরোটাই বিজয়ীর।"),
            RuleBlock.Prize(listOf("এন্ট্রি ৳৫০" to "জিতলে ৳৯০", "এন্ট্রি ৳১০০" to "জিতলে ৳১৮০", "এন্ট্রি ৳২০০" to "জিতলে ৳৩৬০"))
        )),
        RuleSection(Icons.Default.AccountBalanceWallet, "ডিপোজিট", listOf(
            RuleBlock.Para("বিকাশ বা নগদে খুব সহজেই ডিপোজিট করুন। ডিপোজিট পাতার নির্দেশনা মেনে টাকা পাঠালেই কাজ শেষ।"),
            RuleBlock.Chips(listOf("সর্বনিম্ন ডিপোজিট ৳${bnDigits(amount(minDeposit))}"))
        )),
        RuleSection(Icons.Default.AccountBalance, "উইথড্র", listOf(
            RuleBlock.Para("আপনার জেতা টাকা তুলুন নিজের বিকাশ বা নগদ নম্বরে।"),
            RuleBlock.Chips(listOf("সাধারণত ৫-১০ মিনিটে পৌঁছে যায়", "কোনো চার্জ নেই", "সর্বনিম্ন উইথড্র ৳${bnDigits(amount(minWithdraw))}"))
        )),
        RuleSection(Icons.Default.CardGiftcard, "রেফার করে আয়", listOf(
            RuleBlock.Para("বন্ধুদের সাথে আনন্দ ভাগ করুন, আর একসাথে জিতুন!"),
            RuleBlock.Bullets(listOf(
                "বন্ধুকে আপনার রেফার কোড দিন।",
                "কোড দিয়ে রেজিস্টার করলেই বন্ধু পাবেন **৳২০ স্বাগত বোনাস**।",
                "বন্ধু প্রথমবার ৳১০০ বা তার বেশি ডিপোজিট করলে আপনি পাবেন **৳৪০ বোনাস**।",
                "যত বন্ধু, তত বোনাস!"
            ))
        )),
        RuleSection(Icons.Default.SportsEsports, "মিনি-গেম", listOf(
            RuleBlock.Para("অল্প সময়ে একটু বাড়তি মজা আর আনন্দের জন্য।"),
            RuleBlock.Bullets(listOf(
                "ছক্কা, হেড-টেল, স্ক্র্যাচ কার্ড আর স্পিন হুইল খেলা হয় আলাদা গেম ওয়ালেট থেকে।",
                "মেইন ওয়ালেট থেকে গেম ওয়ালেটে টাকা ট্রান্সফার করে খেলুন।",
                "গেম ওয়ালেটের টাকা খুব দ্রুত মেইন ওয়ালেটে ফিরিয়ে আনা যায়।"
            ))
        )),
        RuleSection(Icons.Default.Bolt, "অটো লুডু", listOf(
            RuleBlock.Para("কোনো রুম কোডের ঝামেলা নেই, খেলা হয় অ্যাপের ভেতরেই!"),
            RuleBlock.Bullets(listOf(
                "নিজে ম্যাচ বানান (এন্ট্রি ৳১০ থেকে ৳৫০০), অথবা অন্যের ম্যাচে জয়েন করুন।",
                "জিতলে পাবেন এন্ট্রি ফি-র ১.৮ গুণ।",
                "সময় হলে \"খেলায় প্রবেশ করুন\" চাপুন।",
                "শুরুর ১০ মিনিটের মধ্যে কেউ না এলে দুজনের টাকাই ফেরত; আর একজন এলে তিনিই জয়ী।",
                "কোনো কারণে ম্যাচ বাতিল হলে টাকা নিজে থেকেই ফেরত আসে।"
            ))
        ))
    ),
    footer1 = "নিয়ম মেনে খেলুন, নিরাপদে জিতুন।",
    footer2 = "AD Tournament-এ আপনার প্রতিটি জয় হোক গর্বের!"
)

private fun englishRules(minDeposit: Double, minWithdraw: Double) = RulesText(
    header = "Rules",
    heroTitle = "Rules & Fair Play",
    heroSubtitle = "May every game you play be safe, fair and joyful",
    intro = "Dear player, a warm welcome to the AD Tournament family! We want every match to be completely fair, safe and full of fun. So please take a moment to read the rules below carefully.",
    ruleWord = "RULE",
    sections = listOf(
        RuleSection(Icons.Default.VerifiedUser, "Fair Play Policy", listOf(
            RuleBlock.Para("Honest play is our greatest strength. Here everyone gets an equal chance, and victory comes only from skill."),
            RuleBlock.Label("Strictly prohibited:"),
            RuleBlock.Bullets(listOf(
                "Submitting fake or edited screenshots",
                "Using mods, hacks or any unfair means",
                "Leaving a match midway on purpose",
                "Claiming a false victory"
            )),
            RuleBlock.Warn("If caught, the account will be permanently closed, the device will be blacklisted and the entire balance will be forfeited.")
        )),
        RuleSection(Icons.Default.Casino, "How to Play a Ludo Match", listOf(
            RuleBlock.Steps(listOf(
                "Join the match you like. Every match is 1 vs 1.",
                "Once you join, you cannot leave the match, so get your internet and game ready beforehand.",
                "You will get the room code 5-10 minutes before the start, visible only to you and your opponent.",
                "Enter the room with that code in the Ludo King app and play your best. Good luck!"
            ))
        )),
        RuleSection(Icons.Default.PhotoCamera, "Submitting the Victory Screenshot", listOf(
            RuleBlock.Bullets(listOf(
                "Only the winner submits a screenshot.",
                "Upload a clear picture of the victory screen showing both players' names and the room code.",
                "After verification, the prize is added to your wallet.",
                "If you lose, please don't submit a screenshot. Chin up, the next win could be yours!"
            ))
        )),
        RuleSection(Icons.Default.EmojiEvents, "How Much You Win", listOf(
            RuleBlock.Para("Only a 10% platform fee is taken from both entry fees; everything else goes to the winner."),
            RuleBlock.Prize(listOf("Entry ৳50" to "Win ৳90", "Entry ৳100" to "Win ৳180", "Entry ৳200" to "Win ৳360"))
        )),
        RuleSection(Icons.Default.AccountBalanceWallet, "Deposit", listOf(
            RuleBlock.Para("Deposit easily with bKash or Nagad. Just follow the instructions on the deposit page and you're done."),
            RuleBlock.Chips(listOf("Minimum deposit ৳${amount(minDeposit)}"))
        )),
        RuleSection(Icons.Default.AccountBalance, "Withdraw", listOf(
            RuleBlock.Para("Withdraw your winnings to your own bKash or Nagad number."),
            RuleBlock.Chips(listOf("Usually arrives in 5-10 minutes", "No charge", "Minimum withdrawal ৳${amount(minWithdraw)}"))
        )),
        RuleSection(Icons.Default.CardGiftcard, "Earn by Referring", listOf(
            RuleBlock.Para("Share the joy with friends and win together!"),
            RuleBlock.Bullets(listOf(
                "Give your referral code to a friend.",
                "When they register with your code, your friend gets a **৳20 welcome bonus**.",
                "When your friend makes a first deposit of ৳100 or more, you get a **৳40 bonus**.",
                "More friends, more bonus!"
            ))
        )),
        RuleSection(Icons.Default.SportsEsports, "Mini Games", listOf(
            RuleBlock.Para("A little extra fun in a short time."),
            RuleBlock.Bullets(listOf(
                "Dice, Head-Tail, Scratch Card and Spin Wheel are played from a separate game wallet.",
                "Transfer money from your main wallet to the game wallet to play.",
                "Money in the game wallet can be moved back to the main wallet very quickly."
            ))
        )),
        RuleSection(Icons.Default.Bolt, "Auto Ludo", listOf(
            RuleBlock.Para("No room code hassle, the game is played right inside the app!"),
            RuleBlock.Bullets(listOf(
                "Create your own match (entry ৳10 to ৳500), or join someone else's.",
                "Win and you get 1.8 times the entry fee.",
                "When it's time, tap \"Enter game\".",
                "If nobody comes within 10 minutes of the start, both get their money back; if only one comes, that player wins.",
                "If a match is cancelled for any reason, the money comes back automatically."
            ))
        ))
    ),
    footer1 = "Play by the rules, win safely.",
    footer2 = "May every win at AD Tournament make you proud!"
)

private val RuGoldText = Brush.verticalGradient(listOf(Color(0xFFFFF3C4), Color(0xFFFFC94A), Color(0xFFC98A10)))
private val RuBody = TextStyle(fontSize = 15.sp, lineHeight = 25.sp, color = Color(0xFFE6ECFF))

private fun boldGold(text: String): AnnotatedString = buildAnnotatedString {
    text.split("**").forEachIndexed { i, part ->
        if (i % 2 == 1) withStyle(SpanStyle(color = Color(0xFFFFD45A), fontWeight = FontWeight.Bold)) { append(part) }
        else append(part)
    }
}

/** Full-screen rules page opened from the sign-up screen (no app bar). */
@Composable
fun FairPlayRulesScreen(
    minDepositTaka: Double,
    minWithdrawTaka: Double,
    onNavigateBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AdAuthBackground(modifier = Modifier.fillMaxSize())
        AdRulesContent(
            minDepositTaka = minDepositTaka,
            minWithdrawTaka = minWithdrawTaka,
            onBack = onNavigateBack,
            modifier = Modifier.systemBarsPadding()
        )
    }
}

/** The rules content itself. With onBack = null it shows no back button (used inside the app's own Rules screen). */
@Composable
fun AdRulesContent(
    minDepositTaka: Double,
    minWithdrawTaka: Double,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val t = if (strings === BengaliStrings) bengaliRules(minDepositTaka, minWithdrawTaka) else englishRules(minDepositTaka, minWithdrawTaka)
    val num: (Int) -> String = { n -> if (strings === BengaliStrings) bnDigits(n.toString()) else n.toString() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (onBack != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(Color(0xFF2E4BB8), Color(0xFF0F1A52))))
                            .border(2.dp, Color(0xFFE8B23A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(t.header, style = TextStyle(brush = RuGoldText, fontSize = 20.sp, fontWeight = FontWeight.Bold))
            }
        }

        // hero
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .background(Brush.radialGradient(listOf(Color(0xFFFFC107).copy(alpha = 0.35f), Color.Transparent)), CircleShape)
                )
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFFFFC94A), modifier = Modifier.size(60.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                t.heroTitle,
                style = TextStyle(
                    fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, lineHeight = 35.sp,
                    shadow = Shadow(color = Color(0xFF8A6414), offset = Offset(0f, 5f), blurRadius = 0f)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(t.heroSubtitle, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFD27A), textAlign = TextAlign.Center))
        }

        // welcome note
        Text(
            t.intro,
            style = TextStyle(fontSize = 15.sp, lineHeight = 26.sp, color = Color(0xFFF3E6C0)),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFC107).copy(alpha = 0.12f), Color(0xFFFFC107).copy(alpha = 0.04f))))
                .border(1.dp, Color(0x99E8B23A), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        )

        t.sections.forEachIndexed { i, s -> AdRuleCard(number = num(i + 1), ruleWord = t.ruleWord, section = s, num = num) }

        Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(t.footer1, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD45A), textAlign = TextAlign.Center))
            Spacer(modifier = Modifier.height(4.dp))
            Text(t.footer2, style = TextStyle(fontSize = 14.5.sp, color = Color(0xFFC9D3FF), textAlign = TextAlign.Center, lineHeight = 22.sp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdRuleCard(number: String, ruleWord: String, section: RuleSection, num: (Int) -> String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Brush.verticalGradient(listOf(Color(0xFFFFE7A0), Color(0xFF9A6A10), Color(0xFFE8B23A))), RoundedCornerShape(18.dp))
            .padding(2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1C2350), Color(0xFF0F1438))))
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF2A3156), Color(0xFF141838))))
                        .border(1.5.dp, Color(0xFFC9A04A), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(section.icon, contentDescription = null, tint = Color(0xFFFFC94A), modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("$ruleWord $number", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color(0xFFC9A04A)))
                    Text(section.title, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, lineHeight = 26.sp))
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Color(0xCCE8B23A), Color.Transparent)))
            )
            section.blocks.forEach { b ->
                when (b) {
                    is RuleBlock.Para -> Text(boldGold(b.text), style = RuBody)
                    is RuleBlock.Label -> Text(b.text, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD27A)))
                    is RuleBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        b.items.forEach { item ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 9.dp)
                                        .size(7.dp)
                                        .rotate(45f)
                                        .background(Brush.linearGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE8A820))))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(boldGold(item), style = RuBody)
                            }
                        }
                    }
                    is RuleBlock.Steps -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        b.items.forEachIndexed { i, item ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Brush.radialGradient(listOf(Color(0xFFFFF3C4), Color(0xFFFFC94A), Color(0xFFC98A10)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(num(i + 1), style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3A2300)))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(boldGold(item), style = RuBody)
                            }
                        }
                    }
                    is RuleBlock.Chips -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        b.items.forEach { chip ->
                            Text(
                                chip,
                                style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFE7A0)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Brush.verticalGradient(listOf(Color(0xFF3A2E10), Color(0xFF1E1606))))
                                    .border(1.dp, Color(0xFFC9A04A), RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    is RuleBlock.Warn -> Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF4A1418), Color(0xFF2A0A0E))))
                            .border(1.5.dp, Color(0xFFB8452E), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(b.text, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFCF6B), lineHeight = 22.sp))
                    }
                    is RuleBlock.Prize -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFF2A3156), Color(0xFF151A33))))
                            .border(1.5.dp, Color(0xFFC9A04A), RoundedCornerShape(12.dp))
                    ) {
                        b.rows.forEachIndexed { i, (entry, win) ->
                            if (i > 0) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x59C9A04A)))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(entry, style = TextStyle(fontSize = 15.sp, color = Color(0xFFE6ECFF)))
                                Text(win, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD45A)))
                            }
                        }
                    }
                }
            }
        }
    }
}
