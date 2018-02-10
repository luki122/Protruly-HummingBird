package com.hmb.manager.rubbishclean;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hmb.manager.Constant;
import com.hmb.manager.HMBManagerApplication;
import com.hmb.manager.R;
import com.hmb.manager.adapter.RubblishExpandableListAdapter;
import com.hmb.manager.bean.AppInfo;
import com.hmb.manager.bean.RubblishInfo;
import com.hmb.manager.bean.StorageSize;
import com.hmb.manager.utils.ManagerUtils;
import com.hmb.manager.utils.SPUtils;
import com.hmb.manager.utils.TransUtils;
import com.hmb.manager.widget.textconter.CounterView;
import com.hmb.manager.widget.textcounter.formatters.DecimalFormatter;

import hb.app.HbActivity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import tmsdk.fg.creator.ManagerCreatorF;
import tmsdk.fg.module.cleanV2.AppGroupDesc;
import tmsdk.fg.module.cleanV2.CleanManager;
import tmsdk.fg.module.cleanV2.IScanTaskCallBack;
import tmsdk.fg.module.cleanV2.RubbishEntity;
import tmsdk.fg.module.cleanV2.RubbishHolder;
import hb.preference.Preference;
import hb.preference.PreferenceActivity;
import hb.preference.PreferenceScreen;
import hb.preference.Preference.OnPreferenceClickListener;

public class CleanSpeedActivity extends PreferenceActivity implements OnPreferenceClickListener {

	private CleanManager mCleanV2Manager;

	private Context mContext = null;

	private final int MSG_REFRESH_SPACE_DETAIL = 0x08;
	private final int MSG_REFRESH_HEAD_PROGRESS = 0x15;
	private final int MSG_REFRESH_CONTER = 0x16;

	private final int MSG_REFRESH_ExpandableList = 0x17;

	private final int MSG_CACHESCANNER_START = 0x20;

	private final int MSG_CACHESCANNER_END = 0x21;

	private final int MSG_RUBBLISH_SCANNER_END = 0x23;

	private final int MSG_RUBBLISH_SCANNER_START = 0x25;

	private final int MSG_SCANNER_UI_UPDATE = 0x24;

	private static final String TAG = "CleanSpeedActivity";

	private String mPathTips = "";

	private RubbishHolder mCurrentRubbish;

	StringBuffer mTips = new StringBuffer();

	private long _rubbishSelectedFileCnt = 0;
	private long _rubbishFileCnt = 0;
	private long mRubblishTotalSize;
	private StringBuffer sbtips;
	private String mScanPackage;
	private PackageManager pm;
	private RubblishExpandableListAdapter adapter;
	private List<PackageStats> installedAppLists = new ArrayList<PackageStats>();
	private ArrayList<RubblishInfo> cacheResultInfoList = new ArrayList<RubblishInfo>();
	private List<AppInfo> runningAppList = null;
	private CleanScannerPreference mCleanScannerPreference;
	private PreferenceScreen mRubblishPreference = null;
	private PreferenceScreen mAppCachePreference = null;
	private PreferenceScreen mBackGroundAppPreference = null;
	private static final String KEY_CLEAN_SCRAN = "preference_clean_scanner";
	private static final String KEY_RUBBLISH_FILE = "preference_rubblish";
	private static final String KEY_APP_CACHE = "preference_app_cache";
	private static final String KEY_BACKGROUND_APP = "preference_background_app";
	private long mAppCacheTotalSize = 0;
	private ApplicationInfo mScannAppInfo;
	private boolean rubblishCanClick = false;
	private boolean cacheCanClick = false;
	private String mLastedPkgN = null;
	private HMBManagerApplication application = null;
    private List<String> rubblishListSize=new ArrayList<String>();
    private long mTotalSize=0;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate() : ");
		addPreferencesFromResource(R.layout.activity_clean_speed);
		mContext = this.getApplicationContext();
		mCleanV2Manager = ManagerCreatorF.getManager(CleanManager.class);
		pm = getPackageManager();
		application = (HMBManagerApplication) getApplication();
		initView();
		initData();
	}

	private void initView() {
		mCleanScannerPreference = (CleanScannerPreference) findPreference(KEY_CLEAN_SCRAN);
		mRubblishPreference = (PreferenceScreen) findPreference(KEY_RUBBLISH_FILE);
		mRubblishPreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
		mRubblishPreference.setOnPreferenceClickListener(this);
		mAppCachePreference = (PreferenceScreen) findPreference(KEY_APP_CACHE);
		mAppCachePreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
		mAppCachePreference.setOnPreferenceClickListener(this);
		mBackGroundAppPreference = (PreferenceScreen) findPreference(KEY_BACKGROUND_APP);
		mBackGroundAppPreference.setEnabled(false);
		mBackGroundAppPreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
	}

	private void initData() {
		Log.i(TAG, mContext.getFilesDir().getPath() + "------------initData------------scanDisk");
		mRubblishTotalSize=0;
		sendHandlerMessage(MSG_RUBBLISH_SCANNER_START);
		mCleanV2Manager.scanDisk(mScanTaskCallBack, null);
		scanCaches();
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i(TAG, "onStart() : " + application.getRubblishCleanStatus());
		if (application.isCacheCleaned()) {
			cacheResultInfoList.clear();
			installedAppLists.clear();
			mAppCacheTotalSize = 0;
			scanCaches();
			application.setCacheCleaned(false);
		}
		if (application.getRubblishCleanStatus() == 1) {
			long rubblishCleanUpSize = SPUtils.instance(mContext).getLongValue(Constant.RUBBLISH_CLEANUP_SIZE, 0);
			Log.d(TAG, TransUtils.transformShortType(rubblishCleanUpSize, true) + "rubblishCleanUpSize size "
					+ rubblishCleanUpSize);
			Log.i(TAG, mRubblishTotalSize + "--------onStart()--------" + rubblishCleanUpSize);
			if (mRubblishTotalSize >= rubblishCleanUpSize) {
				mRubblishTotalSize = mRubblishTotalSize - rubblishCleanUpSize;
			} else {
				mRubblishTotalSize = 0;
			}
			sendHandlerMessage(MSG_RUBBLISH_SCANNER_END);
			application.setRubblishCleanStatus(0);
		} else if (application.getRubblishCleanStatus() == 2) {
			mRubblishTotalSize = SPUtils.instance(mContext).getLongValue(Constant.RUBBLISH_CLEANUP_SIZE, 0);
			sendHandlerMessage(MSG_RUBBLISH_SCANNER_END);
			application.setRubblishCleanStatus(0);
		}
	}

	public void onResume() {
		Log.i(TAG, "onResume() : ");
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy() : ");
		if (null != mCleanV2Manager) {
			mCleanV2Manager.onDestroy();
		}
		mCleanV2Manager = null;

	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();
		Intent intent = null;
		switch (key) {
		case KEY_RUBBLISH_FILE: {
			if (rubblishCanClick) {
				intent = new Intent(this, RubbishCleanActivity.class);
				startActivity(intent);
			}
			break;
		}
		case KEY_APP_CACHE: {
			if (cacheCanClick) {
				cacheResultHandler();
				intent = new Intent(this, CacheCleanActivity.class);
				Bundle bundle = new Bundle();
				bundle.putParcelableArrayList(Constant.CACHE_KEY, cacheResultInfoList);  
				intent.putExtras(bundle);
				startActivity(intent);
			}
			break;
		}
		}

		return true;
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK
				&& !rubblishCanClick ) {
			mCleanV2Manager.cancelScan(CleanManager.DISK_SCAN_TAG);
		}
		return super.onKeyDown(keyCode, event);
	}   
	
	private void cacheResultHandler() {
		cacheResultInfoList.clear();
		RubblishInfo info;
		AppInfo appInfo = null;
		if (installedAppLists != null && installedAppLists.size() > 0) {
			for (PackageStats ps : installedAppLists) {
				info = new RubblishInfo();
				info.setmPackageName(ps.packageName);
				info.setmSize(ps.cacheSize+ps.externalCacheSize);
				info.setmDataSize(ps.dataSize+ps.externalDataSize);
				Log.d(TAG, ps.dataSize+" =dataSize  packageName="+ps.packageName+"  cacheSize="+ps.cacheSize);
				info.setIsSuggest(1);
				appInfo = ManagerUtils.getAppInfoByPackageName(mContext, ps.packageName);
				if (appInfo != null) {
					info.setmAppName(appInfo.getAppLabel());
				}
				cacheResultInfoList.add(info);
				
			}
		}
	}

	ScanTaskCallBack mScanTaskCallBack = new ScanTaskCallBack();

	class ScanTaskCallBack implements IScanTaskCallBack {

		@Override
		public void onScanStarted() {
			Log.i(TAG, "onScanStarted : ");
			sendHandlerMessage(MSG_RUBBLISH_SCANNER_START);
		}

		public void onRubbishFound(RubbishEntity aRubbish) {
			//Log.i(TAG, "onRubbishFound : ");
			//rubblishCanClick = false;
			if(aRubbish.getPackageName()!=null){
			Message msg = mUIHandler.obtainMessage(MSG_REFRESH_HEAD_PROGRESS);
			msg.obj = mContext.getString(R.string.clean_scanning, aRubbish.getPackageName());
			mUIHandler.sendMessage(msg);
			}
		}

		@Override
		public void onScanCanceled(RubbishHolder aRubbishHolder) {
			Log.i(TAG, "onScanCanceled : " + aRubbishHolder);
			//sendHandlerMessage(MSG_RUBBLISH_SCANNER_END);
			mCleanV2Manager.cancelScan(CleanManager.DISK_SCAN_TAG);
		}

		@Override
		public void onScanFinished(RubbishHolder aRubbishHolder) {
			Log.i(TAG, "onScanFinished : ");
			mCurrentRubbish = aRubbishHolder;
			sbtips = new StringBuffer();
			List<RubbishEntity> rubblishList = null;
			int mSize = 0;
			if (null != aRubbishHolder) {
					if (null != aRubbishHolder.getmApkRubbishes()) {
						rubblishList = aRubbishHolder.getmApkRubbishes();
						collectRubblish(rubblishList);
					}
					if (null != aRubbishHolder.getmSystemRubbishes()) {
						rubblishList = new ArrayList<RubbishEntity>(aRubbishHolder.getmSystemRubbishes().values());
						collectRubblish(rubblishList);

					}
					if (null != aRubbishHolder.getmInstallRubbishes()) {
						rubblishList = new ArrayList<RubbishEntity>(aRubbishHolder.getmInstallRubbishes().values());
						Log.i(TAG, "apk cache remove : " + rubblishList.size());
					}
					if (null != aRubbishHolder.getmUnInstallRubbishes()) {
						rubblishList = new ArrayList<RubbishEntity>(aRubbishHolder.getmUnInstallRubbishes().values());
						collectRubblish(rubblishList);
					}
			        //mRubblishTotalSize = aRubbishHolder.getAllRubbishFileSize();
					mRubblishTotalSize=addTotalRubblish();
			}
			sendHandlerMessage(MSG_RUBBLISH_SCANNER_END);
		}

		@Override
		public void onScanError(int error, RubbishHolder aRubbishHolder) {
			Log.i(TAG, "onScanError : " + error);
			//mUIHandler.sendEmptyMessage(MSG_RUBBLISH_SCANNER_END);
		}

		@Override
		public void onDirectoryChange(String dirPath, int fileCnt) {
			mPathTips = dirPath;
			Message msg = mUIHandler.obtainMessage(MSG_REFRESH_HEAD_PROGRESS);
			msg.obj = mContext.getString(R.string.clean_scanning, dirPath);
			if(!rubblishCanClick)
			mUIHandler.sendMessage(msg);
			Log.i(TAG, "onDirectoryChange : " + mPathTips);
		}

	}
	
	private void collectRubblish(List<RubbishEntity> rubblishList){
		for(RubbishEntity entity:rubblishList){
			rubblishListSize.add(TransUtils.transformShortType(entity.getSize(), true));
		}

	}
	
	private long addTotalRubblish(){
		long mTotalSize=0;
		if(rubblishListSize!=null&&rubblishListSize.size()>0){
			for(String str:rubblishListSize){
				mTotalSize=mTotalSize+TransUtils.unTransformShortType(str);
			}
		}
		return mTotalSize;
	}

	private void sendHandlerMessage(int msg) {
		Message message = mUIHandler.obtainMessage(msg);
		mUIHandler.sendMessage(message);
	}

	private void handlerRunningAPP(AppInfo appinfo, int i, int j) {
		adapter.getChild_text_array()[i][j] = appinfo.getAppLabel();
		adapter.getChild_checkbox()[i][j] = true;
		adapter.getChild_icon()[i][j] = appinfo.getAppIcon();
	}

	private Handler mUIHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_REFRESH_HEAD_PROGRESS:
				mPathTips = (String) msg.obj;
				mCleanScannerPreference.updateScanDetail(mPathTips);
				break;
			case MSG_REFRESH_SPACE_DETAIL:
				break;
			case MSG_RUBBLISH_SCANNER_END:
				Log.d(TAG, TransUtils.transformShortType(mRubblishTotalSize, true) + "mRubblishTotalSize size "
						+ mRubblishTotalSize);
				rubblishCanClick = true;
				mCleanScannerPreference.setmRubblishTotalSize(mRubblishTotalSize);
				Log.d(TAG, "----MSG_RUBBLISH_SCANNER_END------" + mRubblishPreference.getStatus());
				handerCleanDone();
				break;

			case MSG_RUBBLISH_SCANNER_START:
				Log.d(TAG, "----MSG_RUBBLISH_SCANNER_START------");
				rubblishCanClick = false;
				mCleanScannerPreference.setmRubblishTotalSize(-1);
				mCleanScannerPreference.updateScanDetail(mContext.getString(R.string.scanning_start));
				mRubblishPreference.setEnabled(false);
				mRubblishPreference.showStatusArrow(false);
				mRubblishPreference.setStatus(mContext.getString(R.string.sanning));
				break;
			case MSG_REFRESH_CONTER:
				break;
			case MSG_CACHESCANNER_START:
				mCleanScannerPreference.setmAppCahceSize(-1);
				mAppCachePreference.setEnabled(false);
				mAppCachePreference.showStatusArrow(false);
				mAppCachePreference.setStatus(mContext.getString(R.string.sanning));
				break;

			case MSG_CACHESCANNER_END:
				mCleanScannerPreference.setmAppCahceSize(mAppCacheTotalSize);
				cacheCanClick = true;
				handerCleanDone();
				break;
			case MSG_SCANNER_UI_UPDATE:
				mCleanScannerPreference
						.updateScanDetail(mContext.getString(R.string.clean_scanning, mScannAppInfo.packageName));
				break;
			}
		}

	};

	private void handerCleanDone() {
		Log.d(TAG, rubblishCanClick+"---------handerCleanDone-------"+cacheCanClick);
		if (rubblishCanClick && cacheCanClick) {
			mCleanScannerPreference.updataScanResult(mContext.getString(R.string.scanning_finish));
			mRubblishPreference.setEnabled(true);
			mRubblishPreference.showStatusArrow(true);
			mRubblishPreference.setStatus(TransUtils.transformShortType(mRubblishTotalSize, true));
			mAppCachePreference.setEnabled(true);
			mAppCachePreference.showStatusArrow(true);
			mAppCachePreference.setStatus(TransUtils.transformShortType(mAppCacheTotalSize, true));
			mBackGroundAppPreference.setEnabled(true);
			mBackGroundAppPreference.showStatusArrow(true);
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
				List<ApplicationInfo> packageInfos = ManagerUtils.thirdAppApplicationFilter(pm
						.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES),mContext);

				sendHandlerMessage(MSG_CACHESCANNER_START);
				if (packageInfos != null && packageInfos.size() > 0) {
					mLastedPkgN = packageInfos.get(packageInfos.size() - 1).packageName;
					for (ApplicationInfo info : packageInfos) {
						try {
							getPackageSizeInfoMethod.invoke(pm, info.packageName, new MyDataObserver());
							//Thread.sleep(20);
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
			final long cache = pStats.cacheSize+pStats.externalCacheSize;
			final long dataSize=pStats.dataSize+pStats.externalDataSize;
			final String packname = pStats.packageName;
			//Log.d(TAG, ManagerUtils.isFilterPackage(pStats.packageName,mContext) + "----MyDataObserver-----" + packname);
			if (cache > 12288||dataSize>12288) {
				if (pStats != null) {
					if (!installedAppLists.contains(pStats)
							&& !ManagerUtils.isFilterPackage(pStats.packageName,mContext)) {
						installedAppLists.add(pStats);
						mAppCacheTotalSize = mAppCacheTotalSize + cache+dataSize;
						Log.d(TAG, pStats.dataSize + "------------MyData " + cache + " Observer---------" + packname);
					}
				}
			}
			try {
				mScannAppInfo = pm.getApplicationInfo(packname, 0);
				sendHandlerMessage(MSG_SCANNER_UI_UPDATE);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (packname.equals(mLastedPkgN)) {
				sendHandlerMessage(MSG_CACHESCANNER_END);
			}
		}

	}

}