package com.hb.thememanager.http.request;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.response.HomeThemeHeaderResponse;
import com.hb.thememanager.http.response.IResponseHandler;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeResponse;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.Config;

/**
 *
 */
public class HomeThemeHeaderRequest extends ThemeRequest{

	private static final int BANNER_WALLPAPER = 3;
	private Context mContext;


	public HomeThemeHeaderRequest(Context context,int themeType){
		super(context,themeType);
		mContext = context;
		setUrl(Config.HttpUrl.BANNER);
	}
	


	@Override
	public void request() {
		// TODO Auto-generated method stub
		
	}




	@Override
	protected void generateRequestBody() {
		RequestBody body = new RequestBody();
		body.type = getThemeType();
		body.pageSize = getPageSize();
		body.setupAvaliableProperties("type","pageSize");
		setBody(body);
	}

	@Override
	public Response parseResponse(String responseStr) {
		return JSON.parseObject(responseStr, HomeThemeHeaderResponse.class);
	}


}
