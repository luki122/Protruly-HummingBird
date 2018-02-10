package com.protruly.clouddata.appdata.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import com.protruly.clouddata.appdata.AppDataAgent;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;







public class NetworkUtils
{
  private static DefaultHttpClient getHttpClient(Context context)
  {
     BasicHttpParams localBasicHttpParams = new BasicHttpParams();
     HttpConnectionParams.setConnectionTimeout(localBasicHttpParams, 10000);
     HttpConnectionParams.setSoTimeout(localBasicHttpParams, 30000);
     DefaultHttpClient localDefaultHttpClient = new DefaultHttpClient(
       localBasicHttpParams);
     String str = CommonUtils.getNetWorkProxy(context);
     if (str != null) {
       HttpHost localHttpHost = new HttpHost(str, 80);
       localDefaultHttpClient.getParams().setParameter(
         "http.route.default-proxy", localHttpHost);
    }
     return localDefaultHttpClient;
  }
  






  public static String addParams(String url, Context context, String serviceCode)
  {
     StringBuffer sbParams = new StringBuffer("");
     String rst = "";
     if ((url != null) && (!url.equals(""))) {
       String appKey = "";
       String model = "";
       String sdkVer = "";
       String userCode = "";
       String sCode = "";
      
       appKey = SecurityUtil.encryptLocal(CommonUtils.getAppKey(context));
       model = SecurityUtil.encryptLocal(CommonUtils.getProductName());
       sdkVer = SecurityUtil.encryptLocal(AppDataAgent.getSDKVersion());
       userCode = SecurityUtil.encryptLocal(CommonUtils.getUserCode(model, CommonUtils.getAppDeviceId(context)));
       sCode = SecurityUtil.encryptLocal(serviceCode);
      





      try
      {
         sbParams.append("?").append("appKey").append("=").append(URLEncoder.encode(appKey, "UTF-8")).append("&").append("userCode").append("=").append(URLEncoder.encode(userCode, "UTF-8")).append("&").append("model").append("=").append(URLEncoder.encode(model, "UTF-8")).append("&").append("sdkVer").append("=").append(URLEncoder.encode(sdkVer, "UTF-8")).append("&").append("sCode").append("=").append(URLEncoder.encode(sCode, "UTF-8"));
      } catch (UnsupportedEncodingException e) {
         e.printStackTrace();
      }
       rst = url + sbParams.toString();
    }
     return rst;
  }
  

  public static int sendMessage(Context context, JSONObject paramJSONObject, String strURL, String serviceCode)
  {
     int i = -1;
    try {
       strURL = addParams(strURL, context, serviceCode);
       Log.info("AppDataAgent", "url:" + strURL);
       HttpPost localHttpPost = new HttpPost(strURL);
       DefaultHttpClient localDefaultHttpClient = getHttpClient(context);
      
       String str = new String(paramJSONObject.toString().getBytes());
      






















       Log.info("AppDataAgent", "send data:" + str);
       byte[] arrayOfByte = GzipUtils.gzip(str);
      
       localHttpPost.addHeader("Content-Encoding", "gzip");
       localHttpPost.addHeader("Content-Type", "application/json;charset=UTF-8");
       InputStreamEntity localInputStreamEntity = new InputStreamEntity(
         new ByteArrayInputStream(arrayOfByte), arrayOfByte.length);
       localHttpPost.setEntity(localInputStreamEntity);
       SharedPreferences.Editor localEditor = 
         PreferencesUtils.getAgentStatePreferences(context).edit();
       long startMillis = System.currentTimeMillis();
       HttpResponse localHttpResponse = localDefaultHttpClient
         .execute(localHttpPost);
       long endMillis = System.currentTimeMillis();
       long millis = endMillis - startMillis;
       int j = localHttpResponse.getStatusLine().getStatusCode();
       Log.info("AppDataAgent", "response code : " + j);
       if ((j == 200) || (j == 400))
      {
         PreferencesUtils.setLongPrefProp(localEditor, "last_req_time", millis);
         localEditor.commit();
         i = j;
        

         ConfigUtil.setLastSentTime(context);
      }
       localDefaultHttpClient.getConnectionManager().shutdown();
    } catch (Exception localException) {
       Log.info("AppDataAgent", "Exception occurred in sendMessage.", 
         localException);
    }
     return i;
  }
}

