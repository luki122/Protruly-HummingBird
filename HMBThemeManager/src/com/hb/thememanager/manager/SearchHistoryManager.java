package com.hb.thememanager.manager;

import android.app.Activity;
import android.content.Context;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.hb.thememanager.database.SharePreferenceManager;
import com.hb.thememanager.utils.StringUtils;
import com.hb.thememanager.views.BannerView;

import java.util.HashMap;
import java.util.List;

/**
 * Created by caizhongting on 17-6-9.
 */

public class SearchHistoryManager {
    private static final String TAG = "SearchHistoryManager";
    private static SearchHistoryManager mInstance;

    private SearchHistoryManager(){
    }

    public static SearchHistoryManager getInstance(){
        if(mInstance == null){
            synchronized (SearchHistoryManager.class){
                if(mInstance == null){
                    mInstance = new SearchHistoryManager();
                }
            }
        }
        return mInstance;
    }

    public String[] getHistories(Context context){
        String allHistories = SharePreferenceManager.getStringPreference(context, SharePreferenceManager.KEY_SEARCH_HISTORY_ID, "");
        if(StringUtils.isEmpty(allHistories)){
            return null;
        }
        return allHistories.split("\\|");
    }

    public void append(Context context, String str){
        String allHistories = SharePreferenceManager.getStringPreference(context, SharePreferenceManager.KEY_SEARCH_HISTORY_ID, "");
        if(StringUtils.isEmpty(allHistories)){
            allHistories = str;
        }else {
            allHistories += "|" + str;
        }
        SharePreferenceManager.setStringPreference(context, SharePreferenceManager.KEY_SEARCH_HISTORY_ID, allHistories);
    }

    public void remove(Context context, String str){
        String allHistories = SharePreferenceManager.getStringPreference(context, SharePreferenceManager.KEY_SEARCH_HISTORY_ID, "");
        String remove = "";
        if(allHistories.startsWith(str)){
            remove = str + "\\|";
        }else{
            remove = "\\|" + str;
        }
        SharePreferenceManager.setStringPreference(context, SharePreferenceManager.KEY_SEARCH_HISTORY_ID, allHistories.replaceAll(remove, ""));
    }

    public void remove(Context context, int index){
        String[] histories = getHistories(context);
        if(histories != null){
            if(index >= 0 && index < histories.length) {
                remove(context, histories[index]);
            }
        }
    }

    public boolean has(Context context, String str){
        String allHistories = SharePreferenceManager.getStringPreference(context, SharePreferenceManager.KEY_SEARCH_HISTORY_ID, "");
        String[] hisArray = allHistories.split("\\|");
        for(String his : hisArray){
            if(his.equals(str)){
                return true;
            }
        }
        return false;
    }

    public int size(Context context){
        String[] histories = getHistories(context);
        if(histories == null) return 0;
        return histories.length;
    }

    public void removeAll(Context context){
        SharePreferenceManager.setStringPreference(context, SharePreferenceManager.KEY_SEARCH_HISTORY_ID, "");
    }
}
