package com.hb.netmanage.adapter;

import static android.net.NetworkPolicyManager.POLICY_NONE;
//import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED;
import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.net.NetworkPolicyManager;

import com.hb.netmanage.utils.ToolsUtil;
import com.hb.themeicon.theme.IconManager;

import android.os.Message;
import android.os.UserHandle;
import android.os.AsyncTask;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import com.hb.netmanage.DataManagerApplication;
import com.hb.netmanage.utils.LogUtil;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.hb.netmanage.R;
import com.hb.netmanage.entity.AppItem;
import com.hb.netmanage.net.NetController;
import com.hb.netmanage.utils.PreferenceUtil;
import com.hb.netmanage.utils.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import hb.widget.Switch;
import hb.widget.recycleview.RecyclerView;

/**
 * 添加定向应用
 * 
 * @author zhaolaichao
 *
 */
public class RangeAppAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
	private final static String TAG = "RangeAppAdapter";
	public static final int REJECT_ALL = 1000;
	public static final int ALLOW_ALL = 1001;
	public static final int EMPTY_APP = 1002;
	public static final int RAGNE_APP = 1003;
	public static final int UPDATE_ITEM_APP = 1004;
	/**
	 * 延迟更新
	 */
	public static final int UPDATE_DELAY = 2000;
	public static final String TYPE_DATA = "type_data";
	public static final String TYPE_WLAN = "type_wlan";
	/**
	 * 超过100kb才会显示
	 */
	private final long MAIN_DATA = 100 * 1024;
	/**
	 * 每页显示个数
	 */
	public static final int PAGE_COUNT = 12;
	private NetworkPolicyManager mPolicyManager;
	/**
	 * 要添加的应用集合
	 */
    private ArrayList<AppItem> mAppList = new ArrayList<AppItem>();
	/**
	 * 每页加载集合
	 */
	private ArrayList<AppItem> mPageAppList = new ArrayList<AppItem>();
	private ArrayList<AppItem> mRemoveAppList = new ArrayList<AppItem>();
	private ArrayList<AppItem> mPolicyAppList = new ArrayList<AppItem>();
	private ArrayList<AppItem> mNoPolicyAppList = new ArrayList<AppItem>();
	private ArrayList<AppItem> mUpdateDataList;
    private Context mContext;
    private PackageManager mPManager;
	private IconManager mIconManager;
	private Handler mHandler;
    private String mNetType;
	private String mSimCard;
	private long mTempTime;
	/**
	 * 卡状态标志
	 */
    private int mSimState;

	private HashMap<Integer, Drawable> mAppIconMap = new HashMap<Integer, Drawable>();
	Handler uiHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case UPDATE_DELAY:
					LogUtil.e(TAG, "一开始执行>>>");
					int index = msg.arg1;
					Switch tBtn = (Switch) msg.obj;
					setOnSelectedListener(index, tBtn);
					break;
			}
		}
	};

	public RangeAppAdapter(Context context, Handler handler) {
		super();
		if (context == null) return;
		this.mContext = context;
		mHandler = handler;
		mPolicyManager = NetworkPolicyManager.from(context);
		mPManager = mContext.getPackageManager();
		mIconManager = IconManager.getInstance(mContext, true, false);
		mSimCard = mContext.getString(R.string.card);
	}

	public void setNetType(String metType) {
		this.mNetType = metType;
	}

	public void setAppList(ArrayList<AppItem> appList) {
		//设置sim卡状态
		setSimState();
		if (mHandler != null) {
			mHandler.removeMessages(UPDATE_DELAY);
		}
		mPageAppList.clear();
		mAppList.clear();
		mAppList.addAll(appList == null ? new ArrayList<AppItem>() : appList);
		LogUtil.v(TAG, "setAppList>>移动数据>>>" + mAppList.size());
	}

	public void setDataList(ArrayList<AppItem> appNoPolicyList, ArrayList<AppItem> appPolicyList) {
		mNoPolicyAppList.clear();
		mNoPolicyAppList.addAll(appNoPolicyList == null ? new ArrayList<AppItem>() : appNoPolicyList);
		mPolicyAppList.clear();
		mPolicyAppList.addAll(appPolicyList == null ? new ArrayList<AppItem>() : appPolicyList);
	}

	public ArrayList<AppItem> getmAppList() {
		return mAppList;
	}

	public ArrayList<AppItem> getmPolicyAppList() {
		return mPolicyAppList;
	}

	public ArrayList<AppItem> getmNoPolicyAppList() {
		return mNoPolicyAppList;
	}

	public ArrayList<AppItem> getPageAppList() {
		return mPageAppList;
	}

	public void setPageAppList(ArrayList<AppItem> pageAppList) {
		this.mPageAppList.addAll(pageAppList);
	}

	/**
	 * 设置流量应用的集合
	 * @param appInfosData
	 */
	public void setAppInfosUpdateData(ArrayList<AppItem> appInfosData) {
		mUpdateDataList = appInfosData;
		AppItemTask appItemTask = new AppItemTask();
		appItemTask.execute();
	}

	private void setSimState() {
		if (mContext == null) return;
		if (TextUtils.equals(mNetType, mContext.getString(R.string.data_wifi))) {
			//wifi数据
			mSimState = 3;
		} else {
			if (TextUtils.isEmpty(DataManagerApplication.mImsiArray[0])) {
				//单卡显示在卡1位置
				mSimState = 1;
			} else if (TextUtils.isEmpty(DataManagerApplication.mImsiArray[1])) {
				mSimState = 0;
			} else {
				mSimState = 2;
			}
		}
	}

	@Override
	public int getItemCount() {
		return mPageAppList.size();
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
		AppStateHolder appHolder = (AppStateHolder) viewHolder;
		appHolder.setData(position);
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(mContext).inflate(R.layout.range_app_item, parent, false);
		AppStateHolder appHolder = new AppStateHolder(view);
		return appHolder;
	}


	@Override
	public int getItemViewType(int position) {
		return position;
	}



	class AppStateHolder extends RecyclerView.ViewHolder {

		ImageView imvLogo;
		TextView tvAppName;
		TextView tvSim1UseData;
		TextView tvSim2UseData;
		Switch tBtnRange;
		public AppStateHolder(View itemView) {
			super(itemView);
			imvLogo = (ImageView) itemView.findViewById(R.id.imv_logo);
			tvAppName = (TextView) itemView.findViewById(R.id.tv_app_name);
			tvSim1UseData = (TextView) itemView.findViewById(R.id.tv_app_sim1);
			tvSim2UseData = (TextView) itemView.findViewById(R.id.tv_app_sim2);
			tBtnRange = (Switch) itemView.findViewById(R.id.togglebtn);
			imvLogo.setVisibility(View.VISIBLE);
			tvSim1UseData.setVisibility(View.VISIBLE);
			if (TextUtils.equals(mNetType, mContext.getString(R.string.data_wifi))) {
				tvSim2UseData.setVisibility(View.GONE);
			}
			tBtnRange.setVisibility(View.VISIBLE);
		}

		public void setData(int position) {
			tBtnRange.setOnCheckedChangeListener(null);
			//获取要添加的应用
			AppItem appItem = mPageAppList.get(position);
			ResolveInfo resolveInfo = appItem.getResolveInfo();
			ApplicationInfo applicationInfo = resolveInfo.activityInfo.applicationInfo;
			String appName  = applicationInfo.loadLabel(mPManager).toString();
			if (mSimState == 3) {
				//tvSim2UseData.setVisibility(View.GONE);
				//wifi数据
				tvSim1UseData.setText(StringUtil.formatFloatDataFlowSize(mContext, appItem.getAppDataBySim1()));
			} else {
				if (mSimState == 1) {
					//单卡显示在卡1位置
					tvSim2UseData.setVisibility(View.GONE);
					tvSim1UseData.setText(StringUtil.formatFloatDataFlowSize(mContext, appItem.getAppDataBySim2()));

				} else if (mSimState == 0) {
					tvSim2UseData.setVisibility(View.GONE);
					tvSim1UseData.setText(StringUtil.formatFloatDataFlowSize(mContext, appItem.getAppDataBySim1()));
				} else {
					tvSim1UseData.setText(mSimCard + "1  " + StringUtil.formatFloatDataFlowSize(mContext, appItem.getAppDataBySim1()));
					tvSim2UseData.setText(mSimCard + "2  " + StringUtil.formatFloatDataFlowSize(mContext, appItem.getAppDataBySim2()));
					tvSim1UseData.setVisibility(View.VISIBLE);
					tvSim2UseData.setVisibility(View.VISIBLE);
				}
			}
			//获得应用的logo
			Drawable logo = mAppIconMap.get(appItem.getAppUid());
			if (logo != null) {
				imvLogo.setImageDrawable(logo);
			}
			tvAppName.setText(appName);
			tBtnRange.setTag(position);
			//检查uid是否设置了，并设置checkbox。
			tBtnRange.setChecked(appItem.isPolicyStatus());
			tBtnRange.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					try {
						int index = (Integer) buttonView.getTag();
						Switch tBtn = (Switch) buttonView;
						Message msg = uiHandler.obtainMessage();
						msg.what = UPDATE_DELAY;
						msg.obj = tBtn;
						msg.arg1 = index;
						uiHandler.sendMessageDelayed(msg, 300);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			tBtnRange.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					int action = event.getAction();
					if (MotionEvent.ACTION_DOWN == action) {
						//过滤重复点击
						LogUtil.e(TAG, "点击>>>>");
						if (System.currentTimeMillis() - mTempTime < 800 ) {
							mTempTime = System.currentTimeMillis();
							return true;
						}

					}
					mTempTime = System.currentTimeMillis();
					return false;
				}
			});
		}
	}

	/**
	 * 禁止使用网络
	 * @param uid
	 * @param policy   网络类型
	 */
	private void applyDataChange(int uid, int policy) {
		try {
			mPolicyManager.setUidPolicy(uid, policy);
			LogUtil.v(TAG, "uid->>>>>>" + uid);
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.e("ttt", "No bandwidth control; leaving>>>" + e.getMessage());
		}
	}

	/**
	 * 设置上网类型
	 * @param type
	 * @param uid
	 * @param isReject
	 */
	private void applyChange(String type, int uid, boolean isReject) {
		int netType = -1;
		switch (type) {
			case TYPE_DATA:
				netType = NetController.MOBILE;
				break;
			case TYPE_WLAN:
				netType = NetController.WIFI;
				break;
		}
		//传入false代表要允许其联网。
		//传入true代表要禁止其联网。
		NetController.getInstance().setFirewallUidChainRule(uid, netType, isReject);
	}


	/**
	 * 禁止上网
	 * @param type
	 * @param saveList
     */
	private void save(String type, ArrayList<AppItem> saveList) {
		if (saveList.size() == 0) {
			PreferenceUtil.putString(mContext, "", type, null);
			return;
		}
		StringBuilder sb = new StringBuilder();
		for (AppItem appItem : saveList) {
			sb.append(appItem.getAppUid()).append(",");
		}
		LogUtil.e(TAG, "sb:" + sb);
		switch (type) {
			case TYPE_DATA:
				PreferenceUtil.putString(mContext, "", TYPE_DATA, sb.substring(0, sb.length() - 1));
				break;
			case TYPE_WLAN:
				PreferenceUtil.putString(mContext, "", TYPE_WLAN, sb.substring(0, sb.length() - 1));
				break;
		}
	}
	/**
	 * 点击switch事件
	 * @param switchBtn
	 */
	public void setOnSelectedListener(int index, Switch tBtn) {
		synchronized (mAppList) {
			ResolveInfo resolveInfoItem = mAppList.get(index).getResolveInfo();
			int uid = resolveInfoItem.activityInfo.applicationInfo.uid;
			String typeNet = null;
			if (tBtn.isChecked()) {
				if (mAppList.size() <= mNoPolicyAppList.size()) {
					return;
				}
				if (mNetType.equals(mContext.getString(R.string.data_mobile))) {
					//允许使用移动数据
					typeNet = TYPE_DATA;
					applyChange(TYPE_DATA, uid, false);
				} else if (mNetType.equals(mContext.getString(R.string.net_bg))) {
					//允许使用后台数据
					typeNet = null;
					applyDataChange(uid, POLICY_NONE);
				} else if (mNetType.equals(mContext.getString(R.string.data_wifi))) {
					typeNet = TYPE_WLAN;
					applyChange(TYPE_WLAN, uid, false);
				}
				AppItem appItem = mAppList.get(index);
				appItem.setPolicyStatus(true);
				//Collections.swap(mAppList, index, mNoPolicyAppList.size());
				mPolicyAppList.remove(appItem);
				mAppList.remove(appItem);
				mAppList.add(0, appItem);
				mNoPolicyAppList.add(appItem);
				if (typeNet != null) {
					save(typeNet, mPolicyAppList);
				}
				//更新界面
				updateItem(tBtn.isChecked());
			} else {
				if (mAppList.size() <= mPolicyAppList.size()) {
					return;
				}
				if (mNetType.equals(mContext.getString(R.string.data_mobile))) {
					//禁止使用移动数据
					typeNet = TYPE_DATA;
					applyChange(TYPE_DATA, uid, true);
				} else if (mNetType.equals(mContext.getString(R.string.net_bg))) {
					//禁止使用后台数据
					//保存初始状态
					typeNet = null;
					applyDataChange(uid, POLICY_REJECT_METERED_BACKGROUND);
				} else if (mNetType.equals(mContext.getString(R.string.data_wifi))) {
					typeNet = TYPE_WLAN;
					applyChange(TYPE_WLAN, uid, true);
				}
				AppItem appItem = mAppList.get(index);
				appItem.setPolicyStatus(false);
//				Collections.swap(mAppList, index, mNoPolicyAppList.size() - 1);
				mNoPolicyAppList.remove(appItem);
				mAppList.remove(appItem);
				mAppList.add(appItem);
				mPolicyAppList.add(appItem);
				if (typeNet != null) {
					save(typeNet, mPolicyAppList);
				}
				//更新界面
				updateItem(tBtn.isChecked());
			}
		}
	}

	/**
	 * 更新界面
	 */
	private synchronized void updateItem(boolean isChecked) {
		List<AppItem> noPolicyList = null;
		List<AppItem> policyList = null;
		Collections.sort(mPolicyAppList);
	    Collections.sort(mNoPolicyAppList);
		if (isChecked) {
			noPolicyList = mAppList.subList(0, mNoPolicyAppList.size());
			Collections.sort(noPolicyList);
			if (noPolicyList.size() == mAppList.size()) {
				mHandler.sendEmptyMessage(REJECT_ALL);
			}
		} else {
			policyList = mAppList.subList(mNoPolicyAppList.size(), mAppList.size());
			Collections.sort(policyList);
			if (policyList.size() == mAppList.size()) {
				mHandler.sendEmptyMessage(ALLOW_ALL);
			}
		}
        List<AppItem> updateList = mAppList.subList(0, mPageAppList.size());
		ArrayList<AppItem> tempList = new ArrayList<>();
		tempList.addAll(updateList);
		mPageAppList = tempList;
		notifyDataSetChanged();
    }

	private class AppItemTask extends AsyncTask<Void, Void, Void> {

		public AppItemTask() {
			super();
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {

				if (mAppList.size() == 0) {
					return null;
				}
				mAppIconMap.clear();
				for (int i = 0; i < mAppList.size(); i++) {
					AppItem appItem = mAppList.get(i);
					//获得应用的logo
					Drawable logo = mIconManager.getIconDrawable(appItem.getResolveInfo(), UserHandle.CURRENT);
					mAppIconMap.put(appItem.getAppUid(), logo);
					for (int j = 0; j < mUpdateDataList.size(); j++) {
						AppItem appItemData = mUpdateDataList.get(j);
						//appItemData包含前台数据和后台数据
						if (appItem.getAppUid() == appItemData.getAppUid()) {
							//卡1|卡2流量数据
							long dataBySim1 = appItem.getAppDataBySim1();
							appItem.setAppDataBySim1(dataBySim1 + appItemData.getAppDataBySim1());
							long dataBySim2 = appItem.getAppDataBySim2();
							appItem.setAppDataBySim2(dataBySim2 + appItemData.getAppDataBySim2());
						}
					}
					long total = appItem.getAppDataBySim1() + appItem.getAppDataBySim2();
					if (!TextUtils.equals(mNetType, mContext.getString(R.string.data_wifi))) {
						//超过100KB显示
						if (!(total >= MAIN_DATA)) {
							//要移除的集合
							mRemoveAppList.add(appItem);
						}
					}
				}
				mAppList.removeAll(mRemoveAppList);
				for (AppItem appItem : mRemoveAppList) {
					if (appItem.isPolicyStatus()) {
						mNoPolicyAppList.remove(appItem);
					} else {
						mPolicyAppList.remove(appItem);
					}
				}
				mRemoveAppList.clear();
				if (mNoPolicyAppList.size() > 0 && mAppList.size() >= mNoPolicyAppList.size()) {
					List<AppItem> noPolicyList = mAppList.subList(0, mNoPolicyAppList.size());
					Collections.sort(noPolicyList);
					mNoPolicyAppList.clear();
					mNoPolicyAppList.addAll(noPolicyList);
				}
				if (mPolicyAppList.size() > 0 && mAppList.size() >= mPolicyAppList.size()) {
					//根据所用流量大小进行排序
					List<AppItem> policyList = mAppList.subList(mNoPolicyAppList.size(), mAppList.size());
					Collections.sort(policyList);
					mPolicyAppList.clear();
					mPolicyAppList.addAll(policyList);
				}
				//初始化分页
				if (mAppList.size() <= PAGE_COUNT) {
					mPageAppList.addAll(mAppList);
				} else {
					List<AppItem> pageList = mAppList.subList(0, PAGE_COUNT);
					mPageAppList.addAll(pageList);
				}
				if (mAppList.size() == 0) {
					mHandler.sendEmptyMessage(EMPTY_APP);
				} else {
					mHandler.sendEmptyMessage(RAGNE_APP);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			mHandler.sendEmptyMessage(UPDATE_ITEM_APP);
			cancel(true);
		}
	}


	/**
	 * 全部拒绝
	 */
	public void rejectAll() {
		new AsyncTask<Void, Void, Void>() {
			String type = null;
			@Override
			protected Void doInBackground(Void... params) {
				try {

					if (TextUtils.equals(mNetType, mContext.getString(R.string.data_mobile))){
						type = TYPE_DATA;
					} else if (TextUtils.equals(mNetType, mContext.getString(R.string.data_wifi))) {
						type = TYPE_WLAN;
					} else if (TextUtils.equals(mNetType, mContext.getString(R.string.net_bg))) {
						type = null;
					}
					if (!TextUtils.isEmpty(type)) {
						for(int i = 0; i < mAppList.size(); i++) {
							AppItem appItem = mAppList.get(i);
							appItem.setPolicyStatus(false);
							int uid = appItem.getAppUid();
							//applyChange(type, uid, true);
						}
						save(type, mAppList);
					} else {
						for(int i = 0; i < mAppList.size(); i++) {
							AppItem appItem = mAppList.get(i);
							appItem.setPolicyStatus(false);
							int uid = appItem.getAppUid();
							applyDataChange(uid, POLICY_REJECT_METERED_BACKGROUND);
						}
					}
					mPolicyAppList.clear();
					mNoPolicyAppList.clear();
					mPolicyAppList.addAll(mAppList);
				} catch (Exception e) {
					e.printStackTrace();
				}

				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {
				super.onPostExecute(aVoid);
				updateItem(false);
				if (!TextUtils.isEmpty(type)) {
					new AsyncTask<Void, Void, Void>(){
						@Override
						protected Void doInBackground(Void... params) {
							try {

								for(int i = 0; i < mAppList.size(); i++) {
									AppItem appItem = mAppList.get(i);
									int uid = appItem.getAppUid();
									applyChange(type, uid, true);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
							return null;
						}

						@Override
						protected void onPostExecute(Void aVoid) {
							super.onPostExecute(aVoid);
						}
					}.execute();
				}
			}
		}.execute();
	}

	/**
	 * 全部允许
	 */
	public void allowAll() {
		String type = null;
		String netControllerType = null;
		try {

			if (TextUtils.equals(mNetType, mContext.getString(R.string.net_bg))) {
				type = null;
				for(int i = 0; i < mAppList.size(); i++) {
					AppItem appItem = mAppList.get(i);
					appItem.setPolicyStatus(true);
					int uid = appItem.getAppUid();
					applyDataChange(uid, POLICY_NONE);
				}
			} else {
				if (TextUtils.equals(mNetType, mContext.getString(R.string.data_mobile))){
					netControllerType = NetController.CHAIN_MOBILE;
					type = TYPE_DATA;
				} else if (TextUtils.equals(mNetType, mContext.getString(R.string.data_wifi))) {
					netControllerType = NetController.CHAIN_WIFI;
					type = TYPE_WLAN;
				}
				NetController.getInstance().clearFirewallChain(netControllerType);
				for(int i = 0; i < mAppList.size(); i++) {
					AppItem appItem = mAppList.get(i);
					appItem.setPolicyStatus(true);
				}
			}
			mNoPolicyAppList.clear();
			mPolicyAppList.clear();
			mNoPolicyAppList.addAll(mAppList);
			if (!TextUtils.isEmpty(type)) {
				save(type, mPolicyAppList);
			}
			updateItem(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
