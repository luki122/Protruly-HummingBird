//add for EngineerCode 20111031

package com.android.dialer;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;
import android.widget.Toast;

import static com.android.internal.telephony.TelephonyIntents.SECRET_CODE_ACTION;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ZteEngineerCode {
    /**
     * refer to ZteEngineerCode.java in EngineerCode Application
     */
	private static final String TAG = "ZteEngineerCode";
    private static final String TEST_LIST = "*983*0#";
    //private static final String TEST_LIST_TP = "*983*99#";
    private static final String ZTE_SELF_TEST = "*085*#"; //auto test
    private static final String SELF_TEST = "*983*70#";
    private static final String SELF_TEST_AEON = "*086*#";
    private static final String BATTERY_LOG = "*983*25#";
    private static final String GPS_TEST = "*983*47#";
    //private static final String SMS_STATE = "*983*2#";
    //private static final String SMS_START = "*983*1#";
    //private static final String SMS_STOP = "*983*3#";
    //private static final String SMS_EDITNUMBER = "*983*4#";
    private static final String ENGINEERCODE_LISTVIEW = "*987*0#";
    private static final String PRODUCE_INFO = "*983*154#";
	private static final String SOUDN_RECORD = "*983*11#";
    private static final String BOARD_CODE = "*983*7#";
    private static final String ZTE_VERSION_CMD = "*983*32#";
    private static final String ZTE_CUSTOM_VERSION_CMD = "*983*1275#";
    private static final String AEON_CUSTOM_VERSION_CMD = "*789*1#";
    //private static final String ZTE_HARDWARE_TEST_WIFI = "*983*93#";
    //private static final String ZTE_HARDWARE_TEST_BT = "*983*28#";
    private static final String FACTORY_RECOVER = "*983*57#";
    private static final String FACTORY_RECOVERY_TWO = "*983*987#";
    private static final String MTK_ENGINEERMODE = "*983*3640#";
    private static final String AEON_ENGINEERMODE = "*2366*#";
    //private static final String NETWORK_LOCK_STATE = "*983*239#";
    private static final String AEON_COMPILE_TIME  = "*983*88#";
    private static final String AEON_DEVICE_INFO = "*983*8#";
    private static final String AEON_IMEI_SET = "*#*#4634#*#*";
	private static final String AEON_AGING_TEST = "*983*2#";
	private static final String AEON_AGING_TEST_VR = "*983*3#";
	
    private static final String ACTION_LAUNCHER_TEST_LIST = "com.zte.engineer.action.TEST_LIST";
    private static final String ACTION_SELF_TEST = "com.zte.engineer.action.SELF_TEST";
    private static final String ACTION_BATTERY_LOG = "com.zte.engineer.action.BATTERY_LOG";
	private static final String ACTION_SOUND_RECORD = "com.zte.engineer.action.BATTERY_LOG";
    private static final String ACTION_GPS_TEST = "com.zte.engineer.action.GPS_TEST";

    private static final String OPTION = "option";
    private static final int EDITNUMBER = 1;

    public static boolean handleZteEngineerCode(Context context, String input) {
        if (null == input) {
            return false;
        }
        if (AEON_IMEI_SET.equals(input)) {
           Intent intent = new Intent();
           intent.setClassName("com.zte.engineer","com.zte.engineer.ImeiWriter");
           context.startActivity(intent);
           return true;
        } else if (input.equals(TEST_LIST)) {
            Intent intent = new Intent(ACTION_LAUNCHER_TEST_LIST);
            context.startActivity(intent);
            return true;
        }/* else if (input.equals(TEST_LIST_TP)) {
            File ft_fw_version=new File("/proc/ft_fw_version");
            File ft_fw_vid=new File("/proc/ft_fw_vid");
            String ft_fw_version_str=null;
            String ft_fw_vid_str=null;
            try {
                BufferedReader in_version = new BufferedReader(new FileReader(ft_fw_version));
                ft_fw_version_str = in_version.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                BufferedReader in_vid = new BufferedReader(new FileReader(ft_fw_vid));
                ft_fw_vid_str = in_vid.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("TP Information")
                .setMessage("TP verion: "+ft_fw_version_str+"\n"+"TP ID: "+ft_fw_vid_str)
                .setPositiveButton(android.R.string.ok, null).create().show();
            return true; 
        }*/ else if (input.equals(SELF_TEST) || input.equals(ZTE_SELF_TEST) || input.equals(SELF_TEST_AEON)) {
            Intent intent = new Intent(ACTION_SELF_TEST);
            context.startActivity(intent);
            return true;
        } else if (input.equals(BATTERY_LOG)) {
            Intent intent = new Intent(ACTION_BATTERY_LOG);
            context.startActivity(intent);
            return true;
        } else if (input.equals(SOUDN_RECORD)) {
			Intent intent = new Intent();
			intent.setClassName("com.android.soundrecorder","com.android.soundrecorder.SoundRecorder");
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
		} else if (input.equals(GPS_TEST)) {
            Intent intent = new Intent();
            intent.setClassName("com.mediatek.ygps", "com.mediatek.ygps.YgpsActivity");
            context.startActivity(intent);
            return true;
        } else if (input.equals(ENGINEERCODE_LISTVIEW)) {
            context.startActivity(new Intent("com.zte.engineer.action.EngineerCodeListView"));
            return true;
        } else if (input.equals(PRODUCE_INFO)) {
            Intent intent = new Intent();
            intent.setClassName("com.zte.engineer", "com.zte.engineer.ProduceInfoListView");
            context.startActivity(intent);
            return true;
        } else if (input.equals(BOARD_CODE)) {
            Intent intent = new Intent();
            intent.setClassName("com.zte.engineer", "com.zte.engineer.BoardCode");
            context.startActivity(intent);
            return true;
        } else if (input.equals(ZTE_VERSION_CMD)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("SW VERSION")
                    .setMessage(SystemProperties.get("ro.build.display.id"))
                    .setPositiveButton(android.R.string.ok, null).create().show();
            return true;
        } else if (input.equals(ZTE_CUSTOM_VERSION_CMD) || input.equals(AEON_CUSTOM_VERSION_CMD)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Version Information")
                .setMessage(SystemProperties.get("ro.sw.version")
                            + SystemProperties.get("ro.sw.version.incremental") + "\n"
                                + SystemProperties.get("ro.build.date"))
                    .setPositiveButton(android.R.string.ok, null).create().show();
            return true;
        } else if (input.equals(AEON_COMPILE_TIME)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Compile Time Information")
                .setMessage(SystemProperties.get("ro.build.date"))
                .setPositiveButton(android.R.string.ok, null)
                .create().show();
            return true;
        } else if (input.equals(AEON_DEVICE_INFO)
                && Build.VERSION.SDK_INT >= 23) {
            Intent intent = new Intent();
            intent.setClassName("com.zte.engineer",
                    "com.zte.engineer.DevInfoActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        }
        /* else if (input.equals(ZTE_HARDWARE_TEST_WIFI)) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName("com.mediatek.engineermode", "com.mediatek.engineermode.wifi.WiFi");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } else if (input.equals(ZTE_HARDWARE_TEST_BT)) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName("com.mediatek.engineermode",
                    "com.mediatek.engineermode.bluetooth.BtList");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        }*/ else if ((input.equals(FACTORY_RECOVER)) || (input.equals(FACTORY_RECOVERY_TWO))) {
            Intent factoryIntent = new Intent();
            factoryIntent.setClassName("com.android.settings",
                    "com.android.settings.Settings$PrivacySettingsActivity");
            factoryIntent.putExtra("do_factory_reset", "FactoryMode");
            factoryIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(factoryIntent);
            return true;
        } else if (input.equals(MTK_ENGINEERMODE) || input.equals(AEON_ENGINEERMODE)) {
            Intent intent = new Intent(SECRET_CODE_ACTION,
                    Uri.parse("android_secret_code://3646633"));
            context.sendBroadcast(intent);
            return true;
        } else if (input.equals(AEON_AGING_TEST)) {
            Intent agingTestIntent = new Intent();
            agingTestIntent.setClassName("com.eastaeon.agingtest",
                    "com.eastaeon.agingtest.MainActivity");
            agingTestIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(agingTestIntent);
            return true;
        } else if (input.equals(AEON_AGING_TEST_VR)) {
        	try{
        		Intent agingTestIntent = new Intent();
        		agingTestIntent.setClassName("com.eastaeon.agingtestvr",
        				"com.eastaeon.agingtestvr.StartTest");
        		agingTestIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        		context.startActivity(agingTestIntent);
        		return true;
        	}catch(ActivityNotFoundException e){
        		Log.e(TAG,"e:"+e);
        		Toast.makeText(context, "未找到目标页面,请确认是否已集成", Toast.LENGTH_LONG).show();
        		return true;
        	}
        }/*else if (input.equals(NETWORK_LOCK_STATE)) { 
            Intent intent = new Intent();
            intent.setClassName("com.zte.engineer", "com.zte.engineer.NetlockInfo");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        }*/
        /*PackageInfo sms = null;
        try {
            sms = context.getPackageManager().getPackageInfo("com.zte.smssecurity", 0);
        } catch (NameNotFoundException e) {
        }
        if (null != sms) {
            if (input.equals(SMS_STATE)) {
                Intent intent = new Intent();
                intent.setClassName("com.zte.smssecurity",
                        "com.zte.smssecurity.SMSSecuritySettings");
                context.startActivity(intent);
                return true;
            } else if (input.equals(SMS_START)) {
                context.sendBroadcast(new Intent("com.zte.smssecurity.action.startservice"));
                return true;
            } else if (input.equals(SMS_STOP)) {
                context.sendBroadcast(new Intent("com.zte.smssecurity.action.stopservice"));
                return true;
            } else if (input.equals(SMS_EDITNUMBER)) {
                Intent intent = new Intent();
                intent.setClassName("com.zte.smssecurity",
                        "com.zte.smssecurity.SMSSecuritySettings");
                intent.putExtra(OPTION, EDITNUMBER);
                context.startActivity(intent);
                return true;
            }
        }*/
        return false;
    }

}
