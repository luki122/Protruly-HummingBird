package com.hb.thememanager.http.response.adapter;

/**
 * Created by alexluo on 17-9-8.
 */

public class UpgradeResult {

    public int type;
    public String id;
    public int status;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
