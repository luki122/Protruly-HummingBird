package cn.com.protruly.soundrecorder.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

/**
 * Created by liushitao on 17-8-28.
 */

public class DatabaseUtil {
    private Context mContext;
    private ContentResolver mContentResolver;

    public DatabaseUtil(Context context){
        mContext = context;
        if(null!=mContext){
            mContentResolver = mContext.getContentResolver();
        }
    }

    public Uri insertRecordFile(String filePath){
        if(null == mContentResolver) return Uri.EMPTY;
        ContentValues values = new ContentValues();
        values.put("_data",filePath);
        values.put("format","12297");
        values.put("tags","hmb_record");
        try {
            Log.d("MainActivity","aaaa:"+mContentResolver.insert(MediaStore.Files.getContentUri("external"), values));
            return mContentResolver.insert(MediaStore.Files.getContentUri("external"), values);
        }catch(SQLiteException e){
            Log.d("bql","insertRecordFile  SQLiteException:"+e);
        }
        return null;
    }

    public int deleteRecordFile(String filePath){
        if(null == mContentResolver) return -1;
        try {
            return mContentResolver.delete(MediaStore.Files.getContentUri("external"), "_data like ? AND tags = ?", new String[]{filePath,"hmb_record"});
        }catch(SQLiteException e){
            Log.d("bql","deleteRecordFile SQLiteException:"+e);
        }
        return -2;
    }

    public int deleteAllRecordFile(String fileDir){
        if(null == mContentResolver) return -1;
        try {
            return mContentResolver.delete(MediaStore.Files.getContentUri("external"), "_data like ? AND tags = ?", new String[]{fileDir+"/%","hmb_record"});
        }catch(SQLiteException e){
            Log.d("bql","deleteRecordFile SQLiteException:"+e);
        }
        return -2;
    }

    public Cursor queryRecordFile(String fileDir) {
        if(null == mContentResolver) return null;
        try {
            return mContentResolver.query(MediaStore.Files.getContentUri("external"), new String[]{"_data"}, "_data like ? AND tags = ?", new String[]{fileDir+"/%","hmb_record"}, null);
        } catch (SQLiteException e) {
            Log.w("bql", e);
        }
        return null;
    }

    public boolean isHasThisFileInDB(String fileDir){
        Cursor cursor = queryRecordFile(fileDir);
        return cursor != null && cursor.moveToFirst();
    }


    public Uri getContentUri(String path) {
        if (null == path) return null;
        String[] projection = {"_id"};
        String[] selectionArgs = {path};
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(MediaStore.Files.getContentUri("external"), projection, "_data like ?", selectionArgs, null);
            Log.d("bql", "getContentUriFromPath cursor:" + cursor);
            if (cursor != null && cursor.moveToNext()) {
                String _id = cursor.getString(cursor.getColumnIndex("_id"));
                return Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), _id);
            }
        }catch(SQLiteException e){
            if(cursor != null){
                cursor.close();
            }
        }finally {
            if(cursor != null){
                cursor.close();
            }
        }
        return null;
    }




}
