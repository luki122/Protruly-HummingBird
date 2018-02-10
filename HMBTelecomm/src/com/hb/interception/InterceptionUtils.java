package com.hb.interception;
  
import java.io.ByteArrayOutputStream;  
import java.io.IOException;  
import java.io.InputStream;   
import java.net.HttpURLConnection;  
import java.net.URL; 
import java.util.HashMap;
import java.util.Map;

import com.hb.tms.MarkManager;
import com.hmb.manager.aidl.MarkResult;
import com.hb.utils.ContactsUtils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
// Aurora xuyong 2015-08-29 modified for bug #15926 start
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
// Aurora xuyong 2015-08-29 modified for bug #15926 end
import android.util.Log;
import android.os.Bundle;
import android.os.SystemProperties;
  
public class InterceptionUtils {  
    private static final String TAG = "InterceptionUtils";
    
    private static Map<String, Boolean> mCache =  new HashMap<String, Boolean>();

    private static Uri black_uri = Uri.parse("content://com.hb.contacts/black");
    private static Uri black_quick_uri = Uri.parse("content://com.hb.contacts/quickblack");
    private static Uri white_uri = Uri.parse("content://com.hb.contacts/white");

    public static final int MODE_BLACK = 1;
    public static final int MODE_SMART= 2;    
    
    private static final String[] BLACK_PROJECTION = new String[] {
    	"_id",   //唯一标示，递增
    	"isblack",   // 标记黑白名单（0: 白名单/1:黑名单）
    	"lable",    //通话记录表中获取的标记String, 或添加黑名单时直接获取的标记
    	"black_name",  // 黑名单中的名字
    	"number", //号码
    	"reject" //标示是否拦截通话，短信（0：不拦截/ 1：拦截通话/2:拦截短信/3同时拦截通话、短信）
    };        
    
    private static final int IS_WHITE = 0;
    private static final int IS_BLACK = 1;
    private static final int IS_IGNORE = 2;
    
    public static boolean isNeedRejectQuick(Context context, String number) {
		Log.v(TAG, "isNeedRejectQuick number = " + number);
		
		mRejectName = "";
		mRejectMode = 0;
		mMarkCount = -2;
		
		if(!isSupportBlack() || TextUtils.isEmpty(number)) {
			return false;
		}
		if (ManageReject.sIsBlack) {
			Bundle b = context.getContentResolver().call(black_quick_uri, "isRejectQuick", number, null);
			int result = b.getInt("result");

			if (result == IS_WHITE) {
				return false;
			} else if (result == IS_BLACK) {
				return true;
			}
		} else {
			if(isWhiteNumber(context, number)) {
				return false;
			}
		}
    	return isSmartRejectNumber(context, number);    
    }
    
    public static boolean isNeedReject(Context context, String number) {
		Log.v(TAG, "isNeedReject number = " + number);
		
		mRejectName = "";
		mRejectMode = 0;
		mMarkCount = -2;
		
		if(!isSupportBlack() || TextUtils.isEmpty(number)) {
			return false;
		}
		
		if(isWhiteNumber(context, number)) {
			return false;
		}
		
		if(isBlackNumber(context, number)) {
			return true;
		}

    	return isSmartRejectNumber(context, number);    
    }   
    
    private static boolean isBlackNumber(Context context, String number) {
		if(ManageReject.sIsBlack) {
			Cursor cursor = context.getContentResolver().query(black_uri, BLACK_PROJECTION,
			"PHONE_NUMBERS_EQUAL(number, " + number + ", 0)", null, null);
			Log.v(TAG, " isBlackNumber cursor = " + cursor);
			try {
				if (cursor != null && cursor.getCount() > 0) {
					cursor.moveToFirst();
					mRejectName = cursor.getString(3);
					mRejectMode = MODE_BLACK;
					return true;
				}
			} finally {
				if(cursor != null) {
					cursor.close();
				}
			}
		}
		return false;
    }
    
    private static boolean isWhiteNumber(Context context, String number) {

		if (TextUtils.isEmpty(number)) {
			return false;
		}

		Cursor cursor = context.getContentResolver().query(white_uri, null,
				"PHONE_NUMBERS_EQUAL(number, " + number + ", 0)", null, null);
		try {
			if (cursor != null && cursor.getCount() > 0) {
				Log.v(TAG, "isWhiteNumber = true");
				return true;
			}
			return false;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}    
      }
    
    private static boolean isSmartRejectNumber(Context context, String number) {
    	Uri contactUri = null;
		String userMark = MarkManager.getUserMark(context, number);
		if (!TextUtils.isEmpty(userMark)) {
			if (isSmartReject(userMark)) {
				contactUri = ContactsUtils.queryContactUriByPhoneNumber(
						context, number);
				if (contactUri != null) {
					return false;
				}
				mRejectName = userMark;
				mMarkCount = -1;
				mRejectMode = MODE_SMART;
				return true;
			}
		}
		
    	if(ManageReject.sIsSmart) {
        	MarkResult mark = MarkManager.getMark(16, number);
//        	MarkResult mark = MarkManager.getMark(16, "18665343459"); 
        	if(mark != null) {
            	String tagName = MarkManager.getTagName(mark.tagType);
        		if(isSmartNetReject(tagName, mark.getTagCount())) {
        			if(contactUri == null) {
        				contactUri  = ContactsUtils.queryContactUriByPhoneNumber(context, number);
        				if(contactUri != null) {
        					return false;
        				}
        			}
            		Log.v(TAG, "isSmartRejectNumber true " ); 
            		mRejectName = mark.getName();
            		mMarkCount = mark.getTagCount();
            		mRejectMode = MODE_SMART;
            		return true;	
        		}
        	}
    	}
    	return false;
    } 
    
//    TAG为“ 骚扰电话”、 “房产中介” 、“保险理财” 、“广告推销” 、“诈骗电话” 、“快递送餐” 、“出租车”
    private static boolean isSmartReject(String tagName) {
    	//String[] keywords = {"骚扰", "诈骗"};
    	String[] keywords = {"诈骗"};
    	for(String word : keywords) {
    		if(tagName.contains(word)) {
    			return true;
    		}
    	}
    	return false;
    } 
    
    private static boolean isSmartNetReject(String tagName, int count) {
    	String[] keywords = {"骚扰", "诈骗"};
    	if(tagName.contains("骚扰") && count >= 100) {
    		return true;
    	} else if(tagName.contains("诈骗") && count >= 50) {
    		return true;
    	}
    	return false;
    } 
    
    private static int mRejectMode;
    public static int getLastRejectMode() {
    	return mRejectMode;
    }
    
    private static String mRejectName;
    public static String getLastRejectName() {
    	return mRejectName;
    }
    
    private static int mMarkCount;
    public static int getLastMarkCount() {
    	return mMarkCount;
    }
    
//    public static boolean isToAddBlack(Context context, String number) {
// 		 Log.v("isToAddBlack", " number = " + number);
//    	    	
//		if(!isSupportBlack() || TextUtils.isEmpty(number)) {
//			return false;
//		}		
//		
//    	 String[] projection = {CallLog.Calls.NUMBER};
//    	 Uri uri = Uri.withAppendedPath(CallLog.Calls.CONTENT_FILTER_URI, Uri.encode(number));
//    	 Cursor cursor = context.getContentResolver().query(uri, projection,
//                 CallLog.Calls.TYPE + "='" + CallLog.Calls.INCOMING_TYPE + "' AND " + CallLog.Calls.DURATION + "='0' AND " + CallLog.Calls.DATE + " > '" + (System.currentTimeMillis() - 24 * 3600 * 1000 ) + "'"  , null, null);
//    	 if(cursor != null) {
//    		 Log.v("isToAddBlack", " cursor = " + cursor.getCount());
//    	 }
//		try {
//	    	if (cursor != null && cursor.getCount() >= 3) {
//	    		if(!isBlackNumber(context, number)) {
//	    			Log.v("isToAddBlack", " number = " + number);
//	    		    return true;
//	    		}
//	    	}
//	    	return false;
//		} finally {
//			if(cursor != null) {
//				cursor.close();
//			}
//		}
//    }       
        
    
    public static boolean isSupportBlack() {
        return true;
    }   
    

    
    public static void reset() { 
    	Log.v(TAG, "reset "); 
    	mCache.clear();
	}

}  