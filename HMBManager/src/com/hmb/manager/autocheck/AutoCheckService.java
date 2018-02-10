package com.hmb.manager.autocheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;



import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.hmb.manager.MainActivity;
import com.hmb.manager.R;
import com.hmb.manager.netmanager.DataManagerManager;
import com.hmb.manager.tms.TmsSecureService;
import com.hmb.manager.utils.TransUtils;

import tmsdk.fg.module.cleanV2.IScanTaskCallBack;
import tmsdk.fg.module.cleanV2.RubbishEntity;

import tmsdk.fg.creator.ManagerCreatorF;
import tmsdk.fg.module.cleanV2.CleanManager;
import tmsdk.fg.module.cleanV2.ICleanTaskCallBack;
import tmsdk.fg.module.cleanV2.RubbishHolder;
import tmsdk.common.ITMSApplicaionConfig;
import tmsdk.common.TMSDKContext;

public class AutoCheckService extends Service {
	private static Timer timer = null;
	private CleanManager mCleanV2Manager;
	private long mRubblishTotalSize = 0;
	private static final String TAG = "AutoCheckService";
	private final static long AUTO_CHECK_RUBBLISH_PERIOD = 24 * 60 * 60 * 1000 * 7;
	private final static long AUTO_CHECK_RUBBLISH_START = 8 * 60 * 60 * 1000 +AUTO_CHECK_RUBBLISH_PERIOD;
	private final static long RUBBLISH_SIZE_LEVEL = 456 * 1024 * 1024;
	 public volatile static boolean mBresult = false;
	@Override
	public void onCreate() {
		super.onCreate();
		initTMSDK();
		mCleanV2Manager = ManagerCreatorF.getManager(CleanManager.class);
		Log.i(TAG, "AutoCheckService--onCreate-----");
	}

    private void initTMSDK() {
        TMSDKContext.setTMSDKLogEnable(true);
        long start = System.currentTimeMillis();
        boolean nFlag = true;
        TMSDKContext.setAutoConnectionSwitch(nFlag);
        //该方法在TMSDKContext.init()之前调用 add by zhaolaichao 20170519
        //DataManagerManager.setDualPhoneInfoFetcher(DataManagerManager.getIMSI(this));
        mBresult = TMSDKContext.init(this, TmsSecureService.class, new ITMSApplicaionConfig() {
            @Override
            public HashMap<String, String> config(
                    Map<String, String> src) {
                HashMap<String, String> ret = new HashMap<String, String>(src);
                return ret;
            }
        });
        long end = System.currentTimeMillis();
        Log.v(TAG, "initTMSDK() -> spend = " + (end-start) + ", result = " + mBresult);
    }
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "AutoCheckService--onStartCommand-----");
		startTimer();
		return super.onStartCommand(intent, flags, startId);
	}

	private void startTimer() {
		if (null == timer) {
			timer = new Timer();
		}
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				scanRubblish();
				Log.i(TAG, "AutoCheckService--scanRubblish-----");
			}
		}, AUTO_CHECK_RUBBLISH_START, AUTO_CHECK_RUBBLISH_PERIOD);
	}

	
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
 		if(null!=mCleanV2Manager) {
			mCleanV2Manager.onDestroy(); 
		}
 		mCleanV2Manager=null;
	}

	private void scanRubblish() {
		mCleanV2Manager.scanDisk(mScanTaskCallBack, null);
	}

	ScanTaskCallBack mScanTaskCallBack = new ScanTaskCallBack();

	class ScanTaskCallBack implements IScanTaskCallBack {

		@Override
		public void onScanStarted() {
		}

		public void onRubbishFound(RubbishEntity aRubbish) {
		}

		@Override
		public void onScanCanceled(RubbishHolder aRubbishHolder) {

		}

		@Override
		public void onScanFinished(RubbishHolder aRubbishHolder) {
			Log.i(TAG, "onScanFinished : ");
			// rubblishResultInfoList.clear();
			int mSize = 0;
			if (null != aRubbishHolder) {
				mRubblishTotalSize = aRubbishHolder.getAllRubbishFileSize();
				Log.i(TAG, "AutoCheckService------------onScanFinished : mRubblishTotalSize"
						+ TransUtils.transformShortType(mRubblishTotalSize, true));
				if (mRubblishTotalSize >= RUBBLISH_SIZE_LEVEL) {
					initNotification();
				}
			}
		}

		@Override
		public void onScanError(int error, RubbishHolder aRubbishHolder) {
			Log.i(TAG, "onScanError : " + error);
		}

		@Override
		public void onDirectoryChange(String dirPath, int fileCnt) {
		}

	}

	private void initNotification(){
		NotificationManager mn = (NotificationManager) AutoCheckService.this
				.getSystemService(NOTIFICATION_SERVICE);
		Notification.Builder builder = new Notification.Builder(AutoCheckService.this);
		Intent notificationIntent = new Intent(AutoCheckService.this, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(AutoCheckService.this, 0, notificationIntent,
				0);
		builder.setContentIntent(contentIntent);
		builder.setSmallIcon(R.drawable.hmb_notification);
		builder.setTicker(this.getText(R.string.app_name)); 
		builder.setContentText(this.getString(R.string.notification_text_autocheck, TransUtils.transformShortType(mRubblishTotalSize, true)));
		builder.setContentTitle(this.getText(R.string.app_name));
		builder.setAutoCancel(true);
		builder.setDefaults(Notification.DEFAULT_ALL);
		Notification notification = builder.build();
		mn.notify((int) System.currentTimeMillis(), notification);
	}

}
