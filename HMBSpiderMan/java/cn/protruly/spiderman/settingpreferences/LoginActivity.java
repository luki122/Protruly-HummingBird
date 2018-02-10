package cn.protruly.spiderman.settingpreferences;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import cn.protruly.spiderman.R;
import cn.protruly.spiderman.controldaemon.ConnectSpiderMan;


/**
 * Created by lijia on 17-4-24.
 */

public class LoginActivity extends PreferenceActivity {

    private final String TAG = "LoginActivity";
    private SelfdefinedSwitchPreference mobileLog;
    private SelfdefinedSwitchPreference modemLog;
    private SelfdefinedSwitchPreference powerLog;
    private SelfdefinedSwitchPreference networkLog;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private boolean status = true;
    private Button startAndStop;
    private Button clearAllFile;
    private Button aboutVersion;
    private ConnectSpiderMan connectSpiderMan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.settings_pref);
        setContentView(R.layout.settings);

        initPreference();
        initView();

    }

    private void initPreference() {

        mobileLog = (SelfdefinedSwitchPreference) findPreference("mobile_log");
        modemLog = (SelfdefinedSwitchPreference) findPreference("modem_log");
        powerLog = (SelfdefinedSwitchPreference) findPreference("power_log");
        networkLog = (SelfdefinedSwitchPreference) findPreference("network_log");

        // 监听SwitchPreference上的开关
        listenerSwitchPreferenceButton();

        // 监听SwitchPreference
        listenerSwitchPreference();

    }

    private void listenerSwitchPreferenceButton() {

        mobileLog.setChecked(true);  // mobile log 默认开启
        final CompoundButton.OnCheckedChangeListener mobileSwitchListener = new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                mobileLog.callOnStateChangeListener(isChecked);
                if (isChecked) {

                } else {

                }
            }
        };
        mobileLog.setOnCheckedChangeListener(mobileSwitchListener);


        CompoundButton.OnCheckedChangeListener modemSwitchListener = new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                modemLog.callOnStateChangeListener(isChecked);
                if (isChecked) {
//                    Toast.makeText(LoginActivity.this, "您打开了 " + "ModemLog", Toast.LENGTH_SHORT).show();
                } else {
//                    Toast.makeText(LoginActivity.this, "您关闭了 " + "ModemLog", Toast.LENGTH_SHORT).show();
                }

            }
        };
        modemLog.setOnCheckedChangeListener(modemSwitchListener);

        CompoundButton.OnCheckedChangeListener powerSwitchListener = new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                powerLog.callOnStateChangeListener(isChecked);
                if (isChecked) {
//                    Toast.makeText(LoginActivity.this, "您打开了 " + "PowerLog", Toast.LENGTH_SHORT).show();
                } else {
//                    Toast.makeText(LoginActivity.this, "您关闭了 " + "PowerLog", Toast.LENGTH_SHORT).show();
                }
            }
        };
        powerLog.setOnCheckedChangeListener(powerSwitchListener);

        CompoundButton.OnCheckedChangeListener networkSwitchListener = new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                networkLog.callOnStateChangeListener(isChecked);
                if (isChecked) {
//                    Toast.makeText(LoginActivity.this, "您打开了 " + "NetworkLog", Toast.LENGTH_SHORT).show();
                } else {
//                    Toast.makeText(LoginActivity.this, "您关闭了 " + "NetworkLog", Toast.LENGTH_SHORT).show();
                }
            }
        };
        networkLog.setOnCheckedChangeListener(networkSwitchListener);

    }


    private void listenerSwitchPreference() {

        String[] itemsTitle = {"Android Log", "Kernel Log", "ModemLog1", "ModemLog2", "ModemLog3", "ModemLog4", "ModemLog5",
                "PowerLog1", "PowerLog2", "PowerLog3", "PowerLog4", "PowerLog5", "NetworkLog1", "NetworkLog2", "NetworkLog3", "NetworkLog4", "NetworkLog5"};
        boolean[] selectedState = {true, false, false, false, false, false, false,
                false, false, false, false, false, false, false, false, false, false};

        sharedPreferences = getSharedPreferences("SMData", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        for (int i = 0; i < itemsTitle.length; i++) {
            editor.putBoolean(itemsTitle[i], selectedState[i]);
        }
        editor.commit();

        View.OnClickListener mobileLogSet = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                final String[] items = {"Android Log", "Kernel Log"};
                final boolean[] selected = {false, false};
                updataDialogData(items, selected);
                dialog("MobileLog", items, selected);

            }

        };
        mobileLog.setOnClickListener(mobileLogSet);

        View.OnClickListener modemLogSet = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                final String[] items = {"ModemLog1", "ModemLog2", "ModemLog3", "ModemLog4", "ModemLog5"};
                final boolean[] selected = {false, false, false, false, false};
                updataDialogData(items, selected);
                dialog("ModemLog", items, selected);
            }
        };
        modemLog.setOnClickListener(modemLogSet);

        View.OnClickListener powerLogSet = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final String[] items = {"PowerLog1", "PowerLog2", "PowerLog3", "PowerLog4", "PowerLog5"};
                final boolean[] selected = {false, false, false, false, false};
                updataDialogData(items, selected);
                dialog("PowerLog", items, selected);
            }

        };
        powerLog.setOnClickListener(powerLogSet);

        View.OnClickListener networkLogSet = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final String[] items = {"NetworkLog1", "NetworkLog2", "NetworkLog3", "NetworkLog4", "NetworkLog5"};
                final boolean[] selected = {false, false, false, false, false};
                updataDialogData(items, selected);
                dialog("NetworkLog", items, selected);
            }

        };
        networkLog.setOnClickListener(networkLogSet);

    }

    private void updataDialogData(String[] items, boolean[] selected) {

        for (int j = 0; j < items.length; j++) {
            selected[j] = sharedPreferences.getBoolean(items[j], false);
        }

    }

    private void dialog(String title, final String[] items, final boolean[] selected) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle(title);
        builder.setIcon(R.mipmap.ic_launcher);

        builder.setMultiChoiceItems(items, selected, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(LoginActivity.this, "您打开了 " + items[which], Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "您关闭了 " + items[which], Toast.LENGTH_SHORT).show();
                }
                selected[which] = isChecked;
                editor.putBoolean(items[which], selected[which]);
            }
        });

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                editor.commit();
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void initView() {

        TextView storageSpace = (TextView) findViewById(R.id.storage_space);
        clearAllFile = (Button) findViewById(R.id.clear_all_file);
        startAndStop = (Button) findViewById(R.id.start_and_stop);
        aboutVersion = (Button) findViewById(R.id.about_version);

        storageSpace.setText(getInternalStorageSpaceSize(getApplicationContext()));

        clearAllFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "删除所有log文件", Toast.LENGTH_SHORT).show();
            }
        });

        startAndStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (status) {
                    status = false;
                    startAndStop.setText(R.string.stop);
                    startDebug();
                } else {
                    status = true;
                    startAndStop.setText(R.string.start);
                    stopDebug();
                }

            }

        });

        aboutVersion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "log系统版本", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void startDebug() {

        /**
         * 将所有preference开关置灰，成不可点击状态
         */
        mobileLog.setEnabled(false);
        modemLog.setEnabled(false);
        powerLog.setEnabled(false);
        networkLog.setEnabled(false);
        clearAllFile.setEnabled(false);
        aboutVersion.setEnabled(false);

        connectSpiderMan = new ConnectSpiderMan();
        connectSpiderMan.transact(1);
        Log.v(TAG, "connectSpiderMan");
        Toast.makeText(getApplicationContext(), "开始调试", Toast.LENGTH_SHORT).show();
    }

    private void stopDebug() {

        /**
         * 将所有preference开关激活，成可点击状态
         */
        mobileLog.setEnabled(true);
        modemLog.setEnabled(true);
        powerLog.setEnabled(true);
        networkLog.setEnabled(true);
        clearAllFile.setEnabled(true);
        aboutVersion.setEnabled(true);

        connectSpiderMan.disConnect();
        Toast.makeText(getApplicationContext(), "停止调试", Toast.LENGTH_SHORT).show();
    }


    private String getInternalStorageSpaceSize(Context context) {

        File file = Environment.getDataDirectory();
        StatFs statFs = new StatFs(file.getPath());
        long blockSizeLong = statFs.getBlockSizeLong();
        long blockCountLong = statFs.getBlockCountLong();
        long availableBlocksLong = statFs.getAvailableBlocksLong();
        long usedSize = (blockCountLong - availableBlocksLong) * blockSizeLong;
        long availableSize = availableBlocksLong * blockSizeLong;

        return "已用空间/可用空间: " + Formatter.formatFileSize(context, usedSize)
                + "/" + Formatter.formatFileSize(context, availableSize);

    }

}
