package com.hb.interception;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public class ManageReject{
	
	Context mContext;
	SettingObserver mSettingObserver;
	private static Uri SETTING_URI = Uri.parse("content://com.hb.reject/setting");
	public ManageReject(Context context) {
		mContext = context;
		mSettingObserver = new SettingObserver();
		getHbSettingValue(mContext);
		context.getContentResolver().registerContentObserver(SETTING_URI, true, mSettingObserver);		
	}
	
	public class SettingObserver extends ContentObserver {
		public static final String TAG = "SettingObserver";

		public SettingObserver() {
			super(new Handler());
		}

		@Override
		public void onChange(boolean selfChange) {
			Log.i(TAG, "onChange :");
			super.onChange(selfChange);
			getHbSettingValue(mContext);		
		}
	}
	
    public static boolean sIsBlack = true, sIsSmart = false;
    private void getHbSettingValue(Context context) {
     	Cursor c = context.getContentResolver().query(SETTING_URI, null, null, null, null);
		if (c != null) {
			while (c.moveToNext()) {
				String name = c.getString(c.getColumnIndex("name"));
				boolean value = c.getInt(c.getColumnIndex("value")) > 0;
		       if (name.equalsIgnoreCase("call_black")) {
		    	   sIsBlack = value;
				} else if (name.equalsIgnoreCase("call_smart")) {
					sIsSmart = value;
				}
			}
			c.close();
		}
    }
}