package com.hb.netmanage.service;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.NetworkPolicyManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.CompoundButton;

import com.hb.netmanage.DataManagerApplication;
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
public class AppTaskService extends AccessibilityService {
	private static final String TAG = "AppTaskService";
	public static final String APPTASK_SERVICE = "com.hb.netmanage/com.hb.netmanage.service.AppTaskService";
	//更新桌面应用图标
	public static final String UPDATE_DATAPLAN_ICON_ACTION = "com.hb.netmanage.update_dataplan_icon.action";
	private static final int MSG_TAG = 0;
	
	private NetworkPolicyManager mPolicyManager;
	private PackageManager mPm;
	private ActivityManager mActivityManager;
	private AlertDialog mAlertDialog;
	private String[] mDataLimiteArray = null;
	private String[] mWlanLimiteArray = null;
	/**
	 * 过滤app
	 */
	private String[] mFilterApp = {"com.android.systemui"};
	
	private boolean mIsContainData;
	private boolean mIsContainWlan;
	private ArrayList<Integer> mDataList = new ArrayList<Integer>();
	private ArrayList<Integer> mWlanList = new ArrayList<Integer>();
	private static Bundle mBundle = new Bundle();
	private UpdateIconReceiver mIconReceiver;
	private int mPolicyUid;
	private ArrayList<String> mUIdsByPolicy;
	
	Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
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
	
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		 if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {  
			  String packName = event.getPackageName().toString();
			  String currentPkgName = AppTaskService.this.getPackageName();
			  boolean filterState = false;
			  for (int i = 0; i < mFilterApp.length; i++) {
				  if (TextUtils.equals(packName, mFilterApp[i])) {
					  filterState = true;
					  break;
				  }
			  }
			  if (filterState || TextUtils.equals(packName, currentPkgName)) {
				  return;
			  }
			  boolean filterApp = false;
			  ArrayList<ResolveInfo> resolveInfos = ToolsUtil.getResolveInfos(AppTaskService.this);
			  for (int i = 0; i < resolveInfos.size(); i++) {
				  String packageName = resolveInfos.get(i).activityInfo.applicationInfo.packageName;
				  if (TextUtils.equals(packName, packageName)) {
					  filterApp = true;
					  break;
				  }
			  }
			  ApplicationInfo ai;
			  int uid = 0;
			  try {
				  ai = mPm.getApplicationInfo(packName, PackageManager.GET_ACTIVITIES);
				  uid = ai.uid;
			  } catch (NameNotFoundException e) {
				  e.printStackTrace();
			  }
			  boolean isShowDialog = filterRunningProcessInfo(uid);
			  if (isShowDialog) {
				  String lastPackName  = PreferenceUtil.getString(AppTaskService.this, "", PreferenceUtil.TOP_APP_NAME_KEY, null);
				  LogUtil.i(TAG, "" + packName + ">>>uid>>>" + uid+ ">>>>>lastPackName>>>>>" + lastPackName);
				  if (filterApp && !TextUtils.equals(packName, lastPackName)) {
					  statsTopTaskApp(packName);
				  }
			  }
			  PreferenceUtil.putString(AppTaskService.this, "", PreferenceUtil.TOP_APP_NAME_KEY, packName);
	     }  
		
	}

	@Override
	public void onInterrupt() {
		
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mPolicyManager = NetworkPolicyManager.from(this);
		mPm = getPackageManager();
		mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);  
	}
	
	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		 //Configure these here for compatibility with API 13 and below.  
		LogUtil.e(TAG, "onServiceConnected>>>>");
//        AccessibilityServiceInfo config = new AccessibilityServiceInfo();
//        config.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
//        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
//        if (Build.VERSION.SDK_INT >= 16)  {
//        	//Just in case this helps
//        	config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
//        }
//        setServiceInfo(config);
	}
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	registerReceiver();
    	return START_STICKY;
    }
	@Override
	public void onDestroy() {
		super.onDestroy();
		unRegisterReceiver();
		mAlertDialog = null;
		PreferenceUtil.putString(AppTaskService.this, "", PreferenceUtil.TOP_APP_NAME_KEY, null);
		DataManagerApplication.getInstance().startAccessService(AppTaskService.this);
	}
	
	private void registerReceiver() {
		mIconReceiver = new UpdateIconReceiver();
		IntentFilter filter = new IntentFilter(UPDATE_DATAPLAN_ICON_ACTION);
		AppTaskService.this.registerReceiver(mIconReceiver, filter);
	}
	
	private void unRegisterReceiver() {
		if (null != mIconReceiver) {
			AppTaskService.this.unregisterReceiver(mIconReceiver);
		}
	}
	/**
	 * 获得栈顶app
	 */
	private void statsTopTaskApp(String packageName) {
		synchronized (AppTaskService.class) {
			   if (!TextUtils.isEmpty(packageName) && ToolsUtil.isNetworkApp(AppTaskService.this, packageName)) {
				   ApplicationInfo ai;
				   try {
					   mBundle.clear();
					   mIsContainData = false;
					   mIsContainWlan = false;
					   ai = mPm.getApplicationInfo(packageName, PackageManager.GET_ACTIVITIES);
					   int uidPolicy = mPolicyManager.getUidPolicy(ai.uid);
					   mPolicyUid = ai.uid;
					   //判断是否处于禁止上网状态
					   String netType = ToolsUtil.getNetWorkType(AppTaskService.this);
					   if (ToolsUtil.NET_TYPE_MOBILE.equals(netType)) {
						   String data = PreferenceUtil.getString(AppTaskService.this, "", RangeAppAdapter.TYPE_DATA, null);
						   if (!TextUtils.isEmpty(data)) {
							   mDataLimiteArray = data.split(",");
							   ArrayList<String> uidDataList = new ArrayList<String>( Arrays.asList(mDataLimiteArray));
							   if (uidDataList.contains("" + mPolicyUid)) {
								   mBundle.putString("dialog_content", AppTaskService.this.getString(R.string.data_stop_mobile_info));
								   mBundle.putString("net_type", RangeAppAdapter.TYPE_DATA);
								   mIsContainData = true;
							   }
							}
					   } else if (ToolsUtil.NET_TYPE_WIFI.equals(netType)) {
						   String wlan = PreferenceUtil.getString(AppTaskService.this, "", RangeAppAdapter.TYPE_WLAN, null);
							LogUtil.d(TAG, "initSetting:" + wlan);
							if (!TextUtils.isEmpty(wlan)) {
								mWlanLimiteArray = wlan.split(",");
								 ArrayList<String> uidWlanList = new ArrayList<String>( Arrays.asList(mWlanLimiteArray));
								   if (uidWlanList.contains("" + mPolicyUid)) {
									   mBundle.putString("dialog_content", AppTaskService.this.getString(R.string.wifi_stop_info));
									   mBundle.putString("net_type", RangeAppAdapter.TYPE_WLAN);
									   mIsContainWlan = true;
								   }
							}
					   }
					   if (mIsContainData || mIsContainWlan) {
						   //当前栈顶应用只弹框提醒一次
						  boolean dataInfoState = PreferenceUtil.getBoolean(AppTaskService.this, "" + mPolicyUid, PreferenceUtil.WARN_DATA_USED_KEY, false);
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
					   PreferenceUtil.putString(AppTaskService.this, "", PreferenceUtil.TOP_APP_NAME_KEY, packageName);
				}
			}
		}
	}
    
	/**
	 * 判断指定的UID是否在运行,过滤已提示过的UID;
	 */
	private boolean filterRunningProcessInfo(int specifyUid) {
		mUIdsByPolicy = null;
		String uidsInfo = PreferenceUtil.getString(AppTaskService.this, "", PreferenceUtil.POLICY_UIDS_DIALOG_INFO_KEY, null);
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
			     PreferenceUtil.putString(AppTaskService.this, "", PreferenceUtil.POLICY_UIDS_DIALOG_INFO_KEY, null);
		    } else {
		    	StringBuilder sb = new StringBuilder();
		    	for (String uid : mUIdsByPolicy) {
		    		sb.append(uid).append(",");
		    	}
		    	PreferenceUtil.putString(AppTaskService.this, "", PreferenceUtil.POLICY_UIDS_DIALOG_INFO_KEY, sb.substring(0, sb.length() - 1));
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
    	Context context = ((Context)AppTaskService.this);
    	context.setTheme(com.hb.R.style.Theme_Hb_Material_Light);
    	hb.app.dialog.AlertDialog.Builder builder = new hb.app.dialog.AlertDialog.Builder(context);
    	builder.setTitle(AppTaskService.this.getString(R.string.net_connect_info));
    	NetManageDialogView dialogView = new NetManageDialogView(AppTaskService.this);
    	dialogView.setMessage(msg);
    	dialogView.setOnCheckListener(new NetManageDialogView.ICheckListener() {
			
			@Override
			public void setOnCheckListener(CompoundButton buttonView, boolean isChecked) {
				PreferenceUtil.putBoolean(AppTaskService.this, "" + mPolicyUid, PreferenceUtil.WARN_DATA_USED_KEY, isChecked);
			}
		});
        builder.setView(dialogView); 
    	builder.setPositiveButton(AppTaskService.this.getString(com.hb.R.string.yes), new hb.app.dialog.AlertDialog.OnClickListener() {
 	          @Override
 	          public void onClick(DialogInterface dialog, int which) {
 	        	    //允许使用移动数据
 	        	    try {
 	        	    	applyChange(type, mPolicyUid, false);
 	        	    	PreferenceUtil.putString(AppTaskService.this, "", PreferenceUtil.POLICY_UIDS_DIALOG_INFO_KEY, getDialogUids(mPolicyUid));
 	 	            } catch (Exception e) {
 	 	        	     e.printStackTrace();
 	 	                 LogUtil.e(TAG, "No bandwidth control; leaving>>>" + e.getMessage());
 	 	           }
 	               dialog.dismiss();
 	          }
 	      });
    	builder.setNegativeButton(AppTaskService.this.getString(com.hb.R.string.no), new hb.app.dialog.AlertDialog.OnClickListener() {
 	           @Override
 	           public void onClick(DialogInterface dialog, int which) {
 	        	  PreferenceUtil.putString(AppTaskService.this, "", PreferenceUtil.POLICY_UIDS_DIALOG_INFO_KEY, getDialogUids(mPolicyUid));
 	              dialog.dismiss();
 	           }
 	      });
    	builder.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				
			}
		});
    	if (null == mAlertDialog) {
    		mAlertDialog = builder.create();
    	}
    	if (null != mAlertDialog && !mAlertDialog.isShowing()) {
    		mAlertDialog.setCanceledOnTouchOutside(false);
    		mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
    		mAlertDialog.show();
    	}
    }
    
    /**
     * 保存弹框的APP的UID
     * @param uid
     * @return
     */
    private String getDialogUids(int uid) {
    	StringBuffer uids = new StringBuffer();
    	String uidsInfo = PreferenceUtil.getString(AppTaskService.this, "", PreferenceUtil.POLICY_UIDS_DIALOG_INFO_KEY, null);
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
	 String data = PreferenceUtil.getString(AppTaskService.this, "", RangeAppAdapter.TYPE_DATA, null);
	   if (!TextUtils.isEmpty(data)) {
		   mDataLimiteArray = data.split(",");
		   for (int i = 0; i < mDataLimiteArray.length; i++) {
			    mDataList.add(Integer.parseInt(mDataLimiteArray[i]));
		   }
		}
	   String wlan = PreferenceUtil.getString(AppTaskService.this, "", RangeAppAdapter.TYPE_WLAN, null);
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
		 PreferenceUtil.putString(AppTaskService.this, "", type, null);
         return;
     }
     StringBuilder sb = new StringBuilder();
     for (Integer i : saveList) {
         sb.append(i).append(",");
     }
     LogUtil.d(TAG, "sb:" + sb);
     switch (type) {
         case RangeAppAdapter.TYPE_DATA:
      	     PreferenceUtil.putString(AppTaskService.this, "", RangeAppAdapter.TYPE_DATA, sb.substring(0, sb.length() - 1));
             break;
         case RangeAppAdapter.TYPE_WLAN:
      	     PreferenceUtil.putString(AppTaskService.this, "", RangeAppAdapter.TYPE_WLAN, sb.substring(0, sb.length() - 1));
             break;
     }
  }
  
  class UpdateIconReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (UPDATE_DATAPLAN_ICON_ACTION.equals(intent.getAction())) {
			//用于更新图标
			ToolsUtil.updateIconReceiver();
		}
	}
  }
}