package com.hb.netmanage.service;

//import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED;

import android.app.AppOpsManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicyManager;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.WindowManager;
import android.widget.CompoundButton;

import com.hb.netmanage.DataCorrect;
import com.hb.netmanage.DataManagerApplication;
import com.hb.netmanage.NotifyInfo;
import com.hb.netmanage.R;
import com.hb.netmanage.activity.DataRangeActivity;
import com.hb.netmanage.activity.MainActivity;
import com.hb.netmanage.activity.SimDataSetActivity;
import com.hb.netmanage.observer.StatusObserver;
import com.hb.netmanage.observer.UpdateObserver;
import com.hb.netmanage.utils.LogUtil;
import com.hb.netmanage.utils.NotificationUtil;
import com.hb.netmanage.utils.PreferenceUtil;
import com.hb.netmanage.utils.StringUtil;
import com.hb.netmanage.utils.ToolsUtil;
import com.hb.netmanage.view.NetManageDialogView;
import com.mst.tms.NetInfoEntity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;

import hb.app.dialog.AlertDialog.Builder;

import static android.net.NetworkStats.SET_DEFAULT;
import static android.net.NetworkTemplate.buildTemplateMobileAll;

/**
 * 后台服务
 * 
 * @author zhaolaichao
 *
 */
public class NetManagerService extends Service {

	private static final String TAG = "NetManagerService";
	public static final String UPDATE_UI = "com.hb.netmanage.update.ui";
	public static final String UPDATE_STATS = "com.hb.update_stats.action";
	//更新桌面应用图标
	public static final String UPDATE_DATAPLAN_ICON_ACTION = "com.hb.netmanage.update_dataplan_icon.action";

	private static final Uri SMS_URI = Uri.parse("content://sms/");
	/**
	 * 切换上网卡
	 */
	private static final Uri DATA_CHANGED_URI = Uri.parse("content://settings/global");
	/**
	 * 单位时间内（1分钟）后台跑了至少10M流量
	 */
	private static final long MAX_MINUTE_BGDATA = 10 * 1024 * 1024; 
	/**
	 * 单位时间内（1分钟）
	 */
	private static final int UNIT_MINUTE = 60 * 1000; 
	/**
	 * 提示单位时间内（1分钟）
	 */
	private static final int WARN_INFO_DIALOG = 1000; 
	/**
	 * 流量监控提示
	 */
	private static final int DATA_NOTIFI  = 1001;
	/**
	 * 解析短信
	 */
	private static final int SMS_NOTIFI  = 1002;
	/**
	 * 统计月使用量
	 */
	private static final int DATA_STATUS_MONTH = 1;
	/**
	 * 统计日使用量
	 */
	private static final int DATA_STATUS_DAY = 2;
	/**
	 * 统计每分钟后台使用量
	 */
	private static final int DATA_STATUS_BG = 3;
	/**
	 * 统计当日使用闲时流量
	 */
	private static final int DATA_FREE_STATUS_DAY = 4;
	/**
	 * 统计月使用闲时流量
	 */
	private static final int DATA_FREE_STATUS_MONTH = 5;
	/**
	 * 切换网络方式
	 */
	private static final int SIM_CHANGE_STATE = 2000;
	/**
	 * 每日已用流量的最大限制
	 */
	private static final float LIMIT_RATE_DAY = 0.5f;
	/**
	 * 每月已用流量的最大限制
	 */
	private static final float LIMIT_RATE_MONTH = 0.95F;

	
	private NetworkPolicyManager mPolicyManager;
	private NetworkTemplate mTemplate;
	private INetworkStatsSession mStatsSession;
	private INetworkStatsService mStatsService;
	private TelephonyManager mTelephonyManager;
	
	private DataCorrect mDataCorrect;
	private UpdateIconReceiver mIconReceiver;

	private hb.app.dialog.AlertDialog.Builder mBuilder;
	private hb.app.dialog.AlertDialog mAlertDialog;
	
	private StatsDataTask mTaskBg;
	private StatsDataTask mTaskDay;
	private StatsDataTask mTaskMonth;
	private StatsFreeDataTask mTaskFreeMonth;
	private StatsFreeDataTask mTaskFreeDay;
	
	private boolean mFreeStaus;
	private boolean mUpdateMain;
	private static NetInfoEntity mInfoEntity = new NetInfoEntity();
	
	private  long mFreeUsedForMonth = 0;
	private long mDataForMinute = 0;

	private String mActiveDataImsi;
	
	Handler handler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			    case WARN_INFO_DIALOG:
				     //单位时间内（1分钟)提示
				     long usedDataBg = (Long) msg.obj;
					 showInfoMinute(NetManagerService.this, usedDataBg);
				     break;
			    case DATA_NOTIFI:
					 if (TextUtils.equals(ToolsUtil.NET_TYPE_MOBILE,ToolsUtil.getNetWorkType(NetManagerService.this))) {
						 //手机使用移动数据时提示
						 showNotifyMsg(NetManagerService.this, mInfoEntity);
						 showDataDialogInfo(NetManagerService.this, mInfoEntity);
					 }
					String activeSimImsi = null;
					if (msg.obj != null) {
						activeSimImsi = (String) msg.obj;
						Intent intent = new Intent(UPDATE_STATS);
						intent.putExtra("active_imsi", activeSimImsi);
						intent.putExtra("netinfo", mInfoEntity);
						sendBroadcast(intent);
					}
				     break;
				case SMS_NOTIFI:
					int subId = msg.arg1;
					int state = msg.arg2;
					String  imsi = ToolsUtil.getActiveSubscriberId(NetManagerService.this, subId);
					String[] imsiArray = DataManagerApplication.getInstance().mImsiArray;
					if (null == imsiArray) {
						imsiArray = ToolsUtil.getIMSI(NetManagerService.this);
					}
					int smsIndex = -1;
					for (int i = 0; i < imsiArray.length; i++) {
						if (TextUtils.equals(imsi, imsiArray[i])) {
							smsIndex = i;
							break;
						}
					}
					LogUtil.v(TAG, "subId>>>" + subId + ">>imsi>>" + imsi + ">>state>" + state);
					if (state < 0) {
						DataCorrect.getInstance().sendSMSFail(smsIndex, state);
						return;
					}
					DataCorrect.getInstance().analysisSMS(smsIndex, String.valueOf(msg.obj));
					break;
				case SIM_CHANGE_STATE:
					changeSimState();
					break;
			    default:
					break;
			}
		};
	};
	
	
	@Override
	public IBinder onBind(Intent intent) {
		return new Binder();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		initStats();
		if (mDataCorrect == null) {
			mDataCorrect = DataCorrect.getInstance();
		}
		registerSmsObserver();
		registerObserver();
		registerStatusObserver();
		registerReceiver();
//		handler.postDelayed(statsRn, UNIT_MINUTE);
		handler.removeCallbacks(statsRn);
		new Thread(statsRn).start();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		correctMsg(intent);
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (null != mAlertDialog) {
			mAlertDialog.dismiss();
			mAlertDialog = null;
		}
		unRegisterObserver();
		unRegisterSmsObserver();
		unRegisterStatusObserver();
		unRegisterReceiver();
		if (mTaskMonth != null) {
			mTaskMonth.cancel(true);
			mTaskMonth = null;
		}
		if (mTaskDay != null) {
			mTaskDay.cancel(true);
			mTaskDay = null;
		}
		if (mTaskBg != null) {
			mTaskBg.cancel(true);
			mTaskBg = null;
		}
		if (mTaskFreeMonth != null) {
			mTaskFreeMonth.cancel(true);
			mTaskFreeMonth = null;
		}
		if (mTaskFreeDay != null) {
			mTaskFreeDay.cancel(true);
			mTaskFreeDay = null;
		}
		statsRn = null;
		TrafficStats.closeQuietly(mStatsSession);
	}

	/**
	 * 发送流量校正短信
	 */
	private void correctMsg(Intent intent) {
		if (null != intent) {
			String imsi = intent.getStringExtra("CURRENT_IMSI");
			LogUtil.v(TAG, "correctMsg>>" + imsi);
			if ( !TextUtils.isEmpty(imsi)) {
				int simSlotIndex = ToolsUtil.getCurrentNetSimSubInfo(this);
				if (simSlotIndex != -1) {
					mDataCorrect.startCorrect(NetManagerService.this, false, simSlotIndex);
				}
			}
		}
	}

	private void registerReceiver() {
		mIconReceiver = new UpdateIconReceiver();
		IntentFilter filter = new IntentFilter(UPDATE_DATAPLAN_ICON_ACTION);
		registerReceiver(mIconReceiver, filter);
	}

	private void unRegisterReceiver() {
		if (null != mIconReceiver) {
			unregisterReceiver(mIconReceiver);
		}
	}

	/**
	 * 监听切换上网卡
	 */
	private void registerObserver() {
		// 在这里启动ToolsUtil
	     ContentResolver resolver = getContentResolver();
	     resolver.registerContentObserver(DATA_CHANGED_URI, true, observer);
	}
	
	private void unRegisterObserver() {
		ContentResolver resolver = getContentResolver();
		resolver.unregisterContentObserver(observer);
	}
	
	private ContentObserver observer = new ContentObserver(new Handler()){

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			handler.sendEmptyMessageDelayed(SIM_CHANGE_STATE, 10000);
		}
	};

	private void changeSimState() {
		try {
			//切换上网卡
			String activeSimImsi = ToolsUtil.getActiveSimImsi(NetManagerService.this);
			String saveActiveSimImsi = PreferenceUtil.getString(NetManagerService.this, "", PreferenceUtil.CURRENT_ACTIVE_IMSI_KEY, null);
			if (TextUtils.equals(ToolsUtil.NET_TYPE_WIFI, ToolsUtil.getNetWorkType(NetManagerService.this))) {
				NotificationUtil.clearNotify(NetManagerService.this, NotificationUtil.TYPE_NORMAL);
				//当wifi打开时不操作
				PreferenceUtil.putString(NetManagerService.this, "", PreferenceUtil.CURRENT_ACTIVE_IMSI_KEY, activeSimImsi);
				return;
			}
			if (!TextUtils.equals(saveActiveSimImsi, activeSimImsi)) {
				LogUtil.v(TAG, "ContentObserver>>mContext>>" + activeSimImsi);
				NotificationUtil.clearNotify(NetManagerService.this, NotificationUtil.TYPE_NORMAL);
				NotifyInfo.showNotify(NetManagerService.this);
				//更新统计流量信息
				handler.removeCallbacks(statsRn);
				initStats();
				//初始化更新主界面
				handler.post(statsRn);
				mUpdateMain = false;
			}
			PreferenceUtil.putString(NetManagerService.this, "", PreferenceUtil.CURRENT_ACTIVE_IMSI_KEY, activeSimImsi);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * 监听短信
	 */
	private void registerSmsObserver() {
		// 在这里启动
		ContentResolver resolver = this.getContentResolver();
		resolver.registerContentObserver(SMS_URI, true, smsObserver);
	}

	private void unRegisterSmsObserver() {
		ContentResolver resolver = getContentResolver();
		resolver.unregisterContentObserver(smsObserver);
	}

	public ContentObserver smsObserver = new ContentObserver(handler){

		@Override
		public void onChange(boolean selfChange, Uri uri) {
			super.onChange(selfChange, uri);
			Log.v(TAG, "onchange>>uri>>" + uri);
			if (uri.toString().equals("content://sms/raw")) {
				return;
			}
			Cursor cursor = null;
			AppOpsManager appOpsManager = null;
			int subId = 0;
			String imsi = null;
			try {
				//读取收件箱中的短信  请求默认短信应用权限
				String where = " date >  " + (System.currentTimeMillis() - 5 * 1000);
				String[] projection = new String[] { "body", "address", "person", "thread_id","_id", "sub_id", "type" };
				cursor = getContentResolver().query(uri, projection, where, null, "date desc");
				Log.v(TAG, "cursor>>getCount>>" + cursor.getCount());
				if (cursor != null && cursor.getCount() > 0) {
					appOpsManager = (AppOpsManager) getSystemService("appops");
					appOpsManager.setMode(15, android.os.Process.myUid(), getPackageName(), 0);
//	            	 _id, thread_id, address, person, date, date_sent, protocol, read, status, type, reply_path_present, subject, body, service_center, locked, sub_id, error_code, creator, seen, priority, timezone, car_code]
					if (cursor.moveToNext()) {
						String number = cursor.getString(cursor.getColumnIndex("address"));//手机号
						subId = cursor.getInt(cursor.getColumnIndex("sub_id"));
						int id = cursor.getInt(cursor.getColumnIndex("_id"));
						int type = cursor.getInt(cursor.getColumnIndex("type"));
						String body = cursor.getString(cursor.getColumnIndex("body"));
						imsi = ToolsUtil.getActiveSubscriberId(NetManagerService.this, subId);
						long sentMsgTime = PreferenceUtil.getLong(NetManagerService.this, imsi, PreferenceUtil.SIM_SUBID_SMS_KEY, 0);
						if (sentMsgTime == 0) {
							return;
						}
						String bodyPhoneNo = PreferenceUtil.getString(NetManagerService.this, imsi, PreferenceUtil.SMS_BODY_KEY, null);
						if (TextUtils.isEmpty(bodyPhoneNo) || !bodyPhoneNo.contains(",")) {
							return;
						}
						String[] smsContent = bodyPhoneNo.split(",");
						if (!TextUtils.isEmpty(body) && number.equals(smsContent[1])){
							if (TextUtils.equals(number, "10086") || TextUtils.equals(number, "10010") || TextUtils.equals(number, "10001")) {
								Log.v(TAG, "onchange>>type>>" + type);
								if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
									//收件箱
									Message msg = handler.obtainMessage(SMS_NOTIFI);
									msg.obj = body;
									msg.arg1 = subId;
									msg.sendToTarget();
								}
								int count = 0;
								if (type == Telephony.Sms.MESSAGE_TYPE_SENT && body.equals(smsContent[0]) ) {
									Log.v(TAG, "发送相隔时间>>>" + (System.currentTimeMillis() - sentMsgTime));
									if (System.currentTimeMillis() - sentMsgTime < 8 * 1000) {
										//删除发送短信间隔为8s
										count = getContentResolver().delete(SMS_URI, "_id=?", new String[]{String.valueOf(id)});
									}
								} else if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
									Log.v(TAG, "接收相隔时间>>1111>" + (System.currentTimeMillis() - sentMsgTime));
									if (System.currentTimeMillis() - sentMsgTime < 2 * 60 * 1000) {
										//删除接收校正短信间隔为2min
										count = getContentResolver().delete(SMS_URI, "_id=?", new String[]{String.valueOf(id)});
									}
									PreferenceUtil.putLong(NetManagerService.this, imsi, PreferenceUtil.SIM_SUBID_SMS_KEY, 0);
									PreferenceUtil.putString(NetManagerService.this, imsi, PreferenceUtil.SMS_BODY_KEY, null);
								} else if (type == Telephony.Sms.MESSAGE_TYPE_FAILED) {
									//发送短信失败
									Message msg = handler.obtainMessage(SMS_NOTIFI);
									msg.arg1 = subId;
									msg.arg2 = -1;
									msg.sendToTarget();
									count = getContentResolver().delete(SMS_URI, "_id=?", new String[]{String.valueOf(id)});
									PreferenceUtil.putLong(NetManagerService.this, imsi, PreferenceUtil.SIM_SUBID_SMS_KEY, 0);
									PreferenceUtil.putString(NetManagerService.this, imsi, PreferenceUtil.SMS_BODY_KEY, null);
								}
								Log.v(TAG, "count>>>>>" + count);
							}
						}
						Log.e(TAG, "监听短信内容>>" + body + ">>>>subId>>>" + subId + ">>>>>number>>" + number);
					}
					//释放默认短信应用权限
					appOpsManager.setMode(15, android.os.Process.myUid(), getPackageName(), 2);
				}
			} catch(Exception e) {
				e.printStackTrace();
				PreferenceUtil.putLong(NetManagerService.this, imsi, PreferenceUtil.SIM_SUBID_SMS_KEY, 0);
				PreferenceUtil.putString(NetManagerService.this, imsi, PreferenceUtil.SMS_BODY_KEY, null);
			} finally {
				if (cursor != null) cursor.close();
			}
		}
	};

	/**
	 * 监听状态改变
	 */
	private void registerStatusObserver() {
		UpdateObserver updateObserver = DataManagerApplication.getInstance().getUpdateObserver();
		updateObserver.addObserver(statusObserver);
	}
	/**
	 * 监听状态改变
	 */
	private void unRegisterStatusObserver() {
		UpdateObserver updateObserver = DataManagerApplication.getInstance().getUpdateObserver();
		updateObserver.removeObserver(statusObserver);
	}
	private StatusObserver statusObserver = new StatusObserver() {
		@Override
		public void update() {
			super.update();
			LogUtil.e(TAG, "开始更新>>>>>>");
			handler.removeCallbacks(statsRn);
			mInfoEntity = new NetInfoEntity();
			//重新开始
			mUpdateMain = false;
			handler.post(statsRn);

		}
	};
	private void initStats() {
		int simSlotIndex = ToolsUtil.getCurrentNetSimSubInfo(NetManagerService.this);
		if (simSlotIndex == -1) {
		    return;
		}
		if (null == mPolicyManager) {
			mPolicyManager = NetworkPolicyManager.from(this);
		}
		if (null == mStatsService) {
			mStatsService = INetworkStatsService.Stub.asInterface(ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
		}
		if (null == mTelephonyManager) {
			mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		}
		// Match mobile traffic for this subscriber, but normalize it to
		// catch any other merged subscribers.
		mActiveDataImsi = ToolsUtil.getActiveSimImsi(NetManagerService.this);
		mTemplate = buildTemplateMobileAll(mActiveDataImsi);
		mTemplate = NetworkTemplate.normalize(mTemplate, mTelephonyManager.getMergedSubscriberIds());
	}
	
	private Runnable statsRn = new Runnable() {
		
		@Override
		public void run() {
			initStats();
			String netWorkType = ToolsUtil.getNetWorkType(NetManagerService.this);
			if (null != mTemplate) {
				handler.removeCallbacks(statsRn);
				mActiveDataImsi = ToolsUtil.getActiveSimImsi(NetManagerService.this);
				if (TextUtils.isEmpty(mActiveDataImsi)) {
					handler.postDelayed(statsRn, UNIT_MINUTE);
					return;
				}
				mFreeStaus = PreferenceUtil.getBoolean(NetManagerService.this, mActiveDataImsi, PreferenceUtil.FREE_DATA_STATE_KEY, false);
				long freeTotal = PreferenceUtil.getLong(NetManagerService.this,  mActiveDataImsi, PreferenceUtil.FREE_DATA_TOTAL_KEY, 0);

				try {
					if (mUpdateMain) {
						//非移动网络下不统计
						if (!ToolsUtil.NET_TYPE_MOBILE.equals(netWorkType)) {
							handler.postDelayed(statsRn, UNIT_MINUTE);
							return;
						}
					}
					if (mFreeStaus && freeTotal > 0) {
						//初始化统计闲时流量
						mTaskFreeMonth = new StatsFreeDataTask(DATA_FREE_STATUS_MONTH, mActiveDataImsi);
						mTaskFreeMonth.execute();
						Thread.sleep(80);
						mTaskFreeDay = new StatsFreeDataTask(DATA_FREE_STATUS_DAY, mActiveDataImsi);
						mTaskFreeDay.execute();
						Thread.sleep(80);

					}
					if (mUpdateMain) {
						mDataForMinute = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes() - mDataForMinute;
						Message message = handler.obtainMessage();
						mInfoEntity.setmUsedForMinute(mDataForMinute);
						message.obj = mActiveDataImsi;
						message.what = DATA_NOTIFI;
						handler.sendMessage(message);
						LogUtil.v(TAG, "每分钟数据" + "entry111111111>>>" + Formatter.formatFileSize(NetManagerService.this, mDataForMinute));
					}
					mTaskDay = new StatsDataTask(DATA_STATUS_DAY, mActiveDataImsi);
					mTaskDay.execute();
					Thread.sleep(80);
					mTaskMonth = new StatsDataTask(DATA_STATUS_MONTH, mActiveDataImsi);
				    mTaskMonth.execute();
					Thread.sleep(80);
					mTaskBg = new StatsDataTask(DATA_STATUS_BG, mActiveDataImsi);
					mTaskBg.execute();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (mUpdateMain) {
					mDataForMinute = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes();
					handler.postDelayed(statsRn, UNIT_MINUTE);
				} else {
					//初始化更新主界面
					mDataForMinute = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes();
					handler.post(statsRn);
				}
				mUpdateMain = true;
		      }
		}
	};
	
	private class StatsDataTask extends AsyncTask<NetworkStats, Void, NetworkStats> {

		private Integer statsType;
		private String activeDataImsi;
		public StatsDataTask(int statsType, String imsi) {
			super();
			this.statsType = statsType;
			this.activeDataImsi = imsi;
		}

		@Override
		protected NetworkStats doInBackground(NetworkStats... params) {
		    int simSlotIndex = ToolsUtil.getCurrentNetSimSubInfo(NetManagerService.this);
			if (simSlotIndex == -1) {
			  return null;
			}
			long start = 0;
			long end = 0;
			switch (statsType) {
			case DATA_STATUS_MONTH:
				int closeDay = PreferenceUtil.getInt(NetManagerService.this,  activeDataImsi, PreferenceUtil.CLOSEDAY_KEY, 1);
				start = StringUtil.getDayByMonth(closeDay);
				//流量校正完成时间
				long commonUsedData = PreferenceUtil.getLong(NetManagerService.this, activeDataImsi, PreferenceUtil.USED_DATAPLAN_COMMON_KEY, 0);
				long correctedTime = PreferenceUtil.getLong(NetManagerService.this, activeDataImsi, PreferenceUtil.CORRECT_OK_TIME_KEY, 0);
				if (correctedTime > 0 && commonUsedData > 0) {
					//从上次校正过的时间开始统计
					start = correctedTime;
				}
				end = StringUtil.getDayByNextMonth(closeDay);
				break;
			case DATA_STATUS_DAY:
				start = StringUtil.getStartTime(0, 0, 0);
				end = StringUtil.getEndTime(23, 59, 59);
				if (mFreeStaus) {
					//开始时间
					String freeStartTime = PreferenceUtil.getString(NetManagerService.this, activeDataImsi, PreferenceUtil.FREE_DATA_START_TIME_KEY, NetManagerService.this.getString(R.string.un_set));
					//结束时间
					String freeEndTime = PreferenceUtil.getString(NetManagerService.this, activeDataImsi, PreferenceUtil.FREE_DATA_END_TIME_KEY, NetManagerService.this.getString(R.string.un_set));
					if (!TextUtils.equals(freeStartTime, NetManagerService.this.getString(R.string.un_set)) && !TextUtils.equals(freeEndTime, NetManagerService.this.getString(R.string.un_set))) {
						int startHour = Integer.parseInt(freeStartTime.split(":")[0]);
						int startMinute = Integer.parseInt(freeStartTime.split(":")[1]);
						int endHour = Integer.parseInt(freeEndTime.split(":")[0]);
						int endMinute = Integer.parseInt(freeEndTime.split(":")[1]);
						//闲时时间
						int startTime = Integer.parseInt(startHour + "" + (startMinute == 0 ? "00" : startMinute));
						int endTime = Integer.parseInt(endHour + "" + (endMinute == 0 ? "00" : endMinute));
						if (startTime >= endTime) {
							start = StringUtil.getStartTime(endHour, endMinute, 0);
							end = StringUtil.getEndTime(startHour, startMinute, 0);
						}
					}
				}
				break;
			case DATA_STATUS_BG:
				boolean dataInfoState = PreferenceUtil.getBoolean(NetManagerService.this, activeDataImsi, PreferenceUtil.MINUTE_DATA_USED_DIALOG_KEY, false);
				if (dataInfoState) {
					return null;
				}
				end = System.currentTimeMillis();
				//每分钟统计一次
				start = end - UNIT_MINUTE;
				break;
			default:
				break;
			}
			NetworkStats networkStats = null;
			try {
				mStatsService.forceUpdate();
				mStatsSession = mStatsService.openSession();
				networkStats = mStatsSession.getSummaryForAllUid(mTemplate, start, end, false);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
//			finally {
//				TrafficStats.closeQuietly(mStatsSession);
//			}
			return networkStats;
		}

		@Override
		protected void onPostExecute(NetworkStats result) {
			super.onPostExecute(result);
			NetworkStats.Entry entry = null;
			NetworkStats networkStats = result;
			long usedDataBg = 0;
			long usedDataTotal = 0;
			synchronized (statsType) {
				int size = networkStats != null ? networkStats.size() : 0;
				if (null != networkStats) {
					for (int i = 0; i < size; i++) {
						entry = networkStats.getValues(i, entry);
						//去除定向流量
						boolean isExit = false;
						if (DATA_STATUS_DAY == statsType || DATA_STATUS_MONTH == statsType) {
							ArrayList<String> orientAppUids = getOrientApps(activeDataImsi);
							//统计前台和后台数据
							if (null != orientAppUids && orientAppUids.size() > 0) {
								//过滤定向应用流量
								for (int j = 0; j < orientAppUids.size(); j++) {
									if (entry.uid == Integer.parseInt(orientAppUids.get(j))) {
										isExit = true;
										break;
									}
								}
							}
						}
						if (!isExit) {
							usedDataTotal = usedDataTotal + entry.rxBytes + entry.txBytes;
						}
						if (statsType == DATA_STATUS_BG) {
							if (SET_DEFAULT == entry.set) {
								//统计后台数据
								long usedData = entry.rxBytes + entry.txBytes;
								usedDataBg = usedData + usedDataBg;
							}
						}
					}
					Message msg = handler.obtainMessage();
					switch(statsType) {
					case DATA_STATUS_MONTH:
						long commonTotalData = PreferenceUtil.getLong(NetManagerService.this, activeDataImsi, PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
						long commonRemainData = PreferenceUtil.getLong(NetManagerService.this, activeDataImsi, PreferenceUtil.REMAIN_DATAPLAN_COMMON_KEY, 0);
						long commonUsedData = PreferenceUtil.getLong(NetManagerService.this, activeDataImsi, PreferenceUtil.USED_DATAPLAN_COMMON_KEY, 0);
						boolean freeStatus = PreferenceUtil.getBoolean(NetManagerService.this, activeDataImsi, PreferenceUtil.FREE_DATA_STATE_KEY, false);
						long freeTotal = PreferenceUtil.getLong(NetManagerService.this,  activeDataImsi, PreferenceUtil.FREE_DATA_TOTAL_KEY, 0);
						long usedData = 0;//实际已用
						long remainData = 0;//实际剩余
						if (commonTotalData > 0) {
							usedData = commonUsedData + usedDataTotal / 1024;
							if (freeStatus && freeTotal > 0) {
								//过滤闲时流量 必须设置闲时流量套餐总量
								usedData = usedData - mInfoEntity.mFreeUsedForMonth / 1024;
								if (usedData < 0) {
									usedData = 0;
								}
							}
							long dayUsed = PreferenceUtil.getLong(NetManagerService.this, activeDataImsi, PreferenceUtil.DAY_USED_STATS_KEY, 0);
							if (dayUsed > usedData) {
								usedData = dayUsed;
							}
							//
							boolean cmccFreeTimeTag = PreferenceUtil.getBoolean(NetManagerService.this, activeDataImsi, PreferenceUtil.CMCC_FREE_TMIME_KEY, false);
							if (cmccFreeTimeTag) {
								//从上次校正过的时间开始统计
								remainData = commonRemainData - usedDataTotal / 1024;
								remainData = remainData > 0 ? remainData : 0;
							} else {
								remainData = commonTotalData - usedData;
							}
							PreferenceUtil.putLong(NetManagerService.this, activeDataImsi, PreferenceUtil.REMAIN_DATAPLAN_AFTER_COMMON_KEY, remainData);
							PreferenceUtil.putLong(NetManagerService.this, activeDataImsi, PreferenceUtil.USED_DATAPLAN_AFTER_COMMON_KEY, usedData);
						} else {
							//没有设置套餐总量
							//统计sim卡本机已用流量
							usedData = usedDataTotal / 1024;
							PreferenceUtil.putLong(NetManagerService.this, activeDataImsi, PreferenceUtil.USED_DATAPLAN_AFTER_COMMON_KEY, usedData);

						}
						mInfoEntity.setmUsedForMonth(usedData * 1024 > 0 ? usedData * 1024 : 0);
						msg.obj = activeDataImsi;
						msg.what = DATA_NOTIFI;
						handler.sendMessage(msg);
						LogUtil.v(TAG, "校正过后所用数据" + "entry>>>" + Formatter.formatFileSize(NetManagerService.this, usedDataTotal));
						break;
					case DATA_STATUS_DAY:
						long monthUsed = PreferenceUtil.getLong(NetManagerService.this, activeDataImsi, PreferenceUtil.USED_DATAPLAN_AFTER_COMMON_KEY, 0);
						long dataUsed = usedDataTotal - mInfoEntity.mFreeUsedForDay;
						if (monthUsed <= dataUsed / 1024) {
							dataUsed = monthUsed * 1024;
						}
						mInfoEntity.setmUsedForDay(dataUsed > 0 ? dataUsed : 0);
						msg.obj = activeDataImsi;
						msg.what = DATA_NOTIFI;
						LogUtil.v(TAG, "当天所用数据" + "entry>>>" + Formatter.formatFileSize(NetManagerService.this, dataUsed));
						PreferenceUtil.putLong(NetManagerService.this, activeDataImsi, PreferenceUtil.DAY_USED_STATS_KEY, dataUsed / 1024);
						handler.sendMessage(msg);
						break;
					case DATA_STATUS_BG:
						msg.obj = usedDataBg;
						msg.what = WARN_INFO_DIALOG;
						handler.sendMessage(msg);
						LogUtil.v(TAG, "统计后台数据" + "entry>>>" + Formatter.formatFileSize(NetManagerService.this, usedDataBg));
						break;
					}
				}
			}
		}
		
	}
	
	/**
	 * 统计闲时流量
	 * @author zhaolaichao
	 *
	 */
	private class StatsFreeDataTask extends AsyncTask<NetworkStats, Void, NetworkStats> {

		private int statsTag;
		private String activeImsi;

		public StatsFreeDataTask(int statsTag, String activeImsi) {
			this.statsTag = statsTag;
			this.activeImsi = activeImsi;
		}

		@Override
		protected NetworkStats doInBackground(NetworkStats... params) {
			NetworkStats statsFreeData = null;
			long freeTotalData = PreferenceUtil.getLong(NetManagerService.this, activeImsi, PreferenceUtil.FREE_DATA_TOTAL_KEY, 0);
			if (freeTotalData <= 0) {
				return null;
			}
			if (mStatsSession == null || mTemplate == null) {
				return null;
			}
			mFreeUsedForMonth = 0;
			switch (statsTag) {
				case DATA_FREE_STATUS_MONTH:
					String activeDataImsi = ToolsUtil.getActiveSimImsi(NetManagerService.this);
					int closeDay = PreferenceUtil.getInt(NetManagerService.this, activeDataImsi, PreferenceUtil.CLOSEDAY_KEY, 1);
					//从月结日到当前一共多少天
					int count = StringUtil.getDaysByCloseDay(closeDay);
					long correctedTime = PreferenceUtil.getLong(NetManagerService.this, activeDataImsi, PreferenceUtil.CORRECT_OK_TIME_KEY, 0);
					long usedFreeData = PreferenceUtil.getLong(NetManagerService.this, activeDataImsi, PreferenceUtil.USED_FREE_DATA_TOTAL_KEY, 0);
					int correctDay = 0;
					if (correctedTime > 0) {
						DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
						String date = format.format(correctedTime);
						closeDay = correctDay = Integer.parseInt(date.substring(date.lastIndexOf("-") + 1));
						count = StringUtil.getDaysByCloseDay(correctDay);
					}
					LogUtil.v(TAG, "从月结日到当前一共多少天>>" + count);
					int total = 0;
					try {
						//开始时间
						String freeStartTime = PreferenceUtil.getString(NetManagerService.this, activeDataImsi, PreferenceUtil.FREE_DATA_START_TIME_KEY, getString(R.string.un_set));
						//结束时间
						String freeEndTime = PreferenceUtil.getString(NetManagerService.this, activeDataImsi, PreferenceUtil.FREE_DATA_END_TIME_KEY, getString(R.string.un_set));
						if (!TextUtils.equals(freeStartTime, NetManagerService.this.getString(R.string.un_set)) && !TextUtils.equals(freeEndTime, NetManagerService.this.getString(R.string.un_set))) {

							int startHour = Integer.parseInt(freeStartTime.split(":")[0]);
							int startMinute = Integer.parseInt(freeStartTime.split(":")[1]);
							int endHour = Integer.parseInt(freeEndTime.split(":")[0]);
							int endMinute = Integer.parseInt(freeEndTime.split(":")[1]);
							if (total <= count) {
								//闲时时间
								int startTime = Integer.parseInt(startHour + "" + (startMinute == 0 ? "00" : startMinute));
								int endTime = Integer.parseInt(endHour + "" + (endMinute == 0 ? "00" : endMinute));
								if (startTime > endTime) {
									statsFree(true, closeDay, count, total, startHour, startMinute, endHour, endMinute);
								} else {
									statsFree(false, closeDay, count, total, startHour, startMinute, endHour, endMinute);
								}
								LogUtil.v(TAG, "开时统计每天>>" + total);
							}
							mInfoEntity.setmFreeUsedForMonth(mFreeUsedForMonth);
							//mInfoEntity.setmFreeUsedForMonth(usedFreeData * 1024 + mFreeUsedForMonth);
							freeTotalData = PreferenceUtil.getLong(NetManagerService.this, activeDataImsi, PreferenceUtil.FREE_DATA_TOTAL_KEY, 0);
							PreferenceUtil.putLong(NetManagerService.this, activeDataImsi, PreferenceUtil.USED_FREE_DATA_TOTAL_KEY, mInfoEntity.mFreeUsedForMonth / 1024);
							PreferenceUtil.putLong(NetManagerService.this, activeDataImsi, PreferenceUtil.REMAIN_FREE_DATA_TOTAL_KEY, freeTotalData - mInfoEntity.mFreeUsedForMonth / 1024);
							LogUtil.v(TAG, "当月所用闲时流量数据" + "entry.rxBytes>>>" + Formatter.formatFileSize(NetManagerService.this, mInfoEntity.mFreeUsedForMonth));
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				case DATA_FREE_STATUS_DAY:
					if (mFreeStaus) {
						//开始时间
						String freeStartTime = PreferenceUtil.getString(NetManagerService.this, activeImsi, PreferenceUtil.FREE_DATA_START_TIME_KEY, NetManagerService.this.getString(R.string.un_set));
						//结束时间
						String freeEndTime = PreferenceUtil.getString(NetManagerService.this, activeImsi, PreferenceUtil.FREE_DATA_END_TIME_KEY, NetManagerService.this.getString(R.string.un_set));
						if (!TextUtils.equals(freeStartTime, NetManagerService.this.getString(R.string.un_set)) && !TextUtils.equals(freeEndTime, NetManagerService.this.getString(R.string.un_set))) {
							int startHour = Integer.parseInt(freeStartTime.split(":")[0]);
							int startMinute = Integer.parseInt(freeStartTime.split(":")[1]);
							int endHour = Integer.parseInt(freeEndTime.split(":")[0]);
							int endMinute = Integer.parseInt(freeEndTime.split(":")[1]);
							//闲时时间
							int startTime = Integer.parseInt(startHour + "" + (startMinute == 0 ? "00" : startMinute));
							int endTime = Integer.parseInt(endHour + "" + (endMinute == 0 ? "00" : endMinute));
							long start = 0;
							long end = 0;
							if (startTime < endTime) {
								start = StringUtil.getStartTime(startHour, startMinute, 0);
								end = StringUtil.getEndTime(endHour, endMinute, 0);
								try {
									statsFreeData = mStatsSession.getSummaryForAllUid(mTemplate, start, end, false);
								} catch (RemoteException e) {
									e.printStackTrace();
								}
							}
						}

					}
					break;
			}
			return statsFreeData;
		}

		@Override
		protected void onPostExecute(NetworkStats result) {
			if (null == result) {
				return;
			}
			super.onPostExecute(result);
			switch (statsTag) {
				case DATA_FREE_STATUS_DAY:
					NetworkStats.Entry entry = null;
					NetworkStats networkStats = result;
					long usedFreeData = 0;
					int size = networkStats != null ? networkStats.size() : 0;
					if (null != networkStats) {
						for (int i = 0; i < size; i++) {
							entry = networkStats.getValues(i, entry);
							//去除定向流量
							boolean isExit = false;
							ArrayList<String> orientAppUids = getOrientApps(activeImsi);
							//统计前台和后台数据
							if (null != orientAppUids && orientAppUids.size() > 0) {
								//过滤定向应用流量
								for (int j = 0; j < orientAppUids.size(); j++) {
									if (entry.uid == Integer.parseInt(orientAppUids.get(j))) {
										isExit = true;
										break;
									}
								}
							}
							if (!isExit) {
								usedFreeData = usedFreeData + entry.rxBytes + entry.txBytes;
							}
						}
						mInfoEntity.setmFreeUsedForDay(usedFreeData);
						LogUtil.e(TAG, "每天闲时流量>>>" + Formatter.formatFileSize(NetManagerService.this, usedFreeData));
					}
					break;
			}
		}
		
	}
	
	/**
	 * 统计闲时流量
	 * @param overDay    闲时是否跨天
	 * @param closeDay
	 * @param count
	 * @param total
	 * @param startHour
	 * @param startMinute
	 * @param endHour
	 * @param endMinute
	 * @param freeTotalForMonth
	 */
	private void statsFree(boolean overDay, int closeDay, int count, int total, int startHour, int startMinute, int endHour, int endMinute) {
		try {
			if (mStatsSession == null || mTemplate == null) {
				return;
			}
			NetworkStats.Entry entry = null;
			//统计每天的闲时流量
			if (overDay) {
				//闲时时间跨天
				long startMorning = StringUtil.getDayByCloseDay(closeDay + total, 0, 0, 0);
				long endMorning  = StringUtil.getDayByCloseDay(closeDay + total, endHour, endMinute, 999);
				NetworkStats statsMorning = mStatsSession.getSummaryForAllUid(mTemplate, startMorning, endMorning, false);
				int size = statsMorning != null ? statsMorning.size() : 0;
				if (null != statsMorning) {
					for (int i = 0; i < size; i++) {
						entry = statsMorning.getValues(i, entry);
						mFreeUsedForMonth = mFreeUsedForMonth + entry.rxBytes + entry.txBytes;
					}
				}
				long startNight = StringUtil.getDayByCloseDay(closeDay + total, startHour, startMinute, 0);
				long endNight  = StringUtil.getDayByCloseDay(closeDay + total, 23, 59, 999);
				if (startMorning < startNight) {
					NetworkStats statsNight = mStatsSession.getSummaryForAllUid(mTemplate, startNight, endNight, false);
					size = statsNight != null ? statsNight.size() : 0;
					if (null != statsNight) {
						for (int i = 0; i < size; i++) {
							entry = statsNight.getValues(i, entry);
							mFreeUsedForMonth = mFreeUsedForMonth + entry.rxBytes + entry.txBytes;
						}
					}
				}
			} else {
				//闲时时间不跨天
				long start = StringUtil.getDayByCloseDay(closeDay + total, startHour, startMinute, 0);
				long end  = StringUtil.getDayByCloseDay(closeDay + total, endHour, endMinute, 999);
				NetworkStats statsMorning = mStatsSession.getSummaryForAllUid(mTemplate, start, end, false);
				int size = statsMorning != null ? statsMorning.size() : 0;
				if (null != statsMorning) {
					for (int i = 0; i < size; i++) {
						entry = statsMorning.getValues(i, entry);
						mFreeUsedForMonth = mFreeUsedForMonth + entry.rxBytes + entry.txBytes;
					}
				}
			}
			total ++;
			if (total <= count) {
				statsFree(overDay, closeDay, count, total, startHour, startMinute, endHour, endMinute);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	/**
	 * 获得定向应用
	 * @param imsi
	 * @return
	 */
	private ArrayList<String> getOrientApps(String imsi) {
		  ArrayList<String> addUidList = null;
		  String addUids = PreferenceUtil.getString(this, imsi, PreferenceUtil.ORIENT_APP_ADDED_KEY, "");
		  if (addUids.contains(",")) {
			  String[] addUidsArray = addUids.split(",");
			  if (null != addUidsArray &&  addUidsArray.length > 0) {
				  addUidList = new ArrayList<String>();
				  Collections.addAll(addUidList, addUidsArray);
			  }
		  }
		  return addUidList;
	}
	
	/**
	 * 每分钟统计一次
	 * 后台数据流量提示:单位时间内（1分钟）后台跑了至少10M流量则满足提示条件
	 */
	private void showInfoMinute(Context context, long usedDataBg) {
		if (mInfoEntity.mUsedForMinute < usedDataBg) {
			//流量数据没有及时更新
			return;
		}
		String activeSimImsi = ToolsUtil.getActiveSimImsi(context);
        if (usedDataBg >= MAX_MINUTE_BGDATA) {
        	handler.removeCallbacks(statsRn);
        	warnInfoDialog(context, String.format(context.getString(R.string.data_bg_use_info), Formatter.formatFileSize(context, MAX_MINUTE_BGDATA)));
        }
        PreferenceUtil.putLong(context, activeSimImsi, PreferenceUtil.MINUTE_DATA_USED_KEY, usedDataBg);
	}
	
	/**
	 * 每分钟统计一次
	 * @param msg
	 */
    private void warnInfoDialog(Context context, String msg) {
    	final String activeSimImsi = ToolsUtil.getActiveSimImsi(context);
    	boolean dataInfoState = PreferenceUtil.getBoolean(context, activeSimImsi, PreferenceUtil.MINUTE_DATA_USED_DIALOG_KEY, false);
    	if (dataInfoState) {
    		return;
    	}
    	if (mAlertDialog != null && mAlertDialog.isShowing()){
    		return;
    	}
        context = ((Context)NetManagerService.this);
    	context.setTheme(com.hb.R.style.Theme_Hb_Material_Light);
    	mBuilder = new hb.app.dialog.AlertDialog.Builder(context);
    	mBuilder.setCancelable(false);
    	mBuilder.setTitle(context.getString(R.string.data_bg_warning_info));
    	NetManageDialogView dialogView = new NetManageDialogView(context);
    	dialogView.setMessage(msg);
    	dialogView.setOnCheckListener(new NetManageDialogView.ICheckListener() {
			
			@Override
			public void setOnCheckListener(CompoundButton buttonView, boolean isChecked) {
				LogUtil.v(TAG, "不再提示>>" + isChecked);
				if (!isChecked) {
					handler.postDelayed(statsRn, UNIT_MINUTE);
				}
				PreferenceUtil.putBoolean(NetManagerService.this, "", PreferenceUtil.WARN_DATA_USED_KEY, isChecked);
			}
		});
    	mBuilder.setView(dialogView); 
    	mBuilder.setPositiveButton(context.getString(R.string.konw), new hb.app.dialog.AlertDialog.OnClickListener() {
 	          @Override
 	          public void onClick(DialogInterface dialog, int which) {
 	        	 handler.postDelayed(statsRn, UNIT_MINUTE);
				  mAlertDialog.dismiss();
 	          }
 	      })
 	      .setNegativeButton(context.getString(R.string.see), new hb.app.dialog.AlertDialog.OnClickListener() {
 	           @Override
 	           public void onClick(DialogInterface dialog, int which) {
 	        	   int currentNetSimIndex = ToolsUtil.getCurrentNetSimSubInfo(NetManagerService.this);
				   try {
					   if (currentNetSimIndex != -1) {
						   //当前应用是否运行
						   String pakName = NetManagerService.this.getPackageName();
						   if (ToolsUtil.isAppRunning(NetManagerService.this, pakName)) {
							   //进入后台消耗流量详情界面
							   Intent mainIntent = new Intent(NetManagerService.this, MainActivity.class);
							   mainIntent.setAction("com.hb.netmanage.main.action");
							   mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							   Intent bgIntent = new Intent(NetManagerService.this, DataRangeActivity.class);
							   bgIntent.putExtra("BG_STATE", true);
							   //bgIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
							   Intent[] intents = {mainIntent, bgIntent};
							   NetManagerService.this.startActivities(intents);
							   LogUtil.e(TAG, "pakName>>" + pakName);
						   } else {
							   Intent launchIntent = NetManagerService.this.getPackageManager().
									   getLaunchIntentForPackage(pakName);
							   launchIntent.setFlags(
									   Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
							   launchIntent.putExtra("BG_STATE", true);
							   NetManagerService.this.startActivity(launchIntent);
							   LogUtil.e(TAG, "pakName>2222222>" + pakName);
						   }
					   }
					   handler.postDelayed(statsRn, UNIT_MINUTE);
				   } catch (Exception e) {
                       e.printStackTrace();
				   }
				   mAlertDialog.dismiss();
 	            }
 	      });
    	if (null == mAlertDialog) {
    		mAlertDialog = mBuilder.create();
    		mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
    	}
		mAlertDialog.setCanceledOnTouchOutside(false);
    	if (null != mAlertDialog && !mAlertDialog.isShowing()) {
    		mAlertDialog.show();
    	}
     }
    
    /**
	 * 通知栏中的提示
	 * @param networkInfoEntity
	 */
	private void showNotifyMsg(Context context, NetInfoEntity networkInfoEntity) {
		 int simSlotIndex = ToolsUtil.getCurrentNetSimSubInfo(context);
		 String activeDataImsi = ToolsUtil.getActiveSimImsi(context);
         boolean warnState = PreferenceUtil.getBoolean(context, activeDataImsi, PreferenceUtil.PASS_WARNING_STATE_KEY, true);
         //全部流量:通用 KB为单位
		 long total = PreferenceUtil.getLong(context, activeDataImsi, PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
		 long usedTotal = PreferenceUtil.getLong(context, activeDataImsi, PreferenceUtil.USED_DATAPLAN_AFTER_COMMON_KEY, 0);
          String warnValue = PreferenceUtil.getString(context, activeDataImsi, PreferenceUtil.PASS_WARNING_VALUE_KEY, SimDataSetActivity.DEFAULT_RATE + "%");
          String rateStr = warnValue.substring(0, warnValue.indexOf("%"));
          long usedForMonth =  (usedTotal * 1024  - networkInfoEntity.mUsedForMonth) > 0 ?  usedTotal * 1024 : networkInfoEntity.mUsedForMonth;
          if (warnState && total > 0 && usedForMonth * 100 / (total * 1024) >= Integer.parseInt(rateStr)) {
        	  //月结日
	          int closeDay = PreferenceUtil.getInt(context, activeDataImsi, PreferenceUtil.CLOSEDAY_KEY, 1);
        	  long timeByNextMonth = StringUtil.getDayByNextMonth(closeDay);
        	  long timeNow = System.currentTimeMillis();
        	  if (timeNow >= timeByNextMonth) {
        		  //超过当前月结日周期清除提示框标志
	        	  PreferenceUtil.putBoolean(context, activeDataImsi, PreferenceUtil.NOTIFY_WARN_MONTH_KEY, false);
        	  } else {
        		  //当在此月结日周期内所用流量超过用户设置的最大阀值时则提示,每个月最多提示一次
        		  //跳转到对应的流量排行界面
        		  boolean notify = PreferenceUtil.getBoolean(context, activeDataImsi, PreferenceUtil.NOTIFY_WARN_MONTH_KEY, false);
        		  if (notify) {
        			  return;
        		  }
        		  Intent intent = new Intent(context, DataRangeActivity.class);
        		  NotificationUtil.showNotification(context, context.getString(R.string.app_name), context.getString(R.string.data_pass_warning_info), String.format(context.getString(R.string.data_warning_notifyinfo), StringUtil.formatDataFlowSize(context, usedTotal)), intent);
        		  PreferenceUtil.putBoolean(context, activeDataImsi, PreferenceUtil.NOTIFY_WARN_MONTH_KEY, true);
        	  }
          }
          
          //每日提醒:当日流量超过5%时提示
          if (warnState && total > 0 && networkInfoEntity.mUsedForDay / (total * 1024) >= LIMIT_RATE_DAY) {
        	  //每天最多一次
    		  //跳转到对应的流量管理主界面
    		  Intent intent = new Intent(context, MainActivity.class);
    		  String info = String.format(context.getString(R.string.data_day_notifyinfo), StringUtil.formatDataFlowSize(context, networkInfoEntity.mUsedForDay / 1024) , StringUtil.formatDataFlowSize(context, total));
    		  NotificationUtil.showNotification(context, context.getString(R.string.data_pass_warning_info), context.getString(R.string.data_pass_warning_info), info, intent);
    		  PreferenceUtil.putBoolean(context, activeDataImsi, PreferenceUtil.NOTIFY_WARN_DAY_KEY, true);
          }
	}
	
	/**
	 * 系统中断式对话框提示
	 * @param context
	 * @param networkInfoEntity
	 */
	private void showDataDialogInfo(Context context, NetInfoEntity networkInfoEntity) {
		String activeSimImsi = ToolsUtil.getActiveSimImsi(context);
		String contentInfo = null;
		long commDataTotal = PreferenceUtil.getLong(context, activeSimImsi, PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
		if (commDataTotal <= 0) {
		   return;
		}
		long commUsedTotal = PreferenceUtil.getLong(context, activeSimImsi, PreferenceUtil.USED_DATAPLAN_AFTER_COMMON_KEY, 0);
		long usedMonth = (commUsedTotal * 1024 - networkInfoEntity.mUsedForMonth) > 0 ? commUsedTotal * 1024 : networkInfoEntity.mUsedForMonth;
		if (usedMonth / (commDataTotal * 1024) >= LIMIT_RATE_MONTH) {
		      //月结日
		      int closeDay = PreferenceUtil.getInt(context, activeSimImsi, PreferenceUtil.CLOSEDAY_KEY, 1);
	     	  long timeByNextMonth = StringUtil.getDayByNextMonth(closeDay);
	     	  long timeNow = System.currentTimeMillis();
	     	  if (timeNow >= timeByNextMonth) {
	     		  //超过当前月结日周期清除提示框标志
		     	  PreferenceUtil.putBoolean(context, activeSimImsi, PreferenceUtil.NOTIFY_WARN_MONTH_99_KEY, false);
	     	  } else {
	     		   boolean isNotify = PreferenceUtil.getBoolean(context, activeSimImsi, PreferenceUtil.NOTIFY_WARN_MONTH_99_KEY, false);
	     		   if (isNotify) {
	     			  //提示频率：每月一次
	     			   return;
	     		   }
				   ToolsUtil.setMobileDataState(context, false);//关闭数据接连
		           if (!TextUtils.isEmpty(DataManagerApplication.mImsiArray[0]) && !TextUtils.isEmpty(DataManagerApplication.mImsiArray[1])) {
		  	          //双卡弹框提示
		  	          contentInfo = context.getString(R.string.data_warning_double_info);
		            } else {
		  	          //单卡弹框提示
		  	          contentInfo = context.getString(R.string.data_warning_single_info);
		            }
				  dialogShow(context, contentInfo);
				  PreferenceUtil.putBoolean(context, activeSimImsi, PreferenceUtil.NOTIFY_WARN_MONTH_99_KEY, true);
	     	  }
		}
	}
	
	protected void dialogShow(Context context, String message) {
		  context = ((Context)NetManagerService.this);
	      context.setTheme(com.hb.R.style.Theme_Hb_Material_Light);
	      Builder builder = new hb.app.dialog.AlertDialog.Builder(context);
	      builder.setMessage(message);
	      builder.setCancelable(false);
	      builder.setTitle(context.getString(R.string.data_pass_warning_info));
	      builder.setPositiveButton(context.getString(R.string.use_till), new OnClickListener() {
	          @Override
	          public void onClick(DialogInterface dialog, int which) {
	        	  ToolsUtil.setMobileDataState(NetManagerService.this, true);
				  mAlertDialog.dismiss();
	          }
	      });
	      builder.setNegativeButton(context.getString(R.string.data_close), new OnClickListener() {
	           @Override
	           public void onClick(DialogInterface dialog, int which) {
	        	   ToolsUtil.setMobileDataState(NetManagerService.this, false);
				   mAlertDialog.dismiss();
	            }
	      });
	      if (null == mAlertDialog) {
	    	  mAlertDialog = builder.create();
	      }
		 mAlertDialog.setCanceledOnTouchOutside(false);
	      if (null != mAlertDialog && !mAlertDialog.isShowing()) {
	    	  mAlertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
	    	  mAlertDialog.show();
		  }
	 }
	
	class UpdateIconReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context mContext, Intent intent) {
			if (UPDATE_DATAPLAN_ICON_ACTION.equals(intent.getAction())) {
				//用于更新图标
				ToolsUtil.updateIconReceiver();
			}
		}
	}
}
