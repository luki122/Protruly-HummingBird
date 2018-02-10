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
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.format.DateUtils;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.deskclock.AlarmClockFragment;
import com.android.deskclock.AlarmClockFragment2;
import com.android.deskclock.AlarmUtils;
import com.android.deskclock.LogUtils;
import com.android.deskclock.Utils;
import com.android.deskclock.alarms.AlarmNotifications;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.provider.DaysOfWeek;
import com.android.deskclock.widget.TextTime;
import com.android.deskclock.worldclock.WorldClockAdapter2;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;

import hb.app.dialog.AlertDialog;

/**
 * Created by yubai on 17-5-3.
 */

public class AlarmItemAdapter extends CursorAdapter {

    private static final long INVALID_ID = -1;
    private static final float ALARM_ELEVATION = 8f;
    private static final float TINTED_LEVEL = 0.09f;

    private final Context mContext;
    private final LayoutInflater mFactory;
    private final Typeface mRobotoNormal;
    private final ListView mList;

    private final HashSet<Long> mRepeatChecked = new HashSet<>();
    private final HashSet<Long> mSelectedAlarms = new HashSet<>();
    private Bundle mPreviousDaysOfWeekMap = new Bundle();

    private final boolean mHasVibrator;

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

    public interface HostInterface {
        public void onAlarmItemClick(boolean isLongClick, long id);
        public boolean isAlarmItemSelected(long id);
        public boolean isSelectionMode();
    }

    private HostInterface mHostInterface;

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

    // Used for scrolling an expanded item in the list to make sure it is fully visible.
    private long mScrollAlarmId = INVALID_ID;
    // TODO: 17-4-20
    private final Runnable mScrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (mScrollAlarmId != INVALID_ID) {
                View v = getViewById(mScrollAlarmId);
                if (v != null) {
                    Rect rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                    mList.requestChildRectangleOnScreen(v, rect, false);
                }
                mScrollAlarmId = INVALID_ID;
            }
        }
    };

    public AlarmItemAdapter(Context context, long expandedId, long[] repeatCheckedIds,
                            long[] selectedAlarms, Bundle previousDaysOfWeekMap, ListView list, HostInterface hostInterface) {
        super(context, null, 0);
        mHostInterface = hostInterface;
        mContext = context;
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(SWITCH_STATE_UI_UPDATE);
//        mContext.registerReceiver(mSwitchStateUpdateReceiver, filter);
        mFactory = LayoutInflater.from(context);
        mList = list;

        Resources res = mContext.getResources();

        mRobotoNormal = Typeface.create("sans-serif", Typeface.NORMAL);

        if (repeatCheckedIds != null) {
            buildHashSetFromArray(repeatCheckedIds, mRepeatChecked);
        }
        if (previousDaysOfWeekMap != null) {
            mPreviousDaysOfWeekMap = previousDaysOfWeekMap;
        }
        if (selectedAlarms != null) {
            buildHashSetFromArray(selectedAlarms, mSelectedAlarms);
        }

        mHasVibrator = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE))
                .hasVibrator();

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

        // We must unset the listener first because this maybe a recycled view so changing the
        // state would affect the wrong alarm.
        itemHolder.onoff.setOnCheckedChangeListener(null);

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

        if (mSelectedAlarms.contains(itemHolder.alarm.id)) {
            setAlarmItemBackgroundAndElevation(itemHolder.alarmItem, true /* expanded */);
            setDigitalTimeAlpha(itemHolder, true);
            itemHolder.onoff.setEnabled(false);
        } else {
            itemHolder.onoff.setEnabled(true);
            setAlarmItemBackgroundAndElevation(itemHolder.alarmItem, false /* expanded */);
            setDigitalTimeAlpha(itemHolder, itemHolder.onoff.isChecked());
        }

        if (mHostInterface != null) {
            if (mHostInterface.isSelectionMode()) {
                itemHolder.checkBox.setVisibility(View.VISIBLE);
                itemHolder.onoff.setVisibility(View.GONE);
            } else {
                itemHolder.checkBox.setVisibility(View.GONE);
                itemHolder.onoff.setVisibility(View.VISIBLE);
            }

            if (mHostInterface.isAlarmItemSelected(itemHolder.alarm.id)) {
                itemHolder.checkBox.setChecked(true);
            } else {
                itemHolder.checkBox.setChecked(false);
            }
        }

        // TODO: 17-4-20 点击闹钟时间 进入编辑时间Dialog
        itemHolder.clock.setFormat(mContext,
                mContext.getResources().getDimensionPixelSize(R.dimen.alarm_label_size));
        itemHolder.clock.setTime(alarm.hour, alarm.minutes);

        final CompoundButton.OnCheckedChangeListener onOffListener =
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(final CompoundButton compoundButton,final boolean checked) {
                        if (checked) {
                            if (checked == alarm.enabled && alarm.notFiredNextTime) {
                                AlarmInstance instance
                                        = AlarmInstance.getNextUpcomingInstanceByAlarmId(mContext.getContentResolver(), alarm.id);
                                instance.mNextTimeNotFired = false;
                                AlarmInstance.updateInstance(mContext.getContentResolver(), instance);
                                alarm.notFiredNextTime = false;
                            }
                            setDigitalTimeAlpha(itemHolder, checked);
                            alarm.enabled = checked;
                            ((AlarmClockFragment2)mHostInterface).asyncUpdateAlarm(alarm, false);
                        } else {
                            if (checked != alarm.enabled) {
                                if (alarm.daysOfWeek.isRepeating()) {
                                    Calendar nextAlarmTime = alarm.getNextAlarmTime(Calendar.getInstance());
                                    DateFormatSymbols dfs = new DateFormatSymbols();
                                    String[] dayList = dfs.getShortWeekdays();
                                    String positiveText;
                                    if (Locale.getDefault().getLanguage().equals(Locale.ENGLISH.getLanguage())) {
                                        positiveText = mContext.getString(R.string.dismiss_only_next_time_en,
                                                DateUtils.getMonthString(nextAlarmTime.get(Calendar.MONTH) + 1, DateUtils.LENGTH_SHORT),
                                                nextAlarmTime.get(Calendar.DAY_OF_MONTH),
                                                dayList[nextAlarmTime.get(Calendar.DAY_OF_WEEK)]);
                                    } else {
                                        positiveText = mContext.getString(R.string.dismiss_only_next_time,
                                                nextAlarmTime.get(Calendar.MONTH) + 1, nextAlarmTime.get(Calendar.DAY_OF_MONTH),
                                                dayList[nextAlarmTime.get(Calendar.DAY_OF_WEEK)]);
                                    }
                                    final AlertDialog alertDialog = new AlertDialog.Builder(mContext)
                                            .setPositiveButton(positiveText, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    alarm.notFiredNextTime = true;
                                                    AlarmInstance instance
                                                            = AlarmInstance.getNextUpcomingInstanceByAlarmId(mContext.getContentResolver(), alarm.id);
                                                    alarm.disabledYear = instance.getAlarmTime().get(Calendar.YEAR);
                                                    alarm.disabledMonth = instance.getAlarmTime().get(Calendar.MONTH);
                                                    alarm.disabledDay = instance.getAlarmTime().get(Calendar.DAY_OF_MONTH);
                                                    ((AlarmClockFragment2)mHostInterface).asyncUpdateAlarm(alarm, false);
                                                   
                                                    instance.mNextTimeNotFired = true;
                                                    AlarmInstance.updateInstance(mContext.getContentResolver(), instance);
                                                }
                                            })
                                            .setNeutralButton(R.string.time_picker_cancel, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    compoundButton.setChecked(!checked);
                                                }
                                            })
                                            .setNegativeButton(R.string.dismiss_all,
                                                    new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            setDigitalTimeAlpha(itemHolder, checked);
                                                            alarm.enabled = checked;
                                                            ((AlarmClockFragment2)mHostInterface).asyncUpdateAlarm(alarm, alarm.enabled);
                                                        }
                                                    })
                                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                                @Override
                                                public void onCancel(DialogInterface dialog) {
                                                    compoundButton.setChecked(!checked);
                                                }
                                            })
                                            .setMessage(R.string.switch_off_alarm)
                                            .create();
                                    alertDialog.show();
                                } else {
                                    setDigitalTimeAlpha(itemHolder, checked);
                                    alarm.enabled = checked;
                                    ((AlarmClockFragment2)mHostInterface).asyncUpdateAlarm(alarm, false);
                                }
                            }
                        }
                    }
                };

        if (mRepeatChecked.contains(alarm.id) || itemHolder.alarm.daysOfWeek.isRepeating()) {
            itemHolder.tomorrowLabel.setVisibility(View.GONE);
        } else {
            itemHolder.tomorrowLabel.setVisibility(View.VISIBLE);
            final Resources resources = ((AlarmClockFragment2)mHostInterface).getResources();
            final String labelText = Alarm.isTomorrow(alarm) ?
                    resources.getString(R.string.alarm_tomorrow) :
                    resources.getString(R.string.alarm_today);
            itemHolder.tomorrowLabel.setText(labelText);
        }
        itemHolder.onoff.setOnCheckedChangeListener(onOffListener);

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
//            itemHolder.label.setVisibility(View.VISIBLE);
//            itemHolder.label.setContentDescription(
//                    mContext.getResources().getString(R.string.label_description) + " "
//                            + alarm.label);
        } else {
//            itemHolder.label.setVisibility(View.GONE);
            itemHolder.label.setText(context.getString(R.string.default_label) + "  ");
        }

        itemHolder.alarmItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((AlarmClockFragment2)mHostInterface).mSelectedAlarm = itemHolder.alarm;
                processClick(view, false, itemHolder.alarm.id);
            }
        });

        itemHolder.alarmItem.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View v) {
                return processClick(v, true, itemHolder.alarm.id);
            }
        });

    }

    //elevation  类似阴影的玩意儿
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

    private boolean processClick(final View v, final boolean isLongClick, final long id) {

        if (mHostInterface != null) {
            mHostInterface.onAlarmItemClick(isLongClick, id);
            notifyDataSetChanged();
            // TODO: 17-5-3
            return true;
        }
        return false;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    private View getViewById(long id) {
        for (int i = 0; i < mList.getCount(); i++) {
            View v = mList.getChildAt(i);
            if (v != null) {
                ItemHolder h = (ItemHolder)(v.getTag());
                if (h != null && h.alarm.id == id) {
                    return v;
                }
            }
        }
        return null;
    }

    public long[] getSelectedAlarmsArray() {
        int index = 0;
        long[] ids = new long[mSelectedAlarms.size()];
        for (long id : mSelectedAlarms) {
            ids[index] = id;
            index++;
        }
        return ids;
    }

    public long[] getRepeatArray() {
        int index = 0;
        long[] ids = new long[mRepeatChecked.size()];
        for (long id : mRepeatChecked) {
            ids[index] = id;
            index++;
        }
        return ids;
    }

    public Bundle getPreviousDaysOfWeekMap() {
        return mPreviousDaysOfWeekMap;
    }

    private void buildHashSetFromArray(long[] ids, HashSet<Long> set) {
        for (long id : ids) {
            set.add(id);
        }
    }

    public static final String SWITCH_STATE_UI_UPDATE = "com.android.deskclock.SWITCH_STATE_UI_UPDATE";
    public static final String DISMISS_ONLY_ONCE_ALARM_ID = "dismiss_only_once_alarm_id";
    private BroadcastReceiver mSwitchStateUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("---", "--- onReceive " + intent.getAction());
            if (intent.getAction().equals(SWITCH_STATE_UI_UPDATE)) {
//                long id = intent.getLongExtra(DISMISS_ONLY_ONCE_ALARM_ID, -1);
//                Alarm alarm = Alarm.getAlarm(context.getContentResolver(), id);
//                if (id != -1 && alarm.notFiredNextTime) {
//                    alarm.notFiredNextTime = false;
//                    ((AlarmClockFragment2)mHostInterface).asyncUpdateAlarm(alarm, false);
//                    notifyDataSetChanged();
//                }
                notifyDataSetChanged();
            }
        }
    }; 


    public void unregisterBroadcastReceiver() {
       mContext.unregisterReceiver(mSwitchStateUpdateReceiver);
    }

    public void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(SWITCH_STATE_UI_UPDATE);
        mContext.registerReceiver(mSwitchStateUpdateReceiver, filter);
    }
}