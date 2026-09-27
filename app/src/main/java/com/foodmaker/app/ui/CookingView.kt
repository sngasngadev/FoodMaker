package com.foodmaker.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.foodmaker.app.engine.GameSession
import com.foodmaker.app.model.ActionType
import com.foodmaker.app.model.PartDefinition
import com.foodmaker.app.model.RecipeStep
import kotlin.math.abs
import kotlin.math.hypot

class CookingView(
    context: Context,
    private val session: GameSession,
    private val parts: Map<String, PartDefinition>,
    private val onBack: () -> Unit,
    private val onPrint: () -> Unit
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var dragX = 0f
    private var dragY = 0f
    private var toolX = 0f
    private var toolY = 0f
    private var gestureProgress = 0f
    private var repetitions = 0
    private var cutTravel = 0f
    private var activeDrag = false
    private var enteredTarget = false
    private var transitioning = false

    private val dishArea: RectF
        get() = RectF(width * 0.09f, height * 0.29f, width * 0.91f, height * 0.66f)

    private val workArea: RectF
        get() = RectF(width * 0.13f, height * 0.29f, width * 0.87f, height * 0.76f)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(255, 249, 238))
        drawHeader(canvas)

        if (session.isFinished) {
            drawFinished(canvas)
            return
        }

        val step = session.currentStep ?: return

        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.rgb(64, 53, 46)
        paint.textSize = width * 0.057f
        paint.isFakeBoldText = true
        canvas.drawText(step.instruction, width / 2f, height * 0.205f, paint)
        paint.isFakeBoldText = false

        when (step.action) {
            ActionType.PLACE, ActionType.POUR -> drawPlaceScene(canvas, step)
            ActionType.CUT -> drawCutScene(canvas, step)
            ActionType.COOK -> {
                if (step.tool == "oven") drawOvenScene(canvas, step)
                else drawCookScene(canvas, step)
            }
            ActionType.SPREAD -> drawSpreadScene(canvas, step)
            ActionType.FLIP -> drawFlipScene(canvas, step)
            ActionType.ROLL -> drawRollScene(canvas, step)
        }

        drawHint(canvas, step)
        drawProgress(canvas, step.repeat)
    }

    private fun drawHeader(canvas: Canvas) {
        paint.color = Color.rgb(64, 53, 46)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = width * 0.052f
        paint.isFakeBoldText = true
        canvas.drawText("‹", width * 0.055f, height * 0.073f, paint)
        canvas.drawText(session.recipe.title, width * 0.14f, height * 0.073f, paint)
        paint.isFakeBoldText = false

        val total = session.recipe.steps.size.coerceAtLeast(1)
        val completed = session.stepIndex.coerceAtMost(total)
        val startX = width * 0.50f
        val gap = width * 0.055f
        for (i in 0 until total.coerceAtMost(8)) {
            paint.color = if (i < completed) Color.rgb(238, 151, 79) else Color.rgb(210, 224, 231)
            canvas.drawCircle(startX + i * gap, height * 0.065f, width * 0.012f, paint)
        }
    }

    private fun drawPlate(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(245, 238, 224)
        canvas.drawOval(
            RectF(width * 0.17f, height * 0.38f, width * 0.83f, height * 0.65f),
            paint
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width * 0.006f
        paint.color = Color.rgb(222, 209, 190)
        canvas.drawOval(
            RectF(width * 0.21f, height * 0.41f, width * 0.79f, height * 0.62f),
            paint
        )
        paint.style = Paint.Style.FILL
    }

    private fun drawPlaceScene(canvas: Canvas, step: RecipeStep) {
        drawPlate(canvas)
        FoodPainter.drawDish(canvas, session.dishParts, parts, dishArea, context.assets)

        if (step.repeat > 1 && repetitions > 0) {
            val offsets = listOf(
                -0.18f to -0.07f,
                0.16f to -0.03f,
                -0.02f to 0.06f,
                0.18f to 0.08f,
                -0.16f to 0.09f
            )
            val part = parts[step.input]
            if (part != null) {
                repeat(repetitions.coerceAtMost(offsets.size)) { i ->
                    val (ox, oy) = offsets[i]
                    val size = width * 0.13f
                    val cx = dishArea.centerX() + dishArea.width() * ox
                    val cy = dishArea.centerY() + dishArea.height() * oy
                    FoodPainter.drawPart(
                        canvas,
                        part,
                        RectF(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f),
                        context.assets
                    )
                }
            }
        }

        parts[step.input]?.let { ingredient ->
            val cx = if (dragX == 0f) width / 2f else dragX
            val cy = if (dragY == 0f) height * 0.80f else dragY
            val size = width * if (step.input == "pepperoni") 0.16f else 0.30f
            FoodPainter.drawPart(
                canvas,
                ingredient,
                RectF(cx - size / 2f, cy - size * 0.36f, cx + size / 2f, cy + size * 0.36f),
                context.assets
            )
        }
    }

    private fun drawBoard(canvas: Canvas) {
        paint.color = Color.rgb(224, 184, 123)
        canvas.drawRoundRect(workArea, 40f, 40f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width * 0.007f
        paint.color = Color.rgb(174, 128, 78)
        canvas.drawRoundRect(workArea, 40f, 40f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun targetRect(): RectF =
        RectF(width * 0.25f, height * 0.38f, width * 0.75f, height * 0.64f)

    private fun drawCutScene(canvas: Canvas, step: RecipeStep) {
        drawBoard(canvas)
        val target = targetRect()
        val cutPartId = if (transitioning && step.output != null) step.output else step.input
        cutPartId?.let(parts::get)?.let {
            FoodPainter.drawPart(canvas, it, target, context.assets)
        }

        val markCount = repetitions.coerceAtMost(step.repeat)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width * 0.006f
        paint.color = Color.argb(190, 255, 255, 255)
        for (i in 0 until markCount) {
            val x = target.left + target.width() * (i + 1f) / (step.repeat + 1f)
            canvas.drawLine(x, target.top + target.height() * 0.12f, x, target.bottom - target.height() * 0.12f, paint)
        }
        paint.style = Paint.Style.FILL

        val tx = if (toolX == 0f) target.right + width * 0.04f else toolX
        val ty = if (toolY == 0f) target.centerY() else toolY
        if (step.tool == "pizza_cutter") drawPizzaCutter(canvas, tx, ty)
        else drawKnife(canvas, tx, ty)
    }

    private fun drawCookScene(canvas: Canvas, step: RecipeStep) {
        val panCx = width / 2f
        val panCy = height * 0.51f
        val panR = width * 0.29f

        paint.color = Color.rgb(55, 58, 61)
        canvas.drawCircle(panCx, panCy, panR, paint)
        paint.color = Color.rgb(87, 91, 94)
        canvas.drawCircle(panCx, panCy, panR * 0.82f, paint)
        paint.strokeWidth = width * 0.055f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(panCx + panR * 0.78f, panCy + panR * 0.66f, width * 0.91f, height * 0.70f, paint)
        paint.strokeCap = Paint.Cap.BUTT

        val target = RectF(width * 0.29f, height * 0.405f, width * 0.71f, height * 0.61f)
        drawMorphPart(canvas, step, target, stepProgress(step))

        val tx = if (toolX == 0f) width * 0.72f else toolX
        val ty = if (toolY == 0f) height * 0.61f else toolY
        drawSpatula(canvas, tx, ty)
    }

    private fun drawOvenScene(canvas: Canvas, step: RecipeStep) {
        val oven = RectF(width * 0.18f, height * 0.31f, width * 0.82f, height * 0.62f)
        paint.color = Color.rgb(91, 88, 83)
        canvas.drawRoundRect(oven, 28f, 28f, paint)
        paint.color = Color.rgb(42, 43, 44)
        val inside = RectF(oven.left + width * 0.05f, oven.top + height * 0.055f, oven.right - width * 0.05f, oven.bottom - height * 0.045f)
        canvas.drawRoundRect(inside, 22f, 22f, paint)
        paint.color = Color.rgb(233, 126, 51)
        repeat(3) { i ->
            val y = inside.top + inside.height() * (0.28f + i * 0.2f)
            canvas.drawRoundRect(RectF(inside.left + 18f, y, inside.right - 18f, y + 7f), 4f, 4f, paint)
        }

        parts[step.input]?.let { food ->
            val cx = if (dragX == 0f) width / 2f else dragX
            val cy = if (dragY == 0f) height * 0.79f else dragY
            val size = width * 0.40f
            FoodPainter.drawPart(
                canvas,
                food,
                RectF(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f),
                context.assets
            )
        }
    }

    private fun drawSpreadScene(canvas: Canvas, step: RecipeStep) {
        drawBoard(canvas)
        val target = RectF(width * 0.22f, height * 0.36f, width * 0.78f, height * 0.66f)
        drawMorphPart(canvas, step, target, stepProgress(step))

        val tx = if (toolX == 0f) width * 0.65f else toolX
        val ty = if (toolY == 0f) height * 0.66f else toolY
        if (step.tool == "spoon") drawSpoon(canvas, tx, ty)
        else drawRollingPin(canvas, tx, ty)
    }

    private fun drawFlipScene(canvas: Canvas, step: RecipeStep) {
        drawCookScene(canvas, step)
    }

    private fun drawRollScene(canvas: Canvas, step: RecipeStep) {
        drawBoard(canvas)
        val target = targetRect()
        parts[step.input]?.let {
            FoodPainter.drawPart(canvas, it, target, context.assets)
        }
        paint.color = Color.argb(80, 50, 120, 205)
        val arrow = Path().apply {
            moveTo(width * 0.50f, height * 0.66f)
            lineTo(width * 0.43f, height * 0.58f)
            lineTo(width * 0.47f, height * 0.58f)
            lineTo(width * 0.47f, height * 0.47f)
            lineTo(width * 0.53f, height * 0.47f)
            lineTo(width * 0.53f, height * 0.58f)
            lineTo(width * 0.57f, height * 0.58f)
            close()
        }
        canvas.drawPath(arrow, paint)
    }

    private fun drawMorphPart(canvas: Canvas, step: RecipeStep, rect: RectF, progress: Float) {
        val input = parts[step.input]
        val output = step.output?.let(parts::get)
        if (input != null) {
            FoodPainter.drawPart(canvas, input, rect, context.assets, (255 * (1f - progress * 0.72f)).toInt().coerceIn(70, 255))
        }
        if (output != null && progress > 0f) {
            FoodPainter.drawPart(canvas, output, rect, context.assets, (255 * progress).toInt().coerceIn(0, 255))
        }
    }

    private fun stepProgress(step: RecipeStep): Float =
        ((repetitions + gestureProgress.coerceIn(0f, 1f)) / step.repeat.toFloat()).coerceIn(0f, 1f)

    private fun drawHint(canvas: Canvas, step: RecipeStep) {
        val hint = if (transitioning) {
            "좋아!"
        } else when {
            step.action == ActionType.PLACE -> "재료를 접시 위에 올려요"
            step.action == ActionType.CUT && step.tool == "pizza_cutter" -> "피자 커터를 움직여 잘라요"
            step.action == ActionType.CUT -> "칼을 위아래로 움직여 썰어요"
            step.action == ActionType.COOK && step.tool == "oven" -> "피자를 오븐 안으로 넣어요"
            step.action == ActionType.COOK -> "뒤집개를 움직이며 익혀요"
            step.action == ActionType.SPREAD && step.tool == "spoon" -> "숟가락으로 소스를 펴 발라요"
            step.action == ActionType.SPREAD -> "밀대를 좌우로 굴려요"
            step.action == ActionType.FLIP -> "뒤집개를 위로 밀어 뒤집어요"
            step.action == ActionType.ROLL -> "아래에서 위로 말아요"
            else -> "손가락으로 움직여요"
        }
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = width * 0.036f
        paint.color = Color.rgb(126, 105, 89)
        canvas.drawText(hint, width / 2f, height * 0.925f, paint)
    }

    private fun drawProgress(canvas: Canvas, repeat: Int) {
        val progress = ((repetitions + gestureProgress.coerceIn(0f, 1f)) / repeat.toFloat()).coerceIn(0f, 1f)
        if (progress <= 0f) return
        val rect = RectF(width * 0.25f, height * 0.875f, width * 0.75f, height * 0.89f)
        paint.color = Color.rgb(228, 215, 197)
        canvas.drawRoundRect(rect, 8f, 8f, paint)
        paint.color = Color.rgb(106, 176, 112)
        canvas.drawRoundRect(RectF(rect.left, rect.top, rect.left + rect.width() * progress, rect.bottom), 8f, 8f, paint)
    }

    private fun drawFinished(canvas: Canvas) {
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.rgb(62, 50, 42)
        paint.textSize = width * 0.082f
        paint.isFakeBoldText = true
        canvas.drawText("완성!", width / 2f, height * 0.17f, paint)
        paint.isFakeBoldText = false

        FoodPainter.drawDish(
            canvas,
            session.recipe.previewParts,
            parts,
            RectF(width * 0.08f, height * 0.22f, width * 0.92f, height * 0.61f),
            context.assets
        )

        val buttons = finishButtons()
        val labels = listOf("다시 만들기", "다른 요리", "종이로 놀기")
        val colors = listOf(
            Color.rgb(242, 151, 70),
            Color.rgb(104, 178, 104),
            Color.rgb(82, 157, 219)
        )
        buttons.forEachIndexed { i, rect ->
            paint.color = colors[i]
            canvas.drawRoundRect(rect, 24f, 24f, paint)
            paint.color = Color.WHITE
            paint.textSize = width * 0.034f
            paint.isFakeBoldText = true
            canvas.drawText(labels[i], rect.centerX(), rect.centerY() + width * 0.012f, paint)
            paint.isFakeBoldText = false
        }
    }

    private fun finishButtons(): List<RectF> {
        val gap = width * 0.025f
        val left = width * 0.07f
        val totalW = width * 0.86f
        val buttonW = (totalW - gap * 2f) / 3f
        val top = height * 0.70f
        val bottom = height * 0.79f
        return List(3) { i ->
            RectF(left + i * (buttonW + gap), top, left + i * (buttonW + gap) + buttonW, bottom)
        }
    }

    private fun drawKnife(canvas: Canvas, cx: Float, cy: Float) {
        canvas.save()
        canvas.rotate(-18f, cx, cy)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(45, 45, 45)
        val bladeOutline = Path().apply {
            moveTo(cx - width * 0.16f, cy)
            lineTo(cx + width * 0.04f, cy - width * 0.045f)
            lineTo(cx + width * 0.04f, cy + width * 0.045f)
            close()
        }
        canvas.drawPath(bladeOutline, paint)
        paint.color = Color.rgb(224, 229, 231)
        val blade = Path().apply {
            moveTo(cx - width * 0.145f, cy)
            lineTo(cx + width * 0.035f, cy - width * 0.034f)
            lineTo(cx + width * 0.035f, cy + width * 0.034f)
            close()
        }
        canvas.drawPath(blade, paint)
        paint.color = Color.rgb(65, 137, 86)
        canvas.drawRoundRect(RectF(cx + width * 0.02f, cy - width * 0.035f, cx + width * 0.13f, cy + width * 0.035f), 14f, 14f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width * 0.008f
        paint.color = Color.rgb(45,45,45)
        canvas.drawRoundRect(RectF(cx + width * 0.02f, cy - width * 0.035f, cx + width * 0.13f, cy + width * 0.035f), 14f, 14f, paint)
        paint.style = Paint.Style.FILL
        canvas.restore()
    }

    private fun drawSpatula(canvas: Canvas, cx: Float, cy: Float) {
        canvas.save()
        canvas.rotate(-28f, cx, cy)
        paint.color = Color.rgb(50,50,50)
        canvas.drawRoundRect(RectF(cx - width*.075f, cy - width*.07f, cx + width*.055f, cy + width*.06f), 14f,14f,paint)
        paint.color = Color.rgb(184,190,193)
        canvas.drawRoundRect(RectF(cx - width*.06f, cy - width*.055f, cx + width*.04f, cy + width*.045f), 12f,12f,paint)
        paint.strokeWidth = width*.012f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(170,64,55)
        canvas.drawLine(cx + width*.05f, cy, cx + width*.19f, cy, paint)
        paint.strokeCap = Paint.Cap.BUTT
        canvas.restore()
    }

    private fun drawRollingPin(canvas: Canvas, cx: Float, cy: Float) {
        paint.color = Color.rgb(167, 101, 48)
        canvas.drawRoundRect(RectF(cx - width*.15f, cy - width*.04f, cx + width*.15f, cy + width*.04f), 24f,24f,paint)
        paint.strokeWidth = width*.018f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(115,75,41)
        canvas.drawLine(cx - width*.19f,cy,cx - width*.14f,cy,paint)
        canvas.drawLine(cx + width*.14f,cy,cx + width*.19f,cy,paint)
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun drawSpoon(canvas: Canvas, cx: Float, cy: Float) {
        canvas.save()
        canvas.rotate(-25f,cx,cy)
        paint.color = Color.rgb(65,65,65)
        canvas.drawOval(RectF(cx-width*.055f,cy-width*.045f,cx+width*.035f,cy+width*.045f),paint)
        paint.color = Color.rgb(210,214,216)
        canvas.drawOval(RectF(cx-width*.045f,cy-width*.035f,cx+width*.025f,cy+width*.035f),paint)
        paint.strokeWidth=width*.018f
        paint.strokeCap=Paint.Cap.ROUND
        paint.color=Color.rgb(85,85,85)
        canvas.drawLine(cx+width*.02f,cy,cx+width*.18f,cy,paint)
        paint.strokeCap=Paint.Cap.BUTT
        canvas.restore()
    }

    private fun drawPizzaCutter(canvas: Canvas, cx: Float, cy: Float) {
        paint.color = Color.rgb(48,48,48)
        canvas.drawCircle(cx - width*.035f, cy, width*.07f, paint)
        paint.color = Color.rgb(215,219,220)
        canvas.drawCircle(cx - width*.035f, cy, width*.055f, paint)
        paint.strokeWidth = width*.02f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(65,137,86)
        canvas.drawLine(cx + width*.02f, cy, cx + width*.15f, cy + width*.03f, paint)
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun toolHome(step: RecipeStep): Pair<Float, Float> = when {
        step.action == ActionType.CUT -> {
            val target = targetRect()
            (target.right + width * 0.04f) to target.centerY()
        }
        step.action == ActionType.COOK && step.tool != "oven" -> width * 0.72f to height * 0.61f
        step.action == ActionType.SPREAD -> width * 0.65f to height * 0.66f
        step.action == ActionType.FLIP -> width * 0.72f to height * 0.61f
        else -> width / 2f to height * 0.80f
    }

    private fun ingredientHome(): Pair<Float, Float> = width / 2f to height * 0.80f

    private fun near(x: Float, y: Float, cx: Float, cy: Float, radius: Float): Boolean =
        hypot((x - cx).toDouble(), (y - cy).toDouble()) <= radius

    private fun canGrabTool(step: RecipeStep, x: Float, y: Float): Boolean {
        val (hx, hy) = if (toolX == 0f && toolY == 0f) toolHome(step) else toolX to toolY
        return near(x, y, hx, hy, width * 0.20f)
    }

    private fun canGrabIngredient(x: Float, y: Float): Boolean {
        val (hx, hy) = if (dragX == 0f && dragY == 0f) ingredientHome() else dragX to dragY
        return near(x, y, hx, hy, width * 0.22f)
    }

    private fun successPauseMs(step: RecipeStep): Long = when (step.action) {
        ActionType.CUT -> 850L
        ActionType.COOK, ActionType.SPREAD -> 750L
        else -> 550L
    }

    private fun completeStepAfterPause(step: RecipeStep) {
        if (transitioning) return
        transitioning = true
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        invalidate()

        postDelayed({
            session.completeCurrentStep()
            resetInteraction()
            transitioning = false
            invalidate()
        }, successPauseMs(step))
    }

    private fun addContinuousProgress(step: RecipeStep, dx: Float, dy: Float) {
        val qualifyingDistance = when {
            step.action == ActionType.SPREAD && step.tool == "rolling_pin" -> abs(dx)
            else -> hypot(dx.toDouble(), dy.toDouble()).toFloat()
        }
        val unitDistance = when (step.action) {
            ActionType.COOK -> width * 1.05f
            ActionType.SPREAD -> width * 0.95f
            else -> width
        }
        gestureProgress += qualifyingDistance / unitDistance

        while (gestureProgress >= 1f && !transitioning) {
            gestureProgress -= 1f
            repetitions += 1
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            if (repetitions >= step.repeat) {
                gestureProgress = 0f
                completeStepAfterPause(step)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && event.x < width * 0.16f && event.y < height * 0.13f) {
            onBack()
            return true
        }

        if (session.isFinished) {
            if (event.action == MotionEvent.ACTION_UP) {
                val buttons = finishButtons()
                when {
                    buttons[0].contains(event.x, event.y) -> {
                        session.reset()
                        resetInteraction()
                        invalidate()
                    }
                    buttons[1].contains(event.x, event.y) -> onBack()
                    buttons[2].contains(event.x, event.y) -> onPrint()
                }
            }
            return true
        }

        if (transitioning) return true

        val step = session.currentStep ?: return true

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                lastX = event.x
                lastY = event.y
                enteredTarget = false
                cutTravel = 0f

                activeDrag = when {
                    step.action == ActionType.PLACE ||
                        step.action == ActionType.POUR ||
                        (step.action == ActionType.COOK && step.tool == "oven") -> {
                        if (canGrabIngredient(event.x, event.y)) {
                            dragX = event.x
                            dragY = event.y
                            true
                        } else false
                    }

                    step.action == ActionType.CUT ||
                        step.action == ActionType.COOK ||
                        step.action == ActionType.SPREAD ||
                        step.action == ActionType.FLIP -> {
                        if (canGrabTool(step, event.x, event.y)) {
                            toolX = event.x
                            toolY = event.y
                            true
                        } else false
                    }

                    step.action == ActionType.ROLL -> true
                    else -> false
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (!activeDrag) return true

                val dx = event.x - lastX
                val dy = event.y - lastY

                when {
                    step.action == ActionType.PLACE ||
                        step.action == ActionType.POUR ||
                        (step.action == ActionType.COOK && step.tool == "oven") -> {
                        dragX = event.x
                        dragY = event.y
                    }

                    step.action == ActionType.CUT -> {
                        toolX = event.x
                        toolY = event.y
                        if (targetRect().contains(event.x, event.y)) {
                            enteredTarget = true
                            cutTravel += hypot(dx.toDouble(), dy.toDouble()).toFloat()
                        }
                    }

                    step.action == ActionType.COOK || step.action == ActionType.SPREAD -> {
                        toolX = event.x
                        toolY = event.y
                        if (workArea.contains(event.x, event.y)) {
                            addContinuousProgress(step, dx, dy)
                        }
                    }

                    step.action == ActionType.FLIP -> {
                        toolX = event.x
                        toolY = event.y
                    }
                }

                lastX = event.x
                lastY = event.y
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                if (!activeDrag) return true

                when {
                    step.action == ActionType.PLACE || step.action == ActionType.POUR -> {
                        if (dishArea.contains(event.x, event.y)) {
                            repetitions += 1
                            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            if (repetitions >= step.repeat) completeStepAfterPause(step)
                            else {
                                dragX = 0f
                                dragY = 0f
                            }
                        } else {
                            dragX = 0f
                            dragY = 0f
                        }
                    }

                    step.action == ActionType.COOK && step.tool == "oven" -> {
                        val ovenInside = RectF(width * 0.22f, height * 0.35f, width * 0.78f, height * 0.59f)
                        if (ovenInside.contains(event.x, event.y)) {
                            repetitions = step.repeat
                            completeStepAfterPause(step)
                        } else {
                            dragX = 0f
                            dragY = 0f
                        }
                    }

                    step.action == ActionType.CUT -> {
                        val minStroke = if (step.tool == "pizza_cutter") width * 0.18f else height * 0.10f
                        if (enteredTarget && cutTravel >= minStroke) {
                            repetitions += 1
                            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            if (repetitions >= step.repeat) completeStepAfterPause(step)
                        }
                        toolX = 0f
                        toolY = 0f
                        cutTravel = 0f
                        enteredTarget = false
                    }

                    step.action == ActionType.FLIP -> {
                        if (downY - event.y > height * 0.13f) {
                            repetitions += 1
                            completeStepAfterPause(step)
                        } else {
                            toolX = 0f
                            toolY = 0f
                        }
                    }

                    step.action == ActionType.ROLL -> {
                        if (downY - event.y > height * 0.13f) {
                            repetitions += 1
                            completeStepAfterPause(step)
                        }
                    }
                }

                activeDrag = false
                invalidate()
            }
        }
        return true
    }

    private fun resetInteraction() {
        repetitions = 0
        gestureProgress = 0f
        cutTravel = 0f
        dragX = 0f
        dragY = 0f
        toolX = 0f
        toolY = 0f
        activeDrag = false
        enteredTarget = false
    }

}