package com.android.launcher3.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.model.WidgetsModel;
import com.android.launcher3.pageindicators.PageIndicatorUnderline;


/**
 * Created by lijun on 17-3-7.
 */

public class WidgetsContainerPagedView extends BaseWidgetsContainerView {

//    ImageView widgetsIndicatorLeft;
//    ImageView widgetsIndicatorRight;

    WidgetsPagedView mPagedView;

    public WidgetsContainerPagedView(Context context) {
        this(context, null);
    }

    public WidgetsContainerPagedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetsContainerPagedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void addWidgets(WidgetsModel model) {
        if (mPagedView != null) {
            mPagedView.addWidgets(model);
        }
    }

    @Override
    public void scrollToTop() {
        if (mPagedView != null) {
            mPagedView.scrollToTop();
        }
    }

    @Override
    public boolean isEmpty() {
        return mPagedView == null || mPagedView.isEmpty();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

//        widgetsIndicatorLeft = (ImageView) getContentView().findViewById(R.id.widgets_container_left_indicator);
//        widgetsIndicatorRight = (ImageView) getContentView().findViewById(R.id.widgets_container_right_indicator);

        mPagedView = (WidgetsPagedView) getContentView().findViewById(R.id.widgets_paged_view_content);
//        mPagedView.setIndicator(widgetsIndicatorLeft,widgetsIndicatorRight);
        mPagedView.initParentViews( getContentView());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        final int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);

        final int count = getChildCount();
        if (widthMode != View.MeasureSpec.EXACTLY || heightMode != View.MeasureSpec.EXACTLY) {
            InvariantDeviceProfile idp = LauncherAppState.getInstance().getInvariantDeviceProfile();
            int childWidthMode;
            int childHeightMode;
            childWidthMode = View.MeasureSpec.EXACTLY;
            childHeightMode = View.MeasureSpec.EXACTLY;
            heightSize = idp.portraitProfile.widgetsContainerBarHeightPx;
            final int childWidthMeasureSpec =
                    View.MeasureSpec.makeMeasureSpec(widthSize, childWidthMode);
            final int childHeightMeasureSpec =
                    View.MeasureSpec.makeMeasureSpec(heightSize, childHeightMode);
            for (int i = 0; i < count; i++) {
                getChildAt(i).measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }else{
            for (int i = 0; i < count; i++) {
                getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
            }
        }
//        mViewport.set(0, 0, widthSize, heightSize);
        // 给每一个子view给予相同的空间

        /** 滚动到目标坐标 */
//        scrollTo(mCurScreen * widthSize, 0);
        this.setMeasuredDimension(widthSize,heightSize);
    }
}
