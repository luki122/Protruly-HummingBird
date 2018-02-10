package cn.com.protruly.soundrecorder;

import android.content.Intent;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.com.protruly.soundrecorder.clip.ClipView;
import cn.com.protruly.soundrecorder.clip.DecodedSampleProcessor;
import cn.com.protruly.soundrecorder.clip.Mp3ClipperTask;
import cn.com.protruly.soundrecorder.clip.Mp3Decoder;
import cn.com.protruly.soundrecorder.clip.ProgressDialogManager;
import cn.com.protruly.soundrecorder.clip.UserInteractionListener;
import cn.com.protruly.soundrecorder.clip.UpdateViewListener;
import cn.com.protruly.soundrecorder.common.BaseActivity;
import cn.com.protruly.soundrecorder.R;
import cn.com.protruly.soundrecorder.common.ToolbarManager;
import cn.com.protruly.soundrecorder.managerUtil.AudioPlayManager;
import cn.com.protruly.soundrecorder.recordlist.FileNameInputDialog;
import cn.com.protruly.soundrecorder.util.FileInfo;
import cn.com.protruly.soundrecorder.util.FileNameDialogUtil;
import cn.com.protruly.soundrecorder.util.FilePathUtil;
import cn.com.protruly.soundrecorder.util.GlobalConstant;
import cn.com.protruly.soundrecorder.util.LogUtil;

/**
 * Created by sqf on 17-8-14.
 */

public class ClipActivity extends BaseActivity implements UpdateViewListener, View.OnClickListener, UserInteractionListener {

    private static final String TAG = "ClipActivity";

    private ClipView mClipView;
    private DecodedSampleProcessor mDecodedSampleProcessor;

    private TextView mLeftClipTimeTextView;
    private TextView mRightClipTimeTextView;
    private TextView mPlayPositionTimeTextView;

    private Button mCancelButton;
    private ImageButton mPlayPauseButton;
    private Button mSaveButton;
    private AudioPlayManager mAudioPlayManager;
    private ClipView.TimeInfo mTimeInfo;

    private String mFilePath;
    private boolean mNeedsResumePlaying;

    public static final String KEY_CLIP_FILE_PATH = "CLIP_FILE_PATH";

    @Override
    protected void initData() {
        mDecodedSampleProcessor = new DecodedSampleProcessor(mClipView, mClipView.getHandler());
        mAudioPlayManager = AudioPlayManager.getInstance();
        mAudioPlayManager.setPlayModeChangedCallBack(mModeChangedCallBack);
        mAudioPlayManager.onInitAudioPlayManager(this);
    }

    @Override
    protected void initView() {
        setHbContentView(R.layout.clip_activity);
        mClipView = (ClipView) findViewById(R.id.clip_view);
        mLeftClipTimeTextView = (TextView) findViewById(R.id.left_clip_time);
        mRightClipTimeTextView = (TextView) findViewById(R.id.right_clip_time);
        mPlayPositionTimeTextView = (TextView) findViewById(R.id.play_position_time);

        mClipView.setUpdateViewListener(this);
        mClipView.setUserInteractionListener(this);

        mCancelButton = (Button)findViewById(R.id.cancel);
        mPlayPauseButton = (ImageButton)findViewById(R.id.play_pause);
        mSaveButton = (Button)findViewById(R.id.save);

        mCancelButton.setOnClickListener(this);
        mPlayPauseButton.setOnClickListener(this);
        mSaveButton.setOnClickListener(this);

        mPlayPauseButton.setImageResource(R.drawable.clip_activity_play_btn);
        mPlayPauseButton.setEnabled(false);
        mSaveButton.setEnabled(false);

        //Typeface robotoLight = Typeface.createFromFile("/system/fonts/Roboto-Light.ttf");
        //mLeftClipTimeTextView.setTypeface(robotoLight);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mFilePath = intent.getStringExtra(KEY_CLIP_FILE_PATH);

        //mFilePath = "/storage/emulated/0/新录音7.mp3";

        // mFilePath = "/storage/emulated/0/a.mp3"; //For test
        Mp3Decoder mp3Info = new Mp3Decoder(this, mFilePath, mDecodedSampleProcessor);
        mp3Info.parseWaveFormAsynchronously();

        mToolbarManager.setToolbarTitle(R.string.clip_activity_title);
        //inflateToolbarMenu(R.menu.recordlist_toolbar_menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSPKIcon();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if(mAudioPlayManager==null)return true;
        switch (item.getItemId()){
            case  R.id.spk_choose_menu_one:
                if(mAudioPlayManager.getAudioPlay_SPKMode()==AudioPlayManager.PLAY_SPK_MODE_SPK){
                    mAudioPlayManager.setAudioPlayMode_TelepReceiver(true);
                }else{
                    mAudioPlayManager.setAudioPlayMode_Speeker();
                }
                break;
            case R.id.spk_outdoor_menu:
                   mAudioPlayManager.setAudioPlayMode_Speeker();
                break;
            case R.id.spk_hear_menu:
                   mAudioPlayManager.setAudioPlayMode_TelepReceiver(true);
                break;
            case R.id.spk_bluetooth_menu:
                   mAudioPlayManager.setAudioPlayMode_BlueTooth();
                break;

        }
        updateSPKIcon();
        return super.onMenuItemClick(item);
    }

    private void updateSPKIcon(){
        if(mAudioPlayManager!=null){
            int SPK_Menu_id = 0;
            if(mAudioPlayManager.isBlueToothConnect()){
                mToolbarManager.switchToStatus(ToolbarManager.RECORD_EDIT_SPK_MENU_LIST);
                SPK_Menu_id = R.id.spk_choose_menu_list;
            }else{
                mToolbarManager.switchToStatus(ToolbarManager.RECORD_EDIT_SPK_MENU_ONE);
                SPK_Menu_id = R.id.spk_choose_menu_one;
            }
            switch (mAudioPlayManager.getAudioPlay_SPKMode()){
                case AudioPlayManager.PLAY_SPK_MODE_SPK: mToolbarManager.setToolbarMenuItemIcon(SPK_Menu_id,R.drawable.icon_spk_outdoor); break;
                case AudioPlayManager.PLAY_SPK_MODE_REC: mToolbarManager.setToolbarMenuItemIcon(SPK_Menu_id,R.drawable.icon_spk_hear); break;
                case AudioPlayManager.PLAY_SPK_MODE_BLU: mToolbarManager.setToolbarMenuItemIcon(SPK_Menu_id,R.drawable.icon_spk_bluetooth); break;
            }
        }
    }

    @Override
    public void updateTimeInfo(ClipView.TimeInfo timeInfo) {

        mTimeInfo = timeInfo;

        long leftClipTime = timeInfo.leftClipTime;
        mLeftClipTimeTextView.setText(formatTime(leftClipTime));
        //mLeftClipTimeTextView.setTag(leftClipTime);

        long rightClipTime = timeInfo.rightClipTime;
        mRightClipTimeTextView.setText(formatTime(rightClipTime));
        //mRightClipTimeTextView.setTag(rightClipTime);

        long playPositionTime = timeInfo.playPositionTime;

        mPlayPositionTimeTextView.setText(formatTime(playPositionTime));
        //mPlayPositionTimeTextView.setTag(playPositionTime);
    }

    @Override
    public void notifyProcessFinished() {
        mPlayPauseButton.setEnabled(true);
        mSaveButton.setEnabled(true);
    }

    private String formatTime(long timeInUs) {
        long seconds = timeInUs / 1000000;
        int minute = (int)(seconds / 60);
        int second = (int)(seconds % 60);
        StringBuilder builder = new StringBuilder();
        String minuteText = minute < 10 ? ("0" + minute) : ("" + minute);
        String secondText = second < 10 ? ("0" + second) : ("" + second);
        builder.append(minuteText);
        builder.append(":");
        builder.append(secondText);
        return builder.toString();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.play_pause) {
            //String filePath = "/storage/emulated/0/a.mp3";
            LogUtil.i(TAG, "play_pause clicked");
            LogUtil.i(TAG, "play:" + mFilePath +
                    " mTimeInfo.playPositionTime:" +  mTimeInfo.playPositionTime +
                    " mTimeInfo.rightClipTime:" + mTimeInfo.rightClipTime);
            //mAudioPlayManager.playForAudioEdit(mFilePath, mTimeInfo.playPositionTime / 1000, mTimeInfo.rightClipTime / 1000, mHandler);
            playOrPause();//play
        } else if(id == R.id.cancel) {
            if(isPlaying()) {
                playOrPause();//pause
            }
            this.finish();
        } else if(id == R.id.save) {
            String title = getString(R.string.input_file_name);
            /*
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_hh:mm:ss");
            String dateText = sdf.format(new Date());
            String name = dateText + ".mp3";
            */
            //Mp3ClipperTask clipper = new Mp3ClipperTask(mFilePath, mTimeInfo.leftClipTime, mTimeInfo.rightClipTime, "/storage/emulated/0/" + dateText + ".mp3");
            //FileInfo fileInfo = new FileInfo("/storage/emulated/0/" + dateText + ".mp3");
            File file = FilePathUtil.checkAndReture(this);
            LogUtil.i(TAG, "file. getPath():" + file.getPath() + " getName:" + file.getName());
            String name = FilePathUtil.getName(file.getName());
            LogUtil.i(TAG, "name:" + name);
            FileNameDialogUtil.createNameInputDialog(this, title, name, ".mp3", new FileInfo(file), new FileNameInputDialog.OnConfirmedListener() {
                @Override
                public void onConfirmed(FileInfo fileInfo, String str) {
                    LogUtil.i(TAG, "str: " + str + " ===== ");
                    Mp3ClipperTask clipper = new Mp3ClipperTask(ClipActivity.this, mFilePath, mTimeInfo.leftClipTime, mTimeInfo.rightClipTime, fileInfo.getPath());
                    clipper.execute();
                }
            });
        }
    }

    @Override
    public void onUserInteractionTouchDown() {
        if(isPlaying()) {
            mNeedsResumePlaying = true;
            playOrPause();
        }
    }

    @Override
    public void onUserInteractionTouchUp() {
        if(mNeedsResumePlaying) {
            playOrPause();
            mNeedsResumePlaying = false;
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            if(what == GlobalConstant.PLAY_START) {
                LogUtil.i(TAG, "PLAY_START" );
                mPlayPauseButton.setImageResource(R.drawable.clip_activity_pause_btn);
            } else if(what == GlobalConstant.PLAY_GOING) {
                //LogUtil.i(TAG, "PLAY_GOING --> msg.obj != null:" + (msg.obj != null));
                if(msg.obj != null) {
                    long time = ((Long) msg.obj) * 1000; // convert ms to us
                    LogUtil.i(TAG, "PLAY_GOING --> msg.obj time:" + time);
                    boolean end = mClipView.updatePlayPositionWhenPlaying(time);
                    if(end) {
                        //mClipView.resetPlayPositionToLeftClip();
                    }
                }
            } else if(what == GlobalConstant.PLAY_STOP) {
                LogUtil.i(TAG, "PLAY_STOP" );
                mPlayPauseButton.setImageResource(R.drawable.clip_activity_play_btn);
                //if(mClipView.playPositionReachesRightClipPosition()) {
                    mClipView.resetPlayPositionToLeftClip();
                //}
            } else if(what == GlobalConstant.PLAY_PAUSE) {
                LogUtil.i(TAG, "PLAY_PAUSE" );
                mPlayPauseButton.setImageResource(R.drawable.clip_activity_play_btn);
            } else if(what == GlobalConstant.PLAY_ERROR) {
                LogUtil.i(TAG, "PLAY_ERROR" );
            }
        }
    };

    private void playOrPause() {
        if(!isPlaying()) {
            mAudioPlayManager.playForAudioEdit(mFilePath, mTimeInfo.playPositionTime / 1000, mTimeInfo.rightClipTime / 1000, mHandler);
        } else {
            mAudioPlayManager.playForAudioEdit(mFilePath, -1, -1, mHandler); //pause
        }
    }

    private boolean isPlaying() {
        int playStatus = mAudioPlayManager.GetCurrentPlayStats();
        return playStatus == GlobalConstant.PLAY_GOING || playStatus == GlobalConstant.PLAY_START;
    }

     //音频输出
    AudioPlayManager.playModeChangedCallBack mModeChangedCallBack = new AudioPlayManager.playModeChangedCallBack() {
        @Override
        public void onModeChanged() {
            updateSPKIcon();
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            // 音量减小
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if(mAudioPlayManager.adjustAudioStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,AudioManager.FX_FOCUS_NAVIGATION_UP)){
                    return true;
                }
                break;
            // 音量增大
            case KeyEvent.KEYCODE_VOLUME_UP:
                if(mAudioPlayManager.adjustAudioStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,AudioManager.FX_FOCUS_NAVIGATION_UP)){
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }
   //音频输出
}
