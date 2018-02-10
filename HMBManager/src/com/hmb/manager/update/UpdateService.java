package com.hmb.manager.update;


import android.app.IntentService;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.hmb.manager.Constant;
import com.hmb.manager.utils.SPUtils;

import tmsdk.common.module.update.CheckResult;
import tmsdk.common.module.update.ICheckListener;
import tmsdk.common.module.update.IUpdateListener;
import tmsdk.common.module.update.UpdateConfig;
import tmsdk.common.module.update.UpdateInfo;
import tmsdk.common.module.update.UpdateManager;

public class UpdateService extends IntentService {
    private static final String TAG = "UpdateService";

    /**
     * Request actions
     */
    public static final String ACTION_UPDATE = "com.hmb.manager.action.UPDATE";

    /**
     * Intent extra
     */
    public static final String EXTRA_UPDATE_ACTION = "update_action";

    /**
     * Update Action
     */
    private static int mUpdateAction = HMBUpdateManager.UPDATE_ACTION_AUTO;

    /**
     * Check Result
     */
    private static CheckResult mCheckResults;

    private UpdateManager tmsUpdateManager;
    private HMBUpdateManager hmbUpdateManager;

    public UpdateService() {
        super(TAG);
        Log.d(TAG, "UpdateService()");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null || !intent.getAction().equals(ACTION_UPDATE)) {
            stopSelf();
            return;
        }

        hmbUpdateManager = HMBUpdateManager.getInstance(this);
        tmsUpdateManager = hmbUpdateManager.getUpdateManager();
        mUpdateAction = intent.getIntExtra(EXTRA_UPDATE_ACTION, HMBUpdateManager.UPDATE_ACTION_AUTO);
        Log.d(TAG, "onHandleIntent() -> mUpdateAction = " + mUpdateAction);
        if (mUpdateAction == HMBUpdateManager.UPDATE_ACTION_UPDATE) {
            update();
        } else {
            check();
        }
    }

    private void check() {
        Log.d(TAG, "check()");
        SPUtils.instance(this).setLongValue(Constant.SHARED_PREFERENCES_LAST_CHECK_UPDATE_TIME, 0,
                System.currentTimeMillis());
        long flags = UpdateConfig.UPDATA_FLAG_NUM_MARK// 号码标记模块
                | UpdateConfig.UPDATE_FLAG_SYSTEM_SCAN_CONFIG//病毒扫描模块
                | UpdateConfig.UPDATE_FLAG_ADB_DES_LIST//病毒扫描模块
                | UpdateConfig.UPDATE_FLAG_VIRUS_BASE//病毒扫描模块
                | UpdateConfig.UPDATE_FLAG_VIRUSKILLER_CLOUDSCAN_WHITE//病毒扫描模块
                | UpdateConfig.UPDATE_FLAG_PAY_LIST//病毒扫描模块
                // | UpdateConfig.UPDATE_FLAG_TRAFFIC_MONITOR_CONFIG//流量监控
                | UpdateConfig.UPDATE_FLAG_LOCATION// 归属地模块
                // | UpdateConfig.UPDATE_FLAG_PROCESSMANAGER_WHITE_LIST// 瘦身大文件模块
                // | UpdateConfig.UPDATE_FLAG_WeixinTrashCleanNew//瘦身微信
                | UpdateConfig.UPDATE_FLAG_POSEIDONV2//智能拦截
                ;
        tmsUpdateManager.check(flags, new ICheckListener() {
            @Override
            //检查网络，如果网络失败则回调
            public void onCheckEvent(int arg0) {
                //检查网络状态，如果网络失败则不能更新
                onNetworkError();
            }

            @Override
            public void onCheckStarted() {

            }

            @Override
            public void onCheckCanceled() {

            }

            @Override
            public void onCheckFinished(CheckResult result) {
                mCheckResults = result;
                if (result != null && result.mUpdateInfoList != null
                        && result.mUpdateInfoList.size() > 0) {
                    for (UpdateInfo info : result.mUpdateInfoList) {
                        Log.v(TAG, "onCheckFinished() -> fileName = " + info.fileName
                                + ", url = " + info.url + ", fileSize = " + info.fileSize);
                    }
                    onGetUpdate();
                } else {
                    onNoUpdate();
                }
                if (mUpdateAction == HMBUpdateManager.UPDATE_ACTION_AUTO) {
                    update();
                }
            }
        }, -1);
    }

    private void update() {
        Log.d(TAG, "update()");
        if (mCheckResults != null && mCheckResults.mUpdateInfoList != null
                && mCheckResults.mUpdateInfoList.size() > 0) {
            tmsUpdateManager.update(mCheckResults.mUpdateInfoList, new IUpdateListener() {
                @Override
                public void onProgressChanged(UpdateInfo arg0, int arg1) {

                }
                @Override
                //更新中检查网络
                public void onUpdateEvent(UpdateInfo arg0, int arg1) {
                    onNetworkError();
                }

                @Override
                public void onUpdateFinished() {
                    onUpdateDone();
                }

                @Override
                public void onUpdateStarted() {

                }
                @SuppressWarnings("unused")
                public void onUpdateCanceled() {

                }
            });
        }
    }

    private void onNetworkError() {
        Log.d(TAG, "onNetworkError()");
        if (mUpdateAction == HMBUpdateManager.UPDATE_ACTION_AUTO) {
            return;
        }
        hmbUpdateManager.notifyActivity(HMBUpdateManager.RESULT_CODE_NETWORK_ERROR);
    }

    private void onGetUpdate() {
        Log.d(TAG, "onGetUpdate()");
        if (mUpdateAction == HMBUpdateManager.UPDATE_ACTION_AUTO) {
            return;
        }
        hmbUpdateManager.notifyActivity(HMBUpdateManager.RESULT_CODE_GET_UPDATE);
    }

    private void onNoUpdate() {
        Log.d(TAG, "onNoUpdate()");
        if (mUpdateAction == HMBUpdateManager.UPDATE_ACTION_AUTO) {
            return;
        }
        hmbUpdateManager.notifyActivity(HMBUpdateManager.RESULT_CODE_NO_UPDATE);
    }

    private void onUpdateDone() {
        Log.d(TAG, "onUpdateDone()");
        SPUtils.instance(this).setLongValue(Constant.SHARED_PREFERENCES_LAST_UPDATE_TIME, 0, System.currentTimeMillis());
        //add by lgy
      //  Intent killService =  new Intent("android.intent.action.KillCspService");
      //  killService.setClassName(this, "com.hmb.manager.tms.CspService");
       // this.startService(killService);
        if (mUpdateAction == HMBUpdateManager.UPDATE_ACTION_AUTO) {
            return;
        }
        hmbUpdateManager.notifyActivity(HMBUpdateManager.RESULT_CODE_UPDATE_FINISHED);
    }
}