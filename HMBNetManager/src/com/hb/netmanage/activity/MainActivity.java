package com.hb.netmanage.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.hb.netmanage.DataCorrect;
import com.hb.netmanage.DataManagerApplication;
import com.hb.netmanage.R;
import com.hb.netmanage.adapter.MainFragementAdater;
import com.hb.netmanage.entity.SmsStatus;
import com.hb.netmanage.fragement.SimFragment;
import com.hb.netmanage.receiver.SimStateReceiver;
import com.hb.netmanage.service.NetManagerService;
import com.hb.netmanage.utils.LogUtil;
import com.hb.netmanage.utils.PreferenceUtil;
import com.hb.netmanage.utils.ToolsUtil;
import com.mst.tms.NetInfoEntity;

import java.util.ArrayList;

import hb.widget.ViewPager;
import hb.widget.toolbar.Toolbar;
import hb.widget.toolbar.Toolbar.OnMenuItemClickListener;

/**
 * 流量管理主界面
 * @author zhaolaichao
 *
 */
public class MainActivity extends BaseActivity implements OnClickListener, ViewPager.OnPageChangeListener, OnMenuItemClickListener{
    private final static String TAG = "MainActivity";
	/**
	 * 当前选中的sim卡
	 */
	public static int PAGE_SELECTED_INDEX = 0;
	/**
	 * 第一张SIM
	 */
	public static final int FIRST_SIM_INDEX = 0;
	/**
	 * 第二张SIM
	 */
	public static final int SECOND_SIM_INDEX = 1;

	/**
	 * 今天所用流量统计
	 */
	private static final int STATS_DATA_TODAY = 10000;
	private static final int SELECT_SIM_TAG = 20000;
	/**
	 * SIM卡状态改变
	 */
	private static final int SIM_STATE_CHANGED = 3000;
	public static final int CORRECT_FIRST_LIMITE_MAXTIME_TAG = 4000;
	public static final int CORRECT_SECOND_LIMITE_MAXTIME_TAG = 4100;
	/**
	 * 切换查询频率
	 */
	public static final int LIMIT_MIN_TIME = 30 *1000;
	public static MainActivity mMainActivity;
	private ImageView mImvIndex1;
	private ImageView mImvIndex2;
	private ViewPager mViewPager;

	private SimFragment mSim01Fragment;
	private SimFragment mSim02Fragment;
	private MainFragementAdater mMainAdapter;
	private LinearLayout mLayImvIndex;
	private RelativeLayout mLayNetControl;
	/**
	 * 发送校正短信
	 */
	public static DataCorrect mDataCorrect;
	/**
	 * 当前上网卡的卡槽索引
	 */
	private int mCurrentNetSimIndex = -1;
	private ArrayList<Fragment> mFragements = new ArrayList<Fragment>();

	private boolean mSimChangeState;
	private boolean mUpdateSim;
	private boolean mIsToast;
	private long mChangeTime;
	private NetInfoEntity mNetInfoEntity;
	private Bundle mCorrectBundle;

	Handler mHandler = new Handler() {
	     public void handleMessage(android.os.Message msg) {
	        switch (msg.what) {
				case DataCorrect.SMS_SENT_OK_TAG:
					Toast.makeText(MainActivity.this, getString(R.string.send_info_ok), Toast.LENGTH_SHORT).show();
					break;
				case DataCorrect.SMS_SENT_FAIL_TAG:
					Toast.makeText(MainActivity.this, getString(R.string.send_info_ok), Toast.LENGTH_SHORT).show();
					break;
				case DataCorrect.MSG_TRAFFICT_NOTIFY :
					int simInsex = msg.arg1;
					//流量校正成功
					//boolean dateChange = PreferenceUtil.getBoolean(MainActivity.this, "", PreferenceUtil.DATE_CHANGE_KEY, false);
					mCorrectBundle = msg.getData();
					mIsToast = mCorrectBundle.getBoolean("SHOW_TOAST");
					if (mIsToast) {
						if (TextUtils.isEmpty(DataManagerApplication.mImsiArray[0]) || TextUtils.isEmpty(DataManagerApplication.mImsiArray[1])) {
							Toast.makeText(MainActivity.this, getString(R.string.single_data_correct_ok), Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(MainActivity.this, String.format(getString(R.string.data_correct_ok), "" + (msg.arg1 + 1)), Toast.LENGTH_SHORT).show();
						}
					}
					PreferenceUtil.putInt(MainActivity.this, DataManagerApplication.mImsiArray[msg.arg1], PreferenceUtil.CORRECT_DATA_KEY, SimFragment.BTN_CORRECT_TAG);
					PreferenceUtil.putBoolean(MainActivity.this, DataManagerApplication.mImsiArray[msg.arg1], PreferenceUtil.MAN_INPUT_CORRECT_KEY, false);
					if (simInsex == FIRST_SIM_INDEX && mSim01Fragment != null) {
						mSim01Fragment.updateUISim(simInsex, true);
					} else if (simInsex == SECOND_SIM_INDEX && mSim02Fragment != null) {
						mSim02Fragment.updateUISim(simInsex, true);
					}
					if (FIRST_SIM_INDEX == msg.arg1) {
						//超时处理
						mHandler.removeMessages(CORRECT_FIRST_LIMITE_MAXTIME_TAG);
					} else if (SECOND_SIM_INDEX == msg.arg1) {
						mHandler.removeMessages(CORRECT_SECOND_LIMITE_MAXTIME_TAG);
					}
					mHandler.removeMessages(msg.what);
					break;
				case DataCorrect.MSG_TRAFFICT_ERROR :
					simInsex = msg.arg1;
					mCorrectBundle = msg.getData();
					SmsStatus smsStatus = (SmsStatus)mCorrectBundle.getSerializable("RESULT_ERROR");
					mIsToast = mCorrectBundle.getBoolean("SHOW_TOAST");
					//流量校正失败
					//dateChange = PreferenceUtil.getBoolean(MainActivity.this, "", PreferenceUtil.DATE_CHANGE_KEY, false);
					PreferenceUtil.putBoolean(MainActivity.this, DataManagerApplication.mImsiArray[msg.arg1], PreferenceUtil.MAN_INPUT_CORRECT_KEY, false);
					if (mIsToast) {
						if (TextUtils.isEmpty(DataManagerApplication.mImsiArray[0]) || TextUtils.isEmpty(DataManagerApplication.mImsiArray[1])) {
							Toast.makeText(MainActivity.this, smsStatus.getStatus(), Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(MainActivity.this, String.format(smsStatus.getStatus(), "" + (msg.arg1 + 1)), Toast.LENGTH_SHORT).show();
						}
					}
					PreferenceUtil.putLong(MainActivity.this, DataManagerApplication.mImsiArray[msg.arg1], PreferenceUtil.SIM_SUBID_SMS_KEY, System.currentTimeMillis());
					PreferenceUtil.putString(MainActivity.this, DataManagerApplication.mImsiArray[msg.arg1], PreferenceUtil.SMS_BODY_KEY, null);
					PreferenceUtil.putInt(MainActivity.this, DataManagerApplication.mImsiArray[msg.arg1], PreferenceUtil.CORRECT_DATA_KEY, SimFragment.BTN_CORRECT_TAG);
					if (simInsex == FIRST_SIM_INDEX && mSim01Fragment != null) {
						mSim01Fragment.updateUISim(simInsex, false);
					} else if (simInsex == SECOND_SIM_INDEX && mSim02Fragment != null) {
						mSim02Fragment.updateUISim(simInsex, false);
					}
					if (FIRST_SIM_INDEX == msg.arg1) {
						//超时处理
						mHandler.removeMessages(CORRECT_FIRST_LIMITE_MAXTIME_TAG);
					} else if (SECOND_SIM_INDEX == msg.arg1) {
						mHandler.removeMessages(CORRECT_SECOND_LIMITE_MAXTIME_TAG);
					}
					mHandler.removeMessages(msg.what);
			        break;
	        case STATS_DATA_TODAY:
	        	    statsDataUpdate(PAGE_SELECTED_INDEX, (NetInfoEntity) msg.obj);
	        	    break;
			case SELECT_SIM_TAG:
					mViewPager.setCurrentItem(PAGE_SELECTED_INDEX, true);
				    break;
	        case CORRECT_FIRST_LIMITE_MAXTIME_TAG:
			case CORRECT_SECOND_LIMITE_MAXTIME_TAG:
				PreferenceUtil.putLong(MainActivity.this, DataManagerApplication.mImsiArray[msg.arg1], PreferenceUtil.SIM_SUBID_SMS_KEY, System.currentTimeMillis());
				PreferenceUtil.putString(MainActivity.this, DataManagerApplication.mImsiArray[msg.arg1], PreferenceUtil.SMS_BODY_KEY, null);
				PreferenceUtil.putInt(MainActivity.this, DataManagerApplication.mImsiArray[msg.arg1], PreferenceUtil.CORRECT_DATA_KEY, SimFragment.BTN_CORRECT_TAG);

				//超时处理
				if (TextUtils.isEmpty(DataManagerApplication.mImsiArray[0]) || TextUtils.isEmpty(DataManagerApplication.mImsiArray[1])) {
					Toast.makeText(MainActivity.this, getString(R.string.single_data_correct_error), Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(MainActivity.this, String.format(getString(R.string.data_correct_error), "" + (msg.arg1 + 1)), Toast.LENGTH_SHORT).show();
				}
				if (msg.arg1 == FIRST_SIM_INDEX && mSim01Fragment != null) {
					mSim01Fragment.updateUISim(msg.arg1, true);
				} else if (msg.arg1 == SECOND_SIM_INDEX && mSim02Fragment != null) {
					mSim02Fragment.updateUISim(msg.arg1, true);
				}
				mHandler.removeMessages(msg.what);
				break;
			case SIM_STATE_CHANGED:
				 changeSimState();
				 break;
	        default:
	        	break;
	        }
	 	 }
   	};

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			savedInstanceState = null;
		}
		super.onCreate(savedInstanceState);
		setHbContentView(R.layout.activity_main);
		mMainActivity = this;
		mCurrentNetSimIndex = ToolsUtil.getCurrentNetSimSubInfo(this.getBaseContext());
		LogUtil.e(TAG, "onCreate>>>mCurrentNetSimIndex>>>" + mCurrentNetSimIndex);
		mDataCorrect = DataCorrect.getInstance();
		mDataCorrect.initCorrect(this, mHandler);
		//初始化界面
		initToolBar();
		if (!mSimChangeState) {
			initView();
		}
		PAGE_SELECTED_INDEX = mCurrentNetSimIndex;
		//后台启动联网控制界面
		setBgStart(getIntent());
		registerUpdateUI();
		mViewPager.setCurrentItem(PAGE_SELECTED_INDEX, true);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		setBgStart(intent);
		LogUtil.e(TAG, "onNewIntent>>>>>" + mCurrentNetSimIndex);

	}

	@Override
	protected void onResume() {
		super.onResume();
		ToolsUtil.registerHomeKeyReceiver(this);
		if (mCurrentNetSimIndex == -1 || PAGE_SELECTED_INDEX == -1) {
			return;
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		mCurrentNetSimIndex = ToolsUtil.getCurrentNetSimSubInfo(MainActivity.this);
		if (mCurrentNetSimIndex == -1) {
			//卡已拔出
			mSimChangeState = true;
			PAGE_SELECTED_INDEX = mCurrentNetSimIndex;
			mHandler.sendEmptyMessage(SIM_STATE_CHANGED);
			return;
		}
		mUpdateSim = true;
		if (PAGE_SELECTED_INDEX == FIRST_SIM_INDEX) {
			if (mSim01Fragment != null) {
				mSim01Fragment.updateUISim(PAGE_SELECTED_INDEX, true);
			}
		} else if (PAGE_SELECTED_INDEX == SECOND_SIM_INDEX){
			if (mSim02Fragment != null) {
				mSim02Fragment.updateUISim(PAGE_SELECTED_INDEX, true);
			}
		}
	}
	@Override
	protected void onDestroy() {
		doDestory();
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public void setSimStateChangeListener(int simState) {
		LogUtil.e(TAG, "sim卡状态发生改变时更新UI>>>>" + simState);
		mSimChangeState = true;
		mChangeTime = System.currentTimeMillis();
		if (simState == SimStateReceiver.SIM_INVALID) {
			mCurrentNetSimIndex = ToolsUtil.getCurrentNetSimSubInfo(MainActivity.this);
		} else if (simState == SimStateReceiver.SIM_VALID){
			for (int i = 0; i < DataManagerApplication.mImsiArray.length; i ++) {
				PreferenceUtil.putInt(this, DataManagerApplication.mImsiArray[i], PreferenceUtil.CORRECT_DATA_KEY, SimFragment.BTN_CORRECT_TAG);
			}
			mCurrentNetSimIndex = ToolsUtil.getCurrentNetSimSubInfo(MainActivity.this);
		}
		PAGE_SELECTED_INDEX = mCurrentNetSimIndex;
		mHandler.sendEmptyMessage(SIM_STATE_CHANGED);
	}

	public Handler getmHandler() {
		return mHandler;
	}

	private void doDestory() {
        //移除流量校正监听
		mDataCorrect.destory(this);
		mViewPager.removeOnPageChangeListener(this);
		unRegisterUpdateUI();
		mHandler.removeMessages(STATS_DATA_TODAY);
		mHandler.removeMessages(SELECT_SIM_TAG);
		mHandler.removeMessages(CORRECT_FIRST_LIMITE_MAXTIME_TAG);
		mHandler.removeMessages(CORRECT_SECOND_LIMITE_MAXTIME_TAG);
		mHandler.removeMessages(FIRST_SIM_INDEX);
		//mSim01Fragment = mSim02Fragment = null;
	}

	private void registerUpdateUI() {
		IntentFilter filter = new IntentFilter(NetManagerService.UPDATE_STATS);
		registerReceiver(mUIReceiver, filter);
	}

	private void unRegisterUpdateUI() {
		if (null != mUIReceiver) {
			unregisterReceiver(mUIReceiver);
		}
	}

	/**
	 * 后台启动联网控制界面
	 * @param intent
     */
	private void setBgStart(Intent intent) {
		boolean bgState = intent.getBooleanExtra("BG_STATE", false);
		if (bgState) {
			String simTitle = getString(R.string.net_control);
			Intent startIntent = new Intent(MainActivity.this, DataRangeActivity.class);
			startIntent.putExtra("SIM_TITLE", simTitle);
			startIntent.putExtra("SIM_COUNT", true);
			startIntent.putExtra("BG_STATE", bgState);
			startActivity(startIntent);
			LogUtil.e(TAG, "bgState>>" + bgState);
		}
	}
	/**
	 * 每分钟更新流量使用情况
	 */
	private BroadcastReceiver mUIReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (TextUtils.equals(intent.getAction(), NetManagerService.UPDATE_STATS)) {
				String simImsi = intent.getStringExtra("active_imsi");
				mNetInfoEntity = intent.getParcelableExtra("netinfo");
				if (mNetInfoEntity == null) {
					return;
				}
				if (TextUtils.isEmpty(simImsi)) {
					return;
				}
				int updateIndex = 0;
				for (int i = 0; i < DataManagerApplication.mImsiArray.length; i++) {
					if (TextUtils.equals(simImsi,DataManagerApplication.mImsiArray[i])) {
						updateIndex = i;
						break;
					}
				}
				statsDataUpdate(updateIndex, mNetInfoEntity);
			}
		}
	};


	private void changeSimState() {
		if (mSimChangeState && mFragements != null) {
			mFragements.clear();
			mViewPager.removeAllViewsInLayout();//removeAllViews();//赋值之前先将Adapter中的
			mLayImvIndex.setVisibility(View.GONE);
			mViewPager.removeAllViews();
			mViewPager.removeOnPageChangeListener(this);
			mSimChangeState = false;
			initView();
		}

	}
    private void initToolBar() {
		Toolbar toolbar = getToolbar();
		toolbar.inflateMenu(R.menu.main_set);
		toolbar.setOnMenuItemClickListener(this);
		toolbar.setTitle(R.string.app_title);
		toolbar.setElevation(1);
		toolbar.setNavigationIcon(com.hb.R.drawable.ic_toolbar_back);
		toolbar.setNavigationOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MainActivity.this.finish();
			}
		});
		mLayNetControl = (RelativeLayout) findViewById(R.id.lay_net_control);
		mLayImvIndex = (LinearLayout) findViewById(R.id.lay_imv_index);
		mImvIndex1 = (ImageView) findViewById(R.id.imv_index_01);
		mImvIndex2 = (ImageView) findViewById(R.id.imv_index_02);
		mViewPager = (ViewPager) findViewById(R.id.view_pager_main);

		mLayNetControl.setOnClickListener(this);
    }

	private void initView() {
		Bundle bundle = null;
		mFragements.clear();
		//获得FragmentManager
		FragmentManager fm = getFragmentManager();
		LogUtil.e(TAG, "mCurrentNetSimIndex>>>>" + mCurrentNetSimIndex);
		if (mCurrentNetSimIndex == -1 || (TextUtils.isEmpty(DataManagerApplication.mImsiArray [0]) && TextUtils.isEmpty(DataManagerApplication.mImsiArray [1]))) {
			PAGE_SELECTED_INDEX = 0;
			bundle = new Bundle();
			bundle.putInt("CurrentSimIndex", FIRST_SIM_INDEX);
			if (mSim01Fragment == null) {
				mSim01Fragment = new SimFragment();
				mSim01Fragment.setArguments(bundle);
			} else {
				mSim01Fragment.getArguments().clear();
				mSim01Fragment.getArguments().putAll(bundle);
			}
			mFragements.add(mSim01Fragment);
		} else if (!TextUtils.isEmpty(DataManagerApplication.mImsiArray [0]) && !TextUtils.isEmpty(DataManagerApplication.mImsiArray [1])) {
			//有两张卡,当插入两张SIM卡才显示此指示器
			bundle = new Bundle();
			bundle.putInt("CurrentSimIndex", FIRST_SIM_INDEX);
			if (mSim01Fragment == null) {
				mSim01Fragment = new SimFragment();
				mSim01Fragment.setArguments(bundle);
			} else {
				mSim01Fragment.getArguments().clear();
				mSim01Fragment.getArguments().putAll(bundle);
			}
			mFragements.add(mSim01Fragment);
			bundle = new Bundle();
			bundle.putInt("CurrentSimIndex", SECOND_SIM_INDEX);
			if (mSim02Fragment == null) {
				mSim02Fragment = new SimFragment();
				mSim02Fragment.setArguments(bundle);
			} else {
				mSim02Fragment.getArguments().clear();
				mSim02Fragment.getArguments().putAll(bundle);
			}
			mFragements.add(mSim02Fragment);
		} else if (TextUtils.isEmpty(DataManagerApplication.mImsiArray [0]) && !TextUtils.isEmpty(DataManagerApplication.mImsiArray [1])) {
			//只有一张卡: 不在卡1
			bundle = new Bundle();
			bundle.putInt("CurrentSimIndex", SECOND_SIM_INDEX);
			if (mSim02Fragment == null) {
				mSim02Fragment = new SimFragment();
				mSim02Fragment.setArguments(bundle);
			} else {
				mSim02Fragment.getArguments().clear();
				mSim02Fragment.getArguments().putAll(bundle);
			}
			mFragements.add(mSim02Fragment);
		} else if (TextUtils.isEmpty(DataManagerApplication.mImsiArray [1]) && !TextUtils.isEmpty(DataManagerApplication.mImsiArray [0])) {
			//只有一张卡: 不在卡2
			bundle = new Bundle();
			bundle.putInt("CurrentSimIndex", FIRST_SIM_INDEX);
			if (mSim01Fragment == null) {
				mSim01Fragment = new SimFragment();
				mSim01Fragment.setArguments(bundle);
			} else {
				mSim01Fragment.getArguments().clear();
				mSim01Fragment.getArguments().putAll(bundle);
			}
			mFragements.add(mSim01Fragment);
		}

		if (mMainAdapter == null) {
			mMainAdapter = new MainFragementAdater(this, fm);
			mMainAdapter.setmFragments(mFragements);
			mViewPager.setAdapter(mMainAdapter);
		} else {
			mMainAdapter.setmFragments(mFragements);
			mMainAdapter.notifyDataSetChanged();
		}
		mViewPager.setOnPageChangeListener(this);
		if (mFragements.size() > 1) {
			mLayImvIndex.setVisibility(View.VISIBLE);
		} else {
			mLayImvIndex.setVisibility(View.GONE);
		}
	}


	@Override
	public void onClick(View v) {
        switch (v.getId()) {
			case R.id.lay_net_control:
				String simTitle = getString(R.string.net_control);
				Intent intent = new Intent(MainActivity.this, DataRangeActivity.class);
				intent.putExtra("SIM_TITLE", simTitle);
				intent.putExtra("SIM_COUNT", true);
				startActivity(intent);
			break;
		}
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
	}

	@Override
	public void onPageSelected(int selected) {
		if (mCurrentNetSimIndex == -1 || selected >= DataManagerApplication.mImsiArray.length) {
			return;
		}
		//当前选中项
		PAGE_SELECTED_INDEX = selected;
		if (selected == 0) {
			mImvIndex1.setImageResource(R.drawable.index_rect_corner_selector);
			mImvIndex2.setImageResource(R.drawable.index_rect_corner);
		} else {
			mImvIndex1.setImageResource(R.drawable.index_rect_corner);
			mImvIndex2.setImageResource(R.drawable.index_rect_corner_selector);
		}
		//更新频率
		long time = System.currentTimeMillis();
		if (System.currentTimeMillis() - mChangeTime > LIMIT_MIN_TIME || mUpdateSim) {
			statsDataUpdate(PAGE_SELECTED_INDEX, mNetInfoEntity);
		}
		mChangeTime = time;
		mUpdateSim = false;
	}

	@Override
	public boolean onMenuItemClick(MenuItem menuItem) {
		if (ToolsUtil.getAirPlanMode(this)) {
			Toast.makeText(MainActivity.this, R.string.off_airplane_mode, Toast.LENGTH_SHORT).show();
			return false;
		}
		//切换设置界面
		int itemId = menuItem.getItemId();
		LogUtil.e(TAG, "onMenuItemClick>>" + menuItem.getTitle());
	    if (itemId == R.id.main_menu_info) {
        	//切换设置界面
        	 setDataPlanIndex(false);
         }
		return false;
	}

	/**
	 * 未设置套餐时点击“流量校正”后引导用户设置套餐
	 */
	private void setDataPlanIndex(boolean isCorrect) {
		Intent intent = null;
		//切换设置界面
     	if (TextUtils.isEmpty(DataManagerApplication.mImsiArray [0]) && TextUtils.isEmpty(DataManagerApplication.mImsiArray [1])) {
     		//没有卡时
     		Toast.makeText(MainActivity.this, R.string.insert_sim, Toast.LENGTH_SHORT).show();
     		return;
     	}

        if (!TextUtils.isEmpty(DataManagerApplication.mImsiArray [0]) && !TextUtils.isEmpty(DataManagerApplication.mImsiArray [1])) {
        	if (isCorrect) {
        		intent = new Intent(MainActivity.this, DataPlanSetActivity.class);
        		intent.putExtra("CURRENT_INDEX", PAGE_SELECTED_INDEX);
        	} else {
        		intent = new Intent(MainActivity.this, DataSetActivity.class);
        	}
        	startActivity(intent);
        } else {
            //当前只有一张sim卡
            if (isCorrect) {
            	intent = new Intent(MainActivity.this, DataPlanSetActivity.class);
            	intent.putExtra("CURRENT_INDEX", PAGE_SELECTED_INDEX);
            } else {
            	String simTitle = getString(R.string.sim_set);
            	//获得当前选择的sim的imsi号
            	intent = new Intent(MainActivity.this, SimDataSetActivity.class);
            	intent.putExtra("SIM_TITLE", simTitle);
            	intent.putExtra("CURRENT_INDEX", PAGE_SELECTED_INDEX);
            }
            startActivity(intent);
     	}
	}

	/**
	 * 流量统计界面更新
	 * @param usedTotalForDay 今天已用
	 */
    private void statsDataUpdate(int simIndex, NetInfoEntity netInfoEntity) {
    	if (simIndex == FIRST_SIM_INDEX && mSim01Fragment != null) {
    		mSim01Fragment.updateTiming(simIndex, netInfoEntity);
		} else if (simIndex == SECOND_SIM_INDEX && mSim02Fragment != null){
			mSim02Fragment.updateTiming(simIndex, netInfoEntity);
		}
    }

}