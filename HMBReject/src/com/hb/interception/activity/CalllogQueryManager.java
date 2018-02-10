package com.hb.interception.activity;

import java.util.List;

import hb.app.HbActivity;

import com.hb.interception.adapter.PhoneAdapter;
import com.hb.interception.database.CallLogDatabaseHelper;
import com.hb.interception.database.CallLogEntity;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.CallLog.Calls;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.hb.interception.R;
import hb.widget.HbListView;

public class CalllogQueryManager {

	private AsyncQueryHandler mQueryHandler;
	private InterceptionActivity mActivity;
	private static Uri mCallsUri = Calls.CONTENT_URI;
	private static Uri uriName = ContactsContract.Contacts.CONTENT_URI;
	private static Uri uriNumber = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
	private ContentResolver mCr;
	private boolean isQuery = false;
	private boolean callFlag = false;
	private PhoneAdapter mCallListAdapter = null;

	public CalllogQueryManager(InterceptionActivity context) {
		mActivity = context;
		mCr = context.getContentResolver();
		mQueryHandler = new QueryHandler(mCr, context);
		mCr.registerContentObserver(mCallsUri, true, changeObserver);
		mCr.registerContentObserver(uriName, true, changeObserver);
		mCr.registerContentObserver(uriNumber, true, changeObserver);
	}

	void startQuery() {
		isQuery = true;
		mQueryHandler.startQuery(0, null, mCallsUri, null,
				"type in (1,3) and reject >= 1", null , "_id desc");
		callFlag = false;

	}

	boolean isQuery() {
		return isQuery;
	}

	void destroy() {
		mCr.unregisterContentObserver(changeObserver);
		if(mCallListAdapter != null) {
			mCallListAdapter.changeCursor(null);
		}
	}

	boolean isShouldRequery() {
		return callFlag;
	}

	private ContentObserver changeObserver = new ContentObserver(new Handler()) {

		@Override
		public void onChange(boolean selfUpdate) {
			if (mActivity.isResumed() && !mActivity.mInterceptionItemClickHelper.callBath) {
				startQuery();
			} else {
				callFlag = true;
			}
		}
	};

	private List<CallLogEntity> mCallLogEntityList;
	private MatrixCursor callCursor = null;

	private Handler mCallHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			String[] tableCursor = new String[] { "_id", "number", "date",
					"area", "count", "reject", "name", "lable", "slotid" , "mark", "black_name", "user_mark"};
			callCursor = new MatrixCursor(tableCursor);
			for (int i = 0; i < mCallLogEntityList.size(); i++) {
				callCursor
						.addRow(new Object[] { i,
								mCallLogEntityList.get(i).getDBPhomeNumber(),
								mCallLogEntityList.get(i).getLastCallDate(),
								mCallLogEntityList.get(i).getArea(),
								mCallLogEntityList.get(i).getCount(),
								mCallLogEntityList.get(i).getReject(),
								mCallLogEntityList.get(i).getName(),
								mCallLogEntityList.get(i).getLable(),
								mCallLogEntityList.get(i).getSlotId(),
								mCallLogEntityList.get(i).getMark(),
								mCallLogEntityList.get(i).getBlackName(),
								mCallLogEntityList.get(i).getUserMark()});
			}
			if (mCallListAdapter == null) {
				callCursor.moveToFirst();
				mCallListAdapter = new PhoneAdapter(mActivity,
						callCursor);
				mActivity.setPhoneAdapter(mCallListAdapter);
				mCalllogList.setAdapter(mCallListAdapter);
				updateState(SHOW_LIST);
			} else {
				mCallListAdapter.changeCursor(callCursor);
				mCallListAdapter.notifyDataSetChanged();
				updateState(SHOW_LIST);
			}
			isQuery = false;
		};
	};

	private class QueryHandler extends AsyncQueryHandler {
		private final Context context;

		public QueryHandler(ContentResolver cr, Context context) {
			super(cr);
			this.context = context;
		}

		@Override
		protected void onQueryComplete(int token, Object cookie,
				final Cursor cursor) {
			// TODO Auto-generated method stub
			super.onQueryComplete(token, cookie, cursor);
			if (cursor != null) {
				if (!cursor.moveToFirst()) {
					updateState(SHOW_EMPTY);
					isQuery = false;
					cursor.close();
				} else {
					new Thread() {
						public void run() {
							mCallLogEntityList = CallLogDatabaseHelper.queryCallLogs(cursor, context);
							mCallHandler.obtainMessage().sendToTarget();
						};
					}.start();

				}
			} else {
				if (mCallListAdapter != null) {
					mCallListAdapter.changeCursor(null);
					mCallListAdapter.notifyDataSetChanged();
				}
				updateState(SHOW_EMPTY);
				isQuery = false;
			}

		}

		@Override
		protected void onUpdateComplete(int token, Object cookie, int result) {
			// TODO Auto-generated method stub
			super.onUpdateComplete(token, cookie, result);
		}

		@Override
		protected void onInsertComplete(int token, Object cookie, Uri uri) {
			// TODO Auto-generated method stub
			super.onInsertComplete(token, cookie, uri);
		}

		@Override
		protected void onDeleteComplete(int token, Object cookie, int result) {
			// TODO Auto-generated method stub
			super.onDeleteComplete(token, cookie, result);
			System.out.println("删除完毕" + result);
		}

	}
	
	View phone;
	private HbListView mCalllogList;
	private View mCallogEmpty;
	void setView(View v){
		phone = v;
		mCalllogList=(HbListView) phone.findViewById(R.id.phone_list);
		mCallogEmpty= phone.findViewById(R.id.phone_empty);
	}
	
	private static final int SHOW_BUSY = 0;
	private static final int SHOW_LIST = 1;
	private static final int SHOW_EMPTY = 2;

	private int mState;
	private void updateState(int state) {
		if (mState == state) {
			return;
		}

		mState = state;
		switch (state) {
		case SHOW_LIST:
			mCallogEmpty.setVisibility(View.GONE);
			mCalllogList.setVisibility(View.VISIBLE);
			break;
		case SHOW_EMPTY:
			mCalllogList.setVisibility(View.GONE);
			mCallogEmpty.setVisibility(View.VISIBLE);
			break;
		case SHOW_BUSY:
			mCalllogList.setVisibility(View.GONE);
			mCallogEmpty.setVisibility(View.GONE);
			break;
									
		}
	}
}