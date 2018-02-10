package com.android.launcher3.wallpaperpicker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.android.launcher3.R;

public class WallpaperPagedViewContainer extends FrameLayout {
    WallpaperPagedView mPagedView;

    public WallpaperPagedViewContainer(Context context) {
        this(context, null);
    }

    public WallpaperPagedViewContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WallpaperPagedViewContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public WallpaperPagedViewContainer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView();
    }

    private void initView() {
        View view = LayoutInflater.from(getContext()).inflate(
                R.layout.wallpaper_pagedview_container, this, true);
        mPagedView = (WallpaperPagedView) view.findViewById(R.id.wallpaper_pagedview);
        mPagedView.initParentViews(view);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mPagedView.onActivityResult(requestCode, resultCode, data);
    }

    public void refresh(boolean comeIn) {
        mPagedView.refresh(comeIn);
    }

    public void onSaveInstanceState(Bundle outState) {
        mPagedView.onSaveInstanceState(outState);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mPagedView.onRestoreInstanceState(savedInstanceState);
    }
}
