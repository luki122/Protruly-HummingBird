/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.deskclock;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.transition.AutoTransition;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.alarms.PowerOffAlarm;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.provider.DaysOfWeek;
import com.android.deskclock.widget.ActionableToastBar;

import java.io.File;
import java.util.Calendar;

/**
 * AlarmClock application.
 *
 * 后面替换AlarmClockFragment
 */
public abstract class AlarmClockFragment2 extends DeskClockFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, View.OnTouchListener, AlarmItemAdapter.HostInterface {
    private static final float EXPAND_DECELERATION = 1f;
    private static final float COLLAPSE_DECELERATION = 0.7f;

    private static final int ANIMATION_DURATION = 300;
    private static final int EXPAND_DURATION = 300;
    private static final int COLLAPSE_DURATION = 250;

    private static final int ROTATE_180_DEGREE = 180;
    private static final float ALARM_ELEVATION = 8f;
    private static final float TINTED_LEVEL = 0.09f;

    private static final String KEY_EXPANDED_ID = "expandedId";
    private static final String KEY_REPEAT_CHECKED_IDS = "repeatCheckedIds";
    private static final String KEY_RINGTONE_TITLE_CACHE = "ringtoneTitleCache";
    private static final String KEY_SELECTED_ALARMS = "selectedAlarms";
    private static final String KEY_DELETED_ALARM = "deletedAlarm";
    private static final String KEY_UNDO_SHOWING = "undoShowing";
    private static final String KEY_PREVIOUS_DAY_MAP = "previousDayMap";
    private static final String KEY_SELECTED_ALARM = "selectedAlarm";
    private static final String KEY_DEFAULT_RINGTONE = "default_ringtone";

    private static final int REQUEST_CODE_RINGTONE = 1;
    private static final int REQUEST_CODE_PERMISSIONS = 2;
    private static final long INVALID_ID = -1;
    private static final String PREF_KEY_DEFAULT_ALARM_RINGTONE_URI = "default_alarm_ringtone_uri";

    // Use transitions only in API 21+
    private static final boolean USE_TRANSITION_FRAMEWORK = Utils.isLOrLater();

    // This extra is used when receiving an intent to create an alarm, but no alarm details
    // have been passed in, so the alarm page should start the process of creating a new alarm.
    public static final String ALARM_CREATE_NEW_INTENT_EXTRA = "deskclock.create.new";

    // This extra is used when receiving an intent to scroll to specific alarm. If alarm
    // can not be found, and toast message will pop up that the alarm has be deleted.
    public static final String SCROLL_TO_ALARM_INTENT_EXTRA = "deskclock.scroll.to.alarm";

    private FrameLayout mMainLayout;
    /// M: The Uri string of system default alarm alert
    public static final String SYSTEM_SETTINGS_ALARM_ALERT = "content://settings/system/alarm_alert";

    private AlarmClockFragmentHost mHost;

    private ListView mAlarmsList;
    private AlarmItemAdapter mAdapter;
    private View mEmptyView;
    private View mFooterView;

    private Bundle mRingtoneTitleCache; // Key: ringtone uri, value: ringtone title
    private ActionableToastBar mUndoBar;
    private View mUndoFrame;

    protected Alarm mSelectedAlarm;
    protected long mScrollToAlarmId = INVALID_ID;

    private Loader mCursorLoader = null;

    // Saved states for undo
    private Alarm mDeletedAlarm;
    protected Alarm mAddedAlarm;
    private boolean mUndoShowing;
//     // Determines the order that days of the week are shown in the UI
//        private int[] mDayOrder;
//
//        // A reference used to create mDayOrder
//        private final int[] DAY_ORDER = new int[] {
//                Calendar.SUNDAY,
//                Calendar.MONDAY,
//                Calendar.TUESDAY,
//                Calendar.WEDNESDAY,
//                Calendar.THURSDAY,
//                Calendar.FRIDAY,
//                Calendar.SATURDAY,
//        };

//    private Interpolator mExpandInterpolator;
//    private Interpolator mCollapseInterpolator;

    private Transition mAddRemoveTransition;
    private Transition mRepeatTransition;
    private Transition mEmptyViewTransition;
    ///M: added to sync animation states
    private ValueAnimator mCollapseAnimator;
    private ValueAnimator mExpandAnimator;
    // Abstract methods to to be overridden by for post- and pre-L implementations as necessary
    protected abstract void setTimePickerListener();
    protected abstract void showTimeEditDialog(Alarm alarm);
    protected abstract void startCreatingAlarm();

    protected void processTimeSet(int hourOfDay, int minute) {
        if (mSelectedAlarm == null) {
            // If mSelectedAlarm is null then we're creating a new alarm.
            Alarm a = new Alarm();
            a.alert = getDefaultRingtoneUri();
            if (a.alert == null) {
                a.alert = Uri.parse("content://settings/system/alarm_alert");
            }
            a.hour = hourOfDay;
            a.minutes = minute;
            a.enabled = true;

            mAddedAlarm = a;
            asyncAddAlarm(a);
        } else {
            mSelectedAlarm.hour = hourOfDay;
            mSelectedAlarm.minutes = minute;
            mSelectedAlarm.enabled = true;
            mScrollToAlarmId = mSelectedAlarm.id;
            asyncUpdateAlarm(mSelectedAlarm, true);
            mSelectedAlarm = null;
        }
    }

    public AlarmClockFragment2() {
        // Basic provider required by Fragment.java
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        mCursorLoader = getLoaderManager().initLoader(0, null, this);
        ///M: get default alarm ringtone from the preference,
        // if there was no this item, just save system alarm ringtone to preference @{
        if (TextUtils.isEmpty(getDefaultRingtone(getActivity()))) {
            setSystemAlarmRingtoneToPref();
        }
        ///@}
        ///M: set volume control stream as alarm volume@{
        getActivity().setVolumeControlStream(AudioManager.STREAM_ALARM);
        ///@}
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedState) {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.alarm_clock_2, container, false);

        long expandedId = INVALID_ID;
        long[] repeatCheckedIds = null;
        long[] selectedAlarms = null;
        Bundle previousDayMap = null;
        if (savedState != null) {
            expandedId = savedState.getLong(KEY_EXPANDED_ID);
            repeatCheckedIds = savedState.getLongArray(KEY_REPEAT_CHECKED_IDS);
            mRingtoneTitleCache = savedState.getBundle(KEY_RINGTONE_TITLE_CACHE);
            mDeletedAlarm = savedState.getParcelable(KEY_DELETED_ALARM);
            mUndoShowing = savedState.getBoolean(KEY_UNDO_SHOWING);
            selectedAlarms = savedState.getLongArray(KEY_SELECTED_ALARMS);
            previousDayMap = savedState.getBundle(KEY_PREVIOUS_DAY_MAP);
            mSelectedAlarm = savedState.getParcelable(KEY_SELECTED_ALARM);
        }

//        mExpandInterpolator = new DecelerateInterpolator(EXPAND_DECELERATION);
//        mCollapseInterpolator = new DecelerateInterpolator(COLLAPSE_DECELERATION);

        if (USE_TRANSITION_FRAMEWORK) {
            mAddRemoveTransition = new AutoTransition();
            mAddRemoveTransition.setDuration(ANIMATION_DURATION);

            /// M: Scrap the views in ListView and request layout again, then alarm item will be
            /// attached correctly. This is to avoid the case when some items are not correctly
            ///  attached after animation end  @{
            mAddRemoveTransition.addListener(new Transition.TransitionListenerAdapter() {
                @Override
                public void onTransitionEnd(Transition transition) {
                    mAlarmsList.clearScrapViewsIfNeeded();
                }
            });
            /// @}

            mRepeatTransition = new AutoTransition();
            mRepeatTransition.setDuration(ANIMATION_DURATION / 2);
            mRepeatTransition.setInterpolator(new AccelerateDecelerateInterpolator());

            mEmptyViewTransition = new TransitionSet()
                    .setOrdering(TransitionSet.ORDERING_SEQUENTIAL)
                    .addTransition(new Fade(Fade.OUT))
                    .addTransition(new Fade(Fade.IN))
                    .setDuration(ANIMATION_DURATION);
        }

        boolean isLandscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        mEmptyView = v.findViewById(R.id.alarms_empty_view);

        mMainLayout = (FrameLayout) v.findViewById(R.id.main);
        mAlarmsList = (ListView) v.findViewById(R.id.alarms_list);

        mUndoBar = (ActionableToastBar) v.findViewById(R.id.undo_bar);
        mUndoFrame = v.findViewById(R.id.undo_frame);
        mUndoFrame.setOnTouchListener(this);

        mFooterView = v.findViewById(R.id.alarms_footer_view);
        mFooterView.setOnTouchListener(this);

        mAdapter = new AlarmItemAdapter(getActivity(),
                expandedId, repeatCheckedIds, selectedAlarms, previousDayMap, mAlarmsList, this);
        mAdapter.registerDataSetObserver(new DataSetObserver() {

            private int prevAdapterCount = -1;

            @Override
            public void onChanged() {

                final int count = mAdapter.getCount();
                if (mDeletedAlarm != null && prevAdapterCount > count) {
                    showUndoBar();
                }

                if (USE_TRANSITION_FRAMEWORK &&
                    ((count == 0 && prevAdapterCount > 0) ||  /* should fade  in */
                    (count > 0 && prevAdapterCount == 0) /* should fade out */)) {
                    TransitionManager.beginDelayedTransition(mMainLayout, mEmptyViewTransition);
                }
                mEmptyView.setVisibility(count == 0 ? View.VISIBLE : View.GONE);

                // Cache this adapter's count for when the adapter changes.
                prevAdapterCount = count;
                super.onChanged();
            }
        });

        if (mRingtoneTitleCache == null) {
            mRingtoneTitleCache = new Bundle();
        }

        mAlarmsList.setAdapter(mAdapter);
        mAlarmsList.setVerticalScrollBarEnabled(true);
        mAlarmsList.setOnCreateContextMenuListener(this);

        if (mUndoShowing) {
            showUndoBar();
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.registerBroadcastReceiver();
        final DeskClock activity = (DeskClock) getActivity();
        if (activity.getSelectedTab() == DeskClock.ALARM_TAB_INDEX) {
            setFabAppearance();
            setLeftRightButtonAppearance();
        }
//        final int startDay = Utils.getZeroIndexedFirstDayOfWeek(getActivity());
//        mDayOrder = new int[DaysOfWeek.DAYS_IN_A_WEEK];
//
//        for (int i = 0; i < DaysOfWeek.DAYS_IN_A_WEEK; ++i) {
//            mDayOrder[i] = DAY_ORDER[(startDay + i) % 7];
//        }
        activity.registerPageChangedListener(this);

        // Check if another app asked us to create a blank new alarm.
        final Intent intent = getActivity().getIntent();
        if (intent.hasExtra(ALARM_CREATE_NEW_INTENT_EXTRA)) {
            if (intent.getBooleanExtra(ALARM_CREATE_NEW_INTENT_EXTRA, false)) {
                // An external app asked us to create a blank alarm.
                startCreatingAlarm();
            }

            // Remove the CREATE_NEW extra now that we've processed it.
            intent.removeExtra(ALARM_CREATE_NEW_INTENT_EXTRA);
        } else if (intent.hasExtra(SCROLL_TO_ALARM_INTENT_EXTRA)) {
            long alarmId = intent.getLongExtra(SCROLL_TO_ALARM_INTENT_EXTRA, Alarm.INVALID_ID);
            if (alarmId != Alarm.INVALID_ID) {
                mScrollToAlarmId = alarmId;
                if (mCursorLoader != null && mCursorLoader.isStarted()) {
                    // We need to force a reload here to make sure we have the latest view
                    // of the data to scroll to.
                    mCursorLoader.forceLoad();
                }
            }

            // Remove the SCROLL_TO_ALARM extra now that we've processed it.
            intent.removeExtra(SCROLL_TO_ALARM_INTENT_EXTRA);
        }

        setTimePickerListener();
    }

    private void hideUndoBar(boolean animate, MotionEvent event) {
        if (mUndoBar != null) {
            mUndoFrame.setVisibility(View.GONE);
            if (event != null && mUndoBar.isEventInToastBar(event)) {
                // Avoid touches inside the undo bar.
                return;
            }
            mUndoBar.hide(animate);
        }
        mDeletedAlarm = null;
        mUndoShowing = false;
    }

    private void showUndoBar() {
        final Alarm deletedAlarm = mDeletedAlarm;
        mUndoFrame.setVisibility(View.VISIBLE);
        mUndoBar.show(new ActionableToastBar.ActionClickedListener() {
            @Override
            public void onActionClicked() {
                mAddedAlarm = deletedAlarm;
                mDeletedAlarm = null;
                mUndoShowing = false;

                asyncAddAlarm(deletedAlarm);
            }
        }, 0, getResources().getString(R.string.alarm_deleted), true, R.string.alarm_undo, true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLongArray(KEY_REPEAT_CHECKED_IDS, mAdapter.getRepeatArray());
        outState.putLongArray(KEY_SELECTED_ALARMS, mAdapter.getSelectedAlarmsArray());
        outState.putBundle(KEY_RINGTONE_TITLE_CACHE, mRingtoneTitleCache);
        outState.putParcelable(KEY_DELETED_ALARM, mDeletedAlarm);
        outState.putBoolean(KEY_UNDO_SHOWING, mUndoShowing);
        outState.putBundle(KEY_PREVIOUS_DAY_MAP, mAdapter.getPreviousDaysOfWeekMap());
        outState.putParcelable(KEY_SELECTED_ALARM, mSelectedAlarm);
    }

    @Override
    public void onDestroy() {
        mHost = null;
        super.onDestroy();
        ToastMaster.cancelToast();
    }

    @Override
    public void onPause() {
        super.onPause();
        // When the user places the app in the background by pressing "home",
        // dismiss the toast bar. However, since there is no way to determine if
        // home was pressed, just dismiss any existing toast bar when restarting
        // the app.
        ((DeskClock)getActivity()).unregisterPageChangedListener(this);
        mAdapter.unregisterBroadcastReceiver();
        hideUndoBar(false, null);
    }

    private void showLabelDialog(final Alarm alarm) {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag("label_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        /// M:If the LabelEditDialog Existed,do not create again
        //ft.addToBackStack(null);
        /// M:Don't need use the method ft.commit(), because it may cause IllegalStateException
        final LabelDialogFragment newFragment =
                LabelDialogFragment.newInstance(alarm, alarm.label, getTag());
        ft.add(newFragment, "label_dialog");
        ft.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
    }

    public void setLabel(Alarm alarm, String label) {
        alarm.label = label;
        asyncUpdateAlarm(alarm, false);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return Alarm.getAlarmsCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, final Cursor data) {
        mAdapter.swapCursor(data);
        if (mScrollToAlarmId != INVALID_ID) {
            scrollToAlarm(mScrollToAlarmId);
            mScrollToAlarmId = INVALID_ID;
        }
    }

    /**
     * Scroll to alarm with given alarm id.
     *
     * @param alarmId The alarm id to scroll to.
     */
    private void scrollToAlarm(long alarmId) {
        int alarmPosition = -1;
        for (int i = 0; i < mAdapter.getCount(); i++) {
            long id = mAdapter.getItemId(i);
            if (id == alarmId) {
                alarmPosition = i;
                break;
            }
        }

        if (alarmPosition >= 0) {
            mAlarmsList.smoothScrollToPositionFromTop(alarmPosition, 0);
        } else {
            // Trying to display a deleted alarm should only happen from a missed notification for
            // an alarm that has been marked deleted after use.
            Context context = getActivity().getApplicationContext();
            Toast toast = Toast.makeText(context, R.string.missed_alarm_has_been_deleted,
                    Toast.LENGTH_LONG);
            ToastMaster.setToast(toast);
            toast.show();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }

    private void launchRingTonePicker(Alarm alarm) {
        mSelectedAlarm = alarm;
        Uri oldRingtone = Alarm.NO_RINGTONE_URI.equals(alarm.alert) ? null : alarm.alert;
        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, oldRingtone);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        startActivityForResult(intent, REQUEST_CODE_RINGTONE);
    }

    private void saveRingtoneUri(Intent intent) {
        Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (uri == null) {
            uri = Alarm.NO_RINGTONE_URI;
        }
        /// M: if the alarm to change ringtone is null, then do nothing @{
        if (null == mSelectedAlarm) {
            LogUtils.w("saveRingtoneUri the alarm to change ringtone is null");
            return;
        }
        /// @}
        mSelectedAlarm.alert = uri;

        // Save the last selected ringtone as the default for new alarms
       // setDefaultRingtoneUri(uri);

//        asyncUpdateAlarm(mSelectedAlarm, false);

        // If the user chose an external ringtone and has not yet granted the permission to read
        // external storage, ask them for that permission now.
        if (!AlarmUtils.hasPermissionToDisplayRingtoneTitle(getActivity(), uri)) {
            final String[] perms = {Manifest.permission.READ_EXTERNAL_STORAGE};
            requestPermissions(perms, REQUEST_CODE_PERMISSIONS);
        }
        else{
            /// M: Permissions already granted, save the ringtone
            setDefaultRingtoneUri(uri);
            asyncUpdateAlarm(mSelectedAlarm, false);
        }
    }

    private Uri getDefaultRingtoneUri() {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final String ringtoneUriString = sp.getString(PREF_KEY_DEFAULT_ALARM_RINGTONE_URI, null);

        final Uri ringtoneUri;
        if (ringtoneUriString != null) {
            ringtoneUri = Uri.parse(ringtoneUriString);
        } else {
            ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(getActivity(),
                    RingtoneManager.TYPE_ALARM);
        }

        return ringtoneUri;
    }

    private void setDefaultRingtoneUri(Uri uri) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (uri == null) {
            sp.edit().remove(PREF_KEY_DEFAULT_ALARM_RINGTONE_URI).apply();
        } else {
            sp.edit().putString(PREF_KEY_DEFAULT_ALARM_RINGTONE_URI, uri.toString()).apply();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_RINGTONE:
                    saveRingtoneUri(data);
                    break;
                default:
                    LogUtils.w("Unhandled request code in onActivityResult: " + requestCode);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        // The permission change may alter the cached ringtone titles so clear them.
        // (e.g. READ_EXTERNAL_STORAGE is granted or revoked)
        mRingtoneTitleCache.clear();
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // permission was granted, yay!
               setDefaultRingtoneUri(mSelectedAlarm.alert);
               asyncUpdateAlarm(mSelectedAlarm, false);

            } else {
                // permission denied
              if(!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE))
              {
                 Toast.makeText(getActivity().getApplicationContext(),
                 getString(com.mediatek.R.string.denied_required_permission),
                 Toast.LENGTH_SHORT).show();
              }

            }

    }

    private static AlarmInstance setupAlarmInstance(Context context, Alarm alarm) {
        ContentResolver cr = context.getContentResolver();
        AlarmInstance newInstance = alarm.createInstanceAfter(Calendar.getInstance());
        newInstance = AlarmInstance.addInstance(cr, newInstance);
        // Register instance to state manager
        AlarmStateManager.registerInstance(context, newInstance, true);
        return newInstance;
    }

    private void asyncDeleteAlarm(final Alarm alarm) {
        final Context context = AlarmClockFragment2.this.getActivity().getApplicationContext();
        final AsyncTask<Void, Void, Void> deleteTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... parameters) {
                // Activity may be closed at this point , make sure data is still valid
                if (context != null && alarm != null) {
                    Events.sendAlarmEvent(R.string.action_delete, R.string.label_deskclock);

                    ContentResolver cr = context.getContentResolver();
                    AlarmStateManager.deleteAllInstances(context, alarm.id);
                    Alarm.deleteAlarm(cr, alarm.id);
                }
                return null;
            }
        };
        mUndoShowing = true;
        deleteTask.execute();
    }

    protected void asyncAddAlarm(final Alarm alarm) {
        final Context context = AlarmClockFragment2.this.getActivity().getApplicationContext();
        final AsyncTask<Void, Void, AlarmInstance> updateTask =
                new AsyncTask<Void, Void, AlarmInstance>() {
            @Override
            protected AlarmInstance doInBackground(Void... parameters) {
                if (context != null && alarm != null) {
                    Events.sendAlarmEvent(R.string.action_create, R.string.label_deskclock);
                    ContentResolver cr = context.getContentResolver();

                    // Add alarm to db
                    Alarm newAlarm = Alarm.addAlarm(cr, alarm);
                    mScrollToAlarmId = newAlarm.id;

                    // Create and add instance to db
                    if (newAlarm.enabled) {
                        return setupAlarmInstance(context, newAlarm);
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(AlarmInstance instance) {
                if (instance != null) {
                    AlarmUtils.popAlarmSetToast(context, instance.getAlarmTime().getTimeInMillis());
                }
            }
        };
        updateTask.execute();
    }

    protected void asyncUpdateAlarm(final Alarm alarm, final boolean popToast) {
        final Context context = AlarmClockFragment2.this.getActivity().getApplicationContext();
        final AsyncTask<Void, Void, AlarmInstance> updateTask =
                new AsyncTask<Void, Void, AlarmInstance>() {
            @Override
            protected AlarmInstance doInBackground(Void ... parameters) {
                Events.sendAlarmEvent(R.string.action_update, R.string.label_deskclock);
                ContentResolver cr = context.getContentResolver();

                // Dismiss all old instances
                AlarmStateManager.deleteAllInstances(context, alarm.id);

                // Update alarm
                if(Alarm.updateAlarm(cr, alarm) == true) {
                    if (alarm.enabled) {
                        return setupAlarmInstance(context, alarm);
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(AlarmInstance instance) {
                if (popToast && instance != null) {
                    AlarmUtils.popAlarmSetToast(context, instance.getAlarmTime().getTimeInMillis());
                }
            }
        };
        updateTask.execute();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        hideUndoBar(true, event);
        return false;
    }

    @Override
    public void onFabClick(View view){
        hideUndoBar(true, null);
        startCreatingAlarm();
    }

    @Override
    public void setFabAppearance() {
        final DeskClock activity = (DeskClock) getActivity();
        if (mFab == null || activity.getSelectedTab() != DeskClock.ALARM_TAB_INDEX) {
            return;
        }
        if (!isSelectionMode()) {
            mFab.setVisibility(View.VISIBLE);
        }
        mFab.setImageResource(R.drawable.ic_fab_plus);
        mFab.setContentDescription(getString(R.string.button_alarms));
    }

    @Override
    public void setLeftRightButtonAppearance() {
        final DeskClock activity = (DeskClock) getActivity();
        if (mLeftButton == null || mRightButton == null ||
                activity.getSelectedTab() != DeskClock.ALARM_TAB_INDEX) {
            return;
        }
        mLeftButton.setVisibility(View.INVISIBLE);
        mRightButton.setVisibility(View.INVISIBLE);
    }

    /**
     * M: Set the system default Alarm Ringtone,
     * then save it as the Clock internal used ringtone.
     */
    public void setSystemAlarmRingtoneToPref() {
        Uri systemDefaultRingtone = RingtoneManager.getActualDefaultRingtoneUri(getActivity(),
                RingtoneManager.TYPE_ALARM);
        /// M: The RingtoneManager may return null alert. @{
        if (systemDefaultRingtone == null) {
            systemDefaultRingtone = Uri.parse(SYSTEM_SETTINGS_ALARM_ALERT);
        }
        /// @}
        setDefaultRingtone(systemDefaultRingtone.toString());
        LogUtils.v("setSystemAlarmRingtone: " + systemDefaultRingtone);
    }

    /**
     * M: Set the internal used default Ringtones
     */
    public void setDefaultRingtone(String defaultRingtone) {
        if (TextUtils.isEmpty(defaultRingtone)) {
            LogUtils.e("setDefaultRingtone fail");
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_DEFAULT_RINGTONE, defaultRingtone);
        editor.apply();
        LogUtils.v("Set default ringtone to preference" + defaultRingtone);
    }

    /**
     * M: Get the internal used default Ringtones
     */
    public static String getDefaultRingtone(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultRingtone = prefs.getString(KEY_DEFAULT_RINGTONE, "");
        LogUtils.v("Get default ringtone from preference " + defaultRingtone);
        return defaultRingtone;
    }



    /**
     *M: to check if the ringtone media file is removed from SD-card or not.
     * @param ringtone
     * @return
     */
    public static boolean isRingtoneExisted(Context ctx, String ringtone) {
        boolean result = false;
        if (ringtone != null) {
            if (ringtone.contains("internal")) {
                return true;
            }
            String path = PowerOffAlarm.getRingtonePath(ctx, ringtone);
            if (!TextUtils.isEmpty(path)) {
                result = new File(path).exists();
            }
            LogUtils.v("isRingtoneExisted: " + result + " ,ringtone: " + ringtone
                    + " ,Path: " + path);
        }
        return result;
    }

    public interface AlarmClockFragmentHost {
        public void onAlarmItemClick(boolean isLongClick, long id);
        public boolean isAlarmItemSelected(long id);
        public boolean isSelectionMode();
    }

    //call this method immediately after fragment attached to the window
    public void setHost(final AlarmClockFragmentHost host) {
        if (host != null) {
            mHost = host;
        } else {
            // TODO: 17-5-3 Error Log
        }
    }

    @Override
    public void onAlarmItemClick(boolean isLongClick, long id) {
        if (mHost != null) {
            mHost.onAlarmItemClick(isLongClick, id);
        } else {
            // TODO: 17-5-3 Error Log
        }
    }

    @Override
    public boolean isAlarmItemSelected(long id) {
        if (mHost != null) {
            return mHost.isAlarmItemSelected(id);
        }
        return false;
    }

    @Override
    public boolean isSelectionMode() {
        if (mHost != null) {
            return mHost.isSelectionMode();
        }
        return false;
    }

    @Override
    public void onPageChanged(int page) {
        mAdapter.notifyDataSetChanged();
    }
}
