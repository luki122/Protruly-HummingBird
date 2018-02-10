package com.hb.thememanager.http.response;

import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.http.response.adapter.SearchAssistBody;
import com.hb.thememanager.model.Fonts;

import java.util.List;

public class SearchAssistResponse extends Response{

	/**
	 * 主题列表
	 */
	public SearchAssistBody body;

	@Override
	public String toString(){
		return JSON.toJSONString(this);
	}
}
