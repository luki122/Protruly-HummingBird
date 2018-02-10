package com.hb.thememanager.http.request;

import android.content.Context;
import android.content.res.AssetManager;

import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.response.IResponseHandler;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeResponse;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchResultThemeRequest extends ThemeRequest{
	private Context mContext;
	private String key;

	public SearchResultThemeRequest(Context context,int themeType){
		super(context,themeType);
		mContext = context;
		setUrl(Config.HttpUrl.SEARCH_URL);
	}
	
//	@Override
//	public void request(Http http, IResponseHandler handler) {
//		// TODO Auto-generated method stub
//		AssetManager asset = mContext.getAssets();
//		try{
//			InputStream input = asset.open(getTestJson(getThemeType()));
//					String code=getEncoding(input);
//					input=asset.open(getTestJson(getThemeType()));
//			BufferedReader br=new BufferedReader(new InputStreamReader(input,code));
//
//			StringBuilder sb = new StringBuilder();
//			String lineTxt = null;
//            while((lineTxt = br.readLine()) != null){
//            	sb.append(lineTxt);
//            }
//            br.close();
//			((RawResponseHandler)handler).onSuccess(200, sb.toString());
//		}catch(Exception e){
//
//		}
//	}

//	private String getTestJson(int themeType){
//		if(themeType == Theme.THEME_PKG){
//			return "test_json/search_result_theme.json";
//		}else if(themeType == Theme.WALLPAPER){
//			return "test_json/search_result_wallpaper.json";
//		}else if(themeType == Theme.FONTS){
//			return "test_json/search_result_font.json";
//		}
//		return "";
//	}

	public void setKey(String key){
		this.key = key;
	}

	@Override
	protected void generateRequestBody() {
		SerchResultBody body = new SerchResultBody();
		body.pageNum = getPageNumber();
		body.pageSize = getPageSize();
		body.type = getThemeType();
		body.key = this.key;
		body.setupAvaliableProperties("pageNum","pageSize","type","key");
		setBody(body);
	}

	@Override
	public Response parseResponse(String responseStr) {
		return null;
	}

	public static class SerchResultBody extends RequestBody{
		public String key;
	}

}
