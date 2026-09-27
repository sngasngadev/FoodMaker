package com.foodmaker.pizzamaker

import android.content.Context
import android.graphics.*
import kotlin.math.*

enum class ToppingType { PEPPERONI, MUSHROOM, PEPPER, OLIVE, ONION }

/**
 * Final in-game food artwork generated from deterministic hand-drawn primitives.
 * The uploaded crayon sheet is the visual reference: dark imperfect outlines,
 * visible wax-pencil strokes, warm paper gaps and slightly asymmetric shapes.
 * Nothing here is a temporary placeholder; these bitmaps are the actual sprites
 * the game uses at runtime.
 */
class AssetBook(@Suppress("UNUSED_PARAMETER") context: Context) {
    val dough: Bitmap = makeDough()
    val sauce: Bitmap = makeSauce()
    val cheese: Bitmap = makeCheese()
    val toppings: Map<ToppingType, Bitmap> = mapOf(
        ToppingType.PEPPERONI to makePepperoni(),
        ToppingType.MUSHROOM to makeMushroom(),
        ToppingType.PEPPER to makePepper(),
        ToppingType.OLIVE to makeOlive(),
        ToppingType.ONION to makeOnion()
    )

    private fun bitmap(size: Int, draw: (Canvas, Paint) -> Unit): Bitmap {
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        draw(c, p)
        return b
    }

    private fun jitter(seed: Int, amount: Float): Float {
        val v = sin(seed * 12.9898 + 78.233) * 43758.5453
        return ((v - floor(v)) * 2.0 - 1.0).toFloat() * amount
    }

    private fun roughCircle(c: Canvas, p: Paint, cx: Float, cy: Float, r: Float, fill: Int, stroke: Int, seed: Int) {
        p.style = Paint.Style.FILL
        p.color = fill
        c.drawCircle(cx, cy, r, p)
        p.style = Paint.Style.STROKE
        p.color = stroke
        p.strokeWidth = max(4f, r * .028f)
        repeat(3) { i ->
            c.drawCircle(cx + jitter(seed + i * 7, 2.2f), cy + jitter(seed + i * 11, 2.2f), r + jitter(seed + i * 13, 1.8f), p)
        }
    }

    private fun scribbles(c: Canvas, p: Paint, cx: Float, cy: Float, radius: Float, color: Int, count: Int, seed: Int, width: Float = 4f) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = width
        p.color = color
        for (i in 0 until count) {
            val a = (seed * .13 + i * 2.399).toFloat()
            val rr = radius * (.12f + ((i * 37) % 83) / 100f)
            val x = cx + cos(a) * rr * .82f
            val y = cy + sin(a) * rr * .82f
            val len = 12f + (i % 8) * 3.5f
            val tilt = a + jitter(seed + i, .8f)
            c.drawLine(x - cos(tilt) * len, y - sin(tilt) * len, x + cos(tilt) * len, y + sin(tilt) * len, p)
        }
    }

    private fun makeDough() = bitmap(520) { c, p ->
        roughCircle(c,p,260f,260f,238f,Color.rgb(241,165,67),Color.rgb(45,39,33),10)
        roughCircle(c,p,260f,260f,202f,Color.rgb(255,239,195),Color.rgb(211,143,59),20)
        scribbles(c,p,260f,260f,192f,Color.argb(90,231,181,105),155,30,3.1f)
        scribbles(c,p,260f,260f,192f,Color.argb(65,255,255,238),100,40,2.8f)
        p.style=Paint.Style.FILL
        for(i in 0 until 15){
            p.color=Color.argb(80,218,151,59)
            c.drawCircle(130f+(i*83%270),145f+(i*61%245),5f+(i%4),p)
        }
    }

    private fun makeSauce() = bitmap(520) { c, p ->
        roughCircle(c,p,260f,260f,238f,Color.rgb(244,158,60),Color.rgb(45,39,33),51)
        roughCircle(c,p,260f,260f,207f,Color.rgb(239,56,35),Color.rgb(221,74,39),52)
        p.style=Paint.Style.STROKE
        p.strokeCap=Paint.Cap.ROUND
        repeat(48){i->
            val inset=25f+i*3.3f
            val a0=(i*29%360).toFloat()
            p.strokeWidth=5f+(i%4)
            p.color=if(i%3==0) Color.argb(125,255,117,53) else Color.argb(85,255,205,142)
            c.drawArc(RectF(260-inset,260-inset,260+inset,260+inset),a0,48f+(i%5)*10f,false,p)
        }
        scribbles(c,p,260f,260f,190f,Color.argb(70,150,28,20),100,55,3f)
    }

    private fun makeCheese() = bitmap(440) { c, p ->
        val clip=Path().apply{addCircle(220f,220f,202f,Path.Direction.CW)}
        c.save();c.clipPath(clip)
        p.style=Paint.Style.STROKE;p.strokeCap=Paint.Cap.ROUND
        for(i in 0 until 95){
            val x=25f+(i*79%390); val y=28f+(i*127%385)
            val ang=Math.toRadians(((i*43)%180).toDouble())
            val len=28f+(i%6)*8f
            val dx=cos(ang).toFloat()*len; val dy=sin(ang).toFloat()*len
            p.strokeWidth=11f+(i%3)*2f
            p.color=when(i%4){0->Color.rgb(255,247,192);1->Color.rgb(255,229,83);2->Color.rgb(255,206,58);else->Color.rgb(255,238,133)}
            c.drawLine(x-dx,y-dy,x+dx,y+dy,p)
            if(i%5==0){p.strokeWidth=3f;p.color=Color.argb(80,197,134,40);c.drawLine(x-dx,y-dy,x+dx,y+dy,p)}
        }
        c.restore()
    }

    private fun makePepperoni() = bitmap(180) { c, p ->
        roughCircle(c,p,90f,90f,72f,Color.rgb(242,62,43),Color.rgb(57,42,35),70)
        scribbles(c,p,90f,90f,63f,Color.argb(100,255,133,66),40,71,3.5f)
        p.style=Paint.Style.FILL
        repeat(9){i->
            p.color=if(i%2==0)Color.rgb(255,118,126) else Color.rgb(224,84,63)
            c.drawCircle(45f+(i*37%91),48f+(i*53%86),7f+(i%3),p)
            p.color=Color.argb(100,255,202,196);c.drawCircle(43f+(i*37%91),45f+(i*53%86),2.6f,p)
        }
    }

    private fun makeMushroom() = bitmap(190) { c, p ->
        val cap=Path().apply{
            moveTo(26f,92f);quadTo(34f,28f,95f,24f);quadTo(157f,29f,166f,92f)
            quadTo(151f,102f,133f,104f);quadTo(116f,83f,95f,84f);quadTo(72f,83f,57f,104f);quadTo(38f,101f,26f,92f);close()
        }
        p.style=Paint.Style.FILL;p.color=Color.rgb(250,232,187);c.drawPath(cap,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=7f;p.color=Color.rgb(68,49,37);c.drawPath(cap,p)
        val stem=Path().apply{moveTo(72f,83f);lineTo(117f,83f);lineTo(126f,164f);quadTo(95f,177f,63f,164f);close()}
        p.style=Paint.Style.FILL;p.color=Color.rgb(255,244,213);c.drawPath(stem,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=7f;p.color=Color.rgb(68,49,37);c.drawPath(stem,p)
        p.color=Color.rgb(148,100,53);p.strokeWidth=12f;c.drawArc(RectF(38f,65f,150f,128f),12f,160f,false,p)
        scribbles(c,p,95f,82f,70f,Color.argb(70,192,144,76),28,91,2.8f)
    }

    private fun makePepper() = bitmap(190) { c, p ->
        p.style=Paint.Style.STROKE;p.strokeCap=Paint.Cap.ROUND
        for(pass in 0..2){
            p.strokeWidth=24f-pass*4f
            p.color=when(pass){0->Color.rgb(36,135,39);1->Color.rgb(62,193,44);else->Color.rgb(138,238,66)}
            val path=Path().apply{
                moveTo(95f,35f);cubicTo(70f,33f,64f,56f,57f,64f);cubicTo(37f,67f,29f,84f,34f,101f)
                cubicTo(40f,121f,61f,124f,68f,137f);cubicTo(78f,155f,111f,157f,120f,137f)
                cubicTo(131f,124f,150f,122f,156f,101f);cubicTo(162f,81f,150f,67f,132f,63f)
                cubicTo(125f,47f,115f,35f,95f,35f)
            }
            c.drawPath(path,p)
        }
        p.strokeWidth=5f;p.color=Color.rgb(48,50,35);c.drawCircle(95f,96f,63f,p)
    }

    private fun makeOlive() = bitmap(180) { c, p ->
        roughCircle(c,p,90f,90f,67f,Color.rgb(29,31,29),Color.rgb(45,42,38),120)
        roughCircle(c,p,90f,90f,32f,Color.rgb(251,239,198),Color.rgb(238,238,220),121)
        p.style=Paint.Style.STROKE;p.strokeWidth=7f;p.color=Color.argb(95,125,125,117)
        repeat(12){i->c.drawArc(RectF(31f+i%3*2,31f+i%2*2,149f-i%3*2,149f-i%2*2),i*31f,20f,false,p)}
    }

    private fun makeOnion() = bitmap(210) { c, p ->
        val outer=Path().apply{moveTo(20f,64f);quadTo(95f,150f,190f,66f);quadTo(150f,172f,93f,174f);quadTo(42f,166f,20f,64f);close()}
        p.style=Paint.Style.FILL;p.color=Color.rgb(185,82,211);c.drawPath(outer,p)
        p.style=Paint.Style.STROKE;p.strokeWidth=6f;p.color=Color.rgb(70,42,68);c.drawPath(outer,p)
        val colors=intArrayOf(Color.rgb(245,193,247),Color.rgb(221,134,232),Color.rgb(255,224,251))
        for(i in 0..3){
            p.color=colors[i%colors.size];p.strokeWidth=8f-i
            val inset=i*15f;c.drawArc(RectF(35f+inset,40f+inset*.4f,180f-inset,165f-inset*.2f),12f,158f,false,p)
        }
    }
}
