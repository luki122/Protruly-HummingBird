package com.mediatek.mms.ext;

import android.app.Activity;
import android.os.SystemProperties;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

import com.mediatek.telephony.TelephonyManagerEx;


public class DefaultOpSubSelectActivityExt implements IOpSubSelectActivityExt {
    public static final boolean MTK_C2K_SUPPORT = SystemProperties.get("ro.mtk_c2k_support")
            .equals("1");
    private static final boolean MTK_C2K6M_OM_SUPPORT = SystemProperties.get("ro.mtk.c2k.om.mode")
            .equals("cllwtg");
    private static final String TAG = "Mms/DOSSActivityExt";

    @Override
    public void onCreate(Activity hostActivity) {

    }

    public boolean onListItemClick(Activity hostActivity, final int subId) {
        return false;
    }

    @Override
    public String [] setSaveLocation() {
        return null;
    }

    @Override
    public boolean isSimSupported(int subId) {
        Log.d(TAG, "[isSimSupported]: c2k:" + MTK_C2K_SUPPORT
                + ", c2k6m: " + MTK_C2K6M_OM_SUPPORT
                + ", subId = " + subId);

        if (MTK_C2K_SUPPORT && isUSimType(subId)) {
            if (MTK_C2K6M_OM_SUPPORT) {
                if (!isCSIMInGsmMode(subId)) {
                    Log.d(TAG, "[isSimSupported]: c2k6m false");
                    return false;
                }
            } else {
                Log.d(TAG, "[isSimSupported]: not 6m false");
                return false;
            }
        }

        Log.d(TAG, "[isSimSupported]: true");
        return true;
    }

    private boolean isUSimType(int subId) {
        String phoneType = TelephonyManagerEx.getDefault().getIccCardType(subId);
        if (phoneType == null) {
            Log.d(TAG, "[isUIMType]: phoneType = null");
            return false;
        }
        Log.d(TAG, "[isUIMType]: phoneType = " + phoneType);
        return phoneType.equalsIgnoreCase("CSIM") || phoneType.equalsIgnoreCase("UIM")
            || phoneType.equalsIgnoreCase("RUIM");
    }

    private boolean isCSIMInGsmMode(int subId) {
        if (isUSimType(subId)) {
            TelephonyManagerEx tmEx = TelephonyManagerEx.getDefault();
            int vnt = tmEx.getPhoneType(SubscriptionManager.getSlotId(subId));
            Log.d(TAG,
                "[isCSIMInGsmMode]:[NO_PHONE = 0;" +
                "GSM_PHONE = 1; CDMA_PHONE = 2;]; phoneType:"
                    + vnt);
            if (vnt == TelephonyManager.PHONE_TYPE_GSM) {
                return true;
            }
        }
        return false;
    }

    @Override
    public View  getView(String preferenceKey, View view) {
        return null;
    }
}
