package com.hb.thememanager.http.response;

import android.util.Log;

import com.hb.thememanager.http.response.adapter.ThemeDetailBody;
import com.hb.thememanager.model.HomeThemeCategory;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;

import java.util.ArrayList;
import java.util.List;

public class ThemePkgDetailResponse extends Response {


	private static final double SIZE = 1024d;

	public ThemeDetailBody body;

	public void updateTheme(Theme theme){
		theme.version = body.version;
		theme.grade = body.score;
		theme.downloadTimes = String.valueOf(body.downloadCount);
		theme.downloadUrl = body.downloadUrl;
		theme.size = String.format("%.0f", body.size/SIZE/SIZE);
		theme.description = body.description;
		if(body.screenshots != null) {
			theme.previewArrays = body.screenshots.small;
		}
		theme.designer = body.designerNickname;
		theme.buyStatus = body.buyStatus;
		theme.designerId = body.designer;

	}

	@Override
	public String toString() {
		return "ThemePkgDetailResponse{" +
				"body=" + body +
				'}';
	}
}
