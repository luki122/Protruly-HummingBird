package com.hb.thememanager.http.response;

import com.hb.thememanager.http.response.adapter.ResponseBody;
import com.hb.thememanager.http.response.adapter.UpgradeResult;

import java.util.List;

/**
 * Created by alexluo on 17-9-8.
 */

public class UpgradeResponse extends Response {



    public MyBody body;


    public MyBody getBody() {
        return body;
    }

    public void setBody(MyBody body) {
        this.body = body;
    }



    public List<UpgradeResult> getUpgradeResult(){
        return body == null?null:body.getResource();
    }









    public static class MyBody extends ResponseBody{
        public List<UpgradeResult> resource;

        public List<UpgradeResult> getResource() {
            return resource;
        }

        public void setResource(List<UpgradeResult> resource) {
            this.resource = resource;
        }
    }


}
