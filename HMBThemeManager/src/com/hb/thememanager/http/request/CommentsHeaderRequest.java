package com.hb.thememanager.http.request;

import android.content.Context;
import android.content.res.AssetManager;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.response.CommentsHeaderResponse;
import com.hb.thememanager.http.response.IResponseHandler;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeResponse;
import com.hb.thememanager.utils.Config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by alexluo on 17-8-3.
 */

public class CommentsHeaderRequest extends ThemeRequest {

    private Context mContext;
    public CommentsHeaderRequest(Context context,int themeType) {
        super(context,themeType);
        mContext = context;
        setUrl(Config.HttpUrl.COMMENTS_SCORE_URL);
    }

//    @Override
//    public void request(Http http, IResponseHandler handler) {
//        AssetManager asset = mContext.getAssets();
//        try{
//            InputStream input = asset.open("test_json/comments_header.json");
//            String code=getEncoding(input);
//            input=asset.open("test_json/comments_header.json");
//            BufferedReader br=new BufferedReader(new InputStreamReader(input,code));
//
//            StringBuilder sb = new StringBuilder();
//            String lineTxt = null;
//            while((lineTxt = br.readLine()) != null){
//                sb.append(lineTxt);
//            }
//            br.close();
//            ((RawResponseHandler)handler).onSuccess(200, sb.toString());
//        }catch(Exception e){
//
//        }
//    }

    @Override
    public void request() {

    }


    @Override
    protected void generateRequestBody() {
        RequestBody body = new RequestBody();
        body.id = getId();
        body.type = getThemeType();
        body.setupAvaliableProperties("id","type");
        setBody(body);
    }

    @Override
    public Response parseResponse(String responseStr) {
        return JSON.parseObject(responseStr, CommentsHeaderResponse.class);
    }
}
