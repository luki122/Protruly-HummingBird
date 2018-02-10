package com.android.launcher3;

import java.util.HashMap;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Scroller;



/**
 * A subclass of SlidePagedView to handle scroll effect
 * Mainly implemented in dispatchDraw
 */
public class SpecialEffectPagedView extends SlidePagedView {

    public float EDIT_SCALE = 1.0f;
	public static final String SPECIAL_EFFECT_CYCLE_SLIDE = "special_effect_cycle_slide";
    public static final String SPECIAL_EFFECT_STYLE = "special_effect_style";
	public static final String DEFAULT_SPECIAL_EFFECT_TYPE = "1";
    public static final float CAMERA_DISTANCE = 6500;
    public static String TAG = "SpecialEffectPagedView";

    protected float mDensity;
    private Camera mCamera;
    private Matrix mMatrix;

    private LauncherScroller mOriginScroller;
    private LauncherScroller mBounceScroller;
    
    private SharedPreferences mSharedPref;
    private OnSharedPreferenceChangeListener mSharedPrefListener;

    protected Launcher mLauncher;
	private int screenWidth = 1080;
	private int screenHight = 1788;
    /**
     * When scroll started, we save view's drawing cache in mIconBitmapCache.
     * Every time {@link #dispatchDraw} was called, get Bitmap from it to avoid
     * repeatedly calling {@link View#getDrawingCache}.
     */
    private HashMap<String,Bitmap> mIconBitmapCache;

    public static class Type {
        public final static int HORIZONTAL = 0;
        public final static int BOX_OUT = 1;
        public final static int BOX_IN = 2;
        public final static int ROLL_WINDOW = 3;
        public final static int ROLL_OVER = 4;
        public final static int SCALE_IN_OUT = 5;
        public final static int RANDOM_SWITCH = 6;
        public final static int CASCADE = 7;
        public final static int ROLL_UP = 8;
        public final static int ROLL_DOWN = 9;     
    }

    public SpecialEffectPagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSpecialEffect(context, attrs, 0);
    }

    public SpecialEffectPagedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initSpecialEffect(context, attrs, defStyle);
    }

    private void initSpecialEffect(Context context, AttributeSet attrs, int defStyle) {
		mDensity = getResources().getDisplayMetrics().density;
        mCamera = new Camera();
        mMatrix = new Matrix();
        mLauncher = Launcher.getLauncher(context);
		WindowManager wm1 = mLauncher.getWindowManager();
		screenWidth = wm1.getDefaultDisplay().getWidth();
		screenHight = 1920;//wm1.getDefaultDisplay().getHeight();
		Resources res = getResources(); 
        //EDIT_SCALE = res.getInteger(R.integer.config_workspaceOverviewShrinkPercentage) / 100f; 
        mIconBitmapCache = new HashMap<String,Bitmap>();        
        mOriginScroller = mScroller;
        mBounceScroller = new LauncherScroller(context, new BounceBackInterpolator());
        
        mSharedPref = context.getSharedPreferences(
                LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
		mPageAnimType = getSlideEffectMode(context);
       /* cyl del
       mSharedPrefListener = new OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (SPECIAL_EFFECT_STYLE.equals(key)) {
                    resetTransitionEffect();
                }
            }
        }; 
        mSharedPref.registerOnSharedPreferenceChangeListener(mSharedPrefListener);  */
        
    }

    // ##description: separate mainmenu slide effect from screen slide effect. 
    protected void onTransitionEffectChanged(int oldEffect, int newEffect) {
        if (mLauncher == null || mLauncher.getWorkspace() == null) {
            return;
        }
        mPageAnimType = newEffect;        
        // we don't need to run resetChildrenDrawing when first entered. 
        if (oldEffect != -1) {
            resetChildrenProperties();
        }
    }
    
    private void resetChildrenProperties() {
        for (int i = 0; i < getChildCount(); i++) {
            View page = getPageAt(i);
            page.setPivotX(0);
            page.setPivotY(0);
            page.setRotation(0);
            page.setRotationX(0);
            page.setRotationY(0);

            page.setVisibility(VISIBLE);
            page.setAlpha(1f);

            ViewGroup container;
            if (page instanceof CellLayout) {
                CellLayout cellLayout = (CellLayout) page;
                container = cellLayout.getShortcutsAndWidgets();
            } else { 
                // never
                return;
            }
            for (int j = 0; j < container.getChildCount(); j++) {
                View view = container.getChildAt(j);
                view.setPivotX(view.getMeasuredWidth() * 0.5f);
                view.setPivotY(view.getMeasuredHeight() * 0.5f);
                view.setRotation(0);
                view.setRotationX(0);
                view.setRotationY(0);
                view.setScaleX(1f);
                view.setScaleY(1f);
                view.setTranslationX(0f);
                view.setTranslationY(0f);
                view.setVisibility(VISIBLE);
                view.setAlpha(1f);
            }
        }
    }
   

    private void resetTransitionEffect() {
        for (int i = 0; i < getChildCount(); i++) {
            View page = getPageAt(i);
            page.setPivotX(0);
            page.setPivotY(0);
            page.setRotation(0);
            page.setRotationX(0);
            page.setRotationY(0);
            if (mLauncher.getWorkspace().inScaleState()) {// && (this instanceof Workspace)) {
                page.setScaleX(EDIT_SCALE);
                page.setScaleY(EDIT_SCALE);
          
            } else {
                page.setScaleX(1f);
                page.setScaleY(1f);
            }
            if (mLauncher.getWorkspace().inScaleState()) {// && (this instanceof Workspace)) {
                page.setTranslationX(page.getWidth() * (1 - EDIT_SCALE) / 2);
                page.setTranslationY(page.getHeight() * (1 - EDIT_SCALE) / 2);
            } else {
                page.setTranslationX(0f);
                page.setTranslationY(0f);
            }
            
            page.setX(page.getLeft() + page.getTranslationX());
            page.setY(page.getTop() + page.getTranslationY());
            page.invalidate();
            page.setVisibility(VISIBLE);
            page.setAlpha(1f);

            ViewGroup container;
            if (page instanceof CellLayout) {
                CellLayout cellLayout = (CellLayout) page;
                container = cellLayout.getShortcutsAndWidgets();
            } else {
                // never
                return;
            }
            for (int j = 0; j < container.getChildCount(); j++) {
                View view = container.getChildAt(j);
                view.setPivotX(view.getMeasuredWidth() * 0.5f);
                view.setPivotY(view.getMeasuredHeight() * 0.5f);
                view.setRotation(0);
                view.setRotationX(0);
                view.setRotationY(0);
                view.setScaleX(1f);
                view.setScaleY(1f);
                view.setTranslationX(0f);
                view.setTranslationY(0f);
                view.setVisibility(VISIBLE);
                view.setAlpha(1f);
            }
        }
    }
    
    private double getPercentage(View child, int screen){
        /*  CellLayout related size
         *                       current content
         *                       ***************
         *  *********   *********   *********   *********
         *  *       *   *       *   *       *   *       *
         *  *       *   *       *   *       *   *       *
         *  *       *   *       *   *       *   *       *
         *  *       *   *       *   *       *   *       *
         *  *********   *********   *********   *********
         *           ***         ***************
         *            *                 *
         *            *                 *
         *            *       getWidth() == child.getWidth() + 2 * gapOfCellLayouts
         *    gapOfCellLayouts
         *
         *  mScroll change (child.getWidth() + gapOfCellLayouts) every time you scroll
         */

        // the gap between two CellLayouts
        double gapOfCellLayouts = ( screenWidth - child.getWidth() ) / 2;
        double molecular   = getScrollX() - ( getChildOffset(screen) - gapOfCellLayouts );
       
        double denominator;     
        if (mLauncher.getWorkspace().inScaleState()){
      
            denominator = getScaledMeasuredWidth(getPageAt(screen)) + mPageSpacing;
        } else {
            denominator = child.getWidth() + gapOfCellLayouts;
        }
     
        double percentage  = molecular / denominator;

        if( percentage < -1 || percentage > 1 ) {
            // for the scroll between first and last screen
            if((mPageAnimType != Type.ROLL_DOWN) && (mPageAnimType != Type.ROLL_UP)
				|| (isNormalState() && ((mPageAnimType == Type.ROLL_DOWN) || (mPageAnimType == Type.ROLL_UP)))){ 
                if( getScrollX() < 0 ) {
                    percentage = 1 + getScrollX() / denominator;
                }else{
                    int last = getChildCount() - 1;
                    int leftEdge = getChildOffset(last) + child.getWidth();
                    percentage = (getScrollX() - leftEdge) / denominator;
                }
            } 
        }

        return percentage;
    }


    protected void drawScreen(Canvas canvas, int screen, long drawingTime) {
        if(mLauncher.getDragController().isDragging()
            || (!isPageMoving()) || reordering){
            super.drawScreen(canvas, screen, drawingTime);
			Log.d("cyl", "drawScreen: " + mLauncher.getDragController().isDragging()
				+ "  " +  !isPageMoving());
            return;
        }
      

        //if it's need to call ViewGroup#drawChild
        boolean drawChild = true;
        View child = getChildAt(screen);

        double percentage = getPercentage(child, screen);
			
        if( percentage <= -1 || percentage >= 1 ) {
			Log.d("cyl", "drawScreen: screen = " + screen + "  " +percentage);
			super.drawScreen(canvas, screen, drawingTime);
			return;
        }
        canvas.save();
        mCamera.save();

        /*
         * scroll to right：left 0% ~ 100%   right -100% ~ 0%
         * scroll to left ：left 100% ~ 0%   right 0% ~ -100%
         */
		Log.d("cyl", "drawScreen: mPageAnimType = " + mPageAnimType + " percentage = " +percentage);
        switch (mPageAnimType) {
            case Type.HORIZONTAL:
                break;
            case Type.BOX_OUT:
                drawChild = boxOut(canvas, screen, percentage);
                break;
            case Type.BOX_IN:
                drawChild = boxIn(canvas, screen, percentage);
                break;
            case Type.ROLL_UP:
                drawChild = rollUp(canvas, screen, percentage);
                break;
            case Type.ROLL_DOWN:
                drawChild = rollDown(canvas, screen, percentage);
                break;
            case Type.ROLL_WINDOW:
                drawChild = rollWindow(child, screen, (float)percentage);
                break;
            case Type.ROLL_OVER:
                drawChild = rollOver(child, screen, (float)percentage);
                break;
            case Type.SCALE_IN_OUT:
                drawChild = scaleInOut(canvas, screen, percentage);
                break;
            case Type.RANDOM_SWITCH:
                drawChild = randomSwitch(child, screen, (float)percentage);
                break;
            case Type.CASCADE:
                if (mLauncher.getWorkspace().inScaleState())              
                    drawChild = rightFadeEditMode(child,screen,(float)percentage);
                else
                    drawChild = cascade(child,screen,(float)percentage);
                break;
       
            default:
                break;
        }

        if (drawChild)
            drawChild(canvas, child, drawingTime);
        mCamera.restore();
        canvas.restore();
    }

    @Override
    protected void getEdgeVerticalPostion(int[] pos) {

    }

    private boolean rightFadeEditMode(View v, int i, float scrollProgress) {
        float trans = Math.abs(scrollProgress) * (float) (getScaledMeasuredWidth(v) + mPageSpacing);

        if (scrollProgress >= 0) {
            v.setPivotX(v.getWidth() / 2);
            // v.setPivotY(0);
            v.setScaleX(EDIT_SCALE);
            v.setScaleY(EDIT_SCALE);
            v.setAlpha(1);
            v.setTranslationX(0);
            return true;
        }

        float scaleFactor = EDIT_SCALE / 2.0f;
        float scale = scaleFactor + scaleFactor * (float) (1 + scrollProgress);
        float alpha = (1 + scrollProgress);

        v.setPivotX(v.getWidth() / 2);
        // v.setPivotY(0);
        v.setScaleX(scale);
        v.setScaleY(scale);
        v.setAlpha(alpha);
        v.setTranslationX(-trans);

        return true;
    }

    /*
     * Scroll like looking from outsize of a box
     */
    private boolean boxOut(Canvas canvas, int screen, double percentage ){
        float angle = 90 * (float)percentage;
        float centerX, centerY;

        View child = getChildAt(screen);
        int childW = child.getWidth();
        int childH = child.getHeight();
        int pageOffsetX = getChildOffset(screen);
        int pageOffsetY = (int) ((screenHight - child.getHeight()) / 2);

        if( angle >= 0 ){
            centerX = childW * 5/2 + pageOffsetX;
          
            if (mLauncher.getWorkspace().inScaleState())
             
                centerX -= mPageSpacing;
            centerY = pageOffsetY + childH;
        }else{
            centerX =  childW * 3/2 + pageOffsetX;
           
            if(mLauncher.getWorkspace().inScaleState())
            
                centerX += mPageSpacing;
            centerY = pageOffsetY + childH ;
        }
        mCamera.rotateY(-angle); // rotate around Y-axis reversely
        mCamera.setLocation(0, 0, -14 * mDensity);
        mCamera.getMatrix(mMatrix);

        mMatrix.preScale(1.0f - (Math.abs((float) percentage) * 0.3f), 1.0f);
        mMatrix.preTranslate(-centerX, -centerY);
        mMatrix.postTranslate(centerX, centerY);
        canvas.concat(mMatrix);
        return true;
    }

    /*
     * Scroll like looking from inside of a box
     */
    private boolean boxIn(Canvas canvas, int screen, double percentage ){
        float angle = 90 * (float)percentage;
        float centerX, centerY, changeZ;

        View child = getChildAt(screen);
        int childW = child.getWidth();
        int childH = child.getHeight();
        int pageOffsetX = getChildOffset(screen);
        int pageOffsetY = (int) ((getHeight() - child.getHeight()) / 2);

        if( angle >= 0 ){
            centerX = childW * 5/2 + pageOffsetX;
          
            if (mLauncher.getWorkspace().inScaleState())
            	
                centerX -= mPageSpacing;
            centerY = pageOffsetY + childH/2;
        }else{
            centerX = childW * 3/2 + pageOffsetX;

            if (mLauncher.getWorkspace().inScaleState())
          
                centerX += mPageSpacing;
            centerY = pageOffsetY + childH/2;
        }

        // In case of image expand, change Z-order
        if (angle >= 0) {
            // far to near (0-45), near to far (45-90)
            if (angle <= 45.0f) {
                changeZ = childW*(float)Math.sin(2 * Math.PI * angle /360f);
                mCamera.translate( 0, 0, changeZ );
            } else {
                changeZ = childW*(float)Math.sin(2 * Math.PI *(90-angle)/360f);
                mCamera.translate( 0, 0, changeZ );
            }
        } else {
            // make sure that two views join well
            if (angle > -45.0f) {
                changeZ = childW * (float) Math.sin(2 * Math.PI * (-angle)/ 360f); 
                mCamera.translate( 0, 0, changeZ );
            } else {
                changeZ = childW * (float) Math.sin(2 * Math.PI* (90.0f + angle) / 360f); 
                mCamera.translate( 0, 0, changeZ );
            }
        }
        mCamera.rotateY(angle); 
        mCamera.setLocation(0, 0, -12 * mDensity);
        mCamera.getMatrix(mMatrix);
        mMatrix.preScale(1.0f - (Math.abs((float) percentage) * 0.5f), 1.0f);
        mMatrix.preTranslate(-centerX, -centerY);
        mMatrix.postTranslate(centerX, centerY);
        canvas.concat(mMatrix);

        return true;
    }

    /*
     * Rotate around the top of the screen
     */
    private boolean rollUp(Canvas canvas, int screen, double percentage ){
        float angle = 90 * (float)percentage;
        float baseAngle = angle * 0.35f; // Maximum Angle 30
        float centerX, centerY;

        View child = getChildAt(screen);
        int childW = child.getWidth();
        int childH = child.getHeight();
        double gapOfCellLayouts = ( screenWidth - childW ) / 2;
        double switchWidth = getChildCount() * ( childW + gapOfCellLayouts );
        double wholeWidth = gapOfCellLayouts + switchWidth;

        if( getScrollX() < 0 && screen == getChildCount()-1 ) {
            centerX = getScrollX() + (float)switchWidth + getWidth() / 2;
        }else if( getScrollX() + screenWidth > wholeWidth && screen == 0){
            centerX = getScrollX() - (float)switchWidth + getWidth() / 2; 
        }else{
            centerX = getScrollX() + getWidth() / 2; 
        }
        centerY = getScrollY() + childH * 0.6f;   
		//Log.d("cyl", "screen = " + screen+"  percentage = "+ percentage);


        mMatrix.reset();
        mMatrix.setRotate(baseAngle);
        mMatrix.preTranslate(-centerX, -centerY); 
        mMatrix.postTranslate(centerX, centerY);
        canvas.concat(mMatrix);

        return true;
    }

    /*
     * Rotate around the bottom of the screen
     */
    private boolean rollDown(Canvas canvas, int screen, double percentage ){
        float angle = 90 * (float)percentage;
        float baseAngle = -angle * 0.333f; // Maximum Angle 30
        float centerX, centerY;

        View child = getChildAt(screen);
        int childW = child.getWidth();
        int childH = child.getHeight();
        double gapOfCellLayouts = ( screenWidth - childW ) / 2;
        double switchWidth = getChildCount() * ( childW + gapOfCellLayouts );
        double wholeWidth = gapOfCellLayouts + switchWidth;

        if( getScrollX() < 0 && screen == getChildCount()-1 ) {
			double w = getChildCount() * ( childW + ( screenWidth - childW ) / 2 );
            centerX = getScrollX() + (float)w + getWidth() / 2;
        }else if( getScrollX() + screenWidth > wholeWidth && screen == 0){
            centerX = getScrollX() - (float)switchWidth + getWidth() / 2;
        }else{
            centerX = getScrollX() + getWidth() / 2;
        }
        centerY = childH * 2.1f;

        mMatrix.reset();
        mMatrix.setRotate(baseAngle); 
        mMatrix.preTranslate(-centerX, -centerY); 
        mMatrix.postTranslate(centerX, centerY);
        canvas.concat(mMatrix);
        
        return true;
    }

    private boolean rollOver(View v, int i, float scrollProgress){

       /* if (mLauncher.getWorkspace().inScaleState()){
            scrollProgress = scrollProgress / 0.85f;
            scrollProgress = Math.min(1.0f, scrollProgress);
            scrollProgress = Math.max(-1.0f, scrollProgress);
        } */
        v.setCameraDistance( mDensity * CAMERA_DISTANCE);
        boolean drawChild;
        if (scrollProgress >= -0.5f && scrollProgress <= 0.5f) {
            drawChild = true;
        }else{
            drawChild = false;
        }

        int offset = 0;
        if( scrollProgress > 0.5 ){
            scrollProgress = 1 - scrollProgress;
            if (mLauncher.getWorkspace().inScaleState()){         
                offset = - mPageSpacing;
            }
        }else if( scrollProgress < -0.5){
            scrollProgress = - 1 - scrollProgress;   
            if (mLauncher.getWorkspace().inScaleState()){        
                offset = + mPageSpacing;
            }
        }

        float rotation = -180.0f * Math.max(-1f, Math.min(1f, scrollProgress));
        v.setPivotX(v.getMeasuredWidth() * 0.5f);
 
        if(!mLauncher.getWorkspace().inScaleState()){       
            v.setPivotY(v.getMeasuredHeight() * 0.5f);
        }
        v.setRotationY(rotation);
        v.setTranslationX(v.getMeasuredWidth() * scrollProgress + offset);

        return drawChild;
    }

    /*
     * Flip around y-axis
     */
    private boolean rollWindow(View v, int screen, float scrollProgress){
        CellLayout cellLayout = (CellLayout) v;
        ShortcutAndWidgetContainer container = cellLayout.getShortcutsAndWidgets();
        if (Math.abs(scrollProgress) < 0.5f) {
            v.setAlpha(1);
            if ((mLauncher.getWorkspace().inScaleState())&& scrollProgress != 0) {
                int width = getScaledMeasuredWidth(getPageAt(screen)) + mPageSpacing;
                float trans = scrollProgress * width;// + (screenWidth - (width-mPageSpacing))/2; 
                v.setTranslationX(trans);       
            } else{         
                v.setTranslationX(scrollProgress * (screenWidth + v.getWidth()) / 2);
            }
        } else {
            /* cyl del
            if(mLauncher.getWorkspace().inScaleState()){
                v.setTranslationX(v.getWidth() * (1 - EDIT_SCALE) / 2);
            }*/
            v.setAlpha(0);   
        }

       // if (!cellLayout.isLeftPage()) { cyl del
            int count = container.getChildCount();
            for (int i = 0; i < count; i++) {
                View view = container.getChildAt(i);

                // (Bugmake FolderIcon display normally
                view.setCameraDistance(10000);
                view.setPivotX(view.getWidth() * 0.5f);
                view.setRotationY(-scrollProgress * 180f);
            }
      /* cyl del
      } else {
         
            // for life center, apply the transformation on the cell layout
            v.setCameraDistance(mDensity * CAMERA_DISTANCE);
            v.setPivotX(v.getWidth() * 0.5f);
            v.setRotationY(-scrollProgress * 180f);
        } */

        return true;
    }

    /*
     * Squash and Stretch
     */
    private boolean scaleInOut(Canvas canvas, int screen, double percentage){
        float angle = 90f * (float)percentage;
        View child = getChildAt(screen);
        int pageOffsetX = getChildOffset(screen);
        int pageOffsetY = (int) ((getHeight() - child.getHeight()) / 2);
     
        int childW = child.getWidth();
        int childH = child.getHeight();
		

        if (angle >= 0) { // left page
            float centerX = childW * 5/2 + pageOffsetX;
            if(mLauncher.getWorkspace().inScaleState())          
                centerX -= mPageSpacing;
            float centerY = pageOffsetY;
            canvas.translate(centerX, centerY);
            canvas.scale((90.0f - angle) / 90.0f, 1.0f);
            canvas.translate(-centerX, -centerY);
        } else {
            float centerX = childW * 3/2 + pageOffsetX;
            if(mLauncher.getWorkspace().inScaleState())         
                centerX += mPageSpacing;
            float centerY = pageOffsetY; 
            canvas.translate(centerX, centerY);
            canvas.scale((90.0f + angle) / 90.0f, 1.0f); 
            canvas.translate(-centerX, -centerY);
        }
		//Log.d("cyl", "screen = " + screen+"  percentage = "+ percentage);
        return true;
    }

    /*
     * Icons change it's position randomly in Y-axis.
     */
    private boolean randomSwitch(View v, int screen, float scrollProgress){
        CellLayout cellLayout = (CellLayout) v;
        ShortcutAndWidgetContainer container = cellLayout.getShortcutsAndWidgets();

        final float verticalDelta = 0.7f * cellLayout.getCellHeight()
                                    * (float) (1 - Math.abs(2 * Math.abs(scrollProgress) - 1)); 

        for (int i = 0; i < container.getChildCount(); i++) {
            View view = container.getChildAt(i);
            ItemInfo info = (ItemInfo) view.getTag();
            if ((info.cellX % 2 == 0)) {
                // even columns
                view.setTranslationY(verticalDelta);
            } else {
                // odd columns
                view.setTranslationY(-verticalDelta);
            }
        }
        return true;
    }

    /*
     * Left side scroll to left.Right side fade away.
     */
    private boolean cascade(View v, int i, float scrollProgress){
        if( scrollProgress >= 0 ){
            v.setScaleX(1);
            v.setScaleY(1);
            v.setAlpha(1);
            v.setTranslationX(0);
            return true;
        }

        double gapOfCellLayouts = ( screenWidth - v.getWidth() ) / 2;
        float scale = 0.5f + 0.5f * (float)(1+scrollProgress);
        float alpha = (1+scrollProgress); 
        float trans = Math.abs(scrollProgress) * (float)(gapOfCellLayouts + v.getWidth());

        v.setPivotX(v.getWidth()/2);
        v.setPivotY(v.getHeight()/2);
        v.setScaleX(scale);
        v.setScaleY(scale);
        v.setAlpha(alpha);
        v.setTranslationX(-trans);

        return true;
    }

    @Override
    protected void onPageBeginMoving() {
        super.onPageBeginMoving();
        mPageAnimType = getSlideEffectMode(this.getContext());
        if( mPageAnimType == Type.ROLL_UP || mPageAnimType == Type.ROLL_DOWN ){
            mScroller = mBounceScroller;
        } 
        
    }
    
    @Override
    protected void onPageEndMoving() {
        super.onPageEndMoving();
        mIconBitmapCache.clear();
        if( mPageAnimType == Type.ROLL_UP || mPageAnimType == Type.ROLL_DOWN ){
            mScroller = mOriginScroller;
        } 
     
        
        // recovers the transformation during sliding
        if (mPageAnimType == Type.CASCADE) {
            if(this instanceof Workspace && ((Workspace)this).getOpenFolder() != null ){
                // Bug ,wenliang.dwl,don't restore when folder is open
                return;
            } 
            
            if (mLauncher.getDragController().isDragging()) {
                return;
            }

            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                View v = getPageAt(i);
                if( v != null ){
                    float scaleX = 1.0f;
                    float scaleY = 1.0f;
                    float transX = 0f;
                    
                    if(mLauncher.getWorkspace().inScaleState()){ 
                        scaleX = EDIT_SCALE;
                        scaleY = EDIT_SCALE;
                    }
              

                     v.setScaleX(scaleX);
                     v.setScaleY(scaleY);
                     v.setAlpha(1);
                     v.setTranslationX(-transX);
                }
            }
        } else if (mPageAnimType == Type.ROLL_WINDOW ||
                   mPageAnimType == Type.RANDOM_SWITCH) {
            recoverPageTransformation();
        } else if (mPageAnimType == Type.ROLL_OVER){
            // for bug , restore state
            for( int i = 0; i < getChildCount(); i++ ){
                View v = getChildAt(i);
                if( v != null ) {
                    v.setRotationY(0);
                    v.setTranslationX(0);
                }
            }
        }
    }


    private void recoverPageTransformation() {
        for( int i = 0; i < getChildCount(); i++ ){
            CellLayout cl = (CellLayout)getChildAt(i);
            if( cl == null ) continue;
           // if (!mLauncher.getWorkspace().inScaleState()){         
                cl.setTranslationX(0);
           // }
           // if (cl.isLeftPage()) cyl del
            //    cl.setRotationY(0);
            cl.setAlpha(1); 
            ShortcutAndWidgetContainer container = cl.getShortcutsAndWidgets();
            if( container == null ) continue;
            for( int j = 0; j < container.getChildCount(); j++ ){
                View v = container.getChildAt(j);
                v.setRotationY(0);
                v.setTranslationY(0);
            }
        }
    }

    private static class BounceBackInterpolator extends ScrollInterpolator {
        public BounceBackInterpolator() {
        }

        public float getInterpolation(float t) {
            t = super.getInterpolation(t);
            float UP_BOUND = 1.1f;
            float TURN_POINT = 0.9f;
            if( t < TURN_POINT ){
                return t * ( UP_BOUND / TURN_POINT );
            }else{
                return UP_BOUND - (t - TURN_POINT) * ( (UP_BOUND-1) / (1-TURN_POINT) );
            }
        }
    }

// cyl add
 protected int getScaledMeasuredWidth(View child) {
   	// This functions are called enough times that it actually makes a difference in the
   	// profiler -- so just inline the max() here
   	// make sure measureWidth can be assigned a valid value
   	float mLayoutScale = 1.0f;
   	int measuredWidth = -1;
   	if(child == null) {
   		DisplayMetrics dm = getResources().getDisplayMetrics();
		int hight = measuredWidth = dm.heightPixels;;
   		measuredWidth = dm.widthPixels;
   	} else {
   		measuredWidth = child.getMeasuredWidth();
   	}
      
   	//final int minWidth = mMinimumWidth; 
   	//final int maxWidth = (minWidth > measuredWidth) ? minWidth : measuredWidth; 
   	/* cyl del
   	if(mLauncher.getWorkspace().inScaleState()){
 	   mLayoutScale = EDIT_SCALE;
   	} */
   	return (int) (measuredWidth * mLayoutScale + 0.5f);
 }

public  int getSlideEffectMode(Context ctx) {

	String value = mSharedPref.getString(SPECIAL_EFFECT_STYLE, DEFAULT_SPECIAL_EFFECT_TYPE);
	int mode = Integer.parseInt(value);
	return mode;
}

    public boolean notHorizontal(){
        return getSlideEffectMode(this.getContext()) != Type.HORIZONTAL;
    }
}
