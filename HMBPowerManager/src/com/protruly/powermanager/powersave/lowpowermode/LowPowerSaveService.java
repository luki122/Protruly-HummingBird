package com.protruly.powermanager.powersave.lowpowermode;

import android.app.IntentService;
import android.content.Intent;

import com.protruly.powermanager.utils.LogUtils;

public class LowPowerSaveService extends IntentService {
    private static final String TAG = LowPowerSaveService.class.getSimpleName();

    public static final String EXTRA_LOW_POWER_SAVE = "enable";
    public static final String ACTION_LOW_POWER_SAVE = "hmb.intent.action.ACTION_LOW_POWER_SAVE";

    private final Object mLock = new Object();
    private LowPowerSaveManager mLowPowerSaveManager;

    public LowPowerSaveService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.d(TAG, "onCreate()");
        mLowPowerSaveManager = LowPowerSaveManager.getInstance(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            LogUtils.d(TAG, "onHandleIntent() -> could not handle null intent or action");
            return;
        }

        if (!intent.getAction().equals(ACTION_LOW_POWER_SAVE)) {
            return;
        }

        boolean enable = intent.getBooleanExtra(EXTRA_LOW_POWER_SAVE, false);
        LogUtils.d(TAG, "onHandleIntent() -> enable = " + enable);
        synchronized (mLock) {
            mLowPowerSaveManager.setLowPowerSaveMode(enable);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d(TAG, "onDestroy()");
    }
}