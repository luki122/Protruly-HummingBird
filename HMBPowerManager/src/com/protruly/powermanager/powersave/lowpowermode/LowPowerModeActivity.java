package com.protruly.powermanager.powersave.lowpowermode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.protruly.powermanager.R;
import com.protruly.powermanager.powersave.PowerSaveService;
import com.protruly.powermanager.utils.LogUtils;

import hb.app.HbActivity;

import static com.protruly.powermanager.powersave.PowerSaveService.MODE_LOW_POWER;
import static com.protruly.powermanager.powersave.PowerSaveService.MODE_NORMAL_POWER;


public class LowPowerModeActivity extends HbActivity implements View.OnClickListener {
    private static final String TAG = "LowPowerModeActivity";

    private Button btnMode;
    private ImageView imgBattery;

    private int mBatteryLevel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHbContentView(R.layout.activity_low_power_mode);
        initView();
        initData();
    }

    private void initView() {
        getToolbar().setTitle(R.string.low_power_mode);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        btnMode = (Button) findViewById(R.id.btn_mode);
        imgBattery = (ImageView) findViewById(R.id.img_battery);
        btnMode.setOnClickListener(this);
    }

    private void initData() {
        updateStatus();
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBatteryChangedReceiver, mIntentFilter);
    }

    private void updateStatus() {
        int currentMode = Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MODE, MODE_NORMAL_POWER);
        if (currentMode == MODE_LOW_POWER) {
            btnMode.setText(getString(R.string.exit_low_power_mode));
            btnMode.setBackgroundResource(R.drawable.low_power_mode_button_yellow);
            btnMode.setPadding(0, 0, 0, 0);
            imgBattery.setImageDrawable(getDrawable(R.drawable.battery_icon_yellow));
        } else if (mBatteryLevel <= 10) {
            btnMode.setText(getString(R.string.enter_low_power_mode));
            btnMode.setBackgroundResource(R.drawable.low_power_mode_button_red);
            btnMode.setPadding(0, 0, 0, 0);
            imgBattery.setImageDrawable(getDrawable(R.drawable.battery_icon_red));
        } else {
            btnMode.setText(getString(R.string.enter_low_power_mode));
            btnMode.setBackgroundResource(R.drawable.low_power_mode_button);
            btnMode.setPadding(0, 0, 0, 0);
            imgBattery.setImageDrawable(getDrawable(R.drawable.battery_icon));
        }
    }

    @Override
    public void onClick(View v) {
        if (v == btnMode) {
            int currentMode = Settings.System.getInt(getContentResolver(),
                    Settings.System.POWER_MODE, MODE_NORMAL_POWER);
            if (currentMode != MODE_LOW_POWER) {
                enterLowPowerSaveMode();
            } else {
                exitLowPowerSaveMode();
            }
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBatteryChangedReceiver);
    }

    private void enterLowPowerSaveMode() {
        int currentMode = Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MODE, MODE_NORMAL_POWER);
        if (currentMode == MODE_LOW_POWER) {
            return;
        }
        Intent serverIntent = new Intent(this, PowerSaveService.class);
        serverIntent.setAction(PowerSaveService.ACTION_SWITCH_POWER_MODE);
        serverIntent.putExtra("from", currentMode);
        serverIntent.putExtra("to", MODE_LOW_POWER);
        startService(serverIntent);
    }

    private void exitLowPowerSaveMode() {
        int currentMode = Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MODE, MODE_NORMAL_POWER);
        if (currentMode != MODE_LOW_POWER) {
            return;
        }
        Intent serverIntent = new Intent(this, PowerSaveService.class);
        serverIntent.setAction(PowerSaveService.ACTION_SWITCH_POWER_MODE);
        serverIntent.putExtra("from", MODE_LOW_POWER);
        serverIntent.putExtra("to", MODE_NORMAL_POWER);
        startService(serverIntent);
    }

    private BroadcastReceiver mBatteryChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                mBatteryLevel = intent.getIntExtra("level", 0);
                LogUtils.d(TAG, "onReceive() -> mBatteryLevel = " + mBatteryLevel);
                mHandler.sendEmptyMessage(MSG_UPDATE_UI);
            }
        }
    };

    private static final int MSG_UPDATE_UI = 0;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_UI:
                    updateStatus();
                    break;
            }
            super.handleMessage(msg);
        }
    };
}