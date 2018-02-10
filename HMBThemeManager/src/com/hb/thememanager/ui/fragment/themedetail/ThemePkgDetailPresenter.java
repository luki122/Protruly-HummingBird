package com.hb.thememanager.ui.fragment.themedetail;

import java.io.File;

import android.content.Context;

import com.hb.thememanager.BasePresenter;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;

public class ThemePkgDetailPresenter extends BasePresenter<ThemePkgDetailMVPView> {

	private static final String TAG = "ThemeDetail";
	
	private Theme mCurrentTheme;
	
	private Context mContext;
	
	private String mPreviewDir;
	
	public ThemePkgDetailPresenter(Context context,Theme theme){
		mContext = context;
		mCurrentTheme = theme;
		initPreview();
	}
	
	private void initPreview(){
		StringBuilder builder = new StringBuilder();
		builder.append(mCurrentTheme.loadedPath);
		builder.append(File.separatorChar);
		builder.append(Config.LOCAL_THEME_PREVIEW_DIR_NAME);
		mPreviewDir = builder.toString();
	}
	
	public void loadThemePreview(){
		mCurrentTheme.previewArrays.clear();
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
		detachView();
		
	}
	
	
}
