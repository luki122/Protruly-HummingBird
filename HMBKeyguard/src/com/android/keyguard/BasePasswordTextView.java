package com.android.keyguard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public  class BasePasswordTextView extends View{

	public BasePasswordTextView(Context context, AttributeSet attrs, int defStyleAttr,int defStyleRes) {
		super(context,attrs,defStyleAttr,defStyleRes);
	}
	
	public interface UserActivityListener {
        void onUserActivity();
    }
	
	public  void reset(boolean animated){
		
	}
	
	public  String getText(){
		return "";
	}
	
	public  void setUserActivityListener(UserActivityListener userActivitiListener){
		
	}
	
	public  void deleteLastChar(){
		
	}
	
	public void append(char c){
		
	}
}
