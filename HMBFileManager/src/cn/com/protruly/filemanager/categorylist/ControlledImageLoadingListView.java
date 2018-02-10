package cn.com.protruly.filemanager.categorylist;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AbsListView;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import cn.com.protruly.filemanager.imageloader.ImageLoader;
import cn.com.protruly.filemanager.utils.LogUtil;
import hb.widget.HbListView;

/**
 * Created by sqf on 17-7-11.
 */

public class ControlledImageLoadingListView extends HbListView implements AbsListView.OnScrollListener {

    private static final String TAG = "ControlledImageLoadingListView";

    protected ImageLoader mImageLoader;

    public ControlledImageLoadingListView(Context context) {
        super(context);
        initData();
    }

    public ControlledImageLoadingListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initData();
    }

    public ControlledImageLoadingListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData();
    }

    public ControlledImageLoadingListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initData();
    }

    protected void initData() {

        setOnScrollListener(this);

        setScrollBarStyle(SCROLLBARS_INSIDE_INSET); //SCROLLBARS_OUTSIDE_INSET //SCROLLBARS_OUTSIDE_OVERLAY //SCROLLBARS_INSIDE_INSET //SCROLLBARS_INSIDE_OVERLAY
        setScrollbarFadingEnabled(true);
        setFastScrollEnabled(true);
        //setFastScrollAlwaysVisible(true);
    }

    public void setImageLoader(ImageLoader imageLoader) {
        mImageLoader = imageLoader;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        pauseOrResumeImageLoading(scrollState);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }

    private void pauseOrResumeImageLoading(int scrollState) {
        if(mImageLoader == null) return;
        if(scrollState == SCROLL_STATE_IDLE) {
            LogUtil.i(TAG, "ControlledImageLoadingListView::pauseOrResumeImageLoading resume scrollState:" + scrollState);
            mImageLoader.notifyResume();
        } else {
            LogUtil.i(TAG, "ControlledImageLoadingListView::pauseOrResumeImageLoading pause scrollState:" + scrollState);
            mImageLoader.notifyPause();
        }
    }
}
