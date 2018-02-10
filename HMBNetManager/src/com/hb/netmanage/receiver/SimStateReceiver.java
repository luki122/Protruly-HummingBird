package com.hb.netmanage.receiver;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.hb.netmanage.DataManagerApplication;
import com.hb.netmanage.utils.LogUtil;
import com.hb.netmanage.utils.PreferenceUtil;
import com.hb.netmanage.utils.ToolsUtil;

/**
 * 插拔手机卡监听广播
 * @author zhaolaichao
 */
public class SimStateReceiver extends BroadcastReceiver {
	private final String TAG = "SimStateReceiver";
	private final static String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
	/**
	 * 提示设置套餐action
	 */
	public final static String ACTION_NOTIFY_SET_DATAPLAN = "com.hb.netmanage.action.NOTIFY_SET_DATAPLAN";
	public final static String ACTION_HMB_SIM_STATUS = "com.hb.netmanage.action.hmb_sim_status";
	
	public final static int SIM_VALID = 0;
	public final static int SIM_INVALID = 1;
	/**
	 * 飞行模式：打开
	 */
	public final static int AIRPLANE_MODE_ON = 1;
	/**
	 * 飞行模式：关闭
	 */
	public final static int AIRPLANE_MODE_OFF = 0;

	private int simState = SIM_INVALID;

	public int getSimState() {
		return simState;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		LogUtil.v("SimStateReceiver", "sim state changed>>" + intent.getAction());
		if (intent.getAction().equals(ACTION_SIM_STATE_CHANGED)) {
			TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
			int state = tm.getSimState();
			switch (state) {
			case TelephonyManager.SIM_STATE_READY:
				simState = SIM_VALID;
				break;
			case TelephonyManager.SIM_STATE_UNKNOWN:
			case TelephonyManager.SIM_STATE_ABSENT:
			case TelephonyManager.SIM_STATE_PIN_REQUIRED:
			case TelephonyManager.SIM_STATE_PUK_REQUIRED:
			case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
			default:
				simState = SIM_INVALID;
				break;
			}
			DataManagerApplication.mImsiArray = ToolsUtil.getIMSI(context);
			if (simState == SIM_VALID) {
				//更新当前上网卡的imsi
				//更新IMSI信息
				String activeSimImsi = ToolsUtil.getActiveSimImsi(context);
				String saveNetSimImsi = PreferenceUtil.getString(context, "", PreferenceUtil.CURRENT_ACTIVE_IMSI_KEY, null);
				if (!TextUtils.equals(activeSimImsi, saveNetSimImsi)) {
					if (!TextUtils.isEmpty(activeSimImsi)) {
						PreferenceUtil.putString(context, "", PreferenceUtil.CURRENT_ACTIVE_IMSI_KEY, activeSimImsi);
						//当sim卡的状态发生改变时且不前上网卡没有设置套餐
					}
				}
			}
			PreferenceUtil.putString(context, PreferenceUtil.SIM_1, PreferenceUtil.IMSI_KEY, DataManagerApplication.mImsiArray[0]);
			PreferenceUtil.putString(context, PreferenceUtil.SIM_2, PreferenceUtil.IMSI_KEY, DataManagerApplication.mImsiArray[1]);
		} else if (TextUtils.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED, intent.getAction())) {
			String airState = Settings.System.getString(context.getContentResolver(),android.provider.Settings.Global.AIRPLANE_MODE_ON);
			Log.e(TAG, "飞行模式状态 1为开启状态，0为关闭状态 airState==" + airState);
			if (TextUtils.equals(airState, "" + AIRPLANE_MODE_ON)) {
				simState = SIM_INVALID;
			} else if (TextUtils.equals(airState, "" + AIRPLANE_MODE_OFF)) {
				simState = SIM_VALID;
			}

		}
		Intent simIntent = new Intent();
		simIntent.putExtra("SIM_STATE", simState);
		simIntent.setAction(ACTION_HMB_SIM_STATUS);
		context.sendBroadcast(simIntent);
	}
}
