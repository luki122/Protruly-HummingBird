package com.protruly.powermanager.powersave.lowpowermode.operation;

import android.content.Context;
import android.net.wifi.WifiManager;

/**
 * Wifi Hotspot Power Save Operation.
 */
public class WifiHotspotPowerSaveOperation implements PowerSaveOperation {
    private static final int TETHERING_OFF = 0;
    private static final int TETHERING_ON = 1;

    private WifiManager mWifiManager;
    private boolean mTetheringOriginalState;

    @Override
    public void init(int pre) {
        mTetheringOriginalState = pre == TETHERING_ON;
    }

    @Override
    public int enabled(Context context) {
        mWifiManager = getWifiManager(context);
        if (mWifiManager != null) {
            int tetherState = mWifiManager.getWifiApState();
            if (WifiManager.WIFI_AP_STATE_ENABLING == tetherState
                    || WifiManager.WIFI_AP_STATE_ENABLED == tetherState) {
                mWifiManager.setWifiApEnabled(null, false);
            }
            mTetheringOriginalState = tetherState == WifiManager.WIFI_AP_STATE_ENABLED;
        }
        return mTetheringOriginalState ? TETHERING_ON : TETHERING_OFF;
    }

    @Override
    public void disabled(Context context) {
        mWifiManager = getWifiManager(context);
        if (mWifiManager != null) {
            boolean currentTetheringState =
                    mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED;
            if (!currentTetheringState && mTetheringOriginalState) {
                mWifiManager.setWifiApEnabled(null, true);
            }
        }
    }

    private WifiManager getWifiManager(Context context) {
        if (mWifiManager == null) {
            mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        }
        return mWifiManager;
    }
}