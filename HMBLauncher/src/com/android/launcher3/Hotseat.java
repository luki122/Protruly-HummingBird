/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.dragndrop.DragView;
import com.android.launcher3.dynamicui.ExtractedColors;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.logging.UserEventDispatcher;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.userevent.nano.LauncherLogProto.Target;

import java.util.ArrayList;

public class Hotseat extends FrameLayout
        implements UserEventDispatcher.LaunchSourceProvider {

    private CellLayout mContent;

    private Launcher mLauncher;

    @ViewDebug.ExportedProperty(category = "launcher")
    private final boolean mHasVerticalHotseat;

    @ViewDebug.ExportedProperty(category = "launcher")
    private int mBackgroundColor;
    @ViewDebug.ExportedProperty(category = "launcher")
    private ColorDrawable mBackground;
    private ValueAnimator mBackgroundColorAnimator;

    // cyl add for hotseat icon center start
    private AnimatorSet mAnimatorSet;
    private BubbleTextView mInvisibleView;
    private boolean mInvisibleViewAdded = false;
    private int mCurrentInvisibleIndex = -1;
    private int mAnimStartY, mAnimEndY;
    private boolean mTouchInHotseat;
    private enum HotseatDragState {NONE, DRAG_IN, DRAG_OUT};
    private HotseatDragState mDragState = HotseatDragState.NONE;
    private View mDragedItemView;
    private boolean mAnimEnterRunning = false;
    private boolean mAnimLeftRunning = false;
    private int mLastTouchX = -1;
    private int mMoveDireciton = 0;//0:left; 1:right
    private int mXOffset;
    private ArrayList<View> mViewCacheList = null;
	private int workspaceCountX;
	private int mWGap;
    public static final int MAX_COUNT_X = 4;
    // cyl add for hotseat icon center end

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLauncher = Launcher.getLauncher(context);
        mHasVerticalHotseat = mLauncher.getDeviceProfile().isVerticalBarLayout();
        mBackgroundColor = ColorUtils.setAlphaComponent(
                ContextCompat.getColor(context, R.color.all_apps_container_color), 0);
        mBackground = new ColorDrawable(mBackgroundColor);
//        setBackground(mBackground);
    }

    public CellLayout getLayout() {
        return mContent;
    }

    /**
     * Returns whether there are other icons than the all apps button in the hotseat.
     */
    public boolean hasIcons() {
        return mContent.getShortcutsAndWidgets().getChildCount() > 1;
    }

    /**
     * Registers the specified listener on the cell layout of the hotseat.
     */
    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        mContent.setOnLongClickListener(l);
    }

    /* Get the orientation invariant order of the item in the hotseat for persistence. */
    int getOrderInHotseat(int x, int y) {
        return mHasVerticalHotseat ? (mContent.getCountY() - y - 1) : x;
    }

    /* Get the orientation specific coordinates given an invariant order in the hotseat. */
    int getCellXFromOrder(int rank) {
        return mHasVerticalHotseat ? 0 : rank;
    }

    int getCellYFromOrder(int rank) {
        return mHasVerticalHotseat ? (mContent.getCountY() - (rank + 1)) : 0;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        DeviceProfile grid = mLauncher.getDeviceProfile();
	// cyl add for hotseat icon center start	
		workspaceCountX = (int) grid.inv.numColumns; 
		mViewCacheList = new ArrayList<View>(MAX_COUNT_X);
	// cyl add for hotseat icon center end	
        mContent = (CellLayout) findViewById(R.id.layout);
        if (grid.isLandscape && !grid.isLargeTablet) {
            mContent.setGridSize(1, (int) grid.inv.numHotseatIcons);
        } else {
            mContent.setGridSize((int) grid.inv.numHotseatIcons, 1);
        }
        mContent.setIsHotseat(true);

        resetLayout();
    }

    void resetLayout() {
        mContent.removeAllViewsInLayout();

        if (!FeatureFlags.NO_ALL_APPS_ICON) {
            // Add the Apps button
            Context context = getContext();
            int allAppsButtonRank = mLauncher.getDeviceProfile().inv.getAllAppsButtonRank();

            LayoutInflater inflater = LayoutInflater.from(context);
            TextView allAppsButton = (TextView)
                    inflater.inflate(R.layout.all_apps_button, mContent, false);
            Drawable d = context.getResources().getDrawable(R.drawable.all_apps_button_icon);

            mLauncher.resizeIconDrawable(d);
            int scaleDownPx = getResources().getDimensionPixelSize(R.dimen.all_apps_button_scale_down);
            Rect bounds = d.getBounds();
            d.setBounds(bounds.left, bounds.top + scaleDownPx / 2, bounds.right - scaleDownPx,
                    bounds.bottom - scaleDownPx / 2);
            allAppsButton.setCompoundDrawables(null, d, null, null);

            allAppsButton.setContentDescription(context.getString(R.string.all_apps_button_label));
            allAppsButton.setOnKeyListener(new HotseatIconKeyEventListener());
            if (mLauncher != null) {
                mLauncher.setAllAppsButton(allAppsButton);
                allAppsButton.setOnTouchListener(mLauncher.getHapticFeedbackTouchListener());
                allAppsButton.setOnClickListener(mLauncher);
                allAppsButton.setOnLongClickListener(mLauncher);
                allAppsButton.setOnFocusChangeListener(mLauncher.mFocusHandler);
            }

            // Note: We do this to ensure that the hotseat is always laid out in the orientation of
            // the hotseat in order regardless of which orientation they were added
            int x = getCellXFromOrder(allAppsButtonRank);
            int y = getCellYFromOrder(allAppsButtonRank);
            CellLayout.LayoutParams lp = new CellLayout.LayoutParams(x, y, 1, 1);
            lp.canReorder = false;
            mContent.addViewToCellLayout(allAppsButton, -1, allAppsButton.getId(), lp, true);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // We don't want any clicks to go through to the hotseat unless the workspace is in
        // the normal state or an accessible drag is in progress.
        return mLauncher.getWorkspace().workspaceInModalState() &&
                !mLauncher.getAccessibilityDelegate().isInAccessibleDrag();
    }

    @Override
    public void fillInLaunchSourceData(View v, ItemInfo info, Target target, Target targetParent) {
        target.gridX = info.cellX;
        target.gridY = info.cellY;
        targetParent.containerType = LauncherLogProto.HOTSEAT;
    }

    public void updateColor(ExtractedColors extractedColors, boolean animate) {
        //lijun remove
        /*if (!mHasVerticalHotseat) {
            int color = extractedColors.getColor(ExtractedColors.HOTSEAT_INDEX, Color.TRANSPARENT);
            if (mBackgroundColorAnimator != null) {
                mBackgroundColorAnimator.cancel();
            }
            if (!animate) {
                setBackgroundColor(color);
            } else {
                mBackgroundColorAnimator = ValueAnimator.ofInt(mBackgroundColor, color);
                mBackgroundColorAnimator.setEvaluator(new ArgbEvaluator());
                mBackgroundColorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mBackground.setColor((Integer) animation.getAnimatedValue());
                    }
                });
                mBackgroundColorAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mBackgroundColorAnimator = null;
                    }
                });
                mBackgroundColorAnimator.start();
            }
            mBackgroundColor = color;
        }*/
    }

    public void setBackgroundTransparent(boolean enable) {
        if (enable) {
            mBackground.setAlpha(0);
        } else {
            mBackground.setAlpha(255);
        }
    }

    public int getBackgroundDrawableColor() {
        return mBackgroundColor;
    }

// cyl add for hotseat icon center start
     //generate invisible view for auto-replace
    private void generateInvisibleView() {
        int allAppsButtonRank = mLauncher.getDeviceProfile().inv.getAllAppsButtonRank();
        Context context = getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        mInvisibleView = (BubbleTextView)
                inflater.inflate(R.layout.app_icon, mContent, false);
        mInvisibleView.setAlpha(0);
        int x = 0;
        int y = getCellYFromOrder(allAppsButtonRank);
        CellLayout.LayoutParams lp = new CellLayout.LayoutParams(x,y,1,1);
        lp.canReorder = false;
        mInvisibleView.setLayoutParams(lp);
    }
	
    protected void onEnterHotseat(int touchX, final int screen, boolean fromHotset, int touchY) {
        mTouchInHotseat = true;
        final ShortcutAndWidgetContainer container = mContent.getShortcutsAndWidgets();
        int count = container.getChildCount();
        if(count < 0) {
            return;
        }
        if(!fromHotset && isFull()) {
            return;
        }
        if(fromHotset) {
 
            if(Math.abs(mLastTouchX - touchX) >= 5) {
                mMoveDireciton = mLastTouchX < touchX ? 1 : 0;
                mLastTouchX = touchX;
            }
            
            if((mDragState == HotseatDragState.DRAG_OUT && mDragedItemView != null)
                    || (mDragedItemView != null && mDragedItemView.getParent() == null && mDragState != HotseatDragState.DRAG_IN)) {
                //drag one item belongs to hotseat from workspace back to hotseat
//                mDragState = HotseatDragState.DRAG_SWAP;
                animateBackToHotseat(count, touchX, touchY, container);
            } else {
                //drag one item from hotseat to hotseat
                mDragState = HotseatDragState.DRAG_IN;
                animateSwap(touchX, touchY, screen, fromHotset);
            }
        } else {
            checkAnimateOnEnter(count, touchX, touchY, container);
        }
    }
    
    private void checkAnimateOnEnter(int childCount, int touchX, int touchY, ShortcutAndWidgetContainer container) {
        if(mAnimEnterRunning) {
            return;
        }
        //drag one item from workspace or folder
        int index = getAppropriateIndex(touchX);
        if(mCurrentInvisibleIndex != index || mDragState == HotseatDragState.NONE) {
            mInvisibleViewAdded = false;
          
            removeViewFromCacheList(mInvisibleView);
            mInvisibleView = null;
        }
       
        childCount = mViewCacheList.size();
        
        if(!mInvisibleViewAdded) {
            if(mInvisibleView == null) {
                generateInvisibleView();
            }
            if(index == 0 && childCount <= 1) {
                if(index < childCount) {
                   
                   addToCacheList(index, mInvisibleView);
                    
                } else {
                   
                    addToCacheList(-1, mInvisibleView);
                }
                mCurrentInvisibleIndex = 0;
            } else if(index == (childCount-1)) {
                //View view = container.getChildAt(index);
                View view = mViewCacheList.get(index);
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) view.getLayoutParams();
                int correctedX = getCorrectedX(lp.x, mMoveDireciton == 0);

                if(touchX < correctedX) {
                    addToCacheList(index, mInvisibleView);
                    mCurrentInvisibleIndex = index;
                } else {
                    if(mMoveDireciton == 0) {
                        addToCacheList(index, mInvisibleView);
                        mCurrentInvisibleIndex = index;
                    } else {
                        addToCacheList(-1, mInvisibleView);
                        //container.addView(mInvisibleView);
                        mCurrentInvisibleIndex = index + 1;
                    }
                }
            } else {

                addToCacheList(index, mInvisibleView);
                mCurrentInvisibleIndex = index;
            }
            mInvisibleViewAdded = true;
            //reLayout();
            mDragState = HotseatDragState.DRAG_IN;
            animateOnEnter(false);
        }
    }
    
    private int getCorrectedX (int leftX, boolean leftOrRight) {
        int cellW = mContent.getCellWidth();
        int centerX = leftX + cellW / 2;
        return leftOrRight ? centerX + mXOffset : centerX - mXOffset;
    }
    
    private void animateBackToHotseat(int childCount, int touchX, int touchY, ShortcutAndWidgetContainer container) {

        mDragState = HotseatDragState.DRAG_IN;
        checkAnimateOnEnter(childCount, touchX, touchY, container);
    }
    
    private void animateSwap(int touchX, int touchY, final int screen, boolean fromHotset) {
        final ShortcutAndWidgetContainer container = mContent.getShortcutsAndWidgets();
        //final int index = getSwapIndex(touchX, touchY);
        if(mDragedItemView == null) {
            mDragedItemView = mLauncher.getWorkspace().getDragInfo().cell;
            //container.removeView(mDragedItemView);
            //replace by mInvisibleView

            mViewCacheList.remove(mDragedItemView);
            if(mInvisibleView == null) {
                generateInvisibleView();
            }
            if(screen < mViewCacheList.size()) {
                mCurrentInvisibleIndex = screen;
                addToCacheList(mCurrentInvisibleIndex, mInvisibleView);
            } else {
                addToCacheList(-1, mInvisibleView);
                mCurrentInvisibleIndex = mViewCacheList.size() - 1;
            }
            fillViewsFromCache();
            mInvisibleViewAdded = true;

        }
        
        checkAnimateOnEnter(container.getChildCount(), touchX, touchY, container);
   }
    
    private void animateOnEnter(boolean fromHotseat) {
        //ShortcutAndWidgetContainer container = mContent.getShortcutsAndWidgets();
//        int count = container.getChildCount();
        int count = mViewCacheList.size();
        if(mAnimLeftRunning && mAnimatorSet != null) {
            mAnimatorSet.end();
        }
        
        if(fromHotseat) {
            
        } else {
            int right = getRight();
            int left = getLeft();
            
            Resources res = getResources();
            int width = right - left;
            width = width - getPaddingLeft() - getPaddingRight()
                    - mContent.getPaddingLeft() - mContent.getPaddingRight();

            int cellW = mContent.getCellWidth();
            int l = 0;
            int wGap = 0;
            int space = width - count * cellW;

            if (count >= workspaceCountX) {
                wGap = (int) (space / (float) (count - 1));
            } else {
                wGap = (int) (space / (float) (count + 1));
                l = wGap;
            }

            if(mAnimatorSet == null) {
                mAnimatorSet = new AnimatorSet();
            } else {
                mAnimatorSet.removeAllListeners();
                if(mAnimatorSet.isRunning()) {
                    mAnimatorSet.end();
                }
                clearAnimFlags();
            }
            
            ArrayList<Animator> items = new ArrayList<Animator>();
            int srcX = 0;
            int destX = 0;
            for (int i = 0; i < count; i++) {
                //View v = container.getChildAt(i);
                View v = mViewCacheList.get(i);
                if(v == null) {
                    continue;
                }
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v.getLayoutParams();
                srcX = lp.x;
                destX = l + lp.leftMargin;
                l += (cellW + wGap);
                if(v == mInvisibleView || srcX == destX) {
                    continue;
                }
//                ItemInfo info = (ItemInfo) v.getTag();
				if (mLauncher.isUninstallMode && (v instanceof UninstallMode.UninstallAnimation)) {
					UninstallMode.UninstallAnimation uninstallAnimation = (UninstallMode.UninstallAnimation) v;
					uninstallAnimation.stopShakeAnimation();
				}

                items.add(createAnimator(v, srcX, destX, null, null, true));
            }
            if(!items.isEmpty()) {
                mAnimatorSet.playTogether(items);
                
                mAnimatorSet.addListener(new AnimatorListenerAdapter() {
                    
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mAnimatorSet = null;
                        mAnimEnterRunning = false;
                        fillViewsFromCache();
                    }
                    
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mAnimEnterRunning = true;
                    }
                });
                mAnimatorSet.start();
            }
        }
    }
    
    private void clearAnimFlags() {
        mAnimEnterRunning = false;
        mAnimLeftRunning = false;
    }
    
    private void animateRestorePostion(final Runnable r) {

        ShortcutAndWidgetContainer container = mContent.getShortcutsAndWidgets();
        int count = container.getChildCount();
        ArrayList<View> visibleChild = new ArrayList<View>();
        for (int i = 0; i < count; i++) {
            final View child = container.getChildAt(i);
            if (child != mInvisibleView) {
                visibleChild.add(child);
            }
        }

        int right = getRight();
        int left = getLeft();
        int visibleCount = visibleChild.size();
        if (visibleCount > 0) {
            Resources res = getResources();
            int width = right - left;
            width = width - getPaddingLeft() - getPaddingRight()
                    - mContent.getPaddingLeft() - mContent.getPaddingRight();

            int cellW = mContent.getCellWidth();
            int l = 0;
            int wGap = 0;
            int space = width - visibleCount * cellW;

            if (visibleCount >= workspaceCountX) {
                wGap = (int) (space / (float) (visibleCount - 1));
            } else {
                wGap = (int) (space / (float) (visibleCount + 1));
                l = wGap;
            }

            if(mAnimatorSet == null) {
                mAnimatorSet = new AnimatorSet();
            } else {
                mAnimatorSet.removeAllListeners();
                if(mAnimatorSet.isRunning()) {
                    mAnimatorSet.end();
                }
                clearAnimFlags();
            }
            ArrayList<Animator> items = new ArrayList<Animator>();
            int srcX = 0;
            int destX = 0;
            for (int i = 0; i < visibleCount; i++) {
                View v = visibleChild.get(i);
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v
                        .getLayoutParams();
                srcX = (int)v.getX();
                destX = l + lp.leftMargin;
                items.add(createAnimator(v, srcX, destX, null, null, true));
                l += (cellW + wGap);
            }
            mAnimatorSet.playTogether(items);
            mAnimatorSet.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationEnd(Animator animation) {
                    r.run();
                    mAnimatorSet = null;
                }

                @Override
                public void onAnimationStart(Animator animation) {

                }
            });
            mAnimatorSet.start();
        }
    }
    
    private void animateLeftItems() {
        
        final ShortcutAndWidgetContainer container = mContent.getShortcutsAndWidgets();
        int count = container.getChildCount();
        ArrayList<View> leftChild = new ArrayList<View>();
        for (int i = 0; i < count; i++) {
            final View child = container.getChildAt(i);
            if (child != mDragedItemView && child != mInvisibleView) {
                leftChild.add(child);
            }
        }

        int right = getRight();
        int left = getLeft();
        int leftCount = leftChild.size();
        if (leftCount > 0) {
            Resources res = getResources();
            int width = right - left;
            width = width - getPaddingLeft() - getPaddingRight()
                    - mContent.getPaddingLeft() - mContent.getPaddingRight();

            int cellW = mContent.getCellWidth();
            int l = 0;
            int wGap = 0;
            int space = width - leftCount * cellW;

            if (leftCount >= workspaceCountX) {
                wGap = (int) (space / (float) (leftCount - 1));
            } else {
                wGap = (int) (space / (float) (leftCount + 1));
                l = wGap;
            }

            if(mAnimatorSet == null) {
                mAnimatorSet = new AnimatorSet();
            } else {
                mAnimatorSet.removeAllListeners();
                if(mAnimatorSet.isRunning()) {
                    mAnimatorSet.end();
                }
                clearAnimFlags();
            }
            
            ArrayList<Animator> items = new ArrayList<Animator>();
            int srcX = 0;
            for (int i = 0; i < leftCount; i++) {
                final View v = leftChild.get(i);
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v
                        .getLayoutParams();
                srcX = (int)v.getX();
                final int destX = l + lp.leftMargin;

                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        v.setX(destX);
                    }
                };
                l += (cellW + wGap);
                if(srcX == destX) {
                    continue;
                }
                items.add(createAnimator(v, srcX, destX, null, r, true));
            }
            mAnimatorSet.playTogether(items);
            mAnimatorSet.addListener(new AnimatorListenerAdapter() {
                
                @Override
                public void onAnimationEnd(Animator animation) {
                    if(mAnimLeftRunning) {
                         if (mLauncher.isUninstallMode && (mDragedItemView instanceof UninstallMode.UninstallAnimation)) {
                             UninstallMode.UninstallAnimation uninstallAnimation = (UninstallMode.UninstallAnimation) mDragedItemView;
                             uninstallAnimation.stopShakeAnimation();
                         }	
						stopShakeAnim(mDragedItemView);
                        container.removeView(mDragedItemView);
                        mViewCacheList.remove(mDragedItemView);
                        if(mInvisibleViewAdded && mDragState == HotseatDragState.DRAG_OUT) {
							stopShakeAnim(mInvisibleView);
                            container.removeView(mInvisibleView);
                            mViewCacheList.remove(mInvisibleView);
                            mInvisibleView = null;
                            mInvisibleViewAdded = false;
                            mCurrentInvisibleIndex = -1;
                        }
                    }
                    mAnimatorSet = null;
                    mAnimLeftRunning = false;

                    if(container.getChildCount() <= mViewCacheList.size()) {
                        fillViewsFromCache();
                    }
                }
                
                @Override
                public void onAnimationStart(Animator animation) {
                    mAnimLeftRunning = true;

                }
                
                @Override
                public void onAnimationCancel(Animator animation) {
                    mAnimLeftRunning = false;
                }
            });
            mAnimatorSet.start();
        } 
    }
    
    /**
     * drag exit hotseat
     */
    public void onExitHotseat(boolean fromHotseat) {

        if(mDragState == HotseatDragState.NONE || mDragState == HotseatDragState.DRAG_OUT) {
            return;
        }

        mTouchInHotseat = false;
        mDragState = HotseatDragState.DRAG_OUT;
        
        if(fromHotseat && mDragedItemView != null) {
            //drag one item belongs to hotseat to workspace
            animateLeftItems();
            return;
        }
        
        if(!fromHotseat && mInvisibleViewAdded) {
            Runnable r = new Runnable() {
                
                @Override
                public void run() {
                    //mContent.removeView(mInvisibleView);
                    mViewCacheList.remove(mInvisibleView);
                    mInvisibleView = null;
                    mInvisibleViewAdded = false;
                    mCurrentInvisibleIndex = -1;
                    fillViewsFromCache();
                }
            };
            animateRestorePostion(r);
        }
    }
    
    private void cleanAndReset() {
        mDragState = HotseatDragState.NONE;
        mInvisibleView = null;
        mInvisibleViewAdded = false;
        mCurrentInvisibleIndex = -1;
        mDragedItemView = null;
        //mAnimBackRunning = false;
        mAnimEnterRunning = false;
        mAnimLeftRunning = false;
        mAnimatorSet = null; 
        mLastTouchX = -1;
        updateItemCell();
        reLayout();
        updateItemInDatabase();
    }
    
    /**
     * drop dragItem in hotseat
     * @param success
     * @return
     */
    public int onDrop(boolean success, int touchX, final DragView dragView, final View cell,
                      final boolean removeDragView) {

        if(mDragState == HotseatDragState.NONE && !mInvisibleViewAdded){
		  return 0;
        }
        if(mAnimLeftRunning) {
            if (mAnimatorSet != null){
                mAnimatorSet.cancel();
            }
        } else {
            if(mAnimatorSet != null && mAnimatorSet.isRunning()) {
                mAnimatorSet.end();
            }
        }
        
        
        int index = mCurrentInvisibleIndex;
        final Runnable onDropEndRunnable = new Runnable() {
            @Override
            public void run() {
                if(cell != null && cell.getVisibility() != View.VISIBLE){					
                    cell.setVisibility(VISIBLE);					
                }
                if(dragView != null){
                    mLauncher.getDragController().onDeferredEndDrag(dragView);
                }

                if(mInvisibleViewAdded) {
                    //getContainer().removeView(mInvisibleView);
                    mViewCacheList.remove(mInvisibleView);
                    mInvisibleView = null;
                }
                cleanAndReset();
            }
        };
        
        if(!success || dragView == null) {
            onDropEndRunnable.run();
            return index;
        }
        Animator a = null;
        int srcX = (int) dragView.getX();
        int srcY = (int) dragView.getY();
        
        if(mDragState == HotseatDragState.DRAG_IN && mDragedItemView != null && mDragedItemView.getParent() ==null) {
            //drag one item belongs to hotseat to hotseat again

            if(mInvisibleViewAdded && mCurrentInvisibleIndex != -1) {
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams)mInvisibleView.getLayoutParams();
                int destX = lp.x;
                int destY = getLocationY(); 

                ItemInfo item = (ItemInfo)cell.getTag();
//                int order = getAppropriateIndex(lp.x);
//                int tmpindex = getOrderInHotseat(order, 0);
                 if(item.itemType == LauncherSettings.Favorites.ITEM_TYPE_FOLDER){
                   destX += 10;
				   destY += 10;
                 }else{
                     //liuzuo add for offset begin
                   destX += getDropOffsetX();
				   destY += getDropOffsetY();

                     //liuzuo add for offset end
                 }

                final Runnable r = new Runnable() {
                    
                    @Override
                    public void run() {
                        ShortcutAndWidgetContainer container = mContent.getShortcutsAndWidgets();
						if(mInvisibleView != null){
                         stopShakeAnim(mInvisibleView);							
                         container.removeView(mInvisibleView); 
                         mViewCacheList.remove(mInvisibleView); 
						}
                        if(cell != null && cell.getParent() != null) {
                            ViewGroup parent = (ViewGroup)cell.getParent();
						    stopShakeAnim(cell);
                            parent.removeView(cell);
                        }
                        int count = container.getChildCount();
                        if(mCurrentInvisibleIndex >= count) {
                            container.addView(cell);
                            mViewCacheList.add(cell);
							if(mLauncher.isUninstallMode && cell instanceof BubbleTextView){
							  ((BubbleTextView)cell).setUninstallModeFlag();
							}

                        } else if(mCurrentInvisibleIndex != -1){
                            container.addView(cell, mCurrentInvisibleIndex);
                            mViewCacheList.add(mCurrentInvisibleIndex, cell);
							if(mLauncher.isUninstallMode && cell instanceof BubbleTextView){
							  ((BubbleTextView)cell).setUninstallModeFlag();
							}	
                        }

                        onDropEndRunnable.run();
                    }
                };
                a = createDropAnimator(dragView, srcX, srcY, destX, destY, r);
                a.start();
            } else {
                onDropEndRunnable.run();
            }
            
            return index;
        } else {
            if(success) {
                if(mInvisibleViewAdded && mCurrentInvisibleIndex != -1) {
					stopShakeAnim(mInvisibleView);
                    mContent.getShortcutsAndWidgets().removeView(mInvisibleView);
                    mViewCacheList.remove(mInvisibleView);
                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams) mInvisibleView.getLayoutParams();

                   // ItemInfo item = (ItemInfo)cell.getTag();
//                    int order = mLauncher.getHotseat().getAppropriateIndex(lp.x);
//                    int tmpindex = getOrderInHotseat(order, 0);

                    a = createDropAnimator(dragView, srcX, srcY, lp.x + getDropOffsetX(), getLocationY()+ getDropOffsetY(), onDropEndRunnable);
                } else {
                    onDropEndRunnable.run();
                }
                if(a != null) {
                    a.start();
                }
            }
        }
        
        return index;
    }


    private Animator createAnimator(final View v, final int srcX, final int destX, 
            final Runnable onStartRunnable,final Runnable onEndRunnable, final boolean cleanTransX) {
        ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(v,
                PropertyValuesHolder.ofFloat(ViewHidePropertyName.X, srcX, destX));
        a.setDuration(200);
        a.setInterpolator(new LinearInterpolator());
        a.addListener(new AnimatorListenerAdapter(){

            @Override
            public void onAnimationEnd(Animator animation) {
                if(onEndRunnable != null) {
                    onEndRunnable.run();
                }
                v.setTranslationX(0);
            }
        });
        return a;
    }
    
    private Animator createDropAnimator(View v, int srcX, int srcY, int destX, int destY, final Runnable onDropEndRunnable) {
        ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(v,
                PropertyValuesHolder.ofFloat(ViewHidePropertyName.X, srcX, destX),
                PropertyValuesHolder.ofFloat(ViewHidePropertyName.Y, srcY, destY));

        a.setDuration(150);
        a.setInterpolator(new LinearInterpolator());
        a.addListener(new AnimatorListenerAdapter(){

            
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if(onDropEndRunnable != null) {
                    onDropEndRunnable.run();
                }
            }
        });
        return a;
    }
    
    //override onLayout to resize all children view in hotseat when drag icon in or out
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        reLayout(left, right, false);
        mAnimStartY = getTop();
        mAnimEndY = getBottom();
        super.onLayout(changed, left, top, right, bottom);
    }

    private void reLayout() {
        reLayout(getLeft(), getRight(), false);
    }

    private void reLayout(int left, int right, boolean unvisibleCount) {
        ShortcutAndWidgetContainer container = mContent.getShortcutsAndWidgets();
        int count = container.getChildCount();
        ArrayList<View> visibleChild = new ArrayList<View>();
        boolean flag = false;
        for (int i = 0; i < count; i++) {
            final View child = container.getChildAt(i);
          if (!mLauncher.getDragController().isDragging()
		    && child == mInvisibleView) {
		     flag = true;
		     continue;
	       }
            if (!unvisibleCount && child.getVisibility() != GONE) {
                visibleChild.add(child);
            }
        }
        if(flag) {
            updateItemCell();
        }

        int visibleCount = visibleChild.size();
        if (visibleCount > 0) {
            int width = right - left;
            width = width - getPaddingLeft() - getPaddingRight()
                    - mContent.getPaddingLeft() - mContent.getPaddingRight();


            int cellW = mContent.getCellWidth();
            mXOffset = 0;
            int l = 0;
            int space = width - visibleCount * cellW;


            // for 3*3 layout


            if (visibleCount >= workspaceCountX) {
                mWGap = (int) (space / (float) (visibleCount - 1));
            } else {
                mWGap = (int) (space / (float) (visibleCount + 1));
                l = mWGap;
            }

            boolean rtl = Utilities.isRtl(mLauncher.getResources());

            boolean textViewNeedPadding = false;
            if (visibleCount > workspaceCountX && mWGap < 0) {
                textViewNeedPadding = true;
            }

            for (int i = 0; i < visibleCount; i++) {
                View v = visibleChild.get(!rtl ? i : visibleCount - i - 1);
                //!!important, in some case, the view's TranslationX is not 0 on animation ended 
                v.setTranslationX(0);
                v.setTop(0);
                v.setBottom(mContent.getCellHeight());
         
                BubbleTextView btv;
                if (textViewNeedPadding) {
                    Context context = getContext();
                    int paddingLeftAndRight = (-mWGap + (int) context.getResources().getDimension(
                            R.dimen.dynamic_grid_workspace_hotseat_gap_offset)) / 2;
                            //R.dimen.textview_padding_in_hotseat)) / 2;
                    if (v instanceof BubbleTextView) {
                        btv = (BubbleTextView) v;
                       // btv.setTempPadding(paddingLeftAndRight); cyl del
                    } else if (v instanceof FolderIcon) {
                        FolderIcon fi = (FolderIcon) v;
                       // fi.setTempPadding(paddingLeftAndRight); cyl del
                    }
                } else {
                    if (v instanceof BubbleTextView) {
                        btv = (BubbleTextView) v;
                       // btv.resetTempPadding(); cyl del
                    } else if (v instanceof FolderIcon) {
                       // ((FolderIcon) v).resetTempPadding(); cyl del
                    }
                }

                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v
                        .getLayoutParams();
                lp.cellX = i;
                lp.x = l + lp.leftMargin;
                //ItemInfo info = (ItemInfo)v.getTag();
                l += (cellW + mWGap);
                //lp.startWithGap = (visibleCount < workspaceCountX); cyl del
            }
            mContent.setGridSize(cellW, mContent.getCellHeight(), mWGap, mContent.getHeightGap());
        }
        
        if(count > 0) {
            mContent.getShortcutsAndWidgets().invalidate();
        }
    }


    public int getAppropriateIndex(int dx) {
        int dockChildCount = mViewCacheList.size();
        int index = 0;

        if (dockChildCount == 0) {
            return index;
        }
        int cellWidth = mContent.getCellWidth();

        if (dockChildCount == 1) {
            View v = mViewCacheList.get(0);
            if (v == mInvisibleView) {
                return 0;
            }
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v.getLayoutParams();
            int centerX = lp.x + cellWidth / 2;
            return dx > centerX ? 1 : 0;
        }

        int minResult = Integer.MAX_VALUE;
        int minIndex = 0;
        View child = null;
        int correctedX = 0;
        CellLayout.LayoutParams lp = null; 
//        ItemInfo info = null;
        for (int i = 0; i < dockChildCount; i++) {
            child = mViewCacheList.get(i);
            lp = (CellLayout.LayoutParams) child.getLayoutParams();
            correctedX = getCorrectedX(lp.x, mMoveDireciton == 0);
            if (minResult > Math.abs(correctedX - dx)) {
                minResult = Math.abs(correctedX - dx);
                minIndex = i;
            }
        }
        return minIndex;
    }

    public void touchToPoint(int touchX, int[] topLeft, boolean fromHotseat, boolean toHotseat) {
        int dockChildCount = mViewCacheList.size();
        int cellWidth = mContent.getCellWidth();

        int minResult = Integer.MAX_VALUE;
        int minIndex = 0;
        View child = null;
        int centerX = 0;
        CellLayout.LayoutParams lp = null;
        for (int i = 0; i < dockChildCount; i++) {
            child = mViewCacheList.get(i);
            lp = (CellLayout.LayoutParams) child.getLayoutParams();
            centerX = lp.x + cellWidth / 2;
            if (minResult > Math.abs(centerX - touchX)) {
                minResult = Math.abs(centerX - touchX);
                minIndex = i;
            }
        }
        child = mViewCacheList.get(minIndex);
        if (child != null) {//FIXME
            lp = (CellLayout.LayoutParams) child.getLayoutParams();
            topLeft[0] = lp.x + mContent.getPaddingLeft();
            topLeft[1] = lp.y + mContent.getPaddingTop();
        }
    }

    public int getAppropriateLeft(int dx, boolean fromHotseat) {
        int index = getAppropriateIndex(dx);
        View child = mContent.getShortcutsAndWidgets().getChildAt(index);
        int left = 0;
        if (child != null) {
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
            left = lp.x;
        } else {
            left = 0;
        }
        return left;
    }

    protected boolean isFull() {
        if (mInvisibleViewAdded) { 
            return false;
        }
        if (mViewCacheList.size() > MAX_COUNT_X) {
            fillViewsFromCache();
        }
        return mContent.getShortcutsAndWidgets().getChildCount() >= MAX_COUNT_X;
    }

    /**
     * update hotseat items in database
     */
    public void updateItemInDatabase() {
        int count = mContent.getShortcutsAndWidgets().getChildCount();
        int container = LauncherSettings.Favorites.CONTAINER_HOTSEAT;

        for (int i = 0; i < count; i++) {
            View v = mContent.getShortcutsAndWidgets().getChildAt(i);
            ItemInfo info = (ItemInfo) v.getTag();
            // Null check required as the AllApps button doesn't have an item info
            if (info != null) {

                LauncherModel.modifyItemInDatabase(mLauncher, info, container, info.screenId, info.cellX,
                        info.cellY, info.spanX, info.spanY);
            }
        }
    }

    /**
     * update the screen and cellX of items in hotseat
     */
    public void updateItemCell() {
        //fillViewsFromCache();
        ShortcutAndWidgetContainer container = mContent.getShortcutsAndWidgets();
        int count = container.getChildCount();
        mViewCacheList.clear();

        //clear
        for (int i = 0; i < count; i++) {
            View v = container.getChildAt(i);
            if (v != null && v.getTag() != null) {
                mViewCacheList.add(v);
            }
        }

        count = mViewCacheList.size();
        for (int i = 0; i < count; i++) {
            View v = mViewCacheList.get(i);
            ItemInfo info = (ItemInfo) v.getTag();
            if (info == null) continue;
            info.cellX = i;
            info.cellY = 0;
            info.screenId  = i;
            info.container = LauncherSettings.Favorites.CONTAINER_HOTSEAT;
        }
        fillViewsFromCache();

    }


    protected Animator getHotseatAnimator(boolean hide) {
        int startY = hide ? mAnimStartY : mAnimEndY;
        int endY = hide ? mAnimEndY : mAnimStartY;
        ValueAnimator bounceAnim = ObjectAnimator.ofFloat(this, "y", startY, endY);
        bounceAnim.setDuration(300);
        bounceAnim.setInterpolator(new LinearInterpolator());
        return bounceAnim;
    }


    protected void revisibleHotseat() {
        setY(Math.min(mAnimStartY, mAnimEndY));
    }


    public boolean isTouchInHotseat() {
        return mTouchInHotseat;
    }

    public void onPause() {
        onExitHotseat(false);
    }

    public ShortcutAndWidgetContainer getContainer() {
        return mContent.getShortcutsAndWidgets();
    }

    public CellLayout getCellLayout() {
        return mContent;
    }

    public boolean checkDragitem(View view) {
        ItemInfo info = (ItemInfo) view.getTag();
        return view == mDragedItemView
                || (mDragedItemView == null &&
                info != null &&
                info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT);
    }

    private void relayoutViewCacheList() {
        int visibleCount = mViewCacheList.size();
        if (visibleCount > 0) {
            Resources res = getResources();
            int width = getRight() - getLeft();
            width = width - getPaddingLeft() - getPaddingRight()
                    - mContent.getPaddingLeft() - mContent.getPaddingRight();

            int wGap = 0;
            int cellW = mContent.getCellWidth();
            int l = 0;
            //int wGap = 0;
            int space = width - visibleCount * cellW;

            if (visibleCount >= workspaceCountX) {
                wGap = (int) (space / (float) (visibleCount - 1));
            } else {
                wGap = (int) (space / (float) (visibleCount + 1));
                l = wGap;
            }

            boolean rtl = Utilities.isRtl(mLauncher.getResources());
            for (int i = 0; i < visibleCount; i++) {
                View v = mViewCacheList.get(!rtl ? i : visibleCount - i - 1);
                if (v != mInvisibleView) {
                    l += (cellW + wGap);
                    continue;
                } else {
                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v
                            .getLayoutParams();
                    lp.x = l + lp.leftMargin;
                    return;
                }
            }
        }
    }

    private int getLocationY() {
        int[] location = new int[2];
        mContent.getLocationOnScreen(location);
        return location[1];
    }


    public void removeViewByItemInfo(ItemInfo info) {
        ShortcutAndWidgetContainer container = mContent.getShortcutsAndWidgets();
        int count = container.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = container.getChildAt(i);
            ItemInfo info1 = (ItemInfo) v.getTag();
            if (info == info1) {
                container.removeViewAt(i);
                break;
            }
        }
    }

    public void initViewCacheList() {
        mViewCacheList.clear();
        ShortcutAndWidgetContainer container = mContent.getShortcutsAndWidgets();
        int count = container.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = container.getChildAt(i);
            mViewCacheList.add(v);
        }
    }

    private void removeViewFromCacheList(View v) {
        mViewCacheList.remove(v);
    }

    private void addToCacheList(int index, View view) {
        if (index == -1 || index >= mViewCacheList.size()) {
            mViewCacheList.add(view);
        } else {
            mViewCacheList.add(index, view);
        }
        relayoutViewCacheList();
    }

  private void fillViewsFromCache() {
        ShortcutAndWidgetContainer container = mContent.getShortcutsAndWidgets();  
		int count = container.getChildCount();
        if(mLauncher.isUninstallMode && count > 0) {
            for(int k = 0; k < count; k++) {
                View child = container.getChildAt(k);
				stopShakeAnim(child);
            }
        }
        container.removeAllViews();
        for (View view : mViewCacheList) {
            if (view.getParent() != null) {
				stopShakeAnim(view);
                ((ViewGroup) view.getParent()).removeView(view);
            }
            container.addView(view);
            if (mLauncher.isUninstallMode && null != view && view instanceof UninstallMode.UninstallAnimation) {
                UninstallMode.UninstallAnimation uninstallAnimation = (UninstallMode.UninstallAnimation) view;
                uninstallAnimation.startShakeAnimation();
            }
        }
 }
  
 public void removeHotseatEmptyItem(){
	if(mAnimLeftRunning) {
		if (mAnimatorSet != null){
			mAnimatorSet.cancel();
		}
	} else {
		if(mAnimatorSet != null && mAnimatorSet.isRunning()) {
			mAnimatorSet.end();
		}
	}
	cleanAndReset();
	initViewCacheList();
 }

 public boolean notFull() {
 	if(mInvisibleView != null){
 	  return mContent.getShortcutsAndWidgets().getChildCount() <= MAX_COUNT_X;
 	}else{
	 return mContent.getShortcutsAndWidgets().getChildCount() < MAX_COUNT_X;
 	}
 }

  public boolean removeInvisibleView(){     
	ShortcutAndWidgetContainer container = mContent.getShortcutsAndWidgets();
	int count = container.getChildCount();
    for (int i = 0; i < count; i++) {
      View v = container.getChildAt(i);
        if (v != null) {
            if (v.getTag() == null) {
				stopShakeAnim(v);
                container.removeView(v);
                return true;
            } else if (v.getVisibility() == INVISIBLE) {
                return true;
            }
        }
    }
	
    if(mDragState != HotseatDragState.NONE){
	 return true;
    }
	
	return false;
 }

  public boolean getHotseatDragState(){

  if(mDragState == HotseatDragState.NONE){
     return false;
  }else{
     return true;
  }
}

 public void stopShakeAnim(View v){
  if (mLauncher.isUninstallMode && (null != v && v instanceof UninstallMode.UninstallAnimation)) {
     UninstallMode.UninstallAnimation uninstallAnimation = (UninstallMode.UninstallAnimation) v;
     uninstallAnimation.stopShakeAnimation();
   } 	
 }
 // cyl add for hotseat icon center end

    // liuzuo add for offset drop animation begin
private int  getDropOffsetY(){
    return getResources().getDrawable(R.drawable.ic_uninstall).getIntrinsicHeight()/2-14;
}
    private int  getDropOffsetX(){
        return getResources().getDrawable(R.drawable.ic_uninstall).getIntrinsicWidth()/2-2;
    }
    // liuzuo add for offset drop animation end
}
