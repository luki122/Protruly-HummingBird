package com.hb.thememanager;

import java.util.List;

import android.content.Context;

import com.hb.thememanager.listener.OnThemeLoadedListener;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.state.StateManager;

public interface ThemeManager {
	
	/**
	 * Apply theme here.
	 * @param theme  The theme need to apply.
	 * @param context
	 * @return true if current theme is apply success.
	 */
	public boolean applyTheme(Theme theme,Context context,StateManager stateManager);
	
	/**
	 * Gets apply status of target theme
	 * @param theme
	 * @return true if target theme applied.
	 */
	public boolean themeApplied(Theme theme);
	
	/**
	 * If target theme has new version in server,update it.
	 * @param theme
	 * @return true if update success.
	 */
	public boolean updateThemeFromInternet(Theme theme);
	
	/**
	 * Update target theme status in database.
	 * @param theme
	 * @return true if update success.
	 */
	public boolean updateThemeinDatabase(Theme theme);
	
	/**
	 * Delete target theme.only for the theme not applied.
	 * @param theme
	 */
	public void deleteTheme(Theme theme);
	
	/**
	 * Delete target themes,only for the theme not applied.
	 * @param themes
	 */
	public void deleteTheme(List<Theme> themes);
	
	/**
	 * Load themes by theme type,load  from database.
	 * @param themeType see{@link com.hb.thememanager.model.Theme#type},
	 *                  must be one of{@link com.hb.thememanager.model.Theme#THEME_PKG},
	 *                  {@link com.hb.thememanager.model.Theme#RINGTONE},
	 *                  {@link com.hb.thememanager.model.Theme#WALLPAPER},or
	 *                  {@link com.hb.thememanager.model.Theme#FONTS}
	 * @return Themes was loaded.
	 */
	public void loadThemesFromDatabase(int themeType);

	/**
	 * Load theme from theme file and save information into database
	 * @param themeType see{@link com.hb.thememanager.model.Theme#type},
	 *                  must be one of{@link com.hb.thememanager.model.Theme#THEME_PKG},
	 *                  {@link com.hb.thememanager.model.Theme#RINGTONE},
	 *                  {@link com.hb.thememanager.model.Theme#WALLPAPER},or
	 *                  {@link com.hb.thememanager.model.Theme#FONTS}
	 * @return Theme was loaded.
	 */
	public void loadThemeIntoDatabase(String themePath,int themeType);

	public void loadThemeIntoDatabase(Theme theme);
	
	
	public void setThemeLoadListener(OnThemeLoadedListener listener);
	
	
	public void loadSystemThemeIntoDatabase(int themeType);

	public boolean themeExists(Theme theme);

	public boolean themeIsBuyByUser(Theme theme,int userId);

	public void savePaiedThemeForUser(Theme theme,int userId);
	
}
