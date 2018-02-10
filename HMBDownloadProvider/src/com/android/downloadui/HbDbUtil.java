package com.android.downloadui;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
/**
 * @author wxue
 */
public class HbDbUtil {
	
	public static final boolean DEBUG = true;
	public static final String TAG = "DbUtil";
	
	public static void debugCursor(Context context, Cursor cursor) {
		if(! DEBUG) {
			return;
		}
		int clmCount = cursor.getColumnCount();
		Log.i(TAG, " ===========================debugCursor BEGIN====================cursor.getCount() = " + cursor.getCount());
		while(cursor.moveToNext()) {
			for(int columnIndex = 0; columnIndex < clmCount; columnIndex ++) {
				String colName = cursor.getColumnName(columnIndex);
				switch(cursor.getType(columnIndex)) {
				case Cursor.FIELD_TYPE_INTEGER:
					int i = cursor.getInt(columnIndex);
					Log.i(TAG, cursor.getColumnName(columnIndex) + " " + i);
					/*
					if(colName.equals("_id")) {
						context.getContentResolver().delete(uri, "_id=?", new String[]{String.valueOf(i)});
					}
					*/
					
					break;
				case Cursor.FIELD_TYPE_STRING:
					String str = cursor.getString(columnIndex);
					Log.i(TAG, cursor.getColumnName(columnIndex) + ":" + str);
					break;
				case Cursor.FIELD_TYPE_FLOAT:
					float f = cursor.getFloat(columnIndex);
					Log.i(TAG, cursor.getColumnName(columnIndex) + ":" + f);
					break;
				case Cursor.FIELD_TYPE_BLOB:
					Log.i(TAG, cursor.getColumnName(columnIndex) + "blob");
					break;
				case Cursor.FIELD_TYPE_NULL:
					Log.i(TAG, cursor.getColumnName(columnIndex) + "null");
					break;
				}
			}
		}
		cursor.moveToPosition(-1);
		Log.i(TAG, " ===========================debugCursor END=================================== ");
	}
	
	
	public static void debugUri(Context context, Uri uri) {
		if(! DEBUG) {
			return;
		}
		Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
		debugCursor(context, cursor);
	}
	
	public static String getCursorString(Cursor cursor, String columnName) {
        final int index = cursor.getColumnIndex(columnName);
        return (index != -1) ? cursor.getString(index) : null;
    }

    /**
     * Missing or null values are returned as -1.
     */
    public static long getCursorLong(Cursor cursor, String columnName) {
        final int index = cursor.getColumnIndex(columnName);
        if (index == -1) return -1;
        final String value = cursor.getString(index);
        if (value == null) return -1;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Missing or null values are returned as 0.
     */
    public static int getCursorInt(Cursor cursor, String columnName) {
        final int index = cursor.getColumnIndex(columnName);
        return (index != -1) ? cursor.getInt(index) : 0;
    }
}
