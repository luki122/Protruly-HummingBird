package com.hb.note.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;

import com.hb.note.ui.ToastHelper;

public class SystemUtils {

    private static final String TAG = "SystemUtils";

    public static int getVersionCode(Context context) {
        int version = 0;
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package name not found", e);
        }
        return version;
    }

    public static String getVersionNumber(Context context) {
        String version = "?";
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package name not found", e);
        }
        return version;
    }

    public static String getApplicationName(Context context) {
        String name = "?";
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            name = context.getString(pi.applicationInfo.labelRes);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package name not found", e);
        }
        return name;
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }


    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static boolean isNull(String str) {
        return str == null || str.trim().equals("") || str.trim().toUpperCase().equals("NULL");
    }

    public static int getCharacterNum(final String source) {
        if (TextUtils.isEmpty(source)) {
            return 0;
        } else {
            return source.length();
        }
    }

    public static int getChineseCharacterNum(final String source) {
        int num = 0;
        char[] chars = source.toCharArray();
        for (char c : chars) {
            if ((char) (byte) c != c) {
                num++;
            }
        }
        return num;
    }

    public static void lengthFilter(final EditText editText, final int maxLength, final int resId) {
        InputFilter[] inputFilters = new InputFilter[1];
        inputFilters[0] = new InputFilter() {
            @Override
            public CharSequence filter(
                    CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                int sourceLength = getCharacterNum(source.toString());
                int destLength = getCharacterNum(dest.toString());
                if (sourceLength + destLength > maxLength) {
                    ToastHelper.show(resId);
                }
                return null;
            }
        };
        editText.setFilters(inputFilters);
    }

    public static int getRowStart(String content, int selStart) {
        int rowStart = 0;
        if (!TextUtils.isEmpty(content)) {
            rowStart = content.lastIndexOf(Globals.NEW_LINE, selStart);
            if (rowStart < 0) {
                rowStart = 0;
            } else if (rowStart == selStart) {
                rowStart = content.lastIndexOf(Globals.NEW_LINE, selStart - 1);
                if (rowStart < 0) {
                    rowStart = 0;
                } else {
                    rowStart += 1;
                }
            } else {
                rowStart += 1;
            }
        }
        return rowStart;
    }

    public static int getRowEnd(String content, int selStart) {
        int rowEnd = 0;
        if (!TextUtils.isEmpty(content)) {
            rowEnd = content.indexOf(Globals.NEW_LINE, selStart);
            if (rowEnd < 0) {
                rowEnd = content.length();
            }
        }
        return rowEnd;
    }
}
