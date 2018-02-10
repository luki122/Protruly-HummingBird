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

package com.android.mms.transaction;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.android.mms.ui.MyMessagingPreferenceActivity;
import com.android.mms.util.MmsLog;
import com.google.android.mms.MmsException;
import com.mediatek.mms.ext.IOpSmsMessageSenderExt;
import com.mediatek.opmsg.util.OpMessageUtils;
import com.zzz.provider.Telephony.Sms;
import com.zzz.provider.Telephony.Sms.Inbox;

//import android.provider.Telephony.Sms;
//import android.provider.Telephony.Sms.Inbox;
/// M:
//tangyisen add


public class SmsMessageSender implements MessageSender {
    protected final Context mContext;
    protected final int mNumberOfDests;
    private final String[] mDests;
    protected final String mMessageText;
    protected final String mServiceCenter;
    protected final long mThreadId;
    protected int mSubId;
    protected long mTimestamp;
    private static final String TAG = "SmsMessageSender";

    // Default preference values
    private static final boolean DEFAULT_DELIVERY_REPORT_MODE  = false;

    private static final String[] SERVICE_CENTER_PROJECTION = new String[] {
        Sms.Conversations.REPLY_PATH_PRESENT,
        Sms.Conversations.SERVICE_CENTER,
    };

    private static final int COLUMN_REPLY_PATH_PRESENT = 0;
    private static final int COLUMN_SERVICE_CENTER     = 1;

    // M: add for op
    private IOpSmsMessageSenderExt mOpSmsMessageSender = null;

    public SmsMessageSender(Context context, String[] dests, String msgText, long threadId,
            int subId) {
        mContext = context;
        mMessageText = msgText;
        if (dests != null) {
            mNumberOfDests = dests.length;
            mDests = new String[mNumberOfDests];
            System.arraycopy(dests, 0, mDests, 0, mNumberOfDests);
        } else {
            mNumberOfDests = 0;
            mDests = null;
        }
        mTimestamp = System.currentTimeMillis();
        mThreadId = threadId;
        mSubId = subId;
        mServiceCenter = getOutgoingServiceCenter(mThreadId);
        mOpSmsMessageSender = OpMessageUtils.getOpMessagePlugin()
                .getOpSmsMessageSenderExt();
    }

    //begin tangyisen add for just update sms when resend in group
    protected Uri mSmsUri;
    public SmsMessageSender(Context context, Uri smsUri) {
        mContext = context;
        mSmsUri = smsUri;
        mTimestamp = System.currentTimeMillis();
        mNumberOfDests = 1;
        mDests = null;
        mMessageText = null;
        mServiceCenter = null;
        mThreadId = 0;
    }

    public boolean sendMessage(boolean isGroupResend) throws MmsException {
        if(!isGroupResend || mSmsUri == null) {
            return false;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean requestDeliveryReport = prefs.getBoolean(MyMessagingPreferenceActivity.SMS_DELIVERY_REPORT_MODE,
                DEFAULT_DELIVERY_REPORT_MODE);
        ContentValues values = new ContentValues(5);//4
        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_QUEUED);
        values.put(Sms.DATE, mTimestamp);
        //因为provider里给READ默认赋值为0，所以这里还是需要put(1)
        values.put(Sms.READ, Integer.valueOf(1));
        values.put(Sms.FAILED_UNREAD, Integer.valueOf(0));//lichao modify for failed_unread
        if (requestDeliveryReport) {
            values.put(Sms.STATUS, Sms.STATUS_PENDING);
        } else {
            values.put(Sms.STATUS, Sms.STATUS_NONE);//default is -1
        }
        SqliteWrapper.update(mContext, mContext.getContentResolver(), mSmsUri, values, null, null);
        mContext.sendBroadcast(new Intent(SmsReceiverService.ACTION_SEND_MESSAGE,
                null,
                mContext,
                SmsReceiver.class));
        return false;
    }
    //end tangyisen

    public boolean sendMessage(long token) throws MmsException {
        // In order to send the message one by one, instead of sending now, the message will split,
        // and be put into the queue along with each destinations
        return queueMessage(token);
    }

    private boolean queueMessage(long token) throws MmsException {
        /// M:
        MmsLog.v(MmsApp.TXN_TAG, "queueMessage()");
        if ((mMessageText == null) || mMessageText.isEmpty() || (mNumberOfDests == 0)) {
            // Don't try to send an empty message.
            throw new MmsException("Null message body or dest.");
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        //boolean requestDeliveryReport = prefs.getBoolean(mSubId + "_" + SmsPreferenceActivity.SMS_DELIVERY_REPORT_MODE,
        //        DEFAULT_DELIVERY_REPORT_MODE);
        boolean requestDeliveryReport = prefs.getBoolean(MyMessagingPreferenceActivity.SMS_DELIVERY_REPORT_MODE,
                DEFAULT_DELIVERY_REPORT_MODE);
        MmsLog.d(MmsApp.TXN_TAG, "SMS DR request=" + requestDeliveryReport);
        /// @}
        long timeStamp = System.currentTimeMillis();
        //tangyisen add
        long groupId = 0L;
        Uri smsUri = null;
        for (int i = 0; i < mNumberOfDests; i++) {
            try {
                if (LogTag.DEBUG_SEND) {
                    Log.v(TAG, "queueMessage mDests[i]: " + mDests[i] + " mThreadId: " + mThreadId);
                }

                //tangyisen delete
                if(mNumberOfDests > 1) {
                    if(i == 0) {
                        groupId = -1L;
                    } else if(groupId == -1L){
                        if(smsUri != null) {
                            groupId = Long.parseLong( smsUri.getLastPathSegment());
                        }
                    }
                }
                /*Uri smsUri = mOpSmsMessageSender.queueMessage(mNumberOfDests,
                        mContext.getContentResolver(), mDests[i], mMessageText, mTimestamp,
                        requestDeliveryReport, mThreadId, mSubId, -timeStamp);*/

                //if (smsUri == null) {
                smsUri = Sms.addMessageToUri(mSubId,
                            mContext.getContentResolver(),
                            Uri.parse("content://sms/queued"), mDests[i],
                            mMessageText, null, null, mTimestamp, //tangyisen add null
                            true /* read */,
                            requestDeliveryReport,
                            mThreadId, groupId);//tangyisen add groupId
                //}
            } catch (SQLiteException e) {
                if (LogTag.DEBUG_SEND) {
                    Log.e(TAG, "queueMessage SQLiteException", e);
                }
                SqliteWrapper.checkSQLiteException(mContext, e);
            }
        }
        // Notify the SmsReceiverService to send the message out
        mContext.sendBroadcast(new Intent(SmsReceiverService.ACTION_SEND_MESSAGE,
                null,
                mContext,
                SmsReceiver.class));
        return false;
    }

    /**
     * Get the service center to use for a reply.
     *
     * The rule from TS 23.040 D.6 is that we send reply messages to
     * the service center of the message to which we're replying, but
     * only if we haven't already replied to that message and only if
     * <code>TP-Reply-Path</code> was set in that message.
     *
     * Therefore, return the service center from the most recent
     * message in the conversation, but only if it is a message from
     * the other party, and only if <code>TP-Reply-Path</code> is set.
     * Otherwise, return null.
     */
    private String getOutgoingServiceCenter(long threadId) {
        Cursor cursor = null;

        try {
            cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(),
                            Inbox.CONTENT_URI, SERVICE_CENTER_PROJECTION,
                            "thread_id = " + threadId, null, "date DESC");

            if ((cursor == null) || !cursor.moveToFirst()) {
                return null;
            }

            boolean replyPathPresent = (1 == cursor.getInt(COLUMN_REPLY_PATH_PRESENT));
            return replyPathPresent ? cursor.getString(COLUMN_SERVICE_CENTER) : null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void log(String msg) {
        Log.d(LogTag.TAG, "[SmsMsgSender] " + msg);
    }
}
