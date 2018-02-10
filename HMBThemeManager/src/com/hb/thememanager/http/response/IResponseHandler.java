package com.hb.thememanager.http.response;

/**
 * @author yangyi
 */
public interface IResponseHandler {

    void onFailure(int statusCode, String error_msg);

    void onProgress(long currentBytes, long totalBytes);
}
