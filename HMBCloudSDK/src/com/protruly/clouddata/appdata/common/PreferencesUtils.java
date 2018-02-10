package com.protruly.clouddata.appdata.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class PreferencesUtils
{
  public static SharedPreferences getAgentUserPreferences(Context context)
  {
     return context.getSharedPreferences("appdata_agent_user_" + 
       context.getPackageName(), 0);
  }
  
  public static SharedPreferences getAgentStatePreferences(Context context)
  {
     return context.getSharedPreferences("appdata_agent_state_" + 
       context.getPackageName(), 0);
  }
  
  public static String getStrPrefProp(SharedPreferences sp, String prop, String defaultStr)
  {
     if ((sp != null) && (prop != null))
    {
       Log.info("AppDataAgent", prop + " : " + SecurityUtil.encryptLocal(prop));
       return SecurityUtil.decryptLocal(sp.getString(SecurityUtil.encryptLocal(prop), SecurityUtil.encryptLocal(defaultStr)));
    }
    


     return "";
  }
  
  public static int getIntPrefProp(SharedPreferences sp, String prop, int defaultInt)
  {
     int rst = 0;
     if ((sp != null) && (prop != null))
    {
       String tmp = SecurityUtil.decryptLocal(sp.getString(SecurityUtil.encryptLocal(prop), SecurityUtil.encryptLocal(String.valueOf(defaultInt))));
       if (tmp != null) {
         rst = Integer.parseInt(tmp);
      }
    }
    


     return rst;
  }
  
  public static long getLongPrefProp(SharedPreferences sp, String prop, long defaultLong)
  {
     long rst = 0L;
     if ((sp != null) && (prop != null))
    {
       String tmp = SecurityUtil.decryptLocal(sp.getString(SecurityUtil.encryptLocal(prop), SecurityUtil.encryptLocal(String.valueOf(defaultLong))));
       if (tmp != null) {
         rst = Long.parseLong(tmp);
      }
    }
    


     return rst;
  }
  
  public static float getFloatPrefProp(SharedPreferences sp, String prop, float defaultFloat)
  {
     float rst = 0.0F;
     if ((sp != null) && (prop != null))
    {
       String tmp = SecurityUtil.decryptLocal(sp.getString(SecurityUtil.encryptLocal(prop), SecurityUtil.encryptLocal(String.valueOf(defaultFloat))));
       if (tmp != null) {
         rst = Float.parseFloat(tmp);
      }
    }
    


     return rst;
  }
  





  public static void setStringPrefProp(SharedPreferences.Editor editor, String prop, String value)
  {
     if ((editor != null) && (prop != null) && (value != null))
    {
       editor.putString(SecurityUtil.encryptLocal(prop), SecurityUtil.encryptLocal(value));
    }
    


     editor.commit();
  }
  
  public static void setIntPrefProp(SharedPreferences.Editor editor, String prop, int value) {
     if ((editor != null) && (prop != null))
    {
       editor.putString(SecurityUtil.encryptLocal(prop), SecurityUtil.encryptLocal(String.valueOf(value)));
    }
    


     editor.commit();
  }
  
  public static void setLongPrefProp(SharedPreferences.Editor editor, String prop, long value) {
     if ((editor != null) && (prop != null))
    {
       editor.putString(SecurityUtil.encryptLocal(prop), SecurityUtil.encryptLocal(String.valueOf(value)));
    }
    


     editor.commit();
  }
  
  public static void setFloatPrefProp(SharedPreferences.Editor editor, String prop, float value) {
     if ((editor != null) && (prop != null))
    {
       editor.putString(SecurityUtil.encryptLocal(prop), SecurityUtil.encryptLocal(String.valueOf(value)));
    }
    


     editor.commit();
  }
  
  public static boolean contains(SharedPreferences sp, String prop) {
     boolean rst = false;
     if ((sp != null) && (prop != null))
    {
       rst = sp.contains(SecurityUtil.encryptLocal(prop));
    }
    


     return rst;
  }
  
  public static void remove(SharedPreferences.Editor editor, String prop) {
     if ((editor != null) && (prop != null))
    {
       editor.remove(SecurityUtil.encryptLocal(prop));
      


       editor.commit();
    }
  }
  





  public static SharedPreferences getAgentOnlineSettingPreferences(Context context)
  {
     return context.getSharedPreferences(
       "appdata_agent_online_setting_" + 
       context.getPackageName(), 0);
  }
  
  public static SharedPreferences getSdkParamPreferences(Context context)
  {
     return context.getSharedPreferences(
       "appdata_sdk_param_" + 
       context.getPackageName(), 0);
  }
}

