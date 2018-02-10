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

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

//import com.android.mms.util.PDebug;
import com.android.mms.R;
import com.android.mms.data.Conversation;
import com.android.mms.util.MmsLog;

import java.util.HashSet;
/// M:
/**
 * The back-end data adapter for ConversationList.
 */
//TODO: This should be public class ConversationListAdapter extends ArrayAdapter<Conversation>
public class ConversationListAdapter extends MessageCursorAdapter implements AbsListView.RecyclerListener {
    private static final String TAG = "Mms/ConvListAdapter";
    private static final boolean DEBUG = true;

    private final LayoutInflater mFactory;
    private OnContentChangedListener mOnContentChangedListener;

    private static HashSet<Long> sSelectedTheadsId;//mtk add
    private HashSet<String> mBlackNumSet;//lichao add

    public ConversationListAdapter(Context context, Cursor cursor) {
        super(context, cursor, false /* auto-requery */);
        mFactory = LayoutInflater.from(context);
        sSelectedTheadsId = new HashSet<>();
        mBlackNumSet = new HashSet<>();
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        //PDebug.EndAndStart("onQueryComplete -> changeCursor", "ConversationListAdapter.bindView");
        /*if (!(view instanceof ConversationListItem) ||
                cursor == null || cursor.getPosition() < 0) {
            Log.e(TAG, "Unexpected bound view: " + view);
            return;
        }*/
        //ConversationListItem headerView = (ConversationListItem) view;
        ConversationListViewHolder viewHolder = (ConversationListViewHolder) view.getTag();
        if(null == viewHolder){
            viewHolder = new ConversationListViewHolder(view);
            view.setTag(viewHolder);
        }

        viewHolder.headerView.setCheckBoxEnable(mCheckBoxEnable);

        viewHolder.headerView.setScreenWidthDip(mScreenWidthDip);
        //Conversation conv = Conversation.from(context, cursor);
        //headerView.bind(context, conv);
        /// M: Code analyze 027, For bug ALPS00331731, set conversation cache . @{
        Conversation conv;
        if (!mIsScrolling) {
            Conversation.setNeedCacheConv(false);
            //if (DEBUG) Log.d(TAG, "bindView, >>>Conversation.from");
            conv = Conversation.from(context, cursor);
            Conversation.setNeedCacheConv(true);
            if (conv != null) {
                conv.setIsChecked(sSelectedTheadsId.contains(conv.getThreadId()));
            }
            viewHolder.headerView.bind(context, conv, mBlackNumSet);
        } else {
            conv = Conversation.getConvFromCache(context, cursor);
            if (conv != null) {
                conv.setIsChecked(sSelectedTheadsId.contains(conv.getThreadId()));
            }
            viewHolder.headerView.bindDefault(conv);
        }
        /// @}
        //PDebug.End("ConversationListAdapter.bindView");
    }

    @Override
    public void onMovedToScrapHeap(View view) {
        //if(DEBUG) Log.v(TAG, " == onMovedToScrapHeap ==");
	    //lichao modify begin
        //ConversationListItem headerView = (ConversationListItem)view;
        ConversationListItem headerView = (ConversationListItem) view.findViewById(R.id.conv_list_item);
        //headerView.unbind();
        if(headerView != null) {
            //only ResetCheckBox one time in the lifecycle of ConversationListAdapter
        	headerView.unbind(!mCheckBoxEnable /*resetCheckBox*/);
        }
		//lichao modify end
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        //if (DEBUG) Log.v(TAG, "inflating new view");
        //return mFactory.inflate(R.layout.conversation_list_item, parent, false);
        return mFactory.inflate(R.layout.zzz_conversation_list_item, parent, false);
    }

    public interface OnContentChangedListener {
        void onContentChanged(ConversationListAdapter adapter);
    }

    public void setOnContentChangedListener(OnContentChangedListener l) {
        mOnContentChangedListener = l;
    }

    @Override
    protected void onContentChanged() {
        if (mCursor != null && !mCursor.isClosed()) {
            if (mOnContentChangedListener != null) {
                mOnContentChangedListener.onContentChanged(this);
            }
        }
    }

    /// MTK: Code analyze 026, personal use, caculate time . @{
     @Override
     public void notifyDataSetChanged() {
         super.notifyDataSetChanged();
         if (DEBUG) Log.d(TAG, "==notifyDataSetChanged()==");
         MmsLog.i(TAG, "[Performance test][Mms] loading data end time ["
                 + System.currentTimeMillis() + "]");
     }
     /// @}

    /// MTK: Code analyze 007, For bug ALPS00242955, If adapter data is valid . @{
    public boolean isDataValid() {
        return mDataValid;
    }
    /// @}

    /// MTK: For ConversationList to check listener @{
    public OnContentChangedListener getOnContentChangedListener() {
        return mOnContentChangedListener;
    }
    /// @}

    //MTK
    public void setSelectedState(long threadid) {
        if (sSelectedTheadsId != null) {
            sSelectedTheadsId.add(threadid);
        }
    }

    //MTK
    public static void removeSelectedState(long threadid) {
        if (sSelectedTheadsId != null) {
            sSelectedTheadsId.remove(threadid);
        }
    }

    //MTK
    public boolean isContainThreadId(long threadid) {
        if (sSelectedTheadsId != null) {
            return sSelectedTheadsId.contains(threadid);
        }
        return false;
    }

    //MTK
    public void clearstate() {
        if (sSelectedTheadsId != null) {
            sSelectedTheadsId.clear();
        }
    }

    //MTK
    public HashSet<Long> getSelectedThreadsList() {
            return sSelectedTheadsId;
    }

    //高通这个uncheckAll实际跟MTK的 mListAdapter.clearstate()目的一样:
    // MTK是先在ConversationList.java类里通过clearstate()清除sSelectedTheadsId，
    // 然后在ConversationListAdapter.java的bindView()方法里调用
    // conv.setIsChecked(sSelectedTheadsId.contains(conv.getThreadId()))
	//qcmms
    /*public void uncheckAll(Context context) {
        int count = getCount();
        for (int i = 0; i < count; i++) {
            Cursor cursor = (Cursor)getItem(i);
            if (cursor == null || cursor.getPosition() < 0) {
                continue;
            }
            Conversation conv = Conversation.from(context, cursor);
            conv.setIsChecked(false);
        }
    }*/
    
	//lichao add begin
    public class ConversationListViewHolder {
        ConversationListItem headerView;
        public ConversationListViewHolder(View view) {
            headerView = (ConversationListItem) view.findViewById(R.id.conv_list_item);
        }
    }
	
    private boolean mCheckBoxEnable = false;
	public void setCheckBoxEnable(boolean flag) {
        mCheckBoxEnable = flag;
    }
    public boolean getCheckBoxEnable() {
        return mCheckBoxEnable;
    }

    //lichao add for updateFromViewMaxWidth
    private int mScreenWidthDip = 0;
    public void setScreenWidthDip(int screenWidthDip) {
        mScreenWidthDip = screenWidthDip;
    }

    public void setBlackNumSet(HashSet<String> blackNumSet) {
        mBlackNumSet = blackNumSet;
    }
	//lichao add end
}
