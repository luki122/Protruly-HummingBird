package com.hb.thememanager.ui.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hb.thememanager.R;

/**
 * 主题包Tab内容页面
 *
 */
public class BaseFragment extends Fragment {
	private static final String TAG = "BaseFragment";
	private CharSequence mTitle;

	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			handleCompleteMessage(msg);
		}
	};

	public BaseFragment(){}


	public CharSequence getTitle(){
		return mTitle;
	}

	public void setTitle(CharSequence title){
		mTitle = title;
	}

	protected void handleCompleteMessage(Message msg){

	}

	protected void sendMessage(int what, Object obj, int... args){
		Message msg = Message.obtain();
		msg.what = what;
		msg.obj = obj;
		if(args != null) {
			if(args.length > 0){
				msg.arg1 = args[0];
				if(args.length > 1){
					msg.arg2 = args[1];
				}
			}
		}
		mHandler.sendMessage(msg);
	}
}
