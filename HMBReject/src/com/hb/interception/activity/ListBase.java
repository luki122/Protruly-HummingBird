package com.hb.interception.activity;

import hb.app.dialog.AlertDialog;
import hb.app.dialog.ProgressDialog;
import hb.view.menu.BottomWidePopupMenu;
import hb.view.menu.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;
import hb.widget.HbListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hb.interception.R;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.ContactsContract.Contacts.Entity;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hb.interception.adapter.BlackAdapter;
import com.hb.interception.database.BlackItem;
import com.hb.interception.util.BlackUtils;
import com.hb.interception.util.FormatUtils;
import com.hb.interception.util.InterceptionUtils;

public class ListBase extends InterceptionActivityBase {
	private static final String TAG = "ListBase";

	protected boolean mIsWhite = false;
	protected List<String> mNumberList = new ArrayList<String>();
	private String mIso;
	private ContentResolver mCr;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHbContentView(R.layout.black_name_list);
		init();
	}

	protected void init() {
		mList = (HbListView) findViewById(R.id.black_name_list);
		mList.setItemsCanFocus(false);
		mList.setFastScrollEnabled(false);
		mList.setFastScrollAlwaysVisible(false);
		mEmpty =  findViewById(R.id.black_name_empty);
		super.init();
		mIso = FormatUtils.getCurrentCountryIso(this);
		mCr = getContentResolver();
		mCr.registerContentObserver(mListUri, true, changeObserver);
	}

	private ContentObserver changeObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfUpdate) {
				startQuery();
		}
	};
	
	@Override
	protected void processQueryComplete(Context context, Cursor cursor) {
		createList(cursor);

		if (cursor != null) {
		     if (mAdapter == null) {
				mAdapter = new BlackAdapter(context, cursor);
				mAdapter.setIsWhite(mIsWhite);
				mAdapter.setListener(this);
				mList.setAdapter(mAdapter);
			} else {
				mAdapter.changeCursor(cursor);
				mAdapter.notifyDataSetChanged();
			}
		} else {
			if (mAdapter != null) {
				mAdapter.changeCursor(null);
				mAdapter.notifyDataSetChanged();
			}
		}

	}

	private void createList(Cursor cursor) {
		mNumberList.clear();
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				do {
					String number = cursor.getString(cursor
							.getColumnIndex("number"));
					if (number == null) {
						continue;
					}
					String numberE164 = PhoneNumberUtils.formatNumberToE164(
							number, mIso);
					Log.i(TAG, "numberE164=" + numberE164);
					mNumberList.add(number);

					if (numberE164 != null && !number.equals(numberE164)) {
						mNumberList.add(numberE164);
						continue;
					}

					try { // modify in the future
						if (numberE164 != null
								&& numberE164.equals(number)
								&& mIso
										.equals("CN")
								&& number.startsWith("+86")) {
							numberE164 = number.substring(3, number.length());
							mNumberList.add(numberE164);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

				} while (cursor.moveToNext());
				cursor.moveToFirst();
			}
		}
	}

	protected void showDialogMenu(final int pos) {
		showRemoveDialog(pos);
	}

	protected void showRemoveDialog(final int pos) {
	}

	protected void deleteSingleInternal(int pos, int isblack) {
		Cursor cursor = (Cursor) mList.getItemAtPosition(pos);

		String targetNumber = cursor.getString(cursor
				.getColumnIndex("number"));
		deleteValues(targetNumber, isblack);
		
		Toast.makeText(mContext, "已移除", Toast.LENGTH_LONG).show();
	}
	
	protected void deleteValues(String number, int black) {}

	protected void deleteSelectedInternal(final int isblack) {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected void onPreExecute() {
				showDialog();
			}

			@Override
			protected Void doInBackground(Void... params) {
				List<String> nums = new ArrayList<String>();
				for (int pos : mAdapter.getCheckedItem()) {
					Cursor cursor = (Cursor) mList.getItemAtPosition(pos);
					nums.add(cursor.getString(cursor.getColumnIndex("number")));
				}
				ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
				for (String num : nums) {
					ops.add(getDeleteOperation(num, isblack));
					if (ops.size() + 1 >= 1000) {
						applyBatch(ops);
					}
				}
				if (ops.size() > 0) {
					applyBatch(ops);
				}
				try {
					Thread.sleep(InterceptionUtils.MIN_DIALOG_SHOW_TIME);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
			
			private void applyBatch(ArrayList<ContentProviderOperation> ops) {
				try {
					getContentResolver().applyBatch(BlackUtils.HB_CONTACT_AUTHORITY,
							ops);
					ops.clear();
				} catch (RemoteException e) {
					e.printStackTrace();
				} catch (OperationApplicationException e) {
					e.printStackTrace();
				} catch (SQLiteConstraintException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			protected void onPostExecute(Void result) {
				hideDialog();
//				startQuery();
				changeToNormalMode(true);
			}
		}.execute();
		
	}
	
	protected ContentProviderOperation getDeleteOperation(String number, int black) {
		return null;
	}

	protected String getPhoneNumberEqualString(String number) {
		return " PHONE_NUMBERS_EQUAL(number, \"" + number + "\", 0) ";
	}

	protected AlertDialog mDeleteDialog = null;

	protected void initBottomMenuAndActionbar() {
		super.initBottomMenuAndActionbar();
		mBottomNavigationView
				.setNavigationItemSelectedListener(new OnNavigationItemSelectedListener() {
					@Override
					public boolean onNavigationItemSelected(MenuItem item) {
						switch (item.getItemId()) {
						case R.id.delete:
							delAllSelected();
							return true;
						default:
							return false;
						}
					}
				});
	}

	protected void delAllSelected() {
	}

	protected void initToolBar() {
		super.initToolBar();
		myToolbar.inflateMenu(R.menu.toolbar_menu_add);
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onMenuItemClick--->" + item.getTitle());
		if (item.getItemId() == R.id.add) {
			BottomWidePopupMenu menu = new BottomWidePopupMenu(this);
			menu.inflateMenu(R.menu.bottom_menu_addnumber);
			menu.setOnMenuItemClickedListener(new BottomWidePopupMenu.OnMenuItemClickListener() {
				@Override
				public boolean onItemClicked(MenuItem item) {
					// TODO Auto-generated method stub
					int id = item.getItemId();
					switch (id) {
					case R.id.phone:
						addFromCalllog();
						break;
					case R.id.contact:
						launchMultiplePhonePicker();
						break;
					case R.id.sms:
						addFromSms();
						break;
					case R.id.manually:
						addManually();
						break;
					}
					return true;
				}
			});
			if(!isFinishing()) menu.show();

			return true;
		}
		return false;
	}

	private void addFromCalllog() {
		final Intent callLogIntent = new Intent(
				mIsWhite ? "com.hb.add.white.by.calllog"
						: "com.hb.add.black.by.calllog");
		StringBuilder numbeSql = new StringBuilder();
		for (String num : mNumberList) {
			numbeSql.append("'").append(num).append("',");
		}
		if (!TextUtils.isEmpty(numbeSql)) {
			callLogIntent.putExtra("blacknumbers", numbeSql.toString()
					.substring(0, numbeSql.toString().length() - 1));
		}
		startActivity(callLogIntent);
	}

	private static final int REQUEST_CODE_SMS_PICK = 2;
	private static String RESULT_INTENT_EXTRA_NUMBER = "result_intent_extra_number";
	private void addFromSms() {
		Intent intent = new Intent(
				"android.intent.action.conversation.list.PICKMULTIPHONES");
		intent.setType("vnd.android.cursor.dir/phone");
//		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		//startActivity(intent);
		try {
			startActivityForResult(intent, REQUEST_CODE_SMS_PICK);
		} catch (ActivityNotFoundException ex) {
		}
	}

	private void addManually() {
		Intent intent = new Intent(getApplicationContext(),
				AddBlackManually.class);
		intent.putExtra("white", mIsWhite);
		startActivity(intent);
	}

	public static final int REQUEST_CODE_CONTACT_PHONE_PICK = 1;
	protected static final String RESULT_INTENT_EXTRA_DATA_NAME = "com.mediatek.contacts.list.pickdataresult";

	protected void launchMultiplePhonePicker() {
		Intent intent = new Intent(
				"android.intent.action.contacts.list.PICKMULTIPHONES");
		intent.setType("vnd.android.cursor.dir/phone");
		intent.putExtra("hbFilter","blacklist");
		try {
			startActivityForResult(intent, REQUEST_CODE_CONTACT_PHONE_PICK);
		} catch (ActivityNotFoundException ex) {
		}
	}

	@Override
	protected void onActivityResult(int maskResultCode, int resultCode,
			Intent data) {
		if (resultCode != RESULT_OK) {
			return;
		}
		switch (maskResultCode) {
		case REQUEST_CODE_CONTACT_PHONE_PICK:
			// depend on data id to query name and phonenumber to insert black
			final long[] dataIds = data
					.getLongArrayExtra(RESULT_INTENT_EXTRA_DATA_NAME);
			if (dataIds == null || dataIds.length <= 0) {
				return;
			}

//			Log.d(TAG,"dataIds:"+Arrays.toString(dataIds));
			doSaveFromContact(dataIds);
			break;
			
		case REQUEST_CODE_SMS_PICK:
			ArrayList<String> numberList = data.getStringArrayListExtra(RESULT_INTENT_EXTRA_NUMBER);
			if (numberList == null || numberList.size() <= 0) {
				return;
			}
			doSaveFromSms(numberList);
			break;
		default:
			break;
		}
	}

	protected void doSaveFromContact(long[] dataIds) {
		List<BlackItem> list = getSaveList(dataIds);
		//Log.d(TAG,"list:"+list);
		new AsyncTask<List<BlackItem>, Void, Void>() {

			private ProgressDialog mInsertPregressDialog;

			private void showInsertDialog() {
				mInsertPregressDialog = new ProgressDialog(mContext);
				mInsertPregressDialog.setIndeterminate(true);
				mInsertPregressDialog.setCancelable(false);
				mInsertPregressDialog.setMessage(getResources().getString(
						R.string.save_title));
				mInsertPregressDialog.show();
			}

			private void hideInsertDialog() {
				if (mInsertPregressDialog != null && !isFinishing()) {
					mInsertPregressDialog.dismiss();
					mInsertPregressDialog = null;
				}
			}

			@Override
			protected void onPreExecute() {
				showInsertDialog();
				Log.d(TAG,"add contat black start");
			}

			@Override
			protected Void doInBackground(List<BlackItem>... params) {
				List<BlackItem> mPickBlackItem = params[0];
				if (mPickBlackItem == null || mPickBlackItem.size() == 0) {
					return null;
				}
				ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
				for (BlackItem item : mPickBlackItem) {
					long rawContactId = item.getmRawContactId();
					long dataId = item.getmDataId();
					ops.add(getOperation(item.getmNumber(), item.getmBlackName(), rawContactId,
							dataId));
				}
				try {
					getContentResolver().applyBatch(BlackUtils.HB_CONTACT_AUTHORITY,
							ops);
				} catch (RemoteException e) {
					e.printStackTrace();
				} catch (OperationApplicationException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(InterceptionUtils.MIN_DIALOG_SHOW_TIME);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				Log.d(TAG,"add contat black end");
				hideInsertDialog();
			}
		}.execute(list);
	}

	protected List<BlackItem> getSaveList(long[] dataIds) {
		return null;
	}

	protected ContentProviderOperation getOperation(String number, String name,
			long rawContactId, long dataId) {
		return null;
	}

	protected void showToast(int resId) {
		Toast.makeText(mContext, mContext.getResources().getString(resId),
				Toast.LENGTH_LONG).show();
	}

	private void doSaveFromSms(ArrayList<String> numberList) {
		List<BlackItem> list = getSaveSmsList(numberList);
		Log.d(TAG, "list:" + list);
		new AsyncTask<List<BlackItem>, Void, Void>() {

			private ProgressDialog mInsertPregressDialog;

			private void showInsertDialog() {
				mInsertPregressDialog = new ProgressDialog(mContext);
				mInsertPregressDialog.setIndeterminate(true);
				mInsertPregressDialog.setCancelable(false);
				mInsertPregressDialog.setMessage(getResources().getString(
						R.string.save_title));
				mInsertPregressDialog.show();
			}

			private void hideInsertDialog() {
				if (mInsertPregressDialog != null && !isFinishing()) {
					mInsertPregressDialog.dismiss();
					mInsertPregressDialog = null;
				}
			}

			@Override
			protected void onPreExecute() {
				showInsertDialog();
				Log.d(TAG, "add contat black start");
			}

			@Override
			protected Void doInBackground(List<BlackItem>... params) {
				List<BlackItem> mPickBlackItem = params[0];
				if (mPickBlackItem == null || mPickBlackItem.size() == 0) {
					return null;
				}
				ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
				for (BlackItem item : mPickBlackItem) {
					long rawContactId = item.getmRawContactId();
					long dataId = item.getmDataId();
					ops.add(getOperation(item.getmNumber(),
							item.getmBlackName(), rawContactId, dataId));
				}
				try {
					getContentResolver().applyBatch(
							BlackUtils.HB_CONTACT_AUTHORITY, ops);
				} catch (RemoteException e) {
					e.printStackTrace();
				} catch (OperationApplicationException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(InterceptionUtils.MIN_DIALOG_SHOW_TIME);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				Log.d(TAG, "add contat black end");
				hideInsertDialog();
			}
		}.execute(list);
	}
	
	protected List<BlackItem> getSaveSmsList(ArrayList<String> numberList) {
		return null;
	}
	
	  @Override
	    protected void onDestroy() {
	        // TODO Auto-generated method stub
	        super.onDestroy();
	        mCr.unregisterContentObserver(changeObserver);
	    }
}
