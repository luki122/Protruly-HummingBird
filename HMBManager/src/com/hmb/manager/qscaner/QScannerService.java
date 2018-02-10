package com.hmb.manager.qscaner;

import java.util.List;

import com.hmb.manager.qscaner.bean.RiskEntity;
import com.hmb.manager.qscaner.provider.QScannerRiskProvider;

import android.app.Service;
import android.content.Intent;
import android.content.pm.LauncherApps.Callback;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import tmsdk.common.module.qscanner.QScanConfig;
import tmsdk.common.module.qscanner.QScanListener;
import tmsdk.common.module.qscanner.QScanResultEntity;
import tmsdk.common.module.qscanner.QScannerManagerV2;
import tmsdk.fg.creator.ManagerCreatorF;

public class QScannerService extends Service {

    protected static final String TAG = "QScannerService";

    private boolean connecting = false;
    private int riskSum = -1;

    private Callback callback;

    private QScannerManagerV2 mQScannerManager;
    private Handler mMainJobHandler = null;
    private HandlerThread mMainJobThread = null;

    private static final int MSG_FREE = 0x1000;
    private static final int MSG_START_SCAN_INSTALLED_PACKAGE = 0x1001;
    private static final int MSG_START_SCAN_UNINSTALL_PACKAGE = 0x1002;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "on Bind, intent is : " + intent.toString());
        return new QScannerBinder();
    }

    public class QScannerBinder extends Binder {
        public QScannerService getService() {
            return QScannerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 执行安全防护的扫描过程
        connecting = true;

        new Thread(new Runnable() {

            @Override
            public void run() {
                mQScannerManager = ManagerCreatorF.getManager(QScannerManagerV2.class);
                Log.v(TAG,
                        "initData() -> VirusBaseVersion = "
                                + mQScannerManager.getVirusBaseVersion());
                mMainJobThread = new HandlerThread("qscan");
                mMainJobThread.start();
                mMainJobHandler = new MainJobHandler(mMainJobThread.getLooper());
                mMainJobHandler.sendEmptyMessage(MSG_START_SCAN_INSTALLED_PACKAGE);

            }
        }).start();

    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    /**
     * 回调接口
     * 
     * @author sunguofeng
     * 
     */
    public static interface Callback {
        void onDataChange(String data);
    }

    @Override
    public void onDestroy() {
        mMainJobHandler.sendEmptyMessage(MSG_FREE);
        connecting = false;
        Log.d(TAG, "QScanner Service onDestory.");

        super.onDestroy();
    }

    private class MainJobHandler extends Handler {

        public MainJobHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.e(TAG, "MainJobHandler() -> msg.what = " + msg.what);

            if (msg.what == MSG_START_SCAN_INSTALLED_PACKAGE) {
                int nRet = mQScannerManager.initScanner();
                if (nRet != QScanConfig.S_OK) {
                    Log.e(TAG, "initScanner() -> error = " + nRet);
                    if (callback != null) {
                        callback.onDataChange(String.valueOf(riskSum));
                    }
                    return;
                }

                mQScannerManager.scanInstalledPackages(QScanConfig.SCAN_LOCAL
                        | QScanConfig.SCAN_CLOUD, null, new QScanListenerUI(
                        RiskEntity.RISK_TYPE_APP), QScanConfig.ERT_FAST, 0);

            } else if (msg.what == MSG_START_SCAN_UNINSTALL_PACKAGE) {
                mQScannerManager.scanUninstallApks(QScanConfig.SCAN_LOCAL | QScanConfig.SCAN_CLOUD,
                        null, new QScanListenerUI(RiskEntity.RISK_TYPE_APK), 0);

            } else if (msg.what == MSG_FREE) {
                mQScannerManager.cancelScan();
                mQScannerManager.freeScanner();
                mMainJobThread.quit();
            }
        }
    }

    private class QScanListenerUI extends QScanListener {
        private int mScanMode;

        public QScanListenerUI(int scanMode) {
            mScanMode = scanMode;
        }

        public void onScanStarted(int scanType) {
            Log.v(TAG, "onScanStarted() -> scanType = " + scanType);
        }

        public void onScanProgress(int scanType, int curr, int total, QScanResultEntity result) {
            Log.v(TAG, "onScanProgress() -> scanType = " + scanType + ", curr = " + curr
                    + ", total = " + total + ", packageName = " + result.packageName
                    + ", softName = " + result.softName);
        }

        public void onScanError(int scanType, int errCode) {
            Log.v(TAG, "onScanError() -> scanType = " + scanType + ", errCode = " + errCode);
        }

        public void onScanPaused(int scanType) {
            Log.v(TAG, "onScanPaused() -> scanType = " + scanType);
        }

        public void onScanContinue(int scanType) {
            Log.v(TAG, "onScanContinue, scanType:[" + scanType + "]");
        }

        public void onScanCanceled(int scanType) {
            Log.v(TAG, "onScanCanceled() -> scanType = " + scanType);
        }

        public void onScanFinished(int scanType, List<QScanResultEntity> results) {
            Log.v(TAG, "onScanFinished() -> scanType = " + scanType + ", scanMode = " + mScanMode);
            riskSum = 0;

            for (QScanResultEntity entity : results) {
                if (entity.scanResult == QScanConfig.RET_SAFE) {
                    continue;
                }

                Log.v(TAG, "onScanFinished() -> " + ", packageName = " + entity.packageName
                        + ", softName = " + entity.softName + ", version = " + entity.version
                        + ", versionCode = " + entity.versionCode + ", path = " + entity.path
                        + ", scanResult = " + entity.scanResult + ", description = "
                        + entity.virusDiscription);

                if (entity.scanResult == QScanConfig.RET_VIRUSES
                        || entity.scanResult == QScanConfig.RET_PAY_RISKS
                        || entity.scanResult == QScanConfig.RET_STEALACCOUNT_RISKS
                        || entity.scanResult == QScanConfig.RET_OTHER_RISKS
                        || entity.scanResult == QScanConfig.RET_NOT_OFFICIAL
                        || entity.plugins.size() > 0) {
                    riskSum++;
                }
            }

            if (mScanMode == RiskEntity.RISK_TYPE_APP) {
                mMainJobHandler.sendEmptyMessage(MSG_START_SCAN_UNINSTALL_PACKAGE);
            } else {
                Log.e(TAG, "scanner result riskSum = " + riskSum);
                if (callback != null) {
                    callback.onDataChange(String.valueOf(riskSum));
                }
            }
        }
    }

}
