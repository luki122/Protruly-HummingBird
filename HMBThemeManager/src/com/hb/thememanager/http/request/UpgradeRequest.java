package com.hb.thememanager.http.request;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.UpgradeResponse;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.utils.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexluo on 17-9-8.
 */

public class UpgradeRequest extends ThemeRequest {

    @JSONField(serialize=false)
    private List<Theme> themes;

    public UpgradeRequest(Context context, int themeType) {
        super(context, themeType);
        setUrl(Config.HttpUrl.THEME_UPGRADE);
    }

    @Override
    public Response parseResponse(String responseStr) {
        return JSON.parseObject(responseStr, UpgradeResponse.class);
    }


    @Override
    protected void generateRequestBody() {
        UpgradeBody body = new UpgradeBody();
        body.setupAvaliableProperties("resource");
        body.resource = getThemes();
        setBody(body);
    }


    @JSONField(serialize=false)
    public void setThemes(List<Theme> themes){
        this.themes = themes;
    }

    @JSONField(serialize=false)
    public List<UpgradeResouces> getThemes(){
        ArrayList<UpgradeResouces> resouces = new ArrayList<>();
        if(themes != null){
            for(Theme theme : themes){
                if(theme.isSystemTheme()){
                    continue;
                }
                UpgradeResouces r = new UpgradeResouces();
                r.setType(theme.type);
                r.setId(theme.id);
                r.setVersionName(theme.version);
                resouces.add(r);
            }
        }

        return resouces;
    }


    public static class UpgradeBody extends RequestBody{


        public List<UpgradeResouces> resource;

        public List<UpgradeResouces> getResource() {
            return resource;
        }

        public void setResource(List<UpgradeResouces> resource) {
            this.resource = resource;
        }
    }

    public static class UpgradeResouces{
        public String id;
        public int type;
        public String versionName;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getVersionName() {
            return versionName;
        }

        public void setVersionName(String versionName) {
            this.versionName = versionName;
        }
    }

}
