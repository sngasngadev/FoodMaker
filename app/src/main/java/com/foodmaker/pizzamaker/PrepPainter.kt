package com.foodmaker.pizzamaker

import android.graphics.*
import android.os.SystemClock
import kotlin.math.sin

class PrepPainter(
    private val a: CrayonArt,
    private val f: PizzaPainter,
    private val assets: AssetBook
) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(c: Canvas, g: GameState) {
        when (g.stage) {
            Stage.WELCOME -> welcome(c)
            Stage.MIX -> mix(c, g)
            Stage.ROLL -> roll(c, g)
            Stage.SAUCE -> sauce(c, g)
            Stage.CHEESE -> cheese(c, g)
            else -> Unit
        }
    }

    private fun asset(c: Canvas, b: Bitmap, x: Float, y: Float, h: Float, rotation: Float = 0f, alpha: Int = 255) {
        val w = h * b.width.toFloat() / b.height.coerceAtLeast(1)
        f.bitmap(c, b, x, y, w, h, rotation, alpha)
    }

    private fun welcome(c: Canvas) {
        a.box(c, RectF(90f, 165f, 990f, 590f), 55f, Color.argb(235, 255, 247, 222), 300)
        a.text(c, "피자 메이커", 540f, 305f, 82f, Color.rgb(231, 74, 47))
        a.text(c, "반죽부터 한 입까지 직접 만들어봐", 540f, 390f, 35f)

        f.pizza(c, 540f, 875f, 330f)
        listOf(
            ToppingPiece(ToppingType.PEPPERONI, -.28f, -.18f, -12f, 1.15f),
            ToppingPiece(ToppingType.MUSHROOM, .24f, -.22f, 15f, 1f),
            ToppingPiece(ToppingType.PEPPER, -.08f, .24f, 18f, .95f),
            ToppingPiece(ToppingType.OLIVE, .3f, .18f, 0f, 1f),
            ToppingPiece(ToppingType.ONION, -.33f, .18f, -18f, 1f)
        ).forEach { f.topping(c, it, 540f, 875f, 330f) }

        val q = (sin(SystemClock.uptimeMillis() / 280.0) * .04 + 1).toFloat()
        c.save()
        c.scale(q, q, 540f, 1455f)
        a.box(c, RectF(260f, 1375f, 820f, 1535f), 65f, Color.rgb(250, 196, 76), 310)
        a.text(c, "만들기 시작", 540f, 1476f, 51f)
        c.restore()
        a.text(c, "손가락으로 직접 요리해요", 540f, 1655f, 31f, Color.rgb(92, 77, 62))
    }

    private fun mix(c: Canvas, g: GameState) {
        asset(c, assets.bowl, 540f, 1160f, 360f)

        if (g.mixAdded.all { it }) {
            asset(c, assets.doughBall, 540f, 1125f, 210f + g.stir * 45f)
            asset(c, assets.spoon, if (g.toolHeld) g.touchX else 715f, if (g.toolHeld) g.touchY else 1080f, 270f, -22f)
            if (g.stir >= 1f) hint(c, 540f, 1430f, 540f, 1580f)
            return
        }

        drawIngredient(c, g, 0, assets.flourBag, 230f, 640f, 245f, "밀가루")
        drawIngredient(c, g, 1, assets.waterCup, 540f, 640f, 225f, "물")
        drawIngredient(c, g, 2, assets.oilBottle, 850f, 640f, 235f, "오일")

        if (g.dragIngredient >= 0 && g.dragging) {
            val b = when (g.dragIngredient) {
                0 -> assets.flourBag
                1 -> assets.waterCup
                else -> assets.oilBottle
            }
            asset(c, b, g.touchX, g.touchY, 230f, if (g.dragIngredient == 2) -10f else 0f)
        }

        if (SystemClock.uptimeMillis() - g.lastInput > 1500) hint(c, 230f, 850f, 470f, 1030f)
    }

    private fun drawIngredient(c: Canvas, g: GameState, index: Int, b: Bitmap, x: Float, y: Float, h: Float, label: String) {
        if (!g.mixAdded[index] && !(g.dragIngredient == index && g.dragging)) {
            asset(c, b, x, y, h)
        }
        a.text(c, label, x, 815f, 31f)
    }

    private fun roll(c: Canvas, g: GameState) {
        if (g.roll < .12f) {
            asset(c, assets.doughBall, 540f, 1120f, 330f)
        } else {
            f.dough(c, 540f, 1120f, 215f + g.roll * 120f)
        }
        asset(
            c, assets.rollingPin,
            if (g.toolHeld) g.touchX else 540f,
            if (g.toolHeld) g.touchY else 1420f,
            220f,
            if (g.toolHeld) ((g.touchX - g.prevX) * .18f).coerceIn(-12f, 12f) else -4f
        )
        if (g.roll > .92f) hint(c, 540f, 1545f, 540f, 1675f)
    }

    private fun sauce(c: Canvas, g: GameState) {
        f.dough(c, 540f, 1050f, 335f)
        if (g.sauce > .08f) {
            f.bitmap(c, assets.pizzaSauce, 540f, 1050f, 697f, 697f, alpha = (g.sauce * 210).toInt().coerceIn(25, 210))
        }

        c.save()
        c.clipPath(Path().apply { addCircle(540f, 1050f, 285f, Path.Direction.CW) })
        p.style = Paint.Style.FILL
        g.sauceMarks.forEachIndexed { i, m ->
            p.color = if (i % 3 == 0) Color.argb(115, 239, 66, 43) else Color.argb(95, 224, 53, 39)
            c.drawCircle(m.x, m.y, 48f + (i % 4) * 3, p)
        }
        c.restore()

        asset(c, assets.sauceBowl, 225f, 1580f, 205f)
        a.text(c, "토마토 소스", 230f, 1730f, 30f)
        asset(c, assets.sauceLadle, if (g.toolHeld) g.touchX else 765f, if (g.toolHeld) g.touchY else 1540f, 265f, -18f)
        if (g.sauce > .82f) hint(c, 540f, 1460f, 540f, 1600f)
    }

    private fun cheese(c: Canvas, g: GameState) {
        f.pizza(c, 540f, 1040f, 340f, 1f, 0f)
        if (g.cheese > .06f) {
            f.bitmap(c, assets.pizzaCheese, 540f, 1040f, 707f, 707f, alpha = (g.cheese * 230).toInt().coerceIn(30, 230))
        }
        asset(c, assets.shreddedCheese, 235f, 1580f, 170f)
        asset(c, assets.cheeseShaker, if (g.toolHeld) g.touchX else 790f, if (g.toolHeld) g.touchY else 1530f, 230f, if (g.toolHeld) -15f else 9f)
        if (g.cheese > .78f) hint(c, 540f, 1450f, 540f, 1600f)
    }

    private fun hint(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) {
        val q = (.55 + .45 * ((sin(SystemClock.uptimeMillis() / 220.0) + 1) / 2)).toFloat()
        a.arrow(c, x1, y1, x2, y2, (120 + 120 * q).toInt())
    }
}
