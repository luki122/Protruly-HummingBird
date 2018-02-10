package com.hb.netmanage.activity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.hb.netmanage.R;
import com.hb.netmanage.adapter.RangeAppAdapter;
import com.hb.netmanage.fragement.SimFragment;
import com.hb.netmanage.net.NetController;
import com.hb.netmanage.receiver.SimStateReceiver;
import com.hb.netmanage.service.NetManagerService;
import com.hb.netmanage.utils.LogUtil;
import com.hb.netmanage.utils.PreferenceUtil;
import com.hb.netmanage.utils.StringUtil;
import com.hb.netmanage.utils.ToolsUtil;
import com.hb.netmanage.view.WarnDataPreference;

import hb.app.dialog.AlertDialog;
import hb.app.dialog.AlertDialog.Builder;
import hb.preference.Preference;
import hb.preference.Preference.OnPreferenceChangeListener;
import hb.preference.Preference.OnPreferenceClickListener;
import hb.preference.PreferenceGroup;
import hb.preference.PreferenceScreen;
import hb.preference.SwitchPreference;
import hb.widget.toolbar.Toolbar;

/**
 * SIM卡设置
 * @author zhaolaichao
 */
public class SimDataSetActivity extends BasePreferenceActivity implements OnPreferenceClickListener, OnPreferenceChangeListener {
	/**
	 * 默认显示总量的比例
	 */
	public static int DEFAULT_RATE = 80;
	/**
	 * 默认显示最少总量的比例
	 */
	private static int DEFAULT_MIN_RATE = 50;
	/**
	 * 默认显示最大总量的比例
	 */
	private static int DEFAULT_MAX_RATE = 100;

	private Preference mLayOperatorSet;
	private Preference mLayDataPlan;
	private SwitchPreference mLayPassWarning;
	private PreferenceGroup mLayWarnGroup;
	private WarnDataPreference mLayWarnValue;
	private PreferenceScreen mLayOrientApp;
	private SwitchPreference mLayAutoCorrect;
	private PreferenceScreen mLayDataClean;
	private String mSimTitle;
	/**
	 * 当前选择的sim的IMS号
	 */
	private String mSelectedSimIMSI;
	private String mDefaultWarnValue;
	private long mTotalData;
	private AlarmManager mAlarm;
	private Intent mAlarmIntent;
	private int mSelectedIndex;
	private int mWarnMaxProgress;
	private int mCurrentRate;
	private int mCurrentInfoState;
     @SuppressWarnings("deprecation")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	addPreferencesFromResource(R.xml.activity_sim_set);
	    initSimInfo();
    	initView();
    }

	@Override
	public void setSimStateChangeListener(int simState) {
		mCurrentInfoState = ToolsUtil.getCurrentNetSimSubInfo(this);
		if (simState == SimStateReceiver.SIM_INVALID) {
			setPreferenceState(false);
		} else {
			setPreferenceState(true);
			initSimInfo();
			initView();
		}
	}

	/**
      * 初始化数据
      */
     private void initSimInfo() {
		 mSelectedIndex = getIntent().getIntExtra("CURRENT_INDEX", -1);
  	    if (mSelectedIndex == 0) {
  	    	//卡1
  	    	mSelectedSimIMSI = PreferenceUtil.getString(this, PreferenceUtil.SIM_1, PreferenceUtil.IMSI_KEY, "");
  	    } else if (mSelectedIndex == 1) {
  	    	//卡2
  	    	mSelectedSimIMSI = PreferenceUtil.getString(this, PreferenceUtil.SIM_2, PreferenceUtil.IMSI_KEY, "");
  	    } else {
			//默认为显示上网卡
			mSelectedSimIMSI = ToolsUtil.getActiveSimImsi(this);
			mSelectedIndex = ToolsUtil.getCurrentNetSimSubInfo(this);
			if (mSelectedIndex == -1) {
				mSimTitle = getString(R.string.app_name);
			}
		}
		if (mSelectedIndex == 0) {
			mSimTitle = getString(R.string.sim1_set);
		} else if (mSelectedIndex == 1) {
			mSimTitle = getString(R.string.sim2_set);
		}
     }
     
    @SuppressWarnings("deprecation")
	private void initView() {
		 Toolbar toolbar = getToolbar();
		 toolbar.setTitle(mSimTitle);
		 toolbar.setElevation(1);
		 toolbar.setNavigationOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				SimDataSetActivity.this.finish();
			}
		});
    	//运营商设置
		mLayOperatorSet = findPreference("data_operator");
		mLayOperatorSet.setOnPreferenceClickListener(this);
		//套餐设置
		mLayDataPlan = findPreference("data_plan_set");
		mLayDataPlan.setOnPreferenceClickListener(this);
    	//超额预警  初始化设置完套餐总量后默认打开预警开关
		long dataTotal = PreferenceUtil.getLong(this, mSelectedSimIMSI, PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
		boolean state = dataTotal > 0 ? true : false;
		boolean warnState = PreferenceUtil.getBoolean(this, mSelectedSimIMSI, PreferenceUtil.PASS_WARNING_STATE_KEY, state);
		mLayPassWarning = (SwitchPreference) findPreference("data_plan_warn");
		mLayPassWarning.setEnabled(state);
		mLayPassWarning.setChecked(warnState);
    	mLayPassWarning.setOnPreferenceChangeListener(this);
		
    	//超额预警值
    	mLayWarnGroup = ((PreferenceGroup) findPreference("lay_warn"));  
    	mLayWarnValue = (WarnDataPreference) findPreference("data_plan_warn_value");
    	mDefaultWarnValue = PreferenceUtil.getString(this, mSelectedSimIMSI, PreferenceUtil.PASS_WARNING_VALUE_KEY, DEFAULT_RATE + "%");
    	mTotalData = PreferenceUtil.getLong(this, mSelectedSimIMSI, PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
    	//转为MB
    	mTotalData = mTotalData /1024;
    	mCurrentRate = Integer.parseInt(mDefaultWarnValue.substring(0, mDefaultWarnValue.indexOf("%")));
    	if (mTotalData > 0) {
    		mWarnMaxProgress = (int) (mTotalData * mCurrentRate) /100;
    		mDefaultWarnValue = mCurrentRate + "% (" + mWarnMaxProgress + getString(R.string.megabyte_short)  + ")";
    	} else {
    		mLayWarnValue.setEnabled(false);
    	}
    	PreferenceUtil.putString(this, mSelectedSimIMSI, PreferenceUtil.PASS_WARNING_VALUE_KEY, mDefaultWarnValue);
    	//最左端为50%,最右端为100%
		mLayWarnValue.setMaxProgress(DEFAULT_MIN_RATE);
		mLayWarnValue.setProgress(mCurrentRate == 0 ? (DEFAULT_RATE - DEFAULT_MIN_RATE) : mCurrentRate - DEFAULT_MIN_RATE);

    	mLayWarnValue.setWarnValue(mDefaultWarnValue);
    	if(!warnState) {
    		mLayWarnGroup.removePreference(mLayWarnValue);
    	} 
    	mLayWarnValue.setSeekBarChangeListener(new WarnDataPreference.ISeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				//超额预警值
				PreferenceUtil.putString(SimDataSetActivity.this, mSelectedSimIMSI, PreferenceUtil.PASS_WARNING_VALUE_KEY, mLayWarnValue.getWarnValue());
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				Log.v("mSb.getProgress()", "mSb.getProgress()>>" + progress);
				mCurrentRate = progress + DEFAULT_MIN_RATE;
				int value  = (int)(mCurrentRate * mTotalData / 100);
				mDefaultWarnValue = mCurrentRate + "% (" + value + getString(R.string.megabyte_short)  + ")";
				mLayWarnValue.setWarnValue(mDefaultWarnValue);
			}
		});
    	//定向流量应用
    	mLayOrientApp = (PreferenceScreen) findPreference("orient_app");
    	mLayOrientApp.setOnPreferenceClickListener(this);
		//自动校正流量
		mLayAutoCorrect = (SwitchPreference) findPreference("data_auto");
		//套餐基本信息设置是否完整
		boolean simInfoState = PreferenceUtil.getBoolean(this, mSelectedSimIMSI, PreferenceUtil.SIM_BASEINFO_KEY, false);
		boolean correctState = PreferenceUtil.getBoolean(this, mSelectedSimIMSI, PreferenceUtil.AUTO_CORRECT_STATE_KEY, simInfoState);
		mLayAutoCorrect.setEnabled(simInfoState);
		mLayAutoCorrect.setChecked(correctState);
		mLayAutoCorrect.setOnPreferenceChangeListener(this);
		boolean correct_repeat = PreferenceUtil.getBoolean(this, mSelectedSimIMSI, PreferenceUtil.AUTO_CORRECT_REPEAT_KEY, false);
    	//清空流量数据
    	mLayDataClean = (PreferenceScreen) findPreference("data_clean");
    	mLayDataClean.setOnPreferenceClickListener(this);

		mCurrentInfoState = ToolsUtil.getCurrentNetSimSubInfo(this);
		if (mCurrentInfoState == -1) {
			setPreferenceState(false);
		}
     }
    

	private void setPreferenceState(boolean state) {
		if (mLayOperatorSet == null || mLayDataPlan == null || mLayAutoCorrect == null
				|| mLayWarnValue == null || mLayOrientApp == null || mLayDataClean == null
				|| mLayWarnGroup == null) {
			return;
		}
		mLayOperatorSet.setEnabled(state);
		mLayDataPlan.setEnabled(state);
		mLayAutoCorrect.setEnabled(state);
		mLayAutoCorrect.setChecked(state);
		mLayPassWarning.setChecked(state);
		mLayPassWarning.setEnabled(state);
		if (!state) {
			mLayWarnGroup.removePreference(mLayWarnValue);
		} else {
			mLayWarnGroup.addPreference(mLayWarnValue);
			mLayWarnValue.setMaxProgress(DEFAULT_MIN_RATE);
			mLayWarnValue.setProgress(mCurrentRate - DEFAULT_MIN_RATE);
			mLayWarnValue.setWarnValue(mDefaultWarnValue);
		}
		mLayWarnValue.setEnabled(state);
		LogUtil.e("", ">>>>>>" + !state);
		mLayOrientApp.setEnabled(state);
		mLayDataClean.setEnabled(state);
	}

    @Override
	public boolean onPreferenceClick(Preference preference) {
    	Intent intent = null;
		if (preference == mLayOperatorSet) {
			intent = new Intent(SimDataSetActivity.this, OperatorInfoActivity.class);
			intent.putExtra("CURRENT_INDEX", mSelectedIndex);
			startActivity(intent);
		} else if (preference == mLayDataPlan) {
			intent = new Intent(SimDataSetActivity.this, DataPlanSetActivity.class);
			intent.putExtra("CURRENT_INDEX", mSelectedIndex);
			startActivityForResult(intent, 1000);
		} else if (preference == mLayOrientApp) {
			intent = new Intent(SimDataSetActivity.this, OrientAppActivity.class);
			intent.putExtra("CURRENT_INDEX", mSelectedIndex);
			startActivity(intent);
		} else if (preference == mLayDataClean) {
			showCleanDialog();
		}
		return false;
	}
    
    @Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
    	Log.v("onPreferenceChange", "newValue>>" + newValue);
		if (preference == mLayPassWarning) {
			boolean state = (Boolean) newValue;
			if (state) {
				mLayWarnGroup.addPreference(mLayWarnValue);
				mLayWarnValue.setMaxProgress(DEFAULT_MIN_RATE);
				mLayWarnValue.setProgress(mCurrentRate - DEFAULT_MIN_RATE);
		    	mLayWarnValue.setWarnValue(mDefaultWarnValue);
			} else {
				mLayWarnGroup.removePreference(mLayWarnValue);
			}
			//超额预警开关
			PreferenceUtil.putBoolean(SimDataSetActivity.this, mSelectedSimIMSI, PreferenceUtil.PASS_WARNING_STATE_KEY, state);
		} else if (preference == mLayWarnValue) {
			mLayWarnValue.setWarnValue(mDefaultWarnValue);
		} else if (preference == mLayAutoCorrect) {
			boolean state = (Boolean) newValue;
			//自动校正流量开关
			PreferenceUtil.putBoolean(SimDataSetActivity.this, mSelectedSimIMSI, PreferenceUtil.AUTO_CORRECT_STATE_KEY, state);
			//设置闹钟进行定时任务  监听ACTION_DATE_CHANGED广播
			//setAlarm(state);
		}
		return true;
	}

	/**
	 * 是否每天定时发送验证短信
	 * @param isCorrect
	 */
	private void setAlarm(boolean isCorrect) {
		mAlarmIntent  = new Intent(SimDataSetActivity.this, NetManagerService.class);
		mAlarmIntent.putExtra("CURRENT_IMSI", mSelectedSimIMSI );
		PendingIntent pi = PendingIntent.getService(SimDataSetActivity.this, 0, mAlarmIntent, 0);
		mAlarm = (AlarmManager)SimDataSetActivity.this.getSystemService(Context.ALARM_SERVICE);
		if (isCorrect) {
			//从现在起第三天相同时间开始
			long repeatTime = 24 * 3600 * 1000;
			long triggerAtMillis = StringUtil.getStartTime(0,0,0);
			mAlarm.setRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, repeatTime,  pi);
			PreferenceUtil.putBoolean(this, mSelectedSimIMSI, PreferenceUtil.AUTO_CORRECT_REPEAT_KEY, true);
		} else {
			mAlarm.cancel(pi);
			PreferenceUtil.putBoolean(this, mSelectedSimIMSI, PreferenceUtil.AUTO_CORRECT_REPEAT_KEY, false);
		}
	}

    @Override
	 protected void onRestart() {
		super.onRestart();
		mCurrentInfoState = ToolsUtil.getCurrentNetSimSubInfo(this);
		if (mCurrentInfoState == -1) {
			//拔卡状态
			setPreferenceState(false);
			return;
		}
		if (mSelectedIndex == 0) {
			mSimTitle = getString(R.string.sim1_set);
		} else if (mSelectedIndex == 1) {
			mSimTitle = getString(R.string.sim2_set);
		}
		if (null != getToolbar()) {
			getToolbar().setTitle(mSimTitle);
		}
		mTotalData = PreferenceUtil.getLong(this, mSelectedSimIMSI, PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
		//转为MB
		mTotalData = mTotalData /1024;
		if (mTotalData > 0) {
			boolean warnState = PreferenceUtil.getBoolean(this, mSelectedSimIMSI, PreferenceUtil.PASS_WARNING_STATE_KEY, false);
			if (!warnState && !mLayPassWarning.isEnabled()) {
				//默认为关闭。设置完流量套餐后则打开显示
				mLayPassWarning.setChecked(true);
				mLayPassWarning.setEnabled(true);
				mLayWarnGroup.addPreference(mLayWarnValue);
			}
			mLayWarnValue.setEnabled(true);
			mDefaultWarnValue = PreferenceUtil.getString(this, mSelectedSimIMSI, PreferenceUtil.PASS_WARNING_VALUE_KEY, DEFAULT_RATE + "%");
			if (mDefaultWarnValue.contains("%")) {
				int rate = Integer.parseInt(mDefaultWarnValue.substring(0, mDefaultWarnValue.indexOf("%")));
				//显示MB
				mWarnMaxProgress = (int)(mTotalData * rate) /100;
				mLayWarnValue.setMaxProgress(DEFAULT_MIN_RATE);
				mLayWarnValue.setProgress(rate - DEFAULT_MIN_RATE);
				mDefaultWarnValue = rate + "%(" + mWarnMaxProgress +  getString(R.string.megabyte_short) + ")";
				mLayWarnValue.setWarnValue(mDefaultWarnValue);
				PreferenceUtil.putString(this, mSelectedSimIMSI, PreferenceUtil.PASS_WARNING_VALUE_KEY, mDefaultWarnValue);
			}
		} else {
			mLayWarnValue.setEnabled(false);
			mDefaultWarnValue = DEFAULT_RATE + "%";
			mLayWarnValue.setWarnValue(mDefaultWarnValue);
	    	PreferenceUtil.putString(this, mSelectedSimIMSI, PreferenceUtil.PASS_WARNING_VALUE_KEY, mDefaultWarnValue);
		}
		boolean simInfoState = PreferenceUtil.getBoolean(this, mSelectedSimIMSI, PreferenceUtil.SIM_BASEINFO_KEY, false);
		boolean correctState = PreferenceUtil.getBoolean(this, mSelectedSimIMSI, PreferenceUtil.AUTO_CORRECT_STATE_KEY, simInfoState);
		mLayAutoCorrect.setEnabled(simInfoState);
		mLayAutoCorrect.setChecked(correctState);

	}
    

	/**
	 * 清除缓存数据
	 */
	private void showCleanDialog() {
		hb.app.dialog.AlertDialog.Builder builder = new Builder(this);
		builder.setTitle(getString(R.string.data_clean));
		builder.setMessage(getString(R.string.data_clean_info));
		builder.setPositiveButton(com.hb.R.string.ok, new hb.app.dialog.AlertDialog.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//提示用户
				cleanDataAll();
				Toast.makeText(SimDataSetActivity.this, R.string.clear_data_ok, Toast.LENGTH_SHORT).show();
			}
		});
		builder.setNegativeButton(com.hb.R.string.cancel, new hb.app.dialog.AlertDialog.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
			
		});
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
		Button positiveBtn = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
		if (positiveBtn != null) {
			positiveBtn.setBackgroundResource(com.hb.R.drawable.button_background_hb_delete);
		}
	}

	/**
	 * 清空所有相关套餐设置数据
	 */
	private void cleanDataAll() {
		mLayWarnGroup.removePreference(mLayWarnValue);
		mLayPassWarning.setChecked(false);
		mLayPassWarning.setEnabled(false);
		boolean simBaseState = PreferenceUtil.getBoolean(this, mSelectedSimIMSI, PreferenceUtil.SIM_BASEINFO_KEY, false);
		//超额预警开关
		PreferenceUtil.putBoolean(this, mSelectedSimIMSI, PreferenceUtil.PASS_WARNING_STATE_KEY, false);
		//自动校正开关
		PreferenceUtil.putBoolean(SimDataSetActivity.this, mSelectedSimIMSI, PreferenceUtil.AUTO_CORRECT_STATE_KEY, simBaseState);
		//超额预警值
		PreferenceUtil.putString(this, mSelectedSimIMSI, PreferenceUtil.PASS_WARNING_VALUE_KEY, DEFAULT_RATE + "%");
		//套餐流量大小
		PreferenceUtil.putLong(this, mSelectedSimIMSI, PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
		//月结日
		PreferenceUtil.putInt(this, mSelectedSimIMSI, PreferenceUtil.CLOSEDAY_KEY, 1);
		//闲时流量开关状态
		PreferenceUtil.putBoolean(this, mSelectedSimIMSI, PreferenceUtil.FREE_DATA_STATE_KEY, false);
		//闲时套餐流量
		PreferenceUtil.putLong(this, mSelectedSimIMSI, PreferenceUtil.FREE_DATA_TOTAL_KEY, 0);
		//开始时间
		PreferenceUtil.putString(this, mSelectedSimIMSI, PreferenceUtil.FREE_DATA_START_TIME_KEY, getString(R.string.un_set));
		//结束时间
		PreferenceUtil.putString(this, mSelectedSimIMSI, PreferenceUtil.FREE_DATA_END_TIME_KEY, getString(R.string.un_set));
		if (!TextUtils.equals(ToolsUtil.getActiveSimImsi(this), mSelectedSimIMSI)) {
			//已用套餐流量 非上网卡不清除
			PreferenceUtil.putLong(this, mSelectedSimIMSI, PreferenceUtil.USED_DATAPLAN_COMMON_KEY, 0);
			//sim已使用通用套餐流量
			PreferenceUtil.putLong(this, mSelectedSimIMSI, PreferenceUtil.USED_DATAPLAN_AFTER_COMMON_KEY, 0);
			//清除日使用流量
			PreferenceUtil.putLong(this, mSelectedSimIMSI, PreferenceUtil.DAY_USED_STATS_KEY, 0);
		}
        //清除校正时间
		PreferenceUtil.putLong(this, mSelectedSimIMSI, PreferenceUtil.CORRECT_OK_TIME_KEY, 0);
		//已用闲时流量
		PreferenceUtil.putLong(this, mSelectedSimIMSI, PreferenceUtil.USED_FREE_DATA_TOTAL_KEY, 0);
		//常规-剩余
		PreferenceUtil.putLong(this, mSelectedSimIMSI, PreferenceUtil.REMAIN_DATAPLAN_COMMON_KEY, 0);
		//sim剩余通用套餐流量
		PreferenceUtil.putLong(this, mSelectedSimIMSI, PreferenceUtil.REMAIN_DATAPLAN_AFTER_COMMON_KEY, 0);
		//中国移动 有闲时流量时
		PreferenceUtil.putBoolean(this, mSelectedSimIMSI, PreferenceUtil.CMCC_FREE_TMIME_KEY, false);
		//闲时-剩余
		PreferenceUtil.putLong(this, mSelectedSimIMSI, PreferenceUtil.REMAIN_FREE_DATA_TOTAL_KEY, 0);
		//记录日用流量设置
		PreferenceUtil.putBoolean(this, mSelectedSimIMSI, PreferenceUtil.NOTIFY_WARN_DAY_KEY, false);
		//清除当前上网卡的imsi
		PreferenceUtil.putString(this, "", PreferenceUtil.CURRENT_ACTIVE_IMSI_KEY, null);
		//未设置流量套餐时,提示最多弹出3次
		PreferenceUtil.putInt(this, mSelectedSimIMSI, PreferenceUtil.NOTIFY_UNDATA_COUNT_KEY, 0);
		//第一次加载
		PreferenceUtil.putInt(this, mSelectedSimIMSI, PreferenceUtil.LOAD_FIRST_KEY, 0);
		PreferenceUtil.putInt(this, mSelectedSimIMSI, PreferenceUtil.DATAPLAN_CORRECT_FIRST_KEY, 0);
		PreferenceUtil.putString(this, "", PreferenceUtil.TOP_APP_NAME_KEY, null);
		PreferenceUtil.putBoolean(this, "", PreferenceUtil.DATE_CHANGE_KEY, false);
		PreferenceUtil.putLong(this, mSelectedSimIMSI, PreferenceUtil.SIM_SUBID_SMS_KEY, System.currentTimeMillis());
		//每分钟已用数据
		PreferenceUtil.putLong(this, mSelectedSimIMSI, PreferenceUtil.MINUTE_DATA_USED_KEY, 0);
   	    PreferenceUtil.putString(this, mSelectedSimIMSI, PreferenceUtil.SMS_BODY_KEY, null);
   	    //每分钟提示框标志
   	    PreferenceUtil.putBoolean(this, mSelectedSimIMSI, PreferenceUtil.MINUTE_DATA_USED_DIALOG_KEY, false);
		//清除手动修改已用闲时流量
		PreferenceUtil.putLong(this, mSelectedSimIMSI, PreferenceUtil.USED_MAN_FREE_KEY, 0);
		//清除手动修改已用闲时流量时间
		PreferenceUtil.putLong(this, mSelectedSimIMSI, PreferenceUtil.USED_FREE_DATA_MAN_TIME_KEY, 0);
		//清除手动修改已用日常流量
		PreferenceUtil.putLong(this, mSelectedSimIMSI, PreferenceUtil.USED_MAN_COMMON_KEY, 0);
		//清除手动修改已用日常流量时间
		PreferenceUtil.putLong(this, mSelectedSimIMSI, PreferenceUtil.USED_COMMON_DATA_MAN_TIME_KEY, 0);
		PreferenceUtil.putInt(this, "", PreferenceUtil.SELECTED_DATE_KEY, DataRangeActivity.TODAY_TAG);
		PreferenceUtil.putInt(this, mSelectedSimIMSI, PreferenceUtil.CORRECT_DATA_KEY, SimFragment.BTN_CORRECT_TAG);
		//清除受限网络 移动网络 wifi网络 start
		PreferenceUtil.putString(this, "", RangeAppAdapter.TYPE_DATA, null);
		PreferenceUtil.putString(this, "", RangeAppAdapter.TYPE_WLAN, null);
		NetController.getInstance().clearFirewallChain(NetController.CHAIN_MOBILE);
		NetController.getInstance().clearFirewallChain(NetController.CHAIN_WIFI);
		//清除受限网络 移动网络 wifi网络 end

		//setAlarm(false);
		//stopService(mAlarmIntent);
	}
	
}
