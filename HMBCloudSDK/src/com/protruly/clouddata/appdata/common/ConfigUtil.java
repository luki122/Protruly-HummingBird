package com.protruly.clouddata.appdata.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import com.protruly.clouddata.appdata.Constants;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;






public class ConfigUtil
{
   public static String totalSwitchDefault = "1";
   public static String urlSdkConfigDefault = "";
   public static String urlAppDataDefault = "";
  
  public static final String PREF_SDK_PARAM_TOTAL_SWITCH = "totalSwitch";
  
  public static final String PREF_SDK_PARAM_URL_SDK_CONFIG = "urlSdkConfig";
  
  public static final String PREF_SDK_PARAM_URL_APPDATA = "urlAppData";
  public static final String PREF_SDK_PARAM_CFG_INTERVAL = "cfgInterval";
  public static final String PREF_SDK_PARAM_CFG_LAST_TIME = "lastTime";
  public static final String PREF_SDK_PARAM_LAST_SENT_TIME = "lastSentTime";
   public static final String[] PREF_SDK_PARAM_ARRAY = {
     "totalSwitch", 
     "urlSdkConfig", 
     "urlAppData", 
     "cfgInterval" };
  








  public static String getPara(Context context, String key)
  {
     SharedPreferences sp = PreferencesUtils.getSdkParamPreferences(context);
     if ("totalSwitch".equals(key))
       return PreferencesUtils.getStrPrefProp(sp, "totalSwitch", "1");
     if ("urlSdkConfig".equals(key))
//       return PreferencesUtils.getStrPrefProp(sp, "urlSdkConfig", "http://appdatacfg.coolyun.com/AppCfg/AppCfg");
				return PreferencesUtils.getStrPrefProp(sp, "urlSdkConfig", Constants.PARAM_DEFAULT_URL_SDK_CONFIG);
     if ("urlAppData".equals(key))
       return PreferencesUtils.getStrPrefProp(sp, "urlAppData", "http://113.142.37.246/AppDataReceiver/");
     if ("lastTime".equals(key))
       return PreferencesUtils.getStrPrefProp(sp, "lastTime", "0");
     if ("cfgInterval".equals(key))
       return PreferencesUtils.getStrPrefProp(sp, "cfgInterval", "1440");
     if ("lastSentTime".equals(key)) {
       return PreferencesUtils.getStrPrefProp(sp, "lastSentTime", "0");
    }
     Log.info("AppDataAgent", "not support this param");
     return "";
  }
  






  public static boolean isFirstSent(Context context)
  {
     String lastSentTime = getPara(context, "lastSentTime");
     return "0".equals(lastSentTime);
  }
  




  public static String getLastSentTime(Context context)
  {
     return getPara(context, "lastSentTime");
  }
  




  public static void setLastSentTime(Context context)
  {
     SharedPreferences sp = PreferencesUtils.getSdkParamPreferences(context);
     SharedPreferences.Editor spEdit = sp.edit();
     PreferencesUtils.setStringPrefProp(spEdit, "lastSentTime", System.currentTimeMillis()+"");
     spEdit.commit();
  }
  




  public static boolean updatePara(Context context)
  {
     Log.info("AppDataAgent", "updatePara begin");
     boolean rst = false;
     String url = getPara(context, "urlSdkConfig");
    try {
       String js = getUpdateRequest(url, genPostParams(context));
      


       if ((js != null) && (!js.equals(""))) {
         Log.info("AppDataAgent", "sdk para:" + SecurityUtil.decrypt(js));
         rst = updatePara(context, SecurityUtil.decrypt(js));
      }
    }
    catch (ClientProtocolException e) {
       e.printStackTrace();
    }
    catch (ParseException e) {
       e.printStackTrace();
    }
    catch (IOException e) {
       e.printStackTrace();
    }
     return rst;
  }
  




  private static boolean updatePara(Context context, String js)
  {
     if ((js != null) && (!js.equals(""))) {
       Log.info("AppDataAgent", "js is ok");
      
       SharedPreferences sp = PreferencesUtils.getSdkParamPreferences(context);
       SharedPreferences.Editor spEdit = sp.edit();
      try
      {
         JSONObject jsObj = new JSONObject(js);
        
         Iterator iter = jsObj.keys();
         while (iter.hasNext()) {
           String key = (String)iter.next();
           String value = jsObj.optString(key);
           Log.info("AppDataAgent", "key:" + key);
           Log.info("AppDataAgent", "value:" + value);
           if ((key != null) && (!key.equals("")) && (value != null) && (!value.equals("")) && (isSupportParam(key)))
          {
             Log.info("AppDataAgent", "isSupportParam:" + key);
             PreferencesUtils.setStringPrefProp(spEdit, key, value);
          }
          
           PreferencesUtils.setStringPrefProp(spEdit, "lastTime", System.currentTimeMillis()+"");
           spEdit.commit();
        }
         return true;
      } catch (JSONException e) {
         e.printStackTrace();
      }
    }
     return false;
  }
  







  public static String getUpdateRequest(String url, LinkedList<BasicNameValuePair> params)
    throws ClientProtocolException, IOException, ParseException
  {
     if ((url != null) && (!url.equals("")))
    {
       int maxSendCount = 3;
       int sendCount = 1;
       int resCode = -1;
       while (sendCount <= maxSendCount)
      {
         HttpClient client = new DefaultHttpClient();
         client.getParams().setParameter("http.connection.timeout", Integer.valueOf(6000));
         client.getParams().setParameter("http.socket.timeout", Integer.valueOf(6000));
        
         if ((sendCount > 1) && (sendCount <= Constants.URL_CONFIG_SERVER_BAK.length + 1)) {
           url = Constants.URL_CONFIG_SERVER_BAK[(sendCount - 2)];
        }
         Log.info("AppDataAgent", "cfg url:" + url);
        try {
           HttpPost postMethod = new HttpPost(url);
           postMethod.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
          
           HttpResponse response = client.execute(postMethod);
           resCode = response.getStatusLine().getStatusCode();
           Log.info("AppDataAgent", "request SDK params resCode:" + resCode);
           if (resCode == 200) {
             String result = EntityUtils.toString(response.getEntity(), "utf-8");
             return result;
          }
        } catch (UnsupportedEncodingException e) {
           e.printStackTrace();
        } catch (ClientProtocolException e) {
           e.printStackTrace();
        } catch (IOException e) {
           e.printStackTrace();
        } finally {
           client.getConnectionManager().shutdown(); } client.getConnectionManager().shutdown();
        
         sendCount++;
        try
        {
           Thread.sleep(500L);
        } catch (InterruptedException e) {
           e.printStackTrace();
        }
      }
    }
     return "";
  }
  

  public static String getUpdateRequest(String url, String body) {
	  if ((url != null) && (!url.equals(""))) {
		  int maxSendCount = 3;
		  int sendCount = 1;
		  int resCode = -1;
		  while (sendCount <= maxSendCount) {
			  HttpPost httpPost = new HttpPost(url);
			  HttpEntity requestEntity;
			try {
				requestEntity = new StringEntity(body, "UTF-8");
				  httpPost.setEntity(requestEntity); 
				  HttpClient client = new DefaultHttpClient();
				  client.getParams().setParameter("http.connection.timeout", Integer.valueOf(6000));
				  client.getParams().setParameter("http.socket.timeout", Integer.valueOf(6000));
				  
				  HttpResponse response;
				try {
					response = client.execute(httpPost);
					  resCode = response.getStatusLine().getStatusCode();
					  Log.info("AppDataAgent", "request SDK params resCode:" + resCode);
					  if (resCode == 200) {
						  String result = EntityUtils.toString(response.getEntity(), "utf-8");
					      return result;
					  }
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return "";
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return "";
				}

			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "";
			}  
			  
		  }

			return "";
	  }

		return "";
  }


  public static LinkedList<BasicNameValuePair> genPostParams(Context context)
  {
     String totalSwitch = getPara(context, "totalSwitch");
     LinkedList<BasicNameValuePair> params = new LinkedList();
     params.add(new BasicNameValuePair("p1", SecurityUtil.encryptLocal(CommonUtils.getAppKey(context))));
     params.add(new BasicNameValuePair("p2", SecurityUtil.encryptLocal(CommonUtils.getProductName())));
     params.add(new BasicNameValuePair("p3", SecurityUtil.encryptLocal(CommonUtils.getAppDeviceId(context))));
    
     params.add(new BasicNameValuePair("p4", SecurityUtil.encryptLocal(totalSwitch)));
     params.add(new BasicNameValuePair("p5", SecurityUtil.encryptLocal("getSdkCfg")));
     return params;
  }
  



  public static boolean isSupportParam(String param)
  {
     for (int i = 0; i < PREF_SDK_PARAM_ARRAY.length; i++) {
       if (PREF_SDK_PARAM_ARRAY[i].equals(param)) {
         return true;
      }
    }
     return false;
  }
  





  public static boolean isNeedUpdate(Context context)
  {
     if (!CommonUtils.isNetworkAvailable(context)) {
       return false;
    }
    
     if (!checkCfgInterval(context)) {
       return false;
    }
     return true;
  }
  




  public static boolean checkCfgInterval(Context context)
  {
     long lastTime = Long.parseLong(getPara(context, "lastTime"));
     long nowTime = System.currentTimeMillis();
     long interval = 0L;
    try {
       interval = 60000L * 
         Long.parseLong(getPara(context, "cfgInterval"));
    } catch (Exception e) {
       Log.error("AppDataAgent", e.getMessage());
       interval = 0L;
    }
     return nowTime - lastTime > interval;
  }
}
