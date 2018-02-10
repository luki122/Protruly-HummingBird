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
 * limitations under the License
 */

package com.android.settings.fingerprint;

import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.utils.GuideCompat;

/**
 * Activity which concludes fingerprint enrollment.
 */
public class FingerprintEnrollFinish extends FingerprintEnrollBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.fingerprint_enroll_finish);
        setTitle(getString(R.string.fingerprint_add_title));
        setHeaderText(R.string.security_settings_fingerprint_enroll_finish_title);
        Button addButton = (Button) findViewById(R.id.add_another_button);

        FingerprintManager fpm = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
        int enrolled = fpm.getEnrolledFingerprints().size();
        int max = getResources().getInteger(
                com.android.internal.R.integer.config_fingerprintMaxTemplatesPerUser);
        if (enrolled >= max) {
            /* Don't show "Add" button if too many fingerprints already added */
//            addButton.setVisibility(View.INVISIBLE);
        } else {
            addButton.setOnClickListener(this);
        }

        hideProgressBar();

        guideInit();
    }

    @Override
    protected void onNextButtonClick() {
        setResult(RESULT_FINISHED);
        finish();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.add_another_button) {
            final Intent intent = getEnrollingIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            startActivity(intent);
            finish();
        }
        super.onClick(v);
    }

    private void hideProgressBar() {
        View progressBar = findViewById(R.id.fingerprint_progress_bar);
        if (progressBar != null) {
            progressBar.setVisibility(View.INVISIBLE);
        }

//        View fingerView = findViewById(R.id.fingerprint_animator);
//        TypedValue outValue = new TypedValue();
//        getTheme().resolveAttribute(android.R.attr.colorAccent, outValue, true);
//        ColorStateList mTint = ColorStateList.valueOf(outValue.data);
//        fingerView.setBackgroundTintList(mTint);
    }

    private void guideInit() {
        if (GuideCompat.checkGuide(this)) {
            GuideCompat guideCompat = setGuideCompat(new GuideCompat(this));

//            getToolbar().setTitle(R.string.wizard_back);
            getToolbar().setTitle("");
            getToolbar().setNavigationIcon(null);
            setOnToolbarListener(new OnToolbarListener() {
                @Override
                public void onNavigationBackClick() {
                    getGuideCompat().clearLockscreen();
                    getGuideCompat().removeAllFp();
                    getGuideCompat().guideLaunchLockPattern();
                }
            });

            // hide original button
            findViewById(R.id.button_container).setVisibility(View.GONE);

            // add footer
            View footerView = getLayoutInflater().inflate(R.layout.guide_one_button, null);
            View nextBtn = footerView.findViewById(R.id.guide_positive_btn);
            nextBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getGuideCompat().guideReturn();
                }
            });
            ((ViewGroup)findViewById(R.id.setup_wizard_layout)).addView(footerView);
        }
    }
}
