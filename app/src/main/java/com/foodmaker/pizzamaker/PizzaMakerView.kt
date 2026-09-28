package com.foodmaker.pizzamaker

import android.content.Context
import android.graphics.*
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class PizzaMakerView(context: Context) : View(context) {
    private val art = CrayonArt()
    private val assets = AssetBook(context)
    private val food = PizzaPainter(assets)
    private val g = GameState()
    private val prep = PrepPainter(art, food, assets)
    private val finish = FinishPainter(art, food, assets) { ding() }
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 45)
    private var sc = 1f
    private var ox = 0f
    private var oy = 0f

    override fun onDetachedFromWindow() {
        tone.release()
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        g.takePendingStage()?.let { go(it) }

        super.onDraw(c)
        sc = min(width / 1080f, height / 1920f)
        ox = (width - 1080f * sc) / 2
        oy = (height - 1920f * sc) / 2
        c.save()
        c.translate(ox, oy)
        c.scale(sc, sc)

        food.background(c)
        if (g.stage != Stage.WELCOME) food.workSurface(c)

        prep.draw(c, g)
        finish.draw(c, g)
        top(c)

        c.restore()
        postInvalidateOnAnimation()
    }

    private fun top(c: Canvas) {
        if (g.stage == Stage.WELCOME) return
        art.box(c, RectF(38f, 35f, 1042f, 125f), 35f, Color.argb(225, 255, 249, 230), 600)
        val t = when (g.stage) {
            Stage.MIX -> "재료를 넣고 섞어봐!"
            Stage.ROLL -> "반죽을 쭉쭉 밀어봐!"
            Stage.SAUCE -> "소스를 빙글빙글 발라봐!"
            Stage.CHEESE -> "치즈를 솔솔 뿌려봐!"
            Stage.TOPPINGS -> "좋아하는 토핑을 올려봐!"
            Stage.OVEN -> if (g.bakeDone) "잘 구워졌어! 피자를 꺼내자" else "피자를 오븐에 넣어봐!"
            Stage.CUT -> "피자를 잘라봐!"
            Stage.EAT -> "맛있게 먹어볼까?"
            Stage.DONE -> "완성!"
            else -> ""
        }
        art.text(c, t, 540f, 94f, 38f)
        art.circle(c, 85f, 80f, 31f, Color.rgb(247, 208, 91), 601)
        art.text(c, "‹", 85f, 93f, 45f)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = (e.x - ox) / sc
        val y = (e.y - oy) / sc
        g.prevX = g.touchX
        g.prevY = g.touchY
        g.touchX = x
        g.touchY = y
        g.lastInput = SystemClock.uptimeMillis()

        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                g.dragging = true
                down(x, y)
            }
            MotionEvent.ACTION_MOVE -> move(x, y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                up(x, y)
                g.dragging = false
            }
        }
        invalidate()
        return true
    }

    private fun down(x: Float, y: Float) {
        if (g.stage != Stage.WELCOME && x in 42f..128f && y in 38f..125f) {
            g.back()
            pop()
            return
        }

        when (g.stage) {
            Stage.WELCOME -> if (x in 240f..840f && y in 1340f..1570f) go(Stage.MIX)

            Stage.MIX -> if (!g.mixAdded.all { it }) {
                g.dragIngredient = when {
                    !g.mixAdded[0] && d(x, y, 230f, 640f) < 150 -> 0
                    !g.mixAdded[1] && d(x, y, 540f, 640f) < 150 -> 1
                    !g.mixAdded[2] && d(x, y, 850f, 640f) < 150 -> 2
                    else -> -1
                }
            } else if (d(x, y, 700f, 1080f) < 240 || d(x, y, 540f, 1155f) < 260) {
                g.toolHeld = true
                g.lastStirAngle = ang(x, y, 540f, 1155f)
            }

            Stage.ROLL -> if (y > 1220 || d(x, y, 540f, 1420f) < 340) {
                g.toolHeld = true
            }

            Stage.SAUCE -> if (d(x, y, 765f, 1540f) < 250) {
                g.toolHeld = true
            }

            Stage.CHEESE -> if (d(x, y, 790f, 1530f) < 250) {
                g.toolHeld = true
            }

            Stage.TOPPINGS -> {
                if (g.toppings.size >= 4 && x in 300f..780f && y in 1570f..1780f) {
                    go(Stage.OVEN)
                    return
                }
                val i = ((x - 22) / 207).toInt()
                if (y in 1280f..1500f && i in 0..4) g.selected = ToppingType.values()[i]
            }

            Stage.OVEN -> {
                if (g.bakeDone) {
                    if ((x in 330f..750f && y in 1460f..1615f) || d(x, y, 540f, 760f) < 285) {
                        go(Stage.CUT)
                    }
                } else if (!g.baking && d(x, y, g.ovenX, g.ovenY) < 350) {
                    g.pizzaHeld = true
                }
            }

            Stage.CUT -> if (g.cutCount < 3) {
                g.cutStart = PointF(x, y)
                g.cutterX = x
                g.cutterY = y
            }

            Stage.EAT -> {
                if (g.eaten.all { it }) return
                val dx = x - 540
                val dy = y - 960
                if (hypot(dx, dy) < 370) {
                    var a = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 90
                    if (a < 0) a += 360
                    val i = (a / 60).toInt().coerceIn(0, 5)
                    if (!g.eaten[i]) {
                        g.eaten[i] = true
                        pop()
                        if (g.eaten.all { it }) {
                            ding()
                            g.scheduleNext(Stage.DONE, 700L)
                        }
                    }
                }
            }

            Stage.DONE -> if (x in 250f..830f && y in 1230f..1490f) {
                g.reset()
                pop()
            }
        }
    }

    private fun move(x: Float, y: Float) {
        when (g.stage) {
            Stage.MIX -> if (g.toolHeld && g.mixAdded.all { it }) {
                val a = ang(x, y, 540f, 1155f)
                g.lastStirAngle?.let { old ->
                    var z = abs(a - old)
                    if (z > 180) z = 360 - z
                    g.stir = (g.stir + z / 700f).coerceAtMost(1f)
                }
                g.lastStirAngle = a
                if (g.stir >= 1f) {
                    g.toolHeld = false
                    ding()
                    g.scheduleNext(Stage.ROLL, 700L)
                }
            }

            Stage.ROLL -> if (g.toolHeld && d(x, y, 540f, 1120f) < 470) {
                g.roll = (g.roll + hypot(x - g.prevX, y - g.prevY) / 1650f).coerceAtMost(1f)
                if (g.roll >= .98f) {
                    ding()
                    g.scheduleNext(Stage.SAUCE, 700L)
                }
            }

            Stage.SAUCE -> if (g.toolHeld && d(x, y, 540f, 1050f) < 310) {
                val z = hypot(x - g.prevX, y - g.prevY)
                if (z > 3f) {
                    g.sauceMarks.add(PointF(x, y))
                    if (g.sauceMarks.size > 110) g.sauceMarks.removeAt(0)
                    g.sauce = (g.sauce + z / 1500f).coerceAtMost(1f)
                    if (g.sauce >= .92f) {
                        ding()
                        g.scheduleNext(Stage.CHEESE, 750L)
                    }
                }
            }

            Stage.CHEESE -> if (g.toolHeld && d(x, y, 540f, 1040f) < 310) {
                val z = hypot(x - g.prevX, y - g.prevY)
                if (z > 5f) {
                    val seed = g.cheeseBits.size
                    repeat(2) { k ->
                        val offX = ((seed * 37 + k * 29) % 31 - 15).toFloat()
                        val offY = ((seed * 53 + k * 17) % 27 - 13).toFloat()
                        g.cheeseBits.add(
                            Sprinkle(
                                x = x + offX,
                                y = y + 58f + offY,
                                rotation = ((seed * 47 + k * 61) % 180).toFloat(),
                                length = 28f + ((seed + k) % 5) * 4f
                            )
                        )
                    }
                    if (g.cheeseBits.size > 180) {
                        repeat(g.cheeseBits.size - 180) { g.cheeseBits.removeAt(0) }
                    }
                    g.cheese = (g.cheese + z / 1350f).coerceAtMost(1f)
                    if (g.cheese >= .90f) {
                        ding()
                        g.scheduleNext(Stage.TOPPINGS, 750L)
                    }
                }
            }

            Stage.OVEN -> if (g.pizzaHeld) {
                g.ovenX = x
                g.ovenY = y
            }

            Stage.CUT -> if (g.cutCount < 3) {
                val dx = x - g.prevX
                val dy = y - g.prevY
                g.cutterX = x
                g.cutterY = y
                if (abs(dx) + abs(dy) > 2) {
                    g.cutterRot = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
                }
            }

            else -> Unit
        }
    }

    private fun up(x: Float, y: Float) {
        when (g.stage) {
            Stage.MIX -> {
                if (g.dragIngredient >= 0) {
                    if (d(x, y, 540f, 1155f) < 340) {
                        val index = g.dragIngredient
                        g.mixAdded[index] = true
                        g.mixAddedAt[index] = SystemClock.uptimeMillis()
                        pop()
                    }
                    g.dragIngredient = -1
                }
                g.toolHeld = false
                g.lastStirAngle = null
            }

            Stage.ROLL -> g.toolHeld = false
            Stage.SAUCE -> g.toolHeld = false
            Stage.CHEESE -> g.toolHeld = false

            Stage.TOPPINGS -> {
                g.selected?.let { t ->
                    if (d(x, y, 540f, 830f) < 315) {
                        val b = assets.toppings.getValue(t)
                        val vx = (x - 540f) / (350f * 1.55f)
                        val vy = (y - 830f) / (350f * 1.55f)
                        val maxRadius = when (t) {
                            ToppingType.ONION -> .39f
                            ToppingType.MUSHROOM -> .40f
                            else -> .43f
                        }
                        val len = hypot(vx, vy)
                        val factor = if (len > maxRadius && len > 0f) maxRadius / len else 1f
                        val nx = vx * factor
                        val ny = vy * factor
                        val s = g.toppings.size * 47 + t.ordinal * 19 + b.width
                        g.toppings.add(
                            ToppingPiece(
                                t,
                                nx,
                                ny,
                                (s % 45 - 22).toFloat(),
                                .90f + (s % 15) / 100f
                            )
                        )
                        pop()
                    }
                }
                g.selected = null
            }

            Stage.OVEN -> {
                if (g.pizzaHeld) {
                    val inside = y < 1010f && d(x, y, 540f, 750f) < 360f
                    if (inside) {
                        g.ovenX = 540f
                        g.ovenY = 760f
                        g.baking = true
                        g.bakeStart = SystemClock.uptimeMillis()
                        pop()
                    } else {
                        g.ovenX = 540f
                        g.ovenY = 1395f
                    }
                    g.pizzaHeld = false
                }
            }

            Stage.CUT -> {
                g.cutStart?.let { s ->
                    val len = hypot(x - s.x, y - s.y)
                    val mx = (x + s.x) / 2
                    val my = (y + s.y) / 2
                    if (len > 420 && d(mx, my, 540f, 960f) < 165) {
                        var a = Math.toDegrees(atan2(y - s.y, x - s.x).toDouble()).toFloat()
                        while (a < 0) a += 180
                        while (a >= 180) a -= 180
                        if (g.cutAngles.none { diff(it, a) < 24 } && g.cutAngles.size < 3) {
                            g.cutAngles.add(a)
                            g.cutCount++
                            pop()
                            if (g.cutCount == 3) {
                                ding()
                                g.scheduleNext(Stage.EAT, 800L)
                            }
                        }
                    }
                }
                g.cutStart = null
            }

            else -> Unit
        }
    }

    private fun go(s: Stage) {
        g.go(s)
        pop()
    }

    private fun pop() {
        tone.startTone(ToneGenerator.TONE_PROP_ACK, 55)
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun ding() {
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 170)
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    private fun d(x: Float, y: Float, a: Float, b: Float) = hypot(x - a, y - b)
    private fun ang(x: Float, y: Float, a: Float, b: Float) =
        Math.toDegrees(atan2(y - b, x - a).toDouble()).toFloat()

    private fun diff(a: Float, b: Float): Float {
        var q = abs(a - b) % 180
        if (q > 90) q = 180 - q
        return q
    }
}
