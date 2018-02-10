package com.hb.thememanager.http.request;

import android.content.Context;
import android.content.res.AssetManager;

import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.response.IResponseHandler;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeResponse;
import com.hb.thememanager.utils.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchAssistRequest extends ThemeRequest{
	private Context mContext;
	private String key;

	public SearchAssistRequest(Context context){
		super(context,0);
		mContext = context;
		setUrl(Config.HttpUrl.SEARCH_ASSIST_URL);
	}
//
//	@Override
//	public void request(Http http, IResponseHandler handler) {
//		// TODO Auto-generated method stub
//		AssetManager asset = mContext.getAssets();
//		try{
//			InputStream input = asset.open("test_json/search_assist.json");
//					String code=getEncoding(input);
//					input=asset.open("test_json/search_assist.json");
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
//
//	@Override
//	public void request() {
//		// TODO Auto-generated method stub
//	}

	@Override
	protected void generateRequestBody(){
		SearchAssistBody body = new SearchAssistBody();
		body.type = getThemeType();
		body.key = this.key;
		body.setupAvaliableProperties("type","key");
		setBody(body);
	}

	public void setKey(String key){
		this.key = key;
	}

	public class SearchAssistBody extends RequestBody{
		String key;

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}
	}


	@Override
	public Response parseResponse(String responseStr) {
		return null;
	}
}
