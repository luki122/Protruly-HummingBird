package com.hb.netmanage.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.SpinnerPopupDialog;
import android.widget.Toast;

import com.hb.netmanage.DataManagerApplication;
import com.hb.netmanage.R;
import com.hb.netmanage.receiver.SimStateReceiver;
import com.hb.netmanage.utils.NotificationUtil;
import com.hb.netmanage.utils.PreferenceUtil;
import com.hb.netmanage.utils.StringUtil;
import com.hb.netmanage.utils.ToolsUtil;
import com.hb.netmanage.view.DataplanPreference;

import hb.app.dialog.AlertDialog.Builder;
import hb.app.dialog.TimePickerDialog;
import hb.preference.Preference;
import hb.preference.Preference.OnPreferenceChangeListener;
import hb.preference.Preference.OnPreferenceClickListener;
import hb.preference.PreferenceGroup;
import hb.preference.SwitchPreference;
import hb.widget.TimePicker;
import hb.widget.toolbar.Toolbar;

/**
 * 套餐流量设置
 * @author zhaolaichao
 */
public class DataPlanSetActivity extends BasePreferenceActivity implements OnPreferenceClickListener, OnPreferenceChangeListener{
	private static final String TAG = "DataPlanSetActivity";
	private static final int UPDATE_STATS_TAG = 10000;
	 
	/**
	 * 套餐流量
	 */
	private DataplanPreference mPreDataplanTotal;
	private DataplanPreference mPreCloseDay;
	private SwitchPreference mPreFreeState;
	/**
	 * 闲时套餐流量
	 */
	private DataplanPreference mPreFreeTotal;
	private DataplanPreference mPreStartTime;
	private DataplanPreference mPreEndTime;
	private PreferenceGroup mFreeGroup;
	/**
	 * 点击view的item
	 */
	private Preference mClickPre;
	/**
	 * 当前选择的sim的IMS号
	 */
	String mSelectedSimIMSI;
	private String mSelectedTime = null;
	private int mCloseDay = 1;
	/**
	 * 当前卡索引
	 */
	private int mSelectedIndex;
	/**
	 * 初始化加载
	 */
	private final static int LOAD_FIRST = 1;
	private final static int MONTH_LENGTH = 31;
	private int mClickIndex;
	private static String[] mDays = new String[MONTH_LENGTH];
	/**
	 * 套餐设置状态
	 */

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case UPDATE_STATS_TAG:
					//监听开关状态
					DataManagerApplication.getInstance().getUpdateObserver().setUpdate();
					break;
			}
			removeMessages(UPDATE_STATS_TAG);
		}
	};

	Runnable mRn = new Runnable() {
		@Override
		public void run() {
			mHandler.sendEmptyMessage(UPDATE_STATS_TAG);
		}
	};
	@SuppressWarnings("deprecation")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	addPreferencesFromResource(R.xml.activity_dataplan_set);
		 //设置月结日
		 boolean isEn = ToolsUtil.isEnglish(this);
		 for (int i = 0; i < MONTH_LENGTH; i++) {
			 if (isEn) {
				 mDays[i] = "" + (i + 1);
			 } else {
				 mDays[i] = getString(R.string.every_month) + (i + 1) + getString(R.string.date);
			 }
		 }
    	//初始化数据
    	initSimInfo();
    }
   
     /**
      * 初始化数据
      */
     private void initSimInfo() {
    	 mSelectedIndex = getIntent().getIntExtra("CURRENT_INDEX", 0);
     	if (mSelectedIndex == 0) {
 	    	//卡1
 	    	mSelectedSimIMSI = PreferenceUtil.getString(this, PreferenceUtil.SIM_1, PreferenceUtil.IMSI_KEY, null);
 	    } else if (mSelectedIndex == 1) {
 	    	//卡2
 	    	mSelectedSimIMSI = PreferenceUtil.getString(this, PreferenceUtil.SIM_2, PreferenceUtil.IMSI_KEY, null);
 	    }
     	initView();
     }
     
    @SuppressWarnings("deprecation")
	private void initView() {
    	Toolbar toolbar = getToolbar();
		toolbar.setTitle(getString(R.string.dataplan_set));
		toolbar.setElevation(1);
		toolbar.setNavigationOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
                DataPlanSetActivity.this.finish();				
			}
		});
		int loadFirst = PreferenceUtil.getInt(this, mSelectedSimIMSI, PreferenceUtil.LOAD_FIRST_KEY, 0);
		if (loadFirst == 0) {
			PreferenceUtil.putInt(this, mSelectedSimIMSI, PreferenceUtil.LOAD_FIRST_KEY, LOAD_FIRST);
		}
    	//套餐流量
     	long dataTotal = PreferenceUtil.getLong(this, mSelectedSimIMSI, PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
    	mPreDataplanTotal = (DataplanPreference) findPreference("data_plan_total");
    	mPreDataplanTotal.setItemTitle(getString(R.string.data_total));
    	String formatData = StringUtil.formatDataFlowSize(this, dataTotal);
		mPreDataplanTotal.setSubContent(dataTotal == 0 ? getString(R.string.un_set) : formatData);
		mPreDataplanTotal.setOnPreferenceClickListener(this);
    	//月结日
    	mCloseDay = PreferenceUtil.getInt(this, mSelectedSimIMSI, PreferenceUtil.CLOSEDAY_KEY, 0);
    	mPreCloseDay = (DataplanPreference) findPreference("sim_close_day");
    	mPreCloseDay.setItemTitle(getString(R.string.month_end_day));
		String closeDay = mCloseDay == 0 ? mDays[mCloseDay] : mDays[mCloseDay - 1];
		mPreCloseDay.setSubContent(closeDay);
    	mPreCloseDay.setOnPreferenceClickListener(this);

		PreferenceGroup planGroup = (PreferenceGroup) findPreference("lay_sim_info");
    	//闲时流量
    	mFreeGroup = (PreferenceGroup) findPreference("lay_data_free");
		mPreFreeTotal = (DataplanPreference) findPreference("data_free");
		mPreFreeState = (SwitchPreference) findPreference("free_data_state");
		mPreStartTime = (DataplanPreference) findPreference("sim_start_time");
		mPreEndTime = (DataplanPreference) findPreference("sim_end_time");

		//闲时流量暂时不显示
		if (null != planGroup) {
			if (null != mPreFreeTotal) {
				mFreeGroup.removePreference(mPreFreeTotal);
			}
			if (null != mPreStartTime) {
				mFreeGroup.removePreference(mPreStartTime);
			}
			if (null != mPreEndTime) {
				mFreeGroup.removePreference(mPreEndTime);
			}
			if (null != mPreFreeState) {
				planGroup.removePreference(mPreFreeState);
			}
		}
		//闲时流量暂时不显示
     }
    

	private void showFreeView() {
		mFreeGroup.setEnabled(false);
		boolean freeStaus = PreferenceUtil.getBoolean(this, mSelectedSimIMSI, PreferenceUtil.FREE_DATA_STATE_KEY, false);
		mPreFreeState.setChecked(freeStaus);
		mPreFreeState.setOnPreferenceChangeListener(this);
		mPreFreeState.setEnabled(false);

		//闲时套餐流量
		long freeTotal = PreferenceUtil.getLong(this, mSelectedSimIMSI, PreferenceUtil.FREE_DATA_TOTAL_KEY, 0);
		freeTotal = freeTotal - freeTotal % 1024;
		mPreFreeTotal.setItemTitle(getString(R.string.dataplan_free));
		//显示单位为MB
		String freeData = StringUtil.formatDataFlowSize(this, freeTotal);
		mPreFreeTotal.setSubContent(freeTotal == 0 ? getString(R.string.un_set) : freeData);
		mPreFreeTotal.setOnPreferenceClickListener(this);
		//开始时间
		String startTime = PreferenceUtil.getString(this, mSelectedSimIMSI, PreferenceUtil.FREE_DATA_START_TIME_KEY, getString(R.string.un_set));
		mPreStartTime.setItemTitle(getString(R.string.start_time));
		if (TextUtils.isEmpty(startTime)) {
			startTime = getString(R.string.un_set);
		}
		mPreStartTime.setSubContent(startTime);
		mPreStartTime.setOnPreferenceClickListener(this);

		//结束时间
		String endTime = PreferenceUtil.getString(this, mSelectedSimIMSI, PreferenceUtil.FREE_DATA_END_TIME_KEY, getString(R.string.un_set));
		mPreEndTime.setItemTitle(getString(R.string.end_time));
		if (TextUtils.isEmpty(endTime)) {
			endTime = getString(R.string.un_set);
		}
		mPreEndTime.setSubContent(endTime);
		mPreEndTime.setOnPreferenceClickListener(this);
	}
    @Override
	public boolean onPreferenceClick(Preference preference) {
    	mClickPre = preference;
		if (preference == mPreDataplanTotal) {
			showDataPlan(getString(R.string.common_data_total), PreferenceUtil.DATAPLAN_COMMON_KEY, mPreDataplanTotal);
		} else if (preference == mPreFreeTotal) {
			showDataPlan(getString(R.string.dataplan_free), PreferenceUtil.FREE_DATA_TOTAL_KEY, mPreFreeTotal);
		} else if (preference == mPreCloseDay) {
			mCloseDay = PreferenceUtil.getInt(this, mSelectedSimIMSI, PreferenceUtil.CLOSEDAY_KEY, 1);
			mClickIndex = mCloseDay - 1;
			if (ToolsUtil.isEnglish(this)) {
				showDataType(getString(R.string.select) + " " + getString(R.string.month_end_day), mDays);
			} else {
				showDataType(getString(R.string.select) + getString(R.string.month_end_day), mDays);
			}
		} else if (preference == mPreStartTime) {
			changeFreeTime(mPreStartTime, getString(R.string.set) + getString(R.string.start_time), PreferenceUtil.FREE_DATA_START_TIME_KEY);
		} else if (preference == mPreEndTime) {
			changeFreeTime(mPreEndTime, getString(R.string.set) + getString(R.string.end_time), PreferenceUtil.FREE_DATA_END_TIME_KEY);
		}
		return true;
	}
    
    @Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
    	Log.v(TAG, "changed>>" + newValue);
		if (preference == mPreFreeState) {
			boolean state = (Boolean) newValue;
			PreferenceUtil.putBoolean(DataPlanSetActivity.this, mSelectedSimIMSI, PreferenceUtil.FREE_DATA_STATE_KEY, state);
			if (state) {
				mFreeGroup.addPreference(mPreFreeTotal);
				mFreeGroup.addPreference(mPreStartTime);
				mFreeGroup.addPreference(mPreEndTime);
				//监听开关状态
				mHandler.postDelayed(mRn, 500);
			} else {
				mHandler.removeMessages(UPDATE_STATS_TAG);
		    	mFreeGroup.removePreference(mPreFreeTotal);
		    	mFreeGroup.removePreference(mPreStartTime);
		    	mFreeGroup.removePreference(mPreEndTime);
			}
		}
		return true;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		saveDataPlanSet();
	}

	@Override
	public void setSimStateChangeListener(int simState) {
		if (simState == SimStateReceiver.SIM_INVALID) {
			setPreferenceState(false);
		} else {
			setPreferenceState(true);
			initSimInfo();
		}
	}

	private void setPreferenceState(boolean state) {
		if (mPreDataplanTotal == null || mPreCloseDay == null || mPreFreeState == null
				|| mPreStartTime == null || mPreEndTime == null || mPreFreeTotal == null) {
			return;
		}
		mPreDataplanTotal.setEnabled(state);
		mPreCloseDay.setEnabled(state);
		mPreFreeState.setEnabled(false);
		mPreFreeState.setChecked(false);
		if (!state) {
			mPreDataplanTotal.setSubContent(getString(R.string.un_set));
			mPreCloseDay.setSubContent(getString(R.string.un_set));
			mFreeGroup.removePreference(mPreFreeTotal);
			mFreeGroup.removePreference(mPreStartTime);
			mFreeGroup.removePreference(mPreEndTime);
		} else {
			mFreeGroup.addPreference(mPreFreeTotal);
			mFreeGroup.addPreference(mPreStartTime);
			mFreeGroup.addPreference(mPreEndTime);
		}
		mPreStartTime.setEnabled(state);
		mPreEndTime.setEnabled(state);
	}
	/**
	 *  设置流量
	 * @param title
	 * @param preKey
     */
	private void showDataPlan(String title, final String preKey, final DataplanPreference preference ) {
		hb.app.dialog.AlertDialog.Builder builder = new Builder(this);
		builder.setTitle(title);
		View view = LayoutInflater.from(this).inflate(R.layout.lay_data_plan, null);
		final EditText etData = (EditText)view.findViewById(R.id.edt_dataplan);
		etData.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				//套餐流量大小 ,以KB为单位来存储
				boolean match = StringUtil.matchNumber(TextUtils.isEmpty(s) ? "" : s.toString());
				if (!match) {
					Toast.makeText(DataPlanSetActivity.this, R.string.input_number, Toast.LENGTH_SHORT).show();
				}
			}
		});
		builder.setView(view);
		builder.setPositiveButton(com.hb.R.string.ok, new hb.app.dialog.AlertDialog.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				//保存流量
				String data = etData.getText().toString();
				boolean match = StringUtil.matchNumber(data);
				if (TextUtils.isEmpty(data) || !match) {
					return;
				}
				if (!match) {
					Toast.makeText(DataPlanSetActivity.this, R.string.input_number, Toast.LENGTH_SHORT).show();
					return;
				}
				long totalData = Long.parseLong(data.toString()) * 1024;
				preference.setSubContent(StringUtil.formatDataFlowSize(DataPlanSetActivity.this, totalData));
				PreferenceUtil.putLong(DataPlanSetActivity.this, mSelectedSimIMSI, preKey, totalData);
				//监听手动更改状态
				mHandler.sendEmptyMessage(UPDATE_STATS_TAG);

			}
		});
		builder.setNegativeButton(com.hb.R.string.cancel, new hb.app.dialog.AlertDialog.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}

		});
		builder.create().show();
		ToolsUtil.showInputMethod(this);
	}


	private void showSelectDialog(Preference preference) {
		if (preference == mPreCloseDay) {
		    mCloseDay = PreferenceUtil.getInt(this, mSelectedSimIMSI, PreferenceUtil.CLOSEDAY_KEY, 1);
		    mClickIndex = mCloseDay - 1;
		    showDataType(getString(R.string.select) + getString(R.string.month_end_day), mDays);
	    }
  }
	
	/**
	 * 显示数据类型
	 */
	private void showDataType(String title, String[] valuesArray) {
		SpinnerPopupDialog spinnerPopupDialog = new SpinnerPopupDialog(this);
		spinnerPopupDialog.setNegativeButton(new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		spinnerPopupDialog.setSingleChoiceItems(valuesArray, mClickIndex,	 new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mClickIndex = which;
		        if (mClickPre == mPreCloseDay) {
					PreferenceUtil.putInt(DataPlanSetActivity.this, mSelectedSimIMSI, PreferenceUtil.CLOSEDAY_KEY, (which + 1));
					mPreCloseDay.setSubContent(mDays[which]);
				}
				dialog.dismiss();
			}
		});
		spinnerPopupDialog.setCanceledOnTouchOutside(true);
		spinnerPopupDialog.show();
		spinnerPopupDialog.setTitle(title);
	}

	/**
	 * 闲时时间点
	 * @param tv
	 * @param timeKey
	 */
	private void changeFreeTime(final DataplanPreference preference, String title, final String timeKey) {
		String time = PreferenceUtil.getString(DataPlanSetActivity.this, mSelectedSimIMSI, timeKey, getString(R.string.un_set));
		if (TextUtils.equals(time, getString(R.string.un_set))) {
			time = "00:00";
		}
		String[] split = time.split(":");
		TimePickerDialog timePickerDialog = new TimePickerDialog(this,new TimePickerDialog.OnTimeSetListener() {
			@Override
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				String time = (hourOfDay < 10 ?  "0" + hourOfDay : hourOfDay) + ":" + (minute < 10 ?  "0" + minute : minute);
				mSelectedTime = time;
				String tempTime = null;
				if (TextUtils.equals(timeKey, PreferenceUtil.FREE_DATA_START_TIME_KEY)) {
					tempTime = PreferenceUtil.getString(DataPlanSetActivity.this, mSelectedSimIMSI, PreferenceUtil.FREE_DATA_END_TIME_KEY, getString(R.string.un_set));
				} else if (TextUtils.equals(timeKey, PreferenceUtil.FREE_DATA_END_TIME_KEY)) {
					tempTime = PreferenceUtil.getString(DataPlanSetActivity.this, mSelectedSimIMSI, PreferenceUtil.FREE_DATA_START_TIME_KEY, getString(R.string.un_set));
				}
				if (TextUtils.equals(mSelectedTime, tempTime)) {
					Toast.makeText(DataPlanSetActivity.this, R.string.warn_free_time_info, Toast.LENGTH_SHORT).show();
					return;
				}
				preference.setSubContent(mSelectedTime);
				PreferenceUtil.putString(DataPlanSetActivity.this, mSelectedSimIMSI, timeKey, mSelectedTime);
				//监听手动更改状态
				mHandler.sendEmptyMessage(UPDATE_STATS_TAG);
			}
		},true);
		timePickerDialog.updateTime(Integer.parseInt(split[0]),Integer.parseInt(split[1]));
		timePickerDialog.setCanceledOnTouchOutside(true);
		timePickerDialog.show();
	}
	
	private boolean isSetDataPlan() {
		String province = PreferenceUtil.getString(this, mSelectedSimIMSI, PreferenceUtil.PROVINCE_KEY, getString(R.string.un_set));
		String city = PreferenceUtil.getString(this, mSelectedSimIMSI, PreferenceUtil.CITY_KEY, getString(R.string.un_set));
		String dataPlanType = PreferenceUtil.getString(this, mSelectedSimIMSI, PreferenceUtil.DATAPLAN_TYPE_KEY, getString(R.string.un_set));
    	String operator = PreferenceUtil.getString(DataPlanSetActivity.this, mSelectedSimIMSI, PreferenceUtil.OPERATOR_KEY, getString(R.string.un_set));
		long dataTotal = PreferenceUtil.getLong(DataPlanSetActivity.this, mSelectedSimIMSI, PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
		long freeTotal = PreferenceUtil.getLong(DataPlanSetActivity.this, mSelectedSimIMSI, PreferenceUtil.FREE_DATA_TOTAL_KEY, 0);
		boolean freeStaus = PreferenceUtil.getBoolean(this, mSelectedSimIMSI, PreferenceUtil.FREE_DATA_STATE_KEY, false);
		//sim卡基本信息设置
		if (province.equals(getString(R.string.un_set)) || city.equals(getString(R.string.un_set))
				||dataPlanType. equals(getString(R.string.un_set))) {
			return false;
		}

		if (dataTotal < 0) {
			return false;
		}
//		if (freeStaus && freeTotal < 0) {
//			return false;
//		}
		return true;
	}
	
	/**
	 * 保存sim卡套餐设置
	 */
	private void saveDataPlanSet() {
		long commonTotal = PreferenceUtil.getLong(DataPlanSetActivity.this, mSelectedSimIMSI, PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
		long commonUsed = PreferenceUtil.getLong(DataPlanSetActivity.this, mSelectedSimIMSI, PreferenceUtil.USED_DATAPLAN_COMMON_KEY, 0);
		boolean freeStaus = PreferenceUtil.getBoolean(this, mSelectedSimIMSI, PreferenceUtil.FREE_DATA_STATE_KEY, false);
		if (freeStaus) {
			long freeTotal = PreferenceUtil.getLong(DataPlanSetActivity.this, mSelectedSimIMSI, PreferenceUtil.FREE_DATA_TOTAL_KEY, 0);
			long freeUsed = PreferenceUtil.getLong(DataPlanSetActivity.this, mSelectedSimIMSI, PreferenceUtil.USED_FREE_DATA_TOTAL_KEY, 0);
			PreferenceUtil.putLong(DataPlanSetActivity.this, mSelectedSimIMSI, PreferenceUtil.REMAIN_FREE_DATA_TOTAL_KEY, freeTotal - freeUsed);
		}
	
		String activeSimImsi = ToolsUtil.getActiveSimImsi(this);
        if (commonTotal > 0 && TextUtils.equals(mSelectedSimIMSI, activeSimImsi)) {
        	NotificationUtil.clearNotify(this, NotificationUtil.TYPE_NORMAL);
        }
		//sim基本信息设置完整检查
		PreferenceUtil.putBoolean(this, mSelectedSimIMSI, PreferenceUtil.SIM_BASEINFO_KEY, isSetDataPlan());
		//未设置流量套餐时,提示最多弹出3次
		PreferenceUtil.putInt(this, mSelectedSimIMSI, PreferenceUtil.NOTIFY_UNDATA_COUNT_KEY, 0);
	}

}
