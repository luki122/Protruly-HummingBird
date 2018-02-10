package com.hb.thememanager.http.response;

import java.util.ArrayList;
import java.util.List;

import com.hb.thememanager.model.HomeThemeCategory;

/**
 *列表页面的每个请求从服务器返回之后都会被解析为
 *该类型的Response对象，然后再通过和 {@link HomeThemeCategory}
 *配合设置到列表中
 *
 */
public class ThemeResponse {
	private String status;
	/**
	 * 一条请求返回的数据可能包含0到n个主题分类，每一个主题分类中包含0到n个
	 * 主题信息
	 */
	private List<HomeThemeCategory> themes = new ArrayList<HomeThemeCategory>();



	public List<HomeThemeCategory> getThemes() {
		return themes;
	}

	public void setThemes(List<HomeThemeCategory> themes) {
		this.themes = themes;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "ThemeResponse [retCode=" + status + ", themes=" + themes + "]";
	}
	
	
	
}
