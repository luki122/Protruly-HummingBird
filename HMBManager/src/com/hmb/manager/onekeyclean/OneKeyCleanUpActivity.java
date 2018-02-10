package com.hmb.manager.onekeyclean;

import hb.preference.Preference;
import hb.preference.PreferenceActivity;
import hb.preference.PreferenceScreen;
import hb.preference.PreferenceCategory;
import tmsdk.fg.creator.ManagerCreatorF;
import tmsdk.fg.module.cleanV2.CleanManager;
import tmsdk.fg.module.cleanV2.ICleanTaskCallBack;
import tmsdk.fg.module.cleanV2.AppGroupDesc;
import tmsdk.fg.module.cleanV2.IScanTaskCallBack;
import tmsdk.fg.module.cleanV2.RubbishEntity;
import tmsdk.fg.module.cleanV2.RubbishHolder;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hmb.manager.Constant;
import com.hmb.manager.R;
import com.hmb.manager.adapter.APPAdapter;
import com.hmb.manager.bean.AppInfo;
import com.hmb.manager.bean.RubblishInfo;
import com.hmb.manager.qscaner.QScannerActivity;
import com.hmb.manager.qscaner.QScannerService;
import com.hmb.manager.rubbishclean.CacheCleanActivity;
import com.hmb.manager.rubbishclean.RubbishCleanActivity;
import com.hmb.manager.utils.ManagerUtils;
import com.hmb.manager.utils.SPUtils;
import com.hmb.manager.utils.TransUtils;
import hb.preference.PreferenceActivity;
import hb.preference.Preference.OnPreferenceClickListener;

public class OneKeyCleanUpActivity extends PreferenceActivity implements OnPreferenceClickListener {

	private Context mContext = null;
	private PreferenceScreen mCachePreference = null;
	private PreferenceScreen mAvailMemPreference = null;
	private PreferenceScreen mRubblishPreference = null;
	private PreferenceScreen mMemoryPreference = null;
	private PreferenceScreen mPwdPreference = null;
	private PreferenceScreen mvirtusPreference = null;
	private OneKeyCleanUpPreference mOneKeyPreference = null;
	private PreferenceCategory mHandPreference = null;
	private PreferenceCategory mOptimizedPreference = null;
	private static final String KEY_CLEANUP_ANI = "preference_cleanup_ani";
	private static final String KEY_ONEKEY_CACHE = "preference_onekey_systemcache";
	private static final String KEY_ONEKEY_AVAILMEM = "preference_onekey_availmem";
	private static final String KEY_ONEKEY_RUBBLISH = "preference_onekey_sysrubblish";
	private static final String KEY_ONEKEY_MEMORY = "preference_onekey_sysmemory";
	private static final String KEY_ONEKEY_PWD = "preference_onekey_pwd";
	private static final String KEY_ONEKEY_VIRTUS = "preference_onekey_virtus";
	private static final String KEY_HAND_SETTING = "result_hand_preference";
	private static final String KEY_OPTIMIZED_SETTING = "result_optimized_preference";
	private final int MSG_CACHESCANNER_START = 0x20;

	private final int MSG_CACHESCANNER_END = 0x21;

	private final int MSG_RUBBLISH_SCANNER_END = 0x23;

	private final int MSG_RUBBLISH_SCANNER_START = 0x25;
	
	private final int MSG_VIRTUS_QSCANNER_RESULT = 0x27;

	private static final int APPS_INFO_UPATE = 0x01;
	private static final int APP_KILL_END = 0x02;
	private static final int CACHE_CLEAN_END = 0x03;
	private CleanManager mCleanV2Manager;
	private long mRubblishTotalSize = 0;
	private long mSystemCacheTotalSize = 0;
	private long mSystemMemory = 0;
	private long mAPKTotalSize = 0;
	private static final String TAG = "OneKeyCleanUpActivity";
	private RubbishHolder mCurrentRubbish;
	private Method mGetPackageSizeInfoMethod;
	private Object mSync = new Object();
	private Map<Integer, AppInfo> runningAppMap = new HashMap<Integer, AppInfo>();
	private Map<String, ActivityManager.RunningAppProcessInfo> thirdPgkAppMap = null;
	private PackageManager pm;
	private List<ApplicationInfo> listAppcations = null;
	private ActivityManager activityMgr;
	AsyncTask<Void, Integer, List<AppInfo>> mTask;
	private String mLastPackageName = null;
	private List<PackageStats> installedAppLists = new ArrayList<PackageStats>();
	private String mLastedPkgN = null;
	private long mAppCacheTotalSize = 0;
	private long mVirtusNum=0;
	private boolean isRubblishClean, isCacheClean, isAppKilled = false,isVirtusScaned=false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate()");
		addPreferencesFromResource(R.layout.activity_onekey_cleanup);
		mContext = this.getApplicationContext();
		mCleanV2Manager = ManagerCreatorF.getManager(CleanManager.class);
		try {
			mGetPackageSizeInfoMethod = mContext.getPackageManager().getClass().getMethod("getPackageSizeInfo",
					String.class, IPackageStatsObserver.class);

		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		pm = getPackageManager();
		activityMgr = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		initView();
		mOneKeyPreference.setmScanStatus(OneKeyCleanUpPreference.INITIAL_STATUS);
		mCleanV2Manager.easyScan(mScanTaskCallBack, null);
		initData();
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, isRubblishClean+"  isRubblishClean onStart() isAppKilled "+isAppKilled);
		setMemoryPrefeStatus();
		handUpdate();
		
		bindQScannerService();
	}
	
	private void bindQScannerService() {
	    Intent qscannerIntent = new Intent();
	    qscannerIntent.setClass(mContext, QScannerService.class);
	    Log.d(TAG, "Bind QScanner Service...");
	    mContext.bindService(qscannerIntent, connection, Context.BIND_AUTO_CREATE);
    }

    private void handUpdate(){
		if (isRubblishClean && isAppKilled && isVirtusScaned) {
			if (!ManagerUtils.isPhoneHasLock(mContext)) {
				mPwdPreference.setTitle(mContext.getString(R.string.pwd_setting_title));
				mPwdPreference.setStatus(mContext.getString(R.string.pwd_setting_message));
				mPwdPreference.setStatusColor(Color.RED);
				mPwdPreference.showStatusArrow(true);
				mPwdPreference.setOnPreferenceClickListener(this);
			} else {
				mPwdPreference.setTitle(mContext.getString(R.string.onekey_pwd_check));
				mPwdPreference.setStatus(mContext.getString(R.string.pwd_setting_opened));
				mPwdPreference.setStatusColor(mContext.getResources().getColor(R.color.onekey_green));
				mPwdPreference.showStatusArrow(false);
				mPwdPreference.setOnPreferenceClickListener(null);
			}
			if (mVirtusNum<=0) {
				mvirtusPreference.setTitle(mContext.getString(R.string.onekey_virtus_check));
				mvirtusPreference.setStatus(mContext.getString(R.string.onekey_virtus_scanned));
				mvirtusPreference.setStatusColor(mContext.getResources().getColor(R.color.onekey_green));
				mvirtusPreference.showStatusArrow(false);
				mvirtusPreference.setOnPreferenceClickListener(null);
			} else {
				mvirtusPreference.showStatusArrow(true);
				mvirtusPreference.setTitle(mContext.getString(R.string.onekey_virtus_unscan_title));
				mvirtusPreference.setStatus(mContext.getString(R.string.onekey_virtus_unscan_message,String.valueOf(mVirtusNum)));
				mvirtusPreference.setStatusColor(Color.RED);
				mvirtusPreference.setOnPreferenceClickListener(this);
			}
		}
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.d(TAG, "onDestroy()");
		if (null != mCleanV2Manager) {
			mCleanV2Manager.onDestroy();
		}
		mCleanV2Manager = null;
		
		mContext.unbindService(connection);
	}

	private void initView() {
		mOneKeyPreference = (OneKeyCleanUpPreference) findPreference(KEY_CLEANUP_ANI);
		mCachePreference = (PreferenceScreen) findPreference(KEY_ONEKEY_CACHE);
		mCachePreference.showStatusArrow(false);
		mCachePreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
		mAvailMemPreference = (PreferenceScreen) findPreference(KEY_ONEKEY_AVAILMEM);
		mAvailMemPreference.showStatusArrow(false);
		mAvailMemPreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
		mRubblishPreference = (PreferenceScreen) findPreference(KEY_ONEKEY_RUBBLISH);
		mRubblishPreference.showStatusArrow(false);
		mRubblishPreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
		mMemoryPreference = (PreferenceScreen) findPreference(KEY_ONEKEY_MEMORY);
		mMemoryPreference.showStatusArrow(false);
		mMemoryPreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
		mPwdPreference = (PreferenceScreen) findPreference(KEY_ONEKEY_PWD);
		mPwdPreference.showStatusArrow(false);
		mPwdPreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
		mvirtusPreference = (PreferenceScreen) findPreference(KEY_ONEKEY_VIRTUS);
		mvirtusPreference.showStatusArrow(false);
		mvirtusPreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
		mHandPreference = (PreferenceCategory) findPreference(KEY_HAND_SETTING);
		mOptimizedPreference = (PreferenceCategory) findPreference(KEY_OPTIMIZED_SETTING);
		getPreferenceScreen().removePreference(mHandPreference);
		getPreferenceScreen().removePreference(mOptimizedPreference);
	}

	private void initData() {
		//runningAppMap.clear();
		//queryAllRunningAppInfo();
		new Thread(new QueryRunningAppThread()).start();
		//installedAppLists.clear();
		// scanCaches();
	}

	ScanTaskCallBack mScanTaskCallBack = new ScanTaskCallBack();

	class ScanTaskCallBack implements IScanTaskCallBack {

		@Override
		public void onScanStarted() {
			Log.i(TAG, "onScanStarted : ");
			sendHandlerMessage(MSG_RUBBLISH_SCANNER_START);
		}

		public void onRubbishFound(RubbishEntity aRubbish) {
			Log.i(TAG, "onRubbishFound : ");
		}

		@Override
		public void onScanCanceled(RubbishHolder aRubbishHolder) {
			Log.i(TAG, "onScanCanceled : ");
			sendHandlerMessage(MSG_RUBBLISH_SCANNER_END);
		}

		@Override
		public void onScanFinished(RubbishHolder aRubbishHolder) {
			Log.i(TAG, "onScanFinished : ");
			// rubblishResultInfoList.clear();
			mCurrentRubbish = aRubbishHolder;
			List<RubbishEntity> rubblishList = null;
			int mSize = 0;
			if (null != aRubbishHolder) {
				if (null != aRubbishHolder.getmApkRubbishes()) {
					rubblishList = aRubbishHolder.getmApkRubbishes();
					Log.d(TAG, "apk rubblish size " + aRubbishHolder.getmApkRubbishes().size());
				}
				if (null != aRubbishHolder.getmSystemRubbishes()) {
					rubblishList = new ArrayList<RubbishEntity>(aRubbishHolder.getmSystemRubbishes().values());
					Log.d(TAG, "system rubblish size " + aRubbishHolder.getmSystemRubbishes().size());
				}
				if (null != aRubbishHolder.getmInstallRubbishes()) {
					mSystemCacheTotalSize = countCleanedRubblishSize(
							new ArrayList<RubbishEntity>(aRubbishHolder.getmInstallRubbishes().values()));
					rubblishList = new ArrayList<RubbishEntity>(aRubbishHolder.getmInstallRubbishes().values());
					Log.d(TAG, "install rubblish size " + aRubbishHolder.getmInstallRubbishes().size());
				}
				if (null != aRubbishHolder.getmUnInstallRubbishes()) {
					rubblishList = new ArrayList<RubbishEntity>(aRubbishHolder.getmUnInstallRubbishes().values());
					Log.d(TAG, "uninstall rubblish size " + aRubbishHolder.getmUnInstallRubbishes().size());

				}
				mRubblishTotalSize = aRubbishHolder.getAllRubbishFileSize();

				mCleanV2Manager.cleanRubbish(mCurrentRubbish, mCleanCallback);
			}
			sendHandlerMessage(MSG_RUBBLISH_SCANNER_END);
		}

		@Override
		public void onScanError(int error, RubbishHolder aRubbishHolder) {
			Log.i(TAG, "onScanError : " + error);
			mUIHandler.sendEmptyMessage(MSG_RUBBLISH_SCANNER_END);
		}

		@Override
		public void onDirectoryChange(String dirPath, int fileCnt) {
		}

	}

	private void setMemoryPrefeStatus() {
		long availMem = ManagerUtils.getAvailableInternalMemorySize(mContext);
		long tatalMem = ManagerUtils.getFlashSize(true);
		double percentD = 0;
		percentD = (double) availMem / tatalMem;
		if (percentD > 0.15) {
			mAvailMemPreference.setStatus(mContext.getString(R.string.intern_memory_enough));
			mAvailMemPreference.setStatusColor(mContext.getResources().getColor(R.color.onekey_green));
		} else if (percentD <= 0.15) {
			mAvailMemPreference.setStatus(mContext.getString(R.string.intern_memory_less));
			mAvailMemPreference.setStatusColor(Color.RED);
		}
	}

	CleanCallback mCleanCallback = new CleanCallback();

	class CleanCallback implements ICleanTaskCallBack {

		@Override
		public void onCleanStarted() {
			Log.i(TAG, "onCleanStarted : ");
		}

		@Override
		public void onCleanProcessChange(int nowPercent, String aCleanPath) {
			File file = new File(aCleanPath);
			Log.i(TAG, file.length() + "onCleanProcessChange : " + nowPercent + "% ::" + aCleanPath);
		}

		@Override
		public void onCleanCanceled() {

		}

		@Override
		public void onCleanFinished() {

			Log.i(TAG, "onCleanFinish : ");
		}

		@Override
		public void onCleanError(int error) {

		}

	}

	private long countCleanedRubblishSize(List<RubbishEntity> rubblishList) {
		long mSize = 0;
		for (RubbishEntity entity : rubblishList) {
			mSize = mSize + entity.getSize();
		}
		return mSize;
	}

	private void sendHandlerMessage(int msg) {
		Message message = mUIHandler.obtainMessage(msg);
		mUIHandler.sendMessage(message);
	}

	private Handler mUIHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_RUBBLISH_SCANNER_END:
				if (mRubblishTotalSize > 0) {
					mRubblishPreference.setStatus(TransUtils.transformShortType(mRubblishTotalSize, true));
				} else {
					mRubblishPreference.setStatus(mContext.getString(R.string.onekey_system_norubblish));
				}
				if (mSystemCacheTotalSize > 0) {
					mCachePreference.setStatus(TransUtils.transformShortType(mSystemCacheTotalSize, true));
				} else {
					mCachePreference.setStatus(mContext.getString(R.string.onekey_system_nocache));
				}
				isRubblishClean = true;
				cleanResultHandler();
                                SPUtils.instance(mContext).setLongValue(Constant.CACHE_CLEANUP_TIME, 0,
                                        System.currentTimeMillis());
			        SPUtils.instance(mContext).setLongValue(Constant.RUBBLISH_CLEANUP_TIME, 0,
			                System.currentTimeMillis());
				break;

			case MSG_RUBBLISH_SCANNER_START:
				break;
			case MSG_CACHESCANNER_START:
				// mOneKeyPreference.setmScanStatus(OneKeyCleanUpPreference.SCANNING_STATUS);
				break;

			case MSG_CACHESCANNER_END:
				cleanCache();
				break;
			case CACHE_CLEAN_END:
				mCachePreference.setStatus(TransUtils.transformShortType(mAppCacheTotalSize, true));
				Log.d(TAG, ManagerUtils.isPhoneHasLock(mContext) + "--------mAppCacheTotalSize----------"
						+ TransUtils.transformShortType(mAppCacheTotalSize, true));
				isCacheClean = true;
				cleanResultHandler();
				break;
			case APPS_INFO_UPATE:
				int pid = msg.arg1;
				AppInfo appInfo = (AppInfo) msg.obj;
				if (!runningAppMap.containsKey(pid))
					runningAppMap.put(pid, appInfo);
				Log.d(TAG, runningAppMap.size() + "----APPS_INFO" + appInfo.getPkgName() + "UPATE--------"
						+ thirdPgkAppMap.size());
				if (runningAppMap.size() == thirdPgkAppMap.size()) {
					new Thread(new KillAppThread()).start();
				}
				break;
			case APP_KILL_END:
				Log.d(TAG, ManagerUtils.isPhoneHasLock(mContext) + "---------mSystemMemory----------"
						+ TransUtils.transformShortType(mSystemMemory, true));
				if (mSystemMemory > 0) {
					mMemoryPreference.setStatus(TransUtils.transformShortType(mSystemMemory, true));
				} else {
					mMemoryPreference.setStatus(mContext.getString(R.string.onekey_app_nokilled));
				}
				isAppKilled = true;
				cleanResultHandler();
		        SPUtils.instance(mContext).setLongValue(Constant.MEMORY_CLEAN_TIME, 0,
		                System.currentTimeMillis());
				break;

				
			case MSG_VIRTUS_QSCANNER_RESULT:
			    Log.d(TAG, "virtus count = " + msg.obj.toString());
			    isVirtusScaned=true;
			    mVirtusNum=Integer.parseInt(msg.obj.toString());
			    cleanResultHandler();
			    //mvirtusPreference.setStatus(msg.obj.toString());
                break;
			}
		}

	};

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();
		Intent intent = null;
		switch (key) {
		case KEY_ONEKEY_VIRTUS: {
			intent = new Intent(this, QScannerActivity.class);
			startActivity(intent);
			break;
		}
		case KEY_ONEKEY_PWD: {
			intent = new Intent();
			intent.setAction("android.settings.LOCKSCREEN_SETTINGS");
			startActivity(intent);
			break;
		}
		}

		return true;
	}

	private void cleanResultHandler() {
		Log.d(TAG, isRubblishClean + " =isRubblishClean  isAppKilled=" + isAppKilled);
		if (isRubblishClean && isAppKilled && isVirtusScaned) {
			handUpdate();
			getPreferenceScreen().addPreference(mHandPreference);
			getPreferenceScreen().addPreference(mOptimizedPreference);
			mHandPreference.addPreference(mPwdPreference);
			mHandPreference.addPreference(mvirtusPreference);
			mOptimizedPreference.addPreference(mCachePreference);
			mOptimizedPreference.addPreference(mAvailMemPreference);
			mOptimizedPreference.addPreference(mRubblishPreference);
			mOptimizedPreference.addPreference(mMemoryPreference);
			getPreferenceScreen().removePreference(mPwdPreference);
			getPreferenceScreen().removePreference(mvirtusPreference);
			getPreferenceScreen().removePreference(mCachePreference);
			getPreferenceScreen().removePreference(mAvailMemPreference);
			getPreferenceScreen().removePreference(mRubblishPreference);
			getPreferenceScreen().removePreference(mMemoryPreference);
			SPUtils.instance(mContext).setLongValue(Constant.ONEKEY_CLEANUP_TIME, 0, System.currentTimeMillis());
			mOneKeyPreference.setmScanStatus(OneKeyCleanUpPreference.SCAN_DONE_STATUS);
		}
	}

	private List<AppInfo> queryAllRunningAppInfo() {
		pm = mContext.getPackageManager();
		listAppcations = pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
		Collections.sort(listAppcations, new ApplicationInfo.DisplayNameComparator(pm));
		Map<String, ActivityManager.RunningAppProcessInfo> pgkProcessAppMap = new HashMap<String, ActivityManager.RunningAppProcessInfo>();

		List<ActivityManager.RunningAppProcessInfo> appProcessList = activityMgr.getRunningAppProcesses();

		for (ActivityManager.RunningAppProcessInfo appProcess : appProcessList) {
			int pid = appProcess.pid; // pid
			String processName = appProcess.processName;
			Log.i(TAG, "processName: " + processName + " pid: " + pid);

			String[] pkgNameList = appProcess.pkgList;
			for (int i = 0; i < pkgNameList.length; i++) {
				String pkgName = pkgNameList[i];
				Log.i(TAG, "packageName " + pkgName + " at index " + i + " in process " + pid);
				pgkProcessAppMap.put(pkgName, appProcess);
			}
		}
		thirdPgkAppMap = ManagerUtils.thirdApplicationFilter(pgkProcessAppMap, mContext);
		if (thirdPgkAppMap.size() > 0) {
			for (String pkgName : thirdPgkAppMap.keySet()) {
				final int pid = thirdPgkAppMap.get(pkgName).pid;
				String processName = thirdPgkAppMap.get(pkgName).processName;
				try {
					ApplicationInfo app = pm.getPackageInfo(pkgName, 0).applicationInfo;
					final AppInfo appInfo = ManagerUtils.getAppInfo(mContext, app, pid, processName);
					mGetPackageSizeInfoMethod.invoke(mContext.getPackageManager(),
							new Object[] { app.packageName, new IPackageStatsObserver.Stub() {
								@Override
								public void onGetStatsCompleted(PackageStats pStats, boolean succeeded)
										throws RemoteException {
									synchronized (appInfo) {
										appInfo.setPkgSize(pStats.cacheSize + pStats.codeSize + pStats.dataSize);
										Message msg = mUIHandler.obtainMessage(APPS_INFO_UPATE);
										msg.obj = appInfo;
										msg.arg1 = pid;
										mUIHandler.sendMessage(msg);
										Log.i(TAG, "mGetPackageSizeInfoMethod " + appInfo.getPkgName());
									}
								}
							} });
				} catch (Exception e) {
					e.printStackTrace();
					Log.i(TAG, "mGetPackageSizeInfoMethod " + e.getMessage());
				}
			}
		} else {
			Message msg = mUIHandler.obtainMessage(APP_KILL_END);
			mUIHandler.sendMessage(msg);
		}
		return new ArrayList<AppInfo>(runningAppMap.values());

	}

	class KillAppThread implements Runnable {

		private List<AppInfo> appinfos;

		public KillAppThread() {
			appinfos = new ArrayList<AppInfo>(runningAppMap.values());
		}

		@Override
		public void run() {
			AppInfo app = null;
			if (appinfos != null && appinfos.size() > 0) {
				for (int i = 0; i < appinfos.size(); ++i) {
					app = appinfos.get(i);
					activityMgr.forceStopPackage(app.getPkgName());
					mSystemMemory = mSystemMemory + app.getPkgSize();
				}
			}
			Message msg = mUIHandler.obtainMessage(APP_KILL_END);
			mUIHandler.sendMessage(msg);
		}
	}
	
	class QueryRunningAppThread implements Runnable{

		@Override
		public void run() {
			queryAllRunningAppInfo();
		}
		
	}

	private void scanCaches() {
		new Thread() {
			public void run() {
				Method getPackageSizeInfoMethod = null;
				Method[] methods = PackageManager.class.getMethods();
				for (Method method : methods) {
					if ("getPackageSizeInfo".equals(method.getName())) {
						getPackageSizeInfoMethod = method;
						break;
					}
				}
				List<ApplicationInfo> packageInfos = ManagerUtils
						.filterSystemAPP(pm.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES));

				sendHandlerMessage(MSG_CACHESCANNER_START);
				if (packageInfos != null && packageInfos.size() > 0) {
					mLastedPkgN = packageInfos.get(packageInfos.size() - 1).packageName;
					for (ApplicationInfo info : packageInfos) {
						try {
							getPackageSizeInfoMethod.invoke(pm, info.packageName, new MyDataObserver());
							Thread.sleep(10);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			};
		}.start();

	}

	private class MyDataObserver extends IPackageStatsObserver.Stub {

		@Override
		public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) throws RemoteException {
			final long cache = pStats.cacheSize;
			final String packname = pStats.packageName;
			if (cache > 0) {
				mAppCacheTotalSize = mAppCacheTotalSize + cache;
				Log.d(TAG,
						pStats.dataSize + "------------MyData " + cache + " Observer----packagename-----" + packname);
				if (pStats != null) {
					if (!installedAppLists.contains(pStats)) {
						installedAppLists.add(pStats);
					}
				}

			}

			if (packname.equals(mLastedPkgN)) {
				sendHandlerMessage(MSG_CACHESCANNER_END);
			}
		}
	}

	private void cleanCache() {
		ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);

		if (installedAppLists != null && installedAppLists.size() > 0) {
			List<PackageStats> Lists = ManagerUtils.filterSpecialAPP(installedAppLists,mContext);
			if (Lists != null && Lists.size() > 0) {
				mLastPackageName = Lists.get(Lists.size() - 1).packageName;
				for (PackageStats info : Lists) {
					boolean res = am.clearApplicationUserData(info.packageName, mClearDataObserver);
				}
			} else {
				sendHandlerMessage(CACHE_CLEAN_END);
			}
		} else {
			sendHandlerMessage(CACHE_CLEAN_END);
		}
	}

	ClearUserDataObserver mClearDataObserver = new ClearUserDataObserver();

	class ClearUserDataObserver extends IPackageDataObserver.Stub {
		public void onRemoveCompleted(final String packageName, final boolean succeeded) {
			Log.d(TAG, "packageName " + packageName + "   succeeded  " + succeeded);
			Log.d(TAG, "mLastPackageName =" + mLastPackageName);
			if (mLastPackageName.equals(packageName)) {
				sendHandlerMessage(CACHE_CLEAN_END);
			}
		}
	}

	private ServiceConnection connection = new ServiceConnection() {  
	    
        @Override  
        public void onServiceDisconnected(ComponentName name) {  
        }  
  
        @Override  
        public void onServiceConnected(ComponentName name, IBinder service) {  
            QScannerService.QScannerBinder binder = (QScannerService.QScannerBinder) service;
            QScannerService myService = binder.getService();
            myService.setCallback(new QScannerService.Callback() {
                @Override
                public void onDataChange(String data) {
                    Log.d(TAG, "virtus scan finished, result = " + data);

                    Message msg = new Message();
                    msg.obj = data;
                    msg.what = MSG_VIRTUS_QSCANNER_RESULT;
                    mUIHandler.sendMessage(msg);
                }
            });
        }
    };  

}
