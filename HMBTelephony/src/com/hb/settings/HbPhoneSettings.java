package com.hb.settings;

import android.app.ActionBar;
import android.app.Activity;
import hb.app.dialog.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncResult;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.CdmaCallOptions;
import com.android.phone.GsmUmtsAdditionalCallOptions;
import com.android.phone.GsmUmtsCallForwardOptions;
import com.android.phone.PhoneGlobals;
import com.android.phone.SubscriptionInfoHelper;

import android.os.Build;
import android.telephony.SubscriptionManager;
import hb.preference.*;
import android.provider.Settings;
import com.android.phone.R;
import com.mediatek.settings.cdma.CdmaCallForwardOptions;
import com.mediatek.settings.cdma.CdmaCallWaitSetting;
import com.android.internal.telephony.TelephonyIntents;

public class HbPhoneSettings extends PreferenceActivity  {
	private static final String LOG_TAG = "HbPhoneSettings";
	private static final boolean DBG = true;

	// Information about logical "up" Activity
	private static final String UP_ACTIVITY_PACKAGE = "com.android.dialer";
	private static final String UP_ACTIVITY_CLASS = "com.android.dialer.DialtactsActivity";

	private Phone mPhone;
	private boolean mForeground;

	private Preference[] mCallForward, mCallWaiting, mSimCategory;
	private Preference mStk ;
    private SharedPreferences mSp;

	private int mNumPhones;
	private TelephonyManager mTelephonyManager;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
        log("onCreate(). Intent: " + getIntent());

		mPhone = PhoneGlobals.getPhone();

		addPreferencesFromResource(R.xml.hb_phone_feature_setting);

		PreferenceScreen prefSet = getPreferenceScreen();

		mTelephonyManager = TelephonyManager.from(this);
		mNumPhones = mTelephonyManager.getPhoneCount();
		mCallForward = new Preference[mNumPhones];
		mCallWaiting = new Preference[mNumPhones];
		mSimCategory = new Preference[mNumPhones];

		for (int i = 0; i < mNumPhones; i++) {
			log("init preference  i =" + i);
			mSimCategory[i] = findPreference("sim_category_key" + i);
			
			mCallForward[i] = findPreference("button_cf_expand_key" + i);	
			int subid = PhoneFactory.getPhone(i).getSubId();
		     mCallForward[i].getIntent().putExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, subid);
			int phonetype = PhoneFactory.getPhone(i).getPhoneType();
			log("init preference  PhoneFactory.getPhone("+ i +") =" + PhoneFactory.getPhone(i) );

			Class<?> callforwardClass = phonetype == PhoneConstants.PHONE_TYPE_CDMA ? CdmaCallForwardOptions.class
					: GsmUmtsCallForwardOptions.class;
			mCallForward[i].getIntent().setClass(this, callforwardClass);

			mCallWaiting[i] = findPreference("button_more_expand_key" + i);
			mCallWaiting[i].getIntent().putExtra(SubscriptionInfoHelper.SUB_ID_EXTRA, subid);
			Class<?> callWaitingClass = phonetype == PhoneConstants.PHONE_TYPE_CDMA ? CdmaCallWaitSetting.class
					: GsmUmtsAdditionalCallOptions.class;
			mCallWaiting[i].getIntent().setClass(this, callWaitingClass);
		}
		
		mStk = findPreference("stk");

        IntentFilter intentFilter =
                new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
	}
	
    @Override
    public void onDestroy() {
    	unregisterReceiver(mReceiver);
        super.onDestroy();
    }

	@Override
	protected void onResume() {
		super.onResume();
		mForeground = true;

		updateUiState();
	}
	
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mStk){
            Intent stkIntent = new Intent("android.intent.action.MAIN");
            stkIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            stkIntent.setClassName("com.android.stk","com.android.stk.StkMain");
            startActivity(stkIntent);
            return true;
        }
        return false;
    }

	private static void log(String msg) {
		Log.d(LOG_TAG, msg);
	}
	
	 private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		 @Override
	        public void onReceive(Context context, Intent intent) {
	            String action = intent.getAction();
	            if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
					updateUiState();
	            }
		 }
	 };

	private void updateUiState() {
		log("updateUiState");
        boolean isStkEnable = false;
		for (int i = 0; i < mNumPhones; i++) {
			boolean isCardEnable = mTelephonyManager.getSimState(i) != TelephonyManager.SIM_STATE_ABSENT;
			isStkEnable |= isCardEnable;
			mSimCategory[i].setEnabled(isCardEnable);
		}
		mStk.setEnabled(isStkEnable);
	}	
	

}