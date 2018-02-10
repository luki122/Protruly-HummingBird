package com.hb.netmanage.entity;

import java.io.Serializable;

/**
 * Created by zhaolaichao on 17-5-25.
 */

public class SmsStatus implements Serializable{
    private int errorCode;
    private String status;

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
