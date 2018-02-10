package com.hb.thememanager.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.hb.thememanager.http.request.ThemeRequest;
import com.hb.thememanager.http.response.Response;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.TLog;
import com.hb.thememanager.utils.ToastUtils;
import com.hb.thememanager.views.AutoLoadListView;
import com.hb.thememanager.views.ThemeListView;
import com.hb.thememanager.views.pulltorefresh.PullToRefreshBase;
import com.hb.thememanager.R;

import hb.app.HbActivity;

/**
 * Created by alexluo on 17-8-28.
 */

public abstract class SimpleRequestActivity extends HbActivity implements SimpleRequestView,
        AutoLoadListView.OnAutoLoadListener
        ,PullToRefreshBase.OnRefreshListener,View.OnClickListener{


    private static final String TAG = "SimpleRequestActivity";
    private static final int MSG_LOAD_COMPLETE = 0;
    private static final int MSG_HIDE_LOADINGVIEW = 1;
    private static final int MSG_LOAD_MORE_COMPLETE = 2;
    private static final int MSG_SHOW_EMPTY_VIEW = 3;
    private static final int MSG_SHOW_REQUEST_FAIL_VIEW = 4;
    protected int mCurrentPage = 0;
    protected int mPageSize = 1;
    protected boolean mHasMore = true;
    protected boolean mFromRefresh = false;
    private boolean mRequestError = false;
    private boolean mFirstTimeEnter = false;
    private View mNoNetworkLargeView;
    private Button mBtnClickSetNetwork;
    private TextView mErrorTextView;
    private View mLoadingView;
    protected ThemeListView mList;
    private BaseAdapter mAdapter;
    private SimpleRequestPresenter mPresenter;
    private NetworkChangeReceiver mNetworkReceiver;
    private boolean mFirstTimeRequest = true;
    private boolean mLoadingViewIsShowing = false;
    private boolean mEmptyResources = false;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_LOAD_COMPLETE:
                    if(mList.getVisibility() == View.VISIBLE) {
                        mList.onRefreshComplete();
                    }
                    break;
                case MSG_LOAD_MORE_COMPLETE:
                    if(mList.getVisibility() == View.VISIBLE) {
                        mList.onLoadingComplete(mHasMore);
                    }
                    break;
                case MSG_HIDE_LOADINGVIEW:
                    showLoadingView(false);
                    break;
                case MSG_SHOW_EMPTY_VIEW:
                    showEmptyViewInner();
                    break;
                case MSG_SHOW_REQUEST_FAIL_VIEW:
                    showRequestFailViewInner();
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPresenter = new SimpleRequestPresenter(this.getApplicationContext());
        mPresenter.attachView(this);
        mNetworkReceiver = new NetworkChangeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(mNetworkReceiver,filter);
        setHbContentView(R.layout.activity_theme_list);
        mList = (ThemeListView) findViewById(android.R.id.list);
        mList.setVisibility(View.GONE);
        mList.onRefreshComplete();
        mNoNetworkLargeView = findViewById(R.id.no_network_view_large);
        mBtnClickSetNetwork = (Button)findViewById(R.id.btn_setup_network) ;
        mErrorTextView = (TextView)findViewById(R.id.error_text);
        mLoadingView = findViewById(R.id.loading_widget);
        mBtnClickSetNetwork.setOnClickListener(this);
        mList.setOnAutoLoadListener(this);
        mList.setOnRefreshListener(this);
        mFirstTimeEnter = true;


    }

    @Override
    public void onNavigationClicked(View view) {
        // TODO Auto-generated method stub
        onBackPressed();
    }

    protected void requestFirstPage(){
        boolean hasNetwork = CommonUtil.hasNetwork(getApplicationContext());
        if(hasNetwork) {
            refresh();
        }else{
            showNetworkErrorView(true);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(mAdapter != null && hasData()){
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mNetworkReceiver);
        mPresenter.detachView();
        mFirstTimeEnter = false;
    }

    @Override
    public void onClick(View view) {
        if(view == mBtnClickSetNetwork){
            if(mRequestError){
                showNetworkErrorOnly(false);
                showRequestFailView(false);
                showLoadingView(true);
                mCurrentPage = 0;
                refresh();
            }else if(mEmptyResources){
                refresh();
            }else {
                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
            }

        }
    }

    private void requestBody(){
        ThemeRequest bodyRequest = createBodyRequest();
        if(bodyRequest != null){
            bodyRequest.setPageNumber(mCurrentPage);
            bodyRequest.setPageSize(mPageSize);
            mPresenter.requestTheme(bodyRequest);
        }
    }

    protected boolean hasData(){
        if(mList.getAdapter() == null){
            return false;
        }
        return mList.getAdapter().getCount() > 0;
    }

    /**
     * 分页加载
     */
    protected void loadMore(){
        if(!mHasMore){
            mList.onLoadingComplete(false);
            return;
        }
        mFromRefresh = false;
        mCurrentPage ++;
        requestBody();
    }


    /**
     * 刷新
     */
    protected void refresh(){
        mCurrentPage = 0;
        ThemeRequest headerRequest = createHeaderRequest();
        requestBody();
        if(headerRequest != null){
            mPresenter.requestTheme(headerRequest);
        }
        if(mFirstTimeRequest) {
            showLoadingView(mFirstTimeRequest && !hasData());
        }
        mFirstTimeRequest = false;
    }

    /**
     * 创建页面头部请求，如果有的话就创建，没有就返回null
     * @return
     */
    protected abstract ThemeRequest createHeaderRequest();

    /**
     * 创建资源列表请求
     * @return
     */
    protected abstract ThemeRequest createBodyRequest();

    /**
     * 设置资源列表Adapter
     * @param adapter
     */
    protected void setAdapter(BaseAdapter adapter){
        mAdapter = adapter;
        mList.setAdapter(adapter);
    }

    @Override
    public void showToast(String msg) {

    }

    /**
     * 显示网络错误或者无网络视图
     */
    @Override
    public void showNetworkErrorView(boolean show){
        mLoadingView.setVisibility(View.GONE);
        if(hasData()){
            showNetworkErrorOnly(false);
        }else{
            showNetworkErrorOnly(show);
        }

    }

    /**
     * 显示正在加载进度条，有些界面不需要显示该内容，例如首页热门推荐中的更多
     * @param showLoadingView
     */
    protected void showLoadingView(boolean showLoadingView){
        mLoadingViewIsShowing = showLoadingView;
        mLoadingView.setVisibility(showLoadingView?View.VISIBLE:View.GONE);
        mList.setVisibility(hasData()?View.VISIBLE:View.GONE);
    }

    private void showNetworkErrorOnly(boolean show){
        mNoNetworkLargeView.setVisibility(show?View.VISIBLE:View.GONE);
        mErrorTextView.setText(R.string.no_network_in_large_view);
        mBtnClickSetNetwork.setText(R.string.click_to_setup_network);
        mList.setVisibility(show?View.GONE:View.VISIBLE);
    }


    @Override
    public void showRequestFailView(boolean show) {
        mRequestError = show;
        mHandler.sendEmptyMessageDelayed(MSG_SHOW_REQUEST_FAIL_VIEW,300);
    }

    private void showRequestFailViewInner(){
        if(hasData()){
            ToastUtils.showShortToast(this,R.string.request_fail);
            mList.onRefreshComplete();
            return;
        }
        TLog.d(TAG,"request fail");
        mNoNetworkLargeView.setVisibility(mRequestError?View.VISIBLE:View.GONE);
        mErrorTextView.setText(R.string.request_fail);
        mBtnClickSetNetwork.setText(R.string.click_to_refresh);
        mLoadingView.setVisibility(View.GONE);
        mList.setVisibility(mRequestError?View.GONE:View.VISIBLE);
    }

    @Override
    public synchronized void update(Response result) {
        if(result.returnBody() != null){
            mHasMore = mCurrentPage < result.returnBody().getTotalNum() -1;
            mHandler.sendEmptyMessage(MSG_LOAD_MORE_COMPLETE);
        }


        if(mFromRefresh){
            mHandler.sendEmptyMessage(MSG_LOAD_COMPLETE);
        }
        if(mLoadingView.getVisibility() == View.VISIBLE) {
            mHandler.sendEmptyMessage(MSG_HIDE_LOADINGVIEW);
        }


    }


    /**
     * 显示空视图
     * @param show
     */
    public void showEmptyView(final boolean show){
        if(hasData()){
            mList.onLoadingComplete(false);
            return;
        }
        mEmptyResources = show;
        mHandler.sendEmptyMessageDelayed(MSG_SHOW_EMPTY_VIEW,20);

    }

    public void showList(){
        if(mList.getVisibility() == View.GONE) {
            mList.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyViewInner() {
        mList.setVisibility(View.GONE);
        mNoNetworkLargeView.setVisibility(View.VISIBLE);
        mErrorTextView.setText(R.string.empty_resource);
        mBtnClickSetNetwork.setText(R.string.click_to_refresh);
        mLoadingView.setVisibility(View.GONE);
    }


    @Override
    public void onLoading(boolean hasMore) {
        if(!mFirstTimeEnter) {
            loadMore();
        }
    }

    @Override
    public void onRefresh(PullToRefreshBase refreshView) {
        mFromRefresh = true;
        refresh();
    }



    private class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(mFirstTimeEnter){
                mFirstTimeEnter = false;
                return;
            }
            if("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())){
                    if(CommonUtil.hasNetwork(context)) {
                        if(!hasData()) {
                            refresh();
                        }
                        showNetworkErrorView(false);
                    }else{
                        showNetworkErrorView(true);
                    }
            }
        }
    }
}
