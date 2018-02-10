package cn.com.protruly.soundrecorder.recordlist;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.SeekBar;

import java.util.List;

/**
 * Created by wenwenchao on 17-8-24.
 */

public class ScaleSeekBar extends SeekBar {
    private  List<Long> scalelist;
    public ScaleSeekBar(Context context) {
        super(context);
    }

    public ScaleSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScaleSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ScaleSeekBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    public List<Long> getScalelist() {
        return scalelist;
    }

    public void setScalelist(List<Long> scalelist) {
        this.scalelist = scalelist;
    }

    void initPaint(Canvas canvas, List<Long> scalelist){
        canvas.save();
        Paint  paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.FILL);
        int max = getMax();
        int x= getThumbOffset()+getThumb().copyBounds().width()/2;
        int y = canvas.getHeight()/2;
        int l = (canvas.getWidth()/2-x)*2;
        double des = max/l;
       // int[] il = {10000,20000,30000,40000,50000,60000};
        try {
            for (long i : scalelist) {
                //canvas.drawLine(x+i*des,y,x+i*des,y*2,paint);
                int pointX = (int) (i / des);
                if(pointX>=l)pointX=l;
                canvas.drawPoint(x + pointX, y, paint);
            }
        }catch (Exception E){
            Log.e("wwc","ConcurrentModificationException-->"+E.getStackTrace());
        }
        canvas.restore();

    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        if(scalelist!=null && scalelist.size()>0){
            initPaint(canvas,scalelist);
        }
        super.onDraw(canvas);
    }


}
