package cn.com.protruly.soundrecorder.lockscreen;

import android.app.KeyguardManager;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cn.com.protruly.soundrecorder.RecordActivity;
import cn.com.protruly.soundrecorder.RecordService;
import cn.com.protruly.soundrecorder.Recorder;
import cn.com.protruly.soundrecorder.common.BaseActivity;
import cn.com.protruly.soundrecorder.R;
import cn.com.protruly.soundrecorder.common.RecordStatus;
import cn.com.protruly.soundrecorder.managerUtil.AudioRecordManager;
import cn.com.protruly.soundrecorder.managerUtil.AudioRecordManagerService;
import cn.com.protruly.soundrecorder.util.GlobalConstant;
import cn.com.protruly.soundrecorder.util.GlobalUtil;

/**
 * Created by wenwenchao on 17-9-2.
 */

public class LockScreenActivity extends BaseActivity{

    private final String TAG = "LockScreenActivity";
    private TextView nowTimeTextView;
    private TextView dateTimeTextView;
    private RecordWaveView mRecordWaveView;
    private TextView recordTimeTextView;
  //  AudioRecordManager mAudioRecordManager;
    private RecordService.RecordBinder mRecordBinder;
    private long recordTime;
    private long hanlderLastTimeValue = 0;
    private long hanlderthisTimeValue;
    private KeyguardManager mKeyguardManager;

    @Override
    protected void initData() {
    }
    @Override
    protected void initView() {

    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideNavigationBar();
        Log.d("MediaRecordManagerService","LockScreenActivity>onCreate");
       // mAudioRecordManager = AudioRecordManager.getInstance();
      //  mAudioRecordManager.onInitAudioRecordManager(this);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        linearLayout.setBackground(new BitmapDrawable(blur(getWallPaper(this),24f)));
        PagerLayout myLinearLayout = new PagerLayout(this);
        myLinearLayout.setLayout(this, R.layout.slide_screen_layout);//R.layout.slide_layout
        myLinearLayout.setmHideCallBack(new PagerLayout.HideCallBack() {
            @Override
            public void onPagehided() {
                LockScreenActivity.this.finish();
                mRecordBinder.setHandler(recordmHandler);
            }
        });
        linearLayout.addView(myLinearLayout);
        setContentView(linearLayout);
        nowTimeTextView = (TextView)myLinearLayout.findViewById(R.id.time_h_m);
        Typeface robotoThin = Typeface.createFromFile("/system/fonts/Roboto-Thin.ttf");
        nowTimeTextView.setTypeface(robotoThin);
        dateTimeTextView = (TextView)myLinearLayout.findViewById(R.id.time_m_d_w);

        recordTimeTextView = (TextView)myLinearLayout.findViewById(R.id.time_record);
        Typeface robotoLight = Typeface.createFromFile("/system/fonts/Roboto-Light.ttf");
        recordTimeTextView.setTypeface(robotoLight);
        mRecordWaveView = (RecordWaveView)myLinearLayout.findViewById(R.id.waveView);



        Intent serviceIntent = new Intent(this,RecordService.class);
        getApplicationContext().bindService(serviceIntent,mServiceConnection,BIND_AUTO_CREATE);

        mKeyguardManager = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
    }


   private Handler mHandler = new Handler(){
       @Override
       public void handleMessage(Message msg) {
           super.handleMessage(msg);
           Log.d("cx","lock handler:"+msg.what);
           if(GlobalUtil.isLowPwer(getBaseContext())) {
               mRecordBinder.finishRecord();
               Toast.makeText(getBaseContext(),"低电量停止录音，自动保存文件",Toast.LENGTH_SHORT).show();
               return;
           }
           if(GlobalUtil.isLowerSpace()){
               mRecordBinder.finishRecord();
               Toast.makeText(getBaseContext(),"低内存停止录音，自动保存文件",Toast.LENGTH_SHORT).show();
               return;
           }
           if(msg.what == RecordStatus.RECORDING || msg.what == RecordStatus.RECORD_RESUME){
               if(msg.obj!=null){
                   String fileName = ((Recorder.WaveListInfo)msg.obj).recordName;
                   long timecount = ((Recorder.WaveListInfo)msg.obj).timeCount;
                   recordTime = timecount;
                   List<Recorder.FrameInfo> list = mRecordBinder.getAmplitudeList();
                   if(list!=null){
                       mRecordWaveView.setDataList(list);
                   }
                   recordTimeTextView.setText(GlobalUtil.formatTime_m_s(timecount));
               }
           }
           updateTime();
       }
   };

    private Handler recordmHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d("cx","ssd  handler:"+msg.what);
            if(GlobalUtil.isLowPwer(getBaseContext())) {
                mRecordBinder.finishRecord();
                Toast.makeText(getBaseContext(),getBaseContext().getResources().getString(R.string.low_power_save_record_file),Toast.LENGTH_SHORT).show();
                return;
            }
            if(GlobalUtil.isLowerSpace()){
                mRecordBinder.finishRecord();
                Toast.makeText(getBaseContext(),getBaseContext().getResources().getString(R.string.low_space_save_record_file),Toast.LENGTH_SHORT).show();
                return;
            }
            if(msg.what == RecordStatus.RECORDING || msg.what == RecordStatus.RECORD_RESUME){
                if(msg.obj!=null){
                    String fileName = ((Recorder.WaveListInfo)msg.obj).recordName;
                    long timecount = ((Recorder.WaveListInfo)msg.obj).timeCount;
                    List<Recorder.FrameInfo> list = mRecordBinder.getAmplitudeList();
                    mRecordWaveView.setDataList(list);
                    hanlderthisTimeValue = timecount;
                    if(!mKeyguardManager.isKeyguardLocked()) {
                        if (GlobalUtil.isTimeCountAdvance(hanlderLastTimeValue, hanlderthisTimeValue)) {
                            Log.d("cx", "eeeeeeeeeeeeeeeeeee");
                            mRecordBinder.setRecordTime(GlobalUtil.formatTime_m_s(timecount));
                            mRecordBinder.updateNotificationTime(GlobalUtil.formatTime_m_s(timecount));
                            mRecordBinder.updateFloatWindowViewLayout(getResources().getString(R.string.recording) +" "+GlobalUtil.formatTime_m_s(timecount));
                        }
                    }
                    hanlderLastTimeValue = timecount;
                }
            }else if(msg.what == RecordStatus.RECORD_FINISH){
                mRecordBinder.setRecordTime(GlobalUtil.formatTime_m_s(0));
            }
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mRecordBinder = (RecordService.RecordBinder) service;
            if(mRecordBinder!=null){
                mRecordBinder.setHandler(mHandler);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mRecordBinder = null;
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        hideNavigationBar();
        if(mRecordBinder!=null){
            mRecordBinder.setHandler(mHandler);
            Log.d("aaz","lockactivity onResume removeFloatWindow");
            mRecordBinder.removeFloatWindow();
            mRecordBinder.clearNotification();
        }
        Log.d("MediaRecordManagerService","LockScreenActivity>onResume");

       /* KeyguardManager mKeyguardManager = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock mKeyguardLock = mKeyguardManager.newKeyguardLock("zdLock 1");
        mKeyguardLock.disableKeyguard();*/

    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
                super.onWindowFocusChanged(hasFocus);
                if( hasFocus ) {
                        hideNavigationBar();
                    }
             }

/*    @Override
     public boolean onKeyUp(int keyCode, KeyEvent event) {
                 super.onKeyUp(keyCode, event);
                //if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)){
                         this.hideNavigationBar();
                    // }
                 return false;
             }*/

    //监听系统的物理按键
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.i("tag", "===BACK====");
        } else if (keyCode == KeyEvent.KEYCODE_HOME) {
            Log.i("tag", "===HOME====");
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            Log.i("tag", "===KEYCODE_MENU====");
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            Log.i("tag", "===KEYCODE_MENU====");
        }
        hideNavigationBar();
        return false;
    }


    private  void   hideNavigationBar(){
        final Window win = getWindow();
        win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                // | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER

        );
        win.getDecorView().setSystemUiVisibility(
                          View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
        );
    }





    private void updateTime(){
        nowTimeTextView.setText(GlobalUtil.formatDate_H_m(System.currentTimeMillis()));
        dateTimeTextView.setText(GlobalUtil.formatDate_M_d_w(this,System.currentTimeMillis()));
    }



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
            mWmParams.type = WindowManager.LayoutParams.TYPE_TOAST;
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
     //   View view = LayoutInflater.from(this).inflate(R.layout.activity_media_record_test,null);
     //   mWindowManager.addView(view, mWmParams);
    }


    private Bitmap getWallPaper(Context context) {
        WallpaperManager wallpaperManager = WallpaperManager
                .getInstance(context);
        // 获取当前壁纸
        Drawable wallpaperDrawable = wallpaperManager.getDrawable();
        Bitmap bm = ((BitmapDrawable) wallpaperDrawable).getBitmap();

        int heightPixels = getResources().getDisplayMetrics().heightPixels;
        int widthPixels = getResources().getDisplayMetrics().widthPixels;
        int with = bm.getHeight()*widthPixels/heightPixels > bm.getWidth() ? bm.getWidth():bm.getHeight()*widthPixels/heightPixels;
        Bitmap pbm = Bitmap.createBitmap(bm, 0, 0, with, bm.getHeight());
        // 设置 背景
        return pbm;
    }


    private Bitmap blur(Bitmap bitmap,float radius) {
        Bitmap output = Bitmap.createBitmap(bitmap); // 创建输出图片
        RenderScript rs = RenderScript.create(this); // 构建一个RenderScript对象
        ScriptIntrinsicBlur gaussianBlue = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs)); // 创建高斯模糊脚本
        Allocation allIn = Allocation.createFromBitmap(rs, bitmap); // 创建用于输入的脚本类型
        Allocation allOut = Allocation.createFromBitmap(rs, output); // 创建用于输出的脚本类型
        gaussianBlue.setRadius(radius); // 设置模糊半径，范围0f<radius<=25f
        gaussianBlue.setInput(allIn); // 设置输入脚本类型
        gaussianBlue.forEach(allOut); // 执行高斯模糊算法，并将结果填入输出脚本类型中
        allOut.copyTo(output); // 将输出内存编码为Bitmap，图片大小必须注意
        rs.destroy(); // 关闭RenderScript对象，API>=23则使用rs.releaseAllContexts()
        return output;
    }

}
