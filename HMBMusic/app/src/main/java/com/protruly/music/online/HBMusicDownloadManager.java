package com.protruly.music.online;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.protruly.music.MediaPlaybackService;
import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.adapter.HBMusicDownloadAdapter;
import com.protruly.music.downloadex.DownloadInfo;
import com.protruly.music.downloadex.DownloadManager;
import com.protruly.music.ui.BasicActivity;
import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.util.ThreadPoolExecutorUtils;
import com.protruly.music.util.LogUtil;
import com.protruly.music.util.HBMusicUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import hb.widget.HbListView;
import hb.app.dialog.AlertDialog;

/**
 * Created by hujianwei on 17-9-4.
 */

public class HBMusicDownloadManager extends BasicActivity implements View.OnClickListener {
    private static final String TAG = "HBMusicDownloadManager";
    private static final int PLAY_BUTTON = 0;
    // 动画是否在运行

    private boolean isPlaying = false;
    private View playView; // 播放按钮
    private Animation operatingAnim; // 播放按钮动画
    private HbListView mListView;
    private View mHeadView;
    private TextView mSongSize;
    private Button button;
    private HBMusicDownloadAdapter mHBMusicDownloadAdapter;
    private Handler mHandler = new Handler();
    private final int HB_ID_REMOVE_DOWNLOADS = 101;
    //private HbActionBar mHbActionBar;
    private boolean isEditeMode = false;
    private TextView selectLeftBtn, selectRightBtn, noDownloadTask;
    private ColorStateList oldColer;
    private boolean isOntouch = false;
    private AlertDialog mAlertDialog;
    private DownloadInfo deleteInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHbContentView(R.layout.hb_songsingle_main);
        try {
            initActionbar();
            initView();
        } catch (Exception e) {
            e.printStackTrace();
        }
        new ScanDownloadTask().executeOnExecutor(ThreadPoolExecutorUtils.getThreadPoolExecutor().getExecutor());
    }

    private void initView() {
        mListView = (HbListView) findViewById(R.id.hb_id_song_list);
        // 给listview 添加head
        mHeadView = LayoutInflater.from(this).inflate(R.layout.downloadmanagerhead, null);
        mSongSize = (TextView) mHeadView.findViewById(R.id.id_song_size);
        button = (Button) mHeadView.findViewById(R.id.id_all_pause);
        noDownloadTask = (TextView) findViewById(R.id.hb_no_download_task);
        button.setOnClickListener(this);
        mListView.addHeaderView(mHeadView);
        mHBMusicDownloadAdapter = new HBMusicDownloadAdapter(this, mListView);
        mListView.setAdapter(mHBMusicDownloadAdapter);
        mListView.setSelector(R.drawable.hb_playlist_item_clicked);
        //mListView.hbSetNeedSlideDelete(true);
        //mListView.hbSetHbBackOnClickListener(mHbBackOnClickListener);
        //mListView.hbSetDeleteItemListener(mHbDeleteItemListener);
        mListView.setOnItemLongClickListener(mOnLongClickListener);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                DownloadInfo info = (DownloadInfo) arg0.getAdapter().getItem(arg2);
                boolean is = mHBMusicDownloadAdapter.onItemclick(info, arg2);
                if (!is) {
                    button.setText(R.string.hb_all_download);
                } else {
                    button.setText(R.string.hb_all_pause);
                }
            }
        });
        mListView.setVisibility(View.GONE);
        noDownloadTask.setVisibility(View.VISIBLE);
        mListView.setOnTouchListener(mOnTouchListener);
        mListView.setOnScrollListener(mOnScrollListener);
    }

    private void initActionbar() {
        //setHbBottomBarMenuCallBack(hbMenuCallBack);
        //mHbActionBar = getHbActionBar();// 获取actionbar
        //mHbActionBar.setTitle(R.string.hb_download_manager);
        // 旋转动画方式
        //mHbActionBar.addItem(R.drawable.song_playing, PLAY_BUTTON, null);
        //playView = mHbActionBar.getItem(PLAY_BUTTON).getItemView();
        operatingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate_anim);
        LinearInterpolator lin = new LinearInterpolator();
        operatingAnim.setInterpolator(lin);
        //mHbActionBar.setOnHbActionBarListener(mOnHbActionBarItemClickListener);
        //mHbActionBar.initActionBottomBarMenu(R.menu.menu_songsingle_bottombar, 1);
        initActionbarMenu();
    }

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View arg0, MotionEvent arg1) {
            switch (arg1.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mHBMusicDownloadAdapter.setLeftDelete(true, true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mHBMusicDownloadAdapter.setLeftDelete(isOntouch, true);
                    break;
            }
            return false;
        }
    };

    private void initActionbarMenu() {
        //selectLeftBtn = (TextView) mHbActionBar.getSelectLeftButton();
        //selectRightBtn = (TextView) mHbActionBar.getSelectRightButton();
        oldColer = selectRightBtn.getTextColors();
        if (selectLeftBtn != null) {
            selectLeftBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    // 退出编辑模式
                    exitEditMode();
                }
            });
        }
        if (selectRightBtn != null) {
            selectRightBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    LogUtil.d(TAG, "selectRightBtn.getText():" + selectRightBtn.getText());
                    if (selectRightBtn.getText().equals(getResources().getString(R.string.selectAll))) {
                        selectRightBtn.setText(getString(R.string.selectAllNot));
                        mHBMusicDownloadAdapter.selectAll();
                    } else if (selectRightBtn.getText().equals(getString(R.string.selectAllNot))) {
                        selectRightBtn.setText(getString(R.string.selectAll));
                        mHBMusicDownloadAdapter.selectAllNot();
                    }
                    changeMenuState();
                }
            });
        }
    }
//
//    private HbMenuBase.OnHbMenuItemClickListener hbMenuCallBack = new HbMenuBase.OnHbMenuItemClickListener() {
//        @Override
//        public void hbMenuItemClick(int arg0) {
//            switch (arg0) {
//                case R.id.song_backup:
//                    showDeleteDialog();
//                    break;
//            }
//        }
//    };
//    private HbDeleteItemListener mHbDeleteItemListener = new HbDeleteItemListener() {
//        @Override
//        public void hbDeleteItem(View arg0, final int arg1) {
//            // mHBMusicDownloadAdapter.setLeftDelete(false);
//            mHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    mHBMusicDownloadAdapter.deleteItem(deleteInfo);
//                    notifySongsizeChange();
//                }
//            });
//            isOntouch = false;
//        }
//    };
    private AdapterView.OnItemLongClickListener mOnLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            // 进入编辑模式
            entryEditMode(arg2 - 1);
//            if (!mHbActionBar.hbIsEntryEditModeAnimRunning()) {
//                mHbActionBar.setShowBottomBarMenu(true);
//                mHbActionBar.showActionBarDashBoard();
//                changeMenuState();
//            }
            return false;
        }
    };

    public boolean isEditAnimRunning() {
        //return mHbActionBar.hbIsEntryEditModeAnimRunning() || mHbActionBar.hbIsExitEditModeAnimRunning();
        return false;
    }

    private void entryEditMode(int postion) {
        if (isEditeMode) {
            return;
        }
        isEditeMode = true;
        // mHBMusicDownloadAdapter.setLeftDelete(true);
        setEditeMode(true, postion);
    }

    private void exitEditMode() {
        if (!isEditeMode) {
            return;
        }
        isEditeMode = false;
//        if (!mHbActionBar.hbIsExitEditModeAnimRunning()) {
//            mHbActionBar.setShowBottomBarMenu(false);
//            mHbActionBar.showActionBarDashBoard();
//        }
        // mHBMusicDownloadAdapter.setLeftDelete(false);
        setEditeMode(false, -1);
        selectRightBtn.setText(getResources().getString(R.string.selectAll));
    }

    private void setEditeMode(boolean is, int position) {
        if (is) {
            if (position == -1) {
                mHBMusicDownloadAdapter.setNeedIn();
            } else {
                mHBMusicDownloadAdapter.setNeedIn(position);
            }
            if (isPlaying) {
                playView.clearAnimation();
                playView.setBackgroundResource(R.drawable.hb_left_bar_clicked);
                isPlaying = false;
            }
            //mListView.hbSetNeedSlideDelete(false);
            mListView.setSelector(android.R.color.transparent);
            button.setEnabled(false);
        } else {
            mListView.setSelector(R.drawable.hb_playlist_item_clicked);
            mHBMusicDownloadAdapter.setNeedOut();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setPlayAnimation();
                }
            }, 500);
            //mListView.hbSetNeedSlideDelete(true);
            button.setEnabled(true);
        }
    }

    public void changeMenuState() {
        if (mHBMusicDownloadAdapter.getCheckedCount() == mHBMusicDownloadAdapter.getCount()) {
            if (mHBMusicDownloadAdapter.getCount() == 0) {
                selectRightBtn.setText(getResources().getString(R.string.selectAll));
                selectRightBtn.setEnabled(false);
                selectRightBtn.setTextColor(getResources().getColor(R.color.hb_select_disable));
            } else {
                //((TextView) mHbActionBar.getSelectRightButton()).setText(getResources().getString(R.string.selectAllNot));
            }
        } else {
            selectRightBtn.setText(getResources().getString(R.string.selectAll));
            selectRightBtn.setEnabled(true);
            selectRightBtn.setTextColor(oldColer);
        }
        if (mHBMusicDownloadAdapter.getCheckedCount() == 0) {
            //mHbActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(0, false);
        } else {
            //mHbActionBar.getHbActionBottomBarMenu().setBottomMenuItemEnable(0, true);
        }
    }
//
//    private HbBackOnClickListener mHbBackOnClickListener = new HbBackOnClickListener() {
//        @Override
//        public void hbDragedSuccess(int arg0) {
//            LogUtil.d(TAG, "hbDragedSuccess");
//            mHBMusicDownloadAdapter.setLeftDelete(true, false);
//        }
//
//        @Override
//        public void hbDragedSuccess(int arg0) {
//            LogUtil.d(TAG, "hbDragedSuccess");
//            isOntouch = false;
//            mHBMusicDownloadAdapter.setLeftDelete(false, false);
//        }
//
//        @Override
//        public void hbOnClick(int arg0) {
//            LogUtil.d(TAG, "hbOnClick:" + arg0);
//            deleteInfo = (DownloadInfo) mHBMusicDownloadAdapter.getItem(arg0 - 1);
//            LogUtil.d(TAG, "hbOnClick:" + deleteInfo);
//            showDialog(HB_ID_REMOVE_DOWNLOADS);
//        }
//
//        @Override
//        public void hbPrepareDraged(int arg0) {
//            isOntouch = true;
//            mHBMusicDownloadAdapter.setLeftDelete(true, false);
//            LogUtil.d(TAG, "hbPrepareDraged:" + arg0);
//        }
//    };
//    private OnHbActionBarItemClickListener mOnHbActionBarItemClickListener = new OnHbActionBarItemClickListener() {
//        @Override
//        public void onHbActionBarItemClicked(int itemId) {
//            switch (itemId) {
//                case PLAY_BUTTON:
//                    Intent intent = new Intent();
//                    intent.setClass(HBMusicDownloadManager.this, HBPlayerActivity.class);
//                    startActivity(intent);
//                    // overridePendingTransition(R.anim.slide_right_in,
//                    // R.anim.slide_left_out);
//                    break;
//            }
//        }
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
                    if (!isPlaying && !isEditeMode) {
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

    class ScanDownloadTask extends AsyncTask<Void, Void, List<DownloadInfo>> {
        private DownloadManager mDownloadManager;

        @Override
        protected List<DownloadInfo> doInBackground(Void... arg0) {
            HashMap<Long, DownloadInfo> list = mDownloadManager.getDownloadingMapData();
            List<DownloadInfo> datas = new ArrayList<DownloadInfo>();
            Iterator<DownloadInfo> iterator = list.values().iterator();
            while (iterator.hasNext()) {
                DownloadInfo info = iterator.next();
                if (!info.isDownloadOver()) {
                    datas.add(info);
                }
            }
            return datas;
        }

        @Override
        protected void onPostExecute(List<DownloadInfo> result) {
            if (result == null) {
                return;
            }
            mHBMusicDownloadAdapter.setDatas(result);
            notifySongsizeChange();
        }

        @Override
        protected void onPreExecute() {
            mDownloadManager = DownloadManager.getInstance(getApplicationContext());
        }
    }

    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.id_all_pause:
                if (!HBMusicUtil.isNetWorkActive(this)) {
                    Toast.makeText(this, R.string.hb_network_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean is = mHBMusicDownloadAdapter.isDownloading();
                if (is) {
                    button.setText(R.string.hb_all_download);
                } else {
                    button.setText(R.string.hb_all_pause);
                }
                mHBMusicDownloadAdapter.pauseOrDownloadAll(is);
                break;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle bundle) {
        Dialog dilog;
        switch (id) {
            case HB_ID_REMOVE_DOWNLOADS:
                // String title1 = getString(R.string.remove_download);
                String message1 = getString(R.string.remove_download_message);
                AlertDialog.Builder build = new AlertDialog.Builder(this).setTitle(message1)
                        // .setIcon(android.R.drawable.ic_dialog_info).setMessage(message1)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mHBMusicDownloadAdapter.canDelete(deleteInfo)) {
                                    //mListView.hbDeleteSelectedItemAnim();
                                }
                            }
                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                //mListView.hbSetRubbishBack();
                            }
                        });
                dilog = build.create();
                break;
            default:
                dilog = super.onCreateDialog(id);
                break;
        }
        return dilog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        // TODO Auto-generated method stub
        super.onPrepareDialog(id, dialog, args);
    }

    @Override
    public void onBackPressed() {
        LogUtil.d(TAG, "onBackPressed():" + isEditeMode);
//        if (isEditeMode) {
//            exitEditMode();
//        } else if (mListView.hbIsRubbishOut()) {
//            mListView.hbSetRubbishBack();
//        } else {
//            super.onBackPressed();
//        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK && (isEditeMode)) {
//            if (mHbActionBar.hbIsEntryEditModeAnimRunning() || mHbActionBar.hbIsExitEditModeAnimRunning()) {
//                return true;
//            }
            exitEditMode();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    public void notifySongsizeChange(boolean is) {
        if (!is) {
            button.setText(R.string.hb_all_download);
        } else {
            button.setText(R.string.hb_all_pause);
        }
    }

    public void notifySongsizeChange() {
        mSongSize.setText(getString(R.string.hb_num_songs_of_single, mHBMusicDownloadAdapter.getCount()));
        if (mHBMusicDownloadAdapter.getCount() == 0) {
            mListView.setVisibility(View.GONE);
            noDownloadTask.setVisibility(View.VISIBLE);
            // 如果在编辑模式退出编辑模式
            if (isEditeMode) {
                exitEditMode();
            }
        } else {
            mListView.setVisibility(View.VISIBLE);
            noDownloadTask.setVisibility(View.GONE);
        }
        boolean is = mHBMusicDownloadAdapter.isDownloading();
        LogUtil.d(TAG, "notifySongsizeChange:" + is);
        if (!is) {
            button.setText(R.string.hb_all_download);
        } else {
            button.setText(R.string.hb_all_pause);
        }
        isRubbishout();
    }

    public boolean isRubbishout() {
//        if (mListView.hbIsRubbishOut()) {
//            mListView.hbSetRubbishBack();
//            return true;
//        }
        return false;
    }

    @Override
    protected void onDestroy() {
        DownloadManager.getInstance(getApplicationContext()).clearListenerMap();
        super.onDestroy();
    }

    private void showDeleteDialog() {
        mAlertDialog=null;
//		if (mAlertDialog == null) {
        String msg= getString(R.string.remove_download_message);
        int count = mHBMusicDownloadAdapter.getDatasNum();
        if(count>1){
            msg = getString(R.string.remove_download_num_message,count);
        }
        AlertDialog.Builder build = new AlertDialog.Builder(this)// .setTitle(R.string.remove_download)
                .setTitle(msg).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {}
                }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        mHBMusicDownloadAdapter.deleteSelectItem();
                        exitEditMode();
                        notifySongsizeChange();
                    }
                });
        mAlertDialog = build.create();
//		}
        mAlertDialog.show();
    }

    private AbsListView.OnScrollListener mOnScrollListener = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView abslistview, int arg1) {
            if (arg1 == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            } else if (arg1 == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
            }
        }

        @Override
        public void onScroll(AbsListView abslistview, int i, int j, int k) {}
    };
}
