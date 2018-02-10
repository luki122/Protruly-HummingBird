package com.hb.thememanager.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.hb.thememanager.views.pulltorefresh.PullToRefreshListView;
import com.hb.thememanager.R;

/**
 * Created by caizhongting on 17-7-12.
 */

public class AutoLoadListView extends PullToRefreshListView {
    private static final String TAG = "AutoLoadListView";

    private boolean mHasMore = true;

    public interface OnAutoLoadListener{
        void onLoading(boolean hasMore);
    }

    private OnAutoLoadListener mAutoLoadingListener;

    private int footerLayoutResid = 0;
    private View footerView;
    private ImageView mLoadingImage;
    private TextView mLoadingText;
    private TextView mLoadingSubText;
    private BaseAdapter mAdapter;
    private ListView mList;
    private boolean hasFooter;

    private boolean isInit = false;
    private int mDataCount = 0;

    public AutoLoadListView(Context context) {
        super(context);
        init(null);
    }

    public AutoLoadListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public AutoLoadListView(Context context, Mode mode) {
        super(context, mode);
        init(null);
    }

    public AutoLoadListView(Context context, Mode mode, AnimationStyle style) {
        super(context, mode, style);
        init(null);
    }

    private void init(AttributeSet attrs){
        hasFooter = true;
        footerLayoutResid = R.layout.pull_to_refresh_footer_vertical;
        if(attrs != null){
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.AutoLoadListView);
            if(a.hasValue(R.styleable.AutoLoadListView_footerLayout)) {
                footerLayoutResid = a.getResourceId(R.styleable.AutoLoadListView_footerLayout, 0);
            }
            a.recycle();
        }
        setupView();
    }

    public void setOnAutoLoadListener(OnAutoLoadListener listener){
        mAutoLoadingListener = listener;
    }

    private View getFooterView(){
        Log.e(TAG,"getFooterView");
        if(footerView == null && footerLayoutResid != 0){
            LayoutInflater inflater = LayoutInflater.from(getContext());
            footerView = inflater.inflate(footerLayoutResid,null);
        }
        return footerView;
    }

    public void hasFooterView(boolean hasFooter){
        View footerView = getFooterView();
        this.hasFooter = hasFooter;
        if(hasFooter) {
            footerView.setVisibility(View.VISIBLE);
        }else{
            footerView.setVisibility(View.GONE);
        }
    }

    private void setupView(){
        Log.e(TAG,"setupView");
        setupFooterView();
        getLoadingLayoutProxy(true,false).setLoadingDrawable(getResources().getDrawable(R.drawable.ic_loading));
    }

    private void setupFooterView(){
        Log.e(TAG,"setupFooterView");
        View footerView = getFooterView();
        mLoadingImage = (ImageView) footerView.findViewById(R.id.loading_image);
        mLoadingText = (TextView) footerView.findViewById(R.id.loading_text);
        mLoadingSubText = (TextView) footerView.findViewById(R.id.loading_sub_text);
        mLoadingImage.setBackgroundResource(R.drawable.loading_data);


        mList.removeFooterView(footerView);
        mList.addFooterView(footerView);
    }

    public void setAdapter(BaseAdapter adapter){
        mAdapter = adapter;
        if(mList != null){
            mList.setAdapter(adapter);
        }
    }

    private void loadingMore(boolean hasMore){
        Log.e(TAG,"loadingMore");
        if(hasFooter) {
            if (hasMore) {
                mLoadingImage.setVisibility(View.GONE);
                mLoadingImage.setBackgroundResource(R.drawable.loading_data);
                mLoadingImage.setVisibility(View.VISIBLE);
                mLoadingText.setText(getResources().getString(R.string.loading_more));
            } else {
                mLoadingImage.setVisibility(View.GONE);
                mLoadingText.setText(getResources().getString(R.string.has_no_more));
            }
            if (mAutoLoadingListener != null) {
                mAutoLoadingListener.onLoading(hasMore);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if(mAdapter != null) {
            int firstVisiblePostion = mList.getFirstVisiblePosition(), lastVisiblePosition = mList.getLastVisiblePosition(),
                    count = mAdapter.getCount();
            if (firstVisiblePostion <= 0 && lastVisiblePosition >= count && mDataCount != count && count > 0) {
                mDataCount = count;
                loadingMore(mHasMore);
            }
        }
    }

    public void onLoadingComplete(boolean hasMore){
        Log.e(TAG,"onLoadingComplete : hasMore = "+hasMore);
        if(hasFooter) {
            mHasMore = hasMore;
            if (hasMore) {
                mLoadingImage.setVisibility(View.VISIBLE);
                mLoadingImage.setBackgroundResource(R.drawable.ic_pullup);
                mLoadingText.setText(getResources().getString(R.string.push_loading_more));
            } else {
                mLoadingImage.setVisibility(View.GONE);
                mLoadingText.setText(getResources().getString(R.string.has_no_more));
            }
        }
    }

    protected ListView createListView(Context context, AttributeSet attrs) {
        Log.e(TAG,"createListView");
        mList = super.createListView(context,attrs);
        mList.setSelector(new ColorDrawable());
        return mList;
    }

    public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount,
                               final int totalItemCount) {
        super.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
    }

    public void onScrollStateChanged(final AbsListView view, final int state) {
        super.onScrollStateChanged(view, state);
//        Log.e(TAG,"onScrollStateChanged : count = "+mAdapter.getCount()+" ; lastVisiblePostion = "+view.getLastVisiblePosition());
        if(state == SCROLL_STATE_IDLE){
            if(view.getLastVisiblePosition() >= mAdapter.getCount()){
                loadingMore(mHasMore);
            }
        }
    }

    public ListAdapter getAdapter(){
        return mAdapter;
    }

    public ListView getListView(){
        return mList;
    }

}
