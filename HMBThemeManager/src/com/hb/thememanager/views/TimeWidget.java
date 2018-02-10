package com.hb.thememanager.views;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.UserHandle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.TypedValue;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.internal.widget.LockPatternUtils;
import com.hb.thememanager.R;

import java.util.Locale;

public class TimeWidget extends LinearLayout {
    private static final String TAG = "TimeWidget";

    private final AlarmManager mAlarmManager;

    private TextClock mDateView;

    private ClockView mClockView;



    public TimeWidget(Context context) {
        this(context, null, 0);
    }

    public TimeWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimeWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }


    @Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		LinearLayout mtkClockLayout = (LinearLayout) findViewById(R.id.clock_container_parent);
		mClockView = (ClockView) findViewById(R.id.clock_view);
		mDateView = (TextClock) findViewById(R.id.date_view);


		refresh();
	}
    
    public void showDateWidget(boolean show){
    	mDateView.setVisibility(show?View.VISIBLE:View.GONE);
    }
    
    public void showAmPm(boolean show){
    	mClockView.showAmPmIfNeeded(show);
    }

    public void updateWidgetColor(int color){
    	mDateView.setTextColor(color);
    	mClockView.setTextColor(color);
    }
    
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(mDateView != null){
	        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
	                getResources().getDimensionPixelSize(R.dimen.widget_label_font_size));
        }
    }

    public void refreshTime() {
        mDateView.setFormat24Hour(Patterns.dateView);
        mDateView.setFormat12Hour(Patterns.dateView);

            if (null != mClockView) {
                mClockView.updateTime();
            }
    }

    private void refresh() {
    	AlarmManager.AlarmClockInfo nextAlarm =
                mAlarmManager.getNextAlarmClock(UserHandle.USER_CURRENT);
        Patterns.update(mContext, nextAlarm != null);
        refreshTime();
    }




    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }


    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    // DateFormat.getBestDateTimePattern is extremely expensive, and refresh is called often.
    // This is an optimization to ensure we only recompute the patterns when the inputs change.
    private static final class Patterns {
        static String dateView;
        static String clockView12;
        static String clockView24;
        static String cacheKey;

        static void update(Context context, boolean hasAlarm) {
            final Locale locale = Locale.getDefault();
            final Resources res = context.getResources();
            final String dateViewSkel = res.getString(hasAlarm
                    ? R.string.abbrev_wday_month_day_no_year_alarm
                    : R.string.abbrev_wday_month_day_no_year);
            final String clockView12Skel = res.getString(R.string.clock_12hr_format);
            final String clockView24Skel = res.getString(R.string.clock_24hr_format);
            final String key = locale.toString() + dateViewSkel + clockView12Skel + clockView24Skel;
            if (key.equals(cacheKey)) return;

            dateView = DateFormat.getBestDateTimePattern(locale, dateViewSkel);

            clockView12 = DateFormat.getBestDateTimePattern(locale, clockView12Skel);
            // CLDR insists on adding an AM/PM indicator even though it wasn't in the skeleton
            // format.  The following code removes the AM/PM indicator if we didn't want it.
            if (!clockView12Skel.contains("a")) {
                clockView12 = clockView12.replaceAll("a", "").trim();
            }

            clockView24 = DateFormat.getBestDateTimePattern(locale, clockView24Skel);

            // Use fancy colon.
            clockView24 = clockView24.replace(':', '\uee01');
            clockView12 = clockView12.replace(':', '\uee01');

            cacheKey = key;
        }
    }
}

