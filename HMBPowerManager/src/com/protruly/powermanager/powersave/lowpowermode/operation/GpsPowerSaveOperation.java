package com.protruly.powermanager.powersave.lowpowermode.operation;

import android.content.Context;
import android.provider.Settings;

/**
 * Gps PowerSave Operation.
 *
 * LOCATION_MODE:
 * public static final int LOCATION_MODE_OFF = 0;
 * public static final int LOCATION_MODE_SENSORS_ONLY = 1;
 * public static final int LOCATION_MODE_BATTERY_SAVING = 2;
 * public static final int LOCATION_MODE_HIGH_ACCURACY = 3;
 *
 */
public class GpsPowerSaveOperation implements PowerSaveOperation {
    private int mOriginalState;

    @Override
    public void init(int state) {
        mOriginalState = state;
    }

    @Override
    public int enabled(Context context) {
        mOriginalState = getCurrentGpsState(context);
        if (Settings.Secure.LOCATION_MODE_OFF != mOriginalState) {
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
        }
        return mOriginalState;
    }

    @Override
    public void disabled(Context context) {
        int currentGpsState = getCurrentGpsState(context);
        if (currentGpsState == Settings.Secure.LOCATION_MODE_OFF
                && mOriginalState != currentGpsState) {
            int gpsMode = (mOriginalState - currentGpsState) > 0 ? mOriginalState
                    : currentGpsState;
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.LOCATION_MODE, gpsMode);
        }
    }

    private int getCurrentGpsState(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
    }
}