package cn.protruly.spiderman.uploadnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cn.protruly.spiderman.MainActivity;

public class SwitchLogNotification extends BroadcastReceiver {

    private static final String TAG = "SpiderMan";
    private static final String SECRET_CODE_ACTION = "android.provider.Telephony.SECRET_CODE";
    private String action;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        if(intent != null){
            action = intent.getAction();
        }

        if (action.equals(SECRET_CODE_ACTION)) {
            if(intent.getDataString().equals("android_secret_code://789666")) {
                Intent logSwitchIntent = new Intent(context, MainActivity.class);
                logSwitchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(logSwitchIntent);
                Log.v(TAG, "开启上报日志控制开关");
            }
        }
    }
}
