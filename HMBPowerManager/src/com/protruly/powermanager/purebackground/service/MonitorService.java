package com.protruly.powermanager.purebackground.service;

import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.IProcessObserver;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;

import com.protruly.powermanager.purebackground.Config;
import com.protruly.powermanager.purebackground.Info.AppInfo;
import com.protruly.powermanager.purebackground.Info.AutoStartInfo;
import com.protruly.powermanager.purebackground.interfaces.Observer;
import com.protruly.powermanager.purebackground.interfaces.Subject;
import com.protruly.powermanager.purebackground.model.AutoCleanModel;
import com.protruly.powermanager.purebackground.model.AutoStartModel;
import com.protruly.powermanager.purebackground.model.ConfigModel;
import com.protruly.powermanager.purebackground.model.ForbitAlarmModel;
import com.protruly.powermanager.utils.ApkUtils;
import com.protruly.powermanager.utils.LogUtils;
import com.protruly.powermanager.utils.StringUtils;

import java.util.List;

/**
 * Service that monitor system events changes, all the following events will be monitored:
 * 1. Process state changed.
 * 2. Package changed.
 * 3. Screen on/off.
 */
public class MonitorService extends Service implements Observer {
    private static final String TAG = MonitorService.class.getSimpleName();

	private Context mContext;
	private AlarmManager mAlarmManager;
	private boolean isRegistered = false;

	@Override
	public void onCreate() {
		super.onCreate();
		LogUtils.d(TAG, "onCreate()");
		mContext = MonitorService.this;
		mAlarmManager = (AlarmManager) mContext.getSystemService(ALARM_SERVICE);
		ConfigModel.getInstance(this).getAppInfoModel().attach(this);
		try {
			ActivityManagerNative.getDefault().registerProcessObserver(mProcessObserver);
        } catch (Exception e) {
			LogUtils.e(TAG, "onCreate() -> registerProcessObserver Exception = " + e.toString());
		}
//		setCpuAndRamMonitor(true);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		LogUtils.d(TAG, "onStartCommand()");
		if (!isRegistered) {
			registerMonitorReceiver();
			isRegistered = true;
		}
		loadData();
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		LogUtils.d(TAG, "onDestroy()");
		isRegistered = false;
		mContext.unregisterReceiver(mMonitorReceiver);
		ConfigModel.getInstance(this).getAppInfoModel().detach(this);
		try {
			ActivityManagerNative.getDefault().unregisterProcessObserver(mProcessObserver);
		} catch (Exception e) {
			LogUtils.d(TAG, "onDestroy() -> Exception = " + e.toString());
		}
	}

	private void loadData(){
		if (ConfigModel.getInstance(this).getAppInfoModel().loadAllAppInfoCompleted()) {
			updateOfInit(ConfigModel.getInstance(this).getAppInfoModel());
		} else {
			if (!ConfigModel.getInstance(this).getAppInfoModel().isLoadingAppInfo()) {
				ConfigModel.getInstance(this).getAppInfoModel().startLoadAllAppInfo();
			}
		}
	}

	@Override
	public void updateOfInit(Subject subject) {
		LogUtils.d(TAG, "updateOfInit()");
		ForbitAlarmModel.getInstance(this).applicationStart();
		AutoStartModel.getInstance(this).applicationStart();
		AutoCleanModel.getInstance(this).applicationStart();
		startService(new Intent(mContext, BGCleanService.class));
	}

	@Override
	public void updateOfInStall(Subject subject, String pkgName) {
		LogUtils.d(TAG, "updateOfInStall() -> pkgName = " + pkgName);
		AppInfo appInfo = ConfigModel.getInstance(this).getAppInfoModel().findAppInfo(pkgName);
		if (appInfo == null) {
			LogUtils.d(TAG, "updateOfInStall() -> can't find " + pkgName + " appInfo!");
		}

		if (AutoStartModel.getInstance() != null) {
			AutoStartModel.getInstance(this).inStallApp(appInfo);
		} else {
			AutoStartModel.getInstance(this).inStallApp(appInfo);
			AutoStartModel.releaseObject();
		}

		if (AutoCleanModel.getInstance() != null) {
			AutoCleanModel.getInstance(this).installApp(appInfo);
		}

        if (ForbitAlarmModel.getInstance() != null) {
            ForbitAlarmModel.getInstance(this).installApp(appInfo);
        }
	}

	@Override
	public void updateOfCoverInStall(Subject subject, String pkgName) {
		LogUtils.d(TAG, "updateOfCoverInStall() -> pkgName = " + pkgName);
		AppInfo appInfo = ConfigModel.getInstance(this).getAppInfoModel().findAppInfo(pkgName);

		if (AutoStartModel.getInstance() != null) {
			AutoStartModel.getInstance(this).coverInStallApp(appInfo);
		} else {
			AutoStartModel.getInstance(this).coverInStallApp(appInfo);
			AutoStartModel.releaseObject();
		}
	}

	@Override
	public void updateOfUnInstall(Subject subject, String pkgName) {
		LogUtils.d(TAG, "updateOfUnInstall() -> pkgName = " + pkgName);
        if(AutoStartModel.getInstance() != null) {
        	AutoStartModel.getInstance(this).unInStallApp(pkgName);	
		}

		if (AutoCleanModel.getInstance() != null) {
			AutoCleanModel.getInstance(this).unInStallApp(pkgName);
		}

        if (ForbitAlarmModel.getInstance() != null) {
            ForbitAlarmModel.getInstance(this).unInStallApp(pkgName);
        }
	}
	
	@Override
	public void updateOfExternalAppAvailable(Subject subject, List<String> pkgList) {
		LogUtils.d(TAG, "updateOfExternalAppAvailable() -> pkgList = " + pkgList.toString());
		AutoStartModel.getInstance(this).externalAppAvailable(pkgList);	
	}
	
	@Override
	public void updateOfExternalAppUnAvailable(Subject subject, List<String> pkgList) {
		LogUtils.d(TAG, "updateOfExternalAppUnAvailable() -> pkgList = " + pkgList.toString());
		AutoStartModel.getInstance(this).externalAppUnAvailable(pkgList);	
	}
	
	/**
	 * Monitor Process State.
	 */
	private IProcessObserver mProcessObserver = new IProcessObserver.Stub() {
        @Override
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
			String packageName = ApkUtils.getPackageNameByUid(MonitorService.this, uid);
			LogUtils.d(TAG, "onForegroundActivitiesChanged() -> packageName = " + packageName
					+ ", pid = " + pid + ", uid = " + uid
					+ ", foregroundActivities = " + foregroundActivities);
        	if (Process.myPid() == pid && foregroundActivities) {
        		AutoStartModel.getInstance(MonitorService.this).applicationStart();
        	}
        	
        	if (Config.isAutoStartControlReceive) {
            	dealAutoStart(packageName, foregroundActivities);
        	}        	
        }

        @Override
        public void onProcessDied(int pid, int uid) {
        }

        /*@Override*/
        public void onProcessStateChanged(int pid, int uid, int procState) throws RemoteException {
        }
    };

    /**
     * Application launched, open disabled receiver.
     * @param packageName
     * @param foregroundActivities
     */
    private void dealAutoStart(String packageName, boolean foregroundActivities){
    	AutoStartModel instance = AutoStartModel.getInstance();
    	if (StringUtils.isEmpty(packageName) || instance == null) {
    		return;
    	}
    	LogUtils.d(TAG, "dealAutoStart() -> packageName = " + packageName
				+ ", foregroundActivities = " + foregroundActivities);
		if (foregroundActivities) {
			AutoStartInfo autoStartInfo = instance.getAutoStartInfo(packageName);
			if (autoStartInfo != null && !autoStartInfo.getIsOpen()) {
				ApkUtils.openApkAutoStart(this, autoStartInfo, packageName);
				LogUtils.d(TAG, "dealAutoStart() -> open [" + packageName + "] receiver");
			}			
		}
	}

	private void registerMonitorReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(mMonitorReceiver, filter);
	}

	private MonitorReceiver mMonitorReceiver = new MonitorReceiver();
	private class MonitorReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			LogUtils.d(TAG, "MonitorReceiver() -> action = " + action);
			if (action.equals(Intent.ACTION_SCREEN_OFF)) {
				triggerByScreenOffIntent();
			} else if (action.equals(Intent.ACTION_SCREEN_ON)) {
				triggerByScreenOnIntent();
			}
		}
	}

	private PendingIntent mAutoCleanIntent = null;
	private PendingIntent mIdleFreezeIntent = null;

	private void triggerByScreenOnIntent() {
		cancelScreenOffAlarmManager();
//		setCpuAndRamMonitor(true);
	}

	private void triggerByScreenOffIntent() {
		setScreenOffAlarmManager();
//		setCpuAndRamMonitor(false);
	}

	public void cancelScreenOffAlarmManager() {
		// Cancel clean background alarm.
		if (mAlarmManager != null && mAutoCleanIntent != null) {
			mAlarmManager.cancel(mAutoCleanIntent);
		}

		// Cancel idle freeze alarm and restore standby status.
		if (mAlarmManager != null && mIdleFreezeIntent != null) {
				mAlarmManager.cancel(mIdleFreezeIntent);
		}
		Intent intent = new Intent(this, IdleFreezeService.class);
		intent.setAction(IdleFreezeService.ACTION_IDLE_FREEZE);
		intent.putExtra(IdleFreezeService.IDLE_FREEZE_OP, IdleFreezeService.OP_UNFREEZE);
		startService(intent);
	}

	private void setScreenOffAlarmManager() {
		// Start clean background alarm.
		Intent autoCleanIntent = new Intent();
		autoCleanIntent.setAction(BGCleanService.ACTION_CLEAN_BG);
		autoCleanIntent.putExtra(BGCleanService.ACTION_CLEAN_BG, BGCleanService.AUTO_BG_CLEAN);
		mAutoCleanIntent = PendingIntent.getService(this, 0, autoCleanIntent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		if (mAutoCleanIntent != null) {
			mAlarmManager.cancel(mAutoCleanIntent);
			mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()
							+ Config.SCREEN_OFF_CLEAN_BG_TRIGGER_TIME, mAutoCleanIntent);
		} else {
			RuntimeException e = new RuntimeException("here");
			e.fillInStackTrace();
		}

		// Start idle freeze alarm.
		Intent idleFreezeIntent = new Intent();
		idleFreezeIntent.setAction(IdleFreezeService.ACTION_IDLE_FREEZE);
		idleFreezeIntent.putExtra(IdleFreezeService.IDLE_FREEZE_OP, IdleFreezeService.OP_FREEZE);
		mIdleFreezeIntent = PendingIntent.getService(this, 0, idleFreezeIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		if (mIdleFreezeIntent != null) {
			mAlarmManager.cancel(mIdleFreezeIntent);
			mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()
					+ Config.SCREEN_OFF_IDLE_FREEZE_TRIGGER_TIME, mIdleFreezeIntent);
		} else {
			RuntimeException e = new RuntimeException("here");
			e.fillInStackTrace();
		}
	}

	private void setCpuAndRamMonitor(boolean monitor) {
		Intent intent = new Intent(mContext, CpuRamMonitorService.class);
		if (monitor) {
			startService(intent);
		} else {
			stopService(intent);
		}
	}
}