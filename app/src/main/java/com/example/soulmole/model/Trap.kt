package com.example.soulmole.model

data class Trap(
    val trapId: Int = 0,
    val sessionId: Int,
    val trapType: String, // Ví dụ: "laser", "bẫy đinh"
    val positionX: Int,
    val positionY: Int
)
