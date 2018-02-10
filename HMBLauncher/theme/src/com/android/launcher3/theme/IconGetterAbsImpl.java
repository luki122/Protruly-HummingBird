package com.android.launcher3.theme;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.UserHandle;
import android.text.TextUtils;

import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.theme.utils.PhotoUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by antino on 17-3-14.
 */
public abstract class IconGetterAbsImpl implements IIconGetter{
    protected static String TAG = "IconGetter";
    protected Context mContext = null;
    protected HashMap<String, String> mLabel_Icons = new HashMap<String,String>();
    protected HashMap<String, Integer> mLabel_colors = new HashMap<String,Integer>();
    protected String themeName;
    protected String themeVersion;
    protected int mDensityDpi;

    @Override
    public Drawable getIconDrawable(ResolveInfo info, UserHandle user) {
        if(info!=null&&info.activityInfo!=null){
            return getIconDrawable(info.activityInfo,user);
        }
        return null;
    }

    @Override
    public Drawable getIconDrawable(ActivityInfo info, UserHandle user) {
        return getIconDrawable(info.packageName,info.name,user);
    }

    @Override
    public Drawable getIconDrawable(String pkg, UserHandle user) {
        return getIconDrawable(pkg,null,user);
    }

    protected Drawable getIconDrawable(String pkg,String cls,UserHandle user){
        Drawable result = getIconFromTheme(pkg,cls,user);
        if(result == null){
            result = getIconFromApk(pkg,cls,user);
        }else {
            return result;
        }
        if(result == null ) return null;
        result = new BitmapDrawable(mContext.getResources(), PhotoUtils.composite(result,getMask(),getBackground(),getZoomtemplate()));
        return result;
    }

    @Override
    public Bitmap getIcon(ResolveInfo info, UserHandle user) {
        if(info!=null&&info.activityInfo!=null){
            return getIcon(info.activityInfo,user);
        }
        return null;
    }

    @Override
    public Bitmap getIcon(ActivityInfo info, UserHandle user) {
        return getIcon(info.packageName,info.name,user);
    }

    @Override
    public Bitmap getIcon(String pkg, UserHandle user) {
        return getIcon(pkg,null,user);
    }

    public Bitmap getIcon(String pkg,String cls,UserHandle user) {
        Drawable result = getIconFromTheme(pkg, cls, user);
        if (result == null) {
            result = getIconFromApk(pkg, cls, user);
        }else {
            Bitmap srcBmp = (result instanceof BitmapDrawable)?((BitmapDrawable) result).getBitmap():PhotoUtils.drawable2bitmap(result);
            return srcBmp;
        }
        if(result == null ) return null;
        return (PhotoUtils.composite(result, getMask(), getBackground(),getZoomtemplate()));
    }


    protected boolean init(Context context){
        return false;
    }

    protected Drawable getBackground() {
        return null;
    }

    protected Drawable getZoomtemplate() {
        return null;
    }

    protected Drawable getMask() {
        return null;
    }

    protected Drawable getIconByName(String key) {
        return null;
    }

    private Drawable getIconFromApk(String pkg, String cls, UserHandle user) {
        Drawable result = null;
        PackageManager pm = mContext.getPackageManager();

        if(cls!=null) {
            Intent it = new Intent();
            it.setClassName(pkg, cls);
            List<ResolveInfo> list = pm.queryIntentActivities(it, 0);
            if (list != null && !list.isEmpty()) {
                ResolveInfo rInfo = list.get(0);
                if (rInfo.activityInfo != null) {
                    result = rInfo.activityInfo.loadIcon(pm);
                }
                if (result == null) {
                    result = rInfo.loadIcon(pm);
                }

            }
        }
        if(result==null){
            ApplicationInfo info = getApplicationInfo(pkg,mContext);
            if (info != null) {
                try {
                    result = info.loadIcon(pm);
                } catch (Resources.NotFoundException e) {
                    result = null;
                }
            }
        }
        return result;
    }

    private Drawable getIconFromTheme(String pkg, String cls, UserHandle user){
        boolean isCurrentUser = isCurrentUser(user);
        Drawable result = null;
        if(pkg!=null){
            if(cls!=null){
                if(!isCurrentUser){
                    result = getIconByName(mLabel_Icons.get(pkg+"$"+cls)+"_other");
                }
                if(result==null||user==null){
                    result = getIconByName(mLabel_Icons.get(pkg+"$"+cls));
                }
            }else{
                result = getIconByName(mLabel_Icons.get(pkg));
            }
        }
        return result;
    }

    private ApplicationInfo getApplicationInfo(String pkg,Context context) {
        PackageManager pm = context.getPackageManager();
        ApplicationInfo info;
        try {
            info = pm.getApplicationInfo(pkg, 0);
        } catch (PackageManager.NameNotFoundException e1) {
            info = null;
        }
        return info;
    }

    public static boolean isCurrentUser(UserHandle user) {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN_MR1){
            if(user==null||android.os.Process.myUserHandle().equals(user)){
                return true;
            }else{
                return false;
            }
        }else{
            return true;
        }
    }

    @Override
    public Bitmap normalizeIcons(Bitmap bitmap) {
        return  PhotoUtils.compositeByBitmap(bitmap,getMask(),getBackground(),getZoomtemplate(),false);
    }

    public String getThemeName() {
        return themeName;
    }

    public String getThemeVersion() {
        return themeVersion;
    }

    public Uri getIconUri(String pkg, String cls, UserHandle user){
        Uri result = null;
        boolean isCurrentUser = isCurrentUser(user);
        if(pkg!=null){
            if(cls!=null){
                if(!isCurrentUser){
                    result = getUriByName(mLabel_Icons.get(pkg+"$"+cls)+"_other");
                }
                if(result==null||user==null){
                    result = getUriByName(mLabel_Icons.get(pkg+"$"+cls));
                }
            }else{
                result = getUriByName(mLabel_Icons.get(pkg));
            }
        }
        return result;
    }

    protected Uri getUriByName(String key){
        return null;
    }

    @Override
    public Integer getColor(String name) {
        if(TextUtils.isEmpty(name)){
            return null;
        }
        return mLabel_colors.get(name);
    }
}
