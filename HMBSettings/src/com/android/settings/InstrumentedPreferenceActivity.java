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

import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import hb.preference.PreferenceActivity;
import hb.widget.toolbar.Toolbar;

import com.android.internal.logging.MetricsLogger;

/**
 * Instrumented activity that logs visibility state.
 */
public abstract class InstrumentedPreferenceActivity extends PreferenceActivity {
    /**
     * Declare the view of this category.
     *
     * Categories are defined in {@link com.android.internal.logging.MetricsLogger}
     * or if there is no relevant existing category you may define one in
     * {@link com.android.settings.InstrumentedFragment}.
     */
    protected abstract int getMetricsCategory();

    @Override
    protected void onResume() {
        LocalSettings.disableScreenRotate(this);
        super.onResume();
        MetricsLogger.visible(this, getMetricsCategory());
    }

    @Override
    protected void onPause() {
        super.onPause();
        MetricsLogger.hidden(this, getMetricsCategory());
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        initToolbar();
    }

    private void initToolbar() {
        try{
            Toolbar toolbar = getToolbar();
            if (toolbar != null) {
                toolbar.setTitle(getTitle());
                setActionBar(toolbar);
                toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onNavigationClicked(view);
                    }
                });
                toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        return InstrumentedPreferenceActivity.this.onOptionsItemSelected(menuItem);
                    }
                });
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void onNavigationClicked(View view){
        finish();
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (isChangingConfigurations() || isFinishing()) {
            return false;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }
}
