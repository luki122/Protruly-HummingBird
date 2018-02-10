package cn.com.protruly.soundrecorder.managerUtil;

import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import cn.com.protruly.soundrecorder.R;
import cn.com.protruly.soundrecorder.util.GlobalConstant;
import cn.com.protruly.soundrecorder.util.LogUtil;

import static android.provider.Settings.Global.DEVICE_NAME;


/**
 * Created by wenwenchao on 2017/8/17.
 */


public class AudioPlayManagerService extends Service {

    private final static String TAG = "AudioPlayManagerService";
    private MediaPlayer mMediaPlayer;
    private final static int PLAY_START = GlobalConstant.PLAY_START;
    private final static int PLAY_GOING = GlobalConstant.PLAY_GOING;
    private final static int PLAY_PAUSE = GlobalConstant.PLAY_PAUSE;
    private final static int PLAY_ERROR = GlobalConstant.PLAY_ERROR;
    private final static int PLAY_STOP =  GlobalConstant.PLAY_STOP;
    private final static int PLAY_SPK_MODE_SPK = AudioPlayManager.PLAY_SPK_MODE_SPK;
    private final static int PLAY_SPK_MODE_REC = AudioPlayManager.PLAY_SPK_MODE_REC;
    private final static int PLAY_SPK_MODE_BLU = AudioPlayManager.PLAY_SPK_MODE_BLU;
    private final static int PLAY_SPK_MODE_UNKOWN=3;

    private int mPlayStatus = GlobalConstant.PLAY_STOP;
    private String mCurrentPlayFilePath;
    private Handler mHandler;
    private Timer mSeekbarTimer;
    private Boolean mSeekbarHandleFlag = false;
    private int savePauseTime;


    public AudioPlayManagerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return new AudioPlayBind();
    }
    class AudioPlayBind extends Binder
    {
        public AudioPlayManagerService getService()
        {
            return AudioPlayManagerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initMediaPlayer();
        initAudioManager();
        registerBluetoothReceiver();
        initSensorManager();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        onDestroyPlay();
        unRegisterBluetoothReceiver();
    }

    void initMediaPlayer(){
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                onStopPlay();
                Log.d("audioplay", "播放完毕");
            }
        });

    }

    public void onStartPlay(String path, int startplaytime, Handler handler){
          if(path==null)return;
          if(mMediaPlayer!=null){
              if(path == mCurrentPlayFilePath && mPlayStatus != PLAY_STOP) {    //新歌和停止的歌需要重新重头装载播放
                  onPausePlay();  //播放暂停二合一的按钮
                  Log.d("audioplay","onStartPlay->重复播放同一曲");
              }else{
                  Log.d("audioplay","onStartPlay->从头开始新的一曲");
                  if(mPlayStatus != PLAY_STOP){
                      onStopPlay();
                  }
                  startPlayAtPos(mMediaPlayer,new File(path),startplaytime,-1,handler);
              }
          } else{
              Log.d("audioplay","onStartPlay->重新初始化播放器从头开始");
              initMediaPlayer();
              startPlayAtPos(mMediaPlayer,new File(path),0,-1,handler);
          }
    }
    public void onPausePlay(){
        if(checkErrorForAudioRecorder())return;
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            savePauseTime = mMediaPlayer.getCurrentPosition();
            mPlayStatus = PLAY_PAUSE;
            handleMsgForUI(mHandler, GlobalConstant.PLAY_PAUSE, null);
            setProximitySensorEnabled(false);
        } else if(mPlayStatus==PLAY_PAUSE) {
                mMediaPlayer.seekTo(savePauseTime);
                mMediaPlayer.start();
                mPlayStatus = PLAY_START;
                handleMsgForUI(mHandler, GlobalConstant.PLAY_START, null);
                setProximitySensorEnabled(true);
        }
    }
    public void SeektoPlayAtPos(String path, long pos, Handler handler){      //强制播放，用于标记播放
        if(path==null)return;
        if(checkErrorForAudioRecorder())return;
        if(path!=mCurrentPlayFilePath){          //第一次播放或者另起一首
            mCurrentPlayFilePath = path;
            if(mMediaPlayer==null){
                initMediaPlayer();
            }
            if(mMediaPlayer.isPlaying() || mPlayStatus == PLAY_PAUSE){   //重设播放器
                onStopPlay();
            }
            startPlayAtPos(mMediaPlayer,new File(path),(int)pos,-1,handler);
            return;
        }else{             //同一首播放
            if(mPlayStatus == PLAY_STOP){
                startPlayAtPos(mMediaPlayer,new File(path),(int)pos,-1,handler);
            }else if(mPlayStatus == PLAY_PAUSE ){
                mMediaPlayer.seekTo((int) pos);
                mMediaPlayer.start();
                mPlayStatus = PLAY_START;
                setProximitySensorEnabled(true);
                handleMsgForUI(handler, GlobalConstant.PLAY_START, null);
            }else{
                if(mMediaPlayer.isPlaying()) {
                    mMediaPlayer.seekTo((int) pos);
                }
            }
        }
    }

    public void playForAudioEdit(String path, long satrtPos, long endPos,Handler handler){
        if(path==null)return;
        if(checkErrorForAudioRecorder())return;
        if(mMediaPlayer==null)initMediaPlayer();
        if(satrtPos !=-1 || endPos !=-1) {
            if(mPlayStatus== PLAY_GOING || mPlayStatus == PLAY_PAUSE){
                //onStopPlay();//先停止初始化播放器
                mMediaPlayer.stop();
                mMediaPlayer.reset();
                savePauseTime = 0;
                if(null!=mSeekbarTimer){
                    mSeekbarTimer.cancel();
                    mSeekbarTimer = null;
                }
            }
            if(handler!=null)mHandler = handler;
            startPlayAtPos(mMediaPlayer,new File(path),(int)satrtPos,(int)endPos,mHandler);

        }else{

            if (mMediaPlayer.isPlaying() || mPlayStatus == PLAY_PAUSE) {
                onPausePlay();
            } else {
                onStopPlay();
            }
        }

    }



    public int GetCurrentPlayStats(){
        if(mMediaPlayer==null)return -1;
        return mPlayStatus;
    }


    public long GetNowPosForMark(){
        if(mMediaPlayer.isPlaying()){
            return mMediaPlayer.getCurrentPosition();
        }else if(mPlayStatus == PLAY_STOP || mPlayStatus == PLAY_PAUSE ){
            return savePauseTime;
        }
        return -1;
    }


    public void onStopPlay(){
        if(checkErrorForAudioRecorder())return;
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            savePauseTime = 0;
            mPlayStatus = PLAY_STOP;
            if(null!=mSeekbarTimer){
                mSeekbarTimer.cancel();
                mSeekbarTimer = null;
            }
            setProximitySensorEnabled(false);
            handleMsgForUI(mHandler, GlobalConstant.PLAY_STOP,null);
    }
    public void onDestroyPlay(){
        savePauseTime = 0;
        if(mMediaPlayer!=null){
            if(mMediaPlayer.isPlaying())mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer=null;
        }
        if(mSeekbarTimer!=null){
            mSeekbarTimer.cancel();
            mSeekbarTimer=null;
        }
        if(mHandler!=null){
            mHandler=null;
        }
        setProximitySensorEnabled(false);
    }


    private void startPlayAtPos(final MediaPlayer mediaPlayer, final File srcfile, final int startpos, final int endpos,final Handler handler) {
        if(srcfile.exists())
        {
            try {

                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);//设置音频类型
                final String path = srcfile.getAbsolutePath();
                mediaPlayer.setDataSource(path);//设置mp3数据源
                mediaPlayer.prepareAsync();//数据缓冲
                /*监听缓存 事件，在缓冲完毕后，开始播放*/
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    public void onPrepared(MediaPlayer mp) {
                        mp.start();
                        mp.seekTo(startpos);
                       // seekbar.setMax(mediaPlayer.getDuration());
                       // seekbar.invalidate();
                        if(handler!=null){
                            mHandler = handler;
                            handleMsgForUI(mHandler, GlobalConstant.PLAY_START,null);
                        }
                        mCurrentPlayFilePath = path;
                        mPlayStatus = PLAY_START;
                        setProximitySensorEnabled(true);
                        Log.d("wenwenchao","getFile->name="+srcfile.getName()+"  timelong="+mediaPlayer.getDuration());
                    }
                });
                //监听播放时回调函数
              //  if(seekbar != null) {
              //      mSeekBar = seekbar;
              //      seekbar.setOnSeekBarChangeListener(new MySeekBarListener());
                    if (mSeekbarTimer == null) mSeekbarTimer = new Timer();
                    mSeekbarTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Boolean isPlaying;
                            try {
                                isPlaying = mediaPlayer.isPlaying();
                            }
                            catch (IllegalStateException e) {
                                isPlaying = false;
                            }
                            if (isPlaying && !mSeekbarHandleFlag) {
                                int time = mediaPlayer.getCurrentPosition();
               //                 seekbar.setProgress(time);
                                Log.d("SPKMode","    getAudioPlay_SPKMode="+getAudioPlay_SPKMode()+"   mAudioManager.getMode="+mAudioManager.getMode()+"    mProximitySensorEnabled="+(mProximitySensorEnabled?"true":"false"));
                                mPlayStatus = PLAY_GOING;
                                handleMsgForUI(mHandler, GlobalConstant.PLAY_GOING,(long)time);
                                if(endpos!=-1 && time>=endpos){
                                    onStopPlay();
                                }
                            }
                        }
                    }, 0, 200);
               // }

            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "播放异常", Toast.LENGTH_SHORT).show();
                mPlayStatus = PLAY_ERROR;
                e.printStackTrace();
                System.out.println(e);
            }
        } else{
            mPlayStatus = PLAY_ERROR;
            Toast.makeText(getApplicationContext(), "文件不存在", Toast.LENGTH_SHORT).show();
        }
    }

    public void onSeekBarChange(long progress,Boolean isSeekChanging){
         if(isSeekChanging){
             mSeekbarHandleFlag = true;
             if(mPlayStatus == PLAY_STOP || mPlayStatus == PLAY_PAUSE ){
                 savePauseTime = (int)progress;
             }
         }else{
             mSeekbarHandleFlag = false;
             mMediaPlayer.seekTo((int)progress);
         }
    }

    public void onProgressChanged(long progress){
        if(mPlayStatus == PLAY_STOP || mPlayStatus == PLAY_PAUSE ){
            savePauseTime = (int)progress;
        }
    }
    public void onStartTrackingTouch(long progress){
        mSeekbarHandleFlag = true;
    }
    public void onStopTrackingTouch(long progress){
        mSeekbarHandleFlag = false;
        mMediaPlayer.seekTo((int)progress);
    }



    /*进度条处理*/
    public class MySeekBarListener implements SeekBar.OnSeekBarChangeListener {

        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            if(mPlayStatus == PLAY_STOP || mPlayStatus == PLAY_PAUSE ){
                savePauseTime = progress;
            }
        }

        /*滚动时,应当暂停后台定时器*/
        public void onStartTrackingTouch(SeekBar seekBar) {
            mSeekbarHandleFlag = true;
        }
        /*滑动结束后，重新设置值*/
        public void onStopTrackingTouch(SeekBar seekBar) {
            mSeekbarHandleFlag = false;
            mMediaPlayer.seekTo(seekBar.getProgress());
        }
    }

    public void handleMsgForUI(Handler handler, int msgwhat, Object obj){
        if(handler!=null){
            Message msg = handler.obtainMessage();
            if(msg==null)msg = new Message();
            msg.what =msgwhat;
            msg.obj = obj;
            handler.sendMessage(msg);
        }

    }

    private Boolean checkErrorForAudioRecorder(){
        return mMediaPlayer==null || mPlayStatus == PLAY_ERROR;
    }











    //音频通道切换
    AudioManager mAudioManager;


    private void initAudioManager(){
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        isBlueToothCon=isBlueToothCon();
        if(AudioPlayManager.SPK_MODE==PLAY_SPK_MODE_SPK){
            setAudioPlayMode_Speeker();
        }else if(AudioPlayManager.SPK_MODE==PLAY_SPK_MODE_REC){
            setAudioPlayMode_TelepReceiver(true);
        }else if(AudioPlayManager.SPK_MODE==PLAY_SPK_MODE_BLU){
            setAudioPlayMode_BlueTooth();
        }else{
            setAudioPlayMode_Speeker();
        }
        play_channel_changed();
    }

    public int getAudioPlay_SPKMode(){

        return AudioPlayManager.SPK_MODE;
    }

    public Boolean setAudioPlayMode_Speeker(){
        if(mAudioManager==null)return false;
        mAudioManager.setMicrophoneMute(false);
       if(isBlueToothCon /*|| isWiredHeadsetOn()*/){
           mAudioManager.setMode(AudioManager.STREAM_MUSIC);
       } else{
           mAudioManager.setMode(AudioManager.MODE_NORMAL);
       }
       // if(mAudioManager.isBluetoothA2dpOn())mAudioManager.setBluetoothA2dpOn(false);
        mAudioManager.setSpeakerphoneOn(true);
        //mAudioManager.setMode(AudioManager.STREAM_MUSIC);
        AudioPlayManager.SPK_MODE = PLAY_SPK_MODE_SPK;

        return true;

       /* try {
            Class clazz = Class.forName("android.media.AudioSystem");
            Method m = clazz.getMethod("setForceUse", new Class[]{int.class, int.class});
            m.setAccessible(true);
            m.invoke(null, 1, 1);
            nowAudioPlay_SPKMode = PLAY_SPK_MODE_SPK;
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;*/



    }
    public Boolean setAudioPlayMode_TelepReceiver(Boolean isForce){
        if (mAudioManager == null) return false;
/*
        // 听筒模式下设置为false
        mAudioManager.setSpeakerphoneOn(false);
        // 设置成听筒模式
        mAudioManager.setMode( AudioManager.MODE_IN_COMMUNICATION);
        // 设置为通话状态
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
*/

        mAudioManager.setSpeakerphoneOn(false);
        if (mAudioManager.getMode() != AudioManager.MODE_IN_COMMUNICATION) {
            mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        }

        try {
            Class clazz = Class.forName("android.media.AudioSystem");
            Method m = clazz.getMethod("setForceUse", new Class[]{int.class, int.class});
            m.setAccessible(true);
            m.invoke(null, 0, 0);
            if(isForce)AudioPlayManager.SPK_MODE = PLAY_SPK_MODE_REC;
            return true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
      //  mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
       //         AudioManager.STREAM_VOICE_CALL);

    }

    public Boolean setAudioPlayMode_BlueTooth(){
        if (mAudioManager == null) return false;

        if(isBlueToothCon){
            mAudioManager.setSpeakerphoneOn(false);
            if (mAudioManager.getMode() != AudioManager.MODE_NORMAL) {
                mAudioManager.setMode(AudioManager.MODE_NORMAL);
            }
            try {
                Class clazz = Class.forName("android.media.AudioSystem");
                Method m = clazz.getMethod("setForceUse", new Class[]{int.class, int.class});
                m.setAccessible(true);
                m.invoke(null, 1, 4);
                AudioPlayManager.SPK_MODE = PLAY_SPK_MODE_BLU;
                return true;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        }else {
            Toast.makeText(this,getString(R.string.bluetooth_disable_warn),Toast.LENGTH_SHORT).show();
            return false;
        }
       // mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
               // AudioManager.STREAM_MUSIC);
    }

    private void play_channel_changed(){
        if(mPlayModeChangedCallBack!=null){
            mPlayModeChangedCallBack.onModeChanged();
        }
    }
    AudioPlayManager.playModeChangedCallBack mPlayModeChangedCallBack;
    public void setPlayModeChangedCallBack(AudioPlayManager.playModeChangedCallBack callBack){
        mPlayModeChangedCallBack = callBack;
    }


    BluetoothAdapter mBluetoothAdapter;
    BroadcastReceiver mBroadcastReceiver;
    BluetoothDevice mBluetoothDevice;
    BluetoothA2dp mBluetoothA2dp = null;
    Boolean isBlueToothCon = false;
    private void registerBluetoothReceiver(){

        //监听广播
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BluetoothDevice device;
                switch (intent.getAction()) {
                    case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                        //<editor-fold>
                        switch (intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1)) {
                            case BluetoothA2dp.STATE_CONNECTING:
                                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                                Log.i(TAG, "device: " + device.getName() +" connecting");
                                break;
                            case BluetoothA2dp.STATE_CONNECTED:
                                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                                isBlueToothCon = true;
                                Log.i(TAG, "device: " + device.getName() +" connected");
                                setAudioPlayMode_BlueTooth();
                                play_channel_changed();
                                //连接成功，开始播放
                                // startPlay();
                                break;
                            case BluetoothA2dp.STATE_DISCONNECTING:
                                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                                Log.i(TAG, "device: " + device.getName() +" disconnecting");
                                break;
                            case BluetoothA2dp.STATE_DISCONNECTED:
                                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                                isBlueToothCon = false;
                                Log.i(TAG, "device: " + device.getName() +" disconnected");
                                setAudioPlayMode_Speeker();
                                play_channel_changed();
//                                setResultPASS();
                                break;
                            default:
                                break;
                        }
                        //</editor-fold>
                        break;
                    case BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED:
                        //<editor-fold>
                        int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1);
                        switch (state) {
                            case BluetoothA2dp.STATE_PLAYING:
                                Log.i(TAG, "state: playing.");
                                break;
                            case BluetoothA2dp.STATE_NOT_PLAYING:
                                Log.i(TAG, "state: not playing");
                                break;
                            default:
                                Log.i(TAG, "state: unkown");
                                break;
                        }
                        //</editor-fold>
                        break;
                    case BluetoothDevice.ACTION_FOUND:
                        //<editor-fold>
                        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        int deviceClassType = device.getBluetoothClass().getDeviceClass();
                        //找到指定的蓝牙设备
                        if ((deviceClassType == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET
                                || deviceClassType == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES)
                                && device.getName().equals(DEVICE_NAME)) {
                            Log.i(TAG, "Found device:" + device.getName());
                            mBluetoothDevice = device;
                            //start bond，开始配对
                            //createBond();
                        }
                        //</editor-fold>
                        break;
                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                        //<editor-fold>
                        int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,BluetoothDevice.BOND_NONE);
                        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        switch (bondState){
                            case BluetoothDevice.BOND_BONDED:  //配对成功
                                Log.i(TAG,"Device:"+device.getName()+" bonded.");
                                mBluetoothAdapter.cancelDiscovery();  //取消搜索
                                //connect();  //连接蓝牙设备
                                break;
                            case BluetoothDevice.BOND_BONDING:
                                Log.i(TAG,"Device:"+device.getName()+" bonding.");
                                break;
                            case BluetoothDevice.BOND_NONE:
                                Log.i(TAG,"Device:"+device.getName()+" not bonded.");
                                //不知道是蓝牙耳机的关系还是什么原因，经常配对不成功
                                //配对不成功的话，重新尝试配对
                                //createBond();
                                break;
                            default:
                                break;

                        }

                        //</editor-fold>
                        break;
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        //<editor-fold>
                        state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                        switch (state) {
                            case BluetoothAdapter.STATE_TURNING_ON:
                                Log.i(TAG, "BluetoothAdapter is turning on.");
                                break;
                            case BluetoothAdapter.STATE_ON:
                                Log.i(TAG, "BluetoothAdapter is on.");
                                //蓝牙已打开，开始搜索并连接service
                               // startDiscovery();
                               // getBluetoothA2DP();
                                break;
                            case BluetoothAdapter.STATE_TURNING_OFF:
                                Log.i(TAG, "BluetoothAdapter is turning off.");
                                break;
                            case BluetoothAdapter.STATE_OFF:
                                Log.i(TAG, "BluetoothAdapter is off.");
                                break;
                        }
                        //</editor-fold>
                        break;
                    case Intent.ACTION_HEADSET_PLUG:
                        if (intent.hasExtra("state")) {
                            final int plug_state = intent.getIntExtra("state", 0);
                            if (plug_state == 1) {
                                //Toast.makeText(context, "插入耳机", Toast.LENGTH_SHORT).show();
                            } else if(plug_state == 0){
                                //Toast.makeText(context, "拔出耳机", Toast.LENGTH_SHORT).show();
                            }
                        }
                    default:
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(mBroadcastReceiver, filter);



    }


    private void unRegisterBluetoothReceiver(){
        unregisterReceiver(mBroadcastReceiver);
    }

    /**
     * 耳机是否插入
     * @return 插入耳机返回true,否则返回false
     */
    @SuppressWarnings("deprecation")
    public boolean isWiredHeadsetOn(){
        if(mAudioManager!=null) {
            return mAudioManager.isWiredHeadsetOn();
        }
        return false;
    }
    /**
     * 蓝牙是否链接
     * @return
     */

    public Boolean isBlueToothConnect(){
        return isBlueToothCon;
    }

    public Boolean adjustStreamVolume(int mode,int direction,int flag){
        if(mAudioManager==null)return false;
        if(mAudioManager.getMode()!=mode)return false;
        mAudioManager.adjustStreamVolume(mode, direction==AudioManager.ADJUST_LOWER?AudioManager.ADJUST_LOWER:AudioManager.ADJUST_RAISE, flag);
        return true;
    }

    public Boolean isBlueToothCon(){

        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
//      int isBlueCon;//蓝牙适配器是否存在，即是否发生了错误
        if (ba == null){
//         isBlueCon = -1;     //error
            Log.d("wwc516","isBlueToothCon>BluetoothAdapter is null");
            return false;
        } else if(ba.isEnabled()) {

            int a2dp = ba.getProfileConnectionState(BluetoothProfile.A2DP);              //可操控蓝牙设备，如带播放暂停功能的蓝牙耳机
            int headset = ba.getProfileConnectionState(BluetoothProfile.HEADSET);        //蓝牙头戴式耳机，支持语音输入输出
            int health = ba.getProfileConnectionState(BluetoothProfile.HEALTH);          //蓝牙穿戴式设备

            //查看是否蓝牙是否连接到三种设备的一种，以此来判断是否处于连接状态还是打开并没有连接的状态
            int flag = -1;
            if (a2dp == BluetoothProfile.STATE_CONNECTED) {
                flag = a2dp;
            } else if (headset == BluetoothProfile.STATE_CONNECTED) {
                flag = headset;
            } else if (health == BluetoothProfile.STATE_CONNECTED) {
                flag = health;
            }
            //说明连接上了三种设备的一种
            if (flag != -1) {
//            isBlueCon = 1;            //connected
                Log.d("wwc516516","isBlueToothCon>BluetoothAdapter is isEnabled  flag=="+flag);
                return true;
            }
            return false;
        }
        return false;
    }




    /*

    class HeadsetReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action){
                //插入和拔出耳机会触发此广播
                case Intent.ACTION_HEADSET_PLUG:
                    int state = intent.getIntExtra("state", 0);
                    if (state == 1){
                        playerManager.changeToHeadset();
                    } else if (state == 0){
                        playerManager.changeToSpeaker();
                    }
                    break;
                default:
                    break;
            }
        }
    }

*/

    Boolean mProximitySensorEnabled = false;
    SensorManager mSensorManager ;
    Sensor sensor ;
    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;
    private void initSensorManager(){
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "WWCWakeLock");
    }

    SensorEventListener mSensorEventListener = new SensorEventListener(){
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float value = sensorEvent.values[0];

            if (isWiredHeadsetOn() || AudioPlayManager.SPK_MODE == PLAY_SPK_MODE_BLU){
                return;
            }
            if (mMediaPlayer.isPlaying()){
                if (value >= sensor.getMaximumRange()) {
                    if(AudioPlayManager.SPK_MODE==PLAY_SPK_MODE_SPK && !mAudioManager.isSpeakerphoneOn()){
                        Log.d("Sensor","SensorEventListener->setAudioPlayMode_Speeker");
                        setAudioPlayMode_Speeker();
                    }
                    setScreenOn();
                } else {
                    if(mAudioManager.isSpeakerphoneOn()) {
                        Log.d("Sensor","SensorEventListener->setAudioPlayMode_TelepReceiver");
                        setAudioPlayMode_TelepReceiver(false);
                    }
                    setScreenOff();
                }
            } else {
                if(value == sensor.getMaximumRange()){
                    setAudioPlayMode_Speeker();
                    setScreenOn();
                }
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {}
    };


    private void setProximitySensorEnabled(boolean enable) {

        if (enable) {

            if (!mProximitySensorEnabled) {
                // Register the listener.
                // Proximity sensor state already cleared initially.
                mProximitySensorEnabled = true;
                mSensorManager.registerListener(mSensorEventListener, sensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
        } else {
            if (mProximitySensorEnabled) {
                // Unregister the listener.
                // Clear the proximity sensor state for next time.
                mProximitySensorEnabled = false;
                mSensorManager.unregisterListener(mSensorEventListener);
                setScreenOn();

            }
        }

    }
    private void setScreenOff(){
        if (wakeLock == null){
            wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "WWCWakeLock");
        }
        wakeLock.acquire();
    }

    private void setScreenOn(){
        if (wakeLock != null){
            wakeLock.setReferenceCounted(false);
            wakeLock.release();
            wakeLock = null;
        }
    }

}



/*    1.PARTIAL_WAKE_LOCK : CPU运行,屏幕和键盘可能关闭
2.SCREEN_DIM_WAKE_LOCK :　屏幕亮,键盘灯可能关闭
3.SCREEN_BRIGHT_WAKE_LOCK : 屏幕全亮,键盘灯可能关闭
4.FULL_WAKE_LOCK : 屏幕和键盘灯全亮
5.PROXIMITY_SCREEN_OFF_WAKE_LOCK : 屏幕关闭,键盘灯关闭,CPU运行
6.DOZE_WAKE_LOCK : 屏幕灰显,CPU延缓工作*/

/*    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
    PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, null);*/







