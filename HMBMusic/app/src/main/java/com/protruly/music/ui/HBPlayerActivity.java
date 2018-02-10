package com.protruly.music.ui;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.EdgeEffectCompat;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.protruly.music.Application;
import com.protruly.music.IMediaPlaybackService;
import com.protruly.music.MediaPlaybackService;
import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.RepeatingImageButton;
import com.protruly.music.adapter.HBPlayRadioAdapter;
import com.protruly.music.adapter.HBPlayerListViewAdapter;
import com.protruly.music.adapter.HBPlayerPagerAdapter;
import com.protruly.music.db.HBMusicInfo;
import com.protruly.music.downloadex.BitmapUtil;
import com.protruly.music.downloadex.DownloadInfo;
import com.protruly.music.downloadex.DownloadManager;
import com.protruly.music.downloadex.DownloadStatusListener;
import com.protruly.music.downloadex.DownloadTask;
import com.protruly.music.model.HBAnimationModel;
import com.protruly.music.model.OTAFrameAnimation;
import com.protruly.music.model.OTAMainPageFrameLayout;
import com.protruly.music.model.XiaMiSdkUtils;
import com.protruly.music.online.HBSearchLyricActivity;
import com.protruly.music.share.HBShareXLWb;
import com.protruly.music.share.HBWxShare;
import com.protruly.music.util.Blur;
import com.protruly.music.util.DialogUtil;
import com.protruly.music.util.DisplayUtil;
import com.protruly.music.util.FlowTips;
import com.protruly.music.util.Globals;
import com.protruly.music.util.HBIListItem;
import com.protruly.music.util.HBListItem;
import com.protruly.music.util.IntentFactory;
import com.protruly.music.util.LogUtil;
import com.protruly.music.util.HBMusicUtil;
import com.protruly.music.MusicUtils.ServiceToken;
import com.protruly.music.util.ThreadPoolExecutorUtils;
import com.protruly.music.widget.HBAnimationImageView;
import com.protruly.music.widget.HBImageButton;
import com.protruly.music.widget.HBLyricSingleView;
import com.protruly.music.widget.HBLyricView;
import com.protruly.music.widget.HBPlayListPopuWindow;
import com.protruly.music.widget.HBPlayListPopuWindow.AnimStyle;
import com.protruly.music.widget.HBPlayListPopuWindow.ItemClickCallBack;
import com.protruly.music.widget.HBScrollView;
import com.protruly.music.widget.HBViewPager;
import com.protruly.music.ui.HBDialogFragment.HBDilogCallBack;
import com.xiami.sdk.entities.OnlineSong;
import com.xiami.sdk.utils.ImageUtil;
import com.protruly.music.model.HBAnimationModel.OnAnimationListener;
import com.protruly.music.util.FlowTips.OndialogClickListener;
import com.protruly.music.share.HBShareXLWb.HBWeiBoCallBack;
import com.protruly.music.downloadex.db.Dao;

import org.apache.http.HttpStatus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;



/**
 * Created by hujianwei on 17-8-31.
 */

public class HBPlayerActivity extends AbstractBaseActivity implements OnAnimationListener {

    private static final String TAG = "HBPlayerActivity";
    private boolean mbShowPagerBk = false;


    private HBPlayerPagerAdapter mPagerAdapter = null;
    private EdgeEffectCompat mleftCompat;
    private EdgeEffectCompat mrightCompat;
    private OTAMainPageFrameLayout mPrevButton;

    private final static int HB_PLAYER_SHOW_LRC_AND_ALBUM = 0x01;

    private final static int HB_PLAYER_SHOW_LRC_ONLY = 0x02;

    //显示模式 1显示歌词跟专辑 2显示歌词
    private static  int mPlayViewMode = HB_PLAYER_SHOW_LRC_AND_ALBUM;

    private OTAMainPageFrameLayout mPauseButton;
    private OTAMainPageFrameLayout mNextButton;
    private IMediaPlaybackService mService = null;
    private ImageButton mShuffleButton;
    private ImageButton mLoveButton;
    private ImageButton mPlayListButton;
    private ImageButton mBackButton;
    private ImageButton mShareButton;
    private ServiceToken mToken;
    private Toast mToast;
    //private View mDotLayout;
    private boolean mPlayPause = false;
    private HbWorker mAlbumArtWorker;
    private AlbumArtHandler mAlbumArtHandler;

    private HBAnimationImageView mAlbum = null;
    private HBLyricView mLyricView;

    //add by hujianwei 20170915 start
    private HBLyricSingleView mLyricSingleView;
    private HBPlayListPopuWindow mHBPlayListPopuWindow;
    //add by hujianwei 20170915 end


    private View mPlayerAlbumLayout = null;
    private View mPlayerLrcLayout = null;
    private TextView mArtistName;
    private TextView mAlbumName;
    private TextView mTrackName;
    private TextView mCurrentTime;
    private TextView mTotalTime;
    private SeekBar mProgress;
    private SeekBar mVoicControlSeekBar;
    private  AudioManager mAudioManager;
    private boolean mbUriPath = false;
    private int mPlayMode = 0;
    private int mCurrentPlaying = -1;
    private long mDuration;
    private long mPosOverride = -1;
    private long mLastSeekEventTime;
    private long mStartSeekPos = 0;
    private boolean mSeeking = false;
    private boolean mFromTouch = false;
    private boolean paused;
    private boolean onPaused = false;
    private boolean mFromNotification = false;
  
    
    public final static String ACTION_FROM_MAINACTIVITY = "android.intent.action.mainactivity";
    private final static String ACTION_FROM_URI = "android.intent.action.VIEW";
    private static final String ACTION_CHANGE_STATUSBAR_BG = "hb.action.CHANGE_STATUSBAR_BG";
    private boolean mFromMain = false;
    private boolean mFromUri = false;
    private LinearLayout mViewLayout;
    private FrameLayout mBackGroundLayout;
    private FrameLayout mFullLayout;
    private FrameLayout mPlayerContainer;
    private int mNavigationBar;
    private ImageView mBackGroundView;
    private Drawable mDefautDrawable = null;
    private HBScrollView mLycScrollView = null;
    private Bitmap mDefautBitmap = null;
    private int mPaddingOffset = 0;
    private Bitmap mDefaultBg = null;



    private int mPagePos = 1;
    private boolean isShowAnimator = false;

    private HBImageButton mSongLayout = null;
    private HBImageButton mArtistLayout = null;
    private HBImageButton mRingerLayout = null;
    private HBImageButton mSoundLayout = null;

    private HBImageButton mDownLoadLayout = null;
    private boolean mbDowned = false;
    private boolean mbRepeat = false;
    private boolean mbSelected = false;
    private boolean mbNotValid = false;
    private boolean mbSmallSize = false;

    private HBAnimationModel mAnimationModel1 = null;
    private HBAnimationModel mAnimationModel2 = null;
    private boolean mAnimationStop = false;
    private ImageView mNote1;
    private ImageView mNote2;
    private ScaleAnimation inScaleAnimation;
    private ScaleAnimation outScaleAnimation;
    private AlphaAnimation inAnimation;
    private AnimationSet mAnimationSet;
    private AlphaAnimation outAlphaAnimation;
    private ImageView mAnimView = null;
    private Bitmap mAnimationBitmap = null;
    private boolean mbFirstAnim = true;
    private boolean mOnCreate = false;
    private boolean mFirstAnim = false;

    private long firstAid;
    private long firstSid;
    private ImageView mLrcBlur1 = null;
    private ImageView mLrcBlur2 = null;
    private Uri mPlayUri = null;
    private boolean misMms = false;
    private int offset = 0;


    private String mTitleNameStr;
    private String mArtistNameStr;

    private String mDefaultPicUri = "http_pic";
    private HBListItem mItemInfo = null;

    private String mPicurl = null;
    private boolean mBtnDown = false;


    private HBWxShare mWxShare = null;
    private int mWidth = 0;
    private DisplayImageOptions mOptions = null;
    private static final int HB_REFRESH = 1;
    private static final int HB_REFRESH_LRC = 19;
    private static final int HB_QUIT = 2;
    private static final int HB_GET_ALBUM_ART = 3;
    private static final int HB_ALBUM_ART_DECODED = 4;
    private static final int HB_REFRESH_LYRIC = 5;
    private static final int HB_REFRESH_LISTVIEW = 6;
    private static final int HB_ALBUMBG_DECODED = 7;
    private static final int HB_REFRESH_LISTVIEW_BYURI = 8;
    private static final int HB_DB_CHANGED = 9;

    private static final int HB_ANIMATION_STASRT = 13;
    private static final int HB_GET_ALBUM_ARTDEFAULT = 14;
    private static final int HB_GET_ALBUM_ERROR = 15;
    private static final int HB_META_CHANGED = 16; 
    private static final int HB_SEEK = 17;  
    private static final int HB_STOPTRACKINGTOUCH = 18;
    private static final int HB_REFRESH_SINGLE_LYRIC = 20;
    private static final int HB_REFRESH_SINGLE_LRC = 21;
    private ImageView mSearchLyric;
    private List<String> mPathsXml = new ArrayList<String>();
    private SearchImgTask mSearchImgTask;
    private LoadLrtThread mLoadLrtThread = null;
    private boolean isdestory = false;
    private boolean isRadioType = false;

    @Override
    protected void onNewIntent(Intent intent) {
        LogUtil.d(TAG, "onNewIntent");
        setIntent(intent);
        HBShareXLWb.getInstance(getApplicationContext()).handleWeiboResponse(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.d(TAG, "onCreate()");
        isdestory = false;
        mFromMain = false;

        mbSelected = true;
        mbNotValid = false;
        mbUriPath = false;
        mFromUri = false;
        mPlayUri = null;
        misMms = false;
        mbSmallSize = false;
        mPlayPause = false;
        mFirstAnim = false;
        mPicurl = null;
        offset = DisplayUtil.dip2px(this, 6f);
        Intent tIntent = getIntent();
        if (tIntent != null) {
            if (ACTION_FROM_MAINACTIVITY.equalsIgnoreCase(tIntent.getAction())) {
                mFromMain = true;
            } else if (ACTION_FROM_URI.equalsIgnoreCase(tIntent.getAction())) {
                mFromUri = true;
                MusicUtils.registerDbObserver(this);
            }
        }
        mPathsXml = HBMusicUtil.doParseXml(this, "paths.xml");
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        HBMusicUtil.initData(this);

        setContent();
        initViews();
        findViews();
        initData();

        initNotify();
        mOnCreate = true;
    }

    // 透明通知栏
    private void initNotify() {
        if (Build.VERSION.SDK_INT >= 19 && Globals.SWITCH_FOR_TRANSPARENT_STATUS_BAR) {
            RelativeLayout layoutView = (RelativeLayout) findViewById(R.id.title_layout);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layoutView.getLayoutParams();
            params.topMargin = getResources().getDimensionPixelOffset(R.dimen.hb_action_bar_height);
            layoutView.setLayoutParams(params);
            findViewById(R.id.id_actionbar_bg).setVisibility(View.GONE);
        }
    }

    private void initData() {
        mOptions = new DisplayImageOptions.Builder().cacheInMemory(true).cacheOnDisk(true).considerExifParams(true)
                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2).bitmapConfig(Bitmap.Config.RGB_565).build();
    }

    @Override
    public void onMediaDbChange(boolean selfChange) {

    }


    //加载view
    protected void setContent() {

        LogUtil.d(TAG, "setContent.");

        mAlbumArtWorker = new HbWorker("HB Album Art Worker.");

        mAlbumArtHandler = new AlbumArtHandler(mAlbumArtWorker.getLooper());

        setContentView(R.layout.hb_player);

        mBackGroundLayout = (FrameLayout) findViewById(R.id.layout_bg);

        mFullLayout = (FrameLayout) findViewById(R.id.my_layout_bg);

        mNavigationBar = HBMusicUtil.getNavigationBarPixelHeight(HBPlayerActivity.this);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, getWindowManager().getDefaultDisplay().getHeight() + mNavigationBar);

        mBackGroundLayout.setLayoutParams(lp);

        mPaddingOffset = DisplayUtil.dip2px(this, 2.0f);

        mWidth = DisplayUtil.dip2px(this, 288f);

        mBackGroundView = (ImageView) findViewById(R.id.layout_bg_image);

        if (mDefaultBg == null) {
            mDefaultBg = BitmapUtil.decodeSampledBitmapFromResource(getResources(), R.drawable.default_back, 100, 100);
        }
        if (mBackGroundView != null) {
            mBackGroundView.setScaleType(ImageView.ScaleType.FIT_XY);
            mBackGroundView.setImageBitmap(mDefaultBg);
        }
        if (mDefautBitmap == null) {
            mDefautBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_album_bg);
        }

        mFullLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

    }

    //页面初始化
    protected void initViews() {

        LogUtil.d(TAG, "initViews.");

        LayoutInflater inflater = LayoutInflater.from(this);

        mPlayerAlbumLayout = inflater.inflate(R.layout.hb_viewpager_player, null);

        mPlayerLrcLayout = inflater.inflate(R.layout.hb_viewpager_lrc, null);

        mPlayerContainer = (FrameLayout)findViewById(R.id.hb_player_container);

        mPlayerContainer.addView(mPlayerAlbumLayout);

        mPlayerContainer.addView(mPlayerLrcLayout);

        initLyricSingleView(mPlayerAlbumLayout);

        initLyricView(mPlayerLrcLayout);

        initPlayListPopWindow();

    }

    /**
     * 查找页面控件
     */
    protected void findViews() {
        LogUtil.d(TAG, "findViews.");

        //音符
        mNote1 = (ImageView) findViewById(R.id.img_note1);
        mNote2 = (ImageView) findViewById(R.id.img_note2);

        Drawable d = this.getResources().getDrawable(R.drawable.hb_note1);
        mCenterWidth1 = d.getIntrinsicWidth() / 2;
        mCenterHeight1 = d.getIntrinsicHeight() / 2;

        Drawable d2 = this.getResources().getDrawable(R.drawable.hb_note2);
        mCenterWidth2 = d2.getIntrinsicWidth() / 2;
        mCenterHeight2 = d2.getIntrinsicHeight() / 2;

        //歌手
        mArtistName = (TextView) findViewById(R.id.song_text);
        //专辑名称
        mAlbumName = (TextView) findViewById(R.id.album_text);

        //进度条
        mProgress = (SeekBar) findViewById(R.id.hb_progress);
        mProgress.setOnSeekBarChangeListener(mSeekBarListener);
        mProgress.setMax(1000);

        //音量控制
        mVoicControlSeekBar = (SeekBar) findViewById(R.id.lyric_hb_voice_progress);
        mVoicControlSeekBar.setOnSeekBarChangeListener(mVoiceControlSeekBarChangeListener);

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        //获取系统最大音量
        mVoicControlSeekBar.setMax( mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        mVoicControlSeekBar.setProgress(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));

        //播放时间控制
        mCurrentTime = (TextView) findViewById(R.id.hb_currenttime);
        mTotalTime = (TextView) findViewById(R.id.hb_totaltime);

        //前一首
        mPrevButton = (OTAMainPageFrameLayout) findViewById(R.id.hb_prev);
        mPrevButton.setOnTouchListener(onButtonListener);

        //暂停
        mPauseButton = (OTAMainPageFrameLayout) findViewById(R.id.hb_pause);
        mPauseButton.requestFocus();
        mPauseButton.setOnTouchListener(onButtonListener);

        //后一首
        mNextButton = (OTAMainPageFrameLayout) findViewById(R.id.hb_next);
        mNextButton.setOnTouchListener(onButtonListener);

        //播放模式
        mShuffleButton = (ImageButton) findViewById(R.id.hb_shuffle);
        mShuffleButton.setOnClickListener(mShuffleListener);

        //收藏
        mLoveButton = (ImageButton) findViewById(R.id.hb_love);
        mLoveButton.setOnClickListener(mLoveListener);

        //播放列表
        mPlayListButton = (ImageButton) findViewById(R.id.hb_play_list);
        mPlayListButton.setOnClickListener(mPlayListListener);

        //返回
        mBackButton = (ImageButton) findViewById(R.id.img_bt_back);
        mBackButton.setOnClickListener(mBackListener);

        //分享
        mShareButton = (ImageButton) findViewById(R.id.img_bt_share);
        mShareButton.setOnClickListener(mShareListener);


        if (mPlayerAlbumLayout != null) {
            mAlbum = (HBAnimationImageView) (mPlayerAlbumLayout.findViewById(R.id.image_player_album));

            if( mAlbum != null ){
                mAlbum.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        LogUtil.d(TAG, "mAlbum onClick");

                        mPlayViewMode = HB_PLAYER_SHOW_LRC_ONLY;

                        if( mPlayerLrcLayout.getVisibility() !=  View.VISIBLE){

                            //HIDE ABLUM
                            mPlayerAlbumLayout.setVisibility(View.INVISIBLE);

                            mHandler.removeMessages(HB_REFRESH_LYRIC);

                            mHandler.obtainMessage(HB_REFRESH_LYRIC).sendToTarget();

                            updateHbTrackInfo();

                            //SHOW LRC
                            mPlayerLrcLayout.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }

            mAnimView = (ImageView) (mPlayerAlbumLayout.findViewById(R.id.image_player_anim));
            mAnimView.setImageBitmap(mDefautBitmap);

            onFirstAnimation(mAnimView);

        }


        if (mPlayerLrcLayout != null) {
            mLrcBlur1 = (ImageView) (mPlayerLrcLayout.findViewById(R.id.lrc_image_blur1));
        }

    }

    private void initLyricSingleView(View singleLrcView){
        if( singleLrcView == null){
            return;
        }
        mLyricSingleView = (HBLyricSingleView) (singleLrcView.findViewById(R.id.lyric_single_view));
        mLyricSingleView.setHasLyric(false);

    }


    private void initLyricView(View lrcView) {
        if (lrcView == null) {
            return;
        }

        mLyricView = (HBLyricView) (lrcView.findViewById(R.id.lyric_view));
        mLycScrollView = (HBScrollView) (lrcView.findViewById(R.id.lyric_scrollview));
        mLyricView.setHasLyric(false);
        mLyricView.setFirstScroll();

        mLyricView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mPlayViewMode = HB_PLAYER_SHOW_LRC_AND_ALBUM;

                if( mPlayerAlbumLayout.getVisibility() !=  View.VISIBLE){

                    //HDIE LRC
                    mPlayerLrcLayout.setVisibility(View.INVISIBLE);

                    updateHbTrackInfo();

                    //SHOW ABLUM
                    mPlayerAlbumLayout.setVisibility(View.VISIBLE);

                    mHandler.removeMessages(HB_REFRESH_SINGLE_LRC);

                    mHandler.obtainMessage(HB_REFRESH_SINGLE_LRC).sendToTarget();
                }

            }
        });

        mLycScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (mService != null) {
                    boolean tplaying = false;
                    try {
                        tplaying = mService.isPlaying();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mLyricView.setTouchMode(v, event, tplaying);
                } else {
                    mLyricView.setTouchMode(v, event, false);
                }


                return false;
            }
        });

        mLycScrollView.setScrollViewListener(mLyricView);

        mPlayerLrcLayout.setVisibility(View.INVISIBLE);

        return;
    }

    void initPlayListPopWindow(){

        mHBPlayListPopuWindow = new HBPlayListPopuWindow(this, R.style.exit_animation, new ItemClickCallBack() {
            @Override
            public void callBack(int position) {
                LogUtil.d(TAG, " click :position" + position);
            }
            @Override
            public void toggleShuffleButton() {
                //更新播放模式
                hbToggleShuffle();

                updateHbTrackInfo();
            }
        },null);


    }

    private ArrayList<HBListItem> mPlayerListData = null;

    static final String[] mCursorCols = { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DURATION };

    private void updateLoveButton(long id) {
        if (mService != null) {
            try {
                boolean bfavorite = false;
                if (id > 0) {
                    bfavorite = mService.isFavorite(id);
                } else {
                    bfavorite = mService.isFavorite(mService.getAudioId());
                }
                if (bfavorite) {
                    mLoveButton.setImageResource(R.drawable.hb_play_love_select);
                } else {
                    mLoveButton.setImageResource(R.drawable.hb_play_love);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }


    private void updateNotValidImageButton() {
        String tfilename = "";
        if (mbUriPath) {
            mbNotValid = false;

        } else {
            try {
                tfilename = mService.getFilePath();
                if (!TextUtils.isEmpty(tfilename)) {
                    if ((tfilename.equalsIgnoreCase("default net") || tfilename.toLowerCase().startsWith("http://"))) {
                        mbNotValid = false;
                    } else {
                        File f1 = new File(tfilename);
                        if (!f1.exists()) {
                            mbNotValid = true;
                            return;
                        } else {
                            mbNotValid = (f1.length() < Globals.FILE_SIZE_FILTER) ? true : false;
                        }
                    }
                }else {
                    mbNotValid = true;
                }
            } catch (Exception e) {
                mbNotValid = true;
                e.printStackTrace();
            }
        }
        mFromUri = false;
        if (mbNotValid) {
            if (mLoveButton != null) {
                mLoveButton.setAlpha(0.2f);
                mLoveButton.setEnabled(false);
            }
            if (mFromUri) {
                if (mShuffleButton != null) {
                    mShuffleButton.setAlpha(0.2f);
                    mShuffleButton.setEnabled(false);
                }
                if (mNextButton != null) {
                    mNextButton.setAlpha(0.2f);
                    mNextButton.setEnabled(false);
                }
                if (mPrevButton != null) {
                    mPrevButton.setAlpha(0.2f);
                    mPrevButton.setEnabled(false);
                }
            }
        } else {
            if (misMms || mbSmallSize) {
                mLoveButton.setAlpha(0.2f);
                mLoveButton.setEnabled(false);
            } else {
                mLoveButton.setAlpha(1f);
                mLoveButton.setEnabled(true);
            }
            if (mFromUri) {
                mShuffleButton.setAlpha(1f);
                mShuffleButton.setEnabled(true);
                mNextButton.setAlpha(1f);
                mNextButton.setEnabled(true);
                mPrevButton.setAlpha(1f);
                mPrevButton.setEnabled(true);
            }
        }


        if (isRadioType) {
            if (mShuffleButton != null) {
                mShuffleButton.setAlpha(0.2f);
                mShuffleButton.setEnabled(false);
            }
            if (mPrevButton != null) {
                mPrevButton.setAlpha(0.2f);
                mPrevButton.setEnabled(false);
            }
        } else {
            if (mShuffleButton != null) {
                mShuffleButton.setAlpha(1f);
                mShuffleButton.setEnabled(true);
            }
            if (mPrevButton != null) {
                mPrevButton.setAlpha(1f);
                mPrevButton.setEnabled(true);
            }
        }

        return;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mbRepeat = false;
        paused = false;

        //绑定播放服务
        mToken = MusicUtils.bindToService(this, HbOsc);

        if (mToken == null) {
            mHandler.sendEmptyMessage(HB_QUIT);
        }


        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.QUEUE_CHANGED);
        f.addAction("android.media.VOLUME_CHANGED_ACTION");

        registerReceiver(mStatusListener, f);

        registerReceiver(mVolumeListenerReceiver, f );

        long next = refreshHbNow();

        queueHbNextRefresh(next);
    }

    @Override
    protected void onResume() {
        LogUtil.d(TAG, "onResume.");
        super.onResume();
        mAnimationStop = false;
        mPlayPause = false;
        if (mSeekBarWidth == 0) {

            mSeekBarstart = mProgress.getPaddingLeft() + mProgress.getThumbOffset();
            mSeekBarWidth = getWindowManager().getDefaultDisplay().getWidth() - mSeekBarstart - mProgress.getPaddingRight();
            mStarth = (float) mSeekBarWidth / 1000;
        }
        if (mAnimationModel1 == null) {
            mAnimationModel1 = HBAnimationModel.createAnimation(mNote1);
            mAnimationModel1.setFillAfter(true);
            mAnimationModel1.setMyAnimationListener(this);
            mAnimationModel1.setDuration(3000);
            mAnimationModel1.setInterpolator(new AccelerateInterpolator());
        }
        if (mAnimationModel2 == null) {
            mAnimationModel2 = HBAnimationModel.createAnimation(mNote2);
            mAnimationModel2.setFillAfter(true);
            mAnimationModel2.setMyAnimationListener(this);
            mAnimationModel2.setDuration(3000);
            mAnimationModel2.setInterpolator(new AccelerateInterpolator());
        }

        if (mLyricView != null) {
            mLyricView.setIsTouchScrollView(false);
        }

        setPauseButtonImage();
        updateHbTrackInfo();
        onStopPlayAnimation(false);
        mWxShare = HBWxShare.getInstance(this);
        mWxShare.setHandleIntentAndCallBack(getIntent(), null);
   
    }

    @Override
    protected void onPause() {
        LogUtil.d(TAG, "onPause.");
        super.onPause();
        onPaused = true;
        mOnCreate = false;
        if (misMms && mService != null) {
            try {
                mService.pause();
            } catch (Exception e) {
            }
        }
        onStopPlayAnimation(true);
        if (mAlbum != null) {
            mAlbum.setStartAnimation(false);
        }
        DownloadManager.getInstance(this).clearListenerMap();
        clearData();
    }

    @Override
    protected void onStop() {
        LogUtil.d(TAG, "onStop.");
        paused = true;
        mAlbumArtHandler.removeCallbacksAndMessages(null);
        mHandler.removeCallbacksAndMessages(null);
        try {
            unregisterReceiver(mStatusListener);
            unregisterReceiver(mVolumeListenerReceiver);
        } catch (Exception e) {
        }
        MusicUtils.unbindFromService(mToken);
        mService = null;
        HBAnimationModel.clear();
        super.onStop();
    }

    private void clearData() {
        HBWxShare.unRegisterApp();
        mWxShare = null;
        return;
    }

    @Override
    protected void onDestroy() {

        
        LogUtil.d(TAG, "onDestroy.");
        mPlayUri = null;
        mbSelected = false;
        mFromMain = false;
        mbNotValid = false;
        isdestory = true;

        
        HBDialogFragment.registerItemClickCallback(null);
        HBShareXLWb.getInstance(this).clearData();
        if (mPauseButton != null) {
            mPauseButton.stopAnim();
        }
        if (mLoveButton != null) {
            mLoveButton.clearAnimation();
        }
        if (mAnimView != null) {
            mAnimView.clearAnimation();
        }
        if (mNote1 != null) {
            mNote1.clearAnimation();
        }
        if (mNote2 != null) {
            mNote2.clearAnimation();
        }
        if (mAlbum != null) {
            mAlbum.clearAnimation();
        }
        if (mAlbumArtWorker != null) {
            mAlbumArtWorker.quit();
        }
        if (mAnimationBitmap != null && !mAnimationBitmap.isRecycled()) {
            mAnimationBitmap.recycle();
        }
        if (mDefaultBg != null && !mDefaultBg.isRecycled()) {
            mDefaultBg.recycle();
            mDefaultBg = null;
        }
        if (mDefautBitmap != null && !mDefautBitmap.isRecycled()) {
            mDefautBitmap.recycle();
            mDefautBitmap = null;
        }

        super.onDestroy();
    }

    static final String[] ARTIST_CLOS = { MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.ARTIST,
            "COUNT(*) AS num_hb" };
    private static final String BUCKET_GROUP_BY_IN_TABLE = "2) GROUP BY (2";




    private void queueHbNextRefresh(long delay) {
        if (!paused) {
            Message msg = mHandler.obtainMessage(HB_REFRESH);
            mHandler.removeMessages(HB_REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }

    private void restartNoteAnima(boolean flag) {
        mAnimationStop = !flag;
        if (flag) {
            LogUtil.d(TAG, "restartNoteAnima");
            ani1 = false;
            ani2 = false;
            mNote1.startAnimation(mAnimationModel1);
        } else {
            mNote1.setVisibility(View.GONE);
            mNote2.setVisibility(View.GONE);
        }
        return;
    }

    private boolean onPauseTouch(View v, MotionEvent event) {
        if (mService == null) {
            return false;
        }
        boolean pause = false;
        try {
            if (mService.isPlaying()) {
                pause = true;
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "onPauseTouch error", e);
            return false;
        }
        int action = event.getAction();
        LogUtil.d(TAG, "onPauseTouch 3 pause:" + pause + ",action:" + action);
        if (action == MotionEvent.ACTION_UP) {
            doPauseResume();
        } else if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_CANCEL) {
            if (pause) {
                //((OTAMainPageFrameLayout) v).setBackgroundResource(R.drawable.hb_play_select);
            } else {
                //((OTAMainPageFrameLayout) v).setBackgroundResource(R.drawable.hb_pause_select);
            }
        }
        return true;
    }

    private void onNextButtonAnimation(final OTAMainPageFrameLayout view, final boolean isNextButton) {
        if (view == null) {
            return;
        }
        mPauseButton.stopAnim();
        mNextButton.stopAnim();
        mPrevButton.stopAnim();

        LogUtil.d(TAG, "1onNextButtonAnimation.");
        view.setAnimationListener(new OTAFrameAnimation.AnimationImageListener() {
            @Override
            public void onRepeat(int repeatIndex) {}

            @Override
            public void onFrameChange(int repeatIndex, int frameIndex, int currentTime) {}

            @Override
            public void onAnimationStart() {}

            @Override
            public void onAnimationEnd() {
                if (isNextButton) {
                    view.setBackgroundResource(R.drawable.hb_next);
                    nextClicked();
                } else {
                    LogUtil.d(TAG, "2onNextButtonAnimation.");
                    view.setBackgroundResource(R.drawable.hb_prev);
                    prevClicked();
                }
            }
        });
        if (isNextButton) {
            view.setFrameAnimationList(R.drawable.next_play_anima);
        } else {
            view.setFrameAnimationList(R.drawable.prev_play_anima);
        }
        view.startAnim();
        return;
    }

    private boolean onNextTouch(View v, MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_UP) {
            onNextButtonAnimation(mNextButton, true);
        } else if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_CANCEL) {
            //((OTAMainPageFrameLayout) v).setBackgroundResource(R.drawable.hb_next_select);
        }
        return true;
    }

    private boolean onPrevTouch(View v, MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP) {
            onNextButtonAnimation(mPrevButton, false);
        } else if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_CANCEL) {

        }
        return true;
    }

    private final View.OnTouchListener onButtonListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(final View v, final MotionEvent event) {
            if (mService == null) {
                return false;
            }
            boolean isAction = false;
            LogUtil.d(TAG, "onButtonListener");

            if (mPauseButton == v) {
                return onPauseTouch(v, event);
            } else if (mNextButton == v) {
                return onNextTouch(v, event);
            } else if (mPrevButton == v) {
                return onPrevTouch(v, event);
            }
            return false;
        }
    };

    private void setPauseButtonImage() {
        try {
            boolean flag = false;

            if (mService != null && mService.isPlaying() && mPauseButton != null) {
                flag = true;
                LogUtil.i(TAG, "setPauseButtonImage 1 mPlayPause:" + mPlayPause);
                if (mPlayPause) {
                    onRotationAnimation(mPauseButton, flag);
                } else {
                    mPauseButton.stopAnim();
                    mPauseButton.setBackgroundResource(R.drawable.hb_play);
                }
            } else {
                LogUtil.i(TAG, "setPauseButtonImage 2 mPlayPause:" + mPlayPause);
                if (mPlayPause) {
                    onRotationAnimation(mPauseButton, flag);
                } else {
                    mPauseButton.stopAnim();
                    mPauseButton.setBackgroundResource(R.drawable.hb_pause);
                }
            }
            if (mAlbum != null) {
                mAlbum.setStartAnimation(flag);
            }
            restartNoteAnima(flag);
        } catch (Exception ex) {
            LogUtil.e(TAG, "setPauseButtonImage error", ex);
        }
    }

    private long refreshHbNow() {
        if (mService == null)
            return 500;
        if (mFromTouch) {
            LogUtil.d(TAG, "seeking");
            return 500;
        }
        try {
            mDuration = mService.duration();
            if (mDuration == -1) {

                return 500;
            }
            mTotalTime.setText(MusicUtils.makeTimeString(this, mDuration / 1000));
            long pos = mPosOverride < 0 ? mService.position() : mPosOverride;
            if ((pos >= 0) && (mDuration > 0)) {
                mCurrentTime.setText(MusicUtils.makeTimeString(this, pos / 1000));
                int progress = (int) (1000 * pos / mDuration);
                //LogUtil.d(TAG, "progress:" + progress);
                mProgress.setProgress(progress);
                if (mItemInfo != null && mItemInfo.getIsDownLoadType() == 1) {
                    mProgress.setSecondaryProgress((int) mService.secondaryPosition() * 10);
                }
                if (mLyricView != null) {
                    Message msg = mHandler.obtainMessage(HB_REFRESH_LRC);
                    mHandler.sendMessage(msg);
                }

                if( mLyricSingleView !=null ){
                    Message msg = mHandler.obtainMessage(HB_REFRESH_SINGLE_LRC);
                    mHandler.sendMessage(msg);
                }

                mDistance = progress * mStarth;
            } else {
                mCurrentTime.setText("00:00");
                LogUtil.d(TAG, "progress000");
                mProgress.setProgress(0);
            }

            long remaining = 1000 - (pos % 1000);

            int width = mProgress.getWidth();
            if (width == 0)
                width = 320;
            long smoothrefreshtime = mDuration / width;
            if (smoothrefreshtime > remaining)
                return remaining;
            if (smoothrefreshtime < 20)
                return 20;
            return smoothrefreshtime;
        } catch (Exception e) {
            Log.i(TAG, " refreshHbNow fail ");
            e.printStackTrace();
        }
        return 500;
    }

    /**
     * 立即刷新歌词
     */
    private void refreshHbLrcNow() {
        if( mLyricSingleView == null ){
            return;
        }
        mLyricSingleView.setRefreshNow(true);
        refreshHbSingleLrc(true);

        if (mLyricView == null) {
            return;
        }
        mLyricView.setRefreshNow(true);
        refreshHbLrc(true);


    }

    /**
     * 用于刷新歌词
     */
    private int refreshHbLrc(boolean isFrist) {

        try {
            if (mService == null) {
                return 500;
            }
            mDuration = mService.duration();
            if (mDuration == -1 || mDuration == 0) {
                return 500;
            }
            long pos = mPosOverride < 0 ? mService.position() : mPosOverride;
            if (mLyricView != null) {
                return mLyricView.setCurrentIndex((int) pos);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            LogUtil.e(TAG, "refreshHbLrc error", e);
        }
        return 500;
    }

    private int refreshHbSingleLrc(boolean isFrist) {

        try {
            if (mService == null) {
                return 500;
            }
            mDuration = mService.duration();
            if (mDuration == -1 || mDuration == 0) {
                return 500;
            }
            long pos = mPosOverride < 0 ? mService.position() : mPosOverride;
            if (mLyricSingleView != null) {
                return mLyricSingleView.setCurrentIndex((int) pos);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            LogUtil.e(TAG, "refreshHbLrc error", e);
        }
        return 500;
    }

    private void queueHbNextRefreshLrc(long nextLrc) {
        if (!paused) {
            Message msg = mHandler.obtainMessage(HB_REFRESH_LRC);
            mHandler.removeMessages(HB_REFRESH_LRC);
            mHandler.sendMessageDelayed(msg, nextLrc);
        }
    }


    private void queueHbNextRefreshSingleLrc(long nextLrc) {
        if (!paused) {
            Message msg = mHandler.obtainMessage(HB_REFRESH_SINGLE_LRC);
            mHandler.removeMessages(HB_REFRESH_SINGLE_LRC);
            mHandler.sendMessageDelayed(msg, nextLrc);
        }
    }

    private void updateHbTrackInfo() {
        if (mService == null) {
            return;
        }

        try {
            if (mPlayerListData != null && mPlayerListData.size() > 0) {
                long songid1 = mService.getAudioId();

                LogUtil.d(TAG, "updateHbTrackInfo songid:" + songid1);
                if (songid1 >= 0) {

                    HBListItem item = null;

                    int len = mPlayerListData.size();

                    LogUtil.d(TAG, "updateHbTrackInfo len:" + len);

                    for (int i = 0; i < len; i++) {
                        HBListItem tmpitem = mPlayerListData.get(i);
                        if (songid1 == tmpitem.getSongId()) {
                            item = tmpitem;
                            LogUtil.d(TAG, "lrc:" + item.getLrcUri());
                            break;
                        }
                    }

                    String artistName1 = "";
                    String albumName1 = "";

                    long albumid1 = mService.getAlbumId();

                    LogUtil.d(TAG, "updateHbTrackInfo albumid1:" + albumid1);

                    if (item == null) {
                        String tmp = getString(R.string.unknown_artist_name);
                        mArtistName.setText(tmp);
                        artistName1 = getString(R.string.unknown_artist_name);
                        albumName1 = getString(R.string.unknown_album_name);
                        albumid1 = -1;

                        mTitleNameStr = tmp;
                    } else {
                        mArtistName.setText(item.getTitle());
                        artistName1 = item.getArtistName();
                        if (artistName1 == null || artistName1.equals(MediaStore.UNKNOWN_STRING)) {
                            artistName1 = getString(R.string.unknown_artist_name);
                        }
                        albumName1 = item.getAlbumName();
                        albumName1 = HBMusicUtil.doAlbumName(item.getFilePath(), albumName1);
                        if (albumName1 == null || albumName1.equals(MediaStore.UNKNOWN_STRING)) {
                            albumName1 = getString(R.string.unknown_album_name);
                        }

                        mTitleNameStr = item.getTitle();
                    }

                    mArtistNameStr = artistName1;
                    mItemInfo = item;

                    LogUtil.d(TAG, "mItemInfo:" + mItemInfo.getSongId());
                    StringBuffer tBuffer = new StringBuffer();
                    tBuffer.append(artistName1);

                    mAlbumName.setText(tBuffer.toString());

                    if (!mbFirstAnim) {
                        mAlbumArtHandler.removeMessages(HB_GET_ALBUM_ART);
                        mAlbumArtHandler.obtainMessage(HB_GET_ALBUM_ART, new HbAlbumSongIdWrapper(albumid1, songid1)).sendToTarget();
                    } else {

                        firstAid = albumid1;
                        firstSid = songid1;

                    }
                    mAlbum.setVisibility(View.VISIBLE);
                    mDuration = mService.duration();
                    mTotalTime.setText(MusicUtils.makeTimeString(this, mDuration / 1000));

                } else {
                    try {
                        LogUtil.d(TAG, "updateHbTrackInfo1.1." );
                        String trackName3 = mService.getTrackName();
                        String artistName3 = mService.getArtistName();
                        if (MediaStore.UNKNOWN_STRING.equals(artistName) || artistName == null) {
                            artistName3 = getString(R.string.unknown_artist_name);
                        }
                        String albumName3 = mService.getAlbumName();
                        if (albumName3 == null) {
                            albumName3 = getString(R.string.unknown_album_name);
                        }
                        StringBuffer tBuffer = new StringBuffer();

                        tBuffer.append(artistName3);
                        mAlbumName.setText(tBuffer.toString());
                        mArtistName.setText(artistName3);

                    } catch (Exception e) {
                        LogUtil.d(TAG, "updateHbTrackInfo Exception:" + e.getMessage());
                    }
                }


            } else {
                LogUtil.d(TAG, "updateHbTrackInfo1.2." );
                String path = mService.getPath();
                if (path == null && mService.getFilePath() == null) {
                    mBtnDown = true;

                    return;
                }

                long songid = mService.getAudioId();

                if (songid < 0 && path != null && path.toLowerCase().startsWith("http://")) {
                } else {
                    String trackName = mService.getTrackName();
                    if (trackName == null) {
                        if (mbUriPath && path != null) {
                            if (!misMms) {
                                int idx_start = path.lastIndexOf('/');
                                int idx_end = path.lastIndexOf('.');
                                mArtistName.setText(path.substring(idx_start + 1, idx_end));
                            } else {
                                mArtistName.setText(path);
                            }
                        } else {
                            mArtistName.setText(getString(R.string.unknown_artist_name));
                        }
                    } else {
                        if (misMms) {
                            mArtistName.setText(path);
                        } else {
                            mArtistName.setText(trackName);
                        }
                    }
                    String artistName = mService.getArtistName();
                    if (MediaStore.UNKNOWN_STRING.equals(artistName) || artistName == null) {
                        artistName = getString(R.string.unknown_artist_name);
                    }
                    String albumName = mService.getAlbumName();
                    long albumid = mService.getAlbumId();
                    if (MediaStore.UNKNOWN_STRING.equals(albumName) || albumName == null) {
                        albumName = getString(R.string.unknown_album_name);
                        albumid = -1;
                    }

                    StringBuffer tBuffer = new StringBuffer();
                    tBuffer.append(artistName);


                    mAlbumName.setText(tBuffer.toString());

                    LogUtil.d(TAG, "updateHbTrackInfo1.3 mbFirstAnim:" + mbFirstAnim);

                    if (!mbFirstAnim) {
                        mAlbumArtHandler.removeMessages(HB_GET_ALBUM_ART);
                        mAlbumArtHandler.obtainMessage(HB_GET_ALBUM_ART, new HbAlbumSongIdWrapper(albumid, songid)).sendToTarget();
                    } else {
                        firstAid = albumid;
                        firstSid = songid;
                    }

                    LogUtil.d(TAG, "Albumid:" + albumid + " Songid:" + songid);

                    mAlbum.setVisibility(View.VISIBLE);

                    mDuration = mService.duration();

                    mTotalTime.setText(MusicUtils.makeTimeString(this, mDuration / 1000));

                }

                mBtnDown = true;
            }


        } catch (Exception e) {
            LogUtil.e(TAG, "updateHbTrackInfo error", e);
            showToast(R.string.player_failed);
        }
        return;
    }


    private ServiceConnection HbOsc = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            mService = IMediaPlaybackService.Stub.asInterface(obj);
            startPlayback();
            try {
                // Assume something is playing when the service says it is,
                // but also if the audio ID is valid but the service is paused.
                if (mService.getAudioId() >= 0 || mService.isPlaying() || mService.getPath() != null) {
                    if (!mService.isPlaying()) {
						/*
						 * int bfirst = MusicUtils.getIntPref(HBPlayerActivity.this, MusicUtils.HB_FITST_ENTNER, 0); if (bfirst == 0) {
						 * mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE); mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
						 * doStartPlay(); MusicUtils.setIntPref(HBPlayerActivity.this, MusicUtils.HB_FITST_ENTNER, 1); } else
						 */if (mFromMain) {
                            setPlayShuffleMode();
                            doStartPlay();
                        }
                        mFromMain = false;
                    } else {
                        setPauseButtonImage();
                    }
                    return;
                }
            } catch (RemoteException ex) {
                LogUtil.d(TAG, "ServiceConnection error:" + ex.getMessage());
            } finally {
                mFromMain = false;
                onPaused = false;
                updateNotValidImageButton();
            }
            return;
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };

    private void onSeekBarAnimation() {
        LogUtil.d(TAG, "onSeekBarAnimation()");
        mAnimationModel1.reset();
        mAnimationModel2.reset();
        ani1 = false;
        ani2 = false;
        distance1 = mDistance;
        degree1 = 0;
        hdistance1 = 0;
        distance2 = mDistance;
        degree2 = 0;
        hdistance2 = 0;
        mNote1.startAnimation(mAnimationModel1);
        return;
    }

    private void onStopPlayAnimation(boolean stop) {

        boolean bplaying = false;
        try {
            if (mService != null) {
                bplaying = mService.isPlaying();
            }
        } catch (Exception e) {
        }
        if (!bplaying) {
            return;
        }
        if (mAnimationStop == stop) {
            return;
        }
        mAnimationStop = stop;
        if (stop) {
            mHandler.removeMessages(HB_ANIMATION_STASRT);
            mNote1.setVisibility(View.GONE);
            mNote2.setVisibility(View.GONE);
            mNote1.clearAnimation();
            mNote2.clearAnimation();
        } else {
            mHandler.removeMessages(HB_ANIMATION_STASRT);
            mHandler.sendEmptyMessageDelayed(HB_ANIMATION_STASRT, 500);
        }
        return;
    }

    private SeekBar.OnSeekBarChangeListener mSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            mLastSeekEventTime = 0;
            mFromTouch = true;
            onStopPlayAnimation(true);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser || (mService == null))
                return;
            mHandler.removeMessages(HB_SEEK);
            Message message = mHandler.obtainMessage(HB_SEEK, progress);
            mHandler.sendMessageDelayed(message, 250);
        }

        public void onStopTrackingTouch(SeekBar bar) {
            mHandler.removeMessages(HB_STOPTRACKINGTOUCH);
            mHandler.sendEmptyMessageDelayed(HB_STOPTRACKINGTOUCH, 250);
        }
    };

    private SeekBar.OnSeekBarChangeListener mVoiceControlSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser){

                //设置系统音量
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                seekBar.setProgress(currentVolume);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private void seek(int progress) {
        mPosOverride = mDuration * progress / 1000;
        LogUtil.d(TAG, "mPosOverride:" + mPosOverride + " mDuration:" + mDuration + " progress:" + progress);
        try {
            mService.seek(mPosOverride);
        } catch (RemoteException ex) {
        }
        // trackball event, allow progress updates
        if (!mFromTouch) {
            refreshHbNow();
            mPosOverride = -1;
            onStopPlayAnimation(false);
        }
    }

    private void prevClicked() {
        if (mService == null)
            return;
        try {
            mFirstAnim = true;
            mPlayPause = false;
            mAnimationStop = true;
            mService.prev();

        } catch (RemoteException ex) {
        }
        return;
    }

    private void nextClicked() {
        if (mService == null)
            return;
        try {
            mFirstAnim = true;
            mPlayPause = false;
            mAnimationStop = true;
            mService.next();
            long curId = mService.getAudioId();
            updateLoveButton(curId);

        } catch (RemoteException ex) {
            LogUtil.e(TAG, "nextClicked error ", ex);
        }
        return;
    }

    private View.OnClickListener mHbNextListener = new View.OnClickListener() {
        public void onClick(View v) {
            nextClicked();
        }
    };



    private RepeatingImageButton.RepeatListener mHbFfwdListener = new RepeatingImageButton.RepeatListener() {
        public void onRepeat(View v, long howlong, int repcnt) {
        }
    };

    private View.OnClickListener mLoveListener = new View.OnClickListener() {
        public void onClick(View v) {
            // showToast(R.string.add_to_playlist);
            if (mService != null) {
                try {
                    // mService.toggleMyFavorite();
                    long audioId = mService.getAudioId();
                    if (audioId == -1) {
                        showToast(R.string.player_failed);
                        return;
                    }
                    if (!mService.isFavorite(audioId)) {
                        mService.addToFavorite(audioId);
                        mLoveButton.setImageResource(R.drawable.hb_play_love_select);
                        onLoveBtnScaleAnimation(mLoveButton);
                    } else {
                        mService.removeFromFavorite(audioId);
                        onLoveBtnHideAnimation(mLoveButton);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };


    private View.OnClickListener mPlayListListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mService != null) {
                try {

                    if (mHBPlayListPopuWindow.isShowing()) {
                        mHBPlayListPopuWindow.thisDismiss(AnimStyle.RIGHTANIM);
                    }
                    mHBPlayListPopuWindow.show(AnimStyle.RIGHTANIM);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private View.OnClickListener mBackListener = new View.OnClickListener() {
        public void onClick(View v) {
            finish();

        }
    };
    private View.OnClickListener mShareListener = new View.OnClickListener() {
        public void onClick(View v) {
            // createShareIntent();
            if (mItemInfo == null) {
                return;
            }
            int isnet = mItemInfo.getIsDownLoadType();
            if (isnet == 0) {
                createShareIntent();
                return;
            }
            showSendConfirmDialog(R.string.hb_playerdialog_share);
        }
    };
    private View.OnClickListener mShuffleListener = new View.OnClickListener() {
        public void onClick(View v) {
            //hbToggleShuffle();
            startAlaphAnimation();
        }
    };

    private void hbToggleShuffle() {
        LogUtil.d(TAG, "hbToggleShuffle");
        if (mService == null) {
            return;
        }
        try {
            int mode = mService.getRepeatMode();
            if (mode == MediaPlaybackService.REPEAT_NONE) {
                mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
                mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
                showToast(R.string.repeat_all_notif);
            } else if (mode == MediaPlaybackService.REPEAT_ALL) {
                int shuffle = mService.getShuffleMode();
                if (shuffle == MediaPlaybackService.SHUFFLE_NORMAL || shuffle == MediaPlaybackService.SHUFFLE_AUTO) {// shuffle

                    mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
                    mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
                    showToast(R.string.repeat_all_notif);
                } else {
                    mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
                    mService.setRepeatMode(MediaPlaybackService.REPEAT_CURRENT);
                    showToast(R.string.repeat_current_notif);
                }
            } else if (mode == MediaPlaybackService.REPEAT_CURRENT) {
                mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NORMAL);
                mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
                showToast(R.string.shuffle_on_notif);
            }
            setHbShuffleButtonImage();
        } catch (RemoteException ex) {
        }
    }

    private void setHbShuffleButtonImage() {
        if (mService == null)
            return;
        try {
            LogUtil.d(TAG, " 1 setHbShuffleButtonImage getRepeatMode:" + mService.getRepeatMode() + " getShuffleMode:" + mService.getShuffleMode());
            switch (mService.getRepeatMode()) {
                case MediaPlaybackService.REPEAT_ALL:
                    if (mService.getShuffleMode() == MediaPlaybackService.SHUFFLE_NORMAL) {
                        mShuffleButton.setImageResource(R.drawable.hb_shuffle);

                    } else {
                        mShuffleButton.setImageResource(R.drawable.hb_repeat_all);
                    }
                    break;
                case MediaPlaybackService.REPEAT_CURRENT:
                    mShuffleButton.setImageResource(R.drawable.hb_repeat_one);
                    break;
                default:
                    mShuffleButton.setImageResource(R.drawable.hb_repeat_all);
                    break;
            }
        } catch (RemoteException ex) {
        }
    }

    private void showToast(int resid) {
        if (mToast == null) {
            mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        }
        mToast.setText(resid);
        mToast.show();
    }

    private void startPlayback() {
        LogUtil.d(TAG, "startPlayback");

        if (mService == null) {
            return;
        }

        try {
            mPlayerListData =(ArrayList<HBListItem>) mService.getListInfo();
        }catch (Exception e){
            LogUtil.i(TAG, e.getMessage());
        }

        if (mService != null){
            mHBPlayListPopuWindow.reFreshListData( mService );
        }

        Intent intent = getIntent();
        String filename = "";
        Uri uri = intent.getData();

        if (uri != null && uri.toString().length() > 0) {
            mPlayUri = uri;

            String scheme = uri.getScheme();
            boolean flag = false;
            if ("file".equals(scheme)) {
                filename = uri.getPath();
                flag = true;
            } else {
                misMms = true;
                filename = uri.toString();
            }
            try {
                mService.stop();
                mbSmallSize = false;
                if (flag) {
                    File f = new File(filename);
                    if (!f.exists()) {
                        mbNotValid = true;

                        return;
                    } else {
                        if (f.length() < 500 * 1024) {
                            mbSmallSize = true;
                        }
                    }
                }
                mPlayPause = false;
                mService.openFile(filename);
                mService.play();
                setIntent(new Intent());
                mbUriPath = true;
            } catch (Exception ex) {
                mbUriPath = false;
                Log.d(TAG, "couldn't start playback: " + ex);
            }
        } else {
            if (mPlayUri == null) {
                mbUriPath = false;
                misMms = false;
                mbSmallSize = false;

            } else {
            }
        }
        updateHbTrackInfo();
        long next = refreshHbNow();
        queueHbNextRefresh(next);
    }

    private void setPlayShuffleMode() {
        try {
            int mode = mService.getShuffleMode();
            if (mode == MediaPlaybackService.SHUFFLE_NORMAL) {
                mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
            } else if (mode == MediaPlaybackService.SHUFFLE_AUTO) {
                mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NORMAL);
                mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
            } else if (mode == MediaPlaybackService.SHUFFLE_NONE) {
                int mode2 = mService.getRepeatMode();
                if (mode2 == MediaPlaybackService.REPEAT_CURRENT) {
                    mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
                    mService.setRepeatMode(MediaPlaybackService.REPEAT_CURRENT);
                } else {
                    mService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
                    mService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
                }
            }
            setHbShuffleButtonImage();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    private void doStartPlay() {
        LogUtil.d(TAG, "doStartPlay ");
        try {
            if (mService != null) {
                mPlayPause = false;
                if (mService.isPlaying()) {
                } else {
                    mService.play();
                }
                refreshHbNow();
            }
        } catch (RemoteException ex) {
            LogUtil.e(TAG, "doStartPlay error", ex);
        }
    }

    private void doPauseResume() {
        try {
            if (mService != null) {
                if (mService.isPlaying()) {
                    mService.pause();
                    LogUtil.d(TAG, "doPauseResume pause");
                } else {
                    mService.play();
                    LogUtil.d(TAG, "doPauseResume play");
                }
                refreshHbNow();
                mPlayPause = true;
            } else {
                mPauseButton.setBackgroundResource(R.drawable.hb_pause);
            }
        } catch (RemoteException ex) {
            LogUtil.e(TAG, "doPauseResume Error ", ex);
            mPauseButton.setBackgroundResource(R.drawable.hb_pause);
        }
    }

    private boolean showOnlineLyric(String filename) {
        LogUtil.d(TAG, "showOnlineLyric :" + filename);
        if (filename == null) {
            return false;
        }
        try {
            File file = new File(filename);
            if (file.exists()) {
                executor.execute(new LyricThread(filename));
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 取歌词
     * @return
     */
    private boolean showAndReadLyc() {
        LogUtil.d(TAG, "showAndReadLyc");
        synchronized (this) {
            try {

                String lrcpath = mService.getLrcUri();

                if (!TextUtils.isEmpty(lrcpath)) {
                    executor.submit(new LyricThread(lrcpath));
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                LogUtil.e(TAG, "showAndReadLyc error", e);
            }
            return false;
        }
    }

    private class LyricThread extends Thread {
        private String filename = null;

        public LyricThread(String filename) {
            this.filename = filename;
        }

        @Override
        public void run() {
            if (mLyricView != null) {
                mHandler.removeMessages(HB_REFRESH_LYRIC);
                mLyricView.read(filename);
                mHandler.obtainMessage(HB_REFRESH_LYRIC).sendToTarget();
            }
        }
    }

    //播放页歌词显示
    private boolean showAndReadSingleLyc() {
        LogUtil.d(TAG, "showAndReadSingleLyc");
        synchronized (this) {
            try {
                String lrcpath = mService.getLrcUri();
                LogUtil.d(TAG, "lrcpath:" + lrcpath);
                if (!TextUtils.isEmpty(lrcpath)) {
                    executor.submit(new LyricSingleThread(lrcpath));
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                LogUtil.e(TAG, "showAndReadSingleLyc error!", e);
            }
            return false;
        }
    }

    private class LyricSingleThread extends Thread {
        private String filename = null;

        public LyricSingleThread(String filename) {
            LogUtil.d(TAG, "3send HB_REFRESH_SINGLE_LYRIC. ");
            this.filename = filename;
        }

        @Override
        public void run() {
            if (mLyricSingleView != null) {
                //LogUtil.d(TAG, "1send HB_REFRESH_SINGLE_LYRIC. ");
                mHandler.removeMessages(HB_REFRESH_SINGLE_LYRIC);
                mLyricSingleView.read(filename);
                //LogUtil.d(TAG, "2send HB_REFRESH_SINGLE_LYRIC. ");
                mHandler.obtainMessage(HB_REFRESH_SINGLE_LYRIC).sendToTarget();
            }
        }
    }

    private ThreadPoolExecutor executor = ThreadPoolExecutorUtils.getThreadPoolExecutor().getExecutor();

    private void upDateLayoutBg(final Bitmap bitmap) {
        LogUtil.d(TAG, "upDateLayoutBg");
        if (mBackGroundView == null) {
            return;
        }

        executor.submit(new Runnable() {
            @Override
            public void run() {
                boolean flag = ((bitmap != null) ? true : false);
                Bitmap newImg = Blur.getBgBlurView(HBPlayerActivity.this, bitmap);
                mHandler.removeMessages(HB_ALBUMBG_DECODED);
                mHandler.obtainMessage(HB_ALBUMBG_DECODED, newImg).sendToTarget();
                if (flag && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        });
        return;
    }


    private void onImgBackGroundUpdate(Bitmap tb, int my_args) {
        LogUtil.d(TAG, "onImgBackGroundUpdate");
        if (tb == null) {
            if (mAnimationBitmap == null) {
                mAnimationBitmap = mDefautBitmap;
            }
            if (mbFirstAnim) {
                if (mAnimView != null) {
                    mAnimView.setVisibility(View.GONE);
                }
                setAlphImageDrawable(mDefautBitmap);
            } else {
                setBgAlphDrawable(mDefautBitmap);
            }
            mAlbum.startAnimDefaultBitmap(true);
        } else {
            if (my_args == 1) {
                mAnimationBitmap = mDefautBitmap;
            }
            if (mAnimationBitmap == null) {
                mAnimationBitmap = tb;
            }
            if (mbFirstAnim) {
                if (mAnimView != null) {
                    mAnimView.setVisibility(View.GONE);
                }
                setAlphImageDrawable(tb);
            } else {
                if (my_args == 1) {
                    setBgAlphDrawable(mDefautBitmap);
                } else {
                    setBgAlphDrawable(tb);
                }
            }
        }
        mbFirstAnim = false;
        return;
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HB_META_CHANGED:
                    updateHbTrackInfo();
                    onStopPlayAnimation(false);
                    queueHbNextRefresh(1);
                    updateNotValidImageButton();
                    break;
                case HB_GET_ALBUM_ERROR:
                    if (mLyricView != null) {
                        mLyricView.setHasLyric(false);
                        mLyricView.clearLyricMap();
                    }

                    if (mLyricSingleView != null) {
                        mLyricSingleView.setHasLyric(false);
                    }
                    break;
                case HB_REFRESH_LYRIC:
                    if (mLyricView != null) {
                        mLyricView.setHasLyric(true);
                        mLyricView.setTextsEx(mLycScrollView);
                        refreshHbLrc(true);
                    }
                    break;
                case HB_REFRESH_SINGLE_LYRIC:
                    if (mLyricSingleView != null) {
                        mLyricSingleView.setHasLyric(true);
                        mLyricSingleView.setTextsEx();
                        refreshHbSingleLrc(true);
                    }
                    break;
                case HB_ALBUMBG_DECODED:
                    if (mBackGroundView != null) {
                        Bitmap bitmap = (Bitmap) msg.obj;
                        if (bitmap == null) {
                            mBackGroundView.setScaleType(ImageView.ScaleType.FIT_XY);
                            setLayoutBgAlph(mDefaultBg);
                        } else {
                            mBackGroundView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            setLayoutBgAlph(bitmap);
                        }
                    }
                    break;
                case HB_ANIMATION_STASRT:
                    onSeekBarAnimation();
                    break;
                case HB_ALBUM_ART_DECODED:
                    Bitmap tb = (Bitmap) msg.obj;
                    int my_args = msg.arg1;
                    if (mAnimView != null && mAnimView.getVisibility() != View.GONE) {
                        mAnimView.setVisibility(View.GONE);
                    }
                    onImgBackGroundUpdate(tb, my_args);
                    break;
                case HB_REFRESH:
                    long next = 500;
                    try {
                        if (!mFromTouch && mService.isPlaying()) {
                            next = refreshHbNow();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    queueHbNextRefresh(next);
                    break;
                case HB_REFRESH_LRC:
                    long nextLrc = 500;
                    try {
                        if (!mFromTouch && mService.isPlaying()) {
                            nextLrc = refreshHbLrc(false)  ;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    queueHbNextRefreshLrc(nextLrc);
                    break;
                case HB_REFRESH_SINGLE_LRC:
                    long nextSingleLrc = 500;
                    try {
                        if (!mFromTouch && mService.isPlaying()) {
                            nextSingleLrc = refreshHbSingleLrc(false)  ;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    queueHbNextRefreshSingleLrc(nextSingleLrc);
                    break;
                case HB_QUIT:

                    new AlertDialog.Builder(HBPlayerActivity.this).setTitle(R.string.service_start_error_title)
                            .setMessage(R.string.service_start_error_msg)
                            .setPositiveButton(R.string.service_start_error_button, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    finish();
                                }
                            }).setCancelable(false).show();
                    break;
                case HB_SEEK:
                    int p = (Integer) msg.obj;
                    seek(p);
                    break;
                case HB_STOPTRACKINGTOUCH:
                    mPosOverride = -1;
                    mFromTouch = false;
                    onStopPlayAnimation(false);
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 搜索加载歌词
     */
    private void findNetLyric() {
        if (mLoadLrtThread != null) {
            LogUtil.d(TAG, "remove-mLoadLrtThread");
            executor.remove(mLoadLrtThread);
            if (!mLoadLrtThread.isInterrupted() || mLoadLrtThread.isAlive()) {
                mLoadLrtThread.interrupt();
            }
            mLoadLrtThread.canCel();
        }
        mLoadLrtThread = new LoadLrtThread(mItemInfo);
        LogUtil.d(TAG, "execute-mLoadLrtThread");
        executor.submit(mLoadLrtThread);
    }

    private void showSendConfirmDialog(final int messageId) {
        final DialogFragment frag = HBDialogFragment.newInstance(HBPlayerActivity.this, messageId);
        frag.show(getFragmentManager(), "share confirm");
        HBDialogFragment.registerItemClickCallback(mDilogCallBack);
    }

    private final HBDilogCallBack mDilogCallBack = new HBDilogCallBack() {
        @Override
        public void onFinishDialogFragment(int ret) {
            if (!HBMusicUtil.isNetWorkActive(HBPlayerActivity.this)) {
                showToast(R.string.hb_network_error);
                return;
            }
            LogUtil.d(TAG, "ret:" + ret);
            if (ret == 0) {// 微信会话
                shareOnWeiXin(ret);
            } else if (ret == 1) {// 朋友圈
                shareOnWeiXin(ret);
                //ReportUtils.getInstance(getApplicationContext()).reportMessage(ReportUtils.TAG_BTN_SHARE_FR);
            } else if (ret == 2) {// 微薄
                shareOnXLWb();
                // showToast(R.string.hb_share_failed);
                //ReportUtils.getInstance(getApplicationContext()).reportMessage(ReportUtils.TAG_BTN_SHARE_TW);
            } else {// 其他
                createShareIntent();
            }
        }
    };

    private void shareOnXLWb() {
        HBShareXLWb.getInstance(this).startWeiBoEx(HBPlayerActivity.this, mHBWeiBoCallBack);
        LogUtil.d(TAG, "shareOnXLWb");
        return;
    }

    private final HBWeiBoCallBack mHBWeiBoCallBack = new HBWeiBoCallBack() {
        @Override
        public void onSinaWeiBoCallBack(int ret) {
            LogUtil.d(TAG, " onSinaWeiBoCallBack ret:" + ret);
            if (ret == HBShareXLWb.HB_WEIBO_SUCCESS) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        startWeiBoEx();
                    }
                });
            }
        }
    };

    private void startWeiBoEx() {
        ImageLoader.getInstance().loadImage(ImageUtil.transferImgUrl(mItemInfo.getAlbumImgUri(), 80), mOptions, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                LogUtil.d(TAG, " startWeiBoEx onLoadingComplete");
                startWeiBo(loadedImage);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                LogUtil.d(TAG, " startWeiBoEx onLoadingFailed ");
                startWeiBo(null);
            }
        });
    }

    private void startWeiBo(Bitmap bm) {
        IMediaPlaybackService tmpService = null;
        if (mService == null) {
            LogUtil.d(TAG, "startWeiBo 0 MusicUtils.sService:" + MusicUtils.sService);
            if (MusicUtils.sService == null) {
                showToast(R.string.xlweibosdk_share_failed);
                return;
            } else {
                tmpService = MusicUtils.sService;
            }
        } else {
            tmpService = mService;
        }
        String url = null;
        String title = null;
        String artist = null;
        String ttString = null;
        String albumStr = null;
        String picPath = null;
        String bitrate = null;
        int duration = 0;
        long id = 0;
        try {
            LogUtil.d(TAG, "startWeiBo 0.1");
            id = tmpService.getAudioId();
            title = tmpService.getTrackName();
            artist = tmpService.getArtistName();
            if (TextUtils.isEmpty(artist)) {
                artist = mArtistNameStr;
            }
            albumStr = tmpService.getAlbumName();
            bitrate = tmpService.getMusicbitrate();
            if (TextUtils.isEmpty(bitrate)) {
                bitrate = "128";
            }
            url = "http://www.xiami.com/song/" + id + "?ref=acopy";
            ttString = tmpService.getFilePath();
            HBMusicInfo info = MusicUtils.getDbMusicInfo(HBPlayerActivity.this, id, title, artist, 1);// 1:表示网络
            LogUtil.d(TAG, "startWeiBo 0.2 info:" + info);
            if (info != null) {
                picPath = info.getPicPath();
            } else {
                picPath = null;
            }
            duration = (int) tmpService.duration();
            LogUtil.d(TAG, "startWeiBo 1 id:" + id + ",title:" + title + ",url:" + url + ",picPath:" + picPath);
            LogUtil.d(TAG, "startWeiBo 2 ttString:" + ttString);
        } catch (Exception e) {
            LogUtil.d(TAG, "startWeiBo 3");
            e.printStackTrace();
            showToast(R.string.xlweibosdk_share_failed);
            return;
        }
        HBShareXLWb.getInstance(this).sendMusic2XLWb(url, url, title, artist, bm, duration, HBPlayerActivity.this);
        return;
    }

    private void shareOnWeiXin(final int type) {
        ImageLoader.getInstance().loadImage(ImageUtil.transferImgUrl(mItemInfo.getAlbumImgUri(), 80), mOptions, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                LogUtil.d(TAG, " shareOnWeiXin onLoadingComplete");
                shareOnNet(type, loadedImage);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                LogUtil.d(TAG, " shareOnWeiXin onLoadingFailed ");
                shareOnNet(type, null);
            }
        });
        return;
    }

    private void shareOnNet(int type, Bitmap bm) {
        if (mService == null) {
            showToast(R.string.hb_share_failed);
            return;
        }
        String url = null;
        String title = null;
        String artist = null;
        String ttString = null;
        String albumStr = null;
        String picPath = null;
        String bitrate = null;
        try {
            long id = mService.getAudioId();
            title = mService.getTrackName();
            artist = mService.getArtistName();
            if (artist == null || (artist != null && TextUtils.isEmpty(artist))) {
                artist = mArtistNameStr;
            }
            albumStr = mService.getAlbumName();
            bitrate = mService.getMusicbitrate();
            if (bitrate == null || (bitrate != null && bitrate.isEmpty())) {
                bitrate = "128";
            }
            url = "m.xiami.com/song/" + id;
            ttString = mService.getFilePath();
            if (mItemInfo != null) {
                picPath = mItemInfo.getAlbumImgUri();
            } else {
                picPath = null;
            }
            if (picPath != null) {
                LogUtil.d(TAG, " shareOnNet 1");
            }

        } catch (Exception e) {

            showToast(R.string.hb_share_failed);
            return;
        }
        if (mWxShare == null) {
            return;
        }
        if (type == 0) {// 微信会话
            mWxShare.sendMusic2Wx(ttString, url, false, title, artist, picPath, albumStr, bitrate, bm);
        } else if (type == 1) {// 朋友圈
            mWxShare.sendMusic2Wx(ttString, url, true, title, artist, picPath, albumStr, bitrate, bm);
        }
        return;
    }

    private void createShareIntent() {
        synchronized (this) {
            if (mItemInfo == null || mService == null) {
                Log.i(TAG, " createShareIntent fail  mItemInfo:" + mItemInfo);
                return;
            }
            int isdown = mItemInfo.getIsDownLoadType();
            String tilte = getString(R.string.hb_player_share);
            Uri tUri = null;
            Intent intent = new Intent(Intent.ACTION_SEND);
            if (isdown == 0) {
                tUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf(mItemInfo.getSongId()));
                if (tUri == null) {
                    return;
                }
                intent.setType("audio/*");
                intent.putExtra(Intent.EXTRA_SUBJECT, tilte);
                intent.putExtra(Intent.EXTRA_STREAM, tUri);
            } else if (isdown == 1) {
                String uriStr = getString(R.string.hb_player_shareinfo) + "<" + mItemInfo.getTitle() + ">";
                try {
                    uriStr += mService.getFilePath();
                } catch (Exception e) {
                    uriStr = null;
                }
                if (uriStr == null) {
                    uriStr = mItemInfo.getAlbumName();
                }
                // intent.setType("audio/*");
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, tilte);
                intent.putExtra(Intent.EXTRA_TEXT, uriStr);
            }
            // intent.putExtra(Intent.EXTRA_SUBJECT, title);
            // intent.putExtra(Intent.EXTRA_STREAM, mUri);
            startActivity(Intent.createChooser(intent, tilte));
        }
        return;
    }

    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtil.i(TAG, " action:" + action);

            if (action.equals(MediaPlaybackService.META_CHANGED)) {
                mHandler.removeMessages(HB_META_CHANGED);
                mHandler.sendEmptyMessageDelayed(HB_META_CHANGED, 100);
            } else if (action.equals(MediaPlaybackService.PLAYSTATE_CHANGED)) {
                setPauseButtonImage();
                updateHbTrackInfo();


            } else if (action.equals(MediaPlaybackService.QUEUE_CHANGED)) {
                if (mPlayUri == null) {
                    mHandler.removeMessages(HB_REFRESH_LISTVIEW);
                    mHandler.obtainMessage(HB_REFRESH_LISTVIEW).sendToTarget();
                }
            } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if (!HBMusicUtil.isNetWorkActive(HBPlayerActivity.this)) {
                    showToast(R.string.hb_network_error);
                }
            }
        }
    };

    private final BroadcastReceiver mVolumeListenerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if(intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")){
                    int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    if (mVoicControlSeekBar != null)

                        mVoicControlSeekBar.setProgress(currentVolume);
                }
            }

        }
    };


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if ( mPlayerLrcLayout.getVisibility() == View.VISIBLE){
            if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
            }else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);

            }
            return true;
        }
        return false;

    }

    private static class HbAlbumSongIdWrapper {
        public long albumid;
        public long songid;

        HbAlbumSongIdWrapper(long aid, long sid) {
            albumid = aid;
            songid = sid;
        }
    }

    //
    public class AlbumArtHandler extends Handler {
        private long mSongId = -1;
        private long mSongId2 = -1;

        public AlbumArtHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            long albumid = ((HbAlbumSongIdWrapper) msg.obj).albumid;
            long songid = ((HbAlbumSongIdWrapper) msg.obj).songid;
            LogUtil.i(TAG, " handleMessage msg.what:"+msg.what);

            if (msg.what == HB_GET_ALBUM_ART) {

                if (mSongId != songid || songid < 0) {
                    LogUtil.d(TAG, " handleMessage 3.0: loading local lyric");
                    if (!showAndReadLyc() || !showAndReadSingleLyc()) {

                        // 显示本地歌词
                        LogUtil.d(TAG, "handleMessage 3.1: loading local lyric null");

                        if (HBMusicUtil.isWifiNetActvie(HBPlayerActivity.this) && Globals.SWITCH_FOR_ONLINE_MUSIC) {

                            LogUtil.d(TAG, " handleMessage 3.2: find net lyric");

                            // 网络搜索歌词
                            findNetLyric();

                        } else { 
                            LogUtil.d(TAG, " handleMessage 3.3 no net");

                            mHandler.obtainMessage(HB_GET_ALBUM_ERROR).sendToTarget();
                        }
                    }
                    String imgpathString = null;

                    if (mItemInfo != null) {

                        imgpathString = MusicUtils.getDbImg(HBPlayerActivity.this, songid, mItemInfo.getTitle(), mItemInfo.getArtistName(), 1);
                        LogUtil.d(TAG, " handleMessage 3.4 imgpathString:" + imgpathString);
                    }

                    // 默认图片
                    if (imgpathString != null && imgpathString.equals(HBSearchLyricActivity.DEFUALT_IMG_URL)) {

                        LogUtil.d(TAG, " handleMessage 3.5");
                        mHandler.removeMessages(HB_ALBUMBG_DECODED);
                        mHandler.obtainMessage(HB_ALBUMBG_DECODED, null).sendToTarget();
                        mHandler.removeMessages(HB_ALBUM_ART_DECODED);
                        Message numsg = mHandler.obtainMessage(HB_ALBUM_ART_DECODED, 1, -1, null);
                        mHandler.sendMessage(numsg);

                    } else {
                        String path = null;
                        Bitmap mBitmap = null;

                        if (mItemInfo != null) {
                            path = mItemInfo.getAlbumImgUri();
                            LogUtil.d(TAG, " handleMessage 3.6 imgpathString:" + imgpathString + ",path:" + path + " songid:" + songid
                                    + " albumid:" + albumid);
                            if (imgpathString != null && imgpathString.equals(HBSearchLyricActivity.HB_ORIGINAL_IMG_URL)) {
                                mBitmap = null;
                            } else {
                                LogUtil.d(TAG, " handleMessage 3.7");
                                mBitmap = MusicUtils.getArtwork(HBPlayerActivity.this, songid, albumid, false, path, 0, mWidth, mWidth);
                                LogUtil.d(TAG, "bm:" + mBitmap + " path:" + path);
                            }
                        }

                        if (mBitmap == null) {
                            if (HBMusicUtil.isWifiNetActvie(HBPlayerActivity.this) && mItemInfo != null && Globals.SWITCH_FOR_ONLINE_MUSIC) {
                                LogUtil.d(TAG, " handleMessage 3.8 isnet:" + mItemInfo.getIsDownLoadType());
                                mSearchImgTask = new SearchImgTask(mItemInfo);
                                mSearchImgTask.executeOnExecutor(executor);
                            } else {
                                LogUtil.d(TAG, " handleMessage 3.9");

                                mHandler.removeMessages(HB_ALBUMBG_DECODED);
                                mHandler.obtainMessage(HB_ALBUMBG_DECODED, null).sendToTarget();
                                mHandler.removeMessages(HB_ALBUM_ART_DECODED);
                                Message numsg = mHandler.obtainMessage(HB_ALBUM_ART_DECODED, 1, -1);
                                mHandler.sendMessage(numsg);
                            }
                        } else {
                            LogUtil.d(TAG, " handleMessage 3.10");

                            Bitmap tBitmap = HBMusicUtil.getCircleBitmap(mBitmap);
                            mHandler.removeMessages(HB_ALBUM_ART_DECODED);
                            Message numsg = mHandler.obtainMessage(HB_ALBUM_ART_DECODED, 0, -1, tBitmap);
                            mHandler.sendMessage(numsg);
                            upDateLayoutBg(mBitmap);
                            System.gc();
                        }
                    }

                    mSongId = songid;
                    mSongId2 = -1;

                } else {
                    LogUtil.d(TAG, " handleMessage 3.11");
                    if (!showAndReadLyc()) {
                        LogUtil.d(TAG, " handleMessage 3.12");
                        if (HBMusicUtil.isWifiNetActvie(HBPlayerActivity.this) && Globals.SWITCH_FOR_ONLINE_MUSIC) {
                            findNetLyric();
                        }
                    }
                }
            } else if (msg.what == HB_GET_ALBUM_ARTDEFAULT && (mSongId2 != songid || songid < 0)) {
                LogUtil.d(TAG, "handleMessage.");
                mHandler.removeMessages(HB_ALBUMBG_DECODED);
                mHandler.obtainMessage(HB_ALBUMBG_DECODED, null).sendToTarget();
                mHandler.removeMessages(HB_ALBUM_ART_DECODED);
                Message numsg = mHandler.obtainMessage(HB_ALBUM_ART_DECODED, 1, -1, null);
                mHandler.sendMessage(numsg);
                mSongId2 = songid;
            }
        }
    }

    private static class HbWorker implements Runnable {
        private final Object mLock = new Object();
        private Looper mLooper;

        HbWorker(String name) {
            Thread t = new Thread(null, this, name);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
            synchronized (mLock) {
                while (mLooper == null) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }

        public Looper getLooper() {
            return mLooper;
        }

        public void run() {
            synchronized (mLock) {
                Looper.prepare();
                mLooper = Looper.myLooper();
                mLock.notifyAll();
            }
            Looper.loop();
        }

        public void quit() {
            mLooper.quit();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!mFromNotification) {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                finish();
                // overridePendingTransition(R.anim.slide_left_in, R.anim.slide_right_out);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /*-animation start*/
    private float mDistance = 0f;
    private float distance1 = 0f;
    // private float defautdistance1 = 0f;
    private int mCenterWidth1, mCenterHeight1;
    private float hdistance1 = 0f;
    private float degree1 = 0f;
    // private float scale1 = 0.3f;
    private float alpha1 = 0f;
    private int mSeekBarWidth = 0;
    private int mSeekBarstart = 0;
    private float mStarth = 0f;
    private boolean ani1 = false;
    // private float defautdistance2 = 0f;
    private float distance2 = 0f;
    private int mCenterWidth2, mCenterHeight2;
    private float hdistance2 = 0f;
    private float degree2 = 0f;
    // private float scale2 = 0.3f;
    private float alpha2 = 0f;
    private boolean ani2 = false;

    @Override
    public void onAnimationCallBack(View view, float interpolatedTime, Transformation t) {
        if (mAnimationStop) {
            t.setAlpha(0);
            return;
        }
        Matrix matrix = t.getMatrix();
        if (view == mNote1) {
            if (interpolatedTime <= 0f) {
                distance1 = mDistance;
                // defautdistance1 = mDistance;
                degree1 = 0;
                hdistance1 = 0;
            }
            ani1 = true;
            boolean overHalf = (interpolatedTime > 0.5f);
            if (overHalf) {
                alpha1 = 1f - alpha1;
            } else {
                // alpha1 = 0.3f + (1.0f - 0f) * interpolatedTime;
                alpha1 = (1.0f - 0f) * interpolatedTime;
            }
            float tscale = 0.5f + 1.0f * interpolatedTime;
            if (overHalf) {
                tscale = 0.5f + 1.0f * (1.0f - interpolatedTime);
            }
            matrix.preTranslate(-mCenterWidth1, -mCenterHeight1);
            matrix.setScale(tscale, tscale);
            matrix.postTranslate(mCenterWidth1, mCenterHeight1);
            if (interpolatedTime > 0.45f && !ani2) {
                mNote2.startAnimation(mAnimationModel2);
            }
            if (interpolatedTime >= 1.0f) {
                ani1 = false;
                alpha1 = 0;
            }
            if (!overHalf && alpha1 < 0.3f) {
                distance1 += 0.1f;
            } else {
                distance1 += 0.4f;
            }
            hdistance1 += 0.8f;
            degree1 += 0.3f;
            if (degree1 > 35) {
                degree1 = 35;
            }
            matrix.postRotate(-degree1 * interpolatedTime);
            matrix.postTranslate(distance1, -hdistance1 / 4);
            t.setAlpha(alpha1);
            mNote1.setVisibility(View.VISIBLE);
        } else if (view == mNote2) {
            if (interpolatedTime <= 0f) {
                distance2 = mDistance - offset;
                // defautdistance2 = mDistance;
                degree2 = 0;
                hdistance2 = 0;
            }
            ani2 = true;
            boolean overHalf = (interpolatedTime > 0.5f);
            if (overHalf) {
                alpha2 = 1f - alpha2;
            } else {
                // alpha2 = 0.3f + (1.0f - 0f) * interpolatedTime;
                alpha2 = (1.0f - 0f) * interpolatedTime;
            }
            float tscale = 0.5f + 1.0f * interpolatedTime;
            if (overHalf) {
                tscale = 0.5f + 1.0f * (1.0f - interpolatedTime);
            }
            matrix.preTranslate(-mCenterWidth2, -mCenterHeight2);
            matrix.setScale(tscale, tscale);
            matrix.postTranslate(mCenterWidth2, mCenterHeight2);
            if (interpolatedTime > 0.45f && !ani1) {
                mNote1.startAnimation(mAnimationModel1);
            }
            if (interpolatedTime >= 1.0f) {
                ani2 = false;
                alpha2 = 0;
            }
            if (!overHalf && alpha2 < 0.3f) {
                distance2 += 0.1f;
            } else {
                distance2 += 0.4f;
            }
            hdistance2 += 0.8f;
            degree2 += 0.5f;
            if (degree2 > 45) {
                degree2 = 45;
            }
            matrix.postRotate(-degree2 * interpolatedTime);
            matrix.postTranslate(distance2, -hdistance2 / 6);
            t.setAlpha(alpha2);
            mNote2.setVisibility(View.VISIBLE);
        }
        return;
    }

    private void onLoveBtnHideAnimation(final ImageButton view) {
        if (view == null) {
            return;
        }
        if (outAlphaAnimation == null) {
            outAlphaAnimation = new AlphaAnimation(1.0f, 0f);
            outAlphaAnimation.setDuration(500);
            outAlphaAnimation.setInterpolator(new DecelerateInterpolator());
            outAlphaAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationRepeat(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (view != null) {
                        view.clearAnimation();
                        view.setImageResource(R.drawable.hb_play_love);
                    }
                }
            });
        }
        outAlphaAnimation.reset();
        view.startAnimation(outAlphaAnimation);
        return;
    }

    private void onLoveBtnScaleAnimation(final ImageButton view) {
        if (view == null) {
            return;
        }
        if (inScaleAnimation == null) {
            inScaleAnimation = new ScaleAnimation(0.3f, 1.0f, 0.3f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        }
        if (inAnimation == null) {
            inAnimation = new AlphaAnimation(0.3f, 1.f);
        }
        if (mAnimationSet == null) {
            mAnimationSet = new AnimationSet(true);
            mAnimationSet.setDuration(500);
            mAnimationSet.addAnimation(inAnimation);
            mAnimationSet.addAnimation(inScaleAnimation);
            mAnimationSet.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation arg0) {}

                @Override
                public void onAnimationRepeat(Animation arg0) {}

                @Override
                public void onAnimationEnd(Animation arg0) {
                    if (view != null) {
                        // view.startAnimation(inScaleAnimation2);
                    }
                }
            });
            mAnimationSet.setInterpolator(new OvershootInterpolator(3.0f));
        }
        mAnimationSet.reset();
        view.startAnimation(mAnimationSet);
        return;
    }

    private void setAlphImageDrawable(Bitmap bitmap) {
        if (mAlbum == null || bitmap == null) {
            return;
        }

        TransitionDrawable trDrawable = new TransitionDrawable(new Drawable[] { getResources().getDrawable(R.drawable.default_album_bg),
                new BitmapDrawable(getResources(), bitmap) });
        if (bitmap != mDefautBitmap) {
            mAlbum.setPadding(mPaddingOffset, mPaddingOffset, mPaddingOffset, mPaddingOffset);
        } else {
            mAlbum.setPadding(0, 0, 0, 0);
        }
        trDrawable.setId(0, 0);
        trDrawable.setId(1, 1);
        mAlbum.setImageDrawable(trDrawable);
        if (bitmap != mDefautBitmap) {
            mAlbum.startAnimDefaultBitmap(false);
            mAlbum.setBackgroundResource(R.drawable.default_album_image);
        } else {
            mAlbum.startAnimDefaultBitmap(true);
            mAlbum.setBackground(null);
        }
        trDrawable.startTransition(400);
        return;
    }

    private void onFirstAnimation(final ImageView view) {
        LogUtil.d(TAG, "onFirstAnimation.");

        if (view == null) {
            return;
        }

        AlphaAnimation startAnimation = new AlphaAnimation(0.5f, 1f);
        RotateAnimation rotateAnimation = new RotateAnimation(90, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        TranslateAnimation tAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1.3f, Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);

        final AnimationSet tSet = new AnimationSet(true);
        tSet.addAnimation(startAnimation);
        tSet.addAnimation(rotateAnimation);
        tSet.addAnimation(tAnimation);
        tSet.setDuration(1000);
        tSet.setInterpolator(new DecelerateInterpolator());
        tSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation arg0) {}

            @Override
            public void onAnimationRepeat(Animation arg0) {}

            @Override
            public void onAnimationEnd(Animation arg0) {
                onShowFirstPic();
                mFirstAnim = true;
                return;
            }
        });
        view.startAnimation(tSet);
        return;
    }

    private void onShowFirstPic() {

        LogUtil.d(TAG, "firstAid:" + firstAid + " firstSid:" + firstSid );

        HbAlbumSongIdWrapper wrapper = new HbAlbumSongIdWrapper(firstAid, firstSid);

        mAlbumArtHandler.removeMessages(HB_GET_ALBUM_ART);

        mAlbumArtHandler.obtainMessage(HB_GET_ALBUM_ART, wrapper).sendToTarget();
    }

    private void onRotationAnimation(final OTAMainPageFrameLayout view, final boolean flag) {

        if (view == null || !mPlayPause) {
            return;
        }

        mPlayPause = false;
        view.stopAnim();
        view.setAnimationListener(new OTAFrameAnimation.AnimationImageListener() {
            @Override
            public void onRepeat(int repeatIndex) {}

            @Override
            public void onFrameChange(int repeatIndex, int frameIndex, int currentTime) {}

            @Override
            public void onAnimationStart() {}

            @Override
            public void onAnimationEnd() {
                if (view != null) {
                    boolean tflag = flag;
                    if (tflag) {
                        view.setBackgroundResource(R.drawable.hb_play);
                    } else {
                        view.setBackgroundResource(R.drawable.hb_pause);
                    }
                }
            }
        });
        if (flag) {
            // 暂停
            view.setFrameAnimationList(R.drawable.pause_play_anima);
            view.startAnim();
        } else {
            // 播放
            view.setFrameAnimationList(R.drawable.play_pause_anima);
            view.startAnim();
        }
        return;
    }

    /**
     * 渐显动画  add 2014.5.27
     */
    private void AlphaAnimationIn(final View v, final long durationMillis) {
        if (v == null) {
            return;
        }
        v.clearAnimation();
        Animation mInAlphaAnimation = new AlphaAnimation(1.0f, 0f);
        mInAlphaAnimation.setDuration(durationMillis);
        mInAlphaAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                if (v != null) {
                    v.clearAnimation();
                }

            }
        });
        v.startAnimation(mInAlphaAnimation);
        v.setVisibility(View.GONE);
        return;
    }

    /**
     * 渐隐动画  add 2014.5.27
     */
    private void AlphaAnimationOut(final View v, final long durationMillis) {
        if (v == null) {
            return;
        }
        v.clearAnimation();
        Animation mOutAlphaAnimation = new AlphaAnimation(1.0f, 0.0f);
        mOutAlphaAnimation.setDuration(durationMillis);
        mOutAlphaAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                if (v != null) {
                    v.clearAnimation();
                }
            }
        });
        v.startAnimation(mOutAlphaAnimation);
        v.setVisibility(View.GONE);
        return;
    }



    private View.OnClickListener mSearchLyricListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showSearchLyricDialog();
        }
    };

    private AlertDialog mSearchLyricDialog;
    private EditText songName, artistName;
    private CheckBox modifyCheckBox;
    private int REQUESTCODE = 1000;

    private void showSearchLyricDialog() {
        try {
            if (mService == null || (mService != null && mService.getAudioId() < 0) || mItemInfo == null) {
                showToast(R.string.search_failed);
                return;
            }
        } catch (Exception e) {
            return;
        }
        if (mSearchLyricDialog == null) {
            View viewLayout = LayoutInflater.from(this).inflate(R.layout.hb_search_lyric_dialog, null);
            songName = (EditText) viewLayout.findViewById(R.id.hb_search_songs);
            artistName = (EditText) viewLayout.findViewById(R.id.hb_search_artist);
            modifyCheckBox = (CheckBox) viewLayout.findViewById(R.id.hb_check);
            songName.addTextChangedListener(mTextWatcher);
            artistName.addTextChangedListener(mTextWatcher);
            mSearchLyricDialog = new AlertDialog.Builder(this).setTitle(R.string.hb_search_lyric).setView(viewLayout)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            HBMusicUtil.hideInputMethod(HBPlayerActivity.this, mSearchLyricDialog.getCurrentFocus());
                            if (!HBMusicUtil.isNetWorkActive(HBPlayerActivity.this)) {
                                Toast.makeText(HBPlayerActivity.this, R.string.hb_network_error, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            long id = 0;
                            try {
                                if (mService != null) {
                                    id = mService.getAudioId();
                                }
                            } catch (Exception e) {
                            }
                            String path = null;
                            if (mItemInfo != null) {
                                path = mItemInfo.getFilePath();
                            }
                            Intent intent = new Intent(HBPlayerActivity.this, HBSearchLyricActivity.class);
                            if (path != null && !TextUtils.isEmpty(path) && mItemInfo.getIsDownLoadType() == 0) {
                                path = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf("."));
                                String strpath = HBMusicUtil.getImgPath(getApplication(), HBMusicUtil.MD5(path));
                                intent.putExtra(HBSearchLyricActivity.EXTR_IMG_PATH, strpath);
                            }
                            intent.putExtra(HBSearchLyricActivity.EXTR_ID, String.valueOf(id));
                            intent.putExtra(HBSearchLyricActivity.EXTR_NAME, songName.getText().toString());
                            intent.putExtra(HBSearchLyricActivity.EXTR_ARTIST, artistName.getText().toString());
                            intent.putExtra(HBSearchLyricActivity.EXTR_ALBUM, mItemInfo.getAlbumName());
                            LogUtil.d(TAG, "--mItemInfo.getFilePath():" + mItemInfo.getFilePath());
                            intent.putExtra(HBSearchLyricActivity.EXTR_SONGPATH, mItemInfo.getFilePath());
                            startActivityForResult(intent, REQUESTCODE);
                            // 修改id3信息
                            if (modifyCheckBox.isChecked() && (mItemInfo != null && mItemInfo.getIsDownLoadType() == 0)) {
                                ContentValues values = new ContentValues();
                                values.put(MediaStore.Audio.Media.TITLE, songName.getText().toString());
                                values.put(MediaStore.Audio.Media.ARTIST, artistName.getText().toString());
                                getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values,
                                        MediaStore.Audio.Media._ID + "=" + mItemInfo.getSongId(), null);
                                mItemInfo.setTitle(songName.getText().toString());
                                mItemInfo.setArtistName(artistName.getText().toString());
                                if (MusicUtils.sService != null) {
                                    try {
                                        MusicUtils.sService.updateCursor();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            HBMusicUtil.hideInputMethod(HBPlayerActivity.this, mSearchLyricDialog.getCurrentFocus());
                        }
                    }).create();
        }
        String path = mItemInfo.getFilePath();
        Boolean isHasId3 = false;
        if (path != null && !TextUtils.isEmpty(path) && mItemInfo.getIsDownLoadType() == 0) {
            path = path.substring(path.lastIndexOf("."));
            if (path.equals(".mp3")) {
                isHasId3 = true;
            }
        }
        modifyCheckBox.setChecked(true);
        modifyCheckBox.setEnabled(isHasId3);
        songName.requestFocus();
        songName.setText(mArtistName.getText());
        String tmpArtist = null;
        try {
            if (mService != null) {
                tmpArtist = mService.getArtistName();
            }
        } catch (Exception e) {
            tmpArtist = null;
        }
        if (tmpArtist == null) {
            tmpArtist = mItemInfo.getArtistName();
        }
        artistName.setText(tmpArtist);
        // 设置光标位置
        CharSequence text = songName.getText();
        if (text instanceof Spannable) {
            Spannable spanText = (Spannable) text;
            Selection.setSelection(spanText, text.length());
        }
        if (!mSearchLyricDialog.isShowing()) {
            mSearchLyricDialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtil.d(TAG, "requestCode:" + requestCode + " resultCode:" + resultCode);
        if (requestCode == REQUESTCODE && resultCode == RESULT_OK && data != null) {
            String name = data.getStringExtra(HBSearchLyricActivity.EXTR_NAME);
            String path = data.getStringExtra(HBSearchLyricActivity.EXTR_PATH);
            String imgpath = data.getStringExtra(HBSearchLyricActivity.EXTR_IMG_PATH);
            String songid = data.getStringExtra(HBSearchLyricActivity.EXTR_ID);
            long id = -1;
            try {
                id = Long.valueOf(songid);
            } catch (Exception e) {
                id = -1;
            }
            if (name.equals(songName.getText().toString())) {
                showOnlineLyric(path);
                if (MusicUtils.sService != null) {
                    try {
                        MusicUtils.sService.notifyLrcPath(path);// 更新琐屏
                        MusicUtils.sService.updateNotification();// 更新通知栏
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                // 更新图片
                if (imgpath == null) {
                    return;
                }
                Bitmap bitmap = null;
                if (imgpath.equals(HBSearchLyricActivity.DEFUALT_IMG_URL)) {
                    mHandler.removeMessages(HB_ALBUMBG_DECODED);
                    mHandler.obtainMessage(HB_ALBUMBG_DECODED, null).sendToTarget();
                    mHandler.removeMessages(HB_ALBUM_ART_DECODED);
                    Message numsg = mHandler.obtainMessage(HB_ALBUM_ART_DECODED, 0, -1, null);
                    mHandler.sendMessage(numsg);
                    return;
                } else if (id > 0 && imgpath.equals(HBSearchLyricActivity.HB_ORIGINAL_IMG_URL)) {
                    bitmap = null;
                } else {
                    bitmap = BitmapUtil.decodeSampledBitmapFromFile(imgpath, mWidth, mWidth);
                }
                // Bitmap bitmap = BitmapFactory.decodeFile(imgpath);
                if (bitmap == null) {
                    return;
                }
                Bitmap tBitmap = HBMusicUtil.getCircleBitmap(bitmap);
                mHandler.removeMessages(HB_ALBUM_ART_DECODED);
                Message numsg = mHandler.obtainMessage(HB_ALBUM_ART_DECODED, 0, -1, tBitmap);
                mHandler.sendMessage(numsg);
                upDateLayoutBg(bitmap);
                System.gc();
            }
        } else {
            // 新浪微薄分享
            HBShareXLWb.getInstance(this).authorizeCallBack(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence charsequence, int i, int j, int k) {
            // LogUtil.d(TAG,
            // "songName:"+songName.getText().length()+" artistName:"+artistName.getText().length());
            Button okButton = mSearchLyricDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (okButton == null) {
                return;
            }
            if (songName.getText().length() == 0 || artistName.getText().length() == 0) {
                okButton.setEnabled(false);
            } else {
                okButton.setEnabled(true);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence charsequence, int i, int j, int k) {}

        @Override
        public void afterTextChanged(Editable editable) {}
    };

    private void setBgAlphDrawable(Bitmap bitmap) {
        if (mAlbum == null || bitmap == null) {
            return;
        }
        Drawable oldDrawable = mAlbum.getDrawable();
        TransitionDrawable trDrawable = null;
        BitmapDrawable oldBitmapDrawable = null;
        if (oldDrawable instanceof TransitionDrawable) {
            trDrawable = (TransitionDrawable) oldDrawable;
            oldBitmapDrawable = (BitmapDrawable) trDrawable.findDrawableByLayerId(trDrawable.getId(1));
        } else if (oldDrawable instanceof BitmapDrawable) {
            oldBitmapDrawable = (BitmapDrawable) oldDrawable;
        }
        if (trDrawable == null) {
            trDrawable = new TransitionDrawable(new Drawable[] { oldBitmapDrawable, new BitmapDrawable(getResources(), bitmap) });
            trDrawable.setId(0, 0);
            trDrawable.setId(1, 1);
        } else {
            trDrawable.setDrawableByLayerId(trDrawable.getId(0), oldBitmapDrawable);
            trDrawable.setDrawableByLayerId(trDrawable.getId(1), new BitmapDrawable(getResources(), bitmap));
        }
        if (bitmap != mDefautBitmap) {
            mAlbum.setPadding(mPaddingOffset, mPaddingOffset, mPaddingOffset, mPaddingOffset);
        } else {
            mAlbum.setPadding(0, 0, 0, 0);
        }
        mAlbum.setImageDrawable(trDrawable);
        if (bitmap != mDefautBitmap) {
            mAlbum.startAnimDefaultBitmap(false);
            mAlbum.setBackgroundResource(R.drawable.default_album_image);
        } else {
            mAlbum.startAnimDefaultBitmap(true);
            mAlbum.setBackground(null);
        }
        trDrawable.setCrossFadeEnabled(true);
        trDrawable.startTransition(1000);
        System.gc();
        return;
    }

    private void setLayoutBgAlph(Bitmap bitmap) {
        if (mBackGroundView == null || bitmap == null) {
            return;
        }
        Drawable oldDrawable = mBackGroundView.getDrawable();
        TransitionDrawable trDrawable = null;
        BitmapDrawable oldBitmapDrawable = null;
        if (oldDrawable instanceof TransitionDrawable) {
            trDrawable = (TransitionDrawable) oldDrawable;
            oldBitmapDrawable = (BitmapDrawable) trDrawable.findDrawableByLayerId(trDrawable.getId(1));
        } else if (oldDrawable instanceof BitmapDrawable) {
            oldBitmapDrawable = (BitmapDrawable) oldDrawable;
        }
        if (trDrawable == null) {
            trDrawable = new TransitionDrawable(new Drawable[] { oldBitmapDrawable, new BitmapDrawable(getResources(), bitmap) });
            trDrawable.setId(0, 0);
            trDrawable.setId(1, 1);
        } else {
            trDrawable.setDrawableByLayerId(trDrawable.getId(0), oldBitmapDrawable);
            trDrawable.setDrawableByLayerId(trDrawable.getId(1), new BitmapDrawable(getResources(), bitmap));
        }
        mBackGroundView.setImageDrawable(trDrawable);
        trDrawable.setCrossFadeEnabled(true);
        trDrawable.startTransition(1000);
        System.gc();
    }

    /**
     * search LRC
     */
    private class LoadLrtThread extends Thread {
        private boolean isCancel = false;
        private HBListItem mItemInfoTemp;

        public void canCel() {
            isCancel = true;
        }

        public LoadLrtThread(HBListItem mItemInfo) {
            isCancel = false;
            this.mItemInfoTemp = mItemInfo;
            mHandler.obtainMessage(HB_GET_ALBUM_ERROR).sendToTarget();
        }

        @Override
        public void run() {
            if (isCancel || isInterrupted()) {
                return;
            }
            LogUtil.d(TAG, "LoadLrtThread " + mItemInfoTemp.getSongId());
            String lrcUrl = null;
            try {
                if (mService != null) {
                    lrcUrl = mService.getLryFile();
                }
            } catch (Exception e) {
                e.printStackTrace();
                lrcUrl = null;
            }
            LogUtil.d(TAG, " LoadLrtThread lrcUrl1:" + lrcUrl);
            if (mItemInfoTemp == null || TextUtils.isEmpty(mItemInfoTemp.getTitle()) || TextUtils.isEmpty(mItemInfoTemp.getArtistName())) {
                return;
            }
            if (TextUtils.isEmpty(lrcUrl)) {
                lrcUrl = mItemInfoTemp.getLrcUri();
            }
            // 本地音乐只在wifi下搜词
            if (TextUtils.isEmpty(lrcUrl) && !HBMusicUtil.isWifiNetActvie(getApplicationContext())) {
                return;
            }
            if (TextUtils.isEmpty(lrcUrl)) {
                if (null != mService) {
                    try {
                        DownloadInfo downloadInfo = mService.queryDownloadSong(mItemInfoTemp.getTitle(), mItemInfoTemp.getArtistName());
                        if (downloadInfo != null) {

                            if (TextUtils.isEmpty(lrcUrl)) {
                                OnlineSong song = XiaMiSdkUtils.findSongByIdSync(getApplicationContext(), downloadInfo.getId(),
                                        HBMusicUtil.getOnlineSongQuality());
                                if (song != null)
                                    lrcUrl = song.getLyric();
                                LogUtil.d(TAG, "2 lrcUrl:" + lrcUrl);
                            }
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (TextUtils.isEmpty(lrcUrl)) {
                String artistName = mItemInfoTemp.getArtistName();
                String albumName = mItemInfoTemp.getAlbumName();
                String title = mItemInfoTemp.getTitle();
                albumName = HBMusicUtil.doAlbumName(mItemInfoTemp.getFilePath(), albumName);
                List<OnlineSong> list = XiaMiSdkUtils.findSongByNameSync(getApplicationContext(), title, artistName, albumName);
                if (list != null && list.size() > 0) {
                    lrcUrl = list.get(0).getLyric();
                    LogUtil.d(TAG, "3 lrcUrl:" + lrcUrl);
                }
            }
            LogUtil.d(TAG, "lrcUrl2:" + lrcUrl);
            if (TextUtils.isEmpty(lrcUrl) || isCancel) {
                LogUtil.d(TAG, "no lrcUrl");
                return;
            }
            URL url = null;
            HttpURLConnection http = null;
            InputStream inputStream = null;
            File dirFile = new File(Globals.mLycPath);
            if (!dirFile.exists()) {
                dirFile.mkdirs();
            }
            String ext = HBMusicUtil.getExtFromFilename(lrcUrl).toLowerCase();
            if (TextUtils.isEmpty(ext)) {
                ext = "lrc";
            }
            String name = mItemInfoTemp.getTitle() + "_" + mItemInfoTemp.getArtistName() + "." + ext;
            if (!mItemInfoTemp.getArtistName().equalsIgnoreCase(mItemInfoTemp.getSingers()) && !TextUtils.isEmpty(mItemInfoTemp.getSingers())) {
                name = mItemInfoTemp.getTitle() + "_" + mItemInfoTemp.getArtistName() + "_" + mItemInfoTemp.getSingers() + "." + ext;
            }
            final String filepath = Globals.mLycPath + File.separator + name;
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(filepath);
                url = new URL(lrcUrl);
                http = (HttpURLConnection) url.openConnection();
                http.setConnectTimeout(8 * 1000);
                http.connect();
                int code = http.getResponseCode();
                int offset = 0;
                if (code == HttpStatus.SC_OK) {
                    inputStream = http.getInputStream();
                    byte[] buffer = new byte[4096];
                    while ((offset = inputStream.read(buffer)) != -1) {
                        if (isCancel) {
                            break;
                        }
                        outputStream.write(buffer, 0, offset);
                    }
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (isCancel) {
                                return;
                            }
                            if (mItemInfoTemp != null && mItemInfoTemp.equals(mItemInfo)) {
                                showOnlineLyric(filepath);
                            }
                            if (mItemInfoTemp != null) {
                                MusicUtils.addToAudioInfoLrc(HBPlayerActivity.this, mItemInfoTemp.getSongId(), mItemInfoTemp.getTitle(),
                                        mItemInfoTemp.getArtistName(), filepath);
                            }
                            try {
                                if (mService != null && mItemInfoTemp != null && mItemInfoTemp.equals(mItemInfo)) {//
                                    mService.notifyLrcPath(filepath);
                                }
                            } catch (Exception e) {
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (http != null) {
                    http.disconnect();
                }
                try {
                    if (outputStream != null)
                        outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    if (inputStream != null)
                        inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startAlaphAnimation() {
        if (mShuffleButton == null) {
            return;
        }
        mShuffleButton.clearAnimation();
        AnimationSet set = new AnimationSet(true);
        Animation inAlphaAnimation = new AlphaAnimation(1.0f, 0.25f);
        inAlphaAnimation.setDuration(250);
        inAlphaAnimation.setInterpolator(new AccelerateInterpolator());
        inAlphaAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // TODO Auto-generated method stub
                hbToggleShuffle();
                Animation outAlphaAnimation = new AlphaAnimation(0.25f, 1.0f);
                outAlphaAnimation.setInterpolator(new AccelerateInterpolator());
                outAlphaAnimation.setDuration(250);
                mShuffleButton.startAnimation(outAlphaAnimation);
            }
        });
        mShuffleButton.startAnimation(inAlphaAnimation);
    }

    private class SearchImgTask extends AsyncTask<Void, Void, String> {
        private HBListItem mItemInfo;

        public SearchImgTask(HBListItem mItemInfo) {
            super();
            this.mItemInfo = mItemInfo;
        }

        @Override
        protected String doInBackground(Void... params) {
            if (mItemInfo == null) {
                LogUtil.d(TAG, "mItemInfo is null");
                return null;
            }
            String imageUrl = null;
            try {
                if (null != mService && mService.isOnlineSong()) {
                    OnlineSong onlineSong = XiaMiSdkUtils.findSongByIdSync(getApplicationContext(), mItemInfo.getSongId(),
                            HBMusicUtil.getOnlineSongQuality());
                    if (onlineSong != null) {
                        imageUrl = onlineSong.getImageUrl();
                        LogUtil.d(TAG, " 1-imageUrl:" + imageUrl);
                    }
                }
                if (TextUtils.isEmpty(imageUrl)) {
                    String artistName = mItemInfo.getArtistName();
                    String albumName = mItemInfo.getAlbumName();
                    String title = mItemInfo.getTitle();
                    albumName = HBMusicUtil.doAlbumName(mItemInfo.getFilePath(), albumName);
                    List<OnlineSong> list = XiaMiSdkUtils.findSongByNameSync(getApplicationContext(), title, artistName, albumName);
                    if (list != null && list.size() > 0) {
                        imageUrl = list.get(0).getImageUrl();
                        LogUtil.d(TAG, " 1-imageUrl:" + imageUrl);
                    }
                    if (TextUtils.isEmpty(imageUrl) && null != mService) {
                        DownloadInfo downloadInfo = mService.queryDownloadSong(mItemInfo.getTitle(), mItemInfo.getArtistName());
                        if (downloadInfo != null) {
                            imageUrl = downloadInfo.getImgUrl();
                            LogUtil.d(TAG, "2-imageUrl:" + imageUrl);
                            if (TextUtils.isEmpty(imageUrl)) {
                                List<OnlineSong> lists = XiaMiSdkUtils.findSongByNameSync(getApplicationContext(), downloadInfo.getTitle(),
                                        downloadInfo.getArtist(), downloadInfo.getAlbum());
                                if (list != null && list.size() > 0) {
                                    imageUrl = list.get(0).getImageUrl();
                                    LogUtil.d(TAG, "3-imageUrl:" + imageUrl);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            LogUtil.d(TAG, "imageUrl:" + imageUrl);
            return imageUrl;
        }

        @Override
        protected void onPostExecute(String result) {
            if (mItemInfo == null || (mItemInfo != null && TextUtils.isEmpty(result))) {
                mPicurl = null;
                long tmpId1 = -1;
                long tmpId2 = -1;
                LogUtil.d(TAG, " SearchImgTask 1-");
                mAlbumArtHandler.removeMessages(HB_GET_ALBUM_ARTDEFAULT);
                mAlbumArtHandler.obtainMessage(HB_GET_ALBUM_ARTDEFAULT, new HbAlbumSongIdWrapper(tmpId1, tmpId2)).sendToTarget();
            } else {
                ImageLoader.getInstance().loadImage(ImageUtil.transferImgUrl(result, 330), mOptions, new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        LogUtil.d(TAG, "SearchImgTask onLoadingComplete 1 --imageUri:" + imageUri);
                        if (isdestory) {
                            return;
                        }
                        final Bitmap bm = loadedImage;
                        saveBitmap(bm);
                        if (mHandler != null) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (bm != null) {//
                                        Bitmap tBitmap2 = HBMusicUtil.getCircleBitmap(bm);
                                        int arg = tBitmap2 == null ? 1 : 0;
                                        onImgBackGroundUpdate(tBitmap2, arg);
                                        upDateLayoutBg(bm);
                                    }
                                    if (MusicUtils.sService != null) {
                                        try {
                                            MusicUtils.sService.updateNotification();// 更新通知栏
                                        } catch (RemoteException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                        LogUtil.d(TAG,
                                "SearchImgTask onLoadingFailed 1 --imageUri:" + imageUri + ",failReason:" + failReason.getType());
                        mPicurl = null;
                        mAlbumArtHandler.removeMessages(HB_GET_ALBUM_ARTDEFAULT);
                        mAlbumArtHandler.obtainMessage(HB_GET_ALBUM_ARTDEFAULT, new HbAlbumSongIdWrapper(-1, -1)).sendToTarget();
                    }
                });
            }
        }

        @Override
        protected void onPreExecute() {}
    }

    private void saveBitmap(Bitmap bm) {
        LogUtil.d(TAG, "saveBitmap:" + bm);
        if (bm != null) {
            File dir = new File(Globals.mSongImagePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            if (mItemInfo == null) {
                return;
            }
            File file = new File(Globals.mSongImagePath + File.separator
                    + HBMusicUtil.MD5(mItemInfo.getTitle().trim() + mItemInfo.getArtistName().trim() + mItemInfo.getAlbumName().trim()));
            if (file.exists()) {
                LogUtil.d(TAG, "saveBitmap exists rt");
                return;
            }
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(file);
                bm.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (Exception e) {
                LogUtil.d(TAG, "saveBitmap fail");
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.flush();
                    } catch (Exception e2) {
                    }
                    try {
                        out.close();
                    } catch (Exception e2) {
                    }
                    out = null;
                }
            }
        }
        return;
    }

    private void initXiamiRadio() {
        LogUtil.d(TAG, "initXiamiRadio:" + Application.mRadiotype);
        if (Application.mRadiotype >= 0) {
            isRadioType = true;
        } else {
            isRadioType = false;
        }
    }

    private void startPlayRadio(int posion) {
        ((Application) getApplication()).startPlayRadio(posion, mHandler, this);
    }
}
