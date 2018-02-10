package com.android.deskclock.alarms;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.android.deskclock.R;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.widget.TextTime;

import java.util.HashMap;
import java.util.HashSet;

import hb.app.HbActivity;
import hb.widget.HbListView;


/**
 * Created by yubai on 17-4-24.
 */

public class ChooseRingtoneActivity extends HbActivity implements Runnable {

    private static final String SAVE_CLICKED_POS = "clicked_pos";

    private static final int POS_UNKNOWN = -1;

    /**
     * Keep the currently playing ringtone around when changing orientation, so that it
     * can be stopped later, after the activity is recreated.
     */
    private static Ringtone sPlayingRingtone;

    /** M: The ringtone type to show and add in the list. */
    private int mType = -1;

    /** The position in the list of the last clicked item. */
    private int mClickedPos = POS_UNKNOWN;

    /** The position in the list of the 'Silent' item. */
    private int mSilentPos = POS_UNKNOWN;

    /** The Uri to place a checkmark next to. */
    private Uri mExistingUri;

    /** The position in the list of the ringtone to sample. */
    private int mSampleRingtonePos = POS_UNKNOWN;

    /** Whether this list has the 'Silent' item. */
    private boolean mHasSilentItem;

    /** The number of static items in the list. */
    private int mStaticItemCount;

    /**
     * A Ringtone for the default ringtone. In most cases, the RingtoneManager
     * will stop the previous ringtone. However, the RingtoneManager doesn't
     * manage the default ringtone for us, so we should stop this one manually.
     */
    private Ringtone mDefaultRingtone;

    /**
     * The ringtone that's currently playing, unless the currently playing one is the default
     * ringtone.
     */
    private Ringtone mCurrentRingtone;


    private HbListView mListView;
    private SimpleCursorAdapter mAdapter;
//    private RingtonePickerAdapter mAdapter;
    private Cursor mCursor;
    private RingtoneManager mRingtoneManager;

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mClickedPos = savedInstanceState.getInt(SAVE_CLICKED_POS, POS_UNKNOWN);
        }
        mHandler = new Handler();
        Intent intent = getIntent();

        setHbContentView(R.layout.ringtone_choose);
        getToolbar().setTitle(R.string.alarm_ringtone);
        getToolbar().setNavigationIcon(getDrawable(R.drawable.clock_back));
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        // Get whether to show the 'Silent' item
        mHasSilentItem = intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        mRingtoneManager = new RingtoneManager(this);
        // Get whether to include DRM ringtones
        final boolean includeDrm = intent.getBooleanExtra(
                RingtoneManager.EXTRA_RINGTONE_INCLUDE_DRM, true);
        mRingtoneManager.setIncludeDrm(includeDrm);

        // Get the types of ringtones to show
        mType = intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, -1);
        if (mType != -1) {
            mRingtoneManager.setType(mType);
        }

        mCursor = mRingtoneManager.getCursor();

        // The volume keys will control the stream that we are choosing a ringtone for
        setVolumeControlStream(mRingtoneManager.inferStreamType());

        // Get the URI whose list item should have a checkmark
        mExistingUri = intent
                .getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI);
        mListView = (HbListView) findViewById(R.id.ringtone_list);
        mAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_single_choice,
                mCursor, new String[]{MediaStore.Audio.Media.TITLE}, new int[]{android.R.id.text1});
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setAdapter(mAdapter);
        prepareListView(mListView);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Save the position of most recently clicked item
                mClickedPos = position;
                // Play clip
                playRingtone(position, 0);
                /// M: save the uri of current position
                mExistingUri = mRingtoneManager.getRingtoneUri(getRingtoneManagerPosition(position));
            }
        });
    }

    /// M: Add to refresh activity because some new ringtones will insert to listview.
    @Override
    protected void onResume() {
        super.onResume();
//        /// When activity first start, just return. Only when restart we need to refresh in resume.
//        if (!mNeedRefreshOnResume) {
//            return;
//        }
//        /// Refresh the checked position after activity resume,
//        /// because maybe there are new ringtone insert to listview.
//        ListAdapter adapter = listView.getAdapter();
//        ListAdapter headAdapter = adapter;
//        if (null != headAdapter && (headAdapter instanceof HeaderViewListAdapter)) {
//            /// Get the cursor adapter with the listview
//            adapter = ((HeaderViewListAdapter) headAdapter).getWrappedAdapter();
//            mCursor = mRingtoneManager.getNewCursor();
//            ((SimpleCursorAdapter) adapter).changeCursor(mCursor);
//        } else {
//        }
//        /// Get position from ringtone list with this uri, if the return position is
//        /// valid value, set it to be current clicked position
//        if ((mClickedPos >= mStaticItemCount || (mHasSilentItem && mClickedPos == 1))
//                && (null != mExistingUri)) {
//            /// M: TODO avoid cursor out of bound, so move cursor position.
//            if (null != mCursor && mCursor.moveToFirst()) {
//                mClickedPos = getListPosition(mRingtoneManager.getRingtonePosition(mExistingUri));
//            }
//        }
//
//        /// If no ringtone has been checked, show default instead.
//        if (POS_UNKNOWN == mClickedPos) {
//            if (null != mCursor && mCursor.moveToFirst()) {
//                mClickedPos = getListPosition(mRingtoneManager.getRingtonePosition(
//                        RingtoneManager.getDefaultRingtoneUri(getApplicationContext(), mType)));
//            }
//        }
//        listView.setItemChecked(mClickedPos, true);
//        listView.setSelection(mClickedPos);
//        mNeedRefreshOnResume = false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_CLICKED_POS, mClickedPos);
    }

    private void playRingtone(int position, int delayMs) {
        mHandler.removeCallbacks(this);
        mSampleRingtonePos = position;
        mHandler.postDelayed(this, delayMs);
    }

    @Override
    public void run() {
        stopAnyPlayingRingtone();
        if (mSampleRingtonePos == mSilentPos) {
            return;
        }

        /*
         * Stop the default ringtone, if it's playing (other ringtones will be
         * stopped by the RingtoneManager when we get another Ringtone from it.
         */
        if (mDefaultRingtone != null && mDefaultRingtone.isPlaying()) {
            mDefaultRingtone.stop();
            mDefaultRingtone = null;
        }

        Ringtone ringtone;
        ringtone = mRingtoneManager.getRingtone(getRingtoneManagerPosition(mSampleRingtonePos));
        mCurrentRingtone = ringtone;


        if (ringtone != null) {
            ringtone.play();
        }
    }

    private void stopAnyPlayingRingtone() {
        if (sPlayingRingtone != null && sPlayingRingtone.isPlaying()) {
            sPlayingRingtone.stop();
        }
        sPlayingRingtone = null;

        if (mRingtoneManager != null) {
            mRingtoneManager.stopPreviousRingtone();
        }
    }

    private int getRingtoneManagerPosition(int listPos) {
        return listPos - mStaticItemCount;
    }

    private void prepareListView(ListView listView) {
        /// M: Add "More Ringtone" to the top of listview to let user choose more ringtone

        if (mHasSilentItem) {
            mSilentPos = addSilentItem(listView);

            // The 'Silent' item should use a null Uri
            if (mExistingUri == null) {
                mClickedPos = mSilentPos;
            }
        }

        if (mClickedPos == POS_UNKNOWN) {
            if (RingtoneManager.isRingtoneExist(getApplicationContext(), mExistingUri)) {
                mClickedPos = getListPosition(mRingtoneManager.getRingtonePosition(mExistingUri));
            } else {
                mClickedPos = getListPosition(mRingtoneManager.getRingtonePosition(
                        RingtoneManager.getDefaultRingtoneUri(
                                getApplicationContext(), mType)));
            }
        }
        listView.setItemChecked(mClickedPos, true);
    }

    private int getListPosition(int ringtoneManagerPos) {

        // If the manager position is -1 (for not found), return that
        if (ringtoneManagerPos < 0) return ringtoneManagerPos;

        return ringtoneManagerPos + mStaticItemCount;
    }


    private int addSilentItem(ListView listView) {
        return addStaticItem(listView, com.android.internal.R.string.ringtone_silent);
    }

    /**
     * Adds a static item to the top of the list.
     * A static item is one that is not from the
     * RingtoneManager.
     *
     * @param listView The ListView to add to.
     * @param textResId The resource ID of the text for the item.
     * @return The position of the inserted item.
     */
    private int addStaticItem(ListView listView, int textResId) {
        CheckedTextView layout = (CheckedTextView) getLayoutInflater().inflate(
                android.R.layout.simple_list_item_single_choice,
                listView, false);
        TextView textView = (TextView) layout.findViewById(android.R.id.text1);
        textView.setText(textResId);
        listView.addHeaderView(layout);
        mStaticItemCount++;
        return listView.getHeaderViewsCount() - 1;
    }

    @Override
    public void onBackPressed() {
        mRingtoneManager.stopPreviousRingtone();
        Intent resultIntent = new Intent();
        Uri uri;
        if (mClickedPos == mSilentPos) {
            // A null Uri is for the 'Silent' item
            uri = null;
        } else {
            uri = mRingtoneManager.getRingtoneUri(getRingtoneManagerPosition(mClickedPos));
        }

//        resultIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, uri);
        resultIntent.setData(uri);
        setResult(RESULT_OK, resultIntent);
        super.onBackPressed();
    }

    private static final int checkboxId = android.R.id.button1;


//    private class RingtonePickerAdapter extends CursorAdapter {
//        private final Context mContext;
//        private final LayoutInflater mFactory;
//        private final ListView mList;
//        private Cursor mCursor;
//
//        HashMap<String,Boolean> checkStates = new HashMap<String,Boolean>();
//        int[] mFrom;
//
//        public class ItemHolder {
//            // views for optimization
//            RadioButton radioButton;
//            TextView textView;
//        }
//
//        public RingtonePickerAdapter(Context context, Cursor c, ListView listView, String[] from) {
//            super(context, c);
//            mContext = context;
//            mFactory = LayoutInflater.from(context);
//            mList = listView;
//            mCursor = c;
//            findColumns(c, from);
//        }
//
//        @Override
//        public View getView(final int position, View convertView, ViewGroup parent) {
//            if (!getCursor().moveToPosition(position)) {
//                return null;
//            }
//
//            View v;
//            if (convertView == null) {
//                v = newView(mContext, getCursor(), parent);
//            } else {
//                v = convertView;
//            }
//
//            Object holder = v.getTag();
//            if (holder == null) {
//                setNewHolder(v);
//            }
//
//            final ItemHolder itemHolder = (ItemHolder) holder;
//            final RadioButton radio = (RadioButton) v.findViewById(android.R.id.button1);
//            itemHolder.radioButton = radio;
//            itemHolder.radioButton.setOnClickListener(new View.OnClickListener() {
//
//                public void onClick(View v) {
//
//                    //重置，确保最多只有一项被选中
//                    for(String key:checkStates.keySet()){
//                        checkStates.put(key, false);
//
//                    }
//                    checkStates.put(String.valueOf(position), radio.isChecked());
//                    RingtonePickerAdapter.this.notifyDataSetChanged();
//                }
//            });
//
//            bindView(v, mContext, getCursor(), itemHolder);
//            return v;
//        }
//
//        @Override
//        public View newView(Context context, Cursor cursor, ViewGroup parent) {
//            final View view = mFactory
//                    .inflate(com.hb.R.layout.list_item_1_line_single_choice, parent, false);
//            setNewHolder(view);
//            return view;
//        }
//
//        private ItemHolder setNewHolder(View view) {
//            final ItemHolder itemHolder = new ItemHolder();
////            itemHolder.radioButton = (RadioButton) view.findViewById(android.R.id.button1);
//            itemHolder.textView = (TextView) view.findViewById(android.R.id.text1);
//            view.setTag(itemHolder);
//            return itemHolder;
//        }
//
//        public void bindView(View view, Context context, Cursor cursor, ItemHolder holder) {
//            String text = cursor.getString(mFrom[0]);
//            if (text == null) {
//                text = "";
//            }
//
//            holder.textView.setText(text);
//        }
//
//        @Override
//        public void bindView(View view, Context context, Cursor cursor) {
//
//        }
//
//        private void findColumns(Cursor c, String[] from) {
//            if (c != null) {
//                int i;
//                int count = from.length;
//                if (mFrom == null || mFrom.length != count) {
//                    mFrom = new int[count];
//                }
//                for (i = 0; i < count; i++) {
//                    mFrom[i] = c.getColumnIndexOrThrow(from[i]);
//                }
//            } else {
//                mFrom = null;
//            }
//        }
//
//        public void updateCheckState(int position, RadioButton radio) {
//            //重置，确保最多只有一项被选中
//            for(String key:checkStates.keySet()){
//                checkStates.put(key, false);
//
//            }
//            checkStates.put(String.valueOf(position), radio.isChecked());
//        }
//    }
 }
