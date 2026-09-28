package com.foodmaker.pizzamaker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.ByteArrayOutputStream
import java.util.ArrayDeque
import java.util.zip.ZipInputStream
import kotlin.math.max

enum class ToppingType { PEPPERONI, MUSHROOM, PEPPER, OLIVE, ONION }

class AssetBook(context: Context) {
    private val images: Map<String, Bitmap> = loadPack(context)

    private fun asset(name: String): Bitmap =
        requireNotNull(images[name]) { "Missing PizzaMaker art asset: $name" }

    val background = asset("bg_pizza_kitchen")
    val ovenOpen = asset("oven_open")
    val ovenClosed = asset("oven_closed")

    val bowl = asset("tool_bowl")
    val rollingPin = asset("tool_rolling_pin")
    val spoon = asset("tool_spoon")
    val sauceLadle = asset("tool_sauce_ladle")
    val cheeseShaker = asset("tool_cheese_shaker")
    val pizzaCutter = asset("tool_pizza_cutter")
    val plate = asset("tool_plate")

    val flourBag = asset("ingredient_flour_bag")
    val flourPile = asset("ingredient_flour_pile")
    val oilBottle = asset("ingredient_oil_bottle")
    val waterCup = asset("ingredient_water_cup")
    val doughBall = asset("ingredient_dough_ball")
    val doughRound = asset("ingredient_dough_round")
    val sauceBowl = asset("ingredient_sauce_bowl")
    val shreddedCheese = asset("ingredient_shredded_cheese")

    val pizzaDough = asset("pizza_stage_dough")
    val pizzaSauce = asset("pizza_stage_sauce")
    val pizzaCheese = asset("pizza_stage_cheese")
    val pizzaBaked = asset("pizza_stage_baked_plain")
    val pizzaTopped = asset("pizza_stage_topped")
    val pizzaCut4 = asset("pizza_stage_cut_4")
    val pizzaCut6 = asset("pizza_stage_cut_6")
    val pizzaCut6Spread = asset("pizza_stage_cut_6_spread")

    val toppingGroups: Map<ToppingType, Bitmap> = mapOf(
        ToppingType.PEPPERONI to asset("topping_pepperoni"),
        ToppingType.MUSHROOM to asset("topping_mushroom"),
        ToppingType.PEPPER to asset("topping_pepper_green"),
        ToppingType.OLIVE to asset("topping_olive"),
        ToppingType.ONION to asset("topping_onion")
    )

    /**
     * Each generated topping asset is a transparent group containing several pieces.
     * Extract one complete connected alpha component, then add transparent padding.
     * This avoids the hard rectangular cuts that were visible with percentage crops.
     */
    val toppings: Map<ToppingType, Bitmap> = toppingGroups.mapValues { (_, group) ->
        extractSinglePiece(group)
    }

    val effects: List<Bitmap> = images
        .filterKeys { it.startsWith("fx_") }
        .toSortedMap()
        .values
        .toList()

    private data class Component(
        val minX: Int,
        val minY: Int,
        val maxX: Int,
        val maxY: Int,
        val pixels: Int
    )

    private fun extractSinglePiece(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val seen = BooleanArray(w * h)
        val queue = ArrayDeque<Int>()
        val components = mutableListOf<Component>()

        fun opaque(index: Int): Boolean = Color.alpha(pixels[index]) > 42

        for (start in pixels.indices) {
            if (seen[start] || !opaque(start)) continue

            seen[start] = true
            queue.add(start)
            var minX = w
            var minY = h
            var maxX = 0
            var maxY = 0
            var count = 0

            while (queue.isNotEmpty()) {
                val index = queue.removeFirst()
                val x = index % w
                val y = index / w
                count++
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y

                if (x > 0) {
                    val n = index - 1
                    if (!seen[n] && opaque(n)) { seen[n] = true; queue.add(n) }
                }
                if (x + 1 < w) {
                    val n = index + 1
                    if (!seen[n] && opaque(n)) { seen[n] = true; queue.add(n) }
                }
                if (y > 0) {
                    val n = index - w
                    if (!seen[n] && opaque(n)) { seen[n] = true; queue.add(n) }
                }
                if (y + 1 < h) {
                    val n = index + w
                    if (!seen[n] && opaque(n)) { seen[n] = true; queue.add(n) }
                }
            }

            if (count > 80) {
                components += Component(minX, minY, maxX, maxY, count)
            }
        }

        val chosen = components.maxByOrNull { it.pixels } ?: return bitmap
        val objectW = chosen.maxX - chosen.minX + 1
        val objectH = chosen.maxY - chosen.minY + 1
        val pad = max(10, (max(objectW, objectH) * .12f).toInt())

        val cropLeft = (chosen.minX - pad).coerceAtLeast(0)
        val cropTop = (chosen.minY - pad).coerceAtLeast(0)
        val cropRight = (chosen.maxX + pad + 1).coerceAtMost(w)
        val cropBottom = (chosen.maxY + pad + 1).coerceAtMost(h)

        val crop = Bitmap.createBitmap(
            bitmap,
            cropLeft,
            cropTop,
            cropRight - cropLeft,
            cropBottom - cropTop
        )

        // Guarantee breathing room even when the source component touches an edge.
        val extra = max(8, (max(crop.width, crop.height) * .08f).toInt())
        val padded = Bitmap.createBitmap(
            crop.width + extra * 2,
            crop.height + extra * 2,
            Bitmap.Config.ARGB_8888
        )
        android.graphics.Canvas(padded).drawBitmap(crop, extra.toFloat(), extra.toFloat(), null)
        return padded
    }

    private fun loadPack(context: Context): Map<String, Bitmap> {
        val result = linkedMapOf<String, Bitmap>()
        ZipInputStream(context.assets.open("pizzamaker_assets.zip")).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory && entry.name.endsWith(".webp")) {
                    val bytes = ByteArrayOutputStream()
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val n = zip.read(buffer)
                        if (n <= 0) break
                        bytes.write(buffer, 0, n)
                    }
                    val data = bytes.toByteArray()
                    BitmapFactory.decodeByteArray(
                        data, 0, data.size,
                        BitmapFactory.Options().apply { inScaled = false }
                    )?.let { bitmap ->
                        result[entry.name.substringBeforeLast('.')] = bitmap
                    }
                }
                zip.closeEntry()
            }
        }
        return result
    }
}
