/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.provider.AlarmClock;
import android.text.TextUtils;

import com.android.deskclock.DeskClock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Voice;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Timer;
import com.android.deskclock.events.Events;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;

/**
 * This activity is never visible. It processes all public intents defined by {@link AlarmClock}
 * that apply to alarms and timers. Its definition in AndroidManifest.xml requires callers to hold
 * the com.android.alarm.permission.SET_ALARM permission to complete the requested action.
 */
public class HandleApiCalls extends Activity {

    private Context mAppContext;

    @Override
    protected void onCreate(Bundle icicle) {
        try {
            super.onCreate(icicle);
            mAppContext = getApplicationContext();
            final Intent intent = getIntent();
            final String action = intent == null ? null : intent.getAction();
            if (action == null) {
                return;
            }
            switch (action) {
                case AlarmClock.ACTION_SET_TIMER:
                    handleSetTimer(intent);
                    break;
            }
        } finally {
            finish();
        }
    }

    private void handleSetTimer(Intent intent) {
        // If no length is supplied, show the timer setup view.
        if (!intent.hasExtra(AlarmClock.EXTRA_LENGTH)) {
            startActivity(TimerFragment.createTimerSetupIntent(this));
            LogUtils.i("HandleApiCalls showing timer setup");
            return;
        }

        // Verify that the timer length is between one second and one day.
        final long lengthMillis = SECOND_IN_MILLIS * intent.getIntExtra(AlarmClock.EXTRA_LENGTH, 0);
        if (lengthMillis < Timer.MIN_LENGTH || lengthMillis > Timer.MAX_LENGTH) {
            Voice.notifyFailure(this, getString(R.string.invalid_timer_length));
            LogUtils.i("Invalid timer length requested: " + lengthMillis);
            return;
        }

        final String label = getMessageFromIntent(intent);
        final boolean skipUi = intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false);

        // Attempt to reuse an existing timer that is Reset with the same length and label.
        Timer timer = null;
        for (Timer t : DataModel.getDataModel().getTimers()) {
            if (!t.isReset()) { continue; }
            if (t.getLength() != lengthMillis) { continue; }
            if (!TextUtils.equals(label, t.getLabel())) { continue; }

            timer = t;
            break;
        }

        // Create a new timer if one could not be reused.
        if (timer == null) {
            timer = DataModel.getDataModel().addTimer(lengthMillis, label, skipUi);
            Events.sendTimerEvent(R.string.action_create, R.string.label_intent);
        }

        // Start the selected timer.
        DataModel.getDataModel().startTimer(timer);
        Events.sendTimerEvent(R.string.action_start, R.string.label_intent);
        Voice.notifySuccess(this, getString(R.string.timer_created));

        // If not instructed to skip the UI, display the running timer.
        if (!skipUi) {
            startActivity(new Intent(this, DeskClock.class)
                    .putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.TIMER_TAB_INDEX)
                    .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timer.getId()));
        }
    }


    private static String getMessageFromIntent(Intent intent) {
        final String message = intent.getStringExtra(AlarmClock.EXTRA_MESSAGE);
        return message == null ? "" : message;
    }
}
