package com.example.soulmole.activity

import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.soulmole.R
import com.example.soulmole.db.DatabaseHelper

class RankingActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var listViewRanking: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)

        dbHelper = DatabaseHelper(this)
        listViewRanking = findViewById(R.id.listViewRanking)

        // Lấy top 5 người chơi từ cơ sở dữ liệu
        val leaderboard = dbHelper.getTop5PlayersWithScores()

        // Sử dụng RankingAdapter để hiển thị danh sách top 5
        val adapter = RankingAdapter(this, leaderboard)
        listViewRanking.adapter = adapter
    }
}
