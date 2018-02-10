package com.hb.thememanager.state;


public interface DonwloadOption {
	
	/**
	 * 对于在线主题，需要从服务器下载，在
	 * 这个方法中启动下载操作
	 */
	public  void start();

	/**
	 * 调用这个方法去暂停下载
	 */
	public  void pause();

	/**
	 * 调用这个方法去取消或者停止下载
	 */
	public  void stop();

	/**
	 * 调用这个方法去对处于暂停状态
	 * 的下载进行继续下载
	 */
	public  void resume();

	/**
	 * 调用这个方法去应用选中的主题
	 */
	public  void apply();

	/**
	 * 当检测到服务器有新版本的主题时调用这个方法去
	 * 更新当前主题
	 */
	public  void update();
	
}
