package com.foodmaker.app.ui.scenes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.foodmaker.app.model.ActionType
import com.foodmaker.app.model.PartDefinition
import com.foodmaker.app.model.RecipeStep
import com.foodmaker.app.ui.FoodPainter
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

interface CookingScene {
    val instruction: String
    fun draw(canvas: Canvas)
    fun onTouch(event: MotionEvent): Boolean
}

abstract class BaseCookingScene(
    protected val host: View,
    protected val step: RecipeStep,
    protected val parts: Map<String, PartDefinition>,
    protected val dishParts: List<String>,
    private val onComplete: () -> Unit
) : CookingScene {
    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val w: Int get() = host.width
    protected val h: Int get() = host.height
    protected val assets get() = host.context.assets
    private var completed = false

    protected fun part(id: String?): PartDefinition? = id?.let(parts::get)

    protected fun near(x: Float, y: Float, cx: Float, cy: Float, r: Float): Boolean =
        hypot((x - cx).toDouble(), (y - cy).toDouble()) <= r

    protected fun haptic(tick: Boolean = true) {
        host.performHapticFeedback(
            if (tick) HapticFeedbackConstants.CLOCK_TICK
            else HapticFeedbackConstants.VIRTUAL_KEY
        )
    }

    protected fun finish() {
        if (completed) return
        completed = true
        haptic(false)
        onComplete()
        host.invalidate()
    }

    protected fun drawMorph(
        canvas: Canvas,
        inputId: String,
        outputId: String?,
        rect: RectF,
        progress: Float
    ) {
        val input = part(inputId)
        val output = part(outputId)
        if (input != null) {
            FoodPainter.drawPart(
                canvas, input, rect, assets,
                (255 * (1f - progress * 0.75f)).toInt().coerceIn(60, 255)
            )
        }
        if (output != null && progress > 0f) {
            FoodPainter.drawPart(
                canvas, output, rect, assets,
                (255 * progress).toInt().coerceIn(0, 255)
            )
        }
    }
}

class CutScene(
    host: View,
    step: RecipeStep,
    parts: Map<String, PartDefinition>,
    dishParts: List<String>,
    onComplete: () -> Unit
) : BaseCookingScene(host, step, parts, dishParts, onComplete) {
    private var cuts = 0
    private var active = false
    private var toolX = 0f
    private var toolY = 0f
    private var downY = 0f
    private var travel = 0f
    private var touchedTarget = false
    private var done = false

    override val instruction: String
        get() = when {
            done -> "잘 잘랐어요!"
            step.tool == "pizza_cutter" -> "피자 커터로 한 조각씩 잘라요"
            else -> "칼을 잡고 한 번씩 썰어요"
        }

    private fun visualArea(): RectF {
        return if (step.tool == "pizza_cutter") {
            val board = SceneArt.workArea(w, h)
            val side = min(board.width() * 0.68f, board.height() * 0.78f)
            RectF(
                board.centerX() - side / 2f,
                board.centerY() - side / 2f,
                board.centerX() + side / 2f,
                board.centerY() + side / 2f
            )
        } else {
            val board = SceneArt.workArea(w, h)
            RectF(
                board.left + board.width() * 0.16f,
                board.top + board.height() * 0.22f,
                board.right - board.width() * 0.16f,
                board.bottom - board.height() * 0.22f
            )
        }
    }

    private fun toolHome(): Pair<Float, Float> {
        val r = visualArea()
        return if (step.tool == "pizza_cutter") {
            r.right + w * 0.06f to r.top + r.height() * 0.22f
        } else {
            r.right + w * 0.08f to r.top + r.height() * 0.16f
        }
    }

    override fun draw(canvas: Canvas) {
        SceneArt.drawBoard(canvas, w, h)

        if (step.tool == "pizza_cutter") {
            drawPizzaCut(canvas)
        } else {
            drawSlicedIngredient(canvas)
        }

        if (!done) {
            val (hx, hy) = toolHome()
            val x = if (toolX == 0f) hx else toolX
            val y = if (toolY == 0f) hy else toolY
            if (step.tool == "pizza_cutter") {
                SceneArt.drawPizzaCutter(canvas, w, x, y)
            } else {
                SceneArt.drawKnife(canvas, w, x, y)
            }
        }
    }

    private fun drawSlicedIngredient(canvas: Canvas) {
        val area = visualArea()
        val input = part(step.input) ?: return
        val output = part(step.output)

        if (cuts == 0 || output == null) {
            val side = min(area.width(), area.height()) * 0.78f
            val whole = RectF(
                area.centerX() - side / 2f,
                area.centerY() - side / 2f,
                area.centerX() + side / 2f,
                area.centerY() + side / 2f
            )
            FoodPainter.drawPart(canvas, input, whole, assets)
            return
        }

        val groupWidth = area.width() * 0.86f
        val groupLeft = area.centerX() - groupWidth / 2f
        val groupRight = area.centerX() + groupWidth / 2f
        val wholeSide = min(area.height() * 0.72f, groupWidth * 0.50f)
        val wholeRect = RectF(
            groupLeft,
            area.centerY() - wholeSide / 2f,
            groupLeft + wholeSide,
            area.centerY() + wholeSide / 2f
        )

        val removedFraction = (cuts.toFloat() / step.repeat.coerceAtLeast(1)) * 0.44f
        val visibleFraction = (1f - removedFraction).coerceAtLeast(0.52f)
        val clipRight = wholeRect.left + wholeRect.width() * visibleFraction

        canvas.save()
        canvas.clipRect(
            wholeRect.left - 2f,
            wholeRect.top - 2f,
            clipRight,
            wholeRect.bottom + 2f
        )
        FoodPainter.drawPart(canvas, input, wholeRect, assets)
        canvas.restore()

        val pieceSide = min(area.height() * 0.31f, groupWidth * 0.19f)
        val gap = pieceSide * 0.12f
        val startX = clipRight + gap + pieceSide / 2f
        val availableRight = groupRight - pieceSide / 2f

        repeat(cuts) { i ->
            val cx = (startX + i * (pieceSide * 0.72f + gap)).coerceAtMost(availableRight)
            val stagger = if (i % 2 == 0) -pieceSide * 0.04f else pieceSide * 0.05f
            val cy = area.centerY() + stagger
            FoodPainter.drawPart(
                canvas,
                output,
                RectF(
                    cx - pieceSide / 2f,
                    cy - pieceSide / 2f,
                    cx + pieceSide / 2f,
                    cy + pieceSide / 2f
                ),
                assets
            )
        }

        paint.color = Color.argb(80, 120, 70, 35)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.004f
        canvas.drawLine(
            clipRight,
            wholeRect.top + wholeRect.height() * 0.14f,
            clipRight,
            wholeRect.bottom - wholeRect.height() * 0.14f,
            paint
        )
        paint.style = Paint.Style.FILL
    }

    private fun drawPizzaCut(canvas: Canvas) {
        val pizza = part(step.input) ?: return
        val r = visualArea()
        val cx = r.centerX()
        val cy = r.centerY()
        val radius = min(r.width(), r.height()) / 2f
        val segments = step.repeat.coerceAtLeast(4)
        val baseStart = -90f
        val gapDistance = radius * 0.085f

        repeat(segments) { i ->
            val startDeg = baseStart + 360f * i / segments
            val endDeg = baseStart + 360f * (i + 1) / segments
            val midRad = Math.toRadians(((startDeg + endDeg) / 2f).toDouble())
            val separated = i < cuts
            val dx = if (separated) (cos(midRad) * gapDistance).toFloat() else 0f
            val dy = if (separated) (sin(midRad) * gapDistance).toFloat() else 0f

            val wedge = Path().apply {
                moveTo(cx, cy)
                arcTo(r, startDeg, endDeg - startDeg, false)
                close()
            }

            canvas.save()
            canvas.translate(dx, dy)
            canvas.clipPath(wedge)
            FoodPainter.drawPart(canvas, pizza, r, assets)
            canvas.restore()
        }
    }

    override fun onTouch(event: MotionEvent): Boolean {
        if (done) return true
        val (hx, hy) = if (toolX == 0f) toolHome() else toolX to toolY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!near(event.x, event.y, hx, hy, w * 0.20f)) return true
                active = true
                downY = event.y
                toolX = event.x
                toolY = event.y
                travel = 0f
                touchedTarget = false
            }

            MotionEvent.ACTION_MOVE -> if (active) {
                val dy = event.y - toolY
                toolX = event.x
                toolY = event.y
                travel += abs(dy)
                if (visualArea().contains(event.x, event.y)) touchedTarget = true
                host.invalidate()
            }

            MotionEvent.ACTION_UP -> if (active) {
                val minTravel =
                    if (step.tool == "pizza_cutter") w * 0.16f else h * 0.075f

                if (
                    touchedTarget &&
                    travel >= minTravel &&
                    abs(event.y - downY) >= h * 0.045f
                ) {
                    cuts += 1
                    haptic()
                    toolX = 0f
                    toolY = 0f
                    if (cuts >= step.repeat) {
                        done = true
                        finish()
                    }
                } else {
                    toolX = 0f
                    toolY = 0f
                }

                active = false
                host.invalidate()
            }
        }
        return true
    }
}

class DoughScene(
    host: View,
    step: RecipeStep,
    parts: Map<String, PartDefinition>,
    dishParts: List<String>,
    onComplete: () -> Unit
) : BaseCookingScene(host, step, parts, dishParts, onComplete) {
    private var progress = 0f
    private var active = false
    private var rollerY = 0f
    private var lastY = 0f
    private var done = false

    override val instruction: String
        get() =
            if (done) "도우가 완성됐어요!"
            else "가로 밀대를 위아래로 굴려 반죽을 펴요"

    private fun rollerHome() = w * 0.50f to h * 0.68f

    override fun draw(canvas: Canvas) {
        SceneArt.drawBoard(canvas, w, h)
        val centerX = w / 2f
        val centerY = h * 0.50f

        if (done) {
            val r = RectF(w * 0.20f, h * 0.34f, w * 0.80f, h * 0.66f)
            part(step.output)?.let { FoodPainter.drawPart(canvas, it, r, assets) }
        } else {
            val baseSize = w * 0.22f
            val growW = baseSize + w * 0.42f * progress
            val growH = baseSize + w * 0.32f * progress
            val outputRect = RectF(
                centerX - growW / 2f,
                centerY - growH / 2f,
                centerX + growW / 2f,
                centerY + growH / 2f
            )

            if (progress < 0.08f) {
                val r = RectF(
                    centerX - baseSize / 2f,
                    centerY - baseSize / 2f,
                    centerX + baseSize / 2f,
                    centerY + baseSize / 2f
                )
                part(step.input)?.let { FoodPainter.drawPart(canvas, it, r, assets) }
            } else {
                drawMorph(canvas, step.input, step.output, outputRect, progress)
            }
        }

        val (_, hy) = rollerHome()
        SceneArt.drawRollingPin(
            canvas,
            w,
            w * 0.50f,
            if (rollerY == 0f) hy else rollerY
        )
    }

    override fun onTouch(event: MotionEvent): Boolean {
        if (done) return true
        val (_, hy) = rollerHome()
        val currentY = if (rollerY == 0f) hy else rollerY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!near(event.x, event.y, w * 0.50f, currentY, w * 0.22f)) return true
                active = true
                rollerY = event.y
                lastY = event.y
            }

            MotionEvent.ACTION_MOVE -> if (active) {
                val dy = event.y - lastY
                rollerY = event.y.coerceIn(
                    SceneArt.workArea(w, h).top,
                    SceneArt.workArea(w, h).bottom
                )

                if (SceneArt.workArea(w, h).contains(w * 0.50f, rollerY)) {
                    progress = (
                        progress +
                            abs(dy) / (h * step.repeat * 0.14f)
                        ).coerceAtMost(1f)

                    if (progress >= 1f) {
                        done = true
                        finish()
                    }
                }

                lastY = event.y
                host.invalidate()
            }

            MotionEvent.ACTION_UP -> active = false
        }
        return true
    }
}

class SauceScene(
    host: View,
    step: RecipeStep,
    parts: Map<String, PartDefinition>,
    dishParts: List<String>,
    onComplete: () -> Unit
) : BaseCookingScene(host, step, parts, dishParts, onComplete) {
    private var progress = 0f
    private var active = false
    private var spoonX = 0f
    private var spoonY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var done = false
    private val trail = mutableListOf<PointF>()

    override val instruction: String
        get() =
            if (done) "소스를 골고루 발랐어요!"
            else "숟가락으로 빙글빙글 소스를 펴요"

    private fun spoonHome() = w * 0.68f to h * 0.68f
    private fun pizzaRect() =
        RectF(w * 0.22f, h * 0.34f, w * 0.78f, h * 0.66f)

    override fun draw(canvas: Canvas) {
        SceneArt.drawBoard(canvas, w, h)
        val r = pizzaRect()
        part(step.input)?.let { FoodPainter.drawPart(canvas, it, r, assets) }

        if (progress > 0f) {
            part(step.output)?.let {
                FoodPainter.drawPart(
                    canvas,
                    it,
                    r,
                    assets,
                    (50 + 205 * progress).toInt()
                )
            }
        }

        paint.color = Color.argb(130, 190, 62, 44)
        trail.forEach { p ->
            canvas.drawCircle(p.x, p.y, w * 0.035f, paint)
        }

        val (hx, hy) = spoonHome()
        SceneArt.drawSpoon(
            canvas,
            w,
            if (spoonX == 0f) hx else spoonX,
            if (spoonY == 0f) hy else spoonY
        )
    }

    override fun onTouch(event: MotionEvent): Boolean {
        if (done) return true
        val (hx, hy) =
            if (spoonX == 0f) spoonHome()
            else spoonX to spoonY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!near(event.x, event.y, hx, hy, w * 0.20f)) return true
                active = true
                spoonX = event.x
                spoonY = event.y
                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_MOVE -> if (active) {
                val dx = event.x - lastX
                val dy = event.y - lastY
                spoonX = event.x
                spoonY = event.y

                if (pizzaRect().contains(event.x, event.y)) {
                    progress = (
                        progress +
                            hypot(dx.toDouble(), dy.toDouble()).toFloat() /
                            (w * step.repeat * 0.90f)
                        ).coerceAtMost(1f)

                    if (
                        trail.isEmpty() ||
                        hypot(
                            (event.x - trail.last().x).toDouble(),
                            (event.y - trail.last().y).toDouble()
                        ) > w * 0.045f
                    ) {
                        trail += PointF(event.x, event.y)
                        if (trail.size > 18) trail.removeAt(0)
                    }

                    if (progress >= 1f) {
                        done = true
                        finish()
                    }
                }

                lastX = event.x
                lastY = event.y
                host.invalidate()
            }

            MotionEvent.ACTION_UP -> active = false
        }
        return true
    }
}

class GrillScene(
    host: View,
    step: RecipeStep,
    parts: Map<String, PartDefinition>,
    dishParts: List<String>,
    onComplete: () -> Unit
) : BaseCookingScene(host, step, parts, dishParts, onComplete) {
    private var cook = 0f
    private var phase = 0
    private var active = false
    private var spatX = 0f
    private var spatY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var underPatty = false
    private var liftStartY = 0f
    private var flipping = false
    private var flipStart = 0L

    override val instruction: String
        get() = when {
            flipping -> "휙!"
            phase == 0 -> "뒤집개를 움직이며 한쪽을 익혀요"
            phase == 1 && !underPatty -> "뒤집개를 패티 밑으로 밀어 넣어요"
            phase == 1 && underPatty -> "그대로 위로 들어 올려요"
            phase == 2 -> "반대쪽도 노릇하게 익혀요"
            else -> "패티가 맛있게 익었어요!"
        }

    private fun pattyRect() =
        RectF(w * 0.30f, h * 0.405f, w * 0.70f, h * 0.61f)

    private fun spatHome() = w * 0.73f to h * 0.63f

    override fun draw(canvas: Canvas) {
        SceneArt.drawPan(canvas, w, h)

        if (flipping) {
            drawFlipAnimation(canvas)
        } else {
            drawMorph(canvas, step.input, step.output, pattyRect(), cook)
        }

        if (phase == 0 || phase == 2) {
            paint.color = Color.argb(170, 255, 238, 180)
            repeat(5) { i ->
                val angle = i * 1.3f + cook * 3f
                val x = w * 0.5f + cos(angle) * w * 0.12f
                val y = h * 0.51f + sin(angle) * w * 0.08f
                canvas.drawCircle(x, y, w * 0.009f, paint)
            }
        }

        if (phase == 1 && !flipping) {
            drawFlipHint(canvas)
        }

        if (!flipping && phase < 3) {
            val (hx, hy) = spatHome()
            SceneArt.drawSpatula(
                canvas,
                w,
                if (spatX == 0f) hx else spatX,
                if (spatY == 0f) hy else spatY
            )
        }
    }

    private fun drawFlipHint(canvas: Canvas) {
        val r = pattyRect()
        paint.color = Color.argb(55, 70, 145, 220)
        canvas.drawRoundRect(
            RectF(
                r.left + r.width() * 0.14f,
                r.centerY(),
                r.right - r.width() * 0.14f,
                r.bottom + h * 0.03f
            ),
            22f,
            22f,
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.012f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.argb(145, 60, 130, 210)
        canvas.drawLine(
            r.centerX(),
            r.bottom + h * 0.06f,
            r.centerX(),
            r.top - h * 0.025f,
            paint
        )
        paint.style = Paint.Style.FILL
        val arrow = Path().apply {
            moveTo(r.centerX(), r.top - h * 0.04f)
            lineTo(r.centerX() - w * 0.035f, r.top + h * 0.015f)
            lineTo(r.centerX() + w * 0.035f, r.top + h * 0.015f)
            close()
        }
        canvas.drawPath(arrow, paint)
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun drawFlipAnimation(canvas: Canvas) {
        val elapsed = (System.currentTimeMillis() - flipStart).coerceAtLeast(0L)
        val t = (elapsed / 520f).coerceIn(0f, 1f)
        val r = pattyRect()
        val lift = -sin(PI.toFloat() * t) * w * 0.11f
        val scaleY = abs(cos(PI.toFloat() * t)).coerceAtLeast(0.10f)

        canvas.save()
        canvas.translate(r.centerX(), r.centerY() + lift)
        canvas.scale(1f, scaleY)
        val local = RectF(
            -r.width() / 2f,
            -r.height() / 2f,
            r.width() / 2f,
            r.height() / 2f
        )
        drawMorph(canvas, step.input, step.output, local, 0.55f)
        canvas.restore()

        if (t < 1f) {
            host.postInvalidateOnAnimation()
        } else {
            flipping = false
            phase = 2
            cook = 0.55f
            underPatty = false
            spatX = 0f
            spatY = 0f
            host.invalidate()
        }
    }

    private fun startFlip() {
        if (flipping) return
        flipping = true
        flipStart = System.currentTimeMillis()
        active = false
        haptic(false)
        host.postInvalidateOnAnimation()
    }

    override fun onTouch(event: MotionEvent): Boolean {
        if (phase == 3 || flipping) return true
        val (hx, hy) =
            if (spatX == 0f) spatHome()
            else spatX to spatY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!near(event.x, event.y, hx, hy, w * 0.22f)) return true
                active = true
                spatX = event.x
                spatY = event.y
                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_MOVE -> if (active) {
                val dx = event.x - lastX
                val dy = event.y - lastY

                if (phase == 1) {
                    val r = pattyRect()

                    if (!underPatty) {
                        spatX = event.x
                        spatY = event.y
                        val underZone = RectF(
                            r.left,
                            r.centerY(),
                            r.right,
                            r.bottom + h * 0.05f
                        )
                        if (underZone.contains(event.x, event.y)) {
                            underPatty = true
                            liftStartY = event.y
                            spatX = r.centerX()
                            spatY = r.bottom - r.height() * 0.12f
                            haptic()
                        }
                    } else {
                        spatX = r.centerX()
                        spatY = event.y.coerceAtMost(r.bottom)
                        if (liftStartY - event.y > h * 0.065f) {
                            startFlip()
                        }
                    }
                } else {
                    spatX = event.x
                    spatY = event.y

                    if (SceneArt.workArea(w, h).contains(event.x, event.y)) {
                        val delta =
                            hypot(dx.toDouble(), dy.toDouble()).toFloat() /
                                (w * step.repeat * 0.75f)

                        if (phase == 0) {
                            cook = (cook + delta).coerceAtMost(0.48f)
                            if (cook >= 0.48f) {
                                phase = 1
                                active = false
                                spatX = 0f
                                spatY = 0f
                                haptic()
                            }
                        } else if (phase == 2) {
                            cook = (cook + delta).coerceAtMost(1f)
                            if (cook >= 1f) {
                                phase = 3
                                finish()
                            }
                        }
                    }
                }

                lastX = event.x
                lastY = event.y
                host.invalidate()
            }

            MotionEvent.ACTION_UP -> if (active) {
                active = false
                if (phase != 1 || !underPatty) {
                    spatX = 0f
                    spatY = 0f
                }
                host.invalidate()
            }
        }
        return true
    }
}

class AssemblyScene(
    host: View,
    step: RecipeStep,
    parts: Map<String, PartDefinition>,
    dishParts: List<String>,
    onComplete: () -> Unit
) : BaseCookingScene(host, step, parts, dishParts, onComplete) {
    private var active = false
    private var itemX = 0f
    private var itemY = 0f
    private var done = false
    private val placedPoints = mutableListOf<PointF>()

    override val instruction: String
        get() = if (done) "잘 올렸어요!" else step.instruction

    private fun home() = w * 0.50f to h * 0.80f

    private fun itemSize(): Float =
        w * (step.itemScale ?: if (step.input == "pepperoni") 0.11f else 0.30f)

    private fun slotArea(): RectF {
        val dish = SceneArt.dishArea(w, h)
        val side = min(dish.width(), dish.height()) * 0.90f
        return RectF(
            dish.centerX() - side / 2f,
            dish.centerY() - side / 2f,
            dish.centerX() + side / 2f,
            dish.centerY() + side / 2f
        )
    }

    private fun slots(): List<PointF> {
        val r = slotArea()
        return listOf(
            PointF(r.centerX() - r.width() * 0.20f, r.centerY() - r.height() * 0.19f),
            PointF(r.centerX() + r.width() * 0.20f, r.centerY() - r.height() * 0.19f),
            PointF(r.centerX(), r.centerY()),
            PointF(r.centerX() - r.width() * 0.20f, r.centerY() + r.height() * 0.20f),
            PointF(r.centerX() + r.width() * 0.20f, r.centerY() + r.height() * 0.20f)
        )
    }

    override fun draw(canvas: Canvas) {
        SceneArt.drawPlate(canvas, w, h)
        val dish = SceneArt.dishArea(w, h)
        FoodPainter.drawDish(canvas, dishParts, parts, dish, assets)

        val p = part(step.input)
        if (p != null) {
            if (step.placementMode == "slots") {
                val allSlots = slots()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = w * 0.004f
                paint.color = Color.argb(90, 95, 135, 95)

                for (i in placedPoints.size until min(step.repeat, allSlots.size)) {
                    val s = allSlots[i]
                    canvas.drawCircle(s.x, s.y, itemSize() * 0.38f, paint)
                }
                paint.style = Paint.Style.FILL
            }

            placedPoints.forEach { point ->
                val size = itemSize()
                FoodPainter.drawPart(
                    canvas,
                    p,
                    RectF(
                        point.x - size / 2f,
                        point.y - size / 2f,
                        point.x + size / 2f,
                        point.y + size / 2f
                    ),
                    assets
                )
            }

            if (!done) {
                val (hx, hy) = home()
                val cx = if (itemX == 0f) hx else itemX
                val cy = if (itemY == 0f) hy else itemY
                val size = itemSize()
                FoodPainter.drawPart(
                    canvas,
                    p,
                    RectF(
                        cx - size / 2f,
                        cy - size / 2f,
                        cx + size / 2f,
                        cy + size / 2f
                    ),
                    assets
                )
            }
        }
    }

    override fun onTouch(event: MotionEvent): Boolean {
        if (done) return true
        val (hx, hy) =
            if (itemX == 0f) home()
            else itemX to itemY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!near(event.x, event.y, hx, hy, w * 0.23f)) return true
                active = true
                itemX = event.x
                itemY = event.y
            }

            MotionEvent.ACTION_MOVE -> if (active) {
                itemX = event.x
                itemY = event.y
                host.invalidate()
            }

            MotionEvent.ACTION_UP -> if (active) {
                val acceptedPoint =
                    if (step.placementMode == "slots") {
                        val area = slotArea()
                        if (!area.contains(event.x, event.y)) null
                        else {
                            val available = slots()
                                .take(step.repeat)
                                .drop(placedPoints.size)
                            available.minByOrNull { point ->
                                hypot(
                                    (event.x - point.x).toDouble(),
                                    (event.y - point.y).toDouble()
                                )
                            }
                        }
                    } else {
                        val dish = SceneArt.dishArea(w, h)
                        if (dish.contains(event.x, event.y)) {
                            PointF(dish.centerX(), dish.centerY())
                        } else null
                    }

                if (acceptedPoint != null) {
                    placedPoints += acceptedPoint
                    haptic()

                    if (placedPoints.size >= step.repeat) {
                        done = true
                        finish()
                    } else {
                        itemX = 0f
                        itemY = 0f
                    }
                } else {
                    itemX = 0f
                    itemY = 0f
                }

                active = false
                host.invalidate()
            }
        }
        return true
    }
}

class OvenScene(
    host: View,
    step: RecipeStep,
    parts: Map<String, PartDefinition>,
    dishParts: List<String>,
    onComplete: () -> Unit
) : BaseCookingScene(host, step, parts, dishParts, onComplete) {
    private var phase = 0
    private var active = false
    private var itemX = 0f
    private var itemY = 0f

    override val instruction: String
        get() = when (phase) {
            0 -> "피자를 잡아 오븐 안으로 넣어요"
            1 -> "노릇노릇 굽는 중..."
            2 -> "다 익었어요! 피자를 꺼내요"
            else -> "맛있는 피자가 완성됐어요!"
        }

    private fun inputHome() = w * 0.50f to h * 0.80f

    private fun outputHome(): Pair<Float, Float> {
        val r = SceneArt.ovenInside(w, h)
        return r.centerX() to r.centerY()
    }

    override fun draw(canvas: Canvas) {
        SceneArt.drawOven(canvas, w, h, phase == 1 || phase == 2)
        SceneArt.drawTray(canvas, w, h)
        val inside = SceneArt.ovenInside(w, h)

        when (phase) {
            0 -> part(step.input)?.let {
                val (hx, hy) = inputHome()
                val cx = if (itemX == 0f) hx else itemX
                val cy = if (itemY == 0f) hy else itemY
                val size = w * 0.40f
                FoodPainter.drawPart(
                    canvas, it,
                    RectF(
                        cx - size / 2f,
                        cy - size / 2f,
                        cx + size / 2f,
                        cy + size / 2f
                    ),
                    assets
                )
            }

            1 -> part(step.input)?.let {
                FoodPainter.drawPart(
                    canvas, it,
                    RectF(
                        inside.centerX() - w * 0.18f,
                        inside.centerY() - w * 0.18f,
                        inside.centerX() + w * 0.18f,
                        inside.centerY() + w * 0.18f
                    ),
                    assets,
                    190
                )
            }

            2 -> part(step.output)?.let {
                val (hx, hy) = outputHome()
                val cx = if (itemX == 0f) hx else itemX
                val cy = if (itemY == 0f) hy else itemY
                val size = w * 0.40f
                FoodPainter.drawPart(
                    canvas, it,
                    RectF(
                        cx - size / 2f,
                        cy - size / 2f,
                        cx + size / 2f,
                        cy + size / 2f
                    ),
                    assets
                )
            }

            3 -> part(step.output)?.let {
                val r = SceneArt.trayArea(w, h)
                FoodPainter.drawPart(
                    canvas, it,
                    RectF(
                        r.centerX() - w * 0.20f,
                        r.centerY() - w * 0.20f,
                        r.centerX() + w * 0.20f,
                        r.centerY() + w * 0.20f
                    ),
                    assets
                )
            }
        }
    }

    override fun onTouch(event: MotionEvent): Boolean {
        if (phase == 1 || phase == 3) return true
        val home = if (phase == 0) inputHome() else outputHome()
        val hx = if (itemX == 0f) home.first else itemX
        val hy = if (itemY == 0f) home.second else itemY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!near(event.x, event.y, hx, hy, w * 0.24f)) return true
                active = true
                itemX = event.x
                itemY = event.y
            }

            MotionEvent.ACTION_MOVE -> if (active) {
                itemX = event.x
                itemY = event.y
                host.invalidate()
            }

            MotionEvent.ACTION_UP -> if (active) {
                if (
                    phase == 0 &&
                    SceneArt.ovenInside(w, h).contains(event.x, event.y)
                ) {
                    phase = 1
                    itemX = 0f
                    itemY = 0f
                    haptic()
                    host.postDelayed({
                        phase = 2
                        host.invalidate()
                    }, 1300L)
                } else if (
                    phase == 2 &&
                    SceneArt.trayArea(w, h).contains(event.x, event.y)
                ) {
                    phase = 3
                    finish()
                } else {
                    itemX = 0f
                    itemY = 0f
                }

                active = false
                host.invalidate()
            }
        }
        return true
    }
}

object CookingSceneFactory {
    fun create(
        host: View,
        step: RecipeStep,
        parts: Map<String, PartDefinition>,
        dishParts: List<String>,
        onComplete: () -> Unit
    ): CookingScene {
        return when {
            step.action == ActionType.CUT ->
                CutScene(host, step, parts, dishParts, onComplete)

            step.action == ActionType.SPREAD &&
                step.tool == "rolling_pin" ->
                DoughScene(host, step, parts, dishParts, onComplete)

            step.action == ActionType.SPREAD ->
                SauceScene(host, step, parts, dishParts, onComplete)

            step.action == ActionType.COOK &&
                step.tool == "oven" ->
                OvenScene(host, step, parts, dishParts, onComplete)

            step.action == ActionType.COOK ->
                GrillScene(host, step, parts, dishParts, onComplete)

            else ->
                AssemblyScene(host, step, parts, dishParts, onComplete)
        }
    }
}
