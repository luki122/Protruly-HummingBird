package com.protruly.music.widget;

import android.widget.GridView;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

/**
 * Created by hujianwei on 17-8-31.
 */

public class HBMusicGridView extends GridView {

    public HBMusicGridView(Context context) {
        this(context, null);

    }

    public HBMusicGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public HBMusicGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    /**
     * 设置不滚动
     */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
                MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);

    }
}
