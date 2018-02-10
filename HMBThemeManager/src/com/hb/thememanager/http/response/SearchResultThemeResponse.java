package com.hb.thememanager.http.response;

import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.http.response.adapter.ResponseBody;
import com.hb.thememanager.http.response.adapter.SearchResultBody;

import java.util.List;

public class SearchResultThemeResponse extends Response{

	/**
	 * 主题列表
	 */
	public SearchResultBody body;


	@Override
	public String toString(){
		return JSON.toJSONString(this);
	}

	@Override
	public ResponseBody returnBody() {
		return body;
	}
}
