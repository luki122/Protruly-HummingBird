/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.quicksearchbox.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.quicksearchbox.ClickLogCursor;
import com.android.quicksearchbox.ListSuggestionCursor;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.Suggestion;

/**
 * Created by lijun on 17-5-17.
 */
public class HistorySuggestionView extends RelativeLayout implements SuggestionView{

    private static final boolean DBG = true;

    private static final String VIEW_ID = "history";

    private final String TAG = "QSB.HistorySuggestionView";

    private ImageView mIcon;//show the search icon

    private TextView mTextView;//show the

    private ImageView mDeleteIcon;//to delete the this history

    private long mSuggestionId;
    private SuggestionsAdapter<?> mAdapter;

    public HistorySuggestionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public HistorySuggestionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HistorySuggestionView(Context context) {
        super(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mIcon = (ImageView) findViewById(R.id.icon);
        mTextView = (TextView) findViewById(R.id.history_text);
        mDeleteIcon = (ImageView) findViewById(R.id.delete_icon);
        mDeleteIcon.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (mAdapter != null) {
                    mAdapter.onHistorySuggestionDeleteClicked(mTextView.getText().toString());
                }
            }
        });
    }

    protected void onSuggestionClicked() {
        if (mAdapter != null) {
            mAdapter.onSuggestionClicked(mSuggestionId);
        }
    }

    @Override
    public void bindAsSuggestion(Suggestion suggestion, String userQuery) {
        mIcon.setImageResource(R.drawable.ic_search_clicklog);
        Suggestion hereSuggestion;
        if(suggestion instanceof ClickLogCursor){
            mTextView.setText(((ClickLogCursor)suggestion).getCurrentClickText());
        }else {
            mTextView.setText(suggestion.getSuggestionQuery());
        }
        mDeleteIcon.setImageResource(R.drawable.ic_delete_history);//lijun modify ic_delete_clicklog to ic_delete_history
        setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                onSuggestionClicked();
            }
        });
    }

    @Override
    public void bindAdapter(SuggestionsAdapter<?> adapter, long suggestionId) {
        mAdapter = adapter;
        mSuggestionId = suggestionId;
    }

    public static class Factory extends SuggestionViewInflater {
        public Factory(Context context) {
            super(VIEW_ID, HistorySuggestionView.class, R.layout.history_suggestion, context);
        }

        @Override
        public boolean canCreateView(Suggestion suggestion) {
            if(suggestion instanceof ListSuggestionCursor){
                return ((ListSuggestionCursor)suggestion).isHistory();
            }
            return suggestion.isHistorySuggestion();
        }
    }

}
