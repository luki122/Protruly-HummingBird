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

public class HbCallSettings extends PreferenceActivity implements Preference.OnPreferenceChangeListener  {
	private static final String LOG_TAG = "HbCallSettings";
	private static final boolean DBG = true;

    private SwitchPreference mPower, mVibrate, mFlash, mRinger,mOverturn;
    private SharedPreferences mSp;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
        log("onCreate(). Intent: " + getIntent());

		addPreferencesFromResource(R.xml.hb_call_feature_setting);
		PreferenceScreen prefSet = getPreferenceScreen();
	
		initSwtichs();
	}

	private static void log(String msg) {
		Log.d(LOG_TAG, msg);
	}	 
	
	private void initSwtichs() {
		mPower = (SwitchPreference) findPreference("power_hangup_switch");
		mVibrate = (SwitchPreference) findPreference("vibrate_active_switch");
		mFlash = (SwitchPreference) findPreference("flash_ringing_switch");
		mRinger = (SwitchPreference) findPreference("smart_ringer_switch");
		mOverturn = (SwitchPreference) findPreference("overturn_to_mute");
		Uri uri = Uri.parse("content://com.hb.phone/phone_setting");
		Cursor c = getContentResolver().query(uri, null, null, null, null);
		if (c != null) {
			while (c.moveToNext()) {
				String name = c.getString(c.getColumnIndex("name"));
				boolean value = c.getInt(c.getColumnIndex("value")) > 0;
		       if (name.equalsIgnoreCase("vibrate")) {
					mVibrate.setChecked(value);
				} else if (name.equalsIgnoreCase("flash")) {
					mFlash.setChecked(value);
				} else if (name.equalsIgnoreCase("smartringer")) {
					mRinger.setChecked(value);
				} else if (name.equalsIgnoreCase("overturn")) {
					mOverturn.setChecked(value);
				}
			}
			c.close();
		}
		
		int power = Settings.Secure.getInt(getContentResolver(), Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR, Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
		mPower.setChecked((power & Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP) != 0);		
		
		mPower.setOnPreferenceChangeListener(this);
		mVibrate.setOnPreferenceChangeListener(this);
		mFlash.setOnPreferenceChangeListener(this);	        
		mRinger.setOnPreferenceChangeListener(this);
		mOverturn.setOnPreferenceChangeListener(this);
	}
	
	  @Override
	    public boolean onPreferenceChange(Preference preference, Object objValue) {
			if (preference == mPower) {
//				updateValue("power", (Boolean) objValue);
			    Settings.Secure.putInt(getContentResolver(),
			                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
			                ((Boolean) objValue
			                        ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
			                        : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF));
			} else if (preference == mVibrate) {
				updateValue("vibrate", (Boolean) objValue);
			} else if (preference == mFlash) {
				updateValue("flash", (Boolean) objValue);
			} else if (preference == mRinger) {
				updateValue("smartringer", (Boolean) objValue);
			} else if (preference == mOverturn) {
				updateValue("overturn", (Boolean) objValue);
			}
			return true;		  	  
	  }
	  
	  private void updateValue(String name, boolean value) {
			Uri uri = Uri.parse("content://com.hb.phone/phone_setting");
			ContentResolver cr = getContentResolver();
			ContentValues cv = new ContentValues();
			cv.put("value", value);
			cr.update(uri, cv, "name = '" + name + "'" , null);
	  }


}