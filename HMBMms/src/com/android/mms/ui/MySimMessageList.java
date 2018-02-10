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

import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.mms.MmsApp;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.util.MessageResource;
import com.android.mms.util.MmsLog;
import com.mediatek.setting.SimStateMonitor;
import com.mediatek.simmessage.ManageSimMessages;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hb.app.HbActivity;
import hb.app.dialog.AlertDialog;
import hb.app.dialog.ProgressDialog;
import hb.view.menu.bottomnavigation.BottomNavigationView;
import hb.view.menu.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;
import hb.widget.ActionMode;
import hb.widget.ActionMode.Item;
import hb.widget.ActionModeListener;
import hb.widget.HbListView;
import hb.widget.toolbar.Toolbar;
import hb.widget.toolbar.Toolbar.OnMenuItemClickListener;
import com.zzz.provider.Telephony;
import com.zzz.provider.Telephony.Sms;

//import android.app.AlertDialog;
//import android.view.ActionMode;
//import android.widget.ListView;

import com.mediatek.setting.SimStateMonitor.SimStateListener;

/**
 * Displays a list of the SMS messages stored on the ICC.
 */
//lichao change the class name from ManageSimMessages
//mtk move to package com.mediatek.simmessage
/*lichao modify View.OnCreateContextMenuListener to OnItemLongClickListener*/
public class MySimMessageList extends HbActivity
        implements OnItemClickListener, OnItemLongClickListener,
        OnMenuItemClickListener, SimStateListener {
    private static final Uri ICC_URI = Uri.parse("content://sms/icc");
    private static final Uri ICC1_URI = Uri.parse("content://sms/icc1");
    private static final Uri ICC2_URI = Uri.parse("content://sms/icc2");
    private static final String TAG = "Mms/MySimMessageList";
    private static final boolean DEBUG = true;
    private static final int MENU_COPY_TO_PHONE_MEMORY = 0;
    private static final int MENU_DELETE_FROM_SIM = 1;
    private static final int MENU_VIEW = 2;
    private static final int MENU_REPLY = 3;
    private static final int MENU_FORWARD = 4;
    private static final int MENU_CALL_BACK = 5;
    private static final int MENU_ADD_ADDRESS_TO_CONTACTS = 6;
    private static final int MENU_SEND_EMAIL = 7;
    //private static final int OPTION_MENU_DELETE_ALL = 0;
    //private static final int OPTION_MENU_EDIT = 1;
    //private static final int OPTION_MENU_SIM_CAPACITY = 2;

    private static final int SHOW_LIST = 0;
    private static final int SHOW_EMPTY = 1;
    private static final int SHOW_BUSY = 2;
    private int mState;
    private int mSlotId;

    private Uri mIccUri;
    private ContentResolver mContentResolver;
    private Cursor mCursor = null;
    //private ListView mSimList;
    private HbListView mSimList;
    //private ImageView mEmptyImageView;
    private TextView mEmptyMessage;
    //private MessageListAdapter mListAdapter = null;
    private MySimMessageListAdapter mListAdapter = null;
    private AsyncQueryHandler mQueryHandler = null;
    //private boolean mIsDeleteAll = false;
    private boolean mIsQuerying = false;
    private AlertDialog mDeleteDialog;

    public static final int SIM_FULL_NOTIFICATION_ID = 234;

    public static final int BATCH_DELETE = 100;
    public static final int TYPE_INBOX = 1;
    public static final String FORWARD_MESSAGE_ACTIVITY_NAME =
            "com.android.mms.ui.ForwardMessageActivity";

    private final ContentObserver simChangeObserver =
            new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfUpdate) {
            Log.d(TAG, "\n simChangeObserver onChange(), >>>refreshMessageList()");
            refreshMessageList();
        }
    };

    /*private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                Log.d(TAG, "receive broadcast ACTION_SIM_STATE_CHANGED");
                if (MessageUtils.isMultiSimEnabledMms()) {
                    int subId = intent.getIntExtra(PhoneConstants.SUBSCRIPTION_KEY,
                            SubscriptionManager.getDefaultSubId());
                    int currentSubId = SubscriptionManager.getSubId(mSlotId)[0];
                    Log.d(TAG, "subId: " + subId + " currentSubId: "+currentSubId);
                    if (subId != currentSubId) {
                        return;
                    }
                }
                refreshMessageList();
            }
        }
    };*/

    //lichao add begin
    //private View mFooterView;
    //private TextView mListCountView;

    private List<SubscriptionInfo> mSubInfoList;
    private int[] mAppointedSubArray = null;
    // If intent has longArrayExtra with key EXTRA_APPOINTED_SUBS, activity
    // only show subs in the longArrayExtra. If intent doesn't has the extra
    // value,activity will show all active subs.
    //mtk used in com/mediatek/simmessage/SimFullReceiver.java
    public static final String EXTRA_APPOINTED_SUBS = "subsArray";
    private int mCurrentSubId = 0;
    ///M: fix bug ALPS00850867.
    private boolean mIsCurrentSimFull;

    private boolean mBroadcastReceiverRegisted;

    private boolean mIsSimRelateChanged;
    private boolean mIsAfterDelete;
    private boolean mIsSIMCardEnabled;
    private boolean mIsRadioOn;
    //lichao add end

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.i(TAG, "\n onCreate() begin...");
        //requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        mContentResolver = getContentResolver();
        mQueryHandler = new QueryHandler(mContentResolver, this);
        setHbContentView(R.layout.zzz_sim_message_list_screen);
		//mSimList = (ListView) findViewById(R.id.messages);
        mSimList = (HbListView) findViewById(R.id.messages);
        mSimList.setOnItemClickListener(this);
        mSimList.setOnItemLongClickListener(this);
        //initListFooterView();

        mSimList.setEmptyView(findViewById(R.id.emptyview_layout_id));
        //mEmptyImageView =(ImageView)mSimList.getEmptyView().findViewById(R.id.emptyview_img);
        mEmptyMessage =(TextView)mSimList.getEmptyView().findViewById(R.id.emptyview_tv);
        mEmptyMessage.setText(R.string.sim_empty);

        //ActionBar actionBar = getActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);

        //lichao add begin
        initBottomMenuAndActionbar();
        initToolBar();
        //lichao add end

        MessagingNotification.cancelNotification(getApplicationContext(),
                SIM_FULL_NOTIFICATION_ID);
        if(mProgressDialog == null) {
            mProgressDialog = createProgressDialog();
        }

        mSlotId = getIntent().getIntExtra(PhoneConstants.SLOT_KEY, MessageUtils.SUB_INVALID);
        //mSlotId = getIntent().getIntExtra(PhoneConstants.SLOT_KEY, PhoneConstants.SUB1);
        //lichao add in 2017-04-07 begin
        mAppointedSubArray = getIntent().getIntArrayExtra(EXTRA_APPOINTED_SUBS);
        //lichao add in 2017-04-07 end

        /*IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        registerReceiver(mReceiver, filter);*/
        //registerReceiver(mSimReceiver, new IntentFilter(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED));
        registerReceiver(mSimReceiver, new IntentFilter(Telephony.Sms.Intents.SIM_FULL_ACTION));
        ///M: for 'finish activity when airplane mode on' function.
        registerReceiver(airPlaneReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
        mBroadcastReceiverRegisted = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(TAG, "\n onNewIntent(), intent = "+intent.getAction());
        setIntent(intent);
        MessagingNotification.cancelNotification(getApplicationContext(),
                SIM_FULL_NOTIFICATION_ID);

        mSlotId = getIntent().getIntExtra(PhoneConstants.SLOT_KEY, MessageUtils.SUB_INVALID);
        //mSlotId = getIntent().getIntExtra(PhoneConstants.SLOT_KEY, PhoneConstants.SUB1);
        mAppointedSubArray = getIntent().getIntArrayExtra(EXTRA_APPOINTED_SUBS);
    }

    private Uri initIccUri(int currentSubId) {
        if (currentSubId < 1) {
            return null;
        }
        //Uri iccUri = MessageUtils.getIccUriBySubscription(mSlotId);
        Uri iccUri = ICC_URI.buildUpon().appendQueryParameter(
                PhoneConstants.SUBSCRIPTION_KEY, String.valueOf(currentSubId)).build();
        if (DEBUG) Log.d(TAG, "initIccUri(), iccUri = " + iccUri);
        return iccUri;
    }

    private class QueryHandler extends AsyncQueryHandler {
        private final MySimMessageList mParent;

        public QueryHandler(
                ContentResolver contentResolver, MySimMessageList parent) {
            super(contentResolver);
            mParent = parent;
        }

        @Override
        protected void onQueryComplete(
                int token, Object cookie, Cursor cursor) {
            if (mCursor != null) {
                stopManagingCursor(mCursor);
            }
            mCursor = cursor;
            if (mCursor != null) {
                if (!mCursor.moveToFirst()) {
                    // Let user know the SIM is empty
                    Log.i(TAG, "onQueryComplete(), >>>SHOW_BUSY 111");
                    updateState(SHOW_EMPTY);
                } else if (mListAdapter == null) {
                    // Note that the MessageListAdapter doesn't support auto-requeries. If we
                    // want to respond to changes we'd need to add a line like:
                    //   mListAdapter.setOnDataSetChangedListener(mDataSetChangedListener);
                    // See ComposeMessageActivity for an example.
                    Log.d(TAG, "onQueryComplete(), new MySimMessageListAdapter");
                    mListAdapter = new MySimMessageListAdapter(
                            mParent, mCursor, mSimList, false, null);
                    //add by lgy
                    if(DEBUG) Log.d(TAG, "onQueryComplete(), iccUri = " + mIccUri);
                    mListAdapter.setIccUri(mIccUri);
                    mListAdapter.setCheckList(mSelectedUri);
                    //lichao add
                    mSimList.setAdapter(mListAdapter);
                    Log.i(TAG, "onQueryComplete(), >>>SHOW_LIST 111");
                    updateState(SHOW_LIST);
                } else {
                    Log.d(TAG, "onQueryComplete(), changeCursor");
                    mListAdapter.changeCursor(mCursor);
                    Log.i(TAG, "onQueryComplete(), >>>SHOW_LIST 222");
                    updateState(SHOW_LIST);
                }
                startManagingCursor(mCursor);
            } else {
                // Let user know the SIM is empty
                Log.i(TAG, "onQueryComplete(), >>>SHOW_EMPTY 222");
                updateState(SHOW_EMPTY);
            }
            //lichao add
            //updateFootView();
            // Show option menu when query complete.
            invalidateOptionsMenu();
            //mSimList.setMultiChoiceModeListener(new ModeCallback());
            //mSimList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            mIsQuerying = false;
        }
    }

    private void startQuery(Uri iccUri) {
        try {
            Log.d(TAG, "startQuery(), mIsQuerying :" + mIsQuerying + " iccUri :" + iccUri);
            if (mIsQuerying) {
                Log.d(TAG, "startQuery(), isQuerying, return");
                return;
            }
            if (null == iccUri) {
                return;
            }
            mIsQuerying = true;
            mQueryHandler.startQuery(0, null, iccUri, null, null, null, null);
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    private void refreshMessageList() {
        Log.i(TAG, "\n refreshMessageList() begin...");
        if (mDeleteDialog != null && mDeleteDialog.isShowing()) {
            mDeleteDialog.dismiss();
        }
        if(true == initAndcheckNeedShowEmpty()){
            Log.w(TAG, "refreshMessageList(), >>>SHOW_EMPTY, return");
            updateState(SHOW_EMPTY);
            return;
        }
        if(false == mIsSimRelateChanged && false == mIsAfterDelete){
            Log.i(TAG, "refreshMessageList(), SimRelate not Changed and not AfterDelete, don't startQuery, return");
            return;
        }
        mIsAfterDelete = false;
        Log.i(TAG, "refreshMessageList(), >>>SHOW_BUSY");
        updateState(SHOW_BUSY);
        if(DEBUG) Log.i(TAG, "refreshMessageList(), >>>startQuery");
        startQuery(mIccUri);

    }

    private boolean initAndcheckNeedShowEmpty(){
        mIsSimRelateChanged = false;
        boolean isSIMCardEnabled = isSIMCardEnabled(mSlotId);
        if(mIsSIMCardEnabled != isSIMCardEnabled){
            mIsSIMCardEnabled = isSIMCardEnabled;
            if(DEBUG) Log.i(TAG, "initAndcheckNeedShowEmpty(), mIsSIMCardEnabled Changed");
            mIsSimRelateChanged = true;
        }
        if (false == mIsSIMCardEnabled) {
            Log.w(TAG, "initAndcheckNeedShowEmpty(), SIMCard not Enabled, return true");
            return true;
        }

        initialSubInfoList(mAppointedSubArray);
        if (mSubInfoList == null || mSubInfoList.isEmpty()) {
            Log.w(TAG, "initAndcheckNeedShowEmpty(), SubInfoList.isEmpty, return true");
            return true;
        }

        int old_subId = mCurrentSubId;
        if (mSubInfoList.size() == 1) {
            mCurrentSubId = mSubInfoList.get(0).getSubscriptionId();
        } else {
            mCurrentSubId = mSubInfoList.get(mSlotId).getSubscriptionId();
        }
        if (DEBUG) Log.d(TAG, "initAndcheckNeedShowEmpty(), mCurrentSubId = " + mCurrentSubId);
        if(old_subId != mCurrentSubId){
            if(DEBUG) Log.i(TAG, "initAndcheckNeedShowEmpty(), mCurrentSubId Changed");
            mIsSimRelateChanged = true;
        }
        if(mCurrentSubId < 1){
            Log.w(TAG, "initAndcheckNeedShowEmpty(), CurrentSubId < 1, return true");
            return true;
        }

        boolean isRadioOn = MessageUtils.isRadioOn(mCurrentSubId, getContext());
        if(mIsRadioOn != isRadioOn){
            mIsRadioOn = isRadioOn;
            if(DEBUG) Log.i(TAG, "initAndcheckNeedShowEmpty(), isRadioOn Changed");
            mIsSimRelateChanged = true;
        }
        if (false == mIsRadioOn) {
            Log.w(TAG, "initAndcheckNeedShowEmpty(), Radio not on, return true");
            return true;
        }

        Uri old_iccUri = mIccUri;
        mIccUri = initIccUri(mCurrentSubId);
        if((null != mIccUri && null != old_iccUri && !mIccUri.equals(old_iccUri))
                || (null != mIccUri && null == old_iccUri)
                || (null == mIccUri && null != old_iccUri)){
            if(DEBUG) Log.i(TAG, "initAndcheckNeedShowEmpty(), mIccUri Changed");
            mIsSimRelateChanged = true;
        }
        if(null == mIccUri){
            Log.w(TAG, "initAndcheckNeedShowEmpty(), mIccUri is null");
            return true;
        }
        return false;
    }

    /*
	//lichao delete begin
    // Refs to ComposeMessageActivity.java
    private final void addCallAndContactMenuItems(
            ContextMenu menu, Cursor cursor) {
        String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
        String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
        // Add all possible links in the address & message
        StringBuilder textToSpannify = new StringBuilder();
        if (address != null) {
            textToSpannify.append(address + ": ");
        }
        textToSpannify.append(body);

        SpannableString msg = new SpannableString(textToSpannify.toString());
        Linkify.addLinks(msg, Linkify.ALL);
        ArrayList<String> uris =
            MessageUtils.extractUris(msg.getSpans(0, msg.length(), URLSpan.class));

        while (uris.size() > 0) {
            String uriString = uris.remove(0);
            // Remove any dupes so they don't get added to the menu multiple times
            while (uris.contains(uriString)) {
                uris.remove(uriString);
            }

            int sep = uriString.indexOf(":");
            String prefix = null;
            if (sep >= 0) {
                prefix = uriString.substring(0, sep);
                uriString = uriString.substring(sep + 1);
            }
            boolean addToContacts = false;
            if ("mailto".equalsIgnoreCase(prefix))  {
                String sendEmailString = getString(
                        R.string.menu_send_email).replace("%s", uriString);
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("mailto:" + uriString));
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                menu.add(0, MENU_SEND_EMAIL, 0, sendEmailString)
                    .setIntent(intent);
                addToContacts = !haveEmailContact(uriString);
            } else if ("tel".equalsIgnoreCase(prefix)) {
                String callBackString = getString(
                        R.string.menu_call_back).replace("%s", uriString);
                Intent intent = new Intent(Intent.ACTION_CALL,
                        Uri.parse("tel:" + uriString));
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                addToContacts = !isNumberInContacts(uriString);
                if (addToContacts) {
                    menu.add(0, MENU_CALL_BACK, 0, callBackString).setIntent(intent);
                } else {
                    // Get the name by the number if the number has been saved
                    // in Contacts.
                    String name = Contact.get(uriString, false).getName();
                    menu.add(0, MENU_CALL_BACK, 0,
                            getString(R.string.menu_call_back).replace("%s", name)).setIntent(
                            intent);
                }
            }
            if (addToContacts) {
                Intent intent = ConversationList.createAddContactIntent(uriString);
                String addContactString = getString(
                        R.string.menu_add_address_to_contacts).replace("%s", uriString);
                menu.add(0, MENU_ADD_ADDRESS_TO_CONTACTS, 0, addContactString)
                    .setIntent(intent);
            }
        }
    }

    private boolean haveEmailContact(String emailAddress) {
        Cursor cursor = SqliteWrapper.query(this, getContentResolver(),
                Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(emailAddress)),
                new String[] { Contacts.DISPLAY_NAME }, null, null, null);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    if (!TextUtils.isEmpty(name)) {
                        return true;
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return false;
    }
	//lichao delete end
	*/

    /*private boolean isNumberInContacts(String phoneNumber) {
        return Contact.get(phoneNumber, true).existsInDatabase();
    }*/

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()...");
        // Clean up the notification according to the SIM number.
        MessagingNotification.blockingRemoveIccNotifications(this, mSlotId);

        // Set current SIM thread id according to the SIM number.
        MessagingNotification.setCurrentlyDisplayedThreadId(
                MessageUtils.getSimThreadByPhoneId(mSlotId));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()...");
        SimStateMonitor.getInstance().addListener(this);

        mState = -1;
        if(true == initAndcheckNeedShowEmpty()){
            Log.w(TAG, "onStart(), >>>SHOW_EMPTY, return");
            updateState(SHOW_EMPTY);
            return;
        }
        if(DEBUG) Log.i(TAG, "onStart(), mIsSimRelateChanged = "+mIsSimRelateChanged);
        if(false == mIsSimRelateChanged){
            Log.i(TAG, "onStart(), SimRelate not Changed, don't startQuery, return");
            return;
        }
        Log.i(TAG, "onStart(), >>>SHOW_BUSY");
        updateState(SHOW_BUSY);
        if(DEBUG) Log.w(TAG, "onStart(), >>>startQuery");
        startQuery(mIccUri);
        mContentResolver.registerContentObserver(mIccUri, true, simChangeObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        MessagingNotification.setCurrentlyDisplayedThreadId(MessagingNotification.THREAD_NONE);
    }

    @Override
    public void onBackPressed() {
        if(mEditMode){
            changeToNormalMode();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        super.onStop();
        SimStateMonitor.getInstance().removeListener(this);
        // Simply setting the choice mode causes the previous choice mode to finish and we exit
        // multi-select mode (if we're in it) and remove all the selections.
        //mSimList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    }

    @Override
    public void onDestroy() {
        //unregisterReceiver(mReceiver);
        /// M: fix bug ALPS00414035
        //add if for bug 3098
        if(true == mBroadcastReceiverRegisted){
            unregisterReceiver(mSimReceiver);
            ///M: for 'finish activity when airplane mode on' function.
            unregisterReceiver(airPlaneReceiver);
        }
        //add if for bug 3098
        if(null != mIccUri){
            mContentResolver.unregisterContentObserver(simChangeObserver);
        }
        mQueryHandler.removeCallbacksAndMessages(null);
		//lichao add begin
        if (mListAdapter != null) {
            mListAdapter.changeCursor(null);
        }
        dismissProgressDialog();
		//lichao add end
        super.onDestroy();
    }

    //lichao modify void to boolean
    private boolean copyToPhoneMemory(Cursor cursor) {

        //lichao add if judgement
        if (MessageUtils.checkIsPhoneMessageFull(getContext())) {
            return false;
        }

        String address = cursor.getString(
                cursor.getColumnIndexOrThrow("address"));
        String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
        Long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
        int subId = MessageUtils.SUB_INVALID;
        subId = cursor.getInt(cursor.getColumnIndexOrThrow("sub_id"));

        boolean success = true;
        try {
            if (isIncomingMessage(cursor)) {
                Sms.Inbox.addMessage(subId, mContentResolver, address, body, null,
                        date, true /* read */);
            } else {
                Sms.Sent.addMessage(subId, mContentResolver, address, body, null, date);
            }
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
            success = false;
        }
		//lichao modify
		//showToast(success);
        return success;
    }

    private void showToast(boolean success) {
        int resId = success ? R.string.copy_to_phone_success :
                R.string.copy_to_phone_fail;
        Toast.makeText(getContext(), getString(resId), Toast.LENGTH_SHORT).show();
    }

    private boolean isIncomingMessage(Cursor cursor) {
        int messageStatus = cursor.getInt(
                cursor.getColumnIndexOrThrow("status"));

        return (messageStatus == SmsManager.STATUS_ON_ICC_READ) ||
               (messageStatus == SmsManager.STATUS_ON_ICC_UNREAD);
    }

    /*
	//lichao delete begin
    private void deleteFromSim(Cursor cursor) {
        String messageIndexString =
                cursor.getString(cursor.getColumnIndexOrThrow("index_on_icc"));
        Uri simUri = mIccUri.buildUpon().appendPath(messageIndexString).build();

        SqliteWrapper.delete(this, mContentResolver, simUri, null, null);
    }

    private void deleteAllFromSim() {
        mIsDeleteAll = true;
        Cursor cursor = (Cursor) mListAdapter.getCursor();

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                mContentResolver.unregisterContentObserver(simChangeObserver);
                int count = cursor.getCount();

                for (int i = 0; i < count; ++i) {
                    if (!mIsDeleteAll || cursor.isClosed()) {
                        break;
                    }
                    cursor.moveToPosition(i);
                    deleteFromSim(cursor);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshMessageList();
                        registerSimChangeObserver();
                    }
                });
            }
        }
        mIsDeleteAll = false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();

        if (null != mCursor) {
            if(mState != SHOW_BUSY) {
                menu.add(0, OPTION_MENU_EDIT, 0, R.string.menu_edit);
                menu.add(0, OPTION_MENU_SIM_CAPACITY, 0, R.string.sim_capacity_title);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case OPTION_MENU_EDIT:
                showEditMode(true);
                break;

            case OPTION_MENU_SIM_CAPACITY:
                showSimCapacityDialog();
                break;

            case android.R.id.home:
                // The user clicked on the Messaging icon in the action bar. Take them back from
                // wherever they came from
                finish();
                break;
        }

        return true;
    }

    private void showSimCapacityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.sim_capacity_title);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.yes, null);
        StringBuilder capacityMessage = new StringBuilder();
        capacityMessage.append(getString(R.string.sim_capacity_used));
        capacityMessage.append(" " + mCursor.getCount() + "\n");
        capacityMessage.append(getString(R.string.sim_capacity));
        int iccCapacityAll = -1;
        if (MessageUtils.isMultiSimEnabledMms()) {
            int[] subId = SubscriptionManager.getSubId(mSlotId);
            iccCapacityAll = SmsManager.getSmsManagerForSubscriptionId(subId[0])
                    .getSmsCapacityOnIcc();
        } else {
            iccCapacityAll = SmsManager.getDefault().getSmsCapacityOnIcc();
        }

        capacityMessage.append(" " + iccCapacityAll);
        builder.setMessage(capacityMessage);

        builder.show();
    }
	//lichao delete end
    */
    
    private void updateState(int state) {
        if (mState == state) {
            Log.d(TAG, "updateState(), return, mState == state:"+state);
            return;
        }

        mState = state;
        switch (state) {
            case SHOW_LIST:
                Log.d(TAG, "updateState(), SHOW_LIST");
                mSimList.setVisibility(View.VISIBLE);
                //mEmptyMessage.setVisibility(View.GONE);
                mSimList.getEmptyView().setVisibility(View.GONE);
                //setTitle(getString(R.string.sim_manage_messages_title));
                //setProgressBarIndeterminateVisibility(false);//lichao delete
                dismissProgressDialog();
                mSimList.requestFocus();
                break;
            case SHOW_EMPTY:
                Log.d(TAG, "updateState(), SHOW_EMPTY");
                mSimList.setVisibility(View.GONE);
                setToolbarMenuVisibility(false);
                //mEmptyMessage.setVisibility(View.VISIBLE);
                mSimList.getEmptyView().setVisibility(View.VISIBLE);
                //setTitle(getString(R.string.sim_manage_messages_title));
                //setProgressBarIndeterminateVisibility(false);//lichao delete
                dismissProgressDialog();
                break;
            case SHOW_BUSY:
                Log.d(TAG, "updateState(), SHOW_BUSY");
                mSimList.setVisibility(View.GONE);
                //mEmptyMessage.setVisibility(View.GONE);
                mSimList.getEmptyView().setVisibility(View.GONE);
                //setTitle(getString(R.string.refreshing));
                //setProgressBarIndeterminateVisibility(true);//lichao delete
                if(mProgressDialog != null && !mProgressDialog.isShowing()
                        && MySimMessageList.this!=null && !MySimMessageList.this.isFinishing()) {
                    mProgressDialog.show();//lichao add
                }
                break;
            default:
                Log.e(TAG, "updateState(), Invalid State");
        }
    }

    private void viewMessage(Cursor cursor) {
        // TODO: Add this.
    }

    private Uri getUriStrByCursor(Cursor cursor) {
        String messageIndexString = cursor.getString(cursor.getColumnIndexOrThrow("index_on_icc"));
        return mIccUri.buildUpon().appendPath(messageIndexString).build();
    }

    public Context getContext() {
        return MySimMessageList.this;
    }

    //ListView
    public HbListView getListView() {
        return mSimList;
    }

    /*
	//lichao delete begin
    private class ModeCallback implements ListView.MultiChoiceModeListener {
        private View mMultiSelectActionBarView;
        private TextView mSelectedConvCount;
        private ImageView mSelectedAll;
        private boolean mHasSelectAll = false;
        // build action bar with a spinner
        private SelectionMenu mSelectionMenu;
        ArrayList<Integer> mSelectedPos = new ArrayList<Integer>();
        ArrayList<Uri> mSelectedMsg = new ArrayList<Uri>();

        public void checkAll(boolean isCheck) {
            Log.d(TAG, "check all:" + isCheck);
            for (int i = 0; i < getListView().getCount(); i++) {
                getListView().setItemChecked(i, isCheck);
            }
        }

        private void confirmDeleteDialog(OnClickListener listener) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.confirm_dialog_title);
            builder.setIconAttribute(android.R.attr.alertDialogIcon);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.yes, listener);
            builder.setNegativeButton(R.string.no, null);
            builder.setMessage(R.string.confirm_delete_selected_messages);
            mDeleteDialog = builder.show();
        }

        private class MultiMessagesListener implements OnClickListener {
            public void onClick(DialogInterface dialog, int whichButton) {
                deleteMessages();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshMessageList();
                    }
                });
            }
        }

        private void deleteMessages() {
            for (Uri uri : mSelectedMsg) {
                Log.d(TAG, "uri:" +uri.toString());
                SqliteWrapper.delete(getContext(), mContentResolver, uri, null, null);
            }
        }

        private void forwardMessage() {
            int pos = mSelectedPos.get(0);
            Cursor cursor = (Cursor) getListView().getAdapter().getItem(pos);
            String smsBody = cursor.getString(cursor.getColumnIndexOrThrow("body"));
            Intent intent = new Intent();
            intent.putExtra("exit_on_sent", true);
            intent.putExtra("forwarded_message", true);
            intent.putExtra("sms_body", smsBody);
            intent.setClassName(getContext(), FORWARD_MESSAGE_ACTIVITY_NAME);
            startActivity(intent);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            recordAllSelectedItems();
            switch (item.getItemId()) {
                case R.id.forward:
                    forwardMessage();
                    break;
                case R.id.delete:
                    confirmDeleteDialog(new MultiMessagesListener());
                    break;
                case R.id.copy_to_phone:
                    int pos = mSelectedPos.get(0);
                    Cursor cursor = (Cursor) getListView().getAdapter().getItem(pos);
                    if (!MessageUtils.checkIsPhoneMessageFull(getContext())) {
                        copyToPhoneMemory(cursor);
                    }
                    break;
                case R.id.reply:
                    replyMessage();
                    break;
                default:
                    break;
            }
            mode.finish();
            return true;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.sim_msg_multi_select_menu, menu);

            if (mMultiSelectActionBarView == null) {
                mMultiSelectActionBarView = LayoutInflater.from(getContext()).inflate(
                        R.layout.action_mode, null);
            }
            mode.setCustomView(mMultiSelectActionBarView);
            mSelectionMenu = new SelectionMenu(getContext(),
                    (Button) mMultiSelectActionBarView.findViewById(R.id.selection_menu),
                    new PopupList.OnPopupItemClickListener() {
                        @Override
                        public boolean onPopupItemClick(int itemId) {
                            if (itemId == SelectionMenu.SELECT_OR_DESELECT) {
                                checkAll(!mHasSelectAll);
                            }
                            return true;
                        }
                    });
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            mSelectionMenu.dismiss();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu arg1) {
            if (mMultiSelectActionBarView == null) {
                ViewGroup v = (ViewGroup) LayoutInflater.from(getContext()).inflate(
                        R.layout.conversation_list_multi_select_actionbar, null);
                mode.setCustomView(v);
                mSelectedConvCount = (TextView) v.findViewById(R.id.selected_conv_count);
            }
            return true;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int arg1, long arg2, boolean arg3) {
            final int checkedCount = getListView().getCheckedItemCount();

            mSelectionMenu.setTitle(getApplicationContext().getString(R.string.selected_count,
                    checkedCount));
            if (checkedCount == 1) {
                recoredCheckedItemPositions();
            }
            customMenuVisibility(mode, checkedCount);
            if (getListView().getCount() == checkedCount) {
                mHasSelectAll = true;
                Log.d(TAG, "onItemCheck select all true");
            } else {
                mHasSelectAll = false;
                Log.d(TAG, "onItemCheck select all false");
            }
            mSelectionMenu.updateSelectAllMode(mHasSelectAll);
        }

        private void customMenuVisibility(ActionMode mode, int checkedCount) {
            if (checkedCount > 1) {
                mode.getMenu().findItem(R.id.forward).setVisible(false);
                mode.getMenu().findItem(R.id.reply).setVisible(false);
                mode.getMenu().findItem(R.id.copy_to_phone).setVisible(false);
            } else if(checkedCount == 1) {
                mode.getMenu().findItem(R.id.forward).setVisible(true);
                mode.getMenu().findItem(R.id.copy_to_phone).setVisible(true);
                mode.getMenu().findItem(R.id.reply).setVisible(isInboxSms());
            }
        }

        private void recoredCheckedItemPositions(){
            SparseBooleanArray booleanArray = getListView().getCheckedItemPositions();
            mSelectedPos.clear();
            Log.d(TAG, "booleanArray = " + booleanArray);
            for (int i = 0; i < booleanArray.size(); i++) {
                int pos = booleanArray.keyAt(i);
                boolean checked = booleanArray.get(pos);
                if (checked) {
                    mSelectedPos.add(pos);
                }
            }
        }

        private void recordAllSelectedItems() {
            // must calculate all checked msg before multi selection done.
            recoredCheckedItemPositions();
            calculateSelectedMsgUri();
        }

        private void calculateSelectedMsgUri() {
            mSelectedMsg.clear();
            for (Integer pos : mSelectedPos) {
                Cursor c = (Cursor) getListView().getAdapter().getItem(pos);
                mSelectedMsg.add(getUriStrByCursor(c));
            }
        }

        private boolean isInboxSms() {
            int pos = mSelectedPos.get(0);
            Cursor cursor = (Cursor) getListView().getAdapter().getItem(pos);
            long type = cursor.getLong(cursor.getColumnIndexOrThrow("type"));
            Log.d(TAG, "sms type is: " + type);
            return type == TYPE_INBOX;
        }

        private void replyMessage() {
            int pos = mSelectedPos.get(0);
            Cursor cursor = (Cursor) getListView().getAdapter().getItem(pos);
            String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
            Intent replyIntent = new Intent(getContext(), ComposeMessageActivity.class);
            replyIntent.putExtra("reply_message", true);
            replyIntent.putExtra("address", address);
            replyIntent.putExtra("exit_on_sent", true);
            getContext().startActivity(replyIntent);
        }
    }
    //lichao delete end
    */
    
	////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////
    //add by lgy or lichao begin
	////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////
	/*private void initListFooterView() {
        final LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mFooterView = inflater.inflate(R.layout.zzz_conversation_list_footer, null, false);
        mSimList.addFooterView(mFooterView, null, false);
	}*/
	
	private BottomNavigationView mBottomNavigationView;
	public ActionMode mActionMode;
	private ActionModeListener mActionModeListener = new ActionModeListener() {

		@Override
		public void onActionItemClicked(Item item) {
			// TODO Auto-generated method stub
			switch (item.getItemId()) {
			case ActionMode.POSITIVE_BUTTON:
				int checkedCount = mSelectedUri.size();
				int all = mListAdapter.getCount();			
				selectAll(checkedCount < all);
				
				break;
			case ActionMode.NAGATIVE_BUTTON:
				safeQuitDeleteMode();
				break;
			default:
			}

		}

		@Override
		public void onActionModeDismiss(ActionMode arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onActionModeShow(ActionMode arg0) {
			// TODO Auto-generated method stub
		}

	};
	
    ArrayList<Uri> mSelectedUri = new ArrayList<Uri>();
    Set<Integer> mSelectedPos = new HashSet<Integer>();
    
	private void selectAll(boolean all) {
		if(all) {
	        Cursor cursor  = mListAdapter.getCursor();
	        cursor.moveToFirst();
	        mSelectedUri.clear();
            mSelectedPos.clear();
	        do {
		        Uri uri = getUriStrByCursor(cursor);		
		        mSelectedUri.add(uri);
                mSelectedPos.add(cursor.getPosition());
	        } while (cursor.moveToNext());
		} else {
			mSelectedUri.clear();
            mSelectedPos.clear();
		}
		
		updateActionMode();
		mListAdapter.notifyDataSetChanged();
	}
	
	private void safeQuitDeleteMode() {
		try {
			Thread.sleep(300);
            changeToNormalMode();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initBottomMenuAndActionbar() {
		mActionMode = getActionMode();
		mBottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation_view);
		mBottomNavigationView.setNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
		setActionModeListener(mActionModeListener);
	}
	
	private void confirmDeleteDialog(OnClickListener listener) {
        String messageStr = getString(R.string.confirm_delete_selected_messages_one);
        int selectedSize = mSelectedUri.size();
        if(selectedSize == 1) {
            messageStr = getString(R.string.confirm_delete_selected_messages_one);
        } else {
            messageStr = getString(R.string.confirm_delete_selected_messages_more, selectedSize);
        }
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		//builder.setTitle(R.string.confirm_dialog_title);
		builder.setIconAttribute(android.R.attr.alertDialogIcon);
		builder.setCancelable(true);
		builder.setPositiveButton(R.string.yes, listener);
		builder.setNegativeButton(R.string.no, null);
		builder.setMessage(messageStr);
		mDeleteDialog = builder.show();
	}
	
	private class MultiDeleteMessagesListener implements OnClickListener {
		public void onClick(DialogInterface dialog, int whichButton) {
			deleteMessages();
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
                    changeToNormalMode();//showEditMode(false /*isEditMode*/);
                    Log.d(TAG, "MultiDeleteMessagesListener onClick(), >>>refreshMessageList()");
                    mIsAfterDelete = true;//lichao add for fix BUG #8405
					refreshMessageList();
                    Toast.makeText(MySimMessageList.this, MySimMessageList.this.getResources()
                            .getString(R.string.messages_delete_completed), Toast.LENGTH_SHORT).show();
				}
			});
		}
	}

	private void deleteMessages() {
		for (Uri uri : mSelectedUri) {
			Log.d(TAG, "deleteMessages(), uri:" + uri.toString());
			SqliteWrapper.delete(this, mContentResolver, uri, null,
					null);
		}
	}

    private Toolbar myToolbar;
    private void initToolBar() {
		//myToolbar = (Toolbar) findViewById(com.hb.R.id.toolbar);
        myToolbar = getToolbar();
		myToolbar.setTitle(R.string.sim_manage_messages_title);
		myToolbar.inflateMenu(R.menu.zzz_manage_sim_menu);
        //inflateToolbarMenu(R.menu.zzz_manage_sim_menu);
		myToolbar.setOnMenuItemClickListener(this);
        setupActionModeWithDecor(myToolbar);
    }
    
    @Override
    public boolean onMenuItemClick(MenuItem item) {

        Log.d(TAG, "onMenuItemClick--->"+item.getTitle());

        switch (item.getItemId()) {
            //lichao add in 2016-08-22 begin
            case R.id.action_edit:
                showEditMode();
                updateActionMode();
                return true;
            //lichao add in 2016-08-22 end

            /*
            case R.id.action_sim_capacity:
                showSimCapacityDialog();
                return true;
            */
            default:
                return false;
        }
    }

    @Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (mListAdapter == null) {
			return;
		}
		
		if (!isInDeleteMode()) {     
			showOnClickPopupWindow(position);
			return;
		}

        Cursor c = (Cursor) getListView().getAdapter().getItem(position);
        Uri uri = getUriStrByCursor(c);		

		final CheckBox checkBox = (CheckBox) view
				.findViewById(R.id.sim_msg_check_box);
		if (null != checkBox) {
			boolean checked = checkBox.isChecked();
			checkBox.setChecked(!checked);
			if(!checked) {
				mSelectedUri.add(uri);
                mSelectedPos.add(position);
			} else {
				mSelectedUri.remove(uri);
                mSelectedPos.remove(position);
			}
			updateActionMode();
		}
	}

	private boolean isInDeleteMode() {
	    //tangyisen modify
		//return getActionMode().isShowing();
	    return mEditMode;
	}
    
    private void updateActionMode() {
		if (mListAdapter == null) {
			finish();
			return;
		}
		
		String mSelectAllStr = getResources().getString(R.string.selected_all);
		String mUnSelectAllStr = getResources().getString(R.string.deselected_all);
		
		int checkedCount = mSelectedUri.size();
		int all = mListAdapter.getCount();

		if (checkedCount >= all) {
			mActionMode.setPositiveText(mUnSelectAllStr);
		} else {
			mActionMode.setPositiveText(mSelectAllStr);
		}

        //lichao modify for enable/disable BottomMenuItems
	    //mBottomNavigationView.setEnabled(checkedCount > 0);
        updateBottomMenuItems();

		updateActionModeTitle(getString(R.string.selected_total_num,
				checkedCount));
	}

    //lichao merge from ComposeMessageActivity begin
    private void updateBottomMenuItems() {
        int checkedCount = mSelectedUri.size();

        //Menu menu = mBottomNavigationView.getMenu();

        //see sim_msg_multi_select_menu.xml
        //as hb:menu="@menu/sim_msg_multi_select_menu" in zzz_sim_message_list_screen.xml
//        MenuItem copy_to_phoneItem = menu.findItem(R.id.copy_to_phone);
//        MenuItem sim_msg_deleteItem = menu.findItem(R.id.sim_msg_delete);
//        copy_to_phoneItem.setEnabled(checkedCount > 0);
//        sim_msg_deleteItem.setEnabled(checkedCount > 0);

        mBottomNavigationView.setItemEnable(R.id.copy_to_phone, (checkedCount > 0));
        mBottomNavigationView.setItemEnable(R.id.sim_msg_delete, (checkedCount > 0));
    }
    //lichao merge from ComposeMessageActivity end
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
         if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (isInDeleteMode()) {
				safeQuitDeleteMode();
				return true;
			}
        }

        return super.onKeyDown(keyCode, event);
    }
    
    private ProgressDialog mProgressDialog;
    
    private ProgressDialog createProgressDialog() {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.setMessage(getText(R.string.refreshing));
        return dialog;
    }
    
	private void showOnClickPopupWindow(final int position) {

		// CharSequence[] subString = getResources().getTextArray(
		// R.array.multi_sim_entries);
		Cursor cursor = (Cursor) getListView().getAdapter().getItem(position);
		String smsBody = cursor.getString(cursor.getColumnIndexOrThrow("body"));
        //lichao modify in 2016-08-22 begin

        String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));

        SpannableStringBuilder title_buf = new SpannableStringBuilder();
        title_buf.append(Contact.get(address, true).getName());

        final Uri uri = getUriStrByCursor(cursor);

        new AlertDialog.Builder(this).setTitle(title_buf)
                .setMessage(smsBody)
                .setPositiveButton(R.string.sim_copy_to_phone_memory, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Cursor cursor = (Cursor) getListView().getAdapter().getItem(position);
                        boolean success = copyToPhoneMemory(cursor);
                        showToast(success);
                        dialog.dismiss();
                    }
                })
                .setNeutralButton(R.string.menu_forward, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        forwardMessage(position);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.sim_delete, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mSelectedUri.add(uri);
                        confirmDeleteDialog(new MultiDeleteMessagesListener());
                        dialog.dismiss();
                    }
                })
                .setCancelable(true)
                .show();
        //lichao modify in 2016-08-22 end
	}
	
	private void forwardMessage(int pos) {
		Cursor cursor = (Cursor) getListView().getAdapter().getItem(pos);
		String smsBody = cursor.getString(cursor.getColumnIndexOrThrow("body"));
		Intent intent = new Intent();
		intent.putExtra("exit_on_sent", true);
		intent.putExtra("forwarded_message", true);
		intent.putExtra("sms_body", smsBody);
		intent.setClassName(getContext(), FORWARD_MESSAGE_ACTIVITY_NAME);
		startActivity(intent);
	}

    //lichao add begin
    private boolean mEditMode = false;
    private void showEditMode(){
        if(null == mListAdapter){
            return;
        }
        mEditMode = true;
        mSelectedUri.clear();
        mSelectedPos.clear();
        showActionMode(true);
        mBottomNavigationView.setVisibility(View.VISIBLE);
        mListAdapter.setCheckBoxEnable(true);
        mListAdapter.notifyDataSetChanged();
        //updateFootView();
    }

	private void changeToNormalMode() {
        if(null == mListAdapter){
            return;
        }
        mEditMode = false;
        mSelectedUri.clear();
        mSelectedPos.clear();
        showActionMode(false);
        mBottomNavigationView.setVisibility(View.GONE);
        mListAdapter.setCheckBoxEnable(false);
        mListAdapter.notifyDataSetChanged();
        //updateFootView();
	}

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (mEditMode) {
            return true;
        }
        Cursor cursor = (Cursor) getListView().getAdapter().getItem(position);
        if (cursor == null) {
            Log.w(TAG, "onItemLongClick, cursor is null, return");
            return true;
        }
        Uri uri = getUriStrByCursor(cursor);

        showEditMode();

        mSelectedUri.add(uri);
        mSelectedPos.add(position);

        if (view instanceof MySimMessageListItem) {
            MySimMessageListItem simMsgListItem = (MySimMessageListItem) view;
            simMsgListItem.setItemChecked(true);
        }

        updateActionMode();
        mListAdapter.notifyDataSetChanged();
        return true;//lichao modify to true for fix bug 3991 in 2017-07-07
    }

    private OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.sim_msg_delete:
                    confirmDeleteDialog(new MultiDeleteMessagesListener());
                    return true;
                case R.id.copy_to_phone:
                    multiCopyToPhoneMemory();
                    return true;
                default:
                    return false;
            }
        }
    };

    private void multiCopyToPhoneMemory() {
        if(null == mSelectedPos || mSelectedPos.isEmpty()){
            return;
        }
        final int select_count = mSelectedPos.size();
        int success_msg_count = 0;
        boolean success = false;
        Cursor cursor = null;
        for (int position : mSelectedPos) {
            Log.d(TAG, "multiCopyToPhoneMemory(), position:" + position);
            success = false;
            cursor = (Cursor) getListView().getAdapter().getItem(position);
            success = copyToPhoneMemory(cursor);
            if(true == success){
                success_msg_count++;
            }
        }
        final int success_count = success_msg_count;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                changeToNormalMode();
                showCopyResultToast(select_count, success_count);
            }
        });
    }

    private void showCopyResultToast(int select_count, int success_count) {
        //HB. Comments : add by Engerineer : lichao , Date : 17-6-23 , begin
        if (select_count < 1) {
            return;
        }
        //HB. end
        int failed_count = select_count - success_count;

        int success_resId = R.string.copy_to_phone_success;
        int fail_resId = R.string.copy_to_phone_fail;

        StringBuffer stringBuffer = new StringBuffer();

        //HB. Comments : modify by Engerineer : lichao , Date : 17-6-23 , begin
        if (success_count <= 0 || failed_count <= 0) {
            if (failed_count == 0) {// success_count == select_count
                stringBuffer.append(getString(success_resId));
            } else {
                stringBuffer.append(getString(fail_resId));
            }
        } else {
            stringBuffer.append(getString(R.string.copy_to_phone_success_colon));
            stringBuffer.append(success_count);
            stringBuffer.append(", ");
            stringBuffer.append(getString(R.string.copy_to_phone_fail_colon));
            stringBuffer.append(failed_count);
        }
        //HB. end

        Toast.makeText(getContext(), stringBuffer.toString(), Toast.LENGTH_SHORT).show();
    }

    /*
    private final MySimMessageListAdapter.OnSlideDeleteListener mOnSlideDeleteListener =
            new MySimMessageListAdapter.OnSlideDeleteListener() {
                @Override
                public void onDelete(Uri uri) {
                    mSelectedUri.add(uri);
                    confirmDeleteDialog(new MultiDeleteMessagesListener());
                }
            };
            */

    @Override
    public void onNavigationClicked(View view) {
        //handle click back botton on Toolbar
        onBackPressed();
    }

    //lichao add in 2016-10-27
    /*private void updateFootView() {
        if (mListCountView == null) {
            ViewStub stub = (ViewStub) mFooterView.findViewById(R.id.viewstub_conv_list_footer);
            if (stub != null) {
                stub.inflate();
            }
            mListCountView = (TextView) mFooterView.findViewById(R.id.list_footer_count_textview);
        }
        if(mEditMode){
            mFooterView.setVisibility(View.GONE);
            mListCountView.setVisibility(View.GONE);
            return;
        }
        int listCount = 0;
        if (null != mListAdapter) {
            listCount = mListAdapter.getCount();
        }
        //Log.d(TAG, "onQueryComplete, listCount = "+listCount);
        if (listCount <= 6) {
            mFooterView.setVisibility(View.GONE);
            mListCountView.setVisibility(View.GONE);
        } else {
            mFooterView.setVisibility(View.VISIBLE);
            mListCountView.setVisibility(View.VISIBLE);
            mListCountView.setText(MySimMessageList.this
                    .getString(R.string.zzz_total_sim_messages_count, listCount));
        }
    }*/

    //lichao add in 2016-11-02
    private void setToolbarMenuVisibility(boolean visible){
        if(null == myToolbar){
            return;
        }
        Menu mToolbarMenu = myToolbar.getMenu();
        MenuItem menu_edit = mToolbarMenu.findItem(R.id.action_edit);
        if(null != menu_edit){
            menu_edit.setVisible(visible);
        }
    }

    //lichao add in 2017-04-07 begin
    private void initialSubInfoList(int[] appointedSubArray) {
        //获取mSubInfoList方法一：
        // appointedSubArray 不为空的话只添加 appointedSubArray 里已有subId的 SubscriptionInfo
        // mNeedShowSubArray == null means intent isn't specified
        if(appointedSubArray == null || appointedSubArray.length == 0){
            mSubInfoList = SimStateMonitor.getInstance().getSubInfoList();
            //单卡时候 slotId = mSubInfoList.get(0).getSimSlotIndex();
            //单卡时候 subId = (int) mSubInfoList.get(0).getSubscriptionId();
            return;
        }

        int simCount = TelephonyManager.getDefault().getSimCount();
        mSubInfoList.clear();
        for (int slotId = 0; slotId < simCount; slotId++) {
            SubscriptionInfo subInfoRecordInOneSim = SubscriptionManager.from(MmsApp.getApplication())
                    .getActiveSubscriptionInfoForSimSlotIndex(slotId);
            if (subInfoRecordInOneSim == null) {
                continue;
            } else if (isSubIdInNeededShowArray(appointedSubArray,
                    subInfoRecordInOneSim.getSubscriptionId())) {
                mSubInfoList.add(subInfoRecordInOneSim);
            }
        }
    }

    private boolean isSubIdInNeededShowArray(int[] appointedSubArray, int subId) {
        for (int id : appointedSubArray) {
            if (subId == id) {
                return true;
            }
        }
        return false;
    }
    //lichao add in 2017-04-07 end

    /// M: update sim state dynamically. @{
    private BroadcastReceiver mSimReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(DEBUG) Log.d(TAG, "mSimReceiver action: " + action);
            /*if (action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
                if(initAndcheckNeedShowEmpty()){
                    Log.w(TAG, "mSimReceiver, >>>SHOW_EMPTY");
                    updateState(SHOW_EMPTY);
                }
            } else */if (action.equals(Telephony.Sms.Intents.SIM_FULL_ACTION)) {
                int subId = intent
                        .getIntExtra(PhoneConstants.SUBSCRIPTION_KEY, PhoneConstants.SIM_ID_1);
                if (mCurrentSubId == subId) {
                    mIsCurrentSimFull = true;
                }
            }
        }
    };

    /**
     *  M: listen this for finishing activity if airplane mode on.
     */
    private final BroadcastReceiver airPlaneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean airplaneOn = intent.getBooleanExtra("state", false);
            MmsLog.d(TAG, "airPlaneReceiver, airplaneOn: " + airplaneOn);
            if (airplaneOn) {
                Toast.makeText(getApplicationContext(), getString(MessageResource.string.sim_close),
                        Toast.LENGTH_SHORT).show();
                Log.d(TAG, "airPlaneReceiver onReceive(), >>>refreshMessageList()");
                refreshMessageList();
                MySimMessageList.this.finish();
            }
        }
    };

    private void dismissProgressDialog(){
        if(mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onSimStateChanged() {
        MmsLog.d(TAG, "\n onSimStateChanged(), >>>refreshMessageList()");
        refreshMessageList();
    }

    private boolean isSIMCardEnabled(int slotId){
        if (SimStateMonitor.getInstance().getAirplaneMode()) {
            return false;
        }
        if(!MessageUtils.isIccCardEnabled(slotId)){
            return false;
        }
        return true;
    }
	////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////
    //add by lgy or lichao end
	////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////

}

