package com.hb.netmanage.activity;

import static android.net.NetworkTemplate.buildTemplateMobileAll;
import java.util.ArrayList;
import java.util.Collections;

import com.hb.netmanage.R;
import com.hb.netmanage.adapter.OrientAppAdapter;
import com.hb.netmanage.entity.AppItem;
import com.hb.netmanage.receiver.AppReceiver;
import com.hb.netmanage.utils.PreferenceUtil;
import com.hb.netmanage.utils.StringUtil;
import com.hb.netmanage.utils.ToolsUtil;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.widget.ProgressBar;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.hb.netmanage.utils.LogUtil;
import com.hb.netmanage.view.DividerItemDecoration;

import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import hb.widget.recycleview.LinearLayoutManager;
import hb.widget.recycleview.RecyclerView;
import hb.widget.toolbar.Toolbar;

/**
 * 定向流量应用设置界面
 * 
 * @author zhaolaichao
 */
public class OrientAppActivity extends BaseActivity {

	private static final String TAG = "OrientAppActivity";
	private static final int STATS_APP = 10000;
	private TextView mTvNoApp;
	private ProgressBar mFoldProgressBar;
	/**
	 * 定向应用列表
	 */
	private RecyclerView mRvOrientApp;
    

	private OrientAppAdapter mOrientAppAdapter;
    /**
     * 已添加的定向应用集合
     */
    private ArrayList<AppItem> mAddAppInfos = new ArrayList<AppItem>();
    private ArrayList<ResolveInfo> mAppAllInfos = new ArrayList<ResolveInfo>();
    
    private String mCurrectImsi; 

    private int mSelectedIndex;
    private String[] mAddUidsArray;

    Handler mHandler = new Handler(){
    	public void handleMessage(android.os.Message msg) {
    		switch (msg.what) {
			case OrientAppAdapter.UPDATE_UI_TAG:
				mTvNoApp.setVisibility(View.VISIBLE);
				break;
			case STATS_APP:
				mOrientAppAdapter.setAddAppList(mAddAppInfos);
				mOrientAppAdapter.notifyDataSetChanged();
				break;
			default:
				break;
			}
    	};
    };
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
    	super.onPostCreate(savedInstanceState);
    }
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHbContentView(R.layout.activity_orient_app);

		//初始化数据
		initSimInfo();
		initView();
		registerUpadateApp();
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				mAppAllInfos.addAll(ToolsUtil.getResolveInfos(OrientAppActivity.this));
				getAddApps();
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				mFoldProgressBar.setVisibility(View.GONE);
				if (mAddAppInfos.size() == 0) {
					mHandler.sendEmptyMessage(OrientAppAdapter.UPDATE_UI_TAG);
				} 
				mHandler.sendEmptyMessage(STATS_APP);
			}
			
		}.execute();
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
	 
	private void initView() {
		Toolbar toolbar = getToolbar();
		setTitle(getString(R.string.data_orient_app));
		toolbar.inflateMenu(R.menu.toolbar_action_add_button);
		toolbar.setElevation(1);
		showBackIcon(true);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				OrientAppActivity.this.finish();
			}
		});
		mFoldProgressBar = (ProgressBar) findViewById(R.id.progressbar);
		mFoldProgressBar.setVisibility(View.VISIBLE);
		mRvOrientApp = (RecyclerView) findViewById(R.id.recycler_orient_app);
		mTvNoApp = (TextView) findViewById(R.id.tv_no_orient_app);
		int[] bounds = new int[]{(int)getResources().getDimension(R.dimen.item_divider_left), 0, 0, 0};
		DividerItemDecoration decoration = new DividerItemDecoration(this, LinearLayoutManager.VERTICAL, bounds);
		mRvOrientApp.addItemDecoration(decoration);
		mRvOrientApp.setLayoutManager(new LinearLayoutManager(this));
		mOrientAppAdapter = new OrientAppAdapter(this, mHandler, mCurrectImsi);
		mRvOrientApp.setAdapter(mOrientAppAdapter);
	}

	/**
	 * 初始化数据
	 */
	private void initSimInfo() {
		mSelectedIndex = getIntent().getIntExtra("CURRENT_INDEX", 0);
    	if (mSelectedIndex == 0) {
	    	//卡1
    		mCurrectImsi = PreferenceUtil.getString(this, PreferenceUtil.SIM_1, PreferenceUtil.IMSI_KEY, "");
	    } else if (mSelectedIndex == 1) {
	    	//卡2
	    	mCurrectImsi = PreferenceUtil.getString(this, PreferenceUtil.SIM_2, PreferenceUtil.IMSI_KEY, "");
	    }
	}
	
	@Override
	public void onNavigationClicked(View view) {
		super.onNavigationClicked(view);
		this.finish();
	}
	
	@Override
	public boolean onMenuItemClick(MenuItem item) {
		Intent addIntent = new Intent(OrientAppActivity.this, AddOrientAppActivity.class);
		addIntent.putExtra("CURRENT_INDEX", mSelectedIndex);
		startActivityForResult(addIntent, 0);
		return super.onMenuItemClick(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == 1000) {
			ArrayList<AppItem> appItems = data.getParcelableArrayListExtra("ADD_ORIENT_APPS");
			if (appItems.size() > 0) {
				mTvNoApp.setVisibility(View.GONE);
				mAddAppInfos.addAll(appItems);
				mHandler.sendEmptyMessage(STATS_APP);
			}
			LogUtil.v(TAG, "appItems>>" + appItems.size());
		}
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (null != mAddAppInfos) {
			mAddAppInfos.clear();
		}
		unRegisterUpdateApp();
	}
	
	/**
	 * 获得已添加的应用
	 */
	private void getAddApps() {
		String addUids = PreferenceUtil.getString(this, mCurrectImsi, PreferenceUtil.ORIENT_APP_ADDED_KEY, "");
		LogUtil.v(TAG, "getAddApps>>" + addUids);
		if (!TextUtils.isEmpty(addUids)) {
			if (addUids.contains(",")) {
				mAddUidsArray = addUids.split(",");
				for (String addUid : mAddUidsArray) {
					AppItem appItem = new AppItem();
					for (int i = 0; i < mAppAllInfos.size(); i++) {
						ResolveInfo resolveInfo = mAppAllInfos.get(i);
						int uid = resolveInfo.activityInfo.applicationInfo.uid;
						if (uid == Integer.parseInt(addUid)) {
							appItem.setAppUid(uid);
							appItem.setResolveInfo(resolveInfo);
							mAddAppInfos.add(appItem);
							break;
						}
					}
				}
			}
		}
	}

    /**
	  * 添加或删除应用广播
	  */
	 private BroadcastReceiver updateAppReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String updateAppName = intent.getStringExtra("UPDATE_APP_NAME");
			int updateTag = intent.getIntExtra("UPDATE_APP_TAG", 0);
			try {
			    if (AppReceiver.PACKAGEREMOVED == updateTag) {
			    	for (int i = 0; i < mAddAppInfos.size(); i++) {
			    		AppItem appItem = mAddAppInfos.get(i);
						ResolveInfo resolveInfo = appItem.getResolveInfo();
			    		if (TextUtils.equals(updateAppName, resolveInfo.activityInfo.packageName)) {
			    			mOrientAppAdapter.removeApp(i);
			    			break;
			    		}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		 
	 };
	 
	@Override
	public void setSimStateChangeListener(int simState) {
		// TODO Auto-generated method stub
		
	}
	
}
