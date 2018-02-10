package com.hb.thememanager.ui;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.BasePresenter;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.request.CategoryRequest;
import com.hb.thememanager.http.request.ThemeRequest;
import com.hb.thememanager.http.response.CategoryResponse;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.job.loader.IRequestTheme;

public class CategoryPresenter extends BasePresenter<CategoryView> implements IRequestTheme{

	private Http mHttp;
	private Context mContext;
	public CategoryPresenter(Context context) {
		// TODO Auto-generated constructor stub
		mContext = context;
		mHttp = Http.getHttp(mContext);
	}
	
	@Override
	public void onDestory() {
		// TODO Auto-generated method stub
	}
	

	@Override
	public void requestTheme(final ThemeRequest themeType) {
		// TODO Auto-generated method stub
		themeType.request(mHttp, new RawResponseHandler() {
			
			@Override
			public void onFailure(int statusCode, String error_msg) {
				// TODO Auto-generated method stub
				Log.e("response", "onFailure : statusCode = "+statusCode+" ; error_msg = "+error_msg);
				getMvpView().networkError(statusCode, error_msg);
			}
			
			@Override
			public void onSuccess(int statusCode, String response) {
				// TODO Auto-generated method stub
				Log.e("response", "onSuccess : statusCode = "+statusCode+" ; response = "+response);
				CategoryResponse responseObj = JSON.parseObject(response, CategoryResponse.class);
				if(themeType instanceof CategoryRequest){
					getMvpView().updateList(responseObj);
				}
			}}
		);
	}

	@Override
	public void refresh(ThemeRequest themeType) {
		// TODO Auto-generated method stub
	}

	@Override
	public void loadMore(ThemeRequest themeType) {
		// TODO Auto-generated method stub
	}
	
	
	
	

}
