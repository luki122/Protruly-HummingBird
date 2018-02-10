/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.stopwatch.StopwatchFragment;
import com.android.deskclock.stopwatch.StopwatchService;
import com.android.deskclock.stopwatch.Stopwatches;
import com.android.deskclock.timer2.TimerFragment;
import com.android.deskclock.timer.TimerObj;
import com.android.deskclock.timer.Timers;
import com.android.deskclock.worldclock.Cities;
import com.android.deskclock.worldclock.CityObj;
import com.android.deskclock.worldclock.WorldClockAdapter2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import hb.app.HbActivity;
import hb.widget.ViewPager;
import hb.widget.tab.TabLayout;
import hb.widget.toolbar.Toolbar;
//import hb.widget.FragmentPagerAdapter;

/**
 * DeskClock clock view for desk docks.
 */
public class DeskClock extends HbActivity
        implements LabelDialogFragment.TimerLabelDialogHandler,
        LabelDialogFragment.AlarmLabelDialogHandler,
        AlarmClockFragment2.AlarmClockFragmentHost,
        AlarmViewPager.ViewPagerScrollHost,
        ClockFragment2.WorldClockFragmentHost {

    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "DeskClock";

    // Alarm action for midnight (so we can update the date display).
    private static final String KEY_SELECTED_TAB = "selected_tab";
    public static final String SELECT_TAB_INTENT_EXTRA = "deskclock.select.tab";

    // Request code used when SettingsActivity is launched.
    private static final int REQUEST_CHANGE_SETTINGS = 1;

    public static final int ALARM_TAB_INDEX = 0;
    public static final int CLOCK_TAB_INDEX = 1;
    public static final int STOPWATCH_TAB_INDEX = 2;
    public static final int TIMER_TAB_INDEX = 3;

    private static final int REQUEST_APP_PERMISSIONS = 4;
    private static final int REQUEST_APP_PERMISSIONS_CREATE = 5;

    // Tabs indices are switched for right-to-left since there is no
    // native support for RTL in the ViewPager.
   /* public static final int RTL_ALARM_TAB_INDEX = 3;
    public static final int RTL_CLOCK_TAB_INDEX = 2;
    public static final int RTL_TIMER_TAB_INDEX = 1;
    public static final int RTL_STOPWATCH_TAB_INDEX = 0;*/

    // TODO(rachelzhang): adding a broadcast receiver to adjust color when the timezone/time
    // changes in the background.

    private Menu mMenu;
//    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private AlarmViewPager mViewPager;
    private ImageButton mFab;
    private ImageButton mLeftButton;
    private ImageButton mRightButton;

    private TabsAdapter mTabsAdapter;
    private int mSelectedTab;
    private boolean mActivityResumed;
    private boolean mIsAlarmMultiSelectedMode, mIsClockMultiSelectedMode;

    private Toolbar mToolbar;
    private TextView mTitle;
    private TextView mLeftTitleBtn;
    private TextView mRightTitleBtn;
    private TextView mDeleteBtn;

    private HashSet<Long> mSelectedSet = new HashSet<>();
    private HashMap<Long, Boolean> mAlarms;

    private HashSet<String> mClockSelectedSet = new HashSet<>();
    private HashMap<String, Boolean> mClocks;

    @Override
    public void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        if (DEBUG) Log.d(LOG_TAG, "onNewIntent with intent: " + newIntent);

        // update our intent so that we can consult it to determine whether or
        // not the most recent launch was via a dock event
        setIntent(newIntent);

        // Timer receiver may ask to go to the timers fragment if a timer expired.
        int tab = newIntent.getIntExtra(SELECT_TAB_INTENT_EXTRA, -1);
        if (tab != -1) {
            mSelectedTab = tab;
            mViewPager.setCurrentItem(mSelectedTab);
        }
    }

    /** M: @{ --Menu Key handling--  */
    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v) {
            @Override
            public void show() {
                onPrepareOptionsMenu(getMenu());
                super.show();
            }
        };
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.desk_clock_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onOptionsItemSelected(item);
            }
        });
        popup.show();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

//        if (keyCode == KeyEvent.KEYCODE_MENU) {
//            /* Handle the menu key only for alarm and world clock */
//            if (mMenu != null) {
//                // Make sure the menu's been initialized.
//                if (mSelectedTab == ALARM_TAB_INDEX
//                        || mSelectedTab == CLOCK_TAB_INDEX) {
//                    View menuButton = findViewById(R.id.menu_button);
//                    showPopup(menuButton);
//                }
//            }
//
//        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (mSelectedTab == STOPWATCH_TAB_INDEX) {
                if (getSelectedFragment() instanceof StopwatchFragment) {
                    ((StopwatchFragment) getSelectedFragment()).handleMediaKeyDoLap();
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /** @} --Menu Key handling-- */

    private void initViews() {
        setContentView(R.layout.desk_clock);
        initToolBar();
        mFab = (ImageButton) findViewById(R.id.fab);
        mLeftButton = (ImageButton) findViewById(R.id.left_button);
        mRightButton = (ImageButton) findViewById(R.id.right_button);
        if (mTabsAdapter == null) {
            mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
            mViewPager = (AlarmViewPager) findViewById(R.id.view_pager);

            // Keep all four tabs to minimize jank.
            mViewPager.setOffscreenPageLimit(3);
            // Set Accessibility Delegate to null so ViewPager doesn't intercept movements and
            // prevent the fab from being selected.
            mViewPager.setAccessibilityDelegate(null);
            mViewPager.setViewPagerScrollHost(this);

            mTabsAdapter = new TabsAdapter(this, mViewPager);
            createTabs(mSelectedTab);
//            mTabLayout.setupWithViewPager(mViewPager);
            // 不要使用ViewPager的onPageChangeListener 绑定之后使用onTabSelectedListener
            // 同时 setOnTabSelectedListener要在setupWithViewPager之后设置
            mTabLayout.setOnTabSelectedListener(mTabsAdapter);
            mViewPager.setOnPageChangeListener(mTabsAdapter);
            mTabLayout.setSelectedTabIndicatorColor(Color.WHITE);
            // mTabLayout.setTabsFromPagerAdapter(mTabsAdapter);

        }

        mFab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                getSelectedFragment().onFabClick(view);
            }
        });
        mLeftButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                getSelectedFragment().onLeftButtonClick(view);
            }
        });
        mRightButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                getSelectedFragment().onRightButtonClick(view);
            }
        });

        // TODO: 17-4-18 set selected navigation item  : mSelectedTab
        Log.d("item","item->"+mSelectedTab);
        mViewPager.setCurrentItem(mSelectedTab);
    }



    @VisibleForTesting
    DeskClockFragment getSelectedFragment() {
        /// M: No need do the RTL position translate
        return (DeskClockFragment) mTabsAdapter.getItem(mSelectedTab);
    }

    private void createTabs(int selectedIndex) {
        final TabLayout.Tab alarmTab = mTabLayout.newTab();
        alarmTab.setText(R.string.menu_alarm);
        alarmTab.setIcon(R.drawable.ic_tab_alarm);
        mTabsAdapter.addTab(alarmTab,
                Utils.isLOrLater()
                ? AlarmClockFragmentPostL2.class
                : AlarmClockFragmentPreL.class,
                ALARM_TAB_INDEX, R.string.menu_alarm);
        mTabLayout.addTab(alarmTab);

        final TabLayout.Tab clockTab = mTabLayout.newTab();
        clockTab.setText(R.string.menu_clock);
        clockTab.setIcon(R.drawable.ic_tab_clock);
        mTabsAdapter.addTab(clockTab, ClockFragment2.class, CLOCK_TAB_INDEX, R.string.menu_clock);
        mTabLayout.addTab(clockTab);

        final TabLayout.Tab stopwatchTab = mTabLayout.newTab();
        stopwatchTab.setText(R.string.menu_stopwatch);
        stopwatchTab.setIcon(R.drawable.ic_tab_stopwatch);
        mTabsAdapter.addTab(stopwatchTab, StopwatchFragment.class, STOPWATCH_TAB_INDEX, R.string.menu_stopwatch);
        mTabLayout.addTab(stopwatchTab);

        final TabLayout.Tab timerTab = mTabLayout.newTab();
        timerTab.setText(R.string.menu_timer);
        timerTab.setIcon(R.drawable.ic_tab_timer);
        mTabsAdapter.addTab(timerTab, TimerFragment.class, TIMER_TAB_INDEX, R.string.menu_timer);
        mTabLayout.addTab(timerTab);

        mTabsAdapter.notifySelectedPage(mSelectedTab);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        if (RequestPermissionsActivity.startPermissionActivity(this)) {
            finish();
        }
        if (icicle != null) {
            icicle = null;
        }

        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_ALARM);

        if (icicle != null) {
            mSelectedTab = icicle.getInt(KEY_SELECTED_TAB, ALARM_TAB_INDEX);
        } else {
            mSelectedTab = ALARM_TAB_INDEX;
//            mSelectedTab = CLOCK_TAB_INDEX;
            // Set the background color to initially match the theme value so that we can
            // smoothly transition to the dynamic color.
            // setBackgroundColor(getResources().getColor(R.color.default_background),
            //         false *//* animate *//*);
        }
        // Timer receiver may ask the app to go to the timer fragment if a timer expired
        Intent i = getIntent();
        if (i != null) {
            int tab = i.getIntExtra(SELECT_TAB_INTENT_EXTRA, -1);
            if (tab != -1) {
                mSelectedTab = tab;
            }
        }
        initViews();
        setHomeTimeZone();

        // We need to update the system next alarm time on app startup because the
        // user might have clear our data.
        AlarmStateManager.updateNextAlarm(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // We only want to show notifications for stopwatch/timer when the app is closed so
        // that we don't have to worry about keeping the notifications in perfect sync with
        // the app.
        Intent stopwatchIntent = new Intent(getApplicationContext(), StopwatchService.class);
        stopwatchIntent.setAction(Stopwatches.KILL_NOTIF);
        startService(stopwatchIntent);

        /*SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Timers.NOTIF_APP_OPEN, true);
        editor.apply();
        Intent timerIntent = new Intent();
        timerIntent.setAction(Timers.NOTIF_IN_USE_CANCEL);
        sendBroadcast(timerIntent);*/
        DataModel.getDataModel().setApplicationInForeground(true);

        mActivityResumed = true;

    }


    @Override
    public void onPause() {
        mActivityResumed = false;
        Intent intent = new Intent(getApplicationContext(), StopwatchService.class);
        intent.setAction(Stopwatches.SHOW_NOTIF);
        startService(intent);

        /*SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Timers.NOTIF_APP_OPEN, false);
        editor.apply();
        Utils.showInUseNotifications(this);*/
        DataModel.getDataModel().setApplicationInForeground(false);

        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mTabLayout != null){
            outState.putInt(KEY_SELECTED_TAB, mTabLayout.getSelectedTabPosition());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // We only want to show it as a menu in landscape, and only for clock/alarm fragment.
        mMenu = menu;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            if (mActionBar != null) {
//            if (mActionBar.getSelectedNavigationIndex() == ALARM_TAB_INDEX ||
//                    mActionBar.getSelectedNavigationIndex() == CLOCK_TAB_INDEX) {
//                // Clear the menu so that it doesn't get duplicate items in case onCreateOptionsMenu
//                // was called multiple times.
//                menu.clear();
//                getMenuInflater().inflate(R.menu.desk_clock_menu, menu);
//            }
//            }
            // Always return true for landscape, regardless of whether we've inflated the menu, so
            // that when we switch tabs this method will get called and we can inflate the menu.
            return true;
        }
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateMenu(menu);
        return true;
    }

    private void updateMenu(Menu menu) {
        // Hide "help" if we don't have a URI for it.
        MenuItem help = menu.findItem(R.id.menu_item_help);
        if (help != null) {
            Utils.prepareHelpMenuItem(this, help);
        }

        // Hide "lights out" for timer.
        MenuItem nightMode = menu.findItem(R.id.menu_item_night_mode);
//      if (mActionBar != null) {
//        if (mActionBar.getSelectedNavigationIndex() == ALARM_TAB_INDEX) {
//            nightMode.setVisible(false);
//        } else if (mActionBar.getSelectedNavigationIndex() == CLOCK_TAB_INDEX) {
//            nightMode.setVisible(true);
//        }
//      }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (processMenuClick(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Recreate the activity if any settings have been changed
       /// M:code is commented as recreate disturbing  activity resume cycle
       // if (requestCode == REQUEST_CHANGE_SETTINGS && resultCode == RESULT_OK) {
           // recreate();
        //}
    }

    @Override
    public void onBackPressed() {
        if (isSelectionMode()) {
            exitAlarmMultiSelectState();
        } else if (isClockSelectionMode()){
            exitClockMultiSelectActionMode();
        } else {
            super.onBackPressed();
        }
    }

    private boolean processMenuClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_settings:
                startActivityForResult(new Intent(DeskClock.this, SettingsActivity.class),
                        REQUEST_CHANGE_SETTINGS);
                return true;
            case R.id.menu_item_help:
                Intent i = item.getIntent();
                if (i != null) {
                    try {
                        startActivity(i);
                    } catch (ActivityNotFoundException e) {
                        // No activity found to match the intent - ignore
                    }
                }
                return true;
            case R.id.menu_item_night_mode:
                startActivity(new Intent(DeskClock.this, ScreensaverActivity.class));
            default:
                break;
        }
        return true;
    }

    /**
     * Insert the local time zone as the Home Time Zone if one is not set
     */
    private void setHomeTimeZone() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String homeTimeZone = prefs.getString(SettingsActivity.KEY_HOME_TZ, "");
        if (!homeTimeZone.isEmpty()) {
            return;
        }
        homeTimeZone = TimeZone.getDefault().getID();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SettingsActivity.KEY_HOME_TZ, homeTimeZone);
        editor.apply();
        Log.v(LOG_TAG, "Setting home time zone to " + homeTimeZone);
    }

    public void registerPageChangedListener(DeskClockFragment frag) {
        if (mTabsAdapter != null) {
            mTabsAdapter.registerPageChangedListener(frag);
        }
    }

    public void unregisterPageChangedListener(DeskClockFragment frag) {
        if (mTabsAdapter != null) {
            mTabsAdapter.unregisterPageChangedListener(frag);
        }
    }

    /**
     * Adapter for wrapping together the ActionBar's tab with the ViewPager
     */
    private class TabsAdapter extends hb.widget.FragmentPagerAdapter implements
            TabLayout.OnTabSelectedListener, ViewPager.OnPageChangeListener{
        private static final String KEY_TAB_POSITION = "tab_position";
        private static final String KEY_TAB_NAME = "tab_name";

        final class TabInfo{
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(Class<?> _class, int position, int nameId) {
                clss = _class;
                args = new Bundle();
                args.putInt(KEY_TAB_POSITION, position);
                args.putInt(KEY_TAB_NAME, nameId);
            }

            public int getPosition() {
                return args.getInt(KEY_TAB_POSITION, 0);
            }

            public int getStringId() {
                return args.getInt(KEY_TAB_NAME, -1);
            }
        }

        private final ArrayList<TabInfo> mTabs = new ArrayList<>();
        Context mContext;
        hb.widget.ViewPager mViewPager;
        // Used for doing callbacks to fragments.
        HashSet<String> mFragmentTags = new HashSet<String>();

        public TabsAdapter(HbActivity activity, hb.widget.ViewPager viewPager) {
            super(activity.getFragmentManager());
            mContext = activity;
            mViewPager = viewPager;
            mViewPager.setAdapter(this);
//            mViewPager.setOnPageChangeListener(this);
        }

        @Override
        public Fragment getItem(int position) {
            // Because this public method is called outside many times,
            // check if it exits first before creating a new one.cd app
            final String name = makeFragmentName(R.id.view_pager, position);
            Fragment fragment = getFragmentManager().findFragmentByTag(name);
            if (fragment == null) {
                /// M: No need do the RTL position translate
                TabInfo info = mTabs.get(position);
                fragment = Fragment.instantiate(mContext, info.clss.getName(), info.args);
                // TODO: 17-4-17
                if (fragment instanceof TimerFragment) {
                    ((TimerFragment) fragment).setFabAppearance();
                    ((TimerFragment) fragment).setLeftRightButtonAppearance();
                }
                if (fragment instanceof AlarmClockFragment2) {
                    ((AlarmClockFragment2)fragment).setHost(DeskClock.this);
                }

                if (fragment instanceof ClockFragment2) {
                    ((ClockFragment2)fragment).setHost(DeskClock.this);
                }
            }
            return fragment;
        }


        /**
         * Copied from:
         * android/frameworks/support/v13/java/android/support/v13/app/FragmentPagerAdapter.java#94
         * Create unique name for the fragment so fragment manager knows it exist.
         */
        private String makeFragmentName(int viewId, int index) {
            return "android:switcher:" + viewId + ":" + index;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            CharSequence cs = null;

            TabInfo info = mTabs.get(position);
            int stringId = info.getStringId();
            if (stringId != -1) {
                cs = getResources().getString(stringId);
            }

            return cs;
        }



        @Override
        public int getCount() {
            return mTabs.size();
        }

        public void addTab(TabLayout.Tab tab, Class<?> clss, int position, int nameId) {
            TabInfo info = new TabInfo(clss, position, nameId);
            tab.setTag(info);
            mTabs.add(info);
            notifyDataSetChanged();
        }

        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            final int position = tab.getPosition();
//            final TabInfo info = (TabInfo) tab.getTag();
//            final int position = info.getPosition();
            /// M: not required, variable "position" is enough
            //final int rtlSafePosition = getRtlPosition(position);
            mSelectedTab = position;

            if (mActivityResumed) {
                switch (mSelectedTab) {
                    case ALARM_TAB_INDEX:
                        Events.sendAlarmEvent(R.string.action_show, R.string.label_deskclock);
                        break;
                    case CLOCK_TAB_INDEX:
                        Events.sendClockEvent(R.string.action_show, R.string.label_deskclock);
                        break;
                    case TIMER_TAB_INDEX:
                        Events.sendTimerEvent(R.string.action_show, R.string.label_deskclock);
                        break;
                    case STOPWATCH_TAB_INDEX:
                        Events.sendStopwatchEvent(R.string.action_show, R.string.label_deskclock);
                        break;
                }
            }

//            final DeskClockFragment f = (DeskClockFragment) getItem(position);
//            if (f != null) {
//                f.setFabAppearance();
//                f.setLeftRightButtonAppearance();
//            }
            /// M: No need do the RTL position translate
            mViewPager.setCurrentItem(position);
            notifyPageChanged(position);
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
            //DO NOTHING
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
            //DO NOTHING
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            //DO NOTHING
//            final DeskClockFragment f = (DeskClockFragment) getItem(position);
//            if (f != null) {
//                f.setFabAppearance();
//                f.setLeftRightButtonAppearance();
//            }
            mTabLayout.setScrollPosition(position, positionOffset, true);
        }

        @Override
        public void onPageSelected(int position) {
            mSelectedTab = position;
            notifyPageChanged(position);
        }

        public void notifySelectedPage(int page) {
            notifyPageChanged(page);
        }

        private void notifyPageChanged(int newPage) {
            for (String tag : mFragmentTags) {
                final FragmentManager fm = getFragmentManager();
                DeskClockFragment f = (DeskClockFragment) fm.findFragmentByTag(tag);
                if (f != null) {
                    f.onPageChanged(newPage);
                    if (!isSelectionMode() && !isClockSelectionMode()) {
                        f.setFabAppearance();
                        f.setLeftRightButtonAppearance();
                    }
                }
            }
        }
        @Override
        public void onPageScrollStateChanged(int i) {
            //DO NOTHING
        }

        public void registerPageChangedListener(DeskClockFragment frag) {
            String tag = frag.getTag();
            if (mFragmentTags.contains(tag)) {
//                Log.wtf(LOG_TAG, "Trying to add an existing fragment " + tag);
            } else {
                mFragmentTags.add(frag.getTag());
            }
            // Since registering a listener by the fragment is done sometimes after the page
            // was already changed, make sure the fragment gets the current page
            frag.onPageChanged(mTabLayout.getSelectedTabPosition());
        }

        public void unregisterPageChangedListener(DeskClockFragment frag) {
            mFragmentTags.remove(frag.getTag());
        }

    }

    /**
     * Called by the LabelDialogFormat class after the dialog is finished. *
     */
    @Override
    public void onDialogLabelSet(TimerObj timer, String label, String tag) {
        /*Fragment frag = getFragmentManager().findFragmentByTag(tag);
        if (frag instanceof TimerFragment) {
            ((TimerFragment) frag).setLabel(timer, label);
        }*/
    }

    /**
     * Called by the LabelDialogFormat class after the dialog is finished. *
     */
    @Override
    public void onDialogLabelSet(Alarm alarm, String label, String tag) {
        Fragment frag = getFragmentManager().findFragmentByTag(tag);
        if (frag instanceof AlarmClockFragment) {
            ((AlarmClockFragment) frag).setLabel(alarm, label);
        }
    }

    public int getSelectedTab() {
        return mSelectedTab;
    }

    public ImageButton getFab() {
        return mFab;
    }

    public ImageButton getLeftButton() {
        return mLeftButton;
    }

    public ImageButton getRightButton() {
        return mRightButton;
    }

    @Override
    public boolean isSelectionMode() {
        return mIsAlarmMultiSelectedMode;
    }

    @Override
    public boolean isAlarmItemSelected(long id) {
        if (isSelectionMode() && mSelectedSet.contains(new Long(id))) {
            return true;
        }
        return false;
    }

    @Override
    public void onAlarmItemClick(boolean isLongClick, long id) {
        if (isLongClick && !isSelectionMode()) {
            startAlarmMultiSelectActionMode();
        }

        if (isSelectionMode()) {
            if (mSelectedSet.contains(new Long(id))) {
                mSelectedSet.remove(new Long(id));
                if (mAlarms.containsKey(id)) {
                    mAlarms.put(id, false);
                }
                mHasSelectedNum--;
            } else {
                mSelectedSet.add(new Long(id));
                if (mAlarms.containsKey(id)) {
                    mAlarms.put(id, true);
                }
                mHasSelectedNum++;
            }

            if (isAllChecked()) {
                mRightTitleBtn.setText(getString(R.string.alarm_unselected_all));
            } else {
                mRightTitleBtn.setText(getString(R.string.alarm_selected_all));
            }

            updateTitle();
        } else {
            Fragment f = mTabsAdapter.getItem(mSelectedTab);
            if (f instanceof AlarmClockFragment2) {
                ((AlarmClockFragment2)f).showTimeEditDialog(((AlarmClockFragment2)f).mSelectedAlarm);
            }
        }
    }


    protected void startAlarmMultiSelectActionMode() {
        // TODO: 17-5-3
        mIsAlarmMultiSelectedMode = true;
        mFab.setVisibility(View.GONE);
        mToolbar.setVisibility(View.VISIBLE);
        mDeleteBtn.setVisibility(View.VISIBLE);
        // initial hash map
        initMultiSelectedHashMap();
        updateTitle();
        mRightTitleBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAllChecked()) {
                    mSelectedSet.clear();
                    initMultiSelectedHashMap();
                    mRightTitleBtn.setText(getString(R.string.alarm_selected_all));
                } else {
                    mAlarms.clear();
                    mSelectedSet.clear();
                    List<Alarm> alarms = Alarm.getAlarms(getContentResolver(), null);
                    for (int i = 0; i < alarms.size(); ++i) {
                        mAlarms.put(alarms.get(i).id, true);
                        mSelectedSet.add(alarms.get(i).id);
                    }
                    mHasSelectedNum = mAlarms.size();
                    mRightTitleBtn.setText(getString(R.string.alarm_unselected_all));
                }
                updateTitle();
                if (mSelectedTab == ALARM_TAB_INDEX) {
                    mTabsAdapter.notifySelectedPage(mSelectedTab);
                }
            }
        });

        mLeftTitleBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                exitAlarmMultiSelectState();
            }
        });

        mDeleteBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelectedSet.clear();
                List<Alarm> alarms = Alarm.getAlarms(getContentResolver(), null);
                for (int i = 0; i < alarms.size(); ++i) {
                    if (mAlarms.get(alarms.get(i).id)) {
                        asyncDeleteAlarm(alarms.get(i).id);
                    }
                }
                exitAlarmMultiSelectState();
            }
        });
        
    }

    protected void exitAlarmMultiSelectState() {
        // TODO: 17-5-3
        mIsAlarmMultiSelectedMode = false;
        mToolbar.setVisibility(View.GONE);
        mDeleteBtn.setVisibility(View.GONE);
        if (mSelectedTab == ALARM_TAB_INDEX) {
            mTabsAdapter.notifySelectedPage(mSelectedTab);
        }
        mSelectedSet.clear();
        mFab.setVisibility(View.VISIBLE);

    }

    private int mHasSelectedNum;
    private void initMultiSelectedHashMap() {
        if (mAlarms == null) {
            mAlarms = new HashMap<>();
        }

        mAlarms.clear();
        List<Alarm> alarms = Alarm.getAlarms(getContentResolver(), null);
        for (int i = 0; i < alarms.size(); ++i) {
            mAlarms.put(alarms.get(i).id, false);
        }
        mHasSelectedNum = 0;
    }

    private boolean isAllChecked() {
        return !mAlarms.containsValue(false);
    }


    @Override
    public void onWorldClockItemClicked(boolean isLongClick, String id) {
        if (isLongClick && !isClockSelectionMode()) {
            startClockMultiSelectActionMode();
        }

        if (isClockSelectionMode()) {
            if (mClockSelectedSet.contains(id)) {
                mClockSelectedSet.remove(id);
                if (mClocks.containsKey(id)) {
                    mClocks.put(id, false);
                }
                mHasClockSelectedNum--;
            } else {
                mClockSelectedSet.add(id);
                if (mClocks.containsKey(id)) {
                    mClocks.put(id, true);
                }
                mHasClockSelectedNum++;
            }

            if (isClockAllChecked()) {
                mRightTitleBtn.setText(getString(R.string.alarm_unselected_all));
            } else {
                mRightTitleBtn.setText(getString(R.string.alarm_selected_all));
            }

            updateClockTitle();
        }
    }

    @Override
    public boolean isWorldClockItemSelected(String id) {
        if (isClockSelectionMode() && mClockSelectedSet.contains(id)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isClockSelectionMode() {
        return mIsClockMultiSelectedMode;
    }

    private void startClockMultiSelectActionMode() {
        mIsClockMultiSelectedMode = true;
        mFab.setVisibility(View.GONE);
        mToolbar.setVisibility(View.VISIBLE);
        mDeleteBtn.setVisibility(View.VISIBLE);
        // initial hash map
        initClockMultiSelectedHashMap();
        updateClockTitle();
        mRightTitleBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isClockAllChecked()) {
                    mClockSelectedSet.clear();
                    initClockMultiSelectedHashMap();
                    mRightTitleBtn.setText(getString(R.string.alarm_selected_all));
                } else {
                    mClocks.clear();
                    mClockSelectedSet.clear();
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(DeskClock.this);
                    Object [] citiesList = Cities.readCitiesFromSharedPrefs(prefs).values().toArray();
                    for (int i = 0; i < citiesList.length; ++i) {
                        mClocks.put(((CityObj)citiesList[i]).mCityId, true);
                        mClockSelectedSet.add(((CityObj)citiesList[i]).mCityId);
                    }
                    mHasClockSelectedNum = mClocks.size();
                    mRightTitleBtn.setText(getString(R.string.alarm_unselected_all));
                }
                updateClockTitle();
                if (mSelectedTab == CLOCK_TAB_INDEX) {
                    mTabsAdapter.notifySelectedPage(mSelectedTab);
                }
            }
        });

        mLeftTitleBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                exitClockMultiSelectActionMode();
            }
        });

        mDeleteBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 17-5-18
                SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(DeskClock.this);
                HashMap<String, CityObj> map = Cities.readCitiesFromSharedPrefs(sPref);
                if (map.containsKey(WorldClockAdapter2.HOME_CITY_ID)
                        && sPref.getBoolean(SettingsActivity.KEY_AUTO_HOME_CLOCK, false)) {
                    SharedPreferences.Editor editor = sPref.edit();
                    editor.putBoolean(SettingsActivity.KEY_HOME_HAS_DELETED, true);
                    editor.apply();
                }

                for (String s : mClockSelectedSet) {
                    if (map.containsKey(s)) {
                        map.remove(s);
                    }
                }
                Cities.saveCitiesToSharedPrefs(sPref, map);
                mClockSelectedSet.clear();
                exitClockMultiSelectActionMode();
            }
        });
    }

    private void exitClockMultiSelectActionMode() {
        mIsClockMultiSelectedMode = false;
        mToolbar.setVisibility(View.GONE);
        mDeleteBtn.setVisibility(View.GONE);
        if (mSelectedTab == CLOCK_TAB_INDEX) {
            mTabsAdapter.notifySelectedPage(mSelectedTab);
        }
        mClockSelectedSet.clear();
        mFab.setVisibility(View.VISIBLE);
    }

    private int mHasClockSelectedNum;
    private void initClockMultiSelectedHashMap() {
        if (mClocks == null) {
            mClocks = new HashMap<>();
        }

        mClocks.clear();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(DeskClock.this);
        Object [] citiesList = Cities.readCitiesFromSharedPrefs(prefs).values().toArray();
        for (int i = 0; i < citiesList.length; ++i) {
            mClocks.put(((CityObj)citiesList[i]).mCityId, false);
        }
        mHasClockSelectedNum = 0;
    }

    private boolean isClockAllChecked() {
        return !mClocks.containsValue(false);
    }

    private void initToolBar() {
        mToolbar = (Toolbar) findViewById(R.id.alarm_list_toolbar);
        mToolbar.showBottomDivider(false);
        mTitle = (TextView) mToolbar.findViewById(R.id.toolbar_title);
        mLeftTitleBtn = (TextView) mToolbar.findViewById(R.id.title_left_button);
        mRightTitleBtn = (TextView) mToolbar.findViewById(R.id.title_right_button);
        mDeleteBtn = (TextView) findViewById(R.id.delete_btn);
    }

    private void updateTitle() {
        String titleFormat = getResources().getString(R.string.alarm_has_selected_num);
        String title = String.format(titleFormat, mHasSelectedNum);
        mTitle.setText(title);
        if (mHasSelectedNum == 0) {
            mDeleteBtn.setTextColor(getColor(R.color.delete_btn_disable));
            mDeleteBtn.setEnabled(false);
        } else {
            mDeleteBtn.setTextColor(Color.BLACK);
            mDeleteBtn.setEnabled(true);
        }
    }

    private void updateClockTitle() {
        String titleFormat = getResources().getString(R.string.alarm_has_selected_num);
        String title = String.format(titleFormat, mHasClockSelectedNum);
        mTitle.setText(title);
        if (mHasClockSelectedNum == 0) {
            mDeleteBtn.setTextColor(getColor(R.color.delete_btn_disable));
            mDeleteBtn.setEnabled(false);
        } else {
            mDeleteBtn.setTextColor(Color.BLACK);
            mDeleteBtn.setEnabled(true);
        }
    }

    @Override
    public boolean isScrollEventIntercepted() {
        return isSelectionMode() || isClockSelectionMode();
    }

    private void asyncDeleteAlarm(final long id) {
        final Context context = getApplicationContext();
        final AsyncTask<Void, Void, Void> deleteTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... parameters) {
                // Activity may be closed at this point , make sure data is still valid
                if (context != null) {
                    Events.sendAlarmEvent(R.string.action_delete, R.string.label_deskclock);

                    ContentResolver cr = context.getContentResolver();
                    AlarmStateManager.deleteAllInstances(context, id);
                    Alarm.deleteAlarm(cr, id);
                }
                return null;
            }
        };
        deleteTask.execute();
    }
}
