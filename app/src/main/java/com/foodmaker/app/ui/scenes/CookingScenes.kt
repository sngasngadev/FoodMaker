package com.foodmaker.app.ui.scenes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.foodmaker.app.model.ActionType
import com.foodmaker.app.model.PartDefinition
import com.foodmaker.app.model.RecipeStep
import com.foodmaker.app.ui.FoodPainter
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

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
        host.performHapticFeedback(if (tick) HapticFeedbackConstants.CLOCK_TICK else HapticFeedbackConstants.VIRTUAL_KEY)
    }

    protected fun finish() {
        if (completed) return
        completed = true
        haptic(false)
        onComplete()
        host.invalidate()
    }

    protected fun drawMorph(canvas: Canvas, inputId: String, outputId: String?, rect: RectF, progress: Float) {
        val input = part(inputId)
        val output = part(outputId)
        if (input != null) {
            FoodPainter.drawPart(canvas, input, rect, assets, (255 * (1f - progress * 0.75f)).toInt().coerceIn(60, 255))
        }
        if (output != null && progress > 0f) {
            FoodPainter.drawPart(canvas, output, rect, assets, (255 * progress).toInt().coerceIn(0, 255))
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
        get() = if (done) "잘 잘랐어요!" else if (step.tool == "pizza_cutter") "피자 커터로 선을 따라 잘라요" else "칼을 잡고 한 번씩 썰어요"

    private fun target(): RectF = SceneArt.targetArea(w, h)

    private fun toolHome(): Pair<Float, Float> {
        val r = target()
        val guideX = r.left + r.width() * (cuts + 1f) / (step.repeat + 1f)
        return guideX + w * 0.11f to r.top + r.height() * 0.10f
    }

    override fun draw(canvas: Canvas) {
        SceneArt.drawBoard(canvas, w, h)
        val r = target()

        if (done) {
            part(step.output)?.let { FoodPainter.drawPart(canvas, it, r, assets) }
        } else {
            part(step.input)?.let { FoodPainter.drawPart(canvas, it, r, assets) }

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = w * 0.006f
            repeat(step.repeat) { i ->
                val x = r.left + r.width() * (i + 1f) / (step.repeat + 1f)
                paint.color = if (i < cuts) Color.argb(220, 255, 255, 255) else Color.argb(90, 255, 255, 255)
                canvas.drawLine(x, r.top + r.height() * 0.10f, x, r.bottom - r.height() * 0.10f, paint)
            }
            paint.style = Paint.Style.FILL
        }

        val (hx, hy) = toolHome()
        val x = if (toolX == 0f) hx else toolX
        val y = if (toolY == 0f) hy else toolY
        if (step.tool == "pizza_cutter") SceneArt.drawPizzaCutter(canvas, w, x, y)
        else SceneArt.drawKnife(canvas, w, x, y)
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
                if (target().contains(event.x, event.y)) touchedTarget = true
                host.invalidate()
            }
            MotionEvent.ACTION_UP -> if (active) {
                val minTravel = if (step.tool == "pizza_cutter") w * 0.18f else h * 0.09f
                if (touchedTarget && travel >= minTravel && abs(event.y - downY) >= h * 0.055f) {
                    cuts += 1
                    haptic()
                    if (cuts >= step.repeat) {
                        done = true
                        finish()
                    } else {
                        toolX = 0f
                        toolY = 0f
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
    private var rollerX = 0f
    private var rollerY = 0f
    private var lastX = 0f
    private var done = false

    override val instruction: String
        get() = if (done) "도우가 완성됐어요!" else "밀대를 좌우로 굴려 반죽을 펴요"

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
            val growH = baseSize + w * 0.18f * progress
            val outputRect = RectF(centerX - growW / 2f, centerY - growH / 2f, centerX + growW / 2f, centerY + growH / 2f)
            if (progress < 0.08f) {
                val r = RectF(centerX - baseSize / 2f, centerY - baseSize / 2f, centerX + baseSize / 2f, centerY + baseSize / 2f)
                part(step.input)?.let { FoodPainter.drawPart(canvas, it, r, assets) }
            } else {
                drawMorph(canvas, step.input, step.output, outputRect, progress)
            }
        }

        val (hx, hy) = rollerHome()
        SceneArt.drawRollingPin(canvas, w, if (rollerX == 0f) hx else rollerX, if (rollerY == 0f) hy else rollerY)
    }

    override fun onTouch(event: MotionEvent): Boolean {
        if (done) return true
        val (hx, hy) = if (rollerX == 0f) rollerHome() else rollerX to rollerY
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!near(event.x, event.y, hx, hy, w * 0.22f)) return true
                active = true
                rollerX = event.x
                rollerY = event.y
                lastX = event.x
            }
            MotionEvent.ACTION_MOVE -> if (active) {
                val dx = event.x - lastX
                rollerX = event.x
                rollerY = event.y
                if (SceneArt.workArea(w, h).contains(event.x, event.y)) {
                    progress = (progress + abs(dx) / (w * step.repeat * 0.78f)).coerceAtMost(1f)
                    if (progress >= 1f) {
                        done = true
                        finish()
                    }
                }
                lastX = event.x
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
        get() = if (done) "소스를 골고루 발랐어요!" else "숟가락으로 빙글빙글 소스를 펴요"

    private fun spoonHome() = w * 0.68f to h * 0.68f
    private fun pizzaRect() = RectF(w * 0.22f, h * 0.34f, w * 0.78f, h * 0.66f)

    override fun draw(canvas: Canvas) {
        SceneArt.drawBoard(canvas, w, h)
        val r = pizzaRect()
        part(step.input)?.let { FoodPainter.drawPart(canvas, it, r, assets) }

        if (progress > 0f) {
            part(step.output)?.let { FoodPainter.drawPart(canvas, it, r, assets, (50 + 205 * progress).toInt()) }
        }

        paint.color = Color.argb(130, 190, 62, 44)
        trail.forEach { p -> canvas.drawCircle(p.x, p.y, w * 0.035f, paint) }

        val (hx, hy) = spoonHome()
        SceneArt.drawSpoon(canvas, w, if (spoonX == 0f) hx else spoonX, if (spoonY == 0f) hy else spoonY)
    }

    override fun onTouch(event: MotionEvent): Boolean {
        if (done) return true
        val (hx, hy) = if (spoonX == 0f) spoonHome() else spoonX to spoonY
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
                    progress = (progress + hypot(dx.toDouble(), dy.toDouble()).toFloat() / (w * step.repeat * 0.90f)).coerceAtMost(1f)
                    if (trail.isEmpty() || hypot((event.x - trail.last().x).toDouble(), (event.y - trail.last().y).toDouble()) > w * 0.045f) {
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
    private var phase = 0 // 0 first side, 1 flip, 2 second side, 3 done
    private var active = false
    private var spatX = 0f
    private var spatY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var downY = 0f

    override val instruction: String
        get() = when (phase) {
            0 -> "뒤집개로 패티를 움직이며 한쪽을 익혀요"
            1 -> "이제 뒤집개를 밑에 넣고 위로 밀어요"
            2 -> "반대쪽도 노릇하게 익혀요"
            else -> "패티가 맛있게 익었어요!"
        }

    private fun pattyRect() = RectF(w * 0.30f, h * 0.405f, w * 0.70f, h * 0.61f)
    private fun spatHome() = w * 0.73f to h * 0.63f

    override fun draw(canvas: Canvas) {
        SceneArt.drawPan(canvas, w, h)
        drawMorph(canvas, step.input, step.output, pattyRect(), cook)

        if (phase == 0 || phase == 2) {
            paint.color = Color.argb(170, 255, 238, 180)
            val bubbles = 5
            repeat(bubbles) { i ->
                val angle = (i * 1.3f + cook * 3f)
                val x = w * 0.5f + kotlin.math.cos(angle) * w * 0.12f
                val y = h * 0.51f + kotlin.math.sin(angle) * w * 0.08f
                canvas.drawCircle(x, y, w * 0.009f, paint)
            }
        }

        val (hx, hy) = spatHome()
        SceneArt.drawSpatula(canvas, w, if (spatX == 0f) hx else spatX, if (spatY == 0f) hy else spatY)
    }

    override fun onTouch(event: MotionEvent): Boolean {
        if (phase == 3) return true
        val (hx, hy) = if (spatX == 0f) spatHome() else spatX to spatY
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!near(event.x, event.y, hx, hy, w * 0.22f)) return true
                active = true
                spatX = event.x
                spatY = event.y
                lastX = event.x
                lastY = event.y
                downY = event.y
            }
            MotionEvent.ACTION_MOVE -> if (active) {
                val dx = event.x - lastX
                val dy = event.y - lastY
                spatX = event.x
                spatY = event.y
                if ((phase == 0 || phase == 2) && SceneArt.workArea(w, h).contains(event.x, event.y)) {
                    val delta = hypot(dx.toDouble(), dy.toDouble()).toFloat() / (w * step.repeat * 0.75f)
                    if (phase == 0) cook = (cook + delta).coerceAtMost(0.48f)
                    else cook = (cook + delta).coerceAtMost(1f)
                    if (phase == 0 && cook >= 0.48f) {
                        phase = 1
                        haptic()
                    }
                    if (phase == 2 && cook >= 1f) {
                        phase = 3
                        finish()
                    }
                }
                lastX = event.x
                lastY = event.y
                host.invalidate()
            }
            MotionEvent.ACTION_UP -> if (active) {
                if (phase == 1 && downY - event.y > h * 0.10f && pattyRect().contains(event.x, min(event.y + h * 0.08f, h.toFloat()))) {
                    phase = 2
                    cook = 0.55f
                    haptic()
                }
                active = false
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
    private var placed = 0
    private var active = false
    private var itemX = 0f
    private var itemY = 0f
    private var done = false

    override val instruction: String
        get() = if (done) "잘 올렸어요!" else step.instruction

    private fun home() = w * 0.50f to h * 0.80f

    override fun draw(canvas: Canvas) {
        SceneArt.drawPlate(canvas, w, h)
        val dish = SceneArt.dishArea(w, h)

        if (done && step.output != null) {
            part(step.output)?.let { FoodPainter.drawPart(canvas, it, dish, assets) }
        } else {
            FoodPainter.drawDish(canvas, dishParts, parts, dish, assets)

            val offsets = listOf(
                -0.18f to -0.06f,
                0.17f to -0.03f,
                -0.01f to 0.07f,
                0.18f to 0.09f,
                -0.16f to 0.10f
            )
            part(step.input)?.let { p ->
                repeat(placed.coerceAtMost(offsets.size)) { i ->
                    val (ox, oy) = offsets[i]
                    val size = if (step.input == "pepperoni") w * 0.15f else w * 0.24f
                    val cx = dish.centerX() + dish.width() * ox
                    val cy = dish.centerY() + dish.height() * oy
                    FoodPainter.drawPart(canvas, p, RectF(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f), assets)
                }

                if (!done) {
                    val (hx, hy) = home()
                    val cx = if (itemX == 0f) hx else itemX
                    val cy = if (itemY == 0f) hy else itemY
                    val size = if (step.input == "pepperoni") w * 0.16f else w * 0.30f
                    FoodPainter.drawPart(canvas, p, RectF(cx - size / 2f, cy - size * 0.38f, cx + size / 2f, cy + size * 0.38f), assets)
                }
            }
        }
    }

    override fun onTouch(event: MotionEvent): Boolean {
        if (done) return true
        val (hx, hy) = if (itemX == 0f) home() else itemX to itemY
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
                if (SceneArt.dishArea(w, h).contains(event.x, event.y)) {
                    placed += 1
                    haptic()
                    if (placed >= step.repeat) {
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
    private var phase = 0 // 0 put in, 1 baking, 2 take out, 3 done
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
                FoodPainter.drawPart(canvas, it, RectF(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f), assets)
            }
            1 -> part(step.input)?.let {
                FoodPainter.drawPart(canvas, it, RectF(inside.centerX() - w * 0.18f, inside.centerY() - w * 0.18f, inside.centerX() + w * 0.18f, inside.centerY() + w * 0.18f), assets, 190)
            }
            2 -> part(step.output)?.let {
                val (hx, hy) = outputHome()
                val cx = if (itemX == 0f) hx else itemX
                val cy = if (itemY == 0f) hy else itemY
                val size = w * 0.40f
                FoodPainter.drawPart(canvas, it, RectF(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f), assets)
            }
            3 -> part(step.output)?.let {
                val r = SceneArt.trayArea(w, h)
                FoodPainter.drawPart(canvas, it, RectF(r.centerX() - w * 0.20f, r.centerY() - w * 0.20f, r.centerX() + w * 0.20f, r.centerY() + w * 0.20f), assets)
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
                if (phase == 0 && SceneArt.ovenInside(w, h).contains(event.x, event.y)) {
                    phase = 1
                    itemX = 0f
                    itemY = 0f
                    haptic()
                    host.postDelayed({
                        phase = 2
                        host.invalidate()
                    }, 1300L)
                } else if (phase == 2 && SceneArt.trayArea(w, h).contains(event.x, event.y)) {
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

            step.action == ActionType.SPREAD && step.tool == "rolling_pin" ->
                DoughScene(host, step, parts, dishParts, onComplete)

            step.action == ActionType.SPREAD ->
                SauceScene(host, step, parts, dishParts, onComplete)

            step.action == ActionType.COOK && step.tool == "oven" ->
                OvenScene(host, step, parts, dishParts, onComplete)

            step.action == ActionType.COOK ->
                GrillScene(host, step, parts, dishParts, onComplete)

            else ->
                AssemblyScene(host, step, parts, dishParts, onComplete)
        }
    }
}
