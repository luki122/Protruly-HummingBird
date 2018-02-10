//add by liyang 2017-4-22
package com.android.dialer.list;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import hb.widget.ViewPager;
import hb.widget.ViewPager.OnPageChangeListener;

public class HbViewPager extends ViewPager {  

	private static final String TAG = "HbViewPager";
	private boolean scrollble = true;  

	public HbViewPager(Context context) {  
		super(context);  
	}  

	public HbViewPager(Context context, AttributeSet attrs) {  
		super(context, attrs);  
	}

	@Override
	public boolean onTouchEvent(MotionEvent arg0) {
		if (!scrollble) {
			return false;
		}
		return super.onTouchEvent(arg0);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent arg0) {
		if (!scrollble) {
			return false;
		}
		return super.onInterceptTouchEvent(arg0);
	}



	public boolean isScrollble() {  
		return scrollble;  
	}  

	public void setScrollble(boolean scrollble) {
		Log.d(TAG, "setScrollble:"+scrollble);
		this.scrollble = scrollble;  
	}  
}