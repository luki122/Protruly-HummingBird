package com.protruly.powermanager.powersave.lowpowermode;

import android.content.Context;

import com.protruly.powermanager.powersave.lowpowermode.operation.VibratePowerSaveOperation;
import com.protruly.powermanager.utils.PowerSPUtils;
import com.protruly.powermanager.powersave.lowpowermode.operation.BluetoothPowerSaveOperation;
import com.protruly.powermanager.powersave.lowpowermode.operation.GpsPowerSaveOperation;
import com.protruly.powermanager.powersave.lowpowermode.operation.PowerSaveOperation;
import com.protruly.powermanager.powersave.lowpowermode.operation.RotationPowerSaveOperation;
import com.protruly.powermanager.powersave.lowpowermode.operation.ScreenBrightnessPowerSaveOperation;
import com.protruly.powermanager.powersave.lowpowermode.operation.ScreenOffPowerSaveOperation;
import com.protruly.powermanager.powersave.lowpowermode.operation.WifiHotspotPowerSaveOperation;
import com.protruly.powermanager.utils.LogUtils;
import com.protruly.powermanager.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * The LowPowerSaveManager is responsible for executing low power save operations.
 */
public class LowPowerSaveManager {
    private static final String TAG = LowPowerSaveManager.class.getSimpleName();

    private Context mContext;
    private static LowPowerSaveManager sInstance;

    private PowerSPUtils mSP;
    private String mOriginalStateString;
    private final List<Integer> mOriginalState = new ArrayList<>();
    private List<PowerSaveOperation> mPowerSaveOperationList = new ArrayList<>();

    public static LowPowerSaveManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LowPowerSaveManager(context);
        }
        return sInstance;
    }

    private LowPowerSaveManager(Context context) {
        LogUtils.d(TAG, "LowPowerSaveManager()");

        mContext = context;
        mPowerSaveOperationList.clear();
        mSP = PowerSPUtils.instance(mContext.getApplicationContext());

        // Gps Operation
        GpsPowerSaveOperation gpsOperation = new GpsPowerSaveOperation();
        mPowerSaveOperationList.add(gpsOperation);

        // Bluetooth Operation
        BluetoothPowerSaveOperation bluetoothOperation = new BluetoothPowerSaveOperation();
        mPowerSaveOperationList.add(bluetoothOperation);

        // WifiHotspot Operation
        WifiHotspotPowerSaveOperation wifiPowerSaveCommand = new WifiHotspotPowerSaveOperation();
        mPowerSaveOperationList.add(wifiPowerSaveCommand);

        // ScreenOffTimeOut Operation
        ScreenOffPowerSaveOperation screenOffOperation = new ScreenOffPowerSaveOperation();
        mPowerSaveOperationList.add(screenOffOperation);

        // Rotation Operation
        RotationPowerSaveOperation rotationOperation = new RotationPowerSaveOperation();
        mPowerSaveOperationList.add(rotationOperation);

        // ScreenBrightness Operation
        ScreenBrightnessPowerSaveOperation screenBrightnessOperation
                = new ScreenBrightnessPowerSaveOperation();
        mPowerSaveOperationList.add(screenBrightnessOperation);

        // Vibrate Operation
        VibratePowerSaveOperation vibratePowerSaveOperation = new VibratePowerSaveOperation();
        mPowerSaveOperationList.add(vibratePowerSaveOperation);

        /*
        // AnimationScale Operation
        AnimationScalePowerSaveOperation AnimationScaleOperation
                = new AnimationScalePowerSaveOperation();
        mPowerSaveOperationList.add(AnimationScaleOperation);
        */

        restoreOriginalState();
    }

    boolean setLowPowerSaveMode(boolean enable) {
        LogUtils.d(TAG, "setLowPowerSaveMode() -> enable = " + enable);
        if (enable) {
            mOriginalState.clear();
            for (int i = 0; i < mPowerSaveOperationList.size(); i++) {
                int pre = mPowerSaveOperationList.get(i).enabled(mContext);
                mOriginalState.add(pre);
            }
            saveOriginalState();
        } else {
            for (PowerSaveOperation operation : mPowerSaveOperationList) {
                operation.disabled(mContext);
            }
        }

        return true;
    }

    private static final String KEY_LOW_POWER_SAVE_ORIGINAL_STATE = "low_power_state";

    /**
     * Store original state to shared preferences.
     */
    private void saveOriginalState() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mOriginalState.size(); i++) {
            builder.append(Integer.toString(mOriginalState.get(i)));
            builder.append(":");
        }
        String updateStateString = builder.toString();
        LogUtils.d(TAG, "saveOriginalState() -> mOriginalStateString = " + mOriginalStateString
                + ", updateStateString = " + updateStateString);
        if (!updateStateString.equals(mOriginalStateString)) {
            mSP.setStringValue(KEY_LOW_POWER_SAVE_ORIGINAL_STATE, updateStateString);
            mOriginalStateString = updateStateString;
        }
    }

    /**
     * Restore original state from shared preferences.
     */
    private void restoreOriginalState() {
        mOriginalStateString = mSP.getStringValue(KEY_LOW_POWER_SAVE_ORIGINAL_STATE, "");
        LogUtils.d(TAG, "restoreOriginalState() -> mOriginalStateString = " + mOriginalStateString);
        Utils.mStringColonSplitter.setString(mOriginalStateString);
        while (Utils.mStringColonSplitter.hasNext()) {
            final String st = Utils.mStringColonSplitter.next();
            if (st != null) {
                int val = 0;
                try {
                    val = Integer.parseInt(st);
                } catch (Exception e) {
                    LogUtils.e(TAG, "parse int error! " + e);
                }
                mOriginalState.add(val);
            }
        }

        int N = Math.min(mOriginalState.size(), mPowerSaveOperationList.size());
        for (int i = 0; i < N; i++) {
            PowerSaveOperation op = mPowerSaveOperationList.get(i);
            op.init(mOriginalState.get(i));
        }
    }
}