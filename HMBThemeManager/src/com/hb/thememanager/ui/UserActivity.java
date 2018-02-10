package com.hb.thememanager.ui;

import android.net.Uri;
import android.os.Bundle;
import hb.app.HbActivity;

import com.hb.thememanager.R;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import com.hb.thememanager.model.User;
import com.hb.thememanager.model.getUserInfoCallBack;
import com.hb.thememanager.ui.fragment.themelist.LocalRingtoneListFragment;
import com.hb.thememanager.ui.fragment.themelist.LocalThemeFontsListFragment;
import com.hb.thememanager.ui.fragment.themelist.LocalThemePkgListFragment;
import com.hb.thememanager.ui.fragment.themelist.LocalWallpaperListFragment;
import com.hb.thememanager.ui.fragment.themelist.PurchaseActivity;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.NormalLoadPicture;
import com.hb.thememanager.views.IconFastEntry;

public class UserActivity extends SecondActivity implements OnClickListener, getUserInfoCallBack {

    private ImageView mUserIcon;
    private TextView mUserName;
    private TextView mUserPhone;
    private IconFastEntry mUserTheme;
    private IconFastEntry mUserWallpaper;
    private IconFastEntry mUserRingtone;
    private IconFastEntry mUserFont;
    private User mUser = User.getInstance(this);
    private LinearLayout mLogin_area;
    private Intent mIntentMore;
    
	@Override
	protected void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
		super.onCreate(arg0);
		setHbContentView(R.layout.activity_user_center);
		init();
	}

	private void init() {
		setTitle(R.string.text_user_center);
		mLogin_area = (LinearLayout) findViewById(R.id.login_area);
        mUserIcon = (ImageView) findViewById(R.id.user_icon);
        mUserName = (TextView) findViewById(R.id.user_name);
        mUserPhone = (TextView) findViewById(R.id.user_phone_num);
        mUserTheme = (IconFastEntry) findViewById(R.id.user_theme);
        mUserWallpaper = (IconFastEntry) findViewById(R.id.user_wallpaper);
        mUserRingtone = (IconFastEntry) findViewById(R.id.user_ringtong);
        mUserFont = (IconFastEntry) findViewById(R.id.user_font);
		
        mLogin_area.setOnClickListener(this);
		mUserTheme.setOnClickListener(this);
		mUserWallpaper.setOnClickListener(this);
		mUserRingtone.setOnClickListener(this);
		mUserFont.setOnClickListener(this);
		refreshLoginStatus();
		refreshMyAssets();
	}

    @Override
    protected void onResume() {
        super.onResume();
        refreshLoginStatus();
        refreshMyAssets();
    }
    
	private void refreshMyAssets() {
	}

	private void refreshLoginStatus() {
		if (mUser.isLogin() ) {
			getUserInfoSuccess();
		} else {
			mUserIcon.setImageResource(R.drawable.user_icon);
			mUserName.setText(R.string.text_user_login);
			mUserPhone.setVisibility(View.GONE);
		}
	}

    public void more(View v) {
    	mIntentMore = new Intent(this, MainActivity.class);
        switch (v.getId()) {
            case R.id.purchase_record:
				mIntentMore.setClass(this, PurchaseActivity.class);
    			if (!mUser.isLogin()) {
    				mUser.jumpLogin(this, this);
    			} else { 
        			startActivity(mIntentMore);
        	        mIntentMore = null;
    			}
                break;
            case R.id.user_feedback:
				mIntentMore.putExtra(MainActivity.EXTRA_THIRD_PART_FRAGMENT_ARGUMENTS, Config.ThemeComponent.FEEDBACK);
    			if (!mUser.isLogin()) {
    				mUser.jumpLogin(this, this);
    			} else { 
    				startActivity(mIntentMore);
    		        mIntentMore = null;
    			}
                break;
            case R.id.settings:
            	mIntentMore.putExtra(MainActivity.EXTRA_THIRD_PART_FRAGMENT_ARGUMENTS, Config.ThemeComponent.SETTING);
        		startActivity(mIntentMore);
		        mIntentMore = null;
                break;
        }
    }

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

		if (v == mLogin_area) {
			if (!mUser.isLogin()) {
				mUser.jumpLogin(this, this);
			} else {
//				getUserInfoSuccess();
				Intent intent = new Intent("android.settings.SYNC_SETTINGS");
				this.startActivity(intent);
			}
		}else{
			gotoUserTheme(v);
		}
	}

	private void gotoUserTheme(View v){
		Intent intent = new Intent(this, MainActivity.class);
		if(v == mUserFont){
			intent.putExtra(MainActivity.EXTRA_SHOW_FRAGMENT, LocalThemeFontsListFragment.class.getName());
			intent.putExtra(MainActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID,R.string.text_user_font);
		}else if(v == mUserTheme){
			intent.putExtra(MainActivity.EXTRA_SHOW_FRAGMENT, LocalThemePkgListFragment.class.getName());
			intent.putExtra(MainActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID,R.string.text_user_theme);
		}else if(v ==  mUserRingtone) {
			intent.putExtra(MainActivity.EXTRA_SHOW_FRAGMENT, LocalRingtoneListFragment.class.getName());
			intent.putExtra(MainActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID,R.string.text_user_ringtong);
		}else if(v ==  mUserWallpaper) {
			intent.putExtra(MainActivity.EXTRA_SHOW_FRAGMENT, LocalWallpaperListFragment.class.getName());
			intent.putExtra(MainActivity.EXTRA_SHOW_FRAGMENT_TITLE_RESID,R.string.text_user_wallpaper);
		}
		startActivity(intent);
	}

	private void getPhoto(String uri) {
		NormalLoadPicture loadPicture;
		loadPicture = new NormalLoadPicture();
		loadPicture.getPicture(uri, mUserIcon);
	}

	@Override
	public void getUserInfoSuccess() {
		// TODO Auto-generated method stub
		getPhoto(mUser.getIconUrl());
		mUserName.setText(mUser.getNickName());
		mUserPhone.setText(mUser.getPhone());
		mUserPhone.setVisibility(View.VISIBLE);
		
		if (mIntentMore!=null) {
			startActivity(mIntentMore);
			mIntentMore = null;
		}
	}

}
