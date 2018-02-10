package cn.protruly.spiderman.uploadnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.protruly.clouddata.appdata.AppDataAgent;

import cn.protruly.spiderman.collectservice.CollectLogService;
import cn.protruly.spiderman.collectservice.SystemPropertiesProxy;
import cn.protruly.spiderman.transmitservice.MonitoringLogService;
import cn.protruly.spiderman.transmitservice.NetUtils;
import cn.protruly.spiderman.transmitservice.TransmitLogService;

public class AcceptNotification extends BroadcastReceiver {

    private static final String TAG = "SpiderMan";
    private static final String spiderman = "cn.proturly.spiderman";
    private static final String transmitlog = "cn.proturly.transmitlog";
    private static final String acceptbugreport = "android.intent.action.PROTRULYBUGREPORT";
    private String action;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        if (!Build.TYPE.equals("user")) {
            SystemPropertiesProxy.set(context, "persist.sys.bugreportswitch", "off");
            Log.v(TAG, "版本不是user版本");
        } else {
            if (intent != null) {
                action = intent.getAction();
            }

            if (context == null) {
                return;
            }

            if (action.equals(acceptbugreport)) {
                if (isSystemApp(context, intent.getStringExtra("PACKAGENAME"))) {
                    Intent bugreportIntent = new Intent(context, CollectLogService.class);
                    bugreportIntent.setAction(action);
                    bugreportIntent.putExtra("TYPE", intent.getStringExtra("TYPE"));
                    bugreportIntent.putExtra("PACKAGENAME", intent.getStringExtra("PACKAGENAME"));
                    bugreportIntent.putExtra("REASON", intent.getStringExtra("REASON"));
                    bugreportIntent.putExtra("EXCEPTIONSTACK", intent.getStringExtra("EXCEPTIONSTACK"));
                    bugreportIntent.putExtra("HEADERS", intent.getStringExtra("HEADERS"));
                    bugreportIntent.putExtra("FILEPATH", intent.getStringExtra("FILEPATH"));
                    context.startService(bugreportIntent);
                }

            } else if (action.equals(spiderman)) {
                Intent collectLogIntent = new Intent(context, CollectLogService.class);
                collectLogIntent.setAction(action);
                collectLogIntent.putExtra("tag", intent.getStringExtra("tag"));
                collectLogIntent.putExtra("msg", intent.getStringExtra("msg"));
                context.startService(collectLogIntent);
            } else if (action.equals(transmitlog)) {
                if (NetUtils.isWiFiActive(context)) {
                    Intent transmitLogIntent = new Intent(context, TransmitLogService.class);
                    transmitLogIntent.setAction(action);
                    transmitLogIntent.putExtra("tag", intent.getStringExtra("tag"));
                    transmitLogIntent.putExtra("msg", intent.getStringExtra("msg"));
                    context.startService(transmitLogIntent);
                } else {
                    Toast.makeText(context, "无可用WiFi, log上传失败", Toast.LENGTH_SHORT).show();
                }
            } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                // wifi开关打开与否
                Intent MonitoringLogIntent = new Intent(context, MonitoringLogService.class);
                MonitoringLogIntent.setAction("cn.proturly.transmitlog");
                MonitoringLogIntent.putExtra("tag", "MonitoringLogService");
                MonitoringLogIntent.putExtra("msg", "接受WiFi自动链接, 自动开启上传");
                int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
                if (wifistate == WifiManager.WIFI_STATE_ENABLED) {
                    Log.v(TAG, "系统开启wifi,开始监控WiFi服务");
                    context.startService(MonitoringLogIntent);
                } else if (wifistate == WifiManager.WIFI_STATE_DISABLED) {
                    Log.v(TAG, "系统关闭wifi,停止监控WiFi服务");
                    context.stopService(MonitoringLogIntent);
                }
            }

        }
    }

    private boolean isSystemApp(Context context, String pg) {

        PackageManager pm = context.getPackageManager();

        try {
            PackageInfo pInfo = pm.getPackageInfo(pg, 0);

            if (AppDataAgent.getConfigParams(context, "App-3rd-Switch").equals("off") &&
                    AppDataAgent.getConfigParams(context, "Model").equals(SystemPropertiesProxy.get(context, "ro.product.model"))) {
                Log.v(TAG, "应用不是系统apk, 只上报系统预置apk的错误日志");
                return ((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) ||
                        ((pInfo.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0);
            } else {
                Log.v(TAG, "上报所有apk的错误日志");
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return true;
        }
    }

}
