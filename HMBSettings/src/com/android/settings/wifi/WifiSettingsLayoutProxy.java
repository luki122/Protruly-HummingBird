package com.android.settings.wifi;

import android.content.Context;

import com.android.settings.R;
import com.mediatek.settings.ext.DefaultWifiSettingsExt;

import hb.preference.Preference;
import hb.preference.PreferenceCategory;
import hb.preference.PreferenceScreen;

/**
 * @date Liuqin on 2017-03-27
 */
public class WifiSettingsLayoutProxy extends DefaultWifiSettingsExt {
    private PreferenceCategory mWifiCategory;

    @Override
    public void addCategories(PreferenceScreen screen) {
        ensureSearchedWifiCategory(screen);
        addWlanSettinsCategory(screen);
    }

    @Override
    public void addPreference(PreferenceScreen screen, Preference preference, boolean isConfiged) {
        if (screen != null) {
            PreferenceCategory category = ensureSearchedWifiCategory(screen);
            if (category.getPreferenceCount() <= 0) {
                screen.addPreference(category);
                if (mWifiCategory.findPreference("key_wlan_add_network") == null) {
                    Preference addWifiPreference =   new Preference(screen.getContext());
                    addWifiPreference.setKey("key_wlan_add_network");
                    addWifiPreference.setTitle(R.string.wifi_add_network_title);
                    addWifiPreference.setOrder(9999);
                    mWifiCategory.addPreference(addWifiPreference);
                }
            }
            category.addPreference(preference);
        }
    }


    @Override
    public void emptyScreen(PreferenceScreen screen) {
        emptyCategory(screen);
    }

    @Override
    public void emptyCategory(PreferenceScreen screen) {
        if (mWifiCategory != null) {
            mWifiCategory.removeAll();
            screen.removePreference(mWifiCategory);
        }
    }

    @Override
    public void refreshCategory(PreferenceScreen screen) {
        super.refreshCategory(screen);
    }

    private PreferenceCategory ensureSearchedWifiCategory(PreferenceScreen screen) {
        if (mWifiCategory == null) {
            Context context = screen.getContext();
            mWifiCategory = new PreferenceCategory(screen.getContext());
            mWifiCategory.setKey("key_wlan_searched_wifi");
            mWifiCategory.setTitle(context.getString(R.string.wlan_searched_wifi));
            mWifiCategory.setOrder(1);
        }
        return mWifiCategory;
    }

    private void addWlanSettinsCategory(PreferenceScreen screen) {
        screen.getPreferenceManager().inflateFromResource(screen.getContext(), R.xml.wifi_settings_section, screen);
    }

}
