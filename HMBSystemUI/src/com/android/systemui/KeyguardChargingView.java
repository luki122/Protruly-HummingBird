package com.android.systemui;

import com.android.systemui.statusbar.policy.BatteryController;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * @author wxue
 */
public class KeyguardChargingView extends FrameLayout {
	private TextView mBatteryLevel;
	private KeyguardBatteryView mBatteryView;
	private View mScrimView;
	private BatteryController mBatteryController;
	
	public KeyguardChargingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mBatteryLevel = (TextView)findViewById(R.id.lock_battery_level);
		mBatteryView = (KeyguardBatteryView)findViewById(R.id.lock_battery);
		mScrimView = findViewById(R.id.lock_battery_scrim);
		mBatteryView.setBatteryLevelTextView(mBatteryLevel);
	}
	
	public void setBatteryController(BatteryController batteryController) {
        mBatteryController = batteryController;
        mBatteryView.setBatteryController(mBatteryController);
    }
	
	public void setScrimAlpha(float alpha){
		mScrimView.setAlpha(alpha);
	}
}
