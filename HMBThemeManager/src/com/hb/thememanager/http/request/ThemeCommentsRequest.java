package com.hb.thememanager.http.request;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.response.CommentsHeaderResponse;
import com.hb.thememanager.http.response.IResponseHandler;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeCommentsResponse;
import com.hb.thememanager.http.response.ThemeResponse;
import com.hb.thememanager.utils.Config;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 所有主题的评论列表通过该请求去发起，通过{@link #mId}和{@link #mThemeType}
 * 来区分主题、字体和壁纸
 */
public class ThemeCommentsRequest extends ThemeRequest {


	private Context mContext;
	public ThemeCommentsRequest(Context context,int themeType){
		super(context,themeType);
		mContext = context;
		setUrl(Config.HttpUrl.COMMENTS_URL);
	}


//	@Override
//	public void request(Http http, IResponseHandler handler) {
//		// TODO Auto-generated method stub
//		super.request(http,handler);
//
//		AssetManager asset = mContext.getAssets();
//		try{
//			InputStream input = asset.open("test_json/comments.json");
//					String code=getEncoding(input);
//					input=asset.open("test_json/comments.json");
//			BufferedReader br=new BufferedReader(new InputStreamReader(input,code));
//
//			StringBuilder sb = new StringBuilder();
//			char[] c = new char[1024];
//			String s = null;
//			int len;
//
//            while((len = br.read(c)) != -1){
//            	sb.append(c,0,len);
//            }
//            br.close();
//			((RawResponseHandler)handler).onSuccess(200, sb.toString());
//		}catch(Exception e){
//			Log.d("cate", "exception"+e);
//		}
//
//	}

	


	@Override
	public void request() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void generateRequestBody() {
		RequestBody body = new RequestBody();
		body.id = getId();
		body.type = getThemeType();
		body.pageSize = getPageSize();
		body.pageNum = getPageNumber();
		body.setupAvaliableProperties("id","type","pageSize","pageNum");
		setBody(body);
	}

	@Override
	public Response parseResponse(String responseStr) {
		return JSON.parseObject(responseStr, ThemeCommentsResponse.class);
	}
}
