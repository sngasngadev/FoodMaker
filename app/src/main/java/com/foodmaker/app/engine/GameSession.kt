package com.foodmaker.app.engine

import com.foodmaker.app.model.Recipe
import com.foodmaker.app.model.RecipeStep

class GameSession(val recipe: Recipe) {
    var stepIndex: Int = 0
        private set

    var dishParts: List<String> = emptyList()
        private set

    val currentStep: RecipeStep?
        get() = recipe.steps.getOrNull(stepIndex)

    val isFinished: Boolean
        get() = stepIndex >= recipe.steps.size

    fun completeCurrentStep() {
        val step = currentStep ?: return
        dishParts = step.dishAfter
        stepIndex += 1
    }

    fun reset() {
        stepIndex = 0
        dishParts = emptyList()
    }
}
