package com.hb.thememanager.ui;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.BasePresenter;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.request.SearchAssistRequest;
import com.hb.thememanager.http.request.SearchResultThemeRequest;
import com.hb.thememanager.http.request.ThemeRankingRequest;
import com.hb.thememanager.http.request.ThemeRankingTabRequest;
import com.hb.thememanager.http.request.ThemeRequest;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.SearchAssistResponse;
import com.hb.thememanager.http.response.SearchResultThemeResponse;
import com.hb.thememanager.job.loader.IRequestTheme;

public class SearchPresenter extends BasePresenter<SearchView> implements IRequestTheme{

	private Http mHttp;
	private Context mContext;
	public SearchPresenter(Context context) {
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
				getMvpView().showNetworkErrorView(true);
			}
			
			@Override
			public void onSuccess(int statusCode, String response) {
				// TODO Auto-generated method stub
				android.util.Log.e("search", "statusCode = "+statusCode+" ; response = "+response);
				if(themeType instanceof SearchResultThemeRequest){
					SearchResultThemeResponse responseObj = JSON.parseObject(response, SearchResultThemeResponse.class);
					getMvpView().updateList(responseObj);
				} else if(themeType instanceof SearchAssistRequest){
					SearchAssistResponse responseObj = JSON.parseObject(response, SearchAssistResponse.class);
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
