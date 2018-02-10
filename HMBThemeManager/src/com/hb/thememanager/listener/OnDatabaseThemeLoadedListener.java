package com.hb.thememanager.listener;

import java.util.List;

import com.hb.thememanager.listener.OnThemeLoadedListener;
import com.hb.thememanager.model.Theme;

public interface OnDatabaseThemeLoadedListener extends OnThemeLoadedListener {

	public void onThemeLoaded(boolean loaded,List<Theme> themes);
}
