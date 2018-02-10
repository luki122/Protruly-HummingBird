package com.android.deskclock.timer2;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.deskclock.R;

import java.io.Serializable;
import java.util.Arrays;

import hb.widget.NumberPicker;

public class TimerSetupView2 extends FrameLayout {

    private final int[] mTimeValue = new int[3];
    private NumberPicker mHourPicker;
    private NumberPicker mMinutePicker;
    private NumberPicker mSecondPicker;
    private TextView mCreate;

    private int colorGreen, colorGrayUnable;

    private NumberPicker.OnValueChangeListener mOnValueChangeListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker numberPicker, int oldVal, int newVal) {
            mTimeValue[0] = mHourPicker.getValue();
            mTimeValue[1] = mMinutePicker.getValue();
            mTimeValue[2] = mSecondPicker.getValue();
            updateStartButton();
        }
    };

    public TimerSetupView2(Context context) {
        this(context, null /* attrs */);
    }

    public TimerSetupView2(Context context, AttributeSet attrs) {
        super(context, attrs);

        colorGreen = getResources().getColor(R.color.timer_green_text_color, null);
        colorGrayUnable = getResources().getColor(R.color.timer_gray_unable_text_color, null);

        LayoutInflater.from(context).inflate(R.layout.time_setup_view3, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mHourPicker = (NumberPicker) findViewById(R.id.hour_picker);
        mHourPicker.setMinValue(0);
        mHourPicker.setMaxValue(23);
        mHourPicker.setWrapSelectorWheel(false);
        mHourPicker.setOnValueChangedListener(mOnValueChangeListener);

        mMinutePicker = (NumberPicker) findViewById(R.id.minute_picker);
        mMinutePicker.setMinValue(0);
        mMinutePicker.setMaxValue(59);
        mMinutePicker.setOnValueChangedListener(mOnValueChangeListener);

        mSecondPicker = (NumberPicker) findViewById(R.id.secend_picker);
        mSecondPicker.setMinValue(0);
        mSecondPicker.setMaxValue(59);
        mSecondPicker.setOnValueChangedListener(mOnValueChangeListener);

        reset();
    }

    public void setCreateView(TextView createView) {
        mCreate = createView;
    }

    public void reset() {
        mTimeValue[0] = 0;
        mTimeValue[1] = 1;
        mTimeValue[2] = 0;

        updateTime();

        if (mCreate != null) {
            mCreate.setEnabled(true);
        }
    }

    public void setTime(long millis) {
        long seconds, minutes, hours;
        seconds = millis / 1000;
        minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        hours = minutes / 60;
        minutes = minutes - hours * 60;

        mTimeValue[0] = (int) hours;
        mTimeValue[1] = (int) minutes;
        mTimeValue[2] = (int) seconds;

        updateTime();
        updateStartButton();
    }

    public long getTimeInMillis() {
        final int hoursInSeconds = mTimeValue[0] * 3600;
        final int minutesInSeconds = mTimeValue[1] * 60;
        final int seconds = mTimeValue[2];
        final int totalSeconds = hoursInSeconds + minutesInSeconds + seconds;

        return totalSeconds * DateUtils.SECOND_IN_MILLIS;
    }

    public Serializable getState() {
        return Arrays.copyOf(mTimeValue, mTimeValue.length);
    }

    public void setState(Serializable state) {
        final int[] timeValue = (int[]) state;
        if (mTimeValue != null && mTimeValue.length == timeValue.length) {
            System.arraycopy(timeValue, 0, mTimeValue, 0, mTimeValue.length);
            updateTime();
            updateStartButton();
        }
    }

    private void updateTime() {
        mHourPicker.setValue(mTimeValue[0]);
        mMinutePicker.setValue(mTimeValue[1]);
        mSecondPicker.setValue(mTimeValue[2]);
    }

    private void updateStartButton() {
        boolean enabled = true;
        if (mTimeValue[0] == 0 && mTimeValue[1] == 0 && mTimeValue[2] == 0) {
            enabled = false;
        }

        if (mCreate != null && mCreate.isEnabled() != enabled) {
            mCreate.setEnabled(enabled);
            mCreate.setTextColor(enabled ? colorGreen : colorGrayUnable);
            mCreate.setBackgroundResource(enabled ?
                    R.drawable.bg_circle_green : R.drawable.bg_circle_gray_unable);
        }
    }
}
