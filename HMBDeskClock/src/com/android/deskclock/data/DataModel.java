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

package com.android.deskclock.data;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;

import java.util.List;

/**
 * All application-wide data is accessible through this singleton.
 */
public final class DataModel {

    /** Indicates the display style of clocks. */
    public enum ClockStyle {ANALOG, DIGITAL}

    /** Indicates the preferred sort order of cities. */
    public enum CitySort {NAME, UTC_OFFSET}

    /** The single instance of this data model that exists for the life of the application. */
    private static final DataModel sDataModel = new DataModel();

    private Handler mHandler;

    private Context mContext;

    /** The model from which settings are fetched. */
    private SettingsModel mSettingsModel;

    /** The model from which timer data are fetched. */
    private TimerModel mTimerModel;

    /** The model from which notification data are fetched. */
    private NotificationModel mNotificationModel;

    public static DataModel getDataModel() {
        return sDataModel;
    }

    public static void enforceMainLooper() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalAccessError("May only call from main thread.");
        }
    }

    public static void enforceNotMainLooper() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw new IllegalAccessError("May not call from main thread.");
        }
    }

    /**
     * Return the default shared preferences.
     */
    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private DataModel() {}

    /**
     * The context may be set precisely once during the application life.
     */
    public void setContext(Context context) {
        if (mContext != null) {
//            throw new IllegalStateException("context has already been set");
        }
        mContext = context.getApplicationContext();

        mSettingsModel = new SettingsModel(mContext);
        mNotificationModel = new NotificationModel();
        mTimerModel = new TimerModel(mContext, mSettingsModel, mNotificationModel);
    }

    /**
     * Posts a runnable to the main thread and blocks until the runnable executes. Used to access
     * the data model from the main thread.
     */
    public void run(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
            return;
        }

        final ExecutedRunnable er = new ExecutedRunnable(runnable);
        getHandler().post(er);

        // Wait for the data to arrive, if it has not.
        synchronized (er) {
            if (!er.isExecuted()) {
                try {
                    er.wait();
                } catch (InterruptedException ignored) {
                    // ignore
                }
            }
        }
    }

    /**
     * @return a handler associated with the main thread
     */
    private synchronized Handler getHandler() {
        if (mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }

    //
    // Application
    //

    /**
     * @param inForeground {@code true} to indicate the application is open in the foreground
     */
    public void setApplicationInForeground(boolean inForeground) {
        enforceMainLooper();

        if (mNotificationModel.isApplicationInForeground() != inForeground) {
            mNotificationModel.setApplicationInForeground(inForeground);

            // Refresh all notifications in response to a change in app open state.
            mTimerModel.updateNotification();
        }
    }

    /**
     * @return {@code true} when the application is open in the foreground; {@code false} otherwise
     */
    public boolean isApplicationInForeground() {
        return mNotificationModel.isApplicationInForeground();
    }

    /**
     * Called when the notifications may be stale or absent from the notification manager and must
     * be rebuilt. e.g. after upgrading the application
     */
    public void updateAllNotifications() {
        mTimerModel.updateNotification();
    }

    //
    // Timers
    //

    /**
     * @param timerListener to be notified when timers are added, updated and removed
     */
    public void addTimerListener(TimerListener timerListener) {
        enforceMainLooper();
        mTimerModel.addTimerListener(timerListener);
    }

    /**
     * @param timerListener to no longer be notified when timers are added, updated and removed
     */
    public void removeTimerListener(TimerListener timerListener) {
        enforceMainLooper();
        mTimerModel.removeTimerListener(timerListener);
    }

    /**
     * @return a list of timers for display
     */
    public List<Timer> getTimers() {
        enforceMainLooper();
        return mTimerModel.getTimers();
    }

    /**
     * @return a list of expired timers for display
     */
    public List<Timer> getExpiredTimers() {
        enforceMainLooper();
        return mTimerModel.getExpiredTimers();
    }

    /**
     * @param timerId identifies the timer to return
     * @return the timer with the given {@code timerId}
     */
    public Timer getTimer(int timerId) {
        enforceMainLooper();
        return mTimerModel.getTimer(timerId);
    }

    /**
     * @return the timer that last expired and is still expired now; {@code null} if no timers are
     *      expired
     */
    public Timer getMostRecentExpiredTimer() {
        enforceMainLooper();
        return mTimerModel.getMostRecentExpiredTimer();
    }

    /**
     * @param length the length of the timer in milliseconds
     * @param label describes the purpose of the timer
     * @param deleteAfterUse {@code true} indicates the timer should be deleted when it is reset
     * @return the newly added timer
     */
    public Timer addTimer(long length, String label, boolean deleteAfterUse) {
        enforceMainLooper();
        return mTimerModel.addTimer(length, label, deleteAfterUse);
    }

    /**
     * @param timer the timer to be removed
     */
    public void removeTimer(Timer timer) {
        enforceMainLooper();
        mTimerModel.removeTimer(timer);
    }

    /**
     * @param timer the timer to be started
     */
    public void startTimer(Timer timer) {
        enforceMainLooper();
        mTimerModel.updateTimer(timer.start());
    }

    /**
     * @param timer the timer to be paused
     */
    public void pauseTimer(Timer timer) {
        enforceMainLooper();
        mTimerModel.updateTimer(timer.pause());
    }

    /**
     * @param service used to start foreground notifications for expired timers
     * @param timer the timer to be expired
     */
    public void expireTimer(Service service, Timer timer) {
        enforceMainLooper();
        mTimerModel.expireTimer(service, timer);
    }

    public void startRinger() {
        enforceMainLooper();
        mTimerModel.startRinger();
    }

    public void stopRinger() {
        enforceMainLooper();
        mTimerModel.stopRinger();
    }

    public void shutdown() {
        enforceMainLooper();
        mTimerModel.shutdown();
    }

    public void bootCompleted(Service service) {
        enforceMainLooper();
        mTimerModel.bootCompleted(service);
    }

    public void resetExpiredTimer() {
        enforceMainLooper();
        mTimerModel.resetExpiredTimer();
    }

    /**
     * If the given {@code timer} is expired and marked for deletion after use then this method
     * removes the the timer. The timer is otherwise transitioned to the reset state and continues
     * to exist.
     *
     * @param timer the timer to be reset
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     */
    public void resetOrDeleteTimer(Timer timer, @StringRes int eventLabelId) {
        enforceMainLooper();
        mTimerModel.resetOrDeleteTimer(timer, eventLabelId);
    }

    /**
     * Resets all timers.
     *
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     */
    public void resetTimers(@StringRes int eventLabelId) {
        enforceMainLooper();
        mTimerModel.resetTimers(eventLabelId);
    }

    /**
     * Resets all expired timers.
     *
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     */
    public void resetExpiredTimers(@StringRes int eventLabelId) {
        enforceMainLooper();
        mTimerModel.resetExpiredTimers(eventLabelId);
    }

    /**
     * Resets all unexpired timers.
     *
     * @param eventLabelId the label of the timer event to send; 0 if no event should be sent
     */
    public void resetUnexpiredTimers(@StringRes int eventLabelId) {
        enforceMainLooper();
        mTimerModel.resetUnexpiredTimers(eventLabelId);
    }

    /**
     * @param timer the timer to which a minute should be added to the remaining time
     */
    public void addTimerMinute(Timer timer) {
        enforceMainLooper();
        mTimerModel.updateTimer(timer.addMinute());
    }

    /**
     * @param timer the timer to which the new {@code label} belongs
     * @param label the new label to store for the {@code timer}
     */
    public void setTimerLabel(Timer timer, String label) {
        enforceMainLooper();
        mTimerModel.updateTimer(timer.setLabel(label));
    }

    /**
     * Updates the timer notifications to be current.
     */
    public void updateTimerNotification() {
        enforceMainLooper();
        mTimerModel.updateNotification();
    }

    /**
     * @return the uri of the default ringtone to play for all timers when no user selection exists
     */
    public Uri getDefaultTimerRingtoneUri() {
        enforceMainLooper();
        return mTimerModel.getDefaultTimerRingtoneUri();
    }

    /**
     * @return {@code true} iff the ringtone to play for all timers is the silent ringtone
     */
    public boolean isTimerRingtoneSilent() {
        enforceMainLooper();
        return mTimerModel.isTimerRingtoneSilent();
    }

    /**
     * @return the uri of the ringtone to play for all timers
     */
    public Uri getTimerRingtoneUri() {
        enforceMainLooper();
        return mTimerModel.getTimerRingtoneUri();
    }

    /**
     * @return the title of the ringtone that is played for all timers
     */
    public String getTimerRingtoneTitle() {
        enforceMainLooper();
        return mTimerModel.getTimerRingtoneTitle();
    }

    //
    // Settings
    //

    /**
     * @return the style of clock to display in the clock application
     */
    /*public ClockStyle getClockStyle() {
        enforceMainLooper();
        return mSettingsModel.getClockStyle();
    }*/

    /**
     * @return the style of clock to display in the clock screensaver
     */
    /*public ClockStyle getScreensaverClockStyle() {
        enforceMainLooper();
        return mSettingsModel.getScreensaverClockStyle();
    }*/

    /**
     * @return {@code true} if the users wants to automatically show a clock for their home timezone
     *      when they have travelled outside of that timezone
     */
    public boolean getShowHomeClock() {
        enforceMainLooper();
        return mSettingsModel.getShowHomeClock();
    }

    /**
     * Used to execute a delegate runnable and track its completion.
     */
    private static class ExecutedRunnable implements Runnable {

        private final Runnable mDelegate;
        private boolean mExecuted;

        private ExecutedRunnable(Runnable delegate) {
            this.mDelegate = delegate;
        }

        @Override
        public void run() {
            mDelegate.run();

            synchronized (this) {
                mExecuted = true;
                notifyAll();
            }
        }

        private boolean isExecuted() {
            return mExecuted;
        }
    }
}