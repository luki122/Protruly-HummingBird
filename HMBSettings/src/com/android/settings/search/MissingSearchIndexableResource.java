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

import android.accounts.Account;
import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.SecuritySettings;
import com.android.settings.accounts.AccountSettings;
import com.android.settings.applications.ManageApplications;
import com.android.settings.applications.ManageDefaultApps;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.sim.SimSettings;
import com.android.settings.wifi.WifiSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * The type External search indexable resource.
 *
 * @date Liuqin on 2017-05-02
 */
public class MissingSearchIndexableResource implements Indexable {

    private static final String TAG = "MissingSearchIndexableResource";



    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER
            = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                                                          boolean enabled) {
            List<SearchIndexableRaw> indexables = new ArrayList<SearchIndexableRaw>();

            /************ WLAN ************/
            // WLAN ==> 添加网络
            int rank = Ranking.getRankForClassName(WifiSettings.class.getName());
            int iconResId = R.drawable.ic_settings_wireless;

            SearchIndexableRaw indexable = new SearchIndexableRaw(context);
            indexable.title = context.getString(R.string.wifi_add_network_title);
            indexable.screenTitle = context.getString(R.string.wifi_settings_title);
            indexable.className = WifiSettings.class.getName();
            indexable.iconResId = iconResId;
            indexable.rank = rank;
            indexables.add(indexable);

            // WLAN ==> 开启WLAN
            indexable = new SearchIndexableRaw(context);
            indexable.title = context.getString(R.string.wlan_turn_on);
            indexable.screenTitle = context.getString(R.string.wifi_settings_title);
            indexable.className = WifiSettings.class.getName();
            indexable.iconResId = iconResId;
            indexable.rank = rank;
            indexables.add(indexable);

            /************ Bluetooth ************/
            // 蓝牙 ==> 开启蓝牙
            rank = Ranking.getRankForClassName(BluetoothSettings.class.getName());
            iconResId = R.drawable.ic_settings_bluetooth;
            indexable = new SearchIndexableRaw(context);
            indexable.title = context.getString(R.string.bluetooth_turn_on);
            indexable.screenTitle = context.getString(R.string.bluetooth_settings_title);
            indexable.className = BluetoothSettings.class.getName();
            indexable.iconResId = iconResId;
            indexable.rank = rank;
            indexables.add(indexable);

            // 蓝牙 ==> 手机名称
            indexable = new SearchIndexableRaw(context);
            indexable.title = context.getString(R.string.bluetooth_phone_name);
            indexable.screenTitle = context.getString(R.string.bluetooth_settings_title);
            indexable.className = BluetoothSettings.class.getName();
            indexable.iconResId = iconResId;
            indexable.rank = rank;
            indexables.add(indexable);

            /************ 帐户 ************/
            // 帐户
            rank = Ranking.getRankForClassName(AccountSettings.class.getName());
            iconResId = R.drawable.ic_settings_accounts;
            indexable = new SearchIndexableRaw(context);
            indexable.title = context.getString(R.string.account_settings_title);
            indexable.screenTitle = context.getString(R.string.account_settings_title);
            indexable.className = AccountSettings.class.getName();
            indexable.iconResId = iconResId;
            indexable.rank = rank;
            indexables.add(indexable);

            indexable = new SearchIndexableRaw(context);
            indexable.title = context.getString(R.string.add_account_label);
            indexable.screenTitle = context.getString(R.string.account_settings_title);
            indexable.className = AccountSettings.class.getName();
            indexable.iconResId = iconResId;
            indexable.rank = rank;
            indexables.add(indexable);

            // 应用
            rank = Ranking.getRankForClassName(ManageApplications.class.getName());
            iconResId = R.drawable.ic_settings_applications;
            indexable = new SearchIndexableRaw(context);
            indexable.title = context.getString(R.string.applications_settings);
            indexable.screenTitle = context.getString(R.string.applications_settings);
            indexable.className = ManageApplications.class.getName();
            indexable.iconResId = iconResId;
            indexable.rank = rank;
            indexables.add(indexable);

            // 系统安全 ==> 自启动管理
            rank = Ranking.getRankForClassName(SecuritySettings.class.getName());
            iconResId = R.drawable.ic_settings_security;
            indexable = new SearchIndexableRaw(context);
            indexable.title = context.getString(R.string.security_autostart_title);
            indexable.screenTitle = context.getString(R.string.security_settings_title);
            indexable.className = SecuritySettings.class.getName();
            indexable.iconResId = iconResId;
            indexable.rank = rank;
            indexables.add(indexable);

            // 系统安全 ==> 应用权限
//            indexable = new SearchIndexableRaw(context);
//            indexable.title = context.getString(R.string.security_permission_title);
//            indexable.screenTitle = context.getString(R.string.security_settings_title);
//            indexable.className = SecuritySettings.class.getName();
//            indexable.iconResId = iconResId;
//            indexable.rank = rank;
//            indexables.add(indexable);

            // 系统安全 ==> 数据保护
//            indexable = new SearchIndexableRaw(context);
//            indexable.title = context.getString(R.string.security_data_protection_title);
//            indexable.screenTitle = context.getString(R.string.security_settings_title);
//            indexable.className = SecuritySettings.class.getName();
//            indexable.iconResId = iconResId;
//            indexable.rank = rank;
//            indexables.add(indexable);

            // 系统安全 ==> 手机防盗
//            indexable = new SearchIndexableRaw(context);
//            indexable.title = context.getString(R.string.security_prevent_lost);
//            indexable.screenTitle = context.getString(R.string.security_settings_title);
//            indexable.className = SecuritySettings.class.getName();
//            indexable.iconResId = iconResId;
//            indexable.rank = rank;
//            indexables.add(indexable);

            return indexables;
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            return super.getXmlResourcesToIndex(context, enabled);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            final ArrayList<String> result = new ArrayList<String>();
            return result;
        }
    };

}
