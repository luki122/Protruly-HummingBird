package com.hb.provider;


import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.Settings;
import android.text.TextUtils;

/**
 * 这个类给外部程序提供访问内部数据的一个接口
 * 
 * @author HB
 * 
 */
public class HbSettingProvider extends ContentProvider {
	
	public static final String AUTHORITY = "com.hb.phone";
	public static final String DATABASE_NAME = "hbPhoneSetting.db";
	public static final int DATABASE_VERSION = 4;
	
	public static final String TABLE_NAME = "setting";
	public static final Uri CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/phone_setting");
	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/hb.phone.setting";
	public static final String CONTENT_TYPE_ITME = "vnd.android.cursor.item/hb.phone.setting";
	public static final int SETTINGS = 1;

	public static final String NAME = "name";
	public static final String VALUE = "value";

	public static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, "phone_setting", SETTINGS);
	}

	private DBOpenHelper dbOpenHelper = null;

	@Override
	public boolean onCreate() {
		dbOpenHelper = new DBOpenHelper(this.getContext(),
				DATABASE_NAME, DATABASE_VERSION);
		return true;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		long id = 0;

		switch (uriMatcher.match(uri)) {
		case SETTINGS:
			id = db.insert(TABLE_NAME, null, values); 
			return ContentUris.withAppendedId(uri, id);
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		int count = 0;
		switch (uriMatcher.match(uri)) {
		case SETTINGS:
			count = db.delete(TABLE_NAME, selection, selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		int count = 0;
		switch (uriMatcher.match(uri)) {
		case SETTINGS:
			try {
				count = db.update(TABLE_NAME, values, selection, selectionArgs);
			} catch (Exception e) {
                e.printStackTrace();
                db.close();
                db = dbOpenHelper.getWritableDatabase();
    			count = db.update(TABLE_NAME, values, selection, selectionArgs);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case SETTINGS:
			return CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
		switch (uriMatcher.match(uri)) {
		case SETTINGS:
			return db.query(TABLE_NAME, projection, selection, selectionArgs,
					null, null, sortOrder);
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	public class DBOpenHelper extends SQLiteOpenHelper {
		
	    private final Context mContext;

		public DBOpenHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
	        mContext = context;
		}

		 public DBOpenHelper(Context context, String name) {
			 this(context, name, DATABASE_VERSION);
		 }

		public DBOpenHelper(Context context, String name, int version) {
			this(context, name, null, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			System.out.println("create table");
			db.execSQL("create table " + TABLE_NAME
					+ "(" + BaseColumns._ID + " INTEGER PRIMARY KEY autoincrement,"
					+ NAME + " TEXT,"
					+ VALUE + " boolean)" + ";");
			createDefaultData(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		}
		
	    private void createDefaultData(SQLiteDatabase db) {
		    Settings.Secure.putInt(mContext.getContentResolver(),
	                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
	                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);
	    	String[] mName = {"vibrate", "flash", "smartringer","overturn"};
	    	Boolean[] mValues= {true, false, false,false};
	        db.beginTransaction();
	        for (int i = 0; i < mName.length; i++) {
	        	ContentValues values = new ContentValues();
	        	values.put("name", mName[i]);
	        	values.put("value", mValues[i]);
	            db.insert(TABLE_NAME, null, values);            
	        }
	        db.setTransactionSuccessful();
            db.endTransaction();  
	    }

	}
}