package cn.com.protruly.soundrecorder;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.util.Log;

/**
 * Created by liushitao on 17-9-11.
 */

public class ScreenListener {
    private Context mContext;
    private ScreenBroadcastReceiver mScreenReceiver;
    private ScreenStateListener mScreenStateListener;

    public ScreenListener(Context context){
        mContext = context;
        mScreenReceiver = new ScreenBroadcastReceiver();
    }

    private class ScreenBroadcastReceiver extends BroadcastReceiver{

        private String action = null;

        @Override
        public void onReceive(Context context, Intent intent) {
            action = intent.getAction();
            if(null == action) return;
            if(Intent.ACTION_SCREEN_OFF.equals(action)){
                mScreenStateListener.onScreenOff();
            }else if(Intent.ACTION_SCREEN_ON.equals(action)){
                mScreenStateListener.onSCreenOn();
            }else if(Intent.ACTION_USER_PRESENT.equals(action)){
                mScreenStateListener.onUserPresent();
            }
        }
    }

    void beginListen(ScreenStateListener listener){
        mScreenStateListener = listener;
        registerListener();
        getScreenState();
    }

    private void getScreenState(){
        KeyguardManager manager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        if(manager.isKeyguardLocked()){
            if(null != mScreenStateListener){
                Log.d("aa","isKeyguardLocked");
                mScreenStateListener.onScreenOff();
            }
        }else{
            if(null != mScreenStateListener){
                Log.d("aa","isKeyguardUnLocked");
                mScreenStateListener.onUserPresent();
            }
        }
    }

    private void registerListener(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        mContext.registerReceiver(mScreenReceiver,filter);
    }

    public void stopListen(){
        mContext.unregisterReceiver(mScreenReceiver);
    }

    public interface ScreenStateListener{
        void onSCreenOn();
        void onScreenOff();
        void onUserPresent();
    }
}
