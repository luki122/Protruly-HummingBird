package com.android.launcher3.dynamicui;

import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import com.android.launcher3.AppInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.util.Thunk;

import java.util.ArrayList;

/**
 * Created by liuzuo on 17-4-1.
 */

public class DynamicProvider {


    private final String TAG = "DynamicProvider";
    private final int NORMAL = 10;
    private final int CLOCKDYNAMIC = 0;
    private final int CALENDARDYNAMIC = 1;
    private final int WEATHERDYNAMIC = 2;
    String[] mPackageName;

    static final HandlerThread sWorkerThread = new HandlerThread("launcher-dynamic");
    static {
        sWorkerThread.start();
    }
    @Thunk static final Handler sWorker = new Handler(sWorkerThread.getLooper());

    private DeskClockDynamic mDeskClockDynamic;
    private CalendarDynamic mCalendarDynamic;
    private WeatherDynamic mWeatherDynamic;

    private static ArrayList<IDynamicIcon> mDynamicList;

    public DynamicProvider() {

    }

    private static DynamicProvider mInstance = null;

    public DynamicProvider(Context context) {
        mPackageName = context.getResources().getStringArray(R.array.dym_package_name);
    }

    public static DynamicProvider getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DynamicProvider(context);
            mDynamicList = new ArrayList();
        }
        return mInstance;
    }

    public IDynamicIcon createDynamicIcon(ItemInfo info) {
        switch (isDynamicIcon(info)) {
            case NORMAL:
                return null;
            case CLOCKDYNAMIC:
                if (mDeskClockDynamic == null)
                    mDeskClockDynamic = new DeskClockDynamic();

                if (!mDynamicList.contains(mDeskClockDynamic))
                    mDynamicList.add(mDeskClockDynamic);
                return mDeskClockDynamic;
            case CALENDARDYNAMIC:
                if (mCalendarDynamic == null)
                    mCalendarDynamic = new CalendarDynamic();

                if (!mDynamicList.contains(mCalendarDynamic))
                    mDynamicList.add(mCalendarDynamic);
                return mCalendarDynamic;
            case WEATHERDYNAMIC:
                if (mWeatherDynamic == null)
                    mWeatherDynamic = new WeatherDynamic();

                if (!mDynamicList.contains(mWeatherDynamic))
                    mDynamicList.add(mWeatherDynamic);
                return mWeatherDynamic;
        }
        return null;
    }

    private int isDynamicIcon(ItemInfo info) {
        for (int i = 0; i < mPackageName.length; i++) {
            if (info instanceof ShortcutInfo || info instanceof AppInfo) {
                if (info.getIntent() != null) {
                    if(info.getIntent().getComponent()!=null) {
                        ComponentName cn = info.getIntent().getComponent();
                        if (mPackageName[i].equals(cn.getPackageName())) {
                            return i;
                        }
                    }
                }
            }
        }
        return NORMAL;
    }

    public ArrayList<IDynamicIcon> getAllDynamicIcon() {
        return mDynamicList;
    }
}
