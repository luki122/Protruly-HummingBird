package com.hb.thememanager.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.hb.thememanager.http.request.HomeThemeHeaderRequest;
import com.hb.thememanager.http.request.HomeThemeRequest;
import com.hb.thememanager.http.response.HomeThemeBodyResponse;
import com.hb.thememanager.http.response.HomeThemeHeaderResponse;
import com.hb.thememanager.model.HomeThemeCategory;
import com.hb.thememanager.model.HomeThemeHeaderCategory;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.adapter.AbsHomeThemeListAdapter;
import com.hb.thememanager.ui.adapter.HomeWallpaperListAdapter;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.TLog;

/**
 * 壁纸Tab内容页面
 *
 */
public class HomeWallpaperFragment extends AbsHomeFragment {
	private static final String TAG = "HomeWallpaperFragment";
	private HomeWallpaperListAdapter mAdapter;

	public HomeWallpaperFragment(){}
	public HomeWallpaperFragment(CharSequence title) {
		super(title);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected int getThemeType() {
		return Theme.WALLPAPER;
	}

	@Override
	protected AbsHomeThemeListAdapter createAdapter() {
		mAdapter = new HomeWallpaperListAdapter(getContext());

		return mAdapter;
	}
}
