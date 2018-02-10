package com.protruly.powermanager.purebackground.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * A helper class to manage purebackground database, helps open, create, and upgrade
 * the database file.
 */
public class DbHelper extends SQLiteOpenHelper {
    /**
     * The database version:
     * Database version 1 - initial version
     */
    private static final int DATABASE_VERSION = 1;

    /**
     * The database that the provider uses as its underlying data store
     */
    private static final String DATABASE_NAME = "purebackground.db";

    /**
     * INTEGER PRIMARY KEY AUTOINCREMENT.
     */
    private static final String FIELD_ID = "_id";

    /**
     * Column name for the Package Name
     * <P>Type: TEXT</P>
     */
    public static final String PACKAGE_NAME = "packageName";

    /**
     * The table name of Auto Clean App
     */
    public static final String TABLE_AutoCleanApp = "AutoCleanApp";

    /**
     * The table name of Allow Auto Start App
     */
    public static final String TABLE_AllowAutoStartApp = "AllowAutoStartApp";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_AutoCleanApp +
                " ("+ FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                PACKAGE_NAME + " TEXT);"
        );

        db.execSQL("CREATE TABLE " + TABLE_AllowAutoStartApp +
                " (" + FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"+
                PACKAGE_NAME +" TEXT);"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AutoCleanApp);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AllowAutoStartApp);
        onCreate(db);
    }
}