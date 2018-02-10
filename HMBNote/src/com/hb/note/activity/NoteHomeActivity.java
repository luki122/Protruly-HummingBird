package com.hb.note.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.HbSearchView;
import android.widget.HbSearchView.OnQueryTextListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.hb.note.R;
import com.hb.note.db.NoteData;
import com.hb.note.db.NoteDataHelper;
import com.hb.note.ui.HBTextViewSnippet;
import com.hb.note.util.PatternUtils;
import com.hb.note.util.PermissionUtils;
import com.hb.note.util.TimeUtils;
import com.hb.note.util.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import hb.app.HbActivity;
import hb.app.dialog.ProgressDialog;
import hb.view.menu.bottomnavigation.BottomNavigationView;
import hb.view.menu.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;
import hb.widget.ActionMode;
import hb.widget.ActionMode.Item;
import hb.widget.ActionModeListener;
import hb.widget.FloatingActionButton;
import hb.widget.FloatingActionButton.OnFloatActionButtonClickListener;
import hb.widget.HbCheckListAdapter;
import hb.widget.HbListView;
import hb.widget.SliderView;
import hb.widget.toolbar.Toolbar;

public class NoteHomeActivity extends HbActivity {

    private static final int REQUEST_CODE_EDIT = 1001;

    private Toolbar noteToolbar;
    private ImageView toolbarBack;
    private TextView toolbarTitle;
    private HbSearchView toolbarSearchView;
    private LinearLayout ll_search;
    private HbListView searchListView;

    private View headerView;
    private HbListView listView;
    private FloatingActionButton floatingActionButton;
    private BottomNavigationView mBottombar;

    private List<NoteData> mNoteDataList;
    private NoteListCheckAdapter noteListCheckAdapter;
    private Set<Integer> editSelectSet;

    private List<NoteData> mNoteSearchDataList;
    private NoteListCheckAdapter noteSearchListCheckAdapter;

    private boolean editMode = false;
    private boolean searchMode = false;
    private boolean topMode = true;     // 是否置顶模式，true为设置置顶，false为取消置顶
    private String searchKey = "";

    private ProgressDialog deleteProgressDialog;
    private ProgressDialog stickProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_home_activity);

        initView();
        PermissionUtils.requestAppDefaultPermissions(this, REQUEST_CODE_EDIT);
        initData();
        initActionMode();
        updateNoteStyle();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        updateNoteStyle();
    }

    private void initView() {
        noteToolbar = (Toolbar) findViewById(R.id.note_toolbar);
        toolbarBack = (ImageView) findViewById(R.id.toolbar_back);
        toolbarTitle = (TextView) findViewById(R.id.toolbar_title);
        toolbarSearchView = (HbSearchView) findViewById(R.id.toolbar_search_view);

        ll_search = (LinearLayout) findViewById(R.id.ll_search);
        searchListView = (HbListView) findViewById(R.id.searchListView);

        listView = (HbListView) findViewById(R.id.listView);
        floatingActionButton = (FloatingActionButton) findViewById(R.id.floating_action_button);
        mBottombar = (BottomNavigationView) findViewById(R.id.bottom_menu);

        headerView = LayoutInflater.from(this).inflate(R.layout.note_header, null);
        View footerView = LayoutInflater.from(this).inflate(R.layout.note_footer, null);
        headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editMode) {
                    startSearch();
                }
            }
        });
        footerView.setClickable(false);

        toolbarBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endSearch();
            }
        });

        toolbarSearchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String queryText) {
                searchFromList(queryText);
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String arg0) {
                return false;
            }

        });

        listView.addHeaderView(headerView, null, true);
        listView.addFooterView(footerView, null, true);
        listView.setHeaderDividersEnabled(false);
        listView.setFooterDividersEnabled(false);

        floatingActionButton.setOnFloatingActionButtonClickListener(new OnFloatActionButtonClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NoteHomeActivity.this, NoteEditorActivity.class);
                startActivityForResult(intent, REQUEST_CODE_EDIT);
            }
        });

        mBottombar.setItemTextAppearance(R.style.NoteBottomNavigationItem);
        mBottombar.showItem(R.id.action_top, true);
        mBottombar.showItem(R.id.action_delete, true);

        mBottombar.setNavigationItemSelectedListener(new OnNavigationItemSelectedListener() {
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.action_top:
                        if (editSelectSet.size() > 0) {
                            List<Integer> editList = new ArrayList<Integer>();
                            for (Integer i : editSelectSet) {
                                editList.add(mNoteDataList.get(i).getId());
                            }
                            exitEditMode();

                            if (topMode) {
                                new StickNoteDataTask().execute(editList);
                            } else {
                                new CancelStickNoteDataTask().execute(editList);
                            }
                        }
                        break;
                    case R.id.action_delete:
                        if (editSelectSet.size() > 0) {
                            List<Integer> deleteList = new ArrayList<Integer>();
                            for (Integer i : editSelectSet) {
                                deleteList.add(mNoteDataList.get(i).getId());
                            }
                            exitEditMode();

                            new DeleteNoteDataTask().execute(deleteList);
                        }
                        break;
                }
                return false;
            }
        });
    }

    private void initData() {
        mNoteDataList = new ArrayList<NoteData>();
        noteListCheckAdapter = new NoteListCheckAdapter(this, mNoteDataList, false);
        listView.setAdapter(noteListCheckAdapter);

        mNoteSearchDataList = new ArrayList<NoteData>();
        noteSearchListCheckAdapter = new NoteListCheckAdapter(this, mNoteSearchDataList, true);
        searchListView.setAdapter(noteSearchListCheckAdapter);

        editSelectSet = new HashSet<Integer>();

        new NoteDataLoader().execute();
    }

    private void initActionMode() {
        getActionMode().setTitle(getString(R.string.title_selected_count, 0));
        getActionMode().setPositiveText(getString(R.string.select_all));
        getActionMode().setNagativeText(getString(R.string.cancel));
        getActionMode().bindActionModeListener(new ActionModeListener() {
            /**
             * ActionMode上面的操作按钮点击时触发，在这个回调中，默认提供两个ID使用，
             * 确定按钮的ID是ActionMode.POSITIVE_BUTTON,取消按钮的ID是ActionMode.NAGATIVE_BUTTON
             * @param view
             */
            public void onActionItemClicked(Item item) {
                switch (item.getItemId()) {
                    case ActionMode.NAGATIVE_BUTTON:
                        exitEditMode();
                        break;
                    case ActionMode.POSITIVE_BUTTON:
                        if (topMode) {
                            int topSize = 0;
                            for (int i = 0; i < mNoteDataList.size(); i++) {
                                if (mNoteDataList.get(i).getStickTime() == 0) {
                                    topSize++;
                                }
                            }

                            if (editSelectSet.size() < topSize) {
                                for (int i = 0; i < mNoteDataList.size(); i++) {
                                    if (mNoteDataList.get(i).getStickTime() == 0) {
                                        editSelectSet.add(i);
                                    }
                                }
                            } else {
                                editSelectSet.clear();
                            }
                        } else {
                            int cancelTopSize = 0;
                            for (int i = 0; i < mNoteDataList.size(); i++) {
                                if (mNoteDataList.get(i).getStickTime() != 0) {
                                    cancelTopSize++;
                                }
                            }

                            if (editSelectSet.size() < cancelTopSize) {
                                for (int i = 0; i < mNoteDataList.size(); i++) {
                                    if (mNoteDataList.get(i).getStickTime() != 0) {
                                        editSelectSet.add(i);
                                    }
                                }
                            } else {
                                editSelectSet.clear();
                            }
                        }
                        noteListCheckAdapter.notifyDataSetChanged();
                        updateActionMode();
                        break;

                    default:
                        break;
                }
            }

            /**
             * ActionMode显示的时候触发
             * @param actionMode
             */
            public void onActionModeShow(ActionMode actionMode) {

            }

            /**
             * ActionMode消失的时候触发
             * @param actionMode
             */
            public void onActionModeDismiss(ActionMode actionMode) {

            }

        });
    }

    private void updateActionMode() {
        getActionMode().setTitle(getString(R.string.title_selected_count, editSelectSet.size()));


        if (topMode) {
            int topSize = 0;
            for (int i = 0; i < mNoteDataList.size(); i++) {
                if (mNoteDataList.get(i).getStickTime() == 0) {
                    topSize++;
                }
            }

            if (editSelectSet.size() < topSize) {
                getActionMode().setPositiveText(getString(R.string.select_all));
            } else {
                getActionMode().setPositiveText(getString(R.string.cancel_select_all));
            }
        } else {
            int cancelTopSize = 0;
            for (int i = 0; i < mNoteDataList.size(); i++) {
                if (mNoteDataList.get(i).getStickTime() != 0) {
                    cancelTopSize++;
                }
            }

            if (editSelectSet.size() < cancelTopSize) {
                getActionMode().setPositiveText(getString(R.string.select_all));
            } else {
                getActionMode().setPositiveText(getString(R.string.cancel_select_all));
            }
        }


        if (topMode) {
            mBottombar.setItemTitle(getString(R.string.set_top), R.id.action_top);
        } else {
            mBottombar.setItemTitle(getString(R.string.cancel_set_top), R.id.action_top);
        }
    }

    private void updateNoteStyle() {
        floatingActionButton.setBackgroundResource(Utils.getResIds()[9]);

        getActionMode().setItemTextAppearance(android.R.id.text1, Utils.getResIds()[10]);
        getActionMode().setItemTextAppearance(android.R.id.text2, Utils.getResIds()[10]);
    }

    private void startEditMode() {
        if (!isActionModeShowing()) {
            showActionMode(true);
            editMode = true;
            listView.setLongClickable(false);

            mBottombar.setVisibility(editMode ? View.VISIBLE : View.GONE);
            floatingActionButton.setVisibility(editMode ? View.GONE : View.VISIBLE);

            noteListCheckAdapter.setChecked(true);

            editSelectSet.clear();
        }
    }

    private void exitEditMode() {
        if (isActionModeShowing()) {
            showActionMode(false);
            editMode = false;
            listView.setLongClickable(true);

            mBottombar.setVisibility(editMode ? View.VISIBLE : View.GONE);
            floatingActionButton.setVisibility(editMode ? View.GONE : View.VISIBLE);

            noteListCheckAdapter.setChecked(false);
        }
    }

    private void startSearch() {
        if (!searchMode) {
            searchMode = true;
            toolbarBack.setVisibility(View.VISIBLE);
            toolbarTitle.setVisibility(View.GONE);
            toolbarSearchView.setVisibility(View.VISIBLE);
            ll_search.setVisibility(View.VISIBLE);
            searchListView.setVisibility(View.VISIBLE);
            toolbarSearchView.setIconified(false);
            floatingActionButton.setVisibility(View.GONE);
            searchKey = "";
        }
    }

    private void endSearch() {
        if (searchMode) {
            searchMode = false;
            toolbarBack.setVisibility(View.GONE);
            toolbarTitle.setVisibility(View.VISIBLE);
            toolbarSearchView.setVisibility(View.GONE);
            ll_search.setVisibility(View.GONE);
            searchListView.setVisibility(View.GONE);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    floatingActionButton.setVisibility(View.VISIBLE);
                }
            }, 100);
            toolbarSearchView.setQuery("", true);
        }
    }

    private void searchFromList(String key) {
        mNoteSearchDataList.clear();
        if (!TextUtils.isEmpty(key)) {
            for (NoteData data : mNoteDataList) {
                if (data.getTitle().contains(key)) {
                    mNoteSearchDataList.add(data);
                }
            }
        }
        searchKey = key;
        noteSearchListCheckAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT && resultCode == Activity.RESULT_OK) {
            new NoteDataLoader().execute();
        }
    }

    @Override
    public void onBackPressed() {
        if (searchMode) {
            endSearch();
            return;
        }

        if (editMode) {
            exitEditMode();
        } else {
            super.onBackPressed();
        }
    }

    private class NoteDataLoader extends AsyncTask<Void, Void, List<NoteData>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(List<NoteData> noteDataList) {
            if (noteDataList != null) {
                mNoteDataList.clear();
                mNoteDataList.addAll(noteDataList);

                noteListCheckAdapter.notifyDataSetChanged();
            } else {
                mNoteDataList.clear();
                noteListCheckAdapter.notifyDataSetChanged();
            }

            if (searchMode) {
                searchFromList(toolbarSearchView.getQuery().toString());
            }
        }

        @Override
        protected List<NoteData> doInBackground(Void... params) {
            NoteDataHelper noteDataHelper = new NoteDataHelper(NoteHomeActivity.this);
            List<NoteData> noteDataList = noteDataHelper.queryAll();
            noteDataHelper.shutdown();
            return noteDataList;
        }
    }

    private class DeleteNoteDataTask extends AsyncTask<List<Integer>, Void, Integer> {

        @Override
        protected void onPreExecute() {
            if (deleteProgressDialog == null) {
                deleteProgressDialog = new ProgressDialog(NoteHomeActivity.this);
                deleteProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                deleteProgressDialog.setMessage(deleteProgressDialog.getContext()
                        .getString(R.string.deleting));
                deleteProgressDialog.setCancelable(false);
            }
            if (!deleteProgressDialog.isShowing()) {
                deleteProgressDialog.show();
            }
        }

        @Override
        protected Integer doInBackground(List<Integer>... params) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            List<Integer> deleteList = params[0];
            int result = 0;
            if (deleteList != null && deleteList.size() > 0) {
                NoteDataHelper noteDataHelper = new NoteDataHelper(NoteHomeActivity.this);
                result = noteDataHelper.bulkDelete(deleteList);
                noteDataHelper.shutdown();
            }
            return result;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            if (deleteProgressDialog != null && deleteProgressDialog.isShowing()) {
                deleteProgressDialog.dismiss();
            }
            new NoteDataLoader().execute();
        }

    }

    private class StickNoteDataTask extends AsyncTask<List<Integer>, Void, Integer> {

        @Override
        protected void onPreExecute() {
            if (stickProgressDialog == null) {
                stickProgressDialog = new ProgressDialog(NoteHomeActivity.this);
                stickProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                stickProgressDialog.setMessage(stickProgressDialog.getContext()
                        .getString(R.string.sticking));
                stickProgressDialog.setCancelable(false);
            }
            if (!stickProgressDialog.isShowing()) {
                stickProgressDialog.show();
            }
        }

        @Override
        protected Integer doInBackground(List<Integer>... params) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            List<Integer> deleteList = params[0];
            int result = 0;
            if (deleteList != null && deleteList.size() > 0) {
                NoteDataHelper noteDataHelper = new NoteDataHelper(NoteHomeActivity.this);
                result = noteDataHelper.bulkStick(deleteList);
                noteDataHelper.shutdown();
            }
            return result;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            if (stickProgressDialog != null && stickProgressDialog.isShowing()) {
                stickProgressDialog.dismiss();
            }
            new NoteDataLoader().execute();
        }

    }

    private class CancelStickNoteDataTask extends AsyncTask<List<Integer>, Void, Integer> {

        @Override
        protected void onPreExecute() {
            if (stickProgressDialog == null) {
                stickProgressDialog = new ProgressDialog(NoteHomeActivity.this);
                stickProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                stickProgressDialog.setMessage(stickProgressDialog.getContext()
                        .getString(R.string.canceling_stick));
                stickProgressDialog.setCancelable(false);
            }
            if (!stickProgressDialog.isShowing()) {
                stickProgressDialog.show();
            }
        }

        @Override
        protected Integer doInBackground(List<Integer>... params) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            List<Integer> deleteList = params[0];
            int result = 0;
            if (deleteList != null && deleteList.size() > 0) {
                NoteDataHelper noteDataHelper = new NoteDataHelper(NoteHomeActivity.this);
                result = noteDataHelper.bulkCancelStick(deleteList);
                noteDataHelper.shutdown();
            }
            return result;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            if (stickProgressDialog != null && stickProgressDialog.isShowing()) {
                stickProgressDialog.dismiss();
            }
            new NoteDataLoader().execute();
        }

    }

    private class NoteListCheckAdapter extends HbCheckListAdapter {

        private Context mContext;
        private List<NoteData> mListData;
        private String timePattern = "";
        private boolean mSearchMode = false;

        public NoteListCheckAdapter(Context context, List<NoteData> listData, boolean searchMode) {
            mContext = context;
            mListData = listData;
            timePattern = context.getString(R.string.item_note_time);
            mSearchMode = searchMode;
        }

        @Override
        protected View onCreateView(int i, ViewGroup viewGroup) {
            ViewHolder viewHolder = new ViewHolder();
            View ret = LayoutInflater.from(mContext).inflate(R.layout.note_slider_view, null);
            viewHolder.mSliderView = (SliderView) ret;
            viewHolder.mCheckBox = (CheckBox) viewHolder.mSliderView.findViewById(R.id.checkbox);
            viewHolder.mMoveLayout = viewHolder.mSliderView.findViewById(R.id.note_item_layout);
            viewHolder.tv_title = (TextView) viewHolder.mMoveLayout.findViewById(R.id.tv_title);
            viewHolder.tvs_title = (HBTextViewSnippet) viewHolder.mMoveLayout.findViewById(R.id.tvs_title);
            viewHolder.tv_time = (TextView) viewHolder.mMoveLayout.findViewById(R.id.tv_time);
            viewHolder.iv_img = (ImageView) viewHolder.mMoveLayout.findViewById(R.id.iv_img);
            ret.setTag(viewHolder);
            return ret;
        }

        @Override
        protected void onBindView(final int position, View itemView) {
            final NoteData data = mListData.get(position);
            final ViewHolder viewHolder = (ViewHolder) itemView.getTag();
            viewHolder.mSliderView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (editMode) {

                        // 置顶与取消置顶模式下，相对项不可用
                        if (topMode) {
                            if (data.getStickTime() == 0) {
                                if (editSelectSet.contains(position)) {
                                    editSelectSet.remove(position);
                                } else {
                                    editSelectSet.add(position);
                                }
                                viewHolder.mCheckBox.setChecked(!viewHolder.mCheckBox.isChecked());
                                updateActionMode();
                            }
                        } else {
                            if (data.getStickTime() != 0) {
                                if (editSelectSet.contains(position)) {
                                    editSelectSet.remove(position);
                                } else {
                                    editSelectSet.add(position);
                                }
                                viewHolder.mCheckBox.setChecked(!viewHolder.mCheckBox.isChecked());
                                updateActionMode();
                            }
                        }
                    } else {
                        int realPosition = position;
                        if (realPosition < 0) {
                            Toast.makeText(NoteHomeActivity.this, "open search view", Toast.LENGTH_SHORT).show();
                        } else {
                            if (realPosition < getCount()) {
                                Intent intent = new Intent(NoteHomeActivity.this, NoteEditorActivity.class);
                                intent.putExtra(NoteEditorActivity.NOTE_ID, mListData.get(realPosition).getId());
                                startActivityForResult(intent, REQUEST_CODE_EDIT);
                            }
                        }
                    }
                }
            });
            viewHolder.mSliderView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (!mSearchMode) {
                        if (!editMode) {
                            if (data.getStickTime() != 0) {
                                topMode = false;
                            } else {
                                topMode = true;
                            }
                            startEditMode();
                            viewHolder.mCheckBox.setChecked(true);
                            editSelectSet.add(position);
                            updateActionMode();
                        } else {
                            return false;
                        }
                    }
                    return true;
                }
            });

            viewHolder.mSliderView.setCustomBackground(SliderView.CUSTOM_BACKGROUND_RIPPLE);

            if (data.getStickTime() != 0) {
                viewHolder.mSliderView.setBackgroundResource(R.color.stick_list_item_bg);
            } else {
                viewHolder.mSliderView.setBackgroundResource(R.color.white);
            }

            viewHolder.mCheckBox.setButtonDrawable(Utils.getResIds()[11]);

            if (!isChecked()) {
                viewHolder.mCheckBox.setChecked(false);
            } else {
                if (editSelectSet.contains(position)) {
                    viewHolder.mCheckBox.setChecked(true);
                } else {
                    viewHolder.mCheckBox.setChecked(false);
                }
            }


            if (searchMode) {
                viewHolder.tv_title.setVisibility(View.GONE);
                viewHolder.tvs_title.setVisibility(View.VISIBLE);

                viewHolder.tvs_title.setText(data.getTitle(), searchKey, Utils.getResIds()[9]);
            } else {
                viewHolder.tv_title.setVisibility(View.VISIBLE);
                viewHolder.tvs_title.setVisibility(View.GONE);

                viewHolder.tv_title.setText(data.getTitle());
            }


            viewHolder.tv_time.setText(TimeUtils.formatTime(data.getUpdateTime(), timePattern));

            if (data.getImageCount() > 0) {
                List<String> images = PatternUtils.findAllImages(data.getContent());
                if (images != null && images.size() > 0) {
                    viewHolder.iv_img.setVisibility(View.VISIBLE);
                    Glide.with(NoteHomeActivity.this).load(images.get(0)).into(viewHolder.iv_img);
                } else {
                    viewHolder.iv_img.setVisibility(View.GONE);
                }
            } else {
                viewHolder.iv_img.setVisibility(View.GONE);
            }

        }

        @Override
        protected View getCheckBox(int position, View itemView) {
            ViewHolder viewHolder = (ViewHolder) itemView.getTag();
            return viewHolder.mCheckBox;
        }

        @Override
        protected View getMoveView(int position, View itemView) {
            ViewHolder viewHolder = (ViewHolder) itemView.getTag();
            return viewHolder.mMoveLayout;
        }

        @Override
        public int getCount() {
            return mListData == null ? 0 : mListData.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        private class ViewHolder {
            public SliderView mSliderView;
            public CheckBox mCheckBox;
            public View mMoveLayout;
            public TextView tv_title;
            public HBTextViewSnippet tvs_title;
            public TextView tv_time;
            public ImageView iv_img;
        }

    }

}
