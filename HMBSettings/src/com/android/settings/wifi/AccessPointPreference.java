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
package com.android.settings.wifi;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.os.UserHandle;
import hb.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settingslib.wifi.AccessPoint;

import java.lang.reflect.Field;

public class AccessPointPreference extends Preference {

    private static final int[] STATE_SECURED = {
        R.attr.state_encrypted
    };
    private static final int[] STATE_NONE = {};

    private static int[] wifi_signal_attributes = { R.attr.wifi_signal };

    private final StateListDrawable mWifiSld;
    private final int mBadgePadding;
    private final UserBadgeCache mBadgeCache;

    private TextView mTitleView;
    private boolean mForSavedNetworks = false;
    private AccessPoint mAccessPoint;
    private Drawable mBadge;
    private int mLevel;
    private CharSequence mContentDescription;

    static final int[] WIFI_CONNECTION_STRENGTH = {
        R.string.accessibility_wifi_one_bar,
        R.string.accessibility_wifi_two_bars,
        R.string.accessibility_wifi_three_bars,
        R.string.accessibility_wifi_signal_full
    };

    private TextView mTvSignalText;
    private ImageView mIvSignalWifi;
    private ImageView mIvSignalDetailArrow;
    private Drawable mIcon;
    private boolean mShowWidget = true;

    private Field mRssiField;

    // Used for dummy pref.
    public AccessPointPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mWifiSld = null;
        mBadgePadding = 0;
        mBadgeCache = null;
    }

    public AccessPointPreference(AccessPoint accessPoint, Context context, UserBadgeCache cache,
                                 boolean forSavedNetworks) {
        super(context);
        mBadgeCache = cache;
        mAccessPoint = accessPoint;
        mForSavedNetworks = forSavedNetworks;
        mAccessPoint.setTag(this);
        mLevel = -1;

        mWifiSld = (StateListDrawable) context.getTheme()
                .obtainStyledAttributes(wifi_signal_attributes).getDrawable(0);

        // Distance from the end of the title at which this AP's user badge should sit.
        mBadgePadding = context.getResources()
                .getDimensionPixelSize(R.dimen.wifi_preference_badge_padding);
        refresh();
    }


    public AccessPointPreference(AccessPoint accessPoint, Context context, UserBadgeCache cache,
                                 boolean forSavedNetworks, boolean showWidget) {
        this(accessPoint, context, cache, forSavedNetworks);
        mShowWidget = showWidget;
    }

    public AccessPoint getAccessPoint() {
        return mAccessPoint;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        initWidget(view);
        if (mAccessPoint == null) {
            // Used for dummy pref.
            return;
        }
        Drawable drawable = getIcon();
        if (drawable != null) {
            if (mLevel > 0) {
                drawable.setLevel(mLevel);
            } else {
                if (drawable.getState() == STATE_SECURED) {
                    mIvSignalWifi.setImageResource(R.drawable.ic_wifi_lock_signal_right_0);
                } else {
                    mIvSignalWifi.setImageResource(R.drawable.ic_wifi_signal_right_0);
                }
            }
        }

        mTitleView = (TextView) view.findViewById(com.android.internal.R.id.title);
        if (mTitleView != null) {
            // Attach to the end of the title view
            mTitleView.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, mBadge, null);
            mTitleView.setCompoundDrawablePadding(mBadgePadding);
        }
        view.setContentDescription(mContentDescription);
    }

    protected void updateIcon(int level, Context context) {
        if (level == -1) {
            setIcon(null);
        } else {
            if (getIcon() == null) {
                // To avoid a drawing race condition, we first set the state (SECURE/NONE) and then
                // set the icon (drawable) to that state's drawable.
                // If sld is null then we are indexing and therefore do not have access to
                // (nor need to display) the drawable.
                if (mWifiSld != null) {
                    mWifiSld.setState((mAccessPoint.getSecurity() != AccessPoint.SECURITY_NONE)
                            ? STATE_SECURED
                            : STATE_NONE);
                    Drawable drawable = mWifiSld.getCurrent();
                    if (!mForSavedNetworks) {
                        setIcon(drawable);
                    } else {
                        setIcon(null);
                    }
                }
            }
        }
    }

    protected void updateBadge(Context context) {
        WifiConfiguration config = mAccessPoint.getConfig();
        if (config != null) {
            // Fetch badge (may be null)
            // Get the badge using a cache since the PM will ask the UserManager for the list
            // of profiles every time otherwise.
            mBadge = mBadgeCache.getUserBadge(config.creatorUid);
        }
    }

    /**
     * Updates the title and summary; may indirectly call notifyChanged().
     */
    public void refresh() {
        if (mForSavedNetworks) {
            setTitle(mAccessPoint.getConfigName());
        } else {
            setTitle(mAccessPoint.getSsid());
        }

        final Context context = getContext();
//        int level = mAccessPoint.getLevel();
        int level = getWifiLevel(mAccessPoint);
        if (level != mLevel) {
            mLevel = level;
            updateIcon(mLevel, context);
            notifyChanged();
        }
        updateBadge(context);

        setSummary(mForSavedNetworks ? mAccessPoint.getSavedNetworkSummary()
                : mAccessPoint.getSettingsSummary());

        mContentDescription = getTitle();
        if (getSummary() != null) {
            mContentDescription = TextUtils.concat(mContentDescription, ",", getSummary());
        }
        if (level >= 0 && level < WIFI_CONNECTION_STRENGTH.length) {
            mContentDescription = TextUtils.concat(mContentDescription, ",",
                    getContext().getString(WIFI_CONNECTION_STRENGTH[level]));
        }
    }

    @Override
    protected void notifyChanged() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            // Let our BG thread callbacks call setTitle/setSummary.
            postNotifyChanged();
        } else {
            super.notifyChanged();
        }
    }

    public void onLevelChanged() {
        postNotifyChanged();
    }

    private void postNotifyChanged() {
        if (mTitleView != null) {
            mTitleView.post(mNotifyChanged);
        } // Otherwise we haven't been bound yet, and don't need to update.
    }

    private final Runnable mNotifyChanged = new Runnable() {
        @Override
        public void run() {
            notifyChanged();
        }
    };

    public static class UserBadgeCache {
        private final SparseArray<Drawable> mBadges = new SparseArray<>();
        private final PackageManager mPm;

        UserBadgeCache(PackageManager pm) {
            mPm = pm;
        }

        private Drawable getUserBadge(int userId) {
            int index = mBadges.indexOfKey(userId);
            if (index < 0) {
                Drawable badge = mPm.getUserBadgeForDensity(new UserHandle(userId), 0 /* dpi */);
                mBadges.put(userId, badge);
                return badge;
            }
            return mBadges.valueAt(index);
        }
    }

    /**
     * Init widget
     *
     * @param rootView the root view
     * @date Liuqin on 2017-03-28
     */
    private void initWidget(View rootView) {
        if (!mShowWidget) {
            return;
        }
        View widgetWifiSignal = rootView;
        mIvSignalWifi = (ImageView) widgetWifiSignal.findViewById(R.id.next_widget_icon);
        mIvSignalDetailArrow = (ImageView) widgetWifiSignal.findViewById(R.id.next_widget_arrow);
        mTvSignalText = (TextView) widgetWifiSignal.findViewById(R.id.next_widget_status);
        widgetWifiSignal.setVisibility(View.VISIBLE);

        mIvSignalWifi.setImageDrawable(getIcon());
        mIvSignalDetailArrow.setTag(rootView);
        mIvSignalDetailArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View itemView = (View) view.getTag();
                if (itemView != null) {
                    itemView.showContextMenu();
                }
            }
        });
    }

    @Override
    public Drawable getIcon() {
        return mIcon;
    }

    @Override
    public void setIcon(Drawable icon) {
        if (icon == null && this.mIcon != null || icon != null && this.mIcon != icon) {
            this.mIcon = icon;
            this.notifyChanged();
        }
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        if (mShowWidget) {
            setWidgetLayoutResource(R.layout.preference_widget_next_page_detail);
        }
        return super.onCreateView(parent);
    }

    private int getWifiLevel(AccessPoint accessPoint) {
        int rssi = getWifiRssi(accessPoint);
        if (rssi == Integer.MAX_VALUE) {
            return accessPoint.getLevel();
        }
        return WifiManager.calculateSignalLevel(rssi, 5);
    }

    private Integer getWifiRssi(AccessPoint accessPoint) {
        try {
            Field field = mRssiField;
            if (field == null) {
                field = AccessPoint.class.getDeclaredField("mRssi");
                field.setAccessible(true);
                mRssiField = field;
            }
            return (Integer) field.get(accessPoint);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Integer.MAX_VALUE;
    }
}
