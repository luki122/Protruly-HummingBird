/*
 * Copyright (C) 2014 MediaTek Inc.
 * Modification based on code covered by the mentioned copyright
 * and/or permission notice(s).
 */
/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.contacts.common.list;

import com.google.common.collect.Lists;
import com.android.contacts.common.hb.FragmentCallbacks;
import com.android.contacts.common.hb.DialerSearchHelperForHb.DialerSearchResultColumnsForHb;
import android.content.Context;
import android.content.CursorLoader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import hb.provider.ContactsContract;
import hb.provider.ContactsContract.CommonDataKinds.Phone;
import hb.provider.ContactsContract.Contacts;
import hb.provider.ContactsContract.Data;
import hb.provider.ContactsContract.Directory;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.style.ForegroundColorSpan;
import android.text.style.SuperscriptSpan;
import android.view.Gravity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.SectionIndexer;
import android.widget.TextView;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.ContactPhotoManager.DefaultImageRequest;
import com.android.contacts.common.format.TextHighlighter;
import com.android.contacts.common.R;
import com.android.contacts.common.list.ContactEntryListAdapter.HighlightSequence;
import com.android.contacts.common.list.ContactEntryListFragment.SortCursor;
import com.android.contacts.common.list.ContactListAdapter.ContactQuery;
import com.android.contacts.common.list.IndexerListAdapter.Placement;
import com.android.contacts.common.util.ContactDisplayUtils;
import com.android.contacts.common.util.SearchUtil;
import com.mediatek.contacts.util.ContactsCommonListUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * Common base class for various contact-related lists, e.g. contact list, phone number list
 * etc.
 */
public abstract class ContactEntryListAdapter extends IndexerListAdapter {

	private static final String TAG = "ContactEntryListAdapter";

	/**
	 * Indicates whether the {@link Directory#LOCAL_INVISIBLE} directory should
	 * be included in the search.
	 */
	public static final boolean LOCAL_INVISIBLE_DIRECTORY_ENABLED = false;
	public static final String SEARCH_BEGIN_STRING="hb_querystring_for_contact_search_begin";

	private int mDisplayOrder;
	private int mSortOrder;

	private boolean mDisplayPhotos;
	private boolean mCircularPhotos = true;
	private boolean mQuickContactEnabled;
	private boolean mAdjustSelectionBoundsEnabled;
	protected String hbFilterString;	
	public void setHbFilterString(String hbFilterString) {
		this.hbFilterString = hbFilterString;
	}
	/**
	 * indicates if contact queries include profile
	 */
	private boolean mIncludeProfile;

	/**
	 * indicates if query results includes a profile
	 */
	private boolean mProfileExists;
	private boolean isForDialerSearch=false;	

	public boolean isForDialerSearch() {
		return isForDialerSearch;
	}
	public void setForDialerSearch(boolean isForDialerSearch) {
		this.isForDialerSearch = isForDialerSearch;
	}

	private boolean isForContactsChoice=false;


	public boolean isForContactsChoice() {
		return isForContactsChoice;
	}
	public void setForContactsChoice(boolean isForContactsChoice) {
		this.isForContactsChoice = isForContactsChoice;
	}

	protected View.OnLongClickListener mLongClickListener;
	protected View.OnClickListener mClickListener;

	//	protected SliderView currentSliderView=null;

	//	public SliderView getCurrentSliderView() {
	//		return currentSliderView;
	//	}
	//	public void setCurrentSliderView(SliderView currentSliderView) {
	//		this.currentSliderView = currentSliderView;
	//	}
	public View.OnLongClickListener getLongClickListener() {
		return mLongClickListener;
	}
	public void setLongClickListener(View.OnLongClickListener mLongClickListener) {
		Log.d(TAG,"setLongClickListener:"+mLongClickListener);
		this.mLongClickListener = mLongClickListener;
	}
	public View.OnClickListener getClickListener() {
		return mClickListener;
	}
	public void setClickListener(View.OnClickListener mClickListener) {
		Log.d(TAG,"setClickListener:"+mClickListener);
		this.mClickListener = mClickListener;
	}

	/**
	 * The root view of the fragment that this adapter is associated with.
	 */
	private View mFragmentRootView;

	private ContactPhotoManager mPhotoLoader;

	private String mQueryString;
	private String mUpperCaseQueryString;
	private boolean mSearchMode;
	private int mDirectorySearchMode;
	private int mDirectoryResultLimit = Integer.MAX_VALUE;

	private boolean mEmptyListEnabled = true;

	private boolean mSelectionVisible;
	private boolean mSelectMode;

	protected ContactListFilter mFilter;
	private boolean mDarkTheme = false;

	/** Resource used to provide header-text for default filter. */
	private CharSequence mDefaultFilterHeaderText;

	private boolean mIsNeedStarredShow=true;
	protected FragmentCallbacks mCallbacks;

	public void setmCallbacks(FragmentCallbacks mCallbacks) {
		this.mCallbacks = mCallbacks;
	}

	public ContactEntryListAdapter(Context context) {
		super(context);
		setDefaultFilterHeaderText(R.string.local_search_label);
		addPartitions();
		mTextHighlighter = new TextHighlighter(/*Typeface.BOLD*/
				new ForegroundColorSpan(Color.parseColor(getContext().getResources().getString(R.string.hb_highlight_color_string))));
		mNameHighlightSequence = new ArrayList<HighlightSequence>();
		mNumberHighlightSequence = new ArrayList<HighlightSequence>();
		mUnknownNameText = context.getText(R.string.missing_name);
		mSelectedContactIds = new TreeSet<Long>();
	}

	/**
	 * @param fragmentRootView Root view of the fragment. This is used to restrict the scope of
	 * image loading requests that get cancelled on cursor changes.
	 */
	protected void setFragmentRootView(View fragmentRootView) {
		mFragmentRootView = fragmentRootView;
	}

	protected void setDefaultFilterHeaderText(int resourceId) {
		mDefaultFilterHeaderText = getContext().getResources().getText(resourceId);
	}
	public int getRawContactID(int position) {
		Cursor cursor =  (Cursor)getItem(position);
		if (position <= cursor.getCount()) {
			if (cursor.moveToPosition(position)) {
				return cursor.getInt(16);
			}
		}		
		return 0;
	}

	public int getContactID(int position) {
		Cursor cursor =  (Cursor)getItem(position);
		if (position <= cursor.getCount()) {
			if (cursor.moveToPosition(position)) {
				return cursor.getInt(0);
			}
		}		
		return 0;
	}

	public String getName(int position) {
		Cursor cursor =  (Cursor)getItem(position);
		if (position <= cursor.getCount()) {
			if (cursor.moveToPosition(position)) {
				return cursor.getString(1);
			}
		}		
		return "";
	}
	/// M: For Dialer customization
	@Override
	protected View newView(
			Context context, int partition, Cursor cursor, int position, ViewGroup parent) {
		final ContactListItemView view = new ContactListItemView(context, null);
		view.setIsSectionHeaderEnabled(isSectionHeaderDisplayEnabled());
		view.setAdjustSelectionBoundsEnabled(isAdjustSelectionBoundsEnabled());
		return view;
	}

	@Override
	protected void bindView(View itemView, int partition, Cursor cursor, int position) {
		//		final ContactListItemView view = (ContactListItemView) itemView;
		//		view.setIsSectionHeaderEnabled(isSectionHeaderDisplayEnabled());
	}

	@Override
	protected View createPinnedSectionHeaderView(Context context, ViewGroup parent) {
		return new ContactListPinnedHeaderView(context, null, parent);
	}

	@Override
	protected void setPinnedSectionTitle(View pinnedHeaderView, String title) {
		((ContactListPinnedHeaderView) pinnedHeaderView).setSectionHeaderTitle(title);
	}

	protected void addPartitions() {
		addPartition(createDefaultDirectoryPartition());
	}

	protected DirectoryPartition createDefaultDirectoryPartition() {
		DirectoryPartition partition = new DirectoryPartition(true, true);
		partition.setDirectoryId(Directory.DEFAULT);
		partition.setDirectoryType(getContext().getString(R.string.contactsList));
		partition.setPriorityDirectory(true);
		partition.setPhotoSupported(true);
		partition.setLabel(mDefaultFilterHeaderText.toString());
		return partition;
	}

	/**
	 * Remove all directories after the default directory. This is typically used when contacts
	 * list screens are asked to exit the search mode and thus need to remove all remote directory
	 * results for the search.
	 *
	 * This code assumes that the default directory and directories before that should not be
	 * deleted (e.g. Join screen has "suggested contacts" directory before the default director,
	 * and we should not remove the directory).
	 */
	public void removeDirectoriesAfterDefault() {
		final int partitionCount = getPartitionCount();
		for (int i = partitionCount - 1; i >= 0; i--) {
			final Partition partition = getPartition(i);
			if ((partition instanceof DirectoryPartition)
					&& ((DirectoryPartition) partition).getDirectoryId() == Directory.DEFAULT) {
				break;
			} else {
				removePartition(i);
			}
		}
	}

	protected int getPartitionByDirectoryId(long id) {
		int count = getPartitionCount();
		for (int i = 0; i < count; i++) {
			Partition partition = getPartition(i);
			if (partition instanceof DirectoryPartition) {
				if (((DirectoryPartition)partition).getDirectoryId() == id) {
					return i;
				}
			}
		}
		return -1;
	}

	protected DirectoryPartition getDirectoryById(long id) {
		int count = getPartitionCount();
		for (int i = 0; i < count; i++) {
			Partition partition = getPartition(i);
			if (partition instanceof DirectoryPartition) {
				final DirectoryPartition directoryPartition = (DirectoryPartition) partition;
				if (directoryPartition.getDirectoryId() == id) {
					return directoryPartition;
				}
			}
		}
		return null;
	}

	public abstract String getContactDisplayName(int position);
	public abstract void configureLoader(CursorLoader loader, long directoryId);

	/**
	 * Marks all partitions as "loading"
	 */
	public void onDataReload() {
		boolean notify = false;
		int count = getPartitionCount();
		for (int i = 0; i < count; i++) {
			Partition partition = getPartition(i);
			if (partition instanceof DirectoryPartition) {
				DirectoryPartition directoryPartition = (DirectoryPartition)partition;
				if (!directoryPartition.isLoading()) {
					notify = true;
				}
				directoryPartition.setStatus(DirectoryPartition.STATUS_NOT_LOADED);
			}
		}
		Log.d(TAG,"onDataReload,notify:"+notify);
		if (notify) {
			//			notifyDataSetChanged();//liyang modify
		}
	}

	@Override
	public void clearPartitions() {
		int count = getPartitionCount();
		for (int i = 0; i < count; i++) {
			Partition partition = getPartition(i);
			if (partition instanceof DirectoryPartition) {
				DirectoryPartition directoryPartition = (DirectoryPartition)partition;
				directoryPartition.setStatus(DirectoryPartition.STATUS_NOT_LOADED);
			}
		}
		super.clearPartitions();
	}

	public boolean isSearchMode() {
		return mSearchMode;
	}

	public void setSearchMode(boolean flag) {
		mSearchMode = flag;
	}

	public String getQueryString() {
		return mQueryString;
	}

	private boolean isAllDigit=true;
	public boolean isAllDigit() {
		return isAllDigit;
	}

	public void setAllDigit(boolean isAllDigit) {
		this.isAllDigit = isAllDigit;
	}

	public void setQueryString(String queryString) {
		Log.d(TAG,"setQueryString:"+queryString);
		//		mQueryString = queryString;
		if (TextUtils.isEmpty(queryString)) {
			mUpperCaseQueryString = null;
		} 
		else {
//			mUpperCaseQueryString = SearchUtil
//					.cleanStartAndEndOfSearchQuery(queryString.toUpperCase()) ;
			mUpperCaseQueryString=queryString.toUpperCase();
		}
		mQueryString = queryString;
	}

	public String getUpperCaseQueryString() {
		return mUpperCaseQueryString;
	}

	public int getDirectorySearchMode() {
		return mDirectorySearchMode;
	}

	public void setDirectorySearchMode(int mode) {
		mDirectorySearchMode = mode;
	}

	public int getDirectoryResultLimit() {
		return mDirectoryResultLimit;
	}

	public int getDirectoryResultLimit(DirectoryPartition directoryPartition) {
		final int limit = directoryPartition.getResultLimit();
		return limit == DirectoryPartition.RESULT_LIMIT_DEFAULT ? mDirectoryResultLimit : limit;
	}

	public void setDirectoryResultLimit(int limit) {
		this.mDirectoryResultLimit = limit;
	}

	public int getContactNameDisplayOrder() {
		return mDisplayOrder;
	}

	public void setContactNameDisplayOrder(int displayOrder) {
		mDisplayOrder = displayOrder;
	}

	public int getSortOrder() {
		return mSortOrder;
	}

	public void setSortOrder(int sortOrder) {
		mSortOrder = sortOrder;
	}

	public void setPhotoLoader(ContactPhotoManager photoLoader) {
		mPhotoLoader = photoLoader;
	}

	protected ContactPhotoManager getPhotoLoader() {
		return mPhotoLoader;
	}

	public boolean getDisplayPhotos() {
		return mDisplayPhotos;
	}

	public void setDisplayPhotos(boolean displayPhotos) {
		mDisplayPhotos = displayPhotos;
	}

	public boolean getCircularPhotos() {
		return mCircularPhotos;
	}

	public void setCircularPhotos(boolean circularPhotos) {
		mCircularPhotos = circularPhotos;
	}

	public boolean isEmptyListEnabled() {
		return mEmptyListEnabled;
	}

	public void setEmptyListEnabled(boolean flag) {
		mEmptyListEnabled = flag;
	}

	public boolean isSelectionVisible() {
		return mSelectionVisible;
	}

	public void setSelectionVisible(boolean flag) {
		this.mSelectionVisible = flag;
	}



	public boolean isSelectMode() {
		return mSelectMode;
	}

	public void setSelectMode(boolean mSelectMode) {
		this.mSelectMode = mSelectMode;
	}

	public boolean isQuickContactEnabled() {
		return mQuickContactEnabled;
	}

	public void setQuickContactEnabled(boolean quickContactEnabled) {
		mQuickContactEnabled = quickContactEnabled;
	}

	public boolean isAdjustSelectionBoundsEnabled() {
		return mAdjustSelectionBoundsEnabled;
	}

	public void setAdjustSelectionBoundsEnabled(boolean enabled) {
		mAdjustSelectionBoundsEnabled = enabled;
	}

	public boolean shouldIncludeProfile() {
		return mIncludeProfile;
	}

	public void setIncludeProfile(boolean includeProfile) {
		mIncludeProfile = includeProfile;
	}

	public void setProfileExists(boolean exists) {
		mProfileExists = exists;
		// Stick the "ME" header for the profile
		if (exists) {
			SectionIndexer indexer = getIndexer();
			if (indexer != null) {
				((ContactsSectionIndexer) indexer).setProfileHeader(
						getContext().getString(R.string.user_profile_contacts_list_header));
			}
		}
	}

	public boolean hasProfile() {
		return mProfileExists;
	}

	public void setDarkTheme(boolean value) {
		mDarkTheme = value;
	}

	/**
	 * Updates partitions according to the directory meta-data contained in the supplied
	 * cursor.
	 */
	public void changeDirectories(Cursor cursor) {
		if (cursor.getCount() == 0) {
			// Directory table must have at least local directory, without which this adapter will
			// enter very weird state.
			Log.e(TAG, "Directory search loader returned an empty cursor, which implies we have " +
					"no directory entries.", new RuntimeException());
			return;
		}
		HashSet<Long> directoryIds = new HashSet<Long>();

		int idColumnIndex = cursor.getColumnIndex(Directory._ID);
		int directoryTypeColumnIndex = cursor.getColumnIndex(DirectoryListLoader.DIRECTORY_TYPE);
		int displayNameColumnIndex = cursor.getColumnIndex(Directory.DISPLAY_NAME);
		int photoSupportColumnIndex = cursor.getColumnIndex(Directory.PHOTO_SUPPORT);

		// TODO preserve the order of partition to match those of the cursor
		// Phase I: add new directories
		cursor.moveToPosition(-1);
		while (cursor.moveToNext()) {
			long id = cursor.getLong(idColumnIndex);
			directoryIds.add(id);
			if (getPartitionByDirectoryId(id) == -1) {
				DirectoryPartition partition = new DirectoryPartition(false, true);
				partition.setDirectoryId(id);
				if (isRemoteDirectory(id)) {
					partition.setLabel(mContext.getString(R.string.directory_search_label));
				} else {
					partition.setLabel(mDefaultFilterHeaderText.toString());
				}
				partition.setDirectoryType(cursor.getString(directoryTypeColumnIndex));
				partition.setDisplayName(cursor.getString(displayNameColumnIndex));
				int photoSupport = cursor.getInt(photoSupportColumnIndex);
				partition.setPhotoSupported(photoSupport == Directory.PHOTO_SUPPORT_THUMBNAIL_ONLY
						|| photoSupport == Directory.PHOTO_SUPPORT_FULL);
				addPartition(partition);
			}
		}

		// Phase II: remove deleted directories
		int count = getPartitionCount();
		for (int i = count; --i >= 0; ) {
			Partition partition = getPartition(i);
			if (partition instanceof DirectoryPartition) {
				long id = ((DirectoryPartition)partition).getDirectoryId();
				if (!directoryIds.contains(id)) {
					removePartition(i);
				}
			}
		}

		invalidate();
		notifyDataSetChanged();
	}

	protected int resultCount=0;
	//	protected int preContactId=0;
	@Override
	public void changeCursor(int partitionIndex, Cursor cursor) {
		Log.d(TAG,"cursor:"+cursor);
		if(cursor==null) {
			resultCount=0;
			return;
		}
		else resultCount=cursor.getCount();
		Log.d(TAG,"changeCursor,partitionIndex:"+partitionIndex+" resultCount:"+resultCount);
		if (partitionIndex >= getPartitionCount()) {
			// There is no partition for this data
			return;
		}

		Partition partition = getPartition(partitionIndex);
		if (partition instanceof DirectoryPartition) {
			Log.d(TAG,"changeCursor,partition:"+partition);
			((DirectoryPartition)partition).setStatus(DirectoryPartition.STATUS_LOADED);
		}

		//		if (mDisplayPhotos && mPhotoLoader != null && isPhotoSupported(partitionIndex)) {
		//			mPhotoLoader.refreshCache();
		//		}

		//		preContactId=0;
		super.changeCursor(partitionIndex, cursor);

		if (isSectionHeaderDisplayEnabled() && partitionIndex == getIndexedPartition()) {
			updateIndexer(cursor);
		}

		// When the cursor changes, cancel any pending asynchronous photo loads.
		//		mPhotoLoader.cancelPendingRequests(mFragmentRootView);
	}

	public ProfileAndContactsLoader mSDNLoader = null;

	public void changeCursor(Cursor cursor) {
		changeCursor(0, cursor);
	}

	private int mStarredCount = 0;

	public void setStarredCount(int count) {
		mStarredCount = count;
	}

	public int getStarredCount() {
		return mStarredCount;
	}

	private int mCommonlyCount = 0;
	private int groupCount=0;
	private int isQueryCommon=0;

	public int getIsQueryCommon() {
		return isQueryCommon;
	}

	public void setIsQueryCommon(int isQueryCommon) {
		this.isQueryCommon = isQueryCommon;
	}

	public int getGroupCount() {
		return groupCount;
	}

	public void setGroupCount(int groupCount) {
		this.groupCount = groupCount;
	}

	public void setCommonlyCount(int count) {
		mCommonlyCount = count;
	}

	public int getCommonlyCount(){
		return mCommonlyCount;
	}

	/**
	 * Updates the indexer, which is used to produce section headers.
	 */
	public void updateIndexer(Cursor cursor) {
		Log.d(TAG,"updateIndexer:"+cursor);
		if (cursor == null) {
			setIndexer(null);
			return;
		}

		Bundle bundle = cursor.getExtras();
		if (bundle.containsKey(Contacts.EXTRA_ADDRESS_BOOK_INDEX_TITLES) &&
				bundle.containsKey(Contacts.EXTRA_ADDRESS_BOOK_INDEX_COUNTS)) {
			String sections[] =
					bundle.getStringArray(Contacts.EXTRA_ADDRESS_BOOK_INDEX_TITLES);
			int counts[] = bundle.getIntArray(
					Contacts.EXTRA_ADDRESS_BOOK_INDEX_COUNTS);
			Log.d(TAG,"sections:"+Arrays.toString(sections)+" counts:"+Arrays.toString(counts));

			if (getExtraStartingSection()) {
				// Insert an additional unnamed section at the top of the list.
				String allSections[] = new String[sections.length + 1];
				int allCounts[] = new int[counts.length + 1];
				for (int i = 0; i < sections.length; i++) {
					allSections[i + 1] = sections[i];
					allCounts[i + 1] = counts[i];
				}
				allCounts[0] = 1;
				allSections[0] = "";
				setIndexer(new ContactsSectionIndexer(allSections, allCounts));
			} else {
				String[] mSections=sections;
				int[] mCounts=counts;
				String[] resultSections=null;
				int[] resultCounts=null;
				if(sections!=null&&counts!=null&&sections.length>0&&counts.length>0){
					if(TextUtils.isEmpty(sections[0])){
						if(TextUtils.equals("#", sections[sections.length-1])&&sections.length>1&&counts.length>1){
							resultSections=new String[sections.length-1];
							resultCounts=new int[counts.length-1];
							System.arraycopy(sections, 1, resultSections, 0, resultSections.length);
							System.arraycopy(counts, 1, resultCounts, 0, resultCounts.length);
							resultCounts[resultCounts.length-1]+=counts[0];
							Log.d(TAG,"resultSections1:"+Arrays.toString(resultSections)+" resultCounts1:"+Arrays.toString(resultCounts));
						}else if(!TextUtils.equals("#", sections[sections.length-1])&&sections.length>1&&counts.length>1){
							resultSections=new String[sections.length];
							resultCounts=new int[counts.length];
							System.arraycopy(sections, 1, resultSections, 0, resultSections.length-1);
							System.arraycopy(counts, 1, resultCounts, 0, resultCounts.length-1);
							resultSections[resultSections.length-1]="#";
							resultCounts[resultCounts.length-1]=counts[0];
							Log.d(TAG,"resultSections2:"+Arrays.toString(resultSections)+" resultCounts2:"+Arrays.toString(resultCounts));
						}else if(sections.length==1&&counts.length==1){
							resultSections=new String[1];
							resultCounts=new int[1];
							resultSections[0]="#";
							resultCounts[0]=counts[0];
							Log.d(TAG,"resultSections3:"+Arrays.toString(resultSections)+" resultCounts3:"+Arrays.toString(resultCounts));
						}
					}else if(TextUtils.isEmpty(sections[sections.length-1])){
						sections[sections.length-1]="#";
					}

					mStarredCount=mSDNLoader==null?0:mSDNLoader.getCursorCount();
					Log.d(TAG, "mStarredCount:"+mStarredCount);


					if(resultSections!=null){
						mSections=resultSections;
						mCounts=resultCounts;
					}
					if (mIsNeedStarredShow) {
						if (mStarredCount > 0) {
							String newSections[] = new String[mSections.length + 1];
							int newCounts[] = new int[mCounts.length + 1];
							newSections[0] = mContext.getString(R.string.hb_starred_contacts_header);
							newCounts[0] = mStarredCount;

							for (int i = 0; i < mSections.length; i++) {
								newSections[i + 1] = mSections[i];
							}

							for (int i = 0; i < mCounts.length; i++) {
								newCounts[i + 1] = mCounts[i];
							}
							Log.d(TAG,"newSections:"+Arrays.toString(newSections)+" newCounts:"+Arrays.toString(newCounts));
							setIndexer(new ContactsSectionIndexer(newSections, newCounts));
							return;
						}
					}

				}

				Log.d(TAG,"sections:"+Arrays.toString(mSections)+" counts:"+Arrays.toString(mCounts));
				setIndexer(new ContactsSectionIndexer(mSections, mCounts));
			}
		} else {
			setIndexer(null);
		}
	}

	protected boolean getExtraStartingSection() {
		return false;
	}

	@Override
	public int getViewTypeCount() {
		// We need a separate view type for each item type, plus another one for
		// each type with header, plus one for "other".
		return getItemViewTypeCount() * 2 + 1;
	}

	@Override
	public int getItemViewType(int partitionIndex, int position) {	
		int type = super.getItemViewType(partitionIndex, position);
		if (!isUserProfile(position)
				&& isSectionHeaderDisplayEnabled()
				&& partitionIndex == getIndexedPartition()) {
			Placement placement = getItemPlacementInSection(position);
			return placement.firstInSection ? type : getItemViewTypeCount() + type;
		} else {
			return type;
		}
	}

	@Override
	public boolean isEmpty() {
		// TODO
		//        if (contactsListActivity.mProviderStatus != ProviderStatus.STATUS_NORMAL) {
		//            return true;
		//        }

		if (!mEmptyListEnabled) {
			return false;
		} else if (isSearchMode()) {
			return TextUtils.isEmpty(getQueryString());
		} else {
			return super.isEmpty();
		}
	}

	public boolean isLoading() {
		int count = getPartitionCount();
		for (int i = 0; i < count; i++) {
			Partition partition = getPartition(i);
			if (partition instanceof DirectoryPartition
					&& ((DirectoryPartition) partition).isLoading()) {
				return true;
			}
		}
		return false;
	}

	public boolean areAllPartitionsEmpty() {
		int count = getPartitionCount();
		for (int i = 0; i < count; i++) {
			if (!isPartitionEmpty(i)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Changes visibility parameters for the default directory partition.
	 */
	public void configureDefaultPartition(boolean showIfEmpty, boolean hasHeader) {
		int defaultPartitionIndex = -1;
		int count = getPartitionCount();
		for (int i = 0; i < count; i++) {
			Partition partition = getPartition(i);
			if (partition instanceof DirectoryPartition &&
					((DirectoryPartition)partition).getDirectoryId() == Directory.DEFAULT) {
				defaultPartitionIndex = i;
				break;
			}
		}
		if (defaultPartitionIndex != -1) {
			setShowIfEmpty(defaultPartitionIndex, showIfEmpty);
			setHasHeader(defaultPartitionIndex, /*hasHeader* liyang modify*/ false);
		}
	}

	@Override
	protected View newHeaderView(Context context, int partition, Cursor cursor,
			ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.directory_header, parent, false);
		//		if (!getPinnedPartitionHeadersEnabled()) {
		//			// If the headers are unpinned, there is no need for their background
		//			// color to be non-transparent. Setting this transparent reduces maintenance for
		//			// non-pinned headers. We don't need to bother synchronizing the activity's
		//			// background color with the header background color.
		//			view.setBackground(null);
		//		}
		return view;
	}

	@Override
	protected void bindHeaderView(View view, int partitionIndex, Cursor cursor) {
		Partition partition = getPartition(partitionIndex);
		if (!(partition instanceof DirectoryPartition)) {
			return;
		}

		DirectoryPartition directoryPartition = (DirectoryPartition)partition;
		long directoryId = directoryPartition.getDirectoryId();
		TextView labelTextView = (TextView)view.findViewById(R.id.label);
		TextView displayNameTextView = (TextView)view.findViewById(R.id.display_name);
		labelTextView.setText(directoryPartition.getLabel());
		if (!isRemoteDirectory(directoryId)) {
			displayNameTextView.setText(null);
		} else {
			String directoryName = directoryPartition.getDisplayName();
			String displayName = !TextUtils.isEmpty(directoryName)
					? directoryName
							: directoryPartition.getDirectoryType();
			displayNameTextView.setText(displayName);
		}

		final Resources res = getContext().getResources();
		final int headerPaddingTop = partitionIndex == 1 && getPartition(0).isEmpty() ?
				0 : res.getDimensionPixelOffset(R.dimen.directory_header_extra_top_padding);
		// There should be no extra padding at the top of the first directory header
		view.setPaddingRelative(view.getPaddingStart(), headerPaddingTop, view.getPaddingEnd(),
				view.getPaddingBottom());
	}

	// Default implementation simply returns number of rows in the cursor.
	// Broken out into its own routine so can be overridden by child classes
	// for eg number of unique contacts for a phone list.
	protected int getResultCount(Cursor cursor) {
		return cursor == null ? 0 : cursor.getCount();
	}

	/**
	 * Checks whether the contact entry at the given position represents the user's profile.
	 */
	public boolean isUserProfile(int position) {
		// The profile only ever appears in the first position if it is present.  So if the position
		// is anything beyond 0, it can't be the profile.
		boolean isUserProfile = false;
		if (position == 0) {
			int partition = getPartitionForPosition(position);
			if (partition >= 0) {
				// Save the old cursor position - the call to getItem() may modify the cursor
				// position.
				int offset = getCursor(partition).getPosition();
				Cursor cursor = (Cursor) getItem(position);
				if (cursor != null) {
					int profileColumnIndex = cursor.getColumnIndex(Contacts.IS_USER_PROFILE);
					if (profileColumnIndex != -1) {
						isUserProfile = cursor.getInt(profileColumnIndex) == 1;
					}
					// Restore the old cursor position.
					cursor.moveToPosition(offset);
				}
			}
		}
		return isUserProfile;
	}

	// TODO: fix PluralRules to handle zero correctly and use Resources.getQuantityText directly
	public String getQuantityText(int count, int zeroResourceId, int pluralResourceId) {
		if (count == 0) {
			return getContext().getString(zeroResourceId);
		} else {
			String format = getContext().getResources()
					.getQuantityText(pluralResourceId, count).toString();
			return String.format(format, count);
		}
	}

	public boolean isPhotoSupported(int partitionIndex) {
		Partition partition = getPartition(partitionIndex);
		if (partition instanceof DirectoryPartition) {
			return ((DirectoryPartition) partition).isPhotoSupported();
		}
		return true;
	}

	/**
	 * Returns the currently selected filter.
	 */
	public ContactListFilter getFilter() {
		return mFilter;
	}

	public void setFilter(ContactListFilter filter) {
		mFilter = filter;
	}

	// TODO: move sharable logic (bindXX() methods) to here with extra arguments

	/**
	 * Loads the photo for the quick contact view and assigns the contact uri.
	 * @param photoIdColumn Index of the photo id column
	 * @param photoUriColumn Index of the photo uri column. Optional: Can be -1
	 * @param contactIdColumn Index of the contact id column
	 * @param lookUpKeyColumn Index of the lookup key column
	 * @param displayNameColumn Index of the display name column
	 */
	protected void bindQuickContact(final ContactListItemView view, int partitionIndex,
			Cursor cursor, int photoIdColumn, int photoUriColumn, int contactIdColumn,
			int lookUpKeyColumn, int displayNameColumn) {
		long photoId = 0;
		if (!cursor.isNull(photoIdColumn)) {
			photoId = cursor.getLong(photoIdColumn);
		}

		QuickContactBadge quickContact = view.getQuickContact();
		quickContact.assignContactUri(
				getContactUri(partitionIndex, cursor, contactIdColumn, lookUpKeyColumn));
		// The Contacts app never uses the QuickContactBadge. Therefore, it is safe to assume
		// that only Dialer will use this QuickContact badge. This means prioritizing the phone
		// mimetype here is reasonable.
		quickContact.setPrioritizedMimeType(Phone.CONTENT_ITEM_TYPE);

		if (photoId != 0 || photoUriColumn == -1) {
			getPhotoLoader().loadThumbnail(quickContact, photoId, mDarkTheme, mCircularPhotos,
					null);
		} else {
			final String photoUriString = cursor.getString(photoUriColumn);
			final Uri photoUri = photoUriString == null ? null : Uri.parse(photoUriString);
			DefaultImageRequest request = null;
			if (photoUri == null) {
				request = getDefaultImageRequestFromCursor(cursor, displayNameColumn,
						lookUpKeyColumn);
			}
			getPhotoLoader().loadPhoto(quickContact, photoUri, -1, mDarkTheme, mCircularPhotos,
					request);
		}

	}

	@Override
	public boolean hasStableIds() {
		// Whenever bindViewId() is called, the values passed into setId() are stable or
		// stable-ish. For example, when one contact is modified we don't expect a second
		// contact's Contact._ID values to change.
		return true;
	}

	protected void bindViewId(final ContactListItemView view, Cursor cursor, int idColumn) {
		// Set a semi-stable id, so that talkback won't get confused when the list gets
		// refreshed. There is little harm in inserting the same ID twice.
		long contactId = cursor.getLong(idColumn);
		view.setId((int) (contactId % Integer.MAX_VALUE));

	}

	protected Uri getContactUri(int partitionIndex, Cursor cursor,
			int contactIdColumn, int lookUpKeyColumn) {
		long contactId = cursor.getLong(contactIdColumn);
		String lookupKey = cursor.getString(lookUpKeyColumn);
		long directoryId = ((DirectoryPartition)getPartition(partitionIndex)).getDirectoryId();
		Uri uri = Contacts.getLookupUri(contactId, lookupKey);
		if (uri != null && directoryId != Directory.DEFAULT) {
			uri = uri.buildUpon().appendQueryParameter(
					ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(directoryId)).build();
		}
		return uri;
	}

	public static boolean isRemoteDirectory(long directoryId) {
		return directoryId != Directory.DEFAULT
				&& directoryId != Directory.LOCAL_INVISIBLE;
	}

	/**
	 * Retrieves the lookup key and display name from a cursor, and returns a
	 * {@link DefaultImageRequest} containing these contact details
	 *
	 * @param cursor Contacts cursor positioned at the current row to retrieve contact details for
	 * @param displayNameColumn Column index of the display name
	 * @param lookupKeyColumn Column index of the lookup key
	 * @return {@link DefaultImageRequest} with the displayName and identifier fields set to the
	 * display name and lookup key of the contact.
	 */
	public DefaultImageRequest getDefaultImageRequestFromCursor(Cursor cursor,
			int displayNameColumn, int lookupKeyColumn) {
		final String displayName = cursor.getString(displayNameColumn);
		final String lookupKey = cursor.getString(lookupKeyColumn);
		// / M: Because of sim contacts, ImageRequest will need to pass subId
		// also, instead of just new DefaultImageRequest @{
		//return new DefaultImageRequest(displayName, lookupKey, mCircularPhotos);
		return ContactsCommonListUtils.getDefaultImageRequest(cursor, displayName, lookupKey,
				mCircularPhotos);
		// / @}
	}

	/** M: Don't show sdn number when multiple delete. @{ */
	public boolean mShowSdnNumber = true;

	public void setShowSdnNumber(boolean canDelete) {
		mShowSdnNumber = canDelete;
	}

	/**
	 * Checks whether the contact entry at the given position represents the SDN number.
	 */
	public boolean isSdnNumber(int position) {
		boolean isSdnNumber = false;
		int partition = getPartitionForPosition(position);
		if (partition >= 0) {
			// Save the old cursor position - the call to getItem() may modify the cursor
			// position.
			int offset = getCursor(partition).getPosition();
			Cursor cursor = (Cursor) getItem(position);
			if (cursor != null) {
				isSdnNumber = cursor.getInt(ContactQuery.IS_SDN_CONTACT) == 1;
				// Restore the old cursor position.
				cursor.moveToPosition(offset);
			}
		}
		return isSdnNumber;
	}
	/*@}*/

	///M:
	private TreeSet<Long> mSelectedContactIds;
	/**
	 * Returns set of selected contacts.
	 */
	public TreeSet<Long> getSelectedContactIds() {
		return mSelectedContactIds;
	}

	public void setSelectedContactIds(TreeSet<Long> mSelectedContactIds) {
		Log.d(TAG,"setSelectedContactIds");
		this.mSelectedContactIds = mSelectedContactIds;
	}

	/**
	 * Builds the {@link Contacts#CONTENT_LOOKUP_URI} for the given
	 * {@link ListView} position.
	 */
	public Uri getContactUri(int position) {
		Log.d(TAG,"getContactUri:"+position);
		int partitionIndex = getPartitionForPosition(position);
		Cursor item = (Cursor)getItem(position);
		return item != null ? getContactUri(partitionIndex, item) : null;
	}

	public Uri getContactUri(int partitionIndex, Cursor cursor) {
		Log.d(TAG,"getContactUri,partitionIndex:"+partitionIndex+" issearchmode:"+isSearchMode());
		if(isSearchMode()&&!isForContactsChoice()){//add by liyang
			long contactId = cursor.getLong(DialerSearchResultColumnsForHb.CONTACT_ID_INDEX);
			String lookupKey = cursor.getString(DialerSearchResultColumnsForHb.LOOKUP_KEY_INDEX);
			Log.d(TAG,"contactId:"+contactId+" lookupKey:"+lookupKey);
			Uri uri = Contacts.getLookupUri(contactId, lookupKey);
			long directoryId = ((DirectoryPartition)getPartition(partitionIndex)).getDirectoryId();
			if (uri != null && directoryId != Directory.DEFAULT) {
				uri = uri.buildUpon().appendQueryParameter(
						ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(directoryId)).build();
			}
			Log.d(TAG,"uri:"+uri);
			return uri;
		}


		long contactId = cursor.getLong(ContactQuery.CONTACT_ID);
		String lookupKey = cursor.getString(ContactQuery.CONTACT_LOOKUP_KEY);
		Uri uri = Contacts.getLookupUri(contactId, lookupKey);
		long directoryId = ((DirectoryPartition)getPartition(partitionIndex)).getDirectoryId();
		if (uri != null && directoryId != Directory.DEFAULT) {
			uri = uri.buildUpon().appendQueryParameter(
					ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(directoryId)).build();
		}
		return uri;
	}
	public static class ViewHolderForContacts {        
		public ContactListItemView view;
		public TextView header;
		public TextView name;
		public CheckBox checkBox;
		public TextView snnipet;
	}	



	public ListView mListView;

	public void setListView(ListView mListView) {
		this.mListView = mListView;
	}


	//copy from #ContactListItemView begin
	protected TextHighlighter mTextHighlighter;
	protected ArrayList<HighlightSequence> mNameHighlightSequence;
	protected ArrayList<HighlightSequence> mNumberHighlightSequence;
	protected static class HighlightSequence {
		private final int start;
		private final int end;

		HighlightSequence(int start, int end) {
			this.start = start;
			this.end = end;
		}
	}

	/**
	 * Adds or updates a text view for the search snippet.
	 */
	public void setSnippet(String text,String mHighlightedPrefix,TextView mSnippetView) {
		if (TextUtils.isEmpty(text)) {
			if (mSnippetView != null) {
				mSnippetView.setText("");
			}
		} else {
			mTextHighlighter.setPrefixText(mSnippetView, text, mHighlightedPrefix);
			if (ContactDisplayUtils.isPossiblePhoneNumber(text)) {
				// Give the text-to-speech engine a hint that it's a phone number
				mSnippetView.setContentDescription(PhoneNumberUtils.createTtsSpannable(text));
			} else {
				mSnippetView.setContentDescription(null);
			}
		}
	}

	public void setDisplayName(CharSequence name,String mHighlightedPrefix,TextView mNameTextView) {
		if (!TextUtils.isEmpty(name)) {
//			Log.d(TAG, "setDisplayName,name:"+name+" mHighlightedPrefix:"+mHighlightedPrefix);
			// Chooses the available highlighting method for highlighting.
			if (mHighlightedPrefix != null) {
				name = mTextHighlighter.applyPrefixHighlight(name, mHighlightedPrefix);
			} else if (mNameHighlightSequence.size() != 0) {
				final SpannableString spannableName = new SpannableString(name);
				for (HighlightSequence highlightSequence : mNameHighlightSequence) {
					mTextHighlighter.applyMaskingHighlight(spannableName, highlightSequence.start,
							highlightSequence.end);
				}
				name = spannableName;
			}
		} else {
			name = mUnknownNameText;
		}
		setMarqueeText(mNameTextView, name);

		if (ContactDisplayUtils.isPossiblePhoneNumber(name)) {
			// Give the text-to-speech engine a hint that it's a phone number
			mNameTextView.setContentDescription(
					PhoneNumberUtils.createTtsSpannable(name.toString()));
		} else {
			mNameTextView.setContentDescription(null);
		}
	}
	protected CharSequence mUnknownNameText;
	private TruncateAt getTextEllipsis() {
		return TruncateAt.MARQUEE;
	}
	private void setMarqueeText(TextView textView, CharSequence text) {
		if (getTextEllipsis() == TruncateAt.MARQUEE) {
			// To show MARQUEE correctly (with END effect during non-active state), we need
			// to build Spanned with MARQUEE in addition to TextView's ellipsize setting.
			final SpannableString spannable = new SpannableString(text);
			spannable.setSpan(TruncateAt.MARQUEE, 0, spannable.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			textView.setText(spannable);
		} else {
			textView.setText(text);
		}
	}

	/**
	 * Shows search snippet.
	 */
	public String showSnippet(Cursor cursor, int summarySnippetColumnIndex) {
		Log.d(TAG,"showSnippet");
		if (cursor.getColumnCount() <= summarySnippetColumnIndex) {
			return null;
		}

		String snippet = cursor.getString(summarySnippetColumnIndex);
		snippet=TextUtils.isEmpty(snippet)?"":snippet.replace(" ", "").toUpperCase();
		Log.d(TAG,"snippet:"+snippet);

		// Do client side snippeting if provider didn't do it
		final Bundle extras = cursor.getExtras();
		if (extras.getBoolean(ContactsContract.DEFERRED_SNIPPETING)) {

			final String query = extras.getString(ContactsContract.DEFERRED_SNIPPETING_QUERY);

			String displayName = null;
			int displayNameIndex = cursor.getColumnIndex(Contacts.DISPLAY_NAME);
			if (displayNameIndex >= 0) {
				displayName = cursor.getString(displayNameIndex);
			}
			Log.d(TAG,"snippet0:"+snippet+" query:"+query+" displayName:"+displayName);
			snippet = updateSnippet(snippet, TextUtils.isEmpty(query)?"":query.toUpperCase(), displayName);
			Log.d(TAG,"snippet1:"+snippet);
		} else {
			if (snippet != null) {
				int from = 0;
				int to = snippet.length();
				int start = snippet.indexOf(DefaultContactListAdapter.SNIPPET_START_MATCH);
				if (start == -1) {
					snippet = null;
				} else {
					int firstNl = snippet.lastIndexOf('\n', start);
					if (firstNl != -1) {
						from = firstNl + 1;
					}
					int end = snippet.lastIndexOf(DefaultContactListAdapter.SNIPPET_END_MATCH);
					if (end != -1) {
						int lastNl = snippet.indexOf('\n', end);
						if (lastNl != -1) {
							to = lastNl;
						}
					}

					StringBuilder sb = new StringBuilder();
					for (int i = from; i < to; i++) {
						char c = snippet.charAt(i);
						if (c != DefaultContactListAdapter.SNIPPET_START_MATCH &&
								c != DefaultContactListAdapter.SNIPPET_END_MATCH) {
							sb.append(c);
						}
					}
					snippet = sb.toString();
				}
			}
		}

		Log.d(TAG,"setSnippet,snippet:"+snippet);
		return snippet;
	}
	
	
	/**
     * Sets phone number for a list item. This takes care of number highlighting if the highlight
     * mask exists.
     */
    public void setPhoneNumber(String text, String countryIso, TextView mDataView) {
        if (text != null) {
            final SpannableString textToSet = new SpannableString(text);

            if (mNumberHighlightSequence.size() != 0) {
                final HighlightSequence highlightSequence = mNumberHighlightSequence.get(0);
                mTextHighlighter.applyMaskingHighlight(textToSet, highlightSequence.start,
                        highlightSequence.end);
            }

            setMarqueeText(mDataView, textToSet);            

            // We have a phone number as "mDataView" so make it always LTR and VIEW_START
            mDataView.setTextDirection(View.TEXT_DIRECTION_LTR);
            mDataView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        }
    }

	/**
	 * Used for deferred snippets from the database. The contents come back as large strings which
	 * need to be extracted for display.
	 *
	 * @param snippet The snippet from the database.
	 * @param query The search query substring.
	 * @param displayName The contact display name.
	 * @return The proper snippet to display.
	 */
	private String updateSnippet(String snippet, String query, String displayName) {

		if (TextUtils.isEmpty(snippet) || TextUtils.isEmpty(query)) {
			return null;
		}
//		query = SearchUtil.cleanStartAndEndOfSearchQuery(query.toLowerCase());

		// If the display name already contains the query term, return empty - snippets should
		// not be needed in that case.
//		if (!TextUtils.isEmpty(displayName)) {
//			final String lowerDisplayName = displayName.toLowerCase();
//			final List<String> nameTokens = split(lowerDisplayName);
//			for (String nameToken : nameTokens) {
//				if (nameToken.startsWith(query)) {
//					Log.d(TAG,"updateSnippet0,nameToken:"+nameToken+" query:"+query);
//					return null;
//				}
//			}
//		}

//		Log.d(TAG,"snippet:"+snippet+" query:"+query);
		// The snippet may contain multiple data lines.
		// Show the first line that matches the query.
		final SearchUtil.MatchedLine matched = SearchUtil.findMatchingLineHb(snippet, query);

//		Log.d(TAG,"matched:"+matched+" line:"+(matched==null?"null":matched.line));
		if (matched != null && matched.line != null) {
			// Tokenize for long strings since the match may be at the end of it.
			// Skip this part for short strings since the whole string will be displayed.
			// Most contact strings are short so the snippetize method will be called infrequently.
			final int lengthThreshold = mContext.getResources().getInteger(
					R.integer.snippet_length_before_tokenize);
			if (matched.line.length() > lengthThreshold) {
//				Log.d(TAG,"updateSnippet1");
				return snippetize(matched.line, matched.startIndex, lengthThreshold);
			} else {
//				Log.d(TAG,"updateSnippet2");
				return matched.line;
			}
		}
//		Log.d(TAG,"updateSnippet3");
		// No match found.
		return null;
	}

	private String snippetize(String line, int matchIndex, int maxLength) {
		// Show up to maxLength characters. But we only show full tokens so show the last full token
		// up to maxLength characters. So as many starting tokens as possible before trying ending
		// tokens.
		int remainingLength = maxLength;
		int tempRemainingLength = remainingLength;

		// Start the end token after the matched query.
		int index = matchIndex;
		int endTokenIndex = index;

		// Find the match token first.
		while (index < line.length()) {
			if (!Character.isLetterOrDigit(line.charAt(index))) {
				endTokenIndex = index;
				remainingLength = tempRemainingLength;
				break;
			}
			tempRemainingLength--;
			index++;
		}

		// Find as much content before the match.
		index = matchIndex - 1;
		tempRemainingLength = remainingLength;
		int startTokenIndex = matchIndex;
		while (index > -1 && tempRemainingLength > 0) {
			if (!Character.isLetterOrDigit(line.charAt(index))) {
				startTokenIndex = index;
				remainingLength = tempRemainingLength;
			}
			tempRemainingLength--;
			index--;
		}

		index = endTokenIndex;
		tempRemainingLength = remainingLength;
		// Find remaining content at after match.
		while (index < line.length() && tempRemainingLength > 0) {
			if (!Character.isLetterOrDigit(line.charAt(index))) {
				endTokenIndex = index;
			}
			tempRemainingLength--;
			index++;
		}
		// Append ellipse if there is content before or after.
		final StringBuilder sb = new StringBuilder();
		if (startTokenIndex > 0) {
			sb.append("...");
		}
		sb.append(line.substring(startTokenIndex, endTokenIndex));
		if (endTokenIndex < line.length()) {
			sb.append("...");
		}
		return sb.toString();
	}

	/**
	 * Helper method for splitting a string into tokens.  The lists passed in are populated with
	 * the
	 * tokens and offsets into the content of each token.  The tokenization function parses e-mail
	 * addresses as a single token; otherwise it splits on any non-alphanumeric character.
	 *
	 * @param content Content to split.
	 * @return List of token strings.
	 */
	private static final Pattern SPLIT_PATTERN = Pattern.compile(
			"([\\w-\\.]+)@((?:[\\w]+\\.)+)([a-zA-Z]{2,4})|[\\w]+");
	private static List<String> split(String content) {
		final Matcher matcher = SPLIT_PATTERN.matcher(content);
		final ArrayList<String> tokens = Lists.newArrayList();
		while (matcher.find()) {
			tokens.add(matcher.group());
		}
		return tokens;
	}
	//copy from #ContactListItemView end

	//modify by liyang       
	protected void bindSectionHeaderAndDividerV2(TextView textView, View devider,int position,
			Cursor cursor) {

		//		Log.d(TAG,"bindSectionHeaderAndDividerV2");
		if(isSearchMode()){
			SortCursor sortCursor=(SortCursor)cursor;
			int currentKey=sortCursor.getKey(position);
			if(position==0){
				textView.setText(currentKey==0?mContext.getResources().getString(R.string.hb_name):mContext.getResources().getString(R.string.hb_other));		
				textView.setVisibility(View.VISIBLE);
			}else{
				int preKey=sortCursor.getKey(position-1);
				if(preKey==0 && currentKey==1){
					textView.setText(mContext.getResources().getString(R.string.hb_other));		
					textView.setVisibility(View.VISIBLE);
				}else{
					textView.setVisibility(View.GONE);
				}
			}
			return;
		}

		if (isSectionHeaderDisplayEnabled()) {
			Placement placement = getItemPlacementInSection(position);
			//				Log.d(TAG,"position:"+position+" name:"+cursor.getString(ContactQuery.CONTACT_DISPLAY_NAME)+" sectionHeader:"+placement.sectionHeader);
			if (!TextUtils.isEmpty(placement.sectionHeader)) {
				textView.setText(placement.sectionHeader);		
				textView.setVisibility(View.VISIBLE);
			}else{
				textView.setVisibility(View.GONE);
			}
			Placement placementNext=getItemPlacementInSection(position+1);
			if (!TextUtils.isEmpty(placementNext.sectionHeader)) {
				devider.setVisibility(View.GONE);
			}else devider.setVisibility(View.VISIBLE);
		}else{
			textView.setVisibility(View.GONE);
		}
	}
	
	public ViewGroup newOutView(){
		return (ViewGroup)LayoutInflater.from(mContext).inflate(R.layout.hb_contacts_listview_item,null);
	}
	
	public ViewGroup newOutView1(){
		return (ViewGroup)LayoutInflater.from(mContext).inflate(R.layout.hb_contacts_listview_item2,null);
	}
	
	public static class ViewHolder {        
		public TextView header;
		public TextView name;
		public View devider;
		public TextView secondLine;
		public CheckBox checkBox;
	}
}
