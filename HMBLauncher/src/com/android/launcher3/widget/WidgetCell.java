/*
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

package com.android.launcher3.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewPropertyAnimator;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.SimpleOnStylusPressListener;
import com.android.launcher3.StylusEventHelper;
import com.android.launcher3.WidgetPreviewLoader;
import com.android.launcher3.WidgetPreviewLoader.PreviewLoadRequest;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.model.WidgetItem;

/**
 * Represents the individual cell of the widget inside the widget tray. The preview is drawn
 * horizontally centered, and scaled down if needed.
 *
 * This view does not support padding. Since the image is scaled down to fit the view, padding will
 * further decrease the scaling factor. Drag-n-drop uses the view bounds for showing a smooth
 * transition from the view to drag view, so when adding padding support, DnD would need to
 * consider the appropriate scaling factor.
 */
public class WidgetCell extends LinearLayout implements OnLayoutChangeListener {

    private static final String TAG = "WidgetCell";
    private static final boolean DEBUG = false;

    private static final int FADE_IN_DURATION_MS = 90;

    /** Widget cell width is calculated by multiplying this factor to grid cell width. */
    public static final float WIDTH_SCALE = 0.96f;//lijun modify 2.6 to 1.0

    /** Widget preview width is calculated by multiplying this factor to the widget cell width. */
    public static final float PREVIEW_SCALE = 0.96f;//lijun modify 0.8 to 0.96

    private int mPresetPreviewSize;
    int cellSize;

    private WidgetImageView mWidgetImage;
    private TextView mWidgetName;
    private TextView mWidgetDims;

    private WidgetItem mItem;

    private WidgetPreviewLoader mWidgetPreviewLoader;
    private PreviewLoadRequest mActiveRequest;
    private StylusEventHelper mStylusEventHelper;

    private final Launcher mLauncher;

    public WidgetCell(Context context) {
        this(context, null);
    }

    public WidgetCell(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetCell(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final Resources r = context.getResources();
        mLauncher = Launcher.getLauncher(context);
        mStylusEventHelper = new StylusEventHelper(new SimpleOnStylusPressListener(this), this);

        setContainerWidth();
        setWillNotDraw(false);
        setClipToPadding(false);
        setAccessibilityDelegate(mLauncher.getAccessibilityDelegate());
    }

    private void setContainerWidth() {
        DeviceProfile profile = mLauncher.getDeviceProfile();
        cellSize = (int) (profile.cellWidthPx * WIDTH_SCALE);
        mPresetPreviewSize = (int) (cellSize * PREVIEW_SCALE);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mWidgetImage = (WidgetImageView) findViewById(R.id.widget_preview);
        mWidgetName = ((TextView) findViewById(R.id.widget_name));
        //lijun modify start for page widgets
//        mWidgetDims = ((TextView) findViewById(R.id.widget_dims));
        if(!FeatureFlags.WIDGETS_CONTAINER_PAGE){
            mWidgetDims = ((TextView) findViewById(R.id.widget_dims));
        }
        //lijun modify end
    }

    /**
     * Called to clear the view and free attached resources. (e.g., {@link Bitmap}
     */
    public void clear() {
        if (DEBUG) {
            Log.d(TAG, "reset called on:" + mWidgetName.getText());
        }
        mWidgetImage.animate().cancel();
        mWidgetImage.setBitmap(null);
        mWidgetName.setText(null);
        if(!FeatureFlags.WIDGETS_CONTAINER_PAGE) {
            mWidgetDims.setText(null);
        }

        if (mActiveRequest != null) {
            mActiveRequest.cleanup();
            mActiveRequest = null;
        }
    }

    public void applyFromCellItem(WidgetItem item, WidgetPreviewLoader loader) {
        mItem = item;
        //lijun modify start
//        mWidgetName.setText(mItem.label);
        InvariantDeviceProfile profile =
                LauncherAppState.getInstance().getInvariantDeviceProfile();
        int hSpan = Math.min(mItem.spanX, profile.numColumns);
        int vSpan = Math.min(mItem.spanY, profile.numRows);
        String widgetName = mItem.label;
        String dimensionsFormatString = getContext().getString(R.string.widget_dims_format);
        String widgetDims = String.format(dimensionsFormatString, hSpan, vSpan);
        mWidgetName.setText(mergeDimsToName(widgetName,widgetDims));
        //lijun modify end
        if(!FeatureFlags.WIDGETS_CONTAINER_PAGE) {
            mWidgetDims.setText(getContext().getString(R.string.widget_dims_format,
                    mItem.spanX, mItem.spanY));
            mWidgetDims.setContentDescription(getContext().getString(
                    R.string.widget_accessible_dims_format, mItem.spanX, mItem.spanY));
        }
        mWidgetPreviewLoader = loader;

        if (item.activityInfo != null) {
            setTag(new PendingAddShortcutInfo(item.activityInfo));
        } else {
            setTag(new PendingAddWidgetInfo(mLauncher, item.widgetInfo));
        }
    }

    public int[] getPreviewSize() {
        int[] maxSize = new int[2];

        maxSize[0] = mPresetPreviewSize;
        maxSize[1] = mPresetPreviewSize;
        return maxSize;
    }

    public void applyPreview(Bitmap bitmap) {
        if (bitmap != null) {
            mWidgetImage.setBitmap(bitmap);
            mWidgetImage.setAlpha(0f);
            ViewPropertyAnimator anim = mWidgetImage.animate();
            anim.alpha(1.0f).setDuration(FADE_IN_DURATION_MS);
        }
    }

    public void ensurePreview() {
        if (mActiveRequest != null) {
            return;
        }
        int[] size = getPreviewSize();
        if (DEBUG) {
            Log.d(TAG, String.format("[tag=%s] ensurePreview (%d, %d):",
                    getTagToString(), size[0], size[1]));
        }
        mActiveRequest = mWidgetPreviewLoader.getPreview(mItem, size[0], size[1], this);
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
            int oldTop, int oldRight, int oldBottom) {
        removeOnLayoutChangeListener(this);
        ensurePreview();
    }

    public int getActualItemWidth() {
        ItemInfo info = (ItemInfo) getTag();
        int[] size = getPreviewSize();
        int cellWidth = mLauncher.getDeviceProfile().cellWidthPx;

        return Math.min(size[0], info.spanX * cellWidth);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean handled = super.onTouchEvent(ev);
        if (mStylusEventHelper.onMotionEvent(ev)) {
            return true;
        }
        return handled;
    }

    /**
     * Helper method to get the string info of the tag.
     */
    private String getTagToString() {
        if (getTag() instanceof PendingAddWidgetInfo ||
                getTag() instanceof PendingAddShortcutInfo) {
            return getTag().toString();
        }
        return "";
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return WidgetCell.class.getName();
    }

    /**
     * lijun add
     * @param widgetName
     * @param dims
     * @return
     */
    private static String mergeDimsToName(String widgetName , String dims) {
        String widgetDims = dims;
        widgetDims = widgetDims.replace('\u00d7','*');
        widgetDims = widgetDims.replace('X','*');
        widgetDims = widgetDims.replace('x','*');
        widgetDims = widgetDims.replace(" × ","*");
        widgetDims = widgetDims.replace(" * ","*");
        char[] widgetN = widgetName.toCharArray();
        int length = widgetN.length;
        for (int i = 0; i < length; i++) {
            if (widgetN[i] == 'x' || widgetN[i] == 'X' || widgetN[i] == '*' || widgetN[i] == '\u00d7') {
                if (i >= 1 && i <= length - 2 && Character.isDigit(widgetN[i - 1]) && Character.isDigit(widgetN[i + 1])) {
                    String left,right;
                    if (i >= 2 && i <= length - 3 && (('(' == widgetN[i - 2] && ')' == widgetN[i + 2]) || ('（' == widgetN[i - 2] && '）' == widgetN[i + 2]))) {
                        left = widgetName.substring(0, i - 2);
                        if(i == length-3){
                            right = "";
                        }else {
                            right = widgetName.substring(i + 3);
                        }
                    }else {
                        left = widgetName.substring(0, i - 1);
                        if(i == length-2){
                            right = "";
                        }else {
                            right = widgetName.substring(i + 2);
                        }
                    }
//                    return left + "(" + widgetDims + ")" + right;
                    if(left.length()==0){
                        return right + widgetDims;
                    }else {
                        return left + widgetDims + right;
                    }
                }
            }
        }
//        return "(" + widgetDims + ")" + widgetName;
        return widgetName + widgetDims;
//        return widgetName;
    }
}
