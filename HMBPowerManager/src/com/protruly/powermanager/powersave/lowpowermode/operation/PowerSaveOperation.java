package com.protruly.powermanager.powersave.lowpowermode.operation;

import android.content.Context;

/**
 * Power Save operation.
 */
public interface PowerSaveOperation {
    /**
     * Initialize original state.
     * @hide*/
    public void init(int state);

    /**
     * Enable power save operation.
     * @hide*/
    public int enabled(Context context);

    /**
     * Disable power save operation.
     * @hide*/
    public void disabled(Context context);
}