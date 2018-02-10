package cn.com.protruly.soundrecorder.managerUtil;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.icu.text.LocaleDisplayNames;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import cn.com.protruly.soundrecorder.mp3record.AudioWaveView;

/**
 * Created by wenwenchao on 2017/8/17.
 */

public class AudioRecordManager {

    private static AudioRecordManager mInstance;
    private Context mContext;
    private Boolean isBindFlag = false;
    private ServiceConnection mConn;
    private AudioRecordManagerService mAudioRecordManagerService;
    private AudioRecordManager(){}
    public  Handler mHandler;
    public static AudioRecordManager getInstance() {
        if (mInstance == null) {
            synchronized (AudioRecordManager.class) {
                if (mInstance == null) {
                    mInstance = new AudioRecordManager();
                }
            }
        }
        return mInstance;
    }

    public void onInitAudioRecordManager(Context context){
        if(!isBindFlag) {
            this.mContext = context.getApplicationContext();
            Intent intent = new Intent(mContext, AudioRecordManagerService.class);
            mConn = new Conn();
            mContext.bindService(intent, mConn, Context.BIND_AUTO_CREATE);
            isBindFlag = true;
        }
    }


    class Conn implements ServiceConnection
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioRecordManagerService.AudioRecordBind binder= (AudioRecordManagerService.AudioRecordBind) service;
            mAudioRecordManagerService=binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBindFlag = false;
            mConn=null;
        }
    }


    public void onStartRecord(Handler handler){
        if(mAudioRecordManagerService==null){
            Log.d("wenwenchao","service is not binded,please： 1、check the service permission in manifest, 2、invoke the methord --- onInitAudioRecordManager(Context)");
        }else {
            mAudioRecordManagerService.onStartRecord(handler);
            this.mHandler = handler;
        }
    }

    public void onStopRecord(){
        if(mAudioRecordManagerService!=null) {
            mAudioRecordManagerService.onStopRecord();
        }
    }
    public int getRecordStatus(){
        if(mAudioRecordManagerService!=null) {
            return  mAudioRecordManagerService.getRecordStatus();
        }
        return -1;
    }


    public List<Integer> getWaveAmplitudeList(){
        if(mAudioRecordManagerService!=null) {
            return  mAudioRecordManagerService.getWaveAmplitudeList();
        }
        return null;
    }


    public void markTheTime(){
        if(mAudioRecordManagerService!=null) {
            mAudioRecordManagerService.markTheTime();
        }
    }




    public void onDestroyAudioRecordManager(){
        if(isBindFlag && mConn != null){
            mContext.unbindService(mConn);
            isBindFlag = false;
        }
    }





}
