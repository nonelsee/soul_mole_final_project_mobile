package com.example.soulmole.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.soulmole.R
import com.example.soulmole.db.DatabaseHelper
import com.example.soulmole.db.GameSessionDAO
import com.example.soulmole.model.GameSession
import com.example.soulmole.model.Player
import com.example.soulmole.view.GameView

class GameActivity : AppCompatActivity() {

    private var isDigMode = false
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var gameView: GameView
    lateinit var player: Player

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        dbHelper = DatabaseHelper(this)

        gameView = findViewById<GameView>(R.id.gameView)
        showNameInputDialog()

        val buttonDig = findViewById<Button>(R.id.buttonDig)
        val buttonUp = findViewById<Button>(R.id.buttonUp)
        val buttonDown = findViewById<Button>(R.id.buttonDown)
        val buttonLeft = findViewById<Button>(R.id.buttonLeft)
        val buttonRight = findViewById<Button>(R.id.buttonRight)

        // Bật chế độ dig khi nhấn nút dig
        buttonDig.setOnClickListener {
            isDigMode = true
        }

        buttonUp.setOnClickListener {
            if (isDigMode) {
                gameView.digBlock(0, -1)  // Đào block ở trên
            } else {
                gameView.movePlayer(0f, -1f)  // Di chuyển lên
            }
            isDigMode = false
        }

        buttonDown.setOnClickListener {
            if (isDigMode) {
                gameView.digBlock(0, 1)  // Đào block ở dưới
            } else {
                gameView.movePlayer(0f, 1f)  // Di chuyển xuống
            }
            isDigMode = false
        }

        buttonLeft.setOnClickListener {
            if (isDigMode) {
                gameView.digBlock(-1, 0)  // Đào block bên trái
            } else {
                gameView.movePlayer(-1f, 0f)  // Di chuyển sang trái
            }
            isDigMode = false
        }

        buttonRight.setOnClickListener {
            if (isDigMode) {
                gameView.digBlock(1, 0)  // Đào block bên phải
            } else {
                gameView.movePlayer(1f, 0f)  // Di chuyển sang phải
            }
            isDigMode = false
        }
    }

    private fun showNameInputDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_name_input, null)
        val editTextName = dialogView.findViewById<EditText>(R.id.edit_text_name)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Nhập tên người chơi")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Xác nhận", null)
            .setNegativeButton("Hủy") { _, _ -> finish() }
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val username = editTextName.text.toString().trim()

                if (username.isNotEmpty()) {
                    val existingPlayer = dbHelper.getPlayerByUsername(username)
                    if (existingPlayer == null) {
                        // Tạo Player mới với username và các giá trị mặc định
                        val newPlayer = Player(username = username)

                        // Chèn Player vào cơ sở dữ liệu và lấy playerId tự động tăng
                        val playerId = dbHelper.insertPlayer(newPlayer)
                        if (playerId != -1L) {
                            player = newPlayer.copy(playerId = playerId.toInt())

                            // Khởi tạo gameView với player sau khi tên đã nhập
                            gameView.initializePlayer(player)
                            gameView.playerDAO.onPlayerHealthDepleted = { showGameOverDialog() }

                            dialog.dismiss()
                        } else {
                            Toast.makeText(this, "Lỗi khi lưu người chơi, vui lòng thử lại", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Tên người dùng đã tồn tại, vui lòng nhập lại",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    private fun showGameOverDialog() {
        val score = player.score
        val depth = player.depth

        val gameSession = GameSession(
            playerId = player.playerId,
            finalScore = score,
            distanceDug = depth
        )

        val gameSessionDAO = GameSessionDAO(dbHelper)
        gameSessionDAO.insertGameSession(gameSession)
        // Hiển thị thông báo với điểm số và độ sâu
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage("Score: $score\nDepth: $depth")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    finish()  // Kết thúc GameActivity và quay lại màn hình chính hoặc làm điều gì đó khác
                }
                .setCancelable(false)
                .show()
        }
    }
}

