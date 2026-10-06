package com.example.ui.support

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppSettingsEntity
import com.example.domain.model.UserEntity
import com.example.domain.model.WalletEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

enum class InlineActionType {
    NONE,
    MAIN_MENU,
    WHATSAPP_CARD,
    WALLET_CARD,
    REFERRAL_CONFIRMATION
}

data class SupportMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: InlineActionType = InlineActionType.NONE
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSupportScreen(
    appSettings: AppSettingsEntity? = null,
    currentUser: UserEntity? = null,
    wallet: WalletEntity? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToMatches: () -> Unit = {},
    userName: String = currentUser?.effectiveName?.takeIf { it.isNotBlank() } ?: "দেলোয়ার",
    availableBalance: Double = wallet?.balanceAmount ?: 0.0,
    winningBalance: Double = wallet?.winningsAmount ?: 0.0,
    bonusBalance: Double = wallet?.bonusAmount ?: 0.0,
    whatsappNumber: String = appSettings?.activeWhatsappNumber?.takeIf { it.isNotBlank() } ?: "8801700000000"
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isBotTyping by remember { mutableStateOf(true) }

    // ৮-১০ রকমের র্যান্ডম জীবন্ত শুভেচ্ছা বার্তা
    val randomGreetings = remember(userName) {
        listOf(
            "আসসালামু আলাইকুম $userName ভাই! কেমন আছেন? আজ কয়টি ম্যাচ খেললেন? আশা করি দিনটি দারুণ কাটছে!",
            "আসসালামু আলাইকুম $userName ভাই! আজকের দিনে ভাগ্য কেমন সহায় দিল? নাকি গেমের ভাগ্য কিছুটা খারাপ গেল? যেকোনো প্রয়োজনে আমি পাশে আছি!",
            "হ্যালো $userName ভাই! কেমন চলছে সবকিছু? আজ কি বোর্ডে বড় কোনো জয় আসলো, নাকি নতুন ম্যাচের প্রস্তুতি নিচ্ছেন?",
            "আসসালামু আলাইকুম $userName ভাই! আশা করি ভালো আছেন। আজ কি লুডু বা ক্যারম বোর্ডে ঝড় তোলার মুডে আছেন? কীভাবে সাহায্য করতে পারি?",
            "শুভ দিন $userName ভাই! আজকের টুর্নামেন্টে আপনার পারফরম্যান্স কেমন হলো? কোনো ম্যাচ জিতলেন তো?",
            "আসসালামু আলাইকুম $userName ভাই! দিনকাল কেমন যাচ্ছে? আজকে কি উইনিং ব্যালেন্স বাড়ানোর মিশন চলছে? যেকোনো প্রশ্নে আমি প্রস্তুত!",
            "হ্যালো $userName ভাই! আশা করি মন মেজাজ ভালো আছে। আজ কয়টা ম্যাচ খেলে ফেললেন? গেম বা ওয়ালেট নিয়ে কোনো সমস্যা হলে আমাকে জানান!",
            "আসসালামু আলাইকুম $userName ভাই! কেমন আছেন? আজ কি লাক আপনার ফেভারে ছিল, নাকি অপনেন্ট বেশি কঠিন পড়েছিল? বলুন কীভাবে সহযোগিতা করতে পারি?"
        )
    }

    val messages = remember { mutableStateListOf<SupportMessage>() }

    // স্ক্রিনে ঢোকার সাথে সাথে ১ সেকেন্ড টাইপিং ফিল দিয়ে র্যান্ডম শুভেচ্ছা পাঠানো
    LaunchedEffect(Unit) {
        isBotTyping = true
        delay(1000L)
        val initialGreeting = randomGreetings.random()
        messages.add(
            SupportMessage(
                isUser = false,
                text = initialGreeting,
                actionType = InlineActionType.MAIN_MENU
            )
        )
        isBotTyping = false
    }

    fun openWhatsappChat() {
        val cleanPhone = whatsappNumber.replace("+", "").replace(" ", "").ifBlank { "8801700000000" }
        val msg = Uri.encode("আসসালামু আলাইকুম এডমিন দেলোয়ার ভাই, আমি AD TOURNAMENT অ্যাপ থেকে সহায়তা চাচ্ছি। আমার নাম: $userName")
        val url = "https://wa.me/$cleanPhone?text=$msg"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun processLocalBotReply(userPrompt: String) {
        val raw = userPrompt.trim()
        val text = raw.lowercase()

        coroutineScope.launch {
            isBotTyping = true
            listState.animateScrollToItem(messages.size)
            delay(1100L) // ১.১ সেকেন্ড স্বাভাবিক টাইপিং বিলম্ব

            val botResponse: SupportMessage = when {
                // ১. এডমিন দেলোয়ার ভাই (বাংলা, ইংরেজি ও বাংলিশ কি-ওয়ার্ড)
                text.contains("এডমিন") || text.contains("দেলোয়ার") || text.contains("দেলোয়ার") ||
                text.contains("delowar") || text.contains("admin") || text.contains("মালিক") ||
                text.contains("kotha bolte chai") || text.contains("contact") -> {
                    SupportMessage(
                        isUser = false,
                        text = "$userName ভাই, আমাদের সম্মানিত এডমিন দেলোয়ার ভাই সরাসরি আপনার সাথে কথা বলতে প্রস্তুত।\n\nনিচের বাটনে চাপ দিয়ে সরাসরি ওনার অফিশিয়াল হোয়াটসঅ্যাপে যুক্ত হোন!",
                        actionType = InlineActionType.WHATSAPP_CARD
                    )
                }

                // ২. লাইভ ব্যালেন্স চেক
                text.contains("ব্যালেন্স") || text.contains("balance") || text.contains("wallet") ||
                text.contains("taka koto") || text.contains("koto ache") -> {
                    SupportMessage(
                        isUser = false,
                        text = "💰 আপনার ওয়ালেট ব্যালেন্সের বর্তমান অবস্থা:\n\n" +
                                "• 💵 মূল ব্যালেন্স: ৳$availableBalance\n" +
                                "• 🏆 উইনিং ব্যালেন্স: ৳$winningBalance\n" +
                                "• 🎁 বোনাস ব্যালেন্স: ৳$bonusBalance\n\n" +
                                "$userName ভাই, যেকোনো ১v১ ম্যাচে অংশ নিয়ে এই ব্যালেন্স ব্যবহার করতে পারবেন!",
                        actionType = InlineActionType.WALLET_CARD
                    )
                }

                // ৩. প্রো-গেমিং টিপস ও ট্রিকস
                text.contains("টিপস") || text.contains("tips") || text.contains("trick") ||
                text.contains("jitte") || text.contains("jitar") || text.contains("কৌশল") || text.contains("জেতার") -> {
                    SupportMessage(
                        isUser = false,
                        text = "১v১ ম্যাচে জেতার জন্য কিছু প্রো-টিপস $userName ভাই: 🎯\n\n" +
                                "১. শুরুতেই সব গুটি বের করার ঝুঁকি না নিয়ে একটি গুটিকে দ্রুত স্টার বা সেফ জোনে নিয়ে যান।\n" +
                                "২. প্রতিপক্ষের গুটির ঠিক পেছনে না থেকে অন্তত ৬ ঘরের নিরাপদ দূরত্ব বজায় রাখুন।\n" +
                                "৩. তাড়াহুড়ো করবেন না—প্রতিপক্ষের প্রতিটি চাল পর্যবেক্ষণ করে তাদের ভুলের সুযোগ নিন!\n\n" +
                                "শান্ত মাথায় খেললে জয় আপনারই হবে ইনশাআল্লাহ! 🏆",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ৪. চিটিং বা ফেক রিপোর্ট
                text.contains("চিটিং") || text.contains("ফেক") || text.contains("cheat") ||
                text.contains("fake") || text.contains("palise") || text.contains("chiter") -> {
                    SupportMessage(
                        isUser = false,
                        text = "AD TOURNAMENT-এ চিটিং বা ফেক স্ক্রিনশটের বিরুদ্ধে জিরো টলারেন্স নীতি রয়েছে! 🛡️\n\n" +
                                "$userName ভাই, আপনার ম্যাচের স্পষ্ট উইনিং স্ক্রিনশট নিয়ে সরাসরি এডমিন দেলোয়ার ভাইকে রিপোর্ট করুন। এডমিন ম্যানুয়ালি যাচাই করে প্রতিপক্ষের একাউন্ট ব্যান করবেন এবং আপনার পুরস্কার বুঝিয়ে দেবেন।",
                        actionType = InlineActionType.WHATSAPP_CARD
                    )
                }

                // ৫. টাকা যোগ / ডিপোজিট
                text.contains("টাকা যোগ") || text.contains("deposit") || text.contains("ডিপোজিট") ||
                text.contains("টাকা পাঠাব") || text.contains("add taka") || text.contains("send money") -> {
                    SupportMessage(
                        isUser = false,
                        text = "টাকা যোগ করা একদম সহজ $userName ভাই! 💸\n\n" +
                                "১. ওয়ালেট স্ক্রিনে গিয়ে বিকাশ বা নগদ নম্বরটি কপি করুন।\n" +
                                "২. আপনার বিকাশ/নগদ অ্যাপ থেকে Personal নম্বরে Send Money করুন।\n" +
                                "৩. এরপর অ্যাপে এসে ট্রানজেকশন আইডি (TrxID) এবং কত টাকা পাঠিয়েছেন তা লিখে সাবমিট করুন।\n\n" +
                                "৫ থেকে ১৫ মিনিটের মধ্যেই এডমিন ভেরিফাই করে ব্যালেন্স যোগ করে দেবেন!",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ৬. রুম কোড
                text.contains("রুম কোড") || text.contains("room") || text.contains("code") || text.contains("kod") -> {
                    SupportMessage(
                        isUser = false,
                        text = "ম্যাচে ২ জন প্লেয়ার জয়েন করার পরপরই এডমিন অ্যাপের ভেতর রুম কোড দিয়ে দেন। 🔑\n\n" +
                                "কোডটি পাওয়ার সাথে সাথে কপি করে Ludo King অ্যাপ ওপেন করুন -> 'Play with Friends'-এ যান -> 'Join Room'-এ কোডটি পেস্ট করে ম্যাচে প্রবেশ করুন। খেলা শেষে একটি স্পষ্ট স্ক্রিনশট নিতে ভুলবেন না যেন!",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ৭. অ্যাপ পরিচিতি ও রেফারেল
                text.contains("app") || text.contains("bisoy") || text.contains("kisher") ||
                text.contains("কী অ্যাপ") || text.contains("বিষয়") || text.contains("নিয়ম") -> {
                    SupportMessage(
                        isUser = false,
                        text = "AD TOURNAMENT হলো দেশের বিশ্বস্ত ১v১ লুডু ও ক্যারম ইস্পোর্টস প্ল্যাটফর্ম! 🎮\n\n" +
                                "এখানে ১v১ ম্যাচ খেলে সরাসরি নগদ টাকা জেতা যায় এবং দ্রুত বিকাশ/নগদে উত্তোলন করা যায়।\n\n" +
                                "আর হ্যাঁ $userName ভাই, আপনি কি আমাদের বিশেষ রেফারেল বোনাস পলিসি সম্পর্কে জানেন? বন্ধুদের ইনভাইট করে কিন্তু ঘরে বসেই প্রতিদিন ইনকাম করা যায়! এ বিষয়ে কি বিস্তারিত জানার আগ্রহ আছে আপনার?",
                        actionType = InlineActionType.REFERRAL_CONFIRMATION
                    )
                }

                // রেফারেল সম্মতি (হ্যাঁ)
                text == "he" || text == "ha" || text == "yes" || text.contains("রেফার নিয়ম জানতে চাই") || text == "হ্যাঁ" -> {
                    SupportMessage(
                        isUser = false,
                        text = "দারুণ $userName ভাই! রেফার নিয়মটা একদম সহজ: 🎁\n\n" +
                                "১. প্রোফাইল থেকে আপনার রেফার কোডটি বন্ধুদের শেয়ার করুন।\n" +
                                "২. আপনার কোড দিয়ে বন্ধু একাউন্ট খুললেই আপনার ওয়ালেটে ৳৪০ লকড বোনাস জমা হবে!\n" +
                                "৩. আপনার বন্ধু যখনই প্রথমবার ডিপোজিট করবে, সাথে সাথে এই ৳৪০ আনলক হয়ে মূল বোনাস ব্যালেন্সে চলে আসবে!\n" +
                                "৪. প্রতিটি পেইড ম্যাচে এন্ট্রি ফির সর্বোচ্চ ১০% পর্যন্ত এই বোনাস থেকে ব্যবহার করা যায়।",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // রেফারেল অসম্মতি (না)
                text == "na" || text == "no" || text.contains("পরে জানব") || text == "না" -> {
                    SupportMessage(
                        isUser = false,
                        text = "কোনো সমস্যা নেই $userName ভাই! টুর্নামেন্ট, ম্যাচ বা ওয়ালেট সংক্রান্ত যেকোনো প্রয়োজনে আমাকে নিঃসঙ্কোচে জানাতে পারেন। আপনার দিনটি শুভ হোক!",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ১. টাকা তোলা / উইথড্র
                text.contains("উইথড্র") || text.contains("withdraw") || text.contains("cashout") || text.contains("ক্যাশআউট") || text.contains("tulbo") || text.contains("তুলব") || text.contains("তুলতে") || text.contains("taka ber korbo") -> {
                    SupportMessage(
                        isUser = false,
                        text = "টাকা তোলা একদম সহজ ও দ্রুত $userName ভাই! 💵\n\n১. ওয়ালেট স্ক্রিনে গিয়ে 'Withdraw' অপশনে চাপ দিন।\n২. আপনার বিকাশ বা নগদ পার্সোনাল নম্বর এবং টাকার পরিমাণ লিখুন।\n৩. সাবমিট করলেই অল্প সময়ের মধ্যে এডমিন দেলোয়ার ভাই ভেরিফাই করে সরাসরি আপনার নম্বরে টাকা পাঠিয়ে দেবেন!\n\n(মনে রাখবেন: টাকা তুলতে হলে আপনার ওয়ালেটে উইনিং ব্যালেন্স থাকতে হবে)।",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ২. রিফান্ড ও ম্যাচ বাতিল গ্যারান্টি
                text.contains("রিফান্ড") || text.contains("refund") || text.contains("ফেরত") || text.contains("বাতিল") || text.contains("cancel") || text.contains("ferot") || text.contains("opponent aseni") || text.contains("1 ghonta") -> {
                    SupportMessage(
                        isUser = false,
                        text = "AD TOURNAMENT-এ আপনার টাকার ১০০% নিরাপত্তা রয়েছে $userName ভাই! 🛡️\n\nম্যাচ শুরুর সময় থেকে ১ ঘণ্টার মধ্যে যদি ২য় প্লেয়ার জয়েন না করে অথবা এডমিন রুম কোড না দেয়, তবে আমাদের সিস্টেম স্বয়ংক্রিয়ভাবে (Auto-Sweep) ম্যাচটি বাতিল করে দেয় এবং আপনার সম্পূর্ণ এন্ট্রি ফি ওয়ালেটে রিফান্ড করে দেয়!",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ৩. খেলার পর রেজাল্ট ও উইনিং স্ক্রিনশট জমা
                text.contains("রেজাল্ট") || text.contains("result") || text.contains("স্ক্রিনশট") || text.contains("screenshot") || text.contains("winner") || text.contains("jitechi") || text.contains("jitci") || text.contains("screen shot") -> {
                    SupportMessage(
                        isUser = false,
                        text = "অভিনন্দন $userName ভাই! 🏆\n\n১. ম্যাচ জেতার পরপরই লুডু কিংয়ের স্পষ্ট উইনিং স্ক্রিনশট নিন।\n২. আমাদের অ্যাপে সেই ম্যাচের 'Submit Result' অপশনে যান।\n৩. 'I Won' সিলেক্ট করে স্ক্রিনশটটি আপলোড করুন।\n\nএডমিন চেক করে সাথে সাথে আপনার উইনিং ব্যালেন্সে পুরস্কার যোগ করে দেবেন!",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ৪. ডিপোজিট দেরি হওয়া / টাকা এখনও আসেনি (হোয়াটসঅ্যাপ বাটন সহ)
                (text.contains("deposit") || text.contains("ডিপোজিট") || text.contains("taka") || text.contains("টাকা")) &&
                (text.contains("aseni") || text.contains("ashoni") || text.contains("add hoyni") || text.contains("painai") || text.contains("আসেনি") || text.contains("যোগ হয়নি") || text.contains("দেরি") || text.contains("pending")) -> {
                    SupportMessage(
                        isUser = false,
                        text = "চিন্তার কোনো কারণ নেই $userName ভাই! 🙏\n\nসাধারণত ৫ থেকে ১৫ মিনিটের মধ্যেই এডমিন ডিপোজিট ভেরিফাই করে ব্যালেন্স যোগ করে দেন। যদি ১৫ মিনিটের বেশি হয়ে থাকে, তবে নিচে চাপ দিয়ে সরাসরি এডমিন দেলোয়ার ভাইকে আপনার TrxID পাঠিয়ে জানান—এডমিন সাথে সাথে চেক করে দেবেন!",
                        actionType = InlineActionType.WHATSAPP_CARD
                    )
                }

                // ৫. অ্যাপের বিশ্বস্ততা ও সততা
                text.contains("বিশ্বস্ত") || text.contains("trusted") || text.contains("real naki fake") || text.contains("taka mere dibe") || text.contains("fraud") || text.contains("প্রতারণা") || text.contains("নিরাপদ") || text.contains("fake app") -> {
                    SupportMessage(
                        isUser = false,
                        text = "আলহামদুলিল্লাহ $userName ভাই, AD TOURNAMENT শতভাগ বিশ্বস্ত এবং ফেয়ার-প্লে প্ল্যাটফর্ম! ❤️\n\nআমাদের সম্মানিত এডমিন দেলোয়ার ভাই নিজে প্রতিটি লেনদেন পরিচালনা করেন। এখানে হাজার হাজার প্লেয়ার নিয়মিত খেলছেন এবং নির্ভয়ে টাকা লেনদেন করছেন। কোনো প্রকার প্রতারণার সুযোগ নেই—আপনার প্রতিটি টাকার সম্পূর্ণ গ্যারান্টি আমাদের!",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ১. উইথড্র পেন্ডিং / দেরি হওয়া (হোয়াটসঅ্যাপ বাটন সহ)
                (text.contains("withdraw") || text.contains("উইথড্র") || text.contains("cashout") || text.contains("ক্যাশআউট") || text.contains("টাকা")) &&
                (text.contains("pending") || text.contains("পেন্ডিং") || text.contains("aseni") || text.contains("ashoni") || text.contains("painai") || text.contains("আসেনি") || text.contains("দেরি") || text.contains("koto somoy")) -> {
                    SupportMessage(
                        isUser = false,
                        text = "ধৈর্য ধরার জন্য ধন্যবাদ $userName ভাই! 🙏\n\nসাধারণত উইথড্র রিকোয়েস্ট দেওয়ার ৩০ মিনিট থেকে ১ ঘণ্টার মধ্যে এডমিন দেলোয়ার ভাই নিজে চেক করে টাকা পাঠিয়ে দেন। তবে ১ ঘণ্টার বেশি দেরি হলে নিচে চাপ দিয়ে সরাসরি এডমিন দেলোয়ার ভাইকে আপনার নম্বর ও রিকোয়েস্ট জানিয়ে নক দিন!",
                        actionType = InlineActionType.WHATSAPP_CARD
                    )
                }

                // ২. ভুল TrxID বা ডিপোজিট সমস্যা (হোয়াটসঅ্যাপ বাটন সহ)
                text.contains("vul trx") || text.contains("wrong trx") || text.contains("ভুল trx") ||
                text.contains("ভুল ট্রানজেকশন") || text.contains("reject") || text.contains("রিজেক্ট") ||
                text.contains("vul number") || text.contains("ভুল নম্বরে") -> {
                    SupportMessage(
                        isUser = false,
                        text = "কোনো ভয় নেই $userName ভাই, আপনার টাকা সম্পূর্ণ নিরাপদ! 🛡️\n\nযদি ভুল TrxID বা ভুল নম্বর দিয়ে থাকেন, তবে টাকা পাঠানোর মেসেজ বা বিকাশ/নগদ স্টেটমেন্টের স্ক্রিনশট নিন। এরপর নিচে চাপ দিয়ে সরাসরি এডমিন দেলোয়ার ভাইকে পাঠান—এডমিন ম্যানুয়ালি চেক করে ব্যালেন্স যোগ করে দেবেন।",
                        actionType = InlineActionType.WHATSAPP_CARD
                    )
                }

                // ৩. ম্যাচে জয়েন করার নিয়ম / সমস্যা
                text.contains("kivabe join") || text.contains("join kivabe") || text.contains("match khelbo") ||
                text.contains("ম্যাচে ঢুকব") || text.contains("জয়েন করার নিয়ম") || text.contains("join hocche na") ||
                text.contains("ম্যাচ জয়েন") || text.contains("entry kivabe") -> {
                    SupportMessage(
                        isUser = false,
                        text = "ম্যাচে জয়েন করা একদম সহজ $userName ভাই! 🎮\n\n১. ওয়ালেটে পর্যাপ্ত এন্ট্রি ফি ব্যালেন্স আছে কি না দেখে নিন।\n২. 'Matches' স্ক্রিনে গিয়ে পছন্দের ১v১ ম্যাচের 'Join' বাটনে চাপ দিন।\n৩. ২/২ জন জয়েন হওয়া পর্যন্ত অপেক্ষা করুন। ২ জন হলেই এডমিন চ্যাটে/ম্যাচে রুম কোড দিয়ে দেবেন! কোড দিয়ে লুডু কিংয়ে ঢুকে খেলা শুরু করুন।",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ৪. প্রতিপক্ষ চাল দিচ্ছে না / অফলাইন
                text.contains("chal dicche na") || text.contains("chal dey na") || text.contains("চাল দেয় না") ||
                text.contains("opponent offline") || text.contains("অফলাইন") || text.contains("ber hoye geche") ||
                text.contains("খেলে না") || text.contains("khelteche na") -> {
                    SupportMessage(
                        isUser = false,
                        text = "জরুরি টিপস $userName ভাই: ⚠️\n\nপ্রতিপক্ষ যদি চাল না দেয় বা গেম থেকে বের হয়ে যায়, আপনি ভুলেও গেম কাটবেন না! লুডু কিংয়ের নিয়ম অনুযায়ী অটোমেটিক তার টাইম আউট হয়ে যাবে এবং আপনি ম্যাচটি জিতে যাবেন। জয়ী হওয়ার পর স্পষ্ট উইনিং স্ক্রিনশট নিয়ে রেজাল্ট সাবমিট করে দেবেন!",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ৫. সর্বনিম্ন ডিপোজিট ও উইথড্র লিমিট (৳৫০ ডিপোজিট এবং ৳২০০ উইথড্র)
                text.contains("minimum deposit") || text.contains("minimum withdraw") || text.contains("shorbonimno") ||
                text.contains("সর্বনিম্ন") || text.contains("মিনিমাম") || text.contains("কমপক্ষে কত") ||
                text.contains("minimum limit") || text.contains("সবনিম্ন") -> {
                    SupportMessage(
                        isUser = false,
                        text = "আমাদের প্ল্যাটফর্মের লিমিটগুলো জেনে নিন $userName ভাই: 💳\n\n• 💸 সর্বনিম্ন ডিপোজিট: মাত্র ৳৫০\n• 💵 সর্বনিম্ন উইথড্র: মাত্র ৳২০০ (উইনিং ব্যালেন্স থেকে সরাসরি বিকাশ বা নগদে উত্তোলন করতে পারবেন)\n\nকোনো বাড়তি চার্জ নেই—নিরাপদে ডিপোজিট ও উইথড্র করুন!",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ১. বোনাস ব্যালেন্স ব্যবহারের নিয়ম (বানান ভুল ও বাংলিশ সহ)
                text.contains("bonus") || text.contains("bonas") || text.contains("bonos") ||
                text.contains("বোনাস") || text.contains("10%") || text.contains("ব্যবহার") ||
                text.contains("kaje lage") || text.contains("use korbo") -> {
                    SupportMessage(
                        isUser = false,
                        text = "বোনাস ব্যালেন্স ব্যবহারের সহজ নিয়ম $userName ভাই: 🎁\n\nআপনি যেকোনো ১v১ পেইড ম্যাচে জয়েন করার সময় এন্ট্রি ফির সর্বোচ্চ ১০% স্বয়ংক্রিয়ভাবে আপনার বোনাস ওয়ালেট থেকে কেটে নেওয়া হবে! বাকি ৯০% মূল ব্যালেন্স থেকে কাটা হবে। ফলে বোনাস টাকা দিয়ে আপনি সরাসরি ম্যাচ ফি কমাতে পারবেন!",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ২. রেফার বোনাস আনলক হওয়ার নিয়ম (৳৪০ আনলক)
                (text.contains("refer") || text.contains("reffer") || text.contains("refar") || text.contains("রেফার")) &&
                (text.contains("unlock") || text.contains("আনলক") || text.contains("painai") || text.contains("40") || text.contains("৪০") || text.contains("kobe") || text.contains("আসেনি")) -> {
                    SupportMessage(
                        isUser = false,
                        text = "রেফার বোনাসের নিয়মটি জেনে নিন $userName ভাই: 🤝\n\nআপনার রেফার কোড দিয়ে কোনো বন্ধু একাউন্ট খুললে সাথে সাথে আপনার একাউন্টে ৳৪০ লকড বোনাস জমা হয়। এরপর আপনার বন্ধু যখনই প্রথমবার যেকোনো ডিপোজিট সম্পন্ন করবে, সাথে সাথে এই ৳৪০ সম্পূর্ণ আনলক হয়ে আপনার মূল বোনাস ব্যালেন্সে যুক্ত হয়ে যাবে!",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ৩. ক্যারম ১v১ ম্যাচের নিয়ম
                text.contains("carrom") || text.contains("caram") || text.contains("ceram") ||
                text.contains("ক্যারম") || text.contains("কেরাম") || text.contains("carrom board") -> {
                    SupportMessage(
                        isUser = false,
                        text = "ক্যারম ১v১ ম্যাচ খেলার নিয়ম $userName ভাই: 🎯\n\n১. ক্যারম ম্যাচে ২ জন প্লেয়ার জয়েন করার পর এডমিন রুমে কোড দিয়ে দেবেন।\n২. Carrom Disc Pool অ্যাপে গিয়ে কোড দিয়ে জয়েন করুন।\n৩. কুইন কাভার এবং নিয়মের মধ্যে খেলা শেষ করে স্পষ্ট উইনিং স্ক্রিনশট নিন।\n৪. আমাদের অ্যাপে এসে রেজাল্ট সাবমিট করুন—এডমিন দ্রুত প্রাইজ মানি যোগ করে দেবেন!",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ৪. সাপোর্ট ও হেল্পলাইনের সময়সীমা (২৪/৭ সেবা)
                text.contains("support time") || text.contains("helpline") || text.contains("somoy") ||
                text.contains("কখন খোলা") || text.contains("সময়") || text.contains("খোলা থাকে") ||
                text.contains("khola") || text.contains("active") -> {
                    SupportMessage(
                        isUser = false,
                        text = "আমাদের সার্ভিস সার্বক্ষণিক চালু থাকে $userName ভাই! 🕒\n\n• 🤖 এআই লাইভ সাপোর্ট: ২৪ ঘণ্টা যেকোনো সময় আপনার সহায়তায় প্রস্তুত।\n• 👤 এডমিন দেলোয়ার ভাই: সকাল থেকে গভীর রাত পর্যন্ত যেকোনো জটিল সমস্যা বা আর্থিক ভেরিফিকেশনে হোয়াটসঅ্যাপে নিয়মিত অ্যাক্টিভ থাকেন।\n\nনির্ভয়ে খেলুন, যেকোনো প্রয়োজনে আমরা আপনার পাশেই আছি!",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ১. স্ক্রিনশট ও রেজাল্ট জমা (সব ধরনের ভুল বানান ও বাংলিশ: scrinsort, ss, chobi, joma)
                text.contains("scrin") || text.contains("screen") || text.contains("shot") || text.contains("sort") ||
                text.contains("ss") || text.contains("ছবি") || text.contains("chobi") || text.contains("স্ক্রিনশট") ||
                text.contains("জমা") || text.contains("joma") || text.contains("উইনিং") || text.contains("winner") -> {
                    SupportMessage(
                        isUser = false,
                        text = "অভিনন্দন $userName ভাই! 🏆\n\nম্যাচ জেতার পর স্ক্রিনশট জমা দেওয়া খুব সহজ:\n১. খেলা শেষ হওয়ামাত্রই লুডু কিংয়ের স্পষ্ট উইনিং স্ক্রিনশট নিন।\n২. আমাদের অ্যাপে সেই ম্যাচের 'Submit Result'-এ যান।\n৩. 'I Won' সিলেক্ট করে স্ক্রিনশটটি আপলোড করে সাবমিট করুন।\n\nএডমিন চেক করে সাথে সাথে আপনার উইনিং ব্যালেন্সে টাকা যোগ করে দেবেন!",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ২. কেমন আছেন / কুশল বিনিময় (kemon acho, lemon acho, how are you, ki khobor)
                text.contains("kemon") || text.contains("kemn") || text.contains("lemon") || text.contains("কেমন") ||
                text.contains("ki khobor") || text.contains("how are you") || text.contains("khobor ki") -> {
                    SupportMessage(
                        isUser = false,
                        text = "আলহামদুলিল্লাহ $userName ভাই, আমি সবসময় দারুণ আছি আপনাদের সেবায়! ❤️\n\nআপনার দিনকাল কেমন যাচ্ছে? আজ কি লুডু বা ক্যারমে কোনো ম্যাচ খেলেছেন?",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ৩. ম্যাচে হার / ভাগ্য খারাপ / মন খারাপ (আন্তরিক সান্ত্বনা ও মোটিভেশন)
                text.contains("kharap") || text.contains("খারাপ") || text.contains("harsi") || text.contains("harci") ||
                text.contains("হেরে") || text.contains("হারছি") || text.contains("loss") || text.contains("los") ||
                text.contains("parini") || text.contains("pari nai") || text.contains("mon kharap") || text.contains("বাশ") ||
                text.contains("dhara") -> {
                    SupportMessage(
                        isUser = false,
                        text = "একদম মন খারাপ করবেন না $userName ভাই! ❤️\n\nখেলাধুলায় হার-জিত থাকবেই, এটা খেলারই অংশ। মন শক্ত রাখুন এবং একটু রেস্ট নিয়ে শান্ত মাথায় চালগুলো ভাবুন। আমাদের '🎮 জেতার প্রো-টিপস' দেখে পরবর্তী ম্যাচে নামুন—ইনশাআল্লাহ বড় জয় আপনারই হবে! 🏆 আমি আছি আপনার সাথে!",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ৪. ম্যাচে জয় / ভাগ্য ভালো (উৎসাহ ও অভিনন্দন)
                text.contains("jitsi") || text.contains("jitechi") || text.contains("জিতছি") || text.contains("জিতেছি") ||
                text.contains("win") || text.contains("labh") || text.contains("valo gelo") || text.contains("bhalo gelo") ||
                text.contains("onek jitsi") -> {
                    SupportMessage(
                        isUser = false,
                        text = "মাশাল্লাহ $userName ভাই! 🔥 শুনে মনটা ভরে গেল!\n\nআপনার দুর্দান্ত পারফরম্যান্সের জন্য অনেক অনেক অভিনন্দন! এভাবেই জয়ের ধারা ধরে রাখুন। আর হ্যাঁ, ম্যাচ শেষে স্পষ্ট স্ক্রিনশট দিতে ভুলবেন না যেন! 🏆",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ৫. ভালো আছি / আলহামদুলিল্লাহ
                text == "valo" || text == "bhalo" || text == "valo achi" || text == "bhalo achi" ||
                text.contains("alhamdulillah") || text == "ভালো" || text == "ভালো আছি" || text == "fine" -> {
                    SupportMessage(
                        isUser = false,
                        text = "আলহামদুলিল্লাহ ভাই, শুনে খুব ভালো লাগল! 😊 টুর্নামেন্টে জয় আপনার সঙ্গী হোক। কোনো প্রশ্ন বা সমস্যা থাকলে আমাকে নির্দ্বিধায় জানান!",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }

                // ডিফল্ট হেল্পফুল রেসপন্স
                else -> {
                    SupportMessage(
                        isUser = false,
                        text = "$userName ভাই, আপনার কথাটি বুঝতে আরেকটু সাহায্য প্রয়োজন। আপনি কি ডিপোজিট, রুম কোড, বোনাস বা ওয়ালেট সম্পর্কে জানতে চাচ্ছেন? নিচের অপশনগুলো থেকেও বেছে নিতে পারেন!",
                        actionType = InlineActionType.MAIN_MENU
                    )
                }
            }

            isBotTyping = false
            messages.add(botResponse)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF111827))
                                .border(
                                    1.5.dp,
                                    Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFB45309))),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "এআই লাইভ সাপোর্ট",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val pulseScale by infiniteTransition.animateFloat(
                                    initialValue = 0.8f,
                                    targetValue = 1.25f,
                                    animationSpec = infiniteRepeatable(
                                        tween(1200, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "scale"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .graphicsLayer {
                                            scaleX = if (isBotTyping) 1f else pulseScale
                                            scaleY = if (isBotTyping) 1f else pulseScale
                                        }
                                        .clip(CircleShape)
                                        .background(if (isBotTyping) Color(0xFFF59E0B) else Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isBotTyping) "টাইপ করছেন..." else "ONLINE • ২৪/৭ স্মার্ট সহকারী",
                                    color = if (isBotTyping) Color(0xFFF59E0B) else Color(0xFF34D399),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF131D33))
                            .border(1.dp, Color(0xFF223152), RoundedCornerShape(12.dp))
                            .clickable { onNavigateBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF131D33))
                            .border(1.dp, Color(0xFF223152), RoundedCornerShape(12.dp))
                            .clickable {
                                messages.clear()
                                isBotTyping = true
                                coroutineScope.launch {
                                    delay(1000L)
                                    val newGreeting = randomGreetings.random()
                                    messages.add(
                                        SupportMessage(
                                            isUser = false,
                                            text = newGreeting,
                                            actionType = InlineActionType.MAIN_MENU
                                        )
                                    )
                                    isBotTyping = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Clear Chat",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF070B14))
            )
        },
        bottomBar = {
            Surface(
                color = Color(0xFF0A0F1D),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFF1E293B)))
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("আপনার প্রশ্নটি লিখুন...", color = Color(0xFF64748B), fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = Color(0xFF263554),
                            focusedContainerColor = Color(0xFF131D33),
                            unfocusedContainerColor = Color(0xFF131D33)
                        ),
                        shape = RoundedCornerShape(26.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                )
                            )
                            .clickable {
                                if (inputText.isNotBlank()) {
                                    val textToSend = inputText
                                    inputText = ""
                                    messages.add(SupportMessage(isUser = true, text = textToSend))
                                    processLocalBotReply(textToSend)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF090D16),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages) { msg ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 310.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (msg.isUser) 18.dp else 4.dp,
                                    bottomEnd = if (msg.isUser) 4.dp else 18.dp
                                )
                            )
                            .then(
                                if (msg.isUser) {
                                    Modifier.background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                        )
                                    )
                                } else {
                                    Modifier
                                        .background(Color(0xFF0F172A))
                                        .border(1.2.dp, Color(0xFF263554), RoundedCornerShape(18.dp))
                                }
                            )
                            .padding(14.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = if (msg.isUser) Color(0xFF0A0F1D) else Color(0xFFF8FAFC),
                            fontSize = 14.sp,
                            fontWeight = if (msg.isUser) FontWeight.Bold else FontWeight.Normal,
                            lineHeight = 21.sp
                        )
                    }

                    when (msg.actionType) {
                        InlineActionType.WHATSAPP_CARD -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF16A34A), Color(0xFF15803D))
                                        )
                                    )
                                    .clickable { openWhatsappChat() }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "💬 এডমিন দেলোয়ার ভাইয়ের সাথে হোয়াটসঅ্যাপে চ্যাট করুন",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        InlineActionType.WALLET_CARD -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(0.95f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D33)),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("💰 ওয়ালেট স্ট্যাটাস", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("মূল ব্যালেন্স:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                        Text("৳$availableBalance", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("উইনিং ব্যালেন্স:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                        Text("৳$winningBalance", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("বোনাস ব্যালেন্স:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                        Text("৳$bonusBalance", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        InlineActionType.REFERRAL_CONFIRMATION -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        messages.add(SupportMessage(isUser = true, text = "হ্যাঁ, রেফার নিয়ম জানতে চাই"))
                                        processLocalBotReply("he")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("🎁 হ্যাঁ, রেফার নিয়ম", fontSize = 12.sp, color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        messages.add(SupportMessage(isUser = true, text = "না, পরে জানব"))
                                        processLocalBotReply("na")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("❌ না, পরে", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }

                        InlineActionType.MAIN_MENU -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(0.95f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ActionButton("💰 ব্যালেন্স চেক", Modifier.weight(1f)) {
                                        messages.add(SupportMessage(isUser = true, text = "আমার ব্যালেন্স চেক"))
                                        processLocalBotReply("balance")
                                    }
                                    ActionButton("🎮 জেতার প্রো-টিপস", Modifier.weight(1f)) {
                                        messages.add(SupportMessage(isUser = true, text = "জেতার প্রো-টিপস"))
                                        processLocalBotReply("tips")
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ActionButton("💸 টাকা যোগ নিয়ম", Modifier.weight(1f)) {
                                        messages.add(SupportMessage(isUser = true, text = "টাকা যোগ করার নিয়ম"))
                                        processLocalBotReply("deposit")
                                    }
                                    ActionButton("🔑 রুম কোড গাইড", Modifier.weight(1f)) {
                                        messages.add(SupportMessage(isUser = true, text = "রুম কোড পাচ্ছি না"))
                                        processLocalBotReply("room code")
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ActionButton("👤 এডমিন দেলোয়ার ভাই", Modifier.weight(1f)) {
                                        messages.add(SupportMessage(isUser = true, text = "এডমিন দেলোয়ার ভাইয়ের সাথে কথা বলব"))
                                        processLocalBotReply("admin")
                                    }
                                    ActionButton("🛡️ চিটিং রিপোর্ট", Modifier.weight(1f)) {
                                        messages.add(SupportMessage(isUser = true, text = "চিটিং রিপোর্ট"))
                                        processLocalBotReply("cheat")
                                    }
                                }
                            }
                        }

                        else -> {}
                    }

                    if (!msg.isUser) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.ThumbUp,
                                contentDescription = "Helpful",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp).clickable { }
                            )
                            Icon(
                                Icons.Default.ThumbDown,
                                contentDescription = "Not Helpful",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp).clickable { }
                            )
                        }
                    }
                }
            }

            if (isBotTyping) {
                item {
                    TypingBubble()
                }
            }
        }
    }
}

@Composable
fun ActionButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF131D33))
            .border(1.dp, Color(0xFF263554), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = when {
                text.contains("💰") || text.contains("🎮") -> Color(0xFFFBBF24)
                text.contains("💸") || text.contains("🔑") -> Color(0xFF38BDF8)
                else -> Color(0xFFF1F5F9)
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TypingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), repeatMode = RepeatMode.Reverse),
        label = "dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), repeatMode = RepeatMode.Reverse),
        label = "dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), repeatMode = RepeatMode.Reverse),
        label = "dot3"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E293B))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFF59E0B).copy(alpha = alpha1)))
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFF59E0B).copy(alpha = alpha2)))
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFF59E0B).copy(alpha = alpha3)))
        }
    }
}
