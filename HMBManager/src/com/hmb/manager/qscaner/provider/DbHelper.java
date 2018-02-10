package com.hmb.manager.qscaner.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * A helper class to manage qscaner database, helps open, create, and upgrade
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
    private static final String DATABASE_NAME = "qscaner.db";

    /**
     * INTEGER PRIMARY KEY AUTOINCREMENT.
     */
    private static final String FIELD_ID = "_id";

    /**
     * Column name for the risk File Type
     * <P>Type: INT4</P>
     */
    public static final String RISK_FILE_TYPE = "riskType";

    /**
     * Column name for the Package Name
     * <P>Type: TEXT</P>
     */
    public static final String PACKAGE_NAME = "packageName";

    /**
     * Column name for the Soft Name
     * <P>Type: TEXT</P>
     */
    public static final String SOFT_NAME = "softName";

    /**
     * Column name for the VERSION
     * <P>Type: TEXT</P>
     */
    public static final String VERSION = "version";

    /**
     * Column name for the VERSION
     * <P>Type: INT4</P>
     */
    public static final String VERSION_CODE = "versionCode";

    /**
     * Column name for the PATH
     * <P>Type: TEXT</P>
     */
    public static final String PATH = "path";

    /**
     * Column name for the scanResult
     * <P>Type: INT4</P>
     */
    public static final String SCAN_RESULT = "scanResult";

    /**
     * Column name for the virusName
     * <P>Type: TEXT</P>
     */
    public static final String VIRUS_NAME = "virusName";

    /**
     * Column name for the virusDiscription
     * <P>Type: TEXT</P>
     */
    public static final String VIRUS_DISCRIPTION = "virusDiscription";

    /**
     * Column name for the virusUrl
     * <P>Type: TEXT</P>
     */
    public static final String VIRUS_URL = "virusUrl";

    /**
     * The table name of app risk
     */
    public static final String TABLE_RISK = "Risk";


    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_RISK +
                " ("+ FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                RISK_FILE_TYPE +" INT4," +
                PACKAGE_NAME +" TEXT," +
                SOFT_NAME +" TEXT," +
                VERSION +" TEXT," +
                VERSION_CODE +" INT4," +
                PATH +" TEXT," +
                SCAN_RESULT +" TEXT," +
                VIRUS_NAME +" TEXT," +
                VIRUS_DISCRIPTION +" TEXT," +
                VIRUS_URL + " TEXT);"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RISK);
        onCreate(db);
    }
}