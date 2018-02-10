package com.hb.thememanager.state;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.hb.thememanager.ThemeManager;
import com.hb.thememanager.ThemeManagerApplication;
import com.hb.thememanager.ThemeManagerImpl;
import com.hb.thememanager.listener.OnThemeStateChangeListener;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.DialogUtils;
import com.hb.thememanager.R;
/**
 * Manage Theme download state,See{@link com.hb.thememanager.state.ThemeState}
 *
 */
public class StateManager implements Runnable{

	private static StateManager mInstance;
	
	private ThemeState mState;
	private ThemeManager mThemeManager;
	private WeakReference<ThemeManagerApplication> mContext;
	private Theme mTheme;
	private OnThemeStateChangeListener mStateListener;
	private OnThemeStateChangeListener mTryStateListener;

	private Handler mMainThread;
	private boolean mFromSetupApp;
	private boolean mFromTryTheme;
	private StateManager(Context context){
		mContext = new WeakReference<ThemeManagerApplication>((ThemeManagerApplication)context.getApplicationContext());
		mThemeManager = mContext.get().getThemeManager();
	}
	public static StateManager getInstance(Context context){
		synchronized (StateManager.class) {
			if(mInstance == null){
				mInstance = new StateManager(context.getApplicationContext());
			}
			return mInstance;
		}
	}


	public void setFromSetupApp(boolean fromsetupApp){
		mFromSetupApp = fromsetupApp;
	}

	public boolean isFromSetupApp(){
		return  mFromSetupApp;
	}

	public void setFromTry(boolean fromTry){
		mFromTryTheme = fromTry;
	}

	public boolean isFromTry( ){
		return mFromTryTheme;
	}

	
	public synchronized void setState(ThemeState state){
		mState = state;
	}
	
	public synchronized ThemeState getState(){
		return mState;
	}
	




	public void apply() {
		// TODO Auto-generated method stub
		mThemeManager.applyTheme(mTheme, mContext.get(),this);
	}




	public void postState(final ThemeState state){
			setState(state);
			if(Looper.myLooper() != Looper.getMainLooper()){

				mMainThread = new Handler(Looper.getMainLooper());
				mMainThread.post(this);
			}else{
				run();
			}
			
	}

	@Override
	public void run() {

		if(mTryStateListener != null && mFromTryTheme){
			mTryStateListener.onStateChange(getState());
		}

		if(mStateListener != null) {
			mStateListener.onStateChange(getState());
		}


	}



	public void setTheme(Theme theme) {
		// TODO Auto-generated method stub
		mTheme = theme;
	}
	
	public void setStateChangeListener(OnThemeStateChangeListener listener) {
		// TODO Auto-generated method stub
		mStateListener = listener;
	}
	
	
	public void setTryStateChangeListener(OnThemeStateChangeListener listener){
		mTryStateListener = listener;
	}
	
	
	
}
