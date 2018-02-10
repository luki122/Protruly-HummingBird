package com.hb.netmanage.service;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.NetworkPolicyManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.CompoundButton;

import com.hb.netmanage.R;
import com.hb.netmanage.adapter.RangeAppAdapter;
import com.hb.netmanage.net.NetController;
import com.hb.netmanage.utils.LogUtil;
import com.hb.netmanage.utils.PreferenceUtil;
import com.hb.netmanage.utils.ToolsUtil;
import com.hb.netmanage.view.NetManageDialogView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hb.app.dialog.AlertDialog;

/**
 * 后台服务　监听打开应用动作
 * 
 * @author zhaolaichao
 *
 */
public class AppNetState {
	private static final String TAG = "AppTaskService";
	private static final int MSG_TAG = 0;
	private static AppNetState mAppNetState = new AppNetState();
	private NetworkPolicyManager mPolicyManager;
	private PackageManager mPm;
	private ActivityManager mActivityManager;
	private AlertDialog mAlertDialog;
	private String[] mDataLimiteArray = null;
	private String[] mWlanLimiteArray = null;
	/**
	 * 过滤app
	 */
	private String[] mFilterApp = {"com.android.systemui", "com.android.dlauncher"};

	private boolean mIsContainData;
	private boolean mIsContainWlan;
	private ArrayList<Integer> mDataList = new ArrayList<Integer>();
	private ArrayList<Integer> mWlanList = new ArrayList<Integer>();
	private static Bundle mBundle = new Bundle();
	private int mPolicyUid;
	private ArrayList<String> mUIdsByPolicy;
	private Context mContext;

	private AppNetState() {
	}

	Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_TAG:
				Bundle bundle = (Bundle) msg.getData();
				String content = bundle.getString("dialog_content");
				String net_type = bundle.getString("net_type");
				int uid = bundle.getInt("uid");
				//没有勾选默认选中
				warnInfoDialog(content, net_type, uid);
				break;

			default:
				break;
			}
		};
	};

	public static AppNetState getInstance() {
		if (mAppNetState == null) {
			mAppNetState = new AppNetState();
		}
		return mAppNetState;
	}

	public void init(Context context, int uid , String packName) {
		mContext = context;
		if (mPolicyManager == null || mPm == null || mActivityManager == null) {
			mPolicyManager = NetworkPolicyManager.from(mContext);
			mPm = mContext.getPackageManager();
			mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		}
		String currentPkgName = mContext.getPackageName();
		if (TextUtils.equals(packName, currentPkgName)) {
			return;
		}
		List<ActivityManager.RunningAppProcessInfo> appProcessList = mActivityManager.getRunningAppProcesses();
		String runningProcessName = appProcessList.get(0).processName;
		if (!TextUtils.equals(packName, runningProcessName)) {
			LogUtil.v(TAG, "runningProcessName>>>>>>>" + runningProcessName + "<<packName>>" + packName);
			return;
		}
		boolean filterApp = false;
		ArrayList<ResolveInfo> resolveInfos = ToolsUtil.getResolveInfos(mContext);
		for (int i = 0; i < resolveInfos.size(); i++) {
			String packageName = resolveInfos.get(i).activityInfo.applicationInfo.packageName;
			if (TextUtils.equals(packName, packageName)) {
				filterApp = true;
				break;
			}
		}
		boolean isShowDialog = filterRunningProcessInfo(uid);
		if (isShowDialog) {
			String lastPackName  = PreferenceUtil.getString(mContext, "", PreferenceUtil.TOP_APP_NAME_KEY, null);
			LogUtil.i(TAG, "" + packName + ">>>uid>>>" + uid+ ">>>>>lastPackName>>>>>" + lastPackName);
			if (filterApp && !TextUtils.equals(packName, lastPackName)) {
				statsTopTaskApp(packName);
			}
		}
		PreferenceUtil.putString(mContext, "", PreferenceUtil.TOP_APP_NAME_KEY, packName);
	}

	/**
	 * 获得栈顶app
	 */
	private void statsTopTaskApp(String packageName) {
		synchronized (AppNetState.class) {
			   if (!TextUtils.isEmpty(packageName) && ToolsUtil.isNetworkApp(mContext, packageName)) {
				   ApplicationInfo ai;
				   try {
					   mBundle.clear();
					   mIsContainData = false;
					   mIsContainWlan = false;
					   ai = mPm.getApplicationInfo(packageName, PackageManager.GET_ACTIVITIES);
					   int uidPolicy = mPolicyManager.getUidPolicy(ai.uid);
					   mPolicyUid = ai.uid;
					   //判断是否处于禁止上网状态
					   String netType = ToolsUtil.getNetWorkType(mContext);
					   String appName = ai.loadLabel(mPm).toString();
					   if (ToolsUtil.NET_TYPE_MOBILE.equals(netType)) {
						   String data = PreferenceUtil.getString(mContext, "", RangeAppAdapter.TYPE_DATA, null);
						   if (!TextUtils.isEmpty(data)) {
							   mDataLimiteArray = data.split(",");
							   ArrayList<String> uidDataList = new ArrayList<String>( Arrays.asList(mDataLimiteArray));
							   if (uidDataList.contains("" + mPolicyUid)) {
								   String content = String.format(mContext.getString(R.string.data_stop_mobile_info), appName);
								   mBundle.putString("dialog_content", content);
								   mBundle.putString("net_type", RangeAppAdapter.TYPE_DATA);
								   mIsContainData = true;
							   }
							}
					   } else if (ToolsUtil.NET_TYPE_WIFI.equals(netType)) {
						   String wlan = PreferenceUtil.getString(mContext, "", RangeAppAdapter.TYPE_WLAN, null);
							LogUtil.d(TAG, "initSetting:" + wlan);
							if (!TextUtils.isEmpty(wlan)) {
								mWlanLimiteArray = wlan.split(",");
								 ArrayList<String> uidWlanList = new ArrayList<String>( Arrays.asList(mWlanLimiteArray));
								   if (uidWlanList.contains("" + mPolicyUid)) {
									   String content = String.format(mContext.getString(R.string.wifi_stop_info), appName);
									   mBundle.putString("dialog_content", content);
									   mBundle.putString("net_type", RangeAppAdapter.TYPE_WLAN);
									   mIsContainWlan = true;
								   }
							}
					   }
					   if (mIsContainData || mIsContainWlan) {
						   //当前栈顶应用只弹框提醒一次
						  boolean dataInfoState = PreferenceUtil.getBoolean(mContext, "" + mPolicyUid, PreferenceUtil.WARN_DATA_USED_KEY, false);
                           if (!dataInfoState) {
                              //默认不勾选，若用户勾选后下次达到条件不再进行提示
                              Message msg = mHandler.obtainMessage();
                              msg.what = MSG_TAG;
                              mBundle.putInt("uid", mPolicyUid);
                              msg.setData(mBundle);
                              mHandler.sendMessage(msg);
                           }
					   }
					   LogUtil.d("TAG", "!!>>>" + uidPolicy);
				   } catch (Exception e) {
					   e.printStackTrace();
				   } finally {
					   PreferenceUtil.putString(mContext, "", PreferenceUtil.TOP_APP_NAME_KEY, packageName);
				}
			}
		}
	}

	/**
	 * 判断指定的UID是否在运行,过滤已提示过的UID;
	 */
	private boolean filterRunningProcessInfo(int specifyUid) {
		mUIdsByPolicy = null;
		boolean dataInfoState = PreferenceUtil.getBoolean(mContext, "" + specifyUid, PreferenceUtil.WARN_DATA_USED_KEY, false);
		if (dataInfoState) {
			return false;
		}
		String uidsInfo = PreferenceUtil.getString(mContext, "", PreferenceUtil.POLICY_UIDS_DIALOG_INFO_KEY, null);
		String[] uidsArray = null;
		if (!TextUtils.isEmpty(uidsInfo)) {
		     uidsArray = uidsInfo.split(",");
		     mUIdsByPolicy  = new ArrayList<>(Arrays.asList(uidsArray));
		}
		if (mUIdsByPolicy != null) {
			 if (mUIdsByPolicy.contains("" + specifyUid)) {
				 //相同UID则之前有弹框提示过
				 return false;
			 }
		     // 通过调用ActivityManager的getRunningAppProcesses()方法获得系统里所有正在运行的进程
		     List<ActivityManager.RunningAppProcessInfo> appProcessList = mActivityManager.getRunningAppProcesses();
		     for (int i = 0; i < mUIdsByPolicy.size(); i++) {
		    	 boolean equal = false;
		    	 String uid = mUIdsByPolicy.get(i);
		    	 //判断当前运行的进程中是否有指定UID
		    	 for (int j = 0; j < appProcessList.size(); j++) {
		    		 String processUId = "" + appProcessList.get(j).uid;
		    		 if (TextUtils.equals(uid, processUId)) {
		    			 equal = true;
		    			 break;
		    		 }
				 }
		    	 if (!equal) {
		    		 if (mUIdsByPolicy.contains(uid)) {
		    			 mUIdsByPolicy.remove(uid);
		    		 }
		    	 }
			}
		     //保存提示过的UID
		    if (mUIdsByPolicy.size() == 0) {
			     PreferenceUtil.putString(mContext, "", PreferenceUtil.POLICY_UIDS_DIALOG_INFO_KEY, null);
		    } else {
		    	StringBuilder sb = new StringBuilder();
		    	for (String uid : mUIdsByPolicy) {
		    		sb.append(uid).append(",");
		    	}
		    	PreferenceUtil.putString(mContext, "", PreferenceUtil.POLICY_UIDS_DIALOG_INFO_KEY, sb.substring(0, sb.length() - 1));
		    	mUIdsByPolicy.clear();
		    }
		}
		return true;
	}

    /**
	 * 联网被禁提示
	 * @param msg
	 * @param uid
	 */
    private void warnInfoDialog(String msg, final String type, final int uid) {
    	mPolicyUid = uid;
//    	Context mContext = ((Context)mContext);
    	mContext.setTheme(com.hb.R.style.Theme_Hb_Material_Light);
    	AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
    	builder.setTitle(mContext.getString(R.string.net_connect_info));
    	NetManageDialogView dialogView = new NetManageDialogView(mContext);
    	dialogView.setMessage(msg);
    	dialogView.setOnCheckListener(new NetManageDialogView.ICheckListener() {

			@Override
			public void setOnCheckListener(CompoundButton buttonView, boolean isChecked) {
				PreferenceUtil.putBoolean(mContext, "" + mPolicyUid, PreferenceUtil.WARN_DATA_USED_KEY, isChecked);
			}
		});
        builder.setView(dialogView);
    	builder.setPositiveButton(mContext.getString(com.hb.R.string.yes), new AlertDialog.OnClickListener() {
 	          @Override
 	          public void onClick(DialogInterface dialog, int which) {
 	        	    //允许使用移动数据
 	        	    try {
 	        	    	applyChange(type, mPolicyUid, false);
 	 	            } catch (Exception e) {
 	 	        	     e.printStackTrace();
 	 	                 LogUtil.e(TAG, "No bandwidth control; leaving>>>" + e.getMessage());
 	 	           }
				  mAlertDialog.dismiss();
 	          }
 	      });
    	builder.setNegativeButton(mContext.getString(com.hb.R.string.no), new AlertDialog.OnClickListener() {
 	           @Override
 	           public void onClick(DialogInterface dialog, int which) {
				   mAlertDialog.dismiss();
 	           }
 	      });
    	builder.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				boolean isChecked = PreferenceUtil.getBoolean(mContext, "" + mPolicyUid, PreferenceUtil.WARN_DATA_USED_KEY, false);
				if (isChecked) {
					PreferenceUtil.putString(mContext, "", PreferenceUtil.POLICY_UIDS_DIALOG_INFO_KEY, getDialogUids(mPolicyUid));
				}
				LogUtil.e(TAG, "onDismiss>>>");
			}
		});
		mAlertDialog = builder.create();
		mAlertDialog.setCanceledOnTouchOutside(false);
		mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		mAlertDialog.show();
	}
    
    /**
     * 保存弹框的APP的UID
     * @param uid
     * @return
     */
    private String getDialogUids(int uid) {
    	StringBuffer uids = new StringBuffer();
    	String uidsInfo = PreferenceUtil.getString(mContext, "", PreferenceUtil.POLICY_UIDS_DIALOG_INFO_KEY, null);
    	if (!TextUtils.isEmpty(uidsInfo)) {
    		uids.append(uidsInfo).append(",").append("" + uid).append(",");
    	} else {
    		uids.append("" + uid).append(",");
    	}
    	return uids.toString();
    }
	 /**
	   * 设置上网类型
	   * @param type
	   * @param uid
	   * @param isReject
	   */
 private void applyChange(String type, int uid, boolean isReject) {
	 mDataList.clear();
	 mWlanList.clear();
	 String data = PreferenceUtil.getString(mContext, "", RangeAppAdapter.TYPE_DATA, null);
	   if (!TextUtils.isEmpty(data)) {
		   mDataLimiteArray = data.split(",");
		   for (int i = 0; i < mDataLimiteArray.length; i++) {
			    mDataList.add(Integer.parseInt(mDataLimiteArray[i]));
		   }
		}
	   String wlan = PreferenceUtil.getString(mContext, "", RangeAppAdapter.TYPE_WLAN, null);
		LogUtil.d(TAG, "initSetting:" + wlan);
		if (!TextUtils.isEmpty(wlan)) {
			mWlanLimiteArray = wlan.split(",");
			 for (int i = 0; i < mWlanLimiteArray.length; i++) {
				    mWlanList.add(Integer.parseInt(mWlanLimiteArray[i]));
			   }
		}
	 switch (type) {
		 case RangeAppAdapter.TYPE_DATA:
			 //传入true代表要禁止其联网。
			 NetController.getInstance().setFirewallUidChainRule(uid, NetController.MOBILE, isReject);
			 if (isReject) {
				 if (!mDataList.contains(uid)) {
					 mDataList.add(uid);
				 }
			 } else {
				 if (mDataList.contains(uid)) {
					 mDataList.remove((Integer)uid);
				 }
			 }
			 save(RangeAppAdapter.TYPE_DATA, mDataList);
			 break;
		 case RangeAppAdapter.TYPE_WLAN:
			 NetController.getInstance().setFirewallUidChainRule(uid, NetController.WIFI, isReject);
			 if (isReject) {
				 if (!mWlanList.contains(uid)) {
					 mWlanList.add(uid);
				 }
			 } else {
				 if (mWlanList.contains(uid)) {
					 mWlanList.remove((Integer) uid);
				 }
			 }
			 save(RangeAppAdapter.TYPE_WLAN, mWlanList);
			 break;
	 }

 }
 
  private void save(String type, ArrayList<Integer> saveList) {
	 if (saveList.size() == 0) {  
		 PreferenceUtil.putString(mContext, "", type, null);
         return;
     }
     StringBuilder sb = new StringBuilder();
     for (Integer i : saveList) {
         sb.append(i).append(",");
     }
     LogUtil.d(TAG, "sb:" + sb);
     switch (type) {
         case RangeAppAdapter.TYPE_DATA:
      	     PreferenceUtil.putString(mContext, "", RangeAppAdapter.TYPE_DATA, sb.substring(0, sb.length() - 1));
             break;
         case RangeAppAdapter.TYPE_WLAN:
      	     PreferenceUtil.putString(mContext, "", RangeAppAdapter.TYPE_WLAN, sb.substring(0, sb.length() - 1));
             break;
     }
  }
  
}