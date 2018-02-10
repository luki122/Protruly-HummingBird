package com.android.calendar.hb;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.calendar.Event;
import com.android.calendar.R;
import com.android.calendar.Utils;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

class MonthAgendaAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mInflater;

    private ArrayList<Event> mEvents;

    private final StringBuilder mStringBuilder;
    private final Formatter mFormatter;

    private static boolean mFirstTime = true;
    private static List<String> mDescriptionValues;

    MonthAgendaAdapter(Context context, ArrayList<Event> events) {
        mContext = context;
        mInflater = LayoutInflater.from(context);

        mEvents = events;

        mStringBuilder = new StringBuilder(50);
        mFormatter = new Formatter(mStringBuilder, Locale.getDefault());

        if (mFirstTime) {
            mDescriptionValues = Utils.loadDescriptionValues(context.getResources());
            mFirstTime = false;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.hb_month_agenda_item, null);
        }

        TextView titleView = (TextView) convertView.findViewById(R.id.title);
        TextView whenView = (TextView) convertView.findViewById(R.id.when);
        ImageView labelIconView = (ImageView) convertView.findViewById(R.id.label_icon);

        Event event = mEvents.get(position);
        updateView(event, titleView, whenView, labelIconView);

        return convertView;
    }

    @Override
    public int getCount() {
        return mEvents.size();
    }

    @Override
    public Object getItem(int position) {
        return mEvents.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    private void updateView(final Event event, TextView titleView, TextView whenView,
                            ImageView labelIconView) {

        titleView.setText(event.title);
        whenView.setText(getWhenString(event));

        // label icon
        int iconResId = Utils.LABEL_ICON_CUSTOM_ID;
        String description = event.description;
        if (!TextUtils.isEmpty(description)) {
            int index = mDescriptionValues.indexOf(description);
            if (index != -1) {
                iconResId = Utils.getLabelIconId(index);
            }
        }
        labelIconView.setImageResource(iconResId);
    }

    private String getWhenString(Event event) {
        long begin = event.startMillis;
        long end = event.endMillis;
        boolean allDay = event.allDay;
        String eventTz = event.timeZone;
        int flags = 0;
        String whenString;
        String tzString = Utils.getTimeZone(mContext, null);
        if (allDay) {
            tzString = Time.TIMEZONE_UTC;
        } else if (end - begin < DateUtils.DAY_IN_MILLIS) {
            flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH;
        } else {
            flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH;
        }
        if (DateFormat.is24HourFormat(mContext)) {
            flags |= DateUtils.FORMAT_24HOUR;
        }
        mStringBuilder.setLength(0);
        whenString = DateUtils.formatDateRange(mContext, mFormatter, begin, end, flags, tzString).toString();
        if (!allDay && !TextUtils.equals(tzString, eventTz)) {
            String displayName;
            // Figure out if this is in DST
            Time date = new Time(tzString);
            date.set(begin);

            TimeZone tz = TimeZone.getTimeZone(tzString);
            if (tz == null || tz.getID().equals("GMT")) {
                displayName = tzString;
            } else {
                displayName = tz.getDisplayName(date.isDst != 0, TimeZone.SHORT);
            }
            whenString += " (" + displayName + ")";
        }

        return whenString;
    }

}
