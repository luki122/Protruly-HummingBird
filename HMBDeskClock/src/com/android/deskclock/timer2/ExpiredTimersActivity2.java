/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.deskclock.timer2;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.app.StatusBarManager;

import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Timer;
import com.android.deskclock.data.TimerListener;

import java.util.List;

/**
 * This activity is designed to be shown over the lock screen. As such, it displays the expired
 * timers and a single button to reset them all. Each expired timer can also be reset to one minute
 * with a button in the user interface. All other timer operations are disabled in this activity.
 */
public class ExpiredTimersActivity2 extends Activity {
    public static final String DISABLE_PULLUP_QSPANEL = "disable_pullup_qspanel";
    public static final String ENABLE_PULLUP_QSPANEL = "enable_pullup_qspanel";

    /** Scheduled to update the timers while at least one is expired. */
    private final Runnable mTimeUpdateRunnable = new TimeUpdateRunnable();

    /** Updates the timers displayed in this activity as the backing data changes. */
    private final TimerListener mTimerChangeWatcher = new TimerChangeWatcher();

    private TextView mTimeHourMinute;
    private TextView mTimeDateWeekday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.expired_timers_activity2);

        mTimeHourMinute = (TextView) findViewById(R.id.time_hour_minute);
        mTimeDateWeekday = (TextView) findViewById(R.id.time_date_weekday);

        mTimeHourMinute.setTypeface(
                Typeface.createFromAsset(getAssets(), "fonts/Roboto-Thin.ttf"));

        findViewById(R.id.timer_finish).setOnClickListener(new FabClickListener());

        final View view = findViewById(R.id.expired_timers_activity);
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        WallpaperManager.getInstance(this).forgetLoadedWallpaper();
        Drawable drawable = WallpaperManager.getInstance(this).getLockscreenDrawable();
        if (drawable == null) {
            drawable = WallpaperManager.getInstance(this).getDrawable();
        }
        getWindow().getDecorView().setBackground(drawable);

        getWindow().setStatusBarColor(Color.TRANSPARENT);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        // Update views in response to timer data changes.
        DataModel.getDataModel().addTimerListener(mTimerChangeWatcher);
        StatusBarManager mStatusBarManager = (StatusBarManager) getSystemService(STATUS_BAR_SERVICE);
        mStatusBarManager.disable(StatusBarManager.DISABLE_EXPAND | StatusBarManager.DISABLE_HOME | StatusBarManager.DISABLE_RECENT);
        Intent intent = new Intent();
        intent.setAction(DISABLE_PULLUP_QSPANEL);
        sendBroadcast(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startUpdatingTime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopUpdatingTime();
    }

    @Override
    public void onDestroy() {
        StatusBarManager mStatusBarManager = (StatusBarManager) getSystemService(STATUS_BAR_SERVICE);
        mStatusBarManager.disable(StatusBarManager.DISABLE_NONE);
        Intent intent = new Intent();
        intent.setAction(ENABLE_PULLUP_QSPANEL);
        sendBroadcast(intent);
        super.onDestroy();
        DataModel.getDataModel().removeTimerListener(mTimerChangeWatcher);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_FOCUS:
                DataModel.getDataModel().resetExpiredTimers(R.string.label_hardware_button);
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /*@Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                case KeyEvent.KEYCODE_VOLUME_MUTE:
                case KeyEvent.KEYCODE_CAMERA:
                case KeyEvent.KEYCODE_FOCUS:
                    DataModel.getDataModel().resetExpiredTimers(R.string.label_hardware_button);
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }*/

    /**
     * Post the first runnable to update times within the UI. It will reschedule itself as needed.
     */
    private void startUpdatingTime() {
        // Ensure only one copy of the runnable is ever scheduled by first stopping updates.
        stopUpdatingTime();
        mTimeHourMinute.post(mTimeUpdateRunnable);
    }

    /**
     * Remove the runnable that updates times within the UI.
     */
    private void stopUpdatingTime() {
        mTimeHourMinute.removeCallbacks(mTimeUpdateRunnable);
    }

    private void updateTime() {
        long now = System.currentTimeMillis();
        String hourMinute = DateUtils.formatDateTime(this, now, DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_24HOUR);
        String dateWeekday = DateUtils.formatDateTime(this, now, DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_WEEKDAY);

        mTimeHourMinute.setText(hourMinute);
        mTimeDateWeekday.setText(dateWeekday);
    }

    /**
     * Remove an existing view that corresponds with the given {@code timer}.
     */
    private void removeTimer(Timer timer) {
        // If the second last timer was just removed, center the last timer.
        final List<Timer> expiredTimers = getExpiredTimers();
        if (expiredTimers.isEmpty()) {
            finish();
        }
    }

    private List<Timer> getExpiredTimers() {
        return DataModel.getDataModel().getExpiredTimers();
    }

    /**
     * Periodically refreshes the state of each timer.
     */
    private class TimeUpdateRunnable implements Runnable {
        @Override
        public void run() {
            final long startTime = SystemClock.elapsedRealtime();

            updateTime();

            final long endTime = SystemClock.elapsedRealtime();

            // Try to maintain a consistent period of time between redraws.
            final long delay = Math.max(0, startTime + 500 - endTime);
            mTimeHourMinute.postDelayed(this, delay);
        }
    }

    /**
     * Clicking the fab resets all expired timers.
     */
    private class FabClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            DataModel.getDataModel().removeTimerListener(mTimerChangeWatcher);
            DataModel.getDataModel().resetExpiredTimers(R.string.label_deskclock);
            finish();
        }
    }

//    @Override
//    public void onBackPressed() {
//        DataModel.getDataModel().removeTimerListener(mTimerChangeWatcher);
//        DataModel.getDataModel().resetExpiredTimers(R.string.label_deskclock);
//        finish();
//    }

    /**
     * Adds and removes expired timers from this activity based on their state changes.
     */
    private class TimerChangeWatcher implements TimerListener {
        @Override
        public void timerAdded(Timer timer) {

        }

        @Override
        public void timerUpdated(Timer before, Timer after) {
            if (before.isExpired() && !after.isExpired()) {
                removeTimer(before);
            }
        }

        @Override
        public void timerRemoved(Timer timer) {
            if (timer.isExpired()) {
                removeTimer(timer);
            }
        }
    }
}