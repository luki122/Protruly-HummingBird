package com.hb.thememanager.http.request;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.response.IResponseHandler;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeResponse;
import com.hb.thememanager.utils.Config;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CategoryRequest extends ThemeRequest {


	private Context mContext;


	public CategoryRequest(Context context, int type){
		super(context,type);
		mContext = context;
		setUrl(Config.HttpUrl.getCategoryUrl(type));
	}

	@Override
	public Response parseResponse(String responseStr) {
		return null;
	}

}
