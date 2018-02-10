package com.hb.interception.activity;

import hb.app.dialog.AlertDialog;

import java.util.ArrayList;
import java.util.List;

import com.hb.interception.R;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.provider.ContactsContract.Contacts.Entity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.hb.interception.adapter.BlackAdapter;
import com.hb.interception.database.BlackItem;
import com.hb.interception.util.BlackUtils;
import com.hb.interception.util.ContactUtils;

public class BlackList extends ListBase {
	private static final String TAG = "BlackList";

	protected void init() {
		mListUri = BlackUtils.BLACK_URI;
		super.init();
	}

	protected void startQuery() {
		mQueryHandler.startQuery(0, null, mListUri, null, "isblack=1", null,
				"_id desc");

	}

	protected void showRemoveDialog(final int pos) {
		if (mDeleteDialog != null) {
			mDeleteDialog.dismiss();
			mDeleteDialog = null;
		}

		View view = LayoutInflater.from(mContext).inflate(
				R.layout.black_remove, null);

		AlertDialog dialogs = new AlertDialog.Builder(mContext)
				.setTitle(
						mContext.getResources()
								.getString(R.string.black_remove))
				.setView(view)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								deleteSingleInternal(pos, 0);
							}
						}).setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	protected void deleteValues(String number, int black) {
		ContentResolver cr = getContentResolver();
		ContentValues cv = new ContentValues();
		cv.put("isblack", black);
		cv.put("number", number);
		cv.put("reject", 3);
		cr.update(BlackUtils.BLACK_URI, cv, getPhoneNumberEqualString(number),
				null);
	}
	
	protected ContentProviderOperation getDeleteOperation(String number, int black) {
		return ContentProviderOperation.newUpdate(mListUri)
				.withValue("isblack", black)
				.withValue("number", number)
				.withValue("reject", 3)
			    .withSelection(getPhoneNumberEqualString(number), null)
				.withYieldAllowed(true)
				.build();
	}

	protected void delAllSelected() {
		View view = LayoutInflater.from(mContext).inflate(
				R.layout.black_remove, null);
		// String title = null;
		AlertDialog dialogs = new AlertDialog.Builder(BlackList.this)
				.setTitle(R.string.black_remove)
				.setView(view)
				// .setMessage(R.string.black_remove_dialog_multi_message)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								deleteSelectedInternal(0);
							}
						}).setNegativeButton(android.R.string.cancel, null)
				.show();
		dialogs.setCanceledOnTouchOutside(false);
//		deleteSelectedInternal(0);
	}

	protected void initToolBar() {
		super.initToolBar();
		myToolbar.setTitle(R.string.black);
	}

	protected List<BlackItem> getSaveList(long[] dataIds) {
		return ContactUtils.getBlackItemListByDataId(mContext, dataIds,
				mNumberList);
	}

	protected ContentProviderOperation getOperation(String number, String name,
			long rawContactId, long dataId) {
		return ContentProviderOperation.newInsert(mListUri)
				.withValue("isblack", 1)
				.withValue("number", number)
				.withValue("reject", 3)
	            .withValue("black_name", name)
				// except add manually,other will default 3
				.withValue(Entity.RAW_CONTACT_ID, rawContactId)
				.withValue(Entity.DATA_ID, dataId).withYieldAllowed(true)
				.build();
	}
	
	protected List<BlackItem> getSaveSmsList(ArrayList<String> numberList) {
		return ContactUtils.getBlackItemListByNumber(mContext, numberList,
				mNumberList);
	}
}
