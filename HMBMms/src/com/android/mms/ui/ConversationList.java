/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.mms.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewStub;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.HbPreSearchView;
import android.widget.HbSearchView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.draft.DraftManager;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.transaction.SmsRejectedReceiver;
import com.android.mms.util.DraftCache;
import com.android.mms.util.FeatureOption;
import com.android.mms.util.MmsLog;
import com.android.mms.util.PDebug;
import com.android.mms.util.Recycler;
import com.google.android.mms.pdu.PduHeaders;
import com.mediatek.cb.cbmsg.CBMessageListActivity;
import com.mediatek.cb.cbmsg.CBMessagingNotification;
import com.mediatek.mms.folder.util.FolderModeUtils;
import com.mediatek.mms.util.MmsDialogNotifyUtils;
import com.mediatek.mms.util.PermissionCheckUtil;
import com.mediatek.wappush.WapPushMessagingNotification;
import com.mediatek.wappush.ui.WPMessageActivity;
import com.zzz.provider.Telephony;
import com.zzz.provider.Telephony.Mms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import hb.app.HbActivity;
import hb.app.dialog.AlertDialog;
import hb.app.dialog.ProgressDialog;
import hb.view.menu.bottomnavigation.BottomNavigationView;
import hb.view.menu.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;
import hb.widget.ActionMode;
import hb.widget.ActionMode.Item;
import hb.widget.ActionModeListener;
import hb.widget.FloatingActionButton;
import hb.widget.FloatingActionButton.OnFloatActionButtonClickListener;
import hb.widget.HbListView;
import hb.widget.toolbar.Toolbar;
import hb.widget.toolbar.Toolbar.OnMenuItemClickListener;


//import android.app.AlertDialog;
//import com.mediatek.ipmsg.util.IpMessageUtils;
//import com.android.mms.util.StatusBarSelectorReceiver;
//import com.mediatek.mms.ext.IOpConversationListExt;
//import com.android.mms.util.StatusBarSelectorCreator;
//import android.app.AlertDialog;
//import android.app.ProgressDialog;
//import android.view.ActionMode;
//import android.widget.ListView;
//import android.widget.SearchView;
//import android.widget.Toolbar;
//import com.android.mms.widget.MmsWidgetProvider;

/**
 * This activity provides a list view of existing conversations.
 */
//lichao modify ListActivity to HbActivity
//lichao add OnItemClickListener, OnItemLongClickListener,
//lichao add OnMenuItemClickListener
public class ConversationList extends HbActivity implements DraftCache.OnDraftChangedListener,
        OnItemClickListener, OnItemLongClickListener,
        OnMenuItemClickListener {
    public static final String TAG = "Mms/ConvList";
    private static final boolean DEBUG = true;
    private static final boolean DBG_MENU = false;
    private static final boolean LOCAL_LOGV = DEBUG;

    private static final int THREAD_LIST_QUERY_TOKEN = 1701;
    //lichao delete for no need query unread threads counts
    //private static final int UNREAD_THREADS_QUERY_TOKEN    = 1702;
    public static final int DELETE_CONVERSATION_TOKEN = 1801;
    public static final int HAVE_LOCKED_MESSAGES_TOKEN = 1802;
    public static final int DELETE_OBSOLETE_THREADS_TOKEN = 1803;
    public static final int HAVE_REJECTED_MESSAGES_TOKEN = 1804;

    // IDs of the context menu items for the list of conversations.
    //public static final int MENU_DELETE               = 0;
    //public static final int MENU_VIEW                 = 1;
    //public static final int MENU_VIEW_CONTACT         = 2;
    //public static final int MENU_ADD_TO_CONTACTS      = 3;
    private ThreadListQueryHandler mQueryHandler;
    private ConversationListAdapter mListAdapter;
    private SharedPreferences mPrefs;
    private Handler mHandler;
    private boolean mDoOnceAfterFirstQuery;
    //lichao delete for no need query unread threads counts
    //private TextView mUnreadConvCount;
    private MenuItem mSearchItem;
    private MenuItem mSettingsItem;
    private MenuItem mRejectMsgsItem;
    private MenuItem mCellBroadcastItem;
    private boolean mIsCellBroadcastAppLinkEnabled;
    /// M: fix bug ALPS00374917, cancel sim_sms menu when haven't sim card
    //private MenuItem mSimSmsItem;
    private HbSearchView mSearchView;//lichao modify
    private HbPreSearchView mPreSearchView;//lichao add in 2017-05-08
    private View mHeaderDivider;//lichao add in 2017-05-15
    //private View mSmsPromoBannerView;

    /// Google JB MR1.1 patch. conversation list can restore scroll position
    private int mSavedFirstVisiblePosition = AdapterView.INVALID_POSITION;
    private int mSavedFirstItemOffset;
    //lichao merge from qcmms
    private ProgressDialog mProgressDialog;

    // keys for extras and icicles
    private final static String LAST_LIST_POS = "last_list_pos";
    private final static String LAST_LIST_OFFSET = "last_list_offset";

    private static final String CHECKED_MESSAGE_LIMITS = "checked_message_limits";
    //private final static int DELAY_TIME = 500;

    // Whether or not we are currently enabled for SMS. This field is updated in onResume to make
    // sure we notice if the user has changed the default SMS app.
    private boolean mIsSmsEnabled;
    private Toast mComposeDisabledToast;
    private Toast mToast;//lichao add in 2017-07-12
    //lichao merge from qcmms
    private static long mLastDeletedThread = -1;
    /// M: new members
    //private static final String CONV_TAG = "Mms/convList";
    /// M: Code analyze 002, For new feature ALPS00041233, msim enhancment check in . @{
    //private StatusBarManager mStatusBarManager;
    /// @}
    /// M: Code analyze 001, For new feature ALPS00131956, wappush: add new params . @{
    //private int mType;
    //private static final String WP_TAG = "Mms/WapPush";
    /// @}
    /// M: Code analyze 004, For bug ALPS00247476, ensure the scroll smooth . @{
    private static final int CHANGE_SCROLL_LISTENER_MIN_CURSOR_COUNT = 100;
    /// @}
    /// M: Code analyze 004, For bug ALPS00247476, ensure the scroll smooth . @{
    private MyScrollListener mScrollListener =
            new MyScrollListener(CHANGE_SCROLL_LISTENER_MIN_CURSOR_COUNT, "ConversationList_Scroll_Tread");
    /// @}

    /// M: Code analyze 007, For bug ALPS00242955, If adapter data is valid . @{
    //private boolean mDataValid;
    /// @}
    /// M: Code analyze 008, For bug ALPS00250948, disable search in multi-select status . @{
    //private boolean mDisableSearchFalg = false;
    /// M: Code analyze 005, For new feature ALPS00247476, add selectAll/unSelectAll . @{
    //lichao delete in 2017-03-26
    //private ModeCallback mActionModeListener = new ModeCallback();
    //private ActionMode mActionMode;
    /// @}
    /// M: Optimize select all performance, save actionmode status and reduce select time. @{
    private static String ACTIONMODE = "actionMode";
    private static String NEED_RESTORE_ADAPTER_STATE = "needRestore";
    private boolean mIsNeedRestoreAdapterState = false;
    private static String SELECT_THREAD_IDS = "selectThreadIds";
    private long[] mListSelectedThreads;
    /// @}

    /// M: Code analyze 009, For bug ALPS00270910, Default SIM card icon shown in status bar
    /// is incorrect, need to get current sim information . @{
    //private static Activity sActivity = null;
    /// @}

    /// M: Code analyze 009, For new feature, plugin . @{
    //operator/OP01/packages/apps/Plugins/src/com/mediatek/mms/plugin/Op01MessagePluginExt.java:
    //   public IOpConversationListExt getOpConversationListExt() {
    //lichao comment it
    //private IOpConversationListExt mOpConversationList = null;
    /// @}

    /// M: add for ipmessage
    /// M: add for display unread thread count
    //lichao comment it
    //private static final int MAX_DISPLAY_UNREAD_COUNT = 99;
    //private static final String DISPLAY_UNREAD_COUNT_CONTENT_FOR_ABOVE_99 = "99+";
    //mtk no use
    //private static final String DROP_DOWN_KEY_NAME   = "drop_down_menu_text";
    private HbListView mListView; // we need this to update empty view.
    //private View mEmptyViewDefault;
    //mtk comment it
    //private ConversationEmptyView mEmptyView;
    //private LinearLayout mIpEmptyView;

    private Context mContext = null;
    //mtk no use
    //private ProgressDialog mSaveChatHistory;
    //mtk no use
    //boolean mIsSendEmail = false;
    //mtk no use
    //private int mTypingCounter;
    //for ipmessage
    //private LinearLayout mNetworkStatusBar;
    //mtk no use
    //private BroadcastReceiver mNetworkStateReceiver;
    //mtk no use
    //private static final String SAVE_HISTORY_MIMETYPE_ZIP = "application/zip";
    //private static final String SAVE_HISTORY_SUFFIX = ".zip";
    //private static final String SAVE_HISTORY_MIMETYPE_TEXT = "text/plain";

    /// M: Remove cursor leak @{
    private boolean mNeedQuery = false;    //If onContentChanged is called, set it means we need query again to refresh list
    private boolean mIsInActivity = false; //If activity is not displayed, no need do query
    /// @}

    //private boolean mIsJoynChanged;
    //private boolean mIsFirstCreate;
    private AlertDialog mDeleteAlertDialog;
    private AlertDialog mAddBlackConfirmDialog;
    private AlertDialog mRemoveBlackConfirmDialog;

    private static boolean sIsDeleting = false;

    //add for ipmessage
    //public IIpConversationListExt mIpConvList;
    //public ConversationListCallback mIpConvListCallback = new ConversationListCallback();
    //mtk add for mutli user
    private boolean isUserHasPerUsingMms = true;

    //lichao comment it
    //private StatusBarSelectorReceiver mStatusBarSelectorReceiver;

    //lichao add begin -------------------------------------------------------------------------
    //private int mDeleteThreadCount = -1;
    //private HbListView mListView;
    private FloatingActionButton mFloatButton;

    private boolean mNeedShowDefaultAppDialog = false;

    //private ImageView mEmptyImageView;
    private TextView mEmptyTextview;

    private TextView mEmptyHeaderText;
    private ImageView mEmptyHeaderImage;
    private AnimationDrawable mEmptyLoadAnim;

    private MySearchTask mSearchTask;

    private View mFooterView;
    private TextView mFooterText;

    private View mHeaderView;
    private TextView mHeaderText;
    private ImageView mHeaderImage;
    private AnimationDrawable mLoadAnim;

    private Toolbar myToolbar;
    private Menu mToolbarMenu;
    //private View mBackIcon;//lichao restore in 2017-05-08
    int mBackIconResId = -1;//lichao add in 2017-05-15

    //boolean isKeyboardShowing = false;

    private String mSearchString = "";
    private MySearchListAdapter mSearchAdapter;
    //public static final int CACHE_SIZE = 100;
    public static HashMap<String, Cursor> mSearchCursorCache =
            new HashMap<String, Cursor>();

    public enum ViewMode {NORMAL_MODE, EDIT_MODE, SEARCH_MODE}

    private ViewMode mViewMode;

    public enum SearchStatus {EXITED, EMPTY_INPUT, IS_SEARCHING, SHOWING_RESULT}

    private SearchStatus mSearchStatus;

    public enum QueryStatus {BEFORE_QUERY, IS_QUERY, AFTER_QUERY}

    private QueryStatus mQueryStatus;

    private BottomNavigationView mBottomNavigationView;
    private ActionMode mActionMode;
    //mtk use mSelectedThreadIds only in ModeCallback(ActionMode)
    //MTK是需要用的时候再通过mListAdapter.getSelectedThreadsList()获取
    //private HashSet<Long> mSelectedThreadIds;
    CopyOnWriteArrayList<Long> mSelectedThreadIds;
    //lichao add for blacklist in 2017-04-12
    //HashSet<ContactList> mSelectedRecipients;
    CopyOnWriteArrayList<ContactList> mSelectedRecipients;
    //lichao add in 2017-07-13, CopyOnWriteArrayList for thread-safe and reading effectively,
    // avoid ArrayList ConcurrentModificationExceptione
    CopyOnWriteArrayList<Integer> mSelectedPos;

    public static String ACTION_PICK_MULTIPLE_PHONES = "android.intent.action.conversation.list.PICKMULTIPHONES";
    private boolean mNeedEnterPickNumMode = false;

    public enum EditModeType {NONE, EDIT_THREAD, PICK_NUMBERS}
    private EditModeType mEditModeType;

    public enum EditOperation {NONE, SET_TOP_OPERATION, CANCEL_TOP_OPERATION,
        ADD_BLACK_OPERATION, REMOVE_BLACK_OPERATION, DELETE_THREAD_OPERATION}
    private EditOperation mEditOperation;

    private ProgressDialog mRebuildingDialog;
    private Runnable mRebuildingRunnable = new Runnable() {
        public void run() {
            boolean isRebuildingIndex = isRebuildingIndex();
            if (DEBUG) Log.v(TAG, "mRebuildingRunnable(), isRebuildingIndex: " + isRebuildingIndex);
            if (isRebuildingIndex) {
                mIsRebuildingIndex = true;
                mHandler.postDelayed(this, 1000);
            } else {
                mIsRebuildingIndex = false;
                dismissRebuildingDialog();
                startAsyncQueryNow();
            }
        }
    };

    //maximun select all size,一键批量选择数
    private static int SELECT_ALL_MAX_SIZE = 1000;
    private static int PATCH_CHECK_MAX_SIZE = 300;
    //因为一键批量选择后还可以单击item一个一个地选择，所以这个数目大于上面的一键批量选择数
    private static int SET_TOP_MAX_SIZE = 1100;
    private static int CANCEL_TOP_MAX_SIZE = 1100;
    //maximun add black size
    private static int ADD_BLACK_MAX_SIZE = 1100;
    private static int REMOVE_BLACK_MAX_SIZE = 1100;
    //限制的原因是Intent传递数据被限制在大概500KB之内
    private static int PICK_NUMBER_MAX_SIZE = 1100;

    private boolean mIsBreakedByMax = false;
    private boolean mNeedSmoothScrollToTop = false;
    private boolean mNeedFastScrollToTop = false;

    boolean mContainBlock = false;
    boolean mContainUnblock = false;
    boolean mContainUnTop = false;
    boolean mContainTop = false;
    boolean mIsDoInBackground = false;
    //boolean mIsUpdatingCheckBox = false;

    private final static int DIALOG_DELAY_TIME = 300;

    boolean mIsRebuildingIndex;
    private int mModifiedCout = 0;

    private static int LIST_COUNT_IN_ONE_PAGE = 6;
    //lichao add end -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.d(TAG, "==onCreate()==");
        if (PermissionCheckUtil.requestRequiredPermissions(this)) {
            return;
        } else {
            if (DraftCache.getInstance() == null) {
                MmsApp.getApplication().onRequestPermissionsResult();
            }
        }
        //lichao add begin
        //lichao delete for user experience is not good
        /*
        if (RequestPermissionsActivity.startPermissionActivity(this)) {
            Log.i(TAG, "[onCreate]startPermissionActivity,return.");
            return;
        }
		*/
        //lichao add end

        PDebug.EndAndStart("enterMms()", "ConversationList.onCreate");
        /// M: Code analyze 009, For bug ALPS00270910, Default SIM card icon shown in status
        /// bar is incorrect, need to get current sim information . @{
        //sActivity = ConversationList.this;
        /// @}
        //lichao comment it
        //initPlugin(this);
        //boolean folderMode = mOpConversationList.onCreate(this, savedInstanceState);
        /// M: Code analyze 010, new feature, MTK_OP01_PROTECT_START . @{
        //FolderModeUtils.startFolderViewList(folderMode, this, this,
        //        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        /// @}

        //setContentView(R.layout.conversation_list_screen);
        setHbContentView(R.layout.zzz_conversation_list_screen);
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //mSmsPromoBannerView = findViewById(R.id.banner_sms_promo);
        /// M: Code analyze 002, For new feature ALPS00041233, msim enhancment check in . @{
        //mStatusBarManager = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
        /// @}
        mQueryHandler = new ThreadListQueryHandler(getContentResolver());

        //lichao modify begin
        //mListView = getListView();
        mListView = (HbListView) findViewById(R.id.conversation_list);
        //mListView.setOnCreateContextMenuListener(mConvListOnCreateContextMenuListener);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);
        mListView.setFastScrollEnabled(false);
        mListView.setFastScrollAlwaysVisible(false);
        //lichao modify end
        //mtk add for delete key
        mListView.setOnKeyListener(mThreadListKeyListener);
        /// M: Code analyze 005, For new feature ALPS00247476, add selectAll/unSelectAll . @{
        mListView.setOnScrollListener(mScrollListener);
        mScrollListener.setThreadId(-1, ConversationList.this);
        mScrollListener.setHideSoftKeyboardRunnable(mHideSoftKeyboardRunnable);
        /// @}

        // Tell the list view which view to display when the list is empty
        //mEmptyViewDefault = findViewById(R.id.empty);
        //mIpEmptyView = (LinearLayout)findViewById(R.id.ipmsg_empty_view);
        //mEmptyView = (ConversationEmptyView) findViewById(R.id.empty2);//mtk deleted
        initEmptyView();
        //Must addHeaderView and addFooterView before setAdapter
        initListViewHeadAndFoot();

        //mNetworkStatusBar = (LinearLayout) findViewById(R.id.no_itnernet_view);
        //TextView networkStatusTextView = ((TextView) mNetworkStatusBar.findViewById(R.id.no_internet_text));

        //mListView.setOnItemLongClickListener(new ItemLongClickListener());
        /// M: Optimize select all performance, restore Actionmode status. @{
        if (savedInstanceState != null) {
            mIsNeedRestoreAdapterState = savedInstanceState.getBoolean(NEED_RESTORE_ADAPTER_STATE, false);
        } else {
            mIsNeedRestoreAdapterState = false;
        }
        /// @}
        mContext = ConversationList.this;//mtk add

        initListAdapter();

        //lichao add for updateFromViewMaxWidth begin
        float screenWidth = (float) getWindowManager().getDefaultDisplay().getWidth();
        int screenWidthDip = MessageUtils.px2dip(mContext, screenWidth);
        mListAdapter.setScreenWidthDip(screenWidthDip);
        //lichao add end
        //qcom, lichao modify
        //mProgressDialog = createProgressDialog();
        mProgressDialog = createProgressDialog(getText(R.string.updating_threads));
        //lichao add
        mRebuildingDialog = createProgressDialog(getText(R.string.rebuilding_contacts_index));

        setTitle(R.string.app_label);//qcom

        mHandler = new Handler();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean checkedMessageLimits = mPrefs.getBoolean(CHECKED_MESSAGE_LIMITS, false);
        if (DEBUG) Log.v(TAG, "checkedMessageLimits: " + checkedMessageLimits);
        if (!checkedMessageLimits) {
            runOneTimeStorageLimitCheckForLegacyMessages();
        }

        /// Google JB MR1.1 patch. conversation list can restore scroll position
        if (savedInstanceState != null) {
            mSavedFirstVisiblePosition = savedInstanceState.getInt(LAST_LIST_POS,
                    AdapterView.INVALID_POSITION);
            mSavedFirstItemOffset = savedInstanceState.getInt(LAST_LIST_OFFSET, 0);
        } else {
            mSavedFirstVisiblePosition = AdapterView.INVALID_POSITION;
            mSavedFirstItemOffset = 0;
        }

        //lichao add begin
        initToolBarAndActionbar();

        mViewMode = ViewMode.NORMAL_MODE;
        initMenuItems();

        initBackIcon();

        //initSearchView()
        initPreSearchView();

        initFloatActionButton();

        //setListenerToRootView();

        mSearchStatus = SearchStatus.EXITED;
        mQueryStatus = QueryStatus.BEFORE_QUERY;
        //lichao add end

        //mtk comment it
        //mIsJoynChanged = true;
        //mIsFirstCreate = true;
        PDebug.EndAndStart("ConversationList.onCreate", "onCreate -> onStart");

        //mIpConvList = IpMessageUtils.getIpMessagePlugin(this).getIpConversationList();
        //RCSe/RcsApp/src/com/mediatek/rcse/plugin/message/RcseConversationList.java:    public boolean onIpConversationListCreate(
        //mIpConvList.onIpConversationListCreate(this, mIpConvListCallback, mListView, mIpEmptyView, mNetworkStatusBar, networkStatusTextView);
        /// M: add for update sub state dynamically. @{
        //lichao comment it
        //IntentFilter intentFilter = new IntentFilter(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED);
        //registerReceiver(mSubReceiver, intentFilter);
        /// @}

        //lichao comment it
        //mStatusBarSelectorReceiver = new StatusBarSelectorReceiver(this);
        //IntentFilter statusBarSelectorIntentFilter = new IntentFilter(StatusBarSelectorReceiver.ACTION_MMS_ACCOUNT_CHANGED);
        //registerReceiver(mStatusBarSelectorReceiver, statusBarSelectorIntentFilter);

        registerReceiver(mBlackListChangeReceiver, new IntentFilter(MessageUtils.BLACK_CHANGE_ACTION));

        String actionStr = this.getIntent().getAction();
        Log.v(TAG, "onCreate(), Intent Action: " + actionStr);
        if (ACTION_PICK_MULTIPLE_PHONES.equals(actionStr)) {
            mNeedEnterPickNumMode = true;
        }
        if (!MmsConfig.isSmsEnabled(this) && !mNeedEnterPickNumMode) {
            if (DEBUG) Log.d(TAG, "onCreate(), NeedShowDefaultAppDialog");
            mNeedShowDefaultAppDialog = true;
        }
        initBottomNavigationView();
        updateOrdinaryItemsVisible();
    }

	/*
    //lichao delete begin
    private void setupActionBar() {
        ActionBar actionBar = getActionBar();

        ViewGroup v = (ViewGroup)LayoutInflater.from(this)
            .inflate(R.layout.conversation_list_actionbar, null);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(v,
                new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.END));

        mUnreadConvCount = (TextView)v.findViewById(R.id.unread_conv_count);
    }
	//lichao delete end
	*/

    private final ConversationListAdapter.OnContentChangedListener mContentChangedListener =
            new ConversationListAdapter.OnContentChangedListener() {
                @Override
                public void onContentChanged(ConversationListAdapter adapter) {
                    /// M: Remove cursor leak and reduce needless query @{
                    /* Only need when activity is shown*/
                    Log.d(TAG, "== onContentChanged ==");
                    //if isEditingTop, will call mStartQueryRunnable.run() in onPostExecute()
                    if (mIsInActivity && !isEditMode_EditTop()) {
                        mNeedQuery = true;
                        MmsLog.d(TAG, "onContentChanged >>>startAsyncQuery()");
                        startAsyncQuery();
                    }
                    /// @}
                }
            };

    private void initListAdapter() {
        mListAdapter = new ConversationListAdapter(this, null);
        /** M: now this code is useless and will lead to a JE, comment it.
         *  listener is set in onStart
         */
        //mListAdapter.setOnContentChangedListener(mContentChangedListener);
        //lichao modify begin
        //setListAdapter(mListAdapter);
        mListView.setAdapter(mListAdapter);

        //Log.d(TAG, "mListAdapter："+mListAdapter.toString());
        //mListAdapter：com.android.mms.ui.ConversationListAdapter

        //Log.d(TAG, "because addHeaderView before setAdapter, mListView.getAdapter()："
        //        + mListView.getAdapter().toString());
        //mListView.getAdapter()：android.widget.HeaderViewListAdapter

        //getListView().setRecyclerListener(mListAdapter);
        mListView.setRecyclerListener(mListAdapter);
        //lichao modify end
    }

    /*private void initSmsPromoBanner() {
        /// M: add for Mutli-user, show 'user is not allowed to use SMS' alert if user has no
        // permission to use SMS. @{
        ImageView defaultSmsAppIconImageView =
                (ImageView) mSmsPromoBannerView.findViewById(R.id.banner_sms_default_app_icon);
        TextView permissionAlertView = (TextView) mSmsPromoBannerView
                .findViewById(R.id.sms_permission_alert);
        LinearLayout disabledAlertView = (LinearLayout) mSmsPromoBannerView
                .findViewById(R.id.sms_disabled_alert);
        if (!isUserHasPerUsingMms) {
            mSmsPromoBannerView.setClickable(false);
            permissionAlertView.setVisibility(View.VISIBLE);
            disabledAlertView.setVisibility(View.GONE);
            defaultSmsAppIconImageView.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_launcher_smsmms));
            return;
        } else {
            mSmsPromoBannerView.setClickable(true);
            permissionAlertView.setVisibility(View.GONE);
            disabledAlertView.setVisibility(View.VISIBLE);
        }
        /// @}
        final PackageManager packageManager = getPackageManager();
        final String smsAppPackage = Telephony.Sms.getDefaultSmsPackage(this);

        // Get all the data we need about the default app to properly render the promo banner. We
        // try to show the icon and name of the user's selected SMS app and have the banner link
        // to that app. If we can't read that information for any reason we leave the fallback
        // text that links to Messaging settings where the user can change the default.
        Drawable smsAppIcon = null;
        ApplicationInfo smsAppInfo = null;
        try {
            smsAppIcon = packageManager.getApplicationIcon(smsAppPackage);
            smsAppInfo = packageManager.getApplicationInfo(smsAppPackage, 0);
        } catch (NameNotFoundException e) {
        }
        final Intent smsAppIntent = packageManager.getLaunchIntentForPackage(smsAppPackage);

        // If we got all the info we needed
        if (smsAppIcon != null && smsAppInfo != null && smsAppIntent != null) {
            defaultSmsAppIconImageView.setImageDrawable(smsAppIcon);
            TextView smsPromoBannerTitle =
                    (TextView) mSmsPromoBannerView.findViewById(R.id.banner_sms_promo_title);
            String message = getResources().getString(R.string.banner_sms_promo_title_application,
                    smsAppInfo.loadLabel(packageManager));
            smsPromoBannerTitle.setText(message);
            *//*mOpConversationList.initSmsPromoBanner(defaultSmsAppIconImageView, smsPromoBannerTitle,
                (TextView) mSmsPromoBannerView.findViewById(R.id.banner_sms_promo_message),
                smsAppInfo, packageManager);*//*
        }
        mSmsPromoBannerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch settings
                Intent settingIntent = new Intent(ConversationList.this,
                        MyMessagingPreferenceActivity.class);
                startActivityIfNeeded(settingIntent, -1);
            }
        });
    }*/

    /**
     * Checks to see if the number of MMS and SMS messages are under the limits for the
     * recycler. If so, it will automatically turn on the recycler setting. If not, it
     * will prompt the user with a message and point them to the setting to manually
     * turn on the recycler.
     */
    public synchronized void runOneTimeStorageLimitCheckForLegacyMessages() {
        if (Recycler.isAutoDeleteEnabled(this)) {
            if (DEBUG) Log.v(TAG, "recycler is already turned on");
            // The recycler is already turned on. We don't need to check anything or warn
            // the user, just remember that we've made the check.
            markCheckedMessageLimit();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (Recycler.checkForThreadsOverLimit(ConversationList.this)) {
                    if (DEBUG) Log.v(TAG, "checkForThreadsOverLimit TRUE");
                    // Dang, one or more of the threads are over the limit. Show an activity
                    // that'll encourage the user to manually turn on the setting. Delay showing
                    // this activity until a couple of seconds after the conversation list appears.
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(ConversationList.this,
                                    WarnOfStorageLimitsActivity.class);
                            startActivity(intent);
                        }
                    }, 2000);
                    /** M: comment this else block
                     } else {
                     if (DEBUG) Log.v(TAG, "checkForThreadsOverLimit silently turning on recycler");
                     // No threads were over the limit. Turn on the recycler by default.
                     runOnUiThread(new Runnable() {
                    @Override public void run() {
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putBoolean(GeneralPreferenceActivity.AUTO_DELETE, true);
                    editor.apply();
                    }
                    });
                     */
                }
                // Remember that we don't have to do the check anymore when starting MMS.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        markCheckedMessageLimit();
                    }
                });
            }
        }, "ConversationList.runOneTimeStorageLimitCheckForLegacyMessages").start();
    }

    /**
     * Mark in preferences that we've checked the user's message limits. Once checked, we'll
     * never check them again, unless the user wipe-data or resets the device.
     */
    private void markCheckedMessageLimit() {
        if (DEBUG) Log.v(TAG, "markCheckedMessageLimit");
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(CHECKED_MESSAGE_LIMITS, true);
        editor.apply();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (DEBUG) Log.d(TAG, "==onNewIntent()==");
        //mtk add
        if (PermissionCheckUtil.requestRequiredPermissions(this)) {
            return;
        }

        // Handle intents that occur after the activity has already been created.
        //mtk add
        MessagingNotification.cancelNotification(getApplicationContext(),
                SmsRejectedReceiver.SMS_REJECTED_NOTIFICATION_ID);
        startAsyncQuery();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (DEBUG) Log.d(TAG, "==onStart()==");
        if (!PermissionCheckUtil.checkRequiredPermissions(this)) {
            return;
        }

        PDebug.EndAndStart("onCreate -> onStart", "ConversationList.onStart");
        /// M: Code analyze 010, new feature, MTK_OP01_PROTECT_START . @{
        FolderModeUtils.setMmsDirMode(false);
        Log.i(TAG, "[Performance test][Mms] loading data start time ["
                + System.currentTimeMillis() + "]");

        /*if (!mIpConvList.onIpNeedLoadView(mEmptyViewDefault)) {
            if (mIsFirstCreate) {
                MmsLog.d(TAG, "normal message layout");
              //setupActionBar();
              setTitle(R.string.app_label);
              mIpEmptyView.setVisibility(View.GONE);
              mEmptyViewDefault.setVisibility(View.VISIBLE);
              mListView.setEmptyView(mEmptyViewDefault);
            }
        }*/

        //lichao add this before startAsyncQuery in 2017-07-14
        //move these codes to onResume()
        /*if (!MmsConfig.isSmsEnabled(this)) {
            if(isEditMode_EditThread() && !mIsDoInBackground && !sIsDeleting){
                if (DEBUG) Log.d(TAG, "onStart(), isEditMode, NeedShowDefaultAppDialog");
                mNeedShowDefaultAppDialog = true;
                changeToNormalMode();
            }
        }*/

        PDebug.Start("startAsyncQuery()");
        startAsyncQuery();
        MessagingNotification.cancelNotification(getApplicationContext(),
                SmsRejectedReceiver.SMS_REJECTED_NOTIFICATION_ID);

        DraftCache.getInstance().addOnDraftChangedListener(this);

        mDoOnceAfterFirstQuery = true;

        /// M: setOnContentChangedListener here, it will be removed in onStop @{
        mIsInActivity = true;
        if (mListAdapter != null) {
            if(DEBUG) MmsLog.d(TAG, "set onContentChanged listener");
            mListAdapter.setOnContentChangedListener(mContentChangedListener);
        }
        /// @}
        // We used to refresh the DraftCache here, but
        // refreshing the DraftCache each time we go to the ConversationList seems overly
        // aggressive. We already update the DraftCache when leaving CMA in onStop() and
        // onNewIntent(), and when we delete threads or delete all in CMA or this activity.
        // I hope we don't have to do such a heavy operation each time we enter here.
        /// M: Code analyze 0014, For new feature, third party may add/delete
        /// draft, and we must refresh to check this.
        /// M: to resolve ALPS00812509, do not refresh in onStart() to avoid frequently
        /// db operation while switch launcher and ConversationList.
        //DraftCache.getInstance().refresh();
        /// @}

        // we invalidate the contact cache here because we want to get updated presence
        // and any contact changes. We don't invalidate the cache by observing presence and contact
        // changes (since that's too untargeted), so as a tradeoff we do it here.
        // If we're in the middle of the app initialization where we're loading the conversation
        // threads, don't invalidate the cache because we're in the process of building it.
        // TODO: think of a better way to invalidate cache more surgically or based on actual
        // TODO: changes we care about
        if (!Conversation.loadingThreads()) {
            Contact.invalidateCache();
        }

        /// M: Code analyze 012, new feature, mms dialog notify . @{
        MmsDialogNotifyUtils dialogPlugin = new MmsDialogNotifyUtils(mContext);
        dialogPlugin.closeMsgDialog();
        /// @}

        /// M: add for ALPS01766374 ipMessage merge to L @{
        //IpMessageNmsgUtil.nmsgCheckService();
        /// @}

        PDebug.EndAndStart("ConversationList.onStart", "onStart -> onResume");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (DEBUG) Log.d(TAG, "==onStop()==");
        //qcmms
        stopAsyncQuery();

        /// M: @{
        mIsInActivity = false;
        /// @}

        DraftCache.getInstance().removeOnDraftChangedListener(this);

        //mtk have not while qc have
        unbindListeners(null);

        /// M add: @{
		//same as stopAsyncQuery() in qcom
        /*if (mQueryHandler != null) {
            MmsLog.d(TAG, "cancel query operation in onStop");
            mQueryHandler.cancelOperation(THREAD_LIST_QUERY_TOKEN);
            //mQueryHandler.cancelOperation(UNREAD_THREADS_QUERY_TOKEN);
            mNeedQuery = false;
        }*/
        /// @}

        // Simply setting the choice mode causes the previous choice mode to finish and we exit
        // multi-select mode (if we're in it) and remove all the selections.
        /// M:comment a line
        //getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        /// M: comment this line
        //mListAdapter.changeCursor(null);

        //lichao add begin
        clearSearchTask();
        mSearchStatus = SearchStatus.EXITED;
        updateHeaderView();
        //lichao add end
    }

    //mtk have not while qc have
    private void unbindListeners(final Collection<Long> threadIds) {
        //lichao modify getListView() to mListView
        for (int i = 0; i < mListView.getChildCount(); i++) {
            View view = mListView.getChildAt(i);
            if (view instanceof ConversationListItem) {
                ConversationListItem item = (ConversationListItem) view;
                if (threadIds == null) {
                    item.unbind(false);
                } else if (item.getConversation() != null
				        && threadIds.contains(item.getConversation().getThreadId())) {
                    item.unbind(false);
                }
            }
        }
    }

    @Override
    public void onDraftChanged(final long threadId, final boolean hasDraft) {
        // Run notifyDataSetChanged() on the main thread.
        mQueryHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                    log("onDraftChanged: threadId=" + threadId + ", hasDraft=" + hasDraft);
                }
                mListAdapter.notifyDataSetChanged();
            }
        });
    }

    //lichao refactor in 2017-06-20
    private void startAsyncQuery() {
        boolean isRebuildingIndex = isRebuildingIndex();
        //if (DEBUG) Log.v(TAG, "startAsyncQuery(), isRebuildingIndex: " + isRebuildingIndex);
        if (isRebuildingIndex) {
            mIsRebuildingIndex = true;
            mShowRebuildingDialogRunnable.run();
            mHandler.postDelayed(mRebuildingRunnable, 1000);
        } else {
            startAsyncQueryNow();
        }
    }

    //lichao refactor
    private void startAsyncQueryNow() {
        try {
            mNeedQuery = false;
            //if (!mIpConvList.onIpStartAsyncQuery()) {
            /// M: fix bug ALPS00941735, except Obsolete ThreadId when query
            //lichao modify begin
            //((TextView) (mEmptyViewDefault)).setText(R.string.loading_conversations);
            mQueryStatus = QueryStatus.IS_QUERY;
            updateEmptyView();

            if (DraftManager.sEditingThread.isEmpty()) {
                MmsLog.d(TAG, "DraftManager.sEditingThread isEmpty");
                Conversation.startQueryForAll(mContext, mQueryHandler, THREAD_LIST_QUERY_TOKEN);
            } else {
                long exceptID = getExceptId();
                MmsLog.d(TAG, "DraftManager except ThreadId = " + exceptID);
                Conversation.startQuery(mContext, mQueryHandler, THREAD_LIST_QUERY_TOKEN, "threads._id<>" + exceptID);
            }
            //lichao delete
            //Conversation.startQuery(mQueryHandler, UNREAD_THREADS_QUERY_TOKEN, "allunread");
            //}
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(mContext, e);
        }
    }

    //qcmms
    private void stopAsyncQuery() {
        if (mQueryHandler != null) {
            mQueryHandler.cancelOperation(THREAD_LIST_QUERY_TOKEN);
            //lichao delete
            //mQueryHandler.cancelOperation(UNREAD_THREADS_QUERY_TOKEN);
            //lichao merge from mtk
            mNeedQuery = false;
        }
    }

    //lichao change SearchView to HbSearchView
    HbSearchView.OnQueryTextListener mQueryTextListener = new HbSearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            /*Intent intent = new Intent();
            intent.setClass(ConversationList.this, SearchActivity.class);
            intent.putExtra(SearchManager.QUERY, query);
            startActivity(intent);
            mSearchItem.collapseActionView();
            return true;*/
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            /*// M: Add for OP
            //if (mOpConversationList.onQueryTextChange(newText)) {
            //    return true;
            //}
            return false;*/
            if (DEBUG) Log.d(TAG, "\n onQueryTextChange, newText = " + newText);
            if (newText.equals(mSearchString)) {
                return true;
            }
            //newText maybe is empty
            mSearchString = newText;
            clearSearchTask();
            if (TextUtils.isEmpty(mSearchString)) {
                mSearchStatus = SearchStatus.EMPTY_INPUT;
                updateHeaderView();//hide HeaderView
                showEmptySearchListView();//Empty list
                updateEmptyView();//show EmptyView
                /*if (mListAdapter != null) {
                    mListView.setAdapter(mListAdapter);
                    mListAdapter.notifyDataSetChanged();
                }*/
                updateFooterView();//hide FooterView
                return true;
            }
            mSearchStatus = SearchStatus.IS_SEARCHING;
            updateEmptyView();//show is searching while showing EmptyView
            updateHeaderView();//show is searching while showing ListView
            startSearch(mSearchString);
            return true;
        }
    };

    /*
	//lichao delete for replace ActionBar with Toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MmsLog.d(TAG, "onCreateOptionsMenu enter!!");

        if (!PermissionCheckUtil.checkRequiredPermissions(this)) {
            return false;
        }

        /// M: for ALPS01861847, add for mutli user @{
        if (!isUserHasPerUsingMms) {
            MmsLog.d(TAG, "onCreateOptionsMenu user has no permission");
            return false;
        }
        /// @}
        getMenuInflater().inflate(R.menu.conversation_list_menu, menu);

        mSearchItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) mSearchItem.getActionView();
        mOpConversationList.onCreateOptionsMenu(menu, mSearchItem, MmsConfig.getPluginMenuIDBase(),
                R.id.search, mSearchView);
        mSearchView = (SearchView) mOpConversationList.getSearchView(mSearchView);

        /// @}
        mSearchView.setOnQueryTextListener(mQueryTextListener);
        mSearchView.setQueryHint(getString(R.string.search_hint));
        mSearchView.setIconifiedByDefault(true);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        if (searchManager != null) {
            SearchableInfo info = searchManager.getSearchableInfo(this.getComponentName());
            mSearchView.setSearchableInfo(info);
        }

        MenuItem cellBroadcastItem = menu.findItem(R.id.action_cell_broadcasts);
        if (cellBroadcastItem != null) {
            // Enable link to Cell broadcast activity depending on the value in config.xml.
            boolean isCellBroadcastAppLinkEnabled = this.getResources().getBoolean(
                    com.android.internal.R.bool.config_cellBroadcastAppLinks);
            try {
                if (isCellBroadcastAppLinkEnabled) {
                    PackageManager pm = getPackageManager();
                    if (pm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver")
                            == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        isCellBroadcastAppLinkEnabled = false;  // CMAS app disabled
                    }
                }
            } catch (IllegalArgumentException ignored) {
                isCellBroadcastAppLinkEnabled = false;  // CMAS app not installed
            }
            if (!isCellBroadcastAppLinkEnabled) {
                cellBroadcastItem.setVisible(false);
            }
        }

        mIpConvList.onIpCreateOptionsMenu(menu);
        /// M: for mwi. @{
        if (FeatureOption.MTK_MWI_SUPPORT) {
            MenuItem mwiItem = menu.findItem(R.id.action_mwi);
            mwiItem.setVisible(true);
        }
        /// @}
        return true;
    }
	*/

    //lichao delete for replace ActionBar with Toolbar
	/*
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MmsLog.d(TAG, "onPrepareOptionsMenu enter!!");
        /// M: for ALPS01861847, add for mutli user @{
        if (!isUserHasPerUsingMms) {
            MmsLog.d(TAG, "onPrepareOptionsMenu user has no permission");
            return false;
        }
        /// @}
        mOpConversationList.onPrepareOptionsMenu(menu);
        mOptionsMenu = menu ;
        setDeleteMenuVisible(menu);
        MenuItem item;
        item = menu.findItem(R.id.action_compose_new);
        if (item != null ){
            // Dim compose if SMS is disabled because it will not work (will show a toast)
            item.getIcon().setAlpha(MmsConfig.isSmsEnabled(this) ? 255 : 127);
        }
        if (!LogTag.DEBUG_DUMP) {
            item = menu.findItem(R.id.action_debug_dump);
            if (item != null) {
                item.setVisible(false);
            }
        }

        /// M: Code analyze 011, add code for omacp . @{
        item = menu.findItem(R.id.action_omacp);
        item.setVisible(false);
        Context otherAppContext = null;
        try {
            otherAppContext = this.createPackageContext("com.mediatek.omacp",
                    Context.CONTEXT_IGNORE_SECURITY);
        } catch (Exception e) {
            MmsLog.e(TAG, "ConversationList NotFoundContext");
        }
        if (null != otherAppContext) {
            SharedPreferences sp = otherAppContext.getSharedPreferences("omacp",
                    MODE_WORLD_READABLE | MODE_MULTI_PROCESS);
            boolean omaCpShow = sp.getBoolean("configuration_msg_exist", false);
            if (omaCpShow) {
                item.setVisible(true);
            }
        }
        /// @}

        mIpConvList.onIpPrepareOptionsMenu(menu);
        item = menu.findItem(R.id.action_wappush);
        item.setVisible(true);

        //add for multi user feature
        if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
            MenuItem itemSetting = menu.findItem(R.id.action_settings);
            MenuItem itemSimInfo = menu.findItem(R.id.action_siminfo);
            itemSetting.setVisible(false);
            itemSimInfo.setVisible(false);
        }

        return true;
    }
	*/

    @Override
    public boolean onSearchRequested() {
        if (DEBUG) Log.d(TAG, "[onSearchRequested]");
		/*
		//lichao delete
        if (mSearchItem != null) {
            mSearchItem.expandActionView();
        }
		*/
        //lichao add begin
        if (isEditMode()) {
            // block search entirely (by simply returning false).
            return false;
        }
        enterSearchMode();
        //lichao add end
        return true;
    }

    /*
    //lichao delete with onCreateOptionsMenu(Menu menu) and onPrepareOptionsMenu(Menu menu)
	//replace ActionBar with Toolbar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /// M: Code analyze 009, For new feature, plugin . @{
        if (mOpConversationList.onOptionsItemSelected(item, R.id.action_settings)) {
            return true;
        }
        /// @}

        if (mIpConvList.onIpOptionsItemSelected(item, mIsSmsEnabled)) {
            return true;
        }

        switch(item.getItemId()) {
            case R.id.action_compose_new:
                if (mIsSmsEnabled) {
                    createNewMessage();
                } else {
                    // Display a toast letting the user know they can not compose.
                    if (mComposeDisabledToast == null) {
                        mComposeDisabledToast = Toast.makeText(this,
                                R.string.compose_disabled_toast, Toast.LENGTH_SHORT);
                    }
                    mComposeDisabledToast.show();
                }
                break;
            case R.id.action_delete_all:
                    // The invalid threadId of -1 means all threads here.
                    confirmDeleteThread(-1L, mQueryHandler);
                break;
            case R.id.action_settings:
                Intent settingIntent = new Intent(this, SettingListActivity.class);
                startActivityIfNeeded(settingIntent, -1);
                break;
            /// M: Code analyze 011, add omacp to option menu . @{
            case R.id.action_omacp:
                Intent omacpintent = new Intent();
                omacpintent.setClassName("com.mediatek.omacp", "com.mediatek.omacp.message.OmacpMessageList");
                omacpintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityIfNeeded(omacpintent, -1);
                break;
            /// @}
            case R.id.action_wappush:
                Intent wpIntent = new Intent(this, WPMessageActivity.class);
                startActivity(wpIntent);
                break;
            case R.id.action_debug_dump:
                LogTag.dumpInternalTables(this);
                break;
            case R.id.action_cell_broadcasts:
                Intent cellBroadcastIntent = new Intent(Intent.ACTION_MAIN);
                cellBroadcastIntent.setComponent(new ComponentName(
                        "com.android.cellbroadcastreceiver",
                        "com.android.cellbroadcastreceiver.CellBroadcastListActivity"));
                cellBroadcastIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(cellBroadcastIntent);
                } catch (ActivityNotFoundException ignored) {
                    Log.e(TAG, "ActivityNotFoundException for CellBroadcastListActivity");
                }
                return true;
            case R.id.action_mwi:
                Intent mwiIntent = new Intent(this, MwiListActivity.class);
                startActivity(mwiIntent);
                break;
            default:
                return true;
        }
        return false;
    }
    */

    /*
	//lichao change to onItemClick
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // Note: don't read the thread id data from the ConversationListItem view passed in.
        // It's unreliable to read the cached data stored in the view because the ListItem
        // can be recycled, and the same view could be assigned to a different position
        // if you click the list item fast enough. Instead, get the cursor at the position
        // clicked and load the data from the cursor.
        // (ConversationListAdapter extends CursorAdapter, so getItemAtPosition() should
        // return the cursor object, which is moved to the position passed in)
        Cursor cursor  = (Cursor) getListView().getItemAtPosition(position);
        /// M: Code analyze 015, For bug,  add cursor == null check . @{
        if (cursor == null) {
            return;
        }
        /// @}
        MmsLog.d(TAG, "onListItemClick: pos=" + position);
        Conversation conv = Conversation.from(this, cursor);
        long threadId = conv.getThreadId();
        /// M: Code analyze 005, For new feature ALPS00247476, handle click item with ActionMode . @{
        if (mActionMode != null) {
            boolean checked = mListAdapter.isContainThreadId(threadId);
            mActionModeListener.setItemChecked(position, !checked, null);
            mActionModeListener.updateActionMode();
            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }
            return;
        }
        /// @}

        if (LogTag.VERBOSE) {
            Log.d(TAG, "onListItemClick: pos=" + position + ", view=" + v + ", threadId=" + threadId);
        }

        //mtk move it to viewThread() method
		//openThread(threadId);
        /// M: Fix ipmessage bug
        viewThread(conv, conv.getType()) ;//mtk
    }
	*/

    /// M: Fix ipmessage bug @{
    //lichao delete for ipmessage in 2017-05-02
    private void viewThread(Conversation conv, int type) {

        long threadId = conv.getThreadId();
        /// M: add for ALPS01766374 ipMessage merge to L @{
        //if (IpMessageNmsgUtil.startNmsgActivity(this, conv, IpMessageNmsgUtil.OpenType.SMS_LIST)) {
        //    return;
        //}
        /// @}

        /// M: Code analyze 001, For new feature ALPS00131956, wappush: modify
        /// the calling of openThread, add one parameter. @{
        //MmsLog.i(WP_TAG, "ConversationList: " + "conv.getType() is : " + conv.getType());

        //ContactList list = conv.getRecipients();
        /*if (list != null && list.size() == 1) {
            if (mIpConvList.onIpOpenThread(list.get(0).getNumber(), threadId)) {
                conv.markAsSeen();
                return;
            }
        }*/
        openThread(threadId, type);
        /// @}
    }
    /// @}

    private void createNewMessage() {
        /*if (mIpConvList.onIpCreateNewMessage()) {
            return;
        }*/
        startActivity(ComposeMessageActivity.createIntent(this, 0));
    }

	//mtk overload it
    private void openThread(long threadId) {
        startActivity(ComposeMessageActivity.createIntent(this, threadId));
    }

    /// M: Code analyze 001, For new feature ALPS00131956, the method is extended. @{
    //lichao delete for ipmessage in 2017-05-02
    private void openThread(long threadId, int type) {
        switch (type) {
            case Telephony.Threads.WAPPUSH_THREAD:
                startActivity(WPMessageActivity.createIntent(this, threadId));
                break;
            case Telephony.Threads.CELL_BROADCAST_THREAD:
                startActivity(CBMessageListActivity.createIntent(this, threadId));
                break;
            default:
                startActivity(ComposeMessageActivity.createIntent(this, threadId));
                break;
        }
    }
    /// @}

    public static Intent createAddContactIntent(String address) {
        // address must be a single recipient
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(Contacts.CONTENT_ITEM_TYPE);
        if (Mms.isEmailAddress(address)) {
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, address);
        } else {
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, address);
            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        return intent;
    }

	/*
	//lichao delete
    private final OnCreateContextMenuListener mConvListOnCreateContextMenuListener =
        new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) {
            Cursor cursor = mListAdapter.getCursor();
            if (cursor == null || cursor.getPosition() < 0) {
                return;
            }
            Conversation conv = Conversation.from(ConversationList.this, cursor);
            /// M: Code analyze 001, For new feature ALPS00131956, wappush: get
            /// the added mType value. @{
            mType = conv.getType();
            MmsLog.i(WP_TAG, "ConversationList: " + "mType is : " + mType);
            /// @}

            ContactList recipients = conv.getRecipients();
            menu.setHeaderTitle(recipients.formatNames(","));

            AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.add(0, MENU_VIEW, 0, R.string.menu_view);

            // Only show if there's a single recipient
            if (recipients.size() == 1) {
                // do we have this recipient in contacts?
                if (recipients.get(0).existsInDatabase()) {
                    menu.add(0, MENU_VIEW_CONTACT, 0, R.string.menu_view_contact);
                } else {
                        menu.add(0, MENU_ADD_TO_CONTACTS, 0, R.string.menu_add_to_contacts);
                        String number = recipients.get(0).getNumber();
                        mIpConvList.onIpCreateContextMenu(menu, number);
//                }
                    /// @}
                }
            }
            if (mIsSmsEnabled) {
                menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
            }
        }
    };
	*/

    /*
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Cursor cursor = mListAdapter.getCursor();
        if (cursor != null && cursor.getPosition() >= 0) {
            Conversation conv = Conversation.from(ConversationList.this, cursor);
            long threadId = conv.getThreadId();
            switch (item.getItemId()) {
            case MENU_DELETE: {
                confirmDeleteThread(threadId, mQueryHandler);
                break;
            }
            case MENU_VIEW: {
                /// M: Fix ipmessage bug @{
                /// M: Code analyze 001, For new feature ALPS00131956,
                /// wappush: method is changed. @{
                /// openThread(threadId, mType);
                /// @}
                viewThread(conv, mType) ;
                /// @}
                break;
            }
            case MENU_VIEW_CONTACT: {
                Contact contact = conv.getRecipients().get(0);
                Intent intent = new Intent(Intent.ACTION_VIEW, contact.getUri());
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(intent);
                break;
            }
            case MENU_ADD_TO_CONTACTS: {
                String address = conv.getRecipients().get(0).getNumber();
                startActivity(createAddContactIntent(address));
                break;
            }
            default:
                break;
            }
        }
        return super.onContextItemSelected(item);
    }
	*/

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // We override this method to avoid restarting the entire
        // activity when the keyboard is opened (declared in
        // AndroidManifest.xml).  Because the only translatable text
        // in this activity is "New Message", which has the full width
        // of phone to work with, localization shouldn't be a problem:
        // no abbreviated alternate words should be needed even in
        // 'wide' languages like German or Russian.

        super.onConfigurationChanged(newConfig);
        if (DEBUG) Log.v(TAG, "onConfigurationChanged: " + newConfig);
    }

    /// M: Code analyze 008, For bug ALPS00250948, disable search in
    /// multi-select status . @{
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /*if (mDisableSearchFalg) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_SEARCH:
                    // do nothing since we don't want search box which may cause UI crash
                    // TODO: mapping to other useful feature
                    return true;
                default:
                    break;
            }
        }*/
        if (keyCode == KeyEvent.KEYCODE_SEARCH && isNormalMode()) {
            enterSearchMode();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    /// @}

    /**
     * Start the process of putting up a dialog to confirm deleting a thread,
     * but first start a background query to see if any of the threads or thread
     * contain locked messages so we'll know how detailed of a UI to display.
     *
     * @param threadId id of the thread to delete or -1 for all threads
     * @param handler  query handler to do the background locked query
     */
    public static void confirmDeleteThread(long threadId, AsyncQueryHandler handler) {
        ArrayList<Long> threadIds = null;
        if (threadId != -1) {
            threadIds = new ArrayList<Long>();
            threadIds.add(threadId);
        }
        confirmDeleteThreads(threadIds, handler);
    }

    /**
     * Start the process of putting up a dialog to confirm deleting threads,
     * but first start a background query to see if any of the threads
     * contain locked messages so we'll know how detailed of a UI to display.
     *
     * @param threadIds list of threadIds to delete or null for all threads
     * @param handler   query handler to do the background locked query
     */
    public static void confirmDeleteThreads(Collection<Long> threadIds, AsyncQueryHandler handler) {
        //Conversation.startQueryHaveLockedMessages(handler, threadIds,
        //        HAVE_LOCKED_MESSAGES_TOKEN);
        //先查询是否包含被拦截信息，查询完成之后再进行删除动作
        Conversation.startQueryHaveRejectedMessages(handler, threadIds,
                HAVE_REJECTED_MESSAGES_TOKEN);
    }

    /**
     * Build and show the proper delete thread dialog. The UI is slightly different
     * depending on whether there are locked messages in the thread(s) and whether we're
     * deleting single/multiple threads or all threads.
     *
     * @param listener          gets called when the delete button is pressed
     * @param threadIds         the thread IDs to be deleted (pass null for all threads)
     * @param hasRejectedMessages whether the thread(s) contain locked messages
     * @param context           used to load the various UI elements
     */
    private void confirmDeleteThreadDialog(final DeleteThreadListener listener,
                                           Collection<Long> threadIds,
                                           boolean hasRejectedMessages,
                                           Context context) {
        //lichao modify 
        View contents = View.inflate(context, R.layout.zzz_delete_thread_dialog_view, null);
        //TextView msg = (TextView)contents.findViewById(R.id.message);

        String messageStr = context.getResources().getString(R.string.confirm_delete_message);
        if (threadIds == null) {
            //msg.setText(R.string.confirm_delete_all_conversations);
            messageStr = context.getResources().getString(R.string.confirm_delete_all_conversations);
        } else {
            // Show the number of threads getting deleted in the confirmation dialog.
            int cnt = threadIds.size();
            //messageStr = context.getResources().getQuantityString(
            //        R.plurals.confirm_delete_conversation, cnt, cnt);
            if (cnt == 1) {
                messageStr = context.getResources().getString(R.string.confirm_delete_conversation_one);
            } else {
                messageStr = context.getResources().getString(R.string.confirm_delete_conversation_other, cnt);
            }
        }

        final CheckBox checkbox = (CheckBox)contents.findViewById(R.id.delete_rejected);
        if (!hasRejectedMessages) {
            checkbox.setVisibility(View.GONE);
        } else {
            listener.setDeleteRejectedMessage(checkbox.isChecked());
            checkbox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.setDeleteRejectedMessage(checkbox.isChecked());
                }
            });
        }

        /// M: Code analyze 023, For bug ALPS00268161, when delete one MMS, one sms will not be deleted . @{
        Cursor cursor = null;
        int smsId = 0;
        int mmsId = 0;
        cursor = context.getContentResolver().query(Telephony.Sms.CONTENT_URI,
                new String[]{"max(_id)"}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    smsId = cursor.getInt(0);
                    MmsLog.d(TAG, "confirmDeleteThreadDialog max SMS id = " + smsId);
                }
            } finally {
                cursor.close();
            }
        }
        cursor = context.getContentResolver().query(Mms.CONTENT_URI,
                new String[]{"max(_id)"}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    mmsId = cursor.getInt(0);
                    MmsLog.d(TAG, "confirmDeleteThreadDialog max MMS id = " + mmsId);
                }
            } finally {
                cursor.close();
            }
        }
        listener.setMaxMsgId(mmsId, smsId);
        /// @}

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder/*.setTitle(R.string.confirm_dialog_title)*/
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setCancelable(true)
                .setPositiveButton(R.string.delete, listener)
                .setNegativeButton(R.string.no, getNegativeListener())
                .setOnCancelListener(getCancelListener())
                .setOnDismissListener(getDismissListener())
                .setMessage(messageStr);
        if (hasRejectedMessages) {
            builder.setView(contents);
        }
        mDeleteAlertDialog = builder.create();
        mDeleteAlertDialog.show();
    }

    //HB. Comments :  , Engerineer : lichao , Date : 17-7-15 , begin
    private DialogInterface.OnClickListener getPositiveListener(final boolean isAddBlack){
        DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                if (DBG_MENU) Log.d(TAG, "PositiveButton clicked");
                if(isAddBlack){
                    mEditOperation = EditOperation.ADD_BLACK_OPERATION;
                    addToBlacklist();
                }else{
                    mEditOperation = EditOperation.REMOVE_BLACK_OPERATION;
                    removeFromBlacklist();
                }
            }
        };
        return positiveListener;
    }

    private DialogInterface.OnClickListener getNegativeListener(){
        DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                if(isEditMode()){
                    updateBottomMenuItems();
                }
            }
        };
        return negativeListener;
    }

    private DialogInterface.OnCancelListener getCancelListener(){
        DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog){
                if(isEditMode()){
                    updateBottomMenuItems();
                }
            }
        };
        return cancelListener;
    }

    private DialogInterface.OnDismissListener getDismissListener(){
        DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog){
                if(isEditMode()){
                    updateBottomMenuItems();
                }
            }
        };
        return dismissListener;
    }
    //HB. Comments :  , Engerineer : lichao , Date : 17-7-15 , end

    //mtk add for delete key
    private final OnKeyListener mThreadListKeyListener = new OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DEL: {
                        //lichao modify: getListView()
                        long id = mListView.getSelectedItemId();
                        if (id > 0) {
                            confirmDeleteThread(id, mQueryHandler);
                        }
                        return true;
                    }
                }
            }
            return false;
        }
    };

    public static class DeleteThreadListener implements DialogInterface.OnClickListener {
        private final Collection<Long> mThreadIds;
        //private final ConversationQueryHandler mHandler;//qc
        private final AsyncQueryHandler mHandler;//mtk
        private final Context mContext;
        private boolean mDeleteRejectedMessages;
        //private final Runnable mCallBack_DeletingRunnable;//qcmms
        /// M: Code analyze 023, For bug ALPS00268161, when delete one MMS, one
        /// sms will not be deleted. . @{
        private int mMaxMmsId;
        private int mMaxSmsId;

        public void setMaxMsgId(int mmsId, int smsId) {
            mMaxMmsId = mmsId;
            mMaxSmsId = smsId;
        }
        /// @}

        //qcmms
        //public DeleteThreadListener(Collection<Long> threadIds, ConversationQueryHandler handler,
        //        Runnable callBackDeletingRunnable, Context context) {
        //mtk
        //public DeleteThreadListener(Collection<Long> threadIds, AsyncQueryHandler handler,
        //        Context context) {
        //lichao add callBackDeletingRunnable
        public DeleteThreadListener(Collection<Long> threadIds, AsyncQueryHandler handler,
                                    /*Runnable callBackDeletingRunnable,*/ Context context) {
            //Log.d(TAG, "new DeleteThreadListener: threadIds = "+threadIds);
            mThreadIds = threadIds;
            mHandler = handler;
            mContext = context;
            //mCallBack_DeletingRunnable = callBackDeletingRunnable;//qcmms
        }


        public void setDeleteRejectedMessage(boolean deleteRejectedMessages) {
            mDeleteRejectedMessages = deleteRejectedMessages;
        }

        @Override
        public void onClick(DialogInterface dialog, final int whichButton) {
            MessageUtils.handleReadReport(mContext, mThreadIds,
                    PduHeaders.READ_STATUS__DELETED_WITHOUT_BEING_READ, new Runnable() {
                        @Override
                        public void run() {
                            /// M: fix ALPS01524674, mThreadIds is a weak reference to mSelectedThreadIds.
                            /// if delete once, mThreadIds.size() will be updated to 0.
                            if (mThreadIds != null && mThreadIds.size() == 0) {
                                return;
                            }
                            /// M: Code analyze 013, For bug ALPS00046358 , The method about the
                            /// handler with progress dialog functio . @{
                            //will dismissProgressDialog when onDeleteComplete
                            showProgressDialog();
                            /// @}
                            sIsDeleting = true;
                            /// M: delete ipmessage in ipmessage db
                            //IpMessageUtils.onIpDeleteMessage(mContext, mThreadIds, mMaxSmsId, mDeleteRejectedMessages);

                            int token = DELETE_CONVERSATION_TOKEN;

                            //lichao add for exit select mode begin
                            /*if (mCallBack_DeletingRunnable != null) {
                                mCallBack_DeletingRunnable.run();
                            }*/
                            //lichao merge from qcmms for unbind the deleted Conversation
                            if (mContext instanceof ConversationList) {
                                ((ConversationList) mContext).unbindListeners(mThreadIds);
                            }
                            //lichao add for exit select mode end

                            /// M: wappush: do not need modify the code here, but delete function in provider has been modified.
                            /// M: fix bug ALPS00415754, add some useful log
                            MmsLog.d(TAG, "before delete threads in conversationList, mThreadIds.size = " + (mThreadIds == null ? "null" : mThreadIds.size()));
                            if (mThreadIds == null) {
                                /// M: Code analyze 023, For bug ALPS00268161, when delete one
                                /// MMS, one sms will not be deleted. . @{
                                Conversation.startDeleteAll(mHandler, token, mDeleteRejectedMessages, mMaxMmsId, mMaxSmsId);
                                /// @}
                                /// M: modify for fix ALPS01071334, move to onDeleteCompleted(). in some case, when refresh run, the messages have not
                                ///     been deleted all, the draft state has not been changed, so draftcache is wrong
                                //DraftCache.getInstance().refresh();
                            } else if (mThreadIds.size() <= 1) {
                                /// @}
                                for (long threadId : mThreadIds) {
                                    /// M: Code analyze 023, For bug ALPS00268161, when delete one
                                    /// MMS, one sms will not be deleted . @{
                                    Conversation.startDelete(mHandler, token, mDeleteRejectedMessages,
                                            threadId, mMaxMmsId, mMaxSmsId);
                                    /// @}
                                    DraftCache.getInstance().setDraftState(threadId, false);
                                }
                            } else if (mThreadIds.size() > 1) {
                                /// M: Fix bug ALPS00780175, The 1300 threads deleting will cost more than
                                /// 10 minutes. Avoid to delete multi threads one by one, let MmsSmsProvider
                                /// handle this action. @{
                                String[] threadIds = new String[mThreadIds.size()];
                                int i = 0;
                                for (long thread : mThreadIds) {
                                    threadIds[i++] = String.valueOf(thread);
                                    DraftCache.getInstance().setDraftState(thread, false);
                                }
                                Conversation.startMultiDelete(mHandler, token, mDeleteRejectedMessages,
                                        threadIds, mMaxMmsId, mMaxSmsId);
                                /// @}
                            }
                            MmsLog.d(TAG, "after delete threads in conversationList");
                            /// @}
                        }

                        /// M: Code analyze 013, For bug ALPS00046358 , The method about the handler
                        /// with progress dialog functio . @{
                        private void showProgressDialog() {
                            if (mHandler instanceof BaseProgressQueryHandler) {
                                ((BaseProgressQueryHandler) mHandler).setProgressDialog(
                                        DeleteProgressDialogUtil.getProgressDialog(mContext));
                                ((BaseProgressQueryHandler) mHandler).showProgressDialog();
                            }
                        }
                        /// @}
                    });
            dialog.dismiss();
        }
    }


    private final Runnable mDeleteObsoleteThreadsRunnable = new Runnable() {
        @Override
        public void run() {
            if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
                LogTag.debug("mDeleteObsoleteThreadsRunnable getSavingDraft(): " +
                        DraftCache.getInstance().getSavingDraft());
            }
            if (DraftCache.getInstance().getSavingDraft()) {
                // We're still saving a draft. Try again in a second. We don't want to delete
                // any threads out from under the draft.
                mHandler.postDelayed(mDeleteObsoleteThreadsRunnable, 1000);
            } else {
                /// M: Code analyze 024, For bug ALPS00234739 , draft can't be
                /// saved after share the edited picture to the same ricipient, so
                ///Remove old Mms draft in conversation list instead of compose view . @{
                MessageUtils.asyncDeleteOldMms();
                /// @}
                Conversation.asyncDeleteObsoleteThreads(mQueryHandler,
                        DELETE_OBSOLETE_THREADS_TOKEN);
            }
        }
    };

    //qc:
    //private final class ThreadListQueryHandler extends ConversationQueryHandler {
    //mtk:
    private final class ThreadListQueryHandler extends BaseProgressQueryHandler {
        public ThreadListQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        // Test code used for various scenarios where its desirable to insert a delay in
        // responding to query complete. To use, uncomment out the block below and then
        // comment out the @Override and onQueryComplete line.
//        @Override
//        protected void onQueryComplete(final int token, final Object cookie, final Cursor cursor) {
//            mHandler.postDelayed(new Runnable() {
//                public void run() {
//                    myonQueryComplete(token, cookie, cursor);
//                    }
//            }, 2000);
//        }
//
//        protected void myonQueryComplete(int token, Object cookie, Cursor cursor) {

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
             //lichao move down to THREAD_LIST_QUERY_TOKEN in 2017-06-08 begin
              /// M: Code analyze 015, For bug,  add cursor == null check . @{
              /*MmsLog.d(TAG, "onQueryComplete mNeedQuery = " + mNeedQuery +
                            " mIsInActivity = " + mIsInActivity);
              if (cursor == null) {
              /// M: Decrease query counter and do next query if any request  @{
                  setProgressBarIndeterminateVisibility(false);
                  if (mNeedQuery && mIsInActivity) {
                      MmsLog.d(TAG, "onQueryComplete cursor == null startAsyncQuery");
                      startAsyncQuery();
                  }
                  return;
              /// @}
              }*/
            /// @}
            //lichao move down to THREAD_LIST_QUERY_TOKEN in 2017-06-08 end
            switch (token) {
                case THREAD_LIST_QUERY_TOKEN:
                    Log.d(TAG, "\n ThreadListQueryHandler, onQueryComplete, case THREAD_LIST_QUERY_TOKEN");
                    //lichao move down to here in 2017-06-08 begin
                    /// M: Code analyze 015, For bug,  add cursor == null check . @{
                    MmsLog.d(TAG, "onQueryComplete(), mNeedQuery = " + mNeedQuery +
                            ", mIsInActivity = " + mIsInActivity);
                    if (cursor == null) {
                        /// M: Decrease query counter and do next query if any request  @{
                        setProgressBarIndeterminateVisibility(false);
                        if (mNeedQuery && mIsInActivity) {
                            MmsLog.d(TAG, "onQueryComplete, cursor is null, startAsyncQuery");
                            startAsyncQuery();
                        }
                        //HB. Comments :  , Engerineer : lichao , Date : 17-5-20 , begin
                        mListAdapter.changeCursor(null);
                        mQueryStatus = QueryStatus.AFTER_QUERY;
                        updateEmptyView();
                        updateFooterView();
                        if (DEBUG) Log.v(TAG, "onQueryComplete(), cursor is null, mNeedShowDefaultAppDialog = " + mNeedShowDefaultAppDialog);
                        if (!mIsDoInBackground && !sIsDeleting && !mIsRebuildingIndex
                                && mNeedShowDefaultAppDialog && !MmsConfig.isSmsEnabled(mContext)) {
                            showDefaultSmsAppDialog();
                        }
                        //HB. end
                        MmsLog.w(TAG, "onQueryComplete, cursor is null, updateEmptyView, return");
                        return;
                        /// @}
                    }
                    /// @}
                    //lichao move down to here in 2017-06-08 end

                    /// M: If no listener for content change, means no need to refresh list @{
                    if (mListAdapter.getOnContentChangedListener() == null) {
                        cursor.close();
                        Log.d(TAG, "onQueryComplete: none ContentChangedListener, return");
                        return;
                    }
                    /// @}
                    //MmsLog.d(TAG, "onQueryComplete cursor count is " + cursor.getCount());
                    /// M: add for ipmessage, update Empty View
                    //mIpConvList.onIpUpdateEmptyView(cursor);
                    //updateEmptyView(cursor);
                    HashSet<String> blackNumSet = MessageUtils.getBlacklistSet(mContext);
                    mListAdapter.setBlackNumSet(blackNumSet);
                    if(DEBUG) Log.i(TAG, "onQueryComplete(), changeCursor start time ["
                            + System.currentTimeMillis() + "]");
                    PDebug.EndAndStart("startAsyncQuery()", "onQueryComplete -> changeCursor");
                    mListAdapter.changeCursor(cursor);

                    //HB. Comments : add for setTop , Engerineer : lichao , Date : 17-6-5 , begin
                    if(true == mNeedSmoothScrollToTop){
                        mNeedSmoothScrollToTop = false;
                        mListView.smoothScrollToPosition(0);
                    }else if(true == mNeedFastScrollToTop){
                        mNeedFastScrollToTop = false;
                        mListView.requestFocusFromTouch();
                        mListView.setSelection(0);
                    }
                    //HB. end

                    /// M: make a timer to update the list later, the time should update.
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            /// M: if only has header, not notify
                            //if (!mIpConvList.onIpQueryCompleteQueryList(mListView)) {
                            mListAdapter.notifyDataSetChanged();
                            //}
                        }
                    }, 60000);

                /*
				//lichao delete
                //if (!MmsConfig.isActivated(ConversationList.this)) {
                    if (mListAdapter.getCount() == 0 && getListView().getEmptyView() instanceof TextView) {
                        ((TextView) (getListView().getEmptyView())).setText(R.string.no_conversations);
                    }
                //}
				*/
                    //lichao add begin
                    mQueryStatus = QueryStatus.AFTER_QUERY;
                    updateEmptyView();
                    updateFooterView();
                    //lichao add end

                    /** M: add code @{ */
                    if (!Conversation.isInitialized()) {
                        Conversation.init(getApplicationContext());
                    } else {
                        Conversation.removeInvalidCache(cursor);
                    }
                    /** @} */

                    if (mDoOnceAfterFirstQuery) {
                        mDoOnceAfterFirstQuery = false;
                        /// M: Code analyze 016, For new feature, wappush: method is changed . @{
                        //Conversation.markAllConversationsAsSeen(getApplicationContext(),
                        //        Conversation.MARK_ALL_MESSAGE_AS_SEEN);

                        // 1. Delete any obsolete threads. Obsolete threads are threads that aren't
                        // referenced by at least one message in the pdu or sms tables. We only call
                        // this on the first query (because of mDoOnceAfterFirstQuery).
                        mHandler.post(mDeleteObsoleteThreadsRunnable);

                        // 2. Mark all the conversations as seen.
                        //original
                        //Conversation.markAllConversationsAsSeen(getApplicationContext());
                        //mtk
                        Conversation.markAllConversationsAsSeen(getApplicationContext(), Conversation.MARK_ALL_MESSAGE_AS_SEEN);
                    }

                    /// M: Code analyze 005, For new feature ALPS00247476 . @{
                    //lichao delete in 2017-03-26
                    //if (mActionMode != null) {
                    //    mActionModeListener.updateActionMode();
                    //}
                    /// @}
                    /// M: Fix bug ALPS00416081
                    //setDeleteMenuVisible(mOptionsMenu);

                    /// Google JB MR1.1 patch. conversation list can restore scroll position
                    if (mSavedFirstVisiblePosition != AdapterView.INVALID_POSITION) {
                        // Restore the list to its previous position.
                        //lichao modify: getListView()
                        mListView.setSelectionFromTop(mSavedFirstVisiblePosition,
                                mSavedFirstItemOffset);
                        mSavedFirstVisiblePosition = AdapterView.INVALID_POSITION;
                    }

                    //lichao add begin
                    if(DEBUG) Log.v(TAG, "onQueryComplete(), mIsDoInBackground = " + mIsDoInBackground);
                    if(DEBUG) Log.v(TAG, "onQueryComplete(), sIsDeleting = " + sIsDeleting);
                    if(DEBUG) Log.v(TAG, "onQueryComplete(), mIsRebuildingIndex = " + mIsRebuildingIndex);
                    if(DEBUG) Log.v(TAG, "onQueryComplete(), mNeedEnterPickNumMode = " + mNeedEnterPickNumMode);
                    if(DEBUG) Log.v(TAG, "onQueryComplete(), mNeedShowDefaultAppDialog = " + mNeedShowDefaultAppDialog);
                    //It is showing progressDialog when IsDoInBackground in EditMode,
                    //After dismissProgressDialog and QuitDeleteMode, it will call startAsyncQuery
                    //!isEditMode_EditThread() &&
                    if (!mIsDoInBackground && !sIsDeleting && !mIsRebuildingIndex) {
                        //不是默认短信也可以正常PickNum
                        if (mNeedEnterPickNumMode) {
                            showEditMode();
                            updateActionMode();
                            mListAdapter.notifyDataSetChanged();
                        } else if (mNeedShowDefaultAppDialog && !MmsConfig.isSmsEnabled(mContext)) {
                            showDefaultSmsAppDialog();
                        }
                    }
                    //lichao add end
                    break;

            /*
			//lichao delete
            case UNREAD_THREADS_QUERY_TOKEN:
                int count = 0;
                if (cursor != null) {
                    try {
                        if (cursor.getCount() > 0) {
                            cursor.moveToFirst();
                            count = cursor.getInt(0);
                        }
                        count = mIpConvList.onIpGetUnreadCount(cursor, count);
                        MmsLog.d(TAG, "get threads unread message count = " + count);
                        /// @}
                    } finally {
                        cursor.close();
                    }
                }
                /// M: modified for unread count display
                if (count > MAX_DISPLAY_UNREAD_COUNT) {
                    mUnreadConvCount.setText(DISPLAY_UNREAD_COUNT_CONTENT_FOR_ABOVE_99);
                } else {
                    mUnreadConvCount.setText(count > 0 ? Integer.toString(count) : null);
                }
                break;
                */

                //case HAVE_LOCKED_MESSAGES_TOKEN:
                case HAVE_REJECTED_MESSAGES_TOKEN:
                    /// M: add a log
                    Log.d(TAG, "\n ThreadListQueryHandler, onQueryComplete, case HAVE_REJECTED_MESSAGES_TOKEN");
                    //lichao merge from qcmms begin
                    if (ConversationList.this.isFinishing()) {
                        Log.w(TAG, "onQueryComplete, ConversationList Activity is isFinishing, do nothing ");
                        if (cursor != null) {
                            cursor.close();
                        }
                        return;
                    }
                    //lichao merge from qcmms end

                    @SuppressWarnings("unchecked")
                    Collection<Long> threadIds = (Collection<Long>) cookie;

                    /// M: unwrap adapter if ListView has header @{
                    //lichao delete
                    /*
                    ListView listView = getListView();
                    ConversationListAdapter adapter;
                    if (mIpConvList.onIpQueryComplete(listView) != null) {
                        adapter = (ConversationListAdapter) mIpConvList.onIpQueryComplete(listView);
                    } else {
                        adapter = (ConversationListAdapter) listView.getAdapter();
                    }
                    */
                    if (mListAdapter != null && threadIds != null) {
                        Cursor c = mListAdapter.getCursor();
                        if (c != null && c.getCount() == threadIds.size()) {
                            //如果list列表的总数等于被选择列表的总数，那么就全部一次删除
                            if(DEBUG) Log.d(TAG, "onQueryComplete, list count == selected size, so deleteAll ");
                            threadIds = null;
                        }
                    }
                    //@}
                    boolean hasRejectedMessages = cursor != null && cursor.getCount() > 0;
                    if(DEBUG) Log.d(TAG, "onQueryComplete, hasRejectedMessages = "+hasRejectedMessages);

                    DeleteThreadListener deleteListener = new DeleteThreadListener(threadIds, mQueryHandler, mContext);
                    confirmDeleteThreadDialog(
                            deleteListener,
                            threadIds,
                            hasRejectedMessages,
                            mContext);
                    if (cursor != null) {
                        cursor.close();
                    }
                    break;

                default:
                    Log.e(TAG, "onQueryComplete called with unknown token " + token);
            }

            /// M: if only has header, show listview
            //mIpConvList.onIpQueryCompleteEnd(mListView, mHandler, mListAdapter);

            /// M: Do next query if any requested @{
            if (mNeedQuery && mIsInActivity) {
                startAsyncQuery();
            }
            /// @}
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            /// M: comment it
            //super.onDeleteComplete(token, cookie, result);
            if (DEBUG) Log.d(TAG, "==onDeleteComplete()==");
			//mtk add
            sIsDeleting = false;
            switch (token) {
                case DELETE_CONVERSATION_TOKEN:
                    Log.d(TAG, "\n ThreadListQueryHandler, onDeleteComplete, case DELETE_CONVERSATION_TOKEN");
                    long threadId = cookie != null ? (Long) cookie : -1;     // default to all threads
                    //lichao merge from qcmms begin
                    if (threadId < 0 || threadId == mLastDeletedThread) {
                    /*mHandler.removeCallbacks(mShowProgressDialogRunnable);
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }*/
                        mLastDeletedThread = -1;
                        //lichao add in 2016-11-15 for interactive design requirements
                        Toast.makeText(ConversationList.this, ConversationList.this.getResources()
                                .getString(R.string.conversations_delete_completed), Toast.LENGTH_SHORT).show();
                    }
                    //lichao merge from qcmms end
                    ///M add for CMCC performance auto test case
                    Log.i(TAG, "[CMCC Performance test][Message] delete message end [" + System.currentTimeMillis() + "]" + "threadId=" + threadId);
                    if (threadId == -1) {
                        // Rebuild the contacts cache now that all threads and their associated unique
                        // recipients have been deleted.
                        Contact.init(getApplication());
                        ///M: add for fix bug ALPS01071334. after delete all threads, refresh DraftCache
                        DraftCache.getInstance().refresh();
                    }
                    /// M: threadId == -2 is multidelete. for fix bug ALPS01071334
                    else if (threadId != -2) {
                        // Remove any recipients referenced by this single thread from the
                        // contacts cache. It's possible for two or more threads to reference
                        // the same contact. That's ok if we remove it. We'll recreate that contact
                        // when we init all Conversations below.
                        Conversation conv = Conversation.get(ConversationList.this, threadId, false);
                        if (conv != null) {
                            ContactList recipients = conv.getRecipients();
                            for (Contact contact : recipients) {
                                contact.removeFromCache();
                            }
                        }
                    }
                    // Make sure the conversation cache reflects the threads in the DB.
                    Conversation.init(getApplicationContext());
                    //mtk code begin
                    //this for exit select mode
                    //if (mActionMode != null) {
                    if (isActionMode()) {
                        mHandler.postDelayed(new Runnable() {
                            public void run() {
                                /*if (mActionMode != null && !isFinishing()) {
                                    mActionMode.finish();
                                }*/
                                mQueryStatus = QueryStatus.AFTER_QUERY;
                                changeToNormalMode();
                            }
                        }, 300);
                    }

                    //lichao delete unknown means codes
                /*try {
                    if (TelephonyManagerEx.getDefault().isTestIccCard(0)) {
                        MmsLog.d(TAG, "All threads has been deleted, send notification..");
                            SmsManager
                                    .getSmsManagerForSubscriptionId(
                                SmsReceiverService.sLastIncomingSmsSubId).getDefault().setSmsMemoryStatus(true);
                    }
                } catch (Exception ex) {
                    MmsLog.e(TAG, " " + ex.getMessage());
                }*/
                    //mtk code end

                    // Update the notification for new messages since they
                    // may be deleted.
                    MessagingNotification.nonBlockingUpdateNewMessageIndicator(ConversationList.this,
                            MessagingNotification.THREAD_NONE, false);
                    // Update the notification for failed messages since they
                    // may be deleted.
                    MessagingNotification.nonBlockingUpdateSendFailedNotification(ConversationList.this);
                    /// M: update download failed messages since they may be deleted too.
                    MessagingNotification.updateDownloadFailedNotification(ConversationList.this);

                    /// M: Code analyze 001, For new feature ALPS00131956,
                    /// wappush: Update the notification for new WAP Push/CB
                    /// messages. @{
                    if (FeatureOption.MTK_WAPPUSH_SUPPORT) {
                        WapPushMessagingNotification.nonBlockingUpdateNewMessageIndicator(ConversationList.this,
                                WapPushMessagingNotification.THREAD_NONE);
                    }
                    /// @}
                    /// M: Code analyze 006, For bug ALPS00291435, solve no
                    /// response while deleting 50 messages . @{
                    CBMessagingNotification.updateNewMessageIndicator(ConversationList.this);
                    /// @}
                    // Make sure the list reflects the delete
                    /// M: comment this line
                    // startAsyncQuery();.-
                    /** M: fix bug ALPS00357750 @{ */
                    dismissProgressDialog();
                    /** @} */
                    /** M: show a toast
                     if (DeleteThreadListener.sDeleteNumber > 0) {
                     int count = DeleteThreadListener.sDeleteNumber;
                     String toastString = ConversationList.this.getResources().getQuantityString(
                     R.plurals.deleted_conversations, count, count);
                     Toast.makeText(ConversationList.this, toastString, Toast.LENGTH_SHORT).show();
                     DeleteThreadListener.sDeleteNumber = 0;
                     }
                     */
                    /// M: google android4.2 patch
                    //lichao delete for no need show MmsWidget
                    //MmsWidgetProvider.notifyDatasetChanged(getApplicationContext());
                    /// @}
                    //lichao add for not showDefaultSmsAppDialog  when sIsDeleting in 2017-07-15 begin
                    if(DEBUG) Log.v(TAG, "onDeleteComplete(), mNeedShowDefaultAppDialog = " + mNeedShowDefaultAppDialog);
                    if (!mIsDoInBackground && !sIsDeleting && !mIsRebuildingIndex
                            && mNeedShowDefaultAppDialog && !MmsConfig.isSmsEnabled(mContext)) {
                        showDefaultSmsAppDialog();
                    }
                    //lichao add for not showDefaultSmsAppDialog  when sIsDeleting in 2017-07-15 end
                    break;

                case DELETE_OBSOLETE_THREADS_TOKEN:
                    // Nothing to do here.
                    MmsLog.d(TAG, "DraftManager.sEditingThread.clear()");
                    DraftManager.sEditingThread.clear();
                    break;
            }
        }
    }

    //qcom, lichao modify
    //private ProgressDialog createProgressDialog() {
    private ProgressDialog createProgressDialog(CharSequence message) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setIndeterminate(true);
        //dialog.setMax(1); //default is one complete //lichao add for test
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        //dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);//lichao add for test
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        //dialog.setInformationVisibility(false);//lichao add for test
        //dialog.setProgress(mDeleteThreadCount);//lichao add for test
        //dialog.setMessage(getText(R.string.deleting_threads));
        dialog.setMessage(message);
        return dialog;
    }

    /*
	//qcom
	private Runnable mDeletingRunnable = new Runnable() {
        @Override
        public void run() {
            //mHandler.postDelayed(mShowProgressDialogRunnable, DELAY_TIME);
        }
    };*/

    private Runnable mShowProgressDialogRunnable = new Runnable() {
        @Override
        public void run() {
            //通过mIsDoInBackground和延时来实现只有超过300ms才弹框
            //避免进度框闪现一下，完全没看清楚就又自动消失了。
            if (mProgressDialog != null && !mProgressDialog.isShowing()
                    && (true == mIsDoInBackground)) {
                mProgressDialog.show();
            }
        }
    };

    //lichao add
    private void dismissProgressDialog(){
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    //lichao add in 2017-06-20
    private Runnable mShowRebuildingDialogRunnable = new Runnable() {
        @Override
        public void run() {
            if (mRebuildingDialog != null&& !mRebuildingDialog.isShowing()) {
                mRebuildingDialog.show();
            }
        }
    };

    //lichao add
    private void dismissRebuildingDialog(){
        if (mRebuildingDialog != null && mRebuildingDialog.isShowing()) {
            mRebuildingDialog.dismiss();
        }
    }

    //lichao add
    private Runnable mQuitDeleteModeRunnable = new Runnable() {
        @Override
        public void run() {
            //safeQuitDeleteMode();
            changeToNormalMode();
        }
    };

    /// M: Code analyze 005, For new feature ALPS00247476, replace multichoicemode by longClickListener . @{
	/*
    private class ModeCallback implements ActionMode.Callback {
        private View mMultiSelectActionBarView;
        /// M:
        private Button mSelectionTitle;
        //private TextView mSelectedConvCount;
        private HashSet<Long> mSelectedThreadIds;

        private MenuItem mDeleteItem;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = getMenuInflater();
            /// M: Optimize select all performance, restore actionmode status. @{
            mListAdapter.clearstate();
            // M: add for ALPS01988446, dismiss search view.
            mSearchItem.collapseActionView();
            if (mIsNeedRestoreAdapterState) {
                for (int i = 0; i < mListSelectedThreads.length; i++) {
                    mListAdapter.setSelectedState(mListSelectedThreads[i]);
                }
                mIsNeedRestoreAdapterState = false;
            } else {
                Log.d(TAG, "onCreateActionMode: no need to restore adapter state");
            }
            /// @}
            mSelectedThreadIds = new HashSet<Long>();
            inflater.inflate(R.menu.conversation_multi_select_menu_with_selectall, menu);

            mDeleteItem = menu.findItem(R.id.delete);

            if (mMultiSelectActionBarView == null) {
                mMultiSelectActionBarView = LayoutInflater.from(ConversationList.this)
                    .inflate(R.layout.conversation_list_multi_select_actionbar2, null);
                /// M: change select tips style
                mSelectionTitle = (Button) mMultiSelectActionBarView.findViewById(R.id.selection_menu);
                //mSelectedConvCount =
                //    (TextView)mMultiSelectActionBarView.findViewById(R.id.selected_conv_count);
            }
            mode.setCustomView(mMultiSelectActionBarView);
            ((Button) mMultiSelectActionBarView.findViewById(R.id.selection_menu))
                .setText(R.string.select_conversations);
            /// M: Code analyze 008, For bug ALPS00250948, disable search in
            // multi-select status . @{
            mDisableSearchFalg = true;
            /// @}
            /// M: Code analyze 005, For new feature ALPS00247476, set long clickable . @{
            getListView().setLongClickable(false);
            /// @}
            mIpConvList.onIpCreateActionMode(mode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (mMultiSelectActionBarView == null) {
                ViewGroup v = (ViewGroup)LayoutInflater.from(ConversationList.this)
                    .inflate(R.layout.conversation_list_multi_select_actionbar2, null);
                mode.setCustomView(v);
                /// M: change select tips style
                mSelectionTitle = (Button) mMultiSelectActionBarView.findViewById(R.id.selection_menu);
                //mSelectedConvCount = (TextView)v.findViewById(R.id.selected_conv_count);

            }
            /// M: redesign selection action bar and add shortcut in common version. @{
            if (mCustomMenu == null) {
                mCustomMenu = new CustomMenu(ConversationList.this);
            }
            mSelectionMenu = mCustomMenu.addDropDownMenu(mSelectionTitle, R.menu.selection);
            mSelectionMenuItem = mSelectionMenu.findItem(R.id.action_select_all);
            mCustomMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    if (mSelectionTitle != null) {
                        mSelectionTitle.setEnabled(false);
                    }
                    if (mListAdapter.getCount() == mListAdapter.getSelectedThreadsList().size()) {
                        setAllItemChecked(mActionMode, false);
                    } else {
                        setAllItemChecked(mActionMode, true);
                    }
                    return false;
                }
            });
            /// @}
            mIpConvList.onIpPrepareActionMode(mode, menu);
            return true;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
            mIpConvList.onIpActionItemClicked(mode, item, mSelectedThreadIds);
            switch (item.getItemId()) {
                case R.id.delete:
                    if (mSelectedThreadIds.size() > 0) {
                        Log.v(TAG, "ConversationList->ModeCallback: delete");
                        if (mDeleteAlertDialog != null && mDeleteAlertDialog.isShowing()) {
                            MmsLog.d(TAG, "no need to show delete dialog");
                        } else {
                            confirmDeleteThreads(mSelectedThreadIds, mQueryHandler);
                        }
                    } else {
                        item.setEnabled(false);
                    }
                    break;

                default:
                    break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            /// M: Code analyze 008, For bug ALPS00250948, disable search in multi-select status . @{
            mDisableSearchFalg = false;
            /// @}
            mListAdapter.clearstate();
            /// M: Code analyze 005, For new feature ALPS00247476, add selectAll/unSelectAll . @{
            getListView().setLongClickable(true);
            mSelectionMenu.dismiss();
            mActionMode = null;
            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }
            /// @}
        }

        public void setItemChecked(int position, boolean checked, Cursor cursor) {
            ListView listView = getListView();
            if (cursor == null) {
                cursor = (Cursor) listView.getItemAtPosition(position);
            } else {
                cursor.moveToPosition(position);
            }
            long threadId = cursor.getLong(0);
            boolean isChecked = mListAdapter.isContainThreadId(threadId);
            if (checked == isChecked) {
                return;
            }
            if (checked) {
                mListAdapter.setSelectedState(threadId);
            } else {
                mListAdapter.removeSelectedState(threadId);
            }
        }
        /// @}

        private void updateActionMode() {
            mSelectedThreadIds = mListAdapter.getSelectedThreadsList();
            int checkedNum = mSelectedThreadIds.size();
            /// M: Code analyze 018, For bug, enable or diable mDeleteItem menu . @{
            if (mDeleteItem != null) {
                if (checkedNum > 0) {
                    mDeleteItem.setEnabled(true);
                } else {
                    mDeleteItem.setEnabled(false);
                }
                /// @}
            }
            if (mSelectionTitle != null && !mSelectionTitle.isEnabled()) {
                mSelectionTitle.setEnabled(true);
            }
            /// M: exit select mode if no item select
            if (checkedNum <= 0 && mActionMode != null) {
                mActionMode.finish();
                ///M: add for fix ALPS01448613, when checkedNum == 0, dismiss the deleteAlertDialog. @{
                if (mDeleteAlertDialog != null && mDeleteAlertDialog.isShowing()) {
                    mDeleteAlertDialog.dismiss();
                    mDeleteAlertDialog = null;
                }
                /// @}
            }
            mSelectionTitle.setText(ConversationList.this.getResources().getQuantityString(
                    R.plurals.message_view_selected_message_count, checkedNum, checkedNum));
            if (mActionMode != null) {
                mActionMode.invalidate();
            }
            updateSelectionTitle();
            mIpConvList.onIpUpdateActionMode(mSelectedThreadIds);
        }

        /// M: Code analyze 005, For new feature ALPS00247476, select all messages . @{
        private void setAllItemChecked(ActionMode mode, final boolean checked) {
            mListAdapter.clearstate();
            if (checked) {
                mDeleteItem.setEnabled(false);
            }

            Cursor cursor = null;
//            String selection = null;
            // / M: ipmessage query.
            if (checked) {
                cursor = mIpConvList.onIpGetAllThreads();
                if (cursor == null) {
                    cursor = getContext().getContentResolver().query(Conversation.sAllThreadsUriExtend,
                            Conversation.ALL_THREADS_PROJECTION_EXTEND, null, null,
                            Conversations.DEFAULT_SORT_ORDER);
                }
                try {
                    if (cursor != null) {
                        MmsLog.d(TAG, "select all, cursor count is " + cursor.getCount());
                        for (int position = 0; position < cursor.getCount(); position++) {
                            setItemChecked(position, checked, cursor);
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }

            updateActionMode();
            // / M: Code analyze 018, For bug, enable or diable
            // mDeleteItem menu . @{
            if (checked) {
                mDeleteItem.setEnabled(true);
            } else {
                mDeleteItem.setEnabled(false);
            }
            // / @}

            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }
        }
        /// @}

    }
	*/

    private void log(String format, Object... args) {
        String s = String.format(format, args);
        Log.d(TAG, "[" + Thread.currentThread().getId() + "] " + s);
    }

    //mtk add
    //private boolean mIsShowSIMIndicator = true;
    /// M: Code analyze 003, For new feature ALPS00242732, SIM indicator UI is not good . @{
    //interface OnSubInforChangedListener
    /*@Override
    public void onSubInforChanged() {
        MmsLog.i(MmsApp.LOG_TAG, "onSimInforChanged(): Conversation List");
        /// M: show SMS indicator
        if (!isFinishing() && mIsShowSIMIndicator) {
            MmsLog.i(MmsApp.LOG_TAG, "Hide current indicator and show new one.");
            //mtk comment it
            //mStatusBarManager.hideSIMIndicator(getComponentName());
            //mStatusBarManager.showSIMIndicator(getComponentName(), Settings.System.SMS_SIM_SETTING);
            //lichao comment it
            //StatusBarSelectorCreator creator = StatusBarSelectorCreator
            //        .getInstance(ConversationList.this);
            //creator.updateStatusBarData();

        }
    }*/
    /// @}

    /// M: Code analyze 009, For bug ALPS00270910, Default SIM card icon shown
    /// in status bar is incorrect, need to get current sim information . @{
    //mtk add for used with onSubInforChanged()
    /*public static Activity getContext() {
        return sActivity;
    }*/
    /// @}

    /// M: Code analyze 005, For new feature ALPS00247476, long click Listenner . @{
	/*
    class ItemLongClickListener implements  ListView.OnItemLongClickListener {

        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            mActionMode = startActionMode(mActionModeListener);
            Log.e(TAG, "OnItemLongClickListener");
            mActionModeListener.setItemChecked(position, true, null);
            mActionModeListener.updateActionMode();
            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }
            return true;
        }
    }
	*/
    /// @}

    //mtk add
    /*private Runnable mResumeRunnable = new Runnable() {
        @Override
        public void run() {
            // / M: Code analyze 003, For new feature ALPS00242732, SIM
            // indicator UI is not good . @{
            //final ComponentName name = getComponentName();
            *//*
            //lichao comment it
            if (UserHandle.myUserId() == UserHandle.USER_OWNER) {
                mIsShowSIMIndicator = true;
                //mtk comment it
                //mStatusBarManager.hideSIMIndicator(name);
                //mStatusBarManager.showSIMIndicator(name, Settings.System.SMS_SIM_SETTING);
                StatusBarSelectorCreator.getInstance(ConversationList.this).showStatusBar();
            }
            *//*
            // / @}
            // / M: add for application guide. @{
*//*            IAppGuideExt appGuideExt = (IAppGuideExt) MmsPluginManager
                    .getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_APPLICATION_GUIDE);
            appGuideExt.showAppGuide("MMS");*//*
            // / @}
            mIsFirstCreate = false;
        }
    };*/

    @Override
    protected void onResume() {
        super.onResume();
        PDebug.EndAndStart("onStart -> onResume", "ConversationList.onResume");
        if (DEBUG) Log.d(TAG, "==onResume()==");
        // add for multi user
        isUserHasPerUsingMms = !UserManager.get(getApplicationContext()).hasUserRestriction(
                UserManager.DISALLOW_SMS);

        boolean isSmsEnabled = MmsConfig.isSmsEnabled(this);
        if (isSmsEnabled != mIsSmsEnabled) {
            mIsSmsEnabled = isSmsEnabled;
            /*if (!mIsFirstCreate) {
                invalidateOptionsMenu();
            }*/
        }
        // Multi-select is used to delete conversations. It is disabled if we are not the sms app.
        //ListView listView = getListView();
        //lichao refactor codes in 2017-07-15
        /*if (mIsSmsEnabled) {
            //lichao delete this and showDefaultSmsAppToast in OnItemLongClick()
            //if (mListView.getOnItemLongClickListener() == null) {
            //    mListView.setOnItemLongClickListener(this);
            //}
        } else {
            //mListView.setOnItemLongClickListener(null);
            //mtk code begin
            if (mActionMode != null) {
                mActionMode.finish();
                if (mDeleteAlertDialog != null && mDeleteAlertDialog.isShowing()) {
                    mDeleteAlertDialog.dismiss();
                    mDeleteAlertDialog = null;
                }
            }
            //mtk code end
        }*/
        //当mIsSmsEnabled为false时候不直接设置mNeedShowDefaultAppDialog为true是为了
        // 避免进入compose界面后返回会话界面再次弹出提示框
        //mNeedShowDefaultAppDialog = true;
        if(DEBUG) Log.v(TAG, "onResume(), mNeedShowDefaultAppDialog = " + mNeedShowDefaultAppDialog);
        if(!mIsSmsEnabled && !isEditMode_PickNumbers()){
            if (mDeleteAlertDialog != null && mDeleteAlertDialog.isShowing()) {
                mDeleteAlertDialog.dismiss();
                mDeleteAlertDialog = null;
            }
            if (mAddBlackConfirmDialog != null && mAddBlackConfirmDialog.isShowing()) {
                mAddBlackConfirmDialog.dismiss();
                mAddBlackConfirmDialog = null;
            }
            if (mRemoveBlackConfirmDialog != null && mRemoveBlackConfirmDialog.isShowing()) {
                mRemoveBlackConfirmDialog.dismiss();
                mRemoveBlackConfirmDialog = null;
            }
            //多选操作进行中或者正在删除，则不立即弹默认短信提示框
            //多选操作完成之后会重新查询整个会话，到查询完成或者删除完成的代码里再决定是否弹框
            if(mIsDoInBackground || sIsDeleting || mIsRebuildingIndex){
                if(DEBUG) Log.v(TAG, "onResume(), NeedShowDefaultAppDialog");
                mNeedShowDefaultAppDialog = true;
            }
        }

        // Show or hide the SMS promo banner
        /*if (mIsSmsEnabled || MmsConfig.isSmsPromoDismissed(this)) {
            mSmsPromoBannerView.setVisibility(View.GONE);
        } else {
            initSmsPromoBanner();
            mSmsPromoBannerView.setVisibility(View.VISIBLE);
        }*/

        ComposeMessageActivity.mDestroy = true;
        //mHandler.postDelayed(mResumeRunnable, 400);
        PDebug.End("ConversationList.onResume");
        // M: add for ALPS01846474, dismiss option menu if airplane mode changed.
        //registerReceiver(mAirPlaneReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
        //mOpConversationList.onResume(mIsSmsEnabled);
    }

    /// M: Code analyze 001, For new feature ALPS00242732, SIM indicator UI is not good . @{
    @Override
    protected void onPause() {
        //mHandler.removeCallbacks(mResumeRunnable);
        //mtk comment it
        //mStatusBarManager.hideSIMIndicator(getComponentName());
        //lichao comment it
        //StatusBarSelectorCreator.getInstance(ConversationList.this).hideStatusBar();
        //mIsShowSIMIndicator = false;
        super.onPause();
        if (DEBUG) Log.d(TAG, "==onPause()==");

        /// Google JB MR1.1 patch. conversation list can restore scroll position
        // Remember where the list is scrolled to so we can restore the scroll position
        // when we come back to this activity and *after* we complete querying for the
        // conversations.
        //ListView listView = getListView();
        mSavedFirstVisiblePosition = mListView.getFirstVisiblePosition();
        View firstChild = mListView.getChildAt(0);
        mSavedFirstItemOffset = (firstChild == null) ? 0 : firstChild.getTop();
        // M: add for ALPS01846474, dismiss option menu if airplane mode changed.
        //unregisterReceiver(mAirPlaneReceiver);
    }
    /// @}

    @Override
    protected void onDestroy() {
        /// M: Remove not start queries, and close the last cursor hold be adapter@{
        if (DEBUG) Log.d(TAG, "==onDestroy()==");

        if (!PermissionCheckUtil.checkRequiredPermissions(this)) {
            super.onDestroy();
            return;
        }

        if (mQueryHandler != null) {
            mQueryHandler.removeCallbacksAndMessages(null);
            mQueryHandler.cancelOperation(THREAD_LIST_QUERY_TOKEN);
            //mQueryHandler.cancelOperation(UNREAD_THREADS_QUERY_TOKEN);
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        if (mListAdapter != null) {
            MmsLog.d(TAG, "clear it");
            mListAdapter.clearstate();
            mListAdapter.setOnContentChangedListener(null);
            mListAdapter.changeCursor(null);
        }
        /// @}

        /// M: Code analyze 004, For bug ALPS00247476, ensure the scroll smooth . @{
        mScrollListener.destroyThread();
        /// @}

        //mIpConvList.onIpDestroy();

        if (mActionMode != null) {
            mActionMode = null;
        }
        mDeleteAlertDialog = null;
        mAddBlackConfirmDialog = null;
        mRemoveBlackConfirmDialog = null;
        mProgressDialog = null;
        mRebuildingDialog = null;

        //lichao comment it
        //unregisterReceiver(mSubReceiver);
        //unregisterReceiver(mStatusBarSelectorReceiver);
        unregisterReceiver(mBlackListChangeReceiver);
        super.onDestroy();
    }

    /// M: fix ALPS01535674,ALPS01597191. don't dimiss action mode when back from ComposeMessageActivity. @{
    private boolean mBackKeyisDown = false;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_BACK && !mBackKeyisDown) {
            MmsLog.d(TAG, "ignore action_up");
            mBackKeyisDown = false;
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            mBackKeyisDown = true;
        }
        return super.dispatchKeyEvent(event);
    }
    /// @}


    /// @}

    /// M: Code analyze 009, For new feature, plugin . @{
    //lichao comment it
    /*
    private void initPlugin(Context context) {
        mOpConversationList = OpMessageUtils.getOpMessagePlugin().getOpConversationListExt();
        mOpConversationList.init(this);
    }
    */
    /// @}

    /// M: Code analyze 013, For bug ALPS00046358 , The base class about the
    /// handler with progress dialog functio . @{
    public abstract static class BaseProgressQueryHandler extends AsyncQueryHandler {
        private NewProgressDialog mDialog;
        private int mProgress;

        public BaseProgressQueryHandler(ContentResolver resolver) {
            super(resolver);
        }

        /**
         * M:
         * Sets the progress dialog.
         *
         * @param dialog the progress dialog.
         */
        public void setProgressDialog(NewProgressDialog dialog) {
            // Patch back ALPS00457128 which the "deleting" progress display for a long time
            if (mDialog == null) {
                mDialog = dialog;
            }
        }

        /**
         * M:
         * Sets the max progress.
         *
         * @param max the max progress.
         */
        public void setMax(int max) {
            if (mDialog != null) {
                mDialog.setMax(max);
            }
        }

        /**
         * M:
         * Shows the progress dialog. Must be in UI thread.
         */
        public void showProgressDialog() {
            if (mDialog != null) {
                mDialog.show();
            }
        }

        /**
         * M:
         * Rolls the progress as + 1.
         *
         * @return if progress >= max.
         */
        protected boolean progress() {
            if (mDialog != null) {
                return ++mProgress >= mDialog.getMax();
            } else {
                return false;
            }
        }

        /**
         * M: fix bug ALPS00351620
         * Dismisses the progress dialog.
         */
        protected void dismissProgressDialog() {
            // M: fix bug ALPS00357750
            if (mDialog == null) {
                MmsLog.e(TAG, "mDialog is null!");
                return;
            }

            mDialog.setDismiss(true);
            try {
                mDialog.dismiss();
            } catch (IllegalArgumentException e) {
                // if parent activity is destroyed,and code come here, will happen this.
                // just catch it.
                MmsLog.d(TAG, "ignore IllegalArgumentException");
            }
            mDialog = null;
        }
    }

    /// @}

    /// M: Code analyze 009, For new feature, plugin . @{
    //implements method of interface IConversationListHost
    /*public void showSimSms() {
        int subCount = SubscriptionManager.from(this).getActiveSubscriptionInfoCount();
        if (subCount > 1) {
            Intent simSmsIntent = new Intent();
            simSmsIntent.setClass(this, SubSelectActivity.class);
            simSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            simSmsIntent.putExtra(SmsPreferenceActivity.PREFERENCE_KEY,
                    SettingListActivity.SMS_MANAGE_SIM_MESSAGES);
            simSmsIntent.putExtra(SmsPreferenceActivity.PREFERENCE_TITLE_ID,
                    R.string.pref_title_manage_sim_messages);
            startActivity(simSmsIntent);
        } else if (subCount == 1) {
            Intent simSmsIntent = new Intent();
            simSmsIntent.setClass(this, ManageSimMessages.class);
            simSmsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            simSmsIntent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, SubscriptionManager
                    .from(MmsApp.getApplication()).getActiveSubscriptionInfoList().get(0)
                    .getSubscriptionId());
            startActivity(simSmsIntent);
        } else {
            Toast.makeText(ConversationList.this, R.string.no_sim_1, Toast.LENGTH_SHORT).show();
        } 
    }*/

    //implements method of interface IConversationListHost
    /*public void changeMode() {
        FolderModeUtils.setMmsDirMode(true);
        MessageUtils.updateNotification(this);
        Intent it = new Intent(this, FolderViewList.class);
        it.putExtra("floderview_key", FolderViewList.OPTION_INBOX); // show inbox by default
        startActivity(it);
        finish();
    }*/
    /// @}

    /// M: add for ipmessage
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //mIpConvList.onIpActivityResult(requestCode, resultCode, data);
        if (DEBUG) Log.d(TAG, "==onActivityResult()==");
        return;
    }

    /// M: Fix bug ALPS00416081 @{
    /*private Menu mOptionsMenu;

    private void setDeleteMenuVisible(Menu menu) {
        if (menu != null) {
            MenuItem item = menu.findItem(R.id.action_delete_all);
            if (item != null) {
                mDataValid = mListAdapter.isDataValid();
                item.setVisible(mListAdapter.getCount() > 0 && mIsSmsEnabled);
            }
        }
    }*/
    /// @}

    /// M: redesign selection action bar and add shortcut in common version. @{
    /*
    //lichao delete for use toolbar replace actionbar
    private CustomMenu mCustomMenu;
    private DropDownMenu mSelectionMenu;
    private MenuItem mSelectionMenuItem;
    private void updateSelectionTitle() {
        if (mSelectionMenuItem != null) {
            if (mListAdapter.getCount() == mListAdapter.getSelectedThreadsList().size()) {
                mSelectionMenuItem.setTitle(R.string.unselect_all);
            } else {
                mSelectionMenuItem.setTitle(R.string.select_all);
            }
        }
    }
    */
    /// @}

    /// Google JB MR1.1 patch. conversation list can restore scroll position
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        /// M: Optimize select all performance, save actionmode status. @{
        if (isActionMode() && !sIsDeleting) {
            Log.d(TAG, "onSaveInstanceState: isActionMode");
            outState.putBoolean(ACTIONMODE, true);
            outState.putBoolean(NEED_RESTORE_ADAPTER_STATE, true);
            //lichao modify begin
            //HashSet<Long> selectedThreadSet = mListAdapter.getSelectedThreadsList();
            //Long[] selectList = (Long[]) selectedThreadSet.toArray(new Long[selectedThreadSet.size()]);
            Long[] selectList = mSelectedThreadIds.toArray(new Long[mSelectedThreadIds.size()]);
            //lichao modify end
            long[] selectedThreadsList = new long[selectList.length];
            for (int i = 0; i < selectList.length; i++) {
                selectedThreadsList[i] = selectList[i].longValue();
            }
            outState.putLongArray(SELECT_THREAD_IDS, selectedThreadsList);
            //Log.d(TAG, "onSaveInstanceState--selectThreadIds:" + Arrays.toString(selectedThreadsList));
        }
        /// @}
        outState.putInt(LAST_LIST_POS, mSavedFirstVisiblePosition);
        outState.putInt(LAST_LIST_OFFSET, mSavedFirstItemOffset);
    }

    /// M: Optimize select all performance, restore actionmode status. @{
    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        if (DEBUG) Log.d(TAG, "==onRestoreInstanceState()==");
        if (state.getBoolean(ACTIONMODE, false)) {
            mListSelectedThreads = state.getLongArray(SELECT_THREAD_IDS);
            //还需要完善
            //mActionMode = this.startActionMode(mActionModeListener);
        }
    }
    /// @}

    /// M
    private long getExceptId() {
        long exceptID = 0;
        for (long id : DraftManager.sEditingThread) {
            MmsLog.d(TAG, "getExceptId() id = " + id);
            if (id > exceptID) {
                exceptID = id;
            }
        }
        return exceptID;
    }
    /// @}

    /// M: fix bug ALPS00998351, solute the issue "All of the threads still
    /// highlight after you back to all thread view". @{
    public boolean isActionMode() {
        //return (mActionMode != null);
        return isEditMode();
    }
    /// @}


    //
    /*relate to: vendor/mediatek/proprietary/packages/apps/RCSe/RcsApp/src/com/mediatek/rcse/
    * plugin/message/RcseConversationList.java:    public IConversationListCallback mCallback;
    * The RCS-e stack is an open source implementation of the Rich Communication Suite standards for
    * Google Android platform. This implementation is compliant to GSMA RCS-e Blackbird standards.
    *
    */
    /*
    public class ConversationListCallback implements IConversationListCallback {

        @Override
        public void startIpQuery(String selection) {
          Conversation.startQueryExtend(mQueryHandler, THREAD_LIST_QUERY_TOKEN, selection);
          //Conversation.startQuery(mQueryHandler, UNREAD_THREADS_QUERY_TOKEN, Threads.READ + "=0"
          //        + " and " + selection);
        }

        @Override
        public void setEmptyViewVisible(int visible) {
            mEmptyViewDefault.setVisibility(visible);
        }

        @Override
        public void updateUnreadView(TextView ipUnreadView) {
            //mUnreadConvCount = ipUnreadView;
        }

        @Override
        public void notifyDataSetChanged() {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (mListAdapter != null) {
                        mListAdapter.notifyDataSetChanged();
                    }
                }
            });
        }

        @Override
        public void loadNormalLayout() {
            runOnUiThread(new Runnable() {
                public void run() {
                    //setupActionBar();
                    setTitle(R.string.app_label);
                }
            });
        }

        @Override
        public void startQuery() {
            startAsyncQuery();
        }

        //mtk comment it
//        @Override
//        public IIpContactExt updateGroupInfo(String number) {
//            Contact contact = Contact.get(number, false);
//            if (contact != null) {
//                contact.setName(null);
//                return contact.getIpContact(ConversationList.this);
//            }
//            return null;
//        }

        @Override
        public String getNumbersByThreadId(long threadId) {
            Conversation conversation = Conversation.get(
                    ConversationList.this, threadId, false);
            return TextUtils.join(",", conversation.getRecipients().getNumbers());
        }

        @Override
        public void invalidateGroupCache() {
            Contact.invalidateGroupCache();
        }
        
    }*/

    /**
     * M: add for ALPS01846474, dismiss option menu when AirPlane mode changed.
     */
    /*private final BroadcastReceiver mAirPlaneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            closeOptionsMenu();
        }
    };*/

    /*
    //lichao comment it
    private BroadcastReceiver mSubReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
                    if (!isFinishing() && mIsShowSIMIndicator) {
                        MmsLog.d(TAG, "Hide current indicator and show new one.");
                        StatusBarSelectorCreator creator = StatusBarSelectorCreator
                                .getInstance(ConversationList.this);
                        creator.updateStatusBarData();
                    }
            }
        }
    };
    */


    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    //lichao add in 2017-03-24 begin
    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    //lichao add begin
    public static Intent createNewContactIntent(String address) {
        // address must be a single recipient
        Intent intent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
        if (Mms.isEmailAddress(address)) {
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, address);
        } else {
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, address);
            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        return intent;
    }

    //lichao add
    private ActionModeListener mActionModeListener = new ActionModeListener() {
        @Override
        public void onActionItemClicked(Item item) {
            if(DEBUG) Log.d(TAG, "\n onActionItemClicked begin...");
            //因为执行多选操作任务后延迟300毫秒才弹框，如果在这300毫秒里去点击“全选/取消”按键则会出错
            if(true == mIsDoInBackground){
                if(DEBUG) Log.d(TAG, " onActionItemClicked, IsDoInBackground, return");
                return;
            }
            int checkedCount = mSelectedPos.size();
            if(DEBUG) Log.d(TAG, "onActionItemClicked, checkedCount = " + checkedCount);

            // mListAdapter.getCount返回的是包含HeaderView和FooterView
            // mListView.getAdapter().getCount返回的是包含HeaderView和FooterView
            int all = mListAdapter.getCount();
            if(DEBUG) Log.d(TAG, "onActionItemClicked, all = " + all);

            int needSelectBatch = getNextBatchNumber();
            if(DEBUG) Log.d(TAG, "onActionItemClicked, needSelectBatch = " + needSelectBatch);

            switch (item.getItemId()) {
                case ActionMode.POSITIVE_BUTTON:
                    if(DEBUG) Log.i(TAG, "case ActionMode.POSITIVE_BUTTON");
                    if(!isEditMode_PickNumbers() && !MmsConfig.isSmsEnabled(mContext)){
                        showDefaultSmsAppDialog();
                        if(DEBUG) Log.d(TAG, " onActionItemClicked, is not DefaultSmsApp, return");
                        return;
                    }
                    boolean isSelectAll;
                    if(needSelectBatch < 1){
                        if (checkedCount > 0 && checkedCount >= all) {
                            isSelectAll = false;//“全不选”
                        } else {
                            isSelectAll = true;//“全选”
                        }
                    }else{
                        if (checkedCount > 0 && checkedCount >= all) {
                            isSelectAll = false;//“全不选”
                        } else {
                            isSelectAll = true;//“选第n批”
                        }
                    }
                    selectAll(isSelectAll, needSelectBatch);
                    break;
                case ActionMode.NAGATIVE_BUTTON:
                    if(DEBUG) Log.i(TAG, "case ActionMode.NAGATIVE_BUTTON");
                    onBackPressed();
                    break;
                default:
            }
        }

        @Override
        public void onActionModeDismiss(ActionMode arg0) {
        }

        @Override
        public void onActionModeShow(ActionMode arg0) {
        }
    };

    //lichao add
    private void selectAll(boolean isSelectAll, int needSelectBatch) {
        if (DEBUG) Log.i(TAG, "\n selectAll() begin...");
        if (DEBUG) Log.i(TAG, "selectAll(), isSelectAll = "+isSelectAll);
        if (DEBUG) Log.i(TAG, "selectAll(), needSelectBatch = "+needSelectBatch);
        if (mListAdapter == null) {
            Log.e(TAG, "selectAll(), mListAdapter is null, finish!");
            ConversationList.this.finish();
            return;
        }
        clearSelectedList();
        initContainValues();
        if (isSelectAll) {
            calculateSelectedPos(needSelectBatch);
            if(isEditMode_PickNumbers()){
                updateActionMode_Task(true, false);
            }else{
                updateActionMode_Task(true, true);
            }
        } else {
            //作用跟mListAdapter.clearstate()重复，所以在isSelectAll里没调用uncheckAll也没出BUG
            //this will call conv.setIsChecked(false) for all conversations
            //mListAdapter.uncheckAll(mContext);
            updateActionMode();
            mListAdapter.notifyDataSetChanged();
        }
    }

    private void calculateSelectedPos(int needSelectBatch){
        int headerViewsCount = mListView.getHeaderViewsCount();
        //if(DEBUG) Log.d(TAG, "calculateSelectedPos(), headerViewsCount = " + headerViewsCount);
        Cursor cursor = mListAdapter.getCursor();
        //这个是Cursor的Position所以从0开始，而会话Item的Position从headerViewsCount开始
        int firstPosition = 0;
        if(needSelectBatch >= 1){
            firstPosition = 0+SELECT_ALL_MAX_SIZE*(needSelectBatch-1);
        }
        if(firstPosition < cursor.getCount() && cursor.moveToPosition(firstPosition)){
            if(needSelectBatch >= 1){
                mListView.requestFocusFromTouch();
                int selectionPos = firstPosition+headerViewsCount;
                if(needSelectBatch == 1){
                    View firstItem = mListView.getChildAt(1);
                    int firstItemOffset = (firstItem == null) ? 0 : firstItem.getTop();
                    mListView.setSelectionFromTop(selectionPos, firstItemOffset);
                }else{
                    mListView.setSelection(selectionPos);
                }
            }
            do {
                int position = cursor.getPosition();
                //position+headerViewsCount for position is start from HeaderView
                int addPosition = Integer.valueOf(position+headerViewsCount);
                //if(DEBUG) Log.d(TAG, "selectAll(), mSelectedPos.add: " + addPosition);
                mSelectedPos.add(addPosition);
                if (mSelectedPos.size() >= SELECT_ALL_MAX_SIZE) {
                    //if(DEBUG) Log.d(TAG, "selectAll(), break");
                    //showOutSizeToast(getString(R.string.select_all_maximum_reached, SELECT_ALL_MAX_SIZE));
                    break;
                }
            } while (cursor.moveToNext());
        }else{
            Log.e(TAG, "calculateSelectedPos(), invalide first cursor Position ");
        }
    }

    //HB. Comments :  , Engerineer : lichao , Date : 17-7-13 , begin
    private void updateActionMode_Task(final boolean needReorder, final boolean needPatchCheck) {
        if (DEBUG) Log.d(TAG, "\n\n updateActionMode_Task begin...");
        //AsyncTask<Params, Progress, Result>
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                if (DEBUG) Log.i(TAG, "updateActionMode_Task(), onPreExecute() called");
                mIsDoInBackground = true;
                if(mActionMode != null){
                    mActionMode.enableItem(ActionMode.POSITIVE_BUTTON, false);
                    mActionMode.enableItem(ActionMode.NAGATIVE_BUTTON, false);
                }
                if (DEBUG) Log.i(TAG, "updateActionMode_Task(), >>>postDelayed mShowProgressDialogRunnable");
                mHandler.postDelayed(mShowProgressDialogRunnable, DIALOG_DELAY_TIME);
                //HB. Comments : bug3723 , Engerineer : lichao , Date : 17-7-3 , begin
                setActionModeListener(null);
                mListView.setOnItemClickListener(null);
                //HB. end
            }
            //Result doInBackground(Params... params)
            @Override
            protected Void doInBackground(Void... params) {
                if (DEBUG) Log.i(TAG, "updateActionMode_Task(), doInBackground() called");
                if (DEBUG) Log.i(TAG, "updateActionMode_Task(), checked count = "+mSelectedPos.size());
                if (true == needReorder) {
                    sortSelectedPositions();
                    setSelectedConvListState();
                }
                if (true == needPatchCheck) {
                    if(mSelectedPos.size() <= PATCH_CHECK_MAX_SIZE){
                        mContainBlock = MessageUtils.isContainBlackNumThread(mContext, mSelectedThreadIds);
                        mContainUnblock = MessageUtils.isContainNormalNumThread(mContext, mSelectedThreadIds);
                    } else {
                        mContainBlock = true;
                        mContainUnblock = true;
                    }
                    mContainUnTop = hasSelectedUnToppedMessages();
                    mContainTop = hasSelectedToppedMessages();
                }
                return null;
            }
            //void onPostExecute(Result result)
            @Override
            protected void onPostExecute(Void result) {
                if (DEBUG) Log.i(TAG, "updateActionMode_Task(), onPostExecute() called");
                mIsDoInBackground = false;
                if(mActionMode != null){
                    mActionMode.enableItem(ActionMode.POSITIVE_BUTTON, true);
                    mActionMode.enableItem(ActionMode.NAGATIVE_BUTTON, true);
                }
                dismissProgressDialog();
                if (DEBUG) Log.i(TAG, "updateActionMode_Task(), mNeedEnterPickNumMode = "+mNeedEnterPickNumMode);
                if (mNeedEnterPickNumMode) {
                    //call showEditMode() again for reset data in EditMode
                    showEditMode();
                }
                updateActionMode();
                mListAdapter.notifyDataSetChanged();
                setActionModeListener(mActionModeListener);
                mListView.setOnItemClickListener(ConversationList.this);
                if (mNeedShowDefaultAppDialog && !isEditMode_PickNumbers() && !MmsConfig.isSmsEnabled(mContext)) {
                    showDefaultSmsAppDialog();
                }
            }
        };
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void setSelectedConvListState() {
        if(DEBUG) Log.d(TAG, "\n setSelectedConvListState begin...");
        //作用跟mListAdapter.clearstate()重复，所以这里不调用uncheckAll也没出BUG
        //this will call conv.setIsChecked(false) for all conversations
        //mListAdapter.uncheckAll(mContext);
        if(null == mSelectedPos || mSelectedPos.isEmpty()){
            if(DEBUG) Log.d(TAG, "setSelectedConvListState, SelectedPos isEmpty, return");
            return;
        }
        for (Integer position : mSelectedPos) {
            Cursor cursor = (Cursor) mListView.getItemAtPosition(position);
            //Cursor cursor = (Cursor) mListView.getAdapter().getItem(position);
            if (cursor == null) {
                Log.w(TAG, "setSelectedConvListState, cursor is null, continue, position="+position);
                continue;
            }
            Conversation conv = Conversation.from(mContext, cursor);
            if (conv == null) {
                Log.w(TAG, "setSelectedConvListState, conv is null, continue, position="+position);
                continue;
            }
            //这个跟ConversationListAdapter.java类的bindView()的conv.setIsChecked作用重复
            //conv.setIsChecked(true);
            long threadId = conv.getThreadId();
            mSelectedThreadIds.add(threadId);
            //this for judge checked_before in handleItemClick
            mListAdapter.setSelectedState(threadId);
            ContactList recipients = conv.getRecipients();
            if(!recipients.isEmpty()){
                mSelectedRecipients.add(recipients);
            }
        }
        if(DEBUG) Log.d(TAG, " setSelectedConvListState end...");
    }

    private void sortSelectedPositions(){
        if(DEBUG) Log.d(TAG, "\n sortSelectedPositions begin...");
        //Log.d(TAG, "sortSelectedPositions, mSelectedPos = "+mSelectedPos.toString());
        //Log.d(TAG, "sortSelectedPositions, mSelectedPos.size() = "+mSelectedPos.size());
        if(null == mSelectedPos || mSelectedPos.isEmpty()){
            if(DEBUG) Log.d(TAG, "sortSelectedPositions, SelectedPos isEmpty, return");
            return;
        }
        ArrayList<Integer> temp = new ArrayList<>(mSelectedPos);
        Collections.sort(temp, mComparatorByPos);
        mSelectedPos.clear();
        mSelectedPos.addAll(temp);
        //Log.d(TAG, "sortSelectedPositions, after sort, mSelectedPos = "+mSelectedPos.toString());
        if(DEBUG) Log.d(TAG, " sortSelectedPositions end...");
    }

    class ComparatorByPos implements Comparator {
        public int compare(Object obj1,Object obj2){
            int pos1 = ((Integer)obj1).intValue();
            int pos2 = ((Integer)obj2).intValue();
            if(pos1 > pos2) {
                return 1;
            }
            if(pos1 < pos2) {
                return -1;
            }
            return 0;
        }
    }
    private ComparatorByPos mComparatorByPos = new ComparatorByPos();
    //HB. Comments :  , Engerineer : lichao , Date : 17-7-13 , end

    //lichao add
    private void safeQuitDeleteMode() {
        try {
            Thread.sleep(300);
            changeToNormalMode();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //lichao add
    private void exitEditMode() {
        changeToNormalMode();
    }
    private void changeToNormalMode() {
        if (DEBUG) Log.d(TAG, "[changeToNormalMode] begin");
        /*if(mViewMode == ViewMode.NORMAL_MODE){
            return;
        }*/
        mViewMode = ViewMode.NORMAL_MODE;
        showActionMode(false);
        updateBottomNavigationVisibility();//hide Bottom menu
        updatePreSearchView();//set PreSearchView OnClickListener
        updateEmptyView();
        //updateHeaderView();
        updateFooterView();
        if (mActionMode != null) {
            mActionMode = null;
        }
        clearSelectedList();
        if(null != mSelectedPos){
            mSelectedPos = null;
        }
        if(null != mSelectedThreadIds){
            mSelectedThreadIds = null;
        }
        if(null != mSelectedRecipients){
            mSelectedRecipients = null;
        }
        //mListAdapter.uncheckAll(mContext);
        mListAdapter.setCheckBoxEnable(false);
        mListAdapter.notifyDataSetChanged();
        updateOrdinaryItemsVisible();//show menu_settings
        updateFloatActionButton();//show mFloatButton
        mEditModeType = EditModeType.NONE;
        mEditOperation = EditOperation.NONE;
        if (DEBUG) Log.d(TAG, "[changeToNormalMode] end");
    }

    //lichao add
    //ActionMode, DeleteMode, SelectMode
    private void showEditMode() {
        if (DEBUG) Log.d(TAG, "==[showEditMode] begin==");
        if (mListAdapter == null) {
            Log.e(TAG, "showEditMode(), mListAdapter is null, finish!");
            finish();
            return;
        }
        //lichao delete for BUG3837 in 2017-07-04
        /*if(mListAdapter.getCount() == 0){
            return;
        }*/
        /*if(mViewMode == ViewMode.EDIT_MODE){
            return;
        }*/
        mViewMode = ViewMode.EDIT_MODE;
        if(mNeedEnterPickNumMode){
            mNeedEnterPickNumMode = false;
            mEditModeType = EditModeType.PICK_NUMBERS;
        }else{
            mEditModeType = EditModeType.EDIT_THREAD;
        }
        updateOrdinaryItemsVisible();//hide menu_settings
        updatePreSearchView();//hide HeaderSearchView
        updateFloatActionButton();//hide mFloatButton second
        //updateHeaderView();
        updateFooterView();
        showActionMode(true);
        mActionMode = getActionMode();
        updateBottomNavigationVisibility();//show mBottomNavigationView
        if (null == mSelectedPos) {
            mSelectedPos = new CopyOnWriteArrayList<>();
        }
        if (null == mSelectedThreadIds) {
            //mSelectedThreadIds = new HashSet<>();
            mSelectedThreadIds = new CopyOnWriteArrayList<>();
        }
        if (null == mSelectedRecipients) {
            //mSelectedRecipients = new HashSet<>();
            mSelectedRecipients = new CopyOnWriteArrayList<>();
        }
        clearSelectedList();
        initContainValues();
        //lichao add for temp begin
        if (mIsNeedRestoreAdapterState) {
            for (int i = 0; i < mListSelectedThreads.length; i++) {
                //mtk
                mListAdapter.setSelectedState(mListSelectedThreads[i]);
                //lichao add
                mSelectedThreadIds.add(mListSelectedThreads[i]);
                //lichao add for blacklist in 2017-04-12 begin
//                ContactList recipients = conv.getRecipients();
//                if(!recipients.isEmpty()){
//                    mSelectedRecipients.add(recipients);
//                }
                //lichao add for blacklist in 2017-04-12 end
            }
            mIsNeedRestoreAdapterState = false;
        } else {
            if (DEBUG) Log.d(TAG, "showEditMode: no need to restore adapter state");
        }
        //lichao add for temp end
        mListAdapter.setCheckBoxEnable(true);
        mListAdapter.notifyDataSetChanged();
        if (DEBUG) Log.d(TAG, "==[showEditMode] end==");
    }

    //lichao add
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        if (DEBUG) Log.d(TAG, "\n [onItemClick], position = " + position);

        if (mListAdapter == null) {
            Log.e(TAG, "onItemClick(), mListAdapter is null, finish!");
            ConversationList.this.finish();
            return;
        }

        if (isSearchMode()) {
            return;
        }

        //lichao add in 2017-07-15
        if (!MmsConfig.isSmsEnabled(mContext) && isEditMode_EditThread()) {
            showDefaultSmsAppToast(R.string.edit_disabled_toast);
            return;
        }

        if (view instanceof ConversationListItem) {
            handleItemClick(parent, view, position, false);
        } else {
            Log.e(TAG, "onItemClick, view is not instanceof ConversationListItem");
        }
    }

    //lichao add
    private void updateActionMode() {
        if (DEBUG) Log.d(TAG, "updateActionMode()----begin");
        if (mListAdapter == null) {
            Log.e(TAG, "updateActionMode(), mListAdapter is null, finish!");
            finish();
            return;
        }
        //lichao move to showEditMode()
        /*if(mActionMode == null){
            mActionMode = getActionMode();
        }*/

        int nextBatchNum = getNextBatchNumber();

        String mSelectAllStr = getResources().getString(R.string.selected_all);
        String mDeselectAllStr = getResources().getString(R.string.deselected_all);
        String mSelectBatch = getResources().getString(R.string.selected_one_batch, nextBatchNum);

        //mSelectedThreadIds = mListAdapter.getSelectedThreadsList();//mtk
        if(null == mSelectedPos){
            Log.e(TAG, "updateActionMode(), mSelectedPos is null, return!");
            return;
        }
        int checkedCount = mSelectedPos.size();
        if (DBG_MENU) Log.d(TAG, "updateActionMode, checkedCount = " + checkedCount);

        int all = mListAdapter.getCount();
        if (DBG_MENU) Log.d(TAG, "updateActionMode, all = " + all);

        mActionMode.enableItem(ActionMode.POSITIVE_BUTTON, (all>0));

        if(nextBatchNum < 1){
            if (checkedCount > 0 && checkedCount >= all) {
                mActionMode.setPositiveText(mDeselectAllStr);
            } else {
                mActionMode.setPositiveText(mSelectAllStr);
            }
        }else{
            if (checkedCount > 0 && checkedCount >= all) {
                mActionMode.setPositiveText(mDeselectAllStr);
            } else {
                mActionMode.setPositiveText(mSelectBatch);
            }
        }
        //change to get mContainUnTop and mContainTop in updateActionMode_Task
        updateBottomMenuItems();

        //same as: getActionMode().setTitle(getString(R.string.selected_total_num, checkedCount));
        if (checkedCount <= 0) {
            updateActionModeTitle(getString(R.string.select_conversations));
        } else {
            if (nextBatchNum < 1) {
                updateActionModeTitle(getString(R.string.selected_total_num, checkedCount));
            } else {
                updateActionModeTitle(getString(R.string.zzz_selected_total_num, checkedCount + "/" + all));
            }
        }
        if (DEBUG) Log.d(TAG, "updateActionMode()----end");
    }

    //lichao add
    private void updateBottomMenuItems() {
        if (DBG_MENU) Log.d(TAG, "\n --updateBottomMenuItems()--begin");
        if(null == mSelectedPos){
            Log.w(TAG, "updateBottomMenuItems(), mSelectedPos is null, return");
            return;
        }
        int checkedCount = mSelectedPos.size();
        /*if (!MmsConfig.isSmsEnabled(this)) {
            showDefaultSmsAppToast(R.string.edit_disabled_toast);
        }*/
        boolean isEnable = (checkedCount > 0) /*&& MmsConfig.isSmsEnabled(this)*/;
        if (isEditMode_PickNumbers()) {
            //see zzz_conv_multi_pick_number_menu.xml
            //mBottomNavigationView.showItem(R.id.menu_pick_numbers, true);
            mBottomNavigationView.setItemEnable(R.id.menu_pick_numbers, checkedCount > 0);

        } else if (isEditMode_EditThread()) {
            //hb:menu="@menu/conversation_multi_select_menu"

            boolean showSetTop = (checkedCount <= 0) || mContainUnTop;
            //Log.d(TAG, "updateBottomMenuItems, showSetTop = " + showSetTop);
            mBottomNavigationView.showItem(R.id.menu_set_top, showSetTop);
            if (showSetTop) {
                mBottomNavigationView.setItemEnable(R.id.menu_set_top, isEnable);
            }

            boolean showCancelTop = mContainTop;
            mBottomNavigationView.showItem(R.id.menu_cancel_top, showCancelTop);
            if (showCancelTop) {
                mBottomNavigationView.setItemEnable(R.id.menu_cancel_top, isEnable);
            }

            boolean showAddToBlack = (checkedCount <= 0) || mContainUnblock;
            mBottomNavigationView.showItem(R.id.menu_add_to_black, showAddToBlack);
            if(showAddToBlack){
                mBottomNavigationView.setItemEnable(R.id.menu_add_to_black, isEnable);
            }

            boolean showMoveOutBlack = mContainBlock;
            mBottomNavigationView.showItem(R.id.menu_move_out_black, showMoveOutBlack);
            if(showMoveOutBlack){
                mBottomNavigationView.setItemEnable(R.id.menu_move_out_black, isEnable);
            }

            mBottomNavigationView.showItem(R.id.menu_delete_thread, true);
            mBottomNavigationView.setItemEnable(R.id.menu_delete_thread, isEnable);
        }
        if (DBG_MENU) Log.d(TAG, "--updateBottomMenuItems()--end");
    }

    //lichao add in 2017-07-12
    private void setItemsEnable(boolean enable){
        mBottomNavigationView.setItemEnable(R.id.menu_set_top, enable);
        mBottomNavigationView.setItemEnable(R.id.menu_cancel_top, enable);
        mBottomNavigationView.setItemEnable(R.id.menu_add_to_black, enable);
        mBottomNavigationView.setItemEnable(R.id.menu_move_out_black, enable);
        mBottomNavigationView.setItemEnable(R.id.menu_delete_thread, enable);
    }

    //lichao add
    /*private boolean isInDeleteMode() {
        //tangyisen modify
        //return getActionMode().isShowing();
        return isEditMode();
    }*/

    //lichao add
    private void initBottomNavigationView() {
        mBottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation_view);
        if (mNeedEnterPickNumMode) {
            mBottomNavigationView.clearItems();
            if(DBG_MENU) Log.d(TAG, "initBottomNavigationView(), inflateMenu zzz_conv_multi_pick_number_menu");
            mBottomNavigationView.inflateMenu(R.menu.zzz_conv_multi_pick_number_menu);
            mBottomNavigationView.setNavigationItemSelectedListener(mNavigationItemListenerForGetNumber);
        } else {
            mBottomNavigationView.clearItems();
            if(DBG_MENU) Log.d(TAG, "initBottomNavigationView(), inflateMenu conversation_multi_select_menu");
            mBottomNavigationView.inflateMenu(R.menu.conversation_multi_select_menu);
            mBottomNavigationView.setNavigationItemSelectedListener(mNavigationItemListener);
        }
    }

    //lichao add
    private void updateBottomNavigationVisibility() {
        //if (DEBUG) Log.d(TAG, "==updateBottomNavigationVisibility== begin");
        if (mBottomNavigationView != null) {
            if (isEditMode()) {
                if (DEBUG) Log.d(TAG, "updateBottomNavigationVisibility(), isEditMode");
                mBottomNavigationView.setVisibility(View.VISIBLE);
            } else {
                mBottomNavigationView.setVisibility(View.GONE);
            }
        }
        //if (DEBUG) Log.d(TAG, "==updateBottomNavigationVisibility== end");
    }

    //lichao add
    private OnNavigationItemSelectedListener mNavigationItemListener = new OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            if (null == mSelectedPos || mSelectedPos.isEmpty()) {
                return false;
            }
            if (!MmsConfig.isSmsEnabled(mContext)) {
                if(DEBUG) Log.d(TAG, " mNavigationItemListener, is not DefaultSmsApp, return");
                showDefaultSmsAppDialog();
                return false;
            }

            //lichao add for avoid deliberate touch in 2017-07-14
            setItemsEnable(false);

            switch (item.getItemId()) {
                //case R.id.delete:
                //see conversation_multi_select_menu.xml
                case R.id.menu_set_top:
                    if (DBG_MENU) Log.d(TAG, "menu_set_top clicked");
                    mEditOperation = EditOperation.SET_TOP_OPERATION;
                    markThreadsAsTop();
                    return true;

                case R.id.menu_cancel_top:
                    if (DBG_MENU) Log.d(TAG, "menu_cancel_top clicked");
                    mEditOperation = EditOperation.CANCEL_TOP_OPERATION;
                    cancelThreadsAsTop();
                    return true;

                case R.id.menu_add_to_black:
                    if (DBG_MENU) Log.d(TAG, "menu_add_to_black clicked");
                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            ConversationList.this);
                    builder.setCancelable(true);
                    builder.setTitle(R.string.besure_add_to_blacklist_text);
                    builder.setMessage(R.string.new_messages_will_be_intercepted);
                    builder.setPositiveButton(R.string.yes, getPositiveListener(true));
                    builder.setNegativeButton(R.string.no, getNegativeListener());
                    builder.setOnCancelListener(getCancelListener());
                    builder.setOnDismissListener(getDismissListener());
                    //builder.show();
                    mAddBlackConfirmDialog = builder.create();
                    mAddBlackConfirmDialog.show();
                    return true;

                case R.id.menu_move_out_black:
                    if (DBG_MENU) Log.d(TAG, "menu_move_out_black clicked");
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(
                            ConversationList.this);
                    builder2.setCancelable(true);
                    builder2.setTitle(R.string.besure_move_out_from_blacklist);
                    builder2.setMessage(R.string.recover_black_phone);
                    builder2.setPositiveButton(R.string.yes, getPositiveListener(false));
                    builder2.setNegativeButton(R.string.no, getNegativeListener());
                    builder2.setOnCancelListener(getCancelListener());
                    builder2.setOnDismissListener(getDismissListener());
                    //builder2.show();
                    mRemoveBlackConfirmDialog = builder2.create();
                    mRemoveBlackConfirmDialog.show();
                    return true;

                case R.id.menu_delete_thread:
                    if (DEBUG) Log.d(TAG, "menu_delete_thread clicked");
                    if (mDeleteAlertDialog != null && mDeleteAlertDialog.isShowing()) {
                        MmsLog.d(TAG, "no need to show delete dialog");
                    } else {
                        mEditOperation = EditOperation.DELETE_THREAD_OPERATION;
                        confirmDeleteThreads(mSelectedThreadIds, mQueryHandler);
                    }
                    return true;
                default:
                    return false;
            }
        }
    };

    private OnNavigationItemSelectedListener mNavigationItemListenerForGetNumber = new OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                //see zzz_conv_multi_pick_number_menu.xml
                case R.id.menu_pick_numbers:
                    if (null != mSelectedRecipients && !mSelectedRecipients.isEmpty()) {
                        mBottomNavigationView.setItemEnable(R.id.menu_pick_numbers, false);
                        sendSelectedNumbers_Task();
                        return true;
                    }
                    return false;
                default:
                    return false;
            }
        }
    };

    //lichao add
    /*private boolean hasSelectedUnreadMessages() {
        if (null == mSelectedThreadIds) {
            return false;
        }
        if (mSelectedThreadIds.size() <= 0) {
            return false;
        }
        Conversation conv = null;
        for (long tmpthreadId : mSelectedThreadIds) {
            conv = Conversation.get(ConversationList.this, tmpthreadId, false);
            if (conv.hasUnreadMessages()) {
                return true;
            }
        }
        return false;
    }*/

    //lichao add
    /*private void markThreadsAsRead(Collection<Long> threadIds) {
        for (long tmpthreadId : threadIds) {
            Conversation conv = Conversation.get(ConversationList.this, tmpthreadId, false);
            if (conv.hasUnreadMessages()) {
                conv.markAsRead();
            }
        }
    }*/

    //lichao add
    private boolean hasSelectedUnToppedMessages() {
        if (DEBUG) Log.d(ConversationList.TAG, "\n hasSelectedUnToppedMessages(), begin...");
        if (null == mSelectedThreadIds || mSelectedThreadIds.isEmpty()) {
            return false;
        }
        Conversation conv = null;
        for (long tmpthreadId : mSelectedThreadIds) {
            conv = Conversation.get(mContext, tmpthreadId, false);
            if (!conv.getTop()) {
                if (DEBUG) Log.d(ConversationList.TAG, " hasSelectedUnToppedMessages(), end, return true \n");
                return true;
            }
        }
        if (DEBUG) Log.d(ConversationList.TAG, " hasSelectedUnToppedMessages(), end, return false \n");
        return false;
    }

    //lichao add in 2017-07-12
    private boolean hasSelectedToppedMessages() {
        if (DEBUG) Log.d(ConversationList.TAG, "\n hasSelectedToppedMessages(), begin...");
        if (null == mSelectedThreadIds || mSelectedThreadIds.isEmpty()) {
            return false;
        }
        Conversation conv = null;
        for (long tmpthreadId : mSelectedThreadIds) {
            conv = Conversation.get(mContext, tmpthreadId, false);
            if (conv.getTop()) {
                if (DEBUG) Log.d(ConversationList.TAG, " hasSelectedToppedMessages(), end, return true \n");
                return true;
            }
        }
        if (DEBUG) Log.d(ConversationList.TAG, " hasSelectedToppedMessages(), end, return false \n");
        return false;
    }

    //lichao add
    private void markThreadsAsTop() {
        setThreadsTopValue(true);
    }

    private void cancelThreadsAsTop() {
        setThreadsTopValue(false);
    }

    private void setThreadsTopValue(final boolean isTop) {
        if (DEBUG) Log.d(TAG, "setThreadsTopValue(), begin...");
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                if (DEBUG) Log.i(TAG, "setThreadsTopValue(), onPreExecute() called");
                mIsBreakedByMax = false;
                mIsDoInBackground = true;
                //因为执行该任务后延迟300毫秒才弹框，如果在这300毫秒里去点击“全选/取消”按键则会出错
                if(mActionMode != null){
                    mActionMode.enableItem(ActionMode.POSITIVE_BUTTON, false);
                    mActionMode.enableItem(ActionMode.NAGATIVE_BUTTON, false);
                }
                mHandler.postDelayed(mShowProgressDialogRunnable, DIALOG_DELAY_TIME);
                mModifiedCout = 0;
            }
            //Result doInBackground(Params... params)
            protected Boolean doInBackground(Void... none) {
                if (DEBUG) Log.i(TAG, "doInBackground() called");
                ArrayList<ContentProviderOperation> ops = new ArrayList<>();
                long dateTop;
                int topValue;
                for (long tmpthreadId : mSelectedThreadIds) {
                    Conversation conv = Conversation.get(mContext, tmpthreadId, false);
                    Uri threadUri = conv.getUri();
                    Log.d(TAG, "setThreadsTopValue(), doInBackground(), threadUri = " + threadUri);
                    Uri.Builder uriBuilder = threadUri.buildUpon();
                    uriBuilder.appendQueryParameter("is_top", "true");
                    Uri threadUri_p = uriBuilder.build();
                    //if (Conversation.isNeedUpdate(mContext, isTop, threadUri_p)) {
                    if (conv.getTop() != isTop) {
                        if (isTop) {
                            topValue = 1;
                            dateTop = System.currentTimeMillis();
                        } else {
                            topValue = 0;
                            dateTop = conv.getDate();
                        }
                        ops.add(MessageUtils.getUpdateThreadTopOperation(threadUri_p, topValue, dateTop));
                    }
                    if( (true == isTop && ops.size() >= SET_TOP_MAX_SIZE)
                            || (false == isTop && ops.size() >= CANCEL_TOP_MAX_SIZE) ){
                        mIsBreakedByMax = true;
                        break;
                    }
                }
                mModifiedCout = ops.size();
                if (DEBUG) Log.d(TAG, "setThreadsTopValue(), ops.size() = " + mModifiedCout);
                if(ops.isEmpty()){
                    return false;
                }
                ContentProviderResult[] cpRet = null;
                try {
                    cpRet = mContext.getContentResolver().applyBatch(Conversation.MMS_SMS_AUTHORITY, ops);
                } catch (Exception e) {
                    Log.e(TAG, "setThreadsTopValue Exception: " + e);
                }
                if (null != cpRet) {
                    return true;
                }
                return false;
            }
            //void onPostExecute(Result result)
            protected void onPostExecute(Boolean bRet) {
                if (DEBUG) Log.i(TAG, "setThreadsTopValue(), onPostExecute() called, bRet = "+bRet);
                if (false == bRet) {
                    //show a toast ?
                    Log.w(TAG, "setThreadsTopValue(), onPostExecute, change failed");
                } else if (true == isTop && null != mSelectedPos && !mSelectedPos.isEmpty()
                        && mSelectedPos.get(0) > LIST_COUNT_IN_ONE_PAGE) {
                    if (mSelectedPos.get(0) >= 3*LIST_COUNT_IN_ONE_PAGE) {
                        //if (DEBUG) Log.i(TAG, "setThreadsTopValue(), set mNeedFastScrollToTop true");
                        mNeedFastScrollToTop = true;
                    } else {
                        //if (DEBUG) Log.i(TAG, "setThreadsTopValue(), set mNeedSmoothScrollToTop true");
                        mNeedSmoothScrollToTop = true;
                    }
                }
                mIsDoInBackground = false;
                if(mActionMode != null){
                    mActionMode.enableItem(ActionMode.POSITIVE_BUTTON, true);
                    mActionMode.enableItem(ActionMode.NAGATIVE_BUTTON, true);
                }
                dismissProgressDialog();
                if (!isEditMode_PickNumbers() && !MmsConfig.isSmsEnabled(mContext)) {
                    if(DEBUG) Log.i(TAG, "setThreadsTopValue(), NeedShowDefaultAppDialog");
                    mNeedShowDefaultAppDialog = true;
                }
                mQuitDeleteModeRunnable.run();
                //don't StartQuery in mContentChangedListener
                mStartQueryRunnable.run();
                if (true == mIsBreakedByMax) {
                    if(isTop){
                        showOutSizeToast(getString(R.string.set_top_maximum_reached, SET_TOP_MAX_SIZE));
                    }else{
                        showOutSizeToast(getString(R.string.cancel_top_maximum_reached, CANCEL_TOP_MAX_SIZE));
                    }
                } else {
                    if (isTop) {
                        showOutSizeToast(getString(R.string.set_top_count, mModifiedCout));
                    } else {
                        showOutSizeToast(getString(R.string.cancel_top_count, mModifiedCout));
                    }
                }
            }
            /*@Override
            protected void onCancelled() {
                if (DEBUG) Log.i(TAG, "setThreadsTopValue(), onCancelled() called");
            }*/
        };
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    //lichao add
    private void initToolBarAndActionbar() {
        myToolbar = getToolbar();
        //same as: myToolbar = (Toolbar) findViewById(com.hb.R.id.toolbar);
        myToolbar.setTitle(R.string.app_label);
        //myToolbar.inflateMenu(R.menu.zzz_conversation_list_menu);
        //same as:
        inflateToolbarMenu(R.menu.zzz_conversation_list_menu);
        //lichao add in 2016-10-31
        mToolbarMenu = myToolbar.getMenu();
        myToolbar.setOnMenuItemClickListener(this);
        setupActionModeWithDecor(myToolbar);
        setActionModeListener(mActionModeListener);
    }

    //lichao add
    @Override
    public void onNavigationClicked(View view) {
        if (DEBUG) Log.i(TAG, "[onNavigationClicked]");
        //handle click back botton on Toolbar
        if(isSearchMode()){
            onBackPressed();
        }
    }

    /// M: Code analyze 020, For bug ALPS00050455, enhance the performance of
    /// launch time . @{
    @Override
    public void onBackPressed() {
        if (DEBUG) Log.i(TAG, "[onBackPressed]");
        if (isTaskRoot()) {
            /** M: Instead of stopping, simply push this to the back of the stack.
             * This is only done when running at the top of the stack;
             * otherwise, we have been launched by someone else so need to
             * allow the user to go back to the caller.
             */
            /*
            if (mSearchItem != null && mSearchItem.isActionViewExpanded()) {
                mSearchItem.collapseActionView();
            } else if (FeatureOption.MTK_PERF_RESPONSE_TIME) {
                super.onBackPressed();
            } else {
                moveTaskToBack(false);
            }
			*/
            //same as isInDeleteMode()
            if (isEditMode()) {
                changeToNormalMode();
                return;
            }else if (isSearchMode()) {
                exitSearchMode();
                return;
            }
            if (FeatureOption.MTK_PERF_RESPONSE_TIME) {
                super.onBackPressed();
            } else {
                moveTaskToBack(false);
            }
        }
        super.onBackPressed();
    }

    //lichao add
    public void enterSearchMode() {
        if (DEBUG) Log.i(TAG, "[enterSearchMode]");
        if (!isSearchMode()) {
            mViewMode = ViewMode.SEARCH_MODE;
            updateFooterView();//hide FooterView
            updateFloatActionButton();//hide FloatActionButton
            updatePreSearchView();//hide PreSearchView
            updateOrdinaryItemsVisible();//hide SettingsItem
            updateBackIconVisibility();//show BackIcon
            updateSearchItemVisible();//show SearchItem
            if (mSearchItem != null) {
                mSearchItem.expandActionView();
            }
            initSearchView();
            /*if (null != mListAdapter) {
                //for Lock sliderView
                mListAdapter.setSearchMode(true);
            }*/
            mSearchStatus = SearchStatus.EMPTY_INPUT;
            updateHeaderView();//hide HeaderView
            showEmptySearchListView();//set SearchAdapter
            updateEmptyView();//show EmptyView
            //111
            //getWindow().setBackgroundDrawableResource(R.color.translucent_background);
            //222
            /*WindowManager.LayoutParams lp=getWindow().getAttributes();
            lp.alpha=0.3f;
            getWindow().setAttributes(lp);*/
            //333
            //mListView.getBackground().setAlpha(128);
        }
    }

    //lichao add
    public void exitSearchMode() {
        if (DEBUG) Log.i(TAG, "[exitSearchMode]");
        if (isSearchMode()) {
            //mSearchView.getQuery() is same as mSearchString
            if (!TextUtils.isEmpty(mSearchView.getQuery())) {
                mSearchView.setQuery(null, true);//CharSequence query, boolean submit
                mSearchString = "";
            }
            mViewMode = ViewMode.NORMAL_MODE;
            mSearchStatus = SearchStatus.EXITED;
            updateBackIconVisibility();
            hideSoftKeyboard(this.getCurrentFocus());
            if (mSearchItem != null && mSearchItem.isActionViewExpanded()) {
                mSearchItem.collapseActionView();
            }
            updateOrdinaryItemsVisible();
            updateSearchItemVisible();
            updatePreSearchView();
            if (mListAdapter != null) {
                //mListAdapter.setSearchMode(false);
                mListView.setAdapter(mListAdapter);
                mListAdapter.notifyDataSetChanged();
            }
            updateEmptyView();
            updateFooterView();
            updateFloatActionButton();//show mFloatButton
            clearSearchCache();
            clearSearchTask();
        }
    }

    //lichao add
    private void setSoftKeyboard(View view, boolean show) {
        /*if (show && isKeyboardShowing) {
            return;
        }
        if (!show && !isKeyboardShowing) {
            return;
        }*/
        if (null == view) {
            view = this.getCurrentFocus();
        }
        if (null == view) {
            return;
        }
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && show) {
            imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
            return;
        }
        if (imm != null && imm.isActive() && !show) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            return;
        }
    }

    private void showSoftKeyboard(View view) {
        setSoftKeyboard(view, true);
    }

    private void hideSoftKeyboard(View view) {
        setSoftKeyboard(view, false);
    }

    private void initMenuItems(){
        //see zzz_conversation_list_menu.xml
        mSearchItem = mToolbarMenu.findItem(R.id.action_search);
        mSettingsItem = mToolbarMenu.findItem(R.id.action_settings);
        mRejectMsgsItem = mToolbarMenu.findItem(R.id.action_reject_msgs);
        mCellBroadcastItem = mToolbarMenu.findItem(R.id.action_cell_broadcasts);
        // Enable link to Cell broadcast activity depending on the value in config.xml.
        mIsCellBroadcastAppLinkEnabled = this.getResources().getBoolean(
                com.android.internal.R.bool.config_cellBroadcastAppLinks);
        try {
            if (mIsCellBroadcastAppLinkEnabled) {
                PackageManager pm = getPackageManager();
                if (pm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver")
                        == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    mIsCellBroadcastAppLinkEnabled = false;  // CMAS app disabled
                }
            }
        } catch (IllegalArgumentException ignored) {
            mIsCellBroadcastAppLinkEnabled = false;  // CMAS app not installed
        }
        updateOrdinaryItemsVisible();
        updateSearchItemVisible();
    }

    //lichao add in 2017-05-12
    private void initBackIcon(){
        /*final LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View backIconView = inflater.inflate(R.layout.zzz_back_icon_view, null);
        backIconView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "backIconView onclick");
                exitSearchMode();
            }
        });
        mBackIcon = backIconView.findViewById(R.id.hb_back_icon_img);
        //myToolbar.addView(backIconView,
        //        new hb.widget.toolbar.Toolbar.LayoutParams(Gravity.CENTER_VERTICAL | Gravity.START));
        //mBackIcon.setVisibility(View.GONE);
        Toolbar.LayoutParams t_lp = new Toolbar.LayoutParams(Gravity.CENTER_VERTICAL);
        int margins = MessageUtils.dip2Px(mContext, 10);//12
        t_lp.setMargins(margins, 0, margins, 0);
        backIconView.setLayoutParams(t_lp);
        myToolbar.addView(backIconView);
        mBackIcon.setVisibility(View.GONE);*/
        //lichao add for back icon in 2016-10-31 end
        //lichao modify in 2017-05-15
        updateBackIconVisibility();
    }

    //lichao add
    private void initPreSearchView() {
        mPreSearchView = (HbPreSearchView) mHeaderView.findViewById(R.id.list_pre_search_view);
        mHeaderDivider = mHeaderView.findViewById(R.id.list_header_divider);
        updatePreSearchView();
    }

    //lichao add
    private void initSearchView() {
        /*
        if (!getResources().getBoolean(R.bool.config_classify_search)) {
            mSearchView = (HbSearchView) findViewById(R.id.searchview);
            mSearchView.setOnQueryTextListener(mQueryTextListener);
            mSearchView.setQueryHint(getString(R.string.search_hint));
            //only show the search icon
            mSearchView.setIconifiedByDefault(true);
            //show the whole search input view
            mSearchView.onActionViewExpanded();
        }
        */
        //add SearchView Container begin
        /*
        View mSearchContainer = inflater.inflate(R.layout.zzz_search_bar_expanded, myToolbar,
				*//* attachToRoot = *//* false);
        mSearchContainer.setVisibility(View.VISIBLE);
        //myToolbar.addView(mSearchContainer);
        myToolbar.addView(mSearchContainer,
                new hb.widget.toolbar.Toolbar.LayoutParams(Gravity.CENTER_VERTICAL | Gravity.END));
        */
        //add SearchView Container end
        //mSearchView=(HbSearchView)mSearchContainer.findViewById(R.id.search_view);
        mSearchView = (HbSearchView) mSearchItem.getActionView();
        mSearchView.needHintIcon(true);
        mSearchView.setOnQueryTextListener(mQueryTextListener);
        mSearchView.setOnQueryTextFocusChangeListener(mSearchViewFocusChangeListener);
        mSearchView.setQueryHint(getString(R.string.search_hint));
        //only show the search icon if true
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setFocusableInTouchMode(true);
        mSearchView.setFocusable(true);
        mSearchView.requestFocus();
    }

    //lichao add for show FloatActionButton when hide keyboard begin
    /*private void setListenerToRootView() {
        final View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (DEBUG) Log.i(TAG, "[OnGlobalLayoutListener.onGlobalLayout]");
                //int actionBarHeight = getActionBar().getHeight();
                int actionBarHeight = 144;
                if (null != myToolbar) {
                    actionBarHeight = myToolbar.getHeight();
                }
                final int headerHeight = actionBarHeight + getStatusBarHeight();
                int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();

                if (heightDiff > headerHeight) {
                    //if(DEBUG) Log.d(TAG, "setListenerToRootView(), keyboard is up");
                    isKeyboardShowing = true;
                } else {
                    //if(DEBUG) Log.d(TAG, "setListenerToRootView(), keyboard is hidden");
                    //must judge if isKeyboardShowing in the before
                    if (isKeyboardShowing) {
                        //after clearFocusForCurrentFocusView, go to mSearchViewFocusChangeListener
                        //if mSearchString isEmpty, will exitSearchMode, and updateFloatActionButton
                        //if mSearchString is not Empty, will keep in SearchMode
                        //2017-04-10: no use ?
                        //clearFocusForCurrentFocusView();
                        //lichao modify in 2017-04-26
                        clearFocusForSearchView();
                    }
                    isKeyboardShowing = false;
                }
            }
        });
    }*/

    //lichao add
    private int getStatusBarHeight() {
        Rect frame = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        return frame.top;
    }

    //lichao add in 2017-05-08
    /*OnFocusChangeListener mPreSearchViewFocusChangeListener = new OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            Log.i(TAG, "[HeaderSearchView onFocusChange] v = "+v);
            if (hasFocus) {
                enterSearchMode();
            }
        }
    };*/

    /*boolean hasFocusBeforePost = false;
    boolean hasFocusCurrent = false;
    boolean hasFocusLastTime = false;*/
    //lichao add for show FloatActionButton when hide keyboard end
    OnFocusChangeListener mSearchViewFocusChangeListener = new OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            if (DEBUG) Log.i(TAG, "[onFocusChange] v = "+v);
            if (DEBUG) Log.i(TAG, "[onFocusChange] hasFocus="+hasFocus);
            /*if(v instanceof HbSearchView){
                hasFocusCurrent = hasFocus;
                if (DEBUG) Log.i(TAG, "[onFocusChange] hasFocusLastTime="+hasFocusLastTime);
                if(hasFocusLastTime == hasFocusCurrent){
                    hasFocusLastTime = hasFocusCurrent;
                    Log.i(TAG, "[onFocusChange] Focus actrully not change, return");
                    return;
                }
                hasFocusLastTime = hasFocusCurrent;
                hasFocusBeforePost = hasFocusCurrent;
                mHandler.postDelayed(mHandleFocusChangeRunnable, 20);
            }*/
            if(hasFocus){
                showSoftKeyboard(mSearchView);
            }
        }
    };

    /*private Runnable mHandleFocusChangeRunnable = new Runnable() {
        @Override
        public void run() {
            if (hasFocusCurrent != hasFocusBeforePost) {
                Log.i(TAG, "[onFocusChange] hasFocusCurrent != hasFocusBeforePost, return");
                return;
            }
            if (hasFocusCurrent) {
                Log.i(TAG, "[onFocusChange] showSoftKeyboard");
                showSoftKeyboard(mSearchView);
                //enterSearchMode();
            } else {
                if (null == mSearchView) {
                    return;
                }
                Log.i(TAG, "[onFocusChange] hideSoftKeyboard");
                hideSoftKeyboard(mSearchView);
            }
        }
    };*/

    private void clearFocusForSearchView(){
        //it works while add codes in manifest: android:windowSoftInputMode="stateAlwaysHidden"
        if (mSearchView != null) {
            mSearchView.clearFocus();
        }
        //clear Focus on mSearchView completely by myToolbar.requestFocus()
        if(null != myToolbar){
            myToolbar.setFocusableInTouchMode(true);
            myToolbar.setFocusable(true);
            myToolbar.requestFocus();
        }
    }

    //lichao add
    private void clearFocusForCurrentFocusView() {
        View focusView = ConversationList.this.getCurrentFocus();
        if (focusView != null) {
            focusView.clearFocus();
        }
    }

    /*
	//HbActivity保留该方法: 获取Toolbar上面的Menu并进行增加和删除Item操作
    @Override
    public void updateOptionMenu(){
    	Menu menu = getOptionMenu();
        MenuItem item = menu.findItem(R.id.action_delete_all);
        if (item != null) {
            item.setVisible((mListAdapter.getCount() > 0) && mIsSmsEnabled);
        }
    }
    */

    //lichao add
    @Override
    public boolean onMenuItemClick(MenuItem item) {

        Log.d(TAG, "onMenuItemClick--->" + item.getTitle());

        switch (item.getItemId()) {
            /*case R.id.action_search:
                *//*if (getResources().getBoolean(R.bool.config_classify_search)) {
                    Intent searchintent = new Intent(this, SearchActivityExtend.class);
                    startActivityIfNeeded(searchintent, -1);
                    break;
                }*//*
                enterSearchMode();
                return true;*/

            /*
            case R.id.action_delete_all:
                // The invalid threadId of -1 means all threads here.
                confirmDeleteThread(-1L, mQueryHandler);
                return true;
            */
            case R.id.action_settings:
                /*
import com.mediatek.setting.SettingListActivity;
import com.mediatek.setting.SmsPreferenceActivity;
import com.mediatek.setting.SubSelectActivity;
import com.mediatek.simmessage.ManageSimMessages;
                */
                Intent intent = new Intent(this, MyMessagingPreferenceActivity.class);
                startActivityIfNeeded(intent, -1);
                return true;
            /*
            //qcom
            case R.id.action_cell_broadcasts:
                try {
                    startActivity(MessageUtils.getCellBroadcastIntent());
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "ActivityNotFoundException for CellBroadcastListActivity");
                }
                return true;
            */
            //mtk
            case R.id.action_cell_broadcasts:
                Intent cellBroadcastIntent = new Intent(Intent.ACTION_MAIN);
                cellBroadcastIntent.setComponent(new ComponentName(
                        "com.android.cellbroadcastreceiver",
                        "com.android.cellbroadcastreceiver.CellBroadcastListActivity"));
                cellBroadcastIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(cellBroadcastIntent);
                } catch (ActivityNotFoundException ignored) {
                    Log.e(TAG, "ActivityNotFoundException for CellBroadcastListActivity");
                }
                return true;
            case R.id.action_reject_msgs:
                Intent intent2 = new Intent();
                intent2.setAction("com.hb.reject.main");
                //intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityIfNeeded(intent2, -1);
                return true;
            default:
                return false;
        }
    }

    //lichao add
    private void initListViewHeadAndFoot() {
        final LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        mHeaderView = inflater.inflate(R.layout.zzz_conversation_list_header, null, false);
        //View v,Object data,boolean isSelectable
        mListView.addHeaderView(mHeaderView, null, false);
        ViewStub stub = (ViewStub) mHeaderView.findViewById(R.id.viewstub_conv_list_header);
        if (stub != null) {
            stub.inflate();
        }

        mFooterView = inflater.inflate(R.layout.zzz_conversation_list_footer, null, false);
        //View v,Object data,boolean isSelectable
        mListView.addFooterView(mFooterView, null, false);
        ViewStub stub2 = (ViewStub) mFooterView.findViewById(R.id.viewstub_conv_list_footer);
        if (stub2 != null) {
            stub2.inflate();
        }
    }

    //lichao add
    private void initFloatActionButton() {
        if (mFloatButton == null) {
            mFloatButton = (FloatingActionButton) findViewById(R.id.floating_action_button);
        }
        mFloatButton.setOnFloatingActionButtonClickListener(new OnFloatActionButtonClickListener() {
            public void onClick(View view) {
                if (MmsConfig.isSmsEnabled(mContext)) {
                    createNewMessage();
                } else {
                    // Display a toast letting the user know they can not compose.
                    //showDefaultSmsAppToast(R.string.compose_disabled_toast);
                    showDefaultSmsAppDialog();
                }
            }
        });
    }

    //lichao add
    private void showDefaultSmsAppToast(int resId){
        if (mComposeDisabledToast == null) {
            mComposeDisabledToast = Toast.makeText(mContext, resId, Toast.LENGTH_SHORT);
        }
        mComposeDisabledToast.setText(resId);
        mComposeDisabledToast.show();
    }

    //lichao add
    private void showOutSizeToast(String content){
        if (false == mIsInActivity) {
            return;
        }
        if (mToast == null) {
            mToast = Toast.makeText(mContext, content, Toast.LENGTH_SHORT);
        }
        mToast.setText(content);
        mToast.show();
    }

    //lichao add
    private void showDefaultSmsAppDialog(){
        //Log.i(TAG, Log.getStackTraceString(new Throwable()));
        mNeedShowDefaultAppDialog = false;
        if(false == mIsInActivity){
            return;
        }
        startActivity(MmsConfig.getRequestDefaultSmsAppActivity());
    }

    //lichao add
    private void updateFloatActionButton() {
        //if (DEBUG) Log.d(TAG, "==updateFloatActionButton== begin");
        if (isNormalMode()) {
            //modify for do not show the afterimage
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isNormalMode()) {
                        mFloatButton.setVisibility(View.VISIBLE);
                    } else {
                        mFloatButton.setVisibility(View.GONE);
                    }
                }
            }, 150);
        } else {
            mFloatButton.setVisibility(View.GONE);
        }
        //if (DEBUG) Log.d(TAG, "==updateFloatActionButton== end");
    }

    //lichao add
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (DEBUG) Log.d(TAG, "\n [onItemLongClick], position = " + position);
        if (!MmsConfig.isSmsEnabled(mContext) && !isEditMode_PickNumbers()) {
            showDefaultSmsAppToast(R.string.edit_disabled_toast);
            return true;
        }
        if (view instanceof ConversationListItem) {
            handleItemClick(parent, view, position, true);
            return true;//lichao modify to true for fix bug 3991 in 2017-07-06
        } else {
            Log.e(TAG, "onItemLongClick, view is not instanceof ConversationListItem");
        }
        //if return false here so will goto onItemClick() after
        return false;
    }

    //lichao add for refactor codes in 2017-07-14 begin
    private void handleItemClick(AdapterView<?> parent, View view, int position, boolean isLongClick) {
        //lichao modify for position is start from HeaderView, see:
        //https://blog.chengbo.net/2012/03/09/onitemclick-return-wrong-position-when-listview-has-headerview.html
        //Cursor cursor = (Cursor) mListView.getItemAtPosition(position);
        //Cursor cursor = (Cursor) mListView.getAdapter().getItem(position);
        Cursor cursor = (Cursor) parent.getAdapter().getItem(position);
        if (cursor == null) {
            Log.w(TAG, "handleItemClick, cursor is null, return, position=" + position);
            return;
        }
        Conversation conv = Conversation.from(mContext, cursor);
        if (conv == null) {
            Log.w(TAG, "handleItemClick, conv is null, return, position=" + position);
            return;
        }
        long threadId = conv.getThreadId();
        if (threadId <= 0) {
            Log.w(TAG, "handleItemClick, threadId <= 0, return, threadId=" + threadId);
            return;
        }
        //handle NormalMode
        if(true == isNormalMode()){
            if(true == isLongClick){
                showEditMode();
            }else{
                //openThread(threadId);
                //openThread(threadId, conv.getType());
				viewThread(conv, conv.getType()) ;//mtk
                return;
            }
        }
        //handle EditMode begin...
        //boolean checked_before = checkBox.isChecked();
        boolean checked_before = mListAdapter.isContainThreadId(threadId);
        if (false == checked_before
                && mSelectedPos.size() >= SELECT_ALL_MAX_SIZE) {
            //if(DEBUG) Log.d(TAG, "handleItemClick(), reached SELECT_ALL_MAX_SIZE, return");
            showOutSizeToast(getString(R.string.select_all_maximum_reached, SELECT_ALL_MAX_SIZE));
            return;
        }

        boolean check_now = !checked_before;
        //checkBox.setChecked(check_now);
        ConversationListItem convListItem = (ConversationListItem) view;
        convListItem.setItemChecked(check_now);
        //这个跟ConversationListAdapter.java类的bindView()的conv.setIsChecked作用重复
        //conv.setIsChecked(check_now);
        ContactList recipients = conv.getRecipients();
        if (true == check_now) {
            //单击勾选时候，如果之前IsContainBlack为true，那IsContainBlack维持不变；
            //单击勾选时候，如果之前IsContainBlack为false，那IsContainBlack跟随当前勾选项来变
            if(false == mContainBlock){
                boolean isContainBlockItem = MessageUtils.isContainBlackNumRecipients(mContext, recipients);
                mContainBlock = isContainBlockItem;
            }
            //单击勾选时候，如果之前IsContainNormal为true，那IsContainNormal维持不变；
            //单击勾选时候，如果之前IsContainNormal为false，那IsContainNormal跟随当前勾选项来变
            if(false == mContainUnblock){
                boolean isContainUnblockItem = !MessageUtils.isAllBlackRecipient(mContext, recipients);
                mContainUnblock = isContainUnblockItem;
            }
            //mContainTop is false means all is untop
            if(false == mContainTop){
                mContainTop = conv.getTop();
            }
            //mContainUnTop is false means all is top
            if(false == mContainUnTop){
                mContainUnTop = !conv.getTop();
            }
            mSelectedPos.add(Integer.valueOf(position));
            if (DEBUG) Log.d(TAG, "handleItemClick, mSelectedThreadIds.add: " + threadId);
            mSelectedThreadIds.add(threadId);
            mListAdapter.setSelectedState(threadId);
            if (null != recipients && !recipients.isEmpty()) {
                mSelectedRecipients.add(recipients);
            }
            //单击勾选时候，不需要重新排序，也不需要进行批量判断
            //updateActionMode_Task(false, false);
            updateActionMode();
            mListAdapter.notifyDataSetChanged();
        } else {
            if (DEBUG) Log.d(TAG, "handleItemClick, mSelectedThreadIds.remove: " + threadId);
            mSelectedPos.remove(Integer.valueOf(position));
            if (mSelectedPos.isEmpty()) {
                clearSelectedList();
                initContainValues();
                updateActionMode();
                mListAdapter.notifyDataSetChanged();
            } else {
                mSelectedThreadIds.remove(threadId);
                mListAdapter.removeSelectedState(threadId);
                if (null != recipients && !recipients.isEmpty()) {
                    mSelectedRecipients.remove(recipients);
                }
                if(isEditMode_PickNumbers()){
                    //相当于调用updateActionMode_Task(false, false);
                    updateActionMode();
                    mListAdapter.notifyDataSetChanged();
                }else{
                    //单击取消勾选一项之后，需针对所有依旧勾选项重新计算IsContainBlack和IsContainNormal
                    updateActionMode_Task(false, true);
                }
            }
        }
        //updateActionMode after mSelectedPos size changed

    }
    //lichao add for refactor codes in 2017-07-14 end

    //lichao add
    private void startSearch(final String mSearchString) {
        mSearchTask = new MySearchTask(ConversationList.this);
        mSearchTask.setSearchString(mSearchString);
        mSearchTask.setMySearchListener(mySearchListener);
        //mSearchTask.execute();
        mSearchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }

    //lichao add
    private void clearSearchTask() {
        if (mSearchTask != null) {
            mSearchTask.setSearchString(null);
            mSearchTask.cancel();
            mSearchTask = null;
        }
    }

    //lichao add
    //clearSearchCache only when exitSearchMode
    private void clearSearchCache() {
        if (DEBUG) Log.d(TAG, "[clearSearchCache]");
        if (null == mSearchCursorCache) {
            return;
        }
        for (Map.Entry<String, Cursor> entry : mSearchCursorCache.entrySet()) {
            Cursor c = entry.getValue();
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
        mSearchCursorCache.clear();
    }

    ////lichao add
    private void initEmptyView() {
        mListView.setEmptyView(findViewById(R.id.emptyview_layout_id));
        View emptyView = mListView.getEmptyView();
        //mEmptyImageView = (ImageView) emptyView.findViewById(R.id.emptyview_img);
        mEmptyTextview = (TextView) emptyView.findViewById(R.id.emptyview_tv);
        mEmptyTextview.setText(getString(R.string.loading_conversations));

        mEmptyHeaderText = (TextView) emptyView.findViewById(R.id.list_header_tv);
        mEmptyHeaderImage = (ImageView) emptyView.findViewById(R.id.list_header_img);
        mEmptyHeaderImage.setBackgroundResource(R.drawable.zzz_loading_anim);
        mEmptyLoadAnim = (AnimationDrawable) mEmptyHeaderImage.getBackground();
    }

    //lichao add
    private void updateEmptyView() {
        if (DEBUG) Log.d(TAG, "==updateEmptyView== begin");
        if(!mIsInActivity){
            return;
        }

        if (isSearchMode_Searching()) {
            mEmptyHeaderText.setText(getString(R.string.searching));
            mEmptyHeaderText.setVisibility(View.VISIBLE);
            mEmptyHeaderImage.setVisibility(View.VISIBLE);
            mEmptyLoadAnim.start();
        }else{
            mEmptyLoadAnim.stop();
            mEmptyHeaderImage.setVisibility(View.GONE);
            mEmptyHeaderText.setVisibility(View.GONE);
        }

        if (isSearchMode()) {
            if (isSearchMode_Searching()) {
                mEmptyTextview.setVisibility(View.GONE);
            } else {
                if (null != mSearchAdapter && mSearchAdapter.getCount() == 0) {
                    mEmptyTextview.setText(getString(R.string.none_matched_message));
                    mEmptyTextview.setVisibility(View.VISIBLE);
                } else {
                    mEmptyTextview.setVisibility(View.GONE);
                }
            }
        }
        //isNormalMode() || isEditMode()
        else {
            if (isQuerying()) {
                mEmptyTextview.setText(getString(R.string.loading_conversations));
                mEmptyTextview.setVisibility(View.VISIBLE);
            } else {
                if (null != mListAdapter && mListAdapter.getCount() == 0) {
                    mEmptyTextview.setText(getString(R.string.no_conversations));
                    mEmptyTextview.setVisibility(View.VISIBLE);
                } else {
                    mEmptyTextview.setVisibility(View.GONE);
                }
            }
        }
        if (DEBUG) Log.d(TAG, "==updateEmptyView== end");
    }

    //lichao add
    private void updateFooterView() {
        //if (DEBUG) Log.d(TAG, "==updateFooterView== begin");
        if (!mIsInActivity) {
            return;
        }
        if (null == mFooterText) {
            mFooterText = (TextView) mFooterView.findViewById(R.id.list_footer_count_textview);
        }
        int listCount = 0;
        if (null != mListAdapter) {
            listCount = mListAdapter.getCount();
        }
        if (!isSearchMode() && listCount >= LIST_COUNT_IN_ONE_PAGE) {
            mFooterView.setVisibility(View.VISIBLE);
            mFooterText.setVisibility(View.VISIBLE);
        } else {
            mFooterText.setVisibility(View.GONE);
            mFooterView.setVisibility(View.GONE);
        }
        //if (DEBUG) Log.d(TAG, "==updateFooterView== end");
    }

    //lichao add
    private void updatePreSearchView() {
        //if (DEBUG) Log.d(TAG, "==updatePreSearchView== begin");
        boolean needShow = isNormalMode() || isEditMode_EditThread();
        if (null != mPreSearchView) {
            mPreSearchView.setVisibility(needShow ? View.VISIBLE : View.GONE);
        }
        if (null != mHeaderDivider) {
            mHeaderDivider.setVisibility(needShow ? View.VISIBLE : View.GONE);
        }
        if(isNormalMode()) {
            mPreSearchView.setOnClickListener(mPreSearchViewClickListener);
            mPreSearchView.setQueryHint(getString(R.string.search_hint));
            mPreSearchView.setQueryHintTextColor(getResources().getColor(R.color.zzz_searchview_hint_text_color));
        }else if(isEditMode_EditThread()) {
            mPreSearchView.setOnClickListener(null);
            mPreSearchView.setQueryHint(getString(R.string.search_disabled_hint));
            mPreSearchView.setQueryHintTextColor(getResources().getColor(R.color.zzz_searchview_hint_text_color_disable));
        }
        //if (DEBUG) Log.d(TAG, "==updatePreSearchView== end");
    }

    //lichao add
    private void updateHeaderView() {
        //if (DEBUG) Log.d(TAG, "==updateHeaderView== begin");
        if (mHeaderText == null) {
            mHeaderText = (TextView) mHeaderView.findViewById(R.id.list_header_tv);
        }
        if (mHeaderImage == null) {
            mHeaderImage = (ImageView) mHeaderView.findViewById(R.id.list_header_img);
            mHeaderImage.setBackgroundResource(R.drawable.zzz_loading_anim);
        }
        if (mLoadAnim == null && mHeaderImage != null) {
            mLoadAnim = (AnimationDrawable) mHeaderImage.getBackground();
        }
        if (isSearchMode_Searching()) {
            mHeaderText.setVisibility(View.VISIBLE);
            mHeaderText.setText(getString(R.string.searching));
            mHeaderImage.setVisibility(View.VISIBLE);
            mLoadAnim.start();
        } else if (isSearchMode_ShowingResult()) {
            mLoadAnim.stop();
            mHeaderImage.setVisibility(View.GONE);
            int msgCount = 0;
            if (null != mSearchAdapter) {
                msgCount = mSearchAdapter.getCount();
            }
            mHeaderText.setText(ConversationList.this
                    .getString(R.string.zzz_searched_messages_count, msgCount));
            mHeaderText.setVisibility(View.VISIBLE);
        } else {
            mLoadAnim.stop();
            mHeaderImage.setVisibility(View.GONE);
            mHeaderText.setVisibility(View.GONE);
        }
        //if (DEBUG) Log.d(TAG, "==updateHeaderView== end");
    }

    //lichao add
    private void updateBackIconVisibility() {
        //lichao modify in 2017-05-15
        /*if (null == mBackIcon) {
            return;
        }
        int visibility = (isSearchMode()) ? View.VISIBLE : View.GONE;
        mBackIcon.setVisibility(visibility);*/
        if(isSearchMode()){
            if(mBackIconResId < 0){
                mBackIconResId = com.hb.R.drawable.ic_toolbar_back;
            }
            myToolbar.setNavigationIcon(mBackIconResId);
        }else{
            myToolbar.setNavigationIcon(null);
        }
    }

    //lichao add
    private void updateOrdinaryItemsVisible() {
        //if (DEBUG) Log.d(TAG, "==updateOrdinaryItemsVisible== begin");
        if (null != mSettingsItem) {
            mSettingsItem.setVisible(isNormalMode() && !mNeedEnterPickNumMode);
        }
        if (null != mRejectMsgsItem) {
            mRejectMsgsItem.setVisible(isNormalMode() && !mNeedEnterPickNumMode);
        }

        if (null != mCellBroadcastItem) {
            mCellBroadcastItem.setVisible(isNormalMode() && !mNeedEnterPickNumMode
                    && mIsCellBroadcastAppLinkEnabled);
        }
        //if (DEBUG) Log.d(TAG, "==updateOrdinaryItemsVisible== end");
    }

    //lichao add
    private void updateSearchItemVisible() {
        if (null != mSearchItem) {
            mSearchItem.setVisible(isSearchMode());
        }
    }

    //lichao add
    private MySearchTask.MySearchListener mySearchListener = new MySearchTask.MySearchListener() {
        @Override
        public void onSearchCompleted(boolean isCursorChanged) {
            Log.d(TAG, "mySearchListener");
            mSearchStatus = SearchStatus.SHOWING_RESULT;
            showSearchResultListView(isCursorChanged);
            updateEmptyView();
            updateFooterView();
            updateHeaderView();
        }
    };

    //lichao add
    private void showSearchResultListView(boolean isCursorChanged) {
        if (null == mSearchTask) {
            return;
        }
        if (null == mSearchAdapter) {
            mSearchAdapter = new MySearchListAdapter(ConversationList.this, null);
            mSearchAdapter.setOnContentChangedListener(mSearchContentChangedListener);
        }
        if (isCursorChanged) {
            mSearchAdapter.changeCursor(mSearchTask.getSearchedCursor());
            mSearchAdapter.setSearchString(mSearchString);
        }
        mListView.setAdapter(mSearchAdapter);
        mSearchAdapter.notifyDataSetChanged();
    }

    //lichao add
    private final MySearchListAdapter.OnContentChangedListener mSearchContentChangedListener =
            new MySearchListAdapter.OnContentChangedListener() {
                @Override
                public void onContentChanged(MySearchListAdapter adapter) {
                    if (DEBUG) Log.d(TAG, "onContentChanged(), >>>startSearch()");
                    startSearch(mSearchString);
                }
            };

    //lichao add
    private void showEmptySearchListView() {
        if (null == mSearchAdapter) {
            mSearchAdapter = new MySearchListAdapter(ConversationList.this, null);
            mSearchAdapter.setOnContentChangedListener(mSearchContentChangedListener);
        } else {
            mSearchAdapter.changeCursor(null);
        }
        mSearchAdapter.setSearchString(mSearchString);
        mListView.setAdapter(mSearchAdapter);
        mSearchAdapter.notifyDataSetChanged();
    }

    //HB. Comments :  , Engerineer : lichao , Date : 17-5-22 , begin
    private void addToBlacklist() {
        editBlacklistTask(true);
    }

    private void removeFromBlacklist() {
        editBlacklistTask(false);
    }

    private void editBlacklistTask(final boolean isAddBlack) {
        if (DEBUG) Log.d(TAG, "editBlacklistTask begin...");
        if (mSelectedRecipients == null || mSelectedRecipients.isEmpty()) {
            showOutSizeToast(getString(R.string.valid_recipients_count, 0));
            dismissProgressDialog();
            mQuitDeleteModeRunnable.run();
            mStartQueryRunnable.run();
            return;
        }
        //AsyncTask<Params, Progress, Result>
        AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                if (DEBUG) Log.i(TAG, "editBlacklistTask(), onPreExecute() called");
                mIsDoInBackground = true;
                if(mActionMode != null){
                    mActionMode.enableItem(ActionMode.POSITIVE_BUTTON, false);
                    mActionMode.enableItem(ActionMode.NAGATIVE_BUTTON, false);
                }
                mHandler.postDelayed(mShowProgressDialogRunnable, DIALOG_DELAY_TIME);
                mIsBreakedByMax = false;
                mModifiedCout = 0;
            }
            //Result doInBackground(Params... params)
            protected Boolean doInBackground(Void... none) {
                if (DEBUG) Log.i(TAG, "editBlacklistTask(), doInBackground() called");
                HashSet<String> blackNumSet = MessageUtils.getBlacklistSet(mContext);
                ArrayList<ContentProviderOperation> ops = new ArrayList<>();
                for (ContactList recipients : mSelectedRecipients) {
                    for (Contact contact : recipients) {
                        if (null == contact) {
                            continue;
                        }
                        String number = contact.getNumber();
                        /*if (Mms.isEmailAddress(number) ||
                                !Mms.isPhoneNumber(number)) {
                            continue;
                        }*/
                        String name = contact.getRealName();
                        if (isAddBlack) {
                            //if (!RejectUtil.isRejectAddress(mContext, number)) {
                            if (!MessageUtils.isBlackNumber(blackNumSet, number)) {
                                ops.add(MessageUtils.getAddBlackOperation(mContext, number, name));
                            }
                        } else {
                            if (MessageUtils.isBlackNumber(blackNumSet, number)) {
                                ops.add(MessageUtils.getDeleteBlackOperation(mContext, number, name));
                            }
                        }
                        if ( (true == isAddBlack && ops.size() >= ADD_BLACK_MAX_SIZE)
                                || (false == isAddBlack && ops.size() >= REMOVE_BLACK_MAX_SIZE)) {
                            break;
                        }
                    }
                    if ( (true == isAddBlack && ops.size() >= ADD_BLACK_MAX_SIZE)
                            || (false == isAddBlack && ops.size() >= REMOVE_BLACK_MAX_SIZE)) {
                        mIsBreakedByMax = true;
                        break;
                    }
                }
                mModifiedCout = ops.size();
                if (DEBUG) Log.d(TAG, "editBlacklistTask(), mModifiedCout = " + mModifiedCout);
                if(ops.isEmpty()){
                    return false;
                }
                ContentProviderResult[] cpRet = null;
                try {
                    cpRet = mContext.getContentResolver().applyBatch(
                            MessageUtils.HB_CONTACT_AUTHORITY, ops);
                } catch (Exception e) {
                    Log.e(TAG, "editBlacklistTask Exception: " + e);
                }
                if (null != cpRet) {
                    return true;
                }
                return false;
            }
            //void onPostExecute(Result result)
            protected void onPostExecute(Boolean bRet) {
                if (DEBUG) Log.i(TAG, "editBlacklistTask(), onPostExecute() called, bRet="+bRet);
                mIsDoInBackground = false;
                //if bRet is true, call mStartQueryRunnable.run() in mBlackListChangeReceiver
                if(false == bRet){
                    mModifiedCout = 0;
                    dismissProgressDialog();
                    if(mActionMode != null){
                        mActionMode.enableItem(ActionMode.POSITIVE_BUTTON, true);
                        mActionMode.enableItem(ActionMode.NAGATIVE_BUTTON, true);
                    }
                    if (!isEditMode_PickNumbers() && !MmsConfig.isSmsEnabled(mContext)) {
                        if(DEBUG) Log.i(TAG, "editBlacklistTask(), NeedShowDefaultAppDialog");
                        mNeedShowDefaultAppDialog = true;
                    }
                    mQuitDeleteModeRunnable.run();
                    mStartQueryRunnable.run();
                }else if (true == mIsBreakedByMax) {
                    if(isAddBlack){
                        showOutSizeToast(getString(R.string.block_maximum_reached, ADD_BLACK_MAX_SIZE));
                    }else{
                        showOutSizeToast(getString(R.string.unblock_maximum_reached, REMOVE_BLACK_MAX_SIZE));
                    }
                }
                if(false == mIsBreakedByMax){
                    if(isAddBlack){
                        showOutSizeToast(getString(R.string.block_number_count, mModifiedCout));
                    }else{
                        showOutSizeToast(getString(R.string.unblock_number_count, mModifiedCout));
                    }
                }
            }
        };
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    //HB. end

    //HB. Comments :  , Engerineer : lichao , Date : 17-6-1 , begin
    private static String RESULT_INTENT_EXTRA_NUMBER = "result_intent_extra_number";

    private void sendSelectedNumbers_Task() {
        if (DEBUG) Log.d(TAG, "\n sendSelectedNumbers_Task begin...");
        if (mSelectedRecipients == null || mSelectedRecipients.isEmpty()) {
            showOutSizeToast(getString(R.string.valid_recipients_count, 0));
            Intent retIntent = new Intent();
            ConversationList.this.setResult(Activity.RESULT_CANCELED, retIntent);
            ConversationList.this.finish();
            return;
        }
        //AsyncTask<Params, Progress, Result>
        AsyncTask<Void, Void, ArrayList<String>> task = new AsyncTask<Void, Void, ArrayList<String>>() {
            @Override
            protected void onPreExecute() {
                if (DEBUG) Log.i(TAG, "sendSelectedNumbers_Task(), onPreExecute() called");
                mIsDoInBackground = true;
                if (mActionMode != null) {
                    mActionMode.enableItem(ActionMode.POSITIVE_BUTTON, false);
                    mActionMode.enableItem(ActionMode.NAGATIVE_BUTTON, false);
                }
                mHandler.postDelayed(mShowProgressDialogRunnable, DIALOG_DELAY_TIME);
            }

            //Result doInBackground(Params... params)
            @Override
            protected ArrayList<String> doInBackground(Void... params) {
                if (DEBUG) Log.i(TAG, "sendSelectedNumbers_Task(), doInBackground() called");
                ArrayList<String> numberList = new ArrayList<>();
                for (ContactList recipients : mSelectedRecipients) {
                    for (Contact contact : recipients) {
                        if (null == contact) {
                            continue;
                        }
                        String number = contact.getNumber();
                        if (Mms.isEmailAddress(number) ||
                                !Mms.isPhoneNumber(number)) {
                            continue;
                        }
                        //if(DEBUG) Log.d(TAG, "sendSelectedNumbers_Task, NumberList.add: " + number);
                        numberList.add(number);
                        if (numberList.size() >= PICK_NUMBER_MAX_SIZE) {
                            break;
                        }
                    }
                    if (numberList.size() >= PICK_NUMBER_MAX_SIZE) {
                        showOutSizeToast(getString(R.string.pick_number_maximum_reached, PICK_NUMBER_MAX_SIZE));
                        break;
                    }
                }
                return numberList;
            }

            //void onPostExecute(Result result)
            @Override
            protected void onPostExecute(ArrayList<String> numberList) {
                if (DEBUG) Log.i(TAG, "sendSelectedNumbers_Task(), onPostExecute() called");
                mIsDoInBackground = false;
                dismissProgressDialog();
                if (mActionMode != null) {
                    mActionMode.enableItem(ActionMode.POSITIVE_BUTTON, true);
                    mActionMode.enableItem(ActionMode.NAGATIVE_BUTTON, true);
                }
                if (DEBUG) Log.d(TAG, "sendSelectedNumbers_Task(), numberList.size() = " + numberList.size());
                Intent retIntent = new Intent();
                if (numberList == null || numberList.isEmpty()) {
                    showOutSizeToast(getString(R.string.valid_numbers_count, 0));
                    ConversationList.this.setResult(Activity.RESULT_CANCELED, retIntent);
                } else {
                    retIntent.putExtra(RESULT_INTENT_EXTRA_NUMBER, numberList);
                    ConversationList.this.setResult(Activity.RESULT_OK, retIntent);
                }
                ConversationList.this.finish();
            }
        };
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private final BroadcastReceiver mBlackListChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "\n mBlackListChangeReceiver, onReceive, mIsInActivity = "+mIsInActivity);
            Log.d(TAG, " mBlackListChangeReceiver, mIsDoInBackground = "+mIsDoInBackground);
            Log.d(TAG, " mBlackListChangeReceiver, isEditMode_PickNumbers = "+isEditMode_PickNumbers());
            Log.d(TAG, " mBlackListChangeReceiver, isEditThread_EditBlackList = "+isEditThread_EditBlackList());
            //if isDoInBackground and the operation relate to blacklist
            if (mIsDoInBackground
                    && (isEditMode_PickNumbers() || isEditThread_EditBlackList())) {
                mIsDoInBackground = false;
                Log.d(TAG, " mBlackListChangeReceiver, >>>dismissProgressDialog and QuitDeleteMode");
                dismissProgressDialog();
                if(mActionMode != null){
                    mActionMode.enableItem(ActionMode.POSITIVE_BUTTON, true);
                    mActionMode.enableItem(ActionMode.NAGATIVE_BUTTON, true);
                }
                if (!isEditMode_PickNumbers() && !MmsConfig.isSmsEnabled(mContext)) {
                    if(DEBUG) Log.i(TAG, "mBlackListChangeReceiver, NeedShowDefaultAppDialog");
                    mNeedShowDefaultAppDialog = true;
                }
                mQuitDeleteModeRunnable.run();
            }
            //refresh the black tag in any time
            mStartQueryRunnable.run();
        }
    };
    //HB. end

    //lichao add in 2017-04-26 setRunnable(mHideSoftKeyboardRunnable)
    private Runnable mHideSoftKeyboardRunnable = new Runnable() {
        @Override
        public void run() {
            if(isSearchMode()){
                //hideSoftKeyboard >>>setListenerToRootView() >>>clearFocusForSearchView()
                hideSoftKeyboard(ConversationList.this.getCurrentFocus());
                clearFocusForSearchView();
            }
        }
    };

    private Runnable mStartQueryRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsInActivity) {
                mNeedQuery = true;
                MmsLog.d(TAG, "mStartQueryRunnable >>>startAsyncQuery()");
                startAsyncQuery();
            }
        }
    };

    //lichao add in 2017-05-08
    private View.OnClickListener mPreSearchViewClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "mPreSearchView onclick");
            enterSearchMode();
        }
    };

    //lichao add in 2017-05-15
    private boolean isNormalMode() {
        return (ViewMode.NORMAL_MODE == mViewMode);
    }

    //lichao add in 2017-05-15
    private boolean isQuerying() {
        return (QueryStatus.IS_QUERY == mQueryStatus);
    }

    //lichao add in 2017-05-15
    private boolean isEditMode() {
        return (ViewMode.EDIT_MODE == mViewMode);
    }

    //lichao add in 2017-05-15
    private boolean isSearchMode() {
        return (ViewMode.SEARCH_MODE == mViewMode);
    }

    //lichao add in 2017-07-17
    private boolean isSearchMode_Searching(){
        if(!isSearchMode()){
            return false;
        }
        return mSearchStatus == SearchStatus.IS_SEARCHING;
    }

    //lichao add in 2017-07-17
    private boolean isSearchMode_ShowingResult(){
        if(!isSearchMode()){
            return false;
        }
        return mSearchStatus == SearchStatus.SHOWING_RESULT;
    }

    //lichao add in 2017-05-23
    private boolean isEditMode_PickNumbers(){
        if(!isEditMode()){
            return false;
        }
        return EditModeType.PICK_NUMBERS == mEditModeType;
    }

    private boolean isEditMode_EditThread(){
        if(!isEditMode()){
            return false;
        }
        return EditModeType.EDIT_THREAD == mEditModeType;
    }

    private boolean isEditThread_EditBlackList(){
        if(!isEditMode()){
            return false;
        }
        return EditOperation.ADD_BLACK_OPERATION == mEditOperation ||
                EditOperation.REMOVE_BLACK_OPERATION == mEditOperation;
    }

    //lichao add in 2017-07-07
    private boolean isEditMode_EditTop(){
        if(!isEditMode()){
            return false;
        }
        return EditOperation.SET_TOP_OPERATION == mEditOperation ||
                EditOperation.CANCEL_TOP_OPERATION == mEditOperation;
    }

    private boolean isRebuildingIndex(){
        boolean isRebuildingIndex = false;
        Context pContext = null;
        try {
            pContext = createPackageContext("com.android.providers.contacts",
                    Context.CONTEXT_IGNORE_SECURITY);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "isRebuildingIndex(), NameNotFoundException: "+e);
        }
        if(pContext!=null) {
            SharedPreferences sp = pContext.getSharedPreferences(
                    "com.android.providers.contacts_preferences_hb",
                    Context.MODE_WORLD_READABLE | Context.MODE_MULTI_PROCESS);
            isRebuildingIndex = sp.getBoolean("is_updating_index_for_localechange", false);
        }
        return isRebuildingIndex;
    }

    //HB. Comments :  , Engerineer : lichao , Date : 17-7-18 , begin
    private int getNextBatchNumber(){
        if(false == isEditMode()){
            return 0;
        }
        int all = mListAdapter.getCount();
        if(all <= SELECT_ALL_MAX_SIZE){
            //总数不到1000，显示“取消”和“全选/全不选”
            return 0;
        }

        //后面处理会话总数超过1000的情况
        if(null == mSelectedPos || mSelectedPos.isEmpty()){
            return 1;
        }
        //超过1000条且已选数大于0则需要先排序
        //否则比如当选了第1批后再单击取消第3项，再单击勾选第3项，则后面的isAllInOneBatch判断会不准确
        sortSelectedPositions();
        //第一个勾选项位置
        int selectedFirstItemPos = mSelectedPos.get(0);
        int headerCount = mListView.getHeaderViewsCount();
        //获取第一个勾选项所处批数：
        //1~1000为第1批；1001~2000为第2批...
        //因为包含headView,Position从1(headerCount)开始,所以是selectedFirstPos-headerCount
        //因为是对第n批的最小Pos取模，取模最小值是0，而批数最小数是1，所以最后均+1
        int selectedFirstItemBatch = (selectedFirstItemPos-headerCount)/SELECT_ALL_MAX_SIZE + 1;
        //后面处理会话总数超过1000并且已选会话条数等于或超过1000的情况
        // (实际一次最多选择1000条，因为在单击选择和批量选择里均做了最多选择1000条的限制)
        if(isAllInSelectedFirstItemBatch(selectedFirstItemBatch)){
            int maxBatchNum;
            if (all % SELECT_ALL_MAX_SIZE == 0) {
                maxBatchNum = all / SELECT_ALL_MAX_SIZE;
            } else {
                maxBatchNum = all/SELECT_ALL_MAX_SIZE + 1;
            }
            //最后一批，不管有没有1000条，只要都在同一批次里，就返回初始批次1
            if(selectedFirstItemBatch == maxBatchNum){
                return 1;
            }
            //非最后一批，不到1000条的话，下次还是选择当前所在批次
            if(mSelectedPos.size() < SELECT_ALL_MAX_SIZE){
                return selectedFirstItemBatch;
            }
            //非最后一批，并把所在批次全选了，返回下一批
            return selectedFirstItemBatch+1;
        }else{
            return selectedFirstItemBatch;
        }
    }

    //不管选中的少于还是等于还是多余1000条，只要选中的前1000条都在同一批次里，就返回true
    private boolean isAllInSelectedFirstItemBatch(int selectedFirstItemBatch){
        //第n批标准的起点位置,比如 1, 1001, 2001, 3001...
        int batchFirstPos = SELECT_ALL_MAX_SIZE*(selectedFirstItemBatch-1)+1;
        int maxLimit = mSelectedPos.size();
        if(maxLimit > SELECT_ALL_MAX_SIZE){
            maxLimit = SELECT_ALL_MAX_SIZE;
        }
        for(int i=0; i<maxLimit; i++){
            if(mSelectedPos.get(i) != i+batchFirstPos){
                //Pos(0)到Pos(999)连续分别等于1到1000才算第1批
                //Pos(1000)到Pos(1999)连续分别等于1001到2000才算第2批...
                return false;
            }
        }
        return true;
    }

    //lichao add in 2017-07-27
    private void clearSelectedList(){
        mSelectedPos.clear();
        mSelectedThreadIds.clear();
        mSelectedRecipients.clear();
        if (mListAdapter != null) {
            //这个方法操作的sSelectedTheadsId实际作用跟mSelectedThreadIds一样
            //但是compose类里有多次调用 removeSelectedState 不知有何意图
            mListAdapter.clearstate();
        }
    }
    private void initContainValues(){
        mContainBlock = false;
        mContainUnblock = false;
        mContainUnTop = false;
        mContainTop = false;
    }
    ////HB. end
    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    //lichao add end
    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////

}
