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

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import com.android.settings.R;

import hb.preference.PreferenceManager;
import hb.preference.SeekBarPreference;

/**
 * A slider preference that directly controls an audio stream volume (no dialog).
 */
public class ColorTuning extends SeekBarPreference
        implements PreferenceManager.OnActivityStopListener {
    private static final String TAG = "ColorTuning";

    private SeekBar mSeekBar;
    private String mKey;
	private ColorTuninger mColorTuninger;

    /**
     * Constructor for class.
     *
     * @param context
     *            The application context
     * @param attrs
     *            More attribute set config
     * @param defStyleAttr
     *            Default styly attribute
     * @param defStyleRes
     *            Default style resource
     */
    public ColorTuning(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_volume_slider_hb);
    }

    /**
     * Constructor for class.
     *
     * @param context
     *            The application context
     * @param attrs
     *            More attribute set config
     * @param defStyleAttr
     *            Default styly attribute
     */
    public ColorTuning(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    /**
     * Constructor for class.
     *
     * @param context
     *            The application context
     * @param attrs
     *            More attribute set config
     */
    public ColorTuning(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor for class.
     *
     * @param context
     *            The application context
     */
    public ColorTuning(Context context) {
        this(context, null);
    }


    @Override
    public void onActivityStop() {
//        if (mVolumizer != null) {
//            mVolumizer.stop();
//        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
		Log.d("zengtao", "onBindView");
		final SeekBar seekBar = (SeekBar) view.findViewById(com.android.internal.R.id.seekbar);
		if (seekBar == mSeekBar) {
            return;
        }
		mSeekBar = seekBar;
		if (mColorTuninger == null) {
			mColorTuninger = new ColorTuninger(getContext());
		}
		mColorTuninger.setSeekBar(mSeekBar);
    }

    @Override
    public void onProgressChanged(
            SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) {
            return;
        }
		Log.d("zengtao", "progress = "+progress);
		super.onProgressChanged(seekBar, progress, fromUser);
    }	
}
