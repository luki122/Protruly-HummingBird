package com.protruly.music.share;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.protruly.music.R;
import com.protruly.music.downloadex.BitmapUtil;
import com.protruly.music.util.Globals;
import com.protruly.music.util.LogUtil;
import com.tencent.mm.sdk.constants.ConstantsAPI;
import com.tencent.mm.sdk.modelbase.BaseReq;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXMusicObject;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
/**
 * Created by hujianwei on 17-8-31.
 */

public class HBWxShare implements IWXAPIEventHandler{

    private static final String TAG = "HBWxShare";
    private static HBWxShare mInstance;
    private Context mContext = null;
    private IWXAPI mWxApi;
    private HBWxShareCallBack mWxShareCallBack = null;
    public static final int HB_CMD_GETMESSAGE_FROM_WX = 0;
    public static final int HB_CMD_SHOWMESSAGE_FROM_WX = 1;
    static enum HbRespStatus {
        HB_RESP_OK,
        HB_RESP_CANCEL,
        HB_RESP_SENT_FAILED,
        HB_RESP_DENIED,
        HB_RESP_UNKNOW,
    };
    private static final String HB_SHARE_DEFAULT_ARTIST = "DUI";
    private static final String HB_SHARE_DEFAULT_TITLE = "Music";
    private static final int HB_PIC_SIZE = (32*1024);

    private boolean mbSupport = true;
    private int wxSdkVersion = 0;
    private boolean mbInstalled = true;


    public interface HBWxShareCallBack{
        public void OnHbWxReq(int req);
        public void OnHbWxResp(HbRespStatus respType);
    };


    protected HBWxShare(Context context) {
        mWxApi = WXAPIFactory.createWXAPI(context, Globals.SHARE_WX_APP_ID, true);
        mWxApi.registerApp(Globals.SHARE_WX_APP_ID);
        mContext = context;
        mWxShareCallBack = null;
        initVersion();
    }

    private boolean initVersion(){
        wxSdkVersion = mWxApi.getWXAppSupportAPI();
        if (wxSdkVersion >= Globals.SHARE_TIMELINE_SUPPORTED_VERSION) {
            mbSupport = true;
        } else {
            mbSupport = false;
        }
        return mbSupport;
    }

    public static synchronized HBWxShare getInstance(Context context) {
        if(mInstance == null)
            mInstance = new HBWxShare(context);

        return mInstance;
    }

    public void setHandleIntentAndCallBack(Intent intent, HBWxShareCallBack callBack) {
        if (mInstance != null && mInstance.mWxApi != null) {
            boolean flag = mInstance.mWxApi.handleIntent(intent, this);
            Log.i(TAG, "HBWxShare setHandleIntent 1 flag:"+flag);
            registerWxShareCallBack(callBack);
        }

        return;
    }

    public static void unRegisterApp() {
        if (mInstance != null && mInstance.mWxApi != null) {
            mInstance.mWxApi.unregisterApp();
            mInstance.mWxShareCallBack = null;
        }
        mInstance = null;

        return;
    }

    private void registerWxShareCallBack(HBWxShareCallBack callBack) {
        if (mWxShareCallBack != null && callBack != null) {
            return;
        }

        mWxShareCallBack = callBack;
        return;
    }

    public void unRegisterWxShareCallBack() {
        mWxShareCallBack = null;
        return;
    }

    public void sendWebMusic2Wx(String url, boolean bSceneTimeline, String title, String artist) {
        if (mInstance == null || url == null || mWxApi == null) {
            return;
        }

        WXWebpageObject webpage = new WXWebpageObject();
        webpage.webpageUrl = url;
        WXMediaMessage msg = new WXMediaMessage(webpage);
        msg.title = title;
        msg.description = artist;
        Bitmap thumb = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.app_music);
        msg.thumbData = HBShareUtil.bmpToByteArray(thumb, true);

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("webpage");
        req.message = msg;
        req.scene = bSceneTimeline ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;
        mWxApi.sendReq(req);

        return;
    }

    private Bitmap getShareBitmap(String path, String bitrate, String title, String artist, String album) {
        Bitmap bm = null;

        if ((path != null && !TextUtils.isEmpty(path))) {
            //bm = BitmapFactory.decodeFile(path);
            LogUtil.iv(TAG, "HBWxShare getShareBitmap 1:");
            bm = BitmapUtil.decodeSampledBitmapFromFileForSmall(path, 70, 70);
        }

        if (bm == null) {
            try {

                //bm = BitmapFactory.decodeFile(imgpath);
            } catch (Exception e) {
                Log.i(TAG, "HBWxShare getShareBitmap fail");
                bm = null;
            }
        }

        if (bm == null) {
            LogUtil.iv(TAG, "HBWxShare getShareBitmap 3:");
            bm = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.album_art_default);
        }
        return bm;
    }

    //bSceneTimeline =true,则表示分享至 朋友圈，否则分享至会话:
    public void sendMusic2Wx(String url, String neturl, boolean bSceneTimeline, String title, String artist, String pic, String album, String bitrate, Bitmap bm) {
        if (mInstance == null ||
                (url == null && neturl == null)||
                mWxApi == null) {
            return;
        }

        if (!mWxApi.isWXAppInstalled()) {
            Toast.makeText(mContext, R.string.hb_wx_not_installed, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mbSupport) {
            if(!initVersion()){
                Toast.makeText(mContext, "wxSdkVersion = " + Integer.toHexString(wxSdkVersion) + "\ntimeline not supported", Toast.LENGTH_LONG).show();
                return;
            }
        }

        LogUtil.iv(TAG, "HBWxShare sendMusic2Wx pic:"+pic+",url:"+url+",neturl:"+neturl+",bm:"+bm);
        WXMusicObject music = new WXMusicObject();
        //music.musicUrl = "http://www.baidu.com";
        music.musicDataUrl = url;
        music.musicUrl = neturl ;//url;//"http://staff2.ustc.edu.cn/~wdw/softdown/index.asp/0042515_05.ANDY.mp3";
        //music.musicUrl="http://120.196.211.49/XlFNM14sois/AKVPrOJ9CBnIN556OrWEuGhZvlDF02p5zIXwrZqLUTti4o6MOJ4g7C6FPXmtlh6vPtgbKQ==/31353278.mp3";
        //music.musicDataUrl = neturl;
        //music.musicLowBandUrl = url;//neturl;

        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = music;
        msg.title = title;

        if (artist == null ||
                (artist != null && artist.isEmpty())) {
            msg.description = HB_SHARE_DEFAULT_ARTIST;
        } else {
            msg.description = artist;
        }

        Bitmap thumb = null;

        if (bm == null) {
            thumb = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.album_art_default);
        } else {
            thumb = bm;
        }

		/*if (pic == null ||
			(pic != null && pic.equals(HBSearchLyricActivity.DEFUALT_IMG_URL))) {
			thumb = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.album_art_default);
		} else {
			//thumb = getShareBitmap(pic, bitrate, title, artist, album);
		}*/

        msg.thumbData = HBShareUtil.bmpToByteArray(thumb, true);
        //Log.i(TAG, "HBWxShare sendMusic2Wx 1.2 success thumbData:"+msg.thumbData.length);
        LogUtil.iv(TAG, "HBWxShare getShareBitmap 4 msg.thumbData.length:"+msg.thumbData.length);
        if (msg.thumbData.length >= HB_PIC_SIZE) {
            thumb = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.album_art_default);
            msg.thumbData = HBShareUtil.bmpToByteArray(thumb, true);
        }

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("music");
        req.message = msg;
        req.scene = bSceneTimeline ? SendMessageToWX.Req.WXSceneTimeline : SendMessageToWX.Req.WXSceneSession;
        boolean flag = mWxApi.sendReq(req);

        //Log.i(TAG, "HBWxShare sendMusic2Wx 2 success:");
        return;
    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }

    @Override
    public void onReq(BaseReq req) {
        Log.i(TAG, "HBWxShare onReq req:"+req.getType());
        int ret = -1;

        switch (req.getType()) {
            case ConstantsAPI.COMMAND_GETMESSAGE_FROM_WX:
                ret = HB_CMD_GETMESSAGE_FROM_WX;
                break;

            case ConstantsAPI.COMMAND_SHOWMESSAGE_FROM_WX:
                ret = HB_CMD_SHOWMESSAGE_FROM_WX;
                break;

            default:
                break;
        }

        if (mWxShareCallBack != null) {
            mWxShareCallBack.OnHbWxReq(ret);
        }
    }

    @Override
    public void onResp(BaseResp resq) {
        Log.i(TAG, "HBWxShare onResp resq errCode:"+resq.errCode+",errStr:"+resq.errStr);
        HbRespStatus ret = HbRespStatus.HB_RESP_UNKNOW;

        switch (resq.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                //result = R.string.errcode_success;
                ret = HbRespStatus.HB_RESP_UNKNOW;
                break;

            case BaseResp.ErrCode.ERR_USER_CANCEL:
                //result = R.string.errcode_cancel;
                ret = HbRespStatus.HB_RESP_CANCEL;
                break;

            case BaseResp.ErrCode.ERR_SENT_FAILED:
                ret = HbRespStatus.HB_RESP_SENT_FAILED;
                break;

            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                //result = R.string.errcode_deny;
                ret = HbRespStatus.HB_RESP_DENIED;
                break;

            default:
                //result = R.string.errcode_unknown;
                ret = HbRespStatus.HB_RESP_UNKNOW;
                break;
        }

        if (mWxShareCallBack != null) {
            mWxShareCallBack.OnHbWxResp(ret);
        }
    }
}
