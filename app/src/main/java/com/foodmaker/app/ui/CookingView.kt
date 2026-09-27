package com.foodmaker.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.foodmaker.app.engine.GameSession
import com.foodmaker.app.model.ActionType
import com.foodmaker.app.model.PartDefinition
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
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var dragX = 0f
    private var dragY = 0f
    private var gestureProgress = 0f
    private var repetitions = 0

    private val dishArea: RectF
        get() = RectF(width * 0.08f, height * 0.30f, width * 0.92f, height * 0.67f)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(255, 249, 238))
        drawHeader(canvas)

        if (session.isFinished) {
            drawFinished(canvas)
            return
        }

        val step = session.currentStep ?: return
        FoodPainter.drawDish(canvas, session.dishParts, parts, dishArea, context.assets)

        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.rgb(64, 53, 46)
        paint.textSize = width * 0.06f
        paint.isFakeBoldText = true
        canvas.drawText(step.instruction, width / 2f, height * 0.22f, paint)
        paint.isFakeBoldText = false

        drawWorkSurface(canvas, step.action)
        parts[step.input]?.let { interactive ->
            val defaultX = width / 2f
            val defaultY = height * 0.79f
            val cx = if (dragX == 0f) defaultX else dragX
            val cy = if (dragY == 0f) defaultY else dragY
            val size = width * 0.28f
            FoodPainter.drawPart(canvas, interactive, RectF(cx - size / 2f, cy - size * 0.35f, cx + size / 2f, cy + size * 0.35f), context.assets)
        }

        drawHint(canvas, step.action)
        drawProgress(canvas, step.repeat)
    }

    private fun drawHeader(canvas: Canvas) {
        paint.color = Color.rgb(64, 53, 46)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = width * 0.052f
        paint.isFakeBoldText = true
        canvas.drawText("‹", width * 0.06f, height * 0.075f, paint)
        canvas.drawText(session.recipe.title, width * 0.14f, height * 0.075f, paint)
        paint.isFakeBoldText = false

        val total = session.recipe.steps.size.coerceAtLeast(1)
        val ratio = session.stepIndex.toFloat() / total
        val left = width * 0.14f
        val right = width * 0.92f
        val y = height * 0.105f
        paint.color = Color.rgb(232, 220, 204)
        canvas.drawRoundRect(RectF(left, y, right, y + 14f), 7f, 7f, paint)
        paint.color = Color.rgb(238, 151, 79)
        canvas.drawRoundRect(RectF(left, y, left + (right - left) * ratio, y + 14f), 7f, 7f, paint)
    }

    private fun drawWorkSurface(canvas: Canvas, action: ActionType) {
        paint.color = when (action) {
            ActionType.COOK, ActionType.FLIP -> Color.rgb(70, 72, 75)
            ActionType.CUT -> Color.rgb(222, 190, 139)
            else -> Color.rgb(245, 232, 210)
        }
        canvas.drawRoundRect(RectF(width * 0.17f, height * 0.69f, width * 0.83f, height * 0.90f), 36f, 36f, paint)
    }

    private fun drawHint(canvas: Canvas, action: ActionType) {
        val hint = when (action) {
            ActionType.PLACE -> "재료를 음식 위로 옮겨요"
            ActionType.CUT -> "손가락으로 위아래로 썰어요"
            ActionType.COOK -> "팬 위에서 문질러 익혀요"
            ActionType.FLIP -> "위로 쓱 올려 뒤집어요"
            ActionType.SPREAD -> "빙글빙글 넓게 펴요"
            ActionType.ROLL -> "아래에서 위로 말아요"
            ActionType.POUR -> "그릇 쪽으로 옮겨 부어요"
        }
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = width * 0.035f
        paint.color = Color.rgb(130, 111, 96)
        canvas.drawText(hint, width / 2f, height * 0.955f, paint)
    }

    private fun drawProgress(canvas: Canvas, repeat: Int) {
        if (repeat <= 1 && gestureProgress <= 0f) return
        val ratio = ((repetitions + gestureProgress.coerceIn(0f, 1f)) / repeat).coerceIn(0f, 1f)
        val rect = RectF(width * 0.24f, height * 0.92f, width * 0.76f, height * 0.935f)
        paint.color = Color.rgb(227, 214, 196)
        canvas.drawRoundRect(rect, 8f, 8f, paint)
        paint.color = Color.rgb(107, 174, 116)
        canvas.drawRoundRect(RectF(rect.left, rect.top, rect.left + rect.width() * ratio, rect.bottom), 8f, 8f, paint)
    }

    private fun drawFinished(canvas: Canvas) {
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.rgb(62, 50, 42)
        paint.textSize = width * 0.085f
        paint.isFakeBoldText = true
        canvas.drawText("완성!", width / 2f, height * 0.18f, paint)
        paint.isFakeBoldText = false
        FoodPainter.drawDish(canvas, session.recipe.previewParts, parts, RectF(width * 0.08f, height * 0.25f, width * 0.92f, height * 0.62f), context.assets)

        val printRect = RectF(width * 0.16f, height * 0.70f, width * 0.84f, height * 0.80f)
        paint.color = Color.rgb(247, 179, 82)
        canvas.drawRoundRect(printRect, 28f, 28f, paint)
        paint.color = Color.rgb(60, 47, 38)
        paint.textSize = width * 0.05f
        paint.isFakeBoldText = true
        canvas.drawText("종이 부품 프린트", width / 2f, printRect.centerY() + 16f, paint)
        paint.isFakeBoldText = false
        paint.color = Color.rgb(120, 102, 88)
        paint.textSize = width * 0.038f
        canvas.drawText("왼쪽 위 ‹ 를 누르면 메뉴로 돌아가요", width / 2f, height * 0.88f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && event.x < width * 0.16f && event.y < height * 0.14f) {
            onBack()
            return true
        }

        if (session.isFinished) {
            if (event.action == MotionEvent.ACTION_UP &&
                event.x in width * 0.16f..width * 0.84f &&
                event.y in height * 0.70f..height * 0.80f
            ) onPrint()
            return true
        }

        val step = session.currentStep ?: return true
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.y
                lastX = event.x
                lastY = event.y
                dragX = event.x
                dragY = event.y
                gestureProgress = 0f
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                dragX = event.x
                dragY = event.y
                when (step.action) {
                    ActionType.COOK, ActionType.SPREAD -> {
                        gestureProgress += hypot(dx.toDouble(), dy.toDouble()).toFloat() / (width * 0.75f)
                        if (gestureProgress >= 1f) registerRepetition(step.repeat)
                    }
                    else -> Unit
                }
                lastX = event.x
                lastY = event.y
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                when (step.action) {
                    ActionType.PLACE, ActionType.POUR -> {
                        val d = hypot((event.x - dishArea.centerX()).toDouble(), (event.y - dishArea.centerY()).toDouble())
                        if (d < width * 0.32f) registerRepetition(step.repeat)
                    }
                    ActionType.CUT -> if (abs(event.y - downY) > height * 0.10f) registerRepetition(step.repeat)
                    ActionType.FLIP, ActionType.ROLL -> if (downY - event.y > height * 0.10f) registerRepetition(step.repeat)
                    ActionType.COOK, ActionType.SPREAD -> Unit
                }
                dragX = 0f
                dragY = 0f
                invalidate()
            }
        }
        return true
    }

    private fun registerRepetition(required: Int) {
        repetitions += 1
        gestureProgress = 0f
        if (repetitions >= required) {
            repetitions = 0
            session.completeCurrentStep()
        }
        invalidate()
    }
}
