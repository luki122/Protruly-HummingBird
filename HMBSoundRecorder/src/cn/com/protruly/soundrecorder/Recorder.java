package cn.com.protruly.soundrecorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
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

import cn.com.protruly.soundrecorder.common.RecordError;
import cn.com.protruly.soundrecorder.common.RecordStatus;
import cn.com.protruly.soundrecorder.managerUtil.AudioRecordManagerService;
import cn.com.protruly.soundrecorder.util.DatabaseUtil;
import cn.com.protruly.soundrecorder.util.GlobalConstant;
import cn.com.protruly.soundrecorder.util.GlobalUtil;

/**
 * Created by liushitao on 17-8-15.
 */

public class Recorder implements MediaRecorder.OnInfoListener,MediaRecorder.OnErrorListener{
    private String TAG = "Recorder";
    private MediaRecorder mMediaRecorder = null;
    private File mRecordFile = null;
    private File mRecordLastFile = null;
    private Context mContext;
    private int recordState = RecordStatus.IDLE;
    private int recordError = RecordError.NO_ERROR;
    private boolean isResume = false;
    private RecordListener mRecorderListener = null;
    private int MAX_RECORD_TIME = 2*60*60*1000;
    private DatabaseUtil databaseUtil;

    public Recorder(Context context, RecordListener recordListener) {
        mContext = context;
        mRecorderListener = recordListener;
        databaseUtil = new DatabaseUtil(mContext);
        prepareRecordFile();
        ErrHandler errHandler = new ErrHandler();
        Thread.currentThread().setUncaughtExceptionHandler(errHandler);
    }

    private void prepareRecordFile(){
        File recordDir = new File(GlobalUtil.getRecordDirPath());
        mRecordFile = new File(recordDir, mContext.getString(R.string.new_record) + ".mp3");
        int dirId = 1;
        while ((null != mRecordFile) && mRecordFile.exists() && !mRecordFile.isDirectory()) {
            mRecordFile = new File(recordDir, mContext.getString(R.string.new_record) + dirId + ".mp3");
            dirId++;
        }
        if(dirId>2) {
            mRecordLastFile = new File(recordDir, mContext.getString(R.string.new_record) + (dirId-2) + ".mp3");
        }else{
            mRecordLastFile = new File(recordDir, mContext.getString(R.string.new_record)  + ".mp3");
        }
        Log.d(TAG, TAG + " prepareRecordFile mRecordFile:" + mRecordFile);
        Log.d(TAG, TAG + " prepareRecordFile mRecordLastFile:" + mRecordLastFile);
    }

    private class ErrHandler implements Thread.UncaughtExceptionHandler{

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Log.d(TAG,"Recorder uncaughtException Thread:"+t.getName()+",Throwable:"+e);
            //deleteExceptionRecordFile();
        }
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        Log.d(TAG,"MediaRecorder.OnInfoListener onInfo what:"+what);
        if(what==MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED){
            stopRecord();
            Toast.makeText(mContext,mContext.getResources().getString(R.string.record_reach_two_hours),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        Log.d(TAG,"MediaRecorder.OnErrorListener  onError what:"+what+",extra:"+extra);
    }

    public interface RecordListener {
        void onStateChanged(int stateCode);
        void onError(int errorCode);
    }

    public int getRecordState() {
        return recordState;
    }

    public int getRecordError() {
        return recordError;
    }

    public File getRecordFile() {
        Log.d(TAG, TAG + " getRecordFile");
        return mRecordFile;
    }

    public File getRecordLastFile() {
        Log.d(TAG, TAG + " getRecordLastFile");
        return mRecordLastFile;
    }

    public String getRecordFileName() {
        if (null == mRecordFile) {
            return "";
        }
        String path = mRecordFile.getPath();
        if (TextUtils.isEmpty(path)) {
            return "";
        }
        return path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
    }

    public boolean isProcessResume() {
        return isResume;
    }

    public int getMaxAmplitude() {
        if (null == mMediaRecorder) {
            return 1;
        }
        return mMediaRecorder.getMaxAmplitude();
    }

    private MediaRecorder getMediaRecorderInstance(){
        if (mMediaRecorder==null) {
            mMediaRecorder=new MediaRecorder();
        }
        return mMediaRecorder;
    }

    private void deleteExceptionRecordFile(){
        Log.d(TAG,"deleteExceptionRecordFile mRecordFile:"+mRecordFile);
        Log.d(TAG,"deleteExceptionRecordFile mRecordFile.exists():"+mRecordFile.exists());
        Log.d(TAG,"deleteExceptionRecordFile databaseUtil.isHasThisFileInDB(mRecordFile.getPath())):"+(databaseUtil.isHasThisFileInDB(mRecordFile.getPath())));
        if(null != mRecordFile &&
                mRecordFile.exists()&&
                !databaseUtil.isHasThisFileInDB(mRecordFile.getPath())){
            Log.d(TAG, "aaaaaaaaaaaaaaaaaa");
            mRecordFile.delete();
            mRecordFile = null;
        }
    }

    public void startRecord(int outputfileformat, Context context) {
        isResume = false;
        Log.d(TAG, "startRecord:" + getRecordState());
        if (getRecordState() == RecordStatus.RECORD_PAUSE) {
            resumeRecord();
            return;
        }
        stopRecord();
        prepareRecordFile();
        Log.d(TAG, TAG + " startRecording");
        mMediaRecorder = getMediaRecorderInstance();
        mMediaRecorder.setOnInfoListener(this);
        mMediaRecorder.setOnErrorListener(this);
        mMediaRecorder.reset();
        //mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);//攝像頭旁邊的麥克風
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);////机底下的主麦克风
        mMediaRecorder.setOutputFormat(outputfileformat);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setOutputFile(mRecordFile.getAbsolutePath());
        mMediaRecorder.setAudioSamplingRate(44100);
        mMediaRecorder.setAudioChannels(2);
        mMediaRecorder.setAudioEncodingBitRate(16);
        mMediaRecorder.setMaxDuration(MAX_RECORD_TIME);

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.d(TAG, TAG + "mMediaRecorder.prepare() IOException");
            deleteExceptionRecordFile();
            setError(RecordError.INTERNAL_ERROR);
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            e.printStackTrace();
            return;
        }

        try {
            mMediaRecorder.start();
        } catch (RuntimeException exception) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            boolean isInCall = (audioManager.getMode() == AudioManager.MODE_IN_CALL) ||
                    (audioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION);
            if (isInCall) {
                Log.d(TAG, TAG + " isInCall");
                deleteExceptionRecordFile();
                setError(RecordError.IN_CALL_RESORD_ERROR);
            } else {
                Log.d(TAG, TAG + "mMediaRecorder.start() IOException");
                deleteExceptionRecordFile();
                setError(RecordError.INTERNAL_ERROR);
            }
            Log.d(TAG,"exception:"+exception);
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            return;
        }
        setState(RecordStatus.RECORDING);
    }

    public void pauseRecord() {
        Log.d(TAG, TAG + " pauseRecord mMediaRecorder " + mMediaRecorder);
        mMediaRecorder = getMediaRecorderInstance();
        try {
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
                deleteExceptionRecordFile();
                Log.d(TAG, TAG + "pauseRecord ClassNotFoundException  "+e);
            } catch (NoSuchMethodException e) {
                deleteExceptionRecordFile();
                Log.d(TAG, TAG + "pauseRecord NoSuchMethodException  "+e);
                e.fillInStackTrace();
            } catch (InvocationTargetException e) {
                deleteExceptionRecordFile();
                Log.d(TAG, TAG + "pauseRecord InvocationTargetException "+e);
                e.fillInStackTrace();
            } catch (IllegalAccessException e) {
                deleteExceptionRecordFile();
                Log.d(TAG, TAG + "pauseRecord IllegalAccessException "+e);
                e.fillInStackTrace();
            }
        } catch (RuntimeException exception) {
            deleteExceptionRecordFile();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            Log.d(TAG, TAG + " pauseRecord RuntimeException " + exception);
        }
        setState(RecordStatus.RECORD_PAUSE);
    }

    public void resumeRecord() {
        isResume = true;
        Log.d(TAG, TAG + " resumeRecord");
        mMediaRecorder = getMediaRecorderInstance();
        try {
            //SDK>=24 HAS PAUSE AND RESUME ,SDK<24 HAS JNI CALL
            try {
                Class cls = Class.forName("android.media.MediaRecorder");
                Method pauseMethod = cls.getDeclaredMethod("resume");
                if (pauseMethod != null) {
                    pauseMethod.setAccessible(true);
                }
                pauseMethod.invoke(mMediaRecorder);
            } catch (ClassNotFoundException e) {
                deleteExceptionRecordFile();
                e.fillInStackTrace();
                Log.d(TAG, TAG + "resumeRecord ClassNotFoundException "+e);
            } catch (NoSuchMethodException e) {
                deleteExceptionRecordFile();
                Log.d(TAG, TAG + " resumeRecord NoSuchMethodException:"+e);
                e.fillInStackTrace();
            } catch (InvocationTargetException e) {
                deleteExceptionRecordFile();
                Log.d(TAG, TAG + " resumeRecord InvocationTargetException:"+e);
                e.fillInStackTrace();
            } catch (IllegalAccessException e) {
                deleteExceptionRecordFile();
                Log.d(TAG, TAG + " resumeRecord IllegalAccessException:"+e);
                e.fillInStackTrace();
            }
        } catch (RuntimeException exception) {
            deleteExceptionRecordFile();
            Log.d(TAG, TAG + "resumeRecord RuntimeException "+exception);
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        setState(RecordStatus.RECORD_RESUME);
    }


    public void stopRecord() {
        Log.d(TAG, TAG + " stopRecording");
        if (null == mMediaRecorder) {
            return;
        }
        releaseTimer();
        mMediaRecorder.setOnInfoListener(null);
        mMediaRecorder.setOnErrorListener(null);
        try {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }catch(RuntimeException excption){
            deleteExceptionRecordFile();
            Log.d(TAG, TAG + "stopRecord RuntimeException "+excption);
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        mWaveFrameList.clear();
        setState(RecordStatus.RECORD_FINISH);
    }

    public void cancelRecord() {
        Log.d(TAG, TAG + " cancelRecord");
        if (null == mMediaRecorder) {
            return;
        }
        releaseTimer();
        try {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }catch(RuntimeException excption){
            deleteExceptionRecordFile();
            Log.d(TAG, TAG + "cancelRecord RuntimeException "+excption);
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        deleteExceptionRecordFile();
        mWaveFrameList.clear();
        setState(RecordStatus.RECORD_CANCEL);
    }

    private void setState(int state) {
        Log.d(TAG, TAG + " setState " + state);
        recordState = state;
        getTimeCount();
        mRecorderListener.onStateChanged(state);
    }

    private void setError(int error) {
        Log.d(TAG, TAG + " setError " + error);
        recordError = error;
        mRecorderListener.onError(error);
    }

    //add  by wenwenchao
    private Timer mRecordTimer;
    private Handler mRecordHandler;
    private List<FrameInfo> mWaveFrameList;
    private int   mWaveWidth;
    private long mLastStartTime;
    private long mRecordTimeLong;
   // private long mRecordTimeCount;

    public void setHandler(Handler mHandler){
        mRecordHandler = mHandler;
    }

    public List<FrameInfo> getWaveFrameList() {
        return mWaveFrameList;
    }


    private void getTimeCount(){
        if(recordState == RecordStatus.RECORDING){
            mLastStartTime=System.currentTimeMillis();
            mRecordTimeLong=0;
            //mRecordTimeCount=0;
            TimerGoing();
        }else if(recordState == RecordStatus.RECORD_RESUME){
            mLastStartTime=System.currentTimeMillis();
        }else if(recordState == RecordStatus.RECORD_PAUSE){
            mRecordTimeLong = System.currentTimeMillis()-mLastStartTime+mRecordTimeLong;
        }else{
            releaseTimer();
        }

    }


    private void TimerGoing() {

       // if (recordState == RecordStatus.RECORDING) {
            if (mRecordTimer == null) mRecordTimer = new Timer();
            if (mWaveFrameList == null) {
                mWaveFrameList = new ArrayList<>();
            } else {
                mWaveFrameList.clear();
            }
            initScreenWindow();
      //  }
  //      if (stats == RecordStatus.RECORDING || stats == RecordStatus.RECORD_RESUME || stats == RecordStatus.RECORD_PAUSE){
            mRecordTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(recordState == RecordStatus.RECORDING || recordState == RecordStatus.RECORD_RESUME) {
                        loadDataList();
                    }
                    if (mRecordHandler != null) {
                        Message message = mRecordHandler.obtainMessage();
                        if (message == null) {
                            message = new Message();
                        }
                        message.what = recordState;
                        if (mRecordFile !=null &&(recordState == RecordStatus.RECORDING || recordState == RecordStatus.RECORD_RESUME)) {
                            loadDataList();
                            message.obj = new WaveListInfo(mRecordFile.getName(),/*mWaveFrameList,*/ System.currentTimeMillis()-mLastStartTime+mRecordTimeLong);
                        }else if(mRecordFile !=null ){
                            message.obj = new WaveListInfo(mRecordFile.getName(),/*mWaveFrameList,*/ mRecordTimeLong);
                        }
                        mRecordHandler.sendMessage(message);
                    }
                }
            }, 0, 100);
   //     }

    }

    private void releaseTimer(){
        if (mRecordTimer!=null){
            mRecordTimer.cancel();
            mRecordTimer=null;
            //sendFinishRecordMessage();
        }
    }

    /*private void sendFinishRecordMessage(){
        Message message = mRecordHandler.obtainMessage();
        if (message == null) {
            message = new Message();
        }
        message.what = RecordStatus.RECORD_FINISH;
        mRecordHandler.sendMessage(message);
    }*/


    public void markTheTime(Boolean markable){
        markFlag = markable;
    }

    Boolean markFlag = false;
    int lastRatio = 0;
    private void loadDataList(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mMediaRecorder!=null && (recordState == RecordStatus.RECORDING || recordState == RecordStatus.RECORD_RESUME)) {
                    int MaxAmplitude = mMediaRecorder.getMaxAmplitude();
                    if(MaxAmplitude==0)MaxAmplitude=lastRatio;
                    int ratio = GlobalConstant.WaveVerRate * MaxAmplitude / 32767;
                    if (ratio < 0) ratio = 0;
                    ratio = (ratio + lastRatio) / 2;
                    lastRatio = ratio;
                    FrameInfo frameInfo;
                    if(markFlag){
                        frameInfo = new FrameInfo(ratio, System.currentTimeMillis() - mLastStartTime + mRecordTimeLong, true);
                        markFlag = false;
                    }else{
                        frameInfo = new FrameInfo(ratio, System.currentTimeMillis() - mLastStartTime + mRecordTimeLong, false);
                    }
                    synchronized (Recorder.class) {
                        //test synchronized function code
                        //Log.d("loop","thread before:"+Thread.currentThread().getName());
                        mWaveFrameList.add(frameInfo);
                        //Log.d("loop","thread after:"+Thread.currentThread().getName());
                    }
                    // Log.d("MediaRecordManagerService","lastRatio="+lastRatio+"    mWaveMaxWidth="+mWaveMaxWidth+"    mWaveMaxHight="+mWaveMaxHight);
                    if (mWaveFrameList.size() > mWaveWidth / GlobalConstant.WaveHorRate) {
                        mWaveFrameList.remove(0);
                    }
                }
            }
        }).start();
    }



    public static class WaveListInfo{
       // public List<FrameInfo> wavelist;
        public long  timeCount = -1;
        public String recordName;
        public WaveListInfo(String recordName,/* List<FrameInfo> list,*/ long timecount){
            this.recordName = recordName;
            this.timeCount = timecount;
           // this.wavelist  = list;
        }
    }


    public static class FrameInfo{
        public int amplitude = -1;
       // public long currentTime = -1;
        public Boolean isMark = false;
        public FrameInfo(int amplitude,long currentTime,Boolean isMark){
            this.amplitude=amplitude;
            //this.currentTime=currentTime;
            this.isMark = isMark;
        }
    }


    private void initScreenWindow(){
        WindowManager mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(dm);
        mWaveWidth = dm.widthPixels;
        // mScreenHeight = dm.heightPixels;
    }





}
