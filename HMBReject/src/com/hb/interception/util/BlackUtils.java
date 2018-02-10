package com.hb.interception.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.Contacts.Entity;
import android.text.TextUtils;
import android.util.Log;

public class BlackUtils {
	
	private static final String TAG = "BlackUtils";
	
	public static Uri BLACK_URI = Uri.parse("content://com.hb.contacts/black");
	public static Uri WHITE_URI = Uri.parse("content://com.hb.contacts/white");
	public static Uri MARK_URI = Uri.parse("content://com.hb.contacts/mark");
	public static Uri KEYWORD_URI = Uri.parse("content://com.hb.reject/keyword");
	public static Uri SETTING_URI = Uri.parse("content://com.hb.reject/setting");
	public static final String HB_CONTACT_AUTHORITY = "com.hb.contacts";
	
	public static String getBlackNameByCalllog(Context context,
			String address) {
		Cursor cursor = null;
		cursor = context.getContentResolver().query(Calls.CONTENT_URI,
				null, getPhoneNumberEqualString(address) + " and reject >= 1", null,
				null);
		if (cursor != null) {
			for (int i = 0; i < cursor.getCount(); i++) {
				cursor.moveToPosition(i);
				String name = cursor.getString(cursor
						.getColumnIndex("black_name"));
				cursor.close();
				return name;
			}
			cursor.close();
		}
		return null;
	}
	
	private static Uri uri = BlackUtils.BLACK_URI;
	
	public static String getBlackNameByPhoneNumber(Context context,String address){
		Cursor cursor=null;
		cursor = context.getContentResolver().query(uri, null, getPhoneNumberEqualString(address) , null, null);
		
		if(cursor!=null){
			for(int i = 0; i < cursor.getCount(); i++){
				cursor.moveToPosition(i);
				String name = cursor.getString(cursor.getColumnIndex("black_name"));
				cursor.close();
				return name;
			}
			cursor.close();
		}
		
		return null;
	}
	
	public static String getLableByPhoneNumber(Context context, String address) {
		Cursor cursor = context.getContentResolver().query(uri, null,
				getPhoneNumberEqualString(address) , null, null);
		if (cursor != null) {
			for (int i = 0; i < cursor.getCount(); i++) {
				cursor.moveToPosition(i);
				String lable = cursor.getString(cursor.getColumnIndex("lable"));
				cursor.close();
				return lable;
			}
			cursor.close();
		}

		return null;
	}
	
	public static String getLableByCalllog(Context context,String address){
		Cursor cursor = context.getContentResolver().query(Calls.CONTENT_URI, null, getPhoneNumberEqualString(address) +" and reject >=1", null, null);
		if(cursor!=null){
			for(int i = 0; i < cursor.getCount(); i++){
				cursor.moveToPosition(i);
				String lable = cursor.getString(cursor.getColumnIndex("mark"));
				cursor.close();
				return lable;
			}
			cursor.close();
		}
		
		return null;
	}
	
	public static String getPhoneNumberEqualString(String number) {
        return " PHONE_NUMBERS_EQUAL(number, \"" + number + "\", 0) ";
    }
	
	public static boolean isNumberAlreadyExisted(Context context, String number) {
		Cursor cursor = context.getContentResolver().query(BLACK_URI, null, getPhoneNumberEqualString(number) + " and isblack=1",  null, null);
		if(cursor!=null){
             if(cursor.getCount() > 0) {
				return true;
             }			
			cursor.close();
		}
		
		return false;
	}
	
	
	public static boolean isWhiteNumberAlreadyExisted(Context context, String number) {
		Cursor cursor = context.getContentResolver().query(WHITE_URI, null, getPhoneNumberEqualString(number),  null,  null);
		if(cursor!=null){
             if(cursor.getCount() > 0) {
				return true;
             }			
			cursor.close();
		}
		
		return false;
	}
	
	public static void updateValue(Context context, String name, boolean value) {
			ContentResolver cr = context.getContentResolver();
			ContentValues cv = new ContentValues();
			cv.put("value", value);
			cr.update(SETTING_URI, cv, "name = '" + name + "'" , null);
   }
	
	public static void saveBlackToDb(Context context, String number, String name) {
		ContentResolver cr = context.getContentResolver();
		ContentValues cv = new ContentValues();
		cv.put("isblack", 1);
		cv.put("number", number);
		if(!TextUtils.isEmpty(name)) {
			cv.put("black_name", name);
		}
		cv.put("reject", 3);
		addContactInfo(context, number, cv);

		cr.insert(BLACK_URI, cv);
	}
	
	public static void saveWhiteToDb(Context context, String number, String name) {
		ContentResolver cr = context.getContentResolver();
		ContentValues cv = new ContentValues();
		cv.put("number", number);
		if(!TextUtils.isEmpty(name)) {
			cv.put("white_name", name);
		}
//    	String mark = YuloreUtil.getUserMark(context, number);
//    	int userMark = -1;
//    	Log.d(TAG, "number = " + number + "  mark =  " + mark);
//    	if (mark == null) {
////    		mark = YuloreUtil.getMarkContent(number);
//    		userMark = YuloreUtil.getMarkNumber(context, number);
//    	}
//    	if (null != mark) {
//    		Log.i(TAG, "mark="+mark);
//    		cv.put("lable", mark);
//    		cv.put("user_mark", userMark);
//    	}
		
		addContactInfo(context, number, cv);

		cr.insert(WHITE_URI, cv);
	}
	
	private static void addContactInfo(Context context, String number, final ContentValues cv) {
		Cursor c = null;
		try {
			c = context.getContentResolver().query(Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
					number), new String[] { Entity.RAW_CONTACT_ID,
					Entity.DATA_ID }, null, null, null);
			if (c != null && c.moveToFirst()) {
				// 过滤sim卡联系人 by liyang
				long rawContactId = c.getLong(0);
				if (!ContactUtils.isSimContact(context, rawContactId)) {
					cv.put(Entity.RAW_CONTACT_ID, c.getLong(0));
					cv.put(Entity.DATA_ID, c.getLong(1));
				}
			}
		} catch (Exception e) {
		    e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
				c = null;
			}
		}
	}

}