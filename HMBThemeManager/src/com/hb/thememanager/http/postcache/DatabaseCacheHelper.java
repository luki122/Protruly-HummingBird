package com.hb.thememanager.http.postcache;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by alexluo on 17-8-23.
 */

public class DatabaseCacheHelper extends SQLiteOpenHelper{


    private static final String DB_NAME = "theme_cache.db";
    private static final int DB_VERSION = 1;
    static final String CACHE_TABLE = "theme_cache";
    static final String URL = "url";
    static final String REQUEST_BODY = "request_body";
        static final String RESPONSE = "response";
    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS "
            + CACHE_TABLE + " ("
            +URL+" TEXT,"
            + REQUEST_BODY+" TEXT,"
            +RESPONSE+" TEXT);";

    private SQLiteDatabase mDb;
    public DatabaseCacheHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + CACHE_TABLE;
        db.execSQL(sql);
        onCreate(db);
    }


    public long insert(ContentValues values){
        long result = getDatabase().insert(CACHE_TABLE, null, values);
        return result;
    }

    protected int delete(String whereClause, String[] whereArgs){
        int result = getDatabase().delete(CACHE_TABLE, whereClause, whereArgs);
        return result;
    }

    protected int update(ContentValues values, String whereClause, String[]
            whereArgs){

        int result = getDatabase().update(CACHE_TABLE, values, whereClause, whereArgs);
        return result;
    }

    protected SQLiteDatabase getDatabase(){
        open();
        return mDb;
    }

    public void close() {
        // TODO Auto-generated method stub
        if (mDb != null && mDb.isOpen()) {
            mDb.close();
        }
    }

    protected void open() {
        // TODO Auto-generated method stub
        if(mDb == null || !mDb.isOpen()){
            mDb = getWritableDatabase();
        }

    }

}
