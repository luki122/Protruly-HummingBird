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
	
	public LocalThemeListPresenter(Context context){
		mThemeManager =  ((ThemeManagerApplication)context.getApplicationContext()).getThemeManager();
		mThemeManager.setThemeLoadListener(this);
	}

	@Override
	public void onDestory() {
		// TODO Auto-generated method stub
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
			Handler mainThread = new Handler(Looper.getMainLooper());
			mainThread.post(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					if(loadStatus == Config.LoadThemeStatus.STATUS_SUCCESS){
						if(theme != null){
							getMvpView().updateThemeList(theme);
						}
					}else{
						getMvpView().showTips(loadStatus);
					}
				
				}
			});
		}
	}

	@Override
	public void onThemesLoaded(int loasStatus, List<Theme> themes) {
		// TODO Auto-generated method stub
		
	}



	
	

	
	
}
