package com.protruly.powermanager.utils;


import android.app.ActivityManager;
import android.content.Context;

import com.android.internal.util.MemInfoReader;

public class SystemUtils {

    private Context mContext;
    private ActivityManager mAm;

    private static MemInfoReader mMemInfoReader = new MemInfoReader();
    private static ActivityManager.MemoryInfo mMemInfo = new ActivityManager.MemoryInfo();

    private static SystemUtils sInstance;
    private static final Object LOCK = new Object();

    public static SystemUtils getInstance(Context context) {
        synchronized (LOCK) {
            if (sInstance == null) {
                sInstance = new SystemUtils(context);
            }
            return sInstance;
        }
    }

    private SystemUtils(Context context) {
        mContext = context.getApplicationContext();
        mAm = (ActivityManager) mContext.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
    }

    /**
     * To get total memory size
     *
     * @return size of total memory
     */
    public long getTotalMem() {
        mMemInfoReader.readMemInfo();
        return mMemInfoReader.getTotalSize();
    }

    /**
     * To get available memory size
     *
     * @return size of avail memory
     */
    public long getAvailMem() {
        mAm.getMemoryInfo(mMemInfo);
        return mMemInfo.availMem;
    }

    public double getRatioUsedMem() {
        return (double) (getTotalMem() - getAvailMem()) / getTotalMem();
    }
}