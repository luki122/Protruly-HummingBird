package cn.com.protruly.filemanager.categorylist;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.Comparator;

import cn.com.protruly.filemanager.utils.GlobalConstants;
import cn.com.protruly.filemanager.utils.LogUtil;
import cn.com.protruly.filemanager.utils.PinyinCompareUtil;

/**
 * Created by sqf on 17-5-10.
 */

public class PinyinSortedCursor extends CursorWrapper  {

    private static final String TAG = "PinyinSortedCursor";
    private Cursor mCursor;
    private int mPos;
    private Integer [] mPositions;
    private PositionComparator mPositionComparator;

    /**
     * Creates a cursor wrapper.
     *
     * @param cursor The underlying cursor to wrap.
     */
    public PinyinSortedCursor(Cursor cursor) {
        super(cursor);
        mCursor = cursor;
        mPositionComparator = new PositionComparator();
        intPositionsAndSort();
    }

    public class PositionComparator implements Comparator<Integer> {
        @Override
        public int compare(Integer o1, Integer o2) {
            String name1 = getName(o1);
            String name2 = getName(o2);
            return PinyinCompareUtil.sComparator.compare(name1, name2);
        }
    }

    private void intPositionsAndSort() {
        if(mCursor == null || mCursor.isClosed()) return;
        int total = mCursor.getCount();
        if(total == 0) return;
        mPositions = new Integer[total];
        int i = 0;
        for(i=0; i<total; i++) {
            mPositions[i] = i;
        }
        Arrays.sort(mPositions, mPositionComparator);
        if(GlobalConstants.DEBUG) {
            LogUtil.i(TAG, " --------------------------------------------- ");
            for (i = 0; i < mPositions.length; i++) {
                LogUtil.i(TAG, "i:" + i + " " + mPositions[i]);
            }
            LogUtil.i(TAG, " --------------------------------------------- ");
        }
    }

    private String getName(int pos) {
        mCursor.moveToPosition(pos);
        String displayName = mCursor.getString(CategoryFactory.COLUMN_INDEX_DISPLAY_NAME);
        if(TextUtils.isEmpty(displayName)) {
            displayName = mCursor.getString(CategoryFactory.COLUMN_INDEX_TITLE);
        }
        return displayName;
    }

    @Override
    public boolean moveToFirst() {
        return moveToPosition(0);
    }

    @Override
    public boolean moveToLast() {
        return moveToPosition(mPositions.length - 1);
    }

    @Override
    public boolean move(int offset) {
        return moveToPosition(mPos + offset);
    }

    @Override
    public boolean moveToPosition(int position) {
        if (position >= 0 && position < mPositions.length) {
            mPos = position;
            int originalCursorPosition = mPositions[position];
            return mCursor.moveToPosition(originalCursorPosition);
        }
        if (position < 0) {
            mPos = -1;
        }
        if (position >= mPositions.length) {
            mPos = mPositions.length;
        }
        return mCursor.moveToPosition(position);
    }

    @Override
    public boolean moveToNext() {
        return moveToPosition(mPos + 1);
    }

    @Override
    public boolean isFirst() {
        return mPos == 0;
    }

    @Override
    public boolean isLast() {
        return mPos == mPositions.length - 1;
    }

    @Override
    public boolean moveToPrevious() {
        return moveToPosition(mPos - 1);
    }

    @Override
    public int getPosition() {
        return mPos;
    }


}
