package com.example.ui.autocarrom

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.autoludo.AdExitDialog
import com.example.ui.autoludo.AdGameSfx
import com.example.ui.autoludo.AdResultDialog
import com.example.ui.autoludo.rememberServerConnected
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

// Auto Carrom ONLINE game. The shooter's phone runs the physics and saves the shot (AutoCarromPlay.submitShot);
// the other phone replays it from gameState.lastShot, then shows the saved result. Player 2 sees the board turned.

@Composable
fun AutoCarromGameScreen(matchId: String, onBackClick: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { CarromSfx.prepare(context); AdGameSfx.prepare(context) }
    val me = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val match by AutoCarromManager.observeMatch(matchId).collectAsState(initial = null)
    val connected by rememberServerConnected()
    val scope = rememberCoroutineScope()
    val S = CarromRules.S
    var shown by remember { mutableStateOf<CarromGameState?>(null) }
    var pending by remember { mutableStateOf<CarromGameState?>(null) }
    var mode by remember { mutableIntStateOf(0) } // 0 aim/idle, 1 running, 2 settling, 3 waiting for the saved result
    var replaying by remember { mutableStateOf(false) }
    var sim by remember { mutableStateOf<CarromSim?>(null) }
    var shotInput by remember { mutableStateOf("") }
    var strikerX by remember { mutableDoubleStateOf(S / 2) }
    var aim by remember { mutableStateOf<DoubleArray?>(null) }
    var powderMode by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var toast by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var toastAt by remember { mutableLongStateOf(0L) }
    var passTriedFor by remember { mutableIntStateOf(-1) }
    var waitSince by remember { mutableLongStateOf(0L) }
    val live by CarromLive.observe(matchId).collectAsState(initial = emptyMap())
    var showExit by remember { mutableStateOf(false) }
    val powder = remember { PowderLayer() }
    var showSplash by remember { mutableStateOf(true) }
    var oppX by remember { mutableDoubleStateOf(S / 2) }
    var oppAimShown by remember { mutableStateOf<DoubleArray?>(null) }
    var submitting by remember { mutableStateOf(false) }

    val m = match
    val p1 = m?.player1Uid ?: ""; val p2 = m?.player2Uid ?: ""
    val isP1 = me == p1
    val opp = if (isP1) p2 else p1
    val st = shown
    val myTurn = st != null && st.currentTurnUid == me && m?.status == "IN_GAME"
    fun baseY(uid: String) = if (uid == p1) S - CarromRules.BASE else CarromRules.BASE
    fun show(t: Pair<String, Boolean>?) { if (t != null) { toast = t; toastAt = CarromClock.now() } }
    val boardBodies = remember(st?.board) { CarromRules.decode(st?.board ?: "").map { CBody(it.c, it.x, it.y, CarromRules.RC, 1.0) } }
    fun blocked(): Boolean { val by = baseY(me); return boardBodies.any { val dx = it.x - strikerX; val dy = it.y - by; sqrt(dx * dx + dy * dy) < CarromRules.RC + CarromRules.RS + 1 } }
    fun sounds(s: CarromSim) {
        s.onEvent = { kind, v -> when (kind) { 0 -> CarromSfx.play(CarromSfx.S.HIT, v.toFloat(), 28); 1 -> CarromSfx.play(CarromSfx.S.WALL, 0.6f, 45); else -> CarromSfx.play(CarromSfx.S.POCKET) } }
    }
    fun adopt(ns: CarromGameState) {
        val newShot = shown?.shotNo != ns.shotNo
        shown = ns; pending = null; sim = null; mode = 0; replaying = false
        if (newShot) { strikerX = S / 2; aim = null; powderMode = false; oppX = S / 2; oppAimShown = null; show(carromLogText(ns.lastActionLog)) }
    }

    LaunchedEffect(st?.powder) { if (powder.sync(st?.powder ?: "")) CarromSfx.play(CarromSfx.S.SHH) }

    // server clock + "I am online" every 2 s (Firebase writes 0 by itself if my connection drops)
    LaunchedEffect(matchId) {
        CarromClock.start()
        while (true) { CarromLive.beat(matchId, me); kotlinx.coroutines.delay(CarromLive.BEAT_MS) }
    }
    DisposableEffect(matchId) { onDispose { CarromLive.leave(matchId, me) } }
    val oppInfo = live[opp]
    val oppSeen = oppInfo?.seen
    val oppOnline = oppSeen == null || (oppSeen > 0L && now - oppSeen < CarromLive.ONLINE_MS)
    val oppOnlineNow = rememberUpdatedState(oppOnline)
    // when a net comes back after the 20 s are over, that player still gets 5 s to shoot
    var oppBackAt by remember { mutableLongStateOf(0L) }
    var myBackAt by remember { mutableLongStateOf(0L) }
    var oppWasOnline by remember { mutableStateOf(true) }
    var meWasConnected by remember { mutableStateOf(true) }
    LaunchedEffect(oppOnline) { if (oppOnline && !oppWasOnline) oppBackAt = CarromClock.now(); oppWasOnline = oppOnline }
    LaunchedEffect(connected) { if (connected && !meWasConnected) myBackAt = CarromClock.now(); meWasConnected = connected }
    // my striker and aim go to the other phone about 10 times a second (only when they change)
    LaunchedEffect(matchId) {
        var last = ""
        while (true) {
            kotlinx.coroutines.delay(100)
            val cs = shown
            if (cs != null && cs.currentTurnUid == me && mode == 0 && match?.status == "IN_GAME") {
                val a = aim
                val key = "${cs.shotNo}|${Math.round(strikerX)}|" + (a?.let { "${Math.round(it[0] * 100)},${Math.round(it[1] * 100)},${Math.round(it[2] * 100)}" } ?: "-")
                if (key != last) { last = key; CarromLive.sendAim(matchId, me, cs.shotNo, strikerX, a) }
            }
        }
    }
    // the other player's striker and aim for this shot (null = nothing sent yet)
    val oppLive = if (st != null && st.currentTurnUid == opp) oppInfo?.aim?.let { CarromLive.decode(it, st.shotNo) } else null
    val oppLiveNow = rememberUpdatedState(oppLive)

    // new state from the server
    LaunchedEffect(m?.gameState, m?.status) {
        val mm = m ?: return@LaunchedEffect
        val ns = mm.gameState
        if (mm.status != "IN_GAME" && mm.status != "COMPLETED") return@LaunchedEffect
        val cur = shown
        val parts = ns.lastShot.split("|")
        when {
            cur == null -> shown = ns
            mode == 1 || mode == 2 -> pending = ns
            mode == 3 -> if (ns.shotNo > cur.shotNo) adopt(ns)
            ns.shotNo == cur.shotNo + 1 && parts.size == 7 && parts[0] == cur.shotNo.toString() && parts[1] != me -> {
                val s = CarromSim(cur.board, cur.powder, (parts[2].toDoubleOrNull() ?: 0.0) / 10, (parts[3].toDoubleOrNull() ?: 0.0) / 10)
                sounds(s)
                s.shoot(parts[4].toDoubleOrNull() ?: 0.0, parts[5].toDoubleOrNull() ?: -1.0, parts[6].toDoubleOrNull() ?: 0.5)
                CarromSfx.play(CarromSfx.S.FLICK, 0.7f)
                pending = ns; sim = s; replaying = true; mode = 1
            }
            else -> adopt(ns)
        }
    }

    var submitFailed by remember { mutableStateOf(false) }
    // Own shot: the result is worked out at once and saved right away, while the shot is still being drawn,
    // so the other phone starts its replay almost at the same moment.
    fun submitNow(result: CarromSim, shotNo: Int) {
        submitFailed = false; submitting = true
        scope.launch {
            val r = AutoCarromPlay.submitShot(matchId, me, shotNo, shotInput, result.finalBoard(), result.pocketed.toString(), result.strikerIn)
            submitting = false
            if (r.isFailure) { submitFailed = true; if (mode == 3) match?.gameState?.let { adopt(it) } }
        }
    }
    fun ownShotDrawn() {
        val p = pending; val cur = shown
        when {
            p != null && cur != null && p.shotNo > cur.shotNo -> adopt(p)
            submitFailed -> match?.gameState?.let { adopt(it) } ?: run { mode = 0; sim = null }
            else -> { mode = 3; waitSince = CarromClock.now() }
        }
    }

    LaunchedEffect(Unit) {
        var last = 0L; var acc = 0.0; var settleAt = 0L
        while (true) {
            withFrameNanos { t ->
                val dt = if (last == 0L) 0.0 else ((t - last) / 1e9).coerceAtMost(0.05); last = t
                now = CarromClock.now()
                val s = sim
                if (mode == 1 && s != null) {
                    acc += dt; var n = 0
                    while (acc >= CarromSim.DT && n < 40) { s.step(); acc -= CarromSim.DT; n++ }
                    for (b in s.all) if (!b.inPocket && (abs(b.vx) > 5 || abs(b.vy) > 5)) powder.wipe(b.x, b.y, b.r * 0.92, true)
                    if (!s.moving()) { mode = 2; settleAt = now + 400 }
                } else if (mode == 2 && now >= settleAt) {
                    if (replaying) { val p = pending; if (p != null) adopt(p) else { mode = 0; replaying = false; sim = null } } else ownShotDrawn()
                }
                sim?.all?.forEach { if (it.inPocket && it.fall > 0f) it.fall -= 0.06f }
                val ol = oppLiveNow.value; val f = (dt * 12).coerceAtMost(1.0)
                oppX += ((ol?.first ?: S / 2) - oppX) * f
                val ta = ol?.second; val pa = oppAimShown
                oppAimShown = if (ta == null) null else if (pa == null) ta else {
                    val nx = pa[0] + (ta[0] - pa[0]) * f; val ny = pa[1] + (ta[1] - pa[1]) * f; val nl = sqrt(nx * nx + ny * ny)
                    if (nl < 1e-6) ta else doubleArrayOf(nx / nl, ny / nl, pa[2] + (ta[2] - pa[2]) * f)
                }
                val cs = shown
                if (cs != null && mode == 0 && match?.status == "IN_GAME" && passTriedFor != cs.shotNo) {
                    val el = now - cs.actionStartedAt
                    val pass = if (cs.currentTurnUid == me) connected && el >= CarromRules.TURN_MS && now - myBackAt >= CarromLive.BACK_GRACE_MS
                    else el >= CarromRules.TURN_MS + CarromLive.OFFLINE_GRACE_MS ||
                        (oppOnlineNow.value && el >= CarromRules.TURN_MS + 3000L && now - oppBackAt >= CarromLive.BACK_GRACE_MS)
                    if (pass) {
                        passTriedFor = cs.shotNo
                        scope.launch { AutoCarromPlay.passTurnTimeout(matchId, me, cs.shotNo) }
                    }
                }
                // waited 8 s for my own shot to be saved and nothing came: show the latest saved board
                if (mode == 3 && !submitting && now - waitSince > 8000) match?.gameState?.let { adopt(it) }
                tick++
            }
        }
    }

    LaunchedEffect(m?.status, m?.winnerUid, m?.prizeClaimed) {
        if (m != null && m.status == "COMPLETED" && m.winnerUid == me && !m.prizeClaimed) AutoCarromMoney.claimWinPrize(matchId, me)
    }

    BackHandler { if (m?.status == "IN_GAME") showExit = true else onBackClick() }

    val myName = (if (isP1) m?.player1Name else m?.player2Name) ?: ""
    val oppName = (if (isP1) m?.player2Name else m?.player1Name) ?: ""
    val myPhoto = rememberCarromPhoto(me); val oppPhoto = rememberCarromPhoto(opp)
    val timer = if (st != null && mode == 0) (1f - (now - st.actionStartedAt).toFloat() / CarromRules.TURN_MS).coerceIn(0f, 1f) else 1f
    val left = if (m != null && m.gameStartedAt > 0L) (m.gameStartedAt + CarromRules.GAME_MS - now).coerceAtLeast(0L) / 1000 else 600L

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1D2B93), Color(0xFF1A2685), Color(0xFF161F72), Color(0xFF121A5C))))) {
        if (st == null || m == null) {
            Text("খেলা শুরু হচ্ছে…", color = Color(0xFFFFE7A0), fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center))
        } else Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            CarromHeader(m.prizePool, st.suddenDeath, left, connected, onBack = { if (m.status == "IN_GAME") showExit = true else onBackClick() })
            CcPlayerCard(CcPlayerUi(oppName.ifBlank { "প্রতিপক্ষ" }, oppPhoto, false, st.currentTurnUid == opp && m.status == "IN_GAME", timer,
                if (isP1) st.player2Pocketed else st.player1Pocketed, if (isP1) 'k' else 'w', st.queenOwnerUid == opp,
                if (isP1) st.player2Strikes else st.player1Strikes, "প্রতিপক্ষের পালা"), Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            Box(contentAlignment = Alignment.TopCenter) {
            CarromBoardView(
                tick = tick, now = now, bodies = boardBodies, sim = sim,
                strikerX = if (myTurn) strikerX else oppX, strikerY = baseY(st.currentTurnUid),
                oppAim = if (!myTurn && mode == 0) oppAimShown else null,
                oppTag = if (!myTurn && mode == 0 && oppLive != null && m.status == "IN_GAME") "প্রতিপক্ষ নিশানা করছে…" else null,
                showStrikerRing = myTurn && mode == 0, aim = aim, blocked = myTurn && blocked(),
                canControl = myTurn && mode == 0, powderMode = powderMode, inputKey = st.shotNo, powder = powder, flip = !isP1,
                onSlide = { strikerX = it }, onAim = { aim = it },
                onShoot = { a ->
                    val s = CarromSim(st.board, st.powder, strikerX, baseY(me)); sounds(s); s.shoot(a[0], a[1], a[2])
                    shotInput = "${st.shotNo}|$me|${Math.round(strikerX * 10)}|${Math.round(baseY(me) * 10)}|${a[0]}|${a[1]}|${a[2]}"
                    val r = CarromSim(st.board, st.powder, strikerX, baseY(me)); r.shoot(a[0], a[1], a[2]); r.runToEnd()
                    CarromSfx.play(CarromSfx.S.FLICK, (0.4 + 0.6 * a[2]).toFloat()); aim = null; replaying = false; pending = null; sim = s; mode = 1
                    submitNow(r, st.shotNo)
                },
                onPowder = { x, y -> powderMode = false; val n = st.shotNo; scope.launch { AutoCarromPlay.usePowder(matchId, me, n, x, y) } },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            if (!oppOnline && m.status == "IN_GAME") {
                val rest = (st.actionStartedAt + CarromRules.TURN_MS + CarromLive.OFFLINE_GRACE_MS - now) / 1000
                val n = if (st.currentTurnUid == opp && rest in 0L..30L) " " + CarromRules.bn(rest.toString()) else ""
                Text("প্রতিপক্ষের নেট সমস্যা… অপেক্ষা করুন$n", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp).shadow(8.dp, RoundedCornerShape(50))
                        .background(Brush.verticalGradient(listOf(Color(0xFFE8384E), Color(0xFF9A0E26))), RoundedCornerShape(50))
                        .border(1.5.dp, Color(0xB3FFD278), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 5.dp))
            }
            if (mode == 3 && submitting && now - waitSince > 1500) {
                Text("নেট ধীর… শট পাঠানো হচ্ছে", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp).shadow(8.dp, RoundedCornerShape(50))
                        .background(Brush.verticalGradient(listOf(Color(0xFF3E64E8), Color(0xFF1A2C9A))), RoundedCornerShape(50))
                        .border(1.5.dp, Color(0xB3FFD278), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 5.dp))
            }
            }
            CcPlayerCard(CcPlayerUi(myName.ifBlank { "আপনি" }, myPhoto, true, myTurn, timer,
                if (isP1) st.player1Pocketed else st.player2Pocketed, if (isP1) 'w' else 'k', st.queenOwnerUid == me,
                if (isP1) st.player1Strikes else st.player2Strikes, "আপনার পালা"), Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            val myPowder = if (isP1) st.player1Powder else st.player2Powder
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CcPowderJar(myPowder, powderMode, myTurn && mode == 0 && myPowder > 0, "পাউডার") { powderMode = !powderMode }
                val f = ((strikerX - CarromRules.XMIN) / (CarromRules.XMAX - CarromRules.XMIN)).toFloat()
                CcSlider(if (isP1) f else 1f - f, myTurn && mode == 0 && !powderMode, { v ->
                    val vv = if (isP1) v else 1f - v
                    strikerX = CarromRules.XMIN + vv * (CarromRules.XMAX - CarromRules.XMIN); powder.wipe(strikerX, baseY(me), CarromRules.RS * 0.9, true)
                }, Modifier.weight(1f))
            }
            Text(when { powderMode -> "বোর্ডে যেখানে গুঁড়ি দিতে চান সেখানে ছুঁয়ে দিন"; myTurn -> "স্ট্রাইকার সরান, তারপর স্ট্রাইকার ধরে পেছনে টেনে ছাড়ুন"; m.status == "IN_GAME" -> "প্রতিপক্ষ শট নিচ্ছে…"; else -> "" },
                color = Color(0xFFDCE4FF), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
        }
        CarromToast(toast, now - toastAt < 1500, Modifier.align(Alignment.Center))
        if (showSplash) AdCarromSplashScreen(onFinished = { showSplash = false })
        if (m != null && m.status == "COMPLETED" && mode == 0) {
            AdResultDialog(
                isWin = m.winnerUid == me, prizePool = m.prizePool,
                winnerName = if (m.winnerUid == p1) m.player1Name else m.player2Name, opponentName = oppName,
                isOpponentForfeit = m.forfeitedByUid.isNotBlank() && m.forfeitedByUid != me,
                isOpponentDisqualified = m.winnerUid == me && m.gameState.lastActionLog.contains("Disqualified"),
                onDismiss = onBackClick
            )
        }
        if (showExit && m != null) {
            AdExitDialog(opponentName = oppName, entryFee = m.entryFee,
                onConfirm = { showExit = false; AutoCarromMoney.forfeitMatch(matchId, me) { _, _ -> onBackClick() } },
                onDismiss = { showExit = false })
        }
    }
}
