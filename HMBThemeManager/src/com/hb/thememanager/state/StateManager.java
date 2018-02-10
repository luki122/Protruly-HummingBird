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
import com.hb.thememanager.state.ThemeState.State;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.DialogUtils;
import com.hb.thememanager.R;
/**
 * Manage Theme download state,See{@link com.hb.thememanager.state.ThemeState}
 *
 */
public class StateManager implements DonwloadOption{

	private static StateManager mInstance;
	
	private ThemeState mState;
	private ThemeManager mThemeManager;
	private WeakReference<ThemeManagerApplication> mContext;
	private Theme mTheme;
	private OnThemeStateChangeListener mStateListener;
	private DialogUtils mDialogUtils;
	private DonwloadOption mOptionButton;
	private boolean mFromSetupApp;
	private StateManager(Context context){
		mContext = new WeakReference<ThemeManagerApplication>((ThemeManagerApplication)context.getApplicationContext());
		mThemeManager = mContext.get().getThemeManager();
		mDialogUtils = new DialogUtils();
	}
	public static StateManager getInstance(Context context){
		synchronized (DownloadButtonNormalState.class) {
			if(mInstance == null){
				mInstance = new StateManager(context);
			}
			return mInstance;
		}
	}
	
	public void setOption(DonwloadOption option){
		mOptionButton = option;
	}

	public void setFromSetupApp(boolean fromsetupApp){
		mFromSetupApp = fromsetupApp;
	}

	public boolean isFromSetupApp(){
		return  mFromSetupApp;
	}

	
	public synchronized void setState(ThemeState state){
		mState = state;
	}
	
	public synchronized ThemeState getState(){
		return mState;
	}
	
	public void handleState(){
		if(mState != null){
			mState.handleState();
		}
	}



	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void pause() {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void resume() {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void apply() {
		// TODO Auto-generated method stub
		mThemeManager.applyTheme(mTheme, mContext.get(),this);
	}



	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}

	public void postState(final State state){
		if(mStateListener != null){
			
			if(Looper.myLooper() != Looper.getMainLooper()){
				Handler mainThread = new Handler(Looper.getMainLooper());
				mainThread.post(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						mStateListener.onStateChange(state);
					}
				});
			}else{
				mStateListener.onStateChange(state);
			}
			
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
	
	
	
	
	
	
}
