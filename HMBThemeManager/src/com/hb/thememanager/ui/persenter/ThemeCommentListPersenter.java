package com.hb.thememanager.ui.persenter;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.hb.thememanager.BasePresenter;
import com.hb.thememanager.http.Http;
import com.hb.thememanager.http.request.CommentsHeaderRequest;
import com.hb.thememanager.http.request.ThemeRequest;
import com.hb.thememanager.http.response.CommentsHeaderResponse;
import com.hb.thememanager.http.response.RawResponseHandler;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.ThemeCommentsResponse;
import com.hb.thememanager.job.loader.IRequestTheme;
import com.hb.thememanager.ui.mvpview.CommentsView;
import com.hb.thememanager.utils.TLog;

/**
 * Created by alexluo on 17-8-3.
 */

public class ThemeCommentListPersenter extends BasePresenter<CommentsView> implements IRequestTheme {
    private static final String TAG = "CommentRequest";
    private Http mHttp;
    private Context mContext;
    public ThemeCommentListPersenter(Context context) {
        // TODO Auto-generated constructor stub
        mContext = context;
        mHttp = Http.getHttp(mContext);
    }



    @Override
    public void requestTheme(final ThemeRequest themeType) {
        themeType.request(mHttp, new RawResponseHandler() {

            @Override
            public void onSuccess(int statusCode, String response) {
                TLog.d(TAG,"result->"+response);
                Response result = themeType.parseResponse(response);
                if(result != null){
                    if(result instanceof ThemeCommentsResponse) {
                        getMvpView().updateComments(result);
                    }else{
                        getMvpView().updateCommentsHeader(result);
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, String error_msg) {
                TLog.e(TAG,"statusCode->"+statusCode+" error_msg->"+error_msg);
                getMvpView().showNetworkErrorView(true);
            }
        });
    }

    @Override
    public void refresh(ThemeRequest themeType) {

    }

    @Override
    public void loadMore(ThemeRequest themeType) {

    }

    @Override
    public void onDestory() {
        detachView();
    }
}
