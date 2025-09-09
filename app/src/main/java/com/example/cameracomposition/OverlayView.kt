package com.example.cameracomposition

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class AspectRatio(val displayName: String, val ratio: Float) {
        SQUARE("6x6", 1.0f),
        STANDARD("6x9", 1.5f),
        WIDE("6x12", 2.0f),
        FOUR_BY_FIVE("4x5", 4f / 5f)
    }

    enum class GridType(val displayName: String) {
        NONE("Grid Off"),
        THIRDS("Thirds"),
        CENTER_CROSS("Cross"),
        // --- Tutorial Specific Grids ---
        THIRDS_INTERSECTIONS("Thirds+"),
        HEADROOM_GUIDE("Headroom"),
        LEADING_LINES("Lines"),
        NATURAL_FRAME("Frame")
    }

    private var currentAspectRatio: AspectRatio = AspectRatio.SQUARE
    private var currentGridType: GridType = GridType.THIRDS

    private val framePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    // --- Updated Paint for the "Glowing" Grid ---
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#80FF0000") // Use a semi-transparent red
        style = Paint.Style.STROKE
        strokeWidth = 2.6f // Thinner stroke
        isAntiAlias = true
        // SCREEN blend mode brightens where colors overlap.
        // This makes the red line "glow" over light parts of the image.
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
    }

    private val tutorialHighlightPaint = Paint().apply {
        color = Color.parseColor("#A6FFD700") // Semi-transparent gold
        style = Paint.Style.FILL
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
    }

    private val tutorialFramePaint = Paint().apply {
        color = Color.parseColor("#A640E0D0") // Semi-transparent turquoise
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(30f, 20f), 0f)
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
    }
    // ---------------------------------------------

    private val dimPaint = Paint().apply {
        color = Color.parseColor("#80000000")
        style = Paint.Style.FILL
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val rect = RectF()

    fun setAspectRatio(aspectRatio: AspectRatio) {
        currentAspectRatio = aspectRatio
        invalidate()
    }

    fun setGridType(gridType: GridType) {
        currentGridType = gridType
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val targetWidth: Float
        val targetHeight: Float

        if (viewWidth / viewHeight > currentAspectRatio.ratio) {
            targetHeight = viewHeight
            targetWidth = targetHeight * currentAspectRatio.ratio
        } else {
            targetWidth = viewWidth
            targetHeight = targetWidth / currentAspectRatio.ratio
        }

        val left = (viewWidth - targetWidth) / 2
        val top = (viewHeight - targetHeight) / 2
        rect.set(left, top, left + targetWidth, top + targetHeight)

        val save = canvas.saveLayer(0f, 0f, viewWidth, viewHeight, null)
        canvas.drawRect(0f, 0f, viewWidth, viewHeight, dimPaint)
        canvas.drawRect(rect, clearPaint)
        canvas.restoreToCount(save)

        canvas.drawRect(rect, framePaint)

        // --- Use a 'when' statement to draw the correct grid ---
        when (currentGridType) {
            GridType.THIRDS -> {
                val thirdWidth = rect.width() / 3
                val thirdHeight = rect.height() / 3
                // Vertical lines
                canvas.drawLine(rect.left + thirdWidth, rect.top, rect.left + thirdWidth, rect.bottom, gridPaint)
                canvas.drawLine(rect.left + thirdWidth * 2, rect.top, rect.left + thirdWidth * 2, rect.bottom, gridPaint)
                // Horizontal lines
                canvas.drawLine(rect.left, rect.top + thirdHeight, rect.right, rect.top + thirdHeight, gridPaint)
                canvas.drawLine(rect.left, rect.top + thirdHeight * 2, rect.right, rect.top + thirdHeight * 2, gridPaint)
            }
            GridType.CENTER_CROSS -> {
                val centerX = rect.centerX()
                val centerY = rect.centerY()
                // Vertical line
                canvas.drawLine(centerX, rect.top, centerX, rect.bottom, gridPaint)
                // Horizontal line
                canvas.drawLine(rect.left, centerY, rect.right, centerY, gridPaint)
            }
            GridType.NONE -> {
                // Do nothing
            }
            // --- TUTORIAL GRIDS ---
            GridType.THIRDS_INTERSECTIONS -> {
                val thirdWidth = rect.width() / 3
                val thirdHeight = rect.height() / 3
                // Draw standard thirds grid first
                canvas.drawLine(rect.left + thirdWidth, rect.top, rect.left + thirdWidth, rect.bottom, gridPaint)
                canvas.drawLine(rect.left + thirdWidth * 2, rect.top, rect.left + thirdWidth * 2, rect.bottom, gridPaint)
                canvas.drawLine(rect.left, rect.top + thirdHeight, rect.right, rect.top + thirdHeight, gridPaint)
                canvas.drawLine(rect.left, rect.top + thirdHeight * 2, rect.right, rect.top + thirdHeight * 2, gridPaint)

                // Now highlight the intersections
                val radius = 25f
                canvas.drawCircle(rect.left + thirdWidth, rect.top + thirdHeight, radius, tutorialHighlightPaint)
                canvas.drawCircle(rect.left + thirdWidth * 2, rect.top + thirdHeight, radius, tutorialHighlightPaint)
                canvas.drawCircle(rect.left + thirdWidth, rect.top + thirdHeight * 2, radius, tutorialHighlightPaint)
                canvas.drawCircle(rect.left + thirdWidth * 2, rect.top + thirdHeight * 2, radius, tutorialHighlightPaint)
            }
            GridType.HEADROOM_GUIDE -> {
                val thirdHeight = rect.height() / 3
                canvas.drawLine(rect.left, rect.top + thirdHeight, rect.right, rect.top + thirdHeight, tutorialFramePaint)
            }
            // Add other tutorial grid drawing logic here in the future
            else -> {
                // Default case, do nothing
            }
        }
        // ---------------------------------------------------------
    }
}

