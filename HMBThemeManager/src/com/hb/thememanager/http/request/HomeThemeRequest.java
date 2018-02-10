package com.hb.thememanager.http.request;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.PropertyFilter;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;
import com.hb.themeicon.theme.IconManager;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.response.HomeThemeBodyResponse;
import com.hb.thememanager.http.response.IResponseHandler;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeResponse;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;

/**
 * 主页主题列表获取请求全部通过该类来处理，通过不同的类型{@link Theme#type}，{@link ThemeRequest#mThemeType}
 * 来配置不同的请求地址.
 */
public class HomeThemeRequest extends ThemeRequest {


	private Context mContext;




	public HomeThemeRequest(Context context,int themeType){
		super(context,themeType);
		mContext = context;
		setUrl(Config.HttpUrl.getHomeThemeUrl(themeType));
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
		return JSON.parseObject(responseStr, HomeThemeBodyResponse.class);
	}


}
