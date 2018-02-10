package com.protruly.clouddata.appdata.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.protruly.clouddata.appdata.AppDataAgent;



















public class OnlineConfigUtil
{
  public static final int STATUS_FAIL = -1;
  public static final int STATUS_SUCCESS_NO_CHANGE = 1;
  public static final int STATUS_SUCCESS_HAVE_CHANGE = 2;
  
  public static JSONObject getOnlinePara(Context context)
  {
     Log.info("AppDataAgent", "get online config begin");
     JSONObject jsObject = null;
     String url = ConfigUtil.getPara(context, "urlSdkConfig");
    
    try
    {
       String js = getUpdateRequest(url, genPostBody(context));
       Log.info("AppDataAgent", "online para: " + js);
      


       if (js != null) {
         js = SecurityUtil.decrypt(js);
         Log.info("AppDataAgent", "online para decrypt:" + js);
         jsObject = new JSONObject(js);
      }
    }
    catch (ClientProtocolException e1) {
       e1.printStackTrace();
    }
    catch (ParseException e1) {
       e1.printStackTrace();
    }
    catch (IOException e1) {
       e1.printStackTrace();
    }
    catch (JSONException e) {
       e.printStackTrace();
    }
     Log.info("AppDataAgent", "get online config end");
     return jsObject;
  }
  




  public static int saveOnlinePara(Context context, JSONObject jsObj)
  {
	  int rst = -1;
     boolean isChanged = false;
     if (jsObj != null) {
       Log.info("AppDataAgent", "save online config begin");
       SharedPreferences sp = PreferencesUtils.getAgentOnlineSettingPreferences(context);
      
       SharedPreferences.Editor spEdit = sp.edit();
       Iterator iter = jsObj.keys();
       while (iter.hasNext()) {
         String key = (String)iter.next();
         String value = jsObj.optString(key);
         String oldValue = getConfigParams(context, key);
         Log.info("AppDataAgent", "key:" + key);
         Log.info("AppDataAgent", "oldValue:" + oldValue);
         Log.info("AppDataAgent", "value:" + value);
         if (!oldValue.equals(value)) {
           isChanged = true;
         }
         
         
         
         spEdit.putString(key, value);
         spEdit.commit();
      }
       Log.info("AppDataAgent", "save online config end");
       if (isChanged) {
         rst = 2;
      } else {
         rst = 1;
      }
    }
     return rst;
  }
  





  public static LinkedList<BasicNameValuePair> genPostParams(Context context)
  {
     String totalSwitch = ConfigUtil.getPara(context, "totalSwitch");
     LinkedList<BasicNameValuePair> params = new LinkedList();
     params.add(new BasicNameValuePair("appKey", SecurityUtil.encryptLocal(CommonUtils.getAppKey(context))));
     params.add(new BasicNameValuePair("model", SecurityUtil.encryptLocal(CommonUtils.getProductName())));
     params.add(new BasicNameValuePair("devNo", SecurityUtil.encryptLocal(CommonUtils.getAppDeviceId(context))));
     params.add(new BasicNameValuePair("sdklVer", SecurityUtil.encryptLocal(AppDataAgent.getSDKVersion())));
     params.add(new BasicNameValuePair("versionCode", SecurityUtil.encryptLocal(CommonUtils.getAppVersionCode(context))));
     params.add(new BasicNameValuePair("versionName", SecurityUtil.encryptLocal(CommonUtils.getAppVersionName(context))));


//     params.add(new BasicNameValuePair("p4", SecurityUtil.encryptLocal(totalSwitch)));
//     params.add(new BasicNameValuePair("p5", SecurityUtil.encryptLocal("getOnlineCfg")));
     return params;
  }
  
  
  public static String genPostBody(Context context)
  {

	    StringBuffer sb = new StringBuffer();
	    sb.append("{");
	    sb.append("\"appKey\":\"").append(CommonUtils.getAppKey(context)).append("\",");
	    sb.append("\"model\":\"").append(CommonUtils.getProductName()).append("\",");
	    sb.append("\"devNo\":\"").append(CommonUtils.getAppDeviceId(context)).append("\",");
	    sb.append("\"sdkVer\":").append(AppDataAgent.getSDKVersion()).append(",");
	    sb.append("\"versionCode\":").append(CommonUtils.getAppVersionCode(context)).append(",");
	    sb.append("\"versionName\":\"").append(CommonUtils.getAppVersionName(context)).append("\"");
	    sb.append("}");
	    Log.info("AppDataAgent", "genPostBody:" + sb.toString());
     return sb.toString();
  }
  
  
  
  public static String getUpdateRequest(String url, LinkedList<BasicNameValuePair> params)
    throws ClientProtocolException, IOException, ParseException
  {	  
	  return ConfigUtil.getUpdateRequest(url, params);
  }
  
  public static String getUpdateRequest(String url, String body)
		    throws ClientProtocolException, IOException, ParseException
		  {	  
			  return ConfigUtil.getUpdateRequest(url, SecurityUtil.encrypt(body));
		  }
  
  public static boolean isChanged(Context context, JSONObject jsObj)
  {
     boolean rst = false;
     if (jsObj != null) {
       String oldValue = "";
      
       Iterator iter = jsObj.keys();
       while (iter.hasNext()) {
         String key = (String)iter.next();
         String value = jsObj.optString(key);
         oldValue = getConfigParams(context, key);
         Log.info("AppDataAgent", "key:" + key);
         Log.info("AppDataAgent", "value:" + value);
         if (!oldValue.equals(value)) {
           rst = true;
           break;
        }
      }
    }
    

     return rst;
  }
  






  public static String getConfigParams(Context context, String param)
  {
     String value = "";
     if ((context != null) && (param != null) && (!param.equals(""))) {
       SharedPreferences sp = PreferencesUtils.getAgentOnlineSettingPreferences(context);
       value = sp.getString(param, "");
       Log.info("AppDataAgent", "getAgentOnlineSettingPreferences value:" + value);
    }
     return value;
  }
}

