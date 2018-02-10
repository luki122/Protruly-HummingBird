package com.android.settings;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import static android.provider.Settings.Secure.DOZE_ENABLED;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.widget.LockPatternUtils;

import java.util.ArrayList;

import hb.preference.ListPreference;
import hb.preference.Preference;
import hb.preference.SwitchPreference;

/**
 * Created by liuqin on 17-3-31.
 *
 */
public class LockscreenSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    public static final String TAG = "LockscreenSettings";
    private static boolean IS_SHOW_SCREEN_TIME_OUT = false;

    private static final String KEY_OWNER_INFO_SETTINGS = "key_security_info_in_lockscreen";
    private static final String KEY_SCREEN_TIMEOUT = "key_security_screen_timeout";
    private static final String KEY_DOZE = "doze";
    private static final int MY_USER_ID = UserHandle.myUserId();
    /**
     * If there is no setting in the provider, use this.
     */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;
    private ListPreference mScreenTimeoutPreference;
    private Preference mOwnerInfoPref;
    private SwitchPreference mDozePreference;
    private LockPatternUtils mLockPatternUtils;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.SECURITY;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mLockPatternUtils = new LockPatternUtils(getActivity());
        addPreferencesFromResource(R.xml.lockscreen_settings);
        initPreference();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateOwnerInfo();
        updateState();
    }

    /**
     * Init preference
     *
     * @date Liuqin on 2017-04-05
     */
    private void initPreference() {
        mOwnerInfoPref = findPreference(KEY_OWNER_INFO_SETTINGS);
        if (mOwnerInfoPref != null) {
            mOwnerInfoPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    OwnerInfoSettings.show(LockscreenSettings.this);
                    return true;
                }
            });
        }


        if (IS_SHOW_SCREEN_TIME_OUT) {
            mScreenTimeoutPreference = (ListPreference) findPreference(KEY_SCREEN_TIMEOUT);
            final long currentTimeout = Settings.System.getLong(getContext().getContentResolver(), SCREEN_OFF_TIMEOUT,
                    FALLBACK_SCREEN_TIMEOUT_VALUE);
            mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
            mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
            disableUnusableTimeouts(mScreenTimeoutPreference);
            updateTimeoutPreferenceDescription(currentTimeout);
        } else {
            removePreference(KEY_SCREEN_TIMEOUT);
        }

        if (isDozeAvailable(getContext())) {
            mDozePreference = (SwitchPreference) findPreference(KEY_DOZE);
            mDozePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_DOZE);
        }
    }

    public void updateOwnerInfo() {
        if (mOwnerInfoPref != null) {
            mOwnerInfoPref.setSummary(mLockPatternUtils.isOwnerInfoEnabled(MY_USER_ID)
                    ? mLockPatternUtils.getOwnerInfo(MY_USER_ID)
                    : getString(R.string.owner_info_settings_summary));
        }
    }

    private void disableUnusableTimeouts(ListPreference screenTimeoutPreference) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) getActivity().getSystemService(
                        Context.DEVICE_POLICY_SERVICE);
        final long maxTimeout = dpm != null ? dpm.getMaximumTimeToLock(null) : 0;
        if (maxTimeout == 0) {
            return; // policy not enforced
        }
        final CharSequence[] entries = screenTimeoutPreference.getEntries();
        final CharSequence[] values = screenTimeoutPreference.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.parseLong(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            final int userPreference = Integer.parseInt(screenTimeoutPreference.getValue());
            screenTimeoutPreference.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            screenTimeoutPreference.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            if (userPreference <= maxTimeout) {
                screenTimeoutPreference.setValue(String.valueOf(userPreference));
            } else if (revisedValues.size() > 0
                    && Long.parseLong(revisedValues.get(revisedValues.size() - 1).toString())
                    == maxTimeout) {
                // If the last one happens to be the same as the max timeout, select that
                screenTimeoutPreference.setValue(String.valueOf(maxTimeout));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        screenTimeoutPreference.setEnabled(revisedEntries.size() > 0);
    }

    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        ListPreference preference = mScreenTimeoutPreference;
        String summary;
        if (currentTimeout < 0) {
            // Unsupported value
            summary = "";
        } else {
            final CharSequence[] entries = preference.getEntries();
            final CharSequence[] values = preference.getEntryValues();
            if (entries == null || entries.length == 0) {
                summary = "";
            } else {
                int best = 0;
                for (int i = 0; i < values.length; i++) {
                    long timeout = Long.parseLong(values[i].toString());
                    if (currentTimeout >= timeout) {
                        best = i;
                    }
                }
                summary = preference.getContext().getString(R.string.screen_timeout_summary,
                        entries[best]);
            }
        }
        preference.setSummary(summary);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        final String key = preference.getKey();
        if (KEY_SCREEN_TIMEOUT.equals(key)) {
            try {
                int value = Integer.parseInt((String) o);
                Settings.System.putInt(getContentResolver(), SCREEN_OFF_TIMEOUT, value);
                updateTimeoutPreferenceDescription(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        }
        if (preference == mDozePreference) {
            boolean value = (Boolean) o;
            Settings.Secure.putInt(getContentResolver(), DOZE_ENABLED, value ? 1 : 0);
        }
        return true;
    }

    private static boolean isDozeAvailable(Context context) {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get("debug.doze.component") : null;
        if (TextUtils.isEmpty(name)) {
            name = context.getResources().getString(
                    com.android.internal.R.string.config_dozeComponent);
        }
        return !TextUtils.isEmpty(name);
    }

    private void updateState() {
        // Update doze if it is available.
        if (mDozePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), DOZE_ENABLED, 1);
            mDozePreference.setChecked(value != 0);
        }
    }
}
