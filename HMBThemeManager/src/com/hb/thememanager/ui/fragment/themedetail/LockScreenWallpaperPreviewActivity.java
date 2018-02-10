package com.hb.thememanager.ui.fragment.themedetail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import hb.app.HbActivity;
import hb.widget.ViewPager;
import hb.widget.ViewPager.OnPageChangeListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hb.thememanager.R;
import com.hb.thememanager.job.loader.ImageLoader;
import com.hb.thememanager.listener.OnThemeStateChangeListener;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.state.ThemeState.State;
import com.hb.thememanager.ui.adapter.WallpaperPreivewPagerAdapter;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.views.PreviewIconGrid;
import com.hb.thememanager.views.ThemePreviewDonwloadButton;
import com.hb.thememanager.views.TimeWidget;

import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

public class LockScreenWallpaperPreviewActivity extends HbActivity implements OnPageChangeListener,
OnThemeStateChangeListener{

	private static final int PREVIEW_TOP_ICON_COUNT = 8;
	private static final int PREVIEW_BOTTOM_ICON_COUNT = 4;
	private static final String[] PREVIEW_TOP_ICONS  = {
		"com.android.deskclock","com.android.gallery3d",
		"com.android.quicksearchbox","com.mediatek.camera",
		"com.android.calendar","com.mediatek.filemanager",
		"com.android.documentsui","com.android.calculator2"
	};
	private static final String[] PREVIEW_BOTTOM_ICONS = {
		"com.android.phone","com.mediatek.filemanager",
		"com.android.documentsui","com.android.mms"
	};
	
	private ViewPager mPreviewPager;
	private WallpaperPreivewPagerAdapter mAdapter;
	private int mWallpaperType = Theme.THEME_NULL;
	private PreviewIconGrid mTopIconsList;
	private ThemePreviewDonwloadButton  mApplyButton;
	private PreviewIconGrid mBottomIconsList;
	private IconAdapter mTopIconAdapter;
	private IconAdapter mBottomAdapter;
	private ImageView mIndex;
	private Wallpaper mCurrentWallpaper;
	private List<Wallpaper> mWallpapers;
	private TimeWidget mTimeWidget;
	@Override
	protected void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
		super.onCreate(arg0);
		setContentView(R.layout.activity_wallpaper_perview);
		initial();
	}
	
	private void initial(){
		Intent intent = getIntent();
		Bundle bundle = intent.getBundleExtra(Config.ActionKey.KEY_WALLPAPER_PREVIEW_BUNDLE);
		mWallpapers = bundle.getParcelableArrayList(Config.ActionKey.KEY_WALLPAPER_PREVIEW_LIST);
		mWallpaperType = bundle.getInt(Config.ActionKey.KEY_WALLPAPER_PREVIEW_TYPE);
		int currentPosition = bundle.getInt(Config.ActionKey.KEY_WALLPAPER_PERVIEW_CURRENT_ITEM, 0);
		
		if(mWallpaperType == Theme.WALLPAPER){
			ViewStub stub = (ViewStub)findViewById(R.id.wallpaper_preview_icons);
			stub.inflate();
			loadIcons();
		}
		
		mPreviewPager = (ViewPager)findViewById(R.id.wallpaper_preview_pager);
		mApplyButton = (ThemePreviewDonwloadButton)findViewById(R.id.btn_wallpaper_apply);
		mAdapter = new WallpaperPreivewPagerAdapter(this,mWallpapers);
		mCurrentWallpaper = mWallpapers.get(currentPosition);
		mApplyButton.setTheme(mCurrentWallpaper);
		mApplyButton.setOnStateChangeListener(this);
		mPreviewPager.setAdapter(mAdapter);
		mPreviewPager.setCurrentItem(currentPosition);
		mPreviewPager.setOnPageChangeListener(this);
	}

	
	private void loadIcons() {
		// TODO Auto-generated method stub
		mTopIconsList =(PreviewIconGrid)findViewById(R.id.wallpaper_preview_top_icon_list);
		mBottomIconsList =(PreviewIconGrid)findViewById(R.id.wallpaper_preview_bottom_icon_list);
		mTimeWidget = (TimeWidget)findViewById(R.id.time_widget);
		mTopIconAdapter = new IconAdapter(PREVIEW_TOP_ICONS,this);
		mBottomAdapter = new IconAdapter(PREVIEW_BOTTOM_ICONS,this);
		mTopIconsList.setAdapter(mTopIconAdapter);
		mBottomIconsList.setAdapter(mBottomAdapter);
	}
	
	
	private static final class IconAdapter extends BaseAdapter{
		private String[] icons;
		private LayoutInflater inflater;
		private PackageManager pm;
		 public IconAdapter(String[] icons,Context context) {
			// TODO Auto-generated constructor stub
			 this.icons = icons;
			 inflater = LayoutInflater.from(context);
			 pm = context.getPackageManager();
		}
		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return icons.length;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return icons[position];
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			convertView = inflater.inflate(R.layout.list_item_icons, null);
			String pkgName = icons[position];
			ImageView iconImg = (ImageView)convertView.findViewById(R.id.icon_drawable);
			TextView name = (TextView)convertView.findViewById(R.id.icon_name);
			try {
				ApplicationInfo appInfo = pm.getApplicationInfo(pkgName,
						PackageManager.GET_META_DATA);
				if (appInfo != null) {
					if(getCount() > PREVIEW_BOTTOM_ICON_COUNT){
					CharSequence appName = pm.getApplicationLabel(appInfo);
						if(!TextUtils.isEmpty(appName)){
							name.setText(appName);
						}
					}
					Drawable dw = pm.getApplicationIcon(appInfo);
					if(dw != null){
						iconImg.setImageDrawable(dw);
					}
				}
			} catch (PackageManager.NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return convertView;
		}
		
	}


	@Override
	public void onPageScrollStateChanged(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPageSelected(int position) {
		// TODO Auto-generated method stub
		if(mWallpapers != null && mWallpapers.size() > 0){
			mCurrentWallpaper = mWallpapers.get(position);
			mApplyButton.setTheme(mCurrentWallpaper);
		}
	}

	@Override
	public void onStateChange(State state) {
		// TODO Auto-generated method stub
		if(state == State.STATE_APPLIED){
			Toast.makeText(this, getResources().getString(R.string.msg_select_wallpaper_applied), Toast.LENGTH_LONG).show();
		}else if(state == State.STATE_FAIL){
			Toast.makeText(this, getResources().getString(R.string.msg_apply_wallpaper_fail), Toast.LENGTH_LONG).show();
		}
		
	}
	
	@Override
	public void finish() {
		// TODO Auto-generated method stub
		Config.NEED_UPDATE_LOCKSCREEN_WALLPAPER_LIST = true;
		super.finish();
		
	}
	
	
}
