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

package com.android.settings.search;

import android.content.Context;
import android.content.res.Resources;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

/**
 * The type External search indexable resource.
 *
 * @date Liuqin on 2017-05-02
 */
public class ExternalSearchIndexableResource implements Indexable {

    private static final String TAG = "ExternalSearchIndexableResource";

    private static final String ACTION_WALL_PAPER
            = "#Intent;action=hummingbird.intent.action.THEME;S.theme_component=wallpaper;end";
    private static final String ACTION_THEME
            = "#Intent;action=hummingbird.intent.action.THEME;S.theme_component=theme;end";
    private static final String ACTION_SIM
            = "com.hb.settings.SimSettings";
    private static final String ACTION_DATA_USAGE
            = "com.hb.netmanage.main.action";
    private static final String ACTION_BATTERY_MANAGER
            = "hmb.powermanager.powersave";

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER
            = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                                                          boolean enabled) {
            List<SearchIndexableRaw> indexables = new ArrayList<SearchIndexableRaw>();

            // wallpaper
            SearchIndexableRaw indexable = new SearchIndexableRaw(context);
            indexable.title = context.getString(R.string.wallpaper_settings_title);
            indexable.intentAction = ACTION_WALL_PAPER;
            indexable.iconResId = R.drawable.ic_settings_wallpaper;
            indexables.add(indexable);


            indexable = new SearchIndexableRaw(context);
            indexable.title = context.getString(R.string.theme_settings_title);
            indexable.intentAction = ACTION_THEME;
            indexable.iconResId = R.drawable.ic_settings_theme_alpha;
            indexables.add(indexable);

            // Sim
            indexable = new SearchIndexableRaw(context);
            indexable.title = context.getString(R.string.sim_settings_title);
            indexable.intentAction = ACTION_SIM;
            indexable.iconResId = R.drawable.ic_settings_sim;
            indexables.add(indexable);

            // Data usage
            indexable = new SearchIndexableRaw(context);
            indexable.title = context.getString(R.string.data_usage_summary_title);
            indexable.intentAction = ACTION_DATA_USAGE;
            indexable.iconResId = R.drawable.ic_settings_data_usage;
            indexables.add(indexable);

            // Battery manager
            indexable = new SearchIndexableRaw(context);
            indexable.title = context.getString(R.string.power_usage_summary_title);
            indexable.intentAction = ACTION_BATTERY_MANAGER;
            indexable.iconResId = R.drawable.ic_settings_battery;
            indexables.add(indexable);

            indexAllTempExternalData(context, indexables);

            return indexables;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            final ArrayList<String> result = new ArrayList<String>();
            return result;
        }

        private void indexAllTempExternalData(Context context, List<SearchIndexableRaw> indexables) {
            Resources resources = context.getResources();

            // sim
            String[] indexArray = resources.getStringArray(R.array.search_external_sim);
            indexTempExternalData(context, indexables, ACTION_SIM, R.drawable.ic_settings_sim, indexArray);

            // battery
            indexArray = resources.getStringArray(R.array.search_external_battery);
            indexTempExternalData(context, indexables, ACTION_BATTERY_MANAGER, R.drawable.ic_settings_battery, indexArray);

            // net control
            indexArray = resources.getStringArray(R.array.search_external_net_control);
            indexTempExternalData(context, indexables, ACTION_DATA_USAGE, R.drawable.ic_settings_data_usage, indexArray);
        }

        private void indexTempExternalData(Context context, List<SearchIndexableRaw> indexables,
                                           String intentAction, int resId, String[] indexArray) {
            if (indexArray == null || indexArray.length <= 0) {
                return;
            }
            String[] item;
            String title,summary;
            for (int i = 0; i < indexArray.length; i++) {
                item = indexArray[i].split("@");
                if (item != null && item.length > 0) {
                    title = item[0];
                    summary = item.length > 1 ? item[1] : null;
                    indexables.add(createSearchIndex(context, title, summary, intentAction, resId));
                }
            }
        }

        private SearchIndexableRaw createSearchIndex(Context context, String title, String summary,
                                                  String intentAction, int resId) {
            SearchIndexableRaw indexable = new SearchIndexableRaw(context);
            indexable.title = title;
            indexable.summaryOn = summary;
            indexable.intentAction = intentAction;
            indexable.iconResId = resId;
            return indexable;
        }
    };

}
