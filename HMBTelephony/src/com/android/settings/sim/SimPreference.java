package com.android.settings.sim;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;

import com.mediatek.settings.sim.RadioPowerPreference;
import com.mediatek.settings.sim.TelephonyUtils;

import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;

import com.android.phone.R;
import com.mediatek.internal.telephony.ITelephonyEx;
import android.os.ServiceManager;

 public class SimPreference extends RadioPowerPreference{
	    private static final String TAG = "SimPreference";
        private SubscriptionInfo mSubInfoRecord;
        private int mSlotId;
        Context mContext;
        private ITelephonyEx mTelephonyEx;
        private static final int MODE_PHONE1_ONLY = 1;
        
        public SimPreference(Context context) {
            super(context);
            mContext = context;     
            mTelephonyEx = ITelephonyEx.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        }
        
        public SimPreference(Context context, AttributeSet attrs) {
        	super(context, attrs);
            mContext = context;     
            mTelephonyEx = ITelephonyEx.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        }

        public SimPreference(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            mContext = context;     
            mTelephonyEx = ITelephonyEx.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
        }

        public SimPreference(Context context, SubscriptionInfo subInfoRecord, int slotId) {
            super(context);

            mContext = context;
            mTelephonyEx = ITelephonyEx.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE_EX));
            mSubInfoRecord = subInfoRecord;
            mSlotId = slotId;
//            setKey("sim" + mSlotId);
            update();
        }
        
        public void init( SubscriptionInfo subInfoRecord, int slotId) {
            mSubInfoRecord = subInfoRecord;
            mSlotId = slotId;
            update();
        }
        
        // Returns the line1Number. Line1number should always be read from TelephonyManager since it can
        // be overridden for display purposes.
        private String getPhoneNumber(SubscriptionInfo info) {
            final TelephonyManager tm =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            return tm.getLine1NumberForSubscriber(info.getSubscriptionId());
        }
        
        /**
         * whether radio switch finish on subId.
         */
        private boolean isRadioSwitchComplete(int subId) {
            boolean isComplete = true;
            int slotId = SubscriptionManager.getSlotId(subId);
            if (SubscriptionManager.isValidSlotId(slotId)) {
                Bundle bundle = null;
                try {
                    if (mTelephonyEx != null) {
                        bundle = mTelephonyEx.getServiceState(subId);
                    } else {
                        Log.d(TAG, "mTelephonyEx is null, returen false");
                    }
                } catch (RemoteException e) {
                    isComplete = false;
                    Log.d(TAG, "getServiceState() error, subId: " + subId);
                    e.printStackTrace();
                }
                if (bundle != null) {
                    ServiceState serviceState = ServiceState.newFromBundle(bundle);
                    isComplete = isRadioSwitchComplete(subId, serviceState);
                }
            }
            Log.d(TAG, "isRadioSwitchComplete(" + subId + ")" + ", slotId: " + slotId
                    + ", isComplete: " + isComplete);
            return isComplete;
        }
        
        
        /**
         * whether radio switch finish on subId, according to the service state.
         */
        private boolean isRadioSwitchComplete(final int subId, ServiceState state) {
            if (this == null) {
                Log.d(TAG, "isRadioSwitchComplete()... activity is null");
                return false;
            }
            int slotId = SubscriptionManager.getSlotId(subId);
            int currentSimMode = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.MSIM_MODE_SETTING, -1);
            boolean expectedRadioOn = (currentSimMode & (MODE_PHONE1_ONLY << slotId)) != 0;
            Log.d(TAG, "soltId: " + slotId + ", expectedRadioOn : " + expectedRadioOn);
            if (expectedRadioOn && (state.getState() != ServiceState.STATE_POWER_OFF)) {
                return true;
            } else if (state.getState() == ServiceState.STATE_POWER_OFF) {
                return true;
            }
            return false;
        }


        public void update() {
            boolean mIsAirplaneModeOn = TelephonyUtils.isAirplaneModeOn(mContext);
            final Resources res = mContext.getResources();

            setTitle(mContext.getResources()
                    .getString(R.string.enable_sim));

            /// M: for Plug-in
//            customizePreferenceTitle();
            Log.d(TAG, "update()... mSubInfoRecord = " + mSubInfoRecord);
            if (mSubInfoRecord != null) {
                 setEnabled(true);                
                /// M: add for radio on/off @{
                setRadioEnabled(!mIsAirplaneModeOn
                        && isRadioSwitchComplete(mSubInfoRecord.getSubscriptionId()));
                setRadioOn(TelephonyUtils.isRadioOn(mSubInfoRecord.getSubscriptionId(),
                        getContext()));
                /// @}
            } else {
                setFragment(null);
                setEnabled(false);
            }
        }

        private int getSlotId() {
            return mSlotId;
        }

        /**
         * only for plug-in, change "SIM" to "UIM/SIM".
         */
//        private void customizePreferenceTitle() {
//            int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
//            if (mSubInfoRecord != null) {
//                subId = mSubInfoRecord.getSubscriptionId();
//            }
//            setTitle(String.format(mMiscExt.customizeSimDisplayString(mContext.getResources()
//                    .getString(R.string.sim_editor_title), subId), (mSlotId + 1)));
//        }
    }