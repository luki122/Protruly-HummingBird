package com.mediatek.settings.ext;

import android.content.Context;
import android.widget.HbSpinner;

public interface IWifiApDialogExt {
    /**
     * set adapter for wifi access point security spinner
     * @param context The parent context
     * @internal
     */
    void setAdapter(Context context, HbSpinner spinner, int arrayId);
}
