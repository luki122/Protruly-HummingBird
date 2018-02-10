package com.hb.thememanager.ui.fragment.themelist;

import java.util.List;

import com.hb.thememanager.MvpView;
import com.hb.thememanager.model.Theme;

public interface ThemeListMVPView extends MvpView {
	
	/**
	 * Method for test mvp view,delete later
	 */
	void updateThemeList(Theme theme);
	
	void updateThemeLists(List theme);

	void showTips(int status);
}
