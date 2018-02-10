package com.hb.note.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class NoteDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "note.db";

    private static final int DATABASE_VERSION = 1;

    NoteDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Notes.TABLE_NAME + " (" +
                Notes.ID + " INTEGER PRIMARY KEY," +
                Notes.UUID + " TEXT," +
                Notes.TITLE + " TEXT," +
                Notes.CONTENT + " TEXT," +
                Notes.CHARACTERS + " TEXT," +
                Notes.IMAGE_COUNT + " INTEGER DEFAULT 0," +
                Notes.UPDATE_TIME + " INTEGER DEFAULT 0," +
                Notes.STICK_TIME + " INTEGER DEFAULT 0" +
                ");");

        db.execSQL("CREATE TABLE " + Images.TABLE_NAME + " (" +
                Images.ID + " INTEGER PRIMARY KEY," +
                Images.PATH + " TEXT," +
                Images.ORIGINAL_PATH + " TEXT" +
                ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
//        onCreate(db);
    }
}
