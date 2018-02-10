package com.protruly.powermanager.utils;

import android.content.Context;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.protruly.powermanager.R;

import java.text.Collator;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 60 * 60;
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;


    public static final TextUtils.SimpleStringSplitter mStringColonSplitter =
            new TextUtils.SimpleStringSplitter(':');

    public static boolean isRadioAllowed(Context context, String type) {
        if (!isAirplaneModeOn(context)) {
            return true;
        }

        String toggleable = Settings.Global.getString(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS);
        return toggleable != null && toggleable.contains(type);
    }

    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }



    public static InputMethodInfo getDefInputMethod(Context context) {
        String defInput = android.provider.Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD);
        InputMethodManager mImm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> mImis = mImm.getEnabledInputMethodList();
        for (int i = 0; i < mImis.size(); i++) {
            InputMethodInfo info = mImis.get(i);
            if (info.getId().equals(defInput)) {
                return info;
            }
        }
        return null;
    }

    /**
     * @param context
     * @return
     */
    public static String getApplicationFilesPath(Context context) {
        if (context == null || context.getFilesDir() == null) {
            return null;
        } else {
            return context.getFilesDir().getPath();
        }
    }

    public static boolean isSDCardReady() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns elapsed time for the given millis, in the following format:
     * 2d 5h 40m 29s
     * @param context the application context
     * @param millis the elapsed time in milli seconds
     * @param withSeconds include seconds?
     * @return the formatted elapsed time
     */
    public static String formatElapsedTime(Context context, double millis, boolean withSeconds) {
        StringBuilder sb = new StringBuilder();
        int seconds = (int) Math.floor(millis / 1000);
        if (!withSeconds) {
            // Round up.
            seconds += 30;
        }

        int days = 0, hours = 0, minutes = 0;
        if (seconds >= SECONDS_PER_DAY) {
            days = seconds / SECONDS_PER_DAY;
            seconds -= days * SECONDS_PER_DAY;
        }
        if (seconds >= SECONDS_PER_HOUR) {
            hours = seconds / SECONDS_PER_HOUR;
            seconds -= hours * SECONDS_PER_HOUR;
        }
        if (seconds >= SECONDS_PER_MINUTE) {
            minutes = seconds / SECONDS_PER_MINUTE;
            seconds -= minutes * SECONDS_PER_MINUTE;
        }
        if (withSeconds) {
            if (days > 0) {
                sb.append(context.getString(R.string.battery_history_days,
                        days, hours, minutes, seconds));
            } else if (hours > 0) {
                sb.append(context.getString(R.string.battery_history_hours,
                        hours, minutes, seconds));
            } else if (minutes > 0) {
                sb.append(context.getString(R.string.battery_history_minutes, minutes, seconds));
            } else {
                sb.append(context.getString(R.string.battery_history_seconds, seconds));
            }
        } else {
            if (days > 0) {
                sb.append(context.getString(R.string.battery_history_days_no_seconds,
                        days, hours, minutes));
            } else if (hours > 0) {
                sb.append(context.getString(R.string.battery_history_hours_no_seconds,
                        hours, minutes));
            } else {
                sb.append(context.getString(R.string.battery_history_minutes_no_seconds, minutes));
            }
        }
        return sb.toString();
    }

    /** Formats a double from 0.0..1.0 as a percentage. */
    private static String formatPercentage(double percentage) {
        return NumberFormat.getPercentInstance().format(percentage);
    }

    /** Formats an integer from 0..100 as a percentage. */
    public static String formatPercentage(int percentage) {
        return formatPercentage(((double) percentage) / 100.0);
    }

    /**
     * 返回的拼音为大写字母
     *
     * @param str
     * @return
     */
    public static String getSpell(String str) {
        StringBuffer buffer = new StringBuffer();

        if (str != null && !str.equals("")) {
            char[] cc = str.toCharArray();
            for (int i = 0; i < cc.length; i++) {
                ArrayList<HanziToPinyin.Token> mArrayList = HanziToPinyin.getInstance().get(
                        String.valueOf(cc[i]));
                if (mArrayList.size() > 0) {
                    String n = mArrayList.get(0).target;
                    buffer.append(n);
                }
            }
        }
        String spellStr = buffer.toString().trim();
        return spellStr.toUpperCase().replaceAll("\\u00A0","");
    }

    /**
     * 比较两个拼音string的大小
     *
     * @param s1
     * @param s2
     * @return
     */
    public static int compare(String s1, String s2) {
        if (StringUtils.isEmpty(s1) && StringUtils.isEmpty(s2)) {
            return 0;
        } else if (StringUtils.isEmpty(s1) && !StringUtils.isEmpty(s2)) {
            return -1;
        } else if (!StringUtils.isEmpty(s1) && StringUtils.isEmpty(s2)) {
            return 1;
        }

        int lhs_ascii = s1.toUpperCase().charAt(0);
        int rhs_ascii = s2.toUpperCase().charAt(0);
        // 判断若不是字母，则排在字母之后
        if (lhs_ascii < 65 || lhs_ascii > 90)
            return 1;
        else if (rhs_ascii < 65 || rhs_ascii > 90)
            return -1;

        Collator collator = ((java.text.RuleBasedCollator) java.text.Collator
                .getInstance(java.util.Locale.ENGLISH));
        return collator.compare(s1, s2);
    }
}