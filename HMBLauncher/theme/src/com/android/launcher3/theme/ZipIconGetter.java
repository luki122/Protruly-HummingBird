package com.android.launcher3.theme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import com.android.launcher3.theme.utils.HbDrawableUtils;
import com.android.launcher3.theme.utils.PhotoUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by antino on 17-3-14.
 */
public class ZipIconGetter extends IconGetterAbsImpl{
    public static String TAG = ZipIconGetter.class.getSimpleName();
    private static String THEME_LOCAL_PATH = "/data/hummingbird/theme/current/icons";
    private static String THEME_CONFIG_PATH = "/icon_config.xml";

    private String mPath;
    private String localDrawablelPath;
    private String localConfigPath;

    private String mIconDpiFolder;
    private String mIconDpiFolderHigh;
    private String mIconDpiFolderUnder;

    Bitmap mask_regular;
    Bitmap bg;
    Bitmap zoomTemplate;

    @Override
    public boolean init(Context context) {
        mPath = THEME_LOCAL_PATH;
        if (context == null || mPath == null) {
            Log.e(TAG, "ZIPIconGetter init failed context="+context+",mPath="+mPath);
            return false;
        }
        mContext = context;
        mDensityDpi = context.getResources().getConfiguration().densityDpi;
        mIconDpiFolder = getDrawableDpiFolder(mDensityDpi, 0);
        mIconDpiFolderHigh = getDrawableDpiFolder(mDensityDpi, 1);
        mIconDpiFolderUnder = getDrawableDpiFolder(mDensityDpi, -1);
        localDrawablelPath = mPath + mIconDpiFolder;
        localConfigPath = mPath + THEME_CONFIG_PATH;

        ZIPThemeConfigParseByPull tp = new ZIPThemeConfigParseByPull();
        InputStream instream = null;
        try {
            File file = new File(localConfigPath);
            if (file.exists()) {
                instream = new FileInputStream(localConfigPath);
                if (instream == null) {
                    Log.e(TAG, "can't create inputStream");
                    return false;
                }
                tp.parse(instream);
            } else {
                Log.e(TAG, "can't find config.xml path : " + localConfigPath);
                return false;
            }
            if (!tp.hasData()) return false;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "init IOException : " + e.toString());
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "init Exception : " + e.toString());
            return false;
        }finally {
            if (null != instream) {
                try {
                    instream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        mLabel_Icons = tp.getmLabel_Icons();
        mLabel_colors = tp.getmLabel_colors();
        Log.d(TAG, "mLabel_Icons size : " + mLabel_Icons.size()+", mLabel_colors size : " +mLabel_colors.size());
        themeName = tp.getThemeName();
        themeVersion = tp.getThemeVersion();
        if (getMask() == null || getBackground() == null||getZoomtemplate() == null) {
            Log.e(TAG, "no mask or backgroud icon so ZipIconGetter init failed");
            return false;
        }
        return true;
    }

    @Override
    protected Drawable getMask() {
        if(mask_regular == null){
            mask_regular = getBitmapByName("ic_mask.png");
        }
        if(mask_regular == null)return null;
        return new BitmapDrawable(mContext.getResources(),Bitmap.createBitmap(mask_regular));
    }

    @Override
    protected Drawable getBackground() {
        if(bg == null){
            bg = getBitmapByName("ic_bg.png");
        }
        if(bg == null)return null;
        return new BitmapDrawable(mContext.getResources(),Bitmap.createBitmap(bg));
    }

    @Override
    protected Drawable getZoomtemplate() {
        if(zoomTemplate == null){
            zoomTemplate = getBitmapByName("ic_zoom_template.png");
        }
        if(zoomTemplate == null)return null;
        return new BitmapDrawable(mContext.getResources(),Bitmap.createBitmap(zoomTemplate));
    }

    @Override
    protected Drawable getIconByName(String iconName) {
        String iconPath = localDrawablelPath + "/" + iconName;
        File f = new File(iconPath);
        Log.e(TAG,"f:"+f.getPath()+",can read : " + f.canRead()+",can write : "+f.canWrite());
        Drawable drawable = null;
        float scale = 1.0f;
        if (!f.exists()) {
            Log.e(TAG,"f2:"+f.getPath()+",can read : " + f.canRead()+",can write : "+f.canWrite());
            iconPath = mPath + mIconDpiFolderHigh + "/" + iconName;
            scale = getScaleFroDpi(mDensityDpi, true);
            f = new File(iconPath);
            if (!f.exists()) {
                iconPath = mPath + mIconDpiFolderUnder + "/" + iconName;
                scale = getScaleFroDpi(mDensityDpi, false);
                f = new File(iconPath);
                if (!f.exists()) {
                    return null;
                }
            }
        }
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(f);
            drawable = HbDrawableUtils.decodeDrawableFromStream(fin,mContext.getResources());
        } catch (Exception e) {
            Log.d(TAG,e.getMessage());
        }finally {
            if (null != fin) {
                try {
                    fin.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //drawable = Drawable.createFromPath(iconPath);
        if(drawable == null)return null;
        if(scale!=1.0&&!iconName.contains(".9.")){
            drawable = PhotoUtils.zoomDrawable(mContext.getResources(),drawable,scale);
            Log.e(TAG,"Drawable iconName:"+iconName+",scale:"+scale + ",icon size : " + drawable.getIntrinsicWidth() + "," + drawable.getIntrinsicHeight());
        }
        //drawable.setBounds(0,0,drawable.getIntrinsicWidth()*mDensityDpi,drawable.getIntrinsicHeight()*mDensityDpi);
        //drawable = new BitmapDrawable(mContext.getResources(), getBitmapByName(iconName));

        return drawable;
    }

    @Override
    protected Uri getUriByName(String iconName) {
        String iconPath = localDrawablelPath + "/" + iconName;
        File f = new File(iconPath);
        Log.e(TAG,"f:"+f.getPath()+",can read : " + f.canRead()+",can write : "+f.canWrite());
        Drawable drawable = null;
        float scale = 1.0f;
        if (!f.exists()) {
            iconPath = mPath + mIconDpiFolderHigh + "/" + iconName;
            scale = getScaleFroDpi(mDensityDpi, true);
            f = new File(iconPath);
            if (!f.exists()) {
                iconPath = mPath + mIconDpiFolderUnder + "/" + iconName;
                scale = getScaleFroDpi(mDensityDpi, false);
                f = new File(iconPath);
                if (!f.exists()) {
                    return null;
                }
            }
        }
        drawable = Drawable.createFromPath(iconPath);

        Uri uri = Uri.parse(iconPath);
        return uri;
    }

    public Bitmap getBitmapByName(String iconName) {
        Bitmap result = null;
        FileInputStream fin = null;
        try {
            String iconPath = localDrawablelPath + "/" + iconName;
            File f = new File(iconPath);
            Log.e(TAG,"f:"+f.getPath()+",can read : " + f.canRead()+",can write : "+f.canWrite());
            Drawable drawable = null;
            float scale = 1.0f;
            if (!f.exists()) {
                iconPath = mPath + mIconDpiFolderHigh + "/" + iconName;
                scale = getScaleFroDpi(mDensityDpi, true);
                f = new File(iconPath);
                if (!f.exists()) {
                    iconPath = mPath + mIconDpiFolderUnder + "/" + iconName;
                    scale = getScaleFroDpi(mDensityDpi, false);
                    f = new File(iconPath);
                    if (!f.exists()) {
                        return null;
                    }
                }
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            fin = new FileInputStream(f);
            result = BitmapFactory.decodeStream(fin, null, options).copy(Bitmap.Config.ARGB_8888, true);
            Log.d(TAG, "Bitmap iconName:" + iconName + ",icon size : " + result.getWidth() + "," + result.getHeight());
            if (scale != 1.0f) {
                Log.e(TAG, "Bitmap iconName:" + iconName + ",scale:" + scale);
                result = PhotoUtils.zoom(result, scale);
            }
        } catch (Exception e) {
            Log.i(TAG, "getBitmapByName() :", e);
            e.printStackTrace();
            return null;
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
    /**
     *
     * @param dpi
     * @param level -1:under 0:cur 1:high
     * @return
     */
    public String getDrawableDpiFolder(int dpi ,int level){
        String dpiFolder;
        if(level == -1){
            if(dpi <= 120){
                dpiFolder = "/drawable-ldpi";
            }else if(dpi <= 160){
                dpiFolder = "/drawable-ldpi";
            }else if(dpi <= 240){
                dpiFolder = "/drawable-mdpi";
            }else if(dpi <= 320){
                dpiFolder = "/drawable-hdpi";
            }else if(dpi <= 480){
                dpiFolder = "/drawable-xhdpi";
            }else {
                dpiFolder = "/drawable-xxhdpi";
            }
        }else if(level == 1){
            if(dpi <= 120){
                dpiFolder = "/drawable-mdpi";
            }else if(dpi <= 160){
                dpiFolder = "/drawable-hdpi";
            }else if(dpi <= 240){
                dpiFolder = "/drawable-xhdpi";
            }else if(dpi <= 320){
                dpiFolder = "/drawable-xxhdpi";
            }else if(dpi <= 480){
                dpiFolder = "/drawable-xxxhdpi";
            }else {
                dpiFolder = "/drawable-xxxhdpi";
            }
        }else{
            if(dpi <= 120){
                dpiFolder = "/drawable-ldpi";
            }else if(dpi <= 160){
                dpiFolder = "/drawable-mdpi";
            }else if(dpi <= 240){
                dpiFolder = "/drawable-hdpi";
            }else if(dpi <= 320){
                dpiFolder = "/drawable-xhdpi";
            }else if(dpi <= 480){
                dpiFolder = "/drawable-xxhdpi";
            }else {
                dpiFolder = "/drawable-xxxhdpi";
            }
        }

        return dpiFolder;
    }

    public float getScaleFroDpi(int dpi ,boolean isHight){
        float scale = 1.0f;
        if(isHight){
            if(dpi <= 120){
                scale = 120/160f;
            }else if(dpi <= 160){
                scale = 160/240f;
            }else if(dpi <= 240){
                scale = 240/320f;
            }else if(dpi <= 320){
                scale = 320/480f;
            }else if(dpi <= 480){
                scale = 480/640f;
            }else {
                scale = 1.0f;
            }
        }else {
            if(dpi <= 120){
                scale = 1.0f;
            }else if(dpi <= 160){
                scale = 160/120;
            }else if(dpi <= 240){
                scale = 240/160;
            }else if(dpi <= 320){
                scale = 320/240;
            }else if(dpi <= 480){
                scale = 480/320f;
            }else {
                scale = 640/480f;
            }
        }
        return scale;
    }
}
