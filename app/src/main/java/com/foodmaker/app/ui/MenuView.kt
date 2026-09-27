package com.foodmaker.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.foodmaker.app.model.PartDefinition
import com.foodmaker.app.model.Recipe

class MenuView(
    context: Context,
    private val recipes: List<Recipe>,
    private val parts: Map<String, PartDefinition>,
    private val onRecipeSelected: (Recipe) -> Unit
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cards = mutableListOf<Pair<RectF, Recipe>>()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(255, 248, 234))
        val pad = width * 0.07f

        paint.color = Color.rgb(62, 50, 42)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = width * 0.085f
        paint.isFakeBoldText = true
        canvas.drawText("오늘은 뭘 만들까?", pad, height * 0.11f, paint)
        paint.isFakeBoldText = false
        paint.textSize = width * 0.042f
        paint.color = Color.rgb(120, 102, 88)
        canvas.drawText("음식을 눌러 바로 시작해요", pad, height * 0.16f, paint)

        cards.clear()
        val top = height * 0.22f
        val gap = height * 0.025f
        val cardH = (height * 0.68f - gap * 2) / 3f

        recipes.take(3).forEachIndexed { index, recipe ->
            val rect = RectF(pad, top + index * (cardH + gap), width - pad, top + index * (cardH + gap) + cardH)
            cards += rect to recipe
            paint.color = Color.WHITE
            canvas.drawRoundRect(rect, 34f, 34f, paint)

            val preview = RectF(rect.left + 24f, rect.top + 16f, rect.left + cardH * 0.95f, rect.bottom - 16f)
            FoodPainter.drawDish(canvas, recipe.previewParts, parts, preview, context.assets)

            paint.color = Color.rgb(62, 50, 42)
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = width * 0.06f
            paint.isFakeBoldText = true
            canvas.drawText(recipe.title, rect.left + cardH, rect.centerY() - 6f, paint)
            paint.isFakeBoldText = false
            paint.textSize = width * 0.037f
            paint.color = Color.rgb(125, 105, 90)
            canvas.drawText(recipe.subtitle, rect.left + cardH, rect.centerY() + 42f, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            cards.firstOrNull { it.first.contains(event.x, event.y) }?.second?.let(onRecipeSelected)
        }
        return true
    }
}
