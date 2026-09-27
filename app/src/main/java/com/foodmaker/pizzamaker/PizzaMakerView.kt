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

class PizzaMakerView(context:Context):View(context){
    private val art=CrayonArt();private val assets=AssetBook(context);private val food=PizzaPainter(assets);private val g=GameState()
    private val prep=PrepPainter(art,food);private val finish=FinishPainter(art,food,assets){ding()};private val tone=ToneGenerator(AudioManager.STREAM_MUSIC,45)
    private var sc=1f;private var ox=0f;private var oy=0f
    override fun onDetachedFromWindow(){tone.release();super.onDetachedFromWindow()}
    override fun onDraw(c:Canvas){super.onDraw(c);sc=min(width/1080f,height/1920f);ox=(width-1080f*sc)/2;oy=(height-1920f*sc)/2;c.save();c.translate(ox,oy);c.scale(sc,sc);art.paper(c);art.kitchen(c);prep.draw(c,g);finish.draw(c,g);top(c);c.restore();postInvalidateOnAnimation()}
    private fun top(c:Canvas){if(g.stage==Stage.WELCOME)return;art.box(c,RectF(38f,35f,1042f,125f),35f,Color.argb(225,255,249,230),600);val t=when(g.stage){Stage.MIX->"재료를 넣고 섞어봐!";Stage.ROLL->"반죽을 쭉쭉 밀어봐!";Stage.SAUCE->"소스를 빙글빙글 발라봐!";Stage.CHEESE->"치즈를 솔솔 뿌려봐!";Stage.TOPPINGS->"좋아하는 토핑을 올려봐!";Stage.OVEN->"피자를 오븐에 넣어봐!";Stage.CUT->"피자를 잘라봐!";Stage.EAT->"맛있게 먹어볼까?";Stage.DONE->"완성!";else->""};art.text(c,t,540f,94f,38f);art.circle(c,85f,80f,31f,Color.rgb(247,208,91),601);art.text(c,"‹",85f,93f,45f)}
    override fun onTouchEvent(e:MotionEvent):Boolean{val x=(e.x-ox)/sc;val y=(e.y-oy)/sc;g.prevX=g.touchX;g.prevY=g.touchY;g.touchX=x;g.touchY=y;g.lastInput=SystemClock.uptimeMillis();when(e.actionMasked){MotionEvent.ACTION_DOWN->{g.dragging=true;down(x,y)};MotionEvent.ACTION_MOVE->move(x,y);MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{up(x,y);g.dragging=false}};invalidate();return true}
    private fun down(x:Float,y:Float){if(g.stage!=Stage.WELCOME&&x in 42f..128f&&y in 38f..125f){g.back();pop();return};when(g.stage){
        Stage.WELCOME->if(x in 240f..840f&&y in 1340f..1570f)go(Stage.MIX)
        Stage.MIX->if(!g.mixAdded.all{it})g.dragIngredient=when{d(x,y,230f,640f)<130->0;d(x,y,540f,640f)<130->1;d(x,y,850f,640f)<130->2;else->-1}else if(d(x,y,700f,1080f)<200){g.toolHeld=true;g.lastStirAngle=ang(x,y,540f,1160f)}
        Stage.ROLL->if(y>1270||d(x,y,540f,1420f)<300)g.toolHeld=true
        Stage.SAUCE->if(d(x,y,760f,1550f)<220||d(x,y,230f,1580f)<190)g.toolHeld=true
        Stage.CHEESE->if(d(x,y,790f,1530f)<220)g.toolHeld=true
        Stage.TOPPINGS->{if(g.toppings.size>=4&&x in 300f..780f&&y in 1570f..1780f){go(Stage.OVEN);return};val i=((x-22)/207).toInt();if(y in 1280f..1500f&&i in 0..4)g.selected=ToppingType.values()[i]}
        Stage.OVEN->if(!g.baking&&d(x,y,g.ovenX,g.ovenY)<330)g.pizzaHeld=true else if(g.bakeDone&&y>1370)go(Stage.CUT)
        Stage.CUT->{if(g.cutCount>=3&&y>1420){go(Stage.EAT);return};g.cutStart=PointF(x,y);g.cutterX=x;g.cutterY=y}
        Stage.EAT->{if(g.eaten.all{it}){if(y>1500)go(Stage.DONE);return};val dx=x-540;val dy=y-960;if(hypot(dx,dy)<345){var a=Math.toDegrees(atan2(dy,dx).toDouble()).toFloat()+90;if(a<0)a+=360;val i=(a/60).toInt().coerceIn(0,5);if(!g.eaten[i]){g.eaten[i]=true;pop()}}}
        Stage.DONE->if(x in 250f..830f&&y in 1230f..1490f){g.reset();pop()}
    }}
    private fun move(x:Float,y:Float){when(g.stage){
        Stage.MIX->if(g.toolHeld&&g.mixAdded.all{it}){val a=ang(x,y,540f,1160f);g.lastStirAngle?.let{old->var z=abs(a-old);if(z>180)z=360-z;g.stir=(g.stir+z/620).coerceAtMost(1f)};g.lastStirAngle=a;if(g.stir>=1){g.toolHeld=false;ding()}}
        Stage.ROLL->if(g.toolHeld&&d(x,y,540f,1120f)<440){g.roll=(g.roll+hypot(x-g.prevX,y-g.prevY)/1800).coerceAtMost(1f)}
        Stage.SAUCE->if(g.toolHeld&&d(x,y,540f,1050f)<300){val z=hypot(x-g.prevX,y-g.prevY);if(z>4){g.sauceMarks.add(PointF(x,y));g.sauce=(g.sauce+z/1350).coerceAtMost(1f)}}
        Stage.CHEESE->if(g.toolHeld&&d(x,y,540f,1040f)<300){val z=hypot(x-g.prevX,y-g.prevY);if(z>6){repeat(2){k->g.cheeseBits.add(Sprinkle(x+k*18-9,y+((k+1)%2*14-7),(x+y+k*71)%180,30f+k*13))};g.cheese=(g.cheese+z/1200).coerceAtMost(1f)}}
        Stage.OVEN->if(g.pizzaHeld){g.ovenX=x;g.ovenY=y;if(y<940&&d(x,y,540f,690f)<330){g.pizzaHeld=false;g.baking=true;g.bakeStart=SystemClock.uptimeMillis();pop()}}
        Stage.CUT->{val dx=x-g.prevX;val dy=y-g.prevY;g.cutterX=x;g.cutterY=y;if(abs(dx)+abs(dy)>2)g.cutterRot=Math.toDegrees(atan2(dy,dx).toDouble()).toFloat()}
        else->{}
    }}
    private fun up(x:Float,y:Float){when(g.stage){
        Stage.MIX->{if(g.dragIngredient>=0){if(d(x,y,540f,1160f)<300){g.mixAdded[g.dragIngredient]=true;pop()};g.dragIngredient=-1};if(g.stir>=1&&y>1370)go(Stage.ROLL);g.toolHeld=false;g.lastStirAngle=null}
        Stage.ROLL->{g.toolHeld=false;if(g.roll>.92&&y>1450)go(Stage.SAUCE)}
        Stage.SAUCE->{g.toolHeld=false;if(g.sauce>.82&&y>1400)go(Stage.CHEESE)}
        Stage.CHEESE->{g.toolHeld=false;if(g.cheese>.78&&y>1400)go(Stage.TOPPINGS)}
        Stage.TOPPINGS->{g.selected?.let{t->if(d(x,y,540f,830f)<310){val nx=((x-540)/(350*1.55f)).coerceIn(-.48f,.48f);val ny=((y-830)/(350*1.55f)).coerceIn(-.48f,.48f);val s=g.toppings.size*47+t.ordinal*19;g.toppings.add(ToppingPiece(t,nx,ny,(s%45-22).toFloat(),.9f+(s%17)/100f));pop()}};g.selected=null}
        Stage.OVEN->{g.pizzaHeld=false;if(g.bakeDone&&y>1370)go(Stage.CUT)}
        Stage.CUT->{g.cutStart?.let{s->val len=hypot(x-s.x,y-s.y);val mx=(x+s.x)/2;val my=(y+s.y)/2;if(len>430&&d(mx,my,540f,960f)<150){var a=Math.toDegrees(atan2(y-s.y,x-s.x).toDouble()).toFloat();while(a<0)a+=180;while(a>=180)a-=180;if(g.cutAngles.none{diff(it,a)<24}&&g.cutAngles.size<3){g.cutAngles.add(a);g.cutCount++;pop();if(g.cutCount==3)ding()}}};g.cutStart=null}
        else->{}
    }}
    private fun go(s:Stage){g.go(s);pop()}
    private fun pop(){tone.startTone(ToneGenerator.TONE_PROP_ACK,55);performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)}
    private fun ding(){tone.startTone(ToneGenerator.TONE_PROP_BEEP2,170);performHapticFeedback(HapticFeedbackConstants.CONFIRM)}
    private fun d(x:Float,y:Float,a:Float,b:Float)=hypot(x-a,y-b)
    private fun ang(x:Float,y:Float,a:Float,b:Float)=Math.toDegrees(atan2(y-b,x-a).toDouble()).toFloat()
    private fun diff(a:Float,b:Float):Float{var q=abs(a-b)%180;if(q>90)q=180-q;return q}
}
