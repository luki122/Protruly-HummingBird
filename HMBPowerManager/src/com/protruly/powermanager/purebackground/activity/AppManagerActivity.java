package com.protruly.powermanager.purebackground.activity;

import android.content.Intent;
import android.os.Bundle;

import com.protruly.powermanager.R;

import hb.preference.Preference;
import hb.preference.PreferenceActivity;
import hb.preference.PreferenceScreen;


public class AppManagerActivity extends PreferenceActivity
        implements Preference.OnPreferenceClickListener {

    private static final String KEY_AUTO_START = "preference_auto_start";
    private static final String KEY_LOCK_SCREEN_CLEAN = "preference_lock_screen_clean";
    private static final String KEY_PERMISSION_CONTROL = "preference_permission_control";

    private PreferenceScreen mAutoStartPreference;
    private PreferenceScreen mLockScreenCleanPreference;
    private PreferenceScreen mPermissionControlPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.activity_app_manager);

        mAutoStartPreference = (PreferenceScreen) findPreference(KEY_AUTO_START);
        mAutoStartPreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
        mAutoStartPreference.setOnPreferenceClickListener(this);
        mLockScreenCleanPreference = (PreferenceScreen) findPreference(KEY_LOCK_SCREEN_CLEAN);
        mLockScreenCleanPreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
        mLockScreenCleanPreference.setOnPreferenceClickListener(this);
        mPermissionControlPreference = (PreferenceScreen) findPreference(KEY_PERMISSION_CONTROL);
        mPermissionControlPreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
        mPermissionControlPreference.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (mAutoStartPreference == preference) {
            startActivity(new Intent(this, AutoStartMgrActivity.class));
        } else if (mLockScreenCleanPreference == preference) {
            startActivity(new Intent(this, LockScreenCleanMgrActivity.class));
        } else if (mPermissionControlPreference == preference) {
            Intent i = new Intent("android.intent.action.HMB_MANAGE_PERMISSIONS");
            startActivity(i);
        }
        return true;
    }
}