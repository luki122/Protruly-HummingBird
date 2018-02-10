package com.android.systemui.qs;

import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
/**
 * 
 * @author storktang
 *
 */
public class ViewForQSPanelPullUp extends View{
	private Context mContext;
	private View mDelegateView;
	private PhoneStatusBar mPhoneStatusBar;
    public ViewForQSPanelPullUp(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewForQSPanelPullUp(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }
    
    public void setDelegateView(View view) {
        mDelegateView = view;
    }
    
    public void setBar(PhoneStatusBar bar) {
    	mPhoneStatusBar = bar;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	if(mPhoneStatusBar != null &&mPhoneStatusBar.getBarState() != StatusBarState.SHADE) {
	    	mDelegateView.dispatchTouchEvent(event);
	    	// TODO Auto-generated method stub
	    	return true;
    	} else {
    		return false;
    	}
    }
}
