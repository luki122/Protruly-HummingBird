package cn.com.protruly.soundrecorder.managerUtil;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cn.com.protruly.soundrecorder.MarkManager;
import cn.com.protruly.soundrecorder.lockscreen.LockScreenActivity;
import cn.com.protruly.soundrecorder.mp3record.AudioWaveView;
import cn.com.protruly.soundrecorder.mp3record.MP3Recorder;
import cn.com.protruly.soundrecorder.util.DatabaseUtil;
import cn.com.protruly.soundrecorder.util.GlobalConstant;
import cn.com.protruly.soundrecorder.util.GlobalUtil;

import cn.com.protruly.soundrecorder.R;

/**
 * Created by wenwenchao on 2017/8/17.
 */


public class AudioRecordManagerService extends Service {
/*    private MP3Recorder mMP3Recorder;
    private AudioWaveView mAudioWaveView;
    private String srcFilePath;
    private Handler.Callback   mHandlerCallBack;
    boolean mIsRecord = false;


    public AudioRecordManagerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return new AudioRecordManagerService.AudioRecordBind();
    }
    class AudioRecordBind extends Binder
    {
        public AudioRecordManagerService getService()
        {
            return AudioRecordManagerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initAudioRecord();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    void initAudioRecord(){
    }

    public void onStartRecord(String audioDedualtName, String parentPath, AudioWaveView awv, Handler.Callback callback){
        if(parentPath==null)return;
        if(mIsRecord) return;
        File file = new File(parentPath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Toast.makeText(this, "创建文件失败", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        String filePath = parentPath+File.separator+audioDedualtName;
        mMP3Recorder = new MP3Recorder(new File(filePath));
        mAudioWaveView = awv;
        int size = awv.getWidth() / dip2px(this, 1);//控件默认的间隔是1
        mMP3Recorder.setDataList(mAudioWaveView.getRecList(), size);
        mMP3Recorder.setErrorHandler(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == MP3Recorder.ERROR_TYPE) {
                    Toast.makeText(getApplication(), "没有麦克风权限", Toast.LENGTH_SHORT).show();
                    recordError();
                }
            }
        });


        try {
            mMP3Recorder.start();
            mAudioWaveView.startView();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplication(), "录音出现异常", Toast.LENGTH_SHORT).show();
            recordError();
            return;
        }
        if(callback!=null){
            mHandlerCallBack = callback;
            handleMsgForUI(mHandlerCallBack, GlobalConstant.RECORD_START,null);
        }
        timing();
        srcFilePath = filePath;
        mIsRecord = true;
    }
    public void onPauseRecord(){
        if (!mIsRecord)
            return;

        if (mMP3Recorder.isPause()) {
            mMP3Recorder.setPause(false);
            timing();
            handleMsgForUI(mHandlerCallBack,GlobalConstant.RECORD_START,null);
        } else {
            mMP3Recorder.setPause(true);
            timepause();
            handleMsgForUI(mHandlerCallBack,GlobalConstant.RECORD_PAUSE,null);

        }
    }
    public void onStopRecord(){
        if (mMP3Recorder != null && mMP3Recorder.isRecording()) {
            mMP3Recorder.setPause(false);
            mMP3Recorder.stop();
            mAudioWaveView.stopView();
        }
        handleMsgForUI(mHandlerCallBack,GlobalConstant.RECORD_STOP,null);
        recordTimeCount=0;
        timepause();
        mIsRecord = false;
    }
    public void onDestroyRecord(){
        if(mMP3Recorder==null)return;
        if (mMP3Recorder.isPause() || mMP3Recorder.isRecording()){
            mMP3Recorder.stop();
            timepause();
        }
        recordTimeCount=0;
        mIsRecord = false;
    }

    private void recordError() {
        if(srcFilePath!=null){
            deleteRecordFile(srcFilePath);
        }
        if (mMP3Recorder != null && mMP3Recorder.isRecording()) {
            mMP3Recorder.stop();
            mAudioWaveView.stopView();
        }
        timepause();
        recordTimeCount=0;
        mIsRecord = false;
        handleMsgForUI(mHandlerCallBack,GlobalConstant.RECORD_ERROR,null);

    }

    public void deleteRecordFile(String filePath) {
        File file = new File(filePath);
        try {
            if (file.exists()) {
                if (file.isFile()) {
                    file.delete();
                } else {
                    String[] filePaths = file.list();
                    for (String path : filePaths) {
                        deleteFile(filePath + File.separator + path);
                    }
                    file.delete();
                }
            }
        }catch (Exception e){
            Log.d("wenwenchao","文件删除异常");
        }
    }

    Runnable runnable;
    private Handler handler = new Handler();
    long recordTimeCount;


    private void timing() {
        runnable = new Runnable() {
            @Override
            public void run() {
                recordTimeCount += 200;
                if (recordTimeCount > GlobalConstant.RECORD_MAXTIME) {
                    onStopRecord();
                } else {
                    handleMsgForUI(mHandlerCallBack,GlobalConstant.RECORD_START,recordTimeCount);
                    // timeText.setText(GlobalConstant.toTime(voiceLength));
                    handler.postDelayed(this, 200);
                }
            }
        };
        handler.postDelayed(runnable, 200);
    }

    *//**
     * 暂停录音
     *//*
    public void timepause(){
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
            runnable = null;
        }
    }

    public void handleMsgForUI(Handler.Callback mcallback,int msgwhat,Object obj){
        if(mcallback!=null){
            Message msg = new Message();
            msg.what =msgwhat;
            msg.obj = obj;
            mcallback.handleMessage(msg);
        }

    }


    public  int dip2px(Context context, float dipValue) {
        float fontScale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * fontScale + 0.5f);
    }*/













    private final String TAG = "MediaRecordManagerService";
    private MediaRecorder mMediaRecorder;
    //private RecordWaveView mSurfaceView;
    private   List<Integer> mWaveDataList;
    private   List<FrameInfo> mWaveInfoList;
    private Paint mPaint;
    private File RecordsDir;
    private File mCurrentRecordFile = null;
    private List<Long>  markList;
    private Timer UIupdateTimer;
    private long mCurrentRecordFileTimeLong = 0;
    private long mLastStartTime=0;
    private int mRecordStatus = -1;
    private final int RECORDSTATUS_GOING = GlobalConstant.RECORD_START;
    private final int RECORDSTATUS_PAUSE = GlobalConstant.RECORD_PAUSE;
    private final int RECORDSTATUS_STOP = GlobalConstant.RECORD_STOP;
    Handler mHandler;
    private String DEFUALT_NAME;
    private MarkManager mMarkManager;
    private DatabaseUtil databaseUtil;
    private int mScreenWidth;
    private int mScreenHeight;

    @Override
    public IBinder onBind(Intent intent) {
        return new AudioRecordManagerService.AudioRecordBind();
    }

    class AudioRecordBind extends Binder {
        public AudioRecordManagerService getService() {
            return AudioRecordManagerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerSCREENReceiver();
        initMediaRecord();
        initScreenWindow();
    }


    @Override
    public void onDestroy() {
        clearAndResetAll();
        unregisterSCREENReceiver();
        super.onDestroy();
        Log.e(TAG, "onDestroy");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        clearAndResetAll();
        unregisterSCREENReceiver();
        Log.e(TAG, "onUnbind");
        return super.onUnbind(intent);
    }


    private void initMediaRecord() {
        if(mMediaRecorder==null)mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        if(RecordsDir==null)RecordsDir = new File(GlobalUtil.getRecordDirPath());
        if(markList==null)markList = new ArrayList<>();
        if(UIupdateTimer==null)UIupdateTimer = new Timer();
        if(mMarkManager==null)mMarkManager =  new MarkManager(this);
        if(databaseUtil==null)databaseUtil = new DatabaseUtil(this);
        DEFUALT_NAME = getString(R.string.new_record);

        if(mWaveDataList==null)mWaveDataList = new ArrayList<>();
    }

    private void clearAndResetAll(){
        Log.d("wwcwwcwwc","clearAndResetAll MediaRecordManagerService");
        if(mMediaRecorder!=null){
            if(mRecordStatus==RECORDSTATUS_GOING
                    ||mRecordStatus==RECORDSTATUS_PAUSE){
                mMediaRecorder.stop();
            }
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder=null;
        }
        if(UIupdateTimer!=null){
            UIupdateTimer.cancel();
            UIupdateTimer = null;
        }
        if(markList!=null){
            markList.clear();
            markList=null;
        }
        //clearSurfaceView();
        mCurrentRecordFileTimeLong = 0;
        mLastStartTime=0;
        mRecordStatus = -1;
    }

    public void onStartRecord(Handler handler) {
        //if (parentPath == null) return;
        Log.d("wwc516","onStartRecord----");
        if (mMediaRecorder == null) {
            initMediaRecord();
        }


        if(handler!=null && handler!=mHandler){
            mHandler = handler;
        }

        if (mRecordStatus == RECORDSTATUS_GOING) {
            // mMediaRecorder.pause();
            Toast.makeText(this,"pauseRecord",Toast.LENGTH_SHORT);
            pauseRecord();
            mCurrentRecordFileTimeLong += (System.currentTimeMillis() - mLastStartTime);
            mRecordStatus = RECORDSTATUS_PAUSE;
            return;
        }

        if (mRecordStatus == RECORDSTATUS_PAUSE) {
            resumeRecord();
            mLastStartTime = System.currentTimeMillis();
            mRecordStatus = RECORDSTATUS_GOING;
            return;
        }



        mCurrentRecordFile = new File(RecordsDir, DEFUALT_NAME+".mp3");
        int dirId = 1;
        while ((null != mCurrentRecordFile) && mCurrentRecordFile.exists() && mCurrentRecordFile.isFile()) {
            mCurrentRecordFile = new File(RecordsDir, DEFUALT_NAME + dirId + ".mp3");
            dirId++;
        }

        mMediaRecorder.setOutputFile(mCurrentRecordFile.getAbsolutePath());

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.d(TAG,"MediaRecorder>prepare>error!------");
            e.printStackTrace();
            clearAndResetAll();
            return;
        }

        try {
            mMediaRecorder.start();
            mRecordStatus = RECORDSTATUS_GOING;
            mLastStartTime = System.currentTimeMillis();
            mCurrentRecordFileTimeLong=0;
            markList.clear();
            timerGoing();
        } catch (RuntimeException e) {
            Log.d(TAG,"MediaRecorder>start>error!------");
            e.printStackTrace();
            clearAndResetAll();
            return;
          /*  AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            boolean isInCall = (audioManager.getMode() == AudioManager.MODE_IN_CALL) ||
                    (audioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION);
            if (isInCall) {

            } else {

            }*/
        }
    }


    public void onStopRecord(){
        if (null == mMediaRecorder) {
            return;
        }
        if(mRecordStatus==RECORDSTATUS_GOING||mRecordStatus==RECORDSTATUS_PAUSE){

            mMediaRecorder.stop();
            mRecordStatus = RECORDSTATUS_STOP;
            mCurrentRecordFileTimeLong += (System.currentTimeMillis() - mLastStartTime);
            databaseUtil.insertRecordFile(mCurrentRecordFile.getPath());

            clearAndResetAll();

        }else{
            Toast.makeText(this,"保存录音异常",Toast.LENGTH_SHORT).show();
            clearAndResetAll();
        }
    }



    public void markTheTime(){
        if (null == mMediaRecorder) {
            return;
        }
        if(mRecordStatus==RECORDSTATUS_GOING){
            markList.add(System.currentTimeMillis()-mLastStartTime+mCurrentRecordFileTimeLong);
        }else if(mRecordStatus==RECORDSTATUS_PAUSE){
            markList.add(mCurrentRecordFileTimeLong);
        }

    }



    private void pauseRecord() {
        //SDK>=24 HAS PAUSE AND RESUME ,SDK<24 HAS JNI CALL
        try {
            Class cls = Class.forName("android.media.MediaRecorder");
            Method pauseMethod = cls.getDeclaredMethod("pause");
            if (pauseMethod != null) {
                pauseMethod.setAccessible(true);
            }
            pauseMethod.invoke(mMediaRecorder);
        } catch (ClassNotFoundException e) {
            e.fillInStackTrace();
            Log.d(TAG, TAG + " e");
        } catch (NoSuchMethodException e) {
            Log.d(TAG, TAG + " e");
            e.fillInStackTrace();
        } catch (InvocationTargetException e) {
            Log.d(TAG, TAG + " e");
            e.fillInStackTrace();
        } catch (IllegalAccessException e) {
            Log.d(TAG, TAG + " e");
            e.fillInStackTrace();
        }
    }

    public void resumeRecord(){
        //SDK>=24 HAS PAUSE AND RESUME ,SDK<24 HAS JNI CALL
        try {
            Class cls = Class.forName("android.media.MediaRecorder");
            Method pauseMethod = cls.getDeclaredMethod("resume");
            if (pauseMethod != null) {
                pauseMethod.setAccessible(true);
            }
            pauseMethod.invoke(mMediaRecorder);
        }catch(ClassNotFoundException e){
            e.fillInStackTrace();
            Log.d(TAG,TAG+" e");
        }catch (NoSuchMethodException e){
            Log.d(TAG,TAG+" e");
            e.fillInStackTrace();
        }catch(InvocationTargetException e){
            Log.d(TAG,TAG+" e");
            e.fillInStackTrace();
        }catch(IllegalAccessException e){
            Log.d(TAG,TAG+" e");
            e.fillInStackTrace();
        }
    }


    public int getRecordStatus(){
        if (null == mMediaRecorder) {
            return -1;
        }
        return mRecordStatus;
    }


    public List<Integer>  getWaveAmplitudeList(){
        return mWaveDataList;
    }


    int lastRatio = 0;
    private void dealWaveData(){
        int ratio = 200*mMediaRecorder.getMaxAmplitude()/32767;
        if(ratio<0)ratio=0;
        int temp = ratio;
        ratio = (ratio+lastRatio)/2;
        lastRatio = temp;
        mWaveDataList.add(ratio);
       // Log.d("MediaRecordManagerService","lastRatio="+lastRatio+"    mWaveMaxWidth="+mWaveMaxWidth+"    mWaveMaxHight="+mWaveMaxHight);
        if(mWaveDataList.size()>mScreenWidth/5){
            mWaveDataList.remove(0);
        }

    }

    private void dealWaveData2(){
        int ratio = 200*mMediaRecorder.getMaxAmplitude()/32767;
        if(ratio<0)ratio=0;
        int temp = ratio;
        ratio = (ratio+lastRatio)/2;
        lastRatio = temp;
        FrameInfo frameInfo = new FrameInfo(ratio,System.currentTimeMillis()-mLastStartTime+mCurrentRecordFileTimeLong,false);
        mWaveInfoList.add(frameInfo);
        // Log.d("MediaRecordManagerService","lastRatio="+lastRatio+"    mWaveMaxWidth="+mWaveMaxWidth+"    mWaveMaxHight="+mWaveMaxHight);
        if(mWaveInfoList.size()>mScreenWidth/5){
            mWaveInfoList.remove(0);
        }

    }



    private void timerGoing(){
        final String filename = mCurrentRecordFile.getName();
        UIupdateTimer.schedule(new TimerTask() {
            @Override
            public void run() {

                if(mHandler!=null){
                    Message message = mHandler.obtainMessage();
                    if(message==null){
                        message = new Message();
                    }
                    message.what = mRecordStatus;
                    int ratio = 0;
                    if(mRecordStatus==RECORDSTATUS_GOING && mMediaRecorder!=null){
                        dealWaveData();
                    }
                    if(mRecordStatus==RECORDSTATUS_GOING)message.obj = new RecordMessageInfo(filename,System.currentTimeMillis()-mLastStartTime+mCurrentRecordFileTimeLong);
                    if(mRecordStatus==RECORDSTATUS_PAUSE
                            || mRecordStatus==RECORDSTATUS_STOP)message.obj = new RecordMessageInfo(filename,mCurrentRecordFileTimeLong);
                    mHandler.sendMessage(message);
                }
            }
        },0,100);
    }


    public static class RecordMessageInfo{
        public String name;
        public long   timeCount;

        public RecordMessageInfo(String name,long  timeCount){
            this.name= name;
            this.timeCount= timeCount;
        }
    }

/*
    private Runnable runnable;
    private Handler handler = new Handler();
    private long recordTimeCount;

    private void timing() {
        runnable = new Runnable() {
            @Override
            public void run() {
                recordTimeCount += 200;
                if (recordTimeCount > GlobalConstant.RECORD_MAXTIME) {
                    onStopRecord();
                } else {
                    handleMsgForUI(mHandlerCallBack,GlobalConstant.RECORD_START,recordTimeCount);
                    // timeText.setText(GlobalConstant.toTime(voiceLength));
                    handler.postDelayed(this, 200);
                }
            }
        };
        handler.postDelayed(runnable, 200);
    }*/


    private BroadcastReceiver mScreenActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action==null)return;
            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                Log.e(TAG, "screen on");
                KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                if (km.inKeyguardRestrictedInputMode() && mRecordStatus==RECORDSTATUS_GOING) {
                    // 处于锁屏状态
                    Log.e(TAG, "screen inlock!");
                    Intent LockIntent = new Intent(context,LockScreenActivity.class);/*LockScreenActivity*/
                    LockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    //LockIntent.putExtra("start_locksceen",true);
                    startActivity(LockIntent);
                    //initLockScreenWindow();
                }

            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                Log.e(TAG, "screen off");
            }else if(action.equals(Intent.ACTION_USER_PRESENT)){
                Log.e(TAG, "screen unlock");
            }
        }
    };

    private void registerSCREENReceiver(){
        IntentFilter mScreenOnFilter = new IntentFilter();
        mScreenOnFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenOnFilter.addAction(Intent.ACTION_SCREEN_ON);
        mScreenOnFilter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(mScreenActionReceiver, mScreenOnFilter);
    }

    private void unregisterSCREENReceiver(){
        unregisterReceiver(mScreenActionReceiver);
    }


    private void initScreenWindow(){
        WindowManager mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(dm);
        mScreenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;
    }






    public static class WaveListInfo{
        private List<FrameInfo> wavelist;
        private long  timeCount;
        private String recordName;
        public WaveListInfo(String recordName,List<FrameInfo> list,long timecount){
            this.recordName = recordName;
            this.timeCount = timecount;
            this.wavelist  = list;
        }
    }


    public static class FrameInfo{
        private int amplitude;
        private long currentTime;
        private Boolean isMark;
        public FrameInfo(int amplitude,long currentTime,Boolean isMark){
            this.amplitude=amplitude;
            this.currentTime=currentTime;
            this.isMark = isMark;
        }
    }

/*
    private void initLockScreenWindow() {
        WindowManager mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
// 更新浮动窗口位置参数 靠边
        DisplayMetrics dm = new DisplayMetrics();
// 获取屏幕信息
        mWindowManager.getDefaultDisplay().getMetrics(dm);
        int mScreenWidth = dm.widthPixels;
        int mScreenHeight = dm.heightPixels;
        WindowManager.LayoutParams mWmParams = new WindowManager.LayoutParams();
// 设置window type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        } else {
            mWmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
// 设置图片格式，效果为背景透明
        mWmParams.format = PixelFormat.RGBA_8888;
// 设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
        mWmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
// 调整悬浮窗显示的停靠位置为左侧置�?
        mWmParams.gravity = Gravity.LEFT | Gravity.TOP;
        mScreenHeight = mWindowManager.getDefaultDisplay().getHeight();
// 以屏幕左上角为原点，设置x、y初始值，相对于gravity
        mWmParams.x = 0;
        mWmParams.y = mScreenHeight / 2;
// 设置悬浮窗口长宽数据
        mWmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWmParams.height = 400;//WindowManager.LayoutParams.WRAP_CONTENT;
        View view = LayoutInflater.from(this).inflate(R.layout.activity_media_record_test,null);
        mWindowManager.addView(view, mWmParams);
    }

*/






































}
