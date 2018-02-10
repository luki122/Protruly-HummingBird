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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region.Op;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.dlauncher.badge.BadgeController;
import com.android.launcher3.AppInfo;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.HolographicOutlineHelper;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.PreloadIconDrawable;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.config.FeatureFlags;
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

    private boolean mIsUnread;

    private  int mUninstallDrawableSize;

    public DragPreviewProvider(View view) {
        mView = view;

        if (mView instanceof TextView) {
            //liuzuo add begin
            if (view instanceof BubbleTextView) {
                int unreadWidth = getUnreadWidth();
                int uninstallWidth = getUninstallDrawable().getIntrinsicWidth();
                mUninstallDrawableSize = uninstallWidth > unreadWidth ?uninstallWidth : unreadWidth;
               /* if(((BubbleTextView)mView).isArrayMode()){
                    mUninstallDrawableSize = 0;
                }*/
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
            int widthOfmCheckDrawable;
            mMarkDrawable = getUninstallDrawable();
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
    private Drawable getUninstallDrawable(){
        return LauncherAppState.getInstance().getContext().getResources().getDrawable(R.drawable.ic_uninstall);
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
            float offsetX = 0;
            float offsetY = 0;
          /*  if(!isNormal){
                int [] ints =new int[2];
                int width = bounds.width()-mUninstallDrawableSize-DRAG_BITMAP_PADDING;
                int height = bounds.height()-mUninstallDrawableSize-DRAG_BITMAP_PADDING;

                float scaleAndPosition = getScaleAndPosition(ints);
                Log.d("liuzuo","x="+ints[0]+"        y= "+ints[1]+"    scaleAndPosition="+scaleAndPosition+"   getX="+mView.getX()+"  getY="+mView.getY());
                offsetX = (ints[0]-mView.getX())/2-width*(1-scaleAndPosition)/2;
                offsetY = (ints[1]-mView.getY())/2-height*(1-scaleAndPosition)/2;
            }
*/
            destCanvas.translate(DRAG_BITMAP_PADDING / 2 - bounds.left+offsetX,
                    DRAG_BITMAP_PADDING / 2 - bounds.top+offsetY);
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
        if(mIsUnread) {
            drawUnreadForDragView(destCanvas, mView);//lijun add for unread
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
    public Bitmap createDragBitmap(Canvas canvas,boolean isUnread) {
        mIsUnread = isUnread;
        return createDragBitmap(canvas);
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
    public float getScaleAndPosition( int[] outPos) {
        float scale = Launcher.getLauncher(mView.getContext())
                .getDragLayer().getLocationInDragLayer(mView, outPos);
        outPos[0] = Math.round(outPos[0] - (mView.getWidth() + DRAG_BITMAP_PADDING- scale * mView.getWidth()) / 2);
        outPos[1] = Math.round(outPos[1] - (1 - scale) * (mView.getHeight() + DRAG_BITMAP_PADDING) / 2 - previewPadding / 2);
        return scale;
    }
    public  int getUninstallDrawableSize() {
        return getUninstallDrawable().getIntrinsicWidth();
    }
    private int getUnreadWidth(){
        int unreadBgWidth = 0;
        ItemInfo info = (ItemInfo) mView.getTag();
        if (info != null && info.unreadNum > 0) {
            Resources res = mView.getContext().getResources();

            /// M: Meature sufficent width for unread text and background image
            Paint unreadTextNumberPaint = new Paint();
            unreadTextNumberPaint.setTextSize(res.getDimension(R.dimen.unread_text_number_size));
            unreadTextNumberPaint.setTypeface(Typeface.DEFAULT_BOLD);
            unreadTextNumberPaint.setTextAlign(Paint.Align.CENTER);

            Paint unreadTextPlusPaint = new Paint(unreadTextNumberPaint);
            unreadTextPlusPaint.setTextSize(res.getDimension(R.dimen.unread_text_plus_size));
            Paint unreadBgPaint = new Paint(unreadTextNumberPaint);
            unreadBgPaint.setColor(Color.RED);
            String unreadTextNumber;
            String unreadTextPlus = "+";
            Rect unreadTextNumberBounds = new Rect(0, 0, 0, 0);
            Rect unreadTextPlusBounds = new Rect(0, 0, 0, 0);
            if (info.unreadNum > BadgeController.MAX_UNREAD_COUNT) {
                unreadTextNumber = String.valueOf(BadgeController.MAX_UNREAD_COUNT);
                unreadTextPlusPaint.getTextBounds(unreadTextPlus, 0,
                        unreadTextPlus.length(), unreadTextPlusBounds);
            } else {
                unreadTextNumber = String.valueOf(info.unreadNum);
            }
            unreadTextNumberPaint.getTextBounds(unreadTextNumber, 0,
                    unreadTextNumber.length(), unreadTextNumberBounds);
            int textHeight = unreadTextNumberBounds.height();
            int textWidth = unreadTextNumberBounds.width() + unreadTextPlusBounds.width();

            /// M: Draw unread background image.
            NinePatchDrawable unreadBgNinePatchDrawable =
                    (NinePatchDrawable) res.getDrawable(R.drawable.ic_newevents_numberindication);
               unreadBgWidth = unreadBgNinePatchDrawable.getIntrinsicWidth();

            int unreadMinWidth = (int) res.getDimension(R.dimen.unread_minWidth);
            if (unreadBgWidth < unreadMinWidth) {
                unreadBgWidth = unreadMinWidth;
            }
            int unreadTextMargin = (int) res.getDimension(R.dimen.unread_text_margin);
            if (unreadBgWidth < textWidth + unreadTextMargin) {
                unreadBgWidth = textWidth + unreadTextMargin;
            }
        }
        return unreadBgWidth;
    }

    /**
     * lijun add for unread
     */
    private void drawUnreadForDragView(Canvas destCanvas, View icon) {
        if (!FeatureFlags.UNREAD_ENABLE) return;
        ItemInfo info = (ItemInfo) mView.getTag();
        if (info != null && info.unreadNum > 0) {
            Resources res = mView.getContext().getResources();

            /// M: Meature sufficent width for unread text and background image
            Paint unreadTextNumberPaint = new Paint();
            unreadTextNumberPaint.setTextSize(res.getDimension(R.dimen.unread_text_number_size));
            unreadTextNumberPaint.setTypeface(Typeface.DEFAULT_BOLD);
            unreadTextNumberPaint.setColor(0xffffffff);
            unreadTextNumberPaint.setTextAlign(Paint.Align.CENTER);
            unreadTextNumberPaint.setAntiAlias(true);

            Paint unreadTextPlusPaint = new Paint(unreadTextNumberPaint);
            unreadTextPlusPaint.setTextSize(res.getDimension(R.dimen.unread_text_plus_size));
            Paint unreadBgPaint = new Paint(unreadTextNumberPaint);
            unreadBgPaint.setColor(Color.RED);
            String unreadTextNumber;
            String unreadTextPlus = "+";
            Rect unreadTextNumberBounds = new Rect(0, 0, 0, 0);
            Rect unreadTextPlusBounds = new Rect(0, 0, 0, 0);
            if (info.unreadNum > BadgeController.MAX_UNREAD_COUNT) {
                unreadTextNumber = String.valueOf(BadgeController.MAX_UNREAD_COUNT);
                unreadTextPlusPaint.getTextBounds(unreadTextPlus, 0,
                        unreadTextPlus.length(), unreadTextPlusBounds);
            } else {
                unreadTextNumber = String.valueOf(info.unreadNum);
            }
            unreadTextNumberPaint.getTextBounds(unreadTextNumber, 0,
                    unreadTextNumber.length(), unreadTextNumberBounds);
            int textHeight = unreadTextNumberBounds.height();
            int textWidth = unreadTextNumberBounds.width() + unreadTextPlusBounds.width();

            /// M: Draw unread background image.
            NinePatchDrawable unreadBgNinePatchDrawable =
                    (NinePatchDrawable) res.getDrawable(R.drawable.ic_newevents_numberindication);
            int unreadBgWidth = unreadBgNinePatchDrawable.getIntrinsicWidth();
            int unreadBgHeight = unreadBgNinePatchDrawable.getIntrinsicHeight();

            int unreadMinWidth = (int) res.getDimension(R.dimen.unread_minWidth);
            if (unreadBgWidth < unreadMinWidth) {
                unreadBgWidth = unreadMinWidth;
            }
            int unreadTextMargin = (int) res.getDimension(R.dimen.unread_text_margin);
            if (unreadBgWidth < textWidth + unreadTextMargin) {
                unreadBgWidth = textWidth + unreadTextMargin;
            }
            if (unreadBgHeight < textHeight) {
                unreadBgHeight = textHeight;
            }
            Log.d("lijun", "unreadBgWidth : " + unreadBgWidth + ", unreadBgHeight" + unreadBgHeight);
            Rect unreadBgBounds = new Rect(0, 0, unreadBgWidth, unreadBgHeight);
            unreadBgNinePatchDrawable.setBounds(unreadBgBounds);

            int unreadMarginTop = 0;
            int unreadMarginRight = 0;
            int unreadBgPosX = 0;
            int unreadBgPosY = 0;
            if (info instanceof ShortcutInfo) {
                if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                    unreadMarginTop = (int) res.getDimension(R.dimen.hotseat_unread_margin_top);
                    unreadMarginRight = (int) res.getDimension(R.dimen.hotseat_unread_margin_right);
                } else if (info.container == (long) LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    unreadMarginTop = (int) res.getDimension(R.dimen.workspace_unread_margin_top);
                    unreadMarginRight = (int) res.getDimension(
                            R.dimen.workspace_unread_margin_right);
                } else {
                    unreadMarginTop = (int) res.getDimension(R.dimen.folder_unread_margin_top);
                    unreadMarginRight = (int) res.getDimension(R.dimen.folder_unread_margin_right);
                }
                BubbleTextView textView = (BubbleTextView) icon;
                int iconSize = textView.getIconSize();

                float textSize=textView.getTextSize();
                unreadBgPosX = iconSize-unreadBgWidth/2-1;
                unreadBgPosY = -unreadBgHeight/3;
            } else if (info instanceof FolderInfo) {
                if (info.container == (long) LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                    unreadMarginTop = (int) res.getDimension(R.dimen.hotseat_unread_margin_top);
                    unreadMarginRight = (int) res.getDimension(R.dimen.hotseat_unread_margin_right);
                } else if (info.container == (long) LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                    unreadMarginTop = (int) res.getDimension(R.dimen.workspace_unread_margin_top);
                    unreadMarginRight = (int) res.getDimension(
                            R.dimen.workspace_unread_margin_right);
                }
                return;
            } else if (info instanceof AppInfo) {
                unreadMarginTop = (int) res.getDimension(R.dimen.app_list_unread_margin_top);
                unreadMarginRight = (int) res.getDimension(R.dimen.app_list_unread_margin_right);
                unreadBgPosX = mView.getWidth()
                        - unreadBgWidth - unreadMarginRight;
                unreadBgPosY = unreadMarginTop;
            }



            Log.d("lijun", "unreadBgPosX : " + unreadBgPosX + ", unreadBgPosY : " + unreadBgPosY);
            destCanvas.save();
            destCanvas.translate(unreadBgPosX, unreadBgPosY);

            if (unreadBgNinePatchDrawable != null) {
                //unreadBgNinePatchDrawable.draw(destCanvas);
                if(unreadTextNumber!=null&&unreadTextNumber.length()>1) {
                    RectF rectF = new RectF(unreadBgBounds);
                    destCanvas.drawRoundRect(rectF, unreadBgHeight / 2 + 1, unreadBgHeight / 2 + 1, unreadBgPaint);
                }else {
                    destCanvas.drawCircle(unreadBgWidth/2,unreadBgHeight/2,unreadBgWidth/2,unreadBgPaint);
                }
            } else {
//                Log.d(TAG, "drawUnreadEventIfNeed: "
//                        + "unreadBgNinePatchDrawable is null pointer");
                return;
            }

//            Log.d("lijun", "DragView drawUnread : " + unreadTextNumber + ", " + info.getTargetComponent());
            /// M: Draw unread text.
            Paint.FontMetrics fontMetrics = unreadTextNumberPaint.getFontMetrics();
            if (info.unreadNum > BadgeController.MAX_UNREAD_COUNT) {
                destCanvas.drawText(unreadTextNumber,
                        (unreadBgWidth - unreadTextPlusBounds.width()) / 2,
                        (unreadBgHeight + textHeight) / 2,
                        unreadTextNumberPaint);
                destCanvas.drawText(unreadTextPlus,
                        (unreadBgWidth + unreadTextNumberBounds.width()) / 2,
                        (unreadBgHeight + textHeight) / 2 + fontMetrics.ascent / 2,
                        unreadTextPlusPaint);
            } else {
                destCanvas.drawText(unreadTextNumber,
                        unreadBgWidth / 2,
                        (unreadBgHeight + textHeight) / 2,
                        unreadTextNumberPaint);
            }
            Log.d("lijun", "drawUnreadForDragView canvas: " + destCanvas.getWidth() + "," + destCanvas.getHeight());
            destCanvas.restore();
        }
    }
}
