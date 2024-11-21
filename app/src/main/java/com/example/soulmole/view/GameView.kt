package com.example.soulmole.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.soulmole.activity.GameActivity
import com.example.soulmole.controller.PlayerManager
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

    lateinit var playerManager: PlayerManager
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
        playerManager = PlayerManager(this, player)
        (context as GameActivity).player = player
        isInitialized = true
        if (holder.surface.isValid) {
            playerManager.startHealthDecrement()
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

        // Chỉ vẽ game khi player đã được khởi tạo
        if (isInitialized) {
            playerManager.startHealthDecrement()
            drawGame(holder)
            fallHandler.post(fallRunnable)
        }
    }

    fun drawGame(holder: SurfaceHolder) {
        // Chỉ tiếp tục nếu player đã được khởi tạo
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
        playerManager.isMovingUp = false
        playerManager.freezeRow = -1
        playerManager.player.depth += 5
        playerManager.player.score += 20
        showCurrentLevel()
    }

    fun isPlayerAtBottomRow(): Boolean {
        val playerRow = ((maxY - playerY - pixelSize) / blockSize).toInt()
        return playerRow == 0
    }

    private fun drawRestrictedArea(canvas: Canvas) {
        // Tạo màu gradient giống như ảnh nền bạn đã gửi
        val topColor = Color.rgb(178, 190, 211)    // Màu xanh lam nhạt ở trên cùng
        val middleColor = Color.rgb(223, 212, 191) // Màu be ở giữa
        val bottomColor = Color.rgb(46, 39, 75)    // Màu tím đậm ở phía dưới

        // Tạo một LinearGradient cho hiệu ứng gradient
        val gradient = LinearGradient(
            width * 5 / 7f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(topColor, middleColor, bottomColor),
            floatArrayOf(0.0f, 0.5f, 1.0f), // Điều chỉnh vị trí gradient nếu cần
            Shader.TileMode.CLAMP
        )

        // Áp dụng gradient vào paint và vẽ hình chữ nhật
        paint.shader = gradient
        canvas.drawRect(width * 5 / 7f, 0f, width.toFloat(), height.toFloat(), paint)

        // Xóa shader sau khi vẽ xong để không ảnh hưởng đến các thành phần khác
        paint.shader = null

        // Vẽ đường kẻ màu đen
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
        canvas.drawText(playerManager.player.health.toString(), centerX, centerY + 15f, paint)

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
        canvas.drawText(playerManager.player.depth.toString(), (rectLeft + rectRight) / 2, rectTop + 70f, paint)

        // Vẽ ô chữ nhật chứa "Score" và giá trị điểm số từ playerDAO.player.score
        paint.color = Color.DKGRAY
        val scoreRectTop = rectTop + 120f
        val scoreRectBottom = scoreRectTop + 100f
        canvas.drawRect(rectLeft, scoreRectTop, rectRight, scoreRectBottom, paint)

        paint.color = Color.WHITE
        canvas.drawText("SCORE", (rectLeft + rectRight) / 2, scoreRectTop + 30f, paint)
        canvas.drawText(playerManager.player.score.toString(), (rectLeft + rectRight) / 2, scoreRectTop + 70f, paint)
    }


    private fun drawBlocks(canvas: Canvas) {
        for (row in blocks.indices) {
            for (col in blocks[row].indices) {
                val block = blocks[row][col]
                val x = col * blockSize
                val y = maxY - (row + 1) * blockSize

                when (block.type) {
                    BlockType.DIRT -> {
                        // Màu nền của khối đất
                        paint.color = Color.rgb(139, 69, 19)
                        canvas.drawRect(x, y, x + blockSize, y + blockSize, paint)

                        // Thêm các đốm đen để tạo cảm giác texture của đất
                        paint.color = Color.rgb(120, 60, 15)
                        for (i in 0 until 5) {
                            val dotX = x + (Math.random() * blockSize).toFloat()
                            val dotY = y + (Math.random() * blockSize).toFloat()
                            canvas.drawCircle(dotX, dotY, 6f, paint)
                        }
                    }

                    BlockType.STONE -> {
                        // Màu nền của khối đá
                        paint.color = Color.DKGRAY
                        canvas.drawRect(x, y, x + blockSize, y + blockSize, paint)

                        // Thêm các vệt sáng để tạo hiệu ứng đá
                        paint.color = Color.rgb(169, 169, 169)
                        for (i in 0 until 5) {
                            val lineX1 = x + (Math.random() * blockSize / 2).toFloat()
                            val lineY1 = y + (Math.random() * blockSize).toFloat()
                            val lineX2 = lineX1 + (Math.random() * blockSize / 2).toFloat()
                            val lineY2 = lineY1 + (Math.random() * 5).toFloat()
                            canvas.drawLine(lineX1, lineY1, lineX2, lineY2, paint)
                        }
                    }

                    BlockType.WOOD -> {
                        // Màu nền của khối gỗ (#deb887)
                        paint.color = Color.parseColor("#deb887")
                        canvas.drawRect(x, y, x + blockSize, y + blockSize, paint)

                        // Tạo các vân gỗ rõ rệt bằng cách vẽ các đường cong hình tròn và elip
                        paint.color = Color.rgb(139, 69, 19) // Màu tối hơn cho vân gỗ
                        paint.strokeWidth = 2f
                        paint.style = Paint.Style.STROKE

                        // Vẽ các vòng gỗ
                        val centerX = x + blockSize / 2
                        val centerY = y + blockSize / 2
                        for (i in 1..3) {
                            val radius = (blockSize / 4 * i) / 2f
                            canvas.drawCircle(centerX, centerY, radius, paint)
                        }

                        // Vẽ thêm các đường cong nhỏ để tạo cảm giác tự nhiên của vân gỗ
                        for (i in 0 until 3) {
                            val startX = x + (Math.random() * blockSize).toFloat()
                            val startY = y + (Math.random() * blockSize).toFloat()
                            val endX = startX + (Math.random() * blockSize / 4).toFloat()
                            val endY = startY + (Math.random() * blockSize / 4).toFloat()
                            canvas.drawLine(startX, startY, endX, endY, paint)
                        }

                        paint.style = Paint.Style.FILL // Reset lại style cho các khối khác
                    }

                    BlockType.EMPTY -> {
                        // Khối rỗng (trong suốt)
                        paint.color = Color.TRANSPARENT
                    }
                }
            }
        }
    }

    private fun drawMolePixelArt(canvas: Canvas) {
        // Màu sắc cho các phần của chuột chũi
        val moleBodyColor = Color.rgb(102, 51, 0)  // Màu nâu cho cơ thể
        val moleEyeColor = Color.BLACK             // Màu đen cho mắt
        val moleNoseColor = Color.rgb(255, 100, 100) // Màu hồng nhạt cho mũi
        val moleHandColor = Color.rgb(153, 76, 0)   // Màu cho bàn tay
        val pickaxeHandleColor = Color.rgb(139, 69, 19)  // Màu nâu cho cán cuốc
        val pickaxeHeadColor = Color.DKGRAY        // Màu xám đậm cho đầu cuốc
        val moleToothColor = Color.WHITE           // Màu trắng cho răng

        // Vẽ thân chuột chũi (hình elip lớn hơn, tròn trịa)
        paint.color = moleBodyColor
        val bodyLeft = playerX + pixelSize * 0.15f
        val bodyRight = playerX + pixelSize * 0.85f
        val bodyTop = playerY + pixelSize * 0.4f
        val bodyBottom = playerY + pixelSize * 1.1f
        canvas.drawOval(bodyLeft, bodyTop, bodyRight, bodyBottom, paint)

        // Vẽ đầu chuột chũi (hình tròn nhỏ hơn trên thân)
        val headRadius = pixelSize * 0.3f
        val headCenterX = playerX + pixelSize * 0.5f
        val headCenterY = playerY + pixelSize * 0.35f
        canvas.drawCircle(headCenterX, headCenterY, headRadius, paint)

        // Vẽ tai chuột chũi (hai hình tròn nhỏ phía trên đầu)
        paint.color = moleBodyColor
        val leftEarCenterX = playerX + pixelSize * 0.35f
        val rightEarCenterX = playerX + pixelSize * 0.65f
        val earCenterY = playerY + pixelSize * 0.15f
        val earRadius = pixelSize * 0.1f
        canvas.drawCircle(leftEarCenterX, earCenterY, earRadius, paint)
        canvas.drawCircle(rightEarCenterX, earCenterY, earRadius, paint)

        // Vẽ mắt chuột chũi (hai hình tròn đen nhỏ)
        paint.color = moleEyeColor
        val leftEyeCenterX = playerX + pixelSize * 0.4f
        val rightEyeCenterX = playerX + pixelSize * 0.6f
        val eyeCenterY = playerY + pixelSize * 0.3f
        val eyeRadius = pixelSize * 0.05f
        canvas.drawCircle(leftEyeCenterX, eyeCenterY, eyeRadius, paint)
        canvas.drawCircle(rightEyeCenterX, eyeCenterY, eyeRadius, paint)

        // Vẽ mũi chuột chũi (hình tròn nhỏ màu hồng ở giữa)
        paint.color = moleNoseColor
        val noseCenterX = playerX + pixelSize * 0.5f
        val noseCenterY = playerY + pixelSize * 0.45f
        val noseRadius = pixelSize * 0.07f
        canvas.drawCircle(noseCenterX, noseCenterY, noseRadius, paint)

        // Vẽ răng chuột chũi (hai hình chữ nhật trắng bên dưới mũi)
        paint.color = moleToothColor
        val leftToothLeft = playerX + pixelSize * 0.45f
        val leftToothRight = playerX + pixelSize * 0.48f
        val toothTop = playerY + pixelSize * 0.55f
        val toothBottom = playerY + pixelSize * 0.65f
        canvas.drawRect(leftToothLeft, toothTop, leftToothRight, toothBottom, paint)

        val rightToothLeft = playerX + pixelSize * 0.52f
        val rightToothRight = playerX + pixelSize * 0.55f
        canvas.drawRect(rightToothLeft, toothTop, rightToothRight, toothBottom, paint)

        // Vẽ tay chuột chũi (tay trái cầm cuốc đất)
        paint.color = moleHandColor
        val leftHandLeft = playerX + pixelSize * 0.15f
        val leftHandRight = playerX + pixelSize * 0.3f
        val handTop = playerY + pixelSize * 0.6f
        val handBottom = playerY + pixelSize * 0.75f
        canvas.drawOval(leftHandLeft, handTop, leftHandRight, handBottom, paint)

        // Vẽ tay phải (tay không cầm gì)
        val rightHandLeft = playerX + pixelSize * 0.7f
        val rightHandRight = playerX + pixelSize * 0.85f
        canvas.drawOval(rightHandLeft, handTop, rightHandRight, handBottom, paint)

        // Vẽ cuốc đất
        // Cán cuốc
        paint.color = pickaxeHandleColor
        val handleLeft = playerX + pixelSize * 0.2f
        val handleRight = playerX + pixelSize * 0.35f
        val handleTop = playerY + pixelSize * 0.4f
        val handleBottom = playerY + pixelSize * 0.8f
        canvas.drawRect(handleLeft, handleTop, handleRight, handleBottom, paint)

        // Đầu cuốc
        paint.color = pickaxeHeadColor
        val headLeft = playerX + pixelSize * 0.15f
        val headRight = playerX + pixelSize * 0.4f
        val headTop = playerY + pixelSize * 0.35f
        val headBottom = playerY + pixelSize * 0.45f
        canvas.drawRect(headLeft, headTop, headRight, headBottom, paint)
    }




    fun makeWoodBlocksFall() {
        var hasFloatingBlocks = false

        // Duyệt từ hàng thứ hai lên đến hàng cuối cùng để các khối gỗ rơi xuống
        for (row in 1 until blocks.size) { // Bỏ qua hàng cuoi cùng
            for (col in blocks[row].indices) {
                val block = blocks[row][col]

                // Chỉ xử lý nếu là khối gỗ
                if (block.type == BlockType.WOOD) {
                    var currentRow = row

                    // Di chuyển khối gỗ lên đến vị trí trống phía dưới nó
                    while (currentRow - 1 >= 0 && blocks[currentRow - 1][col].type == BlockType.EMPTY) {
                        // Hoán đổi vị trí khối gỗ với ô trống bên dưới
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

    fun checkStoneBlocks(): Map<String, Boolean> {
        val col = (playerX / blockSize).toInt()
        val row = ((maxY - playerY - pixelSize) / blockSize).toInt()

        val directions = mutableMapOf(
            "up" to false,
            "down" to false,
            "left" to false,
            "right" to false
        )

        // Kiểm tra block phía dưới
        if (row - 1 >= 0 && col in blocks[row - 1].indices) {
            directions["down"] = blocks[row - 1][col].type == BlockType.STONE
        }

        // Kiểm tra block phía dưới
        if (row + 1 < blocks.size && col in blocks[row + 1].indices) {
            directions["up"] = blocks[row + 1][col].type == BlockType.STONE
        }

        // Kiểm tra block bên trái
        if (col - 1 >= 0 && row in blocks.indices && col - 1 in blocks[row].indices) {
            directions["left"] = blocks[row][col - 1].type == BlockType.STONE
        }

        // Kiểm tra block bên phải
        if (col + 1 < numColumns && row in blocks.indices && col + 1 in blocks[row].indices) {
            directions["right"] = blocks[row][col + 1].type == BlockType.STONE
        }

        return directions
    }


    fun movePlayer(dx: Float, dy: Float) {
        playerManager.movePlayer(dx, dy)
    }

    fun digBlock(dx: Int, dy: Int) {
        playerManager.digBlock(dx, dy)
    }


    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        playerManager.stopHealthDecrement()
        fallHandler.removeCallbacks(fallRunnable)
    }
}