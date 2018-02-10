package cn.protruly.spiderman.transmitservice;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by lijia on 17-7-5.
 */

public class SharedPrefsUtil {

    public static final String UPDATEDATATIME = "update_data_time";

    public static void putValue(Context context, String key, long value) {
        SharedPreferences.Editor sp =  context.getSharedPreferences(UPDATEDATATIME, Context.MODE_PRIVATE).edit();
        sp.putLong(key, value);
        sp.commit();
    }
    public static void putValue(Context context,String key, boolean value) {
        SharedPreferences.Editor sp =  context.getSharedPreferences(UPDATEDATATIME, Context.MODE_PRIVATE).edit();
        sp.putBoolean(key, value);
        sp.commit();
    }
    public static void putValue(Context context,String key, String value) {
        SharedPreferences.Editor sp =  context.getSharedPreferences(UPDATEDATATIME, Context.MODE_PRIVATE).edit();
        sp.putString(key, value);
        sp.commit();
    }
    public static long getValue(Context context,String key, Long defValue) {
        SharedPreferences sp =  context.getSharedPreferences(UPDATEDATATIME, Context.MODE_PRIVATE);
        long value = sp.getLong(key, defValue);
        return value;
    }
    public static boolean getValue(Context context,String key, boolean defValue) {
        SharedPreferences sp =  context.getSharedPreferences(UPDATEDATATIME, Context.MODE_PRIVATE);
        boolean value = sp.getBoolean(key, defValue);
        return value;
    }
    public static String getValue(Context context,String key, String defValue) {
        SharedPreferences sp =  context.getSharedPreferences(UPDATEDATATIME, Context.MODE_PRIVATE);
        String value = sp.getString(key, defValue);
        return value;
    }

}
