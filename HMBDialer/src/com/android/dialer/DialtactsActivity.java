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

package com.android.dialer;

import android.content.ClipboardManager;
import com.hb.t9search.ContactsHelper;
import com.hb.t9search.ContactsHelper.OnContactsLoad;
import com.hp.hpl.sparta.Text;
import com.cootek.smartdialer_oem_module.sdk.CooTekPhoneService;
import hb.widget.tab.TabLayout.Tab;

import static android.Manifest.permission.READ_CONTACTS;
import com.android.contacts.common.util.PermissionsUtil;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alipay.mobilesecuritysdk.MainHandler;
import com.android.contacts.common.activity.TransactionSafeActivity;
import com.android.contacts.common.interactions.TouchPointManager;
import com.android.contacts.common.list.OnPhoneNumberPickerActionListener;
import com.android.contacts.common.widget.FloatingActionButtonController;
import com.android.contacts.commonbind.analytics.AnalyticsUtil;
import com.android.dialer.calllog.CallLogFragment;
import com.android.dialer.calllog.CallLogNotificationsService;
import com.android.dialer.database.DialerDatabaseHelper;
import com.android.dialer.dialpad.DialpadFragment;
import com.android.dialer.dialpad.SmartDialNameMatcher;
import com.android.dialer.dialpad.SmartDialPrefix;
import com.android.dialer.interactions.PhoneNumberInteraction;
import com.android.dialer.list.DragDropController;
import com.android.dialer.list.ListsFragment;
import com.android.dialer.list.OnDragDropListener;
import com.android.dialer.list.OnListFragmentScrolledListener;
import com.android.dialer.list.PhoneFavoriteSquareTileView;
import com.android.dialer.list.RegularSearchFragment;
import com.android.dialer.list.SearchFragment;
import com.android.dialer.list.SmartDialSearchFragment;
import com.android.dialer.list.SpeedDialFragment;
import com.android.dialer.util.DialerUtils;
import com.android.dialer.util.IntentUtil;
import com.android.dialer.widget.ActionBarController;
import com.android.dialerbind.DatabaseHelperManager;
//import com.android.ims.ImsManager;
import com.android.phone.common.animation.AnimUtils;
import com.android.phone.common.animation.AnimationListenerAdapter;
import com.android.phone.common.dialpad.DigitsEditText;
import com.mediatek.dialer.util.DialerFeatureOptions;
import com.mediatek.dialer.util.DialerVolteUtils;
import com.mediatek.dialer.util.PhoneInfoUtils;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet.Builder;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Camera;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Trace;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.View.OnDragListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView.OnScrollListener;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.Toolbar;
import hb.view.menu.bottomnavigation.BottomNavigationView;
import hb.view.menu.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;
import hb.widget.FloatingActionButton;
import hb.widget.FloatingActionButton.OnFloatActionButtonClickListener;
import junit.framework.Assert;

/**
 * M: Inherited from NeedTestActivity for easy mock testing The dialer tab's
 * title is 'phone', a more common name (see strings.xml).
 */
public class DialtactsActivity extends TransactionSafeActivity
implements View.OnClickListener, DialpadFragment.OnDialpadQueryChangedListener, OnListFragmentScrolledListener,
CallLogFragment.HostInterface, DialpadFragment.HostInterface, ListsFragment.HostInterface,
SpeedDialFragment.HostInterface, SearchFragment.HostInterface, OnDragDropListener,
OnPhoneNumberPickerActionListener, PopupMenu.OnMenuItemClickListener, ActionBarController.ActivityUi,
Toolbar.OnMenuItemClickListener, hb.widget.tab.TabLayout.OnTabSelectedListener,
OnContactsLoad{
	private static final String TAG = "DialtactsActivity";

	/// M: For the purpose of debugging in eng load
	public static final boolean DEBUG = /* Build.TYPE.equals("eng") */true;

	public static final String SHARED_PREFS_NAME = "com.android.dialer_preferences";

	/** @see #getCallOrigin() */
	private static final String CALL_ORIGIN_DIALTACTS = "com.android.dialer.DialtactsActivity";

	private static final String KEY_IN_REGULAR_SEARCH_UI = "in_regular_search_ui";
	private static final String KEY_IN_DIALPAD_SEARCH_UI = "in_dialpad_search_ui";
	private static final String KEY_SEARCH_QUERY = "search_query";
	private static final String KEY_FIRST_LAUNCH = "first_launch";
	private static final String KEY_IS_DIALPAD_SHOWN = "is_dialpad_shown";
	/// M: Save and restore the mPendingSearchViewQuery
	private static final String KEY_PENDING_SEARCH_QUERY = "pending_search_query";

	private static final String TAG_DIALPAD_FRAGMENT = "dialpad";
	private static final String TAG_REGULAR_SEARCH_FRAGMENT = "search";
	private static final String TAG_SMARTDIAL_SEARCH_FRAGMENT = "smartdial";
	private static final String TAG_FAVORITES_FRAGMENT = "favorites";

	/**
	 * Just for backward compatibility. Should behave as same as
	 * {@link Intent#ACTION_DIAL}.
	 */
	private static final String ACTION_TOUCH_DIALER = "com.android.phone.action.TOUCH_DIALER";
	public static final String EXTRA_SHOW_TAB = "EXTRA_SHOW_TAB";

	private static final int ACTIVITY_REQUEST_CODE_VOICE_SEARCH = 1;
	/// M: Add for import/export function
	private static final int IMPORT_EXPORT_REQUEST_CODE = 2;

	private static final int FAB_SCALE_IN_DELAY_MS = 300;

	//	private RelativeLayout mParentLayout;

	/**
	 * Fragment containing the dialpad that slides into view
	 */
	protected DialpadFragment mDialpadFragment;

	/**
	 * Fragment for searching phone numbers using the alphanumeric keyboard.
	 */
	private RegularSearchFragment mRegularSearchFragment;

	/**
	 * Fragment for searching phone numbers using the dialpad.
	 */
	private SmartDialSearchFragment mSmartDialSearchFragment;

	/**
	 * Animation that slides in.
	 */
	private Animation mSlideIn;

	/**
	 * Animation that slides out.
	 */
	private Animation mSlideOut;

	public View digits_container;

	public View getDigits_container() {
		return digits_container;
	}

	private DigitsEditText mDigitsEditText;

	public BottomNavigationView getmBottomBar() {
		return bottomBar;
	}

	public ActionMode getmActionMode() {
		return actionMode;
	}

	AnimationListenerAdapter mSlideInListener = new AnimationListenerAdapter() {
		@Override
		public void onAnimationEnd(Animation animation) {
			// maybeEnterSearchUi();
		}
	};


	/**
	 * Listener for after slide out animation completes on dialer fragment.
	 */
	AnimationListenerAdapter mSlideOutListener = new AnimationListenerAdapter() {
		@Override
		public void onAnimationEnd(Animation animation) {
			commitDialpadFragmentHide();		
		}
	};
	private int mActionBarHeight;
	@Override
	public int getActionBarHeight() {
		return mActionBarHeight;
	}
	/**
	 * Fragment containing the speed dial list, recents list, and all contacts
	 * list.
	 */
	public ListsFragment mListsFragment;

	/**
	 * Tracks whether onSaveInstanceState has been called. If true, no fragment
	 * transactions can be commited.
	 */
	private boolean mStateSaved;
	private boolean mIsRestarting;
	private boolean mInDialpadSearch;
	private boolean mInRegularSearch;
	private boolean mClearSearchOnPause;
	public boolean mIsDialpadShown;
	private boolean mShowDialpadOnResume;

	/**
	 * Whether or not the device is in landscape orientation.
	 */
	private boolean mIsLandscape;

	/**
	 * True if the dialpad is only temporarily showing due to being in call
	 */
	private boolean mInCallDialpadUp;

	/**
	 * True when this activity has been launched for the first time.
	 */
	private boolean mFirstLaunch;

	/**
	 * Search query to be applied to the SearchView in the ActionBar once
	 * onCreateOptionsMenu has been called.
	 */
	private String mPendingSearchViewQuery;

	private PopupMenu mOverflowMenu;
	// private EditText mSearchView;
	private View mVoiceSearchButton;

	private String mSearchQuery;

	private DialerDatabaseHelper mDialerDatabaseHelper;
	private DragDropController mDragDropController;
	private ActionBarController mActionBarController;

	private FloatingActionButtonController mFloatingActionButtonController;

	public void showFAB(boolean isShow) {
		Log.d(TAG, "showFAB,isShow:" + isShow);
		floatingActionButton.setVisibility(isShow ? View.VISIBLE : View.GONE);
	}

	public void hideDialpad() {
		hideDialpadFragment(true, false);
		hideDialpadByFoldbutton = true;
	}

	public void hbShowDialpad() {
		Log.d(TAG,"hbShowDialpad");
		if (mIsDialpadShown || mStateSaved) {
			return;
		}

		handleHbIntent();
		if(indexFrom==ListsFragment.TAB_INDEX_RECENTS) showDialpadFragment(false);

	}

	public void setQuery() {
		Log.d(TAG, "setQuery,mSmartDialSearchFragment:" + mSmartDialSearchFragment
				+ " mSmartDialSearchFragment.isVisible():" + mSmartDialSearchFragment.isVisible()+" mSearchQuery:"+mSearchQuery);
		if (mSmartDialSearchFragment != null/*
		 * && mSmartDialSearchFragment.
		 * isVisible()
		 */) {
			if (digits_container.getVisibility() != View.VISIBLE) {
				setDigitsState(true);
			}
			if(com.android.contacts.common.HbUtils.isMTK) mSmartDialSearchFragment.setQueryString(mSearchQuery, false /* delaySelection */);
			else mSmartDialSearchFragment.setQueryStringHb(mSearchQuery);
		}
	}

	/**
	 * The text returned from a voice search query. Set in
	 * {@link #onActivityResult} and used in {@link #onResume()} to populate the
	 * search box.
	 */
	private String mVoiceSearchQuery;

	/*
	 * protected class OptionsPopupMenu extends PopupMenu { public
	 * OptionsPopupMenu(Context context, View anchor) { super(context, anchor,
	 * Gravity.END); }
	 * 
	 * @Override public void show() { final boolean hasContactsPermission =
	 * PermissionsUtil.hasContactsPermissions(DialtactsActivity.this); final
	 * Menu menu = getMenu(); final MenuItem clearFrequents =
	 * menu.findItem(R.id.menu_clear_frequents);
	 * clearFrequents.setVisible(mListsFragment != null &&
	 * mListsFragment.getSpeedDialFragment() != null &&
	 * mListsFragment.getSpeedDialFragment().hasFrequents() &&
	 * hasContactsPermission);
	 * 
	 * menu.findItem(R.id.menu_import_export).setVisible(hasContactsPermission);
	 * menu.findItem(R.id.menu_add_contact).setVisible(hasContactsPermission);
	 * 
	 * menu.findItem(R.id.menu_history).setVisible(
	 * PermissionsUtil.hasPhonePermissions(DialtactsActivity.this));
	 * super.show(); } }
	 */

	/**
	 * Listener that listens to drag events and sends their x and y coordinates
	 * to a {@link DragDropController}.
	 */
	private class LayoutOnDragListener implements OnDragListener {
		@Override
		public boolean onDrag(View v, DragEvent event) {
			if (event.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
				mDragDropController.handleDragHovered(v, (int) event.getX(), (int) event.getY());
			}
			return true;
		}
	}

	/**
	 * Listener used to send search queries to the phone search fragment.
	 */
	public final TextWatcher mPhoneSearchQueryTextListener = new TextWatcher() {
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			Log.d(TAG, "onTextChange query: " + s.toString() + " isSmartFragmentAdded:" + isSmartFragmentAdded);
			Log.d(TAG, "Previous Query: " + mSearchQuery);

			final String newText = isFromSaveInstance?mSearchQuery:s.toString().replace(" ", "");
			if (!isFromSaveInstance && newText!=null && mSearchQuery!=null && TextUtils.equals(newText, mSearchQuery)) {
				// If the query hasn't changed (perhaps due to activity being
				// destroyed
				// and restored, or user launching the same DIAL intent twice),
				// then there is
				// no need to do anything here.
				return;
			}

			// Show search fragment only when the query string is changed to
			// non-empty text.
			if (!TextUtils.isEmpty(newText)) {
				// // Call enterSearchUi only if we are switching search modes,
				// or showing a search
				// // fragment for the first time.
				// final boolean sameSearchMode = (mIsDialpadShown &&
				// mInDialpadSearch) ||
				// (!mIsDialpadShown && mInRegularSearch);

				// if(mListsFragment.isVisible())
				// mListsFragment.getView().setVisibility(View.GONE);
				// mListsFragment.getView().animate().alpha(0).withLayer();
				// if(digits_container.getVisibility()!=View.VISIBLE)
				// digits_container.setVisibility(View.VISIBLE);
				maybeEnterSearchUi();
			} else {
				// digits_container.setVisibility(View.GONE);
				// if(!mListsFragment.isVisible())
				// mListsFragment.getView().setVisibility(View.VISIBLE);
				if (isInSearchUi()) {
					if (TextUtils.isEmpty(newText) && !DialpadFragment.isLongClickZero) {
						exitSearchUi();
						setDigitsState(false);
					}
				}
			}

			mSearchQuery = newText;

			Log.d(TAG,
					"mSmartDialSearchFragment:" + mSmartDialSearchFragment + "  mSmartDialSearchFragment.isVisible():"
							+ (mSmartDialSearchFragment == null ? "null" : mSmartDialSearchFragment.isVisible()));
			if (mSmartDialSearchFragment != null && mSmartDialSearchFragment.isVisible()) {
				if(com.android.contacts.common.HbUtils.isMTK) mSmartDialSearchFragment.setQueryString(mSearchQuery, false /* delaySelection */);
				else mSmartDialSearchFragment.setQueryStringHb(mSearchQuery);
			} else if (mRegularSearchFragment != null && mRegularSearchFragment.isVisible()) {
				mRegularSearchFragment.setQueryString(mSearchQuery, false /* delaySelection */);
			}
			isFromSaveInstance=false;
		}

		@Override
		public void afterTextChanged(Editable s) {
		}
	};

	private void setDigitsState(boolean show) {
		digits_container.setVisibility(show ? View.VISIBLE : View.GONE);
		// 设置状态栏颜色
		if (show)
			getWindow().setStatusBarColor(Color.parseColor("#fff8f8f8"));
		else {
			int[] attrsArray = { android.R.attr.colorPrimary };
			android.content.res.TypedArray typedArray = this.obtainStyledAttributes(attrsArray);
			int accentColor = typedArray.getColor(0,
					Color.WHITE/* Default color */);
			typedArray.recycle();
			getWindow().setStatusBarColor(accentColor);
		}
	}

	/**
	 * Open the search UI when the user clicks on the search box.
	 */
	private final View.OnClickListener mSearchViewOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (!isInSearchUi()) {
				// mActionBarController.onSearchBoxTapped();
				enterSearchUi(false /* smartDialSearch */, mDigitsEditText.getText().toString(), true /* animate */);
			}
		}
	};

	// /**
	// * Handles the user closing the soft keyboard.
	// */
	// private final View.OnKeyListener mSearchEditTextLayoutListener = new
	// View.OnKeyListener() {
	// @Override
	// public boolean onKey(View v, int keyCode, KeyEvent event) {
	// if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() ==
	// KeyEvent.ACTION_DOWN) {
	// if (TextUtils.isEmpty(mSearchView.getText().toString())) {
	// // If the search term is empty, close the search UI.
	// maybeExitSearchUi();
	// /// M: end the back key dispatch to avoid activity onBackPressed is
	// called.
	// return true;
	// } else {
	// // If the search term is not empty, show the dialpad fab.
	// showFabInSearchUi();
	// }
	// }
	// return false;
	// }
	// };

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		//		if (ev.getAction() == MotionEvent.ACTION_DOWN) {
		//			TouchPointManager.getInstance().setPoint((int) ev.getRawX(), (int) ev.getRawY());
		//		}
		return super.dispatchTouchEvent(ev);

	}

	public Toolbar getToolBar() {
		return toolbar;
	}

	private FloatingActionButton floatingActionButton;
	private boolean mDialConferenceButtonPressed = false;
	// private LayoutInflater mInflater;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Trace.beginSection(TAG + " onCreate");
		Log.d(TAG, "oncreate");
		super.onCreate(savedInstanceState);

		mFirstLaunch = true;
		toolbar = getToolbar();
		toolbar.setElevation(0f);
		toolbar.inflateMenu(com.android.contacts.common.HbUtils.isMTK?R.menu.dialtacts_options:R.menu.dialtacts_options2);

		Log.d(TAG,"indexFrom0:"+indexFrom);
		Trace.beginSection(TAG + " setContentView");
		setHbContentView(R.layout.dialtacts_activity);
		Trace.endSection();
		getWindow().setBackgroundDrawable(null);
		digits_container = findViewById(R.id.digits_container);
		mDigitsEditText = (DigitsEditText) digits_container.findViewById(R.id.digits);
		mDigitsEditText.addTextChangedListener(mPhoneSearchQueryTextListener);
		Trace.beginSection(TAG + " setup Views");

		mIsLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

		floatingActionButton = (FloatingActionButton) findViewById(R.id.floating_action_button_container);
		floatingActionButton.setOnFloatingActionButtonClickListener(new OnFloatActionButtonClickListener() {
			public void onClick(View view) {

				if(mDialpadFragment!=null && System.currentTimeMillis()-mDialpadFragment.lastTime<600){
					Log.d(TAG,"click too quick");
					return;
				}

				Log.d(TAG, "fab onclick,mIsDialpadShown:" + mIsDialpadShown);

				if (/* mListsFragment.getCurrentTabIndex() */tabIndexPre == ListsFragment.TAB_INDEX_ALL_CONTACTS) {
					DialerUtils.startActivityWithErrorToast(DialtactsActivity.this, IntentUtil.getNewContactIntent(),
							R.string.add_contact_not_available);
					return;
				}

				mDialConferenceButtonPressed = false;
				if (mDialpadFragment != null) {
					// mDialpadFragment.showDialConference(false);
				}
				if (!mIsDialpadShown) {
					mInCallDialpadUp = false;
					showDialpadFragment(true);
				} else {
					// Dial button was pressed; tell the Dialpad fragment
					mDialpadFragment.handleDialButtonPressed();
				}
			}
		});
		mFloatingActionButtonController = new FloatingActionButtonController(this, floatingActionButton, null);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
			.add(R.id.dialtacts_frame, new ListsFragment(), TAG_FAVORITES_FRAGMENT).commit();
		} else {
			mSearchQuery = savedInstanceState.getString(KEY_SEARCH_QUERY);
			mInRegularSearch = savedInstanceState.getBoolean(KEY_IN_REGULAR_SEARCH_UI);
			mInDialpadSearch = savedInstanceState.getBoolean(KEY_IN_DIALPAD_SEARCH_UI);
			mFirstLaunch = savedInstanceState.getBoolean(KEY_FIRST_LAUNCH);
			mShowDialpadOnResume = savedInstanceState.getBoolean(KEY_IS_DIALPAD_SHOWN);
			/// M: Save and restore the mPendingSearchViewQuery
			mPendingSearchViewQuery = savedInstanceState.getString(KEY_PENDING_SEARCH_QUERY);
			isFromSaveInstance=savedInstanceState.getBoolean("isFromSaveInstance");
			//			mActionBarController.restoreInstanceState(savedInstanceState);
			tabIndex=savedInstanceState.getInt("tabIndex");
			Log.d(TAG,"savedInstanceState-mInDialpadSearch:"+mInDialpadSearch+" mSearchQuery:"+mSearchQuery+" isFromSaveInstance:"+isFromSaveInstance
					+" tabIndex:"+tabIndex);
			//			if(isFromSaveInstance && mInDialpadSearch && !TextUtils.isEmpty(mSearchQuery)) {		
			//				toolbar.setVisibility(View.GONE);
			//			}
		}

		final boolean isLayoutRtl = DialerUtils.isRtl();
		if (mIsLandscape) {
			mSlideIn = AnimationUtils.loadAnimation(this,
					isLayoutRtl ? R.anim.dialpad_slide_in_left : R.anim.dialpad_slide_in_right);
			mSlideOut = AnimationUtils.loadAnimation(this,
					isLayoutRtl ? R.anim.dialpad_slide_out_left : R.anim.dialpad_slide_out_right);
		} else {
			mSlideIn = AnimationUtils.loadAnimation(this, R.anim.dialpad_slide_in_bottom);
			mSlideOut = AnimationUtils.loadAnimation(this, R.anim.dialpad_slide_out_bottom);
		}

		mSlideIn.setInterpolator(AnimUtils.EASE_IN);
		mSlideOut.setInterpolator(AnimUtils.EASE_OUT);

		mSlideIn.setAnimationListener(mSlideInListener);
		mSlideOut.setAnimationListener(mSlideOutListener);


		Trace.endSection();

		Trace.beginSection(TAG + " initialize smart dialing");

		/// M: [MTK Dialer Search] @{
		if (!DialerFeatureOptions.isDialerSearchEnabled()) {
			mDialerDatabaseHelper = DatabaseHelperManager.getDatabaseHelper(this);
			SmartDialPrefix.initializeNanpSettings(this);
		}
		/// @}

		actionMode = getActionMode();

		bottomBar = (BottomNavigationView) findViewById(R.id.bottom_navigation_view);
		bottomBar.setNavigationItemSelectedListener(new OnNavigationItemSelectedListener() {

			@Override
			public boolean onNavigationItemSelected(MenuItem arg0) {
				// TODO Auto-generated method stub
				Log.d(TAG, "onNavigationItemSelected,arg0.getItemId():" + arg0.getItemId());
				switch (arg0.getItemId()) {
				case R.id.hb_contacts_delete:
					int count = mListsFragment.getRecentsFragment().getAdapter().getCheckedCount();
					AlertDialog.Builder builder = new AlertDialog.Builder(DialtactsActivity.this);
					builder.setMessage(DialtactsActivity.this.getString(R.string.hb_delete_call_log_message, count));
					builder.setTitle(null);
					builder.setNegativeButton(DialtactsActivity.this.getString(R.string.hb_cancel), null);
					builder.setPositiveButton(DialtactsActivity.this.getString(R.string.hb_ok),
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							mListsFragment.getRecentsFragment().deleteSelectedCallLogs();
						}
					});
					AlertDialog alertDialog = builder.create();
					alertDialog.show();
					break;

				default:
					break;
				}
				return false;
			}
		});

		DialerApplication.isMultiSimEnabled(this);

		if(!com.android.contacts.common.HbUtils.isMTK) cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		//		onPrimaryClipChangedListener=new ClipboardManager.OnPrimaryClipChangedListener() {
		//			@Override
		//			public void onPrimaryClipChanged() {
		//				clipString=cb.getPrimaryClip().getItemAt(0).getText().toString();
		//				Log.d(TAG,"clip-onPrimaryClipChanged:"+clipString);
		//				handler.removeCallbacks(runnable3);
		//				handler.postDelayed(runnable3, 400);
		//			}
		//		};
		//		cb.setPrimaryClip(android.content.ClipData.newPlainText("", ""));
		//		cb.addPrimaryClipChangedListener(onPrimaryClipChangedListener);


		handler.postDelayed(runnable, 600);


		final SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean showDataUsagePrompt=mPrefs.getBoolean("showDataUsagePrompt", true);
		Log.d(TAG,"showDataUsagePrompt"+showDataUsagePrompt);
		if(!showDataUsagePrompt) handler.postDelayed(runnable2, 2000);

		Trace.endSection();
		Trace.endSection();
	}


	private Runnable runnable3=new Runnable() {

		@Override
		public void run() {
			Log.d(TAG,"clip-runnable3");
			String regEx="[0-9+\\-*,;]";
	    	Pattern p = Pattern.compile(regEx);
	    	Matcher m = p.matcher(clipString);
	    	int count=0;
	    	boolean result=false;
	    	while(m.find()) count++;
	    	if(count==clipString.length()) result=true;
	    	else result=false;
	    	Log.d(TAG,"clip-result:"+result);
	    	if(result) {
	    		mDialpadFragment.mDeleteImg.setImageResource(R.drawable.hb_ic_dialpad_delete_paste);
	    		mDialpadFragment.isForPaste=true;
	    	}
	    	else {
	    		mDialpadFragment.mDeleteImg.setImageResource(R.drawable.hb_ic_dialpad_delete_normal);
	    		mDialpadFragment.isForPaste=false;
	    	}
	    	mDialpadFragment.updateDeleteButtonEnabledState();
		}
	};
	public String clipString=null;
	//	ClipboardManager.OnPrimaryClipChangedListener onPrimaryClipChangedListener=null;
		ClipboardManager cb=null;
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		Log.d(TAG,"liyang2017-requestCode:"+requestCode+" permission:"+Arrays.toString(permissions)+" grantResults:"+Arrays.toString(grantResults));		

		if(grantResults==null ||grantResults.length<1) return;
		if(requestCode==PHONE_PERMISSION_REQUEST_CODE){
			boolean denied=false;
			for(int grantResult:grantResults) {
				if (grantResult==PackageManager.PERMISSION_DENIED) {
					denied=true;
					Toast.makeText(this, getString(R.string.hb_phone_permission_denied), Toast.LENGTH_LONG).show();
					break;
				}
				if(!denied)  {
					DialerApplication.isMultiSimEnabled(this);
					//					if(mDialpadFragment!=null) mDialpadFragment.isWfcEnabledByPlatform(this);

				}
			}
		} else if(requestCode==CONTACTS_PERMISSION_REQUEST_CODE) {
			boolean denied=false;
			for(int grantResult:grantResults) {
				if (grantResult==PackageManager.PERMISSION_DENIED) {
					denied=true;
					Toast.makeText(this, getString(R.string.hb_contacts_permission_denied), Toast.LENGTH_LONG).show();
					break;
				}
				if(!denied)  {
					if(mListsFragment!=null && mListsFragment.mAllContactsFragment!=null) mListsFragment.mAllContactsFragment.startLoading();
				}
			}
		} else if (requestCode==STORAGE_PERMISSION_REQUEST_CODE) {
			if(grantResults[0]==PackageManager.PERMISSION_DENIED) 
				Toast.makeText(this, getString(R.string.hb_storage_permission_denied), Toast.LENGTH_LONG).show();
		} else if (requestCode==LOCATION_PERMISSION_REQUEST_CODE) {
			if(grantResults[0]==PackageManager.PERMISSION_DENIED
					|| grantResults[1]==PackageManager.PERMISSION_DENIED) Toast.makeText(this, getString(R.string.hb_location_permission_denied), Toast.LENGTH_LONG).show();
			else {
				final SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
				boolean showDataUsagePrompt=mPrefs.getBoolean("showDataUsagePrompt", true);
				Log.d(TAG,"showDataUsagePrompt"+showDataUsagePrompt);
				if(!showDataUsagePrompt){
					handler.removeCallbacks(runnable2);
					handler.postDelayed(runnable2, 100);
				}
			}
		} else if (requestCode==SMS_PERMISSION_REQUEST_CODE) {
			if(grantResults[0]==PackageManager.PERMISSION_DENIED
					|| grantResults[1]==PackageManager.PERMISSION_DENIED) Toast.makeText(this, getString(R.string.hb_sms_permission_denied), Toast.LENGTH_LONG).show();
			else {
				handler.removeCallbacks(runnable2);
				handler.postDelayed(runnable2, 100);
			}
		} else if (requestCode==CAMERA_PERMISSION_REQUEST_CODE) {
			if(grantResults[0]==PackageManager.PERMISSION_DENIED) Toast.makeText(this, getString(R.string.hb_camera_permission_denied), Toast.LENGTH_LONG).show();
			else {
				handler.removeCallbacks(runnable2);
				handler.postDelayed(runnable2, 100);
			}
		}
	}



	private static final String CONTACT_READ_PERMISSION = "android.permission.READ_CONTACTS";
	private static final String CONTACT_WRITE_PERMISSION = "android.permission.WRITE_CONTACTS";
	private static final String CALLLOG_READ_PERMISSION = "android.permission.READ_CALL_LOG";
	private static final String CALLLOG_WRITE_PERMISSION = "android.permission.WRITE_CALL_LOG";
	private static final String CALL_PHONE_PERMISSION = "android.permission.CALL_PHONE";
	private static final String READ_PHONE_STATE_PERMISSION = "android.permission.READ_PHONE_STATE";
	private static final String WRITE_EXTERNAL_STORAGE="android.permission.WRITE_EXTERNAL_STORAGE";

	private static final String ACCESS_FINE_LOCATION="android.permission.ACCESS_FINE_LOCATION";
	private static final String ACCESS_COARSE_LOCATION="android.permission.ACCESS_COARSE_LOCATION";
	private static final String CAMERA="android.permission.CAMERA";
	private static final String RECEIVE_SMS="android.permission.RECEIVE_SMS";
	private static final String READ_SMS="android.permission.READ_SMS";


	private static final int PHONE_PERMISSION_REQUEST_CODE=0;
	private static final int CONTACTS_PERMISSION_REQUEST_CODE=1;
	private static final int STORAGE_PERMISSION_REQUEST_CODE=2;
	private static final int LOCATION_PERMISSION_REQUEST_CODE=3;
	private static final int CAMERA_PERMISSION_REQUEST_CODE=4;
	private static final int SMS_PERMISSION_REQUEST_CODE=5;
	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "onStart");


		boolean hasReadCalllogPermission = PermissionsUtil.hasPermission(this, CALLLOG_READ_PERMISSION);
		boolean hasWriteCalllogPermission = PermissionsUtil.hasPermission(this, CALLLOG_WRITE_PERMISSION);
		boolean hasCallPhonePermission = PermissionsUtil.hasPermission(this, CALL_PHONE_PERMISSION);
		boolean hasReadPhoneStatePermission = PermissionsUtil.hasPermission(this, READ_PHONE_STATE_PERMISSION);
		boolean hasReadContactPermission = PermissionsUtil.hasPermission(this, CONTACT_READ_PERMISSION);
		boolean hasWriteContactPermission = PermissionsUtil.hasPermission(this, CONTACT_WRITE_PERMISSION);
		//		boolean hasWriteExternalStoragePermission = PermissionsUtil.hasPermission(this, WRITE_EXTERNAL_STORAGE);

		Log.d(TAG, "liyang2017-hasReadContactPermission:"+hasReadContactPermission
				+" hasWriteContactPermission:"+hasWriteContactPermission
				+" hasReadCalllogPermission:"+hasReadCalllogPermission
				+" hasWriteCalllogPermission:"+hasWriteCalllogPermission
				+" hasCallPhonePermission:"+hasCallPhonePermission
				+" hasReadPhoneStatePermission:"+hasReadPhoneStatePermission
				//				+" hasWriteExternalStoragePermission:"+hasWriteExternalStoragePermission
				);

		//		boolean hasGetFineLocationPermission = PermissionsUtil.hasPermission(this, ACCESS_FINE_LOCATION);
		//		boolean hasGetCoarseLocationPermission = PermissionsUtil.hasPermission(this, ACCESS_COARSE_LOCATION);
		boolean hasReceiveSMSPermission = PermissionsUtil.hasPermission(this, RECEIVE_SMS);
		boolean hasReadSMSPermission = PermissionsUtil.hasPermission(this, READ_SMS);
		boolean hasCameraPermission = PermissionsUtil.hasPermission(this, CAMERA);					

		//		if(!hasGetFineLocationPermission 
		//				|| !hasGetCoarseLocationPermission) {
		//			requestPermissions(new String[] {ACCESS_FINE_LOCATION,
		//					ACCESS_COARSE_LOCATION},
		//					LOCATION_PERMISSION_REQUEST_CODE);
		//		}
		if(!hasReceiveSMSPermission 
				|| !hasReadSMSPermission) {
			requestPermissions(new String[] {RECEIVE_SMS,
					READ_SMS},
					SMS_PERMISSION_REQUEST_CODE);
		}
		if(!hasCameraPermission) {
			requestPermissions(new String[] {CAMERA},
					CAMERA_PERMISSION_REQUEST_CODE);
		}


		//		if(!hasWriteExternalStoragePermission) requestPermissions(new String[] {WRITE_EXTERNAL_STORAGE},
		//				STORAGE_PERMISSION_REQUEST_CODE);

		if(!hasReadContactPermission
				|| !hasWriteContactPermission) requestPermissions(new String[] {CONTACT_READ_PERMISSION, 
						CONTACT_WRITE_PERMISSION},
						CONTACTS_PERMISSION_REQUEST_CODE);

		if(!hasReadCalllogPermission 
				|| !hasWriteCalllogPermission 
				||!hasCallPhonePermission 
				||!hasReadPhoneStatePermission) requestPermissions(new String[] {CALLLOG_READ_PERMISSION, 
						CALLLOG_WRITE_PERMISSION,
						CALL_PHONE_PERMISSION,
						READ_PHONE_STATE_PERMISSION},
						PHONE_PERMISSION_REQUEST_CODE);

	}

	@Override
	protected void onStop(){
		super.onStop();
		Log.d(TAG, "onStop");
	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
		Log.d(TAG, "onDestroy");
		handler.removeCallbacksAndMessages(null);
		handler=null;
		
		//		cb.removePrimaryClipChangedListener(onPrimaryClipChangedListener);
		//		onPrimaryClipChangedListener=null;
	}

	private Handler handler=new Handler();
	/**
	 * Listen to broadcast events about permissions in order to be notified if the READ_CONTACTS
	 * permission is granted via the UI in another fragment.
	 */


	//add by liyang
	private ContactsHelper mContactsHelper;
	public void initT9Search(){
		//		ContactsHelper.getInstance().setContext(DialtactsActivity.this);

		if(mContactsHelper==null) {
			mContactsHelper=new ContactsHelper();
			mContactsHelper.setForDialpadT9(true);
		}
		mContactsHelper.setContext(DialtactsActivity.this);

		mContactsHelper.setOnContactsLoad(this);
		boolean startLoad = mContactsHelper.startLoadContacts(true);
	}

	@Override
	protected void onResume() {
		Trace.beginSection(TAG + " onResume");
		super.onResume();
		Log.d(TAG, "onResume,mFirstLaunch:"+mFirstLaunch);
		mStateSaved = false;
		if (mFirstLaunch) {
			displayFragment(getIntent());
			if(!com.android.contacts.common.HbUtils.isMTK) initT9Search();
		} else if (!phoneIsInUse() && mInCallDialpadUp) {
			hideDialpadFragment(false, true);
			mInCallDialpadUp = false;
		} else if (mShowDialpadOnResume) {
			showDialpadFragment(false);
			mShowDialpadOnResume = false;
		}

		if (mIsRestarting) {
			// This is only called when the activity goes from resumed -> paused
			// -> resumed, so it
			// will not cause an extra view to be sent out on rotation
			if (mIsDialpadShown) {
				AnalyticsUtil.sendScreenView(mDialpadFragment, this);
			}
			mIsRestarting = false;
		}

		// prepareVoiceSearchButton();

		/// M: [MTK Dialer Search] @{
		if (!DialerFeatureOptions.isDialerSearchEnabled()) {
			mDialerDatabaseHelper.startSmartDialUpdateThread();
		}
		/// @}

		//		mFloatingActionButtonController.align(getFabAlignment(), false /* animate */);

		Log.d(TAG, "intent:" + getIntent());

		if(mIsDialpadShown && indexFrom==ListsFragment.TAB_INDEX_ALL_CONTACTS) hideDialpadFragment(false, true);
		mFirstLaunch = false;
		Trace.endSection();

		if(tabIndex==ListsFragment.TAB_INDEX_YELLOWPAGE && !hasPermissionOfYellowPagePre) {
			handler.removeCallbacks(runnable2);
			handler.postDelayed(runnable2, 50);
		}



		if(!com.android.contacts.common.HbUtils.isMTK){
			if(cb!=null && cb.getPrimaryClip()!=null) clipString=cb.getPrimaryClip().getItemAt(0).getText().toString();
			final SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
			String clipStringPre=mPrefs.getString("clipStringPre", "");
			Log.d(TAG,"clipStringPre"+clipStringPre);			
			Log.d(TAG,"clip:"+clipString);
			
			if(!TextUtils.isEmpty(clipString) && !TextUtils.equals(clipString, clipStringPre) && TextUtils.isEmpty(mSearchQuery)){
				handler.removeCallbacks(runnable3);
				handler.postDelayed(runnable3, 300);
			} else {
				mDialpadFragment.mDeleteImg.setImageResource(R.drawable.hb_ic_dialpad_delete_normal);
				mDialpadFragment.isForPaste=false;
				mDialpadFragment.updateDeleteButtonEnabledState();
			}
			mPrefs.edit().putString("clipStringPre", clipString).apply();
		}
	}

	private void handleHbIntent(){
		Log.d(TAG,"handleHbIntent:"+getIntent()+" getIntent().getCategories():"+getIntent().getCategories());
		if(TextUtils.equals(getIntent().getAction(), "com.android.dialer.HbContactActivity")){
			indexFrom=ListsFragment.TAB_INDEX_ALL_CONTACTS;
			if(mListsFragment.mViewPager!=null) {
				Log.d(TAG,"handleHbIntent1");
				mListsFragment.mViewPager.setCurrentItem(ListsFragment.TAB_INDEX_ALL_CONTACTS, false);
				tabIndex=ListsFragment.TAB_INDEX_ALL_CONTACTS;
				mListsFragment.mViewPager.setScrollble(true);
			}
		}else{
			//			if (getIntent().getCategories() != null
			//					&& getIntent().getCategories().contains("android.intent.category.LAUNCHER")) {
			if (TextUtils.equals(getIntent().getComponent().getClassName(), "com.android.dialer.HbContactActivity")) {
				indexFrom=ListsFragment.TAB_INDEX_ALL_CONTACTS;
				if(mListsFragment.mViewPager!=null) {
					Log.d(TAG,"handleHbIntent2");
					mListsFragment.mViewPager.setScrollble(true);
					mListsFragment.mViewPager.setCurrentItem(ListsFragment.TAB_INDEX_ALL_CONTACTS, false);
					tabIndex=ListsFragment.TAB_INDEX_ALL_CONTACTS;
					if(mListsFragment.mRecentsFragment!=null && mListsFragment.mRecentsFragment.isEditMode) 
						mListsFragment.mRecentsFragment.switchToEditMode(false);
				}
			} else if (TextUtils.equals(getIntent().getComponent().getClassName(),
					"com.android.dialer.DialtactsActivity")) {
				indexFrom=ListsFragment.TAB_INDEX_RECENTS;
				if(mListsFragment.mViewPager!=null) {
					Log.d(TAG,"handleHbIntent3");
					mListsFragment.mViewPager.setScrollble(true);
					mListsFragment.mViewPager.setCurrentItem(ListsFragment.TAB_INDEX_RECENTS, false);
					tabIndex=ListsFragment.TAB_INDEX_RECENTS;
				}
				Log.d(TAG,"mListsFragment.mAllContactsFragment:"+mListsFragment.mAllContactsFragment);
				if(mListsFragment.mAllContactsFragment!=null && mListsFragment.mAllContactsFragment.mHbIsSearchMode) {
					Log.d(TAG,"mListsFragment.mAllContactsFragment.isSearchMode():"+mListsFragment.mAllContactsFragment.isSearchMode());
					mListsFragment.mAllContactsFragment.exitSearchMode(false);
					if(floatingActionButton.getVisibility()!=View.GONE) floatingActionButton.setVisibility(View.GONE);
				}
			}
			//				getIntent().removeCategory("android.intent.category.LAUNCHER");
			//			}
		}
	}

	public int indexFrom;

	@Override
	protected void onRestart() {
		super.onRestart();
		mIsRestarting = true;
	}

	@Override
	protected void onPause() {
		if (mClearSearchOnPause) {
			hideDialpadAndSearchUi();
			mClearSearchOnPause = false;
		}
		/// M: [Call Account Notification] Hide the call account selection
		/// notification
		// CallAccountSelectionNotificationUtil.getInstance(this).showNotification(false,
		/// this);
		if (mSlideOut.hasStarted() && !mSlideOut.hasEnded()) {
			commitDialpadFragmentHide();
		}
		/// M: WFC <To remove WFC notification from status bar > @{
		//		if (ImsManager.isWfcEnabledByUser(this) && (mDialpadFragment != null)) {
		//			mDialpadFragment.stopWfcNotification();
		//		}
		/// @}
		super.onPause();
	}

	private boolean isFromSaveInstance=false;
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_SEARCH_QUERY, mSearchQuery);
		outState.putBoolean(KEY_IN_REGULAR_SEARCH_UI, mInRegularSearch);
		outState.putBoolean(KEY_IN_DIALPAD_SEARCH_UI, mInDialpadSearch);
		outState.putBoolean(KEY_FIRST_LAUNCH, mFirstLaunch);
		outState.putBoolean(KEY_IS_DIALPAD_SHOWN, mIsDialpadShown);
		/// M: Save and restore the mPendingSearchViewQuery
		outState.putString(KEY_PENDING_SEARCH_QUERY, mPendingSearchViewQuery);
		outState.putBoolean("isFromSaveInstance", true);
		outState.putInt("tabIndex", tabIndex);
		// mActionBarController.saveInstanceState(outState);
		mStateSaved = true;
	}

	@Override
	public void onAttachFragment(Fragment fragment) {
		Log.d(TAG, "onAttachFragment:" + fragment);
		if (fragment instanceof DialpadFragment) {
			mDialpadFragment = (DialpadFragment) fragment;
			if (!mIsDialpadShown && !mShowDialpadOnResume) {
				final FragmentTransaction transaction = getFragmentManager().beginTransaction();
				transaction.hide(mDialpadFragment);
				transaction.commit();
			}
		} else if (fragment instanceof SmartDialSearchFragment) {
			mSmartDialSearchFragment = (SmartDialSearchFragment) fragment;
			mSmartDialSearchFragment.setOnPhoneNumberPickerActionListener(this);
			mSmartDialSearchFragment.setContactsHelper(mContactsHelper);
		} else if (fragment instanceof SearchFragment) {
			mRegularSearchFragment = (RegularSearchFragment) fragment;
			mRegularSearchFragment.setOnPhoneNumberPickerActionListener(this);
		} else if (fragment instanceof ListsFragment) {
			mListsFragment = (ListsFragment) fragment;
			mListsFragment.setOnTabSelectedListener(this);		
			// mListsFragment.addOnPageChangeListener(this);
		}
	}

	public void handleMenuSettings() {
		//		if(true) {
		//			String number="13";
		//			Cursor cursor=getContentResolver().query(Uri.parse("content://com.android.contacts/contacts/filter/"+number+"?directory=0&deferred_snippeting=1"), 
		//					null, null, null, null);
		//			Log.d(TAG,"cursor:"+cursor+" count:"+(cursor==null?"0":cursor.getCount()));
		//			return;
		//		}
		// modify by lgy
		final Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setClassName("com.android.phone", "com.hb.settings.HbCallSettings");
		startActivity(intent);
	}

	@Override
	public void onClick(View view) {
		Log.d(TAG, "onclick:" + view);
		switch (view.getId()) {
		default: {
			Log.wtf(TAG, "Unexpected onClick event from " + view);
			break;
		}
		}
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.hb_menu_settings:
			handleMenuSettings();
			break;

		case R.id.hb_menu_group:
			startActivity(new Intent("com.hb.action.group"));
			break;
			/*
			 * case R.id.menu_history: // Use explicit CallLogActivity intent
			 * instead of ACTION_VIEW + // CONTENT_TYPE, so that we always open our
			 * call log from our dialer final Intent intent = new Intent(this,
			 * CallLogActivity.class); startActivity(intent); break; case
			 * R.id.menu_add_contact: DialerUtils.startActivityWithErrorToast( this,
			 * IntentUtil.getNewContactIntent(),
			 * R.string.add_contact_not_available); break; case
			 * R.id.menu_import_export: // We hard-code the "contactsAreAvailable"
			 * argument because doing it properly would // involve querying a {@link
			 * ProviderStatusLoader}, which we don't want to do right // now in
			 * Dialtacts for (potential) performance reasons. Compare with how it is
			 * // done in {@link PeopleActivity}.
			 *//**
			 * M: When it is A1 project,use Google import/export function or use
			 * MTK. @{
			 */
			/*
			 * if (DialerFeatureOptions.isA1ProjectEnabled()) {
			 * ImportExportDialogFragment.show(getFragmentManager(), true,
			 * DialtactsActivity.class); } else { final Intent importIntent = new
			 * Intent( ContactsIntent.LIST.ACTION_IMPORTEXPORT_CONTACTS);
			 * importIntent.putExtra(VCardCommonArguments.ARG_CALLING_ACTIVITY,
			 * DialtactsActivity.class.getNaonResumeme()); try {
			 * startActivityForResult(importIntent, IMPORT_EXPORT_REQUEST_CODE); }
			 * catch (ActivityNotFoundException ex) {
			 * ImportExportDialogFragment.show(getFragmentManager(), true,
			 * DialtactsActivity.class); } }
			 *//** @} */
			/*
			 * return true; case R.id.menu_clear_frequents:
			 * ClearFrequentsDialog.show(getFragmentManager()); return true; case
			 * R.id.menu_call_settings: handleMenuSettings(); return true;
			 *//** M: [VoLTE ConfCall] handle conference call menu. @{ */
			/*
			 * case R.id.menu_volte_conf_call:
			 * DialerVolteUtils.handleMenuVolteConfCall(this); return true;
			 *//** @} */
		}
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ACTIVITY_REQUEST_CODE_VOICE_SEARCH) {
			if (resultCode == RESULT_OK) {
				final ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				if (matches.size() > 0) {
					final String match = matches.get(0);
					mVoiceSearchQuery = match;
				} else {
					Log.e(TAG, "Voice search - nothing heard");
				}
			} else {
				Log.e(TAG, "Voice search failed");
			}
		}
		/** M: [VoLTE ConfCall] Handle the volte conference call. @{ */
		else if (requestCode == DialerVolteUtils.ACTIVITY_REQUEST_CODE_PICK_PHONE_CONTACTS) {
			if (resultCode == RESULT_OK) {
				DialerVolteUtils.launchVolteConfCall(this, data);
			} else {
				Log.d(TAG, "No contacts picked, Volte conference call cancelled.");
			}
		}
		/** @} */
		/** M: [Import/Export] Handle the import/export activity result. @{ */
		else if (requestCode == IMPORT_EXPORT_REQUEST_CODE) {
			if (resultCode == RESULT_CANCELED) {
				Log.d(TAG, "Import/Export activity create failed! ");
			} else {
				Log.d(TAG, "Import/Export activity create successfully! ");
			}
		}
		/** @} */

		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Initiates a fragment transaction to show the dialpad fragment. Animations
	 * and other visual updates are handled by a callback which is invoked after
	 * the dialpad fragment is shown.
	 * 
	 * @see #onDialpadShown
	 */
	public void showDialpadFragment(boolean animate) {
		Log.d(TAG, "showDialpadFragment,mIsDialpadShown:" + mIsDialpadShown + " animate:" + animate);
		if (mIsDialpadShown || mStateSaved) {
			return;
		}

		mListsFragment.setUserVisibleHint(false);

		final FragmentTransaction ft = getFragmentManager().beginTransaction();
		if (mDialpadFragment == null) {
			mDialpadFragment = new DialpadFragment();
			ft.add(R.id.dialtacts_container, mDialpadFragment, TAG_DIALPAD_FRAGMENT);
		} else {
			ft.show(mDialpadFragment);
		}

		mDialpadFragment.setAnimate(animate);
		AnalyticsUtil.sendScreenView(mDialpadFragment);
		ft.commit();

		if (/*animate*/false) {
			mFloatingActionButtonController.scaleOut();
		} else {
			mFloatingActionButtonController.setVisible(false);
		}

		hideDialpadByFoldbutton = false;

		mIsDialpadShown = true;
		Log.d(TAG, "end");
	}

	/**
	 * Callback from child DialpadFragment when the dialpad is shown.
	 */
	public void onDialpadShown() {
		Assert.assertNotNull(mDialpadFragment);
		if (mDialpadFragment.getAnimate()) {
			mDialpadFragment.getView().startAnimation(mSlideIn);
		} else {
			mDialpadFragment.setYFraction(0);
		}

		updateSearchFragmentPosition();
		Log.d(TAG,"onDialpadShown:"+floatingActionButton.getVisibility());
		if(floatingActionButton.getVisibility()!=View.GONE) floatingActionButton.setVisibility(View.GONE);
	}

	/**
	 * Initiates animations and other visual updates to hide the dialpad. The
	 * fragment is hidden in a callback after the hide animation ends.
	 * 
	 * @see #commitDialpadFragmentHide
	 */
	public void hideDialpadFragment(boolean animate, boolean clearDialpad) {
		Log.d(TAG, "hideDialpadFragment,mIsDialpadShown:" + mIsDialpadShown);
		if (mDialpadFragment == null || mDialpadFragment.getView() == null) {
			return;
		}
		if (clearDialpad) {
			mDialpadFragment.clearDialpad();
		}
		if (!mIsDialpadShown) {
			return;
		}

		mDialpadFragment.setAnimate(animate);
		mListsFragment.setUserVisibleHint(true);
		mListsFragment.sendScreenViewForCurrentPosition();

		updateSearchFragmentPosition();

		// mFloatingActionButtonController.align(getFabAlignment(), animate);
		if (animate) {
			mDialpadFragment.getView().startAnimation(mSlideOut);
		} else {
			commitDialpadFragmentHide();
		}

		// mActionBarController.onDialpadDown();

		if (isInSearchUi()) {
			if (TextUtils.isEmpty(mSearchQuery)) {
				exitSearchUi();
			}
			if (clearDialpad || TextUtils.isEmpty(mSearchQuery)) {
				setDigitsState(false);
			}
		}

		if(tabIndex!=2){
			if(listener==null) listener = (new AnimatorListenerAdapter() {
				@Override
				public void onAnimationStart(Animator animation) {
					floatingActionButton.setVisibility(View.VISIBLE);
				}

				@Override
				public void onAnimationCancel(Animator animation) {
					floatingActionButton.setScaleX(1);
					floatingActionButton.setScaleY(1);
				}
				@Override
				public void onAnimationEnd(Animator animation) {
					//				Log.d(TAG,"onAnimationEnd,tabIndex:"+tabIndex);
					//				if(tabIndex==0&&mIsDialpadShown&&floatingActionButton.getVisibility()!=View.GONE) floatingActionButton.setVisibility(View.GONE);
				}

			});
			//		mFloatingActionButtonController.scaleIn(FAB_SCALE_IN_DELAY_MS);
			animator = floatingActionButton.animate();
			mFloatingActionButtonController.scaleInHb(FAB_SCALE_IN_DELAY_MS,listener,animator);
		}else if(floatingActionButton.getVisibility()!=View.GONE) floatingActionButton.setVisibility(View.GONE);

		//		if (ImsManager.isWfcEnabledByUser(this) && (mDialpadFragment != null)) {
		//			mDialpadFragment.stopWfcNotification();
		//		}

		mIsDialpadShown = false;
	}
	ViewPropertyAnimator animator;
	AnimatorListenerAdapter listener;

	/**
	 * Finishes hiding the dialpad fragment after any animations are completed.
	 */
	private void commitDialpadFragmentHide() {
		if (!mStateSaved && mDialpadFragment != null && !mDialpadFragment.isHidden()) {
			final FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.hide(mDialpadFragment);
			ft.commit();
		}
		// mFloatingActionButtonController.scaleIn(AnimUtils.NO_DELAY);
		// mFloatingActionButtonController.setVisible(true);
	}

	private void updateSearchFragmentPosition() {
		SearchFragment fragment = null;
		if (mSmartDialSearchFragment != null && mSmartDialSearchFragment.isVisible()) {
			fragment = mSmartDialSearchFragment;
		} else if (mRegularSearchFragment != null && mRegularSearchFragment.isVisible()) {
			fragment = mRegularSearchFragment;
		}
		if (fragment != null && fragment.isVisible()) {
			fragment.updatePosition(false /* animate */);
		}
	}

	@Override
	public boolean isInSearchUi() {
		return mInDialpadSearch || mInRegularSearch;
	}

	@Override
	public boolean hasSearchQuery() {
		return !TextUtils.isEmpty(mSearchQuery);
	}

	@Override
	public boolean shouldShowActionBar() {
		return mListsFragment.shouldShowActionBar();
	}

	private void setNotInSearchUi() {
		mInDialpadSearch = false;
		mInRegularSearch = false;
	}

	private void hideDialpadAndSearchUi() {
		if (mIsDialpadShown) {
			hideDialpadFragment(false, true);
		} else {
			exitSearchUi();
		}
	}

	// private void prepareVoiceSearchButton() {
	// final Intent voiceIntent = new
	// Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
	// /**
	// * M: [ALPS02227737] set value for view to record the voice search
	// * button status @{
	// */
	// boolean canBeHandled = canIntentBeHandled(voiceIntent);
	// SearchEditTextLayout searchBox = (SearchEditTextLayout)
	// getActionBar().getCustomView();
	// if (searchBox != null) {
	// searchBox.setCanHandleSpeech(canBeHandled);
	// }
	// /** @} */
	// if (canBeHandled) {
	// mVoiceSearchButton.setVisibility(View.VISIBLE);
	// mVoiceSearchButton.setOnClickListener(this);
	// } else {
	// mVoiceSearchButton.setVisibility(View.GONE);
	// }
	// }

	// protected OptionsPopupMenu buildOptionsMenu(View invoker) {
	// /** M: [VoLTE ConfCall] Show conference call menu for volte. @{ */
	// final OptionsPopupMenu popupMenu = new OptionsPopupMenu(this, invoker) {
	// @Override
	// public void show() {
	// boolean visible = DialerVolteUtils
	// .isVolteConfCallEnable(DialtactsActivity.this);
	// getMenu().findItem(R.id.menu_volte_conf_call).setVisible(visible);
	// super.show();
	// }
	// };
	// /** @} */
	// popupMenu.inflate(R.menu.dialtacts_options);
	//
	// /// M: add for plug-in. @{
	// final Menu menu = popupMenu.getMenu();
	// ExtensionManager.getInstance().getDialPadExtension().buildOptionsMenu(this,
	// menu);
	// /// @}
	//
	// popupMenu.setOnMenuItemClickListener(this);
	// return popupMenu;
	// }

	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// /** M: Modify to set the pending search query only when dialpad is
	// visible. @{ */
	// if (mPendingSearchViewQuery != null
	// && mDialpadFragment != null && mDialpadFragment.isVisible()) {
	// mSearchView.setText(mPendingSearchViewQuery);
	// mPendingSearchViewQuery = null;
	// }
	// /** @} */
	//// if (mActionBarController != null) {
	//// mActionBarController.restoreActionBarOffset();
	//// }
	// return false;
	// }

	/**
	 * Returns true if the intent is due to hitting the green send key (hardware
	 * call button: KEYCODE_CALL) while in a call.
	 *
	 * @param intent
	 *            the intent that launched this activity
	 * @return true if the intent is due to hitting the green send key while in
	 *         a call
	 */
	private boolean isSendKeyWhileInCall(Intent intent) {
		Log.d(TAG,"isSendKeyWhileInCall");
		// If there is a call in progress and the user launched the dialer by
		// hitting the call
		// button, go straight to the in-call screen.
		final boolean callKey = Intent.ACTION_CALL_BUTTON.equals(intent.getAction());

		if (callKey) {
			getTelecomManager().showInCallScreen(false);
			return true;
		}

		return false;
	}

	/**
	 * Sets the current tab based on the intent's request type
	 *
	 * @param intent
	 *            Intent that contains information about which tab should be
	 *            selected
	 */
	private void displayFragment(Intent intent) {
		Log.d(TAG, "displayFragment:"+!TextUtils.equals(intent.getAction(), "com.android.dialer.HbContactActivity"));

		if (TextUtils.equals(intent.getComponent().getClassName(), "com.android.dialer.HbContactActivity")) return;

		if (isPhoneInUse()) {
			if(!TextUtils.equals(intent.getAction(), "com.android.dialer.HbContactActivity")){
				getTelecomManager().showInCallScreen(false);
				finish();
				return;
			} else return;
		}


		// If we got here by hitting send and we're in call forward along to the
		// in-call activity
		if (isSendKeyWhileInCall(intent)) {
			finish();
			return;
		}



		final boolean phoneIsInUse = phoneIsInUse();
		if (phoneIsInUse || (intent.getData() != null && isDialIntent(intent))) {
			showDialpadFragment(false);
			mDialpadFragment.setStartedFromNewIntent(true);
			if (phoneIsInUse && !mDialpadFragment.isVisible()) {
				mInCallDialpadUp = true;
			}
		}
	}

	/**
	 * @return true if the phone is "in use", meaning that at least one line
	 *              is active (ie. off hook or ringing or dialing, or on hold).
	 */
	public boolean isPhoneInUse() {
		return getTelecomManager().isInCall();
	}

	@Override
	public void onNewIntent(Intent newIntent) {
		Log.d(TAG,"onNewIntent");
		setIntent(newIntent);
		mStateSaved = false;

		invalidateOptionsMenu();

		handleHbIntent();

		displayFragment(newIntent);

		Log.d(TAG,"indexFrom1:"+indexFrom);

		handler.postDelayed(runnable, 500);
	}


	private Runnable runnable=new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Log.d(TAG,"runnable tid:"+android.os.Process.myTid());
			try{
				Context otherContext = createPackageContext("com.android.contacts", Context.CONTEXT_IGNORE_SECURITY);
				if(otherContext!=null){
					SharedPreferences sharedPreferences = otherContext.getSharedPreferences(
							"com.android.contacts_hb", Context.MODE_WORLD_READABLE|Context.MODE_MULTI_PROCESS);

					boolean is_exporting_or_importing=sharedPreferences.getBoolean("is_exporting_or_importing", false);
					Log.d(TAG,"is_exporting_or_importing:"+is_exporting_or_importing);
					if(is_exporting_or_importing) {
						Intent intent=new Intent("android.intent.action.contacts.HbContactImportExportActivity");
						intent.putExtra("from", "DialtactsActivity");
						startActivity(intent);
					}else{
						Context otherContext2 = createPackageContext("com.android.providers.contacts", Context.CONTEXT_IGNORE_SECURITY);
						if(otherContext2!=null){
							SharedPreferences sharedPreferences2 = otherContext2.getSharedPreferences(
									"com.android.providers.contacts_preferences_hb", Context.MODE_WORLD_READABLE|Context.MODE_MULTI_PROCESS);
							boolean is_updating_index_for_localechange=sharedPreferences2.getBoolean("is_updating_index_for_localechange", false);
							Log.d(TAG,"is_updating_index_for_localechange:"+is_updating_index_for_localechange);
							if(is_updating_index_for_localechange) Toast.makeText(DialtactsActivity.this, getString(R.string.hb_rebuild_index_toast), Toast.LENGTH_LONG).show();
						}
					}
				}
			}catch(Exception e){
				Log.d(TAG,"e:"+e);
			}
		}
	};

	private boolean hasChangeTabIndex=false;
	private Runnable runnable2=new Runnable() {

		@Override
		public void run() {
			if(isMonkeyRunning()) {
				if(mListsFragment!=null && mListsFragment.mYellowPageFragment!=null) mListsFragment.mYellowPageFragment.removeView();
				return;
			}

			if(!hasChangeTabIndex 
					&& (!TextUtils.isEmpty(mSearchQuery) && mIsDialpadShown)
					&& mListsFragment!=null 
					&& mListsFragment.mYellowPageFragment!=null 
					&& mListsFragment.mYellowPageFragment.loadState==-1) {
				Log.d(TAG,"liyang2017-dialing,do not loading yellowpage background.");
				handler.removeCallbacks(runnable2);
				handler.postDelayed(runnable2, 3000);
				return;
			}
			boolean mHasPermission=checkPermissionOfYellowPage();
			if((mListsFragment!=null && mListsFragment.mYellowPageFragment!=null 
					&& mListsFragment.mYellowPageFragment.loadState==-1)
					|| (!hasPermissionOfYellowPagePre && mHasPermission)) {
				Log.d(TAG,"liyang2017-runnable2 tid:"+android.os.Process.myTid());
				hasPermissionOfYellowPagePre=checkPermissionOfYellowPage();
				mListsFragment.mYellowPageFragment.realLoad();
			}
		}
	};

	/**
	 * Returns true if the given intent contains a phone number to populate the
	 * dialer with
	 */
	private boolean isDialIntent(Intent intent) {
		final String action = intent.getAction();
		if (Intent.ACTION_DIAL.equals(action) || ACTION_TOUCH_DIALER.equals(action)) {
			return true;
		}
		if (Intent.ACTION_VIEW.equals(action)) {
			final Uri data = intent.getData();
			if (data != null && PhoneAccount.SCHEME_TEL.equals(data.getScheme())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns an appropriate call origin for this Activity. May return null
	 * when no call origin should be used (e.g. when some 3rd party application
	 * launched the screen. Call origin is for remembering the tab in which the
	 * user made a phone call, so the external app's DIAL request should not be
	 * counted.)
	 */
	public String getCallOrigin() {
		return !isDialIntent(getIntent()) ? CALL_ORIGIN_DIALTACTS : null;
	}

	private boolean isSmartFragmentAdded;

	/**
	 * Shows the search fragment
	 */
	private void enterSearchUi(boolean smartDialSearch, String query, boolean animate) {
		if (mStateSaved || getFragmentManager().isDestroyed() || isSmartFragmentAdded) {
			// Weird race condition where fragment is doing work after the
			// activity is destroyed
			// due to talkback being on (b/10209937). Just return since we can't
			// do any
			// constructive here.
			return;
		}

		if (toolbar.getVisibility() != View.GONE){
			toolbar.setVisibility(View.GONE);
		}
		if (DEBUG) {
			Log.d(TAG, "Entering search UI - smart dial " + smartDialSearch);
		}

		final FragmentTransaction transaction = getFragmentManager().beginTransaction();
		if (mInDialpadSearch && mSmartDialSearchFragment != null) {
			transaction.remove(mSmartDialSearchFragment);
		} else if (mInRegularSearch && mRegularSearchFragment != null) {
			transaction.remove(mRegularSearchFragment);
		}

		final String tag;
		if (smartDialSearch) {
			tag = TAG_SMARTDIAL_SEARCH_FRAGMENT;
		} else {
			tag = TAG_REGULAR_SEARCH_FRAGMENT;
		}
		mInDialpadSearch = smartDialSearch;
		mInRegularSearch = !smartDialSearch;

		mFloatingActionButtonController.scaleOut();

		SearchFragment fragment = isFromSaveInstance?null:(SearchFragment) getFragmentManager().findFragmentByTag(tag);
		if (animate) {
			transaction.setCustomAnimations(android.R.animator.fade_in, 0);
		} else {
			transaction.setTransition(FragmentTransaction.TRANSIT_NONE);
		}

		/// M: If switch to a new fragment, it need to set query string to this
		// fragment, otherwise the query result would show nothing. @{
		boolean needToSetQuery = false;
		Log.d(TAG, "fragment:" + fragment);
		if (fragment == null) {
			needToSetQuery = true;
			if (smartDialSearch) {
				// fragment = new SmartDialSearchFragment();

				// digits_container.measure(0, 0);
				// int listViewTranslationYHeight =
				// digits_container.getMeasuredHeight();
				// Log.d(TAG,"listViewTranslationYHeight:"+listViewTranslationYHeight);
				fragment = new SmartDialSearchFragment();
				fragment.setForDialerSearch(true);

			} else {
				fragment = new RegularSearchFragment();
				fragment.setOnTouchListener(new View.OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						// Show the FAB when the user touches the lists fragment
						// and the soft
						// keyboard is hidden.
						showFabInSearchUi();
						return false;
					}
				});
			}
			transaction.add(R.id.dialtacts_frame, fragment, tag);
			fragment.setForDialerSearch(true);
		} else {
			transaction.show(fragment);
		}
		// DialtactsActivity will provide the options menu
		fragment.setHasOptionsMenu(false);
		fragment.setShowEmptyListForNullQuery(true);
		// if (!smartDialSearch || needToSetQuery) {
		// fragment.setQueryString(query, false /* delaySelection */);
		// }
		// @}
		transaction.commit();

		// if (animate) {
		// mListsFragment.getView().animate().alpha(0).withLayer();
		// }
		mListsFragment.setUserVisibleHint(false);
		isSmartFragmentAdded = true;
	}

	/**
	 * Hides the search fragment
	 */
	private void exitSearchUi() {
		Log.d(TAG, "exitsearchui");
		// See related bug in enterSearchUI();
		if (getFragmentManager().isDestroyed() || mStateSaved) {
			return;
		}

		mDialpadFragment.clearDialpad();
		setDigitsState(false);
		if (toolbar.getVisibility() != View.VISIBLE){
			toolbar.setVisibility(View.VISIBLE);
		}
		// mSearchView.setText(null);

		if (mDialpadFragment != null) {
			mDialpadFragment.clearDialpad();
		}

		setNotInSearchUi();

		// Restore the FAB for the lists fragment.
		// if (getFabAlignment() != FloatingActionButtonController.ALIGN_END) {
		// mFloatingActionButtonController.setVisible(false);
		// }
		// mFloatingActionButtonController.scaleIn(FAB_SCALE_IN_DELAY_MS);
		// onPageScrolled(/*mListsFragment.getCurrentTabIndex()*/tabIndexPre, 0
		// /* offset */, 0 /* pixelOffset */);
		// onPageSelected(/*mListsFragment.getCurrentTabIndex()*/tabIndexPre);

		final FragmentTransaction transaction = getFragmentManager().beginTransaction();
		if (mSmartDialSearchFragment != null) {
			transaction.remove(mSmartDialSearchFragment);
		}
		if (mRegularSearchFragment != null) {
			transaction.remove(mRegularSearchFragment);
		}
		isSmartFragmentAdded = false;
		transaction.commit();

		// if(!mListsFragment.isVisible())
		// mListsFragment.getView().setVisibility(View.VISIBLE);
		// mListsFragment.getView().animate().alpha(1).withLayer();

		if (mDialpadFragment == null || !mDialpadFragment.isVisible()) {
			// If the dialpad fragment wasn't previously visible, then send a
			// screen view because
			// we are exiting regular search. Otherwise, the screen view will be
			// sent by
			// {@link #hideDialpadFragment}.
			mListsFragment.sendScreenViewForCurrentPosition();
			mListsFragment.setUserVisibleHint(true);
		}

		// mActionBarController.onSearchUiExited();
	}

	@Override
	public void onBackPressed() {
		Log.d(TAG, "onbackpress,mIsDialpadShown:" + mIsDialpadShown + " editMode:"
				+ mListsFragment.getRecentsFragment().mAdapter.getEditMode());
		if (mStateSaved) {
			return;
		}

		if (mListsFragment.mAllContactsFragment.mHbIsSearchMode) {
			mListsFragment.mAllContactsFragment.exitSearchMode(false);
			return;
		}

		if (mListsFragment.getRecentsFragment().mAdapter.getEditMode()) {
			mListsFragment.getRecentsFragment().switchToEditMode(false);
			return;
		}

		if (TextUtils.isEmpty(mSearchQuery)) {
			super.onBackPressed();
			return;
		}

		if (mIsDialpadShown) {
			if (TextUtils.isEmpty(mSearchQuery) || (mSmartDialSearchFragment != null
					&& mSmartDialSearchFragment.isVisible() && mSmartDialSearchFragment.getAdapter().getCount() == 0)) {
				exitSearchUi();
			}
			hideDialpadFragment(true, false);
		} else if (isInSearchUi()) {
			exitSearchUi();
			//			DialerUtils.hideInputMethod(mParentLayout);
		} else {
			super.onBackPressed();
		}
	}

	private void maybeEnterSearchUi() {
		Log.d(TAG, "maybeEnterSearchUi");
		if (toolbar.getVisibility() != View.GONE){
			toolbar.setVisibility(View.GONE);
		}
		if (!isInSearchUi() ||isFromSaveInstance) {
			enterSearchUi(true /* isSmartDial */, mSearchQuery, false);
		}
	}

	/**
	 * @return True if the search UI was exited, false otherwise
	 */
	private boolean maybeExitSearchUi() {
		if (isInSearchUi() && TextUtils.isEmpty(mSearchQuery)) {
			exitSearchUi();
			//			DialerUtils.hideInputMethod(mParentLayout);
			return true;
		}
		return false;
	}

	private void showFabInSearchUi() {
		mFloatingActionButtonController.changeIcon(getResources().getDrawable(R.drawable.fab_ic_dial),
				getResources().getString(R.string.action_menu_dialpad_button));
		mFloatingActionButtonController.align(getFabAlignment(), false /* animate */);
		mFloatingActionButtonController.scaleIn(FAB_SCALE_IN_DELAY_MS);
	}

	@Override
	public void onDialpadQueryChanged(String query) {
		if (mSmartDialSearchFragment != null) {
			mSmartDialSearchFragment.setAddToContactNumber(query);
		}
		final String normalizedQuery = SmartDialNameMatcher.normalizeNumber(query,
				/* M: [MTK Dialer Search] use mtk enhance dialpad map */
				DialerFeatureOptions.isDialerSearchEnabled() ? SmartDialNameMatcher.SMART_DIALPAD_MAP
						: SmartDialNameMatcher.LATIN_SMART_DIAL_MAP);

		if (!TextUtils.equals(mDigitsEditText.getText(), normalizedQuery)) {
			if (DEBUG) {
				Log.d(TAG, "onDialpadQueryChanged - new query: " + query);
			}
			if (mDialpadFragment == null || !mDialpadFragment.isVisible()) {
				// This callback can happen if the dialpad fragment is recreated
				// because of
				// activity destruction. In that case, don't update the search
				// view because
				// that would bring the user back to the search fragment
				// regardless of the
				// previous state of the application. Instead, just return here
				// and let the
				// fragment manager correctly figure out whatever fragment was
				// last displayed.
				if (!TextUtils.isEmpty(normalizedQuery)) {
					mPendingSearchViewQuery = normalizedQuery;
				}
				return;
			}
		}

		try {
			if (mDialpadFragment != null && mDialpadFragment.isVisible()) {
				mDialpadFragment.process_quote_emergency_unquote(normalizedQuery);
			}
		} catch (Exception ignored) {
			// Skip any exceptions for this piece of code
		}
	}

	@Override
	public boolean onDialpadSpacerTouchWithEmptyQuery() {
		Log.d(TAG, "onDialpadSpacerTouchWithEmptyQuery");
		if (mInDialpadSearch && mSmartDialSearchFragment != null
				&& !mSmartDialSearchFragment.isShowingPermissionRequest()) {
			hideDialpadFragment(true /* animate */, true /* clearDialpad */);
			return true;
		}
		return false;
	}

	@Override
	public void onListFragmentScrollStateChange(int scrollState) {
		Log.d(TAG, "onListFragmentScrollStateChange,scrollState:" + scrollState);
		if (scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
			hideDialpadFragment(true, false);
			//			DialerUtils.hideInputMethod(mParentLayout);
		}
	}

	@Override
	public void onListFragmentScroll(int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// TODO: No-op for now. This should eventually show/hide the actionBar
		// based on
		// interactions with the ListsFragments.
	}

	private boolean phoneIsInUse() {
		return getTelecomManager().isInCall();
	}

	private boolean canIntentBeHandled(Intent intent) {
		final PackageManager packageManager = getPackageManager();
		final List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		return resolveInfo != null && resolveInfo.size() > 0;
	}

	/**
	 * Called when the user has long-pressed a contact tile to start a drag
	 * operation.
	 */
	@Override
	public void onDragStarted(int x, int y, PhoneFavoriteSquareTileView view) {
		mListsFragment.showRemoveView(true);
	}

	@Override
	public void onDragHovered(int x, int y, PhoneFavoriteSquareTileView view) {
	}

	/**
	 * Called when the user has released a contact tile after long-pressing it.
	 */
	@Override
	public void onDragFinished(int x, int y) {
		mListsFragment.showRemoveView(false);
	}

	@Override
	public void onDroppedOnRemove() {
	}

	/**
	 * Allows the SpeedDialFragment to attach the drag controller to
	 * mRemoveViewContainer once it has been attached to the activity.
	 */
	@Override
	public void setDragDropController(DragDropController dragController) {
		mDragDropController = dragController;
		mListsFragment.getRemoveView().setDragDropController(dragController);
	}

	/**
	 * Implemented to satisfy {@link SpeedDialFragment.HostInterface}
	 */
	@Override
	public void showAllContactsTab() {
		if (mListsFragment != null) {
			mListsFragment.showTab(ListsFragment.TAB_INDEX_ALL_CONTACTS);
		}
	}

	/**
	 * Implemented to satisfy {@link CallLogFragment.HostInterface}
	 */
	@Override
	public void showDialpad() {
		Log.d(TAG,"showDialpad()");
		showDialpadFragment(true);
	}

	@Override
	public void onPickPhoneNumberAction(Uri dataUri) {
		Log.d(TAG, "onPickPhoneNumberAction:" + dataUri);
		// Specify call-origin so that users will see the previous tab instead
		// of
		// CallLog screen (search UI will be automatically exited).
		PhoneNumberInteraction.startInteractionForPhoneCall(DialtactsActivity.this, dataUri, getCallOrigin());
		mClearSearchOnPause = true;
	}

	@Override
	public void onCallNumberDirectly(String phoneNumber) {
		onCallNumberDirectly(phoneNumber, false /* isVideoCall */);
	}

	@Override
	public void onCallNumberDirectly(String phoneNumber, boolean isVideoCall) {
		if (phoneNumber == null) {
			// Invalid phone number, but let the call go through so that
			// InCallUI can show
			// an error message.
			phoneNumber = "";
		}
		Intent intent = isVideoCall ? IntentUtil.getVideoCallIntent(phoneNumber, getCallOrigin())
				: IntentUtil.getCallIntent(phoneNumber, getCallOrigin());
		DialerUtils.startActivityWithErrorToast(this, intent);
		mClearSearchOnPause = true;
	}

	@Override
	public void onShortcutIntentCreated(Intent intent) {
		Log.w(TAG, "Unsupported intent has come (" + intent + "). Ignoring.");
	}

	@Override
	public void onHomeInActionBarSelected() {
		exitSearchUi();
	}

	public void onTabUnselected(Tab tab) {
	}

	/**
	 * Called when a tab that is already selected is chosen again by the user.
	 * Some applications may use this action to return to the top level of a
	 * category.
	 *
	 * @param tab
	 *            The tab that was reselected.
	 */
	public void onTabReselected(Tab tab) {
	}

	public int tabIndex=0;
	@Override
	public void onTabSelected(Tab tab) {
		Log.d(TAG, "liyang2017-onTabSelected tabIndex:" + tab.getPosition()+" tabIndexPre:"+tabIndexPre);

		tabIndex = tab.getPosition();
		mListsFragment.mViewPager.setCurrentItem(tabIndex);
		// Log.d(TAG, "onPageSelected:"+position+" tabIndex:"+tabIndex);
		if (tabIndex == ListsFragment.TAB_INDEX_ALL_CONTACTS) {
			// mFloatingActionButtonController.changeIcon(
			// getResources().getDrawable(R.drawable.hb_add_icon),
			// getResources().getString(R.string.search_shortcut_create_new_contact));
			// floatingActionButton.setImageDrawable(R.drawable.hb_add_icon);
			hideDialpadFragment(false, true);
			if (tabIndexPre == 0) {	
				// floatingActionButton.setBackgroundColor(getResources().getColor(R.color.contact_fab_background_color));
				floatingActionButton
				.setIconDrawable(getResources().getDrawable(R.drawable.hb_ic_person_add));
				mFloatingActionButtonController.setVisible(true);
			}else if(tabIndexPre==2){
				Log.d(TAG,"test");
				floatingActionButton
				.setIconDrawable(getResources().getDrawable(R.drawable.hb_ic_person_add));
				mFloatingActionButtonController.setVisible(true);
				mFloatingActionButtonController.scaleIn(0);
			}

		} else if (tabIndex == ListsFragment.TAB_INDEX_RECENTS) {
			if (tabIndexPre != 0 && !hideDialpadByFoldbutton) {
				showDialpadFragment(false);
			}
			if (tabIndexPre != 0) {
				// floatingActionButton.setBackgroundColor(getResources().getColor(R.color.hb_fab_bg_dialbutton));
				floatingActionButton.setIconDrawable(
						getResources().getDrawable(R.drawable.hb_dialtacts_activity_dial_fab_background_normal));

				if(mIsDialpadShown){
					if(animator!=null) animator.cancel();
					if(floatingActionButton.getVisibility()!=View.GONE) floatingActionButton.setVisibility(View.GONE);
				}else if(floatingActionButton.getVisibility()!=View.VISIBLE) floatingActionButton.setVisibility(View.VISIBLE);
			}
		} else if (tabIndex == ListsFragment.TAB_INDEX_YELLOWPAGE) {
			if(animator!=null) animator.cancel();

			//			if(mListsFragment.showRedPoint){
			//			mListsFragment.showRedPoint=false;
			//			mListsFragment.tabLayout.setupWithViewPager(mListsFragment.mViewPager);
			//			mListsFragment.tabLayout.setTabsFromPagerAdapter(mListsFragment.mViewPagerAdapter);
			//			}
			if (tabIndexPre == 0) {
				hideDialpadFragment(false, true);
			}
			if (tabIndexPre != 2) mFloatingActionButtonController.setVisible(false);
			boolean hasGetFineLocationPermission = PermissionsUtil.hasPermission(this, ACCESS_FINE_LOCATION);
			boolean hasGetCoarseLocationPermission = PermissionsUtil.hasPermission(this, ACCESS_COARSE_LOCATION);	
			if(!hasGetFineLocationPermission 
					|| !hasGetCoarseLocationPermission) {
				requestPermissions(new String[] {ACCESS_FINE_LOCATION,
						ACCESS_COARSE_LOCATION},
						LOCATION_PERMISSION_REQUEST_CODE);
			}
			if(mListsFragment!=null && mListsFragment.mYellowPageFragment!=null 
					&& mListsFragment.mYellowPageFragment.loadState==-1){

				hasChangeTabIndex=true;
				mListsFragment.mYellowPageFragment.showLoadingView();
				handler.removeCallbacks(runnable2);
				handler.postDelayed(runnable2, 330);
			}
			CooTekPhoneService.onSelectTab(DialtactsActivity.this);
		}
		tabIndexPre = tabIndex;

	}

	private boolean hasPermissionOfYellowPagePre=true;
	private boolean checkPermissionOfYellowPage(){
		boolean hasGetFineLocationPermission = PermissionsUtil.hasPermission(this, ACCESS_FINE_LOCATION);
		boolean hasGetCoarseLocationPermission = PermissionsUtil.hasPermission(this, ACCESS_COARSE_LOCATION);
		boolean hasReceiveSMSPermission = PermissionsUtil.hasPermission(this, RECEIVE_SMS);
		boolean hasReadSMSPermission = PermissionsUtil.hasPermission(this, READ_SMS);
		boolean hasCameraPermission = PermissionsUtil.hasPermission(this, CAMERA);
		Log.d(TAG, "liyang2017-checkPermissionOfYellowPage,hasGetFineLocationPermission:"+hasGetFineLocationPermission
				+" hasGetCoarseLocationPermission:"+hasGetCoarseLocationPermission
				+" hasReceiveSMSPermission:"+hasReceiveSMSPermission
				+" hasReadSMSPermission:"+hasReadSMSPermission
				+" hasCameraPermission:"+hasCameraPermission);
		return hasGetFineLocationPermission && hasGetCoarseLocationPermission && hasReceiveSMSPermission && hasReadSMSPermission && hasCameraPermission;
	}


	/**
	 * Returns true if Monkey is running.
	 */
	public static boolean isMonkeyRunning() {
		boolean result=false;
		try{
			result=ActivityManager.isUserAMonkey();
		}catch(Exception e){
			Log.e(TAG,"e:"+e);
		}
		Log.d(TAG,"isMonkeyRunning:"+result);
		return result;
	}


	public int tabIndexPre = 0;

	private TelecomManager getTelecomManager() {
		return (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
	}

	@Override
	public boolean isActionBarShowing() {
		// return mActionBarController.isActionBarShowing();
		return false;
	}

	@Override
	public ActionBarController getActionBarController() {
		return mActionBarController;
	}

	@Override
	public boolean isDialpadShown() {
		return mIsDialpadShown;
	}

	@Override
	public int getDialpadHeight() {
		if (mDialpadFragment != null) {
			return mDialpadFragment.getDialpadHeight();
		}
		return 0;
	}

	@Override
	public int getActionBarHideOffset() {
		// return getActionBar().getHideOffset();
		return 0;
	}

	@Override
	public void setActionBarHideOffset(int offset) {
		// getActionBar().setHideOffset(offset);
	}

	private int getFabAlignment() {
		if (!mIsLandscape && !isInSearchUi()
				&& mListsFragment.getCurrentTabIndex() == ListsFragment.TAB_INDEX_RECENTS) {
			return FloatingActionButtonController.ALIGN_MIDDLE;
		}
		return FloatingActionButtonController.ALIGN_END;
	}

	/**
	 * M: Set to clear dialpad and exit search ui while activity on pause
	 * 
	 * @param clearSearch
	 *            If true clear dialpad and exit search ui while activity on
	 *            pause
	 */
	public void setClearSearchOnPause(boolean clearSearch) {
		mClearSearchOnPause = clearSearch;
	}

	private boolean hideDialpadByFoldbutton;

	@Override
	public void onContactsLoadSuccess() {
		// TODO Auto-generated method stub
		Log.d(TAG,"onContactsLoadSuccess");

	}

	@Override
	public void onContactsLoadFailed() {
		// TODO Auto-generated method stub
		Log.d(TAG,"onContactsLoadFailed");
	}
}
