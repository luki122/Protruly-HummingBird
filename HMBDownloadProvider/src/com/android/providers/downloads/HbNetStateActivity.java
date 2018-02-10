package com.android.providers.downloads;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import com.android.downloadui.LogUtil;
import android.app.Activity;
import hb.app.dialog.AlertDialog;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Downloads;
import android.provider.Downloads.Impl;

public class HbNetStateActivity extends Activity implements DialogInterface.OnCancelListener, DialogInterface.OnClickListener {
	public static final int DIALOG_TYPE_NO_NETWORK = 1;
	public static final int DIALOG_TYPE_MOBILE_CONNECT = 2;
	private static final String TAG = "HbNetStateActivity";
	private AlertDialog mDialog;
	private Queue<Intent> mDownloadsToShow = new LinkedList<Intent>();
	private Uri mCurrentUri;
	private Intent mCurrentIntent;
	private boolean onClickCalled;
	private int mDialogType = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);
		LogUtil.i(TAG, "---onNewIntent()--");
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		Intent intent = getIntent();
		if (intent != null) {
			/// M: Show dialog once for same download. fix issue: 2334425 @{
			if ((mDialog != null && mDialog.isShowing())
					&& (mCurrentUri != null && mCurrentUri.equals(intent.getData()))) {
				LogUtil.i(TAG, "Dialog is showing for the same download, mCurrentUri is " + mCurrentUri);
				return;
			}
			/// @}
			LogUtil.i(TAG, "---onResume()--add intent-- uri = " + intent.getData());
			mDownloadsToShow.add(intent);
			setIntent(null);
			showNextDialog();
		}
		if (mDialog != null && !mDialog.isShowing()) {
			mDialog.show();
		}
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	private void showNextDialog() {
		if (mDialog != null) {
			return;
		}
		LogUtil.i(TAG, "---showNextDialog()---mDownloadsToShow.size = " + mDownloadsToShow.size());
		if (mDownloadsToShow.isEmpty()) {
			finish();
			return;
		}

		mCurrentIntent = mDownloadsToShow.poll();
		mDialogType = mCurrentIntent.getIntExtra("dialogType", 0);
		mCurrentUri = mCurrentIntent.getData();
		Cursor cursor = getContentResolver().query(mCurrentUri, null, null, null, null);
		try {
			if (!cursor.moveToFirst()) {
				LogUtil.i(Constants.TAG, "Empty cursor for URI " + mCurrentUri);
				dialogClosed();
				return;
			}
			showDialog(cursor);
		} finally {
			cursor.close();
		}
	}

	private void showDialog(Cursor cursor) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert);
		if (mDialogType == DIALOG_TYPE_NO_NETWORK) {
			builder.setTitle(R.string.hmb_dialog_title).setMessage(getString(R.string.hmb_no_network_connection))
					.setPositiveButton(R.string.ok, this);
		} else if (mDialogType == DIALOG_TYPE_MOBILE_CONNECT) {
			builder.setTitle(R.string.hmb_dialog_title).setMessage(getString(R.string.hmb_mobile_download))
					.setPositiveButton(R.string.hmb_download_continue, this)
					.setNegativeButton(R.string.hmb_waiting_wifi, this);
		}
		onClickCalled = false;
		mDialog = builder.setOnCancelListener(this).show();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		// TODO Auto-generated method stub
		LogUtil.i(TAG,"---onClick()--onClickCalled = " + onClickCalled + " mDialogType = " + mDialogType);
		if (onClickCalled) {
			return;
		} else {
			onClickCalled = true;
		}
		if (mDialogType == DIALOG_TYPE_MOBILE_CONNECT) {
			if (which == AlertDialog.BUTTON_NEGATIVE) {
				setDownloadForWifi();
			} else if (which == AlertDialog.BUTTON_POSITIVE) {
				setDownloadForMobile();
			}
		}
		dialogClosed();
	}

	private void setDownloadForWifi() {
		List<DownloadInfo> list = HbDownManager.getInstance(this).getWaitConfirmedTasks();
		if (list != null && list.size() > 0) {
			long ids[] = new long[list.size()];
			for (int i = 0; i < list.size(); i++) {
				ids[i] = list.get(i).mId;
			}
			ContentValues values = new ContentValues();
			values.put(Impl.COLUMN_STATUS, Downloads.Impl.STATUS_QUEUED_FOR_WIFI);
			values.put(Impl.COLUMN_ALLOWED_NETWORK_TYPES, DownloadManager.Request.NETWORK_WIFI);
			getContentResolver().update(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, values, getWhereClauseForIds(ids),
					getWhereArgsForIds(ids));
		}
	}

	private void setDownloadForMobile() {
		List<DownloadInfo> list = HbDownManager.getInstance(this).getWaitConfirmedTasks();
		if (list != null && list.size() > 0) {
			long ids[] = new long[list.size()];
			for (int i = 0; i < list.size(); i++) {
				ids[i] = list.get(i).mId;
			}
			ContentValues values = new ContentValues();
			values.put(Downloads.Impl.COLUMN_ALLOWED_NETWORK_TYPES, ~0);
			getContentResolver().update(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, values, getWhereClauseForIds(ids),
					getWhereArgsForIds(ids));
		}
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		// TODO Auto-generated method stub
		LogUtil.i(TAG, "---onCancel()--onClickCalled = " + onClickCalled);
		if (onClickCalled) {
			return;
		}
		if(mDialogType == DIALOG_TYPE_MOBILE_CONNECT){
			setDownloadForWifi();
		}
		dialogClosed();
	}

	private void dialogClosed() {
		LogUtil.i(TAG, "---dialogClosed()-- ");
		mDialog = null;
		mCurrentUri = null;
		showNextDialog();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		LogUtil.i(TAG,"---onDestroy()---");
		List<DownloadInfo> list = HbDownManager.getInstance(this).getWaitConfirmedTasks();
		if (list != null && list.size() > 0) {
			for (DownloadInfo downloadInfo : list) {
				downloadInfo.onDialogDismiss();
			}
			list.clear(); // clear all waiting confirmed task
		}
	}

	private String getWhereClauseForIds(long[] ids) {
		StringBuilder whereClause = new StringBuilder();
		whereClause.append("(");
		for (int i = 0; i < ids.length; i++) {
			if (i > 0) {
				whereClause.append("OR ");
			}
			whereClause.append(Downloads.Impl._ID);
			whereClause.append(" = ? ");
		}
		whereClause.append(")");
		return whereClause.toString();
	}

	private String[] getWhereArgsForIds(long[] ids) {
		String[] whereArgs = new String[ids.length];
		for (int i = 0; i < ids.length; i++) {
			whereArgs[i] = Long.toString(ids[i]);
		}
		return whereArgs;
	}

	public static interface onDialogDismissListener {
		public void onDialogDismiss();
	}

}
