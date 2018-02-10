package com.hb.netmanage.activity;

import com.hb.netmanage.R;
import com.hb.netmanage.receiver.SimStateReceiver;
import com.hb.netmanage.utils.PreferenceUtil;
import com.hb.netmanage.utils.ToolsUtil;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import hb.preference.Preference;
import hb.preference.Preference.OnPreferenceClickListener;
import hb.preference.PreferenceScreen;
import hb.widget.toolbar.Toolbar;

/**
 * 流量设置界面
 * 
 * @author zhaolaichao
 */
public class DataSetActivity extends BasePreferenceActivity implements OnPreferenceClickListener {

	private PreferenceScreen mPreSim1;
	private PreferenceScreen mPreSim2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.activity_data_set);
		initView();
	}

	@Override
	public void setSimStateChangeListener(int simState) {
		if (simState == SimStateReceiver.SIM_INVALID) {
			setPreferenceState(false);
		} else {
			setPreferenceState(true);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		int currentState = ToolsUtil.getCurrentNetSimSubInfo(this);
		if (currentState == -1) {
			setPreferenceState(false);
		} else {
			setPreferenceState(true);
		}
		initView();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void addPreferencesFromResource(int preferencesResId) {
		super.addPreferencesFromResource(preferencesResId);
	}
	
	@SuppressWarnings("deprecation")
	private void initView() {
		Toolbar toolbar = getToolbar();
		toolbar.setTitle(getString(R.string.data_set));
		toolbar.setElevation(1);
		toolbar.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				DataSetActivity.this.finish();
			}
		});
		String sim1Imsi = PreferenceUtil.getString(this, PreferenceUtil.SIM_1, PreferenceUtil.IMSI_KEY, null);
		String sim2Imsi = PreferenceUtil.getString(this, PreferenceUtil.SIM_2, PreferenceUtil.IMSI_KEY, null);
		String operator1 =ToolsUtil.getSimOperator(this, sim1Imsi);
		String operator2 =ToolsUtil.getSimOperator(this, sim2Imsi);
		mPreSim1 = (PreferenceScreen) findPreference("preference_sim1");
		mPreSim1.setTitle(operator1 + "(" + getString(R.string.sim1) + ")");
		mPreSim1.setOnPreferenceClickListener(this);
		mPreSim2 = (PreferenceScreen) findPreference("preference_sim2");
		mPreSim2.setTitle(operator2 + "(" + getString(R.string.sim2) + ")");
		mPreSim2.setOnPreferenceClickListener(this);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String simTitle = null;
		Intent simIntent = null;
		 if (preference == mPreSim1) {
				// 获得当前选择的sim的imsi号
				simIntent = new Intent(DataSetActivity.this, SimDataSetActivity.class);
				simIntent.putExtra("CURRENT_INDEX", 0);
				startActivity(simIntent);
	      } else if (preference == mPreSim2) {
				// 获得当前选择的sim的imsi号
			  simIntent = new Intent(DataSetActivity.this, SimDataSetActivity.class);
			  simIntent.putExtra("SIM_TITLE", simTitle);
			  simIntent.putExtra("CURRENT_INDEX", 1);
			  startActivity(simIntent);
	      }
		return false;
	}

	private void setPreferenceState(boolean state) {
		if (mPreSim1 == null || mPreSim2 == null ) {
			return;
		}
		String sim1Imsi = PreferenceUtil.getString(this, PreferenceUtil.SIM_1, PreferenceUtil.IMSI_KEY, null);
		String sim2Imsi = PreferenceUtil.getString(this, PreferenceUtil.SIM_2, PreferenceUtil.IMSI_KEY, null);
		if(TextUtils.isEmpty(sim1Imsi)) {
			mPreSim1.setEnabled(false);
		} else {
			mPreSim1.setEnabled(state);
		}
		if(TextUtils.isEmpty(sim2Imsi)) {
			mPreSim2.setEnabled(false);
		} else {
			mPreSim2.setEnabled(state);
		}
	}

}
