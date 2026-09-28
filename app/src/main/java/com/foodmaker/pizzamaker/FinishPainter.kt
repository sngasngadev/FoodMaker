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
        drawPlayerPizza(c, g, 540f, 830f, 350f, baked = false)

        val ts = ToppingType.values()
        ts.forEachIndexed { i, t ->
            val x = 126f + i * 207
            a.circle(
                c, x, 1395f, 83f,
                if (g.selected == t) Color.rgb(255, 232, 137) else Color.rgb(255, 247, 221),
                390 + i
            )
            f.toppingChoice(c, t, x, 1395f, 102f)
        }

        arrayOf("페퍼로니", "버섯", "피망", "올리브", "양파").forEachIndexed { i, s ->
            a.text(c, s, 126f + i * 207, 1515f, 22f)
        }

        g.selected?.let { type ->
            if (g.dragging) {
                val b = assets.toppings.getValue(type)
                val h = if (type == ToppingType.ONION) 105f else 98f
                val w = h * b.width.toFloat() / b.height.coerceAtLeast(1)
                f.bitmap(c, b, g.touchX, g.touchY, w, h)
            }
        }

        if (g.toppings.size >= 4) {
            a.box(c, RectF(330f, 1610f, 750f, 1740f), 52f, Color.rgb(245, 190, 70), 410)
            a.text(c, "오븐으로!", 540f, 1691f, 42f)
        } else {
            a.text(c, "토핑을 ${4 - g.toppings.size}개만 더 올려봐", 540f, 1685f, 29f, Color.rgb(88, 71, 54))
        }
    }

    private fun oven(c: Canvas, g: GameState) {
        val now = SystemClock.uptimeMillis()
        val elapsed = if (g.baking) now - g.bakeStart else 0L
        val q = if (g.baking) (elapsed / 4200f).coerceIn(0f, 1f) else 0f

        if (g.baking && q >= 1f && !g.bakeDone) {
            g.bakeDone = true
            onBakeDone()
        }

        when {
            !g.baking -> {
                drawOpenOven(c)
                val nearOpening = g.ovenY < 1040f
                if (nearOpening) {
                    drawPizzaInOven(c, g, baked = false, x = g.ovenX, y = g.ovenY, r = 245f)
                } else {
                    drawPlayerPizza(c, g, g.ovenX, g.ovenY, 285f, baked = false)
                }
                if (!g.pizzaHeld && now - g.lastInput > 1000L) {
                    hint(c, 540f, 1260f, 540f, 905f)
                }
            }

            g.bakeDone -> {
                drawOpenOven(c)
                drawPizzaInOven(c, g, baked = true, x = 540f, y = 755f, r = 225f)
            }

            elapsed < 320L -> {
                drawOpenOven(c)
                drawPizzaInOven(c, g, baked = false, x = 540f, y = 755f, r = 225f)
            }

            else -> {
                asset(c, assets.ovenClosed, 540f, 665f, 790f)
            }
        }

        if (g.baking) {
            a.box(c, RectF(285f, 1200f, 795f, 1310f), 42f, Color.rgb(255, 245, 214), 430)
            p.style = Paint.Style.FILL
            p.color = Color.rgb(231, 178, 64)
            c.drawRoundRect(RectF(315f, 1230f, 315f + 450f * q, 1280f), 25f, 25f, p)
        }

        if (g.bakeDone) {
            a.text(c, "띵! 내가 만든 피자가 그대로 구워졌어", 540f, 1405f, 34f)
            a.box(c, RectF(330f, 1460f, 750f, 1605f), 52f, Color.rgb(245, 190, 70), 431)
            a.text(c, "피자 꺼내기", 540f, 1550f, 40f)
        }
    }

    private fun drawOpenOven(c: Canvas) {
        asset(c, assets.ovenOpen, 540f, 665f, 790f)
    }

    private fun drawPizzaInOven(
        c: Canvas,
        g: GameState,
        baked: Boolean,
        x: Float,
        y: Float,
        r: Float
    ) {
        c.save()
        c.clipRect(RectF(302f, 485f, 780f, 855f))
        drawPlayerPizza(c, g, x, y, r, baked)
        c.restore()
        f.ovenFront(c, 540f, 665f, 790f)
    }

    private fun cut(c: Canvas, g: GameState) {
        asset(c, assets.plate, 540f, 960f, 810f)
        drawPlayerPizza(c, g, 540f, 960f, 340f, baked = true)

        p.pathEffect = null
        g.cutAngles.forEachIndexed { i, angle ->
            val r = Math.toRadians(angle.toDouble())
            val dx = cos(r).toFloat() * 315f
            val dy = sin(r).toFloat() * 315f
            a.line(
                c,
                540f - dx, 960f - dy,
                540f + dx, 960f + dy,
                Color.rgb(108, 70, 43),
                8f,
                470 + i
            )
        }

        if (g.cutCount < 3) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 4f
            p.pathEffect = DashPathEffect(floatArrayOf(16f, 16f), 0f)
            p.color = Color.argb(72, 76, 65, 55)
            floatArrayOf(0f, 60f, 120f).forEach { angle ->
                if (g.cutAngles.none { angularDiff(it, angle) < 22f }) {
                    val r = Math.toRadians(angle.toDouble())
                    val dx = cos(r).toFloat() * 300f
                    val dy = sin(r).toFloat() * 300f
                    c.drawLine(540f - dx, 960f - dy, 540f + dx, 960f + dy, p)
                }
            }
            p.pathEffect = null
            asset(c, assets.pizzaCutter, g.cutterX, g.cutterY, 180f, g.cutterRot)
        } else {
            completeMark(c, 540f, 1490f)
        }
    }

    private fun eat(c: Canvas, g: GameState) {
        asset(c, assets.plate, 540f, 960f, 820f)

        repeat(6) { i ->
            if (!g.eaten[i]) {
                val start = -90f + i * 60f
                val centerAngle = Math.toRadians((start + 30f).toDouble())
                val offsetX = cos(centerAngle).toFloat() * 8f
                val offsetY = sin(centerAngle).toFloat() * 8f

                c.save()
                c.translate(offsetX, offsetY)
                c.clipPath(Path().apply {
                    moveTo(540f, 960f)
                    arcTo(RectF(200f, 620f, 880f, 1300f), start, 60f)
                    close()
                })
                drawPlayerPizza(c, g, 540f, 960f, 340f, baked = true)
                c.restore()
            }
        }

        floatArrayOf(0f, 60f, 120f).forEachIndexed { i, angle ->
            val r = Math.toRadians(angle.toDouble())
            val dx = cos(r).toFloat() * 315f
            val dy = sin(r).toFloat() * 315f
            a.line(
                c,
                540f - dx, 960f - dy,
                540f + dx, 960f + dy,
                Color.rgb(108, 70, 43),
                6f,
                520 + i
            )
        }

        val left = g.eaten.count { !it }
        a.text(
            c,
            if (left > 0) "내가 만든 피자를 한 조각씩 먹어봐" else "냠! 다 먹었다!",
            540f,
            1470f,
            37f
        )
        if (left == 0) completeMark(c, 540f, 1585f)
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

        a.box(c, RectF(100f, 300f, 980f, 1180f), 70f, Color.argb(238, 255, 248, 223), 530)
        a.text(c, "피자 완성!", 540f, 470f, 78f, Color.rgb(231, 75, 48))
        a.text(c, "처음부터 끝까지 내가 만든 피자야", 540f, 555f, 34f)

        asset(c, assets.plate, 540f, 860f, 590f)
        drawPlayerPizza(c, g, 540f, 860f, 245f, baked = true)
        floatArrayOf(0f, 60f, 120f).forEachIndexed { i, angle ->
            val r = Math.toRadians(angle.toDouble())
            val dx = cos(r).toFloat() * 225f
            val dy = sin(r).toFloat() * 225f
            a.line(
                c,
                540f - dx, 860f - dy,
                540f + dx, 860f + dy,
                Color.rgb(108, 70, 43),
                5f,
                570 + i
            )
        }

        a.box(c, RectF(285f, 1285f, 795f, 1435f), 60f, Color.rgb(245, 190, 70), 550)
        a.text(c, "한 판 더 만들기", 540f, 1378f, 43f)
    }

    private fun drawPlayerPizza(
        c: Canvas,
        g: GameState,
        x: Float,
        y: Float,
        r: Float,
        baked: Boolean
    ) {
        if (baked) f.baked(c, x, y, r)
        else f.pizza(c, x, y, r, 1f, 1f)
        g.toppings.forEach { f.topping(c, it, x, y, r) }
    }

    private fun completeMark(c: Canvas, x: Float, y: Float) {
        val q = (1f + .04f * sin(SystemClock.uptimeMillis() / 120.0)).toFloat()
        c.save()
        c.scale(q, q, x, y)
        a.circle(c, x, y, 49f, Color.rgb(236, 249, 216), 800)
        a.text(c, "✓", x, y + 16f, 50f, Color.rgb(62, 151, 72))
        c.restore()
    }

    private fun angularDiff(a1: Float, a2: Float): Float {
        var q = abs(a1 - a2) % 180f
        if (q > 90f) q = 180f - q
        return q
    }

    private fun hint(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) {
        val q = (.55 + .45 * ((sin(SystemClock.uptimeMillis() / 220.0) + 1) / 2)).toFloat()
        a.arrow(c, x1, y1, x2, y2, (120 + 120 * q).toInt())
    }
}
