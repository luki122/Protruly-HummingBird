package com.protruly.music.online;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.adapter.HBDescriptionAdapter;
import com.protruly.music.adapter.HBRecommendDetailAdapter;
import com.protruly.music.model.HBCollectPlaylist;
import com.protruly.music.model.XiaMiSdkUtils;
import com.protruly.music.util.FlowTips;
import com.protruly.music.util.FlowTips.OndialogClickListener;

import com.protruly.music.util.Globals;
import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.util.LogUtil;
import com.protruly.music.util.HBListItem;
import com.protruly.music.util.ThreadPoolExecutorUtils;
import com.protruly.music.widget.HBTrackListView;
import com.xiami.sdk.entities.OnlineAlbum;
import com.xiami.sdk.entities.OnlineArtist;
import com.xiami.sdk.entities.OnlineCollect;
import com.xiami.sdk.entities.OnlineSong;
import com.xiami.sdk.entities.RankType;
import hb.app.dialog.AlertDialog;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import hb.widget.HbListView;

/**
 * Created by hujianwei on 17-9-1.
 */

public class HBNetTrackDetail implements AdapterView.OnItemClickListener, View.OnClickListener, MusicUtils.Defs{

    private static final String TAG = "HBNetTrackDetail";
    public static final String ID = "Id";
    private static final float HIDE_HEAED_RATE = 2.7f;
    private static final int HIDE_HEAD_SLEEP = 7;
    private static final int TOP_REBOUND_SLEEP = 13;

    private int mType;
    private long mBegin;
    private long mEnd;

    private FrameLayout.LayoutParams mLayoutParams;
    private int paramsmarginsize = 0;
    private HbListView mListView;
    private View headView;
    private View logoView;
    private View footerView;
    private ImageView mRecommendTopbar;
    private int bgHeight = 0;
    private int bgWidth = 0;
    private Matrix matrix = new Matrix();
    private float yDown = 0, yDown1 = 0;
    private boolean ableToPull = false;
    /** 惯性回弹动画是否可运行，true 可运行，false 不可运行。 */
    private boolean mThreadState = true;
    private boolean mRunState = true; // 用于判断是否在滑动
    private int mTime = 15;
    /** 按下的时间 */
    long mStartTime = 0;
    /** 记录按下时滚动条位置 */
    int startPosition = 0;
    private VelocityTracker mVelocityTracker;
    private int mVelocitY; // 记录滚动速度
    private Activity mContext;
    private boolean isdownOnTop = true;
    private boolean isFingerUp = false;
    private ArrayList<HBListItem> mArrayList = new ArrayList<HBListItem>();
    private TextView iv_playAll;
    private TextView tv_songnumber;
    private Button iv_collection;
    private View iv_collection_parent;
    private View iv_downloadAll_parent;
    private View iv_share_parent;
    private Button iv_downloadAll;
    private Button iv_share;
    private float alaphpx;
    public static final int alaphCount = 255;
    private static final int MSG_REFRESH = 1;
    private static final int MSG_REFRESH_ALBUM = 2;
    private static final int MSG_REFRESH_COLLECT = 3;
    int mPageNo = 1;
    private static final int PAGE_SIZE = 100;
    private HBRecommendDetailAdapter mAdapter;
    private List mItems = new ArrayList();
    private ProgressBar mProgressBar;
    private String mShareUrl = "";
    private String imageUrl = "";
    private String id = "";
    public ImageView mPlaySelect;
    ObjectAnimator aima;
    private boolean showlogo = false;
    private DisplayMetrics displayMetrics;
    private String IMAGE_CACHE_DIR = "NetAlbum";
    private static final String album_share_prefix = "http://m.xiami.com/album/";
    private static final String collect_share_prefix = "http://m.xiami.com/collect/";
    private static final String top_share_prefix = "http://m.xiami.com/top/";
    private static final String song_share_prefix = "http://m.xiami.com/song/";
    private static final String artist_share_prefix = "http://m.xiami.com/artist/";
    private View firsItem;
    private int alaph = 0;
    private boolean needScroll;
    private int[] startPoint;
    private boolean isRunning = false;
    private boolean isPause;
    private volatile int clickNumber = 0;
    private String mCollectTitle;
    private static Handler mHandler;
    private String mArtisName;
    private String mTitleStr = null;
    private AlertDialog mAlertDialog;
    private String mDescription = "";
    private HBDescriptionAdapter mHBDescriptionAdapter;
    private ArrayList<String> mDescriprtionList;
    private boolean showShare = false;
    private boolean mBarFlag = false;

    static class HbHandler extends Handler {
        WeakReference<HBNetTrackDetail> mWeakReference;

        public HbHandler(WeakReference<HBNetTrackDetail> weakReference) {
            mWeakReference = weakReference;
        }

        public void handleMessage(Message msg) {
            HBNetTrackDetail HBNetTrackDetail = mWeakReference.get();
            if (HBNetTrackDetail == null) {
                return;
            }
            if (HBNetTrackDetail.mProgressBar != null) {
                HBNetTrackDetail.mProgressBar.setVisibility(View.GONE);
            }
            switch (msg.what) {
                case MSG_REFRESH:
                    if (HBNetTrackDetail.mItems == null || HBNetTrackDetail.mItems.size() == 0) {
                        Toast.makeText(HBNetTrackDetail.mContext, R.string.hb_network_error, Toast.LENGTH_SHORT).show();
                    } else {
                        HBNetTrackDetail.removeUnuse(HBNetTrackDetail.mItems);
                        HBNetTrackDetail.mAdapter = new HBRecommendDetailAdapter(HBNetTrackDetail.mContext, HBNetTrackDetail.mItems, HBNetTrackDetail.mType);
                        HBNetTrackDetail.mListView.setAdapter(HBNetTrackDetail.mAdapter);
                        HBNetTrackDetail.getHBListItems(HBNetTrackDetail.mItems);
                        ((HBNetTrackDetailActivity) (HBNetTrackDetail.mContext)).hideHeader();
                        HBNetTrackDetail.updateFootview();
                    }
                    break;
                case MSG_REFRESH_ALBUM:
                    OnlineAlbum album = (OnlineAlbum) msg.obj;
                    HBNetTrackDetail.refresh(album);
                    break;
                case MSG_REFRESH_COLLECT:
                    OnlineCollect collect = (OnlineCollect) msg.obj;
                    HBNetTrackDetail.refresh(collect);
                    break;
            }
        }
    }

    public void initview(View view, Activity context) {
        mHandler = new HbHandler(new WeakReference<HBNetTrackDetail>(this));
        mContext = context;
        mDescriprtionList = new ArrayList<String>();
        mHBDescriptionAdapter = new HBDescriptionAdapter(mContext, mDescriprtionList);
        mAlertDialog = new AlertDialog.Builder(context).setTitle(R.string.hb_decription)

                .setAdapter(mHBDescriptionAdapter, null).setNegativeButton(mContext.getString(R.string.hb_shutdown), null).setCancelable(true).create();
        mAlertDialog.setCanceledOnTouchOutside(true);
        mType = mContext.getIntent().getExtras().getInt("tag");
        id = mContext.getIntent().getExtras().getString(ID);
        LogUtil.d(TAG, "mType:" + mType + " id:" + id);
        mListView = (HbListView) view.findViewById(R.id.recommend_list);

        //mListView.hbEnableOverScroll(false);

        ((HBTrackListView) mListView).setHBNetTrackDetail(this);
        mListView.setSelector(R.drawable.hb_playlist_item_clicked);
        mPlaySelect = (ImageView) view.findViewById(R.id.hb_song_selected);
        mProgressBar = (ProgressBar) view.findViewById(R.id.hb_loading);

        headView = LayoutInflater.from(context).inflate(R.layout.hb_nettrackdetailheader_layout, null);
        logoView = view.findViewById(R.id.baidulog);
        footerView = LayoutInflater.from(context).inflate(R.layout.hb_recommend_footer, null);
        footerView.setOnClickListener(null);
        mRecommendTopbar = (ImageView) headView.findViewById(R.id.hb_recommend_topbar);
        imageUrl = mContext.getIntent().getExtras().getString("imageUrl");
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displayMetrics);

        mListView.addHeaderView(headView);
        mListView.addFooterView(footerView);
        mListView.setOnItemClickListener(this);
        iv_playAll = (TextView) headView.findViewById(R.id.hb_recommend_play);
        iv_playAll.setOnClickListener(this);
        tv_songnumber = (TextView) headView.findViewById(R.id.hb_recommand_songnumber);
        iv_collection = (Button) headView.findViewById(R.id.hb_id_collection_song);
        iv_collection_parent = headView.findViewById(R.id.hb_id_collection_song_parent);
        iv_collection_parent.setOnClickListener(this);
        iv_downloadAll = (Button) headView.findViewById(R.id.hb_id_download_song);
        iv_downloadAll_parent = headView.findViewById(R.id.hb_id_download_song_parent);
        iv_downloadAll_parent.setOnClickListener(this);
        iv_share = (Button) headView.findViewById(R.id.hb_id_share_single);
        iv_share_parent = headView.findViewById(R.id.hb_id_share_single_parent);
        iv_share_parent.setOnClickListener(this);

        paramsmarginsize = context.getResources().getDimensionPixelSize(R.dimen.hb_my_music_page_zoom_size);
        bgHeight = context.getResources().getDimensionPixelSize(R.dimen.hb_recommend_toplayout_height);
        bgWidth = context.getResources().getDimensionPixelSize(R.dimen.hb_recommend_toplayout_width);
        mListView.setOnTouchListener(mOnTouchListener);
        mListView.setOnScrollListener(mOnScrollListener);
        mLayoutParams = (FrameLayout.LayoutParams) mRecommendTopbar.getLayoutParams();
        alaphpx = (alaphCount * 1.0f) / mContext.getResources().getDimensionPixelSize(R.dimen.hb_detail_header);
        if (!TextUtils.isEmpty(imageUrl)) {
            DisplayImageOptions mOptions = new DisplayImageOptions.Builder().showImageOnLoading(R.drawable.hb_img_recommend_bg).imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
                    .cacheInMemory(true).cacheOnDisk(true).displayer(new SimpleBitmapDisplayer()).build();
            ImageLoader.getInstance().displayImage(imageUrl, mRecommendTopbar, mOptions);
        }
        changeCollectState(id);
        initPlaylistData();
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
            mVelocityTracker.addMovement(event);// 开始捕捉动作
            mVelocityTracker.computeCurrentVelocity(1);
            int velocitY = (int) mVelocityTracker.getYVelocity();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    yDown = event.getRawY();
                    mStartTime = new Date().getTime();
                    startPosition = (int) event.getRawY();
                    if (headView.getTop() == 0) {
                        isdownOnTop = true;
                    } else {
                        isdownOnTop = false;
                    }
                    mVelocitY = 0;
                    break;
                case MotionEvent.ACTION_UP:
                    if (!needScroll && mAdapter != null && mAdapter.getCount() < 10) {
                        ((HBNetTrackDetailActivity) mContext).changeHeaderStatus(0, alaph);
                        ((HBNetTrackDetailActivity) mContext).changeHeaderStatus(3, alaphCount);
                    }
                    if (ableToPull && !mRunState) {
                        float yMove = event.getRawY();
                        float distance = yMove - yDown1;
                        if (yDown1 == 0) {
                            distance = 0;
                        }
                        // LogUtil.d(TAG,
                        // "ableToPull:"+ableToPull+" mRunState:"+mRunState+" yDown1:"+yDown1+" distance:"+distance);
                        hideHead(distance);
                    }
                    isFingerUp = true;
                    mRunState = true;
                    long tuchtime = new Date().getTime() - mStartTime;

                    // final VelocityTracker velocityTracker = mVelocityTracker;
                    // velocityTracker.computeCurrentVelocity(1);

                    mVelocitY = Math.abs(velocitY);
                    // LogUtil.d(TAG, "ACTION_UP>velocitY:" + mVelocitY +
                    // " tuchtime:"
                    // + tuchtime);

                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }

                    // ableToPull=false;
                    yDown = 0;
                    yDown1 = 0;
                    break;
                case MotionEvent.ACTION_MOVE:
                    // LogUtil.d(TAG, "ACTION_MOVE-->velocitY:" +
                    // mVelocitY +" isdownOnTop:"+isdownOnTop);
                    if (mVelocitY < 1 || isdownOnTop) {
                        mRunState = false;
                    } else {
                        mRunState = true;
                        yDown = 0;
                        break;
                    }

                    if (isScrollTop()) {
                        // LogUtil.d(TAG, "onTouch.");

                        float yMove = event.getRawY();
                        // 防止yDown为0的情况
                        if (yDown == 0) {
                            yDown = yMove;
                        }
                        float distance = yMove - yDown - 60;
                        // LogUtil.d(TAG, "yMove:" + yMove + " yDown:" + yDown
                        // + " distance:" + distance);
                        // LogUtil.d(TAG,
                        // "isHeadMove()--->mLayoutParams.topMargin:"
                        // + mLayoutParams.topMargin);
                        mLayoutParams.bottomMargin = paramsmarginsize + (int) (distance / HIDE_HEAED_RATE);
                        mLayoutParams.topMargin = paramsmarginsize + (int) (distance / HIDE_HEAED_RATE);

                        if (mLayoutParams.bottomMargin >= 0) {
                            if (yDown1 == 0) {
                                yDown1 = yMove;
                            }
                            float distance1 = yMove - yDown1;
                            float scale1 = (distance1 / HIDE_HEAED_RATE + bgHeight) / bgHeight;
                            // LogUtil.d(TAG, "--distance1:" + distance1
                            // + " scale1:" + scale1);
                            mLayoutParams.bottomMargin = 0;
                            mLayoutParams.topMargin = 0;
                            // 防止缩小超过原始大小
                            if (bgHeight * scale1 < bgHeight) {
                                scale1 = 1;
                            }
                            // matrix.setScale(scale1, scale1, bgWidth / 2, 0);
                            // mRecommendTopbar.setImageMatrix(matrix);
                            mRecommendTopbar.setScaleX(scale1);
                            mRecommendTopbar.setScaleY(scale1);
                            mLayoutParams.height = (int) (bgHeight * scale1);
                        } else {
                            yDown1 = 0;// 防止卡顿
                        }
                        // LogUtil.d(TAG, "isHeadMove()::"+isHeadMove());
                        if (isHeadMove()) {
                            mRecommendTopbar.setLayoutParams(mLayoutParams);
                            return true;
                        } else {
                            mLayoutParams.bottomMargin = paramsmarginsize;
                            mLayoutParams.topMargin = paramsmarginsize;
                        }

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
            int y = 0;
            alaph = 0;
            if (firstVisibleItem == 0) {
                firsItem = mListView.getChildAt(1);
                if (firsItem != null) {
                    y = firsItem.getTop();
                    int marginTop = mContext.getResources().getDimensionPixelSize(R.dimen.hb_recommend_visibleheight) + getTopHeight()
                            + mContext.getResources().getDimensionPixelSize(R.dimen.hb_action_bar_height);
                    // LogUtil.d(TAG,
                    // "---y:"+y+" marginTop:"+marginTop);
                    if (y <= marginTop) {
                        ((HBNetTrackDetailActivity) mContext).changeHeaderStatus(1, 1);
                        ((HBNetTrackDetailActivity) mContext).changeHeaderStatus(2, alaphCount);
                        //mListView.hbSetHeaderViewYOffset((int) mContext.getResources().getDimension(R.dimen.hb_playmode_height)
                         //       + mContext.getResources().getDimensionPixelSize(R.dimen.hb_actionitem_height) + getTopHeight());
                        needScroll = true;
                        if (!mBarFlag) {
                            mBarFlag = true;
                            //HBMusicUtil.changeBarColor(mContext, mBarFlag);
                        }
                    } else {
                        if (mBarFlag) {
                            mBarFlag = false;
                            //HBMusicUtil.changeBarColor(mContext, mBarFlag);
                        }
                        float total = mContext.getResources().getDimensionPixelSize(R.dimen.hb_recommend_visibleheight)
                                + mContext.getResources().getDimensionPixelSize(R.dimen.hb_action_bar_height)
                                + mContext.getResources().getDimensionPixelSize(R.dimen.hb_detail_header);
                        //mListView.hbSetHeaderViewYOffset(-100);
                        // LogUtil.d(TAG, "total::"+total+" y:"+y);
                        if (y <= total) {
                            alaph = (int) (alaphpx * (total - y));
                            if (alaph < 0) {
                                alaph = alaphCount;
                            }
                            ((HBNetTrackDetailActivity) mContext).changeHeaderStatus(0, alaph);
                        } else {
                            alaph = 0;
                            ((HBNetTrackDetailActivity) mContext).changeHeaderStatus(0, alaph);
                        }
                    }
                }
            } else {
                if (!mBarFlag) {
                    mBarFlag = true;
                    //HBMusicUtil.changeBarColor(mContext, mBarFlag);
                }
                ((HBNetTrackDetailActivity) mContext).changeHeaderStatus(2, alaphCount);
                ((HBNetTrackDetailActivity) mContext).changeHeaderStatus(1, 1);
            }
            // int y = firsItem.getTop();
            // if (!ableToPull) {

            if (isScrollTop() && isFingerUp) {
                isFingerUp = false;
                // LogUtil.d(TAG, "mThreadState:" + mThreadState + " mRunState:"
                // + mRunState);
                if (!mThreadState) {
                    return;
                }
                if (startPosition != 0 && mListView != null && !isdownOnTop) {
                    initTopAnimator();
                    startPosition = 0;
                }
            }
            if (mAdapter != null && mListView.getLastVisiblePosition() >= mAdapter.getCount() && !showlogo) {
                showlogo = true;
                logoView.setVisibility(View.VISIBLE);
                mListView.removeFooterView(footerView);
            } else if (mAdapter != null && mListView.getLastVisiblePosition() < mAdapter.getCount() && !showlogo) {
                showlogo = true;
            }

        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

            // LogUtil.d(TAG, ">scrollState:" + scrollState);
        }

    };

    private boolean isScrollTop() {

        if (headView.getTop() == 0) {
            mListView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            ableToPull = true;
            return true;
        }
        ableToPull = false;
        mListView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        return false;
    }

    private boolean isScrollBottom() {

        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, final int postion, long arg3) {
        if (isRunning) {
            return;
        }
        LogUtil.d(TAG, "-postion:"+postion);
        Object object= mAdapter.getItem(postion-1);
        if(object!=null&&object instanceof OnlineSong){
            if(HBMusicUtil.isNoPermission((OnlineSong)object)){
                Toast.makeText(mContext, R.string.hb_play_permission, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (FlowTips.showPlayFlowTips(mContext, new OndialogClickListener() {

            @Override
            public void OndialogClick() {
                startPlayAnimation(postion, true);
            }
        })) {
            return;
        }
        startPlayAnimation(postion, true);
    }

    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.hb_recommend_play:
                if(mAdapter.isNoAvailable()){
                    Toast.makeText(mContext, R.string.hb_play_permission, Toast.LENGTH_SHORT).show();
                    return;
                }
                MusicUtils.playAll(mContext, mArrayList, 0, 2, true);
                break;
            case R.id.hb_id_collection_song_parent:
                //ReportUtils.getInstance(mContext.getApplicationContext()).reportMessage(113);
                String type = "";
                String showType = "";
                String playid = "";
                switch (mType) {
                    case 0:
                        if (!TextUtils.isEmpty(mArtisName)) {
                            showType = mArtisName;
                        }
                        playid = String.valueOf(id);
                        break;
                    case 1:
                        showType = mContext.getIntent().getExtras().getString("playlist_tag");
                        playid = String.valueOf(id);
                        break;
                    case 2:
                        // showType = HBMusicUtil.formatCurrentTime(mContext);
                        showType = String.valueOf(System.currentTimeMillis());
                        playid = String.valueOf(id);
                        break;
                    case 3:
                        // playid = mContext.getIntent().getExtras().getString("code");
                        playid = String.valueOf(id);
                        type = mContext.getIntent().getExtras().getString("type");
                        // showType = HBMusicUtil.formatCurrentTime(mContext);
                        showType = String.valueOf(System.currentTimeMillis());
                        break;
                }

                if (TextUtils.isEmpty(showType)) {
                    showType = mCollectTitle;
                }
                HBCollectPlaylist list = new HBCollectPlaylist(mTitleStr, playid, imageUrl, mAdapter.getCount(), showType, mType, type);
                if (!MusicUtils.mSongDb.isCollectById(playid)) {
                    MusicUtils.mSongDb.insertCollect(list);
                    changeCollectState(playid);
                    Toast.makeText(mContext, mContext.getString(R.string.hb_collection_suceess), Toast.LENGTH_SHORT).show();
                } else {
                    showCollectionDialog(playid);
                }
                break;
            case R.id.hb_id_download_song_parent:
                //ReportUtils.getInstance(mContext.getApplicationContext()).reportMessage(104);
                if (mAdapter != null) {
                    if (FlowTips.showDownloadFlowTips(mContext, new OndialogClickListener() {

                        @Override
                        public void OndialogClick() {
                            mAdapter.downloadAll();
                        }
                    })) {
                        return;
                    }
                    mAdapter.downloadAll();
                }
                break;
            case R.id.hb_id_share_single_parent:
                sharePlayList(mContext, mContext.getResources().getString(R.string.hb_player_share), mShareUrl);
                break;
            default:
                break;
        }
    }

    private void hideHead(float distance) {
        // LogUtil.d(TAG, "distance:"+distance);
        AnimatorSet scanAnimate = new AnimatorSet();
        AnimatorSet animatorSet = new AnimatorSet();

        ObjectAnimator margin = ObjectAnimator.ofInt(mRecommendTopbar, "margin", new int[] { mLayoutParams.bottomMargin, paramsmarginsize });
        margin.setDuration(150);
        if (distance > 0) {
            float scale = (distance / HIDE_HEAED_RATE + bgHeight) / bgHeight;
            ObjectAnimator scanX = ObjectAnimator.ofFloat(mRecommendTopbar, "scaleX", new float[] { scale, 1 });
            ObjectAnimator scanY = ObjectAnimator.ofFloat(mRecommendTopbar, "scaleY", new float[] { scale, 1 });
            ObjectAnimator heightAnim = ObjectAnimator.ofInt(mRecommendTopbar, "hight", new int[] { (int) (bgHeight * scale), bgHeight });
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

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mThreadState = true;

            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }
        });
        animatorSet.start();
    }

    public void onResume() {
        updateFootview();
        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();
        //mListView.hbOnResume();
        isRunning = false;
        isPause = false;
        if (null != mRecommendTopbar) {
            mRecommendTopbar.postDelayed(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    //HBMusicUtil.changeBarColor(mContext, mBarFlag);
                }
            }, 100);
        }
    }

    public void onPause() {
        //mListView.hbOnPause();
        isFingerUp = false;
        mLayoutParams.bottomMargin = paramsmarginsize;
        mLayoutParams.topMargin = paramsmarginsize;
        mLayoutParams.height = (int) (bgHeight * 1);
        mRecommendTopbar.setScaleX(1);
        mRecommendTopbar.setScaleY(1);
        mRecommendTopbar.setLayoutParams(mLayoutParams);
        isPause = true;
        if (aima != null && aima.isStarted()) {
            aima.end();
        }
        if (mAdapter != null) {
            mAdapter.clearCache();
        }
        HBMusicUtil.clearflyWindown();
    }

    private void initPlaylistData() {
        switch (mType) {
            case 0:
                mBegin = System.currentTimeMillis();
                getAlbum(Integer.valueOf(id));
                break;
            case 1:
                mBegin = System.currentTimeMillis();
                getPlaylist(String.valueOf(id));
                break;
            case 2:
                try {
                    if (Integer.valueOf(id) == 0) {
                        mRecommendTopbar.setImageBitmap(((HBNetTrackDetailActivity) mContext).getRecommendBitmap());
                    } else if (Integer.valueOf(id) == 5) {
                        mRecommendTopbar.setImageBitmap(((HBNetTrackDetailActivity) mContext).getRecommendBitmap());
                    } else if (Integer.valueOf(id) == 16) {
                        mRecommendTopbar.setImageBitmap(((HBNetTrackDetailActivity) mContext).getRecommendBitmap());
                    } else {
                        mRecommendTopbar.setImageBitmap(((HBNetTrackDetailActivity) mContext).getRecommendBitmap());
                    }
                } catch (OutOfMemoryError e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }
                getRank(Integer.valueOf(id));
                break;
            case 3:

                String type = mContext.getIntent().getExtras().getString("type");
                if (type.equals("song")) {
                    getSong(Integer.valueOf(id));
                } else if (type.equals("album")) {
                    getAlbum(Integer.valueOf(id));
                } else if (type.equals("artist")) {
                    getArtist(Long.valueOf(id));
                } else if (type.equals("collect")) {
                    getPlaylist(String.valueOf(id));
                }
                break;

            default:
                break;
        }
    }

    /**
     * 惯性回弹动画
     */
    private void initTopAnimator() {

        AnimatorSet animSetXY = new AnimatorSet();
        AnimatorSet animSetXY1 = new AnimatorSet();
        AnimatorSet animSetXY2 = new AnimatorSet();
        ObjectAnimator margin1 = ObjectAnimator.ofInt(mRecommendTopbar, "margin", 0);
        ObjectAnimator margin2 = ObjectAnimator.ofInt(mRecommendTopbar, "margin", paramsmarginsize);
        ObjectAnimator padding1 = ObjectAnimator.ofInt(mRecommendTopbar, "hight", (int) (bgHeight * 1.3));
        ObjectAnimator paddingX1 = ObjectAnimator.ofFloat(mRecommendTopbar, "scaleX", 1.3f);
        ObjectAnimator paddingY1 = ObjectAnimator.ofFloat(mRecommendTopbar, "scaleY", 1.3f);
        ObjectAnimator padding = ObjectAnimator.ofInt(mRecommendTopbar, "hight", bgHeight);
        ObjectAnimator paddingX = ObjectAnimator.ofFloat(mRecommendTopbar, "scaleX", 1f);
        ObjectAnimator paddingY = ObjectAnimator.ofFloat(mRecommendTopbar, "scaleY", 1f);

        animSetXY1.playTogether(paddingX1, paddingY1, padding1);
        animSetXY2.playTogether(paddingX, paddingY, padding);
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

    private void refresh(Object object) {
        if (object instanceof OnlineAlbum) {
            OnlineAlbum album = (OnlineAlbum) object;
            if (album != null) {
                mArtisName = album.getArtistName();
                mTitleStr = album.getAlbumName();
                List<OnlineSong> onlineSongs = album.getSongs();
                if (onlineSongs != null) {
                    mDescription = album.getDescription();
                    if (TextUtils.isEmpty(mDescription)) {
                        iv_share_parent.setEnabled(false);
                        iv_share.setEnabled(false);
                    }
                    if(onlineSongs.size()==0){
                        Toast.makeText(mContext, R.string.songs_is_shelves, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    for (OnlineSong song : onlineSongs) {
                        if(TextUtils.isEmpty(song.getAlbumName())){
                            song.setAlbumName(mTitleStr);
                        }
                    }
                    mItems= onlineSongs;
                    removeUnuse(mItems);
                    mAdapter = new HBRecommendDetailAdapter(mContext, mItems, 0,mTitleStr);
                    getHBListItems(mItems);
                    mListView.setAdapter(mAdapter);
                    updateFootview();
                    ((HBNetTrackDetailActivity) mContext).hideHeader();
                    ((HBNetTrackDetailActivity) mContext).setTitle(album.getAlbumName());
                } else {
                    Toast.makeText(mContext, R.string.hb_network_error, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(mContext, R.string.hb_network_error, Toast.LENGTH_SHORT).show();
            }
        } else if (object instanceof OnlineCollect) {
            OnlineCollect collect = (OnlineCollect) object;
            if (collect != null && collect.getSongs() != null && collect.getSongs().size() > 0) {
                mTitleStr = collect.getCollectName();
                mItems = collect.getSongs();
                mDescription = collect.getDescription();
                if (TextUtils.isEmpty(mDescription)) {
                    iv_share_parent.setEnabled(false);
                    iv_share.setEnabled(false);
                }
                removeUnuse(mItems);
                mAdapter = new HBRecommendDetailAdapter(mContext, mItems, 0);
                getHBListItems(mItems);
                mListView.setAdapter(mAdapter);
                updateFootview();
                ((HBNetTrackDetailActivity) mContext).hideHeader();
                ((HBNetTrackDetailActivity) mContext).setTitle(collect.getCollectName());
            } else {
                Toast.makeText(mContext, R.string.hb_network_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateFootview() {
        if (mAdapter != null) {
            //tv_songnumber.setText(mContext.getString(R.string.number_track, mAdapter.getCount()));
            tv_songnumber.setText(mContext.getString(R.string.number_track));
            ((HBNetTrackDetailActivity) mContext).showTrackNumber(mAdapter.getCount());
            if (MusicUtils.sService != null && mAdapter != null) {
                try {
                    long id = MusicUtils.getCurrentAudioId();
                    for (int i = 0; i < mArrayList.size(); i++) {
                        if (mArrayList.get(i).getSongId() == id) {
                            mAdapter.setCurrentPosition(i);
                        }
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private ThreadPoolExecutor getThreadPoolExecutor() {
        return ThreadPoolExecutorUtils.getThreadPoolExecutor().getExecutor();
    }

    private void getPlaylist(final String listId) {
        // 调用sdk精选集歌曲获取函数
        mShareUrl = collect_share_prefix + listId;
        getThreadPoolExecutor().submit(new Runnable() {

            @Override
            public void run() {
                OnlineCollect collect = XiaMiSdkUtils.getCollectDetailSync(mContext, Long.valueOf(listId));
                Message msg = Message.obtain();
                msg.what = MSG_REFRESH_COLLECT;
                msg.obj = collect;
                mHandler.sendMessage(msg);
            }
        });

    }

    private void getAlbum(final int id) {
        mShareUrl = album_share_prefix + id;
        getThreadPoolExecutor().submit(new Runnable() {

            @Override
            public void run() {
                OnlineAlbum album = XiaMiSdkUtils.getAlbumsDetailSync(mContext, id);
                Message msg = Message.obtain();
                msg.what = MSG_REFRESH_ALBUM;
                msg.obj = album;
                mHandler.sendMessage(msg);
            }
        });

    }

    private void getRank(final int type) {
        iv_share_parent.setEnabled(false);
        iv_share.setEnabled(false);
        getThreadPoolExecutor().submit(new Runnable() {

            @Override
            public void run() {
                mItems = XiaMiSdkUtils.getRankSongsSync(mContext, RankType.values()[type]);
                mTitleStr = mContext.getIntent().getExtras().getString("title");
                mHandler.sendEmptyMessage(MSG_REFRESH);

            }
        });

    }

    private void getHBListItems(List list) {
        LogUtil.d(TAG, "add all---HBListItem");
        for (int i = 0; i < list.size(); i++) {
            OnlineSong music = (OnlineSong) list.get(i);
            String pic = music.getImageUrl();
            HBListItem songItem = new HBListItem(music.getSongId(), music.getSongName(), music.getListenFile(), music.getAlbumName(), music.getAlbumId(), music.getArtistName(), 1, pic,
                    music.getLyric(), null, -1,!HBMusicUtil.isNoPermission(music));
            songItem.setArtistId(music.getArtistId());
            songItem.setSingers(music.getSingers());
            mArrayList.add(songItem);
        }
    }

    private void sharePlayList(Context context, String title, String text) {
        if (!showShare) {
            if (mAlertDialog != null && !mAlertDialog.isShowing()) {
                mDescription = mDescription.replace("&nbsp;", " ").replace("&ndash;", "–").replace("&ldquo;", "\"").replace("&rdquo;", "\"").replace("&hellip;", "…");
                mDescriprtionList.clear();
                mDescriprtionList.add(mDescription);
                mHBDescriptionAdapter.notifyDataSetChanged();
                mAlertDialog.show();
            }
            //ReportUtils.getInstance(mContext.getApplicationContext()).reportMessage(112);
        } else {
            //ReportUtils.getInstance(mContext.getApplicationContext()).reportMessage(112);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, title);
            intent.putExtra(Intent.EXTRA_TEXT, text);
            context.startActivity(Intent.createChooser(intent, title));
        }
    }

    public void playAll(int start) {
        if (!HBMusicUtil.isNetWorkActive(mContext)) {
            Toast.makeText(mContext, R.string.hb_network_error, Toast.LENGTH_SHORT).show();
            return;
        }
        MusicUtils.playAll(mContext, mArrayList, start, 2, true);
        mListView.invalidateViews();
    }

    private void changeCollectState(String playid) {
        if (!MusicUtils.mSongDb.isCollectById(playid)) {
            Drawable collectDrawable = mContext.getResources().getDrawable(R.drawable.hb_recommend_collection_btn);
            collectDrawable.setBounds(0, 0, collectDrawable.getIntrinsicWidth(), collectDrawable.getIntrinsicHeight());
            iv_collection.setCompoundDrawables(collectDrawable, null, null, null);
            iv_collection.setText(R.string.hb_collect_playlist2);
        } else {
            Drawable collectDrawable = mContext.getResources().getDrawable(R.drawable.hb_recommend_collectioncancel_btn);
            collectDrawable.setBounds(0, 0, collectDrawable.getIntrinsicWidth(), collectDrawable.getIntrinsicHeight());
            iv_collection.setCompoundDrawables(collectDrawable, null, null, null);
            iv_collection.setText(R.string.hb_cancel_collection);
        }
    }

    private void showCollectionDialog(final String playid) {
        new AlertDialog.Builder(mContext)//.setTitle(R.string.hb_cancel_collection)
                // .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.hb_cancel_collection_message).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                MusicUtils.mSongDb.deleteCollectById(playid);
                changeCollectState(playid);
            }

        }).setNegativeButton(R.string.cancel, null).show();
    }

    public void startPlayAnimation(final int postion, final boolean flag) {
        isRunning = true;
        if (aima != null && aima.isStarted()) {
            mAdapter.setCurrentPosition(postion - 1);
            aima.end();
        }
        int[] location = new int[2];
        int[] location1 = new int[2];
        int[] location2 = new int[2];
        int distance = 0; // 移动距离
        mListView.getLocationInWindow(location);
        int currentPosition = mAdapter.getCurrentPosition();
        View arg1 = mListView.getChildAt(postion - mListView.getFirstVisiblePosition());
        if (arg1 == null) {
            mAdapter.setCurrentPosition(postion - 1);
            mListView.invalidateViews();

            if (flag)
                playMusic(mArrayList, postion - 1, 0);
            return;
        }
        arg1.getLocationInWindow(location1);
        startPoint = location1;
        if (currentPosition < 0) {
            // 无动画
            mAdapter.setCurrentPosition(postion - 1);
            mListView.invalidateViews();
            mPlaySelect.postDelayed(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    if (isPause && flag) {
                        playMusic(mArrayList, postion - 1, 0);
                    } else
                        startFly(new HBMusicUtil.AnimationEndListener() {

                            @Override
                            public void onEnd() {
                                // TODO Auto-generated method stub
                                if (flag && mAdapter != null) {
                                    mListView.postDelayed(new Runnable() {
                                        public void run() {
                                            if (flag)
                                                playMusic(mArrayList, postion - 1, 0);
                                        }
                                    }, 100);
                                }
                            }
                        });
                }
            }, 50);
            // if (flag){
            // playMusic(mArrayList, arg2 - 1, 0);
            // }
            return;
        } else if (currentPosition < mListView.getFirstVisiblePosition()) {
            // 从最上面飞进来
            mPlaySelect.setY(-mPlaySelect.getHeight());
            distance = location1[1] - location[1] + mPlaySelect.getHeight();
        } else if (currentPosition > mListView.getLastVisiblePosition()) {
            // 从最下面飞进来
            mPlaySelect.setY(mListView.getBottom());
            distance = mListView.getHeight() - location1[1] + location[1];
        } else {
            // 具体位置飞进
            View view = mListView.getChildAt(currentPosition - mListView.getFirstVisiblePosition());
            view.getLocationInWindow(location2);
            if (currentPosition == 0) {
                mPlaySelect.setY(location2[1] - location[1] + mContext.getResources().getDimension(R.dimen.hb_recommend_toplayout_height)
                        + mContext.getResources().getDimension(R.dimen.hb_netanimation));
            } else if (currentPosition == 1) {
                mPlaySelect.setY(location2[1] - location[1] + mContext.getResources().getDimension(R.dimen.song_itemheight));
            } else {
                mPlaySelect.setY(location2[1] - location[1] + mContext.getResources().getDimension(R.dimen.song_itemheight));
            }
            distance = Math.abs(location2[1] - location1[1]);
        }
        if (postion == 1)
            aima = ObjectAnimator.ofFloat(mPlaySelect, "y", location1[1] - location[1]);
        else
            aima = ObjectAnimator.ofFloat(mPlaySelect, "y", location1[1] - location[1]);
        aima.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator arg0) {
                mPlaySelect.setVisibility(View.VISIBLE);
                mAdapter.setCurrentPosition(-2);
                mListView.invalidateViews();
            }

            @Override
            public void onAnimationRepeat(Animator arg0) {

            }

            @Override
            public void onAnimationEnd(Animator arg0) {
                // playMusic(mArrayList, arg2 - 1, 0);
                mAdapter.setCurrentPosition(postion - 1);
                mListView.invalidateViews();
                mPlaySelect.setVisibility(View.GONE);
                if (isPause && flag) {
                    playMusic(mArrayList, postion - 1, 0);
                } else {
                    startFly(new HBMusicUtil.AnimationEndListener() {

                        @Override
                        public void onEnd() {
                            // TODO Auto-generated method stub
                            if (flag && mAdapter != null) {
                                mListView.postDelayed(new Runnable() {
                                    public void run() {
                                        if (flag)
                                            playMusic(mArrayList, postion - 1, 0);
                                    }
                                }, 100);
                            }
                        }
                    });
                }
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

    public void playMusic(final ArrayList<HBListItem> arrayList, final int position, int reapeat) {
        clickNumber += 1;
        MusicUtils.playAll(mContext, arrayList, position, 0, true);
        isRunning = false;
    }

    public void showAnimation() {
        if (clickNumber == 0 && mAdapter != null) {
            for (int i = 0; i < mArrayList.size(); i++) {
                if (mArrayList.get(i).getSongId() == MusicUtils.getCurrentAudioId()) {
                    startPlayAnimation(i + 1, false);
                    isRunning = false;
                    break;
                }
            }
            mAdapter.notifyDataSetChanged();
        }
        if (clickNumber > 0)
            clickNumber--;
    }

    public void startAnimation(int y, int deltaY) {
        if (needScroll || mAdapter == null || mAdapter.getCount() > 10) {
            return;
        }
        int bottom = headView.getBottom();
        int headerHeight = mContext.getResources().getDimensionPixelSize(R.dimen.hb_detail_margin);
        if (y <= 0 || deltaY < 0) {
            ((HBNetTrackDetailActivity) mContext).changeHeaderStatus(0, alaph);
            ((HBNetTrackDetailActivity) mContext).changeHeaderStatus(3, alaphCount);
        } else if (bottom - y - headerHeight <= 0) {
            ((HBNetTrackDetailActivity) mContext).changeHeaderStatus(0, alaphCount);
            ((HBNetTrackDetailActivity) mContext).changeHeaderStatus(2, alaphCount);
            ((HBNetTrackDetailActivity) mContext).changeHeaderStatus(1, alaphCount);
        }
    }

    public void startFly(HBMusicUtil.AnimationEndListener listener) {
        ((HBNetTrackDetailActivity) mContext).setStartPoint(startPoint[0], startPoint[1]);
        ((HBNetTrackDetailActivity) mContext).startFly(listener);
    }

    public void Destroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        mArrayList.clear();
    }

    private void removeUnuse(List items) {
        OnlineSong item;
        for (int i = items.size() - 1; i >= 0; i--) {
            item = (OnlineSong) items.get(i);
            if (Long.valueOf(item.getSongId()) <= 0) {
                items.remove(i);
            }
        }
    }

    public View getPlaySelect() {
        return mPlaySelect;
    }

    private void getArtist(final long id) {
        iv_share_parent.setEnabled(false);
        iv_share.setEnabled(false);
        mShareUrl = artist_share_prefix + id;
        getThreadPoolExecutor().submit(new Runnable() {

            @Override
            public void run() {

                final OnlineArtist artist = XiaMiSdkUtils.fetchArtistDetailSync(mContext, id);
                if (artist == null) {
                    return;
                }
                mTitleStr = artist.getName();
                final List<OnlineSong> results = XiaMiSdkUtils.fetchSongsByArtistIdSync(mContext, id);
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        mProgressBar.setVisibility(View.GONE);
                        ((HBNetTrackDetailActivity) mContext).hideHeader();
                        if (results != null && results.size() > 0) {
                            mAdapter = new HBRecommendDetailAdapter(mContext, results, mType);
                            mListView.setAdapter(mAdapter);
                            getHBListItems(results);
                            ((HBNetTrackDetailActivity) mContext).setTitle(artist.getName());
                            updateFootview();
                        }
                    }
                });
            }
        });

    }

    private void getSong(final long id) {
        iv_share_parent.setEnabled(false);
        iv_share.setEnabled(false);
        mShareUrl = song_share_prefix + id;
        getThreadPoolExecutor().submit(new Runnable() {

            @Override
            public void run() {
                final OnlineSong song = XiaMiSdkUtils.findSongByIdSync(mContext, id, HBMusicUtil.getOnlineSongQuality());
                if (song == null) {
                    return;
                }
                mTitleStr = song.getSongName();
                final List<OnlineSong> results = new ArrayList<OnlineSong>();
                results.add(song);
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        mProgressBar.setVisibility(View.GONE);
                        ((HBNetTrackDetailActivity) mContext).hideHeader();
                        if (results != null && results.size() > 0) {
                            mAdapter = new HBRecommendDetailAdapter(mContext, results, mType);
                            mListView.setAdapter(mAdapter);
                            getHBListItems(results);
                            ((HBNetTrackDetailActivity) mContext).setTitle(song.getSongName());
                            updateFootview();
                        }
                    }
                });
            }
        });

    }

    public int getTopHeight() {
        if (Build.VERSION.SDK_INT >= 19 && Globals.SWITCH_FOR_TRANSPARENT_STATUS_BAR) {
            return mContext.getResources().getDimensionPixelOffset(R.dimen.hb_action_bar_height);
        }
        return 0;
    }
}
