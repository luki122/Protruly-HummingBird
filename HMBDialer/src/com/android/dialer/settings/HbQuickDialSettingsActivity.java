package com.android.dialer.settings;

import com.android.contacts.common.activity.TransactionSafeActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.android.dialer.R;
import hb.widget.toolbar.Toolbar;

public class HbQuickDialSettingsActivity extends TransactionSafeActivity{

	private Toolbar toolbar;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		toolbar = getToolbar();
		toolbar.setTitle(getString(R.string.hb_quickdial_settings));

		toolbar.setNavigationOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});
		
		setHbContentView(R.layout.hb_quickdial_settings_activity);
		getFragmentManager().beginTransaction()
		.add(R.id.hb_quickdial_settings_fragment, new HbQuickDialSettingsFragment(), "hbQuickDialSettings")
		.commit();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}



}
