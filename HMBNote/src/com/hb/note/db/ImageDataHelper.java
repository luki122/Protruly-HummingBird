package com.hb.note.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class ImageDataHelper {

    private static final String[] IMAGE_COLUMNS = {
            Images.ID,
            Images.PATH,
            Images.ORIGINAL_PATH
    };

    private static final int INDEX_ID = 0;
    private static final int INDEX_PATH = 1;
    private static final int INDEX_ORIGINAL_PATH = 2;

    private static final String WHERE_BY_ORIGINAL_PATH = Images.ORIGINAL_PATH + "=?";

    private NoteDatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    public ImageDataHelper(Context context) {
        mDbHelper = new NoteDatabaseHelper(context);
        mDb = mDbHelper.getWritableDatabase();
    }

    public void shutdown() {
        if (mDbHelper != null) {
            mDbHelper.close();
            mDbHelper = null;
            mDb = null;
        }
    }

    private ContentValues getContentValues(ImageData imageData) {
        ContentValues values = new ContentValues();
        values.put(Images.PATH, imageData.getPath());
        values.put(Images.ORIGINAL_PATH, imageData.getOriginalPath());
        return values;
    }

    private ImageData getImageData(Cursor cursor) {
        ImageData imageData = new ImageData();
        imageData.setId(cursor.getInt(INDEX_ID));
        imageData.setPath(cursor.getString(INDEX_PATH));
        imageData.setOriginalPath(cursor.getString(INDEX_ORIGINAL_PATH));
        return imageData;
    }

    public long insert(ImageData imageData) {
        return mDb.insert(Images.TABLE_NAME, null, getContentValues(imageData));
    }

    public void delete(String originalPath) {
        mDb.delete(Images.TABLE_NAME, WHERE_BY_ORIGINAL_PATH, new String[]{originalPath});
    }

    public ImageData query(String originalPath) {
        Cursor cursor = mDb.query(Images.TABLE_NAME, IMAGE_COLUMNS,
                WHERE_BY_ORIGINAL_PATH, new String[]{originalPath}, null, null, null);
        if (cursor == null) {
            return null;
        }

        ImageData imageData = null;
        if (cursor.moveToFirst()) {
            imageData = getImageData(cursor);
        }
        cursor.close();
        return imageData;
    }
}
