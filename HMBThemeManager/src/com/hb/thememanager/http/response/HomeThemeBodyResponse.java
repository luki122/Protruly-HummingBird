package com.hb.thememanager.http.response;

import com.hb.thememanager.http.response.adapter.HomeThemeBody;
import com.hb.thememanager.http.response.adapter.ResponseBody;
import com.hb.thememanager.http.response.adapter.ThemeResource;
import com.hb.thememanager.model.HomeThemeCategory;
import com.hb.thememanager.model.Theme;

import java.util.ArrayList;
import java.util.List;

public class HomeThemeBodyResponse extends Response {


	public HomeThemeBody body;
	@Override
	public String toString() {
		return "HomeThemeBodyResponse{" +
				"retCode="+retCode+'\'' +
				"body=" + body +
				'}';
	}


	public List<HomeThemeCategory> getThemes(int themeType) {
		ArrayList<HomeThemeCategory> themes = new ArrayList<HomeThemeCategory>();
		if(body == null || body.hotrecommend == null || body.hotrecommend.size() == 0){
			return themes;
		}
		int categorySize = body.hotrecommend.size();

		for(int i = 0;i<categorySize;i++){
			HomeThemeCategory htc = new HomeThemeCategory();
			htc.setType(HomeThemeCategory.TYPE_CATEGORY);
			ArrayList<ThemeResource> resources = body.hotrecommend.get(i).resource;
			if(resources == null || resources.size() == 0){
				continue;
			}else{
				htc.setChildCount(resources.size());
			}
			htc.id = body.hotrecommend.get(i).recommendId;
			htc.setName(body.hotrecommend.get(i).name);
			htc.setThemes(body.hotrecommend.get(i).getThemes(themeType));
			themes.add(htc);
		}

		return themes;
	}

	@Override
	public ResponseBody returnBody() {
		return body;
	}

}
