package com.hb.interception.activity;

import android.content.ContentProviderOperation;
import android.provider.ContactsContract.Contacts.Entity;

import com.hb.interception.util.BlackUtils;

public class AddWhiteByNumber extends AddByNumberBase {
	private static final String TAG = "AddWhiteByNumber";
	
	protected ContentProviderOperation getOperation(String number, String name,
			long rawContactId, long dataId) {
		return ContentProviderOperation
				.newInsert(BlackUtils.WHITE_URI)
				.withValue("number", number)
			    .withValue("white_name", name)
				.withValue(Entity.RAW_CONTACT_ID, rawContactId)
				.withValue(Entity.DATA_ID, dataId)
				.withYieldAllowed(true).build();
	}
}
