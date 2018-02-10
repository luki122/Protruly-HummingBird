package com.android.calendar.hb.legalholiday;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.text.format.Time;

import com.android.calendar.Utils;
import com.protruly.clouddata.appdata.AppDataAgent;
import com.protruly.clouddata.appdata.listener.OnlineConfigureListener;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class LegalHolidayUtils2 implements LegalHoliday {

    public static final String TAG = "LegalHoliday";

    private static final String INIT_NETWORK_DATA_JULIANDAY = "init_network_data_julianday";
    private static final String PARAM_HOLIDAY = "holidayList";
    private static final String PARAM_WORKDAY = "workdayList";
    private static final String SEPARATOR = ",";

    private static LegalHolidayUtils2 sInstance = null;
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

    private LegalHolidayUtils2() {
        mLegalHolidayMap = new LinkedHashMap<>();
    }

    public static LegalHolidayUtils2 getInstance() {
        if (sInstance == null) {
            sInstance = new LegalHolidayUtils2();
        }

        return sInstance;
    }

    public static void initData(Context context) {
        initLocalData(context);

        int lastJulianDay = Utils.getSharedPreference(context, INIT_NETWORK_DATA_JULIANDAY, 0);

        Time now = new Time();
        now.setToNow();
        int todayJulianDay = Time.getJulianDay(now.toMillis(true), now.gmtoff);

        if (lastJulianDay == todayJulianDay) {
            return;
        }

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isAvailable()) {
            initNetworkData(context);
        }
    }

    private static void initNetworkData(final Context context) {
        AppDataAgent.setOnlineConfigureListener(context, new OnlineConfigureListener() {
            @Override
            public void onCfgChanged(JSONObject jsonObject) {
                initLocalData(context);
                getInstance().notifyDataChanged();
            }
        });
        AppDataAgent.updateOnlineConfig(context);
        setInitNetworkDataJulianday(context);
    }

    private static void setInitNetworkDataJulianday(Context context) {
        Time now = new Time();
        now.setToNow();
        int todayJulianDay = Time.getJulianDay(now.toMillis(true), now.gmtoff);
        Utils.setSharedPreference(context, INIT_NETWORK_DATA_JULIANDAY, todayJulianDay);
    }

    private static void initLocalData(Context context) {
        String holidayStr = AppDataAgent.getConfigParams(context, PARAM_HOLIDAY);
        String workdayStr = AppDataAgent.getConfigParams(context, PARAM_WORKDAY);

        getInstance().setLocalData(holidayStr, workdayStr);
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
