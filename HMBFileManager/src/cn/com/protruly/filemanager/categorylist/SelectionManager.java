package cn.com.protruly.filemanager.categorylist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import cn.com.protruly.filemanager.enums.FileInfo;

/**
 * Created by sqf on 17-5-11.
 */

public class SelectionManager {

    private Set<String> mSelected = new HashSet<String>();
    private SelectionListener mSelectionListener;
    private int mAllSize = -1;
    private boolean mInverseSelection;

    public SelectionListener getmSelectionListener() {
        return mSelectionListener;
    }

    public boolean mInSelectionMode;

    public interface SelectionListener {
        public void onEnterSelectionMode();
        public void onLeaveSelectionMode();
        public void onAllSelected();
        public void onAllUnselected();
        public void onSelectionChange(String filePath, boolean selected);
    }

    public void setSelectionListener(SelectionListener listener) {
        mSelectionListener = listener;
    }

    public void setAllSize(int allSize) {
        mAllSize = allSize;
    }

    public void selectAll() {
        mInverseSelection = true;
        mSelected.clear();
        mSelectionListener.onAllSelected();
    }

    public void unselectAll() {
        mInverseSelection = false;
        mSelected.clear();
        mSelectionListener.onAllUnselected();
    }

    /**
     *
    //mInverseSelection    mSelected.isEmpty()
    // true/false               false                 maybe 2,3... items, doesn't make sense
    // fasle                    true                  mSelectionListener.onAllUnselected();
    // true                     true                  mSelectionListener.onAllSelected();

    //mInverseSelection     mSelected.size()==mAllSize
    // true/false               false                 maybe 2,3... items, doesn't make sense
    // true                     true                  mSelectionListener.onAllUnselected();
    // fasle                    true                  mSelectionListener.onAllSelected();
     */
    public boolean isAllSelected() {
        return (mInverseSelection && mSelected.isEmpty()) ||
                (!mInverseSelection && mSelected.size() == mAllSize);
    }

    public boolean isAllUnselected() {
        return (!mInverseSelection && mSelected.isEmpty()) ||
                (mInverseSelection && mSelected.size() == mAllSize);
    }

    public void toggle(String key) {
        checkAllSize();

        if(!mInverseSelection && mSelected.isEmpty()) {
            enterSelectionMode();
        }

        if(mSelected.contains(key)) {
            mSelected.remove(key);
        } else {
            mSelected.add(key);
        }

        if(isAllSelected()) {
            mSelectionListener.onAllSelected();
        } else if(isAllUnselected()) {
            mSelectionListener.onAllUnselected();
        }

        if (mSelectionListener != null) mSelectionListener.onSelectionChange(key, isSelected(key));
    }

    public boolean isSelected(String key) {
        if( ! mInSelectionMode) return false;
        //mInverseSelection    contains
        //     true               true          false
        //     true               false         true
        //     false              true          true
        //     false              false         false
        return mInverseSelection ^ mSelected.contains(key);
    }

    public int getSelectedCount() {
        int count = mSelected.size();
        if (mInverseSelection) {
            count = mAllSize - count;
        }
        return count;
    }

    /*
    public ArrayList<String> getSelected() {
        return null;
    }
    */

    private void checkAllSize() {
        if(-1 == mAllSize) {
            throw new IllegalArgumentException("mAllSize not set");
        }
    }

    public void enterSelectionMode() {
        if(mInSelectionMode) return;
        mInSelectionMode = true;
        mSelectionListener.onEnterSelectionMode();
    }

    public void leaveSelectionMode() {
        if( ! mInSelectionMode) return;
        mInSelectionMode = false;
        mInverseSelection = false;
        mSelected.clear();
        mSelectionListener.onLeaveSelectionMode();
    }

    public boolean isInSelectionMode() {
        return mInSelectionMode;
    }
}
