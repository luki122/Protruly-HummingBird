package com.hb.thememanager.database;


import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.TLog;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.SparseArray;
import android.text.TextUtils;

public class ThemeDataBaseHelper extends SQLiteOpenHelper {
    /** Database filename */
    private static final String DB_NAME = "themes.db";
    /** Current database version */
    private static final int DB_VERSION = 29;
    /**
     *  Name of table in the database 
     *  */
    private static final SparseArray<String> TABLES = new SparseArray<String>();
    private static final String TABLE_THEMES = "themes";
    private static final String TABLE_RINGTONE = "ringtone";
    private static final String TABLE_WALLPAPER = "wallpaper";
    private static final String TABLE_FONTS = "fonts";
    private static final String TABLE_LOCKSCREEN_WALLPAPER = "lockscreen_wallpaper";
    private static final String TABLE_USER_THEMES = "user_themes";
    static{
    	TABLES.clear();
    	TABLES.put(Theme.THEME_PKG, TABLE_THEMES);
    	TABLES.put(Theme.RINGTONE, TABLE_RINGTONE);
    	TABLES.put(Theme.WALLPAPER, TABLE_WALLPAPER);
    	TABLES.put(Theme.FONTS, TABLE_FONTS);
    	TABLES.put(Theme.LOCKSCREEN_WALLPAPER, TABLE_LOCKSCREEN_WALLPAPER);
        TABLES.put(Config.DatabaseColumns.THEME_USERS,TABLE_USER_THEMES);
    }
    /**
     * Sql to create theme package table
     */
    private static final String SQL_CREATE_THEME_PKG_TABLE =  "(" +
            Config.DatabaseColumns._ID + " TEXT PRIMARY KEY," +
            Config.DatabaseColumns.URI + " TEXT, " +
            Config.DatabaseColumns.FILE_PATH + " TEXT, " +
            Config.DatabaseColumns.LOADED_PATH + " TEXT, " +
            Config.DatabaseColumns.NAME + " TEXT, " +
            Config.DatabaseColumns.TYPE + " INTEGER, " +
            Config.DatabaseColumns.APPLY_STATUS + " INTEGER, " +
            Config.DatabaseColumns.LOADED + " INTEGER, " +
            Config.DatabaseColumns.IS_SYSTEM_THEME + " INTEGER, " +
            Config.DatabaseColumns.DOWNLOAD_STATUS + " INTEGER, " +
            Config.DatabaseColumns.LAST_MODIFIED_TIME + " BIGINT, " +
            Config.DatabaseColumns.DESGINER + " TEXT, " +
            Config.DatabaseColumns.DESIGNER_ID + " INTEGER DEFAULT 0, " +
            Config.DatabaseColumns.SIZE+" TEXT, "+
            Config.DatabaseColumns.IS_CHARGE + " INTEGER, " +
            Config.DatabaseColumns.VERSION + " TEXT, " +
            Config.DatabaseColumns.PRICE +" TEXT, "+
            Config.DatabaseColumns.PAID +" INTEGER, "+
            Config.DatabaseColumns.HAS_NEW_VERSION+" INTEGER, "+
            Config.DatabaseColumns.DESCRIPTION + " TEXT);";
    /**
     * Sql to create desktop wallpaper table
     */
    private static final String SQL_CREATE_WALLPAPER_TABLE =  "(" +
            Config.DatabaseColumns._ID + " TEXT PRIMARY KEY," +
            Config.DatabaseColumns.URI + " TEXT, " +
            Config.DatabaseColumns.FILE_PATH + " TEXT, " +
            Config.DatabaseColumns.LOADED_PATH + " TEXT, " +
            Config.DatabaseColumns.NAME + " TEXT, " +
            Config.DatabaseColumns.TYPE + " INTEGER, " +
            Config.DatabaseColumns.APPLY_STATUS + " INTEGER, " +
            Config.DatabaseColumns.LOADED + " INTEGER, " +
            Config.DatabaseColumns.IS_SYSTEM_THEME + " INTEGER, " +
            Config.DatabaseColumns.DOWNLOAD_STATUS + " INTEGER, " +
            Config.DatabaseColumns.LAST_MODIFIED_TIME + " BIGINT, " +
            Config.DatabaseColumns.DESGINER + " TEXT, " +
            Config.DatabaseColumns.SIZE+" TEXT, "+
            Config.DatabaseColumns.TOTAL_BYTES + " INTEGER, " +
            Config.DatabaseColumns.CURRENT_BYTES + " INTEGER, " +
            Config.DatabaseColumns.VERSION + " TEXT, " +
            Config.DatabaseColumns.DESCRIPTION + " TEXT);";
    /**
     * Sql to create ringtong table
     */
    private static final String SQL_CREATE_RINGTONG_TABLE =  "(" +
            Config.DatabaseColumns._ID + " TEXT PRIMARY KEY," +
            Config.DatabaseColumns.URI + " TEXT, " +
            Config.DatabaseColumns.FILE_PATH + " TEXT, " +
            Config.DatabaseColumns.LOADED_PATH + " TEXT, " +
            Config.DatabaseColumns.NAME + " TEXT, " +
            Config.DatabaseColumns.TYPE + " INTEGER, " +
            Config.DatabaseColumns.APPLY_STATUS + " INTEGER, " +
            Config.DatabaseColumns.LOADED + " INTEGER, " +
            Config.DatabaseColumns.IS_SYSTEM_THEME + " INTEGER, " +
            Config.DatabaseColumns.DOWNLOAD_STATUS + " INTEGER, " +
            Config.DatabaseColumns.LAST_MODIFIED_TIME + " BIGINT, " +
            Config.DatabaseColumns.DESGINER + " TEXT, " +
            Config.DatabaseColumns.SIZE+" TEXT, "+
            Config.DatabaseColumns.TOTAL_BYTES + " INTEGER, " +
            Config.DatabaseColumns.CURRENT_BYTES + " INTEGER, " +
            Config.DatabaseColumns.VERSION + " TEXT, " +
            Config.DatabaseColumns.DESCRIPTION + " TEXT);";
    /**
     * Sql to create fonts table
     */
    private static final String SQL_CREATE_FONTS_TABLE =  "(" +
            Config.DatabaseColumns._ID + " TEXT PRIMARY KEY," +
            Config.DatabaseColumns.URI + " TEXT, " +
            Config.DatabaseColumns.FILE_PATH + " TEXT, " +
            Config.DatabaseColumns.LOADED_PATH + " TEXT, " +
            Config.DatabaseColumns.NAME + " TEXT, " +
            Config.DatabaseColumns.TYPE + " INTEGER, " +
            Config.DatabaseColumns.APPLY_STATUS + " INTEGER, " +
            Config.DatabaseColumns.LOADED + " INTEGER, " +
            Config.DatabaseColumns.IS_SYSTEM_THEME + " INTEGER, " +
            Config.DatabaseColumns.DOWNLOAD_STATUS + " INTEGER, " +
            Config.DatabaseColumns.LAST_MODIFIED_TIME + " BIGINT, " +
            Config.DatabaseColumns.DESGINER + " TEXT, " +
            Config.DatabaseColumns.DESIGNER_ID + " INTEGER DEFAULT 0, " +
            Config.DatabaseColumns.SIZE+" TEXT, "+
            Config.DatabaseColumns.IS_CHARGE + " INTEGER, " +
            Config.DatabaseColumns.VERSION + " TEXT, " +
            Config.DatabaseColumns.PRICE +" TEXT, "+
            Config.DatabaseColumns.PAID +" INTEGER, "+
            Config.DatabaseColumns.HAS_NEW_VERSION+" INTEGER, "+
            Config.DatabaseColumns.DESCRIPTION + " TEXT);";
    /**
     * Sql to create lockscreen wallpaper table
     */
    private static final String SQL_CREATE_LOCKSCREEN_WALLPAPER_TABLE =  "(" +
            Config.DatabaseColumns._ID + " TEXT PRIMARY KEY," +
            Config.DatabaseColumns.URI + " TEXT, " +
            Config.DatabaseColumns.FILE_PATH + " TEXT, " +
            Config.DatabaseColumns.LOADED_PATH + " TEXT, " +
            Config.DatabaseColumns.NAME + " TEXT, " +
            Config.DatabaseColumns.TYPE + " INTEGER, " +
            Config.DatabaseColumns.APPLY_STATUS + " INTEGER, " +
            Config.DatabaseColumns.LOADED + " INTEGER, " +
            Config.DatabaseColumns.IS_SYSTEM_THEME + " INTEGER, " +
            Config.DatabaseColumns.DOWNLOAD_STATUS + " INTEGER, " +
            Config.DatabaseColumns.LAST_MODIFIED_TIME + " BIGINT, " +
            Config.DatabaseColumns.DESGINER + " TEXT, " +
            Config.DatabaseColumns.SIZE+" TEXT, "+
            Config.DatabaseColumns.TOTAL_BYTES + " INTEGER, " +
            Config.DatabaseColumns.CURRENT_BYTES + " INTEGER, " +
            Config.DatabaseColumns.VERSION + " TEXT, " +
            Config.DatabaseColumns.DESCRIPTION + " TEXT);";



    /**
     * Sql to create lockscreen wallpaper table
     */
    private static final String SQL_CREATE_USER_THEMES_TABLE =  "(" +
            Config.DatabaseColumns._ID + " TEXT PRIMARY KEY," +
            Config.DatabaseColumns.TYPE + " INTEGER, " +
            Config.DatabaseColumns.USER_ID + " INTEGER DEFAULT 0);";
   
    private String DB_TABLE = "themes";
	private static final String TAG = "DataBase";
    
    
    public ThemeDataBaseHelper(final Context context,int themeType) {
        super(context, DB_NAME, null, DB_VERSION);
        if(themeType != Theme.THEME_NULL) {
            DB_TABLE = TABLES.get(themeType);
        }
        
        if(TextUtils.isEmpty(DB_TABLE)){
        	throw new IllegalArgumentException("unsupport theme type:"+themeType);
        }
        TLog.d(TAG, "create table in themes database:"+DB_TABLE);
    }

    public String getTable(){
    	return DB_TABLE;
    }
    
    /**
     * Creates database the first time we try to open it.
     */
    @Override
    public void onCreate(final SQLiteDatabase db) {
        createThemesTables(db);
    }

   
    @Override
    public void onUpgrade(final SQLiteDatabase db, int oldV, final int newV) {
            upgradeTo(db);
    }

    private void upgradeTo(SQLiteDatabase db) {
    	for(int i = 0;i<TABLES.size();i++){
    		int key = TABLES.keyAt(i);
    		String table = TABLES.get(key);
    		if(!TextUtils.isEmpty(table)){
    			db.execSQL("DROP TABLE IF EXISTS " + table);
    		}
    	}
        createThemesTables(db);
    }


    /**
     * Add a column to a table using ALTER TABLE.
     * @param dbTable name of the table
     * @param columnName name of the column to add
     * @param columnDefinition SQL for the column definition
     */
    public void addColumn(SQLiteDatabase db, String dbTable, String columnName,
                           String columnDefinition) {
        db.execSQL("ALTER TABLE " + dbTable + " ADD COLUMN " + columnName + " "
                   + columnDefinition);
    }

    public long insert(SQLiteDatabase db,ContentValues values){
    	return db.insert(DB_TABLE, null, values);
    }
    
    
    /**
     * Creates the table that'll hold the theme information.
     */
    private void createThemesTables(SQLiteDatabase db) {
    	for(int i = 0;i<TABLES.size();i++){
    		int key = TABLES.keyAt(i);
    		String table = TABLES.get(key);
    		if(!TextUtils.isEmpty(table)){
    			createThemesTable(db, table);
    		}
    	}
    }

    private void createThemesTable(SQLiteDatabase db,String table) {
    	StringBuilder sql = new StringBuilder();
    	sql.append("CREATE TABLE ");
    	sql.append(table);
    	if(TABLE_THEMES.equals(table)){
    		sql.append(SQL_CREATE_THEME_PKG_TABLE);
    	}else if(TABLE_LOCKSCREEN_WALLPAPER.equals(table)){
    		sql.append(SQL_CREATE_LOCKSCREEN_WALLPAPER_TABLE);
    	}else if(TABLE_FONTS.equals(table)){
    		sql.append(SQL_CREATE_FONTS_TABLE);
    	}else if(TABLE_WALLPAPER.equals(table)){
    		sql.append(SQL_CREATE_WALLPAPER_TABLE);
    	}else if(TABLE_RINGTONE.equals(table)){
    		sql.append(SQL_CREATE_RINGTONG_TABLE);
    	}else if(TABLE_USER_THEMES.equals(table)){
            sql.append(SQL_CREATE_USER_THEMES_TABLE);
        }
        try {
            db.execSQL(sql.toString());
        } catch (SQLException ex) {
            TLog.e(TAG, "couldn't create table in themes database:"+ex+"  SQL:"+sql);
        }
    }

}
