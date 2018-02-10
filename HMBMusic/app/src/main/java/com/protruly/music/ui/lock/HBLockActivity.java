package com.protruly.music.ui.lock;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextClock;
import android.widget.TextView;
import com.protruly.music.MediaPlaybackService;
import com.protruly.music.MusicUtils;
import com.protruly.music.R;
import com.protruly.music.util.LogUtil;
import com.protruly.music.util.ThreadPoolExecutorUtils;
import com.protruly.music.widget.HBLyricSingleView;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by wxue on 17-9-18.
 */

public class HBLockActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "HBLockActivity";
    private static final String PREF_MUSIC_LOCK_INFO = "music_lock_info";
    private static final String SHOWED_SHAKE = "showed_shake";
    private static final int INIT_SINGLE_LYRIC = 1;
    private static final int REFRESH_SINGLE_LYRIC = 2;
    private ImageView mIvAlbumArt;
    private ClockView mClockView;
    private TextClock mDateView;
    private TextClock mWeekView;
    private TextView mAudioTitle;
    private TextView mArtistName;
    private HBLyricSingleView mLyricSingleView;
    private SeekBar mVolumeSeekbar;
    private ImageButton mShuffleButton;
    private ImageButton mPreButton;
    private ImageButton mPlayButton;
    private ImageButton mNextButton;
    private SlideUnlockLayout mSlideUnlockLayout;
    private View mShakeView;
    private AudioManager mAudioManager;
    private VolumeChangeObserver mVolumeObserver;
    private ThreadPoolExecutor executor = ThreadPoolExecutorUtils.getThreadPoolExecutor().getExecutor();
    private SensorManager mSensorManager;
    private Vibrator mVibrator;
    private SensorEventListener mSensorListener;
    private SharedPreferences mPreference;
    private MusicUtils.ServiceToken mToken;
    private ObjectAnimator mFadeInAnimator;
    private ObjectAnimator mFadeOutAnimator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        int systemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        systemUiVisibility |= flags;
        getWindow().getDecorView().setSystemUiVisibility(systemUiVisibility);
        setContentView(R.layout.hb_lock_activity);

        Intent intent = new Intent();
        intent.setAction(MediaPlaybackService.LOCK_SCREEN_CHANGED);
        intent.putExtra("islock",true);
        sendBroadcast(intent);

        initView();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mVolumeObserver = new VolumeChangeObserver();
        getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, mVolumeObserver);

        IntentFilter filter = new IntentFilter();
        filter.addAction(MediaPlaybackService.PLAYSTATE_CHANGED);
        filter.addAction(MediaPlaybackService.META_CHANGED);
        filter.addAction(MediaPlaybackService.ALBUM_ART_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mLockStateReceiver, filter);
    }

    private void registerSensor() {
        //获得一个加速度传感器
        Sensor mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorListener = new ShakeListener(this);
        if(mAccelerometerSensor != null){
            mSensorManager.registerListener(mSensorListener, mAccelerometerSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    private void initView(){
        mToken = MusicUtils.bindToService(this,mServiceCon);
        mPreference = getSharedPreferences(PREF_MUSIC_LOCK_INFO,MODE_PRIVATE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        mShakeView = findViewById(R.id.lay_lock_shake);
        mIvAlbumArt = (ImageView) findViewById(R.id.iv_lock_album_art);
        mClockView = (ClockView) findViewById(R.id.lay_lock_clock);
        mDateView = (TextClock) findViewById(R.id.tc_lock_date);
        mWeekView = (TextClock) findViewById(R.id.tc_lock_week);
        mAudioTitle = (TextView) findViewById(R.id.txt_lock_music_name);
        mArtistName = (TextView) findViewById(R.id.txt_lock_music_artsit);
        mLyricSingleView = (HBLyricSingleView) findViewById(R.id.lock_lyric_single_view);
        mVolumeSeekbar = (SeekBar) findViewById(R.id.seek_bar_lock_volume);
        mShuffleButton = (ImageButton) findViewById(R.id.ib_lock_music_shuffle);
        mPreButton = (ImageButton) findViewById(R.id.ib_lock_music_pre);
        mPlayButton = (ImageButton) findViewById(R.id.ib_lock_music_play);
        mNextButton = (ImageButton) findViewById(R.id.ib_lock_music_next);
        mSlideUnlockLayout = (SlideUnlockLayout) findViewById(R.id.lock_root);
        mSlideUnlockLayout.setOnSildingFinishListener(new SlideUnlockLayout.OnSildingFinishListener() {

            @Override
            public void onSildingFinish() {
                finish();
            }
        });
        mShuffleButton.setOnClickListener(this);
        mPreButton.setOnClickListener(this);
        mPlayButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
        mVolumeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.ADJUST_SAME);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        Patterns.update(this);
        refreshDate();
        updateAudioInfo(MediaPlaybackService.PLAYSTATE_CHANGED);
        setVolumeSeekbar();
        setShuffleButtonImage();
    }

    private void refreshDate(){
        mDateView.setFormat24Hour(Patterns.dateView);
        mDateView.setFormat12Hour(Patterns.dateView);
        mWeekView.setFormat24Hour(Patterns.weekView);
        mWeekView.setFormat12Hour(Patterns.weekView);
    }

    private void updateAudioInfo(String action){
        if (MusicUtils.sService == null) {
            return;
        }
        try {
            String titleName = MusicUtils.sService.getTrackName();
            String artistName = MusicUtils.sService.getArtistName();
            mAudioTitle.setText(titleName);
            mArtistName.setText(artistName);
            boolean showShakeView = !isShowedShakeView();
            if(MusicUtils.sService.isPlaying()){
                mPlayButton.setImageResource(R.drawable.hb_play);
                mShakeView.setVisibility(showShakeView ? View.VISIBLE : View.GONE);
            }else{
                mPlayButton.setImageResource(R.drawable.hb_pause);
                mShakeView.setVisibility(View.GONE);
            }
            Bitmap bm = MusicUtils.sService.getAlbumBitmap();
            if(action.equals(MediaPlaybackService.ALBUM_ART_CHANGED)){
                startFadeInFadeOutAnimation(bm);
            }else{
                if(bm != null && !bm.isRecycled()){
                    mIvAlbumArt.setImageBitmap(bm);
                }else{
                    mIvAlbumArt.setImageResource(R.drawable.back);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isShowedShakeView(){
        return mPreference.getBoolean(SHOWED_SHAKE,false);
    }

    private void saveShakeInfo(){
        SharedPreferences.Editor editor = mPreference.edit();
        editor.putBoolean(SHOWED_SHAKE, true);
        editor.commit();
    }

    private void startFadeInFadeOutAnimation(final Bitmap bitmap){
        if(mFadeInAnimator != null){
            mFadeInAnimator.cancel();
        }
        if(mFadeOutAnimator != null){
            mFadeOutAnimator.removeAllListeners();
            mFadeOutAnimator.cancel();
        }
        mFadeInAnimator = ObjectAnimator.ofFloat(mIvAlbumArt, "alpha", 0.1f, 1.0f);
        mFadeInAnimator.setDuration(300);

        mFadeOutAnimator = ObjectAnimator.ofFloat(mIvAlbumArt, "alpha", 1f, 0.1f);
        mFadeOutAnimator.setDuration(300);
        mFadeOutAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if(bitmap != null && !bitmap.isRecycled()){
                    mIvAlbumArt.setImageBitmap(bitmap);
                }else{
                    mIvAlbumArt.setImageResource(R.drawable.back);
                }
                mFadeInAnimator.start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mFadeOutAnimator.start();
    }

    private void setVolumeSeekbar(){
        int vol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVol = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mVolumeSeekbar.setMax(maxVol);
        mVolumeSeekbar.setProgress(vol);

    }

    // ready lyric
    private void startReadyShowLyric(){
        LogUtil.d(TAG,"---startReadyShowLyric()--");
        if(MusicUtils.sService == null){
            return;
        }
        try {
            String lrcpath = MusicUtils.sService.getLrcUri();
            if (!TextUtils.isEmpty(lrcpath)) {
                executor.submit(new LyricSingleThread(lrcpath));
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.e(TAG, "showAndReadLyc error", e);
        }
    }

    private int refreshSingleLrc() {
        try {
            if (MusicUtils.sService == null) {
                return 500;
            }
            long mDuration = MusicUtils.sService.duration();
            if (mDuration == -1 || mDuration == 0) {
                return 500;
            }
            long pos = MusicUtils.sService.position();
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

    private void queueNextRefreshSingleLrc(long nextLrc) {
        Message msg = mHandler.obtainMessage(REFRESH_SINGLE_LYRIC);
        mHandler.removeMessages(REFRESH_SINGLE_LYRIC);
        mHandler.sendMessageDelayed(msg, nextLrc);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        String cmd = null;
        switch (id){
            case R.id.ib_lock_music_pre:
                cmd = MediaPlaybackService.CMDPREVIOUS;
            break;

            case R.id.ib_lock_music_play:
                cmd = MediaPlaybackService.CMDTOGGLEPAUSE;
                break;

            case R.id.ib_lock_music_next:
                cmd = MediaPlaybackService.CMDNEXT;
                break;
            case R.id.ib_lock_music_shuffle:
                setShuffle();
                return;
        }
        if(cmd != null){
            Intent intent = new Intent(this, MediaPlaybackService.class);
            intent.putExtra(MediaPlaybackService.CMDNAME, cmd);
            startService(intent);
        }
    }

    private void setShuffle(){
        if (MusicUtils.sService == null) {
            return;
        }
        try {
            int mode = MusicUtils.sService.getRepeatMode();
            if (mode == MediaPlaybackService.REPEAT_NONE) {
                MusicUtils.sService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
                MusicUtils.sService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
            } else if (mode == MediaPlaybackService.REPEAT_ALL) {
                int shuffle = MusicUtils.sService.getShuffleMode();
                if (shuffle == MediaPlaybackService.SHUFFLE_NORMAL || shuffle == MediaPlaybackService.SHUFFLE_AUTO) {// shuffle
                    // ->
                    // CURRENT
                    MusicUtils.sService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
                    MusicUtils.sService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
                } else {
                    MusicUtils.sService.setShuffleMode(MediaPlaybackService.SHUFFLE_NONE);
                    MusicUtils.sService.setRepeatMode(MediaPlaybackService.REPEAT_CURRENT);
                }
            } else if (mode == MediaPlaybackService.REPEAT_CURRENT) {
                MusicUtils.sService.setShuffleMode(MediaPlaybackService.SHUFFLE_NORMAL);
                MusicUtils.sService.setRepeatMode(MediaPlaybackService.REPEAT_ALL);
            }
            setShuffleButtonImage();
        } catch (RemoteException ex) {
        }
    }

    private void setShuffleButtonImage() {
        if (MusicUtils.sService == null)
            return;
        try {
            switch (MusicUtils.sService.getRepeatMode()) {
                case MediaPlaybackService.REPEAT_ALL:
                    if (MusicUtils.sService.getShuffleMode() == MediaPlaybackService.SHUFFLE_NORMAL) {
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

    private void doScreenOnOff(boolean screenOn){
        LogUtil.d(TAG,"---doScreenOnOff()--screenOn = " + screenOn);
        if(screenOn){
            if(MusicUtils.sService == null){
                return;
            }
            try {
                if (MusicUtils.sService.isPlaying()) {
                    queueNextRefreshSingleLrc(0);
                }
            }catch (Exception e){

            }
        }else{
            mHandler.removeMessages(REFRESH_SINGLE_LYRIC);
        }
    }

    private void doPlayAndPause(){
       if(MusicUtils.sService == null){
           return;
       }
       try{
           if(MusicUtils.sService.isPlaying()){
               queueNextRefreshSingleLrc(0);
               registerSensor();
           }else{
               mHandler.removeMessages(REFRESH_SINGLE_LYRIC);
               mSensorManager.unregisterListener(mSensorListener);
           }
       }catch (Exception e){

       }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mLockStateReceiver);
        getContentResolver().unregisterContentObserver(mVolumeObserver);
        mSensorManager.unregisterListener(mSensorListener);
        mHandler.removeMessages(REFRESH_SINGLE_LYRIC);
        if(mToken != null){
            MusicUtils.unbindFromService(mToken);
        }
        Intent intent = new Intent();
        intent.setAction(MediaPlaybackService.LOCK_SCREEN_CHANGED);
        intent.putExtra("islock",false);
        sendBroadcast(intent);
    }

    private static final class Patterns {
        static String dateView;
        static String weekView;
        static String cacheKey;

        static void update(Context context) {
            final Locale locale = Locale.getDefault();
            final Resources res = context.getResources();
            final String dateViewSkel = res.getString(R.string.hb_month_day_no_year);
            final String weekViewSkel = res.getString(R.string.hb_week_day);
            final String key = locale.toString() + dateViewSkel + weekViewSkel;
            if (key.equals(cacheKey)) return;
            dateView = DateFormat.getBestDateTimePattern(locale, dateViewSkel);
            weekView = DateFormat.getBestDateTimePattern(locale, weekViewSkel);
            cacheKey = key;
        }
    }

    private BroadcastReceiver mLockStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtil.d(TAG, "onReceive(): action = " + intent.getAction()) ;
            if(MediaPlaybackService.PLAYSTATE_CHANGED.equals(action) ||
                    MediaPlaybackService.META_CHANGED.equals(action) ||
                    MediaPlaybackService.ALBUM_ART_CHANGED.equals(action)){
                updateAudioInfo(action);
                if(MediaPlaybackService.PLAYSTATE_CHANGED.equals(action)){
                    doPlayAndPause();
                }else if(MediaPlaybackService.META_CHANGED.equals(action)){
                    startReadyShowLyric();
                }
            } else if(Intent.ACTION_SCREEN_OFF.equals(action)){
                doScreenOnOff(false);
            } else if(Intent.ACTION_SCREEN_ON.equals(action)){
                doScreenOnOff(true);
            }
        }
    };

    private ServiceConnection mServiceCon = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            updateAudioInfo(MediaPlaybackService.PLAYSTATE_CHANGED);
            startReadyShowLyric();
            setShuffleButtonImage();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private class VolumeChangeObserver extends ContentObserver {
        public VolumeChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            LogUtil.d(TAG, "onChange(): FormatChangeObserver") ;
            setVolumeSeekbar();
            mClockView.updateTime();
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case INIT_SINGLE_LYRIC:
                    if (mLyricSingleView != null) {
                        mLyricSingleView.setHasLyric(true);
                        mLyricSingleView.setTextsEx();
                        refreshSingleLrc();
                    }
                    break;
                case REFRESH_SINGLE_LYRIC:
                    long nextSingleLrc = 500;
                    try {
                        if (MusicUtils.sService.isPlaying()) {
                            nextSingleLrc = refreshSingleLrc()  ;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    queueNextRefreshSingleLrc(nextSingleLrc);
                    break;
            }
        }
    };

    private class LyricSingleThread extends Thread {
        private String filename = null;

        public LyricSingleThread(String filename) {
            this.filename = filename;
        }

        @Override
        public void run() {
            if (mLyricSingleView != null) {
                mHandler.removeMessages(INIT_SINGLE_LYRIC);
                mLyricSingleView.read(filename);
                mHandler.sendEmptyMessage(INIT_SINGLE_LYRIC);
            }
        }
    }

    public class ShakeListener implements SensorEventListener {
        private final int UPDATE_INTERVAL = 100;
        private final int shakeThreshold = 3500;
        long mLastUpdateTime;
        float mLastX, mLastY, mLastZ;
        private WeakReference<HBLockActivity> mHbActivity;

        public ShakeListener(HBLockActivity activity){
            mHbActivity = new WeakReference<HBLockActivity>(activity);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            HBLockActivity mHbLockActivity = mHbActivity.get();
            long currentTime = System.currentTimeMillis();
            long diffTime = currentTime - mLastUpdateTime;
            if (diffTime < UPDATE_INTERVAL) {
                return;
            }
            mLastUpdateTime = currentTime;
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float deltaX = x - mLastX;
            float deltaY = y - mLastY;
            float deltaZ = z - mLastZ;
            mLastX = x;
            mLastY = y;
            mLastZ = z;
            float delta = (float) (Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / diffTime * 10000);
            // 当加速度的差值大于指定的阈值，认为这是一摇晃
            if (delta > shakeThreshold && mHbLockActivity != null) {
                mHbLockActivity.mVibrator.vibrate(200);
                Intent intent = new Intent(mHbLockActivity, MediaPlaybackService.class);
                intent.putExtra(MediaPlaybackService.CMDNAME, MediaPlaybackService.CMDNEXT);
                startService(intent);
                if(!isShowedShakeView()){
                    saveShakeInfo();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }
}
