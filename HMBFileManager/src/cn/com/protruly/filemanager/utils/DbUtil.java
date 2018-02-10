package cn.com.protruly.filemanager.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class DbUtil {
	
	public static final boolean DEBUG = false;
	public static final String TAG = "DbUtil";
	
	public static void debugCursor(Context context, Cursor cursor) {
		if(! DEBUG) {
			return;
		}
		int clmCount = cursor.getColumnCount();
		LogUtil.i(TAG, " ===========================debugCursor BEGIN=================================== ");
		while(cursor.moveToNext()) {
			for(int columnIndex = 0; columnIndex < clmCount; columnIndex ++) {
				String colName = cursor.getColumnName(columnIndex);
				switch(cursor.getType(columnIndex)) {
				case Cursor.FIELD_TYPE_INTEGER:
					int i = cursor.getInt(columnIndex);
					LogUtil.i(TAG, cursor.getColumnName(columnIndex) + " " + i);
					/*
					if(colName.equals("_id")) {
						context.getContentResolver().delete(uri, "_id=?", new String[]{String.valueOf(i)});
					}
					*/
					break;
				case Cursor.FIELD_TYPE_STRING:
					String str = cursor.getString(columnIndex);
					LogUtil.i(TAG, cursor.getColumnName(columnIndex) + ":" + str);
					break;
				case Cursor.FIELD_TYPE_FLOAT:
					float f = cursor.getFloat(columnIndex);
					LogUtil.i(TAG, cursor.getColumnName(columnIndex) + ":" + f);
					break;
				case Cursor.FIELD_TYPE_BLOB:
					LogUtil.i(TAG, cursor.getColumnName(columnIndex) + "blob");
					break;
				case Cursor.FIELD_TYPE_NULL:
					LogUtil.i(TAG, cursor.getColumnName(columnIndex) + "null");
					break;
				}
			}
		}
		LogUtil.i(TAG, " ===========================debugCursor END=================================== ");
	}
	
	
	public static void debugUri(Context context, Uri uri) {
		if(! DEBUG) {
			return;
		}
		Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
		debugCursor(context, cursor);
	}

	public static void closeSilently(Cursor cursor) {
		if(cursor != null) {
			cursor.close();
			cursor = null;
		}
	}
}
