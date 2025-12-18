package com.example.levelcheck.ui.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 水平仪指示器视图 - 显示倾斜方向和角度
 */
class LevelIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var tiltAngle = 0
    private var tiltDirectionAngle = 0f  // 相对用户视角的倾斜方向角度

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        style = Paint.Style.FILL
    }

    private val centerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL
    }

    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFA726")
        style = Paint.Style.FILL
        setShadowLayer(8f, 0f, 4f, Color.parseColor("#80000000"))
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BDBDBD")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /**
     * 更新倾斜角度
     */
    fun updateTilt(angle: Int) {
        tiltAngle = angle.coerceIn(0, 90)
        invalidate()
    }

    /**
     * 更新倾斜方向角度（相对用户视角）
     */
    fun updateTiltDirection(angle: Float) {
        tiltDirectionAngle = angle
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 2f - 40f

        // 绘制外圆
        canvas.drawCircle(centerX, centerY, radius, circlePaint)

        // 绘制网格线
        drawGrid(canvas, centerX, centerY, radius)

        // 绘制中心圆（目标区域）
        val centerRadius = radius * 0.15f
        canvas.drawCircle(centerX, centerY, centerRadius, centerCirclePaint)

        // 计算气泡位置（基于倾斜角度和相对方向角度）
        if (tiltAngle > 0) {
            val maxOffset = radius * 0.7f
            val offset = (tiltAngle / 90f) * maxOffset
            // 使用相对方向角度来计算气泡位置
            val angleRad = Math.toRadians(tiltDirectionAngle.toDouble())
            val bubbleX = centerX + (offset * sin(angleRad)).toFloat()
            val bubbleY = centerY - (offset * cos(angleRad)).toFloat()

            // 绘制气泡
            val bubbleRadius = radius * 0.12f
            canvas.drawCircle(bubbleX, bubbleY, bubbleRadius, bubblePaint)
        } else {
            // 水平时气泡在中心
            val bubbleRadius = radius * 0.12f
            canvas.drawCircle(centerX, centerY, bubbleRadius, bubblePaint)
        }

        // 绘制角度文本
        canvas.drawText("${tiltAngle}°", centerX, centerY + radius + 50f, textPaint)
    }

    /**
     * 绘制网格线
     */
    private fun drawGrid(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        // 绘制十字线
        canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, gridPaint)
        canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, gridPaint)

        // 绘制同心圆（表示倾斜程度）
        for (i in 1..3) {
            val r = radius * i / 3f
            canvas.drawCircle(centerX, centerY, r, gridPaint)
        }
    }
}
