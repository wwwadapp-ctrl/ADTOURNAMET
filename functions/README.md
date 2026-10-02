# AD Tournament — Firebase Cloud Functions

## Overview
This folder contains the authoritative trusted backend functions for **AD TOURNAMENT**, specifically the server-side `joinMatch` function.

Client devices **MUST NOT** directly mutate:
- `wallets/$uid/availableBalance`
- `wallets/$uid/pendingBalance`
- `transactions` or `userTransactions`
- `matches/$matchId/joinedPlayersCount` or `matches/$matchId/status`

Instead, clients call the `joinMatch` Firebase Callable Cloud Function (or HTTPS endpoint), which executes with the **Firebase Admin SDK** on trusted Google Cloud infrastructure.
