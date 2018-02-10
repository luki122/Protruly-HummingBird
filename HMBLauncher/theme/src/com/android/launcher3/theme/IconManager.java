package com.android.launcher3.theme;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.UserHandle;
import android.util.Log;

import com.android.launcher3.theme.cache.BitmapCacheDelegate;
import com.android.launcher3.theme.cache.BitmapSdcardCache;
import com.android.launcher3.theme.utils.PhotoUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by antino on 17-3-14.
 */
public class IconManager {
    public static final String TAG = "IconManager";

    private static IconManager mInstance = null;
    private static IconGetterAbsImpl mRealGetter = null;
    private static BitmapCacheDelegate mBitmapCache = null;

    private static String themeName = "";
    private static String themeVersion = "";

    private static boolean sLauncherNeedCleanCaches = false;
    public static boolean issLauncherNeedCleanCaches() {
        return sLauncherNeedCleanCaches;
    }
    public static void setsLauncherNeedCleanCaches(boolean sLauncherNeedCleanCaches,Context context) {
        if(DefaultIconGetter.THEME_PKG.equals(context.getPackageName())){
            IconManager.sLauncherNeedCleanCaches = sLauncherNeedCleanCaches;
        }
    }

    private IconManager(Context context) {

    }

    public static synchronized IconManager getInstance(Context context) {
        return getInstance(context, false, false);
    }

    public static synchronized IconManager getInstance(Context context, boolean useMemoryCache, boolean useSDcardCache) {
        if (mInstance == null || mRealGetter == null) {
            mInstance = new IconManager(context);
            mRealGetter = new ZipIconGetter();
            boolean inited = mRealGetter.init(context);
            Log.d(TAG, "getInstance 1");
            if (!inited) {
                Log.d(TAG, "getInstance 2");
                mRealGetter = new DefaultIconGetter();
                inited = mRealGetter.init(context);
                if (!inited) {
                    Log.d(TAG, "getInstance 3");
                    mRealGetter = null;
                }
            }
            if (inited) {
                mBitmapCache = new BitmapCacheDelegate(context, useMemoryCache, useSDcardCache);
                String[] config = getConfig(context);
                if (config != null && config.length == 2 && config[0] != null && config[1] != null) {
                    themeName = config[0];
                    themeVersion = config[1];
                }

                checkThemeChanged(context);
                if (mRealGetter == null) {
                    mRealGetter = new ZipIconGetter();
                     inited = mRealGetter.init(context);
                    Log.d(TAG, "getInstance 11");
                    if (!inited) {
                        Log.d(TAG, "getInstance 22");
                        mRealGetter = new DefaultIconGetter();
                        inited = mRealGetter.init(context);
                        if (!inited) {
                            Log.d(TAG, "getInstance 33");
                            mRealGetter = null;
                        }
                    }
                }
            }
        }
        if(mBitmapCache != null){
            mBitmapCache.setCacheConfig(useMemoryCache,useSDcardCache);
        }
        return mInstance;
    }

    private static String[] getConfig(Context context) {
        String[] result = new String[2];
        String content = "";
        FileInputStream fin = null;
        try {
            File file = new File(BitmapSdcardCache.CONFIG_FILE_PATH);
            if (!file.exists()) {
                createConfigFileDirOrNot();
                return null;
            }

            fin = new FileInputStream(file);
            if (fin != null) {
                InputStreamReader inputreader = new InputStreamReader(fin);
                BufferedReader buffreader = new BufferedReader(inputreader);
                String line;
                while ((line = buffreader.readLine()) != null) {
                    content += line + "/";
                }
                result = content.split("/");
                Log.d(TAG, "getConfig result:" + result[0]+","+result[1]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != fin) {
                try {
                    fin.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return result;
    }

    private static void saveConfig(Context context) {
        FileOutputStream fos = null;
        try {
            File file = new File(BitmapSdcardCache.CONFIG_FILE_PATH);
            if (!file.exists()) {
                createConfigFileDirOrNot();
                file = new File(BitmapSdcardCache.CONFIG_FILE_PATH);
                Log.e(TAG, "saveConfig file not exists and create it");
            }
            fos = new FileOutputStream(file);
            String content = themeName + "\n" + themeVersion;
            fos.write(content.getBytes());
            fos.flush();
            Log.e(TAG, "saveConfig success");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public static void clearCaches() {
        mRealGetter = null;
        if (mBitmapCache != null) {
            mBitmapCache.clean();
        }
    }

    private static void checkThemeChanged(Context context) {
        if (mRealGetter == null) return;
        String tn = mRealGetter.getThemeName();
        String tv = mRealGetter.getThemeVersion();
        if(tn == null || tv == null){
            themeName = "";
            themeVersion = "";
            return;
        }
        Log.d(TAG,"themeName:"+tn + ", themVersion:"+tv);
        if (!themeName.equals(tn) || !themeVersion.equals(tv)) {
            clearCaches();
            themeName = tn;
            themeVersion = tv;
            saveConfig(context);
        }
        checkThemeChangedForLauncher(context,tn,tv);
    }

    private static void checkThemeChangedForLauncher(Context context,String tn,String tv){
        Log.d(TAG, "checkThemeChangedForLauncher tn:"+tn+", tv:" +tv);
        if(DefaultIconGetter.THEME_PKG.equals(context.getPackageName())){
            SharedPreferences sharedPreferences = context.getSharedPreferences("com.android.launcher3.prefs", Context.MODE_PRIVATE);
            Log.d(TAG, "checkThemeChangedForLauncher sharedPreferences:"+sharedPreferences);
            if(sharedPreferences!=null){
                String launcherTn = sharedPreferences.getString("launcher_theme_name","");
                String launcherTv = sharedPreferences.getString("launcher_theme_version","");
                Log.d(TAG, "checkThemeChangedForLauncher launcherTn : " + launcherTn);
                Log.d(TAG, "checkThemeChangedForLauncher launcherTv : " + launcherTv);
                if(!launcherTn.equals(tn) || !launcherTv.equals(tv)){
                    sLauncherNeedCleanCaches = true;
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("launcher_theme_name",tn);
                    editor.putString("launcher_theme_version",tv);
                    editor.apply();
                }
            }

        }
    }

    /**
     *
     * @param context
     * @return String[] String[0]:ThemeName String[1]:ThemeVersion
     */
    public static String[] getCurrentTheme(Context context){
        String[] theme = new String[2];
        if (mRealGetter != null) {
            theme[0] = mRealGetter.getThemeName();
            theme[1] = mRealGetter.getThemeVersion();
        }else {
            theme[0] = "";
            theme[1] = "";
        }
        return theme;
    }

    public Drawable getIconDrawable(ResolveInfo info, UserHandle user) {
        if (info != null && info.activityInfo != null) {
            return getIconDrawable(info.activityInfo, user);
        }
        return null;
    }

    public Drawable getIconDrawable(ActivityInfo info, UserHandle user) {
        return getIconDrawable(info.packageName, info.name, user);
    }

    public Drawable getIconDrawable(String pkg, UserHandle user) {
        return getIconDrawable(pkg, null, user);
    }

    public Bitmap getIcon(ResolveInfo info, UserHandle user) {
        if (info != null && info.activityInfo != null) {
            return getIcon(info.activityInfo, user);
        }
        return null;
    }

    public Bitmap getIcon(ActivityInfo info, UserHandle user) {
        return getIcon(info.packageName, info.name, user);
    }

    public Bitmap getIcon(String pkg, UserHandle user) {
        return getIcon(pkg, null, user);
    }

    public Drawable getIconDrawable(ComponentName componentName, UserHandle user) {
        return getIconDrawable(componentName.getPackageName(), componentName.getClassName(), user);
    }

    public Bitmap normalizeIcons(Bitmap bitmap) {
        if (mRealGetter == null) return null;
        return mRealGetter.normalizeIcons(bitmap);
    }

    public Drawable getIconDrawable(String pkg, String cls, UserHandle user) {
        if (mRealGetter == null) return null;
        String key = pkg + "$" + cls;
        Drawable result = null;
        if (mBitmapCache != null) {
            result = mBitmapCache.getDrawable(key);
        }
        if (result == null) {
            result = mRealGetter.getIconDrawable(pkg, cls, user);
            if (result != null) {
                mBitmapCache.addBitmap(key, PhotoUtils.drawable2bitmap(result));
            }
        }
        return result;
    }

    private Bitmap getIcon(String pkg, String cls, UserHandle user) {
        if (mRealGetter == null) return null;
        String key = pkg + "$" + cls;
        Bitmap result = null;
        if (mBitmapCache != null) {
            result = mBitmapCache.getBitmap(key);
        }
        if (result == null) {
            result = mRealGetter.getIcon(pkg, cls, user);
            if (result != null) {
                mBitmapCache.addBitmap(key, result);
            }
        }
        return result;
    }

    public Drawable getIconByName(String name){
        if (mRealGetter == null) return null;
        Drawable result = null;
        String key = name + ".png";
        if (mBitmapCache != null) {
            result = mBitmapCache.getDrawable(name);
        }
        if (result == null) {
            result = mRealGetter.getIconByName(key);
            if (result != null) {
                mBitmapCache.addBitmap(name,PhotoUtils.drawable2bitmap(result) );
            }
        }
        return result;
    }

    public Drawable getIconFromManager(Resources resources, String packageName, int resId){
        Drawable iconDrawable = getIconByName(packageName);
        if(iconDrawable!=null){
            return iconDrawable;
        }else if(resId>0) {
            return resources.getDrawable(resId);
        }else {
            return null;
        }
    }

    private Uri getIconUri(String pkg, UserHandle user){
        return mRealGetter.getIconUri(pkg, null, user);
    }

    private Uri getIconUri(String pkg, String cls, UserHandle user){
        return mRealGetter.getIconUri(pkg, cls, user);
    }

    private static boolean createConfigFileDirOrNot(){
        Log.e(TAG, "getConfig file not exists and create it");
        try {
            File rootDir = new File(BitmapSdcardCache.THEME_SDCARD_PATH);
            if (!rootDir.exists() || !rootDir.isDirectory()) {
                boolean create = rootDir.mkdirs();
                rootDir.setExecutable(true);//设置可执行权限
                rootDir.setReadable(true);//设置可读权限
                rootDir.setWritable(true);//设置可写权限
            }
            File file = new File(BitmapSdcardCache.CONFIG_FILE_PATH);
            file.createNewFile();
            file.setExecutable(true);//设置可执行权限
            file.setReadable(true);//设置可读权限
            file.setWritable(true);//设置可写权限
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Integer getColor(String name){
        return mRealGetter.getColor(name);
    }
}
