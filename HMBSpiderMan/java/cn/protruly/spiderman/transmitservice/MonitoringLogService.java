package cn.protruly.spiderman.transmitservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.protruly.clouddata.appdata.AppDataAgent;

import java.util.Timer;
import java.util.TimerTask;

import cn.protruly.spiderman.collectservice.SystemPropertiesProxy;

/**
 * Created by lijia on 17-5-19.
 */

public class MonitoringLogService extends Service {

    private static final String TAG = "SpiderMan";
    private Timer timerUpdateUpload = new Timer();
    private TimerTask taskUpdateUpload;
    private Handler handlerUpdateUpload;
    private Intent transmitLogIntent;

    private BroadcastReceiver mNetReceiver;
    private Context mContext;

    @Override
    public void onCreate() {

        super.onCreate();

        mContext = getApplicationContext();


        if (SharedPrefsUtil.getValue(mContext, "FirstTime", (long) 1) != 2) {
            SharedPrefsUtil.putValue(mContext, "FirstTime", (long) 1);
        }


        mNetReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();

                if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    Log.v(TAG, action);
                    if (NetUtils.isWiFiActive(mContext)) {
                        if (transmitLogIntent != null) {
                            startService(transmitLogIntent);
                        }
                    } else if (NetUtils.isMobileOnline(mContext)) {
                    }
                }
            }
        };

        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetReceiver, mFilter);

        startUpdateUpload();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            String tag = intent.getStringExtra("tag");
            String msg = intent.getStringExtra("msg");
            transmitLogIntent = new Intent(getApplicationContext(), TransmitLogService.class);
            transmitLogIntent.setAction("cn.proturly.transmitlog");
            transmitLogIntent.putExtra("tag", tag);
            transmitLogIntent.putExtra("msg", msg);
        }
        return START_STICKY;
    }

    private void startUpdateUpload() {

        taskUpdateUpload = new TimerTask() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                Message message = new Message();
                message.what = 1;
                handlerUpdateUpload.sendMessage(message);
            }
        };

        timerUpdateUpload.schedule(taskUpdateUpload, 1000 * 60 * 60 * 3, 1000 * 60 * 60 * 3);

        handlerUpdateUpload = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // TODO Auto-generated method stub
                super.handleMessage(msg);
                if (transmitLogIntent != null && NetUtils.isWiFiActive(mContext)) {
                    startService(transmitLogIntent);
                    updateOnlineConfig(mContext);
                } else if (NetUtils.isMobileOnline(mContext)) {
                    updateOnlineConfig(mContext);
                }

                if (SystemPropertiesProxy.get(mContext, "persist.logd.logpersistd").equals("logcatd")) {
                    SystemPropertiesProxy.set(mContext, "persist.logd.logpersistd", "null");
                }
            }
        };

    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mNetReceiver != null) {
            unregisterReceiver(mNetReceiver);
        }
        if (taskUpdateUpload != null) {
            taskUpdateUpload.cancel();
        }

    }

    private void updateOnlineConfig(Context context) {

        if (context == null) {
            return;
        }

        if (SharedPrefsUtil.getValue(context, "FirstTime", (long) 1) == 1) {
            SharedPrefsUtil.putValue(context, "FirstTime", (long) 2);
            SharedPrefsUtil.putValue(context, "LastTime", System.currentTimeMillis());
            Log.v(TAG, "第一次初始化, 储存第一上传的时间");
            AppDataAgent.updateOnlineConfig(context);
        } else {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - SharedPrefsUtil.getValue(context, "LastTime", (long) 0)) /
                    (1000 * 60 * 60) >= (24 * 7)) {
                Log.v(TAG, "两次请求的网络时间差为:" + (currentTime - SharedPrefsUtil.getValue(context, "LastTime", (long) 0)) /
                        (1000 * 60 * 60 * 24) + "天");
                SharedPrefsUtil.putValue(context, "LastTime", currentTime);
                AppDataAgent.updateOnlineConfig(context);
                if (AppDataAgent.getConfigParams(context, "BugreportSwitch").equals("off") &&
                        AppDataAgent.getConfigParams(context, "Model").equals(
                                SystemPropertiesProxy.get(context, "ro.product.model"))) {
                    SystemPropertiesProxy.set(context, "persist.sys.bugreportswitch", "off");
                    Log.v(TAG, "关掉日志上报apk");
                } else if (!AppDataAgent.getConfigParams(context, "BugreportSwitch").equals("off") &&
                        AppDataAgent.getConfigParams(context, "Model").equals(
                                SystemPropertiesProxy.get(context, "ro.product.model"))) {
                    SystemPropertiesProxy.set(context, "persist.sys.bugreportswitch",
                            AppDataAgent.getConfigParams(context, "BugreportSwitch"));
                    Log.v(TAG, "打开日志上报apk");
                } else {
                    Log.v(TAG, "服务器上的参数不满足打开或者关闭日志上报apk的要求");
                }

            }
        }

    }

}
