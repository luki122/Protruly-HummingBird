package com.android.settings.widget.keyboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.w3c.dom.Text;

public  class BasePasswordTextView extends TextView {

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
