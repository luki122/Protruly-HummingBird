 package com.protruly.clouddata.appdata.common;
 
 public class Log {
   public static boolean LOG = true;
   
   public static void info(String tag, String msg) {
     if (LOG) {
       android.util.Log.i(tag, msg);
     }
   }
   
   public static void info(String tag, String msg, Exception paramException) {
     if (LOG)
       android.util.Log.i(tag, paramException.toString() + ":  [" + 
         msg + "]");
   }
   
   public static void error(String tag, String msg) {
     if (LOG) {
       android.util.Log.e(tag, msg);
     }
   }
   
   public static void error(String tag, String msg, Exception paramException) {
     if (LOG)
       android.util.Log.e(tag, paramException.toString() + ":  [" + 
         msg + "]", paramException);
   }
   
   public static void debug(String tag, String msg) {
     if (LOG) {
       android.util.Log.d(tag, msg);
     }
   }
   
   public static void debug(String tag, String msg, Exception paramException) {
     if (LOG)
       android.util.Log.d(tag, paramException.toString() + ":  [" + 
         msg + "]");
   }
   
   public static void verbose(String tag, String msg) {
     if (LOG) {
       android.util.Log.v(tag, msg);
     }
   }
   
   public static void verbose(String tag, String msg, Exception paramException) {
     if (LOG)
       android.util.Log.v(tag, paramException.toString() + ":  [" + 
         msg + "]");
   }
   
   public static void warn(String tag, String msg) {
     if (LOG) {
       android.util.Log.w(tag, msg);
     }
   }
   
   public static void warn(String tag, String msg, Exception paramException) {
     if (LOG)
       android.util.Log.w(tag, paramException.toString() + ":  [" + 
         msg + "]", paramException);
   }
   
   public static boolean isDebugMode() {
     return LOG;
   }
 }
