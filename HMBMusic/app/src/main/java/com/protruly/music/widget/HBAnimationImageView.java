package com.protruly.music.widget;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by hujianwei on 17-8-31.
 */

public class HBAnimationImageView extends ImageView {
    private float mCurrentDegree = 0f;
    private boolean bAnimation = false;
    private boolean bDefault = true;
    private boolean bStop = false;
    private boolean initOnDraw = false;
    private Drawable drawable;
    private int translateDx, translateDy, translateDxW, translateDyH;

    private float ratio, px, py;
    private static final String TAG = "HBAnimationImageView";

    public HBAnimationImageView(Context context) {
        this(context, null, 0);
    }

    public HBAnimationImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HBAnimationImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        bAnimation = false;
        bDefault = true;
        mCurrentDegree = 0;
        bStop = false;

    }

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 100) {
                if (bAnimation) {
                    mCurrentDegree += 0.1f;
                } else {
                    return;
                }
                if (bDefault) {
                    mCurrentDegree += 0.2f;
                }
                invalidate();
            }
        }

    };

    public void initDegree() {
        mCurrentDegree = 0;
        return;
    }

    public synchronized void setStartAnimation(boolean animation) {
        this.bAnimation = animation;
//		LogUtil.d(TAG, "setStartAnimation-bDefault:" + bDefault + " animation:" + animation);
        initOnDraw = false;
        invalidate();
    }

    public void stopAnimation() {
        this.bStop = true;
        initOnDraw = false;
        return;
    }

    public void startAnimDefaultBitmap(boolean bdefault) {
        if(this.bDefault!=bdefault){
            initOnDraw = false;
        }
        this.bDefault = bdefault;
//		LogUtil.d(TAG, "startAnimDefaultBitmap-bDefault:" + bDefault);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		/*if(rect==null){
			rect= new Rect();
			getLocalVisibleRect(rect);
		}*/
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        if (bStop) {
            return;
        }
        if (!initOnDraw) {
            drawable = getDrawable();
            if (drawable == null)
                return;
            Rect bounds = drawable.getBounds();
            int w = bounds.right - bounds.left;
            int h = bounds.bottom - bounds.top;

            if (w == 0 || h == 0) {
                return;
            }
            int left = getPaddingLeft();
            int top = getPaddingTop();
            int right = getPaddingRight();
            int bottom = getPaddingBottom();
            int width = getWidth() - left - right;
            int height = getHeight() - top - bottom;
            ratio = Math.min((float) width / w, (float) height / h);
            translateDx = left + width / 2;
            translateDy = top + height / 2;
            translateDxW = -w / 2;
            translateDyH = -h / 2;
            px = width / 2.0f;
            py = height / 2.0f;
            initOnDraw = true;
        }
		/*if(rect!=null){
			canvas.clipRect(rect);
		}*/
        if (mCurrentDegree > 360f) {
            mCurrentDegree = 0f;
        }
        canvas.scale(ratio, ratio, px, py);
        canvas.translate(translateDx, translateDy);
        canvas.rotate(mCurrentDegree);
        canvas.translate(translateDxW, translateDyH);
        drawable.draw(canvas);
        handler.removeMessages(100);
        if (!bDefault) {
            handler.sendEmptyMessageDelayed(100, 30);
        } else {
            handler.sendEmptyMessageDelayed(100, 60);
        }
    }

}
