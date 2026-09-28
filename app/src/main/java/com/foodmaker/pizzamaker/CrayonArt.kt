package com.foodmaker.pizzamaker

import android.graphics.*
import kotlin.math.*

/** UI-only drawing. All food, tools, oven and background art comes from image assets. */
class CrayonArt {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private fun j(s: Int, a: Float): Float {
        val v = sin(s * 12.9898 + 78.233) * 43758.5453
        return ((v - floor(v)) * 2 - 1).toFloat() * a
    }

    fun line(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, col: Int, w: Float, s: Int = 0) {
        p.style = Paint.Style.STROKE
        repeat(3) { i ->
            p.color = if (i == 0) col else Color.argb(120, Color.red(col), Color.green(col), Color.blue(col))
            p.strokeWidth = w - i * .8f
            c.drawLine(x1 + j(s + i, 2f), y1 + j(s + 7 + i, 2f), x2 + j(s + 13 + i, 2f), y2 + j(s + 19 + i, 2f), p)
        }
    }

    fun circle(c: Canvas, x: Float, y: Float, r: Float, fill: Int, s: Int = 0) {
        p.style = Paint.Style.FILL
        p.color = fill
        c.drawCircle(x, y, r, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f
        p.color = Color.rgb(53, 45, 39)
        repeat(2) { i -> c.drawCircle(x + j(s + i, 1.7f), y + j(s + 8 + i, 1.7f), r + j(s + 16 + i, 1.3f), p) }
    }

    fun box(c: Canvas, r: RectF, rad: Float, fill: Int, s: Int = 0) {
        p.style = Paint.Style.FILL
        p.color = fill
        c.drawRoundRect(r, rad, rad, p)
        p.style = Paint.Style.STROKE
        p.color = Color.rgb(55, 46, 39)
        p.strokeWidth = 5f
        c.drawRoundRect(RectF(r.left + j(s, 1.5f), r.top + j(s + 1, 1.5f), r.right + j(s + 2, 1.5f), r.bottom + j(s + 3, 1.5f)), rad, rad, p)
    }

    fun text(c: Canvas, t: String, x: Float, y: Float, size: Float, col: Int = Color.rgb(45, 39, 34)) {
        p.style = Paint.Style.FILL
        p.color = col
        p.textSize = size
        p.typeface = Typeface.create("sans", Typeface.BOLD)
        p.textAlign = Paint.Align.CENTER
        c.drawText(t, x, y, p)
    }

    fun arrow(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, a: Int = 210) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = 13f
        p.color = Color.argb(a, 66, 143, 88)
        c.drawLine(x1, y1, x2, y2, p)
        val q = atan2(y2 - y1, x2 - x1)
        c.drawLine(x2, y2, x2 - cos(q - .7f) * 34, y2 - sin(q - .7f) * 34, p)
        c.drawLine(x2, y2, x2 - cos(q + .7f) * 34, y2 - sin(q + .7f) * 34, p)
    }
}
