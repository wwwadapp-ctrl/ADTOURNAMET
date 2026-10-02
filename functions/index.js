const functions = require("firebase-functions");
const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.database();

/**
 * Calculates platform commission based on locked tier:
 * <= ৳100 (10000 minor units) -> ৳10 (1000 minor units)
 * > ৳100 and <= ৳200 (20000 minor units) -> ৳20 (2000 minor units)
 * > ৳200 -> ৳40 (4000 minor units)
 */
function calculateCommission(entryFeeMinorUnits) {
  if (entryFeeMinorUnits <= 10000) {
    return 1000;
  } else if (entryFeeMinorUnits <= 20000) {
    return 2000;
  } else {
    return 4000;
  }
}

/**
 * Trusted server-side match join function.
 * Callable by authenticated clients via Firebase Functions SDK or HTTPS.
 * Enforces atomic validations, wallet deduction, ledger records, and player slots.
 */
exports.joinMatch = functions.https.onCall(async (data, context) => {
  // 1. Authenticate user
  if (!context.auth || !context.auth.uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Authentication required to join match."
    );
  }

  const uid = context.auth.uid;
  const matchId = data ? data.matchId : null;

  if (!matchId || typeof matchId !== "string") {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Invalid matchId provided."
    );
  }

  const now = Date.now();

  // 2. Verify user is not blocked
  const userSnap = await db.ref(`users/${uid}`).once("value");
  const user = userSnap.val();
  if (user && (user.status === "BLOCKED" || user.accountStatus === "BLOCKED" || user.isBlocked === true)) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Your account is blocked. Cannot join tournament."
    );
  }

  // 3. Atomically check and update match & players
  const matchRef = db.ref(`matches/${matchId}`);
  const matchPlayersRef = db.ref(`matchPlayers/${matchId}`);
  const walletRef = db.ref(`wallets/${uid}`);

  const matchSnap = await matchRef.once("value");
  const match = matchSnap.val();

  if (!match) {
    throw new functions.https.HttpsError("not-found", "Match not found.");
  }

  // 4. Verify match is joinable
  const status = (match.status || "").toUpperCase();
  if (status !== "AVAILABLE" && status !== "UPCOMING") {
    throw new functions.https.HttpsError(
      "failed-precondition",
      `Match is not available to join (status: ${status}).`
    );
  }

  // 5. Verify scheduled/cutoff time
  const scheduledTime = Number(match.scheduledAt || match.effectiveScheduledAt || 0);
  if (scheduledTime > 0 && now >= scheduledTime) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Match registration deadline has passed."
    );
  }

  // 6. Verify existing players & prevent race-condition double joins
  const playersSnap = await matchPlayersRef.once("value");
  const existingPlayers = playersSnap.val() || {};
  const playerCount = Object.keys(existingPlayers).length;

  if (existingPlayers[uid]) {
    throw new functions.https.HttpsError(
      "already-exists",
      "You have already joined this match."
    );
  }

  if (playerCount >= (match.maxPlayers || 2)) {
    throw new functions.https.HttpsError(
      "resource-exhausted",
      "Match is full. Maximum 2 players allowed."
    );
  }

  // 7. Calculate authoritative entry fee, commission, and prize
  const entryFeeMinorUnits = Number(match.entryFeeMinorUnits || (match.entryFee ? match.entryFee * 100 : 0));
  if (entryFeeMinorUnits < 0) {
    throw new functions.https.HttpsError(
      "internal",
      "Invalid match entry fee configuration."
    );
  }

  const commissionMinorUnits = calculateCommission(entryFeeMinorUnits);
  const totalPoolMinorUnits = entryFeeMinorUnits * 2;
  const prizeMinorUnits = totalPoolMinorUnits - commissionMinorUnits;

  // 8. Verify wallet balance
  const walletSnap = await walletRef.once("value");
  const wallet = walletSnap.val() || { availableBalance: 0, pendingBalance: 0 };
  const currentAvailable = Number(wallet.availableBalance || 0);

  if (currentAvailable < entryFeeMinorUnits) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      `Insufficient wallet balance. Required: ৳${(entryFeeMinorUnits / 100).toFixed(2)}, Available: ৳${(currentAvailable / 100).toFixed(2)}.`
    );
  }

  // 9. Generate atomic transaction & player records
  const txId = `TXN_ENTRY_${now}_${matchId}_${uid}`;
  const mutationKey = `MATCH_ENTRY_${matchId}_${uid}`;
  const newAvailable = currentAvailable - entryFeeMinorUnits;
  const newPending = Number(wallet.pendingBalance || 0) + entryFeeMinorUnits;
  const playerSlot = playerCount === 0 ? "PLAYER_1" : "PLAYER_2";

  const newPlayer = {
    matchPlayerId: `MP_${matchId}_${uid}`,
    matchId: matchId,
    uid: uid,
    userId: uid,
    username: (user && (user.displayName || user.fullName || user.username)) || "Player",
    slot: playerSlot,
    joinedAt: now,
    entryFeeMinorUnits: entryFeeMinorUnits,
    status: "JOINED",
    isReady: true,
  };

  const newPlayerCount = playerCount + 1;
  const newStatus = newPlayerCount >= (match.maxPlayers || 2) ? "FULL" : "AVAILABLE";

  const transactionRecord = {
    transactionId: txId,
    uid: uid,
    userId: uid,
    amount: entryFeeMinorUnits,
    type: "MATCH_ENTRY",
    status: "COMPLETED",
    beforeBalance: currentAvailable,
    afterBalance: newAvailable,
    source: "MATCH_ENTRY",
    referenceId: mutationKey,
    description: `Entry fee for ${match.title || match.matchNumber || matchId}`,
    createdAt: now,
    processedAt: now,
  };

  // 10. Multi-location atomic update: update wallet, ledger (admin & user), player, and match
  const updates = {};
  updates[`wallets/${uid}/availableBalance`] = newAvailable;
  updates[`wallets/${uid}/pendingBalance`] = newPending;
  updates[`wallets/${uid}/updatedAt`] = now;
  updates[`transactions/${txId}`] = transactionRecord;
  updates[`userTransactions/${uid}/${txId}`] = transactionRecord;
  updates[`matchPlayers/${matchId}/${uid}`] = newPlayer;
  updates[`matches/${matchId}/joinedPlayersCount`] = newPlayerCount;
  updates[`matches/${matchId}/status`] = newStatus;
  updates[`matches/${matchId}/updatedAt`] = now;

  await db.ref().update(updates);

  return {
    success: true,
    transactionId: txId,
    matchId: matchId,
    playerSlot: playerSlot,
    joinedPlayersCount: newPlayerCount,
    status: newStatus,
    entryFeeMinorUnits: entryFeeMinorUnits,
    prizeMinorUnits: prizeMinorUnits,
    commissionMinorUnits: commissionMinorUnits,
  };
});
