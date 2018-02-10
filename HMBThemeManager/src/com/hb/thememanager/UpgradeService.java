package com.hb.thememanager;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.hb.thememanager.database.DatabaseFactory;
import com.hb.thememanager.database.ThemeDatabaseController;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.request.UpgradeRequest;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.UpgradeResponse;
import com.hb.thememanager.http.response.adapter.UpgradeResult;
import com.hb.thememanager.model.Fonts;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.TLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexluo on 17-9-5.
 */

public class UpgradeService extends Service implements Runnable{

    private static final String TAG = "UpgradeService";
    private static final int HAS_NEWVERSION = 1;
    private ThemeDatabaseController mFontDbController;
    private ThemeDatabaseController mThemeDbController;
    private Http mHttp;

    private HandlerThread mHandlerThread = new HandlerThread(TAG);
    private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mHandler.post(this);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(this);
        mHandlerThread.quitSafely();
    }

    private void checkNewVersion(List<Fonts> fonts,List<Theme> themes){
        ArrayList<Theme> needCheckThemes = new ArrayList<>();
        if(!listIsEmpty(fonts)){
            needCheckThemes.addAll(fonts);
        }

        if(!listIsEmpty(themes)){
            needCheckThemes.addAll(themes);
        }


        final UpgradeRequest request = new UpgradeRequest(UpgradeService.this,Theme.THEME_NULL);
        request.setThemes(needCheckThemes);

        if(mHttp == null){
            mHttp = Http.getHttp(UpgradeService.this);
        }
        if(listIsEmpty(needCheckThemes)){
            return;
        }
        mHttp.post(request.getMyUrl(),request.createJsonRequest(),new RawResponseHandler(){

            @Override
            public void onSuccess(int statusCode, String resultStr) {
                TLog.d(TAG,"check new Version->"+resultStr);
                if(!TextUtils.isEmpty(resultStr)) {
                    UpgradeResponse response = (UpgradeResponse) request.parseResponse(resultStr);
                    List<UpgradeResult> resultLists = response.getUpgradeResult();
                    if(!listIsEmpty(resultLists)){
                        setupNewVersionStatus(resultLists);
                    }

                }
            }

            @Override
            public void onFailure(int statusCode, String error_msg) {
                //do nothing
                mFontDbController.close();
                mThemeDbController.close();
            }
        });

    }


    @Override
    public void run() {

        if(mFontDbController == null){
            mFontDbController = DatabaseFactory.createDatabaseController(Theme.FONTS,UpgradeService.this);
        }
        if(mThemeDbController == null){
            mThemeDbController = DatabaseFactory.createDatabaseController(Theme.THEME_PKG,UpgradeService.this);
        }

        List<Fonts> localFonts = mFontDbController.getThemes();
        List<Theme> localThemes = mThemeDbController.getThemes();
        checkNewVersion(localFonts,localThemes);
    }

    private void setupNewVersionStatus(List<UpgradeResult> results){
        try {
            for (UpgradeResult r : results) {
                if (r.getStatus() == HAS_NEWVERSION) {
                    if (r.type == Theme.FONTS) {
                        Theme fonts = mFontDbController.getThemeById(r.id);
                        if (fonts != null) {
                            fonts.hasNewVersion = Theme.HAS_NEW_VERSION;
                            mFontDbController.updateTheme(fonts);
                        }
                    } else if (r.type == Theme.THEME_PKG) {
                        Theme theme = mThemeDbController.getThemeById(r.id);
                        if (theme != null) {
                            theme.hasNewVersion = Theme.HAS_NEW_VERSION;
                            mThemeDbController.updateTheme(theme);
                        }
                    }
                }
            }
        }finally {
            mFontDbController.close();
            mThemeDbController.close();
        }


    }




    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    private boolean listIsEmpty(List list){
        return list == null || list.size() == 0;
    }


}
