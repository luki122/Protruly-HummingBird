package com.hb.netmanage;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

import com.hb.netmanage.entity.SmsStatus;
import com.hb.netmanage.service.NetManagerService;
import com.hb.netmanage.utils.LogUtil;
import com.hb.netmanage.utils.PreferenceUtil;
import com.hb.netmanage.utils.ToolsUtil;

import java.util.Arrays;
import com.hb.netmanage.R;
import android.app.hb.TMSManager;
import android.app.hb.CodeNameInfo;
import android.app.hb.ITrafficCorrectListener;

public class DataCorrect {
	private static final String TAG = "DataCorrect";
	public static final String UPDATE_DATAPLAN_ACTION = "com.hb.netmanage.update_dataplan.action";
	private static final String TMS_SERVICE = "tms_service";
	public static final int FIRST_SIM_INDEX = 0;
	public static final int SECOND_SIM_INDEX = 1;
	public static final int START_CORRECT = 20000;

	/**
	 * 短信校正异常码
	 */
	public static final int ERR_CORRECTION_NET = -1; // 网络连接异常
	public static final int ERR_CORRECTION_ERROR = -2; // 卡校正出错
	public static final int ERR_CORRECTION_FEEDBACK_UPLOAD_FAIL = -10002; // 回包异常
	public static final int ERR_CORRECTION_BAD_SMS     = -10003; // 运营商无效短信
	public static final int ERR_CORRECTION_PROFILE_UPLOAD_FAIL = -10004;  // 省、市、运营商上报失败
	public static final int ERR_CORRECTION_LOCAL_NO_TEMPLATE = -10005; // 本地无模板
	public static final int ERR_CORRECTION_PROFILE_ILLEGAL = -10006; //  省、市、运营商不合法
	public static final int ERR_CORRECTION_LOCAL_TEMPLATE_UNMATCH = -10007; // 本地模板不匹配
	/**
	 * 校正设置成功
	 */
	public static final int ERR_NONE = 0;
	/**
	 * 流量短信解析成功
	 */
	public static final int MSG_TRAFFICT_NOTIFY = 10001;
	/**
	 * 流量校正失败
	 */
	public static final int MSG_TRAFFICT_ERROR = 10002;
	/**
	 * 删除发送的短信
	 */
	public static final int SMS_SENT_DELETE_TAG = 10003;
	/**
	 * 发送的短信成功
	 */
	public static final int SMS_SENT_OK_TAG = 10004;
	/**
	 * 发送的短信失败
	 */
	public static final int SMS_SENT_FAIL_TAG = 10005;
	/**
	 * 运营商查询号码
	 */
	private String mQueryCode;
	/**
	 * 向运营商发送流量查询端口
	 */
	private String mQueryPort;
	/**
	 * 单例模式
	 */
	public static DataCorrect mDataCorrect = new DataCorrect();
	private Handler mHandler;
	private Context mContext;

	private TMSManager mTmsManager;

	/**
	 * 是否更新UI
	 */
	private boolean[] mIsUpdateUI = new boolean[2];
	/**
	 * 是否发起校正
	 */
	private boolean[] mIsCorrect = new boolean[2];
	
	private int mSimIndex;
	
    private TrafficCorrectListener mTrafficListener;
	private DataCorrect() {
		super();
	}

	Handler msgHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case START_CORRECT:
					Bundle bundle = msg.getData();
					if (mContext == null || bundle == null)return;
					boolean isUpdateUI = bundle.getBoolean("isUpdateUI");
					int currentIndex = bundle.getInt("currentIndex");
					correct(mContext, isUpdateUI, currentIndex);
					break;
			}

		}
	};
	public static synchronized DataCorrect getInstance() {
		return mDataCorrect;
	}

	public int getmSimIndex() {
		return mSimIndex;
	}

	public TMSManager getmTmsManager() {
		try {
			if (mTmsManager == null) {
				mTmsManager = (TMSManager)mContext.getSystemService(TMS_SERVICE);
			}
			LogUtil.e(TAG, "getmTmsManager>>>>>" + mTmsManager);
			return mTmsManager;
		} catch (Exception e) {
           e.printStackTrace();
		   LogUtil.e(TAG, "Exception>>>>>" + e.getMessage());
		}
		return mTmsManager;
	}

	/**
	 * 初始化校正
	 * 
	 * @param context
	 * @param handler
	 */


	public void initCorrect(Context context, Handler handler) {
		mContext = context;
		if (null != handler) {
			mHandler = handler;
		}
		getmTmsManager();
		LogUtil.e(TAG, "tmsManager>>>>>" + mTmsManager);
		initCorrectData();
	}
	
	private void initCorrectData() {
		if (null == mContext || getmTmsManager() == null)
			return;
		Intent intent = new Intent(mContext, NetManagerService.class);
		mContext.startService(intent);
		if (mTrafficListener == null) {
			mTrafficListener = new TrafficCorrectListener();
			getmTmsManager().trafficCorrectListener(mTrafficListener);
		}
	}

	public void destory(Context context) {
		mContext = context;

	}
	private class TrafficCorrectListener extends ITrafficCorrectListener.Stub{

		@Override
		public void onNeedSmsCorrection(int simIndex, String queryCode, String queryPort) {
			Log.e(TAG, "onNeedSmsCorrection--simIndex:[" + simIndex + "]--queryCode:[" + queryCode + "]queryPort:["
					+ queryPort + "]");
			if (simIndex == -1 || TextUtils.isEmpty(DataManagerApplication.mImsiArray[simIndex])) return;
			//需要发查询短信校正
			mQueryCode = queryCode;
			mQueryPort = queryPort;
			Log.e(TAG, "----mQueryCode-----" + mQueryCode + "---mQueryPort---" + mQueryPort);
			if (!mIsCorrect[simIndex]) return;
			// 发送查询流量短信
			String code = "";
			String operator = ToolsUtil.getSimOperator(mContext,DataManagerApplication.mImsiArray[simIndex]);
			if (TextUtils.equals(operator, mContext.getString(R.string.china_mobile))) {
				code = "10086";
			} else if (TextUtils.equals(operator, mContext.getString(R.string.china_unicom))) {
				code = "10010";
			} else if (TextUtils.equals(operator, mContext.getString(R.string.china_telecom))) {
				code = "10001";
			}
			Log.e(TAG, "----code----" + code + "---mQueryPort---" + mQueryPort);
			if (TextUtils.equals(code, mQueryPort)) {
				sendCorrectMsg(simIndex, mQueryPort, mQueryCode);
			}
		}

		@Override
		public void onTrafficInfoNotify(int simIndex, int trafficClass, int subClass, int kBytes) {
			if (simIndex == -1 || TextUtils.isEmpty(DataManagerApplication.mImsiArray[simIndex])) return;
			//保存套餐信息
			mIsCorrect[simIndex] = false;
			Log.e(TAG, "onTrafficNotify->>" + simIndex + ">subClass>>" + subClass);
			int result = saveDataSet(simIndex);
			if (result < 0) {
				return;
			}
			if (null != mHandler) {
				Message msg = mHandler.obtainMessage(MSG_TRAFFICT_NOTIFY, simIndex, 0);
				Bundle bundle = new Bundle();
				bundle.putBoolean("SHOW_TOAST", mIsUpdateUI[simIndex]);
				msg.setData(bundle);
				mHandler.sendMessageDelayed(msg, 800);
			}
		}

		@Override
		public void onError(int simIndex, int errorCode) {
			String strState = "状态信息：" +  "卡：[" + simIndex + "]校正出错:[" + errorCode + "]";
			if (simIndex == -1 || TextUtils.isEmpty(DataManagerApplication.mImsiArray[simIndex])) return;
			if (null != mHandler) {
				Message msg = mHandler.obtainMessage(MSG_TRAFFICT_ERROR , simIndex, 0);
				msg.obj = setSmsStatus(errorCode);
				Bundle bundle = new Bundle();
				bundle.putBoolean("SHOW_TOAST", mIsUpdateUI[simIndex]);
				bundle.putSerializable("RESULT_ERROR", setSmsStatus(errorCode));
				msg.setData(bundle);
				mHandler.sendMessage(msg);
			}
			mIsCorrect[simIndex] = false;
			Log.v(TAG, "onError--simIndex:" + strState );
		}

	}

	/**
	 * 返回校正异常结果
	 * @param errorCode
	 * @return
     */
	private SmsStatus setSmsStatus(int errorCode) {
		SmsStatus smsStatus = new SmsStatus();
		String status = mContext.getString(R.string.single_data_correct_error);
		smsStatus.setErrorCode(errorCode);
		switch (errorCode) {
			case ERR_CORRECTION_FEEDBACK_UPLOAD_FAIL:
				status = mContext.getString(R.string.err_correction_feedback_upload_fail);
				break;
			case ERR_CORRECTION_BAD_SMS:
				status = mContext.getString(R.string.err_correction_bad_sms);
				break;
			case ERR_CORRECTION_PROFILE_UPLOAD_FAIL:
				status = mContext.getString(R.string.err_correction_profile_upload_fail);
				break;
			case ERR_CORRECTION_LOCAL_NO_TEMPLATE:
				status = mContext.getString(R.string.err_correction_local_no_template);
				break;
			case ERR_CORRECTION_PROFILE_ILLEGAL:
				status = mContext.getString(R.string.err_correction_profile_illegal);
				break;
			case ERR_CORRECTION_LOCAL_TEMPLATE_UNMATCH:
				status = mContext.getString(R.string.err_correction_local_template_unmatch);
				break;
			case ERR_CORRECTION_NET:
				status = mContext.getString(R.string.err_correction_net);
				break;
			case ERR_CORRECTION_ERROR:
				status = mContext.getString(R.string.err_correction_sim);
				break;

		}
		smsStatus.setStatus(status);
		return smsStatus;
	}
	/**
	 * 开始流量校正
	 *
	 * @param currentIndex
	 *            当前选中的sim卡
	 */
	public synchronized void startCorrect(Context context, boolean isUpdateUI, int currentIndex) {
		mContext = context;
		if (mContext == null || getmTmsManager() == null) return;
		mIsUpdateUI[currentIndex] = isUpdateUI;
		if (mIsUpdateUI[currentIndex] && mHandler != null) {
			Message mmsMsg = mHandler.obtainMessage(SMS_SENT_OK_TAG , currentIndex, 0);
			mmsMsg.sendToTarget();
		}
		Message msg = msgHandler.obtainMessage();
		Bundle bundle = new Bundle();
		bundle.putBoolean("isUpdateUI", isUpdateUI);
		bundle.putInt("currentIndex", currentIndex);
		msg.setData(bundle);
		msg.what = START_CORRECT;
		msgHandler.sendMessageDelayed(msg, 12000);
	}

	private void correct(Context context, boolean isUpdateUI, int currentIndex) {
		mContext = context;
		if (mContext == null || getmTmsManager() == null) return;
		int result;
		mIsUpdateUI[currentIndex] = isUpdateUI;
		mIsCorrect[currentIndex] = true;
		if (currentIndex == FIRST_SIM_INDEX) {
			mSimIndex = FIRST_SIM_INDEX;
		} else if (currentIndex == SECOND_SIM_INDEX) {
			mSimIndex = SECOND_SIM_INDEX;
		}
		if (!ToolsUtil.isNetworkAvailable(mContext) && mIsUpdateUI[currentIndex]) {
			mIsCorrect[currentIndex] = false;
			if (mHandler == null) return;
			Message msg = mHandler.obtainMessage(MSG_TRAFFICT_ERROR , mSimIndex, 0);
			msg.obj = setSmsStatus(ERR_CORRECTION_ERROR);
			msg.sendToTarget();
			return;
		}
		//包含只有一张sim卡的情况
		int closeDay = PreferenceUtil.getInt(mContext, DataManagerApplication.mImsiArray[mSimIndex], PreferenceUtil.CLOSEDAY_KEY, 1);
		String provinceCode = PreferenceUtil.getString(mContext, DataManagerApplication.mImsiArray[mSimIndex], PreferenceUtil.PROVINCE_CODE_KEY, "");
		String cityCode = PreferenceUtil.getString(mContext, DataManagerApplication.mImsiArray[mSimIndex], PreferenceUtil.CITY_CODE_KEY, "");
		String carryCode = PreferenceUtil.getString(mContext, DataManagerApplication.mImsiArray[mSimIndex], PreferenceUtil.OPERATOR_CODE_KEY, "");
		String brandCode = PreferenceUtil.getString(mContext, DataManagerApplication.mImsiArray[mSimIndex], PreferenceUtil.DATAPLAN_TYPE_CODE_KEY, "");
		if (null != getmTmsManager()) {
			Log.e(TAG, "provinceCode>>>>" + provinceCode + "<<cityCode>>>" + cityCode + ">>carryCode>>" + carryCode + ">brandCode>>" + brandCode);
			result = getmTmsManager().setConfig(mSimIndex, provinceCode, cityCode, carryCode, brandCode, closeDay);// 保存配置。在进行流量校正之前，必要进行设置。返回ErrorCode
			int retCode = getmTmsManager().startCorrection(mSimIndex);
			Log.e(TAG, "-----result--setConfig-->>>>" + result + "-----retCode--->>>" + retCode);
			if (retCode != ERR_NONE && mIsUpdateUI[currentIndex]){
				//卡校正出错终止
				mIsCorrect[currentIndex] = false;
				if (mHandler == null) return;
				Message msg = mHandler.obtainMessage(MSG_TRAFFICT_ERROR , mSimIndex, 0);
				msg.obj = setSmsStatus(ERR_CORRECTION_ERROR);
				msg.sendToTarget();
				return;
			}
		}
	}
	/**
	 * 发送查询流量短信
	 * @param selectSimIndex   当前使用哪种sim卡发短信校正流量
	 * @param phoneNo   运营商号码
	 * @param message    短信内容
	 */
	private void sendCorrectMsg(int selectSimIndex, String phoneNo, String message) {
		try {
			int subId = ToolsUtil.getIdInDbBySimId(mContext, selectSimIndex);//通过simId来获得subId
			SmsManager sm = SmsManager.getSmsManagerForSubscriptionId(subId);
			Intent smsIntent = new Intent();
			smsIntent.setAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
			Log.e(TAG, TAG + ">>selectedIMSI>>>----subId---->>>>" + subId + "----->>>" + selectSimIndex);
			PendingIntent sentIntent = PendingIntent.getActivity(mContext, 0, smsIntent, 0);
			sm.sendTextMessage(phoneNo, null, message, sentIntent, null);
//			if (mIsUpdateUI[selectSimIndex] && mHandler != null) {
//				Message msg = mHandler.obtainMessage(SMS_SENT_OK_TAG , selectSimIndex, 0);
//				msg.sendToTarget();
//			}
			String imsi = ToolsUtil.getActiveSubscriberId(mContext, subId);
			PreferenceUtil.putLong(mContext, imsi, PreferenceUtil.SIM_SUBID_SMS_KEY, System.currentTimeMillis());
			PreferenceUtil.putString(mContext, imsi, PreferenceUtil.SMS_BODY_KEY, message + "," + phoneNo);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized void analysisSMS(final int simIndex, final String smsBody) {
		if (getmTmsManager() == null) return;
		new AsyncTask<String, String, String>(){

			@Override
			protected String doInBackground(String... params) {
				int state = getmTmsManager().analysisSMS(simIndex, mQueryCode, mQueryPort, smsBody);
				Log.v(TAG, "analysisSMS>>>>" + state);
				return null;
			}

		}.execute();
	}

	/**
	 * 发送短信失败
	 * @param simIndex
	 * @param sendSmsState -1为失败
     */
	public synchronized void sendSMSFail(final int simIndex, final int sendSmsState) {
		if (simIndex < 0) return;
		if (sendSmsState < 0) {
			if (mIsUpdateUI[simIndex]) {
				mIsCorrect[simIndex] = false;
				if (mHandler == null) return;
				Message msg = mHandler.obtainMessage(SMS_SENT_FAIL_TAG , simIndex, 0);
				msg.sendToTarget();
			}
		}
	}
	/**
	 * 保存套餐信息
	 * @param simIndex
	 */
	private int saveDataSet(int simIndex) {
//		logTemp += "常规-剩余[" + retTrafficInfo[0] + "]已用[" + retTrafficInfo[1] + "]总量[" +retTrafficInfo[2] +"]\n";
//		logTemp += "闲时-剩余[" + retTrafficInfo[3] + "]已用[" + retTrafficInfo[4] + "]总量[" +retTrafficInfo[5] +"]\n";
//		logTemp += "4G-剩余[" + retTrafficInfo[6] + "]已用[" + retTrafficInfo[7] + "]总量[" +retTrafficInfo[8] +"]\n";
		int[] trafficInfo;
		try {
			if (getmTmsManager() == null) {
				return -1;
			}
			//返回流量校正后的值以KB为单位
			trafficInfo = getmTmsManager().getTrafficInfo(simIndex);
			Log.e("trafficInfo0", "trafficInfo0------>>>" + Arrays.toString(trafficInfo) + "--->>>"+ trafficInfo.length);
//			[599531, -1, -1, 1056542, -1, -1, -1, -1, -1]

			//联通[4155893, -1, 5242879, -1, -1, -1, -1, -1, -1]
			//尊敬的13802236421客户，您上月结转至本月国内通用流量500.00M。截至02日11时29分，您当月常用流量已用1.85M，
			// 可用1198.14M，其中国内通用流量可用1000.00M（含上月结余500.00M），
			// 省内通用流量可用198.14M；更多流量内容请点击 gd.10086.cn/cxll 查询。以上信息可能存在延时，具体以详单为准。【中国移动】
			//移动[1226895, -1, 1228789, -1, -1, -1, -1, -1, -1]
			//[590407, -1, -1, 1056706, -1, -1, -1, -1, -1]
			//监听短信内容>>尊敬的13823533874客户，您上月结转至本月国内通用流量300.00M。截至08日13时56分，您当月常用流量已用23.47M，可用1608.52M，
			// 其中国内通用流量可用576.57M（含上月结余276.57M），省内闲时流量可用1031.94M；更多流量内容请点击 gd.10086.cn/cxll 查询。
			// 以上信息可能存在延时，具体以详单为准。【中国移动】

			//中国电信
			//trafficInfo0------>>>[2097152, -1, 2097152, -1, -1, -1, 31355546, -1, 31457280]--->>>9

//			监听短信内容>>尊敬的13823533874客户，您上月结转至本月国内通用流量300.00M。截至28日19时34分，您当月常用流量已用128.01M，可用1503.98M，
//          其中国内通用流量可用472.08M（含上月结余172.08M），省内闲时流量可用1031.90M；更多流量内容请点击 gd.10086.cn/cxll 查询。以上信息可能存在延时，具体以详单为准。
//         【中国移动】>>>>subId>>>5>>>>>number>>10086
//			[483409, -1, 1638410, 1056665, -1, -1, -1, -1, -1]--->>>9
			String currentIMSI = DataManagerApplication.mImsiArray[simIndex];
			String operator = ToolsUtil.getSimOperator(mContext, currentIMSI);
			//返回流量校正后的值以KB为单位
			//常规-总量
			long commonTotal = 0;
			commonTotal = PreferenceUtil.getLong(mContext, currentIMSI, PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
			if (trafficInfo[2] > 0) {
				if (TextUtils.equals(operator, mContext.getString(R.string.china_mobile)) && trafficInfo[3] > 0) {
					//中国移动 有闲时流量时不
					PreferenceUtil.putBoolean(mContext, currentIMSI, PreferenceUtil.CMCC_FREE_TMIME_KEY, true);
					PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
				} else {
					PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.DATAPLAN_COMMON_KEY, trafficInfo[2]);
				}
				if (commonTotal == 0) {
					//超额预警状态
					PreferenceUtil.putBoolean(mContext, currentIMSI, PreferenceUtil.PASS_WARNING_STATE_KEY, true);
				}
			} else {
				if (commonTotal == 0) {
					//超额预警状态
					PreferenceUtil.putBoolean(mContext, currentIMSI, PreferenceUtil.PASS_WARNING_STATE_KEY, true);
				}
				PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
			}

			//4G流量
			if (trafficInfo[8] > 0) {
				commonTotal = PreferenceUtil.getLong(mContext, currentIMSI, PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
				PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.DATAPLAN_COMMON_KEY, commonTotal + trafficInfo[8]);
			}

			//常规-剩余
			commonTotal = PreferenceUtil.getLong(mContext, currentIMSI, PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
			long commonRemain = trafficInfo[0] >= 0 ? trafficInfo[0] : 0;
			PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.REMAIN_DATAPLAN_COMMON_KEY, commonRemain);
			PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.REMAIN_DATAPLAN_AFTER_COMMON_KEY, commonRemain);

			//4G流量-剩余
			long remain4G =trafficInfo[6] >= 0 ? trafficInfo[6] : 0;
			commonRemain = PreferenceUtil.getLong(mContext, currentIMSI, PreferenceUtil.REMAIN_DATAPLAN_COMMON_KEY, 0);
			long commonAfterRemain = PreferenceUtil.getLong(mContext, currentIMSI, PreferenceUtil.REMAIN_DATAPLAN_AFTER_COMMON_KEY, 0);
			PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.REMAIN_DATAPLAN_COMMON_KEY, commonRemain + remain4G);
			PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.REMAIN_DATAPLAN_AFTER_COMMON_KEY, commonAfterRemain + remain4G);

			//常规-已用
			if (trafficInfo[1] >= 0) {
				PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.USED_DATAPLAN_COMMON_KEY, trafficInfo[1]);
				PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.USED_DATAPLAN_AFTER_COMMON_KEY, trafficInfo[1]);
			}
			//4G流量-已用
			if (trafficInfo[7] >= 0) {
				PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.USED_DATAPLAN_COMMON_KEY, trafficInfo[1] + trafficInfo[7]);
				PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.USED_DATAPLAN_AFTER_COMMON_KEY, trafficInfo[1] + trafficInfo[7]);
			}

			if (commonTotal > 0 && (trafficInfo[1] < 0 || trafficInfo[7] < 0)) {
				commonRemain = PreferenceUtil.getLong(mContext, currentIMSI, PreferenceUtil.REMAIN_DATAPLAN_COMMON_KEY, 0);
				long usedData = commonTotal - commonRemain;
				PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.USED_DATAPLAN_COMMON_KEY, usedData >= 0 ? usedData : 0);
				PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.USED_DATAPLAN_AFTER_COMMON_KEY, usedData >= 0 ? usedData : 0);
			}
			if (commonTotal <= 0) {
				//流量校正短信中没有总量时，则通过剩余+ 本机已用获得总量
				long commonUsed = PreferenceUtil.getLong(mContext, currentIMSI, PreferenceUtil.USED_DATAPLAN_AFTER_COMMON_KEY, 0);
				//PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.USED_DATAPLAN_COMMON_KEY, commonUsed);
				commonTotal = commonRemain + commonUsed;
				PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.DATAPLAN_COMMON_KEY, commonTotal);
			}


// 一期需求不统计闲时流量
// 			//闲时-总量
//			if (trafficInfo[5] > 0) {
//				PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.FREE_DATA_TOTAL_KEY, (trafficInfo[5]));
//			}
//			//闲时-剩余
//			long freeTotal = PreferenceUtil.getLong(mContext, currentIMSI, PreferenceUtil.FREE_DATA_TOTAL_KEY, 0);
//			if (trafficInfo[3] >= 0) {
//				PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.REMAIN_FREE_DATA_TOTAL_KEY, (trafficInfo[3]));
//			}
//			long reamainFree = PreferenceUtil.getLong(mContext, currentIMSI, PreferenceUtil.REMAIN_FREE_DATA_TOTAL_KEY, 0);
//			//闲时-已用
//			if (trafficInfo[4] >= 0) {
//				PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.USED_FREE_DATA_TOTAL_KEY, trafficInfo[4]);
//			} else {
//				if (freeTotal > 0) {
//					PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.USED_FREE_DATA_TOTAL_KEY, freeTotal - reamainFree);
//				}
//			}
			//首次流量校正成功
			PreferenceUtil.putInt(mContext, currentIMSI, PreferenceUtil.DATAPLAN_CORRECT_FIRST_KEY, 1);
			//流量校正完成时间
			PreferenceUtil.putLong(mContext, currentIMSI, PreferenceUtil.CORRECT_OK_TIME_KEY, System.currentTimeMillis());
//			//用于更新图标
//			ToolsUtil.updateIconReceiver();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
}
