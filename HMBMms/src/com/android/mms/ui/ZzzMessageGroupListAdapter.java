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

import static com.android.mms.ui.MessageListAdapter.COLUMN_ID;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.StaleDataException;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.BaseColumns;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.mms.MmsApp;
import com.android.mms.R;
import com.google.android.mms.MmsException;

import java.util.regex.Pattern;
import android.telephony.SubscriptionManager;


/// M:
import android.content.ContentUris;
import android.net.Uri;
import android.os.Message;
import android.telephony.SmsManager;

import com.android.mms.data.Contact;
import com.android.mms.transaction.MessageSender;
import com.android.mms.transaction.SmsMessageSender;
import com.android.mms.ui.MessageItem.DeliveryStatus;
import com.android.mms.util.MmsLog;
import com.android.mms.util.Recycler;

import com.zzz.provider.Telephony;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
//begin tangyisen
import com.zzz.provider.Telephony.Mms;
import com.zzz.provider.Telephony.MmsSms;
import com.zzz.provider.Telephony.MmsSms.PendingMessages;
import com.zzz.provider.Telephony.Sms;
import com.zzz.provider.Telephony.Sms.Conversations;
import com.zzz.provider.Telephony.TextBasedSmsColumns;
import hb.app.dialog.AlertDialog;
//end tangyisen

/**
 * The back-end data adapter of a message list.
 */
public class ZzzMessageGroupListAdapter extends MessageCursorAdapter {
    private static final String TAG = "Mms/ZzzMGListAdapter";

    private static final boolean LOCAL_LOGV = false;
    private boolean mIsShowSendStatus = false;

    static final String[] PROJECTION = new String[] {
            BaseColumns._ID,
            Conversations.THREAD_ID,
            // For SMS
            Sms.ADDRESS,
            Sms.BODY,
            Sms.DATE,
            Sms.DATE_SENT,
            Sms.READ,
            Sms.TYPE,
            Sms.STATUS,
            Sms.LOCKED,
            Sms.ERROR_CODE,
            Telephony.Sms.SUBSCRIPTION_ID,
            //begin tangyisen
            Sms.GROUP_ID,
            Sms.NOTI_COUNT,
            Sms.FAILS_COUNT,
            //end tangyisen
    };

    static final int COLUMN_ID                  = 0;
    static final int COLUMN_THREAD_ID           = 1;
    static final int COLUMN_SMS_ADDRESS         = 2;
    static final int COLUMN_SMS_BODY            = 3;
    static final int COLUMN_SMS_DATE            = 4;
    static final int COLUMN_SMS_DATE_SENT       = 5;
    static final int COLUMN_SMS_READ            = 6;
    static final int COLUMN_SMS_TYPE            = 7;
    static final int COLUMN_SMS_STATUS          = 8;
    static final int COLUMN_SMS_LOCKED          = 9;
    static final int COLUMN_SMS_ERROR_CODE      = 10;
    static final int COLUMN_SMS_SUBID           = 11;
    static final int COLUMN_SMS_GROUP_ID = 12;
    static final int COLUMN_SMS_NOTI_COUNT = 13;
    static final int COLUMN_SMS_FAILS_COUNT = 14;

    protected LayoutInflater mInflater;
    private final ColumnsMap mColumnsMap;
    private Context mContext;


    private static int VIEW_TYPE_COUNT = 1;


    public ZzzMessageGroupListAdapter(
            Context context, Cursor c, ListView listView,
            boolean useDefaultColumnsMap) {
        super(context, c, FLAG_REGISTER_CONTENT_OBSERVER);
        mContext = context;

        mInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        if (useDefaultColumnsMap) {
            mColumnsMap = new ColumnsMap();
        } else {
            mColumnsMap = new ColumnsMap(c);
        }
        mIsShowSendStatus = MessageUtils.getPreferenceValueBoolean( context, "pref_key_sms_delivery_reports", false );
    }

    public enum DeliveryStatus  { NONE, INFO, FAILED, PENDING }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder=(ViewHolder) view.getTag();
        /*String name=cursor.getString(cursor.getColumnIndex(PersonInfo.NAME));
        String date=cursor.getString(cursor.getColumnIndex(PersonInfo.PHONENUMBER));
        String date=cursor.getString(cursor.getColumnIndex(PersonInfo.PHONENUMBER));
        String date=cursor.getString(cursor.getColumnIndex(PersonInfo.PHONENUMBER));*/
        long status = cursor.getLong(mColumnsMap.mColumnSmsStatus);
        int mBoxId = cursor.getInt(mColumnsMap.mColumnSmsType);
        DeliveryStatus mDeliveryStatus = DeliveryStatus.NONE;
        if (status >= Sms.STATUS_FAILED) {
            // Failure
            mDeliveryStatus = DeliveryStatus.FAILED;
        } else if (status >= Sms.STATUS_PENDING) {
            // Pending
            mDeliveryStatus = DeliveryStatus.PENDING;
        } else {
            mDeliveryStatus = DeliveryStatus.NONE;
        }
        String mBody = cursor.getString(mColumnsMap.mColumnSmsBody);
        String address = cursor.getString(mColumnsMap.mColumnSmsAddress);
        Contact contact = Contact.get( address, true );
        long date = cursor.getLong(mColumnsMap.mColumnSmsDate);
        String mTimestamp = MessageUtils.formatTimeStampStringForItem(context, date);
        int subId = cursor.getInt(mColumnsMap.mColumnSmsSubId);
        viewHolder.displayNameView.setText(contact.getName());
        viewHolder.dateView.setText(mTimestamp);
        updateSimIcon(context, SubscriptionManager.getSlotId(subId), viewHolder.simView);
        updateSendStatus( viewHolder.sendStatusView, mBoxId);
        updateFailView(mBoxId, mDeliveryStatus, viewHolder.failView, viewHolder.sendStatusView);
        Uri smsUri = ContentUris.withAppendedId(
                        Sms.CONTENT_URI, cursor.getLong(mColumnsMap.mColumnMsgId));
        viewHolder.failView.setTag( smsUri );
        viewHolder.bodyView.setText(mBody);
    }

    private void updateFailView(int box, DeliveryStatus deliveryStatus, View failView, TextView sendView) {
        if ( isFailedMessage(box) ||
            deliveryStatus == DeliveryStatus.FAILED) {
            MessageUtils.showView(failView);
        } else {
            MessageUtils.hideView(failView);
            if(sendView.getVisibility() == View.GONE) {
                if (mIsShowSendStatus) {
                    MessageUtils.showView( sendView );
                    sendView.setText( R.string.message_item_sent_sucess );
                }
            }
        }
    }

    public boolean isFailedMessage(int box) {
        boolean isFailedSms = box == Sms.MESSAGE_TYPE_FAILED;
        return isFailedSms;
    }

    public boolean isSending(int box) {
        return !isFailedMessage(box) && isOutgoingMessage(box);
    }

    public boolean isOutgoingMessage(int box) {
        boolean isOutgoingSms = (box == Sms.MESSAGE_TYPE_FAILED)
                                            || (box == Sms.MESSAGE_TYPE_OUTBOX)
                                            || (box == Sms.MESSAGE_TYPE_QUEUED);
        return isOutgoingSms;
    }

    private void updateSendStatus( TextView sendStatus, int box) {
        if (isSending(box)) {
            MessageUtils.showView( sendStatus );
            sendStatus.setText( R.string.message_item_is_sending );
        }else if(sendStatus.getVisibility() == View.VISIBLE) {
            MessageUtils.hideView( sendStatus );
        }
    }

    private void updateSimIcon(Context context, int slotId, ImageView simView) {
        boolean isShowSimIcon = MessageUtils.isTwoSimCardEnabled() && slotId >= 0;
        if(isShowSimIcon) {
            simView.setVisibility( View.VISIBLE );
            Drawable mSimIndicatorIcon = MessageUtils.getMultiSimIcon(context, slotId);
            simView.setImageDrawable(mSimIndicatorIcon);
        }else{
            simView.setVisibility( View.GONE );
        }
   }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view=mInflater.inflate(R.layout.zzz_message_list_item_group_detail, parent, false);
        ViewHolder viewHolder= new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    private class ViewHolder {
        public final TextView displayNameView;
        public final TextView dateView;
        public final ImageView simView;
        public final TextView sendStatusView;
        public final TextView failView;
        public final TextView bodyView;

        public ViewHolder(View view) {
            displayNameView = (TextView) view.findViewById(R.id.contacts_name_view);
            dateView = (TextView) view.findViewById(R.id.date_view);
            simView = (ImageView) view.findViewById(R.id.sim_view);
            sendStatusView = (TextView) view.findViewById(R.id.send_status);
            failView = (TextView) view.findViewById(R.id.failed_resend);
            if(null != failView) {
                Drawable d = mContext.getResources().getDrawable(R.drawable.zzz_ic_message_failed_resend);
                d.setBounds(0, 0, d.getIntrinsicHeight(), d.getIntrinsicHeight());
                failView.setCompoundDrawables(null, null, d, null);
                failView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        // TODO Auto-generated method stub
                        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                        builder.setCancelable(true);
                        builder.setMessage(mContext.getResources().getString( R.string.besure_resend_text ));
                        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                Uri smsUri = (Uri)failView.getTag();
                                if(smsUri == null) {
                                    return;
                                }
                                SmsMessageSender sender = new SmsMessageSender(mContext, smsUri);
                                try {
                                    sender.sendMessage(true);
                                } catch (MmsException mmse) {
                                    mmse.printStackTrace();
                                } catch(Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        builder.setNegativeButton(R.string.no, null);
                        builder.show();
                    }
                });
            }
            bodyView = (TextView) view.findViewById(R.id.detail_body);
        }
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        return 1;
    }


    public static class ColumnsMap {
        public int mColumnMsgId;
        public int mColumnSmsAddress;
        public int mColumnSmsBody;
        public int mColumnSmsDate;
        public int mColumnSmsDateSent;
        public int mColumnSmsType;
        public int mColumnSmsStatus;
        public int mColumnSmsLocked;
        public int mColumnSmsErrorCode;
        public int mColumnSmsSubId;
        public int mColumnSmsGroupId;
        public int mColumnSmsNotiCount;
        public int mColumnSmsFailsCount;

        public ColumnsMap() {
            mColumnMsgId              = COLUMN_ID;
            mColumnSmsAddress         = COLUMN_SMS_ADDRESS;
            mColumnSmsBody            = COLUMN_SMS_BODY;
            mColumnSmsDate            = COLUMN_SMS_DATE;
            mColumnSmsDateSent        = COLUMN_SMS_DATE_SENT;

            mColumnSmsType            = COLUMN_SMS_TYPE;
            mColumnSmsStatus          = COLUMN_SMS_STATUS;
            mColumnSmsLocked          = COLUMN_SMS_LOCKED;
            mColumnSmsErrorCode       = COLUMN_SMS_ERROR_CODE;
            mColumnSmsSubId           = COLUMN_SMS_SUBID;
            mColumnSmsGroupId          =  COLUMN_SMS_GROUP_ID;
            mColumnSmsNotiCount          = COLUMN_SMS_NOTI_COUNT;
            mColumnSmsFailsCount         = COLUMN_SMS_FAILS_COUNT;
        }

        public ColumnsMap(Cursor cursor) {
            // Ignore all 'not found' exceptions since the custom columns
            // may be just a subset of the default columns.
            try {
                mColumnMsgId = cursor.getColumnIndexOrThrow(BaseColumns._ID);
            } catch (IllegalArgumentException e) {
                Log.w("colsMap", e.getMessage());
            }

            try {
                mColumnSmsAddress = cursor.getColumnIndexOrThrow(Sms.ADDRESS);
            } catch (IllegalArgumentException e) {
                Log.w("colsMap", e.getMessage());
            }

            try {
                mColumnSmsBody = cursor.getColumnIndexOrThrow(Sms.BODY);
            } catch (IllegalArgumentException e) {
                Log.w("colsMap", e.getMessage());
            }

            try {
                mColumnSmsDate = cursor.getColumnIndexOrThrow(Sms.DATE);
            } catch (IllegalArgumentException e) {
                Log.w("colsMap", e.getMessage());
            }

            try {
                mColumnSmsDateSent = cursor.getColumnIndexOrThrow(Sms.DATE_SENT);
            } catch (IllegalArgumentException e) {
                Log.w("colsMap", e.getMessage());
            }

            try {
                mColumnSmsType = cursor.getColumnIndexOrThrow(Sms.TYPE);
            } catch (IllegalArgumentException e) {
                Log.w("colsMap", e.getMessage());
            }

            try {
                mColumnSmsStatus = cursor.getColumnIndexOrThrow(Sms.STATUS);
            } catch (IllegalArgumentException e) {
                Log.w("colsMap", e.getMessage());
            }

            try {
                mColumnSmsLocked = cursor.getColumnIndexOrThrow(Sms.LOCKED);
            } catch (IllegalArgumentException e) {
                Log.w("colsMap", e.getMessage());
            }

            try {
                mColumnSmsErrorCode = cursor.getColumnIndexOrThrow(Sms.ERROR_CODE);
            } catch (IllegalArgumentException e) {
                Log.w("colsMap", e.getMessage());
            }

            try {
                mColumnSmsSubId = cursor.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID);
            } catch (IllegalArgumentException e) {
                MmsLog.w(TAG, e.getMessage());
            }

            try {
                mColumnSmsGroupId = cursor.getColumnIndexOrThrow(Sms.GROUP_ID);
            } catch (IllegalArgumentException e) {
                MmsLog.w(TAG, e.getMessage());
            }
            try {
                mColumnSmsNotiCount = cursor.getColumnIndexOrThrow(Sms.NOTI_COUNT);
            } catch (IllegalArgumentException e) {
                MmsLog.w(TAG, e.getMessage());
            }
            try {
                mColumnSmsFailsCount = cursor.getColumnIndexOrThrow(Sms.FAILS_COUNT);
            } catch (IllegalArgumentException e) {
                MmsLog.w(TAG, e.getMessage());
            }
        }

        public int getColumnSmsSubId() {
            return mColumnSmsSubId;
        }

        public int getColumnSmsAddress() {
            return mColumnSmsAddress;
        }

        public int getColumnSmsBody() {
            return mColumnSmsBody;
        }

        public int getColumnSmsType() {
            return mColumnSmsType;
        }

        public int getColumnMsgId() {
            return mColumnMsgId;
        }

        public int getColumnSmsGroupId() {
            return mColumnSmsGroupId;
        }
        public int getColumnSmsNotiCount() {
            return mColumnSmsNotiCount;
        }
        public int getColumnSmsFailsCount() {
            return mColumnSmsFailsCount;
        }
    }

    private OnDataSetChangedListener mOnDataSetChangedListener;
    public interface OnDataSetChangedListener {
        void onDataSetChanged(ZzzMessageGroupListAdapter adapter);
        void onContentChanged(ZzzMessageGroupListAdapter adapter);
    }

    public void setOnDataSetChangedListener(OnDataSetChangedListener l) {
        mOnDataSetChangedListener = l;
    }

    public OnDataSetChangedListener getOnDataSetChangedListener() {
        return mOnDataSetChangedListener;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        if (LOCAL_LOGV) {
            Log.v(TAG, "ZzzMessageGroupListAdapter.notifyDataSetChanged().");
        }

        if (mOnDataSetChangedListener != null) {
            mOnDataSetChangedListener.onDataSetChanged(this);
        }
    }

    @Override
    protected void onContentChanged() {
        if (getCursor() != null && !getCursor().isClosed()) {
            if (mOnDataSetChangedListener != null) {
                mOnDataSetChangedListener.onContentChanged(this);
            }
        }
    }
}