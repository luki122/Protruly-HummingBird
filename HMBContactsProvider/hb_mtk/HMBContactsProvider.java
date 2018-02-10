package com.hb.csp.contactsprovider;

import com.android.providers.contacts.AbstractContactsProvider;
import com.android.providers.contacts.ContactsTransaction;
import com.android.providers.hb.SubPermissions;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class HMBContactsProvider extends AbstractContactsProvider{

	@Override
	public boolean onCreate() {
		return super.onCreate();
		// M: MoMS for controling database access ability
//        setMoMSPermission(SubPermissions.QUERY_CONTACTS, SubPermissions.MODIFY_CONTACTS);
	}
	
	@Override
	public void onBegin() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCommit() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRollback() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected SQLiteOpenHelper getDatabaseHelper(Context context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ThreadLocal<ContactsTransaction> getTransactionHolder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Uri insertInTransaction(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int deleteInTransaction(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int updateInTransaction(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected boolean yield(ContactsTransaction transaction) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void notifyChange() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

}
