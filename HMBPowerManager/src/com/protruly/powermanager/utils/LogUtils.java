package com.protruly.powermanager.utils;

import android.util.Log;

/**
 * This class is designed for simple finding of log information when we got logs.
 * We can simply search "HMBPM@@@" to find all information about this application.
 * Or we can search class name for specific as we did usually.
 */
public class LogUtils {
    /**
     * This TAG can retrieve all the logs about this application.
     */
    private static final String TAG = "HMBPM";

    /**
     * Flag to turn on or off debug logs.
     */
    private static final boolean DEBUG = true;

    /**
     * Simple method to write debug log message. This method will do nothing if DEBUG is false.
     */
    public static void d(String tag, String log) {
        if (DEBUG) {
            Log.d(TAG + "@@@" + tag, log);
        }
    }

    /**
     * Simple method to write error log message. This method will do nothing if DEBUG is false.
     */
    public static void e(String tag, String log) {
        if (DEBUG) {
            Log.e(TAG + "@@@" + tag, log);
        }
    }
}