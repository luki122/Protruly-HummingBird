package com.android.calendar.hb.legalholiday;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.text.format.Time;

import com.android.calendar.Utils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LegalHolidayUtils implements LegalHoliday {

    public static final String TAG = "LegalHoliday";

    private static final String INIT_NETWORK_DATA_JULIANDAY = "init_network_data_julianday";
    private static final String KEY_LEGAL_HOLIDAY = "preference_legal_holiday";
    private static final String KEY_LEGAL_WORKDAY = "preference_legal_workday";

    private static final String SEPARATOR = ",";

    private static LegalHolidayUtils sInstance = null;
    private Map<Integer, Integer> mLegalHolidayMap = null;

    private Callback callback;

    public interface Callback {
        void dataChanged();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    private void notifyDataChanged() {
        if (callback != null) {
            callback.dataChanged();
        }
    }

    private LegalHolidayUtils() {
        mLegalHolidayMap = new LinkedHashMap<>();
    }

    public static LegalHolidayUtils getInstance() {
        if (sInstance == null) {
            sInstance = new LegalHolidayUtils();
        }

        return sInstance;
    }

    public static void initData(Context context) {
        int lastJulianDay = Utils.getSharedPreference(context, INIT_NETWORK_DATA_JULIANDAY, 0);

        Time now = new Time();
        now.setToNow();
        int todayJulianDay = Time.getJulianDay(now.toMillis(true), now.gmtoff);

        if (lastJulianDay == todayJulianDay) {
            initLocalData(context);
            return;
        }

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isAvailable()) {
            LegalHolidayTask task = new LegalHolidayTask(context);
            task.execute((String) null);
        } else {
            initLocalData(context);
        }
    }

    public static void initNetworkData(Context context, LegalHolidayResponseBody legalHoliday) {
        getInstance().setNetworkData(context,
                legalHoliday.getHolidayList(), legalHoliday.getWorkdayList());

        Time now = new Time();
        now.setToNow();
        int todayJulianDay = Time.getJulianDay(now.toMillis(true), now.gmtoff);
        Utils.setSharedPreference(context, INIT_NETWORK_DATA_JULIANDAY, todayJulianDay);
    }

    public static void initLocalData(Context context) {
        String holidayStr = Utils.getSharedPreference(context, KEY_LEGAL_HOLIDAY, "");
        String workdayStr = Utils.getSharedPreference(context, KEY_LEGAL_WORKDAY, "");

        getInstance().setLocalData(holidayStr, workdayStr);
    }

    private void setNetworkData(Context context,
                                List<Integer> holidayList, List<Integer> workdayList) {
        mLegalHolidayMap.clear();

        setDayList(context, KEY_LEGAL_HOLIDAY, holidayList, DAY_TYPE_HOLIDAY);
        setDayList(context, KEY_LEGAL_WORKDAY, workdayList, DAY_TYPE_WORKDAY);

        notifyDataChanged();
    }

    private void setLocalData(String holidayStr, String workdayStr) {
        mLegalHolidayMap.clear();

        if (TextUtils.isEmpty(holidayStr) && TextUtils.isEmpty(workdayStr)) {
            initLocalData();
        } else {
            setDayList(holidayStr, DAY_TYPE_HOLIDAY);
            setDayList(workdayStr, DAY_TYPE_WORKDAY);
        }
    }

    private void setDayList(Context context, String key, List<Integer> dayList, int dayType) {
        if (dayList != null && !dayList.isEmpty()) {
            String dayStr = "";
            for (int day : dayList) {
                dayStr += String.valueOf(day);
                dayStr += SEPARATOR;

                mLegalHolidayMap.put(day, dayType);
            }
            if (!TextUtils.isEmpty(dayStr)) {
                Utils.setSharedPreference(context, key, dayStr);
            }
        }
    }

    private void setDayList(String dayStr, int dayType) {
        if (!TextUtils.isEmpty(dayStr)) {
            String[] days = dayStr.split(SEPARATOR);
            for (String day : days) {
                mLegalHolidayMap.put(Integer.parseInt(day), dayType);
            }
        }
    }

    private void initLocalData() {
        Time t = new Time();
        t.year = 2016;
        t.month = 0;
        t.monthDay = 1;
        t.normalize(true);
        int firstJulianDay2016 = Time.getJulianDay(t.toMillis(true), t.gmtoff);

        int yearDays2016[] = {0, 1, 2, 36, 37, 38, 39, 40, 41, 42, 43, 44, 92, 93, 94, 120, 121, 122, 160, 161, 162, 163,
                258, 259, 260, 261, 274, 275, 276, 277, 278, 279, 280, 281, 282};
        int dayType2016[] = {1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2,
                1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 2};

        for (int i = 0; i < yearDays2016.length; i++) {
            mLegalHolidayMap.put(firstJulianDay2016 + yearDays2016[i], dayType2016[i]);
        }

        t.year = 2017;
        t.month = 0;
        t.monthDay = 1;
        t.normalize(true);
        int firstJulianDay2017 = Time.getJulianDay(t.toMillis(true), t.gmtoff);

        int yearDays2017[] = {-1, 0, 1, 21, 26, 27, 28, 29, 30, 31, 32, 34, 90, 91, 92, 93, 118, 119, 120, 146, 147, 148, 149,
                272, 273, 274, 275, 276, 277, 278, 279, 280};
        int dayType2017[] = {1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 2, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1,
                2, 1, 1, 1, 1, 1, 1, 1, 1};

        for (int i = 0; i < yearDays2017.length; i++) {
            mLegalHolidayMap.put(firstJulianDay2017 + yearDays2017[i], dayType2017[i]);
        }
    }

    @Override
    public int getDayType(int julianDay) {
        if (mLegalHolidayMap.containsKey(julianDay)) {
            return mLegalHolidayMap.get(julianDay);
        }

        return DAY_TYPE_NORMAL;
    }
}
