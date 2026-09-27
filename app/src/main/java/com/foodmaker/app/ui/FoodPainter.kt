package com.foodmaker.app.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.foodmaker.app.model.PartDefinition
import kotlin.math.min

object FoodPainter {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun drawPart(canvas: Canvas, part: PartDefinition, rect: RectF) {
        paint.style = Paint.Style.FILL
        paint.color = parseColor(part.colorHex)
        when (part.shape) {
            "circle" -> canvas.drawCircle(rect.centerX(), rect.centerY(), min(rect.width(), rect.height()) / 2f, paint)
            "oval" -> canvas.drawOval(rect, paint)
            "triangle" -> {
                val p = Path().apply {
                    moveTo(rect.centerX(), rect.top)
                    lineTo(rect.right, rect.bottom)
                    lineTo(rect.left, rect.bottom)
                    close()
                }
                canvas.drawPath(p, paint)
            }
            "lettuce" -> drawLettuce(canvas, rect)
            "cheese" -> drawCheese(canvas, rect)
            "nori" -> {
                canvas.drawRoundRect(rect, 16f, 16f, paint)
                part.accentHex?.let {
                    paint.color = parseColor(it)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 4f
                    canvas.drawRoundRect(rect, 16f, 16f, paint)
                }
            }
            "rice" -> {
                canvas.drawOval(rect, paint)
                part.accentHex?.let {
                    paint.color = parseColor(it)
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 3f
                    canvas.drawOval(rect, paint)
                }
            }
            "salmon" -> {
                canvas.drawRoundRect(rect, 18f, 18f, paint)
                part.accentHex?.let {
                    paint.color = parseColor(it)
                    paint.strokeWidth = 5f
                    repeat(3) { i ->
                        val y = rect.top + rect.height() * (0.28f + i * 0.2f)
                        canvas.drawLine(rect.left + 10f, y, rect.right - 10f, y - 8f, paint)
                    }
                }
            }
            else -> canvas.drawRoundRect(rect, 24f, 24f, paint)
        }
        paint.style = Paint.Style.FILL
    }

    fun drawDish(canvas: Canvas, partIds: List<String>, parts: Map<String, PartDefinition>, area: RectF) {
        if (partIds.isEmpty()) return
        val count = partIds.size
        val layerHeight = (area.height() * 0.56f / count.coerceAtLeast(3)).coerceIn(22f, 48f)
        val centerY = area.centerY()
        partIds.forEachIndexed { index, id ->
            val part = parts[id] ?: return@forEachIndexed
            val y = centerY + (index - (count - 1) / 2f) * layerHeight
            val widthFactor = when (part.shape) {
                "circle" -> 0.62f
                "triangle" -> 0.72f
                else -> 0.76f
            }
            val w = area.width() * widthFactor
            val h = when (part.shape) {
                "circle" -> min(w, area.height() * 0.55f)
                "oval" -> layerHeight * 1.45f
                "triangle" -> layerHeight * 1.9f
                else -> layerHeight * 1.25f
            }
            drawPart(canvas, part, RectF(area.centerX() - w / 2f, y - h / 2f, area.centerX() + w / 2f, y + h / 2f))
        }
    }

    private fun drawLettuce(canvas: Canvas, rect: RectF) {
        val p = Path()
        val waves = 8
        p.moveTo(rect.left, rect.centerY())
        for (i in 0..waves) {
            val x = rect.left + rect.width() * i / waves
            val y = if (i % 2 == 0) rect.top else rect.top + rect.height() * 0.35f
            p.lineTo(x, y)
        }
        p.lineTo(rect.right, rect.bottom)
        for (i in waves downTo 0) {
            val x = rect.left + rect.width() * i / waves
            val y = if (i % 2 == 0) rect.bottom else rect.bottom - rect.height() * 0.35f
            p.lineTo(x, y)
        }
        p.close()
        canvas.drawPath(p, paint)
    }

    private fun drawCheese(canvas: Canvas, rect: RectF) {
        val p = Path().apply {
            moveTo(rect.centerX(), rect.top)
            lineTo(rect.right, rect.centerY())
            lineTo(rect.centerX(), rect.bottom)
            lineTo(rect.left, rect.centerY())
            close()
        }
        canvas.drawPath(p, paint)
    }

    private fun parseColor(value: String): Int =
        runCatching { Color.parseColor(value) }.getOrDefault(Color.LTGRAY)
}
