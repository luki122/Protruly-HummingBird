package com.protruly.music.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.HbSearchView;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.protruly.music.MediaPlaybackService;
import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.adapter.HBMusicListAdapter;
import com.protruly.music.adapter.HBSearchAdapter;
import com.protruly.music.downloadex.DownloadManager;
import com.protruly.music.model.SearchItem;
import com.protruly.music.ui.album.AlbumDetailActivity;
import com.protruly.music.ui.album.AlbumListActivity;
import com.protruly.music.util.DialogUtil;
import com.protruly.music.util.DialogUtil.OnAddPlaylistSuccessListener;
import com.protruly.music.util.Globals;
import com.protruly.music.util.HBListItem;
import com.protruly.music.util.HBMainMenuData;
import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.widget.SideBar;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import hb.app.HbActivity;
import hb.widget.HbListView;
import hb.app.dialog.AlertDialog;
import hb.widget.toolbar.Toolbar;

/**
 * Created by hujianwei on 17-9-5.
 */

public class HBTrackBrowserActivity extends AbstractBaseActivity implements DialogUtil.OnDeleteFileListener {
    private static final int MSG_BTNPLAY_CLICK = 100001;
    private static final int MSG_SHOW_ANIMATION = 100003;
    private static final int MSG_GET_PLAYLIST = 100008;
    private static final int MSG_CHANGE_ADAPTER = 100009;
    private static final int MSG_SHOW_DIALOG = 100012;


    private static final int MSG_UPDATE_LISTVIEW = 100010;

    private volatile int fromShuffl = 0;
    public static final String FROM_CODE = "fromCode";
    public boolean fromAdd = false;
    private TextView tv_menu;
    private static String[] mCursorCols;
    private SideBar sideBar;
    private TextView tv_sidebar;
    private TextView dialog;
    private boolean inSearch = false;
    private TextView btn_menuCanel;
    private TextView btn_menuAll;
    private View actionBar_play;

    // 动画是否在运行
    private boolean isPlaying = false;
    private Animation operatingAnim;

    private ArrayList<String> mAddList = null;
    private HBMainMenuData playlistInfo;

    // 0为默认歌曲模式 1为文件夹进入模式
    private int startMode = 0;
    private String mFoldPath;
    private long addArtistId = -1;
    private ArrayList<HBListItem> mArrayList;
    private HbListView mTrackList;
    private HBMusicListAdapter mAdapter;
    private ArrayList<HBListItem> mList;
    private boolean needUpdate;
    private boolean needShowSeleted = true;
    public int mSongNumber = 0;
    private ArrayList<String> mSearchAddList = new ArrayList<String>();
    //private HbActionBar mHbActionBar ;
    private Toolbar mToolBar;

    private HBSearchAdapter mSearchAdapter;
    private View mHeaderView;
    private HbSearchView searchView;

    private ArrayList<SearchItem> mSearchlist = new ArrayList<SearchItem>();
    private ArrayList<SearchItem> artistlist = new ArrayList<SearchItem>();
    private ArrayList<SearchItem> tracklist = new ArrayList<SearchItem>();
    private HbListView mSearchListView;
    private ImageView mPlaySelect;
    ObjectAnimator aima;
    private ColorStateList colorlist;
    private static final String TAG = "HBTrackBrowserActivity";
    private TextView iv_randPlay;
    private TextView tv_songCount;
    private View mHidHeaderView;
    private static final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    private String keyWord;
    private static WindowManager wm;
    private static WindowManager.LayoutParams params;
    private static WindowManager.LayoutParams params2;
    private ImageView btn_floatView;
    private int[] flyendPoint = new int[2];
    private int[] flystartPoint;
    private int alpha;
    private boolean isStop;
    private AnimationSet set;
    private AlertDialog mAlertDialog;
    private List<String> mPathsXml = null;

    private int mWidth = 0;
    private int mHight = 0;

    private boolean mFinished;

    private class HbOnItemlongClickListener implements AdapterView.OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if(inSearch&&searchView.getVisibility()==View.GONE){
                return true;
            }
            goEditMode(position);
            mAdapter.notifyDataSetChanged();
            changeMenuState();
            return false;
        }

    }

    private HbOnItemlongClickListener mOnItemlongClickListener;
    private DialogUtil.OnAddPlaylistSuccessListener mAddPlaylistSuccessListener = new OnAddPlaylistSuccessListener() {

        @Override
        public void OnAddPlaylistSuccess() {
            // TODO Auto-generated method stub
            exitEidtMode();
        }
    };

    class HbItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {


            if (fromShuffl > 0) {
                return;
            }
            if (position == 0)
                return;
            if (fromAdd)
                tv_menu.setText(getString(R.string.hb_add_playlist_song_num, mAdapter.getCheckedCount()));

            if (!mAdapter.getEidtMode())
                Log.d("JW" , "HbItemClickListener:" + position);
                startPlayAnimation(position, true);
        }

    }

    private HbItemClickListener mHbItemClickListener;

    class SortTask extends AsyncTask<Cursor, String, Boolean> {
        private long time;

        @Override
        protected Boolean doInBackground(Cursor... params) {
            // TODO Auto-generated method stub
            needUpdate = true;
            StringBuilder where = new StringBuilder();

            if (startMode == 1) {
                where.append(Globals.QUERY_SONG_FILTER_1);
                where.append(" and " + MediaStore.Audio.Media.DATA + " like \"" + mFoldPath + "%\"");
            } else if (addArtistId != -1) {
                where.append(Globals.QUERY_SONG_FILTER);
                where.append(" and " + MediaStore.Audio.Media.ARTIST_ID + "=" + addArtistId);
            } else {
                where.append(Globals.QUERY_SONG_FILTER);
            }
            Cursor cursor = HBTrackBrowserActivity.this.getContentResolver().query(uri, mCursorCols, where.toString(), null, null);
            initAdapter(cursor);
            return needUpdate;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // TODO Auto-generated method stub
            if (needUpdate && mAdapter != null) {
                mArrayList.clear();
                mArrayList.addAll(mList);
//                if (mTrackList.hbIsRubbishOut()) {
//                    mTrackList.hbSetRubbishBack();
//                }
                mAdapter.notifyDataSetChanged();
                if (MusicUtils.sService != null && needShowSeleted && !fromAdd) {
                    try {
                        long id = MusicUtils.getCurrentAudioId();
                        for (int i = 0; i < mArrayList.size(); i++) {
                            if (mArrayList.get(i).getSongId() == id) {
                                mAdapter.setCurrentPosition(i);
                                needShowSeleted = false;
                                final int j = i;
                                mHbHandler.post(new Runnable() {

                                    @Override
                                    public void run() {
                                        // TODO Auto-generated method stub
                                        if (j == 0)
                                            mTrackList.setSelectionFromTop(j + 1, 0);
                                        else
                                            mTrackList.setSelectionFromTop(j + 1, getResources().getDimensionPixelSize(R.dimen.hb_playmode_height));
                                    }
                                });

                                break;
                            }
                        }
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            if (fromAdd)
                getSeletedItems();
            if (mArrayList.size() == 0) {
                showNavtitle(true);
//                btn_menuAll.setEnabled(false);
//                btn_menuAll.setTextColor(getResources().getColor(R.color.hb_select_disable));
            } else {
                showNavtitle(false);
//                btn_menuAll.setEnabled(true);
//                if (colorlist != null)
//                    btn_menuAll.setTextColor(colorlist);
//                sideBar.setVisibility(View.VISIBLE);
            }
            setPlayAnimation();
            super.onPostExecute(result);
        }
    }

    // 接收歌曲数据加载完毕或删除数据消息，更新UI
    static class HbHandler extends Handler {
        WeakReference<HBTrackBrowserActivity> mActivity;

        public HbHandler(HBTrackBrowserActivity activity) {
            mActivity = new WeakReference<HBTrackBrowserActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            switch (msg.what) {
                case MSG_SHOW_ANIMATION:
                    mActivity.get().setPlayAnimation();
                    break;
                case MSG_CHANGE_ADAPTER:
                    mActivity.get().mTrackList.setAdapter(mActivity.get().mAdapter);
                case MSG_GET_PLAYLIST:
                    break;

                case MSG_UPDATE_LISTVIEW:
                    mActivity.get().updateScrollListView();
                    break;
            }

            super.handleMessage(msg);
        }
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what==MSG_SHOW_DIALOG){
                ArrayList<String> list = new ArrayList<String>();
                for (int i = 0; i < mAdapter.getCount(); i++) {
                    if (mAdapter.isItemChecked(i)) {
                        list.add(String.valueOf(((HBListItem) mAdapter.getItem(i)).getSongId()));
                    }
                }
                if (mSearchAddList != null && mSearchAddList.size() > 0) {
                    list.addAll(mSearchAddList);
                }
                DialogUtil.showAddDialog(HBTrackBrowserActivity.this, list, mAddPlaylistSuccessListener);
            }
        };
    };

    private void updateScrollListView() {
        if (mTrackList == null || mHidHeaderView == null) {
            return;
        }

        if (mTrackList.getFirstVisiblePosition() == 1)
            setTagState(mTrackList.getFirstVisiblePosition());
        else
            setTagState(mTrackList.getFirstVisiblePosition() + 1);

        if (mTrackList.getFirstVisiblePosition() >= 1) {
            mHidHeaderView.setVisibility(View.VISIBLE);
        } else {
            mHidHeaderView.setVisibility(View.GONE);
            //mTrackList.hbSetHeaderViewYOffset(-1);
        }

        updataSongCount(mArrayList.size());
        setRubbishBack();

        return;
    }

    private HbHandler mHbHandler;

    private SortTask mSortTask;

    // 搜索输入框数据变化接口
//    class SongSearchViewQueryTextChangeListener implements OnQueryTextListener {
//
//        @Override
//        public boolean onQueryTextSubmit(String query) {
//            return false;
//
//        }
//
//        @Override
//        public boolean onQueryTextChange(String newText) {
//            if (TextUtils.isEmpty(newText.trim())) {
//                if (getSearchView()!=null&&getSearchView().getQueryTextView() != null)
//                    getSearchView().getQueryTextView().requestFocus();
//            }
//            keyWord = newText;
//            new GetSearchTask().execute(newText);
//            return false;
//        }
//    }

//    private HbActionBar.OnHbActionBarItemClickListener mOnActionBarListener = new OnHbActionBarItemClickListener() {
//        @Override
//        public void onHbActionBarItemClicked(int itemid) {

    @Override
    public void updateOptionMenu(Menu menu) {
        super.updateOptionMenu(menu);
    }
//            Intent intent = new Intent(HBTrackBrowserActivity.this, HBPlayerActivity.class);
//            startActivity(intent);
//        }
//    };

//    private HBMenuBase.OnHbMenuItemClickListener hbMenuCallBack = new HBMenuBase.OnHbMenuItemClickListener() {
//        @Override
//        public void hbMenuItemClick(int itemId) {
//            if (fromAdd) {
//                if (playlistInfo.getPlaylistId() < 0) {
//                    Log.e(TAG, "PlaylistId error!" );
//                    return;
//                }
//                MusicUtils.addToPlaylist(HBTrackBrowserActivity.this, mAddList, playlistInfo.getPlaylistId(), playlistInfo.getName());
//                gobackToSongSingle();
//            } else {
//                switch (itemId) {
//                    case R.id.song_backup:
//                        deleteSongs();
//                        break;
//                    case R.id.song_add:
//                        handler.sendEmptyMessageDelayed(MSG_SHOW_DIALOG, 100);
//
//                        break;
//                    default:
//                        break;
//                }
//            }
//        }
//    };


    // 屏蔽标题返回功能
//    private OnHbActionBarBackItemClickListener mOnActionBarBackItemListener = new OnHbActionBarBackItemClickListener() {
//        @Override
//        public void onHbActionBarBackItemClicked(int itemid) {
//            if (fromAdd || !mTrackList.hbIsRubbishOut()) {
//                finishWithDataResult();
//            } else if (mTrackList.hbIsRubbishOut()) {
//                mTrackList.hbSetRubbishBack();
//            }
//        }
//    };
//
//    @Override
//    public void hbDeleteItem(View arg0, final int position) {
//        // TODO Auto-generated method stub
//        final HBListItem item = mArrayList.get(position - 1);
//        long id = item.getSongId();
//        mAdapter.deleteItem(position - 1);
//        if (position - 1 < mAdapter.getCurrentPosition()) {
//            mAdapter.setCurrentPosition(mAdapter.getCurrentPosition() - 1);
//        }
//        if (mArrayList.size() == 0) {
//            actionBar_play.clearAnimation();
//            actionBar_play.setBackgroundResource(R.drawable.hb_left_bar_clicked);
//            showNavtitle(true);
//        } else {
//            showNavtitle(false);
//        }
//        mSongNumber = mArrayList.size();
//        final long[] list = new long[1];
//        list[0] = id;
//        mHbHandler.postDelayed(new Runnable() {
//
//            @Override
//            public void run() {
//                // TODO Auto-generated method stub
//                DownloadManager.getInstance(HBTrackBrowserActivity.this).removeDownloadByPath(item.getFilePath());
//                MusicUtils.deleteTracks(HBTrackBrowserActivity.this, list);
//            }
//        }, 100);
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        if (intent != null) {
//            if (intent.getIntExtra(FROM_CODE, -1) == 1) {
//                fromAdd = true;
//
//                playlistInfo = (HBMainMenuData) intent.getSerializableExtra(HBSongSingle.EXTR_PLAYLIST_INFO);
//                mAddList = intent.getStringArrayListExtra(HBNewPlayListActivity.EXTR_ADDLIST_DATA);
//            } else if (intent.getLongExtra("atristid", -1) != -1) {
//                addArtistId = intent.getLongExtra("atristid", -1);
//                fromAdd = true;
//                playlistInfo = (HBMainMenuData) intent.getSerializableExtra(HBSongSingle.EXTR_PLAYLIST_INFO);
//                mAddList = intent.getStringArrayListExtra(HBNewPlayListActivity.EXTR_ADDLIST_DATA);
//            }
//
//            startMode = intent.getIntExtra(HBFoldActivity.EXTR_FOLD_START_MODE, 0);
//            if (startMode == 1) {
//                mFoldPath = intent.getStringExtra(HBFoldActivity.EXTR_FOLD_PATH);
//            }

        }
        if (fromAdd) {
            setHbContentView(R.layout.hb_song_activity);
        } else {
            setHbContentView(R.layout.hb_song_activity);
        }

        mWidth = (int) getResources().getDimension(R.dimen.hb_albumIcon_size);
        mHight = mWidth;

        registerStateChangeReceiver();
        initView();
        initData();
    }

    private void initData() {
        mPathsXml = HBMusicUtil.doParseXml(this, "paths.xml");
        mCursorCols = new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.DURATION };
        mSortTask = new SortTask();
        mArrayList = new ArrayList<HBListItem>();
        mList = new ArrayList<HBListItem>();
        mAdapter = new HBMusicListAdapter(this, mArrayList);
        mTrackList.setAdapter(mAdapter);
        mSearchAdapter = new HBSearchAdapter(this, mSearchlist);
        mSearchListView.setAdapter(mSearchAdapter);
        mSortTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void initAdapter(Cursor cursor) {
        if (cursor == null) {
            return;
        }
        if (cursor.getCount() == mArrayList.size()) {
            needUpdate = false;
            return;
        }
        mList.clear();
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            int mId = cursor.getInt(0);
            String mTitle = "<unknown>";
            if (cursor.getString(2) != null && !TextUtils.isEmpty(cursor.getString(2)))
                mTitle = cursor.getString(2);
            String mPath = "<unknown>";
            if (cursor.getString(3) != null && !TextUtils.isEmpty(cursor.getString(3)))
                mPath = cursor.getString(3);
            String mAlbumName = "<unknown>";
            if (cursor.getString(4) != null && !TextUtils.isEmpty(cursor.getString(4)))
                mAlbumName = cursor.getString(4);
            String mArtistName = "<unknown>";
            if (cursor.getString(5) != null && !TextUtils.isEmpty(cursor.getString(5)))
                mArtistName = cursor.getString(5);
            int mduration = cursor.getInt(7);
            String mUri = mPath;
            String imgUri = HBMusicUtil.getImgPath(getApplication(), HBMusicUtil.MD5(mTitle + mArtistName + mAlbumName));
            long mAlbumId = cursor.getLong(1);
            String mPinyin = MusicUtils.getSpell(mTitle);
            HBListItem listItem = new HBListItem((long) mId, mTitle, mUri, mAlbumName, mAlbumId, mArtistName, 0, imgUri, null, null, -1);
            listItem.setDuration(mduration);
            listItem.setPinyin(mPinyin);
            listItem.setArtistId(cursor.getLong(6));

            listItem.setItemPicSize(mWidth, mWidth);

            mPath = mPath.substring(0, mPath.lastIndexOf("/"));
            if (startMode == 1) {
                // 确保取正确路径
                if (mPath.equals(mFoldPath)) {
                    mList.add(listItem);
                }
            } else {
                if (!mPathsXml.contains(mPath)) {
                    mList.add(listItem);
                }
            }
            
        }
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
        if (mList.size() == 0)
            return;
        Collections.sort(mList, new Comparator<HBListItem>() {

            @Override
            public int compare(HBListItem lhs, HBListItem rhs) {
                // TODO Auto-generated method stub
                if (lhs.getPinyin().charAt(0) == rhs.getPinyin().charAt(0) && (65 > lhs.getTitle().toUpperCase().charAt(0) || lhs.getTitle().toUpperCase().charAt(0) > 90)
                        && (rhs.getTitle().toUpperCase().charAt(0) <= 90 && rhs.getTitle().toUpperCase().charAt(0) >= 65)) {
                    return 1;

                } else if (lhs.getPinyin().charAt(0) == rhs.getPinyin().charAt(0) && (65 > rhs.getTitle().toUpperCase().charAt(0) || rhs.getTitle().toUpperCase().charAt(0) > 90)
                        && (lhs.getTitle().toUpperCase().charAt(0) <= 90 && lhs.getTitle().toUpperCase().charAt(0) >= 65)) {
                    return -1;
                }
                return lhs.getPinyin().compareTo(rhs.getPinyin());
            }
        });
    }

    private void initView() {

        if ( mToolBar == null){
            mToolBar = getToolbar();
            if ( mToolBar == null ){
                return;
            }
        }

        mToolBar.inflateMenu(R.menu.song_playing);


//        setHbBottomBarMenuCallBack(hbMenuCallBack);
//        try {
//            mHbActionBar = getHbActionBar();// 获取actionbar
//            if (mHbActionBar == null) {
//                return;
//            }
//            mHbActionBar.getHbActionBarSearchView().setOnQueryTextListener(new SongSearchViewQueryTextChangeListener());
//
//            if (mHbActionBar.getVisibility() != View.VISIBLE) {
//                ((View) mHbActionBar).setVisibility(View.VISIBLE);
//            }
//            if (startMode == 1) {
//                String titleStr = mFoldPath.substring(mFoldPath.lastIndexOf("/") + 1);
//                mHbActionBar.setTitle(titleStr);
//            } else {
//                mHbActionBar.setTitle(R.string.appwidget_songtitle);
//            }
//            View tView = mHbActionBar.getHomeButton();
//            if (tView != null) {
//                tView.setVisibility(View.VISIBLE);
//            }
            mHbItemClickListener = new HbItemClickListener();
//            mOnItemlongClickListener = new HbOnItemlongClickListener();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        mHbActionBar.setmOnActionBarBackItemListener(mOnActionBarBackItemListener);
//        sideBar = (SideBar) findViewById(R.id.sidebar);
//        tv_sidebar = (TextView) findViewById(R.id.tv_sidebar);
//        dialog = (TextView) findViewById(R.id.dialog);
//        sideBar.setTextView(dialog);
//        sideBar.setOnTouchingLetterChangedListener(new OnTouchingLetterChangedListener() {
//
//            @Override
//            public void onTouchingLetterChanged(String s) {
//                int position = mAdapter.getPositionForSection(s.charAt(0));
//                if (position != -1) {
//                    if (position == 0)
//                        mTrackList.setSelectionFromTop(position + 1, 0);
//                    else
//                        mTrackList.setSelectionFromTop(position + 1, getResources().getDimensionPixelSize(R.dimen.hb_playmode_height));
//                }
//
//            }
//        });
//        setOnSearchViewQuitListener(this);
        mTrackList = (HbListView) findViewById(R.id.song_list);
        mTrackList.setSelector(R.drawable.hb_playlist_item_clicked);
        mPlaySelect = (ImageView) findViewById(R.id.hb_song_selected);
        mSearchListView = (HbListView) findViewById(R.id.hb_search_list);
        mSearchListView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                mSearchListView.requestFocus();
                return false;
            }
        });
        mTrackList.setOnItemLongClickListener(mOnItemlongClickListener);
        mTrackList.setOnItemClickListener(mHbItemClickListener);
        mTrackList.setCacheColorHint(0);

//        // HbListView 删除监听器接口
//        mTrackList.hbSetDeleteItemListener(this);
//
//        // 开启左滑删除功能
//        mTrackList.hbSetNeedSlideDelete(true);


        mTrackList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mTrackList.requestFocus();
                return false;
            }
        });

//
//        mTrackList.hbSetHbBackOnClickListener(new HbListView.HbBackOnClickListener() {
//            @Override
//            public void auroraOnClick(int position) {
//                deleteItem(position);
//            }
//
//            @Override
//            public void auroraPrepareDraged(int position) {
//                sideBar.setVisibility(View.GONE);
//                tv_sidebar.setVisibility(View.GONE);
//            }
//
//            @Override
//            public void auroraDragedSuccess(int position) {
//                if (!mTrackList.hbIsRubbishOut()) {
//                    sideBar.setVisibility(View.VISIBLE);
//                    tv_sidebar.setVisibility(View.VISIBLE);
//                } else {
//                    sideBar.setVisibility(View.GONE);
//                    tv_sidebar.setVisibility(View.GONE);
//                }
//            }
//
//            @Override
//            public void auroraDragedUnSuccess(int position) {
//                if (!mTrackList.hbIsRubbishOut()) {
//                    sideBar.setVisibility(View.VISIBLE);
//                    tv_sidebar.setVisibility(View.VISIBLE);
//                } else {
//                    sideBar.setVisibility(View.GONE);
//                    tv_sidebar.setVisibility(View.GONE);
//                }
//            }
//        });

        mHeaderView = LayoutInflater.from(this).inflate(R.layout.layout_search_view, null);
        mTrackList.addHeaderView(mHeaderView);
        searchView = (HbSearchView) mHeaderView.findViewById(R.id.hb_search);
//        searchView.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                if(!fromAdd&&mAdapter.getEidtMode()){
//                    return;
//                }
//                // TODO Auto-generated method stub
//                inSearch = true;
//                if (mHbActionBar.getHbActionBottomBarMenu().isShowing()) {
//                    mHbActionBar.setShowBottomBarMenu(false);
//                    mHbActionBar.showActionBottomeBarMenu();
//                }
//                showSearchviewLayout();
//                isPlaying=false;
//                final Button btn_search = getSearchViewRightButton();
//                if (btn_search != null) {
//                    if (mAdapter.getEidtMode()) {
//                        btn_search.setText(getResources().getString(R.string.menu_continue));
//                    } else {
//                        btn_search.setText(getResources().getString(R.string.songlist_cancel));
//                    }
//                    btn_search.setOnClickListener(new View.OnClickListener() {
//
//                        @Override
//                        public void onClick(View v) {
//                            inSearch = false;
//                            hideSearchviewLayout();
//                            searchView.setVisibility(View.VISIBLE);
//                            changeMenuState();
//                            mHbActionBar.getHbActionBarSearchView().getQueryTextView().setText("");
//                        }
//                    });
//                }
//                searchView.setVisibility(View.GONE);
//                HBMusicUtil.showInputMethod(HBTrackBrowserActivity.this);
//
//            }
//        });
//        mHbActionBar.getHbActionBarSearchViewBackButton().setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                inSearch = false;
//                searchView.setVisibility(View.VISIBLE);
//                mHbActionBar.getHbActionBarSearchView().getQueryTextView().setText("");
//                hideSearchviewLayout();
//            }
//        });
        mHbHandler = new HbHandler(this);
//        if (fromAdd) {
//            btn_menuCanel = mHbActionBar.getCancelButton();
//            btn_menuCanel.setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    if (fromAdd) {
//                        finishWithDataResult();
//                    } else
//                        exitEidtMode();
//                }
//            });
//
//            btn_menuAll = mHbActionBar.getOkButton();
//            colorlist = btn_menuAll.getTextColors();
//            btn_menuAll.setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    if (((TextView) btn_menuAll).getText().equals(getResources().getString(R.string.selectAll))) {
//                        ((TextView) btn_menuAll).setText(getResources().getString(R.string.selectAllNot));
//                        selectAll();
//                        changeMenuState(2, -1);// modify by chenhl 20140604
//                        mAdapter.notifyDataSetChanged();
//                    } else {
//                        ((TextView) btn_menuAll).setText(getResources().getString(R.string.selectAll));
//                        selectAllNot();
//                        changeMenuState(3, -1);// modify by chenhl 20140604
//                        mAdapter.notifyDataSetChanged();
//                    }
//                }
//            });
//            mHbActionBar.initActionBottomBarMenu(R.menu.menu_songaddedit, 1);
//            tv_menu = mHbActionBar.getHbActionBottomBarMenu().getTitleViewByPosition(0);
//            ((TextView) btn_menuCanel).setText(getResources().getString(R.string.menu_back));
//
//            if (mAddList.size() == 0) {
//                mHbActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(0, false);
//            } else {
//                mHbActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(0, true);
//            }
//            tv_menu.setText(getString(R.string.hb_add_playlist_song_num, mAddList.size()));// modify
//
//        } else {
//            btn_menuCanel = (TextView) mHbActionBar.getSelectLeftButton();
//            btn_menuCanel.setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    if (fromAdd) {
//                        finishWithDataResult();
//                    } else
//                        exitEidtMode();
//                }
//            });
//
//            btn_menuAll = (TextView) mHbActionBar.getSelectRightButton();
//            btn_menuAll.setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    if (((TextView) btn_menuAll).getText().equals(getResources().getString(R.string.selectAll))) {
//                        ((TextView) btn_menuAll).setText(getResources().getString(R.string.selectAllNot));
//                        selectAll();
//                        changeMenuState(2, -1);
//                        mAdapter.notifyDataSetChanged();
//                    } else {
//                        ((TextView) btn_menuAll).setText(getResources().getString(R.string.selectAll));
//                        selectAllNot();
//                        changeMenuState(3, -1);
//                        mAdapter.notifyDataSetChanged();
//                    }
//                }
//            });
//            mHbActionBar.initActionBottomBarMenu(R.menu.menu_songedit, 2);
//            tv_menu = mHbActionBar.getHbActionBottomBarMenu().getTitleViewByPosition(1);
//            mHbActionBar.addItem(R.drawable.song_playing, MSG_BTNPLAY_CLICK, "");
//            actionBar_play = mHbActionBar.getItem(0).getItemView();
//            operatingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate_anim);
//            LinearInterpolator lin = new LinearInterpolator();
//            operatingAnim.setInterpolator(lin);
//            mHbActionBar.setOnHbActionBarListener(mOnActionBarListener);
//        }


        mHidHeaderView = findViewById(R.id.song_hide_header);
        iv_randPlay = (TextView) findViewById(R.id.tv_playmode);
        iv_randPlay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                shufflePlay();
            }
        });
        tv_songCount = (TextView) findViewById(R.id.tv_songnumber);
        tv_songCount.setOnClickListener(null);
        mAlertDialog = new AlertDialog.Builder(this)//.setTitle(R.string.delete)
                // .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final int count = mAdapter.getCheckedCount();
                        final long[] list = new long[count];
                        int index = 0;
                        for (int i = 0; i < mArrayList.size(); i++) {
                            if (mAdapter.isItemChecked(i)) {
                                list[index] = mArrayList.get(i).getSongId();
                                index++;
                            }
                        }
                        MusicUtils.deleteMediaTracks(HBTrackBrowserActivity.this, list, HBTrackBrowserActivity.this, mArrayList);
                    }
                }).setNegativeButton(R.string.cancel, null).create();
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if ( item.getItemId() == R.id.hb_btn_playing ){
            Intent intent = new Intent(HBTrackBrowserActivity.this, HBPlayerActivity.class);
            startActivity(intent);
            Log.d("JW", "onMenuItemClick");
            return  true;
        }
        return false;

    }

    // 显示删除动画
    public void showDeleteAnimation() {
        mAdapter.hasDeleted = true;
        //mTrackList.hbDeleteSelectedItemAnim();
        if (mArrayList.size() > 0) {
            //sideBar.setVisibility(View.VISIBLE);
        }
        //tv_sidebar.setVisibility(View.VISIBLE);
    }

    // 设置分组栏目的状态
    public void setTagState(int position) {
        if (mAdapter == null)
            return;
        if (mArrayList.size() > 0) {
            String pinyin;
            try {
                if (position - 1 >= 0)
                    pinyin = mArrayList.get(position - 1).getPinyin();
                else
                    pinyin = mArrayList.get(position).getPinyin();
                //sideBar.setCurChooseTitle(String.valueOf(pinyin.charAt(0)));
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    // 退出编辑状态，显示退出动画
    public void exitEidtMode() {
//        if (mSearchAddList != null) {
//            mSearchAddList.clear();
//        }
//        iv_randPlay.setEnabled(true);
//        mAdapter.changeEidtMode(false);
//        mAdapter.setNeedout();
//        if (!mHbActionBar.hbIsExitEditModeAnimRunning()) {
//            mHbActionBar.setShowBottomBarMenu(false);
//            mHbActionBar.showActionBarDashBoard();
//        }
//        ((TextView) btn_menuAll).setText(getResources().getString(R.string.selectAll));
//        mHbHandler.sendEmptyMessageDelayed(MSG_SHOW_ANIMATION, 500);
//        mTrackList.hbSetNeedSlideDelete(true);
//        mAdapter.notifyDataSetChanged();
    }

    // 动态更改actionbar 显示全选/反选
    public void changeMenuState() {
//        if (mAdapter.getCheckedCount() == mAdapter.getCount() && mAdapter.getCheckedCount() > 0) {
//            ((TextView) btn_menuAll).setText(getResources().getString(R.string.selectAllNot));
//        } else {
//            ((TextView) btn_menuAll).setText(getResources().getString(R.string.selectAll));
//        }
//        if (fromAdd) {
//            if (mAddList.size() == 0) {
//                mHbActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(0, false);
//            } else {
//                mHbActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(0, true);
//            }
//            tv_menu.setText(getString(R.string.hb_add_playlist_song_num, mAddList.size()));
//
//        } else {
//            if (mAdapter.getCheckedCount() == 0 && (mSearchAddList == null || mSearchAddList.size() == 0)) {
//                mHbActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(0, false);
//                mHbActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(1, false);
//            } else {
//                mHbActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(0, true);
//                mHbActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(1, true);
//            }
//        }
    }

    // 动态更改actionbar 显示全选/反选
    public void changeMenuState(int state, long songId) {
        if (fromAdd) {
            if (state == 1) {
                if (!mAddList.contains(String.valueOf(songId)))
                    mAddList.add(String.valueOf(songId));
            } else if (state == 0) {
                mAddList.remove(String.valueOf(songId));
            } else if (state == 2) {

                // 全选
                for (int i = 0; i < mArrayList.size(); i++) {
                    if (!mAddList.contains(String.valueOf(mArrayList.get(i).getSongId())))
                        mAddList.add(String.valueOf(mArrayList.get(i).getSongId()));
                }
            } else if (state == 3) {

                // 反选
                for (int i = 0; i < mArrayList.size(); i++) {
                    mAddList.remove(String.valueOf(mArrayList.get(i).getSongId()));
                }
            }
        }
        changeMenuState();
    }

    // 删除选中的所有歌曲
    public void deleteSongs() {
        mFinished = false;
        if (mAlertDialog != null && !mAlertDialog.isShowing()) {
            int num = mAdapter.getCheckedCount();
            String title = getResources().getString(R.string.deleteMessage);
            if(num>1){
                //title= getResources().getString(R.string.deleteMessage_num, mAdapter.getCheckedCount());
            }
            mAlertDialog.setTitle(title);
            mAlertDialog.show();
        }
    }

//
//    @Override
//    public void hideSearchviewLayout() {
//        super.hideSearchviewLayout();
//        setPlayAnimation();
//    }
    /**
     * 设置播放动画
     */
    private void setPlayAnimation() {
        if (actionBar_play == null || operatingAnim == null) {
            return;
        }
        try {
            if (MusicUtils.sService != null) {
                if (MusicUtils.sService.isPlaying() && mAdapter.getEidtMode() == false) {
                    if (!isPlaying) {
                        actionBar_play.startAnimation(operatingAnim);
                        actionBar_play.setBackgroundResource(android.R.color.transparent);
                        isPlaying = true;
                    }
                } else if (isPlaying) {
                    actionBar_play.clearAnimation();
                    actionBar_play.setBackgroundResource(R.drawable.hb_left_bar_clicked);
                    isPlaying = false;
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            if (isPlaying) {
                actionBar_play.clearAnimation();
                actionBar_play.setBackgroundResource(R.drawable.hb_left_bar_clicked);
                isPlaying = false;
            }
        }
    }

    // 监听播放状态的变化
    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
                setPlayAnimation();
            } else if (action.equals(MediaPlaybackService.META_CHANGED)) {
                try {
                    if (MusicUtils.sService != null) {
                        if (MusicUtils.sService.getAudioId() == -1) {
                            if (fromShuffl > 0) {
                                fromShuffl--;
                            }
                            return;
                        }
                        for (int i = 0; i < mArrayList.size(); i++) {
                            if ((mArrayList.get(i).getSongId()) == MusicUtils.sService.getAudioId()) {
                                final int j = i;
                                if (fromShuffl > 0) {
                                    mAdapter.setCurrentPosition(i);
                                    mAdapter.notifyDataSetChanged();
                                    if (j == 0) {
                                        mTrackList.setSelectionFromTop(j + 1, 0);
                                    } else {
                                        mTrackList.setSelectionFromTop(j + 1, getResources().getDimensionPixelSize(R.dimen.hb_playmode_height));
                                    }
                                    fromShuffl--;
                                } else if (mArrayList.get(mAdapter.getCurrentPosition()).getSongId() != mArrayList.get(i).getSongId() && fromShuffl == 0 && mFinished) {
                                    startPlayAnimation(i + 1, false);
                                } else {
                                    mAdapter.setCurrentPosition(i);
                                    mAdapter.notifyDataSetChanged();
                                }
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }
    };

    // 注册监听播放器状态更改的广播
    private void registerStateChangeReceiver() {
        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(MediaPlaybackService.META_CHANGED);
        registerReceiver(mStatusListener, new IntentFilter(f));
    }

    private void goEditMode(int position) {
        iv_randPlay.setEnabled(false);
        if (isPlaying && actionBar_play != null) {
            actionBar_play.clearAnimation();
            actionBar_play.setBackgroundResource(R.drawable.hb_left_bar_clicked);
            isPlaying = false;
        }
//        if (mAdapter.getEidtMode() == false) {
//            if (!mHbActionBar.hbIsEntryEditModeAnimRunning()) {
//                mHbActionBar.setShowBottomBarMenu(true);
//                if (position != -1)
//                    mHbActionBar.showActionBarDashBoard();
//                else
//                    mHbActionBar.showActionBottomeBarMenu();
//            }
//            selectAllNot();
//            if (!fromAdd)
//                mAdapter.setNeedin();
//            if (position != -1)
//                mAdapter.setItemChecked(position - 1, true);
//            mTrackList.hbSetNeedSlideDelete(false);
//            mAdapter.changeEidtMode(true);
//        }
        if (fromAdd) {
            changeMenuState();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
//        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK && (inSearch)) {
//            inSearch = false;
//            hideSearchviewLayout();
//            changeMenuState();
//            mHbActionBar.getHbActionBarSearchView().getQueryTextView().setText("");
//            mTrackList.setVisibility(View.VISIBLE);
//            searchView.setVisibility(View.VISIBLE);
//            return true;
//        } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN && fromAdd) {
//            finishWithDataResult();// add by chenhl 20140523
//        } else if (mHbActionBar.hbIsExitEditModeAnimRunning() || mHbActionBar.hbIsEntryEditModeAnimRunning()) {
//            return true;
//        } else if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK && (mAdapter.getEidtMode()) && !fromAdd) {
//            exitEidtMode();
//            return true;
//        } else if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK && mTrackList.hbIsRubbishOut()) {
//            mTrackList.hbSetRubbishBack();
//            return true;
//        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHbHandler.removeCallbacksAndMessages(null);
        mTrackList.setAdapter(null);
        mAdapter = null;
        mSortTask.cancel(true);
        unregisterReceiverSafe(mStatusListener);
    }

    private void unregisterReceiverSafe(BroadcastReceiver receiver) {
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
        }
    }

    // 删除选中的歌曲
    private void deleteItem(int position) {
//        new AlertDialog.Builder(this).setTitle(R.string.delete)
//                // .setIcon(android.R.drawable.ic_dialog_info)
//                .setMessage(getString(R.string.deleteMessage, 1)).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                showDeleteAnimation();
//            }
//        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                // TODO Auto-generated method stub
//                //mTrackList.hbSetRubbishBack();
//            }
//        }).show();
    }

    private void finishWithDataResult() {
        Intent tIntent = new Intent();
//        tIntent.putExtra(HBNewPlayListActivity.EXTR_RESULT_DATA, mAddList);
//        tIntent.putExtra(HBNewPlayListActivity.EXTR_RESULT_MODE, 0);
        setResult(RESULT_OK, tIntent);
        finish();
    }

    /**
     * 回到歌单页面
     */
    private void gobackToSongSingle() {

        Intent intent = new Intent(this, HBSongSingle.class);
        intent.putExtra(HBSongSingle.EXTR_PLAYLIST_INFO, playlistInfo);
        startActivity(intent);
        finish();
   //     HBMusicActivityManiger.getInstance().exit();
    }

    class GetSearchTask extends AsyncTask<String, String, String> {
        private boolean flag = false;

        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            if (params[0] == null || TextUtils.isEmpty(params[0].trim())) {
                flag = true;
                return null;
            }
            tracklist.clear();
            artistlist.clear();
            String filter = params[0];
            if (filter == null) {
                filter = "";
            }

            if (mPathsXml == null) {
                mPathsXml = HBMusicUtil.doParseXml(HBTrackBrowserActivity.this, "paths.xml");
            }

            if (!fromAdd) {
                Cursor cursor = doSearchArtist(filter);
                if (cursor != null) {
                    for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {

                        String path = cursor.getString(4);
                        path = path.substring(0, path.lastIndexOf("/"));
                        if (mPathsXml.contains(path)) {
                            continue;
                        }

                        SearchItem searchItem = new SearchItem(cursor.getLong(0), null, "", "", "", "", cursor.getString(1), "artist");
                        searchItem.mSimilarity = HBMusicUtil.getSimilarity(searchItem.getArtistName().toUpperCase(), filter.toUpperCase());
                        searchItem.mAlbumCount = cursor.getInt(2);
                        searchItem.mSongCount = cursor.getInt(3);
                        searchItem.mPinYin = MusicUtils.getSpell(searchItem.getArtistName());
                        artistlist.add(searchItem);
                    }
                    cursor.close();
                }
            }
            Cursor cursor2 = doSearchTrack(filter);
            if (cursor2 != null) {
                for (cursor2.moveToFirst(); !cursor2.isAfterLast(); cursor2.moveToNext()) {
                    String path = cursor2.getString(4);
                    path = path.substring(0, path.lastIndexOf("/"));
                    if (mPathsXml.contains(path)) {
                        continue;
                    }
                    String imgUri = HBMusicUtil.getImgPath(getApplication(), HBMusicUtil.MD5(cursor2.getString(3) + cursor2.getString(1) + cursor2.getString(2)));
                    SearchItem searchItem = new SearchItem(cursor2.getLong(0), null, cursor2.getString(3), cursor2.getString(4), imgUri, cursor2.getString(2), cursor2.getString(1), "");
                    searchItem.mSimilarity = HBMusicUtil.getSimilarity(searchItem.getTitle().toUpperCase(), filter.toUpperCase());
                    searchItem.mPinYin = MusicUtils.getSpell(searchItem.getTitle());
                    tracklist.add(searchItem);
                }
                cursor2.close();
            }
            if (artistlist.size() > 0)
                Collections.sort(artistlist, new Comparator<SearchItem>() {

                    @Override
                    public int compare(SearchItem lhs, SearchItem rhs) {
                        if (lhs.mSimilarity > rhs.mSimilarity) {
                            return -1;
                        } else if (lhs.mSimilarity < rhs.mSimilarity) {
                            return 1;
                        } else {
                            // TODO Auto-generated method stub
                            if (lhs.mPinYin.charAt(0) == rhs.mPinYin.charAt(0) && (65 > lhs.getArtistName().toUpperCase().charAt(0) || lhs.getArtistName().toUpperCase().charAt(0) > 90)
                                    && (rhs.getArtistName().toUpperCase().charAt(0) <= 90 && rhs.getArtistName().toUpperCase().charAt(0) >= 65)) {
                                return 1;

                            } else if (lhs.mPinYin.charAt(0) == rhs.mPinYin.charAt(0) && (65 > rhs.getArtistName().toUpperCase().charAt(0) || rhs.getArtistName().toUpperCase().charAt(0) > 90)
                                    && (lhs.getArtistName().toUpperCase().charAt(0) <= 90 && lhs.getArtistName().toUpperCase().charAt(0) >= 65)) {
                                return -1;
                            }
                        }
                        return lhs.mPinYin.compareTo(rhs.mPinYin);
                    }
                });
            if (tracklist.size() > 0)
                Collections.sort(tracklist, new Comparator<SearchItem>() {

                    @Override
                    public int compare(SearchItem lhs, SearchItem rhs) {
                        // TODO Auto-generated method stub
                        if (lhs.mSimilarity > rhs.mSimilarity) {
                            return -1;
                        } else if (lhs.mSimilarity < rhs.mSimilarity) {
                            return 1;
                        } else {
                            if (lhs.mPinYin.charAt(0) == rhs.mPinYin.charAt(0) && (65 > lhs.getTitle().toUpperCase().charAt(0) || lhs.getTitle().toUpperCase().charAt(0) > 90)
                                    && (rhs.getTitle().toUpperCase().charAt(0) <= 90 && rhs.getTitle().toUpperCase().charAt(0) >= 65)) {
                                return 1;

                            } else if (lhs.mPinYin.charAt(0) == rhs.mPinYin.charAt(0) && (65 > rhs.getTitle().toUpperCase().charAt(0) || rhs.getTitle().toUpperCase().charAt(0) > 90)
                                    && (lhs.getTitle().toUpperCase().charAt(0) <= 90 && lhs.getTitle().toUpperCase().charAt(0) >= 65)) {
                                return -1;
                            }
                        }
                        return lhs.mPinYin.compareTo(rhs.mPinYin);
                    }
                });
            if (artistlist.size() > 0)
                artistlist.get(0).mTag = true;
            if (tracklist.size() > 0)
                tracklist.get(0).mTag = true;
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            synchronized (this) {
                if (mSearchAdapter != null) {
                    mSearchlist.clear();
                    mSearchlist.addAll(artistlist);
                    mSearchlist.addAll(tracklist);

                    mSearchListView.setVisibility(View.VISIBLE);
                    //tv_sidebar.setVisibility(View.GONE);
                    mTrackList.setVisibility(View.GONE);
                    //sideBar.setVisibility(View.GONE);
                    if (mAdapter.getEidtMode()) {
                        mSearchAdapter.setEidtMode(true);
                    }
                    mSearchAdapter.notifyDataSetChanged();
                    mSearchListView.setSelection(0);
                    mSearchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            mSearchListView.requestFocus();
                            if (!((SearchItem) mSearchAdapter.getItem(position)).mMimeType.equals("artist")) {
                                ArrayList<HBListItem> list = new ArrayList<HBListItem>();
                                SearchItem item = ((SearchItem) mSearchAdapter.getItem(position));
                                long songId = item.getSongId();

                                HBListItem listItem = new HBListItem(songId, item.getTitle(), null, item.getAlbumName(), item.getAlbumId(), item.getArtistName(), 0, null, null, null, -1);

                                listItem.setItemPicSize(mWidth, mWidth);
                                list.add(listItem);
                                MusicUtils.playAll(HBTrackBrowserActivity.this, list, 0, 0);
                                HBMusicUtil.setCurrentPlaylist(HBTrackBrowserActivity.this, -1);
                                mSearchAdapter.notifyDataSetChanged();
                            } else {
                                int artistofalbums = ((SearchItem) mSearchAdapter.getItem(position)).mAlbumCount;
                                int artistofsongs = ((SearchItem) mSearchAdapter.getItem(position)).mSongCount;

                                if (artistofalbums == 1) {
                                    Intent intent = new Intent(HBTrackBrowserActivity.this, AlbumDetailActivity.class);
                                    intent.putExtra(Globals.KEY_ARTIST_ID, String.valueOf(((SearchItem) mSearchAdapter.getItem(position)).getSongId()));
                                    intent.putExtra(Globals.KEY_ARTIST_NAME, ((SearchItem) mSearchAdapter.getItem(position)).getArtistName());
                                    intent.putExtra("artistofalbum", String.valueOf(artistofalbums));
                                    intent.putExtra("artistoftrack", String.valueOf(artistofsongs));
                                    startActivityForResult(intent, Globals.REQUEST_CODE_BROWSER);
                                } else {
                                    Intent intent = new Intent(HBTrackBrowserActivity.this, AlbumListActivity.class);
                                    // 可以获得歌手id 专辑数 歌总数
                                    intent.putExtra(Globals.KEY_ARTIST_ID, String.valueOf(((SearchItem) mSearchAdapter.getItem(position)).getSongId()));
                                    intent.putExtra(Globals.KEY_ARTIST_NAME, ((SearchItem) mSearchAdapter.getItem(position)).getArtistName());
                                    intent.putExtra("artistofalbum", String.valueOf(artistofalbums));
                                    intent.putExtra("artistoftrack", String.valueOf(artistofsongs));
                                    startActivityForResult(intent, Globals.REQUEST_CODE_BROWSER);
                                }
                            }
                        }
                    });
                }
                if (flag) {
                    mSearchListView.setVisibility(View.GONE);
                    // mHeaderHideView.setVisibility(View.GONE);
                    mTrackList.setVisibility(View.VISIBLE);
                    //sideBar.setVisibility(View.VISIBLE);
                }

            }
            super.onPostExecute(result);
        }

    }

    public synchronized void shufflePlay() {
        fromShuffl++;
        MusicUtils.playAll(HBTrackBrowserActivity.this, mArrayList, 0, 3);
        HBMusicUtil.setCurrentPlaylist(HBTrackBrowserActivity.this, -1);
//        if (mTrackList.hbIsRubbishOut()) {
//            mTrackList.hbSetRubbishBackNoAnim();
//            sideBar.setVisibility(View.VISIBLE);
//        }
    }

    public void showNavtitle(boolean flag) {
        if (flag) {
            mTrackList.setVisibility(View.GONE);
            //sideBar.setVisibility(View.GONE);
            findViewById(R.id.hb_no_songs).setVisibility(View.VISIBLE);
        } else {

            mTrackList.setVisibility(View.VISIBLE);
            //sideBar.setVisibility(View.VISIBLE);
            findViewById(R.id.hb_no_songs).setVisibility(View.GONE);
        }
    }

    private void selectAll() {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            mAdapter.setItemChecked(i, true);
        }
    }

    private void selectAllNot() {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            mAdapter.setItemChecked(i, false);
        }
    }

    public void setItemChecked(long i, boolean flag) {
        boolean inAdapter = false;
        for (int index = 0; index < mArrayList.size(); index++) {
            if (mArrayList.get(index).getSongId() == i) {
                mAdapter.setItemChecked(index, flag);
                inAdapter = true;
                break;
            }
        }
        if (inAdapter)
            return;
        if (flag) {
            mSearchAddList.add(String.valueOf(i));
        } else {
            mSearchAddList.remove(String.valueOf(i));
        }
    }

    public void setArtistItemChecked(long i, boolean flag) {
        for (int index = 0; index < mArrayList.size(); index++) {
            if (mArrayList.get(index).getArtistId() == i) {
                mAdapter.setItemChecked(index, flag);
            }
        }
        getSongByArtist(i, flag);
    }

    public boolean getItemCheckedSongId(long songId) {
        if (fromAdd) {
            for (int index = 0; index < mAddList.size(); index++) {
                if (Long.valueOf(mAddList.get(index)) == songId) {
                    return true;
                }
            }
        } else {
            for (int index = 0; index < mArrayList.size(); index++) {
                if (mArrayList.get(index).getSongId() == songId) {
                    return mAdapter.isItemChecked(index);
                }
            }
            for (int i = 0; i < mSearchAddList.size(); i++) {
                if (Long.valueOf(mSearchAddList.get(i)) == songId) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean getArtistItemCheckedSongId(long songId, int count) {
        int number = 0;
        for (int index = 0; index < mArrayList.size(); index++) {
            if (mArrayList.get(index).getArtistId() == songId) {
                if (mAdapter.isItemChecked(index)) {
                    number++;
                }
            }
        }
        if (number == count)
            return true;
        else
            return false;
    }

//    @Override
//    public boolean quit() {
//        // TODO Auto-generated method stub
//        if (mAdapter == null) {
//            return false;
//        }
//        mSearchListView.setVisibility(View.GONE);
//        sideBar.setVisibility(View.VISIBLE);
//        if (mAdapter.getEidtMode()) {
//            if (!mHbActionBar.hbIsExitEditModeAnimRunning()) {
//                mHbActionBar.setShowBottomBarMenu(true);
//                mHbActionBar.showActionBottomeBarMenu();
//            }
//        }
//        if (fromAdd) {
//            if (!mHbActionBar.hbIsExitEditModeAnimRunning()) {
//                mHbActionBar.setShowBottomBarMenu(true);
//                mHbActionBar.showActionBottomeBarMenu();
//            }
//        }
//        inSearch = false;
//        mSearchAdapter.setEidtMode(false);
//        return false;
//    }

    @Override
    public void onMediaDbChange(boolean selfChange) {
        // TODO Auto-generated method stub
        // if (mAdapter != null) {
        // if (mSortTask.getStatus() == Status.FINISHED) {
        // mSortTask = new SortTask();
        // mSortTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        // }
        // }
    }

    @Override
    public void OnDeleteFileSuccess() {
        // TODO Auto-generated method stub
        if (mAdapter.getEidtMode()) {
            exitEidtMode();
        }
        mFinished = true;
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if (mAdapter != null&&!inSearch) {
            if (mSortTask.getStatus() == AsyncTask.Status.FINISHED) {
                mSortTask = new SortTask();
                mSortTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            mAdapter.notifyDataSetChanged();
        }
        //mTrackList.hbOnResume();
        isStop = false;
    }

    @Override
    public void onAttachedToWindow() {
        // TODO Auto-generated method stub
        mTrackList.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // TODO Auto-generated method stub
                if (mAdapter == null) {
                    return;
                }
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING || scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    ImageLoader.getInstance().pause();
                } else {
                    ImageLoader.getInstance().resume();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                mHbHandler.removeMessages(MSG_UPDATE_LISTVIEW);
                mHbHandler.obtainMessage(MSG_UPDATE_LISTVIEW).sendToTarget();
            }
        });
        if (fromAdd && needShowSeleted)
            goEditMode(-1);
        super.onAttachedToWindow();
    }

    private long time = 0;

    public void startPlayAnimation(final int arg2, final boolean flag) {
        if (aima != null && aima.isStarted()) {
            mAdapter.setCurrentPosition(arg2 - 1);
            aima.end();
        }
        int[] location = new int[2];
        int[] location1 = new int[2];
        int[] location2 = new int[2];
        int distance = 0; // 移动距离
        mTrackList.getLocationInWindow(location);
        int currentPosition = mAdapter.getCurrentPosition();
        View arg1 = mTrackList.getChildAt(arg2 - mTrackList.getFirstVisiblePosition());
        if (arg1 == null) {
            mAdapter.setCurrentPosition(arg2 - 1);
            mTrackList.invalidateViews();
            if (flag)
                playMusic(mArrayList, arg2 - 1);
            return;
        }

        arg1.getLocationInWindow(location1);
        int startx = location1[0];
        int starty = location1[1];
        flystartPoint = new int[] { startx, starty };
        if (arg2 == 1) {
            flystartPoint[1] += getResources().getDimension(R.dimen.hb_playmode_height);
        }

        if (currentPosition < 0) {
            // 无动画
            mPlaySelect.postDelayed(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if (isStop) {
                        playMusic(mArrayList, arg2 - 1);
                        return;
                    }
                    startFly();
                    playMusic(mArrayList, arg2 - 1);
                    mAdapter.setCurrentPosition(arg2 - 1);
                    mAdapter.notifyDataSetChanged();
                }
            }, 100);
            if (isStop) {
                playMusic(mArrayList, arg2 - 1);
                mAdapter.setCurrentPosition(arg2 - 1);
                mAdapter.notifyDataSetChanged();
            }
            return;
        } else if (currentPosition < mTrackList.getFirstVisiblePosition()) {
            // 从最上面飞进来
            mPlaySelect.setY(-mPlaySelect.getHeight());
            distance = location1[1] - location[1] + mPlaySelect.getHeight();
        } else if (currentPosition > mTrackList.getLastVisiblePosition()) {
            // 从最下面飞进来
            mPlaySelect.setY(mTrackList.getBottom());
            distance = mTrackList.getHeight() - location1[1] + location[1];
        } else {
            // 具体位置飞进
            View view = mTrackList.getChildAt(currentPosition - mTrackList.getFirstVisiblePosition());
            view.getLocationInWindow(location2);
            if (currentPosition == 0)
                mPlaySelect.setY(location2[1] - location[1] + getResources().getDimension(R.dimen.hb_search_height) + getResources().getDimension(R.dimen.hb_playmode_height));
            else if (currentPosition == 1) {
                mPlaySelect.setY(location2[1] - location[1] + getResources().getDimension(R.dimen.song_itemheight) + getResources().getDimension(R.dimen.hb_playmode_height));
            } else
                mPlaySelect.setY(location2[1] - location[1] + getResources().getDimension(R.dimen.song_itemheight));
            distance = Math.abs(location2[1] - location1[1]);
        }
        if (arg2 == 1)
            aima = ObjectAnimator.ofFloat(mPlaySelect, "y", location1[1] - location[1] + getResources().getDimension(R.dimen.hb_playmode_height));
        else
            aima = ObjectAnimator.ofFloat(mPlaySelect, "y", location1[1] - location[1]);
        aima.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator arg0) {
                mPlaySelect.setVisibility(View.VISIBLE);
                mAdapter.setCurrentPosition(-2);
                mTrackList.invalidateViews();
            }

            @Override
            public void onAnimationRepeat(Animator arg0) {

            }

            @Override
            public void onAnimationEnd(Animator arg0) {
                mHbHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        if (mAdapter == null) {
                            return;
                        }
                        mAdapter.setCurrentPosition(arg2 - 1);
                        mTrackList.invalidateViews();
                        mPlaySelect.setVisibility(View.GONE);
                        if (flag)
                            playMusic(mArrayList, arg2 - 1);

                    }
                });
                mPlaySelect.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        startFly();
                    }
                }, 10);
            }

            @Override
            public void onAnimationCancel(Animator arg0) {

            }

        });

        if (distance < 300) {
            aima.setDuration(150);
        } else {
            aima.setDuration(200);
        }
        aima.start();
    }

    public void playMusic(final ArrayList<HBListItem> arrayList, final int position) {
        MusicUtils.playAll(HBTrackBrowserActivity.this, arrayList, position, 0);
        HBMusicUtil.setCurrentPlaylist(HBTrackBrowserActivity.this, -1);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
        setPlayAnimation();
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
//        if (mTrackList.hbIsRubbishOut()) {
//            mTrackList.hbSetRubbishBack();
//        }
        if (isPlaying && actionBar_play != null) {
            actionBar_play.clearAnimation();
            actionBar_play.setBackgroundResource(R.drawable.hb_left_bar_clicked);
            isPlaying = false;
        }
    }

    public void getSeletedItems() {
        for (int i = 0; i < mArrayList.size(); i++) {
            if (mAddList.contains(String.valueOf(mArrayList.get(i).getSongId()))) {
                mAdapter.setItemChecked(i, true);
            }
        }
        changeMenuState();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
       // mTrackList.hbOnPause();
        //dialog.setVisibility(View.GONE);
        HBMusicUtil.clearflyWindown();
        isStop = true;
    }

    public void updataSongCount(int count) {
        //tv_songCount.setText(getResources().getString(R.string.number_track, count));
    }

    public Cursor doSearchTrack(String searchString) {
        String[] searchCols = new String[] { android.provider.BaseColumns._ID, MediaStore.Audio.Artists.ARTIST, MediaStore.Audio.Albums.ALBUM, MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA };
        searchString = searchString.replace("\\", "\\\\");
        searchString = searchString.replace("%", "\\%");
        searchString = searchString.replace("_", "\\_");
        String wildcardWords = "%" + searchString + "%";
        String where = MediaStore.Audio.Media.TITLE + " LIKE ? AND " + Globals.QUERY_SONG_FILTER;
        Cursor cursor = getContentResolver().query(uri, searchCols, where, new String[] { wildcardWords }, null);
        return cursor;

    }

    public Cursor doSearchArtist(String searchString) {
        searchString = searchString.replace("\\", "\\\\");
        searchString = searchString.replace("%", "\\%");
        searchString = searchString.replace("_", "\\_");
        String wildcardWords = "%" + searchString + "%";
        StringBuilder where = new StringBuilder();
        where.append(MediaStore.Audio.Media.ARTIST + " LIKE ? AND " + Globals.QUERY_SONG_FILTER + ") GROUP BY (" + MediaStore.Audio.Media.ARTIST_ID);
        Cursor cursor = getContentResolver().query(uri,
                new String[] { MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.ARTIST, "COUNT(DISTINCT album_id)", "COUNT(*)", MediaStore.Audio.Media.DATA }, where.toString(),
                new String[] { wildcardWords }, null);
        return cursor;
    }

    public void setRubbishBack() {
//        if (mTrackList != null && mTrackList.hbIsRubbishOut()) {
//            mTrackList.hbSetRubbishBackNoAnim();
//        }
    }

    public void getSongByArtist(long i, boolean flag) {
        StringBuilder where = new StringBuilder();
        where.append(Globals.QUERY_SONG_FILTER);
        where.append(" and " + MediaStore.Audio.Media.ARTIST_ID + "=" + i);
        Cursor cursor = HBTrackBrowserActivity.this.getContentResolver().query(uri, new String[] { MediaStore.Audio.Media._ID }, where.toString(), null, null);
        if (cursor != null) {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                boolean exist = false;
                for (int index = 0; index < mList.size(); index++) {
                    if (mList.get(index).getSongId() == cursor.getLong(0)) {
                        exist = true;
                        break;
                    }
                }
                if (exist)
                    return;
                if (flag && !exist) {
                    mSearchAddList.add(String.valueOf(cursor.getLong(0)));
                } else {
                    mSearchAddList.remove(String.valueOf(cursor.getLong(0)));
                }
            }

        }
    }

    private void startFly() {
        if (isStop || actionBar_play == null || inSearch)
            return;
        if (flyendPoint[0] == 0 || flyendPoint[1] == 0)
            actionBar_play.getLocationInWindow(flyendPoint);
        HBMusicUtil.startFly(HBTrackBrowserActivity.this, flystartPoint[0], flyendPoint[0], flystartPoint[1], flyendPoint[1], true);
    }
}
