package com.hb.tms;
import com.android.incallui.InCallApp;

import android.util.Log;
import android.text.TextUtils;
import android.app.hb.TMSManager;

public class AreaManager {
	
	public static final String TAG = "AreaManager";
	
	public static String getArea(String number) {
//		// get location of the input phone number 
//		// 获取用户输入的号码的归属地
		if(!TextUtils.isEmpty(number)) {
			number = number.replaceAll(" ", "");
		}
		String location = TmsServiceManager.getInstance().getArea(number);
		return location;
//		TMSManager mTMSManager = (TMSManager)InCallApp.getInstance().getSystemService(TMSManager.TMS_SERVICE);
//		try {
//			return mTMSManager.getLocation(number);
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
//		return "";
	}
}
