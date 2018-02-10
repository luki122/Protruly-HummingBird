/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.view.Menu;
import android.view.MenuInflater;

import com.android.settings.utils.GuideCompat;

import hb.preference.PreferenceFragment;

public abstract class FullPreferenceFragmentBase extends PreferenceFragment {
    private GuideCompat mGuideCompat;

    public GuideCompat getGuideCompat() {
        return mGuideCompat;
    }

    public GuideCompat setGuideCompat(GuideCompat mGuideCompat) {
        this.mGuideCompat = mGuideCompat;
        return this.mGuideCompat;
    }

    public boolean isGuide() {
        return mGuideCompat != null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (isGuide()) {
            mGuideCompat.guideAddOptionMenu(menu, inflater);
        }
    }
}
