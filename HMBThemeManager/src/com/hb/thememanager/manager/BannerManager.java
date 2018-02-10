package com.hb.thememanager.manager;

import android.app.Activity;
import android.content.Context;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.hb.thememanager.views.BannerView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by caizhongting on 17-6-9.
 */

public class BannerManager {
    private static final String TAG = "BannerManager";
    private static BannerManager mInstance;
    private HashMap<String, LruCache<Integer, BannerView>> mCache;
    private HashMap<String, Integer> mKeys;


    private BannerManager(){
        mCache = new HashMap<>();
        mKeys = new HashMap<>();
    }

    public static BannerManager getInstance(){
        if(mInstance == null){
            synchronized (BannerManager.class){
                if(mInstance == null){
                    mInstance = new BannerManager();
                }
            }
        }
        return mInstance;
    }

    private int getKey(String className){
        Integer key = mKeys.get(className);
        if(key == null){
            key = new Integer(0);
        }
        mKeys.put(className, ++key);
        return key;
    }

    public void registerBanner(BannerView banner){
        Context context = banner.getContext();
        if(context instanceof Activity){
            Activity activity = (Activity) context;
            String key = activity.getComponentName().getClassName();
            LruCache<Integer, BannerView> banners = mCache.get(key);
            if(banners == null){
                banners = new LruCache<Integer, BannerView>(10){
                    @Override
                    protected void entryRemoved(boolean evicted, Integer key, BannerView oldValue, BannerView newValue) {
                        if(oldValue != null && oldValue != newValue) {
                            oldValue.stopScroll();
                        }
                    }
                };
                mCache.put(key, banners);
            }
            int index =  getKey(key);
            Log.d(TAG,"registerBanner -> activity : "+key+" ; banners size : "+banners.size()+" ; index : "+index);
            banners.put(index,banner);
        }
    }

    public void releaseBanner(String className){
        LruCache<Integer, BannerView> banners = mCache.get(className);
        if(banners != null) {
            banners.trimToSize(0);
        }
        mCache.remove(className);
        mKeys.remove(className);
    }

    public void releaseAll(){
        for(String key : mCache.keySet()){
            LruCache<Integer, BannerView> banners = mCache.get(key);
            banners.trimToSize(0);
        }
        mCache.clear();
        mKeys.clear();
    }
}
