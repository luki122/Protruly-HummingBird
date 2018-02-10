package com.hb.thememanager.state;

/**
 * 该类用于处理针对主题的各种操作状态，包括应用，
 * 下载、暂停下载、重新下载等
 *
 */
public interface ThemeState {
	public enum State{
		STATE_NORMAL,
		STATE_APPLIED,
		STATE_FAIL,
		STATE_FILE_NOT_EXISTS,
		STATE_START_APPLY,
		STATE_APPLY_SUCCESS,
		STATE_SUCCESS,
		STATE_DOWNLOAD,
		STATE_DOWLOADING,
		STATE_PAUSE,
		STATE_RESUME,
		STATE_UPDATE
	}
	
	public  boolean handleState();
}
