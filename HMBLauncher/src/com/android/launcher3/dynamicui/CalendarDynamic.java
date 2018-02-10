package com.android.launcher3.dynamicui;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.FastBitmapDrawable;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.appwidget.LauncherClock;
import com.android.launcher3.theme.IconProvider;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by liuzuo on 17-5-22.
 */

public class CalendarDynamic implements IDynamicIcon {
    private Context mContext;
    private Time mCalendar;

    private int mDateColor;
    private int mWeekColor;
    private int mDateTextSize;
    private int mWeekTextSize;
    private int mHours=12;
    private Drawable mBackground;
    private boolean mIsRegister;
    private int mIconSize;
    private final String TAG = "CalendarDynamic";
    private  String WEEKFORMAT ;
    private final String CALENDAR_BG_PATH = "dym_calendar_bg";
    private final String DYM_CALENDAR_DAY_TEXT = "com.android.launcher3$dym_calendar_day_text";
    private final String DYM_CALENDAR_WEEK_TEXT = "com.android.launcher3$dym_calendar_week_text";

    private BubbleTextView mView;

    @Override
    public boolean init(Context context, BubbleTextView bubbleTextView, ItemInfo info) {
        final Resources r = context.getResources();
        mCalendar = new Time();
        mContext = context;
        mView = bubbleTextView;
        if (mView != null) {
            mIconSize = mView.getIconSize();
        }
        IconProvider provider = LauncherAppState.getInstance().getIconCache().getIconProvider();

        mDateColor = provider.getColor(DYM_CALENDAR_DAY_TEXT , R.color.dym_calender_date_text_color , r );/* r.getColor(R.color.dym_calender_date_text_color);*/
        mWeekColor = provider.getColor(DYM_CALENDAR_WEEK_TEXT , R.color.dym_calender_week_text_color ,r);/*r.getColor(R.color.dym_calender_week_text_color);*/
        mBackground =  provider.getIconFromManager(context, CALENDAR_BG_PATH, R.drawable.dym_calendar_bg);
        WEEKFORMAT = r.getString(R.string.dym_calendar_week_format);
        mDateTextSize = (int) r.getDimension(R.dimen.dym_calender_date_text_size);
        mWeekTextSize = (int) r.getDimension(R.dimen.dym_calender_week_text_size);
        updateDynamicIcon(true);
        return true;
    }

    @Override
    public void removeDynamicReceiver() {
        mIntentReceiver.setUpdate(false);
    }

    @Override
    public boolean updateDynamicIcon(boolean register) {
        mIntentReceiver.setUpdate(true);
        onTimeChanged(false);
        if (register) {
            registerReceiver();
        }
        return true;
    }

    @Override
    public void clearDynamicIcon() {
        unregisterReceiver();
    }

    private void onTimeChanged(boolean isFromBroadcast) {
        mCalendar.setToNow();
        updateLauncherClock();
        updateCalendarIcon(isFromBroadcast);
        updateContentDescription(mCalendar);
    }

    private void updateContentDescription(Time time) {
        final int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR;
        String contentDescription = DateUtils.formatDateTime(mContext,
                time.toMillis(false), flags);
        mView.setContentDescription(contentDescription);
    }

    private void updateCalendarIcon(boolean isFromBroadcast) {
        if (mIconSize == 0||mBackground==null)
            return;
        Bitmap bitmap = Bitmap.createBitmap(mIconSize, mIconSize, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        int w = mBackground.getIntrinsicWidth();
        int h = mBackground.getIntrinsicHeight();
        int x = mIconSize / 2;
        int y = mIconSize / 2;

        if ((mIconSize < w) || (mIconSize < h)) {
            float scale = Math.min((float) mIconSize / (float) w,
                    (float) mIconSize / (float) h);
            canvas.save();

            canvas.scale(scale, scale, x, y);
        }


        mBackground.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        SimpleDateFormat format = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
        format.applyPattern(WEEKFORMAT);
        String weekString = format.format(new Date());
        mBackground.draw(canvas);
        String dayString = String.valueOf(mCalendar.monthDay);
        //final float mDensity = mContext.getResources().getDisplayMetrics().density;

        Rect dateRect = new Rect();
        Rect weekRect = new Rect();

        Paint mDatePaint = new Paint();
        mDatePaint.setAntiAlias(true);
        mDatePaint.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        mDatePaint.setTextSize(mDateTextSize);
        mDatePaint.setFakeBoldText(false);
        mDatePaint.setColor(mWeekColor);
        mDatePaint.setDither(false);
        //mDatePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        mDatePaint.getTextBounds(dayString, 0, dayString.length(), dateRect);
        mDatePaint.setTextSize(mWeekTextSize);
        mDatePaint.getTextBounds(weekString, 0, weekString.length(), weekRect);


        int gap = 15;
        int weekWidth = weekRect.right - weekRect.left;
        int weekHeight = weekRect.bottom - weekRect.top;

        int dateWidth = dateRect.right - dateRect.left;
        int dateHeight = dateRect.bottom - dateRect.top;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int offsetX = 0;
        int top =  (height-weekHeight-dateHeight-gap)/2;
        if (mCalendar.monthDay % 10 == 1) {
            offsetX = 4;
        }
        canvas.drawText(weekString, (width - weekWidth) / 2 - weekRect.left, top-weekRect.top, mDatePaint);

        mDatePaint.setTextSize(mDateTextSize);
        mDatePaint.setColor(mDateColor);
        canvas.drawText(dayString, (width - dateWidth) / 2 - dateRect.left - offsetX, top+weekHeight+gap-dateRect.top, mDatePaint);
        if ((mIconSize < w) || (mIconSize < h)) {
            float scale = Math.min((float) mIconSize / (float) w,
                    (float) mIconSize / (float) h);
            canvas.save();

            //canvas.scale(scale, scale, x, y);
        }
        //Bitmap scalebmp = LauncherAppState.getInstance().getIconCache().getIconProvider().normalizeIcons(bitmap);
        Log.d(TAG, "updateCalendarIcon=" + dayString + "   mCalendar=" + mCalendar.monthDay + "  weekRect.top=" + weekRect.top + "  dateRect.top=" + dateRect.top);
        if (mView != null) {
            mView.setIcon(new FastBitmapDrawable(bitmap));
            mView.updateFolderIcon();
        }
    }


    private synchronized void registerReceiver() {
        if (!mIsRegister) {
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            if (mContext != null) {
                mContext.registerReceiver(mIntentReceiver, filter);
            }
            mIsRegister = true;
            Log.d(TAG, " registerReceiver");
        }
    }
    private synchronized void unregisterReceiver() {
        try {
            if (mIsRegister) {
                mContext.unregisterReceiver(mIntentReceiver);
                mIsRegister = false;
            }
            Log.e(TAG, "mIntentReceiver is unregistered");
        } catch (Exception e) {
            Log.e(TAG, "unregistered failed");
        }
    }
    private final DymBroadcastReceiver mIntentReceiver = new DymBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "deskClock onReceive");
            if(mIntentReceiver.isUpdate()) {
                updateDynamicIcon(false);
            }else {
                mCalendar.setToNow();
                updateLauncherClock();
            }
        }
    };
    private void updateLauncherClock(){
        if(mHours!=mCalendar.hour){
            if(mContext!=null&&!DateFormat.is24HourFormat(mContext)&&mCalendar.hour==12){
                Log.d("LauncherClock","sendBroadcast LauncherClock.UPDATECLOCK");
                mContext.sendBroadcast(new Intent((LauncherClock.UPDATECLOCK)));
            }
        }
        mHours = mCalendar.hour;
    }
}
