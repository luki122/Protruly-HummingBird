package com.hb.netmanage.utils;

import android.util.Log;

/**
 * Created by zhaolaichao on 17-4-18.
 */

public class LogUtil {
    
    public static final boolean IS_DEBUG = false;    //是否打印log

    /**
     * 以级别为 d 的形式输出LOG,输出debug调试信息
     */
    public static void d(String tag, String msg) {
        if (IS_DEBUG) {
            Log.d(tag, msg);
        }
    }
    /**
     * 以级别为 i 的形式输出LOG,一般提示性的消息information
     */
    public static void i(String tag, String msg) {
        if (IS_DEBUG) {
            Log.i(tag, msg);
        }
    }
    /**
     * 以级别为 w 的形式输出LOG,显示warning警告，一般是需要我们注意优化Android代码
     */
    public static void w(String tag, String msg) {
        if (IS_DEBUG){
            Log.w(tag, msg);
        }
    }
    /**
     * 以级别为 e 的形式输出LOG ，红色的错误信息，查看错误源的关键
     */
    public static void e(String tag, String msg) {
        if (IS_DEBUG) {
            Log.e(tag, msg);
        }
    }
    /**
     * 以级别为 v 的形式输出LOG
     *
     */
    public static void v(String tag, String msg) {
        if (IS_DEBUG) {
            Log.v(tag, msg);
        }
    }
}
