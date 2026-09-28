package com.foodmaker.pizzamaker

import android.graphics.*

class PizzaPainter(private val assets: AssetBook) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }

    fun bitmap(c: Canvas, b: Bitmap, x: Float, y: Float, w: Float, h: Float, a: Float = 0f, alpha: Int = 255) {
        c.save()
        c.rotate(a, x, y)
        p.alpha = alpha
        c.drawBitmap(b, null, RectF(x - w / 2, y - h / 2, x + w / 2, y + h / 2), p)
        p.alpha = 255
        c.restore()
    }

    fun background(c: Canvas) = bitmap(c, assets.background, 540f, 960f, 1080f, 1920f)

    fun dough(c: Canvas, x: Float, y: Float, r: Float) =
        bitmap(c, assets.pizzaDough, x, y, r * 2.08f, r * 2.08f)

    fun pizza(c: Canvas, x: Float, y: Float, r: Float, sauce: Float = 1f, cheese: Float = 1f) {
        val image = when {
            sauce <= .05f -> assets.pizzaDough
            cheese <= .05f -> assets.pizzaSauce
            else -> assets.pizzaCheese
        }
        bitmap(c, image, x, y, r * 2.08f, r * 2.08f)
    }

    fun baked(c: Canvas, x: Float, y: Float, r: Float) =
        bitmap(c, assets.pizzaBaked, x, y, r * 2.08f, r * 2.08f)

    fun topping(c: Canvas, t: ToppingPiece, x: Float, y: Float, r: Float) {
        val px = x + t.nx * r * 1.55f
        val py = y + t.ny * r * 1.55f
        val base = (if (t.type == ToppingType.ONION) 120f else if (t.type == ToppingType.MUSHROOM) 112f else 98f) * t.scale * (r / 340f)
        val b = assets.toppings.getValue(t.type)
        val aspect = b.width.toFloat() / b.height.coerceAtLeast(1)
        bitmap(c, b, px, py, base * aspect.coerceIn(.72f, 1.45f), base, t.rotation)
    }
}
