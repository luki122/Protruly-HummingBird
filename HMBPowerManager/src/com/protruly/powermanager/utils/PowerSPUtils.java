package com.protruly.powermanager.utils;


import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;

public class PowerSPUtils {
    private static final String PREFERENCES_FILE_NAME = "power_preferences";

    public static final String KEY_POWER_SAVE_MODE = "power_mode";
    public static final String KEY_AUTO_CLEAN_BACKGROUND = "clean_bg";

    private SharedPreferences mPreferences;
    private static PowerSPUtils sPowerPreferences;

    public static synchronized PowerSPUtils instance(Context context) {
        if (sPowerPreferences == null) {
            sPowerPreferences = new PowerSPUtils(context);
        }
        return sPowerPreferences;
    }

    private PowerSPUtils(Context context) {
        mPreferences = context.getSharedPreferences(
                PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
    }

    public SharedPreferences getPreferences() {
        return mPreferences;
    }

    public boolean getBooleanValue(String key, boolean def) {
        return mPreferences.getBoolean(key, def);
    }

    public int getIntValue(String key, int def) {
        return mPreferences.getInt(key, def);
    }

    public float getFloatValue(String key, float def) {
        return mPreferences.getFloat(key, def);
    }

    public long getLongValue(String key, long def) {
        return mPreferences.getLong(key, def);
    }

    public String getStringValue(String key, String def) {
        return mPreferences.getString(key, def);
    }

    public Set<String> getStringSetValue(String key, Set<String> def) {
        return mPreferences.getStringSet(key, def);
    }

    public void setBooleanValue(String key, boolean val) {
        SharedPreferences.Editor edit = mPreferences.edit();
        edit.putBoolean(key, val);
        edit.apply();
    }

    public int setIntValue(String key, int oldVal, int newVal) {
        if (oldVal != newVal) {
            SharedPreferences.Editor edit = mPreferences.edit();
            edit.putInt(key, newVal);
            edit.apply();
        }
        return newVal;
    }

    public float setFloatValue(String key, float oldVal, float newVal) {
        if (oldVal != newVal) {
            SharedPreferences.Editor edit = mPreferences.edit();
            edit.putFloat(key, newVal);
            edit.apply();
        }
        return newVal;
    }

    public long setLongValue(String key, long oldVal, long newVal) {
        if (oldVal != newVal) {
            SharedPreferences.Editor edit = mPreferences.edit();
            edit.putLong(key, newVal);
            edit.apply();
        }
        return newVal;
    }

    public void setStringValue(String key, String val) {
        SharedPreferences.Editor edit = mPreferences.edit();
        edit.putString(key, val);
        edit.apply();
    }

    public void setStringSetValue(String key, Set<String> val) {
        SharedPreferences.Editor edit = mPreferences.edit();
        edit.putStringSet(key, val);
        edit.apply();
    }
}