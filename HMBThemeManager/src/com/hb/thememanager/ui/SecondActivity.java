package com.hb.thememanager.ui;

import android.view.View;
import hb.app.HbActivity;

public abstract class SecondActivity extends EmptyViewActivity {

	
	@Override
	public void onNavigationClicked(View view) {
		// TODO Auto-generated method stub
		onBackPressed();
	}
	
	
}
