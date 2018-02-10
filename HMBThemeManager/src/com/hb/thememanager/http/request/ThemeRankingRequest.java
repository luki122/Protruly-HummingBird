package com.hb.thememanager.http.request;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.response.IResponseHandler;
import com.hb.thememanager.http.response.ThemeListResponse;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeResponse;
import com.hb.thememanager.utils.Config;
import com.alibaba.fastjson.JSON;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ThemeRankingRequest extends ThemeRequest {


	private Context mContext;
	private int mCategory;

    /**
     * 
     * @param context
     * @param themeType 1-付费、2-免费、3-新品、4-热门
     * @param category : theme | font | wallpaper
     */
	public ThemeRankingRequest(Context context,int themeType,int category){
		super(context, themeType);
		mContext = context;
		mCategory = category;
		setUrl(Config.HttpUrl.getRankUrl(themeType));
	}
	
//	@Override
//	public void request(Http http, IResponseHandler handler) {
//		// TODO Auto-generated method stub
//		AssetManager asset = mContext.getAssets();
//		try{
//			InputStream input = asset.open("test_json/theme_rank.json");
//			String code=getEncoding(input);
//			input=asset.open("test_json/theme_rank.json");
//			BufferedReader br=new BufferedReader(new InputStreamReader(input,"UTF-8"));
//
//			StringBuilder sb = new StringBuilder();
//			char[] c = new char[1024];
//			String s = null;
//			int len;
//
//			while((len = br.read(c)) != -1){
//				sb.append(c,0,len);
//			}
//			br.close();
//			((RawResponseHandler)handler).onSuccess(200, sb.toString());
//		}catch(Exception e){
//			e.printStackTrace();
//			Log.d("cate", "exception"+e);
//		}
//
//	}

	


	@Override
	public void request() {
		// TODO Auto-generated method stub

	}

	@Override
	public Response parseResponse(String responseStr) {
		return JSON.parseObject(responseStr, ThemeListResponse.class);
	}

	@Override
	protected void generateRequestBody() {
		RequestBody body = new RequestBody();
		body.type = mCategory;
		body.pageNum = getPageNumber();
		body.pageSize = getPageSize();
		body.setupAvaliableProperties("type","pageNum","pageSize");
		setBody(body);
	}

}
