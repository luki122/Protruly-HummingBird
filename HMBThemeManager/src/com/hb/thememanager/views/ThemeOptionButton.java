package com.hb.thememanager.views;

import com.hb.thememanager.listener.OnThemeStateChangeListener;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.state.DonwloadOption;
import com.hb.thememanager.state.StateManager;
import com.hb.thememanager.state.DownloadButtonNormalState;
import com.hb.thememanager.state.ThemeState;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * 该类作为主题操作按钮的父类，这种按钮主要包括了对主题的应用、下载状态、
 * 更新状态的处理
 */
public abstract class ThemeOptionButton extends Button  implements DonwloadOption{

	private ThemeState STATE_NORMAL;
	private ThemeState STATE_DOWLOADING;
	private ThemeState STATE_PAUSE;
	private ThemeState STATE_RESUME;
	private ThemeState STATE_APPLIED;
	private StateManager mStateManager;
	
	public ThemeOptionButton(Context context, AttributeSet attrs,
			int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		// TODO Auto-generated constructor stub
		initial();
	}

	public ThemeOptionButton(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
		initial();
	}

	public ThemeOptionButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		initial();
	}

	public ThemeOptionButton(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		initial();
	}
	
	private void initial(){
		mStateManager = StateManager.getInstance(getContext());
		STATE_NORMAL = new DownloadButtonNormalState(this);
		mStateManager.setState(STATE_NORMAL);
		mStateManager.setFromSetupApp(false);
		mStateManager.setOption(this);
		mStateManager.handleState();
		
	}
	
	public void setOnStateChangeListener(OnThemeStateChangeListener listener){
		mStateManager.setStateChangeListener(listener);
	}
	@Override
	public void start() {
		// TODO Auto-generated method stub
		mStateManager.start();
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		mStateManager.pause();
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		mStateManager.stop();
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
		mStateManager.resume();
	}

	@Override
	public void apply() {
		// TODO Auto-generated method stub
		mStateManager.apply();
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
		mStateManager.update();
	}
	
	public  void setTheme(Theme theme){
		mStateManager.setTheme(theme);
	}

	
}
