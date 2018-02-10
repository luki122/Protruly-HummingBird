package cn.com.protruly.filemanager.ui;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import hb.app.HbActivity;
import hb.widget.toolbar.Toolbar;
import cn.com.protruly.filemanager.R;

/**
 * Created by sqf on 17-5-9.
 */

public class ToolbarManager implements View.OnClickListener {

    //private static ToolbarManager sToolbarManager;

    private HbActivity mActivity;
    private Toolbar mToolbar;

    public static final int STATUS_MAIN_PAGE = 1;
    public static final int STATUS_CATEGORY_LIST = 2; //
    public static final int STATUS_CATEGORY_SEARCH_RESULT = 3;
    public static final int STATUS_CATEGORY_PATH_LIST = 4;
    public static final int STATUS_CATEGORY_PATH_HIDE_LIST = 5;
    public static final int STATUS_CATEGORY_HISTORY = 6;
    public static final int STATUS_ZIP_LIST = 7;
    public static final int STATUS_HOME_PATH_SELECTION = 8;
    public static final int STATUS_PATH_SELECTION = 9;
    public static final int STATUS_GLOBALSEARCH_HISTORY = 10;
    public static final int STATUS_GLOBALSEARCH_RESULT = 11;
    public static final int STATUS_HISTORY_PATH_LIST = 12;

    private OnToolbarNavigationIconClickListener mOnToolbarNavigationIconClickListener;

    public interface OnToolbarNavigationIconClickListener {
        public void onToolbarNavigationIconClicked();
    }

    public ToolbarManager(HbActivity activity) {
        mActivity = activity;
        mToolbar = activity.getToolbar();
    }

    @Override
    public void onClick(View v) {

    }

    public interface onNavigationIconClickListener {
        public void onNavigationIconClicked();
    }

    public void setToolbarTitle(int titleResId) {
        if(mToolbar==null || titleResId <= 0){
            return;
        }
        mToolbar.setTitle(titleResId);
    }

    public void setToolbarTitle(String title) {
        if(mToolbar==null || title==null){
            return;
        }
        mToolbar.setTitle(title);
    }

    private void inflateToolbarMenu(int menuResId) {
        clearToolbarMenu();
        mActivity.inflateToolbarMenu(menuResId);
    }

    public void hideNavigationIcon() {
        if(mToolbar == null) return;
        mToolbar.setNavigationIcon(null);
    }

    public void setNavigationIconAsBack() {
        if(mToolbar == null) return;
        mToolbar.setNavigationIcon(com.hb.R.drawable.ic_toolbar_back);
    }

    public void showToolbarMenuItem(int menuItemId, boolean visible) {
        Menu menu = mActivity.getOptionMenu();
        MenuItem item = menu.findItem(menuItemId);
        if(null == item) return;
        item.setVisible(visible);
    }

    public void setToolbarMenuItemEnable(int menuItemId, boolean enable) {
        Menu menu = mActivity.getOptionMenu();
        MenuItem item = menu.findItem(menuItemId);
        if(null == item) return;
        item.setEnabled(enable);
    }

    public void clearToolbarMenu() {
        Menu menu = mActivity.getOptionMenu();
        if(null == menu) return;
        menu.clear();
    }

    public void setOnNavigationIconClickListener(OnToolbarNavigationIconClickListener listener) {
        if(null == mToolbar) return;
        mOnToolbarNavigationIconClickListener = listener;
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnToolbarNavigationIconClickListener.onToolbarNavigationIconClicked();
            }
        });
    }

    public void setOnToolbarMenuItemClickListener(Toolbar.OnMenuItemClickListener listener) {
        if(null == mToolbar) return;
        mToolbar.setOnMenuItemClickListener(listener);
    }

    public void setMenuItemChecked(int menuItemId, boolean checked) {
        Menu menu = mActivity.getOptionMenu();
        MenuItem item = menu.findItem(menuItemId);
        item.setCheckable(true);
        item.setChecked(checked);
    }

    public void switchToStatus(int status) {
        switch (status) {
            case STATUS_MAIN_PAGE:
                hideNavigationIcon();
                setToolbarTitle(R.string.app_name);
                if(mActivity!=null) {
                    mActivity.showActionMode(false);
                    mActivity.setActionModeListener(null);
                }
                clearToolbarMenu();
                inflateToolbarMenu(R.menu.main_page_search);
                break;
            case STATUS_CATEGORY_LIST:
                clearToolbarMenu();
                inflateToolbarMenu(R.menu.category_toolbar_menu_operations);
                setNavigationIconAsBack();
                break;
            case STATUS_CATEGORY_SEARCH_RESULT:
                showToolbarMenuItem(R.id.action_sort, false);
                setToolbarTitle("");
                break;
            case STATUS_CATEGORY_PATH_LIST:
                clearToolbarMenu();
                inflateToolbarMenu(R.menu.more_path_menu_operations);
                showToolbarMenuItem(R.id.hide_hide_file,false);
                showToolbarMenuItem(R.id.show_hide_file,true);
                setNavigationIconAsBack();
                break;
            case STATUS_CATEGORY_PATH_HIDE_LIST:
                clearToolbarMenu();
                inflateToolbarMenu(R.menu.more_path_menu_operations);
                showToolbarMenuItem(R.id.hide_hide_file,true);
                showToolbarMenuItem(R.id.show_hide_file,false);
                setNavigationIconAsBack();
                break;
            case STATUS_ZIP_LIST:
                clearToolbarMenu();
                setNavigationIconAsBack();
                break;
            case STATUS_HOME_PATH_SELECTION:
                clearToolbarMenu();
                Log.d("mode","qqqqqqqqqqqqqqqqqqqqqqqqq");
                inflateToolbarMenu(R.menu.path_select_cancel);
                hideNavigationIcon();
                setToolbarTitle(mActivity.getResources().getString(R.string.select_file_target));
                break;
            case STATUS_PATH_SELECTION:
                clearToolbarMenu();
                inflateToolbarMenu(R.menu.path_select_cancel);
                break;
            case STATUS_GLOBALSEARCH_HISTORY:
                clearToolbarMenu();
                inflateToolbarMenu(R.menu.globalsearch_toolbar_menu);
                showToolbarMenuItem(R.id.action_sort, false);
                setToolbarTitle("");
                setNavigationIconAsBack();
                break;
            case STATUS_GLOBALSEARCH_RESULT:
                clearToolbarMenu();
                inflateToolbarMenu(R.menu.globalsearch_toolbar_menu);
                showToolbarMenuItem(R.id.action_sort, true);
                setToolbarTitle("");
                setNavigationIconAsBack();
                break;
            case STATUS_HISTORY_PATH_LIST:
                clearToolbarMenu();
                setToolbarTitle(R.string.category_history);
                setNavigationIconAsBack();
                break;
        }
    }
}
