package com.protruly.powermanager.powersave.lowpowermode.operation;

import android.content.Context;
import android.os.IPowerManager;
import android.os.ServiceManager;
import android.provider.Settings;

import com.protruly.powermanager.utils.LogUtils;

/**
 * Screen brightness power save operation.
 */
public class ScreenBrightnessPowerSaveOperation implements PowerSaveOperation {
    private static final String TAG = "ScreenBrightness";

    private static final float BRIGHTNESS_ADJ = 0.2f;

    private int mOriginalState;
    private final IPowerManager mPower;

    public ScreenBrightnessPowerSaveOperation() {
        mPower = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
    }

    @Override
    public void init(int state) {
        mOriginalState = state;
    }

    @Override
    public int enabled(Context context) {
        mOriginalState = getCurrentScreenBrightnessMode(context);
        if (Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC != mOriginalState) {
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        } else {
            setAutomaticBrightness(context);
        }

        return mOriginalState;
    }

    @Override
    public void disabled(Context context) {
        int currentScreenBrightnessMode = getCurrentScreenBrightnessMode(context);
        if (mOriginalState != currentScreenBrightnessMode
                && currentScreenBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        } else {
            restoreAutomaticBrightness(context);
        }
    }

    private void setAutomaticBrightness(Context context) {
        try {
            float adj = Settings.System.getFloat(context.getContentResolver(),
                    Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ);
            LogUtils.d(TAG, "setAutomaticBrightness() -> AUTO_BRIGHTNESS_ADJ = " + adj);
            mPower.setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(adj - BRIGHTNESS_ADJ);
            Settings.System.putFloat(context.getContentResolver(),
                    Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, adj - BRIGHTNESS_ADJ);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void restoreAutomaticBrightness(Context context) {
        try {
            float adj = Settings.System.getFloat(context.getContentResolver(),
                    Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ);
            LogUtils.d(TAG, "restoreAutomaticBrightness() -> AUTO_BRIGHTNESS_ADJ = " + adj);
            mPower.setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(adj + BRIGHTNESS_ADJ);
            Settings.System.putFloat(context.getContentResolver(),
                    Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, adj + BRIGHTNESS_ADJ);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getCurrentScreenBrightnessMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }
}