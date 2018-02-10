package cn.protruly.spiderman.transmitservice;

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.util.HashMap;

public class TransmitLogService extends IntentService {

    private static final String TAG = "SpiderMan";
    private HashMap<String, String> map;
    private String url;
    private File file;
    private String transmitDirPath;

    public TransmitLogService() {
        super("TransmitLogService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        url = "http://61.147.171.31/logreceiver/uploadErrorLog";
        PackageManager pm = getApplicationContext().getPackageManager();
        String clientVersion = null;
        String httpVersion = "HTTP/1.0";

        try {
            PackageInfo clientInfo = pm.getPackageInfo(getApplicationContext().getPackageName(), 0);
            clientVersion = clientInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        map = new HashMap();
        map.put("model", Build.MODEL);
        map.put("clientVersion", clientVersion);
        map.put("protocolVersion", httpVersion);

        transmitDirPath = "/data/data/" + getApplicationContext().getPackageName() + "/spiderman/";


    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent != null) {

            String action = intent.getAction();
            String tag = intent.getStringExtra("tag");
            String msg = intent.getStringExtra("msg");
            if (action.equals("cn.proturly.transmitlog")) {
                Log.v(TAG, "I'm " + tag + ", " + msg);
                File logZipFiles = new File(transmitDirPath);
                if (!logZipFiles.exists()) {
                    return;
                }
                File[] logZipFile = logZipFiles.listFiles();
                if (logZipFile.length == 0) {
                    Log.v(TAG, "log文件夹下无文件");
                    return;
                }
                for (int i = 0; i < logZipFile.length; i++) {
                    if (i < 40) {
                        if (logZipFile[i].getName().contains(".zip")) {
                            file = new File(logZipFile[i].getAbsolutePath());
                            new HttpConnectionUtil(url, file, map).sendPostUpFile();
                        } else {
                            return;
                        }
                    } else {
                        break;
                    }

                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
