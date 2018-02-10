package com.android.launcher3.dynamicui;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.drawable.Drawable;
import android.os.Handler;
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

/**
 * Created by liuzuo on 17-5-22.
 */

public class DeskClockDynamic implements IDynamicIcon {

    private Drawable mHourHand;
    private Drawable mMinuteHand;
    private Drawable mSecondHand;
    private Drawable mDial;

    private final String MINUTES_PATH = "dym_clock_hand_minute";
    private final String HOUR_PATH = "dym_clock_hand_hour";
    private final String SECOND_PATH = "dym_clock_hand_second";
    private final String DIAL_PATH = "dym_clock_dial";

    private final String TAG = "DeskClockDynamic";
    PaintFlagsDrawFilter mPaintFlagsDrawFilter = new PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG);
    private Context mContext;
    private Time mCalendar;
    private boolean mIsRegister;
    private boolean mChanged = true;
    public static boolean hasSecondHand = true;
    private final int REFRESH_SECOND_INTERVAL = 1000;

    private final Handler mHandler = new Handler();

    private float mMinutes;
    private float mHour;
    private float mSeconds;

    private BubbleTextView mView;

    @Override
    public boolean init(Context context, BubbleTextView bubbleTextView, ItemInfo info) {
        final Resources r = context.getResources();
        mContext = context;
        IconProvider manager = LauncherAppState.getInstance().getIconCache().getIconProvider();

        mCalendar = new Time();

        mView = bubbleTextView;
        mDial = manager.getIconFromManager(r, DIAL_PATH, R.drawable.dym_clock_dial);
        mHourHand = manager.getIconFromManager(r, HOUR_PATH, R.drawable.dym_clock_hand_hour);
        mMinuteHand = manager.getIconFromManager(r, MINUTES_PATH, R.drawable.dym_clock_hand_minute);
        mSecondHand = manager.getIconFromManager(r, SECOND_PATH, R.drawable.dym_clock_hand_second);

        updateDynamicIcon(true);
        if (hasSecondHand) {
            secondHandRun();
        }
        return true;
    }


    @Override
    public void removeDynamicReceiver() {
/*        try {
            if (mIsRegister) {
                mContext.unregisterReceiver(mIntentReceiver);
                mIsRegister = false;
            }
            Log.e(TAG, "mIntentReceiver is unregistered");
        } catch (Exception e) {
            Log.e(TAG, "unregistered failed");
        }*/
        if (hasSecondHand) {
            mHandler.removeCallbacks(mRunnable);
        }

        mIntentReceiver.setUpdate(false);
    }

    @Override
    public boolean updateDynamicIcon(boolean register) {
        mIntentReceiver.setUpdate(true);
        onTimeChanged(true);
        if (hasSecondHand) {
            secondHandRun();
        }
        if (register) {
            registerReceiver();
        }

        return true;
    }

    private void onTimeChanged(boolean force) {
        int day = mCalendar.yearDay;

        mCalendar.setToNow();
        if(day!=mCalendar.yearDay&&mContext!=null){
            Intent intent = new Intent(LauncherClock.UPDATECLOCK);
            mContext.sendBroadcast(intent);
        }

        float hour = mCalendar.hour + mCalendar.minute / 60.0f;
        float minute = mCalendar.minute;
        float second = mCalendar.second;
       if (force||(mSeconds != second || mMinutes != minute || mHour != hour)) {
            mSeconds = second;
            mMinutes = minute;
            mHour = hour;
            updateClockIcon();
           //Log.d(TAG,"onTimeChanged");
        }
        updateContentDescription(mCalendar);
    }

    private void updateContentDescription(Time time) {
        final int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR;
        String contentDescription = DateUtils.formatDateTime(mContext,
                time.toMillis(false), flags);
        mView.setContentDescription(contentDescription);
    }

    private void updateClockIcon() {
        if(mDial==null)
            return;
        Bitmap clock = Bitmap.createBitmap(mDial.getIntrinsicWidth(), mDial.getIntrinsicHeight(), Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(clock);
        canvas.setDrawFilter(mPaintFlagsDrawFilter);
        boolean changed = mChanged;
/*        if (changed) {
            mChanged = false;
        }*/
        int availableWidth = mDial.getIntrinsicWidth();
        int availableHeight = mDial.getIntrinsicHeight();

        int x = availableWidth / 2;
        int y = availableHeight / 2;

        final Drawable dial = mDial;
        int w = dial.getIntrinsicWidth();
        int h = dial.getIntrinsicHeight();

        boolean scaled = false;

        if (availableWidth < w || availableHeight < h) {
            scaled = true;
            float scale = Math.min((float) availableWidth / (float) w,
                    (float) availableHeight / (float) h);
            canvas.save();

            canvas.scale(scale, scale, x, y);
        }

        if (changed) {
            dial.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        dial.draw(canvas);

        canvas.save();
        canvas.rotate(mHour / 12.0f * 360.0f, x, y);
        final Drawable hourHand = mHourHand;
        if (changed) {
            w = hourHand.getIntrinsicWidth();
            h = hourHand.getIntrinsicHeight();
            hourHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        hourHand.draw(canvas);
        canvas.restore();

        canvas.save();
        canvas.rotate(mMinutes / 60.0f * 360.0f, x, y);

        final Drawable minuteHand = mMinuteHand;
        if (changed) {
            w = minuteHand.getIntrinsicWidth();
            h = minuteHand.getIntrinsicHeight();
            minuteHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        minuteHand.draw(canvas);

        if (hasSecondHand) {
            canvas.restore();

            canvas.save();

            canvas.rotate(mSeconds / 60.0f * 360.0f, x, y);
            final Drawable secondHand = mSecondHand;
            w = secondHand.getIntrinsicWidth();
            h = secondHand.getIntrinsicHeight();
            secondHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
            secondHand.draw(canvas);
        }
        canvas.restore();
        //Bitmap scalebmp = LauncherAppState.getInstance().getIconCache().getIconProvider().normalizeIcons(clock);
        if (mView != null) {
            mView.setIcon(new FastBitmapDrawable(clock));
            //mView.updateFolderIcon();
        }
    }


    @Override
    public void clearDynamicIcon() {
        unregisterReceiver();
        removeDynamicReceiver();
    }

    private synchronized void registerReceiver() {
        if (!mIsRegister) {
            IntentFilter filter = new IntentFilter();
            if(!hasSecondHand) {
                filter.addAction(Intent.ACTION_TIME_TICK);
            }
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            if (mContext != null) {
                mContext.registerReceiver(mIntentReceiver, filter);
            }
            mIsRegister = true;
            //Log.d(TAG, "deskClock registerReceiver");
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
            if (mIntentReceiver.isUpdate()) {
                updateDynamicIcon(false);
            }
        }
    };
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            onTimeChanged(false);
            mHandler.postDelayed(mRunnable, REFRESH_SECOND_INTERVAL);
        }
    };

    private void secondHandRun() {
        mHandler.removeCallbacks(mRunnable);
        mHandler.post(mRunnable);
    }
}
