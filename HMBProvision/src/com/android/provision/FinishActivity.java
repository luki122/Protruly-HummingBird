package com.android.provision;

import android.app.ActivityManager;
import android.app.StatusBarManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.android.provision.R;

import java.util.Set;

/**
 * Created by xiaobin on 17-5-18.
 */

public class FinishActivity extends BaseActivity {

    private static final String HIDE_NAVAGATIONBAR_ACTION = "com_hb_hide_navagationbar_action";
    private static final String HB_HIDE_NAVIGATION_FOR_APP = "hb_hide_bar_for_app";
    private final long KILL_DELAY = 2000L;

    private TextView btn_start;
    private ListView listView;

    private String[] finishItemArray;
    private FinishAdapter finishAdapter;

    private Set<Integer> checkSet;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setHbContentView(R.layout.activity_finish);

        getToolbar().setTitle(getString(R.string.back));

        btn_start = (TextView) findViewById(R.id.btn_start);
        listView = (ListView) findViewById(R.id.listView);

        finishItemArray = getResources().getStringArray(R.array.array_finish_item);
        finishAdapter = new FinishAdapter();
        listView.setAdapter(finishAdapter);

        checkSet = new ArraySet<Integer>();
        checkSet.add(0);
        checkSet.add(1);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (checkSet.contains(position)) {
                    checkSet.remove(position);
                } else {
                    checkSet.add(position);
                }
                finishAdapter.notifyDataSetChanged();
            }
        });

        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                complete();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onNavigationClicked(View view) {
        finish();
    }

    private boolean check() {
//        if (checkSet.size() == 2) {
//            return true;
//        }
//        Toast.makeText(this, getString(R.string.agree_all_tips), Toast.LENGTH_SHORT).show();
//        return false;
        return true;
    }

    private void complete() {

        if (check()) {
//            // 设置状态栏可滑动
//            StatusBarManager mStatusBarManager = (StatusBarManager) getSystemService(STATUS_BAR_SERVICE);
//            mStatusBarManager.disable(StatusBarManager.DISABLE_EXPAND);
//
//            // 显示虚拟按键
//            Settings.Secure.putInt(getContentResolver(), HB_HIDE_NAVIGATION_FOR_APP, 1);

            // Add a persistent setting to allow other apps to know the device has been provisioned.
            Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 1);
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 1);

            // remove this activity from the package manager.
            PackageManager pm = getPackageManager();
            ComponentName name = new ComponentName(this, LanguageSettingsWizard.class);
            pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            // 禁用关机广播
            ComponentName shutdown = new ComponentName(this, ShutdownReceiver.class);
            pm.setComponentEnabledSetting(shutdown, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);


            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage("com.android.dlauncher");
            startActivity(intent);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                    am.forceStopPackage(getPackageName());
                }
            }, KILL_DELAY);

        }

    }

    private class FinishAdapter extends BaseAdapter {

        LayoutInflater inflater;

        public FinishAdapter() {
            inflater = LayoutInflater.from(FinishActivity.this);
        }

        @Override
        public int getCount() {
            return finishItemArray == null ? 0 : finishItemArray.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder = null;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_finish, null);
                holder = new Holder();
                holder.tv_text = (TextView) convertView.findViewById(R.id.tv_text);
                holder.cb_check = (CheckBox) convertView.findViewById(R.id.cb_check);
                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }

            holder.tv_text.setText(finishItemArray[position]);
            if (checkSet.contains(position)) {
                holder.cb_check.setChecked(true);
            } else {
                holder.cb_check.setChecked(false);
            }

            return convertView;
        }

        class Holder {
            TextView tv_text;
            CheckBox cb_check;
        }

    }

}
