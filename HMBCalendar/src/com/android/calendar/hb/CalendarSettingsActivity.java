package com.android.calendar.hb;

import com.android.calendar.GeneralPreferences;
import com.android.calendar.R;
import com.android.calendar.Utils;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import hb.preference.ListPreference;
import hb.preference.Preference;
import hb.preference.PreferenceActivity;
import hb.preference.RingtonePreference;
import hb.preference.SwitchPreference;
import hb.preference.Preference.OnPreferenceChangeListener;

public class CalendarSettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener {

    private static final String RINGTONE_NOTIFICATION_DEFAULT = "content://settings/system/notification_sound";

    private ListPreference mWeekStart;
    private ListPreference mEventsReminder;
    private RingtonePreference mRingtone;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.hb_settings_preferences);

        mWeekStart = (ListPreference) findPreference(GeneralPreferences.KEY_WEEK_START_DAY);
        mEventsReminder = (ListPreference) findPreference(GeneralPreferences.KEY_DEFAULT_REMINDER);
        mRingtone = (RingtonePreference) findPreference(GeneralPreferences.KEY_ALERTS_RINGTONE);

        mWeekStart.setSummary(mWeekStart.getEntry());
        mEventsReminder.setSummary(mEventsReminder.getEntry());

        String ringToneUri = Utils.getRingTonePreference(this);
        Utils.setSharedPreference(this, GeneralPreferences.KEY_ALERTS_RINGTONE, ringToneUri);

        String ringtoneDisplayString = getRingtoneTitleFromUri(this, ringToneUri);
        mRingtone.setSummary(ringtoneDisplayString == null ? getString(R.string.ringtone_none) : ringtoneDisplayString);

        setPreferenceListeners(this);
    }

    private void setPreferenceListeners(OnPreferenceChangeListener listener) {
        mWeekStart.setOnPreferenceChangeListener(listener);
        mEventsReminder.setOnPreferenceChangeListener(listener);
        mRingtone.setOnPreferenceChangeListener(listener);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mWeekStart) {
            mWeekStart.setValue((String) newValue);
            mWeekStart.setSummary(mWeekStart.getEntry());
        } else if (preference == mEventsReminder) {
            mEventsReminder.setValue((String) newValue);
            mEventsReminder.setSummary(mEventsReminder.getEntry());
        } else if (preference == mRingtone) {
            if (newValue instanceof String) {
                Utils.setRingTonePreference(this, (String) newValue);
                String ringtone = getRingtoneTitleFromUri(this, (String) newValue);
                mRingtone.setSummary(ringtone == null ? getString(R.string.ringtone_none) : ringtone);
            }
            return true;
        } else {
            return true;
        }
        return false;
    }

    private String getRingtoneTitleFromUri(Context context, String uri) {
        if (TextUtils.isEmpty(uri)) {
            return null;
        }

        Ringtone ring = RingtoneManager.getRingtone(context, Uri.parse(uri));
        if (ring != null) {
            return ring.getTitle(context);
        }
        if (RINGTONE_NOTIFICATION_DEFAULT.equals(uri)) {
            return getString(R.string.ringtone_default_none);
        }
        return null;
    }

}
