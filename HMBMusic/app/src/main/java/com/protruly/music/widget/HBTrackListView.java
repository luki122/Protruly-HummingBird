package com.protruly.music.widget;

import android.content.Context;
import android.util.AttributeSet;

import com.protruly.music.online.HBNetTrackDetail;

import hb.widget.HbListView;

/**
 * Created by hujianwei on 17-9-1.
 */

public class HBTrackListView extends HbListView {
    private HBNetTrackDetail mTrackDetail;
    public void setHBNetTrackDetail(HBNetTrackDetail detail){
        mTrackDetail = detail;
    }
    public HBTrackListView(Context context){
        this(context,null);
    }
    public HBTrackListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        // TODO Auto-generated constructor stub
    }
    public HBTrackListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // TODO Auto-generated constructor stub
    }
    @Override
    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX,
                                   int scrollY, int scrollRangeX, int scrollRangeY,
                                   int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        // TODO Auto-generated method stub
        if(mTrackDetail!=null){
            mTrackDetail.startAnimation(scrollY,deltaY);
        }
        return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX,
                scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);
    }
    
}
