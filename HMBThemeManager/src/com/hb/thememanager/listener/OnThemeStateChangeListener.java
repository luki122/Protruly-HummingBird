package com.hb.thememanager.listener;

import com.hb.thememanager.state.StateManager;
import com.hb.thememanager.state.ThemeState;

/**
 * Callback for theme option state change
 *
 */
public interface OnThemeStateChangeListener {
	
	/**
	 * Called by {@link StateManager#postState(ThemeState)}
	 * @param state new state of theme option
	 */
	public void onStateChange(ThemeState state);

}
