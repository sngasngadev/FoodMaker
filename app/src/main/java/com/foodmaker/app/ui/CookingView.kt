package com.foodmaker.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.foodmaker.app.engine.GameSession
import com.foodmaker.app.model.PartDefinition
import com.foodmaker.app.ui.scenes.CookingScene
import com.foodmaker.app.ui.scenes.CookingSceneFactory

class CookingView(
    context: Context,
    private val session: GameSession,
    private val parts: Map<String, PartDefinition>,
    private val onBack: () -> Unit,
    private val onPrint: () -> Unit
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var scene: CookingScene? = null
    private var transitioning = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(255, 249, 238))
        drawHeader(canvas)

        if (session.isFinished) {
            drawFinished(canvas)
            return
        }

        ensureScene()
        val activeScene = scene ?: return

        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.rgb(64, 53, 46)
        paint.textSize = width * 0.056f
        paint.isFakeBoldText = true
        canvas.drawText(activeScene.instruction, width / 2f, height * 0.205f, paint)
        paint.isFakeBoldText = false

        activeScene.draw(canvas)

        if (transitioning) {
            paint.textAlign = Paint.Align.CENTER
            paint.color = Color.rgb(80, 145, 80)
            paint.textSize = width * 0.045f
            paint.isFakeBoldText = true
            canvas.drawText("좋아!", width / 2f, height * 0.90f, paint)
            paint.isFakeBoldText = false
        }
    }

    private fun ensureScene() {
        if (scene != null || session.isFinished) return
        val step = session.currentStep ?: return
        scene = CookingSceneFactory.create(
            host = this,
            step = step,
            parts = parts,
            dishParts = session.dishParts,
            onComplete = ::completeScene
        )
    }

    private fun completeScene() {
        if (transitioning) return
        transitioning = true
        invalidate()
        postDelayed({
            session.completeCurrentStep()
            scene = null
            transitioning = false
            invalidate()
        }, 850L)
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
        val visible = total.coerceAtMost(8)
        val startX = width * 0.50f
        val gap = if (visible <= 1) 0f else (width * 0.42f) / (visible - 1)
        repeat(visible) { i ->
            paint.color = if (i < completed) Color.rgb(238, 151, 79) else Color.rgb(210, 224, 231)
            canvas.drawCircle(startX + i * gap, height * 0.065f, width * 0.012f, paint)
        }
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
                        scene = null
                        transitioning = false
                        invalidate()
                    }
                    buttons[1].contains(event.x, event.y) -> onBack()
                    buttons[2].contains(event.x, event.y) -> onPrint()
                }
            }
            return true
        }

        if (transitioning) return true
        ensureScene()
        return scene?.onTouch(event) ?: true
    }
}
