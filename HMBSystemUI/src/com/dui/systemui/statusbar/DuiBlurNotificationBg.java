package com.dui.systemui.statusbar;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.Blur;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.statusbar.phone.ActivityStarter;
import com.android.systemui.statusbar.phone.PhoneStatusBar;


/**
 * Created by chenheliang on 17-3-16.
 */

public class DuiBlurNotificationBg extends FrameLayout {

    private static final int SCALE_FACTOR = 8;
    private DisplayMetrics mDisplayMetrics;
    private Matrix mDisplayMatrix;
    private static Rect mClipRect = new Rect();
    private ImageView mImageView;
    private Display mDisplay;
    private Bitmap mScreenBitmap;
    private Handler mHandler = new Handler();
    private ActivityStarter mActivityStarter;
    private View mNotifyManager,mBgScrimView;
    private int mClipHeight=0;
    private int mManagerTop,mManagerHeight;
    private boolean mShowBg=false;
    private int mLastRotation;
    private int mState=StatusBarState.SHADE;
    private static final int TYPE_LAYER_MULTIPLIER=10000;
    private static final int TYPE_LAYER_OFFSET =1000;
    private static final int STATUSBAR_TYPE=16;
    private boolean mIsKeyguardPulldown=false;
    private TextView mManagerText;
    private WallpaperManager mWallpaperManager;

    private static final String LOCK_WALLPAPER_PATH = "/data/hummingbird/theme/lockscreen_wallpaper/lockscreen_wallpaper";

    public DuiBlurNotificationBg(Context context) {
        super(context);
        init();
    }
    public DuiBlurNotificationBg(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DuiBlurNotificationBg(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        mDisplay = windowManager.getDefaultDisplay();
        mDisplayMetrics = new DisplayMetrics();

        mDisplayMatrix = new Matrix();
        mManagerTop=getResources().getDimensionPixelSize(R.dimen.hb_manager_margintop);
        mManagerHeight=getResources().getDimensionPixelSize(R.dimen.hb_manager_Height);
        mWallpaperManager = (WallpaperManager) getContext().getSystemService(Context.WALLPAPER_SERVICE);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mImageView=(ImageView)findViewById(R.id.dui_id_notify_bg_image);
        mNotifyManager=findViewById(R.id.dui_id_notify_manager_containt);
        mBgScrimView=findViewById(R.id.hb_id_bg_scrim);
        mManagerText=(TextView) findViewById(R.id.dui_id_notify_manager);
        mManagerText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivityStarter.startActivity(new Intent("android.settings.NOTIFICATION_APP_LIST"),
                        true /* dismissShade */);
            }
        });
        updateBlueBgHight(0,0);
    }

    public void setActivityStarter(ActivityStarter activityStarter) {
        mActivityStarter = activityStarter;
    }

    private float getDegreesForRotation(int value) {
        switch (value) {
            case Surface.ROTATION_90:
                return 360f - 90f;
            case Surface.ROTATION_180:
                return 360f - 180f;
            case Surface.ROTATION_270:
                return 360f - 270f;
        }
        return 0f;
    }

    private int getRealRotation(int value){
        switch (value){
            case Surface.ROTATION_90:
                return Surface.ROTATION_270;
            case Surface.ROTATION_270:
                return Surface.ROTATION_90;
        }
        return value;
    }

    private int getWindLayer(){
        return STATUSBAR_TYPE*TYPE_LAYER_MULTIPLIER+TYPE_LAYER_OFFSET-1;
    }

    private void getCurrentImage(){
        //1.构建Bitmap
        if(mImageView==null){
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                long time = System.currentTimeMillis();
                mDisplay.getRealMetrics(mDisplayMetrics);
               /* float[] dims = {mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels};
                float degrees = getDegreesForRotation(mDisplay.getRotation());
                boolean requiresRotation = (degrees > 0);
                if (requiresRotation) {
                    // Get the dimensions of the device in its native orientation
                    mDisplayMatrix.reset();
                    mDisplayMatrix.preRotate(-degrees);
                    mDisplayMatrix.mapPoints(dims);
                    dims[0] = Math.abs(dims[0]);
                    dims[1] = Math.abs(dims[1]);
                }*/
                mLastRotation = mDisplay.getRotation();
                //Bitmap shotBitmap = SurfaceControl.screenshot((int)dims[0], (int)dims[1]);
                Bitmap shotBitmap=null;
                if(mState!=StatusBarState.SHADE){
                    shotBitmap=getWallpaper();
                }

                if(shotBitmap == null){
                    shotBitmap = SurfaceControl.screenshot(new Rect(),mDisplayMetrics.widthPixels,mDisplayMetrics.heightPixels,0,getWindLayer(),false,getRealRotation(mLastRotation));
                }

                if (shotBitmap == null) {
                    return;
                }
                /*if (requiresRotation) {
                    // Rotate the screenshot to the current orientation
                    Bitmap ss = Bitmap.createBitmap(mDisplayMetrics.widthPixels,
                            mDisplayMetrics.heightPixels, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(ss);
                    c.translate(ss.getWidth() / 2, ss.getHeight() / 2);
                    c.rotate(degrees);
                    c.translate(-dims[0] / 2, -dims[1] / 2);
                    c.drawBitmap(shotBitmap, 0, 0, null);
                    c.setBitmap(null);
                    // Recycle the previous bitmap
                    shotBitmap.recycle();
                    shotBitmap = ss;
                }*/
                //Log.d("chenhl","--------> 1 haoshi:"+(System.currentTimeMillis()-time));
                Matrix matrix = new Matrix();
                matrix.postScale(1.0f / SCALE_FACTOR, 1.0f / SCALE_FACTOR);
                // New Compress bitmap
                Bitmap mCompressBitmap = Bitmap.createBitmap(shotBitmap, 0, 0,
                        shotBitmap.getWidth(), shotBitmap.getHeight(), matrix, true);
                //shotBitmap.recycle();
                if (/*mState==StatusBarState.SHADE&&*/!shotBitmap.isRecycled()) {
                    shotBitmap.recycle();
                }

                final Bitmap oldBp = mScreenBitmap;
                mScreenBitmap= Blur.onStackBlur(mCompressBitmap,15);
                //Log.d("chenhl","-------->haoshi:"+(System.currentTimeMillis()-time));
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mImageView.setImageBitmap(mScreenBitmap);
                        if(oldBp!=null&&!oldBp.isRecycled()){
                            oldBp.recycle();
                        }
                    }
                });
            }
        }).start();
    }


    private Bitmap getWallpaper(){
        //Bitmap bitmap = BitmapFactory.decodeFile(LOCK_WALLPAPER_PATH);
        mWallpaperManager.forgetLoadedWallpaper();
        return mWallpaperManager.getLockscreenBitmap();
    }

    public void updateBg(){
        getCurrentImage();
    }

    public void updateBlueBgHight(float height,float transt){
        if(mClipHeight==height){
            return;
        }
        mClipHeight = (int)height;
       // if(mState!=StatusBarState.KEYGUARD) {
            float alp = Math.min(1, Math.max(0, (height - mManagerTop) / mManagerHeight));
            mNotifyManager.setAlpha(alp);
        //}
        mNotifyManager.setTranslationY(transt);
        if(height==0){
            height=1;
        }
        mClipRect.set(0, 0, getWidth(), (int)height);
        setClipBounds(mClipRect);
    }

    public int getClipHeight(){
        return mClipHeight;
    }

    public void setBlurBgVisible(int state){
        if(state== StatusBarState.KEYGUARD){

            if(mIsKeyguardPulldown || mState==StatusBarState.SHADE){
                setVisibility(View.GONE);
                setAlpha(0);
            }else {
                animate().cancel();
                animate().alpha(0)
                        .setDuration(500)
                        .setInterpolator(PhoneStatusBar.ALPHA_OUT)
                        .withEndAction(mManagerGone)
                        .start();
            }
            mIsKeyguardPulldown=false;
        }else if(state==StatusBarState.SHADE_LOCKED){

            getCurrentImage();
            animate().cancel();
            setVisibility(View.VISIBLE);
            animate().alpha(1)
                    .setDuration(500)
                    .setInterpolator(PhoneStatusBar.ALPHA_IN)
                    .start();
        }else{

            setVisibility(View.VISIBLE);
            animate().cancel();
            setAlpha(1);
        }
        mState = state;
    }

    public void setBlueKeyguardPulldown(boolean is){
        mIsKeyguardPulldown=is;
    }

    private Runnable mManagerGone = new Runnable() {
        @Override
        public void run() {
            //mNotifyManager.setVisibility(View.GONE);
            setVisibility(View.GONE);
        }
    };

    public void setmShowBg(boolean is){
        mShowBg =is;
    }
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mManagerText.setText(R.string.dui_notify_manager);
        FontSizeUtils.updateFontSize(mManagerText, R.dimen.hb_search_tips);
        if(mLastRotation==mDisplay.getRotation()){
            return;
        }
        if(mShowBg) {
            /*new Thread(new Runnable() {
                @Override
                public void run() {
                    mDisplay.getRealMetrics(mDisplayMetrics);
                    float currentdegrees = getDegreesForRotation(mDisplay.getRotation());
                    float lastDegrees = getDegreesForRotation(mLastRotation);
                    float degrees = currentdegrees-lastDegrees;
                    boolean requiresRotation = (degrees != 0);
                    if(requiresRotation){
                        mDisplayMatrix.reset();
                        mDisplayMatrix.setRotate(degrees, mScreenBitmap.getWidth() / 2, mScreenBitmap.getHeight() / 2);
                        mScreenBitmap = Bitmap.createBitmap(mScreenBitmap, 0, 0, mScreenBitmap.getWidth(), mScreenBitmap.getHeight(), mDisplayMatrix, true);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mImageView.setImageBitmap(mScreenBitmap);
                            }
                        });
                        mLastRotation = mDisplay.getRotation();
                    }
                }
            }).start();*/
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    getCurrentImage();
                }
            },550);

        }
    }
}
