package com.hb.thememanager.manager;

import android.content.SharedPreferences;
import android.os.Debug;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.Log;

import com.hb.thememanager.model.Theme;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by alexluo on 17-8-31.
 */

public class TimerManager {

    public static final int MAX_TRY_TIME = 5*60*1000;

    public static final String TIMER_ID = "timer_id";
    public static final String TIMER_TYPE = "timer_type";
    public static final String TIMER_STATE = "timer_state";
    public static final String TIMER_FILE_PATH = "file_path";
    public static final String TIMER_START_TIME = "start_time";
    public static final String TIMER_LEFT_TIME = "left_time";
    public static final String TIMER_SETUP_LENGTH = "setup_length";
    public static final String TIMER_ORIGINAL_LENGTH = "original_length";

    /**
     * 将计时器写入sharepreference中
     * @param prefs
     * @param timerObj
     */
    public static void putTimersInSharedPrefs(SharedPreferences prefs, TimerObj timerObj){
        final SharedPreferences.Editor editor = prefs.edit()
                .putString(TIMER_ID+timerObj.type,timerObj.id)
                .putInt(TIMER_TYPE+timerObj.type,timerObj.type)
                .putLong(TIMER_START_TIME+timerObj.type,timerObj.startTime)
                .putLong(TIMER_LEFT_TIME+timerObj.type,timerObj.leftTime)
                .putLong(TIMER_SETUP_LENGTH+timerObj.type,timerObj.setupLength)
                .putLong(TIMER_ORIGINAL_LENGTH+timerObj.type,timerObj.originalLength)
                .putInt(TIMER_STATE+timerObj.type,timerObj.state)
                .putString(TIMER_FILE_PATH+timerObj.type,timerObj.filePath);
        editor.apply();
    }

    public static String getLeftTime(long leftTime){
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
        Date date = new Date(leftTime);
        return sdf.format(date);
    }

    /**
     * 从SharePreference中读取计时器对象
     * @param prefs
     * @return
     */
    public static TimerObj getTimerFromSharedPrefs(SharedPreferences prefs, String timerId) {
        return  readFromSharedPref(prefs,timerId);
    }

    private static TimerObj readFromSharedPref(SharedPreferences prefs,String timerId) {
        TimerObj timerObj = new TimerObj();
        timerObj.id = prefs.getString(TIMER_ID + timerId,"");
        timerObj.startTime = prefs.getLong(TIMER_START_TIME + timerId, 0);
        timerObj.leftTime = prefs.getLong(TIMER_LEFT_TIME + timerId, 0);
        timerObj.originalLength = prefs.getLong(TIMER_ORIGINAL_LENGTH + timerId, 0);
        timerObj.setupLength = prefs.getLong(TIMER_SETUP_LENGTH + timerId, 0);
        timerObj.type = prefs.getInt(TIMER_TYPE + timerId, 0);
        return  timerObj;
    }


    /**
     * 从SharePreference中删除计时器对象
     * @param prefs
     * @param timerObj
     * @return
     */
    public static boolean deleteFromSharedPref(SharedPreferences prefs,TimerObj timerObj) {

        final SharedPreferences.Editor editor = prefs.edit()
                .remove(TIMER_ID+timerObj.type)
                .remove(TIMER_TYPE+timerObj.type)
                .remove(TIMER_START_TIME+timerObj.type)
                .remove(TIMER_LEFT_TIME+timerObj.type)
                .remove(TIMER_SETUP_LENGTH+timerObj.type)
                .remove(TIMER_ORIGINAL_LENGTH+timerObj.type)
                .remove(TIMER_FILE_PATH+timerObj.type);
        return editor.commit();
    }


    /**
     * 获取当前时间
     * @return
     */
    public static long getTimeNow() {
        return SystemClock.elapsedRealtime();
    }

    /**
     * Timer object for try to apply theme or font
     */
    public static class TimerObj implements Parcelable {

        public static final int STATE_RUNNING = 1;
        public static final int STATE_STOPPED = 2;
        public static final int STATE_TIMESUP = 3;
        public static final int STATE_DELETED = 4;
        public String id;//与之相对应的主题或者字体的ID
        public int type;//与之相对应的主题或者字体的类型
        public long startTime;//开始试用的时间
        public long leftTime;//试用剩下的时间
        public long setupLength;
        public long originalLength;
        public int state;
        public String filePath;
        public TimerObj(){
            this.id = "-1";
            this.type = -1;
            this.filePath = "";
            startTime = 0;
            leftTime = originalLength = setupLength = 0;
        }


        public TimerObj(Theme theme){
            this.id = theme.id;
            this.type = theme.type;
            this.filePath = theme.themeFilePath;
            startTime = getTimeNow();
            leftTime = originalLength = setupLength = MAX_TRY_TIME;
        }

        public void updateState(int state){
            this.state = state;

        }


        protected TimerObj(Parcel in) {
            id = in.readString();
            type = in.readInt();
            startTime = in.readLong();
            leftTime = in.readLong();
            filePath = in.readString();
        }

        public long updateTimeLeft(boolean forceUpdate) {
            if (forceUpdate) {
                long millis = getTimeNow();
                leftTime = originalLength - (millis - startTime);
            }
            return leftTime;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(id);
            dest.writeInt(type);
            dest.writeLong(startTime);
            dest.writeLong(leftTime);
            dest.writeString(filePath);
        }


        public long getTimesupTime() {
            return startTime + originalLength;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<TimerObj> CREATOR = new Creator<TimerObj>() {
            @Override
            public TimerObj createFromParcel(Parcel in) {
                return new TimerObj(in);
            }

            @Override
            public TimerObj[] newArray(int size) {
                return new TimerObj[size];
            }
        };


        @Override
        public String toString() {
            return "TimerObj{" +
                    "id='" + id + '\'' +
                    ", type=" + type +
                    ", startTime=" + startTime +
                    ", leftTime=" + leftTime +
                    ", setupLength=" + setupLength +
                    ", originalLength=" + originalLength +
                    '}';
        }
    }





}
