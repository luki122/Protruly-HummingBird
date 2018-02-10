package com.android.deskclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.provider.DaysOfWeek;
import com.android.deskclock.widget.TextTime;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;

import hb.app.dialog.AlertDialog;

/**
 * Created by yubai on 17-5-3.
 */

public class SimpleAlarmItemAdapter extends CursorAdapter {

    private static final float ALARM_ELEVATION = 8f;
    private static final float TINTED_LEVEL = 0.09f;

    private final Context mContext;
    private final LayoutInflater mFactory;
    private final Typeface mRobotoNormal;

    private final HashSet<Long> mRepeatChecked = new HashSet<>();
    private final HashSet<Long> mSelectedAlarms = new HashSet<>();

    private HostInterface mHost;

    public interface HostInterface {
        void onAlarmItemClick(long id);
        boolean isAlarmItemSelected(long id);
    }

    // Determines the order that days of the week are shown in the UI
    private int[] mDayOrder;

    // A reference used to create mDayOrder
    private final int[] DAY_ORDER = new int[] {
            Calendar.SUNDAY,
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
    };

    public class ItemHolder {

        // views for optimization
        RelativeLayout alarmItem;
        TextTime clock;
        TextView tomorrowLabel;
        CompoundButton onoff;
        TextView daysOfWeek;
        TextView label;
        View summary;
        View hairLine;
        View collapseExpandArea;
        CheckBox checkBox;

        // Other states
        Alarm alarm;
    }

    public SimpleAlarmItemAdapter(Context context, long[] repeatCheckedIds, long[] selectedAlarms) {
        super(context, null, 0);
        mContext = context;
        mFactory = LayoutInflater.from(context);

        Resources res = mContext.getResources();

        mRobotoNormal = Typeface.create("sans-serif", Typeface.NORMAL);

        if (repeatCheckedIds != null) {
            buildHashSetFromArray(repeatCheckedIds, mRepeatChecked);
        }

        if (selectedAlarms != null) {
            buildHashSetFromArray(selectedAlarms, mSelectedAlarms);
        }

        setDayOrder();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (!getCursor().moveToPosition(position)) {
            // May happen if the last alarm was deleted and the cursor refreshed while the
            // list is updated.
            LogUtils.v("couldn't move cursor to position " + position);
            return null;
        }
        View v;
        if (convertView == null) {
            v = newView(mContext, getCursor(), parent);
        } else {
            v = convertView;
        }
        bindView(v, mContext, getCursor());
        return v;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final View view = mFactory.inflate(R.layout.alarm_time_2, parent, false);
        setNewHolder(view);
        return view;
    }

    /**
     * In addition to changing the data set for the alarm list, swapCursor is now also
     * responsible for preparing the transition for any added/removed items.
     */
    @Override
    public synchronized Cursor swapCursor(Cursor cursor) {
        final Cursor c = super.swapCursor(cursor);
        return c;
    }

    private void setDayOrder() {
        // Value from preferences corresponds to Calendar.<WEEKDAY> value
        // -1 in order to correspond to DAY_ORDER indexing

        // M T W T F S S  ==>  mDayOrder 0 1 2 3 4 5 6
        // 0 1 2 3 4 5 6                 S M T W T F S
        final int startDay = Utils.getZeroIndexedFirstDayOfWeek(mContext);
        mDayOrder = new int[DaysOfWeek.DAYS_IN_A_WEEK];

        for (int i = 0; i < DaysOfWeek.DAYS_IN_A_WEEK; ++i) {
            mDayOrder[i] = DAY_ORDER[(startDay + i) % 7];
        }
    }

    private ItemHolder setNewHolder(View view) {
        // standard view holder optimization
        final ItemHolder holder = new ItemHolder();
        holder.alarmItem = (RelativeLayout) view.findViewById(R.id.alarm_item);
        holder.tomorrowLabel = (TextView) view.findViewById(R.id.tomorrowLabel);
        holder.clock = (TextTime) view.findViewById(R.id.digital_clock);
        holder.clock.setTypeface(mRobotoNormal);
        holder.onoff = (CompoundButton) view.findViewById(R.id.onoff);
        holder.daysOfWeek = (TextView) view.findViewById(R.id.daysOfWeek);
        holder.label = (TextView) view.findViewById(R.id.label);
        holder.summary = view.findViewById(R.id.summary);
        holder.hairLine = view.findViewById(R.id.hairline);
        holder.collapseExpandArea = view.findViewById(R.id.collapse_expand);
        holder.checkBox = (CheckBox) view.findViewById(R.id.checkbox);

        view.setTag(holder);
        return holder;
    }

    @Override
    public void bindView(final View view, Context context, final Cursor cursor) {
        final Alarm alarm = new Alarm(cursor);
        Object tag = view.getTag();
        if (tag == null) {
            // The view was converted but somehow lost its tag.
            tag = setNewHolder(view);
        }
        final ItemHolder itemHolder = (ItemHolder) tag;
        itemHolder.alarm = alarm;

        // Hack to workaround b/21459481: the SwitchCompat instance must be detached from
        // its parent in order to avoid running the checked animation, which may get stuck
        // when ListView calls View#jumpDrawablesToCurrentState() on a recycled view.
        if (itemHolder.onoff.isChecked() != alarm.enabled) {
            final ViewGroup onoffParent = (ViewGroup) itemHolder.onoff.getParent();
            final int onoffIndex = onoffParent.indexOfChild(itemHolder.onoff);
            onoffParent.removeView(itemHolder.onoff);
            if (!alarm.notFiredNextTime) {
                itemHolder.onoff.setChecked(alarm.enabled);
            } else {
                itemHolder.onoff.setChecked(false);
            }
            onoffParent.addView(itemHolder.onoff, onoffIndex);
        }
        if (mHost != null ) {
            if (mHost.isAlarmItemSelected(itemHolder.alarm.id)) {
                itemHolder.checkBox.setChecked(true);
            } else {
                itemHolder.checkBox.setChecked(false);
            }
        }

        if (mSelectedAlarms.contains(itemHolder.alarm.id)) {
            setAlarmItemBackgroundAndElevation(itemHolder.alarmItem, true /* expanded */);
        } else {
            setAlarmItemBackgroundAndElevation(itemHolder.alarmItem, false /* expanded */);
        }
        setDigitalTimeAlpha(itemHolder, true);

        itemHolder.checkBox.setVisibility(View.VISIBLE);
        itemHolder.onoff.setVisibility(View.GONE);

        itemHolder.clock.setFormat(mContext,
                mContext.getResources().getDimensionPixelSize(R.dimen.alarm_label_size));
        itemHolder.clock.setTime(alarm.hour, alarm.minutes);

        if (mRepeatChecked.contains(alarm.id) || itemHolder.alarm.daysOfWeek.isRepeating()) {
            itemHolder.tomorrowLabel.setVisibility(View.GONE);
        } else {
            itemHolder.tomorrowLabel.setVisibility(View.VISIBLE);
            final Resources resources = context.getResources();
            final String labelText = Alarm.isTomorrow(alarm) ?
                    resources.getString(R.string.alarm_tomorrow) :
                    resources.getString(R.string.alarm_today);
            itemHolder.tomorrowLabel.setText(labelText);
        }

        // Set the repeat text or leave it blank if it does not repeat.
        final String daysOfWeekStr =
                alarm.daysOfWeek.toString(context, Utils.getFirstDayOfWeek(context));
        if (daysOfWeekStr != null && daysOfWeekStr.length() != 0) {
            itemHolder.daysOfWeek.setText(daysOfWeekStr);
            itemHolder.daysOfWeek.setContentDescription(alarm.daysOfWeek.toAccessibilityString(
                    context, Utils.getFirstDayOfWeek(context)));
            itemHolder.daysOfWeek.setVisibility(View.VISIBLE);
        } else {
            itemHolder.daysOfWeek.setVisibility(View.GONE);
        }

        if (alarm.label != null && alarm.label.length() != 0) {
            itemHolder.label.setText(alarm.label + "  ");
        } else {
            itemHolder.label.setText(context.getString(R.string.default_label) + "  ");
        }

        itemHolder.alarmItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                itemHolder.checkBox.setChecked(!itemHolder.checkBox.isChecked());
                // TODO: 17-8-16 record checked
                if (mHost != null) {
                    mHost.onAlarmItemClick(alarm.id);
                }
                notifyDataSetChanged();
            }
        });

    }

    private void setAlarmItemBackgroundAndElevation(RelativeLayout layout, boolean expanded) {
        if (expanded) {
            layout.setBackgroundColor(getTintedBackgroundColor());
            layout.setElevation(ALARM_ELEVATION);
        } else {
            layout.setBackgroundResource(R.drawable.alarm_background_normal);
            layout.setElevation(0f);
        }
    }

    private int getTintedBackgroundColor() {
        final int c = Utils.getCurrentHourColor();
        final int red = Color.red(c) + (int) (TINTED_LEVEL * (255 - Color.red(c)));
        final int green = Color.green(c) + (int) (TINTED_LEVEL * (255 - Color.green(c)));
        final int blue = Color.blue(c) + (int) (TINTED_LEVEL * (255 - Color.blue(c)));
        return Color.rgb(red, green, blue);
    }

    // Sets the alpha of the digital time display. This gives a visual effect
    // for enabled/disabled and expanded/collapsed alarm while leaving the
    // on/off switch more visible
    private void setDigitalTimeAlpha(ItemHolder holder, boolean enabled) {
        float alpha = enabled ? 1f : 0.69f;
        holder.clock.setAlpha(alpha);
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    private void buildHashSetFromArray(long[] ids, HashSet<Long> set) {
        for (long id : ids) {
            set.add(id);
        }
    }

    public void setHost(HostInterface host) {
        mHost = host;
    }
}