package com.protruly.powermanager.powersave.lowpowermode.operation;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

/**
 * Mobile Data Operation.
 */
public class MobileDataPowerSaveOperation implements PowerSaveOperation {
    private int mOriginalState;
    private TelephonyManager mTelephonyManager;

    @Override
    public void init(int state) {
        mOriginalState = state;
    }

    @Override
    public int enabled(Context context) {
        mTelephonyManager = getTelephonyManager(context);
        if (mTelephonyManager != null) {
            setMobileDataEnabled(context, false);
        }

        return mOriginalState;
    }

    @Override
    public void disabled(Context context) {
        mTelephonyManager = getTelephonyManager(context);
        if (mTelephonyManager != null) {
            setMobileDataEnabled(context, true);
        }
    }

    private void setMobileDataEnabled(Context context, boolean enabled) {
        mTelephonyManager.setDataEnabled(enabled);
        /// M: enable the default data SIM and disable another if need.
        if (enabled) {
            int[] subList = SubscriptionManager.from(context).getActiveSubscriptionIdList();
            int dataSubId = SubscriptionManager.getDefaultDataSubId();
            for (int subId : subList) {
                if (subId != dataSubId && mTelephonyManager.getDataEnabled(subId)) {
                    mTelephonyManager.setDataEnabled(subId, false);
                }
            }
        }
    }

    private TelephonyManager getTelephonyManager(Context context) {
        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        }
        return mTelephonyManager;
    }
}