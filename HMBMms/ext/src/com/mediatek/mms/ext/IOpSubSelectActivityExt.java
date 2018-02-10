package com.mediatek.mms.ext;

import android.app.Activity;
import android.view.View;

public interface IOpSubSelectActivityExt {
    /**
     * @internal
     */
    void onCreate(Activity hostActivity);
    /**
     * @internal
     */
    boolean onListItemClick(Activity hostActivity, final int subId);
    /**
     * @internal
     */
    String [] setSaveLocation();
    boolean isSimSupported(int subId);
    View getView(String preferenceKey, View view);
}
