package com.android.calendar.hb;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract.Attendees;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.android.calendar.CalendarController;
import com.android.calendar.CalendarController.EventType;
import com.android.calendar.CalendarController.ViewType;
import com.android.calendar.Event;
import com.android.calendar.EventLoader;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.hb.utils.LunarUtils;
import com.android.calendar.month.MonthByWeekAdapter;
import com.android.calendar.month.MonthByWeekFragment;
import com.android.calendar.month.MonthWeekEventsView;
import com.android.calendar.hb.utils.CalendarUtils;

import java.util.ArrayList;

public class MonthAgendaView extends LinearLayout {

    private static final String TAG = "MonthAgendaView";
    private static final String DEBUG_TAG = "MonthDay";

    private static int sViewId = 0;
    private int mViewId = -1;

    private static final int DEFAULT_WEEK_COUNT = 6;
    private static final int DAYS_PER_WEEK = 7;

    private Context mContext;
    private ViewSwitcher mViewSwitcher = null;
    private MonthByWeekFragment mParentFragment = null;

    private BaseAdapter mAdapter = null;
    private MonthWeekEventsView[] mWeekViews = null;

    private Time mBaseDate = new Time();
    private int mFirstMonthJulianDay = -1;
    private int mLastMonthJulianDay = -1;

    private boolean mHasToday = false;
    private int mTodayJulianDay = -1;
    private boolean mIsTodaySecondary = false;

    private Time mSelectedDate = new Time();

    private int mHighlightWeekIndex = -1;
    private int mHighlightWeekDayIndex = -1;
    private int mActualWeekCount = DEFAULT_WEEK_COUNT;

    private int mPrevHighlightWeekIndex = -2;
    private int mPrevHighlightWeekDayIndex = -2;

    private void resetPrevHighlightIndex() {
        mPrevHighlightWeekIndex = -2;
        mPrevHighlightWeekDayIndex = -2;
    }

    private void resetHighlightIndex() {
        mHighlightWeekIndex = -1;
        mHighlightWeekDayIndex = -1;
    }

    private int mToolbarAndMonthHeaderHeight;

    private float mScale = 1.0f;

    private int mViewPadding = 0;
    private int mViewWidth = 0;
    private int mViewHeight = 0;

    private int mWeekViewWidth = 0;
    private int mWeekViewHeight = 0;
    private int mWeekViewLeft = 0;
    private int mWeekViewTop = 0;

    private static final int TOUCH_MODE_UNKNOWN = 0;
    private static final int TOUCH_MODE_DOWN = 0x01;
    private static final int TOUCH_MODE_HSCROLL = 0x02;
    private static final int TOUCH_MODE_VSCROLL = 0x04;

    private static final float SCROLL_SENSITIVITY = 1.7f;

    private static final float MIN_VSCROLL_DISTANCE = 37.0f/*60.0f*/;

    private static final long DURATION_MONTH_SWITCHING = 450;
    private static final Interpolator DEFAULT_INTERPOLATER = new AccelerateDecelerateInterpolator();

    private int mTouchMode = TOUCH_MODE_UNKNOWN;

    private GestureDetector mGestureDetector = null;

    private boolean mIsNewScrolling = false;
    private float mSumScrollX = 0;
    private float mSumScrollY = 0;

    private EventLoader mEventLoader = null;
    private ArrayList<Event> mEvents = new ArrayList<>();
    private ArrayList<Event> mAdapterEvents = new ArrayList<>();

    private MonthAgendaAdapter mMonthAgendaAdapter = null;

    private static final int MESSAGE_EVENTS_LOADED = 1;

    private Handler mEventLoaderHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_EVENTS_LOADED:
                    updateEventsListView();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private void setHourAndMinute(Time time) {
        Time now = new Time();
        now.setToNow();

        time.hour = now.hour;
        time.minute = now.minute;
        time.second = 0;
    }

    private void updateEventsListView() {
        Log.d(TAG, "updateEventsListView()");

        if (mAgendaList == null) return;

        mAdapterEvents.clear();
        mAdapterEvents.addAll(mEvents);

        Log.d(TAG, "mEvents size = " + mEvents.size());

        if (mMonthAgendaAdapter == null) {
            mMonthAgendaAdapter = new MonthAgendaAdapter(mContext, mAdapterEvents);
        }

        if (mAgendaList.getAdapter() == mMonthAgendaAdapter) {
            mMonthAgendaAdapter.notifyDataSetChanged();
        } else {
            mAgendaList.setAdapter(mMonthAgendaAdapter);
        }
    }

    private Runnable mEventsLoadingFinishedCallback = new Runnable() {
        @Override
        public void run() {
            mEventLoaderHandler.sendEmptyMessage(MESSAGE_EVENTS_LOADED);
        }
    };

    private Runnable mEventsLoadingCanceledCallback = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "events query canceled");
        }
    };

    private static final long DEFAULT_EVENT_ID = -1;

    private void updateActionBarTime(Time targetDate) {
        if (!CalendarUtils.isYearInRange(targetDate)) {
            return;
        }

        Log.d(TAG, "2 - update time " + CalendarUtils.printDate(targetDate));

        CalendarController controller = CalendarController.getInstance(mContext);
        controller.sendEvent(mContext, EventType.UPDATE_TITLE, null, null, targetDate,
                DEFAULT_EVENT_ID, ViewType.CURRENT, 0, null, null);
    }

    private Time getTimeOfController() {
        CalendarController controller = CalendarController.getInstance(mContext);

        Time time = new Time();
        time.set(controller.getTime());
        time.normalize(true);

        return time;
    }

    private void setTimeOfController(long millis) {
        CalendarController controller = CalendarController.getInstance(mContext);
        controller.setTime(millis);
    }

    public MonthAgendaView(Context context) {
        super(context);
        initMonthView(context);
    }

    public MonthAgendaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initMonthView(context);
    }

    public MonthAgendaView(Context context, AttributeSet attr, int defStyle) {
        super(context, attr, defStyle);
        initMonthView(context);
    }

    private class MonthGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            mTouchMode = TOUCH_MODE_DOWN;
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            selectMonthDay(event.getX(0), event.getY(0));
            return true;
        }

        @Override
        public void onShowPress(MotionEvent event) {
            selectMonthDay(event.getX(0), event.getY(0));
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float deltaX, float deltaY) {
            if (mIsNewScrolling) {
                mSumScrollX = 0;
                mSumScrollY = 0;
                mIsNewScrolling = false;
            }
            mSumScrollX += deltaX;
            mSumScrollY += deltaY;

            if (mTouchMode == TOUCH_MODE_DOWN) {
                float absSumScrollX = Math.abs(mSumScrollX);
                float absSumScrollY = Math.abs(mSumScrollY);

                if (absSumScrollX * SCROLL_SENSITIVITY > absSumScrollY) {
                    if (absSumScrollX > MIN_VSCROLL_DISTANCE * mScale) {
                        if (e1.getY() < mActualWeekCount * mWeekViewHeight) {
                            mTouchMode = TOUCH_MODE_HSCROLL;
                        }
                    }
                } else if (absSumScrollY > MIN_VSCROLL_DISTANCE * mScale) {
                    mTouchMode = TOUCH_MODE_VSCROLL;
                }
            } else if (mTouchMode == TOUCH_MODE_HSCROLL) {
                handleHorizontalScroll();
            } else if (mTouchMode == TOUCH_MODE_VSCROLL) {
                // handleVerticalScroll();
            }

            return true;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_CANCEL:
                return false;
            case MotionEvent.ACTION_DOWN:
                mIsNewScrolling = true;
                mGestureDetector.onTouchEvent(event);
                return false;
            /*case MotionEvent.ACTION_MOVE:
                mGestureDetector.onTouchEvent(event);
				return false;*/
            case MotionEvent.ACTION_UP:
                mIsNewScrolling = false;
                mGestureDetector.onTouchEvent(event);
                return false;
            default:
                mGestureDetector.onTouchEvent(event);
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_CANCEL:
                return true;
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                mGestureDetector.onTouchEvent(event);
                return true;
            case MotionEvent.ACTION_UP:
                return true;
            default:
                mGestureDetector.onTouchEvent(event);
                return true;
        }
    }

    private void setInOutAnimation(boolean gotoFuture, Interpolator interpolator, long duration) {
        float inFromXValue, inToXValue;
        float outFromXValue, outToXValue;
        float progress = 0;

        if (gotoFuture) {
            inFromXValue = 1.0f - progress;
            inToXValue = 0.0f;
            outFromXValue = -progress;
            outToXValue = -1.0f;
        } else {
            inFromXValue = progress - 1.0f;
            inToXValue = 0.0f;
            outFromXValue = progress;
            outToXValue = 1.0f;
        }

        TranslateAnimation inAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, inFromXValue,
                Animation.RELATIVE_TO_SELF, inToXValue,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);

        TranslateAnimation outAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, outFromXValue,
                Animation.RELATIVE_TO_SELF, outToXValue,
                Animation.ABSOLUTE, 0.0f,
                Animation.ABSOLUTE, 0.0f);

        if (duration < 0) {
            duration = 0;
        }
        inAnimation.setDuration(duration);
        outAnimation.setDuration(duration);

        if (interpolator != null) {
            inAnimation.setInterpolator(interpolator);
            outAnimation.setInterpolator(interpolator);
        }

        inAnimation.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                MonthAgendaView currentView = (MonthAgendaView) mViewSwitcher.getCurrentView();

                Log.d(DEBUG_TAG, "onAnimationEnd() current view date is " +
                        CalendarUtils.printDate(currentView.getSelectedDate()));
                mParentFragment.onMonthViewSwitched(currentView.getSelectedDate());
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationStart(Animation animation) {

            }
        });

        mViewSwitcher.setInAnimation(inAnimation);
        mViewSwitcher.setOutAnimation(outAnimation);
    }

    private void handleHorizontalScroll() {
        mIsNewScrolling = false;
        mTouchMode = TOUCH_MODE_UNKNOWN;

        Time temp = getTimeOfController();

        boolean gotoNextMonth = (mSumScrollX > 0);
        setInOutAnimation(gotoNextMonth, DEFAULT_INTERPOLATER, DURATION_MONTH_SWITCHING);

        if (gotoNextMonth) {
            temp.month += 1;
        } else {
            temp.month -= 1;
        }
        temp.monthDay = 1;
        temp.normalize(true);

        Log.d(DEBUG_TAG, "handleHorizontalScroll() goto date " + CalendarUtils.printDate(temp));

        if (!CalendarUtils.isYearInRange(temp)) {
            return;
        }

        MonthAgendaView nextView = (MonthAgendaView) mViewSwitcher.getNextView();
        nextView.setParams(mViewSwitcher, mAdapter, mParentFragment, temp, mEventLoader);
        nextView.setRootViewSize(mRootViewWidth, mRootViewHeight);
        mViewSwitcher.showNext();
        nextView.highlightToday();
    }

    public MonthWeekEventsView getFirstWeekView() {
        return mWeekViews == null ? null : mWeekViews[0];
    }

    private boolean switchMonth(Time targetDate) {
        int action = CalendarUtils.compareMonth(targetDate, mSelectedDate);

        boolean donotMove = (action == 0);
        if (donotMove) {
            if (!CalendarUtils.isYearInRange(targetDate)) {
                return false;
            }

            MonthAgendaView currView = (MonthAgendaView) mViewSwitcher.getCurrentView();
            currView.highlightMonthDate(targetDate);
        } else {
            if (!CalendarUtils.isYearInRange(targetDate)) {
                return false;
            }

            boolean gotoFuture = (action > 0);
            setInOutAnimation(gotoFuture, DEFAULT_INTERPOLATER, DURATION_MONTH_SWITCHING);

            MonthAgendaView nextView = (MonthAgendaView) mViewSwitcher.getNextView();
            nextView.setParams(mViewSwitcher, mAdapter, mParentFragment, targetDate, mEventLoader);
            nextView.setRootViewSize(mRootViewWidth, mRootViewHeight);
            mViewSwitcher.showNext();
            nextView.highlightMonthDate(targetDate);
        }

        return !donotMove;
    }

    private void initMonthView(Context context) {
        mViewId = (++sViewId) & 0x1;

        mContext = context;
        mWeekViews = new MonthWeekEventsView[DEFAULT_WEEK_COUNT];

        mSelectedDate = new Time();

        Resources res = context.getResources();
        mScale = res.getDisplayMetrics().density;

        mToolbarAndMonthHeaderHeight = res.getDimensionPixelSize(R.dimen.toolbar_height)
                + res.getDimensionPixelSize(R.dimen.month_header_height);
    }

    public void setMonthWeekAdapter(BaseAdapter adapter) {
        mAdapter = adapter;

        for (int i = 0; i < mActualWeekCount; ++i) {
            ((MonthByWeekAdapter) mAdapter).sendEventsToView(mWeekViews[i]);
            mWeekViews[i].invalidate();
        }
    }

    public void setParams(ViewSwitcher switcher, BaseAdapter adapter, MonthByWeekFragment fragment,
                          Time selectedDay, EventLoader eventLoader) {

        mViewSwitcher = switcher;
        mAdapter = adapter;
        mParentFragment = fragment;
        mEventLoader = eventLoader;

        if (mGestureDetector == null) {
            mGestureDetector = new GestureDetector(mContext, new MonthGestureDetector());
        }

        setBaseDate(selectedDay);
        mSelectedDate.set(selectedDay);

        Log.d(DEBUG_TAG, "update mSelectedDate as " + CalendarUtils.printTime(mSelectedDate));

        initAgendaLayout();

        int weekNum = getWeekNumOfFirstMonthDay(mSelectedDate);
        addWeekViewsByWeekNum(weekNum);

        setSecondaryDayIndex();
        setAgendaListListener(eventLoader);

        resetHighlightIndex();
        resetPrevHighlightIndex();
    }

    private ViewGroup mAgendaField;
    private hb.widget.HbListView mAgendaList;

    private void initAgendaLayout() {
        if (mAgendaField == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);

            mAgendaField = (ViewGroup) layoutInflater.inflate(R.layout.hb_month_agenda_layout, null);
            mAgendaList = (hb.widget.HbListView) mAgendaField.findViewById(R.id.month_agenda_list);
            // mAgendaList.setDivider(null);
        }
    }

    private void setBaseDate(Time date) {
        if (date == null) return;

        mBaseDate.set(date);
        mBaseDate.normalize(true);

        date.monthDay = 1;
        long millis = date.normalize(true);
        mFirstMonthJulianDay = Time.getJulianDay(millis, date.gmtoff);

        date.month += 1;
        date.monthDay = 1;
        date.monthDay -= 1;
        millis = date.normalize(true);
        mLastMonthJulianDay = Time.getJulianDay(millis, date.gmtoff);

        date.set(mBaseDate);
        date.normalize(true);
    }

    private void setSecondaryDayIndex() {
        Log.d(TAG, "setSecondaryDayIndex() has been invoked");

        int lastSecondaryDayInFirstWeek = -1;
        int firstSecondaryDayInLastWeek = -1;
        if (mActualWeekCount > 0 && mFirstMonthJulianDay > 0 && mLastMonthJulianDay > 0) {
            int firstIndex = mWeekViews[0].getWeekDayIndexByJulianDay(mFirstMonthJulianDay);
            if (firstIndex > 0) {
                lastSecondaryDayInFirstWeek = (firstIndex - 1);
                mWeekViews[0].setSecondaryIndex(lastSecondaryDayInFirstWeek, false);
                if (mWeekViews[0].hasToday()) {
                    mIsTodaySecondary = mWeekViews[0].isTodaySecondary();
                }
            } else {
                lastSecondaryDayInFirstWeek = -1;
            }

			/*int lastIndex = mWeekViews[mActualWeekCount - 1].getWeekDayIndexByJulianDay(mLastMonthJulianDay);
            if (lastIndex < (DAYS_PER_WEEK - 1)) {
				firstSecondaryDayInLastWeek = (lastIndex + 1);
				mWeekViews[mActualWeekCount - 1].setSecondaryIndex(firstSecondaryDayInLastWeek, true);
				if (mWeekViews[mActualWeekCount - 1].hasToday()) {
					mIsTodaySecondary = mWeekViews[mActualWeekCount - 1].isTodaySecondary();
				}
			} else {
				firstSecondaryDayInLastWeek = -1;
			}*/
            int index = mWeekViews[4].getWeekDayIndexByJulianDay(mLastMonthJulianDay);
            if (index >= 0 && index < (DAYS_PER_WEEK - 1)) {
                firstSecondaryDayInLastWeek = index + 1;
                mWeekViews[4].setSecondaryIndex(firstSecondaryDayInLastWeek, true);
                if (mWeekViews[4].hasToday()) {
                    mIsTodaySecondary = mWeekViews[4].isTodaySecondary();
                }
            } else {
                firstSecondaryDayInLastWeek = 0;
                if (!mWeekViews[4].hasFocusMonthDay()) {
                    mWeekViews[4].setSecondaryIndex(firstSecondaryDayInLastWeek, true);
                    if (mWeekViews[4].hasToday()) {
                        mIsTodaySecondary = true;
                    }
                }
            }

            int lastIndex = mWeekViews[5].getWeekDayIndexByJulianDay(mLastMonthJulianDay);
            if (lastIndex >= 0 && lastIndex < (DAYS_PER_WEEK - 1)) {
                firstSecondaryDayInLastWeek = lastIndex + 1;
                mWeekViews[5].setSecondaryIndex(firstSecondaryDayInLastWeek, true);
                if (mWeekViews[5].hasToday()) {
                    mIsTodaySecondary = mWeekViews[5].isTodaySecondary();
                }
            } else {
                firstSecondaryDayInLastWeek = mWeekViews[5].getOffsetByJulianDay(mLastMonthJulianDay) + 1;
                mWeekViews[5].setSecondaryIndex(firstSecondaryDayInLastWeek, true);
                if (mWeekViews[5].hasToday()) {
                    mIsTodaySecondary = true;
                }
            }

            Log.d(TAG, "setSecondaryDayIndex() result: " + lastSecondaryDayInFirstWeek + ", " +
                    firstSecondaryDayInLastWeek);
        }
    }

    public Time getSelectedDate() {
        return mSelectedDate;
    }

    private int getWeekNumOfFirstMonthDay(Time date) {
        int firstDayOfWeek = Utils.getFirstDayOfWeek(mContext);
        return CalendarUtils.getWeekNumOfFirstMonthDay(date, firstDayOfWeek);
    }

    public void addWeekViewsByWeekNum(int weekNum) {
        removeAllViews();
        mActualWeekCount = DEFAULT_WEEK_COUNT;

        Time currMonth = CalendarUtils.getJulianMondayTimeFromWeekNum(weekNum + 1);
        ((MonthByWeekAdapter) mAdapter).updateFocusMonth(currMonth.month);

        boolean hasToday = false;
        for (int i = 0; i < mWeekViews.length; ++i) {
            mWeekViews[i] = (MonthWeekEventsView) mAdapter.getView(weekNum + i, null, this);

            addView(mWeekViews[i]);

            if (mWeekViews[i].hasToday()) {
                mHasToday = true;
                mTodayJulianDay = mWeekViews[i].getTodayJulianDay();
                hasToday = true;
            }
        }

        if (!hasToday) {
            mHasToday = false;
            mTodayJulianDay = -1;
        }

        addView(mAgendaField);
    }

    private int getMeasureSpec(int sizeWanted, int sizeMeasured) {
        int result = -1;
        if (sizeWanted > 0) {
            result = MeasureSpec.makeMeasureSpec(sizeWanted, MeasureSpec.EXACTLY);
        } else if (sizeWanted == ViewGroup.LayoutParams.MATCH_PARENT) {
            result = MeasureSpec.makeMeasureSpec(sizeMeasured, MeasureSpec.EXACTLY);
        } else if (sizeWanted == ViewGroup.LayoutParams.WRAP_CONTENT) {
            result = MeasureSpec.makeMeasureSpec(sizeMeasured, MeasureSpec.AT_MOST);
        }

        return result;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mRootView != null) {
            setRootViewSize(mRootView.getMeasuredWidth(), mRootView.getMeasuredHeight());
        }

        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();

        int count = getChildCount();
        if (count > mActualWeekCount) {
            count = mActualWeekCount;
        }

        for (int i = 0; i < count; ++i) {
            View child = getChildAt(i);
            ViewGroup.LayoutParams params = child.getLayoutParams();
            int childWidth = getMeasureSpec(params.width, measuredWidth);
            int childHeight = getMeasureSpec(params.height, measuredHeight);
            child.measure(childWidth, childHeight);
        }

        View weekView = getChildAt(0);
        if (count == mActualWeekCount) {
            int wantedHeight = mRootViewHeight
                    - weekView.getMeasuredHeight() * count
                    - mToolbarAndMonthHeaderHeight;

            int agendaFieldWidth = getMeasureSpec(ViewGroup.LayoutParams.MATCH_PARENT, mViewWidth);
            int agendaFieldHeight = getMeasureSpec(wantedHeight, 0);
            mAgendaField.measure(agendaFieldWidth, agendaFieldHeight);
        }

        int wantedTotalHeight = heightMeasureSpec;
        if (count > 0) {
            wantedTotalHeight = weekView.getMeasuredHeight() * count;
            wantedTotalHeight += mAgendaField.getMeasuredHeight();
        }

        setMeasuredDimension(widthMeasureSpec, wantedTotalHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mViewWidth = w;
        mViewHeight = h;
    }

    // must re-layout the child views, or all week views will be
    // displayed on a single line
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int baseLeft = 0;
        int baseTop = 0;

        int count = getChildCount();
        if (count > mActualWeekCount) {
            count = mActualWeekCount;
        }

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);

            int right = baseLeft + mViewWidth;
            int top = baseTop + child.getMeasuredHeight() * (i);
            int bottom = baseTop + child.getMeasuredHeight() * (i + 1);

            child.layout(baseLeft, top, right, bottom);
        }

        View firstChild = getChildAt(0);
        mWeekViewWidth = firstChild.getMeasuredWidth();
        mWeekViewHeight = firstChild.getMeasuredHeight();

        setWeekViewLT();

        if (mAgendaField != null) {
            int right = baseLeft + mViewWidth;
            int top = baseTop + mWeekViewHeight * mActualWeekCount;
            int bottom = mViewHeight;
            Log.d(TAG, "layout mAgendaField top/bottom " + top + "/" + bottom);

            mAgendaField.layout(baseLeft, top, right, bottom);
        }
    }

    private void setSelectedDateIndex(float x, float y) {
        int totalHeight = mWeekViewHeight * mActualWeekCount;
        if (y < mWeekViewTop || y > totalHeight) {
            Log.d(TAG, "y is out of range");
            return;
        }

        mHighlightWeekIndex = (int) ((y - mWeekViewTop) / mWeekViewHeight);

        if (x < mWeekViewLeft || x > (mViewWidth - mViewPadding)) {
            Log.d(TAG, "x is out of range");
            return;
        }

        mHighlightWeekDayIndex = (int) ((x - mWeekViewLeft) /
                (mViewWidth - mWeekViewLeft - mViewPadding) * DAYS_PER_WEEK);
        Log.d(DEBUG_TAG, "set selected index as " + mHighlightWeekIndex + ", " + mHighlightWeekDayIndex);
    }

    private void setWeekViewLT() {
        mWeekViewLeft = mViewPadding;
        mWeekViewTop = 0;
    }

    private static final int ACTION_STAY = 0;
    private static final int ACTION_NEXT = 1;
    private static final int ACTION_PREVIOUS = -1;

    private void highlightSelectedDate() {
        if (mHighlightWeekIndex >= 0 && mHighlightWeekIndex < mActualWeekCount) {
            MonthWeekEventsView tappedView = mWeekViews[mHighlightWeekIndex];

            int action = ACTION_STAY;
            int secondaryDateIndex = -1;
            // if (mHighlightWeekIndex == 0 || mHighlightWeekIndex == mActualWeekCount - 1) {
            if (mHighlightWeekIndex == 0 || mHighlightWeekIndex == 4) {
                secondaryDateIndex = tappedView.getSecondaryDayIndex();
                if (secondaryDateIndex >= 0) {
                    if (tappedView.getSecondaryDayDirection()) {
                        if (mHighlightWeekDayIndex >= secondaryDateIndex) {
                            action = ACTION_NEXT;
                        }
                    } else {
                        if (mHighlightWeekDayIndex <= secondaryDateIndex) {
                            action = ACTION_PREVIOUS;
                        }
                    }
                }
            }

            if (mHighlightWeekIndex == 5) {
                secondaryDateIndex = tappedView.getSecondaryDayIndex();
                if (mHighlightWeekDayIndex >= secondaryDateIndex) {
                    action = ACTION_NEXT;
                }
            }

            if (action == ACTION_STAY) {
                if (mHighlightWeekIndex == mPrevHighlightWeekIndex &&
                        mHighlightWeekDayIndex == mPrevHighlightWeekDayIndex) {
                    Log.d(DEBUG_TAG, "tapped the same date, return");
                    return;
                }

                int julianDay = tappedView.setAndReturnClickedDay(mHighlightWeekDayIndex);
                mSelectedDate.setJulianDay(julianDay);
                setHourAndMinute(mSelectedDate);
                mSelectedDate.normalize(true);

                Log.d(DEBUG_TAG, "1 - update time " + CalendarUtils.printDate(mSelectedDate));

                updateActionBarTime(mSelectedDate);

                setTimeOfController(mSelectedDate.toMillis(false));

                Log.d(DEBUG_TAG, "to invoke loadEventsOfSelectedDay()");
                loadEventsOfSelectedDay();

                for (int i = 0; i < mActualWeekCount; ++i) {
                    if (i != mHighlightWeekIndex) {
                        mWeekViews[i].clearClickedDay();
                    }
                }
            } else {
                boolean gotoNextMonth = (action == ACTION_NEXT);
                Time targetDate = new Time();
                targetDate.set(mBaseDate);

                int dayOffset = (Math.abs(mHighlightWeekDayIndex - secondaryDateIndex) + 1);
                if (gotoNextMonth) {
                    targetDate.month += 1;
                    targetDate.monthDay = dayOffset;
                } else {
                    targetDate.monthDay = 1;
                    targetDate.monthDay -= dayOffset;
                }
                targetDate.normalize(true);
                Log.d(DEBUG_TAG, "switch secondary month: " + CalendarUtils.printDate(targetDate));

                if (!CalendarUtils.isYearInRange(targetDate)) {
                    mHighlightWeekIndex = mPrevHighlightWeekIndex;
                    mHighlightWeekDayIndex = mPrevHighlightWeekIndex;
                    return;
                }

                switchMonth(targetDate);
            }
        }
    }

    private void selectMonthDay(float x, float y) {
        if (y > mWeekViewHeight * mActualWeekCount) {
            return;
        }

        Log.d(DEBUG_TAG, "save selected index: " + mPrevHighlightWeekIndex + ", " + mPrevHighlightWeekDayIndex);

        mPrevHighlightWeekIndex = mHighlightWeekIndex;
        mPrevHighlightWeekDayIndex = mHighlightWeekDayIndex;

        mHighlightWeekIndex = -1;
        mHighlightWeekDayIndex = -1;

        setSelectedDateIndex(x, y);
        highlightSelectedDate();
    }

    protected void highlightMonthDate(Time targetDate) {
        Log.d(TAG, "highlightMonthDate() has been invoked in view " + mViewId);

        int julianDay = Time.getJulianDay(targetDate.normalize(true), targetDate.gmtoff);

        boolean foundHighlightGrid = false;
        for (int i = 0; i < mActualWeekCount; ++i) {
            int index = mWeekViews[i].getWeekDayIndexByJulianDay(julianDay);
            if (index >= 0) {
                setSelectedWeekIndex(i);
                setSelectedWeekDayIndex(index);

                foundHighlightGrid = true;
                highlightSelectedDate();
                break;
            }
        }

        if (!foundHighlightGrid) {
            mHighlightWeekIndex = -1;
            mHighlightWeekDayIndex = -1;
        }
        Log.d(TAG, "DEBUG: not found grid to highlight");
    }

    private boolean mHighlightGivenDay = false;

    public void setHighlightFlag(boolean flag) {
        mHighlightGivenDay = flag;
    }

    public void highlightToday() {
        Log.d(DEBUG_TAG, "highlightToday() mHighlightGivenDay = " + mHighlightGivenDay);
        if (!mHighlightGivenDay) {
            if (mHasToday && !mIsTodaySecondary) {
                Log.d(DEBUG_TAG, "highlightToday() 1");
                // mSelectedDate.setJulianDay(mTodayJulianDay);
                // mSelectedDate.normalize(true);
                mSelectedDate.setToNow();
            } else {
                Log.d(DEBUG_TAG, "highlightToday() 2");
                mSelectedDate.monthDay = 1;
            }
        }
        mHighlightGivenDay = false;

        Log.d(DEBUG_TAG, "to highlight selected day: " + CalendarUtils.printDate(mSelectedDate));

        highlightMonthDate(mSelectedDate);
    }

    public void setAgendaListListener(EventLoader eventLoader) {
        if (mAgendaList == null) return;

        if (mAgendaList.getOnItemClickListener() == null) {
            mAgendaList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (position >= mAdapterEvents.size()) return;

                    Event event = mAdapterEvents.get(position);
                    sendEventForAgendaListItem(EventType.VIEW_EVENT, event);
                }
            });
        }
        if (mAgendaList.getOnItemLongClickListener() == null) {
            mAgendaList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    if (position >= mAdapterEvents.size()) return false;

                    Event event = mAdapterEvents.get(position);
                    sendEventForAgendaListItem(EventType.DELETE_EVENT, event);
                    return true;
                }
            });
        }
    }

    private void sendEventForAgendaListItem(long eventType, Event event) {
        CalendarController controller = CalendarController.getInstance(mContext);
        Time selectedTime = new Time();
        selectedTime.set(controller.getTime());
        selectedTime.normalize(true);

        controller.sendEventRelatedEventWithExtra(
                mContext,
                eventType,
                event.id,
                event.startMillis,
                event.endMillis,
                0, 0,
                CalendarController.EventInfo.buildViewExtraLong(
                        Attendees.ATTENDEE_STATUS_NONE, event.allDay),
                selectedTime.toMillis(true));
    }

    public void loadEventsOfSelectedDay() {
        if (mEventLoader == null) {
            Log.d(DEBUG_TAG, "mEventLoader is null!");
            return;
        }

        mSelectedDate = getTimeOfController();

        int selectedJulianDay = Time.getJulianDay(mSelectedDate.normalize(true), mSelectedDate.gmtoff);

        Log.d(DEBUG_TAG, "load event date " + CalendarUtils.printDate(mSelectedDate));

        if (mEvents == null) {
            mEvents = new ArrayList<>();
        } else {
            mEvents.clear();
        }

        mEventLoader.loadEventsInBackground(1, mEvents, selectedJulianDay,
                mEventsLoadingFinishedCallback, mEventsLoadingCanceledCallback);
    }


    private View mRootView = null;

    public void setRootView(View root) {
        mRootView = root;
    }

    public void gotoDate(Time targetDate) {
        if (!CalendarUtils.isYearInRange(targetDate)) {
            CalendarUtils.showToast(mContext);
            return;
        }

        Log.d(DEBUG_TAG, "to highlight date " + CalendarUtils.printDate(targetDate));

        switchMonth(targetDate);
    }

    /*private Animation mAnimAgendaFieldFadeIn = null;
    private static final long ANIM_DURATION_AGENDA_LIST_FADE_IN = 600L;

    private void startAgendaListAnim() {
        if (mHighlightWeekIndex == mPrevHighlightWeekIndex
                && mHighlightWeekDayIndex == mPrevHighlightWeekDayIndex) {
            // no need run animation multiple times for the same day
            return;
        }

        if (mAnimAgendaFieldFadeIn == null) {
            mAnimAgendaFieldFadeIn = new AlphaAnimation(0.0f, 1.0f);
            mAnimAgendaFieldFadeIn.setDuration(ANIM_DURATION_AGENDA_LIST_FADE_IN);
        }

        mAgendaList.startAnimation(mAnimAgendaFieldFadeIn);
    }*/

    private int mRootViewWidth = -1;
    private int mRootViewHeight = -1;

    private void setRootViewSize(int w, int h) {
        if (mRootViewWidth < w) mRootViewWidth = w;
        if (mRootViewHeight < h) mRootViewHeight = h;
    }

    private void setSelectedWeekIndex(int index) {
        mPrevHighlightWeekIndex = mHighlightWeekIndex;
        mHighlightWeekIndex = index;
    }

    private void setSelectedWeekDayIndex(int index) {
        mPrevHighlightWeekDayIndex = mHighlightWeekDayIndex;
        mHighlightWeekDayIndex = index;
    }

}
