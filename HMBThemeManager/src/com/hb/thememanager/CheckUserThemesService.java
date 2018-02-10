package com.hb.thememanager;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import com.hb.thememanager.database.DatabaseFactory;
import com.hb.thememanager.database.ThemeDatabaseController;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.request.PurchaseRecordRequest;
import com.hb.thememanager.http.request.ThemeRequest;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.ThemeListResponse;
import com.hb.thememanager.http.response.adapter.ThemeBody;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.User;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.TLog;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 用户主题主要记录用户已购买过的主题信息，目前只需要记录主题ID，主题type以及该主题
 * 被哪个用户购买了。主要目的为：
 * 1、通过这个记录实现主题列表中价格的变化，如果是已经被当前用户购买过的则显示已购买，
 * 如果已购买了并且下载了就显示已下载，免费主题已下载的情况显示已下载。
 * <br>
 * 2、在用户切换时通过记录的用户对应的主题信息来实现不同用户自己购买的主题的状态信息，
 *    例如A用户购买了主题a，A用户登录时主题a就要显示为已购买，而切换到B用户或者A用户退出时
 *    主题则要显示相关价格
 */

public class CheckUserThemesService extends Service implements Runnable{

    private static final String TAG = "CheckUserThemesService";
    private Http mHttp;
    private PurchaseRecordRequest mFontsRequest;
    private PurchaseRecordRequest mThemeRequest;
    private ThemeDatabaseController mDbController;
    private HandlerThread mHandlerThread = new HandlerThread(TAG);
    private Handler mHandler;
    private User mUser;
    @Override
    public void onCreate() {
        super.onCreate();
        mHttp = Http.getHttp(this);
        mThemeRequest = new PurchaseRecordRequest(this, Theme.THEME_PKG,Theme.THEME_PKG);
        mFontsRequest = new PurchaseRecordRequest(this, Theme.FONTS,Theme.FONTS);
        mThemeRequest.setPageNumber(0);
        mThemeRequest.setPageSize(100);
        mFontsRequest.setPageNumber(0);
        mFontsRequest.setPageSize(100);
        mDbController = DatabaseFactory.createDatabaseController(Config.DatabaseColumns.THEME_USERS,this);
        mUser = User.getInstance(this);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        if(!mUser.isLogin()){
            super.onStartCommand(intent, flags, startId);
        }

        mHandler.post(this);

        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void run() {
        List<Theme> userThemes = mDbController.getThemesByUser(Integer.parseInt(mUser.getId()));

        /**
         * 每次用户购买成功后都会把被购买的主题信息存到数据库中，只有在用户切换
         * 或者在系统数据被清空时才会去服务器读取用户已购买的主题信息
         */
        if(userThemes != null && userThemes.size() > 0){
            return;
        }


        mFontsRequest.setId(mUser.getId());
        mThemeRequest.setId(mUser.getId());
        mThemeRequest.request(mHttp,new RawResponseHandler() {
            @Override
            public synchronized void onFailure(int statusCode, String error_msg) {
                TLog.e(TAG,"check user's theme failure error_msg->"+error_msg+" statusCode->"+statusCode);
            }

            @Override
            public synchronized void onSuccess(int statusCode, String response) {
                insertOrUpdateUserTheme(Theme.THEME_PKG,response);

            }
        });

        mFontsRequest.request(mHttp,new RawResponseHandler() {
            @Override
            public synchronized void onFailure(int statusCode, String error_msg) {
                TLog.e(TAG,"check user's theme failure error_msg->"+error_msg+" statusCode->"+statusCode);
            }

            @Override
            public synchronized void onSuccess(int statusCode, String response) {
                insertOrUpdateUserTheme(Theme.FONTS,response);
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mHandler != null) {
            mHandler.removeCallbacks(this);
        }
        mHandlerThread.quitSafely();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void insertOrUpdateUserTheme(int themeType,String response){
        try {
            ThemeListResponse themeResponse = (ThemeListResponse) mThemeRequest.parseResponse(response);
            if(themeResponse.returnBody() != null){
                List<Theme> themes = ((ThemeBody)themeResponse.returnBody()).getThemes(Theme.THEME_NULL);
                if(themes != null && themes.size() > 0){
                    for(Theme theme:themes){
                        theme.userId = Integer.parseInt(mUser.getId());
                        theme.type = themeType;
                        mDbController.insertTheme(theme);
                    }
                }
            }
        }catch (Exception e){
            TLog.e(TAG,"check user's theme catched exception->"+e);
        }finally {
            mDbController.close();
        }

    }


}
