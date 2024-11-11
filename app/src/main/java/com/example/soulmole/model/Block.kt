package com.example.soulmole.model

enum class BlockType {
    DIRT,
    STONE,
    WOOD,
    EMPTY,
}

class Block(var type: BlockType, var hitsRemaining: Int) {

    fun isBroken(): Boolean {
        return hitsRemaining <= 0
    }

    fun dig() {
        hitsRemaining--
    }
}