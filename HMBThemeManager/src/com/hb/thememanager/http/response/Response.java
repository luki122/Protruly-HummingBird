package com.hb.thememanager.http.response;

import com.hb.thememanager.http.response.adapter.ResponseBody;

/**
 * 网络返回数据
 * Created by caizhongting on 17-6-13.
 */

public abstract class Response {


    public static final int STATUS_CODE_OK = 0;

    public static final int STATUS_CODE_ERROR = 1;

    /**
     * 网络状态
     */
    public String retCode;



    public ResponseBody returnBody(){
        return null;
    }


    public String getRetCode() {
        return retCode;
    }

    public void setRetCode(String retCode) {
        this.retCode = retCode;
    }


    @Override
    public String toString() {
        return "Response{" +
                "retCode='" + retCode + '\'' +
                '}';
    }
}
