/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.deskclock.worldclock.CitiesActivity2;
import com.android.deskclock.worldclock.WorldClockAdapter;
import com.android.deskclock.worldclock.WorldClockAdapter2;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment that shows  the clock (analog or digital), the next alarm info and the world clock.
 */
public class ClockFragment2 extends DeskClockFragment implements
        OnSharedPreferenceChangeListener, WorldClockAdapter2.HostInterface {

    private static final String BUTTONS_HIDDEN_KEY = "buttons_hidden";
    private static final boolean PRE_L_DEVICE =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
    private final static String TAG = "ClockFragment";

    private boolean mButtonsHidden = false;
    private View mAnalogClock, mClockFrame, mHairline;
    private TextView mDate,mWeekday;
    private TextClock mTime;
    private WorldClockAdapter2 mAdapter;
    private ListView mList;
    private SharedPreferences mPrefs;
    private String mDateFormat;
    private String mDateFormatForAccessibility;

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean changed = action.equals(Intent.ACTION_TIME_CHANGED)
                    || action.equals(Intent.ACTION_TIMEZONE_CHANGED)
                    || action.equals(Intent.ACTION_LOCALE_CHANGED);
            if (changed) {
//                Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mClockFrame);
                updateDateAndWeekday();
                if (mAdapter != null) {
                    // *CHANGED may modify the need for showing the Home City
                    if (mAdapter.hasHomeCity() != mAdapter.needHomeCity()) {
                        mAdapter.reloadData(context);
                    } else {
                        mAdapter.notifyDataSetChanged();
                    }
                    // Locale change: update digital clock format and
                    // reload the cities list with new localized names
                    if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
//                        if (mDigitalClock != null) {
//                            Utils.setTimeFormat(context,
//                                (TextClock) mDigitalClock.findViewById(R.id.digital_clock),
//                                context.getResources().getDimensionPixelSize(
//                                    R.dimen.main_ampm_font_size));
//                        }
                        mAdapter.loadCitiesDb(context);
                        mAdapter.notifyDataSetChanged();
                    }
                }
                Utils.setQuarterHourUpdater(mHandler, mQuarterHourUpdater);
            }
            if (changed || action.equals(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)) {
//                Utils.refreshAlarm(getActivity(), mClockFrame);
            }
        }
    };

    private final Handler mHandler = new Handler();

    /* Register ContentObserver to see alarm changes for pre-L */
    private final ContentObserver mAlarmObserver = PRE_L_DEVICE
            ? new ContentObserver(mHandler) {
                @Override
                public void onChange(boolean selfChange) {
//                    Utils.refreshAlarm(ClockFragment2.this.getActivity(), mClockFrame);
                }
            }
            : null;

    // Thread that runs on every quarter-hour and refreshes the date.
    private final Runnable mQuarterHourUpdater = new Runnable() {
        @Override
        public void run() {
            // Update the main and world clock dates
//            Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mClockFrame);
            updateDateAndWeekday();
            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
            Utils.setQuarterHourUpdater(mHandler, mQuarterHourUpdater);
        }
    };

    public ClockFragment2() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle icicle) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.clock_fragment_2, container, false);
        if (icicle != null) {
            mButtonsHidden = icicle.getBoolean(BUTTONS_HIDDEN_KEY, false);
        }
        mList = (ListView) v.findViewById(R.id.cities);
//        mList.setDivider(null);

        OnTouchListener longPressNightMode = new OnTouchListener() {
            private float mMaxMovementAllowed = -1;
            private int mLongPressTimeout = -1;
            private float mLastTouchX, mLastTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mMaxMovementAllowed == -1) {
                    mMaxMovementAllowed = ViewConfiguration.get(getActivity()).getScaledTouchSlop();
                    mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
                }

                switch (event.getAction()) {
                    case (MotionEvent.ACTION_DOWN):
                        long time = Utils.getTimeNow();
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startActivity(new Intent(getActivity(), ScreensaverActivity.class));
                            }
                        }, mLongPressTimeout);
                        mLastTouchX = event.getX();
                        mLastTouchY = event.getY();
                        return true;
                    case (MotionEvent.ACTION_MOVE):
                        float xDiff = Math.abs(event.getX() - mLastTouchX);
                        float yDiff = Math.abs(event.getY() - mLastTouchY);
                        if (xDiff >= mMaxMovementAllowed || yDiff >= mMaxMovementAllowed) {
                            mHandler.removeCallbacksAndMessages(null);
                        }
                        break;
                    default:
                        mHandler.removeCallbacksAndMessages(null);
                }
                return false;
            }
        };

        // On tablet landscape, the clock frame will be a distinct view. Otherwise, it'll be added
        // on as a header to the main listview.
        mClockFrame = v.findViewById(R.id.clock_frame);
        mTime = (TextClock) mClockFrame.findViewById(R.id.time);
        mDate = (TextView) mClockFrame.findViewById(R.id.date);
        mWeekday = (TextView)mClockFrame.findViewById(R.id.weekday);
        mHairline = v.findViewById(R.id.divider);

//        mList.setOnTouchListener(longPressNightMode);

//        mDigitalClock = mClockFrame.findViewById(R.id.digital_clock);
//        Utils.setTimeFormat(getActivity(),
//            (TextClock) mDigitalClock.findViewById(R.id.digital_clock),
//            getResources().getDimensionPixelSize(R.dimen.main_ampm_font_size));
        mAnalogClock = mClockFrame.findViewById(R.id.analog_clock);

//        View footerView = inflater.inflate(R.layout.blank_footer_view, mList, false);
//        mList.addFooterView(footerView, null, false);
        mAdapter = new WorldClockAdapter2(getActivity(), this);
        if (mAdapter.getCount() == 0) {
            mHairline.setVisibility(View.GONE);
        }
        mList.setAdapter(mAdapter);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        final DeskClock activity = (DeskClock) getActivity();
        if (activity.getSelectedTab() == DeskClock.CLOCK_TAB_INDEX) {
            setFabAppearance();
            setLeftRightButtonAppearance();
        }

        activity.registerPageChangedListener(this);

        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);

        Utils.setQuarterHourUpdater(mHandler, mQuarterHourUpdater);
        // Besides monitoring when quarter-hour changes, monitor other actions that
        // effect clock time
        IntentFilter filter = new IntentFilter();
        filter.addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        activity.registerReceiver(mIntentReceiver, filter);

        // Resume can invoked after changing the cities list or a change in locale
        if (mAdapter != null) {
            mAdapter.loadCitiesDb(activity);
            mAdapter.reloadData(activity);
        }

        // Center the main clock frame if cities are empty.
        if (getView().findViewById(R.id.main_clock_left_pane) != null && mAdapter.getCount() == 0) {
            mList.setVisibility(View.GONE);
        } else {
            mList.setVisibility(View.VISIBLE);
            /**
             * M: Reset the mHairline's visibility. The condition of hairline's
             * state is visible. @{
             */
            if (mAdapter.getCount() > 0) {
                mHairline.setVisibility(View.VISIBLE);
            } else {
                mHairline.setVisibility(View.GONE);
            }
            /** @} */
        }
        mAdapter.notifyDataSetChanged();

//        Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mClockFrame);
        updateDateAndWeekday();
//        Utils.refreshAlarm(activity, mClockFrame);
        if (PRE_L_DEVICE) {
            activity.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.NEXT_ALARM_FORMATTED),
                false,
                mAlarmObserver);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        Utils.cancelQuarterHourUpdater(mHandler, mQuarterHourUpdater);
        Activity activity = getActivity();
        activity.unregisterReceiver(mIntentReceiver);
        if (PRE_L_DEVICE) {
            activity.getContentResolver().unregisterContentObserver(mAlarmObserver);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(BUTTONS_HIDDEN_KEY, mButtonsHidden);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key == SettingsActivity.KEY_CLOCK_STYLE) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onFabClick(View view) {
        final Activity activity = getActivity();
        startActivity(new Intent(activity, CitiesActivity2.class));
    }

    @Override
    public void setFabAppearance() {
        final DeskClock activity = (DeskClock) getActivity();
        if (mFab == null || activity.getSelectedTab() != DeskClock.CLOCK_TAB_INDEX) {
            return;
        }

        if (!isClockSelectionMode()) {
            mFab.setVisibility(View.VISIBLE);
        }
//        mFab.setImageResource(R.drawable.ic_globe);
        mFab.setImageResource(R.drawable.ic_fab_plus);
        mFab.setContentDescription(getString(R.string.button_cities));
    }

    @Override
    public void setLeftRightButtonAppearance() {
        final DeskClock activity = (DeskClock) getActivity();
        if (mLeftButton == null || mRightButton == null ||
                activity.getSelectedTab() != DeskClock.CLOCK_TAB_INDEX) {
            return;
        }
        mLeftButton.setVisibility(View.INVISIBLE);
        mRightButton.setVisibility(View.INVISIBLE);
    }

    private WorldClockFragmentHost mHost;

    public interface WorldClockFragmentHost {
        public void onWorldClockItemClicked(boolean isLongClick, String id);
        public boolean isWorldClockItemSelected(String id);
        public boolean isClockSelectionMode();
    }

    public void setHost(final WorldClockFragmentHost host) {
        if (host != null) {
            mHost = host;
        } else {
            // TODO: 17-5-3 Error Log
        }
    }

    @Override
    public void onWorldClockItemClicked(boolean isLongClick, String id) {
        if (mHost != null) {
            mHost.onWorldClockItemClicked(isLongClick, id);
        }
    }

    @Override
    public boolean isWorldClockItemSelected(String id) {
        if (mHost != null) {
            return mHost.isWorldClockItemSelected(id);
        }
        return false;
    }

    @Override
    public boolean isClockSelectionMode() {
        if (mHost != null) {
            return mHost.isClockSelectionMode();
        }
        return false;
    }

    @Override
    public void onPageChanged(int page) {
        mAdapter.reloadData(getActivity());
        mAdapter.notifyDataSetChanged();
        if (mAdapter.getCount() > 0) {
            mHairline.setVisibility(View.VISIBLE);
        } else {
            mHairline.setVisibility(View.GONE);
        }
    }

    public void updateDateAndWeekday() {
        Calendar calendar = Calendar.getInstance();
        String date = getActivity().getResources().getString(R.string.clock_list_date);
        mDate.setText(String.format(date, calendar.get(Calendar.MONTH)+1, calendar.get(Calendar.DAY_OF_MONTH)));
        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] dayList = dfs.getWeekdays();
        mWeekday.setText(dayList[calendar.get(Calendar.DAY_OF_WEEK)]);
    }
}
