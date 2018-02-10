package com.hb.thememanager.http.request;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.response.IResponseHandler;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeResponse;
import com.hb.thememanager.http.response.TopicDetailBodyResponse;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;

public class TopicDetailBodyRequest extends ThemeRequest{
	private Context mContext;

	public TopicDetailBodyRequest(Context context, int type){
		super(context, type);
		mContext = context;
		setUrl(Config.HttpUrl.getTopicDetailListUrl(type));
	}

	@Override
	public Response parseResponse(String responseStr) {
		return JSON.parseObject(responseStr,TopicDetailBodyResponse.class);
	}


	@Override
	protected void generateRequestBody() {
		RequestBody body = new RequestBody();
		body.id = getId();
		body.pageSize = getPageSize();
		body.pageNum = getPageNumber();
		body.setupAvaliableProperties("id","pageNum","pageSize");
		setBody(body);
	}
}