package com.foodmaker.pizzamaker

import android.graphics.*

class PizzaPainter(private val assets:AssetBook){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{isFilterBitmap=true}
    fun bitmap(c:Canvas,b:Bitmap,x:Float,y:Float,w:Float,h:Float,a:Float=0f,alpha:Int=255){c.save();c.rotate(a,x,y);p.alpha=alpha;c.drawBitmap(b,null,RectF(x-w/2,y-h/2,x+w/2,y+h/2),p);p.alpha=255;c.restore()}
    fun dough(c:Canvas,x:Float,y:Float,r:Float)=bitmap(c,assets.dough,x,y,r*2.1f,r*2.1f)
    fun pizza(c:Canvas,x:Float,y:Float,r:Float,sauce:Float=1f,cheese:Float=1f){dough(c,x,y,r);if(sauce>0)bitmap(c,assets.sauce,x,y,r*2.02f,r*2.02f,alpha=(255*sauce).toInt());if(cheese>0)bitmap(c,assets.cheese,x,y,r*1.48f,r*1.48f,alpha=(255*cheese).toInt())}
    fun topping(c:Canvas,t:ToppingPiece,x:Float,y:Float,r:Float){val px=x+t.nx*r*1.55f;val py=y+t.ny*r*1.55f;val base=(if(t.type==ToppingType.ONION)118f else if(t.type==ToppingType.MUSHROOM)110f else 94f)*t.scale*(r/340f);bitmap(c,assets.toppings.getValue(t.type),px,py,base,base,t.rotation)}
}
