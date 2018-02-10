package com.android.deskclock.alarms;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.app.hb.HBRingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.deskclock.AlarmClockFragment2;
import com.android.deskclock.AlarmUtils;
import com.android.deskclock.DeskClock;
import com.android.deskclock.LabelDialogFragment;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.provider.DaysOfWeek;
import com.android.deskclock.timer.TimerObj;

import java.util.Calendar;
import java.util.HashSet;

import hb.app.HbActivity;
import hb.app.dialog.AlertDialog;
import hb.widget.TimePicker;
import hb.widget.toolbar.Toolbar;

import android.content.ComponentName;

/**
 * Created by yubai on 17-4-22.
 */

public class EditClockActivity extends HbActivity {

    private static final int REQUEST_CODE_RINGTONE = 1;
    private static final int REQUEST_CODE_PERMISSIONS = 2;
    private static final int REQUEST_CODE_REPEAT = 3;

    private static final String KEY_ALARM = "key_alarm";
    private static final String PREF_KEY_DEFAULT_ALARM_RINGTONE_URI = "default_alarm_ringtone_uri";

    private TimePicker mTimePicker;

    private Alarm mAlarm;
//    private Alarm mTempAlarm;
    private DaysOfWeek mTempDaysOfWeek;
    private Uri mTempUri;

    private Toolbar mToolbar;

    private TextView mLabelText, mRingtoneText, mRepeatText;

    private boolean hasChangedRepeatTime = false, hasChangedRingtone = false, hasChangedLabel = false;


    public static void startActivity(Context context, Alarm alarm) {
        Intent intent = new Intent(context, EditClockActivity.class);
        intent.putExtra(KEY_ALARM, alarm);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHbContentView(R.layout.alarm_edit);
        initToolbar();
        initView();
    }
    
    private void initView() {
        RelativeLayout labelSettingLayout, ringtoneSettingLayout, repeatSettingLayout;
        labelSettingLayout = (RelativeLayout) findViewById(R.id.label_setting);
        mLabelText = (TextView) labelSettingLayout.findViewById(R.id.label_name);
        labelSettingLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 17-4-24
                showLabelDialog(mAlarm);
            }
        });
        ringtoneSettingLayout = (RelativeLayout) findViewById(R.id.ringtone_setting);
        mRingtoneText = (TextView) ringtoneSettingLayout.findViewById(R.id.ringtone_name);
        ringtoneSettingLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasChangedRingtone) {
                    mAlarm.alert = mTempUri;
                }
                launchRingTonePicker(mAlarm);
            }
        });
        repeatSettingLayout = (RelativeLayout) findViewById(R.id.repeat_setting);
        mRepeatText = (TextView) repeatSettingLayout.findViewById(R.id.repeat_content);
        repeatSettingLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasChangedRepeatTime) {
                    mAlarm.daysOfWeek = mTempDaysOfWeek;
                }

                RepeatSettingActivity.startActivityForResult(EditClockActivity.this, mAlarm, REQUEST_CODE_REPEAT);
            }
        });
        setTimePicker();
    }

    private boolean isNewAlarm = false;
    private void setTimePicker() {
        final int hour, minute;
        mTimePicker = (TimePicker) findViewById(R.id.time_picker);
        mTimePicker.setIs24HourView(DateFormat.is24HourFormat(this));
        mAlarm = getIntent().getParcelableExtra(KEY_ALARM);
        if (mAlarm == null) {
            final Calendar c = Calendar.getInstance();
            hour = c.get(Calendar.HOUR_OF_DAY);
            minute = c.get(Calendar.MINUTE);
            mAlarm = new Alarm(hour, minute);
            isNewAlarm = true;
        } else {
            hour = mAlarm.hour;
            minute = mAlarm.minutes;
            ((TextView)mToolbar.findViewById(R.id.title)).setText(getString(R.string.edit_alarm));
            if (mAlarm.label != null && !mAlarm.label.isEmpty()) {
                mLabelText.setText(mAlarm.label);
            }
            final String daysOfWeekStr =
                    mAlarm.daysOfWeek.toString(this, Utils.getFirstDayOfWeek(this));
            if (daysOfWeekStr != null && daysOfWeekStr.length() != 0) {
                mRepeatText.setText(daysOfWeekStr);
            }
        }

        if (mAlarm.alert == null) {
            mRingtoneText.setText(getString(R.string.alarm_no_ringtone));
        } else {
//            String title = getRingToneTitle(mAlarm.alert);
//            if (title.equals(getString(R.string.ringtone_unknown))) {
//                mAlarm.alert = Uri.parse(Alarm.DEFAULT_ALERT_URI);
//                title = getRingToneTitle(mAlarm.alert);
//                if (!isNewAlarm) {
//                    asyncUpdateAlarm(mAlarm, false);
//                }
//            }
//            mRingtoneText.setText(title);
            String title;
            if (HBRingtoneManager.isRingtoneExist(getApplicationContext(), mAlarm.alert) || mAlarm.alert.equals(Alarm.NO_RINGTONE_URI)) {
                title = getRingToneTitle(mAlarm.alert);
            } else {
//                mAlarm.alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                mAlarm.alert = HBRingtoneManager.getInternalDefaultRingtone(this, HBRingtoneManager.TYPE_ALARM);
                if (!HBRingtoneManager.isRingtoneExist(getApplicationContext(), mAlarm.alert)) {
                    mAlarm.alert = Alarm.NO_RINGTONE_URI;
                }
                title = getRingToneTitle(mAlarm.alert);
                if (!isNewAlarm) {
                    asyncUpdateAlarm(mAlarm, false);
                }
            }
            mRingtoneText.setText(title);
        }
        mTimePicker.setHour(hour);
        mTimePicker.setMinute(minute);
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.alarm_edit_toolbar);
        mToolbar.showBottomDivider(false);
        TextView leftButton = (TextView) mToolbar.findViewById(R.id.left_button);
        TextView rightButton = (TextView) mToolbar.findViewById(R.id.right_button);
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTimePicker.getHour() != mAlarm.hour
                        || mTimePicker.getMinute() != mAlarm.minutes
                        || hasChangedRingtone
                        || hasChangedRepeatTime
                        || hasChangedLabel) {
                    showConfirmExitDialog();
                } else {
                    finish();
                }
            }
        });

        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTempUri != null) {
                    saveRingtoneUri(mTempUri);
                }

                if (mTempDaysOfWeek != null) {
                    mAlarm.daysOfWeek = mTempDaysOfWeek;
                }

                if (mTempLabel != null) {
                    mAlarm.label = mTempLabel;
                }
                processTimeSet(mTimePicker.getHour(), mTimePicker.getMinute());
                finish();
            }
        });
    }

    /**
     * The callback interface used to indicate the user is done filling in
     * the time (e.g. they clicked on the 'OK' button).
     */
    public interface OnTimeSetListener {
        /**
         * Called when the user is done setting a new time and the dialog has
         * closed.
         *
         * @param view the view associated with this listener
         * @param hourOfDay the hour that was set
         * @param minute the minute that was set
         */
        void onTimeSet(hb.widget.TimePicker view, int hourOfDay, int minute);
    }


    protected void processTimeSet(int hourOfDay, int minute) {
        if (isNewAlarm) {
            // If mSelectedAlarm is null then we're creating a new alarm.
//            mAlarm.alert = getDefaultRingtoneUri();
//            if (mAlarm.alert == null) {
//                mAlarm.alert = Uri.parse("content://settings/system/alarm_alert");
//            }
            mAlarm.hour = hourOfDay;
            mAlarm.minutes = minute;
            mAlarm.enabled = true;

            asyncAddAlarm(mAlarm);
        } else {
            mAlarm.hour = hourOfDay;
            mAlarm.minutes = minute;
            mAlarm.enabled = true;
            asyncUpdateAlarm(mAlarm, false);
            mAlarm = null;
        }
    }

    protected void asyncUpdateAlarm(final Alarm alarm, final boolean popToast) {
        final Context context = getApplicationContext();
        final AsyncTask<Void, Void, AlarmInstance> updateTask =
                new AsyncTask<Void, Void, AlarmInstance>() {
                    @Override
                    protected AlarmInstance doInBackground(Void ... parameters) {
                        Events.sendAlarmEvent(R.string.action_update, R.string.label_deskclock);
                        ContentResolver cr = context.getContentResolver();

                        // Dismiss all old instances
                        AlarmStateManager.deleteAllInstances(context, alarm.id);
                        alarm.disabledYear = 0;
                        alarm.disabledMonth = 0;
                        alarm.disabledDay = 0;
                        alarm.notFiredNextTime = false;
                        // Update alarm
                        if(Alarm.updateAlarm(cr, alarm) == true) {
                            if (alarm.enabled) {
                                return setupAlarmInstance(context, alarm);
                            }
                        }

                        return null;
                    }

                    @Override
                    protected void onPostExecute(AlarmInstance instance) {
                        if (popToast && instance != null) {
                            AlarmUtils.popAlarmSetToast(context, instance.getAlarmTime().getTimeInMillis());
                        }
                    }
                };
        updateTask.execute();
    }

    private static AlarmInstance setupAlarmInstance(Context context, Alarm alarm) {
        ContentResolver cr = context.getContentResolver();
        AlarmInstance newInstance = alarm.createInstanceAfter(Calendar.getInstance());
        newInstance = AlarmInstance.addInstance(cr, newInstance);
        // Register instance to state manager
        AlarmStateManager.registerInstance(context, newInstance, true);
        return newInstance;
    }

    private Uri getDefaultRingtoneUri() {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        final String ringtoneUriString = sp.getString(PREF_KEY_DEFAULT_ALARM_RINGTONE_URI, null);

        final Uri ringtoneUri;
        if (ringtoneUriString != null) {
            ringtoneUri = Uri.parse(ringtoneUriString);
        } else {
            ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this,
                    RingtoneManager.TYPE_ALARM);
        }
        return ringtoneUri;
    }

    protected void asyncAddAlarm(final Alarm alarm) {
        final Context context = getApplicationContext();
        final AsyncTask<Void, Void, AlarmInstance> updateTask =
                new AsyncTask<Void, Void, AlarmInstance>() {
                    @Override
                    protected AlarmInstance doInBackground(Void... parameters) {
                        if (context != null && alarm != null) {
                            Events.sendAlarmEvent(R.string.action_create, R.string.label_deskclock);
                            ContentResolver cr = context.getContentResolver();

                            // Add alarm to db
                            Alarm newAlarm = Alarm.addAlarm(cr, alarm);

                            // Create and add instance to db
                            if (newAlarm.enabled) {
                                return setupAlarmInstance(context, newAlarm);
                            }
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(AlarmInstance instance) {
                        if (instance != null) {
//                            AlarmUtils.popAlarmSetToast(context, instance.getAlarmTime().getTimeInMillis());
                        }
                    }
                };
        updateTask.execute();
    }

    private void launchRingTonePicker(Alarm alarm) {
        Uri oldRingtone = Alarm.NO_RINGTONE_URI.equals(alarm.alert) ? null : alarm.alert;
//        final Intent intent = new Intent(this, ChooseRingtoneActivity.class);
        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, oldRingtone);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        intent.setComponent( new ComponentName("com.android.providers.media",
                "com.android.providers.media.HbRingtonePickerActivity"));

        startActivityForResult(intent, REQUEST_CODE_RINGTONE);
    }

    @Override
    public void onBackPressed() {
        if (mTimePicker.getHour() != mAlarm.hour
                || mTimePicker.getMinute() != mAlarm.minutes
                || hasChangedRingtone
                || hasChangedRepeatTime
                || hasChangedLabel) {
            showConfirmExitDialog();
        } else {
            finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_RINGTONE:
                    hasChangedRingtone = true;
//                    mTempUri = data.getData();
                    mTempUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (mTempUri == null) {
                        mTempUri = Alarm.NO_RINGTONE_URI;
                    }
//                    String title = getRingToneTitle(mTempUri);
//                    if (title.equals(getString(R.string.ringtone_unknown))) {
////                        mTempUri = Uri.parse(Alarm.DEFAULT_ALERT_URI);
//                        mTempUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
//                        title = getRingToneTitle(mTempUri);
//                    }

                    String title;
                    if (!HBRingtoneManager.isRingtoneExist(getApplicationContext(), mTempUri) && !mTempUri.equals(Alarm.NO_RINGTONE_URI)) {
                        mTempUri = HBRingtoneManager.getInternalDefaultRingtone(this, HBRingtoneManager.TYPE_ALARM);
                    }
                    title = getRingToneTitle(mTempUri);
                    mRingtoneText.setText(title);
                    break;
                case REQUEST_CODE_REPEAT:
                    Alarm alarm = data.getParcelableExtra(KEY_ALARM);
                    if (alarm.daysOfWeek.getBitSet() != mAlarm.daysOfWeek.getBitSet()) {
                        hasChangedRepeatTime = true;
                    }
                    final String daysOfWeekStr =
                            alarm.daysOfWeek.toString(this, Utils.getFirstDayOfWeek(this));
                    if (daysOfWeekStr != null) {
                        if (!daysOfWeekStr.isEmpty()) {
                            mRepeatText.setText(daysOfWeekStr);
                        } else {
                            mRepeatText.setText(R.string.ring_only_once);
                        }
                    }
                    mTempDaysOfWeek = alarm.daysOfWeek;
                    break;
                default:
                    LogUtils.w("Unhandled request code in onActivityResult: " + requestCode);
            }
        }
    }

    private void saveRingtoneUri(Uri uri) {
        /// M: if the alarm to change ringtone is null, then do nothing @{
        if (null == mAlarm) {
            LogUtils.w("saveRingtoneUri the alarm to change ringtone is null");
            return;
        }
        /// @}
        mAlarm.alert = uri;

        // Save the last selected ringtone as the default for new alarms
        // setDefaultRingtoneUri(uri);

        // If the user chose an external ringtone and has not yet granted the permission to read
        // external storage, ask them for that permission now.
        if (!AlarmUtils.hasPermissionToDisplayRingtoneTitle(this, uri)) {
            final String[] perms = {Manifest.permission.READ_EXTERNAL_STORAGE};
            requestPermissions(perms, REQUEST_CODE_PERMISSIONS);
        } else {
            /// M: Permissions already granted, save the ringtone
            setDefaultRingtoneUri(uri);
            asyncUpdateAlarm(mAlarm, false);
        }
    }

    private void setDefaultRingtoneUri(Uri uri) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (uri == null) {
            sp.edit().remove(PREF_KEY_DEFAULT_ALARM_RINGTONE_URI).apply();
        } else {
            sp.edit().putString(PREF_KEY_DEFAULT_ALARM_RINGTONE_URI, uri.toString()).apply();
        }
    }

    private EditText mLabelBox;
    private void showLabelDialog(final Alarm alarm) {
        mLabelBox = new EditText(this);
        mLabelBox.setMaxHeight(180);
        String label = (String)mLabelText.getText();
        if (label != null && !label.isEmpty()) {
            mLabelBox.setText(label);
        } else {
            mLabelBox.setText(alarm.label);
            label = alarm.label;
        }
        mLabelBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    return true;
                }
                return false;
            }
        });
        mLabelBox.setSelection(mLabelBox.getText().length());

        if (label != null && !label.isEmpty()) {
            mLabelBox.setSelectAllOnFocus(true);
        }

        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setView(mLabelBox)
                .setPositiveButton(R.string.time_picker_set, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        hasChangedLabel = true;
                        setLabel();
                        mLabelText.setText(mLabelBox.getText().toString());
                    }
                })
                .setNegativeButton(R.string.time_picker_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setMessage(R.string.label)
                .create();
        alertDialog.show();
        mLabelBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s == null || s.length() == 0 || s.toString().trim().length() == 0) {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                } else {
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    private void showConfirmExitDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setPositiveButton(R.string.time_picker_set, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton(R.string.time_picker_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setMessage(R.string.comfirm_set_canceling)
                .create();
        alertDialog.show();
    }


    private String mTempLabel;
    private void setLabel() {
        String label = mLabelBox.getText().toString();
        if (label.trim().length() == 0) {
            // Don't allow user to input label with only whitespace.
            label = "";
        }

        mTempLabel = label;
    }

    private String getRingToneTitle(Uri uri) {
        // Try the cache first
        String title = "";

        // If the user cannot read the ringtone file, insert our own name rather than the
        // ugly one returned by Ringtone.getTitle().
        if (!AlarmUtils.hasPermissionToDisplayRingtoneTitle(this, uri)) {
            title = getString(R.string.custom_ringtone);
        } else {
            // This is slow because a media player is created during Ringtone object creation.
            if (uri != null && uri.equals(Alarm.NO_RINGTONE_URI)) {
                title = getString(R.string.alarm_no_ringtone);
                return title;
            }

            final Ringtone ringTone = RingtoneManager.getRingtone(this, uri);
            if (ringTone == null) {
                LogUtils.i("No ringtone for uri %s", uri.toString());
                return null;
            }
            title = ringTone.getTitle(this);
        }
        return title;
    }

    @Override
    protected void onResume() {
        if (mTimePicker != null)
            mTimePicker.setIs24HourView(DateFormat.is24HourFormat(this));
        super.onResume();
    }
}
