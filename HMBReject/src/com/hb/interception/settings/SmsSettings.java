package com.hb.interception.settings;

import com.hb.interception.util.BlackUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import hb.preference.PreferenceActivity;
import hb.preference.PreferenceScreen;
import hb.preference.SwitchPreference;
import hb.preference.Preference;
import com.hb.interception.R;

public class SmsSettings extends PreferenceActivity implements Preference.OnPreferenceChangeListener{
	
	SwitchPreference mBlack, mSmart, mKeyword;

	   @Override
	    protected void onCreate(Bundle icicle) {
	        super.onCreate(icicle);
	        addPreferencesFromResource(R.xml.sms_settings);
	        initSwtichs();
	   }

	@Override
	public boolean onPreferenceChange(Preference preference, Object objValue) {
		// TODO Auto-generated method stub
		if (preference == mBlack) {
			BlackUtils.updateValue(this, "sms_black", (Boolean) objValue);
			return true;
		} else if (preference == mSmart) {
			BlackUtils.updateValue(this, "sms_smart", (Boolean) objValue);
			return true;
		} else if (preference == mKeyword) {
			BlackUtils.updateValue(this, "sms_keyword", (Boolean) objValue);
			return true;
			}
		return false;
	}
	
	private void initSwtichs() {
        PreferenceScreen prefSet = getPreferenceScreen();
        mBlack =(SwitchPreference)findPreference("black_reject");
        mSmart =(SwitchPreference)findPreference("smart_reject");
        mKeyword =(SwitchPreference)findPreference("keyword_reject");

		Cursor c = getContentResolver().query(BlackUtils.SETTING_URI, null, null, null, null);
		if (c != null) {
			while (c.moveToNext()) {
				String name = c.getString(c.getColumnIndex("name"));
				boolean value = c.getInt(c.getColumnIndex("value")) > 0;
		       if (name.equalsIgnoreCase("sms_black")) {
		    	   mBlack.setChecked(value);
				} else if (name.equalsIgnoreCase("sms_smart")) {
					mSmart.setChecked(value);
				} else if (name.equalsIgnoreCase("sms_keyword")) {
					mKeyword.setChecked(value);
				}
			}
			c.close();
		}
		
        mBlack.setOnPreferenceChangeListener(this);
        mSmart.setOnPreferenceChangeListener(this);
        mKeyword.setOnPreferenceChangeListener(this);
	}
	
}