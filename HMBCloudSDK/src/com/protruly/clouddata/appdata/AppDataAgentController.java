 package com.protruly.clouddata.appdata;
 
 import android.annotation.TargetApi;
 import android.content.Context;
 import android.content.SharedPreferences;
 import android.content.SharedPreferences.Editor;
 import android.content.pm.ApplicationInfo;
 import android.location.Location;
 import android.net.TrafficStats;
 import android.os.Build;
 import android.os.Build.VERSION;
 import android.os.Bundle;
 import android.os.Environment;
 import android.os.Handler;
 import android.os.HandlerThread;
 import android.os.Message;
 import android.text.TextUtils;
 import android.widget.Toast;
 import com.protruly.clouddata.appdata.common.CachedFileUtils;
 import com.protruly.clouddata.appdata.common.CommonUtils;
 import com.protruly.clouddata.appdata.common.ConfigUtil;
 import com.protruly.clouddata.appdata.common.Log;
 import com.protruly.clouddata.appdata.common.MD5Utils;
 import com.protruly.clouddata.appdata.common.NetworkUtils;
 import com.protruly.clouddata.appdata.common.OnlineConfigUtil;
 import com.protruly.clouddata.appdata.common.PreferencesUtils;
 import com.protruly.clouddata.appdata.listener.AppDataAgentCrashHandlerListener;
 import com.protruly.clouddata.appdata.listener.OnlineConfigureListener;
 import com.protruly.clouddata.appdata.object.GpuInfo;
 import com.protruly.clouddata.appdata.object.TrafficInfo;
 import java.net.URLEncoder;
 import java.util.ArrayList;
 import java.util.Map;
 import javax.microedition.khronos.opengles.GL10;
 import org.json.JSONArray;
 import org.json.JSONException;
 import org.json.JSONObject;
 
 
 
 public class AppDataAgentController
   implements AppDataAgentCrashHandlerListener
 {
   static final int UNDEFINED = -1;
   static final int BATCH_AT_LAUNCH = 1;
   int reportPolicy = 1;
   private String appKey;
   private String channel;
   private boolean isOpenActivitiesTrack = true;
   private boolean isAutoLocation = true;
   private String currentClassName;
   private GpuInfo gpuInfo;
   private EventHandler eventHandler;
   private Handler handler;
   private Object lock = new Object();
   private boolean isPressData = false;
   private boolean upgradeOnlyWifi = true;
   private JSONObject cfgData = null;
   private int updateOnlineStatus = 0;
   public static final Object LOCK = new Object();
   int MAX_REQUEST_TIME = 10000;
   
 
 
   public static String FORCE_INSTALL_DOWNLOAD_APK_PATH = "";
   public static String DOWNLOAD_APK_SDCARD_PATH = "";
   public static String DOWNLOAD_APK_SELF_PATH = "";
   private static final String APK_DIR = "/apk/";
   Handler hd = null;
   Handler feedbackHandler = null;
   int state = 0;
   Handler dlgkHandler = null;
   private final int DLG_SHOW = 1;
   private final int DLG_MISS = 0;
   
   private boolean isToastShow = false;
   private String toastTip = "当前版本已经是最新版本！";
   
   
   private static long UPDATE_HTTPTIME_GAP_LONG = 1296000000L;
   
   AppDataAgentController() {
     HandlerThread localHandlerThread = new HandlerThread("AppDataAgent");
     localHandlerThread.start();
     this.handler = new Handler(localHandlerThread.getLooper());
     this.eventHandler = new EventHandler(this.handler);
   }
   

   
      
   
    
   void setAppKey(String appKey) {
     this.appKey = appKey;
   }
   
   private String getAppkey(Context context) {
     return this.appKey == null ? CommonUtils.getAppKey(context) : this.appKey;
   }
   
   void setChannel(String channel) {
     this.channel = channel;
   }
   
   private String getChannel(Context context) throws Exception {
     if (this.channel == null)
       this.channel = CommonUtils.getChannel(context);
     if (CommonUtils.inputCheck(this.channel)) {
       if (this.channel.length() > 20)
         this.channel = this.channel.substring(0, 20);
     } else
       throw new Exception("The value of channel is illegal.");
     return this.channel;
   }
   
   
   void setPressData(boolean isPressData) {
     this.isPressData = isPressData;
   }

   private JSONObject getHeaderInfo(Context context, boolean paramBoolean) {
     JSONObject localJSONObject = new JSONObject();
     String strAppDeviceId = CommonUtils.getAppDeviceId(context);
     
 
     String userCode = CommonUtils.getUserCode(CommonUtils.getProductName(), strAppDeviceId);
     String strAppKey = getAppkey(context);
     if (TextUtils.isEmpty(strAppKey)) {
       Log.error("AppDataAgent", "Appkey is null, empty or incorrent.");
       return null;
     }
     
     try
     {
       localJSONObject.put("userCode", userCode);
       localJSONObject.put("appKey", strAppKey);
       localJSONObject
         .put("appVerName", CommonUtils.getAppVersionName(context));
       if (ConfigUtil.isFirstSent(context))
       {
         localJSONObject.put("pkgName", CommonUtils.getPackageName(context));
         localJSONObject.put("osType", "Android");
         localJSONObject.put("resolution", CommonUtils.getResolution(context));
         localJSONObject.put("model", CommonUtils.getProductName());
         localJSONObject.put("carrier", CommonUtils.getOperatorName(context));
         localJSONObject.put("appName", CommonUtils.getAppName(context));
         localJSONObject.put("sn", CommonUtils.getSN());
         localJSONObject.put("deviceId", CommonUtils.getAppDeviceId(context));
         localJSONObject.put("manufacturer", CommonUtils.getManufacturer());
         localJSONObject.put("brand", Build.BRAND);
         localJSONObject.put("board", Build.BOARD);
         String strMac = CommonUtils.getMacAdress(context);
         if (!TextUtils.isEmpty(strMac)) {
           localJSONObject.put("mac", strMac);
         }
         localJSONObject.put("cpu", CommonUtils.getCupInfo());
         if (this.gpuInfo != null) {
           if (!TextUtils.isEmpty(this.gpuInfo.vendor))
             localJSONObject.put("gpuVendor", this.gpuInfo.vendor);
           if (!TextUtils.isEmpty(this.gpuInfo.renderer))
             localJSONObject.put("gpuRenderer", this.gpuInfo.renderer);
         }
       }
       localJSONObject.put("sdkVerCode", AppDataAgent.getSDKVersion());
       localJSONObject.put("country", CommonUtils.getCountry(context));
       localJSONObject.put("timezone", CommonUtils.getTimeZone(context));
       localJSONObject.put("channel", getChannel(context));
       localJSONObject.put("language", CommonUtils.getLanguage(context));
       
       localJSONObject.put("osVer", CommonUtils.getOSVersion());
       localJSONObject.put("cpbVer", CommonUtils.getCPBVersion());
       localJSONObject.put("imsi", CommonUtils.getIMSI(context));
       localJSONObject.put("phoneNum", CommonUtils.getPhoneNum(context));
       
       String[] arrayOfString = CommonUtils.getNetWorkInfo(context);
       if (arrayOfString != null) {
         if (!arrayOfString[0].equals("Wi-Fi")) {
           localJSONObject.put("netType", arrayOfString[0]);
           if (!paramBoolean)
             localJSONObject.put("netSub", arrayOfString[1]);
         } else {
           localJSONObject.put("netType", arrayOfString[0]);
         }
       } else
         localJSONObject.put("netType", "Unknown");
       if (!paramBoolean)
       {
 
 
         localJSONObject.put("appVerCode", 
           CommonUtils.getAppVersionCode(context));
         
 
 
 
 
 
 
         SharedPreferences sharedPreferences = 
           PreferencesUtils.getAgentUserPreferences(context);
         if (sharedPreferences.getInt("gender", -1) != -1)
           localJSONObject.put("sex", 
             sharedPreferences.getInt("gender", -1));
         if (sharedPreferences.getInt("age", -1) != -1) {
           localJSONObject.put("age", 
             sharedPreferences.getInt("age", -1));
         }
         if (!"".equals(sharedPreferences.getString("user_id", ""))) {
           localJSONObject.put("userid", sharedPreferences.getString(
             "user_id", ""));
         }
         if (!"".equals(sharedPreferences.getString("id_source", "")))
           localJSONObject.put("uidsource", 
             URLEncoder.encode(sharedPreferences.getString(
             "id_source", "")));
         SharedPreferences localSharedPreferences2 = PreferencesUtils.getAgentStatePreferences(context);
         
         long l = PreferencesUtils.getLongPrefProp(localSharedPreferences2, "last_req_time", 0L);
         if (l != 0L)
           localJSONObject.put("last_req_time", l);
       }
       return localJSONObject;
     } catch (Exception localException) {
       Log.error("AppDataAgent", "Exception occurred in getHeaderInfo", localException);
     }
     return null;
   }
   
   void setDefaultReportPolicy(int policy) {
     this.reportPolicy = policy;
   }
   


   void updateOnlineConfig(final Context context)
   {
     this.updateOnlineStatus = -1;
     new Thread() {
       public void run() {
         try {
           Thread.sleep(1000L);
         }
         catch (InterruptedException e) {
           e.printStackTrace();
         }
         synchronized (AppDataAgentController.LOCK) {
           AppDataAgentController.this.cfgData = OnlineConfigUtil.getOnlinePara(context);
           
           if (AppDataAgentController.this.cfgData != null) {
             Log.info("AppDataAgent", "updateOnlineConfig, cfgData:" + AppDataAgentController.this.cfgData.toString());
						try {
							if (cfgData.getInt("retCode") == 0) { 
								String kv = cfgData.getString("kv");
								if (!TextUtils.isEmpty(kv)) {

									Log.info("AppDataAgent", "kv:" + kv);
			             AppDataAgentController.this.updateOnlineStatus = OnlineConfigUtil.saveOnlinePara(context, new JSONObject(kv));
			             Log.info("AppDataAgent", "updateOnlineStatus:" + AppDataAgentController.this.updateOnlineStatus);

	        Log.info("AppDataAgent", "new dd:" + AppDataAgent.getConfigParams(context, "dd"));
								}
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
           } else {
             Log.info("AppDataAgent", "updateOnlineConfig, cfgData get failed");
           }
           AppDataAgentController.LOCK.notifyAll();
           Log.info("AppDataAgent", "AppDataAgentController.LOCK.notifyAll");
         }
       }
     }.start();
   }

   void setOnlineConfigureListener(Context context, final OnlineConfigureListener listener)
   {
     new Thread()
     {
       public void run()
       {
         Log.info("AppDataAgent", "===setOnlineConfigureListener run");
         synchronized (AppDataAgentController.LOCK)
         {
           Log.info("AppDataAgent", "===setOnlineConfigureListener begin");
           try {
             AppDataAgentController.LOCK.wait(AppDataAgentController.this.MAX_REQUEST_TIME);
           } catch (InterruptedException e) {
             e.printStackTrace();
           }
           Log.info("AppDataAgent", "===setOnlineConfigureListener end");
         }
         
 
         Log.info("AppDataAgent", "update Online complete");
         if (AppDataAgentController.this.updateOnlineStatus == 2)
         {
           Log.info("AppDataAgent", "Online param have changed.........");
           listener.onCfgChanged(AppDataAgentController.this.cfgData);
         }
       }
     }.start();
   }

   String getConfigParams(Context context, String param)
   {
     return OnlineConfigUtil.getConfigParams(context, param);
   }






@Override
public void onAppCrash(Context paramContext) {
	// TODO Auto-generated method stub
	
}


 }
