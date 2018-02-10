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

package com.android.settings.brightness;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.android.settings.R;

import hb.preference.SeekBarPreference;

public class BrightnessSeekBarPreference extends SeekBarPreference {
    private static final String TAG = "BrightnessSeekBarPreference";

    private SeekBar mSeekBar;
    private ImageView mIconView;
    private boolean mTracking;
    private BrightnessController mBrightnessController;

    public BrightnessSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr,
                                       int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_volume_slider_hb);
    }

    public BrightnessSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BrightnessSeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BrightnessSeekBarPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mSeekBar = (SeekBar) view.findViewById(com.android.internal.R.id.seekbar);
        mIconView = (ImageView) view.findViewById(com.android.internal.R.id.icon);
        init();
    }

    public void onResume() {
        if (mBrightnessController != null) {
            mBrightnessController.registerCallbacks();
            mSeekBar.setOnSeekBarChangeListener(this);
        }
    }

    public void onPause() {
        if (mBrightnessController != null) {
            mBrightnessController.unregisterCallbacks();
        }
        if (mSeekBar != null) {
            mSeekBar.setOnSeekBarChangeListener(null);
        }
    }

    private void init() {
        if (mSeekBar == null) return;
        if (!isEnabled()) {
            mSeekBar.setEnabled(false);
        }
        mIconView.setImageResource(R.drawable.ic_display_icon);
        mIconView.setVisibility(View.VISIBLE);

        if (mBrightnessController != null) {
            mBrightnessController.unregisterCallbacks();
        }
        mBrightnessController = new BrightnessController(getContext(), mIconView, mSeekBar);
        mBrightnessController.registerCallbacks();
        mSeekBar.setOnSeekBarChangeListener(this);
    }

//    public void setIcon(int resId) {
//        if (mIconView != null) {
//            mIconView.setImageResource(resId);
//        }
//    }

    // during initialization, this preference is the SeekBar listener
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        super.onProgressChanged(seekBar, progress, fromTouch);

        if (mBrightnessController != null && fromTouch) {
            mBrightnessController.onChanged(mSeekBar, mTracking, false, progress, false);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        super.onStartTrackingTouch(seekBar);

        mTracking = true;
        if (mBrightnessController != null) {
            mBrightnessController.onChanged(mSeekBar, mTracking, false, seekBar.getProgress(), false);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        super.onStopTrackingTouch(seekBar);

        mTracking = false;
        if (mBrightnessController != null) {
            mBrightnessController.onChanged(mSeekBar, mTracking, false, seekBar.getProgress(), true);
        }
    }
}
