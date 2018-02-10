package cn.com.protruly.soundrecorder.util;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class LogUtil {
	
    private static Boolean LOG_SWITCH = true;
    private static Boolean LOG_WRITE_TO_FILE = false;
      
    private static int SDCARD_LOG_FILE_SAVE_DAYS = 0;              
  
    private static String LOGFILENAME = "UM";
    //private static String LOG_PATH_SDCARD_DIR = "/mnt/sdcard/1MyLog";         
    
    private static String LOG_PATH_SDCARD_DIR = "/storage/emulated/0/Log";
      
    private static SimpleDateFormat LogSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static SimpleDateFormat logfile = new SimpleDateFormat("yyyy-MM-dd");
  
    public static void w(String tag, String text) {
        log(tag, text, 'w'); 
    } 
  
    public static void e(String tag, String text) {
        log(tag, text, 'e'); 
    } 
  
    public static void d(String tag, String text) {
        log(tag, text, 'd'); 
    } 
  
    public static void i(String tag, String text) {
        log(tag, text, 'i'); 
    } 
  
    public static void v(String tag, String text) {
        log(tag, text, 'v'); 
    } 
    
    public static void i2(String tag, String text) {
    	RuntimeException e = new RuntimeException("-->This RuntimeException is [JUST FOR TEST]");
    	e.fillInStackTrace();
        log(tag, text, 'i', e); 
    } 
    
    public static void e2(String tag, String text) {
    	RuntimeException e = new RuntimeException("JustForTest");
    	e.fillInStackTrace();
        log(tag, text, 'e'); 
    } 

    private static void log(String tag, String msg, char level) {
        if (LOG_SWITCH) { 
            if ('i' == level) { 
                Log.i(tag, msg);
            } else if ('e' == level) { 
                Log.e(tag, msg);
            } else if ('w' == level) { 
                Log.w(tag, msg);
            } else if ('d' == level) { 
                Log.d(tag, msg);
            }else { 
                Log.v(tag, msg);
            } 
            if (LOG_WRITE_TO_FILE) 
                writeLogtoFile(String.valueOf(level), tag, msg);
        } 
    } 
    
    private static void log(String tag, String msg, char level, Exception e) {
        if (LOG_SWITCH) { 
            if ('i' == level) { 
                Log.i(tag, msg, e);
            } else if ('e' == level) { 
                Log.e(tag, msg, e);
            } else if ('w' == level) { 
                Log.w(tag, msg, e);
            } else if ('d' == level) { 
                Log.d(tag, msg, e);
            }else { 
                Log.v(tag, msg, e);
            } 
              
            if (LOG_WRITE_TO_FILE) 
                writeLogtoFile(String.valueOf(level), tag, msg + "\n" + e.toString());
        } 
    } 

    private static void writeLogtoFile(String mylogtype, String tag, String text) {
		try {
			Date nowtime = new Date();
			String needWriteFiel = logfile.format(nowtime);
			String needWriteMessage = LogSdf.format(nowtime) + " " + mylogtype
					+ " " + tag + " " + text;
			File dir = new File(LOG_PATH_SDCARD_DIR);
			File file = new File(LOG_PATH_SDCARD_DIR, LOGFILENAME + "_Log.txt");
			if(! dir.exists() || !dir.isDirectory()) {
				dir.mkdirs();
			}
			if (!file.exists()) {
				file.createNewFile();
			}

			FileWriter filerWriter = new FileWriter(file, true);
			BufferedWriter bufWriter = new BufferedWriter(filerWriter);
			bufWriter.write(needWriteMessage);
			bufWriter.newLine();
			bufWriter.close();
			filerWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    } 

    public static void delFile() { 
        String needDelFiel = logfile.format(getDateBefore());
        File file = new File(LOG_PATH_SDCARD_DIR, LOGFILENAME + needDelFiel +".txt");
        if (file.exists()) { 
            file.delete(); 
        } 
    } 

    private static Date getDateBefore() {
        Date nowtime = new Date();
        Calendar now = Calendar.getInstance();
        now.setTime(nowtime); 
        now.set(Calendar.DATE, now.get(Calendar.DATE) - SDCARD_LOG_FILE_SAVE_DAYS);
        return now.getTime(); 
    } 
}