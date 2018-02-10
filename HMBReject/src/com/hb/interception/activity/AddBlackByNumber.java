package com.hb.interception.activity;

import android.content.ContentProviderOperation;
import android.provider.ContactsContract.Contacts.Entity;

import com.hb.interception.util.BlackUtils;

public class AddBlackByNumber extends AddByNumberBase {
	private static final String TAG = "AddBlackByNumber";
	 
	protected ContentProviderOperation getOperation(String number, String name,
			long rawContactId, long dataId) {
		return ContentProviderOperation
				.newInsert(BlackUtils.BLACK_URI)
				.withValue("isblack", 1)
				.withValue("number", number)
			    .withValue("black_name", name)
				.withValue("reject", 3)
				.withValue(Entity.RAW_CONTACT_ID, rawContactId)
				.withValue(Entity.DATA_ID, dataId)
				.withYieldAllowed(true).build();
	}
}
