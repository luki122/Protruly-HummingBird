package com.hb.thememanager.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.util.Log;

import com.hb.thememanager.R;

/**
 * Created by caizhongting on 17-7-3.
 */

public class AutoLineLayout extends RelativeLayout {
    private static final String TAG = "AutoLineLayout";

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;

    private int mOrientation = HORIZONTAL;
    private int mMaxLine = 0;

    public AutoLineLayout(Context context) {
        super(context);
        init(null,0);
    }

    public AutoLineLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs,0);
    }

    public AutoLineLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs,defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyleAttr){
        if(attrs != null){
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.AutoLineLayout);
            if(a.hasValue(R.styleable.AutoLineLayout_orientation)){
                mOrientation = a.getInt(R.styleable.AutoLineLayout_orientation, HORIZONTAL);
            }
            if(a.hasValue(R.styleable.AutoLineLayout_maxLine)){
                mMaxLine = a.getInt(R.styleable.AutoLineLayout_maxLine,0);
            }
            a.recycle();
        }
    }

    public int getOrientation(){
        return mOrientation;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int orientation = getOrientation();
        int count = getChildCount();
        int width = getMeasuredWidth(),height = getMeasuredHeight();
        int childrenWidth = 0,childrenHeight = 0;
        int contentWidth = 0,contentHeight = 0;
        int paddingLeft = getPaddingLeft(),paddingRight = getPaddingRight(),paddingTop = getPaddingTop(),paddingBottom = getPaddingBottom();
        int line = 0;
        for(int i = 0; i<count; i++){
            View child = getChildAt(i);
            LayoutParams llp = (LayoutParams) child.getLayoutParams();
            if(orientation == HORIZONTAL){
                childrenWidth += child.getMeasuredWidth() + llp.leftMargin + llp.rightMargin;
                if(childrenWidth > width - paddingLeft - paddingRight || i == 0){
                    if(i > 0) {
                        childrenWidth = child.getMeasuredWidth() + llp.leftMargin + llp.rightMargin;
                    }
                    if(mMaxLine > 0){
                        line++;
                        if(line > mMaxLine){
                            break;
                        }else{
                            contentHeight += child.getMeasuredHeight() + llp.topMargin + llp.bottomMargin;
                        }
                    }else{
                        contentHeight += child.getMeasuredHeight() + llp.topMargin + llp.bottomMargin;
                    }
                }
            }else if(orientation == VERTICAL){
                childrenHeight += child.getMeasuredHeight() + llp.topMargin + llp.bottomMargin;
                if(childrenHeight > height - paddingTop - paddingBottom || i == 0){
                    if(i > 0) {
                        childrenHeight = child.getMeasuredHeight() + llp.topMargin + llp.bottomMargin;
                    }
                    if(mMaxLine > 0){
                        line++;
                        if(line > mMaxLine){
                            break;
                        }else{
                            contentWidth += child.getMeasuredWidth() + llp.leftMargin + llp.rightMargin;
                        }
                    }else {
                        contentWidth += child.getMeasuredWidth() + llp.leftMargin + llp.rightMargin;
                    }
                }
            }
        }
        if(contentWidth > 0){
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(contentWidth + paddingLeft + paddingRight, MeasureSpec.EXACTLY);
        }
        if(contentHeight > 0){
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(contentHeight + paddingTop + paddingBottom, MeasureSpec.EXACTLY);
        }
        setMeasuredDimension(widthMeasureSpec,heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l,height = b - t;
        int paddingLeft = getPaddingLeft(),paddingRight = getPaddingRight(),paddingTop = getPaddingTop(),paddingBottom = getPaddingBottom();
        int orientation = getOrientation();
        int count = getChildCount();
        int currentLeft = 0,currentTop = 0,currentWidth = 0,currentHeight = 0,lastChildrenWidth = 0,lastChildrenHeight = 0;
        int line = 0;
        for(int i = 0; i<count; i++){
            View child = getChildAt(i);
            LayoutParams llp = (LayoutParams) child.getLayoutParams();
            if(orientation == HORIZONTAL){
                currentLeft += llp.leftMargin;
                int cwidth = child.getMeasuredWidth();
                int cheight = child.getMeasuredHeight();
                currentWidth += cwidth + llp.leftMargin + llp.rightMargin;
                if(i == 0){
                    currentLeft += paddingLeft;
                    currentTop = lastChildrenHeight + llp.topMargin + paddingTop;
                    lastChildrenHeight += cheight + llp.topMargin + llp.bottomMargin;
                    line++;
                }
                Log.e(TAG,"onLayout "+i+" : (width,height) = ("+(width - paddingLeft - paddingRight)+","+height+") ; currentWidth = "+currentWidth+" ; currentLeft = "+currentLeft+" ; currentTop = "+currentTop);
                if(currentWidth > width - paddingLeft - paddingRight){
                    currentWidth = cwidth + llp.leftMargin + llp.rightMargin;
                    currentLeft = paddingLeft + llp.leftMargin;
                    currentTop = lastChildrenHeight + llp.topMargin;
                    lastChildrenHeight += cheight + llp.topMargin + llp.bottomMargin;
                    line++;
                }
                if(mMaxLine > 0){
                    if(line > mMaxLine){
                        break;
                    }else{
                        child.layout(currentLeft, currentTop, currentLeft + cwidth, currentTop + cheight);
                    }
                }else {
                    child.layout(currentLeft, currentTop, currentLeft + cwidth, currentTop + cheight);
                }
                currentLeft += cwidth + llp.rightMargin;
            }else if(orientation == VERTICAL){
                currentTop += llp.topMargin;
                int cwidth = child.getMeasuredWidth();
                int cheight = child.getMeasuredHeight();
                currentHeight += cheight + llp.topMargin + llp.bottomMargin;
                if(i == 0){
                    currentTop += paddingTop;
                    currentLeft = lastChildrenWidth + llp.leftMargin + paddingLeft;
                    lastChildrenWidth += cwidth + llp.leftMargin + llp.rightMargin;
                    line++;
                }
                if(currentHeight > height - paddingTop - paddingBottom){
                    currentHeight = cheight + llp.topMargin + llp.bottomMargin;;
                    currentTop = paddingTop + llp.topMargin;
                    currentLeft = lastChildrenWidth + llp.leftMargin;
                    lastChildrenWidth += cwidth + llp.leftMargin + llp.rightMargin;
                    line++;
                }
                if(mMaxLine > 0){
                    if(line > mMaxLine){
                        break;
                    }else{
                        child.layout(currentLeft, currentTop, currentLeft + cwidth, currentTop + cheight);
                    }
                }else {
                    child.layout(currentLeft, currentTop, currentLeft + cwidth, currentTop + cheight);
                }
                currentTop += cheight + llp.bottomMargin;
            }
        }

    }
}
