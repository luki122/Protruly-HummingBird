/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.mediatek.settings.cdma;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.phone.PhoneGlobals;
import com.android.phone.SubscriptionInfoHelper;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import hb.preference.CheckBoxPreference;
import hb.preference.Preference;
import hb.preference.PreferenceActivity;
import hb.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.util.Log;
import android.view.MenuItem;
import com.android.phone.R;

//add by lgy
public class CdmaCallWaitSetting extends PreferenceActivity {
    private static final String LOG_TAG = "CdmaCallOptions";
    private final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    private static final String KEY_CALL_WAIT = "button_cw_key";
    public static final int CW_MODIFY_DIALOG = 1000;
    private Phone mPhone;
    private int mSubId;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.hb_call_wait_setting);   
        mSubId = getIntent().getIntExtra(
                SubscriptionInfoHelper.SUB_ID_EXTRA, SubscriptionInfoHelper.NO_SUB_ID);
        mPhone = PhoneGlobals.getInstance().getPhone(mSubId);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == preferenceScreen.findPreference(KEY_CALL_WAIT)) {
            showDialog(CdmaCallWaitOptions.CW_MODIFY_DIALOG);
            return true;
        }
        return false;
    }
    
    protected Dialog onCreateDialog(int dialogId) {
        /// M: remove CNIR and move CW option to cdma call option.
        if (dialogId == CdmaCallWaitOptions.CW_MODIFY_DIALOG) {
            return new CdmaCallWaitOptions(this, mPhone).createDialog();
        }

        return null;
    }


}
