package com.protruly.powermanager.purebackground.service;


import android.app.IntentService;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;

import com.protruly.powermanager.utils.LogUtils;

import java.util.HashMap;
import java.util.List;

public class UsageStatsMonitorService extends IntentService {
    private static final String TAG = "UsageStatsMonitorService";

    private Context mContext;

    public UsageStatsMonitorService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mContext = getApplicationContext();
        getFrequentAppList();
    }

    public void getFrequentAppList() {
        UsageStatsManager mUsageStatsManager =
                (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);
        final List<UsageStats> statsList = mUsageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, 0, System.currentTimeMillis());
        if (statsList == null) {
            return;
        }
        HashMap<String, UsageStats> usageMap = new HashMap<>();
        final int statCount = statsList.size();
        for (int i = 0; i < statCount; i++) {
            final android.app.usage.UsageStats pkgStats = statsList.get(i);
            usageMap.put(pkgStats.getPackageName(), pkgStats);
            LogUtils.d(TAG, "getFrequentAppList() -> pkgName = " + pkgStats.getPackageName()
                    + ", count = " + pkgStats.mLaunchCount);
        }
    }
}
