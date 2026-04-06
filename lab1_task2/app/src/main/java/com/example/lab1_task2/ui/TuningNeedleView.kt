package com.example.lab1_task2.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin

class TuningNeedleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var centsOff = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arcRect = RectF()
    private var needleAnimator: ValueAnimator? = null

    // Colors
    private val colorInTune = Color.GREEN
    private val colorNearTune = Color.YELLOW
    private val colorOffTune = Color.RED

    fun setCentsOff(cents: Float) {
        val targetCents = cents.coerceIn(-50f, 50f)
        
        needleAnimator?.cancel()
        needleAnimator = ValueAnimator.ofFloat(centsOff, targetCents).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                centsOff = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val centerX = width / 2f
        val centerY = height * 0.8f
        val radius = width.coerceAtMost(height) * 0.4f

        // 1. Draw semicircular arc
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        paint.color = Color.LTGRAY
        arcRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        canvas.drawArc(arcRect, 180f, 180f, false, paint)

        // 2. Draw tick marks
        val tickLength = 20f
        val ticks = listOf(-50f, -25f, 0f, 25f, 50f)
        paint.strokeWidth = 4f
        for (tickCents in ticks) {
            val angleDeg = 180f + (tickCents + 50f) * 1.8f
            val angleRad = Math.toRadians(angleDeg.toDouble())
            
            val startX = centerX + radius * cos(angleRad).toFloat()
            val startY = centerY + radius * sin(angleRad).toFloat()
            val endX = centerX + (radius - tickLength) * cos(angleRad).toFloat()
            val endY = centerY + (radius - tickLength) * sin(angleRad).toFloat()
            
            canvas.drawLine(startX, startY, endX, endY, paint)
        }

        // 3. Draw labels
        paint.style = Paint.Style.FILL
        paint.textSize = 36f
        paint.textAlign = Paint.Align.CENTER
        
        // Flat
        canvas.drawText("Flat", centerX - radius, centerY + 50f, paint)
        // In Tune
        canvas.drawText("In Tune", centerX, centerY - radius - 30f, paint)
        // Sharp
        canvas.drawText("Sharp", centerX + radius, centerY + 50f, paint)

        // 4. Draw needle
        // Determine needle color
        val absCents = kotlin.math.abs(centsOff)
        paint.color = when {
            absCents <= 5f -> colorInTune
            absCents <= 15f -> colorNearTune
            else -> colorOffTune
        }
        
        paint.strokeWidth = 10f
        val needleAngleDeg = 180f + (centsOff + 50f) * 1.8f
        val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
        val needleEndX = centerX + (radius - 10f) * cos(needleAngleRad).toFloat()
        val needleEndY = centerY + (radius - 10f) * sin(needleAngleRad).toFloat()
        
        canvas.drawLine(centerX, centerY, needleEndX, needleEndY, paint)
        
        // Draw pivot point
        canvas.drawCircle(centerX, centerY, 15f, paint)
    }
}
