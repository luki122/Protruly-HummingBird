package com.hb.thememanager.http.request;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.response.DesignerThemeResponse;
import com.hb.thememanager.http.response.IResponseHandler;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeResponse;
import com.hb.thememanager.utils.Config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by alexluo on 17-8-7.
 */

public class DesignerThemeRequest extends ThemeRequest {

    private int mDesignerId;
    private Context mContext;


    public DesignerThemeRequest(Context context,int themeType,int desigerId) {
        super(context,themeType);
        mContext = context;
        mDesignerId = desigerId;
        setUrl(Config.HttpUrl.DISIGNER_THEME_URL);
    }

//    @Override
//    public void request(Http http, IResponseHandler handler) {
//// TODO Auto-generated method stub
//        super.request(http,handler);
//
//        AssetManager asset = mContext.getAssets();
//        try{
//            InputStream input = asset.open("test_json/desinger_theme.json");
//            String code=getEncoding(input);
//            input=asset.open("test_json/desinger_theme.json");
//            BufferedReader br=new BufferedReader(new InputStreamReader(input,code));
//
//            StringBuilder sb = new StringBuilder();
//            char[] c = new char[1024];
//            String s = null;
//            int len;
//
//            while((len = br.read(c)) != -1){
//                sb.append(c,0,len);
//            }
//            br.close();
//            ((RawResponseHandler)handler).onSuccess(200, sb.toString());
//        }catch(IOException e){
//            Log.d("cate", "exception"+e);
//        }
//
//    }
//


    @Override
    protected void generateRequestBody() {
        DisignerThemeBody body = new DisignerThemeBody();
        body.pageSize = getPageSize();
        body.pageNum = getPageNumber();
        body.type = getThemeType();
        body.designer = mDesignerId;
        body.id = getId();
        body.setupAvaliableProperties("id","pageSize","pageNum","type","designer");
        setBody(body);
    }


    public static class DisignerThemeBody extends RequestBody{
        public int designer;

        public int getDesigner() {
            return designer;
        }

        public void setDesigner(int designer) {
            this.designer = designer;
        }
    }
    @Override
    public Response parseResponse(String responseStr) {
        return JSON.parseObject(responseStr, DesignerThemeResponse.class);
    }

}
