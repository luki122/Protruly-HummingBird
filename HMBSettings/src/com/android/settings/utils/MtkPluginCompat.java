package com.android.settings.utils;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hb.preference.Preference;
import hb.preference.PreferenceGroup;
import hb.preference.SwitchPreference;

/**
 * Created by liuqin on 17-5-19.
 *
 */
public class MtkPluginCompat extends android.preference.PreferenceCategory{
    private List<hb.preference.Preference> mPreferenceList = new ArrayList<>();
    private Map<CharSequence, android.preference.Preference> mMtkPreferenceMap =
            new HashMap<>();

    public MtkPluginCompat(Context context) {
        super(context);
    }

    @Override
    public boolean addPreference(android.preference.Preference preference) {
        mPreferenceList.add(convert(preference));
        mMtkPreferenceMap.put(preference.getKey(), preference);
        return true;
    }

    public boolean addPreference(PreferenceGroup preferenceGroup) {
        for (Preference preference : mPreferenceList) {
            preferenceGroup.addPreference(preference);
        }
        return true;
    }

    public void clear() {
        mPreferenceList.clear();
        mMtkPreferenceMap.clear();
    }

    private Preference convert(android.preference.Preference preference) {
        hb.preference.Preference hbPreference = new hb.preference.Preference(getContext());
        hbPreference.setKey(preference.getKey());
        hbPreference.setTitle(preference.getTitle());
        hbPreference.setSummary(preference.getSummary());
        hbPreference.setIntent(preference.getIntent());
        if (preference.getOnPreferenceChangeListener() != null) {
            hbPreference.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
        }
        if (preference.getOnPreferenceClickListener() != null) {
            hbPreference.setOnPreferenceClickListener(mOnPreferenceClickListener);
        }
        mMtkPreferenceMap.put(preference.getTitle(), preference);
        return hbPreference;
    }

    private hb.preference.Preference.OnPreferenceChangeListener mOnPreferenceChangeListener =
            new hb.preference.Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    android.preference.Preference mtkPreference =
                            mMtkPreferenceMap.get(preference.getTitle());
                    if (mtkPreference != null) {
                        return mtkPreference.getOnPreferenceChangeListener()
                                .onPreferenceChange(mtkPreference, o);
                    }
                    return true;
                }
            };

    private hb.preference.Preference.OnPreferenceClickListener mOnPreferenceClickListener =
            new hb.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    android.preference.Preference mtkPreference =
                            mMtkPreferenceMap.get(preference.getTitle());
                    if (mtkPreference != null) {
                        return mtkPreference.getOnPreferenceClickListener()
                                .onPreferenceClick(mtkPreference);
                    }
                    return true;
                }
            };
}
