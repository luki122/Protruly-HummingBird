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

package com.android.deskclock.worldclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.deskclock.R;
import com.android.deskclock.SettingsActivity;
import com.android.deskclock.Utils;

import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class WorldClockAdapter2 extends BaseAdapter {
    protected Object [] mCitiesList;
    private final LayoutInflater mInflater;
    private final Context mContext;
//    private String mClockStyle;
    private final Collator mCollator = Collator.getInstance();
    protected HashMap<String, CityObj> mCitiesDb = new HashMap<String, CityObj>();
    protected HashMap<String, CityObj> mSelectedCitiesMap = new HashMap<>();
//    protected int mClocksPerRow;

    //这样写代码要被打死的..
    public static final String HOME_CITY_ID = "C256";//Beijing
    private static final String HOME_TIME_ZONE = "Asia/Shanghai";

    public WorldClockAdapter2(Context context, HostInterface hostInterface) {
        super();
        mContext = context;
        loadData(context);
        loadCitiesDb(context);
        mCitiesList = addHomeCity();
        mInflater = LayoutInflater.from(context);
//        mClocksPerRow = context.getResources().getInteger(R.integer.world_clocks_per_row);
        mHostInterface = hostInterface;
    }

    public void reloadData(Context context) {
        loadData(context);
        notifyDataSetChanged();
    }

    public void loadData(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
//        mClockStyle = prefs.getString(SettingsActivity.KEY_CLOCK_STYLE,
//                mContext.getResources().getString(R.string.default_clock_style));
        mCitiesList = Cities.readCitiesFromSharedPrefs(prefs).values().toArray();
        sortList();
    }

    public void loadCitiesDb(Context context) {
        mCitiesDb.clear();
        // Read the cities DB so that the names and timezones will be taken from the DB
        // and not from the selected list so that change of locale or changes in the DB will
        // be reflected.
        CityObj[] cities = Utils.loadCitiesFromXml(context);
        if (cities != null) {
            for (int i = 0; i < cities.length; i ++) {
                mCitiesDb.put(cities[i].mCityId, cities [i]);
            }
        }
    }

    /***
     * Adds the home city as the first item of the adapter if the feature is on and the device time
     * zone is different from the home time zone that was set by the user.
     * return the list of cities.
     */
    private Object[] addHomeCity() {
        if (needHomeCity()) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            String homeTZ = sharedPref.getString(SettingsActivity.KEY_HOME_TZ, "");
            CityObj c = new CityObj(
                    mContext.getResources().getString(R.string.home_label), homeTZ, null, null);
            c.mCityId = HOME_CITY_ID;
            mSelectedCitiesMap = Cities.readCitiesFromSharedPrefs(sharedPref);
            if (!mSelectedCitiesMap.containsKey(HOME_CITY_ID)) {
                mSelectedCitiesMap.put(HOME_CITY_ID, c);
                Object[] temp = new Object[mCitiesList.length + 1];
                temp[0] = c;
                for (int i = 0; i < mCitiesList.length; i++) {
                    temp[i + 1] = mCitiesList[i];
                }
                Cities.saveCitiesToSharedPrefs(sharedPref, mSelectedCitiesMap);
                return temp;
            }

            return mCitiesList;
        } else {
            return mCitiesList;
        }
    }

    public void updateHomeLabel(Context context) {
        // Update the "home" label if the home time zone clock is shown
        if (needHomeCity() && mCitiesList.length > 0) {
            ((CityObj) mCitiesList[0]).mCityName =
                    context.getResources().getString(R.string.home_label);
        }
    }

    public boolean needHomeCity() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (!sharedPref.getBoolean(SettingsActivity.KEY_HOME_HAS_DELETED, false)) {
//            String homeTZ = sharedPref.getString(
//                    SettingsActivity.KEY_HOME_TZ, TimeZone.getDefault().getID());
            final Date now = new Date();
            return TimeZone.getTimeZone(HOME_TIME_ZONE).getOffset(now.getTime())
                    != TimeZone.getDefault().getOffset(now.getTime());
        } else {
            return false;
        }
    }

    public boolean hasHomeCity() {
        return (mCitiesList != null) && mCitiesList.length > 0
                && ((CityObj) mCitiesList[0]).mCityId == null;
    }

    private void sortList() {
        final Date now = new Date();

        // Sort by the Offset from GMT taking DST into account
        // and if the same sort by City Name
        Arrays.sort(mCitiesList, new Comparator<Object>() {
            private int safeCityNameCompare(CityObj city1, CityObj city2) {
                if (city1.mCityName == null && city2.mCityName == null) {
                    return 0;
                } else if (city1.mCityName == null) {
                    return -1;
                } else if (city2.mCityName == null) {
                    return 1;
                } else {
                    return mCollator.compare(city1.mCityName, city2.mCityName);
                }
            }

            @Override
            public int compare(Object object1, Object object2) {
                CityObj city1 = (CityObj) object1;
                CityObj city2 = (CityObj) object2;
                if (city1.mTimeZone == null && city2.mTimeZone == null) {
                    return safeCityNameCompare(city1, city2);
                } else if (city1.mTimeZone == null) {
                    return -1;
                } else if (city2.mTimeZone == null) {
                    return 1;
                }

                int gmOffset1 = TimeZone.getTimeZone(city1.mTimeZone).getOffset(now.getTime());
                int gmOffset2 = TimeZone.getTimeZone(city2.mTimeZone).getOffset(now.getTime());
                if (gmOffset1 == gmOffset2) {
                    return safeCityNameCompare(city1, city2);
                } else {
                    return gmOffset1 - gmOffset2;
                }
            }
        });
    }

    @Override
    public int getCount() {
//        if (mClocksPerRow == 1) {
//            // In the special case where we have only 1 clock per view.
//            return mCitiesList.length;
//        }
//
//        // Otherwise, each item in the list holds 1 or 2 clocks
//        return (mCitiesList.length  + 1)/2;
        if (mCitiesList.length > 0) {
            return mCitiesList.length + 1;
        } else {
            return mCitiesList.length;
        }
    }

    @Override
    public Object getItem(int p) {
        return null;
    }

    @Override
    public long getItemId(int p) {
        return p;
    }

    @Override
    public boolean isEnabled(int p) {
        return false;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        // Index in cities list
//        int index = position * mClocksPerRow;
        int index = position;
        if (index < 0 || index >= mCitiesList.length + 1) {
            return null;
        }

        if (view == null) {
            view = mInflater.inflate(R.layout.world_clock_list_item_2, parent, false);
        }
        if (index == mCitiesList.length) {
            view.setVisibility(View.INVISIBLE);
        } else {
            view.setVisibility(View.VISIBLE);
            updateView(view, (CityObj)mCitiesList[index]);
        }
        return view;
    }

    private void updateView(View clock, final CityObj cityObj) {
        TextView name = (TextView)(clock.findViewById(R.id.city_name));
        TextView dateView = (TextView) clock.findViewById(R.id.date);
        TextView dateViewMulti = (TextView) clock.findViewById(R.id.date_multimode);
        CheckBox checkBox = (CheckBox) clock.findViewById(R.id.checkbox);

        TextClock dclock = (TextClock)(clock.findViewById(R.id.digital_clock));
        dclock.setTimeZone(cityObj.mTimeZone);
//        Utils.setTimeFormat(mContext, dclock,
//                mContext.getResources().getDimensionPixelSize(R.dimen.label_font_size));
        CityObj cityInDb = mCitiesDb.get(cityObj.mCityId);
        // Home city or city not in DB , use data from the save selected cities list
        name.setText(Utils.getCityName(cityObj, cityInDb));

        final Calendar now = Calendar.getInstance();
        now.setTimeZone(TimeZone.getDefault());
        // Get timezone from cities DB if available
        String cityTZ = (cityInDb != null) ? cityInDb.mTimeZone : cityObj.mTimeZone;
        now.setTimeZone(TimeZone.getTimeZone(cityTZ));
        int month = now.get(Calendar.MONTH) + 1;
        int day = now.get(Calendar.DAY_OF_MONTH);

        String date, title;
       
        if (Locale.getDefault().getLanguage().equals(Locale.ENGLISH.getLanguage())) {
            date = mContext.getResources().getString(R.string.clock_list_date_en);
            String monthString = DateUtils.getMonthString(month, DateUtils.LENGTH_SHORT);
            title = String.format(date, monthString, day);
        } else {
            date = mContext.getResources().getString(R.string.clock_list_date);
            title = String.format(date, month, day);
        }

        dateView.setText(title);
        dateViewMulti.setText(title);

        if (mHostInterface.isClockSelectionMode()) {
            dateView.setVisibility(View.GONE);
            dateViewMulti.setVisibility(View.VISIBLE);
            checkBox.setVisibility(View.VISIBLE);
        } else {
            dateView.setVisibility(View.VISIBLE);
            dateViewMulti.setVisibility(View.GONE);
            checkBox.setVisibility(View.GONE);
        }

        if (mHostInterface.isWorldClockItemSelected(cityObj.mCityId)) {
            checkBox.setChecked(true);
        } else {
            checkBox.setChecked(false);
        }

        clock.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return processClick(v, true, cityObj.mCityId);
            }
        });

        clock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processClick(v, false, cityObj.mCityId);
            }
        });

    }

    public interface HostInterface {
        public void onWorldClockItemClicked(boolean isLongClick, String id);
        public boolean isWorldClockItemSelected(String id);
        public boolean isClockSelectionMode();
    }

    private HostInterface mHostInterface;

    private boolean processClick(final View v, final boolean isLongClick, final String id) {

        if (mHostInterface != null) {
            mHostInterface.onWorldClockItemClicked(isLongClick, id);
            notifyDataSetChanged();
            // TODO: 17-5-3
            return true;
        }
        return false;
    }
}
