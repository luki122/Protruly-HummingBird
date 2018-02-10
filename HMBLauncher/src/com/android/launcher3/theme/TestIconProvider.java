package com.android.launcher3.theme;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.compat.LauncherActivityInfoCompat;

/**
 * Created by antino on 17-3-14.
 */
public class TestIconProvider extends IconProvider {
    IconManager mIconManager;
    TestIconProvider(Context context){
        mIconManager = IconManager.getInstance(context);
        if (mIconManager.issLauncherNeedCleanCaches()) {
            mIconManager.setsLauncherNeedCleanCaches(false,context);
            LauncherAppState launcherAppState = LauncherAppState.getInstance();
            if (launcherAppState != null) {
                launcherAppState.clearIcons();
            }
        }
    }

    public Drawable getIcon(LauncherActivityInfoCompat info, int iconDpi) {
        Drawable result=null;
        if(mIconManager!=null){
            Log.i(TAG,"getIcon   1");
            result = mIconManager.getIconDrawable(info.getComponentName(),null);
        }
        if(result==null){
            Log.i(TAG,"getIcon 2");
            result = info.getIcon(iconDpi);
        }
        Log.i(TAG,"getIcon 3");
        return result;
    }
    public Bitmap normalizeIcons(Bitmap bitmap) {
        return mIconManager.normalizeIcons(bitmap);
    }
    public Drawable getIconFromManager(Resources resources, String packageName, int resId){
        return  mIconManager.getIconFromManager(resources,packageName,resId);
    }
    public Integer getColor(String name , int resId ,Resources res){
        Integer color = mIconManager.getColor(name);
        if(color==null){
        return res.getColor(resId);
        }else {
            return color;
        }
    }
}
