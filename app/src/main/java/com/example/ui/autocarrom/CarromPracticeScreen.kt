package com.example.ui.autocarrom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.autoludo.AdGameSfx
import com.example.ui.minigames.WhConfetti
import com.example.ui.minigames.WhResultCard
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sqrt

// Auto Carrom PRACTICE (offline, against the computer) — for testing the board, physics and rules on a phone.
// Uses the same CarromRules.resolve as the online game. You = white (bottom), computer = black (top).

private const val ME = "me"
private const val BOT = "bot"

@Composable
fun CarromPracticeScreen(myName: String, myPhoto: String, onBack: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { CarromSfx.prepare(context); AdGameSfx.prepare(context) }
    val S = CarromRules.S
    fun fresh(now: Long) = CarromGameState(currentTurnUid = ME, board = CarromRules.initialBoard(), actionStartedAt = now, lastMoveAt = now)
    var startAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var st by remember { mutableStateOf(fresh(System.currentTimeMillis())) }
    var over by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf("") }
    var mode by remember { mutableIntStateOf(0) } // 0 aim, 1 running, 2 settling
    var sim by remember { mutableStateOf<CarromSim?>(null) }
    var shotInput by remember { mutableStateOf("") }
    var strikerX by remember { mutableDoubleStateOf(S / 2) }
    var aim by remember { mutableStateOf<DoubleArray?>(null) }
    var powderMode by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var toast by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var toastAt by remember { mutableLongStateOf(0L) }
    val powder = remember { PowderLayer() }
    val boardBodies = remember(st.board) { CarromRules.decode(st.board).map { CBody(it.c, it.x, it.y, CarromRules.RC, 1.0) } }
    fun baseY(uid: String) = if (uid == ME) S - CarromRules.BASE else CarromRules.BASE
    fun show(t: Pair<String, Boolean>) { toast = t; toastAt = System.currentTimeMillis() }
    fun blocked(): Boolean { val by = baseY(st.currentTurnUid); return boardBodies.any { val dx = it.x - strikerX; val dy = it.y - by; sqrt(dx * dx + dy * dy) < CarromRules.RC + CarromRules.RS + 1 } }

    LaunchedEffect(st.powder) { if (powder.sync(st.powder)) CarromSfx.play(CarromSfx.S.SHH) }

    fun usePowderLocal(uid: String, x: Double, y: Double) {
        val left = if (uid == ME) st.player1Powder else st.player2Powder
        if (left <= 0) return
        val patch = "${Math.round(x.coerceIn(30.0, S - 30.0) * 10)},${Math.round(y.coerceIn(30.0, S - 30.0) * 10)},100"
        st = st.copy(powder = if (st.powder.isBlank()) patch else st.powder + ";" + patch,
            player1Powder = if (uid == ME) left - 1 else st.player1Powder, player2Powder = if (uid == ME) st.player2Powder else left - 1)
    }

    fun startShot(dx: Double, dy: Double, p: Double) {
        val s = CarromSim(st.board, st.powder, strikerX, baseY(st.currentTurnUid))
        s.onEvent = { kind, v ->
            when (kind) { 0 -> CarromSfx.play(CarromSfx.S.HIT, v.toFloat(), 28); 1 -> CarromSfx.play(CarromSfx.S.WALL, 0.6f, 45); else -> CarromSfx.play(CarromSfx.S.POCKET) }
        }
        s.shoot(dx, dy, p)
        shotInput = "${st.shotNo}|${st.currentTurnUid}|${Math.round(strikerX * 10)}|${Math.round(baseY(st.currentTurnUid) * 10)}|$dx|$dy|$p"
        CarromSfx.play(CarromSfx.S.FLICK, (0.4 + 0.6 * p).toFloat())
        aim = null; sim = s; mode = 1
    }

    fun endGame(w: String) { over = true; winner = w; AdGameSfx.play(if (w == ME) AdGameSfx.Sfx.WIN else AdGameSfx.Sfx.LOSS) }

    fun finishShot() {
        val s = sim ?: return
        val out = CarromRules.resolve(st, ME, BOT, st.currentTurnUid, s.finalBoard(), s.pocketed.toString(), s.strikerIn,
            shotInput, startAt + CarromRules.GAME_MS, System.currentTimeMillis())
        carromLogText(out.state.lastActionLog)?.let { show(it) }
        st = out.state; sim = null; mode = 0; strikerX = S / 2
        if (out.status == "COMPLETED") endGame(out.winnerUid)
    }

    fun passTimeout() {
        val s1 = st.player1Strikes + 1
        if (s1 >= CarromRules.MAX_STRIKES) { st = st.copy(player1Strikes = s1); endGame(BOT); return }
        show("সময় শেষ! পালা গেল (" + CarromRules.bn("$s1") + "/৩)" to true)
        st = st.copy(player1Strikes = s1, currentTurnUid = BOT, shotNo = st.shotNo + 1, actionStartedAt = System.currentTimeMillis())
        strikerX = S / 2; powderMode = false
    }

    LaunchedEffect(Unit) {
        var last = 0L; var acc = 0.0; var settleAt = 0L
        while (true) {
            withFrameNanos { t ->
                val dt = if (last == 0L) 0.0 else ((t - last) / 1e9).coerceAtMost(0.05); last = t
                now = System.currentTimeMillis()
                val s = sim
                if (mode == 1 && s != null) {
                    acc += dt; var n = 0
                    while (acc >= CarromSim.DT && n < 40) { s.step(); acc -= CarromSim.DT; n++ }
                    for (b in s.all) if (!b.inPocket && (abs(b.vx) > 5 || abs(b.vy) > 5)) powder.wipe(b.x, b.y, b.r * 0.92, true)
                    if (!s.moving()) { mode = 2; settleAt = now + 400 }
                } else if (mode == 2 && now >= settleAt) finishShot()
                sim?.all?.forEach { if (it.inPocket && it.fall > 0f) it.fall -= 0.06f }
                if (!over && mode == 0 && st.currentTurnUid == ME && now - st.actionStartedAt >= CarromRules.TURN_MS) passTimeout()
                tick++
            }
        }
    }

    // computer's turn
    LaunchedEffect(st.shotNo, st.currentTurnUid, over) {
        if (over || st.currentTurnUid != BOT) return@LaunchedEffect
        delay(1200)
        val c = CarromBot.choose(boardBodies, 'k', CarromRules.BASE)
        if (st.player2Powder > 0 && kotlin.random.Random.nextDouble() < 0.3) {
            usePowderLocal(BOT, c[0] + c[1] * 200, CarromRules.BASE + c[2] * 200); show("কম্পিউটার গুঁড়ি দিল ✨" to false); delay(900)
        }
        val from = strikerX
        for (i in 1..36) { strikerX = from + (c[0] - from) * i / 36.0; delay(16) }
        if (blocked()) strikerX += 30
        aim = doubleArrayOf(c[1], c[2], c[3]); delay(700)
        startShot(c[1], c[2], c[3])
    }

    val myTurn = st.currentTurnUid == ME && !over
    val timer = if (mode == 0) (1f - (now - st.actionStartedAt).toFloat() / CarromRules.TURN_MS).coerceIn(0f, 1f) else 1f
    val left = (startAt + CarromRules.GAME_MS - now).coerceAtLeast(0L) / 1000

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1D2B93), Color(0xFF1A2685), Color(0xFF161F72), Color(0xFF121A5C))))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).shadow(6.dp, CircleShape).clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(Color(0xFFFFF3C4), Color(0xFFE0A82A), Color(0xFF8A5A08), Color(0xFFE0A82A), Color(0xFFFFF3C4))))
                    .padding(3.5.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF3A4AC8), Color(0xFF121A61)))).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFFFFE27A), modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(10.dp))
                com.example.ui.components.AdGoldText("অটো ক্যারম", fontSize = 22.sp, modifier = Modifier.weight(1f))
                Text("প্র্যাকটিস", color = Color(0xFF3A1A00), fontSize = 12.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.background(Brush.verticalGradient(listOf(Color(0xFFFFF3B0), Color(0xFFFFC21E))), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp))
            }
            CarromClockPill(if (st.suddenDeath) "সাডেন ডেথ" else CarromRules.bn(String.format("%02d:%02d", left / 60, left % 60)), st.suddenDeath || left < 60)
            CcPlayerCard(CcPlayerUi("কম্পিউটার", "", false, st.currentTurnUid == BOT && !over, timer, st.player2Pocketed, 'k',
                st.queenOwnerUid == BOT, st.player2Strikes, "প্রতিপক্ষের পালা"), Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            CarromBoardView(
                tick = tick, now = now, bodies = boardBodies, sim = sim,
                strikerX = strikerX, strikerY = baseY(st.currentTurnUid), showStrikerRing = mode == 0 && !over, aim = aim, blocked = blocked(),
                canControl = !over && mode == 0 && st.currentTurnUid == ME, powderMode = powderMode, inputKey = st.shotNo, powder = powder,
                onSlide = { strikerX = it }, onAim = { aim = it }, onShoot = { startShot(it[0], it[1], it[2]) },
                onPowder = { x, y -> powderMode = false; usePowderLocal(ME, x, y); show("গুঁড়ি দেওয়া হলো ✨" to false) },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            CcPlayerCard(CcPlayerUi(myName.ifBlank { "আপনি" }, myPhoto, true, myTurn, timer, st.player1Pocketed, 'w',
                st.queenOwnerUid == ME, st.player1Strikes, "আপনার পালা"), Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CcPowderJar(st.player1Powder, powderMode, myTurn && mode == 0 && st.player1Powder > 0, "পাউডার") { powderMode = !powderMode }
                CcSlider(((strikerX - CarromRules.XMIN) / (CarromRules.XMAX - CarromRules.XMIN)).toFloat(), myTurn && mode == 0 && !powderMode,
                    { f -> strikerX = CarromRules.XMIN + f * (CarromRules.XMAX - CarromRules.XMIN); powder.wipe(strikerX, S - CarromRules.BASE, CarromRules.RS * 0.9, true) }, Modifier.weight(1f))
            }
            Text(when { powderMode -> "বোর্ডে যেখানে গুঁড়ি দিতে চান সেখানে ছুঁয়ে দিন"; myTurn -> "স্ট্রাইকার সরান, তারপর স্ট্রাইকার ধরে পেছনে টেনে ছাড়ুন"; over -> ""; else -> "কম্পিউটার শট নিচ্ছে…" },
                color = Color(0xFFDCE4FF), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
        }
        val tt = toast
        if (tt != null && now - toastAt < 1500) {
            Text(tt.first, color = if (tt.second) Color.White else Color(0xFF3A1A00), fontSize = 16.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.Center).shadow(10.dp, RoundedCornerShape(50))
                    .background(Brush.verticalGradient(if (tt.second) listOf(Color(0xFFFF6A7A), Color(0xFFB3122F)) else listOf(Color(0xFFFFF3B0), Color(0xFFFFC21E))), RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 6.dp))
        }
        if (over) {
            if (winner == ME) WhConfetti()
            BoxWithConstraints(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                WhResultCard(u = maxWidth / 100, win = winner == ME, bigWin = false,
                    title = if (winner == ME) "অভিনন্দন!" else "দুঃখিত!",
                    amount = CarromRules.bn("${st.player1Pocketed} - ${st.player2Pocketed}"),
                    sub = if (winner == ME) "আপনি জিতেছেন! চাপ দিয়ে আবার খেলুন" else "কম্পিউটার জিতেছে। চাপ দিয়ে আবার খেলুন",
                    onClose = {
                        val t0 = System.currentTimeMillis()
                        st = fresh(t0); startAt = t0; over = false; winner = ""; sim = null; mode = 0; strikerX = S / 2; aim = null; powderMode = false
                    })
            }
        }
    }
}
