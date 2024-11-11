package com.example.soulmole.db

import android.content.ContentValues
import com.example.soulmole.model.GameSession

class GameSessionDAO(private val dbHelper: DatabaseHelper) {

    fun insertGameSession(gameSession: GameSession): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_PLAYER_ID, gameSession.playerId)  // Sử dụng COLUMN_PLAYER_ID
            put(DatabaseHelper.COLUMN_FINAL_SCORE, gameSession.finalScore)
            put(DatabaseHelper.COLUMN_DISTANCE_DUG, gameSession.distanceDug)
        }

        val sessionId = db.insert("game_sessions", null, values)
        db.close()
        return sessionId
    }
}
