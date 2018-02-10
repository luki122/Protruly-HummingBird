package com.android.systemui.qs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;

import com.android.systemui.R;
import com.android.systemui.qs.QSPanelPullUp.QuickSettingCallback;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.statusbar.phone.NavigationBarView;
/**
 * 
 * @author storktang
 *
 */
/**hb tangjun add begin*/
public class QSPanelViewForPullUp extends FrameLayout{
	
	private static final int QUIT_ANIMATION_DURATION = 300;
	private Context mContext;
	private Scroller mScroller;
	private QSPanelPullUp mQSpanelView;
	private ImageView mPullDownView;
	private QSPanelBlurView mBlurView;
	private View mBackView;
    private int mInitQuickSettingTranslationYPort = 0;
    private int mInitQuickSettingTranslationXLand = 0;
    public boolean isScreenOff = false;
    private boolean mListening;
    
    private int mPullDownViewHeightPort;
    private int mPullDownViewWidthPort;
    private int mPullDownViewHeightLand;
    private int mPullDownViewWidthLand;
    private int mPullDownViewMarginBottomPort;
    private int mPullDownViewMarginRightLand;
    private int mBackViewHeightPort;
    private int mBackViewWidthLand;
    
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
        	if (Intent.ACTION_SCREEN_OFF.equals(action)) {
        		//if(isShown()){
        		if(getVisibility() == View.VISIBLE) {
        			isScreenOff = true;
        			//hb tangjun mod begin
        			mListening = false;
        			mQSpanelView.setListening(mListening);
        			//hb tangjun mod end
        			setToInitState();
        		}
            } else if(Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)){
        		//if(isShown()){
            	if(getVisibility() == View.VISIBLE) {
        			setToInitState();
        		}
            }
        }
    };
	
    public QSPanelViewForPullUp(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QSPanelViewForPullUp(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
		mContext = context;
		mScroller = new Scroller(context);
		initDimens();
		registerReceiver();
    }
    
    private void initDimens() {
    	mInitQuickSettingTranslationYPort = (int)(mContext.getResources().getDimension(R.dimen.quicksetting_panel_height_port));
    	mInitQuickSettingTranslationXLand = (int)(mContext.getResources().getDimension(R.dimen.quicksetting_panel_width_land));
    	mPullDownViewHeightPort = mContext.getResources().getDimensionPixelSize(R.dimen.quick_settings_pulldown_view_height_port);
    	mPullDownViewWidthPort = mContext.getResources().getDimensionPixelSize(R.dimen.quick_settings_pulldown_view_width_port);
    	mPullDownViewHeightLand = mContext.getResources().getDimensionPixelSize(R.dimen.quick_settings_pulldown_view_height_land);
    	mPullDownViewWidthLand = mContext.getResources().getDimensionPixelSize(R.dimen.quick_settings_pulldown_view_width_land);
    	mPullDownViewMarginBottomPort = mContext.getResources().getDimensionPixelSize(R.dimen.quick_settings_pulldown_view_marginbottom_port);
    	mPullDownViewMarginRightLand = mContext.getResources().getDimensionPixelSize(R.dimen.quick_settings_pulldown_view_marginright_land);
    	mBackViewHeightPort = mContext.getResources().getDimensionPixelSize(R.dimen.quicksetting_background_height_port);
    	mBackViewWidthLand = mContext.getResources().getDimensionPixelSize(R.dimen.quicksetting_background_width_land);
    }
    
    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
    	// TODO Auto-generated method stub
    	super.onVisibilityChanged(changedView, visibility);
    }
    
    public boolean getQSPanelAlreadyShow() {
    	//滑动完成或者手势松开继续滑动过程中判断为true
    	boolean isQSPanelAlreadyShow = false; 
    	if(Utilities.isOrientationPortrait(mContext)) {
    		isQSPanelAlreadyShow = (getVisibility() == View.VISIBLE) && 
    			((getScrollY() == mInitQuickSettingTranslationYPort && mScroller.isFinished()) || (getScrollY() > 0 && !mScroller.isFinished()));
    	} else {
    		isQSPanelAlreadyShow = (getVisibility() == View.VISIBLE) && 
        			((getScrollX() == mInitQuickSettingTranslationXLand && mScroller.isFinished()) || (getScrollX() > 0 && !mScroller.isFinished()));
    	}
    	return isQSPanelAlreadyShow;
    }
    
	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		Log.d("555555", "---QSPanelViewForPullUp onConfigurationChanged");
		updateChildLayoutParam();
	}
	
	private void updateChildLayoutParam(){
		FrameLayout.LayoutParams qsPanelViewParam = (FrameLayout.LayoutParams)mQSpanelView.getLayoutParams();
		FrameLayout.LayoutParams qsPullDownViewParam = (FrameLayout.LayoutParams)mPullDownView.getLayoutParams();
		FrameLayout.LayoutParams backViewParam = (FrameLayout.LayoutParams)mBackView.getLayoutParams();
		FrameLayout.LayoutParams blurViewParam = (FrameLayout.LayoutParams)mBlurView.getLayoutParams();
		if(Utilities.isOrientationPortrait(mContext)){
			qsPanelViewParam.width = FrameLayout.LayoutParams.MATCH_PARENT;
			qsPanelViewParam.height = mInitQuickSettingTranslationYPort;
			qsPanelViewParam.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
			mQSpanelView.setTranslationY(mInitQuickSettingTranslationYPort);
			mQSpanelView.setTranslationX(0);
			//mQSpanelView.setBackgroundResource(R.drawable.quicksetting_bg);
			//mQSpanelView.setBackgroundColor(Color.parseColor("#00ffffff"));

			qsPullDownViewParam.width = mPullDownViewWidthPort;
			qsPullDownViewParam.height = mPullDownViewHeightPort;
			qsPullDownViewParam.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
			qsPullDownViewParam.bottomMargin = mPullDownViewMarginBottomPort;
			qsPullDownViewParam.rightMargin = 0;
			mPullDownView.setTranslationY(mInitQuickSettingTranslationYPort);
			mPullDownView.setTranslationX(0);
			mPullDownView.setImageResource(R.drawable.qs_pulldown_2);
			
			backViewParam.width = FrameLayout.LayoutParams.MATCH_PARENT;
			backViewParam.height = mBackViewHeightPort;
			
			blurViewParam.width = FrameLayout.LayoutParams.MATCH_PARENT;
			blurViewParam.height = mInitQuickSettingTranslationYPort;
			blurViewParam.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
			mBlurView.setTranslationY(mInitQuickSettingTranslationYPort);
			mBlurView.setTranslationX(0);
		} else {
			qsPanelViewParam.width = mInitQuickSettingTranslationXLand;
			qsPanelViewParam.height = FrameLayout.LayoutParams.MATCH_PARENT;
			qsPanelViewParam.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
			mQSpanelView.setTranslationY(0);
			mQSpanelView.setTranslationX(mInitQuickSettingTranslationXLand);
			//mQSpanelView.setBackgroundResource(R.drawable.quicksetting_bg_land);
			//mQSpanelView.setBackgroundColor(Color.parseColor("#00ffffff"));

			qsPullDownViewParam.width = mPullDownViewWidthLand;
			qsPullDownViewParam.height = mPullDownViewHeightLand;
			qsPullDownViewParam.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
			qsPullDownViewParam.bottomMargin = 0;
			qsPullDownViewParam.rightMargin = mPullDownViewMarginRightLand;
			mPullDownView.setTranslationY(0);
			mPullDownView.setTranslationX(mInitQuickSettingTranslationXLand);
			mPullDownView.setImageResource(R.drawable.qs_pulldown_land_2);
			
			backViewParam.width = mBackViewWidthLand;
			backViewParam.height = FrameLayout.LayoutParams.MATCH_PARENT;
			
			blurViewParam.width = mInitQuickSettingTranslationXLand;
			blurViewParam.height = FrameLayout.LayoutParams.MATCH_PARENT;
			blurViewParam.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
			mBlurView.setTranslationY(0);
			mBlurView.setTranslationX(mInitQuickSettingTranslationXLand);
		}
		mQSpanelView.setLayoutParams(qsPanelViewParam);
		mPullDownView.setLayoutParams(qsPullDownViewParam);
		mBackView.setLayoutParams(backViewParam);
		mBlurView.setLayoutParams(blurViewParam);
	}
	
	@Override
	protected void onFinishInflate() {
		// TODO Auto-generated method stub
		super.onFinishInflate();
		mQSpanelView = (QSPanelPullUp)findViewById(R.id.qspanel_pullup);
		mQSpanelView.setOnQuickSettingCallback(new QuickSettingCallback() {
			
			@Override
			public void finishQuickSetting() {
				// TODO Auto-generated method stub
				setToInitState();
			}
		});
		mPullDownView = (ImageView)findViewById(R.id.pulldownview);
		mBlurView = (QSPanelBlurView)findViewById(R.id.blur_view);
		mBackView = findViewById(R.id.back);
	}
	
	private void registerReceiver() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
		intentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

    	mContext.registerReceiver(mIntentReceiver, intentFilter);
	}
	
	public View getQuickSettingView() {
		if(mQSpanelView == null) {
			mQSpanelView = (QSPanelPullUp)findViewById(R.id.qspanel_pullup);
		}
		return mQSpanelView;
	}
	
	public View getPullDownView() {
		if(mPullDownView == null) {
			mPullDownView = (ImageView)findViewById(R.id.pulldownview);
		}
		return mPullDownView;
	}
	
	public QSPanelBlurView getBlurView() {
		if(mBlurView == null) {
			mBlurView = (QSPanelBlurView)findViewById(R.id.blur_view);
		}
		return mBlurView;
	}
	
    @Override
	public boolean onTouchEvent(MotionEvent event) {
    	//只要up了就隐藏掉
    	/*
    	if(inRangeOfView(mQSpanelView, event)){
    		return true;
    	}
    	*/
    	
    	if (event.getAction() ==  MotionEvent.ACTION_UP || event.getAction() ==  MotionEvent.ACTION_CANCEL){
    		setToInitState();
    	}
		return true;
	}
    
    private boolean inRangeOfView(View view, MotionEvent ev){
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        if(ev.getX() < x || ev.getX() > (x + view.getWidth()) || ev.getY() < y || ev.getY() > (y + view.getHeight())){
            return false;
        }
        return true;
    }
    
    public int getInitQuickSettingInitTranslation() {
    	if(Utilities.isOrientationPortrait(mContext)) {
    		return mInitQuickSettingTranslationYPort;
    	} else {
    		return mInitQuickSettingTranslationXLand;
    	}
    }
    
    public void setToInitState() {
    	if(isScreenOff) {
    		//mView.setBackgroundColor(Color.parseColor("#00000000"));
			scrollTo(0, 0);
			setVisibility(View.GONE);
			isScreenOff = false;
    	} else {
    		smoothScrollTo(0, 0);
    	}
    }
    
    /**
     * Start scroll or extend scroll if already scrolling.
     * Start speed is 0
     */
//    public void scroll(int startZ, int z, int duration) {
//    	if (mScroller.isFinished()) {
//    		startScroll(0, startZ, 0, 0, duration);
//    	} else {
//    		mScroller.setFinalY(-z);
//    		mScroller.extendDuration(200);
//        }
//    }
    
    /**
     * 调用此方法滚动到目标位置
     * @param fx  目标x坐标
     * @param fy  目标Y坐标
     */
    public void smoothScrollTo(int fx, int fy){
    	if((fy == 0 && Utilities.isOrientationPortrait(mContext)) || (fx == 0 && !Utilities.isOrientationPortrait(mContext))) {
        	if (mListening) {
        		mListening = false;
        	}
    	} else {
        	if (!mListening) {
        		mListening = true;
        	}
    	}
		mQSpanelView.setListening(mListening);
		
        int scrollX = getScrollX();
        int dx = fx - scrollX;

        int scrollY = getScrollY();
        int dy = fy - scrollY;

        mScroller.startScroll(scrollX, scrollY, dx, dy, 300);
		//invalidate();
        postInvalidateOnAnimation();
    }
    
    public void setAlphaByScroll(int scrollZ) {
        final int H = Utilities.isOrientationPortrait(mContext) ? mInitQuickSettingTranslationYPort : mInitQuickSettingTranslationXLand;
        scrollZ = Math.min(scrollZ, H);
        float alpha = 1f;
        alpha = 255 * scrollZ / H;
        if(alpha < 0){
        	alpha = 0;
        }
        //setAlpha(alpha);
        mBackView.getBackground().setAlpha((int)alpha);
        //由于做成控制中心上拉盖住虚拟键，所以就不用改变alpha值了，留存实现即可
        //设置虚拟键的alpha值随着scroll相应变化
        //mView.getBackground().setAlpha((int)alpha);
    }
    
    @Override
    public void computeScroll() {
    	// TODO Auto-generated method stub
		// 先判断mScroller滚动是否完成
		if (mScroller.computeScrollOffset()) {
			scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			setAlphaByScroll(Utilities.isOrientationPortrait(mContext) ? mScroller.getCurrY() : mScroller.getCurrX());
			// 必须调用该方法，否则不一定能看到滚动效果
			//postInvalidate();
			postInvalidateOnAnimation();
			mBlurView.postInvalidateOnAnimation();
		} else {
			int scrollZ = Utilities.isOrientationPortrait(mContext) ? getScrollY() : getScrollX();
			if(scrollZ == 0) {
				setVisibility(View.GONE);
				//mView.setBackgroundColor(Color.parseColor("#00000000"));
			}
		}
    	super.computeScroll();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
    	// TODO Auto-generated method stub
    	super.onDraw(canvas);
    }
    
	public void closeScroll(){
		if (!mScroller.isFinished()) {
           mScroller.abortAnimation();
       }
	}
	
	public boolean getScrollFinished() {
		return mScroller.isFinished();
	}
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    	switch (event.getKeyCode()) {
    	case KeyEvent.KEYCODE_BACK:
    		if (event.getAction() == KeyEvent.ACTION_UP) {
    			// Cause pull up from BACK key fail
    			if (!event.isCanceled()) {
    				setToInitState();
    			}
    		}
    		break;

    	}
    	return super.dispatchKeyEvent(event);
    }
    
    private NavigationBarView mView;
    public void setNavigationBarView(NavigationBarView view) {
    	mView = view;
    }
}
/**hb tangjun add end*/
