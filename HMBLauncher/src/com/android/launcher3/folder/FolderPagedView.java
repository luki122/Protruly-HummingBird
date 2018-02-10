/**
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.launcher3.folder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.FastBitmapDrawable;
import com.android.launcher3.FocusHelper.PagedFolderKeyEventListener;
import com.android.launcher3.IconCache;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.PagedView;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace.ItemOperator;
import com.android.launcher3.colors.ColorManager;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.gesturetask.StatusBarAction;
import com.android.launcher3.keyboard.ViewGroupFocusHelper;
import com.android.launcher3.pageindicators.PageIndicator;
import com.android.launcher3.util.Thunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FolderPagedView extends PagedView {

    private static final String TAG = "FolderPagedView";

    private static final boolean ALLOW_FOLDER_SCROLL = true;

    private static final int REORDER_ANIMATION_DURATION = 230;
    private static final int START_VIEW_REORDER_DELAY = 30;
    private static final float VIEW_REORDER_DELAY_FACTOR = 0.9f;

    /**
     * Fraction of the width to scroll when showing the next page hint.
     */
    private static final float SCROLL_HINT_FRACTION = 0.07f;

    private static final int[] sTempPosArray = new int[2];

    public final boolean mIsRtl;

    private final LayoutInflater mInflater;
    private final IconCache mIconCache;
    private final ViewGroupFocusHelper mFocusIndicatorHelper;

    @Thunk final HashMap<View, Runnable> mPendingAnimations = new HashMap<>();

    @ViewDebug.ExportedProperty(category = "launcher")
    private final int mMaxCountX;
    @ViewDebug.ExportedProperty(category = "launcher")
    private final int mMaxCountY;
    @ViewDebug.ExportedProperty(category = "launcher")
    private final int mMaxItemsPerPage;

    private int mAllocatedContentSize;
    @ViewDebug.ExportedProperty(category = "launcher")
    private int mGridCountX;
    @ViewDebug.ExportedProperty(category = "launcher")
    private int mGridCountY;

    public Folder mFolder;
    private PagedFolderKeyEventListener mKeyListener;

    private PageIndicator mPageIndicator;
    //liuzuo : add for addIcon
    private TextView mAddIcon;
    private View.OnClickListener mOnClickListenerl;
    private int mWidthGap;
    AnimatorSet mIconAnima;
    //lijun add start
    final static float MAX_SWIPE_ANGLE = (float) Math.PI / 3;
    final static float START_DAMPING_TOUCH_SLOP_ANGLE = (float) Math.PI / 6;
    final static float TOUCH_SLOP_DAMPING_FACTOR = 4;
    private float mXDown;
    private float mYDown;
    //lijun add end
    public FolderPagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LauncherAppState app = LauncherAppState.getInstance();

        InvariantDeviceProfile profile = app.getInvariantDeviceProfile();
        mMaxCountX = 3/*profile.numFolderColumns*/;
        mMaxCountY = 3/*profile.numFolderRows*/;

        mMaxItemsPerPage = mMaxCountX * mMaxCountY;

        mInflater = LayoutInflater.from(context);
        mIconCache = app.getIconCache();

        mIsRtl = Utilities.isRtl(getResources());
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);

        setEdgeGlowColor(getResources().getColor(R.color.folder_edge_effect_color));
        mFocusIndicatorHelper = new ViewGroupFocusHelper(this);
    }

    public void setFolder(Folder folder) {
        mFolder = folder;
        mKeyListener = new PagedFolderKeyEventListener(folder);
        mPageIndicator = (PageIndicator) folder.findViewById(R.id.folder_page_indicator);
        initParentViews(folder);
        mPageIndicator.setpagedView(this);//lijun add
    }

    /**
     * Sets up the grid size such that {@param count} items can fit in the grid.
     * The grid size is calculated such that countY <= countX and countX = ceil(sqrt(count)) while
     * maintaining the restrictions of {@link #mMaxCountX} &amp; {@link #mMaxCountY}.
     */
    private void setupContentDimensions(int count) {
        mAllocatedContentSize = count;
     //change the UI of folder begin
/*        boolean done;
        if (count >= mMaxItemsPerPage) {
            mGridCountX = mMaxCountX;
            mGridCountY = mMaxCountY;
            done = true;
        } else {
            done = false;
        }

        while (!done) {
            int oldCountX = mGridCountX;
            int oldCountY = mGridCountY;
            if (mGridCountX * mGridCountY < count) {
                // Current grid is too small, expand it
                if ((mGridCountX <= mGridCountY || mGridCountY == mMaxCountY) && mGridCountX < mMaxCountX) {
                    mGridCountX++;
                } else if (mGridCountY < mMaxCountY) {
                    mGridCountY++;
                }
                if (mGridCountY == 0) mGridCountY++;
            } else if ((mGridCountY - 1) * mGridCountX >= count && mGridCountY >= mGridCountX) {
                mGridCountY = Math.max(0, mGridCountY - 1);
            } else if ((mGridCountX - 1) * mGridCountY >= count) {
                mGridCountX = Math.max(0, mGridCountX - 1);
            }
            done = mGridCountX == oldCountX && mGridCountY == oldCountY;
        }*/
        mGridCountX = mMaxCountX;
        mGridCountY = mMaxCountY;
        //change the UI of folder end
        // Update grid size
        for (int i = getPageCount() - 1; i >= 0; i--) {
            getPageAt(i).setGridSize(mGridCountX, mGridCountY);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        mFocusIndicatorHelper.draw(canvas);
        super.dispatchDraw(canvas);
    }

    /**
     * Binds items to the layout.
     * @return list of items that could not be bound, probably because we hit the max size limit.
     */
    public ArrayList<ShortcutInfo> bindItems(ArrayList<ShortcutInfo> items) {
        ArrayList<View> icons = new ArrayList<View>();
        ArrayList<ShortcutInfo> extra = new ArrayList<ShortcutInfo>();

        for (ShortcutInfo item : items) {
            if (!ALLOW_FOLDER_SCROLL && icons.size() >= mMaxItemsPerPage) {
                extra.add(item);
            } else {
                icons.add(createNewView(item));
            }
        }
        arrangeChildren(icons, icons.size(), false);
        return extra;
    }

    /**
     * Create space for a new item at the end, and returns the rank for that item.
     * Also sets the current page to the last page.
     */
    public int allocateRankForNewItem() {
        int rank = getItemCount();
        ArrayList<View> views = new ArrayList<>(mFolder.getItemsInReadingOrder());
        views.add(rank, null);
        arrangeChildren(views, views.size(), false);
        setCurrentPage(rank / mMaxItemsPerPage);
        return rank;
    }

    public View createAndAddViewForRank(ShortcutInfo item, int rank) {
        View icon = createNewView(item);
        addViewForRank(icon, item, rank);
        return icon;
    }

    /**
     * Adds the {@param view} to the layout based on {@param rank} and updated the position
     * related attributes. It assumes that {@param item} is already attached to the view.
     */
    public void addViewForRank(View view, ShortcutInfo item, int rank) {
        int pagePos = rank % mMaxItemsPerPage;
        int pageNo = rank / mMaxItemsPerPage;

        item.rank = rank;
        item.cellX = pagePos % mGridCountX;
        item.cellY = pagePos / mGridCountX;

        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) view.getLayoutParams();
        lp.cellX = item.cellX;
        lp.cellY = item.cellY;
        getPageAt(pageNo).addViewToCellLayout(
                view, -1, mFolder.mLauncher.getViewIdForItem(item), lp, true);
    }

    @SuppressLint("InflateParams")
    public View createNewView(ShortcutInfo item) {
        final BubbleTextView textView = (BubbleTextView) mInflater.inflate(
                R.layout.folder_application, null, false);
        textView.applyFromShortcutInfo(item, mIconCache);
        textView.setOnClickListener(mFolder);
        textView.setOnLongClickListener(mFolder);
        textView.setOnFocusChangeListener(mFocusIndicatorHelper);
        textView.setOnKeyListener(mKeyListener);
        textView.setCompoundDrawablePadding(Launcher.getLauncher(getContext()).getDeviceProfile().iconDrawablePaddingPx);

        textView.setLayoutParams(new CellLayout.LayoutParams(
                item.cellX, item.cellY, item.spanX, item.spanY));
        textView.setTextColor(ColorManager.getInstance().getColors()[0]);//lijun add for colormanager
        return textView;
    }

    @Override
    public CellLayout getPageAt(int index) {
        return (CellLayout) getChildAt(index);
    }

    public CellLayout getCurrentCellLayout() {
        return getPageAt(getNextPage());
    }

    public CellLayout createAndAddNewPage() {
        DeviceProfile grid = Launcher.getLauncher(getContext()).getDeviceProfile();
        CellLayout page = new CellLayout(getContext());
        page.setCellDimensions(grid.folderCellWidthPx, grid.folderCellHeightPx);
        page.getShortcutsAndWidgets().setMotionEventSplittingEnabled(false);
        page.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        page.setInvertIfRtl(true);
        page.setGridSize(mGridCountX, mGridCountY);

        addView(page, -1, generateDefaultLayoutParams());
        return page;
    }

    @Override
    protected int getChildGap() {
        return getPaddingLeft() + getPaddingRight();
    }

    public void setFixedSize(int width, int height) {
        width -= (getPaddingLeft() + getPaddingRight());
        height -= (getPaddingTop() + getPaddingBottom());
        for (int i = getChildCount() - 1; i >= 0; i --) {
            ((CellLayout) getChildAt(i)).setFixedSize(width, height);
        }
    }

    public void removeItem(View v) {
        for (int i = getChildCount() - 1; i >= 0; i --) {
            getPageAt(i).removeView(v);
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        mPageIndicator.setScroll(l, mMaxScrollX);
    }

    /**
     * Updates position and rank of all the children in the view.
     * It essentially removes all views from all the pages and then adds them again in appropriate
     * page.
     *
     * @param list the ordered list of children.
     * @param itemCount if greater than the total children count, empty spaces are left
     * at the end, otherwise it is ignored.
     *
     */
    public void arrangeChildren(ArrayList<View> list, int itemCount) {
        arrangeChildren(list, itemCount, true);
    }

    @SuppressLint("RtlHardcoded")
    private void arrangeChildren(ArrayList<View> list, int itemCount, boolean saveChanges) {
        ArrayList<CellLayout> pages = new ArrayList<CellLayout>();
        for (int i = 0; i < getChildCount(); i++) {
            CellLayout page = (CellLayout) getChildAt(i);
            page.removeAllViews();
            pages.add(page);
        }
        setupContentDimensions(itemCount);

        Iterator<CellLayout> pageItr = pages.iterator();
        CellLayout currentPage = null;

        int position = 0;
        int newX, newY, rank;

        rank = 0;
        for (int i = 0; i < itemCount; i++) {
            View v = list.size() > i ? list.get(i) : null;
            if (currentPage == null || position >= mMaxItemsPerPage) {
                // Next page
                if (pageItr.hasNext()) {
                    currentPage = pageItr.next();
                } else {
                    currentPage = createAndAddNewPage();
                }
                position = 0;
            }

            if (v != null) {
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) v.getLayoutParams();
                newX = position % mGridCountX;
                newY = position / mGridCountX;
                ItemInfo info = (ItemInfo) v.getTag();
                if (info.cellX != newX || info.cellY != newY || info.rank != rank) {
                    info.cellX = newX;
                    info.cellY = newY;
                    info.rank = rank;
                    if (saveChanges) {
                        LauncherModel.addOrMoveItemInDatabase(getContext(), info,
                                mFolder.mInfo.id, 0, info.cellX, info.cellY);
                    }
                }
                lp.cellX = info.cellX;
                lp.cellY = info.cellY;
                currentPage.addViewToCellLayout(
                        v, -1, mFolder.mLauncher.getViewIdForItem(info), lp, true);

                if (rank < FolderIcon.NUM_ITEMS_IN_PREVIEW && v instanceof BubbleTextView) {
                    ((BubbleTextView) v).verifyHighRes();
                }
            }

            rank ++;
            position++;
        }

        // Remove extra views.
        boolean removed = false;
        while (pageItr.hasNext()&&getChildCount()>1) {
            removeView(pageItr.next());
            removed = true;
        }
        if (removed) {
            setCurrentPage(0);
        }

        setEnableOverscroll(getPageCount() > 1);

        // Update footer
        mPageIndicator.setVisibility(getPageCount() > 1 ? View.VISIBLE : View.GONE);
        // Set the gravity as LEFT or RIGHT instead of START, as START depends on the actual text.
        /*mFolder.mFolderName.setGravity(getPageCount() > 1 ?
                (mIsRtl ? Gravity.RIGHT : Gravity.LEFT) : Gravity.CENTER_HORIZONTAL);*/
    }

    public int getDesiredWidth() {
        return getPageCount() > 0 ?
                (getPageAt(0).getDesiredWidth() + getPaddingLeft() + getPaddingRight()) : 0;
    }

    public int getDesiredHeight()  {
        return  getPageCount() > 0 ?
                (getPageAt(0).getDesiredHeight() + getPaddingTop() + getPaddingBottom()) : 0;
    }

    public int getItemCount() {
        int lastPageIndex = getChildCount() - 1;
        if (lastPageIndex < 0) {
            // If there are no pages, nothing has yet been added to the folder.
            return 0;
        }
        return getPageAt(lastPageIndex).getShortcutsAndWidgets().getChildCount()
                + lastPageIndex * mMaxItemsPerPage;
    }

    /**
     * @return the rank of the cell nearest to the provided pixel position.
     */
    public int findNearestArea(int pixelX, int pixelY) {
        int pageIndex = getNextPage();
        CellLayout page = getPageAt(pageIndex);
        page.findNearestArea(pixelX, pixelY, 1, 1, sTempPosArray);
        if (mFolder.isLayoutRtl()) {
            sTempPosArray[0] = page.getCountX() - sTempPosArray[0] - 1;
        }
        return Math.min(mAllocatedContentSize - 1,
                pageIndex * mMaxItemsPerPage + sTempPosArray[1] * mGridCountX + sTempPosArray[0]);
    }

    public boolean isFull() {
        return !ALLOW_FOLDER_SCROLL && getItemCount() >= mMaxItemsPerPage;
    }

    public View getFirstItem() {
        if (getChildCount() < 1 || getCurrentCellLayout() == null) {//lijun add getCurrentCellLayout()
            return null;
        }
        ShortcutAndWidgetContainer currContainer = getCurrentCellLayout().getShortcutsAndWidgets();
        if (mGridCountX > 0) {
            return currContainer.getChildAt(0, 0);
        } else {
            return currContainer.getChildAt(0);
        }
    }

    public View getLastItem() {
        if (getChildCount() < 1 || getCurrentCellLayout() == null) {
            return null;
        }
        ShortcutAndWidgetContainer currContainer = getCurrentCellLayout().getShortcutsAndWidgets();
        int lastRank = currContainer.getChildCount() - 1;
        if (mGridCountX > 0) {
            return currContainer.getChildAt(lastRank % mGridCountX, lastRank / mGridCountX);
        } else {
            return currContainer.getChildAt(lastRank);
        }
    }

    /**
     * Iterates over all its items in a reading order.
     * @return the view for which the operator returned true.
     */
    public View iterateOverItems(ItemOperator op) {
        for (int k = 0 ; k < getChildCount(); k++) {
            CellLayout page = getPageAt(k);
            for (int j = 0; j < page.getCountY(); j++) {
                for (int i = 0; i < page.getCountX(); i++) {
                    View v = page.getChildAt(i, j);
                    if ((v != null) && op.evaluate((ItemInfo) v.getTag(), v)) {
                        return v;
                    }
                }
            }
        }
        return null;
    }

    public String getAccessibilityDescription() {
        return getContext().getString(R.string.folder_opened, mGridCountX, mGridCountY);
    }

    /**
     * Sets the focus on the first visible child.
     */
    public void setFocusOnFirstChild() {
        if (getCurrentCellLayout() == null) return;//lijun add fixed NullPointException
        View firstChild = getCurrentCellLayout().getChildAt(0, 0);
        if (firstChild != null) {
            firstChild.requestFocus();
        }
    }

    @Override
    protected void notifyPageSwitchListener() {
        super.notifyPageSwitchListener();
        if (mFolder != null) {
            mFolder.updateTextViewFocus();
        }
    }

    /**
     * Scrolls the current view by a fraction
     */
    public void showScrollHint(int direction) {
        float fraction = (direction == DragController.SCROLL_LEFT) ^ mIsRtl
                ? -SCROLL_HINT_FRACTION : SCROLL_HINT_FRACTION;
        int hint = (int) (fraction * getWidth());
        int scroll = getScrollForPage(getNextPage()) + hint;
        int delta = scroll - getScrollX();
        if (delta != 0) {
            mScroller.setInterpolator(new DecelerateInterpolator());
            mScroller.startScroll(getScrollX(), 0, delta, 0, Folder.SCROLL_HINT_DURATION);
            invalidate();
        }
    }

    public void clearScrollHint() {
        if (getScrollX() != getScrollForPage(getNextPage())) {
            snapToPage(getNextPage());
        }
    }

    /**
     * Finish animation all the views which are animating across pages
     */
    public void completePendingPageChanges() {
        if (!mPendingAnimations.isEmpty()) {
            HashMap<View, Runnable> pendingViews = new HashMap<>(mPendingAnimations);
            for (Map.Entry<View, Runnable> e : pendingViews.entrySet()) {
                e.getKey().animate().cancel();
                e.getValue().run();
            }
        }
    }

    public boolean rankOnCurrentPage(int rank) {
        int p = rank / mMaxItemsPerPage;
        return p == getNextPage();
    }

    @Override
    protected void onPageBeginMoving() {
        super.onPageBeginMoving();
        getVisiblePages(sTempPosArray);
        for (int i = sTempPosArray[0]; i <= sTempPosArray[1]; i++) {
            verifyVisibleHighResIcons(i);
        }
    }

    /**
     * Ensures that all the icons on the given page are of high-res
     */
    public void verifyVisibleHighResIcons(int pageNo) {
        CellLayout page = getPageAt(pageNo);
        if (page != null) {
            ShortcutAndWidgetContainer parent = page.getShortcutsAndWidgets();
            for (int i = parent.getChildCount() - 1; i >= 0; i--) {
                ((BubbleTextView) parent.getChildAt(i)).verifyHighRes();
            }
        }
    }

    public int getAllocatedContentSize() {
        return mAllocatedContentSize;
    }

    /**
     * Reorders the items such that the {@param empty} spot moves to {@param target}
     */
    public void realTimeReorder(int empty, int target) {
        completePendingPageChanges();
        int delay = 0;
        float delayAmount = START_VIEW_REORDER_DELAY;

        // Animation only happens on the current page.
        int pageToAnimate = getNextPage();

        int pageT = target / mMaxItemsPerPage;
        int pagePosT = target % mMaxItemsPerPage;

        if (pageT != pageToAnimate) {
            Log.e(TAG, "Cannot animate when the target cell is invisible");
        }
        int pagePosE = empty % mMaxItemsPerPage;
        int pageE = empty / mMaxItemsPerPage;

        int startPos, endPos;
        int moveStart, moveEnd;
        int direction;

        if (target == empty) {
            // No animation
            return;
        } else if (target > empty) {
            // Items will move backwards to make room for the empty cell.
            direction = 1;

            // If empty cell is in a different page, move them instantly.
            if (pageE < pageToAnimate) {
                moveStart = empty;
                // Instantly move the first item in the current page.
                moveEnd = pageToAnimate * mMaxItemsPerPage;
                // Animate the 2nd item in the current page, as the first item was already moved to
                // the last page.
                startPos = 0;
            } else {
                moveStart = moveEnd = -1;
                startPos = pagePosE;
            }

            endPos = pagePosT;
        } else {
            // The items will move forward.
            direction = -1;

            if (pageE > pageToAnimate) {
                // Move the items immediately.
                moveStart = empty;
                // Instantly move the last item in the current page.
                moveEnd = (pageToAnimate + 1) * mMaxItemsPerPage - 1;

                // Animations start with the second last item in the page
                startPos = mMaxItemsPerPage - 1;
            } else {
                moveStart = moveEnd = -1;
                startPos = pagePosE;
            }

            endPos = pagePosT;
        }

        // Instant moving views.
        while (moveStart != moveEnd) {
            int rankToMove = moveStart + direction;
            int p = rankToMove / mMaxItemsPerPage;
            int pagePos = rankToMove % mMaxItemsPerPage;
            int x = pagePos % mGridCountX;
            int y = pagePos / mGridCountX;

            final CellLayout page = getPageAt(p);
            final View v = page.getChildAt(x, y);
            if (v != null) {
                if (pageToAnimate != p) {
                    page.removeView(v);
                    addViewForRank(v, (ShortcutInfo) v.getTag(), moveStart);
                } else {
                    // Do a fake animation before removing it.
                    final int newRank = moveStart;
                    final float oldTranslateX = v.getTranslationX();

                    Runnable endAction = new Runnable() {

                        @Override
                        public void run() {
                            mPendingAnimations.remove(v);
                            v.setTranslationX(oldTranslateX);
                            ((CellLayout) v.getParent().getParent()).removeView(v);
                            addViewForRank(v, (ShortcutInfo) v.getTag(), newRank);
                        }
                    };
                    v.animate()
                        .translationXBy((direction > 0 ^ mIsRtl) ? -v.getWidth() : v.getWidth())
                        .setDuration(REORDER_ANIMATION_DURATION)
                        .setStartDelay(0)
                        .withEndAction(endAction);
                    mPendingAnimations.put(v, endAction);
                }
            }
            moveStart = rankToMove;
        }

        if ((endPos - startPos) * direction <= 0) {
            // No animation
            return;
        }

        CellLayout page = getPageAt(pageToAnimate);
        for (int i = startPos; i != endPos; i += direction) {
            int nextPos = i + direction;
            View v = page.getChildAt(nextPos % mGridCountX, nextPos / mGridCountX);
            if (v != null) {
                ((ItemInfo) v.getTag()).rank -= direction;
            }
            if (page.animateChildToPosition(v, i % mGridCountX, i / mGridCountX,
                    REORDER_ANIMATION_DURATION, delay, true, true)) {
                delay += delayAmount;
                delayAmount *= VIEW_REORDER_DELAY_FACTOR;
            }
        }
    }

    public int itemsPerPage() {
        return mMaxItemsPerPage;
    }

    @Override
    protected void getEdgeVerticalPostion(int[] pos) {
        pos[0] = 0;
        pos[1] = getViewportHeight();
    }
    //liuzuo : add for addIcon begin
    public class FolderAddInfo extends ItemInfo{

        public FolderAddInfo(int x, int y) {
            cellX = x;
            cellY = y;
        }
    }
    public void showFolderAddView(boolean animated) {
        if (mAddIcon == null) {
            createFolderAddIconView();
        }
        Log.d(TAG,"state = "+mFolder.mLauncher.getState()+" xxxxxxxxx "+Folder.ICONARRANGING);
        if(/*!mFolder.mLauncher.isUninstallMode&&*/!mFolder.mLauncher.getImportMode()&&!mFolder.mLauncher.getDragController().isDragging()&&!Folder.ICONARRANGING )
            showAddView(animated);
    }
    public void showFolderAddView(boolean animated,Boolean add) {
        if (mAddIcon == null) {
            createFolderAddIconView();
        }
        Log.d(TAG,"state = "+mFolder.mLauncher.getState());
        if(/*!mFolder.mLauncher.isUninstallMode&&*/(!mFolder.mLauncher.getImportMode()&&add&&!Folder.ICONARRANGING))
            showAddView(animated);
    }
    private void createFolderAddIconView() {
        if (mAddIcon == null) {
            mAddIcon = (TextView) mInflater.inflate(R.layout.folder_application, this, false);
            Drawable d = getResources().getDrawable(R.drawable.ic_folder_add);
            Bitmap v = Utilities.createIconBitmap(d, getContext());
            mAddIcon.setCompoundDrawablesWithIntrinsicBounds(null,
                    new FastBitmapDrawable(v), null, null);
            mAddIcon.setCompoundDrawablePadding(Launcher.getLauncher(getContext()).getDeviceProfile().iconDrawablePaddingPx);
            mAddIcon.setText(R.string.folder_import_addicon);
            FolderAddInfo folderCell = new FolderAddInfo(-1, -1);
            mAddIcon.setTag(folderCell);
            mAddIcon.setOnClickListener(mOnClickListenerl);
            mAddIcon.setTextColor(ColorManager.getInstance().getColors()[0]);
            this.setOnClickListener(mOnClickListenerl);
            mAddIcon.setLayoutParams(new CellLayout.LayoutParams(
                    folderCell.cellX, folderCell.cellY, folderCell.spanX, folderCell.spanY));
        }

    }
    public void setFolderClickListener(View.OnClickListener listener){
        mOnClickListenerl = listener;
    }

    private void showAddView(boolean animated) {
        Log.d(TAG,"showAddView");
        if(mAddIcon != null){
            FolderAddInfo info = (FolderAddInfo) mAddIcon.getTag();
            onlyDetachAddView(false);
            if(animated) {
                AnimatorSet animatorSet = LauncherAnimUtils.createAnimatorSet();
                if(mIconAnima!=null&&mIconAnima.isRunning()) {
                    mIconAnima.cancel();
                }

                mIconAnima = animatorSet;
                createAddIconAnimation(mAddIcon, mIconAnima);
                mAddIcon.setVisibility(INVISIBLE);
                mIconAnima.start();
            }else {
                mAddIcon.setAlpha(1.0f);
            }
            mFolder.mItemsInvalidated=true;
            addViewForRank(mAddIcon,info,allocateRankForNewItemNotSetCurrentPage());
            mFolder.setFolderBackground(false);
            // mFolder.centerAboutIcon();
        }
    }

    public void deleteCellLayout(boolean removelayout){
        if(mAddIcon != null && mAddIcon.getParent() != null) {
            CellLayout oldLayout = (CellLayout) mAddIcon.getParent().getParent();
            oldLayout.onlyRemoveView(mAddIcon);
            if(removelayout) {
                removeView(oldLayout);
            } else {
                FolderAddInfo info = (FolderAddInfo) mAddIcon.getTag();
                if(info != null && info.cellX == 0 && info.cellY == 0){
                    removeView(oldLayout);
                }
            }
        }
    }

    public void hideAddView(boolean animated){
        if(mAddIcon != null){
            FolderAddInfo info = (FolderAddInfo) mAddIcon.getTag();
            if(info != null && info.cellX == 0 && info.cellY == 0){
                deleteCellLayout(true);
            } else {
                onlyDetachAddView(false);
            }
            mFolder.setFolderBackground(true);
            mPageIndicator.setVisibility(getPageCount() > 1 ? VISIBLE : GONE);
        }
    }
    public void onlyDetachAddView(boolean animated){
        Log.d(TAG,"onlyDetachAddView");
        int rank = 0;
        if(animated) {
            AnimatorSet animatorSet = LauncherAnimUtils.createAnimatorSet();
            if(mIconAnima!=null&&mIconAnima.isRunning())
                mIconAnima.cancel();
            rank = getItemCount()-1;
            mIconAnima = animatorSet;
            removeAddIconAnimation(mAddIcon, mIconAnima);
            mIconAnima.start();

        }else {
            removeItem(mAddIcon);
            rank =getItemCount();
        }
        setupContentDimensions(rank);

    }
    public void onAddInfo(ArrayList<ShortcutInfo> items) {
        Log.d(TAG,"onAddIconInfo"+items.size());
        AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
        for (int i = 0; i < items.size(); i++) {
            ShortcutInfo child =  items.get(i);
            View icon=createAndAddViewForRank(child, allocateRankForNewItem());
            createAddAppAnimation(icon,anim);
            mFolder.mItemsInvalidated = true;
            LauncherModel.addOrMoveItemInDatabase(
                    mFolder.mLauncher, child, mFolder.getInfo().id, 0, child.cellX, child.cellY);
        }
        // anim.start();
    }



    private AnimatorSet createAddAppAnimation(View icon, AnimatorSet anim) {
        Resources resources = getResources();
        Animator iconAlpha = ObjectAnimator.ofFloat(icon, "alpha", 0f, 1f);
        iconAlpha.setDuration(resources.getInteger(R.integer.folder_import_icon_duration));
        iconAlpha.setStartDelay(resources.getInteger(R.integer.folder_import_icon_delay));
        iconAlpha.setInterpolator(new AccelerateInterpolator(1.5f));
        anim.play(iconAlpha);
        return anim;
    }


    private void createAddIconAnimation(final TextView folderAddIcon, AnimatorSet mIconAnimat) {
        Animator iconAlpha = ObjectAnimator.ofFloat(folderAddIcon, "alpha", 0f, 1f);
        iconAlpha.setDuration(getResources().getInteger(R.integer.folder_add_icon_duration));
        iconAlpha.setStartDelay(getResources().getInteger(R.integer.folder_import_icon_delay));
        iconAlpha.setInterpolator(new AccelerateInterpolator(1.5f));
        iconAlpha.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                folderAddIcon.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mIconAnimat.play(iconAlpha);
    }


    public TextView getFolderAddIcon() {
        if (mAddIcon==null)
            createFolderAddIconView();
        return mAddIcon;
    }
    private void removeAddIconAnimation(final TextView folderAddIcon, AnimatorSet mIconAnimat) {
        Animator iconAlpha = ObjectAnimator.ofFloat(folderAddIcon, "alpha", 1f, 0f);
        iconAlpha.setDuration(getResources().getInteger(R.integer.folder_add_icon_remove_duration));
        iconAlpha.setInterpolator(new AccelerateInterpolator(1.5f));
        iconAlpha.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mAddIcon.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                removeItem(mAddIcon);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                removeItem(mAddIcon);
            }
        });
        mIconAnimat.play(iconAlpha);
    }
    public void ainmationCancel(){
        if(mIconAnima!=null&&mIconAnima.isRunning())
            mIconAnima.cancel();
    }


    public void addViewForRank(View view, ItemInfo item, int rank) {
        int pagePos = rank % mMaxItemsPerPage;
        int pageNo = rank / mMaxItemsPerPage;

        item.rank = rank;
        item.cellX = pagePos % mGridCountX;
        item.cellY = pagePos / mGridCountX;

        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) view.getLayoutParams();
        lp.cellX = item.cellX;
        lp.cellY = item.cellY;
        getPageAt(pageNo).addViewToCellLayout(
                view, -1, mFolder.mLauncher.getViewIdForItem(item), lp, true);
    }
    public int allocateRankForNewItemNotSetCurrentPage() {
        int rank = getItemCount();
        ArrayList<View> views = new ArrayList<>(mFolder.getItemsInReadingOrder());
        views.add(rank, null);
        arrangeChildren(views, views.size(), false);
        return rank;
    }
    //liuzuo : add for addIcon end
    public void setLastPage(){
        setCurrentPage(getItemCount()/mMaxItemsPerPage);
    }

    public int  getLastPageIconCount(){
        return getItemCount()%mMaxItemsPerPage;
    }

    /**
     * lijun add for uprelease arrangeicon
     * @param ev
     */
    @Override
    protected void determineScrollingStart(MotionEvent ev) {

        float deltaX = ev.getX() - mXDown;
        float absDeltaX = Math.abs(deltaX);
        float absDeltaY = Math.abs(ev.getY() - mYDown);

        if (Float.compare(absDeltaX, 0f) == 0) return;

        float slope = absDeltaY / absDeltaX;
        float theta = (float) Math.atan(slope);

        if (theta > MAX_SWIPE_ANGLE) {
            // Above MAX_SWIPE_ANGLE, we don't want to ever start scrolling the workspace
            if(deltaX<0){
                Log.i("StatusBarAction","StatusBarAction collapse statusbar begin.");
                StatusBarAction.collapsingNotification(this.getContext());
            }
            return;
        } else if (theta > START_DAMPING_TOUCH_SLOP_ANGLE) {
            // Above START_DAMPING_TOUCH_SLOP_ANGLE and below MAX_SWIPE_ANGLE, we want to
            // increase the touch slop to make it harder to begin scrolling the workspace. This
            // results in vertically scrolling widgets to more easily. The higher the angle, the
            // more we increase touch slop.
            theta -= START_DAMPING_TOUCH_SLOP_ANGLE;
            float extraRatio = (float)
                    Math.sqrt((theta / (MAX_SWIPE_ANGLE - START_DAMPING_TOUCH_SLOP_ANGLE)));
            super.determineScrollingStart(ev, 1 + TOUCH_SLOP_DAMPING_FACTOR * extraRatio);
        } else {
            // Below START_DAMPING_TOUCH_SLOP_ANGLE, we don't do anything special
            super.determineScrollingStart(ev);
        }
    }

    /**
     * lijun add for uprelease arrangeicon
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mXDown = ev.getX();
                mYDown = ev.getY();
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
        }
        return super.onInterceptTouchEvent(ev);
    }
    protected boolean isNormalState(){return false;} // cyl add for cycle slide
}
