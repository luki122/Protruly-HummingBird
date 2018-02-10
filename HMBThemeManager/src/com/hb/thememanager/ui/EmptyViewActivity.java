package com.hb.thememanager.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.hb.thememanager.R;
import com.hb.thememanager.http.request.ThemeRequest;
import com.hb.thememanager.utils.CommonUtil;
import com.hb.thememanager.utils.ToastUtils;
import com.hb.thememanager.views.AutoLoadListView;
import com.hb.thememanager.views.ThemeListView;
import com.hb.thememanager.views.pulltorefresh.PullToRefreshBase;

/**
 * Created by alexluo on 17-8-28.
 */

public class EmptyViewActivity extends BaseActivity{
    private static final String TAG = "EmptyViewActivity";
    private View mCurrentView;
    private View mErrorView;
    private View mLoadingView;
    private TextView mEmptyTextView;
    private Button mEmptyButton;
    private ViewGroup mParent;
    /**
     * normal state
     */
    public static final int EMPTY_STATE_NONE = 0;
    /**
     * network error
     */
    public static final int EMPTY_STATE_NO_NETWORK = 1;
    /**
     * no data
     */
    public static final int EMPTY_STATE_NO_DATA = 2;

    private int empty_state = EMPTY_STATE_NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setHbContentView(R.layout.empty_panel_layout);
        mParent = (ViewGroup) findViewById(R.id.empty_panel);
        mErrorView = findViewById(R.id.empty_view);
        mLoadingView = findViewById(R.id.loading_view);

        mErrorView.setVisibility(View.GONE);
        mLoadingView.setVisibility(View.GONE);


        Button button = getEmptyButton();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEmptyButtonClick(v, empty_state);
            }
        });
    }

    public void setHbContentView(int resId){
        mCurrentView = LayoutInflater.from(this).inflate(resId, mParent, false);
        mParent.addView(mCurrentView);
    }

    public void setHbContentView(View view){
        mCurrentView = view;
        mParent.addView(view);
    }

    public void setState(int state){
        onStateChanged(empty_state, state);
        empty_state = state;
    }

    protected void onStateChanged(int oldState, int newState){
        if(oldState != newState) {
            switch (newState) {
                case EMPTY_STATE_NONE:
                    showNormalView();
                    break;
                case EMPTY_STATE_NO_NETWORK:
                    showNoNetworkView();
                    break;
                case EMPTY_STATE_NO_DATA:
                    showNoDataView();
                    break;
            }
        }
    }

    private void showNormalView(){
        mErrorView.setVisibility(View.GONE);
        mLoadingView.setVisibility(View.GONE);
        mCurrentView.setVisibility(View.VISIBLE);
    }

    private void showErrorView(){
        mErrorView.setVisibility(View.VISIBLE);
        mLoadingView.setVisibility(View.GONE);
        mCurrentView.setVisibility(View.GONE);
    }

    public void showLoadingView(boolean show){
        if(show){
            mLoadingView.setVisibility(View.VISIBLE);
            mErrorView.setVisibility(View.GONE);
            mCurrentView.setVisibility(View.GONE);
        }else{
            switch (empty_state) {
                case EMPTY_STATE_NONE:
                    showNormalView();
                    break;
                case EMPTY_STATE_NO_NETWORK:
                    showNoNetworkView();
                    break;
                case EMPTY_STATE_NO_DATA:
                    showNoDataView();
                    break;
            }
        }
    }

    private void showNoNetworkView(){
        showErrorView();
        TextView textView = getEmptyTextView();
        textView.setText(R.string.msg_no_network);
    }

    private void showNoDataView(){
        showErrorView();
        TextView textView = getEmptyTextView();
        textView.setText(R.string.empty_text);
    }

    public TextView getEmptyTextView(){
        if(mEmptyTextView == null){
            mEmptyTextView = (TextView) mErrorView.findViewById(R.id.error_text);
        }
        return mEmptyTextView;
    }

    public Button getEmptyButton(){
        if(mEmptyButton == null){
            mEmptyButton = (Button) mErrorView.findViewById(R.id.btn_setup_network);
        }
        return mEmptyButton;
    }

    protected void onEmptyButtonClick(View v, int state){

    }
}
