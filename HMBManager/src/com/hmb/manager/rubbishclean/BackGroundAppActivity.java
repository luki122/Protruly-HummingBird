package com.hmb.manager.rubbishclean;

import hb.app.HbActivity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import hb.widget.HbListView;

import hb.widget.ActionMode;
import hb.widget.ActionModeListener;
import hb.widget.HbListView;
import hb.widget.ActionMode.Item;
import tmsdk.fg.module.cleanV2.RubbishEntity;
import tmsdkobf.r;

import com.hmb.manager.Constant;
import com.hmb.manager.R;
import com.hmb.manager.bean.AppInfo;
import com.hmb.manager.utils.ManagerUtils;
import com.hmb.manager.utils.SPUtils;
import com.hmb.manager.utils.TransUtils;
import com.hmb.manager.adapter.APPAdapter;
import android.content.pm.IPackageStatsObserver;
import hb.app.dialog.AlertDialog;

public class BackGroundAppActivity extends HbActivity
		implements OnItemClickListener, OnItemLongClickListener, OnClickListener {
	private HbListView mListView;
	private List<AppInfo> mRunningAppList = null;
	private PackageManager pm;
	private ActivityManager activityMgr;
	private List<AppInfo> killedItems = new ArrayList<AppInfo>();
	private long killedPkgSize = 0;
	private static final String TAG = "BackGroundAppActivity";
	AsyncTask<Void, Integer, List<AppInfo>> mTask;
	private Method mGetPackageSizeInfoMethod;
	private Context mContext;
	private APPAdapter mAdapter = null;
	private ArrayMap<Integer, Boolean> mSelectedItems = new ArrayMap<Integer, Boolean>();
	private boolean mSelectAll = false;
	private Button forceStopBtn = null;
	private static final int APPS_INFO_UPATE = 0x01;
	private static final int APP_KILL_END = 0x02;
	private static final int APPS_INFO_NULL = 0x03;
	private static final int APPS_QUERY_START = 0x04;
	private Map<Integer, AppInfo> runningAppMap = new HashMap<Integer, AppInfo>();
	private List<ApplicationInfo> listAppcations = null;
	private Map<String, ActivityManager.RunningAppProcessInfo> thirdPgkAppMap = null;
	private Object mSync = new Object();
	private boolean mEditMode = false;
	private Dialog dialog = null;
	private TextView bg_app_num;
	private LinearLayout bg_main_null;
	private LinearLayout bg_app_num_layout,progressLin,bg_listLayout,bottomLin;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHbContentView(R.layout.activity_bg_app);
		mContext = this.getApplicationContext();
		mListView = (HbListView) findViewById(android.R.id.list);
		bg_main_null = (LinearLayout) findViewById(R.id.bg_main_null);
		bg_app_num_layout = (LinearLayout) findViewById(R.id.bg_app_num_layout);
		bg_app_num = (TextView) findViewById(R.id.bg_app_num);
		forceStopBtn = (Button) findViewById(R.id.forceStopBtn);
		progressLin = (LinearLayout) findViewById(R.id.bgprogressLin);
		bg_listLayout=(LinearLayout) findViewById(R.id.bg_listLayout);
		bottomLin=(LinearLayout) findViewById(R.id.bottomLin);
		forceStopBtn.setOnClickListener(this);
		pm = getPackageManager();
		activityMgr = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
		try {
			mGetPackageSizeInfoMethod = mContext.getPackageManager().getClass().getMethod("getPackageSizeInfo",
					String.class, IPackageStatsObserver.class);

		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		setupActionModeWithDecor(getToolbar());
		initData();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	private void enterEditMode(boolean editMode) {
		mEditMode = editMode;
		showActionMode(editMode);
		if (mSelectedItems != null && mSelectedItems.size() > 0) {
			actionBarHandler();
			mAdapter.notifyDataSetChanged();
		}
	}

	private void actionBarHandler(){
		boolean selectedOne = false;
		for (int i = 0; i < mSelectedItems.size(); ++i) {
			if (mSelectedItems.get(i)) {
				selectedOne = true;
				break;
			}
		}
		if (selectedOne) {
			// Toast.makeText(mContext, R.string.select_one_more_message,
			// Toast.LENGTH_LONG).show();
			forceStopBtn.setEnabled(true);
			forceStopBtn.setText(mContext.getString(R.string.app_force_stop, countSelectedNUM(mSelectedItems)));
			if(mEditMode){
				if(countSelectedNUM(mSelectedItems)==mSelectedItems.size()){
				getActionMode().setPositiveText(mContext.getString(R.string.un_select_all));
				mSelectAll=true;
				}
			}
		} else {
			forceStopBtn.setEnabled(false);
			forceStopBtn.setText(mContext.getString(R.string.app_force_stop_null));
			if(mEditMode){
				getActionMode().setPositiveText(mContext.getString(R.string.select_all));
				mSelectAll=false;
			}
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		CheckBox checkBox = (CheckBox) view.findViewById(R.id.app_check_box);

		if (checkBox.isChecked()) {
			mSelectedItems.put(position, false);
		} else {
			mSelectedItems.put(position, true);
		}
		actionBarHandler();
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
		enterEditMode(true);
		return true;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (v.getId() == R.id.forceStopBtn) {
			dialog = new AlertDialog.Builder(this).setTitle(R.string.app_kill_notification)
					.setMessage(R.string.app_kill_message)
					.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					}).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							killedPkgSize=0;
							new Thread(new KillAppThread()).start();
							forceStopBtn.setEnabled(false);
						}
					}).create();
			dialog.show();
		}
	}

	private int countSelectedNUM(ArrayMap<Integer, Boolean> mSelectedItems) {
		int mSelectCon = 0;
		if (mSelectedItems != null && mSelectedItems.size() > 0) {
			for (int i = 0; i < mSelectedItems.size(); ++i) {
				if (mSelectedItems.get(i)) {
					++mSelectCon;
				}
			}
		}
		return mSelectCon;
	}

	private void initData() {
		Message msg = mUIHandler.obtainMessage(APPS_QUERY_START);
		mUIHandler.sendMessage(msg);
		if(dialog!=null){
			dialog.dismiss();
		}
		new Thread(new QueryAPPThread()).start();

		setActionModeListener(new ActionModeListener() {

			@Override
			public void onActionModeShow(ActionMode actionMode) {
				Log.e(TAG, "onActionModeShow");
			}

			@Override
			public void onActionModeDismiss(ActionMode actionMode) {
				Log.e(TAG, "onActionModeDismiss");
			}

			@Override
			public void onActionItemClicked(Item item) {
				switch (item.getItemId()) {
				case ActionMode.NAGATIVE_BUTTON:
					enterEditMode(false);
					break;

				case ActionMode.POSITIVE_BUTTON:
					mSelectAll = !mSelectAll;
					Log.d(TAG, "positive button on clicked, selected all : " + mSelectAll);

					if (mSelectAll) {
						getActionMode().setPositiveText(getResources().getString(R.string.un_select_all));
					} else {
						getActionMode().setPositiveText(getResources().getString(R.string.select_all));
					}

					selectAllHandler(mSelectAll);
					break;

				default:
					break;
				}
			}
		});
	}
	
	
	class QueryAPPThread implements Runnable {

		@Override
		public void run() {
			runningAppMap.clear();
			queryAllRunningAppInfo();
		}
	}

	private void selectAllHandler(boolean selectAll) {
        Log.d(TAG, "select all handler, select all : " + selectAll);

		if (mSelectedItems != null && mSelectedItems.size() > 0) {
			for (int i = 0; i < mSelectedItems.size(); i++) {
				mSelectedItems.put(i, selectAll);
			}
			if (selectAll) {
				forceStopBtn.setEnabled(true);
				forceStopBtn.setText(mContext.getString(R.string.app_force_stop, countSelectedNUM(mSelectedItems)));
			} else {
				forceStopBtn.setEnabled(false);
				forceStopBtn.setText(mContext.getString(R.string.app_force_stop_null));
			}

			mAdapter.notifyDataSetChanged();
		}

	}

	@Override
	public void onNavigationClicked(View view) {

		onBackPressed();
	}

	@Override
	public void onBackPressed() {
		if (mEditMode) {
			enterEditMode(false);
			return;
		}
		super.onBackPressed();
	}

	private void queryAllRunningAppInfo() {
		pm = this.getPackageManager();
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
		thirdPgkAppMap = ManagerUtils.thirdBgApplicationFilter(pgkProcessAppMap,mContext);
		if (thirdPgkAppMap.size() > 0) {
			for (ApplicationInfo app : listAppcations) {
				if ((app.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) {
					if (app.packageName.equals(getPackageName()))
						continue;
					if (thirdPgkAppMap.containsKey(app.packageName)) {
						final int pid = thirdPgkAppMap.get(app.packageName).pid;
						String processName = thirdPgkAppMap.get(app.packageName).processName;
						final AppInfo appInfo = ManagerUtils.getAppInfo(this, app, pid, processName);
						try {
							mGetPackageSizeInfoMethod.invoke(mContext.getPackageManager(),
									new Object[] { app.packageName, new IPackageStatsObserver.Stub() {
										@Override
										public void onGetStatsCompleted(PackageStats pStats, boolean succeeded)
												throws RemoteException {
											synchronized (appInfo) {
												Log.i(TAG, "packageName " + appInfo.getPkgName());
												appInfo.setPkgSize(
														pStats.cacheSize + pStats.codeSize + pStats.dataSize);
												Message msg = mUIHandler.obtainMessage(APPS_INFO_UPATE);
												msg.obj = appInfo;
												msg.arg1 = pid;
												mUIHandler.sendMessage(msg);
											}
										}
									} });
						} catch (Exception e) {
						}

					}
				}
			}
		}else{
			Message msg = mUIHandler.obtainMessage(APPS_INFO_NULL);
			mUIHandler.sendMessage(msg);
		}
		//return new ArrayList<AppInfo>(runningAppMap.values());
	}


	class KillAppThread implements Runnable {

		@Override
		public void run() {
			AppInfo app = null;
			List<AppInfo> appinfos = mAdapter.getMlistAppInfo();
			if (appinfos != null && appinfos.size() > 0) {
				for (int i = 0; i < appinfos.size(); ++i) {
					if (mSelectedItems.get(i)) {
						app = appinfos.get(i);
						Log.d(TAG, "KillAppThread app "+app.getPkgName());
						activityMgr.forceStopPackage(app.getPkgName());
						killedPkgSize = killedPkgSize + app.getPkgSize();
						killedItems.add(app);
					}
				}

				if (killedItems != null && killedItems.size() > 0) {
					for (AppInfo s : killedItems) {
						appinfos.remove(s);
					}
				}
				mSelectedItems.clear();
				for (int i = 0; i < appinfos.size(); i++) {
					mSelectedItems.put(i, false);
				}
				Message msg = mUIHandler.obtainMessage(APP_KILL_END);
				mUIHandler.sendMessage(msg);

			}

		}
	}

	private Handler mUIHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case APPS_INFO_UPATE:
				int pid = msg.arg1;
				AppInfo appInfo = (AppInfo) msg.obj;
				if (!runningAppMap.containsKey(pid))
					runningAppMap.put(pid, appInfo);
				Log.d(TAG, thirdPgkAppMap.size()+" thirdPgkAppMap  and  runningAppMap="+runningAppMap.size());
				if (runningAppMap.size() == thirdPgkAppMap.size()&&runningAppMap.size()>0) {

					List<AppInfo> result=new ArrayList<AppInfo>(runningAppMap.values());
						for (int i = 0; i < result.size(); i++) {
							mSelectedItems.put(i, true);
						}
						if(mEditMode){
							getActionMode().setPositiveText(mContext.getString(R.string.un_select_all));
							mSelectAll=true;
						}
						progressLin.setVisibility(View.GONE);
						bg_listLayout.setVisibility(View.VISIBLE);
						bottomLin.setVisibility(View.VISIBLE);
						mAdapter = new APPAdapter(mContext, result, mSelectedItems);
						mListView.setAdapter(mAdapter);
						forceStopBtn.setEnabled(true);
						forceStopBtn.setVisibility(View.VISIBLE);
						bg_app_num_layout.setVisibility(View.VISIBLE);
						bg_main_null.setVisibility(View.GONE);
						forceStopBtn.setText(mContext.getString(R.string.app_force_stop, mSelectedItems.size()));
						bg_app_num.setText(mContext.getString(R.string.bg_app_num_message, mSelectedItems.size()));
				}
				break;
			case APPS_INFO_NULL:
				forceStopBtn.setEnabled(false);
				forceStopBtn.setText(mContext.getString(R.string.app_force_stop_null));
				forceStopBtn.setVisibility(View.GONE);
				bg_main_null.setVisibility(View.VISIBLE);
				bg_app_num_layout.setVisibility(View.GONE);
				progressLin.setVisibility(View.GONE);
				break;
			case APPS_QUERY_START:
				progressLin.setVisibility(View.VISIBLE);
				bg_listLayout.setVisibility(View.GONE);
				bottomLin.setVisibility(View.GONE);
				bg_main_null.setVisibility(View.GONE);
                                bg_app_num_layout.setVisibility(View.GONE);
				break;
			case APP_KILL_END:
				dialog.dismiss();
				forceStopBtn.setEnabled(false);
				forceStopBtn.setText(mContext.getString(R.string.app_force_stop_null));
				mAdapter.notifyDataSetChanged();
				if(mSelectedItems.size()>0){
				bg_app_num.setText(mContext.getString(R.string.bg_app_num_message, mSelectedItems.size()));
				}else{
				forceStopBtn.setVisibility(View.GONE);
				bg_main_null.setVisibility(View.VISIBLE);
				bg_app_num_layout.setVisibility(View.GONE);
				}
				if(mEditMode){
				showActionMode(false);
				mEditMode=false;
				}
				Toast.makeText(mContext, mContext.getString(R.string.kill_app_result,
						TransUtils.transformShortType(killedPkgSize, true)), Toast.LENGTH_LONG).show();
		        SPUtils.instance(mContext).setLongValue(Constant.MEMORY_CLEAN_TIME, 0,
		                System.currentTimeMillis());
				break;
			}
		}
	};

	private void forceStopPackage(String pkgName) {
		// activityMgr.killBackgroundProcesses(pkgName);
		activityMgr.forceStopPackage(pkgName);
	}
}
