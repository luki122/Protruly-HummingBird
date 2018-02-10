package com.hb.netmanage.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.hb.netmanage.DataCorrect;
import com.hb.netmanage.DataManagerApplication;
import com.hb.netmanage.activity.MainActivity;
import com.hb.netmanage.adapter.RangeAppAdapter;
import com.hb.netmanage.net.NetController;
import com.hb.netmanage.utils.LogUtil;
import com.hb.netmanage.utils.PreferenceUtil;
import com.hb.netmanage.utils.StringUtil;
import com.hb.netmanage.utils.ToolsUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

import com.hb.themeicon.theme.IconManager;
/**
 * 广播接收
 * 
 * @author zhaolaichao
 */
public class NetManagerReceiver extends BroadcastReceiver {

	private static final String TAG = "NetManagerReceiver" ;
	public static NetManagerReceiver mReceiver = new NetManagerReceiver();
	public static final String ACTION_UPDATE_DATA_STATE = "com.hb.netmanage.action.updatedata_state";

	private Context mContext;

	public static synchronized NetManagerReceiver getInstance() {
		return mReceiver;
	}

	public NetManagerReceiver() {
		super();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		mContext = context;
		String action = intent.getAction();
		if (Intent.ACTION_DATE_CHANGED.equals(action)) {
			// 当Day发生变化时回调
			 String activeDataImsi = ToolsUtil.getActiveSimImsi(context);
			 PreferenceUtil.putBoolean(context, activeDataImsi, PreferenceUtil.NOTIFY_WARN_DAY_KEY, false);
			 PreferenceUtil.putBoolean(context, "", PreferenceUtil.DATE_CHANGE_KEY, false);
			 //当前月天数
			 int days = StringUtil.getMonthDays();
			 //清除日使用流量
			 for (int i = 0; i < DataManagerApplication.mImsiArray.length; i++) {
				 if (!TextUtils.isEmpty(DataManagerApplication.mImsiArray[i])) {
					 PreferenceUtil.putLong(context, DataManagerApplication.mImsiArray[i], PreferenceUtil.DAY_USED_STATS_KEY, 0);
					 //月结日
					 int closeDay = PreferenceUtil.getInt(context, DataManagerApplication.mImsiArray[i], PreferenceUtil.CLOSEDAY_KEY, 1);
					 if (closeDay == days) {
						 clearMonth(context, DataManagerApplication.mImsiArray[i]);
					 }
					 //自动流量校正
					 setAutoCorrect(context, i, DataManagerApplication.mImsiArray[i]);
				 }
			 }


		} else if (TextUtils.equals(ConnectivityManager.CONNECTIVITY_ACTION, intent.getAction())) {
//			long lastCorrectTime = 0;//上次校正成功的时间
//			Calendar calendar = Calendar.getInstance();
////			for (int i = 0; i < DataManagerApplication.mImsiArray.length; i++) {
////			}
//			String activeImsi = ToolsUtil.getActiveSimImsi(mContext);
//			if (TextUtils.isEmpty(activeImsi)) {
//				return;
//			}
//			boolean simInfoState = PreferenceUtil.getBoolean(mContext, activeImsi, PreferenceUtil.SIM_BASEINFO_KEY, false);
//			boolean correctState = PreferenceUtil.getBoolean(mContext, activeImsi, PreferenceUtil.AUTO_CORRECT_STATE_KEY, simInfoState);
//			if (!simInfoState || !correctState) {
//				return;
//			}
//			//流量校正完成时间
//			lastCorrectTime = PreferenceUtil.getLong(mContext, activeImsi, PreferenceUtil.CORRECT_OK_TIME_KEY, 0);
//			calendar.setTimeInMillis(lastCorrectTime);
//			int lastDay = calendar.get(Calendar.DAY_OF_MONTH);
//			calendar.setTimeInMillis(System.currentTimeMillis());
//			int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
//			LogUtil.v(TAG, "lastDay>>>>>" + lastDay + ">>currentDay>>>>" + currentDay);
//			if (lastDay != currentDay) {
//				//自动流量校正
//				setAutoCorrect(context, ToolsUtil.getCurrentNetSimSubInfo(mContext), activeImsi);
//			}
		} else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
			//用于更新图标
			ToolsUtil.updateIconReceiver();
		} else if (ACTION_UPDATE_DATA_STATE.equals(action)) {
			int uid = intent.getIntExtra("CHAGE_STATE_UID", 0);
			boolean state = intent.getBooleanExtra("CHAGE_STATE", false);
			if (!state) {
				//传入true代表要禁止其联网。
				NetController.getInstance().setFirewallUidChainRule(uid, NetController.MOBILE, true);
			} else {
				NetController.getInstance().setFirewallUidChainRule(uid, NetController.MOBILE, false);
			}
			String data = PreferenceUtil.getString(context, "", RangeAppAdapter.TYPE_DATA, null);
			String[] uidArray = null;
			ArrayList<String> userList = new ArrayList<String>();
			if (!TextUtils.isEmpty(data)) {
				uidArray = data.split(",");
				Collections.addAll(userList, uidArray);
			}
			if (userList.contains("" + uid)) {
				if (!state) {
					return;
				} else {
					userList.remove("" + uid);
				}
			} else {
				userList.add("" + uid);
			}
			save(userList);
		} else if("com.hb.theme.ACTION_THEME_CHANGE".equals(action)){
			LogUtil.d(TAG, "theme changed:"+action);
			IconManager.clearCaches();
		}

	}

	/**
	 * 自动流量校正  每天凌晨0点钟
	 * @param context
	 * @param simIndex
	 * @param imsi
     */
	private synchronized void setAutoCorrect(Context context, int simIndex, String imsi) {
		boolean simInfoState = PreferenceUtil.getBoolean(context, imsi, PreferenceUtil.SIM_BASEINFO_KEY, false);
		boolean correctState = PreferenceUtil.getBoolean(context, imsi, PreferenceUtil.AUTO_CORRECT_STATE_KEY, simInfoState);
		if (ToolsUtil.isNetworkAvailable(context)) {
			//当前网络可用
			if (simInfoState && correctState) {
				DataCorrect dataCorrect = DataCorrect.getInstance();
				dataCorrect.initCorrect(context, null);
				dataCorrect.startCorrect(context, false, simIndex);
			}
		}
	}

	/**
	 * 月结日到时清除上个月的数据
	 * @param context
	 * @param imsi
	 */
   private void clearMonth(Context context, String imsi) {
	   //已用套餐流量
	   PreferenceUtil.putLong(context, imsi, PreferenceUtil.USED_DATAPLAN_COMMON_KEY, 0);
	   PreferenceUtil.putLong(context, imsi, PreferenceUtil.USED_DATAPLAN_AFTER_COMMON_KEY, 0);
	   //已用闲时流量
	   PreferenceUtil.putLong(context, imsi, PreferenceUtil.USED_FREE_DATA_TOTAL_KEY, 0);
	   //常规-剩余
	   PreferenceUtil.putLong(context, imsi, PreferenceUtil.REMAIN_DATAPLAN_COMMON_KEY, 0);
	   PreferenceUtil.putLong(context, imsi, PreferenceUtil.REMAIN_DATAPLAN_AFTER_COMMON_KEY, 0);
	   //闲时-剩余
	   PreferenceUtil.putLong(context, imsi, PreferenceUtil.REMAIN_FREE_DATA_TOTAL_KEY, 0);
	   //清除日使用流量
	   PreferenceUtil.putLong(context, imsi, PreferenceUtil.DAY_USED_STATS_KEY, 0);
   }
   
	private void save( ArrayList<String> saveList) {
		   if (saveList.size() == 0) {
			   PreferenceUtil.putString(mContext, "", RangeAppAdapter.TYPE_DATA, null);
	           return;
	       }
	       StringBuilder sb = new StringBuilder();
	       for (String item : saveList) {
	           sb.append(item).append(",");
	       }
	       LogUtil.d(TAG, "sb:" + sb);
	       PreferenceUtil.putString(mContext, "", RangeAppAdapter.TYPE_DATA, sb.substring(0, sb.length() - 1));
    }
}
