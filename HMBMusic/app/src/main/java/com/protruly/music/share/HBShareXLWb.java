package com.protruly.music.share;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.protruly.music.R;
import com.sina.weibo.sdk.api.MusicObject;
import com.sina.weibo.sdk.api.WeiboMessage;
import com.sina.weibo.sdk.api.share.BaseResponse;
import com.sina.weibo.sdk.api.share.IWeiboHandler;
import com.sina.weibo.sdk.api.share.IWeiboShareAPI;
import com.sina.weibo.sdk.api.share.SendMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.WeiboShareSDK;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.constant.WBConstants;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.utils.UIUtils;
import com.sina.weibo.sdk.utils.Utility;

import java.text.SimpleDateFormat;

import com.protruly.music.util.LogUtil;

/**
 * Created by hujianwei on 17-8-31.
 */

public class HBShareXLWb implements IWeiboHandler.Response{
    private static final String TAG = "HBShareXLWb";
    private static HBShareXLWb mInstance;
    private IWeiboShareAPI mWeiboShareAPI = null;
    private Context mContext = null;

    private AuthInfo authInfo;
    private Oauth2AccessToken mAccessToken;
    private SsoHandler mSsoHandler;

    private static final String HB_SHARE_DEFAULT_TEXT = "Music(IUNI OS)";

    public static final int HB_WEIBO_FAILED = 0;
    public static final int HB_WEIBO_SUCCESS = 1;
    public static final int HB_WEIBO_CANCEL = 2;
    public static final int HB_WEIBO_UNKOWNERRO = 3;
    public static final int HB_SEND_SUCCESS = 4;
    public static final int HB_SEND_FAILED = 5;

    private HBWeiBoCallBack mCallBack = null;
    private boolean isInitsdk = false;
    private InitSdkThread mInitSdkThread = null;

    public interface HBWeiBoCallBack {
        public void onSinaWeiBoCallBack(int ret);
    }

    protected HBShareXLWb(Context context) {
        com.sina.weibo.sdk.utils.LogUtil.enableLog();
        mContext = context;
        isInitsdk = false;
        authInfo = new AuthInfo(mContext, HBXLWbConstants.APP_KEY,
                HBXLWbConstants.REDIRECT_URL, HBXLWbConstants.SCOPE);
    }

    private boolean isInitAuth() {
        boolean init = false;
        LogUtil.i(TAG, "HBShareXLWbisInitAuth 0:");
        mAccessToken = HBAccessTokenKeeper.readAccessToken(mContext);
        if (mAccessToken.isSessionValid()) {
            LogUtil.i(TAG, "HBShareXLWbisInitAuth 1:");
            updateTokenView(true);
            init = true;
        }

        return init;
    }

    public void startWeiBoEx(Activity activity, HBWeiBoCallBack callBack) {
        mCallBack = callBack;
        if (isInitsdk) {
            initAuth(activity);
            LogUtil.d(TAG, "startWeiBoEx isInitsdk return");
            return;
        }
        if (mInitSdkThread != null) {
            mInitSdkThread.cancel();
        }
        mInitSdkThread = new InitSdkThread(activity);
        mInitSdkThread.start();
    }

    class InitSdkThread extends Thread {
        private boolean iscancel = false;
        private Activity mActivity;

        public InitSdkThread(Activity activity) {
            mActivity = activity;
        }

        public void cancel() {
            iscancel = true;
        }

        @Override
        public void run() {
            mWeiboShareAPI = WeiboShareSDK.createWeiboAPI(mContext,
                    HBXLWbConstants.APP_KEY);
            mWeiboShareAPI.registerApp();
            if (iscancel) {
                return;
            }
            Handler handler = new Handler(mActivity.getMainLooper());
            handler.post(new Runnable() {

                @Override
                public void run() {
                    initAuth(mActivity);
                    isInitsdk = true;
                }
            });
        }
    }

    private void initAuth(Activity activity) {
        LogUtil.i(TAG, "HBShareXLWbinitAuth 1:");
        if (isInitAuth()) {
            mCallBack.onSinaWeiBoCallBack(HB_WEIBO_SUCCESS);
        } else {
            LogUtil.d(TAG, "initAuth 2");
            mSsoHandler = new SsoHandler(activity, authInfo);// Sdk update
            // authorizeClientSso authorize
            mSsoHandler.authorizeClientSso(new HBAuthListener());
        }
    }

    public static synchronized HBShareXLWb getInstance(Context context) {
        if (mInstance == null)
            mInstance = new HBShareXLWb(context);

        return mInstance;
    }

    public void clearData() {
        mCallBack = null;
        if (mInstance != null) {
            mInstance = null;
        }
        if (mInitSdkThread != null) {
            mInitSdkThread.cancel();
            mInitSdkThread = null;
        }
        return;
    }

    public void handleWeiboResponse(Intent intent) {
        if (mWeiboShareAPI != null) {
            mWeiboShareAPI.handleWeiboResponse(intent, this);
        }
    }

    /**
     * 接收微客户端博请求的数据。 当微博客户端唤起当前应用并进行分享时，该方法被调用。
     *
     * @param baseResp
     *            微博请求数据对象
     * @see {@link IWeiboShareAPI#handleWeiboRequest}
     */
    @Override
    public void onResponse(BaseResponse baseResp) {
        LogUtil.i(TAG, "HBShareXLWbonResponse baseResp.errCode:"
                + baseResp.errCode);
        switch (baseResp.errCode) {
            case WBConstants.ErrorCode.ERR_OK:
                Toast.makeText(mContext, R.string.xlweibosdk_share_success,
                        Toast.LENGTH_SHORT).show();
                break;
            case WBConstants.ErrorCode.ERR_CANCEL:
                Toast.makeText(mContext, R.string.xlweibosdk_share_canceled,
                        Toast.LENGTH_SHORT).show();
                break;
            case WBConstants.ErrorCode.ERR_FAIL:
                Toast.makeText(
                        mContext,
                        mContext.getString(R.string.xlweibosdk_share_failed)
                                + "Error Message: " + baseResp.errMsg,
                        Toast.LENGTH_SHORT).show();
                break;
        }

        return;
    }

    private void updateTokenView(boolean hasExisted) {
        String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                .format(new java.util.Date(mAccessToken.getExpiresTime()));

        String format = mContext.getString(R.string.xlweibosdk_token_format);
        LogUtil.d(TAG,
                "HBShareXLWbupdateTokenView 1:"
                        + (String.format(format, mAccessToken.getToken(), date)));
        // Toast.makeText(mContext, String.format(format,
        // mAccessToken.getToken(), date), Toast.LENGTH_SHORT).show();

        String message = String.format(format, mAccessToken.getToken(), date);
        if (hasExisted) {
            message = mContext.getString(R.string.xlweibosdk_token_existed)
                    + "\n" + message;
        }

        LogUtil.d(TAG, "HBShareXLWbupdateTokenView 2:" + message);
        // Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    public void authorizeCallBack(int requestCode, int resultCode, Intent data) {
        // SSO 授权回调, 重要：发起 SSO 登陆的 Activity 必须重写 onActivityResult
        if (mSsoHandler != null) {
            mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
        }
    }

    private class HBAuthListener implements WeiboAuthListener {

        @Override
        public void onCancel() {
            LogUtil.i(TAG, "HBShareXLWb    onCancel ");
            Toast.makeText(mContext, R.string.xlweibosdk_share_canceled,
                    Toast.LENGTH_LONG).show();
            if (mCallBack != null) {
                mCallBack.onSinaWeiBoCallBack(HB_WEIBO_CANCEL);
            }
        }

        @Override
        public void onComplete(Bundle values) {
            LogUtil.d(TAG, "HBShareXLWb    onComplete 1 values:" + values.toString());
            // 从 Bundle 中解析 Token
            mAccessToken = Oauth2AccessToken.parseAccessToken(values);
            if (mAccessToken.isSessionValid()) {
                LogUtil.d(TAG, "HBShareXLWb    onComplete 2 values:");
                // 显示 Token
                updateTokenView(false);

                // 保存 Token 到 SharedPreferences
                HBAccessTokenKeeper
                        .writeAccessToken(mContext, mAccessToken);
                Toast.makeText(mContext, R.string.weibosdk_auth_success,
                        Toast.LENGTH_SHORT).show();
                if (mCallBack != null) {
                    LogUtil.d(TAG, "HBShareXLWb    onComplete 2.1 values .");
                    mCallBack.onSinaWeiBoCallBack(HB_WEIBO_SUCCESS);
                }
            } else {
                LogUtil.d(TAG, "HBShareXLWb    onComplete 3 values:");
                // 以下几种情况，您会收到 Code：
                // 1. 当您未在平台上注册的应用程序的包名与签名时；
                // 2. 当您注册的应用程序包名与签名不正确时；
                // 3. 当您在平台上注册的包名和签名与您当前测试的应用的包名和签名不匹配时。
                String code = values.getString("code");
                String message = mContext
                        .getString(R.string.weibosdk_auth_failed);
                if (!TextUtils.isEmpty(code)) {
                    message = message + "\nObtained the code: " + code;
                }
                Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
                if (mCallBack != null) {
                    mCallBack.onSinaWeiBoCallBack(HB_WEIBO_UNKOWNERRO);
                }
            }
        }

        @Override
        public void onWeiboException(WeiboException e) {
            LogUtil.d(TAG,
                    "HBShareXLWb    onWeiboException getMessage:" + e.getMessage());
            UIUtils.showToast(mContext, "Auth exception : " + e.getMessage(),
                    Toast.LENGTH_LONG);
            if (mCallBack != null) {
                mCallBack.onSinaWeiBoCallBack(HB_WEIBO_FAILED);
            }
        }

    }

    private static class HBWbMusicInfo {
        private String mUrl = null;
        private String mDataUrl = null;
        private String mTitle = null;
        private String mArtist = null;
        private Bitmap mBitmap = null;
        private int mDuration = 10;

        public HBWbMusicInfo(String mUrl, String mDataUrl, String mTitle,
                                 String mArtist, Bitmap bitmap, int mDuration) {
            super();
            this.mUrl = mUrl;
            this.mDataUrl = mDataUrl;
            this.mTitle = mTitle;
            this.mArtist = mArtist;
            this.mDuration = mDuration;
            this.mBitmap = bitmap;
        }
    };

    private HBWbMusicInfo mWbMusicInfo = null;

    public void sendMusic2XLWb(String url, String dataurl, String title,
                               String artist, Bitmap bitmap, int duration, Activity activity) {
        if (mInstance == null || (url == null && dataurl == null)) {
            mWbMusicInfo = null;
            if (mCallBack != null) {
                mCallBack.onSinaWeiBoCallBack(HB_SEND_FAILED);
            }
            return;
        }

        mWbMusicInfo = new HBWbMusicInfo(url, dataurl, title, artist,
                bitmap, duration);
        LogUtil.i(TAG, "    sendMusic2XLWb 1 url:" + url + ",title:"
                + title + ",artist:" + artist + ",dataurl:" + dataurl
                + " bitmap:" + bitmap);
        sendResp(activity);
        return;
    }

    private void sendResp(Activity activity) {
        if (mWbMusicInfo == null) {
            Toast.makeText(mContext,
                    mContext.getString(R.string.xlweibosdk_share_failed),
                    Toast.LENGTH_SHORT).show();
            if (mCallBack != null) {
                mCallBack.onSinaWeiBoCallBack(HB_SEND_FAILED);
            }
            return;
        }

        try {
            if (mWeiboShareAPI == null) {
                mWeiboShareAPI = WeiboShareSDK.createWeiboAPI(mContext,
                        HBXLWbConstants.APP_KEY);
                mWeiboShareAPI.registerApp();
            }
            if (mWeiboShareAPI.isWeiboAppSupportAPI()) {
                mWeiboShareAPI.registerApp();//BUG #14995
                int supportApi = mWeiboShareAPI.getWeiboAppSupportAPI();
                LogUtil.i(TAG, "HBShareXLWb    sendResp 2 supportApi:" + supportApi);
                sendSingleMessage(mWbMusicInfo, activity);
            } else {
                clearData();
                if (mCallBack != null) {
                    mCallBack.onSinaWeiBoCallBack(HB_SEND_FAILED);
                }
                Toast.makeText(mContext, R.string.xlweibosdk_not_support_api,
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.i(TAG, "HBShareXLWb    sendResp 3 fail:");
            e.printStackTrace();
            clearData();
            if (mCallBack != null) {
                mCallBack.onSinaWeiBoCallBack(HB_SEND_FAILED);
            }
            Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        return;
    }

    /*
     * 第三方应用发送请求消息到微博，唤起微博分享界面。 当{@link IWeiboShareAPI#getWeiboAppSupportAPI()}
     * < 10351 时，只支持分享单条消息，即 文本、图片、网页、音乐、视频中的一种，不支持Voice消息。
     */
    private void sendSingleMessage(HBWbMusicInfo musicInfo,
                                   final Activity activity) {
        if (musicInfo == null) {
            if (mCallBack != null) {
                mCallBack.onSinaWeiBoCallBack(HB_SEND_FAILED);
            }
            return;
        }

        Bitmap thumb = null;
        // LogUtil.d(TAG, "mBitmap:"+musicInfo.mBitmap);
        if (musicInfo.mBitmap == null) {
            thumb = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.album_art_default);
        } else {
            thumb = musicInfo.mBitmap;
        }
        // 1. 初始化微博的分享消息
        // 用户可以分享文本、图片、网页、音乐、视频中的一种
        WeiboMessage weiboMessage = new WeiboMessage();
        weiboMessage.mediaObject = getMusicObj(musicInfo, thumb);
        // 2. 初始化从第三方到微博的消息请求
        SendMessageToWeiboRequest request = new SendMessageToWeiboRequest();
        // 用transaction唯一标识一个请求
        request.transaction = String.valueOf(System.currentTimeMillis());
        request.message = weiboMessage;

        // 3. 发送请求消息到微博，唤起微博分享界面
        boolean result = mWeiboShareAPI.sendRequest(activity, request);
        LogUtil.d(TAG, "sendSingleMessage end:" + result);
        if (thumb != null && !thumb.isRecycled()) {
            thumb.recycle();
        }
        if (mCallBack != null) {
            mCallBack.onSinaWeiBoCallBack(HB_SEND_SUCCESS);
        }
    }


    /**
     * 创建多媒体（音乐）消息对象。
     *
     * @return 多媒体（音乐）消息对象。
     */
    private MusicObject getMusicObj(HBWbMusicInfo musicInfo, Bitmap bm) {
        Log.i(TAG, "HBShareXLWb    getMusicObj    len:");
        // 创建媒体消息
        MusicObject musicObject = new MusicObject();
        musicObject.identify = Utility.generateGUID();
        musicObject.title = musicInfo.mTitle;// mShareMusicView.getTitle();
        musicObject.description = musicInfo.mArtist;// mShareMusicView.getShareDesc();
        musicObject.setThumbImage(bm);
        musicObject.actionUrl = musicInfo.mUrl;// mShareMusicView.getShareUrl();
        musicObject.dataUrl = musicInfo.mUrl;
        musicObject.h5Url = musicInfo.mUrl;
        musicObject.duration = musicInfo.mDuration;
        musicObject.defaultText = HB_SHARE_DEFAULT_TEXT;
        return musicObject;
    }
}
