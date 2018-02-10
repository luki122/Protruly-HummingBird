package com.android.provision;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.UserHandle;
import android.util.Log;

import com.android.internal.widget.LockPatternUtils;

import java.util.List;

/**
 * Created by xiaobin on 17-6-2.
 */

public class ShutdownReceiver extends BroadcastReceiver {

    private static final String TAG = "ShutdownReceiver";

    private LockPatternUtils mLockPatternUtils;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "ShutdownReceiver onReceive()");
        Log.i(TAG, "ShutdownReceiver intent action: " + intent.getAction());

        if (!Utils.getHasFinish(context)) {
            // 清除密码
            mLockPatternUtils = new LockPatternUtils(context);
            mLockPatternUtils.clearLock(UserHandle.myUserId());
            mLockPatternUtils.setLockScreenDisabled(false, UserHandle.myUserId());

            // 清除指纹
            FingerprintManager mFingerprintManager = (FingerprintManager) context.getSystemService(
                    Context.FINGERPRINT_SERVICE);
            final List<Fingerprint> items = mFingerprintManager.getEnrolledFingerprints();
            final int fingerprintCount = items.size();
            for (int i = 0; i < fingerprintCount; i++) {
                mFingerprintManager.remove(items.get(i), null);
            }
        }
    }

}


