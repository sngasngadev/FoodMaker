package com.foodmaker.pizzamaker

import android.graphics.*

class PizzaPainter(private val assets: AssetBook) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }

    fun bitmap(
        c: Canvas,
        b: Bitmap,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        a: Float = 0f,
        alpha: Int = 255,
        colorFilter: ColorFilter? = null
    ) {
        c.save()
        c.rotate(a, x, y)
        p.alpha = alpha
        p.xfermode = null
        p.colorFilter = colorFilter
        c.drawBitmap(b, null, RectF(x - w / 2, y - h / 2, x + w / 2, y + h / 2), p)
        p.colorFilter = null
        p.alpha = 255
        c.restore()
    }

    fun bitmapRegion(
        c: Canvas,
        b: Bitmap,
        src: Rect,
        dst: RectF,
        alpha: Int = 255
    ) {
        p.alpha = alpha
        p.xfermode = null
        p.colorFilter = null
        c.drawBitmap(b, src, dst, p)
        p.alpha = 255
    }

    fun background(c: Canvas) = bitmap(c, assets.background, 540f, 960f, 1080f, 1920f)

    fun workSurface(c: Canvas) {
        val b = assets.background
        val src = Rect(
            (b.width * .11f).toInt(),
            (b.height * .54f).toInt(),
            (b.width * .89f).toInt(),
            (b.height * .91f).toInt()
        )
        bitmapRegion(c, b, src, RectF(0f, 430f, 1080f, 1920f))
    }

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

    fun topping(
        c: Canvas,
        t: ToppingPiece,
        x: Float,
        y: Float,
        r: Float,
        bakedLook: Boolean = false
    ) {
        val px = x + t.nx * r * 1.55f
        val py = y + t.ny * r * 1.55f
        val base = (
            if (t.type == ToppingType.ONION) 104f
            else if (t.type == ToppingType.MUSHROOM) 104f
            else 88f
        ) * t.scale * (r / 340f)

        val b = assets.toppings.getValue(t.type)
        val aspect = b.width.toFloat() / b.height.coerceAtLeast(1)
        val warmFilter = if (bakedLook) bakedToppingFilter else null
        bitmap(
            c,
            b,
            px,
            py,
            base * aspect.coerceIn(.65f, 1.55f),
            base,
            t.rotation,
            colorFilter = warmFilter
        )
    }

    fun toppingChoice(c: Canvas, type: ToppingType, x: Float, y: Float, h: Float) {
        val b = assets.toppingGroups.getValue(type)
        val aspect = b.width.toFloat() / b.height.coerceAtLeast(1)
        bitmap(c, b, x, y, h * aspect.coerceIn(.75f, 1.45f), h)
    }

    /**
     * Freeze the whole pizza into one bitmap after topping placement.
     * From this point the exact same composition is reused through oven, cutting,
     * eating and completion instead of reconstructing it at each stage.
     */
    fun composePizza(toppings: List<ToppingPiece>, isBaked: Boolean, size: Int = 768): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val center = size / 2f
        val radius = size / 2.13f

        if (baked) baked(canvas, center, center, radius)
        else pizza(canvas, center, center, radius, 1f, 1f)

        toppings.forEach { topping(canvas, it, center, center, radius, bakedLook = baked) }

        if (baked) {
            val glaze = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(20, 230, 132, 38)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(center, center, radius * .82f, glaze)
        }

        return out
    }

    fun bowlFront(c: Canvas, x: Float, y: Float, h: Float) {
        val b = assets.bowl
        val w = h * b.width.toFloat() / b.height.coerceAtLeast(1)
        val srcTopRatio = .44f
        val srcTop = (b.height * srcTopRatio).toInt()
        val src = Rect(0, srcTop, b.width, b.height)
        val top = y - h / 2 + h * srcTopRatio
        bitmapRegion(c, b, src, RectF(x - w / 2, top, x + w / 2, y + h / 2))
    }

    fun ovenFront(c: Canvas, x: Float, y: Float, h: Float) {
        val b = assets.ovenOpen
        val w = h * b.width.toFloat() / b.height.coerceAtLeast(1)
        val srcTopRatio = .56f
        val srcTop = (b.height * srcTopRatio).toInt()
        val src = Rect(0, srcTop, b.width, b.height)
        val top = y - h / 2 + h * srcTopRatio
        bitmapRegion(c, b, src, RectF(x - w / 2, top, x + w / 2, y + h / 2))
    }

    fun maskedSauce(
        c: Canvas,
        x: Float,
        y: Float,
        size: Float,
        marks: List<PointF>,
        brushRadius: Float = 64f
    ) {
        if (marks.isEmpty()) return
        val bounds = RectF(x - size / 2, y - size / 2, x + size / 2, y + size / 2)
        val save = c.saveLayer(bounds, null)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        marks.forEach { point ->
            c.drawCircle(point.x, point.y, brushRadius, maskPaint)
        }

        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        c.drawBitmap(assets.pizzaSauce, null, bounds, imagePaint)
        imagePaint.xfermode = null
        c.restoreToCount(save)
    }

    private val bakedToppingFilter: ColorFilter by lazy {
        val m = ColorMatrix(
            floatArrayOf(
                1.05f, 0f, 0f, 0f, 8f,
                0f, .92f, 0f, 0f, 0f,
                0f, 0f, .84f, 0f, -4f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        ColorMatrixColorFilter(m)
    }
}
