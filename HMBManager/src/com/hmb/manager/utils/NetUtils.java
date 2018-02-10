package com.hmb.manager.utils;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetUtils {

    /**
     * If there is a network connection connected
     *
     * @param context
     * @return true if there is a network connection connected
     */
    public static boolean isOnline(Context context) {
        Context c = context.getApplicationContext();
        return isWifiOnline(c) || isMobileOnline(c);
    }

    /**
     * If there is a wifi network connection connected
     *
     * @param context
     * @return true if there is a wifi network connection connected
     */
    public static boolean isWifiOnline(Context context) {
        Context c = context.getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager) c
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()
                && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }
        return false;
    }

    /**
     * If there is a mobile network connection connected
     *
     * @param context
     * @return true if there is a mobile network connection connected
     */
    public static boolean isMobileOnline(Context context) {
        Context c = context.getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager) c
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()
                && netInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            return true;
        }
        return false;
    }
}