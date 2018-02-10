package com.hb.thememanager.http.request;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.http.response.ThemeListResponse;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.utils.Config;

public class CategoryDetailRequest extends ThemeRequest {


	private Context mContext;
	private int category;


	public CategoryDetailRequest(Context context, int themeType, int category, int id){
		super(context, themeType);
		mContext = context;
		this.category = category;
		setId(String.valueOf(id));
		setUrl(Config.HttpUrl.getCategoryDetailUrl(themeType));
	}

	@Override
	public Response parseResponse(String responseStr) {
		return JSON.parseObject(responseStr, ThemeListResponse.class);
	}

	@Override
	protected void generateRequestBody() {
		RequestBody body = new RequestBody();
		body.type = category;
		body.pageNum = getPageNumber();
		body.pageSize = getPageSize();
		body.id = getId();
		body.setupAvaliableProperties("id","type","pageNum","pageSize");
		setBody(body);
	}

}
