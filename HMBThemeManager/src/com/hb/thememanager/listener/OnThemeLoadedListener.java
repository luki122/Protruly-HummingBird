package com.hb.thememanager.listener;

import java.util.List;

import com.hb.thememanager.model.Theme;

public interface OnThemeLoadedListener<T extends Theme> {
	
	/**
	 * 加载主题的回调函数
	 * @param loasStatus 加载主题的状态，参考{@link com.hb.thememanager.utils.Config.LoadThemeStatus#STATUS_FAIL}，
	 * {@link com.hb.thememanager.utils.Config.LoadThemeStatus#STATUS_SUCCESS},
	 * {@link com.hb.thememanager.utils.Config.LoadThemeStatus#STATUS_THEME_FILE_ERROR}，
	 * {@link com.hb.thememanager.utils.Config.LoadThemeStatus#STATUS_THEME_NOT_EXISTS}
	 * @param theme 成功加载的主题
	 */
	public void onThemeLoaded(int loasStatus,T theme);
	
	public void onThemesLoaded(int loasStatus,List<T> themes);

	/**
	 * @deprecated
	 * @param finished
	 * @param type
	 */
	public void initialFinished(boolean finished,int type);
}
