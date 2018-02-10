package com.mediatek.security.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceUtils {

    private static final String KEY_HAS_LAUNCH = "key_has_launch";

    public static boolean hasLaunch(Context context) {
        if (context != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            boolean result = sp.getBoolean(KEY_HAS_LAUNCH, false);
            return result;
        }
        return true;
    }

    public static void setHasLaunch(Context context, boolean hasLaunch) {
        if (context != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putBoolean(KEY_HAS_LAUNCH, hasLaunch).commit();
        }
    }

}
