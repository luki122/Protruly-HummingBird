package com.hb.thememanager.http.request;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.response.IResponseHandler;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeResponse;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class WallpaperRankingTabRequest extends ThemeRequest {


	private Context mContext;


	public WallpaperRankingTabRequest(Context context){
		super(context);
		mContext = context;
	}
	
	@Override
	public void request(Http http, IResponseHandler handler) {
		// TODO Auto-generated method stub
		AssetManager asset = mContext.getAssets();
		try{
			InputStream input = asset.open("test_json/wallpaper_tabs.json");
			String code=getEncoding(input);
			input=asset.open("test_json/wallpaper_tabs.json");
			BufferedReader br=new BufferedReader(new InputStreamReader(input,"UTF-8"));

			StringBuilder sb = new StringBuilder();
			char[] c = new char[1024];
			String s = null;
			int len;

			while((len = br.read(c)) != -1){
				sb.append(c,0,len);
			}
			br.close();
			((RawResponseHandler)handler).onSuccess(200, sb.toString());
		}catch(Exception e){
			e.printStackTrace();
			Log.d("cate", "exception"+e);
		}
		
	}


	@Override
	public Response parseResponse(String responseStr) {
		return null;
	}

	@Override
	public void request() {
		// TODO Auto-generated method stub

	}

}
