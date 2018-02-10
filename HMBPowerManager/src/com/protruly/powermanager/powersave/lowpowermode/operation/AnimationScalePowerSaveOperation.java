
package com.protruly.powermanager.powersave.lowpowermode.operation;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.view.IWindowManager;

/**
 * Animation Scale Power Save Operation.
 *
 * Window animation scale,
 * Transition animation scale,
 * Animator duration scale.
 */
public class AnimationScalePowerSaveOperation implements PowerSaveOperation {
    private static final int INDEX_WINDOW_ANIMATION_SCALE = 0;
    private static final int INDEX_TRANSITION_ANIMATION_SCALE = 1;
    private static final int INDEX_ANIMATOR_DURATION_SCALE = 2;

    private IWindowManager mWindowManager;

    @Override
    public void init(int state) {
    }

    @Override
    public int enabled(Context context) {
        mWindowManager = getWindowManager();
        if (mWindowManager != null) {
            writeAnimationScaleOption(INDEX_WINDOW_ANIMATION_SCALE, 0);
            writeAnimationScaleOption(INDEX_TRANSITION_ANIMATION_SCALE, 0);
            writeAnimationScaleOption(INDEX_ANIMATOR_DURATION_SCALE, 0);
        }
        return 0;
    }

    @Override
    public void disabled(Context context) {
        mWindowManager = getWindowManager();
        if (mWindowManager != null) {
            writeAnimationScaleOption(INDEX_WINDOW_ANIMATION_SCALE, 1);
            writeAnimationScaleOption(INDEX_TRANSITION_ANIMATION_SCALE, 1);
            writeAnimationScaleOption(INDEX_ANIMATOR_DURATION_SCALE, 1);
        }
    }

    private void writeAnimationScaleOption(int which, float newValue) {
        try {
            float scale = newValue;
            mWindowManager.setAnimationScale(which, scale);
        } catch (RemoteException e) {
        }
    }

    private IWindowManager getWindowManager() {
        if (mWindowManager == null) {
            mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        }
        return mWindowManager;
    }
}