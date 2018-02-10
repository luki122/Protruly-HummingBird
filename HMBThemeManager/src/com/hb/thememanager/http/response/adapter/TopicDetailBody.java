package com.hb.thememanager.http.response.adapter;

import java.util.ArrayList;
import java.util.List;

import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.TopicDetail;

public class TopicDetailBody extends ResponseBody{
	public long specialId;
	public List<TopicDetail> resource;
	
	public ArrayList<Theme> getThemes() {
		ArrayList<Theme> themes = new ArrayList<>();
		for (TopicDetail topic : resource) {
			Theme theme = new Theme();
			theme.coverUrl = topic.getIcon();
			theme.downloadUrl = topic.downloadUrl;
			themes.add(theme);
		}
		return themes;
	}
	
	@Override
	public String toString() {
		return "TopicDetailBody [specialId=" + specialId + ", resource="
				+ resource + "]";
	}

}


