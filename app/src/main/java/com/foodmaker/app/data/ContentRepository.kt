package com.foodmaker.app.data

import android.content.Context
import com.foodmaker.app.model.ActionType
import com.foodmaker.app.model.PartDefinition
import com.foodmaker.app.model.Recipe
import com.foodmaker.app.model.RecipeStep
import org.json.JSONObject

class ContentRepository(private val context: Context) {

    val parts: Map<String, PartDefinition> by lazy { loadParts() }
    val recipes: List<Recipe> by lazy { loadRecipes() }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    private fun loadParts(): Map<String, PartDefinition> {
        val root = JSONObject(readAsset("parts.json"))
        val array = root.getJSONArray("parts")
        return buildMap {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val part = PartDefinition(
                    id = item.getString("id"),
                    label = item.getString("label"),
                    shape = item.optString("shape", "roundRect"),
                    colorHex = item.optString("color", "#DDDDDD"),
                    accentHex = item.stringOrNull("accent"),
                    printWidthMm = item.optInt("printWidthMm", 55),
                    printHeightMm = item.optInt("printHeightMm", 35),
                    assetPath = item.stringOrNull("asset"),
                    assetCrop = item.optFloatList("assetCrop")
                )
                put(part.id, part)
            }
        }
    }

    private fun loadRecipes(): List<Recipe> {
        return context.assets.list("recipes").orEmpty()
            .filter { it.endsWith(".json") }
            .sorted()
            .map { file ->
                val root = JSONObject(readAsset("recipes/$file"))
                val stepsJson = root.getJSONArray("steps")
                val steps = buildList {
                    for (i in 0 until stepsJson.length()) {
                        val step = stepsJson.getJSONObject(i)
                        add(
                            RecipeStep(
                                id = step.getString("id"),
                                action = ActionType.valueOf(step.getString("action")),
                                input = step.getString("input"),
                                output = step.stringOrNull("output"),
                                instruction = step.getString("instruction"),
                                repeat = step.optInt("repeat", 1).coerceAtLeast(1),
                                dishAfter = step.optStringList("dishAfter"),
                                tool = step.stringOrNull("tool"),
                                placementMode = step.stringOrNull("placementMode"),
                                itemScale = step.floatOrNull("itemScale")
                            )
                        )
                    }
                }
                Recipe(
                    id = root.getString("id"),
                    title = root.getString("title"),
                    subtitle = root.optString("subtitle", ""),
                    previewParts = root.optStringList("previewParts"),
                    steps = steps,
                    printParts = root.optStringList("printParts")
                )
            }
    }

    private fun JSONObject.stringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    private fun JSONObject.floatOrNull(key: String): Float? =
        if (has(key) && !isNull(key)) getDouble(key).toFloat() else null

    private fun JSONObject.optFloatList(key: String): List<Float>? {
        if (!has(key) || isNull(key)) return null
        val array = getJSONArray(key)
        return buildList {
            for (i in 0 until array.length()) add(array.getDouble(i).toFloat())
        }
    }

    private fun JSONObject.optStringList(key: String): List<String> {
        if (!has(key) || isNull(key)) return emptyList()
        val array = getJSONArray(key)
        return buildList {
            for (i in 0 until array.length()) add(array.getString(i))
        }
    }
}
