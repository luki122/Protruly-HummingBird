/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.deskclock.alarms;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextClock;
import android.widget.TextView;
import android.app.StatusBarManager;

import com.android.deskclock.AlarmClockFragment;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.SettingsActivity;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.worldclock.AlarmFrameLayout;

import java.lang.reflect.Method;

public class AlarmActivity2 extends Activity implements View.OnClickListener, AlarmFrameLayout.DismissAlarmListener {

    private static final String LOGTAG = AlarmActivity2.class.getSimpleName();

    public static final String DISABLE_PULLUP_QSPANEL = "disable_pullup_qspanel";
    public static final String ENABLE_PULLUP_QSPANEL = "enable_pullup_qspanel";

    /** Scheduled to update the timers while at least one is expired. */
    private final Runnable mTimeUpdateRunnable = new TimeUpdateRunnable();

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            LogUtils.v(LOGTAG, "Received broadcast: %s", action);

            if (!mAlarmHandled) {
                switch (action) {
                    case AlarmService.ALARM_SNOOZE_ACTION:
                        snooze();
                        break;
                    case AlarmService.ALARM_DISMISS_ACTION:
                        dismiss();
                        break;
                    case AlarmService.ALARM_DONE_ACTION:
                        finish();
                        break;
                    default:
                        LogUtils.i(LOGTAG, "Unknown broadcast: %s", action);
                        break;
                }
            } else {
                LogUtils.v(LOGTAG, "Ignored broadcast: %s", action);
            }
        }
    };

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogUtils.i("Finished binding to AlarmService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogUtils.i("Disconnected from AlarmService");
        }
    };

    private AlarmInstance mAlarmInstance;
    private boolean mAlarmHandled;
    private String mVolumeBehavior;
    private int mCurrentHourColor;
    private boolean mReceiverRegistered;
    /** Whether the AlarmService is currently bound */
    private boolean mServiceBound;
    private View mContentView;
    private TextView mSnoozeButton;
    private TextView mDismissButton;
    private TextView mTextLabel;

//    private TextView mTimeHourMinute;
    private TextView mTimeDateWeekday;

    private TextClock mTimeHourMinute;
    private AlarmFrameLayout mFrameLayout;

    private Context mContext;

//    public static boolean isAlarmBoot = false;

//    public static final String POWER_OFF_ALARM_MODE = "power_off_alarm_mode";
//    public static final int POWER_OFF_ALARM_MODE_ON = 1;
//    public static final int POWER_OFF_ALARM_MODE_OFF = 0;
//
//    private static final String ACTION_POWER_OFF_ALARM = "org.codeaurora.alarm.action.POWER_OFF_ALARM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final long instanceId = AlarmInstance.getId(getIntent().getData());

        mContext = getApplicationContext();

//        String intentAction = getIntent().getAction();
//        if (intentAction == ACTION_POWER_OFF_ALARM) {
//            setPowerOffAlarmMode(POWER_OFF_ALARM_MODE_ON, mContext);
//        }
//        isAlarmBoot = SystemProperties.getBoolean("ro.alarm_boot", false);

        mAlarmInstance = AlarmInstance.getInstance(getContentResolver(), instanceId);
        if (mAlarmInstance == null) {
            // The alarm was deleted before the activity got created, so just finish()
            LogUtils.e(LOGTAG, "Error displaying alarm for intent: %s", getIntent());
            finish();
            return;
        } else if (mAlarmInstance.mAlarmState != AlarmInstance.FIRED_STATE) {
            LogUtils.i(LOGTAG, "Skip displaying alarm for instance: %s", mAlarmInstance);
            finish();
            return;
        }

        LogUtils.i(LOGTAG, "Displaying alarm for instance: %s", mAlarmInstance);
        // Get the volume/camera button behavior setting
//        mVolumeBehavior = PreferenceManager.getDefaultSharedPreferences(this)
//                .getString(SettingsActivity.KEY_VOLUME_BEHAVIOR,
//                        SettingsActivity.DEFAULT_VOLUME_BEHAVIOR);
//        mVolumeBehavior = SettingsActivity.VOLUME_BEHAVIOR_DISMISS;
        mVolumeBehavior = SettingsActivity.VOLUME_BEHAVIOR_SNOOZE;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        ///M: Don't show the wallpaper when the alert arrive. @{
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        ///@}

        // Hide navigation bar to minimize accidental tap on Home key
        hideNavigationBar();

        // Close dialogs and window shade, so this is fully visible
        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        // In order to allow tablets to freely rotate and phones to stick
        // with "nosensor" (use default device orientation) we have to have
        // the manifest start with an orientation of unspecified" and only limit
        // to "nosensor" for phones. Otherwise we get behavior like in b/8728671
        // where tablets start off in their default orientation and then are
        // able to freely rotate.
        if (!getResources().getBoolean(R.bool.config_rotateAlarmAlert)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }
        init();
        setContentView(R.layout.alarm_activity_2);
        mContentView = getWindow().getDecorView();
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
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
        mContentView.setBackground(WallpaperManager.getInstance(this).getDrawable());

        mFrameLayout = (AlarmFrameLayout) findViewById(R.id.alarm_activity_2);
        mFrameLayout.setBackground(drawable);
        mFrameLayout.setOnDismissAlarmListener(this);


        mSnoozeButton = (TextView) mFrameLayout.findViewById(R.id.snooze);
        mDismissButton = (TextView) mFrameLayout.findViewById(R.id.dismiss);

        mSnoozeButton.setOnClickListener(this);
        mDismissButton.setOnClickListener(this);

//        mTimeHourMinute = (TextView) findViewById(R.id.time_hour_minute);
        mTimeHourMinute = (TextClock) findViewById(R.id.time_hour_minute);
        mTimeDateWeekday = (TextView) findViewById(R.id.time_date_weekday);

        mTimeHourMinute.setTypeface(
                Typeface.createFromAsset(getAssets(), "fonts/Roboto-Thin.ttf"));

        mTextLabel = (TextView) findViewById(R.id.label);
        mTextLabel.setText(mAlarmInstance.mLabel);

        StatusBarManager mStatusBarManager = (StatusBarManager) getSystemService(STATUS_BAR_SERVICE);
        mStatusBarManager.disable(StatusBarManager.DISABLE_EXPAND | StatusBarManager.DISABLE_HOME | StatusBarManager.DISABLE_RECENT);
        Intent intent = new Intent();
        intent.setAction(DISABLE_PULLUP_QSPANEL);
        sendBroadcast(intent);
    }

    @Override
    public void dismissAlarm() {
        dismiss();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Bind to AlarmService
        bindService(new Intent(this, AlarmService.class), mConnection, Context.BIND_AUTO_CREATE);
        mServiceBound = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Re-query for AlarmInstance in case the state has changed externally
        final long instanceId = AlarmInstance.getId(getIntent().getData());
        mAlarmInstance = AlarmInstance.getInstance(getContentResolver(), instanceId);

        if (mAlarmInstance == null) {
            LogUtils.i(LOGTAG, "No alarm instance for instanceId: %d", instanceId);
            finish();
            return;
        }

        // Verify that the alarm is still firing before showing the activity
        if (mAlarmInstance.mAlarmState != AlarmInstance.FIRED_STATE) {
            LogUtils.i(LOGTAG, "Skip displaying alarm for instance: %s", mAlarmInstance);
            finish();
            return;
        }

        if (!mReceiverRegistered) {
            // Register to get the alarm done/snooze/dismiss intent.
            final IntentFilter filter = new IntentFilter(AlarmService.ALARM_DONE_ACTION);
            filter.addAction(AlarmService.ALARM_SNOOZE_ACTION);
            filter.addAction(AlarmService.ALARM_DISMISS_ACTION);
            registerReceiver(mReceiver, filter);
            mReceiverRegistered = true;
        }

        startUpdatingTime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopUpdatingTime();
        unbindAlarmService();

        // Skip if register didn't happen to avoid IllegalArgumentException
        if (mReceiverRegistered) {
            unregisterReceiver(mReceiver);
            mReceiverRegistered = false;
        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent keyEvent) {
        // Do this in dispatch to intercept a few of the system keys.
        LogUtils.v(LOGTAG, "dispatchKeyEvent: %s", keyEvent);

        switch (keyEvent.getKeyCode()) {
            // Volume keys and camera keys dismiss the alarm.
            case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_FOCUS:
                if (!mAlarmHandled && keyEvent.getAction() == KeyEvent.ACTION_UP) {
                    switch (mVolumeBehavior) {
                        case SettingsActivity.VOLUME_BEHAVIOR_SNOOZE:
                            snooze();
                            break;
                        case SettingsActivity.VOLUME_BEHAVIOR_DISMISS:
                            dismiss();
                            break;
                        default:
                            break;
                    }
                }
                return true;
            default:
                return super.dispatchKeyEvent(keyEvent);
        }
    }

    @Override
    public void onBackPressed() {
        // Don't allow back to dismiss.
        snooze();
    }

    private void init() {
        //隐藏标题栏
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //把状态栏设置为透明的
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        /**
         * SYSTEM_UI_FLAG_LOW_PROFILE     隐藏虚拟键盘
         * SYSTEM_UI_FLAG_VISIBLE         导航栏显示
         * SYSTEM_UI_FLAG_HIDE_NAVIGATION 隐藏虚拟键盘-->但这对部分硬件设备有效
         */
        //隐藏虚拟按键栏方法一：
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE;
        getWindow().setAttributes(params);
        //把状态栏设置为透明的效果
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
//            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
//            window.getDecorView().setSystemUiVisibility(uiOptions);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
    }

    @Override
    public void onClick(View view) {
        if (mAlarmHandled) {
            LogUtils.v(LOGTAG, "onClick ignored: %s", view);
            return;
        }
        LogUtils.v(LOGTAG, "onClick: %s", view);

        if (view == mSnoozeButton) {
            snooze();
        } else if (view == mDismissButton) {
//            dismiss();
        }

    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void hideNavigationBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    /**
     * Perform snooze animation and send snooze intent.
     */
    private void snooze() {
        mAlarmHandled = true;
        LogUtils.v(LOGTAG, "Snoozed: %s", mAlarmInstance);

        // TODO: 17-5-19
        final int snoozeMinutes = AlarmStateManager.getSnoozedMinutes(this);

        //手动snooze之后次数清 0
        mAlarmInstance.mSnoozeTime = 0;
        AlarmStateManager.setSnoozeState(this, mAlarmInstance, false /* showToast */);

        Events.sendAlarmEvent(R.string.action_dismiss, R.string.label_deskclock);

        // Unbind here, otherwise alarm will keep ringing until activity finishes.
        unbindAlarmService();
        finish();
    }

    /**
     * Perform dismiss animation and send dismiss intent.
     */
    private void dismiss() {
        mAlarmHandled = true;
        LogUtils.v(LOGTAG, "Dismissed: %s", mAlarmInstance);

        AlarmStateManager.setDismissState(this, mAlarmInstance);

        Events.sendAlarmEvent(R.string.action_dismiss, R.string.label_deskclock);

        // Unbind here, otherwise alarm will keep ringing until activity finishes.
        unbindAlarmService();
        finish();
    }

    /**
     * Unbind AlarmService if bound.
     */
    private void unbindAlarmService() {
        if (mServiceBound) {
            unbindService(mConnection);
            mServiceBound = false;
        }
    }

    /**
     * Post the first runnable to update times within the UI. It will reschedule itself as needed.
     */
    private void startUpdatingTime() {
        // Ensure only one copy of the runnable is ever scheduled by first stopping updates.
        stopUpdatingTime();
//        mTimeHourMinute.post(mTimeUpdateRunnable);
        mTimeDateWeekday.post(mTimeUpdateRunnable);
    }

    /**
     * Remove the runnable that updates times within the UI.
     */
    private void stopUpdatingTime() {
//        mTimeHourMinute.removeCallbacks(mTimeUpdateRunnable);
        mTimeDateWeekday.removeCallbacks(mTimeUpdateRunnable);
    }

    private void updateTime() {
        long now = System.currentTimeMillis();
        String hourMinute = DateUtils.formatDateTime(this, now, DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_24HOUR);
        String dateWeekday = DateUtils.formatDateTime(this, now, DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_WEEKDAY);

//        mTimeHourMinute.setText(hourMinute);
        mTimeDateWeekday.setText(dateWeekday);
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
            mTimeDateWeekday.postDelayed(this, delay);
        }
    }

    @Override
    protected void onDestroy() {
        StatusBarManager mStatusBarManager = (StatusBarManager) getSystemService(STATUS_BAR_SERVICE);
        mStatusBarManager.disable(StatusBarManager.DISABLE_NONE);
        Intent intent = new Intent();
        intent.setAction(ENABLE_PULLUP_QSPANEL);
        sendBroadcast(intent);
        super.onDestroy();
    }

}
