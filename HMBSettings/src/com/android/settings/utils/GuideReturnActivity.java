package com.android.settings.utils;

import android.content.Intent;
import android.os.Bundle;

import com.android.settings.FullActivityBase;
import com.android.settings.fingerprint.FingerprintEnrollFinish;

/**
 * Created by liuqin on 17-6-30.
 *
 * @date Liuqin on 2017-06-30
 */
public class GuideReturnActivity extends FullActivityBase {
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (GuideCompat.checkGuide(this)) {
            GuideCompat guideCompat = new GuideCompat(this);
            if (guideCompat.getFpCount() > 0) {
                Intent intent = new Intent(this, FingerprintEnrollFinish.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                guideCompat.guideSetIntent(intent);
                startActivity(intent);
            } else {
                if (guideCompat.guideHasScreenLock()) {
                    guideCompat.clearLockscreen();
                    guideCompat.removeAllFp();
                }
                guideCompat.guideLaunchLockPattern();
            }
        }
        finish();
    }
}
