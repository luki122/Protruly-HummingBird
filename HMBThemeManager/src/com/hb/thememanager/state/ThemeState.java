package com.hb.thememanager.state;

/**
 * 该类用于处理针对主题的各种操作状态，包括应用，
 * 下载、暂停下载、重新下载等
 *
 */
public enum  ThemeState {
	/**
	 * 普通状态，显示应用文字
	 */
	STATE_NORMAL,
	/**
	 * 已应用状态，表示当前主题已经被应用过
	 */
	STATE_APPLIED,
	/**
	 * 失败状态
	 */
	STATE_FAIL,
	/**
	 * 文件不存在状态
	 */
	STATE_FILE_NOT_EXISTS,
	/**
	 * 启动应用状态
	 */
	STATE_START_APPLY,
	/**
	 * 应用成功状态
	 */
	STATE_APPLY_SUCCESS,
	/**
	 * 下载成功状态
	 */
	STATE_SUCCESS,
	/**
	 * 启动下载状态
	 */
	STATE_START_DOWNLOAD,
	/**
	 * 下载完成状态
	 */
	STATE_DOWNLOADED,
	/**
	 * 正在下载状态
	 */
	STATE_DOWLOADING,
	/**
	 * 取消下载状态
	 */
	STATE_CANCEL,
	/**
	 * 暂停下载状态
	 */
	STATE_PAUSE,
	/**
	 * 继续下载状态
	 */
	STATE_RESUME,
	/**
	 * 更新状态，显示可更新文本提示
	 */
	STATE_UPDATE,
	/**
	 * 已支付状态
	 */
	STATE_PAID,


}
