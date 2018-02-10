package com.hb.thememanager.http.response;

import com.hb.thememanager.http.response.adapter.DesignerThemeBody;
import com.hb.thememanager.http.response.adapter.HomeThemeBody;
import com.hb.thememanager.http.response.adapter.ResponseBody;
import com.hb.thememanager.http.response.adapter.ThemeResource;
import com.hb.thememanager.model.Fonts;
import com.hb.thememanager.model.HomeThemeCategory;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.utils.TLog;

import java.util.ArrayList;
import java.util.List;

public class DesignerThemeResponse extends Response {


	public DesignerThemeBody body;
	@Override
	public String toString() {
		return "HomeThemeBodyResponse{" +
				"retCode="+retCode+'\'' +
				"body=" + body +
				'}';
	}


	public List<Theme> getThemes(int themeType) {
		if(body == null || body.resource == null){
			return null;
		}
		int categorySize = body.resource.size();
		ArrayList<Theme> themes = new ArrayList<Theme>();
		for(int i = 0;i<categorySize;i++){
			Theme theme = getThemeByType(themeType);
			if(theme == null){
				continue;
			}
			ThemeResource tr = body.resource.get(i);
			theme.id = tr.id;
			theme.name = tr.name;
			theme.coverUrl = tr.icon;
			theme.downloadUrl = tr.downloadUrl;
			theme.hasComment = Theme.HAS_COMMENT;
			theme.isCharge = tr.isCharge;
			theme.price = tr.isCharge == 0?null:String.valueOf(tr.price/100.0f);
			theme.type = themeType;
			themes.add(theme);
		}

		return themes;
	}

	private Theme getThemeByType(int type){
		switch (type){
			case Theme.FONTS:
				return new Fonts();
			case Theme.WALLPAPER:
				return new Wallpaper();
			case Theme.THEME_PKG:
				return new Theme();
			default:
				return null;
		}
	}

	@Override
	public ResponseBody returnBody() {
		return body;
	}


	public DesignerThemeBody getBody() {
		return body;
	}

	public void setBody(DesignerThemeBody body) {
		this.body = body;
	}
}
