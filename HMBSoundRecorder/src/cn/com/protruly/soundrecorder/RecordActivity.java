package cn.com.protruly.soundrecorder;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StatFs;
import android.os.PowerManager;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.lang.reflect.Field;
import java.util.concurrent.Executors;

import cn.com.protruly.soundrecorder.clip.Mp3Decoder;
import cn.com.protruly.soundrecorder.common.BaseActivity;
import cn.com.protruly.soundrecorder.common.RecordError;
import cn.com.protruly.soundrecorder.common.RecordStatus;
import cn.com.protruly.soundrecorder.lockscreen.RecordWaveView;
import cn.com.protruly.soundrecorder.util.DatabaseUtil;
import cn.com.protruly.soundrecorder.util.GlobalUtil;
import hb.app.dialog.AlertDialog;

/**
 * Created by liushitao on 17-9-1.
 */

public class RecordActivity extends BaseActivity implements View.OnClickListener,
        RecordService.OnStateChangedListener,RecordService.OnErrorListener{

    private String TAG = "RecordActivity";
    private RecordService.RecordBinder mRecordBinder;
    Context mContext;
    private ImageView record,mark,list;
    private TextView mRecordTitle;
    private RecordWaveView mRecordWaveView;
    private TextView recordTime;
    private HorizontalScrollView mHorizontalScrollView;
    private LinearLayout mDynamicMarkViewArea;
    private long hanlderLastTimeValue = 0;
    private long hanlderthisTimeValue;
    private long lastMarkTime;
    private ArrayList<Long> markList;
    private long markPoint;
    private String markPointStr;
    private long MIN_MARK_PERIOD = 1000;
    private long MAX_MARK_NUM = 50;
    private long MIN_RECORD_PERIOD = 1000;
    private MarkManager mMarkManager;
    private DatabaseUtil databaseUtil;
    private boolean isShowWave = true;
    private boolean isKeyBackHit = false;
    private Boolean isGetContentMode = false;
    private boolean isDestroy = false;


    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mRecordBinder = (RecordService.RecordBinder) service;
            mRecordBinder.setOnStateChangedListener(RecordActivity.this);
            mRecordBinder.setOnErrorListener(RecordActivity.this);
            //insertRecordFile();//because in onServiceConnected not do wasting-time task
            //recordUiUpdateForConnectService();
            Log.d(TAG,"onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mRecordBinder = null;
            Log.d(TAG,"onServiceDisconnected");
        }
    };

    @Override
    public void onStateChanged(int state) {
        Log.d(TAG,TAG+"onStateChanged state:"+state);
        switch(state) {
            case RecordStatus.RECORDING:
            case RecordStatus.RECORD_RESUME:
                startRecordUpdateUI();
                showMarkAndList();
                if(markList.size()>=MAX_MARK_NUM){
                    markDisabled();
                }else{
                    markEnabled();
                }
                pauseEnabled();
                if(hanlderthisTimeValue>=MIN_RECORD_PERIOD) {
                    finishEnabled();
                }else{
                    finishDisabled();
                }
                break;
            case RecordStatus.RECORD_PAUSE:
                mRecordTitle.setText(mRecordBinder.getRecordFileName());
                showMarkAndList();
                markDisabled();
                recordEnabled();
                if(hanlderthisTimeValue>=MIN_RECORD_PERIOD) {
                    finishEnabled();
                }else{
                    finishDisabled();
                }
                break;
            case RecordStatus.RECORD_FINISH:
                finishRecordUpdateUI();
                showMarkAndList();
                markDisabled();
                recordEnabled();
                listEnabled();
                break;
            case RecordStatus.RECORD_CANCEL:
            case RecordStatus.IDLE:
                cancelRecordUpdateUI();
                showMarkAndList();
                markDisabled();
                recordEnabled();
                if(isHasRecordFile()) {
                    listEnabled();
                }else{
                    finishDisabled();
                }
                break;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent serviceIntent = new Intent(this,RecordService.class);
        getApplicationContext().bindService(serviceIntent,mServiceConnection,BIND_AUTO_CREATE);
        checkGetContenMode(getIntent());
        Log.d(TAG, "onCreate");
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkGetContenMode(intent);
        Log.d(TAG,"onNewIntent");
    }


    private void checkGetContenMode(Intent intent){
        if(intent!=null && intent.getAction()!=null && intent.getAction().equals("android.intent.action.GET_CONTENT")){
            isGetContentMode = true;
        }else{
            isGetContentMode = false;
        }
    }

    private void sendBackUri(){
        if(isGetContentMode){
            isGetContentMode = false;
            try{
                Uri uri =databaseUtil.getContentUri(mRecordBinder.getRecordFile().getPath());
                Intent intent = null;
                if(uri!=null){
                    intent = Intent.parseUri(uri.toString(),0);
                }else{
                    Uri uri0 = Uri.fromFile(mRecordBinder.getRecordFile());
                    if(null != uri0){
                        intent = Intent.parseUri(uri0.toString(),0);
                    }
                }
                Log.d("wwcwwc","sendBackUri>intent:"+intent);
                RecordActivity.this.setResult(-1,intent);
                RecordActivity.this.finish();
            }catch(URISyntaxException e){
                e.fillInStackTrace();
            }
        }
    }


    @Override
    protected void initData() {
        Log.d(TAG,"initData");
        mContext = this;
        markList = new ArrayList<>();
        mMarkManager =  new MarkManager(mContext);
        databaseUtil = new DatabaseUtil(mContext);
        lastMarkTime = 0;
    }

    @Override
    protected void initView() {
        Log.d(TAG,"initView");
        setContentView(R.layout.recorder_home);
        record = (ImageView)findViewById(R.id.record);
        mark = (ImageView)findViewById(R.id.mark);
        list = (ImageView) findViewById(R.id.list);

        record.setOnClickListener(this);
        mark.setOnClickListener(this);
        mark.setEnabled(false);//becase xml cannot setEnabled(false);
        list.setOnClickListener(this);

        recordTime = (TextView) findViewById(R.id.chronometer);
        mRecordTitle = (TextView) findViewById(R.id.record_title);
        mRecordTitle.setText(R.string.record);

        mHorizontalScrollView = (HorizontalScrollView)findViewById(R.id.mark_list_view);
        mDynamicMarkViewArea = (LinearLayout) findViewById(R.id.mark_list_container);

        mRecordWaveView = (RecordWaveView)findViewById(R.id.waveView);
    }


    private void addMarkTextView(String markPoint){
        if(TextUtils.isEmpty(markPoint)){
            return;
        }
        final TextView view = (TextView) View.inflate(mContext,R.layout.mark_text_layout,null);
        view.setText(markPoint);
        view.setTag(markPoint);
        view.setVisibility(View.VISIBLE);
        view.setOnClickListener(this);
        mDynamicMarkViewArea.addView(view);
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mHorizontalScrollView.smoothScrollTo(mDynamicMarkViewArea.getWidth()+view.getWidth(),0);
            }
        });
    }

    private void removeAllMarkTextView() {
        mDynamicMarkViewArea.removeAllViews();
        markList.clear();
    }

    @Override
    public void onClick(View button) {
        Log.d(TAG,"onClick "+button.getId());
        if(isFinishing()) return;
        if(!button.isEnabled()) return;
        switch (button.getId()){
            case R.id.record:
                if(mRecordBinder != null) {
                    if (mRecordBinder.getRecordState() == RecordStatus.RECORD_RESUME
                            || mRecordBinder.getRecordState() == RecordStatus.RECORDING) {
                        pauseRecord();
                    } else {
                        startRecord();
                    }
                }
                break;
            case R.id.mark:
                markRecord();
                break;
            case R.id.list:
                if(isHasRecordFile()
                        && (mRecordBinder.getRecordState()==RecordStatus.IDLE
                        ||mRecordBinder.getRecordState()==RecordStatus.RECORD_FINISH||
                        mRecordBinder.getRecordState()==RecordStatus.RECORD_CANCEL) && !isGetContentMode){
                        startRecordListActivity();
                }else{
                    finishRecord();
                }
                break;
        }
    }

    private void startRecord(){
        if(GlobalUtil.isLowPwer(mContext)) {
            showRecordInfoDialog(mContext.getResources().getString(R.string.record_notice),mContext.getResources().getString(R.string.message_low_power));
            return;
        }
        if(GlobalUtil.isLowerSpace()){
            showRecordInfoDialog(mContext.getResources().getString(R.string.record_notice),mContext.getResources().getString(R.string.message_low_space));
            return;
        }
        mRecordBinder.startRecord();
    }

    private void showRecordInfoDialog(String tile,String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(tile);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.know_message,null);
        builder.create().show();
    }

    private void startRecordUpdateUI(){
        //showWave();
        mRecordTitle.setText(mRecordBinder.getRecordFileName());
        if(mRecordBinder!=null){
            mRecordBinder.setHandler(mHandler);
        }
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d("bql","handler:"+msg.what);
            if(GlobalUtil.isLowPwer(mContext)) {
                mRecordBinder.finishRecord();
                Toast.makeText(mContext, mContext.getResources().getString(R.string.low_power_save_record_file),Toast.LENGTH_SHORT).show();
                return;
            }
            if(GlobalUtil.isLowerSpace()){
                mRecordBinder.finishRecord();
                Toast.makeText(mContext,mContext.getResources().getString(R.string.low_space_save_record_file),Toast.LENGTH_SHORT).show();
                return;
            }
            if(msg.what == RecordStatus.RECORDING || msg.what == RecordStatus.RECORD_RESUME){
                if(msg.obj!=null){
                    String fileName = ((Recorder.WaveListInfo)msg.obj).recordName;
                    long timecount = ((Recorder.WaveListInfo)msg.obj).timeCount;
                    List<Recorder.FrameInfo> list = mRecordBinder.getAmplitudeList();
                    mRecordWaveView.setDataList(list);
                    hanlderthisTimeValue = timecount;
                    if(hanlderthisTimeValue>=MIN_RECORD_PERIOD) {
                        finishEnabled();
                    }else{
                        finishDisabled();
                    }
                    if(GlobalUtil.isTimeCountAdvance(hanlderLastTimeValue,hanlderthisTimeValue)){
                        recordTime.setText(GlobalUtil.formatTime_m_s(timecount));
                        mRecordBinder.setRecordTime(GlobalUtil.formatTime_m_s(timecount));
                        if(!isShowWave) {
                            mRecordBinder.updateNotificationTime(GlobalUtil.formatTime_m_s(timecount));
                            mRecordBinder.updateFloatWindowViewLayout(mContext.getResources().getString(R.string.recording)+" "+GlobalUtil.formatTime_m_s(timecount));
                        }
                    }
                    hanlderLastTimeValue = timecount;
                }
            }else if(msg.what == RecordStatus.RECORD_FINISH){
                recordTime.setText(GlobalUtil.formatTime_m_s(0));
                mRecordBinder.setRecordTime(GlobalUtil.formatTime_m_s(0));
            }
        }
    };


    private void pauseRecord(){
        mRecordBinder.pauseRecord();
    }

    private void markRecord(){
        if(markList.size()>=MAX_MARK_NUM){
            return;
        }
        if(null != recordTime) {
            lastMarkTime = getLastMarkTime();
            markPoint = hanlderthisTimeValue;
            if(0 != lastMarkTime && ((markPoint-lastMarkTime)<MIN_MARK_PERIOD)){
                //Toast.makeText(mContext,mContext.getResources().getString(R.string.not_mark_in_one_mimute),Toast.LENGTH_SHORT).show();
                return;
            }
            markPointStr = GlobalUtil.formatTime_m_s(markPoint);
        }else{
            Log.d(TAG,"mark faile");
        }
        markList.add(markPoint);
        if(mRecordBinder!=null){
            mRecordBinder.markTheTime(true);
        }
        addMarkTextView(markPointStr);
        if(markList.size()>=MAX_MARK_NUM){
            markDisabled();
            Toast.makeText(mContext,mContext.getResources().getString(R.string.max_mark_fifty),Toast.LENGTH_SHORT).show();
        }else{
            markEnabled();
        }
    }

    private void finishRecord(){
        mRecordBinder.finishRecord();
        /*if(hanlderthisTimeValue>=MIN_RECORD_PERIOD) {
            mRecordBinder.finishRecord();
        }else{
            mRecordBinder.cancelRecord();
            Toast.makeText(mContext,mContext.getResources().getString(R.string.record_not_reach_five_seconds),Toast.LENGTH_SHORT).show();
        }*/
    }

    private void finishRecordUpdateUI(){
        mRecordTitle.setText(R.string.record);
        //hidenWave();
        recordTime.setText(GlobalUtil.formatTime_m_s(0));
        mRecordBinder.setRecordTime(GlobalUtil.formatTime_m_s(0));
        final File file = mRecordBinder.getRecordFile();
        Log.d(TAG,"finishRecord markList.size:"+markList.size());
        mMarkManager.putMarkList(file.getPath(),markList);
        removeAllMarkTextView();
        Executors.newFixedThreadPool(0x5).execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,"finishRecord insertRecordFile:"+file.getPath());
                databaseUtil.insertRecordFile(file.getPath());
                sendBackUri();
            }
        });
        mRecordBinder.clearNotification();
        mRecordBinder.removeFloatWindow();
        Toast.makeText(mContext,mContext.getResources().getString(R.string.record_file_saved, new Object[] {file.getName()}),Toast.LENGTH_SHORT).show();
        Log.d("tts","isGetContentMode:"+isGetContentMode);
        if(!isGetContentMode && !isDestroy)startRecordListActivity();
    }

    private void cancelRecordUpdateUI(){
        mRecordTitle.setText(R.string.record);
        //hidenWave();
        recordTime.setText(GlobalUtil.formatTime_m_s(0));
        mRecordBinder.setRecordTime(GlobalUtil.formatTime_m_s(0));
        removeAllMarkTextView();
        mRecordBinder.clearNotification();
        mRecordBinder.removeFloatWindow();
    }

    private void prepareRecordUpdateUI(){
        insertRecordFile();
        mRecordTitle.setText(R.string.record);
        //hidenWave();
        recordTime.setText(GlobalUtil.formatTime_m_s(0));
        mRecordBinder.setRecordTime(GlobalUtil.formatTime_m_s(0));
        removeAllMarkTextView();
    }

    private void insertRecordFile(){
        final File file = mRecordBinder.getRecordLastFile();
        Log.d("Recorder","file.getname:"+file.getName());
        if(file.exists() && !databaseUtil.isHasThisFileInDB(file.getPath())){
            Executors.newFixedThreadPool(0x5).execute(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"prepareRecordUpdateUI insertRecordFile:"+file.getPath());
                    databaseUtil.insertRecordFile(file.getPath());
                }
            });
        }
    }

    private void startRecordListActivity(){
        Intent intent = new Intent(RecordActivity.this,RecordListActivity.class);
        startActivity(intent);
    }

    private long getLastMarkTime(){
        if(null != markList && markList.size()>=1){
            return markList.get(markList.size()-1);
        }
        return 0;
    }

    private boolean isHasRecordPermission(){
        return Build.VERSION.SDK_INT>=23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG,"keyCode:"+keyCode);
        switch (keyCode){
            case KeyEvent.KEYCODE_BACK:
                if(event.getRepeatCount() == 0 && (mRecordBinder.getRecordState()==RecordStatus.RECORDING
                        ||mRecordBinder.getRecordState()==RecordStatus.RECORD_RESUME||mRecordBinder.getRecordState()==RecordStatus.RECORD_PAUSE)){
                    showDialog(mContext.getResources().getString(R.string.record_notice),mContext.getResources().getString(R.string.giveup_the_record));
                    return true;
                }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showDialog(String title,String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isKeyBackHit = true;
                mRecordBinder.cancelRecord();
                recordTime.setText(GlobalUtil.formatTime_m_s(0));
                mRecordBinder.setRecordTime(GlobalUtil.formatTime_m_s(0));
                mRecordBinder.removeFloatWindow();
                //finish();
            }
        });
        builder.setNegativeButton(android.R.string.cancel,null);
        builder.create().show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
        isShowWave = true;
        if(mRecordBinder!=null){
            mRecordBinder.setHandler(mHandler);
            mRecordBinder.setIsShowWave(true);
            mRecordBinder.removeFloatWindow();
            mRecordBinder.clearNotification();
        }
        if (!isHasRecordPermission()) {
            requestPermissions(new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);
        }else{
            recordUiUpdate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isShowWave = false;
        if(null != mRecordBinder) {
            mRecordBinder.setIsShowWave(false);
        }
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        if(null != mRecordBinder && (mRecordBinder.getRecordState()==RecordStatus.RECORDING
                ||mRecordBinder.getRecordState()==RecordStatus.RECORD_RESUME||mRecordBinder.getRecordState()==RecordStatus.RECORD_PAUSE)) {
            mRecordBinder.createNotification();
        }
        if(null != mRecordBinder && (mRecordBinder.getRecordState()==RecordStatus.RECORDING
                ||mRecordBinder.getRecordState()==RecordStatus.RECORD_RESUME)){
            mRecordBinder.removeFloatWindow();
            if(pm.isScreenOn()) {
                mRecordBinder.ShowFloatWindow(mContext.getResources().getString(R.string.recording) + " "+recordTime.getText().toString());
            }
        }else if(null != mRecordBinder && (mRecordBinder.getRecordState()==RecordStatus.RECORD_PAUSE)){
            mRecordBinder.removeFloatWindow();
            if(pm.isScreenOn()) {
                mRecordBinder.ShowFloatWindow(mContext.getResources().getString(R.string.record_pause) + " "+recordTime.getText().toString());
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("zzz","onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("zzz","onDestroy mRecordBinder:"+mRecordBinder);
        if(null != mRecordBinder && !isKeyBackHit) {
            Log.d("zzz","aaaaaaaaaaaaaa");
            isDestroy = true;
            mRecordBinder.finishRecord();
        }
        getApplicationContext().unbindService(mServiceConnection);
    }

    private void hidenWave(){
        mRecordWaveView.setVisibility(View.INVISIBLE);
    }

    private void showWave(){
        mRecordWaveView.setVisibility(View.VISIBLE);
    }

    private void showMarkAndList(){
        mark.setVisibility(View.VISIBLE);
        list.setVisibility(View.VISIBLE);
    }

    private void markEnabled(){
        if(null == mark) return;
        mark.setBackgroundResource(R.drawable.icon_record_mark);
        //mark.setImageResource(R.drawable.icon_record_mark);
        mark.setEnabled(true);
    }

    private void markDisabled(){
        if(null == mark) return;
        mark.setBackgroundResource(R.drawable.icon_record_mark_disabled);
        //mark.setImageResource(R.drawable.icon_record_mark_disabled);
        mark.setEnabled(false);
    }

    private void finishEnabled(){
        if(null == list) return;
        list.setBackgroundResource(R.drawable.icon_record_finish);
        //list.setImageResource(R.drawable.icon_record_finish);
        list.setEnabled(true);
    }

    private void finishDisabled(){
        if(null == list) return;
        list.setBackgroundResource(R.drawable.icon_record_finish_disabled);
        //list.setImageResource(R.drawable.icon_record_finish_disabled);
        list.setEnabled(false);
    }

    private void listEnabled(){
        if(null == list) return;
        list.setBackgroundResource(R.drawable.icon_record_list);
        //list.setImageResource(R.drawable.icon_record_list);
        list.setEnabled(true);
    }

    private void recordEnabled(){
        if(null == record) return;
        record.setBackgroundResource(R.drawable.icon_record_start);
        //record.setImageResource(R.drawable.icon_record_start);
        record.setEnabled(true);
    }

    private void recordDisabled(){
        if(null == record) return;
        record.setBackgroundResource(R.drawable.icon_record_start);
        //record.setImageResource(R.drawable.icon_record_start);
        record.setEnabled(false);
    }

    private void pauseEnabled(){
        if(null == record) return;
        record.setBackgroundResource(R.drawable.icon_record_pause);
        //record.setImageResource(R.drawable.icon_record_pause);
        record.setEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                Log.d(TAG,"grantResults.length:"+grantResults.length+",grantResults[0]:"+grantResults[0]+",grantResults[1]:"+grantResults[1]);
                if (grantResults.length!=0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG,"onRequestPermissionsResult");
                    recordUiUpdate();
                } else {
                    finish();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void recordUiUpdate(){
        if(mRecordBinder==null){
            Log.d(TAG,"recordUiUpdate mRecordBinder null");
            //hidenWave();
            //recordEnabled();
            //showMarkAndList();
            return;
        }
        Log.d(TAG,"recordUiUpdate mRecordBinder.getRecordState():"+mRecordBinder.getRecordState());
        if(mRecordBinder.getRecordState()==RecordStatus.RECORD_RESUME
                ||mRecordBinder.getRecordState()==RecordStatus.RECORDING){
            showMarkAndList();
            if(markList.size()>=MAX_MARK_NUM) {
                markDisabled();
            }else{
                markEnabled();
            }
            pauseEnabled();
            if(hanlderthisTimeValue>=MIN_RECORD_PERIOD) {
                finishEnabled();
            }else{
                finishDisabled();
            }
        }else if(mRecordBinder.getRecordState()==RecordStatus.RECORD_PAUSE){
            showMarkAndList();
            markDisabled();
            recordEnabled();
            if(hanlderthisTimeValue>=MIN_RECORD_PERIOD) {
                finishEnabled();
            }else{
                finishDisabled();
            }
        }else if(mRecordBinder.getRecordState()==RecordStatus.RECORD_FINISH
                ||mRecordBinder.getRecordState()==RecordStatus.RECORD_CANCEL
                ||mRecordBinder.getRecordState()==RecordStatus.IDLE){
            //hidenWave();
            showMarkAndList();
            markDisabled();
            recordEnabled();
            if(isHasRecordFile()) {
                listEnabled();
            }else{
                finishDisabled();
            }
        }else{
            //hidenWave();
            //recordEnabled();
            //showMarkAndList();
        }
    }

    private void recordUiUpdateForConnectService(){
        if(mRecordBinder==null){
            return;
        }
        //hidenWave();
        showMarkAndList();
        markDisabled();
        recordEnabled();
        if(isHasRecordFile()) {
            listEnabled();
        }else{
            finishDisabled();
        }
    }

    private boolean isHasRecordFile(){
        Cursor recordCursor = databaseUtil.queryRecordFile(GlobalUtil.getRecordDirPath());
        Log.d(TAG,"record count:"+databaseUtil.queryRecordFile(GlobalUtil.getRecordDirPath()).getCount());
        return ((null != recordCursor) && (recordCursor.getCount()>=1));
    }

    @Override
    public void onError(int error) {
        switch(error){
            case RecordError.INTERNAL_ERROR:
                showRecordInfoDialog(mContext.getResources().getString(R.string.record_notice),mContext.getResources().getString(R.string.record_channals_is_used));
                break;
        }
    }

    public static String FormatTimeToStr(long miss){
        long mis = miss/1000;
        String mm = mis/60>9?mis/60+"":"0"+mis/60;
        String ss = mis%60>9?mis%60+"":"0"+mis%60;
        return mm+":"+ss;
    }


}
