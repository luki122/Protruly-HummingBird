package cn.com.protruly.soundrecorder.lockscreen;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import cn.com.protruly.soundrecorder.R;
import cn.com.protruly.soundrecorder.Recorder;
import cn.com.protruly.soundrecorder.util.GlobalConstant;


/**
 * Created by wenwenchao on 17-9-1.
 */

public class RecordWaveView extends View {

    private List<Recorder.FrameInfo> dataList;
    private Paint  mPaint;
    private Paint  mMarkPaint;
    private Bitmap pointBmp;
    private boolean isStartDraw = false;
    public RecordWaveView(Context context) {
        super(context);
        init(context);
    }

    public RecordWaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RecordWaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public RecordWaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context){
        dataList = new ArrayList<>();
        mPaint = new Paint();
        mPaint.setColor(Color.GRAY);
        mPaint.setStrokeWidth(GlobalConstant.WaveHorRate-1);
        mMarkPaint = new Paint();
        mMarkPaint.setColor(Color.RED);
        mMarkPaint.setStrokeWidth(20);
       // pointBmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_mark_draw);
    }

    public void setDataList(List<Recorder.FrameInfo> dataList) {
        this.dataList = dataList;
        isStartDraw = true;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(isStartDraw) {
            drawWave(canvas);
        }
        super.onDraw(canvas);

    }

    private void drawWave(Canvas canvas){
        int start = canvas.getWidth() - dataList.size() * GlobalConstant.WaveHorRate;
        int baseLine = 0;
        if (canvas != null) {
            baseLine = canvas.getHeight() / 2;
            canvas.drawColor(Color.TRANSPARENT);
            canvas.drawLine(0, baseLine, canvas.getWidth(), baseLine, mPaint);
            for (int i = 0; i < dataList.size(); i++) {
                Recorder.FrameInfo info = dataList.get(i);
                if (canvas != null && info != null) {
                    canvas.drawLine(start + i * GlobalConstant.WaveHorRate, baseLine - info.amplitude, start + i * GlobalConstant.WaveHorRate, baseLine + info.amplitude, mPaint);
                    if(info.isMark){
                       // canvas.drawBitmap(pointBmp,i,baseLine,mPaint);
                        canvas.drawPoint(start + i * GlobalConstant.WaveHorRate,baseLine,mMarkPaint);
                    }
                }
            }
        }
    }
}
