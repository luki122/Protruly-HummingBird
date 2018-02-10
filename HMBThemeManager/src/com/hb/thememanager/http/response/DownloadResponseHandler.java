package com.hb.thememanager.http.response;

import java.io.File;

/**
 *@des 下载回调
 */
public abstract class DownloadResponseHandler {

    public abstract void onFinish(File download_file);
    public abstract void onProgress(long surplusCurrentBytes, long surplusTotalBytes);
    public abstract void onFailure(String error_msg);
}
