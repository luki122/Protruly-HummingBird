package com.protruly.music.downloadex.db;

import java.io.Closeable;


import android.database.Cursor;



public class DbUtils {

	private DbUtils() {
    }
	
	public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable e) {
            }
        }
    }

    public static void closeQuietly(Cursor cursor) {
        if (cursor != null) {
            try {
                cursor.close();
                cursor = null;
            } catch (Throwable e) {
            }
        }
    }
	
}
