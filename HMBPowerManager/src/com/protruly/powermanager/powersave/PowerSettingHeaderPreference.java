
package com.protruly.powermanager.powersave;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.protruly.powermanager.R;
import com.protruly.powermanager.powersave.view.WaveLoadingView;

import hb.preference.Preference;

public class PowerSettingHeaderPreference extends Preference {

    private int progress = 0;
    private String availableTime = "";

    private int mWaveColor;
    private int mBorderColor;

    private static final int LOW_POWER_TRIGGER_LEVEL = 10;

    public PowerSettingHeaderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_power_setting_header);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        view.setEnabled(false);
        TextView time = (TextView) view.findViewById(R.id.time);
        WaveLoadingView powerWave = (WaveLoadingView) view.findViewById(R.id.powerWave);
        time.setText(availableTime);
        powerWave.setProgressValue(progress);
        powerWave.setWaveColor(mWaveColor);
        powerWave.setBorderColor(mBorderColor);
    }

    public void update(String availablePower, int level) {
        availableTime = availablePower;
        progress = level;
        if (level > LOW_POWER_TRIGGER_LEVEL) {
            mWaveColor = Color.parseColor("#06C012");
            mBorderColor = Color.parseColor("#D7F5D9");
        } else {
            mWaveColor = Color.parseColor("#F45454");
            mBorderColor = Color.parseColor("#F7EADF");
        }
        notifyChanged();
    }
}