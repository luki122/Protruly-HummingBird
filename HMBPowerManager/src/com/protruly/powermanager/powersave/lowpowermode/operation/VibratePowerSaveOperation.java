package com.protruly.powermanager.powersave.lowpowermode.operation;


import android.content.Context;
import android.os.Vibrator;
import android.provider.Settings;

public class VibratePowerSaveOperation implements PowerSaveOperation {

    private static final int VIBRATE_ON = 1;
    private static final int VIBRATE_OFF = 0;

    private int mOriginalState;

    @Override
    public void init(int state) {
        mOriginalState = state;
    }

    @Override
    public int enabled(Context context) {
        if (hasHaptic(context)) {
            mOriginalState = getCurrentVibrateState(context);
            if (mOriginalState == VIBRATE_ON) {
                setVibrateState(context, VIBRATE_OFF);
            }
        }

        return mOriginalState;
    }

    @Override
    public void disabled(Context context) {
        if (hasHaptic(context)) {
            int currentVibrateState = getCurrentVibrateState(context);
            if (mOriginalState != currentVibrateState) {
                setVibrateState(context, currentVibrateState | mOriginalState);
            }
        }
    }

    private static boolean hasHaptic(Context context) {
        final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        return vibrator != null && vibrator.hasVibrator();
    }

    private int getCurrentVibrateState(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, VIBRATE_ON);
    }

    private void setVibrateState(Context context, int setValue) {
        Settings.System.putInt(context.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                setValue);
    }
}