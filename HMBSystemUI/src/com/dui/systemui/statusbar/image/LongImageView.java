package com.dui.systemui.statusbar.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chenheliang on 17-6-13.
 */

public class LongImageView extends View{

    private List<DrawData> mData = new ArrayList<>();
    private DrawData mTopDrawData;
    private DrawData mBottomDrawData;
    private int mContentHeight,mTopContentH,mBottomContentH;
    private Rect imageRect = new Rect();
    private boolean mCanshow=false;

    private final GestureDetector gestureDetector;
    private final ScrollerCompat mScroller;
    private final int mMinimumVelocity,mMaximumVelocity;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private final boolean SHOWLONG=false;
    private float mScale=0.87f;//0.87f;
    private Rect mLineRect= new Rect();
    private Paint mPaint;

    public LongImageView(Context context) {
        this(context,null);
    }

    public LongImageView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public LongImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mCanshow = false;
        mScroller = ScrollerCompat.create(getContext(), null);
        gestureDetector = new GestureDetector(context, simpleOnGestureListener);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        //mHandlerThread = new HandlerThread("scrollhandle");
        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(1);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mHandlerThread = new HandlerThread("scrollhandle");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandlerThread.quit();
    }

    public void addTopImage(Bitmap bitmap){
        if(bitmap==null){
            return;
        }
        if(SHOWLONG) {
            mTopDrawData = new DrawData();
            mTopDrawData.bitmap = fitBitmap(bitmap, mScale);
            int left = (bitmap.getWidth() - mTopDrawData.bitmap.getWidth()) / 2;
            mTopDrawData.rect = new Rect(left, 0, left + mTopDrawData.bitmap.getWidth(), mTopDrawData.bitmap.getHeight());
            mTopContentH = mTopDrawData.bitmap.getHeight();
        }else {
            setImage(bitmap);
        }
    }

    public void addBottomImage(Bitmap bitmap){
        if(bitmap==null){
            return;
        }
        if(SHOWLONG) {
            mBottomDrawData = new DrawData();
            mBottomDrawData.bitmap = fitBitmap(bitmap, mScale);
            int left = (bitmap.getWidth() - mBottomDrawData.bitmap.getWidth()) / 2;
            mBottomDrawData.rect = new Rect(left, getHeight() - mBottomDrawData.bitmap.getHeight(), left + mBottomDrawData.bitmap.getWidth(), getHeight());
            mBottomContentH = mBottomDrawData.bitmap.getHeight();
        }
    }
    public void addBottomImageAsData(Bitmap bitmap){
        setImage(bitmap);
    }
    public void setImage(final Bitmap bitmap){
        if(bitmap==null){
            return;
        }
        DrawData data = new DrawData();
        data.bitmap = fitBitmap(bitmap, mScale);
        int left = (bitmap.getWidth() - data.bitmap.getWidth()) / 2;
        data.rect = new Rect(left, mTopContentH + mContentHeight, left + data.bitmap.getWidth(), mTopContentH + mContentHeight + data.bitmap.getHeight());
        mContentHeight += data.bitmap.getHeight();
        mData.add(data);
        /*if(!bitmap.isRecycled()){
            bitmap.recycle();
        }*/
        if(mLineRect.isEmpty()){
            mLineRect.left=left-1;
            mLineRect.right=data.rect.right+1;
            mLineRect.top=0;
            mLineRect.bottom=getHeight();
        }
    }

    public Bitmap getBitmap(){
        if(mData.size()==0){
            return null;
        }
        int size = mData.size();
        int width = mData.get(0).bitmap.getWidth();
        int height=0;
        for(DrawData data:mData){
            height+=data.bitmap.getHeight();
        }
        Bitmap result = Bitmap.createBitmap(width, height, mData.get(0).bitmap.getConfig());
        Canvas canvas = new Canvas(result);
        int scrollh=0;
        for(int i=0;i<size-1;i++){
            Bitmap bitmap = mData.get(i).bitmap;
            canvas.drawBitmap(bitmap, 0, scrollh, null);
            scrollh+=bitmap.getHeight();
        }
        Bitmap lastbm = mData.get(size-1).bitmap;
        canvas.drawBitmap(lastbm, 0, scrollh, null);
        return result;
    }

    public static Bitmap fitBitmap(Bitmap target, float scale)
    {
        int width = target.getWidth();
        int height = target.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Bitmap bmp = Bitmap.createBitmap(target, 0, 0, width, height, matrix,
                true);
        return bmp;// Bitmap.createBitmap(target, 0, 0, width, height, matrix,
    }

    private int getContentHeight(){
        synchronized (this) {
            return mContentHeight;
        }
    }

    private int getScrollRangeY() {
        synchronized (this) {
            final int contentHeight = getHeight() - getPaddingBottom() - getPaddingTop() - mBottomContentH - mTopContentH;
            return getContentHeight() - contentHeight;
        }
    }

    public void clearImage(){
        for(DrawData data:mData){
            if(!data.bitmap.isRecycled()){
                data.bitmap.recycle();
                data.bitmap=null;
            }
        }
        scrollTo(0,0);
        mContentHeight=0;
        mData.clear();
        mTopContentH=0;
        if(mBottomDrawData!=null&&!mBottomDrawData.bitmap.isRecycled()){
            mBottomDrawData.bitmap.recycle();
        }
        if(mTopDrawData!=null&&!mTopDrawData.bitmap.isRecycled()){
            mTopDrawData.bitmap.recycle();
        }
        mBottomDrawData=null;
        mTopDrawData=null;
        mCanshow = false;
    }

    public void setCanShow(boolean can){
        if(mCanshow!=can) {
            mCanshow = can;
            postInvalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if(mCanshow) {
            gestureDetector.onTouchEvent(event);
        }else {
            handleStopTouch(event);
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getMeasuredWidth() == 0 || getMeasuredHeight() == 0 ||mContentHeight==0||!mCanshow) {
            return;
        }
        imageRect.left=getScrollX();
        imageRect.top=getScrollY();
        imageRect.right=getMeasuredWidth()+getScrollX();
        imageRect.bottom=getMeasuredHeight()+getScrollY();
        int saveCount = canvas.save();
        for (DrawData data : mData) {
            //data.rect.offset(getScrollX(),-getScrollY());
            if (data.rect.bottom < imageRect.top || data.rect.top > imageRect.bottom) {
                continue;
            }
            canvas.drawBitmap(data.bitmap, null, data.rect, null);
        }
        if(mTopDrawData!=null){
            Rect rect = new Rect(mTopDrawData.rect);
            rect.offset(0,getScrollY());
            canvas.drawBitmap(mTopDrawData.bitmap,rect,rect,null);
        }
        if(mBottomDrawData!=null){
            mBottomDrawData.rect.top=getMeasuredHeight()-mBottomDrawData.bitmap.getHeight()+getScrollY();
            mBottomDrawData.rect.bottom=mBottomDrawData.rect.top+mBottomDrawData.bitmap.getHeight();
            canvas.drawBitmap(mBottomDrawData.bitmap,null,mBottomDrawData.rect,null);
        }
        //draw rect
        mLineRect.top=getScrollY();
        mLineRect.bottom=getMeasuredHeight()+getScrollY();
        canvas.drawRect(mLineRect,mPaint);
        canvas.restoreToCount(saveCount);
    }

    private GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (!isEnabled()) {
                return false;
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (!isEnabled()||mContentHeight==0) {
                return false;
            }
            overScrollByCompat((int) distanceY,getScrollY(),getScrollRangeY());
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (!isEnabled()) {
                return;
            }
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (!isEnabled()) {
                return false;
            }
            fling((int) -velocityY);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (!isEnabled()) {
                return false;
            }
            return true;
        }
    };

    private boolean fling(int velocityY) {
        if (Math.abs(velocityY) < mMinimumVelocity) {
            velocityY = 0;
        }
        final int scrollY = getScrollY();

        final boolean canFlingY = (scrollY > 0 || velocityY > 0) &&
                (scrollY < getScrollRangeY() || velocityY < 0);
        boolean canFling = canFlingY;
        if (canFling) {
            velocityY = Math.max(-mMaximumVelocity, Math.min(velocityY, mMaximumVelocity));
            int height = getHeight() - getPaddingBottom() - getPaddingTop()-mBottomContentH-mTopContentH;
            int width = getWidth() - getPaddingRight() - getPaddingLeft();
            int bottom = getContentHeight();
            int right = getMeasuredWidth();
            mScroller.fling(getScrollX(), getScrollY(), 0, velocityY, 0, Math.max(0, right - width), 0,
                    Math.max(0, bottom - height), width / 2, height / 2);
            notifyInvalidate();
            return true;
        }
        return false;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            int oldY = getScrollY();
            int y = mScroller.getCurrY();
            if (oldY != y) {
                final int rangeY = getScrollRangeY();
                overScrollByCompat(y - oldY,oldY, rangeY);
                if (!mScroller.isFinished()) {
                    notifyInvalidate();
                }
            }
        }
    }

    private void notifyInvalidate() {
        ViewCompat.postInvalidateOnAnimation(LongImageView.this);
    }

    private boolean overScrollByCompat(int deltaY, int scrollY, int scrollRangeY) {
        int oldScrollY = getScrollY();

        int newScrollY = scrollY;

        newScrollY += deltaY;

        // Clamp values if at the limits and record
        final int top = 0;
        final int bottom = scrollRangeY;

        boolean clampedY = false;
        if (newScrollY > bottom) {
            newScrollY = bottom;
            clampedY = true;
        } else if (newScrollY < top) {
            newScrollY = top;
            clampedY = true;
        }
        if (newScrollY < 0) {
            newScrollY = 0;
        }
        onOverScrolled(0, newScrollY, false, clampedY);
        return getScrollY() - oldScrollY == deltaY;
    }

    protected void onOverScrolled(int scrollX, int scrollY,
                                  boolean clampedX, boolean clampedY) {
        super.scrollTo(scrollX, scrollY);
    }

    public void startScroll(){

        mHandler.postDelayed(mScrollRunnable,100);
    }

    private Runnable mScrollRunnable = new Runnable() {
        @Override
        public void run() {
            //Log.d("chenhl","mScrollRunnable:"+getScrollY());
            if(getScrollY()<getScrollRangeY()){
                scrollBy(0,2);
                mHandler.postDelayed(this,10);
            }else {
                //long shot over
            }
        }
    };

    public class DrawData{
        Bitmap bitmap;
        Rect rect;
    }

    private void handleStopTouch(MotionEvent event){

        if(event.getAction()==MotionEvent.ACTION_DOWN){
            if(monStopTouchListener!=null){
                monStopTouchListener.onStopTouch();
            }
        }
    }
    public interface onStopTouchListener{
        void onStopTouch();
    }
    private onStopTouchListener monStopTouchListener;

    public void setonStopTouchListener(onStopTouchListener listener){
        monStopTouchListener = listener;
    }
}
