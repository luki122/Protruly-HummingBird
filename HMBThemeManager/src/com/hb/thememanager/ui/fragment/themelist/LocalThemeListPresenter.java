package com.hb.thememanager.ui.fragment.themelist;

import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.hb.thememanager.BasePresenter;
import com.hb.thememanager.ThemeManager;
import com.hb.thememanager.ThemeManagerApplication;
import com.hb.thememanager.listener.OnThemeLoadedListener;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;

public class LocalThemeListPresenter extends BasePresenter<ThemeListMVPView> implements OnThemeLoadedListener<Theme>{
	private static final String TAG = "ThemeListPresenter";
	private ThemeManager mThemeManager;
	private Handler mainThread;
	private Runnable mainThreadCallback;
	public LocalThemeListPresenter(Context context){
		mThemeManager =  ((ThemeManagerApplication)context.getApplicationContext()).getThemeManager();
		mThemeManager.setThemeLoadListener(this);
	}

	@Override
	public void onDestory() {
		// TODO Auto-generated method stub
		mThemeManager.setThemeLoadListener(null);
		detachView();
		if(mainThread != null){
			mainThread.removeCallbacks(mainThreadCallback);
		}
	}
	
	public void loadThemeIntoDatabase(String customThemePath,int themeType){
		mThemeManager.loadThemeIntoDatabase(customThemePath, themeType);
	}

	
	@Override
	public void initialFinished(boolean finished,int type) {
		// TODO Auto-generated method stub
		if(finished){
			mThemeManager.loadThemesFromDatabase(type);
		}
	}

	@Override
	public void onThemeLoaded(final int loadStatus, final Theme theme) {
		// TODO Auto-generated method stub
		if(Looper.myLooper() != Looper.getMainLooper()){
			mainThread = new Handler(Looper.getMainLooper());
			mainThreadCallback = new Runnable() {

				@Override
				public void run() {
					if(getMvpView() == null){
						return;
					}
					// TODO Auto-generated method stub
					if(loadStatus == Config.LoadThemeStatus.STATUS_SUCCESS){
						if(theme != null){
							getMvpView().updateThemeList(theme);
						}
					}else{
						getMvpView().showTips(loadStatus);
					}

				}
			};
			mainThread.post(mainThreadCallback);
		}
		mThemeManager.setThemeLoadListener(null);
	}

	@Override
	public void onThemesLoaded(int loasStatus, List<Theme> themes) {
		// TODO Auto-generated method stub
		
	}



	
	

	
	
}
