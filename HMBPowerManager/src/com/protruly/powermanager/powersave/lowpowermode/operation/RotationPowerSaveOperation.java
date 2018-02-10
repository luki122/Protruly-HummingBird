package com.protruly.powermanager.powersave.lowpowermode.operation;

import android.content.Context;
import android.provider.Settings;

/**
 * Rotation Power Save Operation.
 */
public class RotationPowerSaveOperation implements PowerSaveOperation {

    private int mOriginalState;

    @Override
    public void init(int state) {
        mOriginalState = state;
    }

    @Override
    public int enabled(Context context) {
        mOriginalState = getCurrentRotationLockedState(context);
        if (0 != mOriginalState) {
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION, 0);
        }
        return mOriginalState;
    }

    @Override
    public void disabled(Context context) {
        int currentRatationState = getCurrentRotationLockedState(context);
        if (currentRatationState == 0 && mOriginalState != currentRatationState) {
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION, 1);
        }
    }

    /**
     * Get current rotation lock state.
     * @param context
     * @return If 0, it will not be used unless explicitly requested
     * by the application; if 1, it will be used by default unless explicitly
     * disabled by the application.
     */
    private int getCurrentRotationLockedState(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0);
    }
}