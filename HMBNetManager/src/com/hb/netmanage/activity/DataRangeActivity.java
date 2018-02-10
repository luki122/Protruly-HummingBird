package com.hb.netmanage.activity;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicyManager;
import android.net.NetworkTemplate;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hb.netmanage.DataManagerApplication;
import com.hb.netmanage.R;
import com.hb.netmanage.adapter.MainFragementAdater;
import com.hb.netmanage.adapter.RangeAppAdapter;
import com.hb.netmanage.entity.AppItem;
import com.hb.netmanage.fragement.RangeFragment;
import com.hb.netmanage.receiver.AppReceiver;
import com.hb.netmanage.receiver.SimStateReceiver;
import com.hb.netmanage.utils.LogUtil;
import com.hb.netmanage.utils.PreferenceUtil;
import com.hb.netmanage.utils.ToolsUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.SynchronousQueue;

import hb.view.menu.PopupMenu;
import hb.widget.ViewPager;
import hb.widget.tab.TabLayout;
import hb.widget.toolbar.Toolbar;

import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;
/**
 * 流量排行展示界面
 * @author zhaolaichao
 */
public class DataRangeActivity extends BaseActivity implements OnClickListener{
	 private final static String TAG = "DataRangeActivity";
	 private static final boolean LOGD = false;
	/**
	 * 今日流量排行
	 */
	public static final int TODAY_TAG = 0;
	/**
	 * 本月流量排行
	 */
	public static final int MONTH_TAG = 1;

	 private static final int DIALOG_TAG = 10000;
	 /**
	  * 默认int值
	  */
	 private static final int DEFAULT_INT = -1;

	 private TextView mTvDateType;
	 private ProgressBar mFoldProgressBar;
	 private RelativeLayout mLayType;
	 private TabLayout mTabLayout;
	 private ViewPager mPager;

     private NetworkPolicyManager mPolicyManager;
	 private ArrayList<Fragment> mFragments = new ArrayList<Fragment>();
	/**
	 *
	 */
	private ArrayList<AppItem> mAllList = new ArrayList<>();
	/**
	 * 带有网络权限的非系统应用
	 */
	private ArrayList<ResolveInfo> mAllAppInfos = new ArrayList<ResolveInfo>();;
	/**
	 * 带有网络权限的非系统应用 中间变量
	 */
	private ArrayList<ResolveInfo> mAppInfos = new ArrayList<ResolveInfo>();
	/**
	 * 非定向应用
	 */
	private ArrayList<ResolveInfo> mNoOrientInfos = new ArrayList<ResolveInfo>();

	private RangeFragment mMobileFragment;
	private RangeFragment mWifiFragment;
	private RangeFragment mBgFragment;
	private MainFragementAdater mPageAdapter;
	/**
	 * 显示标题
	 */
	private String mTitle = null;
	private String[] mTabTitles = null;
	private String[] mUidPolicy = null;
	/**
	  * 移动数据网络
	  */
	private ArrayList<AppItem> mAppInfosByPolicy = new ArrayList<AppItem>();
	private ArrayList<AppItem> mAppInfosNoPolicy = new ArrayList<AppItem>();
	 /**
	  * 联网类型
	  */
	 private String mNetType = null;
	/**
	 * 查询周期
	 */
	private String mDateType = null;
	 /**
	  * 流量统计策略
	  */
	 private static int mPolicy = DEFAULT_INT;
	 /**
	  * 统计流量次数
	  */
	 private int mStatsCount = 0;
	/**
	 * 查询日期
	 */
	private int mItemIndex = 0;
	/**
	 * 选中查询网络类型
	 */
	 private int mSelectNetTypePosition;
	 /**
	  * 是否要统计两个卡流量总和
	  */
	 private boolean mIsStatsTotal = false;
	/**
	 * 每分钟后台消耗10M流量
	 */
	 private boolean mBgState;
	 Handler mHandler = new Handler() {
		 public void handleMessage(android.os.Message msg) {
			 switch (msg.what) {
			 case DIALOG_TAG:
				 if (mFoldProgressBar != null) {
					 if (mBgState) {
						 mPager.setCurrentItem(2, true);
						 mBgState = false;
					 }
					 mFoldProgressBar.setVisibility(View.GONE);
				 }
				break;
			 default:
				break;
			 }
		 };
	 };

	 @Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		task.execute();
	}

	 @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_range_data_app);
		if (savedInstanceState != null) {
			mSelectNetTypePosition = savedInstanceState.getInt("SELECT_INDEX");
		}
		if (TextUtils.isEmpty(DataManagerApplication.mImsiArray[0])
				 || TextUtils.isEmpty(DataManagerApplication.mImsiArray[1])) {
			 mIsStatsTotal = false;
		} else {
			 mIsStatsTotal = true;
		}
		mNetType = getString(R.string.data_mobile);
		mPolicy = getIntent().getIntExtra("STATS_POLICY", mPolicy);
//		updateSimChange();
		initTitle();
		registerUpadateApp();
	}

	@Override
	public void setSimStateChangeListener(int simState) {
		boolean wifiUI = false;
		if (simState == SimStateReceiver.SIM_INVALID) {
			mAllList = new ArrayList<AppItem>();
			mAppInfosByPolicy = new ArrayList<AppItem>();
			mAppInfosNoPolicy = new ArrayList<AppItem>();
			if (mSelectNetTypePosition == 1) {
				wifiUI = true;
			}
		} else if (simState == SimStateReceiver.SIM_VALID) {
			if (TextUtils.isEmpty(DataManagerApplication.mImsiArray[0])
					|| TextUtils.isEmpty(DataManagerApplication.mImsiArray[1])){
				mIsStatsTotal = false;
			} else {
				mIsStatsTotal = true;
			}
            if (mFoldProgressBar != null) {
				mFoldProgressBar.setVisibility(View.VISIBLE);
			}
		}
		if (!wifiUI && mSelectNetTypePosition < mFragments.size()) {
			synchronized (DataRangeActivity.class) {
				boolean isMatchTag = true;
				int curent = mPager.getCurrentItem();
				if (curent != mSelectNetTypePosition) {
					isMatchTag = false;
					mPager.setCurrentItem(mSelectNetTypePosition, true);
				}
				LogUtil.v(TAG, "curent>>>>" + curent);
				if (isMatchTag) {
					updateUI(mSelectNetTypePosition, mDateType);
				}
			}
		}
	}

	private void registerUpadateApp() {
		 IntentFilter intentFilter = new IntentFilter(AppReceiver.UPDATE_APP_ACTION);
		 registerReceiver(updateAppReceiver, intentFilter);
	 }

	 private void unRegisterUpdateApp() {
		 if (updateAppReceiver != null) {
			 unregisterReceiver(updateAppReceiver);
		 }
	 }

	private AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
		@Override
		protected Void doInBackground(Void... params) {
			mPolicyManager = NetworkPolicyManager.from(DataRangeActivity.this);
			mAllAppInfos = ToolsUtil.getResolveInfos(DataRangeActivity.this);
			mAppInfos.addAll(mAllAppInfos);
			matchDataList(); //移除定向流量应用.
//			getDataMobileAppsByPolicy(mNetType, initDataNet(mNetType), mNoOrientInfos);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			initView();
			mHandler.sendEmptyMessageDelayed(DIALOG_TAG, 1500);
		}
	};

	 /**
	  * 移除定向流量应用.
	  */
	private void matchDataList() {
		ArrayList<String> orientApps = null;
		if (mIsStatsTotal) {
			if (!TextUtils.isEmpty(DataManagerApplication.mImsiArray[0]) && !TextUtils.isEmpty(DataManagerApplication.mImsiArray[1])) {
				//移除相同的定向流量应用.
				ArrayList<String> orientAppsFirst = getOrientApps(DataManagerApplication.mImsiArray[0]);
				ArrayList<String> orientAppsSecond = getOrientApps(DataManagerApplication.mImsiArray[1]);
				orientAppsFirst.retainAll(orientAppsSecond);
				orientApps = orientAppsFirst;
			}
		} else {
			if (TextUtils.isEmpty(DataManagerApplication.mImsiArray[0]) && TextUtils.isEmpty(DataManagerApplication.mImsiArray[1])) {
				mNoOrientInfos.addAll(mAppInfos);
				return;
			} else {
				//移除定向流量应用.
				String currentImsi = ToolsUtil.getActiveSimImsi(this);
				orientApps = getOrientApps(currentImsi);
			}
		}
		if (null != orientApps) {
			for (int j = 0; j < orientApps.size(); j++) {
				String appUid = orientApps.get(j);
			    for (int i = 0; i < mAppInfos.size(); i++) {
				    ApplicationInfo applicationInfo = mAppInfos.get(i).activityInfo.applicationInfo;
				    int uid = applicationInfo.uid;
					if (uid == Integer.parseInt(appUid)) {
						mAppInfos.remove(i);
						break;
					}
				}
			}
			mNoOrientInfos.addAll(mAppInfos);
		}
	}


	private String[] initDataNet(String netType) {
		mNetType = netType;
		String[] uidsPolicy = null;
		if (mNetType.equals(getString(R.string.data_mobile))) {
			String data = PreferenceUtil.getString(this, "", RangeAppAdapter.TYPE_DATA, null);
			LogUtil.d(TAG, "initSetting:" + data);
			if (!TextUtils.isEmpty(data)) {
				uidsPolicy = data.split(",");
			}
		} else if (mNetType.equals(getString(R.string.data_wifi))) {
			String wlan = PreferenceUtil.getString(this, "", RangeAppAdapter.TYPE_WLAN, null);
			LogUtil.d(TAG, "initSetting:" + wlan);
			if (!TextUtils.isEmpty(wlan)) {
				uidsPolicy = wlan.split(",");
			}
		}
		mUidPolicy = uidsPolicy;
       return mUidPolicy;
	}

	private void initTitle() {
		mFoldProgressBar = (ProgressBar)findViewById(R.id.progressbar);
		mTabTitles = new String[]{getString(R.string.data_mobile), getString(R.string.data_wifi), getString(R.string.net_bg)};
		Toolbar toolbar =  (Toolbar)findViewById(R.id.app_toolbar);
		toolbar.setTitle(getString(R.string.net_control));
		toolbar.setNavigationIcon(com.hb.R.drawable.ic_toolbar_back);
		toolbar.setNavigationOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				DataRangeActivity.this.finish();
			}
		});
		toolbar.setElevation(1);
		mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
		mPager = (ViewPager) this.findViewById(R.id.view_pager_range);
		mLayType = (RelativeLayout) findViewById(R.id.lay_net_type);
        mTvDateType = (TextView)findViewById(R.id.tv_data_type);
		int rangeType = PreferenceUtil.getInt(this, "", PreferenceUtil.SELECTED_DATE_KEY, TODAY_TAG);
		mDateType = rangeType == 0 ? getString(R.string.day_data) : getString(R.string.month_data);
		mBgState = getIntent().getBooleanExtra("BG_STATE", false);
		if (mBgState) {
			//后台数据流量提示:单位时间内（1分钟）后台跑了至少10M流量则满足提示条件
			mNetType = getString(R.string.net_bg);
			mDateType = getString(R.string.day_data);
			PreferenceUtil.putInt(this, "", PreferenceUtil.SELECTED_DATE_KEY, TODAY_TAG);

		}
		mTvDateType.setText(mDateType);
		mTvDateType.setVisibility(View.VISIBLE);
	}

	private void initView() {
		mLayType.setOnClickListener(this);
		mAllList = new ArrayList<AppItem>();
		mAppInfosByPolicy = new ArrayList<AppItem>();
		mAppInfosNoPolicy = new ArrayList<AppItem>();
		mMobileFragment = RangeFragment.newInstance(mDateType, getString(R.string.data_mobile), mIsStatsTotal, mAllList, mAppInfosByPolicy, mAppInfosNoPolicy);
		mWifiFragment = RangeFragment.newInstance(mDateType, getString(R.string.data_wifi), mIsStatsTotal, mAllList, mAppInfosByPolicy, mAppInfosNoPolicy);
		mBgFragment = RangeFragment.newInstance(mDateType, getString(R.string.net_bg), mIsStatsTotal, mAllList, mAppInfosByPolicy, mAppInfosNoPolicy);
		mFragments.add(mMobileFragment);
		mFragments.add(mWifiFragment);
		mFragments.add(mBgFragment);
		mPageAdapter = new MainFragementAdater(this, getFragmentManager(), mFragments, mTabTitles);
		mPager.setAdapter(mPageAdapter);
		mTabLayout.setupWithViewPager(mPager);
		mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int i, float v, int i1) {

			}

			@Override
			public void onPageSelected(int selected) {
				mSelectNetTypePosition = selected;
				mTabLayout.getTabAt(selected).select();
				updateUI(selected, mDateType);
			}

			@Override
			public void onPageScrollStateChanged(int i) {

			}
		});
		mTabLayout.setTabMode(TabLayout.MODE_FIXED);
		if (!mBgState) {
			updateUI(mSelectNetTypePosition, mDateType);
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		updateUI(mSelectNetTypePosition, mDateType);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.v(TAG, "onConfigurationChanged>>>>" + newConfig.locale);
		initTitle();
		if (mPageAdapter != null) {
			mPageAdapter.setmTitles(mTabTitles);
			mPageAdapter.notifyDataSetChanged();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("SELECT_INDEX", mSelectNetTypePosition);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.lay_net_type:
			showDataType(mLayType);
			break;
		default:
			break;
		}

	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mHandler.removeMessages(DIALOG_TAG);
		clearCrash();
		unRegisterUpdateApp();
		Log.v(TAG, "onDestroy>>>>>");
	}

	/**
	 * 更新界面
	 * @param netType  统计类别
	 */
	private synchronized void updateUI(int position, String dateType) {
		mNetType = mTabTitles[position];
		RangeFragment selectFragment = (RangeFragment)mFragments.get(position);
		mAppInfosByPolicy.clear();
		mAppInfosNoPolicy.clear();
		mDateType = dateType;
		selectFragment.setmFoldProgressBar(mFoldProgressBar);
		int currentState = ToolsUtil.getCurrentNetSimSubInfo(this);
		if (currentState != -1 || position == 1) {
			if (position == 1) {
				getDataMobileAppsByPolicy(mNetType, initDataNet(mNetType), mAllAppInfos);
			} else {
				getDataMobileAppsByPolicy(mNetType, initDataNet(mNetType), mNoOrientInfos);
			}
		}
		//切换流量使用网络类型
		RangeAppAdapter rangeAppAdapter = selectFragment.getmRangeAdapter();
		rangeAppAdapter.setNetType(mNetType);
		rangeAppAdapter.setAppList(mAllList);
		rangeAppAdapter.setDataList(mAppInfosNoPolicy, mAppInfosByPolicy);
		selectFragment.changeStats(mStatsCount, mIsStatsTotal, mDateType, mNetType);
	}

	/**
	 * 显示数据类型
	 */
	private void showDataType(View parent) {
		//初始化PopupMenu
		final PopupMenu popupMenu = new PopupMenu(this, parent, Gravity.BOTTOM);
		//设置popupmenu 中menu的点击事件
		popupMenu.setOnMenuItemClickListener(new  PopupMenu.OnMenuItemClickListener() {

		@Override
		public boolean onMenuItemClick(MenuItem menuItem) {
			String item = (String) menuItem.getTitle();
			if (TextUtils.equals(item, mTvDateType.getText().toString())) {
				return false;
			}
			if (TextUtils.equals(item, getString(R.string.day_data))) {
				mItemIndex = TODAY_TAG;
			} else if (TextUtils.equals(item, getString(R.string.month_data))) {
				mItemIndex = MONTH_TAG;
			}
			PreferenceUtil.putInt(DataRangeActivity.this, "", PreferenceUtil.SELECTED_DATE_KEY, mItemIndex);
			mTvDateType.setText(item);
			updateUI(mSelectNetTypePosition, item);
			popupMenu.dismiss();
			return false;
		  }
		});
		//导入menu布局
		popupMenu.inflate(R.menu.popup_menu);
		//显示popup menu
		popupMenu.show();
	}

	/**
	 * 初始化集合
	 * @param netType  统计类别
	 * @param dataList
	 */
	private void getDataMobileAppsByPolicy(String netType, String[] dataList, ArrayList<ResolveInfo> appResolveInfos) {
		mAllList.clear();
		mAppInfosByPolicy.clear();
		mAppInfosNoPolicy.clear();
		ArrayList<Integer> uidList = new ArrayList<Integer>();
		if (dataList != null) {
			for (int i = 0; i < dataList.length; i++) {
				uidList.add(Integer.parseInt(dataList[i]));
			}
		}
		mAppInfos = appResolveInfos;
		for (int i = 0; i < mAppInfos.size(); i++) {
			ResolveInfo resolveInfo = mAppInfos.get(i);
			int uid = resolveInfo.activityInfo.applicationInfo.uid;
			AppItem appItem = new AppItem();
			appItem.setAppUid(uid);
			appItem.setResolveInfo(resolveInfo);
			if (netType.equals(getString(R.string.net_bg))) {
				if (0 != (resolveInfo.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)) {
					//过虑系统自身应用
					continue;
				}
				int selectedPolicy = mPolicyManager.getUidPolicy(uid);
				if (selectedPolicy == POLICY_REJECT_METERED_BACKGROUND) {
					mAppInfosByPolicy.add(appItem);
					appItem.setPolicyStatus(false);
				} else {
					//允许联网策略
					mAppInfosNoPolicy.add(appItem);
					appItem.setPolicyStatus(true);
				}
			} else {
				if (uidList == null || uidList.size() == 0) {
					mAppInfosNoPolicy.add(appItem);
					appItem.setPolicyStatus(true);
				} else {
					if (!uidList.contains(uid)) {
                        mAppInfosNoPolicy.add(appItem);
                        appItem.setPolicyStatus(true);
                    } else {
						mAppInfosByPolicy.add(appItem);
						appItem.setPolicyStatus(false);
					}
				}
			}
		}

		if (mAppInfosNoPolicy.size() > 0) {
			mAllList.addAll(mAppInfosNoPolicy);
		}
		if (mAppInfosByPolicy.size() > 0) {
			mAllList.addAll(mAppInfosByPolicy);
		}

	}

	/**
	 * 获得定向应用
	 * @param imsi
	 * @return
	 */
	private ArrayList<String> getOrientApps(String imsi) {
		  ArrayList<String> addUidList = new ArrayList<String>();
		  String addUids = PreferenceUtil.getString(this, imsi, PreferenceUtil.ORIENT_APP_ADDED_KEY, "");
		  if (addUids.contains(",")) {
			  String[] addUidsArray = addUids.split(",");
			  if (null != addUidsArray &&  addUidsArray.length > 0) {
				  Collections.addAll(addUidList, addUidsArray);
			  }
		  }
		  return addUidList;
	}

     /**
      * 清除缓存设置
      */
     private void clearCrash() {
    	mAppInfos.clear();
     	mAppInfosByPolicy.clear();
     	mAppInfosNoPolicy.clear();
		mNoOrientInfos.clear();
		mAllAppInfos.clear();
     }
	 /**
	  * 添加或删除应用广播
	  */
	 private BroadcastReceiver updateAppReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String updateAppName = intent.getStringExtra("UPDATE_APP_NAME");
			int updateTag = intent.getIntExtra("UPDATE_APP_TAG", 0);
			PackageManager pm = context.getPackageManager();
			ResolveInfo resolveInfo = null;
			try {
				if (AppReceiver.PACKAGEADDED == updateTag) {
					Intent launchIntent = new Intent(Intent.ACTION_MAIN, null)
							.addCategory(Intent.CATEGORY_LAUNCHER)
							.setPackage(updateAppName);
					List<ResolveInfo> resolveInfos = pm.queryIntentActivities(launchIntent, PackageManager.GET_DISABLED_COMPONENTS);
					resolveInfo = resolveInfos.get(0);
					mAllAppInfos.add(resolveInfo);
					mNoOrientInfos.add(resolveInfo);
					//mAppInfos.add(resolveInfo);
				} else if (AppReceiver.PACKAGEREMOVED == updateTag) {
					for (int i = 0; i < mAllAppInfos.size(); i++) {
						resolveInfo = mAllAppInfos.get(i);
						if (TextUtils.equals(updateAppName, resolveInfo.activityInfo.packageName)) {
							mAllAppInfos.remove(resolveInfo);
							mNoOrientInfos.remove(resolveInfo);
							break;
						}
					}
				}
				LogUtil.v(TAG, "updateAppName>>>" + updateAppName + ">mAppInfos>>>>>" + mAllAppInfos.size());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	 };

}
