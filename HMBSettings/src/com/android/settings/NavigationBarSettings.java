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
import com.android.settings.widget.NavigationbarPositionPerference;

import hb.preference.Preference;
import hb.preference.SwitchPreference;

public class NavigationBarSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener, NavigationbarPositionPerference.OnClickListener {
    private static final String KEY_HIDE_NAVIGATION_BAR = "hide_navigation_bar";
    private static final String KEY_NAVIGATION_BAR_POSITION1 = "navigation_bar_position1";
    private static final String KEY_NAVIGATION_BAR_POSITION2 = "navigation_bar_position2";
    private final H mHandler = new H();
    private final SettingsObserver mSettingsObserver = new SettingsObserver();
    private SwitchPreference mToggleHideNavigationBar;
    private NavigationbarPositionPerference mNavPositionPreference1;
    private NavigationbarPositionPerference mNavPositionPreference2;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DEVICEINFO;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.navigation_bar_settings);
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
        mToggleHideNavigationBar = (SwitchPreference) findPreference(KEY_HIDE_NAVIGATION_BAR);
        mToggleHideNavigationBar.setOnPreferenceChangeListener(this);

        mNavPositionPreference1 = (NavigationbarPositionPerference) findPreference(KEY_NAVIGATION_BAR_POSITION1);
        mNavPositionPreference2 = (NavigationbarPositionPerference) findPreference(KEY_NAVIGATION_BAR_POSITION2);
        mNavPositionPreference1.setNavigationPostionImage(R.drawable.ic_navation_bar_position1);
        mNavPositionPreference2.setNavigationPostionImage(R.drawable.ic_navation_bar_position2);
        mNavPositionPreference1.setOnClickListener(this);
        mNavPositionPreference2.setOnClickListener(this);
        updateRadioButtons(null);
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
        updateToggle(mToggleHideNavigationBar,
                LocalSettings.Secure.HIDE_NAVIGATION_BAR, LocalSettings.FLAG_DISABLED);

        updateRadioButtons(null);
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

            if (preference == mToggleHideNavigationBar) {
                updateSettings(LocalSettings.Secure.HIDE_NAVIGATION_BAR, enabled);
            }

            ((SwitchPreference) preference).setChecked(enabled);
        }

        return true;
    }

    @Override
    public void onRadioButtonClicked(NavigationbarPositionPerference emiter) {
        updateRadioButtons(emiter);

        boolean isSettingsEnabled = (emiter != mNavPositionPreference1);
        updateSettings(LocalSettings.Secure.NAVIGATION_KEY_POSITION, isSettingsEnabled);
    }

    private void updateRadioButtons(NavigationbarPositionPerference activated) {
        if (activated == null) {
            boolean isPosition1Enabled = !LocalSettings.isSettingsEnabled(
                    getContext(), LocalSettings.Secure.NAVIGATION_KEY_POSITION, LocalSettings.FLAG_DISABLED);
            activated = isPosition1Enabled ? mNavPositionPreference1 : mNavPositionPreference2;
        }
        if (activated == mNavPositionPreference1) {
            mNavPositionPreference1.setChecked(true);
            mNavPositionPreference2.setChecked(false);
        } else if (activated == mNavPositionPreference2) {
            mNavPositionPreference1.setChecked(false);
            mNavPositionPreference2.setChecked(true);
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri NAVIGATION_KEY_POSITION =
                Settings.Secure.getUriFor(LocalSettings.Secure.NAVIGATION_KEY_POSITION);
        private final Uri HIDE_NAVIGATION_BAR =
                Settings.Secure.getUriFor(LocalSettings.Secure.HIDE_NAVIGATION_BAR);


        public SettingsObserver() {
            super(mHandler);
        }

        public void register(boolean register) {
            final ContentResolver cr = getContentResolver();
            if (register) {
                cr.registerContentObserver(HIDE_NAVIGATION_BAR, false, this);
                cr.registerContentObserver(NAVIGATION_KEY_POSITION, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);

            if (HIDE_NAVIGATION_BAR.equals(uri)) {
                updateToggle(mToggleHideNavigationBar,
                        LocalSettings.Secure.HIDE_NAVIGATION_BAR, LocalSettings.FLAG_DISABLED);
            } else if (NAVIGATION_KEY_POSITION.equals(uri)) {
                updateRadioButtons(null);
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
