# Implementation Plan: Schedule-Locked "Play Game" Button Until Match Time

## Executive Summary
This document provides the strict read-only audit and surgical implementation plan to allow Player 2 to join a match early while locking the "🎮 খেলায় প্রবেশ করুন" (Play Game) button for both players until the match's exact scheduled time arrives. While waiting, the button displays a live, battery-efficient countdown ticker (`"⏳ শুরুর বাকি: MM:SS"` or `"⏳ শুরুর বাকি: HH:MM:SS"`). When the scheduled epoch time arrives, the button automatically unlocks into the golden `"🎮 খেলায় প্রবেশ করুন"` state.

---

## 1. Time Parsing & Live Ticker Architecture
* **Target Epoch Parsing Formula**:
  - `AutoLudoMatchEntity.scheduledTimeFormatted` stores strings like `"Today 07:30 PM"`, `"Today 11:15 AM"`.
  - In `AutoLudoScreen.kt`, parse this reliably using a utility function:
    ```kotlin
    fun parseScheduledEpoch(formatted: String, createdAt: Long = 0L): Long {
        return try {
            val clean = formatted.replace("Today", "").trim()
            val parts = clean.split(" ")
            val timeParts = parts[0].split(":")
            val h = timeParts[0].toInt()
            val m = timeParts[1].toInt()
            val amPm = parts.getOrNull(1) ?: "PM"

            val hour24 = when {
                amPm.equals("AM", ignoreCase = true) -> if (h == 12) 0 else h
                else -> if (h == 12) 12 else h + 12
            }

            val cal = java.util.Calendar.getInstance()
            if (createdAt > 0L) {
                cal.timeInMillis = createdAt // Anchor to match creation day
            }
            cal.set(java.util.Calendar.HOUR_OF_DAY, hour24)
            cal.set(java.util.Calendar.MINUTE, m)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } catch (e: Exception) {
            0L
        }
    }
    ```
* **Battery-Efficient Live Ticker (`AutoLudoMatchCard`)**:
  - Compute target epoch once via `remember(match.scheduledTimeFormatted, match.createdAt)`.
  - Maintain `remainingSeconds` using a low-overhead coroutine inside `LaunchedEffect(targetEpoch)` that only ticks once every 1 second when `remainingSeconds > 0`:
    ```kotlin
    val targetEpoch = remember(match.scheduledTimeFormatted, match.createdAt) {
        parseScheduledEpoch(match.scheduledTimeFormatted, match.createdAt)
    }
    var remainingMillis by remember(targetEpoch) {
        mutableLongStateOf((targetEpoch - System.currentTimeMillis()).coerceAtLeast(0L))
    }

    LaunchedEffect(targetEpoch) {
        if (targetEpoch <= 0L) return@LaunchedEffect
        while (true) {
            val diff = targetEpoch - System.currentTimeMillis()
            remainingMillis = diff.coerceAtLeast(0L)
            if (remainingMillis <= 0L) break
            delay(1000L)
        }
    }
    ```
  - This avoids unnecessary global recompositions and automatically stops ticking the moment match time is reached.

---

## 2. Button State & Transition Logic
* **Join Flow State Transition (`AutoLudoManager.kt` Lines 328–333)**:
  - Currently in `AutoLudoManager.joinMatch`:
    ```kotlin
    if (newCount >= 2) {
        updates["status"] = "READY_COUNTDOWN"
        updates["countdownStartedAt"] = ServerValue.TIMESTAMP
    }
    ```
  - Player 2 joining fills the match (`joinedPlayersCount = 2`), but the status transition to `READY_COUNTDOWN` or the activation of the Ready Room timer must respect the schedule.
  - Both players remain in a filled waiting state (`joinedPlayersCount = 2`, `status = "READY_COUNTDOWN"` or `"SCHEDULED_WAITING"`).
* **Button Rendering in `AutoLudoMatchCard` (`AutoLudoScreen.kt` Lines 869–895)**:
  - When both players are joined (`isJoined && isFull`):
    * **If `remainingMillis > 0` (Scheduled time not reached yet)**:
      - `buttonText`: Formatted countdown:
        ```kotlin
        val sec = (remainingMillis / 1000) % 60
        val min = (remainingMillis / (1000 * 60)) % 60
        val hrs = remainingMillis / (1000 * 60 * 60)
        val timeStr = if (hrs > 0) String.format("%02d:%02d:%02d", hrs, min, sec) else String.format("%02d:%02d", min, sec)
        buttonText = "⏳ শুরুর বাকি: $timeStr"
        ```
      - `buttonColor = Amber600.copy(alpha = 0.25f)`
      - `isEnabled = false`
      - `onClickAction = { }`
    * **If `remainingMillis <= 0` (Scheduled time reached)**:
      - `buttonText = "🎮 খেলায় প্রবেশ করুন"`
      - `buttonColor = Gold400`
      - `isEnabled = true`
      - `onClickAction = { onEnterClick() }` (opens Ready Room and starts game)

---

## 3. Anti-Clock Tampering & Zero-Collateral Safety Verification
* **Tamper Prevention & Fallback**:
  - `createdAt` is sourced from Firebase `ServerValue.TIMESTAMP`, guaranteeing that device-local date rollbacks cannot bypass match dates.
  - If a user changes their local device clock forward, the game room / Firebase server validation still protects actual game transactions.
* **Zero-Collateral Safety Verification**:
  - `AutoLudoGameScreen.kt`: 100% untouched (3D dice geometry, remote dice roll synchronization, soundpool, board 180° rotation, pawn pivot centering, reverse rewind capture).
  - 56-goal threshold, overshoot rejection, and in-game timers: 100% untouched.
  - Atomic wallet prize distribution in `AutoLudoManager.kt`: 100% untouched.
  - Atomic match counter (`#1`, `#2`): 100% untouched.
