package cn.com.protruly.soundrecorder.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadataRetriever;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.text.Collator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cn.com.protruly.soundrecorder.common.BaseActivity;
import cn.com.protruly.soundrecorder.R;

/**
 * Created by wenwenchao on 17-8-24.
 */

public class GlobalUtil {

    public static  SimpleDateFormat FormatDate_Y_M_d = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
    public static  SimpleDateFormat FormatDate_M_d = new SimpleDateFormat("MM月dd日", Locale.getDefault());
    public static  SimpleDateFormat FormatDate_M_d_w = new SimpleDateFormat("MM月dd日 E", Locale.getDefault());


    private static final String STORAGE_PATH = Environment.getExternalStorageDirectory().getPath();
    private static final String DATA_PATH = Environment.getDataDirectory().getPath();
    private static final String STORAGE_STATE = Environment.getExternalStorageState();
    private static final int MIN_BATTERY_LEVEL = 3;
    private static final int MAX_BATTERY_LEVEL = 100;
    private static final long MIN_SPACE_LEVEL = 80*1024*1024;
    public static final String NAME = "RecordDir";


    public static final SimpleDateFormat FormatDate_H_m = new SimpleDateFormat("HH:mm", Locale.getDefault());
    public static final SimpleDateFormat FormatTime_H_m_s = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    public static final SimpleDateFormat FormatDateAndTime = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss", Locale.getDefault());
    public static final Collator mNameComparator = Collator.getInstance(Locale.getDefault());
    public static final Comparator<RecordFileInfo> mNumComparator = new Comparator<RecordFileInfo>() {
        @Override
        public int compare(RecordFileInfo f1, RecordFileInfo f2) {
            if (f1!=null && f2 != null) {
                return f1.getModifiedTime() < f2.getModifiedTime() ? 1 : -1;
            }
            return 0;
        }
    };
    public static final Comparator<Long> mLongListComparator = new Comparator<Long>() {
        @Override
        public int compare(Long aLong, Long t1) {
            return aLong>t1?1:-1;
        }
    };

    public static String getRecordDirPath() {
        StringBuilder sb = new StringBuilder();
        if (STORAGE_STATE.equals(
                Environment.MEDIA_MOUNTED)) {
            sb.append(STORAGE_PATH);
        } else {
            sb.append(DATA_PATH);
        }
        sb.append(File.separator);
        sb.append(NAME);
        File recordDir = new File(sb.toString());
        if(!recordDir.exists()){
            try{
                recordDir.mkdir();
            }catch (Exception E){
                Log.d("GlobalUtil","创建录音文件夹异常！");
                return null;
            }
        }
        //sb.append(File.separator);
        return sb.toString();
    }

    public static long getTimeLongForAudio(RecordFileInfo file){
        long duration = -1;
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(file.getPath());
            String strDuration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            duration = Long.valueOf(strDuration);
            file.setTimeLong(duration);
        }catch (Exception e){
            return -1;
        }
        return duration;
    }

    public static String formatLongTimeString(long time){         //00:00:00
        long formate_time = time/1000L;
        long hours = formate_time/3600L;
        long minutes = (formate_time%3600L)/60L;
        long seconds = formate_time%3600L%60L;
        String hour_format;
        String minites_format;
        String second_format;
        if(hours<10 && hours>=0){
            hour_format = "0"+hours;
        }else{
            hour_format = ""+hours;
        }
        if(minutes<10 && minutes>=0){
            minites_format = "0"+minutes;
        }else{
            minites_format = ""+minutes;
        }
        if(seconds<10 && seconds>=0){
            second_format = "0"+seconds;
        }else{
            second_format = ""+seconds;
        }
        return hour_format+":"+minites_format+":"+second_format;
    }

    public static String formatTime_m_s(long time){
        long formate_time = time/1000L;
      //  long hours = formate_time/3600L;
        long minutes = formate_time/60L;
        long seconds = formate_time%60L;

        String minites_format;
        String second_format;

        if(minutes<10 && minutes>=0){
            minites_format = "0"+minutes;
        }else {
            minites_format = ""+minutes;
        }
        if(seconds<10 && seconds>=0){
            second_format = "0"+seconds;
        }else{
            second_format = ""+seconds;
        }
        return minites_format+":"+second_format;
    }

    public static boolean isTimeCountAdvance(long before,long after){
        long first = before / 1000;
        long second = after / 1000;
        return first != second;
    }

    public static long FormatTime(String timeStr){
        long timeLong = 0;
        String[] tim = timeStr.split(":");
        if(2 == tim.length) {
            timeLong = Long.valueOf(tim[0]) * 60 + Long.valueOf(tim[1]);
        }
        return timeLong;
    }

    public static String formatDate_Y_M_d(Context context,long time) {
        if(context!=null){
            FormatDate_Y_M_d = new SimpleDateFormat(context.getResources().getString(R.string.FormatDate_Y_M_d), Locale.getDefault());
        }
        Date date = new Date(time);
        return FormatDate_Y_M_d.format(date);
    }
    public static String formatDate_M_d(Context context,long time) {
        if(context!=null){
            FormatDate_M_d = new SimpleDateFormat(context.getResources().getString(R.string.FormatDate_M_d), Locale.getDefault());
        }
        Date date = new Date(time);
        return FormatDate_M_d.format(date);
    }
    public static String formatDate_M_d_w(Context context,long time) {
        if(context!=null){
            FormatDate_M_d_w = new SimpleDateFormat(context.getResources().getString(R.string.FormatDate_M_d_w), Locale.getDefault());
        }
        Date date = new Date(time);
        return FormatDate_M_d_w.format(date);
    }

    public static String formatDate_H_m(long time) {
        Date date = new Date(time);
        return FormatDate_H_m.format(date);
    }

    public static String formatTime_H_m_s(long time) {
        Date date = new Date(time);
        return FormatTime_H_m_s.format(date);
    }



    public static String formatDateAndTime(long time) {
        Date date = new Date(time);
        return FormatDateAndTime.format(date);
    }

    public static int getDayFromNow(long time) throws ParseException {

        Calendar pre = Calendar.getInstance();
        Date predate = new Date(System.currentTimeMillis());
        pre.setTime(predate);
        Calendar cal = Calendar.getInstance();
        String formattime = formatDateAndTime(time);
        Date date = getDateFormat().parse(formattime);
        cal.setTime(date);
        if (cal.get(Calendar.YEAR) == (pre.get(Calendar.YEAR))) {
            int diffDay = pre.get(Calendar.DAY_OF_YEAR)-cal.get(Calendar.DAY_OF_YEAR);
            return diffDay;
        }else {
            return -1;
        }
    }

    public static int getWeekDay(long time) throws ParseException{
        Calendar cal = Calendar.getInstance();
        String formattime = formatDateAndTime(time);
        Date date = getDateFormat().parse(formattime);
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_WEEK);
    }


    public static SimpleDateFormat getDateFormat() {
        if (null == DateLocal.get()) {
            DateLocal.set(new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA));
        }
        return DateLocal.get();
    }
    private static ThreadLocal<SimpleDateFormat> DateLocal = new ThreadLocal<SimpleDateFormat>();






    public static void sortFileListByTime(List<RecordFileInfo> list) {
        Collections.sort(list,mNumComparator);
    }
    public static void sortFileListByName(List<RecordFileInfo> list) {
        Collections.sort(list,mNameComparator);
    }
    public static void sortLongList(List<Long> list) {
        Collections.sort(list,mLongListComparator);
    }


    public static boolean isLowPwer(Context context){
        return getBatteryLevel(context) <= MIN_BATTERY_LEVEL;
    }

    private static int getBatteryLevel(Context context){
        IntentFilter batteryFilter = new IntentFilter();
        batteryFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = context.registerReceiver(null,batteryFilter);
        if(null != intent){
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE,MAX_BATTERY_LEVEL);
            return (level * MAX_BATTERY_LEVEL) / scale;
        }else{
            return MAX_BATTERY_LEVEL;
        }
    }

    public static boolean isLowerSpace(){
        return getFreeSpace("/storage/emulated/0") <= MIN_SPACE_LEVEL;

    }

    private static long getFreeSpace(String path){
        if(null == path) return 0;
        try {
            StatFs statFs = new StatFs(path);
            long avail_blocks = statFs.getAvailableBlocksLong();
            long block_size = statFs.getBlockSizeLong();
            return avail_blocks * block_size;
        }catch(Exception e){
            Log.d("bql","getFreeSpace Exception:"+ e);
        }
        return 0;
    }



}
