package com.foodmaker.pizzamaker

import android.graphics.*
import kotlin.math.*

class CrayonArt {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
    private fun j(s:Int,a:Float):Float{val v=sin(s*12.9898+78.233)*43758.5453;return((v-floor(v))*2-1).toFloat()*a}
    fun line(c:Canvas,x1:Float,y1:Float,x2:Float,y2:Float,col:Int,w:Float,s:Int=0){p.style=Paint.Style.STROKE;repeat(3){i->p.color=if(i==0)col else Color.argb(120,Color.red(col),Color.green(col),Color.blue(col));p.strokeWidth=w-i*.8f;c.drawLine(x1+j(s+i,2f),y1+j(s+7+i,2f),x2+j(s+13+i,2f),y2+j(s+19+i,2f),p)}}
    fun circle(c:Canvas,x:Float,y:Float,r:Float,fill:Int,s:Int=0){p.style=Paint.Style.FILL;p.color=fill;c.drawCircle(x,y,r,p);p.style=Paint.Style.STROKE;p.strokeWidth=5f;p.color=Color.rgb(53,45,39);repeat(2){i->c.drawCircle(x+j(s+i,1.7f),y+j(s+8+i,1.7f),r+j(s+16+i,1.3f),p)}}
    fun box(c:Canvas,r:RectF,rad:Float,fill:Int,s:Int=0){p.style=Paint.Style.FILL;p.color=fill;c.drawRoundRect(r,rad,rad,p);p.style=Paint.Style.STROKE;p.color=Color.rgb(55,46,39);p.strokeWidth=5f;c.drawRoundRect(RectF(r.left+j(s,1.5f),r.top+j(s+1,1.5f),r.right+j(s+2,1.5f),r.bottom+j(s+3,1.5f)),rad,rad,p)}
    fun text(c:Canvas,t:String,x:Float,y:Float,size:Float,col:Int=Color.rgb(45,39,34)){p.style=Paint.Style.FILL;p.color=col;p.textSize=size;p.typeface=Typeface.create("sans",Typeface.BOLD);p.textAlign=Paint.Align.CENTER;c.drawText(t,x,y,p)}
    fun paper(c:Canvas){c.drawColor(Color.rgb(255,250,238));p.style=Paint.Style.STROKE;p.strokeWidth=1.2f;p.color=Color.argb(20,145,110,70);repeat(95){i->val y=10+i*20f+j(i,3f);c.drawLine(0f,y,1080f,y+j(i+200,8f),p)}}
    fun kitchen(c:Canvas){
        box(c,RectF(706f,140f,1015f,510f),22f,Color.rgb(214,239,241),10);p.style=Paint.Style.FILL;p.color=Color.rgb(247,220,112);c.drawCircle(922f,240f,43f,p);line(c,860f,145f,860f,505f,Color.DKGRAY,7f,11);line(c,710f,330f,1010f,330f,Color.DKGRAY,7f,12)
        line(c,70f,280f,520f,280f,Color.rgb(105,73,48),18f,15);val cs=intArrayOf(Color.rgb(245,185,92),Color.rgb(159,202,130),Color.rgb(232,146,150),Color.rgb(206,173,218));repeat(4){i->box(c,RectF(100f+i*102,170f+(i%2)*12,166f+i*102,270f),14f,cs[i],20+i)}
        p.style=Paint.Style.FILL;p.color=Color.rgb(232,184,124);c.drawRect(0f,575f,1080f,920f,p);line(c,0f,580f,1080f,580f,Color.rgb(92,66,47),8f,30);repeat(5){i->line(c,i*216f,580f,i*216f,920f,Color.rgb(134,93,61),4f,31+i)}
        p.color=Color.rgb(210,160,105);c.drawRect(0f,820f,1080f,1920f,p);p.color=Color.argb(26,90,58,37);repeat(42){i->val y=850f+i*24;c.drawOval(RectF((i*137%250)-35f,y,940f+(i*31%120),y+15),p)};line(c,0f,835f,1080f,835f,Color.rgb(94,63,42),10f,40)
    }
    fun bowl(c:Canvas,x:Float,y:Float,w:Float,h:Float,fill:Int=Color.rgb(232,244,239)){val q=Path().apply{moveTo(x-w/2,y-h/2);quadTo(x-w*.38f,y+h*.48f,x,y+h/2);quadTo(x+w*.38f,y+h*.48f,x+w/2,y-h/2);close()};p.style=Paint.Style.FILL;p.color=fill;c.drawPath(q,p);p.style=Paint.Style.STROKE;p.strokeWidth=6f;p.color=Color.rgb(54,48,43);c.drawPath(q,p)}
    fun flour(c:Canvas,x:Float,y:Float){val q=Path().apply{moveTo(x-75,y-100);lineTo(x+65,y-95);lineTo(x+82,y+100);lineTo(x-82,y+100);close()};p.style=Paint.Style.FILL;p.color=Color.rgb(247,238,213);c.drawPath(q,p);p.style=Paint.Style.STROKE;p.strokeWidth=5f;p.color=Color.rgb(63,54,46);c.drawPath(q,p);text(c,"밀가루",x,y+12,29f)}
    fun cup(c:Canvas,x:Float,y:Float){box(c,RectF(x-67,y-88,x+67,y+88),18f,Color.rgb(213,239,247),50);p.style=Paint.Style.FILL;p.color=Color.argb(150,85,191,225);c.drawRect(x-58,y-18,x+58,y+78,p);p.style=Paint.Style.STROKE;p.strokeWidth=8f;p.color=Color.DKGRAY;c.drawArc(RectF(x+45,y-47,x+118,y+45),-70f,215f,false,p)}
    fun oil(c:Canvas,x:Float,y:Float){box(c,RectF(x-48,y-70,x+48,y+95),22f,Color.rgb(229,213,81),60);box(c,RectF(x-22,y-120,x+22,y-68),8f,Color.rgb(87,150,93),61)}
    fun roller(c:Canvas,x:Float,y:Float,a:Float=0f){c.save();c.rotate(a,x,y);box(c,RectF(x-170,y-42,x+170,y+42),38f,Color.rgb(218,153,88),70);box(c,RectF(x-235,y-24,x-165,y+24),20f,Color.rgb(179,112,61),71);box(c,RectF(x+165,y-24,x+235,y+24),20f,Color.rgb(179,112,61),72);c.restore()}
    fun spoon(c:Canvas,x:Float,y:Float,a:Float=-25f){c.save();c.rotate(a,x,y);line(c,x,y+8,x,y+205,Color.rgb(113,86,67),18f,80);circle(c,x,y-28,50f,Color.rgb(211,215,213),81);c.restore()}
    fun shaker(c:Canvas,x:Float,y:Float,a:Float=0f){c.save();c.rotate(a,x,y);box(c,RectF(x-58,y-90,x+58,y+90),22f,Color.rgb(255,220,91),90);p.style=Paint.Style.FILL;p.color=Color.rgb(220,226,222);c.drawRoundRect(RectF(x-52,y-104,x+52,y-65),14f,14f,p);text(c,"치즈",x,y+12,28f);c.restore()}
    fun oven(c:Canvas,x:Float,y:Float,open:Boolean,glow:Float){box(c,RectF(x-350,y-280,x+350,y+300),42f,Color.rgb(233,109,76),100);box(c,RectF(x-285,y-175,x+285,y+170),24f,Color.rgb(62,55,51),101);p.style=Paint.Style.FILL;p.color=Color.rgb((78+120*glow).toInt().coerceAtMost(255),(61+55*glow).toInt(),(54-15*glow).toInt().coerceAtLeast(0));c.drawRoundRect(RectF(x-260,y-150,x+260,y+145),20f,20f,p);if(open){val q=Path().apply{moveTo(x-285,y+170);lineTo(x+285,y+170);lineTo(x+330,y+430);lineTo(x-330,y+430);close()};p.color=Color.rgb(79,70,65);c.drawPath(q,p);p.style=Paint.Style.STROKE;p.strokeWidth=7f;p.color=Color.rgb(49,44,42);c.drawPath(q,p)}}
    fun cutter(c:Canvas,x:Float,y:Float,a:Float){c.save();c.rotate(a,x,y);circle(c,x-50,y,55f,Color.rgb(210,214,213),110);line(c,x,y,x+170,y,Color.rgb(79,83,80),20f,111);box(c,RectF(x+145,y-32,x+275,y+32),28f,Color.rgb(97,174,132),112);c.restore()}
    fun plate(c:Canvas,x:Float,y:Float,r:Float){circle(c,x,y,r,Color.rgb(244,244,235),120);p.style=Paint.Style.STROKE;p.strokeWidth=6f;p.color=Color.rgb(145,195,188);c.drawCircle(x,y,r*.82f,p)}
    fun arrow(c:Canvas,x1:Float,y1:Float,x2:Float,y2:Float,a:Int=210){p.style=Paint.Style.STROKE;p.strokeWidth=13f;p.color=Color.argb(a,66,143,88);c.drawLine(x1,y1,x2,y2,p);val q=atan2(y2-y1,x2-x1);c.drawLine(x2,y2,x2-cos(q-.7f)*34,y2-sin(q-.7f)*34,p);c.drawLine(x2,y2,x2-cos(q+.7f)*34,y2-sin(q+.7f)*34,p)}
}
