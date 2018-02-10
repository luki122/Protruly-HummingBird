package com.android.launcher3.colors;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;

import com.android.launcher3.theme.utils.PhotoUtils;
import com.android.launcher3.wallpaperpicker.WallpaperPicker;

import java.util.ArrayList;

/**
 * Created by lijun on 17-4-5.
 */

public class ColorManager {
    public static final String TAG = "ColorManager";

    public static int CC_TEXT_BLACK_DEFAULT_COLOR = 0xE5000000;

    int[] mColors = {CC_TEXT_BLACK_DEFAULT_COLOR};

    public int[] getColors(){
        return mColors;
    }
    public void  setColors(int color){
        this.mColors[0] = color;
    }
    private static ColorManager instance;

    public static ColorManager getInstance() {
        if (instance == null) {
            instance = new ColorManager();
        }
        return instance;
    }

    private ColorManager() {

    }

    public interface IWallpaperChange {
        void onWallpaperChange();
        void onColorChange(int[] colors);
    }

    private ArrayList<IWallpaperChange> mWallpaperChanges;

    public void addWallpaperCallback(IWallpaperChange cb) {
        if (mWallpaperChanges == null) {
            mWallpaperChanges = new ArrayList<>();
        }
        if (cb != null && !mWallpaperChanges.contains(cb)) {
            mWallpaperChanges.add(cb);
        }
    }

    public void removeWallpaperCallback(IWallpaperChange cb) {
        if (mWallpaperChanges != null && mWallpaperChanges.size() > 0 && mWallpaperChanges.contains(cb)) {
            mWallpaperChanges.remove(cb);
        }

    }
    public void dealWallpaperForLauncher(final Context context) {
        AsyncTask<Void, Void, Void> as = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                 WallpaperManager wpm = WallpaperManager.getInstance(context);
                Drawable d = wpm.getDrawable();
                WallpaperInfo wallpaperInfo = wpm.getWallpaperInfo();
                if (wallpaperInfo != null) {
                    for (WallpaperPicker.WallpaperTileInfo tileInfo : WallpaperPicker.findLiveWallpapers(context)) {
                        String packageName = wallpaperInfo.getPackageName();
                        if (packageName != null && packageName.equals(
                                ((WallpaperPicker.LiveWallpaperInfo) tileInfo).mInfo
                                        .getPackageName())) {
                            d = tileInfo.mThumb;
                            Log.d(TAG, "case LiveWallpaperInfo");
                        }
                    }
                }
                Bitmap wallpaper = null;
                if (d instanceof BitmapDrawable) {
                    wallpaper = ((BitmapDrawable) d).getBitmap();
                }
                if (wallpaper == null) {
                    mColors[0] = CC_TEXT_BLACK_DEFAULT_COLOR;
                } else {
                    int[] result = mColors;
                    result[0] = PhotoUtils.calcTextColor(wallpaper);
                }
                context.getSharedPreferences("com.android.launcher3.prefs",Context.MODE_PRIVATE).edit().putInt("default_text_color",mColors[0]).apply();
                return null;
            }


            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                mColorChameleonRun.run();
            }
        };
        as.execute();
    }
    public void dealWallpaperForLauncher(Bitmap wallpaper,final Runnable callback){
        if(wallpaper == null){
            mColors[0] = CC_TEXT_BLACK_DEFAULT_COLOR;
            callback.run();
        }else{
            AsyncTask<Bitmap,Void,Void> as = new AsyncTask<Bitmap, Void, Void>() {
                @Override
                protected Void doInBackground(Bitmap... params) {
                    int[] result = mColors;
                    if(params!=null&&params[0]!=null){
                        result[0] = PhotoUtils.calcTextColor(params[0]);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    callback.run();
                }
            };
            as.execute(wallpaper);
        }
    }

    private Runnable mWallpaperChameleonRun = new Runnable() {
        @Override
        public void run() {
            if (mWallpaperChanges != null) {
                for (IWallpaperChange change : mWallpaperChanges) {
                    change.onWallpaperChange();
                }
            }
        }
    };

    private Runnable mColorChameleonRun = new Runnable() {
        @Override
        public void run() {
            if (mWallpaperChanges != null) {
                for (IWallpaperChange change : mWallpaperChanges) {
                    change.onWallpaperChange();
                    change.onColorChange(mColors);
                }
            }
        }
    };

    public boolean isBlackText(){
        if(mColors[0]!=-1)
            return true;
        return false;
    }

}

