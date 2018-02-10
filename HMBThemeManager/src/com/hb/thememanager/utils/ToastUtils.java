package com.hb.thememanager.utils;

import android.content.Context;
import android.widget.Toast;

public class ToastUtils {

	
	public static void showShortToast(Context context,String msg){
		Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
	}

	public static void showShortToast(Context context,int stringId){
		showShortToast(context,context.getString(stringId));
	}


	public static void showLongToast(Context context,String msg){
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}
}
