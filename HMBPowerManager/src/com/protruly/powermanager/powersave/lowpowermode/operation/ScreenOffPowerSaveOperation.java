
package com.protruly.powermanager.powersave.lowpowermode.operation;

import android.content.Context;
import android.provider.Settings;

/**
 * Screen off timeout power save operation.
 */
public class ScreenOffPowerSaveOperation implements PowerSaveOperation {

    private int mOriginalState;
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;
    private static final int POWERSAVE_SCREEN_TIMEOUT_VALUE = 15000;

    @Override
    public void init(int pre) {
        mOriginalState = pre;
    }

    @Override
    public int enabled(Context context) {
        mOriginalState = getCurrentScreenTimeOutState(context);
        Settings.System.putInt(context.getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT, POWERSAVE_SCREEN_TIMEOUT_VALUE);
        return mOriginalState;
    }

    @Override
    public void disabled(Context context) {
        int currentScreenTimeOutState = getCurrentScreenTimeOutState(context);
        if (mOriginalState != currentScreenTimeOutState
                && currentScreenTimeOutState == POWERSAVE_SCREEN_TIMEOUT_VALUE) {
            int screenTimeOutMode = (mOriginalState - currentScreenTimeOutState) > 0 ? mOriginalState
                    : currentScreenTimeOutState;
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT, screenTimeOutMode);
        }
    }

    private int getCurrentScreenTimeOutState(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE);
    }
}