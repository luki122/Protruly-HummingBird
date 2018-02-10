package com.hb.thememanager.ui.fragment.themedetail;

import java.io.File;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.hb.thememanager.BasePresenter;
import com.hb.thememanager.ThemeManager;
import com.hb.thememanager.ThemeManagerImpl;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.downloader.DownloadInfo;
import com.hb.thememanager.http.downloader.DownloadManager;
import com.hb.thememanager.http.downloader.DownloadService;
import com.hb.thememanager.http.downloader.callback.DownloadListener;
import com.hb.thememanager.http.downloader.exception.DownloadException;
import com.hb.thememanager.http.request.DesignerThemeRequest;
import com.hb.thememanager.http.request.ThemeCommentsRequest;
import com.hb.thememanager.http.request.ThemeDetailRequest;
import com.hb.thememanager.http.request.ThemeRequest;
import com.hb.thememanager.http.response.DesignerThemeResponse;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeCommentsResponse;
import com.hb.thememanager.http.response.ThemePkgDetailResponse;
import com.hb.thememanager.job.loader.IRequestTheme;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.security.MD5Utils;
import com.hb.thememanager.state.StateManager;
import com.hb.thememanager.state.ThemeState;
import com.hb.thememanager.utils.Config;
import com.alibaba.fastjson.JSON;
import com.hb.thememanager.R;
import com.hb.thememanager.utils.TLog;

public class ThemePkgDetailPresenter extends BasePresenter<ThemePkgDetailMVPView> implements IRequestTheme
{

	private static final String TAG = "ThemeDetail";

	private static final int COMMENT_PAGE_SIZE = 15;

	private static final int DEDIGNER_THEME_PAGE_SIZE = 9;

	private Theme mCurrentTheme;
	
	private Context mContext;
	
	private String mPreviewDir;

	private ThemeRequest mCommentsRequest;

	private ThemeRequest mDesignerThemesRequest;

	private ThemeRequest mThemeDetailRequest;

	private Http mHttp;

	private ThemeManager mThemeManager;


	private StateManager mStateManager;



	public ThemePkgDetailPresenter(Context context,Theme theme){
		mContext = context;
		mCurrentTheme = theme;
		mHttp = Http.getHttp(mContext);
		mThemeManager = ThemeManagerImpl.getInstance(context);
		mThemeManager.themeExists(mCurrentTheme);
		initPreview();
	}


	private void initPreview(){
		StringBuilder builder = new StringBuilder();
		builder.append(mCurrentTheme.loadedPath);
		builder.append(File.separatorChar);
		builder.append(Config.LOCAL_THEME_PREVIEW_DIR_NAME);
		mPreviewDir = builder.toString();
	}

	private void createCommentsRequest(int themeType){
		mCommentsRequest = new ThemeCommentsRequest(mContext,themeType);
		mCommentsRequest.setId(mCurrentTheme.id);
		mCommentsRequest.setPageNumber(0);
		mCommentsRequest.setPageSize(COMMENT_PAGE_SIZE);
	}

	public void createDesignerRequest(int themeType){
		mDesignerThemesRequest = new DesignerThemeRequest(mContext,themeType,mCurrentTheme.designerId);
		mDesignerThemesRequest.setPageNumber(0);
		mDesignerThemesRequest.setPageSize(DEDIGNER_THEME_PAGE_SIZE);
		mDesignerThemesRequest.setId(mCurrentTheme.id);
	}
	
	public void loadThemePreview(){
		if(mCurrentTheme.previewArrays.size() == 0){
			final String previewPath = mPreviewDir;
			File file = new File(previewPath);
			if(file.exists()){
				String[] images = file.list();
				if(images != null){
					for(String s:images){
						mCurrentTheme.previewArrays.add(previewPath+s);
					}
				}
			}
		}
		getMvpView().updateThemeInfo(mCurrentTheme);


	}


	public void updateTheme(){
		
	}
	
	
	
	
	public void updateThemeInfo(){
		getMvpView().updateThemeInfo(mCurrentTheme);
	}
	
	@Override
	public void onDestory() {
		// TODO Auto-generated method stub
		
	}


	public void requestComments(int themeType){
		if(mCommentsRequest == null){
			createCommentsRequest(themeType);
		}

		requestTheme(mCommentsRequest);
	}


	public void requestDetail(int themeType){
		if(mThemeDetailRequest == null){
			mThemeDetailRequest = new ThemeDetailRequest(mContext,themeType);
		}
		mThemeDetailRequest.setId(mCurrentTheme.id);
		requestTheme(mThemeDetailRequest);
	}

	public void requestDesignerThemes(int themeType){
		createDesignerRequest(themeType);
		requestTheme(mDesignerThemesRequest);
	}


	@Override
	public void requestTheme(final ThemeRequest themeType) {
		themeType.request(mHttp, new RawResponseHandler() {
			@Override
			public void onSuccess(int statusCode, String response) {
				Response result = null;
				TLog.d(TAG,"statusCode->"+statusCode+" result->"+"\n"+response);
				if(statusCode == Response.STATUS_CODE_ERROR){
					getMvpView().showRequestFailView(true);
					return;
				}

				if(!TextUtils.isEmpty(response)){
					if(themeType instanceof ThemeCommentsRequest){
						result = JSON.parseObject(response, ThemeCommentsResponse.class);
					}else if(themeType instanceof ThemeDetailRequest){
						result = JSON.parseObject(response, ThemePkgDetailResponse.class);
					}else if(themeType instanceof DesignerThemeRequest){
						result = JSON.parseObject(response,DesignerThemeResponse.class);
					}
					getMvpView().update(result);
				}else{
					getMvpView().showEmptyView(true);
				}

			}

			@Override
			public void onFailure(int statusCode, String error_msg) {
				TLog.e(TAG,"statusCode->"+statusCode+" onFailure->"+"\n"+error_msg);
				getMvpView().showRequestFailView(true);

			}
		});
	}




	@Override
	public void refresh(ThemeRequest themeType) {

	}

	@Override
	public void loadMore(ThemeRequest themeType) {

	}



}
