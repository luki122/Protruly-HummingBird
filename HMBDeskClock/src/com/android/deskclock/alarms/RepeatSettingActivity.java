package com.android.deskclock.alarms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.DaysOfWeek;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import hb.app.HbActivity;
import hb.widget.toolbar.Toolbar;


/**
 * Created by yubai on 17-4-27.
 */

public class RepeatSettingActivity extends HbActivity {
    // Number if days in the week.
    public static final int DAYS_IN_A_WEEK = 7;

    private static final String KEY_ALARM = "key_alarm";

    private ListView mListView;
    private BaseAdapter mAdapter;
    private String[] mDayList = new String[7];

    private Alarm mAlarm;
    private DaysOfWeek mDaysOfWeek;

    private Toolbar mToolbar;

    private int[] mDayOrder;
//    private HashMap<String, Boolean> map = new HashMap<>();

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

    public static void startActivityForResult(Activity context, Alarm alarm, int requestCode) {
        Intent intent = new Intent(context, RepeatSettingActivity.class);
        intent.putExtra(KEY_ALARM, alarm);
        context.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mAlarm = intent.getParcelableExtra(KEY_ALARM);
        if (mAlarm == null) {
            // TODO: 17-4-27 to create a new alarm
        } else {
            mDaysOfWeek = mAlarm.daysOfWeek;
        }
        setHbContentView(R.layout.ringtone_choose);
        mToolbar = getToolbar();
        mToolbar.setTitle(R.string.alarm_is_repeat);
        mToolbar.setNavigationIcon(getDrawable(R.drawable.clock_back));
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        getDayOrder();
        initWeekdayList();
        mListView = (ListView) findViewById(R.id.ringtone_list);
//        mAdapter = new WeekdayAdpater(this, mDayList, map);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mAdapter = new WeekdayAdpater(this, mDayList);
        mListView.setAdapter(mAdapter);
        getDaysOfWeek();
    }

    private void getDayOrder() {
        final int startDay = Utils.getFirstDayOfWeek(this);
        mDayOrder = new int[DaysOfWeek.DAYS_IN_A_WEEK];

        for (int i = 0; i < DaysOfWeek.DAYS_IN_A_WEEK; ++i) {
            mDayOrder[i] = DAY_ORDER[(i + startDay) % 7];
        }
    }

    // TODO: 17-4-27 set listview choice state with daysOfWeek
    private void getDaysOfWeek() {
        if (mDaysOfWeek != null) {
//            int bitSet = mDaysOfWeek.getBitSet();
            HashSet<Integer> setDays = mDaysOfWeek.getSetDays();
            for (Integer integer : setDays) {
                for (int i = 0; i < 7; ++i) {
                    if (mDayOrder[i] == integer.intValue()) {
                        mListView.setItemChecked(i, true);
                    }
                }
            }
        }
    }

    // TODO: 17-4-27 set daysOfWeek from listview
    private void setDaysOfWeek() {
        mAlarm.daysOfWeek.clearAllDays();
        for(long ids :mListView.getCheckItemIds()){
            mAlarm.daysOfWeek.setDaysOfWeek(true, mDayOrder[(int)ids]);
        }
    }

    private void initWeekdayList() {
        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] dayList = new String[7];

        for (int i = 1; i < 8; ++i) {
            dayList[i - 1] = dfs.getShortWeekdays()[i];
        }

        for (int i = 0; i < 7; ++i) {
            mDayList[i] = dayList[(i + 1) % 7];
        }

    }

    @Override
    public void onBackPressed() {
        setDaysOfWeek();
        Intent resultIntent = new Intent();
        resultIntent.putExtra(KEY_ALARM, mAlarm);
        setResult(RESULT_OK, resultIntent);
        super.onBackPressed();

    }

    private class WeekdayAdpater extends BaseAdapter {
        Context mContext;
        String[] mDayList;

        class ItemHolder {
            CheckedTextView checkedTextView;
        }

        public WeekdayAdpater(Context context, String[] dayList) {
            mContext = context;
            mDayList = dayList;
        }

        @Override
        public int getCount() {
            return mDayList.length;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater =  LayoutInflater.from(mContext);
            return createViewFromResource(inflater, convertView, parent,
                    position, android.R.layout.simple_list_item_multiple_choice);
        }

        private View createViewFromResource(LayoutInflater inflater, View convertView,
                                            ViewGroup parent, int position, int resourceId) {
            View v;
            if (convertView == null) {
                v = inflater.inflate(resourceId, parent, false);
                setNewHolder(v);
            } else {
                v = convertView;
            }
            bindView(position, v);
            return v;
        }

        private void bindView(int position, View view) {
            ItemHolder holder = (ItemHolder) view.getTag();
            if (holder == null) {
                setNewHolder(view);
            }

            if (mDaysOfWeek != null) {
                HashSet<Integer> setDays = mDaysOfWeek.getSetDays();
                for (Integer integer : setDays) {
                    if (mDayOrder[position] == integer.intValue()) {
                        holder.checkedTextView.setChecked(true);
                    }
                }
            }
            holder.checkedTextView.setText((String)getItem(position));
        }

        private void setNewHolder(View v) {
            ItemHolder holder = new ItemHolder();
            holder.checkedTextView = (CheckedTextView) v.findViewById(android.R.id.text1);
            v.setTag(holder);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Object getItem(int position) {
            return mDayList[position];
        }
    }
}
