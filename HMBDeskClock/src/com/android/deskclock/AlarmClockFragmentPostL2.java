package com.android.deskclock;

import android.app.TimePickerDialog;
import android.widget.TimePicker;

import com.android.deskclock.alarms.EditClockActivity;
import com.android.deskclock.provider.Alarm;

/**
 * AlarmClockFragment for L+ devices
 */
public class AlarmClockFragmentPostL2 extends AlarmClockFragment2 implements
        EditClockActivity.OnTimeSetListener {

    // Callback used by TimePickerDialog
//    @Override
//    public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
//        processTimeSet(hourOfDay, minute);
//    }


    @Override
    public void onTimeSet(hb.widget.TimePicker view, int hourOfDay, int minute) {
        processTimeSet(hourOfDay, minute);
    }

    @Override
    protected void setTimePickerListener() {
        // Do nothing
    }

    @Override
    protected void startCreatingAlarm() {
        // Set the "selected" alarm as null, and we'll create the new one when the timepicker
        // comes back.
        mSelectedAlarm = null;
        EditClockActivity.startActivity(getActivity(), null);
    }

    @Override
    protected void showTimeEditDialog(Alarm alarm) {
        EditClockActivity.startActivity(getActivity(), alarm);
    }
}
