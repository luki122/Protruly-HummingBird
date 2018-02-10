package com.hb.netmanage.fragement;


import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hb.netmanage.DataCorrect;
import com.hb.netmanage.DataManagerApplication;
import com.hb.netmanage.R;
import com.hb.netmanage.activity.MainActivity;
import com.hb.netmanage.activity.OperatorInfoActivity;
import com.hb.netmanage.activity.SimDataSetActivity;
import com.hb.netmanage.utils.LogUtil;
import com.hb.netmanage.utils.PreferenceUtil;
import com.hb.netmanage.utils.StringUtil;
import com.hb.netmanage.utils.ToolsUtil;
import com.hb.netmanage.view.CircleProgressView;
import com.mst.tms.NetInfoEntity;

import java.util.ArrayList;

/**
 * sim01卡设置
 * @author zhaolaichao
 *
 */
public class SimFragment extends Fragment implements View.OnClickListener{
	private final String TAG = "SimFragment";
	/**
	 * 流量校正
	 */
	public static final int BTN_CORRECT_TAG = 0;
	/**
	 * 正在校正
	 */
	public static final int BTN_CORRECTING_TAG = 1;

	private static DataManagerApplication mInstance;
	private static ArrayList<View> mViewContainer = new ArrayList<View>();
	private View mView;
	/**
	 * sim索引
	 */
	private TextView mTvSimIndex;
	/**
	 * 运营商
	 */
	private TextView mTvOperator;
	/**
	 * 今天已用
	 */
	private TextView mTvTodayUsed;
	/**
	 * 距月结日
	 */
	private TextView mTvMonthEndDay;
	/**
	 * 平均可用
	 */
	private TextView mTvMonthUse;
	private Button mBtnCorrect;
	private CircleProgressView mCircleProgressView;
	/**
	 * sim卡 卡槽位置为0
	 */
	private int mCurrentNetSimIndex = 0;
	private int mCheckedSimIndex = 0;
	/**
	 * sim卡1
	 */
	private final int SIM_1 = 0;
	/**
	 * sim卡2 
	 */
	private final int SIM_2 = 1;
	/**
	 *月结日
	 */
	private int mMonthEndDay = 0;
	private long mFreeData;
	private long mCommonData;
	/**
	 * 是否更新流量变化过程
	 */
	private boolean mIsUpdateWarn = true;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null) {
			return null;
		}
		mView = inflater.inflate(R.layout.fragment_net_current_sim, container, false);
		//sim卡状态发生改变
		mViewContainer.clear();
		mViewContainer.add(mView);
		mInstance = DataManagerApplication.getInstance();
		mCurrentNetSimIndex = ToolsUtil.getCurrentNetSimSubInfo(mInstance);
		mCheckedSimIndex = getArguments().getInt("CurrentSimIndex", mCheckedSimIndex);
		updateUISim(mCheckedSimIndex, mIsUpdateWarn);
		//在配置变化的时候将这个fragment保存下来
		//setRetainInstance(true);
		if (TextUtils.equals(mBtnCorrect.getText().toString(), mInstance.getString(R.string.correcting))) {
			//进行流量校正
			//DataCorrect.getInstance().startCorrect(mInstance, true, mCheckedSimIndex);
		}
		return mView;
	}
    
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}

	/**
	 * 设置警告状态
	 */
    private void setWarnState() {
		LogUtil.e(TAG, "setWarnState>>11111>");
    	if (TextUtils.isEmpty(DataManagerApplication.mImsiArray [0]) && TextUtils.isEmpty(DataManagerApplication.mImsiArray [1])) {
			updateCircleProgressView(CircleProgressView.COMMON_ARC_COLOR, CircleProgressView.TEXT_COLOR_DEFAULT, 0, 0, mInstance.getString(R.string.month_remain), 0, 0, 0, false);
			return;
    	}
    	String activeImsi = DataManagerApplication.mImsiArray[mCheckedSimIndex];
		long commonUsed = PreferenceUtil.getLong(mInstance, activeImsi, PreferenceUtil.USED_DATAPLAN_AFTER_COMMON_KEY, 0);
		long commonTotal = PreferenceUtil.getLong(mInstance, activeImsi, PreferenceUtil.DATAPLAN_COMMON_KEY, 0);
		long commonRemain = PreferenceUtil.getLong(mInstance, activeImsi, PreferenceUtil.REMAIN_DATAPLAN_AFTER_COMMON_KEY, 0);
		if (commonRemain > commonTotal) {
			//手动更改流量总量
			commonRemain = commonTotal - commonUsed;
		}
		boolean warnState = PreferenceUtil.getBoolean(mInstance, activeImsi, PreferenceUtil.PASS_WARNING_STATE_KEY, false);
		String warnRate = PreferenceUtil.getString(mInstance, activeImsi, PreferenceUtil.PASS_WARNING_VALUE_KEY, SimDataSetActivity.DEFAULT_RATE + "%");
		int rate = Integer.parseInt(warnRate.substring(0, warnRate.indexOf("%")));
		if (commonTotal > 0) {
			boolean state = false;
			int arcColor = 0;
			int textColor = 0;
			long remainData = 0;
			String info = null;
			float progress = 0f;
			if (commonUsed > commonTotal) {
				state = true;
				arcColor = textColor = CircleProgressView.OVER_ARC_COLOR;
				remainData = Math.abs(commonUsed - commonTotal);
				info = mInstance.getString(R.string.month_over);
				progress = 0f;
			} else {
				state = commonUsed * 100 / commonTotal >= rate;
				if (state && warnState) {
					//超过最大限制值
					arcColor = CircleProgressView.WARN_ARC_COLOR;
					textColor = CircleProgressView.TEXT_COLOR_DEFAULT;
				} else {
					arcColor = CircleProgressView.COMMON_ARC_COLOR;
					textColor = CircleProgressView.TEXT_COLOR_DEFAULT;
				}
				remainData = Math.abs(commonRemain);
				info = mInstance.getString(R.string.month_remain);
				progress = commonRemain * 100 / commonTotal;
			}
			if (!warnState) {
				//超额开关关闭则以流量总量为准
				rate = 100;
			}
			updateCircleProgressView(arcColor, textColor, commonTotal, remainData, info, rate, progress, commonUsed, warnState);
			LogUtil.v("SimFragment", "state>>" + state + ">>commonRemainData>>" + remainData + ">commonTotal>>>" + commonTotal);
			if (warnState) {
				//超额开关打开
				PreferenceUtil.putBoolean(mInstance, DataManagerApplication.mImsiArray [mCheckedSimIndex], PreferenceUtil.NOTIFY_WARN_MONTH_KEY, state);
			}
		} else {
			updateCircleProgressView(CircleProgressView.ARC_COLOR, CircleProgressView.TEXT_COLOR_DEFAULT, 0, 0, mInstance.getString(R.string.month_remain), 0, 0, 0, warnState);
		}
    }
    
	@Override
	public void onDestroyView() {
		if (getActivity() == null) {
			return;
		} else {
			super.onDestroyView();
		}
		mView = null;
		LogUtil.e(TAG, "onDestroyView>>>>>>");
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}

	/**
	 *
	 * @param arcColor
	 * @param textColor
	 * @param remainData
	 * @param text2
     * @param progress  剩余流量比
     */
	private void updateCircleProgressView(int arcColor, int textColor, long totalData, long remainData, String text2, float warnProgress, float progress, long usedData, boolean isWarnState) {
		if (mCircleProgressView == null) {
			mCircleProgressView = (CircleProgressView)mView.findViewById(R.id.view_circle_progress);
		}
		if (mCircleProgressView == null) {
			return;
		}
		String text1 = !TextUtils.isEmpty(mCircleProgressView.getmTxtHint1()) ? mCircleProgressView.getmTxtHint1() : StringUtil.formatDataFlowSize(mInstance, 0);
		String[] textArray = text1.split(StringUtil.DATA_DIVIDER_TAG);
		String[] remainArray = StringUtil.formatDataFlowSize(mInstance, remainData).split(StringUtil.DATA_DIVIDER_TAG);
		if (!textArray[0].startsWith("0") && TextUtils.equals(textArray[0], remainArray[0])) {
			if (!ToolsUtil.isNetworkAvailable(mInstance)) {
				return;
			}
		}
		mCircleProgressView.setmColor(arcColor);
		mCircleProgressView.setmTxtHint1(totalData, remainData);
		mCircleProgressView.setmTxtHint2(text2);
		mCircleProgressView.setmText1Color(textColor);
		mCircleProgressView.setmText2Color(textColor);
		mCircleProgressView.setmUsedData(usedData);
		mCircleProgressView.setmIsWarnState(isWarnState);
		mCircleProgressView.doAnimation(true, warnProgress, 100 - progress, 100 - progress);
	}

	private void initView() {
		mTvSimIndex = (TextView)mView.findViewById(R.id.tv_sim_index);
		mCircleProgressView = (CircleProgressView)mView.findViewById(R.id.view_circle_progress);
		mTvSimIndex.setText("" + (mCheckedSimIndex + 1) + ".");
		mBtnCorrect = (Button) mView.findViewById(R.id.btn_correct);
		mTvTodayUsed = (TextView) mView.findViewById(R.id.tv_today_used_count);
		mTvMonthEndDay = (TextView) mView.findViewById(R.id.tv_month_end_day);
		mTvMonthUse = (TextView) mView.findViewById(R.id.tv_month_use);
		LogUtil.e("SimFragment", ">>mCheckedSimIndex>>>" + mCheckedSimIndex + ">>>mCurrentNetSimIndex>>>>>" + mCurrentNetSimIndex);
		TextView mTvOperator = (TextView) mView.findViewById(R.id.tv_sim_operator);
		String operator = null;
		int btnTextTag = PreferenceUtil.getInt(mInstance, DataManagerApplication.mImsiArray[mCheckedSimIndex], PreferenceUtil.CORRECT_DATA_KEY, BTN_CORRECT_TAG);
		String btnText = btnTextTag == 0 ? mInstance.getString(R.string.data_correct) : mInstance.getString(R.string.correcting);
		mBtnCorrect.setText(btnText);
		if (mCurrentNetSimIndex == -1 && TextUtils.isEmpty(DataManagerApplication.mImsiArray[mCheckedSimIndex])) {
			operator = mInstance.getResources().getString(R.string.no_card) + mInstance.getResources().getString(R.string.sim);
			setBtnEnable(mBtnCorrect, false, false);
		} else {
			operator = ToolsUtil.getSimOperator(mInstance, DataManagerApplication.mImsiArray[mCheckedSimIndex]);
			if (TextUtils.equals(mBtnCorrect.getText().toString(), mInstance.getString(R.string.correcting))) {
				//正在校正 不允许点击
				setBtnEnable(mBtnCorrect, true, false);
			} else {
				setBtnEnable(mBtnCorrect, true, true);
			}
			mCurrentNetSimIndex = ToolsUtil.getCurrentNetSimSubInfo(mInstance);
			if (mCheckedSimIndex == mCurrentNetSimIndex) {
				mTvSimIndex.setTextColor(mInstance.getResources().getColor(R.color.color_correct, null));
				mTvOperator.setTextColor(mInstance.getResources().getColor(R.color.color_correct, null));
			} else {
				mTvSimIndex.setTextColor(mInstance.getResources().getColor(R.color.color_data_text, null));
				mTvOperator.setTextColor(mInstance.getResources().getColor(R.color.color_data_text, null));
			}
		}
		mTvOperator.setText(operator);
		mBtnCorrect.setOnClickListener(this);
		setWarnState();
		long usedDay = PreferenceUtil.getLong(mInstance, DataManagerApplication.mImsiArray[mCheckedSimIndex], PreferenceUtil.DAY_USED_STATS_KEY, 1);
		String totalData = StringUtil.formatDataFlowSize(mInstance, usedDay);
		mTvTodayUsed.setText(totalData);
		if (mCurrentNetSimIndex == -1) {
			mTvMonthEndDay.setText("--");
		} else {
			mMonthEndDay = PreferenceUtil.getInt(mInstance, DataManagerApplication.mImsiArray[mCheckedSimIndex], PreferenceUtil.CLOSEDAY_KEY, 1);
			String day = mInstance.getString(R.string.date);
			if (ToolsUtil.isEnglish(mInstance)) {
				if (StringUtil.getDaysToMonthEndDay(mMonthEndDay) > 1) {
					day = mInstance.getString(R.string.date);
				} else {
					day = mInstance.getString(R.string.day);
				}
			}
			mTvMonthEndDay.setText(StringUtil.getDaysToMonthEndDay(mMonthEndDay) + " " + day);
		}
		//超时更新
		cancelMsg();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_correct :
			correctDataPlan();
			break;
		}
	}

	private void correctDataPlan() {
		if(ToolsUtil.getAirPlanMode(mInstance)) {
			//获取当前飞行模式状态
			setBtnEnable(mBtnCorrect, false, false);
			return;
		}
		if (TextUtils.isEmpty(ToolsUtil.getNetWorkType(mInstance))){
			//非联网状态下不更新界面
			Toast.makeText(mInstance, mInstance.getString(R.string.err_correction_net), Toast.LENGTH_SHORT).show();
			return;
		}
		//流量校正   要区别sim卡1、sim卡2
		//根据当前选中的位置来区别要使用哪个sim卡进行流量校正
		//sim基本信息设置完整检查
		boolean simState = PreferenceUtil.getBoolean(mInstance, DataManagerApplication.mImsiArray[mCheckedSimIndex], PreferenceUtil.SIM_BASEINFO_KEY, false);
		if (!simState) {
			//引导用户设置套餐界面
			setDataPlanIndex(true);
			return;
		}
		setBtnEnable(mBtnCorrect, false, false);
		mBtnCorrect.setText(mInstance.getString(R.string.correcting));
		DataCorrect.getInstance().startCorrect(mInstance, true, mCheckedSimIndex);
		PreferenceUtil.putInt(mInstance, DataManagerApplication.mImsiArray[mCheckedSimIndex], PreferenceUtil.CORRECT_DATA_KEY, BTN_CORRECTING_TAG);
		//limitedCorrectTime(PAGE_SELECTED_INDEX);
		cancelMsg();
	}

	/**
	 * 超时更新
	 */
	private void cancelMsg() {
		if (TextUtils.equals(mBtnCorrect.getText().toString(), mInstance.getString(R.string.correcting))) {
			//校正超过2分钟提示失败
			Handler handler = MainActivity.mMainActivity.getmHandler();
			Message msg = handler.obtainMessage();
			if (MainActivity.FIRST_SIM_INDEX == mCheckedSimIndex) {
				msg.what = MainActivity.CORRECT_FIRST_LIMITE_MAXTIME_TAG;
			} else {
				msg.what = MainActivity.CORRECT_SECOND_LIMITE_MAXTIME_TAG;
			}
			msg.arg1 = mCheckedSimIndex;
			handler.sendMessageDelayed(msg, 2 * 60* 1000);
		}
	}
	/**
	 * 未设置套餐时点击“流量校正”后引导用户设置套餐
	 */
	private void setDataPlanIndex(boolean isCorrect) {
		Intent intent = null;
		//切换设置界面
		if (TextUtils.isEmpty(DataManagerApplication.mImsiArray [0]) && TextUtils.isEmpty(DataManagerApplication.mImsiArray [1])) {
			//没有卡时
			Toast.makeText(mInstance, R.string.insert_sim, Toast.LENGTH_SHORT).show();
			return;
		}
		intent = new Intent(mInstance, OperatorInfoActivity.class);
		intent.putExtra("CURRENT_INDEX", mCheckedSimIndex);
		if (null != getActivity()) {
			getActivity().startActivity(intent);
		}
	}

	/**
	 * 设置btn的状态
	 * @param btn
	 * @param enable
	 */
	private void setBtnEnable(Button btn, boolean enable, boolean clickable) {
		LogUtil.e(TAG, "setBtnEnable>>>>"+ enable);
		if (ToolsUtil.getAirPlanMode(mInstance)) {
			//飞行模式状态
			btn.setText(mInstance.getString(R.string.data_correct));
			btn.setBackground(mInstance.getDrawable(R.drawable.btn_correct_unclick));
			btn.setClickable(false);
			btn.setEnabled(false);
			return;
		}
		if (enable) {
			btn.setClickable(clickable);
			btn.setEnabled(true);
		} else {
			if (mCurrentNetSimIndex == -1) {
				btn.setBackground(mInstance.getDrawable(R.drawable.btn_correct_unclick));
			}
			btn.setEnabled(false);
			btn.setClickable(clickable);
		}
		if (mCurrentNetSimIndex >= 0) {
			if (clickable) {
				btn.setBackground(mInstance.getDrawable(R.drawable.btn_correct));
			} else {
				btn.setBackground(mInstance.getDrawable(R.drawable.btn_correcting));
			}
		}
	}
	/**
	 * 实时更新界面 每分钟
	 * @param updateIndex
	 * @param netInfoEntity
	 */
	public void updateTiming(int updateIndex, NetInfoEntity netInfoEntity) {
		if (null == mView && mViewContainer.size() > 0) {
			mView = mViewContainer.get(0);
		}
		if (mView != null) {
			mInstance = DataManagerApplication.getInstance();
			mCheckedSimIndex = updateIndex;
			setWarnState();
			setUpdate();
		}
	}

	/**
	 * 校正流量后更新UI界面
	 */
	public void updateUISim(int selectedSimIndex, boolean updateWarn) {
		LogUtil.e(TAG, "updateUISim>>>><<<<" + selectedSimIndex);
		if (null == mView && mViewContainer.size() > 0) {
			mView = mViewContainer.get(0);
		}
		mIsUpdateWarn = updateWarn;
		mCheckedSimIndex = selectedSimIndex;
		mInstance = DataManagerApplication.getInstance();
		mCurrentNetSimIndex = ToolsUtil.getCurrentNetSimSubInfo(mInstance);
		//监听手动更改状态
		DataManagerApplication.getInstance().getUpdateObserver().setUpdate();
		initView();
		setUpdate();
		if (mCurrentNetSimIndex == -1) {
			//校正按钮不可用
			setBtnEnable(mBtnCorrect, false, false);
			PreferenceUtil.putInt(mInstance, DataManagerApplication.mImsiArray[mCheckedSimIndex], PreferenceUtil.CORRECT_DATA_KEY, BTN_CORRECT_TAG);
		} else {
			if (TextUtils.equals(mBtnCorrect.getText().toString(), mInstance.getString(R.string.correcting))) {
				//正在校正 不允许点击
				setBtnEnable(mBtnCorrect, true, false);
			} else {
				setBtnEnable(mBtnCorrect, true, true);
			}
		}

	}

	/**
	 *  update view
	 */
	private void setUpdate() {
		usedForToday();
//		setWarnState();
		setMonthUsed();
	}
	/**
	 * 今天已用
	 */
	private void usedForToday() {
		if (null == mTvTodayUsed) {
			mTvTodayUsed = (TextView) mView.findViewById(R.id.tv_today_used_count);
		}
		long usedTotalForDay = PreferenceUtil.getLong(mInstance, DataManagerApplication.mImsiArray[mCheckedSimIndex], PreferenceUtil.DAY_USED_STATS_KEY, 0);
		String totalData = StringUtil.formatDataFlowSize(mInstance, usedTotalForDay);
		mTvTodayUsed.setText(totalData);
	}


	/**
	 * 本月已用
	 */
	private void setMonthUsed() {
		if (null == mTvMonthUse) {
			mTvMonthUse = (TextView) mView.findViewById(R.id.tv_month_use);
		}
		long commonUsed = PreferenceUtil.getLong(mInstance, DataManagerApplication.mImsiArray[mCheckedSimIndex], PreferenceUtil.USED_DATAPLAN_AFTER_COMMON_KEY, 0);
		mTvMonthUse.setText(StringUtil.formatDataFlowSize(mInstance, commonUsed));
	}
	
	/**
	 * 切换默认上网卡
	 * @param context
	 * @param message
	 */
//	protected void dialogInfo(Context context, String message) {
//	      AlertDialog.Builder builder = new Builder(context);
//	      builder.setMessage(message);
//	      builder.setTitle(context.getString(R.string.show_info));
//	      builder.setPositiveButton(context.getString(com.hb.R.string.ok), new OnClickListener() {
//	          @Override
//	          public void onClick(DialogInterface dialog, int which) {
//	        	  boolean state = ToolsUtil.changeNetSim(mInstance, mCheckedSimIndex);
//	  		      if (state) {
//	  		    	  MainActivity.PAGE_SELECTED_INDEX = mCheckedSimIndex;
//	  		    	   mLayChange.setVisibility(View.GONE);
//	  		    	   mTvCurrent.setText(getString(R.string.current_data_sim));
//	  				   mTvCurrent.setVisibility(View.VISIBLE);
//	  				   if (SIM_1 == mCheckedSimIndex) {
//	  					   mImvSim.setImageResource(R.drawable.ic_sim1);
//	  				   } else if (SIM_2 == mCheckedSimIndex) {
//	  					   mImvSim.setImageResource(R.drawable.ic_sim2);
//	  				   }
//	  		      }
//	              dialog.dismiss();
//	          }
//	      });
//	      builder.setNegativeButton(context.getString(com.hb.R.string.cancel), new OnClickListener() {
//	           @Override
//	           public void onClick(DialogInterface dialog, int which) {
//	                dialog.dismiss();
//	            }
//	      });
//	      builder.create().show();
//	 }
}