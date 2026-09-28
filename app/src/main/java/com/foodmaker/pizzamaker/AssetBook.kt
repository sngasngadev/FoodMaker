package com.foodmaker.pizzamaker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

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
    val oilBottle = asset("ingredient_oil_bottle")
    val waterCup = asset("ingredient_water_cup")
    val doughBall = asset("ingredient_dough_ball")
    val sauceBowl = asset("ingredient_sauce_bowl")
    val shreddedCheese = asset("ingredient_shredded_cheese")

    val pizzaDough = asset("pizza_stage_dough")
    val pizzaSauce = asset("pizza_stage_sauce")
    val pizzaCheese = asset("pizza_stage_cheese")
    val pizzaBaked = asset("pizza_stage_baked_plain")
    val pizzaTopped = asset("pizza_stage_topped")
    val pizzaCut6 = asset("pizza_stage_cut_6")
    val pizzaCut6Spread = asset("pizza_stage_cut_6_spread")

    val toppings: Map<ToppingType, Bitmap> = mapOf(
        ToppingType.PEPPERONI to asset("topping_pepperoni"),
        ToppingType.MUSHROOM to asset("topping_mushroom"),
        ToppingType.PEPPER to asset("topping_pepper_green"),
        ToppingType.OLIVE to asset("topping_olive"),
        ToppingType.ONION to asset("topping_onion")
    )

    val effects: List<Bitmap> = images
        .filterKeys { it.startsWith("fx_") }
        .toSortedMap()
        .values
        .toList()

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
