package com.android.launcher3.specialeffectpreview;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.PagedView;
import com.android.launcher3.R;
import com.android.launcher3.SpecialEffectPagedView;
import com.android.launcher3.pageindicators.PageIndicatorUnderline;
import java.util.ArrayList;


public class EffectPreviewPagedView extends PagedView implements View.OnLongClickListener, View.OnClickListener{
    private static final String TAG = "EffectPreviewPagedView";
    private static final boolean DEBUG = false;

    Launcher mLauncher;

    private int rowSize = 4;

    ArrayList<Object> mWidgetsInfos;
    private LayoutInflater mLayoutInflater;

    private TypedArray mImgTypeArray;
    private String[] mEffectTitle;
    private String[] mEffectValue;
    private String mCurrentChoosedEffectValue;
    private SharedPreferences mSharedPref;
    private SharedPreferences.OnSharedPreferenceChangeListener mSharedPrefListener;
	private final int cellLayoutSize = 3;
	
    class ViewHolder {
        public int position;
        public ImageView previewImgView;
        public TextView titleTextView;
        public ImageView previewChecked;
    }
	

    public EffectPreviewPagedView(Context context) {
        this(context, null);
    }

    public EffectPreviewPagedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EffectPreviewPagedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = (Launcher) context;
        final Resources res = getResources();
        mWidgetsInfos = new ArrayList<Object>();
        mLayoutInflater = LayoutInflater.from(context);
        //rowSize = res.getInteger(R.integer.config_widegtscontainerpageview_cells_count);

        mImgTypeArray = res.obtainTypedArray(R.array.editmode_effect_choose_img);
        mEffectTitle = res.getStringArray(R.array.entries_effect_preference);
        mEffectValue = res.getStringArray(R.array.entryvalues_effect_preference);
        mSharedPref = context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        mCurrentChoosedEffectValue = mSharedPref.getString(SpecialEffectPagedView.SPECIAL_EFFECT_STYLE, SpecialEffectPagedView.DEFAULT_SPECIAL_EFFECT_TYPE);
        //addEffectViews();
		
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
            heightSize = idp.portraitProfile.widgetsContainerBarHeightPx - idp.portraitProfile.widgetsPageviewMarginBottom;
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
        mViewport.set(0, 0, widthSize, heightSize);
        // 给每一个子view给予相同的空间

        /** 滚动到目标坐标 */
//        scrollTo(mCurScreen * widthSize, 0);
        this.setMeasuredDimension(widthSize,heightSize);
    }
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void overScroll(float amount) {
        boolean shouldOverScroll = (amount <= 0 && mIsRtl) ||
                (amount >= 0 &&  !mIsRtl);

        if(shouldOverScroll) {
            dampedOverScroll(amount);
        }
    }
    @Override
    protected void getEdgeVerticalPostion(int[] pos) {
        View child = getChildAt(getPageCount() - 1);
        pos[0] = child.getTop();
        pos[1] = child.getBottom();
    }

    public void scrollToTop() {
//        snapToPage(0);
    }

    public void addEffectViews(){
        if(getChildCount() < cellLayoutSize) {
            GradientDrawable  bg= getBg();
            int position = 0;
            removeAllViews();
            for (int i = 0; i < cellLayoutSize && position <= SpecialEffectPagedView.Type.ROLL_DOWN; i++) {
                LinearLayout cellLayout = (LinearLayout) mLayoutInflater.inflate(R.layout.widgets_container_pageview_celllayout, this, false);

                for (int j = 0; j < rowSize && position <= SpecialEffectPagedView.Type.ROLL_DOWN; j++) {
                    ViewHolder vh = new ViewHolder();
                    View convertView = mLayoutInflater.inflate(R.layout.preview_effects_item, this, false);

                    vh.previewImgView = (ImageView) convertView.findViewById(R.id.preview_effects_image);
                    Drawable previewImg = mImgTypeArray.getDrawable(position);
                    vh.previewImgView.setImageDrawable(previewImg);
                    vh.previewImgView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    vh.titleTextView = (TextView) convertView.findViewById(R.id.preview_effects_title);
                    //vh.titleTextView.setVisibility(View.VISIBLE);
                    vh.titleTextView.setText(mEffectTitle[position]);
                    vh.previewChecked = (ImageView) convertView.findViewById(R.id.preview_effects_checked);
                    if (isChecked(position)) {
                        vh.previewChecked.setVisibility(View.VISIBLE);
                    } else {
                        vh.previewChecked.setVisibility(View.GONE);
                    }
                    vh.position = position;
                    convertView.setTag(vh);
                    LinearLayout previewEffect = (LinearLayout) convertView.findViewById(R.id.preview_effects_linearLayout);
                    previewEffect.setBackground(bg);
                    //convertView.setVisibility(View.VISIBLE);
                    convertView.setOnClickListener(this);
                    convertView.setOnLongClickListener(this);
                    cellLayout.addView(convertView);
                    position++;
                }
                addView(cellLayout);
            }
            requestLayout();
        }
    }

    private GradientDrawable  getBg(){
        float bgRectRadius = 8;
        RectF mDstRectF = new RectF();
        Rect rect = new Rect();
        int aaa = 20;
        rect.left = (int) (mDstRectF.left -aaa);
        rect.right = (int) (mDstRectF.right +aaa);
        rect.top = (int) (mDstRectF.top);
        rect.bottom = (int) (mDstRectF.bottom);

        GradientDrawable bgDrawable = new GradientDrawable();
        bgDrawable.setColor(Color.parseColor("#1A000000"));
        bgDrawable.setCornerRadius(bgRectRadius);
        bgDrawable.setStroke(2, Color.parseColor("#1Affffff"));
        bgDrawable.setBounds(rect);
         return bgDrawable;
    }


    @Override
    public void onClick(View v) {
        if (!mLauncher.getWorkspace().isPageMoving() && v.getTag() instanceof ViewHolder) {
            ViewHolder vh = (ViewHolder) v.getTag();			  
			if (!isChecked(vh.position)) {
				restCheckedState();
                vh.previewChecked.setVisibility(View.VISIBLE);
				setEffectValue(vh.position);
				mLauncher.getWorkspace().setCycleSlideFlag();
            }			
			mLauncher.getWorkspace().animateScrollEffect(true);
			 
        }
    }

    @Override
    public boolean onLongClick(View v) {
        // Log.d("cyl", "onLongClick");
        return false;
    }


    @Override
    protected void snapToPage(int whichPage, int delta, int duration, boolean immediate, TimeInterpolator interpolator) {
        if (mPageIndicator != null) {
            ((PageIndicatorUnderline)mPageIndicator).animateToAlpha(0f);
        }
        super.snapToPage(whichPage, delta, duration, immediate, interpolator);
    }


    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(x, y);
    }

   // public void updateWidgetsPageIndicator(){
    //    boolean isBlacktext = ColorManager.getInstance().isBlackText();
//        if(isBlacktext){
//            leftIndicator.setImageResource(R.drawable.ic_widgets_left_indicator_black);
//            rightIndicator.setImageResource(R.drawable.ic_widgets_right_indicator_black);
//        }else{
//            leftIndicator.setImageResource(R.drawable.ic_widgets_left_indicator);
//            rightIndicator.setImageResource(R.drawable.ic_widgets_right_indicator);
//        }
   // }



    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mPageIndicator != null) {
            mPageIndicator.setScroll(l, mMaxScrollX);
        }
    }

    @Override
    protected void determineScrollingStart(MotionEvent ev, float touchSlopScale) {
        super.determineScrollingStart(ev, touchSlopScale);
        if(mTouchState != TOUCH_STATE_SCROLLING){
            ((PageIndicatorUnderline)mPageIndicator).animateToAlpha(1.0f);
        }
    }

    @Override
    protected void resetTouchState() {
        super.resetTouchState();
        ((PageIndicatorUnderline) mPageIndicator).animateToAlpha(0.0f);
    }

    private boolean isChecked(int position) {
        if (mEffectValue[position].equals(mCurrentChoosedEffectValue)) {
            return true;
        }
        return false;
    }

    public void setEffectValue(int position) {
        mCurrentChoosedEffectValue = mEffectValue[position];
        mSharedPref.edit().putString(SpecialEffectPagedView.SPECIAL_EFFECT_STYLE, mCurrentChoosedEffectValue).commit();
    }

    public void restCheckedState(){
	  int position = 0;
      boolean found = false;
      for(int i = 0; i < cellLayoutSize; i++){
           LinearLayout cellLayout = (LinearLayout) getChildAt(i);
           int childCont = cellLayout.getChildCount();
           for(int j = 0; j < childCont; j++){
               View child = (View) cellLayout.getChildAt(j);
               if(child != null && child.getTag() instanceof ViewHolder) {
                   ViewHolder vh = (ViewHolder) child.getTag();
                   if (isChecked(vh.position)) {
                       vh.previewChecked.setVisibility(View.GONE);
                       found = true;
                       break;
                   }
               }
           }
           if(found){
               break;
           }
      }
    }
}
