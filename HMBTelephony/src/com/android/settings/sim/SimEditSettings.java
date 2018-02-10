/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.sim;

import com.android.ims.ImsManager;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;

import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.phone.ext.IMobileNetworkSettingsExt;
import com.mediatek.settings.Enhanced4GLteSwitchPreference;
import com.mediatek.settings.MobileNetworkSettingsOmEx;
import com.mediatek.settings.TelephonyUtils;
import com.mediatek.settings.cdma.CdmaNetworkSettings;
import com.mediatek.settings.cdma.TelephonyUtilsEx;
import com.mediatek.settings.sim.PhoneServiceStateHandler;
import com.mediatek.settings.sim.RadioPowerPreference;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.ActionBar;
import hb.app.dialog.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import hb.preference.ListPreference;
import hb.preference.Preference;
import hb.preference.PreferenceActivity;
import hb.preference.PreferenceGroup;
import hb.preference.PreferenceScreen;
import hb.preference.SwitchPreference;
import hb.preference.EditTextPreference;
import hb.preference.PreferenceCategory;
import android.telephony.CarrierConfigManager;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.RadioAccessFamily;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.android.settings.sim.SimPreference;
import com.android.phone.*;

import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.widget.Toast;

/**
 * "Mobile network settings" screen.  This preference screen lets you
 * enable/disable mobile data, and control data roaming and other
 * network-specific mobile data features.  It's used on non-voice-capable
 * tablets as well as regular phone devices.
 *
 * Note that this PreferenceActivity is part of the phone app, even though
 * you reach it from the "Wireless & Networks" section of the main
 * Settings app.  It's not part of the "Call settings" hierarchy that's
 * available from the Phone app (see CallFeaturesSetting for that.)
 */
public class SimEditSettings extends PreferenceActivity implements   PhoneServiceStateHandler.Listener {

    // debug data
    private static final String LOG_TAG = "SimEditSettings";
    private static final boolean DBG = true;
 
    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;
    private Phone mPhone;
    
    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
 
    @Override
    protected void onCreate(Bundle icicle) {
        if (DBG) log("onCreate:+");

        super.onCreate(icicle);
   
        mSubscriptionManager = SubscriptionManager.from(this);
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        
        mSlotId =  getIntent().getIntExtra(SimSettings.EXTRA_SLOT_ID, -1);
        mSubInfoRecord = mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(mSlotId);
        mPhone = PhoneFactory.getPhone(mSlotId);

        addPreferencesFromResource(R.xml.hb_network_setting);

        //get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();

        if (DBG) log("onCreate:-");              
        
        initPreference();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mStateHandler.unregisterOnPhoneServiceStateChange();
    }

    //add by lgy 
    private static final int MODE_PHONE1_ONLY = 1;
    private int mSlotId = -1;
    private SimPreference mSim;
    private EditTextPreference mEditName, mEditNumber;
    private PhoneServiceStateHandler mStateHandler;
    
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		// TODO Auto-generated method stub
		super.onPreferenceTreeClick(preferenceScreen, preference);
		// 判断是否是EditTextPreference
		if(preference == mEditNumber) {
			EditText ed = ((EditTextPreference) preference).getEditText();
			ed.setInputType(EditorInfo.TYPE_CLASS_PHONE);
		}
		if (preference instanceof EditTextPreference) {
			EditText ed = ((EditTextPreference) preference).getEditText();
			Editable etable = ed.getText();
			Selection.setSelection(etable, etable.length());// 光标置位
		}
		return false;
	}
    
    private void initPreference() {
    	
    	mSim = (SimPreference)findPreference("sim_switch");
		mSim.init(mSubInfoRecord, mSlotId);
		mSim.bindRadioPowerState(mSubInfoRecord == null ? SubscriptionManager.INVALID_SUBSCRIPTION_ID
                : mSubInfoRecord.getSubscriptionId());
    	mSim.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference,
					Object objValue) {
				if (preference == mSim) {
					boolean isChecked =  (Boolean)objValue;
					mSim.doWork(isChecked);
					maybeSwitchSlot(isChecked);
					return true;
				}
				return false;
			}
		});    	
    	
    	mEditName = (EditTextPreference) findPreference("sim_name");
    	mEditName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference,
					Object objValue) {
				if (preference == mEditName) {
					String displayName = (String) objValue;
					if(isNeedReturn(mSubInfoRecord.getDisplayName().toString(), displayName)) {
						return false;
					}
					int subId = mSubInfoRecord.getSubscriptionId();
					mSubInfoRecord.setDisplayName(displayName);
					mSubscriptionManager.setDisplayName(displayName, subId,
							SubscriptionManager.NAME_SOURCE_USER_INPUT);
					mEditName.setSummary(displayName);
			    	setTitle(displayName + (mSlotId + 1));
					return true;
				}
				return false;
			}
		});

    	mEditNumber = (EditTextPreference) findPreference("sim_number");
		mEditNumber.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference,
					Object objValue) {
				if (preference == mEditNumber) {
					String displayNumber = (String) objValue;
					if(isNeedReturn(mSubInfoRecord.getNumber(), displayNumber)) {
						return false;
					}
					int subId = mSubInfoRecord.getSubscriptionId();
					mSubInfoRecord.setNumber(displayNumber);
					mSubscriptionManager.setDisplayNumber(displayNumber, subId);
					mEditNumber.setSummary(displayNumber);
					return true;
				}
				return false;
			}
		});
		
		updateSimInfo();
		
        mStateHandler = new PhoneServiceStateHandler(this.getApplicationContext());
        mStateHandler.registerOnPhoneServiceStateChange(this);
    }
    
    private SubscriptionInfo mSubInfoRecord;
    private void updateSimInfo() {
    	
    	String def =  getString(R.string.unknown) ;
    	
    	  if(TextUtils.isEmpty(mSubInfoRecord.getNumber())) {
              final String rawNumber =  mTelephonyManager.getLine1NumberForSubscriber(
                      mSubInfoRecord.getSubscriptionId());
              if (TextUtils.isEmpty(rawNumber)) {
            	  mEditNumber.setText(def);
              } else {
            	  mEditNumber.setText(PhoneNumberUtils.formatNumber(rawNumber));
              }
    	  } else {
    		  mEditNumber.setText(mSubInfoRecord.getNumber());
    	  }
    	  mEditNumber.setSummary(mEditNumber.getText());

    	  if(TextUtils.isEmpty(mSubInfoRecord.getDisplayName())) {
              String simCarrierName = mTelephonyManager.getSimOperatorNameForSubscription(mSubInfoRecord
                      .getSubscriptionId());
              mEditName.setText(!TextUtils.isEmpty(simCarrierName) ? simCarrierName :
            	  def);
    	  } else {
    		  mEditName.setText(mSubInfoRecord.getDisplayName().toString());
    	  }
    	  mEditName.setSummary(mEditName.getText());
    	  setTitle(mEditName.getText() + (mSlotId + 1));

    }
    

    /**
     * whether radio switch finish on subId, according to the service state.
     */
    private boolean isRadioSwitchComplete(final int subId, ServiceState state) {
        if (this == null) {
            Log.d(LOG_TAG, "isRadioSwitchComplete()... activity is null");
            return false;
        }
        int slotId = SubscriptionManager.getSlotId(subId);
        int currentSimMode = Settings.System.getInt(this.getContentResolver(),
                Settings.System.MSIM_MODE_SETTING, -1);
        boolean expectedRadioOn = (currentSimMode & (MODE_PHONE1_ONLY << slotId)) != 0;
        Log.d(LOG_TAG, "soltId: " + slotId + ", expectedRadioOn : " + expectedRadioOn +  " state.getState() = " + state.getState());
        if (expectedRadioOn && (state.getState() != ServiceState.STATE_POWER_OFF)) {
            return true;
        } else if (state.getState() == ServiceState.STATE_POWER_OFF) {
            return true;
        }
        return false;
    }
    
    @Override
    public void onServiceStateChanged(ServiceState state, int subId) {
        Log.d(LOG_TAG, "PhoneStateListener:onServiceStateChanged: subId: " + subId
                + ", state: " + state);
        if (isRadioSwitchComplete(subId, state)) {
            handleRadioPowerSwitchComplete();
        }
    }
    
    /**
     * update SIM values after radio switch
     */
    private void handleRadioPowerSwitchComplete() {
        Log.d(LOG_TAG, "handleRadioPowerSwitchComplete()... isResumed = " + isResumed());
        if(isResumed()) {
            mSim.update();
        }
    }
    
    private void maybeSwitchSlot(boolean isChecked) {
    	if(!isChecked) {
    		int otherSubId = getOtherSubId();
    		if(otherSubId > SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
    			mSubscriptionManager.setDefaultDataSubId(otherSubId);	
    			mSubscriptionManager.setDefaultSmsSubId(otherSubId);
    			PhoneAccountHandle phoneAccountHandle = subscriptionIdToPhoneAccountHandle(otherSubId);
    		    final TelecomManager telecomManager = TelecomManager.from(PhoneGlobals.getInstance());
                telecomManager.setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
    		}
    	}
    }
    
    private int getOtherSubId() {
    	int otherSlot = mSlotId == 0 ? 1 : 0 ;
    	return SubscriptionManager.getSubIdUsingPhoneId(otherSlot);
    }
    
    private PhoneAccountHandle subscriptionIdToPhoneAccountHandle(final int subId) {
        final TelecomManager telecomManager = TelecomManager.from(this);
        final TelephonyManager telephonyManager = TelephonyManager.from(this);
        final Iterator<PhoneAccountHandle> phoneAccounts =
                telecomManager.getCallCapablePhoneAccounts().listIterator();

        while (phoneAccounts.hasNext()) {
            final PhoneAccountHandle phoneAccountHandle = phoneAccounts.next();
            final PhoneAccount phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle);
            if (subId == telephonyManager.getSubIdForPhoneAccount(phoneAccount)) {
                return phoneAccountHandle;
            }
        }

        return null;
    }
    
    
    private boolean isNeedReturn(String olds, String news) {
    	  if(TextUtils.isEmpty(news)) {
              Toast.makeText(this, R.string.empty_sim, Toast.LENGTH_SHORT).show();
              return true;
    	  }
          if(TextUtils.isEmpty(olds)) {
    		  return false;
    	  } else {
			  return olds.equals(news);
    	  }
    }
    
}
