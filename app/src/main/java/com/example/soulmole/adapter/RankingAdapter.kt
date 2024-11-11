package com.example.soulmole.activity

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.BaseAdapter
import com.example.soulmole.R

class RankingAdapter(private val context: Context, private val leaderboard: List<Pair<String, Int>>) : BaseAdapter() {

    override fun getCount(): Int = leaderboard.size + 1 // Thêm 1 cho hàng tiêu đề

    override fun getItem(position: Int): Any = leaderboard.getOrNull(position - 1) ?: "Header" // Trả về "Header" cho hàng đầu tiên

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_ranking, parent, false)

        val rankTextView = view.findViewById<TextView>(R.id.rankTextView)
        val usernameTextView = view.findViewById<TextView>(R.id.usernameTextView)
        val scoreTextView = view.findViewById<TextView>(R.id.scoreTextView)

        if (position == 0) {
            // Thiết lập dòng tiêu đề cho hàng đầu tiên
            rankTextView.text = "RANKING"
            usernameTextView.text = "NAME"
            scoreTextView.text = "SCORE"
        } else {
            // Thiết lập dữ liệu cho các hàng xếp hạng
            val (username, score) = leaderboard[position - 1]
            rankTextView.text = "$position"
            usernameTextView.text = username
            scoreTextView.text = "$score"
        }
        return view
    }
}
