package com.android.launcher3.folder;

import android.content.res.Resources;

import com.android.launcher3.R;

/**
 * Created by liuzuo on 17-3-9.
 */

public class SudokuFolderIconLayoutRule implements FolderIcon.PreviewLayoutRule {
    static final int MAX_NUM_ITEMS_IN_PREVIEW = 9;
    private final String TAG = "SudokuFolderIconLayoutRule";
    final float MIN_SCALE = 0.48f;
    final float MAX_SCALE = 0.18f;
    private final  int MAX_ROW = 3;
    private final  int MAX_COLUMN = 3;
    private int folderGapWidth;
    private int folderGapHeight;
    private int folderBgSize;
    private float[] mTmpPoint = new float[2];

    private float mAvailableSpace;
    private float mIconSize;
    private boolean mIsRtl;
    private float mBaselineIconScale;

    @Override
    public void init(int availableSpace, int intrinsicIconSize, boolean rtl ,Resources r ) {
        mAvailableSpace = availableSpace;
        mIconSize = intrinsicIconSize;
        mIsRtl = rtl;
        mBaselineIconScale = availableSpace / (intrinsicIconSize * 1f);
        folderGapWidth = r.getDimensionPixelSize(R.dimen.folder_width_gap);
        folderGapHeight = r.getDimensionPixelSize(R.dimen.folder_height_gap);
        folderBgSize =  r.getDimensionPixelSize(R.dimen.folder_bg_size);
    }

    @Override
    public FolderIcon.PreviewItemDrawingParams computePreviewItemDrawingParams(int index,
                                                                               int curNumItems, FolderIcon.PreviewItemDrawingParams params) {

        float totalScale = scaleForNumItems(curNumItems);
        float transX;
        float transY;
        float overlayAlpha = 0;

        // Items beyond those displayed in the preview are animated to the center
        if (index >= MAX_NUM_ITEMS_IN_PREVIEW) {
            transX = transY = mAvailableSpace / 3 - (mIconSize * totalScale) / 3;
        } else {
            getPosition(index, curNumItems, mTmpPoint);
            transX = mTmpPoint[0];
            transY = mTmpPoint[1];
        }

        if (params == null) {
            params = new FolderIcon.PreviewItemDrawingParams(transX, transY, totalScale, overlayAlpha);
        } else {
            params.update(transX, transY, totalScale);
            params.overlayAlpha = overlayAlpha;
        }
        return params;
    }

    private void getPosition(int index, int curNumItems, float[] result) {
//        result[0] =/*mIconSize*getGapScaleX()/2*/+Math.abs(mAvailableSpace - mIconSize) /2+ mIconSize * scaleXForNumItems(index) * (index % 3);
//        result[1] =  /*mIconSize*getGapScaleX()/2*/+Math.abs(mAvailableSpace - mIconSize) / 2 + mIconSize * scaleYForNumItems(index) * (index / 3);
        result[0] =folderBgSize*getGapScaleX()/2+ folderBgSize * scaleXForNumItems(index) * (index % 3)-1;
        result[1] =  folderBgSize*getGapScaleY()/2 + folderBgSize * scaleYForNumItems(index) * (index / 3)-1;

    }

    private float scaleForNumItems(int numItems) {
            return MAX_SCALE * mBaselineIconScale;
    }
    private float scaleXForNumItems(int numItems) {
            float gapScaleX =getGapScaleX();
            return MAX_SCALE +gapScaleX/2;
    }
    private float scaleYForNumItems(int numItems) {
            float gapScaleY = getGapScaleY();
            return MAX_SCALE + gapScaleY/2;
    }
    @Override
    public int numItems() {
        return MAX_NUM_ITEMS_IN_PREVIEW;
    }

    @Override
    public boolean clipToBackground() {
        return false;
    }
    private float getGapScaleX(){
        return  (1 - MAX_COLUMN * MAX_SCALE) / (MAX_COLUMN - 1);
    }
    private float getGapScaleY(){
        return  (1 - MAX_ROW * MAX_SCALE) / (MAX_ROW - 1);
    }
}
