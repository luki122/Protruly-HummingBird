package com.hb.thememanager.ui.fragment.themedetail;

import java.util.List;

import android.graphics.Bitmap;

import com.hb.thememanager.MvpView;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.model.Theme;

public interface ThemePkgDetailMVPView extends MvpView {
	
	
	
	
	void updateThemeInfo(Theme theme);
	

	void update(Response result);

	void showRequestFailView(boolean show);

}
