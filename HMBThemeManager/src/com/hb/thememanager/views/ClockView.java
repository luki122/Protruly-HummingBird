package com.hb.thememanager.views;

import android.widget.RelativeLayout;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log ;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.TimeZone;
import com.hb.thememanager.utils.TLog;
import libcore.icu.LocaleData;

import com.hb.thememanager.R ;

public class ClockView extends RelativeLayout {
    private static final String TAG = "ClockView" ;

    private static final String ANDROID_CLOCK_FONT_FILE = "/system/fonts/AndroidClock.ttf";
    private static Typeface mClockTypeface = null;
    private final static String M12 = "h:mm";
    private final static String M24 = "kk:mm";

    private Calendar mCalendar;
    private String mFormat;
    private TextView mTimeView;
    private AmPm mAmPm;
    private ContentObserver mFormatChangeObserver;
    private int mAttached = 0; // for debugging - tells us whether attach/detach is unbalanced

    /* called by system on minute ticks */
    private final Handler mHandler = new Handler();
    private BroadcastReceiver mIntentReceiver;

    private static class TimeChangedReceiver extends BroadcastReceiver {
        private WeakReference<ClockView> mClock;
        private Context mContext;

        public TimeChangedReceiver(ClockView clock) {
            mClock = new WeakReference<ClockView>(clock);
            mContext = clock.getContext();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // Post a runnable to avoid blocking the broadcast.
            final boolean timezoneChanged =
                    intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED);
            final boolean screenOn =
                    intent.getAction().equals(Intent.ACTION_SCREEN_ON);
            final ClockView clock = mClock.get();
            if (clock != null) {
                clock.mHandler.post(new Runnable() {
                    public void run() {
                        if (timezoneChanged || screenOn) {
                            TLog.d(TAG, "need to update timezone.");
                            clock.mCalendar = Calendar.getInstance();
                        }
                        clock.updateTime();
                    }
                });
            } else {
                try {
                    mContext.unregisterReceiver(this);
                } catch (IllegalArgumentException e) {
                    // Shouldn't happen
                    Log.e(TAG, "onReceive() - unregisterReceiver(this) fails") ;
                }
            }
        }
    };

    static class AmPm {
        private TextView mAmPmTextView;
        private String mAmString, mPmString;

        AmPm(View parent, Typeface tf) {
            // No longer used, uncomment if we decide to use AM/PM indicator again
            mAmPmTextView = (TextView) parent.findViewById(R.id.am_pm);

            if (mAmPmTextView != null && tf != null) {
                mAmPmTextView.setTypeface(tf);
            }

            updateAmPmText() ;
        }

        void setShowAmPm(boolean show) {
            if (mAmPmTextView != null) {
                mAmPmTextView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        }
        void  setTextColor(int color){
        	mAmPmTextView.setTextColor(color);
        }

        void setIsMorning(boolean isMorning) {
            if (mAmPmTextView != null) {
                mAmPmTextView.setText(isMorning ? mAmString : mPmString);
            }
        }

        void updateAmPmText() {
            TLog.d(TAG, "updateAmPmText() enters.") ;
            String[] ampm = new DateFormatSymbols().getAmPmStrings();
            mAmString = ampm[0];
            mPmString = ampm[1];
            TLog.d(TAG, "mAmString = " + mAmString + " mPmString = " + mPmString) ;
        }
    }

    private static class FormatChangeObserver extends ContentObserver {
        private WeakReference<ClockView> mClock;
        private Context mContext;
        public FormatChangeObserver(ClockView clock) {
            super(new Handler());
            mClock = new WeakReference<ClockView>(clock);
            mContext = clock.getContext();
        }
        @Override
        public void onChange(boolean selfChange) {
            ClockView digitalClock = mClock.get();
            if (digitalClock != null) {
                digitalClock.setDateFormat();
                digitalClock.updateTime();
            } else {
                try {
                    mContext.getContentResolver().unregisterContentObserver(this);
                } catch (IllegalStateException ise) {
                    // Shouldn't happen
                	TLog.e(TAG, "onChange() - unregisterContentObserver() fails.") ;
                }
            }
        }
    }

    public ClockView(Context context) {
        this(context, null);
    }

    /*package*/ void showAmPmIfNeeded(boolean show){
    	mAmPm.setShowAmPm(show);
    }
    
    public ClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public void setTextColor(int color){
    	mTimeView.setTextColor(color);
    	mAmPm.setTextColor(color);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTimeView = (TextView) findViewById(R.id.clock_text);

        mTimeView.setTypeface(getClockTypeface());
        mAmPm = new AmPm(this, null);
        mCalendar = Calendar.getInstance();
        setDateFormat();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        TLog.d(TAG, "onAttachedToWindow()");
        mAttached++;

        /* monitor time ticks, time changed, timezone */
        if (mIntentReceiver == null) {
            mIntentReceiver = new TimeChangedReceiver(this);
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            mContext.registerReceiverAsUser(mIntentReceiver, UserHandle.OWNER, filter, null, null);
        }

        /* monitor 12/24-hour display preference */
        if (mFormatChangeObserver == null) {
            mFormatChangeObserver = new FormatChangeObserver(this);
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.CONTENT_URI, true, mFormatChangeObserver);
        }

        // The time zone may have changed while the receiver wasn't registered, so update the Time
        mCalendar = Calendar.getInstance(TimeZone.getDefault());
        TLog.d(TAG, "set default timezone, timezone = " + TimeZone.getDefault());

        updateTime();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mAttached--;

        if (mIntentReceiver != null) {
            mContext.unregisterReceiver(mIntentReceiver);
        }
        if (mFormatChangeObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(
                    mFormatChangeObserver);
        }

        mFormatChangeObserver = null;
        mIntentReceiver = null;
    }

    void updateTime(Calendar c) {
        mCalendar = c;
        updateTime();
    }

    public void updateTime() {
        mCalendar.setTimeInMillis(System.currentTimeMillis());

        CharSequence newTime = DateFormat.format(mFormat, mCalendar);
        mTimeView.setText(newTime);
        mAmPm.setIsMorning(mCalendar.get(Calendar.AM_PM) == 0);
    }

    private void setDateFormat() {
        LocaleData ld = LocaleData.get(getContext().getResources().getConfiguration().locale);
        boolean isTimeFormat24 = android.text.format.DateFormat.is24HourFormat(getContext()) ;
        mFormat = isTimeFormat24 ? ld.timeFormat_Hm : ld.timeFormat_hm;

        if (mFormat == null) {
            mFormat = isTimeFormat24 ? M24 : M12;
        } else {
            mFormat = mFormat.replace(" ", "") ;
            mFormat = mFormat.replace("a", "") ;
        }

        mAmPm.setShowAmPm(!isTimeFormat24);
    }

    private Typeface getClockTypeface() {
        if (mClockTypeface == null) {
            mClockTypeface = Typeface.createFromFile(ANDROID_CLOCK_FONT_FILE);
        }
        return mClockTypeface;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(mAmPm != null){
	        mAmPm.updateAmPmText() ;
	        setDateFormat() ;
        }
    }
}