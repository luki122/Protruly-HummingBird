package com.protruly.music.downloadex;


import java.util.List;
import com.protruly.music.util.LogUtil;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;


public class DownloadService extends Service{
	private static final String TAG = "DownloadService";
	private static DownloadManager mDownloadManager;

    public static DownloadManager getDownloadManager(Context appContext) {
        if (!DownloadService.isServiceRunning(appContext)) {
            Intent downloadSvr = new Intent("download.service.action");
            appContext.startService(downloadSvr);
        }
        
        if (DownloadService.mDownloadManager == null) {
            DownloadService.mDownloadManager = new DownloadManager(appContext);
        }
        
        return mDownloadManager;
    }

    public DownloadService() {
        super();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy() {
        if (mDownloadManager != null) {
            try {
            	mDownloadManager.removeAllDownload();
            	mDownloadManager.backupDownloadInfoList();
            } catch (Exception e) {
            	LogUtil.iv(TAG, "DownloadService onDestroy fail e:"+e.getMessage());
            }
        }
        super.onDestroy();
    }

    public static boolean isServiceRunning(Context context) {
        boolean isRunning = false;

        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList
                = activityManager.getRunningServices(Integer.MAX_VALUE);

        if (serviceList == null || serviceList.size() == 0) {
            return false;
        }

        for (int i = 0; i < serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(DownloadService.class.getName())) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }

}
