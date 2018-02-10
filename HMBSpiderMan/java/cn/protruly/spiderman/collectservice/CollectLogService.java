package cn.protruly.spiderman.collectservice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.File;

import cn.protruly.spiderman.action.ActionFactory;

public class CollectLogService extends Service {

    private String TAG = "SpiderMan";
    private final int WAIT_LOGCAT_CATCH_LOG = 1;
    private final int CLOSE_LOGCAT = 2;
    private Context mContext;
    private Handler mHandler;
    private Runnable mRunnablePause;
    private Runnable mRunnableSetLogcat;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        mContext = getApplicationContext();

        SystemPropertiesProxy.set(mContext, "persist.logd.logpersistd", "logcatd");

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case WAIT_LOGCAT_CATCH_LOG:
                        String zipLogPath = "/data/data/" + mContext.getPackageName() + "/spiderman/";
                        File zipLogFile = new File(zipLogPath);

                        if (!zipLogFile.exists()) {
                            zipLogFile.mkdir();
                        }
                        File[] zipFile = zipLogFile.listFiles();
                        if (getTotalSizeOfFilesInDir(zipLogFile) < 200) {
                            if (zipFile.length <= 199) {
                                Log.v(TAG, "文件个数小于或者等于200个");
                            } else {
                                deleteLogFile(zipFile);
                            }
                            if (intent != null) {
                                new ActionFactory().creator(intent, mContext);
                            }
                        } else {
                            Log.v(TAG, "文件夹下的文件大小超过200MB，： " + getTotalSizeOfFilesInDir(zipLogFile) + " MB");
                            for (int i = 0; i < zipFile.length / 2; i++) {
                                if (!zipFile[i].isDirectory()) {
                                    zipFile[i].delete();
                                }
                            }
                        }
                        break;
                    case CLOSE_LOGCAT:
                        if (SystemPropertiesProxy.get(mContext, "persist.logd.logpersistd").equals("logcatd")) {
                            SystemPropertiesProxy.set(mContext, "persist.logd.logpersistd", "null");
                        }
                        break;
                    default:
                        break;
                }

            }
        };

        mRunnablePause = new Runnable() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(WAIT_LOGCAT_CATCH_LOG);
            }
        };

        mRunnableSetLogcat = new Runnable() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(CLOSE_LOGCAT);
            }
        };

        mHandler.postDelayed(mRunnablePause, 1000 * 2);
        mHandler.postDelayed(mRunnableSetLogcat, 1000 * 60 * 3);

        return START_STICKY;

    }

    private void deleteLogFile(File[] fl) {

        for (int i = 0; i < fl.length; i++) {
            if (i <= (fl.length - 200)) {
                if (!fl[i].isDirectory()) {
                    fl[i].delete();
                    Log.v(TAG, "删除文件的名称： " + fl[i].getName());
                }
            }
        }

    }

    private long getTotalSizeOfFilesInDir(File file) {
        if (file.isFile())
            return file.length();
        final File[] children = file.listFiles();
        long total = 0;
        if (children != null)
            for (final File child : children)
                total += getTotalSizeOfFilesInDir(child);
        return total / 1024 / 1024;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (SystemPropertiesProxy.get(mContext, "persist.logd.logpersistd").equals("logcatd")) {
            SystemPropertiesProxy.set(mContext, "persist.logd.logpersistd", "null");
        }
    }
}
