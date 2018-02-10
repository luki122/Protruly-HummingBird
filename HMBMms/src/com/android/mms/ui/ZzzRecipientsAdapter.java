/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.util.Rfc822Token;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.support.annotation.Nullable;

import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.android.mms.R;

/**
 * Adapter for showing a recipient list.
 */
public class ZzzRecipientsAdapter extends BaseAdapter implements Filterable {
    private static final String TAG = "BaseRecipientAdapter";

    private static final boolean DEBUG = false;

    private static final int DEFAULT_PREFERRED_MAX_RESULT_COUNT = 10;
    static final int ALLOWANCE_FOR_DUPLICATES = 5;

    // This is ContactsContract.PRIMARY_ACCOUNT_NAME. Available from ICS as hidden
    static final String PRIMARY_ACCOUNT_NAME = "name_for_primary_account";
    // This is ContactsContract.PRIMARY_ACCOUNT_TYPE. Available from ICS as hidden
    static final String PRIMARY_ACCOUNT_TYPE = "type_for_primary_account";

    private static final int MESSAGE_SEARCH_PENDING_DELAY = 1000;
    private static final int MESSAGE_SEARCH_PENDING = 1;

    /*private static class DefaultFilterResult {
        public String number;
        public String displayName;
        public String matchString;

        public DefaultFilterResult(String number, String displayName, String matchString) {
            super();
            this.number = number;
            this.displayName = displayName;
            this.matchString = matchString;
        }
    }*/
    public static final int NUMBER = 0;         // String
    public static final int NAME = 1;                // String
    public static final int CONTACT_ID = 2;          // long
    public static final int DATA_ID = 3;             // long
    public static final int LOOKUP_KEY = 4;          // String
    
    public static final int LOCATION = 0;
    public static final int TYPE = 1;
    public static final int CALLLOGNUMBER = 2;
    public static final int DATE = 3;
    public static final int CALLLOG_NAME = 4;

    ArrayList<MatchEntry> mMatchEntry = new ArrayList<>();

    //tmp not add email
    public class MatchEntry {
        public String number;
        public String displayName;
        public String matchString;
        public long contactId;
        public long dataId;
        public String lookupKey;
        boolean isShowDisplayName = true;
        
        public int type;
        public static final int CONTACT = 0;
        public static final int CALLLOG = 1;
        public String location;
        public int calllogType;
        public String calllogTypeStr;
        public String date;

        public MatchEntry(String number, String displayName, String matchString, long contactId, long dataId,
                          String lookupKey) {
            super();
            this.number = number;
            this.displayName = displayName;
            this.matchString = matchString;
            this.contactId = contactId;
            this.dataId = dataId;
            this.lookupKey = lookupKey;
            this.type = CONTACT;
            if(mCurrContactId == this.contactId) {
                isShowDisplayName = false;
            }
            mCurrContactId = this.contactId;
        }

        public MatchEntry(String number, String location, int calllogType, long timestamp) {
            this.number = number;
            this.displayName = number;
            this.location = location;
            this.calllogType = calllogType;
            this.calllogTypeStr = getCalllogTypeString(calllogType);
            this.date = MessageUtils.formatTimeStampStringForItem(mContext, timestamp);;
            this.type = CALLLOG;
        }

        public MatchEntry(Cursor cursor, int type) {
            if(type == CONTACT) {
                this.number = cursor.getString(NUMBER);
                this.displayName = cursor.getString(NAME);
                this.matchString = null;
                this.contactId = cursor.getLong(CONTACT_ID);
                this.dataId = cursor.getLong(DATA_ID);
                this.lookupKey = cursor.getString(LOOKUP_KEY);
                this.type = CONTACT;
                if(mCurrContactId == this.contactId) {
                    isShowDisplayName = false;
                }
                mCurrContactId = this.contactId;
            } else {
                this.type = CALLLOG;
                this.location = cursor.getString( LOCATION );
                this.number = cursor.getString( CALLLOGNUMBER );
                this.displayName = this.number;
                this.calllogType = cursor.getInt( TYPE );
                this.calllogTypeStr = getCalllogTypeString(this.calllogType);
                long timestamp = cursor.getLong( DATE );
                this.date = MessageUtils.formatTimeStampStringForItem(mContext, timestamp);
            }
        }

        public String getCalllogTypeString(int type) {
            String calllogName = null;
            if(calllogType == Calls.INCOMING_TYPE) {
                calllogName = mContext.getString(R.string.calllog_type_incoming);
            } else if (calllogType == Calls.OUTGOING_TYPE) {
                calllogName = mContext.getString(R.string.calllog_type_outgoing);
            } else if (calllogType == Calls.MISSED_TYPE) {
                calllogName = mContext.getString(R.string.callog_type_miss);
            }
            return calllogName;
        }
        /*public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public long getDataId() {
            return dataId;
        }*/
    }

    private long mCurrContactId;
    private final class DefaultFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (DEBUG) {
                Log.d(TAG, "start filtering. constraint: " + constraint + ", thread:"
                        + Thread.currentThread());
            }

            final FilterResults results = new FilterResults();
            Cursor cursor = null;

            if (TextUtils.isEmpty(constraint)) {
                return results;
            }
            String mCurrCalllogNumber = "";
            ArrayList<MatchEntry> matchEntry = new ArrayList<MatchEntry>();

            try {
                constraint = trimAllSpace(constraint);
                cursor = doQuery(constraint, mPreferredMaxResultCount);

                if (cursor == null) {
                    if (DEBUG) {
                        Log.w(TAG, "null cursor returned for default Email filter query.");
                    }
                } else {
                    int i = 0;
                    MatchEntry entry = null;
                    if(cursor.moveToFirst()) {
                        do {
                            if (i < mContactCursorCount) {
                                 entry = new MatchEntry(cursor, MatchEntry.CONTACT);
                            } else {
                                String numberTmp = cursor.getString( CALLLOGNUMBER );
                                String nameTmp = cursor.getString( CALLLOG_NAME );
                                if(!TextUtils.isEmpty( nameTmp ) || mCurrCalllogNumber.equals( numberTmp )) {
                                    continue;
                                }
                                 entry = new MatchEntry(cursor, MatchEntry.CALLLOG);
                                 mCurrCalllogNumber = numberTmp;
                            }
                            i ++;
                            matchEntry.add(entry);
                        } while(cursor.moveToNext());
                    }
                    //need it
                    results.values = matchEntry;
                    results.count = matchEntry.size();
                }
            } finally {
                mCurrContactId = 0;
                if (cursor != null) {
                    cursor.close();
                }
            }
            return results;
        }

        @Override
        protected void publishResults(final CharSequence constraint, FilterResults results) {
            /*if(constraint != null && !constraint.equals( mCurrentConstraint )) {
                mCurrentConstraint = trimAllSpace(constraint.toString());
                updateEntries();
            }*/
             //add synchronized for multi thread,will has “The content of the adapter has changed but ListView did not receive a notification” exception,becuase i hav call
            mMatchEntry = (ArrayList<MatchEntry>) results.values;
            mCurrentConstraint = trimAllSpace(constraint);//constraint;
            if(results != null && results.count > 0) {
                notifyDataSetChanged();
            }/* else {
                notifyDataSetInvalidated();
            }*/
            /*mCurrentConstraint = constraint;
            updateEntries();*/
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            final MatchEntry entry = (MatchEntry)resultValue;
            final String displayName = entry.displayName;
            /*final String emailAddress = entry.getDestination();
            if (TextUtils.isEmpty(displayName) || TextUtils.equals(displayName, emailAddress)) {
                 return emailAddress;
            } else {
                return new Rfc822Token(displayName, emailAddress, null).toString();
            }*/
            return displayName;
        }
    }


    private final Context mContext;
    private final ContentResolver mContentResolver;
    private Account mAccount;
    protected final int mPreferredMaxResultCount;

    private int mRemainingDirectoryCount;

    protected CharSequence mCurrentConstraint;

    public ZzzRecipientsAdapter(Context context) {
        this(context, DEFAULT_PREFERRED_MAX_RESULT_COUNT);
    }

    public ZzzRecipientsAdapter(Context context, int preferredMaxResultCount) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mPreferredMaxResultCount = preferredMaxResultCount;
    }

    public Context getContext() {
        return mContext;
    }

    public void setAccount(Account account) {
        mAccount = account;
    }

    @Override
    public Filter getFilter() {
        return new DefaultFilter();
    }

    protected void updateEntries() {
        notifyDataSetChanged();
    }

    protected List<MatchEntry> getEntries() {
        return mMatchEntry;
    }

    public static final Uri CONTACTS_CONTENT_FILTER_URI = Uri.withAppendedPath(
            Phone.CONTENT_URI, "hbfilter");
    //public static final Uri CONTENT_FILTER_URI = Phone.CONTENT_FILTER_URI;
    private String[] contacts_project = new String[] {
            Phone.NUMBER,                                   // 0
            Contacts.DISPLAY_NAME,                          // 1
            //Phone.TYPE,                                     // 2
            //Phone.LABEL,                                    // 3
            Phone.CONTACT_ID,                               // 4
            Phone._ID,                                      // 5
            Contacts.LOOKUP_KEY,                            // 6
            //ContactsContract.CommonDataKinds.Email.MIMETYPE // 7
    };

    public static final Uri CALLLOG_CONTENT_FILTER_URI = /*Uri.withAppendedPath(
            CallLog.AUTHORITY, "calls/search_filter");*/Uri.parse("content://" + CallLog.AUTHORITY +  "/calls/search_filter");
    //public static final Uri CONTENT_FILTER_URI = Phone.CONTENT_FILTER_URI;
    private String[] calllog_project = new String[] {
            Calls.GEOCODED_LOCATION,
            Calls.TYPE,
            Calls.NUMBER,
            Calls.DATE,
            Contacts.DISPLAY_NAME,
    };
    public int mContactCursorCount;
    private Cursor doQuery(CharSequence constraint, int limit) {
        if(isAllWhitespace(constraint.toString())) {
            mMatchEntry.clear();
            return null;
        }
        //contact query
        final Uri.Builder builder = CONTACTS_CONTENT_FILTER_URI.buildUpon()
                .appendPath(constraint.toString())
                .appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY,
                        String.valueOf(limit + ALLOWANCE_FOR_DUPLICATES));
        if (mAccount != null) {
            builder.appendQueryParameter(PRIMARY_ACCOUNT_NAME, mAccount.name);
            builder.appendQueryParameter(PRIMARY_ACCOUNT_TYPE, mAccount.type);
        }
        final long start = System.currentTimeMillis();
        final Cursor cursor = mContentResolver.query(
                builder.build(), contacts_project, null, null, Phone.CONTACT_ID +" ASC");
        final long end = System.currentTimeMillis();
        mContactCursorCount = cursor.getCount();
        //begin calllog query
        final Uri.Builder builder2 = CALLLOG_CONTENT_FILTER_URI.buildUpon()
                .appendPath(constraint.toString())
                .appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY,
                        String.valueOf(limit + ALLOWANCE_FOR_DUPLICATES));
        if (mAccount != null) {
            builder.appendQueryParameter(PRIMARY_ACCOUNT_NAME, mAccount.name);
            builder.appendQueryParameter(PRIMARY_ACCOUNT_TYPE, mAccount.type);
        }
        final long start2 = System.currentTimeMillis();
        final Cursor cursor2 = mContentResolver.query(
                builder2.build(), calllog_project, null, null, Calls.DEFAULT_SORT_ORDER);
        final long end2 = System.currentTimeMillis();
        Cursor[] cursors = {cursor, cursor2};
        MergeCursor mergeCursor = new MergeCursor(cursors);
        //end calllog query
        /*if (DEBUG) {
            Log.d(TAG, "Time for autocomplete (query: " + constraint
                    + ", num_of_results: "
                    + (cursor != null ? cursor.getCount() : "null") + "): "
                    + (end - start) + " ms");
        }*/
        return mergeCursor;
    }

    private CharSequence trimAllSpace(CharSequence src) {
        //String rtn = new String(src);
        if(TextUtils.isEmpty( src )) {
            return src;
        }
        return src.toString().replaceAll("\\s*", ""); 
    }

    @Override
    public int getCount() {
        final List<MatchEntry> entries = getEntries();
        return entries != null ? entries.size() : 0;
    }

    @Override
    public MatchEntry getItem(int position) {
        return getEntries().get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return 1;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //avoid IndexOutOfBoundsException
        if(position >= mMatchEntry.size()) {
            return null;
        }
        final MatchEntry entry = mMatchEntry.get(position);
        CharSequence[] styledResults =
                getStyledResults(mCurrentConstraint.toString(), entry.displayName, entry.number);
        CharSequence displayName = styledResults[0];
        CharSequence number = styledResults[1];
        final String constraint = mCurrentConstraint == null ? null :
                mCurrentConstraint.toString();
        ViewHolder holder = null;
        if(convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.zzz_recipients_editor_popupwindow, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }
        holder = (ViewHolder)convertView.getTag();
        if(entry.type == MatchEntry.CONTACT) {
            bindTextToView(displayName, holder.displayNameView);
            bindTextToView(number, holder.destinationView);
            MessageUtils.hideView( holder.dateView );
            if(entry.isShowDisplayName) {
                MessageUtils.showView( holder.titleParent );
                MessageUtils.showView( holder.displayNameView );
                MessageUtils.hideView( holder.topSpace );
                MessageUtils.hideView( holder.bottomSpace );
            } else {
                MessageUtils.hideView( holder.displayNameView );
                MessageUtils.hideView( holder.titleParent );
                MessageUtils.showView( holder.topSpace );
                MessageUtils.showView( holder.bottomSpace );
            }
        } else {
            MessageUtils.showView( holder.dateView );
            bindTextToView(displayName, holder.displayNameView);
            String calllogDestination = entry.calllogTypeStr + " " + entry.location;
            bindTextToView(calllogDestination, holder.destinationView);
            bindTextToView(entry.date, holder.dateView);
        }
        return convertView;
    }

    protected void bindTextToView(CharSequence text, TextView view) {
        if (view == null) {
            return;
        }

        if (text != null) {
            view.setText(text);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    protected CharSequence[] getStyledResults(@Nullable String constraint, String... results) {
        if (isAllWhitespace(constraint)) {
            return results;
        }

        CharSequence[] styledResults = new CharSequence[results.length];
        boolean foundMatch = false;
        for (int i = 0; i < results.length; i++) {
            String result = results[i];
            if (result == null) {
                continue;
            }

            //if (!foundMatch) {
                //int index = result.toLowerCase().indexOf(constraint.toLowerCase());
                int[] index = getIndexOf(result.toLowerCase(),constraint.toLowerCase());
                if (index[0] != -1) {
                    SpannableStringBuilder styled = SpannableStringBuilder.valueOf(result);
                    ForegroundColorSpan highlightSpan =
                            new ForegroundColorSpan(mContext.getResources().getColor(
                                    R.color.recipients_edit_hightlight));
                    styled.setSpan(highlightSpan,
                            index[0], index[1]+1 , Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    styledResults[i] = styled;
                    foundMatch = true;
                    continue;
                }
            //}
            styledResults[i] = result;
        }
        return styledResults;
    }

    private static int[] getIndexOf(String source, String target) {
        int sourceCount = source.length();
        int targetCount = target.length();
        int[] rtn = new int[2];
        if (targetCount == 0) {
            return rtn;
        }

        char first  = target.charAt( 0 );
        int max = sourceCount - targetCount;

        for (int i = 0; i <= max; i++) {
            /* Look for first character. */
            if (source.charAt(i) != first) {
                while (++i <= max && source.charAt( i ) != first);
            }

            /* Found first character, now look at the rest of v2 */
            int k =  1;
            if (i <= max) {
                int j = i + 1;
                //int end = j + targetCount - 1;
                for (; k < targetCount  && j < sourceCount ; ) {
                    if(source.charAt( j ) == ' ') {
                        j++;
                        continue;
                    }else if(source.charAt( j ) == target.charAt( k )) {
                        j++;
                        k++;
                        continue;
                    } else {
                        break;
                    }
                }
                if (k == targetCount) {
                    /* Found whole string. */
                    rtn[0] = i;
                    rtn[1] = j - 1;
                    return rtn;
                }
            }
        }
        rtn[0] = -1;
        return rtn;
    }

    private static boolean isAllWhitespace(@Nullable String string) {
        if (TextUtils.isEmpty(string)) {
            return true;
        }

        for (int i = 0; i < string.length(); ++i) {
            if (!Character.isWhitespace(string.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    protected class ViewHolder {
        public final TextView displayNameView;
        public final TextView destinationView;
        public View topSpace;
        public View bottomSpace;
        public TextView dateView;
        public View titleParent;

        public ViewHolder(View view) {
            displayNameView = (TextView) view.findViewById(R.id.recipients_dropdown_title);
            destinationView = (TextView) view.findViewById(R.id.recipients_dropdown_subtitle);
            topSpace = view.findViewById(R.id.top_space);
            bottomSpace = view.findViewById(R.id.bottom_space);
            dateView = (TextView) view.findViewById(R.id.recipients_dropdown_dateview);
            titleParent = view.findViewById(R.id.recipients_title_parent);
        }
    }
}