package com.android.launcher3.theme.cache;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

/**
 * Created by lijun on 17-4-21.
 */

public class BitmapCacheDelegate {
    private static BitmapMemoryCache mMemoryCache;
    private static BitmapSdcardCache mSdcarCache;
    private static Resources res;

    public BitmapCacheDelegate(Context context) {
        this(context, false, true);
    }

    public BitmapCacheDelegate(Context context, boolean useMemoryCache, boolean useSDCardCache) {
        res = context.getResources();
        if (useMemoryCache) {
            mMemoryCache = new BitmapMemoryCache(res);
        } else {
            mMemoryCache = null;
        }
        if (useSDCardCache && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            mSdcarCache = new BitmapSdcardCache(res);
        } else {
            mSdcarCache = null;
        }
    }

    public void setCacheConfig(boolean useMemoryCache, boolean useSDCardCache) {
        if (useMemoryCache && mMemoryCache == null) {
            mMemoryCache = new BitmapMemoryCache(res);
        }else if(!useMemoryCache && mMemoryCache != null) {
            mMemoryCache.clean();
        }
        if (useSDCardCache && mSdcarCache == null) {
            mSdcarCache = new BitmapSdcardCache(res);
        }
    }

    public void clean() {
        if (mSdcarCache != null) {
            mSdcarCache.clean();
        }
        if (mMemoryCache != null) {
            mMemoryCache.clean();
        }
    }

    public void addBitmap(String key, Bitmap bitmap) {
        if (mMemoryCache != null && mMemoryCache.getBitmapFromMemCache(key) == null) {
            mMemoryCache.addBitmapToMemoryCache(key, bitmap);
        }
        if (mSdcarCache != null && mSdcarCache.getIcon(key) == null) {
            mSdcarCache.save(key, bitmap);
        }
    }

    public void addBitmapToMemory(String key, Bitmap bitmap) {
        if (mMemoryCache != null && mMemoryCache.getBitmapFromMemCache(key) == null) {
            mMemoryCache.addBitmapToMemoryCache(key, bitmap);
        }
    }

    public Bitmap getBitmap(String key) {
        Log.d("BitmapCache", "getBitmapFromMemCache by key:" + key);
        Bitmap result = null;
        if (mMemoryCache != null) {
            result = mMemoryCache.getBitmapFromMemCache(key);
            if (result != null) {
                addBitmap(key, result);
            }
        }
        if (result == null && mSdcarCache != null) {
            result = mSdcarCache.getIcon(key);
            if (result != null) {
                addBitmapToMemory(key, result);
            }
        }
        return result;
    }

    public Drawable getDrawable(String key) {
        Bitmap bitmap = getBitmap(key);
        return bitmap == null ? null : new BitmapDrawable(res, bitmap);
    }

}
