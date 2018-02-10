package com.hb.thememanager.ui.fragment;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.R;
import com.hb.thememanager.http.request.HomeThemeHeaderRequest;
import com.hb.thememanager.http.request.HomeThemeRequest;
import com.hb.thememanager.http.response.HomeThemeBodyResponse;
import com.hb.thememanager.http.response.HomeThemeHeaderResponse;
import com.hb.thememanager.model.HomeThemeCategory;
import com.hb.thememanager.model.HomeThemeHeaderCategory;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.adapter.AbsHomeThemeListAdapter;
import com.hb.thememanager.ui.adapter.HomeThemeListAdapter;
import com.hb.thememanager.utils.Config;

/**
 * 主题包Tab内容页面
 *
 */
public class HomeThemeFragment extends AbsHomeFragment  {
	private static final String TAG = "HomeThemeFragment";
	private HomeThemeListAdapter mAdapter;
	public HomeThemeFragment(){}
	public HomeThemeFragment(CharSequence title) {
		super(title);
		// TODO Auto-generated constructor stub
		
	}

	@Override
	protected int getThemeType() {
		return Theme.THEME_PKG;
	}

	@Override
	protected AbsHomeThemeListAdapter createAdapter() {
		mAdapter = new HomeThemeListAdapter(getContext());
		return mAdapter;
	}
}
