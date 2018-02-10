package cn.com.protruly.soundrecorder;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Method;

import java.io.File;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.List;
import java.util.Timer;

import cn.com.protruly.soundrecorder.common.RecordStatus;
import cn.com.protruly.soundrecorder.lockscreen.LockScreenActivity;
import cn.com.protruly.soundrecorder.util.GlobalUtil;
import cn.com.protruly.soundrecorder.util.LogUtil;

/**
 * Created by liushitao on 17-9-1.
 */

public class RecordService extends Service implements ScreenListener.ScreenStateListener{

    private static final String TAG = "RecordService";
    private RecordBinder mRecordBinder;
    private Context mContext;
    private Recorder mRecorder = null;
    private ScreenListener screenListener = new ScreenListener(this);
    private boolean isFloatWindowShow = false;
    private WindowManager wm;
    private WindowManager.LayoutParams params;
    private TextView float_window_show;
    private String mTimeStr;
    private boolean mIsShowWave = true;
    private NotificationManager manager;
    private Notification notification;
    private int RECORD_NOTIFICATION_ID = 100;
    private boolean isShowLockSCreenActivity = false;
    private AudioManager mAudioManager;
    private float mPosX;
    private float mPosY;
    private float mCurPosX;
    private float mCurPosY;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mRecordBinder = new RecordBinder();
        screenListener.beginListen(this);
        wm = (WindowManager) mContext.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        params = new WindowManager.LayoutParams();
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
    }

    AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.d(TAG,TAG+"onAudioFocusChange focusChange:"+focusChange);
            switch(focusChange){
                //你会短暂的失去音频焦点，你可以暂停音乐，但不要释放资源，因为你一会就可以夺回焦点并继续使用
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    //你的焦点会短暂失去，但是你可以与新的使用者共同使用音频焦点
                //case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    mRecordBinder.pauseRecord();
                    break;
                //你已经完全获得了音频焦点
                case AudioManager.AUDIOFOCUS_GAIN:
                    mRecordBinder.startRecord();
                    break;
                //你会长时间的失去焦点，所以不要指望在短时间内能获得。请结束自己的相关音频工作并做好收尾工作
                case AudioManager.AUDIOFOCUS_LOSS:
                    mRecordBinder.finishRecord();
                    break;
            }
        }
    };


    private boolean requesFocus(){
        if(Build.VERSION.SDK_INT<=23) {
            int result = mAudioManager.requestAudioFocus(onAudioFocusChangeListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }else{
            return true;
        }
    }

    private void abandonFocus(){
        if(Build.VERSION.SDK_INT<=23) {
            mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        }
    }

    @Override
    public int onStartCommand(Intent intent,int flags, int startId) {
        if(null == intent){
            return super.onStartCommand(intent,flags,startId);
        }
        String action = intent.getAction();
        Log.d(TAG,TAG+"onStartCommand action:"+action);
        switch(action){
            case "pause":
                mRecordBinder.pauseRecord();
                break;
            case "resume":
                mRecordBinder.startRecord();
                break;
            case "finish":
                mRecordBinder.finishRecord();
                break;
            default:break;
        }
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,TAG+"onDestroy");
        screenListener.stopListen();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG,TAG+"onUnbind");
        return super.onUnbind(intent);
    }

    public interface OnStateChangedListener{
        void onStateChanged(int stateCode);
    }

    public interface OnErrorListener{
        void onError(int errorCode);
    }

    private Notification.Builder buildNotificationBuilder(Notification.Builder builder){
        builder.setSmallIcon(R.drawable.icon_notification_bar);
        builder.setContentIntent(initPendingIntentForActivity("open"));
        if(mRecordBinder.getRecordState()==RecordStatus.RECORD_RESUME
                ||mRecordBinder.getRecordState()==RecordStatus.RECORDING){
            builder.setContentTitle(mContext.getResources().getString(R.string.recording));
            builder.addAction(0,mContext.getResources().getString(R.string.pause),initPendingIntentForService("pause"));
            builder.addAction(0,mContext.getResources().getString(R.string.finish),initPendingIntentForService("finish"));
        }else if(mRecordBinder.getRecordState()==RecordStatus.RECORD_PAUSE){
            builder.setContentTitle(mContext.getResources().getString(R.string.record_pause));
            builder.addAction(0,mContext.getResources().getString(R.string.resume),initPendingIntentForService("resume"));
            builder.addAction(0,mContext.getResources().getString(R.string.finish),initPendingIntentForService("finish"));
        }
        builder.setAutoCancel(true);
        return builder;
    }

    private PendingIntent initPendingIntentForService(String action){
        Intent intent = new Intent(action);
        intent.setClass(mContext, RecordService.class);
        return PendingIntent.getService(mContext,0,intent,0);
    }

    private PendingIntent initPendingIntentForActivity(String action){
        Intent intent = new Intent(action);
        intent.setClass(mContext, RecordActivity.class);
        return PendingIntent.getActivity(mContext,0,intent,0);
    }

    @Override
    public void onSCreenOn() {
        Log.e("scd", "screen on");
        mRecordBinder.removeFloatWindow();
        mRecordBinder.clearNotification();
        KeyguardManager km = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        Log.e("scd", "km.inKeyguardRestrictedInputMode():"+km.inKeyguardRestrictedInputMode());
        Log.e("scd", "km.isKeyguardLocked():"+km.isKeyguardLocked());
        if (km.inKeyguardRestrictedInputMode() && (mRecordBinder.getRecordState()== RecordStatus.RECORDING || mRecordBinder.getRecordState()== RecordStatus.RECORD_RESUME) ) {
            Log.e(TAG, "screen inlock!");
            Intent LockIntent = new Intent(mContext,LockScreenActivity.class);/*LockScreenActivity*/
            LockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(LockIntent);
            isShowLockSCreenActivity = true;
            Log.d("aaz","startActivity(LockIntent)");
            //initLockScreenWindow();
        }else {
            isShowLockSCreenActivity = false;
        }
    }

    @Override
    public void onScreenOff() {
        Log.e("scd", "onScreenOff");
        mRecordBinder.removeFloatWindow();
        mRecordBinder.clearNotification();
    }



    @Override
    public void onUserPresent() {
        Log.e("scd", "onUserPresent isShowLockSCreenActivity:"+isShowLockSCreenActivity);
        if(null != mRecordBinder && !isShowLockSCreenActivity && !mIsShowWave && (mRecordBinder.getRecordState()==RecordStatus.RECORD_PAUSE)){
            mRecordBinder.createNotification();
            if(isFloatWindowShow)  return;
            mRecordBinder.ShowFloatWindow(mContext.getResources().getString(R.string.record_pause)+" "+mTimeStr);
            Log.d(TAG,"onUserPresent RECORD_PAUSE ShowFloatWindow");
        }else if(null != mRecordBinder && !isShowLockSCreenActivity && !mIsShowWave &&
                (mRecordBinder.getRecordState()==RecordStatus.RECORD_RESUME|mRecordBinder.getRecordState()==RecordStatus.RECORDING)){
            mRecordBinder.createNotification();
            if(isFloatWindowShow)  return;
            mRecordBinder.ShowFloatWindow(mContext.getResources().getString(R.string.recording)+" "+mTimeStr);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mRecordBinder;
    }


    public class RecordBinder extends Binder implements Recorder.RecordListener,IRecordService {
        private OnStateChangedListener mOnStateChangedListener = null;
        private OnErrorListener mOnErrorListener = null;

        public RecordBinder() {
            mRecorder = new Recorder(getApplicationContext(), this);
        }

        public void setOnStateChangedListener(OnStateChangedListener onStateChangedListener) {
            mOnStateChangedListener = onStateChangedListener;
        }

        public void setOnErrorListener(OnErrorListener onErrorListener) {
            mOnErrorListener = onErrorListener;
        }

        @Override
        public void startRecord() {
            Log.d(TAG, TAG + "startRecord requesFocus():"+requesFocus());
            if(requesFocus()) {
                mRecorder.startRecord(MediaRecorder.OutputFormat.MPEG_4, mContext);
            }
        }

        @Override
        public void pauseRecord() {
            Log.d(TAG, TAG + "pauseRecord");
            mRecorder.pauseRecord();
        }

        @Override
        public void finishRecord() {
            Log.d(TAG, TAG + "finishRecord");
            abandonFocus();
            mRecorder.stopRecord();
        }

        @Override
        public void cancelRecord() {
            Log.d(TAG, TAG + "cancelRecord");
            abandonFocus();
            mRecorder.cancelRecord();
        }

        @Override
        public File getRecordFile(){
            Log.d(TAG,TAG+"getRecordFile");
            return mRecorder.getRecordFile();
        }

        @Override
        public File getRecordLastFile() {
            Log.d(TAG, TAG + " getRecordLastFile");
            return mRecorder.getRecordLastFile();
        }

        @Override
        public String getRecordFileName() {
            Log.d(TAG, TAG + "getRecordFileName");
            return mRecorder.getRecordFileName();
        }

        @Override
        public int getRecordState() {
            Log.d(TAG, TAG + "getRecordState");
            return mRecorder.getRecordState();
        }

        @Override
        public boolean isProcessResume() {
            Log.d(TAG, TAG + "isProcessResume");
            return mRecorder.isProcessResume();
        }

        @Override
        public int getMaxAmplitude() {
            //  Log.d(TAG,TAG+"getMaxAmplitude");
            return mRecorder.getMaxAmplitude();
        }

        @Override
        public void onStateChanged(int stateCode) {
            Log.d(TAG, TAG + "onStateChanged stateCode:" + stateCode);
            switch (stateCode) {
                case RecordStatus.RECORDING:
                case RecordStatus.RECORD_RESUME:
                    if (!mIsShowWave) {
                        Log.d(TAG, TAG + " RECORD_RESUME updateNotificationStatus");
                        updateNotificationStatus(mContext.getResources().getString(R.string.recording), "pause", mContext.getResources().getString(R.string.pause));
                    }
                    break;
                case RecordStatus.RECORD_PAUSE:
                    if (!mIsShowWave) {
                        Log.d(TAG, TAG + " RECORD_PAUSE updateNotificationStatus");
                        updateNotificationStatus(mContext.getResources().getString(R.string.record_pause), "resume", mContext.getResources().getString(R.string.resume));
                        updateFloatWindowViewLayout(mContext.getResources().getString(R.string.record_pause) + " "+mTimeStr);
                    }
                    break;
                case RecordStatus.RECORD_FINISH:
                    if (!mIsShowWave) {
                        clearNotification();
                        removeFloatWindow();
                    }
                    break;
                case RecordStatus.RECORD_CANCEL:
                case RecordStatus.IDLE:
                    if (!mIsShowWave) {
                        clearNotification();
                        removeFloatWindow();
                    }
                    break;
            }
            mOnStateChangedListener.onStateChanged(stateCode);
        }

        @Override
        public void onError(int errorCode) {
            Log.d(TAG, TAG + "onError errorCode:" + errorCode);
            mOnErrorListener.onError(errorCode);

        }

        @Override
        public void setHandler(Handler handler) {
            mRecorder.setHandler(handler);
        }

        @Override
        public List<Recorder.FrameInfo> getAmplitudeList() {
            return mRecorder.getWaveFrameList();
        }

        public void markTheTime(Boolean markable){
            mRecorder.markTheTime(markable);
        }

        @Override
        public void removeFloatWindow() {
            if(!isFloatWindowShow){
                return;
            }
            if (null != float_window_show) {
                wm.removeView(float_window_show);
                Log.d("az","cccccccccccccccccc");
                isFloatWindowShow = false;
                float_window_show = null;
            }
        }

        public int getStatusBarHeight() {
            int result = 0;
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = getResources().getDimensionPixelSize(resourceId);
            }
            return result;
        }

        @Override
        public void ShowFloatWindow(String timeStr) {
            if (isFloatWindowShow) {
                return;
            }
            Log.d("aaz","onUserPresent RECORDING ShowFloatWindow getStatusBarHeight():"+getStatusBarHeight());
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
            params.format = PixelFormat.RGBA_8888;
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_FULLSCREEN;
            params.gravity = Gravity.LEFT | Gravity.TOP;
            params.x = 0;
            params.y = 0;
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = getStatusBarHeight();
            //params.format = PixelFormat.OPAQUE;
            params.alpha = 0.9f;
            float_window_show = new TextView(mContext);
            float_window_show.setBackgroundColor(0xFFF45454);
            float_window_show.setTextSize(14);
            float_window_show.setTextColor(0xFFFFFFFF);
            float_window_show.setGravity(Gravity.CENTER);
            float_window_show.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("zzz", "aaaaaaaaaaaaaaaa");
                    Intent intent = new Intent();
                    intent.setClass(mContext, RecordActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    Log.d("zzz", "bbbbbbbbbbbbbbbbb");
                    mContext.startActivity(intent);
                }
            });
            float_window_show.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // TODO Auto-generated method stub
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            mPosX = event.getX();
                            mPosY = event.getY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            mCurPosX = event.getX();
                            mCurPosY = event.getY();

                            break;
                        case MotionEvent.ACTION_UP:
                            if (mCurPosY - mPosY > 0
                                    && (Math.abs(mCurPosY - mPosY) > 5)) {
                                Log.d("zzz", "down filing");
                                openExpandSatusBar();
                                //向下滑動

                            } else if (mCurPosY - mPosY < 0
                                    && (Math.abs(mCurPosY - mPosY) > 5)) {
                                Log.d("zzz", "up filing");
                                //向上滑动
                            }

                            break;
                    }
                    return false;
                }
            });
            float_window_show.setText(timeStr);
            wm.addView(float_window_show, params);
            isFloatWindowShow = true;
        }


        private void openExpandSatusBar() {
            try{
                Object statuabarService = getSystemService("statusbar");
                Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
                if(statuabarService!=null){
                    Method expandStatusbar=statusbarManager.getMethod("expandNotificationsPanel");
                    expandStatusbar.setAccessible(true);
                    expandStatusbar.invoke(statuabarService);
                    Log.d(TAG, "openExpandSatusBar");
                }
            }catch(Exception e){
                Log.d(TAG, "openExpandSatusBar exception:"+e);
                e.printStackTrace();
            }
        }

        @Override
        public void setRecordTime(String timeStr) {
            mTimeStr = timeStr;
        }

        @Override
        public void updateFloatWindowViewLayout(String winText) {
            if (null != float_window_show) {
                float_window_show.setText(winText);
                wm.updateViewLayout(float_window_show, params);
            }else{
                mRecordBinder.ShowFloatWindow(winText);
            }
        }

        @Override
        public void setIsShowWave(boolean isShowWave) {
            mIsShowWave = isShowWave;
        }

        @Override
        public void createNotification() {
            LogUtil.i2("log","aaaaaaaaaaaaaa");
            Notification.Builder builder = new Notification.Builder(mContext);
            builder = buildNotificationBuilder(builder);
            builder.setContentText(mTimeStr);
            notification = builder.build();
            manager.notify(RECORD_NOTIFICATION_ID, notification);
            startForeground(RECORD_NOTIFICATION_ID,notification);
        }

        @Override
        public void clearNotification() {
            stopForeground(true);
            manager.cancel(RECORD_NOTIFICATION_ID);
        }

        @Override
        public void updateNotificationTime(String time) {
            Notification.Builder builder = new Notification.Builder(mContext);
            builder = buildNotificationBuilder(builder);
            builder.setContentText(time);
            notification = builder.build();
            manager.notify(RECORD_NOTIFICATION_ID, notification);
            startForeground(RECORD_NOTIFICATION_ID,notification);
        }

        @Override
        public void updateNotificationStatus(String contentTitle, String intentStr, String buttonStatus) {
            PendingIntent intent = initPendingIntentForService(intentStr);
            Notification.Builder builder = new Notification.Builder(mContext);
            builder.setSmallIcon(R.drawable.icon_notification_bar);
            builder.setAutoCancel(true);
            builder.setContentTitle(contentTitle);
            builder.setContentText(mTimeStr);
            builder.setContentIntent(initPendingIntentForActivity("open"));
            builder.addAction(0, buttonStatus, intent);
            builder.addAction(0, mContext.getResources().getString(R.string.finish), initPendingIntentForService("finish"));
            notification = builder.build();
            manager.notify(RECORD_NOTIFICATION_ID, notification);
            startForeground(RECORD_NOTIFICATION_ID,notification);
        }
    }
}
