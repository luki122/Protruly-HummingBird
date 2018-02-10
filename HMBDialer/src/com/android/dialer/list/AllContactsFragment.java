/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.dialer.list;

import hb.view.menu.BottomWidePopupMenu;
import  hb.view.menu.bottomnavigation.BottomNavigationView;
import com.android.dialer.widget.EmptyContentView;
import hb.widget.HbIndexBar;
import static android.Manifest.permission.READ_CONTACTS;

import android.accounts.Account;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.QuickContact;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.contacts.common.list.ContactEntryListAdapter;
import com.android.contacts.common.list.ContactEntryListFragment;
import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.list.DefaultContactListAdapter;
import com.android.contacts.common.list.DirectoryPartition;
import com.android.contacts.common.util.PermissionsUtil;
import com.android.contacts.common.util.ViewUtil;
import com.android.contacts.common.list.ProfileAndContactsLoader;
import com.android.contacts.common.list.ContactListAdapter.ContactQuery;
import com.android.contacts.common.preference.ContactsPreferences;
import com.android.dialer.DialtactsActivity;
import com.android.dialer.R;
import com.android.dialer.calllog.CallLogAsyncTaskUtil;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.IntentUtil;
import com.android.dialer.widget.EmptyContentView.OnEmptyViewActionButtonClickedListener;
/**
 * Fragments to show all contacts with phone numbers.
 */
public class AllContactsFragment extends ContactEntryListFragment<ContactEntryListAdapter>
implements OnEmptyViewActionButtonClickedListener {

	private static final int READ_CONTACTS_PERMISSION_REQUEST_CODE = 1;

	private static final String TAG = "AllContactsFragment";


	//	/**
	//	 * Listen to broadcast events about permissions in order to be notified if the READ_CONTACTS
	//	 * permission is granted via the UI in another fragment.
	//	 */
	//	private BroadcastReceiver mReadContactsPermissionGrantedReceiver = new BroadcastReceiver() {
	//		@Override
	//		public void onReceive(Context context, Intent intent) {
	//			reloadData();
	//		}
	//	};

	public AllContactsFragment() {
		setQuickContactEnabled(false);
		setAdjustSelectionBoundsEnabled(true);
		setPhotoLoaderEnabled(true);
		setSectionHeaderDisplayEnabled(true);
		setDarkTheme(false);
		setVisibleScrollbarEnabled(true);

	}

	@Override
	public void onResume() {
		Log.d(TAG,"onResumt");
		super.onResume();
	}

	//	@Override
	//	protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
	//		super.onCreateView(inflater, container);
	//		mSearchHeaderView = inflater.inflate(R.layout.search_header, null, false);
	//		mSearchHeaderView.setOnClickListener(new View.OnClickListener() {
	//			
	//			@Override
	//			public void onClick(View v) {
	//				// TODO Auto-generated method stub
	//				
	//			}
	//		});
	//		getListView().addHeaderView(mSearchHeaderView, null, false);
	//	}

	public void exitSearchMode(boolean hasActionMode){
		super.exitSearchMode(false);
		((DialtactsActivity)getActivity()).showFAB(true);
		if(((DialtactsActivity)getActivity()).mListsFragment.mViewPager!=null) 
			((DialtactsActivity)getActivity()).mListsFragment.mViewPager.setScrollble(true);

	}

	public void enterSearchMode(boolean hasActionMode){
		super.enterSearchMode(false);

		((DialtactsActivity)getActivity()).showFAB(false);
		if(((DialtactsActivity)getActivity()).mListsFragment.mViewPager!=null) 
			((DialtactsActivity)getActivity()).mListsFragment.mViewPager.setScrollble(false);

	}
	@Override
	public void onViewCreated(View view, android.os.Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mEmptyListView = (EmptyContentView) view.findViewById(R.id.empty_list_view);
		//		mEmptyListView.setImage(R.drawable.empty_contacts);
		//		mEmptyListView.setDescription(R.string.all_contacts_empty);
		mEmptyListView.setActionClickedListener(this);
		//		getListView().setEmptyView(mEmptyListView);
		mEmptyListView.setVisibility(View.GONE);

		//		ColorStateList csl=(ColorStateList)getActivity().getResources().getColorStateList(R.color.hb_indexbar_letter_color_selector);
		//		mIndexBar.setColor(csl);
		setmIndexBar((HbIndexBar) view.findViewById(R.id.index_bar));
		initHbSearchView();

		mHbSearchView.setOnQueryTextFocusChangeListener(new OnFocusChangeListener(){
			public void onFocusChange(View v, boolean hasFocus){
				Log.d(TAG,"onFocusChange,mHbIsSearchMode:"+mHbIsSearchMode);
				if(!mHbIsSearchMode){
					enterSearchMode(false);
					setShowEmptyListForNullQuery(true);
					setQueryString(null,true);
					if(mEmptyListView!=null) mEmptyListView.setVisibility(View.GONE);	
				}
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();
		//		PermissionsUtil.registerPermissionReceiver(getActivity(),
		//				mReadContactsPermissionGrantedReceiver, READ_CONTACTS);	
	}

	@Override
	public void onStop() {
		//		PermissionsUtil.unregisterPermissionReceiver(getActivity(),
		//				mReadContactsPermissionGrantedReceiver);
		super.onStop();
	}

	@Override
	public void startLoading() {
		if (PermissionsUtil.hasPermission(getActivity(), READ_CONTACTS)) {
			super.startLoading();
			//			mEmptyListView.setDescription(R.string.all_contacts_empty);
			//			mEmptyListView.setActionLabel(R.string.all_contacts_empty_add_contact_action);
		} else {
			mEmptyListView.setDescription(R.string.permission_no_contacts);
			mEmptyListView.setActionLabel(R.string.permission_single_turn_on);
			mEmptyListView.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		nameColumnIndex=ContactQuery.CONTACT_DISPLAY_NAME;
		if(!isSearchMode()){
			quanpinyinColumnIndex=ContactQuery.QUAN_PINYIN;
			jianpinyinColumnIndex=ContactQuery.JIAN_PINYIN;
			phonebookbucketColumnIndex=ContactQuery.PHONEBOOK_BUCKET;
			snippetColumnIndex=ContactQuery.CONTACT_SNIPPET;
			_PROJECTION=ContactQuery.CONTACT_PROJECTION_PRIMARY;
		}else{
			jianpinyinColumnIndex=12;
		}

		super.onLoadFinished(loader, data);		
		//控制搜索栏、字母导航条与空页面的显示与隐藏
		if (data == null || data.getCount() == 0) {
			if(mEmptyListView!=null){
				if(mEmptyListView.getVisibility()!=View.VISIBLE) mEmptyListView.setVisibility(View.VISIBLE);
				if(isSearchMode()){
					mEmptyListView.setImage(EmptyContentView.NO_IMAGE);
					mEmptyListView.setDescription(R.string.hb_find_empty_contacts);
					mEmptyListView.setActionLabel(EmptyContentView.NO_LABEL);
					mEmptyListView.showButtons(false);
					Log.d(TAG,"onLoadFinished2");
				}else{
					mEmptyListView.setImage(EmptyContentView.NO_IMAGE);
					mEmptyListView.setDescription(EmptyContentView.NO_LABEL);
					mEmptyListView.setActionLabel(EmptyContentView.NO_LABEL);
					mEmptyListView.showButtons(true);
					Log.d(TAG,"onLoadFinished3");
				}
			}
		}else {
			if(mEmptyListView!=null && mEmptyListView.getVisibility()!=View.GONE) mEmptyListView.setVisibility(View.GONE);
		}
		
		if(!com.android.contacts.common.HbUtils.isMTK && data!=null){
			final Bundle bundle=data.getExtras();
			if(bundle!=null) {
				mOwnerInfoDiaplayNameTextView.setText(bundle.getString("displayName"));
				mOwnerInfoNumberTextView.setText(bundle.getString("displayName"));
				long photoId=bundle.getLong("photoId");
		        if (photoId != 0) {
		        	mPhotoManager.loadThumbnail(mOwnerInfoPhotoImageView, photoId, false, true,
		                    null);
		        	ownerHeader.setOnClickListener(new View.OnClickListener() {
						
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							Log.d(TAG,"ownerHeader click");
							long contactId = bundle.getLong("contactId");
							String lookupKey =  bundle.getString("lookup");
							Uri uri = Contacts.getLookupUri(contactId, lookupKey);
							if (uri != null) {
								QuickContact.showQuickContact(getContext(), ownerHeader, uri, null,
										Phone.CONTENT_ITEM_TYPE);
							}
						}
					});
		        }
			}
		}		
	}

	public EmptyContentView mEmptyListView;
	@Override
	public CursorLoader createCursorLoader(Context context) {
		Log.d(TAG, "createCursorLoader12");
		return new ProfileAndContactsLoader(context);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
	}

	private TextView mOwnerInfoDiaplayNameTextView;
	private TextView mOwnerInfoNumberTextView;
	private ImageView mOwnerInfoPhotoImageView;
	private View ownerHeader;
	@Override
	protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
		super.onCreateView(inflater, container);

		if(!com.android.contacts.common.HbUtils.isMTK){
			ownerHeader = inflater.inflate(R.layout.hb_ownerinfo_header, null, false);
			mOwnerInfoDiaplayNameTextView=(TextView) ownerHeader.findViewById(R.id.ownerinfo_name);
			mOwnerInfoNumberTextView=(TextView) ownerHeader.findViewById(R.id.ownerinfo_number);
			mOwnerInfoPhotoImageView=(ImageView) ownerHeader.findViewById(R.id.ownerinfo_photo);
			
			getListView().addHeaderView(ownerHeader, null, false);
		}
	}
	@Override
	protected ContactEntryListAdapter createListAdapter() {
		final DefaultContactListAdapter adapter = new DefaultContactListAdapter(getActivity());
		adapter.setDisplayPhotos(false);
		adapter.setFilter(ContactListFilter.createFilterWithType(
				ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS));
		adapter.setSectionHeaderDisplayEnabled(isSectionHeaderDisplayEnabled());
		adapter.isForContactListMain=true;
		return adapter;
	}

	@Override
	protected View inflateView(LayoutInflater inflater, ViewGroup container) {
		return inflater.inflate(R.layout.all_contacts_fragment, null);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Log.d(TAG, "onitemclick,view:"+view+" view.getTag():"+view.getTag()+" position:"+position);
		final Uri uri=getAdapter().getContactUri(com.android.contacts.common.HbUtils.isMTK?position:position-1);
		Log.d(TAG, "onitemclick uri:"+uri);
		if (uri != null) {
			QuickContact.showQuickContact(getContext(), view, uri, null,
					Phone.CONTENT_ITEM_TYPE);
		}
	}

	@Override
	protected void onItemClick(int position, long id) {
		// Do nothing. Implemented to satisfy ContactEntryListFragment.
	}

	private BottomWidePopupMenu bottomWidePopupMenu1;
	@Override
	protected boolean onItemLongClick(int position, long id) {
		Log.d(TAG,"onitemlongclick,position:"+position);
		if(com.android.contacts.common.HbUtils.isMTK) return false;
		if(bottomWidePopupMenu1==null) {
			bottomWidePopupMenu1 = new BottomWidePopupMenu(getActivity());
			bottomWidePopupMenu1.inflateMenu(R.menu.hb_contact_bottom_menu);		

			bottomWidePopupMenu1.setOnMenuItemClickedListener(new BottomWidePopupMenu.OnMenuItemClickListener() {
				@Override
				public boolean onItemClicked(MenuItem item) {
					// TODO Auto-generated method stub
					Log.d(TAG,"onItemClicked Item:"+item.getTitle());
					switch(item.getItemId()){
					case R.id.hb_contacts_delete:
						//						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						//						builder.setMessage(getActivity().getString(R.string.hb_delete_call_log_message,1));
						//						builder.setTitle(null);
						//						builder.setNegativeButton(getActivity().getString(R.string.hb_cancel), null);
						//						builder.setPositiveButton(getActivity().getString(R.string.hb_ok), new DialogInterface.OnClickListener() {
						//							public void onClick(DialogInterface dialog, int which)
						//							{
						//								dialog.dismiss();
						//
						//								long[] ids=mAdapter.getcallIds(position);
						//								final StringBuilder callIds = new StringBuilder();
						//								for(long id:ids){
						//									if (callIds.length() != 0) {
						//										callIds.append(",");
						//									}
						//									callIds.append(id);
						//								}
						//
						//								Log.d(TAG,"callIds:"+callIds);
						//
						//								CallLogAsyncTaskUtil.deleteCalls(
						//										getContext(), callIds.toString(), mCallLogAsyncTaskListener,mIsPrivate);
						//							}
						//						});
						//						AlertDialog alertDialog = builder.create();
						//						alertDialog.show();
						break;

					default:
						break;
					}
					return true;
				}
			});
		}

		bottomWidePopupMenu1.show();
		return true;
	}

	@Override
	public void onEmptyViewActionButtonClicked(int id) {
		Log.d(TAG,"onEmptyViewActionButtonClicked");
		if (getActivity() == null) {
			return;
		}

		if (!PermissionsUtil.hasPermission(getActivity(), READ_CONTACTS)) {
			requestPermissions(new String[] {READ_CONTACTS}, READ_CONTACTS_PERMISSION_REQUEST_CODE);
		} 
		//		else {
		//			// Add new contact
		//			DialerUtils.startActivityWithErrorToast(getActivity(), IntentUtil.getNewContactIntent(),
		//					R.string.add_contact_not_available);
		//		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions,
			int[] grantResults) {
		if (requestCode == READ_CONTACTS_PERMISSION_REQUEST_CODE) {
			if (grantResults.length >= 1 && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
				// Force a refresh of the data since we were missing the permission before this.
				reloadData();
			}
		}
	}
}
