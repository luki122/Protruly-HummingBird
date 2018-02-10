package com.hb.thememanager.http.downloader;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.hb.thememanager.http.downloader.DownloadManager;
import com.hb.thememanager.http.downloader.config.DownloadConfig;
import com.hb.thememanager.utils.CommonUtil;

import java.util.List;


public class DownloadService extends Service {

  private static final String TAG = "DownloadService";
  public static DownloadManager downloadManager;
  private NetworkChangeReceiver mNetworkChangeReceiver;
  public static DownloadManager getDownloadManager(Context context) {
    return getDownloadManager(context, null);
  }

  public static DownloadManager getDownloadManager(Context context, DownloadConfig downloadConfig) {
    if (!isServiceRunning(context)) {
      Intent downloadSvr = new Intent(context.getApplicationContext(), DownloadService.class);
      context.startService(downloadSvr);
    }
    if (DownloadService.downloadManager == null) {
      DownloadService.downloadManager = DownloadManagerImpl.getInstance(context, downloadConfig);
    }
    return downloadManager;
  }

  private static boolean isServiceRunning(Context context) {
    boolean isRunning = false;
    ActivityManager activityManager = (ActivityManager) context
        .getSystemService(Context.ACTIVITY_SERVICE);
    List<RunningServiceInfo> serviceList = activityManager
        .getRunningServices(Integer.MAX_VALUE);

    if (serviceList == null || serviceList.size() == 0) {
      return false;
    }

    for (int i = 0; i < serviceList.size(); i++) {
      if (serviceList.get(i).service.getClassName().equals(
          DownloadService.class.getName())) {
        isRunning = true;
        break;
      }
    }
    return isRunning;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    IntentFilter filter = new IntentFilter();
    filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
    mNetworkChangeReceiver = new NetworkChangeReceiver();
    registerReceiver(mNetworkChangeReceiver,filter);
    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onDestroy() {
    if (downloadManager != null) {
      downloadManager.onDestroy();
      downloadManager = null;
    }
    unregisterReceiver(mNetworkChangeReceiver);
    super.onDestroy();
  }


  class NetworkChangeReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
      if("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())){
        boolean connected = CommonUtil.hasNetwork(context);
        if(downloadManager != null) {
          List<DownloadInfo> allDownloading = downloadManager.findAllDownloading();
          if(allDownloading == null || allDownloading.size() == 0){
            return;
          }
          if (connected) {
            for(DownloadInfo info : allDownloading){
              downloadManager.resume(info);
            }

          } else {
            for(DownloadInfo info : allDownloading){
              downloadManager.pause(info);
            }
          }
        }
      }

    }



  }







}
