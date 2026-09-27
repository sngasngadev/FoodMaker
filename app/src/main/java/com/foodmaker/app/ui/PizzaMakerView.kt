package com.foodmaker.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.foodmaker.app.model.PartDefinition
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class PizzaMakerView(
    context: Context,
    private val parts: Map<String, PartDefinition>,
    private val onBack: () -> Unit,
    private val onPrint: () -> Unit
) : View(context) {

    private enum class Phase {
        DOUGH, ROLL, SAUCE, CHEESE, TOPPING, OVEN, CUT, DONE
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = Phase.DOUGH

    private var active = false
    private var dragType: String? = null
    private var dragX = 0f
    private var dragY = 0f
    private var lastX = 0f
    private var lastY = 0f

    // Dough
    private var doughStep = 0 // flour, water, stir, knead
    private var pourStart = 0L
    private var stirAngle = 0.0
    private var stirTravel = 0.0
    private var kneadTravel = 0f
    private var kneadPulse = 0f

    // Rolling
    private var rollerY = 0f
    private var rollProgress = 0f

    // Sauce
    private var spoonLoaded = 0f
    private val saucePoints = mutableListOf<PointF>()
    private val sauceCells = mutableSetOf<Int>()

    // Cheese
    private var cheeseY = 0f
    private var cheeseTravel = 0f
    private val cheesePoints = mutableListOf<PointF>()

    // Toppings
    private val pepperoniPoints = mutableListOf<PointF>()

    // Oven
    private var ovenStep = 0 // in, bake, out
    private var bakeStart = 0L
    private var pizzaDragX = 0f
    private var pizzaDragY = 0f
    private var baked = false

    // Cutting
    private var cutterX = 0f
    private var cutterY = 0f
    private var cutEntry: PointF? = null
    private var cutLastInside: PointF? = null
    private val cutAngles = mutableListOf<Float>()

    private val pizzaCx: Float get() = width * 0.50f
    private val pizzaCy: Float get() = height * 0.50f
    private val pizzaRadius: Float get() = width * 0.285f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(255, 248, 234))
        drawHeader(canvas)

        when (phase) {
            Phase.DOUGH -> drawDough(canvas)
            Phase.ROLL -> drawRoll(canvas)
            Phase.SAUCE -> drawSauce(canvas)
            Phase.CHEESE -> drawCheese(canvas)
            Phase.TOPPING -> drawTopping(canvas)
            Phase.OVEN -> drawOven(canvas)
            Phase.CUT -> drawCut(canvas)
            Phase.DONE -> drawDone(canvas)
        }

        drawSmallHint(canvas)
    }

    private fun drawHeader(canvas: Canvas) {
        paint.color = Color.rgb(67, 53, 43)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = width * 0.055f
        paint.isFakeBoldText = true
        canvas.drawText("‹", width * 0.045f, height * 0.070f, paint)
        canvas.drawText("피자", width * 0.115f, height * 0.070f, paint)
        paint.isFakeBoldText = false
    }

    private fun boardRect(): RectF =
        RectF(width * 0.055f, height * 0.15f, width * 0.945f, height * 0.84f)

    private fun drawBoard(canvas: Canvas) {
        val r = boardRect()
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(225, 183, 119)
        canvas.drawRoundRect(r, width * 0.035f, width * 0.035f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width * 0.008f
        paint.color = Color.rgb(171, 124, 72)
        canvas.drawRoundRect(r, width * 0.035f, width * 0.035f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun hintText(): String = when (phase) {
        Phase.DOUGH -> when (doughStep) {
            0 -> "밀가루를 그릇 위로 가져가 봐"
            1 -> "이번엔 물을 부어 봐"
            2 -> "숟가락으로 빙글빙글"
            else -> "반죽을 손가락으로 꾹꾹 치대 봐"
        }
        Phase.ROLL -> "밀대를 위아래로 굴려 봐"
        Phase.SAUCE -> if (spoonLoaded > 0.05f) "도우 위에 소스를 펴 발라 봐" else "숟가락에 소스를 떠 봐"
        Phase.CHEESE -> "치즈를 강판에 위아래로 문질러 봐"
        Phase.TOPPING -> "페퍼로니를 원하는 곳에 올려 봐"
        Phase.OVEN -> when (ovenStep) {
            0 -> "피자를 오븐 안으로 넣어 봐"
            1 -> "굽는 중..."
            else -> "구운 피자를 꺼내 쟁반에 놓아 봐"
        }
        Phase.CUT -> if (cutAngles.size < 3) "커터를 잡고 피자 끝에서 끝까지 잘라 봐" else "완성!"
        Phase.DONE -> ""
    }

    private fun drawSmallHint(canvas: Canvas) {
        if (phase == Phase.DONE) return
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = width * 0.031f
        paint.color = Color.rgb(116, 93, 76)
        canvas.drawText(hintText(), width / 2f, height * 0.91f, paint)
    }

    private fun drawDough(canvas: Canvas) {
        drawBoard(canvas)
        val bowl = RectF(width * 0.26f, height * 0.34f, width * 0.74f, height * 0.65f)
        drawBowl(canvas, bowl)

        when (doughStep) {
            0 -> {
                drawFlourBag(canvas, currentX(width * 0.25f), currentY(height * 0.76f))
                drawPourIfNeeded(canvas, bowl, true)
            }
            1 -> {
                drawWaterCup(canvas, currentX(width * 0.75f), currentY(height * 0.76f))
                drawPourIfNeeded(canvas, bowl, false)
            }
            2 -> {
                drawMixture(canvas, bowl)
                val sx = if (dragType == "spoon") dragX else width * 0.76f
                val sy = if (dragType == "spoon") dragY else height * 0.67f
                drawWoodSpoon(canvas, sx, sy)
            }
            3 -> {
                val size = width * (0.27f + 0.018f * sin(kneadPulse))
                parts["dough_ball"]?.let {
                    FoodPainter.drawPart(
                        canvas, it,
                        RectF(
                            width * 0.50f - size / 2f,
                            height * 0.50f - size / 2f,
                            width * 0.50f + size / 2f,
                            height * 0.50f + size / 2f
                        ),
                        context.assets
                    )
                }
                paint.color = Color.argb(75, 255, 255, 255)
                canvas.drawOval(
                    RectF(width * .38f, height * .47f, width * .62f, height * .54f),
                    paint
                )
            }
        }
    }

    private fun drawBowl(canvas: Canvas, r: RectF) {
        paint.color = Color.rgb(103, 161, 203)
        canvas.drawOval(r, paint)
        paint.color = Color.rgb(167, 211, 235)
        canvas.drawOval(
            RectF(
                r.left + r.width() * .07f,
                r.top + r.height() * .06f,
                r.right - r.width() * .07f,
                r.centerY()
            ),
            paint
        )

        if (doughStep >= 1) {
            paint.color = if (doughStep == 1) Color.rgb(244, 234, 215) else Color.rgb(225, 198, 156)
            canvas.drawOval(
                RectF(
                    r.left + r.width() * .15f,
                    r.top + r.height() * .16f,
                    r.right - r.width() * .15f,
                    r.bottom - r.height() * .26f
                ),
                paint
            )
        }
    }

    private fun drawMixture(canvas: Canvas, r: RectF) {
        val p = (stirTravel / (PI * 5.0)).toFloat().coerceIn(0f, 1f)
        paint.color = Color.rgb(
            (236 - 20 * p).toInt(),
            (214 - 22 * p).toInt(),
            (178 - 22 * p).toInt()
        )
        canvas.drawOval(
            RectF(
                r.left + r.width() * .15f,
                r.top + r.height() * .17f,
                r.right - r.width() * .15f,
                r.bottom - r.height() * .25f
            ),
            paint
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width * .008f
        paint.color = Color.argb(110, 143, 105, 68)
        repeat(3) { i ->
            val rr = RectF(
                r.centerX() - width * (.08f + i * .025f),
                r.centerY() - width * (.045f + i * .013f),
                r.centerX() + width * (.08f + i * .025f),
                r.centerY() + width * (.045f + i * .013f)
            )
            canvas.drawArc(rr, 30f + i * 40f, 220f, false, paint)
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawPourIfNeeded(canvas: Canvas, bowl: RectF, flour: Boolean) {
        if (pourStart == 0L) return
        val t = ((System.currentTimeMillis() - pourStart) / 800f).coerceIn(0f, 1f)
        val sx = dragX
        val sy = dragY
        val ex = bowl.centerX()
        val ey = bowl.top + bowl.height() * .20f

        if (flour) {
            paint.color = Color.rgb(248, 237, 215)
            repeat((14 * t).toInt().coerceAtLeast(3)) { i ->
                val f = (i + 1f) / 15f
                canvas.drawCircle(
                    sx + (ex - sx) * f + sin(i * 1.7f) * width * .01f,
                    sy + (ey - sy) * f,
                    width * .008f,
                    paint
                )
            }
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = width * .017f
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = Color.rgb(110, 187, 220)
            canvas.drawLine(sx - width * .02f, sy, ex, ey, paint)
            paint.style = Paint.Style.FILL
            paint.strokeCap = Paint.Cap.BUTT
        }

        if (t < 1f) {
            postInvalidateOnAnimation()
        } else {
            pourStart = 0L
            active = false
            dragType = null
            dragX = 0f
            dragY = 0f
            doughStep += 1
            haptic()
            invalidate()
        }
    }

    private fun drawRoll(canvas: Canvas) {
        drawBoard(canvas)
        val cx = pizzaCx
        val cy = pizzaCy
        val size = width * (0.25f + 0.35f * rollProgress)

        if (rollProgress < .08f) {
            parts["dough_ball"]?.let {
                FoodPainter.drawPart(
                    canvas, it,
                    RectF(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f),
                    context.assets
                )
            }
        } else {
            parts["pizza_dough"]?.let {
                FoodPainter.drawPart(
                    canvas, it,
                    RectF(cx - size / 2f, cy - size * .42f, cx + size / 2f, cy + size * .42f),
                    context.assets
                )
            }
        }

        drawRollingPin(canvas, cx, if (rollerY == 0f) height * .70f else rollerY)
    }

    private fun drawSauce(canvas: Canvas) {
        drawBoard(canvas)
        drawUserPizza(canvas, pizzaCx, pizzaCy, pizzaRadius, false)

        // Sauce bowl
        val bowlCx = width * .20f
        val bowlCy = height * .74f
        paint.color = Color.rgb(145, 79, 66)
        canvas.drawCircle(bowlCx, bowlCy, width * .085f, paint)
        paint.color = Color.rgb(205, 64, 43)
        canvas.drawCircle(bowlCx, bowlCy, width * .064f, paint)

        val sx = if (dragType == "sauce_spoon") dragX else width * .79f
        val sy = if (dragType == "sauce_spoon") dragY else height * .73f
        drawWoodSpoon(canvas, sx, sy)

        if (spoonLoaded > .05f) {
            paint.color = Color.rgb(202, 55, 38)
            canvas.drawCircle(sx - width * .035f, sy - width * .004f, width * .022f, paint)
        }
    }

    private fun drawCheese(canvas: Canvas) {
        drawBoard(canvas)
        drawUserPizza(canvas, pizzaCx, pizzaCy, pizzaRadius, false)
        drawGrater(canvas)

        val cx = width * .79f
        val cy = if (cheeseY == 0f) height * .74f else cheeseY
        drawCheeseWedge(canvas, cx, cy)
    }

    private fun drawTopping(canvas: Canvas) {
        drawBoard(canvas)
        drawUserPizza(canvas, pizzaCx, pizzaCy, pizzaRadius, false)

        val trayCx = width * .78f
        val trayCy = height * .76f
        paint.color = Color.rgb(236, 218, 184)
        canvas.drawCircle(trayCx, trayCy, width * .105f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width * .006f
        paint.color = Color.rgb(174, 135, 88)
        canvas.drawCircle(trayCx, trayCy, width * .105f, paint)
        paint.style = Paint.Style.FILL

        parts["pepperoni"]?.let { p ->
            repeat((5 - pepperoniPoints.size).coerceAtLeast(0).coerceAtMost(3)) { i ->
                val ox = (i - 1) * width * .055f
                FoodPainter.drawPart(
                    canvas, p,
                    RectF(
                        trayCx + ox - width * .035f,
                        trayCy - width * .035f,
                        trayCx + ox + width * .035f,
                        trayCy + width * .035f
                    ),
                    context.assets
                )
            }

            if (dragType == "pepperoni") {
                FoodPainter.drawPart(
                    canvas, p,
                    RectF(
                        dragX - width * .050f,
                        dragY - width * .050f,
                        dragX + width * .050f,
                        dragY + width * .050f
                    ),
                    context.assets
                )
            }
        }
    }

    private fun drawOven(canvas: Canvas) {
        val oven = RectF(width * .12f, height * .23f, width * .88f, height * .62f)
        paint.color = Color.rgb(79, 74, 69)
        canvas.drawRoundRect(oven, width * .03f, width * .03f, paint)
        val inside = RectF(
            oven.left + width * .07f,
            oven.top + height * .06f,
            oven.right - width * .07f,
            oven.bottom - height * .05f
        )
        paint.color = Color.rgb(40, 40, 39)
        canvas.drawRoundRect(inside, width * .02f, width * .02f, paint)
        paint.color = Color.rgb(232, 115, 42)
        repeat(3) { i ->
            val y = inside.top + inside.height() * (.25f + i * .23f)
            canvas.drawRoundRect(
                RectF(inside.left + width * .03f, y, inside.right - width * .03f, y + width * .008f),
                width * .004f, width * .004f, paint
            )
        }

        val tray = RectF(width * .20f, height * .68f, width * .80f, height * .82f)
        paint.color = Color.rgb(203, 210, 212)
        canvas.drawRoundRect(tray, width * .02f, width * .02f, paint)

        when (ovenStep) {
            0 -> {
                val cx = if (pizzaDragX == 0f) width * .50f else pizzaDragX
                val cy = if (pizzaDragY == 0f) height * .75f else pizzaDragY
                drawUserPizza(canvas, cx, cy, width * .19f, false)
            }
            1 -> {
                drawUserPizza(canvas, inside.centerX(), inside.centerY(), width * .18f, true)
                val elapsed = System.currentTimeMillis() - bakeStart
                if (elapsed < 1700L) {
                    postInvalidateOnAnimation()
                } else {
                    ovenStep = 2
                    baked = true
                    haptic()
                    invalidate()
                }
            }
            2 -> {
                val cx = if (pizzaDragX == 0f) inside.centerX() else pizzaDragX
                val cy = if (pizzaDragY == 0f) inside.centerY() else pizzaDragY
                drawUserPizza(canvas, cx, cy, width * .19f, true)
            }
        }
    }

    private fun drawCut(canvas: Canvas) {
        drawBoard(canvas)
        drawCutPizza(canvas)

        if (cutAngles.size < 3) {
            val cx = if (cutterX == 0f) width * .82f else cutterX
            val cy = if (cutterY == 0f) height * .28f else cutterY
            drawPizzaCutter(canvas, cx, cy)
        }
    }

    private fun drawDone(canvas: Canvas) {
        drawBoard(canvas)
        drawCutPizza(canvas)
        drawSparkles(canvas)

        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.rgb(74, 55, 42)
        paint.textSize = width * .060f
        paint.isFakeBoldText = true
        canvas.drawText("완성!", width / 2f, height * .20f, paint)
        paint.isFakeBoldText = false

        val buttons = doneButtons()
        val labels = listOf("다시 만들기", "다른 요리", "종이로 놀기")
        val colors = listOf(
            Color.rgb(239, 151, 73),
            Color.rgb(103, 174, 106),
            Color.rgb(83, 155, 211)
        )
        buttons.forEachIndexed { i, r ->
            paint.color = colors[i]
            canvas.drawRoundRect(r, width * .018f, width * .018f, paint)
            paint.color = Color.WHITE
            paint.textSize = width * .028f
            paint.isFakeBoldText = true
            canvas.drawText(labels[i], r.centerX(), r.centerY() + width * .010f, paint)
            paint.isFakeBoldText = false
        }
    }

    private fun drawUserPizza(canvas: Canvas, cx: Float, cy: Float, radius: Float, bakedState: Boolean) {
        val r = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        parts["pizza_dough"]?.let {
            FoodPainter.drawPart(canvas, it, r, context.assets)
        }

        canvas.save()
        val clip = Path().apply { addCircle(cx, cy, radius * .88f, Path.Direction.CW) }
        canvas.clipPath(clip)

        paint.color = Color.rgb(203, 65, 43)
        saucePoints.forEach { p ->
            val x = cx + p.x * radius
            val y = cy + p.y * radius
            canvas.drawCircle(x, y, radius * .10f, paint)
        }

        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = radius * .035f
        paint.color = if (bakedState) Color.rgb(238, 190, 72) else Color.rgb(249, 210, 86)
        cheesePoints.forEachIndexed { i, p ->
            val x = cx + p.x * radius
            val y = cy + p.y * radius
            val angle = (i * 37 % 180) * PI.toFloat() / 180f
            val dx = cos(angle) * radius * .055f
            val dy = sin(angle) * radius * .055f
            canvas.drawLine(x - dx, y - dy, x + dx, y + dy, paint)
        }
        paint.strokeCap = Paint.Cap.BUTT

        parts["pepperoni"]?.let { pep ->
            pepperoniPoints.forEach { p ->
                val x = cx + p.x * radius
                val y = cy + p.y * radius
                val s = radius * .20f
                FoodPainter.drawPart(
                    canvas, pep,
                    RectF(x - s / 2f, y - s / 2f, x + s / 2f, y + s / 2f),
                    context.assets
                )
            }
        }

        if (bakedState) {
            paint.color = Color.argb(28, 190, 100, 28)
            canvas.drawCircle(cx, cy, radius * .90f, paint)
        }

        canvas.restore()
    }

    private fun drawCutPizza(canvas: Canvas) {
        val cx = pizzaCx
        val cy = pizzaCy
        val r = pizzaRadius

        if (cutAngles.isEmpty()) {
            drawUserPizza(canvas, cx, cy, r, true)
            return
        }

        val bounds = mutableListOf<Float>()
        cutAngles.forEach {
            val a = normalizeAngle(it)
            bounds += a
            bounds += normalizeAngle(a + PI.toFloat())
        }
        bounds.sort()

        val offset = r * .075f
        for (i in bounds.indices) {
            val start = bounds[i]
            val end = if (i == bounds.lastIndex) bounds[0] + (2f * PI.toFloat()) else bounds[i + 1]
            val mid = (start + end) / 2f
            val dx = cos(mid) * offset
            val dy = sin(mid) * offset
            val wedge = Path().apply {
                moveTo(cx, cy)
                arcTo(
                    RectF(cx - r, cy - r, cx + r, cy + r),
                    Math.toDegrees(start.toDouble()).toFloat(),
                    Math.toDegrees((end - start).toDouble()).toFloat(),
                    false
                )
                close()
            }
            canvas.save()
            canvas.translate(dx, dy)
            canvas.clipPath(wedge)
            drawUserPizza(canvas, cx, cy, r, true)
            canvas.restore()
        }
    }

    private fun drawSparkles(canvas: Canvas) {
        paint.color = Color.rgb(245, 179, 45)
        val pts = listOf(
            width * .23f to height * .32f,
            width * .78f to height * .36f,
            width * .22f to height * .66f,
            width * .79f to height * .68f
        )
        pts.forEach { (x, y) ->
            canvas.drawCircle(x, y, width * .010f, paint)
            canvas.drawCircle(x + width * .025f, y - width * .020f, width * .006f, paint)
        }
    }

    private fun drawFlourBag(canvas: Canvas, cx: Float, cy: Float) {
        canvas.save()
        if (pourStart > 0L) canvas.rotate(-45f, cx, cy)
        paint.color = Color.rgb(230, 194, 123)
        val r = RectF(cx - width * .105f, cy - height * .065f, cx + width * .105f, cy + height * .065f)
        canvas.drawRoundRect(r, width * .018f, width * .018f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width * .006f
        paint.color = Color.rgb(129, 94, 59)
        canvas.drawRoundRect(r, width * .018f, width * .018f, paint)
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = width * .032f
        paint.isFakeBoldText = true
        paint.color = Color.rgb(95, 72, 52)
        canvas.drawText("밀가루", cx, cy + width * .011f, paint)
        paint.isFakeBoldText = false
        canvas.restore()
    }

    private fun drawWaterCup(canvas: Canvas, cx: Float, cy: Float) {
        canvas.save()
        if (pourStart > 0L) canvas.rotate(-45f, cx, cy)
        paint.color = Color.argb(180, 168, 219, 239)
        val r = RectF(cx - width * .070f, cy - height * .070f, cx + width * .070f, cy + height * .070f)
        canvas.drawRoundRect(r, width * .015f, width * .015f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width * .005f
        paint.color = Color.rgb(86, 147, 177)
        canvas.drawRoundRect(r, width * .015f, width * .015f, paint)
        paint.style = Paint.Style.FILL
        canvas.restore()
    }

    private fun drawWoodSpoon(canvas: Canvas, cx: Float, cy: Float) {
        canvas.save()
        canvas.rotate(-28f, cx, cy)
        paint.color = Color.rgb(155, 94, 48)
        canvas.drawOval(
            RectF(cx - width * .055f, cy - width * .045f, cx + width * .035f, cy + width * .045f),
            paint
        )
        paint.strokeWidth = width * .020f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(cx + width * .020f, cy, cx + width * .18f, cy, paint)
        paint.strokeCap = Paint.Cap.BUTT
        canvas.restore()
    }

    private fun drawRollingPin(canvas: Canvas, cx: Float, cy: Float) {
        paint.color = Color.rgb(169, 102, 52)
        canvas.drawRoundRect(
            RectF(cx - width * .17f, cy - width * .045f, cx + width * .17f, cy + width * .045f),
            width * .02f, width * .02f, paint
        )
        paint.strokeWidth = width * .018f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(111, 73, 43)
        canvas.drawLine(cx - width * .22f, cy, cx - width * .16f, cy, paint)
        canvas.drawLine(cx + width * .16f, cy, cx + width * .22f, cy, paint)
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun graterRect(): RectF =
        RectF(width * .72f, height * .34f, width * .86f, height * .62f)

    private fun drawGrater(canvas: Canvas) {
        val r = graterRect()
        paint.color = Color.rgb(182, 188, 190)
        canvas.drawRoundRect(r, width * .015f, width * .015f, paint)
        paint.color = Color.rgb(101, 107, 109)
        repeat(5) { row ->
            repeat(3) { col ->
                canvas.drawCircle(
                    r.left + r.width() * (.24f + col * .26f),
                    r.top + r.height() * (.14f + row * .17f),
                    width * .007f,
                    paint
                )
            }
        }
    }

    private fun drawCheeseWedge(canvas: Canvas, cx: Float, cy: Float) {
        val p = Path().apply {
            moveTo(cx - width * .065f, cy + width * .050f)
            lineTo(cx + width * .065f, cy + width * .050f)
            lineTo(cx + width * .040f, cy - width * .060f)
            close()
        }
        paint.color = Color.rgb(255, 213, 82)
        canvas.drawPath(p, paint)
        paint.color = Color.rgb(208, 159, 45)
        repeat(3) { i ->
            canvas.drawCircle(
                cx - width * .025f + i * width * .025f,
                cy + width * .015f - i * width * .012f,
                width * .008f,
                paint
            )
        }
    }

    private fun drawPizzaCutter(canvas: Canvas, cx: Float, cy: Float) {
        paint.color = Color.rgb(58, 58, 58)
        canvas.drawCircle(cx - width * .035f, cy, width * .070f, paint)
        paint.color = Color.rgb(210, 215, 217)
        canvas.drawCircle(cx - width * .035f, cy, width * .055f, paint)
        paint.strokeWidth = width * .022f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(64, 139, 88)
        canvas.drawLine(cx + width * .020f, cy, cx + width * .15f, cy + width * .030f, paint)
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun currentX(home: Float): Float = if (dragType != null) dragX else home
    private fun currentY(home: Float): Float = if (dragType != null) dragY else home

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP &&
            event.x < width * .12f && event.y < height * .12f
        ) {
            onBack()
            return true
        }

        if (phase == Phase.DONE) {
            if (event.action == MotionEvent.ACTION_UP) {
                val b = doneButtons()
                when {
                    b[0].contains(event.x, event.y) -> resetPizza()
                    b[1].contains(event.x, event.y) -> onBack()
                    b[2].contains(event.x, event.y) -> onPrint()
                }
            }
            return true
        }

        when (phase) {
            Phase.DOUGH -> touchDough(event)
            Phase.ROLL -> touchRoll(event)
            Phase.SAUCE -> touchSauce(event)
            Phase.CHEESE -> touchCheese(event)
            Phase.TOPPING -> touchTopping(event)
            Phase.OVEN -> touchOven(event)
            Phase.CUT -> touchCut(event)
            Phase.DONE -> Unit
        }
        return true
    }

    private fun touchDough(event: MotionEvent) {
        val bowl = RectF(width * .26f, height * .34f, width * .74f, height * .65f)

        if (doughStep == 0 || doughStep == 1) {
            val homeX = if (doughStep == 0) width * .25f else width * .75f
            val homeY = height * .76f
            val kind = if (doughStep == 0) "flour" else "water"

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (near(event.x, event.y, homeX, homeY, width * .16f)) {
                        active = true
                        dragType = kind
                        dragX = event.x
                        dragY = event.y
                    }
                }
                MotionEvent.ACTION_MOVE -> if (active && pourStart == 0L) {
                    dragX = event.x
                    dragY = event.y
                    invalidate()
                }
                MotionEvent.ACTION_UP -> if (active && pourStart == 0L) {
                    val zone = RectF(bowl.left, bowl.top - height * .14f, bowl.right, bowl.centerY())
                    if (zone.contains(event.x, event.y)) {
                        dragX = event.x
                        dragY = event.y
                        pourStart = System.currentTimeMillis()
                        haptic()
                        postInvalidateOnAnimation()
                    } else {
                        active = false
                        dragType = null
                        dragX = 0f
                        dragY = 0f
                        invalidate()
                    }
                }
            }
            return
        }

        if (doughStep == 2) {
            val homeX = width * .76f
            val homeY = height * .67f
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (near(event.x, event.y, homeX, homeY, width * .18f)) {
                        active = true
                        dragType = "spoon"
                        dragX = event.x
                        dragY = event.y
                        stirAngle = atan2(
                            (event.y - bowl.centerY()).toDouble(),
                            (event.x - bowl.centerX()).toDouble()
                        )
                    }
                }
                MotionEvent.ACTION_MOVE -> if (active) {
                    dragX = event.x
                    dragY = event.y
                    if (bowl.contains(event.x, event.y)) {
                        val a = atan2(
                            (event.y - bowl.centerY()).toDouble(),
                            (event.x - bowl.centerX()).toDouble()
                        )
                        var d = a - stirAngle
                        while (d > PI) d -= 2 * PI
                        while (d < -PI) d += 2 * PI
                        stirTravel += abs(d)
                        stirAngle = a
                        if (stirTravel >= PI * 5.0) {
                            active = false
                            dragType = null
                            dragX = 0f
                            dragY = 0f
                            doughStep = 3
                            haptic()
                        }
                    }
                    invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    active = false
                    dragType = null
                    dragX = 0f
                    dragY = 0f
                    invalidate()
                }
            }
            return
        }

        // knead directly with the finger
        val cx = width * .50f
        val cy = height * .50f
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (near(event.x, event.y, cx, cy, width * .18f)) {
                    active = true
                    lastX = event.x
                    lastY = event.y
                }
            }
            MotionEvent.ACTION_MOVE -> if (active) {
                val d = hypot((event.x - lastX).toDouble(), (event.y - lastY).toDouble()).toFloat()
                kneadTravel += d
                kneadPulse += d / (width * .05f)
                lastX = event.x
                lastY = event.y
                if (kneadTravel >= width * 1.8f) {
                    active = false
                    phase = Phase.ROLL
                    haptic()
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> active = false
        }
    }

    private fun touchRoll(event: MotionEvent) {
        val homeY = if (rollerY == 0f) height * .70f else rollerY
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (near(event.x, event.y, pizzaCx, homeY, width * .23f)) {
                    active = true
                    dragType = "roller"
                    rollerY = event.y
                    lastY = event.y
                }
            }
            MotionEvent.ACTION_MOVE -> if (active) {
                val dy = event.y - lastY
                rollerY = event.y.coerceIn(height * .30f, height * .72f)
                val currentRadius = width * (.13f + .18f * rollProgress)
                if (abs(rollerY - pizzaCy) < currentRadius + width * .07f) {
                    rollProgress = (rollProgress + abs(dy) / (height * .75f)).coerceAtMost(1f)
                    if (rollProgress >= 1f) {
                        active = false
                        dragType = null
                        phase = Phase.SAUCE
                        haptic()
                    }
                }
                lastY = event.y
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                active = false
                dragType = null
                invalidate()
            }
        }
    }

    private fun touchSauce(event: MotionEvent) {
        val bowlCx = width * .20f
        val bowlCy = height * .74f
        val spoonHomeX = width * .79f
        val spoonHomeY = height * .73f

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val sx = if (dragType == "sauce_spoon") dragX else spoonHomeX
                val sy = if (dragType == "sauce_spoon") dragY else spoonHomeY
                if (near(event.x, event.y, sx, sy, width * .17f)) {
                    active = true
                    dragType = "sauce_spoon"
                    dragX = event.x
                    dragY = event.y
                    lastX = event.x
                    lastY = event.y
                }
            }
            MotionEvent.ACTION_MOVE -> if (active) {
                dragX = event.x
                dragY = event.y

                if (near(event.x, event.y, bowlCx, bowlCy, width * .10f)) {
                    spoonLoaded = 1f
                    haptic()
                } else {
                    val dx = event.x - pizzaCx
                    val dy = event.y - pizzaCy
                    val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                    if (dist < pizzaRadius * .82f && spoonLoaded > .02f) {
                        val add = saucePoints.isEmpty() ||
                            hypot(
                                (dx / pizzaRadius - saucePoints.last().x).toDouble(),
                                (dy / pizzaRadius - saucePoints.last().y).toDouble()
                            ) > .07

                        if (add) {
                            saucePoints += PointF(dx / pizzaRadius, dy / pizzaRadius)
                            spoonLoaded = (spoonLoaded - .055f).coerceAtLeast(0f)
                            val gx = (((dx / pizzaRadius + 1f) * 2.5f).toInt()).coerceIn(0, 4)
                            val gy = (((dy / pizzaRadius + 1f) * 2.5f).toInt()).coerceIn(0, 4)
                            sauceCells += gy * 5 + gx

                            if (sauceCells.size >= 11) {
                                active = false
                                dragType = null
                                dragX = 0f
                                dragY = 0f
                                phase = Phase.CHEESE
                                haptic()
                            }
                        }
                    }
                }
                lastX = event.x
                lastY = event.y
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                active = false
                dragType = null
                dragX = 0f
                dragY = 0f
                invalidate()
            }
        }
    }

    private fun touchCheese(event: MotionEvent) {
        val homeX = width * .79f
        val homeY = if (cheeseY == 0f) height * .74f else cheeseY
        val gr = graterRect()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (near(event.x, event.y, homeX, homeY, width * .16f)) {
                    active = true
                    dragType = "cheese"
                    cheeseY = event.y
                    lastY = event.y
                }
            }
            MotionEvent.ACTION_MOVE -> if (active) {
                val dy = event.y - lastY
                cheeseY = event.y.coerceIn(gr.top - height * .06f, gr.bottom + height * .16f)
                if (gr.contains(homeX, cheeseY)) {
                    cheeseTravel += abs(dy)
                    while (cheeseTravel >= width * .035f && cheesePoints.size < 34) {
                        cheeseTravel -= width * .035f
                        val i = cheesePoints.size
                        val a = (i * 137.5f) * PI.toFloat() / 180f
                        val rr = .72f * kotlin.math.sqrt((i + 1f) / 35f)
                        cheesePoints += PointF(cos(a) * rr, sin(a) * rr)
                        haptic()
                    }
                    if (cheesePoints.size >= 34) {
                        active = false
                        dragType = null
                        phase = Phase.TOPPING
                        haptic()
                    }
                }
                lastY = event.y
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                active = false
                dragType = null
                cheeseY = 0f
                invalidate()
            }
        }
    }

    private fun touchTopping(event: MotionEvent) {
        val trayCx = width * .78f
        val trayCy = height * .76f

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (pepperoniPoints.size < 5 &&
                    near(event.x, event.y, trayCx, trayCy, width * .15f)
                ) {
                    active = true
                    dragType = "pepperoni"
                    dragX = event.x
                    dragY = event.y
                }
            }
            MotionEvent.ACTION_MOVE -> if (active) {
                dragX = event.x
                dragY = event.y
                invalidate()
            }
            MotionEvent.ACTION_UP -> if (active) {
                val dx = event.x - pizzaCx
                val dy = event.y - pizzaCy
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (dist < pizzaRadius * .78f) {
                    pepperoniPoints += PointF(dx / pizzaRadius, dy / pizzaRadius)
                    haptic()
                    if (pepperoniPoints.size >= 5) {
                        phase = Phase.OVEN
                    }
                }
                active = false
                dragType = null
                dragX = 0f
                dragY = 0f
                invalidate()
            }
        }
    }

    private fun touchOven(event: MotionEvent) {
        if (ovenStep == 1) return

        val oven = RectF(width * .12f, height * .23f, width * .88f, height * .62f)
        val inside = RectF(
            oven.left + width * .07f,
            oven.top + height * .06f,
            oven.right - width * .07f,
            oven.bottom - height * .05f
        )
        val tray = RectF(width * .20f, height * .68f, width * .80f, height * .82f)

        val homeCx = if (ovenStep == 0) width * .50f else inside.centerX()
        val homeCy = if (ovenStep == 0) height * .75f else inside.centerY()
        val cx = if (pizzaDragX == 0f) homeCx else pizzaDragX
        val cy = if (pizzaDragY == 0f) homeCy else pizzaDragY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (near(event.x, event.y, cx, cy, width * .22f)) {
                    active = true
                    dragType = "pizza"
                    pizzaDragX = event.x
                    pizzaDragY = event.y
                }
            }
            MotionEvent.ACTION_MOVE -> if (active) {
                pizzaDragX = event.x
                pizzaDragY = event.y
                invalidate()
            }
            MotionEvent.ACTION_UP -> if (active) {
                if (ovenStep == 0 && inside.contains(event.x, event.y)) {
                    ovenStep = 1
                    bakeStart = System.currentTimeMillis()
                    pizzaDragX = 0f
                    pizzaDragY = 0f
                    haptic()
                    postInvalidateOnAnimation()
                } else if (ovenStep == 2 && tray.contains(event.x, event.y)) {
                    pizzaDragX = 0f
                    pizzaDragY = 0f
                    phase = Phase.CUT
                    haptic()
                } else {
                    pizzaDragX = 0f
                    pizzaDragY = 0f
                }
                active = false
                dragType = null
                invalidate()
            }
        }
    }

    private fun touchCut(event: MotionEvent) {
        val homeX = if (cutterX == 0f) width * .82f else cutterX
        val homeY = if (cutterY == 0f) height * .28f else cutterY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (near(event.x, event.y, homeX, homeY, width * .18f)) {
                    active = true
                    dragType = "cutter"
                    cutterX = event.x
                    cutterY = event.y
                    cutEntry = null
                    cutLastInside = null
                }
            }
            MotionEvent.ACTION_MOVE -> if (active) {
                cutterX = event.x
                cutterY = event.y
                val dx = event.x - pizzaCx
                val dy = event.y - pizzaCy
                val inside = hypot(dx.toDouble(), dy.toDouble()) <= pizzaRadius
                if (inside) {
                    if (cutEntry == null) cutEntry = PointF(event.x, event.y)
                    cutLastInside = PointF(event.x, event.y)
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> if (active) {
                val entry = cutEntry
                val exit = cutLastInside
                val endOutside = hypot(
                    (event.x - pizzaCx).toDouble(),
                    (event.y - pizzaCy).toDouble()
                ) > pizzaRadius

                if (entry != null && exit != null && endOutside) {
                    val vx = event.x - entry.x
                    val vy = event.y - entry.y
                    val len = hypot(vx.toDouble(), vy.toDouble()).toFloat()
                    if (len > pizzaRadius * 1.15f) {
                        val dist = pointToLineDistance(
                            pizzaCx, pizzaCy,
                            entry.x, entry.y,
                            event.x, event.y
                        )
                        if (dist < pizzaRadius * .28f) {
                            var angle = atan2(vy, vx)
                            while (angle < 0f) angle += PI.toFloat()
                            while (angle >= PI.toFloat()) angle -= PI.toFloat()

                            val duplicate = cutAngles.any {
                                val d = abs(it - angle)
                                min(d, PI.toFloat() - d) < (15f * PI.toFloat() / 180f)
                            }
                            if (!duplicate) {
                                cutAngles += angle
                                haptic()
                                if (cutAngles.size >= 3) {
                                    active = false
                                    dragType = null
                                    cutterX = 0f
                                    cutterY = 0f
                                    postDelayed({
                                        phase = Phase.DONE
                                        invalidate()
                                    }, 550L)
                                }
                            }
                        }
                    }
                }

                active = false
                dragType = null
                cutterX = 0f
                cutterY = 0f
                cutEntry = null
                cutLastInside = null
                invalidate()
            }
        }
    }

    private fun doneButtons(): List<RectF> {
        val gap = width * .020f
        val left = width * .08f
        val total = width * .84f
        val bw = (total - gap * 2f) / 3f
        val top = height * .77f
        val bottom = height * .835f
        return List(3) { i ->
            RectF(
                left + i * (bw + gap),
                top,
                left + i * (bw + gap) + bw,
                bottom
            )
        }
    }

    private fun resetPizza() {
        phase = Phase.DOUGH
        active = false
        dragType = null
        dragX = 0f
        dragY = 0f
        lastX = 0f
        lastY = 0f

        doughStep = 0
        pourStart = 0L
        stirAngle = 0.0
        stirTravel = 0.0
        kneadTravel = 0f
        kneadPulse = 0f

        rollerY = 0f
        rollProgress = 0f

        spoonLoaded = 0f
        saucePoints.clear()
        sauceCells.clear()

        cheeseY = 0f
        cheeseTravel = 0f
        cheesePoints.clear()

        pepperoniPoints.clear()

        ovenStep = 0
        bakeStart = 0L
        pizzaDragX = 0f
        pizzaDragY = 0f
        baked = false

        cutterX = 0f
        cutterY = 0f
        cutEntry = null
        cutLastInside = null
        cutAngles.clear()

        invalidate()
    }

    private fun haptic() {
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun near(x: Float, y: Float, cx: Float, cy: Float, r: Float): Boolean =
        hypot((x - cx).toDouble(), (y - cy).toDouble()) <= r

    private fun pointToLineDistance(
        px: Float, py: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float
    ): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        if (dx == 0f && dy == 0f) return hypot((px - x1).toDouble(), (py - y1).toDouble()).toFloat()
        val t = (((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)).coerceIn(0f, 1f)
        val cx = x1 + t * dx
        val cy = y1 + t * dy
        return hypot((px - cx).toDouble(), (py - cy).toDouble()).toFloat()
    }

    private fun normalizeAngle(a: Float): Float {
        var v = a
        val twoPi = 2f * PI.toFloat()
        while (v < 0f) v += twoPi
        while (v >= twoPi) v -= twoPi
        return v
    }
}
