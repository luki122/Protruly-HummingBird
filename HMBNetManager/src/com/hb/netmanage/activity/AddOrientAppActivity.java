package com.hb.netmanage.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.hb.netmanage.R;
import android.widget.ProgressBar;
import com.hb.netmanage.adapter.AddOrientAppAdapter;
import com.hb.netmanage.entity.AppItem;
import com.hb.netmanage.receiver.AppReceiver;
import com.hb.netmanage.utils.LogUtil;
import com.hb.netmanage.utils.PreferenceUtil;
import com.hb.netmanage.utils.ToolsUtil;
import com.hb.netmanage.view.DividerItemDecoration;

import java.util.ArrayList;
import java.util.List;

import hb.widget.recycleview.LinearLayoutManager;
import hb.widget.recycleview.RecyclerView;
import hb.widget.toolbar.Toolbar;

/**
 * 添加定向流量应用界面
 * 
 * @author zhaolaichao
 */
public class AddOrientAppActivity extends BaseActivity implements View.OnClickListener{

	private AddOrientAppAdapter mAddAppAdapter;
	/**
	 * 定向应用列表
	 */
    private RecyclerView mRvAddOrientApp;
    private ProgressBar mFoldProgressBar;
    private TextView mTvCancel;
	private TextView mTvOk;
	private TextView mTvSelectAll;
	private LinearLayout mLayAction;
	private LinearLayout mLaySelectAll;

    private ArrayList<ResolveInfo> mAppInfos;
    private ArrayList<AppItem> mUnAppInfos = new ArrayList<AppItem>();
    private String mCurrectImsi;
    private int mSelectedIndex;
    private AsyncTask<Void, Void, Void> mTask;

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case AddOrientAppAdapter.SAVE_TAG:
					int selectCount = mAddAppAdapter.getAddAppList().size();
					if (selectCount > 0) {
						mLayAction.setVisibility(View.VISIBLE);
						if (selectCount == mUnAppInfos.size()) {
							mTvSelectAll.setText(getString(R.string.all_unselect));
						}
					} else {
						mLayAction.setVisibility(View.GONE);
						mTvSelectAll.setText(getString(R.string.all_select));
					}
					break;
			}
		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHbContentView(R.layout.activity_add_orient_app);
		initSimInfo();
		initView();
		registerUpadateApp();
		mTask = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				mAppInfos = ToolsUtil.getResolveInfos(AddOrientAppActivity.this);
				//初始化数据
				getUnAddApps();
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				mFoldProgressBar.setVisibility(View.GONE);
				mAddAppAdapter.setAppList(mUnAppInfos);
				mRvAddOrientApp.setAdapter(mAddAppAdapter);
			}
			
		}.execute();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (null != mTask) {
			mTask.cancel(true);
		}
		mAddAppAdapter = null;
		unRegisterUpdateApp();
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
		Toolbar toolbar = (Toolbar) findViewById(R.id.add_toolbar);
		toolbar.setTitle(getString(R.string.add_data_orient_app));
		toolbar.setElevation(1);
		toolbar.setNavigationIcon(com.hb.R.drawable.ic_toolbar_back);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AddOrientAppActivity.this.finish();
			}
		});
		mTvSelectAll = (TextView)findViewById(R.id.tv_all_select);
		mTvCancel = (TextView) findViewById(R.id.tv_cancel);
		mTvOk = (TextView) findViewById(R.id.tv_ok);
		mLayAction = (LinearLayout)findViewById(R.id.lay_action);
		mLayAction.setVisibility(View.GONE);
		mFoldProgressBar = (ProgressBar) findViewById(R.id.progressbar);
		mFoldProgressBar.setVisibility(View.VISIBLE);
		mRvAddOrientApp = (RecyclerView) findViewById(R.id.recycler_add_orient_app);
		mLaySelectAll = (LinearLayout) findViewById(R.id.lay_all_select);
		int[] bounds = new int[]{(int)getResources().getDimension(R.dimen.item_divider_left), 0, 0, 0};
		DividerItemDecoration decoration = new DividerItemDecoration(this,LinearLayoutManager.VERTICAL, bounds);

		mRvAddOrientApp.addItemDecoration(decoration);
		mAddAppAdapter = new AddOrientAppAdapter(this, mCurrectImsi, mHandler);
		mRvAddOrientApp.setLayoutManager(new LinearLayoutManager(this));
		mTvCancel.setOnClickListener(this);
		mTvOk.setOnClickListener(this);
		mLaySelectAll.setOnClickListener(this);
	}

	private void getUnAddApps() {
		//取出已添加过的UID
		String addedAppUids = PreferenceUtil.getString(this, mCurrectImsi, PreferenceUtil.ORIENT_APP_ADDED_KEY, "");
	    LogUtil.e("addedAppUids", "addedAppUids>>>" + addedAppUids);
		if (!TextUtils.isEmpty(addedAppUids) && addedAppUids.length() > 0) {
	    	if (addedAppUids.contains(",")) {
	    		String[] uidsArray = addedAppUids.split(",");
	    		for (int i = 0; i < mAppInfos.size(); i++) {
	    			ResolveInfo resolveInfo = mAppInfos.get(i);
	    			int uid = resolveInfo.activityInfo.applicationInfo.uid;
	    			boolean isexit = false;
	    			for (int j = 0; j < uidsArray.length; j++) {
	    				if (uid == Integer.parseInt(uidsArray[j])){
	    					isexit = true;
	    					break;
	    				} 
	    			}
	    			//添加没有添加过的应用
	    			if (!isexit) {
						AppItem appItem = new AppItem();
						appItem.setResolveInfo(resolveInfo);
						appItem.setAppUid(resolveInfo.activityInfo.applicationInfo.uid);
						appItem.setTag(getString(R.string.all_unselect));
	    				mUnAppInfos.add(appItem);
	    			}
	    		}
	    	}
	    } else {
			for (int i = 0; i < mAppInfos.size(); i++) {
				AppItem appItem = new AppItem();
				appItem.setTag(getString(R.string.all_unselect));
				appItem.setAppUid(mAppInfos.get(i).activityInfo.applicationInfo.uid);
				appItem.setResolveInfo(mAppInfos.get(i));
				mUnAppInfos.add(appItem);
			}
	    }
	}
	
	@Override
	public void onNavigationClicked(View view) {
		super.onNavigationClicked(view);
		this.finish();
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	private void saveAddApps() {
		Intent intent = new Intent();
		intent.putParcelableArrayListExtra("ADD_ORIENT_APPS", mAddAppAdapter.getAddAppList());
		setResult(1000, intent);
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
			AppItem appItem = null;
			try {
				if (AppReceiver.PACKAGEADDED == updateTag) {
					appItem = new AppItem();
					Intent launchIntent = new Intent(Intent.ACTION_MAIN, null)
							.addCategory(Intent.CATEGORY_LAUNCHER)
							.setPackage(updateAppName);
					List<ResolveInfo> resolveInfos = pm.queryIntentActivities(launchIntent, PackageManager.GET_DISABLED_COMPONENTS);
					resolveInfo = resolveInfos.get(0);
					appItem.setResolveInfo(resolveInfo);
					appItem.setAppUid(resolveInfo.activityInfo.applicationInfo.uid);
					appItem.setTag(getString(R.string.all_unselect));
					mUnAppInfos.add(appItem);
					mAddAppAdapter.setAppList(mUnAppInfos);
					mAddAppAdapter.notifyItemInserted(mUnAppInfos.size() == 0 ? 0 : mUnAppInfos.size() -1);
				} else if (AppReceiver.PACKAGEREMOVED == updateTag) {
					for (int i = 0; i < mUnAppInfos.size(); i++) {
						appItem = mUnAppInfos.get(i);
						resolveInfo = appItem.getResolveInfo();
						if (TextUtils.equals(updateAppName, resolveInfo.activityInfo.packageName)) {
							mUnAppInfos.remove(i);
							mAddAppAdapter.setAppList(mUnAppInfos);
							mAddAppAdapter.notifyItemRemoved(i);
							break;
						}
					}
				}
				LogUtil.v("AddOrientAppActivity", "updateAppName>>>" + updateAppName + ">mAppInfos>>>>>" + mAppInfos.size());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		 
	 };
	@Override
	public void setSimStateChangeListener(int simState) {
		
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.tv_cancel:
				mAddAppAdapter.getAddAppList().clear();
				for (AppItem appItem : mAddAppAdapter.getmAppList()) {
					appItem.setTag(getString(R.string.all_unselect));
				}
				mAddAppAdapter.setAppList(mUnAppInfos);
				mAddAppAdapter.notifyDataSetChanged();
				mTvSelectAll.setText(getString(R.string.all_select));
				mLayAction.setVisibility(View.GONE);
				break;
			case R.id.tv_ok:
				mAddAppAdapter.saveOrientAdd(mAddAppAdapter.getAddAppList());
				mLayAction.setVisibility(View.GONE);
				saveAddApps();
				Toast.makeText(this, R.string.add_orient_app_ok, Toast.LENGTH_SHORT).show();
				finish();
				break;
			case R.id.lay_all_select:
				//全选
				selectAll();
				break;
		}
	}

	private void selectAll() {
		if (mUnAppInfos.size() == 0) {
			return;
		}
		mAddAppAdapter.getmAppList().clear();
		mAddAppAdapter.setAppList(mUnAppInfos);
		if (TextUtils.equals(getString(R.string.all_select), mTvSelectAll.getText().toString())) {
			//全选
			for (AppItem appItem : mAddAppAdapter.getmAppList()) {
				appItem.setTag(getString(R.string.all_select));
			}
			mAddAppAdapter.getAddAppList().clear();
			mAddAppAdapter.getAddAppList().addAll(mUnAppInfos);
			mTvSelectAll.setText(getString(R.string.all_unselect));
		} else {
			//全不选
			for (AppItem appItem : mAddAppAdapter.getmAppList()) {
				appItem.setTag(getString(R.string.all_unselect));
			}
			mAddAppAdapter.getAddAppList().clear();
			mTvSelectAll.setText(getString(R.string.all_select));
		}
		mAddAppAdapter.notifyDataSetChanged();
		mHandler.sendEmptyMessage(AddOrientAppAdapter.SAVE_TAG);
	}
}
