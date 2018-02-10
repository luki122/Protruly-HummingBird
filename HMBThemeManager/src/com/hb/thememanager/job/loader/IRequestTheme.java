package com.hb.thememanager.job.loader;

import com.hb.thememanager.model.Theme;
import com.hb.thememanager.http.request.ThemeRequest;

/**
 *实现这个接口去向服务器请求主题数据 
 *
 * @param <T>
 */
public interface IRequestTheme {
	
	
	/**
	 * 向服务器发起请求获取对应类别的主题
	 * 数据
	 * @param themeType see {@link RequestThemeType}
	 */
	public void requestTheme(ThemeRequest themeType);
	
	/**
	 * 向服务器请求刷新界面
	 * @param themeType see {@link RequestThemeType}
	 * @return
	 */
	public void refresh(ThemeRequest themeType);
	
	/**
	 * 向服务器请求加载更多数据
	 * @param themeType
	 * @return see {@link RequestThemeType}
	 */
	public void loadMore(ThemeRequest themeType);
	
	

}
