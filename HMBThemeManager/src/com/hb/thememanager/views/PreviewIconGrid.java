package com.hb.thememanager.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.GridView;

public class PreviewIconGrid extends GridView {

	public PreviewIconGrid(Context context, AttributeSet attrs,
			int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		// TODO Auto-generated constructor stub
	}

	public PreviewIconGrid(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		// TODO Auto-generated constructor stub
	}

	public PreviewIconGrid(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public PreviewIconGrid(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	
	
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent arg0) {
		//返回false表示不对触摸事件进行分发，否则的话会影响其底部
		//ViewPager的滑动事件。
		return false;
	}
	
	
}
