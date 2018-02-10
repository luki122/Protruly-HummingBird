package com.android.provision;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by xiaobin on 17-6-29.
 */

public class Utils {

    public static void goNext(Context context, String from) {
        Intent next = new Intent("com.android.provision.TRANSFER");
        next.putExtra("from", from);
        context.startActivity(next);
    }

    private static final String PRE_HAS_FINISH = "pre_has_finish";

    public static void setHasFinish(Context context, boolean hasFinish) {
        if (context != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putBoolean(PRE_HAS_FINISH, hasFinish).commit();
        }
    }

    public static boolean getHasFinish(Context context) {
        if (context != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            boolean result = sp.getBoolean(PRE_HAS_FINISH, false);
            return result;
        }
        return false;
    }

}
