package com.protruly.music.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HbSearchView;
import android.widget.ImageView;
import android.widget.ListView;

import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.adapter.HBMainMenuAdapter;
import com.protruly.music.online.HBMusicDownloadManager;
import com.protruly.music.online.HBRankList;
import com.protruly.music.util.Globals;
import com.protruly.music.util.HBMainMenuData;
import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.util.ThreadPoolExecutorUtils;
import com.protruly.music.util.LogUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import hb.widget.HbEditText;
import hb.app.dialog.AlertDialog;

import static com.protruly.music.R.id.id_songs_btn;

/**
 * Created by hujianwei on 17-9-4.
 */

public class HBMyMusicFragment implements AdapterView.OnItemClickListener, View.OnClickListener, MusicUtils.Defs{

    private static final String TAG = "HBMyMusicFragment";
    private static final float HIDE_HEAED_RATE = 2.7f;
    private static final int HIDE_HEAD_SLEEP = 7;
    private static final int TOP_REBOUND_SLEEP = 13;

    private FrameLayout.LayoutParams mLayoutParams;
    private HBMainMenuAdapter mHBMainMenuAdapter;
    private int paramsmarginsize = 0;
    private ListView firstMain;
    private View headView;
    private ImageView myMusicTopbar;
    private int bgHeight = 0;
    private int bgWidth = 0;
    private Matrix matrix = new Matrix();
    private float yDown = 0, yDown1 = 0;
    private boolean ableToPull = false;

    //惯性回弹动画是否可运行，true 可运行，false 不可运行。
    private boolean mThreadState = true;

    // 用于判断是否在滑动
    private boolean mRunState = true;

    private int mTime = 15;

    //记录按下时滚动条位置
    int startPosition = 0;
    
    private VelocityTracker mVelocityTracker;

    // 记录滚动速度
    private int mVelocitY;
    private Context mContext;
    private boolean isdownOnTop = true;
    private Dialog mDialog;
    private HbEditText inputName;
    private boolean isFingerUp = false;
    private int myfavoriteId = -2;
    private boolean isclicked = false;
    private ImageView mAudioButtonAnim, mArtistButtonAnim, mFoldButtonAnim, mAudioButtonPress, mArtistButtonPress, mFoldButtonPress;
    private Bitmap musicBg;

    public void initview(View view, Context context) {
        LogUtil.d(TAG, "initview");
        mContext = context;
        firstMain = (ListView) view.findViewById(R.id.id_first_main);
        mHBMainMenuAdapter = new HBMainMenuAdapter(context);

        // 添加listvew head
        headView = LayoutInflater.from(context).inflate(R.layout.hb_mymusic_top_layout, null);
        myMusicTopbar = (ImageView) headView.findViewById(R.id.id_my_music_topbar);

        firstMain.addHeaderView(headView);
        firstMain.setAdapter(mHBMainMenuAdapter);
        firstMain.setOnItemClickListener(this);

        if (musicBg == null) {
            musicBg = BitmapFactory.decodeResource(context.getResources(), R.drawable.my_music_bg);
        }
        // 设置按钮监听
        headView.findViewById(id_songs_btn).setOnClickListener(this);
        headView.findViewById(R.id.id_singer_btn).setOnClickListener(this);
        headView.findViewById(R.id.id_fold_btn).setOnClickListener(this);
        mAudioButtonAnim = (ImageView) headView.findViewById(R.id.id_song_btn_press_anim);
        mAudioButtonPress = (ImageView) headView.findViewById(R.id.id_song_btn_press);
        mArtistButtonAnim = (ImageView) headView.findViewById(R.id.id_singer_btn_press_anim);
        mArtistButtonPress = (ImageView) headView.findViewById(R.id.id_singer_btn_press);
        mFoldButtonAnim = (ImageView) headView.findViewById(R.id.id_fold_btn_press_anim);
        mFoldButtonPress = (ImageView) headView.findViewById(R.id.id_fold_btn_press);

        paramsmarginsize = context.getResources().getDimensionPixelSize(R.dimen.hb_my_music_page_zoom_size);
        bgHeight = context.getResources().getDimensionPixelSize(R.dimen.hb_my_music_toplayout_height);
        bgWidth = context.getResources().getDimensionPixelSize(R.dimen.hb_my_music_toplayout_width);
        firstMain.setOnTouchListener(mOnTouchListener);
        firstMain.setOnScrollListener(mOnScrollListener);
        mLayoutParams = (FrameLayout.LayoutParams) myMusicTopbar.getLayoutParams();
    }

    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if (event.getPointerCount() > 1) {
                return true;
            }
            // 正在播放动画禁用系统touch
            if (!mThreadState) {
                return true;
            }

            if (!ableToPull) {
                yDown = event.getRawY();
            }
            // 获得动作捕捉器
            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            }


            // 开始捕捉动作
            mVelocityTracker.addMovement(event);
            mVelocityTracker.computeCurrentVelocity(1);
            int velocitY = (int) mVelocityTracker.getYVelocity();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    yDown = event.getRawY();
                    startPosition = (int) event.getRawY();
                    if (headView.getTop() == 0) {
                        isdownOnTop = true;
                    } else {
                        isdownOnTop = false;
                    }
                    mVelocitY = 0;
                    break;
                case MotionEvent.ACTION_UP:
                    if (ableToPull && !mRunState) {
                        float yMove = event.getRawY();
                        float distance = yMove - yDown1;
                        if (yDown1 == 0) {
                            distance = 0;
                        }
                        hideHead(distance);
                    }
                    isFingerUp = true;
                    mRunState = true;

                    mVelocitY = Math.abs(velocitY);

                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }

                    yDown = 0;
                    yDown1 = 0;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mVelocitY < 1 || isdownOnTop) {
                        mRunState = false;
                    } else {
                        mRunState = true;
                        yDown = 0;
                        break;
                    }

                    if (isScrollTop()) {

                        float yMove = event.getRawY();
                        // 防止yDown为0的情况
                        if (yDown == 0) {
                            yDown = yMove;
                        }
                        float distance = yMove - yDown - 60;

                        mLayoutParams.bottomMargin = paramsmarginsize + (int) (distance / HIDE_HEAED_RATE);
                        mLayoutParams.topMargin = paramsmarginsize + (int) (distance / HIDE_HEAED_RATE);

                        if (mLayoutParams.bottomMargin >= 0) {
                            if (yDown1 == 0) {
                                yDown1 = yMove;
                            }
                            float distance1 = yMove - yDown1;
                            float scale1 = (distance1 / HIDE_HEAED_RATE + bgHeight) / bgHeight;
                            mLayoutParams.bottomMargin = 0;
                            mLayoutParams.topMargin = 0;

                            if (bgHeight * scale1 < bgHeight) {
                                scale1 = 1;
                            }

                            myMusicTopbar.setScaleX(scale1);
                            myMusicTopbar.setScaleY(scale1);
                            mLayoutParams.height = (int) (bgHeight * scale1);
                        } else {

                            // 防止卡顿
                            yDown1 = 0;
                        }

                        if (isHeadMove()) {
                            // LogUtil.d(TAG, "禁用系统滚动");
                            myMusicTopbar.setLayoutParams(mLayoutParams);
                            return true;
                        } else {
                            mLayoutParams.bottomMargin = paramsmarginsize;
                            mLayoutParams.topMargin = paramsmarginsize;
                        }

                    } else {

                    }
                    break;
            }

            return false;
        }
    };

    private boolean isHeadMove() {
        return mLayoutParams.bottomMargin > paramsmarginsize ? true : false;
    }

    private AbsListView.OnScrollListener mOnScrollListener = new AbsListView.OnScrollListener() {

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            if (isScrollTop() && isFingerUp) {
                isFingerUp = false;

                if (!mThreadState) {
                    return;
                }
                if (startPosition != 0 && firstMain != null && !isdownOnTop) {
                    LogUtil.d(TAG, "initTopAnimator...");
                    initTopAnimator();
                    startPosition = 0;
                }
            }

        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

    };

    private boolean isScrollTop() {

        if (headView.getTop() == 0) {
            firstMain.setOverScrollMode(View.OVER_SCROLL_NEVER);
            ableToPull = true;
            return true;
        }
        ableToPull = false;
        firstMain.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        HBMainMenuData info = (HBMainMenuData) arg0.getAdapter().getItem(arg2);

        if (isHeadMove() || isclicked) {
            return;
        }
        LogUtil.d(TAG, "arg2:"+arg2);
        if (arg2 == 1) {
            initCreatePlayList();
        } else if (arg2 == 2) {
            Intent intent = new Intent();
            intent.setClass(mContext, HBSongSingle.class);
            info.setPlaylistId(myfavoriteId);
            intent.putExtra(HBSongSingle.EXTR_PLAYLIST_INFO, info);
            intent.putExtra(HBSongSingle.EXTR_PLAYLIST_START_MODE, 2);
            mContext.startActivity(intent);
        } else if (arg2 == 3) {
            Intent intent = new Intent();
            intent.setClass(mContext, HBSongSingle.class);
            info.setPlaylistId(-1);
            intent.putExtra(HBSongSingle.EXTR_PLAYLIST_INFO, info);
            intent.putExtra(HBSongSingle.EXTR_PLAYLIST_START_MODE, 1);
            mContext.startActivity(intent);
        } else if (arg2 == 4 && Globals.SWITCH_FOR_ONLINE_MUSIC) {
            Intent intent = new Intent(mContext, HBMusicDownloadManager.class);
            mContext.startActivity(intent);
        } else if (arg2 == 5 && Globals.SWITCH_FOR_ONLINE_MUSIC) {
            Intent intent = new Intent(mContext, HBRankList.class);
            mContext.startActivity(intent);
        } else if (Globals.NO_MEUN_KEY&&arg2 == 6) {
            HBMediaPlayHome activity = (HBMediaPlayHome) mContext;
            activity.addMusicCoverView();
//            HbSystemMenu systemMenu = ((HBMediaPlayHome)mContext).getHbMenu();
//            if (!Globals.STORAGE_PATH_SETTING) {
//                systemMenu.removeMenuByItemId(R.id.hb_storage_setting);
//                return;
//            }
//            if (!((Application) mContext.getApplicationContext()).isHaveSdStorage()) {
//                systemMenu.setMenuItemEnable(R.id.hb_storage_setting, false);
//            } else {
//                systemMenu.setMenuItemEnable(R.id.hb_storage_setting, true);
//            }
//            activity.showHbMenu(activity.getWindow().getDecorView(), Gravity.BOTTOM, 0, 0);
        } else {
            Intent intent = new Intent();
            intent.setClass(mContext, HBSongSingle.class);
            intent.putExtra(HBSongSingle.EXTR_PLAYLIST_INFO, info);
            mContext.startActivity(intent);
        }

    }

    @Override
    public void onClick(View arg0) {
        if (isclicked) {
            return;
        }
        isclicked = true;
        switch (arg0.getId()) {
            case id_songs_btn:
                LogUtil.d(TAG, "onClick myMusic");
                showButtonAnim(mAudioButtonPress, mAudioButtonAnim, HBTrackBrowserActivity.class);
                break;
            case R.id.id_singer_btn:
                showButtonAnim(mArtistButtonPress, mArtistButtonAnim, HBLocalMusicActivity.class);
                break;
            case R.id.id_fold_btn:
                //showButtonAnim(mFoldButtonPress, mFoldButtonAnim, HBFoldActivity.class);
                break;
        }
    }

    private void showButtonAnim(final View pressView, final View animView, final Class<?> cls) {

        AnimatorSet mAnimatorSet = new AnimatorSet();
        AnimatorSet animatorset1 = new AnimatorSet();
        ObjectAnimator objanimator1 = ObjectAnimator.ofFloat(pressView, "alpha", new float[] { 1, 0.1f });
        ObjectAnimator objanimator2 = ObjectAnimator.ofFloat(animView, "scaleX", new float[] { 0.2f, 1 });
        ObjectAnimator objanimator3 = ObjectAnimator.ofFloat(animView, "scaleY", new float[] { 0.2f, 1 });
        ObjectAnimator objanimator4 = ObjectAnimator.ofFloat(animView, "alpha", new float[] { 0, 0.4f });
        ObjectAnimator objanimator5 = ObjectAnimator.ofFloat(animView, "alpha", new float[] { 0.4f, 0f });

        objanimator1.setDuration(240);
        objanimator2.setDuration(240);
        objanimator3.setDuration(240);
        objanimator4.setDuration(80);
        objanimator5.setDuration(160);
        objanimator1.setInterpolator(new LinearInterpolator());
        objanimator2.setInterpolator(new DecelerateInterpolator());
        objanimator3.setInterpolator(new DecelerateInterpolator());
        objanimator4.setInterpolator(new LinearInterpolator());
        objanimator5.setInterpolator(new DecelerateInterpolator());
        animatorset1.play(objanimator5).after(objanimator4);
        mAnimatorSet.playTogether(objanimator1, objanimator2, objanimator3, animatorset1);

        mAnimatorSet.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator arg0) {

                pressView.setVisibility(View.VISIBLE);
                animView.setVisibility(View.VISIBLE);

            }

            @Override
            public void onAnimationRepeat(Animator arg0) {

            }

            @Override
            public void onAnimationEnd(Animator arg0) {

                Intent intent = new Intent(mContext, cls);
                mContext.startActivity(intent);
                pressView.setVisibility(View.GONE);
                animView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator arg0) {

            }
        });
        // mAnimatorSet.setInterpolator(new DecelerateInterpolator());
        mAnimatorSet.start();

    }

    private void hideHead(float distance) {
        LogUtil.d(TAG, "distance:" + distance);
        AnimatorSet scanAnimate = new AnimatorSet();
        AnimatorSet animatorSet = new AnimatorSet();

        ObjectAnimator margin = ObjectAnimator.ofInt(myMusicTopbar, "margin", new int[] { mLayoutParams.bottomMargin, paramsmarginsize });
        margin.setDuration(150);
        if (distance > 0) {
            float scale = (distance / HIDE_HEAED_RATE + bgHeight) / bgHeight;
            ObjectAnimator scanX = ObjectAnimator.ofFloat(myMusicTopbar, "scaleX", new float[] { scale, 1 });
            ObjectAnimator scanY = ObjectAnimator.ofFloat(myMusicTopbar, "scaleY", new float[] { scale, 1 });
            ObjectAnimator heightAnim = ObjectAnimator.ofInt(myMusicTopbar, "hight", new int[] { (int) (bgHeight * scale), bgHeight });
            scanAnimate.playTogether(scanX, scanY, heightAnim);
            if (distance < 200) {
                scanAnimate.setDuration(150);
            } else {
                scanAnimate.setDuration(250);
            }
            animatorSet.setInterpolator(new LinearInterpolator());
            animatorSet.play(margin).after(scanAnimate);
        } else {
            animatorSet.play(margin);
        }
        animatorSet.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animator) {
                mThreadState = false;
                LogUtil.d(TAG, "onAnimationStart");

            }

            @Override
            public void onAnimationRepeat(Animator animator) {
                LogUtil.d(TAG, "onAnimationRepeat");
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mThreadState = true;
                LogUtil.d(TAG, "onAnimationEnd");

            }

            @Override
            public void onAnimationCancel(Animator animator) {
                LogUtil.d(TAG, "onAnimationCancel");
            }
        });
        animatorSet.start();
    }

    private ScanPlaylistTask scanPlaylistTask;

    public void onResume() {
        isclicked = false;
        // mHBMainMenuAdapter.notifyDataSetChanged();
        if (scanPlaylistTask != null) {
            if (scanPlaylistTask.getStatus() != AsyncTask.Status.FINISHED) {
                scanPlaylistTask.cancel(true);
            }
        }
        scanPlaylistTask = new ScanPlaylistTask();
        // scanPlaylistTask.execute();
        // modify by  20150811 to
        scanPlaylistTask.executeOnExecutor(ThreadPoolExecutorUtils.getThreadPoolExecutor().getFULL_TASK_EXECUTOR());
    }

    public void onPause() {
        isFingerUp = false;
        mLayoutParams.bottomMargin = paramsmarginsize;
        mLayoutParams.topMargin = paramsmarginsize;
        mLayoutParams.height = (int) (bgHeight * 1);
        myMusicTopbar.setScaleX(1);
        myMusicTopbar.setScaleY(1);
        myMusicTopbar.setLayoutParams(mLayoutParams);
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    List<HBMainMenuData> list = (List<HBMainMenuData>) msg.obj;
                    // myMusicTopbar.setImageResource(R.drawable.my_music_bg);
                    myMusicTopbar.setImageBitmap(musicBg);// add by
                    initPlaylistData(list);
                    if (firstMain.getVisibility() == View.GONE) {
                        firstMain.setVisibility(View.VISIBLE);
                        firstMain.setAlpha(0);
                        ObjectAnimator anim = ObjectAnimator.ofFloat(firstMain, "alpha", 1);
                        anim.setDuration(500);
                        anim.start();
                    }
                    break;
                case 1:
                    HBMainMenuData info = (HBMainMenuData) msg.obj;
                    //Intent intent = new Intent(mContext, HBNewPlayListActivity.class);
                    //intent.putExtra(HBSongSingle.EXTR_PLAYLIST_INFO, info);
                    //mContext.startActivity(intent);
                    break;
            }
        }
    };

    private void initPlaylistData(List<HBMainMenuData> list) {
        mHBMainMenuAdapter.clearData();
        mHBMainMenuAdapter.addDatas(list);
    }

    /**
     * 新建歌单dialog
     */
    private void initCreatePlayList() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.create_playlist_view, null);
        inputName = (HbEditText) view.findViewById(R.id.hb_edit_playlist_name);
        final ImageView deleteButton = (ImageView) view.findViewById(R.id.hb_input_delete);
        deleteButton.setVisibility(View.GONE);
        deleteButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                inputName.setText("");
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext).setTitle(R.string.new_singer).setView(view).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                arg0.dismiss();

                HBMusicUtil.hideInputMethod(mContext, mDialog.getCurrentFocus());
                ThreadPoolExecutor executor = ThreadPoolExecutorUtils.getThreadPoolExecutor().getExecutor();
                executor.submit(dialogButtonClick);
                //ReportUtils.getInstance(mContext.getApplicationContext()).reportMessage(ReportUtils.TAG_CREATE_PL);
            }

        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                HBMusicUtil.hideInputMethod(mContext, mDialog.getCurrentFocus());

            }
        });

        mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (!mDialog.isShowing()){
            mDialog.show();
        }
        HBMusicUtil.showInputMethod(mContext);
        final Button positiveButton = ((AlertDialog) mDialog).getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setEnabled(false);
        inputName.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                String newText = inputName.getText().toString();
                if (newText.trim().length() == 0) {
                    deleteButton.setVisibility(View.GONE);
                    positiveButton.setEnabled(false);
                } else {
                    deleteButton.setVisibility(View.VISIBLE);
                    positiveButton.setEnabled(true);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

            }

            @Override
            public void afterTextChanged(Editable arg0) {

            }
        });
    }

    private Runnable dialogButtonClick = new Runnable() {

        @Override
        public void run() {

            String name = inputName.getText().toString();
            if (name != null && name.length() > 0) {
                ContentResolver resolver = mContext.getContentResolver();
                int id = HBMusicUtil.idForplaylist(name, mContext);

                if (id >= 0) {
                    try {
                        name = makePlaylistName(name);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                LogUtil.d(TAG, "name:" + name + " id:" + id);
                try {
                    ContentValues values = new ContentValues();
                    String name2 = Globals.HB_PLAYLIST_TIP + name;
                    values.put(MediaStore.Audio.Playlists.NAME, name2);
                    Uri uri = resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);

                    HBMainMenuData info = new HBMainMenuData();
                    info.setName(name);
                    info.setPlaylistId(Integer.parseInt(uri.getLastPathSegment()));
                    mHandler.obtainMessage(1, info).sendToTarget();
                } catch (Exception e) {
                    e.printStackTrace();
                    LogUtil.d(TAG, "insert fail!");
                }
            }
        }

    };

    private String makePlaylistName(String template) throws Exception {

        String[] cols = new String[] { MediaStore.Audio.Playlists.NAME };
        ContentResolver resolver = mContext.getContentResolver();
        String whereclause = MediaStore.Audio.Playlists.NAME + " LIKE ?";
        Cursor c = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, cols, whereclause, new String[] { Globals.HB_PLAYLIST_TIP + "%" }, MediaStore.Audio.Playlists.NAME);
        if (c == null) {
            return template;
        }
        int num = 1;
        // modify by  begin BUG #14130
        String suggestedname = "";
        if (template.endsWith("%")) {
            suggestedname = template + (num++);
        } else {
            suggestedname = String.format(template + "(%d)", num++);
        }
        // modify by  end BUG #14130
        boolean done = false;
        while (!done) {
            done = true;
            c.moveToFirst();
            while (!c.isAfterLast()) {
                String playlistname = c.getString(0);
                if (playlistname.compareToIgnoreCase(Globals.HB_PLAYLIST_TIP + suggestedname) == 0) {
                    if (template.endsWith("%")) {
                        suggestedname = template + (num++);
                    } else {
                        suggestedname = String.format(template + "(%d)", num++);
                    }
                    done = false;
                }
                c.moveToNext();
            }
        }
        c.close();
        LogUtil.d(TAG, "suggestedname:" + suggestedname);
        return suggestedname;
    }

    /**
     * 惯性回弹动画
     */
    private void initTopAnimator() {
        AnimatorSet animSetXY = new AnimatorSet();
        ObjectAnimator margin1 = ObjectAnimator.ofInt(myMusicTopbar, "margin", 0);
        ObjectAnimator margin2 = ObjectAnimator.ofInt(myMusicTopbar, "margin", paramsmarginsize);
        animSetXY.playSequentially(margin1, margin2);
        animSetXY.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator arg0) {
                mThreadState = false;

            }

            @Override
            public void onAnimationRepeat(Animator arg0) {

            }

            @Override
            public void onAnimationEnd(Animator arg0) {
                mThreadState = true;
            }

            @Override
            public void onAnimationCancel(Animator arg0) {

            }
        });
        animSetXY.setDuration(150);
        if (!mRunState) {
            return;
        }
        animSetXY.start();
    }

    public void notifiData() {
        if (mHBMainMenuAdapter != null)
            mHBMainMenuAdapter.notifyDataSetChanged();
    }

    class ScanPlaylistTask extends AsyncTask<Void, Void, List<HBMainMenuData>> {
        // private Bitmap mBitmap;

        @Override
        protected List<HBMainMenuData> doInBackground(Void... arg0) {
            Cursor mCursor = mContext.getContentResolver().query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, new String[] {
			/* 0 */
                    BaseColumns._ID,
			/* 1 */
                    MediaStore.Audio.PlaylistsColumns.NAME }, MediaStore.Audio.Playlists.NAME + " LIKE ?", new String[] { Globals.HB_PLAYLIST_TIP + "%" }, null);

            List<HBMainMenuData> list = new ArrayList<HBMainMenuData>();
            list.add(new HBMainMenuData(mContext.getString(R.string.new_singer), R.drawable.hb_create_song_list_icon, -1, false));
            list.add(new HBMainMenuData(mContext.getString(R.string.my_favorite_songs), R.drawable.hb_my_favorite_song_icon, 4, myfavoriteId));
            list.add(new HBMainMenuData(mContext.getString(R.string.recently_added_songs), R.drawable.hb_recently_added_songs_icon, 5, -1));
            if (Globals.SWITCH_FOR_ONLINE_MUSIC) {
                list.add(new HBMainMenuData(mContext.getString(R.string.hb_download_manager), R.drawable.hb_download_manage, 7, -4));
                list.add(new HBMainMenuData(mContext.getString(R.string.hb_collect_playlist), R.drawable.hb_collect_playlist, 6, -3));
            }
            if(Globals.NO_MEUN_KEY){
                list.add(new HBMainMenuData(mContext.getString(R.string.setting), R.drawable.setting, 8, -1));
            }
            if (mCursor != null && mCursor.moveToFirst()) {
                do {
                    int id = mCursor.getInt(0);
                    String name = mCursor.getString(1);
                    name = name.substring(Globals.HB_PLAYLIST_TIP.length());
                    HBMainMenuData info = new HBMainMenuData();
                    info.setName(name);
                    info.setPlaylistId(id);
                    info.setSongSizeType(0);
                    info.setShowArrow(true);
                    info.setResouceId(R.drawable.hb_create_song_list_default_icon);
                    list.add(info);
                } while (mCursor.moveToNext());
            }

            if (mCursor != null) {
                mCursor.close();
            }
            return list;
        }

        @Override
        protected void onPostExecute(List<HBMainMenuData> result) {
            if (mHBMainMenuAdapter == null || musicBg == null) {
                return;
            }
            mHandler.obtainMessage(0, result).sendToTarget();
        }

    }

    public void destroy() {
        if (firstMain != null) {
            firstMain.setAdapter(null);
            mHBMainMenuAdapter = null;
        }
    }

}
