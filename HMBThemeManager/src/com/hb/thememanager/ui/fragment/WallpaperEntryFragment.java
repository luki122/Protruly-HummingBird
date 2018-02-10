package com.hb.thememanager.ui.fragment;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hb.thememanager.R;
import com.hb.thememanager.ThemeManagerApplication;
import com.hb.thememanager.ui.MainActivity;
import com.hb.thememanager.ui.fragment.themelist.LocalLockScreenWallpaperFragment;
import com.hb.thememanager.ui.fragment.themelist.LocalDesktopWallpaperListFragment;
import com.hb.thememanager.ui.fragment.themelist.VrWallpaperFragment;
import com.hb.thememanager.utils.Config;

import hb.widget.toolbar.Toolbar;

public class WallpaperEntryFragment extends AbsThemeFragment implements OnClickListener{

	private View mContentView;
	private ImageView mWallpaperItem;
	private ImageView mLockScreenItem;
	private TextView mVrLockScreenItem;
	private WallpaperManager mWm ;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		mContentView = inflater.inflate(R.layout.activity_wallpaper_entry, container, false);
		initView();
		mWm = WallpaperManager.getInstance(getActivity());
		return mContentView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Toolbar toolbar = ((MainActivity)getActivity()).getToolbar();
		if(toolbar != null){
			toolbar.getMenu().clear();
		}

	}

	@Override
	protected void initView() {
		// TODO Auto-generated method stub
		mWallpaperItem = (ImageView)mContentView.findViewById(R.id.desktop_wallpaper_entry);
		mLockScreenItem = (ImageView)mContentView.findViewById(R.id.lockscreen_wallpaper_entry);
		mVrLockScreenItem = (TextView)mContentView.findViewById(R.id.vr_lockscreen_wallpaper_entry);
		mWallpaperItem.setOnClickListener(this);
		mLockScreenItem.setOnClickListener(this);
		mVrLockScreenItem.setOnClickListener(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		WallpaperInfo wInfo = mWm.getWallpaperInfo();
		Drawable wallpaper;
		if(wInfo != null){
		Drawable thumbNail = wInfo.loadThumbnail(getActivity().getPackageManager());
		 wallpaper = thumbNail;
		}else{
			wallpaper = mWm.getDrawable();
		}
		if(wallpaper != null){
			mWallpaperItem.setImageDrawable(wallpaper);
//			mLockScreenItem.setImageDrawable(wallpaper);
		}
		Drawable lockscreenWallpaper = mWm.getLockscreenDrawable();
		if(lockscreenWallpaper != null){
			mLockScreenItem.setImageDrawable(lockscreenWallpaper);
		}else{
			mLockScreenItem.setImageDrawable(wallpaper);
		}
//		File lockscreen_wallpaper = new File(Config.THEME_APPLY_LOCKSCREEN_WALLPAPER,Config.LOCKSCREEN_WALLPAPER_FILENAME);
//		if(lockscreen_wallpaper.exists()) {
//			Glide.with(this).load(lockscreen_wallpaper).
//					diskCacheStrategy(DiskCacheStrategy.NONE).
//					skipMemoryCache(true).
//					into(mLockScreenItem);
//		}
	}
	

	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub
		if(view == mWallpaperItem){
			startFragment(this, LocalDesktopWallpaperListFragment.class.getName(), true, R.string.title_wallpaper, 0, null);
		}else if(view == mLockScreenItem){
			startFragment(this, LocalLockScreenWallpaperFragment.class.getName(), true, R.string.title_lockscreen_wallpaper, 0, null);
		}else if(view == mVrLockScreenItem){
			startFragment(this, VrWallpaperFragment.class.getName(), true, R.string.vr_wallpaper_entry_title, 0, null);
		}
	}
}
