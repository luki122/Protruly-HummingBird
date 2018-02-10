package com.hb.network.response;

/**
 *@des raw 字符串结果回调
 */
public abstract class RawResponseHandler implements IResponseHandler {

    public abstract void onSuccess(int statusCode, String response);

    @Override
    public void onProgress(long currentBytes, long totalBytes) {

    }
}
