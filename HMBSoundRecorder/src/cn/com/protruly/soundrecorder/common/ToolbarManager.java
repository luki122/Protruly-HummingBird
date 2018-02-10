package cn.com.protruly.soundrecorder.common;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import cn.com.protruly.soundrecorder.R;
import hb.app.HbActivity;
import hb.widget.toolbar.Toolbar;

/**
 * Created by sqf on 17-5-9.
 */

public class ToolbarManager implements View.OnClickListener {

    private HbActivity mActivity;
    private Toolbar mToolbar;

    public static final int RECORDING_PAGE = 1;
    public static final int RECORD_LIST_SPK_MENU_ONE = 2;
    public static final int RECORD_LIST_SPK_MENU_LIST = 3;
    public static final int RECORD_EDIT_SPK_MENU_ONE = 4;
    public static final int RECORD_EDIT_SPK_MENU_LIST = 5;

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

    public void setToolbarMenuItemIcon(int menuItemId,int id){
        Menu menu = mActivity.getOptionMenu();
        MenuItem item = menu.findItem(menuItemId);
        if(null == item) return;
        item.setIcon(id);
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
            case RECORDING_PAGE:
                hideNavigationIcon();
                setToolbarTitle(R.string.app_name);
                break;
            case RECORD_LIST_SPK_MENU_LIST:
                hideNavigationIcon();
                setNavigationIconAsBack();
                setToolbarTitle(R.string.app_name);
                clearToolbarMenu();
                inflateToolbarMenu(R.menu.recordlist_toolbar_menu);
                break;
            case RECORD_LIST_SPK_MENU_ONE:
                hideNavigationIcon();
                setNavigationIconAsBack();
                setToolbarTitle(R.string.app_name);
                clearToolbarMenu();
                inflateToolbarMenu(R.menu.recordlist_toolbar_menu2);
                break;

            case RECORD_EDIT_SPK_MENU_LIST:
                clearToolbarMenu();
                inflateToolbarMenu(R.menu.recordlist_toolbar_menu);
                break;
            case RECORD_EDIT_SPK_MENU_ONE:
                clearToolbarMenu();
                inflateToolbarMenu(R.menu.recordlist_toolbar_menu2);
                break;

        }
    }
}
