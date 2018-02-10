package com.protruly.music.ui.album;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import com.protruly.music.MediaPlaybackService;
import com.protruly.music.MusicUtils;
import com.protruly.music.MusicUtils.ServiceToken;
import com.protruly.music.R;
import com.protruly.music.ui.AbstractBaseActivity;
import com.protruly.music.ui.HBPlayerActivity;
import com.protruly.music.util.Globals;
import com.protruly.music.util.HBListItem;
import com.protruly.music.util.HBMusicUtil;

import java.util.ArrayList;
import java.util.List;

import hb.app.HbActivity;

/**
 * Created by hujianwei on 17-9-1.
 */

public class AlbumListActivity extends AbstractBaseActivity implements AlbumListFragment.Callbacks, MusicUtils.Defs, ServiceConnection {

    private String mArtistId;
    private String mArtistName;
    private int numAlbums = 0;
    private int numTracks = 0;
    private ServiceToken mToken;
    HbActivity mActivity;

    //HbActionBar mActionBar;
    AlbumListFragment mFragment;
    private View vNowPlaying;
    private View ivPlayAll;
    private TextView tvAlbumTrack;
    View btn_cancel;
    View btn_selectAll;

    String selectAll;
    String unselectAll;
    private MyHandler mHandler = new MyHandler();
    private List<String> mPathsXml = new ArrayList<String>();
//    private OnHbActionBarItemClickListener mOnHbActionBarItemClickListener = new OnHbActionBarItemClickListener() {
//
//        @Override
//        public void onHbActionBarItemClicked(int itemId) {
//            switch (itemId) {
//                case Globals.NOW_PLAYING:
//                    Intent intent = new Intent(mActivity, HBPlayerActivity.class);
//                    startActivity(intent);
//                    break;
//
//                default:
//                    break;
//            }
//
//        }
//    };
//
//    private OnHBMenuItemClickListener hbMenuCallBack = new OnHBMenuItemClickListener() {
//        @Override
//        public void hbMenuItemClick(int itemId) {
//            switch (itemId) {
//                case R.id.menu_album_delete:
//                    mFragment.deleteAlbums();
//                    break;
//                case R.id.menu_album_addtolist:
//                    mFragment.addAlbumsToPlaylist();
//                    break;
//                default:
//                    break;
//            }
//        }
//    };

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    // private boolean mTwoPane;
    public AlbumListActivity() {
        mActivity = AlbumListActivity.this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            mArtistId = getIntent().getStringExtra(Globals.KEY_ARTIST_ID);
            mArtistName = getIntent().getStringExtra(Globals.KEY_ARTIST_NAME);
            numAlbums = getIntent().getIntExtra("artistofalbum", 0);
            numTracks = getIntent().getIntExtra("artistoftrack", 0);

        } else {
            mArtistId = savedInstanceState.getString(Globals.KEY_ARTIST_ID);
            mArtistName = savedInstanceState.getString(Globals.KEY_ARTIST_NAME);
            numAlbums = savedInstanceState.getInt("artistofalbum");
            numTracks = savedInstanceState.getInt("artistoftrack");
        }
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mToken = MusicUtils.bindToService(this, this);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        f.addDataScheme("file");
        registerReceiver(mScanListener, f);

        //setHbContentView(R.layout.activity_album_list, HbActionBar.Type.Normal);
        setHbContentView(R.layout.activity_album_list);

        mPathsXml = HBMusicUtil.doParseXml(this, "paths.xml");
        Fragment frag = getFragmentManager().findFragmentById(R.id.album_list);

        if (frag instanceof AlbumListFragment) {
            mFragment = (AlbumListFragment) frag;
        } else {
            mFragment = new AlbumListFragment();
            getFragmentManager().beginTransaction().add(R.id.album_list, mFragment).commitAllowingStateLoss();
        }
        selectAll = mActivity.getResources().getString(R.string.selectAll);
        unselectAll = mActivity.getResources().getString(R.string.selectAllNot);
        initActionBar();
        iniView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerStateChangeReceiver();
        setPlayAnimation();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiverSafe(mStatusListener);
    }

    private void initActionBar() {
//        setHbBottomBarMenuCallBack(hbMenuCallBack);
//        mActionBar = getHbActionBar();
//        if (null != mActionBar) {
//            mActionBar.setOnHbActionBarListener(mOnHbActionBarItemClickListener);
//            mActionBar.addItem(R.drawable.song_playing, Globals.NOW_PLAYING, null);
//            vNowPlaying = mActionBar.getItem(0).getItemView();
//            mActionBar.initActionBottomBarMenu(R.menu.menu_albumedit, 2);
//
//            btn_cancel = mActionBar.getSelectLeftButton();
//            btn_cancel.setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    if (null != mFragment) {
//                        mFragment.quitEditMode();
//                    }
//                }
//            });
//
//            btn_selectAll = mActionBar.getSelectRightButton();
//            btn_selectAll.setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    if (((TextView) btn_selectAll).getText().equals(selectAll)) {
//                        ((TextView) btn_selectAll).setText(unselectAll);
//                        mFragment.getAdapter().selectAll();
//                        changeMenuState();
//                        mFragment.getAdapter().notifyDataSetChanged();
//                    } else {
//                        ((TextView) btn_selectAll).setText(selectAll);
//                        mFragment.getAdapter().selectAllNot();
//                        changeMenuState();
//                        mFragment.getAdapter().notifyDataSetChanged();
//                    }
//                }
//            });
//        }
    }

    private void iniView() {
        ivPlayAll = findViewById(R.id.iv_playall);
        tvAlbumTrack = (TextView) findViewById(R.id.tv_tracknumber);

//        mActionBar.setTitle(mArtistName);
        ivPlayAll.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mHandler.playAll();
            }
        });
        updateHeader(numAlbums, numTracks);
    }


    @Override
    public void onItemSelected(String id) {



    }

    public String getArtistId() {
        return mArtistId;
    }

    public String getArtistName() {
        return mArtistName;
    }

    public int getNumAlbums() {
        return numAlbums;
    }

    public void setNumAlbums(int numAlbums) {
        this.numAlbums = numAlbums;
    }

    public int getNumTracks() {
        return numTracks;
    }

    public void setNumTracks(int numTracks) {
        this.numTracks = numTracks;
    }

    public View getIvPlayAll() {
        return ivPlayAll;
    }

    public TextView getTvAlbumTrack() {
        return tvAlbumTrack;
    }

    public void updateHeader(int numa, int numt) {
        // numa, numt 如果小于0 表示不更新 numAlbums, numTracks的值
        if (numa >= 0) {
            setNumAlbums(numa);
        }
        if (numt >= 0) {
            setNumTracks(numt);
        }
        tvAlbumTrack.setText(getResources().getString(R.string.num_albums_num_songs, numAlbums, numTracks));
    }

    // 动态更改actionbar 显示全选/反选
    public void changeMenuState() {

        Log.e("liumx", "mAdapter:size-----------------" + mFragment.getAdapter().getCount());
        if (mFragment.getAdapter().getCheckedCount() == mFragment.getAdapter().getCount()) {
            ((TextView) btn_selectAll).setText(unselectAll);
        } else {
            ((TextView) btn_selectAll).setText(selectAll);
        }
//        if (mFragment.getAdapter().getCheckedCount() == 0) {
//            mActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(0, false);
//            mActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(1, false);
//        } else {
//            mActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(0, true);
//            mActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(1, true);
//        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {

//                if (mActionBar != null && (mActionBar.hbIsExitEditModeAnimRunning() || mActionBar.hbIsEntryEditModeAnimRunning())) {
//                    return true;
//                }
//                try {
//                    boolean deleteIsShow = mFragment.getHbListView().hbIsRubbishOut();
//                    if (deleteIsShow) {
//                        mFragment.getHbListView().SetRubbishBack();
//                        return true;
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }

                if (mFragment.getAdapter().isEditMode()) {
                    mFragment.quitEditMode();
                    return true;
                }
                break;
            }
            default: {

            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        // need to store the selected item so we don't lose it in case
        // of an orientation switch. Otherwise we could lose it while
        // in the middle of specifying a playlist to add the item to.
        // outcicle.putString("selectedalbum", mCurrentAlbumId);
        outcicle.putString(Globals.KEY_ARTIST_ID, mArtistId);
        outcicle.putString(Globals.KEY_ARTIST_NAME, mArtistName);
        outcicle.putInt("artistofalbum", numAlbums);
        outcicle.putInt("artistoftrack", numTracks);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onServiceConnected(ComponentName arg0, IBinder arg1) {
        MusicUtils.updateNowPlaying(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        finish();
    }

    @Override
    public void onDestroy() {
        MusicUtils.unbindFromService(mToken);
        unregisterReceiverSafe(mScanListener);
        super.onDestroy();
    }

    private void unregisterReceiverSafe(BroadcastReceiver receiver) {
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }

    // 启动播放的动画
    public void startAnimation() {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.rotate_anim);
        LinearInterpolator lin = new LinearInterpolator();
        animation.setInterpolator(lin);
        vNowPlaying.startAnimation(animation);
        vNowPlaying.setBackgroundResource(android.R.color.transparent);

    }

    /**
     * 设置播放动画
     */
    public void setPlayAnimation() {
        try {
            if (MusicUtils.sService != null) {
                if (MusicUtils.sService.isPlaying()) {
                    startAnimation();
                } else {
                    vNowPlaying.clearAnimation();
                    vNowPlaying.setBackgroundResource(R.drawable.hb_left_bar_clicked);

                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            vNowPlaying.clearAnimation();
            vNowPlaying.setBackgroundResource(R.drawable.hb_left_bar_clicked);

        }
    }

    // 监听播放状态的变化
    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
                setPlayAnimation();
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

    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicUtils.setSpinnerState(mActivity);
            mReScanHandler.sendEmptyMessage(0);
            if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                MusicUtils.clearAlbumArtCache();
            }
        }
    };

    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mFragment.getAdapter() != null) {
                mFragment.reloadData();
            }
        }
    };

    @Override
    public void onMediaDbChange(boolean selfChange) {
        mFragment.reloadData();
    }

    private class MyHandler extends Handler {
        private final int play_all = 0;

        @Override
        public void handleMessage(Message msg) {
            if (play_all == msg.what) {
                ArrayList<HBListItem> arrayList = MusicUtils.getSongsForArtist(mActivity, Long.parseLong(mArtistId), mPathsXml);

                MusicUtils.playAll(mActivity, arrayList, 0, 2);
                HBMusicUtil.setCurrentPlaylist(mActivity, -1);

            }
        }

        public void playAll() {
            sendEmptyMessage(play_all);
        }
    };
}
