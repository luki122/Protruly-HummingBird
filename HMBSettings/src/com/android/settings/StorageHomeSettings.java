package com.android.settings;

import android.os.Bundle;

import com.android.internal.logging.MetricsLogger;

/**
 * Created by liuqin on 17-3-31.
 *
 */
public class StorageHomeSettings extends SettingsPreferenceFragment {

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEVICEINFO;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.storage_home_settings);
    }
}
