package com.protruly.clouddata.appdata;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import com.protruly.clouddata.appdata.common.CachedFileUtils;
import com.protruly.clouddata.appdata.common.CommonUtils;
import com.protruly.clouddata.appdata.common.Log;
import com.protruly.clouddata.appdata.common.PreferencesUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public class EventHandler
{
  private List<JSONObject> eventList;
  private HashMap<String, Long> eventBeginTimeMap;
  private HashMap<String, Map<String, String>> paramMap;
  private HashMap<String, Map<String, Integer>> adMonitorEventMap;
  private List<JSONObject> adList;
  private static final int MAX_BUFFER_SIZE = 10;
  private Handler handler;
  
  EventHandler(Handler paramHandler)
  {
     this.handler = paramHandler;
     this.eventList = new ArrayList();
     this.adList = new ArrayList();
     this.eventBeginTimeMap = new HashMap();
     this.paramMap = new HashMap();
     this.adMonitorEventMap = new HashMap();
  }
  
  void onEvent(Context context, String eventId, String lable, long duration, int count)
  {
     onEvent(context, eventId, lable, duration, count, 0);
  }
  
  void onEvent(final Context context, final String eventId, final String lable, final long duration, int count, final int flag)
  {
    try {
       if (context == null) {
         Log.error("AppDataAgent", "unexpected null context in onEvent");
         return;
      }
       if ((TextUtils.isEmpty(eventId)) || (count <= 0)) {
         Log.info("AppDataAgent", "invalid params in onEvent");
         return;
      }
       if (!CommonUtils.inputCheck(eventId))
         throw new Exception("Illegal eventId:" + eventId);
       this.handler.post(new Runnable()
      {
        public void run()
        {
           EventHandler.this.addEvent(eventId, lable, 
             duration, flag);
           if (EventHandler.this.isBufferFull())
             EventHandler.this.flushEventBuffer(context);
        }
      });
    } catch (Exception localException) {
       Log.error("AppDataAgent", "Exception occurred in onEvent(). ", 
         localException);
    }
  }
  

   private Context contextEvt = null;
  

  void onEvent(Context context, final String eventId, Map<String, String> paramMap, final long duration)
  {
     this.contextEvt = context;
    try
    {
       if (context == null) {
         Log.error("AppDataAgent", "unexpected null context in onEvent");
         return;
      }
       if (TextUtils.isEmpty(eventId)) {
         Log.info("AppDataAgent", "invalid params in onEvent");
         return;
      }
       if ((paramMap == null) || (paramMap.isEmpty())) {
         Log.info("AppDataAgent", "map is null or empty in onEvent");
         return;
      }
       if (!CommonUtils.inputCheck(eventId))
         throw new Exception("Illegal eventId:" + eventId);
       Iterator localIterator = paramMap.entrySet().iterator();
       final HashMap localHashMap = new HashMap();
       while (localIterator.hasNext()) {
         Map.Entry localEntry = (Map.Entry)localIterator.next();
         if ((((String)localEntry.getKey()).length() > 30) || 
           (((String)localEntry.getValue()).length() > 30)) {
           Log.info("AppDataAgent", "map data (" + 
             (String)localEntry.getKey() + "=" + 
             (String)localEntry.getValue() + 
             ") key or value is longer than 30.");
        } else {
           if (!CommonUtils.inputCheck((String)localEntry.getKey())) {
             throw new Exception("Illegal param key:" + 
               (String)localEntry.getKey());
          }
           localHashMap.put(localEntry.getKey(), localEntry.getValue());
        }
      }
       this.handler.post(new Runnable() {
        public void run() {
           EventHandler.this.addEvent(eventId, localHashMap, 
             duration, 1);
           if (EventHandler.this.isBufferFull())
             EventHandler.this.flushEventBuffer(EventHandler.this.contextEvt);
        }
      });
    } catch (Exception localException) {
       Log.error("AppDataAgent", "Exception occurred in onEvent(). ", 
         localException);
    }
  }
  




  void addAdMonitorEvent(JSONObject paramJSONObject)
  {
     JSONObject localJSONObject = paramJSONObject;
     this.adList.add(localJSONObject);
  }
  






















  void onEventBegin(Context context, String eventId)
  {
     saveEventBeginInfo("e_" + eventId);
  }
  
  void onEventBegin(Context context, String eventId, String lable)
  {
     saveEventBeginInfo("el_" + eventId + lable);
  }
  
  void onEventEnd(Context context, String eventId) {
     onEventEnd(context, eventId, null, "e_" + eventId);
  }
  
  void onEventEnd(Context context, String eventId, String lable)
  {
     onEventEnd(context, eventId, lable, "el_" + 
       eventId + lable);
  }
  
  private void onEventEnd(Context context, String eventId, String lable, String duration)
  {
     long l = getEventDuration(duration);
     if (l < 0L) {
       Log.info("AppDataAgent", 
         "event duration less than 0 in onEventEnd");
       return;
    }
     onEvent(context, eventId, lable, l, 1);
  }
  
  void onKVEventBegin(Context context, String eventId, Map<String, String> paramMap, String ekvFlag)
  {
    try {
       String str = "kv_" + eventId + ekvFlag;
       saveEventParamMap(str, paramMap);
       saveEventBeginInfo(str);
    } catch (Exception localException) {
       Log.info("AppDataAgent", "Exception occurred in onKVEventBegin", 
         localException);
    }
  }
  
  void onKVEventEnd(Context context, String eventId, String ekvFlag)
  {
     String str = "kv_" + eventId + ekvFlag;
     long l = getEventDuration(str);
     if (l < 0L) {
       Log.info("AppDataAgent", 
         "event duration less than 0 in onEvnetEnd");
       return;
    }
     onEvent(context, eventId, getEventParamMap(str), l);
  }
  
  void flushEventBuffer(Context context) {
     saveEventBuffer(context);
  }
  
  boolean hasEventBuffer() {
     return getEventSize() > 0;
  }
  
  private int getEventSize() {
     return this.eventList.size();
  }
  
  private int getAdSize() {
     return this.adList.size();
  }
  
  private boolean hasAdMonitor() {
     return !this.adMonitorEventMap.isEmpty();
  }
  
  private boolean isBufferFull() {
     return this.eventList.size() >= 10;
  }
  
  private boolean isBufferFullAd() {
     return this.adList.size() >= 50;
  }
  
  private void saveEventBeginInfo(String strEvent) {
     this.eventBeginTimeMap.put(strEvent, 
       Long.valueOf(System.currentTimeMillis()));
  }
  
  private long getEventBeginTime(String paramString) {
     if (this.eventBeginTimeMap.containsKey(paramString))
       return 
         ((Long)this.eventBeginTimeMap.remove(paramString)).longValue();
     return -1L;
  }
  
  private void saveEventParamMap(String strEvent, Map<String, String> paramMap)
  {
     if (!this.paramMap.containsKey(strEvent))
       this.paramMap.put(strEvent, paramMap);
  }
  
  private Map<String, String> getEventParamMap(String strEvent) {
     return (Map)this.paramMap.remove(strEvent);
  }
  
  private synchronized void addEvent(String eventId, String lable, long duration, int count)
  {
    try {
       JSONObject localJSONObject = null;
       if (!TextUtils.isEmpty(lable)) {
         localJSONObject = new JSONObject();
         localJSONObject.put(eventId, lable);
      }
       addEvent(eventId, localJSONObject, duration, count, 0);
    } catch (Exception localException) {
       localException.printStackTrace();
    }
  }
  
  private synchronized void addEvent(String eventId, String lable, long duration, int count, int flag)
  {
    try {
       JSONObject localJSONObject = null;
       if (!TextUtils.isEmpty(lable)) {
         localJSONObject = new JSONObject();
         localJSONObject.put(eventId, lable);
      }
       addEvent(eventId, localJSONObject, duration, count, flag);
    } catch (Exception localException) {
       localException.printStackTrace();
    }
  }
  
  private synchronized void addEvent(String eventId, Map<String, String> paramMap, long duration, int count)
  {
    try {
       Iterator localIterator = paramMap.entrySet().iterator();
       JSONObject localJSONObject = new JSONObject();
       while (localIterator.hasNext()) {
         Map.Entry localEntry = (Map.Entry)localIterator.next();
         localJSONObject.put((String)localEntry.getKey(), 
           localEntry.getValue());
      }
       addEvent(eventId, localJSONObject, duration, count, 1);
    } catch (Exception localException) {
       localException.printStackTrace();
    }
  }
  
  private void addEvent(String eventId, JSONObject paramJSONObject, long duration, int count, int lable)
  {
     JSONObject localJSONObject = new JSONObject();
    try {
       localJSONObject.put("event", eventId);
       localJSONObject.put("time", CommonUtils.getUnixTimestamp());
       localJSONObject.put("count", count);
       localJSONObject.put("flag", lable);
       if (paramJSONObject != null)
         localJSONObject.put("map", paramJSONObject);
       if (duration > 0L)
         localJSONObject.put("duration", duration);
       this.eventList.add(localJSONObject);
    } catch (Exception localException) {
       localException.printStackTrace();
    }
  }
  
  private long getEventDuration(String strEvent) {
     long l1 = -1L;
    try {
       long l2 = getEventBeginTime(strEvent);
       if (l2 > 0L)
         l1 = System.currentTimeMillis() - l2;
    } catch (Exception localException) {
       Log.info("AppDataAgent", "Exception occurred in getEventDuration", 
         localException);
    }
     return l1;
  }
  
  private void saveEventBuffer(Context context) {
     if ((getEventSize() <= 0) && (!hasAdMonitor()))
       return;
     JSONObject localJSONObject = getEventBufferAndCache(context);
     CachedFileUtils.saveMessageToCacheFile(context, localJSONObject);
  }
  
  public synchronized JSONObject getEventBufferAndCache(Context context) {
     JSONObject localJSONObject = 
       CachedFileUtils.getCacheMessage(context);
    

    try
    {
       if (this.eventList.size() > 0)
      {

         android.content.SharedPreferences sp = PreferencesUtils.getAgentStatePreferences(context);
         Object localObject1 = PreferencesUtils.getStrPrefProp(sp, "session_id", "");
        
         if (!TextUtils.isEmpty((CharSequence)localObject1)) {
           if (localJSONObject == null)
             localJSONObject = new JSONObject();
           Object localObject2 = null;
           if (localJSONObject.isNull("event")) {
             localObject2 = new JSONArray();
             localJSONObject.put("event", localObject2);
          } else {
             localObject2 = localJSONObject.getJSONArray("event");
          }
           Object localObject3 = null;
           for (int i = 0; i < this.eventList.size(); i++) {
             localObject3 = (JSONObject)this.eventList.get(i);
             ((JSONObject)localObject3).put("sessionid", localObject1);
             ((JSONArray)localObject2).put(localObject3);
          }
           this.eventList.clear();
        }
      }
      
       if (!this.adList.isEmpty()) {
         if (localJSONObject == null)
           localJSONObject = new JSONObject();
         Object localObject1 = null;
         if (localJSONObject.isNull("adv")) {
           localObject1 = new JSONObject();
        } else
           localObject1 = localJSONObject.getJSONObject("adv");
        Object localObject2;
         if (localJSONObject.isNull("event")) {
           localObject2 = new JSONArray();
           localJSONObject.put("event", localObject2);
        } else {
           localObject2 = localJSONObject.getJSONArray("event");
        }
         Object localObject3 = null;
         for (int i = 0; i < this.eventList.size(); i++) {
           localObject3 = (JSONObject)this.eventList.get(i);
           ((JSONObject)localObject3).put("sessionid", localObject1);
           ((JSONArray)localObject2).put(localObject3);
        }
         this.eventList.clear();
















      }
      

















    }
    catch (Exception localException)
    {
















       localException.printStackTrace();
    }
     return localJSONObject;
  }
}