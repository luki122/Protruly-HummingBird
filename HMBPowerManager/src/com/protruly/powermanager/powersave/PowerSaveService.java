package com.protruly.powermanager.powersave;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.WindowManager;

import com.protruly.powermanager.R;
import com.protruly.powermanager.powersave.lowpowermode.LowPowerSaveService;
import com.protruly.powermanager.utils.LogUtils;

import hb.app.dialog.AlertDialog;

/**
 * Power save service of HMBPowerManager, controls the power mode of the device.
 */
public class PowerSaveService extends Service {
    private static final String TAG = PowerSaveService.class.getSimpleName();

    public static final int MODE_NORMAL_POWER = 0;
    public static final int MODE_LOW_POWER = 1;
    public static final int MODE_SUPER_POWER = 2;
    public static final String ACTION_SWITCH_POWER_MODE = "hmb.intent.action.ACTION_SWITCH_POWER_MODE";

    private int mBatteryLevel = 100;
    private int mBatteryStatus = BatteryManager.BATTERY_STATUS_UNKNOWN;

    private Context mContext;
    private AlertDialog mDialog;
    private boolean isRegistered = false;
    private PowerSaveReceiver mPowerSaveReceiver;

    // For powersave mode change synchronization.
    private final Object mLock = new Object();

    // This should probably be exposed in the API, though it's not critical
    private static final int BATTERY_PLUGGED_NONE = 0;

    private static final int LOW_POWER_MODE_TRIGGER_LEVEL = 30;
    private static final int SUPER_POWER_MODE_TRIGGER_LEVEL = 10;
    private static final int EXIT_POWER_MODE_TRIGGER_LEVEL = 60;

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.d(TAG, "onCreate()");
        mContext = PowerSaveService.this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d(TAG, "onStartCommand()");
        if (!isRegistered) {
            registerPowerSaveReceiver();
            isRegistered = true;
        }
        parserPowerSaveIntent(intent);
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d(TAG, "onDestroy()");
        isRegistered = false;
        mContext.unregisterReceiver(mPowerSaveReceiver);
    }

    private void parserPowerSaveIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        if (action.equals(ACTION_SWITCH_POWER_MODE)) {
            Bundle bundle = intent.getExtras();
            int from = bundle.getInt("from");
            int to = bundle.getInt("to");
            LogUtils.d(TAG, "parserPowerSaveIntent() -> Switch Power Mode " + from + " to " + to);
            switchPowerSaveMode(from, to);
        }
    }

    private void switchPowerSaveMode(int from, int targetMode) {
        int currentMode = getCurrentPowerSaveMode();
        if (currentMode == targetMode) {
            return;
        }
        synchronized (mLock) {
            switch (targetMode) {
                case MODE_NORMAL_POWER:
                    if (currentMode == MODE_LOW_POWER) {
                        startLowPowerSaveModeService(false);
                    }
                    break;
                case MODE_LOW_POWER:
                    if (currentMode == MODE_NORMAL_POWER) {
                        startLowPowerSaveModeService(true);
                    }
                    break;
                case MODE_SUPER_POWER:
                    break;
            }
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.POWER_MODE, targetMode);
        }
    }

    private void startLowPowerSaveModeService(boolean enable) {
        Intent intent = new Intent(mContext, LowPowerSaveService.class);
        intent.setAction(LowPowerSaveService.ACTION_LOW_POWER_SAVE);
        intent.putExtra(LowPowerSaveService.EXTRA_LOW_POWER_SAVE, enable);
        startService(intent);
    }

    private int getCurrentPowerSaveMode() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.POWER_MODE, MODE_NORMAL_POWER);
    }

    private void registerPowerSaveReceiver() {
        mPowerSaveReceiver = new PowerSaveReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(mPowerSaveReceiver, filter);
    }

    private class PowerSaveReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                triggerByBatteryChanged(intent);
            }
        }
    }

    private void triggerByBatteryChanged(Intent intent) {
        final int oldBatteryLevel = mBatteryLevel;
        mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100);
        final int oldBatteryStatus = mBatteryStatus;
        mBatteryStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN);

        final int powerMode = getCurrentPowerSaveMode();
        LogUtils.d(TAG, "triggerByBatteryChanged() -> powerMode = " + powerMode
                + ", oldBatteryLevel = " + oldBatteryLevel
                + ", mBatteryLevel = " + mBatteryLevel
                + ", oldBatteryStatus = " + oldBatteryStatus
                + ", mBatteryStatus = " + mBatteryStatus);
        if (mBatteryLevel < oldBatteryLevel && powerMode == MODE_NORMAL_POWER
                && mBatteryStatus != BatteryManager.BATTERY_STATUS_CHARGING) {
            if (mBatteryLevel == LOW_POWER_MODE_TRIGGER_LEVEL) {
                showLowPowerWarning();
            } else if (mBatteryLevel == SUPER_POWER_MODE_TRIGGER_LEVEL) {
                showSuperPowerWarning();
            }
        } else if (mBatteryLevel > oldBatteryLevel && mBatteryLevel == EXIT_POWER_MODE_TRIGGER_LEVEL
                && powerMode == MODE_LOW_POWER) {
//            showBatteryEnough();
            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
            }
            switchPowerSaveMode(MODE_LOW_POWER, MODE_NORMAL_POWER);
        } else if (mBatteryStatus == BatteryManager.BATTERY_STATUS_CHARGING
                || mBatteryLevel < oldBatteryLevel) {
            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
            }
        }
    }

    private void showLowPowerWarning() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        mDialog = new AlertDialog.Builder(mContext)
                .setMessage(getResources().getString(R.string.title_low_power))
//                .setMessage(getResources().getString(R.string.message_low_power))
                .setNegativeButton(getResources().getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mDialog.dismiss();
                            }
                        })
                .setPositiveButton(getResources().getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switchPowerSaveMode(getCurrentPowerSaveMode(), MODE_LOW_POWER);
                    }
                }).setCancelable(false).create();
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mDialog.show();
    }

    private void showSuperPowerWarning() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        mDialog = new AlertDialog.Builder(mContext)
                .setMessage(getResources().getString(R.string.title_super_power))
//                .setMessage(getResources().getString(R.string.message_low_power))
                .setNegativeButton(getResources().getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mDialog.dismiss();
                                    }
                                })
                .setPositiveButton(getResources().getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switchPowerSaveMode(getCurrentPowerSaveMode(), MODE_LOW_POWER);
                            }
                        }).setCancelable(false).create();
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mDialog.show();
    }

    private void showBatteryEnough() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        mDialog = new AlertDialog.Builder(mContext)
                .setMessage(getResources().getString(R.string.title_power_enough))
//                .setMessage(getResources().getString(R.string.message_power_enough))
                .setPositiveButton(getResources().getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switchPowerSaveMode(MODE_LOW_POWER, MODE_NORMAL_POWER);
                    }
                }).setCancelable(false).create();
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mDialog.show();
    }
}