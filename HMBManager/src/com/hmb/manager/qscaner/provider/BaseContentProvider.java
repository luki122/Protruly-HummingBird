package com.hmb.manager.qscaner.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Base class for QScanner provider. A QScanner provider offers read and write
 * access to database files.
 */
public abstract class BaseContentProvider extends ContentProvider {
    /**
     * SQLiteOpenHelper
     */
    private DbHelper mDbHelper;

    /**
     * Get the current ContentProvider URI
     * @return ContentProvider URI
     */
    public abstract Uri getContentUri();

    /**
     * Get the name of the table for the current operation
     * @return table name for the current operation
     */
    public abstract String getTableName();

    @Override
    public boolean onCreate() {
        mDbHelper = new DbHelper(getContext());
        return mDbHelper != null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection,
                        String selection, String[] selectionArgs, String sortOrder) {
        synchronized (getContentUri()) {
            SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
            SQLiteDatabase db = mDbHelper.getReadableDatabase();
            qb.setTables(getTableName());
            return qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        }
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        synchronized (getContentUri()) {
            if (uri == null || values == null || mDbHelper == null) {
                return null;
            }

            Uri rowUri = null;
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            if(db.isOpen()){
                long rowId = db.insert(getTableName(), null, values);
                if (rowId > 0) {
                    rowUri = ContentUris.appendId(getContentUri().buildUpon(), rowId).build();
                }
                db.close();
            }
            return rowUri;
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        synchronized (getContentUri()) {
            if(mDbHelper == null || selectionArgs == null){
                return 0;
            }
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            if(db.isOpen()){
                db.delete(getTableName(), selection, selectionArgs);
                db.close();
            }
            return 0;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        synchronized (getContentUri()) {
            int rows = 0;

            if(values == null || mDbHelper == null){
                return rows;
            }
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            if(db.isOpen()){
                rows = db.update(getTableName(), values, selection, selectionArgs);
                db.close();
            }
            return rows;
        }
    }

    /**
     * Determine whether the specified value exists in the database.
     * @param context
     * @param selection
     * @param selectionArgs
     * @param content_uri
     * @return  true : exists
     *          false : not exists
     */
    public static boolean isInDB(Context context, String selection,
                                 String[] selectionArgs, Uri content_uri){

        if(context == null || TextUtils.isEmpty(selection) || selectionArgs == null){
            return false;
        }

        Cursor cursor = null;
        boolean isInDB = false;
        try {
            cursor = context.getContentResolver().query(
                    content_uri,
                    null,
                    selection,
                    selectionArgs,
                    null);
        } catch(Exception e) {
            e.printStackTrace();
        }

        if (cursor != null) {
            if(cursor.moveToFirst()){
                isInDB = true;
            }
            cursor.close();
        }
        return isInDB;
    }
}