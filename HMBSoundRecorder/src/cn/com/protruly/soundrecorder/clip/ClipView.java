package cn.com.protruly.soundrecorder.clip;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.media.MediaCodec;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import cn.com.protruly.soundrecorder.R;
import cn.com.protruly.soundrecorder.util.LogUtil;

/**
 * Created by sqf on 17-8-14.
 */

public class ClipView extends View implements WaveFormProcessListener, Animation.AnimationListener{

    private static final String TAG = "ClipView";
    private static final String TAG_1 = "TAGTAG";

    private int mAmplitudeBarColor;
    private int mLeftClipBarColor;
    private int mRightClipBarColor;
    private int mBlackMaskColor;
    private int mPlayPositionBarColor;
    private int mCentralHorizontalLineColor;
    private int mUpperLowerLineColor;
    private int mTriangleIndicatorColor;

    private float mCentralHorizontalLineHeight;

    private float mAmplitudeWidth;
    private float mAmplitudeGapWidth;

    private float mTriangleIndicatorHeight;
    private float mTriangleIndicatorDrawMargin;

    private float mDrawPadding; //left and right are the same
    private float mHoldStillThreshold;
    private float mLeftClipPositionMoveArea;
    private float mRightClipPositionMoveArea;

    private int mWidth;
    private int mHeight;
    private float mWaveFormDrawHeight; //mHeight - mTriangleIndicatorHeight - mTriangleIndicatorDrawMargin

    private Paint mPaint;
    private Path mCentralHorizontalLinePath = new Path();
    private DashPathEffect mDashPathEffect = new DashPathEffect(new float[] {15, 5}, 0);

    private Mp3FileInfo mMp3FileInfo;

    private static final int MODE_NORMAL = 0;
    private static final int MODE_TOUCH_ZOOM_IN = 1;
    private int mMode = MODE_NORMAL;

    public static final int NORMAL_MODE_DRAW_STEP = 2;

    private ArrayList<WaveFormData> mWaveFormDataList;

    public static final float MAX_AMPLITUDE_LIMIT = 400.0f;

    private float mLeftClipPositionInNormalMode;
    private float mRightClipPositionInNormalMode;
    private float mPlayPositionInNormalMode;

    private float mRightClipRestrictedPosition;

    private float mClipTouchTolerance;
    private boolean mProcessFinished;

    private static final int UNKNOWN = -1;

    private static final int CLIP_POSITION_MODE_LEFT = 1;
    private static final int CLIP_POSITION_MODE_RIGHT = 2;

    private int mClipPositionMode = UNKNOWN;

    private RectF mTempRect = new RectF();

    private Animation mAnimation;
    //private float mTouchSlop;
    private float mDownX;
    private float mPrevX;
    private float mPrevDeltaX;

    private ZoomManager mZoomManager;

    private static final int MSG_UPDATE_TIME_DISPLAY = 1100;

    private UpdateViewListener mUpdateViewListener;
    private UserInteractionListener mUserInteractionListener;

    boolean mTouchInPlayPosition;
    boolean mTouchInLeftClipArea;
    boolean mTouchInRightClipArea;

    boolean mFirstMeasure = true;

    public class TimeInfo {
        public long leftClipTime;   // left clip position
        public long rightClipTime;     //right clip position
        public long playPositionTime;//play position

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("leftClipTime:" + leftClipTime);
            sb.append("rightClipTime:" + rightClipTime);
            sb.append("playPositionTime:" + playPositionTime);
            return super.toString();
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            if(what == WaveFormProcessListener.MSG_REFRESH_UI) {
                boolean finished = msg.arg1 == 1;
                refresh((ArrayList<WaveFormData>)msg.obj, finished);
            } else if(what == MSG_UPDATE_TIME_DISPLAY) {
                TimeInfo timeInfo = (TimeInfo) msg.obj;
                mUpdateViewListener.updateTimeInfo(timeInfo);
                //LogUtil.i("CCC", "MSG_UPDATE_TIME_DISPLAY --> handleMessage: timeInfo-->" + timeInfo);
            }
        }
    };

    public ClipView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setUpdateViewListener (UpdateViewListener listener) {
        mUpdateViewListener = listener;
    }

    public void setUserInteractionListener(UserInteractionListener listener) {
        mUserInteractionListener = listener;
    }

    private void init() {
        //LogUtil.i(TAG, "init....");//
        Resources res = getContext().getResources();
        mAmplitudeBarColor = res.getColor(R.color.clip_amplitude_bar_color);
        mLeftClipBarColor = res.getColor(R.color.clip_left_bar_color);
        mRightClipBarColor = res.getColor(R.color.clip_right_bar_color);
        mBlackMaskColor = res.getColor(R.color.clip_black_mask);
        mPlayPositionBarColor = res.getColor(R.color.play_position_bar_color);
        mCentralHorizontalLineColor = res.getColor(R.color.clip_upper_lower_line_color);
        mUpperLowerLineColor = res.getColor(R.color.clip_upper_lower_line_color);
        mTriangleIndicatorColor = res.getColor(R.color.clip_triangle_indicator_color);

        mCentralHorizontalLineHeight = res.getDimension(R.dimen.clip_central_horizontal_line_height);
        mAmplitudeWidth = res.getDimension(R.dimen.clip_amplitude_width);
        mAmplitudeGapWidth = res.getDimension(R.dimen.clip_amplitude_gap_width);
        mTriangleIndicatorHeight = res.getDimension(R.dimen.clip_triangle_indicator_height);
        mTriangleIndicatorDrawMargin = res.getDimension(R.dimen.clip_triangle_indicator_top_margin);

        mDrawPadding = res.getDimension(R.dimen.clip_wave_form_draw_padding);
        mClipTouchTolerance = res.getDimension(R.dimen.clip_touch_tolerance);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        //mTouchSlop = ViewConfiguration.get(this.getContext()).getScaledTouchSlop();
        mHoldStillThreshold = res.getDimension(R.dimen.clip_hold_still_threshold);
    }

    public Handler getHandler() {
        return mHandler;
    }

    public static int calculateApproximateSampleTime(Context context) {
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        int width = dm.widthPixels;
        float amplitudeWidth = res.getDimension(R.dimen.clip_amplitude_width);
        float amplitudeGapWidth = res.getDimension(R.dimen.clip_amplitude_gap_width);
        float drawPadding = res.getDimension(R.dimen.clip_wave_form_draw_padding);
        // mApproximateSampleTime is calculated according to equation below:
        // width = 2 * drawPadding + mApproximateSampleTime * amplitudeWidth + (mApproximateSampleTime - 1) * amplitudeGapWidth
        return (int)(((width - 2 * drawPadding) + amplitudeGapWidth ) / (amplitudeGapWidth + amplitudeWidth)) * ClipView.NORMAL_MODE_DRAW_STEP;
    }

    public void setMediaInfos(Mp3FileInfo info) {
        mMp3FileInfo = info;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //LogUtil.i("TTT", "onAttachedToWindow --- ");
        mFirstMeasure = true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //LogUtil.i("TTT", "onMeasure --- ");
        if(mFirstMeasure) {
            mWidth = getMeasuredWidth();
            mHeight = getMeasuredHeight();

            LogUtil.i("TTT", "onMeasure --- mWidth:" + mWidth + " mHeight:" + mHeight);

            mLeftClipPositionInNormalMode = mDrawPadding;
            mRightClipPositionInNormalMode = mWidth - mDrawPadding;
            mPlayPositionInNormalMode = mLeftClipPositionInNormalMode;

            //LogUtil.i("Left", "onMeasure -----mRightClipPositionInNormalMode: " + mRightClipPositionInNormalMode);
            mLeftClipPositionMoveArea = 2 * mDrawPadding;
            mRightClipPositionMoveArea = mWidth - 2 * mDrawPadding;
            mWaveFormDrawHeight = mHeight - 2 * (mTriangleIndicatorHeight + mTriangleIndicatorDrawMargin);

            mFirstMeasure = false;
        }
    }

    /**
     * @param index index in normal mode, something like 0, 1, 2, 3, 4, .... will be divided by NORMAL_MODE_DRAW_STEP
     *              and converted to 0, 4, 8, 12, ...... (times of NORMAL_MODE_DRAW_STEP)
     * @return
     */
    private RectF getRectInNormalMode(int index, RectF outRect) {
        int dataIndex = index - (index % NORMAL_MODE_DRAW_STEP);
        WaveFormData waveFormData = mWaveFormDataList.get(dataIndex);
        float amplitude = waveFormData.getAmplitude();
        float left = mDrawPadding + (index / NORMAL_MODE_DRAW_STEP) * (mAmplitudeWidth + mAmplitudeGapWidth);
        float drawHeight = Math.min(amplitude * mWaveFormDrawHeight / MAX_AMPLITUDE_LIMIT, mWaveFormDrawHeight);
        float top = (mWaveFormDrawHeight - drawHeight) / 2.0f + mTriangleIndicatorHeight + mTriangleIndicatorDrawMargin;
        float right = left + mAmplitudeWidth;
        float bottom = top + drawHeight;
        outRect.set(left, top, right, bottom);
        return outRect;
    }

    /**
     * index in normal mode, something like 0, 4, 8, 12, 16, ....
     * next index = (previous index + NORMAL_MODE_DRAW_STEP);
     * @param strideIndex
     * @param outRect
     * @return
     */
    private RectF getRectInNormalModeForEveyStride(int strideIndex, RectF outRect) {
        WaveFormData waveFormData = mWaveFormDataList.get(strideIndex);
        float amplitude = waveFormData.getAmplitude();
        float left = mDrawPadding + (strideIndex / NORMAL_MODE_DRAW_STEP) * (mAmplitudeWidth + mAmplitudeGapWidth);
        float drawHeight = Math.min(amplitude * mWaveFormDrawHeight / MAX_AMPLITUDE_LIMIT, mWaveFormDrawHeight);
        //LogUtil.i(TAG, "ddddd amplitude: " + amplitude + " mWaveFormDrawHeight:" + mWaveFormDrawHeight + " MAX_AMPLITUDE_LIMIT:" + MAX_AMPLITUDE_LIMIT);
        float top = (mWaveFormDrawHeight - drawHeight) / 2.0f + mTriangleIndicatorHeight + mTriangleIndicatorDrawMargin;
        float right = left + mAmplitudeWidth;
        float bottom = top + drawHeight;
        outRect.set(left, top, right, bottom);
        return outRect;
    }

    private void updateTimeDisplay() {
        int start = getSampleIndexAtPositionInNormalMode(mLeftClipPositionInNormalMode, CLIP_POSITION_MODE_LEFT);
        int end = getSampleIndexAtPositionInNormalMode(mRightClipPositionInNormalMode, CLIP_POSITION_MODE_RIGHT);
        int current = getSampleIndexAtPositionInNormalMode(mPlayPositionInNormalMode, CLIP_POSITION_MODE_LEFT);
        WaveFormData startData = mWaveFormDataList.get(start);
        WaveFormData endData = mWaveFormDataList.get(end);
        WaveFormData currentData = mWaveFormDataList.get(current);

        //LogUtil.i("CCC", "updateTimeDisplay start:" + start + " end:" + end + " current:" + current);

        TimeInfo timeInfo = new TimeInfo();
        timeInfo.leftClipTime = startData.getPresentationTime();
        timeInfo.rightClipTime = endData.getPresentationTime();
        timeInfo.playPositionTime = currentData.getPresentationTime();

        mHandler.removeMessages(MSG_UPDATE_TIME_DISPLAY);
        mHandler.obtainMessage(MSG_UPDATE_TIME_DISPLAY, timeInfo).sendToTarget();
    }


    public void resetPlayPositionToLeftClip() {
        mPlayPositionInNormalMode = mLeftClipPositionInNormalMode;
        //LogUtil.i("CCC", "resetPlayPositionToLeftClip !!! mPlayPositionInNormalMode reset to mLeftClipPositionInNormalMode:" + mLeftClipPositionInNormalMode);
        updateTimeDisplay();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //setBackgroundColor(Color.CYAN);
        if(mWaveFormDataList == null || mWaveFormDataList.isEmpty()) return;

        //draw central horizontal line
        mPaint.setStyle(Paint.Style.STROKE);//dash line needs this
        mPaint.setStrokeWidth(mCentralHorizontalLineHeight);
        mPaint.setColor(mCentralHorizontalLineColor);
        mPaint.setPathEffect(mDashPathEffect);

        float startX = 0;
        float startY = mWaveFormDrawHeight / 2 + mTriangleIndicatorHeight + mTriangleIndicatorDrawMargin;;
        float stopX = mWidth;
        float stopY = startY;
        mCentralHorizontalLinePath.reset();
        mCentralHorizontalLinePath.moveTo(startX, startY);
        mCentralHorizontalLinePath.lineTo(stopX, stopY);
        //canvas.drawLine(startX, startY, stopX, stopY, mPaint);
        canvas.drawPath(mCentralHorizontalLinePath, mPaint);

        mPaint.setStyle(Paint.Style.FILL);//FILL_AND_STROKE
        mPaint.setColor(mCentralHorizontalLineColor);
        //draw upper line
        startX = 0;
        startY = mTriangleIndicatorHeight;
        stopX = mWidth;
        stopY = startY;
        canvas.drawLine(startX, startY, stopX, stopY, mPaint);

        //draw lower line
        startX = 0;
        startY = mHeight - mTriangleIndicatorHeight;
        stopX = mWidth;
        stopY = startY;
        canvas.drawLine(startX, startY, stopX, stopY, mPaint);

        //draw wave form

        mPaint.setStrokeWidth(0);
        mPaint.setColor(mAmplitudeBarColor);
        mPaint.setPathEffect(null);
        if(mAnimation != null && mAnimation.isStarted()) {
            boolean more = mAnimation.draw(canvas);
            if(more) {
                invalidate();
            }
            return;
        }
        if(mMode == MODE_NORMAL) {
            int waveDataNum = mWaveFormDataList.size();
            RectF rectF = new RectF();
            for (int i = 0; i < waveDataNum; i += NORMAL_MODE_DRAW_STEP) {
                rectF = getRectInNormalModeForEveyStride(i, rectF);
                canvas.drawRect(rectF.left, rectF.top, rectF.right, rectF.bottom, mPaint);
            }
        }
        if(!mProcessFinished) return;
        if(mMode == MODE_NORMAL) {
            // play position
            drawPlayPosition(canvas, mPlayPositionInNormalMode);
            // left clip position
            drawClipPosition(canvas, mLeftClipPositionInNormalMode);
            // right clip position
            drawClipPosition(canvas, mRightClipPositionInNormalMode);
            drawLeftBlackMask(canvas, mLeftClipPositionInNormalMode);
            drawRightBlackMask(canvas, mRightClipPositionInNormalMode);
        } else if(mMode == MODE_TOUCH_ZOOM_IN) {
            if(mZoomManager != null) {
                // draw wave form
                int waveDataNum = mWaveFormDataList.size();
                RectF tmpRect = new RectF();
                for (int i = 0; i < waveDataNum; i++) {
                    RectF rectF = mZoomManager.getRectInZoomInMode(i, tmpRect);
                    if(rectF.left > 0 && rectF.right < mWidth) {
                        canvas.drawRect(rectF.left, rectF.top, rectF.right, rectF.bottom, mPaint);
                    }
                    if(rectF.right > mWidth) break;
                }
                // draw play position
                drawPlayPosition(canvas, mZoomManager.getPlayPositionInZoomInMode() - mZoomManager.getOffset());
                //LogUtil.i("SSS", " mZoomManager.getPlayPositionInZoomInMode(): " +  mZoomManager.getPlayPositionInZoomInMode()  + " mZoomManager.getOffset():" + mZoomManager.getOffset());

                // left clip position
                drawClipPosition(canvas, mZoomManager.getLeftClipPositionInZoomInMode() - mZoomManager.getOffset());
                //LogUtil.i("SSS", " mZoomManager.getLeftClipPositionInZoomInMode(): " +  mZoomManager.getLeftClipPositionInZoomInMode()  + " mZoomManager.getOffset():" + mZoomManager.getOffset());

                // right clip position
                drawClipPosition(canvas, mZoomManager.getRightClipPositionInZoomInMode() - mZoomManager.getOffset());
                //LogUtil.i("SSS", " mZoomManager.getRightClipPositionInZoomInMode(): " +  mZoomManager.getRightClipPositionInZoomInMode()  + " mZoomManager.getOffset():" + mZoomManager.getOffset());

                // draw left black mask
                drawLeftBlackMask(canvas, mZoomManager.getLeftClipPositionInZoomInMode() - mZoomManager.getOffset());

                // draw right black mask
                drawRightBlackMask(canvas, mZoomManager.getRightClipPositionInZoomInMode() - mZoomManager.getOffset());
            }
        }
    }

    private void drawLeftBlackMask(Canvas canvas, float position) {
        if(position <= 0.0f) return;
        mPaint.setColor(mBlackMaskColor);
        float left = 0.0f;
        float top = mTriangleIndicatorHeight;
        float right = position;
        float bottom = mHeight - mTriangleIndicatorHeight;
        canvas.drawRect(left, top, right, bottom, mPaint);
    }

    private void drawRightBlackMask(Canvas canvas, float position) {
        if(position >= mWidth) return;
        mPaint.setColor(mBlackMaskColor);
        float left = position;
        float top = mTriangleIndicatorHeight;
        float right = mWidth;
        float bottom = mHeight - mTriangleIndicatorHeight;
        canvas.drawRect(left, top, right, bottom, mPaint);
    }

    /**
     * Used to draw left or right clip position,
     */
    private void drawClipPosition(Canvas canvas, float clipPosition) {
        mPaint.setColor(mLeftClipBarColor);
        canvas.drawLine(clipPosition, mTriangleIndicatorHeight, clipPosition,
                mHeight - mTriangleIndicatorHeight, mPaint);

        mPaint.setColor(mTriangleIndicatorColor);
        float halfTriangleWidth = (float) (mTriangleIndicatorHeight * Math.tan(Math.PI / 6.0f));
        Path path = new Path();
        path.moveTo(clipPosition, mWaveFormDrawHeight + mTriangleIndicatorDrawMargin + mTriangleIndicatorHeight + mTriangleIndicatorDrawMargin);
        path.lineTo(clipPosition - halfTriangleWidth, mHeight);
        path.lineTo(clipPosition + halfTriangleWidth, mHeight);
        path.lineTo(clipPosition, mWaveFormDrawHeight + mTriangleIndicatorDrawMargin + mTriangleIndicatorHeight + mTriangleIndicatorDrawMargin);
        canvas.drawPath(path, mPaint);
        path.close();
    }

    private void drawPlayPosition(Canvas canvas, float playPosition) {

        mPaint.setColor(mPlayPositionBarColor);
        canvas.drawLine(playPosition, 0, playPosition, mHeight, mPaint);

        // lower triangle
        float halfTriangleWidth = (float) (mTriangleIndicatorHeight * Math.tan(Math.PI / 6.0f));
        Path path = new Path();
        path.moveTo(playPosition, mWaveFormDrawHeight + mTriangleIndicatorDrawMargin + mTriangleIndicatorHeight + mTriangleIndicatorDrawMargin);
        path.lineTo(playPosition - halfTriangleWidth, mHeight);
        path.lineTo(playPosition + halfTriangleWidth, mHeight);
        path.lineTo(playPosition, mWaveFormDrawHeight + mTriangleIndicatorDrawMargin + mTriangleIndicatorHeight + mTriangleIndicatorDrawMargin);
        canvas.drawPath(path, mPaint);

        // upper triangle
        path.moveTo(playPosition - halfTriangleWidth, 0);
        path.lineTo(playPosition + halfTriangleWidth, 0);
        path.lineTo(playPosition, mTriangleIndicatorHeight);
        path.lineTo(playPosition - halfTriangleWidth, 0);
        canvas.drawPath(path, mPaint);
        path.close();
    }

    @Override
    public void refresh(ArrayList<WaveFormData> data, boolean finished) {
        if(data == null || data.isEmpty()) return;
        mWaveFormDataList = data;
        //LogUtil.i(TAG, "refresh --> " + mWaveFormDataList.size() + " finished:" + finished);
        mProcessFinished = finished;

        if(finished) {
            calculateRightClipRestrictedPositionInNormalMode();
            updateTimeDisplay();
            mUpdateViewListener.notifyProcessFinished();
        }
        invalidate();
    }

    /**
     * mRightClipRestrictedPosition: do not include right mDrawPadding here.
     */
    private void calculateRightClipRestrictedPositionInNormalMode() {
        int len = mWaveFormDataList.size() / NORMAL_MODE_DRAW_STEP;
        float lastWaveFormLeft = mDrawPadding + (len - 1) * (mAmplitudeWidth + mAmplitudeGapWidth);
        float lastWaveFormRight = lastWaveFormLeft + mAmplitudeWidth;
        float drawRestrictedPosition = mWidth - mDrawPadding;
        if(lastWaveFormRight < drawRestrictedPosition) {
            mRightClipRestrictedPosition = lastWaveFormRight;
            //mRightClipRestricted = true;
        } else {
            mRightClipRestrictedPosition = drawRestrictedPosition;
            //mRightClipRestricted = false;
        }
        // set right clip position to restricted position
        mRightClipPositionInNormalMode = mRightClipRestrictedPosition;
        //LogUtil.i("Left", "calculateRightClipRestrictedPositionInNormalMode ----- mRightClipPositionInNormalMode:" + mRightClipPositionInNormalMode);
    }

    @Override
    public void setMaxAmplitude(float maxAmplitude) {
        //mMaxAmplitude = maxAmplitude;
        //LogUtil.i(TAG, " set max amplitude:" + maxAmplitude);
    }

    @Override
    public void onMediaInfoExtracted(Mp3FileInfo mp3FileInfo) {
        mMp3FileInfo = mp3FileInfo;
    }

    public long getDuration() {
        if(mMp3FileInfo == null) return 0;
        return mMp3FileInfo.getDuration();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(! mProcessFinished) return false;
        float x = event.getX();
        int action = event.getAction();

        if(action == MotionEvent.ACTION_DOWN) {
            mUserInteractionListener.onUserInteractionTouchDown();
            mDownX = x;
            mPrevX = mDownX;
            //LogUtil.i("TTTTTT","onTouchDown..... m");
            if (mMode == MODE_NORMAL) {
                //LogUtil.i("TTTTTT","onTouchDown.....MODE_NORMAL");
                int sampleIndex = 0;
                //float position = 0.0f;
                mTouchInLeftClipArea = touchInLeftClipArea(x);
                mTouchInRightClipArea = touchInRightClipArea(x);
                mTouchInPlayPosition = touchInPlayPositionArea(x);
                /*
                LogUtil.i("PlayPosition", "mTouchInPlayPosition:" + mTouchInPlayPosition +
                        " touchInLeftClipArea:" + mTouchInLeftClipArea +
                        " touchInRightClipArea:" + mTouchInRightClipArea);*/
                // if left clip position is too close with right clip position
                if(mTouchInLeftClipArea && mTouchInRightClipArea) {
                    float distanceLeft = Math.abs(mLeftClipPositionInNormalMode - x);
                    float distanceRight = Math.abs(mRightClipPositionInNormalMode - x);
                    if(distanceLeft <= distanceRight) {
                        mTouchInLeftClipArea = true;
                        mTouchInRightClipArea = false;
                    } else {
                        mTouchInLeftClipArea = false;
                        mTouchInRightClipArea = true;
                    }
                }
                //LogUtil.i("TTTTTT", "touchInLeftClipArea:" + mTouchInLeftClipArea + " touchInRightClipArea:" + mTouchInRightClipArea);
                if (mTouchInLeftClipArea && mAnimation == null) {
                    //LogUtil.i("TTTTTT", "touch in left clip");
                    mClipPositionMode = CLIP_POSITION_MODE_LEFT;
                    sampleIndex = getSampleIndexAtPositionInNormalMode(mLeftClipPositionInNormalMode, mClipPositionMode);
                    //LogUtil.i("TTTTTT", "CLIP_POSITION_MODE_LEFT sampleIndex: " + sampleIndex);
                    startZoomIn(sampleIndex, mClipPositionMode);
                } else if (mTouchInRightClipArea && mAnimation == null) {
                    //LogUtil.i("TTTTTT", "touch in right clip");
                    mClipPositionMode = CLIP_POSITION_MODE_RIGHT;
                    sampleIndex = getSampleIndexAtPositionInNormalMode(mRightClipPositionInNormalMode, mClipPositionMode);
                    //LogUtil.i("TTTTTT", "CLIP_POSITION_MODE_RIGHT sampleIndex: " + sampleIndex);
                    startZoomIn(sampleIndex, mClipPositionMode);
                }
                // we will zoom to left and right at sample index
                return true;
            } else if (mMode == MODE_TOUCH_ZOOM_IN) {
                return false;
            }
        } else if(action == MotionEvent.ACTION_MOVE) {
            float deltaX = x - mPrevX;
            /*
            LogUtil.i("PlayPosition", "ACTION_MOVE ------ mTouchInPlayPosition:" + mTouchInPlayPosition +
                    " mMode == MODE_NORMAL:" + (mMode == MODE_NORMAL)); */
            if(! mTouchInLeftClipArea && ! mTouchInRightClipArea && mTouchInPlayPosition && mMode == MODE_NORMAL) {
                movePlayPosition(deltaX);
                invalidate();
                updateTimeDisplay();
                mPrevX = x;
                return true;
            }
            if(mZoomManager != null) {
                mZoomManager.moveClipPosition(deltaX);
            }
            mPrevX = x;
        } else if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if(mZoomManager != null) {
                startZoomOut();
            }
            mTouchInPlayPosition = false;
            mTouchInLeftClipArea = false;
            mTouchInRightClipArea = false;
            return true;
        }
        return super.onTouchEvent(event);
    }

    private boolean touchInLeftClipArea(float touchX) {
        //LogUtil.i(TAG, "mLeftClipPositionInNormalMode:" + mLeftClipPositionInNormalMode + " touchX:" + touchX + " mClipTouchTolerance:" + mClipTouchTolerance);
        return touchX < mLeftClipPositionInNormalMode + mClipTouchTolerance &&
                touchX > mLeftClipPositionInNormalMode - mClipTouchTolerance;
    }

    private boolean touchInRightClipArea(float touchX) {
        return touchX < mRightClipPositionInNormalMode + mClipTouchTolerance &&
                touchX > mRightClipPositionInNormalMode - mClipTouchTolerance;
    }

    /**
     * This function MUST only be used in normal mode.
     * @param position
     * @param clipPositionMode
     * @return
     */
    private int getSampleIndexAtPositionInNormalMode(float position, int clipPositionMode) {
        //binary search the wave data list
        int left = 0;
        int right = (mWaveFormDataList.size() - 1) / NORMAL_MODE_DRAW_STEP;// divided by 4
        while (left <= right) {
            int mid = (left + right) / 2;
            float posLeft = mDrawPadding + mid * (mAmplitudeWidth + mAmplitudeGapWidth);
            float posRight = posLeft + (mAmplitudeWidth + mAmplitudeGapWidth);
            if(position <= posRight && position >= posLeft) {
                return mid * NORMAL_MODE_DRAW_STEP;
            } else if (position > posRight) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        if(clipPositionMode == CLIP_POSITION_MODE_LEFT) {
            return 0;
        }
        return mWaveFormDataList.size() - 1;
    }

    private int getSampleIndexByPresentationTime(long presentationTime) {
        //binary search the wave data list
        int len = mWaveFormDataList.size();
        int left = 0;
        int right = len - 1;
        while (left <= right) {
            int mid = (left + right) / 2;
            long time = mWaveFormDataList.get(mid).getPresentationTime();
            //long prevTime = 0;
            //long nextTime = mWaveFormDataList.get(len - 1).getPresentationTime();
            int prevIndex = mid - 1 >= 0 ? (mid - 1) : 0;
            int nextIndex = mid + 1 <= (len - 1) ? (mid + 1) : (len - 1);
            long prevTime = mWaveFormDataList.get(prevIndex).getPresentationTime();
            long nextTime = mWaveFormDataList.get(nextIndex).getPresentationTime();
            if(presentationTime < prevTime) {
                right = mid - 1;
            } else if(presentationTime >= prevTime && presentationTime < time) {
                return prevIndex;
            } else if(presentationTime >= time && presentationTime < nextTime) {
                return mid;
            } else if(presentationTime == nextTime) {
                return nextIndex;
            } else {
                left = mid + 1;
            }
        }
        return -1;
    }

    private float getPlayPositionByPresentationTimeInNormalMode(long presentationTime) {
        int index = getSampleIndexByPresentationTime(presentationTime);
        //LogUtil.i(TAG, "getPlayPositionByPresentationTimeInNormalMode presentationTime: " + presentationTime);
        if(index == -1) {
            //LogUtil.e(TAG, "getPlayPositionByPresentationTimeInNormalMode ---> return -1 ERROR occurs");
            return 0;
        }
        //int drawIndexInNormalMode = (index % NORMAL_MODE_DRAW_STEP > 0) ? (index / NORMAL_MODE_DRAW_STEP) : (index / NORMAL_MODE_DRAW_STEP + 1);
        int drawIndexInNormalMode = index / NORMAL_MODE_DRAW_STEP;
        //LogUtil.i(TAG, "getPlayPositionByPresentationTimeInNormalMode drawIndexInNormalMode: " + drawIndexInNormalMode);
        float playPosition = mDrawPadding + (drawIndexInNormalMode + 1) * (mAmplitudeWidth + mAmplitudeGapWidth);
        return playPosition;
    }

    /**
     * @param touchDownSampleIndex index in whole wave form data list
     */
    private void startZoomIn(int touchDownSampleIndex, int clipPositionMode) {
        mZoomManager = new ZoomManager(touchDownSampleIndex, clipPositionMode);
        mAnimation = mZoomManager.createZoomInAnimation();
        invalidate();
    }

    private void startZoomOut() {
        if(mZoomManager == null) return;
        boolean isZoomingIn = mAnimation != null && mAnimation.isActive() &&
                mAnimation.getType() == Animation.ANIM_TYPE_ZOOM_IN;
        if(mMode == MODE_TOUCH_ZOOM_IN || isZoomingIn) {
            mAnimation = mZoomManager.createZoomOutAnimation();
            invalidate();
        }
    }

    private float getAmplitudeDrawHeight(float amplitude) {
        return Math.min(amplitude * mWaveFormDrawHeight / MAX_AMPLITUDE_LIMIT, mWaveFormDrawHeight);
    }

    @Override
    public void onAnimationStart(int animType) {

    }

    @Override
    public void onAnimationEnd(int animType) {
        if(animType == Animation.ANIM_TYPE_ZOOM_IN) {
            //LogUtil.i("TTTTTT", "onAnimationEnd ANIM_TYPE_ZOOM_IN ");
            mMode = MODE_TOUCH_ZOOM_IN;
            if(mAnimation != null) {
                mAnimation.stop();
                mAnimation = null;
            }
        } else if(animType == Animation.ANIM_TYPE_ZOOM_OUT) {
            //LogUtil.i("TTTTTT", "onAnimationEnd ANIM_TYPE_ZOOM_OUT ");
            mMode = MODE_NORMAL;
            if(mAnimation != null) {
                mAnimation.stop();
                mAnimation = null;
            }
        }
    }

    /**
     * This function works only in normal mode
     */
    private void movePlayPosition(float deltaX) {
        mPlayPositionInNormalMode += deltaX;
        //LogUtil.i("PlayPosition", "movePlayPosition ...... mPlayPositionInNormalMode:" + mPlayPositionInNormalMode);
        mPlayPositionInNormalMode = clamp(mPlayPositionInNormalMode, mLeftClipPositionInNormalMode, mRightClipPositionInNormalMode);
        //LogUtil.i("PlayPosition", "movePlayPosition ...... after clamp mPlayPositionInNormalMode:" + mPlayPositionInNormalMode);
    }

    private boolean touchInPlayPositionArea(float touchX) {
        return touchX < mPlayPositionInNormalMode + mClipTouchTolerance &&
                touchX > mPlayPositionInNormalMode - mClipTouchTolerance;
    }


    public boolean playPositionReachesRightClipPosition() {
        boolean reachesRight = mPlayPositionInNormalMode >= mRightClipPositionInNormalMode;
        /*
        LogUtil.i("CCC", "playPositionReachesRightClipPosition mPlayPositionInNormalMode:" + mPlayPositionInNormalMode +
                " mRightClipPositionInNormalMode:" + mRightClipPositionInNormalMode +
                " ends?: " + reachesRight);*/
        return reachesRight;
    }

    //
    // ---------------------------------------------------
    // |
    // |
    // |
    // all datas are in Zoom-In mode
    private class ZoomManager {
        private float mWholeLength;
        // relative to whole scroller length, it's the position in zoom in mode
        private int mTouchDownSampleIndexInNormalMode;
        private float mLeftClipPositionInZoomInMode;
        private float mRightClipPositionInZoomInMode;
        private float mPlayPositionInZoomInMode;
        private int mClipPositionMode;
        private float mOffset;

        public ZoomManager(int touchDownSampleIndexInNormalMode, int clipPositionMode) {
            mTouchDownSampleIndexInNormalMode = touchDownSampleIndexInNormalMode;
            mClipPositionMode = clipPositionMode;
            initWholeLengthInZoomInMode();
            initCurrentPositionInZoomInMode();
            //LogUtil.i(TAG, "ZoomManager --> mTouchDownSampleIndexInNormalMode:" + mTouchDownSampleIndexInNormalMode + " ");
        }

        private void initWholeLengthInZoomInMode() {
            int size = mWaveFormDataList.size();
            mWholeLength = mAmplitudeWidth * size + mAmplitudeGapWidth * (size - 1) + 2 * mDrawPadding; // add 2 mDrawPadding here.
            //LogUtil.i(TAG, "mWholeLength:" + mWholeLength);
        }

        private void initCurrentPositionInZoomInMode() {
            //these two ratio is relative to wave form without mDrawingPadding

            /*
            float leftPositionRatio = (mLeftClipPositionInNormalMode - mDrawPadding) /  (mWidth -2 * mDrawPadding);
            float rightPositionRatio = (mRightClipPositionInNormalMode - mDrawPadding) / (mWidth -2 * mDrawPadding);
            float playPositionRatio = (mPlayPositionInNormalMode - mDrawPadding) / (mWidth - 2 * mDrawPadding);
            */
            float leftPositionRatio = (mLeftClipPositionInNormalMode - mDrawPadding) / (mRightClipRestrictedPosition - mDrawPadding);
            float rightPositionRatio = (mRightClipPositionInNormalMode - mDrawPadding) / (mRightClipRestrictedPosition - mDrawPadding);
            float playPositionRatio = (mPlayPositionInNormalMode - mDrawPadding) / (mRightClipRestrictedPosition - mDrawPadding);

            mLeftClipPositionInZoomInMode = (mWholeLength - 2 * mDrawPadding) * leftPositionRatio + mDrawPadding;
            mRightClipPositionInZoomInMode = (mWholeLength - 2 * mDrawPadding) * rightPositionRatio + mDrawPadding;
            mPlayPositionInZoomInMode = (mWholeLength - 2 * mDrawPadding) * playPositionRatio + mDrawPadding;
            /*
            LogUtil.i(TAG_1, "leftPositionRatio:" + leftPositionRatio + " rightPositionRatio:" + rightPositionRatio +
             " mLeftClipPositionInZoomInMode:" + mLeftClipPositionInZoomInMode + " mRightClipPositionInZoomInMode:" + mRightClipPositionInZoomInMode);
             */
            if(mClipPositionMode == CLIP_POSITION_MODE_LEFT) {
                mOffset = mLeftClipPositionInZoomInMode - mLeftClipPositionInNormalMode;
                //LogUtil.i(TAG_1, "mOffset 1111111:" + mOffset);
            } else {
                mOffset = mRightClipPositionInZoomInMode - mRightClipPositionInNormalMode;
                //LogUtil.i(TAG_1, "mOffset 2222222:" + mOffset);
            }
        }

        public float getLeftClipPositionInZoomInMode() {
            return mLeftClipPositionInZoomInMode;
        }

        public float getRightClipPositionInZoomInMode() {
            return mRightClipPositionInZoomInMode;
        }

        public float getPlayPositionInZoomInMode() { return mPlayPositionInZoomInMode; }

        public float getOffset() { return mOffset; }

        /**
         * @param index index in zoom in mode, something like 0, 1, 2, 3 ,4, 5, ...... starts with 0
         * @return
         */
        private RectF getRectInZoomInMode(int index, RectF outRect) {
            WaveFormData waveFormData = mWaveFormDataList.get(index);
            float amplitude = waveFormData.getAmplitude();
            float left = mDrawPadding + index * (mAmplitudeWidth + mAmplitudeGapWidth) - mOffset;
            float drawHeight = getAmplitudeDrawHeight(amplitude);
            float top = (mWaveFormDrawHeight - drawHeight) / 2.0f + mTriangleIndicatorHeight + mTriangleIndicatorDrawMargin;
            float right = left + mAmplitudeWidth;
            float bottom = top + drawHeight;
            outRect.set(left, top, right, bottom);
            //LogUtil.i(TAG_1, "getRectInZoomInMode $$$$ mTempRect:" + outRect);
            return outRect;
        }

        public Animation createZoomInAnimation() {
            Animation animation = new Animation(Animation.ANIM_TYPE_ZOOM_IN) {
                @Override
                public boolean onDraw(Canvas canvas) {
                    float progress = getProgress();
                    int traverseTimes = Math.max(mWaveFormDataList.size() - 1 - mTouchDownSampleIndexInNormalMode, mTouchDownSampleIndexInNormalMode);
                    //LogUtil.i(TAG_1, "traverseTimes:" + traverseTimes + " mTouchDownSampleIndexInNormalMode:" + mTouchDownSampleIndexInNormalMode + " progress:" + progress);
                    boolean leftQuit = false;
                    boolean rightQuit = false;
                    RectF srcRect = new RectF();
                    RectF dstRect = new RectF();
                    for(int i = 0; i <= traverseTimes; i++) {
                        int leftIndex = mTouchDownSampleIndexInNormalMode - i;
                        int rightIndex = mTouchDownSampleIndexInNormalMode + i;
                        if(leftIndex >= 0 && ! leftQuit) {
                            WaveFormData data = mWaveFormDataList.get(leftIndex);
                            srcRect = getRectInNormalMode(leftIndex, srcRect);
                            dstRect = getRectInZoomInMode(leftIndex, dstRect);
                            float centerX = srcRect.centerX() + (dstRect.centerX() - srcRect.centerX()) * progress;
                            float centerY = srcRect.centerY() /*+ (dstRect.centerY() - srcRect.centerY()) * progress*/;
                            float drawHeight = getAmplitudeDrawHeight(data.getAmplitude());
                            mTempRect.set(centerX - mAmplitudeWidth / 2f, centerY - drawHeight / 2f,
                                    centerX + mAmplitudeWidth / 2f, centerY + drawHeight / 2f);
                            canvas.drawRect(mTempRect, mPaint);
                            /*
                            if(dstRect.right < 0) {
                                leftQuit = true;
                            }
                            */
                        }
                        if(rightIndex <= mWaveFormDataList.size() - 1 && ! rightQuit) {
                            WaveFormData data = mWaveFormDataList.get(rightIndex);
                            srcRect = getRectInNormalMode(rightIndex, srcRect);
                            dstRect = getRectInZoomInMode(rightIndex, dstRect);
                            float centerX = srcRect.centerX() + (dstRect.centerX() - srcRect.centerX()) * progress;
                            float centerY = srcRect.centerY() /*+ (dstRect.centerY() - srcRect.centerY()) * progress*/;
                            float drawHeight = getAmplitudeDrawHeight(data.getAmplitude());
                            mTempRect.set(centerX - mAmplitudeWidth / 2f, centerY - drawHeight / 2f,
                                    centerX + mAmplitudeWidth / 2f, centerY + drawHeight / 2f);
                            canvas.drawRect(mTempRect, mPaint);
                            /*
                            if(dstRect.left > mWidth) {
                                rightQuit = true;
                            }
                            */
                        }
                    }
                    float leftPositionRelativeToScreenLeft = mLeftClipPositionInZoomInMode - mOffset;
                    float rightPositionRelativeToScreenLeft = mRightClipPositionInZoomInMode - mOffset;
                    float playPositionRelativeToScreenLeft = mPlayPositionInZoomInMode - mOffset;

                    float leftClipPosition = mLeftClipPositionInNormalMode + (leftPositionRelativeToScreenLeft - mLeftClipPositionInNormalMode) * progress;
                    drawClipPosition(canvas, leftClipPosition);
                    /*
                    LogUtil.i("TTTTTT", " drawClipPosition mLeftClipPositionInNormalMode:" + mLeftClipPositionInNormalMode +
                            " leftPositionRelativeToScreenLeft:" + leftPositionRelativeToScreenLeft +
                            " mLeftClipPositionInNormalMode:" + mLeftClipPositionInNormalMode + " progress:" + progress); */

                    float rightClipPosition = mRightClipPositionInNormalMode + (rightPositionRelativeToScreenLeft - mRightClipPositionInNormalMode) * progress;
                    drawClipPosition(canvas, rightClipPosition);

                    /*
                    LogUtil.i("TTTTTT", " drawClipPosition mRightClipPositionInNormalMode:" + mRightClipPositionInNormalMode +
                            " rightPositionRelativeToScreenLeft:" + rightPositionRelativeToScreenLeft +
                            " mRightClipPositionInNormalMode:" + mRightClipPositionInNormalMode + " progress:" + progress); */

                    drawPlayPosition(canvas, mPlayPositionInNormalMode + (playPositionRelativeToScreenLeft - mPlayPositionInNormalMode) * progress);
                    //LogUtil.i("aaaaaa", " 000 left :" + leftPositionRelativeToScreenLeft);
                    //LogUtil.i("aaaaaa", " 000 right :" + rightPositionRelativeToScreenLeft);

                    drawLeftBlackMask(canvas, leftClipPosition);
                    drawRightBlackMask(canvas, rightClipPosition);


                    if(progress < 1.0f) return true;
                    return false;
                }
            };
            animation.setInterpolator(new DecelerateInterpolator());
            animation.setAnimationListener(ClipView.this);
            animation.start();
            return animation;
        }

        private void calculateNewClipPositionInNormalMode() {
            float leftPositionRatio = (mLeftClipPositionInZoomInMode - mDrawPadding) /  (mWholeLength -2 * mDrawPadding);
            float rightPositionRatio = (mRightClipPositionInZoomInMode - mDrawPadding) / (mWholeLength -2 * mDrawPadding);
            float playPositionRatio = (mPlayPositionInZoomInMode - mDrawPadding) / (mWholeLength -2 * mDrawPadding);

            /*
            mLeftClipPositionInNormalMode = (mWidth - 2 * mDrawPadding) * leftPositionRatio + mDrawPadding;
            mRightClipPositionInNormalMode = (mWidth - 2 * mDrawPadding) * rightPositionRatio + mDrawPadding;
            mPlayPositionInNormalMode = (mWidth - 2 * mDrawPadding) * playPositionRatio + mDrawPadding;
            */
            mLeftClipPositionInNormalMode = (mRightClipRestrictedPosition - mDrawPadding) * leftPositionRatio + mDrawPadding;
            mRightClipPositionInNormalMode = (mRightClipRestrictedPosition - mDrawPadding) * rightPositionRatio + mDrawPadding;
            mPlayPositionInNormalMode = (mRightClipRestrictedPosition - mDrawPadding) * playPositionRatio + mDrawPadding;


            mLeftClipPositionInNormalMode = clamp(mLeftClipPositionInNormalMode, mDrawPadding, mRightClipRestrictedPosition);
            mRightClipPositionInNormalMode = clamp(mRightClipPositionInNormalMode, mLeftClipPositionInNormalMode, mRightClipRestrictedPosition);
            mPlayPositionInNormalMode = clamp(mPlayPositionInNormalMode, mLeftClipPositionInNormalMode, mRightClipPositionInNormalMode);
        }

        public Animation createZoomOutAnimation() {
            Animation animation = new Animation(Animation.ANIM_TYPE_ZOOM_OUT) {
                @Override
                public boolean onDraw(Canvas canvas) {
                    float progress = getProgress();
                    int traverseTimes = mWaveFormDataList.size() - 1 ;
                    RectF srcRect = new RectF();
                    RectF dstRect = new RectF();
                    for(int i = 0; i <= traverseTimes; i++) {
                        WaveFormData data = mWaveFormDataList.get(i - (i % NORMAL_MODE_DRAW_STEP));
                        srcRect = getRectInZoomInMode(i, srcRect);
                        dstRect = getRectInNormalMode(i, dstRect);
                        float centerX = srcRect.centerX() + (dstRect.centerX() - srcRect.centerX()) * progress;
                        float centerY = srcRect.centerY() /*+ (dstRect.centerY() - srcRect.centerY()) * progress*/;
                        float drawHeight = getAmplitudeDrawHeight(data.getAmplitude());
                        //LogUtil.i(TAG, "drawHeight:" + drawHeight + " data.getAmplitude():" + data.getAmplitude());
                        mTempRect.set(centerX - mAmplitudeWidth / 2f, centerY - drawHeight / 2f,
                                centerX + mAmplitudeWidth / 2f, centerY + drawHeight / 2f);
                        canvas.drawRect(mTempRect, mPaint);
                    }
                    float leftPositionRelativeToScreenLeft = mLeftClipPositionInZoomInMode - mOffset;
                    float rightPositionRelativeToScreenLeft = mRightClipPositionInZoomInMode - mOffset;
                    float playPositionRelativeToScreenLeft = mPlayPositionInZoomInMode - mOffset;
                    float leftClipPosition = leftPositionRelativeToScreenLeft + (mLeftClipPositionInNormalMode - leftPositionRelativeToScreenLeft) * progress;
                    float rightClipPosition = rightPositionRelativeToScreenLeft + (mRightClipPositionInNormalMode - rightPositionRelativeToScreenLeft) * progress;
                    drawPlayPosition(canvas, playPositionRelativeToScreenLeft + (mPlayPositionInNormalMode - playPositionRelativeToScreenLeft) * progress);
                    drawClipPosition(canvas,  leftClipPosition);
                    drawClipPosition(canvas,  rightClipPosition);
                    drawLeftBlackMask(canvas, leftClipPosition);
                    drawRightBlackMask(canvas, rightClipPosition);
                    if(progress < 1.0f) return true;
                    return false;
                }
            };
            animation.setInterpolator(new AccelerateInterpolator());
            animation.setAnimationListener(ClipView.this);
            animation.start();
            return animation;
        }

        private void changeOffset(float deltaX) {
            //LogUtil.i("PPPPPP", "mOffset:" + mOffset + " BEFORE add deltaX:" + deltaX);
            mOffset += deltaX;
            //LogUtil.i("PPPPPP", "mOffset:" + mOffset + " AFTER add deltaX:" + deltaX);
            mOffset = clamp(mOffset, 0, Math.max(0, mWholeLength - mWidth));
            //LogUtil.i("PPPPPP", "mOffset:" + mOffset + " AFTER clamp deltaX:" + deltaX + " mWholeLength: " + mWholeLength + " mWidth:" + mWidth + " mWholeLength - mWidth:" + (mWholeLength - mWidth));
        }

        private void changeLeftClipPositionInZoomMode(float deltaX) {
            //LogUtil.i("PPPPPP", " changeLeftClipPositionInZoomMode ----- mLeftClipPositionInZoomInMode BEFORE add delata:" + mLeftClipPositionInZoomInMode);

            mLeftClipPositionInZoomInMode += deltaX;

            //LogUtil.i("PPPPPP", " changeLeftClipPositionInZoomMode ----- mLeftClipPositionInZoomInMode AFTER add delata:" + mLeftClipPositionInZoomInMode);

            mLeftClipPositionInZoomInMode = clamp(mLeftClipPositionInZoomInMode, mDrawPadding, mRightClipPositionInZoomInMode);
            /*
            LogUtil.i("PPPPPP", " changeLeftClipPositionInZoomMode ----- mLeftClipPositionInZoomInMode AFTER CLAMP:" + mLeftClipPositionInZoomInMode +
                    " mDrawPadding:" + mDrawPadding +
                    " mRightClipPositionInZoomInMode:" + mRightClipPositionInZoomInMode); */
        }

        private void changeRightClipPositionInZoomMode(float deltaX) {
            //LogUtil.i(TAG, "mRightClipPositionInZoomInMode:" + mRightClipPositionInZoomInMode);
            mRightClipPositionInZoomInMode += deltaX;
            //LogUtil.i(TAG, "deltaX:" + deltaX );
            mRightClipPositionInZoomInMode = clamp(mRightClipPositionInZoomInMode, mLeftClipPositionInZoomInMode, mWholeLength - mDrawPadding);
            //LogUtil.i(TAG, "deltaX:" + deltaX);
        }

        private void changePlayPositionInZoomMode() {
            if(mPlayPositionInZoomInMode < mLeftClipPositionInZoomInMode) {
                mPlayPositionInZoomInMode = mLeftClipPositionInZoomInMode;
            }
            if(mPlayPositionInZoomInMode > mRightClipPositionInZoomInMode) {
                mPlayPositionInZoomInMode = mRightClipPositionInZoomInMode;
            }
        }

        /**
         * deltaX may be negative,
         * if negative means moving left
         * if positive means moving right
         * @param deltaX
         */
        public void moveClipPosition(float deltaX) {
            //LogUtil.i("PPPPPP", "moveClipPosition -----");
            if(mClipPositionMode == CLIP_POSITION_MODE_LEFT) {
                //if move to left or right edge of a screen,
                float leftClipPositionRelativeToScreen = mLeftClipPositionInZoomInMode - mOffset;
                if(isInLeftClipPositionMoveArea(leftClipPositionRelativeToScreen)) {
                    if(isHeldStill(deltaX) && mPrevDeltaX < 0) {
                        changeOffset(mPrevDeltaX);
                        changeLeftClipPositionInZoomMode(mPrevDeltaX);
                        //LogUtil.i("PPPPPP", "LLLLLLL 1111111 isHeldStill: ===== " + isHeldStill(deltaX) + " deltaX:" + deltaX + " mHoldStillThreshold:" + mHoldStillThreshold + " mOffset:" + mOffset);
                    } else if(deltaX < 0) {
                        changeOffset(deltaX);
                        changeLeftClipPositionInZoomMode(deltaX);
                        mPrevDeltaX = deltaX;
                        //LogUtil.i("PPPPPP", "LLLLLLL 2222222 deltaX:" + deltaX + " mOffset:" + mOffset);
                    } else {
                        //LogUtil.i("PPPPPP", "LLLLLLL 3333333 deltaX:" + deltaX + " mOffset:" + mOffset);
                        changeLeftClipPositionInZoomMode(deltaX);
                        mPrevDeltaX = deltaX;
                    }
                } else if(isInRightClipPositionMoveArea(leftClipPositionRelativeToScreen)) {
                    if(isHeldStill(deltaX) && mPrevDeltaX > 0) {
                        changeOffset(mPrevDeltaX);
                        changeLeftClipPositionInZoomMode(mPrevDeltaX);
                        /*
                        LogUtil.i("PPPPPP", "RRRRRRR 11111111 isHeldStill: ===== " + isHeldStill(deltaX) + " deltaX:" + deltaX +
                                " mPrevDeltaX:" + mPrevDeltaX +
                                " mHoldStillThreshold:" + mHoldStillThreshold + " mOffset:" + mOffset);*/
                    } else if(deltaX > 0) {
                        changeOffset(deltaX);
                        changeLeftClipPositionInZoomMode(deltaX);
                        mPrevDeltaX = deltaX;
                        //LogUtil.i("PPPPPP", "RRRRRRR 2222222 deltaX:" + deltaX + " mOffset:" + mOffset);
                    } else {
                        //LogUtil.i("PPPPPP", "RRRRRRR 3333333 deltaX:" + deltaX);
                        changeLeftClipPositionInZoomMode(deltaX);
                        mPrevDeltaX = deltaX;
                    }
                } else {
                    //LogUtil.i("PPPPPP", "ELSE deltaX:" + deltaX + " mOffset:" + mOffset);
                    changeLeftClipPositionInZoomMode(deltaX);
                    mPrevDeltaX = deltaX;
                }
            } else if(mClipPositionMode == CLIP_POSITION_MODE_RIGHT){
                //if move to left or right edge of a screen,
                float rightClipPositionRelativeToScreen = mRightClipPositionInZoomInMode - mOffset;
                if(isInLeftClipPositionMoveArea(rightClipPositionRelativeToScreen)) {
                    if(isHeldStill(deltaX) && mPrevDeltaX < 0) {
                        changeOffset(mPrevDeltaX);
                        changeRightClipPositionInZoomMode(mPrevDeltaX);
                        //LogUtil.i("PPPPPP", "LLLLLLL 1111111 isHeldStill: ===== " + isHeldStill(deltaX) + " deltaX:" + deltaX + " mHoldStillThreshold:" + mHoldStillThreshold);
                    } else if(deltaX < 0) {
                        changeOffset(deltaX);
                        changeRightClipPositionInZoomMode(deltaX);
                        mPrevDeltaX = deltaX;
                        //LogUtil.i("PPPPPP", "LLLLLLL 2222222 deltaX:" + deltaX + " mOffset:" + mOffset);
                    } else {
                        //LogUtil.i("PPPPPP", "LLLLLLL 3333333 deltaX:" + deltaX);
                        changeRightClipPositionInZoomMode(deltaX);
                        mPrevDeltaX = deltaX;
                    }
                } else if(isInRightClipPositionMoveArea(rightClipPositionRelativeToScreen)) {
                    if(isHeldStill(deltaX) && mPrevDeltaX > 0) {
                        changeOffset(mPrevDeltaX);
                        changeRightClipPositionInZoomMode(mPrevDeltaX);
                        /*
                        LogUtil.i("PPPPPP", "RRRRRRR 11111111 isHeldStill: ===== " + isHeldStill(deltaX) + " deltaX:" + deltaX +
                                " mPrevDeltaX:" + mPrevDeltaX +
                                " mHoldStillThreshold:" + mHoldStillThreshold);*/
                    } else if(deltaX > 0) {
                        changeOffset(deltaX);
                        changeRightClipPositionInZoomMode(deltaX);
                        mPrevDeltaX = deltaX;
                        //LogUtil.i("PPPPPP", "RRRRRRR 2222222 deltaX:" + deltaX + " mOffset:" + mOffset);
                    } else {
                        //LogUtil.i("PPPPPP", "RRRRRRR 3333333 deltaX:" + deltaX);
                        changeRightClipPositionInZoomMode(deltaX);
                        mPrevDeltaX = deltaX;
                    }
                } else {
                    changeRightClipPositionInZoomMode(deltaX);
                    mPrevDeltaX = deltaX;
                }
            }
            //change play position according to left&right clip position
            changePlayPositionInZoomMode();
            //calculate corresponding position in normal mode
            calculateNewClipPositionInNormalMode();
            //mPrevDeltaX = deltaX;
            invalidate();
            updateTimeDisplay();
        }
    }

    private boolean isInLeftClipPositionMoveArea(float clipPositionRelativeToScreen) {
        if(clipPositionRelativeToScreen > 0 && clipPositionRelativeToScreen < mLeftClipPositionMoveArea) return true;
        return false;
    }

    public boolean isInRightClipPositionMoveArea(float clipPositionRelativeToScreen) {
        if(clipPositionRelativeToScreen > mRightClipPositionMoveArea && clipPositionRelativeToScreen < mWidth) return true;
        return false;
    }

    // finger not moved
    private boolean isHeldStill(float deltaX) {
        return Math.abs(deltaX) < mHoldStillThreshold;
    }

    public static float clamp(float value, float lowerLimit, float upperLimit) {
        if(value < lowerLimit) value = lowerLimit;
        if(value > upperLimit) value = upperLimit;
        return value;
    }

    /**
     * This function MUST be used in normal mode
     * @param presentationTime
     * @return return whether play postion reaches right clip position
     */
    public boolean updatePlayPositionWhenPlaying(long presentationTime) {
        float playPosition = getPlayPositionByPresentationTimeInNormalMode(presentationTime);
        //LogUtil.i(TAG, "updatePlayPositionWhenPlaying playPosition: " + playPosition);
        mPlayPositionInNormalMode = playPosition;
        boolean reachRight = mPlayPositionInNormalMode >= mRightClipPositionInNormalMode;
        //LogUtil.i("XXX", "reachRight:" + reachRight + " ----------- " );
        if(reachRight) {
            mPlayPositionInNormalMode = mRightClipPositionInNormalMode;
        }
        updateTimeDisplay();
        invalidate();
        return reachRight;
    }
}
