package cn.com.protruly.soundrecorder.recordlist;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridView;
import android.widget.ListView;

/**
 * Created by wenwenchao on 17-8-28.
 */

public class CustomView {


    public static class MyListView extends ListView{

        public MyListView(Context context) {
            super(context);
        }

        public MyListView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MyListView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        public MyListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }


        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {

            int itemPos = this.pointToPosition((int)ev.getX(),(int)ev.getY());
            View lastItemView = this.getChildAt(getLastVisiblePosition());
            int lastItemBottom = lastItemView==null?-1:lastItemView.getBottom();

            if(itemPos==-1 &&lastItemBottom!=-1 && (int)ev.getY()>lastItemBottom){    //监听listview占满屏后的空白处点击事件
                if(mOnBlankClickListener!=null){
                    mOnBlankClickListener.OnBlankClick();
                }
            }

            if(enableGridScroll){                           //内嵌scroll滑动使能时穿透点击事件
                return false;
            }else{
                return super.onInterceptTouchEvent(ev);
            }
        }

        private static Boolean  enableGridScroll = false;
        public static void setEnableGridScroll(Boolean enableGridScroll) {
            MyListView.enableGridScroll = enableGridScroll;
        }

        public OnBlankClickListener mOnBlankClickListener;
        public interface OnBlankClickListener{
            void OnBlankClick();
        }
        public void setOnBlankClickListener(OnBlankClickListener mOnBlankClickListener) {
            this.mOnBlankClickListener = mOnBlankClickListener;
        }
    }


    public static class TimeGridView extends GridView {
        private static Boolean  setGridHighMax = true;
        public TimeGridView(Context context) {
            super(context);
        }

        public TimeGridView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public TimeGridView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        public TimeGridView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if (setGridHighMax) {
                int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
                super.onMeasure(widthMeasureSpec, expandSpec);
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }

        public static void setSetGridHighMax(Boolean setGridHighMax) {
            TimeGridView.setGridHighMax = setGridHighMax;
        }
    }
}
