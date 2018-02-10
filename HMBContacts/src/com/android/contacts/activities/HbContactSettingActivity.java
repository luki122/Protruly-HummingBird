package com.android.contacts.activities;

import java.util.List;

import com.android.contacts.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import hb.preference.Preference;
import hb.preference.Preference.OnPreferenceClickListener;
import hb.preference.PreferenceActivity;
import hb.preference.PreferenceGroup;
import hb.widget.toolbar.Toolbar;

import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.util.AccountFilterUtil;

public class HbContactSettingActivity extends PreferenceActivity implements OnPreferenceClickListener {
	private Preference accountPrefs;
	private Preference mNormalPrefs2;
	private Preference mergePrefs;
	
	private static final int SUBACTIVITY_ACCOUNT_FILTER = 2;

    public static final String KEY_EXTRA_CURRENT_FILTER = "currentFilter";
	private static final String TAG = "HbContactSettingActivity";

    private ContactListFilter mCurrentFilter;
	private Toolbar toolbar;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		toolbar = getToolbar();
        toolbar.setTitle(getResources().getString(R.string.hb_contact_setting));
		toolbar.setElevation(0f);
		addPreferencesFromResource(R.xml.preference_contact_setting);
		findPreferences();
		bindListenerToPreference();
		mCurrentFilter = getIntent().getParcelableExtra(KEY_EXTRA_CURRENT_FILTER);
		
	}

	/**
	 * Find all the Preference by key,
	 */
	private void findPreferences() {
		// normal preference
		accountPrefs = findPreference("preference_accounts");

		mNormalPrefs2 = findPreference("preference_contactio");
		mergePrefs = findPreference("preference_merge_contacts");
	}

	private void bindListenerToPreference() {
		/*
		 * bind ClickListener
		 */
		accountPrefs.setOnPreferenceClickListener(this);
		mNormalPrefs2.setOnPreferenceClickListener(this);
		mergePrefs.setOnPreferenceClickListener(this);

	}
	
//	Intent data;
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "[onActivityResult]requestCode = " + requestCode
				+ ",resultCode = " + resultCode+" data:"+data+" bundle:"+(data==null?"null":data.getExtras()));
		if(requestCode==SUBACTIVITY_ACCOUNT_FILTER&&resultCode==Activity.RESULT_OK){
//			this.data=data;
			setResult(Activity.RESULT_OK,data);
			finish();
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == accountPrefs) {			
			AccountFilterUtil.startAccountFilterActivityForResult(
					this, SUBACTIVITY_ACCOUNT_FILTER,
					mCurrentFilter);
			return true;
		} else if (preference == mNormalPrefs2) {
			Intent intent = new Intent(this, HbContactImportExportActivity.class);
			startActivity(intent);
			return true;
		}else if (preference == mergePrefs) {
			Intent intent = new Intent(this, HbMergeContactsActivity.class);
			startActivity(intent);
			return true;
		}
		return false;
	}
	
//	@Override
//	public void onBackPressed() {
//		Log.d(TAG, "[onBackPressed]");
//		setResult(Activity.RESULT_OK,data);
//		finish();
//	}
}
