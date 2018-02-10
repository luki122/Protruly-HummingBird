package cn.protruly.spiderman;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;

import cn.protruly.spiderman.collectservice.SystemPropertiesProxy;
import cn.protruly.spiderman.settingpreferences.LoginActivity;


public class MainActivity extends Activity {

    private static final String spiderman = "cn.proturly.spiderman";
    private static final String transmitlog = "cn.proturly.transmitlog";
    private CompoundButton logSwitch;
    private CompoundButton logcatSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logSwitch = (CompoundButton) findViewById(R.id.log_switch);
        logcatSwitch = (CompoundButton) findViewById(R.id.logcat_switch);

        checkBoxSwitch();

        Button bt = (Button) findViewById(R.id.updatedata);
        Button anrBt = (Button) findViewById(R.id.anr);
        Button javaCrashBt = (Button) findViewById(R.id.JavaCrash);
        Button startTransmit = (Button) findViewById(R.id.starttransmit);
        Button offlineLogBt = (Button) findViewById(R.id.settingpreferences);

        bt.setOnClickListener(new ButtonOnClickListener());
        anrBt.setOnClickListener(new ButtonOnClickListener());
        javaCrashBt.setOnClickListener(new ButtonOnClickListener());
        startTransmit.setOnClickListener(new ButtonOnClickListener());
        offlineLogBt.setOnClickListener(new ButtonOnClickListener());

        logSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SystemPropertiesProxy.set(getApplicationContext(), "persist.sys.bugreportswitch", "open");
                    Toast.makeText(getApplicationContext(), "打开日志抓取功能", Toast.LENGTH_SHORT).show();
                } else {
                    SystemPropertiesProxy.set(getApplicationContext(), "persist.sys.bugreportswitch", "off");
                    Toast.makeText(getApplicationContext(), "关闭日志抓取功能", Toast.LENGTH_SHORT).show();
                }
            }
        });

        logcatSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SystemPropertiesProxy.set(getApplicationContext(), "persist.logd.logpersistd", "logcatd");
                    Toast.makeText(getApplicationContext(), "logcatd进程打开,关掉日志上报功能", Toast.LENGTH_SHORT).show();
                } else {
                    SystemPropertiesProxy.set(getApplicationContext(), "persist.logd.logpersistd", "null");
                    Toast.makeText(getApplicationContext(), "logcatd进程关闭,打开日志上报功能", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private final class ButtonOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {

            if (v.getId() == R.id.updatedata) {
                Toast.makeText(getApplicationContext(), "更新数据按钮", Toast.LENGTH_SHORT).show();
            } else if (v.getId() == R.id.anr) {

                Intent intent = new Intent();
                intent.setAction(spiderman);
                intent.putExtra("tag", "anr");
                intent.putExtra("msg", "模拟anr发送广播成功");
                sendBroadcast(intent);

            } else if (v.getId() == R.id.JavaCrash) {

                Intent intent = new Intent();
                intent.setAction(spiderman);
                intent.putExtra("tag", "java_crash");
                intent.putExtra("msg", "模拟java_crash发送广播成功");
                sendBroadcast(intent);

            } else if (v.getId() == R.id.starttransmit) {

                Intent intent = new Intent();
                intent.setAction(transmitlog);
                intent.putExtra("tag", "transmit_service");
                intent.putExtra("msg", "开启传送服务上传log文件");
                sendBroadcast(intent);

            } else if (v.getId() == R.id.settingpreferences) {

                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);

            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkBoxSwitch();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void checkBoxSwitch() {

        if (SystemPropertiesProxy.get(getApplicationContext(), "persist.sys.bugreportswitch").equals("off")) {
            if (logSwitch != null) {
                logSwitch.setChecked(false);
            }
        } else {
            if (logSwitch != null) {
                logSwitch.setChecked(true);
            }

        }

        if (SystemPropertiesProxy.get(getApplicationContext(), "persist.logd.logpersistd").equals("null")) {
            if (logcatSwitch != null) {
                logcatSwitch.setChecked(false);
            }
        } else {
            if (logcatSwitch != null) {
                logcatSwitch.setChecked(true);
            }

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
