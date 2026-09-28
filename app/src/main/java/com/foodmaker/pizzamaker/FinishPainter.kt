package com.foodmaker.pizzamaker

import android.graphics.*
import android.os.SystemClock
import kotlin.math.*

class FinishPainter(
    private val a: CrayonArt,
    private val f: PizzaPainter,
    private val assets: AssetBook,
    private val onBakeDone: () -> Unit
) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(c: Canvas, g: GameState) {
        when (g.stage) {
            Stage.TOPPINGS -> toppings(c, g)
            Stage.OVEN -> oven(c, g)
            Stage.CUT -> cut(c, g)
            Stage.EAT -> eat(c, g)
            Stage.DONE -> done(c, g)
            else -> Unit
        }
    }

    private fun asset(c: Canvas, b: Bitmap, x: Float, y: Float, h: Float, rotation: Float = 0f, alpha: Int = 255) {
        val w = h * b.width.toFloat() / b.height.coerceAtLeast(1)
        f.bitmap(c, b, x, y, w, h, rotation, alpha)
    }

    private fun toppings(c: Canvas, g: GameState) {
        f.pizza(c, 540f, 830f, 350f)
        g.toppings.forEach { f.topping(c, it, 540f, 830f, 350f) }

        val ts = ToppingType.values()
        ts.forEachIndexed { i, t ->
            val x = 126f + i * 207
            a.circle(c, x, 1395f, 83f, if (g.selected == t) Color.rgb(255, 232, 137) else Color.rgb(255, 247, 221), 390 + i)
            val b = assets.toppings.getValue(t)
            val h = 104f
            val w = h * b.width.toFloat() / b.height.coerceAtLeast(1)
            f.bitmap(c, b, x, 1395f, w.coerceAtMost(145f), h)
        }

        arrayOf("페퍼로니", "버섯", "피망", "올리브", "양파").forEachIndexed { i, s ->
            a.text(c, s, 126f + i * 207, 1515f, 22f)
        }

        g.selected?.let {
            if (g.dragging) {
                val b = assets.toppings.getValue(it)
                val h = 125f
                val w = h * b.width.toFloat() / b.height.coerceAtLeast(1)
                f.bitmap(c, b, g.touchX, g.touchY, w.coerceAtMost(175f), h)
            }
        }

        if (g.toppings.size >= 4) {
            a.box(c, RectF(330f, 1610f, 750f, 1740f), 52f, Color.rgb(245, 190, 70), 410)
            a.text(c, "오븐으로!", 540f, 1691f, 42f)
        }
    }

    private fun oven(c: Canvas, g: GameState) {
        val now = SystemClock.uptimeMillis()
        val q = if (g.baking) ((now - g.bakeStart) / 4200f).coerceIn(0f, 1f) else 0f
        if (g.baking && q >= 1 && !g.bakeDone) {
            g.bakeDone = true
            onBakeDone()
        }

        val ovenImage = if (!g.baking || g.bakeDone) assets.ovenOpen else assets.ovenClosed
        asset(c, ovenImage, 540f, 665f, if (g.baking && !g.bakeDone) 760f else 790f)

        if (!g.baking) {
            f.pizza(c, g.ovenX, g.ovenY, 285f)
            g.toppings.forEach { f.topping(c, it, g.ovenX, g.ovenY, 285f) }
            if (!g.pizzaHeld && now - g.lastInput > 1200) hint(c, 540f, 1280f, 540f, 900f)
        } else {
            if (g.bakeDone) {
                f.baked(c, 540f, 760f, 220f)
                g.toppings.forEach { f.topping(c, it, 540f, 760f, 220f) }
            }
            a.box(c, RectF(285f, 1200f, 795f, 1310f), 42f, Color.rgb(255, 245, 214), 430)
            p.style = Paint.Style.FILL
            p.color = Color.rgb(231, 178, 64)
            c.drawRoundRect(RectF(315f, 1230f, 315f + 450f * q, 1280f), 25f, 25f, p)
            if (g.bakeDone) {
                a.text(c, "띵! 잘 구워졌어", 540f, 1420f, 42f)
                hint(c, 540f, 1470f, 540f, 1620f)
            }
        }
    }

    private fun cut(c: Canvas, g: GameState) {
        asset(c, assets.plate, 540f, 960f, 810f)
        if (g.cutCount >= 3) {
            f.bitmap(c, assets.pizzaCut6, 540f, 960f, 700f, 700f)
        } else {
            f.baked(c, 540f, 960f, 340f)
            g.toppings.forEach { f.topping(c, it, 540f, 960f, 340f) }
        }

        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f
        p.pathEffect = DashPathEffect(floatArrayOf(17f, 15f), 0f)
        p.color = Color.argb(95, 76, 65, 55)
        if (g.cutCount < 3) {
            floatArrayOf(0f, 60f, 120f).forEach { angle ->
                val r = Math.toRadians(angle.toDouble())
                val dx = cos(r).toFloat() * 300
                val dy = sin(r).toFloat() * 300
                c.drawLine(540 - dx, 960 - dy, 540 + dx, 960 + dy, p)
            }
        }
        p.pathEffect = null

        asset(c, assets.pizzaCutter, g.cutterX, g.cutterY, 180f, g.cutterRot)
        if (g.cutCount >= 3) hint(c, 540f, 1460f, 540f, 1605f)
    }

    private fun eat(c: Canvas, g: GameState) {
        asset(c, assets.plate, 540f, 960f, 820f)
        repeat(6) { i ->
            if (!g.eaten[i]) {
                val a0 = -90f + i * 60
                c.save()
                c.clipPath(Path().apply {
                    moveTo(540f, 960f)
                    arcTo(RectF(210f, 630f, 870f, 1290f), a0, 60f)
                    close()
                })
                f.bitmap(c, assets.pizzaCut6Spread, 540f, 960f, 700f, 700f)
                c.restore()
            }
        }

        val left = g.eaten.count { !it }
        a.text(c, if (left > 0) "피자 조각을 톡톡 눌러봐" else "냠! 다 먹었다!", 540f, 1470f, 39f)
        if (left == 0) {
            a.box(c, RectF(335f, 1570f, 745f, 1700f), 50f, Color.rgb(246, 194, 72), 510)
            a.text(c, "짜잔!", 540f, 1650f, 44f)
        }
    }

    private fun done(c: Canvas, g: GameState) {
        val t = (SystemClock.uptimeMillis() - g.stageStart) / 1000f
        if (assets.effects.isNotEmpty()) {
            repeat(18) { i ->
                val b = assets.effects[i % assets.effects.size]
                val x = ((i * 173 + (t * 55).toInt()) % 1080).toFloat()
                val y = ((i * 251 + (t * 210).toInt()) % 1900).toFloat()
                asset(c, b, x, y, 85f + (i % 4) * 16f, i * 19f + t * 45f, 230)
            }
        }

        a.box(c, RectF(100f, 350f, 980f, 1130f), 70f, Color.argb(238, 255, 248, 223), 530)
        a.text(c, "피자 완성!", 540f, 535f, 83f, Color.rgb(231, 75, 48))
        a.text(c, "처음부터 끝까지 직접 만들었어", 540f, 630f, 36f)
        f.bitmap(c, assets.pizzaTopped, 540f, 865f, 470f, 470f)
        a.box(c, RectF(285f, 1285f, 795f, 1435f), 60f, Color.rgb(245, 190, 70), 550)
        a.text(c, "한 판 더 만들기", 540f, 1378f, 43f)
    }

    private fun hint(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) {
        val q = (.55 + .45 * ((sin(SystemClock.uptimeMillis() / 220.0) + 1) / 2)).toFloat()
        a.arrow(c, x1, y1, x2, y2, (120 + 120 * q).toInt())
    }
}
