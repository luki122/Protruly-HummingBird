package com.hb.thememanager.listener;

import android.view.KeyEvent;
/**
 *Callback for handle KeyEvent on Fragment. 
 */
public interface OnKeyPressListener {
	
	/**
	 * Handle back key pressed event,see{@link android.app.Activity#onBackPressed()}.
	 */
	void onBackPressed();
	
	/**
	 * Handle KeyDown events,see{@link android.app.Activity#onKeyDown(int, KeyEvent)}
	 * @param keyCode
	 * @param event
	 * @return
	 */
	boolean onKeyDown(int keyCode, KeyEvent event);

	/**
	 * Handle KeyUp events,see{@link android.app.Activity#onKeyUp(int, KeyEvent)}
	 * @param keyCode
	 * @param event
	 * @return
	 */
	boolean onKeyUp(int keyCode, KeyEvent event);
}
