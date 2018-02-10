package cn.com.protruly.filemanager.categorylist;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import cn.com.protruly.filemanager.imageloader.ImageLoader;
import cn.com.protruly.filemanager.utils.LogUtil;
import hb.widget.HbListView;
import cn.com.protruly.filemanager.R;

/**
 * Created by sqf on 17-5-13.
 */

public class CategoryListView extends ControlledImageLoadingListView implements AbsListView.OnScrollListener {

    private static final String TAG = "CategoryListView";

    private int mHeaderViewHeight;

    private float mLastY;
//    private int mCurrentScrollDirection;
//    private static final int SCROLL_DIRECTION_UNKNOWN = 1;
//    private static final int SCROLL_DIRECTION_UP = 1;
//    private static final int SCROLL_DIRECTION_DOWN = 2;
    private int mScrollState;

    private int mCurrentScrollPosition;
    private int mPrevScrollPosition;

    private boolean mIsFlinging;


    public interface SearchViewActionNotifier {
        void doSearchViewShowAnimation();
        void doSearchViewHideAnimation();
        void moveSearchViewTop(int moveTop);
    }

    private SearchViewActionNotifier mSearchViewActionNotifier;

    public void setSearchViewActionNotifier(SearchViewActionNotifier notifier) {
        this.mSearchViewActionNotifier = notifier;
    }

    public CategoryListView(Context context) {
        super(context);
        init();
    }

    public CategoryListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CategoryListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CategoryListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private int getScrollPosition(int firstVisibleItem) {
        int scrollPosition = 0;
        View v = getChildAt(0/*firstVisibleItem*/);
        if(v == null) {
            //LogUtil.i(TAG, "getScrollPosition view is null . return 0");
            return 0;
        }
        if(0 == firstVisibleItem) {
            //LogUtil.i(TAG, "getScrollPosition firstVisibleItem is 0 . return 0");
            scrollPosition = v == null ? 0 : (-1 * v.getTop());
        } else {
            int heightBeforeCurrentVisibleItem = getHeightBeforeCurrentVisibleItem(firstVisibleItem);
            int currentVisibleItemOffset = heightBeforeCurrentVisibleItem - v.getTop();
            scrollPosition = heightBeforeCurrentVisibleItem + currentVisibleItemOffset;
        }
        /*
        LogUtil.i(TAG, "getScrollPosition--> v.getTop: " + v.getTop() + " firstVisibleItem:" + firstVisibleItem +
                " v.getHeight():" + v.getHeight() +
                " getY():" + v.getY() +
                " scrollPosition:" + scrollPosition); */
        return scrollPosition;
    }

    private void init() {
        Context context = getContext();
        Resources res = context.getResources();
        mHeaderViewHeight = res.getDimensionPixelSize(R.dimen.category_search_layout_height);
    }

    public int getHeaderViewHeight() {
        return mHeaderViewHeight;
    }

    public void addHeaderView() {
        View headView = new View(getContext());
        headView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mHeaderViewHeight));
        headView.setClickable(false);
        setHeaderDividersEnabled(false);
        addHeaderView(headView ,null,false);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        super.onScrollStateChanged(view, scrollState);
        mScrollState = scrollState;
        if(mScrollState == SCROLL_STATE_IDLE) {
            mIsFlinging = false;
            //LogUtil.i(TAG, "onScrollStateChanged --> : scrollState: SCROLL_STATE_IDLE 111 " + SCROLL_STATE_IDLE);
            if(mCurrentScrollPosition < mHeaderViewHeight) {
                /*
                LogUtil.i(TAG, "onScrollStateChanged --> : scrollState: SCROLL_STATE_IDLE 222 SHOWWWWWWWWW mCurrentScrollPosition:" + mCurrentScrollPosition +
                        " mHeaderViewHeight:" + mHeaderViewHeight); */
                mSearchViewActionNotifier.doSearchViewShowAnimation();
            }
        } else if(mScrollState == SCROLL_STATE_TOUCH_SCROLL) {
            //LogUtil.i(TAG, "onScrollStateChanged --> : scrollState: SCROLL_STATE_TOUCH_SCROLL  " + SCROLL_STATE_TOUCH_SCROLL);
            mIsFlinging = false;
            doSearchViewAnimation();
        } else if(mScrollState == SCROLL_STATE_FLING) {
            //LogUtil.i(TAG, "onScrollStateChanged --> : scrollState: SCROLL_STATE_FLING  " + SCROLL_STATE_FLING);
            mIsFlinging = true;
            doSearchViewAnimation();
        }

        //LogUtil.i(TAG, "onScrollStateChanged --> : scrollState:" + scrollState );
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        super.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        mCurrentScrollPosition = getScrollPosition(firstVisibleItem);
        if(mCurrentScrollPosition < mHeaderViewHeight && firstVisibleItem == 0) {
            //LogUtil.i(TAG, "onScroll 111: mCurrentScrollPosition:" + mCurrentScrollPosition + " mHeaderViewHeight:" + mHeaderViewHeight);
            mSearchViewActionNotifier.moveSearchViewTop(mCurrentScrollPosition);
        } else if(mCurrentScrollPosition >= mHeaderViewHeight && ! mIsFlinging) {
            doSearchViewAnimation();
            //LogUtil.i(TAG, "onScroll 222: mCurrentScrollPosition:" + mCurrentScrollPosition + " mHeaderViewHeight:" + mHeaderViewHeight);
        } else {
            //LogUtil.i(TAG, "onScroll 333: mCurrentScrollPosition:" + mCurrentScrollPosition + " mHeaderViewHeight:" + mHeaderViewHeight);
        }
        mPrevScrollPosition = mCurrentScrollPosition;
    }

    private void doSearchViewAnimation() {
        mCurrentScrollPosition = getScrollPosition(getFirstVisiblePosition());
        if(mPrevScrollPosition == mCurrentScrollPosition) {
            return;
        }
        if(mPrevScrollPosition < mCurrentScrollPosition) {
            // finger move upward
            mSearchViewActionNotifier.doSearchViewHideAnimation();
            //LogUtil.i(TAG, "doSearchViewAnimation 111 HIDE: mCurrentScrollPosition:" + mCurrentScrollPosition + " mHeaderViewHeight:" + mHeaderViewHeight);
        } else if(mPrevScrollPosition > mCurrentScrollPosition) {
            // finger move downward
            mSearchViewActionNotifier.doSearchViewShowAnimation();
            //LogUtil.i(TAG, "doSearchViewAnimation 222 SHOW: mCurrentScrollPosition:" + mCurrentScrollPosition + " mHeaderViewHeight:" + mHeaderViewHeight);
        } else {
            //LogUtil.i(TAG, "doSearchViewAnimation 333 WHAT to Do?: mCurrentScrollPosition:" + mCurrentScrollPosition + " mHeaderViewHeight:" + mHeaderViewHeight);
        }
    }

    private int getHeightBeforeCurrentVisibleItem(int currentVisibleItem) {
        if(currentVisibleItem == 0) {
            return 0;
        }
        View currentVisibleView = getChildAt(0/*currentVisibleItem*/);
        return mHeaderViewHeight + (currentVisibleItem - 1) * currentVisibleView.getHeight();
    }
}
