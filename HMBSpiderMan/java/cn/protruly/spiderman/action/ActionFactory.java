package cn.protruly.spiderman.action;

import android.content.Context;
import android.content.Intent;


/**
 * Created by lijia on 17-5-13.
 */

public class ActionFactory {

    private final String TAG = "SpiderMan";
    private String action;
    private Context mContext;

    public ActionFactory() {

    }

    public void creator(final Intent intent, Context context) {

        action = intent.getAction();
        mContext = context;

        if (action.equals("cn.proturly.spiderman")) {
            String tag = intent.getStringExtra("tag");
            if (tag.equals("anr")) {
                ANRAction anrAction = new ANRAction(mContext, tag, "null", "null", "null");
            } else if (tag.equals("java_crash")) {
                JavaCrashAction javaCrashAction = new JavaCrashAction(mContext, tag, "null", "null", "null");
            }
        } else if (action.equals("android.intent.action.PROTRULYBUGREPORT")) {

            String type = intent.getStringExtra("TYPE");
            String packageName = intent.getStringExtra("PACKAGENAME");
            String erroReason = intent.getStringExtra("REASON");
            String crashReason = intent.getStringExtra("EXCEPTIONSTACK");
            String headersReason = intent.getStringExtra("HEADERS");
            String filepath = intent.getStringExtra("FILEPATH");
            String erroInfo;

            if (erroReason != null && !erroReason.equals("UNKNOWN") && crashReason.equals("UNKNOWN")) {
                erroInfo = erroReason;
            } else if (crashReason != null && !crashReason.equals("UNKNOWN")) {
                erroInfo = crashReason;
            } else if (headersReason != null) {
                erroInfo = headersReason;
            } else {
                erroInfo = "UNKNOWN";
            }

            if (type.equalsIgnoreCase("anr")) {
                Action anrAction = new ANRAction(mContext, type, packageName, erroInfo, filepath);
                //    Toast.makeText(mContext, "发生ANR问题" + packageName, Toast.LENGTH_SHORT).show();
            } else if (type.equalsIgnoreCase("crash")) {
                Action javaCrashAction = new JavaCrashAction(mContext, type, packageName, erroInfo, filepath);
                //    Toast.makeText(mContext, "发生CRASH问题" + packageName, Toast.LENGTH_SHORT).show();
            } else if (type.equalsIgnoreCase("lowmem")) {
                Action lowmemAction = new LowmemAction(mContext, type, packageName, erroInfo, filepath);
                //    Toast.makeText(mContext, "发生LOWMEM问题" + packageName, Toast.LENGTH_SHORT).show();
            } else if (type.equalsIgnoreCase("wtf")) {
                Action wtfAction = new WTFAction(mContext, type, packageName, erroInfo, filepath);
                //    Toast.makeText(mContext, "发生WTF问题" + packageName, Toast.LENGTH_SHORT).show();
            } else if (type.equalsIgnoreCase("TOMBSTONE")) {
                Action TombstoneAction = new TombstoneAction(mContext, type, packageName, erroInfo, filepath);
                //    Toast.makeText(mContext, "发生Tombstone问题" + packageName, Toast.LENGTH_SHORT).show();
            } else if (type.equalsIgnoreCase("watchdog")) {
                Action watchdotAction = new WatchdogAction(mContext, type, packageName, erroInfo, filepath);
                //    Toast.makeText(mContext, "发生Tombstone问题" + packageName, Toast.LENGTH_SHORT).show();
            }

        }
    }

}
