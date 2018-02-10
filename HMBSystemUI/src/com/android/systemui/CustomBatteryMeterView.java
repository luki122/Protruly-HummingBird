package com.android.systemui;

import java.text.NumberFormat;

import com.android.systemui.statusbar.policy.BatteryController;

import android.animation.ArgbEvaluator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.systemui.R;

public class CustomBatteryMeterView extends View implements DemoMode, BatteryController.BatteryStateChangeCallback {
    
    public static final String TAG = "CustomBatteryMeterView";
    public static final String ACTION_LEVEL_TEST = "com.android.systemui.BATTERY_LEVEL_TEST";
    public static final String SHOW_PERCENT_SETTING = "status_bar_show_battery_percent";
    
    //defined by LiuQin in Settings.apk
    public static final String NOTIFICATION_SHOW_BATTERY_PERCENTAGE = "notification_show_battery_percentage";
    
    private int mWidth;
    private int mHeight;
    
    private RectF mFrameRect = new RectF();
    private RectF mStickOutRect = new RectF();
    private RectF mBatteryIndicatorRect = new RectF();
    
    private int mFrameAndStickOutColor = Color.WHITE;
    private int mChargingColor = Color.parseColor("#7ED321");
    private int mLowBatteryIndicatorColor = Color.RED;
    //private int mFullBatteryIndicatorColor = Color.;
    private int mNormalBatteryIndicatorColor = Color.WHITE;
    
    private int mBatteryPercentColor = Color.WHITE;
    
    private Paint mFramePaint /*= new Paint(Paint.ANTI_ALIAS_FLAG)*/;
    private Paint mStickOutPaint /*= new Paint(Paint.ANTI_ALIAS_FLAG)*/;
    private Paint mBatteryIndicatorPaint;
    
    private float mFrameBorderWidth = 6; //in dp
    
    private float mStickOutWidth;
    private float mStickOutHeight;
    private float mStickOutCornerRadius;
    
    private float mFramePaddingLeft;
    private float mFramePaddingTop;
    private float mFramePaddingRight;
    private float mFramePaddingBottom;
    
    private float mBatteryIndicatorPaddingLeft;
    private float mBatteryIndicatorPaddingTop;
    private float mBatteryIndicatorPaddingRight;
    private float mBatteryIndicatorPaddingBottom;

    //defined by Google
    private boolean mShowPercent;
    
    //defined by HMB begin
    private boolean mShowBatteryPercentage;
    //defined by HMB begin
    
    private int mDarkModeBackgroundColor;
    private int mDarkModeFillColor;
    private int mDarkModeFrameAndStickOutColor;
    private int mDarkModeChargingColor;
    private int mDarkModeLowBatteryColor;
    private int mDarkModeBatteryPercentColor;

    private int mLightModeBackgroundColor;
    private int mLightModeFillColor;
    private int mLightModeFrameAndStickOutColor;
    private int mLightModeChargingColor;
    private int mLightModeLowBatteryColor;
    private int mLightModeBatteryPercentColor;

    private int mIconTint = Color.WHITE;

    private BatteryTracker mTracker = new BatteryTracker();
    private final SettingObserver mSettingObserver = new SettingObserver();
    
    //added by ShenQianfeng begin
    private final ShowBatteryPercentageSettingObserver mBatteryPercentageSettingObserver = new ShowBatteryPercentageSettingObserver(); 
    //added by ShenQianfeng begin
    
    private boolean mDemoMode;
    private BatteryTracker mDemoTracker = new BatteryTracker();
    
    private BatteryController mBatteryController;
    private boolean mPowerSaveEnabled;

    //text view used to show battery percentage
    private TextView mBatteryLevelTextView;

    //add by chenhl start
    private int mPowerModeColor;
    //add by chenhl end
    
    public CustomBatteryMeterView(Context context){
        super(context);
    }

    public CustomBatteryMeterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final Resources res = context.getResources();
        
        mFramePaddingLeft = res.getDimensionPixelSize(R.dimen.custom_battery_padding_left);
        mFramePaddingTop = res.getDimensionPixelSize(R.dimen.custom_battery_padding_top);
        mFramePaddingRight = res.getDimensionPixelSize(R.dimen.custom_battery_padding_right);
        mFramePaddingBottom = res.getDimensionPixelSize(R.dimen.custom_battery_padding_bottom);
        mFrameBorderWidth = res.getDimensionPixelSize(R.dimen.custom_battery_frame_border_width);
        
        mStickOutWidth = res.getDimensionPixelSize(R.dimen.custom_battery_stickout_width);
        mStickOutHeight = res.getDimensionPixelSize(R.dimen.custom_battery_stickout_height);
        mStickOutCornerRadius = res.getDimensionPixelSize(R.dimen.custom_battery_stickout_corner_radius);
        
        mBatteryIndicatorPaddingLeft = res.getDimensionPixelSize(R.dimen.custom_battery_indicator_padding_left);
        mBatteryIndicatorPaddingTop = res.getDimensionPixelSize(R.dimen.custom_battery_indicator_padding_top);
        mBatteryIndicatorPaddingRight = res.getDimensionPixelSize(R.dimen.custom_battery_indicator_padding_right);
        mBatteryIndicatorPaddingBottom = res.getDimensionPixelSize(R.dimen.custom_battery_indicator_padding_bottom);
        
        updateShowPercent();
        
        mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint.setDither(true);
        mFramePaint.setStrokeWidth(mFrameBorderWidth);
        mFramePaint.setStyle(Paint.Style.STROKE);
        //mFramePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        
        mStickOutPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStickOutPaint.setDither(true);
        mStickOutPaint.setStyle(Paint.Style.FILL);
        
        mBatteryIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStickOutPaint.setDither(true);
        mStickOutPaint.setStyle(Paint.Style.FILL);
        
        mChargingColor = context.getColor(R.color.batterymeter_charge_color);
        
        mDarkModeBackgroundColor = context.getColor(R.color.dark_mode_icon_color_dual_tone_background);
        mDarkModeFillColor = context.getColor(R.color.dark_mode_icon_color_dual_tone_fill);
        mDarkModeFrameAndStickOutColor = context.getColor(R.color.dark_mode_frame_and_stickout_color);
        mDarkModeChargingColor = context.getColor(R.color.dark_mode_charging_color);
        mDarkModeLowBatteryColor = context.getColor(R.color.dark_mode_low_battery_color);
        
        mDarkModeBatteryPercentColor = context.getColor(R.color.dark_mode_battery_percent_color);
        		
        mLightModeBackgroundColor = context.getColor(R.color.light_mode_icon_color_dual_tone_background);
        mLightModeFillColor = context.getColor(R.color.light_mode_icon_color_dual_tone_fill);
        mLightModeFrameAndStickOutColor = context.getColor(R.color.light_mode_frame_and_stickout_color);
        mLightModeChargingColor = context.getColor(R.color.light_mode_charging_color);
        mLightModeLowBatteryColor = context.getColor(R.color.light_mode_low_battery_color);
        
        mLightModeBatteryPercentColor = context.getColor(R.color.light_mode_battery_percent_color);

        //add by chenhl  start
        mPowerModeColor = context.getColor(R.color.hb_low_power_mode_color);
        updatePowerMode();
        //add by chenhl end
    }
    
    public void setBatteryLevelTextView (TextView batteryLevelView) {
    	mBatteryLevelTextView = batteryLevelView;
        updateShowPercent();//add by chenhl
    }

    public void setBatteryController(BatteryController batteryController) {
        Log.i(TAG, "CustomBatteryMeterView::setBatteryController ------ ");
        mBatteryController = batteryController;
        mPowerSaveEnabled = mBatteryController.isPowerSave();
    }
    
    public void setDarkIntensity(float darkIntensity) {
        int backgroundColor = getBackgroundColor(darkIntensity);
        int fillColor = getFillColor(darkIntensity);
        //mIconTint = fillColor;
        //this.setBackgroundColor(backgroundColor);
        //mFramePaint.setColor(backgroundColor);
        //mBoltPaint.setColor(fillColor);
        mFrameAndStickOutColor = getFrameAndStickOutColor(darkIntensity);
        //mFramePaint.setColor(mFrameAndStickOutColor);
        //mStickOutPaint.setColor(mFrameAndStickOutColor);
        mChargingColor = getChargeColor(darkIntensity);
        mLowBatteryIndicatorColor = getLowBatteryColor(darkIntensity);
        
        mBatteryPercentColor = getBatteryPercentageColor(darkIntensity);
        mBatteryLevelTextView.setTextColor(mBatteryPercentColor);
        //mChargeColor = fillColor;
        invalidate();
    }
    
    private int getBackgroundColor(float darkIntensity) {
        return getColorForDarkIntensity(
                darkIntensity, mLightModeBackgroundColor, mDarkModeBackgroundColor);
    }
    
    private int getFrameAndStickOutColor(float darkIntensity) {
    	 return getColorForDarkIntensity(darkIntensity, mLightModeFrameAndStickOutColor, mDarkModeFrameAndStickOutColor);
    }
    
    private int getChargeColor(float darkIntensity) {
        return getColorForDarkIntensity(darkIntensity, mLightModeChargingColor, mDarkModeChargingColor);
    }
    
    private int getLowBatteryColor(float darkIntensity) {
    	return getColorForDarkIntensity(darkIntensity, mLightModeLowBatteryColor, mDarkModeLowBatteryColor);
    }
    
    private int getColorForDarkIntensity(float darkIntensity, int lightColor, int darkColor) {
        return (int) ArgbEvaluator.getInstance().evaluate(darkIntensity, lightColor, darkColor);
    }

    private int getFillColor(float darkIntensity) {
        return getColorForDarkIntensity(
                darkIntensity, mLightModeFillColor, mDarkModeFillColor);
    }
    
    private int getBatteryPercentageColor(float darkIntensity) {
        return getColorForDarkIntensity(darkIntensity, mLightModeBatteryPercentColor, mDarkModeBatteryPercentColor);
    }

    private void updateShowPercent() {
        mShowPercent = 0 != Settings.System.getInt(getContext().getContentResolver(), SHOW_PERCENT_SETTING, 0);
        mShowBatteryPercentage = 0 != Settings.Secure.getInt(getContext().getContentResolver(), NOTIFICATION_SHOW_BATTERY_PERCENTAGE, 1);
        updateBatteryLevelViewVisibility();
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        String percentage = NumberFormat.getPercentInstance().format((double) level / 100.0);
        if(mBatteryLevelTextView != null) {
            mBatteryLevelTextView.setText(percentage);
        }
    }

    @Override
    public void onPowerSaveChanged() {
        mPowerSaveEnabled = mBatteryController.isPowerSave();
        invalidate();
    }

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        if (!mDemoMode && command.equals(COMMAND_ENTER)) {
            mDemoMode = true;
            mDemoTracker.level = mTracker.level;
            mDemoTracker.plugged = mTracker.plugged;
        } else if (mDemoMode && command.equals(COMMAND_EXIT)) {
            mDemoMode = false;
            postInvalidate();
        } else if (mDemoMode && command.equals(COMMAND_BATTERY)) {
           String level = args.getString("level");
           String plugged = args.getString("plugged");
           if (level != null) {
               mDemoTracker.level = Math.min(Math.max(Integer.parseInt(level), 0), 100);
           }
           if (plugged != null) {
               mDemoTracker.plugged = Boolean.parseBoolean(plugged);
           }
           postInvalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        BatteryTracker tracker = mDemoMode ? mDemoTracker : mTracker;
        final int level = tracker.level;
        if (level == BatteryTracker.UNKNOWN_LEVEL) return;
        
        // draw battery frame
        float left = mFramePaddingLeft;
        float top = mFramePaddingTop;
        float right = mWidth - mFramePaddingRight - mStickOutWidth;
        float bottom = mHeight - mFramePaddingBottom;
        mFrameRect.set(left, top, right, bottom);
        mFramePaint.setColor(mFrameAndStickOutColor);
        canvas.drawRoundRect(mFrameRect, 5, 5, mFramePaint);
        
        // draw stick out 
        left = mWidth - mFramePaddingRight - mStickOutWidth + mFrameBorderWidth/2f;
        top = (mHeight - mStickOutHeight) / 2;
        right = left + mStickOutWidth;
        bottom = top + mStickOutHeight;
        mStickOutRect.set(left, top, right, bottom);
        mStickOutPaint.setColor(mFrameAndStickOutColor);
        canvas.drawRoundRect(mStickOutRect, mStickOutCornerRadius, mStickOutCornerRadius, mStickOutPaint);
        
        // draw battery indicator
        left = mFramePaddingLeft + mFrameBorderWidth + mBatteryIndicatorPaddingLeft; 
        float wholeRight = mWidth - mFramePaddingRight - mStickOutWidth - mBatteryIndicatorPaddingRight - mFrameBorderWidth;
        top = mFramePaddingTop + mFrameBorderWidth + mBatteryIndicatorPaddingTop;
        bottom =  mHeight - mFramePaddingBottom - mFrameBorderWidth - mBatteryIndicatorPaddingBottom;
        Log.i(TAG, "level : " + level);
        //modify by chenhl start
        if(mPowerMode){
            mBatteryIndicatorPaint.setColor(mPowerModeColor);
        }else if(level < 10&&!tracker.plugged) {
        	mBatteryIndicatorPaint.setColor(mLowBatteryIndicatorColor);
        } else if(tracker.plugged) {
        	mBatteryIndicatorPaint.setColor(mChargingColor);
        } else {
        	mBatteryIndicatorPaint.setColor(mFrameAndStickOutColor);//normal state color is the same to mFrameAndStickOutColor
        }
        //modeify by chenhl end
        mBatteryIndicatorRect.set(left, top, (left + (wholeRight - left) * level / 100.0f), bottom);
        canvas.drawRoundRect(mBatteryIndicatorRect, 1, 1, mBatteryIndicatorPaint);
        /*
        mBatteryPaint.setColor(tracker.plugged ? mChargeColor : getColorForLevel(level));
        mBatteryIndicatorPaddingLeft
        */
    }
    
    /*
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }
    */

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(ACTION_LEVEL_TEST);
        //add by chenhl start
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        //add by chenhl end
        final Intent sticky = getContext().registerReceiver(mTracker, filter);
        if (sticky != null) {
            // preload the battery level
            mTracker.onReceive(getContext(), sticky);
        }
        mBatteryController.addStateChangedCallback(this);
        getContext().getContentResolver().registerContentObserver(Settings.System.getUriFor(SHOW_PERCENT_SETTING), false, mSettingObserver);
        
        getContext().getContentResolver().registerContentObserver(Settings.Secure.getUriFor(NOTIFICATION_SHOW_BATTERY_PERCENTAGE), false, mBatteryPercentageSettingObserver);

        getContext().getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.POWER_MODE),false,mBatteryPercentageSettingObserver); //add by chenhl
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getContext().unregisterReceiver(mTracker);
        mBatteryController.removeStateChangedCallback(this);
        getContext().getContentResolver().unregisterContentObserver(mSettingObserver);
        getContext().getContentResolver().unregisterContentObserver(mBatteryPercentageSettingObserver);
        removeCallbacks(mLevelCallback); //add by chenhl
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
    }

    private final class BatteryTracker extends BroadcastReceiver {
        public static final int UNKNOWN_LEVEL = -1;

        // current battery status
        int level = UNKNOWN_LEVEL;
        String percentStr;
        int plugType;
        boolean plugged;
        int health;
        int status;
        String technology;
        int voltage;
        int temperature;
        boolean testmode = false;
        int curlevel;

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                if (testmode && ! intent.getBooleanExtra("testmode", false)) return;

                curlevel = (int)(100f * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) /
                        intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
                plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);

                health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH,
                        BatteryManager.BATTERY_HEALTH_UNKNOWN);
                status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN);
                technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
                voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);

                setContentDescription(context.getString(R.string.accessibility_battery_level, level));
                //modify  by chenhl start
                boolean lastPlug=plugged;
                plugged = plugType != 0;
                if(plugged!=lastPlug){
                    updateBatteryLevelViewVisibility();
                }

                if(curlevel<100&&plugged&&!isShowAnimation){
                    level = curlevel;
                    showLevelAnimation();
                }else if(curlevel>=100|| !plugged){
                    isShowAnimation = false;
                    removeCallbacks(mLevelCallback);
                }

                if(!isShowAnimation) {
                    level = curlevel;
                    postInvalidate();
                }
                //modify by chenhl end
            } else if (action.equals(ACTION_LEVEL_TEST)) {
                testmode = true;
                post(new Runnable() {
                    int curLevel = 0;
                    int incr = 1;
                    int saveLevel = level;
                    int savePlugged = plugType;
                    Intent dummy = new Intent(Intent.ACTION_BATTERY_CHANGED);
                    @Override
                    public void run() {
                        if (curLevel < 0) {
                            testmode = false;
                            dummy.putExtra("level", saveLevel);
                            dummy.putExtra("plugged", savePlugged);
                            dummy.putExtra("testmode", false);
                        } else {
                            dummy.putExtra("level", curLevel);
                            dummy.putExtra("plugged", incr > 0 ? BatteryManager.BATTERY_PLUGGED_AC
                                    : 0);
                            dummy.putExtra("testmode", true);
                        }
                        getContext().sendBroadcast(dummy);

                        if (!testmode) return;

                        curLevel += incr;
                        if (curLevel == 100) {
                            incr *= -1;
                        }
                        postDelayed(this, 200);
                    }
                });
            }
            //add by chenhl start
            else if(action.equals(Intent.ACTION_SCREEN_OFF)){
                isShowAnimation=false;
                removeCallbacks(mLevelCallback);
            }else if(action.equals(Intent.ACTION_SCREEN_ON)){
                if(curlevel<100&&plugged&&!isShowAnimation){
                    showLevelAnimation();
                }
            }
            //add by chenhl end
        }
    }
    
    private void updateBatteryLevelViewVisibility() {
        if(mBatteryLevelTextView != null) {
            //modify by chenhl start for add plugin judge
            if(mShowBatteryPercentage||mTracker.plugged) {
                mBatteryLevelTextView.setVisibility(View.VISIBLE);
            } else {
                mBatteryLevelTextView.setVisibility(View.GONE);
            }
            //modify by chenhl end
        } 
    }

    private final class SettingObserver extends ContentObserver {
        public SettingObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            updateShowPercent();
            postInvalidate();
        }
    }
    
    
    //defined by ShenQianfeng
    private final class ShowBatteryPercentageSettingObserver extends ContentObserver {
        public ShowBatteryPercentageSettingObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            //modify by chenhl start
            if(uri.equals(Settings.System.getUriFor(Settings.System.POWER_MODE))){
                updatePowerMode();
                postInvalidate();
            }

            if(uri.equals(Settings.Secure.getUriFor(NOTIFICATION_SHOW_BATTERY_PERCENTAGE))) {
                updateShowPercent();
                postInvalidate();
            }
            //modify by chenhl end
        }
    }

    //add by chenhl start

    private boolean isShowAnimation=false;
    private boolean mPowerMode=false;
    private void showLevelAnimation(){
        isShowAnimation = true;
        removeCallbacks(mLevelCallback);
        mTracker.level+=20;
        if(mTracker.level>100){
            mTracker.level=0;
        }
        postInvalidate();
        postDelayed(mLevelCallback,300);
    }

    private  Runnable mLevelCallback = new Runnable() {
        @Override
        public void run() {
            showLevelAnimation();
        }
    };

    private void updatePowerMode(){
        int mode=Settings.System.getInt(getContext().getContentResolver(),
                Settings.System.POWER_MODE,0);
        mPowerMode = mode==1;
    }
    //add by chenhl end
}
