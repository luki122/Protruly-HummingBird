package com.hb.thememanager.http.response;

import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.http.response.adapter.ResponseBody;
import com.hb.thememanager.http.response.adapter.ThemeBody;
import com.hb.thememanager.model.RankingCategory;

import java.util.List;

public class ThemeListResponse extends Response {

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
