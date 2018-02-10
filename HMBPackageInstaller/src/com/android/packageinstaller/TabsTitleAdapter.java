/*
**
** Copyright 2013, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/
package com.android.packageinstaller;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import hb.widget.PagerAdapter;

/**
 * This is a helper class that implements the management of tabs and all
 * details of connecting a ViewPager with associated TabHost.  It relies on a
 * trick.  Normally a tab host has a simple API for supplying a View or
 * Intent that each tab will show.  This is not sufficient for switching
 * between pages.  So instead we make the content part of the tab host
 * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
 * view to show as the tab content.  It listens to changes in tabs, and takes
 * care of switch to the correct paged in the ViewPager whenever the selected
 * tab changes.
 */
public class TabsTitleAdapter extends PagerAdapter {
    private final Context mContext;
    private ArrayList<View> mViews = new ArrayList<View>();
    private ArrayList<String> mTitles = new ArrayList<String>();

    public TabsTitleAdapter(Activity activity) {
        mContext = activity;
    }

    public void addTab(int position, String tabTitle, View view) {
        mViews.add(position, view);
        mTitles.add(position, tabTitle);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mViews.size();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = mViews.get(position);
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles.get(position);
    }
//    public void setOnTabChangedListener(TabHost.OnTabChangeListener listener) {
//        mOnTabChangeListener = listener;
//    }

//    @Override
//    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//    }

//    @Override
//    public void onPageSelected(int position) {
    // Unfortunately when TabHost changes the current tab, it kindly
    // also takes care of putting focus on it when not in touch mode.
    // The jerk.
    // This hack tries to prevent this from pulling focus out of our
    // ViewPager.
//        TabWidget widget = mTabHost.getTabWidget();
//        int oldFocusability = widget.getDescendantFocusability();
//        widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
//        mTabHost.setCurrentTab(position);
//        widget.setDescendantFocusability(oldFocusability);
//
//        // Scroll the current tab into visibility if needed.
//        View tab = widget.getChildTabViewAt(position);
//        mTempRect.set(tab.getLeft(), tab.getTop(), tab.getRight(), tab.getBottom());
//        widget.requestRectangleOnScreen(mTempRect, false);
//
//        // Make sure the scrollbars are visible for a moment after selection
//        final View contentView = mTabs.get(position).view;
//        if (contentView instanceof CaffeinatedScrollView) {
//            ((CaffeinatedScrollView) contentView).awakenScrollBars();
//        }
//    }
//
//    @Override
//    public void onPageScrollStateChanged(int state) {
//    }
}
