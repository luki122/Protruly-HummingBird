package com.hmb.upload.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.hmb.upload.AccountUtils;
import com.hmb.upload.LogCollectRequest;
import com.hmb.upload.SecurityUtil;
import com.hmb.upload.ToolUtils;
import com.hmb.upload.net.HttpRequest;
import com.hmb.upload.net.HttpUtil;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * 激活时间上传条件：1.开机超过10分钟; 2.开机联网超过5分钟;
 *
 *
 *
 * 上传手机信息
 */
public class UploadService extends IntentService {
    private static final String TAG = "UploadService";
    /**
     * 取得首次激活时间
     */
    private static final String UPLOAD_TAG = "com.hmb.upload.action";
    /**
     * 上传失败最多上传两次
     */
    private static final int CONNECT_MAX_COUNTS = 2;
    /**
     * 开机联网超过10分钟
     */
    private static final long CONNECTED_TIME_MIN = 10 * 60 * 1000;
    /**
     * 开机超过20分钟
     */
    private static final long ON_TIME_MIN = 20 * 60 * 1000;
    /**
     * 上传标志
     */
    private static final int UPLOAD_TIME = 1000;
    /**
     * 手机首次激活时间key
     */
    private final String mActiveTimeKey = "active_time_key";
    /**
     * 激活时间上传状态key
     */
    private final String mActiveTimeUploadKey = "active_time_upload_key";
    public static String mUploadOkKey = "upload_ok_key";
    private int mConnectTime = 0;
    private long mTimeCount;
    private boolean mFirstActive;


    public UploadService() {
        super(TAG);
    }


    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPLOAD_TIME:
                    final String result = (String)msg.obj;
                    new AsyncTask<Void, Void, Void>(){
                        @Override
                        protected Void doInBackground(Void... params) {
                            upload(result);
                            return null;
                        }
                    }.execute();
                    break;
            }
        }
    };

    private Runnable TimerRunnable = new Runnable() {

        @Override
        public void run() {
            boolean netState = HttpUtil.isNetworkAvailable(UploadService.this);
            if (mTimeCount >= CONNECTED_TIME_MIN && netState) {
                //获得首次激活时间
                PreferenceManager.getDefaultSharedPreferences(UploadService.this).edit().putLong(mActiveTimeUploadKey, System.currentTimeMillis()).commit();
                //网络连接超5分钟
                PreferenceManager.getDefaultSharedPreferences(UploadService.this).edit().putLong(mActiveTimeKey, System.currentTimeMillis()).commit();
                setUPload();
                mHandler.removeCallbacks(TimerRunnable);
                mFirstActive = false;
                Log.e(TAG, "网络连接超……" + mTimeCount);
                return;
            } else if (!netState){
                mTimeCount = 0;
            }
            long rootTime = SystemClock.elapsedRealtime();
            if (rootTime >= ON_TIME_MIN) {
                mFirstActive = false;
                //获得首次激活时间
                PreferenceManager.getDefaultSharedPreferences(UploadService.this).edit().putLong(mActiveTimeUploadKey, System.currentTimeMillis()).commit();

                //开机超过10分钟
                PreferenceManager.getDefaultSharedPreferences(UploadService.this).edit().putLong(mActiveTimeKey, System.currentTimeMillis()).commit();
                mHandler.removeCallbacks(TimerRunnable);
                Log.e(TAG, "开机超过20分钟……" + rootTime);
                if (netState) {
                    //开始上传
                    setUPload();
                }
                return;
            }
            countTimer();
        }
    };

    private void countTimer(){
        mTimeCount = mTimeCount + 1000;
        mHandler.postDelayed(TimerRunnable, 1000);
    }


    @Override
    protected synchronized void onHandleIntent(Intent intent) {
        String[] phoneIMEIs = ToolUtils.getPhoneIMEIs(UploadService.this);
        Log.v(TAG, ">>>phoneIMEIs>>>" + Arrays.toString(phoneIMEIs));
        if(TextUtils.isEmpty(phoneIMEIs[0])) {
            return;
        }
        if (phoneIMEIs.length > 1 && TextUtils.isEmpty(phoneIMEIs[1])) {
            return;
        }
        if (intent != null) {
            String tag = intent.getStringExtra("alarm");
            Log.v(TAG, "alarm>>>>>" + tag);
            try {
                //获得首次激活时间的状态
                long activeTimeState = PreferenceManager.getDefaultSharedPreferences(UploadService.this).getLong(mActiveTimeUploadKey, 0);
                if (TextUtils.equals(UPLOAD_TAG, intent.getAction()) || activeTimeState == 0) {
                    if (TextUtils.equals(UPLOAD_TAG, intent.getAction())) {
                        //设置上传时间
                        setUploadTime(UploadService.this);
                    }
                    mFirstActive = true;
                    Log.e(TAG, "获得首次激活时间的状态……" + activeTimeState);
                }
                if (mFirstActive) {
                    //上传首次激活时间
                    countTimer();
                    return;
                }
                long uploadOk = PreferenceManager.getDefaultSharedPreferences(this).getLong(mUploadOkKey, 0);
                if (uploadOk == 0) {
                    if (TextUtils.isEmpty(HttpUtil.getNetWorkType(this))) {
                        //没有网络不上传
                        return;
                    }
                    if (uploadOk > 0) {
                        return;
                    }
                    setUploadParams();
                    Log.e(TAG, "开始上传……");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mConnectTime = 0;
            }

        }
    }

    /**
     * 上传数据信息
     */
    private void upload(String parms) {
        String netType = ToolUtils.getNetWorkType(this);
        if (ToolUtils.isRoaming(this) && !TextUtils.equals(netType, ToolUtils.NET_TYPE_WIFI)) {
            //判断是否在漫游且非wifi情况下
            return;
        }
        boolean uploadState = false;
        try{
            HttpRequest http = HttpRequest.post(this,HttpRequest.mUrl, parms);
            if (http != null && http.mInStream != null) {
                byte[] res = HttpUtil.convertStreamToByteArray(http.mInStream);
                String result = SecurityUtil.decrypt(new String(res));
                Log.e(TAG , "http--result>>>" + result);
                JSONObject json = new JSONObject(result);
                String retCode = json.optString("retCode");
                if (TextUtils.equals(retCode, "0")) {
                    PreferenceManager.getDefaultSharedPreferences(this).edit().putLong(mUploadOkKey, System.currentTimeMillis()).commit();
                    uploadState = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (!uploadState) {
                mConnectTime ++;
                if (mConnectTime < CONNECT_MAX_COUNTS) {
                    try {
                        Thread.sleep(3000);
                        upload(parms);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    return;
                }
            }
        }
    }

    private void setUPload() {
        setUploadParams();
    }

    /**
     * 设置上传时间
     * @param context
     */
    private void setUploadTime(Context context) {
        AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(ToolUtils.getIntent(context));
        ToolUtils.setAlarm(context, alarm, false);
    }

    /**
     * 上传信息
     * @return
     */
    private void setUploadParams() {
        final LogCollectRequest collectRequest = new LogCollectRequest();
        //协议版本
        collectRequest.setProtocalVer(ToolUtils.PROTOCAL_VER);
        //客户端版本
        collectRequest.setClientVer(ToolUtils.getVersion(this));
        //机型
        collectRequest.setModel("" + android.os.Build.MODEL);
        //系统版本
        collectRequest.setRomVersion("" + android.os.Build.DISPLAY);
        //安卓版本号
        collectRequest.setAndroidVersionCode(android.os.Build.VERSION.SDK_INT);
        //设备IMEI1
        String[] imeis = ToolUtils.getPhoneIMEIs(this);
        collectRequest.setImei1(imeis[0]);
        collectRequest.setImei2(imeis[1]);
        //首次激活时间（激活条件：设备联网超过十分钟之后）格式：yyyy-MM-dd HH:mm:ss
        long activeTime = PreferenceManager.getDefaultSharedPreferences(this).getLong(mActiveTimeKey, 0);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String activationTime = sdf.format(new Date(activeTime));
        collectRequest.setActivationTime(activationTime);
        //SIM1 卡号  主卡
        String simImsiHost = ToolUtils.getActiveSimImsi(this);
        collectRequest.setSim1(simImsiHost);
        //SIM2 卡号  副卡
        String simImsi2 = null;
        String[] imsis = ToolUtils.getIMSI(this);
        for (String imsi : imsis) {
            if (!TextUtils.equals(imsi, simImsiHost)) {
                simImsi2 = imsi;
                collectRequest.setSim2(imsi);
            }
        }
        //主卡 运营商类型（移动联通电信等）
        collectRequest.setOperator1(ToolUtils.getSimOperator(this, simImsiHost));
        //副卡 运营商类型
        collectRequest.setOperator2(ToolUtils.getSimOperator(this, simImsi2));
        //设备mac地址
        collectRequest.setMac(ToolUtils.getMacAddress());
        //设备sn号
        collectRequest.setSn(android.os.Build.SERIAL);
        //芯片平台
        collectRequest.setPlatform(android.os.Build.HARDWARE);
        final Gson gson = new Gson();
        //账号ID: 千里云ID
        boolean loginState = AccountUtils.getIntance().getLoginState(this);
        if (loginState) {
            AccountUtils.getIntance().setAccountCallBack(new AccountUtils.IAccountUIDCallBack() {
                @Override
                public void getAccountInfo(Bundle bundle) {
                    String uid = bundle.getString("GET_UID");
                    collectRequest.setAccountName(uid);
                    String result = SecurityUtil.encrypt(gson.toJson(collectRequest));
                    Message msg = mHandler.obtainMessage();
                    msg.obj = result;
                    msg.what = UPLOAD_TIME;
                    mHandler.sendMessage(msg);
                    Log.e(TAG, "toJson>GET_UID>>>" + gson.toJson(collectRequest));
                }
            });
            AccountUtils.getIntance().getAccountID(this);
        } else {
            collectRequest.setAccountName("");
            String result = SecurityUtil.encrypt(gson.toJson(collectRequest));
            Message msg = mHandler.obtainMessage();
            msg.obj = result;
            msg.what = UPLOAD_TIME;
            mHandler.sendMessage(msg);
            Log.e(TAG, "toJson>>>>" + gson.toJson(collectRequest));
        }
    }
}
