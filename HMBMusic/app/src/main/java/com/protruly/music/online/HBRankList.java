package com.protruly.music.online;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.TextView;

import com.protruly.music.MediaPlaybackService;
import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.adapter.OnlineRankListAdapter;
import com.protruly.music.model.HBCollectPlaylist;
import com.protruly.music.ui.BasicActivity;
import com.protruly.music.util.ThreadPoolExecutorUtils;
import com.protruly.music.model.HBCollectPlaylist;
import com.protruly.music.util.LogUtil;

import java.util.List;

import hb.widget.HbListView;
import hb.app.dialog.AlertDialog;

/**
 * Created by hujianwei on 17-9-4.
 */

public class HBRankList extends BasicActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = "HbRankList";
    private static final int PLAY_BUTTON = 0;

    // 动画是否在运行
    private boolean isPlaying = false;

    // 播放按钮
    private View playView;

    // 播放按钮动画
    private Animation operatingAnim;
    private HbListView mListView;
    private OnlineRankListAdapter mOnlineRankListAdapter;
    private AlertDialog mAlertDialog;
    private TextView noPlaylist;
    private List<HBCollectPlaylist> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHbContentView(R.layout.album_list_content);

        try {
            initView();
            initActionbar();

        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.d(TAG, "INIT ERROR!!!");
        }
    }

    private void initView() {
        mListView = (HbListView) findViewById(android.R.id.list);
        noPlaylist = (TextView) findViewById(R.id.hb_no_playlist);
//        mListView.hbSetNeedSlideDelete(false);
//        mListView.hbSetSelectorToContentBg(false);
//        mListView.setOnItemClickListener(this);
//        mListView.hbSetNeedSlideDelete(true);
//        mListView.hbSetDeleteItemListener(mHbDeleteItemListener);
//        mListView.hbSetHbBackOnClickListener(mHbBackOnClickListener);
        mListView.setSelector(R.drawable.hb_playlist_item_clicked);
        mOnlineRankListAdapter = new OnlineRankListAdapter(this);
        mListView.setAdapter(mOnlineRankListAdapter);
        mListView.setDivider(getResources().getDrawable(R.drawable.line2));
        changeViewState(0);
    }

    private void initActionbar() {
//        HbActionBar mActionBar = getHbActionBar();// 获取actionbar
//        mActionBar.setTitle(R.string.hb_collect_playlist);
//        // 旋转动画方式
//        mActionBar.addItem(R.drawable.song_playing, PLAY_BUTTON, null);
//        playView = mActionBar.getItem(PLAY_BUTTON).getItemView();
//        operatingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate_anim);
//
//        LinearInterpolator lin = new LinearInterpolator();
//        operatingAnim.setInterpolator(lin);
//        mActionBar.setOnHbActionBarListener(mOnHbActionBarItemClickListener);
    }
//
//    private HbDeleteItemListener mHbDeleteItemListener = new HbDeleteItemListener() {
//
//        @Override
//        public void hbDeleteItem(View arg0, int arg1) {
//
//            HBCollectPlaylist item = (HBCollectPlaylist) mOnlineRankListAdapter.getItem(arg1);
//            MusicUtils.mSongDb.deleteCollectById(item.getPlaylistid());
//            mOnlineRankListAdapter.deleteItem(arg1);
//            changeViewState(list.size());
//        }
//
//    };
//
//    private HbBackOnClickListener mHbBackOnClickListener = new HbBackOnClickListener() {
//
//        @Override
//        public void hbDragedSuccess(int arg0) {
//
//        }
//
//        @Override
//        public void hbDragedUnSuccess(int arg0) {
//
//        }
//
//        @Override
//        public void hbOnClick(int arg0) {
//            showDeleteDialog();
//        }
//
//        @Override
//        public void hbPrepareDraged(int arg0) {
//
//        }
//
//    };

    @Override
    protected void onPause() {
        //mListView.hbOnPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //mListView.hbOnResume();
        new RankDataTask().executeOnExecutor(ThreadPoolExecutorUtils.getThreadPoolExecutor().getExecutor());
    }

    @Override
    protected void onStart() {
        super.onStart();
        setPlayAnimation();
        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(MediaPlaybackService.META_CHANGED);
        registerReceiver(mStatusListener, new IntentFilter(f));
    }

    @Override
    protected void onStop() {
        if (isPlaying) {
            playView.clearAnimation();
            playView.setBackgroundResource(R.drawable.hb_left_bar_clicked);
            isPlaying = false;
        }
        unregisterReceiver(mStatusListener);
        super.onStop();
    }

    /**
     * 设置播放动画
     */
    private void setPlayAnimation() {
        try {
            if (MusicUtils.sService != null) {
                LogUtil.d(TAG, "isplaying:" + MusicUtils.sService.isPlaying());
                if (MusicUtils.sService.isPlaying()) {
                    if (!isPlaying) {
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

    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
                LogUtil.d(TAG, "mStatusListener:");
                setPlayAnimation();

            }
        }
    };
//
//    private OnHbActionBarItemClickListener mOnHbActionBarItemClickListener = new OnHbActionBarItemClickListener() {
//        @Override
//        public void onHbActionBarItemClicked(int itemId) {
//            switch (itemId) {
//                case PLAY_BUTTON:
//
//                    Intent intent = new Intent();
//                    intent.setClass(HbRankList.this, HbPlayerActivity.class);
//                    startActivity(intent);
//                    break;
//            }
//        }
//
//    };

    class RankDataTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... arg0) {
            if (MusicUtils.mSongDb != null) {
                list = MusicUtils.mSongDb.queryCollectInfo();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (list == null) {
                return;
            }
            mOnlineRankListAdapter.addDatas(list);
            changeViewState(list.size());
        }

        @Override
        protected void onPreExecute() {

        }

    }

    private void changeViewState(int size) {
        if (size > 0) {
            noPlaylist.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
        } else {
            noPlaylist.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        HBCollectPlaylist item = (HBCollectPlaylist) arg0.getAdapter().getItem(arg2);
        if (item == null) {
            return;
        }
        switch (item.getListType()) {
            case 0:
                Intent intent = new Intent(this, HBNetTrackDetailActivity.class);
                //intent.putExtra("tag", OnGridViewClickListener.NEW_ALBUM);
                intent.putExtra("title", item.getPlaylistname());
                intent.putExtra(HBNetTrackDetail.ID, item.getPlaylistid());
                intent.putExtra("imageUrl", item.getImgUrl());
                intent.putExtra("artist", item.getInfo());
                startActivity(intent);
                break;
            case 1:
                Intent intent2 = new Intent(this, HBNetTrackDetailActivity.class);
                //intent2.putExtra("tag", OnGridViewClickListener.RECOMMEND_PLAYLIST);
                intent2.putExtra(HBNetTrackDetail.ID, item.getPlaylistid());
                intent2.putExtra("imageUrl", item.getImgUrl());
                intent2.putExtra("title", item.getPlaylistname());
                intent2.putExtra("playlist_tag", item.getInfo());
                startActivity(intent2);
                break;
            case 2:
                Intent intent3 = new Intent(this, HBNetTrackDetailActivity.class);
                //intent3.putExtra("tag", OnGridViewClickListener.RANKING);
                intent3.putExtra(HBNetTrackDetail.ID, item.getPlaylistid());
                intent3.putExtra("title", item.getPlaylistname());
                startActivity(intent3);
                break;
            case 3:
                Intent intent4 = new Intent(this, HBNetTrackDetailActivity.class);
                //intent4.putExtra("tag", OnGridViewClickListener.BANNER);
                intent4.putExtra("type", item.getType());
                intent4.putExtra(HBNetTrackDetail.ID, item.getPlaylistid());
                intent4.putExtra("title", item.getPlaylistname());
                intent4.putExtra("imageUrl", item.getImgUrl());
                startActivity(intent4);
                break;
        }
    }

    private void showDeleteDialog() {
        if (mAlertDialog == null) {
            AlertDialog.Builder build = new AlertDialog.Builder(this).setTitle(R.string.hb_cancel_collection).setMessage(R.string.hb_cancel_collection_message)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {

//                            if (mListView.hbIsRubbishOut()) {
//                                mListView.hbSetRubbishBack();
//                            }
                        }
                    }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {

//                            mListView.hbDeleteSelectedItemAnim();
                        }
                    });
            mAlertDialog = build.create();
        }
        mAlertDialog.show();

    }

    @Override
    public void onBackPressed() {

//        if (mListView.hbIsRubbishOut()) {
//            mListView.hbSetRubbishBack();
//        } else {
//            super.onBackPressed();
//        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }
}
