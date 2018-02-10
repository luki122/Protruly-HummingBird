package com.protruly.clouddata.appdata;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import com.protruly.clouddata.appdata.common.Log;
import com.protruly.clouddata.appdata.common.PreferencesUtils;
import com.protruly.clouddata.appdata.listener.OnlineConfigureListener;
import com.protruly.clouddata.appdata.object.Gender;
import java.util.Map;
import java.util.Map.Entry;
import javax.microedition.khronos.opengles.GL10;








public class AppDataAgent
{
   private static AppDataAgentController controller = new AppDataAgentController();
  
  static AppDataAgentController getAgentController()
  {
     return controller;
  }
  
  public static void setPressData(boolean isPressData)
  {
     controller.setPressData(isPressData);
  }
  

  public static void setDebugMode(boolean isDebugMode)
  {
     Log.LOG = isDebugMode;
  }
  


  public static void setAppKey(String appKey)
  {
     controller.setAppKey(appKey);
  }
  

  public static void setChannel(String channel)
  {
     controller.setChannel(channel);
  }
  

  
  public static void setAge(Context context, int age)
  {
     SharedPreferences localSharedPreferences = 
       PreferencesUtils.getAgentUserPreferences(context);
     if ((age < 0) || (age > 150)) {
       Log.info("AppDataAgent", "not a valid age!");
       return;
    }
     localSharedPreferences.edit().putInt("age", age).commit();
  }
  





  public static void setGender(Context context, Gender gender)
  {
     SharedPreferences localSharedPreferences = 
       PreferencesUtils.getAgentUserPreferences(context);
     int i = 0;
     switch (gender.ordinal()) {
    case 1: 
       i = 0;
       break;
    case 2: 
       i = 1;
       break;
    case 3: 
       i = 2;
       break;
    default: 
       i = 2;
    }
     localSharedPreferences.edit().putInt("gender", i).commit();
  }
  







  public static void setUserID(Context context, String userId, String idSource)
  {
     SharedPreferences localSharedPreferences = 
       PreferencesUtils.getAgentUserPreferences(context);
     if (TextUtils.isEmpty(userId)) {
       Log.info("AppDataAgent", "userID is null or empty");
       return;
    }
    
     localSharedPreferences.edit().putString("user_id", userId).commit();
     if (TextUtils.isEmpty(idSource)) {
       Log.info("AppDataAgent", "user source is null or empty");
       return;
    }
    
     localSharedPreferences.edit().putString("id_source", idSource).commit();
  }
  







  public static String getSDKVersion()
  {
//     return "1.2.20150103.0001";
	  return "1";
  }  

  public static void updateOnlineConfig(Context context) {
     controller.updateOnlineConfig(context);
  }
  
  
  public static void setOnlineConfigureListener(Context context, OnlineConfigureListener listener) {
     controller.setOnlineConfigureListener(context, listener);
  }
  
  public static String getConfigParams(Context context, String param) {
     return controller.getConfigParams(context, param);
  }
  

}

