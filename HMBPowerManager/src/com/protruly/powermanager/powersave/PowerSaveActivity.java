package com.protruly.powermanager.powersave;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;

import com.android.internal.os.PowerProfile;
import com.protruly.powermanager.R;
import com.protruly.powermanager.powersave.fuelgauge.PowerUsageSummaryActivity;
import com.protruly.powermanager.powersave.lowpowermode.LowPowerModeActivity;
import com.protruly.powermanager.utils.LogUtils;

import hb.preference.Preference;
import hb.preference.PreferenceActivity;
import hb.preference.PreferenceScreen;

import static com.protruly.powermanager.powersave.PowerSaveService.MODE_LOW_POWER;
import static com.protruly.powermanager.powersave.PowerSaveService.MODE_NORMAL_POWER;

public class PowerSaveActivity extends PreferenceActivity implements
        Preference.OnPreferenceClickListener {
    private static final String TAG = "PowerSaveActivity";

    private static final String KEY_AVAILABLE_POWER = "preference_available_power";
    private static final String KEY_POWER_DETAIL = "preference_power_detail";
    private static final String KEY_LOW_POWER_MODE = "preference_power_mode";
   // private static final String KEY_POWER_FORBIT_ALARMS = "preference_power_forbitalarms";

    private PreferenceScreen mPowerDetailPreference;
    private PreferenceScreen mPowerModePreference;
    // private PreferenceScreen mPowerForbitAlarmsPreference;
    private PowerSettingHeaderPreference mAvailablePowerPreference;

    private int mBatteryLevel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.activity_power_save);
        initView();
        initData();
    }

    private void initView() {
        mAvailablePowerPreference = (PowerSettingHeaderPreference) findPreference(KEY_AVAILABLE_POWER);
        mPowerDetailPreference = (PreferenceScreen) findPreference(KEY_POWER_DETAIL);
        mPowerDetailPreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
        mPowerDetailPreference.setOnPreferenceClickListener(this);
        mPowerModePreference = (PreferenceScreen) findPreference(KEY_LOW_POWER_MODE);
        mPowerModePreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
        mPowerModePreference.setOnPreferenceClickListener(this);
       // mPowerForbitAlarmsPreference = (PreferenceScreen) findPreference(KEY_POWER_FORBIT_ALARMS);
       // mPowerForbitAlarmsPreference.setLayoutResource(com.hb.R.layout.preference_material_hb);
       // mPowerForbitAlarmsPreference.setOnPreferenceClickListener(this);
    }

    private void initData() {
        ContentObserver obs = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                updatePowerModeStatus();
            }
        };
        getContentResolver().registerContentObserver(Settings.System
                .getUriFor(Settings.System.POWER_MODE), false, obs);
        updatePowerModeStatus();
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBatteryChangedReceiver, mIntentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePowerWave();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBatteryChangedReceiver);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (mPowerDetailPreference == preference) {
            startActivity(new Intent(PowerSaveActivity.this, PowerUsageSummaryActivity.class));
        } else if (mPowerModePreference == preference) {
            startActivity(new Intent(PowerSaveActivity.this, LowPowerModeActivity.class));
        } /*else if (mPowerForbitAlarmsPreference == preference) {
            startActivity(new Intent(PowerSaveActivity.this, ForbitAlarmsActivity.class));
        }*/
        return true;
    }

    private void updatePowerModeStatus() {
        int powerMode = Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MODE, MODE_NORMAL_POWER);
        LogUtils.d(TAG, "updatePowerModeStatus() -> mPowerMode = " + powerMode);
        if (powerMode == MODE_LOW_POWER) {
            mPowerModePreference.setStatus(getString(R.string.in_low_power_mode));
        } else {
            mPowerModePreference.setStatus(getString(R.string.not_low_power_mode));
        }
    }

    // FIXME: 17-5-9, RobinHE, the code is very shit!!!!!
    private static final int BATTERY_CAPACITY = 3260;
    private static final int MSG_UPDATE_POWER_WAVE = 0;

    private void updatePowerWave() {
        PowerProfile powerProfile = new PowerProfile(this);
        int powerMode = Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MODE, MODE_NORMAL_POWER);
        double averagePower;
        if (powerMode == MODE_LOW_POWER) {
            averagePower = 150;
        } else {
            averagePower = powerProfile.getAveragePower(PowerProfile.POWER_SCREEN_ON) - 20;
        }
        double availablePower = BATTERY_CAPACITY * mBatteryLevel / 100;
        double availableTime = availablePower / averagePower;
        int totalMinute = (int) (availableTime * 60);
        int hours = totalMinute / 60;
        int minutes = totalMinute % 60;
        String time;
        if (hours > 0) {
            time = getResources().getString(R.string.available_hours, hours, minutes);
        } else {
            time = getResources().getString(R.string.available_minutes, minutes);
        }
        mAvailablePowerPreference.update(time, mBatteryLevel);
    }

    private BroadcastReceiver mBatteryChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                mBatteryLevel = intent.getIntExtra("level", 0);
                LogUtils.d(TAG, "onReceive() -> mBatteryLevel = " + mBatteryLevel);
                mHandler.sendEmptyMessage(MSG_UPDATE_POWER_WAVE);
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_POWER_WAVE:
                    updatePowerWave();
                    break;
            }
            super.handleMessage(msg);
        }
    };
}
