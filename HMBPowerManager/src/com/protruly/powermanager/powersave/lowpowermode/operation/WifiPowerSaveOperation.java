package com.protruly.powermanager.powersave.lowpowermode.operation;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.provider.Settings;

/**
 * Wifi PowerSave Operation.
 */
public class WifiPowerSaveOperation implements PowerSaveOperation {
    private int mOriginalState;

    @Override
    public void init(int state) {
        mOriginalState = state;
    }

    @Override
    public int enabled(Context context) {
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mWifiManager.setWifiEnabled(false);
        return mOriginalState;
    }

    @Override
    public void disabled(Context context) {
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int wifiState = mWifiManager.getWifiApState();
        if (wifiState != WifiManager.WIFI_STATE_ENABLED && wifiState != WifiManager.WIFI_STATE_ENABLING) {
            mWifiManager.setWifiEnabled(true);
        }
    }
}