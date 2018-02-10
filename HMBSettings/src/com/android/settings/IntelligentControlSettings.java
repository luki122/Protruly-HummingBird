package com.android.settings;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;

import com.android.internal.logging.MetricsLogger;

import hb.preference.Preference;
import hb.preference.SwitchPreference;

public class IntelligentControlSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private static final String KEY_THREE_FINGER_CAPTURE = "three_finger_capture";
    private static final String KEY_HIDE_NAVIGATION_BAR = "hide_navigation_bar";
    private final H mHandler = new H();
    private final SettingsObserver mSettingsObserver = new SettingsObserver();
    private SwitchPreference mToggleThreeFingerCapture;
    private SwitchPreference mToggleHideNavigationBar;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEVICEINFO;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.intelligent_control);
        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAllToggle();
        mSettingsObserver.register(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSettingsObserver.register(false);
    }

    private void init() {
        mToggleThreeFingerCapture = (SwitchPreference) findPreference(KEY_THREE_FINGER_CAPTURE);
        mToggleThreeFingerCapture.setOnPreferenceChangeListener(this);

        mToggleHideNavigationBar = (SwitchPreference) findPreference(KEY_HIDE_NAVIGATION_BAR);
        mToggleHideNavigationBar.setOnPreferenceChangeListener(this);
        removePreference(KEY_HIDE_NAVIGATION_BAR);
    }

    /**
     * Is settings enabled
     *
     * @param key the key
     * @param def the def
     * @return the boolean
     * @date Liuqin on 2017-03-23
     */
    private boolean isSettingsEnabled(String key, int def) {
        return Settings.Secure.getInt(getContentResolver(), key, def) != 0;
    }

    /**
     * Update toggle
     *
     * @param preference the preference
     * @param key        the key
     * @param def        the def
     * @date Liuqin on 2017-03-23
     */
    private void updateToggle(SwitchPreference preference, String key, int def) {
        preference.setChecked(isSettingsEnabled(key, def));
    }

    /**
     * Update all toggle
     *
     * @date Liuqin on 2017-03-23
     */
    private void updateAllToggle() {
        updateToggle(mToggleThreeFingerCapture,
                LocalSettings.Secure.THREE_FINGER_CAPTURE, LocalSettings.FLAG_ENABLED);

        updateToggle(mToggleHideNavigationBar,
                LocalSettings.Secure.HIDE_NAVIGATION_BAR, LocalSettings.FLAG_DISABLED);
    }

    /**
     * Update settings
     *
     * @param key     the key
     * @param enabled the enabled
     * @date Liuqin on 2017-03-23
     */
    private void updateSettings(String key, boolean enabled) {
        Settings.Secure.putInt(getContentResolver(), key,
                enabled ? LocalSettings.FLAG_ENABLED : LocalSettings.FLAG_DISABLED);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof SwitchPreference) {
            Boolean enabled = (Boolean) newValue;

            if (preference == mToggleThreeFingerCapture) {
                updateSettings(LocalSettings.Secure.THREE_FINGER_CAPTURE, enabled);
            } else if (preference == mToggleHideNavigationBar) {
                updateSettings(LocalSettings.Secure.HIDE_NAVIGATION_BAR, enabled);
            }

            ((SwitchPreference) preference).setChecked(enabled);
        }

        return true;
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri THREE_FINGER_CAPTURE =
                Settings.Secure.getUriFor(LocalSettings.Secure.THREE_FINGER_CAPTURE);
        private final Uri HIDE_NAVIGATION_BAR =
                Settings.Secure.getUriFor(LocalSettings.Secure.HIDE_NAVIGATION_BAR);


        public SettingsObserver() {
            super(mHandler);
        }

        public void register(boolean register) {
            final ContentResolver cr = getContentResolver();
            if (register) {
                cr.registerContentObserver(THREE_FINGER_CAPTURE, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);

            if (THREE_FINGER_CAPTURE.equals(uri)) {
                updateToggle(mToggleThreeFingerCapture,
                        LocalSettings.Secure.THREE_FINGER_CAPTURE, LocalSettings.FLAG_ENABLED);
            } else if (HIDE_NAVIGATION_BAR.equals(uri)) {
                updateToggle(mToggleHideNavigationBar,
                        LocalSettings.Secure.HIDE_NAVIGATION_BAR, LocalSettings.FLAG_DISABLED);
            }
        }
    }

    private final class H extends Handler {
        private H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
        }
    }
}
