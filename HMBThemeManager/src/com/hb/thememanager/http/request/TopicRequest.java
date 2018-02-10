package com.hb.thememanager.http.request;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.res.AssetManager;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.response.IResponseHandler;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeResponse;
import com.hb.thememanager.http.response.TopicThemeResponse;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;

public class TopicRequest extends ThemeRequest{
	private Context mContext;
	
	public TopicRequest(Context context, int themeType){
		super(context,themeType);
		mContext = context;
		setUrl(Config.HttpUrl.getTopicUrl(themeType));
	}
	
	@Override
	public void generateRequestBody() {
		RequestBody body = new RequestBody();
		body.pageNum = getPageNumber();
		body.pageSize = getPageSize();
		body.setupAvaliableProperties("pageNum","pageSize");
		setBody(body);
	}

	@Override
	public Response parseResponse(String responseStr) {
		return JSON.parseObject(responseStr, TopicThemeResponse.class);
	}

}

