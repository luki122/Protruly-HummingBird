package com.hb.interception.settings;

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

public class HbSettingProvider extends ContentProvider {

	public static final String AUTHORITY = "com.hb.reject";
	public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
	public static final String DATABASE_NAME = "hbRejectSetting.db";
	public static final int DATABASE_VERSION = 4;

	public static final String TABLE_NAME = "setting";
	public static final String TABLE_KEYWORD = "keyword";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/setting");
	public static final int SETTINGS = 1;
	public static final int KEYWORDS = 2;

	public static final String NAME = "name";
	public static final String VALUE = "value";

	public static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, "setting", SETTINGS);
		uriMatcher.addURI(AUTHORITY, "keyword", KEYWORDS);
	}

	private DBOpenHelper dbOpenHelper = null;

	@Override
	public boolean onCreate() {
		dbOpenHelper = new DBOpenHelper(this.getContext(), DATABASE_NAME,
				DATABASE_VERSION);
		return true;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		long id = 0;

		try {
			switch (uriMatcher.match(uri)) {
			case SETTINGS:
				id = db.insert(TABLE_NAME, null, values);
				return ContentUris.withAppendedId(uri, id);
			case KEYWORDS:
				id = db.insert(TABLE_KEYWORD, null, values);
				return ContentUris.withAppendedId(uri, id);
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
			}
		} finally {
			notifyChange();
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		int count = 0;
		try {
			switch (uriMatcher.match(uri)) {
			case SETTINGS:
				count = db.delete(TABLE_NAME, selection, selectionArgs);
				break;
			case KEYWORDS:
				count = db.delete(TABLE_KEYWORD, selection, selectionArgs);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
			}
			return count;
		} finally {
			notifyChange();
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		int count = 0;
		try {
			switch (uriMatcher.match(uri)) {
			case SETTINGS:
				count = db.update(TABLE_NAME, values, selection, selectionArgs);
				break;
			case KEYWORDS:
				count = db.update(TABLE_KEYWORD, values, selection,
						selectionArgs);
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
			}
			return count;
		} finally {
			notifyChange();
		}
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case SETTINGS:
			return "vnd.android.cursor.dir/hb.reject.setting";
		case KEYWORDS:
			return "vnd.android.cursor.dir/hb.reject.keyword";
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
		Cursor c = null;
		switch (uriMatcher.match(uri)) {
		case SETTINGS:
			c = db.query(TABLE_NAME, projection, selection, selectionArgs,
					null, null, sortOrder);
			break;
		case KEYWORDS:
			c = db.query(TABLE_KEYWORD, projection, selection, selectionArgs,
					null, null, sortOrder);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		if (c != null) {
			c.setNotificationUri(getContext().getContentResolver(),
					AUTHORITY_URI);
		}
		return c;
	}

	protected void notifyChange() {
		getContext().getContentResolver().notifyChange(AUTHORITY_URI, null,
				false);
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
			db.execSQL("create table " + TABLE_NAME + "(" + BaseColumns._ID
					+ " INTEGER PRIMARY KEY autoincrement," + NAME + " TEXT,"
					+ VALUE + " boolean)" + ";");
			db.execSQL("create table " + TABLE_KEYWORD + "(" + BaseColumns._ID
					+ " INTEGER PRIMARY KEY autoincrement," + " word TEXT)"
					+ ";");
			createDefaultData(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		}

		private void createDefaultData(SQLiteDatabase db) {
			String[] mName = { "sms_black", "sms_smart", "sms_keyword",
					"call_black", "call_smart", "notification" };
			Boolean[] mValues = { true, true, false, true, true, true };
			for (int i = 0; i < mName.length; i++) {
				ContentValues values = new ContentValues();
				values.put("name", mName[i]);
				values.put("value", mValues[i]);
				db.insert(TABLE_NAME, null, values);
			}
		}

	}
}