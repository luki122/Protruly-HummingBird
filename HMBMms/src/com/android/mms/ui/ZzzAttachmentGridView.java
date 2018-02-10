package com.android.mms.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;
/**
 * hummingbird add by tangyisen for  hb style 2017.3.28
 */
//tangyisen
public class ZzzAttachmentGridView extends GridView
{

    public ZzzAttachmentGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ZzzAttachmentGridView(Context context) {
        super(context);
    }

    public ZzzAttachmentGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {       
          
        /*
         * int expandSpec = MeasureSpec.makeMeasureSpec( Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
         * super.onMeasure(widthMeasureSpec, expandSpec);
         */
        int heightSpec;
        if (getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
            heightSpec = MeasureSpec.makeMeasureSpec(
                    Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        }
        else {
            heightSpec = heightMeasureSpec;
        }
        super.onMeasure(widthMeasureSpec, heightSpec);
    }
}
