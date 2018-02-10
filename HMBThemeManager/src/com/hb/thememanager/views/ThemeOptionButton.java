package com.hb.thememanager.views;

import com.hb.thememanager.listener.OnThemeStateChangeListener;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.state.DonwloadOption;
import com.hb.thememanager.state.StateManager;
import com.hb.thememanager.state.ThemeState;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import com.hb.thememanager.R;
/**
 * 该类作为主题操作按钮的父类，这种按钮主要包括了对主题的应用、下载状态、
 * 更新状态的处理
 */
public abstract class ThemeOptionButton extends LinearLayout implements
		OnThemeStateChangeListener{
	protected StateManager mStateManager;
	private Theme mTheme;

	public ThemeOptionButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		initial();
	}

	private void initial(){
		setOrientation(LinearLayout.VERTICAL);
		LayoutInflater.from(getContext()).inflate(getLayoutRes(),this,true);
		mStateManager = StateManager.getInstance(getContext());
		setOnStateChangeListener(this);

	}



	protected abstract int getLayoutRes();


	public Theme getTheme(){
		return mTheme;
	}

	public void setOnStateChangeListener(OnThemeStateChangeListener listener){
		mStateManager.setStateChangeListener(listener);
	}

	public void apply() {
		// TODO Auto-generated method stub
		mStateManager.apply();
	}

	public  void setTheme(Theme theme){
		mTheme = theme;
		mStateManager.setTheme(theme);
	}

	
}
