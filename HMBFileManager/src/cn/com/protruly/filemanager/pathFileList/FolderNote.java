package cn.com.protruly.filemanager.pathFileList;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

import cn.com.protruly.filemanager.utils.Util;

/**
 * Created by liushitao on 17-5-12.
 */

public class FolderNote {

    private static Map<String, String> mFolderMap;
    private static String mPhoneStorage = "/storage/emulated/0";

    static {
        mFolderMap = new HashMap<>();
        mFolderMap.put(mPhoneStorage + "/Movies", "视频");
        mFolderMap.put(mPhoneStorage + "/Music", "音乐");
        mFolderMap.put(mPhoneStorage + "/Pictures", "图片");
        mFolderMap.put(mPhoneStorage + "/DCIM", "相册");
        mFolderMap.put(mPhoneStorage + "/Pictures/Screenshots", "截图");
        mFolderMap.put(mPhoneStorage + "/Download", "系统下载");
        mFolderMap.put(mPhoneStorage + "/DCIM/Camera", "系统相机");
        mFolderMap.put(mPhoneStorage + "/alipay", "支付宝钱包");
        mFolderMap.put(mPhoneStorage + "/Android", "安卓");
        mFolderMap.put(mPhoneStorage + "/baidu", "百度");
        mFolderMap.put(mPhoneStorage + "/tencent", "腾讯");
        mFolderMap.put(mPhoneStorage + "/BaiduMap", "百度地图");
        mFolderMap.put(mPhoneStorage + "/browser", "浏览器");
        mFolderMap.put(mPhoneStorage + "/taobao", "淘宝");
        mFolderMap.put(mPhoneStorage + "/ctrip", "携程旅行");
        mFolderMap.put(mPhoneStorage + "/didi", "滴滴打车");
        mFolderMap.put(mPhoneStorage + "/downloaded_rom", "系统更新包");
        mFolderMap.put(mPhoneStorage + "/netease", "网易");
        mFolderMap.put(mPhoneStorage + "/qiyivideo", "奇艺影视");
        mFolderMap.put(mPhoneStorage + "/QQBrowser", "QQ浏览器");
        mFolderMap.put(mPhoneStorage + "/sogou", "搜狗");
        mFolderMap.put(mPhoneStorage + "/UCDownloads", "UC浏览器");
        mFolderMap.put(mPhoneStorage + "/AndroidZip", "压缩文件");
    }

    public static String getFolderName(String paramString) {
        Log.d("bql","Locale.getDefault().toString():"+Locale.getDefault().toString());
        if ((Locale.getDefault().toString().equals("zh_CN_#Hans")|| Locale.getDefault().toString().equals("zh_TW")
        ||Locale.getDefault().toString().equals("zh_CN")) && mFolderMap.get(paramString) != null)
            return "| "+(String)mFolderMap.get(paramString);
        return "";
    }

    public static boolean needToShowFolderName(String paramString) {
        return mFolderMap.containsKey(paramString);
    }
}
