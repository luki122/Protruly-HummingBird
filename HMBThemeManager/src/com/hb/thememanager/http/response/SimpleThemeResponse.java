package com.hb.thememanager.http.response;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.http.response.adapter.ResponseBody;
import com.hb.thememanager.http.response.adapter.ThemeBody;

public class SimpleThemeResponse extends Response {

	/**
	 * 主题列表
	 */
	public ThemeBody body;


	@Override
	public String toString(){
		return JSON.toJSONString(this);
	}

	@Override
	public ResponseBody returnBody() {
		return body;
	}
}
