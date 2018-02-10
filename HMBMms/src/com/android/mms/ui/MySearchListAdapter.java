package com.android.mms.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.mms.R;
import com.android.mms.data.ContactList;
import com.zzz.provider.Telephony.Threads;
import com.zzz.provider.Telephony.ThreadsColumns;

import java.util.HashSet;

import static com.android.mms.ui.MySearchUtils.getRecipientsByThreadId;

//this class add by lichao for Mms Search
public class MySearchListAdapter extends CursorAdapter {

    private static final String TAG = "Mms/MySearchListAdapter";
    private static final boolean DEBUG = false;

    private String mSearchString = "";

    private static final int VIEW_TYPE_TOTAL_COUNT = 3;
    public static final int VIEW_TYPE_INVALID = -1;
    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_THREAD = 1;
    public static final int VIEW_TYPE_MESSAGE = 2;

    public static final String HIGH_LIGHT_TEXT = "highlight";
    public static final String HIGH_LIGHT_PLACE = "highlight_place";
    /*
    public static final int HIGH_LIGHT_PLACE_INVALID = -1;
    public static final int HIGH_LIGHT_PLACE_TITLE = 1;
    public static final int HIGH_LIGHT_PLACE_CONTENT = 2;
    */

    //private static int mPreDataType = VIEW_TYPE_INVALID;

    //private static final int POSITION_INVALID = -1;
    //int mFirstThreadPosition = POSITION_INVALID;
    //int mFirstMessagePosition = POSITION_INVALID;

    private HashSet<Integer> mShowedPosition;
    private HashSet<String> mShowedAddress;
    private HashSet<Integer> mShowedAddressPosition;

    public MySearchListAdapter(Context context, Cursor c) {
        super(context, c);
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_TOTAL_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = (Cursor)getItem(position);
        return getItemViewType(cursor);
    }

    private int getItemViewType(Cursor cursor) {
        int recipientIdsPos  = cursor.getColumnIndex(ThreadsColumns.RECIPIENT_IDS);
        //if(DEBUG) Log.d(TAG, "getItemViewType(), recipientIdsPos = " + recipientIdsPos);
        if(recipientIdsPos > 0){
            return VIEW_TYPE_THREAD;
        }else{
            return VIEW_TYPE_MESSAGE;
        }
    }

    public void changeCursor(Cursor cursor) {
        if(DEBUG) Log.d(TAG, "\n\n ----------------changeCursor() ----------------");
        //mPreDataType = VIEW_TYPE_INVALID;
        //mFirstThreadPosition = POSITION_INVALID;
        //mFirstMessagePosition = POSITION_INVALID;
        if(null == mShowedPosition){
            mShowedPosition = new HashSet<>();
        }else{
            mShowedPosition.clear();
        }
        if(null == mShowedAddress){
            mShowedAddress = new HashSet<>();
        }else{
            mShowedAddress.clear();
        }
        if(null == mShowedAddressPosition){
            mShowedAddressPosition = new HashSet<>();
        }else{
            mShowedAddressPosition.clear();
        }
        //Cursor old = swapCursor(cursor);
        swapCursor(cursor);
        //don't close here because mSearchCursorCache will use it after
        /*if (old != null) {
            old.close();
        }*/
    }

    //Recycling use
    public class ViewHolder {
        //lichao delete category in 2017-04-25
        //TextView category;

        ViewGroup msgGroup;
        ViewGroup msgTitleGroup;
        TextView msgName;
        TextView msgNumber;
        //ViewGroup msgClickGroup;
        MyTextViewSnippet msgBody;
        TextView msgDate;
        ImageView simicon;

        ViewGroup threadClickGroup;
        MyTextViewSnippet threadName;
        MyTextViewSnippet threadNumber;
        TextView threadCount;

        public ViewHolder(View view) {
            //category = (TextView)(view.findViewById(R.id.category_title));
            msgGroup = (ViewGroup)(view.findViewById(R.id.search_item_msg_layout));
            msgTitleGroup = (ViewGroup)(view.findViewById(R.id.search_item_msg_title_layout));
            msgName = (TextView)(view.findViewById(R.id.search_item_msg_name));
            msgNumber = (TextView)(view.findViewById(R.id.search_item_msg_number));
            //msgClickGroup = (ViewGroup)(view.findViewById(R.id.search_item_msg_layout));
            msgBody = (MyTextViewSnippet)(view.findViewById(R.id.search_item_msg_body));
            msgDate = (TextView)(view.findViewById(R.id.search_item_msg_date));
            simicon = (ImageView)(view.findViewById(R.id.search_item_msg_sim));

            threadClickGroup = (ViewGroup)(view.findViewById(R.id.search_item_thread_layout));
            threadName = (MyTextViewSnippet)(view.findViewById(R.id.search_item_thread_name));
            threadNumber = (MyTextViewSnippet)(view.findViewById(R.id.search_item_thread_number));
            threadCount = (TextView)(view.findViewById(R.id.search_item_thread_count));
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if(DEBUG) Log.d(TAG, "\n\n ----------------bindView() ----------------");

        int position = cursor.getPosition();
        if(DEBUG) Log.d(TAG, "bindView(), position = " + position);

        final int f_type = getItemViewType(position);
        if(DEBUG) Log.d(TAG, "bindView(), item f_type: " + f_type);

        ViewHolder viewHolder = (ViewHolder) view.getTag();
        if(null == viewHolder){
            if(DEBUG) Log.d(TAG, "bindView(), new ViewHolder");
            viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
        }

        long msgId = -1L;
        long threadId = -1L;
        String titleString = "";
        String titleName = "";
        String titleNumber = "";
        boolean nameIsNumeric = false;
        String bodyString = "";
        String dateString = "";
        //String categoryString = "";
        //String canonicalAddress = "";
        int subId = -1;
        boolean isNeedShowTitle = false;
        boolean mFirstBindThisPosition = true;
        if (!mShowedPosition.contains(position)) {
            mShowedPosition.add(position);
        } else {
            mFirstBindThisPosition = false;
        }
        if(f_type == VIEW_TYPE_THREAD){
            /*if(f_type != mPreDataType && mFirstThreadPosition == POSITION_INVALID){
                mFirstThreadPosition = position;
            }*/
            int column_id = cursor.getColumnIndex(Threads._ID);//"_id"
            if(column_id >= 0){
                threadId = cursor.getLong(column_id);
            }
            if (DEBUG) Log.d(TAG, "bindView(), VIEW_TYPE_THREAD, threadId = " + threadId);

            String recipient_ids = cursor.getString(
                    cursor.getColumnIndex(ThreadsColumns.RECIPIENT_IDS));//"recipient_ids"
            ContactList recipients = ContactList.getByIds(recipient_ids, false);
            if (DEBUG) Log.d(TAG, "bindView(), VIEW_TYPE_THREAD, recipients: " + recipients.serialize());

            //get name and number if saved
            titleString = MySearchUtils.getRecipientsStrByContactList(recipients);
            if(DEBUG) Log.d(TAG, "bindView(), VIEW_TYPE_THREAD, titleString = " + titleString);

            nameIsNumeric = MessageUtils.isNumeric(recipients.formatNamesAndNumbers(","));
            if(DEBUG) Log.d(TAG, "bindView(), VIEW_TYPE_THREAD, nameIsNumeric: " + nameIsNumeric);
            if(!nameIsNumeric){
                //only get name if saved
                titleName = MessageUtils.formatFrom(context, recipients).toString();
                //only get number if saved
                titleNumber = MySearchUtils.getRecipientsNumbersByContactList(recipients);
            }
            int msg_count = cursor.getInt(cursor.getColumnIndex(ThreadsColumns.MESSAGE_COUNT));//"message_count"
            if(msg_count == 0){
                bodyString = context.getString(R.string.zzz_has_draft);
            }else if(msg_count > 0){
                bodyString = context.getString(R.string.zzz_messages_count, msg_count);
            }

            long when = cursor.getLong(cursor.getColumnIndex(ThreadsColumns.DATE));//"date"
            dateString = MessageUtils.formatTimeStampStringForItem(context, when);

            /*int column_subId = cursor.getColumnIndex(Threads.SUBSCRIPTION_ID);//"sub_id"
            if(column_subId >= 0){
                subId = cursor.getInt(column_subId);
            }*/
        }
        else if(f_type == VIEW_TYPE_MESSAGE){
            /*if(f_type != mPreDataType && mFirstMessagePosition == POSITION_INVALID){
                mFirstMessagePosition = position;
            }*/
            msgId = cursor.getLong(cursor.getColumnIndex("_id"));
            if(DEBUG) Log.d(TAG, "bindView(), msgId = " + msgId);
            threadId = cursor.getLong(cursor.getColumnIndex("thread_id"));
            String address = cursor.getString(cursor.getColumnIndex("address"));
            Log.d(TAG, "bindView(), address = " + address);
            Log.d(TAG, "bindView(), mFirstBindThisPosition = " + mFirstBindThisPosition);
            if (mFirstBindThisPosition) {
                if (!mShowedAddress.contains(address)) {
                    mShowedAddress.add(address);
                    isNeedShowTitle = true;
                    mShowedAddressPosition.add(position);
                }
            } else {
                if(mShowedAddressPosition.contains(position)){
                    isNeedShowTitle = true;
                }
            }
            Log.d(TAG, "bindView(), isNeedShowTitle = " + isNeedShowTitle);
            if(isNeedShowTitle){
                ContactList recipients = getRecipientsByThreadId(context, threadId);
                titleString = MySearchUtils.getRecipientsStrByThreadId(context, threadId);
                if (!TextUtils.isEmpty(titleString)) {
                    nameIsNumeric = MessageUtils.isNumeric(titleString);
                    if(!nameIsNumeric){
                        titleName = MessageUtils.formatFrom(context, recipients).toString();
                        titleNumber = MySearchUtils.getRecipientsNumbersByContactList(recipients);
                    }
                }else{
                    titleString = MySearchUtils.getNameAndNumberByAddress(address);
                    nameIsNumeric = MessageUtils.isNumeric(titleString);
                    if(!nameIsNumeric){
                        titleName = MySearchUtils.getNameByAddress(address);
                        titleNumber = MySearchUtils.getNumberByAddress(address);
                    }
                }
            }
            bodyString = cursor.getString(cursor.getColumnIndex("body"));
            long when = cursor.getLong(cursor.getColumnIndex("date"));
            dateString = MessageUtils.formatTimeStampStringForItem(context, when);
            int column_subId = cursor.getColumnIndex(Threads.SUBSCRIPTION_ID);//"sub_id"
            if(column_subId >= 0){
                subId = cursor.getInt(column_subId);
            }
        }
        //set mPreDataType value after used mPreDataType
        //mPreDataType = f_type;

        /*
        //lichao delete category in 2017-04-25
        //only do not show category view, keep the ItemViewType
        if (position == mFirstThreadPosition) {
            //show category title
            viewHolder.category.setVisibility(View.VISIBLE);
            viewHolder.category.setText(context.getString(R.string.recipient_category));
        } else if (position == mFirstMessagePosition) {
            //show category title
            viewHolder.category.setVisibility(View.VISIBLE);
            viewHolder.category.setText(context.getString(R.string.message_category));
        } else {
            //hide category title
            viewHolder.category.setVisibility(View.GONE);
        }
        */

        final Context f_context = context;
        final long f_threadId = threadId;
        final long f_msgId = msgId;

        if(f_type == VIEW_TYPE_THREAD){
            viewHolder.threadClickGroup.setVisibility(View.VISIBLE);
            viewHolder.msgGroup.setVisibility(View.GONE);
            if(!nameIsNumeric){
                if(DEBUG) Log.d(TAG, "bindView(), VIEW_TYPE_THREAD, threadName.set: " + titleName);
                if(DEBUG) Log.d(TAG, "bindView(), VIEW_TYPE_THREAD, threadNumber.set: " + titleNumber);
                if(titleName.contains(mSearchString)){
                    viewHolder.threadName.setText(titleName, mSearchString);
                    viewHolder.threadNumber.setText(titleNumber, "");
                }
                if(titleNumber.contains(mSearchString)){
                    viewHolder.threadName.setText(titleName, "");
                    viewHolder.threadNumber.setText(titleNumber, mSearchString);
                }
            }else{
                if(DEBUG) Log.d(TAG, "bindView(), VIEW_TYPE_THREAD, not Numeric, threadName.set: " + titleString);
                if(DEBUG) Log.d(TAG, "bindView(), VIEW_TYPE_THREAD, not Numeric, threadNumber.set empty" );
                viewHolder.threadName.setText(titleString, mSearchString);
                viewHolder.threadNumber.setText("", "");
            }
            viewHolder.threadCount.setText(bodyString);
            viewHolder.threadClickGroup.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    final Intent onClickIntent = new Intent(f_context, ComposeMessageActivity.class);
                    onClickIntent.putExtra(ComposeMessageActivity.THREAD_ID, f_threadId);
                    onClickIntent.putExtra(HIGH_LIGHT_PLACE, f_type);
                    onClickIntent.putExtra(HIGH_LIGHT_TEXT, mSearchString);
                    onClickIntent.putExtra(ComposeMessageActivity.SELECT_ID, f_msgId);
                    f_context.startActivity(onClickIntent);
                }
            });
        } else if(f_type == VIEW_TYPE_MESSAGE){
            viewHolder.threadClickGroup.setVisibility(View.GONE);
            viewHolder.msgGroup.setVisibility(View.VISIBLE);
            if(isNeedShowTitle){
                viewHolder.msgTitleGroup.setVisibility(View.VISIBLE);
                if(!nameIsNumeric){
                    if(DEBUG) Log.d(TAG, "bindView(), VIEW_TYPE_MESSAGE, not Numeric, msgName.set: " + titleName);
                    viewHolder.msgName.setText(titleName);
                    if(DEBUG) Log.d(TAG, "bindView(), VIEW_TYPE_MESSAGE, not Numeric, msgNumber.set: " + titleNumber);
                    viewHolder.msgNumber.setText(titleNumber);
                }else{
                    if(DEBUG) Log.d(TAG, "bindView(), VIEW_TYPE_MESSAGE, is Numeric, msgName.set: " + titleString);
                    viewHolder.msgName.setText(titleString);
                    viewHolder.msgNumber.setText("");
                }
            }else {
                viewHolder.msgTitleGroup.setVisibility(View.GONE);
            }
            viewHolder.msgBody.setText(bodyString, mSearchString);
            viewHolder.msgDate.setText(dateString);
            int slotId = -1;
            if (subId >= 0) {
                slotId = SubscriptionManager.getSlotId(subId);
            }
            if(DEBUG) Log.d(TAG, "bindView(), slotId = "+slotId);
            boolean isIccCardEnabled = MessageUtils.isIccCardEnabled(slotId);
            boolean isTwoSimCardEnabled = MessageUtils.isTwoSimCardEnabled();
            //if isIccCardEnabled, no need to judge slotId >= 0
            boolean isShowSimIcon = isIccCardEnabled && isTwoSimCardEnabled;
            if (isShowSimIcon) {
                viewHolder.simicon.setVisibility(View.VISIBLE);
                Drawable mSimIndicatorIcon = MessageUtils.getMultiSimIcon(f_context, slotId);
                viewHolder.simicon.setImageDrawable(mSimIndicatorIcon);
            } else {
                viewHolder.simicon.setVisibility(View.GONE);
            }
            viewHolder.msgGroup.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    final Intent onClickIntent = new Intent(f_context, ComposeMessageActivity.class);
                    onClickIntent.putExtra(ComposeMessageActivity.THREAD_ID, f_threadId);
                    onClickIntent.putExtra(HIGH_LIGHT_PLACE, f_type);
                    onClickIntent.putExtra(HIGH_LIGHT_TEXT, mSearchString);
                    onClickIntent.putExtra(ComposeMessageActivity.SELECT_ID, f_msgId);
                    f_context.startActivity(onClickIntent);
                }
            });
        }


    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        if(DEBUG) Log.d(TAG, "\n\n ----------------newView() ----------------");
        //int itemType = getItemViewType(cursor);
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.zzz_search_item, parent, false);
        //View v = inflater.inflate(R.layout.zzz_search_item2, parent, false);
        return v;
    }

    public String getSearchString() {
        return mSearchString;
    }

    public void setSearchString(String searchString) {
        if(null == searchString){
            mSearchString = "";
        }
        mSearchString = searchString;
    }

    public interface OnContentChangedListener {
        void onContentChanged(MySearchListAdapter adapter);
    }

    private OnContentChangedListener mOnContentChangedListener;
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

}//end of MySearchListAdapter
