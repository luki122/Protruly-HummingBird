package com.android.quicksearchbox.hotsearch;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

/**
 * Created by lijun on 17-8-29.
 */

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class HotSearchHttpGet extends AsyncTask<String, Object, Object> {

    HotSearchView mHotSearchView;

    public HotSearchHttpGet(HotSearchView view) {
        mHotSearchView = view;
    }

    @Override
    protected Object doInBackground(String... params) {
//        Log.d(HotSearchView.TAG, "HotSearchHttpGet doInBackground : " + params[0].toString());

        try {
            //get
            /*HttpGet httpRequest = new HttpGet(params[0].toString());
            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse httpResponse = httpClient.execute(httpRequest);*/

            //post
            HttpPost postRequest = new HttpPost(HotSearchInfo.HMB_WEIBO_URL);
            JSONObject para = new JSONObject();
            para.put("deviceId", /*"865821030006764"*/MobileInfoUtil.getIMEI(mHotSearchView.getContext()));
            para.put("model", Build.MODEL);
            para.put("romVersion", MobileInfoUtil.getHMBVersion());
            Log.d(HotSearchView.TAG,"postRequest url : " + HotSearchInfo.HMB_WEIBO_URL);
            Log.d(HotSearchView.TAG,"postRequest deviceId : " + MobileInfoUtil.getIMEI(mHotSearchView.getContext())+", model : " + Build.MODEL+", romVersion : " + MobileInfoUtil.getHMBVersion());
            StringEntity entity = new StringEntity(para.toString(), "utf-8");
            HttpClient client = new DefaultHttpClient();
            postRequest.setEntity(entity);
            HttpResponse httpResponse = client.execute(postRequest);

            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String strResult = EntityUtils.toString(httpResponse.getEntity());
                return strResult;
            } else {
                Log.d(HotSearchView.TAG, "HotSearchHttpGet doInBackground error : " + httpResponse.getStatusLine().toString());
                return "error";
            }
        } catch (ClientProtocolException e) {
            Log.e(HotSearchView.TAG, "HotSearchHttpGet doInBackground ClientProtocolException : " + e.toString());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e(HotSearchView.TAG, "HotSearchHttpGet doInBackground IOException : " + e.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(HotSearchView.TAG, "HotSearchHttpGet doInBackground JSONException : " + e.toString());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object result) {
        super.onPostExecute(result);
        boolean loadScuccess = true;
        ArrayList<HotSearchInfo> searchResults = new ArrayList<HotSearchInfo>();
        if (result == null) {
            loadScuccess = false;
        }else {
            Log.d(HotSearchView.TAG, "HotSearchHttpGet onPostExecute : " + result);
            try {
                JSONObject jsonObject = new JSONObject(result.toString());
                int retCode = jsonObject.getInt("retCode");
                String errorMsg = jsonObject.getString("errorMsg");

                if(retCode != 0){
                    loadScuccess = false;
                    Log.d(HotSearchView.TAG, "HotSearchHttpGet onPostExecute errorMsg : " + errorMsg);
                }else {
                    JSONObject search;
                    try{
                        String sc = jsonObject.getString("search");
                        search = new JSONObject(sc);
                    }catch (JSONException e) {
                        // TODO Auto-generated catch block
                        search = jsonObject.getJSONObject("search");
                        Log.d(HotSearchView.TAG, "onPostExecute JSONException : " + e.toString());
                    }

                    JSONArray jsonArray = search.getJSONArray("data");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jo = jsonArray.getJSONObject(i);
                        HotSearchInfo info = new HotSearchInfo();
                        info.s_id = jo.getInt("id");
                        info.word = jo.getString("word");
                        info.flag = jo.getInt("flag");
                        info.num = jo.getInt("num");
                        info.app_query_link = jo.getString("app_query_link");
                        info.h5_query_link = jo.getString("h5_query_link");
                        info.flag_link = jo.getString("flag_link");
                        searchResults.add(info);
                    }
                    if (searchResults == null || searchResults.size() < HotSearchView.SHOW_COUNT) {
                        loadScuccess = false;
                    }
                }

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                loadScuccess = false;
                e.printStackTrace();
                Log.d(HotSearchView.TAG, "onPostExecute JSONException : " + e.toString());
            }
        }
        if(loadScuccess){
            mHotSearchView.updateData(searchResults);
            mHotSearchView.loadHotSearchSuccess(loadScuccess);
        }else {
            if(!mHotSearchView.isEmpty()){
                mHotSearchView.loadHotSearchSuccess(true);
                mHotSearchView.updateViews();
            }
        }
    }

//    protected void onPostExecute(Object result) {
//        super.onPostExecute(result);
//        boolean loadScuccess = false;
//        if (result == null) {
//            if (!mHotSearchView.isEmpty() && !loadScuccess) {
//                mHotSearchView.loadHotSearchSuccess(true);
//                mHotSearchView.updateViews();
//            } else {
//                mHotSearchView.loadHotSearchSuccess(loadScuccess);
//            }
//            return;
//        }
//        Log.d(HotSearchView.TAG, "HotSearchHttpGet onPostExecute : " + result);
//        ArrayList<HotSearchInfo> searchResults = new ArrayList<HotSearchInfo>();
//        try {
//            JSONObject jsonObject = new JSONObject(result.toString());
//            JSONArray jsonArray = jsonObject.getJSONArray("data");
//            StringBuilder builder = new StringBuilder();
//            for (int i = 0; i < jsonArray.length(); i++) {
//                JSONObject jo = jsonArray.getJSONObject(i);
//                HotSearchInfo info = new HotSearchInfo();
//                info.s_id = jo.getInt("id");
//                info.word = jo.getString("word");
//                info.flag = jo.getInt("flag");
//                info.num = jo.getInt("num");
//                info.app_query_link = jo.getString("app_query_link");
//                info.h5_query_link = jo.getString("h5_query_link");
//                info.flag_link = jo.getString("flag_link");
//                searchResults.add(info);
//            }
//            if (searchResults != null && searchResults.size() > HotSearchView.SHOW_COUNT) {
//                mHotSearchView.updateData(searchResults);
//                loadScuccess = true;
//            }
//        } catch (JSONException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//            Log.d(HotSearchView.TAG, "onPostExecute JSONException : " + e.toString());
//        }
//        if (!mHotSearchView.isEmpty() && !loadScuccess) {
//            mHotSearchView.loadHotSearchSuccess(true);
//            mHotSearchView.updateViews();
//        } else {
//            mHotSearchView.loadHotSearchSuccess(loadScuccess);
//        }
//    }

    @Override
    protected void onProgressUpdate(Object... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }
}
