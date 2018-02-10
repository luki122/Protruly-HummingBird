package com.hb.thememanager.ui;

import java.io.File;

import hb.app.HbActivity;
import android.Manifest;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.app.FragmentManager;

import com.hb.imageloader.HbImageLoader;
import com.hb.thememanager.R;
import com.hb.thememanager.ThemeManagerApplication;
import com.hb.thememanager.listener.FragmentKeyListener;
import com.hb.thememanager.listener.OnKeyPressListener;
import com.hb.thememanager.ui.fragment.AbsThemeFragment;
import com.hb.thememanager.ui.fragment.DirectoryFragment;
import com.hb.thememanager.ui.fragment.WallpaperEntryFragment;
import com.hb.thememanager.ui.fragment.themelist.LocalThemeListFragment;
import com.hb.thememanager.ui.fragment.themelist.LocalLockScreenWallpaperFragment;
import com.hb.thememanager.ui.fragment.themelist.LocalDesktopWallpaperListFragment;
import com.hb.thememanager.ui.fragment.themelist.VrWallpaperFragment;
import com.hb.thememanager.ui.fragment.themedetail.ThemePkgDetailFragment;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.FileUtils;
import com.hb.thememanager.utils.FragmentUtils;
import com.hb.thememanager.utils.TLog;
public class MainActivity extends HbActivity implements FragmentManager.OnBackStackChangedListener{
	
	 /**
     * When starting this activity, the invoking Intent can contain this extra
     * string to specify which fragment should be initially displayed.
     * <p/>Starting from Key Lime Pie, when this argument is passed in, the activity
     * will call isValidFragment() to confirm that the fragment class name is valid for this
     * activity.
     */
    public static final String EXTRA_SHOW_FRAGMENT = ":theme:show_fragment";

    /**
     * When starting this activity and using {@link #EXTRA_SHOW_FRAGMENT},
     * this extra can also be specified to supply a Bundle of arguments to pass
     * to that fragment when it is instantiated during the initial creation
     * of the activity.
     */
    public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":theme:show_fragment_args";

    /**
     * Fragment "key" argument passed thru {@link #EXTRA_SHOW_FRAGMENT_ARGUMENTS}
     */
    public static final String EXTRA_FRAGMENT_ARG_KEY = ":theme:fragment_args_key";

	public static final String EXTRA_FROM_SETUPAPP_ARG_KEY = ":theme:from_setupapp";
    
    /**
     * The package name used to resolve the title resource id.
     */
    public static final String EXTRA_SHOW_FRAGMENT_TITLE_RES_PACKAGE_NAME =
            ":theme:show_fragment_title_res_package_name";
    public static final String EXTRA_SHOW_FRAGMENT_TITLE_RESID =
            ":theme:show_fragment_title_resid";
    
    /**
     * Fragment "key" argument to switch to target,always used by third part application.
     */
    private static final String EXTRA_THIRD_PART_FRAGMENT_ARGUMENTS = "theme_component";

    /**
     * When starting this activity and using {@link #EXTRA_SHOW_FRAGMENT},
     * those extra can also be specify to supply the title or title res id to be shown for
     * that fragment.
     */
    public static final String EXTRA_SHOW_FRAGMENT_TITLE = ":me:show_fragment_title";

    private FragmentKeyListener mFragmentKeyPressListener;

	private boolean mClearImageCache = true;

	public static final String[] ENTRY_FRAGMENTS = {
		LocalThemeListFragment.class.getName(),
		ThemePkgDetailFragment.class.getName(),
		LocalDesktopWallpaperListFragment.class.getName(),
		LocalLockScreenWallpaperFragment.class.getName(),
		VrWallpaperFragment.class.getName(),
	};
	
	private static final int PERMISSION_REQUEST_CODE = 100;
	
	private CharSequence mInitialTitle;
	private int mInitialTitleResId;
	 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestStoragePermission();
		final Intent intent = getIntent();
	  switchPage(intent);
		
	}
	
	private void switchPage(Intent intent){
		// Getting Intent properties can only be done after the super.onCreate(...)
        final String initialFragmentName = intent.getStringExtra(EXTRA_SHOW_FRAGMENT);


        final ComponentName cn = intent.getComponent();
        getFragmentManager().addOnBackStackChangedListener(this);
        setTitleFromIntent(intent);
        Bundle initialArguments = intent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        String targetComponent = intent.getStringExtra(EXTRA_THIRD_PART_FRAGMENT_ARGUMENTS);
        if(!TextUtils.isEmpty(targetComponent)){
        	if(targetComponent.equals(Config.ThemeComponent.WALLPAPER)){
        		switchToFragment(WallpaperEntryFragment.class.getName(), null, false, false, R.string.theme_wallpaper_title, null, false);
        	}else if(targetComponent.equals(Config.ThemeComponent.THEME)){
        		switchToFragment(LocalThemeListFragment.class.getName(), null, false, false, R.string.local_theme, null, false);
        	}
        }else{
        	if(TextUtils.isEmpty(initialFragmentName)){
        		switchToFragment(LocalThemeListFragment.class.getName(), null, false, false, R.string.local_theme, null, false);
        	}else{
        		switchToFragment(initialFragmentName, initialArguments, true, false,
                        mInitialTitleResId, mInitialTitle, false);
        	}
        	
        }
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		switchPage(intent);
	}
	

	 private void setTitleFromIntent(Intent intent) {
	        final int initialTitleResId = intent.getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, -1);
	        if (initialTitleResId > 0) {
	            mInitialTitle = null;
	            mInitialTitleResId = initialTitleResId;

	            final String initialTitleResPackageName = intent.getStringExtra(
	                    EXTRA_SHOW_FRAGMENT_TITLE_RES_PACKAGE_NAME);
	            if (initialTitleResPackageName != null) {
	                try {
	                    Context authContext = createPackageContextAsUser(initialTitleResPackageName,
	                            0 /* flags */, new UserHandle(UserHandle.myUserId()));
	                    mInitialTitle = authContext.getResources().getText(mInitialTitleResId);
	                    setTitle(mInitialTitle);
	                    mInitialTitleResId = -1;
	                    return;
	                } catch (NameNotFoundException e) {
	                	
	                }
	            } else {
	                setTitle(mInitialTitleResId);
	            }
	        } else {
	            mInitialTitleResId = -1;
	            final String initialTitle = intent.getStringExtra(EXTRA_SHOW_FRAGMENT_TITLE);
	            mInitialTitle = (initialTitle != null) ? initialTitle : "";
	            setTitle(mInitialTitle);
	        }
	    }
	 
	 @Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}
	
	private void requestStoragePermission() {
		checkStoragePermission();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
			String[] permissions, int[] grantResults) {
		// TODO Auto-generated method stub
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		createThemeDirs();
	}
	
	private void createThemeDirs(){
		FileUtils.createDirectory(Config.LOCAL_THEME_PATH);
		FileUtils.createDirectory(Config.LOCAL_THEME_PACKAGE_PATH);
		FileUtils.createDirectory(Config.LOCAL_THEME_RINGTONG_PATH);
		FileUtils.createDirectory(Config.LOCAL_THEME_WALLPAPER_PATH);
		FileUtils.createDirectory(Config.LOCAL_THEME_FONTS_PATH);
		
	}

	private void checkStoragePermission() {
		int permission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) & 
    			checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    	if(permission != PackageManager.PERMISSION_GRANTED){
    		requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE}
    		, PERMISSION_REQUEST_CODE);
    	}
	}


	public void startThemePanel(String fragmentClass, Bundle extras,boolean 
			 addToBackStack,int titleRes,  Fragment caller, int requestCode) {
		// TODO Auto-generated method stub
		startThemePanel(fragmentClass, extras, addToBackStack, getResources().getString(titleRes), caller,  requestCode);
	}

	public void startThemePanel(String fragmentClass, Bundle extras,boolean 
			 addToBackStack,CharSequence title,  Fragment caller, int requestCode) {
		// TODO Auto-generated method stub
		startThemePanel(fragmentClass, extras, addToBackStack, 0, title, null, requestCode);
	}
	
	@Override
	public void onNavigationClicked(View view) {
		// TODO Auto-generated method stub
		super.onNavigationClicked(view);
		onBackPressed();
	}
	
	


    public boolean isValidFragment(String fragmentName) {
        // Almost all fragments are wrapped in this,
        // except for a few that have their own activities.
        for (int i = 0; i < ENTRY_FRAGMENTS.length; i++) {
            if (ENTRY_FRAGMENTS[i].equals(fragmentName)) return true;
        }
        return false;
    }
    

   
    public void startThemePanel(String fragmentClass, Bundle args, boolean addToBackStack,int titleRes,
            CharSequence titleText, Fragment resultTo, int resultRequestCode) {
        FragmentUtils.startWithFragment(this, fragmentClass, args, resultTo, resultRequestCode,
                titleRes, titleText, false);
    }
    
    
    public Fragment switchToFragment(String fragmentName, Bundle args, boolean validate,
            boolean addToBackStack, int titleResId, CharSequence title, boolean withTransition) {
     return switchToFragment(fragmentName, com.hb.R.id.content, args, validate, addToBackStack, titleResId, title, withTransition);
    }
    
    public void setFragmentKeyListener(FragmentKeyListener listener){
    	mFragmentKeyPressListener = listener;
    }
    
    @Override
    public void onBackPressed() {
    	// TODO Auto-generated method stub
		try {
			if (mFragmentKeyPressListener != null) {
				if (mFragmentKeyPressListener.isEditMode()) {
					mFragmentKeyPressListener.onBackPressed();
					return;
				}
			}
			super.onBackPressed();
		}catch (Exception e){
			//do nothing
		}
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
    	// TODO Auto-generated method stub
    	if(mFragmentKeyPressListener != null){
    		if(mFragmentKeyPressListener.onKeyUp(keyCode,event)){
    			return true;
    		}
    	}
    	return super.onKeyUp(keyCode, event);
    }


	@Override
	protected void onDestroy() {
//		if(mClearImageCache){
//			clearImageCache();
//		}
		super.onDestroy();
	}

	public void clearImageCache(){
		HbImageLoader imageloader = HbImageLoader.getInstance(this);
		imageloader.clearMemoryCache();
	}

	protected void clearImageCacheIfNeeded(boolean clear){
		mClearImageCache = clear;
	}

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	// TODO Auto-generated method stub
    	if(mFragmentKeyPressListener != null){
    		if(mFragmentKeyPressListener.onKeyDown(keyCode,event)){
    			return true;
    		}
    	}
    	
    	return super.onKeyDown(keyCode, event);
    }
    
    public Fragment switchToFragment(String fragmentName, int contentViewId,Bundle args, boolean validate,
            boolean addToBackStack, int titleResId, CharSequence title, boolean withTransition) {
        if (validate && !isValidFragment(fragmentName)) {
            throw new IllegalArgumentException("Invalid fragment for this activity: "
                    + fragmentName);
        }
        setFragmentKeyListener(null);
        AbsThemeFragment f = (AbsThemeFragment) Fragment.instantiate(this, fragmentName, args);
        setFragmentKeyListener(f);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(contentViewId, f);
        ViewGroup content = (ViewGroup)findViewById(contentViewId);
        if (withTransition) {
            TransitionManager.beginDelayedTransition(content);
        }
        if (addToBackStack) {
            transaction.addToBackStack(":theme_manager:prefs");
        }
        if (titleResId > 0) {
        	setTitle(titleResId);
        } else if (title != null) {
        	setTitle(title);
        }
        transaction.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
        return f;
    }

	@Override
	public void onBackStackChanged() {
		// TODO Auto-generated method stub
		
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
