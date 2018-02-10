package cn.com.protruly.soundrecorder.lockscreen;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cn.com.protruly.soundrecorder.MarkManager;
import cn.com.protruly.soundrecorder.R;
import cn.com.protruly.soundrecorder.RecordActivity;
import cn.com.protruly.soundrecorder.RecordListActivity;
import cn.com.protruly.soundrecorder.common.BaseActivity;
import cn.com.protruly.soundrecorder.managerUtil.AudioRecordManager;
import cn.com.protruly.soundrecorder.managerUtil.AudioRecordManagerService;
import cn.com.protruly.soundrecorder.util.DatabaseUtil;
import cn.com.protruly.soundrecorder.util.GlobalConstant;
import cn.com.protruly.soundrecorder.util.GlobalUtil;

public class MediaRecordTestActivity extends BaseActivity implements View.OnClickListener{
    private RecordWaveView surfaceView;
    private TextView TimeCounter;
    private TextView RecordFileName;
    private Button markButtom;
    private Button startButtom;
    private Button stopButtom;
    AudioRecordManager mAudioRecordManager;
    private List<Integer> wavelist;
    private int mWaveMaxWidth;
    private int mWaveMaxHight;

    @Override
    protected void initData() {

    }

    @Override
    protected void initView() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MediaRecordManagerService","onCreate MediaRecordTestActivity");

    /*    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);*/

        setContentView(R.layout.activity_media_record_test);
        surfaceView = (RecordWaveView)findViewById(R.id.record_surfaceView);
        TimeCounter = (TextView)findViewById(R.id.time_count_tv);
        RecordFileName = (TextView)findViewById(R.id.record_filename_tv);
        markButtom = (Button) findViewById(R.id.mark_buttom);
        markButtom.setOnClickListener(this);
        startButtom = (Button)findViewById(R.id.start_buttom);
        startButtom.setOnClickListener(this);
        stopButtom = (Button)findViewById(R.id.stop_buttom);
        stopButtom.setOnClickListener(this);
        mAudioRecordManager = AudioRecordManager.getInstance();
        mAudioRecordManager.onInitAudioRecordManager(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!Settings.canDrawOverlays(getApplicationContext())) {
                //启动Activity让用户授权
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent,100);
            }
        }
        initRecordWave(surfaceView);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("MediaRecordManagerService", "onDestroy MediaRecordTestActivity");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.mark_buttom:
                Intent intent2 = new Intent(MediaRecordTestActivity.this,RecordListActivity.class);
                startActivity(intent2);
                break;
            case R.id.start_buttom:
                Log.d("wwc516","onClick----record");
                mAudioRecordManager.onStartRecord(mHandler);
                break;
            case R.id.stop_buttom:
                Log.d("wwc516","onClick----finish");
                mAudioRecordManager.onStopRecord();
                break;
        }
    }


    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what== GlobalConstant.RECORD_START) {
                if (msg.obj != null) {
                    final AudioRecordManagerService.RecordMessageInfo info = (AudioRecordManagerService.RecordMessageInfo) msg.obj;
                  //  surfaceView.setDataList(mAudioRecordManager.getWaveAmplitudeList());
                    TimeCounter.setText(GlobalUtil.formatLongTimeString(info.timeCount));
                    RecordFileName.setText(info.name);
                }
            }else if(msg.what== GlobalConstant.RECORD_PAUSE){

            }else if(msg.what== GlobalConstant.RECORD_STOP){

            }else{

            }

        }
    };

    int lastRatio = 0;
    private void dealWaveData(int value){
        int ratio = mWaveMaxHight*value/32767;
        if(ratio<0)ratio=0;
        int temp = ratio;
        ratio = (ratio+lastRatio)/2;
        lastRatio = temp;
        wavelist.add(ratio);
        Log.d("MediaRecordManagerService","lastRatio="+lastRatio+"    mWaveMaxWidth="+mWaveMaxWidth+"    mWaveMaxHight="+mWaveMaxHight);
        if(wavelist.size()>mWaveMaxWidth/5){
            wavelist.remove(0);
        }


    }

    private void initRecordWave(final RecordWaveView wave){
        wave.post(new Runnable() {
            @Override
            public void run() {
                mWaveMaxWidth = wave.getWidth();
                mWaveMaxHight = wave.getHeight();
            }
        });
        wavelist = new ArrayList<>();
    }



}
