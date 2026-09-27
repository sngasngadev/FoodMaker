package com.foodmaker.app.model

enum class ActionType {
    PLACE, CUT, COOK, FLIP, SPREAD, ROLL, POUR
}

data class PartDefinition(
    val id: String,
    val label: String,
    val shape: String,
    val colorHex: String,
    val accentHex: String?,
    val printWidthMm: Int,
    val printHeightMm: Int,
    val assetPath: String? = null,
    val assetCrop: List<Float>? = null
)

data class RecipeStep(
    val id: String,
    val action: ActionType,
    val input: String,
    val output: String?,
    val instruction: String,
    val repeat: Int,
    val dishAfter: List<String>
)

data class Recipe(
    val id: String,
    val title: String,
    val subtitle: String,
    val previewParts: List<String>,
    val steps: List<RecipeStep>,
    val printParts: List<String>
)
