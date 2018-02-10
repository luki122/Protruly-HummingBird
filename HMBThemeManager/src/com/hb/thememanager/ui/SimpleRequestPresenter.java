package com.hb.thememanager.ui;

import android.content.Context;
import android.os.Debug;
import android.text.TextUtils;
import android.util.Log;

import com.hb.thememanager.BasePresenter;
import com.hb.thememanager.exception.MvpViewNotAttachedException;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.request.ThemeRequest;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.job.loader.IRequestTheme;
import com.hb.thememanager.utils.TLog;

/**
 * Created by alexluo on 17-8-26.
 */

public class SimpleRequestPresenter extends BasePresenter<SimpleRequestView> implements IRequestTheme {

    private static final String TAG = "SimpleRequest";
    private Http mHttp;
    private Context mContext;
    public  SimpleRequestPresenter(Context context) {
        // TODO Auto-generated constructor stub
        mContext = context;
        mHttp = Http.getHttp(mContext);
    }

    @Override
    public void onDestory() {
        // TODO Auto-generated method stub
    }


    @Override
    public void requestTheme(final ThemeRequest themeType) {
        // TODO Auto-generated method stub
        themeType.request(mHttp, new RawResponseHandler() {

            @Override
            public void onFailure(int statusCode, String error_msg) {
                // TODO Auto-generated method stub
                TLog.e(TAG,"statusCode:"+statusCode+" error_msg:"+error_msg);
                if(getMvpView() != null) {
                    getMvpView().showRequestFailView(true);
                }
            }

            @Override
            public void onSuccess(int statusCode, String response) {
                // TODO Auto-generated method stub
                TLog.d(TAG,""+response);
                try {
                    checkViewAttached();
                }catch (MvpViewNotAttachedException e){
                    TLog.e(TAG,"MVP view is not attach to this presenter->"+SimpleRequestPresenter.class.getName());
                    return;
                }
                if(!TextUtils.isEmpty(response)){
                    Response responseObj = themeType.parseResponse(response);
                    if(responseObj != null){
                        getMvpView().update(responseObj);
                    }else{
                        getMvpView().showEmptyView(true);
                    }
                }else{
                    getMvpView().showEmptyView(true);
                }
            }});
    }

    @Override
    public void refresh(ThemeRequest themeType) {
        // TODO Auto-generated method stub
    }

    @Override
    public void loadMore(ThemeRequest themeType) {
        // TODO Auto-generated method stub
    }


}
