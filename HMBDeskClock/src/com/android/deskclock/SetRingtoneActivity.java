package com.android.deskclock;

import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import hb.app.HbActivity;
import hb.widget.toolbar.Toolbar;

/**
 * Created by yubai on 17-8-15.
 */

public class SetRingtoneActivity extends HbActivity implements SimpleAlarmItemAdapter.HostInterface,
        LoaderManager.LoaderCallbacks<Cursor> {

    private ListView mAlarmsList;
    private SimpleAlarmItemAdapter mAdapter;

    private long[] repeatCheckedIds = null;
    private long[] selectedAlarms = null;

    private int mHasSelectedNum;
    private HashSet<Long> mSelectedSet = new HashSet<>();
    private HashMap<Long, Boolean> mAlarms;

    private Toolbar mToolbar;
    private TextView mTitle;
    private TextView mLeftTitleBtn;
    private TextView mRightTitleBtn;
    private TextView mConfirmBtn;

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, SetRingtoneActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        // TODO: 17-8-17 get Uri
        final Uri uri = intent.getData();
        getLoaderManager().initLoader(0, null, this);
        initMultiSelectedHashMap();
        setContentView(R.layout.ringtone_setting);
        mAlarmsList = (ListView) findViewById(R.id.alarms_list);
        mAdapter = new SimpleAlarmItemAdapter(this, repeatCheckedIds, selectedAlarms);
        mAdapter.setHost(this);
        mAlarmsList.setVerticalScrollBarEnabled(true);
        mAlarmsList.setAdapter(mAdapter);

        initToolBar();
        updateTitle();

        mLeftTitleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mRightTitleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAllChecked()) {
                    mSelectedSet.clear();
                    initMultiSelectedHashMap();
                    mRightTitleBtn.setText(getString(R.string.alarm_selected_all));
                } else {
                    mAlarms.clear();
                    mSelectedSet.clear();
                    List<Alarm> alarms = Alarm.getAlarms(getContentResolver(), null);
                    for (int i = 0; i < alarms.size(); ++i) {
                        mAlarms.put(alarms.get(i).id, true);
                        mSelectedSet.add(alarms.get(i).id);
                    }
                    mHasSelectedNum = mAlarms.size();
                    mRightTitleBtn.setText(getString(R.string.alarm_unselected_all));
                }
                updateTitle();
                mAdapter.notifyDataSetChanged();
            }
        });
        mConfirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelectedSet.clear();
                List<Alarm> alarms = Alarm.getAlarms(getContentResolver(), null);
                for (int i = 0; i < alarms.size(); ++i) {
                    if (mAlarms.get(alarms.get(i).id)) {
                        // TODO: 17-8-17
                        if (uri != null) {
                            alarms.get(i).alert = uri;
                            asyncUpdateAlarm(alarms.get(i), false);
                        }
                    }
                }
                finish();
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return Alarm.getAlarmsCursorLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void onAlarmItemClick(long id) {
        // TODO: 17-8-16 add to checked-array
        if (mSelectedSet.contains(new Long(id))) {
            mSelectedSet.remove(new Long(id));
            if (mAlarms.containsKey(id)) {
                mAlarms.put(id, false);
            }
            mHasSelectedNum--;
        } else {
            mSelectedSet.add(new Long(id));
            if (mAlarms.containsKey(id)) {
                mAlarms.put(id, true);
            }
            mHasSelectedNum++;
        }

        if (isAllChecked()) {
            mRightTitleBtn.setText(getString(R.string.alarm_unselected_all));
        } else {
            mRightTitleBtn.setText(getString(R.string.alarm_selected_all));
        }

        updateTitle();
    }

    @Override
    public boolean isAlarmItemSelected(long id) {
        if (mSelectedSet.contains(new Long(id))) {
            return true;
        }
        return false;
    }

    private void initMultiSelectedHashMap() {
        if (mAlarms == null) {
            mAlarms = new HashMap<>();
        }

        mAlarms.clear();
        List<Alarm> alarms = Alarm.getAlarms(getContentResolver(), null);
        for (int i = 0; i < alarms.size(); ++i) {
            mAlarms.put(alarms.get(i).id, false);
        }
        mHasSelectedNum = 0;
    }

    private boolean isAllChecked() {
        return !mAlarms.containsValue(false);
    }

    private void updateTitle() {
        String titleFormat = getResources().getString(R.string.alarm_has_selected_num);
        String title = String.format(titleFormat, mHasSelectedNum);
        mTitle.setText(title);
        if (mHasSelectedNum == 0) {
            mConfirmBtn.setTextColor(getColor(R.color.delete_btn_disable));
            mConfirmBtn.setEnabled(false);
        } else {
            mConfirmBtn.setTextColor(Color.BLACK);
            mConfirmBtn.setEnabled(true);
        }
    }

    private void initToolBar() {
        mToolbar = (Toolbar) findViewById(R.id.alarm_list_toolbar);
        mToolbar.showBottomDivider(false);
        mTitle = (TextView) mToolbar.findViewById(R.id.toolbar_title);
        mLeftTitleBtn = (TextView) mToolbar.findViewById(R.id.title_left_button);
        mRightTitleBtn = (TextView) mToolbar.findViewById(R.id.title_right_button);
        mConfirmBtn = (TextView) findViewById(R.id.delete_btn);
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
}
