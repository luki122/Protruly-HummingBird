package cn.com.protruly.soundrecorder.managerUtil;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by wenwenchao on 2017/8/17.
 */

public class AudioPlayManager {

    public final static int PLAY_SPK_MODE_SPK = 0;
    public final static int PLAY_SPK_MODE_REC = 1;
    public final static int PLAY_SPK_MODE_BLU = 2;
    public static int SPK_MODE = PLAY_SPK_MODE_SPK;


    private static AudioPlayManager mInstance;
    private Context mContext;
    private Boolean isBindFlag =false;
    private ServiceConnection mConn;
    private AudioPlayManagerService mAudioPlayManagerService;
    private AudioPlayManager(){}
    public static AudioPlayManager getInstance() {
        if (mInstance == null) {
            synchronized (AudioPlayManager.class) {
                if (mInstance == null) {
                    mInstance = new AudioPlayManager();
                }
            }
        }
        return mInstance;
    }
    public void onInitAudioPlayManager(Context context){
        this.mContext = context.getApplicationContext();
        if(!isBindFlag) {
            Log.d("audioplay", "onInitAudioPlayManager!");
            Intent intent = new Intent(mContext, AudioPlayManagerService.class);
            mConn = new Conn();
            mContext.bindService(intent, mConn, Context.BIND_AUTO_CREATE);
            isBindFlag = true;
        }
    }

    class Conn implements ServiceConnection
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioPlayManagerService.AudioPlayBind binder= (AudioPlayManagerService.AudioPlayBind) service;
            Log.d("audioplay", "onServiceConnected!");
            mAudioPlayManagerService=binder.getService();
            if(mAudioPlayManagerService !=null && mplayModeChangedCallBack!=null){
                mAudioPlayManagerService.setPlayModeChangedCallBack(mplayModeChangedCallBack);
                mplayModeChangedCallBack.onModeChanged();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBindFlag = false;
            mConn=null;
        }
    }
    //用于播放暂停一体按钮
    public void onStartPlay(String path, int startplaytime, Handler handler){
        if(mAudioPlayManagerService==null)
        {
            Log.d("audioplay", "mAudioPlayManagerService is not bind!");
            Intent intent=new Intent(mContext,AudioPlayManagerService.class);
            mConn = new Conn();
            mContext.bindService(intent, mConn, Context.BIND_AUTO_CREATE);
            isBindFlag = true;
        }
        else
        {
            mAudioPlayManagerService.onStartPlay( path,  startplaytime,handler);
        }

    }
    //强制播放，用于标记点击播放
    public void SeektoPlayAtPos(String path, long pos, Handler handler){
        if(mAudioPlayManagerService!=null){
            mAudioPlayManagerService.SeektoPlayAtPos(path,pos,handler);
        }
    }
    //用于编辑时试听，主要添加头尾时间点
    public void playForAudioEdit(String path, long satrtPos, long endPos,Handler handler){
        if(mAudioPlayManagerService!=null){
            mAudioPlayManagerService.playForAudioEdit(path, satrtPos,endPos, handler);
        }
    }

    public void onPausePlay(){
        if(mAudioPlayManagerService!=null){
            mAudioPlayManagerService.onPausePlay();
        }

    }
    public void onStopPlay(){
        if(mAudioPlayManagerService!=null){
            mAudioPlayManagerService.onStopPlay();
        }
    }
    public void onDestroyAudioPlayManager(){
        if(isBindFlag && mConn != null){
            mContext.unbindService(mConn);
            isBindFlag = false;
        }
    }

    public long GetNowPosForMark(){
        if(mAudioPlayManagerService!=null){
            return  mAudioPlayManagerService.GetNowPosForMark();
        }
        return -1;
    }

    public int GetCurrentPlayStats(){
        if(mAudioPlayManagerService!=null){
            return  mAudioPlayManagerService.GetCurrentPlayStats();
        }
        return -1;
    }




    //设置音频通道
    public int getAudioPlay_SPKMode(){
        if(mAudioPlayManagerService!=null){
            return   mAudioPlayManagerService.getAudioPlay_SPKMode();
        }
        return -1;
    }
    public Boolean isBlueToothConnect(){
        if(mAudioPlayManagerService!=null){
            return   mAudioPlayManagerService.isBlueToothConnect();
        }
        return false;
    }
    public Boolean isWiredHeadsetOn(){
        if(mAudioPlayManagerService!=null){
            return   mAudioPlayManagerService.isWiredHeadsetOn();
        }
        return false;
    }

    public Boolean setAudioPlayMode_Speeker(){
        if(mAudioPlayManagerService!=null){
            return   mAudioPlayManagerService.setAudioPlayMode_Speeker();
        }
        return false;
    }
    public Boolean setAudioPlayMode_TelepReceiver(Boolean isForce){
        if(mAudioPlayManagerService!=null){
            return  mAudioPlayManagerService.setAudioPlayMode_TelepReceiver(isForce);
        }
        return false;
    }
    public Boolean setAudioPlayMode_BlueTooth(){
        if(mAudioPlayManagerService!=null){
            return  mAudioPlayManagerService.setAudioPlayMode_BlueTooth();
        }
        return false;
    }
    public Boolean adjustAudioStreamVolume(int mode,int direction,int flag){
        if(mAudioPlayManagerService!=null){
         return mAudioPlayManagerService.adjustStreamVolume(mode,direction,flag);
        }
        return false;
    }

    private playModeChangedCallBack mplayModeChangedCallBack;
    public void setPlayModeChangedCallBack(playModeChangedCallBack callBack){
        if(callBack!=null){
            mplayModeChangedCallBack = callBack;
        }
        if(mAudioPlayManagerService!=null){
            mAudioPlayManagerService.setPlayModeChangedCallBack(mplayModeChangedCallBack);
        }
    }
    public interface  playModeChangedCallBack{
        void onModeChanged();
    }



    //以下是播放seekbar滑动事件
    public void onProgressChanged(long progress){
        if(mAudioPlayManagerService!=null){
            mAudioPlayManagerService.onProgressChanged(progress);
        }
    }
    public void onStartTrackingTouch(long progress){
        if(mAudioPlayManagerService!=null){
            mAudioPlayManagerService.onStartTrackingTouch(progress);
        }
    }
    public void onStopTrackingTouch(long progress){
        if(mAudioPlayManagerService!=null){
            mAudioPlayManagerService.onStopTrackingTouch(progress);
        }
    }


}
