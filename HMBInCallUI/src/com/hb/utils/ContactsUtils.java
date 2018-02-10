package com.hb.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.text.TextUtils;
import android.util.Log;

public class ContactsUtils {
	public final static String TAG = "ContactsUtils"; 

    private static final String[] PROJECTION = new String[] {
        PhoneLookup._ID,
        PhoneLookup.DISPLAY_NAME,
        PhoneLookup.TYPE,
        PhoneLookup.LABEL,
        PhoneLookup.NUMBER,
        PhoneLookup.NORMALIZED_NUMBER,
        PhoneLookup.PHOTO_ID,
        PhoneLookup.LOOKUP_KEY,
        PhoneLookup.PHOTO_URI,
        PhoneLookup.STARRED};
    
    public static final int PERSON_ID = 0;
    public static final int NAME = 1;
    public static final int PHONE_TYPE = 2;
    public static final int LABEL = 3;
    public static final int MATCHED_NUMBER = 4;
    public static final int NORMALIZED_NUMBER = 5;
    public static final int PHOTO_ID = 6;
    public static final int LOOKUP_KEY = 7;
    public static final int PHOTO_URI = 8;
    public static final int STARRED = 9;
	
    public static Uri queryContactUriByPhoneNumber(Context context, String number) {
		Log.d(TAG,"queryContactUriByPhoneNumber,number:"+number);
		if (TextUtils.isEmpty(number)) {
			return null;
		}	

		// The "contactNumber" is a regular phone number, so use the PhoneLookup table.
		Uri uri = Uri.withAppendedPath(/*PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI*/Uri.parse("content://com.android.contacts/hb_phone_lookup_enterprise"),//update by liyang
				Uri.encode(number));

		Cursor phonesCursor =
				context.getContentResolver().query(uri, PROJECTION, null, null, "_id DESC");

		if (phonesCursor != null) {
			try {
				if (phonesCursor.moveToFirst()) {
					long contactId = phonesCursor.getLong(PERSON_ID);
					String lookupKey = phonesCursor.getString(LOOKUP_KEY);
					return Contacts.getLookupUri(contactId, lookupKey);
				} 
			} finally {
				phonesCursor.close();
			}
		}
        return null;
	}
}