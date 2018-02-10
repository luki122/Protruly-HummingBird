package com.hmb.manager;

import android.app.Application;
import android.util.Log;

import com.hmb.manager.tms.TmsSecureService;

import java.util.HashMap;
import java.util.Map;

import tmsdk.common.ITMSApplicaionConfig;
import tmsdk.common.TMSDKContext;

/**
 * Created by xiaobin on 17-3-20.
 */

public class HMBManagerApplication extends Application {

    private static HMBManagerApplication instance;
    
    private boolean isCacheCleaned=false;
    
    private int rubblishCleanStatus=0;

    public volatile static boolean mBresult = false;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        initTMSDK();
    }


    private void initTMSDK() {
        TMSDKContext.setTMSDKLogEnable(true);
        long start = System.currentTimeMillis();
        boolean nFlag = false;
        TMSDKContext.setAutoConnectionSwitch(nFlag);
        mBresult = TMSDKContext.init(this, TmsSecureService.class, new ITMSApplicaionConfig() {
            @Override
            public HashMap<String, String> config(
                    Map<String, String> src) {
                HashMap<String, String> ret = new HashMap<String, String>(src);
                return ret;
            }
        });
        long end = System.currentTimeMillis();
        Log.v("initTMSDK", "initTMSDK() -> spend = " + (end-start) + ", result = " + mBresult);
    }

    public static HMBManagerApplication getInstance() {
        return instance;
    }

	public boolean isCacheCleaned() {
		return isCacheCleaned;
	}

	public void setCacheCleaned(boolean isCacheCleaned) {
		this.isCacheCleaned = isCacheCleaned;
	}

	public int getRubblishCleanStatus() {
		return rubblishCleanStatus;
	}

	public void setRubblishCleanStatus(int rubblishCleanStatus) {
		this.rubblishCleanStatus = rubblishCleanStatus;
	}
}