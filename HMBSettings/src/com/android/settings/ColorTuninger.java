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

package com.android.settings;

import android.util.Log;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.content.Context;
import com.mediatek.miravision.setting.MiraVisionJni;
import com.mediatek.miravision.setting.MiraVisionJni.Range;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

public class ColorTuninger implements OnSeekBarChangeListener {
	
	private final Context mContext;
	private SeekBar mSeekBar;
	private int mLastProgress = -1;
	private int mMax;
	private int mMin;
	private Range mGammaRange;
	private final Receiver mReceiver = new Receiver();
	
    public ColorTuninger(Context context) {
		mContext = context;
		mGammaRange = MiraVisionJni.getGammaIndexRange();
		mMax = mGammaRange.max - mGammaRange.min;
		mReceiver.setListening(true);
	}
	
    public void setSeekBar(SeekBar seekBar) {
        if (mSeekBar != null) {
            mSeekBar.setOnSeekBarChangeListener(null);
        }
        mSeekBar = seekBar;
        mSeekBar.setOnSeekBarChangeListener(null);
        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(MiraVisionJni.getGammaIndex() - mGammaRange.min);
        mSeekBar.setOnSeekBarChangeListener(this);
		if (MiraVisionJni.nativeGetPictureMode() == MiraVisionJni.PIC_MODE_USER_DEF) {
			Log.d("zengtao", "user mode");
			mSeekBar.setEnabled(true);
		} else {
			Log.d("zengtao", "other mode");
			mSeekBar.setEnabled(false);
		}		
    }
	
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromTouch) {
        if (!fromTouch) {
            return;
        }

        postSetProgress(progress);
    }
	
    void postSetProgress(int progress) {
        // Do the volume changing separately to give responsive UI
        MiraVisionJni.setGammaIndex(progress - mGammaRange.min);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
	
	private final class Receiver extends BroadcastReceiver {
        private boolean mListening;

        public void setListening(boolean listening) {
            if (mListening == listening) {
                return;
            }
            mListening = listening;
            if (listening) {
                final IntentFilter filter = new IntentFilter("com.android.PIC_MODE_CHANGE_USER");
				filter.addAction("com.android.PIC_MODE_CHANGE_OTHER");
                mContext.registerReceiver(this, filter);
            } else {
                mContext.unregisterReceiver(this);
            }
        }
		
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.android.PIC_MODE_CHANGE_USER".equals(intent.getAction())) {
				if (mSeekBar != null) {
					Log.d("zengtao", "click");
					mSeekBar.setEnabled(true);
				}
			} else if ("com.android.PIC_MODE_CHANGE_OTHER".equals(intent.getAction())) {
				if (mSeekBar != null) {
					Log.d("zengtao", "no click");
					MiraVisionJni.setGammaIndex((mGammaRange.max - mGammaRange.min)/2);
					mSeekBar.setProgress(MiraVisionJni.getGammaIndex() - mGammaRange.min);
					mSeekBar.setEnabled(false);
				}				
			}
        }		
	}
	
    public void stop() {
        mSeekBar.setOnSeekBarChangeListener(null);
        mReceiver.setListening(false);
    }	
}