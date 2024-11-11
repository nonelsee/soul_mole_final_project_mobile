package com.example.soulmole.model

data class GameSession(
    val sessionId: Int = 0,
    val playerId: Int,
    val finalScore: Int = 0,
    val distanceDug: Int = 0,
)

