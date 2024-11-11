package com.example.soulmole.model

data class Leaderboard(
    val leaderboardId: Int = 0,
    val playerId: Int,
    val score: Int,
    val ranking: Int
)
