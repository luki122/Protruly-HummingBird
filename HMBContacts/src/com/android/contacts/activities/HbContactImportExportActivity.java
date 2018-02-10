//add by liyang
package com.android.contacts.activities;

import com.android.contacts.common.activity.RequestImportVCardPermissionsActivity;

import com.mediatek.contacts.simservice.SIMServiceUtils;

import com.android.internal.telephony.ITelephony;
import android.os.ServiceManager;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.activity.RequestImportVCardPermissionsActivity;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.mediatek.contacts.ContactsSystemProperties;
import android.os.StatFs;
import android.os.Trace;

import com.mediatek.contacts.list.ContactsIntentResolverEx;
import com.mediatek.contacts.util.ContactsIntent;
import com.mediatek.contacts.model.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.simservice.SIMProcessorService;
import com.mediatek.contacts.util.AccountTypeUtils;
import android.provider.ContactsContract.Contacts;
import com.android.contacts.common.vcard.CancelActivity;
import com.android.contacts.common.vcard.CancelRequest;
import com.android.contacts.common.vcard.ImportVCardActivity;
import com.android.contacts.common.vcard.VCardService;
import com.android.contacts.common.vcard.NotificationImportExportListener;
import android.R.integer;
import android.app.PendingIntent;
import android.widget.RemoteViews;
import com.android.contacts.common.util.HbUtils;
import com.android.contacts.common.util.AccountSelectionUtil;
import hb.app.dialog.AlertDialog;
import hb.view.menu.BottomWidePopupMenu;
import hb.app.dialog.ProgressDialog;
import com.android.contacts.common.vcard.VCardCommonArguments;
import com.android.contacts.common.vcard.ExportVCardActivity;
import com.android.contacts.hb.HbExportContactsToSimService;
import static android.view.Window.PROGRESS_VISIBILITY_OFF;
import static android.view.Window.PROGRESS_VISIBILITY_ON;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.mediatek.contacts.activities.ActivitiesUtils;
import com.mediatek.contacts.activities.ContactImportExportActivity;
import com.mediatek.contacts.list.ContactListMultiChoiceActivity;
import com.mediatek.contacts.model.AccountWithDataSetEx;
import com.mediatek.contacts.util.PDebug;
import com.mediatek.storage.StorageManagerEx;
import android.accounts.Account;
import android.app.ActivityManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.AsyncTaskLoader;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.PreferenceManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import hb.preference.Preference;
import hb.preference.Preference.OnPreferenceClickListener;
import hb.preference.PreferenceActivity;
import hb.preference.PreferenceGroup;
import hb.widget.toolbar.Toolbar;
import com.android.contacts.R;

public class HbContactImportExportActivity extends PreferenceActivity implements OnPreferenceClickListener {

	private static final String TAG = "HbContactImportExportActivity";

	public static final String STORAGE_ACCOUNT_TYPE = "_STORAGE_ACCOUNT";
	private Preference mSimToPhonePref;
	private Preference mSDToPhonePref;
	private Preference mExternalSDToPhonePref;
	private Preference mPhoneToSDPref;
	private Preference mPhoneToExternalSDPref;

	private Preference mMergeContactsPref;

	private static final int SUBACTIVITY_ACCOUNT_FILTER = 2;

	protected static final int SIM_CONTACTS_LOADED = 0;

	private static final boolean DBG = true;

	public boolean isMultiSimEnabled;//是否启用双卡

	SharedPreferences prefs;
	private void querySimState(){
	}
	private Context mActivity;
	private Toolbar toolbar;
	private BottomWidePopupMenu bottomWidePopupMenu;
	private List<SubscriptionInfo> mActiveSimInfoList;
	private boolean firstLoad=true;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Log.d(TAG,"onCreate1:"+HbContactImportExportActivity.this+" firstload:"+firstLoad);
		
//		if (RequestImportVCardPermissionsActivity.startPermissionActivity(this)) {
//			return;
//		}
		
		nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);  
		toolbar = getToolbar();
		toolbar.setTitle(getResources().getString(R.string.hb_contacts_management));
		toolbar.setElevation(0f);
		mActivity = HbContactImportExportActivity.this;
		addPreferencesFromResource(R.xml.preference_contact_io);

		findPreferences();

		mEmptyText = (TextView) findViewById(android.R.id.empty);

		bottomWidePopupMenu = new BottomWidePopupMenu(mActivity);
		bottomWidePopupMenu.inflateMenu(R.menu.sim_import_export_selection);
		bottomWidePopupMenu.setOnMenuItemClickedListener(new BottomWidePopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onItemClicked(MenuItem item) {
				// TODO Auto-generated method stub
				Log.d(TAG,"onItemClicked Item:"+item.getTitle());
				switch(item.getItemId()){
				case R.id.import_from_sim1_menu:{
					doImportExport(getSubId(0));
					break;
				}
				case R.id.import_from_sim2_menu:{
					doImportExport(getSubId(1));
					break;
				}
				default:
					break;
				}
				return true;
			}
		});

		Bundle extras=getIntent().getExtras();
		if(extras!=null)
			mCallingActivityName = extras.getString(
					VCardCommonArguments.ARG_CALLING_ACTIVITY, null);

		myReceiver = new ContactsExportCompletedReceiver();


		Intent notificationIntent = new Intent(this,HbContactImportExportActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		contentIntent = PendingIntent.getActivity(this,0,notificationIntent,0);   

		firstLoad=false;

		Intent fromIntent=getIntent();
		if(fromIntent.hasExtra("source")){
			source=fromIntent.getStringExtra("source");
			Log.d(TAG,"source:"+source);
			if(TextUtils.equals("importFromStorage",source)){
				mHandler.removeCallbacks(runnable);
				mHandler.postDelayed(runnable, 500);
			}
		}

		getLoaderManager().restartLoader(ACCOUNT_LOADER_ID, null, new MyLoaderCallbacks());

		IntentFilter intentFilter=new IntentFilter();
		intentFilter.addAction("CONTACTS_EXPORT_FULL");
		intentFilter.addAction("CONTACTS_EXPORT_PART_FULL");
		intentFilter.addAction("CONTACTS_EXPORT_DOING");
		intentFilter.addAction("USER_CANCEL_EXPORT");
		intentFilter.addAction("EXPORT_TO_SD_CARD_DOING");
		intentFilter.addAction("CONTACTS_IMPORT_FROM_SD_DOING");
		intentFilter.addAction("CONTACTS_IMPORT_CANCEL_COMPLETE");
		intentFilter.addAction("CONTACTS_IMPORT_FROM_SD_COMPLETE");
		intentFilter.addAction(HB_ACTION_SIM_STATE_CHANGED);
		intentFilter.addAction("HB_IMPORT_FROM_SIM_FINISHED");
		registerReceiver(myReceiver, intentFilter);	
		
	}
	private Runnable runnable=new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			importFromStorage();
		}
	};

	private String source;
	private static final int ACCOUNT_LOADER_ID = 0;
	private final static String HB_ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
	private ContactsExportCompletedReceiver myReceiver;
	private int totalContactsForExport=0;
	public class ContactsExportCompletedReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			String action=intent.getAction();
			Bundle bundle=intent.getExtras();
			Log.d(TAG,"onReceive,action:"+action+" bundle:"+bundle);
			if(TextUtils.equals(action, "CONTACTS_EXPORT_FULL")){
				if(mProgressDialog!=null) mProgressDialog.dismiss();
				AlertDialog d=new AlertDialog.Builder(HbContactImportExportActivity.this)
						.setTitle(getString(R.string.hb_menu_export))
						.setMessage(getString(R.string.hb_import_sim_contacts_full))
						.setNegativeButton(getString(R.string.hb_ok),new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								if(mProgressDialog!=null) mProgressDialog.dismiss();
							}
						}).create();
				d.show();
			}else if(TextUtils.equals(action, "CONTACTS_EXPORT_PART_FULL")){
				if(mProgressDialog!=null) {
					mProgressDialog.dismiss();
				}

				if(!isRunningForeground()){
					Message message=Message.obtain(mHandler,UPDATE_NOTIFICATION_PROGRESSBAR_FOR_EXPORT_PART_FULL,bundle.getInt("index"));
					mHandler.sendMessage(message);
				}else{



				}
			}else if(TextUtils.equals(action, "CONTACTS_EXPORT_DOING")){
				int index=bundle.getInt("index");
				totalContactsForExport=bundle.getInt("totalContacts");
				if(index==0){
					prepareProgressDialog(getString(R.string.hb_menu_export), getString(R.string.hb_export_sim_contacts_doing),totalContactsForExport);
					if(mProgressDialog!=null)  mProgressDialog.setButton(getString(R.string.hb_cancel), new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							Log.d(TAG,"onClick cancel export");
							HbExportContactsToSimService.isCancel=true;
						}
					});

				}else{
					if(mProgressDialog!=null)  mProgressDialog.incrementProgressBy(1);
					if(index==totalContactsForExport){
						if(mProgressDialog!=null) {
							mProgressDialog.dismiss();
						}
						Toast.makeText(HbContactImportExportActivity.this, getString(R.string.hb_export_sim_contacts_result,index), Toast.LENGTH_LONG).show();
					}
				}

				if(!isRunningForeground()){
					Message message=Message.obtain(mHandler,UPDATE_NOTIFICATION_PROGRESSBAR_FOR_EXPORT,index);
					mHandler.sendMessage(message);
				}
			}else if(TextUtils.equals(action, "USER_CANCEL_EXPORT")){
				int index=bundle.getInt("index");
				totalContactsForExport=bundle.getInt("totalContacts");
				Message message=Message.obtain(mHandler,USER_CANCEL_EXPORT,index);
				mHandler.sendMessage(message);

			}else if(TextUtils.equals(action, "EXPORT_TO_SD_CARD_DOING")){
				int index=bundle.getInt("index");
				int total=bundle.getInt("totalContacts");
				final int mJobId=bundle.getInt("mJobId");
				final String displayName=bundle.getString("displayName");

				if(index==0){
					prepareProgressDialog(getString(R.string.hb_menu_export_to_sd), 
							getString(isClickExternalSD?R.string.hb_export_sim_contacts_doing_to_sd:R.string.hb_export_sim_contacts_doing_to_internal_storage,total),total);
					if(mProgressDialog!=null)  mProgressDialog.setButton(getString(R.string.hb_cancel), new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							Log.d(TAG,"onClick cancel export");
							//							Intent intent=new Intent("CONTACTS_EXPORT_CANCEL");
							//							HbContactImportExportActivity.this.sendBroadcast(intent);

							if(mProgressDialog!=null) {
								mProgressDialog.dismiss();

							}
							Toast.makeText(HbContactImportExportActivity.this, getString(R.string.hb_cancel_msg), Toast.LENGTH_LONG).show();

							final Intent intent = new Intent(HbContactImportExportActivity.this, CancelActivity.class);
							final Uri uri = (new Uri.Builder())
									.scheme("invalidscheme")
									.authority("invalidauthority")
									.appendQueryParameter(CancelActivity.JOB_ID, String.valueOf(mJobId))
									.appendQueryParameter(CancelActivity.DISPLAY_NAME, displayName)
									.appendQueryParameter(CancelActivity.TYPE, String.valueOf(VCardService.TYPE_EXPORT)).build();
							intent.setData(uri);
							startActivity(intent);	
						}
					});
				}else{
					if(mProgressDialog!=null)  mProgressDialog.incrementProgressBy(1);
					if(index==total) {
						if(mProgressDialog!=null) {
							mProgressDialog.dismiss();

						};
						if(!bundle.getBoolean("isForShareContacts")){
							Toast.makeText(HbContactImportExportActivity.this, getString(R.string.hb_export_sim_contacts_result,index),
									Toast.LENGTH_LONG).show();
						}
					}
				}
			}else if(TextUtils.equals(action, "CONTACTS_IMPORT_FROM_SD_DOING")){
				int index=bundle.getInt("index");
				int total=bundle.getInt("totalContacts");
				final int mJobId=bundle.getInt("mJobId");
				final String displayName=bundle.getString("displayName");				
				Log.d(TAG,"CONTACTS_IMPORT_FROM_SD_DOING,index:"+index+" total:"+total);
				if(index==1){
					Log.d(TAG,"index 1");
					SharedPreferences sharedPreferences=getSharedPreferences("com.android.contacts_hb", Context.MODE_WORLD_READABLE);
					SharedPreferences.Editor editor=sharedPreferences.edit();
					editor.putBoolean("is_exporting_or_importing", true);
					editor.commit();
					prepareProgressDialog(getString(R.string.hb_menu_import_from_sd), getString(R.string.hb_import_sim_contacts_doing_from_sd), total);
					if(mProgressDialog!=null)  mProgressDialog.setButton(getString(R.string.hb_cancel), new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							if(mProgressDialog!=null) {
								mProgressDialog.dismiss();							
							}

							prepareProgressDialogSpinner(null, getString(R.string.hb_import_canceling_message));

							final Intent intent = new Intent(HbContactImportExportActivity.this, CancelActivity.class);
							final Uri uri = (new Uri.Builder())
									.scheme("invalidscheme")
									.authority("invalidauthority")
									.appendQueryParameter(CancelActivity.JOB_ID, String.valueOf(mJobId))
									.appendQueryParameter(CancelActivity.DISPLAY_NAME, displayName)
									.appendQueryParameter(CancelActivity.TYPE, String.valueOf(VCardService.TYPE_IMPORT)).build();
							intent.setData(uri);
							startActivity(intent);	
						}
					});
					if(mProgressDialog!=null)  mProgressDialog.incrementProgressBy(1);
				}else{
					//modify by lgy for 3430302
					if(mProgressDialog != null) {
						mProgressDialog.incrementProgressBy(1);
					}
					if(index==total){
						if(mProgressDialog!=null) {
							mProgressDialog.dismiss();
						}
						prepareProgressDialogSpinner(null, getResources().getString(R.string.hb_saving_contacts));
						
					}
				}			
			}else if(TextUtils.equals(action, "CONTACTS_IMPORT_CANCEL_COMPLETE")){
				Log.d(TAG,"CONTACTS_IMPORT_CANCEL_COMPLETE1");
				if(mProgressDialog!=null) {
					mProgressDialog.dismiss();
				}
				Toast.makeText(HbContactImportExportActivity.this, getString(R.string.hb_cancel_msg), Toast.LENGTH_LONG).show();
			}else if(TextUtils.equals(action, "CONTACTS_IMPORT_FROM_SD_COMPLETE")){
				Log.d(TAG,"CONTACTS_IMPORT_FROM_SD_COMPLETE");
				if(mProgressDialog!=null)  mProgressDialog.incrementProgressBy(1);
				if(mProgressDialog!=null) {
					mProgressDialog.dismiss();
					SharedPreferences sharedPreferences=getSharedPreferences("com.android.contacts_hb", Context.MODE_WORLD_READABLE);
					SharedPreferences.Editor editor=sharedPreferences.edit();
					editor.putBoolean("is_exporting_or_importing", false);
					editor.commit();
					//					Toast.makeText(HbContactImportExportActivity.this, getString(R.string.hb_import_sim_contacts_result,String.valueOf(intent.getExtras().getInt("mTotalCount"))), Toast.LENGTH_LONG).show();
				}
			}else if(TextUtils.equals(action,HB_ACTION_SIM_STATE_CHANGED)){
				Log.d(TAG,"HB_ACTION_SIM_STATE_CHANGED");				
				updatePreference();
			}else if(TextUtils.equals(action, "HB_IMPORT_FROM_SIM_FINISHED")){
				Log.d(TAG,"HB_IMPORT_FROM_SIM_FINISHED");
				releaseWakeLock();
				if(mProgressDialog!=null) {
					mProgressDialog.dismiss();
					String message=intent.getIntExtra("count", 0)==0?getString(R.string.hb_import_sim_contacts_zero):getString(R.string.hb_import_sim_contacts_result,intent.getIntExtra("count", 0));
					Toast.makeText(HbContactImportExportActivity.this,message, Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	private void updatePreference(){
		boolean isSimEnable=isAnySimEnabled(this);
		Log.d(TAG,"isSimEnable:"+isSimEnable);
		mSimToPhonePref.setEnabled(isSimEnable);
		mSimToPhonePref.setSelectable(isSimEnable);
	}

	private TextView mEmptyText;
	/**
	 * Find all the Preference by key,
	 */
	private void findPreferences() {
		mSimToPhonePref = findPreference("hb_sim_to_phone");		
		mSDToPhonePref = findPreference("hb_sd_to_phone");	
		//		mPhoneToSimPref = findPreference("hb_phone_to_sim");
		mPhoneToSDPref = findPreference("hb_phone_to_sd");
		mMergeContactsPref=findPreference("hb_merge_contacts");

		mExternalSDToPhonePref=findPreference("hb_external_sd_to_phone");
		mPhoneToExternalSDPref=findPreference("hb_phone_to_external_sd");	

		mSDToPhonePref.setEnabled(true);
		mSDToPhonePref.setSelectable(true);

		mPhoneToSDPref.setEnabled(true);
		mPhoneToSDPref.setSelectable(true);		

		mSimToPhonePref.setOnPreferenceClickListener(this);
		mSDToPhonePref.setOnPreferenceClickListener(this);
		//		mPhoneToSimPref.setOnPreferenceClickListener(this);
		mPhoneToSDPref.setOnPreferenceClickListener(this);
		mMergeContactsPref.setOnPreferenceClickListener(this);


		getExternalSDPath();
		if(sdcardPath!=null) {
			mExternalSDToPhonePref.setEnabled(true);
			mExternalSDToPhonePref.setSelectable(true);
			mPhoneToExternalSDPref.setEnabled(true);
			mPhoneToExternalSDPref.setSelectable(true);
			mExternalSDToPhonePref.setOnPreferenceClickListener(this);
			mPhoneToExternalSDPref.setOnPreferenceClickListener(this);
		} else {
			mExternalSDToPhonePref.setEnabled(false);
			mExternalSDToPhonePref.setSelectable(false);
			mPhoneToExternalSDPref.setEnabled(false);
			mPhoneToExternalSDPref.setSelectable(false);
		}
	}

	public void getExternalSDPath(){
		HashMap<String, File> map=com.android.contacts.common.util.HbUtils.getAllStorages(this);
		//		File emulatedPath=map.get("emulatedPath");
		if(map!=null) sdcardPath=map.get("sdcardPath");
	}

	private File sdcardPath;
	@Override
	public void onResume() {
		Trace.beginSection(TAG + " onResume");
		Log.d(TAG,"onResume");
		super.onResume();
		updatePreference();
	}
	
	public void onPause() {
		Log.d(TAG,"onPause");
		super.onPause();
	}


	@Override
	protected void onStart(){
		Log.d(TAG,"onStart");
		super.onStart();

	}
	@Override
	protected void onStop() {
		Log.d(TAG,"onStop");
		super.onStop();
		getLoaderManager().destroyLoader(ACCOUNT_LOADER_ID);
		releaseWakeLock();
	}

	private boolean isClickExternalSD=false;
	@Override
	public boolean onPreferenceClick(Preference preference) {

		Log.d(TAG,"onPreferenceClick:"+preference);

		if(preference==mSimToPhonePref){//从sim导入到手机
			isMultiSimEnabled=isMultiSimEnabled(mActivity);
			Log.d(TAG,"isMultiSimEnabled:"+isMultiSimEnabled);
			if(isMultiSimEnabled){				
				bottomWidePopupMenu.show();
			}else{
				doImportExport(getSingleSubId());
			}
		}else if(preference==mSDToPhonePref || preference==mExternalSDToPhonePref){//内部存储器导入到手机
			if(preference==mExternalSDToPhonePref) isClickExternalSD=true;
			else isClickExternalSD=false;

			importFromStorage();
		}else if(preference==mPhoneToSDPref || preference==mPhoneToExternalSDPref){//手机导出到内部存储器
			if(preference==mPhoneToExternalSDPref) isClickExternalSD=true;
			else isClickExternalSD=false;

			if (VCardService.isProcessing(VCardService.TYPE_IMPORT)
					|| VCardService.isProcessing(VCardService.TYPE_EXPORT)) {
				Toast.makeText(this, R.string.contact_import_export_tips, Toast.LENGTH_SHORT)
				.show();
				return true;
			}

			Intent intent = new Intent(ContactsIntent.LIST.ACTION_PICK_MULTI_CONTACTS);
			intent.setType(Contacts.CONTENT_TYPE);
			intent.putExtra("isSelectedAddInitial", true);//默认全选
			startActivityForResult(intent, REQUEST_CODE_PICK_FOR_SDCARD_EXPORT);

		}else if(preference==mMergeContactsPref) {
			startActivity(new Intent(HbContactImportExportActivity.this,HbMergeContactsActivity.class));
		}
		return true;
	}

	//begin
	private AccountWithDataSetEx mCheckedAccount1 = null;
	private AccountWithDataSetEx mCheckedAccount2 = null;
	private AccountWithDataSetEx mPhoneAccount = null;
	public void doImportExport(int subId) {
		Log.i(TAG, "[doImportExport],subId:"+subId);
		if (!SimCardUtils.isPhoneBookReady(subId)) {
			Toast.makeText(this, R.string.icc_phone_book_invalid, Toast.LENGTH_LONG).show();
			Log.i(TAG, "[doImportExport] phb is not ready.");
		} else {
			handleImportExportAction(subId);
		}
		/** @} */
	}

	public void doImportExport( AccountWithDataSetEx mCheckedAccount) {

	}

	private static class AccountsLoader extends AsyncTaskLoader<List<AccountWithDataSetEx>> {
		private Context mContext;

		public AccountsLoader(Context context) {
			super(context);
			mContext = context;
		}

		@Override
		public List<AccountWithDataSetEx> loadInBackground() {
			return loadAccountFilters(mContext);
		}

		@Override
		protected void onStartLoading() {
			forceLoad();
		}

		@Override
		protected void onStopLoading() {
			cancelLoad();
		}

		@Override
		protected void onReset() {
			onStopLoading();
		}
	}



	private static List<AccountWithDataSetEx> loadAccountFilters(Context context) {
		Log.d(TAG,"loadAccountFilters1");
		List<AccountWithDataSetEx> accountsEx = new ArrayList<AccountWithDataSetEx>();
		final AccountTypeManager accountTypes = AccountTypeManager.getInstance(context);
		List<AccountWithDataSet> accounts = accountTypes.getAccounts(true);

		for (AccountWithDataSet account : accounts) {
			AccountType accountType = accountTypes.getAccountType(account.type, account.dataSet);
			Log.d(TAG, "[loadAccountFilters]account.type = " + account.type
					+ ",account.name =" + account.name);
			if (accountType.isExtension() && !account.hasData(context)) {
				Log.d(TAG, "[loadAccountFilters]continue.");
				// Hide extensions with no raw_contacts.
				continue;
			}
			int subId = SubInfoUtils.getInvalidSubId();
			if (account instanceof AccountWithDataSetEx) {
				subId = ((AccountWithDataSetEx) account).getSubId();
			}
			Log.d(TAG, "[loadAccountFilters]subId = " + subId);
			accountsEx.add(new AccountWithDataSetEx(account.name, account.type, subId));
		}

		return accountsEx;
	}

	private boolean mIsFinished = false;
	private boolean isActivityFinished() {
		return mIsFinished;
	}

	@Override
	protected void onDestroy() {
		mIsFinished = true;
		super.onDestroy();
		Log.i(TAG, "[onDestroy]");
		try {
			unregisterReceiver(myReceiver);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class MyLoaderCallbacks implements LoaderCallbacks<List<AccountWithDataSetEx>> {
		@Override
		public Loader<List<AccountWithDataSetEx>> onCreateLoader(int id, Bundle args) {
			return new AccountsLoader(HbContactImportExportActivity.this);
		}

		@Override
		public void onLoadFinished(Loader<List<AccountWithDataSetEx>> loader,
				List<AccountWithDataSetEx> data) {
			// /check whether the Activity's status still ok
			if (isActivityFinished()) {
				Log.w(TAG, "[onLoadFinished]isActivityFinished is true,return.");
				return;
			}

			if (data == null) { // Just in case...
				Log.e(TAG, "[onLoadFinished]data is null,return.");
				//				runOnUiThread(new Runnable() {
				//					@Override
				//					public void run() {
				//						Toast.makeText(HbContactImportExportActivity.this, R.string.icc_phone_book_invalid, Toast.LENGTH_LONG).show();
				//					}
				//				});			
				return;
			}
			Log.d(TAG, "[onLoadFinished]data = " + data);
			mAccounts = data;
			// Add all of storages accounts
			//				mAccounts.addAll(getStorageAccounts());
			// If the accounts size is less than one item, we should not
			// show this view for user to import or export operations.
			if (mAccounts.size() <= 0) {
				Log.i(TAG, "[onLoadFinished]mAccounts.size = " + mAccounts.size());
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(),
								R.string.xport_error_one_account, Toast.LENGTH_SHORT).show();
					}
				});
				return;
			}
			Log.i(TAG, "[onLoadFinished]mAccounts.size() = " + mAccounts.size() +",mAccounts:"
					+ mAccounts);

			for (AccountWithDataSetEx account : mAccounts) {
				AccountTypeManager atm = AccountTypeManager.getInstance(HbContactImportExportActivity.this);
				AccountType accountType = atm.getAccountType(account
						.getAccountTypeWithDataSet());
				Log.d(TAG,"accountType:"+accountType);
				if (accountType.isIccCardAccount()) {
					int subId=account.getSubId();
					int slotId=SubscriptionManager.getSlotId(subId);
					Log.d(TAG, "[setShowingStep]isIccCardAccount,accountType: "
							+ accountType+" subId:"+subId+" slotId:"+slotId);
					if(slotId==0) mCheckedAccount1=account;
					else if(slotId==1) mCheckedAccount2=account;
				}
			}

			if(mCheckedAccount1==null && mCheckedAccount2==null) {
				//				runOnUiThread(new Runnable() {
				//					@Override
				//					public void run() {
				//						Toast.makeText(HbContactImportExportActivity.this, R.string.icc_phone_book_invalid, Toast.LENGTH_LONG).show();
				//					}
				//				});
				return;
			}
			Log.d(TAG,"mCheckedAccount1:"+mCheckedAccount1+" mCheckedAccount2:"+mCheckedAccount2);
			mPhoneAccount = new AccountWithDataSetEx("Phone","Local Phone Account",null);

			if(TextUtils.equals("importFromSIM1",source)){
				doImportExport(getSubId(0));
			}else if(TextUtils.equals("importFromSIM2",source)){
				doImportExport(getSubId(1));
			}else if(TextUtils.equals("importFromSIM",source)){
				doImportExport(getSingleSubId());
			}

		}

		@Override
		public void onLoaderReset(Loader<List<AccountWithDataSetEx>> loader) {
		}
	}

	private List<AccountWithDataSetEx> mAccounts = null;
	private String mCallingActivityName = null;
	private void handleImportExportAction(AccountWithDataSetEx mCheckedAccount) {
		Log.d(TAG, "[handleImportExportAction]...");
		Intent intent = new Intent(this,
				com.mediatek.contacts.list.ContactListMultiChoiceActivity.class)
				.setAction(ContactsIntent.LIST.ACTION_PICK_MULTI_CONTACTS)
				.putExtra("request_type",
						ContactsIntentResolverEx.REQ_TYPE_IMPORT_EXPORT_PICKER)
				.putExtra("toSDCard", false).putExtra("fromaccount", mCheckedAccount)
				.putExtra("toaccount", mPhoneAccount)
				.putExtra(VCardCommonArguments.ARG_CALLING_ACTIVITY, mCallingActivityName)
				.putExtra("isSelectedAddInitial", true);//默认全选
		startActivityForResult(intent, ContactImportExportActivity.REQUEST_CODE);
	}

	WakeLock wakeLock = null;
	private void acquireWakeLock(){
		if (null == wakeLock)  
		{  
			final PowerManager powerManager = (PowerManager) getApplicationContext()
					.getSystemService(Context.POWER_SERVICE);
			wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
					| PowerManager.ON_AFTER_RELEASE, TAG);
			if (null != wakeLock)  wakeLock.acquire();  
		}
	}

	//释放设备电源锁
	private void releaseWakeLock()  {
		if (null != wakeLock)  
		{  
			wakeLock.release();  
			wakeLock = null;  
		}  
	}
	
	
	private void handleImportExportAction(final int subId) {
		Log.d(TAG, "[handleImportExportAction],subId:"+subId);		
		if (subId > 0) {
			new AlertDialog.Builder(HbContactImportExportActivity.this)
			.setTitle(null) 
			.setMessage(getString(R.string.hb_confirm_import_sim_contacts_message))
			.setPositiveButton(getString(com.hb.R.string.ok), new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					prepareProgressDialogSpinner(null,getString(R.string.hb_import_sim_contacts_doing));
					acquireWakeLock();
					com.mediatek.contacts.simcontact.SlotUtils.clearActiveUsimPhbInfoMap();
					startSimService(HbContactImportExportActivity.this, subId, SIMServiceUtils.SERVICE_WORK_IMPORT);
				}
			})
			.setNegativeButton(getString(com.hb.R.string.cancel), null)
			.show(); 
		}
	}

	private void startSimService(Context context, int subId, int workType) {
		Intent intent = null;
		intent = new Intent(context, SIMProcessorService.class);
		intent.putExtra(SIMServiceUtils.SERVICE_SUBSCRIPTION_KEY, subId);
		intent.putExtra(SIMServiceUtils.SERVICE_WORK_TYPE, workType);
		Log.d(TAG, "[startSimService]subId:" + subId + "|workType:" + workType);
		context.startService(intent);
	}


	private boolean checkSDCardAvaliable(final String path) {
		if (TextUtils.isEmpty(path)) {
			Log.w(TAG, "[checkSDCardAvaliable]path is null!");
			return false;
		}
		StorageManager storageManager = (StorageManager)getSystemService(Context.STORAGE_SERVICE);
		if (null == storageManager) {
			Log.d(TAG, "-----story manager is null----");
			return false;
		}
		String storageState = storageManager.getVolumeState(path);
		Log.d(TAG, "[checkSDCardAvaliable]path = " + path + ",storageState = " + storageState);
		return storageState.equals(Environment.MEDIA_MOUNTED);
	}


	private boolean isSDCardFull(final String path) {
		if (TextUtils.isEmpty(path)) {
			Log.w(TAG, "[isSDCardFull]path is null!");
			return false;
		}
		Log.d(TAG, "[isSDCardFull] storage path is " + path);
		if (checkSDCardAvaliable(path)) {
			StatFs sf = null;
			try {
				sf = new StatFs(path);
			} catch (IllegalArgumentException e) {
				Log.e(TAG, "[isSDCardFull]catch exception:");
				e.printStackTrace();
				return false;
			}
			long availCount = sf.getAvailableBlocks();
			return !(availCount > 0);
		}

		return true;
	}

	private static boolean isStorageAccount(final Account account) {
		if (account != null) {
			return STORAGE_ACCOUNT_TYPE.equalsIgnoreCase(account.type);
		}
		return false;
	}
	//end

	public static final int REQUEST_CODE_PICK             = 109;
	public static final int REQUEST_CODE_IMPORT             = 110;
	public static final int REQUEST_CODE_PICK_FOR_SDCARD_EXPORT             = 111;
	public void importFromStorage(){
		if (VCardService.isProcessing(VCardService.TYPE_IMPORT)
				|| VCardService.isProcessing(VCardService.TYPE_EXPORT)) {
			Toast.makeText(this, R.string.contact_import_export_tips, Toast.LENGTH_SHORT)
			.show();
			return;
		}

		//			ActivitiesUtils.doImportExport(this);
		List<AccountWithDataSetEx> stores = getStorageAccounts();
		Log.d(TAG,"mSDToPhonePref:"+stores.size()+" stores:"+stores);
		if (stores != null && stores.size() > 0) {
			AccountWithDataSetEx mCheckedAccount1 = stores.get(0);
			AccountWithDataSetEx mCheckedAccount2 = new AccountWithDataSetEx("Phone","Local Phone Account",null);
			AccountSelectionUtil.doImportFromSdCard(this, isClickExternalSD?sdcardPath.toString():mCheckedAccount1.dataSet,
					mCheckedAccount2);
		} else {
			Toast.makeText(mActivity, "未找到内部存储器", Toast.LENGTH_LONG).show();
		}

	}

	public List<AccountWithDataSetEx> getStorageAccounts() {
		List<AccountWithDataSetEx> storageAccounts = new ArrayList<AccountWithDataSetEx>();
		StorageManager storageManager = (StorageManager) getApplicationContext().getSystemService(
				STORAGE_SERVICE);
		if (null == storageManager) {
			Log.w(TAG, "[getStorageAccounts]storageManager is null!");
			return storageAccounts;
		}
		String defaultStoragePath = StorageManagerEx.getDefaultPath();
		if (!storageManager.getVolumeState(defaultStoragePath).equals(Environment.MEDIA_MOUNTED)) {
			Log.w(TAG, "[getStorageAccounts]State is  not MEDIA_MOUNTED!");
			return storageAccounts;
		}

		// change for ALPS02390380, different user can use different storage, so change the API
		// to user related API.
		StorageVolume volumes[] = StorageManager.getVolumeList(UserHandle.myUserId(),
				StorageManager.FLAG_FOR_WRITE);
		if (volumes != null) {
			Log.d(TAG, "[getStorageAccounts]volumes are: " + volumes);
			for (StorageVolume volume : volumes) {
				String path = volume.getPath();
				//if (!Environment.MEDIA_MOUNTED.equals(path)) {
				//        continue;
				// }
				storageAccounts.add(new AccountWithDataSetEx(volume.getDescription(this),
						STORAGE_ACCOUNT_TYPE, path));
			}
		}
		return storageAccounts;
	}

	private File getExternalStorageDirectory() {
		//String path = StorageManagerEx.getDefaultPath();
		//        String path = StorageManagerEx.getExternalStoragePath();
		//        final File file = getDirectory(path, Environment.getExternalStorageDirectory().toString());
		final File file=new File(Environment.getExternalStorageDirectory().toString());
		Log.d(TAG, "[getExternalStorageDirectory]file.path : " + file.getPath());

		return file;
	}

	private final static int REPLACE_ATTACHMEN_MASK = 1 << 16;
	private int getRequestCode(int requestCode) {
		return requestCode & ~REPLACE_ATTACHMEN_MASK;
	}
	private NotificationManager nm;
	int notification_id=19172439; 
	int finish_notification_id=19172440;

	int notification_id_for_export=19172441; 
	int finish_notification_id_for_export=19172442;
	private Notification notification; 
	private PendingIntent contentIntent;
	@Override
	protected void onActivityResult(int maskResultCode, int resultCode, Intent data) {

		Log.d(TAG,"onActivityResult: requestCode=" + getRequestCode(maskResultCode) +
				", resultCode=" + resultCode + ", data=" + data);

		if (resultCode != RESULT_OK){
			Log.d(TAG,"bail due to resultCode=" + resultCode);
			return;
		}
		int requestCode = getRequestCode(maskResultCode);
		switch (requestCode) {
		case REQUEST_CODE_PICK:
			if (data != null) {

			}
			break;

		case REQUEST_CODE_PICK_FOR_SDCARD_EXPORT:
			if(data!=null){
				processPickResultHbForExportToSD(data);
			}
			break;

		case REQUEST_CODE_IMPORT:
			break;
		}
	}

	private static final String RESULT_INTENT_EXTRA_DATA_NAME = "com.mediatek.contacts.list.pickdataresult";
	private static final String RESULT_INTENT_EXTRA_CONTACTS_NAME = "com.mediatek.contacts.list.pickcontactsresult";

	private void processPickResultHbForExportToSD(Intent data) {

		long[] contactIds = data.getLongArrayExtra("com.mediatek.contacts.list.pickcontactsresult");

		if (contactIds == null || contactIds.length <= 0) {
			return;
		}

		StringBuilder selection = new StringBuilder();
		selection.append(Contacts._ID);
		selection.append(" IN (");
		selection.append(contactIds[0]);
		for (int i = 1; i < contactIds.length; i++) {
			selection.append(",");
			selection.append(contactIds[i]);
		}
		selection.append(")");

		String exportselection = selection.toString();
		Intent it = new Intent(this, ExportVCardActivity.class);
		it.putExtra("multi_export_type", 1); // TODO: 1 ,what's meaning?
		it.putExtra("exportselection", exportselection);
		//		Log.d(TAG,"isClickExternalSD:"+isClickExternalSD+" sdcardPath:"+sdcardPath.toString());
		it.putExtra("dest_path", isClickExternalSD?(sdcardPath==null?getExternalStorageDirectory().getPath():sdcardPath.toString()):getExternalStorageDirectory().getPath());
		it.putExtra(VCardCommonArguments.ARG_CALLING_ACTIVITY, "PeopleActivity");
		startActivity(it);
	}




	private boolean isRunningForeground()  
	{
		ActivityManager am = (ActivityManager)mActivity.getSystemService(Context.ACTIVITY_SERVICE);  
		ComponentName cn = am.getRunningTasks(1).get(0).topActivity;  
		String currentPackageName = cn.getPackageName();  
		if(!TextUtils.isEmpty(currentPackageName) && currentPackageName.equals("com.android.contacts"))  
		{  
			return true;  
		}         
		return false;  
	} 

	private static final int EVENT_CONTACTS_DELETED = 9;
	private static final int UPDATE_NOTIFICATION_PROGRESSBAR = 10;
	private static final int CANCEL_NOTIFICATION_PROGRESSBAR = 11;
	private static final int UPDATE_NOTIFICATION_PROGRESSBAR_FOR_EXPORT=12;
	private static final int UPDATE_NOTIFICATION_PROGRESSBAR_FOR_EXPORT_PART_FULL=13;
	private static final int  USER_CANCEL_IMPORT=14;
	private static final int  USER_CANCEL_EXPORT=15;
	private static final int  UPDATE_SIM_PREFERENCE=16;
	int alarmCount=0;
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case EVENT_CONTACTS_DELETED:
				if(mProgressDialog!=null) {
					mProgressDialog.dismiss();

				}
				int result = (Integer)msg.obj;
				if (result == 1) {
					showAlertDialog("删除成功");
				} else {
					showAlertDialog("删除失败");
				}
				break;

			case UPDATE_NOTIFICATION_PROGRESSBAR:{
				break;
			}

			case UPDATE_NOTIFICATION_PROGRESSBAR_FOR_EXPORT:{
				int rate=(Integer)msg.obj;
				Log.d(TAG,"rate:"+rate+" total:"+totalContactsForExport);;
				//				notification.contentView.setProgressBar(R.id.pb, totalContactsForExport,rate, false); 
				//				notification.contentView.setTextViewText(R.id.text2, "("+rate+"/"+totalContactsForExport+")");
				if(rate==totalContactsForExport) {
					nm.cancel(notification_id_for_export);	
					nm.cancel(finish_notification_id_for_export);	

					String resultString=getString(R.string.hb_export_sim_contacts_result,rate);
					Intent notificationIntent = new Intent(HbContactImportExportActivity.this,HbContactImportExportActivity.class);
					notificationIntent.putExtra("from", "importfromsim");
					notificationIntent.putExtra("importSimContactsCount", rate);
					notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

					PendingIntent contentIntent = PendingIntent.getActivity(HbContactImportExportActivity.this,alarmCount++,notificationIntent,PendingIntent.FLAG_UPDATE_CURRENT);   

					Notification notification = new Notification(R.drawable.ic_launcher_contacts, 
							resultString, System.currentTimeMillis());
					notification.flags = Notification.FLAG_AUTO_CANCEL;
					notification.setLatestEventInfo(HbContactImportExportActivity.this, resultString,null, contentIntent); 					
					nm.notify(finish_notification_id_for_export, notification);
				}else{
					//					nm.notify(notification_id_for_export, notification); 

					String textString=getString(R.string.hb_export_sim_contacts_doing);
					final Notification notification1 = NotificationImportExportListener.hbConstructProgressNotification(HbContactImportExportActivity.this,
							VCardService.TYPE_IMPORT, textString, textString, 0, null, totalContactsForExport, rate,contentIntent);
					nm.notify(notification_id_for_export,notification1);
				}
				break;
			}

			case UPDATE_NOTIFICATION_PROGRESSBAR_FOR_EXPORT_PART_FULL:{
				int rate=(Integer)msg.obj;
				Log.d(TAG,"rate:"+rate+" total:"+totalContactsForExport);;
				//				notification.contentView.setProgressBar(R.id.pb, totalContactsForExport,rate, false); 	
				nm.cancel(notification_id_for_export);	
				nm.cancel(finish_notification_id_for_export);	

				String resultString=getString(R.string.hb_import_sim_contacts_part_success,rate,totalContactsForExport-rate);
				Intent notificationIntent = new Intent(HbContactImportExportActivity.this,HbContactImportExportActivity.class);
				notificationIntent.putExtra("from", "importfromsim");
				notificationIntent.putExtra("importSimContactsCount", rate);
				notificationIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);					
				PendingIntent contentIntent = PendingIntent.getActivity(HbContactImportExportActivity.this,0,notificationIntent,0);   

				Notification notification = new Notification(R.drawable.ic_launcher_contacts, 
						resultString, System.currentTimeMillis());
				notification.flags = Notification.FLAG_AUTO_CANCEL;
				notification.setLatestEventInfo(HbContactImportExportActivity.this, resultString,null, contentIntent); 					
				nm.notify(finish_notification_id_for_export, notification);
				break;
			}

			case CANCEL_NOTIFICATION_PROGRESSBAR:
				break;

			case USER_CANCEL_IMPORT:{
				int rate2=(Integer)msg.obj;
				Toast.makeText(HbContactImportExportActivity.this, getString(R.string.hb_import_sim_contacts_result_with_cancel,rate2),
						Toast.LENGTH_LONG).show();
				break;
			}

			case USER_CANCEL_EXPORT:{
				if(mProgressDialog!=null) {
					mProgressDialog.dismiss();
				}
				int rate2=(Integer)msg.obj;
				Toast.makeText(HbContactImportExportActivity.this, getString(R.string.hb_export_sim_contacts_result_with_cancel,rate2),
						Toast.LENGTH_LONG).show();
				nm.cancel(notification_id_for_export);	
				nm.cancel(finish_notification_id_for_export);
				break;
			}

			//			case UPDATE_SIM_PREFERENCE:{
			//				updateSimPreference(slot0Status==1||slot1Status==1);
			//				break;
			//			}

			default:
				break;
			}
		}
	};

	void prepareProgressDialog(String title, String message,int count) {
		Log.d(TAG,"prepareProgressDialog");
		if(mProgressDialog!=null) {
			mProgressDialog.dismiss();

		}
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setTitle(title);
		mProgressDialog.setMessage(message);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setProgress(0);
		mProgressDialog.setMax(count);
		mProgressDialog.show();
		mProgressDialog.setCanceledOnTouchOutside(false);

		mProgressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				Log.d(TAG, "onKey:"+keyCode);
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					return true;
				}
				return false;
			}
		});
	}

	void prepareProgressDialogSpinner(String title, String message) {
		if(mProgressDialog!=null) {
			mProgressDialog.dismiss();

		}

		mProgressDialog = new ProgressDialog(this);
		if(title!=null) mProgressDialog.setTitle(title);
		mProgressDialog.setMessage(message);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgressDialog.setCanceledOnTouchOutside(false);

		mProgressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				Log.d(TAG, "onKey:"+keyCode);
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					return true;
				}
				return false;
			}
		});

		mProgressDialog.show();
	}

	private ProgressDialog mProgressDialog;


	protected void showAlertDialog(String value) {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle("结果");
		alertDialog.setMessage(value);
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				//Just to provide information to user no need to do anything.
			}
		});
		alertDialog.show();
	}

	public boolean isMultiSimEnabled(Context mContext){
		if(mContext==null) return false;
		int activeSubscriptionInfoCount=SubscriptionManager.from(mContext).getActiveSubscriptionInfoCount();
		Log.d(TAG,"activeSubscriptionInfoCount:"+activeSubscriptionInfoCount);

		if(activeSubscriptionInfoCount<2) {
			isMultiSimEnabled=false;
			return isMultiSimEnabled;
		}

		int[] subIds0=SubscriptionManager.getSubId(0);
		int[] subIds1=SubscriptionManager.getSubId(1);
		int subId0=-1,subId1=-1;
		if(subIds0!=null&&subIds0.length>0) subId0=subIds0[0];
		if(subIds1!=null&&subIds1.length>0) subId1=subIds1[0];

		Log.d(TAG, "subId0:"+subId0+" subId1:"+subId1);

		boolean isRadioOn0=isRadioOn(subId0, mContext);
		boolean isRadioOn1=isRadioOn(subId1,mContext);

		Log.d(TAG, "isRadioOn0:"+isRadioOn0+" isRadioOn1"+isRadioOn1);
		if(isRadioOn0 && isRadioOn1) isMultiSimEnabled=true;
		else isMultiSimEnabled=false;
		return isMultiSimEnabled;
	}

	public int getSingleSubId(){
		int[] subIds0=SubscriptionManager.getSubId(0);
		int[] subIds1=SubscriptionManager.getSubId(1);
		int subId0=-1,subId1=-1;
		if(subIds0!=null&&subIds0.length>0) subId0=subIds0[0];
		if(subIds1!=null&&subIds1.length>0) subId1=subIds1[0];		

		boolean isRadioOn0=isRadioOn(subId0, HbContactImportExportActivity.this);
		boolean isRadioOn1=isRadioOn(subId1,HbContactImportExportActivity.this);

		if(isRadioOn0 && !isRadioOn1) return subId0;
		else return subId1;
	}

	public boolean isAnySimEnabled(Context mContext){
		int activeSubscriptionInfoCount=SubscriptionManager.from(mContext).getActiveSubscriptionInfoCount();
		Log.d(TAG,"activeSubscriptionInfoCount:"+activeSubscriptionInfoCount);

		if(activeSubscriptionInfoCount<1) return false;

		int[] subIds0=SubscriptionManager.getSubId(0);
		int[] subIds1=SubscriptionManager.getSubId(1);
		int subId0=-1,subId1=-1;
		if(subIds0!=null&&subIds0.length>0) subId0=subIds0[0];
		if(subIds1!=null&&subIds1.length>0) subId1=subIds1[0];

		boolean isRadioOn0=isRadioOn(subId0, mContext);
		boolean isRadioOn1=isRadioOn(subId1,mContext);

		Log.d(TAG, "subId0:"+subId0+" subId1:"+subId1+" isRadioOn0:"+isRadioOn0+" isRadioOn1:"+isRadioOn1);
		return isRadioOn0 || isRadioOn1;
	}

	public int getSubId(int slotId){
		int[] subIds=SubscriptionManager.getSubId(slotId);
		int subId=-1;
		if(subIds!=null&&subIds.length>0) {
			subId=subIds[0];
		}
		Log.d(TAG, "subId:"+subId);
		return subId;
	}


	public boolean isRadioOn(int subId, Context context) {
		ITelephony phone = ITelephony.Stub.asInterface(ServiceManager
				.getService(Context.TELEPHONY_SERVICE));
		boolean isOn = false;
		try {
			// for ALPS02460942, during SIM switch, radio is unavailable, consider it as OFF
			if (phone != null) {
				isOn = subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID ? false :
					phone.isRadioOnForSubscriber(subId, context.getPackageName());
			} else {
				Log.d(TAG, "capability switching, or phone is null ? " + (phone == null));
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		Log.d(TAG, "isOn = " + isOn + ", subId: " + subId);
		return isOn;
	}


}
