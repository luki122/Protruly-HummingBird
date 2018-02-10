package com.protruly.music.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by hujianwei on 17-8-29.
 */

public class LogUtil {

    //日志开关
    public static boolean DEBUG =true;

    public static final String filepath = "/storage/sdcard0/debug.log";

    private static String PATH_LOGCAT;

    private static LogUtil INSTANCE = null;

    private int mPId;

    private LogDumper mLogDumper = null;

    private static final String TAG="Hb_Music";


    public static void d(String tag,String log){
        if(DEBUG)
            Log.d(TAG, "["+tag+"] "+log);
    }

    public static void e(String tag,String log){
        Log.e(TAG, "["+tag+"] "+log);
    }

    public static void e(String tag,String log,Throwable throwable){
        Log.e(TAG, "["+tag+"] "+log,throwable);
    }


    public static void d(String tag,String log,Throwable tr){
        if(DEBUG)
            Log.d(TAG, "["+tag+"] "+log,tr);
    }

    public static void i(String tag,String log){
        if(DEBUG)
            Log.i(TAG, "["+tag+"] "+log);
    }

    public static void v(String tag,String log){
        if(DEBUG)
            Log.v(TAG, "["+tag+"] "+log);
    }

    public static void iv(String tag,String log){
        if(DEBUG)
            Log.i(TAG, "["+tag+"] "+log);
    }
    /**
     * 控制log开关
     * @return
     */
    public static boolean isOpenLog(){
        File f= new  File(filepath);
        if(f.exists()){
            return true;
        }
        return false;
    }

    public void init(Context context) {

        // 优先保存到SD卡中
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            PATH_LOGCAT = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + File.separator + "MusicLog";
            // 如果SD卡不存在，就保存到本应用的目录下
        } else {
            PATH_LOGCAT = context.getFilesDir().getAbsolutePath()
                    + File.separator + "MusicLog";
        }


        File file = new File(PATH_LOGCAT);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static LogUtil getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new LogUtil(context);
        }
        return INSTANCE;
    }

    private LogUtil(Context context) {
        init(context);
        mPId = android.os.Process.myPid();
    }

    public void start() {
        if (mLogDumper == null)
            mLogDumper = new LogDumper(String.valueOf(mPId), PATH_LOGCAT);
        mLogDumper.start();
    }

    public void stop() {
        if (mLogDumper != null) {
            mLogDumper.stopLogs();
            mLogDumper = null;
        }
    }

    public static String getFileName() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String date = format.format(new Date(System.currentTimeMillis()));
        return date;
    }

    public static String getDateEN() {
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date1 = format1.format(new Date(System.currentTimeMillis()));
        return date1;
    }

    private class LogDumper extends Thread {

        private Process logcatProc;
        private BufferedReader mReader = null;
        private boolean mRunning = true;
        String cmds = null;
        private String mPID;
        private FileOutputStream out = null;

        public LogDumper(String pid, String dir) {
            mPID = pid;
            try {
                out = new FileOutputStream(new File(dir, "Music-"
                        + getFileName() + ".log"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            /**
             *
             * 日志等级：*:v , *:d , *:w , *:e , *:f , *:s
             *
             * 显示当前mPID程序的 E和W等级的日志.
             *
             * */

            // cmds = "logcat *:e *:w | grep \"(" + mPID + ")\"";
            // cmds = "logcat  | grep \"(" + mPID + ")\"";//打印所有日志信息
            // cmds = "logcat -s way";//打印标签过滤信息
            cmds = "logcat *:e *:i *:d | grep \"(" + mPID + ")\"";
        }

        public void stopLogs() {
            mRunning = false;
        }

        @Override
        public void run() {
            try {
                logcatProc = Runtime.getRuntime().exec(cmds);
                mReader = new BufferedReader(new InputStreamReader(logcatProc.getInputStream()), 1024);
                String line = null;
                while (mRunning && (line = mReader.readLine()) != null) {
                    if (!mRunning) {
                        break;
                    }
                    if (line.length() == 0) {
                        continue;
                    }
                    if (out != null && line.contains(mPID)) {
                        out.write((getDateEN() + "  " + line + "\n")
                                .getBytes());
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (logcatProc != null) {
                    logcatProc.destroy();
                    logcatProc = null;
                }
                if (mReader != null) {
                    try {
                        mReader.close();
                        mReader = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    out = null;
                }
            }
        }
    }
}
