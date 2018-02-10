package com.hb.thememanager.http.response;

import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.http.response.adapter.CategoryBody;
import com.hb.thememanager.http.response.adapter.ResponseBody;
import com.hb.thememanager.model.Category;
import com.hb.thememanager.model.Tab;

import java.util.List;

public class CategoryResponse extends Response {

	/**
	 * categories
	 */
	public CategoryBody body;


	@Override
	public String toString(){
		return JSON.toJSONString(this);
	}

	@Override
	public ResponseBody returnBody() {
		return body;
	}
}
