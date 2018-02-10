package com.android.contacts.common.hb;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

public class HbScrollListView extends ListView {

    public HbScrollListView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }
    public HbScrollListView(Context context, AttributeSet attrs) {  
        super(context, attrs);  
    }  
  
    public HbScrollListView(Context context, AttributeSet attrs, int defStyle) {  
        super(context, attrs, defStyle);  
    }  
  
    @Override  
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {  
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,MeasureSpec.AT_MOST);  
        super.onMeasure(widthMeasureSpec, expandSpec);  
    }  
  
    @Override  
    public boolean dispatchTouchEvent(MotionEvent ev) {  
        if(ev.getAction() == MotionEvent.ACTION_MOVE){     
            return true;   
        }   
        return super.dispatchTouchEvent(ev);  
    }  
}