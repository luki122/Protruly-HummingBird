package cn.com.protruly.soundrecorder.util;

/**
 * Created by wenwenchao on 2017/8/15.
 */

public class GlobalConstant {

    public static int PLAY_START = 1;
    public static int PLAY_GOING = 11;
    public static int PLAY_PAUSE = 2;
    public static int PLAY_STOP = 3;
    public static int PLAY_ERROR = 4;
    public static int PLAY_TIME = 5;
    public static int RECORD_START = 6;
    public static int RECORD_PAUSE = 7;
    public static int RECORD_STOP = 8;
    public static int RECORD_ERROR = 9;
    public static int RECORD_TIME = 10;

    public static long RECORD_MAXTIME = 120*60*1000;

    public static final long NOW = System.currentTimeMillis()/1000;
    public static final long JUSTNOW = NOW-5*60;

    public static final int WaveHorRate  = 3;
    public static final int WaveVerRate  = 400;



}
