/*
 * Copyright (c) 2014, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 *
 * Copyright (C) 2012 The Android Open Source Project.
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

import static com.android.mms.ui.ZzzMessageGroupListAdapter.PROJECTION;

import java.util.ArrayList;

import hb.app.dialog.AlertDialog;
import hb.app.dialog.ProgressDialog;
import hb.app.HbActivity;
import hb.widget.toolbar.Toolbar;
import hb.widget.HbListView;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.telephony.SmsManager;
import android.text.TextUtils;

import com.android.mms.LogTag;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;
import com.android.mms.data.Conversation;
import com.android.mms.data.Conversation.ConversationQueryHandler;
import com.android.mms.transaction.MessagingNotification;
import com.android.mms.transaction.SmsReceiverService;
import com.android.mms.ui.MessageUtils;
import com.android.mms.util.MmsLog;
import com.android.mms.util.ThreadCountManager;
import com.mediatek.mms.folder.util.FolderModeUtils;
import com.zzz.provider.Telephony.Sms;
import android.database.sqlite.SqliteWrapper;
/**
 * hummingbird add by tangyisen for  hb style 2017.3.28
 * */
public class ZzzMessageItemGroupDetailActivity extends HbActivity{

    private static final String TAG = "Mms/ZzzMIGDActivity";
    HbListView mContent;
    Toolbar myToolbar;
    ZzzMessageGroupListAdapter mZzzMessageGroupListAdapter;
    ContentResolver mContentResolver;
    //private Conversation mConversation;
    private long groupId;
    public static final String GROUP_ID_KEY = "group_id";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHbContentView(R.layout.zzz_message_list_group_activity);
        mContentResolver = getContentResolver();
        mBackgroundQueryHandler = new BackgroundQueryHandler(mContentResolver);
        handleIntent();
        initUi();
        startMsgListQuery();
    }

    private void handleIntent() {
        Intent intent = getIntent();
        groupId = intent.getLongExtra(GROUP_ID_KEY, 0);

        // Cancel failed notification. if need
        /*MessageUtils.cancelFailedToDeliverNotification(intent, this);
        MessageUtils.cancelFailedDownloadNotification(intent, this);*/

        if (groupId == 0) {
            Log.e(TAG, "There's no sms uri!");
            finish();
        }
    }

    private final ZzzMessageGroupListAdapter.OnDataSetChangedListener
            mDataSetChangedListener = new ZzzMessageGroupListAdapter.OnDataSetChangedListener() {


        @Override
        public void onDataSetChanged(ZzzMessageGroupListAdapter adapter) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onContentChanged(ZzzMessageGroupListAdapter adapter) {
            // TODO Auto-generated method stub
            if (mZzzMessageGroupListAdapter != null &&
                    mZzzMessageGroupListAdapter.getOnDataSetChangedListener() != null) {
                MmsLog.d(TAG, "OnDataSetChangedListener is not cleared");
                startMsgListQuery();
            } else {
                MmsLog.d(TAG, "OnDataSetChangedListener is cleared");
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBackgroundQueryHandler != null) {
            MmsLog.d(TAG, "clear pending queries in onDestroy");
            mBackgroundQueryHandler.cancelOperation(MESSAGE_GROUP_LIST_QUERY_TOKEN);
        }
        if (mZzzMessageGroupListAdapter != null) {
            mZzzMessageGroupListAdapter.changeCursor(null);
            /// M: Remove listener @{
            mZzzMessageGroupListAdapter.setOnDataSetChangedListener(null);
            /// @}
        }
    }

    private void initUi() {
        myToolbar = getToolbar();
        myToolbar.setTitle(R.string.group_send_detail);
        mContent = (HbListView)findViewById(R.id.group_detail_list_view);
        mZzzMessageGroupListAdapter = new ZzzMessageGroupListAdapter(this, null, mContent, true);
        mZzzMessageGroupListAdapter.setOnDataSetChangedListener(mDataSetChangedListener);
        mContent.setAdapter(mZzzMessageGroupListAdapter);
    }

    @Override
    public void onNavigationClicked(View view) {
        if(!(view instanceof TextView)) {
            finish();
        }
    }

    private void startMsgListQuery() {
        try {
            /*Uri uri = ContentUris.withAppendedId(
                    Sms.CONTENT_URI, groupId);*/
            String selection = "_id =" + groupId + " OR group_id = " + groupId;
            mBackgroundQueryHandler.startQuery(
                    MESSAGE_GROUP_LIST_QUERY_TOKEN,
                    groupId /* cookie */,
                    Sms.CONTENT_URI,
                    PROJECTION,
                    selection, null, null);
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(this, e);
        }
    }

    BackgroundQueryHandler mBackgroundQueryHandler;
    private static final int MESSAGE_GROUP_LIST_QUERY_TOKEN = 0;
    private final class BackgroundQueryHandler extends AsyncQueryHandler {
        public BackgroundQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch(token) {
                case MESSAGE_GROUP_LIST_QUERY_TOKEN:
                    if (cursor == null) {
                        return;
                    }
                    /// M: If adapter or listener has been cleared, just close this cursor@{
                    if (mZzzMessageGroupListAdapter == null) {
                        cursor.close();
                        return;
                    }
                    mZzzMessageGroupListAdapter.changeCursor(cursor);
                    break;
                default:
                    break;
            }
        }
    }
}
