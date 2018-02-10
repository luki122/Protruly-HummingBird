package com.hb.interception.activity;

import hb.app.dialog.AlertDialog;

import java.util.ArrayList;
import java.util.List;

import com.hb.interception.R;
import android.content.ContentProviderOperation;
import android.content.DialogInterface;
import android.database.Cursor;
import android.provider.ContactsContract.Contacts.Entity;
import android.widget.TextView;
import android.widget.Toast;

import com.hb.interception.adapter.BlackAdapter;
import com.hb.interception.database.BlackItem;
import com.hb.interception.util.BlackUtils;
import com.hb.interception.util.ContactUtils;

public class WhiteList extends ListBase {
	private static final String TAG = "WhiteList";

	protected void init() {
		mIsWhite = true;
		mListUri = BlackUtils.WHITE_URI;
		super.init();		
		TextView EmptyText = (TextView) findViewById(R.id.name_empty_text);
		EmptyText.setText(R.string.white_list_empty);
	}

	protected void startQuery() {
		mQueryHandler.startQuery(0, null, mListUri, null, null, null,
				"_id desc");

	}

	protected void showRemoveDialog(final int pos) {
		if (mDeleteDialog != null) {
			mDeleteDialog.dismiss();
			mDeleteDialog = null;
		}

		AlertDialog dialogs = new AlertDialog.Builder(mContext)
				.setTitle(
						mContext.getResources()
								.getString(R.string.white_remove))
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
		mContext.getContentResolver().delete(mListUri,
				getPhoneNumberEqualString(number), null);
	}
	
	protected ContentProviderOperation getDeleteOperation(String number, int black) {
		return ContentProviderOperation.newDelete(mListUri)
			    .withSelection(getPhoneNumberEqualString(number), null)
				.withYieldAllowed(true)
				.build();
	}

	protected void delAllSelected() {
//		AlertDialog dialogs = new AlertDialog.Builder(WhiteList.this)
//				.setTitle(R.string.white_remove)
//				.setMessage(R.string.white_remove_dialog_multi_message)
//				.setPositiveButton(R.string.remove_confirm,
//						new DialogInterface.OnClickListener() {
//							@Override
//							public void onClick(DialogInterface dialog,
//									int whichButton) {
//								deleteSelectedInternal(0);
//							}
//						}).setNegativeButton(android.R.string.cancel, null)
//				.show();
//		dialogs.setCanceledOnTouchOutside(false);
		deleteSelectedInternal(0);
	}

	protected void initToolBar() {
		super.initToolBar();
		myToolbar.setTitle(R.string.white_list);
	}

	protected List<BlackItem> getSaveList(long[] dataIds) {
		return ContactUtils.getWhiteItemListByDataId(mContext, dataIds,
				mNumberList);
	}

	protected ContentProviderOperation getOperation(String number, String name,
			long rawContactId, long dataId) {
		return ContentProviderOperation.newInsert(mListUri)
				.withValue("number", number)
			    .withValue("white_name", name)
				.withValue(Entity.RAW_CONTACT_ID, rawContactId)
				.withValue(Entity.DATA_ID, dataId).withYieldAllowed(true)
				.build();
	}

	protected List<BlackItem> getSaveSmsList(ArrayList<String> numberList) {
		return ContactUtils.getBlackItemListByNumber(mContext, numberList,
				mNumberList);
	}
}
