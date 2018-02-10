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
import com.hb.thememanager.model.User;
import com.hb.thememanager.utils.Config;
import com.alibaba.fastjson.JSON;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PurchaseRecordRequest extends ThemeRequest {


	private Context mContext;
	private int mCategory;

    /**
     * 
     * @param context
     * @param themeType 1-付费、2-免费、3-新品、4-热门
     * @param category : theme | font | wallpaper
     */
	public PurchaseRecordRequest(Context context,int themeType,int category){
		super(context, themeType);
		mContext = context;
		mCategory = category;
		setUrl(Config.HttpUrl.getPurchaseRecordUrl());
	}
	
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
		purchaseRequeseBody body = new purchaseRequeseBody();
		body.type = mCategory;
		body.qlcId = User.getInstance(mContext).getId();
		body.pageNum = getPageNumber();
		body.pageSize = getPageSize();
		body.setupAvaliableProperties("type","qlcId","pageNum","pageSize");
		setBody(body);
	}

	private class purchaseRequeseBody extends RequestBody {
		public String qlcId;
		
		public void setQlcId( String id) {
			qlcId = id;
		}
	}
}
