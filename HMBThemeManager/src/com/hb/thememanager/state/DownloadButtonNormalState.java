package com.hb.thememanager.state;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.hb.thememanager.views.ThemeOptionButton;
import com.hb.thememanager.R;
public class DownloadButtonNormalState implements ThemeState ,OnClickListener{


	private ThemeOptionButton mButton;
	
	public DownloadButtonNormalState(ThemeOptionButton button){
		mButton = button;
		mButton.setOnClickListener(this);
	}

	@Override
	public boolean handleState() {
		if(mButton == null){
			return false;
		}
		mButton.setText(R.string.download_state_apply);
		return false;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		mButton.apply();
	}

}
