/*
 * Copyright (C) 2010-2014, The Linux Foundation. All rights reserved.
 * Not a Contribution.
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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import com.android.mms.R;
import com.android.mms.ui.MessageListAdapter.ColumnsMap;
import com.google.android.mms.MmsException;

import java.util.ArrayList;
import java.util.regex.Pattern;

import hb.widget.HbListView;

//import android.graphics.Color;
//import android.widget.ListView;

/**
 * The back-end data adapter of a message list.
 */
public class MySimMessageListAdapter extends CursorAdapter {
    private static final String TAG = "MySimMessageListAdapter";
    private static final boolean LOCAL_LOGV = false;
    private static final boolean DEBUG = false;

    private static final int CACHE_SIZE         = 50;

    protected LayoutInflater mInflater;
    private final HbListView mListView;
    private final MessageItemCache mMessageItemCache;
    private final ColumnsMap mColumnsMap;
    private OnDataSetChangedListener mOnDataSetChangedListener;
    private Pattern mHighlight;
    private Context mContext;
    private boolean mIsGroupConversation;

    private float mTextSize = 0;

    public static final int SLIDER_BTN_POSITION_DELETE = 1;
    private final LayoutInflater mFactory;

    public MySimMessageListAdapter(
            Context context, Cursor c, HbListView listView,
            boolean useDefaultColumnsMap, Pattern highlight) {
        super(context, c, FLAG_REGISTER_CONTENT_OBSERVER);
        mContext = context;
        mHighlight = highlight;

        mInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        mMessageItemCache = new MessageItemCache(CACHE_SIZE);
        mListView = listView;

        if (useDefaultColumnsMap) {
            mColumnsMap = new ColumnsMap();
        } else {
            mColumnsMap = new ColumnsMap(c);
        }
        mFactory = LayoutInflater.from(context);
    }

    public class SimMessageListViewHolder {
        MySimMessageListItem headerView;
        public SimMessageListViewHolder(View view) {
            headerView = (MySimMessageListItem) view.findViewById(R.id.sim_msg_list_item);
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        SimMessageListViewHolder viewHolder = (SimMessageListViewHolder) view.getTag();
        if(null == viewHolder){
            viewHolder = new SimMessageListViewHolder(view);
            view.setTag(viewHolder);
        }

        final Uri uri = getUriStrByCursor(cursor);
        viewHolder.headerView.setCheckBoxEnable(mCheckBoxEnable);
        viewHolder.headerView.setChecked(mSelectedUri.contains(uri));

        String type = cursor.getString(mColumnsMap.mColumnMsgType);
        long msgId = cursor.getLong(mColumnsMap.mColumnMsgId);
        MessageItem msgItem = getCachedMessageItem(type, msgId, cursor);
        if (msgItem != null) {
            int position = cursor.getPosition();
            viewHolder.headerView.bindSimMessage(msgItem, mIsGroupConversation, position);
        }

        //View mAvatar = view.findViewById(R.id.avatar);
        //mAvatar.setVisibility(View.GONE);
        //lichao modify in 2016-08-09 end
    }

    @Override
    public long getItemId(int position) {
        if (getCursor() != null) {
            getCursor().moveToPosition(position);
            return position;
        }
        return 0;
    }

    public interface OnDataSetChangedListener {
        void onDataSetChanged(MySimMessageListAdapter adapter);
        void onContentChanged(MySimMessageListAdapter adapter);
    }

    public void setOnDataSetChangedListener(OnDataSetChangedListener l) {
        mOnDataSetChangedListener = l;
    }

    public void setIsGroupConversation(boolean isGroup) {
        mIsGroupConversation = isGroup;
    }

    public void cancelBackgroundLoading() {
        mMessageItemCache.evictAll();   // causes entryRemoved to be called for each MessageItem
                                        // in the cache which causes us to cancel loading of
                                        // background pdu's and images.
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        if (LOCAL_LOGV) {
            Log.v(TAG, "MySimMessageListAdapter.notifyDataSetChanged().");
        }

        mMessageItemCache.evictAll();

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

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mFactory.inflate(R.layout.zzz_sim_message_list_item, parent, false);
    }

    public MessageItem getCachedMessageItem(String type, long msgId, Cursor c) {
        MessageItem item = mMessageItemCache.get(getKey(type, msgId));
        if (item == null && c != null && isCursorValid(c)) {
            try {
                item = new MessageItem(mContext, type, c, mColumnsMap, mHighlight, false, 0L, mIsGroupConversation);
                mMessageItemCache.put(getKey(item.mType, item.mMsgId), item);
            } catch (MmsException e) {
                Log.e(TAG, "getCachedMessageItem: ", e);
            }
        }
        return item;
    }

    private boolean isCursorValid(Cursor cursor) {
        // Check whether the cursor is valid or not.
        if (cursor == null || cursor.isClosed() || cursor.isBeforeFirst() || cursor.isAfterLast()) {
            return false;
        }
        return true;
    }

    private static long getKey(String type, long id) {
        if (type.equals("mms")) {
            return -id;
        } else {
            return id;
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    public Cursor getCursorForItem(MessageItem item) {
        Cursor cursor = getCursor();
        if (isCursorValid(cursor)) {
            if (cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(mRowIDColumn);
                    String type = cursor.getString(mColumnsMap.mColumnMsgType);
                    if (id == item.mMsgId && (type != null && type.equals(item.mType))) {
                        return cursor;
                    }
                } while (cursor.moveToNext());
            }
        }
        return null;
    }


    private static class MessageItemCache extends LruCache<Long, MessageItem> {
        public MessageItemCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected void entryRemoved(boolean evicted, Long key,
                MessageItem oldValue, MessageItem newValue) {
            oldValue.cancelPduLoading();
        }
    }

    public void setTextSize(float size) {
        mTextSize = size;
    }
    
    private ArrayList<Uri> mSelectedUri = new ArrayList<Uri>();
    

	public void setCheckList(ArrayList<Uri> l) {
		mSelectedUri = l;
    }
    
	private boolean mCheckBoxEnable = false;
	
	public void setCheckBoxEnable(boolean flag) {
        mCheckBoxEnable = flag;
    }
    
    public boolean getCheckBoxEnable() {
        return mCheckBoxEnable;
    }
    
	
    private Uri getUriStrByCursor(Cursor cursor) {
        String messageIndexString = cursor.getString(cursor.getColumnIndexOrThrow("index_on_icc"));
        return mIccUri.buildUpon().appendPath(messageIndexString).build();
    }
    
    private Uri mIccUri;
    public void setIccUri(Uri uri) {
    	mIccUri = uri;
    }
    
}
