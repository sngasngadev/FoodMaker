package com.foodmaker.pizzamaker

import android.graphics.PointF
import android.os.SystemClock

enum class Stage { WELCOME, MIX, ROLL, SAUCE, CHEESE, TOPPINGS, OVEN, CUT, EAT, DONE }
data class ToppingPiece(val type:ToppingType,val nx:Float,val ny:Float,val rotation:Float,val scale:Float)
data class Sprinkle(val x:Float,val y:Float,val rotation:Float,val length:Float)

class GameState {
    var stage = Stage.WELCOME
    var stageStart = SystemClock.uptimeMillis()
    var lastInput = stageStart

    var touchX = 0f
    var touchY = 0f
    var prevX = 0f
    var prevY = 0f
    var dragging = false

    val mixAdded = booleanArrayOf(false, false, false)
    val mixAddedAt = longArrayOf(0L, 0L, 0L)
    var dragIngredient = -1
    var stir = 0f
    var lastStirAngle: Float? = null

    var roll = 0f
    var sauce = 0f
    var cheese = 0f
    val sauceMarks = mutableListOf<PointF>()
    val cheeseBits = mutableListOf<Sprinkle>()

    var toolHeld = false
    var selected: ToppingType? = null
    val toppings = mutableListOf<ToppingPiece>()

    var ovenX = 540f
    var ovenY = 1395f
    var pizzaHeld = false
    var baking = false
    var bakeStart = 0L
    var bakeDone = false

    var cutCount = 0
    var cutStart: PointF? = null
    val cutAngles = mutableListOf<Float>()
    var cutterX = 830f
    var cutterY = 1540f
    var cutterRot = -25f

    val eaten = BooleanArray(6)

    var pendingStage: Stage? = null
    var pendingStageAt = 0L

    fun go(s: Stage) {
        stage = s
        stageStart = SystemClock.uptimeMillis()
        lastInput = stageStart
        pendingStage = null
        pendingStageAt = 0L
    }

    fun scheduleNext(s: Stage, delayMs: Long = 650L) {
        if (pendingStage == null) {
            pendingStage = s
            pendingStageAt = SystemClock.uptimeMillis() + delayMs
        }
    }

    fun takePendingStage(now: Long = SystemClock.uptimeMillis()): Stage? {
        val next = pendingStage ?: return null
        if (now < pendingStageAt) return null
        pendingStage = null
        pendingStageAt = 0L
        return next
    }

    fun back() {
        pendingStage = null
        pendingStageAt = 0L
        go(
            when(stage) {
                Stage.MIX -> Stage.WELCOME
                Stage.ROLL -> Stage.MIX
                Stage.SAUCE -> Stage.ROLL
                Stage.CHEESE -> Stage.SAUCE
                Stage.TOPPINGS -> Stage.CHEESE
                Stage.OVEN -> Stage.TOPPINGS
                Stage.CUT -> Stage.OVEN
                Stage.EAT -> Stage.CUT
                Stage.DONE -> Stage.EAT
                else -> Stage.WELCOME
            }
        )
    }

    fun reset() {
        mixAdded.fill(false)
        mixAddedAt.fill(0L)
        dragIngredient = -1
        stir = 0f
        roll = 0f
        sauce = 0f
        cheese = 0f
        sauceMarks.clear()
        cheeseBits.clear()
        toolHeld = false
        selected = null
        toppings.clear()
        ovenX = 540f
        ovenY = 1395f
        pizzaHeld = false
        baking = false
        bakeDone = false
        cutCount = 0
        cutStart = null
        cutAngles.clear()
        eaten.fill(false)
        cutterX = 830f
        cutterY = 1540f
        cutterRot = -25f
        pendingStage = null
        pendingStageAt = 0L
        go(Stage.MIX)
    }
}
