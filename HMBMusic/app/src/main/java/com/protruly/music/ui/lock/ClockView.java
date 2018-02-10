package com.protruly.music.ui.lock;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.os.Handler;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.protruly.music.R;
import com.protruly.music.util.LogUtil;
import java.lang.ref.WeakReference;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by wxue on 17-9-18.
 */
public class ClockView extends LinearLayout {
    private static final String TAG = "ClockView" ;
    private static Typeface mClockTypeface = null;
    private final static String M12 = "h:mm";
    private final static String M24 = "kk:mm";

    private Calendar mCalendar;
    private String mFormat;
    private TextView mTimeView;
    private AmPm mAmPm;
    private int mAttached = 0;
    private Context mContext;
    private static final String ANDROID_CLOCK_FONT_FILE = "/system/fonts/AndroidClock.ttf";
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
                            LogUtil.d(TAG, "need to update timezone.");
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
                    LogUtil.e(TAG, "onReceive() - unregisterReceiver(this) fails") ;
                }
            }
        }
    };

    static class AmPm {
        private TextView mAmPmTextView;
        private String mAmString, mPmString;

        AmPm(View parent, Typeface tf) {
            mAmPmTextView = (TextView) parent.findViewById(R.id.txt_am_pm);

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

        void setIsMorning(boolean isMorning) {
            if (mAmPmTextView != null) {
                mAmPmTextView.setText(isMorning ? mAmString : mPmString);
            }
        }

        void updateAmPmText() {
            String[] ampm = new DateFormatSymbols().getAmPmStrings();
            mAmString = ampm[0];
            mPmString = ampm[1];
            LogUtil.d(TAG, "mAmString = " + mAmString + " mPmString = " + mPmString) ;
        }
    }

    public ClockView(Context context) {
        this(context, null);
    }

    public ClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTimeView = (TextView) findViewById(R.id.txt_clock);
       
        mTimeView.setTypeface(getClockTypeface());
        mAmPm = new AmPm(this, null);
        mCalendar = Calendar.getInstance();
        setDateFormat();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        LogUtil.d(TAG, "onAttachedToWindow()");
        mAttached++;

        /* monitor time ticks, time changed, timezone */
        if (mIntentReceiver == null) {
            mIntentReceiver = new TimeChangedReceiver(this);
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            mContext.registerReceiver(mIntentReceiver,filter);
        }

        mCalendar = Calendar.getInstance(TimeZone.getDefault());
        LogUtil.d(TAG, "set default timezone, timezone = " + TimeZone.getDefault());

        updateTime();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttached--;
        if (mIntentReceiver != null) {
            mContext.unregisterReceiver(mIntentReceiver);
        }
        mIntentReceiver = null;
    }

    public void updateTime() {
        setDateFormat();
        mCalendar.setTimeInMillis(System.currentTimeMillis());

        CharSequence newTime = DateFormat.format(mFormat, mCalendar);
        mTimeView.setText(newTime);
        mAmPm.setIsMorning(mCalendar.get(Calendar.AM_PM) == 0);
    }

    private void setDateFormat() {
        boolean isTimeFormat24 = DateFormat.is24HourFormat(getContext()) ;
        mFormat = isTimeFormat24 ? M24 : M12;
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
        LogUtil.d(TAG,"---onConfigurationChanged()--");
        mAmPm.updateAmPmText() ;
        setDateFormat() ;
    }
}
