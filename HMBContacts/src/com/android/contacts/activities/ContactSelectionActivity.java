/*
 * Copyright (C) 2014 MediaTek Inc.
 * Modification based on code covered by the mentioned copyright
 * and/or permission notice(s).
 */
/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.contacts.activities;

import hb.widget.HbIndexBar;
import android.widget.HbSearchView.OnQueryTextListener;
import android.widget.HbSearchView.OnCloseListener;
import android.widget.HbSearchView.OnSuggestionListener;
import com.android.contacts.common.hb.FragmentCallbacks;

import hb.view.menu.bottomnavigation.BottomNavigationView;
import hb.widget.toolbar.Toolbar;

import com.mediatek.contacts.list.ContactListMultiChoiceActivity;
import com.mediatek.contacts.list.ContactsRequestAction;
import com.mediatek.contacts.util.ContactsSettingsUtils;
import com.mediatek.contacts.activities.ActivitiesUtils;
import com.mediatek.contacts.activities.GroupBrowseActivity;

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import hb.provider.ContactsContract.Contacts;
import hb.provider.ContactsContract.Intents.Insert;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
//import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.HbSearchView;
import android.widget.Toast;

import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.common.activity.RequestPermissionsActivity;
import com.android.contacts.common.list.ContactEntryListFragment;
import com.android.contacts.common.util.ImplicitIntentsUtil;
import com.android.contacts.editor.EditorIntents;
import com.android.contacts.list.ContactPickerFragment;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.common.list.DirectoryListLoader;
import com.android.contacts.list.EmailAddressPickerFragment;
import com.android.contacts.list.JoinContactListFragment;
import com.android.contacts.list.LegacyPhoneNumberPickerFragment;
import com.android.contacts.list.OnContactPickerActionListener;
import com.android.contacts.list.OnEmailAddressPickerActionListener;
import com.android.contacts.list.UiIntentActions;
import com.android.contacts.common.list.OnPhoneNumberPickerActionListener;
import com.android.contacts.list.OnPostalAddressPickerActionListener;
import com.android.contacts.common.list.PhoneNumberPickerFragment;
import com.android.contacts.list.PostalAddressPickerFragment;
import com.google.common.collect.Sets;

import java.util.Set;
import java.util.TreeSet;

/**
 * Displays a list of contacts (or phone numbers or postal addresses) for the
 * purposes of selecting one.
 */
public class ContactSelectionActivity extends ContactsActivity
implements View.OnCreateContextMenuListener, OnQueryTextListener, OnClickListener,
OnCloseListener, OnFocusChangeListener,hb.widget.toolbar.Toolbar.OnMenuItemClickListener,FragmentCallbacks {
	private static final String TAG = "ContactSelectionActivity";
	private static final int SUBACTIVITY_ADD_TO_EXISTING_CONTACT = 0;

	private static final String KEY_ACTION_CODE = "actionCode";
	private static final String KEY_SEARCH_MODE = "searchMode";
	private static final int DEFAULT_DIRECTORY_RESULT_LIMIT = 20;

	private ContactsIntentResolver mIntentResolver;
	protected ContactEntryListFragment<?> mListFragment;

	private int mActionCode = -1;
	private boolean mIsSearchMode;
	private boolean mIsSearchSupported;

	private ContactsRequest mRequest;
//	private HbSearchView mSearchView;
//	private View mSearchViewContainer;


	/** M: New Feature */
	private String mFromWhereActivity = "";

	public ContactSelectionActivity() {
		mIntentResolver = new ContactsIntentResolver(this);
	}
	public void hbShowActionMode(boolean show){
		this.showActionMode(show);
	}
	@Override
	public void onAttachFragment(Fragment fragment) {
		if (fragment instanceof ContactEntryListFragment<?>) {
			mListFragment = (ContactEntryListFragment<?>) fragment;
			setupActionListener();
		}
	}
	private Handler mHandler = new Handler();
	private Runnable mShowActionModeRunnable = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			showActionMode(true);
		}
	};

	@Override
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);

		if (RequestPermissionsActivity.startPermissionActivity(this)) {
			return;
		}

		if (savedState != null) {
			mActionCode = savedState.getInt(KEY_ACTION_CODE);
			mIsSearchMode = savedState.getBoolean(KEY_SEARCH_MODE);
		}

		// Extract relevant information from the intent
		mRequest = mIntentResolver.resolveIntent(getIntent());
		if (!mRequest.isValid()) {
			Log.w(TAG, "[onCreate] mRequest is Invalid,finish activity...mRequest:"
					+ mRequest);
			setResult(RESULT_CANCELED);
			finish();
			return;
		}

		configureActivityTitle();

		setHbContentView(R.layout.contact_picker);		

		toolbar = getToolbar();
		toolbar.setElevation(0f);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.d(TAG,"NavigationOnClickListener");
				if(mIsSearchMode){
					back();
				}else{
					setResult(Activity.RESULT_CANCELED);
					finish();
				}
			}
		});
		setupActionModeWithDecor(toolbar);
		
//		initializeSearchView();
		
		if (mActionCode != mRequest.getActionCode()) {
			mActionCode = mRequest.getActionCode();
			configureListFragment();
		}

		
		bottomBar = (BottomNavigationView)findViewById(R.id.bottom_navigation_view);
		bottomBar.setVisibility(View.GONE);
		//        prepareSearchViewAndActionBar();
		
		mListFragment.setmIndexBar((HbIndexBar) findViewById(R.id.index_bar));
		
		addOnSoftKeyBoardVisibleListener();
	}
	
	private boolean sLastVisiable=false;
	public void addOnSoftKeyBoardVisibleListener() {
		final View decorView = getWindow().getDecorView();
		decorView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if(!mIsSearchMode||isOnResumeInSearchMode) {
					isOnResumeInSearchMode=false;
					return;
				}
				Rect rect = new Rect();
				decorView.getWindowVisibleDisplayFrame(rect);
				int displayHight = rect.bottom - rect.top;
				int hight = decorView.getHeight();
				boolean visible = (double) displayHight / hight < 0.8;

				Log.d(TAG, "DecorView display hight = " + displayHight);
				Log.d(TAG, "DecorView hight = " + hight);
				Log.d(TAG, "softkeyboard visible = " + visible);

				if(!visible && visible != sLastVisiable){
//					mSearchView.clearFocus();
				}
				sLastVisiable = visible;
			}
		});
	}
	
	private HbIndexBar mIndexBar;
	public ImageView backIcon;
	private String mQueryString;
	public View mSearchContainer;
//	private void initializeSearchView() {
//		// TODO Auto-generated method stub
//		LayoutInflater inflater = (LayoutInflater) getSystemService(
//				Context.LAYOUT_INFLATER_SERVICE);
////		View backIconView=inflater.inflate(R.layout.hb_back_icon_view, null);
////
////		backIcon=(ImageView)backIconView.findViewById(R.id.hb_back_icon_img);
////		backIconView.setOnClickListener(new View.OnClickListener() {
////
////			@Override
////			public void onClick(View v) {
////				Log.d(TAG,"backIconView onclick");
////				// TODO Auto-generated method stub
////				back();
////			}
////		});
//////		backIcon.setVisibility(View.GONE);
////		backIcon.setColorFilter(getResources().getColor(R.color.hb_toolbar_icon_normal_color));
////		toolbar.addView(backIconView,
////				new hb.widget.toolbar.Toolbar.LayoutParams(Gravity.CENTER_VERTICAL | Gravity.START));
//
//		mSearchContainer = inflater.inflate(R.layout.hb_search_bar_expanded,
//				null);
//		mSearchView = (HbSearchView) mSearchContainer
//				.findViewById(R.id.search_view);
//		
//		int id = mSearchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);    
//		EditText  textView = (EditText ) mSearchView.findViewById(id); 
//		textView.setBackground(getResources().getDrawable(R.drawable.hb_searchview_bg));
//		LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//		layoutParams1.setMargins(0,0,0,0);
//		textView.setLayoutParams(layoutParams1);
//		
//		int search_edit_frame_id=mSearchView.getContext().getResources().getIdentifier("android:id/search_edit_frame", null, null);
//		View search_edit_frame=mSearchView.findViewById(search_edit_frame_id); 		
//		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//		layoutParams.setMargins(getResources().getDimensionPixelSize(R.dimen.hb_choice_searchview_edittext_margin_left_or_right),0,
//				getResources().getDimensionPixelSize(R.dimen.hb_choice_searchview_edittext_margin_left_or_right),0);
//		search_edit_frame.setLayoutParams(layoutParams);	
//		
//		
////		toolbar.setBackgroundColor(getResources().getColor(R.color.hb_toolbar_background_color));
//		mSearchView.needHintIcon(false);
//		mSearchView.setVisibility(View.GONE);
//		mSearchView.setQueryHint(getString(R.string.hb_search_contacts_hint));
//		mSearchView.setQueryHintTextColor(getResources().getColor(R.color.hb_searchview_hint_text_color));
//		//		mSearchView.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
//		//		mSearchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
//		Log.d(TAG,"mSearchContainer:"+mSearchContainer);
//		toolbar.addView(mSearchContainer,
//				new hb.widget.toolbar.Toolbar.LayoutParams(Gravity.CENTER_VERTICAL | Gravity.START));
//		//		mSearchView = (HbSearchView)mToolbar.getMenu().findItem(R.id.menu_search).getActionView();
//		mSearchView.setIconifiedByDefault(false);
//		mSearchView.setOnQueryTextListener(new OnQueryTextListener(){
//			@Override
//			public boolean onQueryTextChange(String queryString) {
//				Log.d(TAG,"onQueryTextChange,queryString:"+queryString);
//				if (queryString.equals(mQueryString)) {
//					return true;
//				}
//				mQueryString = queryString;
//
//				//				if(mListFragment instanceof MultiGroupAddPickerFragment){
//				//					Log.d(TAG,"onQueryTextChange MultiGroupAddPickerFragment");
//				//					mListFragment.setQueryString(mQueryString, true);
//				//					return false;
//				//				}
//
//				mListFragment.setQueryString(mQueryString, true);
//				return false;
//			}
//
//			@Override
//			public boolean onQueryTextSubmit(String str) {
//				return false;
//			}
//		});
//
//		mSearchView.setOnQueryTextFocusChangeListener(new OnFocusChangeListener(){
//			public void onFocusChange(View v, boolean hasFocus){
//				if(hasFocus){
//					Log.d(TAG,"hasFocus,v:"+v);
//					showInputMethod(mSearchView.findFocus());
//					mIsSearchMode=true;
//
//				}
//			}
//		});
//
//	}
	
	public void back(){
		Log.d(TAG, "[back],mIsSearchMode:"+mIsSearchMode);
//		if (mSearchView != null /*&& !mSearchView.isFocused()*/&&mIsSearchMode) {
//			if (!TextUtils.isEmpty(mSearchView.getQuery())) {
//				mSearchView.setQuery(null, true);
//			}
//			hideInputMethod(ContactSelectionActivity.this,mSearchView.findFocus());
////			fragment.updateSelectedItemsView();
//			mListFragment.showHeader(true);
//			mListFragment.setSearchMode(false);
//			mSearchView.clearFocus();
//			mSearchView.setVisibility(View.GONE);
//			toolbar.setBackgroundColor(getResources().getColor(R.color.toolbar_background_color));
//			getWindow().setStatusBarColor(getResources().getColor(R.color.toolbar_background_color)); 
//			mIsSearchMode=false;
////			mListFragment.getmIndexBar().setVisibility(View.VISIBLE);
//			mListFragment.startLoad();
//			return;
//		}
		Log.d(TAG,"back1");
		setResult(Activity.RESULT_CANCELED);
		super.onBackPressed();
	}
	
	public void showInputMethod(View view) {
		if(view==null) return;
        final InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, 0);
        }
    }
	
	/**
     * Hide software keyboard for the given {@link View}.
     */
    public void hideInputMethod(Context context, View view) {
    	if(view==null) return;
        InputMethodManager imm = (InputMethodManager) context.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

	private void prepareSearchViewAndActionBar() {/*
		final ActionBar actionBar = getActionBar();
		mSearchViewContainer = LayoutInflater.from(actionBar.getThemedContext())
				.inflate(R.layout.custom_action_bar, null);
		mSearchView = (SearchView) mSearchViewContainer.findViewById(R.id.search_view);

		// Postal address pickers (and legacy pickers) don't support search, so just show
		// "HomeAsUp" button and title.
		if (mRequest.getActionCode() == ContactsRequest.ACTION_PICK_POSTAL ||
				mRequest.isLegacyCompatibilityMode()) {
			mSearchView.setVisibility(View.GONE);
			if (actionBar != null) {
				actionBar.setDisplayShowHomeEnabled(true);
				actionBar.setDisplayHomeAsUpEnabled(true);
				actionBar.setDisplayShowTitleEnabled(true);
			}
			mIsSearchSupported = false;
			configureSearchMode();
			return;
		}

		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);

		// In order to make the SearchView look like "shown via search menu", we need to
		// manually setup its state. See also DialtactsActivity.java and ActionBarAdapter.java.
		mSearchView.setIconifiedByDefault(true);
		mSearchView.setQueryHint(getString(R.string.hint_findContacts));
		mSearchView.setIconified(false);
		mSearchView.setFocusable(true);

		mSearchView.setOnQueryTextListener(this);
		mSearchView.setOnCloseListener(this);
		mSearchView.setOnQueryTextFocusChangeListener(this);

		actionBar.setCustomView(mSearchViewContainer,
				new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		actionBar.setDisplayShowCustomEnabled(true);

		mIsSearchSupported = true;
		configureSearchMode();
	*/}

	private void configureSearchMode() {/*
		final ActionBar actionBar = getActionBar();
		if (mIsSearchMode) {
//			actionBar.setDisplayShowTitleEnabled(false);
			mSearchViewContainer.setVisibility(View.VISIBLE);
			mSearchView.requestFocus();
		} else {
//			actionBar.setDisplayShowTitleEnabled(true);
			mSearchViewContainer.setVisibility(View.GONE);
			mSearchView.setQuery(null, true);
		}
		invalidateOptionsMenu();
	*/}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// Go back to previous screen, intending "cancel"
			setResult(RESULT_CANCELED);
			/// M: Bug fix ALPS02013610. Need add isResumed() judgement.
			if (isResumed()) {
				onBackPressed();
			}
			return true;
		case R.id.menu_search:
			mIsSearchMode = !mIsSearchMode;
			configureSearchMode();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_ACTION_CODE, mActionCode);
		outState.putBoolean(KEY_SEARCH_MODE, mIsSearchMode);
	}

	private void configureActivityTitle() {
		if (!TextUtils.isEmpty(mRequest.getActivityTitle())) {
			setTitle(mRequest.getActivityTitle());
			Log.w(TAG,
					"[configureActivityTitle] mRequest.getActivityTile != null,return.mRequest:"
							+ mRequest);
			return;
		}

		int actionCode = mRequest.getActionCode();
		Log.d(TAG, "[configureActivityTitle] actionCode:" + actionCode);
		switch (actionCode) {
		case ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT: {
			setTitle(R.string.contactInsertOrEditActivityTitle);
			break;
		}

		case ContactsRequest.ACTION_PICK_CONTACT: {
			setTitle(R.string.contactPickerActivityTitle);
			break;
		}

		case ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT: {
			setTitle(R.string.contactPickerActivityTitle);
			break;
		}

		case ContactsRequest.ACTION_CREATE_SHORTCUT_CONTACT: {
			setTitle(R.string.shortcutActivityTitle);
			break;
		}

		case ContactsRequest.ACTION_PICK_PHONE: {
			setTitle(R.string.contactPickerActivityTitle);
			break;
		}

		case ContactsRequest.ACTION_PICK_EMAIL: {
			setTitle(R.string.contactPickerActivityTitle);
			break;
		}

		case ContactsRequest.ACTION_CREATE_SHORTCUT_CALL: {
			setTitle(R.string.callShortcutActivityTitle);
			break;
		}

		case ContactsRequest.ACTION_CREATE_SHORTCUT_SMS: {
			setTitle(R.string.messageShortcutActivityTitle);
			break;
		}

		case ContactsRequest.ACTION_PICK_POSTAL: {
			setTitle(R.string.contactPickerActivityTitle);
			break;
		}

		case ContactsRequest.ACTION_PICK_JOIN: {
			setTitle(R.string.titleJoinContactDataWith);
			break;
		}
		}
	}

	/**
	 * Creates the fragment based on the current request.
	 */
	public void configureListFragment() {
		Log.d(TAG, "[configureListFragment]mActionCode is::" + mActionCode);
		switch (mActionCode) {
		case ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT: {//添加到已有联系人
			ContactPickerFragment  fragment= new ContactPickerFragment();
			fragment.setEditMode(true);
			fragment.setBottomBar(bottomBar);
//			fragment.setSearchView(mSearchView);
			fragment.setForContactsChoice(true);
			fragment.setCallbacks(this);
//			mSearchView.setVisibility(View.GONE);
			toolbar.setTitle(getResources().getString(R.string.hb_add_to_exist_contact));
			fragment.setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_NONE);
			fragment.setCreateContactEnabled(/*!mRequest.isSearchMode()*/false);
//			fragment.setSectionHeaderDisplayEnabled(false);
			mListFragment = fragment;
			break;
		}

		case ContactsRequest.ACTION_DEFAULT:
		case ContactsRequest.ACTION_PICK_CONTACT: {//信息里面添加联系人附件；
			ContactPickerFragment fragment = new ContactPickerFragment();
			fragment.setIncludeProfile(mRequest.shouldIncludeProfile());
			
			fragment.setBottomBar(bottomBar);
//			fragment.setSearchView(mSearchView);
			fragment.setForContactsChoice(true);
			fragment.setCallbacks(this);
//			mSearchView.setVisibility(View.GONE);
			
			mListFragment = fragment;
			break;
		}

		case ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT: {
			ContactPickerFragment fragment = new ContactPickerFragment();
			fragment.setCreateContactEnabled(!mRequest.isSearchMode());
			mListFragment = fragment;
			break;
		}

		case ContactsRequest.ACTION_CREATE_SHORTCUT_CONTACT: {
			ContactPickerFragment fragment = new ContactPickerFragment();
			fragment.setShortcutRequested(true);
			mListFragment = fragment;
			
//			fragment.setSearchView(mSearchView);
			fragment.setForContactsChoice(true);
			fragment.setCallbacks(this);
//			mSearchView.setVisibility(View.GONE);
			toolbar.setTitle(R.string.hb_choose_contacts);
			break;
		}

		case ContactsRequest.ACTION_PICK_PHONE: {//单选号码(快速拨号设置)
			PhoneNumberPickerFragment fragment = getPhoneNumberPickerFragment(mRequest);
			//CallableUri's default value is false
			//If it set to true, query uri will be Callable.CONTENT_URI
			boolean isCallableUri = getIntent().getBooleanExtra("isCallableUri", false);
			fragment.setUseCallableUri(isCallableUri);			
			fragment.setBottomBar(bottomBar);
//			fragment.setSearchView(mSearchView);
			fragment.setForContactsChoice(true);
			fragment.setCallbacks(this);
//			mSearchView.setVisibility(View.GONE);
			toolbar.setTitle(R.string.hb_choose_number);
			mListFragment = fragment;
			break;
		}

		case ContactsRequest.ACTION_PICK_EMAIL: {
			mListFragment = new EmailAddressPickerFragment();
			break;
		}

		case ContactsRequest.ACTION_CREATE_SHORTCUT_CALL: {
			PhoneNumberPickerFragment fragment = getPhoneNumberPickerFragment(mRequest);
			fragment.setShortcutAction(Intent.ACTION_CALL);

//			fragment.setSearchView(mSearchView);
			fragment.setForContactsChoice(true);
			fragment.setCallbacks(this);
//			mSearchView.setVisibility(View.GONE);
			toolbar.setTitle(R.string.hb_choose_number);
			mListFragment = fragment;
			break;
		}

		case ContactsRequest.ACTION_CREATE_SHORTCUT_SMS: {
			PhoneNumberPickerFragment fragment = getPhoneNumberPickerFragment(mRequest);
			fragment.setShortcutAction(Intent.ACTION_SENDTO);

//			fragment.setSearchView(mSearchView);
			fragment.setForContactsChoice(true);
			fragment.setCallbacks(this);
//			mSearchView.setVisibility(View.GONE);
			toolbar.setTitle(R.string.hb_choose_number);
			mListFragment = fragment;
			break;
		}

		case ContactsRequest.ACTION_PICK_POSTAL: {
			PostalAddressPickerFragment fragment = new PostalAddressPickerFragment();

			mListFragment = fragment;
			break;
		}

		case ContactsRequest.ACTION_PICK_JOIN: {
			JoinContactListFragment joinFragment = new JoinContactListFragment();
			joinFragment.setTargetContactId(getTargetContactId());
			mListFragment = joinFragment;
			break;
		}

		default:
			throw new IllegalStateException("Invalid action code: " + mActionCode);
		}

		/** M: */
		ActivitiesUtils.setPickerFragmentAccountType(this, mListFragment);


		// Setting compatibility is no longer needed for PhoneNumberPickerFragment since that logic
		// has been separated into LegacyPhoneNumberPickerFragment.  But we still need to set
		// compatibility for other fragments.
		mListFragment.setLegacyCompatibilityMode(mRequest.isLegacyCompatibilityMode());
		mListFragment.setDirectoryResultLimit(DEFAULT_DIRECTORY_RESULT_LIMIT);

		getFragmentManager().beginTransaction()
		.replace(R.id.list_container, mListFragment)
		.commitAllowingStateLoss();
	}

	private PhoneNumberPickerFragment getPhoneNumberPickerFragment(ContactsRequest request) {
		if (mRequest.isLegacyCompatibilityMode()) {
			return new LegacyPhoneNumberPickerFragment();
		} else {
			return new PhoneNumberPickerFragment();
		}
	}

	public void setupActionListener() {
		if (mListFragment instanceof ContactPickerFragment) {
			((ContactPickerFragment) mListFragment).setOnContactPickerActionListener(
					new ContactPickerActionListener());
		} else if (mListFragment instanceof PhoneNumberPickerFragment) {
			((PhoneNumberPickerFragment) mListFragment).setOnPhoneNumberPickerActionListener(
					new PhoneNumberPickerActionListener());
		} else if (mListFragment instanceof PostalAddressPickerFragment) {
			((PostalAddressPickerFragment) mListFragment).setOnPostalAddressPickerActionListener(
					new PostalAddressPickerActionListener());
		} else if (mListFragment instanceof EmailAddressPickerFragment) {
			((EmailAddressPickerFragment) mListFragment).setOnEmailAddressPickerActionListener(
					new EmailAddressPickerActionListener());
		} else if (mListFragment instanceof JoinContactListFragment) {
			((JoinContactListFragment) mListFragment).setOnContactPickerActionListener(
					new JoinContactActionListener());
		} else {
			throw new IllegalStateException("Unsupported list fragment type: " + mListFragment);
		}
	}

	private final class ContactPickerActionListener implements OnContactPickerActionListener {
		@Override
		public void onCreateNewContactAction() {
			startCreateNewContactActivity();
		}

		@Override
		public void onEditContactAction(Uri contactLookupUri) {
			Bundle extras = getIntent().getExtras();
			if (/*launchAddToContactDialog(extras)*/false) {
				// Show a confirmation dialog to add the value(s) to the existing contact.
				Intent intent = new Intent(ContactSelectionActivity.this,
						ConfirmAddDetailActivity.class);
				intent.setData(contactLookupUri);
				if (extras != null) {
					// First remove name key if present because the dialog does not support name
					// editing. This is fine because the user wants to add information to an
					// existing contact, who should already have a name and we wouldn't want to
					// override the name.
					extras.remove(Insert.NAME);
					intent.putExtras(extras);
				}

				// Wait for the activity result because we want to keep the picker open (in case the
				// user cancels adding the info to a contact and wants to pick someone else).
				startActivityForResult(intent, SUBACTIVITY_ADD_TO_EXISTING_CONTACT);
			} else {
				// Otherwise launch the full contact editor.
				/// M: it should add isEditingUserProfile flag
//				startActivityAndForwardResult(EditorIntents.createEditContactIntent(
//						contactLookupUri, /* materialPalette =*/ null, /* photoId =*/ -1,
//						/* nameId =*/ -1, /* isEditingUserProfile =*/ false));
				startActivityAndForwardResult(EditorIntents.createCompactEditContactIntent(
						contactLookupUri, /* materialPalette =*/ null, /* updatedPhotos =*/ null, /* photoId =*/ -1,
						/* nameId =*/ -1, /* isEditingUserProfile =*/ false));
			}
		}

		@Override
		public void onPickContactAction(Uri contactUri) {
			returnPickerResult(contactUri);
		}

		@Override
		public void onShortcutIntentCreated(Intent intent) {
			returnPickerResult(intent);
		}

		/**
		 * Returns true if is a single email or single phone number provided in the {@link Intent}
		 * extras bundle so that a pop-up confirmation dialog can be used to add the data to
		 * a contact. Otherwise return false if there are other intent extras that require launching
		 * the full contact editor. Ignore extras with the key {@link Insert.NAME} because names
		 * are a special case and we typically don't want to replace the name of an existing
		 * contact.
		 */
		private boolean launchAddToContactDialog(Bundle extras) {
			if (extras == null) {
				Log.w(TAG, "[launchAddToContactDialog] extras is null");
				return false;
			}

			// Copy extras because the set may be modified in the next step
			Set<String> intentExtraKeys = Sets.newHashSet();
			intentExtraKeys.addAll(extras.keySet());

			// Ignore name key because this is an existing contact.
			if (intentExtraKeys.contains(Insert.NAME)) {
				intentExtraKeys.remove(Insert.NAME);
			}

			int numIntentExtraKeys = intentExtraKeys.size();
			if (numIntentExtraKeys == 2) {
				boolean hasPhone = intentExtraKeys.contains(Insert.PHONE) &&
						intentExtraKeys.contains(Insert.PHONE_TYPE);
				boolean hasEmail = intentExtraKeys.contains(Insert.EMAIL) &&
						intentExtraKeys.contains(Insert.EMAIL_TYPE);
				return hasPhone || hasEmail;
			} else if (numIntentExtraKeys == 1) {
				return intentExtraKeys.contains(Insert.PHONE) ||
						intentExtraKeys.contains(Insert.EMAIL);
			}
			// Having 0 or more than 2 intent extra keys means that we should launch
			// the full contact editor to properly handle the intent extras.
			return false;
		}
	}

	private final class PhoneNumberPickerActionListener implements
	OnPhoneNumberPickerActionListener {
		@Override
		public void onPickPhoneNumberAction(Uri dataUri) {
			returnPickerResult(dataUri);
		}

		@Override
		public void onCallNumberDirectly(String phoneNumber) {
			Log.w(TAG, "Unsupported call.");
		}

		@Override
		public void onCallNumberDirectly(String phoneNumber, boolean isVideoCall) {
			Log.w(TAG, "Unsupported call.");
		}

		@Override
		public void onShortcutIntentCreated(Intent intent) {
			returnPickerResult(intent);
		}

		public void onHomeInActionBarSelected() {
			ContactSelectionActivity.this.onBackPressed();
		}
	}

	private final class JoinContactActionListener implements OnContactPickerActionListener {
		@Override
		public void onPickContactAction(Uri contactUri) {
			Intent intent = new Intent(null, contactUri);
			setResult(RESULT_OK, intent);
			finish();
		}

		@Override
		public void onShortcutIntentCreated(Intent intent) {
		}

		@Override
		public void onCreateNewContactAction() {
		}

		@Override
		public void onEditContactAction(Uri contactLookupUri) {
		}
	}

	private final class PostalAddressPickerActionListener implements
	OnPostalAddressPickerActionListener {
		@Override
		public void onPickPostalAddressAction(Uri dataUri) {
			returnPickerResult(dataUri);
		}
	}

	private final class EmailAddressPickerActionListener implements
	OnEmailAddressPickerActionListener {
		@Override
		public void onPickEmailAddressAction(Uri dataUri) {
			returnPickerResult(dataUri);
		}
	}

	public void startActivityAndForwardResult(final Intent intent) {
		intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

		/** M: New Feature @{ */
			intent.putExtra(ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, false);
			intent.putExtra("isFromCallDetail",getIntent().getBooleanExtra("isFromCallDetail", false));
			/** @} */
			// Forward extras to the new activity
			Bundle extras = getIntent().getExtras();
			if (extras != null) {
				intent.putExtras(extras);
			}
			try {
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "startActivity() failed: " + e);
				Toast.makeText(ContactSelectionActivity.this, R.string.missing_app,
						Toast.LENGTH_SHORT).show();
			}
			finish();
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		mListFragment.setQueryString(newText, true);
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		return false;
	}

	@Override
	public boolean onClose() {
//		if (!TextUtils.isEmpty(mSearchView.getQuery())) {
//			mSearchView.setQuery(null, true);
//		}
		return true;
	}

	@Override
	public void onFocusChange(View view, boolean hasFocus) {
		switch (view.getId()) {
		case R.id.search_view: {
			if (hasFocus) {
//				showInputMethod(mSearchView.findFocus());
			}
		}
		}
	}

	public void returnPickerResult(Uri data) {
		Intent intent = new Intent();
		intent.setData(data);
		returnPickerResult(intent);
	}

	public void returnPickerResult(Intent intent) {
		intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.floating_action_button: {
			startCreateNewContactActivity();
			break;
		}
		}
	}

	private long getTargetContactId() {
		Intent intent = getIntent();
		final long targetContactId = intent.getLongExtra(
				UiIntentActions.TARGET_CONTACT_ID_EXTRA_KEY, -1);
		if (targetContactId == -1) {
			Log.e(TAG, "Intent " + intent.getAction() + " is missing required extra: "
					+ UiIntentActions.TARGET_CONTACT_ID_EXTRA_KEY);
			setResult(RESULT_CANCELED);
			finish();
			return -1;
		}
		return targetContactId;
	}

	private void startCreateNewContactActivity() {
		Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
		intent.putExtra(ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, false);
		/// M: Add account type for handling special case for add new contactor
		intent.putExtra(ContactsSettingsUtils.ACCOUNT_TYPE,
				getIntent().getIntExtra(ContactsSettingsUtils.ACCOUNT_TYPE,
						ContactsSettingsUtils.ALL_TYPE_ACCOUNT));
		intent.setClassName("com.android.contacts","com.android.contacts.activities.CompactContactEditorActivity"); 
		startActivityAndForwardResult(intent);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "[onActivityResult] requestCode:" + requestCode + ",resultCode:"
				+ resultCode);
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == SUBACTIVITY_ADD_TO_EXISTING_CONTACT) {
			if (resultCode == Activity.RESULT_OK) {
				if (data != null) {
					ImplicitIntentsUtil.startActivityInAppIfPossible(this, data);
				}
				finish();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.search_menu, menu);

		final MenuItem searchItem = menu.findItem(R.id.menu_search);
		/// M:Fix ALPS01777704,dismiss searchItem when mSearchView set gone
		// change for ALPS02364621, mSearchView would not been inited at onCreate if permission
		// check fail. @{
//		if (mSearchView != null) {
//			searchItem.setVisible(!mIsSearchMode && mIsSearchSupported
//					&& mSearchView.getVisibility() != View.GONE);
//			Log.d(TAG, "searchMode:" + mIsSearchMode + ",mSearchView Visib:"
//					+ mSearchView.getVisibility());
//		} else {
//			Log.d(TAG, "mSearchView has not been inited ");
//		}
		/// @}
		return true;
	}

	@Override
	public void onBackPressed() {
//		if (mIsSearchMode) {
//			mIsSearchMode = false;
//			configureSearchMode();
//		} else {
//			super.onBackPressed();
//		}
		back();
//		super.onBackPressed();
		
	}
	
	private boolean isOnResumeInSearchMode=false;
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if(mIsSearchMode) isOnResumeInSearchMode=true;
	}
	@Override
	public Object onFragmentCallback(int what, final Object obj) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onFragmentCallback,what:" + what + " obj:" + obj);
		switch (what) {
		case FragmentCallbacks.SWITCH_TO_SEARCH_MODE:
//			actionMode.dismiss();
//			mSearchView.setVisibility(View.VISIBLE);
//			mSearchView.requestFocus();
			mListFragment.setSearchMode(true);
			toolbar.setBackgroundColor(getResources().getColor(R.color.hb_toolbar_background_color));
			getWindow().setStatusBarColor(getResources().getColor(R.color.hb_toolbar_background_color)); 
			mListFragment.getmIndexBar().setVisibility(View.GONE);
//			if(addRecordContactsMenuItem!=null) addRecordContactsMenuItem.setVisible(false);
			break;
		}
		return null;
	}
}
