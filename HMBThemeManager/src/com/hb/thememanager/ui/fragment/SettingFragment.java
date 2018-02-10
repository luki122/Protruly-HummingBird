package com.hb.thememanager.ui.fragment;

import hb.preference.Preference;
import hb.preference.Preference.OnPreferenceChangeListener;
import hb.preference.Preference.OnPreferenceClickListener;
import hb.preference.PreferenceFragment;
import hb.preference.PreferenceManager;
import hb.preference.PreferenceScreen;
import hb.preference.SwitchPreference;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.hb.thememanager.R;
import com.hb.thememanager.database.SharePreferenceManager;
import com.hb.thememanager.ui.MainActivity;

public class SettingFragment extends PreferenceFragment {

	private Preference mAbout;
	private SwitchPreference mWifiOnly;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.setting_preference);
		
		mAbout = this.findPreference("preference_about");
		mWifiOnly = (SwitchPreference)this.findPreference("switch_preference");
		mWifiOnly.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){

			@Override
			public boolean onPreferenceChange(Preference arg0, Object newValue) {
				// TODO Auto-generated method stub
				SharePreferenceManager.setBooleanPreference(getContext(),
						SharePreferenceManager.KEY_DOWNLOAD_WITH_MOBILE_NETWORK,(Boolean)newValue);
				if (!(Boolean)newValue) {
					arg0.setSummary(R.string.msg_no_wifi_default_setting);	
				} else {
					arg0.setSummary(R.string.msg_no_wifi_open);
					SharePreferenceManager.setIntPreference(getContext(),
							SharePreferenceManager.KEY_TIMES_SHOW_MOBILE_CONFIRM_DIALOG,1);

				}
				return true;
			}
			
		});

		boolean checkbox = SharePreferenceManager.getBooleanPreference(getContext()
				,SharePreferenceManager.KEY_DOWNLOAD_WITH_MOBILE_NETWORK,false);
        if (!checkbox) {
        	mWifiOnly.setSummary(R.string.msg_no_wifi_default_setting);	
        } else {
        	mWifiOnly.setSummary(R.string.msg_no_wifi_open);
        }
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		// TODO Auto-generated method stub
		if (preference.equals(mAbout)) {
			Intent intent = new Intent(this.getContext(), MainActivity.class);
			intent.putExtra(MainActivity.EXTRA_SHOW_FRAGMENT, AboutFragment.class.getName());
			intent.putExtra(MainActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID, R.string.msg_about);
			startActivity(intent);
			return true;
		}

		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}


}

