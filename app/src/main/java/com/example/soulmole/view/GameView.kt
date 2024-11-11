package com.example.soulmole.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.soulmole.activity.GameActivity
import com.example.soulmole.dao.PlayerDAO
import com.example.soulmole.model.Block
import com.example.soulmole.model.BlockType
import com.example.soulmole.model.Player

class GameView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private val paint = Paint()
    var playerX = 0f
    var playerY = 0f
    var pixelSize = 20f
    var currentLevel = 1

    // Số lượng cột cho block và kích thước block
    val numColumns = 5
    var blockSize = 0f
    val blocks = mutableListOf<MutableList<Block>>()

    // Giới hạn di chuyển
    var minX = 0f
    var maxX = 0f
    var minY = 0f
    var maxY = 0f

    var isLevelTransitioning = false  // Cờ đánh dấu quá trình chuyển level

    lateinit var playerDAO: PlayerDAO
    private var isInitialized = false

    private val fallHandler = Handler(Looper.getMainLooper())
    private val fallRunnable = object : Runnable {
        override fun run() {
            makeWoodBlocksFall()
            fallHandler.postDelayed(this, 100) // Lặp lại sau 100ms
        }
    }


    init {
        holder.addCallback(this)
    }

    // Khởi tạo người chơi và chỉ bắt đầu giảm máu sau khi đã vẽ xong màn hình
    fun initializePlayer(player: Player) {
        playerDAO = PlayerDAO(this, player)
        (context as GameActivity).player = player
        isInitialized = true
        if (holder.surface.isValid) {
            playerDAO.startHealthDecrement()
            drawGame(holder) // Vẽ lại game sau khi khởi tạo playerDAO
        }
    }

    private fun calculateMovementBounds() {
        val screenWidth = width
        val screenHeight = height

        minX = 0f
        maxX = screenWidth * 5 / 7f
        minY = 0f
        maxY = screenHeight.toFloat()

        blockSize = maxX / numColumns
        pixelSize = blockSize
    }

    private fun initializeBlocks() {
        val numRows = (maxY / blockSize * 2 / 3).toInt()

        blocks.clear()

        for (row in 0 until numRows) {
            val rowBlocks = mutableListOf<Block>()
            for (col in 0 until numColumns) {
                val randomValue = (0..99).random()
                val type = when {
                    randomValue < 70 -> BlockType.DIRT
                    randomValue < 90 -> BlockType.STONE
                    else -> BlockType.WOOD
                }
                val hitsRequired = if (type == BlockType.DIRT) 1 else 2
                rowBlocks.add(Block(type, hitsRequired))
            }
            blocks.add(rowBlocks)
        }
    }

    private fun initializePlayerPosition() {
        playerX = blockSize * 2
        playerY = maxY - blocks.size * blockSize - pixelSize - 1
    }

    private fun showCurrentLevel() {
        isLevelTransitioning = true  // Bắt đầu quá trình chuyển level
        val canvas = holder.lockCanvas()
        if (canvas != null) {
            // Hiển thị số level với màu đen trên nền trắng
            canvas.drawColor(Color.WHITE)
            paint.color = Color.BLACK
            paint.textSize = 100f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Level $currentLevel", width / 2f, height / 2f, paint)
            holder.unlockCanvasAndPost(canvas)
        }

        // Đợi 2 giây để hiển thị level, sau đó gọi hàm khởi tạo ngay lập tức
        Handler(Looper.getMainLooper()).postDelayed({
            initializeBlocks()         // Khởi tạo các block cho level mới
            initializePlayerPosition() // Đặt lại vị trí người chơi
            isLevelTransitioning = false  // Kết thúc quá trình chuyển level
            drawGame(holder)           // Vẽ lại màn hình với bản đồ và người chơi
        }, 2000) // Đợi 2 giây trước khi khởi tạo bản đồ
    }



    override fun surfaceCreated(holder: SurfaceHolder) {
        calculateMovementBounds()
        isLevelTransitioning = true
        showCurrentLevel()
        initializeBlocks()
        initializePlayerPosition()

        // Chỉ vẽ game khi playerDAO đã được khởi tạo
        if (isInitialized) {
            playerDAO.startHealthDecrement()
            drawGame(holder)
            fallHandler.post(fallRunnable)
        }
    }

    fun drawGame(holder: SurfaceHolder) {
        // Chỉ tiếp tục nếu playerDAO đã được khởi tạo
        if (!isInitialized || isLevelTransitioning) return

        val canvas = holder.lockCanvas()
        if (canvas != null) {
            canvas.drawColor(Color.WHITE)

            drawRestrictedArea(canvas)
            drawBlocks(canvas)
            drawMolePixelArt(canvas)

            holder.unlockCanvasAndPost(canvas)
        }
    }

    fun goToNextLevel() {
        if (isLevelTransitioning) return  // Nếu đang trong quá trình chuyển level, không làm gì cả
        currentLevel++
        playerDAO.player.depth += 5
        playerDAO.player.score += 20
        showCurrentLevel()
    }

    fun isPlayerAtBottomRow(): Boolean {
        val playerRow = ((maxY - playerY - pixelSize) / blockSize).toInt()
        return playerRow == 0
    }

    private fun drawRestrictedArea(canvas: Canvas) {
        val restrictedColor = Color.parseColor("#6A1E55")
        paint.color = restrictedColor
        canvas.drawRect(width * 5 / 7f, 0f, width.toFloat(), height.toFloat(), paint)

        paint.color = Color.BLACK
        canvas.drawLine(width * 5 / 7f, 0f, width * 5 / 7f, height.toFloat(), paint)

        // Vẽ hình tròn chứa số máu
        paint.color = Color.RED
        val centerX = width * 6 / 7f
        val centerY = height / 6f
        val radius = 50f
        canvas.drawCircle(centerX, centerY, radius, paint)

        // Vẽ số máu từ playerDAO.player.health
        paint.color = Color.WHITE
        paint.textSize = 40f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(playerDAO.player.health.toString(), centerX, centerY + 15f, paint)

        // Vẽ ô chữ nhật chứa "DEPTH" và giá trị độ sâu từ playerDAO.player.depth
        paint.color = Color.DKGRAY
        val rectLeft = width * 5.5f / 7f
        val rectTop = height / 3f
        val rectRight = width * 6.5f / 7f
        val rectBottom = rectTop + 100f
        canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, paint)

        paint.color = Color.WHITE
        paint.textSize = 30f
        canvas.drawText("DEPTH", (rectLeft + rectRight) / 2, rectTop + 30f, paint)
        canvas.drawText(playerDAO.player.depth.toString(), (rectLeft + rectRight) / 2, rectTop + 70f, paint)

        // Vẽ ô chữ nhật chứa "Score" và giá trị điểm số từ playerDAO.player.score
        paint.color = Color.DKGRAY
        val scoreRectTop = rectTop + 120f
        val scoreRectBottom = scoreRectTop + 100f
        canvas.drawRect(rectLeft, scoreRectTop, rectRight, scoreRectBottom, paint)

        paint.color = Color.WHITE
        canvas.drawText("SCORE", (rectLeft + rectRight) / 2, scoreRectTop + 30f, paint)
        canvas.drawText(playerDAO.player.score.toString(), (rectLeft + rectRight) / 2, scoreRectTop + 70f, paint)
    }

    private fun drawBlocks(canvas: Canvas) {
        for (row in blocks.indices) {
            for (col in blocks[row].indices) {
                val block = blocks[row][col]
                paint.color = when (block.type) {
                    BlockType.DIRT -> Color.rgb(139, 69, 19)
                    BlockType.STONE -> Color.DKGRAY
                    BlockType.WOOD -> Color.rgb(160, 82, 45)
                    BlockType.EMPTY -> Color.TRANSPARENT
                }

                val x = col * blockSize
                val y = maxY - (row + 1) * blockSize
                canvas.drawRect(x, y, x + blockSize, y + blockSize, paint)
            }
        }
    }

    private fun drawMolePixelArt(canvas: Canvas) {
        val moleBodyColor = Color.rgb(102, 51, 0)
        val moleEyeColor = Color.BLACK
        val moleNoseColor = Color.rgb(255, 0, 0)

        paint.color = moleBodyColor
        canvas.drawRect(playerX, playerY, playerX + pixelSize, playerY + pixelSize, paint)

        paint.color = moleEyeColor
        canvas.drawRect(playerX + pixelSize * 0.2f, playerY + pixelSize * 0.3f, playerX + pixelSize * 0.4f, playerY + pixelSize * 0.5f, paint)
        canvas.drawRect(playerX + pixelSize * 0.6f, playerY + pixelSize * 0.3f, playerX + pixelSize * 0.8f, playerY + pixelSize * 0.5f, paint)

        paint.color = moleNoseColor
        canvas.drawRect(playerX + pixelSize * 0.4f, playerY + pixelSize * 0.7f, playerX + pixelSize * 0.6f, playerY + pixelSize * 0.9f, paint)
    }

    fun makeWoodBlocksFall() {
        var hasFloatingBlocks = false

        // Duyệt từ hàng thứ hai lên đến hàng cuối cùng để các khối gỗ rơi xuống
        for (row in 1 until blocks.size) { // Bỏ qua hàng đầu tiên
            for (col in blocks[row].indices) {
                val block = blocks[row][col]

                // Chỉ xử lý nếu là khối gỗ
                if (block.type == BlockType.WOOD) {
                    var currentRow = row

                    // Di chuyển khối gỗ lên đến vị trí trống phía trên nó
                    while (currentRow - 1 >= 0 && blocks[currentRow - 1][col].type == BlockType.EMPTY) {
                        // Hoán đổi vị trí khối gỗ với ô trống bên trên
                        blocks[currentRow - 1][col] = block
                        blocks[currentRow][col] = Block(BlockType.EMPTY, 0)

                        // Cập nhật vị trí hiện tại lên trên một hàng
                        currentRow--
                        hasFloatingBlocks = true
                    }
                }
            }
        }

        // Vẽ lại giao diện nếu có thay đổi
        if (hasFloatingBlocks) {
            drawGame(holder)
        }
    }

    fun movePlayer(dx: Float, dy: Float) {
        playerDAO.movePlayer(dx, dy)
    }

    fun digBlock(dx: Int, dy: Int) {
        playerDAO.digBlock(dx, dy)
    }


    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        playerDAO.stopHealthDecrement()
        fallHandler.removeCallbacks(fallRunnable)
    }
}