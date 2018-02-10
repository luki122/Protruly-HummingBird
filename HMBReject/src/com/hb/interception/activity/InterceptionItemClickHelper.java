package com.hb.interception.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import hb.app.dialog.AlertDialog;
import hb.app.dialog.ProgressDialog;
import hb.view.menu.BottomWidePopupMenu;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.DialogInterface.OnKeyListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.CallLog.Calls;
import android.provider.Telephony.Sms;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.hb.interception.InterceptionApplication;
import com.hb.interception.adapter.InterceptionAdapterBase;
import com.hb.interception.util.BlackUtils;
import com.hb.interception.util.FormatUtils;
import com.hb.interception.util.InterceptionUtils;
import com.hb.interception.util.SimUtils;
import com.hb.interception.util.YuloreUtil;
import com.hb.interception.R;

import android.location.CountryDetector;

import android.app.AppOpsManager;

public class InterceptionItemClickHelper {

	private InterceptionActivity mActivity;
	private ContentResolver mContentResolver;
	private String targetNumber;
	private String blackName;
	private int targetSimId;
	private String targetId;
	private int type;
	private String mIso;
	private AppOpsManager mAppOpsManager;
	public InterceptionItemClickHelper(InterceptionActivity context) {
		mActivity = context;
		mContentResolver = context.getContentResolver();
		mIso = FormatUtils.getCurrentCountryIso(context);
		mAppOpsManager = (AppOpsManager)mActivity. getSystemService("appops");
	}

	// onItemClick start
	void initData(Cursor c) {
		targetNumber = c.getString(c.getColumnIndex("number"));
		targetSimId = c.getInt(c.getColumnIndex("simId"));
		targetId = c.getString(c.getColumnIndex("_id"));
		type = c.getInt(c.getColumnIndex("reject"));
		blackName = c.getString(c.getColumnIndex("name"));
	}

	void showDialogMenu() {

		BottomWidePopupMenu menu = new BottomWidePopupMenu(mActivity);
		menu.inflateMenu(R.menu.bottom_calllog_menu);
		menu.setOnMenuItemClickedListener(new BottomWidePopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onItemClicked(MenuItem item) {
				// TODO Auto-generated method stub
				int id = item.getItemId();
				switch (id) {
				case R.id.place_call:
					placeCall();
					break;
				case R.id.send_sms:
					sendSms();
					break;
				case R.id.remove_black:
					removeBlack();
					break;
				/*
				 * case R.id.view_call_detail: viewCallLog(); break; case
				 * R.id.delete_calllog: deleteCallLog(); break;
				 */

				}
				return true;
			}
		});
		menu.show();

	}

	private void placeCall() {
		Intent intents;
		TelephonyManager tm = (TelephonyManager) mActivity
				.getSystemService(Context.TELEPHONY_SERVICE);
		if (tm.isMultiSimEnabled()) {
			intents = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
					+ targetNumber));
			if (SimUtils.isShowDoubleButton(mActivity)) {
				intents.putExtra("slot", targetSimId);
			}
		} else {
			intents = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
					+ targetNumber));
		}
		mActivity.startActivity(intents);
	}

	private void sendSms() {
		Uri uri = Uri.parse("smsto:" + targetNumber);
		Intent it = new Intent(Intent.ACTION_SENDTO, uri);
		mActivity.startActivity(it);
	}

	private void removeBlack() {
		removeBlack(targetNumber);
	}

	void removeBlack(final String number) {
		View view = LayoutInflater.from(mActivity).inflate(
				R.layout.black_phone_remove, null);
		final CheckBox phone = (CheckBox) view.findViewById(R.id.phone);
		phone.setChecked(true);
		AlertDialog dia = new AlertDialog.Builder(mActivity)
				.setTitle(
						mActivity.getResources().getString(
								R.string.confirm_no_reject))
				.setView(view)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								ContentResolver cr = mActivity
										.getContentResolver();
								ContentValues cv = new ContentValues();
								if (phone.isChecked()) {
									cv.put("isblack", 0);

								} else {
									cv.put("isblack", -1);
								}
								cv.put("number", number);
								cv.put("reject", type);
								int uri2 = cr.update(BlackUtils.BLACK_URI, cv,
										"number=?", new String[] { number });
								System.out.println("updated" + ":" + uri2);
								dialog.dismiss();
								Toast.makeText(mActivity,
										R.string.no_reject_success_toast,
										Toast.LENGTH_SHORT).show();
							}
						}).setNegativeButton(android.R.string.cancel, null)
				.show();
		dia.setCanceledOnTouchOutside(false);
	}

	private void viewCallLog() {
		// TODO Auto-generated method stub
		final Intent intent = new Intent();
		// String name = blackName;
		Log.e("System.out", "name = " + blackName + "  number = "
				+ targetNumber);
		intent.setClassName("com.android.contacts",
				"com.android.contacts.FullCallDetailActivity");
		intent.putExtra("number", targetNumber);
		intent.putExtra("black_name", blackName);
		intent.putExtra("reject_detail", true);
		// intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

		String userMark = YuloreUtil.getUserMark(mActivity, targetNumber);
		String markContent = YuloreUtil.getMarkContent(targetNumber, mActivity);
		int markCount = YuloreUtil.getMarkNumber(mActivity, targetNumber);
		intent.putExtra("user-mark", userMark);
		intent.putExtra("mark-content", markContent);
		intent.putExtra("mark-count", markCount);

		try {
			mActivity.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void deleteCallLog() {
		AlertDialog dialog = new AlertDialog.Builder(mActivity)
				.setTitle(
						mActivity.getResources().getString(
								R.string.remove_one_dial_bn))
				.setPositiveButton(R.string.remove_dial_confirm,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								dialog.dismiss();
								deleteOne();
							}
						}).setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void deleteOne() {
		new Thread() {
			public void run() {
				String p = targetNumber;
				String numberE164 = PhoneNumberUtils.formatNumberToE164(p,
						mIso);
				if (numberE164 != null) {
					mContentResolver.delete(Calls.CONTENT_URI,
							"number=? and type in (1,3) and reject >= 1",
							new String[] { numberE164 });
				}
				mContentResolver.delete(Calls.CONTENT_URI,
						"number=? and type in (1,3) and reject >= 1",
						new String[] { p });

			};
		}.start();
	}

	void viewSms(Cursor c) {
	//	final Intent SmsIntent = new Intent();
//		SmsIntent.setClassName("com.android.mms",
//				"com.android.mms.ui.ComposeMessageActivity");
//		SmsIntent.putExtra("thread_id",
//				c.getLong(c.getColumnIndex("thread_id")));
//		SmsIntent.putExtra("isFromReject", true);
		if (c.getInt(c.getColumnIndex("ismms")) == 1) {
			return;
		}
		String smsMsg = c.getString(c.getColumnIndex("body"));
		if (TextUtils.isEmpty(smsMsg)) {
			return;
		}
		final Intent smsIntent = new Intent("com.hb.message_detail");
		smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		smsIntent.putExtra("msgBody", smsMsg);
		try {
			mActivity.startActivity(smsIntent);
		} catch (ActivityNotFoundException a) {
			a.printStackTrace();
		}
	}

	// onNavigationItemSelected start

	boolean handleBottomMenuItemClick(MenuItem item) {

		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.delCall:
			deleteSelectedCallLog();
			return true;
		case R.id.delSms:
			deleteSelectedSms();
			return true;
		case R.id.restoreCall:
			recoverSelectedCall();
			return true;
		case R.id.restoreSms:
			recoverSelectedSms();
			return true;
		}
		return false;
	}

	private void deleteSelectedCallLog() {
		boolean isHasBlack = getSelectBlackReject(false).size() > 0;
		if (isHasBlack) {
			View view = LayoutInflater.from(mActivity).inflate(
					R.layout.black_log_recovery_delete_confirm, null);
			TextView title =(TextView) view.findViewById(R.id.title);
			title.setText(R.string.multi_delete_calllog_reject_dialg_msg);
			final CheckBox phone = (CheckBox) view.findViewById(R.id.phone);
			phone.setChecked(true);
			AlertDialog dialogs = new AlertDialog.Builder(mActivity)
					// .setTitle(R.string.recove_reject_calls_title)
					.setView(view)
					// .setMessage(R.string.recove_reject_calls_message)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {
									deleteSelectedCallLogInternal(phone.isChecked());
								}
							}).setNegativeButton(android.R.string.cancel, null)
					.show();
			dialogs.setCanceledOnTouchOutside(false);
		} else {
		AlertDialog dialogs = new AlertDialog.Builder(mActivity)
				.setTitle(R.string.delete_reject)
				.setMessage(R.string.multi_delete_calllog_reject_dialg_msg)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								deleteSelectedCallLogInternal();
							}
						}).setNegativeButton(android.R.string.cancel, null)
				.show();
		dialogs.setCanceledOnTouchOutside(false);
		}
	}

	private void recoverSelectedCall() {
		boolean isHasBlack = getSelectBlackReject(false).size() > 0;
		if (isHasBlack) {
			View view = LayoutInflater.from(mActivity).inflate(
					R.layout.black_log_recovery_delete_confirm, null);
			final CheckBox phone = (CheckBox) view.findViewById(R.id.phone);
			phone.setChecked(true);
			AlertDialog dialogs = new AlertDialog.Builder(mActivity)
					// .setTitle(R.string.recove_reject_calls_title)
					.setView(view)
					// .setMessage(R.string.recove_reject_calls_message)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {
									recoverCallsInternal(phone.isChecked());
								}
							}).setNegativeButton(android.R.string.cancel, null)
					.show();
			dialogs.setCanceledOnTouchOutside(false);
		} else {
			AlertDialog dialogs = new AlertDialog.Builder(mActivity)
					.setTitle(R.string.recove_reject_calls_title)
					.setMessage(R.string.recove_reject_calls_message)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {
									recoverCallsInternal();
								}
							}).setNegativeButton(android.R.string.cancel, null)
					.show();
			dialogs.setCanceledOnTouchOutside(false);
		}
	}

	private void deleteSelectedSms() {
		boolean isHasBlack = getSelectBlackReject(true).size() > 0;
		if (isHasBlack) {
			View view = LayoutInflater.from(mActivity).inflate(
					R.layout.black_log_recovery_delete_confirm, null);
			TextView title =(TextView) view.findViewById(R.id.title);
			title.setText(R.string.multi_delete_sms_reject_dialg_msg);
			final CheckBox phone = (CheckBox) view.findViewById(R.id.phone);
			phone.setChecked(true);
			AlertDialog dias = new AlertDialog.Builder(mActivity)
					.setView(view)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {
									delSelectedSmsInternal(phone.isChecked());
								}
							}).setNegativeButton(android.R.string.cancel, null)
					.show();
			dias.setCanceledOnTouchOutside(false);
		} else {
			AlertDialog dias = new AlertDialog.Builder(mActivity)
				.setTitle(R.string.delete_reject)
				.setMessage(R.string.multi_delete_sms_reject_dialg_msg)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								dialog.dismiss();
								delSelectedSmsInternal();
							}
						}).setNegativeButton(android.R.string.cancel, null)
				.show();
		dias.setCanceledOnTouchOutside(false);
		}
	}

	private void recoverSelectedSms() {
		boolean isHasBlack = getSelectBlackReject(true).size() > 0;
		if (isHasBlack) {
			View view = LayoutInflater.from(mActivity).inflate(
					R.layout.black_log_recovery_delete_confirm, null);
			TextView title =(TextView) view.findViewById(R.id.title);
			title.setText(R.string.multi_recover_sms_reject_dialg_msg);
			final CheckBox phone = (CheckBox) view.findViewById(R.id.phone);
			phone.setChecked(true);
			AlertDialog dialog = new AlertDialog.Builder(mActivity)
					.setView(view)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {
									recoverSmsInternal(phone.isChecked());
								}
							}).setNegativeButton(android.R.string.cancel, null)
					.show();
			dialog.setCanceledOnTouchOutside(false);
		} else {
		AlertDialog dialog = new AlertDialog.Builder(mActivity)
				// .setTitle(mActivity.getResources().getString(R.string.recover_sms))
				.setTitle(R.string.recover_sms_dlg_title)
				.setMessage(R.string.multi_recover_sms_reject_dialg_msg)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								dialog.dismiss();
								recoverSmsInternal();
							}
						}).setNegativeButton(android.R.string.cancel, null)
				.show();
		dialog.setCanceledOnTouchOutside(false);
		}
	}

	boolean smsBath = false;
	boolean callBath = false;
	private Handler mDialogHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (msg.arg1 == 0) {
				showDialog();
			} else {
				mActivity.changeToNormalMode(true);
				if (msg.arg1 == 2) {
					callBath = false;
					hideDialog();
					mActivity.mCalllogQueryManager.startQuery();
					Toast.makeText(
							mActivity,
							mActivity
									.getString(R.string.delete_record_success_toast),
							Toast.LENGTH_SHORT).show();
				} else if (msg.arg1 == 1) {
					smsBath = false;
					hideDialog();
					mActivity.mSmsQueryManager.startQueryMms();
					Toast.makeText(
							mActivity,
							mActivity
									.getString(R.string.delete_record_success_toast),
							Toast.LENGTH_SHORT).show();
					mAppOpsManager.setMode(15, android.os.Process.myUid(), mActivity.getPackageName(), 2);
					
				} else if (msg.arg1 == 3) {
					smsBath = false;
					hideDialog();
					mActivity.mSmsQueryManager.startQueryMms();
					Toast.makeText(
							mActivity,
							mActivity
									.getString(R.string.recover_record_success_toast),
									Toast.LENGTH_SHORT).show();
					
					mAppOpsManager.setMode(15, android.os.Process.myUid(), mActivity.getPackageName(), 2);
				} else if (msg.arg1 == 4) {
					callBath = false;
					hideDialog();
					mActivity.mCalllogQueryManager.startQuery();
					Toast.makeText(
							mActivity,
							mActivity
									.getString(R.string.recover_record_success_toast),
							Toast.LENGTH_SHORT).show();
				}

			}
		};
	};

	private ProgressDialog mProgressDialog;
	private int operationType = 0;

	private void showDialog() {
		if (mProgressDialog == null) {
			mProgressDialog = new ProgressDialog(mActivity);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setCancelable(false);
		}
		if (operationType == 0) {
			mProgressDialog.setMessage(mActivity.getResources().getString(
					R.string.dels));
		} else {
			mProgressDialog.setMessage(mActivity.getResources().getString(
					R.string.recovery));
			operationType = 0;
		}
		mProgressDialog.show();
	}

	private void hideDialog() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
		}
	}

	boolean isDialogShowing() {
		return mProgressDialog != null && mProgressDialog.isShowing();
	}
	
	private void deleteSelectedCallLogInternal() {
		deleteSelectedCallLogInternal(false);
	}
	
	private void deleteSelectedCallLogInternal(Boolean deleteBlack) {
		new Thread() {
			@Override
			public void run() {
				Uri callsUri = Calls.CONTENT_URI;
				callBath = true;
				Message message = mDialogHandler.obtainMessage();
				message.arg1 = 0;
				message.sendToTarget();
				// TODO Auto-generated method stub
				Set<Integer> list = mActivity.mCallListAdapter.getCheckedItem();
				List<String> calls = new ArrayList<String>();
				Cursor pcursor;
				String num;
				for (int pos : list) {
					pcursor = mActivity.mCallListAdapter.getCursor();
					pcursor.moveToPosition(pos);
					num = pcursor.getString(pcursor.getColumnIndex("number"));
					calls.add(num);
				}
				ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
				for (int i = 0; i < calls.size(); i++) {
					ops.add(ContentProviderOperation.newDelete(callsUri)
						    .withSelection(BlackUtils.getPhoneNumberEqualString(calls.get(i))
									+ " and type in (1,3) and reject >= 1", null)
							.withYieldAllowed(true)
							.build());
				}
				try {
					mContentResolver.applyBatch(callsUri.getAuthority(),
							ops);
				} catch (Exception e) {
					e.printStackTrace();
				}
				message = mDialogHandler.obtainMessage();
				message.arg1 = 2;
				mDialogHandler.sendMessageDelayed(message, InterceptionUtils.MIN_DIALOG_SHOW_TIME);
			}
		}.start();
		
		if(deleteBlack) {
			 recoverBlack(getSelectBlackReject(false));
		}
	}
	
	private void recoverCallsInternal() {
		recoverCallsInternal(false);
	}
	
	private void recoverCallsInternal(final boolean deleteBlack) {

		new Thread() {
			@Override
			public void run() {
				Uri callsUri = Calls.CONTENT_URI;
				callBath = true;
				Message message = mDialogHandler.obtainMessage();
				message.arg1 = 0;
				message.sendToTarget();
				// TODO Auto-generated method stub
				Set<Integer> list = mActivity.mCallListAdapter.getCheckedItem();
				List<String> calls = new ArrayList<String>();
				Cursor pcursor;
				String num;
				for (int pos : list) {
					pcursor = mActivity.mCallListAdapter.getCursor();
					pcursor.moveToPosition(pos);
					num = pcursor.getString(pcursor.getColumnIndex("number"));
					calls.add(num);
				}
				ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
				for (int i = 0; i < calls.size(); i++) {
					ops.add(ContentProviderOperation.newUpdate(callsUri)
							.withValue("reject", 0)
						    .withSelection(BlackUtils.getPhoneNumberEqualString(calls.get(i))
									+ " and type in (1,3) and reject >= 1", null)
							.withYieldAllowed(true)
							.build());
				}
				try {
					mContentResolver.applyBatch(callsUri.getAuthority(),
							ops);
				} catch (Exception e) {
					e.printStackTrace();
				}
				message = mDialogHandler.obtainMessage();
				message.arg1 = 4;
				mDialogHandler.sendMessageDelayed(message, InterceptionUtils.MIN_DIALOG_SHOW_TIME);
			}
		}.start();
		if(deleteBlack) {
		   recoverBlack(getSelectBlackReject(false));
		}
	}

	private List<String> getSelectBlackReject(boolean isSms) {
		InterceptionAdapterBase adapter = mActivity.mCallListAdapter;
		String item = "number";
		if (isSms) {
			adapter = mActivity.mSmsListAdapter;
			item = "address";
		}
		Set<Integer> list = adapter.getCheckedItem();
		List<String> nums = new ArrayList<String>();
		Cursor pcursor;
		String num;
		for (int pos : list) {
			pcursor = adapter.getCursor();
			pcursor.moveToPosition(pos);
			num = pcursor.getString(pcursor.getColumnIndex(item));
			if (nums != null && nums.contains(num)) {
                continue;
            } 
			if(1 == pcursor.getInt(pcursor.getColumnIndex("reject"))) {
				nums.add(num);
			}
		}
		return nums;
	}
	
	private void recoverBlack(final List<String> nums) {
		Thread mThread = new Thread() {
			@Override
			public void run() {
				ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
				for (String number : nums) {
					ops.add(getDeleteOperation(number,  0));
				}
				try {
					mContentResolver.applyBatch(
							BlackUtils.HB_CONTACT_AUTHORITY, ops);
				}  catch (Exception e) {
					e.printStackTrace();
				}	
			}
		};
		mDialogHandler.postDelayed(mThread, 1500);
	}
	
	protected String getPhoneNumberEqualString(String number) {
		return " PHONE_NUMBERS_EQUAL(number, \"" + number + "\", 0) ";
	}

	protected ContentProviderOperation getDeleteOperation(String number,
			int black) {
		return ContentProviderOperation.newUpdate(BlackUtils.BLACK_URI)
				.withValue("isblack", black).withValue("number", number)
				.withValue("reject", 3)
				.withSelection(getPhoneNumberEqualString(number), null)
				.withYieldAllowed(true).build();
	}

	private void delSelectedSmsInternal() {
		delSelectedSmsInternal(false);
	}
	private void delSelectedSmsInternal(boolean deleteBlack) {
		// 点击了垃圾桶的响应事件
	    mAppOpsManager.setMode(15, android.os.Process.myUid(), mActivity.getPackageName(), 0);
		new Thread() {
			@Override
			public void run() {
				Uri uriSms = Uri.parse("content://sms");
				Uri uriMms = Uri.parse("content://mms");
				smsBath = true;
				// TODO Auto-generated method stub
				Message message = mDialogHandler.obtainMessage();
				message.arg1 = 0;
				message.sendToTarget();
				Set<Integer> list = mActivity.mSmsListAdapter.getCheckedItem();
				Cursor pcursor = mActivity.mSmsListAdapter.getCursor();
				List<String> smsIds = new ArrayList<String>();
				List<String> mmsIds = new ArrayList<String>();
				for (int pos : list) {
					pcursor.moveToPosition(pos);
					String num = pcursor.getString(pcursor.getColumnIndex("_id"));
					if (pcursor.getInt(pcursor.getColumnIndex("ismms")) == 1) {
						mmsIds.add(num);
					} else {
						smsIds.add(num);
					}
				}
				for (int i = 0; i < mmsIds.size(); i++) {
					try {
						mContentResolver.delete(uriMms, "_id=?",
								new String[] { mmsIds.get(i) });
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				for (int i = 0; i < smsIds.size(); i++)  {
					try {
						mContentResolver.delete(uriSms, "_id=?",
								new String[] { smsIds.get(i) });
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				message = mDialogHandler.obtainMessage();
				message.arg1 = 1;
				mDialogHandler.sendMessageDelayed(message, InterceptionUtils.MIN_DIALOG_SHOW_TIME);
			}
		}.start();
		
		if(deleteBlack) {
			   recoverBlack(getSelectBlackReject(true));
		}
	}

	private void recoverSmsInternal() {
		recoverSmsInternal(false);
	}
	private void recoverSmsInternal(boolean deleteBlack) {
		mAppOpsManager.setMode(15, android.os.Process.myUid(), mActivity.getPackageName(), 0);
		// 点击了垃圾桶的响应事件
		operationType = 1;
		new Thread() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Uri uriSms = Uri.parse("content://sms");
				Uri uriMms = Uri.parse("content://mms");
				smsBath = true;
				Message message = mDialogHandler.obtainMessage();
				message.arg1 = 0;
				message.sendToTarget();
				
				Set<Integer> list = mActivity.mSmsListAdapter.getCheckedItem();
				Cursor 	pcursor = mActivity.mSmsListAdapter.getCursor();
				List<String> smsIds = new ArrayList<String>();
				List<String> mmsIds = new ArrayList<String>();
				for (int pos : list) {
					pcursor.moveToPosition(pos);
					String num = pcursor.getString(pcursor
							.getColumnIndex("_id"));
					if (pcursor.getInt(pcursor.getColumnIndex("ismms")) == 1) {
						mmsIds.add(num);
					} else {
						smsIds.add(num);
					}
				}
				ContentValues cv = new ContentValues();
				cv.put("reject", 0);
				for (int i = 0; i < mmsIds.size(); i++) {
					try {
						mContentResolver.update(uriMms, cv, "_id=?",
								new String[] { mmsIds.get(i) });
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				for (int i = 0; i < smsIds.size(); i++)  {
					try {
						mContentResolver.update(uriSms, cv, "_id=?",
								new String[] { smsIds.get(i) });
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				message = mDialogHandler.obtainMessage();
				message.arg1 = 3;
				mDialogHandler.sendMessageDelayed(message, InterceptionUtils.MIN_DIALOG_SHOW_TIME);
			}
		}.start();
		
		if(deleteBlack) {
			   recoverBlack(getSelectBlackReject(true));
		}
	}

}