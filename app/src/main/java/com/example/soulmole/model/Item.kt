package com.example.soulmole.model

data class Item(
    val itemId: Int = 0,
    val sessionId: Int,
    val itemType: String, // Ví dụ: "trái tim"
    val positionX: Int,
    val positionY: Int
)
