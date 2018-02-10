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

package com.android.deskclock.worldclock;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.HbSearchView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.android.deskclock.R;
import com.android.deskclock.SettingsActivity;
import com.android.deskclock.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import hb.app.HbActivity;
import hb.widget.HbIndexBar;
import hb.widget.toolbar.Toolbar;

import android.widget.HbPreSearchView;
/**
 * Cities chooser for the world clock
 */
public class CitiesActivity2 extends HbActivity implements AdapterView.OnItemClickListener,
        HbSearchView.OnQueryTextListener, HbIndexBar.OnSelectListener,
        HbIndexBar.OnTouchStateChangedListener,
        AbsListView.OnScrollListener {

    private static final String KEY_SEARCH_QUERY = "search_query";
    private static final String KEY_SEARCH_MODE = "search_mode";
    private static final String KEY_LIST_POSITION = "list_position";

    private static final String PREF_SORT = "sort_preference";

    private static final int SORT_BY_NAME = 0;
    private static final int SORT_BY_GMT_OFFSET = 1;

    /**
     * This must be false for production. If true, turns on logging, test code,
     * etc.
     */
    static final boolean DEBUG = false;
    static final String TAG = "CitiesActivity";

    private LayoutInflater mFactory;
    private ListView mCitiesList;
    private TextView mNoResultLayout;
    private CityAdapter mAdapter;
    private HashMap<String, CityObj> mUserSelectedCities;
    private Calendar mCalendar;

    private HbSearchView mActualSearchView;
    private HbPreSearchView mFakeSearchView;
    private Toolbar mToolbar;
    private HbIndexBar mIndexBar;
    private StringBuffer mQueryTextBuffer = new StringBuffer();
    private boolean mSearchMode;
    private int mPosition = -1;

    private SharedPreferences mPrefs;
    private int mSortType;

//    private String mSelectedCitiesHeaderString;


    /***
     * Adapter for a list of cities with the respected time zone. The Adapter
     * sorts the list alphabetically and create an indexer.
     ***/
    private class CityAdapter extends BaseAdapter implements Filterable, SectionIndexer {
        private static final int VIEW_TYPE_CITY = 0;
        private static final int VIEW_TYPE_HEADER = 1;

        private static final String DELETED_ENTRY = "C0";

        private List<CityObj> mDisplayedCitiesList;

//        private CityObj[] mCities;
        private ArrayList<CityObj> mCities = new ArrayList<>();
        private CityObj[] mSelectedCities;

        private final int mLayoutDirection;

        // A map that caches names of cities in local memory.  The names in this map are
        // preferred over the names of the selected cities stored in SharedPreferences, which could
        // be in a different language.  This map gets reloaded on a locale change, when the new
        // language's city strings are read from the xml file.
        private HashMap<String, String> mCityNameMap = new HashMap<String, String>();

        private String[] mSectionHeaders;
        private Integer[] mSectionPositions;

        private CityNameComparator mSortByNameComparator = new CityNameComparator();
        private CityGmtOffsetComparator mSortByTimeComparator = new CityGmtOffsetComparator();

        private final LayoutInflater mInflater;
        private boolean mIs24HoursMode; // AM/PM or 24 hours mode

        private final String mPattern12;
        private final String mPattern24;

        private int mSelectedEndPosition = 0;

        private Map<String, Integer> mIndexMap = new HashMap<>();

        private Filter mFilter = new Filter() {

            @Override
            protected synchronized FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                String modifiedQuery = constraint.toString().trim().toUpperCase();

                ArrayList<CityObj> filteredList = new ArrayList<>();
                ArrayList<String> sectionHeaders = new ArrayList<>();
                ArrayList<Integer> sectionPositions = new ArrayList<>();

                // Update the list first when user using search filter
                final Collection<CityObj> selectedCities = mUserSelectedCities.values();
                mSelectedCities = selectedCities.toArray(new CityObj[selectedCities.size()]);
                // If the search query is empty, add in the selected cities
//                if (TextUtils.isEmpty(modifiedQuery) && mSelectedCities != null) {
//                    if (mSelectedCities.length > 0) {
//                        sectionHeaders.add("+");
//                        sectionPositions.add(0);
//                        filteredList.add(new CityObj(mSelectedCitiesHeaderString,
//                                mSelectedCitiesHeaderString, null, null));
//                    }
//                    for (CityObj city : mSelectedCities) {
//                        city.isHeader = false;
//                        filteredList.add(city);
//                    }
//                }

                final HashSet<String> selectedCityIds = new HashSet<>();
                for (CityObj c : mSelectedCities) {
                    selectedCityIds.add(c.mCityId);
                }
                mSelectedEndPosition = filteredList.size();

                long currentTime = System.currentTimeMillis();
                String val = null;
                int offset = -100000; //some value that cannot be a real offset
                for (int i = 0; i < mCities.size(); ++i) {
                    CityObj city = mCities.get(i);
                    // If the city is a deleted entry, ignore it.
                    if (city.mCityId.equals(DELETED_ENTRY)) {
                        continue;
                    }

                    // If the search query is empty, add section headers.
                    if (TextUtils.isEmpty(modifiedQuery)) {
                        if (!selectedCityIds.contains(city.mCityId)) {
                            // If the list is sorted by name, and the city has an index
                            // different than the previous city's index, update the section header.
                            if (mSortType == SORT_BY_NAME
                                    && !city.mCityIndex.equals(val)) {
                                val = city.mCityIndex.toUpperCase();
                                sectionHeaders.add(val);
                                sectionPositions.add(filteredList.size());
                                mIndexMap.put(val, filteredList.size());
                                city.isHeader = true;
                            } else {
                                city.isHeader = false;
                            }

                            // If the list is sorted by time, and the gmt offset is different than
                            // the previous city's gmt offset, insert a section header.
//                            if (mSortType == SORT_BY_GMT_OFFSET) {
//                                TimeZone timezone = TimeZone.getTimeZone(city.mTimeZone);
//                                int newOffset = timezone.getOffset(currentTime);
//                                if (offset != newOffset) {
//                                    offset = newOffset;
//                                    // Because JB fastscroll only supports ~1 char strings
//                                    // and KK ellipsizes strings, trim section headers to the
//                                    // nearest hour.
//                                    final String offsetString = Utils.getGMTHourOffset(timezone,
//                                            Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT
//                                            /* useShortForm */ );
//                                    sectionHeaders.add(offsetString);
//                                    sectionPositions.add(filteredList.size());
//                                    city.isHeader = true;
//                                } else {
//                                    city.isHeader = false;
//                                }
//                            }

                            filteredList.add(city);
                        }
                    } else {
                        // If the city name begins with the non-empty query, add it into the list.
                        String cityName = city.mCityName.trim().toUpperCase();
                        String tmp = city.mCityAbbr;
                        String abbr = "";
                        if (tmp != null && !tmp.isEmpty()) {
                            abbr = tmp;
                        }
                        if (city.mCityId != null
                                && (cityName.startsWith(modifiedQuery) || (!abbr.isEmpty() && abbr.startsWith(modifiedQuery)))) {
                            city.isHeader = false;
                            filteredList.add(city);
                        }
                    }
                }

                mSectionHeaders = sectionHeaders.toArray(new String[sectionHeaders.size()]);
                mSectionPositions = sectionPositions.toArray(new Integer[sectionPositions.size()]);

                results.values = filteredList;
                results.count = filteredList.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mDisplayedCitiesList = (ArrayList<CityObj>) results.values;
                initIndexBar();
                if (mDisplayedCitiesList.size() == 0) {
                    mNoResultLayout.setVisibility(View.VISIBLE);
                    mIndexBar.setVisibility(View.INVISIBLE);
                } else {
                    mNoResultLayout.setVisibility(View.GONE);
                    mIndexBar.setVisibility(View.VISIBLE);
                }
                if (mPosition >= 0) {
                    mCitiesList.setSelectionFromTop(mPosition, 0);
                    mPosition = -1;
                }
                notifyDataSetChanged();
            }
        };

        public CityAdapter(
                Context context, LayoutInflater factory) {
            super();
            mCalendar = Calendar.getInstance();
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            mLayoutDirection = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault());
            mInflater = factory;

            // Load the cities from xml.
            CityObj[] cities = Utils.loadCitiesFromXml(context);
            for (CityObj city : cities) {
                if (!mUserSelectedCities.containsKey(city.mCityId)) {
                    mCities.add(city);
                }
            }
//            mCities = Utils.loadCitiesFromXml(context);

            // Reload the city name map with the recently parsed city names of the currently
            // selected language for use with selected cities.
            mCityNameMap.clear();
            for (int i = 0; i < mCities.size(); ++i) {
                mCityNameMap.put(mCities.get(i).mCityId, mCities.get(i).mCityName);
            }

            // Re-organize the selected cities into an array.
            Collection<CityObj> selectedCities = mUserSelectedCities.values();
            mSelectedCities = selectedCities.toArray(new CityObj[selectedCities.size()]);

            // Override the selected city names in the shared preferences with the
            // city names in the updated city name map, which will always reflect the
            // current language.
            for (CityObj city : mSelectedCities) {
                String newCityName = mCityNameMap.get(city.mCityId);
                if (newCityName != null) {
                    city.mCityName = newCityName;
                }
            }

            mPattern24 = Utils.isJBMR2OrLater()
                    ? DateFormat.getBestDateTimePattern(Locale.getDefault(), "Hm")
                    : getString(R.string.time_format_24_mode);

            // There's an RTL layout bug that causes jank when fast-scrolling through
            // the list in 12-hour mode in an RTL locale. We can work around this by
            // ensuring the strings are the same length by using "hh" instead of "h".
            String pattern12 = Utils.isJBMR2OrLater()
                    ? DateFormat.getBestDateTimePattern(Locale.getDefault(), "hma")
                    : getString(R.string.time_format_12_mode);

            if (mLayoutDirection == View.LAYOUT_DIRECTION_RTL) {
                pattern12 = pattern12.replaceAll("h", "hh");
            }
            mPattern12 = pattern12;

            sortCities(mSortType);
            set24HoursMode(context);
        }

        public void toggleSort() {
            if (mSortType == SORT_BY_NAME) {
                sortCities(SORT_BY_GMT_OFFSET);
            } else {
                sortCities(SORT_BY_NAME);
            }
        }

        private void sortCities(final int sortType) {
            mSortType = sortType;
            CityObj[] cities = new CityObj[mCities.size()];
            for (int i = 0; i < mCities.size(); ++i) {
                cities[i] = mCities.get(i);
            }
            Arrays.sort(cities, sortType == SORT_BY_NAME ? mSortByNameComparator
                    : mSortByTimeComparator);
            mCities.clear();
            for (CityObj cityObj : cities) {
                mCities.add(cityObj);
            }
            if (mSelectedCities != null) {
                Arrays.sort(mSelectedCities, sortType == SORT_BY_NAME ? mSortByNameComparator
                        : mSortByTimeComparator);
            }
            mPrefs.edit().putInt(PREF_SORT, sortType).commit();
            mFilter.filter(mQueryTextBuffer.toString());
        }

        @Override
        public int getCount() {
            return mDisplayedCitiesList != null ? mDisplayedCitiesList.size() : 0;
        }

        @Override
        public Object getItem(int p) {
            if (mDisplayedCitiesList != null && p >= 0 && p < mDisplayedCitiesList.size()) {
                return mDisplayedCitiesList.get(p);
            }
            return null;
        }

        @Override
        public long getItemId(int p) {
            return p;
        }

        @Override
        public boolean isEnabled(int p) {
            return mDisplayedCitiesList != null && mDisplayedCitiesList.get(p).mCityId != null;
        }

        @Override
        public synchronized View getView(int position, View view, ViewGroup parent) {
            if (mDisplayedCitiesList == null || position < 0
                    || position >= mDisplayedCitiesList.size()) {
                return null;
            }
            CityObj c = mDisplayedCitiesList.get(position);
            // Header view: A CityObj with nothing but the "selected cities" label
            if (c.mCityId == null) {
                if (view == null) {
                    view = mInflater.inflate(R.layout.city_list_header, parent, false);
                }
            } else { // City view
                // Make sure to recycle a City view only
                if (view == null) {
                    view = mInflater.inflate(R.layout.city_list_item_2, parent, false);
                    final CityViewHolder holder = new CityViewHolder();
                    holder.index = (TextView) view.findViewById(R.id.index);
                    holder.name = (TextView) view.findViewById(R.id.city_name);
                    holder.line = (View) view.findViewById(R.id.divider);
                    view.setTag(holder);
                }
                CityViewHolder holder = (CityViewHolder) view.getTag();
                holder.name.setTag(c);
//                holder.name.setSelected(mUserSelectedCities.containsKey(c.mCityId));
                holder.name.setText(c.mCityName, TextView.BufferType.SPANNABLE);
                if (c.isHeader) {
                    holder.index.setVisibility(View.VISIBLE);
                    holder.index.setText(c.mCityIndex);
                    holder.line.setVisibility(View.GONE);
                } else {
                    // If not a header, use the invisible index for left padding
                    holder.index.setVisibility(View.GONE);
                    holder.line.setVisibility(View.VISIBLE);
                }
                // skip checkbox and other animations
                view.jumpDrawablesToCurrentState();
            }
            return view;
        }

        private CharSequence getTimeCharSequence(String timeZone) {
            mCalendar.setTimeZone(TimeZone.getTimeZone(timeZone));
            return DateFormat.format(mIs24HoursMode ? mPattern24 : mPattern12, mCalendar);
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return (mDisplayedCitiesList.get(position).mCityId != null)
                    ? VIEW_TYPE_CITY : VIEW_TYPE_HEADER;
        }

        private class CityViewHolder {
            TextView index;
            TextView name;
            View line;
        }

        public void set24HoursMode(Context c) {
            mIs24HoursMode = DateFormat.is24HourFormat(c);
            notifyDataSetChanged();
        }

        @Override
        public int getPositionForSection(int section) {
            return !isEmpty(mSectionPositions) ? mSectionPositions[section] : 0;
        }


        @Override
        public int getSectionForPosition(int p) {
            final Integer[] positions = mSectionPositions;
            if (!isEmpty(positions)) {
                for (int i = 0; i < positions.length - 1; i++) {
                    if (p >= positions[i]
                            && p < positions[i + 1]) {
                        return i;
                    }
                }
                if (p >= positions[positions.length - 1]) {
                    return positions.length - 1;
                }
            }
            return 0;
        }

        @Override
        public Object[] getSections() {
            return mSectionHeaders;
        }

        @Override
        public Filter getFilter() {
            return mFilter;
        }

        private boolean isEmpty(Object[] array) {
            return array == null || array.length == 0;
        }

        public Map<String, Integer> getIndexMap() {
            return mIndexMap;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_ALARM);

        mFactory = LayoutInflater.from(this);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSortType = mPrefs.getInt(PREF_SORT, SORT_BY_NAME);
//        mSelectedCitiesHeaderString = getString(R.string.selected_cities_label);
        if (savedInstanceState != null) {
            mQueryTextBuffer.append(savedInstanceState.getString(KEY_SEARCH_QUERY));
            mSearchMode = savedInstanceState.getBoolean(KEY_SEARCH_MODE);
            mPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
        }
        updateLayout();
//        initIndexBar();
        initToolbar();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString(KEY_SEARCH_QUERY, mQueryTextBuffer.toString());
        bundle.putBoolean(KEY_SEARCH_MODE, mSearchMode);
        bundle.putInt(KEY_LIST_POSITION, mCitiesList.getFirstVisiblePosition());
    }

    private void updateLayout() {
        setHbContentView(R.layout.cities_activity_2);
        mCitiesList = (ListView) findViewById(R.id.cities_list);
//        setFastScroll(TextUtils.isEmpty(mQueryTextBuffer.toString().trim()));
//        mCitiesList.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        mCitiesList.setVerticalScrollBarEnabled(false);
        mCitiesList.setDivider(null);
        mUserSelectedCities = Cities.readCitiesFromSharedPrefs(
                PreferenceManager.getDefaultSharedPreferences(this));
        mAdapter = new CityAdapter(this, mFactory);
        mCitiesList.setAdapter(mAdapter);
        mCitiesList.setOnItemClickListener(this);
        mCitiesList.setOnScrollListener(this);
        mNoResultLayout = (TextView) findViewById(R.id.no_result_layout);
    }

    private void setFastScroll(boolean enabled) {
        if (mCitiesList != null) {
            mCitiesList.setFastScrollAlwaysVisible(enabled);
            mCitiesList.setFastScrollEnabled(enabled);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.set24HoursMode(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Cities.saveCitiesToSharedPrefs(PreferenceManager.getDefaultSharedPreferences(this),
                mUserSelectedCities);
        Intent i = new Intent(Cities.WORLDCLOCK_UPDATE_INTENT);
        sendBroadcast(i);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        TextView name = (TextView) view.findViewById(R.id.city_name);
        CityObj c = (CityObj) name.getTag();
        if (!mUserSelectedCities.containsKey(c.mCityIndex)) {
            mUserSelectedCities.put(c.mCityId, c);
        }
        finish();
    }

    @Override
    public boolean onQueryTextChange(String queryText) {
        mQueryTextBuffer.setLength(0);
        mQueryTextBuffer.append(queryText);
//        mCitiesList.setFastScrollEnabled(TextUtils.isEmpty(mQueryTextBuffer.toString().trim()));
        mAdapter.getFilter().filter(queryText);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String arg0) {
        return false;
    }

    @Override
    public void onStateChanged(HbIndexBar.TouchState touchState, HbIndexBar.TouchState touchState1) {

    }

    @Override
    public void onSelect(int i, int layer, HbIndexBar.Letter letter) {
        if (indexMap != null && indexMap.containsKey(letter.text.toUpperCase())) {
            mCitiesList.setSelection(indexMap.get(letter.text.toUpperCase()));
        }
    }

    Map<String, Integer> indexMap;
    private void initIndexBar() {
        mIndexBar = (HbIndexBar) findViewById(R.id.index_bar);
        mIndexBar.setOnSelectListener(this);
        mIndexBar.clear(0);
        mIndexBar.clear(2);
        indexMap = mAdapter.getIndexMap();
        for (int i = 0; i < mIndexBar.size(); ++i) {
            if (indexMap.containsKey(mIndexBar.getString(i))) {
                mIndexBar.setEnables(true, i);
            }
//            mIndexBar.setEnables(true, i);
        }
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.cities_toolbar);
        ImageView backButton = (ImageView) mToolbar.findViewById(R.id.back_btn);
        final TextView title = (TextView) mToolbar.findViewById(R.id.cities_title);
        mActualSearchView = (HbSearchView) mToolbar.findViewById(R.id.actual_searchview);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(mActualSearchView.getWindowToken(), imm.HIDE_NOT_ALWAYS);
                }
                finish();
            }
        });
        mActualSearchView.setOnQueryTextListener(this);

        mFakeSearchView = (HbPreSearchView) findViewById(R.id.fake_searchview);
//        mFakeSearchView.setEnabled(true);
        mFakeSearchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFakeSearchView.setVisibility(View.GONE);
                title.setVisibility(View.GONE);
                mActualSearchView.setVisibility(View.VISIBLE);
                mActualSearchView.setIconified(false);
//                mActualSearchView.setFocusable(true);
//                mActualSearchView.requestFocus();
            }
        });

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        CityObj cityObj = (CityObj) mAdapter.getItem(firstVisibleItem);
        if(cityObj != null) {
            int index = -1;
            if (cityObj.mCityIndex != null) {
                index = mIndexBar.getIndex(cityObj.mCityIndex);
            }

            if(index == -1){
                index = mIndexBar.size() - 1;
            }

            mIndexBar.setFocus(index);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        //DO NOTHING
    }
}
