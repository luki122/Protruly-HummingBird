package com.hb.thememanager.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import hb.app.HbActivity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.hb.thememanager.R;
import com.hb.thememanager.http.downloader.DownloadInfo;
import com.hb.thememanager.http.downloader.DownloadManager;
import com.hb.thememanager.http.downloader.DownloadService;
import com.hb.thememanager.http.downloader.callback.DownloadListener;
import com.hb.thememanager.http.downloader.exception.DownloadException;
import com.hb.thememanager.http.request.DesignerThemeRequest;
import com.hb.thememanager.http.request.RequestBody;
import com.hb.thememanager.http.request.ThemeRequest;
import com.hb.thememanager.http.response.DesignerThemeResponse;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.http.response.SimpleThemeResponse;
import com.hb.thememanager.model.Advertising;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.ui.adapter.ThemeListAdapter;
import com.hb.thememanager.utils.Config;
import com.hb.thememanager.utils.TLog;
import com.hb.thememanager.views.AutoLoadListView;
import com.hb.thememanager.views.ThemeListView;
import com.hb.thememanager.views.pulltorefresh.PullToRefreshBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexluo on 17-7-27.
 */

public class LoadMoreActivity extends SimpleRequestActivity {

    private static final int PAGE_SIZE = 15;
    protected static final int COMPLETE_REQUEST = 0;
    private ArrayList<Theme> mCurrentThemes;
    private int mThemeType;
    private ThemeListAdapter mAdapter;
    private int mRecommendId;
    private MoreRequest mRequest;
    private DesignerThemeRequest mDesignerRequest;
    private Advertising mAdv;
    private int mDesignerId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mThemeType = intent.getIntExtra(Config.ActionKey.KEY_LOAD_MORE_TYPE,0);
        mCurrentThemes = intent.getParcelableArrayListExtra(Config.ActionKey.KEY_LOAD_MORE_LIST);
        mRecommendId = intent.getIntExtra(Config.ActionKey.KEY_LOAD_MORE_ID,-1);
        mDesignerId = intent.getIntExtra(Config.ActionKey.KEY_LOAD_MORE_DESIGNER_ID,-1);
        if(mDesignerId != -1){
            mDesignerRequest = new DesignerThemeRequest(this.getApplicationContext(),mThemeType,mDesignerId);
        }
        mAdv = intent.getParcelableExtra(Config.ActionKey.KEY_ADV_DETAIL);
        String title = intent.getStringExtra(Config.ActionKey.KEY_LOAD_MORE_NAME);
        if(mAdv != null){
            mThemeType = mAdv.getWaresType();
            title = mAdv.getName();
            try {
                mRecommendId = Integer.parseInt(mAdv.getParameter());
            }catch (Exception e){
                mRecommendId = -1;
            }
        }
        if(!TextUtils.isEmpty(title)){
            setTitle(title);
        }

        mRequest = new MoreRequest(this,mThemeType);
        mRequest.setId(String.valueOf(mRecommendId));
        mPageSize = PAGE_SIZE;
        mAdapter = new ThemeListAdapter(this);
        mAdapter.setType(mThemeType);
        mAdapter.setJumpWallpaperDetailData(ThemeListAdapter.URL_TYPE_LOAD_MORE_ACTIVITY, mRecommendId, ThemeListAdapter.NULL, "");
        setAdapter(mAdapter);
        addOriginalItem();

        refresh();
    }

    private void addOriginalItem(){
        mAdapter.removeAll();
    }


    @Override
    public void onNavigationClicked(View view) {
        onBackPressed();
    }


    @Override
    protected ThemeRequest createHeaderRequest() {
        return null;
    }

    @Override
    protected ThemeRequest createBodyRequest() {
        if(mDesignerId != -1){
            return mDesignerRequest;
        }

        return mRequest;
    }

    @Override
    public void update(Response result) {
        super.update(result);
        List<Theme> themeList = null;
        if(result instanceof SimpleThemeResponse){
            SimpleThemeResponse stp = (SimpleThemeResponse)result;
            themeList = stp.body.getThemes(mThemeType);
        }else if(result instanceof DesignerThemeResponse){

            themeList = ((DesignerThemeResponse) result).getThemes(mThemeType);
        }

        if(themeList != null && themeList.size() >0){
            if(mFromRefresh){
                addOriginalItem();
            }
            showList();

            mAdapter.addData(themeList);
        }else{
            if(mCurrentPage == 0){
                showEmptyView(true);
            }
        }

    }





    public static class MoreRequest extends ThemeRequest{

        public MoreRequest(Context context, int themeType) {
            super(context, themeType);
            setUrl(Config.HttpUrl.getHotRecommendUrl(themeType));
        }

        @Override
        protected void generateRequestBody() {
            RequestBody body = new RequestBody();
            body.setId(getId());
            body.setPageNum(getPageNumber());
            body.setPageSize(getPageSize());
            body.setupAvaliableProperties("id","pageNum","pageSize");
            setBody(body);

        }

        @Override
        public Response parseResponse(String responseStr) {
            return JSON.parseObject(responseStr,SimpleThemeResponse.class);
        }
    }


}
