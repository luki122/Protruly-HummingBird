/*
 * Copyright (C) 2015 The Android Open Source Project
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

import java.lang.reflect.Field;

import com.android.mms.ui.ZzzRecipientsAdapter.MatchEntry;
import com.android.mms.util.MmsLog;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;
import com.android.mms.R;


/**
 * hummingbird add by tangyisen for  hb style 2017.3.28
 * */
public class ZzzRecipientsAutoCompleteEditor extends AutoCompleteTextView implements OnItemClickListener{
    private ZzzRecipientsFlowLayout mFlowLayout;
    private Field mPopupRef;
    private ListPopupWindow mListPopupRef;
    private static int CHANGE_SCROLL_LISTENER_MIN_CURSOR_COUNT = 10000;//tangyisen tmp modify 100 to 10000 to fix bug944;
    private MyScrollListener mScrollListener = new MyScrollListener(
            CHANGE_SCROLL_LISTENER_MIN_CURSOR_COUNT, "MessageList_Scroll_Tread");

    public ZzzRecipientsAutoCompleteEditor(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        try {
            mPopupRef = AutoCompleteTextView.class.getDeclaredField("mPopup");
            mPopupRef.setAccessible(true);
            mListPopupRef = (ListPopupWindow)mPopupRef.get(this);
            if(mListPopupRef != null) {
                //click space in popupwindow will miss others ui
                mListPopupRef.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        // TODO Auto-generated method stub
                        //super.dismissDropDown();
                        if(mOnDropDownListener != null) {
                            mOnDropDownListener.onDropDownDismiss();
                        }
                    }
                });
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        setOnItemClickListener(this);
        mScrollListener.setHideSoftKeyboardRunnable(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                hideKeyboard();
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }

    @Override
    protected void replaceText(CharSequence text) {
        //super.replaceText(text);
        setText(null);//clear text
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO Auto-generated method stub
        final MatchEntry mRecipientEntry = (MatchEntry) getAdapter().getItem(position);
        if(TextUtils.isEmpty(mRecipientEntry.number)) {
            Toast.makeText(mContext, R.string.toast_contact_no_number, Toast.LENGTH_SHORT).show();
            return;
        }
        if(mFlowLayout != null) {
            mFlowLayout.addFlowChild( mRecipientEntry.displayName, mRecipientEntry.number );
        }
    }

    public void setFlowLayout(ZzzRecipientsFlowLayout layout) {
        mFlowLayout = layout;
    }

    private OnDropDownListener mOnDropDownListener;
    public interface OnDropDownListener {
        void onDropDownShow();
        void onDropDownDismiss();
    }
    public void setOnDropDownListener(OnDropDownListener listener) {
        mOnDropDownListener = listener;
    }
    @Override
    public void showDropDown() {
        super.showDropDown();
        if(mOnDropDownListener != null) {
            mOnDropDownListener.onDropDownShow();
        }
        //the listview will builder in show()
        ListView listView = mListPopupRef.getListView();
        if(listView != null) {
            listView.setOnScrollListener(mScrollListener);
        }
    }

    @Override
    public void dismissDropDown() {
        super.dismissDropDown();
        if(mOnDropDownListener != null) {
            mOnDropDownListener.onDropDownDismiss();
        }
    }
}