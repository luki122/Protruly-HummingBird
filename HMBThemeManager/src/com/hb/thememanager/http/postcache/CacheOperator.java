package com.hb.thememanager.http.postcache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.hb.thememanager.utils.Config;

/**
 * Created by alexluo on 17-8-23.
 */

public class CacheOperator {

    private static volatile CacheOperator mOperator;
    private static final String CACHE_PAGE_ZERO = "\"pageNum\":0";
    private static final String CACHE_PAGE_BANNER = "themestore-web-api/banner";
    private static final String CACHE_DETIAL = "Details";
    private DatabaseCacheHelper mHelper;

    private CacheOperator(Context context){
        mHelper = new DatabaseCacheHelper(context.getApplicationContext());
    }
    public static CacheOperator getInstance(Context context) {
        if (mOperator == null) {
            synchronized (CacheOperator.class) {
                if (mOperator == null) {
                    mOperator = new CacheOperator(context);
                }
            }
        }
        return mOperator;
    }

    public synchronized String queryResponse(String url, String requestBody) {
        if (mHelper.getDatabase() != null) {
            Cursor cursor = mHelper.getDatabase().query(DatabaseCacheHelper.CACHE_TABLE,
                    null, DatabaseCacheHelper.URL + "=? and "+DatabaseCacheHelper.REQUEST_BODY
                    + "=?", new String[]{url,requestBody}, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {

                if (cursor.moveToNext()) {
                    String response = cursor.getString(cursor.getColumnIndex(DatabaseCacheHelper.RESPONSE));
                    return response;
                }
            }
            cursor.close();
        }

        return "";
    }

    public synchronized void insertResponse(String url, String requestBody, String response) {
        if(requestBody.contains(CACHE_PAGE_ZERO) ||url.contains(CACHE_PAGE_BANNER)
                || url.contains(CACHE_DETIAL)) {
            int updateResult = updateResponse(url,requestBody,response);
            if(updateResult == 0) {
                ContentValues values = new ContentValues();
                values.put(DatabaseCacheHelper.URL, url);
                values.put(DatabaseCacheHelper.REQUEST_BODY, requestBody);
                values.put(DatabaseCacheHelper.RESPONSE, response);
                mHelper.insert(values);
            }
        }
        mHelper.close();

    }

    synchronized int  updateResponse(String url, String requestBody, String response) {
         ContentValues values = new ContentValues();
         values.put(DatabaseCacheHelper.URL, url);
         values.put(DatabaseCacheHelper.REQUEST_BODY, requestBody);
         values.put(DatabaseCacheHelper.RESPONSE, response);
         return mHelper.update(values, DatabaseCacheHelper.URL + "=? and "+DatabaseCacheHelper.REQUEST_BODY
                 +"=?",
                 new String[]{url,requestBody});

    }

     void deleteResponse(String url, String requestBody) {

    }

     void clearCache(){

    }
}
