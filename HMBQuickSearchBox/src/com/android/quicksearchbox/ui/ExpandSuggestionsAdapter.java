/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.quicksearchbox.ClickLogCursor;
import com.android.quicksearchbox.CorpusResult;
import com.android.quicksearchbox.ListSuggestionCursor;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.Source;
import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.SuggestionPosition;
import com.android.quicksearchbox.SuggestionUtils;
import com.android.quicksearchbox.Suggestions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

/**
 * Adapter for suggestions list where suggestions are clustered by corpus.
 */
public class ExpandSuggestionsAdapter extends SuggestionsAdapterBase<ExpandableListAdapter> {

    private static final String TAG = "QSB.ClusteredSuggestionsAdapter";

    private final static int GROUP_SHIFT = 32;
    private final static long CHILD_MASK = 0xffffffff;
    private final static int DEFAULT_EXPAND_COUNT =3;

    private final Adapter mAdapter;
    private final Context mContext;
    private final LayoutInflater mInflater;

    public ExpandSuggestionsAdapter(SuggestionViewFactory viewFactory, Context context) {
        super(viewFactory);
        mAdapter = new Adapter();
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public boolean isEmpty() {
        return mAdapter.getGroupCount() == 0;
    }

    @Override
    public boolean willPublishNonPromotedSuggestions() {
        return true;
    }

    @Override
    public SuggestionPosition getSuggestion(long suggestionId) {
        return mAdapter.getChildById(suggestionId);
    }

    @Override
    public ExpandableListAdapter getListAdapter() {
        return mAdapter;
    }

    @Override
    protected void notifyDataSetChanged() {
        mAdapter.buildCorpusGroups();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void notifyDataSetInvalidated() {
        mAdapter.buildCorpusGroups();
        mAdapter.notifyDataSetInvalidated();
    }

    protected  void notifyExpand(int position, boolean isExpanded) {
        mAdapter.buildCorpusGroups(position,isExpanded);
        mAdapter.notifyDataSetChanged();
    }
    public  boolean isFirstExpand(){
        return mAdapter.isFirstExpand();
    }
    private class Adapter extends BaseExpandableListAdapter {

        private ArrayList<SuggestionCursor> mCorpusGroups;

        public void buildCorpusGroups() {
            Suggestions suggestions = getSuggestions();
            SuggestionCursor promoted = getCurrentPromotedSuggestions();
            HashSet<String> promotedSuggestions = new HashSet<String>();
            if (promoted != null && promoted.getCount() > 0) {
                promoted.moveTo(0);
                if(!suggestions.isHistory()) {//lijun add if
                    do {
                        promotedSuggestions.add(SuggestionUtils.getSuggestionKey(promoted));
                    } while (promoted.moveToNext());
                }
            }
            if (suggestions == null) {
                mCorpusGroups = null;
            } else {
                if (mCorpusGroups == null) {
                    mCorpusGroups = new ArrayList<SuggestionCursor>();
                } else {
                    mCorpusGroups.clear();
                }

                //lijun add start
                if (suggestions.getClickLogCursor() != null && "".equals(suggestions.getQuery())) {
                    ListSuggestionCursor corpusSuggestions = new ListSuggestionCursor("");
                    ClickLogCursor clickLogCursor = suggestions.getClickLogCursor();
                    mCorpusGroups.add(clickLogCursor);
                }else {
                //lijun add end
                    for (CorpusResult result : suggestions.getCorpusResults()) {
                        ListSuggestionCursor corpusSuggestions = new ListSuggestionCursor(
                                result.getUserQuery());
                        for (int i = 0; i < result.getCount(); ++i) {
                            result.moveTo(i);
                            if (!result.isWebSearchSuggestion()) {
                                if (promotedSuggestions.contains(//M : liuzuo why add the !   ?
                                        SuggestionUtils.getSuggestionKey(result))) {
                                    if (!(corpusSuggestions.getCount() >= DEFAULT_EXPAND_COUNT)) {
                                        corpusSuggestions.add(new SuggestionPosition(result, i));
                                    } else {
                                        corpusSuggestions.setFullData(false);
                                        break;
                                    }
                                }
                            }
                        }
                        if (corpusSuggestions.getCount() > 0) {
                            corpusSuggestions.setFirstExpand(true);
                            mCorpusGroups.add(corpusSuggestions);
                        }
                    }
                }
                if(promoted!=null)
                if(mCorpusGroups.size()==0){
                    mCorpusGroups.add(promoted);
                }
                sortGroups(mCorpusGroups);//lijun add
            }
        }

        /**
         * lijun add to sort groups
         * @param groups
         */
        private void sortGroups(ArrayList<SuggestionCursor>  groups) {
            SuggestionCursor apps = null;
            SuggestionCursor contacts = null;
//            SuggestionCursor files = null;
            for (SuggestionCursor suggestionCursor : groups) {
//                if(suggestionCursor.getSuggestionSource() != null) {
//                    Log.d("ExpandSuggestions", "sort item : " + suggestionCursor.getSuggestionSource().getName());
//                }
                if (suggestionCursor.getSuggestionSource() != null && suggestionCursor.getSuggestionSource() != null && "com.android.providers.applications/.ApplicationLauncher".equals(suggestionCursor.getSuggestionSource().getName())) {
                    apps = suggestionCursor;
                } else if (suggestionCursor.getSuggestionSource() != null && suggestionCursor.getSuggestionSource() != null && "com.android.contacts/.activities.PeopleActivity".equals(suggestionCursor.getSuggestionSource().getName())) {
                    contacts = suggestionCursor;
                }/*else if(suggestionCursor.getSuggestionSource()!=null && "cn.com.protruly.filemanager/.CategoryActivity".equals(suggestionCursor.getSuggestionSource().getName())){
                    files = suggestionCursor;
                }*/
            }
            if (contacts != null) {
//                Log.d("ExpandSuggestions","sort contacts : " + contacts.getSuggestionSource().getName());
                groups.remove(contacts);
                groups.add(0, contacts);
            }
            if (apps != null) {
//                Log.d("ExpandSuggestions","sort apps : " + apps.getSuggestionSource().getName());
                groups.remove(apps);
                groups.add(0, apps);
            }
        }
        public void buildCorpusGroups(int position,boolean isExpanded) {
            Suggestions suggestions = getSuggestions();
            SuggestionCursor promoted = getCurrentPromotedSuggestions();
            HashSet<String> promotedSuggestions = new HashSet<String>();
            if (promoted != null && promoted.getCount() > 0) {
                promoted.moveTo(0);
                do {
                    promotedSuggestions.add(SuggestionUtils.getSuggestionKey(promoted));
                } while (promoted.moveToNext());
            }
            if (suggestions == null) {
                mCorpusGroups = null;
            } else {
                if (mCorpusGroups == null) {
                    mCorpusGroups = new ArrayList<SuggestionCursor>();
                } else {
                    //mCorpusGroups.clear();
                }
                ListSuggestionCursor suggestionCursor = (ListSuggestionCursor) getGroup(position);
                if(suggestionCursor == null)return;//fixed the NullPointException
                suggestionCursor.setFirstExpand(false);
                CharSequence label =  suggestionCursor.getSuggestionSource().getLabel();
                removeAllRows(suggestionCursor);
                int count = 0;
                for (CorpusResult result : suggestions.getCorpusResults()) {
                    for (int i = 0; i < result.getCount(); ++i) {
                        result.moveTo(i);
                        if (!result.isWebSearchSuggestion()&&!result.isHistorySuggestion()) {
                            if (promotedSuggestions.contains(//M : liuzuo why add the !   ?
                                    SuggestionUtils.getSuggestionKey(result))&&result.getSuggestionSource()!=null&&/*position-1==count*/label.equals(result.getSuggestionSource().getLabel())) {
                                if(isExpanded&&!(result.getCount()>=DEFAULT_EXPAND_COUNT)){

                                }else{
                                    suggestionCursor.add(new SuggestionPosition(result, i));
                                }
                            }
                        }
                    }
                    if(result.getCount()>0){
                        count ++;
                    }
                }
                if(suggestionCursor.getCount()>0){
                    suggestionCursor.setFullData(true);
                }
            }
        }
        public boolean isFirstExpand(){
            for(SuggestionCursor cursor :mCorpusGroups){
                if(cursor instanceof  ListSuggestionCursor&& !((ListSuggestionCursor) cursor).isFirstExpand()){
                   return false ;
                }
            }
            return true ;
        }
        private void removeAllRows(ListSuggestionCursor cursor){
            cursor.close();
        }
        @Override
        public long getCombinedChildId(long groupId, long childId) {
            // add one to the child ID to ensure that the group elements do not have the same ID
            // as the first child within the group.
            return (groupId << GROUP_SHIFT) | ((childId + 1) & CHILD_MASK);
        }

        @Override
        public long getCombinedGroupId(long groupId) {
            return groupId << GROUP_SHIFT;
        }

        public int getChildPosition(long childId) {
            return (int) (childId & CHILD_MASK) - 1;
        }

        public int getGroupPosition(long childId) {
            return (int) ((childId >> GROUP_SHIFT) & CHILD_MASK);
        }

        @Override
        public Suggestion getChild(int groupPosition, int childPosition) {
            SuggestionCursor c = getGroup(groupPosition);
            if (c != null) {
                c.moveTo(childPosition);
                return new SuggestionPosition(c, childPosition);
            }
            return null;
        }

        public SuggestionPosition getChildById(long childId) {
            SuggestionCursor groupCursor = getGroup(getGroupPosition(childId));
            if (groupCursor != null) {
                SuggestionPosition suggestionPosition = new SuggestionPosition(groupCursor, getChildPosition(childId));
                if(groupCursor instanceof ClickLogCursor){

                }
                return new SuggestionPosition(groupCursor, getChildPosition(childId));
            } else {
                Log.w(TAG, "Invalid childId " + Long.toHexString(childId) + " (invalid group)");
                return null;
            }
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
            SuggestionCursor cursor = getGroup(groupPosition);
            if (cursor == null) return null;
            View view = getView(cursor, childPosition, getCombinedChildId(groupPosition, childPosition),
                    convertView, parent);
            view.setBackgroundResource(R.drawable.expand_list_background_line);
            return view;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            SuggestionCursor group = getGroup(groupPosition);

            if(group instanceof  ListSuggestionCursor){
                ListSuggestionCursor listSuggestionCursor = (ListSuggestionCursor) group;
                if(!listSuggestionCursor.isFullData()&&listSuggestionCursor.getCount()>=DEFAULT_EXPAND_COUNT){
                  return   DEFAULT_EXPAND_COUNT;
                }
            }
            return group == null ? 0 : group.getCount();
        }

        @Override
        public SuggestionCursor getGroup(int groupPosition) {
            if (groupPosition < promotedGroupCount()) {
                return getCurrentPromotedSuggestions();
            } else {
                int pos = groupPosition - promotedGroupCount();
                if ((pos < 0) || (pos >= mCorpusGroups.size())) return null;
                return mCorpusGroups.get(pos);
            }
        }

        private int promotedCount() {
            SuggestionCursor promoted = getCurrentPromotedSuggestions();
            return (promoted == null ? 0 : promoted.getCount());
        }

        private int promotedGroupCount() {
            return (promotedCount() == 0) ? 0 : 1;
        }

        private int corpusGroupCount() {
            return mCorpusGroups == null ? 0 : mCorpusGroups.size();
        }

        @Override
        public int getGroupCount() {
            return promotedGroupCount() + corpusGroupCount();
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public View getGroupView(
                int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
 /*           if(groupPosition==0){
                convertView = mInflater.inflate(R.layout.suggestion_group, parent, false);
            } else*/ if (convertView == null) {
                convertView = mInflater.inflate(R.layout.suggestion_group, parent, false);
            }
            TextView lable = (TextView) convertView.findViewById(R.id.suggestions_group_text);
            TextView expandView = (TextView) convertView.findViewById(R.id.suggestions_group_expand);
            if (groupPosition == 0 || getGroup(groupPosition)==null) {//lijun add || getGroup(groupPosition)==null
                // don't show the group separator for the first group, to avoid seeing an empty
                // gap at the top of the list.
                lable.setVisibility(View.GONE);
                expandView.setVisibility(View.GONE);
                convertView.getLayoutParams().height = 0;
            } else {
                    convertView.getLayoutParams().height = mContext.getResources().
                        getDimensionPixelSize(R.dimen.suggestion_group_spacing);
                  addLable(convertView,groupPosition,isExpanded,parent);
            }
            // since we've fiddled with the layout params:
            SuggestionCursor suggestionCursor = getGroup(groupPosition);

            if (groupPosition == 0 || getGroup(groupPosition)==null) {
                convertView.getLayoutParams().height = 0;
                lable.getLayoutParams().height = 0;
                expandView.getLayoutParams().height = 0;
            }else if(suggestionCursor instanceof ListSuggestionCursor && ((ListSuggestionCursor) suggestionCursor).isHistory()){
                convertView.getLayoutParams().height = mContext.getResources().getDimensionPixelSize(R.dimen.hotsearch_hearder_height);
                lable.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                expandView.getLayoutParams().height = 0;
                convertView.setBackground(null);
//                convertView.requestLayout();
            }else if(groupPosition!=0 && getGroup(groupPosition)!=null){
                convertView.getLayoutParams().height = mContext.getResources().getDimensionPixelSize(R.dimen.suggestion_group_spacing);
                lable.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                expandView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                convertView.setBackgroundDrawable(new ColorDrawable(0xfff4f4f4));
//                convertView.requestLayout();
            }
            convertView.requestLayout();
            return convertView;
        }

        private void addLable(final View view, final int groupPosition, final boolean isExpanded, final ViewGroup parent) {
            TextView lable = (TextView) view.findViewById(R.id.suggestions_group_text);
            TextView expandView = (TextView) view.findViewById(R.id.suggestions_group_expand);
            lable.setVisibility(View.VISIBLE);
            final boolean finalExpand = isFullExpand(getGroup(groupPosition))&&isExpanded;
            if(lable!=null){
                SuggestionCursor suggestionCursor = getGroup(groupPosition);
                //lijun modify start
                Source suggestionSource;
                if(suggestionCursor instanceof ClickLogCursor){
                    suggestionSource = null;
                }else {
                    suggestionSource = getGroup(groupPosition).getSuggestionSource();
                }
                //lijun modify end
                if(suggestionSource!=null){
                    expandView.setVisibility(View.VISIBLE);
                    if(finalExpand){
                        //expandView.setText(R.string.tap_shrink);
                        expandView.setText(null);
                    }else {
                         expandView.setText(R.string.tap_expand);
                    }
                    lable.setText(suggestionSource.getLabel());
                }else {
                    expandView.setVisibility(View.GONE);
                    lable.setText(R.string.tap_history);

                    showHistoryLable(lable);//lijun add
                }

                expandView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(parent instanceof SuggestionsView) {
                            SuggestionsView suggestionsView = (SuggestionsView) parent;
                            if (finalExpand ) {
                                //suggestionsView.collapseGroup(groupPosition);
                                //expandInGroupPosition(groupPosition, isExpanded);
                                //suggestionsView.expandGroup(groupPosition);
                            } else {
                                expandInGroupPosition(groupPosition, isExpanded);
                                suggestionsView.expandGroup(groupPosition,true);
                            }
                        }
                    }
                });
            }
        }

        public boolean isFullExpand(SuggestionCursor group) {
               if(group instanceof  ListSuggestionCursor){
                   ListSuggestionCursor suggestionCursor = (ListSuggestionCursor) group;
                   return suggestionCursor.isFullData();
               } else {
                   return true;
               }
        }

        private void expandInGroupPosition(int groupPosition,boolean isExpanded) {
            notifyExpand(groupPosition,isExpanded);
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        @Override
        public int getChildType(int groupPosition, int childPosition) {
            return getSuggestionViewType(getGroup(groupPosition), childPosition);
        }

        @Override
        public int getChildTypeCount() {
            return getSuggestionViewTypeCount();
        }
    }

    //lijun add
    private void showHistoryLable(TextView lable) {
        Resources resources = mContext.getResources();
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lable.setLayoutParams(layoutParams);
        int padding = resources.getDimensionPixelSize(R.dimen.hotsearch_item_padding);
        lable.setPadding(padding, 0, padding, 0);
        lable.setBackgroundResource(R.drawable.expand_list_background_line);
        lable.setTextColor(resources.getColor(R.color.hotsearch_title_color));
        lable.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.hotsearch_hearder_text_size));
    }

}
