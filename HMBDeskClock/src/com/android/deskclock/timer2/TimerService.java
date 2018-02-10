/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.deskclock.timer2;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.android.deskclock.R;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Timer;
import com.android.deskclock.events.Events;

/**
 * <p>This service exists solely to allow {@link android.app.AlarmManager} and timer notifications
 * to alter the state of timers without disturbing the notification shade. If an activity were used
 * instead (even one that is not displayed) the notification manager implicitly closes the
 * notification shade which clashes with the use case of starting/pausing/resetting timers without
 * disturbing the notification shade.</p>
 *
 * <p>The service has a second benefit. It is used to start heads-up notifications for expired
 * timers in the foreground. This keeps the entire application in the foreground and thus prevents
 * the operating system from killing it while expired timers are firing.</p>
 */
public final class TimerService extends Service {

    private static final String ACTION_PREFIX = "com.android.deskclock.action.";

    private static final String ACTION_LISTEN_PHONE_STATE = ACTION_PREFIX + "LISTEN_PHONE_STATE";
    private static final String ACTION_SHUTDOWN = ACTION_PREFIX + "SHUTDOWN";
    private static final String ACTION_BOOT_COMPLETED = ACTION_PREFIX + "BOOT_COMPLETED";
    private static final String ACTION_RESET_EXPIRED_TIMER = ACTION_PREFIX + "RESET_EXPIRED_TIMER";

    private static final String ACTION_TIMER_EXPIRED = ACTION_PREFIX + "TIMER_EXPIRED";
    private static final String ACTION_UPDATE_NOTIFICATION = ACTION_PREFIX + "UPDATE_NOTIFICATION";
    private static final String ACTION_RESET_EXPIRED_TIMERS = ACTION_PREFIX +
            "RESET_EXPIRED_TIMERS";
    private static final String ACTION_RESET_UNEXPIRED_TIMERS = ACTION_PREFIX +
            "RESET_UNEXPIRED_TIMERS";

    public static Intent createListenPhoneStateIntent(Context context) {
        return new Intent(context, TimerService.class)
                .setAction(ACTION_LISTEN_PHONE_STATE);
    }

    public static Intent createShutdownIntent(Context context) {
        return new Intent(context, TimerService.class)
                .setAction(ACTION_SHUTDOWN);
    }

    public static Intent createBootCompletedIntent(Context context) {
        return new Intent(context, TimerService.class)
                .setAction(ACTION_BOOT_COMPLETED);
    }

    public static Intent createResetExpiredTimerIntent(Context context) {
        return new Intent(context, TimerService.class)
                .setAction(ACTION_RESET_EXPIRED_TIMER);
    }

    public static Intent createTimerExpiredIntent(Context context, Timer timer) {
        final int timerId = timer == null ? -1 : timer.getId();
        return new Intent(context, TimerService.class)
                .setAction(ACTION_TIMER_EXPIRED)
                .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timerId);
    }

    public static Intent createResetExpiredTimersIntent(Context context) {
        return new Intent(context, TimerService.class)
                .setAction(ACTION_RESET_EXPIRED_TIMERS);
    }

    public static Intent createResetUnexpiredTimersIntent(Context context) {
        return new Intent(context, TimerService.class)
                .setAction(ACTION_RESET_UNEXPIRED_TIMERS);
    }

    public static Intent createAddMinuteTimerIntent(Context context, int timerId) {
        return new Intent(context, TimerService.class)
                .setAction(HandleDeskClockApiCalls.ACTION_ADD_MINUTE_TIMER)
                .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timerId);
    }

    public static Intent createUpdateNotificationIntent(Context context) {
        return new Intent(context, TimerService.class)
                .setAction(ACTION_UPDATE_NOTIFICATION);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            switch (intent.getAction()) {
                case ACTION_LISTEN_PHONE_STATE: {
                    startListenPhoneState();
                    return START_NOT_STICKY;
                }
                case ACTION_SHUTDOWN: {
                    DataModel.getDataModel().shutdown();
                    return START_NOT_STICKY;
                }
                case ACTION_BOOT_COMPLETED: {
                    DataModel.getDataModel().bootCompleted(this);
                    return START_NOT_STICKY;
                }
                case ACTION_RESET_EXPIRED_TIMER: {
                    DataModel.getDataModel().resetExpiredTimer();
                    return START_NOT_STICKY;
                }
                case ACTION_UPDATE_NOTIFICATION: {
                    DataModel.getDataModel().updateTimerNotification();
                    return START_NOT_STICKY;
                }
                case ACTION_RESET_EXPIRED_TIMERS: {
                    DataModel.getDataModel().resetExpiredTimers(R.string.label_notification);
                    return START_NOT_STICKY;
                }
                case ACTION_RESET_UNEXPIRED_TIMERS: {
                    DataModel.getDataModel().resetUnexpiredTimers(R.string.label_notification);
                    return START_NOT_STICKY;
                }
            }

            // Look up the timer in question.
            final int timerId = intent.getIntExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, -1);
            final Timer timer = DataModel.getDataModel().getTimer(timerId);

            // If the timer cannot be located, ignore the action.
            if (timer == null) {
                return START_NOT_STICKY;
            }

            // Perform the action on the timer.
            switch (intent.getAction()) {
                case HandleDeskClockApiCalls.ACTION_START_TIMER:
                    DataModel.getDataModel().startTimer(timer);
                    Events.sendTimerEvent(R.string.action_start, R.string.label_notification);
                    break;
                case HandleDeskClockApiCalls.ACTION_PAUSE_TIMER:
                    DataModel.getDataModel().pauseTimer(timer);
                    Events.sendTimerEvent(R.string.action_pause, R.string.label_notification);
                    break;
                /// M: Added handling for Add minute action for unepired timer.
                case HandleDeskClockApiCalls.ACTION_ADD_MINUTE_TIMER_UNEXPIRED:
                case HandleDeskClockApiCalls.ACTION_ADD_MINUTE_TIMER:
                    DataModel.getDataModel().addTimerMinute(timer);
                    Events.sendTimerEvent(R.string.action_add_minute, R.string.label_notification);
                    break;
                case HandleDeskClockApiCalls.ACTION_RESET_TIMER:
                    DataModel.getDataModel().resetOrDeleteTimer(timer, R.string.label_notification);
                    break;
                case HandleDeskClockApiCalls.ACTION_REMOVE_TIMER:
                    DataModel.getDataModel().removeTimer(timer);
                    break;
                case ACTION_TIMER_EXPIRED:
                    DataModel.getDataModel().expireTimer(this, timer);
                    Events.sendTimerEvent(R.string.action_fire, R.string.label_intent);
                    break;
            }
        } finally {
            // This service is foreground when expired timers exist and stopped when none exist.
            if (DataModel.getDataModel().getExpiredTimers().isEmpty()) {
                stopSelf();
            }
        }

        return START_NOT_STICKY;
    }

    private TelephonyManager mTelephonyManager;
    private int mInitialCallState;

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String ignored) {
            if (state == mInitialCallState) return;

            if (state != TelephonyManager.CALL_STATE_IDLE) {
                DataModel.getDataModel().stopRinger();
            } else {
                DataModel.getDataModel().startRinger();
            }
            mInitialCallState = state;
        }
    };

    @Override
    public void onCreate() {
        mTelephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public void onDestroy() {
        stopListenPhoneState();
    }

    private void startListenPhoneState() {
        mInitialCallState = mTelephonyManager.getCallState();
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void stopListenPhoneState() {
        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_NONE);
    }
}