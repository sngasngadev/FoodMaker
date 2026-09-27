package com.foodmaker.app.ui

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.foodmaker.app.model.PartDefinition
import kotlin.math.min
import kotlin.math.roundToInt

object FoodPainter {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val bitmapCache = mutableMapOf<String, Bitmap>()

    fun drawPart(
        canvas: Canvas,
        part: PartDefinition,
        rect: RectF,
        assets: AssetManager? = null,
        alpha: Int = 255
    ) {
        paint.alpha = alpha
        if (assets != null && drawAsset(canvas, part, rect, assets)) {
            paint.alpha = 255
            return
        }
        drawFallback(canvas, part, rect)
        paint.alpha = 255
    }

    fun drawDish(
        canvas: Canvas,
        partIds: List<String>,
        parts: Map<String, PartDefinition>,
        area: RectF,
        assets: AssetManager? = null
    ) {
        if (partIds.isEmpty()) return

        if (partIds.size == 1) {
            val part = parts[partIds.first()] ?: return
            val insetX = area.width() * 0.04f
            val insetY = area.height() * 0.04f
            drawPart(
                canvas,
                part,
                RectF(area.left + insetX, area.top + insetY, area.right - insetX, area.bottom - insetY),
                assets
            )
            return
        }

        val count = partIds.size
        val visualHeight = area.height() * 0.40f
        val spacing = min(area.height() * 0.09f, area.height() * 0.52f / (count - 1).coerceAtLeast(1))
        val totalHeight = visualHeight + spacing * (count - 1)
        val topCenterY = area.centerY() - totalHeight / 2f + visualHeight / 2f
        val widthInset = area.width() * 0.055f

        partIds.forEachIndexed { index, id ->
            val part = parts[id] ?: return@forEachIndexed
            // Recipe lists are bottom-to-top. Draw the first item visually lowest.
            val visualIndex = count - 1 - index
            val cy = topCenterY + spacing * visualIndex
            drawPart(
                canvas,
                part,
                RectF(
                    area.left + widthInset,
                    cy - visualHeight / 2f,
                    area.right - widthInset,
                    cy + visualHeight / 2f
                ),
                assets
            )
        }
    }

    private fun drawAsset(
        canvas: Canvas,
        part: PartDefinition,
        rect: RectF,
        assets: AssetManager
    ): Boolean {
        val path = part.assetPath ?: return false
        val bitmap = bitmapCache[path] ?: runCatching {
            assets.open(path).use(BitmapFactory::decodeStream)
        }.getOrNull()?.also { bitmapCache[path] = it } ?: return false

        val crop = part.assetCrop
        val src = if (crop != null && crop.size == 4) {
            Rect(
                (crop[0].coerceIn(0f, 1f) * bitmap.width).roundToInt(),
                (crop[1].coerceIn(0f, 1f) * bitmap.height).roundToInt(),
                (crop[2].coerceIn(0f, 1f) * bitmap.width).roundToInt(),
                (crop[3].coerceIn(0f, 1f) * bitmap.height).roundToInt()
            )
        } else {
            Rect(0, 0, bitmap.width, bitmap.height)
        }

        if (src.width() <= 0 || src.height() <= 0) return false

        val scale = min(rect.width() / src.width(), rect.height() / src.height())
        val drawWidth = src.width() * scale
        val drawHeight = src.height() * scale
        val dest = RectF(
            rect.centerX() - drawWidth / 2f,
            rect.centerY() - drawHeight / 2f,
            rect.centerX() + drawWidth / 2f,
            rect.centerY() + drawHeight / 2f
        )
        canvas.drawBitmap(bitmap, src, dest, paint)
        return true
    }

    private fun drawFallback(canvas: Canvas, part: PartDefinition, rect: RectF) {
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
            "shreds" -> drawShreds(canvas, rect)
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

    private fun drawShreds(canvas: Canvas, rect: RectF) {
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = (rect.width() * 0.035f).coerceAtLeast(4f)
            color = Color.rgb(214, 155, 25)
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = (rect.width() * 0.022f).coerceAtLeast(3f)
            color = Color.rgb(255, 218, 76)
        }
        val lines = listOf(
            floatArrayOf(.14f,.28f,.42f,.42f),
            floatArrayOf(.36f,.22f,.70f,.34f),
            floatArrayOf(.58f,.46f,.87f,.34f),
            floatArrayOf(.18f,.55f,.48f,.65f),
            floatArrayOf(.47f,.62f,.78f,.73f),
            floatArrayOf(.25f,.78f,.58f,.72f),
            floatArrayOf(.63f,.78f,.84f,.61f)
        )
        lines.forEach { l ->
            val x1 = rect.left + rect.width()*l[0]
            val y1 = rect.top + rect.height()*l[1]
            val x2 = rect.left + rect.width()*l[2]
            val y2 = rect.top + rect.height()*l[3]
            canvas.drawLine(x1,y1,x2,y2,outline)
            canvas.drawLine(x1,y1,x2,y2,fill)
        }
    }

    private fun parseColor(value: String): Int =
        runCatching { Color.parseColor(value) }.getOrDefault(Color.LTGRAY)
}
