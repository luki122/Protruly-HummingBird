package com.protruly.clouddata.appdata.common;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;



public class CachedFileUtils
{
  public static String getCacheFileName(Context context)
  {
     return 
       "appdata_agent_cached_" + context.getPackageName();
  }
  

  public static String getErrorFileName(Context context)
  {
     return 
       "appdata_agent_cached_" + context.getPackageName();
  }
  
   public static JSONObject getCacheMessage(Context context) { String strFile = getCacheFileName(context);
     StringBuilder localStringBuilder = new StringBuilder();
     FileInputStream localFileInputStream = null;
    try {
       File localFile = new File(context.getFilesDir(), strFile);
       if (localFile.exists()) {
         localFileInputStream = context.openFileInput(strFile);
         int length = localFileInputStream.available();
        
         byte[] arrayOfByte = new byte[length];
         int i = 0;
         while ((i = localFileInputStream.read(arrayOfByte)) != -1) {
           localStringBuilder.append(new String(arrayOfByte, 0, i));
        }
        
         if (localStringBuilder.length() != 0) {
           JSONObject localJSONObject1 = new JSONObject(
             localStringBuilder.toString());
           String strVersion = (String)localJSONObject1
             .remove("cache_version");
           String str3 = CommonUtils.getAppVersionCode(context);
           if (!str3.equals(strVersion))
             localJSONObject1.remove("error");
           JSONObject localJSONObject2 = localJSONObject1;
           return localJSONObject2;
        }
      }
    } catch (JSONException localJSONException) {
       deleteCacheLogFile(context);
       Log.error("AppDataAgent", "json format error, delete cache.", 
         localJSONException);
    } catch (Exception localException) {
       localException.printStackTrace();
    } finally {
       if (localFileInputStream != null) {
        try {
           localFileInputStream.close();
        } catch (IOException localIOException5) {
           localIOException5.printStackTrace();
        }
      }
    }
     if (localFileInputStream != null) {
      try {
         localFileInputStream.close();
      } catch (IOException localIOException5) {
         localIOException5.printStackTrace();
      }
    }
     return null;
  }
  
  public static void deleteCacheLogFile(Context context) {
     context.deleteFile(getCacheFileName(context));
     Log.info("AppDataAgent", "deleteCacheLogFile");
  }
  
  public static void deleteErrorFile(Context context) {
     context.deleteFile("com_appdata_crash.cache");
     Log.info("AppDataAgent", "deleteCacheErrorFile");
  }
  

  public static void saveMessageToCacheFile(Context context, JSONObject paramJSONObject)
  {
     Log.info("AppDataAgent", "check data file size:");
     if (isExceedMaxSize(context))
    {
       Log.info("AppDataAgent", "isExceedMaxSize");
       return;
    }
     String str = getCacheFileName(context);
    try
    {
       paramJSONObject.put("cache_version", 
         CommonUtils.getAppVersionCode(context));
       FileOutputStream localFileOutputStream = context
         .openFileOutput(str, 0);
      





       localFileOutputStream.write(paramJSONObject.toString().getBytes());
       localFileOutputStream.flush();
       localFileOutputStream.close();
    } catch (Exception localException) {
       Log.error("AppDataAgent", 
         "Exception occurred when save cache message.", 
         localException);
    }
  }
  




  public static boolean isExceedMaxSize(Context context)
  {
     String path = context.getApplicationContext().getFilesDir().getAbsolutePath();
     String str = path + "/" + getCacheFileName(context);
    
     Log.info("AppDataAgent", "file:" + str);
     return CommonUtils.isExceedMaxSize(context, str, 5242880L);
  }
}

