package com.hmb.manager.rubbishclean;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.graphics.drawable.Drawable;
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
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import hb.app.HbActivity;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hb.widget.HbListView;

import com.hmb.manager.Constant;
import com.hmb.manager.HMBManagerApplication;
import com.hmb.manager.R;
import hb.widget.ActionMode;
import hb.widget.ActionModeListener;
import hb.widget.HbListView;
import hb.widget.ActionMode.Item;
import tmsdk.fg.creator.ManagerCreatorF;
import tmsdk.fg.module.cleanV2.CleanManager;
import tmsdk.fg.module.cleanV2.IScanTaskCallBack;
import tmsdk.fg.module.cleanV2.RubbishEntity;
import tmsdk.fg.module.cleanV2.RubbishHolder;

import com.hmb.manager.adapter.APPAdapter;
import com.hmb.manager.adapter.CacheExpandableListAdapter;
import com.hmb.manager.adapter.RubblishExpandableListAdapter;
import com.hmb.manager.bean.AppInfo;
import com.hmb.manager.bean.RubblishInfo;
import com.hmb.manager.utils.ManagerUtils;
import com.hmb.manager.utils.SPUtils;
import com.hmb.manager.utils.TransUtils;
import android.content.pm.IPackageDataObserver;
import hb.app.dialog.AlertDialog;

public class CacheCleanActivity extends HbActivity implements OnItemLongClickListener, OnClickListener {
	private static final String TAG = "CacheCleanActivity";
	private ExpandableListView expandableListView;
	private boolean mSelectAll = false;
	private Context mContext;
	private Button forceCacheCleanBtn = null;
	private LinearLayout cache_main_null;
	private LinearLayout cache_listLayout;
	private List<PackageStats> installedAppLists = new ArrayList<PackageStats>();
	private String mLastedPkgN = null;
	private static final int CACHE_INFO_UPATE = 0x01;
	private static final int CACHE_CLEAN_END = 0x02;
	private static final int DATA_CLEAN_END = 0x03;
	private ApplicationInfo mScannAppInfo;
	private PackageManager pm;
	private boolean mEditMode = false;
	private boolean[] mgroup_checked = null;
	private List<RubblishInfo> resultInfoList;
	private List<RubblishInfo> selectedInfoList = new ArrayList<RubblishInfo>();
	private List<RubblishInfo> cacheInfoList = new ArrayList<RubblishInfo>();
	private List<RubblishInfo> dataInfoList = new ArrayList<RubblishInfo>();
	private List<RubblishInfo> unSelectedInfoList, unCacheInfoList, unDataInfoList = null;
	private CacheExpandableListAdapter mAdapter = null;
	private Dialog dialog = null;
	CleanManager mCleanV2Manager;
	private HMBManagerApplication application = null;
	private Method mFreeStorageAndNotifyMethod;
	private boolean isCacheClean, isDataClean = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHbContentView(R.layout.activity_cache_clean);
		mContext = this.getApplicationContext();
		expandableListView = (ExpandableListView) findViewById(R.id.cacheList);
		expandableListView.setOnItemLongClickListener(this);
		Bundle bundle = getIntent().getExtras();
		resultInfoList = bundle.getParcelableArrayList(Constant.CACHE_KEY);
		cache_main_null = (LinearLayout) findViewById(R.id.cache_main_null);
		cache_listLayout = (LinearLayout) findViewById(R.id.cache_listLayout);
		forceCacheCleanBtn = (Button) findViewById(R.id.forceCacheCleanBtn);
		forceCacheCleanBtn.setOnClickListener(this);
		pm = getPackageManager();
		mCleanV2Manager = ManagerCreatorF.getManager(CleanManager.class);
		application = (HMBManagerApplication) getApplication();
		try {
			mFreeStorageAndNotifyMethod = getPackageManager().getClass().getMethod("freeStorageAndNotify", long.class,
					IPackageDataObserver.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		setExpandListView();
	}

	@Override
	protected void onStart() {
		super.onStart();
		setupActionModeWithDecor(getToolbar());
	}

	@Override
	public void onResume() {
		super.onResume();

		initData();
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (null != mCleanV2Manager) {
			mCleanV2Manager.onDestroy();
		}
		mCleanV2Manager = null;
	}

	private void enterEditMode(boolean editMode) {
		mEditMode = editMode;
		showActionMode(editMode);
		if (editMode) {
			if (ManagerUtils.selectAll(mAdapter.getChild_checkbox()) == 1) {
				getActionMode().setPositiveText(mContext.getString(R.string.un_select_all));
				mSelectAll = true;
			} else if (ManagerUtils.selectAll(mAdapter.getChild_checkbox()) == 0) {
				getActionMode().setPositiveText(mContext.getString(R.string.select_all));
				mSelectAll = false;
			}
		}
		if (mgroup_checked != null && mgroup_checked.length > 0) {
			mAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		enterEditMode(true);
		return true;
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.forceCacheCleanBtn) {
			dialog = new AlertDialog.Builder(this).setTitle(R.string.app_kill_notification)
					.setMessage(R.string.rubblish_clean_message)
					.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					}).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// new Thread(new KillAppThread()).start();
							cleanCache();
							if (mEditMode) {
								showActionMode(false);
								mEditMode = false;
							}
							application.setCacheCleaned(true);
							forceCacheCleanBtn.setEnabled(false);
						}
					}).create();
			dialog.show();
		}
	}

	private void initData() {
		// scanCaches();

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
					if (ManagerUtils.selectAll(mAdapter.getChild_checkbox()) == 1) {
						mSelectAll = true;
					} else if (ManagerUtils.selectAll(mAdapter.getChild_checkbox()) == 0) {
						mSelectAll = false;
					}
					mSelectAll = !mSelectAll;
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

	private void setExpandListView() {

		forceCacheCleanBtn.setEnabled(true);
		mAdapter = new CacheExpandableListAdapter(mContext, resultInfoList);
		mAdapter.setCacheCleanBtn(forceCacheCleanBtn);
		mAdapter.setActionModeView(getActionMode());
		expandableListView.setAdapter(mAdapter);
		expandableListView.setGroupIndicator(null);
		initArrayList(resultInfoList);
		expandableListView.setOnGroupClickListener(new OnGroupClickListener() {

			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				((BaseExpandableListAdapter) mAdapter).notifyDataSetChanged();
				return false;
			}
		});

		expandableListView.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition,
					long id) {
				mAdapter.setChild_groupId(groupPosition);
				mAdapter.setChild_childId(childPosition);
				((BaseExpandableListAdapter) mAdapter).notifyDataSetChanged();
				return false;
			}
		});

		if (resultInfoList.size() == 0) {
			resultNullHandler();
		}
		if (mEditMode && resultInfoList.size() > 0) {
			getActionMode().setPositiveText(mContext.getString(R.string.select_all));
			mSelectAll=false;
		}
	}

	private void resultNullHandler() {
		cache_listLayout.setVisibility(View.GONE);
		cache_main_null.setVisibility(View.VISIBLE);
		forceCacheCleanBtn.setEnabled(false);
		forceCacheCleanBtn.setVisibility(View.GONE);
		forceCacheCleanBtn.setText(mContext.getString(R.string.clean_btn_message));
		cache_main_null.setVisibility(View.VISIBLE);
	}

	private void initArrayList(List<RubblishInfo> info) {
		int mSize = 0;
		boolean[] group_checked = null;
		String[][] child_text_array = null;
		String[][] child_text_size = null;
		String[] group_title_arry = null;
		boolean[][] child_checkbox = null;
		AppInfo appInfo = null;
		PackageStats ps = null;
		if (info != null && info.size() > 0) {
			mSize = info.size();
			group_checked = new boolean[mSize];
			group_title_arry = new String[mSize];
			child_text_array = new String[mSize][];
			child_text_size = new String[mSize][];
			child_checkbox = new boolean[mSize][];
			for (int i = 0; i < mSize; ++i) {
				appInfo = getAPPInfo(i);
				RubblishInfo r = info.get(i);
				if (getInitChildSize(r) == 2) {
					child_text_array[i] = new String[2];
					child_text_size[i] = new String[2];
					child_checkbox[i] = new boolean[2];
					group_checked[i] = true;
					child_text_array[i][0] = mContext.getString(R.string.cache_data);
					child_text_array[i][1] = mContext.getString(R.string.data_data);
					if (appInfo != null)
						group_title_arry[i] = appInfo.getAppLabel();
					child_text_size[i][0] = TransUtils.transformShortType(r.getmSize(), true);
					child_text_size[i][1] = TransUtils.transformShortType(r.getmDataSize(), true);
					child_checkbox[i][0] = true;
					child_checkbox[i][1] = false;
				} else if (getInitChildSize(r) == 1) {
					child_text_array[i] = new String[1];
					child_text_size[i] = new String[1];
					child_checkbox[i] = new boolean[1];
					if (r.getmSize() > 12288) {
						child_text_array[i][0] = mContext.getString(R.string.cache_data);
						child_text_size[i][0] = TransUtils.transformShortType(r.getmSize(), true);
						child_checkbox[i][0] = true;
					} else if (r.getmDataSize() > 12288) {
						child_text_array[i][0] = mContext.getString(R.string.data_data);
						child_text_size[i][0] = TransUtils.transformShortType(r.getmDataSize(), true);
						child_checkbox[i][0] = false;
					}
				}
			}
			mgroup_checked = group_checked;
			mAdapter.setGroup_checked(group_checked);
			mAdapter.setGroup_title_arry(group_title_arry);
			mAdapter.setChild_text_array(child_text_array);
			mAdapter.setChild_text_size(child_text_size);
			mAdapter.setChild_checkbox(child_checkbox);
		}
	}
	
	private int getInitChildSize(RubblishInfo info){
		if(info.getmSize()>12288&&info.getmDataSize()>12288){
			return 2;
		}
	    return 1;
	}

	private void refreshArrayList(List<RubblishInfo> info) {
		int mSize = 0;
		boolean[] group_checked = null;
		String[][] child_text_array = null;
		String[][] child_text_size = null;
		String[] group_title_arry = null;
		boolean[][] child_checkbox = null;
		AppInfo appInfo = null;
		RubblishInfo r = null;
		PackageStats ps = null;
		if (info != null && info.size() > 0) {
			mSize = info.size();
			group_checked = new boolean[mSize];
			group_title_arry = new String[mSize];
			child_text_array = new String[mSize][];
			child_text_size = new String[mSize][];
			child_checkbox = new boolean[mSize][];
			for (int i = 0; i < mSize; ++i) {
				appInfo = getAPPInfo(i);
				r = info.get(i);
				//group_checked[i] = true;
				if (appInfo != null)
					group_title_arry[i] = appInfo.getAppLabel();
				if (getChildSize(r) == 2) {
					child_text_array[i] = new String[2];
					child_text_size[i] = new String[2];
					child_checkbox[i] = new boolean[2];
					child_text_array[i][0] = mContext.getString(R.string.cache_data);
					child_text_array[i][1] = mContext.getString(R.string.data_data);
					child_text_size[i][0] = TransUtils.transformShortType(r.getmSize(), true);
					child_text_size[i][1] = TransUtils.transformShortType(r.getmDataSize(), true);
					child_checkbox[i][0] = true;
					child_checkbox[i][1] = false;
				} else if (getChildSize(r) == 1) {
					child_text_array[i] = new String[1];
					child_text_size[i] = new String[1];
					child_checkbox[i] = new boolean[1];
					if (unCacheInfoList.contains(r)) {
						child_text_array[i][0] = mContext.getString(R.string.cache_data);
						child_text_size[i][0] = TransUtils.transformShortType(r.getmSize(), true);
						child_checkbox[i][0] = true;
					} else if (unDataInfoList.contains(r)) {
						child_text_array[i][0] = mContext.getString(R.string.data_data);
						child_text_size[i][0] = TransUtils.transformShortType(r.getmDataSize(), true);
						child_checkbox[i][0] = false;
					}
				}
			}
			mgroup_checked = group_checked;
			mAdapter.setGroup_checked(group_checked);
			mAdapter.setGroup_title_arry(group_title_arry);
			mAdapter.setChild_text_array(child_text_array);
			mAdapter.setChild_text_size(child_text_size);
			mAdapter.setChild_checkbox(child_checkbox);
		}
	}

	private int getChildSize(RubblishInfo appInfo) {
		if (unCacheInfoList.contains(appInfo) && unDataInfoList.contains(appInfo)) {
			return 2;
		}else if(unCacheInfoList.contains(appInfo) &&dataInfoList.contains(appInfo)){
			return 0;
		}
		return 1;
	}

	private AppInfo getAPPInfo(int id) {
		AppInfo appInfo = null;
		if (resultInfoList != null && resultInfoList.size() > 0) {
			if (id < resultInfoList.size()) {
				RubblishInfo ps = resultInfoList.get(id);
				if (ps != null) {
					appInfo = ManagerUtils.getAppInfoByPackageName(mContext, ps.getmPackageName());

					return appInfo;

				}
			}
		}
		return null;
	}

	private void selectAllHandler(boolean selectAll) {
		boolean[][] child_checkbox = mAdapter.getChild_checkbox();
		if (mgroup_checked != null && mgroup_checked.length > 0) {
			for (int i = 0; i < mgroup_checked.length; i++) {
				mgroup_checked[i] = selectAll;
				if (child_checkbox != null && child_checkbox[i].length > 0) {
					for (int j = 0; j < child_checkbox[i].length; ++j) {
						child_checkbox[i][j] = selectAll;
					}
					// child_checkbox[i][0] = selectAll;
					// child_checkbox[i][1] = selectAll;
				}
			}
			mAdapter.setGroup_checked(mgroup_checked);
			mAdapter.setChild_checkbox(child_checkbox);
			if (selectAll) {
				forceCacheCleanBtn.setText(mContext.getString(R.string.app_force_stop));
			} else {
				forceCacheCleanBtn.setEnabled(false);
				forceCacheCleanBtn.setText(mContext.getString(R.string.app_force_stop_null));
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
				List<PackageInfo> packageInfos = pm.getInstalledPackages(0);

				if (packageInfos != null && packageInfos.size() > 0) {
					mLastedPkgN = packageInfos.get(packageInfos.size() - 1).packageName;
					for (PackageInfo info : packageInfos) {
						try {
							getPackageSizeInfoMethod.invoke(pm, info.packageName, new MyDataObserver());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			};
		}.start();

	}

	private void cleanCache() {
		RubblishInfo mInfo = null;
		boolean[][] child_checkbox = mAdapter.getChild_checkbox();
		String[][] child_text_array = mAdapter.getChild_text_array();
		ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		resetVar();
		unSelectedInfoList = new ArrayList<RubblishInfo>();
		unCacheInfoList = new ArrayList<RubblishInfo>();
		unDataInfoList = new ArrayList<RubblishInfo>();
		if (child_checkbox != null && child_checkbox.length > 0) {
			for (int i = 0; i < child_checkbox.length; ++i) {
				mInfo = resultInfoList.get(i);
				if (child_checkbox[i].length == 2) {
					if (child_checkbox[i][0]) {
						// selectedInfoList.add(resultInfoList.get(i));
						cacheInfoList.add(mInfo);
					} else {
						if (!unSelectedInfoList.contains(mInfo)) {
							unSelectedInfoList.add(mInfo);
						}
						unCacheInfoList.add(mInfo);
					}
					if (child_checkbox[i][1]) {
						// selectedInfoList.add(resultInfoList.get(i));
						dataInfoList.add(mInfo);
						if (unSelectedInfoList.contains(mInfo)) {
							unSelectedInfoList.remove(mInfo);
						}
					} else {
						if (!unSelectedInfoList.contains(mInfo)) {
							unSelectedInfoList.add(mInfo);
						}
						unDataInfoList.add(mInfo);
					}
				} else if (child_checkbox[i].length == 1) {
					if (child_text_array[i] != null && child_text_array[i].length > 0) {
						if (child_text_array[i][0].equals(mContext.getString(R.string.cache_data))) {
							if (child_checkbox[i][0]) {
								// selectedInfoList.add(resultInfoList.get(i));
								cacheInfoList.add(mInfo);
							} else {
								if (!unSelectedInfoList.contains(mInfo)) {
									unSelectedInfoList.add(mInfo);
								}
								unCacheInfoList.add(mInfo);
							}
						} else {
							if (child_checkbox[i][0]) {
								// selectedInfoList.add(resultInfoList.get(i));
								dataInfoList.add(mInfo);
							} else {
								if (!unSelectedInfoList.contains(mInfo)) {
									unSelectedInfoList.add(mInfo);
								}
								unDataInfoList.add(mInfo);
							}
						}
					}
				}
			}
			Log.d(TAG, "unCacheInfoList= " + unCacheInfoList.size() + " unDataInfoList=" + unDataInfoList.size());
			// unSelectedInfoList = mergeList(unCacheInfoList, unDataInfoList);
			Log.d(TAG, "unSelectedInfoList=" + unSelectedInfoList.size());
			if (cacheInfoList != null && cacheInfoList.size() > 0) {
				String mLastCachePkgName = cacheInfoList.get(cacheInfoList.size() - 1).getmPackageName();
				mClearCachebserver.setmCleanType(0);
				mClearCachebserver.setmLastPackageName(mLastCachePkgName);
				for (RubblishInfo info : cacheInfoList) {
					cleanAppCache(info.getmPackageName());
				}
			} else {
				isCacheClean = true;
			}
			if (dataInfoList != null && dataInfoList.size() > 0) {
				String mLastDataPkgName = dataInfoList.get(dataInfoList.size() - 1).getmPackageName();
				mClearDatabserver.setmCleanType(1);
				mClearDatabserver.setmLastPackageName(mLastDataPkgName);
				for (RubblishInfo info : dataInfoList) {
					am.clearApplicationUserData(info.getmPackageName(), mClearDatabserver);
				}
			} else {
				isDataClean = true;
			}
		}
	}

	private void resetVar() {
		selectedInfoList.clear();
		cacheInfoList.clear();
		dataInfoList.clear();
		isCacheClean = false;
		isDataClean = false;
	}

	private List<RubblishInfo> mergeList(List<RubblishInfo> unCacheInfoList, List<RubblishInfo> unDataInfoList) {
		Set<RubblishInfo> set = new HashSet<RubblishInfo>();
		if (unCacheInfoList != null && unCacheInfoList.size() > 0) {
			set.addAll(unCacheInfoList);
		}
		if (unDataInfoList != null && unDataInfoList.size() > 0) {
			for (RubblishInfo info : unDataInfoList) {
				set.add(info);
			}
		}
		return new ArrayList<RubblishInfo>(set);
	}

	private void cleanAppCache(String packageName) {

		pm.deleteApplicationCacheFiles(packageName, mClearCachebserver);

	}

	ClearUserDataObserver mClearDatabserver = new ClearUserDataObserver();
	ClearUserDataObserver mClearCachebserver = new ClearUserDataObserver();

	class ClearUserDataObserver extends IPackageDataObserver.Stub {
		private int mCleanType;
		private String mLastPackageName;

		public void onRemoveCompleted(final String packageName, final boolean succeeded) {
			Log.d(TAG, "packageName " + packageName + "   succeeded  " + succeeded);
			Log.d(TAG, "mLastPackageName =" + mLastPackageName);
			if (mLastPackageName.equals(packageName)) {
				if (mCleanType == 0) {
					isCacheClean = true;
					sendHandlerMessage(CACHE_CLEAN_END);
				} else if (mCleanType == 1) {
					isDataClean = true;
					sendHandlerMessage(DATA_CLEAN_END);
				}
			}
		}

		public String getmLastPackageName() {
			return mLastPackageName;
		}

		public void setmLastPackageName(String mLastPackageName) {
			this.mLastPackageName = mLastPackageName;
		}

		public int getmCleanType() {
			return mCleanType;
		}

		public void setmCleanType(int mCleanType) {
			this.mCleanType = mCleanType;
		}

	}

	private void refreshList() {
		if (unSelectedInfoList != null && unSelectedInfoList.size() > 0) {
			refreshArrayList(unSelectedInfoList);
			mAdapter.setMlistAppInfo(unSelectedInfoList);
			resultInfoList = unSelectedInfoList;
			((BaseExpandableListAdapter) mAdapter).notifyDataSetChanged();
		} else {
			resultNullHandler();
		}
	}

	private int getIntLength(boolean[] child_checkbox) {
		int size = 0;
		if (child_checkbox != null && child_checkbox.length > 0) {
			for (int i = 0; i < child_checkbox.length; ++i) {
				if (!child_checkbox[i]) {
					++size;
				}
			}
		}
		return size;
	}

	private Handler mUIHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CACHE_INFO_UPATE:
				break;
			case CACHE_CLEAN_END:
				// setExpandListView();
				cleanProcess();
				break;
			case DATA_CLEAN_END:
				cleanProcess();
				break;
			}
		}
	};

	private void cleanProcess() {
		if (isCacheClean && isDataClean) {
			refreshList();
			Toast.makeText(mContext, mContext.getString(R.string.cache_clean_result,
					TransUtils.transformShortType(mAdapter.getCleanSize(), true)), Toast.LENGTH_LONG).show();
			forceCacheCleanBtn.setEnabled(false);
			SPUtils.instance(mContext).setLongValue(Constant.CACHE_CLEANUP_TIME, 0, System.currentTimeMillis());
		}
	}

	private class MyDataObserver extends IPackageStatsObserver.Stub {

		@Override
		public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) throws RemoteException {
			final long cache = pStats.cacheSize;
			final String packname = pStats.packageName;
			if (cache > 12288) {
				// mAppCacheTotalSize = mAppCacheTotalSize + cache;
				Log.d(TAG, pStats.dataSize + "------------MyData " + cache + " Observer---------" + packname);
				if (pStats != null) {
					if (!installedAppLists.contains(pStats)) {
						installedAppLists.add(pStats);
					}
				}
			}
			try {
				mScannAppInfo = pm.getApplicationInfo(packname, 0);
				sendHandlerMessage(CACHE_INFO_UPATE);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (packname.equals(mLastedPkgN)) {
				sendHandlerMessage(CACHE_CLEAN_END);
			}
		}

	}

	private void sendHandlerMessage(int msg) {
		Message message = mUIHandler.obtainMessage(msg);
		mUIHandler.sendMessage(message);
	}

}
