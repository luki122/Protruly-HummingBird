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
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hb.imageloader.HbImageLoader;
import com.hb.imageloader.ImageLoaderConfig;
import com.hb.themeicon.theme.IconManager;
import com.hb.thememanager.R;
import com.hb.thememanager.job.BitmapColorPickerThread;
import com.hb.thememanager.job.BitmapColorPickerThread.OnColorPickerListener;
import com.hb.thememanager.job.loader.ImageLoader;
import com.hb.thememanager.job.loader.ImageLoaderListener;
import com.hb.thememanager.job.loader.WallpaperPreviewIconLoader;
import com.hb.thememanager.job.loader.WallpaperPreviewIconLoader.IconLoadCallBack;
import com.hb.thememanager.listener.OnThemeStateChangeListener;
import com.hb.thememanager.model.PreviewIcon;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.state.ThemeState.State;
import com.hb.thememanager.ui.adapter.WallpaperPreivewPagerAdapter;
import com.hb.thememanager.ui.fragment.themelist.LocalDesktopWallpaperListFragment;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.views.PreviewIconGrid;
import com.hb.thememanager.views.ThemePreviewDonwloadButton;
import com.hb.thememanager.views.TimeWidget;

import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;

public class WallpaperPreviewActivity extends HbActivity implements OnPageChangeListener,
OnThemeStateChangeListener,OnColorPickerListener,IconLoadCallBack{
	private static final int TEXT_COLOR_WHITE = 0xffffffff;
	private static final int TEXT_COLOR_DARK = 0xff4c4c4c;
	private static final int PREVIEW_TOP_ICON_COUNT = 8;
	private static final int PREVIEW_BOTTOM_ICON_COUNT = 4;
	private static final int ICON_BOTTOM = 0;
	private static final int ICON_TOP = 1;
	private static final String[] PREVIEW_TOP_ICONS  = {
		"com.moji.daling","com.android.calendar",
		"com.android.deskclock","com.android.settings",
		"com.darling.appstore","com.hmb.manager",
		"com.protruly.gallery3d.app","com.hb.lockscreenapp"
	};
	private static final String[] PREVIEW_BOTTOM_ICONS = {
		"com.android.dialer","com.android.mms",
		"com.android.browser","com.bql.camera"
	};
	
	private ViewPager mPreviewPager;
	private WallpaperPreivewPagerAdapter mAdapter;
	private int mWallpaperType = Theme.THEME_NULL;
	private int mStartingPosition;
	private int mCurrentPosition;
	private PreviewIconGrid mTopIconsList;
	private ThemePreviewDonwloadButton  mApplyButton;
	private PreviewIconGrid mBottomIconsList;
	private IconAdapter mTopIconAdapter;
	private IconAdapter mBottomAdapter;
	private ImageView mIndex;
	private ImageView mFirstImage;
	private Wallpaper mCurrentWallpaper;
	private List<Wallpaper> mWallpapers;
	private TimeWidget mTimeWidget;
	private WallpaperPreviewIconLoader mIconLoader;
	private SparseArray<Integer> mIconNameColorCache = new SparseArray<Integer>();
	private Handler mHandler = new Handler();
	private HbImageLoader mImageLoader;
	private ImageLoaderConfig mImageLoaderConfig;
	private int mImageWidth,mImageHeight;
	private Runnable mColorAction = new Runnable(){
		@Override
		public void run() {
			int position = mCurrentPosition;
			if(mIconNameColorCache.get(position) != null){
				int color = mIconNameColorCache.get(position).intValue();
				onColorPicked(color, position);
			}else{
				ImageView itemView = mAdapter.getItemView(position);
				BitmapColorPickerThread colorPicker = new BitmapColorPickerThread(itemView, position);
				colorPicker.setOnColorPickerListener(WallpaperPreviewActivity.this);
				colorPicker.execute();
			}
		}
	};
	@Override
	protected void onCreate(Bundle arg0) {
		// TODO Auto-generated method stub
		super.onCreate(arg0);
		setContentView(R.layout.activity_wallpaper_perview);
		mImageWidth = getResources().getDimensionPixelSize(R.dimen.size_image_item_default_width);
		mImageHeight = getResources().getDimensionPixelSize(R.dimen.size_image_item_default_height);
		configImageLoader();
		initial();

	}

	private void configImageLoader(){
		mImageLoader = HbImageLoader.getInstance(this);
		mImageLoaderConfig = new ImageLoaderConfig();
		ImageLoaderConfig.Size size = new ImageLoaderConfig.Size(mImageWidth*2,mImageHeight*2);
		mImageLoaderConfig.setDecodeFormat(Bitmap.Config.ARGB_8888);
		mImageLoaderConfig.setSize(size);
		mImageLoader.setConfig(mImageLoaderConfig);

	}

	private void initial(){
		Intent intent = getIntent();
		Bundle bundle = intent.getBundleExtra(Config.ActionKey.KEY_WALLPAPER_PREVIEW_BUNDLE);
		mWallpapers = bundle.getParcelableArrayList(Config.ActionKey.KEY_WALLPAPER_PREVIEW_LIST);
		mWallpaperType = bundle.getInt(Config.ActionKey.KEY_WALLPAPER_PREVIEW_TYPE);
		int currentPosition = bundle.getInt(Config.ActionKey.KEY_WALLPAPER_PERVIEW_CURRENT_ITEM, 0);
		mStartingPosition = currentPosition;
		mTimeWidget = (TimeWidget)findViewById(R.id.time_widget);
		if(mWallpaperType == Theme.WALLPAPER){
			ViewStub stub = (ViewStub)findViewById(R.id.wallpaper_preview_icons);
			stub.inflate();
			mIconLoader = new WallpaperPreviewIconLoader(getApplicationContext());
			mIconLoader.setIconLoadCallback(this);
			loadIcons();
		//	mTimeWidget.setVisibility(View.GONE);
		}
		
		mPreviewPager = (ViewPager)findViewById(R.id.wallpaper_preview_pager);
		mPreviewPager.setOffscreenPageLimit(2);
		mApplyButton = (ThemePreviewDonwloadButton)findViewById(R.id.btn_wallpaper_apply);
		mFirstImage = (ImageView)findViewById(R.id.image_first);
		if(Config.sStaringImageInPreview != null){
			mFirstImage.setImageDrawable(Config.sStaringImageInPreview);
		}
		mAdapter = new WallpaperPreivewPagerAdapter(this,mWallpapers);
		mAdapter.setImageLoader(mImageLoader);
		mCurrentWallpaper = mWallpapers.get(currentPosition);
		mApplyButton.setTheme(mCurrentWallpaper);
		mApplyButton.setOnStateChangeListener(this);
		mPreviewPager.setAdapter(mAdapter);
		mPreviewPager.setOnPageChangeListener(this);
		mPreviewPager.setCurrentItem(currentPosition);
		
	}

	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		
	}
	
	
	
	private void loadIcons() {
		// TODO Auto-generated method stub
		mTopIconsList =(PreviewIconGrid)findViewById(R.id.wallpaper_preview_top_icon_list);
		mBottomIconsList =(PreviewIconGrid)findViewById(R.id.wallpaper_preview_bottom_icon_list);
		mTimeWidget = (TimeWidget)findViewById(R.id.time_widget);
		mTopIconAdapter = new IconAdapter(PREVIEW_TOP_ICONS, this, ICON_TOP);
		mBottomAdapter = new IconAdapter(PREVIEW_BOTTOM_ICONS, this, ICON_BOTTOM);
		mTopIconsList.setAdapter(mTopIconAdapter);
		mBottomIconsList.setAdapter(mBottomAdapter);
		mIconLoader.execute(PREVIEW_TOP_ICONS,PREVIEW_BOTTOM_ICONS);
	}
	
	
	private static final class IconAdapter extends BaseAdapter{
		private String[] icons;
		private LayoutInflater inflater;
		private int iconNameColor;
		private int defaultWhite;
		private PreviewIcon[] mIcons;
		private int iconPosition = ICON_TOP;
		 public IconAdapter(String[] icons,Context context, int position) {
			// TODO Auto-generated constructor stub
			iconPosition = position;
			 this.icons = icons;
			 inflater = LayoutInflater.from(context);
			 iconNameColor = context.getColor(R.color.wallpaper_preview_ic_name_color);
			 defaultWhite = iconNameColor;
		}
		void setIconNameColor(int color){
			if(color == Config.Color.WHITE){
				iconNameColor = defaultWhite;
			}else{
				iconNameColor = color;
			}
			notifyDataSetChanged();
		}
		public void setIcons(PreviewIcon[] icons){
			mIcons = icons;
			notifyDataSetChanged();
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
			name.setTextColor(iconNameColor);
			if(mIcons != null){
				PreviewIcon icon = mIcons[position];
				if(icon != null){
					name.setText(icon.getName());
					iconImg.setImageDrawable(icon.getThemeIcon());
				}
				
			}
			if(iconPosition == ICON_BOTTOM) {
				name.setVisibility(View.INVISIBLE);
			}
			
			return convertView;
		}
		
	}


	@Override
	public void onPageScrollStateChanged(int arg0) {
		// TODO Auto-generated method stub
		if(mFirstImage.getVisibility() == View.VISIBLE){
			mFirstImage.setVisibility(View.GONE);
		}
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onPageSelected(int position) {
		// TODO Auto-generated method stub
		if(mWallpapers != null && mWallpapers.size() > 0){
			mCurrentWallpaper = mWallpapers.get(position);
			mApplyButton.setTheme(mCurrentWallpaper);
		}
		if(position != -1 && position != mStartingPosition){
			startPickerColor(position);
		}
		
		
	}

	@Override
	public void onStateChange(State state) {
		if(state == State.STATE_APPLIED){
			mApplyButton.setClickable(true);
			Toast.makeText(this, getResources().getString(R.string.msg_select_wallpaper_applied), Toast.LENGTH_LONG).show();
		}else if(state == State.STATE_FAIL){
			mApplyButton.setClickable(true);
			Toast.makeText(this, getResources().getString(R.string.msg_apply_wallpaper_fail), Toast.LENGTH_LONG).show();
		}else if(state == State.STATE_START_APPLY){
			mApplyButton.setClickable(false);
		}
		
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		Config.sStaringImageInPreview = null;
		
	}
	
	@Override
	public void finish() {
		// TODO Auto-generated method stub
		Config.NEED_UPDATE_WALLPAPER_LIST = true;
		super.finish();
		
	}

	@Override
	public void onColorPicked(int color, int position) {
		// TODO Auto-generated method stub
		if(mIconNameColorCache.get(position) == null){
			mIconNameColorCache.put(position, Integer.valueOf(color));
		}
		if(mWallpaperType == Wallpaper.WALLPAPER){
			if(color == TEXT_COLOR_WHITE) {
				mTopIconAdapter.setIconNameColor(color);
			}else {
				mTopIconAdapter.setIconNameColor(TEXT_COLOR_DARK);
			}
		}
		
		if(mTimeWidget != null){
			if(color == TEXT_COLOR_WHITE) {
				mTimeWidget.updateWidgetColor(color);
			}else {
				mTimeWidget.updateWidgetColor(TEXT_COLOR_DARK);
			}
		}
		
		int flag = getWindow().getDecorView().getSystemUiVisibility();
		if(color == Config.Color.WHITE){
			getWindow().getDecorView().setSystemUiVisibility(
					flag &~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
					&~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
		}else{
			getWindow().getDecorView().setSystemUiVisibility(
					flag |View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR 
					|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
		}
		
		
	
	}

	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mHandler.removeCallbacks(mColorAction);
		mWallpapers.clear();
		mAdapter.onDestory();
		mPreviewPager.setOnPageChangeListener(null);
		if(mIconLoader != null){
			mIconLoader.onDestory();
		}

		ImageLoader.cleanAll(getApplicationContext());
	}
	
	private void startPickerColor(int position){
		mCurrentPosition = position;
		mHandler.post(mColorAction);
	}
	

	@Override
	public void onEnterAnimationComplete() {
		// TODO Auto-generated method stub
		if(mStartingPosition != -1){
			startPickerColor(mStartingPosition);
		}
		mStartingPosition = -1;
		
	}

	@Override
	public void onIconLoaded(ArrayList<PreviewIcon[]> icons) {
		// TODO Auto-generated method stub
		if(icons != null && icons.size() == 2){
				mTopIconAdapter.setIcons(icons.get(0));
				mBottomAdapter.setIcons(icons.get(1));
		}
	}
	
	
}
