package com.hb.thememanager.views;

import hb.app.dialog.ProgressDialog;
import hb.widget.FoldProgressBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.hb.thememanager.R;
import com.hb.thememanager.listener.OnThemeStateChangeListener;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.state.DonwloadOption;
import com.hb.thememanager.state.StateManager;
import com.hb.thememanager.state.DownloadButtonNormalState;
import com.hb.thememanager.state.ThemeState;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.DialogUtils;
public class ThemePreviewDonwloadButton extends ThemeOptionButton{
	
	
	
	public ThemePreviewDonwloadButton(Context context, AttributeSet attrs,
			int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		// TODO Auto-generated constructor stub
	}

	public ThemePreviewDonwloadButton(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
	}

	public ThemePreviewDonwloadButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public ThemePreviewDonwloadButton(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}


	
	
}
