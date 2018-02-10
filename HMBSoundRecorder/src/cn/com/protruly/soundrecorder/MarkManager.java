package cn.com.protruly.soundrecorder;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by liushitao on 17-8-25.
 */

public class MarkManager {
    public String TAG = "MarkManager";
    public JSONObject jsonObject;
    public JSONArray jsonArray;
    public SharedPreferences markPreferences;
    public Context mContext;

    public MarkManager(Context context) {
        mContext = context;
        jsonObject = new JSONObject();
        jsonArray = new JSONArray();
        markPreferences = context.getSharedPreferences("MarkManager", Activity.MODE_PRIVATE);
    }

    public void putMarkList(String filepath, List<Long> timeList) {
        if(jsonArray==null)jsonArray = new JSONArray();//clear jsonArray
        if(jsonObject==null)jsonObject = new JSONObject();
        for (int i = 0; i < timeList.size(); i++) {
            jsonArray.put(timeList.get(i));
        }
        try {
            jsonObject.put(filepath, jsonArray);
            Log.d(TAG,"putMarkList jsonObject:"+jsonObject);
            SharedPreferences.Editor editor = markPreferences.edit();
            editor.putString(filepath,jsonObject.toString());
            editor.apply();
        } catch (JSONException e) {
            Log.d(TAG,"putMarkList JSONException:"+e);
            e.fillInStackTrace();
        }
    }

    public void removeSomeMark(String filepath,int index){
        try {
            JSONObject jsonObject = new JSONObject();
            String result = markPreferences.getString(filepath,"");
            if(!TextUtils.isEmpty(result)) {
                jsonObject = new JSONObject(result);
            }
            JSONArray jsonArray = jsonObject.getJSONArray(filepath);
            jsonArray.remove(index);
            jsonObject.put(filepath, jsonArray);
            SharedPreferences.Editor editor = markPreferences.edit();
            editor.putString(filepath,jsonObject.toString());
            editor.apply();
        } catch (JSONException e) {
            Log.d(TAG,"getMarkList JSONException:"+e);
            e.printStackTrace();
        }
    }

    public ArrayList<Long> getMarkList(String filepath) {
        try {
            JSONObject jsonObject = new JSONObject();
            String result = markPreferences.getString(filepath,"");
            if(!TextUtils.isEmpty(result)) {
                jsonObject = new JSONObject(result);
            }
            Log.d(TAG,"getMarkList jsonObject:"+jsonObject);
            JSONArray jsonArray = jsonObject.getJSONArray(filepath);
            ArrayList<Long> timelist = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                timelist.add((long)(jsonArray.getLong(i)));
            }
            return timelist;
        } catch (JSONException e) {
            Log.d(TAG,"getMarkList JSONException:"+e);
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public void clearAllMarkData(String filepath){
        jsonObject = null;
        jsonArray = null;
        SharedPreferences.Editor editor = markPreferences.edit();
        editor.remove(filepath);
        editor.apply();
    }
}
