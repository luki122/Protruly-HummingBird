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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;

import com.android.deskclock.DeskClock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Voice;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Timer;
import com.android.deskclock.events.Events;

import java.util.List;

public class HandleDeskClockApiCalls extends Activity {
    private Context mAppContext;

    private static final String ACTION_PREFIX = "com.android.deskclock.action.";

    // shows the tab with timers; optionally scrolls to a specific timer
    public static final String ACTION_SHOW_TIMERS = ACTION_PREFIX + "SHOW_TIMERS";
    // pauses running timers; resets expired timers
    public static final String ACTION_PAUSE_TIMER = ACTION_PREFIX + "PAUSE_TIMER";
    // starts the sole timer
    public static final String ACTION_START_TIMER = ACTION_PREFIX + "START_TIMER";
    // resets the timer
    public static final String ACTION_RESET_TIMER = ACTION_PREFIX + "RESET_TIMER";
    // removes the timer
    public static final String ACTION_REMOVE_TIMER = ACTION_PREFIX + "REMOVE_TIMER";
    // adds an extra minute to the expired timer
    public static final String ACTION_ADD_MINUTE_TIMER = ACTION_PREFIX + "ADD_MINUTE_TIMER";
    /// M: adds an extra minute to the unexpired timer
    public static final String ACTION_ADD_MINUTE_TIMER_UNEXPIRED =
        ACTION_PREFIX + "ADD_MINUTE_TIMER_UNEXPIRED";

    // extra for many actions specific to a given timer
    public static final String EXTRA_TIMER_ID =
            "com.android.deskclock.extra.TIMER_ID";

    // Describes the entity responsible for the action being performed.
    public static final String EXTRA_EVENT_LABEL = "com.android.deskclock.extra.EVENT_LABEL";

    public static final long RESET_EXPIRED_TIMER_DELAY_MILLIS = DateUtils.MINUTE_IN_MILLIS * 3;

    @Override
    protected void onCreate(Bundle icicle) {
        try {
            super.onCreate(icicle);
            mAppContext = getApplicationContext();

            final Intent intent = getIntent();
            if (intent == null) {
                return;
            }

            final String action = intent.getAction();
            LogUtils.i("HandleDeskClockApiCalls " + action);

            switch (action) {
                case ACTION_SHOW_TIMERS:
                case ACTION_RESET_TIMER:
                case ACTION_PAUSE_TIMER:
                case ACTION_START_TIMER:
                    handleTimerIntent(intent);
                    break;
            }
        } finally {
            finish();
        }
    }

    private void handleTimerIntent(Intent intent) {
        final String action = intent.getAction();

        // Determine where this intent originated.
        final int eventLabel = intent.getIntExtra(EXTRA_EVENT_LABEL, R.string.label_intent);
        int timerId = intent.getIntExtra(EXTRA_TIMER_ID, -1);
        Timer timer = null;

        if (ACTION_SHOW_TIMERS.equals(action)) {
            Events.sendTimerEvent(R.string.action_show, eventLabel);
        } else {
            String reason = null;
            if (timerId == -1) {
                // No timer id was given explicitly, so check if only one timer exists.
                final List<Timer> timers =  DataModel.getDataModel().getTimers();
                if (timers.isEmpty()) {
                    // No timers exist to control.
                    reason = getString(R.string.no_timers_exist);
                } else if (timers.size() > 1) {
                    // Many timers exist so the control command is ambiguous.
                    reason = getString(R.string.too_many_timers_exist);
                } else {
                    timer = timers.get(0);
                }
            } else {
                // Verify that the given timer does exist.
                timer = DataModel.getDataModel().getTimer(timerId);
                if (timer == null) {
                    reason = getString(R.string.timer_does_not_exist);
                }
            }

            if (timer == null) {
                Voice.notifyFailure(this, reason);
            } else {
                timerId = timer.getId();

                // Otherwise the control command can be honored.
                switch (action) {
                    case ACTION_RESET_TIMER: {
                        DataModel.getDataModel().resetOrDeleteTimer(timer, eventLabel);
                        if (timer.isExpired() && timer.getDeleteAfterUse()) {
                            timerId = -1;
                            reason = getString(R.string.timer_deleted);
                        } else {
                            reason = getString(R.string.timer_was_reset);
                        }
                        break;
                    }
                    case ACTION_START_TIMER: {
                        DataModel.getDataModel().startTimer(timer);
                        Events.sendTimerEvent(R.string.action_start, eventLabel);
                        reason = getString(R.string.timer_started);
                        break;
                    }
                    case ACTION_PAUSE_TIMER: {
                        DataModel.getDataModel().pauseTimer(timer);
                        Events.sendTimerEvent(R.string.action_pause, eventLabel);
                        reason = getString(R.string.timer_paused);
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("unknown timer action: " + action);
                }

                Voice.notifySuccess(this, reason);
            }

            LogUtils.i(reason);
        }

        // Open the UI to the timers.
        final Intent showTimers = new Intent(mAppContext, DeskClock.class)
                .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.TIMER_TAB_INDEX);
        if (timerId != -1) {
            showTimers.putExtra(EXTRA_TIMER_ID, timerId);
        }
        startActivity(showTimers);
    }
}