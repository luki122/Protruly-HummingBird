/*
 * Copyright (C) 2014 MediaTek Inc.
 * Modification based on code covered by the mentioned copyright
 * and/or permission notice(s).
 */
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
 * limitations under the License
 */

package com.android.contacts.activities;

import hb.app.dialog.ProgressDialog;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import  hb.view.menu.bottomnavigation.BottomNavigationView;
import  hb.view.menu.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;
import hb.widget.toolbar.Toolbar;
import hb.widget.ActionMode;
import hb.widget.ActionModeListener;
import hb.widget.ActionMode.Item;
import android.accounts.Account;
import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import hb.app.dialog.AlertDialog;
import hb.app.dialog.AlertDialog.Builder;
//import hb.widget.HbListView;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Trace;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsActivity;
import com.android.contacts.ContactsApplication;
import com.android.contacts.R;
import com.android.contacts.quickcontact.QuickContactActivity;
import com.hb.record.PhoneCallRecord;
import com.hb.record.RecordParseUtil;
import com.android.contacts.quickcontact.ExpandingEntryCardView.Entry;
import com.android.contacts.interactions.CallLogInteraction;
import com.android.contacts.interactions.CallLogInteractionsLoader2;
import com.android.contacts.interactions.ContactInteraction;

public class CallLogDetailActivity extends ContactsActivity implements hb.widget.toolbar.Toolbar.OnMenuItemClickListener{
	private ActionMode actionMode;
	private BottomNavigationView bottomBar;
	public Toolbar toolbar;
	public ListView lv;
	private static final String KEY_LOADER_EXTRA_PHONES =
			QuickContactActivity.class.getCanonicalName() + ".KEY_LOADER_EXTRA_PHONES";

	private static final String TAG = "CallLogDetailActivity";

	/// M:
	private static final int LOADER_CALL_LOG_ID = 3;

	private Map<Integer, List<ContactInteraction>> mRecentLoaderResults =
			new ConcurrentHashMap<>(4, 0.9f, 1);
	private AsyncTask<Void, Void, Void> mRecentDataTask;
	Activity mContext;
	//	final List<ContactInteraction> allInteractions = new ArrayList<>();

	@Override
	protected void onResume() {
		startInteractionLoaders();
		super.onResume();
		ContactsApplication.isMultiSimEnabled(this);
	}

	private String[] phoneNumbers;
	private void startInteractionLoaders() {
		final Bundle phonesExtraBundle = new Bundle();
		phonesExtraBundle.putStringArray(KEY_LOADER_EXTRA_PHONES, phoneNumbers);

		Trace.beginSection("start call log loader");
		getLoaderManager().initLoader(
				LOADER_CALL_LOG_ID,
				phonesExtraBundle,
				mLoaderInteractionsCallbacks);
		Trace.endSection();
	}

	private final LoaderCallbacks<List<ContactInteraction>> mLoaderInteractionsCallbacks =
			new LoaderCallbacks<List<ContactInteraction>>() {

		@Override
		public Loader<List<ContactInteraction>> onCreateLoader(int id, Bundle args) {
			Loader<List<ContactInteraction>> loader = null;
			loader = new CallLogInteractionsLoader2(
					CallLogDetailActivity.this,
					args.getStringArray(KEY_LOADER_EXTRA_PHONES),
					5);
			return loader;
		}

		@Override
		public void onLoadFinished(Loader<List<ContactInteraction>> loader, List<ContactInteraction> data) {
			mRecentLoaderResults.put(loader.getId(), data);
			bindRecentData();
		}

		@Override
		public void onLoaderReset(Loader<List<ContactInteraction>> loader) {
			mRecentLoaderResults.remove(loader.getId());

		}
	};

	private void bindRecentData() {
		final List<ContactInteraction> allInteractions = new ArrayList<>();
		final List<List<Entry>> interactionsWrapper = new ArrayList<>();

		// Serialize mRecentLoaderResults into a single list. This should be done on the main
		// thread to avoid races against mRecentLoaderResults edits.
		for (List<ContactInteraction> loaderInteractions : mRecentLoaderResults.values()) {
			allInteractions.addAll(loaderInteractions);
		}
		Log.e("liumx-xxx", "allInteractions size : "+ (allInteractions == null? 0:allInteractions.size()));
		mRecentDataTask = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				Trace.beginSection("sort recent loader results");

				// Sort the interactions by most recent
				Collections.sort(allInteractions, new Comparator<ContactInteraction>() {
					@Override
					public int compare(ContactInteraction a, ContactInteraction b) {
						if (a == null && b == null) {
							return 0;
						}
						if (a == null) {
							return 1;
						}
						if (b == null) {
							return -1;
						}
						if (a.getInteractionDate() > b.getInteractionDate()) {
							return -1;
						}
						if (a.getInteractionDate() == b.getInteractionDate()) {
							return 0;
						}
						return 1;
					}
				});

				Trace.endSection();
				//                Trace.beginSection("contactInteractionsToEntries");
				//
				//                // Wrap each interaction in its own list so that an icon is displayed for each entry
				//				for (ContactInteraction contactInteraction : allInteractions) {
				//					Log.e("liumx-xxx", "contactInteraction  : "+ (contactInteraction.getViewHeader(mContext)));
				//					//                    List<Entry> entryListWrapper = new ArrayList<>(1);
				//					//                    entryListWrapper.add(contactInteraction);
				//					//                    interactionsWrapper.add(entryListWrapper);
				//				}
				//                
				//                Trace.endSection();
				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {
				super.onPostExecute(aVoid);
				Trace.beginSection("initialize recents card");
				
				if (allInteractions.size() > 0) {
					final CallDetailHistoryAdapter adapter = new CallDetailHistoryAdapter(allInteractions);

					lv.setAdapter(adapter);
//					lv.setOnItemClickListener(null);
					lv.setOnItemClickListener(new OnItemClickListener(){
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						Log.d(TAG, "onItemClick position = " + position);
						adapter.playRecord(position);
					}
				});
					//                    mRecentCard.initialize(interactionsWrapper,
					//                    /* numInitialVisibleEntries = */ MIN_NUM_COLLAPSED_RECENT_ENTRIES_SHOWN,
					//                    /* isExpanded = */ mRecentCard.isExpanded(), /* isAlwaysExpanded = */ false,
					//                            mExpandingEntryCardViewListener, mScroller);
					lv.setVisibility(View.VISIBLE);
				} else {
					// M: Fix ALPS01763309
					lv.setVisibility(View.GONE);
				}
				showPhoneRecords(allInteractions);
				Trace.endSection();
				mRecentDataTask = null;
			}
		};
		
		mRecentDataTask.execute();
		
	}

	private boolean isForAddFlag=false;
	AlertDialog builder=null;

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		Log.d(TAG,"onMenuItemClick:"+item.getItemId());
		switch (item.getItemId()) {
		case R.id.menu_remove_from_call_log:

			new AlertDialog.Builder(CallLogDetailActivity.this)
			.setTitle(null) 
			.setMessage(R.string.slide_delete_calllog_message)
			.setPositiveButton(mContext.getString(com.hb.R.string.ok), new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					deleteCallsByNumber();
				}
			})
			.setNegativeButton(mContext.getString(com.hb.R.string.cancel), null)
			.show();			
			break;
		}
		return false;
	}

	/**
	 * Delete specified calls from the call log.
	 *
	 * @param context The context.
	 * @param callIds String of the callIds to delete from the call log, delimited by commas (",").
	 */
	public void deleteCallsByNumber() {
		new AsyncTask<Void, Void, Void>() {

			@Override
			public Void doInBackground(Void... params) {				
				if(phoneNumbers==null || phoneNumbers.length<1) return null;
				StringBuilder sb=new StringBuilder("number in(");
				for(int i=0;i<phoneNumbers.length;i++){
					sb.append("'"+phoneNumbers[i].replace(" ", "")+"',");
				}
				sb.delete(sb.length()-1,sb.length());
				sb.append(") AND reject=0");
				Log.d(TAG, "sb:"+sb.toString());
				mContext.getContentResolver().delete(
						Uri.parse("content://call_log/calls"),
						sb.toString(), null);
				return null;
			}

			@Override
			protected void onPreExecute() {
				// TODO Auto-generated method stub
				super.onPreExecute();
				prepareProgressDialogSpinner(null,mContext.getString(R.string.hb_clearing_calllog));
			}



			@Override
			public void onPostExecute(Void result) {
				if(mProgressDialog!=null) mProgressDialog.dismiss();
				Toast.makeText(CallLogDetailActivity.this,mContext.getString(R.string.hb_cleared_calllog), Toast.LENGTH_LONG).show();
				finish();
			}
		}.execute();

	}
	
	private void showPhoneRecords(final List<ContactInteraction> data) {
		new com.hb.record.SimpleAsynTask() {
			@Override
			protected Integer doInBackground(Integer... params) {
				boolean founded = RecordParseUtil.foundAndSetPhoneRecords(data);
				return founded ? 1 : 0;
			}

			@Override
			protected void onPostExecute(Integer result) {
				if (0 == result) {
					return;
				}
				if (null != lv && null != lv.getAdapter()) {
					((BaseAdapter) (lv.getAdapter()))
					.notifyDataSetChanged();
				}
			}
		}.execute();
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

	class CallDetailHistoryAdapter extends BaseAdapter {

		List<ContactInteraction> allInteractions;
		LayoutInflater mLayoutInflater;

		CallDetailHistoryAdapter(List<ContactInteraction> allInteractions) {
			this.allInteractions = allInteractions;
			mLayoutInflater = LayoutInflater.from(mContext);
		}
		@Override
		public int getCount() {
			return allInteractions.size();
		}

		@Override
		public Object getItem(int position) {
			return allInteractions.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final View result  = convertView == null? mLayoutInflater.inflate(R.layout.call_detail_history_item, parent, false):convertView;

			ContactInteraction details = allInteractions.get(position);
			//        CallTypeIconsView callTypeIconView =
			//                (CallTypeIconsView) result.findViewById(R.id.call_type_icon);
			TextView callTypeTextView = (TextView) result.findViewById(R.id.call_type_text);
			TextView dateView = (TextView) result.findViewById(R.id.date);
			TextView durationView = (TextView) result.findViewById(R.id.duration);
			TextView phoneNumber = (TextView) result.findViewById(R.id.phone_number);
			phoneNumber.setText(details.getViewHeader(mContext));
			durationView.setText(details.getViewBody(mContext));
			Log.d(TAG,"details.getViewBody(mContext):"+details.getViewBody(mContext));
			ImageView recordView=(ImageView)result.findViewById(R.id.callrecord_icon);
			//add by lgy for record 
            if(((CallLogInteraction)details).getPhoneRecords() != null) {
                recordView.setVisibility(View.VISIBLE);
            } else {
                recordView.setVisibility(View.GONE);
                result.setBackgroundColor(android.R.color.white);
            }
			String mDate=longToString(details.getInteractionDate(), "yyyy年MM月dd日 HH:mm");
			dateView.setText(mDate);

			ImageView simView=(ImageView)result.findViewById(R.id.sim_icon);					
			if(simView!=null){
				if(ContactsApplication.isMultiSimEnabled){
//					simView.setBackground(details.getSimIcon(mContext));
					

					int slotId=-1;
					try{
						slotId=SubscriptionManager.getSlotId(Integer.parseInt(details.getSimName(mContext)));
					}catch(Exception e){
						Log.d(TAG, "e:"+e);
					}
					if (slotId == 1) {
						simView.setBackground(mContext.getDrawable(R.drawable.hb_sim2_icon));
						simView.setVisibility(View.VISIBLE);
					} else if (slotId == 0) {
						simView.setBackground(mContext.getDrawable(R.drawable.hb_sim1_icon));
						simView.setVisibility(View.VISIBLE);
					}else{
						simView.setVisibility(View.GONE);
					}
				}else{
					simView.setVisibility(View.GONE);
				}
			} else simView.setVisibility(View.GONE);
			
//			Log.d(TAG,"simname:"+details.getSimName(mContext));
			return result;
		}
		
		//add by lgy for record
		public void playRecord(int position) {
			final List<PhoneCallRecord> mPhoneRecords = ((CallLogInteraction)(allInteractions.get(position))).getPhoneRecords();//List<PhoneCallRecord>[position];
			playRecord(mPhoneRecords);
		}
		
		private void playRecord(final List<PhoneCallRecord> records) {
			if (null == records) {
				return;
			}

			final int size = records.size();

			if (size == 1) {
				playRecord(records.get(0));
				return;
			}

			CharSequence[] items = new CharSequence[size];
			for (int i = 0; i < items.length; i++) {
				items[i] = new File(records.get(i).getPath()).getName().substring(0, 13) + ".amr";
			}
			new AlertDialog.Builder(mContext)
			.setTitle(R.string.record_settings_label)
			.setItems(items, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					playRecord(records.get(which));
				}
			})
			.show();
		}
		
		private void playRecord(PhoneCallRecord record) {
			if (null == record) {
				return;
			}

			Uri data = Uri.fromFile(new File(record.getPath()));  
			Intent intent = new Intent(Intent.ACTION_VIEW);  
			//        intent.setClassName("com.android.music", "com.android.music.AudioPreview");
			intent.setDataAndType(data, record.getMimeType());                
			try {
				mContext.startActivity(intent);
			} catch(Exception e) {
				e.printStackTrace();
				Toast.makeText(
						mContext,
						mContext.getResources().getString(
								R.string.no_music_activity), Toast.LENGTH_SHORT)
								.show();
			}

		}

	}

	// date类型转换为String类型
	// formatType格式为yyyy-MM-dd HH:mm:ss//yyyy年MM月dd日 HH时mm分ss秒
	// data Date类型的时间
	public String dateToString(Date data, String formatType) {
		return new SimpleDateFormat(formatType).format(data);
	}

	// string类型转换为date类型
	// strTime要转换的string类型的时间，formatType要转换的格式yyyy-MM-dd HH:mm:ss//yyyy年MM月dd日
	// HH时mm分ss秒，
	// strTime的时间格式必须要与formatType的时间格式相同
	public Date stringToDate(String strTime, String formatType)
			throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat(formatType);
		Date date = null;
		date = formatter.parse(strTime);
		return date;
	}

	// long转换为Date类型
	// currentTime要转换的long类型的时间
	// formatType要转换的时间格式yyyy-MM-dd HH:mm:ss//yyyy年MM月dd日 HH时mm分ss秒
	public Date longToDate(long currentTime, String formatType)
			throws ParseException {
		Date dateOld = new Date(currentTime); // 根据long类型的毫秒数生命一个date类型的时间
		String sDateTime = dateToString(dateOld, formatType); // 把date类型的时间转换为string
		Date date = stringToDate(sDateTime, formatType); // 把String类型转换为Date类型
		return date;
	}

	// long类型转换为String类型
	// currentTime要转换的long类型的时间
	// formatType要转换的string类型的时间格式
	public String longToString(long currentTime, String formatType) {
		try{
			Date date = longToDate(currentTime, formatType); // long类型转成Date类型
			int year=date.getYear();

			long now=System.currentTimeMillis();
			Date nowDate=longToDate(now, formatType);
			int nowYear=nowDate.getYear();
			if(year==nowYear) formatType="MM月dd日 HH:mm";

			String strTime = dateToString(date, formatType); // date类型转成String
			return strTime;
		}catch(Exception e){
			Log.d(TAG,"e:"+e);
			return "时间未知";
		}
	}


	@Override
	public void onCreate(Bundle savedState) {
		Log.d(TAG,"onCreate");
		super.onCreate(savedState);
		mContext = CallLogDetailActivity.this;

		// TODO: Create Intent Resolver to handle the different ways users can get to this list.
		// TODO: Handle search or key down

		setHbContentView(R.layout.calllog_detail_activity);
		lv = (ListView)findViewById(R.id.list);
		//lv.setAdapter(adapter);
		toolbar = getToolbar();
		toolbar.setElevation(0f);
		toolbar.setTitle(getResources().getString(R.string.hb_all_calls));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.d(TAG,"NavigationOnClickListener");
				finish();
			}
		});
		setupActionModeWithDecor(toolbar);
		toolbar.inflateMenu(R.menu.all_calllog);

		Intent intent=getIntent();
		if(intent != null) {
			phoneNumbers=intent.getStringArrayExtra(KEY_LOADER_EXTRA_PHONES);
			Log.d(TAG,"phoneNumbers:"+Arrays.toString(phoneNumbers)+" KEY_LOADER_EXTRA_PHONES:"+KEY_LOADER_EXTRA_PHONES);
		}
		//		phoneNumbers =new String[]{"123","1234"};//for test

		
	}
}
