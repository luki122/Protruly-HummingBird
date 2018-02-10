package cn.com.protruly.filemanager.categorylist;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;

import cn.com.protruly.filemanager.enums.SortOrder;

/**
 * Created by sqf on 17-6-3.
 */

public class CategoryCursorLoader extends CursorLoader {

    private int mSortOrder;

    public CategoryCursorLoader(Context context, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, int sortOrderType) {
        super(context, uri, projection, selection, selectionArgs, sortOrder);
        mSortOrder = sortOrderType;
    }

    @Override
    public Cursor loadInBackground() {
        Cursor cursor = super.loadInBackground();
        if (SortOrder.SORT_ORDER_NAME == mSortOrder) {
            return new PinyinSortedCursor(cursor);
        }
        return cursor;
    }
}
