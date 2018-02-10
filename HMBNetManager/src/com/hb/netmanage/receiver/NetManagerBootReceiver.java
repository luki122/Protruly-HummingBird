package com.hb.netmanage.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.hb.netmanage.DataManagerApplication;
import com.hb.netmanage.fragement.SimFragment;
import com.hb.netmanage.net.NetController;
import com.hb.netmanage.service.AppNetState;
import com.hb.netmanage.utils.LogUtil;
import com.hb.netmanage.utils.PreferenceUtil;

import java.util.ArrayList;
import java.util.Collections;

import static com.hb.netmanage.adapter.RangeAppAdapter.TYPE_DATA;
import static com.hb.netmanage.adapter.RangeAppAdapter.TYPE_WLAN;

/**
 * 开机事件监听
 * 
 * @author
 */
public final class NetManagerBootReceiver extends BroadcastReceiver {

	/**
	 * 通过设置清除应用数据广播
	 */
	private static final String ACTION_SETTINGS_PACKAGE_DATA_CLEARED = "com.mediatek.intent.action.SETTINGS_PACKAGE_DATA_CLEARED";
	/**
	 * /frameworks/base/services/core/java/com/android/server/am/ActivityStackSupervisor.java: startActivityLocked
	 * com.android.server.am
	 */
	public static final String OPEN_APP_ACTION = "com.hbmonster.open.app.action";
	public static final int OPEN_APP_TAG = 1000;

	private Context mContext;

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case OPEN_APP_TAG:
					if (mContext == null) {
						return;
					}
					int uid = msg.arg1;
					String packageName = String.valueOf(msg.obj);
					AppNetState.getInstance().init(mContext, uid, packageName);
					break;
			}
		}
	};
	@Override
	public void onReceive(Context context, final Intent intent) {
		mContext = context;
		LogUtil.e("NetManagerBootReceiver", "NetManagerBootReceiver>>>" + intent.getAction());
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			// 监听用户打开应用操作
			//DataManagerApplication.getInstance().startAccessService(context);
			//重置网络限制
			setNetType (context);
			//清除保存
			PreferenceUtil.putString(context, "", PreferenceUtil.TOP_APP_NAME_KEY, null);
			//初始化校正
			for (int i = 0; i < DataManagerApplication.mImsiArray.length; i ++) {
				PreferenceUtil.putInt(context, DataManagerApplication.mImsiArray[i], PreferenceUtil.CORRECT_DATA_KEY, SimFragment.BTN_CORRECT_TAG);
			}
		} else if (ACTION_SETTINGS_PACKAGE_DATA_CLEARED.equals(intent.getAction())) {
			String pkgName = intent.getStringExtra("packageName");
			if (TextUtils.equals(pkgName, context.getPackageName())) {
				NetController.getInstance().clearFirewallChain(NetController.CHAIN_MOBILE);
				NetController.getInstance().clearFirewallChain(NetController.CHAIN_WIFI);
			}
		} else if (TextUtils.equals(intent.getAction(), OPEN_APP_ACTION)) {
			if (handler.hasMessages(OPEN_APP_TAG)) {
				handler.removeMessages(OPEN_APP_TAG);
				return;
			}

			new AsyncTask<Void, Void, Void>(){
				@Override
				protected Void doInBackground(Void... params) {
					int uid = intent.getIntExtra("APP_UID", 0);
					String packageName = intent.getStringExtra("APP_PKNAME");
					Message msg = handler.obtainMessage();
					msg.what = OPEN_APP_TAG;
					msg.arg1 = uid;
					msg.obj = packageName;
					handler.sendMessageDelayed(msg, 1000);
					LogUtil.e("AppReceiver", "uid>>>>" + uid + "<<<packName<<<" + packageName);
					return null;
				}
			}.execute();

		}
	}

	/**
	 * 重置网络限制
	 *
	 * @param context
	 *
	 * 调用mNetworkService.setFirewallUidChainRule　or mNetworkService.clearFirewallChain设置和清空相应的APP的限制即可．
	 * 1.每次重新开机，其Iptable都会被清空，如果下次重新开机时，需要重新下一遍command
	 * 2.setFirewallUidChainRule这个方法,针对同一个AP其allow和deny要成对出现
	 * 如果是要禁止掉某个APP访问网络的话，应该是要下allow,而不是下deny，deny是不禁止，allow是允许禁止
	 * clearFirewallChain是重置规则,一般在APK reset的时候使用,它里面传的参数可以为wifi 或者mobile
	 *
	 *
     */
	private void setNetType (Context context) {
		String data = PreferenceUtil.getString(context, "", TYPE_DATA, null);
		String wifi = PreferenceUtil.getString(context, "", TYPE_WLAN, null);
		String[] uidArray = null;
		ArrayList<String> netList = new ArrayList<String>();
		if (!TextUtils.isEmpty(data)) {
			//设置数据流量限制
			uidArray = data.split(",");
			Collections.addAll(netList, uidArray);
			for(int i = 0; i < netList.size(); i++) {
				int uid = Integer.parseInt(netList.get(i));
				NetController.getInstance().setFirewallUidChainRule(uid, NetController.MOBILE, true);
			}
			netList.clear();
		}
		if (!TextUtils.isEmpty(wifi)) {
			//设置wifi限制
			uidArray = wifi.split(",");
			Collections.addAll(netList, uidArray);
			for(int i = 0; i < netList.size(); i++) {
				int uid = Integer.parseInt(netList.get(i));
				NetController.getInstance().setFirewallUidChainRule(uid, NetController.WIFI, true);
			}
			netList.clear();
		}
	}


}
