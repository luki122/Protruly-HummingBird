/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.ResourceIcon;
import com.android.systemui.statusbar.policy.FlashlightController;

/** Quick settings tile: Control flashlight **/
public class FlashlightTile extends QSTile<QSTile.BooleanState> implements
        FlashlightController.FlashlightListener {
	/**hb tangjun mod begin*/
	/*
    private final AnimationIcon mEnable
            = new AnimationIcon(R.drawable.ic_signal_flashlight_enable_animation);
    private final AnimationIcon mDisable
            = new AnimationIcon(R.drawable.ic_signal_flashlight_disable_animation);
            */
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			//Log.d("tangjun222", "-- intent.getAction() = " + intent.getAction());
			if(Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
				mLevel = (int)(100f
						* intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
						/ intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
				if(mLevel <= 10) {
					mFlashlightController.setFlashlight(false);
				}
			} else if(Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
				mFlashlightController.setFlashlight(false);
			}
		}
	};
	private int mLevel;
    /**hb tangjun mod end*/
    private final FlashlightController mFlashlightController;

    public FlashlightTile(Host host) {
        super(host);
        mFlashlightController = host.getFlashlightController();
        mFlashlightController.addListener(this);
        
        /**hb tangjun mod begin*/
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Intent.ACTION_SHUTDOWN);
        mContext.registerReceiver(mBroadcastReceiver, filter);
        /**hb tangjun mod end*/
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        mFlashlightController.removeListener(this);
        /**hb tangjun mod begin*/
		mContext.unregisterReceiver(mBroadcastReceiver);
        /**hb tangjun mod end*/
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
    }

    @Override
    protected void handleClick() {
    	/**hb tangjun mod begin*/
    	if(mLevel <= 10) {
    		Toast.makeText(mContext, R.string.do_not_open_flashlight, Toast.LENGTH_SHORT).show();
    		return;
    	}
    	if(!mFlashlightController.isAvailable()) {
    		return;
    	}
    	/**hb tangjun mod end*/
        if (ActivityManager.isUserAMonkey()) {
            return;
        }
        MetricsLogger.action(mContext, getMetricsCategory(), !mState.value);
        boolean newState = !mState.value;
        refreshState(newState ? UserBoolean.USER_TRUE : UserBoolean.USER_FALSE);
        mFlashlightController.setFlashlight(newState);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
    	/**hb tangjun mod begin*/
        //state.visible = mFlashlightController.isAvailable();
    	state.visible = true;
    	/**hb tangjun mod end*/
        state.label = mHost.getContext().getString(R.string.quick_settings_flashlight_label);
        if (arg instanceof UserBoolean) {
            boolean value = ((UserBoolean) arg).value;
            if (value == state.value) {
                return;
            }
            state.value = value;
        } else {
            state.value = mFlashlightController.isEnabled();
        }
        /**hb tangjun mod begin*/
        /*
        final AnimationIcon icon = state.value ? mEnable : mDisable;
        icon.setAllowAnimation(arg instanceof UserBoolean && ((UserBoolean) arg).userInitiated);
        state.icon = icon;
        */
        state.icon = state.value ?ResourceIcon.get(R.drawable.hb_flashlight_on):ResourceIcon.get(R.drawable.hb_flashlight_off);
        if(!mFlashlightController.isAvailable()) {
        	state.icon = ResourceIcon.get(R.drawable.hb_flashlight_disable);
    	}
        state.color=state.value ?mContext.getResources().getColor(R.color.qs_title_color_on):mContext.getResources().getColor(R.color.qs_title_color_off);
        /**hb tangjun mod end*/
        int onOrOffId = state.value
                ? R.string.accessibility_quick_settings_flashlight_on
                : R.string.accessibility_quick_settings_flashlight_off;
        state.contentDescription = mContext.getString(onOrOffId);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_FLASHLIGHT;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_flashlight_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_flashlight_changed_off);
        }
    }

    @Override
    public void onFlashlightChanged(boolean enabled) {
        refreshState(enabled ? UserBoolean.BACKGROUND_TRUE : UserBoolean.BACKGROUND_FALSE);
    }

    @Override
    public void onFlashlightError() {
        refreshState(UserBoolean.BACKGROUND_FALSE);
    }

    @Override
    public void onFlashlightAvailabilityChanged(boolean available) {
        refreshState();
    }
    
    /**hb tangjun add begin*/
    public int getBatteryLevel() {
    	return mLevel;
    }
    /**hb tangjun add end*/
}
