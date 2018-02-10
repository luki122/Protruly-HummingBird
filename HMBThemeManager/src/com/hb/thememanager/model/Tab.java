package com.hb.thememanager.model;

/**
 * 可配置tab
 * @author alexluo
 *
 */
public class Tab {
	/**
	 * 1-付费、2-免费、3-新品、4-热门
	 * 主题和字体
	 */
	public static final int CHARG = 1;
	public static final int FREE = 2;
	public static final int NEW = 3;
	public static final int HOT = 4;

	/**
	 * 1-热门、2-新品
	 * 壁纸
	 */
	public static final int WALLPAPER_NEW = 2;
	public static final int WALLPAPER_HOT = 1;
	
	public static final int PURCHASE_THEME = 1;
	public static final int PURCHASE_FONT = 2;

	/**
	 * tab数据接口
	 */
	public String url;
	
	/**
	 * tab标题
	 */
	public String title;

	public int type;
	


	
}
