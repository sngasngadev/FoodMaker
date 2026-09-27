package com.foodmaker.app.ui.scenes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

object SceneArt {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun workArea(w: Int, h: Int): RectF =
        RectF(w * 0.12f, h * 0.28f, w * 0.88f, h * 0.76f)

    fun targetArea(w: Int, h: Int): RectF =
        RectF(w * 0.23f, h * 0.35f, w * 0.77f, h * 0.67f)

    fun dishArea(w: Int, h: Int): RectF =
        RectF(w * 0.10f, h * 0.30f, w * 0.90f, h * 0.66f)

    fun trayArea(w: Int, h: Int): RectF =
        RectF(w * 0.18f, h * 0.69f, w * 0.82f, h * 0.84f)

    fun drawBoard(canvas: Canvas, w: Int, h: Int) {
        val r = workArea(w, h)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(226, 188, 128)
        canvas.drawRoundRect(r, 38f, 38f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.007f
        paint.color = Color.rgb(174, 129, 79)
        canvas.drawRoundRect(r, 38f, 38f, paint)
        paint.style = Paint.Style.FILL
    }

    fun drawPlate(canvas: Canvas, w: Int, h: Int) {
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(247, 241, 229)
        canvas.drawOval(RectF(w * 0.17f, h * 0.37f, w * 0.83f, h * 0.66f), paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.006f
        paint.color = Color.rgb(221, 210, 194)
        canvas.drawOval(RectF(w * 0.22f, h * 0.41f, w * 0.78f, h * 0.62f), paint)
        paint.style = Paint.Style.FILL
    }

    fun drawTray(canvas: Canvas, w: Int, h: Int) {
        val r = trayArea(w, h)
        paint.color = Color.rgb(214, 220, 222)
        canvas.drawRoundRect(r, 24f, 24f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.006f
        paint.color = Color.rgb(150, 157, 160)
        canvas.drawRoundRect(r, 24f, 24f, paint)
        paint.style = Paint.Style.FILL
    }

    fun drawPan(canvas: Canvas, w: Int, h: Int) {
        val cx = w / 2f
        val cy = h * 0.51f
        val r = w * 0.29f
        paint.color = Color.rgb(54, 57, 60)
        canvas.drawCircle(cx, cy, r, paint)
        paint.color = Color.rgb(86, 90, 93)
        canvas.drawCircle(cx, cy, r * 0.82f, paint)
        paint.strokeWidth = w * 0.055f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(72, 74, 76)
        canvas.drawLine(cx + r * 0.78f, cy + r * 0.65f, w * 0.92f, h * 0.70f, paint)
        paint.strokeCap = Paint.Cap.BUTT
    }

    fun ovenRect(w: Int, h: Int): RectF =
        RectF(w * 0.16f, h * 0.29f, w * 0.84f, h * 0.63f)

    fun ovenInside(w: Int, h: Int): RectF {
        val o = ovenRect(w, h)
        return RectF(o.left + w * 0.05f, o.top + h * 0.055f, o.right - w * 0.05f, o.bottom - h * 0.05f)
    }

    fun drawOven(canvas: Canvas, w: Int, h: Int, hot: Boolean) {
        val o = ovenRect(w, h)
        val inside = ovenInside(w, h)
        paint.color = Color.rgb(92, 89, 84)
        canvas.drawRoundRect(o, 28f, 28f, paint)
        paint.color = Color.rgb(42, 43, 44)
        canvas.drawRoundRect(inside, 22f, 22f, paint)
        paint.color = if (hot) Color.rgb(238, 126, 48) else Color.rgb(124, 116, 106)
        repeat(3) { i ->
            val y = inside.top + inside.height() * (0.28f + i * 0.20f)
            canvas.drawRoundRect(RectF(inside.left + 18f, y, inside.right - 18f, y + 7f), 4f, 4f, paint)
        }
    }

    fun drawKnife(canvas: Canvas, w: Int, cx: Float, cy: Float) {
        canvas.save()
        canvas.rotate(-18f, cx, cy)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(45, 45, 45)
        val outline = Path().apply {
            moveTo(cx - w * 0.16f, cy)
            lineTo(cx + w * 0.04f, cy - w * 0.045f)
            lineTo(cx + w * 0.04f, cy + w * 0.045f)
            close()
        }
        canvas.drawPath(outline, paint)
        paint.color = Color.rgb(224, 229, 231)
        val blade = Path().apply {
            moveTo(cx - w * 0.145f, cy)
            lineTo(cx + w * 0.035f, cy - w * 0.034f)
            lineTo(cx + w * 0.035f, cy + w * 0.034f)
            close()
        }
        canvas.drawPath(blade, paint)
        paint.color = Color.rgb(65, 137, 86)
        canvas.drawRoundRect(RectF(cx + w * 0.02f, cy - w * 0.035f, cx + w * 0.13f, cy + w * 0.035f), 14f, 14f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.008f
        paint.color = Color.rgb(45, 45, 45)
        canvas.drawRoundRect(RectF(cx + w * 0.02f, cy - w * 0.035f, cx + w * 0.13f, cy + w * 0.035f), 14f, 14f, paint)
        paint.style = Paint.Style.FILL
        canvas.restore()
    }

    fun drawPizzaCutter(canvas: Canvas, w: Int, cx: Float, cy: Float) {
        paint.color = Color.rgb(48, 48, 48)
        canvas.drawCircle(cx - w * 0.035f, cy, w * 0.07f, paint)
        paint.color = Color.rgb(215, 219, 220)
        canvas.drawCircle(cx - w * 0.035f, cy, w * 0.055f, paint)
        paint.strokeWidth = w * 0.02f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(65, 137, 86)
        canvas.drawLine(cx + w * 0.02f, cy, cx + w * 0.15f, cy + w * 0.03f, paint)
        paint.strokeCap = Paint.Cap.BUTT
    }

    fun drawRollingPin(canvas: Canvas, w: Int, cx: Float, cy: Float) {
        paint.color = Color.rgb(168, 102, 50)
        canvas.drawRoundRect(RectF(cx - w * 0.15f, cy - w * 0.04f, cx + w * 0.15f, cy + w * 0.04f), 24f, 24f, paint)
        paint.strokeWidth = w * 0.018f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(116, 76, 42)
        canvas.drawLine(cx - w * 0.19f, cy, cx - w * 0.14f, cy, paint)
        canvas.drawLine(cx + w * 0.14f, cy, cx + w * 0.19f, cy, paint)
        paint.strokeCap = Paint.Cap.BUTT
    }

    fun drawSpoon(canvas: Canvas, w: Int, cx: Float, cy: Float) {
        canvas.save()
        canvas.rotate(-25f, cx, cy)
        paint.color = Color.rgb(65, 65, 65)
        canvas.drawOval(RectF(cx - w * 0.055f, cy - w * 0.045f, cx + w * 0.035f, cy + w * 0.045f), paint)
        paint.color = Color.rgb(210, 214, 216)
        canvas.drawOval(RectF(cx - w * 0.045f, cy - w * 0.035f, cx + w * 0.025f, cy + w * 0.035f), paint)
        paint.strokeWidth = w * 0.018f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(85, 85, 85)
        canvas.drawLine(cx + w * 0.02f, cy, cx + w * 0.18f, cy, paint)
        paint.strokeCap = Paint.Cap.BUTT
        canvas.restore()
    }

    fun drawSpatula(canvas: Canvas, w: Int, cx: Float, cy: Float) {
        canvas.save()
        canvas.rotate(-28f, cx, cy)
        paint.color = Color.rgb(50, 50, 50)
        canvas.drawRoundRect(RectF(cx - w * 0.075f, cy - w * 0.07f, cx + w * 0.055f, cy + w * 0.06f), 14f, 14f, paint)
        paint.color = Color.rgb(184, 190, 193)
        canvas.drawRoundRect(RectF(cx - w * 0.06f, cy - w * 0.055f, cx + w * 0.04f, cy + w * 0.045f), 12f, 12f, paint)
        paint.strokeWidth = w * 0.012f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(170, 64, 55)
        canvas.drawLine(cx + w * 0.05f, cy, cx + w * 0.19f, cy, paint)
        paint.strokeCap = Paint.Cap.BUTT
        canvas.restore()
    }
}
