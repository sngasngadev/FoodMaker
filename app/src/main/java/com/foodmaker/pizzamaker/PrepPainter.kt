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
        val bowlX = 540f
        val bowlY = 1155f
        val bowlH = 390f

        // Back of the bowl first.
        asset(c, assets.bowl, bowlX, bowlY, bowlH)

        // Every ingredient produces an immediate visible change inside the bowl.
        c.save()
        c.clipPath(Path().apply {
            addOval(RectF(340f, 1005f, 740f, 1245f), Path.Direction.CW)
        })

        if (g.mixAdded[0]) {
            val pulse = addedPulse(g.mixAddedAt[0])
            asset(c, assets.flourPile, 540f, 1125f, 165f * pulse, alpha = 245)
        }

        if (g.mixAdded[1]) {
            p.style = Paint.Style.FILL
            p.color = Color.argb(if (g.mixAdded[0]) 78 else 135, 104, 194, 225)
            c.drawOval(RectF(390f, 1090f, 690f, 1228f), p)
        }

        if (g.mixAdded[2]) {
            p.style = Paint.Style.FILL
            p.color = Color.argb(100, 244, 206, 64)
            c.drawOval(RectF(430f, 1100f, 680f, 1228f), p)
        }

        if (g.mixAdded.all { it }) {
            val t = g.stir.coerceIn(0f, 1f)
            val wetAlpha = (80 + 175 * t).toInt()
            val h = 135f + 95f * t
            asset(c, assets.doughBall, 540f, 1130f, h, alpha = wetAlpha)
            if (t > .35f) {
                p.style = Paint.Style.FILL
                p.color = Color.argb((75 * (1f - t)).toInt(), 255, 244, 219)
                c.drawOval(RectF(405f, 1080f, 675f, 1228f), p)
            }
        }
        c.restore()

        // Spoon belongs between the contents and the front wall of the bowl.
        if (g.mixAdded.all { it }) {
            val spoonX = if (g.toolHeld) g.touchX else 710f
            val spoonY = if (g.toolHeld) g.touchY else 1085f
            asset(c, assets.spoon, spoonX, spoonY, 275f, if (g.toolHeld) -28f else -22f)
        }

        // Front wall occludes food/tool and makes them read as being inside the bowl.
        f.bowlFront(c, bowlX, bowlY, bowlH)

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

        if (g.mixAdded.all { it }) {
            if (g.stir >= 1f) completeMark(c, 540f, 1470f)
            else if (!g.toolHeld) hint(c, 710f, 1090f, 555f, 1150f)
        } else if (SystemClock.uptimeMillis() - g.lastInput > 1100) {
            val next = g.mixAdded.indexOfFirst { !it }.coerceAtLeast(0)
            val x = floatArrayOf(230f, 540f, 850f)[next]
            hint(c, x, 835f, 500f, 1030f)
        }
    }

    private fun drawIngredient(c: Canvas, g: GameState, index: Int, b: Bitmap, x: Float, y: Float, h: Float, label: String) {
        if (!g.mixAdded[index] && !(g.dragIngredient == index && g.dragging)) {
            asset(c, b, x, y, h)
        } else if (g.mixAdded[index]) {
            a.circle(c, x, y, 46f, Color.argb(220, 240, 250, 221), 710 + index)
            a.text(c, "✓", x, y + 15f, 45f, Color.rgb(66, 145, 76))
        }
        a.text(c, label, x, 815f, 31f)
    }

    private fun roll(c: Canvas, g: GameState) {
        asset(c, assets.flourPile, 540f, 1135f, 285f, alpha = 62)

        val t = g.roll.coerceIn(0f, 1f)
        val ballFade = (255 * (1f - (t / .38f).coerceIn(0f, 1f))).toInt()
        if (ballFade > 0) {
            val w = 315f + 180f * (t / .38f).coerceIn(0f, 1f)
            val h = 315f - 105f * (t / .38f).coerceIn(0f, 1f)
            f.bitmap(c, assets.doughBall, 540f, 1120f, w, h, alpha = ballFade)
        }

        if (t > .10f) {
            val q = ((t - .10f) / .90f).coerceIn(0f, 1f)
            val w = 360f + 350f * q
            val h = 250f + 440f * q
            f.bitmap(c, assets.doughRound, 540f, 1120f, w, h, alpha = (80 + 175 * q).toInt())
        }

        asset(
            c, assets.rollingPin,
            if (g.toolHeld) g.touchX else 540f,
            if (g.toolHeld) g.touchY else 1420f,
            220f,
            if (g.toolHeld) ((g.touchX - g.prevX) * .18f).coerceIn(-12f, 12f) else -4f
        )

        if (g.roll >= .98f) completeMark(c, 540f, 1590f)
        else if (!g.toolHeld) hint(c, 350f, 1420f, 730f, 1420f)
    }

    private fun sauce(c: Canvas, g: GameState) {
        f.dough(c, 540f, 1050f, 335f)

        c.save()
        c.clipPath(Path().apply { addCircle(540f, 1050f, 282f, Path.Direction.CW) })
        f.maskedSauce(c, 540f, 1050f, 696f, g.sauceMarks, 66f)
        c.restore()

        asset(c, assets.sauceBowl, 225f, 1580f, 205f)
        a.text(c, "토마토 소스", 230f, 1730f, 30f)
        asset(c, assets.sauceLadle, if (g.toolHeld) g.touchX else 765f, if (g.toolHeld) g.touchY else 1540f, 265f, -18f)

        if (g.sauce >= .92f) completeMark(c, 540f, 1535f)
        else if (g.sauceMarks.isEmpty()) hint(c, 765f, 1450f, 620f, 1220f)
    }

    private fun cheese(c: Canvas, g: GameState) {
        f.pizza(c, 540f, 1040f, 340f, 1f, 0f)

        c.save()
        c.clipPath(Path().apply { addCircle(540f, 1040f, 282f, Path.Direction.CW) })
        g.cheeseBits.takeLast(180).forEachIndexed { i, bit ->
            val h = 27f + (i % 4) * 3f
            asset(c, assets.shreddedCheese, bit.x, bit.y, h, bit.rotation, 245)
        }
        c.restore()

        asset(c, assets.shreddedCheese, 235f, 1580f, 170f)
        asset(c, assets.cheeseShaker, if (g.toolHeld) g.touchX else 790f, if (g.toolHeld) g.touchY else 1530f, 230f, if (g.toolHeld) -15f else 9f)

        if (g.cheese >= .90f) completeMark(c, 540f, 1530f)
        else if (g.cheeseBits.isEmpty()) hint(c, 790f, 1455f, 650f, 1220f)
    }

    private fun addedPulse(time: Long): Float {
        if (time <= 0L) return 1f
        val age = (SystemClock.uptimeMillis() - time).coerceAtLeast(0L)
        if (age >= 420L) return 1f
        return 1f + .12f * sin(age / 420f * Math.PI).toFloat()
    }

    private fun completeMark(c: Canvas, x: Float, y: Float) {
        val q = (1f + .04f * sin(SystemClock.uptimeMillis() / 120.0)).toFloat()
        c.save()
        c.scale(q, q, x, y)
        a.circle(c, x, y, 49f, Color.rgb(236, 249, 216), 800)
        a.text(c, "✓", x, y + 16f, 50f, Color.rgb(62, 151, 72))
        c.restore()
    }

    private fun hint(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) {
        val q = (.55 + .45 * ((sin(SystemClock.uptimeMillis() / 220.0) + 1) / 2)).toFloat()
        a.arrow(c, x1, y1, x2, y2, (120 + 120 * q).toInt())
    }
}
