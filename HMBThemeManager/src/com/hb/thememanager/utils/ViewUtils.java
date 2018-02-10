package com.hb.thememanager.utils;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListAdapter;

/**
 * Created by alexluo on 17-8-16.
 */

public class ViewUtils {


    public static void setGridViewHeightBasedOnChildren(int columns,GridView gridView,ListAdapter adapter) {
       final ListAdapter listAdapter = adapter;

        if (listAdapter == null) {
            return;
        }
        int heightOffset = (int)(12 * gridView.getResources().getDisplayMetrics().density);
        int totalHeight = 0;
        int count = listAdapter.getCount();
            // i每次加col，相当于listAdapter.getCount()小于等于4时 循环一次，计算一次item的高度，
            // listAdapter.getCount()小于等于8时计算两次高度相加
            for (int i = 0; i < listAdapter.getCount(); i += columns) {
                // 获取listview的每一个item
                View listItem = listAdapter.getView(i, null, gridView);
                listItem.measure(0, 0);
                // 获取item的高度和
                totalHeight += listItem.getMeasuredHeight();

            }

        ViewGroup.LayoutParams params = gridView.getLayoutParams();
        params.height = totalHeight+heightOffset;
        gridView.setLayoutParams(params);
    }
}
