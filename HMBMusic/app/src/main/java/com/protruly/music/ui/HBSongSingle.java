package com.protruly.music.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.protruly.music.MediaPlaybackService;
import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.adapter.HBSongSingleAdapter;

import com.protruly.music.util.FlowTips;
import com.protruly.music.util.FlowTips.OndialogClickListener;

import com.protruly.music.util.Globals;
import com.protruly.music.util.LogUtil;
import com.protruly.music.util.HBListItem;
import com.protruly.music.util.HBMainMenuData;
import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.util.ThreadPoolExecutorUtils;
import com.protruly.music.MusicUtils.ServiceToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import hb.widget.HbEditText;
import hb.widget.HbListView;
import hb.app.dialog.AlertDialog;


/**
 * Created by hujianwei on 17-9-4.
 */

public class HBSongSingle extends AbstractBaseActivity implements Runnable, View.OnClickListener {
    private static final String TAG = "HBSongSingle";
    private static final int PLAY_BUTTON = 0;

    // 播放按钮
    private View playView;

    // 播放按钮动画
    private Animation operatingAnim;
    
    private ServiceToken mToken;

    // 动画是否在运行
    private boolean isPlaying = false;
    private HbListView mHbListView;
    
    //private HbActionBar mHbActionBar;

    private boolean isEditMode = false;
    private boolean isRenameMode = false;

    private View mHeadView;
    private HBSongSingleAdapter mHBSongSingleAdapter;
    private int deleteId = -1;
    private ArrayList<HBListItem> songList = new ArrayList<HBListItem>();
    private FrameLayout addButton, editButton, deleteButton;
    private Button playButton;
    private ImageView renameButton, noSongRenameButton;
    private TextView mTitle, mSongSize, noSongTitle;
    private HbEditText renameEdit, nosongEdit;

    private static final int HB_ID_DELETE_SINGLE = 0;
    private static final int HB_ID_DELETE_SONGS = 1;

    public static final String EXTR_PLAYLIST_INFO = "playlist_info";
    public static final String EXTR_PLAYLIST_START_MODE = "playlist_start_mode";

    private HBMainMenuData playlistInfo;
    private String[] mPlaylistMemberCols, mCursorCols;
    private int mStartMode = 0; // 0为自定义歌单， 1为最近添加歌曲， 2为我喜欢的歌曲
    private boolean isQuery = false;
    private ImageView mPlaySelect;
    private boolean isFromBroadcast = false;
    private ColorStateList oldColer;
    private View noSongHeadView;

    private int[] flyendPoint = new int[2];
    private int[] flystartPoint = new int[2];
    private boolean isStop; 
    private List<String> mPathsXml = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHbContentView(R.layout.hb_songsingle_main);
        mToken = MusicUtils.bindToService(this);
        mPathsXml = HBMusicUtil.doParseXml(this, "paths.xml");

        Intent intent = getIntent();
        if (intent != null) {
            playlistInfo = (HBMainMenuData) intent.getSerializableExtra(EXTR_PLAYLIST_INFO);
            mStartMode = intent.getIntExtra(EXTR_PLAYLIST_START_MODE, 0);
        } else {
            finish();
            return;
        }
        mPlaylistMemberCols = new String[] { MediaStore.Audio.Playlists.Members.AUDIO_ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Playlists.Members.PLAY_ORDER, MediaStore.Audio.Media.IS_MUSIC };
        mCursorCols = new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DURATION };
        initview();
        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.FAVORITE_CHANGED);
        //registerReceiver(mStatusListener, new IntentFilter(f));

    }

    private ThreadPoolExecutor executor = ThreadPoolExecutorUtils.getThreadPoolExecutor().getExecutor();

    private void initview() {
        
        //setHbBottomBarMenuCallBack(hbMenuCallBack);
        try {
            //mHbActionBar = getHbActionBar();// 获取actionbar
            //mHbActionBar.addItem(R.drawable.song_playing, PLAY_BUTTON, null);
            //playView = mHbActionBar.getItem(PLAY_BUTTON).getItemView();
            operatingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate_anim);
            LinearInterpolator lin = new LinearInterpolator();
            operatingAnim.setInterpolator(lin);
            //mHbActionBar.setOnHbActionBarListener(mOnHbActionBarItemClickListener);

            // 添加底部删除
            if (mStartMode == 0) {
                //mHbActionBar.setTitle(R.string.hb_song_single);
                //mHbActionBar.initActionBottomBarMenu(R.menu.menu_songsingle_bottombar, 1);
            } else {
                if (mStartMode == 1) {
                    //mHbActionBar.setTitle(R.string.recently_added_songs);
                    //mHbActionBar.initActionBottomBarMenu(R.menu.menu_songedit, 2);
                } else {
                    //mHbActionBar.setTitle(R.string.my_favorite_songs);
                    //mHbActionBar.initActionBottomBarMenu(R.menu.menu_songedit, 2);
                }
            }
            initActionbarMenu();
            //oldColer = ((TextView) mHbActionBar.getSelectRightButton()).getTextColors();
            mHbListView = (HbListView) findViewById(R.id.hb_id_song_list);
            mPlaySelect = (ImageView) findViewById(R.id.hb_song_selected);
            // 给listview 添加head
            mHeadView = LayoutInflater.from(this).inflate(R.layout.hb_songsingle_headview, null);
            mTitle = (TextView) mHeadView.findViewById(R.id.hb_single_title);
            mSongSize = (TextView) mHeadView.findViewById(R.id.hb_single_song_num);

            mHbListView.addHeaderView(mHeadView);

            mHBSongSingleAdapter = new HBSongSingleAdapter(this, playlistInfo.getPlaylistId(), mStartMode);
            //mHBSongSingleAdapter.setHandler(mHandler);
            mHbListView.setAdapter(mHBSongSingleAdapter);
            //mHbListView.hbSetNeedSlideDelete(true);
            //mHbListView.hbSetHbBackOnClickListener(mHbBackOnClickListener);
            //mHbListView.setOnItemClickListener(mOnItemClickListener);
            mHbListView.setOnItemLongClickListener(mOnItemLongClickListener);
            //mHbListView.hbSetDeleteItemListener(mHBDeleteItemListener); // item删除监听
            //mHbListView.hbEnableSelector(false);

            executor.execute(this);
            // 开启扫描歌单数据库

            // 设置添加、删除、编辑、播放歌单监听
            addButton = (FrameLayout) mHeadView.findViewById(R.id.hb_id_add_song);
            addButton.setOnClickListener(this);
            editButton = (FrameLayout) mHeadView.findViewById(R.id.hb_id_eidt_song);
            editButton.setOnClickListener(this);
            deleteButton = (FrameLayout) mHeadView.findViewById(R.id.hb_id_delete_single);
            deleteButton.setOnClickListener(this);
            playButton = (Button) mHeadView.findViewById(R.id.hb_id_play_single);
            playButton.setOnClickListener(this);

            renameEdit = (HbEditText) mHeadView.findViewById(R.id.hb_single_rename_edit);
            renameButton = (ImageView) mHeadView.findViewById(R.id.hb_single_rename);
            mHeadView.findViewById(R.id.hb_single_rename_bg).setOnClickListener(this);
            mHeadView.findViewById(R.id.hb_single_rename_bg).setEnabled(false);

            renameEdit.setText(playlistInfo.getName());
            renameEdit.setSelectAllOnFocus(true);
            renameEdit.clearFocus();

            // 隐藏head 底部菜单
            noSongHeadView = findViewById(R.id.hb_headview);

            if (mStartMode != 0) {
                mHeadView.findViewById(R.id.hb_single_rename_bg).setVisibility(View.GONE);
                mHeadView.findViewById(R.id.hb_headbar_menu).setVisibility(View.GONE);
                noSongHeadView.findViewById(R.id.hb_single_rename_bg).setVisibility(View.GONE);
                noSongHeadView.findViewById(R.id.hb_headbar_menu).setVisibility(View.GONE);
            } else {
                mHeadView.findViewById(R.id.hb_single_rename_bg).setVisibility(View.VISIBLE);
                noSongHeadView.findViewById(R.id.hb_single_rename_bg).setVisibility(View.VISIBLE);
            }
            // 无歌曲页面
            noSongHeadView.findViewById(R.id.hb_id_add_song).setOnClickListener(this);
            noSongHeadView.findViewById(R.id.hb_id_eidt_song).setOnClickListener(this);
            noSongHeadView.findViewById(R.id.hb_id_delete_single).setOnClickListener(this);
            noSongHeadView.findViewById(R.id.hb_id_play_single).setOnClickListener(this);
            noSongHeadView.findViewById(R.id.hb_single_rename_bg).setOnClickListener(this);
            noSongHeadView.findViewById(R.id.hb_single_rename_bg).setEnabled(false);

            noSongTitle = (TextView) noSongHeadView.findViewById(R.id.hb_single_title);
            TextView noSongSongSize = (TextView) noSongHeadView.findViewById(R.id.hb_single_song_num);
            noSongSongSize.setText(getString(R.string.hb_num_songs_of_single, 0));
            nosongEdit = (HbEditText) (EditText) noSongHeadView.findViewById(R.id.hb_single_rename_edit);
            nosongEdit.setText(playlistInfo.getName());
            nosongEdit.setSelectAllOnFocus(true);
            nosongEdit.clearFocus();
            noSongRenameButton = (ImageView) noSongHeadView.findViewById(R.id.hb_single_rename);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * item 删除监听
     */
//    private HBDeleteItemListener mHBDeleteItemListener = new HBDeleteItemListener() {
//
//        @Override
//        public void hbDeleteItem(View arg0, final int arg1) {
//            // 删除处理
//
//            mHandler.post(new Runnable() {
//
//                @Override
//                public void run() {
//                    long songId = songList.get(arg1 - 1).getSongId();
//                    final long[] list = new long[1];
//                    list[0] = songId;
//                    if (mStartMode == 1) {
//                        MusicUtils.deleteTracks(HBSongSingle.this, list);
//                    } else if (mStartMode == 2) {
//                        // 移除我喜欢的歌曲
//                        MusicUtils.mSongDb.deleteFavoritesById(list);
//                        MusicUtils.removeTracksFromCurrentPlaylist(HBSongSingle.this, list, -2);
//                        HBMusicUtil.showDeleteToast(HBSongSingle.this, 1, mStartMode);
//                    } else {
//                        // 移除自定义歌单
//                        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistInfo.getPlaylistId());
//                        getContentResolver().delete(uri, MediaStore.Audio.Playlists.Members.AUDIO_ID + "=?", new String[] { String.valueOf(songId) });
//                        MusicUtils.removeTracksFromCurrentPlaylist(HBSongSingle.this, list, playlistInfo.getPlaylistId());
//                        HBMusicUtil.showDeleteToast(HBSongSingle.this, 1, mStartMode);
//                    }
//
//                    mHBSongSingleAdapter.deleteItem(arg1 - 1);
//                    updataSingleTitleAndSongSize(songList.size());
//
//                }
//            });
//
//        }
//
//    };

    /**
     * 初始化actionbar顶部 按钮
     */
    private void initActionbarMenu() {
//        if (mHbActionBar.getSelectLeftButton() != null) {
//            mHbActionBar.getSelectLeftButton().setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    exitEidtMode();
//                }
//            });
//        }
//
//        if (mHbActionBar.getSelectRightButton() != null) {
//            mHbActionBar.getSelectRightButton().setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    if (((TextView) mHbActionBar.getSelectRightButton()).getText().equals(getResources().getString(R.string.selectAll))) {
//                        ((TextView) mHbActionBar.getSelectRightButton()).setText(getResources().getString(R.string.selectAllNot));
//                        mHBSongSingleAdapter.selectAll();
//                        changeMenuState();
//                    } else {
//                        ((TextView) mHbActionBar.getSelectRightButton()).setText(getResources().getString(R.string.selectAll));
//                        mHBSongSingleAdapter.selectAllNot();
//                        changeMenuState();
//                    }
//                }
//            });
//        }
    }

    /**
     * 长按item响应
     */
    private AdapterView.OnItemLongClickListener mOnItemLongClickListener = new AdapterView.OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            if (!isEditMode) {
//                setEditeMode(true, arg2 - 1);
//                if (!mHbActionBar.hbIsEntryEditModeAnimRunning()) {
//                    mHbActionBar.setShowBottomBarMenu(true);
//                    mHbActionBar.showActionBarDashBoard();
//                }
//
//                changeMenuState();
            }
            return false;
        }

    };
    /**
     * 点击item响应
     */
    private AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, final int position, long arg3) {

            if (position > 0) {
                if (isOnline(position - 1)) {
                    boolean isshow = FlowTips.showPlayFlowTips(HBSongSingle.this, new OndialogClickListener() {

                        @Override
                        public void OndialogClick() {
                            startPlayAnimation(position, 0);
                        }
                    });
                    if (isshow) {
                        return;
                    }
                }
                startPlayAnimation(position, 0);
            }
        }

    };

//    private OnHbActionBarItemClickListener mOnHbActionBarItemClickListener = new OnHbActionBarItemClickListener() {
//        @Override
//        public void onHbActionBarItemClicked(int itemId) {
//            switch (itemId) {
//                case PLAY_BUTTON:
//                    Intent intent = new Intent(HBSongSingle.this, HBPlayerActivity.class);
//                    startActivity(intent);
//                    break;
//            }
//        }
//
//    };

    /**
     * 删除按钮响应
     */
//    private HbMenuBase.OnHbMenuItemClickListener hbMenuCallBack = new HbMenuBase.OnHbMenuItemClickListener() {
//        @SuppressWarnings("deprecation")
//        @Override
//        public void hbMenuItemClick(int itemId) {
//            switch (itemId) {
//                case R.id.song_backup:
//                    deleteId = -1;
//                    showDialog(HB_ID_DELETE_SONGS);
//                    break;
//                case R.id.song_add:
//                    // 添加到歌单
//                    DialogUtil.showAddDialog(HBSongSingle.this, mHBSongSingleAdapter.getCheckedId(), new DialogUtil.OnAddPlaylistSuccessListener() {
//
//                        @Override
//                        public void OnAddPlaylistSuccess() {
//                            // 添加成功退出编辑模式
//                            exitEidtMode();
//                        }
//                    });
//                    break;
//                default:
//                    break;
//            }
//        }
//    };

    /**
     * 滑动删除响应
     */
//    private HbBackOnClickListener mHbBackOnClickListener = new HbBackOnClickListener() {
//        // 点击了垃圾桶的响应事件
//        @SuppressWarnings("deprecation")
//        @Override
//        public void hbOnClick(int position) {
//            deleteId = position - 1;
//            showDialog(HB_ID_DELETE_SONGS);
//        }
//
//        // 准备滑动删除的响应事件，滑动之前允许用户做一些初始化操作
//        @Override
//        public void hbPrepareDraged(int position) {
//
//        }
//
//        // 成功拖出垃圾桶之后的响应事件
//        @Override
//        public void hbDragedSuccess(int position) {
//
//        }
//
//        // 进行了拖动垃圾桶操作，但是没有成功，比如只拖动了一点点
//        @Override
//        public void hbDragedUnSuccess(int position) {
//
//        }
//    };

    @Override
    public void onDestroy() {
        MusicUtils.unbindFromService(mToken);
        unregisterReceiver(mStatusListener);
        mHbListView.setAdapter(null);
        mHandler.removeMessages(0);
        mHandler.removeMessages(1);
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setPlayAnimation();

    }

    @Override
    protected void onStop() {
        if (isPlaying) {
            playView.clearAnimation();
            playView.setBackgroundResource(R.drawable.hb_left_bar_clicked);
            isPlaying = false;
        }
        isStop = true;
        super.onStop();
    }

    /**
     * 设置播放动画
     */
    private void setPlayAnimation() {
        try {
            if (MusicUtils.sService != null) {
                if (MusicUtils.sService.isPlaying()) {
                    if (!isPlaying && !isEditMode) {
                        playView.startAnimation(operatingAnim);
                        playView.setBackgroundResource(android.R.color.transparent);
                        isPlaying = true;
                    }
                } else if (isPlaying) {
                    playView.clearAnimation();
                    playView.setBackgroundResource(R.drawable.hb_left_bar_clicked);
                    isPlaying = false;
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            if (isPlaying) {
                playView.clearAnimation();
                playView.setBackgroundResource(R.drawable.hb_left_bar_clicked);
                isPlaying = false;
            }
        }
    }

    private boolean isThisPlaylist = false;
    private int olbPosition = -3;
    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
                setPlayAnimation();

            } else if (action.equals(MediaPlaybackService.META_CHANGED)) {

                try {
                    if (MusicUtils.sService != null && MusicUtils.sService.getAudioId() != -1) {
                        for (int i = 0; i < songList.size(); i++) {

                            if ((songList.get(i).getSongId()) == MusicUtils.sService.getAudioId()) {
                                int currentPosition = mHBSongSingleAdapter.getCurrentPlayPosition() - 1;
                                if (currentPosition < 0) {
                                    // mHBSongSingleAdapter.setSelected(i);
                                    mHbListView.invalidateViews();
                                } else if (songList.get(currentPosition).getSongId() != MusicUtils.sService.getAudioId()) {
                                    if (!isFromBroadcast) {
                                        isFromBroadcast = true;
                                        startPlayAnimation(i + 1, 0);
                                    }
                                } else if (!isfromanimation && (olbPosition != currentPosition)) {
//                                    if (mHbListView.hbIsRubbishOut()) {
//                                        mHbListView.hbSetRubbishBackNoAnim();
//                                    }
                                    mHbListView.invalidateViews();
                                }
                                olbPosition = currentPosition;
                                isfromanimation = false;
                                isThisPlaylist = true;
                                break;
                            }
                        }
                        // 防止播放其他歌单时，前一首在这个歌单里，后一首不在这个歌单时，绿色播放条异常
                        if (!isThisPlaylist) {
//                            if (mHbListView.hbIsRubbishOut()) {
//                                mHbListView.hbSetRubbishBackNoAnim();
//                            }
                            mHBSongSingleAdapter.setSelected(-3);
                            mHbListView.invalidateViews();
                        } else {
                            isThisPlaylist = false;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (action.equals(MediaPlaybackService.FAVORITE_CHANGED)) {
                if (!isQuery) {
                    executor.submit(HBSongSingle.this);
                }
            }

        }
    };

    private ObjectAnimator aima;
    private boolean isfromanimation = false;

    public void startPlayAnimation(final int arg2, final int repeat_mode) {
        if (playlistInfo == null) {
            Log.e(TAG, "startPlayAnimation playlistInfo is null");
            return;
        }
        if (aima != null && aima.isStarted()) {
            mHBSongSingleAdapter.setSelected(arg2 - 1);
            aima.end();
        }
        int[] location = new int[2];
        int[] location1 = new int[2];
        int[] location2 = new int[2];
        int distance = 0; // 移动距离
        mHbListView.getLocationInWindow(location);
        int currentPosition = mHBSongSingleAdapter.getCurrentPlayPosition();
        View arg1 = mHbListView.getChildAt(arg2 - mHbListView.getFirstVisiblePosition());

        if (arg1 == null) {
//            if (mHbListView.hbIsRubbishOut()) {
//                mHbListView.hbSetRubbishBackNoAnim();
//            }
            mHBSongSingleAdapter.setSelected(arg2 - 1);
            mHbListView.invalidateViews();
            if (!isFromBroadcast) {
                MusicUtils.playAll(HBSongSingle.this, songList, arg2 - 1, repeat_mode);
                HBMusicUtil.setCurrentPlaylist(HBSongSingle.this, playlistInfo.getPlaylistId());
            }
            isFromBroadcast = false;
            return;
        }
        arg1.getLocationInWindow(location1);
        flystartPoint = location1;
        if (currentPosition < 0) {
            // 无动画
//            if (mHbListView.hbIsRubbishOut()) {
//                mHbListView.hbSetRubbishBackNoAnim();
//            }
            mHBSongSingleAdapter.setSelected(arg2 - 1);
            mHbListView.invalidateViews();
            if (!isFromBroadcast) {
                MusicUtils.playAll(HBSongSingle.this, songList, arg2 - 1, repeat_mode);
                HBMusicUtil.setCurrentPlaylist(HBSongSingle.this, playlistInfo.getPlaylistId());
            }
            isFromBroadcast = false;
            startFly();
            return;
        } else if (currentPosition < mHbListView.getFirstVisiblePosition()) {
            // 从最上面飞进来
            mPlaySelect.setY(-mPlaySelect.getHeight());
            distance = location1[1] - location[1] + mPlaySelect.getHeight();
        } else if (currentPosition > mHbListView.getLastVisiblePosition()) {
            // 从最下面飞进来
            mPlaySelect.setY(mHbListView.getHeight());
            distance = mHbListView.getHeight() - location1[1] + location[1];
        } else {
            // 具体位置飞进
            View view = mHbListView.getChildAt(currentPosition - mHbListView.getFirstVisiblePosition());
            view.getLocationInWindow(location2);
            mPlaySelect.setY(location2[1] - location[1]);
            distance = Math.abs(location2[1] - location1[1]);
        }

        aima = ObjectAnimator.ofFloat(mPlaySelect, "y", location1[1] - location[1]);
        aima.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator arg0) {
                mPlaySelect.setVisibility(View.VISIBLE);
                mHBSongSingleAdapter.setNotSelect();
                mHbListView.invalidateViews();
            }

            @Override
            public void onAnimationRepeat(Animator arg0) {

            }

            @Override
            public void onAnimationEnd(Animator arg0) {

                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
//                        if (mHbListView.hbIsRubbishOut()) {
//                            mHbListView.hbSetRubbishBackNoAnim();
//                        }
                        mHBSongSingleAdapter.setSelected(arg2 - 1);
                        mHbListView.invalidateViews();
                        mPlaySelect.setVisibility(View.GONE);
                        if (!isFromBroadcast) {
                            isfromanimation = true;
                            MusicUtils.playAll(HBSongSingle.this, songList, arg2 - 1, repeat_mode);
                            HBMusicUtil.setCurrentPlaylist(HBSongSingle.this, playlistInfo.getPlaylistId());
                        }
                        isFromBroadcast = false;
                        mPlaySelect.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                // TODO Auto-generated method stub
                                startFly();
                            }
                        }, 10);
                    }
                });

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

    @SuppressLint("UseSparseArrays")
    @Override
    public void run() {
        isQuery = true;
        Uri uri = null;
        Cursor tCursor = null;
        HashMap<Long, Integer> tMap = new HashMap<Long, Integer>();
        if (mStartMode == 1) {
            // 最近添加的歌曲
            tCursor = this.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mCursorCols, Globals.QUERY_SONG_FILTER, null, MediaStore.Audio.Media.DATE_ADDED + " desc");
        } else if (mStartMode == 2) {
            // 我喜欢的歌曲
            songList = MusicUtils.mSongDb.querySongIdFromFavorites(mPathsXml);
            mHandler.obtainMessage(0, songList).sendToTarget();
            return;
        } else {
            // 自定义歌单
            if (playlistInfo == null) {
                return;
            }
            int playlistid = playlistInfo.getPlaylistId();
            if (playlistid < 0) {
                return;
            }
            uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistid);
            tCursor = this.getContentResolver().query(uri, mPlaylistMemberCols, Globals.QUERY_SONG_FILTER, null, MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER + " desc");
        }

        if (tCursor == null) {
            return;
        }

        ArrayList<HBListItem> tList = new ArrayList<HBListItem>();
        if (tCursor.moveToFirst()) {
            do {
                long mId = tCursor.getLong(0);
                String mTitle = tCursor.getString(1);
                String mPath = tCursor.getString(2);
                String mAlbumName = tCursor.getString(3);
                String mArtistName = tCursor.getString(4);
                String mUri = mPath;
                long albumId = tCursor.getLong(5);
                HBListItem item = null;
                String imgUri = HBMusicUtil.getImgPath(getApplication(), HBMusicUtil.MD5(mTitle + mArtistName + mAlbumName));
                if (mStartMode == 0) {
                    int playOrder = tCursor.getInt(6);
                    item = new HBListItem(mId, mTitle, mUri, mAlbumName, albumId, mArtistName, 0, imgUri, null, null, playOrder);
                } else {
                    item = new HBListItem(mId, mTitle, mUri, mAlbumName, albumId, mArtistName, 0, imgUri, null, null, -1);
                }
                String dir = mUri.substring(0, mUri.lastIndexOf("/"));
                if (!mPathsXml.contains(dir)) {
                    tList.add(item);
                }
            } while (tCursor.moveToNext());
        }
        tCursor.close();

        if (mStartMode == 2 && tList.size() > 0) {
            Collections.sort(tList, new MyListComparator(tMap));
        }
        songList = tList;
        mHandler.obtainMessage(0, tList).sendToTarget();

    }

    private static class MyListComparator implements Comparator<HBListItem> {
        private HashMap<Long, Integer> map = new HashMap<Long, Integer>();

        public MyListComparator(HashMap<Long, Integer> tMap) {
            this.map = tMap;
        }

        @Override
        public int compare(HBListItem lhs, HBListItem rhs) {
            return map.get(Long.valueOf((long) lhs.getSongId())).compareTo(map.get(Long.valueOf((long) rhs.getSongId())));
        }
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (mHBSongSingleAdapter == null) {
                        return;
                    }

                    ArrayList<HBListItem> list = (ArrayList<HBListItem>) msg.obj;

                    for (int i = 0; i < songList.size(); i++) {
                        try {
                            if (MusicUtils.sService == null) {
                                break;
                            }
                            if ((songList.get(i).getSongId()) == MusicUtils.sService.getAudioId()) {

                                mHBSongSingleAdapter.setSelected(i);
                                break;
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                    mHBSongSingleAdapter.setSonglist(list);
                    updataSingleTitleAndSongSize(list.size());
                    isQuery = false;
                    break;
                case 1:
                    changeMenuState();
                    break;
            }
        }

    };

    @SuppressWarnings("deprecation")
    @Override
    public void onClick(View arg0) {
//        if (mHbListView.hbIsRubbishOut()) {
//            mHbListView.hbSetRubbishBackNoAnim();
//            mHbListView.invalidateViews();
//        }
        switch (arg0.getId()) {
            case R.id.hb_id_add_song:
                // 添加歌曲
//                Intent intent = new Intent(this, HBNewPlayListActivity.class);
//                intent.putExtra(HBSongSingle.EXTR_PLAYLIST_INFO, playlistInfo);
//                startActivity(intent);
                break;
            case R.id.hb_id_eidt_song:
                if (!isEditMode) {
//                    if (!mHbActionBar.hbIsEntryEditModeAnimRunning()) {
//                        setEditeMode(true);
//                        mHbActionBar.setShowBottomBarMenu(true);
//                        mHbActionBar.showActionBarDashBoard();
//                        changeMenuState();
//                    }
                }
                break;
            case R.id.hb_id_delete_single:
                // 删除歌单
                showDialog(HB_ID_DELETE_SINGLE);
                break;
            case R.id.hb_id_play_single:
                if (songList.size() == 0) {
                    Toast.makeText(this, R.string.hb_playlist_no_songs, Toast.LENGTH_SHORT).show();
                } else {
                    if (isOnline(0)) {
                        boolean isshow = FlowTips.showPlayFlowTips(HBSongSingle.this, new OndialogClickListener() {

                            @Override
                            public void OndialogClick() {
                                startPlayAnimation(1, 2);
                            }
                        });
                        if (isshow) {
                            return;
                        }
                    }
                    startPlayAnimation(1, 2);
                }

                break;
            case R.id.hb_single_rename_bg:
                // 在编辑模式 点击重命名
                if (isEditMode) {
                    goToRenameMode();
                }
                break;
        }
    }

    private void exitEidtMode() {
//        if (!mHbActionBar.hbIsEntryEditModeAnimRunning() || mHbActionBar.hbIsExitEditModeAnimRunning()) {
//            mHbActionBar.setShowBottomBarMenu(false);
//            mHbActionBar.showActionBarDashBoard();
//            setEditeMode(false);
//            ((TextView) mHbActionBar.getSelectRightButton()).setText(getResources().getString(R.string.selectAll));
//        }
    }

    private void setEditeMode(boolean is, int position) {
        if (is) {
            isEditMode = true;
            addButton.setEnabled(false);
            editButton.setEnabled(false);
            deleteButton.setEnabled(false);
            playButton.setEnabled(false);
            setNoSongHeadButton(false);
            // mSongSize.setText(" ");
            if (mStartMode == 0) {
                renameButton.setVisibility(View.VISIBLE);
                noSongRenameButton.setVisibility(View.VISIBLE);
                mHeadView.findViewById(R.id.hb_single_rename_bg).setEnabled(true);
                noSongHeadView.findViewById(R.id.hb_single_rename_bg).setEnabled(true);
            }
            if (position == -1) {
                mHBSongSingleAdapter.setNeedIn();
            } else {
                mHBSongSingleAdapter.setNeedIn(position);
            }
            if (isPlaying) {
                playView.clearAnimation();
                playView.setBackgroundResource(R.drawable.hb_left_bar_clicked);
                isPlaying = false;
            }
            //mHbListView.hbSetNeedSlideDelete(false);

        } else {
            isEditMode = false;
            addButton.setEnabled(true);
            editButton.setEnabled(true);
            deleteButton.setEnabled(true);
            playButton.setEnabled(true);
            setNoSongHeadButton(true);
            updataSingleTitleAndSongSize(songList.size());
            renameButton.setVisibility(View.GONE);
            noSongRenameButton.setVisibility(View.GONE);
            mHBSongSingleAdapter.setNeedOut();
            renameEdit.setVisibility(View.GONE);
            renameEdit.clearFocus(); // 获取焦点
            nosongEdit.setVisibility(View.GONE);
            nosongEdit.clearFocus();
            mTitle.setVisibility(View.VISIBLE);
            noSongTitle.setVisibility(View.VISIBLE);

            mHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    setPlayAnimation();
                }
            }, 500);
            //mHbListView.hbSetNeedSlideDelete(true);
            mHeadView.findViewById(R.id.hb_single_rename_bg).setEnabled(false);
            noSongHeadView.findViewById(R.id.hb_single_rename_bg).setEnabled(false);
        }

    }

    private void setEditeMode(boolean is) {
        setEditeMode(is, -1);
    }

    @Override
    public void onBackPressed() {

        // LogUtil.d(TAG, "onBackPressed():" + isEditMode);
//        if (isEditMode) {
//            exitEidtMode();
//        } else if (mHbListView.hbIsRubbishOut()) {
//            mHbListView.hbSetRubbishBack();
//        } else {
//            super.onBackPressed();
//        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK && (isEditMode)) {
//            if (mHbActionBar.hbIsEntryEditModeAnimRunning() || mHbActionBar.hbIsExitEditModeAnimRunning()) {
//                return true;
//            }
            if (isRenameMode) {
                // 重命名模式
                // 返回不保存重命名
                renameEdit.setText(playlistInfo.getName());
                nosongEdit.setText(playlistInfo.getName());
                mTitle.setText(playlistInfo.getName());
                noSongTitle.setText(playlistInfo.getName());
                gobackEditMode();
            }
            exitEidtMode();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    public void changeMenuState() {
//        if (mHBSongSingleAdapter.getCheckedCount() == mHBSongSingleAdapter.getCount()) {
//            if (mHBSongSingleAdapter.getCount() == 0) {
//                ((TextView) mHbActionBar.getSelectRightButton()).setText(getResources().getString(R.string.selectAll));
//                ((TextView) mHbActionBar.getSelectRightButton()).setEnabled(false);
//                ((TextView) mHbActionBar.getSelectRightButton()).setTextColor(getResources().getColor(R.color.hb_select_disable));
//            } else {
//                ((TextView) mHbActionBar.getSelectRightButton()).setText(getResources().getString(R.string.selectAllNot));
//            }
//        } else {
//            ((TextView) mHbActionBar.getSelectRightButton()).setText(getResources().getString(R.string.selectAll));
//            ((TextView) mHbActionBar.getSelectRightButton()).setEnabled(true);
//            ((TextView) mHbActionBar.getSelectRightButton()).setTextColor(oldColer);
//        }
//        if (mHBSongSingleAdapter.getCheckedCount() == 0) {
//
//            mHbActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(0, false);
//            if (mStartMode != 0) {
//                mHbActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(1, false);
//            }
//
//        } else {
//            mHbActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(0, true);
//            if (mStartMode != 0) {
//                if (mStartMode == 2 && mHBSongSingleAdapter.ischeckedOnline()) {
//                    mHbActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(1, false);
//                } else {
//                    mHbActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(1, true);
//                }
//            }
//        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isStop = false;
        //mHbListView.hbOnResume();

        if (mHBSongSingleAdapter != null) {
            mHBSongSingleAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onPause() {
        //mHbListView.hbOnPause();
        HBMusicUtil.clearflyWindown();
        super.onPause();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case HB_ID_DELETE_SONGS:
                removeDialog(id);
                break;

            default:
                break;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dilog;
        switch (id) {
            case HB_ID_DELETE_SINGLE:
                LogUtil.d(TAG, "HB_ID_DELETE_SINGLE");
                AlertDialog.Builder build = new AlertDialog.Builder(this).setTitle(R.string.hb_delete_message)
                         .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (playlistInfo.getPlaylistId() >= 0) {

                                    // 删除操作
                                    Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, playlistInfo.getPlaylistId());
                                    int del = getContentResolver().delete(uri, null, null);
                                    LogUtil.d(TAG, "delete uri:" + uri + " del:" + del);
                                }
                                finish();
                            }
                        }).setNegativeButton(R.string.cancel, null);
                dilog = build.create();
                break;
            case HB_ID_DELETE_SONGS:
                LogUtil.d(TAG, "HB_ID_DELETE_SONGS");
                 String message = null;
                int mode = R.string.hb_remove;
                if (mStartMode == 1) {
                    if (deleteId != -1||mHBSongSingleAdapter.getCheckedCount()==1) {
                        message = getString(R.string.deleteMessage);
                    } else {
                        //message = getString(R.string.deleteMessage_num, mHBSongSingleAdapter.getCheckedCount());
                    }
                    mode = R.string.delete;

                } else {
                    if (deleteId != -1||mHBSongSingleAdapter.getCheckedCount()==1) {
                        message = getString(R.string.hb_remove_message);
                    } else {
                        //message = getString(R.string.hb_remove_num_message, mHBSongSingleAdapter.getCheckedCount());
                    }
                    mode = R.string.hb_remove;
                }
                AlertDialog.Builder build1 = new AlertDialog.Builder(this).setTitle(message)//.setMessage(message)
                        .setPositiveButton(mode, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 删除歌曲操作
                                if (deleteId != -1) {
                                    LogUtil.d(TAG, "HB_ID_DELETE_SONGS 1");
                                    deleteId = -1;
                                    mHBSongSingleAdapter.hasDeleted = true;
                                    //mHbListView.hbDeleteSelectedItemAnim();
                                } else {
                                    LogUtil.d(TAG, "HB_ID_DELETE_SONGS 2");
                                    mHBSongSingleAdapter.deletesongs(mStartMode);
                                    if (mStartMode != 1) {
                                        exitEidtMode();
                                        updataSingleTitleAndSongSize(songList.size());
                                    }
                                }

                            }
                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
//                                if (mHbListView.hbIsRubbishOut()) {
//                                    mHbListView.hbSetRubbishBack();
//                                }
                            }
                        });
                dilog = build1.create();
                break;
            default:
                dilog = super.onCreateDialog(id);
                break;
        }
        return dilog;
    }

    public void updateTitleSizeAndExitMode() {
        exitEidtMode();
    }

    /**
     * 处理触摸事件分发
     * @param ev
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (HBMusicUtil.isShouldHideInput(v, ev)) {

                HBMusicUtil.hideInputMethod(this, v);
                // 保存重命名
                String renameStr = renameEdit.getText().toString();
                if (songList.size() == 0) {
                    renameStr = nosongEdit.getText().toString();
                }
                if (HBMusicUtil.isBlank(renameStr)) {
                    Toast.makeText(this, getString(R.string.error_input_null), Toast.LENGTH_SHORT).show();
                    renameStr = playlistInfo.getName();
                } else if (HBMusicUtil.idForplaylist(renameStr, this) >= 0 && !renameStr.equals(playlistInfo.getName())) {
                    Toast.makeText(this, getString(R.string.error_input_exists), Toast.LENGTH_SHORT).show();
                    renameStr = playlistInfo.getName();
                } else {
                    reNamePlaylist(renameStr);
                }
                playlistInfo.setName(renameStr);
                renameEdit.setText(renameStr);
                nosongEdit.setText(renameStr);
                mTitle.setText(renameStr);
                noSongTitle.setText(renameStr);
                // 回到编辑界面
                gobackEditMode();
                // 退出编辑模式
                exitEidtMode();
                return true; // 隐藏键盘时，其他控件不响应点击事件==》注释则不拦截点击事件
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void reNamePlaylist(String name) {
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues(1);
        name = Globals.HB_PLAYLIST_TIP + name;
        values.put(MediaStore.Audio.Playlists.NAME, name);
        values.put(MediaStore.Audio.Playlists.DATA, HBMusicUtil.getNewPlaylistData(name, playlistInfo.getPlaylistId(), this));
        resolver.update(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values, MediaStore.Audio.Playlists._ID + "=?", new String[] { Long.valueOf(playlistInfo.getPlaylistId()).toString() });
    }

    /**
     * 回到编辑模式
     */
    private void gobackEditMode() {
        isRenameMode = false;
        renameEdit.setVisibility(View.GONE);
        renameEdit.clearFocus(); // 获取焦点
        nosongEdit.setVisibility(View.GONE);
        nosongEdit.clearFocus();
        renameButton.setVisibility(View.VISIBLE);
        noSongRenameButton.setVisibility(View.VISIBLE);
        mTitle.setVisibility(View.VISIBLE);
        noSongTitle.setVisibility(View.VISIBLE);
        // 显示bottombar
//        ((TextView) mHbActionBar.getSelectRightButton()).setTextColor(oldColer);
//        ((TextView) mHbActionBar.getSelectLeftButton()).setTextColor(oldColer);
    }

    /**
     * 进入重命名模式
     */
    private void goToRenameMode() {
        isRenameMode = true;

        renameEdit.setVisibility(View.VISIBLE);
        nosongEdit.setVisibility(View.VISIBLE);
        if (mHbListView.getVisibility() == View.VISIBLE) {
            renameEdit.requestFocus(); // 获取焦点
        } else {
            nosongEdit.requestFocus();
        }
        renameButton.setVisibility(View.GONE);
        noSongRenameButton.setVisibility(View.GONE);
        mTitle.setVisibility(View.GONE);
        noSongTitle.setVisibility(View.GONE);
        // 强制弹出输入法
        HBMusicUtil.showInputMethod(this);
        // 隐藏bottombar
//        mHbActionBar.setShowBottomBarMenu(false);
//        mHbActionBar.showActionBottomeBarMenu();
//        ((TextView) mHbActionBar.getSelectRightButton()).setTextColor(getResources().getColor(R.color.hb_select_disable));
//        ((TextView) mHbActionBar.getSelectLeftButton()).setTextColor(getResources().getColor(R.color.hb_select_disable));
    }

    private void updataSingleTitleAndSongSize(int size) {
        if (playlistInfo == null) {
            LogUtil.e(TAG, "---playlistInfo is null");
            return;
        }
        String name = playlistInfo.getName();
        if (TextUtils.isEmpty(name)) {
            return;
        }
        mTitle.setText(name);
        noSongTitle.setText(name);
        mSongSize.setText(getString(R.string.hb_num_songs_of_single, size));
        if (mStartMode == 1) {
            MusicUtils.setIntPref(this, Globals.PREF_RECENTLY, size);
        }
        if (size == 0) {
            mHbListView.setVisibility(View.GONE);
            findViewById(R.id.hb_id_no_songs).setVisibility(View.VISIBLE);
        } else {
            mHbListView.setVisibility(View.VISIBLE);
            findViewById(R.id.hb_id_no_songs).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        playlistInfo = (HBMainMenuData) intent.getSerializableExtra(EXTR_PLAYLIST_INFO);
        executor.submit(this);

    }

    @Override
    public void onMediaDbChange(boolean selfChange) {

    }

    private void setNoSongHeadButton(boolean is) {
        noSongHeadView.findViewById(R.id.hb_id_add_song_button).setEnabled(is);
        noSongHeadView.findViewById(R.id.hb_id_edit_song_button).setEnabled(is);
        noSongHeadView.findViewById(R.id.hb_id_delete_single_button).setEnabled(is);
        mHeadView.findViewById(R.id.hb_id_add_song_button).setEnabled(is);
        mHeadView.findViewById(R.id.hb_id_edit_song_button).setEnabled(is);
        mHeadView.findViewById(R.id.hb_id_delete_single_button).setEnabled(is);
        noSongHeadView.findViewById(R.id.hb_id_add_song).setEnabled(is);
        noSongHeadView.findViewById(R.id.hb_id_eidt_song).setEnabled(is);
        noSongHeadView.findViewById(R.id.hb_id_delete_single).setEnabled(is);
        noSongHeadView.findViewById(R.id.hb_id_play_single).setEnabled(is);
    }

    private void startFly() {
        if (isStop)
            return;
        playView.getLocationInWindow(flyendPoint);
        HBMusicUtil.startFly(HBSongSingle.this, flystartPoint[0], flyendPoint[0], flystartPoint[1], flyendPoint[1], true);
    }

    private boolean isOnline(int position) {
        if (songList == null || position >= songList.size()) {
            return false;
        }
        return songList.get(position).getIsDownLoadType() == 1;
    }

}
