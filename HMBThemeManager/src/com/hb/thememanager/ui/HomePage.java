package com.hb.thememanager.ui;

import java.util.ArrayList;
import java.util.List;

import com.hb.thememanager.R;
import com.hb.thememanager.UpgradeService;
import com.hb.thememanager.database.SharePreferenceManager;
import com.hb.thememanager.manager.BannerManager;
import com.hb.thememanager.model.User;
import com.hb.thememanager.ui.adapter.HomePagerAdapter;
import com.hb.thememanager.ui.fragment.AbsHomeFragment;
import com.hb.thememanager.ui.fragment.HomeFontFragment;
import com.hb.thememanager.ui.fragment.HomeRingTongFragment;
import com.hb.thememanager.ui.fragment.HomeThemeFragment;
import com.hb.thememanager.ui.fragment.HomeWallpaperFragment;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;

import android.app.Dialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewStub;
import android.widget.CheckBox;
import android.widget.HbSearchView;
import android.widget.ImageButton;
import hb.app.HbActivity;
import hb.app.dialog.AlertDialog;
import hb.widget.ViewPager;
import hb.widget.tab.TabLayout;
import hb.widget.tab.TabLayout.OnTabSelectedListener;
import hb.widget.tab.TabLayout.Tab;
import hb.widget.toolbar.Toolbar;
import hb.widget.toolbar.Toolbar.OnMenuItemClickListener;

public class HomePage extends HbActivity implements OnClickListener,OnMenuItemClickListener{

	private Toolbar mToolbar;
	private Toolbar mRingtoneToolbar;
	private TabLayout mTab;
	private ViewPager mTabPager;
	private ImageButton mUserIcon;
	private HomePagerAdapter mAdapter;
	private List<Fragment> mFragments;
	private ViewStub mPanelNoNetwork;
	private View mPanelNoNetworkView;
	private Handler mHandler = new Handler();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		startCheckNewVersion();
		setContentView(R.layout.activity_home_page);
		mPanelNoNetwork = (ViewStub)findViewById(R.id.stub_panel_no_network);
		initialUI(savedInstanceState);
		showAuthorizationDialog();
	}
	
	@Override
	protected void initialUI(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		mToolbar = (Toolbar)findViewById(R.id.toolbar);
		mToolbar.inflateMenu(R.menu.menu_search_icon);
		mTab = (TabLayout)findViewById(R.id.tab_layout);
		mTabPager = (ViewPager)findViewById(R.id.view_pager);
		mUserIcon = (ImageButton)findViewById(R.id.toolbar_user_icon);
		mToolbar.setOnMenuItemClickListener(this);
		mUserIcon.setOnClickListener(this);
		mRingtoneToolbar = (Toolbar)findViewById(R.id.ringtone_toolbar);
		
		setupPager();
		//Check network state when start theme application first time
		if(!CommonUtil.hasNetwork(this)){
			showNetworkErrorPanel(false);
		}else{
			mPanelNoNetwork.setVisibility(View.GONE);
		}


	}

	private void showAuthorizationDialog(){
		boolean noShowAuthorization = SharePreferenceManager.getBooleanPreference(getApplicationContext(),
				SharePreferenceManager.KEY_SHOW_AUTHORIZATION,false);
		if(!noShowAuthorization) {
			View tipsView = getLayoutInflater().inflate(R.layout.authorization_view,null);
			final CheckBox checkBox = (CheckBox) tipsView.findViewById(android.R.id.button1);
			checkBox.setChecked(true);
			AlertDialog dialog = new AlertDialog.Builder(this)
					.setTitle(R.string.tips_title)
					.setView(tipsView)
					.setPositiveButton(R.string.no_tips, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							SharePreferenceManager.setBooleanPreference(getApplicationContext(),
									SharePreferenceManager.KEY_SHOW_AUTHORIZATION,checkBox.isChecked());
						}
					})
					.setNegativeButton(R.string.quit, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							finish();
						}
					})
					.create();
			dialog.setCancelable(false);
			dialog.setCanceledOnTouchOutside(false);
			dialog.show();

		}

	}

	private void startCheckNewVersion(){
		Intent intent = new Intent(Config.Action.UPGRADE_THEME_SERVICE);
		intent.setClass(this, UpgradeService.class);
		startService(intent);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		return super.onCreateDialog(id);
	}




	private void initialNoNetworkPanel(){
		if(mPanelNoNetworkView == null) {
			mPanelNoNetwork.inflate();
			mPanelNoNetworkView = findViewById(mPanelNoNetwork.getInflatedId());
		}
		mPanelNoNetworkView.setOnClickListener(this);
	}
	public Toolbar  getRingtoneToolbar() {
		return mRingtoneToolbar;
	}
	
	@Override
	public Toolbar getToolbar() {
		return mToolbar;
	}


	public void showNetworkErrorPanel(boolean show){
		initialNoNetworkPanel();
		mPanelNoNetwork.setVisibility(show?View.VISIBLE:View.GONE);
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
		BannerManager.getInstance().releaseBanner(getComponentName().getClassName());
	}

	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub
		if(view == mUserIcon){
			startActivity(new Intent(Config.Action.ACTION_USER));
		}else if(view == mPanelNoNetworkView){
			startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
		}
	}
	
	@Override
	public boolean onMenuItemClick(MenuItem item) {
		// TODO Auto-generated method stub
		if(item.getItemId() == R.id.search_icon){
			startActivity(new Intent(Config.Action.ACTION_SEARCH));
		}
		return super.onMenuItemClick(item);
	}
	
	
	private void setupPager(){
		mTab.addTab(mTab.newTab());
		mTab.addTab(mTab.newTab());
		mTab.addTab(mTab.newTab());
		mFragments = new ArrayList<Fragment>();
		mFragments.add(new HomeThemeFragment(getResources().getString(R.string.tab_title_theme)));
		mFragments.add(new HomeWallpaperFragment(getResources().getString(R.string.tab_title_wallpaper)));
		mFragments.add(new HomeRingTongFragment(getResources().getString(R.string.tab_title_ringtong)));
		mFragments.add(new HomeFontFragment(getResources().getString(R.string.tab_title_font)));
        mAdapter = new HomePagerAdapter(getFragmentManager(), mFragments);
		mTabPager.setOffscreenPageLimit(4);
        mTabPager.setAdapter(mAdapter);
        mTab.setupWithViewPager(mTabPager);
        
        mTab.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mTabPager) {

			@Override
			public void onTabReselected(Tab tab) {
				// TODO Auto-generated method stub
				super.onTabReselected(tab);
			}

			@Override
			public void onTabSelected(Tab tab) {
				// TODO Auto-generated method stub
				super.onTabSelected(tab);
				if (2 != tab.getPosition()) {
					HomeRingTongFragment ringtone;
					ringtone = (HomeRingTongFragment) mFragments.get(2);
					try {
						if (null != ringtone && null != ringtone.mWebView ) {
							ringtone.mWebView.loadUrl("javascript:KY.ine.stopPlay()");
						}
					} catch (Exception e) {

					}
				}
			}

			@Override
			public void onTabUnselected(Tab tab) {
				// TODO Auto-generated method stub
				super.onTabUnselected(tab);
			}

        });
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
		if (keyCode == keyEvent.KEYCODE_BACK) {// 监听返回键，如果可以后退就后退
			if (2 == mTabPager.getCurrentItem()) {
				HomeRingTongFragment ringtone;
				ringtone = (HomeRingTongFragment) mFragments.get(2);
				if (ringtone.mWebView.canGoBack()) {
					ringtone.mWebView.goBack();
					return true;
				}
			}
		}
		return super.onKeyDown(keyCode, keyEvent);
	}
	
	public int getCurrentPage() {
	    return mTabPager.getCurrentItem();
	}
	
	public ViewPager getViewPager() {
		return mTabPager;
	}




}
