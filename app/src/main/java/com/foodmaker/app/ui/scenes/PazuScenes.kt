package com.foodmaker.app.ui.scenes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.foodmaker.app.model.PartDefinition
import com.foodmaker.app.model.RecipeStep
import com.foodmaker.app.ui.FoodPainter
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class BowlPourScene(
    host: View,
    step: RecipeStep,
    parts: Map<String, PartDefinition>,
    dishParts: List<String>,
    onComplete: () -> Unit
) : BaseCookingScene(host, step, parts, dishParts, onComplete) {
    private var active = false
    private var itemX = 0f
    private var itemY = 0f
    private var pouringSince = 0L
    private var done = false

    override val instruction: String
        get() = if (done) "잘 넣었어요!" else step.instruction

    private fun bowlRect() = RectF(w * .26f, h * .40f, w * .74f, h * .66f)
    private fun sourceHome() = w * .78f to h * .74f

    override fun draw(canvas: Canvas) {
        SceneArt.drawBoard(canvas, w, h)
        drawBowl(canvas)

        val (hx, hy) = sourceHome()
        val cx = if (itemX == 0f) hx else itemX
        val cy = if (itemY == 0f) hy else itemY
        drawContainer(canvas, cx, cy)

        if (pouringSince > 0L && !done) {
            val t = ((System.currentTimeMillis() - pouringSince) / 900f).coerceIn(0f, 1f)
            drawPourStream(canvas, cx, cy, t)
            if (t < 1f) {
                host.postInvalidateOnAnimation()
            } else {
                done = true
                finish()
            }
        }
    }

    private fun drawBowl(canvas: Canvas) {
        val r = bowlRect()
        paint.color = Color.rgb(116, 103, 167)
        canvas.drawOval(r, paint)
        paint.color = Color.rgb(166, 151, 213)
        canvas.drawOval(
            RectF(r.left + r.width() * .06f, r.top + r.height() * .06f, r.right - r.width() * .06f, r.centerY()),
            paint
        )
        paint.color = Color.rgb(239, 225, 198)
        canvas.drawOval(
            RectF(r.left + r.width() * .12f, r.top + r.height() * .10f, r.right - r.width() * .12f, r.centerY() - r.height() * .02f),
            paint
        )
    }

    private fun drawContainer(canvas: Canvas, cx: Float, cy: Float) {
        val flour = step.input == "flour"
        val rw = if (flour) w * .22f else w * .15f
        val rh = if (flour) h * .14f else h * .15f
        canvas.save()
        if (pouringSince > 0L) canvas.rotate(-45f, cx, cy)
        paint.color = if (flour) Color.rgb(242, 211, 133) else Color.rgb(190, 226, 240)
        canvas.drawRoundRect(RectF(cx-rw/2, cy-rh/2, cx+rw/2, cy+rh/2), 22f, 22f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * .006f
        paint.color = Color.rgb(93, 78, 62)
        canvas.drawRoundRect(RectF(cx-rw/2, cy-rh/2, cx+rw/2, cy+rh/2), 22f, 22f, paint)
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = w * .033f
        paint.isFakeBoldText = true
        paint.color = Color.rgb(78, 65, 54)
        canvas.drawText(if (flour) "밀가루" else "물", cx, cy + w * .012f, paint)
        paint.isFakeBoldText = false
        canvas.restore()
    }

    private fun drawPourStream(canvas: Canvas, cx: Float, cy: Float, t: Float) {
        val bowl = bowlRect()
        val ex = bowl.centerX()
        val ey = bowl.top + bowl.height() * .18f
        if (step.input == "flour") {
            paint.color = Color.rgb(246, 234, 211)
            val count = (12 * t).toInt().coerceAtLeast(3)
            repeat(count) { i ->
                val f = (i + 1f) / (count + 1f)
                val x = cx + (ex - cx) * f + sin(i * 1.7f) * w * .012f
                val y = cy + (ey - cy) * f
                canvas.drawCircle(x, y, w * .009f, paint)
            }
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = w * .018f
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = Color.rgb(121, 191, 221)
            canvas.drawLine(cx - w * .03f, cy, ex, ey, paint)
            paint.style = Paint.Style.FILL
            paint.strokeCap = Paint.Cap.BUTT
        }
    }

    override fun onTouch(event: MotionEvent): Boolean {
        if (done) return true
        val (hx, hy) = if (itemX == 0f) sourceHome() else itemX to itemY
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!near(event.x, event.y, hx, hy, w * .24f)) return true
                active = true
                itemX = event.x
                itemY = event.y
            }
            MotionEvent.ACTION_MOVE -> if (active && pouringSince == 0L) {
                itemX = event.x
                itemY = event.y
                val b = bowlRect()
                val pourZone = RectF(b.left, b.top - h * .15f, b.right, b.centerY())
                if (pourZone.contains(event.x, event.y)) {
                    pouringSince = System.currentTimeMillis()
                    haptic()
                }
                host.invalidate()
            }
            MotionEvent.ACTION_UP -> {
                active = false
                if (pouringSince == 0L) {
                    itemX = 0f
                    itemY = 0f
                }
                host.invalidate()
            }
        }
        return true
    }
}

class MixScene(
    host: View,
    step: RecipeStep,
    parts: Map<String, PartDefinition>,
    dishParts: List<String>,
    onComplete: () -> Unit
) : BaseCookingScene(host, step, parts, dishParts, onComplete) {
    private var active = false
    private var spoonX = 0f
    private var spoonY = 0f
    private var lastAngle = 0.0
    private var angularTravel = 0.0
    private var done = false

    override val instruction: String
        get() = if (done) "반죽이 되었어요!" else step.instruction

    private fun bowlRect() = RectF(w * .24f, h * .36f, w * .76f, h * .67f)
    private fun spoonHome() = w * .69f to h * .63f

    override fun draw(canvas: Canvas) {
        SceneArt.drawBoard(canvas, w, h)
        val b = bowlRect()
        paint.color = Color.rgb(116, 103, 167)
        canvas.drawOval(b, paint)
        paint.color = Color.rgb(166, 151, 213)
        canvas.drawOval(RectF(b.left+b.width()*.06f,b.top+b.height()*.06f,b.right-b.width()*.06f,b.centerY()),paint)

        val p = (angularTravel / (2 * PI * step.repeat.coerceAtLeast(1))).toFloat().coerceIn(0f,1f)
        paint.color = Color.rgb(
            (242 - 22*p).toInt(),
            (227 - 28*p).toInt(),
            (195 - 36*p).toInt()
        )
        canvas.drawOval(RectF(b.left+b.width()*.12f,b.top+b.height()*.14f,b.right-b.width()*.12f,b.bottom-b.height()*.24f),paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * .011f
        paint.color = Color.argb(120, 169, 128, 77)
        repeat(3) { i ->
            val rr = RectF(
                b.centerX()-w*(.10f+i*.025f),
                b.centerY()-w*(.055f+i*.014f),
                b.centerX()+w*(.10f+i*.025f),
                b.centerY()+w*(.055f+i*.014f)
            )
            canvas.drawArc(rr, 20f + i*45f, 210f, false, paint)
        }
        paint.style = Paint.Style.FILL

        val (hx,hy)=spoonHome()
        SceneArt.drawSpoon(canvas,w,if(spoonX==0f)hx else spoonX,if(spoonY==0f)hy else spoonY)

        if (done) {
            part(step.output)?.let {
                FoodPainter.drawPart(canvas,it,RectF(w*.37f,h*.45f,w*.63f,h*.60f),assets)
            }
        }
    }

    override fun onTouch(event: MotionEvent): Boolean {
        if (done) return true
        val b=bowlRect()
        val cx=b.centerX()
        val cy=b.centerY()
        val (hx,hy)=if(spoonX==0f)spoonHome() else spoonX to spoonY
        when(event.action){
            MotionEvent.ACTION_DOWN -> {
                if(!near(event.x,event.y,hx,hy,w*.22f)) return true
                active=true
                spoonX=event.x
                spoonY=event.y
                lastAngle=atan2((event.y-cy).toDouble(),(event.x-cx).toDouble())
            }
            MotionEvent.ACTION_MOVE -> if(active){
                spoonX=event.x
                spoonY=event.y
                if(b.contains(event.x,event.y)){
                    val a=atan2((event.y-cy).toDouble(),(event.x-cx).toDouble())
                    var d=a-lastAngle
                    while(d>PI)d-=2*PI
                    while(d< -PI)d+=2*PI
                    angularTravel+=abs(d)
                    lastAngle=a
                    if(angularTravel>=2*PI*step.repeat.coerceAtLeast(1)){
                        done=true
                        haptic(false)
                        finish()
                    }
                }
                host.invalidate()
            }
            MotionEvent.ACTION_UP -> active=false
        }
        return true
    }
}

class SauceCookScene(
    host: View,
    step: RecipeStep,
    parts: Map<String, PartDefinition>,
    dishParts: List<String>,
    onComplete: () -> Unit
) : BaseCookingScene(host, step, parts, dishParts, onComplete) {
    private var active=false
    private var spoonX=0f
    private var spoonY=0f
    private var lastX=0f
    private var lastY=0f
    private var progress=0f
    private var done=false

    override val instruction:String
        get()=if(done)"소스가 완성됐어요!" else step.instruction

    private fun panRect()=RectF(w*.24f,h*.35f,w*.76f,h*.66f)
    private fun spoonHome()=w*.72f to h*.66f

    override fun draw(canvas:Canvas){
        SceneArt.drawBoard(canvas,w,h)
        val r=panRect()
        paint.color=Color.rgb(60,63,65)
        canvas.drawOval(r,paint)
        paint.color=Color.rgb(93,96,98)
        canvas.drawOval(RectF(r.left+r.width()*.07f,r.top+r.height()*.08f,r.right-r.width()*.07f,r.bottom-r.height()*.08f),paint)

        if(progress<.88f){
            val p=part(step.input)
            val size=w*.14f
            val pts=listOf(-.17f to -.08f,.10f to -.10f,-.04f to .10f)
            pts.forEach{(ox,oy)->
                p?.let{
                    FoodPainter.drawPart(canvas,it,RectF(
                        r.centerX()+r.width()*ox-size/2,
                        r.centerY()+r.height()*oy-size/2,
                        r.centerX()+r.width()*ox+size/2,
                        r.centerY()+r.height()*oy+size/2
                    ),assets,(255*(1f-progress)).toInt().coerceAtLeast(40))
                }
            }
        }
        paint.color=Color.argb((235*progress).toInt().coerceIn(0,235),210,62,42)
        canvas.drawOval(RectF(r.left+r.width()*.17f,r.top+r.height()*.22f,r.right-r.width()*.17f,r.bottom-r.height()*.22f),paint)

        val(hx,hy)=spoonHome()
        SceneArt.drawSpoon(canvas,w,if(spoonX==0f)hx else spoonX,if(spoonY==0f)hy else spoonY)
    }

    override fun onTouch(event:MotionEvent):Boolean{
        if(done)return true
        val r=panRect()
        val(hx,hy)=if(spoonX==0f)spoonHome() else spoonX to spoonY
        when(event.action){
            MotionEvent.ACTION_DOWN->{
                if(!near(event.x,event.y,hx,hy,w*.22f))return true
                active=true
                spoonX=event.x;spoonY=event.y;lastX=event.x;lastY=event.y
            }
            MotionEvent.ACTION_MOVE->if(active){
                val dx=event.x-lastX
                val dy=event.y-lastY
                spoonX=event.x;spoonY=event.y
                if(r.contains(event.x,event.y)){
                    progress=(progress+hypot(dx.toDouble(),dy.toDouble()).toFloat()/(w*step.repeat*.65f)).coerceAtMost(1f)
                    if(progress>=1f){done=true;finish()}
                }
                lastX=event.x;lastY=event.y
                host.invalidate()
            }
            MotionEvent.ACTION_UP->active=false
        }
        return true
    }
}

class CheeseGrateScene(
    host: View,
    step: RecipeStep,
    parts: Map<String, PartDefinition>,
    dishParts: List<String>,
    onComplete: () -> Unit
) : BaseCookingScene(host, step, parts, dishParts, onComplete) {
    private var active=false
    private var cheeseY=0f
    private var lastY=0f
    private var progress=0f
    private var done=false

    override val instruction:String
        get()=if(done)"치즈가 듬뿍 올라갔어요!" else step.instruction

    private fun pizzaRect()=RectF(w*.20f,h*.34f,w*.70f,h*.65f)
    private fun graterRect()=RectF(w*.70f,h*.37f,w*.84f,h*.64f)
    private fun cheeseHome()=w*.77f to h*.72f

    override fun draw(canvas:Canvas){
        SceneArt.drawBoard(canvas,w,h)
        val pr=pizzaRect()
        val base=dishParts.lastOrNull()?.let(parts::get)
        base?.let{FoodPainter.drawPart(canvas,it,pr,assets)}

        val shredCount=(progress*28).toInt()
        paint.strokeCap=Paint.Cap.ROUND
        paint.strokeWidth=w*.012f
        paint.color=Color.rgb(248,205,74)
        repeat(shredCount){i->
            val fx=((i*37)%100)/100f
            val fy=((i*61)%100)/100f
            val x=pr.left+pr.width()*(.12f+.76f*fx)
            val y=pr.top+pr.height()*(.15f+.70f*fy)
            canvas.drawLine(x-w*.012f,y-w*.008f,x+w*.015f,y+w*.009f,paint)
        }
        paint.strokeCap=Paint.Cap.BUTT

        if(done){
            part(step.output)?.let{FoodPainter.drawPart(canvas,it,pr,assets)}
            return
        }

        val gr=graterRect()
        paint.color=Color.rgb(180,187,189)
        canvas.drawRoundRect(gr,16f,16f,paint)
        paint.color=Color.rgb(105,112,115)
        repeat(4){row->
            repeat(3){col->
                canvas.drawCircle(
                    gr.left+gr.width()*(.24f+col*.26f),
                    gr.top+gr.height()*(.18f+row*.20f),
                    w*.008f,paint
                )
            }
        }
        val(hx,hy)=cheeseHome()
        val cy=if(cheeseY==0f)hy else cheeseY
        drawCheeseWedge(canvas,hx,cy)
    }

    private fun drawCheeseWedge(canvas:Canvas,cx:Float,cy:Float){
        val p=Path().apply{
            moveTo(cx-w*.07f,cy+w*.05f)
            lineTo(cx+w*.07f,cy+w*.05f)
            lineTo(cx+w*.04f,cy-w*.055f)
            close()
        }
        paint.color=Color.rgb(255,211,83)
        canvas.drawPath(p,paint)
        paint.color=Color.rgb(208,159,45)
        repeat(3){i->canvas.drawCircle(cx-w*.025f+i*w*.025f,cy+w*.015f-i*w*.012f,w*.008f,paint)}
    }

    override fun onTouch(event:MotionEvent):Boolean{
        if(done)return true
        val(hx,hy)=cheeseHome()
        val cy=if(cheeseY==0f)hy else cheeseY
        when(event.action){
            MotionEvent.ACTION_DOWN->{
                if(!near(event.x,event.y,hx,cy,w*.18f))return true
                active=true
                cheeseY=event.y
                lastY=event.y
            }
            MotionEvent.ACTION_MOVE->if(active){
                val dy=event.y-lastY
                cheeseY=event.y.coerceIn(graterRect().top,graterRect().bottom+h*.12f)
                if(graterRect().contains(hx,cheeseY)){
                    progress=(progress+abs(dy)/(h*step.repeat*.12f)).coerceAtMost(1f)
                    if(progress>=1f){done=true;finish()}
                }
                lastY=event.y
                host.invalidate()
            }
            MotionEvent.ACTION_UP->active=false
        }
        return true
    }
}

class BurgerGrillScene(
    host: View,
    step: RecipeStep,
    parts: Map<String, PartDefinition>,
    dishParts: List<String>,
    onComplete: () -> Unit
) : BaseCookingScene(host, step, parts, dishParts, onComplete) {
    private var phase=0
    private var active=false
    private var pattyX=0f
    private var pattyY=0f
    private var spatX=0f
    private var spatY=0f
    private var under=false
    private var liftStartY=0f
    private var cookStart=0L
    private var flipStart=0L
    private var flipping=false

    override val instruction:String
        get()=when{
            flipping->"휙!"
            phase==0->"패티를 집어 그릴 위에 올려요"
            phase==1->"지글지글 익는 중..."
            phase==2&&!under->"뒤집개를 패티 밑으로 넣어요"
            phase==2&&under->"그대로 위로 들어 올려요"
            phase==3->"반대쪽도 익는 중..."
            else->"패티가 완성됐어요!"
        }

    private fun grillRect()=RectF(w*.14f,h*.34f,w*.86f,h*.64f)
    private fun pattyRect()=RectF(w*.35f,h*.43f,w*.65f,h*.58f)
    private fun rawHome()=w*.50f to h*.78f
    private fun spatHome()=w*.78f to h*.69f

    override fun draw(canvas:Canvas){
        SceneArt.drawBoard(canvas,w,h)
        val g=grillRect()
        paint.color=Color.rgb(55,57,59)
        canvas.drawRoundRect(g,22f,22f,paint)
        paint.color=Color.rgb(82,84,86)
        repeat(7){i->
            val y=g.top+g.height()*(.12f+i*.125f)
            canvas.drawRoundRect(RectF(g.left+w*.03f,y,g.right-w*.03f,y+w*.012f),6f,6f,paint)
        }

        when{
            phase==0->{
                val(hx,hy)=rawHome()
                val cx=if(pattyX==0f)hx else pattyX
                val cy=if(pattyY==0f)hy else pattyY
                part(step.input)?.let{FoodPainter.drawPart(canvas,it,RectF(cx-w*.18f,cy-w*.09f,cx+w*.18f,cy+w*.09f),assets)}
            }
            flipping->drawFlip(canvas)
            else->{
                val progress=when(phase){
                    1->(((System.currentTimeMillis()-cookStart)/1300f).coerceIn(0f,1f)*.48f)
                    2->.52f
                    3->(.52f+((System.currentTimeMillis()-cookStart)/1200f).coerceIn(0f,1f)*.48f).coerceAtMost(1f)
                    else->1f
                }
                drawMorph(canvas,step.input,step.output,pattyRect(),progress)
                if(phase==1||phase==3){
                    drawSizzle(canvas,progress)
                    if((phase==1&&progress<.48f)||(phase==3&&progress<1f)) host.postInvalidateOnAnimation()
                    else if(phase==1&&progress>=.48f){phase=2;haptic();host.invalidate()}
                    else if(phase==3&&progress>=1f){phase=4;finish()}
                }
            }
        }

        if(phase==2&&!flipping){
            val(hx,hy)=spatHome()
            SceneArt.drawSpatula(canvas,w,if(spatX==0f)hx else spatX,if(spatY==0f)hy else spatY)
            drawFlipHint(canvas)
        }
    }

    private fun drawSizzle(canvas:Canvas,p:Float){
        paint.color=Color.argb(180,255,235,170)
        repeat(6){i->
            val a=i*1.07f+p*3f
            canvas.drawCircle(w*.5f+cos(a)*w*.12f,h*.505f+sin(a)*w*.055f,w*.008f,paint)
        }
    }

    private fun drawFlipHint(canvas:Canvas){
        val r=pattyRect()
        paint.style=Paint.Style.STROKE
        paint.strokeWidth=w*.010f
        paint.color=Color.argb(135,70,140,220)
        canvas.drawLine(r.centerX(),r.bottom+h*.06f,r.centerX(),r.top-h*.02f,paint)
        paint.style=Paint.Style.FILL
        val p=Path().apply{
            moveTo(r.centerX(),r.top-h*.04f)
            lineTo(r.centerX()-w*.03f,r.top+h*.01f)
            lineTo(r.centerX()+w*.03f,r.top+h*.01f)
            close()
        }
        canvas.drawPath(p,paint)
    }

    private fun startCookFirst(){
        phase=1
        cookStart=System.currentTimeMillis()
        pattyX=0f;pattyY=0f
        haptic()
        host.postInvalidateOnAnimation()
    }

    private fun startFlip(){
        flipping=true
        flipStart=System.currentTimeMillis()
        active=false
        haptic(false)
        host.postInvalidateOnAnimation()
    }

    private fun drawFlip(canvas:Canvas){
        val t=((System.currentTimeMillis()-flipStart)/520f).coerceIn(0f,1f)
        val r=pattyRect()
        val lift=-sin(PI.toFloat()*t)*w*.10f
        val sy=abs(cos(PI.toFloat()*t)).coerceAtLeast(.09f)
        canvas.save()
        canvas.translate(r.centerX(),r.centerY()+lift)
        canvas.scale(1f,sy)
        drawMorph(canvas,step.input,step.output,RectF(-r.width()/2,-r.height()/2,r.width()/2,r.height()/2),.55f)
        canvas.restore()
        if(t<1f)host.postInvalidateOnAnimation()
        else{
            flipping=false
            phase=3
            cookStart=System.currentTimeMillis()
            spatX=0f;spatY=0f;under=false
            host.postInvalidateOnAnimation()
        }
    }

    override fun onTouch(event:MotionEvent):Boolean{
        if(flipping||phase==1||phase==3||phase==4)return true

        if(phase==0){
            val(hx,hy)=if(pattyX==0f)rawHome() else pattyX to pattyY
            when(event.action){
                MotionEvent.ACTION_DOWN->{
                    if(!near(event.x,event.y,hx,hy,w*.23f))return true
                    active=true;pattyX=event.x;pattyY=event.y
                }
                MotionEvent.ACTION_MOVE->if(active){pattyX=event.x;pattyY=event.y;host.invalidate()}
                MotionEvent.ACTION_UP->if(active){
                    if(grillRect().contains(event.x,event.y))startCookFirst()
                    else{pattyX=0f;pattyY=0f}
                    active=false;host.invalidate()
                }
            }
            return true
        }

        val(hx,hy)=if(spatX==0f)spatHome() else spatX to spatY
        when(event.action){
            MotionEvent.ACTION_DOWN->{
                if(!near(event.x,event.y,hx,hy,w*.22f))return true
                active=true;spatX=event.x;spatY=event.y
            }
            MotionEvent.ACTION_MOVE->if(active){
                val r=pattyRect()
                if(!under){
                    spatX=event.x;spatY=event.y
                    val zone=RectF(r.left,r.centerY(),r.right,r.bottom+h*.05f)
                    if(zone.contains(event.x,event.y)){
                        under=true
                        liftStartY=event.y
                        spatX=r.centerX()
                        spatY=r.bottom-r.height()*.10f
                        haptic()
                    }
                }else{
                    spatX=r.centerX()
                    spatY=event.y.coerceAtMost(r.bottom)
                    if(liftStartY-event.y>h*.06f)startFlip()
                }
                host.invalidate()
            }
            MotionEvent.ACTION_UP->{
                active=false
                if(!under){spatX=0f;spatY=0f}
                host.invalidate()
            }
        }
        return true
    }
}

class DoughPrepScene(
    host: View,
    step: RecipeStep,
    parts: Map<String, PartDefinition>,
    dishParts: List<String>,
    onComplete: () -> Unit
) : BaseCookingScene(host, step, parts, dishParts, onComplete) {
    private var phase = 0 // 0 flour, 1 water, 2 mix, 3 done
    private var active = false
    private var itemX = 0f
    private var itemY = 0f
    private var pouringSince = 0L
    private var spoonX = 0f
    private var spoonY = 0f
    private var lastAngle = 0.0
    private var angularTravel = 0.0

    override val instruction: String
        get() = when (phase) {
            0 -> "밀가루를 그릇에 부어요"
            1 -> "물을 넣어요"
            2 -> "숟가락으로 빙글빙글 섞어요"
            else -> "반죽이 완성됐어요!"
        }

    private fun bowlRect() = RectF(w * .25f, h * .37f, w * .75f, h * .67f)
    private fun sourceHome() = w * .78f to h * .75f
    private fun spoonHome() = w * .69f to h * .64f

    override fun draw(canvas: Canvas) {
        SceneArt.drawBoard(canvas, w, h)
        drawBowl(canvas)

        when (phase) {
            0, 1 -> {
                val (hx, hy) = sourceHome()
                val cx = if (itemX == 0f) hx else itemX
                val cy = if (itemY == 0f) hy else itemY
                drawIngredientContainer(canvas, cx, cy, phase == 0)

                if (pouringSince > 0L) {
                    val t = ((System.currentTimeMillis() - pouringSince) / 750f).coerceIn(0f, 1f)
                    drawStream(canvas, cx, cy, phase == 0, t)
                    if (t < 1f) {
                        host.postInvalidateOnAnimation()
                    } else {
                        phase += 1
                        active = false
                        itemX = 0f
                        itemY = 0f
                        pouringSince = 0L
                        haptic()
                        host.invalidate()
                    }
                }
            }

            2 -> {
                drawMixture(canvas)
                val (hx, hy) = spoonHome()
                SceneArt.drawSpoon(
                    canvas, w,
                    if (spoonX == 0f) hx else spoonX,
                    if (spoonY == 0f) hy else spoonY
                )
            }

            3 -> {
                part(step.output)?.let {
                    FoodPainter.drawPart(
                        canvas, it,
                        RectF(w * .37f, h * .45f, w * .63f, h * .60f),
                        assets
                    )
                }
            }
        }
    }

    private fun drawBowl(canvas: Canvas) {
        val b = bowlRect()
        paint.color = Color.rgb(112, 100, 165)
        canvas.drawOval(b, paint)
        paint.color = Color.rgb(166, 151, 213)
        canvas.drawOval(
            RectF(
                b.left + b.width() * .06f,
                b.top + b.height() * .06f,
                b.right - b.width() * .06f,
                b.centerY()
            ),
            paint
        )

        when (phase) {
            1 -> {
                paint.color = Color.rgb(244, 233, 211)
                canvas.drawOval(
                    RectF(
                        b.left + b.width() * .18f,
                        b.top + b.height() * .18f,
                        b.right - b.width() * .18f,
                        b.bottom - b.height() * .31f
                    ),
                    paint
                )
            }
            2, 3 -> {
                paint.color = Color.rgb(226, 199, 157)
                canvas.drawOval(
                    RectF(
                        b.left + b.width() * .14f,
                        b.top + b.height() * .16f,
                        b.right - b.width() * .14f,
                        b.bottom - b.height() * .25f
                    ),
                    paint
                )
            }
        }
    }

    private fun drawIngredientContainer(canvas: Canvas, cx: Float, cy: Float, flour: Boolean) {
        val rw = if (flour) w * .22f else w * .15f
        val rh = if (flour) h * .14f else h * .15f
        canvas.save()
        if (pouringSince > 0L) canvas.rotate(-45f, cx, cy)
        paint.color = if (flour) Color.rgb(242, 211, 133) else Color.rgb(190, 226, 240)
        canvas.drawRoundRect(RectF(cx-rw/2,cy-rh/2,cx+rw/2,cy+rh/2),22f,22f,paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * .006f
        paint.color = Color.rgb(91, 76, 61)
        canvas.drawRoundRect(RectF(cx-rw/2,cy-rh/2,cx+rw/2,cy+rh/2),22f,22f,paint)
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = w * .032f
        paint.isFakeBoldText = true
        paint.color = Color.rgb(75, 62, 51)
        canvas.drawText(if(flour)"밀가루" else "물",cx,cy+w*.012f,paint)
        paint.isFakeBoldText = false
        canvas.restore()
    }

    private fun drawStream(canvas: Canvas, cx: Float, cy: Float, flour: Boolean, t: Float) {
        val b = bowlRect()
        val ex = b.centerX()
        val ey = b.top + b.height() * .20f
        if (flour) {
            paint.color = Color.rgb(247, 235, 214)
            val count = (14 * t).toInt().coerceAtLeast(3)
            repeat(count) { i ->
                val f = (i + 1f) / (count + 1f)
                val x = cx + (ex - cx) * f + sin(i * 1.7f) * w * .012f
                val y = cy + (ey - cy) * f
                canvas.drawCircle(x,y,w*.009f,paint)
            }
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = w * .018f
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = Color.rgb(119, 190, 222)
            canvas.drawLine(cx-w*.03f,cy,ex,ey,paint)
            paint.style = Paint.Style.FILL
            paint.strokeCap = Paint.Cap.BUTT
        }
    }

    private fun drawMixture(canvas: Canvas) {
        val b = bowlRect()
        val progress = (angularTravel / (2 * PI * step.repeat.coerceAtLeast(1))).toFloat().coerceIn(0f,1f)
        paint.color = Color.rgb(
            (231 - 15 * progress).toInt(),
            (205 - 18 * progress).toInt(),
            (165 - 18 * progress).toInt()
        )
        canvas.drawOval(
            RectF(
                b.left+b.width()*.15f,
                b.top+b.height()*.19f,
                b.right-b.width()*.15f,
                b.bottom-b.height()*.24f
            ),paint
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * .010f
        paint.color = Color.argb(120, 151, 113, 73)
        repeat(3){i->
            val rr=RectF(
                b.centerX()-w*(.09f+i*.025f),
                b.centerY()-w*(.05f+i*.013f),
                b.centerX()+w*(.09f+i*.025f),
                b.centerY()+w*(.05f+i*.013f)
            )
            canvas.drawArc(rr,20f+i*40f,220f,false,paint)
        }
        paint.style = Paint.Style.FILL
    }

    override fun onTouch(event: MotionEvent): Boolean {
        if (phase == 3) return true

        if (phase == 0 || phase == 1) {
            val (hx, hy) = if (itemX == 0f) sourceHome() else itemX to itemY
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!near(event.x,event.y,hx,hy,w*.24f)) return true
                    active = true
                    itemX = event.x
                    itemY = event.y
                }
                MotionEvent.ACTION_MOVE -> if (active && pouringSince == 0L) {
                    itemX = event.x
                    itemY = event.y
                    val b = bowlRect()
                    val zone = RectF(b.left,b.top-h*.15f,b.right,b.centerY())
                    if (zone.contains(event.x,event.y)) {
                        pouringSince = System.currentTimeMillis()
                        haptic()
                    }
                    host.invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    active = false
                    if (pouringSince == 0L) {
                        itemX = 0f
                        itemY = 0f
                    }
                    host.invalidate()
                }
            }
            return true
        }

        val b = bowlRect()
        val cx = b.centerX()
        val cy = b.centerY()
        val (hx,hy)=if(spoonX==0f)spoonHome() else spoonX to spoonY
        when(event.action){
            MotionEvent.ACTION_DOWN->{
                if(!near(event.x,event.y,hx,hy,w*.22f))return true
                active=true
                spoonX=event.x
                spoonY=event.y
                lastAngle=atan2((event.y-cy).toDouble(),(event.x-cx).toDouble())
            }
            MotionEvent.ACTION_MOVE->if(active){
                spoonX=event.x
                spoonY=event.y
                if(b.contains(event.x,event.y)){
                    val a=atan2((event.y-cy).toDouble(),(event.x-cx).toDouble())
                    var d=a-lastAngle
                    while(d>PI)d-=2*PI
                    while(d< -PI)d+=2*PI
                    angularTravel+=abs(d)
                    lastAngle=a
                    if(angularTravel>=2*PI*step.repeat.coerceAtLeast(1)){
                        phase=3
                        haptic(false)
                        finish()
                    }
                }
                host.invalidate()
            }
            MotionEvent.ACTION_UP->active=false
        }
        return true
    }
}

class BurgerStackScene(
    host: View,
    step: RecipeStep,
    parts: Map<String, PartDefinition>,
    dishParts: List<String>,
    onComplete: () -> Unit
) : BaseCookingScene(host, step, parts, dishParts, onComplete) {
    private var active=false
    private var itemX=0f
    private var itemY=0f
    private var done=false

    override val instruction:String
        get()=if(done)"착!" else step.instruction

    private fun home()=w*.50f to h*.79f
    private fun target()=RectF(w*.25f,h*.36f,w*.75f,h*.65f)

    override fun draw(canvas:Canvas){
        SceneArt.drawPlate(canvas,w,h)
        drawBurger(canvas,if(done)step.dishAfter else dishParts)

        if(!done){
            val p=part(step.input)?:return
            val(hx,hy)=home()
            val cx=if(itemX==0f)hx else itemX
            val cy=if(itemY==0f)hy else itemY
            val size=w*(step.itemScale?:.34f)
            FoodPainter.drawPart(canvas,p,RectF(cx-size/2,cy-size*.32f,cx+size/2,cy+size*.32f),assets)
        }
    }

    private fun drawBurger(canvas:Canvas,ids:List<String>){
        if(ids.isEmpty())return
        val cx=w*.50f
        val bottom=h*.585f
        val gap=w*.044f
        ids.forEachIndexed{index,id->
            val p=part(id)?:return@forEachIndexed
            val cy=bottom-index*gap
            val widthScale=when(id){
                "bun_bottom"->.45f
                "patty_cooked"->.43f
                "cheese"->.44f
                "lettuce"->.46f
                "tomato_slices"->.22f
                "bun_top"->.47f
                else->.40f
            }
            val hScale=when(id){
                "bun_top"->.18f
                "bun_bottom"->.14f
                "tomato_slices"->.10f
                else->.13f
            }
            val rw=w*widthScale
            val rh=w*hScale
            FoodPainter.drawPart(canvas,p,RectF(cx-rw/2,cy-rh/2,cx+rw/2,cy+rh/2),assets)
        }
    }

    override fun onTouch(event:MotionEvent):Boolean{
        if(done)return true
        val(hx,hy)=if(itemX==0f)home() else itemX to itemY
        when(event.action){
            MotionEvent.ACTION_DOWN->{
                if(!near(event.x,event.y,hx,hy,w*.23f))return true
                active=true;itemX=event.x;itemY=event.y
            }
            MotionEvent.ACTION_MOVE->if(active){itemX=event.x;itemY=event.y;host.invalidate()}
            MotionEvent.ACTION_UP->if(active){
                if(target().contains(event.x,event.y)){
                    done=true
                    haptic()
                    finish()
                }else{itemX=0f;itemY=0f}
                active=false
                host.invalidate()
            }
        }
        return true
    }
}
