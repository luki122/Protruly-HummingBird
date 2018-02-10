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
 * limitations under the License.
 */

package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.util.Log;

import com.android.systemui.R;
import com.android.systemui.qs.HbMuteAndVibrateLinkage;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.HbScreenShotHelper;
import com.android.systemui.qs.QSTile;
import android.media.AudioManager;

/**
 * Author:tangjun
 * Function:Quick settings tile: ScreenShotTile
 */
/** Quick settings tile: ScreenShot mode **/
public class ScreenShotTile extends QSTile<QSTile.BooleanState> {

    private static final String TAG = "ScreenShotTile";
    private static final boolean DEBUG = true;
    private boolean mListening;
    private HbScreenShotHelper mHbScreenShotHelper;

    public ScreenShotTile(Host host) {
        super(host);
        mHbScreenShotHelper = new HbScreenShotHelper(mContext);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleClick() {
    	mHbScreenShotHelper.takeScreenShot();
    }
    
    /**hb tangjun add begin*/
    @Override
    public void handleLongClick() {
    	 Intent intent =  new Intent("com.android.settings.INTELLIGENT_CONTROL_SETTINGS");  
    	 intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	 mHost.startActivityDismissingKeyguard(intent);
    }
    /**hb tangjun add end*/

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.visible = true;
        state.label = mContext.getString(R.string.accessibility_quick_settings_screenshot);
        state.icon = ResourceIcon.get(R.drawable.hb_screenshots_off);
        state.color = mContext.getResources().getColor(R.color.qs_title_color_off);
        state.contentDescription =  mContext.getString(
        		R.string.accessibility_quick_settings_screenshot);
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_screenshot);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_screenshot);
        }
    }

    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
    }

	@Override
	public int getMetricsCategory() {
		// TODO Auto-generated method stub
		return 3;
	}
}
