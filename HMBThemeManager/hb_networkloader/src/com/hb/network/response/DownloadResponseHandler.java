package com.hb.network.response;

import java.io.File;

/**
 *@des 下载回调
 */
public abstract class DownloadResponseHandler {

    public abstract void onFinish(File download_file);
    public abstract void onProgress(long currentBytes, long totalBytes);
    public abstract void onFailure(String error_msg);
}
