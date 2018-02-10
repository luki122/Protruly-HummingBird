/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.dashboard;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.HelpUtils;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsActivity;

import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.ISettingsMiscExt;

import java.util.List;

public class DashboardSummary extends InstrumentedFragment {
    private static final String LOG_TAG = "DashboardSummary";

    private LayoutInflater mLayoutInflater;
    private ViewGroup mDashboard;
    private IntentFilter mIntentFilter;
    private DashboardTileView mWifiTitleView;
    private DashboardTileView mBluetoothTitleView;

    private static final int MSG_REBUILD_UI = 1;
    private ISettingsMiscExt mExt;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REBUILD_UI: {
                    final Context context = getActivity();
                    rebuildUI(context);
                } break;
            }
        }
    };

    private class HomePackageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            rebuildUI(context);
        }
    }
    private HomePackageReceiver mHomePackageReceiver = new HomePackageReceiver();

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DASHBOARD_SUMMARY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        HelpUtils.prepareHelpMenuItem(getActivity(), menu, R.string.help_uri_dashboard,
                getClass().getName());
    }

    @Override
    public void onResume() {
        super.onResume();

        sendRebuildUI();

        final IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        getActivity().registerReceiver(mHomePackageReceiver, filter);

        listenBluetoothWifi();
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mHomePackageReceiver);
        unlistenBluetoothWifi();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mExt = UtilsExt.getMiscPlugin(this.getActivity());
        mLayoutInflater = inflater;

        final View rootView = inflater.inflate(R.layout.dashboard, container, false);
        mDashboard = (ViewGroup) rootView.findViewById(R.id.dashboard_container);

        return rootView;
    }

    private void rebuildUI(Context context) {
        if (!isAdded()) {
            Log.w(LOG_TAG, "Cannot build the DashboardSummary UI yet as the Fragment is not added");
            return;
        }

        long start = System.currentTimeMillis();
        final Resources res = getResources();

        mDashboard.removeAllViews();
        addSearchView();

        List<DashboardCategory> categories =
                ((SettingsActivity) context).getDashboardCategories(true);

        final int count = categories.size();

        for (int n = 0; n < count; n++) {
            DashboardCategory category = categories.get(n);

            View categoryView = mLayoutInflater.inflate(R.layout.dashboard_category, mDashboard,
                    false);

            TextView categoryLabel = (TextView) categoryView.findViewById(android.R.id.title);
            if (categoryLabel != null) {
                categoryLabel.setText(category.getTitle(res));
            }

            ViewGroup categoryContent =
                    (ViewGroup) categoryView.findViewById(R.id.category_content);

            final int tilesCount = category.getTilesCount();
            for (int i = 0; i < tilesCount; i++) {
                DashboardTile tile = category.getTile(i);

                DashboardTileView tileView = new DashboardTileView(context);
                updateTileView(context, res, tile, tileView.getImageView(),
                        tileView.getTitleTextView(), tileView.getStatusTextView());

                tileView.setTile(tile);
                updateWidgetStatus(tileView, tile);

                categoryContent.addView(tileView);
            }

            // Add the category
            mDashboard.addView(categoryView);
        }
        mLayoutInflater.inflate(R.layout.divider, mDashboard, true);
        long delta = System.currentTimeMillis() - start;
        Log.d(LOG_TAG, "rebuildUI took: " + delta + " ms");
    }

    private void updateTileView(Context context, Resources res, DashboardTile tile,
            ImageView tileIcon, TextView tileTextView, TextView statusTextView) {

        if (!TextUtils.isEmpty(tile.iconPkg)) {
            try {
                Drawable drawable = context.getPackageManager()
                        .getResourcesForApplication(tile.iconPkg).getDrawable(tile.iconRes, null);
                if (!tile.iconPkg.equals(context.getPackageName()) && drawable != null) {
                    // If this drawable is coming from outside Settings, tint it to match the color.
                    TypedValue tintColor = new TypedValue();
                    context.getTheme().resolveAttribute(com.android.internal.R.attr.colorAccent,
                            tintColor, true);
                    drawable.setTint(tintColor.data);
                }
                tileIcon.setImageDrawable(drawable);
            } catch (NameNotFoundException | Resources.NotFoundException e) {
                tileIcon.setImageDrawable(null);
                tileIcon.setBackground(null);
            }
        } else if (tile.iconRes > 0) {
            tileIcon.setImageResource(tile.iconRes);
        } else {
            tileIcon.setImageDrawable(null);
            tileIcon.setBackground(null);
            mExt.customizeDashboardTile(tile, tileIcon);
        }

        ///M: feature replace sim to uim
        tileTextView.setText(mExt.customizeSimDisplayString(
            tile.getTitle(res).toString(), SubscriptionManager.INVALID_SUBSCRIPTION_ID));

        CharSequence summary = tile.getSummary(res);
        if (!TextUtils.isEmpty(summary)) {
            statusTextView.setVisibility(View.VISIBLE);
            statusTextView.setText(summary);
        } else {
            statusTextView.setVisibility(View.GONE);
        }
    }

    private void sendRebuildUI() {
        if (!mHandler.hasMessages(MSG_REBUILD_UI)) {
            mHandler.sendEmptyMessage(MSG_REBUILD_UI);
        }
    }

    private void updateWidgetStatus(DashboardTileView dashboardTileView, DashboardTile tile) {
        if (dashboardTileView == null || tile == null) {
            return;
        }
        int resId = 0;
        if (tile.id == R.id.wifi_settings) {
            WifiManager wifimanager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
            resId = wifimanager.isWifiEnabled() ? R.string.toggle_enabled : R.string.toggle_disabled;
            mWifiTitleView = dashboardTileView;
        } else if (tile.id == R.id.bluetooth_settings) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            resId = (adapter != null && adapter.isEnabled()) ? R.string.toggle_enabled : R.string.toggle_disabled;
            mBluetoothTitleView = dashboardTileView;
        }
        if (resId > 0) {
            dashboardTileView.setWidgetStatus(getContext().getString(resId));
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state =
                        intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                    case BluetoothAdapter.STATE_OFF:
                        DashboardTileView titleView = mBluetoothTitleView;
                        if (titleView != null) {
                            updateWidgetStatus(titleView, titleView.getTile());
                        }
                        break;

                }
            } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                switch (state) {
                    case WifiManager.WIFI_STATE_ENABLED:
                    case WifiManager.WIFI_STATE_DISABLED:
                        DashboardTileView titleView = mWifiTitleView;
                        if (titleView != null) {
                            updateWidgetStatus(titleView, titleView.getTile());
                        }
                        break;
                }
            }
        }
    };

    private void listenBluetoothWifi() {
        getContext().registerReceiver(mReceiver, mIntentFilter);
    }

    private void unlistenBluetoothWifi() {
        getContext().unregisterReceiver(mReceiver);
    }

    /**
     * Add search view
     *
     * @date Liuqin on 2017-05-08
     */
    private void addSearchView() {
        View searchItem = mLayoutInflater.inflate(R.layout.search_stub_hb, mDashboard, true);
        View editView = searchItem.findViewById(R.id.input);
        editView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MenuItem searchMenuItem = ((SettingsActivity)getActivity()).getmSearchMenuItem();
                searchMenuItem.expandActionView();
            }
        });
    }
}
