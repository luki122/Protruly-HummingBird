package com.android.contacts.common.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog.Calls;

public class BlackUtils {

	public static Uri BLACK_URI = Uri.parse("content://com.hb.contacts/black");
	public static Uri MARK_URI = Uri.parse("content://com.hb.contacts/mark");
	public static final String BLACK_AUTHORITY = "com.android.contacts";

	public static String getBlackNameByCalllog(Context context, String address) {
		Cursor cursor = null;
		cursor = context.getContentResolver().query(Calls.CONTENT_URI, null,
				getPhoneNumberEqualString(address) + " and reject=1", null, null);
		if (cursor != null) {
			for (int i = 0; i < cursor.getCount(); i++) {
				cursor.moveToPosition(i);
				String name = cursor.getString(cursor.getColumnIndex("black_name"));
				cursor.close();
				return name;
			}
			cursor.close();
		}
		return null;
	}

	private static Uri uri = BlackUtils.BLACK_URI;

	public static String getBlackNameByPhoneNumber(Context context, String address) {
		Cursor cursor = null;
		cursor = context.getContentResolver().query(uri, null, getPhoneNumberEqualString(address), null, null);

		if (cursor != null) {
			for (int i = 0; i < cursor.getCount(); i++) {
				cursor.moveToPosition(i);
				String name = cursor.getString(cursor.getColumnIndex("black_name"));
				cursor.close();
				return name;
			}
			cursor.close();
		}

		return null;
	}

	public static String getLableByPhoneNumber(Context context, String address) {
		Cursor cursor = context.getContentResolver().query(uri, null, getPhoneNumberEqualString(address), null, null);
		if (cursor != null) {
			for (int i = 0; i < cursor.getCount(); i++) {
				cursor.moveToPosition(i);
				String lable = cursor.getString(cursor.getColumnIndex("lable"));
				cursor.close();
				return lable;
			}
			cursor.close();
		}

		return null;
	}

	public static String getLableByCalllog(Context context, String address) {
		Cursor cursor = context.getContentResolver().query(Calls.CONTENT_URI, null,
				getPhoneNumberEqualString(address) + " and reject=1", null, null);
		if (cursor != null) {
			for (int i = 0; i < cursor.getCount(); i++) {
				cursor.moveToPosition(i);
				String lable = cursor.getString(cursor.getColumnIndex("mark"));
				cursor.close();
				return lable;
			}
			cursor.close();
		}

		return null;
	}

	public static String getPhoneNumberEqualString(String number) {
		return " PHONE_NUMBERS_EQUAL(number, \"" + number + "\", 0) ";
	}

	public static boolean isNumberAlreadyExisted(Context context, String number) {
		Cursor cursor = context.getContentResolver().query(BLACK_URI, null,
				getPhoneNumberEqualString(number) + " and isblack=1", null, null);
		if (cursor != null) {
			if (cursor.getCount() > 0) {
				return true;
			}
			cursor.close();
		}

		return false;
	}

	public static final int SLIDER_BTN_POSITION_DELETE = 1;

	public static boolean isNoneDigit(String number) {
		boolean isDigit = false;
		for (int i = 0; i < number.length(); i++) {
			if (Character.isDigit(number.charAt(i))) {
				isDigit = true;
			}
		}
		if (number.indexOf('+', 1) > 0) {
			isDigit = false;
		}
		if (!isDigit) {
			return true;
		}
		return false;
	}

	public static String getUserMark(Context context, String number) {
		if (number == null) {
			return null;
		}
		String result = null;
		try {
			Cursor cursor = context.getContentResolver().query(MARK_URI, null,
					"PHONE_NUMBERS_EQUAL(number, " + number + ", 0)", null, null);
			if (cursor != null) {
				try {
					if (cursor.moveToFirst()) {
						result = cursor.getString(1);
					}
				} finally {
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();

		}

		return result;
	}

}