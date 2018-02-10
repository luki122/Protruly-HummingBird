package com.android.launcher3.theme.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

/**
 * Created by antino on 16-11-9.
 */
public class ResUtils {
    public static int  getDimen(Context context, String resPkg, String resName, int defaultValue){
        int dimen =1;
        int resId =0;
        Resources resources = context.getResources();
        if (resources != null) {
            resId = resources.getIdentifier(resName, "dimen",
                    resPkg);
        }
        if(resId==0){
            return defaultValue;
        }
        dimen = resources.getDimensionPixelSize(resId);
        return dimen;
    }

    public static Integer getColor(Context context,String resPkg, String resName){
        Resources resources = context.getResources();
        int resId =0;
        if (resources != null) {
            resId = resources.getIdentifier(resName, "color",
                    resPkg);
        }
        if(resId == 0){
            return null;
        }
        return new Integer(resources.getColor(resId));
    }

    public static boolean getBoolean(Context context, String resPkg, String resName, boolean defaultValue){
        boolean  result =false;
        int resId =0;
        Resources resources = context.getResources();
        if (resources != null) {
            resId = resources.getIdentifier(resName, "bool",
                    resPkg);

        }
        if(resId==0){
            return false;
        }
        result = resources.getBoolean(resId);
        return result;
    }

    public static Drawable getIconDrawable(String packageName, String resourceName,
                                           Context context, int iconDpi) {
        Drawable drawable = null;
        Resources resources = context.getResources();
        if (resources != null) {
            int resId = resources.getIdentifier(resourceName, "drawable",
                    packageName);
            if (resId == 0) {
                return null;
            }
            drawable = resources.getDrawableForDensity(resId,iconDpi);
        }
        return drawable;
    }

    public static String getString(Context context, String resPkg, String resName){
        String result = "";
        int resId =0;
        Resources resources = context.getResources();
        if (resources != null) {
            resId = resources.getIdentifier(resName, "string",
                    resPkg);
        }
        if(resId==0){
            return null;
        }
        result = resources.getString(resId);
        return result;
    }

    public static String[] getStringArray(Context context, String resPkg, String resName){
        String[] result = null;
        int resId =0;
        Resources resources = context.getResources();
        if (resources != null) {
            resId = resources.getIdentifier(resName, "array",
                    resPkg);
        }
        if(resId==0){
            return null;
        }
        result = resources.getStringArray(resId);
        return result;
    }

    //lijun add
    public static Uri getIconUri(String packageName, String resourceName, Context context, int iconDpi) {
        Drawable drawable = null;
        Resources resources = context.getResources();
        int resId = 0;
        if (resources != null) {
            resId = resources.getIdentifier(resourceName, "drawable",
                    packageName);
            if (resId == 0) {
                return null;
            }
        } else {
            return null;
        }
        return getResourceUri(resources, packageName, resId);
    }

    private static Uri getResourceUri(Resources resources, String appPkg, int res)
            throws Resources.NotFoundException {
        String resPkg = resources.getResourcePackageName(res);
        String type = resources.getResourceTypeName(res);
        String name = resources.getResourceEntryName(res);
        return makeResourceUri(appPkg, resPkg, type, name);
    }

    private static Uri makeResourceUri(String appPkg, String resPkg, String type, String name)
            throws Resources.NotFoundException {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(ContentResolver.SCHEME_ANDROID_RESOURCE);
        uriBuilder.encodedAuthority(appPkg);
        uriBuilder.appendEncodedPath(type);
        if (!appPkg.equals(resPkg)) {
            uriBuilder.appendEncodedPath(resPkg + ":" + name);
        } else {
            uriBuilder.appendEncodedPath(name);
        }
        return uriBuilder.build();
    }
}
