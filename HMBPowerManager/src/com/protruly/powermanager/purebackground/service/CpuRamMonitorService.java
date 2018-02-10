package com.protruly.powermanager.purebackground.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;

import com.android.internal.os.ProcessCpuTracker;
import com.protruly.powermanager.utils.LogUtils;
import com.protruly.powermanager.utils.SystemUtils;


public class CpuRamMonitorService extends Service {
    private static final String TAG = "CpuRamMonitorService";

    private static final int MSG_MONITOR = 1;
    private static final int MONITOR_PERIOD = 30000;
    private static final double MAX_CPU_OVERLOAD_TRIGGER = 30;
    private static final double MAX_USED_RAM_RATIO_TRIGGER = 0.80;

    private CpuTracker mTracker;

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.d(TAG, "onCreate");
        mTracker = new CpuTracker();
        mTracker.init();

        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm.isInteractive()) {
            LogUtils.d(TAG, "isInteractive() -> sendEmptyMessage");
            mHandler.sendEmptyMessage(MSG_MONITOR);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d(TAG, "onDestroy()");
        mHandler.removeMessages(MSG_MONITOR);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static final class CpuTracker extends ProcessCpuTracker {
        float mLoad1 = 0;
        float mLoad5 = 0;
        float mLoad15 = 0;

        CpuTracker() {
            super(false);
        }

        @Override
        public void onLoadChanged(float load1, float load5, float load15) {
            mLoad1 = load1;
            mLoad5 = load5;
            mLoad15 = load15;
            LogUtils.d(TAG, "onLoadChanged() -> mLoad1 = " + mLoad1 + ", mLoad5 = "
                    + mLoad5 + ", mLoad15 = " + mLoad15);
        }
    }

    private void processCpuOrMemOverload() {
        mTracker.update();
        double usedRamRatio = SystemUtils.getInstance(this).getRatioUsedMem();
        LogUtils.d(TAG, "processCpuOrMemOverload() -> usedRamRatio = " + usedRamRatio
                + ", Load1 = " + mTracker.mLoad1
                + ", Load5 = " + mTracker.mLoad5
                + ", Load15 = " + mTracker.mLoad15);
        if (usedRamRatio > MAX_USED_RAM_RATIO_TRIGGER || mTracker.mLoad1 >= MAX_CPU_OVERLOAD_TRIGGER) {
            LogUtils.d(TAG, "processCpuOrMemOverload() -> Overload!!!");
            Intent intent = new Intent(CpuRamMonitorService.this, BGCleanService.class);
            intent.setAction(BGCleanService.ACTION_CLEAN_BG);
            intent.putExtra(BGCleanService.BG_CLEAN_TYPE, BGCleanService.OVERLOAD_BG_CLEAN);
            startService(intent);
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_MONITOR) {
                processCpuOrMemOverload();
                Message m = obtainMessage(MSG_MONITOR);
                sendMessageDelayed(m, MONITOR_PERIOD);
            }
        }
    };
}