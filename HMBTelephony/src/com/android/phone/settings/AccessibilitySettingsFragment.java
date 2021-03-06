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

package com.android.phone.settings;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import hb.preference.CheckBoxPreference;
import hb.preference.Preference;
import hb.preference.PreferenceFragment;
import hb.preference.PreferenceScreen;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.ims.ImsManager;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;
import com.android.phone.settings.TtyModeListPreference;

public class AccessibilitySettingsFragment extends PreferenceFragment {
    private static final String LOG_TAG = AccessibilitySettingsFragment.class.getSimpleName();
    private static final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    private static final String BUTTON_TTY_KEY = "button_tty_mode_key";
    private static final String BUTTON_HAC_KEY = "button_hac_key";

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        /**
         * Disable the TTY setting when in/out of a call (and if carrier doesn't
         * support VoLTE with TTY).
         * @see android.telephony.PhoneStateListener#onCallStateChanged(int,
         * java.lang.String)
         */
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (DBG) Log.d(LOG_TAG, "PhoneStateListener.onCallStateChanged: state=" + state);
            Preference pref = getPreferenceScreen().findPreference(BUTTON_TTY_KEY);
            if (pref != null) {
                pref.setEnabled(state == TelephonyManager.CALL_STATE_IDLE);
            }
        }
    };

    private Context mContext;
    private AudioManager mAudioManager;

    private TtyModeListPreference mButtonTty;
    private CheckBoxPreference mButtonHac;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity().getApplicationContext();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        addPreferencesFromResource(R.xml.accessibility_settings);

        mButtonTty = (TtyModeListPreference) findPreference(
                getResources().getString(R.string.tty_mode_key));
        mButtonHac = (CheckBoxPreference) findPreference(BUTTON_HAC_KEY);

        if (PhoneGlobals.getInstance().phoneMgr.isTtyModeSupported()) {
            mButtonTty.init();
        } else {
            getPreferenceScreen().removePreference(mButtonTty);
            mButtonTty = null;
        }

        if (PhoneGlobals.getInstance().phoneMgr.isHearingAidCompatibilitySupported()) {
            int hac = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.HEARING_AID, SettingsConstants.HAC_DISABLED);
            mButtonHac.setChecked(hac == SettingsConstants.HAC_ENABLED);
        } else {
            getPreferenceScreen().removePreference(mButtonHac);
            mButtonHac = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (ImsManager.isVolteEnabledByPlatform(mContext) && !getVolteTtySupported()) {
            TelephonyManager tm =
                    (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (ImsManager.isVolteEnabledByPlatform(mContext) && !getVolteTtySupported()) {
            TelephonyManager tm =
                    (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mButtonTty) {
            return true;
        } else if (preference == mButtonHac) {
            int hac = mButtonHac.isChecked()
                    ? SettingsConstants.HAC_ENABLED : SettingsConstants.HAC_DISABLED;
            // Update HAC value in Settings database.
            Settings.System.putInt(mContext.getContentResolver(), Settings.System.HEARING_AID, hac);

            // Update HAC Value in AudioManager.
            mAudioManager.setParameter(SettingsConstants.HAC_KEY,
                    hac == SettingsConstants.HAC_ENABLED
                            ? SettingsConstants.HAC_VAL_ON : SettingsConstants.HAC_VAL_OFF);
            return true;
        }
        return false;
    }

    private boolean getVolteTtySupported() {
        CarrierConfigManager configManager =
                (CarrierConfigManager) mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        return configManager.getConfig().getBoolean(
                CarrierConfigManager.KEY_CARRIER_VOLTE_TTY_SUPPORTED_BOOL);
    }
}
