package com.android.providers.contacts;

import java.util.ArrayList;
import java.util.Arrays;

import com.android.providers.contacts.ContactsDatabaseHelper.BlackColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.ContactsDatabaseHelper.Views;
import com.android.providers.contacts.ContactsDatabaseHelper.WhiteColumns;
import com.android.providers.hb.ContactsContract;
import com.android.providers.hb.ContactsContract.Contacts;
import com.android.providers.hb.ContactsContract.Data;
import com.android.common.content.ProjectionMap;
import com.mediatek.providers.contacts.LogUtils;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.provider.BaseColumns;
import android.provider.Settings;
import com.android.providers.hb.CallLog.Calls;
import android.text.TextUtils;
import android.util.Log;

public class HbContactsProvider extends ContentProvider {
	private final static String TAG = "HbContactsProvider";

	public static final String AUTHORITY = "com.hb.contacts";
	public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

	// add by lgy
	private static final int QUICK_BLACK = 90009;
	private static final int BLACKS = 90010;
	private static final int BLACK = 90011;
	private static final int MARKS = 90012;
	private static final int MARK = 90013;
	private static final int WHITES = 90017;
	private static final int WHITE = 90018;

	public static final UriMatcher matcher;
	static {
		matcher = new UriMatcher(UriMatcher.NO_MATCH);
		matcher.addURI(AUTHORITY, "quickblack", QUICK_BLACK);
		matcher.addURI(AUTHORITY, "black", BLACKS);
		matcher.addURI(AUTHORITY, "black/#", BLACK);
		matcher.addURI(AUTHORITY, "mark", MARKS);
		matcher.addURI(AUTHORITY, "mark/#", MARK);
		matcher.addURI(AUTHORITY, "white", WHITES);
		matcher.addURI(AUTHORITY, "white/#", WHITE);
	}

	private ContactsDatabaseHelper mContactsHelper;

	@Override
	public boolean onCreate() {
		mContactsHelper = ContactsDatabaseHelper.getInstance(getContext());
		return true;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = mContactsHelper.getWritableDatabase();
		long id = 0;

		String path = "";

		try {
			switch (matcher.match(uri)) {
				case BLACKS:
					id = db.replace("black", null, values); // 返回的是记录的行号，主键为int，实际上就是主键值
	
					String name = values.getAsString("black_name");
					String number = values.getAsString("number");
					int rejected = values.getAsInteger("reject");
					Log.d(TAG, "number = " + number + "  rejected = " + rejected);
	
					if (id > 0) {
						updateRejectMms(1, 1, name, number, rejected);
					}
	
					return ContentUris.withAppendedId(uri, id);
				case BLACK:
					id = db.replace("black", null, values);
					path = uri.toString();
					return Uri.parse(path.substring(0, path.lastIndexOf("/")) + id);
				case MARKS:
					id = db.insert("mark", null, values);
					return ContentUris.withAppendedId(uri, id);
				case MARK:
					id = db.insert("mark", null, values);
					path = uri.toString();
					return Uri.parse(path.substring(0, path.lastIndexOf("/")) + id);
				case WHITES:
					id = db.replace("white", null, values);
					return ContentUris.withAppendedId(uri, id);
				case WHITE:
					id = db.replace("white", null, values);
					path = uri.toString();
					return Uri.parse(path.substring(0, path.lastIndexOf("/")) + id);
				default:
					throw new IllegalArgumentException("Unknown URI " + uri);
			}
		} finally {
			notifyChange();
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mContactsHelper.getWritableDatabase();
		int count = 0;
		String where = "";
		try {
			switch (matcher.match(uri)) {
				case BLACKS:
					Log.d(TAG, "BLACKS selection = " + selection);
					if (selection.startsWith("_ID=?")) {
						if (selectionArgs != null) {
							for (String id : selectionArgs) {
								String number = queryNumberForId(id);
								updateCallsForBlack(number, null, false);
	
								updateRejectMms(3, 0, null, number, 0);
							}
						}
					} else if (selection.startsWith("_ID in")) {
						String ids = selection.replaceAll("_ID in", "");
						ids = ids.replaceAll(" ", "");
						ids = ids.substring(1, ids.length() - 1);
						Log.d(TAG, "ids = " + ids);
	
						String[] idsArr = ids.split(",");
						for (String id : idsArr) {
							String number = queryNumberForId(id);
							updateCallsForBlack(number, null, false);
	
							updateRejectMms(3, 0, null, number, 0);
						}
					} else if (selection.startsWith("_ID=")) {
						String id = selection.replaceAll("_ID=", "");
						id = id.replaceAll(" ", "");
						Log.d(TAG, "id = " + id);
	
						String number = queryNumberForId(id);
						updateCallsForBlack(number, null, false);
	
						updateRejectMms(3, 0, null, number, 0);
					}
	
					return db.delete("black", selection, selectionArgs);
				case BLACK:
					long blackid = ContentUris.parseId(uri);
					where = "_ID=" + blackid;
					where += !TextUtils.isEmpty(selection) ? " and (" + selection
							+ ")" : "";
					return db.delete("black", where, selectionArgs);
				case MARKS:
					return db.delete("mark", selection, selectionArgs);
				case MARK:
					long markid = ContentUris.parseId(uri);
					where = "_ID=" + markid;
					where += !TextUtils.isEmpty(selection) ? " and (" + selection
							+ ")" : "";
					return db.delete("mark", where, selectionArgs);
				case WHITES:
					Log.d(TAG, "WHITES selection = " + selection);
					return db.delete("white", selection, selectionArgs);
				case WHITE:
					long whiteid = ContentUris.parseId(uri);
					where = "_ID=" + whiteid;
					where += !TextUtils.isEmpty(selection) ? " and (" + selection
							+ ")" : "";
					return db.delete("white", where, selectionArgs);
				default:
					throw new IllegalArgumentException("Unknown URI " + uri);
			}
		} finally {
			notifyChange();
		}
		// db.close();
		// return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = mContactsHelper.getWritableDatabase();
		int count = 0;
		String where = "";
		try {
			switch (matcher.match(uri)) {
				case BLACKS:
					count = db.update("black", values, selection, selectionArgs);
	
					if (count > 0) {
						boolean needNotif = false;
						int isBlack = 0;
						if (values.containsKey("isblack")) {
							isBlack = values.getAsInteger("isblack");
							needNotif = true;
						}
	
						String number = null;
						if (values.containsKey("number")) {
							number = values.getAsString("number");
						}
	
						int reject = 0;
						String name = null;
						if (values.containsKey("black_name")) {
							name = values.getAsString("black_name");
						}
	
						if (values.containsKey("reject")) {
							reject = values.getAsInteger("reject");
						} else {
							Cursor cursor = db.query("black", new String[] {
									"reject", "black_name" }, "number='" + number
									+ "'", null, null, null, null);
							if (cursor != null) {
								if (cursor.moveToFirst()) {
									try {
										reject = cursor.getInt(0);
										if (name == null) {
											name = cursor.getString(1);
										}
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
	
								cursor.close();
							}
						}
	
						Log.d(TAG, "number = " + number + "  rejected = " + reject
								+ "  isblack = " + isBlack + "  name = " + name);
						if (!needNotif) {
							break;
						}
	
						updateRejectMms(2, isBlack, null, number, reject);
	
						if (isBlack > 0) {
							if (reject == 1 || reject == 3) {
								updateCallsForBlack(number, name, true);
							} else {
								updateCallsForBlack(number, null, false);
							}
						} else {
	
							String black_name = null;
							Log.d(TAG, "isBlack = " + isBlack + "  number = "
									+ number);
							if (isBlack == 0) {
								updateCallsForBlack(number, black_name, false);
							}
	
							db.delete("black", "number='" + number
									+ "' and isblack<1", null);
							notifyBlacklistChange(number);
						}
					}
	
					break;
				case BLACK:
					long blackid = ContentUris.parseId(uri);
					where = "_ID=" + blackid;
					where += !TextUtils.isEmpty(selection) ? " and (" + selection
							+ ")" : "";
					count = db.update("black", values, where, selectionArgs);
	
					break;
				case MARKS:
					count = db.update("mark", values, selection, selectionArgs);
					break;
				case MARK:
	
					long markid = ContentUris.parseId(uri);
					where = "_ID=" + markid;
					where += !TextUtils.isEmpty(selection) ? " and (" + selection
							+ ")" : "";
					count = db.update("mark", values, where, selectionArgs);
					break;
				case WHITES:
					count = db.update("white", values, selection, selectionArgs);
					break;
				case WHITE:
					long whiteid = ContentUris.parseId(uri);
					where = "_ID=" + whiteid;
					where += !TextUtils.isEmpty(selection) ? " and (" + selection
							+ ")" : "";
					count = db.update("white", values, where, selectionArgs);
	
					break;
				default:
					throw new IllegalArgumentException("Unknown URI " + uri);
			}
		} finally {
			notifyChange();
		}
		// db.close();
		return count;
	}
	//zhangcj add start
	private ArrayList<String> mDeletNumList = new ArrayList<String>();
	private boolean mIsBlackMultipleDelete = false;
	private void notifyBlacklistChange(String number) {
		if (mDeletNumList == null) {
			mDeletNumList = new ArrayList<String>();
		} 
		if (mIsInTransaction) {
			mDeletNumList.add(number);
			mIsBlackMultipleDelete = true;
			return;
		} 
			Intent intent = new Intent("android.intent.action.BLACK_DATABASE_DELETE");
			mDeletNumList.add(number);
			String[] num = (String[]) mDeletNumList.toArray(new String [mDeletNumList.size()] );
			intent.putExtra("hb_delete_black_nums", num);
			getContext().sendBroadcast(intent);	
	}
	//zhangcj add end
	
	@Override
	public String getType(Uri uri) {
		switch (matcher.match(uri)) {
		case QUICK_BLACK:
		case BLACKS:
		case MARKS:
		case WHITES:
		case WHITE:
			return "vnd.android.cursor.dir/com.hb.reject";
		case BLACK:
		case MARK:
			return "vnd.android.cursor.item/com.hb.reject";
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db = mContactsHelper.getReadableDatabase();
		String where = "";
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch (matcher.match(uri)) {
		case QUICK_BLACK:
			return db.query("view_black_not_white", projection, where, selectionArgs, null,
					null, sortOrder);
		case BLACKS:
			// return db.query("black", projection, selection,
			// selectionArgs, null, null, sortOrder);
			setTablesAndProjectionMapForBlack(qb);
			break;
		case BLACK:
			long blackid = ContentUris.parseId(uri);
			where = "_ID=" + blackid;
			where += !TextUtils.isEmpty(selection) ? " and (" + selection + ")"
					: "";
			return db.query("black", projection, where, selectionArgs, null,
					null, sortOrder);
		case MARKS:
			String groupBY = "lable";
			return db.query("mark", projection, selection, selectionArgs,
					groupBY, null, "_id");
		case MARK:
			long markid = ContentUris.parseId(uri);
			where = "_ID=" + markid;
			where += !TextUtils.isEmpty(selection) ? " and (" + selection + ")"
					: "";
			return db.query("mark", projection, where, selectionArgs, null,
					null, sortOrder);
		case WHITES:
			setTablesAndProjectionMapForWhite(qb);
			break;
		case WHITE:
			long whiteid = ContentUris.parseId(uri);
			where = "_ID=" + whiteid;
			where += !TextUtils.isEmpty(selection) ? " and (" + selection + ")"
					: "";
			return db.query("white", projection, where, selectionArgs, null,
					null, sortOrder);
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		qb.setStrict(true);

		Cursor cursor = null;
		try {
			cursor = doQuery(db, qb, projection, selection, selectionArgs,
					sortOrder, null, null, null, null);
			return cursor;

		} catch (OperationCanceledException e) {
			LogUtils.e(TAG, "OperationCanceledException :" + e);
			if (cursor != null) {
				cursor.close();
			}
			throw e;
		}
	}

	private Cursor doQuery(final SQLiteDatabase db, SQLiteQueryBuilder qb,
			String[] projection, String selection, String[] selectionArgs,
			String sortOrder, String groupBy, String having, String limit,
			CancellationSignal cancellationSignal) {
		Log.d(TAG,
				"doQuery,qb:" + qb + " projection:"
						+ Arrays.toString(projection) + " selection:"
						+ selection + " selectionArgs:"
						+ Arrays.toString(selectionArgs) + " sortOrder:"
						+ sortOrder + " groupBy:" + groupBy + " having:"
						+ having + " limit:" + limit);
		final Cursor c = qb.query(db, projection, selection, selectionArgs,
				groupBy, having, sortOrder, limit, cancellationSignal);
		if (c != null) {
			LogUtils.d(TAG, "[query]c.count(): " + c.getCount());
			c.setNotificationUri(getContext().getContentResolver(),
					AUTHORITY_URI);
		}
		return c;
	}

	private String queryNumberForId(String id) {
		Log.d(TAG, "BlackTableData id = " + id);
		String result = null;
		final SQLiteDatabase db = mContactsHelper.getReadableDatabase();
		Cursor cursor = db.query("black", new String[] { "number" }, "_id="
				+ id, null, null, null, null);

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				result = cursor.getString(0);
				Log.d(TAG, "result = " + result);
			}
			cursor.close();
		}

		return result;
	}


	/*
	 * flag: true(rejected), false(not rejected)
	 */
	private void updateCallsForBlack(String number, String name, boolean flag) {
		if (number == null) {
			return;
		}

		int rejected = 0;
		if (flag) {
			rejected = 1;
		}

		try {
			final SQLiteDatabase db = mContactsHelper.getWritableDatabase();
			ContentValues cv = new ContentValues();
			cv.put("reject", rejected);
			cv.put("black_name", name);
			db.update(Tables.CALLS, cv, "PHONE_NUMBERS_EQUAL(number, " + number
					+ ", 0) and type in (1, 3) and reject in(0, 1)", null);
			cv.clear();
			cv = null;

			getContext().getContentResolver().notifyChange(Calls.CONTENT_URI,
					null, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateRejectMms(int type, int isBlack, String name,
			String number, int reject) {
		// ContentValues values = new ContentValues();
		// values.put("insert_update_delete", type);
		// if (type < 3) {
		// values.put("isblack", isBlack);
		// if (name != null) {
		// values.put("name", name);
		// }
		// values.put("reject", reject);
		// }
		// values.put("number", number);
		// values.put("feature_name", "reject");
		//
		// getContext().getContentResolver().update(
		// Uri.parse("content://mms-sms/aurora_special_feature"), values,
		// null, null);
		// values.clear();
		// values = null;
	}

	private static final String sStableBlackJoinData = Tables.BLACK
			+ " LEFT JOIN " + " (SELECT * FROM " + Views.DATA + " WHERE "
			+ Data._ID + " IN " + "(SELECT " + Calls.DATA_ID + " FROM "
			+ Tables.BLACK + ")) AS " + Views.DATA + " ON(" + Tables.BLACK
			+ "." + Calls.DATA_ID + " = " + Views.DATA + "." + Data._ID + ")";

	private static final ProjectionMap sBlackProjectionMap = ProjectionMap
			.builder()
			.add(BlackColumns._ID, Tables.BLACK + "." + BlackColumns._ID)
			.add(BlackColumns.ISBLACK)
			.add(BlackColumns.LABLE)
			.add(BlackColumns.NUMBER)
			.add(BlackColumns.REJECT, Tables.BLACK + "." + BlackColumns.REJECT)
			.add(BlackColumns.BLACK_NAME)
			.add(Contacts.DISPLAY_NAME)
			.add(Calls.RAW_CONTACT_ID,
					Tables.BLACK + "." + Calls.RAW_CONTACT_ID)
			.add(Calls.DATA_ID, Tables.BLACK + "." + Calls.DATA_ID)
			.add("user_mark", Tables.BLACK + ".user_mark").build();

	private void setTablesAndProjectionMapForBlack(SQLiteQueryBuilder qb) {
		qb.setTables(sStableBlackJoinData);
		qb.setProjectionMap(sBlackProjectionMap);
	}

	private static final String sStableWhiteJoinData = Tables.WHITE
			+ " LEFT JOIN " + " (SELECT * FROM " + Views.DATA + " WHERE "
			+ Data._ID + " IN " + "(SELECT " + Calls.DATA_ID + " FROM "
			+ Tables.WHITE + ")) AS " + Views.DATA + " ON(" + Tables.WHITE
			+ "." + Calls.DATA_ID + " = " + Views.DATA + "." + Data._ID + ")";

	private static final ProjectionMap sWhiteProjectionMap = ProjectionMap
			.builder()
			.add(WhiteColumns._ID, Tables.WHITE + "." + WhiteColumns._ID)
			.add(WhiteColumns.LABLE)
			.add(WhiteColumns.NUMBER)
		    .add(WhiteColumns.NAME)
			.add(Contacts.DISPLAY_NAME)
			.add(Calls.RAW_CONTACT_ID,
					Tables.WHITE + "." + Calls.RAW_CONTACT_ID)
			.add(Calls.DATA_ID, Tables.WHITE + "." + Calls.DATA_ID).build();

	private void setTablesAndProjectionMapForWhite(SQLiteQueryBuilder qb) {
		qb.setTables(sStableWhiteJoinData);
		qb.setProjectionMap(sWhiteProjectionMap);
	}

	protected void notifyChange() {
		if(!mIsInTransaction) {
			getContext().getContentResolver().notifyChange(AUTHORITY_URI, null,
					false);

			Intent intent = new Intent("android.intent.action.BLACK_DATABASE_CHANGE");
			getContext().sendBroadcast(intent);
			//zhangcj add start
			if(mIsBlackMultipleDelete) {
				Intent intent1 = new Intent("android.intent.action.BLACK_DATABASE_DELETE");
				String[] num = (String[]) mDeletNumList.toArray(new String [mDeletNumList.size()] );
				intent1.putExtra("hb_delete_black_nums", num);
				getContext().sendBroadcast(intent1);	
				mIsBlackMultipleDelete = false;
				mDeletNumList.clear();
				mDeletNumList = null;
			}
			//zhangcj add end
		}
	}

    private static final int IS_WHITE = 0;
    private static final int IS_BLACK = 1;
    private static final int IS_IGNORE = 2;
    @Override  
    public Bundle call(String method, String arg, Bundle extras) {  
        System.out.println("method:" + method);  
        if(method.equalsIgnoreCase("isRejectQuick")) {
        	int  result = IS_IGNORE;
    		SQLiteDatabase db = mContactsHelper.getWritableDatabase();
            Cursor cursor = db.query("view_black_not_white", new String[]{"number", "type"}, "PHONE_NUMBERS_EQUAL(number, " + arg + ", 0) ", null, null,
    				null, null);
        	try {
    			if (cursor != null && cursor.getCount() > 0) {
    				cursor.moveToFirst();
    				result = IS_BLACK ;
    				do {
    					if(cursor.getInt(1) == 0) {
    						result = IS_WHITE;
    					}
    				} while(cursor.moveToNext());
    			}
    		} finally {
    			if(cursor != null) {
    				cursor.close();
    			}
    		}
            Bundle bundle = new Bundle();  
            bundle.putInt("result", result);
            return bundle;
        }
        return null;  
    }      
    
	public int bulkInsert(Uri uri, ContentValues[] values) {
 	    SQLiteDatabase db = mContactsHelper.getWritableDatabase();
        db.beginTransaction();//开始事务  
        mIsInTransaction = true;
        try{  
                 int results = super.bulkInsert(uri, values);  
                 db.setTransactionSuccessful();//设置事务标记为successful  
                 return results;  
        }finally {  
                 mIsInTransaction = false;
                 db.endTransaction();//结束事务  
                 notifyChange();
        }  
	}
    
	private boolean mIsInTransaction = false;
    public ContentProviderResult[] applyBatch(
            ArrayList<ContentProviderOperation> operations)
                    throws OperationApplicationException {      
 	    SQLiteDatabase db = mContactsHelper.getWritableDatabase();
        db.beginTransaction();//开始事务
        mIsInTransaction = true;
        try{  
                 ContentProviderResult[] results = super.applyBatch(operations);  
                 db.setTransactionSuccessful();//设置事务标记为successful  
                 return results;  
        } finally {  
                 mIsInTransaction = false;
                 db.endTransaction();//结束事务  
                 notifyChange();
        }  
    }

}