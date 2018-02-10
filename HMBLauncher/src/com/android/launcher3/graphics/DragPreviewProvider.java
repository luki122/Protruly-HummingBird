/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.launcher3.graphics;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.HolographicOutlineHelper;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.PreloadIconDrawable;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.config.ProviderConfig;
import com.android.launcher3.folder.FolderIcon;

/**
 * A utility class to generate preview bitmap for dragging.
 */
public class DragPreviewProvider {

    public static final int DRAG_BITMAP_PADDING = 2;

    private final Rect mTempRect = new Rect();

    protected final View mView;

    // The padding added to the drag view during the preview generation.
    public final int previewPadding;

    public Bitmap gerenatedDragOutline;



    private  int mUninstallDrawableSize;

    public DragPreviewProvider(View view) {
        mView = view;

        if (mView instanceof TextView) {
            //liuzuo add begin
            if (view instanceof BubbleTextView) {
                Drawable uninstallDrawable = getUninstallDrawable((BubbleTextView) view);
                mUninstallDrawableSize = uninstallDrawable == null ? 0 : uninstallDrawable.getIntrinsicWidth();
            }
            //liuzuo add end
            Drawable d = Workspace.getTextViewIcon((TextView) mView);
            Rect bounds = getDrawableBounds(d);
            previewPadding = DRAG_BITMAP_PADDING - bounds.left - bounds.top;
        } else {
            previewPadding = DRAG_BITMAP_PADDING;
        }
    }

    private Drawable getUninstallDrawable(BubbleTextView view) {
        Drawable mMarkDrawable ;
        if (view.getTag() instanceof ShortcutInfo) {
            ShortcutInfo info = (ShortcutInfo) view.getTag();
            if (Utilities.isSystemApp(LauncherAppState.getInstance().getContext(), info.getIntent()) && info.itemType == 0) {
                return null;
            }
            final Resources res = LauncherAppState.getInstance().getContext().getResources();
            int widthOfmCheckDrawable;
            mMarkDrawable = res.getDrawable(R.drawable.ic_uninstall);
            widthOfmCheckDrawable = mMarkDrawable.getIntrinsicWidth();
            int left = -widthOfmCheckDrawable / 2 - 1;
            int top;
            top = -widthOfmCheckDrawable / 2+5;
            int right = left + widthOfmCheckDrawable;
            int button = top + widthOfmCheckDrawable;
            if (right > view.getWidth()) {
                left = view.getWidth() - widthOfmCheckDrawable;
                right = view.getWidth();
            } else {

            }
            mMarkDrawable.setBounds(left, top, right, button);

            return mMarkDrawable;
        } else {
            return null;
        }
    }

    private void drawUninstallDrawable(Drawable drawable, Canvas canvas) {
        if (drawable != null) {
            canvas.save();
            canvas.translate(mUninstallDrawableSize /2, mUninstallDrawableSize /2);
            drawable.draw(canvas);
            canvas.restore();
        }
    }
    public  void drawDragViewUninstallDrawable(Canvas canvas){
        if(mView instanceof  BubbleTextView) {
            drawUninstallDrawable(getUninstallDrawable((BubbleTextView) mView), canvas);
        }
    }
    /**
     * Draws the {@link #mView} into the given {@param destCanvas}.
     */
    private void drawDragView(Canvas destCanvas) {
        destCanvas.save();
        if (mView instanceof TextView) {
            Drawable d = Workspace.getTextViewIcon((TextView) mView);
            Rect bounds = getDrawableBounds(d);
            destCanvas.translate(DRAG_BITMAP_PADDING / 2 - bounds.left,
                    DRAG_BITMAP_PADDING / 2 - bounds.top);
            d.draw(destCanvas);
        } else {
            final Rect clipRect = mTempRect;
            mView.getDrawingRect(clipRect);

            boolean textVisible = false;
            if (mView instanceof FolderIcon) {
                // For FolderIcons the text can bleed into the icon area, and so we need to
                // hide the text completely (which can't be achieved by clipping).
                if (((FolderIcon) mView).getTextVisible()) {
                    ((FolderIcon) mView).setTextVisible(false);
                    textVisible = true;
                }
            }
            destCanvas.translate(-mView.getScrollX() + DRAG_BITMAP_PADDING / 2,
                    -mView.getScrollY() + DRAG_BITMAP_PADDING / 2);
            destCanvas.clipRect(clipRect, Op.REPLACE);
            mView.draw(destCanvas);

            // Restore text visibility of FolderIcon if necessary
            if (textVisible) {
                ((FolderIcon) mView).setTextVisible(true);
            }
        }
        destCanvas.restore();
    }

    /**
     * Returns a new bitmap to show when the {@link #mView} is being dragged around.
     * Responsibility for the bitmap is transferred to the caller.
     */
    public Bitmap createDragBitmap(Canvas canvas) {
        Bitmap b;

        if (mView instanceof TextView) {
            Drawable d = Workspace.getTextViewIcon((TextView) mView);
            Rect bounds = getDrawableBounds(d);
            b = Bitmap.createBitmap(bounds.width() + DRAG_BITMAP_PADDING,
                    bounds.height() + DRAG_BITMAP_PADDING, Bitmap.Config.ARGB_8888);
        } else {
            b = Bitmap.createBitmap(mView.getWidth() + DRAG_BITMAP_PADDING,
                    mView.getHeight() + DRAG_BITMAP_PADDING, Bitmap.Config.ARGB_8888);
        }

        canvas.setBitmap(b);
        drawDragView(canvas);
        canvas.setBitmap(null);

        return b;
    }

    public final void generateDragOutline(Canvas canvas) {
        if (ProviderConfig.IS_DOGFOOD_BUILD && gerenatedDragOutline != null) {
            throw new RuntimeException("Drag outline generated twice");
        }

        gerenatedDragOutline = createDragOutline(canvas);
    }

    /**
     * Returns a new bitmap to be used as the object outline, e.g. to visualize the drop location.
     * Responsibility for the bitmap is transferred to the caller.
     */
    public Bitmap createDragOutline(Canvas canvas) {
        final Bitmap b = Bitmap.createBitmap(mView.getWidth() + DRAG_BITMAP_PADDING,
                mView.getHeight() + DRAG_BITMAP_PADDING, Bitmap.Config.ARGB_8888);//liuzuo ARGB_8888 >> ALPHA_8
        canvas.setBitmap(b);
        drawDragView(canvas);
        HolographicOutlineHelper.obtain(mView.getContext())
                .applyExpensiveOutlineWithBlur(b, canvas);
        canvas.setBitmap(null);
        return b;
    }

    protected  Rect getDrawableBounds(Drawable d) {
        Rect bounds = new Rect();
        d.copyBounds(bounds);
        if (bounds.width() == 0 || bounds.height() == 0) {
            bounds.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        } else {
            bounds.offsetTo(0, 0);
        }
        if (d instanceof PreloadIconDrawable) {
            int inset = -((PreloadIconDrawable) d).getOutset();
            bounds.inset(inset, inset);
        }
        //liuzuo add begin
        bounds.left += -mUninstallDrawableSize /2;
        bounds.top += -mUninstallDrawableSize /2;
        bounds.bottom+= mUninstallDrawableSize /2;
        bounds.right+= mUninstallDrawableSize /2;
        //liuzuo add end
        return bounds;
    }

    public float getScaleAndPosition(Bitmap preview, int[] outPos) {
        float scale = Launcher.getLauncher(mView.getContext())
                .getDragLayer().getLocationInDragLayer(mView, outPos);
        outPos[0] = Math.round(outPos[0] - (preview.getWidth() - scale * mView.getWidth()) / 2);
        outPos[1] = Math.round(outPos[1] - (1 - scale) * preview.getHeight() / 2 - previewPadding / 2);
        return scale;
    }
    public  int getUninstallDrawableSize() {
        return mUninstallDrawableSize;
    }
}
