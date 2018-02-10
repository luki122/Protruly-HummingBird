/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dialer.calllog;

import static android.Manifest.permission.READ_CALL_LOG;

import java.util.ArrayList;

import com.android.contacts.common.GeoUtil;
import com.android.contacts.common.activity.TransactionSafeActivity;
import com.android.contacts.common.util.PermissionsUtil;
import com.android.dialer.DialerApplication;
import com.android.dialer.DialtactsActivity;
import com.android.dialer.HbPrivateCallLogActivity;
import com.android.dialer.PhoneCallDetails;
import com.android.dialer.calllog.CallLogAsyncTaskUtil.CallLogAsyncTaskListener;
import com.android.dialer.list.HbViewPager;
import com.android.dialer.list.OnListFragmentScrolledListener;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.EmptyLoader;
import com.android.dialer.util.IntentUtil;
import com.android.dialer.voicemail.VoicemailPlaybackPresenter;
import com.android.dialer.widget.EmptyContentView;
import com.android.dialer.widget.EmptyContentView.OnEmptyViewActionButtonClickedListener;
import com.android.dialerbind.ObjectFactory;
import com.android.dialer.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PinnedPositions;
import android.provider.ContactsContract.Profile;
import android.provider.VoicemailContract.Status;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.PhoneStateListener;
//import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import hb.view.menu.BottomWidePopupMenu;
import  hb.view.menu.bottomnavigation.BottomNavigationView;
import hb.widget.ActionModeListener;
import hb.widget.ActionMode;
import hb.widget.ActionMode.Item;
/**
 * Displays a list of call log entries. To filter for a particular kind of call
 * (all, missed or voicemails), specify it in the constructor.
 */
public class CallLogFragment extends Fragment implements CallLogQueryHandler.Listener,
CallLogAdapter.CallFetcher, OnEmptyViewActionButtonClickedListener{
	private static final String TAG = "CallLogFragment";
	private ActionMode actionMode;
	private BottomNavigationView bottomBar;
	/**
	 * ID of the empty loader to defer other fragments.
	 */
	private static final int EMPTY_LOADER_ID = 0;
	private BottomWidePopupMenu bottomWidePopupMenu1,bottomWidePopupMenu2;
	private static final String KEY_FILTER_TYPE = "filter_type";
	private static final String KEY_LOG_LIMIT = "log_limit";
	private static final String KEY_DATE_LIMIT = "date_limit";
	private HbViewPager mViewPager;
	// No limit specified for the number of logs to show; use the CallLogQueryHandler's default.
	private static final int NO_LOG_LIMIT = -1;
	// No date-based filtering.
	private static final int NO_DATE_LIMIT = 0;

	private static final int READ_CALL_LOG_PERMISSION_REQUEST_CODE = 1;

	private RecyclerView mRecyclerView;
	private LinearLayoutManager mLayoutManager;
	public CallLogAdapter mAdapter;
	private CallLogQueryHandler mCallLogQueryHandler;
	private VoicemailPlaybackPresenter mVoicemailPlaybackPresenter;
	private boolean mScrollToTop;

	/** Whether there is at least one voicemail source installed. */
	private boolean mVoicemailSourcesAvailable = false;

	private EmptyContentView mEmptyListView;


	public void setEmptyListView(EmptyContentView mEmptyListView) {
		this.mEmptyListView = mEmptyListView;
	}
	private KeyguardManager mKeyguardManager;

	private boolean mEmptyLoaderRunning;
	private boolean mCallLogFetched;
	private boolean mVoicemailStatusFetched;

	private final Handler mHandler = new Handler();

	private class CustomContentObserver extends ContentObserver {
		public CustomContentObserver() {
			super(mHandler);
		}
		@Override
		public void onChange(boolean selfChange) {
			Log.d(TAG,"liyangs-CustomContentObserver onchange");
			mRefreshDataRequired = true;
			//			if(count==0) {
			//				mHandler.removeCallbacks(runnable);
			//				mHandler.postDelayed(runnable, 1000);
			//			}
		}
	}


	//	private int prePhoneState=TelephonyManager.CALL_STATE_IDLE;
	//	class MyPhoneStateListener extends PhoneStateListener{  
	//
	//		@Override  
	//		public void onCallStateChanged(int state, String incomingNumber) { 
	//			Log.d(TAG,"liyangs-onCallStateChanged:"+state);
	//			switch (state) {  
	//			case TelephonyManager.CALL_STATE_IDLE: 
	//				break;  
	//			case TelephonyManager.CALL_STATE_RINGING:  
	//				break;  
	//			case TelephonyManager.CALL_STATE_OFFHOOK:  //电话被挂起了
	//				mHandler.removeCallbacks(runnable);
	//				mHandler.postDelayed(runnable,300);
	//				break;
	//			default:  
	//				break;  
	//			}  
	//			prePhoneState=state;
	//			super.onCallStateChanged(state, incomingNumber);  
	//		}
	//	}
	private String mQueryData = null;
	/**
	 * Use it to inject search data.
	 * This is the entrance of call log search mode.
	 * @param query
	 */
	public void setQueryData(String query) {
		mQueryData = query;
		mAdapter.setQueryString(query);
	}
	// See issue 6363009
	private final ContentObserver mCallLogObserver = new CustomContentObserver();
	private final ContentObserver mContactsObserver = new CustomContentObserver();
	private final ContentObserver mVoicemailStatusObserver = new CustomContentObserver();
	private final ContentObserver mMarkObserver = new CustomContentObserver();
	private boolean mRefreshDataRequired = true;

	private boolean mHasReadCallLogPermission = false;

	// Exactly same variable is in Fragment as a package private.
	private boolean mMenuVisible = true;

	// Default to all calls.
	private int mCallTypeFilter = CallLogQueryHandler.CALL_TYPE_ALL;
	public Cursor getCursor() {
		return mAdapter.getCursor();
	}
	// Log limit - if no limit is specified, then the default in {@link CallLogQueryHandler}
	// will be used.
	private int mLogLimit = NO_LOG_LIMIT;

	// Date limit (in millis since epoch) - when non-zero, only calls which occurred on or after
	// the date filter are included.  If zero, no date-based filtering occurs.
	private long mDateLimit = NO_DATE_LIMIT;

	/*
	 * True if this instance of the CallLogFragment is the Recents screen shown in
	 * DialtactsActivity.
	 */
	private boolean mIsRecentsFragment;

	public interface HostInterface {
		public void showDialpad();
	}

	public CallLogFragment() {
		this(CallLogQueryHandler.CALL_TYPE_ALL, NO_LOG_LIMIT);
	}

	public CallLogFragment(int filterType) {
		this(filterType, NO_LOG_LIMIT);
	}

	public CallLogFragment(int filterType, int logLimit) {
		this(filterType, logLimit, NO_DATE_LIMIT);
	}

	/**
	 * Creates a call log fragment, filtering to include only calls of the desired type, occurring
	 * after the specified date.
	 * @param filterType type of calls to include.
	 * @param dateLimit limits results to calls occurring on or after the specified date.
	 */
	public CallLogFragment(int filterType, long dateLimit) {
		this(filterType, NO_LOG_LIMIT, dateLimit);
	}

	/**
	 * Creates a call log fragment, filtering to include only calls of the desired type, occurring
	 * after the specified date.  Also provides a means to limit the number of results returned.
	 * @param filterType type of calls to include.
	 * @param logLimit limits the number of results to return.
	 * @param dateLimit limits results to calls occurring on or after the specified date.
	 */
	public CallLogFragment(int filterType, int logLimit, long dateLimit) {
		mCallTypeFilter = filterType;
		mLogLimit = logLimit;
		mDateLimit = dateLimit;
	}

	public CallLogFragment(int filterType, int logLimit, long dateLimit,HbViewPager mViewPager) {
		mCallTypeFilter = filterType;
		mLogLimit = logLimit;
		mDateLimit = dateLimit;
		this.mViewPager=mViewPager;
	}

	public CallLogFragment(int filterType, int logLimit, long dateLimit,ActionMode actionMode,BottomNavigationView bottomBar) {
		this.actionMode=actionMode;
		mCallTypeFilter = filterType;
		mLogLimit = logLimit;
		mDateLimit = dateLimit;
		this.bottomBar=bottomBar;
	}


	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);

		if(getActivity() instanceof HbPrivateCallLogActivity){ 
			mIsPrivate=true;
		}

		if (state != null) {
			mCallTypeFilter = state.getInt(KEY_FILTER_TYPE, mCallTypeFilter);
			mLogLimit = state.getInt(KEY_LOG_LIMIT, mLogLimit);
			mDateLimit = state.getLong(KEY_DATE_LIMIT, mDateLimit);
		}

		mIsRecentsFragment = mLogLimit != NO_LOG_LIMIT;

		final Activity activity = getActivity();
		final ContentResolver resolver = activity.getContentResolver();
		String currentCountryIso = GeoUtil.getCurrentCountryIso(activity);
		mCallLogQueryHandler = new CallLogQueryHandler(activity, resolver, this, mLogLimit);
		mKeyguardManager =
				(KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
		resolver.registerContentObserver(CallLog.CONTENT_URI, true, mCallLogObserver);
		resolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true,
				mContactsObserver);
		resolver.registerContentObserver(Status.CONTENT_URI, true, mVoicemailStatusObserver);
		resolver.registerContentObserver(Uri.parse("content://com.hb.contacts/mark"), true, mMarkObserver);
		setHasOptionsMenu(true);

		if (mCallTypeFilter == Calls.VOICEMAIL_TYPE) {
			mVoicemailPlaybackPresenter = VoicemailPlaybackPresenter
					.getInstance(activity, state);
		}

		Log.d(TAG,"liyangs-oncreate");

		//        //获取电话服务  
		//		telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);  
		//       // 手动注册对PhoneStateListener中的listen_call_state状态进行监听  
		//		telephonyManager.listen(new MyPhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE);   

		beginTime=System.currentTimeMillis();


	}


	//	private TelephonyManager telephonyManager;
	private long beginTime;
	private void setStarred(Uri contactUri,boolean value) {
		if (contactUri == null) {
			Log.e(TAG, "Invalid arguments for setStarred request");
			return;
		}

		final ContentValues values = new ContentValues(1);
		values.put(Contacts.STARRED, value);
		getContext().getContentResolver().update(contactUri, values, null, null);

		// Undemote the contact if necessary
		final Cursor c = getContext().getContentResolver().query(contactUri, new String[] {Contacts._ID},
				null, null, null);

		if (c == null) {
			Toast.makeText(getContext(),"非联系人不能收藏", Toast.LENGTH_LONG).show();
			return;
		}
		try {
			if (c.moveToFirst()) {
				final long id = c.getLong(0);

				// Don't bother undemoting if this contact is the user's profile.
				if (id < Profile.MIN_ID) {
					PinnedPositions.undemote(getContext().getContentResolver(), id);
				}
			}
		} finally {
			c.close();
		}

		Toast.makeText(getContext(),value?getContext().getString(R.string.have_added_star):getContext().getString(R.string.have_removed_star), Toast.LENGTH_LONG).show();
	}

	public int getSDKVersionNumber() {
		int sdkVersion;
		try {
			sdkVersion = Integer.valueOf(android.os.Build.VERSION.SDK);
		} catch (NumberFormatException e) {
			sdkVersion = 0;
		}
		return sdkVersion;
	}

	private final static String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
	private SimStateChangeReceiver myReceiver;
	public class SimStateChangeReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG,"liyangs-onreceive");
			if(System.currentTimeMillis()-beginTime<2000) return;
			String action=intent.getAction();
			Bundle bundle=intent.getExtras();
			Log.d(TAG,"onReceive,action:"+action+" bundle:"+bundle);
			if(TextUtils.equals(action, ACTION_SIM_STATE_CHANGED)){
				mHandler.removeCallbacks(runnable);
				mHandler.postDelayed(runnable, 2000);

			}
		}
	}

	private Runnable runnable=new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.d(TAG,"liyangs-runnable run");
			if(getActivity() != null) {
				refreshData();
			}
		}
	};

	public void deleteSelectedCallLogs(){
		ArrayList<Long> calls=new ArrayList<Long>();
		Log.d(TAG,"mAdapter.getmCount():"+mAdapter.getmCount());
		for(int i=0;i<mAdapter.getmCount();i++){
			if(mAdapter.getCheckedArrayValue(i)){
				long[] ids=mAdapter.getcallIds(i);
				for(long id:ids) {
					calls.add(id);
				}
			}
		}
		final StringBuilder callIds = new StringBuilder();
		for(int j=0;j<calls.size();j++){
			Log.d(TAG,"j:"+j+" "+calls.get(j));
			if (callIds.length() != 0) {
				callIds.append(",");
			}
			callIds.append(calls.get(j));
		}

		Log.d(TAG,"callIds:"+callIds);

		CallLogAsyncTaskUtil.deleteCalls(
				getActivity(), callIds.toString(), mCallLogAsyncTaskListener,mIsPrivate);
	}

	public boolean isEditMode;
	public void switchToEditMode(boolean isEditMode){
		//		((DialtactsActivity)getActivity()).showFab(!isEditMode);
		if(bottomBar==null||actionMode==null){
			if(getActivity() instanceof DialtactsActivity){
				Log.d(TAG,"activity1");
				TransactionSafeActivity activity=(TransactionSafeActivity)getActivity();
				actionMode=activity.actionMode;
				bottomBar=activity.bottomBar;
				mAdapter.setActionMode(actionMode);
				mAdapter.setBottomBar(bottomBar);
				//mAdapter.setRecyclerView(mRecyclerView);
			}else if(getActivity() instanceof HbPrivateCallLogActivity){
				Log.d(TAG,"activity2");
				TransactionSafeActivity activity=(TransactionSafeActivity)getActivity();
				actionMode=activity.actionMode;
				bottomBar=activity.bottomBar;
				mAdapter.setActionMode(actionMode);
				mAdapter.setBottomBar(bottomBar);
				//mAdapter.setRecyclerView(mRecyclerView);
			}

			if(actionMode!=null){
				actionMode.setNagativeText(getActivity().getString(R.string.hb_cancel));
				actionMode.bindActionModeListener(new ActionModeListener(){
					/**
					 * ActionMode上面的操作按钮点击时触发，在这个回调中，默认提供两个ID使用，
					 * 确定按钮的ID是ActionMode.POSITIVE_BUTTON,取消按钮的ID是ActionMode.NAGATIVE_BUTTON
					 * @param view
					 */
					public void onActionItemClicked(Item item){
						Log.d(TAG,"onActionItemClicked,itemid:"+item.getItemId());
						switch (item.getItemId()) {
						case ActionMode.POSITIVE_BUTTON:	
							if(mAdapter.isAllSelect()) {
								mAdapter.setAllSelect(false);							
							}else {
								mAdapter.setAllSelect(true);
							}
							mAdapter.updateActionMode();
							mAdapter.notifyDataSetChanged();
							break;

						case ActionMode.NAGATIVE_BUTTON:
							switchToEditMode(false);					
							break;
						default:
							break;
						}
					}

					/**
					 * ActionMode显示的时候触发
					 * @param actionMode
					 */
					public void onActionModeShow(ActionMode actionMode){

					}

					/**
					 * ActionMode消失的时候触发
					 * @param actionMode
					 */
					public void onActionModeDismiss(ActionMode actionMode){

					}
				});
			}
		}


		if(isEditMode){
			if(mViewPager!=null) mViewPager.setScrollble(false);
			bottomBar.setVisibility(View.VISIBLE);
			mAdapter.setEditMode(true);
			mAdapter.updateActionMode();
			//			mAdapter.setCurrentSliderView(null);
			if(getActivity() instanceof DialtactsActivity){
				((DialtactsActivity)getActivity()).showActionMode(true);
			}else if(getActivity() instanceof HbPrivateCallLogActivity){
				((HbPrivateCallLogActivity)getActivity()).showActionMode(true);
			}

			mAdapter.notifyDataSetChanged();
			int bottommenuHeight = getResources().getDimensionPixelOffset(
					R.dimen.bottom_menu_height);
			int paddingRight=getResources().getDimensionPixelOffset(
					R.dimen.contact_listview_header_padding_left);
			mRecyclerView.setPadding(0, 0, paddingRight, bottommenuHeight);
		}else{
			if(mAdapter.getEditMode()){
				if(mViewPager!=null)  mViewPager.setScrollble(true);
				bottomBar.setVisibility(View.GONE);
				mAdapter.setAllSelect(false);
				mAdapter.setEditMode(false);
				if(getActivity() instanceof DialtactsActivity){
					((DialtactsActivity)getActivity()).showActionMode(false);
				}else if(getActivity() instanceof HbPrivateCallLogActivity){
					((HbPrivateCallLogActivity)getActivity()).showActionMode(false);
				}
				mAdapter.notifyDataSetChanged();
				mRecyclerView.setPadding(0, 0, 0, 0);
			}
		}

		((DialtactsActivity)getActivity()).showFAB(!isEditMode);
		this.isEditMode=isEditMode;
	}
	private CallLogAsyncTaskListener mCallLogAsyncTaskListener = new CallLogAsyncTaskListener() {
		@Override
		public void onDeleteCall() {
			Log.d(TAG,"onDeleteCall");
			switchToEditMode(false);
		}

		@Override
		public void onDeleteVoicemail() {

		}

		@Override
		public void onGetCallDetails(PhoneCallDetails[] details) {
			// TODO Auto-generated method stub

		}
	};


	private int position;
	private String mNumber,mName;
	private int mSlotId;
	private Uri contactUri;
	private MenuItem menuItemStar;
	private boolean isStarred;
	/**
	 * The OnClickListener used to expand or collapse the action buttons of a call log entry.
	 */
	private final View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {

		@Override
		public boolean onLongClick(View v) {
			// TODO Auto-generated method stub
			Log.d(TAG,"onLongClick,v:"+v);

			if(getAdapter().getEditMode()) return true;
			((DialtactsActivity)getActivity()).hideDialpad();

			final IntentProvider intentProvider = (IntentProvider) v.getTag();
			if (intentProvider != null) {
				position=intentProvider.getPosition();
				mNumber=intentProvider.getNumber();
				mSlotId=intentProvider.getSlotId();
				contactUri=intentProvider.getContactUri();
				mName=intentProvider.getName();
			}
			Log.d(TAG,"contactUri:"+contactUri+" position:"+position+" number:"+mNumber+" slotid:"+mSlotId+" mName:"+mName);
			if(!TextUtils.isEmpty(mName)){
				if(bottomWidePopupMenu1==null) {
					bottomWidePopupMenu1 = new BottomWidePopupMenu(getActivity());
					bottomWidePopupMenu1.inflateMenu(R.menu.calllog_bottom_menu1);		

					bottomWidePopupMenu1.setOnMenuItemClickedListener(new BottomWidePopupMenu.OnMenuItemClickListener() {
						@Override
						public boolean onItemClicked(MenuItem item) {
							// TODO Auto-generated method stub
							Log.d(TAG,"onItemClicked Item:"+item.getTitle());
							switch(item.getItemId()){
							case R.id.hb_dial_with_which:
								try{
									Intent intent=IntentUtil.getCallIntent(mNumber);
									int slotId=mSlotId;
									if(mSlotId==0) slotId=1;
									else if(mSlotId==1) slotId=0;
									if(slotId!=-1) intent.putExtra("slot",slotId);//-1 不指定；0指定卡槽1拨号；1指定卡槽2拨号
									getContext().startActivity(intent);
								}catch(Exception e){
									Log.d(TAG,"e:"+e);
								}
								break;

							case R.id.hb_send_message:
								getContext().startActivity(IntentUtil.getSendSmsIntent(mNumber));
								break;

							case R.id.hb_add_star:
								setStarred(contactUri,!isStarred);
								break;

							case R.id.hb_copy_number:
								if(getSDKVersionNumber() >= 11){
									android.text.ClipboardManager clipboardManager = (android.text.ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);  
									clipboardManager.setText(mNumber);
								}else{
									// 得到剪贴板管理器
									android.content.ClipboardManager clipboardManager = (android.content.ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);  
									clipboardManager.setPrimaryClip(ClipData.newPlainText(null, mNumber));
								}
								Toast.makeText(getActivity(), "已复制到粘贴板",Toast.LENGTH_LONG).show();
								break;

							case R.id.hb_delete_calllog:
								AlertDialog.Builder builder = new Builder(getActivity());
								builder.setMessage(getActivity().getString(R.string.hb_delete_call_log_message,1));
								builder.setTitle(null);
								builder.setNegativeButton(getActivity().getString(R.string.hb_cancel), null);
								builder.setPositiveButton(getActivity().getString(R.string.hb_ok), new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which)
									{
										dialog.dismiss();

										long[] ids=mAdapter.getcallIds(position);
										final StringBuilder callIds = new StringBuilder();
										for(long id:ids){
											if (callIds.length() != 0) {
												callIds.append(",");
											}
											callIds.append(id);
										}

										Log.d(TAG,"callIds:"+callIds);

										CallLogAsyncTaskUtil.deleteCalls(
												getContext(), callIds.toString(), mCallLogAsyncTaskListener,mIsPrivate);
									}
								});
								AlertDialog alertDialog = builder.create();
								alertDialog.show();
								break;

							case R.id.delete_multi:
								if(CallLogFragment.this.getUserVisibleHint() == false) {
									return true;
								}						
								mAdapter.setCheckedArrayValue(position,true);	
								Log.d(TAG,"position:"+position+" this:"+CallLogFragment.this);
								switchToEditMode(true);
								break;							
							default:
								break;
							}
							return true;
						}
					});
				}

				Menu menu=bottomWidePopupMenu1.getMenu();
				MenuItem menuItem0=menu.findItem(R.id.hb_dial_with_which);

				if(DialerApplication.isMultiSimEnabled){
					if(mSlotId==0) menuItem0.setTitle(getContext().getString(R.string.hb_dial_with_which,2));
					else if(mSlotId==1) menuItem0.setTitle(getContext().getString(R.string.hb_dial_with_which,1));
					else if(mSlotId==-1) menuItem0.setTitle(getContext().getString(R.string.call_other));
				}else{
					menuItem0.setTitle(getContext().getString(R.string.call_other));
				}				

				menuItemStar=menu.findItem(R.id.hb_add_star);
				checkStar();

				bottomWidePopupMenu1.show();
			}else{
				if(bottomWidePopupMenu2==null) {
					bottomWidePopupMenu2 = new BottomWidePopupMenu(getActivity());
					bottomWidePopupMenu2.inflateMenu(R.menu.calllog_bottom_menu2);		

					bottomWidePopupMenu2.setOnMenuItemClickedListener(new BottomWidePopupMenu.OnMenuItemClickListener() {
						@Override
						public boolean onItemClicked(MenuItem item) {
							// TODO Auto-generated method stub
							Log.d(TAG,"onItemClicked Item:"+item.getTitle());
							switch(item.getItemId()){
							case R.id.hb_dial_with_which:
								try{
									Intent intent=IntentUtil.getCallIntent(mNumber);
									int slotId=mSlotId;
									if(mSlotId==0) slotId=1;
									else if(mSlotId==1) slotId=0;
									if(slotId!=-1) intent.putExtra("slot",slotId);//-1 不指定；0指定卡槽1拨号；1指定卡槽2拨号
									getContext().startActivity(intent);
								}catch(Exception e){
									Log.d(TAG,"e:"+e);
								}
								break;

							case R.id.hb_send_message:
								getContext().startActivity(IntentUtil.getSendSmsIntent(mNumber));
								break;

							case R.id.hb_add_to_contact:{
								Log.d(TAG, "hb_add_to_contact");
								Intent intent = IntentUtil.getNewContactIntent(mNumber);
								intent.setClassName("com.android.contacts", "com.android.contacts.activities.CompactContactEditorActivity");
								DialerUtils.startActivityWithErrorToast(getActivity(), intent);
								break;
							}

							case R.id.hb_add_to_exist_contact:{
								Intent intent=IntentUtil.getAddToExistingContactIntent(mNumber);
								intent.setClassName("com.android.contacts", "com.android.contacts.activities.ContactSelectionActivity");
								DialerUtils.startActivityWithErrorToast(getActivity(), intent,R.string.add_contact_not_available);
								break;
							}

							case R.id.hb_copy_number:
								if(getSDKVersionNumber() >= 11){
									android.text.ClipboardManager clipboardManager = (android.text.ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);  
									clipboardManager.setText(mNumber);
								}else{
									// 得到剪贴板管理器
									android.content.ClipboardManager clipboardManager = (android.content.ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);  
									clipboardManager.setPrimaryClip(ClipData.newPlainText(null, mNumber));
								}
								Toast.makeText(getActivity(), "已复制到粘贴板",Toast.LENGTH_LONG).show();
								break;

							case R.id.hb_delete_calllog:
								AlertDialog.Builder builder = new Builder(getActivity());
								builder.setMessage(getActivity().getString(R.string.hb_delete_call_log_message,1));
								builder.setTitle(null);
								builder.setNegativeButton(getActivity().getString(R.string.hb_cancel), null);
								builder.setPositiveButton(getActivity().getString(R.string.hb_ok), new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which)
									{
										dialog.dismiss();

										long[] ids=mAdapter.getcallIds(position);
										final StringBuilder callIds = new StringBuilder();
										for(long id:ids){
											if (callIds.length() != 0) {
												callIds.append(",");
											}
											callIds.append(id);
										}

										Log.d(TAG,"callIds:"+callIds);

										CallLogAsyncTaskUtil.deleteCalls(
												getContext(), callIds.toString(), mCallLogAsyncTaskListener,mIsPrivate);
									}
								});
								AlertDialog alertDialog = builder.create();
								alertDialog.show();
								break;

							case R.id.delete_multi:
								if(CallLogFragment.this.getUserVisibleHint() == false) {
									return true;
								}						
								mAdapter.setCheckedArrayValue(position,true);	
								Log.d(TAG,"position:"+position+" this:"+CallLogFragment.this);
								switchToEditMode(true);
								break;
							default:
								break;
							}
							return true;
						}
					});
				}

				Menu menu=bottomWidePopupMenu2.getMenu();
				MenuItem menuItem0=menu.findItem(R.id.hb_dial_with_which);
				if(DialerApplication.isMultiSimEnabled){
					if(mSlotId==0) menuItem0.setTitle(getContext().getString(R.string.hb_dial_with_which,2));
					else if(mSlotId==1) menuItem0.setTitle(getContext().getString(R.string.hb_dial_with_which,1));
					else if(mSlotId==-1) menuItem0.setTitle(getContext().getString(R.string.call_other));
				}else{
					menuItem0.setTitle(getContext().getString(R.string.call_other));
				}	

				bottomWidePopupMenu2.show();
			}

			return false;
		}
	};

	private void checkStar(){
		Cursor c=null;
		try{
			c = getContext().getContentResolver().query(contactUri, new String[] {"starred"},null, null, null);
			//			Log.d(TAG,"c:"+c+" count:"+(c==null?0:c.getCount()));
			if(c!=null&&c.getCount()>0) {
				c.moveToFirst();
				isStarred=c.getInt(0)==1?true:false;

				Log.d(TAG,"isStarred:"+isStarred+" contactUri:"+contactUri);				

				if(isStarred){
					menuItemStar.setTitle(getContext().getString(R.string.hb_remove_star));					
				}else{
					menuItemStar.setTitle(getContext().getString(R.string.hb_add_star));
				}
			}else{
				Log.d(TAG, "setvisible false");
				menuItemStar.setVisible(false);
			}
		}catch(Exception e){
			Log.d(TAG, "e:"+e.toString());
		}finally{
			if(c!=null){
				c.close();
				c=null;
			}
		}
	}

	public boolean showListView;
	private int count;
	/** Called by the CallLogQueryHandler when the list of calls has been fetched or updated. */
	@Override
	public boolean onCallsFetched(Cursor cursor) {
		Log.d(TAG, "liyangs-onCallsFetched");
		if (getActivity() == null || getActivity().isFinishing()) {
			// Return false; we did not take ownership of the cursor
			return false;
		}

		//		if(cursor!=null&&cursor.getCount()>500){
		//			mAdapter.setIsShowingRecentsTab(true);
		//		}else{
		//			mAdapter.setIsShowingRecentsTab(false);
		//		}

		if(cursor != null) {
			count=cursor.getCount();
			Log.d(TAG,"liyangs-onCallsFetched,count:"+count);
		}

		mAdapter.setLoading(false);
		mAdapter.changeCursor(cursor);		
		// This will update the state of the "Clear call log" menu item.
		getActivity().invalidateOptionsMenu();

		showListView = cursor != null && cursor.getCount() > 0;
		mRecyclerView.setVisibility(showListView ? View.VISIBLE : View.GONE);
		if(mEmptyListView!=null) mEmptyListView.setVisibility(!showListView ? View.VISIBLE : View.GONE);


		if (mScrollToTop) {
			// The smooth-scroll animation happens over a fixed time period.
			// As a result, if it scrolls through a large portion of the list,
			// each frame will jump so far from the previous one that the user
			// will not experience the illusion of downward motion.  Instead,
			// if we're not already near the top of the list, we instantly jump
			// near the top, and animate from there.
			if (mLayoutManager.findFirstVisibleItemPosition() > 5) {
				// TODO: Jump to near the top, then begin smooth scroll.
				mRecyclerView.smoothScrollToPosition(0);
			}
			// Workaround for framework issue: the smooth-scroll doesn't
			// occur if setSelection() is called immediately before.
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					if (getActivity() == null || getActivity().isFinishing()) {
						return;
					}
					mRecyclerView.smoothScrollToPosition(0);
				}
			});

			mScrollToTop = false;
		}
		mCallLogFetched = true;
		destroyEmptyLoaderIfAllDataFetched();


		return true;
	}

	/**
	 * Called by {@link CallLogQueryHandler} after a successful query to voicemail status provider.
	 */
	@Override
	public void onVoicemailStatusFetched(Cursor statusCursor) {
		Activity activity = getActivity();
		if (activity == null || activity.isFinishing()) {
			return;
		}

		mVoicemailStatusFetched = true;
		destroyEmptyLoaderIfAllDataFetched();
	}

	private void destroyEmptyLoaderIfAllDataFetched() {
		if (mCallLogFetched && mVoicemailStatusFetched && mEmptyLoaderRunning) {
			mEmptyLoaderRunning = false;
			getLoaderManager().destroyLoader(EMPTY_LOADER_ID);
		}
	}


	private OnListFragmentScrolledListener mActivityScrollListener;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mActivityScrollListener = (OnListFragmentScrolledListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnListFragmentScrolledListener");
		}
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
		Bundle bundle=getArguments();
		if(bundle!=null){
			mCallTypeFilter =bundle.getInt("filterType");
			mLogLimit = bundle.getInt("logLimit");
			mDateLimit = bundle.getLong("dateLimit");
		}


		View view = inflater.inflate(R.layout.call_log_fragment, container, false);

		mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
		mRecyclerView.setHasFixedSize(true);
		mLayoutManager = new LinearLayoutManager(getActivity());
		mRecyclerView.setLayoutManager(mLayoutManager);
		//		mRecyclerView.addItemDecoration(new HbRecycleViewDevider(
		//			    getActivity(), LinearLayoutManager.VERTICAL, 
		//			    getActivity().getResources().getDimensionPixelOffset(R.dimen.hb_contacts_listview_devider_height),
		//			    getActivity().getResources().getColor(R.color.hb_devider_line_background_color)));

		mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {

			@Override  
			public void onScrollStateChanged(RecyclerView recyclerView, int scrollState) {
				Log.d(TAG,"onScrollStateChanged,state:"+scrollState);
				mActivityScrollListener.onListFragmentScrollStateChange(scrollState);
			}  

			@Override  
			public void onScrolled(RecyclerView recyclerView, int dx, int dy){}
		});  

		mEmptyListView = (EmptyContentView) view.findViewById(R.id.empty_list_view);
		mEmptyListView.setImage(/*R.drawable.hb_no_calllog_image*/EmptyContentView.NO_IMAGE);
		if(mEmptyListView!=null) mEmptyListView.setActionClickedListener(this);

		String currentCountryIso = GeoUtil.getCurrentCountryIso(getActivity());
		//		boolean isShowingRecentsTab = mLogLimit != NO_LOG_LIMIT || mDateLimit != NO_DATE_LIMIT;
		mAdapter = ObjectFactory.newCallLogAdapter(
				getActivity(),
				this,
				new ContactInfoHelper(getActivity(), currentCountryIso),
				mVoicemailPlaybackPresenter,
				/*isShowingRecentsTab*/false);
		mAdapter.setPrivate(mIsPrivate);	
		mAdapter.setmLongClickListener(mLongClickListener);
		mAdapter.setActionMode(actionMode);
		mAdapter.setBottomBar(bottomBar);
		mRecyclerView.setAdapter(mAdapter);

		//		Log.d(TAG,"fetchCalls0");
		//		fetchCalls();
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		updateEmptyMessage(mCallTypeFilter);
		mAdapter.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	public void onStart() {
		// Start the empty loader now to defer other fragments.  We destroy it when both calllog
		// and the voicemail status are fetched.
		getLoaderManager().initLoader(EMPTY_LOADER_ID, null,
				new EmptyLoader.Callback(getActivity()));
		mEmptyLoaderRunning = true;
		super.onStart();
		myReceiver = new SimStateChangeReceiver();
		IntentFilter intentFilter=new IntentFilter(ACTION_SIM_STATE_CHANGED);
		getActivity().registerReceiver(myReceiver, intentFilter);
	}

	@Override
	public void onResume() {
		Log.d(TAG,"liyang2017-onResume");
		super.onResume();
		final boolean hasReadCallLogPermission =
				PermissionsUtil.hasPermission(getActivity(), READ_CALL_LOG);
		if (!mHasReadCallLogPermission && hasReadCallLogPermission) {
			// We didn't have the permission before, and now we do. Force a refresh of the call log.
			// Note that this code path always happens on a fresh start, but mRefreshDataRequired
			// is already true in that case anyway.
			mRefreshDataRequired = true;
			updateEmptyMessage(mCallTypeFilter);
		}
		mHasReadCallLogPermission = hasReadCallLogPermission;
		refreshData();
		mAdapter.startCache();
	}

	@Override
	public void onPause() {
		Log.d(TAG,"liyangs-onPause");
		if (mVoicemailPlaybackPresenter != null) {
			mVoicemailPlaybackPresenter.onPause();
		}
		mAdapter.pauseCache();
		super.onPause();
	}

	@Override
	public void onStop() {
		updateOnTransition(false /* onEntry */);

		super.onStop();
		try {
			getActivity().unregisterReceiver(myReceiver);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDestroy() {
		mAdapter.pauseCache();
		mAdapter.changeCursor(null);

		if (mVoicemailPlaybackPresenter != null) {
			mVoicemailPlaybackPresenter.onDestroy();
		}

		getActivity().getContentResolver().unregisterContentObserver(mCallLogObserver);
		getActivity().getContentResolver().unregisterContentObserver(mContactsObserver);
		getActivity().getContentResolver().unregisterContentObserver(mVoicemailStatusObserver);
		getActivity().getContentResolver().unregisterContentObserver(mMarkObserver);



		mHandler.removeCallbacksAndMessages(null);
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_FILTER_TYPE, mCallTypeFilter);
		outState.putInt(KEY_LOG_LIMIT, mLogLimit);
		outState.putLong(KEY_DATE_LIMIT, mDateLimit);

		mAdapter.onSaveInstanceState(outState);

		if (mVoicemailPlaybackPresenter != null) {
			mVoicemailPlaybackPresenter.onSaveInstanceState(outState);
		}
	}

	@Override
	public void fetchCalls() {
		Log.d(TAG,"liyangs-fetchCalls");
		mCallLogQueryHandler.fetchCalls(mCallTypeFilter, mDateLimit,mIsPrivate);
	}

	private void updateEmptyMessage(int filterType) {
		final Context context = getActivity();
		if (context == null) {
			return;
		}

		Log.d(TAG,"updateEmptyMessage,mEmptyListView:"+mEmptyListView);
		if (!PermissionsUtil.hasPermission(context, READ_CALL_LOG)&&mEmptyListView!=null) {
			mEmptyListView.setDescription(R.string.permission_no_calllog);
			mEmptyListView.setActionLabel(R.string.permission_single_turn_on);
			mEmptyListView.setVisibility(View.VISIBLE);
			return;
		}

		final int messageId;
		switch (filterType) {
		case Calls.MISSED_TYPE:
			messageId = R.string.recentMissed_empty;
			break;
		case Calls.VOICEMAIL_TYPE:
			messageId = R.string.recentVoicemails_empty;
			break;
		case CallLogQueryHandler.CALL_TYPE_ALL:
			messageId = R.string.recentCalls_empty;
			break;
		default:
			throw new IllegalArgumentException("Unexpected filter type in CallLogFragment: "
					+ filterType);
		}
		if(mEmptyListView!=null) {
			mEmptyListView.setDescription(messageId);
			if (mIsRecentsFragment) {
				mEmptyListView.setActionLabel(/*R.string.recentCalls_empty_action*/EmptyContentView.NO_LABEL);
			} else {
				mEmptyListView.setActionLabel(EmptyContentView.NO_LABEL);
			}
		}
	}

	public CallLogAdapter getAdapter() {
		return mAdapter;
	}

	@Override
	public void setMenuVisibility(boolean menuVisible) {
		super.setMenuVisibility(menuVisible);
		//		if (mMenuVisible != menuVisible) {
		//			mMenuVisible = menuVisible;
		//			if (!menuVisible) {
		//				updateOnTransition(false /* onEntry */);
		//			} else if (isResumed()) {
		//				refreshData();
		//			}
		//		}
	}

	/** Requests updates to the data to be shown. */
	private void refreshData() {
		Log.d(TAG,"liyangs-refreshData0,mRefreshDataRequired1:"+mRefreshDataRequired);
		// Prevent unnecessary refresh.
		if (mRefreshDataRequired) {
			// Mark all entries in the contact info cache as out of date, so they will be looked up
			// again once being shown.
			mAdapter.invalidateCache();
			mAdapter.setLoading(true);
			Log.d(TAG,"liyangs-refreshData1");
			fetchCalls();
			mCallLogQueryHandler.fetchVoicemailStatus();
			Log.d(TAG,"liyangs-refreshData2");
			updateOnTransition(true /* onEntry */);
			mRefreshDataRequired = false;
		} else {
			// Refresh the display of the existing data to update the timestamp text descriptions.
			mAdapter.notifyDataSetChanged();
		}
	}

	/**
	 * Updates the call data and notification state on entering or leaving the call log tab.
	 *
	 * If we are leaving the call log tab, mark all the missed calls as read.
	 *
	 * TODO: Move to CallLogActivity
	 */
	private void updateOnTransition(boolean onEntry) {
		// We don't want to update any call data when keyguard is on because the user has likely not
		// seen the new calls yet.
		// This might be called before onCreate() and thus we need to check null explicitly.
		if (mKeyguardManager != null && !mKeyguardManager.inKeyguardRestrictedInputMode()) {
			// On either of the transitions we update the missed call and voicemail notifications.
			// While exiting we additionally consume all missed calls (by marking them as read).
			mCallLogQueryHandler.markNewCallsAsOld();
			if (!onEntry) {
				mCallLogQueryHandler.markMissedCallsAsRead();
			}
			CallLogNotificationsHelper.removeMissedCallNotifications(getActivity());
			CallLogNotificationsHelper.updateVoicemailNotifications(getActivity());
		}
	}

	@Override
	public void onEmptyViewActionButtonClicked(int id) {
		final Activity activity = getActivity();
		if (activity == null) {
			return;
		}

		if (!PermissionsUtil.hasPermission(activity, READ_CALL_LOG)) {
			requestPermissions(new String[] {READ_CALL_LOG}, READ_CALL_LOG_PERMISSION_REQUEST_CODE);
		} else if (mIsRecentsFragment) {
			// Show dialpad if we are the recents fragment.
			((HostInterface) activity).showDialpad();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions,
			int[] grantResults) {
		if (requestCode == READ_CALL_LOG_PERMISSION_REQUEST_CODE) {
			if (grantResults.length >= 1 && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
				// Force a refresh of the data since we were missing the permission before this.
				mRefreshDataRequired = true;
			}
		}
	}

	private boolean mIsPrivate = false;
	public void setPrivate(boolean isPrivate) {
		mIsPrivate = isPrivate;
	}
}
