/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.provision;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.StatusBarManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.app.LocalePicker;
import com.android.provision.R;

import java.util.Locale;

/**
 * Application that sets the provisioned bit, like SetupWizard does.
 */
public class LanguageSettingsWizard extends BaseActivity {

    private static final String TAG = "LanguageSettingsWizard";
    private static final String HIDE_NAVAGATIONBAR_ACTION = "com_hb_hide_navagationbar_action";
    private static final String HB_HIDE_NAVIGATION_FOR_APP = "hb_hide_bar_for_app";

    private ListView listView;
    private TextView tv_next;

    private String[] languageArray;

    private Locale mCurrentLocale = null;
    private ArrayAdapter<LocalePicker.LocaleInfo> mAdapter = null;

    private static boolean hasInitLang = false;		// 初始化是否完成
    private int mSelected = -1;
    private LanguageAdapter languageAdapter;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // reset setting.
        Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 0);
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 0);

        setContentView(R.layout.activity_language_setting);

        listView = (ListView) findViewById(R.id.listView);
        tv_next = (TextView) findViewById(R.id.next);

        mCurrentLocale = getCurrentLanguage();

        mAdapter = LocalePicker.constructAdapter(this);
        selectLocale(mAdapter, 0);
        languageArray = new String[mAdapter.getCount()];
        for (int i = 0; i < languageArray.length; i++) {
            LocalePicker.LocaleInfo localeInfo = mAdapter.getItem(i);
            languageArray[i] = localeInfo.getLabel();

            if (languageArray[i].contains("English")) {
                languageArray[i] = "English";
            }

            if (hasInitLang) {
                if (mCurrentLocale != null && localeInfo != null
                        && mCurrentLocale.equals(localeInfo.getLocale())) {
                    mSelected = i;
                }
            } else {
                if (localeInfo != null && Locale.SIMPLIFIED_CHINESE.equals(localeInfo.getLocale())) {
                    LocalePicker.updateLocale(localeInfo.getLocale());
                    mSelected = i;
                    hasInitLang = true;
                }
            }
        }

        languageAdapter = new LanguageAdapter();
        listView.setAdapter(languageAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSelected = position;
                languageAdapter.notifyDataSetChanged();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        hasInitLang = true;
                        updateLocale();
                    }
                }, 400);
            }
        });

        tv_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelected == -1) {
                    return;
                }

                Utils.goNext(LanguageSettingsWizard.this, TransferActivity.LANGUAGE_SETTING);
            }
        });

        // 设置状态栏不可滑动以及home recent不显示
        StatusBarManager mStatusBarManager = (StatusBarManager) getSystemService(STATUS_BAR_SERVICE);
        mStatusBarManager.disable(StatusBarManager.DISABLE_EXPAND | StatusBarManager.DISABLE_HOME | StatusBarManager.DISABLE_RECENT);

//        // 隐藏虚拟按键
//        Settings.Secure.putInt(getContentResolver(), HB_HIDE_NAVIGATION_FOR_APP, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

//        getWindow().getDecorView().setSystemUiVisibility(
//                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

    }

    private Locale getCurrentLanguage() {
        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            Configuration config = am.getConfiguration();
            return config.locale;
        } catch (android.os.RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 更新语言设置
     */
    private void updateLocale() {
        LocalePicker.LocaleInfo localeInfo = mAdapter.getItem(mSelected);
        if (localeInfo != null && localeInfo.getLocale() != null
                && !localeInfo.getLocale().equals(mCurrentLocale)) {
            LocalePicker.updateLocale(localeInfo.getLocale());
        }
    }

    private void selectLocale(ArrayAdapter<LocalePicker.LocaleInfo> adapter, int begin) {
        if (begin == adapter.getCount()) {
            return;
        }
        LocalePicker.LocaleInfo tempLocaleInfo = adapter.getItem(begin);
        if (tempLocaleInfo != null
                && (Locale.SIMPLIFIED_CHINESE.equals(tempLocaleInfo.getLocale())
                || Locale.TAIWAN.equals(tempLocaleInfo.getLocale())
                || Locale.US.equals(tempLocaleInfo.getLocale()))) {
            selectLocale(adapter, begin + 1);
        } else {
            adapter.remove(tempLocaleInfo);
            selectLocale(adapter, begin);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private class LanguageAdapter extends BaseAdapter {

        LayoutInflater inflater;

        public LanguageAdapter() {
            inflater = LayoutInflater.from(LanguageSettingsWizard.this);
        }

        @Override
        public int getCount() {
            return languageArray == null ? 0 : languageArray.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder = null;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_language, null);
                holder = new Holder();
                holder.tv_text = (TextView) convertView.findViewById(R.id.tv_text);
                holder.iv_tick = (ImageView) convertView.findViewById(R.id.iv_tick);
                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }

            holder.tv_text.setText(languageArray[position]);
            if (mSelected == position) {
                holder.iv_tick.setVisibility(View.VISIBLE);
            } else {
                holder.iv_tick.setVisibility(View.INVISIBLE);
            }

            return convertView;
        }

        class Holder {
            TextView tv_text;
            ImageView iv_tick;
        }

    }


}

