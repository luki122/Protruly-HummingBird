package com.mediatek.settings.sim;

import android.content.Context;
import hb.preference.Preference;
import hb.preference.SwitchPreference;
import android.telephony.SubscriptionManager;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.util.AttributeSet;
import android.util.Log;

import com.android.phone.R;
import com.mediatek.settings.FeatureOption;

/**
 * A preference for radio switch function.
 */
public class RadioPowerPreference extends SwitchPreference {

    private static final String TAG = "RadioPowerPreference";
    private boolean mPowerState;
    private boolean mPowerEnabled = true;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private Switch mRadioSwith = null;
    private RadioPowerController mController;

    /**
     * Construct of RadioPowerPreference.
     * @param context Context.
     */
    public RadioPowerPreference(Context context) {
        super(context);
        mController = RadioPowerController.getInstance(context);
    }
    
    
    public RadioPowerPreference(Context context, AttributeSet attrs) {
    	super(context, attrs);
        mController = RadioPowerController.getInstance(context);
    }

    public RadioPowerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mController = RadioPowerController.getInstance(context);
    }

    /**
     * Set the radio switch state.
     * @param state On/off.
     */
    public void setRadioOn(boolean state) {
        mPowerState = state;
        setChecked(state);       
    }

    /**
     * Set the radio switch enable state.
     * @param enable Enable.
     */
    public void setRadioEnabled(boolean enable) {
        mPowerEnabled = enable;
        setEnabled(enable);
    }

//    @Override
//    protected void onBindView(View view) {
//        super.onBindView(view);
//        mRadioSwith = (Switch) view.findViewById(R.id.radio_state);
//        if (mRadioSwith != null) {
//            if (FeatureOption.MTK_A1_FEATURE) {
//                mRadioSwith.setVisibility(View.GONE);
//            }
//            mRadioSwith.setChecked(mPowerState);
//            mRadioSwith.setEnabled(mPowerEnabled);
//            mRadioSwith.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//                @Override
//                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                    Log.d(TAG, "checked state change, mPowerState = " + mPowerState
//                            + ", isChecked = " + isChecked + ", subId = " + mSubId);
//                    if (mPowerState != isChecked) {
//                        if (mController.setRadionOn(mSubId, isChecked)) {
//                            // disable radio switch to prevent continuous click
//                            setRadioEnabled(false);
//                        } else {
//                            // if set radio fail, revert button status.
//                            Log.w(TAG, "set radio power FAIL!");
//                            setRadioOn(!isChecked);
//                        }
//                    }
//                }
//            });
//        }
//    }
    
    public void doWork(boolean isChecked) {
        Log.d(TAG, "checked state change, mPowerState = " + mPowerState
                + ", isChecked = " + isChecked + ", subId = " + mSubId);
        if (mPowerState != isChecked) {
            if (mController.setRadionOn(mSubId, isChecked)) {
                // disable radio switch to prevent continuous click
                setRadioEnabled(false);
            } else {
                // if set radio fail, revert button status.
                Log.w(TAG, "set radio power FAIL!");
                setRadioOn(!isChecked);
            }
        }    
    }

    @Override
    public void setEnabled(boolean enabled) {
        mPowerEnabled = enabled;     
        Log.d(TAG, "setEnabled = " + enabled);
        super.setEnabled(enabled);
    }

    /**
     * Bind the preference with corresponding property.
     * @param preference {@link RadioPowerPreference}.
     * @param subId subId
     */
    public void bindRadioPowerState(final int subId) {
        mSubId = subId;
        setRadioOn(TelephonyUtils.isRadioOn(subId, getContext()));
        setRadioEnabled(SubscriptionManager.isValidSubscriptionId(subId));
    }
}
