package com.android.launcher3.theme;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.android.launcher3.theme.utils.ResUtils;

/**
 * Created by antino on 17-3-14.
 */
public class DefaultIconGetter extends IconGetterAbsImpl {
    public static String THEME_PKG="com.android.dlauncher";

    @Override
    protected boolean init(Context context) {
        try {
            this.mContext = createThemeContext(context);
            if (mContext == null) return false;
        } catch (Exception e) {
            Log.i(TAG,"DefaultIconGetter init fail");
            return false;
        }
        mDensityDpi = context.getResources().getConfiguration().densityDpi;
        final String[] packageClasseIcons = ResUtils.getStringArray(mContext,THEME_PKG,"icon_array");
        for (String packageClasseIcon : packageClasseIcons) {
            String[] packageClasses_Icon = packageClasseIcon.split("#");
            if (packageClasses_Icon.length == 2) {
                String[] packageClasses = packageClasses_Icon[0].split("\\|");
                for (String s : packageClasses) {
                    mLabel_Icons.put(s.trim(), packageClasses_Icon[1]);
                }
            }
        }
        themeName = ResUtils.getString(mContext,THEME_PKG,"theme_name");
        themeVersion = ResUtils.getString(mContext,THEME_PKG,"theme_version");
        if(mLabel_Icons.isEmpty())return false;
        return true;
    }

    private Context createThemeContext(Context context) throws Exception{
        Context themeContext = null;
        if(context!=null){
            if(context.getPackageName().equals(THEME_PKG)){
                return context;
            }
            themeContext = context.createPackageContext(THEME_PKG, Context.CONTEXT_IGNORE_SECURITY);
        }
        return themeContext;
    }

    @Override
    protected Drawable getMask() {
        return ResUtils.getIconDrawable(mContext.getPackageName(),"ic_mask", mContext, mDensityDpi);
    }

    @Override
    protected Drawable getBackground() {
        return ResUtils.getIconDrawable(mContext.getPackageName(),"ic_bg", mContext, mDensityDpi);
    }

    @Override
    protected Drawable getZoomtemplate() {
        return ResUtils.getIconDrawable(mContext.getPackageName(),"ic_zoom_template", mContext, mDensityDpi);
    }

    @Override
    protected Drawable getIconByName(String key) {
        Log.i("IconProvider","key = "+key);
        if(key==null)return null;
        String[] keys = key.split("\\.");
        if(keys!=null&&keys.length==2){
            key = keys[0];
            Log.i("IconProvider","2:key = "+key);
        }else if(keys.length==3){
            Log.i("IconProvider","3:key = "+key);
            key = keys[0];
        }else{
            return null;
        }
        return ResUtils.getIconDrawable(mContext.getPackageName(),key, mContext, mDensityDpi);
    }

    @Override
    protected Uri getUriByName(String key) {
        if(key==null)return null;
        String[] keys = key.split("\\.");
        if(keys!=null&&keys.length==2){
            key = keys[0];
            Log.i("IconProvider","2:key = "+key);
        }else{
            return null;
        }
        return ResUtils.getIconUri(mContext.getPackageName(),key, mContext, mDensityDpi);
    }

    @Override
    public Integer getColor(String name) {
        return ResUtils.getColor(mContext,THEME_PKG,name);
    }
}
